package com.operaboys.cinemashotgenerator.domain.validation

import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingMotivation
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType

// واحد ۰۷ — بخش الف: Validation Engine
// منبع حقیقت: docs/blueprints/07-validation-and-consistency-v2.md

/**
 * نوع سراسری Severity/ValidationIssue/ValidationReport طبق بلوپرینت ۰۷.
 *
 * واحدهای ۰۱، ۰۲، و ۰۶ قبلاً هرکدام یک ValidationResult محلی و متفاوت‌الشکل داشتند
 * (docs/adr/001، 002، 003)؛ طبق تصمیم معمار، این سه واحد اکنون به این نوع سراسری
 * Migrate شده‌اند (Migration ۱، docs/adr/010-cross-unit-migrations.md). استثنا:
 * UpdateResult در domain.asset (Hard Lock واحد ۰۶) عمداً migrate نشد، چون هرگز نباید
 * قابل‌بیان‌شدن به‌صورت Warning باشد — جزئیات در همان ADR.
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
 * است، امضا به پارامترهای مستقیمِ همان فیلدهایی که بدنه‌ی تابع واقعاً می‌خواند
 * (shotDescription، characterIds، objectIds، locationIds) بازنویسی شد — به‌جای ساخت
 * یک نوع Shot Placeholder. (تصمیم مستقل، ثبت‌شده در docs/adr/004-...)
 *
 * MIGRATED (Migration ۴، docs/adr/010-cross-unit-migrations.md): Rule 2 قبلاً فقط
 * characterIds/objectIds را با Severity=WARNING بررسی می‌کرد و locationIds را کلاً
 * نادیده می‌گرفت — که با Rule 2 بلوپرینت ۰۵ (Blocking، هر سه دسته) در تضاد بود. اکنون
 * locationIds هم بررسی می‌شود و Severity به BLOCKING تغییر کرده تا دقیقاً با
 * validateShotHasSubject واحد ۰۵ یکسان باشد.
 */
fun validateDataCompleteness(
    shotDescription: String,
    characterIds: List<String>,
    objectIds: List<String>,
    locationIds: List<String>
): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    if (shotDescription.length < 10) {
        issues += ValidationIssue(Severity.BLOCKING, "shot_description", "توضیح شات الزامی است (حداقل ۱۰ کاراکتر)")
    }
    if (characterIds.isEmpty() && objectIds.isEmpty() && locationIds.isEmpty()) {
        issues += ValidationIssue(Severity.BLOCKING, "subjects", "هر شات باید حداقل به یک Character، Object یا Location متصل باشد")
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
 *
 * تاریخچه (MIGRATED، Migration ۳، docs/adr/073-...): weather/lightingMotivation قبلاً
 * String خام بودند چون واحدهای ۰۵/۰۸ در زمان نوشتن این تابع هنوز پیاده نشده بودند —
 * تنها راه فراخوانی، تبدیل دستی enum های واقعی به رشته (`.name.lowercase()`) در سمت
 * فراخوان (`checkFireInRainOutdoors`) بود، دقیقاً همان Type-Unsafety که این Migration
 * حذف کرد. حالا از WeatherType و LightingMotivation واقعی (واحد ۰۸) استفاده می‌شود.
 * locationType طبق تصمیم اصلی همچنان پارامتری مستقل باقی ماند (متعلق به Scene، واحد
 * ۰۴، نه واحد ۰۷) — اما چون Scene.location.type از قبل LocationType واقعی است (نه
 * String)، این پارامتر هم به همان enum واقعی تبدیل شد؛ این بخش فراتر از دستور صریح
 * بلوپرینت است، تصمیمی مستقل برای رفع همان الگوی Type-Unsafety در سمت فراخوان.
 */
fun validateLogicConsistency(
    weatherType: WeatherType,
    lightingMotivation: LightingMotivation,
    locationType: LocationType
): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    if (weatherType == WeatherType.RAIN && lightingMotivation == LightingMotivation.FIRE && locationType == LocationType.OUTDOOR) {
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
