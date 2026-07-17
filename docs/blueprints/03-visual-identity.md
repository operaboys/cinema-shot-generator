# واحد ۰۳: هویت بصری (Visual Identity System)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**ادغام از:** Style Matrix + Cinematic Language
**وضعیت:** فعال
**وابستگی:** DNA Manager

هر دو بخش این واحد زیرمجموعه‌ی DNA هستند و همیشه با هم خوانده می‌شوند تا «حال‌وهوای کلی بصری-روایی» پروژه را بسازند.

---

## بخش الف — Style Matrix (سبک بصری)

### تعریف
مدیریت سبک بصری پروژه: یک سبک اصلی (اجباری) + یک سبک ثانویه‌ی اختیاری با تأثیر کیفی (subtle/moderate/strong) — **بدون وزن‌دهی درصدی**. مدل‌های AI نسبت‌های عددی دقیق را قابل‌اعتماد رعایت نمی‌کنند؛ کاربر هم به‌ندرت به ترکیب پیچیده‌ی چند سبک نیاز دارد.

### ساختار داده

```json
{
  "style_matrix_id": "sm_001",
  "primary_style": {
    "style_id": "style_ghibli",
    "style_name": "Studio Ghibli",
    "category": "cinematic"
  },
  "secondary_style": {
    "style_id": "style_realistic",
    "style_name": "Photorealistic",
    "category": "technical",
    "influence": "subtle"
  },
  "custom_styles": [
    {
      "custom_style_id": "custom_001",
      "name": "My Dream Style",
      "base_style": "style_ghibli",
      "modifications": { "color_palette": "pastel", "texture": "soft" }
    }
  ]
}
```

### کتابخانه‌ی سبک‌های از پیش تعریف‌شده

| دسته | سبک‌ها |
|---|---|
| **سینمایی** | Studio Ghibli، Disney 1950s، Pixar Modern، Anime Makoto Shinkai، Wes Anderson |
| **هنری** | Oil Painting، Watercolor، Charcoal Sketch، Ink Wash، Ukiyo-e |
| **فنی** | Photorealistic، 8K Resolution، Unreal Engine 5، IMAX 70mm، Vintage Film Stock |
| **استیلایز** | Low Poly، Claymation، Cyberpunk Neon، Steampunk Copper |

هر سبک یک `prompt_tokens` مرجع دارد (نمونه: Studio Ghibli → `"hand-drawn animation, watercolor backgrounds, soft pastel colors, detailed natural scenery"`). این توکن‌ها بخشی از `docs/reference/universal-technical-variables.md` هستند.

### ترکیب کیفی (نه عددی)

```kotlin
enum class StyleInfluence { SUBTLE, MODERATE, STRONG }

fun getInfluenceModifier(influence: StyleInfluence): String = when (influence) {
    StyleInfluence.SUBTLE -> "with subtle hints of"
    StyleInfluence.MODERATE -> "with elements of"
    StyleInfluence.STRONG -> "strongly influenced by"
}

data class StyleReference(val styleId: String, val name: String, val promptTokens: String)

fun combineStyles(primary: StyleReference, secondary: StyleReference?, influence: StyleInfluence?): String {
    var prompt = primary.promptTokens
    if (secondary != null && influence != null) {
        prompt += ", ${getInfluenceModifier(influence)} ${secondary.promptTokens}"
    }
    return prompt
}
```

**مثال خروجی:** Primary=Ghibli + Secondary=Photorealistic(subtle) →
`"Studio Ghibli style, hand-drawn animation, watercolor backgrounds, with subtle hints of photorealistic rendering, realistic lighting and textures"`

### بررسی سازگاری

سازگاری بین سبک اصلی و ثانویه چک می‌شود، اما نتیجه **همیشه فقط Warning است، هرگز Blocking**:

```kotlin
enum class CompatibilityLevel { HIGH, MEDIUM, LOW, INCOMPATIBLE }

data class CompatibilityResult(val level: CompatibilityLevel, val warning: Boolean)

fun checkStyleCompatibility(primary: String, secondary: String): CompatibilityResult {
    // نمونه قوانین: Oil Painting + 8K Resolution => LOW (ترکیب غیرمعمول)
    // Studio Ghibli + Watercolor => HIGH
    // پیش‌فرض در نبود قانون خاص: MEDIUM
    return CompatibilityResult(level = CompatibilityLevel.MEDIUM, warning = false)
}
```

### قوانین

| Rule | شرح | Severity |
|---|---|---|
| ۱ | سبک اصلی همیشه باید تعیین‌شده باشد | **Blocking** |
| ۲ | ناسازگاری بین سبک اصلی و ثانویه | **Warning** (غیرمسدودکننده) |
| ۳ | سبک اصلی قابل حذف نیست، فقط قابل تعویض | **Blocking** روی تلاش حذف |

---

## بخش ب — Cinematic Language (زبان سینمایی)

### تعریف
ضرب‌آهنگ کلی روایت پروژه: Long-take (نماهای طولانی و متأملانه)، Fast-cut (برش‌های سریع)، یا Hybrid (ترکیب هوشمند بر اساس Beat Sheet).

### سه حالت

| حالت | مدت شات | استفاده | Transition |
|---|---|---|---|
| **Long-take** | ۸–۶۰ ثانیه | درام هنری، صحنه‌ی احساسی، مکالمه | Slow Fade/Dissolve |
| **Fast-cut** | ۱–۵ ثانیه | اکشن، تعقیب‌وگریز، مبارزه | Hard Cut |
| **Hybrid** | ۳–۲۰ ثانیه (متغیر) | اکثر پروژه‌های روایی چندژانره | متناسب با Context |

### منطق تطبیق Hybrid با Beat Sheet

```kotlin
enum class CinematicMode { LONG_TAKE, FAST_CUT, BALANCED }

/** بر اساس شدت میانگین Beat های یک Scene، حالت مناسب Hybrid را تعیین می‌کند. */
fun determineHybridPacing(sceneType: String, avgBeatIntensity: Float): CinematicMode {
    return when {
        sceneType == "action" || avgBeatIntensity >= 7f -> CinematicMode.FAST_CUT
        sceneType == "emotional" || avgBeatIntensity <= 3f -> CinematicMode.LONG_TAKE
        sceneType == "dialogue" -> CinematicMode.BALANCED
        else -> CinematicMode.BALANCED
    }
}

/** نگاشت احساس اصلی Scene (از DNA.global_mood_base) به یک پیش‌فرض ریتم. */
fun getPacingFromEmotion(emotion: String): CinematicMode = when (emotion) {
    "tense", "terrified", "furious" -> CinematicMode.FAST_CUT
    "melancholy", "serene", "contemplative" -> CinematicMode.LONG_TAKE
    else -> CinematicMode.BALANCED
}
```

### ساختار داده

```json
{
  "cinematic_language_id": "cl_001",
  "global_mode": "hybrid",
  "mode_settings": {
    "long_take": { "min_shot_duration": 8, "max_shot_duration": 60, "preferred_duration": 15 },
    "fast_cut": { "min_shot_duration": 1, "max_shot_duration": 5, "preferred_duration": 2.5 },
    "hybrid": {
      "adaptation_rules": {
        "action_scenes": "fast_cut",
        "emotional_scenes": "long_take",
        "dialogue_scenes": "medium_pacing"
      }
    }
  },
  "scene_overrides": [
    { "scene_id": "scene_003", "override_mode": "fast_cut", "reason": null }
  ],
  "pacing_rules": {
    "allow_scene_override": true,
    "allow_shot_override": true,
    "auto_adapt_to_mood": true
  }
}
```

**نکته:** `allow_shot_override` همیشه `true` است — کنترل دستی در سطح Shot همیشه در دسترس کاربر می‌ماند. `reason` در `scene_overrides` اختیاری است (طبق `docs/governance/override-policy.md`).

### پیاده‌سازی مفهومی (Kotlin)

```kotlin
data class CinematicLanguageSettings(
    val globalMode: CinematicMode,
    val sceneOverrides: Map<String, CinematicMode> = emptyMap()  // sceneId -> override mode
)

/**
 * تعیین حالت مؤثر یک Scene: اول Override محلی، بعد حالت سراسری.
 * کاربر همیشه از طریق Override می‌تواند این را دستی تغییر دهد.
 */
fun resolveEffectiveMode(
    settings: CinematicLanguageSettings,
    sceneId: String
): CinematicMode {
    return settings.sceneOverrides[sceneId] ?: settings.globalMode
}
```

### قوانین

| Rule | شرح | Severity |
|---|---|---|
| ۱ | حالت انتخابی باید یکی از سه مقدار معتبر باشد | **Blocking** |
| ۲ | مدت شات خارج از محدوده‌ی مجاز حالت انتخابی | **Warning** |

---

## تداخل با واحدهای دیگر (از Logic Conflict Checker)

این دو تنظیم می‌توانند با تنظیمات Shot در واحد Camera & Motion تضاد ایجاد کنند — این تضادها همیشه **Warning** هستند، نه Blocking:

- **حرکت سریع + Long-take:** «حرکت سریع با حالت Long-take (که معمولاً آرام است) ناسازگار است.»
- **دوربین ثابت + صحنه‌ی تعقیب:** «صحنه‌ی تعقیب با دوربین ثابت انرژی لازم را ندارد؛ Tracking یا Handheld پیشنهاد می‌شود.»
- **Slow Motion + دیالوگ:** «Slow Motion برای دیالوگ غیرمعمول است، مگر برای جلوه‌ی خاص عمدی.»

---

## معیارهای موفقیت

- سبک اصلی همیشه باید مقداردهی‌شده باشد؛ سبک ثانویه اختیاری است.
- ناسازگاری سبک‌ها فقط هشدار می‌دهد، هرگز بلاک نمی‌کند.
- هر سه حالت Cinematic Language درست کار می‌کنند.
- Hybrid Mode با Beat Sheet و Mood پروژه تطبیق می‌یابد.
- Override در سطح Scene و Shot همیشه در دسترس است، بدون الزام دلیل.
