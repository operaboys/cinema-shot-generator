# ADR-069: Export واقعی فایل (Intent.ACTION_SEND) + محدودیت شناخته‌شده‌ی ترجمه‌ی فارسی

## زمینه

آخرین یافته‌ی باقی‌مانده از تصمیمات مشترک (ADR-067، بخش G4/G18): دکمه‌ی
«Export» صفحه‌ی Output Delivery فعلاً مستعار «Copy» بود (هر دو فقط
`clipboardManager.setText` صدا می‌زدند)؛ `domain/outputdelivery/
OutputComposer.kt` (`composeOutput`) هرگز از `ui/` صدا زده نمی‌شد.

## بازبینی مستقل پیش‌بررسی دستور کار

پیش‌بررسی («composeOutput نیاز به BilingualPrompts دارد، اما
generateBilingualPrompt/translateToFarsi عمداً پیاده نشده‌اند») با خواندن
مستقیم `domain/outputdelivery/Bilingual.kt` تأیید شد — کامنت خودِ فایل
صریحاً می‌گوید: «NOTE (خارج از Scope این قدم، عمداً پیاده نشد):
generateBilingualPrompt/translateToFarsi ... یک موتور ترجمه‌ی واقعی
... خودش یک تصمیم معماری جداست». همچنین تأیید شد `RenderedOutput.language`
همیشه `"en"` است (`Renderer.kt: render(...) = RenderedOutput(..., language
= "en")`، با کامنت مطابق در `PromptCleaner.kt`). این محدودیت، طبق دستور
صریح، **یک تصمیم معماری در حال بحث بین معمار و کاربر پروژه است، نه یک
بدهی فنی ساده یا TODO** — گزینه‌های در حال بررسی (ML Kit Translation
آفلاین، یا فیلد دوگانه‌ی ورودی کاربر) هنوز انتخاب نشده‌اند.

## بخش الف — اتصال composeOutput

`OutputDeliveryViewModel.regenerate()` اکنون بلافاصله بعد از رندر/پاک‌سازی
موفق (نه در لحظه‌ی کلیک Export) `composeOutput` را با
`renderedOutputs = listOf(finalRenderedOutput)` (این ViewModel هر لحظه
دقیقاً یک مدل هدف انتخاب‌شده دارد — بررسی با grep تأیید کرد `_state`
همیشه یک `RenderedOutput` تکی نگه می‌دارد، نه لیستی از چند مدل هم‌زمان)
صدا می‌زند. نتیجه (`OutputPackage`) در `OutputDeliveryState.Ready.outputPackage`
ذخیره می‌شود.

**تصمیم مستقل — Bilingual Fallback صادقانه:** طبق راه‌حل عملی دستور کار،
`enVersion`/`faVersion` هر دو برابر همان `finalRenderedOutput.formattedPrompt`
هستند — نه یک ترجمه‌ی جعلی، نه یک رشته‌ی خالی گمراه‌کننده. این یک
Placeholder صادقانه و مستند است تا زمانی که تصمیم معماری بالا (موتور
ترجمه‌ی واقعی) نهایی شود.

**نتیجه‌ی واقعی composeOutput در این پروژه:** چون همیشه دقیقاً یک مدل
انتخاب‌شده وجود دارد، `exportFiles` همیشه دقیقاً ۳ فایل است (`..._prompt_en.txt`,
`..._prompt_fa.txt`, `..._<profileId>.txt`) — هرگز یک فایل تنها. این
یافته مستقیماً روی طراحی Intent در بخش ب اثر گذاشت (پایین).

## بخش ب — Export واقعی با Intent.ACTION_SEND

با grep تأیید شد پروژه **هیچ FileProvider موجودی نداشت** — این اولین
`<provider>` کل پروژه است. الگوی `BackupFileStorage`/`DeviceBackupFileStorage`
(Interface + پیاده‌سازی Context-محور، برای قابل‌تست‌ماندن با Robolectric)
دقیقاً برای `ExportFileWriter`/`DeviceExportFileWriter` (`data/repository/
ExportFileWriter.kt`) بازاستفاده شد.

**تصمیم مستقل — `cacheDir/exports`، نه `filesDir/backups`:** فایل‌های
Export موقتی‌اند (فقط تا زمانی که اپ مقصد آن‌ها را از طریق FileProvider
خوانده لازم‌اند) — دقیقاً کاربرد مستند `cacheDir`، برخلاف `BackupFileStorage.
backupDir` که Persistent است و از صفحه‌ی Backups قابل مرور/بازیابی.

**زیرساخت FileProvider:** `AndroidManifest.xml` (`<provider
android:name="androidx.core.content.FileProvider" android:authorities=
"com.operaboys.cinemashotgenerator.fileprovider" android:exported="false"
android:grantUriPermissions="true">`) + `res/xml/file_paths.xml`
(`<cache-path name="exports" path="exports/" />`، هم‌نام دقیق با
`DeviceExportFileWriter.exportDir`).

**جدایی مسئولیت ViewModel/UI:** `exportOutput(): Job` (ViewModel) فقط
`exportFiles` بسته‌ی فعلی را واقعاً روی دیسک می‌نویسد (`ExportFileWriter`
تزریقی) و نتیجه را در یک رویداد یک‌باره (`exportedFiles: StateFlow<List<File>?>`
+ `clearExportedFiles()`، هم‌الگو دقیق با `lastActionMessage`/
`clearLastActionMessage` موجود `ProjectListViewModel`) می‌گذارد.
ساخت/باز‌کردن واقعی `Intent.ACTION_SEND`/`Intent.createChooser` فقط از
`ui/outputdelivery/OutputDeliveryScreen.kt` (یک `LaunchedEffect`) انجام
می‌شود — چون این کار نیازمند Context یک Activity واقعی است، نه چیزی که
ViewModel باید نگه دارد.

**`buildExportShareIntent`** (`ui/outputdelivery/ExportIntentBuilder.kt`)
عمداً از `context.startActivity`/`Intent.createChooser` جدا شد — طبق
دستور کار («بدون نیاز به تست واقعی Intent System»)، این تابع (ساخت Intent
از یک لیست File، شامل فراخوانی واقعی `FileProvider.getUriForFile`) کاملاً
مستقل و قابل‌تست است؛ فقط خودِ `startActivity` (که تنها معنادار در یک
Activity واقعی است) از تست جدا نگه داشته شد.

**تصمیم مستقل — Copy و Export دیگر مستعار هم نیستند:** دکمه‌ی Copy
دست‌نخورده ماند (فقط Clipboard). دکمه‌ی Export اکنون `viewModel.exportOutput()`
را صدا می‌زند — رفتار کاملاً متفاوت (نوشتن فایل واقعی + اشتراک‌گذاری
سیستمی)، نه یک نام دوم برای همان عملیات. کلید `outputDelivery.exportedMessage`
(که فقط برای پیام Clipboard قدیمی بود) حذف و با `outputDelivery.exportChooserTitle`
(عنوان Chooser) جایگزین شد.

## یافته‌ی واقعی دیباگ — محدودیت Robolectric/FileProvider (نه باگ محصول)

حین نوشتن تست‌ها کشف شد: `androidx.core.content.FileProvider` لیست
«ریشه‌های پیکربندی‌شده» (از `file_paths.xml`) را فقط یک‌بار در
`attachInfo` می‌سازد و Cache می‌کند. وقتی دو کلاس تست جدا (مثلاً یک
`ExportIntentBuilderTest` مستقل + `OutputDeliveryFlowTest`) هرکدام
`context.cacheDir` مخصوص Sandbox موقت خودشان را دارند (رفتار عادی
Robolectric)، اما در همان JVM اجرای Gradle (بدون Fork جداگانه به‌ازای هر
کلاس) نمونه‌ی ContentProvider واقعی بین این کلاس‌ها بازاستفاده می‌شود،
ریشه‌ی Cache‌شده‌ی اولین کلاس برای کلاس دوم دیگر معتبر نیست —
`IllegalArgumentException: Failed to find configured root`. این کاملاً
محدود به محیط تست Robolectric است (در یک اپ واقعی، `cacheDir` در طول عمر
Process همیشه ثابت است، پس این هرگز رخ نمی‌دهد) — تأییدشده Deterministic
(نه فقط گاهی)، نه از کلاس Flake محیطی مستندشده‌ی موجود (ADR-044/۰۵۹/۰۶۰/۰۶۲).
**رفع:** به‌جای یک کلاس تست جداگانه‌ی سطح‌پایین برای `buildExportShareIntent`،
اثبات «Uri واقعاً به محتوای فایل حل می‌شود» مستقیماً داخل تست End-to-End
موجود (`OutputDeliveryFlowTest`) ادغام شد — پوشش کامل بدون هیچ دو کلاس
تست جدا که هر دو FileProvider واقعی را لمس کنند.

## تست‌ها

- `OutputDeliveryViewModelTest.kt` (۲ تست تازه): `regenerate` یک
  `OutputPackage` واقعی با ۳ `exportFiles` و Fallback صادقانه‌ی Bilingual
  می‌سازد؛ `exportOutput` واقعاً `exportFiles` را از طریق `ExportFileWriter`
  تزریقی (`FakeExportFileWriter`، هم‌الگو با `FakeBackupFileStorage`)
  می‌نویسد و رویداد `exportedFiles` را پر/پاک می‌کند.
- `OutputDeliveryFlowTest.kt` (۱ تست تازه، End-to-End): کلیک واقعی روی
  Export → `Intent.ACTION_CHOOSER` واقعی (پوشاننده‌ی `ACTION_SEND_MULTIPLE`،
  چون همیشه ۳ فایل) با `EXTRA_STREAM` واقعی → محتوای یکی از Uri ها واقعاً
  از طریق `ContentResolver` (FileProvider واقعی) قابل خواندن است — اثبات
  مستقیم که Export و Copy دیگر یک رفتار مشترک زیر دو نام نیستند.

## پیوست — جزئیات کامل ۳ شکست تست گزارش‌شده‌ی این قدم (پاسخ به سؤال پیگیری معمار)

گزارش نهایی این قدم فقط به‌صورت خلاصه («همان کلاس Flake محیطی
از‌پیش‌مستند») به ۳ شکست اشاره کرد، بدون نام دقیق — بر خلاف الگوی
همیشگی این پروژه (مثل ADR-060/062). این بخش آن نقص را رفع می‌کند: نام
دقیق هر ۳ تست، تشخیص «قبلاً مستند شده» در برابر «تازه»، و شواهد واقعی
(نه فقط استنتاج از روی امضای خطا) برای هرکدام — شامل اجراهای Isolated
تازه‌ای که مخصوص همین پاسخ انجام شدند.

### ۱. نام دقیق هر ۳ تست (از فایل‌های XML واقعی همان اجرا، `commit 253381d`)

1. `AssetFormFlowTest > "clicking an existing character card opens the
   edit form pre-filled, and saving updates the same asset instead of
   creating a new one"` (`AssetFormFlowTest.kt:210`) —
   `ComposeTimeoutException`: منتظر گره‌ای با متن «کاراکتر جدید» ماند و
   ظرف ۵۰۰۰ms پیدا نکرد.
2. `OutputDeliveryFlowTest > "model picker chips render as distinct,
   fully visible, opaque nodes both selected and unselected"` —
   `IllegalStateException: Cannot perform this operation because the
   connection pool has been closed.` (خطای SQLite، هنگام
   `ShotDao_Impl.loadShot`).
3. `ShotsFlowTest > "deleting a scene that still has a shot is blocked
   with a real message, deleting after the shot is removed actually
   deletes it"` (`ShotsFlowTest.kt:458`) — `ComposeTimeoutException`
   عمومی (بدون شرط مشخص در پیام).

### ۲. کدام‌ها قبلاً مستند بودند، کدام‌ها تازه‌اند

- **#۳ (`ShotsFlowTest > deleting a scene...`) — دقیقاً همان تست
  قبلاً مستندشده است**، کلمه‌به‌کلمه: در README (دو بار، خط ۱۱۲۰ و
  ۱۱۵۰ نسخه‌ی این قدم) و در `docs/adr/062-post-unit16-audit-g6-g22-
  fixes.md` (خط ۱۴۱/۱۵۸) و `docs/adr/063-post-unit16-audit-g15-g16-
  fixes.md` (خط ۱۲۲) — این چهارمین باری است که همین تست دقیقاً با همین
  نام در قدم‌های متفاوت شکست می‌خورد.
- **#۱ (`AssetFormFlowTest > clicking an existing character card...`)
  — تازه است.** کلاس `AssetFormFlowTest` قبلاً چند بار (README خطوط
  ۱۱۸۷/۱۲۴۹/۱۲۸۹) به‌عنوان یکی از چند کلاس Flake محتمل نام برده شده
  بود، اما **هرگز این متد مشخص با این نام** ثبت نشده بود — این اولین
  بار است که این Test Case خاص با نام کامل مستند می‌شود.
- **#۲ (`OutputDeliveryFlowTest > model picker chips...`) — کاملاً
  تازه است.** هیچ‌جای README یا هیچ ADR ای تا پیش از این قدم این تست
  را (نه به نام، نه به‌طور ضمنی) به‌عنوان Flake ثبت نکرده بود — اولین
  باری است که این تست مشخص شکست خورده.

### ۳. شواهد واقعی (نه فقط تطبیق امضا) — اجراهای Isolated تازه، مخصوص همین پاسخ

با `git diff`، هیچ‌کدام از سه فایل تست (`AssetFormFlowTest.kt`،
`OutputDeliveryFlowTest.kt` به‌جز افزودن یک تست کاملاً جدید و مستقل در
انتهای فایل، `ShotsFlowTest.kt`) یا وابستگی‌های واقعی‌شان
(`ModelPickerSection`/`ALL_MODEL_PROFILES`، فرم ویرایش کاراکتر، حذف
Scene) توسط این قدم دست نخورده‌اند. برای اثبات فراتر از این (طبق
درخواست صریح معمار)، ۴ اجرای Isolated تازه انجام شد:

1. `AssetFormFlowTest` به‌تنهایی (به همراه `OutputDeliveryFlowTest` در
   همان دستور) → **۸ از ۸ موفق، صفر شکست.** همان تستی که در اجرای اصلی
   شکست خورده بود، این‌بار تمیز رد شد.
2. `OutputDeliveryFlowTest` در همان اجرا (کنار `AssetFormFlowTest`) →
   ۱ شکست، اما در `model picker chips...` این‌بار با یک خطای SQLite
   **متفاوت** (`IllegalStateException: attempt to re-open an
   already-closed object: SQLiteDatabase: :memory:` — نه همان
   Connection Pool قبلی).
3. `OutputDeliveryFlowTest` کاملاً تنها (بدون هیچ کلاس دیگر) →
   **۶ از ۶ موفق، صفر شکست** — شامل همان `model picker chips...`.
4. `ShotsFlowTest` کاملاً تنها (بدون هیچ کلاس دیگر) → ۲ شکست از ۹:
   همان `deleting a scene...` (مورد #۳ بالا) **و یک تست کاملاً دیگر و
   جدید** (`selecting each camera movement kind shows its own
   conditional fields`) که تا این اجرا هرگز شکست نخورده بود — با همان
   `SQLiteConnectionPool` closed.

**نتیجه‌گیری:** هیچ‌کدام از این تست‌ها به‌طور Deterministic شکست
نمی‌خورند (هرکدام گاهی رد می‌شوند، گاهی نه، با پیام خطای متفاوت در
اجراهای مختلف، حتی وقتی هیچ کدی بین دو اجرا عوض نشده) — دقیقاً همان
تعریف صریح خودِ این پروژه از Flake محیطی (`docs/adr/062-...`: «در
اجراهای مکرر Isolated در ۴ نقطه‌ی کاملاً متفاوت شکست خورد، نه همیشه
همان خط»). امضای خطا هم دقیقاً همان کلاس شناخته‌شده است:
`SQLiteConnectionPool`/`SQLiteDatabase` بسته‌شده زیر بار (مستند در
ADR-059/060) یا `ComposeTimeoutException` بدون هیچ شرط تازه‌ی مرتبط با
کد این قدم. با این حال، چون #۱ و #۲ اولین‌بار با این نام مشخص ثبت
می‌شوند، این دو از این پس به فهرست تست‌های شناخته‌شده‌ی این کلاس Flake
اضافه شدند (کنار `ShotsFlowTest > deleting a scene...` و
`ScenesFlowTest > connecting a location asset...`).

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
