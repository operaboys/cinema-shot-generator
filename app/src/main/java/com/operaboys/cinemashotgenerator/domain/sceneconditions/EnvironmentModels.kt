package com.operaboys.cinemashotgenerator.domain.sceneconditions

// واحد ۰۸ — بخش ب: Environment & Weather Engine (ساختار داده)
// منبع حقیقت: docs/blueprints/08-scene-conditions.md
//
// همه‌ی ۷ پارامتر جدول این بخش گزینه‌های ثابت و محدود دارند — همه enum.

enum class WeatherType { CLEAR, RAIN, STORM, SNOW, FOG }
enum class WeatherIntensity { LIGHT, MEDIUM, HEAVY }
enum class WindStrength { NONE, LIGHT, STRONG }

/** طبق بلوپرینت، environmental_motion یک لیست با حداکثر ۳ مورد از این مقادیر است. */
enum class EnvironmentalMotion { FALLING_RAIN, BLOWING_LEAVES, SNOWFALL, DUST_CLOUDS, FLYING_DEBRIS }

enum class GroundState { DRY, WET, MUDDY, SNOW_COVERED, SANDY, ICY }
enum class Visibility { CLEAR, REDUCED, LOW }
enum class TemperatureFeel { HOT, MILD, COLD }

/**
 * NOTE: شکل داده‌ای این نوع دقیقاً با AmbientSound (واحد ۰۵، domain.shot) یکسان است
 * (هر دو: type/intensity/description از جنس String) — بلوپرینت ۰۸ عمداً نام جدیدی
 * برای خروجی این Mapping تعریف کرده، نه بازاستفاده مستقیم از AmbientSound. جزئیات
 * در docs/adr/009-unit08-scene-conditions-deviations.md.
 */
data class AmbientSoundSuggestion(val type: String, val intensity: String, val description: String)

/**
 * تجمیع محلیِ فقط weatherType — همان فیلدی که واحد ۱۱ (Prompt Engineering Core) واقعاً
 * می‌خواند. ابتدا در domain.promptengine تعریف شده بود؛ به اینجا منتقل شد
 * (docs/adr/013-...) با همان دلیل LightingSettings.
 */
data class EnvironmentSettings(
    val weatherType: WeatherType
)
