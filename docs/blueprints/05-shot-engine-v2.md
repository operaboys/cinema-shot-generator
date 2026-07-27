# واحد ۰۵: موتور شات (Shot Engine)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**وضعیت:** فعال
**وابستگی:** Scene Engine، Asset & Continuity System، Camera & Motion (واحد ۰۹)، DNA Manager (واحد ۰۲)

**نسخه:** ۳ — بازنویسی برای رفع تناقضات بحرانی کشف‌شده در یک بازبینی معماری مستقل (خارج از این چت): (۱) `SourcedSettings<T>` که در بلوپرینت ۱۱ به آن ارجاع داده می‌شد، هرگز در این‌جا به‌عنوان یک `data class` واقعی تعریف نشده بود. (۲) `data class Shot` فیلدهای `camera`/`lighting`/`environment` را اصلاً نداشت، با اینکه در ساختار JSON و دیاگرام معماری حاضر بودند. (۳) `LightingSettings`/`EnvironmentSettings` (type هایی که واحد ۱۱ به آن‌ها ارجاع می‌داد) در هیچ بلوپرینتی (نه اینجا، نه ۰۸) تعریف نشده بودند — این نسخه آن‌ها را این‌جا تعریف می‌کند، چون `Shot` مصرف‌کننده‌ی مستقیم آن‌هاست. **تغییرات با «🆕v3» علامت‌گذاری شده‌اند.**

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
├─ 🆕 Negative Prompt (پیش‌فرض از DNA، قابل Override در این سطح)
└─ Subjects (Characters, Objects, Locations از Asset & Continuity)
```

چون خروجی این پروژه فقط پرامپت متنی است (نه رندر رسانه‌ای)، هیچ محدودیت پردازشی برای عمق این واحد وجود ندارد — تمام فیلدها با جزئیات کامل حفظ می‌شوند.

---

## 🆕 چرا `negativePromptOverride` لازم بود

بلوپرینت ۰۲ (DNA Manager، نسخه‌ی بازنویسی‌شده) یک `negativePrompt` سراسری در سطح پروژه تعریف می‌کند (مثل `"blurry, low quality, distorted, watermark, text"`) که برای همه‌ی Shot ها اعمال می‌شود. اما در تحلیل نمونه‌ی کارکردی مشخص شد که هنگام ساخت هر Shot جدید، این مقدار **به‌طور خودکار داخل خودِ Shot کپی می‌شود** — یعنی طراحی اصلی از ابتدا این امکان را در نظر داشته که کاربر بعداً بتواند این مقدار را **فقط برای یک Shot خاص** تغییر دهد (مثلاً یک Shot که عمداً می‌خواهد افکت "blur" داشته باشد، نباید از negative prompt سراسری که "blurry" را ممنوع می‌کند، آسیب ببیند).

این دقیقاً همان الگوی «ارث‌بری با امکان Override» است که این بلوپرینت از قبل برای Camera/Lighting/Environment داشت (بخش «ارث‌بری و Override» پایین‌تر) — فقط این‌بار منبع ارث‌بری Scene نیست، بلکه DNA پروژه است.

---

## ساختار داده (کامل، به‌روزشده)

```json
{
  "shot_id": "shot_001",
  "scene_id": "scene_001",
  "shot_number": 1,
  "shot_title": "ورود قهرمان",
  "shot_description": "مرد جوان با کت مشکی وارد خیابان باران‌زده می‌شود",
  "shot_goal": "establishing",
  "shot_type": "wide",
  "duration_seconds": 4,
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

  "negative_prompt_override": null,

  "subjects": { "characters": ["char_001"], "objects": ["obj_005"], "locations": ["loc_002"] },
  "override_scene": false
}
```

🆕 **فیلد جدید: `negative_prompt_override`** — پیش‌فرض `null` (یعنی از DNA پروژه ارث می‌برد). اگر کاربر دستی مقداری تنظیم کند، همان مقدار برای این Shot خاص استفاده می‌شود، مستقل از تغییرات بعدی `negativePrompt` سطح DNA.

---

## پیاده‌سازی مفهومی (Kotlin) — نسخه‌ی به‌روزشده

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

/**
 * 🆕v3 تعریف رسمی — این نوع تا این نسخه فقط در متن نثر بلوپرینت ۱۱ ("Shot.camera از این نوع است")
 * ارجاع داده می‌شد اما هیچ‌جا (نه اینجا، نه ۱۱) به‌عنوان data class واقعی تعریف نشده بود.
 * Wrapper عمومی برای هر تنظیمی که می‌تواند از Scene ارث برده شود یا در سطح Shot Override شود.
 */
enum class SettingsSource { SCENE, OVERRIDE }

data class SourcedSettings<T>(
    val source: SettingsSource,
    val settings: T   // مقدار واقعی وقتی source=OVERRIDE؛ وقتی source=SCENE، مقدار از Scene خوانده می‌شود نه از اینجا
)

/**
 * 🆕v3 تعریف رسمی — این نوع در واحد ۱۱ (`PromptGenerationInput.lighting`) و واحد ۰۷
 * (`validateLogicConsistency`) مصرف می‌شود، اما تا این نسخه در هیچ بلوپرینتی (نه اینجا نه ۰۸)
 * تعریف نشده بود. چون Shot مصرف‌کننده‌ی مستقیم این نوع است، تعریفش این‌جا قرار می‌گیرد؛
 * `LightingPreset` (بلوپرینت ۰۸) پیش‌فرض پیشنهادی/محاسبه‌شده است، این‌جا مقدار نهایی Resolve‌شده است —
 * تفاوت دقیقاً مثل تفاوت SourcedSettings.settings (نهایی) در برابر یک پیش‌فرض قابل‌جایگزینی.
 */
data class LightingSettings(
    val style: LightingStyle,           // import از domain.dna (بلوپرینت ۰۲)
    val keyLightPosition: String,
    val fillLight: String,
    val contrastRatio: String,
    val shadowQuality: String,
    val colorTemperature: String,
    val motivation: String = "artificial"   // 🆕v3 فیلدی که واحد ۰۷ (validateLogicConsistency) به آن ارجاع می‌داد اما در LightingPreset (۰۸) هم نبود؛ اینجا اضافه شد
)

/**
 * 🆕v3 تعریف رسمی — مشابه LightingSettings، در واحد ۱۱ و ۰۷ مصرف می‌شد اما تعریف نداشت.
 * فیلدهای weatherType/locationType/weather که واحد ۰۷ به آن‌ها ارجاع می‌داد، اینجا گنجانده شدند.
 */
data class EnvironmentSettings(
    val weatherType: String,       // "clear" | "rain" | "storm" | "snow" | "fog" (طبق بلوپرینت ۰۸)
    val weatherIntensity: String? = null,
    val locationType: String,      // "indoor" | "outdoor" | "mixed" — فیلدی که ۰۷ به آن ارجاع می‌داد
    val groundState: String = "dry",
    val visibility: String = "clear",
    val temperatureFeel: String = "mild"
)

data class ImageReference(
    val type: String,           // "character" | "style" | "composition" | "lighting"
    val localFilePath: String,  // فقط فایل محلی — بدون URL خارجی
    val description: String
)

fun ReferenceImage.toImageReference(type: String): ImageReference =
    ImageReference(type = type, localFilePath = this.localFilePath, description = this.description)

data class AmbientSound(val type: String, val intensity: String, val description: String, val source: String = "auto_generated")   // 🆕v3 فیلد source اضافه شد تا با AmbientSound واحد ۱۰ یکسان شود (طبق رفع هم‌نامی)
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
    val shotTitle: String? = null,   // 🆕v3 فیلدی که در JSON نمونه بود اما از data class جا افتاده بود
    val shotDescription: String,   // الزامی
    val shotGoal: ShotGoal,
    val shotType: ShotType,
    val durationSeconds: Float,
    val motionLevel: MotionLevel,
    val beats: List<Beat> = emptyList(),
    val imageReferences: List<ImageReference> = emptyList(),
    // 🆕v3 سه فیلد کاملاً جدید — تا این نسخه اصلاً در data class Shot نبودند،
    // با اینکه در JSON نمونه و دیاگرام معماری این بلوپرینت حضور داشتند.
    // مقدار پیش‌فرض null یعنی «هنوز Resolve نشده»؛ توابع resolve*Settings مقدار نهایی را می‌سازند.
    val camera: SourcedSettings<CameraSettings>? = null,
    val lighting: SourcedSettings<LightingSettings>? = null,
    val environment: SourcedSettings<EnvironmentSettings>? = null,
    val soundProfile: SoundProfile,
    val negativePromptOverride: String? = null,   // 🆕 null یعنی از DNA ارث می‌برد
    val characterIds: List<String>,   // حداقل یکی الزامی
    val objectIds: List<String> = emptyList(),
    val locationIds: List<String> = emptyList()
)

/**
 * 🆕 حل نهایی negative prompt مؤثر این Shot: اگر Override دستی وجود دارد
 * (حتی رشته‌ی خالی، برای غیرفعال‌کردن کامل negative prompt در این Shot)، همان استفاده می‌شود؛
 * وگرنه مقدار سطح DNA پروژه.
 */
fun resolveNegativePrompt(shot: Shot, dnaNegativePrompt: String): String {
    return shot.negativePromptOverride ?: dnaNegativePrompt
}

/**
 * 🆕v3 سه تابع resolve — این‌ها در بلوپرینت ۱۱ ("توابع resolve* بلوپرینت ۰۵") ارجاع داده می‌شدند
 * اما تا این نسخه هیچ‌کدام واقعاً تعریف نشده بودند. الگو: اگر source=OVERRIDE، مقدار settings همین Shot
 * استفاده می‌شود؛ اگر source=SCENE، مقدار از sceneDefault گرفته می‌شود (چون Scene خودش فیلد
 * مستقیم camera/lighting/environment ندارد، این پیش‌فرض باید از بیرون — مثلاً از تنظیمات DNA یا یک
 * پیش‌فرض سراسری — تزریق شود؛ Result.failure اگر source=SCENE ولی sceneDefault در دسترس نباشد).
 */
fun resolveCameraSettings(shot: Shot, sceneDefault: CameraSettings?): Result<CameraSettings> {
    val sourced = shot.camera ?: return Result.failure(IllegalStateException("Shot.camera هنوز تنظیم نشده"))
    return when (sourced.source) {
        SettingsSource.OVERRIDE -> Result.success(sourced.settings)
        SettingsSource.SCENE -> sceneDefault?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("source=SCENE ولی sceneDefault در دسترس نیست"))
    }
}

fun resolveLightingSettings(shot: Shot, sceneDefault: LightingSettings?): Result<LightingSettings> {
    val sourced = shot.lighting ?: return Result.failure(IllegalStateException("Shot.lighting هنوز تنظیم نشده"))
    return when (sourced.source) {
        SettingsSource.OVERRIDE -> Result.success(sourced.settings)
        SettingsSource.SCENE -> sceneDefault?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("source=SCENE ولی sceneDefault در دسترس نیست"))
    }
}

fun resolveEnvironmentSettings(shot: Shot, sceneDefault: EnvironmentSettings?): Result<EnvironmentSettings> {
    val sourced = shot.environment ?: return Result.failure(IllegalStateException("Shot.environment هنوز تنظیم نشده"))
    return when (sourced.source) {
        SettingsSource.OVERRIDE -> Result.success(sourced.settings)
        SettingsSource.SCENE -> sceneDefault?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("source=SCENE ولی sceneDefault در دسترس نیست"))
    }
}

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

## قوانین اعتبارسنجی (به‌روزشده)

| Rule | شرح | Severity |
|---|---|---|
| ۱ | `shot_description` الزامی (حداقل ۱۰ کاراکتر) | **Blocking** |
| ۲ | حداقل یک Subject (Character/Object/Location) متصل باشد | **Blocking** |
| ۳ | Beat timestamp باید بین ۰ و `duration` شات باشد | **Blocking** |
| ۴ | فایل `image_references[].local_file_path` باید در دستگاه موجود باشد | **Blocking** |
| ۵ | `sound_profile.character_sounds` هرگز خودکار پر نمی‌شود؛ فقط از طریق ورودی صریح کاربر | ساختاری (Enforcement در کد، نه Validation runtime) |
| ۶ | ترکیب لنز نامناسب با فاصله‌ی دوربین (جزئیات در واحد ۰۹) | **Warning** |
| ۷ | ناسازگاری آب‌وهوا و نور (جزئیات در واحد ۰۸) | **Warning** |
| 🆕 ۸ | `negative_prompt_override` (در صورت غیر-null بودن) رشته‌ای معقول است (بدون محدودیت طول سخت‌گیرانه، فقط نباید کاملاً بی‌معنی/whitespace-only باشد) | **Warning** |

---

## ارث‌بری و Override (به‌روزشده)

هر فیلد Camera/Lighting/Environment یک `source` دارد: `"scene"` (ارث‌بری مستقیم) یا `"override"` (تنظیمات مخصوص همین Shot). اگر `override_scene = false`، تغییر بعدی Scene به‌طور خودکار در این Shot منعکس می‌شود؛ در غیر این صورت، Shot مستقل از تغییرات بعدی Scene باقی می‌ماند.

🆕 **الگوی مشابه برای Negative Prompt، اما با منبع متفاوت:** برخلاف Camera/Lighting/Environment (که منبع ارث‌بری‌شان Scene است)، منبع ارث‌بری Negative Prompt **DNA پروژه** است، نه Scene — چون Negative Prompt یک مفهوم سراسری پروژه است (طبق بلوپرینت ۰۲)، نه یک تنظیم مخصوص یک صحنه‌ی خاص. `resolveNegativePrompt` مشابه الگوی `inheritOrOverride` (واحد ۰۴) عمل می‌کند اما مستقیماً بین Shot و DNA، بدون واسطه‌ی Scene.

---

## معیارهای موفقیت (به‌روزشده)

- Shot تنظیمات Scene را به‌درستی ارث می‌برد یا Override می‌کند.
- Beat Sheet timeline کاملاً معتبر است (بدون Beat خارج از محدوده‌ی Duration).
- Image Reference همیشه به فایل محلی موجود اشاره می‌کند.
- Sound Profile به‌درستی Ambient (خودکار) را از Character (دستی) تفکیک می‌کند.
- تغییر Subject، Dependency Resolver را فعال می‌کند تا اثرات جانبی بررسی شود.
- 🆕 هر Shot می‌تواند مستقل از DNA پروژه، Negative Prompt خودش را داشته باشد؛ در غیر این صورت به‌طور خودکار از DNA پیروی می‌کند.

---

## یادداشت پیاده‌سازی (برای Claude Code، هنگام اجرای این بلوپرینت به‌روزشده)

این یک قدم **Migration** روی واحد ۰۵ موجود است (که در `domain/shot/` از قبل پیاده‌سازی و تست شده)، نه یک واحد جدید:

- فیلد جدید `negativePromptOverride: String? = null` باید **Backward Compatible** باشد — تمام تست‌های موجود `ShotValidationTest.kt`/سایر تست‌های `domain/shot/` باید بدون تغییر همچنان pass شوند.
- تابع `resolveNegativePrompt` باید در همان فایلی قرار گیرد که `resolveCameraSettings`/`resolveLightingSettings`/`resolveEnvironmentSettings` هستند (طبق ADR-013، در `ShotSettingsResolution.kt`) — این یک الگوی مشابه resolve است، هرچند منبع ارث‌بری‌اش Scene نیست بلکه DNA است؛ با grep بررسی کن که آیا امضای تابع باید `ProjectDna` کامل بگیرد یا فقط `String` (مقدار negativePrompt استخراج‌شده) — پیشنهاد: فقط `String`، تا وابستگی غیرضروری به کل `ProjectDna` در این تابع ایجاد نشود.
- بررسی کن آیا `PromptGenerationRepository.collectData` (واحد ۱۵، قدم ۳) باید به‌روزرسانی شود تا این مقدار Resolve‌شده را به `PromptGenerationInput` (واحد ۱۱) برساند — این محتمل‌ترین جایی است که این فیلد جدید باید واقعاً مصرف شود؛ اگر چنین اتصالی لازم است ولی خارج از دامنه‌ی یک Migration کوچک است، فقط پیشنهاد بده و در گزارش/ADR مستند کن، خودت اجرا نکن.
- 🆕v3 **این تغییرات، ساختاری‌ترین Migration این بلوپرینت تا کنون هستند** — با grep بررسی کن که آیا `data class Shot` واقعی در `domain/shot/ShotModels.kt` از قبل فیلدهای `camera`/`lighting`/`environment` را دارد یا نه:
  - **اگر ندارد** (محتمل، چون خودِ متن این بلوپرینت هم تا این نسخه این فیلدها را نداشت): این سه فیلد را طبق تعریف بالا اضافه کن؛ `SourcedSettings<T>`, `LightingSettings`, `EnvironmentSettings` را هم اضافه کن. این می‌تواند روی کد موجود `domain/promptengine/` (واحد ۱۱) هم اثر بگذارد چون آن واحد به همین type ها ارجاع می‌داد بدون این‌که تعریفشان وجود داشته باشد — با grep در `domain/promptengine/` هم بررسی کن که آیا نسخه‌ی محلی/موقتی از این type ها آن‌جا ساخته شده بود؛ اگر بله، آن نسخه‌ی محلی را حذف کن و از این‌جا import کن (Single Source of Truth).
  - **اگر دارد** (یعنی پیاده‌سازی واقعی زودتر از متن این بلوپرینت به این نتیجه رسیده بود): فقط مطمئن شو ساختار دقیقاً با تعریف بالا یکی است؛ اگر تفاوتی داری (مثلاً نام‌گذاری فیلد متفاوت)، به‌جای تغییر کد موجود، متن این بلوپرینت را با کد واقعی هماهنگ کن (چون کد واقعی و تست‌شده اولویت دارد) و در ADR مستند کن.
- 🆕v3 فیلد `AmbientSound.source` تازه اضافه شده — طبق `ADR` مربوط به رفع هم‌نامی با `AmbientSound` واحد ۱۰؛ اگر کد موجود `domain/shot/` این فیلد را ندارد، اضافه کن (Backward Compatible با پیش‌فرض `"auto_generated"`).
- 🆕v3 فیلد `shotTitle: String?` هم برای اولین‌بار به `data class Shot` اضافه شد (قبلاً فقط در JSON نمونه بود) — Backward Compatible (پیش‌فرض null).
