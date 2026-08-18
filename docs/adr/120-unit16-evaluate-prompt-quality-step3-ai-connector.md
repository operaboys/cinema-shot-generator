# ADR-120: هوشمندسازی evaluatePromptQuality — قدم ۳ از ۳ زیرقدم (پایانی): دکمه‌ی «تحلیل عمیق‌تر با AI» روی `PromptQualityCard` — همان معماری امنیتی G2 AI Connector، برای صفحه‌ای دیگر

## زمینه

این ADR سومین و **آخرین** زیرقدم از برنامه‌ی «هوشمندسازی و اتصال
evaluatePromptQuality» است (ADR-118 → ADR-119 → این ADR):

- **ADR-118 (قدم ۱)**: خودِ `evaluatePromptQuality`/`scoreTextRichness`
  با دو سیگنال تحقیق‌شده (کلمات مبهم، MATTR) هوشمندتر شد.
- **ADR-119 (قدم ۲)**: نتیجه به مسیر ۱ (بدون AI) `OutputDeliveryScreen`
  وصل شد — کارت «شاخص کیفیت نوشتاری پرامپت (سریع نه دقیق)».
- **این قدم (قدم ۳، پایانی)**: مسیر ۲ اختیاری — دکمه‌ی «تحلیل عمیق‌تر
  با AI» که همان پرامپت را به یکی از سرویس‌های AI (Claude/OpenAI/
  Gemini/DeepSeek/Qwen) می‌فرستد و تحلیل کیفی متنی واقعی سرویس را
  نمایش می‌دهد. با این ADR، کل برنامه‌ی سه‌قدمی بسته می‌شود.

معمار به‌صراحت خواسته بود این قدم معماری امنیتی G2 (`AiStoryBreakdownViewModel`/
`AiStoryBreakdownScreen`، ADR-098–105) را **عیناً تکرار** کند، نه یک
معماری موازی تازه اختراع کند.

## بررسی مستقل — نتیجه: بدون هیچ انحراف واقعی از پیش‌بریفینگ

پیش از نوشتن هر خط کد، طبق الزام صریح دستور کار، فایل‌های زیر به‌طور
کامل خوانده شدند: `AiStoryBreakdownViewModel.kt` (کامل، خصوصاً
`refreshApiKeySavedStatus` خط ۱۸۶-۱۸۸، `selectProfile` خط ۱۹۰-۱۹۷،
`sendPromptAutomatically` خط ۳۳۰-۳۳۷)، `AiStoryBreakdownScreen.kt`
(کامل، خصوصاً `GeneratedPromptCard` خط ۵۰۵-۵۶۳)،
`domain/storybreakdown/AiConnector.kt` (`sendToAiConnector`،
`validateApiKeyProvided`، `validateAiConnectorErrorMessage`)،
`data/repository/SecureKeyRepository.kt`، و
`OutputDeliveryViewModel.kt`/`OutputDeliveryScreen.kt` پس از ADR-119.

**تأیید شد**: تمام شماره‌خط‌ها و امضاهای تابعِ ذکرشده در پیش‌بریفینگ با
کد واقعی مطابقت داشتند — بدون هیچ جابه‌جایی. **بدون هیچ انحراف واقعی.**

همچنین با `grep` تأیید شد `OutputDeliveryState.Ready(...)` دقیقاً در
یک نقطه از کل کدبیس ساخته می‌شود (خودِ `regenerate()`)، پس افزودن
فیلدهای StateFlow تازه به ViewModel هیچ تست دیگری را تحت تأثیر قرار
نمی‌داد.

## قاعده‌ی امنیتی — تصریح صریح و تکراری کاربر پروژه، نه پیشنهاد Claude Code

**این قاعده را معمار/کاربر پروژه به‌صراحت و به‌طور تکراری در طول این
جلسه (و ازجمله در همین دستور کار) بیان کرده: «تا زمانی که کلید API
به‌صورت امن ذخیره نشده، مسیر AI Connector باید خاموش/غیرفعال باشد.»**
این یک تصمیم طراحی از طرف Claude Code نیست — عیناً همان قاعده‌ای است
که در G2 (ADR-101) پیاده شد و اینجا **بدون کوچک‌ترین تغییر** برای این
صفحه‌ی دوم تکرار شده است:

- دکمه‌ی «تحلیل عمیق‌تر با AI» از **همان اولین رندر** (نه فقط پس از
  کلیک) با `enabled = apiKeySavedForQualityAnalysis && !qualityAnalysisInProgress`
  غیرفعال است — یک بررسی پیشگیرانه (`hasApiKey` پیش از کلیک)، نه صرفاً
  واکنش به خطای پس از کلیک.
- `analyzePromptQualityWithAi()` نیز مستقل و در سطح ViewModel همین
  قاعده را دوباره اجرا می‌کند (`apiKey == null || validateApiKeyProvided(apiKey) != null`
  → خطا و بازگشت فوری، **پیش از** `_qualityAnalysisInProgress.value = true`
  و پیش از هرگونه فراخوانی `sendToAiConnector`) — یعنی حتی اگر یک باگ
  فرضی در UI دکمه را غیرفعال نکند، لایه‌ی ViewModel هم مستقل از HTTP
  واقعی جلوگیری می‌کند (Defense in depth، هم‌الگو با Rule 4 در G2).

## پیاده‌سازی

**۱. `OutputDeliveryViewModel.kt`:**
- دو وابستگی تازه‌ی سازنده (هم‌الگوی دقیق `AiStoryBreakdownViewModel`):
  `secureKeyRepository: SecureKeyRepository = SecureKeyRepository(application)`،
  `httpClientEngine: HttpClientEngine = OkHttp.create()`.
- StateFlow های تازه، کاملاً مستقل از `_selectedProfileId` موجود
  (که `ModelProfile`/Veo/Kling است، نه AI Connector):
  `_selectedAiConnectorProfileId` (پیش‌فرض: اولین
  `BUILTIN_AI_CONNECTOR_PROFILES`)، `_apiKeySavedForQualityAnalysis`،
  `_qualityAnalysisInProgress`، `_qualityAnalysisResult`،
  `_qualityAnalysisError`.
- `refreshApiKeySavedForQualityAnalysis(profileId)`: عیناً همان الگوی
  `runCatching { secureKeyRepository.hasApiKey(profileId) }.getOrDefault(false)`
  از `AiStoryBreakdownViewModel` — یک محافظ ضروری مستندشده، نه احتیاط
  اضافی: `SecureKeyRepository` تزریق‌نشده روی Robolectric واقعاً
  `KeyStoreException` پرتاب می‌کند، و چون تست‌های ViewModel با
  `ioScopeOverride=Dispatchers.Unconfined` اجرا می‌شوند (یک `Job` ساده،
  نه `SupervisorJob`)، شکست یک Coroutine فرزند کل Job والد را لغو
  می‌کند. `fail-closed` به `false` هم با الزام محصول («بدون کلید،
  غیرفعال») هم‌راستاست.
- `selectAiConnectorProfileForQuality(profileId)`: هم‌الگوی دقیق
  `selectProfile`.
- `analyzePromptQualityWithAi()`: طبق قاعده‌ی امنیتی بالا با بررسی
  کلید شروع می‌شود؛ در موفقیت، پرامپت ارزیابی را با
  `buildQualityAnalysisPrompt(formattedPrompt, qualityScore)` می‌سازد
  (شامل متن نهایی پرامپت و شکست پنج‌محوری `QualityScore`) و
  `sendToAiConnector` را عیناً همان تابع آماده‌ی G2 صدا می‌زند —
  بدون بازنویسی.
- `clearQualityAnalysisError()`/`clearQualityAnalysisResult()`.
- `factory(...)` با الگوی «anyInjected» (رفع G16، ADR-063) گسترش یافت:
  هر پارامتر تزریقی اختیاری (`promptGenerationRepository`/
  `secureKeyRepository`/`httpClientEngine`) مستقلاً با `?:` به مقدار
  واقعی پیش‌فرض خودش برمی‌گردد.

**۲. سادگی آگاهانه نسبت به G2 (نه یک اشتباه)**: پاسخ این ویژگی متن
آزاد توصیفی است (تحلیل کیفی AI از پرامپت)، نه JSON ساخت‌یافته — پس
`ChunkCombiner`/`JsonDoctor`/`processAiResponse` (مختص Parse چندتکه‌ی
JSON مربوط به G2) لازم نبودند؛ `Result<String>` خروجی
`sendToAiConnector` مستقیماً همان متن قابل‌نمایش است.

**۳. `OutputDeliveryScreen.kt`:** کامپوزبل تازه‌ی `AiQualityAnalysisCard`
(نه تودرتوی `PromptQualityCard`، بلافاصله بعدش) شامل: چیپ‌های انتخاب
پروفایل (`OpaqueChip`، فقط اگر `BUILTIN_AI_CONNECTOR_PROFILES.size > 1`
— هم‌الگوی دقیق `GeneratedPromptCard` خط ۵۳۳-۵۴۲)، دکمه‌ی تحلیل با
`enabled = apiKeySaved && !inProgress` و `CircularProgressIndicator`
هنگام بارگذاری، متن راهنمای کوچک وقتی کلید ذخیره نشده، نمایش نتیجه
(`Text` با testTag)، و نمایش خطا (`Card` قابل‌بستن، هم‌الگوی خطای G2
خط ۴۶۴-۴۷۳).

**تصمیم طراحی — چرا کامپوزبل جدا (نه تودرتوی `PromptQualityCard`)**:
این بخش یک اقدام واقعاً مستقل است (یک فراخوانی HTTP واقعی)، نه بخشی
از محاسبه‌ی محلی امتیاز — جدا نگه‌داشتنش خوانایی و تست‌پذیری مستقل
هرکدام را حفظ می‌کند.

**تصمیم طراحی — متن ثابت دکمه (نه قالب هر‌سرویس)**: برخلاف
`"ارسال خودکار به {service}"` در G2، متن دکمه اینجا ثابت
(`«تحلیل عمیق‌تر با AI»`/`"Deeper Analysis with AI"`) است، چون خودِ
عنوان این قدم در دستور کار دقیقاً همین نام را برای دکمه به کار برده؛
نام سرویس انتخاب‌شده در یک برچسب کوچک جداگانه زیر دکمه نمایش داده
می‌شود.

**۴. testTag های تازه**: `OUTPUT_DELIVERY_ANALYZE_WITH_AI_BUTTON_TAG`،
`OUTPUT_DELIVERY_AI_ANALYSIS_RESULT_TAG`،
`OUTPUT_DELIVERY_AI_ANALYSIS_ERROR_TAG`،
`outputDeliveryAiConnectorProfileChipTag(profileId)`.

**۵. `UiStrings.kt`**: دو کلید تازه در **هر دو** نقشه (فارسی/انگلیسی)
هم‌زمان اضافه شدند — دقیقاً برای پرهیز از باگ واقعی مشابهِ ADR-115
(که یک‌بار فقط نقشه‌ی فارسی به‌روزرسانی شد و `UiStringsTest` آن را
گرفت): `outputDelivery.analyzeWithAiButton`،
`outputDelivery.analyzeWithAiNoKeyHint`.

## Scope

فقط `OutputDeliveryViewModel.kt`، `OutputDeliveryScreen.kt`،
`UiStrings.kt` (فقط کلید تازه) و `OutputDeliveryViewModelTest.kt`
تغییر کردند. `AiConnector.kt`، `SecureKeyRepository.kt`،
`AiStoryBreakdownViewModel.kt`/`Screen.kt` **دست‌نخورده باقی ماندند**
— این قدم فقط الگوی موجودشان را برای صفحه‌ای دیگر تکرار می‌کند.

## تست

**۳ تست امنیتی/عملکردی تازه در `OutputDeliveryViewModelTest.kt`**
(هم‌الگوی دقیق تست‌های معادل در `AiStoryBreakdownViewModelTest.kt`):

1. **`... never attempts a real HTTP call and sets a meaningful error`**
   (مهم‌ترین تست امنیتی این قدم): بدون کلید ذخیره‌شده،
   `analyzePromptQualityWithAi().join()` صدا زده می‌شود؛ یک
   `MockEngine` با یک پرچم بولی (`engineCalled`) که فقط داخل Handler
   واقعی HTTP به `true` تغییر می‌کند تزریق شده؛ assert می‌شود
   `engineCalled == false` (یعنی Ktor هرگز حتی یک درخواست HTTP واقعی
   نساخت) و `qualityAnalysisError` غیر-null است.
2. **`... with a saved key and a successful response populates qualityAnalysisResult`**:
   با `SecureKeyRepository` واقعی (پشتیبان `SharedPreferences` معمولی
   تزریقی، نه AndroidKeyStore واقعی — `buildTestSecureKeyRepository`)
   و کلید ذخیره‌شده‌ی Claude، و `MockEngine` موفق با شکل واقعی پاسخ
   Claude (`{"content":[{"type":"text","text":"..."}]}`)، بعد از
   `.join()`، `qualityAnalysisResult` دقیقاً همان متن را دارد.
3. **`... with a saved key but a failing HTTP response sets a meaningful qualityAnalysisError`**:
   با کلید ذخیره‌شده اما `MockEngine` با `HttpStatusCode.Unauthorized`
   و بدنه‌ی خطای واقعی Claude، `qualityAnalysisError` شامل متن واقعی
   خطای سرویس (`invalid x-api-key`) است، نه فقط یک پیام عمومی.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:testDebugUnitTest` (`OutputDeliveryViewModelTest`، مجزا) | موفق — ۸ تست (۵ موجود + ۳ تازه)، ۰ شکست |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۹۱۱ تست، ۳ شکست (`AppNavigationTest`، `OutputDeliveryFlowTest`، `ShotsFlowTest`) — هر سه در اجرای مجزا موفق |
| مقایسه با `git stash` روی HEAD تمیز (بدون تغییرات این قدم) | همان الگوی Flake مستقل از این قدم دیده شد (`OutputDeliveryFlowTest` باز هم فقط در کل Suite شکست خورد) — تأیید شد این شکست‌ها ناشی از تغییرات این قدم نیستند |
| `gradle :app:assembleDebug` | موفق (فقط هشدارهای از پیش موجود `ClipboardManager`، نامرتبط) |

**درباره‌ی `ShotsFlowTest`**: این کلاس پیش‌تر در فهرست کلاس Flake
مستندشده‌ی این جلسه (`OutputDeliveryFlowTest`، `AssetFormFlowTest`،
`AppNavigationTest`، ADR-044/070/071) نامبرده نشده بود؛ اما در اجرای
مجزا سبز ماند و در HEAD تمیز نیز (با `git stash`) رفتار مشابه Flake
محیطی/وابسته‌به‌ترتیب دیده شد — این کلاس‌ها به‌طور شناخته‌شده Robolectric/
Compose state را در کل Suite به‌اشتراک می‌گذارند؛ ظاهرشدن یک عضو تازه‌ی
گاه‌به‌گاه در این فهرست با ریشه‌ی مستندشده‌ی ADR-070/071 سازگار است، نه
نشانه‌ای از رگرسیون این قدم — هیچ‌کدام از فایل‌های این قدم به
`Shots`/`AppNavigation` مرتبط نیستند.

## End-to-End Verification امنیتی

سوال کلیدی: آیا قفل «بدون کلید ذخیره‌شده = بدون AI Connector» واقعاً
کار می‌کند، یا فقط در UI به نظر می‌رسد کار می‌کند (یعنی صرفاً دکمه
خاکستری است اما اگر کاربر یا یک باگ آینده مسیر را دور بزند، درخواست
واقعی همچنان می‌رود)؟

تست امنیتی #۱ این را در **پایین‌ترین لایه‌ی ممکن** اثبات می‌کند، نه در
لایه‌ی UI:

- تزریق مستقیم یک `MockEngine` به‌عنوان `httpClientEngine` به خودِ
  ViewModel (نه شبیه‌سازی کلیک روی دکمه‌ی Compose) — یعنی این تست حتی
  فرض نمی‌کند دکمه واقعاً غیرفعال است؛ مستقیماً تابع منطق تجاری
  `analyzePromptQualityWithAi()` را صدا می‌زند، دقیقاً همان مسیری که
  اگر یک باگ UI دکمه را فعال نگه دارد هم طی می‌شود.
- `engineCalled` فقط داخل Handler واقعی `MockEngine` (یعنی همان نقطه‌ای
  که Ktor واقعاً یک درخواست HTTP را پردازش می‌کند) به `true` تغییر
  می‌کند — نه یک Mock سطح‌بالاتر که ممکن است چیزی را مخفی کند.
- Assert می‌کند این پرچم پس از `.join()` کامل (یعنی کل زنجیره‌ی async
  به پایان رسیده) هنوز `false` است.

این یعنی اثبات‌شده که مسیر کد **قبل از رسیدن به لایه‌ی HTTP واقعی**
(همان بررسی `apiKey == null || validateApiKeyProvided(apiKey) != null`
در `analyzePromptQualityWithAi`) خارج می‌شود — یک قفل واقعی در منطق
تجاری، مستقل از این‌که UI درست رندر شده یا نه. این دقیقاً همان استاندارد
اثباتی است که در G2 (ADR-101) برای مسیر مشابه اعمال شد.

## خارج از Scope این قدم (عمداً)

- منطق `evaluatePromptQuality`/`scoreTextRichness` (ADR-118) و اتصال
  مسیر ۱/بدون AI (ADR-119) دست‌نخورده.
- معماری خودِ `AiConnector.kt`/`SecureKeyRepository.kt`/G2 دست‌نخورده
  — این قدم فقط الگوی موجودشان را تکرار کرد.

## نتیجه‌گیری — بسته‌شدن کامل برنامه‌ی سه‌قدمی

با این ADR، برنامه‌ی «هوشمندسازی و اتصال evaluatePromptQuality»
(ADR-118 → ADR-119 → ADR-120) به‌طور کامل بسته می‌شود: خودِ الگوریتم
هوشمندتر شد (قدم ۱)، به مسیر بدون‌AI وصل شد (قدم ۲)، و اکنون یک مسیر
اختیاری AI Connector — با همان استاندارد امنیتی سخت‌گیرانه‌ی G2 —
برایش اضافه شد (قدم ۳).

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
