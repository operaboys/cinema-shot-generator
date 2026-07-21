# ADR-023: تصمیمات پیاده‌سازی واحد ۱۵ — تکمیل `BackupManager`

**تاریخ:** 2026-07-21
**وضعیت:** تکمیل دومین و آخرین کلاسی که فقط امضا داشت (طبق ADR-017/022)؛ Export/Import قدم بعدی جداست.

## Context

`BackupManager(projectId, backupIntervalMinutes, maxBackupsToKeep)` با `createBackup()`/`restoreFromBackup()` در قدم اول واحد ۱۵ فقط امضای TODO بود. کد مفهومی بلوپرینت ۱۵ به `serializeFullProject`/`writeFileToDevice`/`listBackupsForProject`/`deleteBackupFile`/`loadBackup`/`restoreProjectFromSnapshot` ارجاع می‌دهد — همه بدون بدنه، برخی (فایل‌های دستگاه) اصلاً به Room ربطی ندارند.

## تصمیم ۱: انتزاع I/O فایل پشت `BackupFileStorage`

`writeFileToDevice`/`readFileFromDevice`/`listBackupsForProject`/`deleteBackupFile` به `android.content.Context`/`java.io.File` واقعی وابسته‌اند — برخلاف بقیه‌ی این واحد که فقط Room بود. طبق پیشنهاد صریح دستور کار، این چهار عملیات پشت `BackupFileStorage` (`writeFile`/`readFile`/`listFiles`/`deleteFile`) قرار گرفتند؛ `DeviceBackupFileStorage` (در همان فایل `BackupFileStorage.kt`) با `context.filesDir/backups` پیاده‌سازی واقعی است؛ `FakeBackupFileStorage` (در `BackupManagerTest.kt`، Map درون‌حافظه) برای تست بدون I/O واقعی دستگاه. این دقیقاً همان دلیلی است که `AppDatabase.getInstance(context)` هم `Context` را از بیرون تزریق می‌کند، نه Singleton داخلی.

`BackupFileInfo(path, createdAt)` جایگزین ساختار نام‌برده‌ولی‌تعریف‌نشده‌ی بلوپرینت شد؛ در `DeviceBackupFileStorage`، `createdAt` از `File.lastModified()` واقعی گرفته می‌شود (نه یک فیلد جعلی)؛ در `FakeBackupFileStorage`، از یک `clock: () -> String` تزریقی گرفته می‌شود — دقیقاً هم‌الگو با `clock` تزریقی `AutoSaveManager`/`VersioningRepository`.

## تصمیم ۲: بدون Timer/Debounce واقعی — عیناً هم‌الگو با ADR-022

`backupIntervalMinutes` («هر ۵ دقیقه») همان استدلال `AutoSaveManager` را دارد: زمان‌بندی واقعی به Lifecycle اپ (ViewModel/UI، واحد ۱۶) وابسته است، نه به این لایه. `backupIntervalMinutes` به‌عنوان `val` عمومی (نه `private`) نگه داشته شد تا لایه‌ی UI آینده بتواند بخواندش، بدون این‌که خودِ `BackupManager` Timer را اجرا کند.

## تصمیم ۳: تعریف «پروژه‌ی کامل» برای `serializeFullProject`/`FullProjectSnapshot`

دستور کار صریحاً تعریف کرد: «ProjectEntity + تمام Scene/Shot/Asset/ProjectDna/AudioContext مرتبط با آن projectId». با grep در همه‌ی `data/dao/*.kt` (۱۱ DAO) این فهرست تأیید و تکمیل شد؛ سه گروه دیگر **عمداً خارج از Snapshot ماندند**:

1. **`PromptBlueprintEntity`/`RenderedOutputEntity`** — این‌ها خروجی تولیدشده‌اند (نتیجه‌ی Pipeline واحد ۱۱→۱۴)، نه «حالت منبع» پروژه؛ از همان Scene/Shot/Asset/ProjectDna موجود در Snapshot دوباره قابل‌تولیدند. فهرست صریح دستور کار هم آن‌ها را نام نبرد.
2. **`OverrideEntity`/`VersionEntity`/`EventLogEntity`** — هر سه `entityId: String` Polymorphic دارند (می‌تواند به Scene، Shot، Asset یا هر چیز دیگری اشاره کند). DAO های موجودشان فقط کوئری «طبق یک entityId مشخص» دارند (`getOverridesForEntity(entityId)` و مشابه)؛ هیچ کوئری «همه‌ی X های متعلق به یک پروژه» وجود ندارد و ساختن چنین کوئری‌ای (که نیاز به دانستن تمام entityId های ممکن یک پروژه دارد) خارج از Scope همین قدم است. این‌ها تاریخچه/Audit اند، نه بخشی از حالت فعلی محتوای خلاقانه‌ی پروژه.
3. **`DependencyEdgeEntity`** — اصلاً فیلد `projectId` ندارد (`sourceId`/`targetId`/`type` عمومی)؛ یک گراف وابستگی سراسری است، نه محدود به یک پروژه؛ مفهوماً «داده‌ی این پروژه» نیست.

این یک تصمیم Scope مستدل بود، نه یک ابهام معماری — طبق پیش‌تأیید صریح دستور کار («اگر معماری بهتری به ذهنت می‌رسد ... با آن پیش برو و در گزارش دلیلش را بگو»)، بدون توقف ادامه یافت.

## تصمیم ۴: DTO های محلی `@Serializable` به‌جای Annotate کردن مستقیم Entity های Room

`ProjectEntityDto`/`SceneEntityDto`/... در `ProjectSnapshot.kt` دقیقاً هم‌شکل Entity های Room متناظرشان‌اند، اما جدا تعریف شدند (نه افزودن `@Serializable` مستقیم روی خودِ `data/entity/*.kt`). دلیل: (۱) دستور کار صریحاً گفت Entity های موجود را تغییر ندهم مگر واقعاً لازم باشد — افزودن `@Serializable` تغییر در فایل‌های خارج از Scope این قدم بود؛ (۲) این دقیقاً همان جداسازی‌ای است که پروژه از قبل بین Domain و Serialization دارد (`ShotDto`/`SceneDto` و غیره در `data/repository/`) — همان الگو اینجا بین Entity Room و DTO سریالایز اعمال شد.

## تصمیم ۵: `restoreFromBackup` — یافتن فایل با `backupId` از طریق `listFiles` + پسوند نام فایل

`BackupFileStorage` متد «resolve مستقیم از backupId به path» ندارد (طبق Interface پیشنهادی دستور کار). `restoreFromBackup` با `listFiles(backupPrefix())` تمام بک‌آپ‌های همین پروژه را می‌گیرد و فایلی که نامش دقیقاً با `backup_${projectId}_${backupId}.json` تمام می‌شود را پیدا می‌کند — معادل دقیق `loadBackup(backupId)` بلوپرینت، بدون نیاز به متد جدید در Interface.

## تصمیم ۶: ترتیب نوشتن در `restoreProjectFromSnapshot` — والد قبل از فرزند (طبق هشدار صریح ADR-017)

`ADR-017` به‌صراحت هشدار داده بود: «هر عملیات چندجدولی که یک ردیف والد را با `OnConflictStrategy.REPLACE` می‌نویسد، باید *قبل* از درج/به‌روزرسانی فرزندان همان ردیف انجام شود». چون Restore ممکن است روی یک دیتابیس با داده‌ی از قبل موجود اجرا شود (نه فقط خالی)، ترتیب دقیقاً رعایت شد: `Project → Scene → Shot → AudioContext → Asset → ProjectDna`. اگر `Project`/`Scene` از قبل با همان کلید وجود داشته باشند، `REPLACE` آن‌ها (که SQLite با حذف+درج پیاده می‌کند) هر Cascade Delete احتمالی روی فرزندان قدیمی را *قبل* از نوشتن فرزندان جدید انجام می‌دهد — نه بعد از آن.

## ⚠️ دو باگ واقعی کشف‌شده و رفع‌شده در بازبینی خصمانه (adversarial-verify)

پیش از اعلام «تمام شد»، پیاده‌سازی اولیه با فرض «خراب است» بازبینی شد (نه فقط تکیه بر عبور تست‌های نوشته‌شده). دو باگ واقعی پیدا شد:

1. **تصادم پیشوند بین دو پروژه:** `backupPrefix() = "backup_${projectId}_"` وقتی یک `projectId` خودش پیشوند رشته‌ای `projectId` دیگری باشد (مثلاً `"p1"` و `"p1_v2"`) درست کار نمی‌کند — چون `"backup_p1_v2_....json"` واقعاً با `"backup_p1_"` شروع می‌شود. نتیجه: `cleanOldBackups` پروژه‌ی `"p1"` می‌توانست بک‌آپ پروژه‌ی `"p1_v2"` را هم در فهرست ببیند و — اگر مجموع تعداد از `maxBackupsToKeep` بیشتر می‌شد — واقعاً حذفش کند (از دست رفتن سکوت‌آمیز داده‌ی یک پروژه‌ی کاملاً دیگر). **رفع:** طول `projectId` به‌صورت صریح در پیشوند رمزگذاری شد: `"backup_${projectId.length}_${projectId}_"` — چون دو `projectId` متفاوت (مگر این‌که واقعاً یکسان باشند) نمی‌توانند هم طول یکسان داشته باشند هم همان تعداد کاراکتر بلافاصله بعد از عدد طول را یکسان داشته باشند، این رمزگذاری تصادم را ساختاری غیرممکن می‌کند.
2. **تطبیق سست‌تر از لازم در `restoreFromBackup`:** تطبیق فایل با `path.endsWith(expectedFileName)` بود، نه برابری دقیق نام فایل — در تئوری اگر دو `backupId` در همان پروژه به‌گونه‌ای بودند که یکی پسوند رشته‌ای دیگری باشد، `restoreFromBackup` می‌توانست بک‌آپ اشتباه را بازیابی کند. **رفع:** تطبیق به برابری دقیق `path.substringAfterLast('/') == expectedFileName` تغییر کرد.

یک تست رگرسیون جدید (`cleanOldBackups does not cross-contaminate a different project whose id is a string prefix of this one`) دقیقاً سناریوی باگ ۱ را بازتولید می‌کند: دو `BackupManager` با `projectId = "p1"` و `"p1_v2"` روی یک `FakeBackupFileStorage` مشترک؛ تأیید می‌شود `cleanOldBackups` پروژه‌ی `"p1"` بک‌آپ پروژه‌ی دیگر را پاک نمی‌کند.

**محدودیت شناخته‌شده باقی‌مانده (رفع نشد، مستند شد):** `DeviceBackupFileStorage.listFiles` مقدار `createdAt` را از `File.lastModified()` (دقت میلی‌ثانیه) می‌گیرد. اگر چند `createBackup()` واقعاً در یک میلی‌ثانیه‌ی یکسان روی دستگاه واقعی اجرا شوند، ترتیب `sortedByDescending(createdAt)` برای رکوردهای هم‌زمان به ترتیب برگشتی `File.listFiles()` (وابسته به فایل‌سیستم، نه لزوماً ترتیب ایجاد) وابسته می‌شود. با توجه به بازه‌ی طراحی‌شده‌ی ۵ دقیقه‌ای Backup، این ریسک عملاً بسیار کم‌احتمال است — رفعش نیازمند افزودن یک شماره‌ی ترتیبی Monotonic به `BackupFileInfo` است که خارج از Scope این قدم دانسته شد.

## تست: پنج سناریوی end-to-end (Robolectric + `FakeBackupFileStorage`)

- `createBackup` یک پروژه‌ی کامل (۲ Scene، ۲ Shot، ۱ Asset، ۱ ProjectDna، ۱ AudioContext) → JSON واقعی معتبر و کامل (با `deserializeFullProject` بررسی شد، نه فقط عدم-کرش).
- `cleanOldBackups`: ۵ بک‌آپ با `maxBackupsToKeep=3` → دقیقاً ۳ جدیدترین باقی می‌مانند (با `clock` شمارنده‌ی تزریقی، ترتیب قطعی).
- `restoreFromBackup`: Round-Trip کامل بین دو نمونه‌ی جدای `AppDatabase` (نه فقط همان دیتابیس) — دقیقاً معادل «ذخیره → Backup → پاک‌کردن دیتابیس → Restore → بررسی یکسانی».
- `restoreFromBackup` با `backupId` ناموجود → `Result.failure`.
- `cleanOldBackups` بین دو پروژه با `projectId` پیشوند-هم‌پوشان (`"p1"`/`"p1_v2"`) تداخل نمی‌کند (رگرسیون باگ ۱ بالا).

چهار تست اول در همان اجرای اول موفق شدند؛ دو باگ بالا با بازبینی خصمانه (نه با شکست تست) پیدا شدند — تست پنجم بعد از رفع باگ نوشته شد تا رگرسیون آینده را بگیرد.

## خارج از Scope (بدون تغییر)

- Export/Import (`exportProject`/`importProject`) — قدم بعدی جداگانه؛ شباهت زیاد به `createBackup`/`restoreFromBackup` دارد اما برای انتقال دستی با Storage Access Framework طراحی شده، نه Backup داخلی خودکار.
- ViewModel/UI که `createBackup`/`restoreFromBackup` را دوره‌ای یا با تعامل کاربر فراخوانی کند — واحد ۱۶.
- Entity/DAO موجود دست‌نخورده ماندند.

## Consequences

- **آسان می‌شود:** واحد ۱۵ اکنون هر دو کلاس «فقط امضا»ی باقی‌مانده (`AutoSaveManager`، `BackupManager`) را با پیاده‌سازی واقعی و تست‌شده دارد. `serializeFullProject`/`deserializeFullProject`/`restoreProjectFromSnapshot` توابع سطح‌فایل عمومی‌اند و قدم بعدی (Export/Import) می‌تواند مستقیماً از همین‌ها بازاستفاده کند.
- **بدهی مستند باقی‌مانده:** `Override`/`Version`/`EventLog` در Backup لحاظ نمی‌شوند (تاریخچه/Audit، نه حالت پروژه) — اگر در آینده نیاز به بازیابی تاریخچه‌ی کامل باشد، به یک کوئری DAO جدید «همه‌ی X های یک پروژه» نیاز دارد؛ Export/Import هنوز پیاده نشده.
