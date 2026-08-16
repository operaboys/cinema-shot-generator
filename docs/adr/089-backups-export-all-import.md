# ADR-089: Export All / Import Backup در صفحه‌ی Backups (یافته‌ی #۱۲ appendix، آخرین یافته‌ی سطح ۳ باقی‌مانده)

## زمینه

`BackupsScreen.kt` فقط عملیات تک‌تک (Restore/Delete روی هر ردیف) داشت — هیچ
عملیات دسته‌جمعی (اشتراک‌گذاری همه‌ی بکاپ‌های این پروژه با اپ‌های دیگر، یا
وارد کردن یک فایل بکاپ خارجی به فهرست) وجود نداشت.

## تصمیم قطعی

- **Export All**: تمام بکاپ‌های پروژه‌ی جاری با `Intent.ACTION_SEND[_MULTIPLE]`
  واقعی اشتراک‌گذاری می‌شوند — دقیقاً هم‌الگو با Export خروجی پرامپت
  (`docs/adr/069-real-output-export-and-bilingual-limitation.md`).
- **Import Backup**: یک فایل بکاپ خارجی را فقط به فهرست بکاپ‌های پروژه‌ی جاری
  **معرفی** می‌کند (نه Restore فوری) — کاربر بعداً از همان دکمه‌ی Restore
  موجود روی این ردیف تازه استفاده می‌کند، دقیقاً مثل هر بکاپ دیگر.

## پیاده‌سازی

### Export All

- `BackupManager.readAllBackupsForExport(): Result<List<Pair<fileName, content>>>`
  (تازه) — محتوای واقعی هر بکاپ این پروژه را می‌خواند؛ نام فایل مستقیماً از
  مسیر واقعی هر بکاپ گرفته می‌شود (نه بازسازی دستی).
- `BackupsViewModel` هم‌الگو دقیق با `OutputDeliveryViewModel` (ADR-069) شد:
  `exportFileWriter: ExportFileWriter = DeviceExportFileWriter(application)`
  (پیش‌فرض، بدون نیاز به سیم‌کشی تازه از `BackupsScreen`/`AppNavHost`) +
  `exportedFiles: StateFlow<List<File>?>` (رویداد یک‌باره) + `exportAll()`.
- `BackupsScreen.kt`: دکمه‌ی «خروجی گرفتن از همه» (فقط وقتی حداقل یک بکاپ
  وجود دارد فعال است) + `LaunchedEffect(exportedFiles)` که
  `buildExportShareIntent` موجود (ADR-069) را با `mimeType = "text/plain"`
  صدا می‌زند (محتوای بکاپ عملاً متن JSON خام است، دقیقاً هم‌الگو با متن خروجی
  پرامپت) و Chooser سیستم را باز می‌کند. `cacheDir/exports` (مسیر نوشتن
  `DeviceExportFileWriter`) از قبل در `file_paths.xml` expose شده بود — هیچ
  تغییری در FileProvider لازم نبود.

### Import Backup

- `BackupManager.importBackup(content: String): Result<String>` (تازه):
  ۱) `deserializeFullProject(content)` — همان سطح اعتبارسنجی‌ای که
  `restoreFromBackup` از قبل به آن اعتماد می‌کند (فقط شکل معتبر JSON، **نه**
  بررسی سخت‌گیرانه‌ی یکپارچگی ارجاعی `ExportImportRepository.importProject`
  — آن تابع یک ویژگی جدا («Import Project») برای وارد کردن یک پروژه‌ی کامل
  خارجی است؛ اینجا فقط یک بکاپ از همین اپ به فهرست همین پروژه اضافه می‌شود،
  همان سطح اعتماد بکاپ‌های موجود). ۲) بررسی تازه‌ی این قدم:
  `snapshot.project.projectId == projectId` — بدون آن، یک بکاپ متعلق به
  پروژه‌ی دیگر می‌توانست بی‌صدا زیر پیشوند نام‌فایل این پروژه ثبت شود اما روز
  Restore داده‌ی یک پروژه‌ی کاملاً دیگر را بنویسد. هر دو شکست پیام واقعی
  برمی‌گردانند، نه سکوت.
- `BackupsScreen.kt`: دکمه‌ی «وارد کردن بکاپ» یک `ActivityResultContracts.
  GetContent()` باز می‌کند (**نه** `OpenDocument()`).

## تصمیم مستقل — GetContent() نه OpenDocument()

دستور کار پرسیده بود این دو الگوی موجود پروژه مقایسه شوند:
- **`ProjectsScreen.kt` (Import Project، ADR-065)**: `GetContent()` — محتوای
  Uri همان‌جا، همان یک‌بار، فوری خوانده می‌شود.
- **`SettingsScreen.kt` (Choose Home Image، G10/ADR-075)**: `OpenDocument()` +
  `takePersistableUriPermission` — چون آن Uri در DataStore Persist می‌شود و
  قرار است در اجراهای بعدی اپ دوباره خوانده شود (کامنت صریح خودِ آن فایل).

Import Backup دقیقاً هم‌شکل حالت اول است: محتوای فایل انتخاب‌شده همان‌جا (در
callback خودِ Launcher) خوانده و بلافاصله به `BackupManager.importBackup`
داده می‌شود؛ Uri بعد از آن هرگز دوباره لازم نیست. پس `GetContent()` انتخاب
شد — تطبیق مستقیم با تمایز مستندی که خودِ `SettingsScreen.kt` قبلاً ثبت کرده
بود، نه یک انتخاب تازه.

## تصمیم مستقل — سطح تست Import

کلیک واقعی روی دکمه‌ی Import در یک تست Compose End-to-End، خودِ File Picker
سیستم‌عامل را باز می‌کند — چیزی که در کل این کدبیس، حتی برای Import Project
موجود (ADR-065)، هیچ‌جا شبیه‌سازی نشده (فقط سطح ViewModel/Repository تست
شده، `ProjectListViewModelImportTest.kt`). همان مرز اینجا هم رعایت شد:
۴ تست جدید `BackupManagerTest.kt` مستقیماً `importBackup`/
`readAllBackupsForExport` را می‌سنجند (اعتبارسنجی موفق/فرمت نامعتبر/
projectId نامتناظر/جمع‌آوری محتوای واقعی چند بکاپ)؛ Export All (که نیازی به
File Picker ندارد) در `BackupsFlowTest.kt` یک تست End-to-End واقعی کامل شد
(کلیک دکمه → `Intent.ACTION_SEND_MULTIPLE` واقعی با Uri های واقعی، هم‌الگو
دقیق با تست معادل `OutputDeliveryFlowTest.kt`).

## یافته‌ی واقعی رگرسیون تست

افزودن ردیف دکمه‌های Export All/Import بالای فهرست، ارتفاع محتوای بالای هر
کارت بکاپ را زیاد کرد — یک تست موجود قبلی (`creating a manual backup shows
it in the list with a Manual badge`) که بدون `performScrollTo()` مستقیم
`assertIsDisplayed()` روی دکمه‌های Restore/Delete صدا می‌زد، روی صفحه‌ی کوچک
شبیه‌سازی‌شده‌ی تست شکست خورد (دیگر از ابتدا در Viewport نبودند). رفع: افزودن
`performScrollTo()`، هم‌الگو با هر ارجاع دیگر به این دو دکمه در همان فایل.

همچنین یک یافته‌ی دیباگ مستقل: انتظار اولیه برای Seed کردن ۲ بکاپ با بررسی
مستقیم `fakeBackupStorage` درون `waitUntil { runBlocking {...} }` بی‌اعتماد
بود (رقابت دو Dispatcher روی یک Thread). رفع با ماندن کاملاً درون درخت
Semantics خودِ Compose (شمارش تعداد دکمه‌ی «بازیابی» رندرشده)، هم‌الگو با
سایر `waitUntilXxx` این فایل.

## تست

- `BackupManagerTest.kt`: ۴ تست تازه (Import موفق/فرمت نامعتبر/projectId
  نامتناظر + جمع‌آوری محتوای Export All).
- `BackupsFlowTest.kt`: ۲ تست تازه (Export All → Intent واقعی با ۲ Uri؛
  دکمه‌ی Export All غیرفعال بدون هیچ بکاپی) + رفع رگرسیون تست موجود بالا.
  Rule کلاس از `createComposeRule()` به `createAndroidComposeRule<
  ComponentActivity>()` تغییر کرد (هم‌الگو دقیق با ADR-069 — همان چیز را
  داخلاً می‌سازد، فقط `.activity` را هم افشا می‌کند؛ ۹ تست قبلی این فایل بدون
  تغییر رفتار سبز ماندند).

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest --tests "...ui.backups.*" --tests "...BackupManagerTest"` | ۲۲ تست، ۰ شکست |
| `gradle :app:testDebugUnitTest` (کل Suite) | موفق |
| `gradle :app:assembleDebug` | موفق |
