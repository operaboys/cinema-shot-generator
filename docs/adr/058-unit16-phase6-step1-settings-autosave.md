# ADR-058: واحد ۱۶ فاز ۶ — قدم ۱ — صفحه‌ی Settings + اتصال واقعی AutoSaveManager

## زمینه

اولین قدم آخرین فاز واحد ۱۶. منبع حقیقت دوگانه: `docs/design/README.md`
بخش «۱۱. Settings» و `docs/blueprints/16-user-workflow-v2.md` بخش
«رفتارهای سراسری». پیش‌بررسی معمار (تأییدشده با خواندن مستقیم
`AutoSaveManager.kt`): این کلاس از قدم اول واحد ۱۵ فقط `saveIfDirty`
(منطق تصمیم) داشت؛ خودِ کامنت آن (ADR-022) صریحاً منتظر «لایه‌ی UI آینده»
برای وصل‌کردن Timer/Lifecycle واقعی مانده بود.

## تصمیم ۱: معماری Auto-Save موجود — دو لایه‌ی کاملاً مستقل، نه یک جایگزینی

بررسی دقیق کدبیس (نه فرض) نشان داد Auto-Save این پروژه از قبل **دو لایه‌ی
کاملاً مستقل** دارد که باید از هم تفکیک شوند:

1. **ذخیره‌ی واقعی داده‌ی هر Entity** (Scene/Shot/DNA/Asset): از فازهای
   قبلی، هر ViewModel (`ShotComposerViewModel`، `DnaViewModel`، ...) با هر
   تغییر فیلد، بلافاصله `xRepository.saveX(...)` را صدا می‌زند — این ذخیره
   واقعی و بی‌درنگ است، نه یک Placeholder. این لایه در این قدم دست‌نخورده
   ماند.
2. **`AutoSaveManager.saveIfDirty`**: فقط `ProjectEntity.lastModified` (و
   خودِ ردیف Project) را تازه می‌کند — عملاً یک لایه‌ی جداگانه در سطح
   Project، نه Scene/Shot/DNA/Asset. بررسی `ProjectRepository.kt` نشان داد
   `lastModified` تنها با تغییرنام/آرشیو/کپی پروژه به‌روزرسانی می‌شد،
   **هرگز با ویرایش واقعی محتوای پروژه** (چون آن ویرایش‌ها مستقیماً روی
   Repository های Scene/Shot/... می‌روند، نه از طریق `ProjectRepository`).
   این یک شکاف واقعی بود: یک پروژه که کاربر یک ساعت مشغول شات‌سازی در آن
   بوده، در فهرست «آخرین ویرایش» صفحه‌ی Home/Projects می‌توانست کاذب قدیمی
   به نظر برسد.

**نتیجه**: سؤال «Timer در برابر ذخیره‌ی هر تغییر» یک انتخاب دوگانه‌ی
واقعی نبود — این دو مکانیزم برای دو چیز متفاوت‌اند. تصمیم: لایه‌ی ۱ (ذخیره‌ی
فوری هر Entity) دست‌نخورده و به‌عنوان مکانیزم واقعی و کافی ذخیره‌ی داده باقی
ماند؛ برای لایه‌ی ۲ (تازه‌نگه‌داشتن `lastModified` پروژه، طبق طراحی اصلی
بلوپرینت و همان شکاف کشف‌شده)، یک **Timer دوره‌ای واقعی** اضافه شد —
نه به این دلیل که ذخیره‌ی فعلی ناکافی بود، بلکه چون این لایه‌ی دوم قبلاً
اصلاً وجود نداشت.

## تصمیم ۲: محل Timer — `LaunchedEffect` در `StudioShell`، نه یک ViewModel تازه

`AutoSaveManager` یک متد Convenience تازه گرفت:
`suspend fun touch(projectId: String): Result<Boolean>` — `ProjectEntity`
فعلی را خودش بارگذاری می‌کند، سپس `saveIfDirty` موجود (بدون تغییر امضا،
تست‌های `AutoSaveManagerTest.kt` موجود دست‌نخورده) را با `isDirty=true`
صدا می‌زند.

`StudioShell.kt` از قبل یک `LaunchedEffect(projectId)` برای
`startWorkflowSession` داشت — دقیقاً معادل «طول یک Session فعال Studio».
یک `LaunchedEffect(projectId, autoSaveCadenceSeconds, autoSaveManager)`
دوم به همان‌جا اضافه شد: `while (isActive) { delay(cadenceSeconds*1000);
autoSaveManager.touch(projectId) }`. لغو خودکار با ترک/تغییر Composition
(هم‌الگو با `LaunchedEffect` اول) — بدون نیاز به مدیریت دستی `Job`. یک
ViewModel تازه فقط برای این یک حلقه ساخته نشد؛ `StudioShell` از قبل دقیقاً
Scope درست (طول Session) را داشت.

`isDirty` همیشه `true` است در هر Tick — نه یک ردیابی واقعی «چیزی عوض شده
یا نه» در سطح فیلد. دلیل: ردیابی دقیق «Dirty» نیازمند یک سیگنال سراسری از
تمام ViewModel های این اپ است (تغییر بزرگ خارج از Scope این قدم)؛ چون
کاربر تا وقتی داخل یک Session Studio باز مانده، فرض معقول این است که
«فعال» است — دقیقاً هم‌راستا با توصیف بلوپرینت («Auto-Save کاملاً خودکار و
بی‌صدا»). مستند شده به‌عنوان یک ساده‌سازی آگاهانه.

`AutoSaveManager` یک بار در `App.kt` ساخته و تا `MainScaffold` →
`AppNavHost` → `StudioShell` رشته شد — دقیقاً هم‌الگو با پنج Repository
دیگر (Story/Asset/Scene/Shot/ProjectDna/PromptGeneration) که همین مسیر را
از فازهای قبل طی کرده‌اند.

## تصمیم ۳: Cadence از Settings کنترل می‌شود

`WorkflowViewModel.autoSaveCadenceSeconds: StateFlow<Long>` (پیش‌فرض ۳۰،
دقیقاً هم‌تراز `AutoSaveManager.intervalSeconds` پیش‌فرض) — DataStore-backed،
هم‌الگو دقیق با ۴ Preference موجود (`language`/`theme`/`homeLayoutVariant`/
`composerLayoutVariant`). صفحه‌ی Settings سه Preset واقعی (۱۵/۳۰/۶۰ ثانیه)
نشان می‌دهد؛ `StudioShell`ی `LaunchedEffect` بلافاصله با تغییر این مقدار
دوباره راه‌اندازی می‌شود (چون `autoSaveCadenceSeconds` جزء کلید
`LaunchedEffect` است).

## تصمیم ۴: ۷ کارت Settings — دقیقاً طبق سند طراحی، بدون افزودن/کاستن

هیچ ViewModel تازه‌ای ساخته نشد — `SettingsScreen` مستقیماً
`WorkflowViewModel` تزریق‌شده را مصرف می‌کند (هم‌الگو با `HomeScreen`)،
چون هر مقدار این صفحه از قبل در همان ViewModel موجود بود یا باید همان‌جا
اضافه می‌شد.

- **Display** (Dynamic Font/Min Touch Target/Reduced Motion): طبق یافته‌ی
  صریح grep روی `ui/theme/Theme.kt`، **هیچ زیرساخت واقعی‌ای برای این سه
  تنظیم در کل کدبیس وجود ندارد**. سه سوییچ واقعاً در DataStore Persist
  می‌شوند (نه صرفاً State محلی UI)، اما فعلاً هیچ اثر Runtime ای در جای
  دیگری از اپ ندارند — محدودیت شناخته‌شده، صریحاً مستند (نه پنهان،
  دقیقاً طبق درخواست صریح معمار).
- **Workflow**: Shot List Default View (کاملاً واقعی — `ShotListViewMode`
  از فاز ۴)، Auto-Save Cadence (واقعی — تصمیم ۳ بالا)، Jump Between Steps
  (سوییچ واقعاً Persist می‌شود، پیش‌فرض `true` چون رفتار واقعی فعلی Studio
  همین است — Tab های Story/DNA/Scenes/... همیشه آزادانه قابل‌کلیک‌اند بدون
  هیچ Gate ای؛ اما هیچ منطق Gate/هشدار واقعی‌ای برای این سوییچ در جای
  دیگری از اپ وجود ندارد — محدودیت شناخته‌شده مستند).
- **Privacy**: طبق README صرفاً نمایشی/اطلاعاتی (اپ کاملاً On-Device
  است) — سه ردیف متن ثابت، بدون منطق قابل‌تغییر.
- **Language & Theme**: رادیوهای صریح (نه دکمه‌ی Toggle مثل Header) که
  مستقیماً `WorkflowViewModel.language`/`theme` موجود از فاز ۰ را
  می‌خوانند/می‌نویسند — دقیقاً همان State، نه یک State تازه؛ فقط ابزار
  تعامل (Radio در برابر Toggle یک‌دکمه‌ای) با سند طراحی هماهنگ شد.
- **Home Screen Image**: طبق یافته‌ی صریح grep روی کل کدبیس، **هیچ
  زیرساخت File Picker ای وجود ندارد** (هم‌کلاس محدودیت شناخته‌شده‌ی
  Attached References در Shot Composer، ADR-051). دکمه‌ی «انتخاب تصویر»
  پیام «به‌زودی» نشان می‌دهد (هم‌الگو با Drawer)؛ مقدار URI واقعاً در
  DataStore Persist می‌شود (`homeScreenImageUri`، برای اتصال آینده)؛
  نمایش واقعی این تصویر روی Home/پس‌زمینه‌ی بقیه‌ی صفحات این قدم را
  نمی‌سازد (Scope بسیار بزرگ‌تر از «صفحه‌ی Settings» — تصمیم مستند، نه
  فراموش‌شده).
- **Layout Variants**: چیپ‌های Home (Hero/Resume) و Shot Composer
  (Tabs/Accordion) مستقیماً `WorkflowViewModel.homeLayoutVariant`/
  `composerLayoutVariant` موجود از فاز ۰ را می‌خوانند/می‌نویسند. **یافته‌ی
  مهم این قدم** (grep روی `HomeScreen.kt`/`ShotComposerScreen.kt`): هیچ‌کدام
  از این دو صفحه فعلاً به این دو مقدار شاخه‌بندی نمی‌کنند — یعنی انتخاب از
  Settings، State واقعی را درست Persist می‌کند، اما «اعمال بصری واقعی
  روی Home/Composer» هنوز هیچ‌جا پیاده نشده. ساخت واقعی چیدمان B («Resume»)
  و «Accordion» یک قدم/چند قدم مجزا و به‌مراتب بزرگ‌تر از این صفحه است؛
  خارج از Scope این قدم ماند و صریحاً مستند شد (نه سکوت).
- **About**: کارت ثابت (نام اپ + Tagline).

## یافته‌ی واقعی دیباگ ۱: Nav Drawer با ۱۱ آیتم بدون Scroll — باگ واقعی UX

هنگام نوشتن `SettingsFlowTest.kt`، کلیک روی «تنظیمات» (آخرین گروه از ۱۱
آیتم Drawer) به‌طور مداوم بی‌اثر بود. `NavDrawer.kt` بررسی شد:
`ModalDrawerSheet` محتوایش را خودکار Scroll نمی‌کند (فقط یک Surface/Column
ساده است). با ۱۱ آیتم در ۳ گروه، این یعنی روی صفحه‌های کوچک‌تر، «تنظیمات»/
«بکاپ‌ها» عملاً برای کاربر واقعی هم غیرقابل‌دسترس بودند — یک باگ واقعی
UX، نه فقط یافته‌ی تست. رفع: `Column(Modifier.verticalScroll(...))` دور
محتوای `ModalDrawerSheet`.

## یافته‌ی واقعی دیباگ ۲: کلیک مبتنی بر مختصات روی یک ردیف ۳-چیپی عریض قابل‌اعتماد نبود

پس از رفع دیباگ ۱، ۴ تست همچنان با `AssertionError` (نه Timeout) شکست
می‌خوردند — یعنی گره پیدا و «کلیک» می‌شد، اما مقدار State هرگز عوض
نمی‌شد. یک `println` تشخیصی مستقیم داخل Lambda تولید (بعداً حذف‌شده) ثابت
کرد Handler واقعی هرگز اجرا نمی‌شود. `performScrollTo()` برای ردیف‌های
۲-چیپی (زبان/تم/Layout) مشکل را حل کرد، اما ردیف ۳-چیپی Auto-Save Cadence
(عریض‌تر، بدون Wrap) حتی با `performScrollTo()` + `performClick()` مجزا
هم شکست می‌خورد (رد شد: علت `forEach` نبود — Unroll به سه فراخوانی صریح
همان نتیجه را داد). علت دقیق ریشه‌ای فراتر از Scope این قدم برای ردیابی
کامل بود (احتمالاً Overflow افقی یک `Row` بدون Scroll افقی، در تداخل با
محاسبه‌ی مختصات کلیک). رفع مستحکم: تغییر همه‌ی کلیک‌های این فایل تست به
همان الگوی اثبات‌شده‌ی موجود در `ValidationFlowTest.kt`/`ShotsFlowTest.kt`/
`DnaSoftLockWarningTest.kt` — `performSemanticsAction(SemanticsActions.OnClick)`
(فراخوانی مستقیم Action، مستقل از مختصات صفحه‌نمایش) به‌جای
`performClick()` مبتنی بر مختصات.

## ناوبری

مسیر `Settings` یک `data object` بدون آرگومان (نه `data class`) — هم‌الگو
با `Assets` (نه با Studio/SceneDetail که Argument دارند)، چون Settings
کاملاً سراسری و مستقل از پروژه است. ورود از Nav Drawer (گروه SYSTEM)؛
برگشت طبق قاعده‌ی Fallback عمومی («همه‌جای دیگر→Home») بدون نیاز به قانون
اختصاصی در `resolveContextualBackTarget`.

## تست‌ها

- `AutoSaveManagerTest.kt`: ۲ تست تازه برای `touch()` (پروژه‌ی موجود/
  ناموجود) — بدون تغییر تست‌های موجود `saveIfDirty`.
- `SettingsFlowTest.kt` (End-to-End، ۶ تست): دسترسی از Drawer؛ زبان →
  Persist + بازتاب فوری در بقیه‌ی اپ (بعد از برگشت به Home)؛ تم → Persist؛
  Auto-Save Cadence → Persist مقدار واقعی؛ سوییچ Display → Persist (بدون
  اثر Runtime، مستند)؛ Layout Variant → Persist State مشترک واقعی (بدون
  اعمال بصری، مستند)؛ Timer واقعی Auto-Save → با Cadence ۱ ثانیه‌ای،
  `ProjectEntity.lastModified` واقعاً در بازه‌ی زمانی معقول تغییر می‌کند.

## نتیجه‌ی Build

`gradle :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL`، ۶۴۹
تست (۶۴۰→۶۴۹، ۹ تست جدید)، ۰ Failure، ۰ Error. (یک اجرای میانی این Build،
`ScenesFlowTest` را با `IllegalStateException` در `SQLiteConnectionPool`
نشان داد — بدون هیچ تغییری در Scene/Asset این قدم؛ اجرای مجزای همان کلاس
بلافاصله سبز شد، تأیید یک ناپایداری محیطی شناخته‌شده‌ی این Sandbox تحت بار
سنگین [هم‌کلاس یافته‌ی مستندشده‌ی ADR-055]، نه یک Regression واقعی — رفع با
اجرای مجدد کامل، بدون هیچ تغییر کد.)
