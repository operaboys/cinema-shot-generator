# ADR-024: تصمیمات پیاده‌سازی واحد ۱۵ — تکمیل Export/Import (آخرین مورد فهرست ADR-017)

**تاریخ:** 2026-07-21
**وضعیت:** آخرین مورد «فقط امضا»ی باقی‌مانده از ADR-017 تکمیل شد. با این قدم، واحد ۱۵ (Project Storage) کاملاً بدون هیچ عملکرد فقط-امضایی باقی‌مانده است.

## Context

`exportProject`/`importProject` آخرین دو تابع فهرست ADR-017 بودند که فقط امضا داشتند. برخلاف `AutoSaveManager`/`BackupManager` (که هرکدام زیرساخت تازه‌ای لازم داشتند)، این قدم عمدتاً **بازترکیب زیرساخت قدم قبل** بود: `serializeFullProject`/`deserializeFullProject`/`restoreProjectFromSnapshot`/`FullProjectSnapshot`/`BackupFileStorage` (همه از `docs/adr/023-...md`) مستقیماً بازاستفاده شدند — هیچ‌کدام دوباره ساخته نشدند.

## تصمیم ۱: تفاوت واقعی Export با Backup

- **Backup** (قدم قبل): نام‌گذاری خودکار (`backup_${projectId}_${backupId}.json`)، محدود به `maxBackupsToKeep` (`cleanOldBackups`)، برای بازیابی در همان دستگاه.
- **Export** (این قدم): نام فایل با یک الگوی معنادار (`${projectId}_export_${timestamp}.json`)، بدون هیچ محدودیت تعداد یا حذف خودکار (Export یک عملیات دستی برای انتقال بین دستگاه است، نه یک چرخه‌ی نگه‌داری خودکار) — پس فقط از `BackupFileStorage.writeFile`/`readFile` استفاده شد؛ `listFiles`/`deleteFile` برای این دو تابع لازم نبودند (طبق تصریح دستور کار).
- **انحراف صریح از کد مفهومی بلوپرینت:** بلوپرینت نام فایل Export را `"${projectId}_export.json"` (بدون timestamp) می‌نویسد. طبق دستور صریح این قدم، یک timestamp (`Instant.now().toEpochMilli()`) اضافه شد تا دو Export پیاپی از یک پروژه یکدیگر را overwrite نکنند — یک بهبود خواسته‌شده، نه انحراف بی‌دلیل.

## ⚠️ یافته‌ی مهم: `validateReferentialIntegrity` از قبل وجود داشت — این قدم آن را دوباره نساخت، فقط سیم‌کشی کرد

دستور کار این قدم فرض کرده بود «بررسی ارجاع شکسته هنوز نیست، این قدم آن را اضافه می‌کند» و خواسته بود `IntegrityIssue`/`ShotReferences`/`validateReferentialIntegrity` در `domain/storage/StorageValidation.kt` اضافه شود. پیاده‌سازی اولیه دقیقاً همین را انجام داد — اما اجرای واقعی `gradle :app:testDebugUnitTest` با خطای کامپایل **Redeclaration** شکست خورد: `domain/storage/ReferentialIntegrity.kt` (فایل جدا، همان پکیج) از قبل دقیقاً همین سه ساختار را دارد — `IntegrityIssue`، `ShotReferenceData` (نه `ShotReferences`)، `validateReferentialIntegrity`، به‌علاوه یک Wrapper اضافه (`validateReferentialIntegrityAsIssues` که به `ValidationIssue` سراسری نگاشت می‌کند) — همراه با تست کامل خودش (`ReferentialIntegrityTest.kt`، ۴ تست: سالم، Scene نامعتبر، Asset نامعتبر، نگاشت به ValidationIssue). با `git log` تأیید شد این فایل‌ها از **همان commit اول واحد ۱۵** (`311f23d`, «Add unit 15 step 1»)، یعنی خیلی قبل‌تر از این قدم، وجود داشتند — ساخته شده بودند ولی تا این قدم به هیچ `Export`/`Import` واقعی وصل نشده بودند.

**رفع:** افزوده‌ی تکراری از `StorageValidation.kt`/`StorageValidationTest.kt` کاملاً برداشته شد (`git checkout` — این دو فایل اکنون دقیقاً دست‌نخورده‌اند، برخلاف Scope اولیه‌ی دستور کار که این‌ها را «افزوده» می‌خواست، چون افزودن دیگر لازم نبود). `ExportImportRepository.kt` مستقیماً از `domain.storage.ShotReferenceData`/`validateReferentialIntegrity` موجود Import و استفاده می‌کند. این تصمیم به‌جای پرسیدن از کاربر گرفته شد چون یک واقعیت مکانیکی قابل‌کشف با `git log` بود (نه یک ابهام معماری) — مسیر درست، بازاستفاده از کد موجود به‌جای دو نسخه‌ی موازی از یک منطق، کاملاً هم‌راستا با روح خودِ دستور کار («بازترکیب زیرساخت موجود، نه کد جدید بزرگ») بود، فقط این‌که زیرساخت موجود بیشتر از آنچه دستور کار فرض کرده بود بود.

**درس برای آینده:** پیش از افزودن هر تابع دامنه‌ی جدید، grep کل پکیج مقصد (نه فقط فایلی که دستور کار نام برده) برای نام تابع/نوع مدنظر ارزش دارد — این‌بار فقط `StorageValidation.kt` بررسی شد (طبق تصریح دستور کار)، نه کل `domain/storage/`.

## تصمیم ۲: `ShotReferenceData` — نوع ساده به‌جای `ShotEntity`/`domain.shot.Shot` کامل (پیش‌ازاین در `ReferentialIntegrity.kt` تصمیم‌گیری شده بود)

`validateReferentialIntegrity` موجود یک `List<ShotReferenceData>` می‌گیرد — نه `List<ShotEntity>` (وابستگی Room/data به یک تابع خالص دامنه) و نه `List<Shot>` دامنه‌ی کامل (نیاز به Decode enum های `ShotGoal`/`ShotType`/`MotionLevel` غیرلازم). `ExportImportRepository` این استخراج را انجام می‌دهد: `sceneId` مستقیماً از `ShotEntityDto` (فیلد سطح Room، بدون Decode)؛ `characterIds`/`objectIds`/`locationIds` با Decode کردن `shotDataJson` به `ShotDto` موجود (نه `Shot` دامنه‌ی کامل — دقیقاً برای پرهیز از اعتبارسنجی enum غیرلازم).

## تصمیم ۴: ترتیب اجرا در `importProject` — یکپارچگی ارجاعی همیشه قبل از هر نوشتنی

`importProject` این ترتیب دقیق را رعایت می‌کند: (۱) خواندن فایل، (۲) `deserializeFullProject`، (۳) استخراج `ShotReferences`/`sceneIds`/`assetIds`، (۴) `validateReferentialIntegrity` — **فقط اگر لیست `issues` خالی باشد**، (۵) `restoreProjectFromSnapshot` فراخوانی می‌شود. اگر هر `IntegrityIssue` ای پیدا شود، `restoreProjectFromSnapshot` **اصلاً فراخوانی نمی‌شود** و `Result.failure` با پیام کامل (شامل `source`/`brokenReferenceTo`/`message` هر مورد، نه فقط «شکست») برگردانده می‌شود.

**درباره‌ی Atomicity:** `restoreProjectFromSnapshot` (از قدم قبل، ADR-023) در یک `@Transaction` واحد Room پیچیده نشده — نوشتن‌های آن ترتیبی و suspend جداگانه‌اند، نه یک تراکنش SQLite واحد. این قدم آن را تغییر نداد (خارج از Scope، طبق تصریح دستور کار «عمدتاً بازترکیب، نه کد جدید بزرگ»). آنچه این قدم واقعاً و با اطمینان کامل تضمین می‌کند، مسیر خطای «ارجاع شکسته» است: چون اعتبارسنجی همیشه *قبل* از هر فراخوانی `restoreProjectFromSnapshot` انجام می‌شود، برای این مسیر خاص خطا، تضمین «صفر نوشتن» با قطعیت کامل برقرار است — نه با تکیه بر Rollback سطح دیتابیس، بلکه چون کد نوشتن اصلاً اجرا نمی‌شود. تست‌ها این را مستقیماً تأیید می‌کنند (بعد از `importProject` ناموفق، `loadProject`/`loadScene`/`loadShot` هر سه `null` هستند).

**محدودیت شناخته‌شده‌ی باقی‌مانده (پذیرفته‌شده، نه رفع‌شده):** اگر `restoreProjectFromSnapshot` خودش در میانه‌ی نوشتن (بعد از عبور موفق از اعتبارسنجی) با یک خطای غیرمرتبط با یکپارچگی ارجاعی مواجه شود (مثلاً خطای دیسک)، داده‌ی نیمه‌نوشته ممکن است باقی بماند — همان ریسک پذیرفته‌شده‌ی ADR-023 برای `BackupManager.restoreFromBackup`، این قدم آن را نه بدتر و نه بهتر نکرد.

## تصمیم ۵: بازگرداندن `ProjectEntity` واقعی از DAO، نه از DTO

بعد از موفقیت `restoreProjectFromSnapshot`، `importProject` مقدار بازگشتی‌اش را با `projectDao.loadProject(projectId)` واقعی می‌سازد (نه با تبدیل مستقیم `snapshot.project` DTO به Entity) — این هم داده‌ی واقعاً نوشته‌شده را تأیید می‌کند (نه فقط آنچه قرار بود نوشته شود) و هم از دسترسی به تابع `private fun ProjectEntityDto.toEntity()` (که در فایل دیگری، `ProjectSnapshot.kt`، `private` سطح فایل است و از این فایل قابل‌دسترس نیست) بی‌نیاز می‌کند.

## تست

- **`ReferentialIntegrityTest.kt`** (از قبل موجود، دست‌نخورده) قبلاً دقیقاً همین سه سناریو را پوشش می‌داد (سالم، Scene نامعتبر، Asset نامعتبر) — این قدم چیزی به آن اضافه نکرد چون نیازی نبود.
- **`ExportImportRepositoryTest.kt`** (۴ تست جدید، Robolectric + `FakeBackupFileStorage` بازاستفاده‌شده از `BackupManagerTest.kt`): `exportProject` یک Snapshot کامل و معتبر با نام فایل صحیح می‌نویسد؛ `importProject` با فایل سالم یک Round-Trip کامل بین دو `AppDatabase` جدا انجام می‌دهد؛ `importProject` با یک Shot دارای `sceneId` جعلی شکست می‌خورد و هیچ داده‌ای نوشته نمی‌شود؛ `importProject` با یک Shot دارای Asset ID جعلی هم همین‌طور.

هر ۴ تست جدید در همان اجرای دوم موفق شدند (اجرای اول با خطای کامپایل Redeclaration شکست خورد — بالا؛ بعد از رفع، کامپایل و تست هر دو موفق).

## خارج از Scope (بدون تغییر)

- UI/ViewModel و Storage Access Framework واقعی (`Intent.ACTION_OPEN_DOCUMENT`) — واحد ۱۶؛ `fileUri` در این قدم فقط یک `String` (مسیر از `BackupFileStorage`) است.
- Migration واقعی Schema قدیمی‌تر — `validateSchemaVersion` (از قبل موجود) فقط Warning می‌دهد؛ منطق Migration واقعی طبق تصریح بلوپرینت قابلیتی جدا و بزرگ‌تر است.
- تغییر `BackupFileStorage`/`FullProjectSnapshot`/`serializeFullProject`/`restoreProjectFromSnapshot`/`ReferentialIntegrity.kt` — همه عیناً بازاستفاده شدند، هیچ‌کدام تغییر نکردند.
- `domain/storage/StorageValidation.kt` — دستور کار این فایل را در Scope گذاشته بود، اما طبق یافته‌ی بالا هیچ افزودنی در آن لازم نشد؛ کاملاً دست‌نخورده ماند.

## Consequences

- **آسان می‌شود:** واحد ۱۵ اکنون **هیچ مورد «فقط امضا»ی باقی‌مانده‌ای از ADR-017 ندارد** — `AutoSaveManager` (ADR-022)، `BackupManager` (ADR-023)، و اکنون `Export/Import` (این ADR) هرسه پیاده‌سازی واقعی و تست‌شده دارند.
- **بدهی مستند باقی‌مانده:** عدم Wrap شدن `restoreProjectFromSnapshot` در یک `@Transaction` Room واحد (بالا)؛ Migration واقعی Schema قدیمی‌تر هنوز پیاده نشده؛ UI انتخاب فایل واقعی (واحد ۱۶).
