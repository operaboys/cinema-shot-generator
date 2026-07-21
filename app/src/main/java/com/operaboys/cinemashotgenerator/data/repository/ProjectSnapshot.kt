package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.AssetDao
import com.operaboys.cinemashotgenerator.data.dao.AudioContextDao
import com.operaboys.cinemashotgenerator.data.dao.ProjectDao
import com.operaboys.cinemashotgenerator.data.dao.ProjectDnaDao
import com.operaboys.cinemashotgenerator.data.dao.SceneDao
import com.operaboys.cinemashotgenerator.data.dao.ShotDao
import com.operaboys.cinemashotgenerator.data.entity.AssetEntity
import com.operaboys.cinemashotgenerator.data.entity.AudioContextEntity
import com.operaboys.cinemashotgenerator.data.entity.ProjectDnaEntity
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.data.entity.SceneEntity
import com.operaboys.cinemashotgenerator.data.entity.ShotEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// واحد ۱۵ — تکمیل BackupManager: پیاده‌سازی واقعی serializeFullProject/
// deserializeFullProject/restoreProjectFromSnapshot (کد مفهومی بلوپرینت ۱۵ فقط اسم
// این توابع را برده بود، بدون بدنه).
//
// تصمیم Scope («یک پروژه‌ی کامل یعنی چه؟»): طبق تعریف صریح دستور کار، شامل
// ProjectEntity + تمام Scene/Shot/Asset/ProjectDna/AudioContext مرتبط با همان
// projectId است. با grep در همه‌ی DAO ها (data/dao/) تأیید شد PromptBlueprint/
// RenderedOutput (خروجی تولیدشده، از داده‌ی بالا با Pipeline واحد ۱۱→۱۴ دوباره
// قابل‌ساخت است، نه بخشی از «حالت منبع» پروژه) و Override/Version/EventLog
// (entityId در این سه Polymorphic است — DAO فعلی هیچ کوئری «همه‌ی X های یک پروژه» را
// پشتیبانی نمی‌کند، فقط per-entityId؛ ساختن چنین کوئری‌ای خارج از Scope همین قدم است)
// و DependencyEdge (بدون فیلد projectId اصلاً — گراف وابستگی سراسری است، نه
// محدودشده به یک پروژه) عمداً از این Snapshot کنار گذاشته شدند. جزئیات کامل و
// استدلال در docs/adr/023-unit15-backup-manager-deviations.md.
//
// DTO های محلی @Serializable (نه Entity های Room مستقیم) — هم‌الگو با تصمیم قبلی
// پروژه (ShotDto/SceneDto و...) که domain را از kotlinx.serialization جدا نگه
// می‌دارد؛ اینجا همان جداسازی بین Entity Room (که Room/KSP آن را می‌خواند) و DTO
// سریالایز (که فقط برای فایل Backup لازم است) اعمال شد.

@Serializable
data class ProjectEntityDto(
    val projectId: String,
    val projectName: String,
    val createdAt: String,
    val lastModified: String,
    val uiLanguage: String
)

@Serializable
data class SceneEntityDto(val sceneId: String, val projectId: String, val sceneDataJson: String)

@Serializable
data class ShotEntityDto(val shotId: String, val sceneId: String, val shotDataJson: String)

@Serializable
data class AssetEntityDto(
    val assetId: String,
    val projectId: String,
    val assetType: String,
    val assetDataJson: String
)

@Serializable
data class ProjectDnaEntityDto(val dnaId: String, val projectId: String, val dnaDataJson: String)

@Serializable
data class AudioContextEntityDto(
    val audioContextId: String,
    val shotId: String,
    val audioContextDataJson: String
)

@Serializable
data class FullProjectSnapshot(
    val project: ProjectEntityDto,
    val scenes: List<SceneEntityDto>,
    val shots: List<ShotEntityDto>,
    val assets: List<AssetEntityDto>,
    val projectDna: ProjectDnaEntityDto?,
    val audioContexts: List<AudioContextEntityDto>
)

private val snapshotJson = Json { ignoreUnknownKeys = true }

/**
 * پروژه‌ی کامل (طبق تعریف بالا) را از DAO های واقعی می‌خواند و به یک رشته‌ی JSON
 * واحد سریالایز می‌کند. سه DAO ای که Flow برمی‌گردانند (Scene/Shot/Asset) با
 * `.first()` به یک خواندن تک‌باره تبدیل می‌شوند — Room برای کوئری‌های Flow همیشه
 * مقدار جاری را بلافاصله در subscribe اول Emit می‌کند، پس این معادل یک خواندن
 * suspend معمولی است، نه یک اشتراک زنده‌ی نگه‌داشته‌شده.
 */
suspend fun serializeFullProject(
    projectId: String,
    projectDao: ProjectDao,
    sceneDao: SceneDao,
    shotDao: ShotDao,
    assetDao: AssetDao,
    projectDnaDao: ProjectDnaDao,
    audioContextDao: AudioContextDao
): Result<String> = runCatching {
    val project = projectDao.loadProject(projectId)
        ?: throw IllegalArgumentException("پروژه یافت نشد: $projectId")
    val scenes = sceneDao.getScenesForProject(projectId).first()
    val shots = scenes.flatMap { scene -> shotDao.getShotsForScene(scene.sceneId).first() }
    val assets = assetDao.getAssetsForProject(projectId).first()
    val projectDna = projectDnaDao.loadProjectDnaForProject(projectId)
    val audioContexts = shots.mapNotNull { shot -> audioContextDao.loadAudioContextForShot(shot.shotId) }

    val snapshot = FullProjectSnapshot(
        project = project.toDto(),
        scenes = scenes.map { it.toDto() },
        shots = shots.map { it.toDto() },
        assets = assets.map { it.toDto() },
        projectDna = projectDna?.toDto(),
        audioContexts = audioContexts.map { it.toDto() }
    )
    snapshotJson.encodeToString(FullProjectSnapshot.serializer(), snapshot)
}

fun deserializeFullProject(json: String): Result<FullProjectSnapshot> = runCatching {
    snapshotJson.decodeFromString(FullProjectSnapshot.serializer(), json)
}

/**
 * تمام Entity های یک Snapshot را در DAO های واقعی می‌نویسد. ترتیب دقیقاً «والد قبل
 * از فرزند» است (Project → Scene → Shot → AudioContext → Asset → ProjectDna) —
 * طبق هشدار صریح ADR-017: چون saveXxx از OnConflictStrategy.REPLACE استفاده می‌کند
 * و REPLACE در SQLite با حذف+درج پیاده می‌شود، اگر یک ردیف والد (مثلاً Project یا
 * Scene) از قبل با همان کلید وجود داشته باشد، REPLACE آن یک Cascade Delete روی
 * فرزندانش راه می‌اندازد. با نوشتن والد همیشه قبل از فرزند، این Cascade Delete
 * احتمالی همیشه قبل از این‌که فرزند تازه نوشته شود اتفاق می‌افتد، نه بعد از آن.
 */
suspend fun restoreProjectFromSnapshot(
    snapshot: FullProjectSnapshot,
    projectDao: ProjectDao,
    sceneDao: SceneDao,
    shotDao: ShotDao,
    assetDao: AssetDao,
    projectDnaDao: ProjectDnaDao,
    audioContextDao: AudioContextDao
): Result<Unit> = runCatching {
    projectDao.saveProject(snapshot.project.toEntity())
    snapshot.scenes.forEach { sceneDao.saveScene(it.toEntity()) }
    snapshot.shots.forEach { shotDao.saveShot(it.toEntity()) }
    snapshot.audioContexts.forEach { audioContextDao.saveAudioContext(it.toEntity()) }
    snapshot.assets.forEach { assetDao.saveAsset(it.toEntity()) }
    snapshot.projectDna?.let { projectDnaDao.saveProjectDna(it.toEntity()) }
}

private fun ProjectEntity.toDto() = ProjectEntityDto(projectId, projectName, createdAt, lastModified, uiLanguage)
private fun ProjectEntityDto.toEntity() = ProjectEntity(projectId, projectName, createdAt, lastModified, uiLanguage)

private fun SceneEntity.toDto() = SceneEntityDto(sceneId, projectId, sceneDataJson)
private fun SceneEntityDto.toEntity() = SceneEntity(sceneId, projectId, sceneDataJson)

private fun ShotEntity.toDto() = ShotEntityDto(shotId, sceneId, shotDataJson)
private fun ShotEntityDto.toEntity() = ShotEntity(shotId, sceneId, shotDataJson)

private fun AssetEntity.toDto() = AssetEntityDto(assetId, projectId, assetType, assetDataJson)
private fun AssetEntityDto.toEntity() = AssetEntity(assetId, projectId, assetType, assetDataJson)

private fun ProjectDnaEntity.toDto() = ProjectDnaEntityDto(dnaId, projectId, dnaDataJson)
private fun ProjectDnaEntityDto.toEntity() = ProjectDnaEntity(dnaId, projectId, dnaDataJson)

private fun AudioContextEntity.toDto() = AudioContextEntityDto(audioContextId, shotId, audioContextDataJson)
private fun AudioContextEntityDto.toEntity() = AudioContextEntity(audioContextId, shotId, audioContextDataJson)
