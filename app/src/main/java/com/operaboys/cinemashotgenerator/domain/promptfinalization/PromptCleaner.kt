package com.operaboys.cinemashotgenerator.domain.promptfinalization

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۱۳ — Prompt Finalization Pipeline (بخش الف: Prompt Cleaner)
// منبع حقیقت: docs/blueprints/13-prompt-finalization.md
//
// NOTE (محدودیت صریح بلوپرینت، تغییرناپذیر در این قدم): این منطق (SYNONYM_GROUPS،
// WEATHER_CONFLICTS، Stop Words، Final Polish) فقط برای متن انگلیسی طراحی شده است —
// دقیقاً هم‌راستا با ADR-015 که render() واحد ۱۴ فعلاً فقط متن انگلیسی تولید می‌کند
// (RenderedOutput.language = "en" همیشه). معادل فارسی این قوانین پیاده نشد.

data class CleaningReport(
    val conflictsDetected: Int,
    val conflictsResolved: List<String>,
    val redundancyRemoved: Int,
    val stopWordsRemoved: Int,
    val originalLength: Int,
    val cleanedLength: Int,
    val compressionRatio: Float
)

data class CleaningOptions(
    val detectConflicts: Boolean = true,
    val removeRedundancy: Boolean = true,
    val filterStopWords: Boolean = true,
    val optimizeTokens: Boolean = true,
    val aggressiveMode: Boolean = false,
    // واحد ۱۴/۱۳ (ADR-026): وقتی cleanPrompt روی یک مقدار تکی از فیلد JSON اجرا
    // می‌شود (نه یک جمله‌ی کامل رندرشده)، فاز ۵ (بزرگ‌کردن حرف اول + الزام نقطه‌ی
    // پایانی) بی‌معناست — مثلاً فیلد camera="eye level, medium shot" نباید به
    // "Eye level, medium shot." تبدیل شود. پیش‌فرض true تمام رفتار/تست‌های موجود را
    // دست‌نخورده نگه می‌دارد (هیچ فراخوانی موجودی این فیلد را صریح نمی‌دهد).
    val applyFinalPolish: Boolean = true
)

val SYNONYM_GROUPS = listOf(
    listOf("beautiful", "pretty", "gorgeous", "stunning"),
    listOf("big", "large", "huge", "massive"),
    listOf("fast", "quick", "rapid", "swift")
)

val WEATHER_CONFLICTS = listOf("sunny" to "rainy", "clear" to "stormy")
val LOGICAL_CONTRADICTIONS = listOf(
    "standing" to "sitting", "open" to "closed", "moving" to "static", "alive" to "dead"
)

/**
 * Phase 3 — Stop Words Filtering. لیست دقیقاً همان کلمات نمونه‌ی خودِ بلوپرینت
 * («very very, a, that») + چند کلمهٔ پرکنندهٔ رایج مشابه (just, really, quite).
 * این کلمات برای پرامپت مدل‌های ویدیوساز اطلاعات معنایی اضافه نمی‌کنند.
 */
val STOP_WORDS = listOf("a", "the", "very", "that", "just", "really", "quite")

/** تشخیص تضادهای منطقی/آب‌وهوایی در متن رندرشده. */
fun detectConflicts(text: String): List<Pair<String, String>> {
    val found = mutableListOf<Pair<String, String>>()
    for ((a, b) in WEATHER_CONFLICTS + LOGICAL_CONTRADICTIONS) {
        if (text.contains(a, ignoreCase = true) && text.contains(b, ignoreCase = true)) {
            found += a to b
        }
    }
    return found
}

/** حذف مترادف‌های تکراری، فقط اولین رخداد هر گروه را نگه می‌دارد. */
fun removeSynonymDuplicates(text: String): String {
    var result = text
    for (group in SYNONYM_GROUPS) {
        val present = group.filter { result.contains(it, ignoreCase = true) }
        if (present.size > 1) {
            present.drop(1).forEach { word ->
                result = result.replace(Regex("\\b$word\\b", RegexOption.IGNORE_CASE), "")
            }
        }
    }
    return result.replace(Regex("\\s+"), " ").trim()
}

private fun countStopWordOccurrences(text: String): Int =
    STOP_WORDS.sumOf { word -> Regex("\\b$word\\b", RegexOption.IGNORE_CASE).findAll(text).count() }

/** Phase 3 — حذف واقعی کلمات پرکننده (نه صرفاً شمارش). */
fun filterStopWords(text: String): String {
    var result = text
    for (word in STOP_WORDS) {
        result = result.replace(Regex("\\b$word\\b", RegexOption.IGNORE_CASE), "")
    }
    return result.replace(Regex("\\s+"), " ").trim()
}

/** Phase 4 — فشرده‌سازی ساده: فقط حذف فاصله‌های اضافه، بدون تغییر معنایی جمله. */
fun optimizeTokens(text: String): String = text.replace(Regex("\\s+"), " ").trim()

/** Phase 5 — Grammar/حروف بزرگ ابتدای جمله/نقطه‌ی پایانی. */
fun applyFinalPolish(text: String): String {
    if (text.isBlank()) return text
    val capitalized = text.replaceFirstChar { it.uppercase() }
    val trimmed = capitalized.trimEnd()
    return if (trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?")) trimmed else "$trimmed."
}

/**
 * تابع اصلی پایپ‌لاین تمیزکاری. ترتیب فازها ثابت است: Conflict Detection → Redundancy
 * Removal → Stop Words Filtering → Token Optimization → Final Polish.
 *
 * برخلاف کد مفهومی بلوپرینت (که Phase 3-5 را کامنت جای‌گذار گذاشته بود)، این سه فاز
 * واقعاً پیاده شدند — جزئیات الگوریتم هرکدام در ADR-016.
 */
fun cleanPrompt(renderedText: String, options: CleaningOptions = CleaningOptions()): Pair<String, CleaningReport> {
    var cleaned = renderedText

    val conflicts = if (options.detectConflicts) detectConflicts(cleaned) else emptyList()
    conflicts.forEach { (_, loser) -> cleaned = cleaned.replace(Regex("\\b$loser\\b", RegexOption.IGNORE_CASE), "") }

    val beforeRedundancy = cleaned
    if (options.removeRedundancy) cleaned = removeSynonymDuplicates(cleaned)
    val redundancyRemoved = if (beforeRedundancy != cleaned) 1 else 0

    val stopWordsRemoved = if (options.filterStopWords) countStopWordOccurrences(cleaned) else 0
    if (options.filterStopWords) cleaned = filterStopWords(cleaned)

    if (options.optimizeTokens) cleaned = optimizeTokens(cleaned)

    if (options.applyFinalPolish) cleaned = applyFinalPolish(cleaned)

    val report = CleaningReport(
        conflictsDetected = conflicts.size,
        conflictsResolved = conflicts.map { "${it.first} vs ${it.second}" },
        redundancyRemoved = redundancyRemoved,
        stopWordsRemoved = stopWordsRemoved,
        originalLength = renderedText.length,
        cleanedLength = cleaned.length,
        compressionRatio = (renderedText.length - cleaned.length).toFloat() / renderedText.length
    )
    return cleaned to report
}

/**
 * Rule «تضاد آب‌وهوا/منطقی تشخیص داده شود → باید حل شود». طبق بلوپرینت این یک تضمین
 * پردازشی است، نه Validation کاربر — cleanPrompt خودش تضادها را حل می‌کند. این تابع
 * روی متنِ نهایی (بعد از cleanPrompt) دوباره detectConflicts را اجرا می‌کند تا تأیید
 * کند واقعاً چیزی باقی نمانده — نه فقط این‌که CleaningReport همین را «ادعا» کند. اگر
 * options.detectConflicts=false بوده باشد، این تابع همچنان تضاد باقی‌مانده را می‌بیند.
 */
fun validateConflictsResolved(cleanedText: String): ValidationIssue? {
    val remaining = detectConflicts(cleanedText)
    if (remaining.isEmpty()) return null
    return ValidationIssue(
        severity = Severity.BLOCKING,
        message = "تضاد حل‌نشده در متن باقی مانده: ${remaining.joinToString(", ") { "${it.first} vs ${it.second}" }}"
    )
}

/** Rule «کاهش طول کمتر از حداقل مورد انتظار». آستانه‌ی ۳۰٪ دقیقاً طبق مثال بلوپرینت. */
fun validateCompressionRatio(report: CleaningReport, minimumExpectedRatio: Float = 0.3f): ValidationIssue? {
    if (report.compressionRatio >= minimumExpectedRatio) return null
    return ValidationIssue(
        severity = Severity.WARNING,
        message = "کاهش طول (${(report.compressionRatio * 100).toInt()}%) کمتر از حداقل مورد انتظار " +
            "(${(minimumExpectedRatio * 100).toInt()}%) است؛ ممکن است تمیزکاری ناقص باشد"
    )
}
