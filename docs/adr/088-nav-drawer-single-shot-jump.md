# ADR-088: پرش مستقیم Nav Drawer به Validation/OutputDelivery وقتی پروژه دقیقاً یک Shot دارد (یافته‌ی #۱۰ appendix ADR-081)

## زمینه

طبق ADR-061، لینک‌های «اعتبارسنجی» و «تحویل خروجی» در Nav Drawer همیشه به Tab
«صحنه‌ها»ی Studio می‌روند — چون این دو مقصد به `shotId` مشخص نیاز دارند
(`AppDestinations.kt`: `Validation`/`OutputDelivery` غیر-nullable) که Drawer
در سطح اپ (بدون آگاهی از یک Shot خاص) نمی‌داند. این Fallback درست و مستند بود،
اما یک حالت واقعی را نادیده می‌گرفت: وقتی پروژه‌ی جاری دقیقاً یک Shot دارد،
هیچ ابهامی در انتخاب وجود ندارد — کاربر باید بدون گام میانی Studio→صحنه→شات
مستقیماً به همان Shot برسد.

## تصمیم قطعی (از پیش با کاربر گرفته‌شده)

اگر پروژه‌ی جاری دقیقاً یک Shot داشته باشد، این دو لینک مستقیماً همان Shot را
باز می‌کنند. در غیر این صورت (صفر یا بیش‌از‌یک Shot، یا پروژه‌ی فعالی وجود
نداشته باشد) رفتار قبلی (Fallback به Tab «صحنه‌ها») بدون تغییر می‌ماند —
تنها تغییر مجاز همین استثنا بود.

## چرا منطق در MainScaffold.kt، نه NavDrawer.kt

`NavDrawer.kt` عمداً یک لایه‌ی Presentation خالص است: هیچ Repository/Coroutine
Scope ای ندارد، فقط callback های ساده‌ی `() -> Unit` از فراخوان (`MainScaffold.kt`)
می‌گیرد. `MainScaffold.kt` از قبل به `navController`، `coroutineScope`،
`shotRepository`، `sceneRepository` و `activeOrRecentProjectId` دسترسی دارد —
همان الگوی دقیق هر Drawer Action دیگر (Assets/Settings/Backups/Studio/AiBreakdown:
callback ساده در Drawer، منطق واقعی در MainScaffold). این مرز حفظ شد؛ فقط دو
enum/callback تازه (`DrawerAction.VALIDATION`/`OUTPUT_DELIVERY`،
`onNavigateValidation`/`onNavigateOutputDelivery`) اضافه شدند.

## پیاده‌سازی

- **`resolveSingleShotForProject`** (تابع suspend خصوصی تازه، `MainScaffold.kt`):
  `shotRepository.loadAllShotsForProject(projectId).singleOrNull()` را می‌گیرد؛
  اگر دقیقاً یک Shot بود، `sceneRepository.loadScene(shot.sceneId)` را هم
  می‌خواند (چون `Validation`/`OutputDelivery` به `sceneNumber`/`sceneDisplayTitle`
  نیاز دارند که فقط از `Scene` در دسترس‌اند، نه از خودِ `Shot`). هر شکست دفاعی
  (Repository تزریق‌نشده، صفر/بیش‌از‌یک Shot، Scene والد پیدا نشد) با `null`
  گزارش می‌شود که فراخوان دقیقاً مثل Fallback موجود تفسیرش می‌کند —
  بدون شرط تازه/جدا برای هر حالت خطا.
- `sceneDisplayTitle` مقصد از تابع بازاستفاده‌شده‌ی موجود
  `ui/scenes/SceneLabels.kt` ساخته می‌شود (نه بازتعریف).
- **یافته‌ی واقعی این قدم — Navigate بعد از suspend روی Main Thread تضمین
  نیست**: در تست، `navController.navigate(...)` که بعد از دو فراخوان suspend
  (`loadAllShotsForProject`/`loadScene`) اجرا می‌شد، با خطای واقعی
  `IllegalStateException: Method setCurrentState must be called on the main thread`
  شکست خورد — Continuation بعد از این دو Query گاهی روی Thread دیگری (نه
  Main) ادامه می‌یافت. رفع: بخش تصمیم/Navigate با
  `withContext(Dispatchers.Main.immediate) { ... }` صریحاً به Main Thread
  برگردانده شد — یک محافظت صریح و صحیح، نه فقط رفع مخصوص تست، چون Navigate
  ذاتاً یک عملیات محدود به Main Thread است.
- **بدون Loading Indicator**: `loadAllShotsForProject`/`loadScene` هر دو
  Query های محلی و کوچک روی Room هستند (توابع suspend تولیدشده‌ی Room که خودِ
  Room همیشه خارج از Main Thread اجرا می‌کند)، نه I/O سنگین — تأخیر محسوس
  کاربر انتظار نمی‌رود، پس Loading Indicator اضافه نشد.

## کامنت به‌روزرسانی‌شده (نه حذف‌شده)

کامنت موجود `NavDrawer.kt` (ارجاع به ADR-061) دست‌نخورده ماند و فقط یک پاراگراف
تازه به آن اضافه شد که این استثنای تک-شات و مسئولیت MainScaffold.kt را مستند
می‌کند.

## تست

دو تست End-to-End تازه در `NavDrawerNavigationTest.kt` (الگوی Seed مستقیم
Scene/Shot از `ValidationFlowTest.kt`):
- پرش مستقیم Validation با دقیقاً یک Shot در پروژه (+ تست مشابه برای
  OutputDelivery).
- حفظ Fallback به Tab «صحنه‌ها» با بیش‌از‌یک Shot در پروژه.

حالت «صفر Shot» را دو تست موجود قبلی («no shot context available») از قبل
پوشش می‌دادند و بدون تغییر سبز ماندند.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest --tests "...ui.navigation.*"` | ۳۸ تست، ۰ شکست |
| `gradle :app:testDebugUnitTest` (کل Suite) | موفق |
| `gradle :app:assembleDebug` | موفق |
