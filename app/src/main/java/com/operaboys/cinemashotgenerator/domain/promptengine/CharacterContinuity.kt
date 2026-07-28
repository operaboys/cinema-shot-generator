package com.operaboys.cinemashotgenerator.domain.promptengine

import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.selectOutfitForScene
import com.operaboys.cinemashotgenerator.domain.scene.Scene

// واحد ۱۱ — فاز ۴: اعمال Character Continuity
// منبع حقیقت: docs/blueprints/11-prompt-engineering-core.md
//
// دو انحراف تأییدشده از کد مفهومی بلوپرینت (هر دو تأییدشده توسط کاربر پیش از پیاده‌سازی،
// جزئیات در docs/adr/012-unit11-prompt-engineering-core-deviations.md):
// ۱. scene.weather در بلوپرینت فرض شده بود، اما Scene واقعاً چنین فیلدی ندارد (weather
//    مفهوم واحد ۰۸/Environment است، نه Scene) — sceneWeather به‌عنوان پارامتر خارجی
//    جدید اضافه شد.
// ۲. character.outfitOverride در بلوپرینت فرض شده بود، اما CharacterAsset چنین فیلدی
//    ندارد و هیچ منبع داده‌ی دیگری هم برای «انتخاب دستی Outfit در یک شات خاص» در
//    واحدهای پیاده‌شده وجود ندارد — manualOverrideId همیشه null است.
//
// MIGRATED (docs/adr/031-unit06-physical-appearance-gender-migration.md): تابع محلی
// describePhysicalAppearance حذف شد — PhysicalAppearance.toPromptString() (واحد ۰۶،
// نسخه ۵) اکنون همان کار را انجام می‌دهد. این دقیقاً همان باگی بود که بلوپرینت واحد ۰۶
// نسخه ۵ کشف کرد (توصیف مستقیم فیلدهای خام به‌جای تابع رسمی)؛ در این پروژه توصیف
// دستی صریح بود (نه toString() پیش‌فرض)، پس خروجی همیشه معنادار بوده، اما اکنون منبع
// واحد این منطق toPromptString() است، نه دو محل مستقل.

/** توصیف فیزیکی قفل‌شده‌ی کاراکتر را بدون تغییر در پرامپت اعمال می‌کند (طبق واحد ۰۶ - Hard Lock). */
fun enforceCharacterContinuity(
    characters: List<CharacterAsset>,
    scene: Scene,
    sceneWeather: String?
): List<String> {
    return characters.map { character ->
        val outfit = selectOutfitForScene(character.outfits, sceneWeather, manualOverrideId = null)
        "${character.physicalAppearance.toPromptString()}, wearing ${outfit.description}"
    }
}
