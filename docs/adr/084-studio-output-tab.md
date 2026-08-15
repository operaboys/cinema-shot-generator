# ADR-084: پیاده‌سازی Tab «خروجی» Studio (یافته‌ی #۱۴ appendix ADR-081)

## زمینه

طبق `docs/adr/081-appendix-mockup-audit.md`، Tab «خروجی» Studio بزرگ‌ترین
یافته‌ی باز appendix mockup بود: `StudioShell.kt`'s `when (selectedTab)` فقط
STORY/DNA/SCENES را صریح مدیریت می‌کرد؛ OUTPUT از ابتدا (ADR-050) به
`else -> StudioTabPlaceholder` می‌افتاد.

## ۱. مرجع دقیق mockup (`docs/design/Cinema Studio.html`، `st.output`، آفست ۱۹۸۸۲۸۶)

بلوک `<sc-if value="{{ st.output }}">` دقیقاً ۳ عنصر (نه بیشتر) دارد، به
همراه یک دیکشنری متن در همان فایل (آفست ~۲۰۷۷۰۰۰–۲۰۷۹۲۰۰):

| کلید | متن دقیق mockup | نقش در UI |
|---|---|---|
| `x.k19` | «مرحله ۶ — Validation» | عنوان کارت ۱ |
| `x.k20` | «۱ Blocking · ۲ Warning — تا رفع Blocking، Prompt Generation غیرفعال است» | زیرمتن کارت ۱ (آیکون قرمز `error` #FF5A6A، کلیک → `act.goValidation`) |
| `x.k21` | «مرحله ۷ — Prompt Generation» | عنوان کارت ۲ |
| `y.m4` | «PromptBlueprint · structuredParts · weighted emphasis» | زیرعنوان توصیفی کارت ۲ (فقط یک برچسب مفهومی، نه داده) |
| `y.m5` | «Quality Score — 84 / 100» | متن Pill سبز کارت ۲ (آیکون `check_circle` #3DDC97) |
| `x.k22` | «مرحله ۸ — Output Delivery» | متن دکمه‌ی گرادیانی (آیکون `ios_share`، کلیک → `act.goOutput`) |

هر دو `act.goValidation` و `act.goOutput` مقصدهای واقعی این اپ‌اند
(`AppDestinations.Validation`/`OutputDelivery`) که هر دو نیازمند
`(projectId, sceneId, sceneNumber, sceneDisplayTitle, shotId)`اند — یعنی
یک `shotId` مشخص، درحالی‌که این Tab سطح‌پروژه است (بدون Shot خاص).

## ۲. تصمیم مستقل — جایگزینی «Quality Score» با معیار واقعی «آماده‌بودن»

### چرا این یک اختراع داده‌ی جعلی می‌بود
`y.m5` («Quality Score — 84 / 100») داخل `{{ }}` است، یعنی خودِ mockup هم
آن را Data-bound طراحی کرده (نه یک متن ثابت تزئینی — برخلاف نوار پیشرفت
AI-Breakdown در ADR-083 که رنگ‌هایش Literal بودند). اما grep مستقیم روی
کل `domain/` این جلسه هیچ الگوریتم «امتیاز کیفیت ۰-۱۰۰» ندارد — نه در
`ValidationAggregator`، نه در جای دیگر. ساختن چنین امتیازی از صفر (چه
وزنی از تعداد Issue ها) یک تصمیم طراحی الگوریتمی تازه و خودسرانه می‌بود،
نه «پر کردن یک جزئیات محلی»، و می‌توانست به کاربر عددی نشان دهد که هیچ
پشتوانه‌ی واقعی محاسباتی مستندی ندارد.

### جایگزین انتخاب‌شده
خودِ متن mockup برای کارت ۱ (`x.k20`) دقیقاً «آماده‌بودن» را با همین عبارت
تعریف می‌کند: «**تا رفع Blocking، Prompt Generation غیرفعال است**». پس
معیار واقعی و از پیش موجودِ این اپ برای «آماده» دقیقاً همین است: شاتی
بدون هیچ Issue سطح BLOCKING. کارت ۲ به‌جای امتیاز جعلی، شمارش واقعی
«N از M شات آماده (بدون خطای Blocking)» را نشان می‌دهد — محاسبه‌شده از
همان `aggregateShotValidation` موجود (واحد ۰۷/ADR-055)، بدون هیچ Rule
تازه. زیرعنوان فنی `y.m4` (که خودش هم فقط یک برچسب مفهومی است، نه یک
مقدار محاسبه‌شدنی) عمداً حذف شد — جایگزینی مستقیمی برایش وجود نداشت که
جعلی نباشد.

## ۳. تصمیم مستقل — تفکیک Shot برای دو مقصد Navigate (Validation + Output Delivery)

### مسئله
این Tab سطح‌پروژه است، اما هر دو مقصد (`Validation`/`OutputDelivery`)
نیازمند یک `shotId` مشخص‌اند.

### گزینه‌های بررسی‌شده
۱. اگر پروژه دقیقاً یک Shot دارد، مستقیم باز شود.
۲. اگر چند Shot دارد، یک دیالوگ انتخاب کوتاه نشان داده شود.
۳. رفتن به آخرین Shot ویرایش‌شده.

گزینه‌ی ۳ رد شد: `ShotEntity` هیچ فیلد `lastModified` ای ندارد (تأییدشده
با خواندن مستقیم فایل Entity) — پیاده‌سازی آن نیازمند یک Migration Schema
تازه بود که خارج از بودجه‌ی این قدم (سبک‌ترین راه‌حل ممکن) است.

### تصمیم نهایی
ترکیب گزینه‌های ۱ و ۲: صفر Shot → غیرفعال (حالت خالی Tab)؛ دقیقاً یک
Shot → مستقیم Navigate؛ چند Shot → `AlertDialog` کوتاه با فهرست
`shotCode` + عنوان صحنه (نه یک صفحه‌ی سنگین تازه). این منطق مشترک
(`startAction`/`navigate` در `StudioOutputTabContent.kt`) برای هر دو کلیک
(کارت Validation و دکمه‌ی اشتراک‌گذاری) یکسان اعمال شد.

## پیاده‌سازی

- **`ShotRepository.kt`**: `loadAllShotsForProject(projectId): List<Shot>`
  تازه — معادل دامنه‌ای که تا این قدم وجود نداشت (`ShotDao.getShotsForProject`
  در سطح Dao از قبل بود، اما هیچ Wrapper دامنه‌ای برایش افشا نشده بود).
- **`UiStrings.kt`**: ۷ کلید تازه fa+en زیر `studio.tabPlaceholder`
  (`studioOutput.validationCardTitle/validationCountsTemplate/promptGenerationCardTitle/readyShotsTemplate/shareButtonLabel/emptyState/pickShotDialogTitle`).
- **`StudioOutputViewModel.kt`** (تازه): `StudioOutputShotSummary`/
  `ProjectOutputSummary` (با `totalShots/totalBlockingCount/totalWarningCount/readyShotCount`
  محاسبه‌شده) و خودِ ViewModel — یک‌بار همه‌ی Shot/Scene/DNA پروژه را
  بارگذاری می‌کند، روی هر Shot حلقه می‌زند و `aggregateShotValidation`
  موجود را صدا می‌زند (دقیقاً هم‌الگو با حلقه‌ی تک‌شاتیِ `ValidationViewModel`،
  فقط این‌بار روی همه‌ی Shot های پروژه). هیچ Rule دامنه‌ی تازه‌ای نوشته نشد.
- **`StudioOutputTabContent.kt`** (تازه): ۲ کارت وضعیت + ۱ دکمه‌ی
  گرادیانی + دیالوگ انتخاب کوتاه، طبق بخش ۲/۳ بالا.
- **`StudioShell.kt`**: شاخه‌ی صریح `StudioTab.OUTPUT -> StudioOutputTabContent(...)`
  پیش از `else -> StudioTabPlaceholder` اضافه شد (شاخه‌ی `else` بدون تغییر
  باقی ماند — طبق دستور کار، فقط برای وضعیت‌های واقعاً ناشناخته/آینده).
- **`AppNavHost.kt`**: `composable<Studio>` اکنون `shotRepository`/
  `assetRepository` را هم به `StudioShell` پاس می‌دهد، به‌همراه دو
  Callback تازه‌ی Navigate (`onNavigateToValidation`/`onNavigateToOutputDelivery`)
  که دقیقاً هم‌الگو با مسیرهای موجود `ShotComposer`→`Validation`/`OutputDelivery`اند.

## تست

`StudioOutputTabFlowTest.kt` (تازه، ۳ تست End-to-End واقعی، الگوی
راه‌اندازی از `ValidationFlowTest.kt`):
1. یک پروژه با یک Scene/Shot Seed‌شده با نقض Blocking واقعی (نور خورشید
   در شب) — کارت Validation شمارش واقعی «۱ Blocking · ۰ Warning» و کارت
   Prompt Generation «۰ از ۱ آماده» را نشان می‌دهد؛ کلیک کارت Validation
   (چون فقط یک Shot هست) مستقیماً به صفحه‌ی واقعی Validation می‌رود.
2. همان پروژه — کلیک دکمه‌ی اشتراک‌گذاری مستقیماً به `OutputDeliveryScreen`
   واقعی می‌رسد.
3. پروژه‌ای با ۲ Scene/Shot بدون هیچ نقضی («۲ از ۲ آماده») — کلیک دکمه‌ی
   اشتراک‌گذاری دیالوگ انتخاب کوتاه را نشان می‌دهد؛ انتخاب یک Shot واقعاً
   به `OutputDeliveryScreen` Navigate می‌کند.

`HomeProjectsStudioFlowTest.kt` به‌روزرسانی شد: انتظار قبلی («Tab خروجی
هنوز Placeholder است») که دیگر درست نیست، با انتظار حالت خالی واقعی
Tab (پروژه‌ی بدون هیچ Shot) جایگزین شد.

## یافته‌های واقعی دیباگ (تست)

اجرای واقعی این تست‌ها ۴ باگ/یافته‌ی واقعی را کشف کرد که هیچ‌کدام از راه
حدس رفع نشدند — همه با شواهد مستقیم (پیام خطای Compose/Assert، خواندن
کد منبع مقصد) تشخیص داده شدند:

1. **`StudioOutputViewModel` هرگز بارگذاری‌اش تمام نمی‌شد**: نسخه‌ی اولیه
   `sceneRepository.loadAllScenes(projectId).first()` را روی یک `Flow`
   صدا می‌زد — این `Flow` (برای UI زنده‌ی `ScenesListScreen` طراحی شده)
   داخل `ioScope.launch` (پیش‌فرض `viewModelScope` = Main.immediate زیر
   Robolectric) هرگز Resolve نمی‌شد. رفع با هم‌الگو‌کردن با
   `ValidationViewModel`'s اثبات‌شده: `sceneRepository.loadScene(sceneId)`
   مستقیم (suspend یک‌باره) برای هر `sceneId` متمایز شات‌های پروژه.
2. **Foreign Key واقعی Scene/Shot → Project**: `SceneEntity`/`ShotEntity`
   هر دو `ForeignKey` به `projects.projectId` دارند (`onDelete=CASCADE`)
   که Room حتی روی دیتابیس In-Memory تست هم اجرا می‌کند. نسخه‌ی اولیه‌ی
   تست Scene/Shot را پیش از ایجاد واقعی ردیف Project (از UI) Seed
   می‌کرد؛ چون `saveScene`/`saveShot` با `runCatching` پیچیده شده‌اند،
   این نقض بی‌صدا به `Result.failure` تبدیل می‌شد (نه Exception
   قابل‌مشاهده) — پروژه با ۰ Shot باز می‌شد. رفع با هم‌ترازکردن ترتیب با
   الگوی `ValidationFlowTest.kt`: اول ایجاد واقعی پروژه، بعد Seed.
3. **کارت «آماده‌بودن Prompt Generation» بدون Merge Semantics**: کارت
   دوم (`STUDIO_OUTPUT_PROMPT_CARD_TAG`) برخلاف کارت اول، `onClick`
   ندارد (صرفاً نمایشی است) — Compose به‌طور خودکار متن فرزندانش را در
   Semantics ادغام نمی‌کند مگر صریحاً درخواست شود. رفع با
   `Modifier.semantics(mergeDescendants = true)` روی خودِ Card — هم رفع
   تست، هم بهبود واقعی دسترس‌پذیری (خواننده‌ی صفحه این وضعیت را یک واحد
   معنایی می‌خواند).
4. **رسیدن واقعی به `OutputDeliveryScreen` نیازمند بیش از یک Shot معتبر
   است**: طبق یافته‌ی از پیش مستندشده‌ی `OutputDeliveryFlowTest.kt`،
   `PromptGenerationRepository.collectData` هم به یک `ProjectDna`
   *ذخیره‌شده* نیاز دارد (نه Fallback پیش‌فرض)، هم به این‌که camera و
   environment شات (نه فقط lighting) `source="override"` باشند —
   وگرنه `resolveSourcedSettings` شکست می‌خورد. فیکسچرهای این تست با
   یک ترکیب دوربین/محیط «بی‌خطر» (بدون نقض هیچ Rule سطح ۲) و ذخیره‌ی
   صریح DNA پیش‌فرض کامل شدند. علاوه بر این، `MainScaffold` این تست
   باید صریحاً `promptGenerationRepository`ای وصل به همان دیتابیس
   In-Memory تست بسازد — بدون آن، صفحه به `AppDatabase.getInstance`
   واقعی (نه دیتابیس تست) سقوط می‌کرد.

جدا از این ۴، کارت شمارش BLOCKING خودِ `ValidationScreen` (بدون هیچ
`isLoaded` Gate ای، `report?.blockingCount ?: 0`) بلافاصله با «۰» رندر
می‌شود پیش از تکمیل بارگذاری Async — همان الگوی مستندشده‌ی
`ValidationFlowTest.kt` («صبر روی متن خودِ نقض، نه صرفاً وجود testTag
کارت») دوباره لازم بود.

## راستی‌آزمایی نهایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق (فقط یک هشدار بی‌خطر «else redundant» — عمدی، برای وضعیت‌های آینده) |
| `StudioOutputTabFlowTest.kt` + `HomeProjectsStudioFlowTest.kt` (۲ اجرای متوالی، `--rerun`) | هر دو بار سبز — پایدار |
| `gradle :app:testDebugUnitTest` (کل Suite، `--rerun`) | **۷۳۹ تست، ۰ شکست** |
| `gradle :app:assembleDebug` | موفق |

## قدم بعدی پیشنهادی

باقی‌مانده‌ی appendix ADR-081 (نبود کامل `ModalBottomSheet`، اتصال واقعی
Asset↔Scene برای Tab ASSETS صحنه، پنل خلاصه‌ی زنده‌ی Composer، هدر
سراسری غیریکسان بین صفحات) هنوز رفع نشده و منتظر اولویت‌بندی/تصمیم
معمار پروژه‌اند.

## Skills استفاده‌شده

`zero-hallucination-coder` — قبل از هر تصمیم، متن دقیق mockup با grep/
خواندن مستقیم HTML خام (نه خلاصه‌ی حافظه) دوباره تأیید شد؛ منبع داده‌ی
واقعی (`aggregateShotValidation`) پیش از هر کد نوشتن جست‌وجو شد، نه فرض
گرفته شد. `decision-record` — هر دو تصمیم مستقل (جایگزینی Quality Score،
تفکیک Shot برای Navigate) با دلیل صریح در همین سند مستند شدند، شامل
گزینه‌های رد‌شده و چرایی رد آن‌ها. `proportional-effort` — راه‌حل داده
عمداً کوچک‌ترین ممکن نگه داشته شد (یک تابع Repository + یک حلقه در
ViewModel، نه یک زیرسیستم تازه)، دقیقاً طبق محدودیت صریح دستور کار.
