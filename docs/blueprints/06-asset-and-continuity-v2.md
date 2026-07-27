# واحد ۰۶: دارایی‌ها و تداوم (Asset & Continuity System)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**ادغام از:** Asset Library + Character & Subject System + Reference Image Manager (ساده‌شده)
**وضعیت:** فعال

**نسخه:** ۴ — بازنویسی برای افزودن دو enum کشف‌شده در بررسی تکمیلی فرم واقعی «Add New Asset» نمونه‌ی مادر: `Gender` (Female/Male/Other) و `ObjectSubtype` (Personal Prop/General Prop/Costume).

**نسخه:** ۵ — بازنویسی برای رفع دو تناقض کشف‌شده در بازبینی معماری مستقل: (۱) ناسازگاری ساختاری JSON↔Kotlin در `PhysicalAppearance` — نمونه‌ی JSON فیلدهای `hair`/`facial_features` را به‌صورت Object نشان می‌دهد، اما `data class` معادل آن‌ها را `String?` ساده تعریف کرده بود؛ اکنون `Hair`/`FacialFeatures` به‌عنوان `data class` مستقل تعریف شدند تا با JSON هماهنگ باشند. (۲) نبود راهی برای تبدیل `PhysicalAppearance` به یک جمله‌ی خوانا برای پرامپت — تابع `toPromptString()` اضافه شد، چون بدون آن، `enforceCharacterContinuity` (بلوپرینت ۱۱) مجبور بود از `toString()` پیش‌فرض Kotlin استفاده کند که خروجی غیرقابل‌استفاده (`PhysicalAppearance(ageRange=...)`) تولید می‌کرد. **تغییرات با «🆕v5» علامت‌گذاری شده‌اند.**

---

## تعریف

مدیریت تمام دارایی‌های قابل‌استفاده‌ی مجدد پروژه — کاراکترها، مکان‌ها، اشیا — با تمرکز بر **تداوم (Continuity)**: تضمین این‌که یک کاراکتر در شات ۵۰ همان ظاهری را دارد که در شات ۱ داشت.

## تفکیک مفهومی

- **Subject:** هر عنصر قابل‌مشاهده در یک Shot (انسان، حیوان، شیء، موجود خیالی).
- **Character:** یک Subject با هویت پایدار که در چند Shot/Scene تکرار می‌شود — Continuity برایش حیاتی است.

---

## 🆕 چرا سطح‌بندی Lock لازم بود

نسخه‌ی اول این بلوپرینت فرض می‌کرد **همه‌ی کاراکترها به یک اندازه مهم‌اند** — یعنی یک کاراکتر اصلی که در ۵۰ شات ظاهر می‌شود، دقیقاً همان سطح قفل تداوم را می‌گرفت که یک رهگذر پس‌زمینه‌ای که فقط یک‌بار برای دو ثانیه دیده می‌شود. این نادرست است: تحمیل Hard Lock کامل به یک کاراکتر کاملاً فرعی، هم غیرضروری است (چون تداوم دقیق ظاهری‌اش اهمیتی ندارد) و هم بار اضافی روی کاربر می‌گذارد که مجبور شود برای هر شخصیت گذری هم یک Asset کامل با تمام جزئیات بسازد.

به همین ترتیب، مفهوم Lock برای **مکان** و **شیء** از اساس با مفهوم Lock برای **کاراکتر** فرق دارد — یک مکان نیاز به تداوم ظاهری پیکسل‌به‌پیکسل ندارد (نورش می‌تواند بین صحنه‌ها فرق کند)، بلکه فقط باید از نظر **سبک کلی** یکدست بماند. یک شیء/لباس هم فقط باید از نظر **فرم/شکل ظاهری** یکدست بماند، نه هر جزئیات دیگری.

به همین دلیل، این نسخه سه enum **کاملاً مجزا** تعریف می‌کند (نه یک enum مشترک) — چون طبق `concept-ownership-map.md` («هر مفهوم فقط یک مالک اصلی دارد»)، تداوم کاراکتر، تداوم مکان، و تداوم شیء سه مفهوم متفاوتند که فقط شباهت اسمی («Lock») دارند، نه یکسانی معنایی. جداسازی در سطح Type System همچنین از ترکیب‌های بی‌معنی (مثل تنظیم سطح Lock مخصوص مکان روی یک کاراکتر) در زمان کامپایل جلوگیری می‌کند.

---

## دو نوع Asset (به‌روزشده)

### ۱. Character Asset

```json
{
  "asset_id": "char_001",
  "asset_type": "character",
  "character_tier": "main",
  "name": "Detective John",
  "physical_appearance": {
    "age_range": "35-40",
    "gender": "male",
    "height": "tall",
    "build": "athletic",
    "hair": { "color": "black", "style": "short", "length": "short" },
    "facial_features": { "eyes": "brown", "distinctive_marks": ["scar on left cheek"] }
  },
  "outfits": [
    { "id": "outfit_01", "name": "Default Look", "description": "black leather jacket, jeans", "is_default": true, "condition": null },
    { "id": "outfit_02", "name": "Rain Coat", "description": "long dark raincoat", "is_default": false, "condition": { "weather": "rain" } }
  ],
  "expressions": [
    { "id": "exp_calm", "name": "Calm", "description": "neutral face, steady gaze", "emotion": "calm" },
    { "id": "exp_angry", "name": "Angry", "description": "furrowed brows, clenched jaw", "emotion": "angry" }
  ],
  "props": [
    { "id": "prop_gun", "name": "Service Pistol", "description": "standard issue handgun", "category": "weapon" }
  ],
  "default_mood": "calm and observant, slightly guarded",
  "continuity_rules": {
    "identity_lock": true,
    "appearance_lock": true,
    "age_lock": true,
    "anti_drift": true,
    "allowed_overrides": ["emotion", "pose", "outfit", "expression", "prop"]
  },
  "reference_images": [
    { "local_file_path": "/storage/project_001/assets/char_001_ref.jpg", "description": "رفرنس چهره" }
  ]
}
```

🆕 **فیلد جدید: `character_tier`** — سه مقدار ممکن: `main` (اصلی)، `secondary` (فرعی)، `background` (پس‌زمینه/گذری). این فیلد تعیین می‌کند کدام سطح از `ContinuityLockLevel` (پایین‌تر توضیح داده شده) به‌صورت پیش‌فرض روی این کاراکتر اعمال شود — اما **کاربر همیشه می‌تواند این پیش‌فرض را دستی تغییر دهد** (مثلاً یک کاراکتر «فرعی» که تصمیم می‌گیرد باید Full Lock بگیرد).

🆕v3 **فیلد جدید: `default_mood`** — یک رشته‌ی آزاد و اختیاری که حالت روحی/شخصیتی پیش‌فرض کاراکتر را توصیف می‌کند (مثال بالا: «آرام و کنجکاو، کمی محتاط»). این مقدار وقتی به‌کار می‌آید که یک Shot به این کاراکتر متصل است اما هیچ Expression خاصی برایش انتخاب نشده — در آن صورت این توصیف پیش‌فرض به پرامپت نهایی اضافه می‌شود (نه یک مقدار خالی). این فیلد مستقل از `expressions` (که حالت‌های چهره‌ی گسسته و مشخص‌اند) است؛ `default_mood` بیشتر یک رنگ‌وبوی کلی شخصیتی است، نه یک حالت چهره‌ی خاص.

🆕v3 **فیلد جدید (روی هر سه نوع Asset): `base_prompt`** — یک `textarea`/رشته‌ی آزاد کاملاً اختیاری، مستقل از تمام فیلدهای ساختاریافته‌ی بالا، برای جزئیات اضافی که کاربر می‌خواهد همیشه به توصیف این Asset اضافه شود اما در هیچ فیلد ساختاریافته‌ای نمی‌گنجد. این با فلسفه‌ی کلی پروژه («پیش‌فرض هوشمند ساختاریافته + همیشه یک راه برای Override/افزودن دستی») هم‌راستاست — دقیقاً مشابه نقشی که `shot_description` در سطح Shot دارد، اما اینجا در سطح Asset.

### ۱ب. 🆕v3 Object / Prop Asset (ساختار مستقل و کامل)

نسخه‌ی قبلی این بلوپرینت فقط اشاره کرده بود که Object/Prop Asset «ساختار مشابه Location دارد» — این نادرست بود؛ Object/Prop از نظر فیلدهای اختصاصی با Location فرق دارد. ساختار کامل و مستقل آن:

```json
{
  "asset_id": "obj_005",
  "asset_type": "object",
  "name": "فانوس قدیمی",
  "description": "فانوس فلزی قدیمی با شمعی روشن در داخل",
  "size": "small",
  "material_and_color": "متال زنگ‌زده، رنگ مسی تیره",
  "special_trait": "glowing, antique",
  "continuity_lock_level": "form"
}
```

فیلدهای اختصاصی: `size` (اندازه‌ی نسبی، مثل `small`/`medium`/`large`)، `material_and_color` (جنس و رنگ به‌صورت رشته‌ی آزاد توصیفی)، `special_trait` (ویژگی خاص مثل «درخشان»، «عتیقه»، «جادویی» — رشته‌ی آزاد کوتاه). این‌ها با فیلدهای Location (`environment`, `time_compatibility`, `weather_compatibility`) کاملاً متفاوتند چون Object یک شیء ثابت است، نه یک محیط.

### ۲. Location Asset

```json
{
  "asset_id": "loc_001",
  "asset_type": "location",
  "name": "دفتر کارآگاه",
  "description": "دفتر کوچک و شلوغ با میز چوبی قدیمی",
  "environment": { "type": "indoor", "size": "small", "lighting_condition": "dim" },
  "time_compatibility": ["morning", "afternoon", "night"],
  "weather_compatibility": ["all"],
  "key_elements": ["wooden desk", "old filing cabinet", "vintage lamp"],
  "continuity_lock_level": "style"
}
```

🆕 **فیلد جدید: `continuity_lock_level` برای مکان** — همیشه مقدار `"style"` (سبک) دارد؛ توضیح در بخش بعد.

---

## 🆕 سه سطح Lock مجزا (نه یک enum مشترک)

### الف) سطح Lock کاراکتر — `CharacterContinuityLevel`

| سطح | معنا | کِی اعمال می‌شود |
|---|---|---|
| `FULL` | Hard Lock کامل (دقیقاً مثل نسخه‌ی قبلی این بلوپرینت) — `identity_lock`, `appearance_lock`, `age_lock` همه فعال و Blocking | پیش‌فرض برای `character_tier = "main"` |
| `MEDIUM` | تداوم مهم است اما با انعطاف بیشتر — فقط `identity_lock` (نام/ID) Blocking است؛ تغییرات جزئی ظاهر فقط Warning می‌گیرند | پیش‌فرض برای `character_tier = "secondary"` |
| `NONE` | بدون هیچ قفل تداوم — این کاراکتر آنقدر گذری است که تداوم دقیق ظاهری‌اش اهمیت ندارد | پیش‌فرض برای `character_tier = "background"` |

⚠️ **نکته‌ی حیاتی که نباید نقض شود:** سطح `FULL` دقیقاً همان رفتار Hard Lock نسخه‌ی قبلی این بلوپرینت را دارد — **بدون هیچ استثنا، همیشه Blocking**. این سطح نباید سست‌تر شود؛ تغییری که این نسخه معرفی می‌کند فقط این است که حالا کاربر می‌تواند **آگاهانه** یک کاراکتر را در سطح پایین‌تر (`MEDIUM` یا `NONE`) قرار دهد — نه اینکه سطح `FULL` خودش نرم‌تر شده باشد.

### ب) سطح Lock مکان — `LocationContinuityLevel`

فقط یک مقدار وجود دارد: `STYLE` — یعنی فقط سبک بصری کلی مکان (نوع محیط، حس‌وحال نورپردازی پایه) باید بین شات‌های مختلف یکدست بماند؛ جزئیات دقیق‌تر (زاویه‌ی دوربین، نور لحظه‌ای صحنه) آزادانه تغییر می‌کنند. این هرگز Blocking نیست، همیشه فقط یک یادآوری/Warning در صورت ناسازگاری آشکار سبکی.

### ج) سطح Lock شیء/لباس — `PropContinuityLevel`

فقط یک مقدار وجود دارد: `FORM` — یعنی فقط شکل/فرم ظاهری شیء (نه رنگ دقیق، نه بافت) باید یکدست بماند. مثل مکان، این هم همیشه Warning است، هرگز Blocking.

---

## قوانین شرطی برای Outfit/Expression (انتخاب خودکار) — بدون تغییر از نسخه‌ی قبلی

هر `outfit` می‌تواند یک `condition` اختیاری داشته باشد که تعیین می‌کند در چه شرایطی به‌طور خودکار انتخاب شود.

```kotlin
data class OutfitCondition(
    val weather: String? = null,
    val timeOfDay: String? = null,
    val locationType: String? = null
)

data class Outfit(
    val id: String,
    val name: String,
    val description: String,
    val isDefault: Boolean,
    val condition: OutfitCondition? = null
)

/**
 * انتخاب Outfit: اول اولویت با Override دستی کاربر، بعد شرط منطبق
 * با صحنه، در نهایت Fallback به Default.
 */
fun selectOutfitForScene(
    outfits: List<Outfit>,
    sceneWeather: String?,
    manualOverrideId: String? = null
): Outfit {
    if (manualOverrideId != null) {
        return outfits.first { it.id == manualOverrideId }
    }
    val matched = outfits.firstOrNull { it.condition?.weather == sceneWeather }
    return matched ?: outfits.first { it.isDefault }
}
```

همین منطق برای `expressions` هم قابل استفاده است (مثلاً Expression مخصوص «بعد از دویدن»).

---

## Continuity Rules — پیاده‌سازی مفهومی به‌روزشده (Kotlin)

```kotlin
// 🆕 سه enum کاملاً مجزا — طبق تصمیم معماری بالا، هرگز با هم قاطی نشوند
enum class CharacterTier { MAIN, SECONDARY, BACKGROUND }

// 🆕v4 طبق فرم واقعی «Add New Asset» — سه مقدار دقیق، نه رشته‌ی آزاد
enum class Gender { FEMALE, MALE, OTHER }

// 🆕v5 data class های مستقل برای هماهنگی با JSON که این دو فیلد را به‌صورت Object نشان می‌داد
data class Hair(val color: String, val style: String, val length: String)
data class FacialFeatures(val eyes: String, val distinctiveMarks: List<String> = emptyList())

// 🆕v4 ساختار کامل PhysicalAppearance که در نسخه‌های قبلی فقط در نمونه‌ی JSON اشاره شده بود، نه Kotlin
data class PhysicalAppearance(
    val ageRange: String,           // رشته‌ی آزاد کوتاه، مثل "25 years old" (طبق فرم واقعی)
    val gender: Gender,
    val height: String? = null,
    val build: String? = null,
    val hair: Hair? = null,                        // 🆕v5 اصلاح شد — قبلاً String? بود، با JSON (Object) ناسازگار
    val physicalFeatures: String? = null,   // طبق فرم واقعی: "Brown hair, green eyes, tall..." — توصیف آزاد تکمیلی، مستقل از hair/facialFeatures ساختاریافته
    val facialFeatures: FacialFeatures? = null     // 🆕v5 اصلاح شد — قبلاً String? بود، با JSON (Object) ناسازگار
) {
    /**
     * 🆕v5 تبدیل به یک جمله‌ی خوانا برای پرامپت — بدون این تابع، enforceCharacterContinuity
     * (بلوپرینت ۱۱) مجبور بود از toString() پیش‌فرض Kotlin استفاده کند که خروجی
     * غیرقابل‌استفاده (مثل "PhysicalAppearance(ageRange=35-40, ...)") تولید می‌کرد.
     */
    fun toPromptString(): String {
        val parts = mutableListOf<String>()
        parts += "$ageRange ${gender.name.lowercase()}"
        height?.let { parts += it }
        build?.let { parts += "$it build" }
        hair?.let { parts += "${it.length} ${it.color} hair, ${it.style} style" }
        facialFeatures?.let { ff ->
            parts += "${ff.eyes} eyes"
            if (ff.distinctiveMarks.isNotEmpty()) parts += ff.distinctiveMarks.joinToString(", ")
        }
        physicalFeatures?.let { parts += it }
        return parts.joinToString(", ")
    }
}
enum class CharacterContinuityLevel { FULL, MEDIUM, NONE }
enum class LocationContinuityLevel { STYLE }
enum class PropContinuityLevel { FORM }

/** 🆕 پیش‌فرض سطح Lock بر اساس Tier — کاربر همیشه می‌تواند override کند. */
fun defaultLockLevelForTier(tier: CharacterTier): CharacterContinuityLevel = when (tier) {
    CharacterTier.MAIN -> CharacterContinuityLevel.FULL
    CharacterTier.SECONDARY -> CharacterContinuityLevel.MEDIUM
    CharacterTier.BACKGROUND -> CharacterContinuityLevel.NONE
}

data class ContinuityRules(
    val identityLock: Boolean = true,
    val appearanceLock: Boolean = true,
    val ageLock: Boolean = true,
    val antiDrift: Boolean = true,
    val allowedOverrides: List<String> = listOf("emotion", "pose", "outfit", "expression", "prop")
)

/** 🆕v3 data class کامل CharacterAsset، شامل defaultMood — طبق ساختار JSON بالا. */
data class CharacterAsset(
    val assetId: String,
    val characterTier: CharacterTier,
    val name: String,
    val physicalAppearance: PhysicalAppearance,
    val outfits: List<Outfit>,
    val expressions: List<Expression> = emptyList(),
    val props: List<Prop> = emptyList(),
    val defaultMood: String? = null,   // 🆕v3 حالت روحی/شخصیتی پیش‌فرض، وقتی Expression خاصی انتخاب نشده
    val basePrompt: String? = null,    // 🆕v3 توضیحات آزاد اضافی، مستقل از فیلدهای ساختاریافته
    val continuityRules: ContinuityRules = ContinuityRules(),
    val referenceImages: List<ReferenceImage> = emptyList()
)

/** 🆕v3 ساختار مستقل و کامل برای Object/Prop Asset — قبلاً فقط اشاره‌ی گذرا به Location بود. */
// 🆕v4 طبق فرم واقعی «Add New Asset» — سه زیرگروه Object که کاملاً غایب بود
enum class ObjectSubtype { PERSONAL_PROP, GENERAL_PROP, COSTUME }

data class ObjectAsset(
    val assetId: String,
    val name: String,
    val description: String,
    val subtype: ObjectSubtype,         // 🆕v4 الزامی — تعیین می‌کند شیء شخصی/عمومی/لباس است
    val size: String,                  // مثل "small" / "medium" / "large"
    val materialAndColor: String,       // رشته‌ی آزاد توصیفی جنس/رنگ
    val specialTrait: String? = null,   // مثل "glowing", "antique", "magical"
    val basePrompt: String? = null,     // 🆕v3 توضیحات آزاد اضافی، مستقل از فیلدهای ساختاریافته
    val continuityLockLevel: PropContinuityLevel = PropContinuityLevel.FORM
)

/** 🆕v3 ساختار کامل LocationAsset، شامل basePrompt — طبق ساختار JSON بخش Location Asset. */
data class LocationAsset(
    val assetId: String,
    val name: String,
    val description: String,
    val environment: Environment,
    val timeCompatibility: List<String> = emptyList(),
    val weatherCompatibility: List<String> = emptyList(),
    val keyElements: List<String> = emptyList(),
    val basePrompt: String? = null,   // 🆕v3 توضیحات آزاد اضافی، مستقل از فیلدهای ساختاریافته
    val continuityLockLevel: LocationContinuityLevel = LocationContinuityLevel.STYLE
)

data class Environment(val type: String, val size: String, val lightingCondition: String)

sealed class UpdateResult {
    object Allowed : UpdateResult()
    data class Blocked(val reason: String) : UpdateResult()
    data class Warned(val message: String) : UpdateResult()   // 🆕 حالت جدید — فقط برای MEDIUM/STYLE/FORM
}

/**
 * 🆕 Hard Lock حالا شرطی به سطح Lock است، نه یکسان برای همه‌ی کاراکترها.
 * سطح FULL دقیقاً رفتار قبلی (Blocking بدون استثنا) را حفظ می‌کند.
 * سطح MEDIUM فقط identity را Blocking نگه می‌دارد، بقیه فقط Warning می‌گیرند.
 * سطح NONE هیچ قفلی اعمال نمی‌کند.
 */
fun validateCharacterUpdate(
    level: CharacterContinuityLevel,
    rules: ContinuityRules,
    fieldBeingChanged: String
): UpdateResult {
    return when (level) {
        CharacterContinuityLevel.FULL -> when {
            rules.identityLock && fieldBeingChanged in listOf("name", "asset_id") ->
                UpdateResult.Blocked("این کاراکتر identity_lock دارد؛ نام و ID قابل تغییر نیستند")
            rules.appearanceLock && fieldBeingChanged == "physical_appearance" ->
                UpdateResult.Blocked("این کاراکتر appearance_lock دارد؛ ظاهر پس از قفل‌شدن قابل تغییر نیست")
            rules.ageLock && fieldBeingChanged == "age_range" ->
                UpdateResult.Blocked("این کاراکتر age_lock دارد؛ سن قابل تغییر نیست")
            fieldBeingChanged in rules.allowedOverrides -> UpdateResult.Allowed
            else -> UpdateResult.Allowed
        }
        CharacterContinuityLevel.MEDIUM -> when {
            fieldBeingChanged in listOf("name", "asset_id") ->
                UpdateResult.Blocked("این کاراکتر حتی در سطح Medium، identity_lock دارد؛ نام و ID قابل تغییر نیستند")
            fieldBeingChanged == "physical_appearance" ->
                UpdateResult.Warned("تغییر ظاهر یک کاراکتر Medium‌-lock — ممکن است باعث ناسازگاری جزئی شود")
            else -> UpdateResult.Allowed
        }
        CharacterContinuityLevel.NONE -> UpdateResult.Allowed   // بدون هیچ محدودیتی
    }
}

/** 🆕 معادل برای مکان — همیشه فقط Warning، هرگز Blocking. */
fun validateLocationUpdate(fieldBeingChanged: String, isStyleField: Boolean): UpdateResult {
    return if (isStyleField)
        UpdateResult.Warned("تغییر سبک بصری این مکان ممکن است با شات‌های قبلی ناسازگار باشد")
    else UpdateResult.Allowed
}

/** 🆕 معادل برای شیء/لباس — همیشه فقط Warning، هرگز Blocking. */
fun validatePropUpdate(fieldBeingChanged: String, isFormField: Boolean): UpdateResult {
    return if (isFormField)
        UpdateResult.Warned("تغییر فرم ظاهری این شیء ممکن است با شات‌های قبلی ناسازگار باشد")
    else UpdateResult.Allowed
}
```

### Character Evolution (تغییر کنترل‌شده در طول زمان) — بدون تغییر از نسخه‌ی قبلی

برای تغییرات مجاز و آگاهانه (نه Drift ناخواسته) — مثلاً کاراکتر بعد از یک صحنه‌ی مبارزه زخمی می‌شود:

```json
{
  "character_id": "char_001",
  "evolution_timeline": [
    {
      "from_shot": "shot_051",
      "to_shot": "shot_100",
      "changes": { "distinctive_marks": ["add: bruise on right eye"] },
      "reason": "بعد از صحنه‌ی مبارزه"
    }
  ]
}
```

Evolution یک مسیر رسمی و ثبت‌شده برای تغییر است — متفاوت از نقض Continuity Lock؛ باید صریحاً توسط کاربر تعریف شود، نه به‌صورت ضمنی رخ دهد.

🆕 **نکته:** Evolution Timeline فقط برای کاراکترهایی معنا دارد که سطح `FULL` یا `MEDIUM` دارند — یک کاراکتر `NONE` (بدون هیچ قفلی) نیازی به مسیر رسمی Evolution ندارد، چون تغییرش از اساس محدود نشده است.

---

## Reference Image — فقط فایل محلی (بدون تغییر از نسخه‌ی قبلی)

طبق تصمیم بنیادی پروژه (فقط پرامپت، نه پردازش تصویر)، تصویر مرجع فقط به‌صورت **فایل محلی پیوست‌شده** نگهداری می‌شود:

```kotlin
data class ReferenceImage(val localFilePath: String, val description: String)
```

بدون آپلود، بدون URL خارجی، بدون پردازش/تبدیل فرمت، بدون وزن‌دهی عددی. نحوه‌ی معرفی این تصویر به یک مدل خاص (مثل `--cref` در Midjourney) در لحظه‌ی Rendering، توسط Output Delivery System (بر اساس Model Profile)، تعیین می‌شود — نه اینجا.

### اعتبارسنجی فایل قبل از پیوست

هرچند این فایل مستقیم روی سرور آپلود نمی‌شود (چون On-Device است)، همچنان باید قبل از پذیرفتن آن به‌عنوان Reference Image یک Asset یک بررسی‌های پایه انجام شود:

```kotlin
data class ImageValidationResult(val valid: Boolean, val reason: String? = null)

fun validateImageFile(filePath: String, fileSizeBytes: Long, mimeType: String): ImageValidationResult {
    val allowedTypes = setOf("image/jpeg", "image/png", "image/webp")
    val maxSizeBytes = 10 * 1024 * 1024  // 10MB

    return when {
        mimeType !in allowedTypes ->
            ImageValidationResult(false, "فرمت پشتیبانی نمی‌شود؛ فقط JPEG/PNG/WebP مجاز است")
        fileSizeBytes > maxSizeBytes ->
            ImageValidationResult(false, "حجم فایل بیش از حد مجاز (۱۰ مگابایت) است")
        fileSizeBytes == 0L ->
            ImageValidationResult(false, "فایل خراب یا خالی است")
        else -> ImageValidationResult(true)
    }
}
```

---

## قوانین اعتبارسنجی (جدول به‌روزشده)

| Rule | شرح | Severity |
|---|---|---|
| ۱ | Asset ID باید یکتا باشد | **Blocking** |
| ۲ | Character باید `physical_appearance` داشته باشد | **Blocking** |
| ۳ | Asset در حال استفاده (در یک یا چند Shot) قابل حذف نیست | **Blocking** |
| ۴ | نقض `identity_lock` / `appearance_lock` / `age_lock` در سطح **FULL** | **Blocking** (بدون استثنا) |
| 🆕 ۴ب | نقض `identity_lock` در سطح **MEDIUM** | **Blocking** |
| 🆕 ۴پ | تغییر ظاهر در سطح **MEDIUM** (غیر از identity) | **Warning** |
| ۵ | حداقل یک Outfit باید `is_default = true` باشد | **Blocking** |
| ۶ | فایل `reference_images[].local_file_path` باید موجود باشد | **Blocking** |
| ۶ب | فرمت/سایز فایل تصویر باید معتبر باشد | **Blocking** |
| ۷ | نام Asset مشابه با Asset دیگر | **Warning** |
| 🆕 ۸ | تغییر سبک یک Location (سطح STYLE) | **Warning** (هرگز Blocking) |
| 🆕 ۹ | تغییر فرم یک Prop (سطح FORM) | **Warning** (هرگز Blocking) |
| 🆕v3 ۱۰ | `ObjectAsset.size`/`materialAndColor` الزامی (رشته‌ی غیرخالی)؛ `specialTrait` اختیاری | **Blocking** برای `size`/`materialAndColor`، فیلد اختیاری نیازی به Rule ندارد |
| 🆕v3 ۱۱ | `base_prompt` (روی هر سه نوع Asset، در صورت پر بودن) هیچ محدودیت طول سخت‌گیرانه‌ای ندارد؛ فقط نباید whitespace-only باشد | **Warning** |
| 🆕v4 ۱۲ | `ObjectAsset.subtype` باید یکی از سه مقدار معتبر `ObjectSubtype` باشد (بدون مقدار پیش‌فرض ضمنی) | **Blocking** |

---

## معیارهای موفقیت (به‌روزشده)

- هر Asset شناسه‌ی یکتا و قابل جستجو دارد.
- 🆕 Continuity Lock سطح **FULL** همیشه Blocking است، بدون استثنا (دقیقاً مثل نسخه‌ی قبلی).
- 🆕 سطوح **MEDIUM/STYLE/FORM** فقط هشدار می‌دهند، هرگز جلوی کاربر را نمی‌گیرند.
- 🆕 سطح **NONE** هیچ محدودیتی اعمال نمی‌کند.
- انتخاب Outfit/Expression خودکار از شرایط صحنه کار می‌کند، با امکان Override دستی.
- Asset در حال استفاده قابل حذف نیست.
- Reference Image فقط فایل محلی است؛ بدون وابستگی به سرویس خارجی.

---

## یادداشت پیاده‌سازی (برای Claude Code، هنگام اجرای این بلوپرینت به‌روزشده)

این یک قدم **Migration** روی واحد ۰۶ موجود است (که در `domain/asset/` از قبل پیاده‌سازی و تست شده، شامل Hard Lock که در چند قدم بعدی پروژه — از جمله در Migration جامع قبلی — به‌عنوان تنها Lock مطلق پروژه به‌طور خاص محافظت شد)، نه یک واحد جدید از صفر:

- **حساس‌ترین نکته:** رفتار فعلی `UpdateResult` (فقط `Allowed`/`Blocked`، بدون Warning) باید برای سطح `FULL` **دقیقاً** حفظ شود — این دقیقاً همان تضمین Type System بود که در یک Migration قبلی (هنگام یکسان‌سازی انواع Validation در سراسر پروژه) عمداً از تبدیل‌شدن به یک نوع سراسری منعطف‌تر مستثنی نگه داشته شد. افزودن حالت `Warned` به `UpdateResult` یک تغییر واقعی در این sealed class است — قبل از اعمال، با grep بررسی کن که آیا جایی از کد موجود روی «`UpdateResult` فقط دو حالت دارد» فرض کرده (مثلاً یک `when` بدون `else` که با افزودن حالت سوم دیگر Exhaustive نیست) — اگر چنین جایی پیدا کردی، آن را هم به‌روزرسانی کن و در گزارش فهرست کن.
- فیلد جدید `character_tier` باید **Backward Compatible** باشد — Asset های موجود بدون این فیلد باید مقدار پیش‌فرض معقول بگیرند (پیشنهاد: `MAIN`، تا رفتار فعلی/محافظه‌کارانه حفظ شود؛ اما این تصمیم را می‌توانی خودت با دلیل در گزارش مشخص کنی).
- `LocationContinuityLevel` و `PropContinuityLevel` مفاهیم کاملاً جدیدی هستند که در نسخه‌ی قبلی این بلوپرینت اصلاً وجود نداشتند — بررسی کن آیا `LocationAsset`/Object Asset موجود در `domain/asset/` نیاز به فیلد جدید دارند یا این منطق می‌تواند به‌صورت مستقل (بدون فیلد ذخیره‌شده، چون همیشه یک مقدار ثابت دارد) پیاده شود.
- 🆕v3 فیلد `defaultMood: String? = null` باید به `CharacterAsset` موجود در `domain/asset/` اضافه شود — Backward Compatible (پیش‌فرض null).
- 🆕v3 `ObjectAsset` (اگر در `domain/asset/` این‌طور نام‌گذاری نشده و چیزی مثل `LocationAsset` مشترک برای location/object استفاده می‌شود، با grep بررسی کن) نیاز به سه فیلد جدید دارد: `size: String`, `materialAndColor: String`, `specialTrait: String? = null`. اگر Object/Location فعلاً یک `data class` مشترک هستند (طبق کامنت نسخه‌ی قبلی «پوشش‌دهنده‌ی هر دو AssetType.LOCATION و AssetType.OBJECT»)، بررسی کن آیا باید این دو را جدا کرد (چون فیلدهای اختصاصی‌شان اکنون کاملاً متفاوت است) یا فیلدهای جدید را nullable/اختیاری روی همان data class مشترک اضافه کرد (فقط برای AssetType.OBJECT پر می‌شوند) — این یک تصمیم طراحی واقعی است، با دلیل در گزارش/ADR مستند کن.
- 🆕v3 فیلد `basePrompt: String? = null` باید به هر سه نوع Asset (`CharacterAsset`, `LocationAsset`, و معادل Object) اضافه شود — Backward Compatible (پیش‌فرض null). این فیلد باید در نهایت به توصیف متنی نهایی هر Asset (جایی که در واحد ۱۱ برای `subjectDescription` استفاده می‌شود) اضافه شود، اما اتصال کامل به Pipeline خارج از دامنه‌ی این Migration کوچک است — فقط ساختار داده در این قدم اضافه شود؛ اگر اتصال ساده و بی‌خطر بود، می‌توانی همان‌جا هم انجام دهی، در غیر این صورت به‌عنوان TODO مستند کن.
- 🆕v4 `PhysicalAppearance` باید از یک فیلد آزاد/نامشخص (اگر در کد فعلی این‌طور است) به `data class` کامل با `Gender` enum (نه رشته‌ی آزاد `"male"`/`"female"`) Migrate شود. این یک Breaking Change است اگر جای دیگری (Story-to-Domain Mapper در `01b-ai-story-breakdown.md`) از رشته‌ی خام gender استفاده می‌کند — بعد از این Migration، آن فایل هم باید هماهنگ شود (طبق یادداشت مشابه در همان بلوپرینت).
- 🆕v4 `ObjectAsset.subtype: ObjectSubtype` یک فیلد **الزامی جدید** است (نه اختیاری با پیش‌فرض) — Asset های Object موجود (اگر داده‌ی واقعی از قبل ذخیره شده) باید یک مقدار پیش‌فرض معقول بگیرند در زمان Migration؛ پیشنهاد: `GENERAL_PROP` (محافظه‌کارانه‌ترین حالت)، اما این تصمیم را با دلیل در گزارش/ADR مستند کن.
- 🆕v5 `Hair`/`FacialFeatures` باید به `domain/asset/` اضافه شوند؛ فیلدهای `PhysicalAppearance.hair`/`.facialFeatures` از `String?` به این دو نوع جدید تغییر می‌کنند — این یک **Breaking Change واقعی** است، هر جای کد که این دو فیلد را به‌عنوان String می‌خواند (احتمالاً در `enforceCharacterContinuity` بلوپرینت ۱۱، قبل از افزودن `toPromptString`) باید بازبینی شود.
- 🆕v5 `PhysicalAppearance.toPromptString()` باید در `enforceCharacterContinuity` (بلوپرینت ۱۱) جایگزین استفاده‌ی مستقیم `"${character.physicalAppearance}"` شود — این دقیقاً همان باگی بود که در بازبینی معماری کشف شد؛ هنگام Migration بلوپرینت ۱۱ (که هم‌زمان با این فایل اصلاح می‌شود) به این نکته توجه کن.
