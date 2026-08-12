package com.operaboys.cinemashotgenerator.data.repository

import android.content.Context
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ExportFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// رفع یافته‌ی معماری «دکمه‌ی Export مستعار Copy است» (G4/G18، ADR-067، رفع در
// ADR-069) — نوشتن واقعی ExportFile های composeOutput (واحد ۱۴) روی دیسک، پیش
// از اشتراک‌گذاری با Intent.ACTION_SEND. هم‌الگو دقیق با BackupFileStorage.kt
// (Interface + پیاده‌سازی واقعی مبتنی‌بر Context، برای قابل‌تست‌ماندن لایه‌ی
// data/ با Robolectric بدون نیاز به Fake).
//
// تصمیم مستقل — cacheDir/exports، نه filesDir/backups: این فایل‌ها موقتی‌اند و
// فقط برای مدت کوتاه (تا زمانی که اپ دیگر آن‌ها را از طریق FileProvider خوانده)
// لازم‌اند — دقیقاً همان کاربرد cacheDir طبق مستندات Android (بر خلاف
// BackupFileStorage.backupDir که Persistent است و کاربر می‌تواند بعداً آن را
// از صفحه‌ی Backups مرور/بازیابی کند).
interface ExportFileWriter {
    /** هر ExportFile را می‌نویسد و File نوشته‌شده را برمی‌گرداند (به همان ترتیب ورودی). */
    suspend fun writeExportFiles(files: List<ExportFile>): List<File>
}

class DeviceExportFileWriter(private val context: Context) : ExportFileWriter {

    private val exportDir: File by lazy {
        File(context.applicationContext.cacheDir, "exports").apply { mkdirs() }
    }

    override suspend fun writeExportFiles(files: List<ExportFile>): List<File> =
        withContext(Dispatchers.IO) {
            files.map { exportFile ->
                File(exportDir, exportFile.filename).apply { writeText(exportFile.content) }
            }
        }
}
