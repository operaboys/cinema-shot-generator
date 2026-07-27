# واحد ۱۴: تحویل خروجی (Output Delivery System)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**ادغام از:** Output Target Profiles (Model Profile Library) + Output Composer + Bilingual System
**وضعیت:** فعال — **دومین واحد کلیدی معماری** (Renderer قلب سیستم)
**وابستگی:** Prompt Engineering Core (واحد ۱۱)، Prompt Finalization Pipeline (واحد ۱۳)

**نسخه:** ۳ — بازنویسی برای رفع یک تناقض واقعی کشف‌شده در بازبینی معماری مستقل: جدول «کتابخانه‌ی پروفایل‌ها» (که یک نمونه‌ی نسبتاً قدیمی/توصیفی بود، نه فهرست واقعی پیاده‌سازی‌شده در ADR-021) با فهرست واقعی ۱۳ Model Profile که در قدم اجرایی واقعی ساخته شدند (بدون `Sora`، طبق تصمیم صریح کاربر؛ شامل `Wan`/`HunyuanVideo`/`LTX`/`Vidu` که در نسخه‌ی قبلی این جدول اصلاً نبودند) ناهماهنگ بود. این نسخه جدول را با فهرست واقعی ۱۳ مدل یکسان کرد. **تغییرات با «🆕v3» علامت‌گذاری شده‌اند.**

---

## جایگاه در معماری

```
PromptBlueprint (خنثی، واحد ۱۱)
        ↓
Model Profile Library (بخش الف) → انتخاب پروفایل مدل هدف
        ↓
Renderer (بخش الف) → متن خام مخصوص آن مدل
        ↓
Prompt Finalization Pipeline (واحد ۱۳: Cleaner + Token Calculator)
        ↓
Output Composer (بخش ب) → بسته‌بندی نهایی + هر دو نسخه‌ی زبانی (بخش ج)
```

---

## 🆕 چرا این بازنویسی لازم بود — یک وظیفه‌ی تعریف‌شده اما اجرانشده

بلوپرینت واحد ۱۱ (Prompt Engineering Core) از ابتدا صریحاً می‌گفت: *«تزریق `image_references` به نحو مخصوص هر پلتفرم، کاملاً وظیفه‌ی Output Delivery System (واحد ۱۴) است»* — و `PromptBlueprint` از قبل یک فیلد `imageReferences: List<ImageReference>` را حمل می‌کند. اما در پیاده‌سازی نسخه‌ی اول این بلوپرینت، تابع `render()` هرگز واقعاً به این فیلد نگاه نمی‌کرد — یعنی این داده تا اینجا می‌رسید و سپس نادیده گرفته می‌شد.

این یک تصمیم آگاهانه نبود، بلکه یک قدم جامانده بود که در بررسی مستقیم یک نمونه‌ی کارکردی (پروژه‌ی مادر) کشف شد: وقتی یک Character/Location/Prop Asset با تصویر رفرنس پیوست‌شده به یک Shot متصل است، پرامپت نهایی باید **صریحاً** به مدل بگوید از تصویر(های) پیوست‌شده برای ظاهر آن استفاده کند — **نه صرفاً به توصیف متنی اکتفا کند.**

### چرا این تفاوت حیاتی است (نه یک ظرافت کوچک)

توصیف متنی به‌تنهایی (مثلاً «دختری ۱۶ ساله با موهای قرمز») هر بار که به یک مدل تصویرساز/ویدیوساز داده می‌شود، **یک تفسیر بصری جدید و کمی متفاوت** تولید می‌کند — چون مدل حافظه‌ی بصری واقعی از یک شخص خاص ندارد، فقط دارد کلمات را دوباره تفسیر می‌کند. این دقیقاً همان مشکلی است که `Continuity Lock` (واحد ۰۶) قرار است حل کند، اما توصیف متنی به‌تنهایی این تضمین را نمی‌دهد. راه‌حل واقعی: وقتی یک عکس رفرنس واقعی وجود دارد و مدل هدف از آپلود Reference Image پشتیبانی می‌کند، پرامپت باید مدل را به **کپی از تصویر واقعی** هدایت کند، نه حدس از روی توصیف کلامی.

⚠️ **نکته‌ی مهم درباره‌ی نحوه‌ی این ارجاع:** این ارجاع باید یک **جمله‌ی دستوری کلی** باشد (مثل: `"use the attached reference image(s) for this character's appearance"`)، **نه** نام فایل یا مسیر تصویر. خودِ فایل تصویر به‌صورت جدا (attachment واقعی) به مدل داده می‌شود؛ متن پرامپت فقط باید به مدل بگوید که باید از آن استفاده کند، دقیقاً هماهنگ با تصمیم بنیادی قبلی این پروژه (`ReferenceImage`, واحد ۰۶) که هرگز نام/مسیر فایل را در متن پرامپت درج نمی‌کند.

---

## بخش الف — Model Profile Library

### فلسفه: Data-driven، نه هاردکد

هر پروفایل مدل (JSON قابل‌ویرایش) شامل: `capabilities` (قابلیت‌ها)، `constraints` (محدودیت‌ها)، `parameters` (پارامترهای قابل‌تنظیم)، `optimization_rules`. این طراحی یعنی افزودن/تغییر یک مدل بدون Build مجدد اپ ممکن است.

### پروفایل Universal/Default (همیشه موجود، fallback)

```json
{
  "profile_id": "universal_default",
  "platform": "generic",
  "capabilities": { "supports_video": true, "supports_image": true, "supports_weighted_tags": false, "supports_image_prompt": false },
  "constraints": { "max_prompt_length": 2000, "max_tokens": 500 },
  "parameters": { "format": { "type": "plain_text", "structure": "paragraph" } },
  "optimization_rules": { "avoid_platform_specific_syntax": true }
}
```

این پروفایل هیچ نحو خاص پلتفرمی اعمال نمی‌کند — فقط توصیف متنی واضح از تمام `structured_parts` می‌سازد. 🆕 چون پروفایل جهانی نمی‌داند مدل واقعی هدف از Reference Image پشتیبانی می‌کند یا نه، `supports_image_prompt` در اینجا محافظه‌کارانه `false` است.

### کتابخانه‌ی پروفایل‌ها (فهرست واقعی — ۱۳ مدل پیاده‌سازی‌شده طبق قدم اجرایی، جولای ۲۰۲۶)

⚠️ **یادداشت مهم درباره‌ی این جدول:** بازار مدل‌های ویدیوساز AI بسیار سریع تغییر می‌کند. جدول زیر فهرست واقعی ۱۳ مدلی است که در قدم اجرایی این پروژه پیاده‌سازی شدند (نه یک نمونه‌ی توصیفی) — با `Universal Default` جمعاً ۱۴ پروفایل. **`Sora` به تصمیم صریح کاربر عمداً از این فهرست حذف شده و هیچ پروفایلی برایش وجود ندارد.**

**مدل‌های تجاری (Proprietary):**

| مدل | سازنده | نوع فرمت | پشتیبانی Reference Image | ویژگی خاص | وضعیت |
|---|---|---|---|---|---|
| **Veo 3.1** | Google | JSON | بله | صدای همزمان با لب (lip-sync) | فعال |
| **Kling 3.0** | Kuaishou | JSON | بله | ارزان‌ترین گزینه‌ی باکیفیت | فعال |
| **Seedance 2.0** | ByteDance | JSON | بله | رتبه‌ی اول کیفیت خام | فعال |
| **Pika 2.5** | Pika Labs | JSON ساده | بله | تکرار سریع | فعال |
| **Runway Gen-4.5** | Runway | Structured Text | بله | بهترین کنترل خلاقانه | فعال |
| **Luma Ray 3** | Luma AI | JSON | بله (قوی، Image-to-Video) | بهترین HDR | فعال |
| **Hailuo (MiniMax) 2.3** | MiniMax | JSON | بله | بهترین ثبات ظاهری کاراکتر | فعال |
| **Midjourney v6/v7** (تصویر) | Midjourney | Command String | بله (پارامتر `--cref`) | مرجع تصویر ثابت | فعال |

**مدل‌های Open-Source (قابل اجرای محلی):**

| مدل | سازنده | نوع فرمت | پشتیبانی Reference Image | ویژگی خاص | وضعیت |
|---|---|---|---|---|---|
| **Wan 2.2** | Alibaba | JSON | بله | بهترین کیفیت Open-weight | فعال |
| **HunyuanVideo 1.5** | Tencent | JSON | بله | حرکت سینمایی قوی | فعال |
| **LTX-2.3** | Lightricks | JSON | بله | صدای استریو، کیفیت لوکال ۴K | فعال |
| **Vidu Q3** | Shengshu | JSON | بله | کلیپ تا ۱۶ ثانیه | فعال |
| **Stable Diffusion XL / SD3** | Stability AI | JSON | بله (ControlNet، وزن‌دهی غیرفعال طبق ADR-021) | Open-weight | فعال |

| **Universal / Default** | — | Plain Text | 🆕 خیر (پیش‌فرض محافظه‌کارانه) | Fallback عمومی | همیشه فعال |

🆕v3 **جمع کل: ۱۳ Model Profile واقعی + Universal Default = ۱۴ پروفایل.** این فهرست باید همیشه با `00-master-overview-v2.md` (که همین فهرست را در معرفی کلی پروژه ذکر می‌کند) و `docs/reference/type-registry.md` هماهنگ بماند — طبق قانون «منبع واحد حقیقت».

**نکته‌ی عملی:** قبل از پیاده‌سازی واقعی هر پروفایل، مستندات رسمی همان مدل باید بررسی شود؛ فیلد `supports_image_prompt` هم باید طبق مستندات واقعی هر مدل (نه فرض) تنظیم شود — این جدول تخمین اولیه است.

### نمونه پروفایل کامل — Veo 3.1 (نمونه‌ی الگو برای مدل‌های JSON با پشتیبانی Reference Image)

```json
{
  "profile_id": "veo_3_1",
  "platform": "veo",
  "version": "3.1",
  "active": true,
  "capabilities": {
    "supports_video": true,
    "supports_audio_sync": true,
    "supports_lip_sync": true,
    "supports_weighted_tags": false,
    "supports_image_prompt": true
  },
  "constraints": { "max_prompt_length": 2000, "max_duration_seconds": 60 },
  "parameters": {
    "required": ["prompt"],
    "optional": ["duration", "aspect_ratio", "audio_description"],
    "format": { "type": "json", "structure": "paragraph" }
  },
  "optimization_rules": {
    "prefer_paragraph_style": true,
    "enforce_camera_description": true
  }
}
```

⚠️ **هشدار عملی برای Sora:** طبق وضعیت بازار جولای ۲۰۲۶، پروفایل Sora باید با `active: false` یا یک پرچم هشدار ثبت شود.

### پیاده‌سازی مفهومی (Kotlin) — نسخه‌ی به‌روزشده

```kotlin
data class ModelCapabilities(
    val supportsVideo: Boolean, val supportsImage: Boolean,
    val supportsWeightedTags: Boolean, val supportsImagePrompt: Boolean, val supportsNegativePrompt: Boolean
)
data class ModelConstraints(val maxPromptLength: Int, val maxTokens: Int)
data class ModelFormat(val type: String, val structure: String? = null, val commandPrefix: String? = null)
data class ModelProfile(
    val profileId: String, val platform: String,
    val capabilities: ModelCapabilities, val constraints: ModelConstraints,
    val format: ModelFormat, val optimizationRules: Map<String, Any> = emptyMap()
)

/** انتخاب پروفایل با Fallback به آخرین نسخه‌ی فعال همان پلتفرم. */
fun selectProfile(targetPlatform: String, profiles: List<ModelProfile>): ModelProfile {
    return profiles.firstOrNull { it.platform == targetPlatform }
        ?: profiles.first { it.profileId == "universal_default" }
}

/**
 * 🆕 اگر Shot به Asset هایی با تصویر رفرنس متصل است و مدل هدف پشتیبانی می‌کند،
 * یک جمله‌ی دستوری کلی (نه نام فایل) تولید می‌کند.
 * این تابع کاملاً مستقل و قابل‌تست است، جدا از renderBlueprintToText.
 */
fun buildReferenceImageInstruction(
    imageReferences: List<ImageReference>,
    profile: ModelProfile
): String? {
    if (imageReferences.isEmpty()) return null
    if (!profile.capabilities.supportsImagePrompt) return null

    return if (imageReferences.size == 1)
        "use the attached reference image for this subject's appearance"
    else
        "use the attached reference images for these subjects' appearances"
}

/** قلب Renderer: تبدیل structured_parts به یک متن پایه بر اساس ترجیح ساختاری پروفایل. */
fun renderBlueprintToText(blueprint: PromptBlueprint, profile: ModelProfile): String {
    val parts = blueprint.structuredParts
    val segments = listOfNotNull(
        parts.subjectDescription, parts.sceneContext, parts.shotDescription,
        parts.cameraSpecs, parts.lightingSpecs, parts.environmentSpecs, parts.styleModifiers
    ).toMutableList()

    if (parts.timelineBeats != null && profile.capabilities.supportsVideo) segments += "Timeline: ${parts.timelineBeats}"
    if (parts.audioDescription != null && profile.capabilities.supportsVideo) segments += "Audio: ${parts.audioDescription}"

    // 🆕 دستور استفاده از عکس رفرنس — همیشه نزدیک ابتدای توصیف سوژه اضافه می‌شود، نه انتهای پرامپت،
    // چون این یک دستور راهنمای ادراک بصری است، نه یک جزئیات فرعی.
    val referenceInstruction = buildReferenceImageInstruction(blueprint.imageReferences, profile)
    if (referenceInstruction != null) segments.add(1, referenceInstruction)

    var text = segments.joinToString(if (profile.format.structure == "paragraph") ". " else ", ")

    if (profile.capabilities.supportsWeightedTags) {
        for ((tag, weight) in blueprint.weightedEmphasis) {
            text = applyWeightSyntax(text, tag, weight, profile)
        }
    }
    return text
}

fun applyWeightSyntax(text: String, tag: String, weight: Float, profile: ModelProfile): String {
    return if (profile.platform == "midjourney") text.replace(tag, "$tag::$weight") else text
}

/** بهینه‌سازی نهایی: Rendering + اعمال Keyword Mapping و کوتاه‌سازی. */
fun optimizeForProfile(blueprint: PromptBlueprint, profile: ModelProfile): String {
    var optimized = renderBlueprintToText(blueprint, profile)
    if (optimized.length > profile.constraints.maxPromptLength) {
        optimized = optimized.take(profile.constraints.maxPromptLength - 3) + "..."
    }
    return optimized
}

data class RenderedOutput(val modelProfileId: String, val formattedPrompt: String, val language: String)

/**
 * خروجی نهایی برای یک پروفایل خاص.
 * 🆕 توجه: خودِ فایل‌های تصویر (imageReferences) در این تابع "ضمیمه" نمی‌شوند —
 * این وظیفه‌ی لایه‌ی UI/اتصال به مدل (خارج از دامنه‌ی این واحد؛ صرفاً متن پرامپت اینجا تولید می‌شود)
 * است که فایل‌های واقعی را جدا از متن، به‌عنوان attachment، به مدل هدف ارسال کند.
 */
fun render(blueprint: PromptBlueprint, profile: ModelProfile): RenderedOutput {
    val optimizedText = optimizeForProfile(blueprint, profile)
    val formatted = when (profile.format.type) {
        "command_string" -> "${profile.format.commandPrefix} $optimizedText"
        "plain_text" -> optimizedText  // Universal — بدون فرمت‌دهی اضافه
        else -> optimizedText
    }
    return RenderedOutput(profile.profileId, formatted, language = "en")
}
```

### قوانین اعتبارسنجی (به‌روزشده)

| Rule | شرح | Severity |
|---|---|---|
| Profile برای پلتفرم درخواستی یافت نشد | باید Fallback به Universal شود یا خطا بدهد | **Blocking** |
| طول متن Render شده بیشتر از `max_prompt_length` | نیاز به کوتاه‌سازی یا هشدار | **Warning** |
| استفاده از فیچر پشتیبانی‌نشده (مثل Timeline Beats برای مدلی که ویدیو ندارد) | آن بخش نادیده گرفته می‌شود | **Warning** |
| 🆕 `imageReferences` غیرخالی ولی مدل هدف `supports_image_prompt = false` | دستور تزریق نمی‌شود؛ کاربر باید در UI آگاه شود که این مدل از تصویر رفرنس پشتیبانی نمی‌کند | **Warning** |

---

## بخش ب — Output Composer (بسته‌بندی نهایی) — بدون تغییر از نسخه‌ی قبلی

**وظیفه:** فقط بسته‌بندی خروجی‌های آماده؛ **هیچ منطق تولید محتوا یا فرمت‌دهی مدل ندارد**.

```kotlin
data class OutputPackage(
    val outputId: String,
    val shotId: String,
    val promptBlueprintId: String,
    val bilingualPrompts: BilingualPrompts,
    val renderedOutputs: List<RenderedOutput>,
    val exportFiles: List<ExportFile>
)

data class BilingualPrompts(val enVersion: String, val faVersion: String)
data class ExportFile(val filename: String, val content: String, val mimeType: String)

/** فقط بسته‌بندی — بدون تغییر محتوای rendered_outputs دریافتی. */
fun composeOutput(
    shotId: String,
    promptBlueprintId: String,
    renderedOutputs: List<RenderedOutput>,
    bilingualPrompts: BilingualPrompts
): OutputPackage {
    require(renderedOutputs.isNotEmpty()) { "حداقل یک rendered_output لازم است" }

    val exportFiles = mutableListOf(
        ExportFile("${shotId}_prompt_en.txt", bilingualPrompts.enVersion, "text/plain"),
        ExportFile("${shotId}_prompt_fa.txt", bilingualPrompts.faVersion, "text/plain")
    )
    renderedOutputs.forEach { rendered ->
        exportFiles += ExportFile("${shotId}_${rendered.modelProfileId}.txt", rendered.formattedPrompt, "text/plain")
    }

    return OutputPackage(generateId(), shotId, promptBlueprintId, bilingualPrompts, renderedOutputs, exportFiles)
}
```

### قوانین

| Rule | شرح | Severity |
|---|---|---|
| `rendered_outputs` خالی | حداقل یک خروجی لازم است | **Blocking** |
| یکی از نسخه‌های زبانی موجود نیست | باید هر دو نسخه تولید شده باشند | **Warning** |
| محتوای `rendered_output` در حین بسته‌بندی تغییر کند | نقض اصل «فقط بسته‌بندی، بدون تغییر محتوا» | ساختاری (باگ پیاده‌سازی) |

---

## بخش ج — Bilingual System (دوزبانگی) — بدون تغییر از نسخه‌ی قبلی

⚠️ **دامنه‌ی این بخش دو چیز کاملاً متفاوت است — نباید با هم اشتباه شوند:**

1. **دوزبانگی خروجی پرامپت**: تولید نسخه‌ی EN و FA از پرامپت نهایی.
2. **دوزبانگی خودِ اپ (UI)**: تمام صفحات، دکمه‌ها، پیام‌های خطا، برچسب‌های فرم باید دوزبانه باشند.

### اصل بنیادی: هر دو زبان معتبر، بدون برتری پیش‌فرض

```kotlin
enum class Language { FA, EN }

data class LanguagePreferences(
    val uiLanguage: Language = Language.FA,
    val promptLanguage: Language = Language.FA,
    val fallbackLanguage: Language = Language.EN
)

fun generateBilingualPrompt(englishText: String): BilingualPrompts {
    return BilingualPrompts(enVersion = englishText, faVersion = translateToFarsi(englishText))
}
```

### پوشش کامل — تمام ۱۶ واحد

```
/locales
  ├─ fa.json
  │   ├─ common, story_wizard, dna_manager, visual_identity, scene, shot,
  │   │  asset_library, validation, scene_conditions, camera_motion, audio,
  │   │  output_delivery, project_storage, workflow, errors
  └─ en.json
      └─ (همان ساختار، به انگلیسی)
```

### تابع ترجمه

```kotlin
fun t(key: String, uiLanguage: Language, translations: Map<Language, Map<String, String>>): String {
    return translations[uiLanguage]?.get(key)
        ?: translations[Language.EN]?.get(key)
        ?: key
}
```

### قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| کلید ترجمه در هیچ‌کدام از دو زبان یافت نشود | خودِ کلید نمایش داده می‌شود | **Warning** |
| زبان درخواستی خارج از FA/EN | پشتیبانی نمی‌شود | **Blocking** |
| رشته‌ی متنی Hardcode در کد UI | نقض اصل پوشش کامل i18n | ساختاری |
| افزودن واحد/صفحه‌ی جدید بدون کلیدهای ترجمه | ناقص‌ماندن پوشش دوزبانگی | **Blocking** |

---

## معماری کلی سه‌لایه

| جنبه | Prompt Engineering Core (واحد ۱۱) | Output Delivery System (این واحد) |
|---|---|---|
| مسئولیت | حل‌تضاد و ترکیب داده | Render برای مدل خاص + بسته‌بندی + دوزبانگی |
| ورودی | Scene, Shot, DNA data | PromptBlueprint + Model Profile |
| خروجی | PromptBlueprint خنثی | متن فرمت‌شده + بسته‌ی نهایی Export |
| آگاهی از مدل | ندارد | دارد (از Model Profile) |

---

## معیارهای موفقیت (به‌روزشده)

- Model Profile Library به‌صورت JSON قابل‌ویرایش است.
- پروفایل Universal همیشه به‌عنوان Fallback در دسترس است.
- Output Composer هرگز محتوای `rendered_output` دریافتی را تغییر نمی‌دهد.
- هر دو نسخه‌ی زبانی همیشه تولید می‌شوند.
- یک PromptBlueprint می‌تواند بدون تکرار حل‌تضاد، برای چند مدل مختلف Render شود.
- دوزبانگی UI سراسری است.
- پوشش دو فایل زبان همیشه ۱۰۰٪ هم‌ساختار است.
- 🆕 وقتی یک Shot به Asset هایی با عکس رفرنس متصل است و مدل هدف پشتیبانی می‌کند، پرامپت نهایی همیشه شامل یک دستور صریح (نه نام فایل) برای استفاده از آن تصویر است.
- 🆕 هرگز نام فایل یا مسیر تصویر در متن پرامپت درج نمی‌شود — فقط یک جمله‌ی دستوری کلی.

---

## یادداشت پیاده‌سازی (برای Claude Code، هنگام اجرای این بلوپرینت به‌روزشده)

این یک قدم **Migration** روی واحد ۱۴ موجود است (که در `domain/outputdelivery/` از قبل پیاده‌سازی و تست شده، شامل ADR-021 برای ۱۳ Model Profile واقعی و ADR-025 برای رفع محدودیت JSON/وزن‌دهی)، نه یک واحد جدید:

- تابع جدید `buildReferenceImageInstruction` باید به `Renderer.kt` اضافه شود؛ `renderBlueprintToText` باید آن را فراخوانی کند.
- فیلد `supports_image_prompt` باید برای هر ۱۳ پروفایل موجود (از ADR-021) بازبینی و به‌روزرسانی شود — با grep بررسی کن که آیا این فیلد از قبل در `ModelCapabilities` وجود دارد (طبق نسخه‌ی اول این بلوپرینت باید وجود داشته باشد) یا باید اضافه شود؛ سپس برای هر پروفایل، مقدار درست (طبق جدول بالا) را ست کن.
- **نکته‌ی مهم درباره‌ی Backward Compatibility:** افزودن یک عنصر به وسط لیست `segments` (`segments.add(1, referenceInstruction)`) در `renderBlueprintToText` باعث تغییر خروجی متنی برای هر پروفایلی می‌شود که `imageReferences` غیرخالی دارد — این یعنی تست‌های موجود `RendererTest.kt` که سناریوی `imageReferences` غیرخالی را پوشش می‌دهند (اگر موجودند) ممکن است نیاز به به‌روزرسانی داشته باشند، نه چون اشتباه بودند، بلکه چون رفتار مورد انتظار عوض شده. این را با اجرای تست‌های موجود قبل و بعد از تغییر، به‌طور صریح در گزارش مقایسه کن.
- تابع `buildReferenceImageInstruction` باید کاملاً مستقل و به‌سادگی قابل‌تست باشد (ورودی: لیست + پروفایل؛ خروجی: `String?`) — بدون وابستگی به I/O یا فایل واقعی.
- 🆕v3 این نسخه صرفاً **مستندسازی** جدول را با کد واقعی (طبق ADR-021) هماهنگ کرد؛ هیچ تغییر کد جدیدی لازم نیست مگر با grep کشف شود که کد واقعی هم فهرست متفاوتی (مثلاً شامل `Sora` یا فاقد `Pika`) دارد — که بعید است، چون کاربر صراحتاً `Sora` را رد کرد و `Pika` را در قدم اجرایی اصلی پذیرفت. اگر چنین ناهماهنگی در کد واقعی دیده شد (نه در متن بلوپرینت)، آن را به‌عنوان یک یافته‌ی جدید گزارش بده، خودسرانه اصلاح نکن.
