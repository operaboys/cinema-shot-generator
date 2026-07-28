package com.operaboys.cinemashotgenerator.domain.asset

// واحد ۰۶ — Hard/Soft Lock تداوم (Rule 4 + سطوح جدید Character/Location/Prop)
// منبع حقیقت: docs/blueprints/06-asset-and-continuity-v2.md (نسخه ۵)
//
// MIGRATION (docs/adr/029-unit06-continuity-tiers-migration-part1.md، هر دو بخش کامل
// شدند): AssetContinuityTest.kt در بخش دوم به امضای جدید سه‌آرگومانی validateCharacterUpdate
// به‌روزرسانی و exhaustiveness آن روی UpdateResult (سه حالت اکنون) اصلاح شد.
//
// تفاوت بنیادی با Soft Lock واحد ۰۲ (DNA Manager): سطح FULL هنوز دقیقاً همان رفتار Hard
// Lock قدیمی (بدون هیچ استثنا، بدون هیچ نرم‌ترشدن) را دارد — پیام‌های خطا byte-for-byte
// با نسخه‌ی قبل از Migration یکسان‌اند. سطوح MEDIUM/NONE و معادل‌های Location/Prop تازه‌اند.

/** MIGRATED: حالت سوم Warned اضافه شد — فقط برای MEDIUM/STYLE/FORM، هرگز برای FULL. */
sealed class UpdateResult {
    object Allowed : UpdateResult()
    data class Blocked(val reason: String) : UpdateResult()
    data class Warned(val message: String) : UpdateResult()
}

/**
 * MIGRATED: پارامتر level اضافه شد. منطق عیناً از کد مفهومی بلوپرینت کپی شده —
 * بدون تفسیر شخصی. شاخه‌ی FULL دقیقاً همان منطق/پیام‌های خطای نسخه‌ی قبل از Migration
 * است (هیچ استثنای جدیدی اضافه نشده)؛ شاخه‌های MEDIUM/NONE تازه‌اند.
 */
fun validateCharacterUpdate(
    level: CharacterContinuityLevel,
    rules: ContinuityRules,
    fieldBeingChanged: String
): UpdateResult {
    return when (level) {
        CharacterContinuityLevel.FULL -> when {
            rules.identityLock && fieldBeingChanged in listOf("name", "asset_id") ->
                UpdateResult.Blocked("این کاراکتر identity_lock دارد؛ نام و ID قابل تغییر نیستند")
            rules.appearanceLock && fieldBeingChanged == "physical_appearance" ->
                UpdateResult.Blocked("این کاراکتر appearance_lock دارد؛ ظاهر پس از قفل‌شدن قابل تغییر نیست")
            rules.ageLock && fieldBeingChanged == "age_range" ->
                UpdateResult.Blocked("این کاراکتر age_lock دارد؛ سن قابل تغییر نیست")
            fieldBeingChanged in rules.allowedOverrides -> UpdateResult.Allowed
            else -> UpdateResult.Allowed
        }
        CharacterContinuityLevel.MEDIUM -> when {
            fieldBeingChanged in listOf("name", "asset_id") ->
                UpdateResult.Blocked("این کاراکتر حتی در سطح Medium، identity_lock دارد؛ نام و ID قابل تغییر نیستند")
            fieldBeingChanged == "physical_appearance" ->
                UpdateResult.Warned("تغییر ظاهر یک کاراکتر Medium‌-lock — ممکن است باعث ناسازگاری جزئی شود")
            else -> UpdateResult.Allowed
        }
        CharacterContinuityLevel.NONE -> UpdateResult.Allowed
    }
}

/** معادل برای مکان — همیشه فقط Warning، هرگز Blocking. عیناً طبق کد مفهومی بلوپرینت. */
fun validateLocationUpdate(fieldBeingChanged: String, isStyleField: Boolean): UpdateResult {
    return if (isStyleField)
        UpdateResult.Warned("تغییر سبک بصری این مکان ممکن است با شات‌های قبلی ناسازگار باشد")
    else UpdateResult.Allowed
}

/** معادل برای شیء/لباس — همیشه فقط Warning، هرگز Blocking. عیناً طبق کد مفهومی بلوپرینت. */
fun validatePropUpdate(fieldBeingChanged: String, isFormField: Boolean): UpdateResult {
    return if (isFormField)
        UpdateResult.Warned("تغییر فرم ظاهری این شیء ممکن است با شات‌های قبلی ناسازگار باشد")
    else UpdateResult.Allowed
}
