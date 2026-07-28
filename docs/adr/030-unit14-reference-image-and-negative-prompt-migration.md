# ADR-030: Migration واحد ۱۴ (Output Delivery) — Reference Image Instruction + اتصال negativePrompt

**تاریخ:** 2026-07-28
**وضعیت:** کامل شد. پروژه کامل کامپایل می‌شود و تست می‌گذراند (۴۳۴ تست، ۰ Failure، ۰
Error — `gradle :app:assembleDebug :app:testDebugUnitTest`، `BUILD SUCCESSFUL`).

## Context

`docs/blueprints/14-output-delivery-v2.md` (نسخه ۳) دو کمبود واقعی را مستند می‌کند:

1. `renderBlueprintToText` هرگز به `PromptBlueprint.imageReferences` نگاه نمی‌کرد —
   یعنی وقتی یک Asset با تصویر رفرنس به یک Shot متصل است، پرامپت نهایی هیچ‌وقت به مدل
   نمی‌گفت که باید از آن تصویر استفاده کند.
2. `negativePrompt` (ADR-028، Migration واحد ۰۵) — `resolveNegativePrompt` نوشته شده بود
   اما به هیچ فیلد واقعی در مسیر Blueprint→Render وصل نبود.

## بخش الف — `buildReferenceImageInstruction`

پیاده‌سازی عیناً طبق کد مفهومی بلوپرینت: تابع مستقل `(imageReferences, profile) -> String?`
که فقط وقتی لیست غیرخالی است **و** `profile.capabilities.supportsImagePrompt` باشد یک
جمله‌ی دستوری کلی (مفرد/جمع بسته به تعداد) برمی‌گرداند — هرگز نام فایل/مسیر را درج
نمی‌کند (طبق تصمیم بنیادی `ReferenceImage` واحد ۰۶).

`renderBlueprintToText` این دستور را در **جایگاه دوم** لیست `segments` وارد می‌کند
(`segments.add(1, referenceInstruction)`)، نه انتهای پرامپت — طبق تأکید صریح بلوپرینت
که این یک دستور راهنمای ادراک بصری است، نه جزئیات فرعی.

### بررسی Backward Compatibility (طبق الزام صریح دستور کار)

با grep در `RendererTest.kt` قبل از تغییر: تابع کمکی `blueprint()` همیشه
`imageReferences = emptyList()` می‌ساخت و **هیچ تست موجودی** سناریوی `imageReferences`
غیرخالی را پوشش نمی‌داد. یعنی تغییر `segments.add(1, ...)` هیچ تست موجودی را نمی‌شکند —
این یک یافته‌ی واقعی است، نه فرض. تست‌های جدید (`renderBlueprintToText inserts the
reference image instruction near the start...`) این سناریو را برای اولین بار پوشش
می‌دهند، با مقایسه‌ی صریح متن قبل/بعد از افزودن instruction در همان تست.

### `supportsImagePrompt` روی هر ۱۳ پروفایل (بررسی، بدون تغییر)

با grep تأیید شد هر ۱۳ پروفایل واقعی (`ModelProfiles.kt`) از قبل `supportsImagePrompt =
true` دارند (به‌جز `universal_default` که عمداً `false` است) — این بخش از پیش انجام شده
بود، طبق ادعای دستور کار؛ چیزی برای تغییر نبود.

## بخش ب — اتصال `negativePrompt` (تکمیل ADR-028)

بلوپرینت v3 نمونه‌ی کد مشخصی برای این اتصال نداد (فقط فیلد `supportsNegativePrompt` را
در `ModelCapabilities` تعریف کرده بود) — طراحی مسیر واقعی بر عهده‌ی این قدم بود، با تکیه
بر پیشنهاد خودِ ADR-028.

### تصمیم ۱: فیلد جدید روی کجا؟ `PromptBlueprint`، نه `PromptGenerationInput`

ADR-028 هر دو گزینه را مطرح کرده بود. تصمیم: `PromptBlueprint.negativePrompt: String =
""`. دلیل: `PromptGenerationInput` از قبل هم `shot` (با `negativePromptOverride`) و هم
`dna` (با `qualityDirectives.negativePrompt`) را دارد — یعنی مواد خام لازم برای
`resolveNegativePrompt` از قبل در دسترس‌اند، نیازی به فیلد اضافه روی این نوع نیست.
`PromptBlueprint` خروجی «نهاییِ Resolve‌شده»ی واحد ۱۱ است — دقیقاً همان الگویی که
`seed`/`conflictsResolved`/`warnings` از قبل دنبال می‌کنند (مقدار خام جای دیگر، مقدار
Resolve‌شده اینجا). پیش‌فرض `""` (نه `null`) چون `resolveNegativePrompt` خودش هم همیشه
`String` غیر-nullable برمی‌گرداند.

### تصمیم ۲: کدام لایه `resolveNegativePrompt` را صدا می‌زند؟ `assemblePromptBlueprint`

با grep در `data/repository/PromptGenerationRepository.kt` تأیید شد `collectData` فقط
داده‌ی خام (`shot`, `dna`, ...) را جمع می‌کند و خودش هیچ منطق Resolve‌ای اجرا نمی‌کند
(این منطق در `assemblePromptBlueprint`، واحد ۱۱، انجام می‌شود — جایی که `seed`/
`conflictsResolved` هم Resolve می‌شوند). پس `assemblePromptBlueprint`
(`domain/promptengine/PromptAssembly.kt`) محل منطقی فراخوانی `resolveNegativePrompt(
input.shot, input.dna.qualityDirectives.negativePrompt)` است — نه `collectData`. این
یک import جدید از `domain.shot` به `domain.promptengine` اضافه کرد که وابستگی تازه‌ای
نیست: `PromptEngineModels.kt` از قبل به `domain.shot.Shot`/`ImageReference` وابسته است.

### تصمیم ۳: اعمال در Renderer — فقط وقتی مدل پشتیبانی می‌کند، بی‌صدا نادیده گرفته می‌شود

با grep تأیید شد **هیچ‌جای** `Renderer.kt` قبلاً `ModelCapabilities.supportsNegativePrompt`
را چک نمی‌کرد (فقط تعریف/مقداردهی شده بود، هرگز خوانده نمی‌شد). اضافه شد:

- **مسیر متن** (`renderBlueprintToText`): اگر `blueprint.negativePrompt.isNotBlank() &&
  profile.capabilities.supportsNegativePrompt`، یک segment `"Negative prompt: ..."` قبل
  از تزریق دستور عکس رفرنس اضافه می‌شود (که segment های بعدی را جابه‌جا می‌کند؛ ترتیب
  نسبی timeline→audio→negativePrompt→[reference instruction در index ۱] حفظ شده است).
- **مسیر JSON** (`buildJsonPrompt`): همان شرط، به‌عنوان یک کلید جدا `"negativePrompt"` در
  انتهای Object (بعد از `weightedEmphasis`).
- وقتی مدل پشتیبانی نمی‌کند یا مقدار خالی/whitespace-only است: هیچ تغییری در خروجی —
  دقیقاً طبق تصریح بلوپرینت ۰۲ («بی‌صدا نادیده گرفته می‌شود، نه خطا»).

## بخش ج — کامنت هدر

هر ۵ فایل `domain/outputdelivery/*.kt` که به `docs/blueprints/14-output-delivery.md`
(بدون `-v2`) اشاره می‌کردند (`Renderer.kt`, `ModelProfileLibrary.kt`, `ModelProfiles.kt`,
`OutputComposer.kt`, `Bilingual.kt`) به `14-output-delivery-v2.md (نسخه ۳)` اصلاح شدند.

## فهرست ۱۳ Model Profile واقعی (بررسی، بدون یافته‌ی جدید)

با grep تأیید شد فهرست واقعی `ModelProfiles.kt` (Veo, Kling, Seedance, HappyHorse,
Runway, Luma, Hailuo, Midjourney, Wan, HunyuanVideo, LTX, Vidu, Stable Diffusion) دقیقاً
با فهرست به‌روزشده‌ی بلوپرینت v3 یکی است — هیچ ناهماهنگی‌ای پیدا نشد (طبق پیش‌بینی
بلوپرینت که این ناهماهنگی «بعید» است).

## تست

- ۵ تست جدید برای `buildReferenceImageInstruction` (خالی→null، مدل بدون پشتیبانی→null،
  یک تصویر→مفرد، چند تصویر→جمع، هرگز نام فایل را درج نمی‌کند).
- ۲ تست برای تزریق در `renderBlueprintToText` (جایگاه صحیح + عدم تزریق وقتی پشتیبانی
  نمی‌شود) — با مقایسه‌ی صریح متن قبل/بعد در همان تست.
- ۳ تست برای `negativePrompt` در مسیر متن (اعمال، عدم اعمال بدون پشتیبانی، عدم اعمال
  وقتی خالی است).
- ۲ تست برای `negativePrompt` در مسیر JSON.
- ۲ تست در `PromptAssemblyTest.kt` برای Resolve واقعی end-to-end (fallback به DNA،
  اولویت override شات).

جمعاً ۱۴ تست جدید (۴۲۰ → ۴۳۴).

## Consequences

- **آسان می‌شود:** واحد ۱۴ اکنون هم دستور استفاده از تصویر رفرنس و هم negativePrompt
  Resolve‌شده را در مسیر واقعی Blueprint→Render اعمال می‌کند؛ هر دو کمبود مستندشده در
  بلوپرینت v3 و ADR-028 برطرف شدند.
- **بدون بدهی جدید شناخته‌شده** از این قدم.
