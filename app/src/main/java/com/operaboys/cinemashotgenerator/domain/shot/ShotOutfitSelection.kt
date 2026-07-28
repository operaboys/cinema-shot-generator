package com.operaboys.cinemashotgenerator.domain.shot

import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset

// واحد ۰۵ — انتخاب Outfit یک کاراکتر در سطح Shot
// منبع حقیقت: docs/blueprints/05-shot-engine-v2.md
//
// از نوع واقعی CharacterAsset (واحد ۰۶، از قبل پیاده‌سازی‌شده) استفاده می‌کند —
// طبق دستور کار صریح، بازتعریف نشد.

/**
 * انتخاب Outfit یک کاراکتر در این شات: خودکار از شرایط Scene
 * استنباط می‌شود (طبق condition در Asset)، مگر کاربر دستی Override کرده باشد.
 */
fun selectOutfitForShot(
    character: CharacterAsset,
    sceneWeather: String?,
    manualOverrideOutfitId: String?
): String {
    if (manualOverrideOutfitId != null) return manualOverrideOutfitId
    val matched = character.outfits.firstOrNull { it.condition?.weather == sceneWeather }
    return matched?.id ?: character.outfits.first { it.isDefault }.id
}
