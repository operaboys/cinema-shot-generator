package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.AssetDao
import com.operaboys.cinemashotgenerator.data.dao.AudioContextDao
import com.operaboys.cinemashotgenerator.data.dao.ProjectDao
import com.operaboys.cinemashotgenerator.data.dao.ProjectDnaDao
import com.operaboys.cinemashotgenerator.data.dao.SceneDao
import com.operaboys.cinemashotgenerator.data.dao.ShotDao
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.domain.storage.ShotReferenceData
import com.operaboys.cinemashotgenerator.domain.storage.validateReferentialIntegrity
import java.time.Instant
import kotlinx.serialization.json.Json

// واحد ۱۵ — تکمیل Export/Import (آخرین مورد فهرست ADR-017). منبع حقیقت:
// docs/blueprints/15-project-storage.md، بخش «Export/Import».
//
// این فایل عمدتاً بازترکیب زیرساخت موجود است، نه کد جدید بزرگ:
// serializeFullProject/deserializeFullProject/restoreProjectFromSnapshot/
// BackupFileStorage از قدم قبل (BackupManager/ADR-023) بازاستفاده شدند؛ **یافته‌ی
// مهم:** validateReferentialIntegrity/IntegrityIssue/ShotReferenceData هم از قبل
// در domain/storage/ReferentialIntegrity.kt وجود داشتند — از همان قدم اول واحد ۱۵
// (commit 311f23d)، ساخته شده ولی تا این قدم به هیچ Import/Export ای وصل نشده
// بودند. این قدم آن‌ها را دوباره نساخت، فقط واقعاً سیم‌کشی‌شان کرد. جزئیات کامل در
// docs/adr/024-unit15-export-import-deviations.md.
//
// تفاوت واقعی Export با Backup: نام فایل مشخص/معنادار (نه نام خودکار محدود به
// تعداد مثل backup_*)، بدون cleanOldBackups (Export یک عملیات دستی و
// بدون‌محدودیت‌تعداد است). نام‌گذاری فایل: بلوپرینت کد مفهومی‌اش
// "${projectId}_export.json" (بدون timestamp) است؛ طبق دستور صریح این قدم، یک
// timestamp اضافه شد ("${projectId}_export_<epochMillis>.json") تا دو Export
// پیاپی از یک پروژه یکدیگر را overwrite نکنند.
//
// وضعیت Atomicity: restoreProjectFromSnapshot (از قدم قبل) در یک Room @Transaction
// واحد پیچیده نشده — همان پذیرش ریسک ADR-023. آنچه این قدم واقعاً تضمین می‌کند این
// است: اگر یکپارچگی ارجاعی نقض شده باشد، restoreProjectFromSnapshot اصلاً فراخوانی
// نمی‌شود — یعنی برای این مسیر خطا خاص، تضمین «صفر نوشتن» با اطمینان کامل برقرار
// است، نه با تکیه بر Rollback سطح دیتابیس.

private val exportImportJson = Json { ignoreUnknownKeys = true }

suspend fun exportProject(
    projectId: String,
    projectDao: ProjectDao,
    sceneDao: SceneDao,
    shotDao: ShotDao,
    assetDao: AssetDao,
    projectDnaDao: ProjectDnaDao,
    audioContextDao: AudioContextDao,
    backupFileStorage: BackupFileStorage,
    clock: () -> String = ::defaultExportTimestamp
): Result<String> {
    val projectData = serializeFullProject(
        projectId, projectDao, sceneDao, shotDao, assetDao, projectDnaDao, audioContextDao
    ).getOrElse { return Result.failure(it) }

    return runCatching {
        backupFileStorage.writeFile("${projectId}_export_${clock()}.json", projectData)
    }
}

suspend fun importProject(
    fileUri: String,
    backupFileStorage: BackupFileStorage,
    projectDao: ProjectDao,
    sceneDao: SceneDao,
    shotDao: ShotDao,
    assetDao: AssetDao,
    projectDnaDao: ProjectDnaDao,
    audioContextDao: AudioContextDao
): Result<ProjectEntity> {
    val content = runCatching { backupFileStorage.readFile(fileUri) }
        .getOrElse { return Result.failure(it) }
    val snapshot = deserializeFullProject(content).getOrElse { return Result.failure(it) }

    // sceneId مستقیماً از ShotEntityDto (فیلد سطح Room، همیشه در دسترس) خوانده
    // می‌شود؛ فقط characterIds/objectIds/locationIds نیاز به Decode کردن
    // shotDataJson دارند (این سه فیلد در سطح Entity وجود ندارند). اگر shotDataJson
    // یک شات به‌درستی Decode نشود، خودش یک نشانه‌ی فایل خراب است — به‌جای بی‌صدا
    // نادیده‌گرفتن، به‌عنوان Result.failure واقعی گزارش می‌شود (نه فقط لیست خالی
    // فرضی).
    val shotReferences = runCatching {
        snapshot.shots.map { shot ->
            val dto = exportImportJson.decodeFromString(ShotDto.serializer(), shot.shotDataJson)
            ShotReferenceData(
                shotId = shot.shotId,
                sceneId = shot.sceneId,
                characterIds = dto.characterIds,
                objectIds = dto.objectIds,
                locationIds = dto.locationIds
            )
        }
    }.getOrElse { return Result.failure(it) }

    val sceneIds = snapshot.scenes.map { it.sceneId }.toSet()
    val assetIds = snapshot.assets.map { it.assetId }.toSet()
    val issues = validateReferentialIntegrity(shotReferences, sceneIds, assetIds)
    if (issues.isNotEmpty()) {
        val details = issues.joinToString("; ") { "${it.source} -> ${it.brokenReferenceTo}: ${it.message}" }
        return Result.failure(IllegalStateException("یکپارچگی ارجاعی نقض شده، Import انجام نشد: $details"))
    }

    restoreProjectFromSnapshot(
        snapshot, projectDao, sceneDao, shotDao, assetDao, projectDnaDao, audioContextDao
    ).getOrElse { return Result.failure(it) }

    val restoredProject = projectDao.loadProject(snapshot.project.projectId)
        ?: return Result.failure(IllegalStateException("Import ناموفق: پروژه بعد از نوشتن یافت نشد"))
    return Result.success(restoredProject)
}

private fun defaultExportTimestamp(): String = Instant.now().toEpochMilli().toString()
