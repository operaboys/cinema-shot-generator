# ADR-059: واحد ۱۶ فاز ۶ — قدم ۲ (آخرین قدم کل واحد ۱۶) — صفحه‌ی Backups + بازبینی یکپارچه‌ی نهایی

## زمینه

آخرین قدم کل واحد ۱۶. منبع حقیقت دوگانه: `docs/design/README.md` بخش
«۱۲. Backups» و `docs/blueprints/15-project-storage.md` بخش «Auto-Save و
Backup». پیش‌بررسی معمار (تأییدشده با خواندن مستقیم `BackupManager.kt`):
`createBackup`/`restoreFromBackup` از واحد ۱۵ کامل‌اند، اما یک تناقض واقعی
بین دو منبع حقیقت وجود دارد — جزئیات در تصمیم ۱.

## تصمیم ۱: پسوند فایل — تغییر به `.csgb` (نه صرفاً نمایشی)

**تناقض واقعی**: `docs/design/README.md` صراحتاً `project-slug-YYYY-MM-DD.csgb`
را نشان می‌دهد؛ اما کد مفهومی خودِ `docs/blueprints/15-project-storage.md`
(`"backup_${projectId}_${backupId}.json"`) و پیاده‌سازی واقعی هر دو `.json`
بودند.

طبق ترجیح صریح معمار («هماهنگی واقعی نام فایل با طراحی، مگر دلیل فنی قوی
برای `.json`») و طبق سیاست کلی این پروژه (بدون نسخه‌ی منتشرشده، Migration
بدون ریسک Backward-Compat)، پسوند واقعیِ فایل ذخیره‌شده روی دیسک از `.json`
به `.csgb` تغییر کرد — نه فقط یک نام نمایشی گمراه‌کننده. محتوای فایل همچنان
متن JSON خام است (طبق `serializeFullProject`/`deserializeFullProject`، بدون
تغییر) — فقط پسوند، نه فرمت داخلی، عوض شد؛ این یک الگوی رایج و بی‌خطر است
(مثل `.docx` که خودش zip است).

## تصمیم ۲: الگوی نام فایل ذخیره‌شده دست‌نخورده ماند؛ نام «نمایشی» جدا ساخته شد

سند طراحی الگوی `project-slug-YYYY-MM-DD` را نشان می‌دهد، اما پیاده‌سازی
واقعی از یک الگوی عمداً امن-در-برابر-تصادم استفاده می‌کند
(`backup_{طول-projectId}_{projectId}_...`، طبق تصمیم مستندشده‌ی ADR-023 —
رگرسیون واقعی کشف‌شده در همان قدم: پروژه‌ی «p1» می‌توانست بک‌آپ‌های
«p1_v2» را هم ببیند/پاک کند). تغییر این الگو به slug+date خام، دقیقاً همان
باگ را دوباره باز می‌کرد (دو پروژه‌ی هم‌نام یا هم‌روزه به‌راحتی تصادم
می‌کنند). تصمیم: الگوی ذخیره‌سازی واقعی (امن) دست‌نخورده ماند؛ صفحه‌ی
Backups یک نام «نمایشی» جداگانه می‌سازد (`displayBackupFileName` در
`BackupLabels.kt`) که دقیقاً الگوی سند طراحی را برای کاربر نشان می‌دهد،
بدون اینکه امنیت تصادم واقعی فایل زیرین را قربانی کند.

## تصمیم ۳: تمایز auto/manual — واقعاً اضافه شد (قبلاً اصلاً وجود نداشت)

طبق یافته‌ی صریح این قدم (grep تأییدشده): نه `BackupManager` و نه
`BackupFileInfo` هیچ تمایز auto/manual ای نداشتند. اضافه شد:
`enum class BackupKind { AUTO, MANUAL }`، به‌عنوان یک Segment در نام فایل
ذخیره‌شده (بعد از Prefix امن، نه قبل از آن — تا `backupPrefix()` موجود
دست‌نخورده بماند)، و `BackupSummary` (نوع تازه‌ی UI-پسند) که این را همراه
حجم/سن/شناسه برمی‌گرداند. `hjم` هم به همین مناسبت به `BackupFileInfo`
اضافه شد (`sizeBytes: Long`، از قبل وجود نداشت).

## تصمیم ۴: Backups per-project است؛ Scope آن = Session فعال Studio

`BackupManager` سازنده‌اش `projectId` می‌خواهد (per-project، نه سراسری مثل
Settings). چون مسیر `Backups` در Nav Drawer (طبق سند طراحی، هم‌ردیف
Settings) هیچ آرگومانی ندارد، این صفحه از `WorkflowState.projectId`
(Session فعال Studio، همان الگوی `PLACEHOLDER_ACTIVE_PROJECT_ID` که
ADR-048 برای Assets استفاده کرد) می‌خواند. اگر هیچ Session فعالی نباشد
(کاربر هرگز وارد یک پروژه نشده)، یک پیام واضح («ابتدا یک پروژه را در
Studio باز کنید») نشان داده می‌شود، نه خطا یا صفحه‌ی خالی گمراه‌کننده.

## تصمیم ۵: `BackupManager` در سطح مصرف‌کننده ساخته می‌شود، نه یک نمونه‌ی مشترک App.kt

برخلاف بقیه‌ی Repository های این اپ (که یک‌بار در `App.kt` ساخته و پایین
تزریق می‌شوند)، `BackupManager` به دلیل ساختار سازنده‌اش (per-project)
نمی‌تواند همان الگو را دنبال کند. فقط لایه‌ی I/O مشترک
(`BackupFileStorage`) یک‌بار در `App.kt` ساخته می‌شود؛ خودِ `BackupManager`
هم در `StudioShell` (برای Timer دوره‌ای Auto-Backup) و هم در
`BackupsViewModel` (برای صفحه‌ی Backups) به‌طور مستقل، با همان
`backupFileStorage` تزریقی، ساخته می‌شود.

## تصمیم ۶: Auto-Backup دوره‌ای واقعی وصل شد (`backupIntervalMinutes`)

یافته‌ی صریح بازبینی نهایی (بخش ب-۶ دستور کار این قدم، grep تأییدشده):
`backupIntervalMinutes` از واحد ۱۵ تا این لحظه هرگز مصرف نشده بود — دقیقاً
همان شکاف کشف‌شده‌ی `AutoSaveManager.intervalSeconds` در قدم قبل (ADR-058)،
این‌بار برای Backup. رفع هم‌الگو: یک `LaunchedEffect` دوم در `StudioShell`
(کنار Timer موجود Auto-Save)، هر `backupIntervalMinutes` یک `createBackup(
BackupKind.AUTO)` واقعی می‌سازد — کاملاً متمایز از بکاپ‌های دستی صفحه‌ی
Backups (`BackupKind.MANUAL`).

---

## بازبینی یکپارچه‌ی نهایی (بخش ب دستور کار)

### ۱. Nav Drawer Scroll

تأیید شد: رفع قدم قبل (ADR-058) برای هر ۱۱ آیتم (۳ گروه) کار می‌کند —
تست تازه (`BackupsFlowTest`) با `performScrollTo` صریح روی «بکاپ‌ها»
(آخرین آیتم آخرین گروه) موفق است.

### ۲. Back Navigation Contextual — یافته‌ی واقعی، رفع شد

`Validation`/`OutputDelivery` (فاز ۵) هرگز به `when` صریح `MainScaffold.kt`
اضافه نشده بودند — دکمه‌ی برگشت درون‌خودِ صفحه درست به `ShotComposer`
برمی‌گشت، اما دکمه‌ی سخت‌افزاری Back (BackHandler) به‌جایش به Home می‌رفت
(چون این دو مسیر در `backTargetsByRouteKey` هم نبودند). یک ناهماهنگی واقعی
بین دو مسیر برگشت همان صفحه — رفع شد (هر دو مسیر اکنون به `ShotComposer`
برمی‌گردند). `backTargetsByRouteKey` (طراحی اصلی فاز ۰) در عمل هرگز استفاده
نشد — همه‌ی قوانین Runtime-Argument-Dependent به‌جایش مستقیماً در `when`
صریح `MainScaffold.kt` اضافه شدند؛ محدودیت شناخته‌شده، نه یک باگ (خودِ
Map برای مسیرهای بدون‌آرگومان طراحی شده بود، که هیچ‌کدام از مسیرهای واقعی
این اپ نبودند).

### ۳. Toast/Snackbar — یافته‌ی واقعی، تا حد ممکن رفع شد

یک مکانیزم واحد در کل اپ (`MainScaffold`، grep تأییدشده) — سازگاری خوب،
اما شکل پیش‌فرض Material3 (مستطیل‌گوشه‌گرد) با «bottom-anchored pill»ی
سند طراحی می‌خواند، رفع شد. مدت‌زمان دقیق «~۲ ثانیه» رفع **نشد** —
`SnackbarDuration` استاندارد فقط سه مقدار گسسته (Short/Long/Indefinite)
دارد، نه میلی‌ثانیه‌ی دلخواه؛ Short (از قبل استفاده‌شده) نزدیک‌ترین گزینه
است — محدودیت شناخته‌شده، مستند شده.

### ۴. EntityState (۵ حالت) — تأیید شد، Contrast رفع شد

هر ۵ حالت (Draft/Review/Locked/Final/Archived) واقعاً از فاز ۱ در
`ProjectCard.kt` پیاده‌سازی و رنگ/برچسب‌بندی شده بودند — کامل. اما همان
چیپ (و کپی مشابهش در `ScenesListScreen.EntityStateChip`) باگ Contrast
واقعی داشت — بخش ۵ زیر.

### ۵. Contrast — پنج باگ واقعی پیدا و رفع شد

grep سراسری روی `.copy(alpha` پنج نمونه‌ی واقعی از همان کلاس باگ
Low-opacity Tinted Fill (که ADR-055 قبلاً برای Validation مستند کرده بود)
پیدا کرد که هرگز رفع نشده بودند — همه با پس‌زمینه‌ی Solid + حاشیه‌ی ۲dp
(+ متن سیاه ثابت روی رنگ‌های روشن، هم‌الگو با ADR-055) رفع شدند:

1. `ProjectCard.kt` — چیپ EntityState (`.copy(alpha = 0.16f)`).
2. `ScenesListScreen.kt` — `EntityStateChip` (کپی از بالا، همان باگ).
3. `DnaTabContent.kt` — `SoftLockBanner` (حاشیه داشت، اما پرشده‌اش فقط
   ۱۴٪ Alpha بود).
4. `HomeScreen.kt` — پیل برند «Cinema Studio» روی پس‌زمینه‌ی Blur/Hero
   خودِ Home — دقیقاً همان سناریوی صریح هشدارداده‌شده در Implementation
   Notes سند طراحی.
5. `StudioShell.kt` — چیپ «ذخیره شد».

### ۶. AutoSave/BackupManager — هماهنگ شد

`backupIntervalMinutes` وصل شد (تصمیم ۶ بالا).

---

## تصمیم ۷: باگ واقعی DI کشف و رفع شد — `BackupManager` دیگر `AppDatabase.getInstance` را مستقیم صدا نمی‌زند

**یافته‌ی واقعی (نه صرفاً محدودیت تست):** بعد از رفع اولیه‌ی سه تست شکست‌خورده‌ی
`BackupsFlowTest` با `.performScrollTo().clickViaSemantics()` (همان الگوی
تکرارشونده‌ی این Session)، هر سه تست باز هم با `ComposeTimeoutException`
شکست خوردند — این‌بار سر انتظار برای نشان «دستی» بعد از کلیک دکمه‌ی
«ساخت بکاپ دستی». بررسی ریشه‌ای (نه فقط تکرار الگوی قبلی) نشان داد این
یک باگ متفاوت است: `BackupsViewModel`/`StudioShell` هر دو مستقیماً
`AppDatabase.getInstance(application)` را صدا می‌زدند — برخلاف الگوی
تزریق سراسری این پروژه که `App.kt` یک `database` واحد می‌سازد و پایین
تزریق می‌کند (`autoSaveManager`/`backupFileStorage` دقیقاً همین‌طور).
در `BackupsFlowTest`، پروژه‌ی واقعاً ساخته‌شده در `database` تزریقی تست
(`Room.inMemoryDatabaseBuilder` جدا، برای `ProjectListViewModel`) در آن
Singleton سراسری دیگر (که `BackupManager` DAO هایش را از آن می‌گرفت)
هرگز وجود نداشت — پس `createBackup` همیشه با `serializeFullProject`
شکست‌خورده (پروژه یافت نشد) بی‌صدا Result.failure برمی‌گرداند و فهرست
هرگز به‌روز نمی‌شد.

**مهم:** این باگ رفتار واقعی اپ در Production را تغییر نمی‌دهد —
`AppDatabase.getInstance()` در مسیر واقعی همیشه همان یک Singleton سراسری
است، هرجا صدا زده شود. این یک ناهماهنگی خالص در الگوی DI بود (تنها
استثنای این الگو در کل کدبیس) که به‌طور تصادفی توسط تست‌های End-to-End
همین قدم آشکار شد — دقیقاً همان نوع باگی که Session های قبلی هم به‌خاطر
تست واقعی (نه Mock کامل) کشف کرده بودند.

**رفع:** `database: AppDatabase? = null` به‌عنوان پارامتر تزریقی جدید
(هم‌الگو دقیق با `autoSaveManager`/`backupFileStorage`) در زنجیره‌ی کامل
اضافه شد: `BackupsViewModel`(+`factory`) → `BackupsScreen` → `AppNavHost`
→ `MainScaffold` → `App.kt` (مقدار واقعی `database` موجود پاس داده شد)
و مستقیماً در `StudioShell` (برای `backupManager` Timer). فقط وقتی
`null` باشد (مسیر واقعی اپ) به `AppDatabase.getInstance(application)`
برمی‌گردد؛ `BackupsFlowTest` اکنون `database` تست خودش را صریح پاس
می‌دهد. بعد از این رفع، هر ۴ تست `BackupsFlowTest` و هر دو تست تازه‌ی
`BackupManagerTest` (پایین) موفق شدند.

---

## نتیجه‌ی Build

`gradle :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL`،
۶۵۵ تست (۶۴۹→۶۵۵، ۶ تست جدید: ۴ در `BackupsFlowTest`، ۲ در
`BackupManagerTest`)، ۰ Failure، ۰ Error، ۰ Skipped (شمارش دقیق از جمع
`tests`/`failures`/`errors`/`skipped` در تمام ۹۳ فایل JUnit XML). APK
واقعی `assembleDebug` هم ساخته شد.

## تست‌ها (`BackupsFlowTest.kt`)

- Scroll واقعی Drawer روی صفحه‌ی کوچک → «بکاپ‌ها» رسیدنی است.
- ساخت بکاپ دستی → در فهرست با نشان Manual ظاهر می‌شود.
- بازیابی از بکاپ → داده‌ی واقعی پروژه جایگزین می‌شود.
- حذف بکاپ → از فهرست و از حافظه‌ی واقعی حذف می‌شود.

به‌علاوه ۲ تست تازه در `BackupManagerTest.kt` (`listBackups`/`deleteBackup`).

## محدودیت شناخته‌شده‌ی تازه (این قدم)

Restore/Delete در صفحه‌ی Backups بدون دیالوگ تأیید («آیا مطمئنید؟») هستند —
هر دو عملیات مخرب‌اند (Restore داده‌ی جاری را جایگزین می‌کند، Delete
غیرقابل‌بازگشت است)؛ سند طراحی چنین دیالوگی را الزام نکرده (فقط دکمه‌ی
Restore/Delete روی هر ردیف)، پس این تصمیم Scope رعایت شد، اما به‌عنوان
یک شکاف UX واقعی مستند می‌شود.

## Skills استفاده‌شده

هیچ Skill ای در این قدم فراخوانی نشد.
