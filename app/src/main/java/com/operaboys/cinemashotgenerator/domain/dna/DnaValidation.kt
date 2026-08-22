package com.operaboys.cinemashotgenerator.domain.dna

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۰۲ — منطق Soft Lock و قوانین اعتبارسنجی (Rule 1 تا Rule 7)
// منبع حقیقت: docs/blueprints/02-dna-manager-v2.md (نسخه ۵)
//
// MIGRATED (docs/adr/010-cross-unit-migrations.md، Migration ۱): این فایل قبلاً یک
// sealed class ValidationResult محلی (Valid/Warning/Blocking) داشت؛ اکنون از
// ValidationIssue/Severity سراسری واحد ۰۷ استفاده می‌کند. معادل «Valid» در دنیای
// سراسری، مقدار null است.
//
// تغییرات این قدم (Migration بلوپرینت ۰۲ نسخه ۵؛ جزئیات کامل در
// docs/adr/027-unit02-dna-manager-v5-migration.md):
// - requiresApprovalForOverride حذف شد — overrideRules دیگر بخشی از ProjectDna
//   نیست (طبق تصمیم صریح معمار، هم‌راستا با data class ProjectDna بلوپرینت و
//   type-registry.md). دو تست مرتبط در DnaValidationTest.kt هم حذف می‌شوند.
// - validateShotAspectRatio اکنون AspectRatio می‌گیرد، نه String (Breaking Change
//   واقعی OutputConstraints.aspectRatio).
// - validateColorPalette (Rule ۶/۷ جدید) اضافه شد — منطق دقیقاً طبق بلوپرینت، اما
//   خروجی ValidationIssue? (نه ValidationResult سه‌حالته‌ی کد مفهومی بلوپرینت) تا با
//   الگوی موجود همین فایل (تعیین‌شده در ADR-010) هم‌راستا بماند: null=Valid،
//   ValidationIssue(WARNING)=Warning بلوپرینت، ValidationIssue(BLOCKING)=Blocking بلوپرینت.

/** Soft Lock: تغییر Core Identity همیشه مجاز است؛ فقط در صورت ناسازگاری هشدار می‌دهد. */
data class DnaUpdateResult(
    val updatedDna: ProjectDna,
    val warning: String? = null
)

/**
 * Rule 1 (Warning، هرگز Blocking): تغییر Core Identity همیشه اعمال می‌شود؛
 * اگر Shot وابسته‌ای وجود داشته باشد فقط هشدار می‌دهد.
 */
fun updateCoreIdentity(
    currentDna: ProjectDna,
    newStyle: VisualStyle,
    dependentShotsCount: Int
): DnaUpdateResult {
    val updated = currentDna.copy(
        coreIdentity = currentDna.coreIdentity.copy(dominantVisualStyle = newStyle)
    )
    val warning = if (dependentShotsCount > 0) {
        "این تغییر با $dependentShotsCount شات موجود ناسازگار است. آیا مطمئنید؟"
    } else null

    return DnaUpdateResult(updated, warning)
}

/**
 * Rule 2 (Blocking): استفاده از عنصر در forbidden_elements آن دسته.
 *
 * تعمیم آگاهانه از امضای دقیق کد مفهومی بلوپرینت
 * (که فقط validateShotAgainstDna(shotCameraAngle: String, dna) با دسته‌ی ثابت "camera" را نشان می‌دهد):
 * چون forbidden_elements سه دسته دارد (camera/lighting/weather)، پارامتر elementCategory
 * اضافه شد تا یک تابع واحد هر سه دسته را پوشش دهد، نه فقط camera.
 * (ثبت‌شده به‌صورت جداگانه در ADR-002، چون یک تعمیم آگاهانه است نه فقط جزئیات محلی)
 */
fun validateShotAgainstDna(
    shotElement: String,
    elementCategory: String,
    dna: ProjectDna
): ValidationIssue? {
    val forbidden = dna.outputConstraints.forbiddenElements[elementCategory] ?: emptyList()
    if (shotElement in forbidden) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "این عنصر ('$shotElement') در DNA این پروژه ممنوع است"
        )
    }
    return null
}

/** Rule 4 (Blocking): نقض technical_constraints — تجاوز از max_shot_duration. */
fun validateShotDuration(durationSeconds: Int, dna: ProjectDna): ValidationIssue? {
    val max = dna.outputConstraints.maxShotDurationSeconds
    if (durationSeconds > max) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "مدت این شات ($durationSeconds ثانیه) از حداکثر مجاز ($max ثانیه) در DNA این پروژه بیشتر است"
        )
    }
    return null
}

/**
 * Rule ۶/۷ (جدید، بلوپرینت ۰۲ نسخه ۵): اعتبارسنجی colorPalette.
 * خالی = Valid (اختیاری است)؛ بیش از ۵ مقدار = Warning (فقط ۵ مورد اول استفاده
 * می‌شود، بدون بررسی Hex بودن باقی مقادیر — دقیقاً طبق ترتیب کد مفهومی بلوپرینت)؛
 * هر مقدار غیر-Hex معتبر (طبق ^#[0-9A-Fa-f]{6}$) = Blocking.
 */
fun validateColorPalette(palette: List<String>): ValidationIssue? {
    if (palette.isEmpty()) return null
    if (palette.size > 5) {
        return ValidationIssue(
            Severity.WARNING,
            message = "بیش از ۵ رنگ توصیه نمی‌شود؛ فقط ۵ مورد اول استفاده می‌شود"
        )
    }
    val hexPattern = Regex("^#[0-9A-Fa-f]{6}$")
    val invalid = palette.filterNot { hexPattern.matches(it) }
    if (invalid.isNotEmpty()) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "مقادیر رنگ نامعتبر: ${invalid.joinToString()}"
        )
    }
    return null
}
