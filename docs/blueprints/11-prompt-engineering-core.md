# واحد ۱۱: هسته‌ی مهندسی پرامپت (Prompt Engineering Core) ⭐️

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**وضعیت:** فعال — **قلب معماری کل سیستم**
**وابستگی:** تمام واحدهای داده (DNA، Scene، Shot، Asset، Camera، Lighting، Environment، Audio)

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
data class PromptGenerationInput(
    val dna: ProjectDna,
    val scene: Scene,
    val shot: Shot,
    val characters: List<CharacterAsset>,
    val objects: List<Asset>,
    val locations: List<Asset>,
    val camera: CameraSettings,
    val lighting: LightingSettings,
    val environment: EnvironmentSettings,
    val audioContext: AudioContext?
)

fun collectData(shotId: String): PromptGenerationInput {
    val shot = getShot(shotId)
    val scene = getScene(shot.sceneId)
    val dna = getProjectDna(scene.projectId)
    return PromptGenerationInput(
        dna = dna, scene = scene, shot = shot,
        characters = shot.characterIds.map { getCharacter(it) },
        objects = shot.objectIds.map { getAsset(it) },
        locations = shot.locationIds.map { getAsset(it) },
        camera = shot.camera, lighting = shot.lighting, environment = shot.environment,
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
/** توصیف فیزیکی قفل‌شده‌ی کاراکتر را بدون تغییر در پرامپت اعمال می‌کند (طبق واحد ۰۶ - Hard Lock). */
fun enforceCharacterContinuity(characters: List<CharacterAsset>, scene: Scene): List<String> {
    return characters.map { character ->
        val outfit = selectOutfitForScene(character.outfits, scene.weather, character.outfitOverride)
        "${character.physicalAppearance.describe()}, wearing ${outfit.description}"
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

/** تابع اصلی مونتاژ PromptBlueprint از داده‌ی جمع‌آوری‌شده. */
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

    val structuredParts = StructuredParts(
        subjectDescription = enforceCharacterContinuity(input.characters, input.scene).joinToString(", "),
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
