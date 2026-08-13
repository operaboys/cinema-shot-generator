# ADR-078: بررسی ریشه‌ای Flake تازه در `HomeProjectsStudioFlowTest` — قدم تشخیصی

## زمینه

در ADR-077، اجرای ترکیبی `ui/scenes` + `ui/i18n` +
`HomeProjectsStudioFlowTest` دو شکست در `HomeProjectsStudioFlowTest`
نشان داد (Timeout/`NavBackStackEntry` Lifecycle)، بی‌ربط به تغییرات
همان قدم (فقط ۲ خط `contentDescription`). این یک امضای کاملاً متفاوت
از Flake تاریخی این پروژه (`SQLiteConnectionPool`/`database.close()`،
ADR-070/071/072) است — طبق تذکر صریح این قدم، فرض «همان علت قبلی دوباره
سر برآورده» گرفته نشد؛ این یک بررسی مستقل و از صفر بود.

## قدم ۱ — ۵ اجرای تازه‌ی همان Suite ترکیبی (بدون تغییر کد)

| اجرا | کل تست | شکست | تست‌های شکست‌خورده |
|---|---|---|---|
| ۱ | ۱۱ | ۲ | `opening a project card...` (Timeout ۵۰۰۰ms) + `creating a new project...` (`IllegalStateException: NavBackStackEntry` Lifecycle) |
| ۲ | ۱۱ | ۰ | — |
| ۳ | ۱۱ | ۰ | — |
| ۴ | ۱۱ | ۰ | — |
| ۵ | ۱۱ | ۰ | — |

**نتیجه:** این یک Flake واقعی و نامنظم است، نه Deterministic — دقیقاً
همان ۲ تست، با پیام‌های کاملاً یکسان با ADR-077 (اجرای ۱ این قدم و
ادعای ADR-077 هم‌امضا هستند). نرخ مشاهده‌شده: ۲ از ۶ اجرای کاملاً
مستقل (این ۵ + یک وقوع ADR-077) ≈ ۳۳٪.

## قدم ۲ — بررسی ساختاری: آیا این الگو (MainScaffold واقعی) منحصر به این فایل است؟

با `grep` سراسری، **۱۴ فایل تست دیگر** هم دقیقاً همین الگو
(`composeRule.setContent { MainScaffold(...) } `، با `NavController`
واقعی) دارند — از جمله `ShotsFlowTest.kt`، `ScenesFlowTest.kt`،
`AppNavigationTest.kt` و غیره. **یافته‌ی مهم که فرضیه‌ی اولیه‌ی دستور
کار را تصحیح می‌کند:** این ساختار به‌هیچ‌وجه منحصر به
`HomeProjectsStudioFlowTest` نیست — اکثریت قریب‌به‌اتفاق تست‌های
End-to-End این پروژه از همین الگو استفاده می‌کنند. پس «رندر
MainScaffold واقعی» به‌تنهایی توضیح کافی برای این Flake نیست.

با بررسی دقیق‌تر، `ProjectListViewModel` در **همه‌ی** این فایل‌ها
(نه فقط `HomeProjectsStudioFlowTest`) بدون `ioScopeOverride` ساخته
می‌شود — یعنی همیشه از `viewModelScope` واقعی استفاده می‌کند (تأییدشده
با `grep` روی `ShotsFlowTest.kt`، `ScenesFlowTest.kt`،
`AppNavigationTest.kt`، `BackupsFlowTest.kt`، `SettingsFlowTest.kt`).
این هم یک الگوی سراسری است، نه یک تفاوت منحصر.

**نتیجه‌ی صادقانه:** علت اینکه چرا این Flake دقیقاً در همین دو تست این
فایل خاص (نه در `ShotsFlowTest`/`ScenesFlowTest` که همان الگوی
`createProject` را هم دارند) ظاهر می‌شود، با شواهد این قدم به‌طور کامل
مشخص نشد — به احتمال زیاد به فاصله‌ی زمانی/تعداد گام‌های UI بین کلیک
«ایجاد پروژه» و اولین `assert`/`waitUntil` بعدی در این دو تست خاص
مربوط است (کوتاه‌تر از الگوی معمول در فایل‌های دیگر)، اما این فرضیه در
همین قدم آزمایش نشد — بررسی بیشتر (اجرای همین Suite ترکیبی با یکی از
فایل‌های دیگر) به دلیل محدودیت زمان/تناسب حجم انجام نشد.

## قدم ۳ — تحلیل کامل Stack Trace

**شکست ۱ (`opening a project card...`)، `HomeProjectsStudioFlowTest.kt:163`:**
```
androidx.compose.ui.test.ComposeTimeoutException: Condition (at least
one node matches (Text + InputText + EditableText contains 'Studio Test
Project')) still not satisfied after 5000 ms
	at ...waitUntilAtLeastOneExists(AndroidComposeTestRule.android.kt:394)
	at HomeProjectsStudioFlowTest.kt:163
```
یک `waitUntilAtLeastOneExists` ساده که Timeout می‌خورد — نشانه‌ی عدم
هم‌زمانی Compose/Navigation، نه یک Crash.

**شکست ۲ (`creating a new project...`)، یک Crash واقعی، نه فقط Timeout:**
```
java.lang.IllegalStateException: State must be at least 'CREATED' to
be moved to 'DESTROYED' in component NavBackStackEntry(...)
destination=... route=.../Studio/{projectId}?initialTab={initialTab}
	at LifecycleRegistry.moveToState(...)
	at NavBackStackEntryImpl.setMaxLifecycle$navigation_common_release(...)
	at NavControllerImpl.navigate$navigation_runtime_release(...)
	at AppNavHostKt.AppNavHost$lambda$0$0$0$0$0(AppNavHost.kt:70)
	at HomeScreenKt.HomeScreen$lambda$9$0$0(HomeScreen.kt:170)
	at ProjectListViewModel$createProject$2.invokeSuspend(ProjectListViewModel.kt:58)
	at ...DispatchedTask.run(...)
	at Handler.$$robo$$android_os_Handler$handleCallback(...)
```
این دقیقاً یک Race است: کال‌بک Navigate (`onCreated = {
onOpenProject(it.projectId) }`، طبق کامنت هدر خودِ همین فایل) از داخل
Coroutine ای که `ProjectListViewModel.createProject` روی `viewModelScope`
اجرا می‌کند، بعد از اینکه تست/Compose Rule شروع به Dispose کردن کرده
(یک `NavBackStackEntry` دیگر که پیشتر ساخته شده، دیگر قابل `DESTROYED`
شدن نیست) فراخوانی می‌شود.

## قدم ۴ — یک آزمایش هدفمند: `ioScopeOverride` برای `ProjectListViewModel`

**فرضیه:** چون `ProjectListViewModel` در این فایل (هم‌الگو با همه‌ی
فایل‌های دیگر) بدون `ioScopeOverride` ساخته می‌شود، `createProject`
روی `viewModelScope` واقعی (نه Deterministic) اجرا می‌شود — دادن
`ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)` (هم‌الگو با
`workflowViewModel` در همین فایل) باید این Race را از بین ببرد.

**آزمایش:** یک تغییر موقت و مجزا در `HomeProjectsStudioFlowTest.kt`
(فقط تست، هیچ کد `app/src/main/` تغییر نکرد): افزودن
`ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)` به سازنده‌ی
`projectListViewModel`. یک اجرای کامل Suite ترکیبی همان سه گروه.

| اجرا | کل تست (این فایل) | شکست | تست‌های شکست‌خورده |
|---|---|---|---|
| آزمایشی ۱ | ۴ | **۴** | همه‌ی ۴ تست — امضاهای تازه: `AssertionError: TestTag studioTab.story is not displayed`، **`IllegalStateException: Method setCurrentState must be called on the main thread`**، دو `ComposeTimeoutException` دیگر |

**نتیجه: فرضیه به‌طور قطعی رد شد — با شواهد قوی‌تر از قبل.**
`Dispatchers.Unconfined` نتیجه را بدتر کرد (۴ از ۴ شکست، به‌جای Flake
نامنظم ۲ از ۶) و یک نوع Crash کاملاً تازه اضافه کرد: «باید روی Main
Thread صدا زده شود» — یعنی `Dispatchers.Unconfined` باعث شد کال Navigate
روی همان Thread ای که Coroutine قبلی (احتمالاً Executor داخلی Room،
دقیقاً همان مکانیزم مستندشده در کامنت هدر خودِ این فایل درباره‌ی
`projectSummaries`) تعلیق شده بود اجرا شود — نه لزوماً Main Thread.
این تأیید می‌کند `viewModelScope` واقعی (Main-محور) اینجا **درست کار
می‌کرد**، نه بخشی از مشکل.

**این تغییر آزمایشی بلافاصله Revert شد** — `git diff` بعد از Revert
خالی است (تأییدشده).

## جمع‌بندی صادقانه

- این Flake **واقعی و تأییدشده** است (۲ از ۶ اجرای مستقل)، اما
  **رفع نشد** در این قدم — طبق قانون این قدم («قدم تشخیصی، نه لزوماً
  رفع کامل»).
- یک فرضیه‌ی مشخص و قابل‌آزمایش رد شد، با شواهد قوی (نه فقط رد شد،
  بلکه دلیل رد شدنش هم روشن است: مشکل Thread Safety، نه فقدان
  Determinism).
- ریشه‌ی دقیق هنوز نامشخص است. نامزد محتمل باقی‌مانده (آزمایش‌نشده):
  یک `composeRule.waitForIdle()` صریح بلافاصله بعد از کلیک تأیید ایجاد
  پروژه، پیش از `waitUntilAtLeastOneExists` — این با معماری فعلی
  (`viewModelScope` واقعی + Navigate از یک Callback async) سازگارتر
  است چون فقط منتظر Idle واقعی می‌ماند، نه Dispatcher را عوض می‌کند.
  **این فرضیه در این قدم آزمایش نشد** (پیشنهاد صریح برای قدم بعدی،
  منتظر تأیید).
- **هیچ کد `app/src/main/` در این قدم تغییر نکرد.** تنها تغییر تست
  (آزمایش ioScopeOverride) کاملاً Revert شد؛ `git status`/`git diff`
  بعد از این قدم دقیقاً با شروع آن یکسان است.

## قدم بعدی پیشنهادی (منتظر تأیید، اجرا نشده)

۱. آزمایش `composeRule.waitForIdle()` صریح بعد از تأیید ایجاد پروژه در
هر دو تست شکست‌خورده — ۵ اجرای کامل تازه برای تأیید/رد.
۲. اگر گزینه‌ی ۱ کار نکرد: اجرای همین Suite ترکیبی با یکی از فایل‌های
دیگر (`ShotsFlowTest`/`ScenesFlowTest`) به‌جای `HomeProjectsStudioFlowTest`
برای فهم اینکه آیا این کلاس Flake فقط منحصر به این فایل است یا یک
الگوی گسترده‌تر که فقط در فایل‌های دیگر کمتر ظاهر می‌شود.

## Skills استفاده‌شده

`systematic-debugging` — یک فرضیه در هر آزمایش، یک متغیر تغییر کرد،
شواهد واقعی (نه حدس) پیش از هر ادعا؛ رد صریح فرضیه‌ی اول با وجود اینکه
منطقی به نظر می‌رسید، به‌محض این‌که شواهد آن را رد کردند — دقیقاً همان
انضباط ADR-070.
