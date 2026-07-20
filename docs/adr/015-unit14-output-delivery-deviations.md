# ADR-015: تصمیمات پیاده‌سازی واحد ۱۴ (Output Delivery System) — لایه‌ی دامنه

**تاریخ:** 2026-07-20
**وضعیت:** تمام تصمیمات مستقل (طبق دستور کار: جزئیات پیاده‌سازی محلی، نه سؤال معماری)

## Context

پیاده‌سازی لایه‌ی دامنه‌ی خالص واحد ۱۴ (Output Delivery System — «دومین واحد کلیدی معماری») طبق `docs/blueprints/14-output-delivery.md`، هر سه بخش: الف (Model Profile Library + Renderer)، ب (Output Composer)، ج (Bilingual System — فقط ساختار داده/منطق ترجمه). `PromptBlueprint`/`StructuredParts` از `domain.promptengine` (واحد ۱۱) import شدند؛ تمام فیلدهایی که کد این واحد می‌خواند (`subjectDescription`, `sceneContext`, `shotDescription`, `cameraSpecs`, `lightingSpecs`, `environmentSpecs`, `styleModifiers`, `timelineBeats`, `audioDescription`, `weightedEmphasis`) با grep تأیید شدند و دقیقاً با کد مفهومی بلوپرینت ۱۴ یکی بودند — هیچ نگاشت اسم یا ناسازگاری ساختاری لازم نشد.

## تصمیم مستقل ۱: جایگاه Pipeline — `composeOutput` هرگز `render()` را خودش صدا نمی‌زند

طبق نمودار Pipeline بلوپرینت (`PromptBlueprint → Renderer (بخش الف) → Prompt Finalization (واحد ۱۳، هنوز پیاده نشده) → Output Composer (بخش ب)`)، امضای `composeOutput` دقیقاً همان امضای بلوپرینت خودش را حفظ کرد: `renderedOutputs: List<RenderedOutput>` و `bilingualPrompts: BilingualPrompts` هر دو پارامتر ورودی‌اند، نه چیزی که خودِ تابع تولید کند. این یعنی همان لیست می‌تواند بعداً یا مستقیماً خروجی خام `render()` این واحد باشد، یا متن Clean‌شده‌ی واحد ۱۳ — بدون این‌که `composeOutput` نیاز به تغییر داشته باشد. هیچ تغییر امضایی لازم نبود چون بلوپرینت خودش از ابتدا همین طراحی را داشت.

## تصمیم مستقل ۲: هر Rule جدولی که «قبلاً در کد اصلی حل شده» یک تابع گزارشی جداگانه گرفت

هر سه بخش، Rule‌هایی دارند که خودِ تابع اصلی (`selectProfile`, `renderBlueprintToText`, `optimizeForProfile`, `t()`) از قبل به‌صورت graceful (Fallback خودکار، کوتاه‌سازی خودکار، نادیده‌گرفتن فیچر پشتیبانی‌نشده) رفتار درست را انجام می‌دهد — طبق الگوی مستقر این پروژه (مثلاً واحد ۰۸ mapMoodToLighting)، یک تابع `validateX` جداگانه فقط برای *گزارش* اتفاقی که رخ داده نوشته شد، بدون تغییر رفتار تابع اصلی:

- `validateProfileAvailability` — Blocking فقط وقتی که حتی `universal_default` هم در دسترس نیست (یعنی جایی که `selectProfile` واقعاً کرش می‌کرد، چون از `.first { ... }` بدون `null`-safety استفاده می‌کند).
- `validatePromptLength` — Warning وقتی متنِ پیش از کوتاه‌سازی از `maxPromptLength` بیشتر است؛ خودِ `optimizeForProfile` کوتاه‌سازی را انجام می‌دهد.
- `validateUnsupportedFeatureUsage` — Warning وقتی `timelineBeats`/`audioDescription` به‌خاطر `supportsVideo=false` نادیده گرفته می‌شوند؛ خودِ `renderBlueprintToText` این حذف را انجام می‌دهد.
- `validateBilingualCompleteness` — Warning وقتی یکی از دو نسخه‌ی زبانی خالی/Blank است (چون `BilingualPrompts` دو فیلد غیر-Nullable `String` دارد، نه Nullable — «موجود نبودن» یعنی Blank، نه null).
- `validateTranslationKeyFound` — Warning وقتی کلید در هیچ‌کدام از دو زبان نیست؛ خودِ `t()` به همان کلید Fallback می‌کند.

## تصمیم مستقل ۳: «رشته‌ی متنی Hardcode در UI» و «نقض بسته‌بندی بدون تغییر محتوا» — تضمین ساختاری، نه تابع Runtime

دو Rule از جدول‌های بلوپرینت («رشته‌ی متنی Hardcode در کد UI» بخش ج، و «محتوای `rendered_output` در حین بسته‌بندی تغییر کند» بخش ب) در خودِ بلوپرینت با برچسب «ساختاری (باید در Code Review تشخیص داده شود)» یا «ساختاری (باگ پیاده‌سازی)» مشخص شده‌اند — یعنی خودِ بلوپرینت هم این‌ها را Rule‌های Runtime نمی‌داند. طبق الگوی مستقر (ADR-006 واحد ۰۵، Rule 5 — «تضمین ساختاری، نه Runtime»):
- برای «نقض بسته‌بندی»: `composeOutput` هرگز `rendered.formattedPrompt` را نمی‌نویسد یا تغییر نمی‌دهد، فقط می‌خواند تا `ExportFile` بسازد — این با یک تست (`assertEquals(renderedOutputs, result.renderedOutputs)`) تأیید شد، نه با تابع `validateX` جداگانه.
- برای «Hardcode در UI»: این Rule اصلاً به لایه‌ی دامنه تعلق ندارد (کد UI/Compose در این قدم وجود ندارد) — فقط در همین ADR یادداشت شد، بدون تابع.

## تصمیم مستقل ۴: `validateLanguageSupported(languageCode: String)` — نه `Language` تایپ‌شده

Rule «زبان درخواستی خارج از FA/EN → Blocking» در سطح Type System همیشه غیرقابل‌نقض است اگر پارامتر از نوع `Language` (enum با فقط دو مقدار) باشد — دقیقاً مثل تضمین `UpdateResult` واحد ۰۶. برای این‌که این Rule واقعاً یک حالت Runtime تست‌پذیر داشته باشد (نه یک تابع که همیشه `null` برمی‌گرداند)، ورودی آن یک `String` خام گرفته شد — نمایانگر یک منبع بیرونی که هنوز اعتبارسنجی نشده (مثلاً Locale سیستم‌عامل). این تنها نقطه‌ای است که «زبان نامعتبر» واقعاً می‌تواند رخ دهد.

## تصمیم مستقل ۵ (خارج از Scope، عمداً پیاده نشد): `generateBilingualPrompt`/`translateToFarsi`

فهرست صریح کارهای این قدم برای بخش ج فقط `Language`، `LanguagePreferences`، `t()`، `validateTranslationCoverage`، و Rule‌های جدول را نام برده — `generateBilingualPrompt`/`translateToFarsi` بلوپرینت را نام نبرده. این عمداً پیاده نشد: `translateToFarsi` یک موتور ترجمه‌ی واقعی نیاز دارد (کدام سرویس/API، دقت، هزینه) که یک تصمیم معماری جداست، نه جزئیات پیاده‌سازی محلی این قدم. این حذف زنجیره‌ی Pipeline را نمی‌شکند، چون `composeOutput` (بخش ب) هم `bilingualPrompts: BilingualPrompts` را از بیرون می‌گیرد، نه این‌که خودش تولید کند — دقیقاً هم‌راستا با تصمیم مستقل ۱.

## تصمیم مستقل ۶: دو فیلد `ModelCapabilities` در `universalDefaultProfile` که در JSON نمونه‌ی بلوپرینت نیامده‌اند

نمونه‌ی JSON بلوپرینت برای `universal_default` فقط ۳ فیلد (`supports_video`, `supports_image`, `supports_weighted_tags`) از ۵ فیلد `ModelCapabilities` کد مفهومی Kotlin را ذکر کرده. دو فیلد باقی‌مانده (`supportsImagePrompt`, `supportsNegativePrompt`) با مقدار محافظه‌کارانه‌ی `false` پر شدند — این پروفایل قابلیت خاصی برای آن‌ها ادعا نکرده، پس امن‌ترین مقدار پیش‌فرض `false` است.

## Consequences

- **آسان می‌شود:** واحد ۱۴ اکنون می‌تواند یک `PromptBlueprint` واقعی واحد ۱۱ را برای چند پروفایل مختلف Render کند و بسته‌بندی نهایی بسازد، همه بدون I/O واقعی.
- **بدهی باقی‌مانده:** فقط `universal_default` پیاده شده — پروفایل‌های JSON واقعی مدل‌های دیگر (Veo, Kling, Runway, …) پیاده نشدند (خارج از Scope این قدم)؛ `generateBilingualPrompt`/`translateToFarsi` هنوز پیاده نشده (منتظر تصمیم معماری درباره‌ی موتور ترجمه)؛ اتصال واقعی به واحد ۱۳ (Prompt Finalization) هنوز برقرار نشده چون آن واحد پیاده نشده.
