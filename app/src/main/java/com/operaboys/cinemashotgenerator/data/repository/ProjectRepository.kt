package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.ProjectDao
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.project.Project
import com.operaboys.cinemashotgenerator.domain.project.ProjectSummary
import com.operaboys.cinemashotgenerator.domain.project.archiveProject as applyArchiveTransition
import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

// واحد ۱۶ فاز ۱ — اولین Repository ای که مستقیماً برای واحد ۱۶ ساخته شده (نه بلوپرینت
// ۱۵). تصمیم مستند (docs/adr/044-unit16-phase1-app-shell.md، «آیا ViewModel مستقیماً
// از ProjectDao استفاده کند یا یک ProjectRepository ساخته شود»): یک Repository
// ساخته شد — طبق الگوی مستقر بقیه‌ی پروژه (SceneRepository/AssetRepository/...، هیچ
// ViewModel/مصرف‌کننده‌ای مستقیماً یک DAO خام نمی‌بیند)، و چون این لایه دقیقاً همان
// جایی است که تصمیمات دامنه‌ای (Rule واقعی Archive، تولید شناسه/Timestamp) باید
// بنشینند، نه در ViewModel لایه‌ی UI.
class ProjectRepository(
    private val projectDao: ProjectDao,
    private val idProvider: () -> String = ::defaultProjectId,
    private val clock: () -> String = ::nowIso8601
) {
    fun observeProjectSummaries(): Flow<List<ProjectSummary>> =
        projectDao.getAllProjectsWithCounts().map { rows -> rows.map { it.toSummary() } }

    fun observeProjectSummary(projectId: String): Flow<ProjectSummary?> =
        projectDao.getProjectWithCounts(projectId).map { it?.toSummary() }

    suspend fun createProject(name: String, uiLanguage: Language = Language.FA): Result<Project> = runCatching {
        val now = clock()
        val project = Project(
            projectId = idProvider(),
            projectName = name,
            createdAt = now,
            lastModified = now,
            uiLanguage = uiLanguage,
            state = EntityState.DRAFT
        )
        projectDao.saveProject(project.toEntity())
        project
    }

    suspend fun renameProject(projectId: String, newName: String): Result<Unit> = runCatching {
        val entity = projectDao.loadProject(projectId)
            ?: throw IllegalArgumentException("پروژه یافت نشد: $projectId")
        projectDao.saveProject(entity.copy(projectName = newName, lastModified = clock()))
    }

    /**
     * Rule واقعی State Machine واحد ۱۲ (domain.project.archiveProject) — طبق
     * ALLOWED_TRANSITIONS، فقط پروژه‌های FINAL قابل آرشیو شدن‌اند؛ برای بقیه
     * Result.failure با پیام دقیق برمی‌گردد، نه یک No-Op بی‌صدا یا Bypass ساختگی.
     */
    suspend fun archiveProject(projectId: String): Result<Unit> {
        val project = projectDao.loadProject(projectId)?.toDomain()
            ?: return Result.failure(IllegalArgumentException("پروژه یافت نشد: $projectId"))
        val archived = applyArchiveTransition(project).getOrElse { return Result.failure(it) }
        return runCatching {
            projectDao.saveProject(archived.copy(lastModified = clock()).toEntity())
        }
    }

    /**
     * محدودیت مستند (ADR-044): فقط ردیف Project کپی می‌شود (نام/شناسه/وضعیت تازه‌ی
     * DRAFT) — نه Scene/Shot/Asset/ProjectDna آن. تکثیر عمیق محتوا به فازی موکول شد
     * که آن Content واقعاً از طریق UI ساخته می‌شود (فاز ۲ به بعد)؛ بدون بازنویسی
     * شناسه‌های فرزند (sceneId/shotId/...)، یک تکثیر عمیق ساده‌لوحانه داده‌ی پروژه‌ی
     * اصلی را (نه کپی‌اش را) خراب می‌کرد — جزئیات کامل در ADR-044.
     */
    suspend fun duplicateProject(projectId: String): Result<Project> = runCatching {
        val original = projectDao.loadProject(projectId)?.toDomain()
            ?: throw IllegalArgumentException("پروژه یافت نشد: $projectId")
        val now = clock()
        val duplicate = original.copy(
            projectId = idProvider(),
            projectName = "${original.projectName} (کپی)",
            createdAt = now,
            lastModified = now,
            state = EntityState.DRAFT
        )
        projectDao.saveProject(duplicate.toEntity())
        duplicate
    }

    suspend fun deleteProject(projectId: String): Result<Unit> = runCatching {
        val entity = projectDao.loadProject(projectId)
            ?: throw IllegalArgumentException("پروژه یافت نشد: $projectId")
        projectDao.deleteProject(entity)
    }
}

private fun defaultProjectId(): String = "proj_" + UUID.randomUUID().toString().replace("-", "").take(12)
private fun nowIso8601(): String = Instant.now().toString()
