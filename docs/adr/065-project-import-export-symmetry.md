# ADR-065: رفع «عدم قرینگی Export/Import» — G14/ADR-064

## زمینه

ADR-064 (رفع G14) این یافته‌ی معماری را کشف و برای تصمیم مشترک علامت‌گذاری
کرده بود: کاربر می‌توانست پروژه Export کند اما هرگز نمی‌توانست فایل Export
را دوباره Import کند — `importProject` (تابع دامنه‌ی موجود،
`data/repository/ExportImportRepository.kt`) هیچ فراخوان‌کننده‌ای در `ui/`
نداشت. این قدم آن را رفع می‌کند.

## بازبینی مستقل پیش‌بررسی‌های دستور کار (طبق دستور صریح)

- `importProject(fileUri, backupFileStorage, projectDao, sceneDao, shotDao, assetDao, projectDnaDao, audioContextDao): Result<ProjectEntity>` —
  تأیید شد با خواندن کامل `ExportImportRepository.kt`: کاملاً پیاده‌سازی و
  تست شده (`ExportImportRepositoryTest.kt`)، شامل `validateReferentialIntegrity`
  واقعی که قبل از هر نوشتنی اجرا می‌شود — تضمین «صفر-نوشتن» در کامنت خودِ
  فایل مستند است.
- هیچ زیرساخت File Picker واقعی در کل پروژه پیدا نشد
  (`grep -rln "ActivityResultContracts\|GetContent\|OpenDocument\|rememberLauncherForActivityResult"`
  صفر نتیجه) — G10/G11 (File Picker تصویر) دست‌نخورده ماندند، طبق دستور
  صریح.
- **یک مغایرت واقعی/شکاف پیدا شد که پیش‌بررسی به آن اشاره نکرده بود:**
  `BackupFileStorage.readFile(path: String)` (پیاده‌سازی واقعی،
  `DeviceBackupFileStorage`) مستقیماً `java.io.File(path).readText()`
  می‌سازد — یعنی یک مسیر فایل خام روی دیسک انتظار می‌رود، **نه** یک
  `content://` Uri. اما `ActivityResultContracts.GetContent()` (تنها راه
  استاندارد Android برای انتخاب فایل دلخواه از حافظه‌ی دستگاه) همیشه یک
  `content://` Uri برمی‌گرداند که `java.io.File` نمی‌تواند مستقیماً آن را
  بخواند. این یک تضاد واقعی قرارداد بود که باید حل می‌شد — جزئیات تصمیم
  زیر.

## تصمیم ۱ — پل بین content:// Uri و قرارداد File-Path موجود: Staging در همان backupDir

به‌جای تغییر امضای `importProject`/`BackupFileStorage` (که هم دامنه را لمس
می‌کرد، هم برخلاف صراحت دستور کار «هیچ منطق دامنه‌ی جدیدی لازم نیست»
بود)، حل مسئله در لایه‌ی ViewModel (که از قبل `AndroidViewModel` است و به
`Context` دسترسی دارد) انجام شد:

1. `ProjectListViewModel.importProject(fileUri: String)` محتوای فایل
   انتخاب‌شده را از طریق `ContentResolver.openInputStream(Uri.parse(fileUri))`
   می‌خواند (پشتیبانی داخلی و همیشگی Android از `content://`/`file://`).
2. همان محتوا با همان `backupFileStorage.writeFile(...)` **موجود** (بدون
   هیچ تغییری در `BackupFileStorage.kt`) در `backupDir` همین اپ Stage
   می‌شود — یک مسیر فایل واقعی برمی‌گرداند.
3. `importProject` دامنه دقیقاً طبق قرارداد مستندش (مسیر فایل واقعی) با
   همان مسیر Stage‌شده صدا زده می‌شود.

**نتیجه:** هیچ خطی در `ExportImportRepository.kt`/`BackupFileStorage.kt`
تغییر نکرد — دقیقاً طبق ادعای دستور کار («هیچ منطق دامنه‌ی جدیدی لازم
نیست»)؛ فقط شکافِ واقعیِ Uri-vs-Path (که پیش‌بررسی از آن غافل بود) در
لایه‌ی UI/ViewModel حل شد.

**تصمیم فرعی — عدم پاک‌سازی فایل Stage‌شده بعد از Import:** هم‌الگو با
تصمیم قبلی مستند Export («Export یک عملیات دستی و بدون‌محدودیت‌تعداد
است، بدون `cleanOldBackups`») — فایل موقت Stage‌شده هم بدون پاکسازی
خودکار باقی می‌ماند. یک ساده‌سازی آگاهانه و کم‌ریسک، نه یک نشتی حافظه‌ی
قابل توجه (فایل‌های JSON پروژه معمولاً کوچک‌اند).

## تصمیم ۲ — `importProject(fileUri: String): Job`، هم‌الگو دقیق با `exportProject`

طبق دستور صریح، امضا و الگوی داخلی (`ioScope.launch`، `db` تزریقی،
`onFailure → _lastActionMessage`) عیناً از `exportProject` کپی شد.
`Job` برگردانده می‌شود تا تست‌های مستقیم (`ProjectListViewModelImportTest.kt`)
بتوانند `.join()` کنند — همان دلیل مستندشده در G15/ADR-063.

## تصمیم ۳ — نقطه‌ی ورود UI: دکمه‌ی سطح-فهرست در `ProjectsScreen.kt`، نه Per-Card

Export یک آیتم منوی سه‌نقطه‌ی **هرکارت پروژه** است (`ProjectCard.kt` →
`ProjectCardActions.onExport`) — منطقی، چون یک پروژه‌ی *موجود* را خروجی
می‌گیرد. Import برخلاف آن **یک پروژه‌ی تازه می‌سازد**، پس به هیچ کارتی
تعلق ندارد — یک عملیات سطح-فهرست است، هم‌الگو مفهومی با «پروژه‌ی جدید»ی
`HomeScreen.kt`. یک `IconButton` (آیکون `FileUpload`) در Header صفحه‌ی
`ProjectsScreen.kt` (کنار عنوان/زیرعنوان) اضافه شد — تنها صفحه‌ای که
مدیریت کامل فهرست پروژه‌ها را نشان می‌دهد (طبق docs/design/README.md
بخش «۲. Projects»).

## تصمیم ۴ — مدیریت خطا: بدون پیام موفقیت تازه (هم‌الگو با عملیات خواهر)

بررسی شد `createProject`/`renameProject`/`duplicateProject`/`archiveProject`/
`deleteProject` هیچ‌کدام پیام موفقیت صریح ندارند — فقط شکست از طریق
`_lastActionMessage` نمایش داده می‌شود؛ موفقیت با ظاهرشدن/تغییر خودِ
لیست (Flow-محور، خودکار) دیده می‌شود. `importProject` هم دقیقاً همین
الگو را دنبال کرد — هیچ کلید UiStrings تازه‌ای برای «موفقیت» اضافه نشد؛
فقط `projects.importButton` (برچسب/توضیح خودِ دکمه) اضافه شد.

## تست‌ها (`ProjectListViewModelImportTest.kt`، تازه)

- **Round-Trip واقعی:** Export یک پروژه‌ی واقعی → حذف آن از دیتابیس
  (شبیه‌سازی «حذف تصادفی/نصب دیگر»، دقیقاً سناریوی محصولی ADR-064) →
  `importProject` روی همان فایل → پروژه با همان نام دوباره در دیتابیس
  ظاهر می‌شود.
- **فایل خراب:** یک رشته‌ی غیر-JSON در یک فایل موقت نوشته و Import
  می‌شود → `lastActionMessage` واقعی پر می‌شود، هیچ پروژه‌ای ساخته
  نمی‌شود.
- **یکپارچگی ارجاعی نقض‌شده:** یک `FullProjectSnapshot` دستی با یک
  `ShotEntityDto` که به یک `sceneId` ناموجود ارجاع می‌دهد (طبق دستور کار،
  با موفقیت شبیه‌سازی شد) → رد می‌شود با پیام واقعی حاوی «یکپارچگی
  ارجاعی»، و پروژه هرگز نوشته نمی‌شود (تضمین صفر-نوشتن تأیید شد).

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
