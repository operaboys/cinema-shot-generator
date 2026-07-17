# واحد ۰۲: مدیر DNA پروژه (DNA Manager)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**وضعیت:** فعال
**وابستگی:** Story & Override (ورودی اولیه از StoryContext)

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
├─ Global Mood Base (حس کلی پروژه)
├─ Style Preferences (قابل Override در سطح Shot)
│   ├─ Cinematic Language
│   ├─ Color Philosophy
│   └─ Camera/Lighting Preferences
└─ Output Constraints
    ├─ Forbidden Elements (Blocking)
    ├─ Mandatory Elements (Warning)
    └─ Technical Constraints (Blocking)
```

---

## Soft Lock — اصل کلیدی این واحد

Core Identity (سبک بصری غالب، سطح رئالیسم) **همیشه قابل تغییر است**، حتی پس از "قفل شدن". اگر تغییر با Scene/Shot موجود ناسازگار باشد، سیستم فقط **هشدار** می‌دهد؛ هرگز بلاک نمی‌کند. کاربر تصمیم‌گیرنده‌ی نهایی است.

این با **Character Continuity Lock** (در واحد Asset & Continuity، بخش Hard Lock) کاملاً متفاوت است — آنجا Lock مطلق است چون هدف تضمین کیفی بنیادی (ثبات ظاهری) است؛ اینجا DNA یک چارچوب سبکی قابل‌تجدیدنظر است.

---

## ساختار داده

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
    "color_grading_preset": "natural"
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
  "override_rules": {
    "allow_scene_override": true,
    "allow_shot_override": true,
    "requires_human_approval": true
  }
}
```

---

## پیاده‌سازی مفهومی (Kotlin)

```kotlin
enum class VisualStyle { REALISTIC, STYLIZED, CINEMATIC, HYBRID }
enum class RealismLevel { GROUNDED, SEMI_REALISTIC, FANTASTICAL }
enum class StyleConsistency { STRICT, MODERATE, FLEXIBLE }
enum class ColorTemperature { WARM, COOL, NEUTRAL }
enum class SaturationLevel { LOW, MEDIUM, HIGH, VERY_HIGH }

data class CoreIdentity(
    val dominantVisualStyle: VisualStyle,
    val realismLevel: RealismLevel,
    val styleConsistency: StyleConsistency,
    val locked: Boolean = false
)

data class MasterPalette(
    val colorTemperature: ColorTemperature,
    val globalSaturation: SaturationLevel,
    val globalContrast: SaturationLevel,
    val colorGradingPreset: String
)

data class OutputConstraints(
    val forbiddenElements: Map<String, List<String>>,   // مثلاً "camera" -> ["dutch_angle"]
    val mandatoryElements: List<String>,
    val maxShotDurationSeconds: Int,
    val aspectRatio: String
)

data class ProjectDna(
    val dnaId: String,
    val projectId: String,
    val coreIdentity: CoreIdentity,
    val masterPalette: MasterPalette,
    val outputConstraints: OutputConstraints
    // ... globalMoodBase, stylePreferences مشابه
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

## قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| ۱ | تغییر Core Identity در صورت ناسازگاری با Shot موجود | **Warning** (هرگز Blocking) |
| ۲ | استفاده از عنصر در `forbidden_elements` | **Blocking** |
| ۳ | عدم وجود عنصر در `mandatory_elements` هنگام Finalize شات | **Warning** |
| ۴ | نقض `technical_constraints` (مثل تجاوز از `max_shot_duration`) | **Blocking** |
| ۵ | Override DNA وقتی `requires_human_approval = true` | نمایش تأیید + ثبت خودکار Scope |

---

## اثر روی Prompt نهایی (نمونه)

```
DNA:
- Visual Style: Cinematic, Grounded Realism
- Palette: Warm, Medium Saturation, High Contrast
- Mood: Mysterious, Strong Intensity
- Cinematic Language: Long-take

اثر در structured_parts (بخشی از PromptBlueprint):
style_modifiers: "cinematic style, grounded realism, warm color temperature,
medium saturated tones, high contrast lighting, mysterious atmosphere,
strong intensity, continuous long-take shot, atmospheric depth,
avoid dutch angles"
```

---

## معیارهای موفقیت

- DNA به تمام Scene و Shot اعمال می‌شود.
- Forbidden Elements همیشه مسدود می‌شوند.
- Mandatory Elements به‌صورت Warning یادآوری می‌شوند.
- تغییر Core Identity هرگز کاربر را بلاک نمی‌کند، فقط هشدار می‌دهد.
- Override با ثبت خودکار Scope قابل کنترل و ردیابی است.
