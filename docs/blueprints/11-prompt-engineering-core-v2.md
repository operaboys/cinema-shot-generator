# واحد ۱۱: هسته‌ی مهندسی پرامپت (Prompt Engineering Core) ⭐️

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**وضعیت:** فعال — **قلب معماری کل سیستم**
**وابستگی:** تمام واحدهای داده (DNA، Scene، Shot، Asset، Camera، Lighting، Environment، Audio)

**نسخه:** ۲ — رفع سه انحراف قدیمی (`outfitOverride` ناموجود، خواندن مستقیم `shot.camera` بدون resolve، `Asset` مشترک قدیمی به‌جای `ObjectAsset`/`LocationAsset`).

**نسخه:** ۳ — بازنویسی برای رفع دو تناقض بحرانی کشف‌شده در بازبینی معماری مستقل: (۱) `enforceCharacterContinuity` مستقیماً `"${character.physicalAppearance}"` را در یک String Template قرار می‌داد — چون `physicalAppearance` یک `data class` است نه `String`، این کد `toString()` پیش‌فرض Kotlin را صدا می‌زد و خروجی غیرقابل‌استفاده (`PhysicalAppearance(ageRange=...)`) تولید می‌کرد؛ اکنون از `physicalAppearance.toPromptString()` (بلوپرینت ۰۶، نسخه ۵) استفاده می‌شود. (۲) `LightingSettings`/`EnvironmentSettings` که این بلوپرینت به آن‌ها ارجاع می‌داد، در نسخه‌های قبلی در هیچ‌جا (نه اینجا، نه ۰۸) تعریف نشده بودند — اکنون این‌ها در بلوپرینت ۰۵ (نسخه ۳) تعریف شده‌اند و این‌جا فقط `import` می‌شوند. **تغییرات با «🆕v3» علامت‌گذاری شده‌اند.**

**تغییرات با «🆕v2» علامت‌گذاری شده‌اند.**

---

## اصل معماری بنیادی — جداسازی کامل

این واحد **فقط** جمع‌آوری داده + حل تضاد + اعمال اولویت را انجام می‌دهد. خروجی آن یک `PromptBlueprint` **کاملاً خنثی نسبت به مدل هدف** است — بدون هیچ نحو یا فرمت مخصوص پلتفرم (نه `--ar`، نه `--seed` به‌عنوان پارامتر خروجی، نه `Negative:`، نه هیچ‌چیز مخصوص Midjourney/Sora/Runway).

```
Prompt Engineering Core (این واحد)
  → جمع‌آوری داده + حل تضاد + اولویت‌بندی (Human Override > Shot > Scene > DNA)
  → خروجی: PromptBlueprint خنثی
        ↓
Output Delivery System (واحد ۱۴)
  → Renderer که Model Profile را می‌خواند
  → فرمت نهایی مخصوص یک مدل خاص را می‌سازد
```

**چرا این جداسازی حیاتی است:** بدون آن، افزودن هر مدل جدید نیازمند تغییر در قلب منطق حل‌تضاد می‌شود. با این جداسازی، منطق گران‌قیمت (حل‌تضاد) فقط یک‌بار اجرا می‌شود و همان `PromptBlueprint` می‌تواند برای چند مدل مختلف Render شود.

---

## فرآیند تولید (۶ فاز)

### فاز ۱: جمع‌آوری داده

```kotlin
// 🆕v3 lighting/environment از نوع LightingSettings/EnvironmentSettings هستند که اکنون
// در بلوپرینت ۰۵ (نسخه ۳) تعریف شده‌اند (import از domain.shot) — نه یک نوع محلی موقت.
// 🆕v2 objects/locations اکنون انواع مجزا؛ camera/lighting/environment از نوع Resolve‌شده‌ی نهایی
data class PromptGenerationInput(
    val dna: ProjectDna,
    val scene: Scene,
    val shot: Shot,
    val characters: List<CharacterAsset>,
    val objects: List<ObjectAsset>,      // 🆕v2 قبلاً List<Asset> مشترک بود
    val locations: List<LocationAsset>,  // 🆕v2 قبلاً List<Asset> مشترک بود
    val camera: CameraSettings,          // مقدار نهایی Resolve‌شده، نه SourcedSettings خام
    val lighting: LightingSettings,      // 🆕v3 import از domain.shot (بلوپرینت ۰۵) — مقدار نهایی Resolve‌شده
    val environment: EnvironmentSettings,// 🆕v3 import از domain.shot (بلوپرینت ۰۵) — مقدار نهایی Resolve‌شده
    val audioContext: AudioContext?
)

/**
 * 🆕v2 نسخه‌ی هم‌راستا با پیاده‌سازی واقعی: camera/lighting/environment از طریق
 * توابع resolve* (بلوپرینت ۰۵) به مقدار نهایی تبدیل می‌شوند، نه مستقیماً خوانده می‌شوند —
 * چون Shot.camera/.lighting/.environment در واقعیت از نوع SourcedSettings<T> هستند
 * (شامل source: "scene" یا "override")، نه مستقیماً نوع تنظیمات نهایی.
 * پیاده‌سازی واقعی این تابع در لایه‌ی Repository (واحد ۱۵) قرار دارد؛ اینجا فقط
 * قرارداد/امضای مورد انتظار نشان داده شده است.
 */
suspend fun collectData(shotId: String): PromptGenerationInput {
    val shot = getShot(shotId)
    val scene = getScene(shot.sceneId)
    val dna = getProjectDna(scene.projectId)

    // مقدار پیش‌فرض Scene برای هرکدام (اگر Scene فیلد پیش‌فرض معادلی دارد؛ طبق ADR-013
    // ممکن است Scene فاقد این فیلدها باشد و sceneDefault همیشه null باشد — به پیاده‌سازی واقعی رجوع شود)
    // 🆕v3 توابع resolve* اکنون Result<T> برمی‌گردانند (بلوپرینت ۰۵، نسخه ۳) — نه مقدار مستقیم؛
    // getOrThrow اینجا به این معناست که اگر Shot.camera/.lighting/.environment هنوز تنظیم نشده باشند
    // (source=SCENE بدون sceneDefault در دسترس)، کل collectData شکست می‌خورد — این رفتار عمدی است،
    // چون تولید پرامپت بدون این تنظیمات اصلاً معنا ندارد.
    val resolvedCamera = resolveCameraSettings(shot, sceneDefault = null).getOrThrow()
    val resolvedLighting = resolveLightingSettings(shot, sceneDefault = null).getOrThrow()
    val resolvedEnvironment = resolveEnvironmentSettings(shot, sceneDefault = null).getOrThrow()

    return PromptGenerationInput(
        dna = dna, scene = scene, shot = shot,
        characters = shot.characterIds.map { getCharacterAsset(it) },
        objects = shot.objectIds.map { getObjectAsset(it) },
        locations = shot.locationIds.map { getLocationAsset(it) },
        camera = resolvedCamera, lighting = resolvedLighting, environment = resolvedEnvironment,
        audioContext = getAudioContext(shotId)
    )
}
```

### فاز ۲: سیستم اولویت‌بندی

```kotlin
enum class PriorityLevel { HUMAN_OVERRIDE, SHOT_SPECIFIC, CHARACTER_CONTINUITY, SCENE_CONTEXT, PROJECT_DNA }

/** ترتیب اولویت از بالا به پایین — اولین مقدار غیرخالی در این ترتیب برنده است. */
val PRIORITY_ORDER = listOf(
    PriorityLevel.HUMAN_OVERRIDE,
    PriorityLevel.SHOT_SPECIFIC,
    PriorityLevel.CHARACTER_CONTINUITY,
    PriorityLevel.SCENE_CONTEXT,
    PriorityLevel.PROJECT_DNA
)

fun <T> resolvePriority(valuesByLevel: Map<PriorityLevel, T?>): T? {
    for (level in PRIORITY_ORDER) {
        valuesByLevel[level]?.let { return it }
    }
    return null
}
```

**نکته:** این اولویت‌بندی («Human Override > Shot > Scene > DNA») مستقل از سیستم Override ساده‌شده در واحد ۰۱ است — آنجا درباره‌ی *مجاز بودن* Override بود (Blocking/Warning)؛ اینجا درباره‌ی *ترتیب اعمال مقادیر* وقتی چند منبع همزمان یک فیلد را تعیین می‌کنند.

### فاز ۳: حل تضاد

منطق کامل تشخیص تضاد در واحد ۰۷ (Validation & Consistency Engine) تعریف شده. این فاز فقط از آن واحد استفاده می‌کند و نتیجه را در `PromptBlueprint` منعکس و در `metadata.conflicts_resolved` ثبت می‌کند.

### فاز ۴: اعمال Character Continuity

```kotlin
/**
 * 🆕v2 توصیف فیزیکی قفل‌شده‌ی کاراکتر را بدون تغییر در پرامپت اعمال می‌کند (طبق واحد ۰۶ - Hard Lock).
 * manualOverrideOutfitId یک پارامتر مستقل است (نه character.outfitOverride که اصلاً وجود ندارد) —
 * منبع این پارامتر معمولاً سطح Shot یا یک انتخاب صریح کاربر در UI است، نه خودِ Asset.
 */
fun enforceCharacterContinuity(
    characters: List<CharacterAsset>,
    scene: Scene,
    sceneWeather: String?,
    manualOverrideOutfitIds: Map<String, String?> = emptyMap()   // assetId -> outfitId، در صورت انتخاب دستی کاربر
): List<String> {
    return characters.map { character ->
        val outfitId = selectOutfitForScene(
            character.outfits, sceneWeather, manualOverrideOutfitIds[character.assetId]
        )
        val outfit = character.outfits.first { it.id == outfitId }
        val moodPart = character.defaultMood?.let { ", $it" } ?: ""   // 🆕v2 استفاده از defaultMood در نبود Expression خاص
        // 🆕v3 اصلاح باگ: قبلاً "${character.physicalAppearance}" بود که toString() پیش‌فرض
        // (خروجی غیرقابل‌استفاده مثل "PhysicalAppearance(ageRange=...)") تولید می‌کرد.
        "${character.physicalAppearance.toPromptString()}, wearing ${outfit.description}$moodPart"
    }
}
```

### فاز ۵: مدیریت Seed

**تصمیم آگاهانه:** `seed` یک پارامتر فرمت خروجی (مثل `--ar`) نیست — یک تصمیم محتوایی برای **ثبات بصری بین شات‌ها** است، هم‌رده با Character Continuity. بنابراین در `PromptBlueprint` باقی می‌ماند، نه در Output Delivery.

```kotlin
/** Seed ثابت و قابل‌پیش‌بینی از shotId؛ شات‌های مشابه در یک صحنه می‌توانند Seed های نزدیک به هم بگیرند. */
fun manageSeed(shotId: String, useSeed: Boolean, explicitSeedValue: Int?): Int? {
    if (!useSeed) return null
    return explicitSeedValue ?: shotId.hashCode()
}
```

آیا یک مدل خاص (مثل Sora) اصلاً از Seed پشتیبانی می‌کند یا نه، تصمیمی است که **Renderer** در Output Delivery (واحد ۱۴) بر اساس Model Profile می‌گیرد — نه اینجا.

### فاز ۶: Weighted Emphasis (بدون نحو خاص پلتفرم)

```kotlin
/** فقط داده‌ی خام "کدام تگ چه‌قدر تأکید دارد" را نگه می‌دارد؛ نحو نمایش (پرانتز، ::عدد) در Renderer تعیین می‌شود. */
fun collectWeightedEmphasis(weightedTags: Map<String, Float>?): Map<String, Float> {
    return weightedTags ?: emptyMap()
}
```

---

## ساختار خروجی: PromptBlueprint

```kotlin
data class StructuredParts(
    val subjectDescription: String,
    val sceneContext: String,
    val shotDescription: String,
    val cameraSpecs: String,
    val lightingSpecs: String,
    val environmentSpecs: String?,
    val styleModifiers: String,
    val timelineBeats: String?,      // از Beat Sheet، اگر فعال باشد
    val audioDescription: String?    // از Ambient/Action خودکار + Character دستی
)

data class PromptBlueprint(
    val promptBlueprintId: String,
    val shotId: String,
    val structuredParts: StructuredParts,
    val imageReferences: List<ImageReference>,  // فقط local_file_path، بدون فرمت‌دهی
    val weightedEmphasis: Map<String, Float>,
    val seed: Int?,
    val conflictsResolved: Int,
    val warnings: List<ValidationIssue>
)

/** 🆕v2 تابع اصلی مونتاژ PromptBlueprint — هم‌راستا با امضای جدید enforceCharacterContinuity. */
fun assemblePromptBlueprint(input: PromptGenerationInput, useSeed: Boolean, weightedTags: Map<String, Float>?): PromptBlueprint {
    val timelineBeats = input.shot.beats.takeIf { it.isNotEmpty() }
        ?.joinToString(", ") { "[${it.timestampSeconds}s] ${it.description}" }

    val audioDescription = input.audioContext?.let { audio ->
        val parts = mutableListOf<String>()
        if (audio.ambientSounds.isNotEmpty()) parts += "ambient: " + audio.ambientSounds.joinToString(", ") { "${it.type} (${it.intensity})" }
        if (audio.actionSounds.isNotEmpty()) parts += "action sounds: " + audio.actionSounds.joinToString(", ") { it.type }
        if (audio.characterSounds.isNotEmpty()) parts += "character sounds: " + audio.characterSounds.joinToString(", ") { it.description }
        parts.takeIf { it.isNotEmpty() }?.joinToString("; ")
    }

    // 🆕v2 sceneWeather از Scene استخراج می‌شود اگر چنین مفهومی وجود دارد؛ طبق ADR-013 ممکن است
    // این مقدار از منبع دیگری (مثلاً Environment مرتبط با Scene) تأمین شود — به پیاده‌سازی واقعی رجوع شود.
    val subjectDescription = enforceCharacterContinuity(
        input.characters, input.scene, sceneWeather = null
    ).joinToString(", ")

    val structuredParts = StructuredParts(
        subjectDescription = subjectDescription,
        sceneContext = "${input.scene.atmospherePrimary} atmosphere, ${input.scene.timeOfDay} time",
        shotDescription = input.shot.shotDescription,
        cameraSpecs = "${input.camera.angle} angle, ${input.camera.distance} shot, ${input.camera.lensType} lens",
        lightingSpecs = "${input.lighting.style} lighting, ${input.lighting.keyLightPosition} key light, ${input.lighting.contrastRatio} contrast",
        environmentSpecs = if (input.environment.weatherType != "clear") "${input.environment.weatherType} weather" else null,
        styleModifiers = "${input.dna.coreIdentity.dominantVisualStyle} style, ${input.dna.masterPalette.colorGradingPreset}",
        timelineBeats = timelineBeats,
        audioDescription = audioDescription
    )

    return PromptBlueprint(
        promptBlueprintId = generateId(),
        shotId = input.shot.shotId,
        structuredParts = structuredParts,
        imageReferences = input.shot.imageReferences,
        weightedEmphasis = collectWeightedEmphasis(weightedTags),
        seed = manageSeed(input.shot.shotId, useSeed, null),
        conflictsResolved = 0,  // از واحد ۰۷ پر می‌شود
        warnings = emptyList()   // از واحد ۰۷ پر می‌شود
    )
}
```

**نکته حیاتی:** این خروجی هنوز برای هیچ مدلی فرمت نشده — تبدیل `PromptBlueprint` به متن نهایی (یا JSON، یا هر فرمت دیگر) و تزریق `image_references` به نحو مخصوص هر پلتفرم، کاملاً وظیفه‌ی **Output Delivery System** (واحد ۱۴) است.

---

## قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| پرامپت شامل عنصری از `DNA.forbidden_elements` | نقض قانون سراسری پروژه | **Blocking** |
| عدم شمول همه‌ی `DNA.mandatory_elements` | عناصر الزامی جا افتاده | **Warning** |
| نقض `anti_drift` در توصیف کاراکتر نسبت به شات‌های قبلی | Continuity Drift | **Warning** |
| وجود تضاد حل‌نشده (از واحد ۰۷) | باید ثبت و به کاربر اطلاع داده شود | **Warning** |

**مهم:** بررسی محدودیت طول/توکن **در این واحد انجام نمی‌شود** — این کار Token Cost Calculator (واحد ۱۳) بعد از Rendering است، چون در این مرحله مدل هدف هنوز مشخص نیست.

---

## معیارهای موفقیت

- PromptBlueprint تمام داده‌های ورودی را به‌صورت ساختاریافته (نه رشته‌ی خام) ترکیب می‌کند.
- خروجی کاملاً خنثی نسبت به مدل هدف است — هیچ نحو مخصوص پلتفرمی در آن نیست.
- تضادها حل و ثبت می‌شوند؛ Character Continuity حفظ می‌شود.
- Seed برای ثبات بصری بین شات‌ها مدیریت می‌شود (به‌عنوان تصمیم محتوایی، نه فرمت خروجی).
- Beat Sheet و Audio Description (در صورت وجود) در `structured_parts` منعکس می‌شوند.
- 🆕v2 هیچ ارجاعی به فیلدها یا انواع منسوخ (`character.outfitOverride`, `Asset` مشترک قدیمی, خواندن مستقیم `shot.camera`) در متن این بلوپرینت باقی نمانده است.

---

## یادداشت پیاده‌سازی (برای Claude Code، هنگام اجرای این بلوپرینت به‌روزشده)

این یک قدم **Migration مستندسازی** است، نه لزوماً تغییر منطقی جدید در کد — طبق شواهد (ADR-012، ADR-013)، پیاده‌سازی واقعی احتمالاً **از قبل درست است** و این بازنویسی فقط متن بلوپرینت را با آن هماهنگ می‌کند:

- 🆕v2 با grep بررسی کن که `domain/promptengine/` (کد واقعی) از قبل امضای درست (بدون `outfitOverride`، با `resolveCameraSettings` و مشابه) را دارد یا نه. اگر بله (محتمل‌ترین حالت)، فقط این بلوپرینت را commit کن، هیچ کدی تغییر نکند.
- اگر کد واقعی هنوز جایی از الگوی قدیمی پیروی می‌کند (کمتر محتمل، ولی چک کن، حدس نزن)، آن را مطابق این بلوپرینت اصلاح کن و در گزارش/ADR جدید (بعد از آخرین ADR موجود) مستند کن.
- توجه به علامت `sceneWeather = null` و `sceneDefault = null` در نمونه‌کدهای بالا — این‌ها Placeholder برای نشان‌دادن قرارداد امضا هستند؛ مقدار واقعی این پارامترها باید از منابع درست (طبق ADR-013) در لایه‌ی Repository واقعی تأمین شوند، نه همیشه `null` در کد واقعی.
- 🆕v3 **این نسخه دو تغییر واقعاً بحرانی دارد که باید در کد هم اعمال شوند (نه فقط مستندسازی):**
  1. با grep بررسی کن که `enforceCharacterContinuity` در `domain/promptengine/` واقعاً از `physicalAppearance.toPromptString()` استفاده می‌کند یا هنوز `"${character.physicalAppearance}"` خام است (باگ). اگر باگ در کد هم هست، **این یک تغییر منطقی واقعی است، نه صرفاً مستندسازی** — باید اصلاح شود و تست جدیدی اضافه شود که بررسی کند خروجی `enforceCharacterContinuity` حاوی رشته‌ی `"PhysicalAppearance("` نیست (یعنی توصیف واقعاً خوانا تولید می‌شود).
  2. با grep بررسی کن که `LightingSettings`/`EnvironmentSettings` در کد واقعی از `domain.shot` (بلوپرینت ۰۵) import می‌شوند، نه یک تعریف محلی/موقت در `domain/promptengine/`. اگر یک نسخه‌ی محلی وجود دارد، آن را حذف کن و از منبع واقعی import کن (Single Source of Truth) — این تغییر باید هم‌زمان با Migration بلوپرینت ۰۵ (که این دو نوع را برای اولین‌بار رسماً تعریف کرد) انجام شود، نه قبل از آن.
  3. با grep بررسی کن که فراخوانی `resolveCameraSettings`/`resolveLightingSettings`/`resolveEnvironmentSettings` در `collectData` با امضای جدید `Result<T>` (نه مقدار مستقیم) هماهنگ است — طبق نمونه‌کد بالا، `.getOrThrow()` رفتار پیش‌فرض پیشنهادی است، اما اگر ترجیح می‌دهی خطا را با `Result.failure` به بالا منتقل کنی (به‌جای throw کردن)، امضای `collectData` را به `Result<PromptGenerationInput>` تغییر بده و در گزارش/ADR دلیلش را بگو.
