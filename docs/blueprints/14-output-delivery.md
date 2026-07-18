# واحد ۱۴: تحویل خروجی (Output Delivery System)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**ادغام از:** Output Target Profiles (Model Profile Library) + Output Composer + Bilingual System
**وضعیت:** فعال — **دومین واحد کلیدی معماری** (Renderer قلب سیستم)
**وابستگی:** Prompt Engineering Core (واحد ۱۱)، Prompt Finalization Pipeline (واحد ۱۳)

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

## بخش الف — Model Profile Library

### فلسفه: Data-driven، نه هاردکد

هر پروفایل مدل (JSON قابل‌ویرایش) شامل: `capabilities` (قابلیت‌ها)، `constraints` (محدودیت‌ها)، `parameters` (پارامترهای قابل‌تنظیم)، `optimization_rules`. این طراحی یعنی افزودن/تغییر یک مدل بدون Build مجدد اپ ممکن است.

### پروفایل Universal/Default (همیشه موجود، fallback)

```json
{
  "profile_id": "universal_default",
  "platform": "generic",
  "capabilities": { "supports_video": true, "supports_image": true, "supports_weighted_tags": false },
  "constraints": { "max_prompt_length": 2000, "max_tokens": 500 },
  "parameters": { "format": { "type": "plain_text", "structure": "paragraph" } },
  "optimization_rules": { "avoid_platform_specific_syntax": true }
}
```

این پروفایل هیچ نحو خاص پلتفرمی اعمال نمی‌کند — فقط توصیف متنی واضح از تمام `structured_parts` می‌سازد.

### کتابخانه‌ی پروفایل‌ها (به‌روز، جولای ۲۰۲۶)

⚠️ **یادداشت مهم درباره‌ی این جدول:** بازار مدل‌های ویدیوساز AI بسیار سریع تغییر می‌کند (مدل‌ها می‌آیند، منسوخ می‌شوند، قیمت‌ها عوض می‌شوند). به همین دلیل **اصل Data-driven بودن این کتابخانه حیاتی است** — جدول زیر یک نقطه‌ی شروع واقعی بر اساس وضعیت بازار در جولای ۲۰۲۶ است، نه یک فهرست ثابت و نهایی. باید بتوان بدون تغییر کد Kotlin، فایل JSON مدل‌های جدید را اضافه یا مدل‌های منسوخ را غیرفعال (`active: false`) کرد.

| مدل | سازنده | نوع فرمت | ویژگی خاص | وضعیت |
|---|---|---|---|---|
| **Veo 3.1** | Google | JSON ساختاریافته | صدای همزمان با لب (lip-sync)، بهترین گزینه‌ی سینمایی غربی | فعال |
| **Sora 2** | OpenAI | JSON | فیزیک واقع‌گرایانه‌ی حرکت | ⚠️ **در حال توقف** — اپ مصرفی از آوریل ۲۰۲۶ بسته شده؛ API تا سپتامبر ۲۰۲۶ خاموش می‌شود. نباید پایپ‌لاین جدید روی آن ساخته شود |
| **Kling 3.0** | Kuaishou | JSON | ارزان‌ترین گزینه‌ی باکیفیت، کلیپ تا چند دقیقه، Multilingual Lip-sync | فعال |
| **Runway Gen-4.5** | Runway | Structured Text | بهترین کنترل خلاقانه (Motion Brushes)، محیط ویرایش کامل | فعال |
| **Seedance 2.0** | ByteDance | JSON | رتبه‌ی اول کیفیت خام؛ دسترسی از طریق Doubao/fal.ai | فعال (دسترسی محدودتر) |
| **Luma Ray 3** | Luma AI | JSON | بهترین HDR، مناسب Image-to-Video اتمسفریک | فعال |
| **Hailuo (MiniMax) 2.3** | MiniMax | JSON | بهترین ثبات ظاهری کاراکتر بین شات‌ها | فعال |
| **Pika 2.5** | Pika Labs | JSON ساده | تکرار سریع، مناسب شبکه‌های اجتماعی، کنترل دوربین محدود | فعال |
| **Midjourney v6/v7** (تصویر، نه ویدیو) | Midjourney | Command String | مرجع تصویر ثابت با کنترل دقیق سبک | فعال |
| **Stable Diffusion XL / SD3** | Stability AI | JSON | Open-weight، ControlNet، اجرای محلی ممکن | فعال |
| **Universal / Default** | — | Plain Text | Fallback عمومی برای هر مدل شناخته‌نشده یا جدید | همیشه فعال |

**نکته‌ی عملی:** طبق قانون خودِ این معماری، Claude Code یا هر توسعه‌دهنده نباید این جدول را به‌عنوان فهرست ثابت فرض کند — قبل از پیاده‌سازی واقعی هر پروفایل، باید مستندات رسمی همان مدل (قیمت، محدودیت طول، پارامترهای واقعی API) به‌روز بررسی شود، چون این اطلاعات به‌سرعت تغییر می‌کنند.

### نمونه پروفایل کامل — Veo 3.1 (نمونه‌ی الگو برای مدل‌های JSON)

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
    "supports_weighted_tags": false
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

⚠️ **هشدار عملی برای Sora:** طبق وضعیت بازار جولای ۲۰۲۶، پروفایل Sora باید با `active: false` یا یک پرچم هشدار ثبت شود تا کاربر بداند این مسیر رو به توقف است (API تا سپتامبر ۲۰۲۶ خاموش می‌شود) — نباید کاربر را به سمت شروع یک پروژه‌ی تازه بر پایه‌ی Sora هدایت کرد بدون این‌که این هشدار را ببیند.

### پیاده‌سازی مفهومی (Kotlin)

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

/** قلب Renderer: تبدیل structured_parts به یک متن پایه بر اساس ترجیح ساختاری پروفایل. */
fun renderBlueprintToText(blueprint: PromptBlueprint, profile: ModelProfile): String {
    val parts = blueprint.structuredParts
    val segments = listOfNotNull(
        parts.subjectDescription, parts.sceneContext, parts.shotDescription,
        parts.cameraSpecs, parts.lightingSpecs, parts.environmentSpecs, parts.styleModifiers
    ).toMutableList()

    if (parts.timelineBeats != null && profile.capabilities.supportsVideo) segments += "Timeline: ${parts.timelineBeats}"
    if (parts.audioDescription != null && profile.capabilities.supportsVideo) segments += "Audio: ${parts.audioDescription}"

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

/** خروجی نهایی برای یک پروفایل خاص — شامل تزریق image_references به فرمت مخصوص همان پلتفرم. */
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

### قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| Profile برای پلتفرم درخواستی یافت نشد | باید Fallback به Universal شود یا خطا بدهد | **Blocking** |
| طول متن Render شده بیشتر از `max_prompt_length` | نیاز به کوتاه‌سازی یا هشدار | **Warning** (خودکار کوتاه می‌شود) |
| استفاده از فیچر پشتیبانی‌نشده (مثل Timeline Beats برای مدلی که ویدیو ندارد) | آن بخش نادیده گرفته می‌شود | **Warning** |

---

## بخش ب — Output Composer (بسته‌بندی نهایی)

**وظیفه:** فقط بسته‌بندی خروجی‌های آماده؛ **هیچ منطق تولید محتوا یا فرمت‌دهی مدل ندارد** — آن‌ها قبلاً در بخش الف انجام شده‌اند.

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

## بخش ج — Bilingual System (دوزبانگی)

⚠️ **دامنه‌ی این بخش دو چیز کاملاً متفاوت است — نباید با هم اشتباه شوند:**

1. **دوزبانگی خروجی پرامپت** (که در بخش‌های الف/ب همین سند پوشش داده شد): تولید نسخه‌ی EN و FA از پرامپت نهایی.
2. **دوزبانگی خودِ اپ (UI)** — که موضوع این بخش است: **تمام صفحات، دکمه‌ها، پیام‌های خطا، برچسب‌های فرم، و متن‌های قابل‌مشاهده در سرتاسر اپ** باید دوزبانه باشند. این یک سیستم **سراسری (System-wide)** است که تمام ۱۶ واحد دیگر (هر جا که UI دارند) به آن وابسته‌اند — دقیقاً مثل اسناد اولیه‌ی این معماری که صراحتاً می‌گفتند: *«Dependencies: All UI Modules»*.

### اصل بنیادی: هر دو زبان معتبر، بدون برتری پیش‌فرض

سیستم **همیشه هر دو نسخه** (فارسی + انگلیسی) پرامپت را تولید می‌کند. **کاربر تصمیم می‌گیرد** کدام نسخه به مدل داده شود — نه یک فرض ثابت «انگلیسی همیشه بهتر است»، چون مدل‌های جدید با هر دو زبان به‌خوبی کار می‌کنند.

```kotlin
enum class Language { FA, EN }

data class LanguagePreferences(
    val uiLanguage: Language = Language.FA,
    val promptLanguage: Language = Language.FA,   // بر اساس UI Language؛ بدون فرض ثابت
    val fallbackLanguage: Language = Language.EN  // فقط برای ترجمه‌ی UI Labels، نه Prompt
)

/** تولید هر دو نسخه؛ هیچ‌کدام به‌طور پیش‌فرض «نسخه‌ی اصلی» نیست. */
fun generateBilingualPrompt(englishText: String): BilingualPrompts {
    return BilingualPrompts(enVersion = englishText, faVersion = translateToFarsi(englishText))
}
```

**نکته مهم درباره‌ی `fallbackLanguage`:** این فیلد فقط برای ترجمه‌ی رابط کاربری (UI Labels) است — اگر کلیدی در زبان انتخابی کاربر پیدا نشد، به انگلیسی برمی‌گردد تا UI خالی/خراب نشود. این ربطی به `promptLanguage` ندارد و به‌معنای «انگلیسی برتر است» نیست.

### پوشش کامل — تمام ۱۶ واحد، نه چند Label نمونه

هر واحدی که در `docs/blueprints/` صفحه، فرم، پیام خطا، یا هر متن قابل‌مشاهده‌ی دیگری دارد، باید کلیدهای ترجمه‌ی خودش را در فایل‌های زیر داشته باشد. **هیچ رشته‌ی متنی قابل‌مشاهده در کد UI (Composable ها) نباید مستقیم Hardcode شود** — همه باید از طریق تابع `t()` عبور کنند.

```
/locales
  ├─ fa.json
  │   ├─ common          → دکمه‌های عمومی (ذخیره/لغو/حذف/ویرایش)
  │   ├─ story_wizard     → واحد ۰۱ (سؤالات، گزینه‌ها)
  │   ├─ dna_manager      → واحد ۰۲ (فیلدهای DNA، هشدار Soft Lock)
  │   ├─ visual_identity  → واحد ۰۳ (انتخاب سبک، Cinematic Language)
  │   ├─ scene            → واحد ۰۴ (فیلدهای Scene)
  │   ├─ shot             → واحد ۰۵ (فیلدهای Shot، Beat Sheet)
  │   ├─ asset_library    → واحد ۰۶ (کاراکتر/مکان/شیء، هشدار Hard Lock)
  │   ├─ validation       → واحد ۰۷ (پیام‌های Blocking/Warning)
  │   ├─ scene_conditions → واحد ۰۸ (نور، آب‌وهوا)
  │   ├─ camera_motion    → واحد ۰۹ (تنظیمات دوربین)
  │   ├─ audio            → واحد ۱۰ (توصیف صدا)
  │   ├─ output_delivery  → همین واحد (انتخاب مدل، Copy/Export)
  │   ├─ project_storage  → واحد ۱۵ (پیام‌های ذخیره/بازیابی)
  │   ├─ workflow         → واحد ۱۶ (مراحل گردش کار)
  │   └─ errors           → پیام‌های خطای سراسری (مشترک بین همه‌ی واحدها)
  └─ en.json
      └─ (همان ساختار، به انگلیسی)
```

**قانون:** وقتی بلوپرینت یک واحد جدید نوشته یا ویرایش می‌شود، بخش ترجمه‌ی همان واحد (`fa.json`/`en.json`) باید هم‌زمان به‌روزرسانی شود — دقیقاً طبق اصل «مستندات هم‌زمان با کد» که در `governance/ai-coding-guidelines.md` آمده.

### تابع ترجمه (مستقل از پلتفرم، با پوشش کامل)

```kotlin
/** تابع ترجمه‌ی UI با Fallback؛ کاملاً مستقل از prompt_language. باید در تمام Composable ها به‌جای متن مستقیم استفاده شود. */
fun t(key: String, uiLanguage: Language, translations: Map<Language, Map<String, String>>): String {
    return translations[uiLanguage]?.get(key)
        ?: translations[Language.EN]?.get(key)   // fallback فقط برای UI
        ?: key   // در حالت توسعه، خودِ کلید نمایش داده می‌شود تا کلید گم‌شده فوراً مشخص شود
}

// نمونه‌ی استفاده‌ی صحیح در یک Composable (نه رشته‌ی Hardcode):
// Text(text = t("shot.description_label", uiLanguage, translations))
// نمونه‌ی نادرست (ممنوع):
// Text(text = "توضیح شات")
```

### RTL/LTR (معادل اندروید) — باید در تمام صفحات اعمال شود

```kotlin
// معادل مفهومی برای کل اپ در Jetpack Compose (نه فقط یک صفحه):
// CompositionLocalProvider(LocalLayoutDirection provides
//     if (uiLanguage == Language.FA) LayoutDirection.Rtl else LayoutDirection.Ltr) {
//     AppNavigation()  // تمام صفحات اپ، نه یک Composable منفرد
// }
```

### قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| کلید ترجمه در هیچ‌کدام از دو زبان یافت نشود | خودِ کلید نمایش داده می‌شود (برای Debug) | **Warning** |
| زبان درخواستی خارج از FA/EN | پشتیبانی نمی‌شود | **Blocking** |
| رشته‌ی متنی Hardcode در کد UI (بدون عبور از `t()`) | نقض اصل پوشش کامل i18n | ساختاری (باید در Code Review تشخیص داده شود) |
| افزودن یک واحد/صفحه‌ی جدید بدون کلیدهای ترجمه‌ی متناظر | ناقص‌ماندن پوشش دوزبانگی | **Blocking** (قبل از Merge) |

### معیار پوشش (Coverage)

طبق سند اصلی، هدف **۱۰۰٪ پوشش** برای هر دو زبان است — یعنی هیچ کلیدی نباید در یکی از دو فایل (`fa.json` یا `en.json`) وجود داشته باشد و در دیگری غایب باشد. این با یک تست خودکار ساده (مقایسه‌ی کلیدهای دو فایل) قابل تضمین است:

```kotlin
/** تست ساختاری: باید در CI اجرا شود تا اطمینان دهد دو فایل زبان دقیقاً هم‌ساختارند. */
fun validateTranslationCoverage(faKeys: Set<String>, enKeys: Set<String>): List<String> {
    val missingInEn = faKeys - enKeys
    val missingInFa = enKeys - faKeys
    return (missingInEn.map { "کلید '$it' در en.json موجود نیست" } +
            missingInFa.map { "کلید '$it' در fa.json موجود نیست" })
}
```

---

## معماری کلی سه‌لایه (Clear Separation of Concerns)

| جنبه | Prompt Engineering Core (واحد ۱۱) | Output Delivery System (این واحد) | 
|---|---|---|
| مسئولیت | حل‌تضاد و ترکیب داده | Render برای مدل خاص + بسته‌بندی + دوزبانگی |
| ورودی | Scene, Shot, DNA data | PromptBlueprint + Model Profile |
| خروجی | PromptBlueprint خنثی | متن فرمت‌شده + بسته‌ی نهایی Export |
| آگاهی از مدل | ندارد | دارد (از Model Profile) |

---

## معیارهای موفقیت

- Model Profile Library به‌صورت JSON قابل‌ویرایش است؛ افزودن مدل جدید بدون تغییر کد ممکن است.
- پروفایل Universal همیشه به‌عنوان Fallback در دسترس است.
- Output Composer هرگز محتوای `rendered_output` دریافتی را تغییر نمی‌دهد.
- هر دو نسخه‌ی زبانی همیشه تولید می‌شوند؛ کاربر بدون فشار به سمت یکی، انتخاب می‌کند.
- یک PromptBlueprint می‌تواند بدون تکرار حل‌تضاد، برای چند مدل مختلف Render شود.
- **دوزبانگی UI سراسری است:** تمام ۱۶ واحد دیگر (هر جا صفحه/فرم/پیام دارند) کلیدهای ترجمه‌ی خودشان را در `fa.json`/`en.json` دارند؛ هیچ رشته‌ی Hardcode در کد UI باقی نمانده است.
- پوشش دو فایل زبان همیشه ۱۰۰٪ هم‌ساختار است (تست خودکار `validateTranslationCoverage` این را تضمین می‌کند).
