# واحد ۰۶: دارایی‌ها و تداوم (Asset & Continuity System)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**ادغام از:** Asset Library + Character & Subject System + Reference Image Manager (ساده‌شده)
**وضعیت:** فعال

---

## تعریف

مدیریت تمام دارایی‌های قابل‌استفاده‌ی مجدد پروژه — کاراکترها، مکان‌ها، اشیا — با تمرکز بر **تداوم (Continuity)**: تضمین این‌که یک کاراکتر در شات ۵۰ همان ظاهری را دارد که در شات ۱ داشت.

## تفکیک مفهومی

- **Subject:** هر عنصر قابل‌مشاهده در یک Shot (انسان، حیوان، شیء، موجود خیالی).
- **Character:** یک Subject با هویت پایدار که در چند Shot/Scene تکرار می‌شود — Continuity برایش حیاتی است.

---

## دو نوع Asset

### ۱. Character Asset

```json
{
  "asset_id": "char_001",
  "asset_type": "character",
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

### ۲. Location / Object Asset

```json
{
  "asset_id": "loc_001",
  "asset_type": "location",
  "name": "دفتر کارآگاه",
  "description": "دفتر کوچک و شلوغ با میز چوبی قدیمی",
  "environment": { "type": "indoor", "size": "small", "lighting_condition": "dim" },
  "time_compatibility": ["morning", "afternoon", "night"],
  "weather_compatibility": ["all"],
  "key_elements": ["wooden desk", "old filing cabinet", "vintage lamp"]
}
```

---

## قوانین شرطی برای Outfit/Expression (انتخاب خودکار)

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

## Continuity Rules — Hard Lock مطلق (نه Soft Lock)

**این تفاوت آگاهانه و بنیادی با DNA Manager (Soft Lock) است.** `identity_lock`, `appearance_lock`, `age_lock` همیشه به‌صورت **Blocking** اجرا می‌شوند، بدون استثنا — چون این‌ها یک تضمین کیفی بنیادی‌اند (ثبات ظاهری کاراکتر)، نه یک چارچوب سبکی قابل‌تجدیدنظر مثل DNA.

```kotlin
data class ContinuityRules(
    val identityLock: Boolean = true,
    val appearanceLock: Boolean = true,
    val ageLock: Boolean = true,
    val antiDrift: Boolean = true,
    val allowedOverrides: List<String> = listOf("emotion", "pose", "outfit", "expression", "prop")
)

sealed class UpdateResult {
    object Allowed : UpdateResult()
    data class Blocked(val reason: String) : UpdateResult()
}

/** Hard Lock — بدون استثنا. برخلاف DNA Soft Lock، اینجا هرگز فقط هشدار داده نمی‌شود. */
fun validateCharacterUpdate(
    rules: ContinuityRules,
    fieldBeingChanged: String
): UpdateResult {
    return when {
        rules.identityLock && fieldBeingChanged in listOf("name", "asset_id") ->
            UpdateResult.Blocked("این کاراکتر identity_lock دارد؛ نام و ID قابل تغییر نیستند")
        rules.appearanceLock && fieldBeingChanged == "physical_appearance" ->
            UpdateResult.Blocked("این کاراکتر appearance_lock دارد؛ ظاهر پس از قفل‌شدن قابل تغییر نیست")
        rules.ageLock && fieldBeingChanged == "age_range" ->
            UpdateResult.Blocked("این کاراکتر age_lock دارد؛ سن قابل تغییر نیست")
        fieldBeingChanged in rules.allowedOverrides ->
            UpdateResult.Allowed
        else -> UpdateResult.Allowed
    }
}
```

### Character Evolution (تغییر کنترل‌شده در طول زمان)

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

---

## Reference Image — فقط فایل محلی

طبق تصمیم بنیادی پروژه (فقط پرامپت، نه پردازش تصویر)، تصویر مرجع فقط به‌صورت **فایل محلی پیوست‌شده** نگهداری می‌شود:

```kotlin
data class ReferenceImage(val localFilePath: String, val description: String)
```

بدون آپلود، بدون URL خارجی، بدون پردازش/تبدیل فرمت، بدون وزن‌دهی عددی. نحوه‌ی معرفی این تصویر به یک مدل خاص (مثل `--cref` در Midjourney) در لحظه‌ی Rendering، توسط Output Delivery System (بر اساس Model Profile)، تعیین می‌شود — نه اینجا.

### اعتبارسنجی فایل قبل از پیوست (از سند اصلی، جا افتاده بود در نسخه‌ی قبلی)

هرچند این فایل مستقیم روی سرور آپلود نمی‌شود (چون On-Device است)، همچنان باید قبل از پذیرفتن آن به‌عنوان Reference Image یک Asset، بررسی‌های پایه انجام شود — چون فایل انتخاب‌شده از حافظه‌ی دستگاه ممکن است خراب، بیش‌ازحد بزرگ، یا با فرمت پشتیبانی‌نشده باشد:

```kotlin
data class ImageValidationResult(val valid: Boolean, val reason: String? = null)

/** بررسی پایه‌ی فایل تصویر قبل از پیوست به یک Asset — فرمت، سایز، و سلامت فایل. */
fun validateImageFile(filePath: String, fileSizeBytes: Long, mimeType: String): ImageValidationResult {
    val allowedTypes = setOf("image/jpeg", "image/png", "image/webp")
    val maxSizeBytes = 10 * 1024 * 1024  // 10MB — محدودیت معقول برای رفرنس محلی

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

**نکته:** چون این اپ On-Device و بدون سرور است، بررسی «Malware Scan» به شکل سرویس ابری اینجا موضوعیت ندارد؛ بررسی معادل و کافی برای این محیط، اعتبارسنجی MIME Type واقعی فایل (نه فقط پسوند اسم فایل) و رد فایل‌های خراب/غیرقابل‌رمزگشایی به‌عنوان تصویر است.

---

## قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| ۱ | Asset ID باید یکتا باشد | **Blocking** |
| ۲ | Character باید `physical_appearance` داشته باشد | **Blocking** |
| ۳ | Asset در حال استفاده (در یک یا چند Shot) قابل حذف نیست | **Blocking** |
| ۴ | نقض `identity_lock` / `appearance_lock` / `age_lock` | **Blocking** (Hard Lock، بدون استثنا) |
| ۵ | حداقل یک Outfit باید `is_default = true` باشد | **Blocking** |
| ۶ | فایل `reference_images[].local_file_path` باید موجود باشد | **Blocking** |
| ۶ب | فرمت/سایز فایل تصویر باید معتبر باشد (JPEG/PNG/WebP، حداکثر ۱۰ مگابایت) | **Blocking** |
| ۷ | نام Asset مشابه با Asset دیگر | **Warning** (غیرمسدودکننده) |

---

## معیارهای موفقیت

- هر Asset شناسه‌ی یکتا و قابل جستجو دارد.
- Continuity Lock همیشه Blocking است، بدون استثنا.
- انتخاب Outfit/Expression خودکار از شرایط صحنه کار می‌کند، با امکان Override دستی.
- Asset در حال استفاده قابل حذف نیست.
- Reference Image فقط فایل محلی است؛ بدون وابستگی به سرویس خارجی.
