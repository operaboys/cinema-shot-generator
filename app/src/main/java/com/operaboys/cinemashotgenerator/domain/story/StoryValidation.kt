package com.operaboys.cinemashotgenerator.domain.story

import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۰۱ — قوانین اعتبارسنجی Story Wizard (Rule 1 تا Rule 4)
// منبع حقیقت: docs/blueprints/01-story-and-override.md
//
// MIGRATED (docs/adr/010-cross-unit-migrations.md، Migration ۱): این فایل قبلاً
// یک ValidationIssue/ValidationResult محلی داشت (ثبت‌شده در ADR-001)؛ اکنون از
// ValidationIssue/Severity سراسری واحد ۰۷ استفاده می‌کند. RuleSeverity (تعریف‌شده
// در HumanOverride.kt) دست‌نخورده ماند — آن enum برای مدل مجوز Override است، نه
// برای گزارش نتیجه‌ی اعتبارسنجی، و خارج از Scope این Migration بود.

/**
 * بررسی ترکیبات غیرمعمول Genre+Mood — نتیجه فقط Warning است، هرگز Blocking.
 * خروجی null یعنی ترکیب عادی است.
 */
fun checkMoodGenreCompatibility(genre: Genre, mood: Mood): String? {
    return when {
        genre == Genre.HORROR && mood == Mood.HOPEFUL ->
            "ترکیب Horror + Hopeful غیرمعمول است — آیا مطمئن هستید؟"
        genre == Genre.ROMANCE && mood == Mood.DARK ->
            "ترکیب Romance + Dark غیرمعمول است (مگر Dark Romance مدنظر باشد)"
        else -> null
    }
}

/**
 * اعتبارسنجی StoryContext طبق قوانین بلوپرینت:
 * - Rule 1 (Blocking): فیلد الزامی خالی — تنها حالت قابل‌وقوع در این نوع، لیست ژانر تهی است.
 * - Rule 2 (Blocking): اگر genre شامل DOCUMENTARY باشد، کل لیست باید زیرمجموعه‌ی
 *   {DOCUMENTARY, DRAMA} باشد. (تفسیر تأییدشده — StoryType مقدار Documentary ندارد؛ ADR-001)
 * - Rule 3 (Warning، غیرمسدودکننده): ترکیب غیرمعمول Genre + Mood اصلی.
 * - Rule 4 در validate بررسی نمی‌شود؛ مقدار درست از [deriveCompletionStatus] محاسبه می‌شود.
 *
 * لیست خالی معادل «Valid» است (طبق همان قرارداد validateDataCompleteness واحد ۰۷ که
 * Blocking و Warning را در یک لیست ترکیب می‌کند، نه ValidationReport — هیچ‌کدام از
 * توابع Rule-level موجود در پروژه از ValidationReport استفاده نمی‌کنند چون به
 * targetId نیاز دارد که StoryContext فاقد آن است؛ جزئیات در ADR-010).
 */
fun validateStoryContext(context: StoryContext): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()

    // Rule 1 (Blocking)
    if (context.genre.isEmpty()) {
        issues.add(
            ValidationIssue(
                severity = Severity.BLOCKING,
                field = "genre",
                message = "ژانر انتخاب نشده است؛ انتخاب حداقل یک ژانر الزامی است"
            )
        )
    }

    // Rule 2 (Blocking)
    val documentaryAllowed = setOf(Genre.DOCUMENTARY, Genre.DRAMA)
    if (Genre.DOCUMENTARY in context.genre && !documentaryAllowed.containsAll(context.genre)) {
        issues.add(
            ValidationIssue(
                severity = Severity.BLOCKING,
                field = "genre",
                message = "پروژه‌ی Documentary فقط می‌تواند با ژانرهای Documentary/Drama ترکیب شود"
            )
        )
    }

    // Rule 3 (Warning)
    context.genre.forEach { genre ->
        checkMoodGenreCompatibility(genre, context.moodPrimary)?.let { message ->
            issues.add(
                ValidationIssue(
                    severity = Severity.WARNING,
                    field = "mood",
                    message = message
                )
            )
        }
    }

    return issues
}

/**
 * Rule 4: اگر همه‌ی فیلدهای الزامی پر شده باشند COMPLETE، در غیر این صورت PARTIAL.
 * سایر فیلدهای الزامی در خودِ نوع non-null تضمین شده‌اند؛ فقط ژانر قابل‌خالی‌بودن است.
 */
fun deriveCompletionStatus(context: StoryContext): CompletionStatus {
    return if (context.genre.isNotEmpty()) CompletionStatus.COMPLETE else CompletionStatus.PARTIAL
}
