package com.operaboys.cinemashotgenerator.domain.validation

// واحد ۰۷ — بخش الف: Validation Engine
// منبع حقیقت: docs/blueprints/07-validation-and-consistency.md

/**
 * نوع سراسری Severity/ValidationIssue/ValidationReport طبق بلوپرینت ۰۷.
 *
 * NOTE: واحدهای ۰۱، ۰۲، و ۰۶ هرکدام یک ValidationResult محلی و متفاوت‌الشکل دارند
 * (docs/adr/001، 002، 003). این قدم آن‌ها را Migrate نمی‌کند — طبق ai-coding-guidelines
 * («بدون گسترش دامنه‌ی کار»، Refactor چند واحده باید جداگانه تأیید شود). سؤال Migration
 * به‌صراحت در پایین این فایل (و در گزارش نهایی) برای تصمیم معمار مطرح شده است.
 */
enum class Severity { BLOCKING, WARNING }

data class ValidationIssue(
    val severity: Severity,
    val field: String? = null,
    val message: String,
    val suggestion: String? = null
)

data class ValidationReport(
    val targetId: String,
    val blockingErrors: List<ValidationIssue>,
    val warnings: List<ValidationIssue> // Hint های قدیمی هم اینجا قرار می‌گیرند
) {
    val isValid: Boolean get() = blockingErrors.isEmpty()
}

/**
 * Level 1: کامل بودن داده.
 *
 * امضای بلوپرینت `validateDataCompleteness(shot: Shot)` بود؛ چون Shot متعلق به واحد ۰۵
 * (هنوز پیاده نشده) است، امضا به پارامترهای مستقیمِ همان فیلدهایی که بدنه‌ی تابع واقعاً
 * می‌خواند (shotDescription، characterIds، objectIds) بازنویسی شد — به‌جای ساخت یک نوع
 * Shot ناقص/Placeholder. (تصمیم مستقل، ثبت‌شده در docs/adr/004-...)
 */
fun validateDataCompleteness(
    shotDescription: String,
    characterIds: List<String>,
    objectIds: List<String>
): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    if (shotDescription.length < 10) {
        issues += ValidationIssue(Severity.BLOCKING, "shot_description", "توضیح شات الزامی است (حداقل ۱۰ کاراکتر)")
    }
    if (characterIds.isEmpty() && objectIds.isEmpty()) {
        issues += ValidationIssue(Severity.WARNING, "subjects", "شات بدون سوژه ممکن است خالی به نظر برسد")
    }
    return issues
}

/**
 * Level 2: سازگاری منطقی.
 *
 * امضای بلوپرینت `validateLogicConsistency(shot, environment: EnvironmentSettings, lighting: LightingSettings)`
 * بود؛ `shot` اصلاً در بدنه استفاده نمی‌شد و EnvironmentSettings/LightingSettings متعلق
 * به واحدهای ۰۵/۰۸ (هنوز پیاده نشده) هستند — امضا به همان سه فیلد واقعاً مصرف‌شده
 * بازنویسی شد.
 */
fun validateLogicConsistency(
    weather: String,
    lightingMotivation: String,
    locationType: String
): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    if (weather == "rain" && lightingMotivation == "fire" && locationType == "outdoor") {
        issues += ValidationIssue(
            Severity.WARNING,
            message = "آتش در باران بیرون از ساختمان غیرمنطقی است",
            suggestion = "Location را به Indoor تغییر دهید یا Lighting Motivation را عوض کنید"
        )
    }
    return issues
}

/**
 * Level 1: اعتبارسنجی بازه‌ی زمانی Beat Sheet.
 *
 * امضای بلوپرینت `validateBeatSheetTimeline(beats: List<Beat>, durationSeconds: Float)`
 * بود؛ Beat متعلق به واحد ۰۵ (هنوز پیاده نشده) است — امضا به لیست مستقیم
 * timestamp ها بازنویسی شد.
 */
fun validateBeatSheetTimeline(
    beatTimestampsSeconds: List<Float>,
    durationSeconds: Float
): List<ValidationIssue> {
    return beatTimestampsSeconds.filter { it < 0f || it > durationSeconds }
        .map {
            ValidationIssue(
                Severity.BLOCKING,
                message = "Beat timestamp $it خارج از محدوده‌ی شات (0-$durationSeconds) است"
            )
        }
}
