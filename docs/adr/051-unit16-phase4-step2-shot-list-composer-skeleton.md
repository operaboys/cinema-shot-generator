# ADR-051: واحد ۱۶ — فاز ۴، قدم ۲: Shot List + اسکلت Shot Composer

**تاریخ:** 2026-08-05
**وضعیت:** کامل شد. `gradle :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL` (۶۱۲ تست، ۰ شکست، ۰ خطا).

## Context

دومین قدم فاز ۴ واحد ۱۶. منابع حقیقت: `docs/blueprints/16-user-workflow-v2.md`
بخش «مرحله ۵: Shot Creation» و `docs/design/README.md` بخش‌های «۶. Shots List»
و «۷. Shot Composer». محتوای کامل هر Tab از Shot Composer (فیلدهای دوربین/نور/
صدا/محدودیت‌های خروجی) عمداً خارج از Scope این قدم است — کار قدم ۳.

## تصمیم ۱: کمبود Repository رفع شد — `ShotRepository.loadAllShots`/`loadShot`

`ShotDao.getShotsForScene` از قبل موجود بود (واحد ۱۵) اما `ShotRepository` فقط
`saveShot` داشت — هیچ متد خواندنی. اضافه شد: `loadAllShots(sceneId): Flow<List<Shot>>`
(مرتب‌شده بر اساس `shotNumber`، چون Query خودِ DAO بدون `ORDER BY` است) و
`loadShot(shotId): Result<Shot?>` — هم‌الگو با `SceneRepository`/`AssetRepository`.

## تصمیم ۲ (باگ واقعی کشف‌شده، رفع شد): `ShotDto` فیلد `negativePromptOverride` نداشت

`Shot` (دامنه) این فیلد را از ADR-028 دارد، اما هرگز به `ShotDto` اضافه نشده
بود — یعنی هر Shot که از مسیر Repository/DAO Round-Trip می‌کرد، این فیلد را
بی‌صدا گم می‌کرد (نه Exception، فقط از دست رفتن داده). رفع شد: فیلد به `ShotDto`
اضافه شد (`String? = null`، Backward Compatible) و هر دو جهت `DtoMappers.kt`
(`ShotDto.toDomain()`/`Shot.toDto()`) به‌روزرسانی شدند. تست
`ShotRepositoryTest.kt` این فیلد را صریحاً در Round-Trip بررسی می‌کند تا این
رگرسیون دوباره رخ ندهد.

## تصمیم ۳: `WorkflowState.shotListViewMode` به‌جای Session، نه DataStore

`ShotListViewMode` (GRID/TIMELINE) از فاز ۰ در `WorkflowState` وجود داشت اما
هیچ Setter‌ای نداشت. `WorkflowViewModel.setShotListViewMode` اضافه شد که
مستقیماً `_workflowState.value` را به‌روز می‌کند — عمداً در DataStore Persist
نمی‌شود (بقیه‌ی `WorkflowState` هم همین‌طور، یک وضعیت زودگذر per-Studio-session
است). این انتخاب هم‌زمان دو مسئله را حل کرد: (۱) الزام بلوپرینت که انتخاب
Grid/Timeline باید «برای طول Session» باقی بماند، و (۲) مسئله‌ی بازنشانی حالت
بین Scene Detail ↔ Shot Composer — چون `workflowState` در طول یک Session
واحد Studio ثابت می‌ماند (فقط یک‌بار در `startWorkflowSession` ساخته می‌شود)،
بدون نیاز به `rememberSaveable` (که با هر بازسازی `SceneDetailScreen` بازنشانی
می‌شد — همان محدودیت پذیرفته‌شده‌ی ADR-049/050).

## تصمیم ۴: تغییر دید `SceneDetailTab` از `private` به `internal`

`AppNavHost.kt` و `MainScaffold.kt` (بسته‌های متفاوت، همان ماژول Gradle) برای
قانون «بازگشت از Composer دقیقاً به Tab شات‌ها» به `SceneDetailTab.SHOTS.name`
نیاز داشتند. این خودش یک خطای کامپایل تازه ایجاد کرد: `'public' function
exposes its 'internal' parameter type` — چون `SceneDetailScreen` تابعی `public`
بود با پارامتر از نوع `internal enum`. رفع شد با تغییر امضای پارامتر از
`initialTab: SceneDetailTab` به `initialTab: String = SceneDetailTab.OVERVIEW.name`؛
تبدیل به enum فقط داخل بدنه‌ی تابع انجام می‌شود (`SceneDetailTab.entries.find
{ it.name == initialTab } ?: SceneDetailTab.OVERVIEW`)، جایی که نوع `internal`
در امضای عمومی ظاهر نمی‌شود.

## تصمیم ۵: بازگشت «Composer→Shots» واقعاً حفظ می‌شود (نه یک محدودیت تازه‌ی پذیرفته‌شده)

برخلاف دو نمونه‌ی قبلی «بازنشانی به پیش‌فرض» (فیلتر Assets در ADR-049، Tab
Scenes در ADR-050)، این مورد در همان کامنت Interactions موجود در
`docs/design/README.md` (از قبل در `ui/navigation/BackNavigation.kt` نقل‌قول
شده) صراحتاً نام برده شده: «Composer→Shots، Shots/Breakdown→Studio،
SceneDetail→Studio، همه‌جای دیگر→Home». چون این یک هدف مشخصاً نام‌برده‌شده در
سند طراحی است (نه «Overview» پیش‌فرض)، پیاده‌سازی واقعی سرمایه‌گذاری شد: مسیر
`SceneDetail.initialTab: String = "OVERVIEW"` اضافه شد و در هر دو مسیر بازگشت
از `ShotComposer` (ناوبری صریح در `AppNavHost.kt` + `BackHandler` سخت‌افزاری در
`MainScaffold.kt`) با `SceneDetailTab.SHOTS.name` صدا زده می‌شود.

## تصمیم ۶: امضای `onNavigateToShot` با ۳ پارامتر (اجتناب از Fetch تکراری)

از `(String?) -> Unit` به `(String?, Int, String) -> Unit` (شناسه‌ی شات یا
null، شماره‌ی صحنه، عنوان نمایشی صحنه) تغییر کرد — چون `SceneDetailScreen`
همین حالا `Scene` بارگذاری‌شده را در اختیار دارد، این دو مقدار مستقیماً به
مسیر `ShotComposer` پاس داده می‌شوند، به‌جای این‌که Shot Composer دوباره همان
Scene را از Repository بخواند.

## تصمیم ۷: `AssetFormHeader.backTestTag` اختیاری اضافه شد

پارامتر اختیاری `backTestTag: String? = null` (پیش‌فرض `null` رفتار سه فرم
Asset موجود را بدون تغییر نگه می‌دارد) اضافه شد چون Shot Composer برای هدف‌گیری
قابل‌اعتماد دکمه‌ی برگشت در تست نیاز داشت — قبلاً `IconButton` این تابع هیچ
`testTag` نداشت.

## تصمیم ۸: Auto-Save فیلدهای سطح‌بالای Shot Composer، بدون قفل توسط اعتبارسنجی

هم‌الگو با Auto-Save بی‌صدای `DnaViewModel` (بدون دکمه‌ی ذخیره‌ی صریح)، طبق
دستور کار صریح. اعتبارسنجی زنده‌ی Rule ۱ (`domain.shot.validateShotDescription`،
Blocking اگر کمتر از ۱۰ نویسه) به‌صورت زنده نمایش داده می‌شود
(`AssetFormValidationIssueRow`) اما Auto-Save بی‌صدا را قفل نمی‌کند — برخلاف
الگوی «دکمه‌ی ذخیره‌ی غیرفعال» فرم‌های Asset (ADR-049)، چون Shot Composer اصلاً
دکمه‌ی ذخیره‌ی صریحی ندارد که غیرفعال شود؛ قفل کردن یک Auto-Save پیوسته با
اولین ضربه‌های کیبورد ریسک از‌دست‌رفتن داده داشت.

## تصمیم ۹: بازاستفاده‌ی Cross-Feature از اجزای `ui/assets`

`ShotListScreen.kt` از `OpaqueChip` (سوییچ Grid/Timeline + نشان `shotType`)
و `ShotComposerScreen.kt` از `AssetFormHeader`/`AssetFormEnumDropdownField`/
`AssetFormFlatEntries`/`AssetFormValidationIssueRow` بازاستفاده کردند — همان
تصمیم بازاستفاده‌ی الگوی ADR-049/050، به‌جای نوشتن نسخه‌ی موازی.

## یافته‌ی واقعی ۱ (تست): ترتیب Seed کردن Scene/Shot پیش از ساخت واقعی Project

اولین تلاش برای نوشتن `ShotsFlowTest.kt`، Scene/Shot را در `setUp()` (پیش از
ساخت پروژه‌ی واقعی از طریق UI در بدنه‌ی تست) Seed می‌کرد. چون `SceneEntity` یک
`ForeignKey` واقعی به `ProjectEntity.projectId` دارد (`onDelete = CASCADE`،
تأییدشده با grep روی `SceneEntity.kt`)، این باعث دو مشکل متوالی شد:

1. Seed کردن Scene پیش از وجود ردیف Project متناظر، بی‌صدا شکست می‌خورد
   (`Result.failure` درون `runCatching` داخل Repository — بدون Exception
   قابل‌مشاهده در تست).
2. با اضافه‌کردن یک `ProjectEntity` موقت پیش از Seed (هم‌الگو با
   `ShotRepositoryTest.kt`)، مشکل تازه‌ای ظاهر شد: `ProjectDao.saveProject` از
   `OnConflictStrategy.REPLACE` استفاده می‌کند که در SQLite معادل واقعی
   DELETE+INSERT روی همان کلید اصلی است. وقتی کاربر پروژه‌ی واقعی را (با همان
   `projectId`) از طریق UI می‌ساخت، این REPLACE ردیف موقت را حذف می‌کرد و همین
   حذف، به‌خاطر `onDelete=CASCADE`، تمام Scene/Shot از‌پیش‌Seed‌شده را هم پاک
   می‌کرد — فهرست همیشه خالی می‌ماند، بدون هیچ نشانه‌ی خطا.

رفع نهایی: Scene/Shot در بدنه‌ی تست، *پس از* ساخت واقعی پروژه از طریق UI Seed
می‌شوند (نه در `setUp()`) — زمانی که ردیف Project واقعاً و پایدار وجود دارد.

## یافته‌ی واقعی ۲ (باگ Production واقعی، نه فقط تست): `SceneDetailViewModel.factory` شرط `&&` نادرست

پس از رفع یافته‌ی ۱، Scene به‌درستی در لیست صحنه‌ها نمایش داده می‌شد، اما ورود
به Scene Detail همیشه با «…» (حالت بارگذاری‌نشده‌ی همیشگی) گیر می‌کرد. با
دیباگ مستقیم (`println` داخل `init` — تأییدشده که `loadScene` واقعاً
`Success(null)` برمی‌گرداند، یعنی روی یک دیتابیس *متفاوت* اجرا می‌شد، نه
دیتابیس درون‌حافظه‌ی تست) کشف شد: `SceneDetailViewModel.factory` شرط
`if (sceneRepository != null && assetRepository != null)` داشت — یعنی اگر
فقط یکی از دو Repository به `MainScaffold` تزریق می‌شد (در این تست، فقط
`sceneRepository`/`shotRepository` پاس داده می‌شد، نه `assetRepository`)، این
شرط `false` می‌شد و کل ViewModel — از‌جمله `sceneRepository`، با اینکه واقعاً
غیر-null بود — بی‌صدا به سازنده‌ی پیش‌فرض (`AppDatabase.getInstance(application)`،
دیتابیس Production Singleton، نه دیتابیس تست) برمی‌گشت. این یک باگ واقعی و
مستقل از تست است: هر Caller آینده که فقط یکی از دو Repository را در اختیار
داشته باشد (نه هردو) به همین شکل بی‌صدا با دیتابیس اشتباه کار می‌کرد. رفع شد
در `SceneDetailViewModel.kt`: هر دو Repository حالا مستقل بررسی می‌شوند
(`sceneRepository ?: ...` / `assetRepository ?: ...`) — دقیقاً هم‌راستا با
پیش‌فرض‌های خودِ سازنده‌ی کلاس؛ `ShotsFlowTest.kt` هم اکنون `assetRepository`
را صریحاً به `MainScaffold` می‌دهد (برای پوشش کامل مسیر واقعی، نه فقط برای
دور زدن باگ).

## Consequences

- `ScenesListScreen.kt`/`ScenesListViewModel.kt`/`SceneDetailViewModel.kt`
  دست‌نخورده باقی ماندند از نظر رفتار Production بجز رفع باگ `&&` بالا (بدون
  تغییر رفتار مسیرهای دیگر — هر دو تست‌های موجود `ScenesFlowTest.kt` نیز پس از
  این تغییر بدون رگرسیون سبز ماندند).
- کار آینده‌ی شناخته‌شده (قدم ۳ فاز ۴): محتوای کامل هر ۴ Tab از Shot Composer
  (دوربین/نور و محیط/صدا+کاراکترها/مدل هدف و Negative Prompt)، شامل دو محدودیت
  مستندشده در `unit16-execution-plan.md` درباره‌ی `dependentShotsCount` و
  `OutputConstraints`.
