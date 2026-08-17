# ADR-101: G2 قدم ۳ از ۳ (آخرین قدم) — فیلد کلید API در Settings + انتخابگر مسیر ۱/۲ در AI Story Breakdown

## زمینه

ADR-035 (Option A) عمداً `sendToAiConnector` را `TODO()` گذاشته بود. ADR-098
(G2 قدم ۱) لایه‌ی ذخیره‌سازی امن کلید (`SecureKeyRepository`) را مستقل ساخت.
ADR-100 (G2 قدم ۲) اتصال HTTP واقعی (`sendToAiConnector`) و اولین پروفایل
واقعی (`CLAUDE_API_PROFILE`) را اضافه کرد — اما هیچ‌کدام از این دو قدم به UI
وصل نبودند. این قدم **آخرین قدم G2** است: این دو زیرساخت را برای اولین بار
به UI وصل می‌کند و کل مسیر end-to-end (وارد کردن کلید در Settings → نوشتن
داستان → ارسال خودکار → دریافت Scene/Shot/Asset) را در یک تلاش ممکن می‌سازد.

## بررسی مستقل (پیش از نوشتن کد) — و اختلاف با پیش‌بریفینگ معمار

پیش از نوشتن کد، سه فرض صریح دستور اجرایی مستقیماً در کد بررسی شد:

- **`SettingsScreen.kt` بدون ViewModel اختصاصی**: تأیید شد — کامنت سربرگ خودِ
  فایل صریحاً می‌گوید «دقیقاً همان ۷ کارت... چیزی اضافه/کم نشده» و همه‌چیز از
  `WorkflowViewModel` می‌آید. مطابق پیش‌بریفینگ.
- **`AiStoryBreakdownViewModel.kt`: `enum BreakdownPhase { WRITE_STORY,
  PASTE_RESPONSE, FINAL_REVIEW }`**: تأیید شد (خواندن کامل فایل، ۳۱۹ خط
  قبل از این قدم). مطابق پیش‌بریفینگ.
- **امضای دقیق زنجیره‌ی Parse/Repair مسیر ۱**: با grep روی
  `StoryToDomainMapper.kt` تأیید شد امضای واقعی
  `fun processAiResponse(chunks: List<String>, targetShotCount: Int):
  ProcessAiResponseResult` است و `ProcessAiResponseResult` یک sealed class با
  سه حالت (`Success`/`NeedsManualRepair`/`MissingRequiredKeys`) — دقیقاً مطابق
  پیش‌بریفینگ. `processResponse()` موجود در `AiStoryBreakdownViewModel.kt` هم
  همین تابع را دقیقاً با همین امضا صدا می‌زد.

**نتیجه: هیچ اختلافی بین پیش‌بریفینگ معمار و کد واقعی پیدا نشد** — هر سه فرض
تأیید شدند، نه رد.

## یافته‌ی مهم پنهان (کشف‌شده حین راستی‌آزمایی، نه از قبل مفروض): رگرسیون واقعی روی تست‌های موجود

افزودن یک فراخوان بدون‌شرط `secureKeyRepository.hasApiKey(...)` در `init{}` دو
ViewModel (`AiStoryBreakdownViewModel`، `ApiKeysViewModel`) — بدون بررسی
اضافی — دو مشکل واقعی ایجاد کرد که با اجرای واقعی Test Suite کشف شدند، نه با
حدس:

1. **`AiStoryBreakdownViewModelFactoryTest`**: چون `SecureKeyRepository`
   پیش‌فرض (تزریق‌نشده) روی Robolectric واقعاً `KeyStoreException:
   AndroidKeyStore not found` پرتاب می‌کند (محدودیت مستندشده‌ی ADR-098)، و چون
   این تست با `ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)` اجرا
   می‌شود (یک `Job` معمولی، نه `SupervisorJob`)، شکست یک Coroutine فرزند کل Job
   والد و Coroutine های خواهر (از‌جمله نوشتن‌های واقعی `confirmAndSave`) را لغو
   می‌کرد. نتیجه: `assetRepository` تزریقی هیچ‌چیز واقعاً ذخیره نمی‌کرد
   (`expected:<2> but was:<0>`).
2. **`SettingsFlowTest.kt`**: همان الگو، شدیدتر — چون این فایل `SettingsScreen`
   را از طریق `MainScaffold` (بدون تزریق `secureKeyRepository`) رندر می‌کند،
   `ApiKeysViewModel` واقعی پیش‌فرض ساخته می‌شد و همان Exception را در حین
   Composition پرتاب می‌کرد: **۹ از ۱۰ تست آن فایل شکست می‌خوردند.**

**رفع:** هر دو فراخوان با `runCatching { ... }.getOrDefault(false)` محافظت
شدند — fail-closed به `false` (نه crash) دقیقاً با شرط سخت‌گیرانه‌ی محصولی
هم‌راستاست: اگر وضعیت کلید قابل‌تشخیص نباشد، گزینه‌ی ارسال خودکار باید
غیرفعال بماند، نه اینکه کل صفحه را خراب کند. بعد از این رفع، هر دو فایل تست
دوباره ۱۰۰٪ سبز شدند (تأییدشده با اجرای مجزای هر دو).

این یک یافته‌ی واقعی حین راستی‌آزمایی بود، نه بخشی از دستور اجرایی — طبق قانون
این قدم («خارج از Scope مگر باگ واقعی؛ در آن صورت توقف و گزارش»)، چون علت
واقعی در کد **این قدم** (`AiStoryBreakdownViewModel.kt`/`ApiKeysViewModel.kt`،
نه `SecureKeyRepository.kt`) بود، رفع مستقیم انجام شد، نه توقف.

## تصمیم ۱ — Part A: یک `ApiKeysViewModel` کوچک و مجزا، نه افزودن به `WorkflowViewModel`

پیش‌بریفینگ معمار پیشنهاد همین را داده بود؛ بررسی مستقل کامنت سربرگ
`WorkflowViewModel.kt` («سایر State های همان بخش سند... عمداً اینجا نیستند»)
این تصمیم را تأیید کرد: مدیریت کلید API پشت `EncryptedSharedPreferences` است
(نه DataStore Preferences مثل بقیه‌ی `WorkflowViewModel`) — افزودنش به آن
ViewModel انضباط دامنه‌ی مستندشده‌ی خودش را نقض می‌کرد. الگوی فایل تازه
(Repository تزریق‌پذیر با پیش‌فرض واقعی، `ioScopeOverride` برای تست) عیناً
هم‌شکل `AiStoryBreakdownViewModel`/`CharacterAssetFormViewModel` است.

`ApiKeysViewModel.savedStatus: StateFlow<Map<String, Boolean>>` روی
`BUILTIN_AI_CONNECTOR_PROFILES` پیمایش می‌کند (نه یک فیلد هاردکد Claude) —
`SettingsScreen.kt`'s `ApiKeysCard` هم به همین شکل خودکار برای چندسرویسی کار
می‌کند، بدون نیاز به تغییر وقتی پروفایل دوم اضافه شود.

## تصمیم ۲ — Part B: `AiStoryBreakdownViewModel` هاردکد به `CLAUDE_API_PROFILE`، نه یک انتخابگر پروفایل عمومی

برخلاف Part A (که خودکار برای چندسرویسی است)، مسیر ۲ در AI Story Breakdown
مستقیماً `CLAUDE_API_PROFILE` را صدا می‌زند، نه یک انتخابگر عمومی. **دلیل:**
`BUILTIN_AI_CONNECTOR_PROFILES` فعلاً فقط شامل همین یک پروفایل است (طبق
ADR-100)؛ ساخت یک UI انتخابگر کامل برای فهرستی با یک عضو، پیچیدگی بی‌دلیل
اضافه می‌کرد بدون فایده‌ی واقعی فعلی. وقتی پروفایل دوم واقعی اضافه شود
(OpenAI/Gemini/...)، این تصمیم باید بازبینی شود — این یک محدودیت شناخته‌شده و
عمدی است، نه پنهان.

## تصمیم ۳ — کد Parse/Repair به‌اشتراک‌گذاشته‌شده، نه تکرارشده

`processResponse()` (مسیر ۱ موجود) به یک تابع خصوصی مشترک
`applyProcessAiResponseResult(result: ProcessAiResponseResult)` بازتوان‌سازی
شد. `sendPromptAutomatically()` (مسیر ۲ تازه) دقیقاً همین تابع را صدا می‌زند.
هر دو مسیر یک نتیجه‌ی موفق را عیناً یکسان مدیریت می‌کنند (`_breakdownResult`،
`_phase = FINAL_REVIEW`) — بدون کد تکراری Parse/Repair.

اگر پردازش موفقیت‌آمیز HTTP به `NeedsManualRepair`/`MissingRequiredKeys` برسد
(نه شکست HTTP، بلکه پاسخ واقعی ناقص/بدشکل)، فاز عمداً به `PASTE_RESPONSE`
منتقل می‌شود و پاسخ خام در `currentChunkInput` پر می‌شود — زیرساخت UI موجود
فاز ۲ (Modal تعمیر/کارت خطا) بدون کد UI تکراری بازاستفاده می‌شود.

## تصمیم ۴ — شکست HTTP کاربر را گیر نمی‌اندازد

طبق شرط صریح دستور اجرایی، شکست `sendToAiConnector` (`result.isFailure`) فاز
را تغییر **نمی‌دهد** — کاربر دقیقاً در `WRITE_STORY` با دکمه‌ی «کپی» موجود
می‌ماند، فقط یک کارت خطای معنادار (از Rule ۵ — `validateAiConnectorErrorMessage`،
بدون تغییر در `AiConnector.kt`) با دکمه‌ی «متوجه شدم» نمایش داده می‌شود.

## تصمیم ۵ — شرط سخت‌گیرانه‌ی UI: دکمه‌ی ارسال خودکار در سطح UI غیرفعال است، نه فقط خطای بعد از کلیک

`GeneratedPromptCard`'s «ارسال خودکار به Claude» `Button`: `enabled =
claudeApiKeySaved && !autoSendInProgress`. تا `claudeApiKeySaved=false`، خودِ
دکمه Disabled است (نه صرفاً یک خطای بعد از کلیک) — به‌همراه یک متن راهنمای
کوتاه («ابتدا کلید API را در تنظیمات وارد کنید»). این هم در سطح UI (تست
Compose) و هم در سطح ViewModel (Rule ۴ — `validateApiKeyProvided`، دفاع دوم
برای فراخوان مستقیم بدون UI) اعمال شده.

## تصمیم ۶ — هزینه‌ی واقعی: یک اعلان یک‌باره، نه هشدار مزاحم هر استفاده

طبق تصمیم محصولی، متن «هزینه‌ی واقعی استفاده از این سرویس‌ها... بر عهده‌ی
شماست» یک‌بار زیر عنوان کارت کلید API در Settings نمایش داده می‌شود — نه
هشدار تکراری هر بار که کاربر از مسیر ۲ استفاده می‌کند.

## تصمیم ۷ — `saveApiKey`/`deleteApiKey`/`sendPromptAutomatically` همگی `Job` برمی‌گردانند

هم‌الگو دقیق با `AiStoryBreakdownViewModel.confirmAndSave` و
`OutputDeliveryViewModel.regenerate/exportOutput` (یافته‌ی مستندشده‌ی همان
فایل): چون `SecureKeyRepository`/`sendToAiConnector` هر دو داخلاً روی
Dispatcher های واقعی (`Dispatchers.IO`/Ktor Engine) اجرا می‌شوند، صرفاً
`ioScopeOverride=Unconfined` در تست کافی نیست — تست باید `.join()` کند تا
مطمئن شود کل زنجیره‌ی async قبل از `assert` کامل شده. بدون این، تست‌های تازه
واقعاً Race Condition داشتند (تجربه‌شده حین نوشتن تست، نه فرضی).

## تست

- **`ApiKeysViewModelTest.kt` (تازه، ۵ تست)**: `savedStatus` اولیه false،
  `saveApiKey` واقعاً از طریق `SecureKeyRepository` قابل بازیابی و
  `savedStatus` را true می‌کند، کلید خالی/فقط‌فاصله نادیده گرفته می‌شود،
  `deleteApiKey` واقعاً حذف می‌کند، دو profileId مستقل از هم.
- **`AiStoryBreakdownViewModelTest.kt` (۵ تست تازه، مجموع فایل ۱۳→۱۸)**:
  `claudeApiKeySaved` false بدون کلید/true بعد از ذخیره‌ی از‌پیش (با
  `awaitCondition`، چون بارگذاری init `Job` برنمی‌گرداند)، Rule ۴ بدون کلید
  (هیچ HTTP واقعی نمی‌رود)، ارسال موفق (`MockEngine`) دقیقاً همان مسیر
  `applyProcessAiResponseResult` را طی می‌کند و به `FINAL_REVIEW` می‌رسد (مثل
  Paste دستی)، ارسال ناموفق پیام معنادار نشان می‌دهد و فاز را عوض نمی‌کند
  (کاربر گیر نمی‌کند، `generatedPrompt` دست‌نخورده می‌ماند).
- **`ApiKeysFlowTest.kt` (تازه، ۴ تست Compose، Render مستقیم — نه
  MainScaffold)**: ذخیره‌ی کلید در Settings واقعاً از طریق `SecureKeyRepository`
  قابل بازیابی است و وضعیت را عوض می‌کند؛ حذف کلید واقعاً حذف می‌کند؛ دکمه‌ی
  ارسال خودکار در `AiStoryBreakdownScreen` قبل از ذخیره Disabled و بعد از
  ذخیره Enabled است.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`ApiKeysViewModelTest`) | ۵ تست، موفق |
| `gradle :app:testDebugUnitTest` (`AiStoryBreakdownViewModelTest`) | ۱۸ تست (۱۳ قبلی + ۵ تازه)، موفق |
| `gradle :app:testDebugUnitTest` (`ApiKeysFlowTest`) | ۴ تست، موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۰۰ تست (۷۸۶ + ۱۴ خالص تازه، دقیقاً مطابق انتظار)، ۱ شکست نامرتبط (`OutputDeliveryFlowTest`) — flaky شناخته‌شده‌ی محیط (مستند در ADR-098/096/100)، در اجرای مجزا موفق |
| `gradle :app:assembleDebug` | موفق |

## این قدم کل G2 را می‌بندد

با این قدم، G2 («اتصال واقعی به AI Connector») به‌طور کامل بسته می‌شود — هر
سه قدم (ADR-098: ذخیره‌سازی امن کلید، ADR-100: HTTP واقعی، ADR-101: UI کامل)
انجام شده‌اند. کاربر اکنون می‌تواند end-to-end: کلید را در Settings وارد کند
→ داستان بنویسد → با یک کلیک به Claude ارسال کند → مستقیماً Scene/Shot/Asset
دریافت کند — یا در هر لحظه به مسیر ۱ (کپی/پیست دستی) برگردد، بدون گیر کردن.

## خارج از Scope این قدم (کار آینده)

- انتخابگر عمومی پروفایل در AI Story Breakdown، وقتی پروفایل دوم واقعی اضافه
  شود (تصمیم ۲ بالا).
- فهرست پروفایل‌های واقعی سرویس‌های دیگر (OpenAI، Gemini، DeepSeek، Qwen) —
  خارج از Scope ADR-100 و همچنان این قدم.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
