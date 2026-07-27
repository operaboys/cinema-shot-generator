# واحد ۰۸: شرایط صحنه (Scene Conditions System)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**ادغام از:** Lighting System + Environment & Weather Engine + Beat Sheet (منطق زمان‌بندی)
**وضعیت:** فعال
**وابستگی:** Shot Engine، DNA Manager (برای `LightingStyle` enum)

**نسخه:** ۲ — بازنویسی برای هم‌راستاسازی با Migration بزرگ بلوپرینت `02-dna-manager-v2.md` (نسخه ۳): پارامترهای Lighting که تا این نسخه فقط جدول متنی بودند (نه enum) اکنون از `LightingStyle` (تعریف‌شده در بلوپرینت ۰۲) استفاده می‌کنند؛ `mapMoodToLighting` دیگر رشته‌ی خام Mood نمی‌گیرد، بلکه enum کامل `Mood` را می‌پذیرد. **تغییرات با «🆕v2» علامت‌گذاری شده‌اند.**

نور، آب‌وهوا و زمان‌بندی با هم «شرایط لحظه‌ای صحنه» را می‌سازند و به‌شدت به‌هم وابسته‌اند (Mood-to-Lighting، Environment-to-Sound). چون خروجی این پروژه فقط پرامپت متنی است، هیچ محدودیت پردازشی برای عمق کامل این واحد وجود ندارد.

---

## بخش الف — Lighting System (نورپردازی)

### پارامترها

🆕v2 **نکته:** ستون «گزینه‌ها» زیر به‌عنوان توصیف مفهومی باقی مانده، اما `style` اکنون باید از enum کامل `LightingStyle` (بلوپرینت ۰۲، ۲۵+ مقدار در ۴ دسته: Natural/Night/Studio/Special) بیاید، نه این فهرست کوچک‌تر و کلی. بقیه‌ی پارامترها (`key_light_position` و مشابه) هنوز رشته‌ی ساده باقی می‌مانند چون تعداد گزینه‌هایشان کم و پایدار است — فقط `style` (که مستقیماً معادل `LightingStyle` سراسری DNA است) به enum مشترک Migrate شد.

| پارامتر | گزینه‌ها | پیش‌فرض |
|---|---|---|
| `style` | 🆕v2 از `LightingStyle` (بلوپرینت ۰۲) — مثلاً `GOLDEN_HOUR`, `DRAMATIC_LIGHT`, `NEON`, ... | `DRAMATIC_LIGHT` |
| `key_light_position` | Front, Side, Back, Top, Bottom | Side |
| `fill_light` | None, Soft, Strong | Soft |
| `rim_light` | true/false | true |
| `light_source_count` | Single, Dual, Multi | Dual |
| `contrast_ratio` | Low, Medium, High | Medium |
| `color_temperature` | Warm, Neutral, Cold, Mixed | Neutral |
| `shadow_quality` | Soft Shadows, Hard Shadows | Soft Shadows |
| `lighting_motivation` | Sunlight, Artificial, Moonlight, Fire, Practical, Mixed | Artificial |

### Mood-to-Lighting Mapping (پیش‌فرض هوشمند)

```kotlin
// 🆕v2 style اکنون از نوع LightingStyle واقعی (import از domain.dna)، نه String
data class LightingPreset(
    val style: LightingStyle, val keyLightPosition: String, val fillLight: String,
    val contrastRatio: String, val shadowQuality: String, val colorTemperature: String
)

/**
 * 🆕v2 نگاشت خودکار از Mood پروژه/صحنه به یک پیش‌فرض نورپردازی — قابل Override دستی.
 * پارامتر اکنون از نوع Mood واقعی (import از domain.dna) است، نه رشته‌ی خام؛
 * نگاشت بر اساس MoodCategory انجام می‌شود تا تمام ۲۵+ مقدار Mood پوشش داده شوند،
 * نه فقط ۶ مقدار خاصی که نسخه‌ی قبلی می‌شناخت.
 */
fun mapMoodToLighting(mood: Mood): LightingPreset = when (mood.category) {
    MoodCategory.DARK -> LightingPreset(LightingStyle.DRAMATIC_LIGHT, "side", "none", "high", "hard_shadows", "cold")
    MoodCategory.CALM -> LightingPreset(LightingStyle.SOFT_LIGHT, "front", "strong", "low", "soft_shadows", "warm")
    MoodCategory.HIGH_ENERGY -> LightingPreset(LightingStyle.HIGH_KEY, "side", "soft", "medium", "soft_shadows", "warm")
    MoodCategory.POSITIVE -> LightingPreset(LightingStyle.GOLDEN_HOUR, "side", "soft", "medium", "soft_shadows", "warm")
    MoodCategory.EMOTIONAL -> LightingPreset(LightingStyle.SOFT_LIGHT, "side", "soft", "medium", "soft_shadows", "warm")
}
```

### قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| نور خورشید (`sunlight`) + شب (`night`) | ناسازگاری فیزیکی مطلق | **Blocking** |
| نور ماه (`moonlight`) + ظهر (`noon`) | ناسازگاری فیزیکی مطلق | **Blocking** |
| سبک Noir + Contrast غیر از High | ناسازگاری سبکی | **Warning** |
| Fill Light قوی + سبک Dramatic/Noir | تضعیف کنتراست دراماتیک مدنظر | **Warning** |
| نور از پایین (`bottom`) | غیرطبیعی، فقط برای هورور مناسب | **Warning** |

---

## بخش ب — Environment & Weather Engine (محیط و آب‌وهوا)

### پارامترها

| پارامتر | گزینه‌ها | پیش‌فرض |
|---|---|---|
| `weather.type` | Clear, Rain, Storm, Snow, Fog | — |
| `weather.intensity` | Light, Medium, Heavy | — |
| `wind` | None, Light, Strong | None |
| `environmental_motion` | falling_rain, blowing_leaves, snowfall, dust_clouds, flying_debris (۰ تا ۳) | [] |
| `ground_state` | Dry, Wet, Muddy, Snow-covered, Sandy, Icy | Dry |
| `visibility` | Clear, Reduced, Low | Clear |
| `temperature_feel` | Hot, Mild, Cold | Mild |

### قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| مه (`fog`) + دید واضح (`clear`) | ناسازگاری فیزیکی | **Blocking** |
| `falling_rain` در `environmental_motion` بدون آب‌وهوای بارانی | ناسازگاری ساختاری | **Blocking** |
| `snowfall` بدون آب‌وهوای برفی | ناسازگاری ساختاری | **Blocking** |
| باران + زمین خشک | ناسازگاری منطقی | **Warning** |
| طوفان + بدون باد | ناسازگاری منطقی | **Warning** |
| برف + زمین غیر برف‌پوش/یخ‌زده | ناسازگاری منطقی | **Warning** |
| Extreme Wide + دید کم | بی‌معنی بودن ترکیب | **Warning** |
| آتش + باران + فضای بیرونی | ناسازگاری منطقی (مشترک با واحد ۰۷) | **Warning** |

### Environment-to-Sound Mapping (پیش‌فرض هوشمند برای Ambient)

```kotlin
data class AmbientSoundSuggestion(val type: String, val intensity: String, val description: String)

/**
 * تولید خودکار صدای محیطی از شرایط آب‌وهوا — این خروجی مستقیماً به
 * ambient_sounds در Sound Profile (واحد ۰۵) می‌رود، چون Ambient
 * همیشه خودکار است (برخلاف Character Sound که همیشه دستی می‌ماند).
 */
fun mapEnvironmentToSound(weatherType: String, weatherIntensity: String, windStrength: String): List<AmbientSoundSuggestion> {
    val sounds = mutableListOf<AmbientSoundSuggestion>()
    when (weatherType) {
        "rain" -> sounds += AmbientSoundSuggestion("rain", weatherIntensity, "$weatherIntensity rain on surfaces")
        "storm" -> {
            sounds += AmbientSoundSuggestion("rain", "heavy", "torrential rain and wind")
            sounds += AmbientSoundSuggestion("thunder", "high", "loud thunder cracks")
        }
        "snow" -> sounds += AmbientSoundSuggestion("wind", "low", "gentle wind through snow")
        "fog" -> sounds += AmbientSoundSuggestion("ambient", "low", "eerie silence with muffled sounds")
    }
    if (windStrength != "none") {
        val intensity = if (windStrength == "strong") "high" else "medium"
        sounds += AmbientSoundSuggestion("wind", intensity, "$windStrength wind blowing")
    }
    return sounds
}
```

---

## بخش ج — Beat Sheet (زمان‌بندی) — ارجاع

ساختار کامل Beat Sheet (زمان‌بندی ثانیه‌به‌ثانیه‌ی رویدادها داخل یک Shot) در **واحد ۰۵ (Shot Engine)** تعریف شده، چون Beat مستقیماً به یک Shot خاص و Duration آن وابسته است. Beat می‌تواند به تغییرات نور یا محیط در طول یک شات هم اشاره کند (`event_type: "lighting_change"` یا `"environmental"`) — این‌جا فقط یادآوری می‌شود که این تغییرات باید با شرایط تعریف‌شده در این واحد سازگار باشند.

---

## ترکیب در Prompt نهایی (نمونه)

```
Lighting: Dramatic side lighting with soft fill, rim light enabled, high contrast
noir style, warm color temperature, hard shadows

Environment: Heavy rain storm with strong wind, falling rain visible, wet ground
with puddles, reduced visibility, cold atmosphere, autumn season feel
```

---

## معیارهای موفقیت

- نورپردازی با Time of Day سازگار است (ترکیبات فیزیکاً غیرممکن Blocking هستند).
- محیط با آب‌وهوا سازگار است (ترکیبات ساختاری غیرممکن Blocking، ترکیبات نامعمول Warning).
- Mood-to-Lighting Mapping پیش‌فرض هوشمند و قابل Override می‌دهد.
- Environment-to-Sound Mapping به‌درستی فقط Ambient Sound تولید می‌کند (نه Character Sound).

---

## یادداشت پیاده‌سازی (برای Claude Code، هنگام اجرای این بلوپرینت به‌روزشده)

این Migration باید **بعد از** Migration بلوپرینت ۰۲ (نسخه ۳) اجرا شود، چون به `Mood`/`MoodCategory`/`LightingStyle` از آن‌جا وابسته است — همان ترتیبی که برای بلوپرینت ۰۳ هم لازم بود:

- `mapMoodToLighting`/`LightingPreset` باید `import` واقعی `Mood`/`MoodCategory`/`LightingStyle` از `domain.dna` داشته باشند.
- ⚠️ **تغییر امضای واقعی، نه فقط نوع پارامتر:** خروجی از `LightingPreset?` (nullable) به `LightingPreset` (غیر-nullable) تغییر کرد، چون نگاشت جدید بر اساس ۵ دسته‌ی `MoodCategory` تمام مقادیر ممکن `Mood` را پوشش می‌دهد (`when` Exhaustive، بدون نیاز به `else -> null`). هر جای کد که فراخوانی این تابع را با فرض نتیجه‌ی nullable مدیریت می‌کرد (مثلاً `?.let { ... }` یا بررسی `!= null`)، باید بازبینی شود — این کد دیگر لازم نیست، اما حذفش باید آگاهانه و با بررسی باشد، نه فرض شود که خودکار درست کار می‌کند.
- مقادیر دقیق `LightingStyle` انتخاب‌شده برای هر `MoodCategory` (`GOLDEN_HOUR` برای Positive، `HIGH_KEY` برای High Energy، و غیره) پیشنهادهای این بازنویسی‌اند، نه یک تصمیم غیرقابل‌تغییر — اگر حین پیاده‌سازی منطق بهتری به ذهنت رسید، با دلیل در گزارش/ADR مستند کن و اعمال کن.
- اگر تست‌های موجود این تابع روی ورودی‌های رشته‌ای (`"tense"`, ...) یا خروجی nullable نوشته شده‌اند، باید کامل بازنویسی شوند، نه صرفاً حذف.
