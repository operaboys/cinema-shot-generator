# ADR-044: واحد ۱۶ — فاز ۱ (App Shell): Home، Projects، Studio Shell، Assets Container، Nav Drawer

**تاریخ:** 2026-08-04
**وضعیت:** کامل شد. `gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL`.

## Context

فاز ۰ (ADR-042/043) فقط زیرساخت مشترک (Design Tokens، اسکلت Navigation دو‌لایه،
`WorkflowViewModel`) را ساخت — هیچ صفحه‌ی واقعی وجود نداشت. این فاز اولین صفحات
واقعی محصول را می‌سازد: Home، Projects، Studio Shell (Container خالی)، Assets
Container (Placeholder)، و منوی همبرگری. منبع حقیقت دوگانه: بلوپرینت ۱۶
نسخه ۷ (منطق/ساختار) و `docs/design/README.md` (توکن‌های دقیق UI هر صفحه).

قبل از این فاز، `ProjectDao.getAllProjects()` وجود داشت اما هیچ Repository ای
برای Project نبود، و `ProjectEntity` نه `state` داشت نه شمارش صحنه/شات.

## تصمیم ۱: `EntityState` یک فیلد واقعی و Persist‌شده روی `ProjectEntity` است، نه Placeholder سطح UI

**گزینه‌های بررسی‌شده:**
- (الف) فیلد جدید `state: String` روی `ProjectEntity` (نیاز به هیچ Migration
  ای ندارد چون `version = 1` و هنوز منتشر نشده — تأییدشده با grep روی
  `AppDatabase.kt`).
- (ب) محاسبه/Placeholder ثابت (مثلاً همیشه `DRAFT`) در لایه‌ی UI/ViewModel تا
  وقتی واحد ۱۲ (State Machine) کامل به Project سیم‌کشی شود.

**تصمیم: گزینه (الف).** دلایل:
1. **سازگاری معماری:** واحد ۱۲ (`domain.stateversioning`) از قبل کامل و تست‌شده
   بود؛ `EntityState`/`ALLOWED_TRANSITIONS`/`canTransition()` واقعی‌اند، نه
   طرح‌شده برای آینده. یک Placeholder ثابت یعنی ساختن یک دروغ موقت روی چیزی که
   همین الان واقعی است.
2. **سادگی:** چون Migration لازم نیست (نسخه‌ی دیتابیس هنوز منتشر نشده)، هزینه‌ی
   فیلد واقعی تقریباً صفر است؛ در مقابل، یک Placeholder سطح UI بعداً باید کامل
   بازنویسی شود (بدهی فنی مستند اما غیرضروری).
3. **کارت پروژه (بخش الف دستور کار) واقعاً به یک State Chip واقعی نیاز دارد** —
   یک مقدار ثابت جعلی، تست end-to-end واقعی («ایجاد پروژه → ظاهر با وضعیت
   درست») را غیرممکن می‌کرد.

**پیامد فرعی کشف‌شده:** طبق `ALLOWED_TRANSITIONS` واقعی، `ARCHIVED` فقط از
`FINAL` قابل‌دسترس است. یعنی منوی Overflow «آرشیو» روی یک پروژه‌ی تازه‌ساز
(`DRAFT`) واقعاً باید شکست بخورد — این یک تنش واقعی و شفاف بین Mockup طراحی
(که فرض می‌کند Archive همیشه در دسترس است) و State Machine رسمی پروژه است، نه
یک باگ. `ProjectLifecycle.archiveProject()` این را با `Result.failure` و پیام
فارسی دقیق (نه یک No-Op بی‌صدا) مدل کرده؛ `ProjectListViewModel.archiveProject`
شکست را در `lastActionMessage` به کاربر نشان می‌دهد.

## تصمیم ۲: شمارش صحنه/شات یک Query محاسبه‌شونده (SQL Subquery) است، نه یک فیلد Denormalized

**گزینه‌های بررسی‌شده:**
- (الف) `sceneCount`/`shotCount` به‌عنوان فیلد ذخیره‌شده روی `ProjectEntity`،
  با به‌روزرسانی دستی هر جا Scene/Shot تغییر می‌کند.
- (ب) Query محاسبه‌شونده — `ProjectDao.getAllProjectsWithCounts()` با دو
  Correlated Subquery (`SELECT COUNT(*) FROM scenes ...`/`shots ...`) روی
  `projects`.

**تصمیم: گزینه (ب).** دلایل:
1. **صحت/عدم Staleness:** فیلد Denormalized نیاز به به‌روزرسانی دستی همزمان در
   هر مسیر کد دارد که Scene/Shot اضافه/حذف می‌کند (`SceneRepository`،
   `ShotRepository`، Cascade Delete، Import/Restore، ...) — دقیقاً همان کلاس
   باگ («شمارنده‌ی حافظه‌ی کهنه») که پروژه قبلاً در ADR-036/038 برایش هزینه داده.
2. **Room و Flow به‌صورت خودکار Invalidate می‌شوند:** Room جدول‌های
   خوانده‌شده در یک `@Query` را ردیابی می‌کند و با هر `INSERT`/`UPDATE`/`DELETE`
   روی `scenes`/`shots`، `Flow<List<ProjectWithCounts>>` را دوباره Emit
   می‌کند — بدون نیاز به `Flow.combine`/`flatMapLatest` دستی (که
   `@ExperimentalCoroutinesApi` هم هست).
3. **کارایی:** تعداد پروژه‌های یک کاربر تک‌کاربره روی دستگاه بسیار کوچک است
   (ده‌ها، نه میلیون‌ها)؛ هزینه‌ی دو Subquery به‌ازای هر ردیف Project ناچیز است
   — این یک تصمیم درست «Correctness-first» برای این مقیاس است، نه یک
   بهینه‌سازی زودهنگام اشتباه.

## تصمیم ۳: ساخت `ProjectRepository` (نه بای‌پس مستقیم `ProjectListViewModel` → `ProjectDao`)

طبق الگوی مستقر کل پروژه (`SceneRepository`/`AssetRepository`/... واحد ۱۵)، هیچ
ViewModel مستقیماً یک DAO خام نمی‌بیند. `ProjectRepository` علاوه‌بر CRUD ساده،
دقیقاً همان جایی است که تصمیمات دامنه‌ای واقعی می‌نشینند: اعمال Rule واقعی
State Machine در `archiveProject` (با import alias
`domain.project.archiveProject as applyArchiveTransition` برای پرهیز از تصادف
نام با متد هم‌نام خودِ Repository)، تولید شناسه/Timestamp
(`idProvider`/`clock` تزریق‌پذیر برای تست Deterministic، هم‌الگو با
`ioScopeOverride` فاز ۰).

## تصمیم ۴: `duplicateProject` فقط پوسته را کپی می‌کند (نه Scene/Shot/Asset/DNA)

چون هیچ ID-Remapping ای برای فرزندان (`sceneId`/`shotId`/...) ساخته نشده،
یک تکثیر عمیق ساده‌لوحانه یا داده‌ی پروژه‌ی اصلی را (نه کپی‌اش را) خراب می‌کرد
یا نیاز به یک الگوریتم Remap کامل داشت که این فاز جزو Scope اش نیست (و فعلاً هم
محتوای واقعی Scene/Shot برای تست وجود ندارد — آن‌ها کار فازهای ۲ به بعدند).
مستند شده، نه یک محدودیت پنهان: نام کپی `"{name} (کپی)"`، شناسه/Timestamp تازه،
وضعیت بازنشانی‌شده به `DRAFT`.

## تصمیم ۵: خروجی‌گیری (Export) با بازاستفاده از تابع آزاد موجود `exportProject`

واحد ۱۵ از قبل `exportProject` (در `ExportImportRepository.kt`) را داشت.
`ProjectListViewModel.exportProject` مستقیماً همان تابع را (با
`DeviceBackupFileStorage`/DAO های واقعی از `AppDatabase.getInstance`) صدا
می‌زند — طبق همان انضباط ضد-تکرار پروژه (ADR-036/038/042): هیچ پیاده‌سازی موازی
جدیدی برای همان قابلیت ساخته نشد.

## تصمیم ۶: منوی Overflow — کدام ۵ عملیات نیاز به متد جدید Repository داشتند

طبق «More_options.txt مورد ۲»: تغییر نام، تکثیر، آرشیو، خروجی، حذف. با grep
تأیید شد که پیش از این فاز **هیچ‌کدام** روی `ProjectRepository` وجود نداشت
(چون خودِ Repository هم وجود نداشت) — هر ۵ متد
(`renameProject`/`duplicateProject`/`archiveProject`/`exportProject`
[بازاستفاده]/`deleteProject`) در همین فاز اضافه شدند.

## تصمیم ۷: منوی همبرگری — مقصدهای بدون صفحه‌ی واقعی، Snackbar «به‌زودی» نشان می‌دهند (نه ظاهر غیرفعال/خاکستری)

طبق `docs/design/README.md` بخش Interactions، منو سه گروه دارد: STUDIO
(دستیار داستان، تفکیک داستان با AI، مدیریت DNA، صحنه‌ها، شات‌ها، دارایی‌ها)،
TOOLS (اعتبارسنجی، تولید پرامپت، تحویل خروجی)، SYSTEM (تنظیمات، بکاپ‌ها). فقط
«دارایی‌ها» یک مسیر Navigation واقعی دارد (هم‌زمان با نوار پایین). دو گزینه
برای بقیه بررسی شد:
- (الف) ظاهر Disabled/خاکستری.
- (ب) `NavigationDrawerItem` کاملاً فعال/قابل‌کلیک، اما `onClick` یک Snackbar
  با پیام «این بخش به‌زودی در دسترس خواهد بود» نشان می‌دهد.

**تصمیم: گزینه (ب).** یک آیتم خاکستری/غیرفعال در تست دستی معمولاً به چشم «این
قابلیت خراب/فراموش‌شده است» می‌آید؛ Snackbar واضح می‌گوید این بخش هنوز ساخته
نشده (وضعیت طبیعی یک اپ در حال توسعه‌ی فازبندی‌شده)، بدون این‌که هیچ‌کدام از
۱۱ مقصد Crash کنند یا یک مسیر Navigation نامعتبر بسازند.

## تصمیم ۸: بازآرایی `StudioTopTabRow` از `MainScaffold` به `StudioShell`

فاز ۰ این Tab Row را مستقیماً در `MainScaffold` (سطح ناوبری اپ) رندر می‌کرد،
چون هنوز خودِ صفحه‌ی Studio وجود نداشت. حالا که `StudioShell` (بخش ج) واقعاً
ساخته شد، مکان طبیعی این Tab Row داخل خودِ Shell است (کنار Header/Back/Saved
Chip)، نه در سطح Scaffold — `MainScaffold` فقط نوار پایین (لایه‌ی ۱، همیشگی) و
میزبانی `NavHost`/`ModalNavigationDrawer` را نگه می‌دارد؛ هیچ منطق Tab دیگر در
آن نیست.

## تصمیم ۹: انتخاب Tab فعلی Studio، `rememberSaveable` محلی در `StudioShell` است (نه `WorkflowViewModel`)

هم‌راستا با تصمیم مشابه فاز ۰ (ADR-042): سند طراحی «Language/theme/layout» را
Persisted و «Active project tab» را State ناوبری لحظه‌ای طبقه‌بندی کرده. هیچ
تغییری در این تفکیک لازم نبود، فقط پیاده‌سازی واقعی‌اش در `StudioShell`.

## تصمیم ۱۰: باگ تصادف متن تست‌های Compose (`ModalNavigationDrawer` همیشه در Composition)

بعد از افزودن Drawer واقعی، ۷ تست Compose UI با
`AssertionError: Expected exactly '1' node but found '2' nodes` شکست خوردند.
علت ریشه‌ای: محتوای `drawerContent` در `ModalNavigationDrawer` همیشه در درخت
Semantics حاضر است (حتی وقتی بسته/خارج از دید Translate‌شده)، پس برچسب‌های
متنی مشترک بین Drawer و بقیه‌ی UI («استودیو»/nav.studio+drawer.groupStudio،
«دارایی‌ها»/nav.assets+drawer.assets، «صحنه‌ها»/studioTab.scenes+drawer.scenes)
با `onNodeWithText` Ambiguous می‌شوند. یک کلاس مشابه در خودِ دیالوگ ایجاد/تغییر
نام پروژه هم پیدا شد: عنوان `AlertDialog` («پروژه‌ی جدید») دقیقاً همان متن
`QuickCreateRow` است، پس `onNodeWithText(...).performTextInput(...)` هم به
دو گره می‌رسید (و هیچ‌کدام واقعاً خودِ `OutlinedTextField` نبود).

**تصمیم: `Modifier.testTag(...)` روی هر عنصر تعاملی که این تصادف را دارد** —
نه تغییر محتوای ترجمه فقط برای فرار از این تصادف تست (برچسب‌های تکراری بین
نواحی مختلف UI یک الگوی طبیعی و معتبر در اپ‌های واقعی است، نه یک باگ محتوا).
اعمال شد روی: ۴ آیتم `BottomNavBar` (`BOTTOM_NAV_*_TAG`، از فاز ۰ باقی مانده و
اینجا هم استفاده شد)، ۴ `Tab` در `StudioTopTabRow` (`studioTabTestTag`، از فاز
۰)، فیلد نام در `CreateProjectDialog` (`CREATE_PROJECT_NAME_FIELD_TAG`، جدید
این فاز)، فیلد نام در `RenameProjectDialog` (`RENAME_PROJECT_NAME_FIELD_TAG`،
جدید این فاز، برای ثبات — هرچند تست فعلی این فاز مستقیماً از آن استفاده
نمی‌کند). `AppNavigationTest.kt` و `HomeProjectsStudioFlowTest.kt` هر دو
بازنویسی شدند تا از `onNodeWithTag` به‌جای `onNodeWithText` برای این موارد
استفاده کنند.

## تصمیم ۱۱: محدودیت‌های مستند بخش الف (Home)

- **تصویر پس‌زمینه‌ی Full-bleed:** طبق تصریح صریح دستور کار، یک Placeholder
  گرادیانت جایگزین آپلود واقعی کاربر شد؛ آپلود واقعی («Home Screen Image» در
  Settings) کار فاز Settings آینده است.
- **لوگوی «Aperture C»:** یک آیکون Material موقت (`Icons.Filled.Movie`)
  جایگزین SVG سفارشی سند طراحی شد — وارد کردن `ImageVector` سفارشی از Path
  Data یک کار جداگانه‌ی Visual Polish است.
- **Toggle های زبان/تم:** واقعاً به `WorkflowViewModel` فاز ۰ سیم‌کشی شدند
  (نه Cosmetic) — `onToggleLanguage`/`onToggleTheme` مستقیماً
  `setLanguage`/`setTheme` را صدا می‌زنند.

## تست

- `ProjectLifecycleTest.kt` (۴ تست): `archiveProject` روی FINAL/DRAFT/REVIEW/
  LOCKED/ARCHIVED.
- `ProjectMappersTest.kt` (۳ تست): Round-trip کامل `EntityState`/`Language`.
- `ProjectRepositoryTest.kt` (۷ تست، Room واقعی In-Memory): ایجاد+شمارش صفر،
  صحت Join شمارش صحنه/شات، تغییر نام، Archive (شکست روی DRAFT/موفق روی FINAL
  دستی)، تکثیر (پوسته‌ای)، حذف (Cascade واقعی).
- `AppNavigationTest.kt` (۴ تست، بازنویسی‌شده با `onNodeWithTag`).
- `HomeProjectsStudioFlowTest.kt` (۳ تست جدید End-to-End): ایجاد پروژه از Home
  → ظاهر در Home و Projects؛ ورود به Studio از کارت → ۴ Tab قابل‌جابه‌جایی؛
  حذف واقعی از منوی Overflow.
- `BackupManagerTest.kt`: یک تست Regression جدید — `state` پروژه در
  Backup/Restore واقعاً حفظ می‌شود (همان کلاس باگ ADR-036/038: فیلد جدید که در
  DTO فراموش شود یعنی از دست رفتن بی‌صدای داده در Export/Import).

## Consequences

- **آسان می‌شود:** فازهای ۲ تا ۵ (Story→DNA→Scenes→Output) می‌توانند مستقیماً
  از `ProjectRepository`/`ProjectListViewModel`/`StudioShell` موجود استفاده
  کنند و فقط محتوای هر Tab را پر کنند، بدون بازطراحی Shell.
- **کار آینده‌ی شناخته‌شده (نه بدهی پنهان):** تصویر پس‌زمینه‌ی واقعی Home
  (Settings)؛ لوگوی SVG سفارشی؛ ۱۰ مقصد Drawer بدون صفحه‌ی واقعی (فازهای بعدی)؛
  تکثیر عمیق واقعی Scene/Shot/Asset با ID-Remapping؛ مفهوم «آخرین/فعال پروژه»
  برای جایگزینی `PLACEHOLDER_ACTIVE_PROJECT_ID` نوار پایین.
