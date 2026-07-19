package com.operaboys.cinemashotgenerator.domain.story

// واحد ۰۱ — قوانین اعتبارسنجی Story Wizard (Rule 1 تا Rule 4)
// منبع حقیقت: docs/blueprints/01-story-and-override.md

/** یک مورد اعتبارسنجی — Blocking (error) یا Warning. */
data class ValidationIssue(
    val field: String,
    val reason: String,
    val severity: RuleSeverity
)

/**
 * NOTE: این نوع فعلاً محلیِ واحد ۰۱ است؛ نسخه‌ی سراسری در واحد ۰۷
 * (Validation & Consistency) تعریف خواهد شد و این نوع به آن مهاجرت می‌کند.
 * (ثبت‌شده در docs/adr/001-unit01-story-override-deviations.md)
 */
data class ValidationResult(
    val valid: Boolean,
    val errors: List<ValidationIssue>,
    val warnings: List<ValidationIssue> = emptyList()
)

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
 */
fun validateStoryContext(context: StoryContext): ValidationResult {
    val errors = mutableListOf<ValidationIssue>()
    val warnings = mutableListOf<ValidationIssue>()

    // Rule 1 (Blocking)
    if (context.genre.isEmpty()) {
        errors.add(
            ValidationIssue(
                field = "genre",
                reason = "ژانر انتخاب نشده است؛ انتخاب حداقل یک ژانر الزامی است",
                severity = RuleSeverity.BLOCKING
            )
        )
    }

    // Rule 2 (Blocking)
    val documentaryAllowed = setOf(Genre.DOCUMENTARY, Genre.DRAMA)
    if (Genre.DOCUMENTARY in context.genre && !documentaryAllowed.containsAll(context.genre)) {
        errors.add(
            ValidationIssue(
                field = "genre",
                reason = "پروژه‌ی Documentary فقط می‌تواند با ژانرهای Documentary/Drama ترکیب شود",
                severity = RuleSeverity.BLOCKING
            )
        )
    }

    // Rule 3 (Warning)
    context.genre.forEach { genre ->
        checkMoodGenreCompatibility(genre, context.moodPrimary)?.let { message ->
            warnings.add(
                ValidationIssue(
                    field = "mood",
                    reason = message,
                    severity = RuleSeverity.WARNING
                )
            )
        }
    }

    return ValidationResult(
        valid = errors.isEmpty(),
        errors = errors,
        warnings = warnings
    )
}

/**
 * Rule 4: اگر همه‌ی فیلدهای الزامی پر شده باشند COMPLETE، در غیر این صورت PARTIAL.
 * سایر فیلدهای الزامی در خودِ نوع non-null تضمین شده‌اند؛ فقط ژانر قابل‌خالی‌بودن است.
 */
fun deriveCompletionStatus(context: StoryContext): CompletionStatus {
    return if (context.genre.isNotEmpty()) CompletionStatus.COMPLETE else CompletionStatus.PARTIAL
}
