package com.operaboys.cinemashotgenerator.domain.dna

import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicLanguageSettings
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode

// واحد ۰۲ — DNA Manager (ساختار داده)
// منبع حقیقت: docs/blueprints/02-dna-manager-v2.md (نسخه ۵)
//
// Migration بزرگ (بزرگ‌ترین Breaking Change این بلوپرینت تا کنون؛ جزئیات کامل در
// docs/adr/027-unit02-dna-manager-v5-migration.md):
// - VisualStyle: از ۴ مقدار کلی به ۳۴ مقدار دقیق در ۵ دسته (VisualStyleCategory).
// - Mood: از domain.story منتقل شد به اینجا — این واحد از این پس تنها مالک این نام
//   در کل پروژه است (طبق type-registry.md)؛ ۲۵ مقدار در ۵ دسته (MoodCategory).
// - LightingStyle: از domain.sceneconditions منتقل شد به اینجا؛ ۲۲ مقدار در ۴ دسته
//   (LightingCategory).
// - ContrastLevel: enum جدید — قبلاً MasterPalette.globalContrast اشتباهاً از
//   SaturationLevel استفاده می‌کرد (باگ واقعی، تأییدشده با grep، رفع شد).
// - AspectRatio: enum جدید با ۱۱ مقدار — OutputConstraints.aspectRatio از String
//   Breaking Change به این enum تغییر کرد.
// - QualityDirectives: data class کاملاً جدید (qualityTags/negativePrompt).
// - GlobalMoodBase/LightingPreference: از کامنت جای‌گذار/عدم‌وجود به data class کامل.
// - stylePreferences/overrideRules: طبق تصمیم صریح معمار (این قدم Migration)،
//   کاملاً از ProjectDna حذف شدند — دقیقاً مطابق data class ProjectDna بلوپرینت و
//   ردیف ProjectDna در type-registry.md (که هیچ‌کدام را نمی‌آورند)، با اینکه نمونه‌ی
//   JSON خودِ همین بلوپرینت هنوز این دو را دارد؛ این تناقض به معمار گزارش و توسط او
//   حل شد. جزئیات کامل در ADR-027.
// - ValidationResult (sealed class محلی که کد مفهومی این بلوپرینت برای
//   validateColorPalette استفاده کرده) عمداً اینجا بازتعریف نشد — طبق ADR-010
//   (Migration قبلی، واقعی و ثبت‌شده)، این نوع دقیقاً از همین فایل حذف شده بود و هر
//   تابع این واحد اکنون ValidationIssue? سراسری برمی‌گرداند؛ validateColorPalette
//   (در DnaValidation.kt) از همین الگوی موجود پیروی می‌کند، نه از کد مفهومی این
//   بلوپرینت که این تصمیم قدیمی‌تر را نادیده گرفته.

/** ۵ دسته‌ی VisualStyle — طبق universal-technical-variables.md بخش ۱، منبع واحد. */
enum class VisualStyleCategory { CINEMATIC, ANIMATION_3D, ANIMATION_2D, ARTISTIC, GENRE }

/** ۳۴ مقدار دقیق — کپی مستقیم از بلوپرینت ۰۲ نسخه ۵، بدون هیچ تغییر. */
enum class VisualStyle(val category: VisualStyleCategory) {
    // سینمایی
    CINEMATIC_STYLE(VisualStyleCategory.CINEMATIC),
    PHOTOREALISTIC(VisualStyleCategory.CINEMATIC),
    FILM_NOIR(VisualStyleCategory.CINEMATIC),
    VINTAGE_RETRO(VisualStyleCategory.CINEMATIC),
    DOCUMENTARY(VisualStyleCategory.CINEMATIC),
    BLOCKBUSTER(VisualStyleCategory.CINEMATIC),
    INDIE_ARTHOUSE(VisualStyleCategory.CINEMATIC),
    // انیمیشن ۳D
    PIXAR_DISNEY(VisualStyleCategory.ANIMATION_3D),
    DREAMWORKS(VisualStyleCategory.ANIMATION_3D),
    ILLUMINATION(VisualStyleCategory.ANIMATION_3D),
    LOW_POLY(VisualStyleCategory.ANIMATION_3D),
    CLAYMATION(VisualStyleCategory.ANIMATION_3D),
    ISOMETRIC(VisualStyleCategory.ANIMATION_3D),
    // انیمیشن ۲D
    ANIME(VisualStyleCategory.ANIMATION_2D),
    STUDIO_GHIBLI(VisualStyleCategory.ANIMATION_2D),
    DISNEY_CLASSIC(VisualStyleCategory.ANIMATION_2D),
    CARTOON(VisualStyleCategory.ANIMATION_2D),
    COMIC_BOOK(VisualStyleCategory.ANIMATION_2D),
    MANGA(VisualStyleCategory.ANIMATION_2D),
    // هنری
    WATERCOLOR(VisualStyleCategory.ARTISTIC),
    OIL_PAINTING(VisualStyleCategory.ARTISTIC),
    PENCIL_SKETCH(VisualStyleCategory.ARTISTIC),
    IMPRESSIONIST(VisualStyleCategory.ARTISTIC),
    POP_ART(VisualStyleCategory.ARTISTIC),
    ART_NOUVEAU(VisualStyleCategory.ARTISTIC),
    MINIMALIST(VisualStyleCategory.ARTISTIC),
    // ژانر
    EPIC_FANTASY(VisualStyleCategory.GENRE),
    SCI_FI(VisualStyleCategory.GENRE),
    CYBERPUNK(VisualStyleCategory.GENRE),
    STEAMPUNK(VisualStyleCategory.GENRE),
    GOTHIC(VisualStyleCategory.GENRE),
    HORROR(VisualStyleCategory.GENRE),
    SURREAL(VisualStyleCategory.GENRE),
    DREAMY(VisualStyleCategory.GENRE)
}

/** ۵ دسته‌ی Mood — طبق universal-technical-variables.md بخش ۲، منبع واحد. */
enum class MoodCategory { HIGH_ENERGY, POSITIVE, EMOTIONAL, DARK, CALM }

/**
 * ۲۵ مقدار دقیق — کپی مستقیم از بلوپرینت ۰۲ نسخه ۵. این enum از این پس تنها مالک
 * نام «Mood» در کل پروژه است (طبق type-registry.md) — نسخه‌ی قبلی در
 * domain.story.StoryContext.kt حذف و مصرف‌کنندگانش به اینجا وصل شدند.
 */
enum class Mood(val category: MoodCategory) {
    // انرژی بالا
    EPIC(MoodCategory.HIGH_ENERGY),
    ACTION(MoodCategory.HIGH_ENERGY),
    EXCITING(MoodCategory.HIGH_ENERGY),
    ENERGETIC(MoodCategory.HIGH_ENERGY),
    // مثبت
    HAPPY(MoodCategory.POSITIVE),
    JOYFUL(MoodCategory.POSITIVE),
    PLAYFUL(MoodCategory.POSITIVE),
    HOPEFUL(MoodCategory.POSITIVE),
    WARM(MoodCategory.POSITIVE),
    ROMANTIC(MoodCategory.POSITIVE),
    // احساسی
    EMOTIONAL(MoodCategory.EMOTIONAL),
    MELANCHOLIC(MoodCategory.EMOTIONAL),
    NOSTALGIC(MoodCategory.EMOTIONAL),
    BITTERSWEET(MoodCategory.EMOTIONAL),
    DRAMATIC(MoodCategory.EMOTIONAL),
    // تاریک
    DARK(MoodCategory.DARK),
    MYSTERIOUS(MoodCategory.DARK),
    TENSE(MoodCategory.DARK),
    SCARY(MoodCategory.DARK),
    EERIE(MoodCategory.DARK),
    // آرام
    CALM(MoodCategory.CALM),
    PEACEFUL(MoodCategory.CALM),
    SERENE(MoodCategory.CALM),
    CONTEMPLATIVE(MoodCategory.CALM),
    DREAMY_MOOD(MoodCategory.CALM)   // نام متفاوت از DREAMY بالا (VisualStyle) تا تداخل نامی در Kotlin رخ ندهد
}

/** ۴ دسته‌ی LightingStyle — طبق universal-technical-variables.md بخش ۳، منبع واحد. */
enum class LightingCategory { NATURAL, NIGHT, STUDIO, SPECIAL }

/**
 * ۲۲ مقدار دقیق — کپی مستقیم از بلوپرینت ۰۲ نسخه ۵. این enum از این پس تنها مالک
 * نام «LightingStyle» در کل پروژه است — نسخه‌ی قبلی (۶ مقدار) در
 * domain.sceneconditions.LightingModels.kt حذف و مصرف‌کنندگانش به اینجا وصل شدند.
 */
enum class LightingStyle(val category: LightingCategory) {
    // طبیعی
    NATURAL_LIGHT(LightingCategory.NATURAL),
    DAYLIGHT(LightingCategory.NATURAL),
    OVERCAST(LightingCategory.NATURAL),
    GOLDEN_HOUR(LightingCategory.NATURAL),
    BLUE_HOUR(LightingCategory.NATURAL),
    SUNSET(LightingCategory.NATURAL),
    SUNRISE(LightingCategory.NATURAL),
    // شب
    MOONLIGHT(LightingCategory.NIGHT),
    STARLIGHT(LightingCategory.NIGHT),
    CITY_NIGHT(LightingCategory.NIGHT),
    // استودیویی
    SOFT_LIGHT(LightingCategory.STUDIO),
    DRAMATIC_LIGHT(LightingCategory.STUDIO),
    HIGH_KEY(LightingCategory.STUDIO),
    LOW_KEY(LightingCategory.STUDIO),
    RIM_LIGHT(LightingCategory.STUDIO),
    BACKLIT(LightingCategory.STUDIO),
    SIDE_LIGHT(LightingCategory.STUDIO),
    // خاص
    NEON(LightingCategory.SPECIAL),
    VOLUMETRIC(LightingCategory.SPECIAL),
    CANDLELIGHT(LightingCategory.SPECIAL),
    FIRELIGHT(LightingCategory.SPECIAL),
    BIOLUMINESCENT(LightingCategory.SPECIAL)
}

enum class RealismLevel { GROUNDED, SEMI_REALISTIC, FANTASTICAL }

enum class StyleConsistency { STRICT, MODERATE, FLEXIBLE }

enum class ColorTemperature { WARM, COOL, NEUTRAL }

enum class SaturationLevel { LOW, MEDIUM, HIGH, VERY_HIGH }

/**
 * enum جدید — قبلاً MasterPalette.globalContrast اشتباهاً از SaturationLevel استفاده
 * می‌کرد (کنتراست ≠ اشباع رنگ؛ مقدار نمونه‌ی بلوپرینت "medium_high" هم اصلاً در
 * SaturationLevel وجود نداشت) — این باگ واقعی با grep تأیید و در همین Migration رفع شد.
 */
enum class ContrastLevel { LOW, MEDIUM, MEDIUM_HIGH, HIGH }

/** فهرست ثابت نسبت‌های تصویر — جایگزین رشته‌ی آزاد قبلی OutputConstraints.aspectRatio. */
enum class AspectRatio(val displayValue: String) {
    LANDSCAPE_16_9("16:9"),
    LANDSCAPE_21_9("21:9"),
    CINEMASCOPE_2_35("2.35:1"),
    ANAMORPHIC_2_39("2.39:1"),
    FLAT_1_85("1.85:1"),
    CLASSIC_4_3("4:3"),
    PHOTO_3_2("3:2"),
    PORTRAIT_9_16("9:16"),
    INSTAGRAM_4_5("4:5"),
    PORTRAIT_2_3("2:3"),
    SQUARE_1_1("1:1")
}

/** Core Identity — Soft Lock: همیشه قابل تغییر، حتی پس از locked=true. */
data class CoreIdentity(
    val dominantVisualStyle: VisualStyle,
    val realismLevel: RealismLevel,
    val styleConsistency: StyleConsistency,
    val locked: Boolean = false
)

data class MasterPalette(
    val colorTemperature: ColorTemperature,
    val globalSaturation: SaturationLevel,
    val globalContrast: ContrastLevel,   // اصلاح‌شده — قبلاً اشتباهاً SaturationLevel بود
    val colorGradingPreset: String,
    val colorPalette: List<String> = emptyList()   // دقیقاً تا ۵ مقدار Hex، مثل "#3B82F6"؛ اختیاری، مکمل colorPhilosophy توصیفی
)

data class OutputConstraints(
    val forbiddenElements: Map<String, List<String>>, // مثلاً "camera" -> ["dutch_angle"]
    val mandatoryElements: List<String>,
    val maxShotDurationSeconds: Int,
    val aspectRatio: AspectRatio                        // Breaking Change — قبلاً String آزاد بود
)

/** بخش کاملاً جدید — پیش‌فرض خالی، Backward Compatible. */
data class QualityDirectives(
    val qualityTags: String = "",       // رشته‌ی آزاد، افزوده به انتهای هر پرامپت رندرشده
    val negativePrompt: String = ""     // رشته‌ی آزاد، آنچه باید غایب باشد (در سطح Shot هم قابل Override، طبق واحد ۰۵)
)

/**
 * اکنون کامل تعریف شده — در نسخه‌های قبلی فقط کامنت جای‌گذار (primaryEmotion: String)
 * بود. این نوع مستقیماً توسط getPacingFromEmotion (۰۳) و mapMoodToLighting (۰۸)
 * مصرف می‌شود — هر دو در Migration های جداگانه‌ی بعدی به این نوع وصل می‌شوند.
 */
data class GlobalMoodBase(
    val primaryEmotion: Mood,               // اکنون از فهرست کامل ۲۵ گزینه‌ای، نه رشته‌ی آزاد
    val intensity: String = "medium",       // low/medium/high — رشته‌ی ساده، طبق تصریح بلوپرینت (نه IntensityLevel enum قبلی)
    val consistency: StyleConsistency = StyleConsistency.MODERATE
)

/** پیش‌فرض سراسری نورپردازی پروژه — قابل Override در سطح Scene/Shot (طبق واحد ۰۸). */
data class LightingPreference(
    val preferredStyle: LightingStyle? = null   // اختیاری؛ اگر null، واحد ۰۸ از Mood-to-Lighting Mapping پیش‌فرض استفاده می‌کند
)

/**
 * فیلدهای این data class دقیقاً مطابق data class ProjectDna بلوپرینت ۰۲ نسخه ۵ و
 * ردیف ProjectDna در type-registry.md است — بدون stylePreferences/overrideRules
 * (طبق تصمیم صریح معمار در این Migration؛ جزئیات کامل در ADR-027).
 *
 * cinematicLanguage (تکمیل Rule یتیم — قدم ۱ از ۴، ADR-106): این افزودنی تازه‌ای
 * است، نه بازگرداندن stylePreferences/overrideRules بالا — آن دو مفهومی کاملاً
 * متفاوت بودند (requiresApprovalForOverride، تأیید دستی Override؛ ADR-027)، در
 * حالی که CinematicLanguageSettings از قبل در domain.visualidentity.CinematicLanguage.kt
 * (واحد ۰۳ بخش ب) کامل و درست تعریف شده بود اما تا این قدم به هیچ‌جا وصل نشده بود.
 * پیش‌فرض globalMode=BALANCED (خنثی‌ترین حالت سه‌گانه، طبق بلوپرینت) و sceneOverrides
 * خالی — Breaking-Change-کمینه، پروژه‌ها/تست‌های موجود بدون این فیلد صریح هم کار
 * می‌کنند.
 */
data class ProjectDna(
    val dnaId: String,
    val projectId: String,
    val coreIdentity: CoreIdentity,
    val masterPalette: MasterPalette,
    val outputConstraints: OutputConstraints,
    val globalMoodBase: GlobalMoodBase,
    val lightingPreference: LightingPreference = LightingPreference(),
    val qualityDirectives: QualityDirectives = QualityDirectives(),
    val cinematicLanguage: CinematicLanguageSettings = CinematicLanguageSettings(globalMode = CinematicMode.BALANCED)
)
