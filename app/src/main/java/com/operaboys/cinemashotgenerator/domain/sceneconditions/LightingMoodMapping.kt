package com.operaboys.cinemashotgenerator.domain.sceneconditions

import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.dna.MoodCategory

// واحد ۰۸ — Mood-to-Lighting Mapping
// منبع حقیقت: docs/blueprints/08-scene-conditions.md
//
// MIGRATED (docs/adr/039-unit03-unit08-mood-type-safety-migration.md، رفع F3 ممیزی
// pre-Unit 16): امضا از mood: String به mood: Mood (domain.dna، ۲۵ مقدار واقعی) تغییر
// کرد؛ LightingPreset هم از فیلدهای String آزاد به enum های واقعی متناظرشان
// (LightingModels.kt) Type-Safe شد. جزئیات کامل نگاشت هر مقدار و تصمیم پوشش کامل
// ۲۵ مقدار (به‌جای فقط ۶ مورد قبلی) در همان ADR.

/**
 * نگاشت خودکار از Mood پروژه/صحنه به یک پیش‌فرض نورپردازی — قابل Override دستی.
 * Total است (بدون null): ۶ مقدار اصلی بلوپرینت با پیش‌فرض دقیق و نوانس‌دار خودشان
 * حفظ شدند؛ برای ۱۹ مقدار باقی‌مانده‌ی Mood، یک پیش‌فرض معقول بر اساس
 * `mood.category` (`defaultLightingPresetForCategory`) استفاده می‌شود.
 */
fun mapMoodToLighting(mood: Mood): LightingPreset = when (mood) {
    Mood.TENSE -> LightingPreset(
        LightingStyle.DRAMATIC_LIGHT, KeyLightPosition.SIDE, FillLight.NONE,
        ContrastRatio.HIGH, ShadowQuality.HARD_SHADOWS, ColorTemperature.COLD
    )
    Mood.CALM -> LightingPreset(
        LightingStyle.SOFT_LIGHT, KeyLightPosition.FRONT, FillLight.STRONG,
        ContrastRatio.LOW, ShadowQuality.SOFT_SHADOWS, ColorTemperature.WARM
    )
    Mood.DARK -> LightingPreset(
        LightingStyle.LOW_KEY, KeyLightPosition.SIDE, FillLight.NONE,
        ContrastRatio.HIGH, ShadowQuality.HARD_SHADOWS, ColorTemperature.NEUTRAL
    )
    Mood.MYSTERIOUS -> LightingPreset(
        LightingStyle.DRAMATIC_LIGHT, KeyLightPosition.BACK, FillLight.SOFT,
        ContrastRatio.MEDIUM, ShadowQuality.SOFT_SHADOWS, ColorTemperature.COLD
    )
    Mood.HOPEFUL -> LightingPreset(
        LightingStyle.HIGH_KEY, KeyLightPosition.SIDE, FillLight.SOFT,
        ContrastRatio.MEDIUM, ShadowQuality.SOFT_SHADOWS, ColorTemperature.WARM
    )
    Mood.EMOTIONAL -> LightingPreset(
        LightingStyle.SOFT_LIGHT, KeyLightPosition.SIDE, FillLight.SOFT,
        ContrastRatio.MEDIUM, ShadowQuality.SOFT_SHADOWS, ColorTemperature.WARM
    )
    else -> defaultLightingPresetForCategory(mood.category)
}

/**
 * پیش‌فرض سطح‌دسته برای ۱۹ مقدار Mood که معادل نوانس‌دار اختصاصی ندارند — هرکدام
 * برگرفته از همان پیش‌فرض بالا برای عضو نمونه‌ی همان دسته (مثلاً DARK از پیش‌فرض
 * `Mood.DARK` الگو گرفته)، تا رفتار برای اعضای هم‌خانواده یک دسته یکدست بماند.
 */
private fun defaultLightingPresetForCategory(category: MoodCategory): LightingPreset = when (category) {
    MoodCategory.HIGH_ENERGY -> LightingPreset(
        LightingStyle.DRAMATIC_LIGHT, KeyLightPosition.SIDE, FillLight.NONE,
        ContrastRatio.HIGH, ShadowQuality.HARD_SHADOWS, ColorTemperature.NEUTRAL
    )
    MoodCategory.POSITIVE -> LightingPreset(
        LightingStyle.HIGH_KEY, KeyLightPosition.FRONT, FillLight.STRONG,
        ContrastRatio.LOW, ShadowQuality.SOFT_SHADOWS, ColorTemperature.WARM
    )
    MoodCategory.EMOTIONAL -> LightingPreset(
        LightingStyle.SOFT_LIGHT, KeyLightPosition.SIDE, FillLight.SOFT,
        ContrastRatio.MEDIUM, ShadowQuality.SOFT_SHADOWS, ColorTemperature.WARM
    )
    MoodCategory.DARK -> LightingPreset(
        LightingStyle.LOW_KEY, KeyLightPosition.SIDE, FillLight.NONE,
        ContrastRatio.HIGH, ShadowQuality.HARD_SHADOWS, ColorTemperature.COLD
    )
    MoodCategory.CALM -> LightingPreset(
        LightingStyle.SOFT_LIGHT, KeyLightPosition.FRONT, FillLight.STRONG,
        ContrastRatio.LOW, ShadowQuality.SOFT_SHADOWS, ColorTemperature.WARM
    )
}
