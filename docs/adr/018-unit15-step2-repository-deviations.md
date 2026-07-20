# ADR-018: تصمیمات پیاده‌سازی واحد ۱۵ — قدم ۲ (اتصال واقعی Repository ها)

**تاریخ:** 2026-07-20
**وضعیت:** دو تصمیم معماری با تأیید صریح معمار + چند تصمیم پیاده‌سازی محلی + یک باگ واقعی کشف/رفع‌شده

## Context

قدم دوم و پایانیِ واحد ۱۵: اتصال واقعی توابع تزریق‌پذیر واحدهای ۰۵ (`resolveCameraSettings`/`resolveLightingSettings`/`resolveEnvironmentSettings`)، ۱۲ (`rollbackToVersion`, `StateVersioningEventLogger`, `analyzeVersionImpact`) به DAO های Room ساخته‌شده در قدم اول (ADR-017)، از طریق یک لایه‌ی جدید Repository در `data/repository/`.

## تصمیم معماری ۱ (پرسیده و تأییدشده): scope این قدم کوچک شد — `collectData` (PromptGenerationRepository) به قدم سوم موکول شد

بررسی دقیق نشان داد `PromptGenerationRepository.collectData` برای سریالایز واقعی به تقریباً ۶۰-۸۰ نوع در ۸ پکیج دامنه نیاز دارد: `ProjectDna` (~۱۵ نوع تو در تو)، `CharacterAsset`/`LocationAsset` (۱۴+ نوع فقط در `AssetModels.kt`)، `CameraSettings` (Sealed Class شش‌شاخه‌ای `CameraMovement` + ۷ enum)، `LightingSettings`/`EnvironmentSettings`، `AudioContext` — عملاً کل لایه‌ی دامنه. این فراتر از محدوده‌ی «فقط `data/repository/`» بود، چه با annotate کردن مستقیم دامنه (نیازمند تغییر ده‌ها فایل `domain/`، ممنوع طبق مرز صریح این قدم)، چه با DTO محلی (باز هم ده‌ها کلاس جدید فقط برای یک تابع).

**تصمیم معمار:** این قدم فقط `SettingsResolutionRepository` + `VersioningRepository` + `ImpactAnalysisRepository` را پوشش می‌دهد؛ `PromptGenerationRepository.collectData` به یک قدم سوم جداگانه موکول شد.

## تصمیم معماری ۲ (پرسیده و تأییدشده): افزودن `EventLogEntity`/`EventLogDao`

`StateVersioningEventLogger` (واحد ۱۲) برای ثبت واقعی رویداد به یک جدول نیاز دارد که در قدم اول ساخته نشده بود (خارج از فهرست آن قدم، ثبت‌شده به‌عنوان بدهی در ADR-017).

**تصمیم معمار:** یک Entity جدید اضافه شود.

`data/entity/EventLogEntity.kt` (`id` autoGenerate، `eventType`, `entityId`, `timestamp`؛ `Index("entityId")`، بدون ForeignKey چون `entityId` Polymorphic است — مشابه `OverrideEntity`/`VersionEntity`) + `data/dao/EventLogDao.kt` (`logEvent` suspend insert، `getEventsForEntity` Flow) اضافه شدند و به `AppDatabase` (entities + `abstract fun eventLogDao()`) وصل شدند. `version` همچنان `1` می‌ماند چون هیچ نسخه‌ای منتشر نشده.

## تصمیم مستقل: DTO محلی در `data/repository/` به‌جای `@Serializable` مستقیم روی `domain/`

برای `SettingsResolutionRepository` (که به سریالایز `Shot` + `CameraSettings` + `LightingSettings` + `EnvironmentSettings` نیاز دارد، ~۲۰-۲۵ نوع)، به‌جای افزودن `@Serializable` مستقیم روی فایل‌های `domain/shot`/`domain/camera`/`domain/sceneconditions` (که به‌معنای تغییر فایل‌های `domain/` بود، حتی اگر فقط یک Annotation باشد — خارج از مرز صریح این قدم: «فقط مصرف (import)»)، یک لایه‌ی DTO مستقل و کامل در `data/repository/` نوشته شد (`ShotDto.kt`, `CameraSettingsDto.kt`, `SceneConditionsDto.kt`) + توابع نگاشت دوطرفه (`DtoMappers.kt`، تنها فایلی که هم `data.repository` و هم `domain.*` را import می‌کند — دقیقاً جهت مجاز). Enum های دامنه در DTO ها به‌صورت `String` (نام Enum) ذخیره می‌شوند؛ نیازی به DTO Enum جداگانه نبود (کاهش قابل‌توجه تعداد کلاس‌ها). سه DTO مشخص (`SourcedCameraSettingsDto`/`SourcedLightingSettingsDto`/`SourcedEnvironmentSettingsDto`) به‌جای یک DTO Generic نوشته شدند، چون kotlinx.serialization برای انواع Generic نیاز به تزریق دستی Serializer در هر فراخوانی دارد — سه نوع غیر-Generic ساده‌تر بود.

## ⚠️ باگ واقعی کشف‌شده و رفع‌شده: تداخل نام فیلد `type` با Discriminator چندریختی

اولین اجرای تست `SettingsResolutionRepositoryTest` **شکست خورد**: `IllegalStateException: Sealed class 'basic' cannot be serialized as base class 'CameraMovementDto' because it has property name that conflicts with JSON class discriminator 'type'`.

**علت:** kotlinx.serialization برای Sealed Class ها به‌طور پیش‌فرض یک ستون Discriminator با نام `"type"` اضافه می‌کند تا زیرکلاس واقعی را در JSON مشخص کند. `CameraMovementDto.Basic` هم یک فیلد به نام `type` داشت (مطابق دامنه‌ی واقعی `CameraMovement.Basic.type: BasicMovementType`) — این دو تداخل مستقیم داشتند.

**رفع:** فیلد `CameraMovementDto.Basic.type` به `movementType` تغییر نام یافت (فقط در DTO؛ نوع دامنه‌ی واقعی `CameraMovement.Basic` دست‌نخورده ماند). مکان‌های نگاشت در `DtoMappers.kt` متناظر به‌روزرسانی شدند.

## تصمیم مستقل: `sceneDefault` همیشه `null` است در `SettingsResolutionRepository`

طبق ADR-013 (واحد ۰۵، قبلاً تصمیم‌گیری و مستندشده)، `Scene` هیچ فیلد پیش‌فرض camera/lighting/environment ندارد — این تصمیم قبلی است، نه ابهام جدید این قدم. `SettingsResolutionRepository` فقط از `SceneDao` برای تأیید *وجود* صحنه‌ی مرتبط با شات استفاده می‌کند (اگر صحنه حذف شده، `Result.failure`)، نه برای استخراج مقداری که اصلاً جایی برای ذخیره ندارد. معنای عملی: فقط شات‌هایی با `source="override"` و مقدار override واقعی قابل‌حل‌شدن‌اند — این رفتار صادقانه‌ی معماری فعلی است (تست شده)، نه نقص این قدم.

## تصمیم مستقل: افزودن سه فیلد به `VersionEntity` (`changeSummary`, `modifiedFieldsJson`, `parentVersionId`)

هنگام نوشتن نگاشت `EntityVersion ↔ VersionEntity`، مشخص شد `VersionEntity` قدم ۱ (که دقیقاً طبق نمونه‌ی ناقص خودِ بلوپرینت ساخته شده بود) فاقد این سه فیلد است، در حالی‌که `domain.stateversioning.EntityVersion` هرسه را دارد — یعنی بدون این افزودن، این داده‌ها حین ذخیره‌سازی بی‌صدا گم می‌شدند. این سه فیلد (با مقدار پیش‌فرض، Backward-Compatible) اضافه شدند — همان‌جنس تصمیم افزودن `EventLogEntity` که معمار تازه تأیید کرده بود (تغییر Schema در قدم ۲ مجاز است، چون `version` هنوز `1` و منتشرنشده است)؛ بنابراین دور سوم پرسش لازم دیده نشد — این یک پیامد طبیعی و کوچک‌تر از همان تصمیم تأییدشده بود، نه یک انشعاب معماری جدید.

## تصمیم مستقل: الگوی «Buffer-then-persist» برای Callback های Synchronous

`createSnapshot`/`logEvent` (پارامترهای `rollbackToVersion`) و `findDependents` (پارامتر `analyzeVersionImpact`) همگی **Synchronous** هستند (نه `suspend`)، در حالی‌که هر نوشتن/خواندن واقعی در Room `suspend` است. به‌جای `runBlocking` (بلاک‌کردن نامناسب Thread درون یک Callback) یا `GlobalScope.launch` (Concurrency بی‌ساختار، بدون زیرساخت DI/Scope در این پروژه)، یک الگوی یکنواخت اعمال شد:
- **برای خواندن (findDependents):** تمام داده‌ی لازم از قبل (Prefetch) با یک فراخوان `suspend` واحد خوانده می‌شود (`DependencyEdgeDao.getAllEdges()`)؛ خودِ Callback فقط فیلتر خالص در حافظه انجام می‌دهد.
- **برای نوشتن (createSnapshot/logEvent):** Callback فقط یک شیء جدید در حافظه می‌سازد یا در یک لیست موقت جمع می‌کند؛ نوشتن واقعی در `VersionDao`/`EventLogDao` بعد از بازگشت `rollbackToVersion` (خارج از آن دو Callback) با `suspend` انجام می‌شود.

این الگو `VersioningRepository` و `ImpactAnalysisRepository` را به‌طور یکسان اداره می‌کند و در هر دو مستند شده است.

## تصمیم مستقل: `VersionType.SAFE` پیش‌فرض در `createSnapshot` تزریق‌شده

`createSnapshot` تزریق‌شده در `rollbackToVersion` پارامتر `modifiedFields` دریافت نمی‌کند (امضای بلوپرینت چنین چیزی نمی‌دهد)، پس `determineVersionType` (که به `modifiedFields` نیاز دارد) قابل‌فراخوانی نیست. `VersionType.SAFE` به‌عنوان پیش‌فرض محافظه‌کارانه انتخاب شد — منطق: Rollback بازگشت به وضعیت قبلی است (نه یک تغییر ساختاری جدید)، پس فرض SAFE معقول‌تر از حدس RISKY بدون داده است.

## خارج از Scope (طبق دستور کار، بدون تغییر)

`AutoSaveManager`/`BackupManager`/Export-Import و `PromptGenerationRepository.collectData` (موکول به قدم سوم) پیاده نشدند.

## Consequences

- **آسان می‌شود:** واحد ۱۵ اکنون (به‌جز `collectData`) کاملاً به دامنه وصل است؛ `resolveCameraSettings`/`resolveLightingSettings`/`resolveEnvironmentSettings`، `rollbackToVersion` (با `createSnapshot`/`logEvent` واقعی)، و `analyzeVersionImpact` (با `findDependents` واقعی) همگی روی داده‌ی واقعی Room تست شده‌اند.
- **بدهی باقی‌مانده:** `PromptGenerationRepository.collectData` هنوز پیاده نشده (قدم سوم)؛ `Scene` هنوز فیلد پیش‌فرض camera/lighting/environment ندارد (تصمیم قدیمی، هنوز حل‌نشده)؛ `AutoSaveManager`/`BackupManager`/Export-Import هنوز فقط امضا دارند.
