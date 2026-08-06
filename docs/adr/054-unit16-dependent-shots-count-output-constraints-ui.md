# ADR-054: رفع دو محدودیت ثبت‌شده — dependentShotsCount در Tab DNA + سه فیلد بدون UI در OutputConstraints

**تاریخ:** 2026-08-06
**وضعیت:** کامل شد.

## Context

قدم مستقل کوچک بعد از تکمیل کامل فاز ۴ واحد ۱۶. دو محدودیت ثبت‌شده در
`unit16-execution-plan.md` (که در فاز ۲ قدم ۳ — ADR-047 — عمداً به بعد از فاز ۴
موکول شده بودند، چون فاز ۴ اولین Shot واقعی را ساخت):

1. `DnaViewModel.setDominantVisualStyle` همیشه `dependentShotsCount=0` به
   `DnaValidation.updateCoreIdentity` (Rule 1) می‌داد — چون هیچ Repository ای
   شمار واقعی شات‌های یک پروژه را ارائه نمی‌داد.
2. سه فیلد `OutputConstraints` (`forbiddenElements`, `mandatoryElements`,
   `maxShotDurationSeconds`) در دامنه/DB کامل بودند اما هیچ UI ای برای
   ویرایش‌شان در Tab DNA وجود نداشت.

## تصمیم ۱: بازاستفاده از `ProjectDao.getProjectWithCounts` موجود — نه یک Query تازه در ShotDao

پیش‌بررسی معمار به‌درستی تشخیص داد که جدول `shots` ستون `projectId` مستقیم
ندارد و شمارش نیازمند JOIN با `scenes` است. اما grep قبل از نوشتن هر Query
تازه نشان داد این JOIN **از قبل** به‌صورت کامل و تست‌شده وجود دارد:
`ProjectDao.kt` (واحد ۱۶ فاز ۱، ADR-044) از قبل یک زیرکوئری همبسته دارد:

```sql
(SELECT COUNT(*) FROM shots sh WHERE sh.sceneId IN
  (SELECT sceneId FROM scenes WHERE projectId = p.projectId))
```

که از طریق `ProjectRepository.observeProjectSummary(projectId): Flow<ProjectSummary?>`
به لایه‌ی دامنه راه پیدا می‌کند و `shotCount` واقعی را برمی‌گرداند — دقیقاً همان
عددی که `StudioShell` از قبل برای نمایش «X صحنه / Y شات» در Header خودش
مصرف می‌کند (`summary?.shotCount`). این Flow روی جدول‌های `projects`/`scenes`/
`shots` توسط Room InvalidationTracker خودکار Invalidate می‌شود، پس همیشه
به‌روز است.

**به همین دلیل هیچ Query تازه‌ای اضافه نشد** (نه در `ShotDao`، نه یک JOIN
دستی در سطح Repository) — به‌جایش، `StudioShell` مقدار `summary?.shotCount`
ی که از قبل محاسبه می‌کرد را مستقیماً به‌عنوان پارامتر `dependentShotsCount: Int`
به `DnaTabContent` می‌دهد، که آن را به `viewModel.setDominantVisualStyle(style,
dependentShotsCount)` پاس می‌دهد. `DnaViewModel` هیچ وابستگی تازه‌ای به
DB/Repository اضافه نکرد — امضای تابعش فقط یک پارامتر `Int` گرفت، دقیقاً
هم‌شکل با امضای از پیش موجود `DnaValidation.updateCoreIdentity(currentDna,
newStyle, dependentShotsCount: Int)` که این عدد را از ابتدا به‌عنوان
پارامتر خارجی می‌گرفت، نه چیزی که خودش محاسبه کند.

این تصمیم مستقیماً تست تکراری را هم حذف کرد: تست الزامی «شمارش شات: یک
پروژه با چند Scene و چند Shot در هرکدام → مجموع صحیح» از قبل با
`ProjectRepositoryTest.kt` → `observeProjectSummaries counts scenes and
shots correctly through the scene-shot join` (۲ Scene، ۳ Shot، جمع=۳) پوشش
داده شده بود — همان Query که این قدم بازاستفاده می‌کند، نه یک مسیر کد
جدید. نوشتن یک تست تکراری روی همان Query امتیاز پوششی تازه‌ای اضافه
نمی‌کرد؛ به‌جایش تلاش تست این قدم روی چیزی که واقعاً تازه است متمرکز شد:
آیا `DnaViewModel` این عدد را واقعاً مصرف می‌کند.

## تصمیم ۲: نمایش هشدار Rule 1 — بازاستفاده از کانال `onShowMessage`/Snackbar سراسری موجود

الگوی `lastActionMessage: StateFlow<String?>` + `clearLastActionMessage()` +
`LaunchedEffect(lastActionMessage) { onShowMessage(it); clear() }` عیناً از
`SceneDetailViewModel`/`SceneDetailScreen.kt` (فاز ۴ قدم ۱) کپی شد —
همان کانال Snackbar سراسری که از `MainScaffold` (`snackbarHostState`) از
طریق `AppNavHost.onShowMessage` → `StudioShell.onWarning` (که همین حالا هم
برای هشدارهای `StudioTopTabRow` استفاده می‌شد) به پایین جریان دارد.
`DnaTabContent` یک پارامتر `onShowMessage: (String) -> Unit = {}` تازه
گرفت که در `StudioShell` مستقیماً `onWarning` را می‌گیرد — بدون هیچ کانال
جدید.

## تصمیم ۳: سه دسته‌ی ثابت برای `forbiddenElements` (نه فیلد متنی آزاد دسته)

`OutputConstraints.forbiddenElements: Map<String, List<String>>` عمداً یک
Map باز است (بدون enum دسته در دامنه). اما نمونه‌ی «مثلاً» بلوپرینت ۰۲
دقیقاً سه دسته می‌دهد: `camera`/`lighting`/`weather` — همان سه دسته‌ای که
`validateShotAgainstDna` (Rule 2) در عمل با آن‌ها مقایسه می‌شود (تست‌های
موجود `DnaValidationTest.kt` هم دقیقاً همین سه رشته را می‌آزمایند). یک
Dropdown بسته از این سه دسته (به‌جای یک فیلد متنی آزاد) در UI اضافه شد —
از تایپوی بی‌اثر (کلیدی که هیچ Rule ای هرگز با آن مطابقت نمی‌کند) جلوگیری
می‌کند، بدون این‌که خودِ نوع دامنه `Map<String, List<String>>` را به یک enum
محدود کند (که چیزی فراتر از دستور کار این قدم بود).

## تصمیم ۴: فیلد متن خام برای `maxShotDurationSeconds` (هم‌الگو با `ShotComposerViewModel._durationSecondsText`)

اگر فیلد مستقیماً از `dna.outputConstraints.maxShotDurationSeconds.toString()`
مشتق می‌شد، پاک‌کردن کامل فیلد برای تایپ دوباره غیرممکن می‌شد (رشته‌ی خالی
`toIntOrNull()` نال می‌دهد و بلافاصله به مقدار قبلی بازمی‌گشت). دقیقاً همان
راه‌حل از پیش تثبیت‌شده در `ShotComposerViewModel._durationSecondsText`
(فاز ۴ قدم ۲) بازاستفاده شد: یک `StateFlow<String>` مستقل برای متن خام
فیلد، که فقط وقتی `toIntOrNull() > 0` باشد مقدار واقعی `ProjectDna` را
به‌روز/ذخیره می‌کند.

## تصمیم ۵: `mandatoryElements`/`forbiddenElements` — فرم افزودن دستی ساده، هم‌الگو با actionSounds (ADR-053)

هر دو یک لیست Chip قابل‌حذف + فیلد(های) متنی + دکمه‌ی افزودن دارند —
دقیقاً همان الگوی تثبیت‌شده در Sound Tab (قدم قبل، ADR-053 تصمیم ۶). هیچ
ساخت‌وساز جدیدی (مثل Autocomplete یا اعتبارسنجی تکراری‌نبودن پیچیده) اضافه
نشد؛ `addForbiddenElement` فقط از افزودن یک مقدار تکراری در همان دسته
جلوگیری می‌کند (بررسی ساده‌ی `in current`).

## تست‌ها

- `DnaSoftLockWarningTest.kt` (فایل تست تازه، نه افزوده به `DnaTabFlowTest.kt`):
  دو تست — با `dependentShotsCount=4` هشدار واقعی حاوی «۴» نمایش داده
  می‌شود؛ با `dependentShotsCount=0` هیچ هشداری نمایش داده نمی‌شود. این دو
  تست مستقیماً `DnaTabContent` را (بدون `MainScaffold`/`StudioShell`) Mount
  می‌کنند — **یافته‌ی واقعی این قدم**: `composeRule` در `DnaTabFlowTest.kt`
  از قبل در `setUp()` یک‌بار `MainScaffold` کامل را `setContent` می‌کند؛
  Compose Test Rule اجازه‌ی دومین فراخوان `setContent` در همان تست را
  نمی‌دهد (`IllegalStateException: ...has already set content`) — به همین
  دلیل این دو تست به یک فایل کاملاً جدا (`composeRule` مستقل، بدون
  `setContent` در `setUp`) منتقل شدند.
- `DnaTabFlowTest.kt`: یک تست تازه — افزودن یک عنصر الزامی، یک عنصر ممنوع،
  و تنظیم حداکثر مدت شات، سپس بستن/بازکردن مجدد Studio (عیناً الگوی تست
  Round-Trip موجود Visual Style) → هر سه مقدار باقی می‌مانند.
  **یافته‌ی واقعی**: `assertTextContains` روی یک Chip با متن ترکیبی
  فارسی+انگلیسی («دوربین: dutch_angle ×») وقتی substring از مرز
  RTL(فارسی)→LTR(انگلیسی) عبور می‌کند («دوربین: dutch_angle») شکست
  می‌خورد، درحالی‌که همان الگو روی یک Chip کاملاً انگلیسی
  («subject_visible ×») بدون مشکل بود. رفع: بررسی substring فقط روی بخش
  انگلیسی («dutch_angle»، بدون پیشوند فارسی دسته).
- Round-Trip کامل سه فیلد جدید `OutputConstraints` در سطح DTO/Repository
  (`forbiddenElements`/`mandatoryElements`/`maxShotDurationSeconds`) از قبل
  با `ProjectDnaRepositoryTest.kt` (`fullDna`) پوشش داده شده بود — تست تازه
  در این قدم مسیر UI+ViewModel (کد واقعاً جدید این قدم) را آزمود، نه دوباره
  همان مسیر DTO/Mapper از پیش‌سبز را.

## Consequences

- هر دو محدودیت ثبت‌شده در `unit16-execution-plan.md` برای Tab DNA رفع شدند.
- `DnaViewModel` هیچ وابستگی تازه‌ای به `SceneDao`/`ShotDao`/`ProjectDao`
  پیدا نکرد — شمارش کاملاً در لایه‌ی UI (`StudioShell`، که از قبل آن را
  برای Header محاسبه می‌کرد) می‌ماند.
