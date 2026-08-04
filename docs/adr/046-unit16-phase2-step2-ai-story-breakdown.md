# ADR-046: واحد ۱۶ — فاز ۲، قدم ۲: صفحه‌ی AI Story Breakdown (اولین اتصال UI کل زنجیره‌ی واحد ۰۱ب)

**تاریخ:** 2026-08-04
**وضعیت:** کامل شد. `gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL` (۵۸۵ تست، ۰ شکست، ۰ خطا).

## Context

`domain.storybreakdown` (واحد ۰۱ب: PromptBuilder، ChunkCombiner، JsonDoctor،
StoryToDomainMapper، AiConnector) و لایه‌ی ذخیره‌سازی خروجی آن
(AssetRepository/SceneRepository) قبل از این قدم کاملاً موجود و تست‌شده بودند
(تأییدشده با grep و مطالعه‌ی مستقیم کد) اما به هیچ UI ای وصل نبودند — این قدم
اولین اتصال کامل این زنجیره از UI واقعی تا ذخیره‌سازی Room واقعی است. طبق
دستور کار صریح، فقط صفحه‌ی AI Story Breakdown ساخته شد؛ DNA Tab به قدم بعدی
موکول شد.

## تصمیم ۱: `StoryBreakdownSessionEntity` مجزا از `StoryContextEntity`

پیش‌نویس فاز ۱ صفحه (`freeformStory`/`targetShotCount`/
`defaultShotDurationSeconds`) مفهوماً بخشی از `StoryBreakdownRequest` (واحد
۰۱ب) است، نه `StoryContext` (واحد ۰۱) — دو نوع دامنه‌ی کاملاً مستقل با چرخه‌ی
حیات متفاوت (StoryContext یک‌بار در Story Wizard ساخته می‌شود؛ Session این صفحه
هر بار که کاربر یک تفکیک جدید انجام می‌دهد ممکن است عوض شود). موجودیت جدید و
مجزا با همان الگوی «`projectId` مستقیماً `@PrimaryKey`» (طبق ADR-045 تصمیم ۲،
چون رابطه واقعاً یک‌به‌یک است و نوع دامنه هیچ `id` ای ندارد) ساخته شد؛
`StoryRepository` (نه یک کلاس جدا) این متدهای جدید (`saveBreakdownSession`/
`loadBreakdownSession`) را هم گرفت، چون هر دو Entity مربوط به «داستان» پروژه‌اند
و از یک Repository منطقی سرچشمه می‌گیرند.

## تصمیم ۲: `AiStoryBreakdown` یک مسیر مستقل سطح‌بالا است (نه Sub-view داخل Tab «داستان»)

طبق `docs/design/README.md` بخش «۴. AI Story Breakdown»، این صفحه Header/Back
مستقل خودش را دارد — دقیقاً هم‌رده با ۴ مسیر ریشه‌ی موجود (Home/Projects/
Studio/Assets)، نه یک بخش اسکرول‌شونده‌ی دیگر داخل `StudioShell`. مسیر جدید
`data class AiStoryBreakdown(val projectId: String)` به `AppDestinations.kt`
اضافه شد.

## تصمیم ۳: Back Navigation Contextual برای `AiStoryBreakdown` مستقیماً در `MainScaffold.kt` Special-case شد (نه تعمیم `resolveContextualBackTarget`)

`resolveContextualBackTarget(currentRouteKey: String?): Any?` (`BackNavigation.kt`)
یک تابع خالص است که فقط رشته‌ی کلید مسیر می‌گیرد و روی
`backTargetsByRouteKey: Map<String, Any>` (۵ تست مستقیم در
`BackNavigationTest.kt`) کار می‌کند. مقصد `AiStoryBreakdown` باید
`Studio(projectId)` باشد — یک مقدار Runtime که نوع `Map<String, Any>` نمی‌تواند
پویا بسازد. تغییر امضای این تابع (مثلاً افزودن یک پارامتر `projectId`) یک تابع
خالص تست‌شده را بدون نیاز واقعی بازنویسی می‌کرد. به‌جایش، این یک مورد استثنا در
محل واقعی `navController` (`MainScaffold.kt`، با
`NavDestination.hasRoute<AiStoryBreakdown>()`/`NavBackStackEntry.toRoute<AiStoryBreakdown>()`
موجود) مدیریت شد؛ همچنین `onBack`/`onConfirmedAndSaved` صفحه (که در
`AppNavHost.kt` تعریف می‌شوند) مستقیماً `Studio(route.projectId)` را از
`backStackEntry.toRoute<AiStoryBreakdown>()` می‌سازند.

## تصمیم ۴: `ShotRepository` جدید — بدون بازاستفاده از `ProjectTransactionDao.saveShotWithSceneUpdate`

با grep تأیید شد که تا این قدم هیچ Repository ای برای ذخیره‌ی یک `Shot`
دامنه‌ای از صفر وجود نداشت؛ `ShotDao` فقط از `ProjectTransactionDao`/
`PromptGenerationRepository`/`SettingsResolutionRepository` (فقط خواندن)
استفاده می‌شد. `ProjectTransactionDao.saveShotWithSceneUpdate` عمداً استفاده
نشد — طبق کامنت خودش و ADR-017، آن تابع برای «به‌روزرسانی یک Scene موجود
همراه یک Shot» طراحی شده؛ اینجا Scene و همه‌ی Shot هایش تازه ساخته می‌شوند، نه
به‌روزرسانی یک Scene موجود. `ShotRepository` جدید فقط `Shot.toDto()`/`ShotDto`
موجود (واحد ۱۵) را به `ShotDao.saveShot` وصل می‌کند — هم‌الگو با
`AssetRepository`/`SceneRepository`. ترتیب فراخوان در
`AiStoryBreakdownViewModel.confirmAndSave()` (Scene قبل از Shot) طبق همان
هشدار FK ADR-017 است.

## تصمیم ۵: الگوی تزریق‌پذیری Repository از همان ابتدا روی `AiStoryBreakdownViewModel` اعمال شد

قدم قبل (ADR-045) یک باگ واقعی کشف کرد: `ViewModel` ساخته‌شده با
`viewModel(factory=...)` داخل یک Composable، بدون پارامتر Repository
تزریق‌پذیر، بی‌صدا از `AppDatabase.getInstance(application)` (Singleton واقعی
دستگاه) استفاده می‌کند — نوشتن‌های تست به دیتابیس اشتباهی می‌روند. این‌بار،
همان الگو (۴ Repository به‌عنوان پارامتر Nullable در `factory(...)`) از همان
ابتدا روی `AiStoryBreakdownViewModel` اعمال شد، بدون نیاز به کشف مجدد همان باگ.

## تصمیم ۶: `defaultStoryContext` در `StoryViewModel.kt` از `private` به `internal` تغییر کرد

`AiStoryBreakdownViewModel.generatePrompt()` به یک `StoryContext` نیاز دارد
(برای ساخت `StoryBreakdownRequest`)؛ اگر کاربر هنوز وارد Story Tab نشده باشد
(هیچ `StoryContext` ای ذخیره نشده)، باید همان پیش‌فرض خنثی
(`StoryType.NARRATIVE`/`Mood.CALM`، طبق ADR-045 تصمیم ۳) استفاده شود. به‌جای
تکرار همین منطق، تابع موجود `internal` شد تا هر دو ViewModel از یک منبع واحد
پیش‌فرض استفاده کنند (بدون Drift بین دو کپی مستقل).

## تصمیم ۷: مدل ساده‌شده‌ی State فاز ۲ (Chunk Combiner)

به‌جای دنبال کردن وضعیت تعمیر جداگانه به‌ازای هر Chunk، دو `StateFlow` ساده
کافی بود: `chunks: List<String>` (تکه‌های قبلاً تأییدشده با دکمه‌ی «تکه‌ی
جدید») و `currentChunkInput: String` (جعبه‌ی متنی در‌حال‌تایپ). دکمه‌ی «ادامه»
خودکار محتوای فعلی جعبه (اگر خالی نباشد) را به‌عنوان آخرین تکه لحاظ می‌کند —
یعنی کاربر برای یک پاسخ تک‌تکه‌ای مجبور به کلیک «تکه‌ی جدید» نیست. اکشن‌های
Modal تعمیر (`attemptAutoFixAndCollapse`/`dismissRepairModalForManualEdit`) هر
دو تکه‌ها را با `smartCombineChunks` هم‌زمان جمع می‌کنند و نتیجه را در همان
جعبه‌ی واحد می‌گذارند — چون بعد از ترکیب، دیگر مرز «تکه‌ی جدا» معنا ندارد.

## تصمیم ۸ (یافته‌ی واقعی حین نوشتن تست): `processAiResponse` خطاهای `autoFixable=true` (مثل کاما اضافه) را بی‌صدا و کامل داخل `repairJson` تعمیر می‌کند — Modal تعمیر دستی UI هرگز برای این نوع خطا باز نمی‌شود

`repairJson()` (واحد ۰۱ب، `JsonDoctor.kt`، از قدم‌های قبلی) وقتی
`diagnosis.autoFixable == true` است، همان‌جا `attemptAutoFix` را امتحان و در
صورت موفقیت مستقیماً `JsonRepairResult.Success(wasAutoFixed=true)` برمی‌گرداند؛
`NeedsManualRepair` (و در نتیجه‌ی زنجیره، Modal تعمیر دستی UI این قدم) فقط برای
خطاهایی صدا زده می‌شود که حتی بعد از تعمیر خودکار داخلی هم Parse نشوند. این
یک واقعیت لایه‌ی دامنه‌ی از قبل تثبیت‌شده است (ADR-033)، نه یک باگ این قدم —
تست «کاما اضافه» طبق همین واقعیت نوشته شد: مستقیم و بی‌صدا به فاز ۳ می‌رسد
(مسیر JsonDoctor + تعمیر خودکار را با اجرای واقعی کد اثبات می‌کند، نه با باز
شدن Modal). Modal UI برای انواع خطای واقعاً غیرقابل‌تعمیر خودکار (مثل
`INCOMPLETE_RESPONSE`/`UNMATCHED_BRACKET`/`UNKNOWN`) همچنان ساخته و کامل تست
شد (منطق دکمه‌های «تعمیر خودکار»/«ویرایش دستی»)، فقط با داده‌ی تست فعلی هرگز
Trigger نشد؛ بدون نیاز به تغییر کد تولید.

## تصمیم ۹ (یافته‌ی واقعی تست): تصادف متنی «تولید پرامپت» با کلید موجود `drawer.promptGenerator`

مقدار رشته‌ی `aiBreakdown.generatePromptButton` («تولید پرامپت») عیناً با مقدار
کلید از قبل موجود `drawer.promptGenerator` (لینک Drawer «تولید پرامپت»)
یکسان از آب درآمد. چون محتوای `ModalNavigationDrawer.drawerContent` همیشه در
درخت Composition زنده می‌ماند (حتی بسته، فقط بیرون از دید — یافته‌ی مستند
BottomNavBar.kt از فاز ۱)، `onNodeWithText` روی این متن در تست Ambiguous
می‌شد (۲ گره). رفع شد با افزودن `testTag` مجزا
(`AI_BREAKDOWN_GENERATE_PROMPT_BUTTON_TAG`) به دکمه‌ی واقعی صفحه — بدون تغییر
متن ترجمه (که خودش درست و از قبل تثبیت‌شده است).

## تصمیم ۱۰ (یافته‌ی واقعی تست): `performScrollTo()` برای عناصر بدون والد Scrollable شکست می‌خورد

برخلاف تصور اولیه، `performSemanticsAction(SemanticsActions.OnClick)` (یافته‌ی
ADR-045) فقط برای عناصر **داخل** یک `Column` با `verticalScroll` لازم/بی‌خطر
است. اعمال آن (با `performScrollTo()` پیش از آن) روی عناصر بیرون از هر
Scrollable (مثل کارت «پروژه‌ی جدید» در Home، دکمه‌ی تأیید Dialog تغییر نام، یا
دکمه‌های داخل `AlertDialog` تعمیر JSON) با `AssertionError: Action
performScrollTo() failed` شکست می‌خورد. این عناصر عیناً با `performClick()`
ساده (بدون Scroll) تعامل گرفتند — دقیقاً هم‌الگو با تفکیک موجود
`StoryTabFlowTest.kt`.

## تصمیم ۱۱: مقصد Navigation بعد از تأیید فاز ۳ = `Studio(projectId)` (بدون انتخاب Tab خاص)

با grep تأیید شد هنوز هیچ صفحه‌ی فهرست Scene/Shot مستقلی در `ui/` ساخته نشده
(تب «صحنه‌ها»ی `StudioShell` هنوز Placeholder است). چون `selectedTab` در
`StudioShell` یک `rememberSaveable` محلی است (نه پارامتر ورودی)، هیچ راه فعلی
برای هدایت خودکار به یک Tab خاص وجود ندارد. `onConfirmedAndSaved` مثل `onBack`
ساده به `Studio(projectId)` (Tab پیش‌فرض «داستان») ناوبری می‌کند — وقتی Tab
«صحنه‌ها»ی واقعی ساخته شد (کار فاز بعدی)، افزودن یک آرگومان «Tab اولیه» به
`StudioShell` می‌تواند این را دقیق‌تر کند؛ این محدودیت شناخته‌شده در README هم
ثبت شد.

## تست

`AiStoryBreakdownFlowTest.kt` (۴ تست End-to-End، `MainScaffold` کامل + Room
واقعی In-Memory، طبق دستور کار):

- **پاسخ معتبر ساده:** فاز ۱ (نوشتن داستان + Steppers + تولید Prompt) → فاز ۲
  (چسباندن JSON معتبر) → فاز ۳ (بازبینی) → «تأیید و ادامه» → ذخیره‌ی واقعی در
  `AssetDao` (۲ ردیف: کاراکتر + مکان)، `SceneDao` (۱ ردیف)، `ShotDao` (۱ ردیف)
  — تأییدشده با Query مستقیم دیتابیس بعد از انتظار برای بازگشت واقعی به
  `Studio` (سیگنال UI که فقط بعد از تکمیل کامل `confirmAndSave()` رخ می‌دهد).
- **JSON با کاما اضافه:** مسیر JsonDoctor + تعمیر خودکار (طبق تصمیم ۸ بالا) با
  رسیدن مستقیم به فاز ۳ اثبات می‌شود.
- **Chunk Combiner:** دو تکه که اولی با `[CONTINUE]` تمام می‌شود، با دکمه‌ی
  «تکه‌ی جدید» + «ادامه» به‌درستی ترکیب و Parse می‌شوند.
- **نام یافت‌نشده (Rule 9):** شاتی که یک نام کاراکتر ناموجود (`Eve`) ارجاع
  می‌دهد، هشدار مربوطه را در فاز ۳ نمایش می‌دهد.

## Consequences

- **آسان می‌شود:** قدم ۳ فاز ۲ (DNA Tab) می‌تواند از همان الگوی تزریق‌پذیری
  Repository و ساختار Navigation مستقل (در صورت نیاز) استفاده کند.
- **کار آینده‌ی شناخته‌شده:** `sendToAiConnector` هنوز `TODO()` است (طبق
  ADR-035، تصمیم قبلی معمار) — کاربر باید متن Prompt را دستی کپی و پاسخ را
  دستی بچسباند؛ این به‌صراحت در متن راهنمای صفحه توضیح داده شده. مقصد دقیق‌تر
  Navigation بعد از تأیید (Tab «صحنه‌ها» به‌جای پیش‌فرض «داستان») به‌محض ساخته
  شدن آن Tab قابل بهبود است (تصمیم ۱۱ بالا).
