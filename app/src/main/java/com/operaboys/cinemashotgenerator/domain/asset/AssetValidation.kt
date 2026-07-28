package com.operaboys.cinemashotgenerator.domain.asset

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import kotlin.math.min

// واحد ۰۶ — قوانین اعتبارسنجی Asset (Rule 1، ۲، ۳، ۵، ۶، ۶ب، ۷، و از Migration بخش
// دوم: ۱۰، ۱۱، ۱۲)
// منبع حقیقت: docs/blueprints/06-asset-and-continuity-v2.md (نسخه ۵)
//
// MIGRATED (docs/adr/010-cross-unit-migrations.md، Migration ۱): این فایل قبلاً
// یک sealed class ValidationResult محلی (Valid/Warning/Blocking) داشت (ثبت‌شده در
// ADR-003)؛ اکنون از ValidationIssue/Severity سراسری واحد ۰۷ استفاده می‌کند (معادل
// Valid = null). Rule 4 (Hard Lock) همچنان از UpdateResult در AssetContinuity.kt
// استفاده می‌کند — آن نوع عمداً از این Migration مستثنا شد تا تضمین سطح Type System
// «Hard Lock هرگز فقط Warning نیست» از بین نرود (تصمیم تأییدشده، جزئیات در ADR-010).
//
// MIGRATION بخش دوم (docs/adr/029-unit06-continuity-tiers-migration-part1.md، بخش
// «تکمیل Migration»): Rule 10/11/12 اضافه شدند.

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
fun validateAssetIdUniqueness(assetId: String, existingIds: List<String>): ValidationIssue? {
    if (assetId in existingIds) {
        return ValidationIssue(Severity.BLOCKING, message = "شناسه‌ی '$assetId' قبلاً برای یک Asset دیگر استفاده شده است")
    }
    return null
}

/**
 * Rule 3 (Blocking): Asset در حال استفاده قابل حذف نیست.
 * shotsUsingAsset: شناسه‌ی شات‌هایی که از قبل توسط فراخوان فیلتر شده‌اند (فقط شات‌های
 * مرتبط با همین assetId) — چون واحد ۰۵ (Shot Engine) هنوز وجود ندارد، لیست ساده‌ی
 * String دریافت می‌شود، نه یک نوع Shot واقعی.
 */
fun validateAssetDeletion(assetId: String, shotsUsingAsset: List<String>): ValidationIssue? {
    if (shotsUsingAsset.isNotEmpty()) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "Asset '$assetId' در ${shotsUsingAsset.size} شات استفاده می‌شود و قابل حذف نیست"
        )
    }
    return null
}

/** Rule 5 (Blocking): حداقل یک Outfit باید is_default=true باشد. */
fun validateDefaultOutfitExists(outfits: List<Outfit>): ValidationIssue? {
    if (outfits.none { it.isDefault }) {
        return ValidationIssue(Severity.BLOCKING, message = "حداقل یک Outfit باید به‌عنوان Default مشخص شود")
    }
    return null
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
): ValidationIssue? {
    if (!fileExists(localFilePath)) {
        return ValidationIssue(Severity.BLOCKING, message = "فایل مرجع تصویر پیدا نشد: $localFilePath")
    }
    val imageCheck = validateImageFile(localFilePath, fileSizeBytes, mimeType)
    return if (imageCheck.valid) {
        null
    } else {
        ValidationIssue(Severity.BLOCKING, message = imageCheck.reason ?: "فایل تصویر نامعتبر است")
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
fun checkSimilarAssetName(name: String, existingNames: List<String>): ValidationIssue? {
    val normalized = name.trim().lowercase()
    val similarTo = existingNames.firstOrNull { existing ->
        val normalizedExisting = existing.trim().lowercase()
        val threshold = maxOf(1, (maxOf(normalized.length, normalizedExisting.length) * 0.2).toInt())
        normalized == normalizedExisting || levenshteinDistance(normalized, normalizedExisting) <= threshold
    }
    if (similarTo != null) {
        return ValidationIssue(Severity.WARNING, message = "نام '$name' با Asset موجود '$similarTo' مشابه است")
    }
    return null
}

/**
 * Rule 10 (Blocking): ObjectAsset.size/materialAndColor الزامی هستند و نمی‌توانند
 * رشته‌ی خالی/whitespace-only باشند. specialTrait اختیاری است — طبق جدول بلوپرینت
 * («فیلد اختیاری نیازی به Rule ندارد») عمداً هیچ بررسی‌ای برایش نوشته نشده.
 */
fun validateObjectAsset(asset: ObjectAsset): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    if (asset.size.isBlank()) {
        issues += ValidationIssue(Severity.BLOCKING, message = "فیلد size برای Object/Prop الزامی است و نمی‌تواند خالی باشد")
    }
    if (asset.materialAndColor.isBlank()) {
        issues += ValidationIssue(Severity.BLOCKING, message = "فیلد materialAndColor برای Object/Prop الزامی است و نمی‌تواند خالی باشد")
    }
    return issues
}

/**
 * Rule 11 (Warning): basePrompt (هر سه نوع Asset) اگر پر شده اما فقط whitespace
 * است، هشدار می‌دهد. null یا رشته‌ی کاملاً خالی مشکلی ندارند (فیلد اختیاری).
 *
 * یک تابع عمومی روی String? به‌جای سه تابع تقریباً تکراری برای هر نوع Asset —
 * دقیقاً همان الگوی تعمیم‌یافته‌ی قبلاً تأییدشده در ADR-027 (تعمیم
 * validateShotAgainstDna فراتر از کد مفهومی تحت‌اللفظی بلوپرینت) — چون basePrompt
 * روی CharacterAsset/LocationAsset/ObjectAsset هیچ Supertype مشترکی ندارد و منطق
 * بررسی صرفاً روی مقدار String متکی است، نه روی نوع Asset.
 */
fun validateBasePrompt(basePrompt: String?): ValidationIssue? {
    if (basePrompt != null && basePrompt.isNotEmpty() && basePrompt.isBlank()) {
        return ValidationIssue(Severity.WARNING, message = "basePrompt فقط شامل فاصله‌ی خالی است؛ یا محتوای واقعی وارد کنید یا خالی بگذارید")
    }
    return null
}

// Rule 12 (Blocking): ObjectAsset.subtype باید یکی از سه مقدار معتبر ObjectSubtype
// باشد. عمداً هیچ تابع Runtime برای این Rule نوشته نشده — subtype در امضای
// ObjectAsset از نوع enum class ObjectSubtype غیر-nullable است، پس این تضمین از قبل
// در سطح Type System برقرار است: کامپایلر اصلاً اجازه نمی‌دهد یک ObjectAsset با
// subtype نامعتبر ساخته شود. این دقیقاً همان الگوی اثبات‌شده با AssetContinuityTest
// («hard lock fields never produce anything other than Blocked or Allowed») است —
// یک قانون که به‌جای کد Runtime، با خودِ Type System اجرا می‌شود. تست اثباتی این
// مورد در AssetValidationTest.kt (نه یک تابع validate جدید) آمده.

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
