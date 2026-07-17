# واحد ۰۴: موتور صحنه (Scene Engine)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**وضعیت:** فعال
**وابستگی:** DNA Manager

---

## تعریف

Scene کانتینر منطقی و بصری برای Shot هاست؛ تنظیمات سراسری صحنه را به تمام Shot های زیرمجموعه‌اش منتقل می‌کند.

## معماری

```
DNA Manager
        ↓
Scene (کانتینر)
├─ تنظیمات سراسری Scene
│   ├─ Location, Time of Day, Atmosphere
│   └─ Global Visual Style (ارث‌بری از DNA یا Override محلی)
└─ Shot ها (۱ تا N)
    ├─ Shot 1 (ارث‌بری از Scene)
    └─ Shot 2 (می‌تواند Override کند)
```

---

## ساختار داده

```json
{
  "scene_id": "scene_001",
  "scene_title": "فرار در باران",
  "scene_number": 1,
  "narrative_role": "climax",
  "location": { "type": "outdoor", "description": "خیابان شلوغ شهری، شب" },
  "time_of_day": "night",
  "atmosphere": ["tense"],
  "global_visual_style": { "source": "project_dna", "override": null },
  "scene_constraints": {
    "camera_restrictions": ["no_crane", "handheld_only"],
    "lighting_restrictions": ["low_key", "natural_only"],
    "environment_restrictions": ["rainy_weather"]
  },
  "metadata": { "shot_count": 5, "state": "draft" }
}
```

## فیلدها

| فیلد | مقادیر | الزامی |
|---|---|---|
| `narrative_role` | Introduction, Development, Climax, Resolution, Transition | بله |
| `location.type` | Indoor, Outdoor, Mixed, Custom | بله |
| `time_of_day` | Dawn, Morning, Noon, Afternoon, Sunset, Night | بله |
| `atmosphere` | Calm, Tense, Dark, Bright, Mysterious, Emotional (تا ۲ مورد) | بله |
| `global_visual_style` | ارث‌بری از DNA یا Override محلی | بله |
| `scene_constraints` | محدودسازی گزینه‌های Shot Engine | اختیاری |

---

## پیاده‌سازی مفهومی (Kotlin)

```kotlin
enum class NarrativeRole { INTRODUCTION, DEVELOPMENT, CLIMAX, RESOLUTION, TRANSITION }
enum class LocationType { INDOOR, OUTDOOR, MIXED, CUSTOM }
enum class TimeOfDay { DAWN, MORNING, NOON, AFTERNOON, SUNSET, NIGHT }
enum class Atmosphere { CALM, TENSE, DARK, BRIGHT, MYSTERIOUS, EMOTIONAL }

data class SceneLocation(val type: LocationType, val description: String)

data class SceneConstraints(
    val cameraRestrictions: List<String> = emptyList(),
    val lightingRestrictions: List<String> = emptyList(),
    val environmentRestrictions: List<String> = emptyList()
)

data class Scene(
    val sceneId: String,
    val sceneTitle: String? = null,
    val sceneNumber: Int,
    val narrativeRole: NarrativeRole,
    val location: SceneLocation,
    val timeOfDay: TimeOfDay,
    val atmospherePrimary: Atmosphere,
    val atmosphereSecondary: Atmosphere? = null,
    val constraints: SceneConstraints = SceneConstraints(),
    val shotCount: Int = 0
)

/**
 * هنگام ساخت یک Shot جدید، تنظیمات Scene به آن ارث می‌رسد؛
 * فیلدهای صریح Shot، مقادیر ارثی را Override می‌کنند.
 */
fun <T> inheritOrOverride(sceneValue: T, shotValue: T?): T = shotValue ?: sceneValue
```

---

## قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| ۱ | Scene قبل از Finalize باید حداقل یک Shot داشته باشد | **Blocking** |
| ۲ | Shot بدون اتصال به یک Scene قابل ایجاد نیست | **Blocking** |
| ۳ | تنظیمات Scene به‌طور پیش‌فرض به Shot های زیرمجموعه اعمال می‌شود، مگر Shot صریحاً Override کند | — |
| ۴ | نور ظهر (Noon) در فضای داخلی (Indoor) | **Warning** («ممکن است غیرطبیعی به نظر برسد») |
| ۵ | اتمسفر آرام (Calm) برای صحنه‌ی اوج (Climax) | **Warning** («غیرمعمول است») |

### حذف Scene

```kotlin
/** حذف Scene ای که هنوز Shot دارد مجاز نیست؛ باید اول Shot ها حذف شوند. */
fun deleteScene(scene: Scene, existingShotsCount: Int): Result<Unit> {
    if (existingShotsCount > 0) {
        return Result.failure(
            IllegalStateException("این صحنه دارای $existingShotsCount شات است. ابتدا شات‌ها را حذف کنید.")
        )
    }
    return Result.success(Unit)
}
```

---

## معیارهای موفقیت

- تنظیمات Scene به‌درستی به تمام Shot های زیرمجموعه ارث می‌رسد.
- Shot می‌تواند هر بخشی از تنظیمات ارثی را دستی Override کند.
- هیچ Shot ای بدون Scene والد قابل ایجاد نیست.
- حذف Scene دارای Shot مسدود می‌شود.
