package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.AssetDao
import com.operaboys.cinemashotgenerator.data.dao.AudioContextDao
import com.operaboys.cinemashotgenerator.data.dao.ProjectDao
import com.operaboys.cinemashotgenerator.data.dao.ProjectDnaDao
import com.operaboys.cinemashotgenerator.data.dao.SceneDao
import com.operaboys.cinemashotgenerator.data.dao.ShotDao
import java.util.UUID

// واحد ۱۵ — تکمیل BackupManager (طبق ADR-017/022، دومین و آخرین کلاسی که فقط امضا
// داشت). منبع حقیقت: docs/blueprints/15-project-storage.md، بخش «Backup Manager».
//
// مثل AutoSaveManager (ADR-022)، هیچ Timer/Debounce واقعی («هر ۵ دقیقه») در این
// لایه ساخته نشد — همان استدلال: زمان‌بندی به Lifecycle واقعی اپ وابسته است که فقط
// لایه‌ی ViewModel/UI (واحد ۱۶، هنوز ساخته نشده) دارد. backupIntervalMinutes به‌عنوان
// مقدار پیکربندی public نگه داشته شد، دقیقاً هم‌الگو با intervalSeconds در
// AutoSaveManager.
class BackupManager(
    private val projectId: String,
    private val projectDao: ProjectDao,
    private val sceneDao: SceneDao,
    private val shotDao: ShotDao,
    private val assetDao: AssetDao,
    private val projectDnaDao: ProjectDnaDao,
    private val audioContextDao: AudioContextDao,
    private val backupFileStorage: BackupFileStorage,
    val backupIntervalMinutes: Long = 5,
    private val maxBackupsToKeep: Int = 10,
    private val idProvider: () -> String = ::defaultBackupId
) {
    suspend fun createBackup(): Result<String> {
        val projectData = serializeFullProject(
            projectId, projectDao, sceneDao, shotDao, assetDao, projectDnaDao, audioContextDao
        ).getOrElse { return Result.failure(it) }

        val backupId = idProvider()
        val backupPath = backupFileStorage.writeFile(backupFileName(backupId), projectData)
        cleanOldBackups()
        return Result.success(backupPath)
    }

    /** فقط جدیدترین maxBackupsToKeep بک‌آپ نگه داشته می‌شود؛ بقیه حذف می‌شوند. */
    private suspend fun cleanOldBackups() {
        val backups = backupFileStorage.listFiles(backupPrefix()).sortedByDescending { it.createdAt }
        backups.drop(maxBackupsToKeep).forEach { backupFileStorage.deleteFile(it.path) }
    }

    suspend fun restoreFromBackup(backupId: String): Result<Unit> {
        val expectedFileName = backupFileName(backupId)
        val match = backupFileStorage.listFiles(backupPrefix())
            .firstOrNull { it.path.substringAfterLast('/') == expectedFileName }
            ?: return Result.failure(IllegalArgumentException("Backup یافت نشد: $backupId"))

        val content = backupFileStorage.readFile(match.path)
        val snapshot = deserializeFullProject(content).getOrElse { return Result.failure(it) }
        return restoreProjectFromSnapshot(
            snapshot, projectDao, sceneDao, shotDao, assetDao, projectDnaDao, audioContextDao
        )
    }

    // طول projectId به‌عنوان یک عدد صریح در پیشوند رمزگذاری شد (نه فقط
    // "backup_${projectId}_") تا دو projectId که یکی پیشوند رشته‌ای دیگری است
    // (مثلاً "p1" و "p1_v2") هرگز پیشوند فایل یکسان تولید نکنند — بدون این طول
    // صریح، backupPrefix پروژه‌ی "p1" ("backup_p1_") روی فایل‌های پروژه‌ی "p1_v2"
    // هم true می‌شد (چون "backup_p1_v2_....json" واقعاً با "backup_p1_" شروع
    // می‌شود) و cleanOldBackups می‌توانست بک‌آپ یک پروژه‌ی دیگر را پاک کند. جزئیات
    // در docs/adr/023-unit15-backup-manager-deviations.md.
    private fun backupPrefix(): String = "backup_${projectId.length}_${projectId}_"
    private fun backupFileName(backupId: String): String = "${backupPrefix()}$backupId.json"
}

private fun defaultBackupId(): String = UUID.randomUUID().toString().replace("-", "").take(12)
