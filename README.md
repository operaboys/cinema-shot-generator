# Cinema Shot Generator

اپ اندرویدِ تک‌کاربره و On-Device (بدون سرور) برای تولید پرامپت‌های متنی سینمایی برای مدل‌های AI ویدیوساز (Sora، Veo، Runway، Kling و مشابه). این اپ فقط **متن** تولید می‌کند، نه تصویر یا ویدیو.

## وضعیت فعلی

**در حال پیاده‌سازی تدریجی واحدهای معماری — Room/Persistence اکنون کامل و به‌طور واقعی به دامنه وصل است؛ هنوز بدون UI واقعی.**

Scaffold پروژه (یک صفحه‌ی تست «Hello World») برقرار است. تاکنون لایه‌ی دامنه‌ی خالص (Kotlin، بدون Room و بدون UI) این واحدها پیاده‌سازی شده:

- **واحد ۰۱ — Story & Override (تأییدشده هم‌راستا با بلوپرینت نسخه ۴):** `domain/story/` — مدل‌های Story Wizard، قوانین اعتبارسنجی، مدل‌های Human Override. بازبینی این قدم تأیید کرد کد از قبل کاملاً با `docs/blueprints/01-story-and-override-v2.md` (نسخه ۴) هم‌راستا بود (فیلدهای مسطح `moodPrimary`/`moodSecondary`، بدون فرمت تودرتوی قدیمی؛ `Mood` از `domain.dna` طبق ADR-027) — فقط کامنت هدر ۳ فایل (`HumanOverride.kt`, `OverrideActions.kt`, `StoryValidation.kt`) که هنوز به بلوپرینت بدون `-v2` اشاره می‌کردند اصلاح شد.
- **واحد ۰۲ — DNA Manager (Migration بلوپرینت نسخه ۵ کامل شد — بزرگ‌ترین Breaking Change تا کنون):** `domain/dna/` — `VisualStyle` اکنون ۳۴ مقدار دقیق در ۵ دسته (`VisualStyleCategory`)؛ `Mood` (۲۵ مقدار، ۵ دسته) از `domain/story/` به اینجا منتقل شد و تنها مالک این نام در کل پروژه است؛ `LightingStyle` (۲۲ مقدار، ۴ دسته) از `domain/sceneconditions/` به اینجا منتقل شد؛ `ContrastLevel` (رفع باگ واقعی: `MasterPalette.globalContrast` اشتباهاً `SaturationLevel` بود)؛ `AspectRatio` (۱۱ مقدار — `OutputConstraints.aspectRatio` از `String` به enum، Breaking Change واقعی)؛ `QualityDirectives`/`GlobalMoodBase`/`LightingPreference` کامل شدند؛ `stylePreferences`/`overrideRules` طبق تصمیم صریح معمار کاملاً حذف شدند؛ `validateColorPalette` (Rule جدید) اضافه شد. جزئیات کامل، دو تناقض واقعی کشف‌شده بین بلوپرینت/type-registry (و نحوه‌ی حل‌شان)، و فهرست کامل مصرف‌کنندگان اصلاح‌شده در `docs/adr/027-unit02-dna-manager-v5-migration.md`.
- **واحد ۰۶ — Asset & Continuity (Migration بلوپرینت نسخه ۵ کامل شد):** `domain/asset/` — `CharacterTier` (MAIN/SECONDARY/BACKGROUND) با `defaultLockLevelForTier`؛ Hard Lock قدیمی (فقط Allowed/Blocked مطلق) اکنون شرطی به سه سطح مستقل `CharacterContinuityLevel`(FULL/MEDIUM/NONE)/`LocationContinuityLevel`(STYLE)/`PropContinuityLevel`(FORM) است — `UpdateResult` حالت سوم `Warned` گرفت، اما سطح **FULL** بایت‌به‌بایت همان رفتار Blocking بدون استثنای قبلی را حفظ کرده. `ObjectAsset` از `LocationAsset` مشترک قدیمی کاملاً تفکیک شد (Option A طبق توصیه‌ی بلوپرینت) — `LocationAsset` اکنون فقط مکان است. Rule های جدید ۱۰ (size/materialAndColor الزامی)، ۱۱ (basePrompt نباید whitespace-only باشد، روی هر سه نوع Asset)، ۱۲ (ObjectAsset.subtype — تضمین‌شده در سطح Type System، بدون تابع Runtime). جزئیات کامل در `docs/adr/029-unit06-continuity-tiers-migration-part1.md`. **تکمیل شد:** `PhysicalAppearance` اکنون `Gender` enum واقعی دارد (نه رشته‌ی آزاد)؛ `height`/`build`/`hair`/`facialFeatures` nullable شدند؛ `physicalFeatures`/`toPromptString()` اضافه شدند — جزئیات در `docs/adr/031-unit06-physical-appearance-gender-migration.md`.
- **واحد ۰۷ — Validation & Consistency Engine:** `domain/validation/` — Validation Engine (Severity/ValidationIssue/ValidationReport سراسری)، Logic Conflict Checker (اکنون با `MotionLevel`/`CinematicMode` واقعی)، Dependency Resolver (تحلیل اثر، تشخیص وابستگی دایره‌ای).
- **واحد ۰۳ — Visual Identity:** `domain/visualidentity/` — Style Matrix (ترکیب کیفی سبک‌ها، بررسی سازگاری)، Cinematic Language (صاحب اصلی `CinematicMode`، تعیین ریتم Hybrid، اعتبارسنجی مدت شات).
- **واحد ۰۵ — Shot Engine (Migration بلوپرینت نسخه ۳: negativePromptOverride):** `domain/shot/` — مدل‌های Shot/Beat/SoundProfile، صاحب اصلی `MotionLevel`، ارث‌بری از Scene (از طریق `inheritOrOverride` واحد ۰۴)، انتخاب Outfit (با `CharacterAsset` واقعی واحد ۰۶)، قوانین اعتبارسنجی Rule 1 تا Rule 5. `Shot.camera`/`.lighting`/`.environment` اکنون `SourcedSettings<T>` تایپ‌شده‌اند (نه `Map<String,String>` جای‌نگهدار)؛ `resolveCameraSettings`/`resolveLightingSettings`/`resolveEnvironmentSettings` مقدار نهایی را از `source` واقعی و `inheritOrOverride` تولید می‌کنند (ADR-013). `Shot.negativePromptOverride: String?` (پیش‌فرض `null`) اضافه شد — منبع ارث‌بری‌اش برخلاف camera/lighting/environment، **DNA پروژه** است نه Scene؛ `resolveNegativePrompt(shot, dnaNegativePrompt)` این ارث‌بری را حل می‌کند. **یافته‌ی این Migration** (کل مسیر مصرف واقعی هنوز سیم‌کشی نشده بود) در Migration واحد ۱۴ رفع شد — جزئیات در `docs/adr/028-unit05-negative-prompt-override-migration.md` و `docs/adr/030-unit14-reference-image-and-negative-prompt-migration.md`.
- **واحد ۰۴ — Scene Engine:** `domain/scene/` — مدل‌های Scene/SceneLocation/SceneConstraints، **صاحب اصلی `inheritOrOverride`** (تنها نسخه‌ی موجود در پروژه)، حذف Scene، قوانین اعتبارسنجی Rule 1/4/5.
- **واحد ۰۹ — Camera & Motion:** `domain/camera/` — Camera System (زاویه/فاصله/لنز/حرکت پایه و پیشرفته، ۵ قانون اعتبارسنجی)، Motion Intensity (سرعت/شدت سوژه، سرعت دوربین، Motion Blur).
- **واحد ۰۸ — Scene Conditions:** `domain/sceneconditions/` — Lighting System (Mood-to-Lighting، ۵ قانون اعتبارسنجی)، Environment & Weather Engine (Environment-to-Sound، ۸ قانون اعتبارسنجی، یکی مشترک با واحد ۰۷).
- **واحد ۱۰ — Audio Context Generator:** `domain/audio/` — تولید خودکار Ambient/Action Sound، پیشنهاد Breathing، `ActionSound`/`CharacterSound` بازاستفاده‌شده از واحد ۰۵ (نه بازتعریف)، Rule اعتبارسنجی تعداد لایه‌ی صوتی و timeline (با delegation به واحد ۰۷).
- **واحد ۱۱ — Prompt Engineering Core (قلب معماری کل سیستم):** `domain/promptengine/` — نقطه‌ی تجمیع واقعیِ تمام ده واحد قبلی در یک `PromptGenerationInput` واحد (بدون هیچ Placeholder)؛ `resolvePriority`/`PriorityLevel` (اولویت‌بندی HUMAN_OVERRIDE → SHOT_SPECIFIC → CHARACTER_CONTINUITY → SCENE_CONTEXT → PROJECT_DNA)؛ `enforceCharacterContinuity` با `selectOutfitForScene` واقعی واحد ۰۶؛ `manageSeed`، `collectWeightedEmphasis`؛ `assemblePromptBlueprint` که `StructuredParts`/`PromptBlueprint` نهایی را از داده‌ی واقعی همه‌ی واحدها (شامل `conflictsResolved`/`warnings` واقعی از واحد ۰۷، نه هاردکد) می‌سازد. `collectData` (اتصال به Room) خارج از Scope این واحد است — به واحد ۱۵ موکول شد.
- **واحد ۱۲ — State & Versioning:** `domain/stateversioning/` — State Machine (۵ حالت: DRAFT→REVIEW→LOCKED→FINAL→ARCHIVED، `canTransition` + ۳ Rule اعتبارسنجی)؛ Lock Mechanism ساده‌ی تک‌کاربره (`EntityLock`, `setLock`, `unlockWithOverride`، بدون هیچ فیلد چندکاربره‌ای طبق تصریح بلوپرینت)؛ Versioning (`VersionType` SAFE/RISKY — **مستقل از** سطح‌بندی Low/Medium/High Risk در `change-management.md`؛ `determineVersionType`، `rollbackToVersion` تزریق‌پذیر)؛ Impact Analysis (`analyzeVersionImpact` با `findDependents` تزریق‌پذیر). تمام I/O واقعی (Room، واحد ۱۵) با پارامتر تزریق‌پذیر جایگزین شد.
- **واحد ۱۴ — Output Delivery System (دومین واحد کلیدی معماری، پروفایل‌های مدل تکمیل‌شده):** `domain/outputdelivery/` — Model Profile Library (`ModelProfile`, `selectProfile` با Fallback خودکار به `universal_default`)؛ **۱۳ پروفایل مدل واقعی بازار** (`ModelProfiles.kt`، جولای ۲۰۲۶) به‌علاوه‌ی `universalDefaultProfile` در `ALL_MODEL_PROFILES`: Veo 3.1، Kling 3.0/Turbo، Seedance 2.5، HappyHorse-1.0، Runway Gen-4.5، Luma Ray3.14، Hailuo/MiniMax 2.3، Midjourney v7، Wan 2.2، HunyuanVideo 1.5، LTX-2.3، Vidu Q3، Stable Diffusion SD3 (Sora عمداً حذف شد؛ جزئیات و تخمین‌های مستندشده در `docs/adr/021-unit14-model-profiles-deviations.md`)؛ Renderer (`renderBlueprintToText`, `optimizeForProfile`, `render` — مصرف‌کننده‌ی واقعیِ `PromptBlueprint`/`StructuredParts` واحد ۱۱، بدون بازتعریف)؛ Output Composer (`composeOutput` — فقط بسته‌بندی، `renderedOutputs`/`bilingualPrompts` همیشه از بیرون تزریق می‌شوند، هرگز خودش Render نمی‌کند تا جای واحد ۱۳ در Pipeline باز بماند)؛ Bilingual System (`Language`, `t()` با Fallback به EN، `validateTranslationCoverage` — فقط ساختار داده و منطق ترجمه، بدون UI/Compose واقعی و بدون `translateToFarsi`).
- **واحد ۱۳ — Prompt Finalization Pipeline:** `domain/promptfinalization/` — Prompt Cleaner (`cleanPrompt` با ۵ فاز **واقعاً پیاده‌شده**، نه کامنت جای‌گذار: Conflict Detection، Redundancy Removal، Stop Words Filtering، Token Optimization، Final Polish — فقط برای متن انگلیسی، طبق تصریح بلوپرینت)؛ Token Cost Calculator (`estimateTokensFromWords`, `checkTokenLimit` با `ModelProfile` واقعی واحد ۱۴)؛ **`finalizePrompt`** — تابع پل‌زننده‌ای که `RenderedOutput` واحد ۱۴ را می‌گیرد، متنش را Clean می‌کند، و یک `RenderedOutput` جدید تمیزشده برمی‌گرداند که مستقیماً قابل تزریق به `composeOutput` است.
- **واحد ۱۵ — Project Storage/Room (✅ تکمیل‌شده):** `data/entity/`, `data/dao/`, `data/AppDatabase.kt`, `data/repository/` — Room 2.8.4 + KSP + kotlinx.serialization واقعاً وصل شده‌اند. تمام ۹ Entity بلوپرینت + سه Entity اضافه (`EventLogEntity`, `ProjectDnaEntity`, `AudioContextEntity` — بلوپرینت ۱۵ اصلاً فهرستشان نکرده بود؛ طبق الگوی «هر Aggregate یک جدول مستقل») + DAO متناظر (suspend/Flow) + `ProjectTransactionDao` (اثبات Atomicity با `@Transaction`). **`data/` هیچ وابستگی‌ای به `domain/` ندارد** (تأیید با grep) — `DependencyEdgeEntity` مستقل بازتعریف شد. `domain/storage/`: `validateReferentialIntegrity` + ۴ Rule جدول با `ValidationIssue` سراسری.
  **همه‌ی Repository ها:** `SettingsResolutionRepository`، `VersioningRepository` (با `StateVersioningEventLogger` واقعی)، `ImpactAnalysisRepository`، `ProjectDnaRepository`، `AssetRepository`، `SceneRepository`، `AudioContextRepository`، و در نهایت **`PromptGenerationRepository.collectData`** که همه‌ی آن‌ها را در یک `PromptGenerationInput` واقعی (واحد ۱۱) تجمیع می‌کند.

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

### 🎯 نقطه‌ی عطف: واحد ۱۵ (Project Storage/Room) کامل شد

پس از سه قدم اجرایی (Entity/DAO/Database → Settings/Versioning/Impact Repository ها → ProjectDna/Asset → Scene/AudioContext/`collectData`)، **تمام توابع تزریق‌پذیر کل پروژه اکنون به پیاده‌سازی واقعی Room وصل شده‌اند** — نه فقط طراحی‌شده روی کاغذ:

| تابع تزریق‌پذیر | واحد دامنه | Repository متصل |
|---|---|---|
| `resolveCameraSettings`/`resolveLightingSettings`/`resolveEnvironmentSettings` | ۰۵ | `SettingsResolutionRepository` |
| `collectData` (کل `PromptGenerationInput`) | ۱۱ | `PromptGenerationRepository` |
| `createSnapshot`/`logEvent` (در `rollbackToVersion`) | ۱۲ | `VersioningRepository` |
| `StateVersioningEventLogger` | ۱۲ | `VersioningRepository` (پشت `EventLogDao`) |
| `findDependents` (در `analyzeVersionImpact`) | ۱۲ | `ImpactAnalysisRepository` |

سه باگ واقعی در طول این سه قدم با تست کشف و رفع شدند (ترتیب `saveShotWithSceneUpdate` در برابر Cascade Delete؛ تداخل نام فیلد `type` با Discriminator چندریختی kotlinx.serialization) — جزئیات کامل در `docs/adr/017` تا `docs/adr/020`.

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

پروفایل `universal_default` پیاده شده بود؛ پروفایل‌های JSON واقعی مدل‌های دیگر (Veo, Kling, Runway, Seedance, …) در آن قدم پیاده نشدند؛ `generateBilingualPrompt`/`translateToFarsi` بلوپرینت ۱۴ عمداً پیاده نشدند (نیازمند تصمیم معماری درباره‌ی موتور ترجمه‌ی واقعی، خارج از Scope آن قدم) — جزئیات در `docs/adr/015-unit14-output-delivery-deviations.md`.

### 🎯 نقطه‌ی عطف: واحد ۱۴ — ۱۳ پروفایل مدل واقعی بازار تکمیل شد

شکاف بالا («فقط universal_default») رفع شد. `domain/outputdelivery/ModelProfiles.kt` اکنون ۱۳ پروفایل مدل واقعی (بر اساس داده‌ی بازار جولای ۲۰۲۶ تأییدشده توسط معمار) اضافه کرد:

**پروفایل‌های پشتیبانی‌شده (`ALL_MODEL_PROFILES`):** `universal_default`، Veo 3.1 (`veo`)، Kling 3.0/Turbo (`kling`)، Seedance 2.5 (`seedance`)، HappyHorse-1.0 (`happyhorse`)، Runway Gen-4.5 (`runway`)، Luma Ray3.14 (`luma`)، Hailuo/MiniMax 2.3 (`hailuo`)، Midjourney v7 (`midjourney`)، Wan 2.2 (`wan`)، HunyuanVideo 1.5 (`hunyuan`)، LTX-2.3 (`ltx`)، Vidu Q3 (`vidu`)، Stable Diffusion SD3 (`stable_diffusion`). Sora (OpenAI) عمداً از این فهرست حذف شد (تصمیم صریح معمار).

بلوپرینت ۱۴ دست‌نخورده ماند (خودش تصریح کرده جدول مدل‌هایش نقطه‌ی شروع است، نه نهایی — این قدم فقط داده اضافه کرد). فقط `veo.maxPromptLength=2000` عدد واقعی (از نمونه‌ی بلوپرینت) است؛ بقیه‌ی اعداد فنی (`maxTokens` همه، `maxPromptLength` بقیه) **تخمین محافظه‌کارانه‌ی مستندشده‌اند**، نه رسمی API. آن قدم دو محدودیت شناخته‌شده در `Renderer.kt` مستند کرد (بدون رفع، چون آن قدم فقط داده اضافه می‌کرد) — **هر دو در قدم بعدی رفع شدند** (پایین را ببینید). جزئیات کامل در `docs/adr/021-unit14-model-profiles-deviations.md`.

### 🎯 نقطه‌ی عطف: واحد ۱۴ — دو محدودیت شناخته‌شده‌ی Renderer رفع شدند (رندر JSON واقعی + وزن‌دهی واقعی SD)

هر دو محدودیت مستندشده در ADR-021 رفع شدند — **بدون تغییر رفتار پروفایل‌های غیر-JSON/غیر-SD موجود** (راستی‌آزمایی رگرسیون صریح در تست‌ها):

1. **رندر JSON واقعی:** `render()` اکنون برای `format.type == "json"` (۹ از ۱۳ پروفایل مدل) یک `JsonObject` واقعی و Parse‌پذیر می‌سازد (با `kotlinx.serialization.json`، نه String concatenation دستی) — مستقیماً از `structuredParts` (subject/scene/shot/camera/lighting/environment/style/timeline/audio)، نه از متن مسطح‌شده. `weightedEmphasis` (وقتی پروفایل `supportsWeightedTags` دارد) به‌عنوان یک Object جدا در همان JSON اضافه می‌شود — نه با دستکاری متنی. در حال حاضر هیچ پروفایلی هم‌زمان json+weightedTags ندارد؛ این مسیر برای پروفایل‌های آینده مستند و آماده است.
2. **وزن‌دهی واقعی Stable Diffusion:** `applyWeightSyntax` اکنون `platform == "stable_diffusion"` را هم ویژه می‌کند — نحو معروف `(tag:weight)` (مثل `(cinematic lighting:1.2)`)، دقیقاً مثل نحو `tag::weight` میجرنی که از قبل موجود بود.

جزئیات کامل تصمیمات (ساختار JSON پیشنهادی، چرا از `structuredParts` خام نه متن مسطح) در `docs/adr/025-unit14-renderer-json-and-weighting-deviations.md`.

### 🎯 نقطه‌ی عطف: مرز Renderer↔Prompt Finalization برای JSON ایمن شد

دو مشکل مرتبط که بعد از رندر JSON واقعی (بالا) کشف شدند، رفع شدند:

1. **Truncation JSON (بدهی ADR-025):** بررسی نشان داد `render()` از قبل خروجی JSON را کوتاه نمی‌کرد (فقط مستند نبود) — این رفتار اکنون صریحاً مستند و با تست قفل شده: JSON طولانی هرگز بریده نمی‌شود، `validatePromptLength` موجود (بدون تغییر) همچنان به‌درستی Warning می‌دهد.
2. **`finalizePrompt` کور نسبت به JSON:** `cleanPrompt` (واحد ۱۳) فرض متن آزاد دارد — روی یک رشته‌ی JSON واقعی می‌توانست ساختار را خراب کند (بدترینش: `applyFinalPolish` بعد از `}` پایانی نقطه اضافه می‌کرد → JSON نامعتبر). `finalizePrompt` اکنون برای `format.type=="json"` به‌جای Clean کردن کل رشته، JSON را Parse می‌کند (`kotlinx.serialization.json`)، فقط مقدار هر فیلد رشته‌ای را Clean می‌کند (با `CleaningOptions.applyFinalPolish=false` تازه‌افزوده — Phase 5 برای یک مقدار تکی فیلد بی‌معناست)، و دوباره Serialize می‌کند؛ Object تودرتوی `weightedEmphasis` دست‌نخورده می‌ماند. اگر Parse شکست بخورد (یعنی واقعاً JSON نبوده)، به مسیر متن آزاد قبلی Fallback می‌کند، نه Crash.

**محدودیت پذیرفته‌شده:** چون هر فیلد جدا Clean می‌شود، تشخیص تضاد بین‌فیلدی (نه درون یک فیلد) دیگر ممکن نیست — رفعش خارج از Scope این قدم بود. جزئیات کامل و نظر تخصصی درباره‌ی انتخاب این رویکرد (به‌جای غیرفعال‌کردن کامل Cleaning برای JSON) در `docs/adr/026-unit13-json-aware-finalization-deviations.md`.

### پس از واحد ۱۳

اتصال واقعی به واحد ۱۴ اکنون برقرار است (`finalizePrompt` بین `render()` و `composeOutput()` جا گرفت — نقطه‌ی عطف بالا). معادل فارسی Prompt Cleaner پیاده نشد (طبق تصریح خودِ بلوپرینت، این منطق فقط برای متن انگلیسی طراحی شده، چون `render()` واحد ۱۴ فعلاً فقط انگلیسی تولید می‌کند)؛ Budget Tracking/Cost Estimation طبق بلوپرینت پیاده نشد (فیچر اختیاری) — جزئیات در `docs/adr/016-unit13-prompt-finalization-deviations.md`.

### پس از واحد ۱۵ — قدم ۱

**فقط Room (Entity/DAO/Database) پیاده شد؛ صفر اتصال به دامنه.** یک باگ واقعی حین تست کشف و رفع شد: ترتیب فراخوانی در `ProjectTransactionDao.saveShotWithSceneUpdate` (که در کد مفهومی بلوپرینت `saveShot` را قبل از `saveScene` می‌آورد) باعث می‌شد `OnConflictStrategy.REPLACE` روی `SceneEntity` — که SQLite آن را با حذف+درج پیاده می‌کند — به‌خاطر `ForeignKey(onDelete=CASCADE)` از `ShotEntity`، شات تازه‌درج‌شده را در همان تراکنش پاک کند؛ ترتیب به `saveScene` سپس `saveShot` تغییر کرد (جزئیات کامل در `docs/adr/017-unit15-project-storage-deviations.md`). این یک هشدار عمومی برای هر `@Transaction` آینده‌ی مشابه است.

### پس از واحد ۱۵ — قدم ۲ (تکمیل‌شده — به‌جز `collectData`)

`data/repository/` سه Repository واقعی اضافه کرد (بالا). یک باگ واقعی دیگر حین تست کشف و رفع شد: `CameraMovementDto.Basic` یک فیلد به نام `type` داشت که با ستون Discriminator پیش‌فرض kotlinx.serialization برای Sealed Class ها (که هم `"type"` نام دارد) تداخل می‌کرد — به `movementType` تغییر نام یافت. `VersionEntity` سه فیلد (`changeSummary`, `modifiedFieldsJson`, `parentVersionId`) گرفت که در قدم ۱ نبودند (نمونه‌ی ناقص خودِ بلوپرینت) تا داده‌ی `EntityVersion` واحد ۱۲ حین ذخیره گم نشود. جزئیات کامل در `docs/adr/018-unit15-step2-repository-deviations.md`.

`AutoSaveManager`/`BackupManager`/Export-Import همچنان پیاده نشدند (فقط امضای پیشنهادی، طبق ADR-017). **`PromptGenerationRepository.collectData` به یک قدم سوم جداگانه موکول شد** (تأییدشده توسط معمار) — سریالایز کامل `PromptGenerationInput` عملاً کل لایه‌ی دامنه را پوشش می‌دهد.

### پس از واحد ۱۵ — قدم ۳، زیرقدم ۱ (تکمیل‌شده)

طبق همان تصمیم قدم ۲ (Scope بزرگ بود)، قدم ۳ هم به دو زیرقدم تقسیم شد. این زیرقدم `ProjectDna` (واحد ۰۲) و `CharacterAsset`/`LocationAsset` (واحد ۰۶) را به Room وصل کرد — با یک `ProjectDnaEntity` جدید (بلوپرینت ۱۵ اصلاً چنین Entity ای فهرست نکرده بود؛ هم‌جنس تصمیمات قبلاً تأییدشده‌ی «افزودن Entity وقتی Schema هنوز version=1 و منتشرنشده است»، پس این‌بار پرسیده نشد). تأیید شد `CharacterAsset` واقعاً فاقد `outfitOverride` است (طبق پیش‌بینی، هم‌راستا با ADR-012). هر دو تست Round-Trip کامل (نه نمونه‌ی مینیمال) در همان اجرای اول موفق شدند — بدون باگ جدید. جزئیات کامل در `docs/adr/019-unit15-step3a-dna-asset-deviations.md`.

### پس از واحد ۱۵ — قدم ۳، زیرقدم ۲ (تکمیل‌شده — آخرین زیرقدم واحد ۱۵)

`AudioContext` (واحد ۱۰) به Room وصل شد (`AudioContextEntity` جدید، هم‌جنس `ProjectDnaEntity`) و `SceneRepository` هم اضافه شد (طبق پیش‌بینی صریح دستور کار — `SceneEntity`/`SceneDao` قدم ۱ به‌تنهایی کافی نبودند). یافته‌ی مهم: `Scene` دامنه هیچ فیلد `projectId` ندارد (تأیید با grep) — این فیلد فقط در `SceneEntity` وجود دارد، پس `collectData` برای بارگذاری `ProjectDna` از `sceneEntity.projectId` استفاده می‌کند، نه از خودِ `Scene`. `PromptGenerationRepository.collectData` سرانجام نوشته شد: `Shot` → `Scene` → `ProjectDna` → `characters`/`objects`/`locations` → `camera`/`lighting`/`environment` (Resolve شده) → `audioContext` (nullable) را به ترتیب می‌خواند و یک `PromptGenerationInput` واقعی می‌سازد. هر سه تست (Scene، AudioContext، collectData) در همان اجرای اول موفق شدند — بدون باگ جدید. جزئیات کامل در `docs/adr/020-unit15-step3b-audio-collectdata-deviations.md`.

**باقی‌مانده (در آن زمان، طبق ADR-017):** `AutoSaveManager`/`BackupManager`/Export-Import — فقط امضای پیشنهادی، هنوز کد واقعی نداشتند (هر دوی `AutoSaveManager`/`BackupManager` در دو قدم بعدی تکمیل شدند — پایین را ببینید).

### پس از واحد ۱۵ — تکمیل `AutoSaveManager`

`data/repository/AutoSaveManager.kt` بدنه‌ی واقعی گرفت (تا اینجا فقط امضا داشت، طبق ADR-017): `suspend fun saveIfDirty(project: ProjectEntity, isDirty: Boolean): Result<Boolean>` با `ProjectDao` واقعی — `isDirty=true` ذخیره می‌کند و `lastModified` را به‌روز می‌کند، `isDirty=false` هیچ نوشتنی انجام نمی‌دهد. **تصمیم طراحی:** منطق Timer/Debounce واقعی («هر ۳۰ ثانیه») در این لایه پیاده نشد — به Lifecycle واقعی اپ (ViewModel/UI، واحد ۱۶، هنوز ساخته نشده) موکول شد؛ `intervalSeconds` به‌عنوان مقدار پیکربندی `public` نگه داشته شد تا آن لایه بتواند بخواندش. جزئیات کامل در `docs/adr/022-unit15-autosave-manager-deviations.md`.

`BackupManager`/Export-Import همچنان فقط امضای پیشنهادی دارند — قدم بعدی جداگانه.

### پس از واحد ۱۵ — تکمیل `BackupManager` (دومین و آخرین کلاس «فقط امضا»)

`data/repository/BackupManager.kt` بدنه‌ی واقعی گرفت: `createBackup()` یک Snapshot کامل پروژه (`Project` + تمام `Scene`/`Shot`/`Asset`/`ProjectDna`/`AudioContext` مرتبط، طبق grep در همه‌ی DAO ها) را به JSON سریالایز می‌کند و در فایل می‌نویسد؛ `restoreFromBackup(backupId)` همان Snapshot را بازمی‌خواند و با ترتیب «والد قبل از فرزند» (Project→Scene→Shot→AudioContext→Asset→ProjectDna — طبق هشدار REPLACE+CASCADE در ADR-017) در DAO ها می‌نویسد؛ `cleanOldBackups` فقط جدیدترین `maxBackupsToKeep` بک‌آپ را نگه می‌دارد.

`PromptBlueprint`/`RenderedOutput` (خروجی تولیدشده، از داده‌ی بالا دوباره قابل‌ساخت) و `Override`/`Version`/`EventLog` (entityId Polymorphic، بدون کوئری «همه‌ی X یک پروژه» در DAO فعلی) و `DependencyEdge` (بدون فیلد `projectId`، گراف سراسری) عمداً از Snapshot خارج ماندند.

**تصمیم معماری کلیدی:** چون I/O فایل واقعی (نه Room) لازم بود، این عملیات پشت `BackupFileStorage` (interface) انتزاع شد — `DeviceBackupFileStorage` با `Context.filesDir` پیاده‌سازی واقعی است؛ تست‌ها از یک Fake درون‌حافظه استفاده می‌کنند، بدون I/O واقعی دستگاه. مثل `AutoSaveManager`، هیچ Timer واقعی («هر ۵ دقیقه») ساخته نشد — به لایه‌ی UI آینده (واحد ۱۶) موکول شد؛ `backupIntervalMinutes` عمومی نگه داشته شد. جزئیات کامل در `docs/adr/023-unit15-backup-manager-deviations.md`.

**باقی‌مانده (در آن زمان):** فقط Export/Import — آخرین مورد فهرست ADR-017 (تکمیل شد — پایین را ببینید).

### 🎯 نقطه‌ی عطف: واحد ۱۵ کاملاً تکمیل شد — Export/Import (آخرین مورد فهرست ADR-017)

`data/repository/ExportImportRepository.kt` بدنه‌ی واقعی `exportProject`/`importProject` را اضافه کرد — با بازاستفاده‌ی کامل از زیرساخت `BackupManager` (`serializeFullProject`/`deserializeFullProject`/`restoreProjectFromSnapshot`/`BackupFileStorage`)، نه بازسازی از صفر. `exportProject` فایلی با نام معنادار (`${projectId}_export_<timestamp>.json`، نه نام محدود-به-تعداد مثل Backup) می‌نویسد؛ `importProject` قبل از هر نوشتنی در DAO ها، یکپارچگی ارجاعی واقعی (`validateReferentialIntegrity` تازه‌افزوده در `domain/storage/StorageValidation.kt`: Shot→Scene، Shot→Character/Object/Location) را بررسی می‌کند و در صورت شکست، `Result.failure` با جزئیات کامل برمی‌گرداند بدون این‌که هیچ داده‌ای نوشته شود.

**تصمیم کلیدی:** `IntegrityIssue(source, brokenReferenceTo, message)` مستقل از `ValidationIssue` سراسری تعریف شد (بدون `severity` — «ارجاع شکسته» طبق بلوپرینت همیشه Blocking است)؛ `validateReferentialIntegrity` نوع ساده‌ی `ShotReferences` می‌گیرد (نه `ShotEntity`/`Shot` کامل) تا بدون وابستگی به Room قابل‌تست بماند. جزئیات کامل و محدودیت شناخته‌شده‌ی Atomicity (عدم Wrap شدن `restoreProjectFromSnapshot` در یک `@Transaction` واحد — پذیرفته‌شده از ADR-023) در `docs/adr/024-unit15-export-import-deviations.md`.

**با این قدم، تمام موارد «فقط امضا»ی فهرست ADR-017 (`AutoSaveManager`، `BackupManager`، `Export/Import`) اکنون پیاده‌سازی واقعی و تست‌شده دارند — واحد ۱۵ (Project Storage) کاملاً تکمیل است.**

### 🎯 نقطه‌ی عطف: واحد ۰۲ — Migration بلوپرینت نسخه ۵ (بزرگ‌ترین Breaking Change تا کنون)

`domain/dna/ProjectDna.kt` طبق `docs/blueprints/02-dna-manager-v2.md` (نسخه ۵) کاملاً بازنویسی شد: `VisualStyle` از ۴ مقدار کلی به ۳۴ مقدار دقیق در ۵ دسته (`VisualStyleCategory`)؛ `Mood` (۲۵ مقدار، ۵ دسته) از `domain/story/` منتقل شد — `domain/dna/` از این پس تنها مالک این نام در کل پروژه است؛ `LightingStyle` (۲۲ مقدار، ۴ دسته) از `domain/sceneconditions/` منتقل شد؛ `ContrastLevel` (رفع باگ واقعی — تأییدشده با grep: `MasterPalette.globalContrast` اشتباهاً `SaturationLevel` بود)؛ `AspectRatio` (۱۱ مقدار — `OutputConstraints.aspectRatio` از `String` به enum، Breaking Change واقعی)؛ `QualityDirectives`/`GlobalMoodBase`(کامل، `primaryEmotion: Mood`)/`LightingPreference` طبق بلوپرینت اضافه/کامل شدند.

**دو تناقض واقعی بین بلوپرینت/`type-registry.md`/کد موجود** پیدا و توسط معمار حل شدند (نه به‌صورت خودسرانه): (۱) `stylePreferences`/`overrideRules` — نمونه‌ی JSON بلوپرینت هنوز داشت، ولی `data class ProjectDna` مفهومی و ردیف `type-registry.md` هر دو حذف کرده بودند؛ تصمیم نهایی: حذف کامل از `ProjectDna` (`requiresApprovalForOverride` هم حذف شد). (۲) `LightingStyle` جدید نامی دقیقاً معادل `NOIR`/`DRAMATIC`/`NATURAL` قدیمی نداشت؛ تصمیم نهایی: `NOIR→LOW_KEY`، `DRAMATIC→DRAMATIC_LIGHT`، `NATURAL→NATURAL_LIGHT` (در `LightingValidation.kt` و همه‌ی تست‌های مصرف‌کننده اعمال شد).

**مصرف‌کنندگان مستقیم اصلاح‌شده** (با grep در کل پروژه پیدا شدند): `domain/story/StoryContext.kt`+`StoryValidation.kt` (import `Mood` جدید)؛ `domain/sceneconditions/LightingModels.kt`+`LightingValidation.kt` (import `LightingStyle` جدید + نگاشت نام)؛ `data/repository/ProjectDnaDto.kt`+`DnaAssetMappers.kt`+`DtoMappers.kt` (واحد ۱۵، بدون اصلاحشان کامپایل نمی‌شد)؛ ۶ فایل تست (`DnaValidationTest`, `StoryValidationTest`, `LightingValidationTest`, `ShotSettingsResolutionTest`, `PromptAssemblyTest`, `ProjectDnaRepositoryTest`, `PromptGenerationRepositoryTest`).

**خارج از Scope این قدم (طبق تصریح معمار):** واحد ۰۳ (`getPacingFromEmotion`، هنوز `String` می‌گیرد نه `Mood`)، واحد ۰۸ (`mapMoodToLighting`، هنوز `String`/`LightingPreset` رشته‌ای است)، و `universal-technical-variables.md` — همگی در Migration های جداگانه‌ی بعدی به این enum های جدید وصل می‌شوند. جزئیات کامل در `docs/adr/027-unit02-dna-manager-v5-migration.md`.

### 🎯 نقطه‌ی عطف: واحد ۰۶ — Migration بلوپرینت نسخه ۵ (بزرگ‌ترین Migration پروژه از نظر تعداد نوع/فیلد جدید)

`domain/asset/AssetModels.kt`/`AssetContinuity.kt`/`AssetValidation.kt` طبق `docs/blueprints/06-asset-and-continuity-v2.md` (نسخه ۵) در دو قدم متوالی Migrate شدند. **تصمیم طراحی کلیدی (Option A):** `ObjectAsset` قبلاً با `LocationAsset` مشترک بود (یک فیلد `assetType` تفکیک‌کننده)؛ بررسی Blast Radius با grep نشان داد جداسازی کامل هزینه‌ی معقولی دارد (بدون نیاز به Migration اسکیمای Room، چون `AssetEntity` از قبل یک جدول عمومی Blob است) — پس `ObjectAsset` اکنون نوع کاملاً مستقل است، مطابق توصیه‌ی صریح بلوپرینت و تأیید مستقل `type-registry.md`.

**مصرف‌کنندگان اصلاح‌شده** (با grep در کل پروژه پیدا شدند): `data/repository/AssetRepository.kt` (توابع جدید `saveObjectAsset`/`loadObjectAssets`، `loadLocationAssets` جایگزین `loadAssets` قدیمی مشترک)، `data/repository/AssetDto.kt` (`ObjectAssetDto` جدید؛ `characterTier`/`subtype` در سطح DTO عمداً پیش‌فرض محافظه‌کارانه دارند — `MAIN`/`GENERAL_PROP` — برای Backward Compatibility داده‌ی واقعاً سریالایز‌شده‌ی قدیمی، درحالی‌که نوع دامنه‌ی Kotlin همچنان این دو فیلد را الزامی نگه می‌دارد)، `data/repository/DnaAssetMappers.kt`، `domain/promptengine/PromptEngineModels.kt` (`PromptGenerationInput.objects` از `List<LocationAsset>` به `List<ObjectAsset>` تغییر کرد)، `data/repository/PromptGenerationRepository.kt`، و ۶ فایل تست.

**تصمیم مستند دیگر:** `characterTier: CharacterTier` روی `CharacterAsset` الزامی ماند (بدون پیش‌فرض) — با اینکه بلوپرینت پیشنهاد پیش‌فرض `MAIN` هم داده بود، grep تأیید کرد هر ۶ محل ساخت واقعی `CharacterAsset` در کل پروژه فقط Named Argument دارند، پس این تصمیم هیچ خطر Silent Breakage ندارد. جزئیات کامل هر دو بخش این Migration در `docs/adr/029-unit06-continuity-tiers-migration-part1.md`.

### 🎯 نقطه‌ی عطف: واحد ۱۴ — دستور تصویر رفرنس + اتصال negativePrompt (Migration بلوپرینت نسخه ۳)

دو کمبود مستندشده رفع شدند — جزئیات کامل در `docs/adr/030-unit14-reference-image-and-negative-prompt-migration.md`:

1. **`buildReferenceImageInstruction` جدید در `Renderer.kt`:** وقتی یک Shot به Asset هایی با تصویر رفرنس متصل است و مدل هدف `supportsImagePrompt=true` دارد، `renderBlueprintToText` اکنون یک جمله‌ی دستوری کلی («use the attached reference image(s)...») در جایگاه دوم پرامپت تزریق می‌کند — هرگز نام فایل/مسیر تصویر را درج نمی‌کند. با grep تأیید شد هیچ تست موجودی سناریوی `imageReferences` غیرخالی را پوشش نمی‌داد، پس این تغییر هیچ تست قبلی را نشکست.
2. **اتصال `negativePrompt` (تکمیل ADR-028):** `PromptBlueprint.negativePrompt: String = ""` اضافه شد؛ `assemblePromptBlueprint` (واحد ۱۱) اکنون `resolveNegativePrompt` (واحد ۰۵) را با `shot`/`dna.qualityDirectives` واقعی صدا می‌زند؛ `Renderer.kt` این مقدار را فقط برای پروفایل‌هایی که `supportsNegativePrompt=true` دارند در هر دو مسیر متن و JSON اعمال می‌کند — بی‌صدا نادیده گرفته می‌شود وقتی مدل پشتیبانی نمی‌کند.

### 🎯 نقطه‌ی عطف: شروع واحد ۰۱ب (AI Story Breakdown) — قدم اول: Prompt Builder + Chunk Combiner

اولین قدم از ساخت یک واحد کاملاً جدید — `domain/storybreakdown/` — طبق `docs/blueprints/01b-ai-story-breakdown.md` (نسخه ۲). این قدم فقط بخش الف (Prompt Builder) و بخش پ (Chunk Combiner) را پیاده کرد؛ بخش‌های ب (AI Connector)، ت (JSON Doctor)، ث (Story-to-Domain Mapper) در قدم‌های بعدی می‌آیند — اما **این قدم خودش کامل، مستقل، و کامپایل‌شونده است** (بر خلاف یک Migration قبلی که عمداً یک وضعیت میانی ناقص رها شد؛ طبق الزام صریح این مرحله، هر قدم این واحد باید به‌تنهایی `BUILD SUCCESSFUL` بدهد).

- **`PromptBuilder.kt`:** `StoryBreakdownRequest`، `buildStoryBreakdownPrompt` (پرامپت متنی آماده برای AI بیرونی، شامل سبک/حال‌وهوا از `StoryContext` واحد ۰۱، داستان آزاد کاربر، و دستور فرمت JSON خروجی + `[CONTINUE]`)، سه Rule اعتبارسنجی (طول داستان، محدوده‌ی تعداد شات، هشدار تعداد شات بالا).
- **`ChunkCombiner.kt`:** `isPartialResponse`/`smartCombineChunks` (چسباندن پاسخ‌های چندبخشی AI قبل از Parse JSON)، Rule هشدار برای تکه‌ی آخر ناقص.
- جزئیات کامل (شامل تصمیم مستند درباره‌ی `genre: List<Genre>` به‌جای مقدار تکی بلوپرینت) در `docs/adr/032-unit01b-story-breakdown-step1-prompt-builder-chunk-combiner.md`.

### 🎯 نقطه‌ی عطف: واحد ۰۱ب — قدم دوم: JSON Doctor

بخش ت بلوپرینت پیاده شد — `JsonDoctor.kt`: `diagnoseJsonError` (که در بلوپرینت `TODO()` بود، اکنون الگوریتم واقعی دارد: تشخیص کاما اضافه، نقل‌قول‌های تزئینی، پاسخ ناتمام، براکت نامتعادل، یا `UNKNOWN`)، `attemptAutoFix` (تعمیر خودکار کاما اضافه/نقل‌قول تزئینی، عیناً طبق بلوپرینت)، و `repairJson` (جریان کامل Parse→Diagnose→AutoFix، با نتیجه‌ی `JsonRepairResult.Success`/`NeedsManualRepair`). Rule 7 (JSON بعد از تعمیر هنوز نامعتبر) و Rule 8 (کلیدهای الزامی `characters`/`locations`/`shots` غایب) با `ValidationIssue` سراسری پیاده شدند. تصمیم مستند مهم: ترتیب بررسی انواع خطا (`INCOMPLETE_RESPONSE` قبل از `UNMATCHED_BRACKET`) تا پاسخ‌های واقعاً بریده‌شده از خطاهای ساختاری معمولی تفکیک شوند — جزئیات کامل در `docs/adr/033-unit01b-story-breakdown-step2-json-doctor.md`.

## Stack

- **زبان:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **معماری:** MVVM ساده (ViewModel + StateFlow) — بدون فریمورک DI در فاز اول
- **ذخیره‌سازی:** Room 2.8.4 + KSP (روی SQLite) + kotlinx.serialization (برای فیلدهای `*DataJson`) — پشتیبانی از چند پروژه‌ی همزمان؛ کاملاً پیاده‌سازی و به دامنه وصل شده (واحد ۱۵ تکمیل‌شده)
- **اتصال AI (اختیاری):** Ktor Client — فقط وقتی کاربر کلید API شخصی وارد کند

## ساختار

```
app/src/main/java/com/operaboys/cinemashotgenerator/
├── data/    → Room entities, DAO, Database, Repository (واحد ۱۵)
│   ├── entity/ → ۱۲ Room Entity (Project/Scene/Shot/Asset/PromptBlueprint/RenderedOutput/Override/Version/DependencyEdge/EventLog/ProjectDna/AudioContext)
│   ├── dao/    → DAO های suspend/Flow متناظر + ProjectTransactionDao (اثبات Atomicity)
│   ├── repository/ → Settings/Versioning/ImpactAnalysis/ProjectDna/Asset/Scene/AudioContext Repository + PromptGenerationRepository.collectData + DTO محلی (data ← domain مجاز، domain ← data ممنوع)
│   └── AppDatabase.kt → RoomDatabase + Singleton Provider (بدون DI)
├── domain/  → مدل‌های دامنه و منطق کسب‌وکار
│   ├── story/  → واحد ۰۱: Story Wizard + Human Override
│   ├── storybreakdown/ → واحد ۰۱ب: AI Story Breakdown (Prompt Builder + Chunk Combiner + JSON Doctor — قدم‌های اول و دوم؛ AI Connector/Mapper در قدم‌های بعدی)
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
