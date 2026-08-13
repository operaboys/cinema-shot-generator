# ADR-082: رفع ۵ یافته‌ی سطح ۱ (کوچک) از appendix ممیزی سراسری mockup (ADR-081)

## زمینه

`docs/adr/081-appendix-mockup-audit.md` یک فهرست کامل و غیرخلاصه‌شده از
یافته‌های ممیزی سراسری mockup در برابر کد فعلی بود، بدون رفع (طبق قانون همان
قدم). این قدم اولین دسته از یک لیست اولویت‌بندی‌شده‌ی بزرگ‌تر را رفع می‌کند —
۵ مورد که کوچک‌ترین و کم‌ریسک‌ترین تشخیص داده شدند. هرکدام قبل از شروع مستقلاً
با grep/خواندن مستقیم `docs/design/Cinema Studio.html` دوباره تأیید شد.

## ۱. FAB مرکزی «ایجاد سریع» بی‌اثر

### مرجع appendix
بخش «عناصر مشترک» appendix: «FAB مرکزی («ایجاد سریع») روی نوار پایین
همیشه‌روشن — پیاده‌نشده به‌طور کامل... `onQuickCreate = { /* comment only */ }`».

### تصمیم UX (طبق دستور کار این قدم)
mockup فقط یک نوع Entity برای Quick Create نشان می‌دهد (پروژه) — ساده‌ترین
تصمیم سازگار: این FAB همیشه همان دیالوگ «ایجاد پروژه‌ی جدید» را باز می‌کند
که `QuickCreateRow` در Home باز می‌کند، صرف‌نظر از صفحه‌ی فعلی، نه یک رفتار
Context-aware تازه (که خودش یک تصمیم UX جداگانه‌ی بزرگ‌تر بود).

### پیاده‌سازی
`CreateProjectDialog` (`ui/home/HomeScreen.kt`) از `private` به `public`
تغییر کرد — بدون تکرار کد، از `MainScaffold.kt` هم قابل‌فراخوانی است.
`MainScaffold.kt`: `showQuickCreateDialog` (state تازه)، `onQuickCreate`ی
که قبلاً کاملاً خالی بود اکنون این state را true می‌کند؛ دیالوگ در پایان
تابع رندر می‌شود و `onConfirm` مستقیماً `projectListViewModel.createProject`
+ `navController.navigate(Studio(...))` را صدا می‌زند (هم‌الگو دقیق با
`HomeScreen`'s `CreateProjectDialog` usage). `BottomNavBar.kt`: FAB اکنون
`testTag` دارد (`BOTTOM_NAV_QUICK_CREATE_FAB_TAG`) — قبلاً هیچ testTag ای
نداشت.

## ۲. بنر «بدون Cloud Sync» غایب در Backups

### مرجع appendix + mockup
appendix بخش Backups: «بنر «بدون Cloud Sync» — پیاده‌نشده به‌طور کامل».
متن دقیق از mockup (`docs/design/Cinema Studio.html`، `x.k49`):
`'On-Device مطلق — بدون همگام‌سازی ابری'`، آیکون `cloud_off`، رنگ `#3DDC97`.

### پیاده‌سازی
`BackupsScreen.kt`: `CloudSyncBanner` composable تازه — طبق همان الگوی
کنتراست‌فیکس‌شده‌ی این پروژه (پرکردن Solid با `CinemaTheme.extendedColors.success`،
نه Tint کم‌آلفای ۱۲٪ mockup؛ دقیقاً هم‌الگو با چیپ «ذخیره شد» StudioHeader،
ADR-055/059). بالای هر دو حالت (با/بدون پروژه‌ی فعال) رندر می‌شود، چون یک
پیام حریم‌خصوصی سراسری است، نه وابسته به پروژه. کلید تازه‌ی UiStrings:
`backups.cloudSyncBanner` (fa/en).

## ۳. چیپ فیلتر وضعیت غایب در ProjectsScreen

### مرجع appendix + mockup
appendix بخش Projects: «ردیف چیپ فیلتر وضعیت — پیاده‌نشده به‌طور کامل».
mockup (`stateFilters`): ۶ مورد دقیق — «همه»، DRAFT، REVIEW، LOCKED، FINAL،
ARCHIVED؛ هرکدام نقطه‌ی رنگی + برچسب.

### تصمیم‌های مستقل
- **enum**: `Project.state` از قبل مستقیماً `domain.stateversioning.EntityState`
  را بازاستفاده می‌کند (تأیید با grep) — بدون enum موازی.
- **رنگ/برچسب هر چیپ**: `stateChipLabel`/`stateChipColor` موجود در
  `ProjectCard.kt` (که قبلاً `private` بودند) به `internal` تغییر کردند تا
  `ProjectsScreen.kt` هم آن‌ها را بازاستفاده کند — به‌جای یک نگاشت رنگ/برچسب
  دوم و موازی (رنگ‌های دقیق mockup اندکی متفاوت‌اند، اما استفاده از نگاشت
  سراسری موجود این پروژه بر تطبیق پیکسل‌به‌پیکسل رنگ یک عنصر mockup اولویت
  دارد).
- **رفتار پیش‌فرض «همه»**: طبق دستور کار صریح این قدم («تصمیم را از
  mockup/منطق فعلی صفحه استخراج کن، حدس نزن») — نه mockup و نه کد فعلی هیچ
  منطق مخفی‌سازی پیش‌فرض ARCHIVED ندارند (این صفحه از قبل بدون هیچ فیلتری
  همه‌ی پروژه‌ها را نشان می‌داد). پس «همه» یعنی واقعاً همه، بدون استثنای
  پنهان.

### پیاده‌سازی
`ProjectsScreen.kt`: `selectedStateFilter: EntityState?` (`remember`،
پیش‌فرض `null` = همه)، `filteredSummaries` محاسبه‌شده، `ProjectStateFilterRow`
(`LazyRow` اسکرول‌پذیر افقی) + `StateFilterChip` (نقطه‌ی رنگی + برچسب،
حاشیه‌ی متفاوت وقتی انتخاب‌شده — هم‌الگو کنتراست با `OpaqueChip` سراسری
اپ). زیرعنوان شمارش («N پروژه‌ی محلی») عمداً از `summaries.size` کامل
(نه فیلترشده) استفاده می‌کند — یک برچسب شمارش کل کتابخانه، نه شمارش نمای
فیلترشده.

## ۴. طراحی واقعی Timeline (جدا از Grid) در Shots List

### مرجع appendix + mockup
appendix بخش Shots List: «Grid و Timeline... از نظر بصری یکسان‌اند».
mockup (`vm.timeline`): ریل عمودی — نقطه‌ی ۱۲px بنفش (`#7C5CFF`، margin-top
۲۴px) + خط اتصال ۲px (`var(--hair)`) بین شات‌ها.

### پیاده‌سازی
`ShotListScreen.kt`: `ShotTimelineRow` composable تازه — `Row` با
`IntrinsicSize.Min` (تا ریل و کارت هم‌ارتفاع شوند)، ریل چپ (`Column` عرض
۳۲dp: `Box` دایره‌ای ۱۲dp با `MaterialTheme.colorScheme.primary` — که
دقیقاً معادل `#7C5CFF` mockup در این تم است — + `Box` خط ۲dp با
`CinemaTheme.extendedColors.hairline`)، سپس همان `ShotCard` مشترک با Grid
(طبق اجازه‌ی صریح دستور کار: «می‌تواند از همان ShotCard موجود استفاده کند،
فقط چیدمان/تزیین اطراف آن اضافه شود» — بدون بازسازی کامل کارت با
`type·goal·motion` جداگانه‌ی mockup). `ShotCard` یک پارامتر `modifier`
تازه گرفت (پیش‌فرض `Modifier`، بدون شکستن فراخوان Grid موجود) تا در
`ShotTimelineRow` بتوان `Modifier.weight(1f)` را پاس داد.

## ۵. رنگ Hero Composer (گرادیان به‌جای رنگ تخت)

### مرجع appendix + mockup
appendix بخش Shot Composer TABS: «رنگ تخت `inset`... نه حتی به‌سبک گرادیان
جایگزین سراسری اپ». mockup (`cs-shot-hero`/`cs-shot-hero-b`، هر دو نسخه‌ی
TABS و ACCORDION): `linear-gradient(150deg,#2C4260 0%,#3C5570 48%,#6E5B72 100%)`.

### پیاده‌سازی
`ShotComposerScreen.kt`: `ComposerHeroGradientColors` (val سطح‌بالای
`internal`، ۳ رنگ دقیق mockup) + `Brush.linearGradient(...)` جایگزین
`CinemaTheme.extendedColors.inset` تخت قبلی روی همان `Box` ۱۶۰dp مشترک
(هم TABS هم ACCORDION از همین یک Box استفاده می‌کنند، پس هر دو نسخه با
یک تغییر رفع شدند).

## تست

- `QuickCreateFabTest.kt` (تازه، ۱ تست): کلیک FAB از صفحه‌ی Projects
  دیالوگ را باز می‌کند و تأیید آن واقعاً پروژه می‌سازد و وارد Studio می‌شود.
- `BackupsFlowTest.kt` (+۱ تست): بنر Cloud Sync با متن دقیق mockup نمایش
  داده می‌شود.
- `ProjectsStateFilterTest.kt` (تازه، ۱ تست): چیپ DRAFT پروژه‌ی ARCHIVED
  را واقعاً از فهرست حذف می‌کند؛ چیپ «همه» آن را برمی‌گرداند.
- `ShotsFlowTest.kt` (+۱ تست): نقطه‌ی ریل فقط در حالت Timeline رندر
  می‌شود، نه Grid.
- `ShotComposerHeroGradientTest.kt` (تازه، ۱ تست واحد ساده — نه Compose UI
  Test، چون Brush در Semantics Tree قابل‌بازرسی نیست): مقادیر دقیق ۳ رنگ
  گرادیان با مقادیر mockup برابرند.

## یافته‌ی دیباگ واقعی

`ProjectsStateFilterTest` در اولین اجرا با `ComposeTimeoutException` شکست
خورد: انتظار برای متن پروژه‌ی دوم (`Alpha`) بلافاصله بعد از Navigate به
Projects — طبق ترتیب `lastModified DESC` این پروژه آیتم #۲ فهرست بود، و
`LazyColumn` در viewport کوچک تست، آیتم دوم را اصلاً Compose نمی‌کند (نه
یک مشکل Scroll ساده، چون آیتم اصلاً در درخت Semantics وجود نداشت). رفع:
بازطراحی توالی تست تا هر Assertion همیشه روی آیتمی باشد که یا اول فهرست
است یا (بعد از فیلتر DRAFT) تنها آیتم فهرست — هرگز به آیتم #۲ در فهرست
۲تایی وابسته نیست. یک Assertion باقی‌مانده (`assertIsDisplayed` بعد از
بازگشت به «همه») همچنان با «not displayed» شکست می‌خورد با شواهد ناکافی
برای ریشه‌یابی قطعی؛ رفع با همان راه‌حل مستندشده‌ی این پروژه:
`.performScrollTo()` صریح پیش از Assertion (مثل ده‌ها مورد مشابه در
`ShotsFlowTest.kt`/`ShotComposerAccordionFlowTest.kt`). راستی‌آزمایی: ۴
اجرای پیاپی موفق (۱ اجرای اولیه بعد از رفع + ۳ اجرای تکراری صریح).

## راستی‌آزمایی نهایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:compileDebugUnitTestKotlin` | موفق |
| ۵ تست تازه/افزوده‌شده، هرکدام مجزا | همگی سبز |
| `ProjectsStateFilterTest` (۴ اجرای پیاپی) | ۴ از ۴ موفق |
| `gradle :app:testDebugUnitTest` (کل Suite، `--rerun`) | **۷۳۲ تست، ۰ شکست** |
| `gradle :app:assembleDebug` | موفق |

یک اجرای میانی کل Suite با ۱ شکست (`AssetFormFlowTest`، فایلی که این قدم
اصلاً لمس نکرد) مشاهده شد — دقیقاً همان کلاس Flake سراسری Suite که قبلاً
در ADR-070/071/072/078/079/081 ریشه‌یابی و به‌عنوان محدودیت شناخته‌شده
مستند شده (فشار زمان‌بندی CPU/GC تحت بار کامل Suite). اجرای دوباره‌ی کامل
Suite بدون هیچ شکستی این را تأیید کرد.

## قدم بعدی پیشنهادی

باقی‌مانده‌ی appendix ADR-081 (یافته‌های سطح ۲ و بزرگ‌تر — مثل Tab «خروجی»
Studio که هنوز Placeholder است، نبود کامل `ModalBottomSheet`، پنل خلاصه‌ی
زنده‌ی Composer، هدر سراسری غیریکسان بین صفحات) هنوز رفع نشده و منتظر
اولویت‌بندی/تصمیم معمار پروژه‌اند.

## Skills استفاده‌شده

`systematic-debugging` — یافته‌ی دیباگ `ProjectsStateFilterTest` با شواهد
واقعی (Stack Trace دقیق، تفاوت بین Timeout و AssertionError) قدم‌به‌قدم
ریشه‌یابی شد، نه یک رفع حدسی یک‌باره. `decision-record` — هر ۵ تصمیم
مستقل (رفتار FAB، منبع رنگ/برچسب فیلتر، رفتار پیش‌فرض «همه»، بازاستفاده‌ی
`ShotCard` برای Timeline، گرادیان مشترک هر دو نسخه‌ی Composer) با دلیل
صریح در همین سند مستند شد.
