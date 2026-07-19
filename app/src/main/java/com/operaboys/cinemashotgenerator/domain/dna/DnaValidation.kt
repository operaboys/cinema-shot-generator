package com.operaboys.cinemashotgenerator.domain.dna

// واحد ۰۲ — منطق Soft Lock و قوانین اعتبارسنجی (Rule 1 تا Rule 5)
// منبع حقیقت: docs/blueprints/02-dna-manager.md

/**
 * NOTE: این نوع محلیِ واحد ۰۲ است و عمداً شکلی متفاوت از ValidationResult واحد ۰۱ دارد
 * (Sealed تک‌نتیجه‌ای در برابر data class لیست خطا/هشدار). هر دو موقتی‌اند تا واحد ۰۷
 * نوع سراسری را تعریف کند. (ثبت‌شده در docs/adr/002-unit02-dna-manager-deviations.md)
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Warning(val message: String) : ValidationResult()
    data class Blocking(val message: String) : ValidationResult()
}

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
): ValidationResult {
    val forbidden = dna.outputConstraints.forbiddenElements[elementCategory] ?: emptyList()
    if (shotElement in forbidden) {
        return ValidationResult.Blocking(
            "این عنصر ('$shotElement') در DNA این پروژه ممنوع است"
        )
    }
    return ValidationResult.Valid
}

/** Rule 4 (Blocking): نقض technical_constraints — تجاوز از max_shot_duration. */
fun validateShotDuration(durationSeconds: Int, dna: ProjectDna): ValidationResult {
    val max = dna.outputConstraints.maxShotDurationSeconds
    if (durationSeconds > max) {
        return ValidationResult.Blocking(
            "مدت این شات ($durationSeconds ثانیه) از حداکثر مجاز ($max ثانیه) در DNA این پروژه بیشتر است"
        )
    }
    return ValidationResult.Valid
}

/** Rule 4 (Blocking): نقض technical_constraints — عدم تطابق aspect_ratio. */
fun validateShotAspectRatio(aspectRatio: String, dna: ProjectDna): ValidationResult {
    val required = dna.outputConstraints.aspectRatio
    if (aspectRatio != required) {
        return ValidationResult.Blocking(
            "نسبت تصویر ('$aspectRatio') با نسبت الزامی DNA این پروژه ('$required') مطابقت ندارد"
        )
    }
    return ValidationResult.Valid
}

/**
 * Rule 3 (Warning) — تابع کمکی مستقل: عدم وجود عنصر در mandatory_elements هنگام Finalize شات.
 *
 * خارج از Scope این قدم: اتصال واقعی به «Finalize شدن یک Shot» متعلق به واحد ۰۵
 * (Shot Engine) است که هنوز وجود ندارد؛ این تابع فقط لیست عناصر نهاییِ یک شات را
 * (از هر منبعی که واحد ۰۵ بعداً فراهم کند) می‌گیرد و عناصر الزامی غایب را برمی‌گرداند.
 */
fun checkMandatoryElementsPresent(
    includedElements: List<String>,
    dna: ProjectDna
): ValidationResult {
    val missing = dna.outputConstraints.mandatoryElements.filter { it !in includedElements }
    if (missing.isNotEmpty()) {
        return ValidationResult.Warning(
            "عناصر الزامی زیر در این شات وجود ندارند: ${missing.joinToString(", ")}"
        )
    }
    return ValidationResult.Valid
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
