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

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
