package com.operaboys.cinemashotgenerator.domain.asset

import kotlin.math.min

// واحد ۰۶ — قوانین اعتبارسنجی Asset (Rule 1، ۲، ۳، ۵، ۶، ۶ب، ۷)
// منبع حقیقت: docs/blueprints/06-asset-and-continuity.md
//
// NOTE: این ValidationResult محلیِ واحد ۰۶ است (Valid/Warning/Blocking)، هم‌شکل با
// نوع محلی واحد ۰۲ (DNA Manager) اما در پکیج مجزا — هر دو موقتی تا واحد ۰۷ نوع
// سراسری را تعریف کند. Rule 4 (Hard Lock) از UpdateResult در AssetContinuity.kt
// استفاده می‌کند، نه این نوع — چون آنجا حتی Warning هم نباید ممکن باشد.
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Warning(val message: String) : ValidationResult()
    data class Blocking(val message: String) : ValidationResult()
}

data class ImageValidationResult(val valid: Boolean, val reason: String? = null)

/** بررسی پایه‌ی فایل تصویر قبل از پیوست به یک Asset — فرمت، سایز، و سلامت فایل. دقیقاً طبق کد مفهومی بلوپرینت. */
fun validateImageFile(filePath: String, fileSizeBytes: Long, mimeType: String): ImageValidationResult {
    val allowedTypes = setOf("image/jpeg", "image/png", "image/webp")
    val maxSizeBytes = 10 * 1024 * 1024 // 10MB — محدودیت معقول برای رفرنس محلی

    return when {
        mimeType !in allowedTypes ->
            ImageValidationResult(false, "فرمت پشتیبانی نمی‌شود؛ فقط JPEG/PNG/WebP مجاز است")
        fileSizeBytes > maxSizeBytes ->
            ImageValidationResult(false, "حجم فایل بیش از حد مجاز (۱۰ مگابایت) است")
        fileSizeBytes == 0L ->
            ImageValidationResult(false, "فایل خراب یا خالی است")
        else -> ImageValidationResult(true)
    }
}

/** Rule 1 (Blocking): یکتایی asset_id. */
fun validateAssetIdUniqueness(assetId: String, existingIds: List<String>): ValidationResult {
    if (assetId in existingIds) {
        return ValidationResult.Blocking("شناسه‌ی '$assetId' قبلاً برای یک Asset دیگر استفاده شده است")
    }
    return ValidationResult.Valid
}

/**
 * Rule 3 (Blocking): Asset در حال استفاده قابل حذف نیست.
 * shotsUsingAsset: شناسه‌ی شات‌هایی که از قبل توسط فراخوان فیلتر شده‌اند (فقط شات‌های
 * مرتبط با همین assetId) — چون واحد ۰۵ (Shot Engine) هنوز وجود ندارد، لیست ساده‌ی
 * String دریافت می‌شود، نه یک نوع Shot واقعی.
 */
fun validateAssetDeletion(assetId: String, shotsUsingAsset: List<String>): ValidationResult {
    if (shotsUsingAsset.isNotEmpty()) {
        return ValidationResult.Blocking(
            "Asset '$assetId' در ${shotsUsingAsset.size} شات استفاده می‌شود و قابل حذف نیست"
        )
    }
    return ValidationResult.Valid
}

/** Rule 5 (Blocking): حداقل یک Outfit باید is_default=true باشد. */
fun validateDefaultOutfitExists(outfits: List<Outfit>): ValidationResult {
    if (outfits.none { it.isDefault }) {
        return ValidationResult.Blocking("حداقل یک Outfit باید به‌عنوان Default مشخص شود")
    }
    return ValidationResult.Valid
}

/**
 * Rule 6 (وجود فایل) + Rule 6ب (فرمت/سایز، از validateImageFile) در یک تابع ترکیبی.
 * fileExists تزریق‌پذیر است چون در این قدم فقط منطق دامنه‌ی خالص پیاده می‌شود،
 * بدون I/O واقعی — لایه‌ی Data (واحد ۱۵) پیاده‌سازی واقعی فایل‌سیستم را تزریق می‌کند.
 */
fun validateReferenceImageFile(
    localFilePath: String,
    fileSizeBytes: Long,
    mimeType: String,
    fileExists: (String) -> Boolean
): ValidationResult {
    if (!fileExists(localFilePath)) {
        return ValidationResult.Blocking("فایل مرجع تصویر پیدا نشد: $localFilePath")
    }
    val imageCheck = validateImageFile(localFilePath, fileSizeBytes, mimeType)
    return if (imageCheck.valid) {
        ValidationResult.Valid
    } else {
        ValidationResult.Blocking(imageCheck.reason ?: "فایل تصویر نامعتبر است")
    }
}

/**
 * Rule 7 (Warning): نام مشابه با Asset دیگر.
 *
 * الگوریتم: فاصله‌ی ویرایشی Levenshtein بین نام‌های نرمال‌شده (trim + lowercase).
 * دو نام «مشابه» تلقی می‌شوند اگر پس از نرمال‌سازی دقیقاً یکسان باشند، یا فاصله‌ی
 * ویرایشی‌شان حداکثر ۲۰٪ طول بلندتر نام باشد (حداقل ۱) — یک آستانه‌ی ساده برای
 * گرفتن غلط‌های تایپی/شباهت نزدیک، بدون وابستگی به کتابخانه‌ی خارجی fuzzy-matching.
 */
fun checkSimilarAssetName(name: String, existingNames: List<String>): ValidationResult {
    val normalized = name.trim().lowercase()
    val similarTo = existingNames.firstOrNull { existing ->
        val normalizedExisting = existing.trim().lowercase()
        val threshold = maxOf(1, (maxOf(normalized.length, normalizedExisting.length) * 0.2).toInt())
        normalized == normalizedExisting || levenshteinDistance(normalized, normalizedExisting) <= threshold
    }
    if (similarTo != null) {
        return ValidationResult.Warning("نام '$name' با Asset موجود '$similarTo' مشابه است")
    }
    return ValidationResult.Valid
}

private fun levenshteinDistance(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length

    var previousRow = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        val currentRow = IntArray(b.length + 1)
        currentRow[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            currentRow[j] = min(
                min(currentRow[j - 1] + 1, previousRow[j] + 1),
                previousRow[j - 1] + cost
            )
        }
        previousRow = currentRow
    }
    return previousRow[b.length]
}
