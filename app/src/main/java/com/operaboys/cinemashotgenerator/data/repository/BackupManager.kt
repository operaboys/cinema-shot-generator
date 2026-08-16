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
// MIGRATED (واحد ۱۶ فاز ۶ — قدم ۲، docs/adr/059-unit16-phase6-step2-backups-final-review.md):
// پسوند فایل از `.json` به `.csgb` تغییر کرد (هماهنگی واقعی با
// docs/design/README.md بخش «۱۲. Backups» — `project-slug-YYYY-MM-DD.csgb`)؛
// محتوای فایل همچنان متن JSON خام است (فقط پسوند عوض شد، نه فرمت). تمایز
// auto/manual (`BackupKind`) هم به همین مناسبت اضافه شد — قبلاً هیچ‌جای این
// کلاس این تمایز را نداشت. `backupIntervalMinutes` اکنون واقعاً توسط یک Timer
// دوره‌ای در StudioShell مصرف می‌شود (هم‌الگو با AutoSaveManager.intervalSeconds
// در قدم قبل)، برای backup های `AUTO`.
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
    suspend fun createBackup(kind: BackupKind = BackupKind.MANUAL): Result<String> {
        val projectData = serializeFullProject(
            projectId, projectDao, sceneDao, shotDao, assetDao, projectDnaDao, audioContextDao
        ).getOrElse { return Result.failure(it) }

        val backupId = idProvider()
        val backupPath = backupFileStorage.writeFile(backupFileName(kind, backupId), projectData)
        cleanOldBackups()
        return Result.success(backupPath)
    }

    /** فهرست UI-پسند برای صفحه‌ی Backups — نام/حجم/نوع/سن، هر کدام از خودِ فایل واقعی استخراج‌شده. */
    suspend fun listBackups(): Result<List<BackupSummary>> = runCatching {
        backupFileStorage.listFiles(backupPrefix())
            .mapNotNull { info -> parseBackupFileName(info.path.substringAfterLast('/'))?.let { (kind, id) ->
                BackupSummary(backupId = id, kind = kind, createdAt = info.createdAt, sizeBytes = info.sizeBytes, path = info.path)
            } }
            .sortedByDescending { it.createdAt }
    }

    suspend fun deleteBackup(backupId: String): Result<Unit> = runCatching {
        val match = findBackupFile(backupId) ?: return@runCatching Unit
        backupFileStorage.deleteFile(match.path)
    }

    // رفع یافته‌ی #۱۲ appendix (ADR-089): محتوای واقعی هر بکاپ این پروژه (نام فایل +
    // محتوا) برای Export All — هم‌الگو دقیق با ورودی موردنیاز
    // ExportFileWriter.writeExportFiles (ADR-069). نام فایل مستقیماً از خودِ مسیر
    // واقعی هر بکاپ گرفته می‌شود (نه بازسازی از backupFileName)، تا همیشه دقیقاً
    // با فایل روی دیسک یکی باشد.
    suspend fun readAllBackupsForExport(): Result<List<Pair<String, String>>> = runCatching {
        listBackups().getOrThrow().map { summary ->
            summary.path.substringAfterLast('/') to backupFileStorage.readFile(summary.path)
        }
    }

    // رفع یافته‌ی #۱۲ appendix (ADR-089): محتوای یک فایل بکاپ خارجی را فقط به
    // فهرست بکاپ‌های همین پروژه «معرفی» می‌کند — Restore فوری نیست؛ کاربر بعداً از
    // همان دکمه‌ی Restore موجود روی این ردیف تازه استفاده می‌کند (دقیقاً مثل هر
    // بکاپ دیگر). اعتبارسنجی عمداً هم‌سطح چیزی است که restoreFromBackup از قبل
    // اعتماد می‌کند (فقط Deserialize موفق FullProjectSnapshot، بدون بررسی
    // یکپارچگی ارجاعی سخت‌گیرانه‌ی ExportImportRepository.importProject) — یک
    // بکاپ همین اپ از قبل در همین سطح مورد اعتماد است؛ فقط یک بررسی تازه اضافه شد
    // که آنجا لازم نبود: projectId داخل فایل باید با پروژه‌ی جاری یکی باشد، وگرنه
    // این بکاپ بی‌صدا زیر پیشوند نام‌فایل این پروژه ثبت می‌شد اما وقتی روزی
    // Restore می‌شد، داده‌ی یک پروژه‌ی کاملاً دیگر را می‌نوشت.
    suspend fun importBackup(content: String): Result<String> {
        val snapshot = deserializeFullProject(content).getOrElse {
            return Result.failure(IllegalArgumentException("فایل انتخاب‌شده یک بکاپ معتبر نیست"))
        }
        if (snapshot.project.projectId != projectId) {
            return Result.failure(IllegalArgumentException("این فایل بکاپ متعلق به پروژه‌ی دیگری است"))
        }
        val backupId = idProvider()
        val path = backupFileStorage.writeFile(backupFileName(BackupKind.MANUAL, backupId), content)
        cleanOldBackups()
        return Result.success(path)
    }

    /** فقط جدیدترین maxBackupsToKeep بک‌آپ نگه داشته می‌شود؛ بقیه حذف می‌شوند. */
    private suspend fun cleanOldBackups() {
        val backups = backupFileStorage.listFiles(backupPrefix()).sortedByDescending { it.createdAt }
        backups.drop(maxBackupsToKeep).forEach { backupFileStorage.deleteFile(it.path) }
    }

    suspend fun restoreFromBackup(backupId: String): Result<Unit> {
        val match = findBackupFile(backupId)
            ?: return Result.failure(IllegalArgumentException("Backup یافت نشد: $backupId"))

        val content = backupFileStorage.readFile(match.path)
        val snapshot = deserializeFullProject(content).getOrElse { return Result.failure(it) }
        return restoreProjectFromSnapshot(
            snapshot, projectDao, sceneDao, shotDao, assetDao, projectDnaDao, audioContextDao
        )
    }

    private suspend fun findBackupFile(backupId: String): BackupFileInfo? =
        backupFileStorage.listFiles(backupPrefix())
            .firstOrNull { parseBackupFileName(it.path.substringAfterLast('/'))?.second == backupId }

    // طول projectId به‌عنوان یک عدد صریح در پیشوند رمزگذاری شد (نه فقط
    // "backup_${projectId}_") تا دو projectId که یکی پیشوند رشته‌ای دیگری است
    // (مثلاً "p1" و "p1_v2") هرگز پیشوند فایل یکسان تولید نکنند — بدون این طول
    // صریح، backupPrefix پروژه‌ی "p1" ("backup_p1_") روی فایل‌های پروژه‌ی "p1_v2"
    // هم true می‌شد (چون "backup_p1_v2_....csgb" واقعاً با "backup_p1_" شروع
    // می‌شود) و cleanOldBackups می‌توانست بک‌آپ یک پروژه‌ی دیگر را پاک کند. جزئیات
    // در docs/adr/023-unit15-backup-manager-deviations.md.
    private fun backupPrefix(): String = "backup_${projectId.length}_${projectId}_"

    // ترتیب segment ها عمداً kind را بلافاصله بعد از Prefix امن (بالا) می‌گذارد،
    // نه قبل از آن — تا backupPrefix() (مصرف‌شده برای فیلتر cleanOldBackups/
    // listFiles) دست‌نخورده بماند. backupId خودش (مثلاً idProvider های تست) ممکن
    // است حاوی "_" باشد، پس Parse فقط روی اولین "_" بعد از Prefix تکیه می‌کند —
    // کافی چون مقادیر BackupKind خودشان هرگز "_" ندارند.
    private fun backupFileName(kind: BackupKind, backupId: String): String =
        "${backupPrefix()}${kind.name.lowercase()}_$backupId.csgb"

    private fun parseBackupFileName(fileName: String): Pair<BackupKind, String>? {
        val prefix = backupPrefix()
        if (!fileName.startsWith(prefix) || !fileName.endsWith(".csgb")) return null
        val remainder = fileName.removePrefix(prefix).removeSuffix(".csgb")
        val kindRaw = remainder.substringBefore('_', missingDelimiterValue = "")
        val backupId = remainder.substringAfter('_', missingDelimiterValue = "")
        if (kindRaw.isEmpty() || backupId.isEmpty()) return null
        val kind = BackupKind.entries.firstOrNull { it.name.lowercase() == kindRaw } ?: return null
        return kind to backupId
    }
}

/** نوع Backup — طبق docs/design/README.md بخش «۱۲. Backups» («kind (auto/manual)»). */
enum class BackupKind { AUTO, MANUAL }

/** یک ردیف صفحه‌ی Backups — همه‌ی فیلدهای لازم UI (نام/حجم/نوع/سن) از قبل استخراج‌شده. */
data class BackupSummary(val backupId: String, val kind: BackupKind, val createdAt: String, val sizeBytes: Long, val path: String)

private fun defaultBackupId(): String = UUID.randomUUID().toString().replace("-", "").take(12)
