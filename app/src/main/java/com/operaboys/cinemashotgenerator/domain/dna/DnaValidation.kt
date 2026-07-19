package com.operaboys.cinemashotgenerator.domain.dna

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۰۲ — منطق Soft Lock و قوانین اعتبارسنجی (Rule 1 تا Rule 5)
// منبع حقیقت: docs/blueprints/02-dna-manager.md
//
// MIGRATED (docs/adr/010-cross-unit-migrations.md، Migration ۱): این فایل قبلاً
// یک sealed class ValidationResult محلی (Valid/Warning/Blocking) داشت (ثبت‌شده در
// ADR-002)؛ اکنون از ValidationIssue/Severity سراسری واحد ۰۷ استفاده می‌کند.
// معادل «Valid» در دنیای سراسری، مقدار null است (چون هر تابع این فایل حداکثر یک
// نتیجه دارد، نه لیست).

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

/** Rule 4 (Blocking): نقض technical_constraints — عدم تطابق aspect_ratio. */
fun validateShotAspectRatio(aspectRatio: String, dna: ProjectDna): ValidationIssue? {
    val required = dna.outputConstraints.aspectRatio
    if (aspectRatio != required) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "نسبت تصویر ('$aspectRatio') با نسبت الزامی DNA این پروژه ('$required') مطابقت ندارد"
        )
    }
    return null
}

/**
 * Rule 3 (Warning) — تابع کمکی مستقل: عدم وجود عنصر در mandatory_elements هنگام Finalize شات.
 *
 * خارج از Scope این قدم: اتصال واقعی به «Finalize شدن یک Shot» متعلق به واحد ۰۵
 * (Shot Engine) است؛ این تابع فقط لیست عناصر نهاییِ یک شات را می‌گیرد و عناصر
 * الزامی غایب را برمی‌گرداند.
 */
fun checkMandatoryElementsPresent(
    includedElements: List<String>,
    dna: ProjectDna
): ValidationIssue? {
    val missing = dna.outputConstraints.mandatoryElements.filter { it !in includedElements }
    if (missing.isNotEmpty()) {
        return ValidationIssue(
            Severity.WARNING,
            message = "عناصر الزامی زیر در این شات وجود ندارند: ${missing.joinToString(", ")}"
        )
    }
    return null
}

/**
 * Rule 5 — تابع کمکی مستقل: آیا Override این DNA نیاز به تأیید انسانی دارد.
 *
 * خارج از Scope این قدم: نمایش دیالوگ تأیید (UI) و ثبت خودکار Scope در حین Override
 * متعلق به HumanOverride/OverrideScope واحد ۰۱ به‌علاوه‌ی لایه‌ی UI است — این تابع فقط
 * پرچم مربوطه را از DNA می‌خواند تا فراخوان (واحدهای بعدی) بتواند تصمیم بگیرد.
 */
fun requiresApprovalForOverride(dna: ProjectDna): Boolean =
    dna.overrideRules.requiresHumanApproval
