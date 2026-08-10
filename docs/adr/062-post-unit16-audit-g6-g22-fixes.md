# ADR-062: رفع G6 (باگ واقعی Bottom Nav) و G22 (قابلیت کاملاً غایب Delete) — ممیزی post-Unit16

## زمینه

`docs/audit/post-unit16-full-audit.md` این دو یافته را ثبت کرده بود — هر دو
🟡 دسته‌بندی شده‌اند (نه 🟠 — طبق بخش «اولویت‌بندی پیشنهادی» خودِ ممیزی،
هر دو در فهرست «باقی موارد 🟡 (G3–G6, G8, G12, G18, G22) — بدون فوریت»
بودند؛ صراحتاً برای شفافیت اینجا اصلاح می‌شود، چون توضیح این قدم آن‌ها را
بدون درجه‌بندی صریح معرفی کرده بود):

- **G6** — دکمه‌ی Studio نوار پایین همیشه به `PLACEHOLDER_ACTIVE_PROJECT_ID`
  (رشته‌ی ثابت جعلی) Navigate می‌کرد، مستقل از پروژه‌ی واقعی کاربر.
- **G22** — Delete Scene/Shot/Asset در سطح DAO/Rule دامنه کامل و
  تست‌شده بودند، اما هیچ‌کدام از UI اصلاً قابل‌دسترس نبودند.

## بازبینی مستقل پیش‌بررسی‌های دستور کار (طبق دستور صریح)

- `PLACEHOLDER_ACTIVE_PROJECT_ID` (`BottomNavBar.kt:41`، پیش از این قدم) —
  تأیید شد با grep، اما **مغایرت واقعی پیدا شد**: دستور کار فقط
  `BottomNavBar.kt:89` را نام برده بود، در حالی که این ثابت در ۴ محل دیگر
  هم استفاده می‌شد — `AssetsScreen.kt` و هر سه فرم Asset
  (`CharacterAssetFormScreen.kt`/`LocationAssetFormScreen.kt`/
  `ObjectAssetFormScreen.kt`)، همگی برای همان مفهوم «پروژه‌ی فعال». خودِ
  کامنت موجود `AssetsScreen.kt` (پیش از این قدم) صراحتاً می‌گفت این صفحه
  «دقیقاً همان محدودیت شناخته‌شده‌ی Studio را به ارث می‌برد» — یعنی نویسندگان
  قبلی خودشان این دو محل را یک مسئله‌ی واحد می‌دانستند. طبق دستور صریح
  («PLACEHOLDER_ACTIVE_PROJECT_ID را کاملاً حذف کن؛ هیچ رفتاری نباید به یک
  شناسه‌ی جعلی متکی بماند») **دامنه به هر ۵ محل گسترش یافت**، نه فقط
  Bottom Nav — این تصمیم مستقل اصلی این قدم است.
- `SceneDao.deleteScene`/`ShotDao.deleteShot`/`AssetDao.deleteAsset` —
  تأیید شد هر سه موجودند و از UI صدا زده نمی‌شدند.
- `domain/scene/SceneValidation.kt:60`ی `deleteScene(scene,
  existingShotsCount)` — تأیید شد؛ Rule واقعی **Blocking کامل** است
  («ابتدا شات‌ها را حذف کنید»)، نه فقط یک هشدار قوی‌تر — UI باید این را
  به‌عنوان یک خطای واقعی نمایش دهد، نه صرفاً یک تأیید دوباره.
- `domain/asset/AssetValidation.kt:53`ی `validateAssetDeletion(assetId,
  shotsUsingAsset)` — تأیید شد (Rule 3 واحد ۰۶). برای فراخوانی واقعی این
  Rule، «کدام Shot های این پروژه از این assetId استفاده می‌کنند» لازم بود —
  هیچ Query ای برای این از قبل وجود نداشت، پس اضافه شد
  (`ShotDao.getShotsForProject` + `ShotRepository.findShotIdsUsingAsset`).

هیچ مغایرت دیگری پیدا نشد.

## تصمیم ۱ — G6: `resolveActiveOrRecentProjectId` به‌جای PLACEHOLDER

تابع خالص تازه (`ui/navigation/ActiveProject.kt`):
```kotlin
fun resolveActiveOrRecentProjectId(workflowState: WorkflowState?, projectSummaries: List<ProjectSummary>): String? =
    workflowState?.projectId ?: projectSummaries.firstOrNull()?.project?.projectId
```
اگر یک Session واقعی Studio باز است، همان برنده است؛ در غیر این صورت،
پروژه‌ای که آخرین‌بار تغییر کرده (`projectSummaries` از قبل توسط
`ProjectDao.getAllProjectsWithCounts` روی `lastModified DESC` مرتب
می‌شود)؛ اگر اصلاً هیچ پروژه‌ای وجود ندارد، `null`.

**تصمیم فرعی — یکسان‌سازی Fallback Drawer (G1) با این تابع:** قدم قبلی
(G1، ADR-061) وقتی `WorkflowState.projectId == null` بود بی‌واسطه به
`Projects` می‌رفت. حالا که مفهوم «آخرین پروژه» واقعی ساخته شده،
`MainScaffold.kt`ی Drawer هم به همین تابع migrate شد — یعنی بدون Session
فعال، Drawer هم دیگر بی‌واسطه به Projects نمی‌رود، بلکه اول آخرین پروژه
را امتحان می‌کند. این یک ناهماهنگی رفتاری بین Bottom Nav و Drawer برای
دقیقاً همان وضعیت را حذف می‌کند — تصمیم مستقل این قدم، نه بخشی از دستور
کار صریح.

**تصمیم فرعی — Fallback واقعی برای Assets وقتی هیچ پروژه‌ای نیست:**
`AssetsScreen`ی سطح‌بالا اکنون `projectId: String?` می‌گیرد (نه دیگر
PLACEHOLDER)؛ اگر `null` (فقط وقتی اصلاً هیچ پروژه‌ای در کل اپ نیست)،
یک محتوای جایگزین با دکمه‌ی «برو به پروژه‌ها» نشان می‌دهد، به‌جای ساخت
`AssetLibraryViewModel` با یک شناسه‌ی جعلی. `AssetForm` (که فقط از داخل
همین شاخه‌ی غیر-null قابل‌دسترس است) یک محافظ دفاعی مشابه دارد
(`LaunchedEffect` که در عمل هرگز اجرا نمی‌شود، طبق تأیید grep).

## تصمیم ۲ — G22 بخش الف: Delete Scene، Rule واقعی Blocking

`SceneRepository.deleteScene(scene, existingShotsCount)` لایه‌ی نازک I/O
روی `domain.scene.deleteScene` موجود است. UI: منوی سه‌نقطه‌ی هدر Scene
Detail (مستقل از Tab انتخاب‌شده) + `AlertDialog` تأیید (هم‌الگو دقیق با
`DeleteBackupDialog`/`ArchiveProjectDialog`، ADR-060). اگر صحنه هنوز
Shot دارد، پیام واقعی Exception (نه یک متن جعلی) از طریق Snackbar موجود
نمایش داده می‌شود و صفحه عوض نمی‌شود.

## تصمیم ۳ — G22 بخش ب: Delete Shot، بدون Rule (طبق طراحی)

`ShotRepository.deleteShot(shotId)` — بدون Validation، چون هیچ بلوپرینتی
(۰۴/۰۶) Rule ای برای حذف Shot تعریف نکرده. UI: منوی سه‌نقطه‌ی هر کارت
Shot (نه Scene) + همان الگوی AlertDialog.

## تصمیم ۴ — G22 بخش ج: Delete Asset، Rule واقعی Blocking سطح-پروژه

بر خلاف Scene (که Rule اش فقط به همان Scene نیاز دارد)، Rule حذف Asset
(`validateAssetDeletion`) به «همه‌ی Shot های این پروژه» نیاز دارد — Asset
یک مفهوم سطح-پروژه است، نه سطح-صحنه. `ShotDao.getShotsForProject`
(Query تازه، Join با scenes روی projectId) + `ShotRepository.
findShotIdsUsingAsset(projectId, assetId)` (فیلتر بعد از Deserialize،
چون `shotDataJson` یک Blob است، نه ستون‌های جدا) اضافه شدند.

**تصمیم فرعی — Rule در ViewModel اجرا می‌شود، نه در `AssetRepository`:**
`AssetRepository` عمداً به `ShotRepository` وابسته نشد (هم‌الگو با بقیه‌ی
Repository های این پروژه — هرکدام فقط یک DAO). `AssetLibraryViewModel`
اکنون هر دو Repository را دارد و خودش Rule را قبل از فراخوانی
`repository.deleteAsset` اجرا می‌کند — دقیقاً هم‌الگو با `DnaViewModel`ی
`dependentShotsCount` (ADR-054، که همان تصمیم را قبلاً برای یک سناریوی
مشابه گرفته بود).

## تست‌ها

- `ActiveProjectTest.kt` (تازه، JUnit خالص بدون Compose/Robolectric):
  ۳ تست روی خودِ `resolveActiveOrRecentProjectId`.
- `BottomNavStudioResolutionTest.kt` (تازه): بدون هیچ پروژه‌ای → Projects
  + پیام؛ با ۲ پروژه (بدون Session فعال) → آخرین پروژه‌ی واقعی باز می‌شود
  (اثبات با `STUDIO_TITLE_TAG` تازه‌اضافه‌شده به `StudioHeader`، نه
  `hasText` که با ابهام واقعی روبه‌رو شد — پایین توضیح داده شده).
- `AppNavigationTest.kt`: `setUp()` اکنون یک پروژه‌ی واقعی می‌سازد؛ ۳ تست
  موجود که Studio نوار پایین را کلیک می‌کنند یک `waitForProjectListToLoad()`
  تازه گرفتند.
- `ShotsFlowTest.kt`: ۲ تست تازه (حذف Shot با Cancel/Confirm؛ حذف Scene
  دارای Shot مسدود می‌شود، بعد از حذف Shot موفق می‌شود).
- `AssetsScreenFlowTest.kt`: ۲ تست تازه (حذف Asset در حال استفاده مسدود
  می‌شود با پیام واقعی؛ حذف Asset بدون استفاده با Cancel/Confirm).

## یافته‌های واقعی دیباگ (Race Condition مستندشده، ADR-044)

سه یافته‌ی مجزا، همگی از همان دسته‌ی از‌پیش‌مستندشده‌ی ADR-044
(«`projectSummaries` از یک Flow واقعی Room می‌آید که روی Executor داخلی
خودش، نه Dispatcher تزریق‌شده، دوباره Query می‌شود»):

1. `BottomNavStudioResolutionTest`ی تست «آخرین پروژه» — بدون یک انتظار
   صریح قبل از کلیک Studio، تصمیم Navigation گاهی روی یک `projectSummaries`
   هنوز-خالی گرفته می‌شد. رفع: باز کردن Projects و دیدن هر دو نام پروژه
   قبل از کلیک Studio.
2. همان یافته دقیقاً در `AppNavigationTest`ی ۳ تست موجود (که این قدم
   `setUp` شان را برای G6 عوض کرد) — تحت بار کامل Suite واقعاً رخ داد
   (شکست واقعی مشاهده‌شده). رفع: `waitForProjectListToLoad()`.
3. `hasText("Newer Project", substring=true)` در ابتدا **دقیقاً ۲ گره**
   پیدا می‌کرد (نه ۱) با اینکه Navigate واقعاً درست بود — خودِ assertion
   Ambiguous بود، نه رفتار Production. رفع با افزودن `STUDIO_TITLE_TAG`
   (testTag صریح روی عنوان `StudioHeader`) به‌جای تکیه به متن.

## یافته‌ی محیطی تأییدشده (Flake از‌پیش‌مستند، نه باگ این قدم)

`ShotsFlowTest > deleting a scene that still has a shot is blocked...`
در اجراهای مکرر Isolated **در ۴ نقطه‌ی کاملاً متفاوت و نامرتبط** شکست
خورد (خط ۴۵۳، سپس داخل `createProjectAndOpenShotsTab` در خط ۴۳۱، دوباره
۴۵۳، سپس ۴۵۸) و یک‌بار هم کاملاً موفق بود — امضای کلاسیک ناپایداری
زمان‌بندی محیط (نه یک باگ قطعی منطقی، که همیشه در همان نقطه شکست
می‌خورد). یک تست خواهر کاملاً بی‌ربط در همان فایل
(`selecting each camera movement kind...`) هم در همان اجراها با
`SQLiteConnectionPool` (همان Flake از‌پیش‌مستندشده‌ی ADR-059/ADR-060)
شکست خورد — تأیید مستقل که این محیط، نه این تست، ناپایدار بود. یک
`waitForIdle()` اضافه شد (بین تلاش ناموفق حذف Scene و تلاش بعدی حذف
Shot) به‌عنوان یک بهبود منطقی مستقل از خودِ Flake، اما این Flake را کامل
حذف نکرد.

## نتیجه‌ی Build

`gradle :app:testDebugUnitTest :app:assembleDebug` → **۶۸۱ تست (۶۷۲→۶۸۱،
۹ تست جدید)، ۶۸۰ موفق.** یک شکست
(`ShotsFlowTest > deleting a scene that still has a shot...`) — طبق
یافته‌ی بالا، Flake محیطی تأییدشده، بدون ارتباط قطعی با کد این قدم. APK
واقعی `assembleDebug` هم به‌طور جداگانه با موفقیت ساخته شد (چون شکست
تست، اجرای `assembleDebug` را در همان فرمان متوقف کرده بود).

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
