# ADR-136: اتصال Rule های یتیم پرامپت عکس مرجع (ADR-132)

## وضعیت

پذیرفته‌شده

## زمینه

فیچر مستقل «پرامپت ساخت عکس مرجع» در ۵ زیرقدم (ADR-131 تا ۱۳۵) کامل شد.
زیرقدم ۲ (ADR-132) علاوه بر موتور Template، چهار تابع اعتبارسنجی هم نوشته
بود — `validateCharacterImagePromptInputs`، `validateOutfitImagePromptInputs`،
`validateLocationImagePromptInputs`، `validateObjectImagePromptInputs`
(همه در `domain/asset/ImagePromptValidation.kt`) — اما زیرقدم‌های ۴ و ۵
(ADR-134/135) هیچ‌کدام را به UI/ViewModel وصل نکردند؛ این تصمیم آن‌جا به
شکل یک انحراف ثبت‌شده مستند شد، بدون بررسی مجدد دلیل دقیق.

### راستی‌آزمایی دلیل واقعی عدم اتصال (پیش از این قدم)

قبل از نوشتن این قدم، معمار مستقیماً همین سؤال را پرسید و پاسخ صادقانه‌ی
زیر داده شد (و اینجا بازتأیید می‌شود، نه صرفاً تکرار): مانع اصلی **فنی/
طراحی** بود، نه یک تصمیم دامنه‌ای که دستور کار صریحاً خواسته باشد. هر ۴
تابع امضای `(..., generatedPrompt: String): List<ValidationIssue>` دارند —
یعنی به خودِ رشته‌ی **تولیدشده** نیاز دارند، نه فیلدهای زنده‌ی فرم. این با
الگوی موجود `validationIssues` هر سه ViewModel (که از `combine` روی
فیلدهای فرم ساخته می‌شود و همیشه به‌روز است) جور نبود؛ نیاز به یک
StateFlow/محل نمایش کاملاً جدا داشت که در زیرقدم‌های ۴/۵ طراحی نشده بود.
چون این توابع فقط Warning تولید می‌کنند (نه Blocking)، عدم اتصال هیچ
محدودیت عملکردی واقعی ایجاد نکرد — همین باعث شد آن مانع فنی به‌جای رفع‌شدن
در لحظه، به یک انحراف مستندشده موکول شود.

این قدم دقیقاً همان محل جدا را می‌سازد — بدون تغییر هیچ Rule یا تابع
Template موجود.

## تصمیم

### الگوی مشترک هر سه ViewModel

یک StateFlow جدید نگه‌دارنده‌ی نتیجه‌ی آخرین اعتبارسنجی:
- `CharacterAssetFormViewModel`: `imagePromptValidationIssues:
  StateFlow<List<ValidationIssue>>` برای شخصیت پایه، و
  `outfitImagePromptValidationIssues: StateFlow<Map<String,
  List<ValidationIssue>>>` برای هر Outfit — هم‌الگو دقیق با
  `outfitImagePromptAiInProgress`/`outfitImagePromptAiError` موجود (ردیابی
  با `outfit.id`، نه Index، چون چند Outfit هم‌زمان مستقل‌اند).
- `LocationAssetFormViewModel`/`ObjectAssetFormViewModel`: یک
  `imagePromptValidationIssues: StateFlow<List<ValidationIssue>>` ساده —
  این دو نوع Outfit ندارند، پس نیازی به Map نیست.

بلافاصله بعد از هر بار موفقیت‌آمیز پر‌شدن `imagePromptQuick`/`imagePromptAi`
(هر دو مسیر، هر ۴ نوع Asset)، تابع اعتبارسنجی متناظر روی همان رشته‌ی
تازه‌تولیدشده صدا زده می‌شود و نتیجه در StateFlow جدید می‌نشیند. روی مسیر
AI ناموفق (خطای شبکه) یا وقتی `ProjectDna` هنوز بارگذاری نشده، هیچ‌چیز
تغییر نمی‌کند — دقیقاً هم‌رفتار با خودِ `imagePromptQuick`/`imagePromptAi`
که هم روی شکست دست‌نخورده می‌مانند.

### UI

کنار همان محل نمایش `imagePromptQuick`/`imagePromptAi` (بعد از ترجمه‌ی
فارسی، قبل از برچسب قدیمی‌شدن)، لیست جدید با همان `AssetFormValidationIssueRow`
موجود رندر می‌شود — بدون کامپوننت تازه، بدون تست‌تگ تازه (یک لیست ایستا،
هم‌رده با لیست `validationIssues` سطح فرم موجود که هم بدون تگ رندر
می‌شود).

## پیامدها

- `ImagePromptEngine.kt`/`ImagePromptValidation.kt`/`ImagePromptAiConnector.kt`/
  `AssetModels.kt` دست‌نخورده ماندند — فقط از توابع موجود
  `ImagePromptValidation.kt` Import/استفاده شد.
- این اعتبارسنجی همچنان کاملاً Warning-only است — هیچ دکمه‌ی تولید یا
  رفتار موجودی مسدود/تغییر نکرد.
- هر ۴ Rule یتیم ADR-132 اکنون واقعاً به کاربر می‌رسند — فیچر «پرامپت
  ساخت عکس مرجع» از این نظر هم کامل شد (نه فقط تولید پرامپت، بلکه
  هشدارهای کیفیت آن هم دیده می‌شوند).

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`: موفق.
- ۴ تست تازه (یکی به‌ازای هر نوع Rule — Character با `ageRange` خالی،
  Outfit با تناقض کلیدواژه‌ی سبک فوتورئال/انیمیشنی، Location با
  `description` خالی، Object با `materialAndColor` خالی): هرکدام با
  `generate...Quick()` واقعی روی ViewModel (نه فراخوانی مستقیم تابع خالص
  دامنه، چون `generatedPrompt` فقط بعد از تولید واقعی موجود است) تأیید
  شدند که `imagePromptValidationIssues`/`outfitImagePromptValidationIssues`
  واقعاً غیرخالی می‌شود؛ تست Outfit همچنین تأیید کرد نتیجه‌ی Outfit دوم،
  Outfit اول (پیش‌فرض) را دست نمی‌زند.
- `CharacterAssetFormViewModelTest.kt`/`LocationAssetFormViewModelTest.kt`/
  `ObjectAssetFormViewModelTest.kt` (کل کلاس هر سه): بدون رگرسیون.
- `ui.assets.*`/`domain.asset.*`/`UiStringsTest.kt`: Pass.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۱۰۰۲ تست، ۱۰۰۱ Pass، ۱
  Fail — `OutputDeliveryFlowTest`، از پیش در فهرست کلاس‌های ناپایدار
  (Flaky) مستندشده‌ی این پروژه (بدون ارتباط با Asset). با اجرای مجدد و
  مجزا کامل Pass شد.
- `./gradlew :app:assembleDebug`: موفق.
