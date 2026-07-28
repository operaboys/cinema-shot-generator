package com.operaboys.cinemashotgenerator.domain.shot

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.domain.validation.validateBeatSheetTimeline

// واحد ۰۵ — قوانین اعتبارسنجی Shot (Rule 1 تا Rule 4؛ Rule 5 ساختاری، پایین توضیح داده شده؛
// Rule 8 اضافه‌شده در Migration بعدی — رفع F9 ممیزی pre-Unit 16)
// منبع حقیقت: docs/blueprints/05-shot-engine-v2.md
//
// همه‌ی توابع این فایل از ValidationIssue/Severity سراسری واحد ۰۷ استفاده می‌کنند —
// ادامه‌ی همان تصمیم تأییدشده در واحد ۰۳ (docs/adr/005-...) که برای کد جدید،
// استفاده از نوع سراسری موجود به‌جای ساخت نوع محلی دیگر ترجیح دارد.
//
// NOTE: این فایل هیچ تابع تجمیع‌کننده‌ی سطح‌بالا (validateShot) ندارد — هر Rule
// مستقل و جداگانه فراخوانی می‌شود (تأییدشده با grep،
// docs/adr/040-unit05-unit08-type-registry-negative-prompt-rule8.md). Rule 8 هم به
// همین شکل مستقل اضافه شد؛ سیم‌کشی به یک جریان Validation واقعی (مثلاً هنگام Save
// شات در واحد ۱۶) کار آینده است.

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
 * NOTE: این Rule قبلاً با نمونه‌ی مشابه در واحد ۰۷ (validateDataCompleteness) در تضاد
 * بود (آنجا Warning بود و locationIds را بررسی نمی‌کرد؛ جزئیات در ADR-006). این تضاد
 * در Migration ۴ (docs/adr/010-cross-unit-migrations.md) با هم‌ترازکردن واحد ۰۷ با
 * همین‌جا رفع شد — منطق این تابع بدون تغییر ماند، فقط واحد ۰۷ به‌روزرسانی شد.
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

/**
 * Rule 8 (Warning): negative_prompt_override، اگر null نباشد، نباید کاملاً
 * whitespace-only باشد (رفع F9 ممیزی pre-Unit 16، docs/blueprints/05-shot-engine-v2.md:272).
 * `null` یک حالت کاملاً معتبر است (یعنی از DNA پروژه ارث می‌برد —
 * `resolveNegativePrompt`)؛ این Rule فقط زمانی فعال می‌شود که کاربر صریحاً یک
 * Override نوشته باشد اما آن مقدار بی‌معنی باشد.
 */
fun validateNegativePromptOverride(shot: Shot): ValidationIssue? {
    val override = shot.negativePromptOverride ?: return null
    if (override.isBlank()) {
        return ValidationIssue(
            Severity.WARNING,
            field = "negative_prompt_override",
            message = "negative_prompt_override فقط فاصله‌ی خالی است — یا آن را حذف کن تا از DNA پروژه ارث ببرد، یا یک مقدار معنادار وارد کن"
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
