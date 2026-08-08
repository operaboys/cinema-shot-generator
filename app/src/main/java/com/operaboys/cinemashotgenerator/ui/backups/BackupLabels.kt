package com.operaboys.cinemashotgenerator.ui.backups

import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

// واحد ۱۶ فاز ۶ — قدم ۲: توابع نگاشت UI برای صفحه‌ی Backups — طبق
// docs/design/README.md بخش «۱۲. Backups». نام فایل ذخیره‌شده‌ی واقعی (طبق ADR-023،
// عمداً یک الگوی امن-در-برابر-تصادم و غیرقابل‌خواندن است، نه slug+date) با نام
// «نمایشی» این صفحه فرق دارد — displayFileName این‌جا همان الگوی مستند سند طراحی
// (`project-slug-YYYY-MM-DD.csgb`) را فقط برای نمایش می‌سازد؛ جزئیات کامل تصمیم در
// docs/adr/059-unit16-phase6-step2-backups-final-review.md.

fun slugify(text: String): String =
    text.trim().lowercase()
        .replace(Regex("[^a-z0-9\\u0600-\\u06FF]+"), "-")
        .trim('-')
        .ifEmpty { "project" }

fun displayBackupFileName(projectName: String, createdAtIso: String): String {
    val datePart = runCatching { Instant.parse(createdAtIso).toString().substring(0, 10) }
        .getOrDefault(createdAtIso.take(10))
    return "${slugify(projectName)}-$datePart.csgb"
}

fun formatBackupSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

fun formatBackupAge(createdAtIso: String, language: Language, now: Instant = Instant.now()): String {
    val created = try {
        Instant.parse(createdAtIso)
    } catch (e: DateTimeParseException) {
        return createdAtIso
    }
    val minutes = Duration.between(created, now).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> uiString("backups.ageJustNow", language)
        minutes < 60 -> uiTemplate("backups.ageMinutesTemplate", language, "minutes" to minutes.toString())
        minutes < 60 * 24 -> uiTemplate("backups.ageHoursTemplate", language, "hours" to (minutes / 60).toString())
        else -> uiTemplate("backups.ageDaysTemplate", language, "days" to (minutes / (60 * 24)).toString())
    }
}
