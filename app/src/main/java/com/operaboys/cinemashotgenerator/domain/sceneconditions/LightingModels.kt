package com.operaboys.cinemashotgenerator.domain.sceneconditions

import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle

// واحد ۰۸ — بخش الف: Lighting System (ساختار داده)
// منبع حقیقت: docs/blueprints/08-scene-conditions.md
//
// همه‌ی ۹ پارامتر جدول این بخش گزینه‌های ثابت و محدود دارند (به‌جز rim_light که
// خودش Boolean است) — بنابراین همه به‌صورت enum مدل شدند، نه String.
//
// MIGRATED (docs/adr/027-unit02-dna-manager-v5-migration.md): LightingStyle محلی
// این واحد (۶ مقدار: NATURAL, SOFT, HARD, DRAMATIC, CINEMATIC, NOIR) حذف شد؛
// domain.dna.LightingStyle (واحد ۰۲، ۲۲ مقدار) از این پس تنها مالک این نام در کل
// پروژه است. سه مقدار قدیمی نام دقیقاً یکسان در enum جدید نداشتند (NATURAL→
// NATURAL_LIGHT، DRAMATIC→DRAMATIC_LIGHT، NOIR→LOW_KEY — طبق تصمیم صریح معمار)؛
// SOFT/HARD/CINEMATIC اصلاً استفاده‌ای در کد پروژه نداشتند (تأیید با grep).

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

/**
 * تجمیع محلیِ فقط همان فیلدهایی از Lighting که واحد ۱۱ (Prompt Engineering Core) واقعاً
 * می‌خواند (style، keyLightPosition، contrastRatio) — نه یک نوع کامل جدید. ابتدا در
 * domain.promptengine تعریف شده بود؛ به اینجا منتقل شد (docs/adr/013-...) چون واحد ۰۵
 * (Shot.lighting) هم اکنون به نوع واقعیِ نتیجه‌ی resolve نیاز دارد و domain.shot نباید
 * به domain.promptengine (بالادست‌ترین واحد) وابسته شود.
 */
data class LightingSettings(
    val style: LightingStyle,
    val keyLightPosition: KeyLightPosition,
    val contrastRatio: ContrastRatio
)
