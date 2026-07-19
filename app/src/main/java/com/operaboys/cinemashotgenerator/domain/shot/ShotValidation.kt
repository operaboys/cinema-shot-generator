package com.operaboys.cinemashotgenerator.domain.shot

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.domain.validation.validateBeatSheetTimeline

// واحد ۰۵ — قوانین اعتبارسنجی Shot (Rule 1 تا Rule 4؛ Rule 5 ساختاری، پایین توضیح داده شده)
// منبع حقیقت: docs/blueprints/05-shot-engine.md
//
// همه‌ی توابع این فایل از ValidationIssue/Severity سراسری واحد ۰۷ استفاده می‌کنند —
// ادامه‌ی همان تصمیم تأییدشده در واحد ۰۳ (docs/adr/005-...) که برای کد جدید،
// استفاده از نوع سراسری موجود به‌جای ساخت نوع محلی دیگر ترجیح دارد.

/** Rule 1 (Blocking): shot_description حداقل ۱۰ کاراکتر. */
fun validateShotDescription(shotDescription: String): ValidationIssue? {
    if (shotDescription.length < 10) {
        return ValidationIssue(
            Severity.BLOCKING,
            field = "shot_description",
            message = "توضیح شات الزامی است (حداقل ۱۰ کاراکتر)"
        )
    }
    return null
}

/**
 * Rule 2 (Blocking): حداقل یک Subject (Character/Object/Location) متصل باشد.
 *
 * NOTE: این Rule با نمونه‌ی مشابه در بلوپرینت ۰۷ (validateDataCompleteness) که همین
 * مفهوم را Warning اعلام کرده بود (و locationIds را اصلاً بررسی نمی‌کرد) در تضاد است.
 * طبق تأیید صریح کاربر، Severity این‌جا از بلوپرینت ۰۵ (Blocking، هر سه دسته‌ی
 * Character/Object/Location) پیروی می‌کند. جزئیات در ADR-006.
 */
fun validateShotHasSubject(
    characterIds: List<String>,
    objectIds: List<String>,
    locationIds: List<String>
): ValidationIssue? {
    if (characterIds.isEmpty() && objectIds.isEmpty() && locationIds.isEmpty()) {
        return ValidationIssue(
            Severity.BLOCKING,
            field = "subjects",
            message = "هر شات باید حداقل به یک Character، Object یا Location متصل باشد"
        )
    }
    return null
}

/**
 * Rule 3 (Blocking): هر Beat.timestampSeconds باید بین ۰ و duration شات باشد.
 * مستقیماً از پیاده‌سازی موجود واحد ۰۷ استفاده می‌کند (بدون تکرار منطق) — حالا که
 * Beat واقعی اینجا تعریف شده، فقط timestamp ها استخراج و به همان تابع سراسری داده می‌شود.
 */
fun validateShotBeatTimeline(beats: List<Beat>, durationSeconds: Float): List<ValidationIssue> {
    return validateBeatSheetTimeline(beats.map { it.timestampSeconds }, durationSeconds)
}

/**
 * Rule 4 (Blocking): وجود فایل local_file_path.
 * fileExists تزریق‌پذیر است — مشابه واحد ۰۶ (validateReferenceImageFile) — چون این
 * قدم فقط منطق دامنه‌ی خالص است، بدون I/O واقعی.
 */
fun validateImageReferenceFile(localFilePath: String, fileExists: (String) -> Boolean): ValidationIssue? {
    if (!fileExists(localFilePath)) {
        return ValidationIssue(
            Severity.BLOCKING,
            field = "local_file_path",
            message = "فایل رفرنس تصویر پیدا نشد: $localFilePath"
        )
    }
    return null
}

// Rule 5 (ساختاری، نه Validation runtime): sound_profile.character_sounds هرگز
// خودکار پر نمی‌شود؛ فقط از طریق ورودی صریح کاربر.
//
// تضمین: در کل این فایل (و ShotModels.kt) هیچ تابعی برای auto-generate کردن
// CharacterSound نوشته نشده — برخلاف AmbientSound که SoundProfile.ambientAutoGenerate
// و مکانیزم تولید خودکار مشخصی برایش در نظر گرفته شده، CharacterSound هیچ پرچم یا
// تابع سازنده‌ی خودکاری ندارد؛ تنها راه افزودن یک نمونه، ساخت مستقیم و صریح
// CharacterSound(...) توسط فراخوان (لایه‌ی بالاتر/UI) است. این یک تضمین «عدم وجود
// کد»، نه یک بررسی Runtime قابل‌شکست — نمی‌توان به‌طور تصادفی آن را دور زد بدون
// نوشتن کد جدید که خودش این قاعده را نقض کند.
