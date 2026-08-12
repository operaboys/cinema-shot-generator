package com.operaboys.cinemashotgenerator.ui.outputdelivery

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

// ADR-069 — ساخت واقعی Intent.ACTION_SEND (اشتراک‌گذاری فایل، نه فقط Copy به
// Clipboard). این تابع عمداً از هر Context.startActivity/Intent.createChooser
// جدا شد (که فقط از یک Activity واقعی معنا دارد و در Robolectric Unit Test
// دشوار/شکننده است) — طبق دستور کار («بدون نیاز به تست واقعی Intent System»).
// این تابع خودش (ساخت Intent از یک لیست File) کاملاً Deterministic و در
// Robolectric قابل‌تست است (FileProvider.getUriForFile یک ContentProvider واقعی
// را resolve می‌کند که از AndroidManifest.xml این پروژه می‌آید).

const val EXPORT_FILE_PROVIDER_AUTHORITY = "com.operaboys.cinemashotgenerator.fileprovider"

/**
 * از روی فایل‌های واقعاً نوشته‌شده (ExportFileWriter) یک Intent اشتراک‌گذاری واقعی
 * می‌سازد — ACTION_SEND برای یک فایل، ACTION_SEND_MULTIPLE برای بیش از یکی (طبق
 * مستندات رسمی Android). فراخوان (Composable) این Intent را با
 * `Intent.createChooser` باز می‌کند.
 */
fun buildExportShareIntent(context: Context, files: List<File>, mimeType: String): Intent {
    require(files.isNotEmpty()) { "حداقل یک فایل برای Export لازم است" }

    val uris = files.map { file ->
        FileProvider.getUriForFile(context, EXPORT_FILE_PROVIDER_AUTHORITY, file)
    }

    return if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
