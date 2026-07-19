package com.operaboys.cinemashotgenerator.domain.sceneconditions

// واحد ۰۸ — Mood-to-Lighting Mapping
// منبع حقیقت: docs/blueprints/08-scene-conditions.md
// دقیقاً طبق کد مفهومی بلوپرینت.

/** نگاشت خودکار از Mood پروژه/صحنه به یک پیش‌فرض نورپردازی — قابل Override دستی. */
fun mapMoodToLighting(mood: String): LightingPreset? = when (mood) {
    "tense" -> LightingPreset("dramatic", "side", "none", "high", "hard_shadows", "cold")
    "calm" -> LightingPreset("soft", "front", "strong", "low", "soft_shadows", "warm")
    "dark" -> LightingPreset("noir", "side", "none", "high", "hard_shadows", "neutral")
    "mysterious" -> LightingPreset("dramatic", "back", "soft", "medium", "soft_shadows", "cold")
    "hopeful" -> LightingPreset("cinematic", "side", "soft", "medium", "soft_shadows", "warm")
    "emotional" -> LightingPreset("soft", "side", "soft", "medium", "soft_shadows", "warm")
    else -> null
}
