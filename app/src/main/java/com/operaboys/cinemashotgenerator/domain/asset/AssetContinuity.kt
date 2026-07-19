package com.operaboys.cinemashotgenerator.domain.asset

// واحد ۰۶ — Hard Lock مطلق (Rule 4)
// منبع حقیقت: docs/blueprints/06-asset-and-continuity.md
//
// تفاوت بنیادی با Soft Lock واحد ۰۲ (DNA Manager): identity_lock/appearance_lock/age_lock
// همیشه Blocking هستند، بدون هیچ استثنا. UpdateResult حتی نوع Warning ندارد — یعنی در سطح
// Type System هم امکان «فقط هشدار دادن» برای این قوانین وجود ندارد.

sealed class UpdateResult {
    object Allowed : UpdateResult()
    data class Blocked(val reason: String) : UpdateResult()
}

/**
 * Hard Lock — بدون استثنا. برخلاف DNA Soft Lock، اینجا هرگز فقط هشدار داده نمی‌شود.
 * پیاده‌سازی دقیقاً طبق کد مفهومی بلوپرینت — بدون هیچ تغییر یا نرم‌تر کردن.
 */
fun validateCharacterUpdate(
    rules: ContinuityRules,
    fieldBeingChanged: String
): UpdateResult {
    return when {
        rules.identityLock && fieldBeingChanged in listOf("name", "asset_id") ->
            UpdateResult.Blocked("این کاراکتر identity_lock دارد؛ نام و ID قابل تغییر نیستند")
        rules.appearanceLock && fieldBeingChanged == "physical_appearance" ->
            UpdateResult.Blocked("این کاراکتر appearance_lock دارد؛ ظاهر پس از قفل‌شدن قابل تغییر نیست")
        rules.ageLock && fieldBeingChanged == "age_range" ->
            UpdateResult.Blocked("این کاراکتر age_lock دارد؛ سن قابل تغییر نیست")
        fieldBeingChanged in rules.allowedOverrides ->
            UpdateResult.Allowed
        else -> UpdateResult.Allowed
    }
}
