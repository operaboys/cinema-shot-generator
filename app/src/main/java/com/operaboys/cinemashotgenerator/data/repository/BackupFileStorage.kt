package com.operaboys.cinemashotgenerator.data.repository

import android.content.Context
import java.io.File
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// واحد ۱۵ — تکمیل BackupManager (طبق ADR-017/022، دومین و آخرین کلاسی که فقط امضا
// داشت). کد مفهومی بلوپرینت ۱۵ از writeFileToDevice/readFileFromDevice/
// listBackupsForProject/deleteBackupFile استفاده می‌کند — این‌ها به Android
// Context/File API واقعی وابسته‌اند، نه Room. برای این‌که لایه‌ی data/ با Robolectric
// قابل‌تست بماند (بدون نیاز به دستگاه واقعی)، این عملیات فایل پشت این Interface
// انتزاع شدند — DeviceBackupFileStorage (پایین همین فایل) پیاده‌سازی واقعی است؛
// پیاده‌سازی Fake/in-memory برای تست در BackupManagerTest.kt قرار دارد (تست-محور،
// نه بخشی از کد اصلی). جزئیات کامل در docs/adr/023-unit15-backup-manager-deviations.md.

/**
 * یک فایل Backup روی دیسک: مسیر کامل + زمان ایجاد (برای مرتب‌سازی/حذف
 * قدیمی‌ترها) + حجم (برای صفحه‌ی Backups، واحد ۱۶ فاز ۶ قدم ۲ — طبق
 * docs/design/README.md بخش «۱۲. Backups»).
 */
data class BackupFileInfo(val path: String, val createdAt: String, val sizeBytes: Long)

interface BackupFileStorage {
    /** فایل را می‌نویسد و مسیر کامل نوشته‌شده را برمی‌گرداند. */
    suspend fun writeFile(fileName: String, content: String): String

    suspend fun readFile(path: String): String

    /** تمام فایل‌هایی که نامشان با prefix شروع می‌شود، مرتب‌نشده. */
    suspend fun listFiles(prefix: String): List<BackupFileInfo>

    suspend fun deleteFile(path: String)
}

/** پیاده‌سازی واقعی — با android.content.Context.filesDir، طبق قرارداد AppDatabase.kt. */
class DeviceBackupFileStorage(private val context: Context) : BackupFileStorage {

    private val backupDir: File by lazy {
        File(context.applicationContext.filesDir, "backups").apply { mkdirs() }
    }

    override suspend fun writeFile(fileName: String, content: String): String =
        withContext(Dispatchers.IO) {
            val file = File(backupDir, fileName)
            file.writeText(content)
            file.absolutePath
        }

    override suspend fun readFile(path: String): String =
        withContext(Dispatchers.IO) { File(path).readText() }

    override suspend fun listFiles(prefix: String): List<BackupFileInfo> =
        withContext(Dispatchers.IO) {
            backupDir.listFiles { file -> file.isFile && file.name.startsWith(prefix) }
                ?.map { file ->
                    BackupFileInfo(
                        path = file.absolutePath,
                        createdAt = Instant.ofEpochMilli(file.lastModified()).toString(),
                        sizeBytes = file.length()
                    )
                }
                ?: emptyList()
        }

    override suspend fun deleteFile(path: String) {
        withContext(Dispatchers.IO) { File(path).delete() }
        Unit
    }
}
