# ADR-037: واحد ۱۶ (User Workflow) — `domain/workflow/` و `evaluatePromptQuality`

**تاریخ:** 2026-07-28
**وضعیت:** کامل شد و مستقل قابل‌کامپایل/تست است. `gradle :app:assembleDebug
:app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۵۱۷ تست، ۰ Failure، ۰ Error.

## Context

این Migration مستقیماً یافته‌ی F1 ممیزی `docs/audit/pre-unit16-audit.md` را رفع
می‌کند: `domain/workflow/` کاملاً غایب بود، در حالی که `docs/reference/type-registry.md`
(بخش «واحد ۱۶ — User Workflow»، خطوط ۲۹۳–۳۰۲) از قبل جدول کامل این واحد را با وضعیت
✅ («تکمیل‌شده») نشان می‌داد. طبق دستور صریح معمار، جدول موجود درست فرض شد و کد به آن
مطابق شد — نه برعکس.

## پیاده‌سازی `domain/workflow/WorkflowModels.kt`

`WorkflowStep`، `StepStatus`، `ShotListViewMode`، `FeedbackType`، `WorkflowState`
(با `progressPercentage`)، `canJumpToStep` — همگی عیناً طبق کد مفهومی بلوپرینت ۱۶
کپی شدند، بدون تفسیر شخصی. هر ۵ نوع/enum دقیقاً با نام و مقادیر ردیف متناظرشان در
`type-registry.md` یکی هستند (تأیید شده با مقایسه‌ی مستقیم).

## تصمیم: Option A برای `evaluatePromptQuality`

بلوپرینت این تابع را `TODO()` گذاشته بود، اما بر خلاف `diagnoseJsonError` (واحد ۰۱ب)
که توضیح متنی دقیقی برای الگوریتم داشت، اینجا فقط پنج محور کیفی با یک جمله‌ی توضیحی
هرکدام آمده بود — بدون فرمول یا آستانه‌ی مشخص.

**تصمیم: Option A (پیاده‌سازی حداقلی واقعی، نه TODO).** دلایل:

1. ورودی‌های لازم واقعاً در دسترس‌اند: `blueprint.structuredParts` (۹ فیلد متنی) و
   `renderedOutput.formattedPrompt` (متن نهایی رندرشده) — هر دو از قبل در واحدهای
   ۱۱/۱۴ کامل و تست‌شده وجود دارند.
2. خودِ بلوپرینت این تابع را «ابزار کمکی/اختیاری... نه بخشی الزامی از Validation»
   توصیف کرده — یعنی خطای جزئی در دقت الگوریتم هیچ Rule واقعی را نمی‌شکند و هیچ
   جریان کاری را مسدود نمی‌کند؛ ریسک پایین است، دقیقاً طبق ترجیح معمار.
3. برخلاف `sendToAiConnector` (ADR-035، Option A همان‌جا) که پیاده‌سازی واقعی‌اش
   نیازمند یک Dependency جدید (Ktor) و بررسی مستندات API خارجی بود، اینجا هیچ
   وابستگی بیرونی یا حجم کار غیرمنتظره‌ای وجود ندارد — یک تابع خالص محاسباتی است.

### الگوریتم (مستند، نه ادعای «تحلیل معنایی واقعی»)

- **`subjectClarity`/`cinematicClarity`/`styleCoherence`:** هرکدام از یک تابع مشترک
  `scoreTextRichness(text)` می‌آیند — سه سطح ساده بر اساس طول متن پس از `trim()`:
  خالی→۰، کوتاه‌تر از ۱۰ کاراکتر→۵، کوتاه‌تر از ۲۵→۱۲، وگرنه→۲۰. این یک **تخمین طول
  متن** است، نه تحلیل معنایی واقعی محتوا — محدودیت شناخته‌شده و پذیرفته‌شده، منطبق
  با سطح «ابزار کمکی» که بلوپرینت خواسته.
- **`visualSpecificity`:** میانگین `scoreTextRichness(lightingSpecs)` و
  `scoreTextRichness(environmentSpecs)` — اما `environmentSpecs` (طبق
  `StructuredParts`، واحد ۱۱) وقتی `null` است به‌معنای «آب‌وهوای Clear» است (یک حالت
  کاملاً معتبر)، نه داده‌ی جاافتاده؛ پس وقتی `null` باشد، به‌جای صفر کردن نیمی از
  امتیاز، از همان امتیاز `lightingSpecs` به‌عنوان مقدار خنثی استفاده می‌شود — تا یک
  Shot با آب‌وهوای صاف و توصیف نوری غنی، به‌ناحق امتیاز پایین نگیرد.
- **`conciseness`:** بر اساس طول `renderedOutput.formattedPrompt` با چند آستانه‌ی
  ساده (≤۸۰۰→۲۰، ≤۱۵۰۰→۱۵، ≤۲۵۰۰→۱۰، بیشتر→۵) — تخمین ایجاز از روی طول کلی، نه
  تشخیص واقعی تکرار معنایی درون متن (که نیازمند یک الگوریتم NLP واقعی است، خارج از
  دامنه‌ی این ابزار کمکی).

## تست

`WorkflowModelsTest.kt` — ۹ تست: `canJumpToStep` (همه‌ی حالت‌های قبلی کامل→بدون
هشدار؛ ناقص→با هشدار؛ یک تست صریح که برای **هر** `WorkflowStep` هدف، خروجی هرگز
`false` نیست)؛ `progressPercentage` (۰٪، جزئی ۳۳٪، ۱۰۰٪)؛ `evaluatePromptQuality`
(ورودی غنی→امتیاز کل بالا؛ ورودی حداقلی/خالی→امتیاز کل پایین؛ یک تست مجزا که تأیید
می‌کند `environmentSpecs=null` امتیاز `visualSpecificity` را به‌ناحق پایین
نمی‌آورد).

جمعاً ۹ تست جدید (۵۰۸ → ۵۱۷).

## Consequences

- **آسان می‌شود:** یافته‌ی F1 ممیزی pre-Unit 16 کاملاً رفع شد؛ `domain/workflow/`
  اکنون دقیقاً با `type-registry.md` هم‌راستاست؛ واحد ۱۶ می‌تواند از `WorkflowState`/
  `canJumpToStep` برای Bottom Navigation Bar و از `evaluatePromptQuality` برای
  نمایش اختیاری «خودارزیابی کیفیت» استفاده کند.
- **بدون بدهی جدید شناخته‌شده:** الگوریتم `evaluatePromptQuality` صریحاً به‌عنوان
  یک تخمین ساده (نه تحلیل معنایی) در کامنت کد و این ADR مستند شد.
