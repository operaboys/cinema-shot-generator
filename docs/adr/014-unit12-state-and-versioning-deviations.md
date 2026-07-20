# ADR-014: تصمیمات پیاده‌سازی واحد ۱۲ (State & Versioning) — لایه‌ی دامنه

**تاریخ:** 2026-07-20
**وضعیت:** تمام تصمیمات مستقل (طبق دستور کار: جزئیات پیاده‌سازی محلی، نه سؤال معماری)

## Context

پیاده‌سازی لایه‌ی دامنه‌ی خالص واحد ۱۲ (State & Versioning) طبق `docs/blueprints/12-state-and-versioning.md`. این واحد به Room (واحد ۱۵، هنوز پیاده نشده) و Dependency Resolver (واحد ۰۷) وابسته است؛ طبق دستور کار، تمام I/O واقعی (loadVersion، createVersionFromSnapshot، logEvent، findDependents) با پارامتر تزریق‌پذیر جایگزین شد — الگویی که ADR-013 هم برای `sceneDefault` به کار برد («پارامتر خارجی چون منبع واقعی داده هنوز وجود ندارد»).

## تصمیم مستقل ۱: چهار Rule جدول State Machine ← سه تابع (نه چهار)

جدول بلوپرینت دو Rule جدا فهرست کرده: «انتقال خارج از مسیر مجاز» و «انتقال به Final بدون عبور از Locked»؛ بخش پایانی «قوانین اعتبارسنجی» دوباره «انتقال وضعیت خارج از مسیر مجاز» را تکرار می‌کند. اما طبق خودِ `ALLOWED_TRANSITIONS`، تنها راه رسیدن به `FINAL` از `LOCKED` است — یعنی «رسیدن به Final بدون عبور از Locked» همیشه و فقط همان چیزی است که `canTransition` هم `false` برمی‌گرداند. هر سه‌ی این عبارت‌ها یک بررسی واحد هستند.

**تصمیم:** یک تابع مشترک `validateStateTransition(current, target): ValidationIssue?` نوشته شد (نه سه نسخه‌ی تکراری از همان منطق). دو Rule باقی‌مانده‌ی جدول (فیلدهای الزامی ناقص برای Review؛ وابستگی‌های غیر-Final برای رسیدن به Final) هرکدام تابع مستقل خودشان را دارند: `validateReviewReadiness`، `validateDependenciesFinalized`. جمعاً به‌همراه `validateEditPermission` (Rule جداگانه‌ی «ویرایش در Locked/Final بدون Override»)، ۴ تابع Rule مستقل و قابل‌تست پیاده شد.

## تصمیم مستقل ۲: نوع محلی `Entity`

هیچ نوع عمومی «Entity» که Project/Scene/Shot/Output را یکسان نمایندگی کند در هیچ‌جای پروژه وجود ندارد (تأیید شده با grep) — Scene/Shot/ProjectDna هرکدام نوع کاملاً مستقل خودشان را دارند، بدون فیلد `lock`/`state` مشترک. یک `data class Entity(id: String, type: String, lock: EntityLock)` محلی در `domain.stateversioning` تعریف شد — فقط همان فیلدهایی که کد مفهومی بلوپرینت واقعاً می‌خواند (`entity.id`, `entity.type`, `entity.lock`). این دقیقاً همان الگوی «تجمیع محلی حداقلی» است که در واحدهای قبلی (مثلاً `GlobalMoodBase` واحد ۰۲، `LightingSettings` اولیه‌ی واحد ۱۱) به کار رفته — نه یک Placeholder با فیلد حدسی، بلکه ترکیب دقیق همان چیزهایی که منطق واقعاً به آن‌ها نیاز دارد. اتصال واقعی این نوع به Scene/Shot/ProjectDna/Output واقعی (که هرکدام باید فیلد `lock`/`state` بگیرند یا از طریق یک Wrapper پوشش داده شوند)، تصمیم لایه‌ی Persistence (واحد ۱۵) است — این قدم آن تصمیم را نگرفت.

## تصمیم مستقل ۳: `StateVersioningEventLogger` جدید (نه بازاستفاده از `OverrideEventLogger` واحد ۰۱)

`domain.story.OverrideActions.kt` از قبل یک `fun interface OverrideEventLogger { fun log(event: OverrideEvent) }` + `NoOp` companion دارد، با کامنت صریح: «پیاده‌سازی واقعی ثبت رویداد متعلق به واحد ۱۲ است و بعداً همین Interface را پیاده می‌کند» (ADR-001). بررسی شد که آیا این Interface مستقیماً برای `logEvent` این واحد قابل بازاستفاده است.

**نتیجه:** بازاستفاده‌ی مستقیم مناسب نبود. `OverrideEvent(type, overrideId, scope: OverrideScope?)` برای رویداد Human Override طراحی شده — فیلد `overrideId` معنایی متفاوت از `entityId` بلوپرینت ۱۲ دارد و `scope: OverrideScope` (با `entityType`, `entityId`, `field`, `originalValue`, `overrideValue`) برای رویدادهای قفل Entity (که فقط `logEvent(eventType: String, entityId: String)` نیاز دارند) بیش‌ازحد اختصاصی و نامرتبط است. به‌جای شوهورن‌کردن یک Payload نامناسب، **الگوی** `OverrideEventLogger` (fun interface + `NoOp` companion) دوباره پیاده شد — نه خودِ نوع — با یک `fun interface StateVersioningEventLogger { fun log(eventType: String, entityId: String) }` جدید که دقیقاً امضای دو-رشته‌ای بلوپرینت ۱۲ را دارد. کامنت مشابهی که رابطه‌ی این دو Interface را توضیح می‌دهد اضافه شد.

**یادداشت باز:** ADR-001 پیش‌بینی کرده بود واحد ۱۲ خودِ `OverrideEventLogger` واحد ۰۱ را پیاده‌سازی می‌کند (یعنی زمانی که Room واقعی وجود داشته باشد، `OverrideEventLogger` واحد ۰۱ به تاریخچه‌ی رویداد واحد ۱۲ وصل می‌شود) — این هنوز درست است و تناقضی با تصمیم بالا ندارد؛ آن اتصال یک موضوع Wiring آینده (واحد ۱۵) است، نه اینکه `StateVersioningEventLogger` باید همان `OverrideEventLogger` باشد.

## تصمیم مستقل ۴: `rollbackToVersion` — حذف `loadVersion`، `Result<T>` همیشه موفق

امضای بلوپرینت (`rollbackToVersion(entityId, targetVersionId): Result<EntityVersion>`) داخلش `loadVersion(targetVersionId)` (I/O) را صدا می‌زند و اگر `null` بود Failure برمی‌گرداند. طبق دستور کار، `loadVersion` حذف شد و `targetVersion: EntityVersion` (غیر-nullable) مستقیماً پارامتر شد — یعنی مسیر «نسخه‌ی هدف یافت نشد» بلوپرینت اکنون مسئولیت لایه‌ی بارگذاری بیرونی (که این نسخه را قبل از فراخوانی این تابع پیدا می‌کند) است، نه این تابع.

`createSnapshot` دقیقاً هر سه آرگومان واقعی `createVersionFromSnapshot` بلوپرینت (`entityId`, `snapshotData`, `changeSummary`) را می‌گیرد — نه دو آرگومان که در پیشنهاد اولیه‌ی دستور کار آمده بود — برای وفاداری کامل به فراخوانی واقعی بلوپرینت؛ `entityId`/`snapshotData` از خودِ `targetVersion` گرفته می‌شوند (نیازی به پارامتر جدا نبود، چون `EntityVersion` خودش این فیلد را دارد).

امضای بازگشتی `Result<EntityVersion>` (طبق پیشنهاد اولیه‌ی دستور کار) نگه داشته شد، هرچند با این طراحی تزریق‌پذیر تابع همیشه `Result.success` برمی‌گرداند — `createSnapshot` تزریق‌شده امکان گزارش شکست را ندارد چون نوع بازگشتی‌اش `EntityVersion` غیر-Null است، نه `Result<EntityVersion>`. این عدم‌تقارن آگاهانه ثبت می‌شود؛ اگر معمار ترجیح دهد `createSnapshot` هم `Result<EntityVersion>` برگرداند (برای واقعاً قابل‌شکست بودن)، یک تغییر بعدی جزئی است.

## تصمیم مستقل ۵: `riskLevel: String` (نه enum) در `ImpactResult`

طبق کد مفهومی بلوپرینت که مقادیر رشته‌ای `"low"`/`"high"`/`"critical"` را صریحاً تعریف کرده (مشابه `LightingPreset` واحد ۰۸ که فیلدهای رشته‌ای‌اش عمداً enum نشدند)، `riskLevel` به همان شکل `String` باقی ماند — بدون ارتقای گرتویی به enum.

**نکته‌ی صریح طبق دستور کار:** این `riskLevel` («low»/«high»/«critical») هیچ ربطی به سطح‌بندی Low/Medium/High Risk در `docs/governance/change-management.md` ندارد — آن سطح‌بندی برای تصمیمات معماری پس از Architecture Lock است؛ این یکی برای ریسک نسخه‌بندی داده‌ی runtime یک Entity است. همچنین `VersionType` (`SAFE`/`RISKY`) این واحد یک مفهوم سومِ کاملاً مستقل است. این سه سیستم قاطی نشدند.

## تصمیم مستقل ۶: `analyzeImpact` واحد ۰۷ مستقیماً برای `findDependents` قابل‌استفاده نیست (بررسی شد، اتصال کامل گرفته نشد)

`domain.validation.analyzeImpact(changedNodeId: String, edges: List<DependencyEdge>): ImpactReport` (واحد ۰۷) ورودی‌اش کل گراف وابستگی از پیش‌ساخته‌شده (`List<DependencyEdge>`) است، در حالی‌که `findDependents` این واحد فقط یک `entityId` می‌گیرد و مستقیماً `List<String>` برمی‌گرداند — دو شکل ورودی/خروجی متفاوت. پس `analyzeImpact` نمی‌تواند مستقیم (drop-in) به‌جای پارامتر `findDependents` استفاده شود؛ برای اتصال واقعی، یک لایه‌ی میانی لازم است که گراف `edges` را از داده‌ی واقعی بسازد و سپس `analyzeImpact(entity.id, edges).directlyAffected` را به `findDependents` تبدیل کند. طبق دستور کار، این اتصال کامل گرفته نشد — فقط بررسی و در همین‌جا مستند شد.

## Consequences

- **آسان می‌شود:** State Machine، Lock، Versioning، و Impact Analysis همگی به‌صورت منطق خالص و کاملاً قابل‌تست (بدون I/O) آماده‌اند؛ واحد ۱۵ (Room) می‌تواند مستقیماً این توابع را با پیاده‌سازی‌های واقعی `createSnapshot`/`logEvent`/`findDependents` تزریق کند.
- **بدهی باقی‌مانده:** نوع محلی `Entity` هنوز به Scene/Shot/ProjectDna/Output واقعی وصل نیست؛ اتصال `findDependents` به `analyzeImpact` واحد ۰۷ هنوز طراحی نشده؛ `StateVersioningEventLogger` و `OverrideEventLogger` (واحد ۰۱) هنوز به یک تاریخچه‌ی رویداد واحد نیازمند سیم‌کشی واقعی‌اند (کار واحد ۱۵).
