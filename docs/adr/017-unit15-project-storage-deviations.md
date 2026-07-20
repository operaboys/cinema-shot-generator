# ADR-017: تصمیمات پیاده‌سازی واحد ۱۵ — قدم ۱ (Project Storage / Room: Entity+DAO+Database)

**تاریخ:** 2026-07-20
**وضعیت:** تمام تصمیمات مستقل + یک باگ واقعی کشف و رفع شد (نه فقط تصمیم پیاده‌سازی)

## Context

قدم اول از دو قدم واحد ۱۵: فقط Room (Entity/DAO/Database) — **بدون** هیچ اتصال واقعی به توابع تزریق‌پذیر واحدهای ۰۵ (`resolveCameraSettings`/...)، ۱۱ (`collectData`)، ۱۲ (`createSnapshot`/`logEvent`/`findDependents`). این مرز به‌طور کامل رعایت شد — `data/` هیچ import ای از `domain/shot`، `domain/promptengine`، `domain/stateversioning` ندارد (تأیید شده با grep، صفر نتیجه).

## تنظیمات Gradle

`Room 2.8.4` و `KSP 2.3.10` از قبل در `libs.versions.toml` اعلام شده بودند («Stack مصوب») ولی به هیچ ماژولی وصل نشده بودند. فقط وصل شدند:
- افزودن `alias(libs.plugins.ksp)` به `app/build.gradle.kts`.
- افزودن `room-runtime`, `room-ktx` (implementation) و `room-compiler` (`ksp(...)`, نه kapt — طبق تصریح بلوپرینت).
- `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` — برای Export واقعی Schema JSON (پیش‌نیاز استاندارد Room برای Migration های آینده؛ فایل `app/schemas/.../1.json` تولید شد). این خودِ Migration نیست، فقط جلوگیری از هشدار کامپایلر Room است.

**نتیجه‌ی سازگاری نسخه:** هیچ ناسازگاری واقعی بین Kotlin 2.2.21 و KSP 2.3.10 مشاهده نشد — `gradle :app:assembleDebug` (فقط با پلاگین KSP فعال، پیش از نوشتن هیچ Entity ای) قبل از شروع کدنویسی واقعی اجرا و تأیید شد (BUILD SUCCESSFUL)، دقیقاً طبق دستور کار («پیش از هر ابهام، تلاش کن نسخه‌ها را واقعاً بسازی، نه فرض کنی»).

## `DependencyEdgeEntity`: بازتعریف مستقل، نه Import از Domain

بلوپرینت فقط اسم این Entity را آورده، بدون فیلد. طبق دستور صریح این قدم، شکل آن از نوع واقعی `domain.validation.DependencyResolver.DependencyEdge` (`sourceId`, `targetId`, `type: DependencyType{STRONG,WEAK,REFERENCE}`) الگو گرفته شد، اما **بازتعریف مستقل** شد — بدون import از `domain/` — چون Entity های Room مدل ذخیره‌سازی‌اند، نه مدل دامنه؛ لایه‌ی `data/` نباید به `domain/` وابسته باشد (این استقلال با ابهامی روبه‌رو نشد، پس نیازی به توقف/پرسش نبود — تصمیم مستقل، دقیقاً طبق دستور). `type` به‌صورت `String` ذخیره شد (نه enum Room-native)، هم‌راستا با الگوی `versionType`/`assetType` در بقیه‌ی Entity ها. `id: Long` با `autoGenerate=true` به‌عنوان کلید اصلی انتخاب شد (به‌جای کلید ترکیبی)، چون ساده‌تر و بدون محدودیت غیرضروری روی تکرار یال‌ها.

## Index/FK های اضافه‌شده فراتر از نمونه‌ی کد بلوپرینت (تصمیمات فنی جزئی)

بلوپرینت برای `AssetEntity` و `PromptBlueprintEntity` هیچ `Index`ای نشان نداده بود، با این‌که بخش «مقیاس‌پذیری» همان سند صریحاً می‌گوید کوئری‌ها روی این جداول باید Index مناسب داشته باشند (Asset Library تا ۱۰,۰۰۰+ آیتم). `Index("projectId")` روی `AssetEntity` و `Index("shotId")` روی `PromptBlueprintEntity` اضافه شدند — بدون `ForeignKey` (بلوپرینت هم برایشان FK نخواسته بود). همچنین `Index("promptBlueprintId")` به `RenderedOutputEntity` اضافه شد تا هشدار استاندارد Room («FK بدون Index ممکن است باعث Full Table Scan شود») رخ ندهد — یک بهینه‌سازی فنی، نه تغییر مدل داده.

## `domain/storage/`: پارامترهای ساده به‌جای Entity/نوع تجمیعی نامشخص

بلوپرینت `validateReferentialIntegrity(project: ProjectData)` را با یک نوع `ProjectData` نوشته که هیچ‌جای بلوپرینت تعریف نشده. طبق دستور صریح این قدم، این تابع با پارامترهای ساده بازنویسی شد:
```kotlin
fun validateReferentialIntegrity(shots: List<ShotReferenceData>, sceneIds: Set<String>, assetIds: Set<String>): List<IntegrityIssue>
```
`ShotReferenceData` فقط همان ۵ فیلدی را دارد که منطق واقعاً می‌خواند (`shotId`, `sceneId`, `characterIds`, `objectIds`, `locationIds`) — نه یک Shot/ShotEntity کامل. این تابع بدون Room کاملاً قابل تست است (تأیید شده — تست‌های این فایل به Robolectric نیازی ندارند).

**`validateReferentialIntegrityAsIssues`** یک Wrapper نازک اضافه است که همان نتایج را به `ValidationIssue`/`Severity` سراسری واحد ۰۷ نگاشت می‌کند — چون دستور کار هم «طبق کد مفهومی بلوپرینت» (شکل `IntegrityIssue`) و هم «قوانین جدول را با ValidationIssue سراسری پیاده کن» را همزمان خواسته بود. `validateReferentialIntegrity` خودش دست‌نخورده ماند (شکل غنی‌تر `IntegrityIssue`: source/brokenReferenceTo/message حفظ شد)؛ فقط یک تابع نگاشت جدا اضافه شد — هر دو خواسته بدون تناقض برآورده شدند.

سه Rule دیگر جدول (`validateProjectId`, `validateStorageAvailable`, `validateSchemaVersion`) در `StorageValidation.kt` با `ValidationIssue` نوشته شدند. `validateStorageAvailable` یک `Boolean` تزریق‌پذیر می‌گیرد (نه خودش `File.usableSpace` را چک می‌کند) — بررسی فضای دیسک یک API پلتفرمی است، نه منطق دامنه‌ی خالص؛ دقیقاً هم‌الگو با `findDependents`/`logEvent` تزریق‌پذیر واحد ۱۲.

## انتخاب تست: Robolectric (نه instrumented androidTest)

این محیط Sandbox به امولاتور/دستگاه اندروید واقعی دسترسی ندارد (محدودیت شناخته‌شده‌ی همین پروژه، مشابه محدودیت‌های قبلی gradle wrapper) — پس `androidTest` اصلاً قابل اجرا نبود. **Robolectric** انتخاب شد چون کلاس‌های فریمورک اندروید (از جمله SQLite) را روی JVM خالص شبیه‌سازی می‌کند و با همان `gradle :app:testDebugUnitTest` معمولی اجرا می‌شود. یک دیتابیس Room واقعی و درون‌حافظه‌ای (`Room.inMemoryDatabaseBuilder`) در تست‌ها ساخته می‌شود — نه Mock؛ FK constraint ها هم واقعاً اعمال می‌شوند (دقیقاً همین باعث کشف باگ زیر شد). `@Config(sdk = [34])` یک نسخه‌ی SDK پایدار و پشتیبانی‌شده توسط Robolectric را هدف گرفته (نه `compileSdk=36` پروژه)، فقط برای پایداری تست — ربطی به `minSdk`/`targetSdk` واقعی اپ ندارد.

چهار DAO کلیدی تست شدند: `ProjectDao` (CRUD کامل + ترتیب Flow)، `SceneDao` (FK به Project)، `ShotDao` (نسخه‌ی Paged)، `ProjectTransactionDao` (Atomicity). بقیه‌ی DAO ها (Asset/PromptBlueprint/RenderedOutput/Override/Version/DependencyEdge) الگوی یکسانی دارند و تست نشدند — طبق دستور صریح «حداقل چند DAO کلیدی».

## ⚠️ باگ واقعی کشف‌شده و رفع‌شده: ترتیب `saveShotWithSceneUpdate`

تست اول `ProjectTransactionDao` (با ترتیب دقیق کد مفهومی بلوپرینت: `saveShot` سپس `saveScene`) **شکست خورد** — `loadShot("shot_001")` بعد از تراکنش `null` برمی‌گرداند، بدون هیچ Exception ای.

**علت واقعی:** `SceneEntity` با `OnConflictStrategy.REPLACE` ذخیره می‌شود. وقتی یک ردیف با همان Primary Key از قبل وجود دارد، SQLite استراتژی REPLACE را با **حذف ردیف قدیمی و درج ردیف جدید** پیاده می‌کند (نه UPDATE درجا). چون `ShotEntity.sceneId` یک `ForeignKey` با `onDelete = CASCADE` به `SceneEntity.sceneId` دارد، این حذف داخلی به‌طور خودکار تمام Shot های آن Scene را هم حذف می‌کند — از جمله `shot_001` که همان لحظه، در همان تراکنش، تازه درج شده بود.

**رفع:** ترتیب در `ProjectTransactionDao.saveShotWithSceneUpdate` به `saveScene` سپس `saveShot` تغییر کرد — یعنی REPLACE احتمالی روی Scene (و Cascade Delete ناشی از آن) *قبل* از درج Shot جدید اتفاق می‌افتد، نه بعد از آن.

**چرا این یک تصمیم پیاده‌سازی محلی است، نه سؤال معماری:** این یک باگ واقعی و قابل‌بازتولید بود (با تست کشف شد، نه حدس)؛ راه‌حل (تعویض ترتیب دو خط) کاملاً درون‌محدوده‌ی همین تابع است، هیچ تصمیم معماری جدیدی نمی‌طلبد، و نیت اصلی بلوپرینت (ذخیره‌ی Atomic هر دو Entity) را کاملاً حفظ می‌کند.

**هشدار برای آینده (قدم دوم این واحد، یا هر `@Transaction` جدید روی این Schema):** هر عملیات چندجدولی که یک ردیف والد را با `OnConflictStrategy.REPLACE` می‌نویسد، باید *قبل* از درج/به‌روزرسانی فرزندان همان ردیف انجام شود، وگرنه Cascade Delete داخلی SQLite می‌تواند فرزندان تازه‌نوشته‌شده را در همان تراکنش پاک کند. این یک ریسک سیستمی است که باید در قدم دوم (اتصال واقعی State & Versioning) دوباره در نظر گرفته شود.

## خارج از Scope (فقط یادداشت TODO، بدون کد واقعی — طبق دستور کار)

- `AutoSaveManager(projectDao: ProjectDao, intervalSeconds: Long = 30)` با `suspend fun saveIfDirty(project, isDirty)`.
- `BackupManager(projectId, backupIntervalMinutes, maxBackupsToKeep)` با `createBackup()`/`restoreFromBackup()`.
- `suspend fun exportProject(projectId): Result<String>` / `suspend fun importProject(fileUri): Result<ProjectEntity>`.

این‌ها به DAO های همین قدم وابسته‌اند اما منطق سطح بالاتری‌اند (فایل/دستگاه) — کد واقعی‌شان ساخته نشد، فقط این یادداشت (و در گزارش نهایی) ثبت شد.

## Consequences

- **آسان می‌شود:** واحد ۱۵ اکنون یک لایه‌ی Persistence واقعی و تست‌شده دارد؛ قدم دوم (اتصال resolve*/collectData/createSnapshot/logEvent/findDependents) می‌تواند مستقیماً روی این DAO ها بنا شود.
- **بدهی باقی‌مانده:** هیچ اتصالی به دامنه هنوز برقرار نیست (عمدی، قدم دوم)؛ `AutoSaveManager`/`BackupManager`/Export-Import پیاده نشدند؛ فقط ۴ از ۹ DAO با Robolectric تست شدند (بقیه هم‌الگو، تست نشدند)؛ ریسک ترتیب REPLACE+CASCADE (بالا) باید در قدم دوم دوباره یادآوری شود.
