# واحد ۰۲: مدیر DNA پروژه (DNA Manager)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**وضعیت:** فعال
**وابستگی:** Story & Override (ورودی اولیه از StoryContext)

**نسخه:** ۴ — رفع باگ نوع `MasterPalette.globalContrast` (بود `SaturationLevel`، باید `ContrastLevel` باشد؛ enum جدید تعریف شد).

**نسخه:** ۵ — بازنویسی برای رفع یک تناقض عددی کشف‌شده در بازبینی معماری مستقل: اعداد تقریبی («۳۵+»، «۲۵+») که در متن این بلوپرینت برای توصیف تعداد مقادیر enum های `VisualStyle`/`Mood`/`LightingStyle` به‌کار رفته بودند، با شمارش دقیق مقادیر واقعی enum (که در همین سند تعریف شده) هماهنگ نبودند. شمارش دقیق: `VisualStyle` = ۳۴ مقدار (نه ۳۵+) در ۵ دسته (نه ۶ دسته)، `Mood` = ۲۵ مقدار (تطبیق کامل با «۲۵+»، فقط علامت + حذف شد برای دقت)، `LightingStyle` = ۲۲ مقدار (نه ۲۵+). این اعداد اکنون در سرتاسر این سند دقیق و یکسان‌اند.

---

## تعریف

DNA پروژه مجموعه‌ای از قوانین سراسری است که هویت بصری و سینمایی پروژه را در تمام Scene و Shot ها حفظ می‌کند.

## مسئولیت

تعریف و اعمال قوانین سراسری؛ تضمین Consistency در کل پروژه؛ جلوگیری از Drift تدریجی سبک؛ ارائه‌ی مکانیزم Override برای موارد خاص.

## معماری

```
Story & Override (StoryContext)
        ↓
DNA Manager
├─ Core Identity (Soft Lock)
│   ├─ Dominant Visual Style
│   ├─ Realism Level
│   └─ Style Consistency
├─ Master Palette (پالت رنگی سراسری)
│   └─ 🆕 Color Palette مستقیم (۵ رنگ Hex قابل‌انتخاب کاربر)
├─ Global Mood Base (حس کلی پروژه)
├─ Style Preferences (قابل Override در سطح Shot)
│   ├─ Cinematic Language
│   ├─ Color Philosophy
│   └─ Camera/Lighting Preferences
├─ Output Constraints
│   ├─ Forbidden Elements (Blocking)
│   ├─ Mandatory Elements (Warning)
│   ├─ Technical Constraints (Blocking)
│   └─ 🆕 Aspect Ratio صریح (فهرست ثابت نسبت‌های سینمایی/اجتماعی)
└─ 🆕 Quality & Negative Directives
    ├─ Quality Tags (رشته‌ی آزاد، افزوده به انتهای هر پرامپت)
    └─ Negative Prompt (رشته‌ی آزاد، آنچه باید در تصویر/ویدیو غایب باشد)
```

---

## Soft Lock — اصل کلیدی این واحد

Core Identity (سبک بصری غالب، سطح رئالیسم) **همیشه قابل تغییر است**، حتی پس از "قفل شدن". اگر تغییر با Scene/Shot موجود ناسازگار باشد، سیستم فقط **هشدار** می‌دهد؛ هرگز بلاک نمی‌کند. کاربر تصمیم‌گیرنده‌ی نهایی است.

این با **Character Continuity Lock** (در واحد Asset & Continuity، بخش Hard Lock) کاملاً متفاوت است — آنجا Lock مطلق است چون هدف تضمین کیفی بنیادی (ثبات ظاهری) است؛ اینجا DNA یک چارچوب سبکی قابل‌تجدیدنظر است.

🆕 **نکته‌ی مهم درباره‌ی فیلدهای این نسخه:** `colorPalette`، `qualityTags`، و `negativePrompt` هم مثل بقیه‌ی Core Identity در سطح **Soft Lock** قرار می‌گیرند — یعنی کاربر همیشه می‌تواند این‌ها را تغییر دهد، فقط در صورت ناسازگاری با Shot های موجود هشدار می‌گیرد، هرگز بلاک نمی‌شود.

---

## 🆕 Aspect Ratio — فهرست ثابت (نه رشته‌ی آزاد)

نسخه‌ی قبلی `aspect_ratio` را به‌صورت یک رشته‌ی آزاد در `technical_constraints` تعریف کرده بود. این نسخه آن را به یک **enum با فهرست ثابت** تبدیل می‌کند، چون نسبت‌های تصویر یک مجموعه‌ی محدود و شناخته‌شده در صنعت هستند و انتخاب آزاد باعث ورودی نامعتبر می‌شود.

| دسته | مقادیر |
|---|---|
| افقی (Landscape) | `16:9` (HD استاندارد)، `21:9` (اولترا واید)، `2.35:1` (سینماسکوپ)، `2.39:1` (آنامورفیک)، `1.85:1` (فلت سینمایی)، `4:3` (کلاسیک)، `3:2` (عکاسی) |
| عمودی (Portrait) | `9:16` (موبایل/استوری)، `4:5` (اینستاگرام)، `2:3` (پرتره) |
| مربع (Square) | `1:1` |

این فهرست باید در سطح enum یا یک `Set` ثابت پیاده‌سازی شود، نه یک `String` کاملاً آزاد — تا از ورودی نامعتبر (مثل `"16x9"` یا `"widescreen"`) جلوگیری شود.

---

## 🆕 Color Palette مستقیم — مکمل Color Philosophy توصیفی

نسخه‌ی قبلی فقط `style_preferences.color_philosophy` (توصیفی، مثل `"earth_tones"`, `"muted_blues"`) را داشت. این کافی برای **جهت‌گیری کلی** است، اما کاربرانی که می‌خواهند رنگ دقیق (Hex) انتخاب کنند نیازی جدا دارند — دقیقاً مثل یک هنرمند که یک پالت رنگ مشخص برای پروژه‌اش انتخاب می‌کند، نه فقط توصیف کلامی رنگ.

این نسخه یک فیلد جدید و **مستقل** اضافه می‌کند: `colorPalette: List<String>` — دقیقاً ۵ مقدار رنگ به فرمت Hex (مثل `#3B82F6`). این فیلد **مکمل** `color_philosophy` توصیفی است، نه جایگزینش — کاربر می‌تواند هر دو، یکی، یا هیچ‌کدام را پر کند. اگر `colorPalette` پر شده باشد، رندر نهایی باید ترجیحاً از این مقادیر دقیق استفاده کند؛ اگر خالی بود، به `color_philosophy` توصیفی برگردد.

---

## 🆕 Quality Tags و Negative Prompt — رشته‌های آزاد تکمیلی

این دو فیلد در نسخه‌ی قبلی اصلاً وجود نداشتند، اما در تحلیل نمونه‌ی کارکردی، جزو مهم‌ترین ابزارهای کاربر برای کنترل کیفیت خروجی بودند:

- **`qualityTags: String`** — رشته‌ی آزاد کوتاه که همیشه به انتهای هر پرامپت اضافه می‌شود (مثال: `"8K, cinematic, highly detailed, masterpiece"`). هدف: تقویت کیفیت بصری کلی خروجی، بدون نیاز به تکرار در هر Shot.
- **`negativePrompt: String`** — رشته‌ی آزاد که چیزهایی را که **نباید** در تصویر/ویدیو ظاهر شوند مشخص می‌کند (مثال: `"blurry, low quality, distorted, watermark, text"`). این مفهوم به‌طور گسترده در مدل‌های تصویرسازی/ویدیوسازی استفاده می‌شود و نسخه‌ی قبلی این پروژه هیچ مکانیزمی برایش نداشت — این یک خلأ واقعی بود، نه یک ویژگی اختیاری.

هر دو فیلد در سطح `ProjectDna` **مقدار پیش‌فرض سراسری** پروژه را تعریف می‌کنند — دقیقاً مثل نقشی که DNA برای Camera/Lighting/Environment (واحد ۰۸/۰۹) دارد. 🆕v2 **اصلاح مهم:** بر خلاف نگارش نسخه‌ی قبلی این بخش، `negativePrompt` **در سطح Shot هم قابل Override است**، نه فقط سطح DNA — طبق تحلیل تکمیلی نمونه‌ی مادر (`addShot()` مقدار DNA را به‌طور خودکار در Shot تازه‌ساخته کپی می‌کند تا کاربر بتواند بعداً مستقل تغییرش دهد). منطق کامل این Override (فیلد `negativePromptOverride` و تابع `resolveNegativePrompt`) در بلوپرینت `05-shot-engine.md` (نسخه‌ی ۲) تعریف شده است؛ این بلوپرینت (۰۲) فقط صاحب **مقدار پیش‌فرض سراسری** است، نه منطق Override.

در Model Profile های خاص (واحد ۱۴) که `supports_negative_prompt` ندارند (مثل برخی پلتفرم‌های فقط-متنی)، این فیلد باید نادیده گرفته شود، نه به‌زور در متن اصلی پرامپت تزریق شود — این تصمیم به Renderer (واحد ۱۴) واگذار می‌شود، نه به این واحد.

---

## ساختار داده (نسخه‌ی کامل و به‌روزشده)

```json
{
  "dna_id": "dna_001",
  "project_id": "proj_001",
  "core_identity": {
    "dominant_visual_style": "cinematic",
    "realism_level": "grounded",
    "style_consistency": "strict",
    "locked": true
  },
  "master_palette": {
    "color_temperature": "warm",
    "global_saturation": "medium",
    "global_contrast": "medium_high",
    "color_grading_preset": "natural",
    "color_palette": ["#3B82F6", "#8B5CF6", "#EC4899", "#F59E0B", "#10B981"]
  },
  "global_mood_base": {
    "primary_emotion": "mysterious",
    "intensity": "medium",
    "consistency": "strict"
  },
  "style_preferences": {
    "cinematic_language": "long_take",
    "color_philosophy": {
      "palette_type": "natural",
      "dominant_colors": ["earth_tones", "muted_blues"],
      "contrast_preference": "medium_high"
    },
    "camera_preferences": {
      "preferred_movements": ["dolly", "tracking", "static"],
      "avoid_movements": ["crane", "extreme_handheld"]
    },
    "lighting_preferences": {
      "preferred_styles": ["cinematic", "dramatic"],
      "avoid_styles": ["flat", "overly_bright"]
    }
  },
  "output_constraints": {
    "forbidden_elements": {
      "camera": ["dutch_angle"],
      "lighting": ["top_light"],
      "weather": []
    },
    "mandatory_elements": {
      "always_include": ["atmospheric_depth", "color_grading"]
    },
    "technical_constraints": {
      "max_shot_duration": 10,
      "aspect_ratio": "2.39:1"
    }
  },
  "quality_directives": {
    "quality_tags": "8K, cinematic, highly detailed, masterpiece",
    "negative_prompt": "blurry, low quality, distorted, watermark, text"
  },
  "override_rules": {
    "allow_scene_override": true,
    "allow_shot_override": true,
    "requires_human_approval": true
  }
}
```

---

## پیاده‌سازی مفهومی (Kotlin) — نسخه‌ی به‌روزشده

```kotlin
// 🆕v5 جایگزین enum قبلی (فقط ۴ مقدار کلی) — طبق فرم واقعی «Project DNA»، ۵ دسته با دقیقاً ۳۴ مقدار.
// این فهرست باید دقیقاً با universal-technical-variables.md (بخش ۱) هم‌راستا بماند — منبع واحد.
enum class VisualStyleCategory { CINEMATIC, ANIMATION_3D, ANIMATION_2D, ARTISTIC, GENRE }

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

// 🆕v3 enum جدید — تا این نسخه اصلاً وجود نداشت، فقط رشته‌ی خام در توابع مصرف‌کننده (۰۳، ۰۸) بود.
enum class MoodCategory { HIGH_ENERGY, POSITIVE, EMOTIONAL, DARK, CALM }

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

// 🆕v3 enum جدید — تا این نسخه Lighting فقط در بلوپرینت ۰۸ به‌صورت جدول رشته‌ای (نه enum) بود.
enum class LightingCategory { NATURAL, NIGHT, STUDIO, SPECIAL }

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

// 🆕v4 enum مستقل — قبلاً globalContrast اشتباهاً از SaturationLevel استفاده می‌کرد (کنتراست ≠ اشباع رنگ)
// و مقدار نمونه‌ی JSON ("medium_high") در SaturationLevel اصلاً وجود نداشت.
enum class ContrastLevel { LOW, MEDIUM, MEDIUM_HIGH, HIGH }

// 🆕 فهرست ثابت نسبت‌های تصویر — جایگزین رشته‌ی آزاد قبلی
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

data class CoreIdentity(
    val dominantVisualStyle: VisualStyle,   // اکنون از فهرست کامل ۳۴ گزینه‌ای بالا
    val realismLevel: RealismLevel,
    val styleConsistency: StyleConsistency,
    val locked: Boolean = false
)

data class MasterPalette(
    val colorTemperature: ColorTemperature,
    val globalSaturation: SaturationLevel,
    val globalContrast: ContrastLevel,   // 🆕v4 اصلاح شد — قبلاً اشتباهاً SaturationLevel بود
    val colorGradingPreset: String,
    val colorPalette: List<String> = emptyList()   // 🆕 دقیقاً تا ۵ مقدار Hex، مثل "#3B82F6"؛ اختیاری، مکمل color_philosophy
)

data class OutputConstraints(
    val forbiddenElements: Map<String, List<String>>,   // مثلاً "camera" -> ["dutch_angle"]
    val mandatoryElements: List<String>,
    val maxShotDurationSeconds: Int,
    val aspectRatio: AspectRatio                        // 🆕 اکنون enum، نه String آزاد
)

// 🆕 بخش کاملاً جدید
data class QualityDirectives(
    val qualityTags: String = "",       // رشته‌ی آزاد، افزوده به انتهای هر پرامپت رندرشده
    val negativePrompt: String = ""     // رشته‌ی آزاد، آنچه باید غایب باشد
)

/**
 * 🆕v3 اکنون کامل تعریف شده — در نسخه‌های قبلی فقط در نمونه‌ی JSON اشاره شده بود
 * ("primary_emotion": "mysterious")، بدون data class/enum مستقل در Kotlin.
 * این نوع مستقیماً توسط getPacingFromEmotion (۰۳) و mapMoodToLighting (۰۸) مصرف می‌شود.
 */
data class GlobalMoodBase(
    val primaryEmotion: Mood,               // اکنون از فهرست کامل ۲۵ گزینه‌ای بالا، نه رشته‌ی آزاد
    val intensity: String = "medium",       // low/medium/high — رشته‌ی ساده، چون فقط سه سطح دارد
    val consistency: StyleConsistency = StyleConsistency.MODERATE
)

/** 🆕v3 پیش‌فرض سراسری نورپردازی پروژه — قابل Override در سطح Scene/Shot (طبق واحد ۰۸). */
data class LightingPreference(
    val preferredStyle: LightingStyle? = null   // اختیاری؛ اگر null، واحد ۰۸ از Mood-to-Lighting Mapping پیش‌فرض استفاده می‌کند
)

data class ProjectDna(
    val dnaId: String,
    val projectId: String,
    val coreIdentity: CoreIdentity,
    val masterPalette: MasterPalette,
    val outputConstraints: OutputConstraints,
    val globalMoodBase: GlobalMoodBase,                                // 🆕v3 اکنون Type کامل، نه کامنت جای‌گذار
    val lightingPreference: LightingPreference = LightingPreference(),  // 🆕v3 فیلد کاملاً جدید
    val qualityDirectives: QualityDirectives = QualityDirectives()      // 🆕 پیش‌فرض خالی، Backward Compatible
)

/**
 * Soft Lock: تغییر Core Identity همیشه مجاز است؛ فقط در صورت
 * ناسازگاری با Shot های موجود هشدار می‌دهد، هرگز بلاک نمی‌کند.
 */
data class DnaUpdateResult(
    val updatedDna: ProjectDna,
    val warning: String? = null
)

fun updateCoreIdentity(
    currentDna: ProjectDna,
    newStyle: VisualStyle,
    dependentShotsCount: Int
): DnaUpdateResult {
    val updated = currentDna.copy(
        coreIdentity = currentDna.coreIdentity.copy(dominantVisualStyle = newStyle)
    )
    val warning = if (dependentShotsCount > 0) {
        "این تغییر با $dependentShotsCount شات موجود ناسازگار است. آیا مطمئنید؟"
    } else null

    return DnaUpdateResult(updated, warning)
}

/**
 * 🆕 اعتبارسنجی colorPalette: اگر پر شده، باید دقیقاً بین ۱ تا ۵ مقدار Hex معتبر باشد.
 * این یک Rule جدید است (Rule ۶ در جدول پایین‌تر).
 */
fun validateColorPalette(palette: List<String>): ValidationResult {
    if (palette.isEmpty()) return ValidationResult.Valid   // اختیاری است
    if (palette.size > 5) {
        return ValidationResult.Warning("بیش از ۵ رنگ توصیه نمی‌شود؛ فقط ۵ مورد اول استفاده می‌شود")
    }
    val hexPattern = Regex("^#[0-9A-Fa-f]{6}$")
    val invalid = palette.filterNot { hexPattern.matches(it) }
    if (invalid.isNotEmpty()) {
        return ValidationResult.Blocking("مقادیر رنگ نامعتبر: ${invalid.joinToString()}")
    }
    return ValidationResult.Valid
}

/** Forbidden Elements همیشه Blocking هستند — این تنها استثنای واقعی Soft Lock است. */
fun validateShotAgainstDna(shotCameraAngle: String, dna: ProjectDna): ValidationResult {
    val forbiddenCameraAngles = dna.outputConstraints.forbiddenElements["camera"] ?: emptyList()
    if (shotCameraAngle in forbiddenCameraAngles) {
        return ValidationResult.Blocking(
            "این عنصر ('$shotCameraAngle') در DNA این پروژه ممنوع است"
        )
    }
    return ValidationResult.Valid
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Warning(val message: String) : ValidationResult()
    data class Blocking(val message: String) : ValidationResult()
}
```

---

## قوانین اعتبارسنجی (جدول به‌روزشده)

| Rule | شرح | Severity |
|---|---|---|
| ۱ | تغییر Core Identity در صورت ناسازگاری با Shot موجود | **Warning** (هرگز Blocking) |
| ۲ | استفاده از عنصر در `forbidden_elements` | **Blocking** |
| ۳ | عدم وجود عنصر در `mandatory_elements` هنگام Finalize شات | **Warning** |
| ۴ | نقض `technical_constraints` (مثل تجاوز از `max_shot_duration`) | **Blocking** |
| ۵ | Override DNA وقتی `requires_human_approval = true` | نمایش تأیید + ثبت خودکار Scope |
| 🆕 ۶ | مقدار نامعتبر در `color_palette` (نه Hex معتبر) | **Blocking** |
| 🆕 ۷ | بیش از ۵ مقدار در `color_palette` | **Warning** (فقط ۵ مورد اول استفاده می‌شود) |

---

## اثر روی Prompt نهایی (نمونه‌ی به‌روزشده)

```
DNA:
- Visual Style: Cinematic, Grounded Realism
- Palette: Warm, Medium Saturation, High Contrast
- Color Palette (دقیق): #3B82F6, #8B5CF6, #EC4899
- Mood: Mysterious, Strong Intensity
- Cinematic Language: Long-take
- Aspect Ratio: 2.39:1 (Anamorphic)
- Quality Tags: 8K, cinematic, highly detailed, masterpiece
- Negative Prompt: blurry, low quality, distorted, watermark, text

اثر در structured_parts (بخشی از PromptBlueprint):
style_modifiers: "cinematic style, grounded realism, warm color temperature,
medium saturated tones, high contrast lighting, mysterious atmosphere,
strong intensity, continuous long-take shot, atmospheric depth,
avoid dutch angles, 2.39:1 anamorphic aspect ratio"

quality_suffix (همیشه به انتهای پرامپت رندرشده افزوده می‌شود، طبق واحد ۱۴):
"8K, cinematic, highly detailed, masterpiece"

negative_prompt (مقدار پیش‌فرض DNA؛ در پروفایل‌های مدل که پشتیبانی می‌کنند، جدا از پرامپت اصلی ارسال می‌شود — اگر Shot مربوطه یک negativePromptOverride داشته باشد، طبق resolveNegativePrompt در واحد ۰۵، همان مقدار Shot جایگزین این پیش‌فرض DNA می‌شود):
"blurry, low quality, distorted, watermark, text"
```

---

## معیارهای موفقیت (به‌روزشده)

- DNA به تمام Scene و Shot اعمال می‌شود.
- Forbidden Elements همیشه مسدود می‌شوند.
- Mandatory Elements به‌صورت Warning یادآوری می‌شوند.
- تغییر Core Identity هرگز کاربر را بلاک نمی‌کند، فقط هشدار می‌دهد.
- Override با ثبت خودکار Scope قابل کنترل و ردیابی است.
- 🆕 `aspect_ratio` همیشه یکی از مقادیر ثابت enum است، هرگز رشته‌ی دلخواه.
- 🆕 `color_palette` (در صورت پر بودن) همیشه شامل مقادیر Hex معتبر است.
- 🆕 `negative_prompt` در تمام مدل‌هایی که پشتیبانی می‌کنند اعمال می‌شود؛ در مدل‌هایی که پشتیبانی نمی‌کنند، بی‌صدا نادیده گرفته می‌شود (نه خطا).

---

## یادداشت پیاده‌سازی (برای Claude Code، هنگام اجرای این بلوپرینت به‌روزشده)

این یک قدم **Migration** روی واحد ۰۲ موجود است (که در `domain/dna/` از قبل پیاده‌سازی و تست شده)، نه یک واحد جدید از صفر:
- فیلدهای جدید (`colorPalette`, `qualityDirectives`) باید **Backward Compatible** باشند — تمام تست‌های موجود `DnaValidationTest.kt` باید بدون تغییر همچنان pass شوند (مقدار پیش‌فرض خالی/آبجکت خالی برای فیلدهای جدید).
- تغییر `aspectRatio` از `String` به `enum AspectRatio` یک **Breaking Change واقعی** است — تمام جاهایی که از `outputConstraints.aspectRatio` به‌عنوان String استفاده می‌کنند (اگر جایی در واحدهای دیگر مثل Prompt Engineering Core یا Output Delivery وجود دارد) باید بررسی و به‌روزرسانی شوند. این را با grep در کل پروژه بررسی کن، حدس نزن.
- 🆕v2 اگر بلوپرینت `05-shot-engine.md` (نسخه‌ی ۲، شامل `negativePromptOverride`) در همین Migration یا یک Migration جداگانه اجرا می‌شود، مطمئن شو ترتیب اجرا یا حداقل هماهنگی این دو بلوپرینت رعایت شده — یعنی `resolveNegativePrompt` (واحد ۰۵) باید مقدار `qualityDirectives.negativePrompt` همین واحد (۰۲) را به‌عنوان ورودی پیش‌فرض بگیرد، نه یک رشته‌ی هاردکد یا Placeholder جدا.
- 🆕v3 **این Migration بزرگ‌ترین Breaking Change این بلوپرینت تا کنون است:** `VisualStyle` از ۴ مقدار کلی به ۳۴ مقدار دقیق تغییر کرد؛ `Mood` و `LightingStyle` که تا این نسخه اصلاً enum نبودند (فقط رشته‌ی خام در کد مصرف‌کننده) اکنون enum کامل شدند (به‌ترتیب ۲۵ و ۲۲ مقدار). این یعنی حداقل سه فایل دیگر باید هم‌زمان یا بلافاصله بعد از این Migration به‌روزرسانی شوند: `03-visual-identity.md` (تابع `getPacingFromEmotion(emotion: String)` باید `Mood` بگیرد، نه `String`)، `08-scene-conditions.md` (تابع `mapMoodToLighting(mood: String)` مشابه، به‌علاوه‌ی تمام پارامترهای جدول Lighting که باید `LightingStyle`/enum های مرتبط شوند)، و `universal-technical-variables.md` (فهرست‌های بخش ۱ و ۲ باید دقیقاً با این enum ها یکی باشند، نه یک زیرمجموعه‌ی ناقص). این سه فایل هم‌زمان در همین دور بازنویسی اصلاح شده‌اند — با grep بررسی کن که نسخه‌ی هماهنگ هرکدام موجود است، قبل از شروع Migration کد.
- 🆕v5 با grep در `domain/dna/` بررسی کن که کد واقعی (اگر از قبل پیاده‌سازی شده) از همین اعداد دقیق (۳۴/۲۵/۲۲) پیروی می‌کند یا نسخه‌ی دیگری دارد که با شمارش این بلوپرینت مطابقت ندارد — اگر مطابقت نداشت، به‌عنوان یک یافته‌ی جدید گزارش بده، خودسرانه یکی را بر دیگری ترجیح نده.
- 🆕v3 `GlobalMoodBase`/`LightingPreference` برای اولین‌بار در این نسخه به‌طور کامل در Kotlin تعریف شدند (نسخه‌های قبلی فقط کامنت جای‌گذار داشتند) — بررسی کن آیا `ProjectDna` موجود در `domain/dna/` این دو فیلد را دارد یا باید کاملاً از صفر اضافه شوند.
- 🆕v4 `ContrastLevel` یک enum کاملاً جدید است — با grep بررسی کن که آیا `domain/dna/DnaModels.kt` (کد واقعی) از قبل `globalContrast` را با نوع `SaturationLevel` تعریف کرده (باگ) یا از قبل به‌درستی enum جدا داشته (بعید، ولی چک کن، حدس نزن). اگر باگ در کد واقعی هم هست، آن را اصلاح کن؛ هر جای دیگری که `MasterPalette.globalContrast` خوانده می‌شود (مثلاً در `domain/promptengine/` برای `styleModifiers`) هم باید بررسی شود که آیا فرض نوع اشتباه داشته یا نه.
- 🆕 این بلوپرینت (به‌عنوان صاحب اصلی enum های سراسری پروژه: `VisualStyle`, `Mood`, `LightingStyle`, `AspectRatio`, `ContrastLevel`) و `ContrastLevel` تازه اضافه‌شده باید در `docs/reference/type-registry.md` ثبت شده باشند/بمانند — طبق قانون جدید `ai-coding-guidelines.md` («هر enum جدید اول در type-registry ثبت شود»).
