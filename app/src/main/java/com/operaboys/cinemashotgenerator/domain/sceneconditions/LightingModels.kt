package com.operaboys.cinemashotgenerator.domain.sceneconditions

// واحد ۰۸ — بخش الف: Lighting System (ساختار داده)
// منبع حقیقت: docs/blueprints/08-scene-conditions.md
//
// همه‌ی ۹ پارامتر جدول این بخش گزینه‌های ثابت و محدود دارند (به‌جز rim_light که
// خودش Boolean است) — بنابراین همه به‌صورت enum مدل شدند، نه String.

enum class LightingStyle { NATURAL, SOFT, HARD, DRAMATIC, CINEMATIC, NOIR }
enum class KeyLightPosition { FRONT, SIDE, BACK, TOP, BOTTOM }
enum class FillLight { NONE, SOFT, STRONG }
enum class LightSourceCount { SINGLE, DUAL, MULTI }
enum class ContrastRatio { LOW, MEDIUM, HIGH }

/**
 * محلی به این واحد — عمداً بازاستفاده از ColorTemperature واحد ۰۲ (domain.dna) نشد،
 * چون واژگان یکسان نیست: آنجا {WARM, COOL, NEUTRAL} است، اینجا طبق بلوپرینت ۰۸
 * {Warm, Neutral, Cold, Mixed} — «Cold» به‌جای «Cool» و یک مقدار MIXED اضافه دارد.
 */
enum class ColorTemperature { WARM, NEUTRAL, COLD, MIXED }

enum class ShadowQuality { SOFT_SHADOWS, HARD_SHADOWS }
enum class LightingMotivation { SUNLIGHT, ARTIFICIAL, MOONLIGHT, FIRE, PRACTICAL, MIXED }

/**
 * خروجی mapMoodToLighting — دقیقاً طبق کد مفهومی بلوپرینت با فیلدهای String
 * (نه enum های بالا)، چون بلوپرینت این نوع را صراحتاً با مقادیر رشته‌ای snake_case
 * تعریف کرده (مثل "hard_shadows"، "cold").
 */
data class LightingPreset(
    val style: String,
    val keyLightPosition: String,
    val fillLight: String,
    val contrastRatio: String,
    val shadowQuality: String,
    val colorTemperature: String
)
