# واحد ۰۵: موتور شات (Shot Engine)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**وضعیت:** فعال
**وابستگی:** Scene Engine، Asset & Continuity System، Camera & Motion (واحد ۰۹)

---

## تعریف

Shot کوچک‌ترین واحد اجرایی پروژه است — یک واحد سینمایی مستقل با تنظیمات دقیق که می‌تواند تنظیمات Scene را ارث ببرد یا Override کند.

## معماری

```
Scene
        ↓
Shot (کوچک‌ترین واحد)
├─ Shot Settings (Description, Goal, Type, Duration, Motion Level)
├─ Beat Sheet (زمان‌بندی دقیق ثانیه‌به‌ثانیه)
├─ Image References (رفرنس فایل محلی)
├─ Camera → واحد ۰۹ (Camera & Motion)
├─ Lighting/Environment → واحد ۰۸ (Scene Conditions)
├─ Sound Profile (Ambient خودکار / Character دستی)
└─ Subjects (Characters, Objects, Locations از Asset & Continuity)
```

چون خروجی این پروژه فقط پرامپت متنی است (نه رندر رسانه‌ای)، هیچ محدودیت پردازشی برای عمق این واحد وجود ندارد — تمام فیلدها با جزئیات کامل حفظ می‌شوند.

---

## ساختار داده (کامل)

```json
{
  "shot_id": "shot_001",
  "scene_id": "scene_001",
  "shot_number": 1,
  "shot_title": "ورود قهرمان",
  "shot_description": "مرد جوان با کت مشکی وارد خیابان باران‌زده می‌شود",
  "shot_goal": "establishing",
  "shot_type": "wide",
  "duration": { "value": 4, "unit": "seconds" },
  "motion_level": "moderate",

  "beat_sheet": {
    "enabled": true,
    "beats": [
      { "timestamp": 0.0, "event_type": "camera_move", "description": "شروع dolly in", "camera_action": "dolly_in_start" },
      { "timestamp": 1.5, "event_type": "subject_action", "description": "قدم اول", "subject_id": "char_001", "action": "step_forward" },
      { "timestamp": 3.0, "event_type": "environmental", "description": "رعد و برق", "effect": "lightning_flash" }
    ]
  },

  "image_references": {
    "enabled": true,
    "references": [
      { "type": "character", "local_file_path": "/storage/project_001/assets/char_001_ref.jpg", "description": "رفرنس ظاهر کاراکتر" }
    ]
  },

  "camera": { "source": "override", "settings": { "...": "جزئیات کامل در واحد ۰۹" } },
  "lighting": { "source": "scene", "settings": { "...": "جزئیات کامل در واحد ۰۸" } },
  "environment": { "source": "scene", "settings": { "...": "جزئیات کامل در واحد ۰۸" } },

  "sound_profile": {
    "enabled": true,
    "ambient_auto_generate": true,
    "ambient_sounds": [{ "type": "rain", "intensity": "heavy", "description": "باران شدید" }],
    "action_sounds": [{ "timestamp": 1.5, "type": "footstep", "description": "قدم روی زمین خیس" }],
    "character_sounds": [{ "character_id": "char_001", "type": "breathing", "description": "نفس سنگین (کاربر تعریف کرده)" }]
  },

  "subjects": { "characters": ["char_001"], "objects": ["obj_005"], "locations": ["loc_002"] },
  "override_scene": false
}
```

---

## پیاده‌سازی مفهومی (Kotlin)

```kotlin
enum class ShotGoal { ESTABLISHING, ACTION, EMOTIONAL, DIALOGUE, TRANSITION }
enum class ShotType { EXTREME_WIDE, WIDE, MEDIUM, CLOSE_UP, EXTREME_CLOSE_UP }
enum class MotionLevel { STATIC, SUBTLE, MODERATE, DYNAMIC, EXTREME }
enum class BeatEventType { CAMERA_MOVE, SUBJECT_ACTION, ENVIRONMENTAL, LIGHTING_CHANGE }

data class Beat(
    val timestampSeconds: Float,
    val eventType: BeatEventType,
    val description: String,
    val subjectId: String? = null
)

data class ImageReference(
    val type: String,           // "character" | "style" | "composition" | "lighting"
    val localFilePath: String,  // فقط فایل محلی — بدون URL خارجی
    val description: String
)

data class AmbientSound(val type: String, val intensity: String, val description: String)
data class ActionSound(val timestampSeconds: Float, val type: String, val description: String)

/** صدای کاراکتر همیشه source="user_defined" است؛ هرگز خودکار تولید نمی‌شود. */
data class CharacterSound(val characterId: String, val type: String, val description: String)

data class SoundProfile(
    val enabled: Boolean,
    val ambientAutoGenerate: Boolean = true,
    val ambientSounds: List<AmbientSound> = emptyList(),
    val actionSounds: List<ActionSound> = emptyList(),
    val characterSounds: List<CharacterSound> = emptyList()  // همیشه دستی
)

data class Shot(
    val shotId: String,
    val sceneId: String,
    val shotNumber: Int,
    val shotDescription: String,   // الزامی
    val shotGoal: ShotGoal,
    val shotType: ShotType,
    val durationSeconds: Float,
    val motionLevel: MotionLevel,
    val beats: List<Beat> = emptyList(),
    val imageReferences: List<ImageReference> = emptyList(),
    val soundProfile: SoundProfile,
    val characterIds: List<String>,   // حداقل یکی الزامی
    val objectIds: List<String> = emptyList(),
    val locationIds: List<String> = emptyList()
)

/**
 * انتخاب Outfit/Expression یک کاراکتر در این شات: خودکار از شرایط Scene
 * استنباط می‌شود (طبق condition در Asset)، مگر کاربر دستی Override کرده باشد.
 */
fun selectOutfitForShot(
    character: CharacterAsset,
    sceneWeather: String?,
    manualOverrideOutfitId: String?
): String {
    if (manualOverrideOutfitId != null) return manualOverrideOutfitId
    val matched = character.outfits.firstOrNull { it.condition?.weather == sceneWeather }
    return matched?.id ?: character.outfits.first { it.isDefault }.id
}
```

---

## قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| ۱ | `shot_description` الزامی (حداقل ۱۰ کاراکتر) | **Blocking** |
| ۲ | حداقل یک Subject (Character/Object/Location) متصل باشد | **Blocking** |
| ۳ | Beat timestamp باید بین ۰ و `duration` شات باشد | **Blocking** |
| ۴ | فایل `image_references[].local_file_path` باید در دستگاه موجود باشد | **Blocking** |
| ۵ | `sound_profile.character_sounds` هرگز خودکار پر نمی‌شود؛ فقط از طریق ورودی صریح کاربر | ساختاری (Enforcement در کد، نه Validation runtime) |
| ۶ | ترکیب لنز نامناسب با فاصله‌ی دوربین (جزئیات در واحد ۰۹) | **Warning** |
| ۷ | ناسازگاری آب‌وهوا و نور (جزئیات در واحد ۰۸) | **Warning** |

---

## ارث‌بری و Override

هر فیلد Camera/Lighting/Environment یک `source` دارد: `"scene"` (ارث‌بری مستقیم) یا `"override"` (تنظیمات مخصوص همین Shot). اگر `override_scene = false`، تغییر بعدی Scene به‌طور خودکار در این Shot منعکس می‌شود؛ در غیر این صورت، Shot مستقل از تغییرات بعدی Scene باقی می‌ماند.

---

## معیارهای موفقیت

- Shot تنظیمات Scene را به‌درستی ارث می‌برد یا Override می‌کند.
- Beat Sheet timeline کاملاً معتبر است (بدون Beat خارج از محدوده‌ی Duration).
- Image Reference همیشه به فایل محلی موجود اشاره می‌کند.
- Sound Profile به‌درستی Ambient (خودکار) را از Character (دستی) تفکیک می‌کند.
- تغییر Subject، Dependency Resolver را فعال می‌کند تا اثرات جانبی بررسی شود.
