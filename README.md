# Cinema Shot Generator

اپ اندرویدِ تک‌کاربره و On-Device (بدون سرور) برای تولید پرامپت‌های متنی سینمایی برای مدل‌های AI ویدیوساز (Sora، Veo، Runway، Kling و مشابه). این اپ فقط **متن** تولید می‌کند، نه تصویر یا ویدیو.

## وضعیت فعلی

**در حال پیاده‌سازی تدریجی واحدهای معماری — هنوز بدون Room/Persistence و بدون UI واقعی.**

Scaffold پروژه (یک صفحه‌ی تست «Hello World») برقرار است. تاکنون لایه‌ی دامنه‌ی خالص (Kotlin، بدون Room و بدون UI) این واحدها پیاده‌سازی شده:

- **واحد ۰۱ — Story & Override:** `domain/story/` — مدل‌های Story Wizard، قوانین اعتبارسنجی، مدل‌های Human Override.
- **واحد ۰۲ — DNA Manager:** `domain/dna/` — مدل‌های DNA پروژه، منطق Soft Lock، قوانین اعتبارسنجی Rule 1 تا Rule 5.
- **واحد ۰۶ — Asset & Continuity:** `domain/asset/` — مدل‌های Character/Location Asset، Hard Lock مطلق (identity/appearance/age)، انتخاب خودکار Outfit/Expression، اعتبارسنجی فایل تصویر مرجع.
- **واحد ۰۷ — Validation & Consistency Engine:** `domain/validation/` — Validation Engine (Severity/ValidationIssue/ValidationReport سراسری)، Logic Conflict Checker (اکنون با `MotionLevel`/`CinematicMode` واقعی)، Dependency Resolver (تحلیل اثر، تشخیص وابستگی دایره‌ای).
- **واحد ۰۳ — Visual Identity:** `domain/visualidentity/` — Style Matrix (ترکیب کیفی سبک‌ها، بررسی سازگاری)، Cinematic Language (صاحب اصلی `CinematicMode`، تعیین ریتم Hybrid، اعتبارسنجی مدت شات).
- **واحد ۰۵ — Shot Engine:** `domain/shot/` — مدل‌های Shot/Beat/SoundProfile، صاحب اصلی `MotionLevel`، ارث‌بری از Scene (از طریق `inheritOrOverride` واحد ۰۴)، انتخاب Outfit (با `CharacterAsset` واقعی واحد ۰۶)، قوانین اعتبارسنجی Rule 1 تا Rule 5. `Shot.camera`/`.lighting`/`.environment` اکنون `SourcedSettings<T>` تایپ‌شده‌اند (نه `Map<String,String>` جای‌نگهدار)؛ `resolveCameraSettings`/`resolveLightingSettings`/`resolveEnvironmentSettings` مقدار نهایی را از `source` واقعی و `inheritOrOverride` تولید می‌کنند (ADR-013).
- **واحد ۰۴ — Scene Engine:** `domain/scene/` — مدل‌های Scene/SceneLocation/SceneConstraints، **صاحب اصلی `inheritOrOverride`** (تنها نسخه‌ی موجود در پروژه)، حذف Scene، قوانین اعتبارسنجی Rule 1/4/5.
- **واحد ۰۹ — Camera & Motion:** `domain/camera/` — Camera System (زاویه/فاصله/لنز/حرکت پایه و پیشرفته، ۵ قانون اعتبارسنجی)، Motion Intensity (سرعت/شدت سوژه، سرعت دوربین، Motion Blur).
- **واحد ۰۸ — Scene Conditions:** `domain/sceneconditions/` — Lighting System (Mood-to-Lighting، ۵ قانون اعتبارسنجی)، Environment & Weather Engine (Environment-to-Sound، ۸ قانون اعتبارسنجی، یکی مشترک با واحد ۰۷).
- **واحد ۱۰ — Audio Context Generator:** `domain/audio/` — تولید خودکار Ambient/Action Sound، پیشنهاد Breathing، `ActionSound`/`CharacterSound` بازاستفاده‌شده از واحد ۰۵ (نه بازتعریف)، Rule اعتبارسنجی تعداد لایه‌ی صوتی و timeline (با delegation به واحد ۰۷).
- **واحد ۱۱ — Prompt Engineering Core (قلب معماری کل سیستم):** `domain/promptengine/` — نقطه‌ی تجمیع واقعیِ تمام ده واحد قبلی در یک `PromptGenerationInput` واحد (بدون هیچ Placeholder)؛ `resolvePriority`/`PriorityLevel` (اولویت‌بندی HUMAN_OVERRIDE → SHOT_SPECIFIC → CHARACTER_CONTINUITY → SCENE_CONTEXT → PROJECT_DNA)؛ `enforceCharacterContinuity` با `selectOutfitForScene` واقعی واحد ۰۶؛ `manageSeed`، `collectWeightedEmphasis`؛ `assemblePromptBlueprint` که `StructuredParts`/`PromptBlueprint` نهایی را از داده‌ی واقعی همه‌ی واحدها (شامل `conflictsResolved`/`warnings` واقعی از واحد ۰۷، نه هاردکد) می‌سازد. `collectData` (اتصال به Room) خارج از Scope این واحد است — به واحد ۱۵ موکول شد.
- **واحد ۱۲ — State & Versioning:** `domain/stateversioning/` — State Machine (۵ حالت: DRAFT→REVIEW→LOCKED→FINAL→ARCHIVED، `canTransition` + ۳ Rule اعتبارسنجی)؛ Lock Mechanism ساده‌ی تک‌کاربره (`EntityLock`, `setLock`, `unlockWithOverride`، بدون هیچ فیلد چندکاربره‌ای طبق تصریح بلوپرینت)؛ Versioning (`VersionType` SAFE/RISKY — **مستقل از** سطح‌بندی Low/Medium/High Risk در `change-management.md`؛ `determineVersionType`، `rollbackToVersion` تزریق‌پذیر)؛ Impact Analysis (`analyzeVersionImpact` با `findDependents` تزریق‌پذیر). تمام I/O واقعی (Room، واحد ۱۵) با پارامتر تزریق‌پذیر جایگزین شد.
- **واحد ۱۴ — Output Delivery System (دومین واحد کلیدی معماری):** `domain/outputdelivery/` — Model Profile Library (`ModelProfile`, `universalDefaultProfile`, `selectProfile` با Fallback خودکار)؛ Renderer (`renderBlueprintToText`, `optimizeForProfile`, `render` — مصرف‌کننده‌ی واقعیِ `PromptBlueprint`/`StructuredParts` واحد ۱۱، بدون بازتعریف)؛ Output Composer (`composeOutput` — فقط بسته‌بندی، `renderedOutputs`/`bilingualPrompts` همیشه از بیرون تزریق می‌شوند، هرگز خودش Render نمی‌کند تا جای واحد ۱۳ در Pipeline باز بماند)؛ Bilingual System (`Language`, `t()` با Fallback به EN، `validateTranslationCoverage` — فقط ساختار داده و منطق ترجمه، بدون UI/Compose واقعی و بدون `translateToFarsi`).
- **واحد ۱۳ — Prompt Finalization Pipeline:** `domain/promptfinalization/` — Prompt Cleaner (`cleanPrompt` با ۵ فاز **واقعاً پیاده‌شده**، نه کامنت جای‌گذار: Conflict Detection، Redundancy Removal، Stop Words Filtering، Token Optimization، Final Polish — فقط برای متن انگلیسی، طبق تصریح بلوپرینت)؛ Token Cost Calculator (`estimateTokensFromWords`, `checkTokenLimit` با `ModelProfile` واقعی واحد ۱۴)؛ **`finalizePrompt`** — تابع پل‌زننده‌ای که `RenderedOutput` واحد ۱۴ را می‌گیرد، متنش را Clean می‌کند، و یک `RenderedOutput` جدید تمیزشده برمی‌گرداند که مستقیماً قابل تزریق به `composeOutput` است.
- **واحد ۱۵ — Project Storage/Room (⚠️ فقط قدم ۱ از ۲):** `data/entity/`, `data/dao/`, `data/AppDatabase.kt` — Room 2.8.4 + KSP اکنون واقعاً وصل شده‌اند (نه فقط اعلام‌شده در کاتالوگ نسخه‌ها). تمام ۹ Entity بلوپرینت (Project/Scene/Shot/Asset/PromptBlueprint/RenderedOutput/Override/Version/DependencyEdge) + DAO متناظر (suspend/Flow، طبق الزام Room جدید) + `ProjectTransactionDao` (اثبات Atomicity با `@Transaction`) پیاده شدند. **`data/` هیچ وابستگی‌ای به `domain/` ندارد** (تأیید با grep) — `DependencyEdgeEntity` عمداً مستقل از `domain.validation.DependencyEdge` بازتعریف شد. `domain/storage/` هم اضافه شد: `validateReferentialIntegrity` (منطق خالص، بدون Room) + ۴ Rule جدول با `ValidationIssue` سراسری. **قدم ۲ (اتصال واقعی resolve\*/collectData/createSnapshot/logEvent/findDependents واحدهای ۰۵/۱۱/۱۲ به این DAO ها) هنوز انجام نشده — یک قدم اجرایی جداگانه‌ی بعدی است.**

### 🎯 نقطه‌ی عطف: Pipeline اصلی کامل شد (واحد ۱۱ → ۱۴ → ۱۳ → ۱۴)

با تکمیل واحد ۱۳، مسیر اصلی تولید خروجی از ابتدا تا انتها بدون هیچ حلقه‌ی جاافتاده‌ای قابل زنجیره‌سازی است:

```
PromptBlueprint (واحد ۱۱)
    → render() (واحد ۱۴، بخش الف: Renderer)
    → finalizePrompt() (واحد ۱۳: Cleaner + Token Calculator)
    → composeOutput() (واحد ۱۴، بخش ب: Output Composer)
    → OutputPackage نهایی (شامل ExportFile ها و هر دو نسخه‌ی زبانی)
```

هر چهار واحد این زنجیره (۱۱، ۱۳، ۱۴) اکنون منطق دامنه‌ی واقعی و کامپایل‌شونده دارند — بدون Placeholder در مسیر اصلی.

بلوپرینت هر واحد در `docs/blueprints/` منبع حقیقت است؛ تصمیمات و انحرافات تأییدشده در `docs/adr/` ثبت شده‌اند.

### تصمیمات Migration انجام‌شده

چهار سؤال باز تجمیعی قبلی (ADR-004، ADR-006، ADR-007) در یک قدم Migration حل شدند — جزئیات کامل در `docs/adr/010-cross-unit-migrations.md`:

1. **واحدهای ۰۱/۰۲/۰۶ به `ValidationIssue`/`Severity` سراسری واحد ۰۷ Migrate شدند.** معادل «Valid» قبلی: `null` (توابع تک‌نتیجه‌ای) یا لیست خالی (`validateStoryContext`). استثنا: `UpdateResult` (Hard Lock واحد ۰۶) عمداً دست‌نخورده ماند تا تضمین سطح Type System «هرگز فقط Warning نیست» حفظ شود.
2. **واحد ۰۷ اکنون از `MotionLevel`/`CinematicMode` واقعی استفاده می‌کند** (به‌جای String موقت) — یک وابستگی چرخه‌ای آگاهانه بین پکیج‌های `domain.validation` و `domain.shot` ایجاد کرد (مستند در ADR-010، بدون مشکل کامپایل).
3. **نسخه‌ی تکراری `inheritOrOverride` در واحد ۰۵ حذف شد**؛ واحد ۰۴ تنها صاحب این تابع است. تست تکراری متناظر (`ShotInheritanceTest.kt`) هم حذف شد.
4. **تضاد Rule «Shot بدون Subject» رفع شد:** `validateDataCompleteness` واحد ۰۷ اکنون `locationIds` را هم بررسی می‌کند و Severity آن Blocking است — دقیقاً هم‌راستا با `validateShotHasSubject` واحد ۰۵. **پیشنهاد باز (اجرا نشده):** یکی‌سازی این دو تابع یا نگه‌داشتن عمدی‌شان به‌عنوان دو Rule مستقل.

**یادداشت‌های باقی‌مانده (نه سؤال باز جدی):** هم‌پوشانی مفهومی `SubjectSpeed` (واحد ۰۹) و `MotionLevel` (واحد ۰۵)؛ همپوشانی `canDeleteAsset` (واحد ۰۷) و `validateAssetDeletion` (واحد ۰۶)؛ **سه نوع «صدای محیطی» مستقل** — `domain.shot.AmbientSound` (۳ فیلد)، `domain.sceneconditions.AmbientSoundSuggestion` (۳ فیلد)، `domain.audio.AmbientSound` (۴ فیلد، با `source`) — یکی‌سازی واقعی‌شان به تصمیم معمار درباره‌ی پایپ‌لاین بین واحدهای ۰۵/۰۸/۱۰ نیاز دارد (جزئیات در ADR-009 و ADR-011؛ ADR-012 تأیید کرد که این سه‌گانگی برای `audioDescription` واحد ۱۱ بی‌خطر است، چون فقط فیلدهای مشترک `type`/`intensity` خوانده می‌شوند). در مقابل، `ActionSound`/`CharacterSound` واحد ۱۰ همان نمونه‌ی واحد ۰۵ را بازاستفاده کردند (بدون تکرار).

**پس از واحد ۱۱:** `PromptGenerationInput.collectData` (اتصال واقعی به داده‌ی Room) هنوز پیاده نشده — منتظر واحد ۱۵ می‌ماند.

### بستن شکاف SourcedSettings ↔ انواع واقعی (رفع‌شده)

یادداشت باقی‌مانده‌ی ADR-012 («`Shot.camera`/`.lighting`/`.environment` هنوز `SourcedSettings` جای‌نگهدار هستند») **رفع شد** — جزئیات کامل در `docs/adr/013-unit05-settings-resolution-deviations.md`:

- `SourcedSettings` (واحد ۰۵) از `settings: Map<String,String>` بدون schema به `SourcedSettings<T>(source, overrideValue: T?)` تایپ‌شده تغییر کرد.
- `LightingSettings`/`EnvironmentSettings` (که ابتدا محلیِ واحد ۱۱ بودند) به `domain.sceneconditions` (واحد ۰۸) منتقل شدند تا `domain.shot` مجبور به وابستگی معکوس به `domain.promptengine` (بالادست‌ترین واحد) نباشد.
- سه تابع جدید `resolveCameraSettings`/`resolveLightingSettings`/`resolveEnvironmentSettings` (`domain/shot/ShotSettingsResolution.kt`) با `inheritOrOverride` واقعی واحد ۰۴ مقدار نهایی را حل می‌کنند؛ چون `Scene` خودش فیلد پیش‌فرض camera/lighting/environment ندارد، `sceneDefault` پارامتر خارجی است؛ خروجی `Result<T>` است (نه مقدار غیرقابل‌Null جعلی) برای حالت نظری «نه Scene پیش‌فرض دارد و نه Shot override».

### پس از واحد ۱۲

`Entity` (واحد ۱۲) یک نوع محلی حداقلی است، هنوز به Scene/Shot/ProjectDna/Output واقعی وصل نیست — کار لایه‌ی Persistence (واحد ۱۵)؛ `findDependents` تزریق‌پذیر واحد ۱۲ مستقیماً به `analyzeImpact` واحد ۰۷ وصل نشد چون شکل ورودی/خروجی‌شان متفاوت است (جزئیات در `docs/adr/014-unit12-state-and-versioning-deviations.md`)؛ `StateVersioningEventLogger` (واحد ۱۲) و `OverrideEventLogger` (واحد ۰۱) هنوز به یک تاریخچه‌ی رویداد واحد سیم‌کشی نشده‌اند.

### پس از واحد ۱۴

فقط پروفایل `universal_default` پیاده شده — پروفایل‌های JSON واقعی مدل‌های دیگر (Veo, Kling, Runway, Seedance, …) پیاده نشدند؛ `generateBilingualPrompt`/`translateToFarsi` بلوپرینت ۱۴ عمداً پیاده نشدند (نیازمند تصمیم معماری درباره‌ی موتور ترجمه‌ی واقعی، خارج از Scope این قدم) — جزئیات در `docs/adr/015-unit14-output-delivery-deviations.md`.

### پس از واحد ۱۳

اتصال واقعی به واحد ۱۴ اکنون برقرار است (`finalizePrompt` بین `render()` و `composeOutput()` جا گرفت — نقطه‌ی عطف بالا). معادل فارسی Prompt Cleaner پیاده نشد (طبق تصریح خودِ بلوپرینت، این منطق فقط برای متن انگلیسی طراحی شده، چون `render()` واحد ۱۴ فعلاً فقط انگلیسی تولید می‌کند)؛ Budget Tracking/Cost Estimation طبق بلوپرینت پیاده نشد (فیچر اختیاری) — جزئیات در `docs/adr/016-unit13-prompt-finalization-deviations.md`.

### پس از واحد ۱۵ — قدم ۱ (⚠️ نیمه‌ی اول، نه واحد کامل)

**فقط Room (Entity/DAO/Database) پیاده شد؛ صفر اتصال به دامنه.** یک باگ واقعی حین تست کشف و رفع شد: ترتیب فراخوانی در `ProjectTransactionDao.saveShotWithSceneUpdate` (که در کد مفهومی بلوپرینت `saveShot` را قبل از `saveScene` می‌آورد) باعث می‌شد `OnConflictStrategy.REPLACE` روی `SceneEntity` — که SQLite آن را با حذف+درج پیاده می‌کند — به‌خاطر `ForeignKey(onDelete=CASCADE)` از `ShotEntity`، شات تازه‌درج‌شده را در همان تراکنش پاک کند؛ ترتیب به `saveScene` سپس `saveShot` تغییر کرد (جزئیات کامل در `docs/adr/017-unit15-project-storage-deviations.md`). این یک هشدار عمومی برای هر `@Transaction` آینده‌ی مشابه است.

`AutoSaveManager`/`BackupManager`/Export-Import پیاده نشدند (فقط امضای پیشنهادی در ADR-017 یادداشت شد) — به DAO های این قدم وابسته‌اند اما منطق سطح بالاتری‌اند. **قدم ۲ این واحد (اتصال واقعی به واحدهای ۰۵/۱۱/۱۲) هنوز باقی مانده.**

## Stack

- **زبان:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **معماری:** MVVM ساده (ViewModel + StateFlow) — بدون فریمورک DI در فاز اول
- **ذخیره‌سازی:** Room 2.8.4 + KSP (روی SQLite) — پشتیبانی از چند پروژه‌ی همزمان؛ Entity/DAO/Database وصل شده‌اند (واحد ۱۵، قدم ۱)، اتصال به دامنه هنوز باقی مانده (قدم ۲)
- **اتصال AI (اختیاری):** Ktor Client — فقط وقتی کاربر کلید API شخصی وارد کند

## ساختار

```
app/src/main/java/com/operaboys/cinemashotgenerator/
├── data/    → Room entities, DAO, Database class (واحد ۱۵، قدم ۱ — بدون وابستگی به domain/)
│   ├── entity/ → ۹ Room Entity (Project/Scene/Shot/Asset/PromptBlueprint/RenderedOutput/Override/Version/DependencyEdge)
│   ├── dao/    → DAO های suspend/Flow متناظر + ProjectTransactionDao (اثبات Atomicity)
│   └── AppDatabase.kt → RoomDatabase + Singleton Provider (بدون DI)
├── domain/  → مدل‌های دامنه و منطق کسب‌وکار
│   ├── story/  → واحد ۰۱: Story Wizard + Human Override
│   ├── dna/    → واحد ۰۲: DNA Manager (Soft Lock)
│   ├── asset/  → واحد ۰۶: Asset & Continuity (Hard Lock)
│   ├── validation/ → واحد ۰۷: Validation & Consistency Engine
│   ├── visualidentity/ → واحد ۰۳: Visual Identity (Style Matrix + Cinematic Language)
│   ├── shot/   → واحد ۰۵: Shot Engine
│   ├── scene/  → واحد ۰۴: Scene Engine (صاحب اصلی inheritOrOverride)
│   ├── camera/ → واحد ۰۹: Camera & Motion
│   ├── sceneconditions/ → واحد ۰۸: Scene Conditions (Lighting + Environment)
│   ├── audio/  → واحد ۱۰: Audio Context Generator
│   ├── promptengine/ → واحد ۱۱: Prompt Engineering Core (قلب سیستم — تجمیع همه‌ی واحدها)
│   ├── stateversioning/ → واحد ۱۲: State & Versioning (State Machine + Lock + Versioning + Impact Analysis)
│   ├── outputdelivery/ → واحد ۱۴: Output Delivery System (Model Profile + Renderer + Composer + Bilingual)
│   ├── promptfinalization/ → واحد ۱۳: Prompt Finalization Pipeline (Cleaner + Token Calculator)
│   └── storage/ → واحد ۱۵ (منطق خالص): validateReferentialIntegrity + قوانین جدول
├── ui/      → صفحه‌های Compose (فعلاً فقط صفحه‌ی تست)
└── di/      → (خالی، برای بعد)

docs/blueprints/  → بلوپرینت‌های معماری (منبع حقیقت) — قبل از پیاده‌سازی هر واحد بخوانید
docs/adr/         → تصمیمات و انحرافات تأییدشده در هر قدم اجرایی
```

## Build

نیازمندی‌ها: JDK 17+، Android SDK (compileSdk 36). نسخه‌ها: AGP 8.13.0، Gradle 8.14.3، Kotlin 2.2.21، Room 2.8.4، KSP 2.3.10 — minSdk 26، targetSdk 36.

تست‌های Room (DAO) با **Robolectric** (نه instrumented `androidTest`) اجرا می‌شوند — این محیط توسعه به امولاتور/دستگاه واقعی دسترسی ندارد؛ Robolectric یک دیتابیس Room واقعیِ درون‌حافظه‌ای روی JVM شبیه‌سازی می‌کند و با همان `gradle :app:testDebugUnitTest` معمولی اجرا می‌شود.

```
./gradlew :app:assembleDebug
```

## محدودیت‌های شناخته‌شده

(فعلاً خالی — طبق قانون پروژه، این بخش باید هر بار که محدودیتی اضافه یا رفع شد به‌روزرسانی شود.)
