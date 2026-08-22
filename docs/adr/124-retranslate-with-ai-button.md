# ADR-124: دکمه‌ی «ترجمه‌ی مجدد با AI» روی فیلد Preview فارسی (هر ۴ فرم)

## وضعیت

پذیرفته‌شده

## زمینه

**این یک فیچر مستقل است — بخشی از برنامه‌ی سه‌قدمی «سیستم Preview دوزبانه‌ی
پرامپت» (ADR-121 تا ADR-123) نیست.** آن برنامه فیلد `descriptionFaPreview`/
`shotDescriptionFaPreview` را اضافه کرد و راه پر کردنش را «کاربر خودش تایپ
می‌کند یا از پاسخ AI دوزبانه‌ی Story Breakdown می‌آید» گذاشت. این ADR یک راه
سوم اضافه می‌کند: یک دکمه‌ی مستقل روی خودِ هر فرم که همان متن انگلیسی همان
فرم را با یک فراخوان AI جداگانه به فارسی ترجمه می‌کند — مفید برای Asset ها/
Shot هایی که از مسیر Story Breakdown نیامده‌اند (مثلاً دستی ساخته شده‌اند).

### راستی‌آزمایی مستقل (قبل از کدنویسی)

طبق دستور صریح این قدم، همه‌ی فایل‌های زیر قبل از کدنویسی کامل خوانده شدند —
هیچ مغایرتی با پیش‌بریفینگ معمار پیدا نشد:

- `domain/storybreakdown/AiConnector.kt`: `GEMINI_API_PROFILE` و
  `BUILTIN_AI_CONNECTOR_PROFILES` (۵ پروفایل: Claude/OpenAI/Gemini/DeepSeek/
  Qwen) دقیقاً طبق بریفینگ از پیش کامل و تست‌شده بودند (G2، ADR-098 تا ۱۰۵).
  `sendToAiConnector` دقیقاً همان امضا و رفتار توصیف‌شده را داشت.
- `data/repository/SecureKeyRepository.kt`: چندسرویسی بودن (هر کلید با
  `profileId` مستقل) دقیقاً طبق بریفینگ تأیید شد.
- `ui/storybreakdown/AiStoryBreakdownScreen.kt` (`GeneratedPromptCard`) و
  `ui/assets/AssetsScreen.kt` (`OpaqueChip`، `internal`): الگوی چیپ‌های
  انتخاب پروفایل دقیقاً طبق بریفینگ بازاستفاده شد.
- `ui/outputdelivery/OutputDeliveryViewModel.kt`
  (`analyzePromptQualityWithAi`/`refreshApiKeySavedForQualityAnalysis`): الگوی
  امنیتی («بررسی پیش‌فعال `hasApiKey` قبل از هر تلاش HTTP») دقیقاً طبق
  بریفینگ همان الگوی G2/ADR-101 بود.
- هر ۴ فرم (`CharacterAssetFormViewModel`/`LocationAssetFormViewModel`/
  `ObjectAssetFormViewModel`/`ShotComposerViewModel` + ۴ Screen متناظر، پس از
  ADR-123): تأیید شد سه ViewModel فرم Asset با `canSave`-gated دکمه‌ی صریح
  «ذخیره» کار می‌کنند و هیچ Setterشان خودکار `save()` صدا نمی‌زند، درحالی‌که
  `ShotComposerViewModel` با هر Setter بلافاصله `save()` صدا می‌زند — دقیقاً
  همان تفاوتی که بریفینگ صریحاً هشدار داده بود که نباید یکسان فرض شود.

**یک یافته‌ی جانبی مفید (نه انحراف)**: `Icons.Filled.Translate` در پروژه تا
این قدم فقط برای دکمه‌ی «تعویض زبان کل UI» استفاده شده بود (نه ترجمه‌ی محتوای
یک فیلد) — اما چون همان آیکون معنایی دقیقاً «Translate» دارد و آیکون تازه‌ای
لازم نبود، همان بازاستفاده شد؛ این تفاوت کاربرد ذکر می‌شود صرفاً برای شفافیت،
نه به‌عنوان مشکل.

## تصمیم

### تحقیق معمار: چرا Gemini پیش‌فرض است (نه انحصاری)

طبق تصمیم محصولی توافق‌شده با معمار پروژه: دکمه محدود به Gemini نیست — کاربر
هر پروفایلی که کلیدش را در تنظیمات ذخیره کرده می‌تواند انتخاب کند (چیپ‌های
انتخاب، دقیقاً الگوی `GeneratedPromptCard`). Gemini صرفاً **پیش‌فرض انتخاب‌شده**
است، با یک برچسب کوچک «رایگان»/«Free» فقط کنار چیپ Gemini.

توجیه این پیش‌فرض (طبق تحقیق معمار، مستندشده در پرامپت این قدم): در منابع
قیمت‌گذاری AI API معتبر ۲۰۲۶، Gemini (روی `gemini-2.5-flash`، همان مدلی که
`GEMINI_API_PROFILE` این پروژه از قبل استفاده می‌کند — ADR-103) تنها پروفایل
این پنج‌تایی با یک لایه‌ی رایگان **دائمی/perpetual** است — نه فقط یک اعتبار
اولیه‌ی یک‌بارمصرف (Free Trial Credit) مثل رقبا. این پروژه خودش راستی‌آزمایی
مستقل تازه‌ای از قیمت‌گذاری زنده‌ی هر پنج سرویس در این قدم انجام نداد (خارج از
Scope یک UI Decision — قیمت‌گذاری واقعی زمان اجرا تغییر می‌کند و مسئولیت
کاربر/تنظیمات AI Connector Profile است، نه چیزی که این کد باید Hardcode کند)؛
این ADR فقط توجیه محصولی پیش‌فرض UI را مستند می‌کند، نه یک ادعای فنی
Verify‌شده درباره‌ی قیمت زنده‌ی هر سرویس.

### معماری سه‌لایه

**۱. Domain Layer مشترک (یک‌بار نوشته شده)**: `translateToFarsi` در
`domain/storybreakdown/AiConnector.kt` (نه فایل جدا — چون فقط چند خط است و
مستقیم `sendToAiConnector` موجود همان فایل را صدا می‌زند، جدا کردنش پیچیدگی
بی‌دلیل اضافه می‌کرد):

```kotlin
suspend fun translateToFarsi(
    englishText: String,
    profile: AiConnectorProfile,
    apiKey: String,
    engine: HttpClientEngine = OkHttp.create()
): Result<String> {
    val prompt = "این متن را دقیق و روان به فارسی ترجمه کن، فقط خودِ ترجمه را برگردان، بدون توضیح اضافه: $englishText"
    return sendToAiConnector(profile, apiKey, prompt, engine)
}
```

هیچ Rule/HTTP تازه‌ای اینجا نیست — `sendToAiConnector` موجود بدون تغییر
بازاستفاده شد؛ همه‌ی ۵ پروفایل را همان‌قدر پشتیبانی می‌کند.

**۲. ViewModel Layer (۴ ViewModel، هرکدام مستقل وایر شد)**: هر چهار
(`CharacterAssetFormViewModel`/`LocationAssetFormViewModel`/
`ObjectAssetFormViewModel`/`ShotComposerViewModel`) گرفتند:

- وابستگی سازنده‌ی `secureKeyRepository`/`httpClientEngine` (تزریق‌پذیر، هم‌الگو
  دقیق با `AiStoryBreakdownViewModel`/`OutputDeliveryViewModel`).
- `selectedTranslationProfileId` (پیش‌فرض `GEMINI_API_PROFILE.profileId`)،
  `apiKeySavedForTranslationProfile` (با محافظ `runCatching` — یافته‌ی
  مستندشده‌ی G2/ADR-101، هنوز صادق روی Robolectric)، `translationInProgress`،
  `translationError`.
- `selectTranslationProfile(profileId)`.
- `retranslate()`: امنیت پیش‌فعال (بررسی `apiKeySavedForTranslationProfile`
  قبل از هر تلاش)؛ متن انگلیسی منبع فرق دارد (`basePrompt` برای Character،
  `description` برای Location/Object، `shotDescription` برای Shot) — هر چهار
  به `translateToFarsi` می‌دهند.

**تفاوت رفتاری واقعی، تأییدشده با بررسی مستقل (نه فرض یکسان‌بودن)**: در سه
ViewModel فرم Asset، نتیجه‌ی موفق فقط StateFlow را به‌روز می‌کند — هم‌الگو
دقیق با بقیه‌ی Setterهای همان ViewModel که هیچ‌کدام خودکار `save()` صدا
نمی‌زنند (فقط دکمه‌ی صریح «ذخیره»/`canSave` این کار را می‌کند).
`ShotComposerViewModel` برخلاف آن سه، بلافاصله `save()` را هم صدا می‌زند —
هم‌الگو دقیق با `setShotDescriptionFaPreview` موجود همان ViewModel.

**۳. UI Layer مشترک — اولین کامپوننت UI مشترک این پروژه**: `ui/common/
RetranslateButton.kt` (تأییدشده با grep: تا این قدم هیچ پوشه‌ی
`ui.common`/کامپوننت مشترک بین چند پکیج UI وجود نداشت). یک Composable عمومی
(نه `private`/`internal`) که هر دو پکیج `ui.assets` (سه فرم) و `ui.shots`
(فرم Shot) صدا می‌زنند. `OpaqueChip` (تعریف‌شده `internal` در
`ui/assets/AssetsScreen.kt`) مستقیماً از `ui.common` Import شد — چون `internal`
در Kotlin یعنی «قابل‌مشاهده در کل ماژول»، نه فقط همان پکیج؛ نیازی به کپی یا
تعریف تازه نبود.

دکمه شکل «آیکون (`Icons.Filled.Translate`) + متن کوتاه» دارد، زیر فیلد فارسی
موجود هر فرم قرار می‌گیرد. چیپ‌های انتخاب پروفایل فقط وقتی
`BUILTIN_AI_CONNECTOR_PROFILES.size > 1` نمایش داده می‌شوند (هم‌الگو دقیق با
`GeneratedPromptCard`)؛ چیپ Gemini برچسب «رایگان» کنارش دارد.

## پیامدها

- `PromptAssembly.kt`/`Renderer.kt` دست‌نخورده ماندند (تأییدشده با
  `git status`) — این فیچر هیچ‌جا در مسیر پرامپت نهایی نیست، فقط فیلد
  Preview فارسی (که خودش هرگز وارد پرامپت نهایی نمی‌شود، طبق قرارداد
  ADR-121) را پر می‌کند.
- بدون هیچ Migration دیتابیس (نه فیلد تازه‌ای در Entity/DTO اضافه شد — این
  فیچر فقط یک مسیر تازه برای پرکردن فیلد از‌قبل‌موجود `descriptionFaPreview`/
  `shotDescriptionFaPreview` است).
- هر ۵ پروفایل AI Connector موجود از این فیچر پشتیبانی می‌کنند، بدون هیچ کد
  تازه‌ای per-profile — چون `translateToFarsi` فقط یک استفاده‌ی تازه از
  `sendToAiConnector` عمومی موجود است.

## جایگزین‌های رد‌شده

- **یک ViewModel/Repository جدا برای «ترجمه»**: رد شد — منطق واقعی فقط چند
  خط است (ساخت پرامپت + صدازدن تابع موجود)؛ یک لایه‌ی جدید معماری بدون
  توجیه واقعی اضافه می‌کرد.
- **قفل‌کردن این دکمه فقط به Gemini**: رد شد — تصمیم محصولی صریح می‌گفت هر
  پروفایل ذخیره‌شده باید قابل‌انتخاب باشد؛ Gemini فقط پیش‌فرض UI است.
- **کپی مستقل UI برای هر ۴ فرم**: رد شد — چهار فرم منطق UI تقریباً یکسانی
  نیاز داشتند (چیپ + دکمه)؛ یک Composable مشترک در `ui.common` هم تکرار کد
  را حذف کرد و هم اولین سنگ‌بنای این پوشه برای فیچرهای مشترک آینده شد.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`: موفق.
- تست‌های تازه/گسترش‌یافته: `AiConnectorTest.kt` (۲ تست تازه برای
  `translateToFarsi`)، `CharacterAssetFormViewModelTest.kt` (۳ تست امنیتی
  تازه، نماینده‌ی الگوی سه ViewModel فرم Asset)، `ShotComposerViewModelTest.kt`
  (۳ تست امنیتی تازه، نماینده‌ی الگوی متفاوت Auto-Save). تصمیم آگاهانه‌ی
  محدوده‌ی تست: `LocationAssetFormViewModel`/`ObjectAssetFormViewModel`
  تکرار نشدند چون `retranslate()`شان کاملاً هم‌ساختار با
  `CharacterAssetFormViewModel` است (فقط فیلد متن منبع فرق می‌کند) — تکرار
  کامل سه تست امنیتی برای هر چهار چیز تازه‌ای اثبات نمی‌کرد.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۹۴۶ تست، ۲ شکست — هر دو از
  کلاس‌های از‌قبل‌مستندشده‌ی ناپایدار فقط در اجرای کامل (`AppNavigationTest`،
  `OutputDeliveryFlowTest` — مستندشده از ADR-044، ریشه‌یابی‌شده ADR-070/071)؛
  هر دو در اجرای ایزوله دوباره Pass شدند، بدون هیچ شکست واقعی مرتبط با این
  قدم.
- `./gradlew :app:assembleDebug`: موفق.
