# ADR-079: رفع نهایی Flake در `HomeProjectsStudioFlowTest` — ادامه‌ی ADR-078

## زمینه

ADR-078 (قدم تشخیصی) ریشه را با اطمینان بالا شناسایی کرد اما رفع نکرد:
یک Race بین Navigate (که از داخل یک Coroutine async روی
`ProjectListViewModel.createProject` اجرا می‌شود، طبق زنجیره‌ی
`HomeScreen.kt` → `AppNavHost.kt`) و Dispose شدن `NavBackStackEntry`
توسط تست. آن قدم یک فرضیه (`Dispatchers.Unconfined` برای
`ProjectListViewModel`) را با شواهد قوی رد کرد، و یک فرضیه‌ی
آزمایش‌نشده پیشنهاد داد: `composeRule.waitForIdle()` صریح بعد از تأیید
ایجاد پروژه، پیش از `waitUntilAtLeastOneExists`.

این قدم دستور صریح داشت: رفع واقعی، نه صرفاً آزمایش دیگر — بدون نیاز به
تأیید معمار پیش از هر آزمایش، و با اجازه‌ی صریح تغییر کد
`app/src/main/` در صورت لزوم. استاندارد پایانی: **۱۰ اجرای پیاپی موفق**
(نه ۵)، هم‌تراز با ADR-072.

## آزمایش ۱ — فرضیه‌ی پیشنهادی خودِ ADR-078: `composeRule.waitForIdle()` صریح

**تغییر:** افزودن `composeRule.waitForIdle()` بلافاصله بعد از کلیک
تأیید ایجاد پروژه، پیش از `waitUntilAtLeastOneExists(hasText(...))`، در
هر دو تست شکست‌خورده.

**نتیجه (۱ اجرای کامل Suite ترکیبی):**

| اجرا | کل تست | شکست | تست‌های شکست‌خورده |
|---|---|---|---|
| آزمایشی ۱ | ۱۱ | ۲ | `opening a project card...` (`ComposeTimeoutException` — همان پیام دقیق ADR-078) + `creating a new project...` (`IllegalStateException: NavBackStackEntry` Lifecycle — همان پیام دقیق ADR-078) |

**رد شد.** `composeRule.waitForIdle()` فقط منابع Idling شناخته‌شده‌ی
خودِ Compose (زمان‌بندی Recomposition) را همگام می‌کند — نه یک
Runnable ای که از طریق `Handler` توسط زنجیره‌ی Dispatch یک Coroutine
پست شده و خارج از ردیابی Idle-Detection کامپوز است. این با یافته‌ی
پیشین این پروژه (ADR-070/071) هم‌سو است.

## آزمایش ۲ — تحلیل دقیق‌تر: شرط انتظار مبهم است

با بازخوانی دقیق `ProjectListViewModel.kt` (`createProject`،
`renameProject`، `archiveProject` — هر سه `Unit`-محور، بدون `Job`
برگشتی، الگویی یکسان در کل این ViewModel، نه یک ناهنجاری قابل‌رفع
منحصر) و `HomeScreen.kt` (خط ۱۶۰-۱۷۹):

```kotlin
CreateProjectDialog(
    onConfirm = { name ->
        projectListViewModel.createProject(
            name = name, uiLanguage = language,
            onCreated = { onOpenProject(it.projectId) }
        )
        showCreateDialog = false
    },
    ...
)
```

فرضیه‌ی جدید: `waitUntilAtLeastOneExists(hasText(projectName))` **مبهم**
است — متن نام پروژه هم در فهرست Home (به‌روزرسانی‌شده توسط یک Room Flow
مستقل و معمولاً سریع‌تر) و هم در عنوان Tab «داستان» داخل Studio (بعد از
تکمیل واقعی Navigate خودکار async) ظاهر می‌شود. چون این شرط با **یکی**
از این دو ارضا می‌شود، تست می‌تواند با ارضای فقط فهرست Home ادامه یابد
درحالی‌که فراخوان `NavController.navigate()` هنوز در حال اجرا روی
Coroutine است — دقیقاً همان Race مستندشده در Stack Trace ADR-078.

**رفع:** جایگزینی شرط مبهم با انتظار برای یک نشانه‌ی **غیرمبهم و
منحصر به Studio**: خودِ Tab «داستان» (`studioTabTestTag(StudioTab.STORY)`)،
که فقط بعد از تکمیل واقعی Navigate در درخت Semantics ظاهر می‌شود.

```kotlin
composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
composeRule.waitUntilAtLeastOneExists(hasTestTag(studioTabTestTag(StudioTab.STORY)), timeoutMillis = 5_000)
```

در هر دو تست (`creating a new project from Home makes it appear in both
Home and Projects` و `opening a project card navigates into Studio and
shows all 4 tabs, switchable`)، جایگزین `composeRule.waitForIdle()` +
`waitUntilAtLeastOneExists(hasText(projectName))`.

**تنها فایل تغییریافته:** `app/src/test/.../HomeProjectsStudioFlowTest.kt`
(+ ایمپورت `hasTestTag`). **هیچ کد `app/src/main/` تغییر نکرد** — علت
واقعی یک شرط انتظار ضعیف در خودِ تست بود، نه یک باگ در کد محصول.

## نتایج تأییدی — ۱۰ اجرای پیاپی کامل Suite ترکیبی (استاندارد ADR-072)

Suite: `ui/scenes/*` (۴ تست) + `ui/i18n/*` (۳ تست) + `HomeProjectsStudioFlowTest` (۴ تست) = ۱۱ تست/اجرا.

| اجرا | کل تست | شکست | یادداشت |
|---|---|---|---|
| ۱ | ۱۱ | ۰ | — |
| ۲ | ۱۱ | ۰ | — |
| ۳ | ۱۱ | ۰ | — |
| ۴ | ۱۱ | ۰ | — |
| ۵ | ۱۱ | ۰ | — |
| ۶ | ۱۱ | ۰ | — |
| ۷ | ۱۱ | ۰ | — |
| ۸ | ۱۱ | ۰ | — |
| ۹ | ۱۱ | ۰ | — |
| ۱۰ | ۱۱ | ۰ | — |

**۱۰ از ۱۰ اجرای پیاپی موفق، بدون هیچ استثنا** — دو برابر حداقل استاندارد
درخواستی این قدم (۱۰، نه ۵) رعایت شد. علاوه‌بر این، `gradle
:app:assembleDebug` بعد از این تغییر با موفقیت اجرا شد (بدون رگرسیون
کامپایل).

## جمع‌بندی صادقانه

- فرضیه‌ی پیشنهادی خودِ ADR-078 (`waitForIdle()` صریح) آزمایش و **رد
  شد** با شواهد واقعی (۱ اجرا، همان ۲ شکست/پیام دقیق قبلی).
- ریشه‌ی واقعی: **شرط انتظار مبهم** در خودِ تست (`hasText(projectName)`
  به‌جای یک نشانه‌ی منحصر به Studio) — نه یک مشکل Dispatcher/Thread
  (که ADR-078 با آزمایش `Unconfined` قبلاً رد کرده بود) و نه یک باگ در
  کد محصول.
- رفع نهایی فقط تست را تغییر داد؛ هیچ کد `app/src/main/` لازم نبود.
- **۱۰ از ۱۰ اجرای پیاپی کامل Suite موفق** — استاندارد این قدم
  (هم‌تراز ADR-072) به‌طور کامل رعایت شد.

## Skills استفاده‌شده

`systematic-debugging` — دو فرضیه به‌ترتیب آزمایش شد (نه هم‌زمان)؛
فرضیه‌ی اول با شواهد واقعی (نه حدس) رد شد پیش از حرکت به فرضیه‌ی بعدی؛
فرضیه‌ی دوم مستقیماً از تحلیل دقیق Stack Trace و کد واقعی (نه حدس)
استخراج شد؛ رفع نهایی با ۱۰ اجرای مستقل (نه فقط یک اجرای «موفق به
شانس») تأیید شد — دقیقاً همان انضباط ADR-070/072.
