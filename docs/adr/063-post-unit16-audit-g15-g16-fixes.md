# ADR-063: رفع G15 و G16 — آخرین دو نمونه‌ی الگوی باگ DI — ممیزی post-Unit16

## زمینه

طبق `docs/audit/post-unit16-full-audit.md`، «پیشنهاد ترتیب رفع»، اولویت ۳
بند ۶، این قدم آخرین دو نمونه‌ی شناخته‌شده‌ی الگوی باگ DI را یکدست
می‌کند — هر دو طبق خودِ ممیزی روی Production فعلی بی‌اثرند (فقط
تست‌پذیری/یکدستی الگو)، اما باید رفع شوند تا این کلاس باگ کاملاً از
پروژه پاک شود:

- **G15** — `ProjectListViewModel.exportProject` مستقیماً
  `AppDatabase.getInstance(getApplication())` را صدا می‌زد، برخلاف بقیه‌ی
  این کلاس (`repository: ProjectRepository` تزریقی).
- **G16** — `AiStoryBreakdownViewModel.factory` با `if (args.size == 4)`
  رفتار همه‌یا‌هیچ داشت — اگر فقط ۱ تا ۳ از ۴ Repository داده می‌شد، هر ۴
  تا (حتی آن‌هایی که واقعاً تزریق شده بودند) بی‌صدا دور ریخته می‌شدند.

## بازبینی مستقل پیش‌بررسی‌های دستور کار (طبق دستور صریح)

- `ProjectListViewModel.kt:76-90` (پیش از این قدم) — تأیید شد با grep،
  دقیقاً مطابق ادعای دستور کار: `exportProject` یک `val database =
  AppDatabase.getInstance(getApplication())` محلی می‌ساخت و همه‌ی DAO ها
  را از آن می‌گرفت.
- `exportProjectFile` (تابع دامنه‌ی زیرین، الحاق‌شده با نام مستعار از
  `ExportImportRepository.kt:42`) — تأیید شد امضایش مستقیماً
  `projectDao`/`sceneDao`/`shotDao`/`assetDao`/`projectDnaDao`/
  `audioContextDao`/`backupFileStorage` می‌گیرد، نه `AppDatabase`. پس طبق
  دستور صریح، فقط منبع این DAO ها باید عوض شود، نه خودِ
  `exportProjectFile`.
- `AiStoryBreakdownViewModel.kt:236-241` (پیش از این قدم) — تأیید شد
  دقیقاً مطابق ادعای دستور کار: `val args = listOfNotNull(...)؛ if
  (args.size == 4) { ... } else { AiStoryBreakdownViewModel(application,
  projectId) }` — یعنی حتی با ۳ از ۴ تزریق‌شده، هر سه دور ریخته می‌شدند.
- تنها محل تولیدی فراخوانی هرکدام — `ProjectListSection.kt:55`
  (`viewModel.exportProject(projectId)`، مقدار برگشتی نادیده گرفته
  می‌شود) و `AiStoryBreakdownScreen.kt:154`
  (`viewModel::confirmAndSave`) — با grep تأیید شد؛ تغییر نوع بازگشتی هر
  دو تابع به `Job` بی‌خطر است.

هیچ مغایرت دیگری پیدا نشد.

## تصمیم ۱ — G15: `database: AppDatabase?` تزریقی، هم‌الگو با ADR-059

`ProjectListViewModel` یک پارامتر تازه `database: AppDatabase? = null`
گرفت؛ `private val db: AppDatabase = database ?: AppDatabase.getInstance(application)`.
`exportProject` اکنون همه‌ی DAO ها را از `db` می‌گیرد. `factory(application,
database: AppDatabase? = null)` این پارامتر را پاس می‌دهد.
`ui/App.kt` بازچینی شد: `database` اکنون **پیش از**
`projectListViewModel` ساخته می‌شود (نه بعدش) تا بتواند به `factory` آن
تزریق شود.

**تصمیم فرعی — `DeviceBackupFileStorage` تزریق‌پذیر نشد:** با grep تأیید
شد این کلاس فقط از `context.applicationContext.filesDir` استفاده
می‌کند — بدون مشکل واقعی زیر Robolectric. دستور کار صریحاً فقط `database`
را خواسته بود؛ گسترش دامنه به این فایل هم لازم نبود.

## تصمیم ۲ — G16: هر ۴ پارامتر مستقل با `?:`، هم‌الگو با `SceneDetailViewModel.factory`

`factory` بازنویسی شد: به‌جای `if (args.size == 4)`، اکنون
`val anyInjected = storyRepository != null || assetRepository != null ||
sceneRepository != null || shotRepository != null` — و اگر `true`، هرکدام
از ۴ پارامتر مستقل با `?:` به پیش‌فرض خودش (`AppDatabase.getInstance`
ساخته‌شده) می‌رسد؛ دیگر هیچ تزریق واقعی‌ای بی‌صدا دور ریخته نمی‌شود.

## تصمیم ۳ — `ioScopeOverride` + بازگشت `Job` — زیرساخت لازم برای هر دو تست (فراتر از متن صریح دستور کار)

دستور کار صریحاً یک تست جدید برای `ProjectListViewModel` (که تا این قدم
اصلاً تستی نداشت) و یک تست تزریق‌جزئی برای `AiStoryBreakdownViewModel.factory`
خواسته بود. برای این‌که این دو تست واقعاً قابل‌اعتماد باشند — نه صرفاً
«ظاهراً سبز» — دو تغییر مستقل اضافه به هر دو ViewModel لازم شد:

1. **`ProjectListViewModel`**: `ioScopeOverride: CoroutineScope? = null` +
   `private val ioScope = ioScopeOverride ?: viewModelScope`؛ هر ۶ فراخوان
   `viewModelScope.launch` به `ioScope.launch` تغییر کرد. `exportProject`
   اکنون `Job` برمی‌گرداند (نه `Unit`).
2. **`AiStoryBreakdownViewModel`**: خودِ کلاس از قبل `ioScopeOverride`
   داشت، اما `factory()` آن را Thread نمی‌کرد — اضافه شد (پارامتر تازه‌ی
   اختیاری، فقط برای تست مستقیم factory، نه Compose). `confirmAndSave`
   هم اکنون `Job` برمی‌گرداند (نه `Unit`).

**دلیل مستند این تصمیم**: طبق یافته‌ی از‌پیش‌مستندشده در
`OutputDeliveryViewModelTest.kt`/ADR-057 (`regenerate()`)،
`Dispatchers.Unconfined` به‌تنهایی برای همگام‌سازی تست کافی نیست، چون
DAO های Suspend واقعی Room به Executor داخلی خودشان hop می‌کنند و
اجرای coroutine روی یک Thread دیگر ادامه پیدا می‌کند. تنها راه‌حل
قابل‌اعتماد مستندشده در این پروژه (نه اختراع تازه): تابع `Job` برگرداند،
تست با `.join()` واقعاً منتظر تکمیل بماند. این دقیقاً همان الگویی است که
`exportProject` (G15) نیاز داشت؛ برای `AiStoryBreakdownViewModel` هم
همین مشکل عیناً برای `confirmAndSave` (که هر ۴ Repository را از طریق
DAO های Suspend واقعی صدا می‌زند) صادق بود — پس همان الگو تکرار شد.

هر دو تغییر Zero-risk برای Production تأیید شدند (بالا، بخش بازبینی
مستقل) — تنها فراخوان‌های تولیدی مقدار برگشتی را نادیده می‌گیرند.

## تست‌ها

- **`ProjectListViewModelTest.kt`** (تازه، اولین تست مستقل این
  ViewModel): یک `database` In-Memory جدا Seed می‌شود (پروژه فقط آنجا
  وجود دارد، هرگز در Singleton سراسری). `ProjectListViewModel` مستقیماً
  با `database`/`ioScopeOverride = Dispatchers.Unconfined` ساخته می‌شود؛
  `exportProject(...).join()` صدا زده می‌شود؛ `lastActionMessage` باید
  `null` بماند (یعنی `exportProjectFile` واقعاً پروژه را در `database`
  تزریقی پیدا کرد — اگر هنوز Singleton سراسری خوانده می‌شد،
  `serializeFullProject` با «پروژه یافت نشد» شکست می‌خورد).
- **`AiStoryBreakdownViewModelFactoryTest.kt`** (تازه): فقط ۲ از ۴
  Repository (`storyRepository`، `assetRepository`) به `factory(...)`
  تزریق می‌شوند (روی یک `database` In-Memory جدا)؛ `sceneRepository`/
  `shotRepository` عمداً `null` می‌مانند (باید Fallback کنند). برای
  اجتناب از تداخل FK بین دو دیتابیس متفاوت (Shot باید در همان دیتابیسی
  باشد که Scene آن است)، JSON این تست عمداً `"shots": []` است — پس
  `confirmAndSave` حلقه‌ی Scene/Shot را اصلاً اجرا نمی‌کند و آن دو
  Repository (چه Fallback چه نه) هرگز لمس نمی‌شوند؛ فقط `assetRepository`
  تزریقی محک زده می‌شود. `viewModel.confirmAndSave().join()` صدا زده
  می‌شود؛ سپس مستقیماً از `injectedDatabase.assetDao()` (نه Singleton)
  خوانده می‌شود و ۲ Asset ذخیره‌شده (character + location) تأیید
  می‌شود — اگر باگ قدیمی هنوز بود (`args.size == 4` false برای این ۲
  تزریق، کل ۴ تا دور ریخته می‌شد)، این Asset ها در Singleton سراسری
  (خالی در این فرآیند تست) ذخیره می‌شدند و این Assertion شکست می‌خورد.

## یافته‌ی محیطی از‌پیش‌مستند (تکرارشده، بدون ارتباط با این قدم)

`ShotsFlowTest > deleting a scene that still has a shot is blocked...`
دوباره، در ۳ اجرای مستقل این قدم (کل Suite دوبار، و یک اجرای Isolated
فقط همین کلاس)، در همان نقطه (`ComposeTimeoutException` در
`ShotsFlowTest.kt:468`) شکست خورد. این دقیقاً همان Flake محیطی است که
ADR-062 قبلاً با جزئیات کامل مستند کرده بود (شکست در ۴ نقطه‌ی نامرتبط در
اجراهای مختلف + یک تست خواهر بی‌ربط با `SQLiteConnectionPool` در همان
اجراها) — نه یک باگ قطعی منطقی. `git diff` این قدم تأیید می‌کند هیچ فایلی
در مسیر وابستگی این تست (`ShotsFlowTest.kt`، `SceneRepository`،
`ShotRepository`، منطق حذف Scene/Shot) دست نخورده — فقط
`ProjectListViewModel.kt`، `AiStoryBreakdownViewModel.kt`، `App.kt`، و
۲ فایل تست تازه تغییر کردند. بدون رفع تازه‌ای در این قدم برای این Flake
(خارج از Scope دستور کار).

## نتیجه‌ی Build

`gradle :app:testDebugUnitTest` → **۶۸۳ تست (۶۸۱→۶۸۳، ۲ تست جدید)، ۶۸۲
موفق.** یک شکست (همان Flake محیطی بالا). `gradle :app:assembleDebug`
جداگانه → موفق (APK واقعی ساخته شد).

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
