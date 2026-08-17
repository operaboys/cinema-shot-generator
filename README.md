# Cinema Shot Generator

اپ اندرویدِ تک‌کاربره و On-Device (بدون سرور) برای تولید پرامپت‌های متنی سینمایی برای مدل‌های AI ویدیوساز (Sora، Veo، Runway، Kling و مشابه). این اپ فقط **متن** تولید می‌کند، نه تصویر یا ویدیو.

## وضعیت فعلی

**🎯 واحد ۱۶ (UI/User Workflow) به‌طور کامل تکمیل شد — هر ۶ فاز.** Room/Persistence (واحد ۱۵) کامل و به‌طور واقعی به دامنه وصل است؛ فاز ۰ (پایه‌ی مشترک)، فاز ۱ (App Shell: Home/Projects/Studio Shell/Assets Container)، فاز ۲ (Story Tab + AI Story Breakdown + DNA Tab)، فاز ۳ (Asset Library)، فاز ۴ (Scene + Shot Composer، شامل هر ۴ Tab کامل) و فاز ۵ (Validation → Output Delivery) پیش‌تر تکمیل شده بودند. **فاز ۶ (آخرین فاز) اکنون کامل است** — قدم ۱ (صفحه‌ی Settings + اتصال واقعی Timer دوره‌ای Auto-Save) و قدم ۲ (آخرین قدم کل واحد ۱۶: صفحه‌ی Backups + بازبینی یکپارچه‌ی نهایی کل اپ) — پایین را ببینید. لیست تجمیع‌شده‌ی تمام محدودیت‌های شناخته‌شده‌ی باقی‌مانده (از همه‌ی فازها) در بخش «محدودیت‌های شناخته‌شده» پایین صفحه است.**

Scaffold اولیه‌ی «Hello World» جای خود را به صفحات واقعی داده است. لایه‌ی دامنه‌ی خالص (Kotlin، بدون Room) این واحدها پیاده‌سازی شده:

- **واحد ۰۱ — Story & Override (تأییدشده هم‌راستا با بلوپرینت نسخه ۴):** `domain/story/` — مدل‌های Story Wizard، قوانین اعتبارسنجی، مدل‌های Human Override. بازبینی این قدم تأیید کرد کد از قبل کاملاً با `docs/blueprints/01-story-and-override-v2.md` (نسخه ۴) هم‌راستا بود (فیلدهای مسطح `moodPrimary`/`moodSecondary`، بدون فرمت تودرتوی قدیمی؛ `Mood` از `domain.dna` طبق ADR-027) — فقط کامنت هدر ۳ فایل (`HumanOverride.kt`, `OverrideActions.kt`, `StoryValidation.kt`) که هنوز به بلوپرینت بدون `-v2` اشاره می‌کردند اصلاح شد.
- **واحد ۰۱ب — AI Story Breakdown (واحد کاملاً جدید، منطق دامنه کامل شد):** `domain/storybreakdown/` — تجزیه‌ی داستان آزاد کاربر با کمک یک AI متنی بیرونی: `PromptBuilder.kt` (ساخت پرامپت درخواست)، `ChunkCombiner.kt` (چسباندن پاسخ‌های چندبخشی)، `JsonDoctor.kt` (تشخیص/تعمیر خودکار خطای JSON)، `StoryToDomainMapper.kt` (تبدیل خروجی ساده‌ی AI به `CharacterAsset`/`LocationAsset`/`ObjectAsset`/`Scene`/`Shot` واقعی)، `AiConnector.kt` (قرارداد مسیر ارسال مستقیم API — HTTP واقعی کار آینده است). جزئیات کامل در `docs/adr/032` تا `docs/adr/035`.
- **واحد ۰۲ — DNA Manager (Migration بلوپرینت نسخه ۵ کامل شد — بزرگ‌ترین Breaking Change تا کنون):** `domain/dna/` — `VisualStyle` اکنون ۳۴ مقدار دقیق در ۵ دسته (`VisualStyleCategory`)؛ `Mood` (۲۵ مقدار، ۵ دسته) از `domain/story/` به اینجا منتقل شد و تنها مالک این نام در کل پروژه است؛ `LightingStyle` (۲۲ مقدار، ۴ دسته) از `domain/sceneconditions/` به اینجا منتقل شد؛ `ContrastLevel` (رفع باگ واقعی: `MasterPalette.globalContrast` اشتباهاً `SaturationLevel` بود)؛ `AspectRatio` (۱۱ مقدار — `OutputConstraints.aspectRatio` از `String` به enum، Breaking Change واقعی)؛ `QualityDirectives`/`GlobalMoodBase`/`LightingPreference` کامل شدند؛ `stylePreferences`/`overrideRules` طبق تصمیم صریح معمار کاملاً حذف شدند؛ `validateColorPalette` (Rule جدید) اضافه شد. جزئیات کامل، دو تناقض واقعی کشف‌شده بین بلوپرینت/type-registry (و نحوه‌ی حل‌شان)، و فهرست کامل مصرف‌کنندگان اصلاح‌شده در `docs/adr/027-unit02-dna-manager-v5-migration.md`.
- **واحد ۰۶ — Asset & Continuity (Migration بلوپرینت نسخه ۵ کامل شد):** `domain/asset/` — `CharacterTier` (MAIN/SECONDARY/BACKGROUND) با `defaultLockLevelForTier`؛ Hard Lock قدیمی (فقط Allowed/Blocked مطلق) اکنون شرطی به سه سطح مستقل `CharacterContinuityLevel`(FULL/MEDIUM/NONE)/`LocationContinuityLevel`(STYLE)/`PropContinuityLevel`(FORM) است — `UpdateResult` حالت سوم `Warned` گرفت، اما سطح **FULL** بایت‌به‌بایت همان رفتار Blocking بدون استثنای قبلی را حفظ کرده. `ObjectAsset` از `LocationAsset` مشترک قدیمی کاملاً تفکیک شد (Option A طبق توصیه‌ی بلوپرینت) — `LocationAsset` اکنون فقط مکان است. Rule های جدید ۱۰ (size/materialAndColor الزامی)، ۱۱ (basePrompt نباید whitespace-only باشد، روی هر سه نوع Asset)، ۱۲ (ObjectAsset.subtype — تضمین‌شده در سطح Type System، بدون تابع Runtime). جزئیات کامل در `docs/adr/029-unit06-continuity-tiers-migration-part1.md`. **تکمیل شد:** `PhysicalAppearance` اکنون `Gender` enum واقعی دارد (نه رشته‌ی آزاد)؛ `height`/`build`/`hair`/`facialFeatures` nullable شدند؛ `physicalFeatures`/`toPromptString()` اضافه شدند — جزئیات در `docs/adr/031-unit06-physical-appearance-gender-migration.md`.
- **واحد ۰۷ — Validation & Consistency Engine:** `domain/validation/` — Validation Engine (Severity/ValidationIssue/ValidationReport سراسری)، Logic Conflict Checker (اکنون با `MotionLevel`/`CinematicMode` واقعی)، Dependency Resolver (تحلیل اثر، تشخیص وابستگی دایره‌ای).
- **واحد ۰۳ — Visual Identity:** `domain/visualidentity/` — Style Matrix (ترکیب کیفی سبک‌ها، بررسی سازگاری)، Cinematic Language (صاحب اصلی `CinematicMode`، تعیین ریتم Hybrid، اعتبارسنجی مدت شات).
- **واحد ۰۵ — Shot Engine (Migration بلوپرینت نسخه ۳: negativePromptOverride):** `domain/shot/` — مدل‌های Shot/Beat/SoundProfile، صاحب اصلی `MotionLevel`، ارث‌بری از Scene (از طریق `inheritOrOverride` واحد ۰۴)، انتخاب Outfit (با `CharacterAsset` واقعی واحد ۰۶)، قوانین اعتبارسنجی Rule 1 تا Rule 5. `Shot.camera`/`.lighting`/`.environment` اکنون `SourcedSettings<T>` تایپ‌شده‌اند (نه `Map<String,String>` جای‌نگهدار)؛ `resolveCameraSettings`/`resolveLightingSettings`/`resolveEnvironmentSettings` مقدار نهایی را از `source` واقعی و `inheritOrOverride` تولید می‌کنند (ADR-013). `Shot.negativePromptOverride: String?` (پیش‌فرض `null`) اضافه شد — منبع ارث‌بری‌اش برخلاف camera/lighting/environment، **DNA پروژه** است نه Scene؛ `resolveNegativePrompt(shot, dnaNegativePrompt)` این ارث‌بری را حل می‌کند. **یافته‌ی این Migration** (کل مسیر مصرف واقعی هنوز سیم‌کشی نشده بود) در Migration واحد ۱۴ رفع شد — جزئیات در `docs/adr/028-unit05-negative-prompt-override-migration.md` و `docs/adr/030-unit14-reference-image-and-negative-prompt-migration.md`.
- **واحد ۰۴ — Scene Engine (`locationAssetId` برای اتصال به کتابخانه‌ی Location اضافه شد):** `domain/scene/` — مدل‌های Scene/SceneLocation/SceneConstraints، **صاحب اصلی `inheritOrOverride`** (تنها نسخه‌ی موجود در پروژه)، حذف Scene، قوانین اعتبارسنجی Rule 1/4/5. `Scene.locationAssetId: String?` (رفع F2 ممیزی pre-Unit 16 — جزئیات در `docs/adr/038-unit04-scene-location-asset-link.md`).
- **واحد ۰۹ — Camera & Motion:** `domain/camera/` — Camera System (زاویه/فاصله/لنز/حرکت پایه و پیشرفته، ۵ قانون اعتبارسنجی)، Motion Intensity (سرعت/شدت سوژه، سرعت دوربین، Motion Blur).
- **واحد ۰۸ — Scene Conditions (LightingSettings/EnvironmentSettings برای Tab «نور و محیط» واحد ۱۶ گسترش یافتند):** `domain/sceneconditions/` — Lighting System (Mood-to-Lighting، ۵ قانون اعتبارسنجی)، Environment & Weather Engine (Environment-to-Sound، ۸ قانون اعتبارسنجی، یکی مشترک با واحد ۰۷). `LightingSettings` اکنون ۵ فیلد جدید nullable دارد (`fillLight`, `colorTemperature`, `shadowQuality`, `lightSourceCount`, `lightingMotivation`)؛ `EnvironmentSettings` هم ۶ فیلد جدید (`weatherIntensity`, `windStrength`, `groundState`, `visibility`, `temperatureFeel`, `environmentalMotion: List<...>`) — هر دو کاملاً Backward Compatible. `PromptAssembly.kt` (واحد ۱۱) عمداً دست‌نخورده ماند چون بلوپرینت ۱۱ به این فیلدها ارجاع نمی‌دهد (تأییدشده با grep). **یافته‌ی مهم فراتر از Blast Radius اعلام‌شده:** `data/repository/DtoMappers.kt`/`SceneConditionsDto.kt` هم به‌روزرسانی شدند — بدون این تغییر، مقادیر ویرایش‌شده‌ی کاربر در این فیلدهای جدید حین ذخیره در Room بی‌صدا گم می‌شدند. جزئیات کامل در `docs/adr/036-unit08-lighting-environment-ui-fields-migration.md`.
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
5. **Migration نسخه ۳ (`validateLogicConsistency`، ADR-073):** `weather`/`lightingMotivation` از String به `WeatherType`/`LightingMotivation` واقعی واحد ۰۸ تبدیل شدند؛ `locationType` هم (تصمیم مستقل، فراتر از دستور صریح بلوپرینت) به `domain.scene.LocationType` تبدیل شد چون `Scene.location.type` از قبل همان enum را داشت.

**یادداشت‌های باقی‌مانده (نه سؤال باز جدی):** هم‌پوشانی مفهومی `SubjectSpeed` (واحد ۰۹) و `MotionLevel` (واحد ۰۵)؛ همپوشانی `canDeleteAsset` (واحد ۰۷) و `validateAssetDeletion` (واحد ۰۶)؛ **سه نوع «صدای محیطی» مستقل** — `domain.shot.AmbientSound` (۴ فیلد، از ADR-074 با `source`)، `domain.sceneconditions.AmbientSoundSuggestion` (۳ فیلد، بدون `source`)، `domain.audio.AmbientSound` (۴ فیلد، با `source`) — یکی‌سازی واقعی‌شان (نه فقط هماهنگی ساختار فیلد که در ADR-074 انجام شد) به تصمیم معمار درباره‌ی پایپ‌لاین بین واحدهای ۰۵/۰۸/۱۰ نیاز دارد (جزئیات در ADR-009 و ADR-011؛ ADR-012 تأیید کرد که این سه‌گانگی برای `audioDescription` واحد ۱۱ بی‌خطر است، چون فقط فیلدهای مشترک `type`/`intensity` خوانده می‌شوند). در مقابل، `ActionSound`/`CharacterSound` واحد ۱۰ همان نمونه‌ی واحد ۰۵ را بازاستفاده کردند (بدون تکرار).

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

**خارج از Scope این قدم (طبق تصریح معمار):** واحد ۰۳ (`getPacingFromEmotion`، هنوز `String` می‌گیرد نه `Mood`)، واحد ۰۸ (`mapMoodToLighting`، هنوز `String`/`LightingPreset` رشته‌ای است)، و `universal-technical-variables.md` — همگی در Migration های جداگانه‌ی بعدی به این enum های جدید وصل می‌شوند. جزئیات کامل در `docs/adr/027-unit02-dna-manager-v5-migration.md`. **(به‌روزرسانی: هر دو تابع بعداً در `docs/adr/039-unit03-unit08-mood-type-safety-migration.md` کاملاً Type-Safe شدند — رفع F3/F4 ممیزی pre-Unit 16، پایین را ببینید.)**

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

### 🎯 نقطه‌ی عطف: واحد ۰۱ب — قدم سوم: Story-to-Domain Mapper (حیاتی‌ترین بخش این واحد)

بخش ث بلوپرینت پیاده شد — `StoryToDomainMapper.kt`: `mapAiCharacterToAsset`/`mapAiLocationToAsset`/`mapAiObjectToAsset` (تبدیل خروجی ساده‌ی AI به `CharacterAsset`/`LocationAsset`/`ObjectAsset` واقعی با مقادیر پیش‌فرض معقول — `role`→`CharacterTier`، `gender`→`Gender` enum، هر دو case-insensitive)، `groupAiShotsIntoScenes` (گروه‌بندی شات‌ها بر اساس نام صحنه)، `mapAiShotToShot` (اتصال به Asset ها بر اساس تطبیق نام، با گزارش صریح نام‌های یافت‌نشده به‌جای نادیده‌گرفتن بی‌صدا)، و `processAiResponse` که کل جریان بخش پ→ت→ث را به هم وصل می‌کند (`ChunkCombiner` → `JsonDoctor` → Parse → سه Mapper → `StoryBreakdownResult` نهایی). Rule 9 (نام یافت‌نشده) و Rule 10 (مغایرت تعداد شات با هدف) با `ValidationIssue` سراسری پیاده شدند. جزئیات کامل تصمیم‌ها (شناسه‌های تایپ‌شده، Placeholder های محیط/مکان، ساختار `ProcessAiResponseResult` سه‌حالته) در `docs/adr/034-unit01b-story-breakdown-step3-story-to-domain-mapper.md`.

### 🎯 نقطه‌ی عطف: واحد ۰۱ب (AI Story Breakdown) کامل شد — قدم چهارم و آخر: AI Connector Profile

بخش ب بلوپرینت (مسیر ۲: AI Connector Profile) پیاده شد — `AiConnector.kt`: `AiConnectorProfile`، `createCustomAiConnectorProfile`، Rule 4 (کلید API خالی) و Rule 5 (پیام خطای واقعی سرویس). **تصمیم مستند (Option A):** پیاده‌سازی واقعی HTTP Call با Ktor Client عمداً به یک قدم کاملاً جداگانه‌ی آینده موکول شد — دقیقاً طبق اجازه‌ی صریح خودِ بلوپرینت («می‌تواند به زیرقدم بعدی موکول شود اگر حجم واقعی بزرگ‌تر از انتظار بود»)؛ `sendToAiConnector` عمداً `TODO()` است (تست صریح تأیید می‌کند `NotImplementedError` می‌دهد، نه یک باگ خاموش). جزئیات کامل تصمیم (شامل بررسی هزینه‌ی واقعی Option B) در `docs/adr/035-unit01b-story-breakdown-step4-ai-connector.md`.

**با این قدم، واحد ۰۱ب (AI Story Breakdown) از نظر منطق دامنه کامل است — خلاصه‌ی هر چهار قدم:**

| قدم | فایل(ها) | بخش بلوپرینت | ADR |
|---|---|---|---|
| ۱ | `PromptBuilder.kt`, `ChunkCombiner.kt` | الف (Prompt Builder)، پ (Chunk Combiner) | `docs/adr/032-...md` |
| ۲ | `JsonDoctor.kt` | ت (JSON Doctor) | `docs/adr/033-...md` |
| ۳ | `StoryToDomainMapper.kt` | ث (Story-to-Domain Mapper) | `docs/adr/034-...md` |
| ۴ | `AiConnector.kt` | ب (AI Connector Profile) | `docs/adr/035-...md` |

**کار آینده‌ی شناخته‌شده (نه بدهی پنهان — صریحاً مستند شده، نه ادعای اتمام کامل):**
- پیاده‌سازی واقعی HTTP در `sendToAiConnector` با Ktor Client (که نسخه‌اش از قبل در `gradle/libs.versions.toml` اعلام شده اما هنوز به هیچ ماژولی وصل نشده) — نیازمند انتخاب Engine مناسب Android و تست با Mock Engine.
- فهرست واقعی `BUILTIN_AI_CONNECTOR_PROFILES` (Claude API، OpenAI API، Gemini، ...) با جزئیات دقیق request/response از مستندات رسمی هرکدام — در حال حاضر عمداً خالی است.
- اتصال واقعی UI (واحد ۱۶، هنوز ساخته نشده) که گام تعاملی Rule 11 (تأیید نهایی کاربر پیش از اعمال قطعی نتیجه‌ی Mapper) را پیاده می‌کند.

### 🎯 نقطه‌ی عطف: `domain/workflow/` ساخته شد — رفع یافته‌ی F1 ممیزی pre-Unit 16

اولین قدم آماده‌سازی واحد ۱۶ (User Workflow / اولین UI واقعی پروژه)، مستقیماً یافته‌ی
F1 ممیزی `docs/audit/pre-unit16-audit.md` را رفع می‌کند: `domain/workflow/` که کاملاً
غایب بود (در حالی که `docs/reference/type-registry.md` از قبل جدول کامل آن را ✅
نشان می‌داد)، اکنون ساخته شد — `WorkflowModels.kt`: `WorkflowStep`، `StepStatus`،
`ShotListViewMode`، `FeedbackType`، `WorkflowState` (با `progressPercentage`)،
`canJumpToStep` (هرگز پیشروی را Block نمی‌کند، فقط هشدار می‌دهد)، و `QualityScore`/
`evaluatePromptQuality` (ابزار اختیاری خودارزیابی کیفیت پرامپت — بلوپرینت این تابع
را `TODO()` گذاشته بود؛ چون ابزاری کمکی و کم‌ریسک است، یک پیاده‌سازی حداقلی واقعی
نوشته شد، نه TODO). جزئیات کامل تصمیم در `docs/adr/037-unit16-workflow-models-quality-score.md`.

این پکیج کاملاً مستقل است — فقط یافته‌ی F1 ممیزی را رفع می‌کند؛ یافته‌ی F2 در قدم
بعدی رفع شد (پایین را ببینید).

### 🎯 نقطه‌ی عطف: `Scene` به کتابخانه‌ی `LocationAsset` وصل شد — رفع یافته‌ی F2 ممیزی pre-Unit 16 (آخرین یافته‌ی 🔴)

`Scene.locationAssetId: String? = null` اضافه شد — ارجاع اختیاری به `LocationAsset.assetId`
واحد ۰۶، در کنار `SceneLocation` توصیفی موجود (نه جایگزین آن)؛ اکنون فرم «تنظیمات
Scene» بلوپرینت ۱۶ می‌تواند واقعاً از کتابخانه‌ی Location انتخاب کند، نه یک Label
متنی آزاد. **بررسی مستقل مهم:** پیشنهاد اولیه‌ی افزودن یک فیلد دوم به `Shot`
(الگوی `SourcedSettings<T>`، مشابه camera/lighting/environment) برای ارث‌بری از
Scene رد شد — با grep تأیید شد `Shot.locationIds: List<String>` موجود از قبل
دقیقاً همان نقش را (هم در Rule اعتبارسنجی BLOCKING «حداقل یک Subject»، هم در
بارگذاری واقعی `LocationAsset` حین تولید پرامپت) ایفا می‌کند؛ افزودن فیلد دوم فقط
یک مسیر موازیِ سیم‌کشی‌نشده می‌ساخت (همان الگوی «فیلد تزئینی» که ADR-036 از آن
پرهیز کرد). به‌جای آن، تابع خالص `resolveSceneLocation(shot, scene): List<String>`
در `ShotSettingsResolution.kt` اضافه شد که تهی‌بودن `locationIds` موجود را
به‌عنوان سیگنال ارث‌بری/Override بازتفسیر می‌کند، و در
`PromptGenerationRepository.collectData` سیم‌کشی شد. `SceneDto`/`SceneMappers.kt`
هم برای این فیلد جدید کامل شدند (طبق الگوی ADR-036 — بدون این تغییر، مقدار
انتخاب‌شده‌ی کاربر حین ذخیره در Room بی‌صدا گم می‌شد). جزئیات کامل در
`docs/adr/038-unit04-scene-location-asset-link.md`.

**با این قدم، هر دو یافته‌ی 🔴 (مسدودکننده) ممیزی pre-Unit 16 (F1 و F2) رفع
شده‌اند** — تنها ۷ یافته‌ی 🟡 و ۳ یافته‌ی ⚪ غیرمسدودکننده از آن ممیزی باقی مانده‌اند.

### 🎯 نقطه‌ی عطف: Type Safety کامل `mapMoodToLighting`/`getPacingFromEmotion` — رفع یافته‌های F3 و F4 ممیزی pre-Unit 16

هر دو تابع (باقی‌مانده از قبل از Migration واحد ۰۲) رشته‌ی خام می‌گرفتند/می‌دادند
و اکنون کاملاً Type-Safe شدند:

- **`mapMoodToLighting` (واحد ۰۸، F3):** امضا به `mood: Mood` تغییر کرد؛
  `LightingPreset` کاملاً Type-Safe شد (هر ۶ فیلد enum واقعی متناظرش را می‌گیرد —
  `LightingStyle`/`KeyLightPosition`/`FillLight`/`ContrastRatio`/`ShadowQuality`/
  `ColorTemperature` — نه `String` آزاد). تابع اکنون Total است (بدون `null`):
  ۶ مقدار اصلی بلوپرینت پیش‌فرض نوانس‌دار خودشان را حفظ کردند؛ ۱۹ مقدار باقی‌مانده‌ی
  `Mood` از یک پیش‌فرض معقول در سطح `MoodCategory` استفاده می‌کنند.
- **`getPacingFromEmotion` (واحد ۰۳، F4):** امضا به `emotion: Mood` تغییر کرد و
  اکنون Total روی `Mood.category` است (`HIGH_ENERGY`/`DARK`→`FAST_CUT`،
  `EMOTIONAL`/`CALM`→`LONG_TAKE`، `POSITIVE`→`BALANCED`) — به‌جای فهرست دستی چند
  رشته که با enum واقعی `Mood` هم‌خوان نبودند.

**یافته‌ی اصلاحی نسبت به پیش‌بررسی دستور کار:** با grep مستقیم در `Mood` enum
تأیید شد `Mood.SERENE` و `Mood.CONTEMPLATIVE` (هر دو دسته‌ی `CALM`) برخلاف ادعای
اولیه‌ی دستور کار **واقعاً وجود دارند** — رویکرد `MoodCategory`-محور این عدم‌قطعیت
را بی‌اثر می‌کند، چون اصلاً به تطبیق تک‌تک رشته‌ها وابسته نیست. `type-registry.md`
از قبل امضای هدف (Mood-typed، غیر-nullable) را دقیقاً مستند کرده بود — دقیقاً
همان الگوی F1 — پس نیازی به تغییر در آن نبود. جزئیات کامل در
`docs/adr/039-unit03-unit08-mood-type-safety-migration.md`.

### 🎯 نقطه‌ی عطف: اصلاح مستندسازی + Rule 8 — رفع یافته‌های F7، F8، F9 ممیزی pre-Unit 16

سه یافته‌ی کوچک و مستقل با هم رفع شدند:

- **F7 (`type-registry.md`، ردیف `LightingSettings`):** فیلد جاافتاده‌ی
  `lightSourceCount` اضافه و نام اشتباه `motivation` به `lightingMotivation`
  اصلاح شد — ردیف اکنون دقیقاً با ۸ فیلد واقعی `LightingModels.kt` (به همان
  ترتیب) یکی است.
- **F8 (`type-registry.md`، ردیف `EnvironmentSettings`):** فیلد نادرست/بی‌ربط
  `locationType` (که هیچ‌جا در `EnvironmentSettings` واقعی وجود نداشت) حذف شد؛
  `windStrength` و `environmentalMotion` (هر دو کاملاً غایب از سند بودند) اضافه
  شدند — ردیف اکنون دقیقاً با ۷ فیلد واقعی `EnvironmentModels.kt` یکی است.
- **F9 (`ShotValidation.kt`، Rule 8 جدید):** `validateNegativePromptOverride(shot)`
  اضافه شد — طبق `docs/blueprints/05-shot-engine-v2.md:272`، `negative_prompt_override`
  اگر `null` نباشد نباید whitespace-only باشد (Warning). **یافته‌ی جانبی مستند
  (نه رفع‌شده):** این فایل هیچ تابع تجمیع‌کننده‌ی سطح‌بالا (`validateShot`) ندارد —
  هر ۵ Rule (شامل Rule 8 جدید) مستقل فراخوانی می‌شوند؛ سیم‌کشی به یک جریان
  Validation واقعی به لایه‌ی UI آینده (واحد ۱۶) موکول است.

جزئیات کامل در `docs/adr/040-unit05-unit08-type-registry-negative-prompt-rule8.md`.

### 🎯 نقطه‌ی عطف: ممیزی pre-Unit 16 کاملاً بسته شد — رفع F10، F11، F12 (آخرین یافته‌ها)

سه یافته‌ی ⚪ (صرفاً مستندسازی، بدون اثر بر رفتار/کامپایل کد) رفع شدند:

- **F10:** ۱۲ کامنت هدر «منبع حقیقت» که هنوز به نام بلوپرینت بدون `-v2` اشاره
  می‌کردند (`ProjectDnaEntity.kt`، `ShotSettingsResolution.kt`،
  `ShotOutfitSelection.kt`، `ShotValidation.kt`، `AssetSelection.kt`، و ۷ فایل
  `domain/promptengine/`) به نسخه‌ی `-v2` واقعی موجود در `docs/blueprints/`
  اصلاح شدند. ADR های تاریخی طبق تصریح دستور کار دست‌نخورده ماندند.
- **F11:** ردیف `Shot` در `type-registry.md` — علامت اشتباه `?` (nullable) از هر
  سه فیلد `camera`/`lighting`/`environment` حذف شد (با grep تأیید شد این سه در
  `ShotModels.kt` واقعی non-nullable هستند).
- **F12:** `docs/blueprints/16-user-workflow-v2.md` — فهرست ناقص `EntityState`
  (`Draft/Review/Locked/Final`) به ۵ مقدار واقعی enum (`+Archived`) اصلاح شد؛
  چون هیچ‌جای بلوپرینت نگاشت رنگ/آیکون per-value برای هیچ‌کدام از ۵ حالت مشخص
  نکرده بود (نه فقط برای Archived)، یک پیشنهاد ساده‌ی اولیه برای هر ۵ حالت اضافه
  شد (راهنما، نه الزام طراحی نهایی).

جزئیات کامل در `docs/adr/041-pre-unit16-audit-final-cleanup-f10-f11-f12.md`.

**با این قدم، ممیزی `docs/audit/pre-unit16-audit.md` کاملاً بسته شده است** —
F1 تا F4 و F7 تا F12 رفع شدند؛ F5 (Outfit) و F6 (Location fields) عمداً برای
زمان طراحی واقعی فرم‌های واحد ۱۶ نگه داشته شده‌اند (تصمیم آگاهانه‌ی ممیزی اصلی،
نه یافته‌ی فراموش‌شده). هیچ مانع شناخته‌شده‌ای دیگر پیش از شروع Composable های
واقعی واحد ۱۶ باقی نمانده است.

### 🎯 نقطه‌ی عطف: `docs/blueprints/16-user-workflow-v2.md` با DDR-002 هماهنگ شد (ساختار Navigation دو‌لایه)

این یک اصلاح مستندسازی صرف است (بدون تغییر کد Kotlin) — طبق تصمیم UX رسمی و
نهایی معمار، ثبت‌شده در `design_v8.md` (DDR-002)، ساختار Navigation بلوپرینت ۱۶
از یک Bottom Navigation Bar تک‌لایه با ۵ آیکون مستقیم مراحل به **دو لایه‌ی
مستقل** بازطراحی شد:

1. **نوار پایین سطح اپ (همیشه ثابت):** Home · Projects · Studio · [FAB مرکزی] ·
   Assets — ۴ آیتم + FAB، مستقل از توالی ۹ مرحله‌ای Workflow.
2. **Top Tab Row درون‌پروژه‌ای (پس از ورود به Studio):** ۴ تب — داستان
   (مراحل ۱+۱ب)، DNA (مرحله ۲)، صحنه‌ها (مراحل ۴+۵)، خروجی (مراحل ۶+۷+۸). مرحله
   ۳ (Asset Library) در این Top Tab تکرار نمی‌شود — از طریق آیتم Assets در نوار
   پایین سطح اپ در دسترس است.

هر ۴ محل واقعی که به ساختار قدیم اشاره می‌کردند (بخش «مکانیزم Navigation کلی»،
بخش «رفتارهای سراسری»، یادداشت پیاده‌سازی، و یک یادداشت 🆕v7 جدید در بالای سند)
اصلاح شدند؛ اصل «کاربر آزادانه بین تب‌ها/آیتم‌ها جابه‌جا می‌شود، نه قفل ترتیبی»
بدون تغییر باقی ماند. با grep مستقل در کل `docs/blueprints/` و `docs/reference/`
تأیید شد هیچ محل دیگری به ساختار قدیم اشاره نمی‌کرد. `domain/workflow/WorkflowStep`
(کد واقعی) از قبل مستقل از تعداد آیکون/تب بود (۹ مقدار، یکی به‌ازای هر مرحله‌ی
Workflow، نه ساختار Navigation) — نیازی به تغییر کد نداشت.

### 🎯 نقطه‌ی عطف: واحد ۱۶ — فاز ۰ (پایه‌ی مشترک) کامل شد — اولین کد Compose واقعی پروژه

پیش از این قدم، تنها UI موجود یک `MainScreen.kt` «Hello World» و یک `Theme.kt`
موقت با رنگ‌های پیش‌فرض M3 بود. این فاز طبق منبع حقیقت دوگانه (`docs/blueprints/16-user-workflow-v2.md`
نسخه ۷/DDR-002 + `docs/design/README.md`/`Cinema Studio.html`) سه بخش را کامل
کرد:

- **Design Tokens واقعی:** `ui/theme/Color.kt`/`ExtendedColors.kt`/`Type.kt`/`Theme.kt`
  — رنگ‌های Dark/Light، مقیاس تایپوگرافی، Spacing/Radius دقیقاً طبق جدول‌های سند
  طراحی. توکن‌های بدون معادل مستقیم در `ColorScheme` استاندارد M3
  (fg2/fg3/fg4/success/warning/orange/hairline/...) از طریق یک `CompositionLocal`
  جداگانه (`CinemaTheme.extendedColors`) در دسترس‌اند.
- **Navigation دو‌لایه (DDR-002):** `ui/navigation/` — `MainScaffold` با نوار
  پایین ثابت (`AppBottomNavBar`: Home/Projects/FAB/Studio/Assets) + Top Tab Row
  شرطی درون‌Studio (`StudioTopTabRow`: داستان/DNA/صحنه‌ها/خروجی، با `canJumpToStep`
  واقعی برای هشدار غیرمسدودکننده)؛ Navigation Graph با مسیرهای Type-Safe
  (`@Serializable`)؛ Back Navigation Contextual (نه Stack Pop ساده) با یک Map
  قابل‌گسترش (`resolveContextualBackTarget`).
- **`WorkflowViewModel`:** language/theme/layout A-B picks واقعی و
  Persist‌شده با DataStore Preferences (نه Room — این‌ها Preference سبک UI
  هستند، نه Entity دامنه)؛ `WorkflowState` (nullable تا Session واقعی شروع شود).

**یافته‌ی مهم:** `domain.outputdelivery.Language`/`t()` (واحد ۱۴، از قبل موجود
اما بدون هیچ مصرف‌کننده‌ی UI واقعی) مستقیماً بازاستفاده شد — به‌جای تعریف یک
`AppLanguage` جدید و تکراری؛ `validateTranslationCoverage` (هم قبلاً بدون مصرف‌کننده‌ی
واقعی) روی کلیدهای Navigation/Tab این فاز سیم‌کشی شد.

الزام سخت‌گیرانه‌ی RTL (fa/RTL ↔ en/LTR، از همان ابتدا نه یک قدم بعدی) با
Override کردن `LocalLayoutDirection` بر اساس زبان انتخابی کاربر (نه Locale
سیستم) در `App.kt` برقرار شد؛ زیرساخت Unicode Bidi Isolate برای توکن‌های فنی
لاتین/عددی (`String.asLtrToken()`) هم آماده شد، برای مصرف در فازهای بعدی.

جزئیات کامل هر تصمیم مستقل (مکانیزم Persistence، طراحی `ioScopeOverride`
تزریق‌پذیر برای تست‌پذیری بدون قفل‌شدگی Robolectric، محدودیت شناخته‌شده‌ی فونت
واقعی/Liquid Glass) در `docs/adr/042-unit16-phase0-shared-foundation.md`.

`gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۵۵۴
تست (۵۳۴→۵۵۴، ۲۰ تست جدید شامل اولین Compose UI Test پروژه)، ۰ Failure، ۰ Error.

### 🎯 نقطه‌ی عطف: فونت‌های واقعی Inter/Vazirmatn متصل شدند — تکمیل فاز ۰ واحد ۱۶

محدودیت شناخته‌شده‌ی فاز ۰ (`FontFamily.Default` به‌جای Inter/Vazirmatn واقعی، چون
هیچ فایل فونتی ضمیمه نشده بود) رفع شد — ۸ فایل واقعی
(`inter_regular/medium/semibold/bold.ttf`، `vazirmatn_regular/medium/semibold/bold.ttf`،
تأییدشده با `file` که واقعاً TrueType معتبرند) اکنون در `app/src/main/res/font/`
موجودند. `ui/theme/Type.kt` دو `FontFamily` واقعی ساخت
(`InterFontFamily`/`VazirmatnFontFamily`) و تابع `cinemaFontFamily(language)` که
طبق `docs/design/README.md` («Vazirmatn — used automatically whenever lang = fa»)
خودکار بین این دو سوییچ می‌کند؛ `CinemaShotGeneratorTheme` اکنون `language` را
مستقیماً از `WorkflowViewModel` می‌گیرد (هم‌الگو با `darkTheme` موجود). `CinemaFontFamily`
قدیمی (فونت سیستم) کاملاً حذف شد — هیچ Composable ای دیگر به فونت سیستم
Fallback نمی‌کند. جزئیات کامل در `docs/adr/043-unit16-phase0-real-fonts.md`.

`gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۵۵۷ تست
(۵۵۴→۵۵۷، ۳ تست جدید: سوییچ واقعی FontFamily با تغییر زبان)، ۰ Failure، ۰ Error.

**فاز ۰ واحد ۱۶ اکنون کاملاً بدون محدودیت شناخته‌شده است. فاز ۱ (پوسته‌ی برنامه —
محتوای واقعی صفحه‌های Home/Projects/Assets/Studio Shell) آماده‌ی شروع است.**

### 🎯 نقطه‌ی عطف: واحد ۱۶ — فاز ۱ (App Shell) کامل شد — اولین صفحات واقعی محصول

فاز ۰ فقط زیرساخت مشترک را ساخته بود؛ این فاز اولین صفحات واقعی را می‌سازد: Home،
Projects، Studio Shell (Container خالی)، Assets Container، و منوی همبرگری —
طبق منبع حقیقت دوگانه‌ی بلوپرینت ۱۶ نسخه ۷ و `docs/design/README.md`.

- **لایه‌ی داده‌ی Project واقعی (جدید این فاز):** `domain/project/` (`Project`،
  `ProjectSummary`، `archiveProject` با اجرای واقعی State Machine واحد ۱۲)؛
  `data/repository/ProjectRepository.kt` (CRUD کامل: create/rename/archive/
  duplicate/delete + `observeProjectSummaries`)؛ `ProjectEntity` فیلد واقعی و
  Persist‌شده‌ی `state` گرفت (نه Placeholder سطح UI)؛ `ProjectDao` شمارش صحنه/شات
  را با Correlated Subquery محاسبه می‌کند (نه فیلد Denormalized). دلیل کامل هر دو
  تصمیم (به‌همراه یافته‌ی جانبی: طبق قوانین رسمی State Machine، یک پروژه‌ی تازه‌ساز
  DRAFT واقعاً نمی‌تواند آرشیو شود) در `docs/adr/044-unit16-phase1-app-shell.md`.
- **Home:** Header با همبرگر/Logo/Toggle زبان و تم (واقعاً به `WorkflowViewModel`
  فاز ۰ وصل)، خوش‌آمدگویی، ردیف «ایجاد سریع»، «پروژه‌های اخیر» + لینک «همه»، کارت
  پروژه با Thumbnail/چیپ وضعیت/خط Meta/منوی Overflow (تغییر نام، تکثیر، آرشیو،
  خروجی گرفتن — با بازاستفاده از `exportProject` واحد ۱۵، حذف).
- **Projects:** همان فهرست کامل کارت‌ها بدون Hero Image، با زیرعنوان تعداد پویا.
- **Studio Shell:** Header با Back Navigation Contextual فاز ۰، عنوان/Meta پروژه،
  چیپ «ذخیره شد»؛ ۴ Tab فاز ۰ (`StudioTopTabRow`) از `MainScaffold` به داخل این
  Shell منتقل شدند — هر Tab فعلاً یک Placeholder ساده نشان می‌دهد (محتوای واقعی هر
  Tab کار فازهای ۲ تا ۵ است).
- **Assets Container:** Placeholder («در فاز ۳ تکمیل می‌شود») طبق دستور کار.
- **منوی همبرگری:** گروه‌های STUDIO/TOOLS/SYSTEM طبق سند طراحی؛ فقط لینک
  «دارایی‌ها» واقعاً Navigate می‌کند (چون تنها مقصدی است که صفحه‌ی واقعی دارد)،
  بقیه یک Snackbar «به‌زودی» نشان می‌دهند — تصمیم مستند در ADR-044 (نه ظاهر
  Disabled/خاکستری، چون آن در تست دستی به چشم «خراب» می‌آید).

**یافته‌های تست (فراتر از منطق دامنه، این‌بار درباره‌ی خودِ Compose Testing):**
`ModalNavigationDrawer` محتوایش را همیشه در درخت Composition نگه می‌دارد (حتی
بسته)، پس برچسب‌های متنی تکراری بین Drawer و نوار پایین/Tab با `onNodeWithText`
Ambiguous می‌شوند — رفع شد با `Modifier.testTag(...)` (نه تغییر متن ترجمه صرفاً
برای فرار از تصادف تست). `ProjectListViewModel.projectSummaries` از یک Flow واقعی
Room می‌آید که روی Executor داخلی خودش (نه Dispatcher تزریق‌شده) دوباره Query
می‌شود — `waitUntilExactlyOneExists`/`waitUntilDoesNotExist` واقعی جایگزین تکیه‌ی
ضمنی به idle خودکار شدند. همچنین کشف شد `performClick()` روی عنصری بیرون از
Viewport دیده‌شونده‌ی یک لیست اسکرول‌شونده بدون خطا «موفق» گزارش می‌شود اما لمس
واقعی هرگز به View نمی‌رسد — رفع با `performScrollTo()` قبل از کلیک. جزئیات کامل
هر سه یافته در ADR-044 و کامنت‌های `HomeProjectsStudioFlowTest.kt`.

`gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۵۷۵ تست
(۵۵۷→۵۷۵، ۱۸ تست جدید)، ۰ Failure، ۰ Error.

**فاز ۱ واحد ۱۶ (App Shell) کامل شد. فاز ۲ (Story→DNA — محتوای واقعی اولین دو Tab
Studio) آماده‌ی شروع است.**

### 🎯 نقطه‌ی عطف: واحد ۱۶ — فاز ۲، قدم ۱ — لایه‌ی ذخیره‌سازی StoryContext + Story Tab واقعی

قبل از این قدم، `StoryContext` (واحد ۰۱) هیچ مسیر ذخیره‌سازی‌ای نداشت — داستانی
که کاربر در Story Wizard می‌ساخت با بستن اپ گم می‌شد (همان کلاس باگ قبلاً
کشف/رفع‌شده در ADR-036/044). این قدم اول رفعش کرد، سپس Tab «داستان» (Placeholder
فاز ۱) را با محتوای واقعی جایگزین کرد.

- **لایه‌ی داده (جدید):** `data/entity/StoryContextEntity.kt` (فیلدهای مسطح، نه
  JSON — چون `StoryContext` ساختار تودرتو ندارد؛ `genre: List<Genre>` به‌صورت
  رشته‌ی جداشده با کاما؛ `projectId` مستقیماً `@PrimaryKey` چون رابطه یک‌به‌یک
  است و خودِ StoryContext فیلد id ندارد)؛ `data/dao/StoryDao.kt`؛
  `data/repository/StoryRepository.kt`/`StoryMappers.kt`.
- **Story Tab:** فیلد عنوان (بازاستفاده از `Project.projectName` موجود، نه فیلد
  جدید)، فرم StoryType/Genre (چندانتخابی)/MoodPrimary/MoodSecondary/
  NarrativeIntensity/VisualIntent (Dropdown با enum واقعی دامنه، نه Textarea/سه
  گزینه طبق یادداشت قدیمی‌تر بلوپرینت که با نوع Kotlin واقعی در تناقض بود)،
  Auto-Save کاملاً خودکار روی هر تغییر فیلد (بدون دکمه‌ی Submit)، نمایش واقعی
  اعتبارسنجی‌های `StoryValidation.kt` (Blocking/Warning). Stepper های «هدف تعداد
  شات»/«ثانیه به‌ازای شات» — State محلی این صفحه (نه Room)، چون واقعاً متعلق به
  `StoryBreakdownRequest` واحد ۰۱ب هستند، آماده‌ی پاس‌شدن به فاز AI Breakdown.
- **دو باگ واقعی کشف‌شده حین تست** (نه فرضی): (۱) `StoryViewModel` (ساخته‌شده با
  `viewModel(factory=...)` داخل یک Composable) بدون تزریق صریح، از
  `AppDatabase.getInstance()` (Singleton واقعی دستگاه) استفاده می‌کرد، نه
  دیتابیس In-Memory تست — رفع با تزریق `storyRepository` در کل زنجیره
  `App.kt`→`MainScaffold`→`AppNavHost`→`StudioShell`→`StoryTabContent` (هم‌الگو
  با `workflowViewModel`/`projectListViewModel`). (۲) `performClick()` روی
  `FilterChip`/آیتم‌های `ExposedDropdownMenu` این صفحه «موفق» گزارش می‌شد اما
  وضعیت واقعی هرگز تغییر نمی‌کرد — رفع با `performSemanticsAction` (API رسمی
  Compose Testing). جزئیات کامل هر دو در
  `docs/adr/045-unit16-phase2-step1-story-tab.md`.

`gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۵۸۱ تست
(۵۷۵→۵۸۱، ۶ تست جدید)، ۰ Failure، ۰ Error.

**قدم ۲ فاز ۲ (AI Story Breakdown + DNA Tab) آماده‌ی شروع است.**

### 🎯 نقطه‌ی عطف: واحد ۱۶ — فاز ۲، قدم ۲ — صفحه‌ی AI Story Breakdown (اولین اتصال UI کل زنجیره‌ی واحد ۰۱ب)

اولین بار که زنجیره‌ی کامل واحد ۰۱ب (PromptBuilder→ChunkCombiner→JsonDoctor→
StoryToDomainMapper، همه از قدم‌های قبلی موجود و تست‌شده بودند اما به هیچ UI ای
وصل نبودند) از UI واقعی تا ذخیره‌سازی Room واقعی (AssetRepository/
SceneRepository/ShotRepository جدید) وصل شد. طبق دستور کار، فقط این صفحه ساخته
شد؛ DNA Tab به قدم بعدی موکول شد.

- **مسیر مستقل جدید:** `AiStoryBreakdown(projectId)` — هم‌رده با ۴ مسیر ریشه‌ی
  موجود (Header/Back مستقل خودش، نه Sub-view داخل Tab «داستان»)، طبق
  `docs/design/README.md` بخش «۴. AI Story Breakdown». Back Navigation
  Contextual برای این مسیر (که به یک آرگومان Runtime نیاز دارد) در
  `MainScaffold.kt` Special-case شد، بدون تغییر امضای تابع خالص تست‌شده‌ی
  `resolveContextualBackTarget`.
- **سه فاز صفحه:** فاز ۱ (نوشتن داستان آزاد + Stepper های هدف‌شات/ثانیه‌به‌شات +
  «تولید پرامپت» واقعی + کارت فقط‌خواندنی + دکمه‌ی کپی — چون `sendToAiConnector`
  هنوز `TODO()` است طبق تصمیم قبلی معمار، ADR-035)؛ فاز ۲ (چسباندن پاسخ AI +
  Chunk Combiner برای پاسخ‌های چندبخشی + Modal تعمیر JSON)؛ فاز ۳ (بازبینی
  شمارش Asset/Scene/Shot + هشدارهای نام‌های یافت‌نشده + «تأیید و ادامه» که واقعاً
  در سه Repository ذخیره می‌کند).
- **لایه‌ی داده‌ی جدید:** `StoryBreakdownSessionEntity`/`StoryBreakdownSessionDao`
  (مجزا از `StoryContextEntity` — دو نوع دامنه‌ی مستقل)؛ `ShotRepository` (اولین
  Repository مستقل برای ذخیره‌ی یک `Shot` از صفر — قبلاً فقط از داخل
  `ProjectTransactionDao`/فقط‌خواندنی وجود داشت).
- **یافته‌ی واقعی دامنه‌ی از قبل تثبیت‌شده (نه باگ این قدم):** `repairJson`
  خطاهای `autoFixable=true` (مثل کاما اضافه) را همان‌جا بی‌صدا تعمیر می‌کند؛
  Modal تعمیر دستی UI فقط برای خطاهای واقعاً غیرقابل‌تعمیر خودکار Trigger
  می‌شود.
- **دو یافته‌ی واقعی تست:** (۱) تصادف متنی «تولید پرامپت» با کلید موجود
  `drawer.promptGenerator` (چون محتوای Drawer همیشه در Composition زنده
  می‌ماند) — رفع با `testTag` مجزا؛ (۲) `performScrollTo()` روی عناصر بدون والد
  Scrollable (دکمه‌های Home/Dialog/AlertDialog) با `AssertionError` شکست
  می‌خورد — این عناصر با `performClick()` ساده (بدون Scroll) تعامل گرفتند.
  جزئیات کامل همه‌ی تصمیمات در
  `docs/adr/046-unit16-phase2-step2-ai-story-breakdown.md`.

`AiStoryBreakdownFlowTest.kt` (۴ تست End-to-End با `MainScaffold` کامل + Room
واقعی In-Memory): پاسخ معتبر ساده → ذخیره‌ی واقعی؛ کاما اضافه → تعمیر خودکار
بی‌صدا؛ Chunk Combiner (دو تکه با `[CONTINUE]`)؛ هشدار نام یافت‌نشده (Rule 9).

`gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۵۸۵ تست
(۵۸۱→۵۸۵، ۴ تست جدید)، ۰ Failure، ۰ Error.

**قدم ۳ فاز ۲ (DNA Tab) آماده‌ی شروع است.**

### 🎯 نقطه‌ی عطف: واحد ۱۶ — فاز ۲، قدم ۳ (آخرین قدم) — Tab «DNA» واقعی؛ فاز ۲ کامل شد

`domain.dna.ProjectDna` (واحد ۰۲) و `ProjectDnaRepository` (واحد ۱۵) قبل از این
قدم کامل و تست‌شده بودند اما به هیچ UI ای وصل نبودند — این قدم اولین اتصال است، و
آخرین قدم فاز ۲ (بعد از Story Tab و AI Story Breakdown). با تکمیل این قدم، **فاز
۲ واحد ۱۶ به‌طور کامل تکمیل شد.**

- **بنر Soft Lock:** نارنجی/Bold/حاشیه‌ی واضح بالای صفحه — نمایش بصری فلسفه‌ی
  Soft Lock پروژه (تغییر DNA هرگز کاربر را Block نمی‌کند، فقط هشدار می‌دهد).
- **۶ گروه Collapsible** (پیش‌فرض باز) روی فیلدهای واقعی: هویت اصلی (VisualStyle
  ۳۴ مقدار/۵ دسته، RealismLevel، StyleConsistency)، پالت اصلی (۵ Swatch رنگ +
  ColorTemperature/Saturation/Contrast/پریست)، پایه‌ی احساسی کلی (Mood ۲۵
  مقدار/۵ دسته، Intensity، Consistency)، ترجیح نور (LightingStyle ۲۲ مقدار/۴
  دسته، nullable با گزینه‌ی صریح «بدون ترجیح»)، محدودیت‌های خروجی (AspectRatio ۱۱
  مقدار)، دستورالعمل‌های کیفیت (qualityTags/negativePrompt).
- **فهرست‌های گروه‌بندی‌شده بر اساس دسته** (نه Dropdown تخت) برای VisualStyle/
  Mood/LightingStyle — طبق تصریح صریح سند طراحی.
- **اعتبارسنجی زنده:** `validateColorPalette` (تنها تابع `DnaValidation.kt` که
  واقعاً روی ویرایش زنده‌ی یک فیلد این فرم قابل‌اعمال است — بقیه‌ی قوانین آن فایل
  یک Shot را در برابر DNA اعتبارسنجی می‌کنند) — Hex نامعتبر بلافاصله Blocking
  نشان می‌دهد، بدون جلوگیری از ادامه‌ی ویرایش فیلدهای دیگر.
- **Auto-Save بی‌صدا** روی هر تغییر فیلد (هم‌الگو با Story Tab).
- **یافته‌ی واقعی تست:** `performClick()` (لمس مبتنی بر مختصات) روی کارت پروژه
  (`ProjectCard.kt`) در سناریوی «بازگشت به Home از عمق یک Tab غیر-پیش‌فرض
  استودیو» هیچ ناوبری‌ای Trigger نمی‌کند — تأییدشده با شمارش مستقیم گره‌های
  Semantics؛ همان کلاس مشکل تثبیت‌شده در ADR-045 برای FilterChip/
  DropdownMenuItem، این‌بار روی یک Card؛ رفع با `performSemanticsAction`. جزئیات
  کامل همه‌ی تصمیمات در `docs/adr/047-unit16-phase2-step3-dna-tab.md`.

`DnaTabFlowTest.kt` (۳ تست End-to-End با `MainScaffold` کامل + Room واقعی
In-Memory): Round-Trip واقعی (تغییر Visual Style → بستن/بازکردن Studio → مقدار
باقی می‌ماند)؛ اعتبارسنجی زنده (Hex نامعتبر → Blocking فوری، بدون مسدودشدن
فیلدهای دیگر)؛ انتخاب گروه‌بندی‌شده‌ی Mood/LightingStyle.

`gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۵۸۸ تست
(۵۸۵→۵۸۸، ۳ تست جدید)، ۰ Failure، ۰ Error.

**فاز ۲ واحد ۱۶ (Story Tab + AI Story Breakdown + DNA Tab) به‌طور کامل تکمیل
شد. فاز ۳ (Asset Library) آماده‌ی شروع است.**

### 🎯 نقطه‌ی عطف: واحد ۱۶ — فاز ۳، قدم ۱ — صفحه‌ی Asset Library (لیست + فیلتر)

اولین قدم فاز ۳؛ منابع حقیقت: `docs/blueprints/16-user-workflow-v2.md` بخش
«مرحله ۳» و `docs/design/README.md` بخش «۸. Assets Library».

- **کمبود Repository رفع شد:** `AssetRepository` قبل از این قدم فقط
  `load*(ids: List<String>)` داشت (برای زمانی که ID ها از قبل معلوم‌اند)، نه
  «همه‌ی Asset های یک نوع در یک پروژه» که صفحه‌ی Library نیاز دارد. یک `@Query`
  پارامتری‌شده‌ی جدید در `AssetDao` (`getAssetsForProjectByType`، فیلتر روی
  `projectId` و `assetType`) به‌علاوه‌ی سه تابع نوع‌محور واقعی در سطح Repository
  (`loadAllCharacterAssets`/`loadAllLocationAssets`/`loadAllObjectAssets`)
  اضافه شد.
- **یافته‌ی معماری واقعی (نه فقط جزئیات محلی):** برخلاف `CharacterAsset`
  (`characterTier`) و `ObjectAsset` (`subtype`)، مدل دامنه‌ی `LocationAsset`
  اصلاً فیلد نوع‌دار نداشت — با اینکه هم سند طراحی و هم دستور کار این قدم صریحاً
  یک زیرفیلتر `LocationType` (INDOOR/OUTDOOR/MIXED/CUSTOM) برایش فرض کرده
  بودند. تنها `enum` مشابه از قبل در `domain.scene` بود، اما برای مفهومی کاملاً
  متفاوت (دسته‌بندی یک Scene، نه یک Asset دائمی کتابخانه). یک `enum` تازه و
  مستقل در `domain.asset` ساخته شد (هم‌راستا با اصل مستندشده‌ی
  `AssetContinuity.kt`: «هرگز یک enum مشترک» برای مفاهیمی که فقط شباهت اسمی
  دارند) و فیلد `locationType: LocationType = LocationType.CUSTOM` با مقدار
  پیش‌فرض (Backward Compatible، هر ۴ محل ساخت واقعی بررسی شد) به `LocationAsset`
  اضافه شد.
- **صفحه‌ی Asset Library واقعی** جای Placeholder فاز ۱ را گرفت: فیلتر Segmented
  بالا (کاراکترها/مکان‌ها/اشیاء)، ردیف زیرفیلتر متناظر با نوع فعال
  (CharacterTier/LocationType/ObjectSubtype)، کارت Asset (Thumbnail جای‌گیر،
  نام، Badge نارنجی سطح، توضیح، خط سطح تداوم مطابق نوع دقیق آن Asset)، و دکمه‌ی
  شناور «افزودن Asset جدید» (فعلاً فقط Snackbar «به‌زودی» — بدنه‌ی فرم کار قدم
  بعدی است).
- **رعایت دقیق دو باگ Contrast مستندشده‌ی سند طراحی:** فیلتر Segmented و همه‌ی
  Badge/Chip این صفحه کاملاً Opaque‌اند (بدون `alpha` روی رنگ زمینه) با حاشیه‌ی
  ۲dp واضح — دقیقاً همان دستورالعمل Implementation Notes سند طراحی، هم‌الگو با
  `PhaseCircle` (ADR-046).

`AssetRepositoryTest.kt` (۶ تست جدید: لیست خالی/پر برای هر سه نوع Asset،
نادیده‌گرفتن انواع دیگر و پروژه‌های دیگر) و `AssetsScreenFlowTest.kt` (۳ تست
End-to-End با `MainScaffold` کامل + Room واقعی In-Memory: فیلتر پیش‌فرض
Characters + زیرفیلتر صحیح، سوییچ به Locations، سوییچ به Objects). جزئیات کامل
همه‌ی تصمیمات در `docs/adr/048-unit16-phase3-step1-asset-library.md`.

`gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۵۹۷
تست (۵۸۸→۵۹۷، ۹ تست جدید)، ۰ Failure، ۰ Error.

### 🎯 نقطه‌ی عطف: واحد ۱۶ — فاز ۳، قدم ۲ (آخرین قدم) — فرم‌های ساخت Asset؛ فاز ۳ کامل شد

دومین و آخرین قدم فاز ۳ — فرم ساخت واقعی هر سه نوع Asset، جایگزین دکمه‌ی شناور
«به‌زودی» قدم قبل. با تکمیل این قدم، **فاز ۳ واحد ۱۶ (Asset Library) به‌طور کامل
تکمیل شد.**

- **سه فرم واقعی:** Character (Tier/PhysicalAppearance/Hair/FacialFeatures/Outfit
  پیش‌فرض)، Location (Environment/LocationType اکنون قابل‌ویرایش/برچسب‌های آزاد
  زمانی-آب‌وهوایی-عناصر کلیدی)، Object (Subtype/size/materialAndColor/specialTrait) —
  هر سه با اعتبارسنجی زنده‌ی `domain/asset/AssetValidation.kt` (بدون نیاز به کلیک
  «ذخیره») و دکمه‌ی ذخیره‌ای که تا رفع کامل موارد Blocking غیرفعال می‌ماند.
- **یافته‌ی واقعی (تناقض دستور کار با کد موجود):** `timeCompatibility`/
  `weatherCompatibility`/`keyElements` روی `LocationAsset` با grep تأیید شد از نوع
  `List<String>` (واژگان باز) هستند، نه `List<Enum>` که دستور کار فرض کرده بود —
  به‌جای چندانتخابی روی enum ثابت، یک ورودی «افزودن برچسب آزاد» ساخته شد.
- **تصمیم F5 (نگهداری‌شده از ممیزی pre-unit16):** فرم Character فقط یک Outfit
  پیش‌فرض ساده می‌سازد (نام+توضیح، هم‌الگو با `defaultOutfitPlaceholder` واحد
  ۰۱ب)؛ مدیریت کامل چند-Outfit به یک بخش پیشرفته‌ی آینده موکول شد.
- **مسیر Navigation مشترک:** `AssetForm(kind: AssetKind)` واحد به‌جای سه مسیر جدا —
  دکمه‌ی شناور صفحه‌ی Assets بسته به فیلتر فعال به فرم درست Navigate می‌کند.
- **محدودیت شناخته‌شده‌ی پذیرفته‌شده:** بعد از ذخیره، فیلتر صفحه‌ی Assets خودکار
  روی نوع تازه‌ساخته‌شده نمی‌ماند (همیشه به Characters بازمی‌گردد) — چون مسیر
  `Assets` بدون‌آرگومان است و رفعش نیازمند تغییری خارج از Scope این قدم بود؛ کاربر
  باید دستی فیلتر مربوطه را لمس کند. جزئیات کامل همه‌ی تصمیمات در
  `docs/adr/049-unit16-phase3-step2-asset-forms.md`.

`AssetFormFlowTest.kt` (۵ تست End-to-End با `MainScaffold` کامل + Room واقعی
In-Memory): ساخت هر سه نوع Asset از فرم واقعی تا ظاهرشدن در لیست، نمایش زنده‌ی یک
Rule Blocking (size خالی روی Object)، تغییر خودکار سطح قفل تداوم پیش‌فرض با تغییر
Tier کاراکتر.

`gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۶۰۲
تست (۵۹۷→۶۰۲، ۵ تست جدید)، ۰ Failure، ۰ Error.

**فاز ۳ واحد ۱۶ (Asset Library) به‌طور کامل تکمیل شد. فاز ۴ (Scene + Shot
Composer) آماده‌ی شروع است.**

### 🎯 نقطه‌ی عطف: واحد ۱۶ — فاز ۴، قدم ۱ — لیست صحنه‌ها + Scene Detail (اتصال Asset↔Scene نهایی شد)

اولین قدم فاز ۴ (طبق پلن اجرایی، پیچیده‌ترین فاز). Tab «صحنه‌ها» (Placeholder از
فاز ۱) جای خودش را به لیست واقعی صحنه‌ها داد؛ صفحه‌ی مستقل Scene Detail هم
اولین‌بار ساخته شد.

- **کمبود Repository رفع شد:** `SceneRepository.loadAllScenes(projectId):
  Flow<List<Scene>>` — `SceneDao.getScenesForProject` از قبل موجود بود ولی هیچ
  معادل سطح دامنه‌ای نداشت.
- **اتصال Asset↔Scene نهایی شد (مورد ۶):** دکمه‌ی «اتصال به کتابخانه» در
  Overview صفحه‌ی Scene Detail، `AssetRepository.loadAllLocationAssets` (فاز ۳)
  را می‌خواند؛ انتخاب کاربر `Scene.locationAssetId` را واقعاً ذخیره می‌کند و
  Overview نام همان Asset متصل را نمایش می‌دهد.
- **یافته‌ی معماری واقعی:** `Scene` هیچ فیلد `EntityState` نداشت — با اینکه
  بلوپرینت ۱۶ صریحاً می‌گوید «هر Entity یک وضعیت `EntityState` دارد» (همان اصلی
  که `Project.state` را در فاز ۱ ساخت، ADR-044). `state: EntityState =
  EntityState.DRAFT` به `Scene` اضافه شد؛ `domain/scene/SceneLifecycle.kt`
  (تابع `lockScene`) قانون واقعی State Machine واحد ۱۲ را برای Quick Action
  «Lock Scene» اجرا می‌کند.
- **یافته‌ی واقعی (اصلاح دستور کار):** سند طراحی صریحاً ۳ Tab برای Scene Detail
  مشخص کرده (Overview/Shots/Assets) — نه ۴ Tab با «Notes» که دستور کار فرض کرده
  بود.
- **Quick Actions:** Edit Scene (Dialog «تنظیمات صحنه» با دکمه‌ی ذخیره‌ی صریح،
  طبق بلوپرینت ۱۶)، Add Shot (Snackbar «به‌زودی» — منتظر قدم بعدی)، Duplicate
  (واقعاً پیاده شد، هم‌الگو با `ProjectRepository.duplicateProject`)، Lock
  Scene.
- **محدودیت شناخته‌شده‌ی پذیرفته‌شده:** بعد از بازگشت از Scene Detail، Tab
  «صحنه‌ها» به‌طور خودکار انتخاب نمی‌ماند (همان محدودیت پذیرفته‌شده‌ی
  مستندشده‌ی ADR-049 برای فیلتر صفحه‌ی Assets). جزئیات کامل همه‌ی تصمیمات در
  `docs/adr/050-unit16-phase4-step1-scene-detail.md`.

`SceneRepositoryTest.kt` (۲ تست جدید: خالی برای پروژه‌ی جدید، بازگرداندن صحیح با
نادیده‌گرفتن پروژه‌های دیگر) و `ScenesFlowTest.kt` (۲ تست End-to-End با
`MainScaffold` کامل + Room واقعی In-Memory: Navigation از لیست به Detail و
برگشت؛ اتصال یک LocationAsset از کتابخانه تا نمایش صحیح در Overview).

`gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۶۰۶
تست (۶۰۲→۶۰۶، ۴ تست جدید)، ۰ Failure، ۰ Error.

### 🎯 نقطه‌ی عطف: واحد ۱۶ — فاز ۴، قدم ۲ — Shot List + اسکلت Shot Composer

دومین قدم فاز ۴. Tab «شات‌ها»ی Scene Detail (خالی از قدم قبل) جای خودش را به
لیست واقعی شات‌های صحنه داد؛ صفحه‌ی مستقل Shot Composer هم اولین‌بار ساخته شد
(فقط اسکلت — محتوای کامل هر Tab کار قدم ۳ است).

- **کمبود Repository رفع شد:** `ShotRepository.loadAllShots(sceneId):
  Flow<List<Shot>>` و `loadShot(shotId): Result<Shot?>` — تا این قدم
  `ShotRepository` فقط `saveShot` داشت، هیچ متد خواندنی.
- **باگ واقعی کشف و رفع شد:** `ShotDto` فیلد `negativePromptOverride` را
  نداشت — با اینکه `Shot` (دامنه) این فیلد را از ADR-028 دارد؛ یعنی هر Shot که
  از Repository/DAO Round-Trip می‌کرد، این فیلد را بی‌صدا گم می‌کرد. رفع شد و
  با تست Round-Trip صریح پوشش داده شد.
- **Shot List:** سوییچ Grid/Timeline (`WorkflowState.shotListViewMode` —
  Session-Scoped، نه DataStore)، کارت هر شات (شماره، عنوان/توصیف کوتاه‌شده،
  نوع نما، مدت، نشانگر Override تنظیمات صحنه)، دکمه‌ی «شات جدید».
  Shot Composer: هدر با شماره‌ی شات، فیلدهای سطح‌بالای کامل و واقعی (عنوان،
  توصیف با اعتبارسنجی زنده، هدف، نوع نما، مدت، سطح حرکت) با Auto-Save بی‌صدا،
  و ۴ Tab اسکلتی (اصلی/دوربین/نور و محیط/صدا) — محتوای هر Tab کار قدم بعد.
- **باگ Production واقعی کشف و رفع شد:** `SceneDetailViewModel.factory` دو
  Repository اختیاری‌اش را با `&&` به‌هم بسته بود — یعنی اگر فقط یکی تزریق
  می‌شد، هر دو بی‌صدا به دیتابیس Production Singleton برمی‌گشتند (نه فقط آن
  یکی که واقعاً `null` بود). هر دو حالا مستقل بررسی می‌شوند.
- **تصمیم ناوبری:** بازگشت از Shot Composer دقیقاً به Tab «شات‌ها» برمی‌گردد
  (نه بازنشانی به Overview) — چون این مسیر صراحتاً در سند طراحی نام برده شده،
  برخلاف دو نمونه‌ی محدودیت پذیرفته‌شده‌ی قبلی (ADR-049/050). جزئیات کامل همه‌ی
  تصمیمات (شامل یافته‌های Repository/باگ Production بالا) در
  `docs/adr/051-unit16-phase4-step2-shot-list-composer-skeleton.md`.

`ShotRepositoryTest.kt` (۴ تست جدید: Round-Trip کامل شامل
`negativePromptOverride`، شات ناموجود، خالی برای صحنه‌ی بدون شات، بازگرداندن
صحیح مرتب‌شده بر اساس `shotNumber` با نادیده‌گرفتن صحنه‌های دیگر) و
`ShotsFlowTest.kt` (۲ تست End-to-End با `MainScaffold` کامل + Room واقعی
In-Memory: نمایش صحیح شات‌های صحنه + سوییچ Grid/Timeline؛ ساخت شات جدید با
فیلدهای سطح‌بالا → ذخیره‌ی واقعی → بازگشت به لیست).

`gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۶۱۲
تست (۶۰۶→۶۱۲، ۶ تست جدید)، ۰ Failure، ۰ Error.

### 🎯 نقطه‌ی عطف: واحد ۱۶ — فاز ۴، قدم ۳ — محتوای کامل Tab «دوربین» Shot Composer

سومین قدم فاز ۴ — عمداً محدود به Tab «دوربین» (پیچیده‌ترین Tab این صفحه)؛
Tab‌های «نور و محیط» و «صدا» قدم بعدی جداگانه‌اند.

- **چالش اصلی: فرم شرطی Movement.** `CameraMovement` (`sealed class`، ۶
  زیرکلاس با فیلدهای کاملاً متفاوت) اولین sealed class این کدبیس بود که به
  فرم UI شرطی نیاز داشت — هیچ الگوی مشابه از قبل در پروژه وجود نداشت. طراحی
  نهایی: سوییچ دو‌لایه‌ی Basic/Advanced (دقیقاً هم‌ساختار خودِ بلوپرینت)، با
  `AdvancedMovementType` (enum موجود اما تا این قدم در هیچ‌جای کد استفاده
  نشده) به‌عنوان انتخاب‌گر ۵ Variant پیشرفته. State یک `MutableStateFlow<CameraMovement>`
  تکی است — Tier/Variant فعلی مستقیماً از روی نوع Runtime همین مقدار مشتق
  می‌شود، نه یک State موازی قابل Desync.
- **واژگان بسته کجا Dropdown، کجا متن آزاد:** با خواندن دقیق جدول بلوپرینت
  (نه فقط امضای Kotlin)، فیلدهایی مثل `Orbit.degrees` (۹۰/۱۸۰/۳۶۰)،
  `DronePath.altitudeChange`/`.pathType`، و `HandheldShake.intensity`
  (Slider ۰ تا ۱۰) واژگان بسته‌ی صریح داشتند؛ بقیه (سرعت‌ها، `frequency`،
  ترکیب Compound) متن آزاد ماندند.
- **منبع/Override:** فیلدها فقط در حالت «سفارشی‌سازی برای این شات» رندر
  می‌شوند (نه Disabled) — چون طبق ADR-013 (واحد ۰۵)، Scene هیچ مقدار
  camera واقعی برای ارث‌بری ندارد. `overrideValue` همیشه با آخرین State فرم
  پر می‌شود (حتی در حالت «scene») تا سوییچ رفت‌وبرگشتی داده گم نکند.
- **۴ از ۵ Rule اعتبارسنجی بخش الف واحد ۰۹ زنده وایر شدند** (لنز/فاصله،
  Static+Handheld، Extreme Wide+Shallow DoF، Rack Focus+Subject Count) —
  Rule پنجم («مدت حرکت دوربین») عمداً نه، چون طبق ADR-008 هیچ Variant
  فیلد duration ندارد.
- **Attached References:** فقط نوع (character/style/composition) + توضیح
  متنی — با grep تأیید شد هیچ Infra انتخاب‌گر تصویر در کل کدبیس وجود ندارد؛
  مدیریت واقعی آپلود فایل بدهی ثبت‌شده برای قدمی مستقل است.
- **یافته‌ی تست:** کلیک روی Dropdown پایین‌تر از ناحیه‌ی دیده‌شده‌ی
  `verticalScroll` بدون `performScrollTo()` صریح باز نمی‌شد؛ و تستی با چند
  سوییچ سریع UI پشت‌سرهم گاهی با Race واقعی بین Auto-Save ناهمگام و
  `database.close()` شکست می‌خورد — هر دو رفع شدند. جزئیات کامل در
  `docs/adr/052-unit16-phase4-step3-camera-tab.md`.

`ShotRepositoryTest.kt` (۶ تست جدید: Round-Trip کامل جداگانه برای هر ۶ نوع
`CameraMovement`) و `ShotsFlowTest.kt` (۲ تست End-to-End جدید: انتخاب هر ۶
نوع Movement → نمایش صحیح فرم شرطی متناظر؛ سوییچ منبع/Override → صحت
`source` در Shot ذخیره‌شده).

`gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۶۲۰
تست (۶۱۲→۶۲۰، ۸ تست جدید)، ۰ Failure، ۰ Error.

### 🎯 نقطه‌ی عطف: واحد ۱۶ — فاز ۴، قدم ۴ (آخرین قدم) — Tab «نور و محیط» + Tab «صدا»؛ فاز ۴ به‌طور کامل تکمیل شد

آخرین قدم فاز ۴ — Tab «نور و محیط» (`LightingSettings`/`EnvironmentSettings`،
واحد ۰۸) و Tab «صدا» (`SoundProfile` + Environment-to-Sound Mapping).

- **دو سوییچ منبع/Override مستقل** (نه یک سوییچ ترکیبی) — `Shot.lighting` و
  `Shot.environment` دو `SourcedSettings` کاملاً جدای واحد ۰۵ هستند؛ سوییچ
  نور می‌تواند مستقل از سوییچ محیط تغییر کند.
- **محدودیت ۳موردی `environmentalMotion`** با غیرفعال‌کردن Chip (نه Exception)
  اعمال شد — دو لایه محافظت (UI + ViewModel).
- **Rule 5** («Ambient — auto from Weather... manual only / never
  auto-generated»): دکمه‌ی صریح «تولید صداهای محیط» تنها راه پر شدن
  `ambientSounds` است — هیچ Effect خودکاری در بارگذاری Tab وجود ندارد؛
  مستقیماً از `mapEnvironmentToSound` (واحد ۰۸) استفاده می‌کند.
- **بازاستفاده:** `lightingStyleLabel`/کلیدهای `lightingStyle.*` موجود
  (فاز ۲، DNA Tab) مستقیماً بازاستفاده شدند؛ `ColorTemperature` این واحد
  (WARM/NEUTRAL/COLD/MIXED) با `ColorTemperature` سطح DNA (WARM/COOL/NEUTRAL)
  تداخل ندارد — کلید ترجمه‌ی جدا (`sceneConditionsColorTemperature.*`).
- **تصمیم آگاهانه:** برخلاف Tab دوربین (قدم قبل)، هیچ‌کدام از ۱۳ Rule
  اعتبارسنجی واحد ۰۸ زنده وایر نشدند — سه‌تای‌شان به `TimeOfDay`/`locationType`
  از Scene نیاز دارند (وابستگی تازه‌ای که `ShotComposerViewModel` ندارد)،
  و وایرکردن ناقص بدون نشانه‌ی بصری گمراه‌کننده بود. جزئیات کامل در
  `docs/adr/053-unit16-phase4-step4-lighting-environment-sound.md`.

`ShotRepositoryTest.kt` (۲ تست Round-Trip جدید: همه‌ی فیلدهای Nullable
پر/خالی) و `ShotsFlowTest.kt` (۳ تست End-to-End جدید: سوییچ مستقل منبع
نور/محیط، محدودیت ۳موردی `environmentalMotion`، Rule 5).

`gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۶۲۵
تست (۶۲۰→۶۲۵، ۵ تست جدید)، ۰ Failure، ۰ Error.

**فاز ۴ واحد ۱۶ (Scene + Shot Composer) به‌طور کامل تکمیل شد — هر ۴ Tab
Shot Composer (اصلی/دوربین/نور و محیط/صدا) اکنون محتوای واقعی دارند.**

### 🎯 نقطه‌ی عطف: رفع دو محدودیت ثبت‌شده‌ی Tab DNA (`dependentShotsCount` + سه فیلد بدون UI `OutputConstraints`)

قدم مستقل کوچک بعد از فاز ۴ — رفع دو محدودیت ثبت‌شده در
`unit16-execution-plan.md` (ADR-047) که در فاز ۲ قدم ۳ عمداً به بعد از فاز ۴
موکول شده بودند:

- **Rule 1 (Soft Lock) اکنون شمار واقعی شات‌های وابسته‌ی پروژه را می‌گیرد** —
  به‌جای همیشه صفر. با بازاستفاده‌ی مستقیم از Query موجود
  `ProjectDao.getProjectWithCounts` (واحد ۱۶ فاز ۱، ADR-044؛ همان عددی که
  Header استودیو از قبل نشان می‌دهد)، نه یک Query تازه در `ShotDao`. تغییر
  سبک بصری با شات وابسته اکنون واقعاً هشدار Snackbar نشان می‌دهد (نه فقط در
  تئوری).
- **سه فیلد `OutputConstraints`** (`forbiddenElements` با سه دسته‌ی ثابت
  camera/lighting/weather، `mandatoryElements` با فرم افزودن دستی، و
  `maxShotDurationSeconds`) اکنون در گروه «محدودیت‌های خروجی» Tab DNA
  قابل‌ویرایش‌اند.
- تست‌ها: ۲ تست تازه در `DnaSoftLockWarningTest.kt` (فایل جدا، به‌دلیل
  محدودیت واقعی «هر Compose Test فقط یک‌بار `setContent`»)، ۱ تست Round-Trip
  تازه در `DnaTabFlowTest.kt`. جزئیات کامل تصمیمات (خصوصاً چرا Query تازه‌ای
  اضافه نشد) در `docs/adr/054-unit16-dependent-shots-count-output-constraints-ui.md`.

`gradle :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL`، ۶۲۸
تست (۶۲۵→۶۲۸، ۳ تست جدید)، ۰ Failure، ۰ Error.

### 🎯 نقطه‌ی عطف: واحد ۱۶ — فاز ۵، قدم ۱ — صفحه‌ی Validation (تجمیع سه‌سطحی)

اولین قدم فاز ۵ (Validation → Prompt Generation → Output Delivery) — طبق
`docs/blueprints/16-user-workflow-v2.md` و `docs/design/README.md` بخش «۹.
Validation».

- **تجمیع‌کننده‌ی خالص جدید** (`domain/validation/ValidationAggregator.kt`):
  چون `ValidationIssue` سراسری واحد ۰۷ هیچ مفهوم Level نداشت (تأییدشده با
  grep)، یک نوع Wrapper تازه (`LeveledValidationIssue`) ساخته شد — بدون
  دست‌زدن به خودِ نوع سراسری. Level 1 (کامل بودن داده)، Level 2 (سازگاری
  منطقی — شامل هر ۱۳ Rule نور/محیط واحد ۰۸، طبق یادآوری دستور کار اکنون
  زنده وایر شدند)، Level 3 (تداوم و وابستگی — DNA پروژه + Asset های متصل).
- **صفحه‌ی Validation**: Header + دو کارت شمارش کاملاً Solid/حاشیه‌دار
  (BLOCKING قرمز/WARNING کهربایی، طبق الزام Contrast صریح سند طراحی) + سه
  بخش سطح.
- نقطه‌ی ورود: دکمه‌ی «اعتبارسنجی این شات» داخل Header خودِ Shot Composer
  (فقط برای شات‌های از‌پیش‌ذخیره‌شده) — نه Nav Drawer (که هنوز برای هیچ
  مقصد وابسته‌به‌Context واقعی Navigate نمی‌کند).
- سه یافته‌ی واقعی دیباگ مستند شدند: تداخل متن با لینک Drawer، نیاز
  `mergeDescendants=true` روی کارت‌های شمارش برای تست‌پذیری، و یک باگ
  Fixture در تست (`shotCount` صحنه). جزئیات کامل در
  `docs/adr/055-unit16-phase5-step1-validation-screen.md`.

`gradle :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL`، ۶۳۵
تست (۶۲۸→۶۳۵، ۷ تست جدید)، ۰ Failure، ۰ Error.

### 📌 قدم ۲ فاز ۵ («Prompt Generation») با قدم ۳ («Output Delivery») ادغام شد

تحقیق اولیه‌ی الزامی این قدم (grep دقیق فهرست «Screens / Views» در
`docs/design/README.md`) نشان داد **هیچ بخش مستقلی برای «Prompt Generation»
بین بخش ۹ (Validation) و ۱۰ (Output Delivery) وجود ندارد** — همان چیزی که
`docs/blueprints/16-user-workflow-v2.md` هم صریحاً می‌گوید («مرحله ۷:
کاملاً پشت‌صحنه، بدون UI مستقل»). با تأیید مستقیم معمار، این قدم رسماً با
قدم بعدی (Output Delivery) ادغام شد — هیچ صفحه‌ی مستقلی ساخته نشد؛ انتخاب
مدل/warnings/پیش‌نمایش/تولید نهایی همگی در همان یک صفحه‌ی «Output Delivery»
پیاده می‌شوند. زنجیره‌ی دامنه‌ی پیش‌نیاز
(`PromptGenerationRepository.collectData` → `assemblePromptBlueprint` →
`render`) از قبل کامل و تست‌شده بود، پس هیچ کد تازه‌ای هم لازم نشد. جزئیات
کامل در `docs/adr/056-unit16-phase5-step2-prompt-generation-merged.md`.

### 🎯 نقطه‌ی عطف: واحد ۱۶ — فاز ۵، قدم ۳ (آخرین قدم) — صفحه‌ی Output Delivery؛ فاز ۵ به‌طور کامل تکمیل شد

آخرین قدم فاز ۵ (طبق ADR-056 شامل هر دو وظیفه‌ی «تولید Prompt» و «تحویل
خروجی»). طبق `docs/design/README.md` بخش «۱۰. Output Delivery»:

- **Model Picker**: چیپ Wrap‌شونده‌ی هر ۱۴ پروفایل (۱۳ مدل واقعی +
  Universal Default) + «هزینه‌ی Token» هر مدل. منبع این عدد نه یک تخمین
  کاراکتر→توکن تازه (Option A) و نه تغییر برچسب به شمار کاراکتر
  (Option B) بود — بلکه یک گزینه‌ی سوم بهتر: فیلد از‌پیش‌موجود و دقیق
  `ModelConstraints.maxTokens` (که با جدول «Model List» سند طراحی کاملاً
  مطابقت داشت)، مستقل از هشدار واقعی Over-limit (`validatePromptLength`،
  که همچنان شمارش کاراکتر واقعی است).
- **Output Preview Card**: Badge «پاک‌سازی‌شده · نهایی‌شده»، متن Read-only
  واقعاً رندرشده (`render(blueprint, profile)`)، خط هزینه‌ی Token +
  هشدار Over-limit، ردیف Copy/Regenerate، دکمه‌ی تمام‌عرض Export.
- **Warnings**: هماهنگ با صفحه‌ی Validation (همان `aggregateShotValidation`
  ADR-055، به‌عنوان `validationIssues` تزریقی به `assemblePromptBlueprint`)
  + دو هشدار واقعاً تازه‌ی مختص Render/مدل (`validatePromptLength`،
  `validateUnsupportedFeatureUsage`) — نه یک منبع منطق کاملاً جدا.
- **زنجیره‌ی کامل پشت‌صحنه**: انتخاب مدل → `collectData` → `assemblePromptBlueprint`
  → `render` → نمایش؛ عوض‌کردن مدل کل زنجیره را واقعاً دوباره اجرا می‌کند.
- **Export**: فعلاً هم‌ارز Copy to Clipboard (با پیام تأیید متفاوت) —
  هیچ Infra نوشتن فایل/Share Intent واقعی در کدبیس وجود ندارد؛ محدودیت
  شناخته‌شده و مستند (پایین را ببینید).
- نقطه‌ی ورود: هم از داخل Shot Composer و هم از داخل صفحه‌ی Validation.
- انتخاب مدل هدف بین بازدیدهای این صفحه حفظ می‌شود (`WorkflowState.
  selectedModelProfileId`، هم‌الگو دقیق با `shotListViewMode` — طول یک
  نشست Studio، نه DataStore).
- یک باگ واقعی کد محصول در همین قدم پیدا و رفع شد: `OutputDeliveryViewModel.
  regenerate()` بدون لغو صریح Job قبلی، در برابر انتخاب سریع چند مدل
  پشت‌سرهم آسیب‌پذیر بود (نتیجه‌ی قدیمی می‌توانست جای نتیجه‌ی تازه را
  بگیرد) — رفع با لغو صریح `regenerateJob` قبل از هر اجرای تازه.

جزئیات کامل تصمیمات (خصوصاً منبع «هزینه‌ی Token» و یافته‌های دیباگ) در
`docs/adr/057-unit16-phase5-step3-output-delivery.md`.

`gradle :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL`، ۶۴۰
تست (۶۳۵→۶۴۰، ۵ تست جدید)، ۰ Failure، ۰ Error.

### 🎯 نقطه‌ی عطف: واحد ۱۶ — فاز ۶ (آخرین فاز)، قدم ۱ — صفحه‌ی Settings + اتصال واقعی Timer دوره‌ای Auto-Save

اولین قدم آخرین فاز واحد ۱۶. طبق `docs/design/README.md` بخش «۱۱.
Settings»، دقیقاً همان ۷ کارت:

- **Display** (Dynamic Font/Min Touch Target/Reduced Motion): سه سوییچ
  واقعاً Persist می‌شوند؛ طبق یافته‌ی صریح این قدم (grep روی `Theme.kt`)
  هیچ زیرساخت Runtime واقعی‌ای برای این سه در کل کدبیس وجود ندارد —
  محدودیت شناخته‌شده، صریحاً مستند.
- **Workflow**: Shot List Default View (کاملاً واقعی)، Auto-Save Cadence
  (واقعاً به `AutoSaveManager` وصل شد — پایین را ببینید)، Jump Between
  Steps (Persist واقعی، بدون منطق Gate ای در جای دیگر — مستند).
- **Privacy**: صرفاً نمایشی/اطلاعاتی، طبق README.
- **Language & Theme**: رادیوهای صریح، مستقیماً روی همان
  `WorkflowViewModel.language`/`theme` موجود از فاز ۰.
- **Home Screen Image**: کارت واقعی + Persist مقدار URI؛ بدون File
  Picker واقعی (هیچ‌جای کدبیس چنین Infra ای ندارد؛ هم‌کلاس محدودیت
  Attached References، ADR-051) — «انتخاب تصویر» پیام «به‌زودی» می‌دهد.
- **Layout Variants**: چیپ‌های Home/Composer مستقیماً به
  `homeLayoutVariant`/`composerLayoutVariant` موجود وصل شدند؛ یافته‌ی
  این قدم: نه `HomeScreen` و نه `ShotComposerScreen` فعلاً به این دو
  مقدار شاخه‌بندی نمی‌کنند — Persist واقعی است، اعمال بصری هنوز نیست
  (مستند، خارج از Scope این قدم).
- **About**: کارت ثابت.

**اتصال واقعی Auto-Save**: `AutoSaveManager` متد تازه‌ی `touch(projectId)`
گرفت؛ `StudioShell` اکنون یک `LaunchedEffect` دوره‌ای واقعی دارد که هر
Cadence (از همین صفحه‌ی Settings، پیش‌فرض ۳۰ ثانیه) آن را صدا می‌زند —
طول یک Session فعال Studio. تصمیم مستند: ذخیره‌ی فوری هر Entity
(Scene/Shot/DNA/Asset، از فازهای قبل) و این Timer دو لایه‌ی کاملاً
مستقل‌اند، نه یک جایگزینی برای هم — Timer فقط شکاف واقعی کشف‌شده
(`ProjectEntity.lastModified` هرگز با ویرایش واقعی محتوا تازه نمی‌شد) را
پر می‌کند.

یک باگ واقعی UX هم در همین قدم پیدا و رفع شد: Nav Drawer با ۱۱ آیتم
بدون هیچ Scroll ای بود — روی صفحه‌های کوچک‌تر، «تنظیمات»/«بکاپ‌ها»
(آخرین گروه) عملاً غیرقابل‌دسترس بودند.

جزئیات کامل تصمیمات و یافته‌های دیباگ در
`docs/adr/058-unit16-phase6-step1-settings-autosave.md`.

`gradle :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL`، ۶۴۹
تست (۶۴۰→۶۴۹، ۹ تست جدید)، ۰ Failure، ۰ Error.

### 🎯 نقطه‌ی عطف: واحد ۱۶ — فاز ۶ (آخرین فاز)، قدم ۲ (آخرین قدم کل واحد ۱۶) — صفحه‌ی Backups + بازبینی یکپارچه‌ی نهایی؛ **واحد ۱۶ کاملاً تکمیل شد**

آخرین قدم کل واحد ۱۶. دو بخش: (الف) صفحه‌ی Backups، (ب) بازبینی
یکپارچه‌ی نهایی کل اپ روی ۶ محور. جزئیات کامل تصمیمات در
`docs/adr/059-unit16-phase6-step2-backups-final-review.md`.

**صفحه‌ی Backups** (طبق `docs/design/README.md` بخش «۱۲. Backups»):
فهرست بکاپ‌ها (نام نمایشی/حجم/نوع Auto-Manual/سن)، دکمه‌ی «ساخت بکاپ
دستی»، Restore/Delete روی هر ردیف — همگی به `BackupManager` واقعی واحد
۱۵ وصل شدند (تا این قدم بلااستفاده بود). چون `BackupManager` per-project
است، این صفحه از `WorkflowState.projectId` (Session فعال Studio) می‌خواند؛
بدون Session فعال، پیام واضح «ابتدا یک پروژه را باز کنید» نشان می‌دهد.

**تصمیم معماری کلیدی (تناقض واقعی بین دو منبع حقیقت):** سند طراحی پسوند
`.csgb` را نشان می‌داد؛ کد/بلوپرینت ۱۵ هر دو `.json` بودند. طبق ترجیح
صریح معمار، پسوند واقعی فایل به `.csgb` تغییر کرد (نه فقط نام نمایشی
گمراه‌کننده) — محتوای فایل همچنان JSON خام است. الگوی نام‌گذاری امن-در-
برابر-تصادم (ADR-023) دست‌نخورده ماند؛ یک نام «نمایشی» جدا
(`displayBackupFileName`) دقیقاً الگوی سند طراحی (`slug-YYYY-MM-DD.csgb`)
را برای کاربر می‌سازد. تمایز `BackupKind` (Auto/Manual) که تا این قدم
اصلاً وجود نداشت هم اضافه شد (Chip هر دو نوع Opaque با حاشیه‌ی ۲dp، طبق
الزام صریح Contrast این قدم).

**بازبینی یکپارچه‌ی نهایی (۶ محور)** — یافته‌های واقعی:

1. **Nav Drawer Scroll**: رفع قدم قبل برای هر ۱۱ آیتم تأیید شد (تست صریح).
2. **Back Navigation Contextual**: یافته‌ی واقعی — `Validation`/`OutputDelivery`
   (فاز ۵) هرگز به `when` صریح `MainScaffold.kt` اضافه نشده بودند؛ دکمه‌ی
   برگشت درون‌صفحه درست کار می‌کرد، اما دکمه‌ی سخت‌افزاری Back به Home
   می‌رفت. رفع شد.
3. **Toast/Snackbar**: شکل پیش‌فرض Material3 (نه Pill سند طراحی) رفع شد؛
   مدت‌زمان دقیق «~۲ ثانیه» رفع **نشد** (محدودیت API استاندارد
   `SnackbarDuration`، فقط Short/Long/Indefinite).
4. **EntityState (۵ حالت)**: تأیید شد از فاز ۱ کامل بوده؛ فقط Contrast
   رفع شد (پایین).
5. **Contrast**: پنج باگ واقعی Low-opacity Tinted Fill (که ADR-055
   قبلاً برای Validation مستند کرده بود) در `ProjectCard`،
   `ScenesListScreen.EntityStateChip`، `DnaTabContent.SoftLockBanner`،
   `HomeScreen` (پیل برند روی پس‌زمینه‌ی Blur خودِ Home) و `StudioShell`
   (چیپ «ذخیره شد») پیدا و رفع شدند.
6. **AutoSave/BackupManager**: یافته‌ی واقعی — `backupIntervalMinutes`
   از واحد ۱۵ هرگز مصرف نشده بود؛ یک Timer دوره‌ای دوم (هم‌الگو با
   AutoSave قدم قبل) در `StudioShell` وصل شد.

**باگ واقعی DI کشف و رفع شد** (کشف‌شده حین دیباگ تست‌های End-to-End
Backups، نه صرفاً یک محدودیت تست): `BackupsViewModel`/`StudioShell`
هر دو مستقیماً `AppDatabase.getInstance(application)` را صدا می‌زدند —
تنها استثنای الگوی تزریق سراسری این پروژه (که همه‌جای دیگر `database` را
از `App.kt` تزریق می‌کند، نه Singleton داخلی). در تست End-to-End، این
باعث می‌شد `BackupManager` پروژه‌ی واقعاً ساخته‌شده در `database` تزریقی
تست را هرگز پیدا نکند و `createBackup` بی‌صدا شکست بخورد. **این باگ روی
رفتار واقعی Production تأثیری نداشت** (چون `AppDatabase.getInstance()`
در مسیر واقعی همیشه همان Singleton است) — یک ناهماهنگی خالص در الگوی DI
بود که فقط با تست End-to-End واقعی (نه Mock) قابل‌کشف بود. رفع شد:
`database: AppDatabase?` اکنون پارامتر تزریقی است در کل زنجیره
(`BackupsViewModel`→`BackupsScreen`→`AppNavHost`→`MainScaffold`→`App.kt`
و مستقیماً `StudioShell`)، هم‌الگو دقیق با `autoSaveManager`/
`backupFileStorage`.

~~**محدودیت شناخته‌شده‌ی تازه:** Restore/Delete در صفحه‌ی Backups بدون
دیالوگ تأیید هستند~~ — **رفع شد** در قدم بعدی (ممیزی post-Unit16، پایین
را ببینید).

`gradle :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL`،
۶۵۵ تست (۶۴۹→۶۵۵، ۶ تست جدید)، ۰ Failure، ۰ Error، ۰ Skipped. APK واقعی
هم ساخته شد.

**🏁 با این قدم، واحد ۱۶ (User Workflow/UI) به‌طور کامل تکمیل شد — هر ۶
فاز (App Shell، Story+DNA، Asset Library، Scene+Shot Composer،
Validation→Output Delivery، Settings+Backups) اکنون منطق واقعی، UI واقعی،
و تست End-to-End واقعی دارند.**

### 🎯 نقطه‌ی عطف: ممیزی جامع post-Unit16 + رفع هر ۴ یافته‌ی 🔴 بحرانی

بعد از تکمیل کامل واحد ۱۶، یک ممیزی جامع کل پروژه (`docs/audit/post-unit16-full-audit.md`،
واحد ۰۱ تا ۱۶، نه فقط مطابقت بلوپرینت) ۲۲ یافته‌ی تازه ثبت کرد — ۴ مورد
🔴 بحرانی، در همین قدم بعدی رفع شدند:

1. **بج «پاک‌سازی‌شده· نهایی‌شده» صفحه‌ی Output Delivery دروغ بود** — تا
   این قدم Pipeline واقعی واحد ۱۳ (Prompt Cleaner + Token Cost Calculator)
   هرگز از `OutputDeliveryViewModel` صدا زده نمی‌شد؛ متن نمایش/Copy/Export
   شده هرگز واقعاً پاک‌سازی یا از نظر توکن بررسی نمی‌شد. `finalizePrompt`
   اکنون واقعاً در `regenerate()` اجرا می‌شود؛ بج فقط با موفقیت واقعی
   نمایش داده می‌شود؛ `validateConflictsResolved`/`validateCompressionRatio`
   به Warnings اضافه شدند؛ خط «هزینه‌ی توکن» اکنون عدد Estimated واقعی
   نشان می‌دهد (نه فقط سقف ثابت پروفایل).
2. **آرشیو پروژه بدون تأیید و بدون بازگشت** — طبق
   `ALLOWED_TRANSITIONS[ARCHIVED]=emptyList()` (`StateMachine.kt`)، آرشیو
   یک وضعیت کاملاً پایانی است؛ اکنون یک `AlertDialog` تأیید واقعی
   (هم‌الگو با Delete پروژه‌ی موجود) پیش از اجرا نشان داده می‌شود.
3-4. **Restore/Delete بکاپ بدون تأیید** — هر دو اکنون `AlertDialog` تأیید
   واقعی دارند (بالا هم اشاره شد).

هیچ الگوی UI تازه‌ای اختراع نشد — هر سه دیالوگ از الگوی موجود
`RenameProjectDialog`/`DeleteProjectDialog` پیروی می‌کنند. جزئیات کامل
تصمیمات (خصوصاً تغییر امضای `finalizePrompt` از `Pair` به
`FinalizationResult` برای دسترسی به `CleaningReport`) در
`docs/adr/060-post-unit16-audit-critical-fixes.md`.

`gradle :app:testDebugUnitTest :app:assembleDebug` → ۶۶۱ تست (۶۵۵→۶۶۱، ۶
تست جدید)، ۶۶۰ موفق. یک شکست (`ScenesFlowTest`، `SQLiteConnectionPool`)
تأییدشده پیش‌ازاین/غیرمرتبط با این قدم است — در اجرای مجزا (بدون بقیه‌ی
Test Suite) دو بار پیاپی ۱۰۰٪ موفق بود؛ یک Flake شناخته‌شده‌ی محیط
Robolectric تحت بار کامل Test Suite، نه Regression این قدم. APK واقعی هم
ساخته شد.

**۱۸ یافته‌ی 🟠/🟡/⚪ باقی‌مانده‌ی همان ممیزی** — از این تعداد، دو یافته‌ی 🟠
(G1: لینک‌های Nav Drawer، G7: مسیر ویرایش Asset) در قدم بعدی رفع شدند
(`docs/adr/061-post-unit16-audit-g1-g7-fixes.md`). ۱۶ یافته‌ی باقی‌مانده
(از جمله بخش زیادی از Rule های Validation یتیم، الگوی DI باقی‌مانده در دو
نقطه) هنوز باز و مستند هستند — جزئیات و اولویت‌بندی پیشنهادی در خودِ
`docs/audit/post-unit16-full-audit.md`.

### 🎯 نقطه‌ی عطف: رفع هر دو یافته‌ی 🟠 ممیزی جامع post-Unit16 (G1 + G7)

1. **G1 — Nav Drawer:** از ۷ لینکی که مسیر واقعی داشتند اما هنوز
   `COMING_SOON` نشان می‌دادند، هر ۷ تا اکنون واقعاً Navigate می‌کنند:
   «دستیار داستان»/«مدیریت DNA»/«صحنه‌ها» به `Studio(projectId,
   initialTab)` با Tab درست؛ «تفکیک داستان با AI» به مسیر مستقل خودش؛
   «شات‌ها»/«اعتبارسنجی»/«تحویل خروجی» (که به یک Scene/Shot مشخص نیاز
   دارند که Drawer نمی‌داند) به Tab «صحنه‌ها» به‌عنوان نزدیک‌ترین مقصد
   واقعی. بدون پروژه‌ی فعال، کاربر به‌جای Crash به `Projects` هدایت
   می‌شود (+ پیام توضیحی). «تولید پرامپت» طبق ADR-056 عمداً دست‌نخورده
   ماند (برچسبش گمراه‌کننده نیست).
2. **G7 — مسیر ویرایش Asset:** لمس یک کارت Asset موجود (هر سه نوع) اکنون
   فرم را با داده‌ی واقعی پیش‌پر می‌کند و ذخیره همان شناسه را
   به‌روزرسانی می‌کند، نه یک رکورد تازه — دقیقاً هم‌الگو با
   `ShotComposer.shotId` (تنها الگوی «ساخت/ویرایش» موجود پروژه).

جزئیات کامل تصمیمات (خصوصاً Fallback های بدون Shot/بدون پروژه‌ی فعال) در
`docs/adr/061-post-unit16-audit-g1-g7-fixes.md`.

`gradle :app:testDebugUnitTest :app:assembleDebug` → ۶۷۲ تست (۶۶۱→۶۷۲، ۱۱
تست جدید)، ۰ Failure، ۰ Error — بدون هیچ Flake ای در این اجرا. APK واقعی
هم ساخته شد.

**۱۶ یافته‌ی 🟡/⚪ باقی‌مانده‌ی همان ممیزی** هنوز باز و مستند هستند —
جزئیات و اولویت‌بندی پیشنهادی در `docs/audit/post-unit16-full-audit.md`.

### 🎯 نقطه‌ی عطف: رفع G6 (باگ واقعی Bottom Nav) و G22 (قابلیت کاملاً غایب Delete)

1. **G6 — `PLACEHOLDER_ACTIVE_PROJECT_ID` کاملاً حذف شد:** این ثابت جعلی
   قبلاً نه فقط دکمه‌ی Studio نوار پایین، بلکه صفحه‌ی Assets و هر سه فرم
   Asset را هم تغذیه می‌کرد (مغایرت واقعی با پیش‌بررسی دستور کار، صریح در
   `docs/adr/062-...` مستند شده). تابع تازه‌ی
   `resolveActiveOrRecentProjectId` جایگزین شد: Session فعال Studio اگر
   باشد، وگرنه آخرین پروژه‌ی واقعاً تغییریافته، وگرنه هدایت به Projects.
   Nav Drawer (G1) هم برای یکدستی به همین تابع migrate شد.
2. **G22 — Delete Scene/Shot/Asset اکنون واقعاً از UI در دسترس‌اند:** هر
   سه از قبل در سطح DAO/Rule دامنه کامل بودند اما هیچ‌کدام از UI صدا زده
   نمی‌شدند. اکنون هرکدام یک مسیر Delete واقعی (منوی سه‌نقطه + AlertDialog
   تأیید، هم‌الگو با Archive/Restore/Delete بکاپ موجود) دارند — حذف Scene/
   Asset هر دو Rule واقعی Blocking دارند (صحنه‌ی دارای Shot / Asset در حال
   استفاده قابل حذف نیستند)، حذف Shot بدون Rule (طبق طراحی بلوپرینت).

جزئیات کامل تصمیمات (خصوصاً گسترش دامنه‌ی G6 به هر ۵ محل PLACEHOLDER،
نه فقط Bottom Nav) در `docs/adr/062-post-unit16-audit-g6-g22-fixes.md`.

`gradle :app:testDebugUnitTest :app:assembleDebug` → ۶۸۱ تست (۶۷۲→۶۸۱، ۹
تست جدید)، ۶۸۰ موفق. یک شکست
(`ShotsFlowTest > deleting a scene that still has a shot is blocked...`)
در اجراهای مکرر Isolated در ۴ نقطه‌ی کاملاً متفاوت شکست خورد (نه همیشه
همان خط) — امضای کلاسیک Flake محیطی (نه باگ منطقی قطعی)، تأییدشده مستقل
توسط شکست هم‌زمان یک تست کاملاً بی‌ربط در همان فایل با همان
`SQLiteConnectionPool` از‌پیش‌مستندشده‌ی ADR-059/ADR-060. APK واقعی هم
جداگانه با موفقیت ساخته شد.

**۱۴ یافته‌ی 🟡/⚪ باقی‌مانده‌ی همان ممیزی** هنوز باز و مستند هستند —
جزئیات و اولویت‌بندی پیشنهادی در `docs/audit/post-unit16-full-audit.md`.

### 🎯 نقطه‌ی عطف: رفع G15 و G16 (آخرین دو نمونه‌ی الگوی باگ DI)

1. **G15 — `ProjectListViewModel.exportProject` اکنون `database` تزریقی
   می‌گیرد** (نه دیگر `AppDatabase.getInstance` مستقیم داخل خودِ تابع) —
   هم‌الگو دقیق با ADR-059 (`BackupsViewModel`/`SceneDetailViewModel`).
   این ViewModel اولین تست مستقل خودش را هم گرفت.
2. **G16 — `AiStoryBreakdownViewModel.factory` دیگر همه‌یا‌هیچ نیست:**
   قبلاً با `if (args.size == 4)` اگر فقط ۱ تا ۳ از ۴ Repository داده
   می‌شد، هر ۴ تا (حتی تزریق‌شده‌ها) بی‌صدا دور ریخته می‌شدند. اکنون هرکدام
   مستقل با `?:` به پیش‌فرض خودش می‌رسد — هم‌الگو با
   `SceneDetailViewModel.factory`.

هر دو یافته طبق خودِ ممیزی روی Production فعلی بی‌اثر بودند (فقط
تست‌پذیری/یکدستی الگو)؛ با این قدم، این کلاس باگ DI کاملاً از پروژه پاک
شد. جزئیات کامل تصمیمات (خصوصاً افزودن `ioScopeOverride`/بازگشت `Job`
به هر دو ViewModel به‌عنوان زیرساخت لازم تست، فراتر از متن صریح دستور
کار) در `docs/adr/063-post-unit16-audit-g15-g16-fixes.md`.

`gradle :app:testDebugUnitTest` → ۶۸۳ تست (۶۸۱→۶۸۳، ۲ تست جدید)، ۶۸۲
موفق. یک شکست، همان Flake محیطی از‌پیش‌مستند در ADR-062
(`ShotsFlowTest > deleting a scene that still has a shot is blocked...`)
— `git diff` این قدم تأیید می‌کند هیچ فایلی در مسیر وابستگی آن تست دست
نخورده. `gradle :app:assembleDebug` جداگانه → موفق.

**۱۲ یافته‌ی 🟡/⚪ باقی‌مانده‌ی همان ممیزی** هنوز باز و مستند هستند —
جزئیات و اولویت‌بندی پیشنهادی در `docs/audit/post-unit16-full-audit.md`.
طبق ترتیب رسمی خودِ ممیزی (اولویت ۳ بند ۷)، قدم بعدی تصمیم آگاهانه درباره‌ی
Rule های یتیم G14 است.

### 🎯 نقطه‌ی عطف: رفع G14 (Rule های یتیم) — بخش الف رفع واقعی، بخش ب تصمیم آگاهانه

طبق ترتیب رسمی ممیزی (اولویت ۳ بند ۷)، بیش از ۳۰ Rule اعتبارسنجی یتیم در
۱۰+ دسته یا وصل شدند یا رسماً به‌عنوان تصمیم مستند شدند — نه بی‌صدا فراموش
ماندند:

1. **بخش الف — رفع واقعی: `validateTargetShotCountRange`.**
   `AiStoryBreakdownViewModel.setTargetShotCount` دیگر مقدار خارج از
   محدوده‌ی ۱-۱۵۰ را با `coerceIn` بی‌صدا کوتاه نمی‌کند — مقدار خام کاربر
   نگه داشته می‌شود، خطای واقعی Rule 2 در UI نمایش داده می‌شود، و دکمه‌ی
   «تولید Prompt» تا رفع خطا غیرفعال است.
2. **بخش ب — تصمیم آگاهانه برای ۱۱ گروه باقی‌مانده.** هر گروه یا (الف)
   قبلاً در ADR-055 آگاهانه رد شده بود (دوربین/حرکت، DNA، تصویر Shot/Asset)،
   یا (ب) از قبل کاملاً وصل بود برخلاف پیش‌بررسی (کل زنجیره‌ی Prompt
   Finalization، `validateAssetDeletion`)، یا (ج) یک تصمیم تازه گرفت: دو
   کاندید رفع نزدیک (`validateDataCompleteness`، `validateActionSoundTimeline`)،
   دو ماژول کاملاً منسوخ برای علامت‌گذاری حذف احتمالی (`OutputComposer.kt`،
   `StyleMatrix.kt`)، و **دو یافته که برای تصمیم مشترک با معمار پروژه
   علامت‌گذاری شدند**: یکسان‌سازی معماری State Machine/Lock
   (`canTransition` خام در `SceneLifecycle`/`ProjectLifecycle` به‌جای لایه‌ی
   `ValidationIssue`ی موجود — رفتار کاربر امروز درست است، فقط تکرار کد) و
   قابلیت کاملاً غایب Import پروژه از فایل (نامتقارن با Export واقعی G15).

جزئیات کامل هر تصمیم (جدول کامل ۱۱ گروه + دلیل هرکدام) در
`docs/adr/064-g14-orphaned-rules-decisions.md`.

`gradle :app:testDebugUnitTest` → ۶۸۷ تست (۶۸۳→۶۸۷، ۴ تست جدید)، ۶۸۶
موفق. یک شکست، همان کلاس Flake محیطی از‌پیش‌مستند در ADR-044/ADR-062 (این
بار در `ShotsFlowTest`؛ در اجراهای دیگر همین قدم به‌جایش `AssetFormFlowTest`/
`AppNavigationTest` — تست‌های نامرتبط و متفاوت در هر اجرا، امضای کلاسیک
ناپایداری محیط، نه یک باگ منطقی قطعی) — `git diff` این قدم تأیید می‌کند
هیچ فایلی در مسیر وابستگی این تست‌ها دست نخورده. `gradle :app:assembleDebug`
جداگانه → موفق.

**طبق ترتیب رسمی ممیزی، اولویت ۳ بند ۸ (باقی موارد 🟡 بدون فوریت:
G3-G5, G8, G12, G18) آخرین بند فهرست رسمی است** — به‌علاوه‌ی دو یافته‌ی
بالا که برای تصمیم مشترک علامت‌گذاری شدند.

### 🎯 نقطه‌ی عطف: رفع «عدم قرینگی Export/Import» (یافته‌ی معماری G14/ADR-064)

کاربر می‌توانست پروژه Export کند اما هرگز نمی‌توانست فایل Export را دوباره
Import کند — `importProject` (تابع دامنه‌ی موجود و کاملاً تست‌شده،
`ExportImportRepository.kt`) هیچ فراخوان‌کننده‌ای در UI نداشت. رفع شد:

1. **`ProjectListViewModel.importProject(fileUri: String): Job`** —
   هم‌الگو دقیق با `exportProject`. **تصمیم مستقل مهم:** File Picker
   استاندارد Android همیشه یک `content://` Uri برمی‌گرداند، اما
   `BackupFileStorage.readFile` طبق قرارداد موجودش یک مسیر فایل خام
   انتظار دارد — این تضاد با Stage‌کردن محتوای انتخاب‌شده (از طریق
   `ContentResolver`) در همان `backupDir` اپ حل شد، بدون هیچ تغییری در
   `ExportImportRepository.kt`/`BackupFileStorage.kt`.
2. **دکمه‌ی «وارد کردن پروژه»** در Header صفحه‌ی Projects — سطح-فهرست
   (نه Per-Card مثل Export)، چون Import پروژه‌ی تازه می‌سازد.
3. خطاهای واقعی (فایل خراب، یکپارچگی ارجاعی نقض‌شده) از طریق همان
   `lastActionMessage` موجود نمایش داده می‌شوند — بدون هیچ لایه‌ی خطای
   جدید.

جزئیات کامل تصمیمات در `docs/adr/065-project-import-export-symmetry.md`.

`gradle :app:testDebugUnitTest` → ۶۹۰ تست (۶۸۷→۶۹۰، ۳ تست جدید: Round-Trip
واقعی، فایل خراب، یکپارچگی ارجاعی نقض‌شده)، ۶۸۹ موفق. یک شکست، همان
کلاس Flake محیطی از‌پیش‌مستند (`ShotsFlowTest`). `gradle :app:assembleDebug`
جداگانه → موفق.

### 🎯 نقطه‌ی عطف: یکسان‌سازی معماری State Machine/Lock (بدهی فنی مستند در ADR-064)

`domain/project/ProjectLifecycle.kt` (`archiveProject`) و
`domain/scene/SceneLifecycle.kt` (`lockScene`) به‌جای `validateStateTransition`
(لایه‌ی `ValidationIssue`-برگردان واحد ۱۲ که دقیقاً برای همین منظور ساخته
شده بود)، مستقیماً از `canTransition` خام استفاده می‌کردند — تکرار همان
بررسی «آیا این انتقال مجاز است» در دو جای مجزا. رفتار کاربر همیشه درست
بود (پیام Blocking واقعی)؛ این فقط بدهی فنی معماری بود، نه یک باگ.

**تصمیم معماری:** `validateStateTransition` یک پارامتر اختیاری
`customMessage: String? = null` گرفت. `archiveProject`/`lockScene`
اکنون از این تابع (تنها فراخوان‌کننده‌ی `canTransition` در کل پروژه —
تأیید با grep) عبور می‌کنند و پیام سفارشی دقیق خودشان را پاس می‌دهند —
بدون هیچ افت کیفیت پیام برای کاربر واقعی، و بدون هیچ تغییری در امضا/
رفتار عمومی این دو تابع (تأیید شد `ProjectRepository`/`SceneDetailViewModel`
بدون تغییر کار می‌کنند). جزئیات کامل (و گزینه‌های رد‌شده) در
`docs/adr/066-state-machine-lock-unification.md`.

تست‌ها: ۳ تست تازه برای `customMessage` (`StateMachineTest.kt`)، ۱ تست
تازه در `ProjectLifecycleTest.kt` (تأیید حفظ دقیق پیام سفارشی)، و
`SceneLifecycleTest.kt` **تازه** (۵ تست — این تابع تا این قدم هیچ تست
مستقلی نداشت، فقط غیرمستقیم از طریق UI).

`gradle :app:testDebugUnitTest` → ۶۹۹ تست (۶۹۰→۶۹۹، ۹ تست جدید)، ۶۹۷
موفق در دو اجرا (هر بار یک شکست، هر بار تست‌های متفاوت و نامرتبط —
`AppNavigationTest`/`ShotsFlowTest` در اجرای اول، `ShotsFlowTest`/
`AssetFormFlowTest` در اجرای دوم — همان کلاس Flake محیطی از‌پیش‌مستند در
ADR-044/۰۶۲، تأییدشده با `git diff` که هیچ فایلی در مسیر وابستگی
این تست‌ها دست نخورده). `gradle :app:assembleDebug` جداگانه → موفق.

**طبق ترتیب رسمی ممیزی، تنها بند باقی‌مانده اکنون اولویت ۳ بند ۸ است**
(باقی موارد 🟡 بدون فوریت: G3-G5, G8, G12, G18).

### 🏁 فهرست رسمی ممیزی جامع post-Unit16 به پایان رسید

آخرین بند فهرست رسمی ممیزی (`docs/audit/post-unit16-full-audit.md`،
«پیشنهاد ترتیب رفع»، اولویت ۳ بند ۸ — G3، G4، G5، G8، G12، G18) با
رفتار متفاوت برای هرکدام رسیدگی شد:

1. **G3 (رفع واقعی، با یک محدودیت صادقانه):** `RoomOverrideEventLogger`
   (`data/repository/RoomOverrideEventLogger.kt`) پیاده‌سازی واقعی
   `OverrideEventLogger` است — از `EventLogDao` موجود بازاستفاده می‌کند.
   **یافته‌ی تازه‌تر و مهم‌تر از خودِ G3:** با grep تأیید شد کل مفهوم
   Human Override (واحد ۰۱) هنوز هیچ‌جای UI فراخوانی نمی‌شود — این Logger
   آماده و تست‌شده است، اما فعلاً هیچ کد تولیدی صدایش نمی‌زند.
2. **G12 (رفع بخشی):** `minTouchTargetEnabled` وصل شد (`ui/theme/AccessibilityLocals.kt`،
   اعمال‌شده روی `PhaseCircle` واحد AI Story Breakdown). `reducedMotionEnabled`
   با grep تأیید شد قابل‌وصل نبود — این اپ اصلاً هیچ انیمیشنی در هیچ‌جای
   UI ندارد.
3. **G8 (رفع واقعی):** `location` اکنون از داخل `SceneSettingsDialog` هم
   قابل تغییر است — با بازاستفاده از همان `LocationPickerDialog`/
   `connectLocationAsset` موجود (فاز ۴). `globalVisualStyle` (معنای دقیق
   نامشخص) موکول شد.
4. **G5 (تصمیم مستقل، موکول شد):** بعد از بررسی کد واقعی، چند-Outfit یک
   Refactor ساختاری واقعی است (نه چند خط) — موکول شد، اما به‌عنوان
   کوچک‌تر و مستقل‌تر از G4/G18 ثبت شد.
5. **G4 + G18 (مستندسازی آگاهانه):** Export واقعی فایل یک قابلیت محصولی
   Post-MVP واقعی است (نیاز به `Intent.ACTION_SEND`/`MediaStore`، اولین
   زیرساخت Android این‌چنینی در کل پروژه) — برای یک قدم مستقل آینده
   مستند و موکول شد.

جزئیات کامل هر تصمیم در `docs/adr/067-remaining-p3-findings.md`.

`gradle :app:testDebugUnitTest` → ۷۰۴ تست (۶۹۹→۷۰۴، ۵ تست جدید)، ۷۰۱
موفق. در اجراهای مکرر این قدم، هر بار ۱ تا ۳ شکست دیدیم — هر بار
تست‌های متفاوت و نامرتبط (`ScenesFlowTest`ی SQLiteConnectionPool،
`ShotsFlowTest`، `AssetFormFlowTest`، `AppNavigationTest`) — همان کلاس
Flake محیطی از‌پیش‌مستند در ADR-044/۰۵۹/۰۶۰/۰۶۲، تأییدشده با `git diff`
که هیچ فایلی در مسیر وابستگی این تست‌ها دست نخورده. `gradle
:app:assembleDebug` جداگانه → موفق.

**نتیجه:** فهرست رسمی «پیشنهاد ترتیب رفع» ممیزی جامع post-Unit16 اکنون
کاملاً رسیدگی‌شده است — همه‌ی یافته‌های 🔴/🟠 رفع واقعی شدند، و هر یافته‌ی
🟡 باقی‌مانده یا رفع شد یا آگاهانه به‌عنوان Post-MVP/قدم آینده مستند و
موکول شد (نه بی‌صدا فراموش). موارد باز باقی‌مانده برای تصمیم مشترک آینده:
G3 (اتصال واقعی Human Override به یک اقدام کاربر)، G4/G18 (Export واقعی
فایل)، G5 (چند-Outfit).

### 🎯 نقطه‌ی عطف: اتصال واقعی Human Override به UI (رفع یافته‌ی معماری G3/ADR-067)

کل مفهوم Human Override (واحد ۰۱، بخش ب) هرگز به هیچ اقدام واقعی کاربر
وصل نشده بود. **یافته‌ی مهم بازبینی مستقل این قدم (خلاف پیش‌بررسی دستور
کار):** با grep تأیید شد `OverrideEntity`/`OverrideDao` از قدم اول
واحد ۱۵ از قبل ساخته و در `AppDatabase.kt` ثبت شده بودند — دقیقاً همان
الگوی `EventLogDao` (قدم قبل، G3) که ساخته شد اما هرگز وصل نشد. یعنی این
قدم به هیچ Entity/Dao/Migration تازه‌ای نیاز نداشت.

1. **`HumanOverrideRepository`** (`data/repository/`) — نگاشت
   `HumanOverride` دامنه ↔ `OverrideEntity` موجود (هم‌الگو DTO محلی
   `@Serializable` با `ProjectEntityDto`/`ShotDto`).
2. **نقطه‌ی ورود UI — صفحه‌ی Validation:** هر `ValidationIssue` با
   `Severity.WARNING` یک دکمه‌ی «تجاوز از این هشدار» می‌گیرد (طبق Rule 1
   دامنه، `Severity.BLOCKING` هرگز این دکمه را نمی‌گیرد) → Dialog کوچک
   (نوع Override + دلیل اختیاری) → `createOverride` واقعی (دامنه، واحد
   ۰۱) با `RoomOverrideEventLogger` واقعی صدا زده و در `HumanOverrideRepository`
   ذخیره می‌شود. Issue بازبینی‌شده متمایز می‌شود (Strikethrough + برچسب
   «Override شده»)، نه ناپدید. یک مسیر Revoke Inline (روی همان کارت)
   هم اضافه شد — یک صفحه‌ی مستقل Browse-همه موکول شد (`OverrideEntity`
   فاقد `projectId` است).
3. **تصمیم مستقل مهم — نگاشت Scope:** `OverrideScope` دامنه برای «تغییر
   واقعی یک مقدار فیلد» طراحی شده، اما این نقطه‌ی ورود مقدار تازه‌ای
   نمی‌گیرد. راه‌حل: `originalValue = issue.message`، `overrideValue`
   یک Sentinel ثابت `"acknowledged_by_user"` — سازش صادقانه، نه
   سوءاستفاده‌ی خاموش. جزئیات کامل (و دو باگ Async واقعی کشف‌شده حین
   دیباگ) در `docs/adr/068-human-override-storage-and-ui.md`.

تست‌ها: `ValidationViewModelOverrideTest.kt` (۳ تست — Round-Trip واقعی
روی Room، رد Override برای BLOCKING، EventLog واقعی برای create/revoke)؛
`ValidationOverrideFlowTest.kt` (۱ تست End-to-End — کلیک واقعی دکمه →
Dialog → بج «Override شده» → Revoke → دکمه دوباره ظاهر می‌شود).

`gradle :app:testDebugUnitTest` → ۷۰۸ تست (۷۰۴→۷۰۸، ۴ تست جدید)، ۷۰۷
موفق. یک شکست، همان کلاس Flake محیطی از‌پیش‌مستند در ADR-044/۰۵۹/۰۶۰/۰۶۲
(این بار `ShotsFlowTest`) — `git diff` این قدم تأیید می‌کند هیچ فایلی
در مسیر وابستگی این تست دست نخورده. `gradle :app:assembleDebug`
جداگانه → موفق.

**قدم بعدی پیشنهادی:** G4/G18 — Export واقعی فایل (`Intent.ACTION_SEND`/
`MediaStore`) برای صفحه‌ی Output Delivery.

### 🏁 آخرین یافته‌ی معماری باز از سه موردی که با هم تصمیم گرفتیم رفع شد: Export واقعی فایل (G4/G18)

دکمه‌ی «Export» صفحه‌ی Output Delivery تا این قدم فقط مستعار «Copy» بود
(هر دو فقط Clipboard را عوض می‌کردند)؛ `composeOutput` (واحد ۱۴) هرگز از
`ui/` صدا زده نمی‌شد.

**بازبینی مستقل پیش‌بررسی دستور کار (تأیید شد، خلاف نبود):** با خواندن
مستقیم `Bilingual.kt` تأیید شد `generateBilingualPrompt`/`translateToFarsi`
واقعاً عمداً پیاده نشده‌اند (کامنت صریح خودِ فایل) — یک تصمیم معماری در
حال بحث بین معمار و کاربر پروژه، نه یک بدهی فنی این قدم.

1. **اتصال `composeOutput`:** `OutputDeliveryViewModel.regenerate()`
   اکنون بلافاصله بعد از رندر/پاک‌سازی موفق `composeOutput` را با
   `renderedOutputs = listOf(finalRenderedOutput)` (این ViewModel هر لحظه
   دقیقاً یک مدل انتخاب‌شده دارد) صدا می‌زند؛ نتیجه (`OutputPackage`) در
   `OutputDeliveryState.Ready.outputPackage` ذخیره می‌شود.
2. **Fallback صادقانه‌ی Bilingual:** تا انتخاب یک موتور ترجمه‌ی واقعی،
   `enVersion`/`faVersion` هر دو برابر همان متن رندرشده‌ی نهایی‌اند —
   نه ترجمه‌ی جعلی.
3. **اولین FileProvider کل پروژه:** `AndroidManifest.xml` +
   `res/xml/file_paths.xml` (`cacheDir/exports`) + `ExportFileWriter`/
   `DeviceExportFileWriter` (هم‌الگو با `BackupFileStorage`) +
   `buildExportShareIntent` (`ui/outputdelivery/ExportIntentBuilder.kt`).
   دکمه‌ی Export اکنون فایل‌ها را واقعاً می‌نویسد و یک
   `Intent.ACTION_SEND_MULTIPLE` واقعی (همیشه ≥۳ فایل: en/fa + هر مدل)
   با `Intent.createChooser` باز می‌کند — رفتاری کاملاً متفاوت از Copy.

جزئیات کامل (و یافته‌ی دیباگ درباره‌ی Cache شدن ریشه‌های FileProvider در
Robolectric بین کلاس‌های تست جدا) در
`docs/adr/069-real-output-export-and-bilingual-limitation.md`.

تست‌ها: `OutputDeliveryViewModelTest.kt` (۲ تست تازه — `OutputPackage`
واقعی با ۳ exportFiles، `exportOutput` واقعاً از طریق `ExportFileWriter`
تزریقی می‌نویسد)؛ `OutputDeliveryFlowTest.kt` (۱ تست End-to-End تازه —
کلیک واقعی روی Export → Chooser واقعی → Uri واقعاً از طریق FileProvider
قابل خواندن است، اثبات مستقیم که Export/Copy دیگر یک رفتار مشترک زیر دو
نام نیستند).

`gradle :app:testDebugUnitTest` → ۷۱۱ تست (۷۰۸→۷۱۱، ۳ تست جدید)، ۷۰۸
موفق. سه شکست، هر سه از همان کلاس Flake محیطی از‌پیش‌مستند در
ADR-044/۰۵۹/۰۶۰/۰۶۲ (`AssetFormFlowTest`، `ShotsFlowTest`، و یک SQLiteConnectionPool
متفاوت این‌بار در `OutputDeliveryFlowTest`ی «model picker chips» —
تست‌های نامرتبط، نه تست‌های تازه‌ی این قدم که همگی سبزند) — `git diff`
این قدم تأیید می‌کند هیچ فایلی در مسیر وابستگی این تست‌ها دست نخورده؛
`testDebugUnitTest`/`assembleDebug` جداگانه (طبق همان الگو) اجرا شدند —
`assembleDebug` → **موفق**.

**نتیجه:** هر سه یافته‌ی معماری باز از تصمیم مشترک (G3 Human Override،
G4/G18 Export واقعی، G5 چند-Outfit) اکنون یا رفع شده‌اند یا آگاهانه
مستند و موکول (G5 — جزئیات در `docs/adr/067-remaining-p3-findings.md`).
فهرست کامل «کارهای آینده‌ی شناخته‌شده» (ترجمه‌ی فارسی، Revoke Screen
مستقل، چند-Outfit، و بقیه) در بخش «محدودیت‌های شناخته‌شده» پایین همین
فایل جمع‌آوری شده است.

**وضعیت نهایی هر ۲۲ یافته‌ی این ممیزی** (بازبینی مستقل هر مورد با کد
فعلی، نه فقط متن ADR ها) اکنون در جدول انتهای خودِ
`docs/audit/post-unit16-full-audit.md` مستند است. **به‌روزرسانی
۲۰۲۶-۰۸-۱۳:** G10 (انتخاب تصویر Settings، با `ActivityResultContracts.OpenDocument`
واقعی) رفع شد؛ G11 با بازبینی مستقل بلوپرینت ۰۶/۱۴ تصحیح شد — یافته‌ی
اصلی آن ممیزی نادرست بود (`ImageReference`/`ReferenceImage` متن‌محور از
ابتدا طراحی عمدی و کامل بوده‌اند، نه یک Gap) — جزئیات در
`docs/adr/075-settings-image-picker-and-g11-correction.md`. سپس G8
(`globalVisualStyle` هم اکنون در کنار `location` از داخل
`SceneSettingsDialog` قابل ویرایش است) کامل رفع شد و جدول G14 تصحیح شد
— دو مورد «برای تصمیم مشترک» (State Machine/Lock، Storage/Import) که
هنوز معلق ثبت شده بودند، در واقع از قبل (به ترتیب ADR-066 و ADR-065)
رفع شده بودند — جزئیات در
`docs/adr/076-scene-globalvisualstyle-edit-and-g14-table-correction.md`.
سپس یک باگ ترجمه‌ی واقعی در همان خط نمایش `globalVisualStyle` (نام خام
Enum به‌جای برچسب فارسی) رفع شد، به‌همراه یک بررسی سراسری کل `ui/` برای
همین کلاس باگ — دو مورد مشابه دیگر (`contentDescription` دکمه‌های
سوییچ زبان/تم در Home) پیدا و رفع شد؛ فهرست کامل موارد بررسی‌شده
(رفع‌شده و False Positive رد‌شده) در
`docs/adr/077-globalvisualstyle-translation-bug-and-codebase-sweep.md`.

**قدم تشخیصی (بدون رفع):** یک Flake تازه در `HomeProjectsStudioFlowTest`
(Timeout/`NavBackStackEntry` Lifecycle، امضایی متفاوت از Flake تاریخی
`database.close()`) با ۵ اجرای تازه تأیید شد (نرخ ≈۳۳٪، ۲ از ۶ اجرای
مستقل). یک فرضیه‌ی مشخص (`ioScopeOverride` برای `ProjectListViewModel`)
آزمایش و با شواهد قوی رد شد — نتیجه را بدتر کرد، نه بهتر. ریشه‌ی دقیق
هنوز باز است؛ هیچ کد محصولی تغییر نکرد. جزئیات کامل در
`docs/adr/078-homeprojectsstudioflowtest-flake-investigation.md`.

### 🔎 قدم تشخیصی: بررسی ریشه‌ای Flake — بدون تغییر کد محصول

بعد از ۵ بار «Flake محیطی بی‌ضرر» رد شدن (ADR-044/059/060/062/069) بدون
بررسی ریشه‌ای، این قدم فقط تشخیصی بود (هیچ کد `app/src/main/` تغییر
نکرد). با ۷ اجرای واقعی تکراری (پایه، Heap افزایش‌یافته، و
`maxParallelForks=1`، هرکدام چندبار)، **ریشه با اطمینان بالا شناسایی
شد:** یک Race درون‌کلاسی بین `tearDown()`'s `database.close()` و
Coroutine های ناتمام `viewModelScope` روی Executor داخلی Room —
`composeRule.waitForIdle()` (رفع نیمه‌کاره‌ی موجود از ADR-052) این کار
پس‌زمینه را نمی‌بیند. حیاتی‌ترین شاهد: حتی با `maxParallelForks=1`
(بدون هیچ هم‌زمانی بین کلاس‌های تست)، همان امضای دقیق
`SQLiteConnectionPool has been closed` باز هم رخ داد — قطعاً رد می‌کند
«تداخل بین Process های موازی» را. رفع پیشنهادی (حذف کامل
`database.close()` از `tearDown()` هر تست End-to-End، چون
`Room.inMemoryDatabaseBuilder` نیازی به Close صریح ندارد) کوچک و
کم‌ریسک است اما **اجرا نشد — منتظر تأیید معمار**. جزئیات کامل (جدول هر
۷ اجرا) در `docs/adr/070-flake-root-cause-investigation.md`.

### 🔧 رفع پیشنهادی ADR-070 اجرا شد — نتیجه جزئی، نه کامل (صادقانه ثبت‌شده)

با تأیید معمار، `database.close()` از `tearDown()` هر ۱۹ فایل تست
End-to-End حذف شد (فقط `app/src/test/`، هیچ کد محصول دست نخورد). ۵
اجرای کامل و تازه‌ی `gradle :app:testDebugUnitTest` (طبق دستور صریح،
نه فقط ۲-۳ اجرا) نشان داد: **رفع جزئی موفق بود** — امضای دقیق
`SQLiteConnectionPool has been closed` که ADR-070 آن را مکانیزم اصلی
تشخیص داده بود، در هیچ‌کدام از ۵ اجرا دیگر دیده نشد. **اما یافته‌ی
مهم‌تر:** `ShotsFlowTest > "deleting a scene that still has a shot..."`
در هر ۵ اجرا، بدون استثنا، شکست خورد — با بازبینی جدول ADR-070 مشخص شد
همین یک تست در هر ۷ اجرای آن قدم تشخیصی هم شکست خورده بود (جمعاً ۱۲ از
۱۲ اجرای مستقل، پیش و پس از این رفع، بدون تأثیر از Heap/`maxParallelForks`
یا حذف `database.close()`). این یعنی این تست خاص یک ریشه‌ی کاملاً
جداگانه دارد — نه Race مربوط به `database.close()` — و به یک قدم
تشخیصی مستقل دیگر نیاز دارد (فرضیه‌ی اولیه: طولانی‌ترین زنجیره‌ی این
Suite با ۷ عملیات ناهمگام پشت‌سرهم، هرکدام با پنجره‌ی سخت‌گیرانه‌ی
۵۰۰۰ms — این فرضیه هنوز آزمایش نشده). جزئیات کامل در
`docs/adr/071-flake-mitigation-database-close-removal.md`.

### ✅ رفع `ShotsFlowTest > "deleting a scene..."` — یافته‌ی جدید و مجزا (ADR-072)

بررسی مستقل این تست خاص (نه ادامه‌ی رفع ADR-071) یک مکانیزم متفاوت پیدا
کرد: افزایش Timeout (آزمایش‌شده، ۵ اجرای کامل با `10_000ms`) نرخ شکست را
تغییر نداد و رد شد. مقایسه‌ی این تست با تست خواهرش (`"cancelling shot
deletion..."`، که هرگز شکست نمی‌خورد) نشان داد بعد از کلیک روی تأیید
حذف، تست ناموفق مستقیماً سراغ بررسی DB می‌رفت بدون این‌که ابتدا منتظر
ناپدیدشدن واقعی گره‌ی مربوطه در درخت Semantics کامپوز
(`composeRule.waitUntilDoesNotExist`) بماند — برخلاف تست موفق. افزودن
همین انتظار (برای حذف Shot و حذف Scene، هردو) نتیجه داد: **۵ از ۵ اجرای
کلاس مجزا موفق + ۵ از ۵ اجرای کامل Suite موفق** برای این تست مشخص —
در برابر ۱۷ شکست پیاپی پیشین (۱۲ اجرای ADR-070/071 + ۵ اجرای آزمایش
Timeout رد‌شده). این رفع فقط `app/src/test/` را تغییر داد. جزئیات کامل
(جدول هر اجرا) در `docs/adr/072-shotsflowtest-deleting-scene-fix-investigation.md`.

### ✅ رفع نهایی Flake `HomeProjectsStudioFlowTest` (ادامه‌ی ADR-078، ADR-079)

فرضیه‌ی پیشنهادی خودِ ADR-078 (یک `composeRule.waitForIdle()` صریح بعد
از تأیید ایجاد پروژه) آزمایش و **رد شد** — ۱ اجرا، همان ۲ شکست/همان
پیام‌ها، بدون تغییر. بررسی دقیق‌تر Race مستندشده در ADR-078 (Navigate
async از `ProjectListViewModel.createProject` → `onCreated` →
`HomeScreen.kt` → `AppNavHost.kt`) نشان داد ریشه‌ی واقعی، شرط انتظار
مبهم تست‌ها بود: `waitUntilAtLeastOneExists(hasText(projectName))`
می‌توانست فقط با به‌روزرسانی سریع‌تر فهرست Home ارضا شود، درحالی‌که
Navigate خودکار به Studio هنوز در حال اجرا بود — تست بعدی
(`BOTTOM_NAV_HOME_TAG`) با همان فراخوان async هنوز-معلق مسابقه می‌داد.
رفع: جایگزینی آن شرط با انتظار برای یک نشانه‌ی غیرمبهمِ «Navigate واقعاً
کامل شد» (خودِ Tab «داستان» در Studio، `studioTabTestTag(StudioTab.STORY)`).
فقط `app/src/test/` تغییر کرد؛ هیچ کد `app/src/main/` لازم نبود.
**۱۰ از ۱۰ اجرای کامل پیاپی Suite ترکیبی موفق** (همان استاندارد
ADR-072)، به‌علاوه `gradle :app:assembleDebug` موفق. جدول کامل هر ۱۰
اجرا در `docs/adr/079-homeprojectsstudioflowtest-flake-final-fix.md`.

### ✅ رفع ۳ فیلد بی‌اثر باقی‌مانده‌ی G12 (ADR-080) — ۲ مورد موکول

از ۵ فلگ Persist-only باقی‌مانده‌ی ADR-067 (`dynamicFontEnabled`،
`allowFreeStepJump`، `homeScreenImageUri`، `homeLayoutVariant`،
`composerLayoutVariant`)، ۳ مورد اول رفع شدند: **`dynamicFontEnabled`**
اکنون واقعاً `LocalDensity.fontScale` را (×۱.۳، معادل «Large Text»
اندروید) در سطح `Theme.kt` تغییر می‌دهد — کل اپ را می‌پوشاند، نه یک هدف
مشخص. **`allowFreeStepJump`** اکنون واقعاً کنترل می‌کند که آیا هشدار
پرش بین Tab های Studio (که هرگز خودِ پرش را Block نمی‌کند، طبق ADR-037)
به‌صورت غیرمزاحم (Snackbar) نمایش داده شود یا یک Dialog تأیید صریح لازم
باشد. **`homeScreenImageUri`** اکنون به‌عنوان پس‌زمینه‌ی Full-bleed واقعی
Home رندر می‌شود (دیکود با API بومی اندروید، بدون افزودن Coil/Glide).
**`homeLayoutVariant`/`composerLayoutVariant`** موکول شدند — نیازمند یک
طراحی UI تازه (نه فقط اتصال یک مقدار موجود)، طبق قانون
mockup/تأیید بصری پروژه. جزئیات کامل + جدول تست در
`docs/adr/080-g12-remaining-persist-only-settings.md`.

### ✅ رفع homeLayoutVariant.RESUME + composerLayoutVariant.ACCORDION (ADR-081) — فرض ADR-080 اشتباه بود

یک mockup کامل و تعاملی برای هر دو حالت جایگزین از قبل در
`docs/design/Cinema Studio.html` وجود داشت (بخش‌های `homeB`/`composerB`)
— فقط داخل رشته‌های JS تودرتوی این فایل ۲.۱ مگابایتی پنهان بود و با
خواندن سطحی/README خلاصه قابل کشف نبود. **RESUME**: کارت «ادامه از
جایی که ماندید» برای آخرین پروژه (نوار پیشرفت ۹ بخشی + دکمه‌ی ادامه)،
۴ کاشی میان‌بر (AI Breakdown/DNA/اعتبارسنجی/خروجی)، فهرست افقی
«پروژه‌های دیگر» — با یک محدودیت صادقانه‌ی مستند: نوار پیشرفت فقط وقتی
`WorkflowState` واقعی این نشست برای همان پروژه موجود باشد دقیق است؛
در غیر این صورت (اپ تازه باز شده) از شمارش واقعی صحنه/شات به‌جای حدس
استفاده می‌کند. **ACCORDION**: ۴ گروه هم‌ارز (اصلی/دوربین/نور و
محیط/صدا)، همیشه فقط یک گروه باز (کلیک روی گروه باز آن را می‌بندد،
دقیقاً طبق منطق mockup). یافته‌ی دیباگ واقعی: `performClick()` روی
سربرگ گروه با `ComposeTimeoutException` شکست می‌خورد — رفع با همان
راه‌حل قبلاً مستندشده‌ی این پروژه (`performSemanticsAction`، نه
`performClick()`). یک بررسی سراسری تکمیلی mockup در برابر کل ۱۲ صفحه‌ی
کد فعلی هم انجام شد (به‌علاوه‌ی ۴ عنصر مشترک: Nav Drawer، Bottom-sheet،
Toast، هدر سراسری) — فهرست کامل و غیرخلاصه‌شده‌ی هر مورد (کامل/جزئی/
پیاده‌نشده، با مرجع دقیق mockup + کد) در
`docs/adr/081-appendix-mockup-audit.md`. این بررسی چند کاستی واقعی و
مستندنشده کشف کرد (مثل Tab «خروجی» در Studio که هنوز کاملاً Placeholder
است، نبود هیچ `ModalBottomSheet` در کل کدبیس، FAB مرکزی نوار پایین که
هیچ‌کاری انجام نمی‌دهد) — طبق قانون این قدم عمداً رفع نشدند و منتظر
تصمیم معمار پروژه‌اند. جزئیات کامل + جدول تست در
`docs/adr/081-home-resume-composer-accordion-layouts.md`.

### ✅ رفع ۵ یافته‌ی سطح ۱ (کوچک) از appendix ممیزی mockup (ADR-082)

اولین دسته از یک لیست اولویت‌بندی‌شده‌ی بزرگ‌تر از یافته‌های
`docs/adr/081-appendix-mockup-audit.md`، همگی کوچک/کم‌ریسک: (۱) FAB مرکزی
سراسری «ایجاد سریع» که قبلاً کاملاً بی‌اثر بود، اکنون از هر صفحه دیالوگ
ایجاد پروژه را باز می‌کند؛ (۲) بنر «On-Device مطلق — بدون همگام‌سازی
ابری» (متن دقیق mockup) به Backups اضافه شد؛ (۳) ردیف چیپ فیلتر وضعیت
(همه/DRAFT/REVIEW/LOCKED/FINAL/ARCHIVED) به Projects اضافه شد؛ (۴) ریل
نقطه+خط اتصال واقعی برای حالت Timeline در Shots List (قبلاً دقیقاً همان
Grid بود)؛ (۵) گرادیان دقیق mockup (`#2C4260→#3C5570→#6E5B72`) جایگزین
رنگ تخت نوار Hero در Shot Composer شد. یافته‌ی دیباگ واقعی: تست فیلتر
Projects با `ComposeTimeoutException` شکست خورد چون `LazyColumn` در
viewport کوچک تست آیتم دوم فهرست را اصلاً Compose نمی‌کرد — رفع با
بازطراحی توالی Assertion‌ها (نه صرفاً performScrollTo). جزئیات کامل +
جدول تست در `docs/adr/082-appendix-audit-tier1-fixes.md`.

### ✅ رفع ۴ یافته‌ی سطح ۲ از appendix ممیزی mockup (ADR-083)

دومین دسته: (۱) Badge شمارشی زنده روی Tab «شات‌ها»ی Scene Detail (طبق
mockup، Tab «دارایی‌ها» عمداً بدون Badge ماند چون اتصال واقعی
Asset↔Scene هنوز پیاده نیست — Badge همیشه-صفر گمراه‌کننده بود)؛ (۲)
کاشی پیش‌نمایش واقعی ۱۴۰px تصویر Home در Settings (قبلاً فقط متن خام
URI)، با استخراج منطق decode مشترک از HomeScreen.kt؛ (۳) رشته‌ی نسخه‌ی
واقعی (`BuildConfig.VERSION_NAME`) به کارت «درباره» اضافه شد — لوگوی
گرافیکی سفارشی Aperture-C رسماً به‌عنوان بدهی طراحی (نه کدی) موکول ماند؛
(۴) نوار پیشرفت ۳بخشی AI-Breakdown Tab داستان اضافه شد — یافته‌ی دیباگ
واقعی: خواندن خام HTML نشان داد رنگ هر ۳ Segment در خودِ mockup Literal
است (نه `{{ }}`-bound به هیچ State ای)، یعنی این نوار در mockup هم
صرفاً یک نشانه‌ی بصری ثابت است، نه Progress Indicator متصل به داده —
پیاده‌سازی این اپ هم دقیقاً همان رفتار ثابت را کپی کرد. جزئیات کامل +
جدول تست در `docs/adr/083-appendix-audit-tier2-fixes.md`.

### ✅ Tab «خروجی» Studio — بزرگ‌ترین یافته‌ی باز appendix (ADR-084)

یافته‌ی #۱۴ appendix ADR-081: `StudioShell.kt` تا این قدم Tab «خروجی» را
به `StudioTabPlaceholder` می‌فرستاد. اکنون طبق mockup (`st.output`) یک
Dashboard سطح‌پروژه‌ی واقعی است — نه خودِ `OutputDeliveryScreen` (که
نیازمند `shotId` مشخص است، درحالی‌که این Tab سطح‌پروژه است): (۱) کارت
وضعیت Validation با شمارش واقعی Blocking/Warning کل پروژه (کلیک →
Validation)؛ (۲) کارت آماده‌بودن Prompt Generation با شمارش واقعی «N از
M شات آماده (بدون خطای Blocking)» — به‌جای «Quality Score» جعلی mockup
که هیچ الگوریتم واقعی پشتش نیست؛ (۳) دکمه‌ی گرادیانی اشتراک‌گذاری (کلیک
→ Output Delivery). چون هر دو مقصد Navigate نیازمند یک Shot مشخص‌اند و
این Tab بدون Shot خاص است: صفر Shot → غیرفعال؛ یک Shot → مستقیم؛ چند
Shot → دیالوگ انتخاب کوتاه. پیاده‌سازی سبک‌وزن ماند —
`ShotRepository.loadAllShotsForProject` تازه + یک `StudioOutputViewModel`
که فقط `aggregateShotValidation` موجود (واحد ۰۷) را روی همه‌ی Shot های
پروژه حلقه می‌زند، بدون هیچ Rule دامنه‌ی تازه. جزئیات کامل تصمیم‌ها
(جایگزینی Quality Score، تفکیک Shot) + یافته‌های دیباگ + جدول تست در
`docs/adr/084-studio-output-tab.md`.

### ✅ اتصال واقعی Asset↔Scene برای Tab «دارایی‌ها» (ADR-085)

یافته‌ی #۱۱ appendix ADR-081: Tab «دارایی‌ها»ی Scene Detail تا این قدم
همیشه یک empty-state ثابت نشان می‌داد، صرف‌نظر از وجود اتصال واقعی — هیچ
رابطه‌ی ذخیره‌شده‌ای بین Scene و Character/Object Asset وجود نداشت (فقط
`locationAssetId` تکی از قبل). اکنون `Scene.linkedAssetIds: List<String>`
تازه (همان الگوی کم‌ریسک `locationAssetId` — بدون Migration رسمی Room،
چون `SceneEntity` فقط یک JSON Blob است، نه ستون‌های مجزا؛ با یک تست واقعی
Decode روی JSON قدیمی بدون این کلید هم اثبات شد) این اتصال را نگه می‌دارد.
Tab اکنون فهرست واقعی کارت‌های متصل (آیکون بر اساس نوع + نام + متا +
`continuityLockLevel`، طبق نمونه‌ی واقعی mockup) + دکمه‌ی افزودن (که طبق
تصمیم ثابت این پروژه — بدون ModalBottomSheet — یک Dialog باز می‌کند، نه
Sheet) را نشان می‌دهد؛ Badge شمارشی Tab هم اضافه شد (بستن یافته‌ی #۶
appendix که قبلاً در ADR-083 عمداً بدون Badge رها شده بود). یک دکمه‌ی حذف
اتصال هم اضافه شد — mockup آن را نشان نداده اما بدون آن این ویژگی
یک‌طرفه و ناقص می‌بود. جزئیات کامل + جدول تست در
`docs/adr/085-scene-asset-linking.md`.

### ✅ پنل خلاصه‌ی زنده‌ی پایین Shot Composer (ADR-086)

یافته‌ی #۱۳ appendix ADR-081 — آخرین یافته‌ی سطح ۴ باقی‌مانده. Shot Composer
اکنون یک پنل خلاصه‌ی زنده در پایین صفحه دارد: نوار قابل‌کلیک شمارش
Blocking/Warning (از `aggregateShotValidation` واقعی، بازمحاسبه‌شده روی هر
تغییر فیلد شات، بدون Debounce — ارزیابی صریح نشان داد این محاسبه‌ی خالص
قطعاً کم‌هزینه‌تر از I/O ذخیره‌سازی موجود است که از قبل روی همان نقطه سوار
بود)، کارت برچسب «Full Video Prompt» با هشدار شرطی، و ردیف چیپ‌های مدل
(veo/kling/runway/seedance) که هرکدام یک آیکون کپی نام مدل به Clipboard واقعی
دارند. دو یافته‌ی اصلاح‌کننده‌ی فرض اولیه‌ی دستور کار، هر دو با خواندن دقیق
markup خام mockup (نه حدس): کارت پیش‌نمایش متن زنده‌ی Prompt نیست (فقط
برچسب+هشدار)، و خودِ چیپ مدل قابل‌کلیک نیست (فقط آیکون کپی داخلش). یافته‌ی
واقعی دیباگ این قدم: چیدمان اولیه‌ی Pinned (`flex:none` دقیق mockup) روی
Viewport محدود می‌توانست کل محتوای فرم را به ارتفاع صفر فشرده کند — رفع با
ادغام همه‌چیز در یک ناحیه‌ی Scroll واحد (انحراف مستند از Pinned دقیق، بدون
تأثیر روی دستگاه‌های واقعی). جزئیات کامل + جدول تست در
`docs/adr/086-shot-composer-summary-panel.md`.

### ✅ دکمه‌های سریع زبان/تم روی هدر هر صفحه‌ی داخلی (ADR-087)

یافته‌ی #۱۶ appendix ADR-081: فقط `HomeScreen` دکمه‌های سریع
Translate/تم داشت؛ همه‌ی صفحات داخلی دیگر فقط برگشت+عنوان داشتند. اکنون
هر ۱۲ صفحه‌ی داخلی (۸ صفحه از طریق گسترش `AssetFormHeader` مشترک + ۳ صفحه
با هدر خصوصی خودشان — `SceneDetailScreen`/`StudioShell`/
`AiStoryBreakdownScreen`) همان دو دکمه را دارند؛ طبق تصمیم قطعی این قدم،
**بدون** ساختن یک Composable هدر سراسری تازه یا بازنویسی ساختاری —
فقط تزریق دو `IconButton` به هدر موجود هر صفحه، برای پرهیز از ریسک
Refactor بزرگ روی ۱۲+ فایل. جزئیات کامل + جدول تست در
`docs/adr/087-global-language-theme-quick-access.md`.

### ✅ پرش مستقیم Nav Drawer به Validation/Output Delivery با دقیقاً یک Shot (ADR-088)

یافته‌ی #۱۰ appendix ADR-081: لینک‌های «اعتبارسنجی»/«تحویل خروجی» در Nav
Drawer طبق ADR-061 همیشه به Tab «صحنه‌ها» می‌رفتند (چون Drawer هیچ `shotId`
ای نمی‌شناسد). اکنون اگر پروژه‌ی جاری دقیقاً یک Shot داشته باشد، این دو لینک
مستقیماً همان Shot را باز می‌کنند؛ در غیر این صورت (صفر/بیش‌از‌یک Shot)
رفتار قبلی بدون تغییر می‌ماند. منطق در `MainScaffold.kt` (نه `NavDrawer.kt`،
که عمداً لایه‌ی Presentation خالص باقی ماند) با `ShotRepository`/
`SceneRepository` موجود پیاده شد. جزئیات کامل + جدول تست در
`docs/adr/088-nav-drawer-single-shot-jump.md`.

### ✅ Export All / Import Backup در صفحه‌ی Backups (ADR-089)

یافته‌ی #۱۲ appendix ADR-081 (آخرین یافته‌ی سطح ۳ باقی‌مانده): صفحه‌ی Backups
فقط عملیات تک‌تک (Restore/Delete) داشت. اکنون دو دکمه‌ی تازه دارد: «خروجی
گرفتن از همه» (تمام بکاپ‌های پروژه با `Intent.ACTION_SEND_MULTIPLE` واقعی
اشتراک‌گذاری می‌شوند، هم‌الگو با Export خروجی پرامپت ADR-069) و «وارد کردن
بکاپ» (یک فایل بکاپ خارجی، بعد از اعتبارسنجی — شکل معتبر JSON + تطابق
`projectId` — فقط به فهرست همین پروژه معرفی می‌شود؛ Restore فوری نیست).
جزئیات کامل + جدول تست در `docs/adr/089-backups-export-all-import.md`.

### ✅ اتصال واقعی نشان «Aperture C» به کد (ADR-090)

یافته‌ی #۸ appendix ADR-081 (آخرین یافته‌ی UI باقی‌مانده): تا اینجا فقط یک
آیکون Material موقت (`Icons.Filled.Movie`/`MovieFilter`) جای نشان سفارشی
Aperture C را می‌گرفت — موکول‌شده در ADR-083 چون دارایی گرافیکی واقعی وجود
نداشت. اکنون که فایل‌های اصلی لوگو در پروژه‌اند، هر ۸ Path/Gradient
`docs/design/logo/aperture-c-mark.svg` دقیق و کامل (نه ساده‌شده) به یک
Vector Drawable واقعی (`ic_aperture_c_logo.xml`) تبدیل شدند و در هر دو محل
آیکون موقت موجود وصل شدند — Pill هدر Home (یکی از سه محل مستند mockup) و
کارت «درباره»‌ی Settings (قبلاً بدون هیچ آیکونی) — به‌همراه کاشی «خروجی»
Home Resume Layout (طبق درخواست صریح این قدم). سومین محل مستند mockup
(«drawer header») در کد این پروژه اصلاً هیچ‌گاه ساخته نشده (نه یک آیکون
موقت که باید تعویض شود — یک بخش UI کاملاً غایب)؛ خارج از دامنه‌ی این قدم
ماند، فقط گزارش شد. جزئیات کامل + توضیح روش تبدیل در
`docs/adr/090-aperture-c-real-logo.md`.

### ✅ آیکون اصلی اپ (Launcher) با نشان واقعی Aperture C (ADR-091)

یافته‌ی #۸ appendix ADR-081 — تکمیل نهایی. آخرین Placeholder موقت باقی‌مانده
(`ic_launcher_foreground.xml`، یک دایره‌ی زرد ساده) با نسخه‌ی Scale‌شده‌ی
همان نشان Aperture C جایگزین شد — نه ارجاع مستقیم، چون شعاع بیرونی واقعی
طرح (تأییدشده با نمونه‌برداری نقطه‌به‌نقطه‌ی مستقیم روی مسیرها، نه صرفاً bbox)
از نیم‌قطر Safe Zone تضمین‌شده‌ی Adaptive Icon (۳۳dp از ۶۶dp) بزرگ‌تر بود؛
با Scale ۰.۳۰۷۷ دقیقاً داخل آن جا داده شد. رنگ پس‌زمینه (`#1A1A2E`) بعد از
بررسی کنتراست واقعی WCAG هر ۹ رنگ گرادیان (۶ از ۹ ≥۴.۵:۱) دست‌نخورده ماند.
تأیید بصری واقعی (رندر SVG معادل زیر ماسک دایره‌ای کامل در ۹۶dp/۴۸dp) بدون
هیچ بریدگی بود. جزئیات کامل در `docs/adr/091-launcher-icon-real-logo.md`.

### ✅ وصل شدن گروه «کاندید رفع نزدیک» G14 — چهار Rule یتیم کوچک (ADR-092)

طبق جدول تصمیمات ADR-064: Audio Timeline (`validateActionSoundTimeline`)
اکنون در `ValidationAggregator` Level 1 وایر است؛ هشدار نام مشابه
(`checkSimilarAssetName`) در هر سه فرم Asset زنده کار می‌کند؛ و هر سه Rule
باقی‌مانده‌ی AI Story Breakdown (طول داستان آزاد، تعداد شات بالا، تکه‌ی
ناقص) هم‌الگو دقیق با Rule 2 موجود وصل شدند. یافته‌ی مستقل: مورد چهارم
(`validateDataCompleteness`) عمداً وصل **نشد** — با خواندن کامل کد مشخص شد
این تابع از سال‌ها پیش (Migration ۴، ADR-010) دقیقاً معادل دو Rule
از‌قبل‌وایرشده‌ی دیگر است؛ وصل کردنش هر نقض واقعی را دوبار در صفحه‌ی
Validation نشان می‌داد. جزئیات کامل در
`docs/adr/092-g14-close-candidates-wiring.md`.

### ✅ حذف کد مرده‌ی G14 — validateAssetIdUniqueness و validateBilingualCompleteness (ADR-093)

طبق دسته‌ی «ج: منسوخ، کاندید حذف» ADR-064: `validateAssetIdUniqueness`
(واحد ۰۶) صفر فراخوان‌کننده داشت — `assetId` همیشه با UUID خودکار ساخته
می‌شود، کاربر هرگز مستقیماً وارد نمی‌کند، پس یکتایی از قبل تضمین بود. این
تابع + دو تست اختصاصی‌اش کامل حذف شدند. مورد دوم (حذف کل
`OutputComposer.kt`) ابتدا اجرا **نشد** — بررسی نشان داد بخش عمده‌ی این
فایل (`composeOutput`، `OutputPackage`، `ExportFile`) هنوز توسط صفحه‌ی
واقعی Output Delivery و Export All بک‌آپ‌ها استفاده می‌شود؛ فقط
`validateBilingualCompleteness` واقعاً مرده بود (صفر فراخوان‌کننده). با
تأیید کاربر، فقط همین تابع مرده + دو تست اختصاصی‌اش حذف شدند، بدون لمس
بقیه‌ی فایل. جزئیات کامل در
`docs/adr/093-g14-dead-code-removal-asset-id-uniqueness.md`.

### ✅ پیاده‌سازی G5 — مدیریت چند-Outfit شرطی برای Character Asset (ADR-067 بخش ه، ADR-095)

فرم Character اکنون یک لیست واقعی `Outfit` مدیریت می‌کند (نه دو رشته‌ی تخت
+ همیشه دقیقاً یک Outfit): افزودن/حذف، تعیین پیش‌فرض، و سه شرط اختیاری
(آب‌وهوا/زمان روز/نوع مکان — هر سه از enum بسته‌ی از‌قبل‌موجود در دامنه، نه
رشته‌ی آزاد). دامنه (`selectOutfitForScene`، `enforceCharacterContinuity`)
از قبل درست پیاده بود و دست‌نخورده ماند — این قدم فقط UI را وصل کرد. رفتار
مرزی: حذف تنها Outfit باقی‌مانده مسدود می‌شود (ساختاری، چون منطق دامنه روی
لیست خالی Crash می‌کند)؛ حذف Outfit پیش‌فرض اولین عضو باقی‌مانده را خودکار
پیش‌فرض می‌کند. جزئیات کامل در `docs/adr/095-g5-multi-outfit-management.md`.

### ✅ تکمیل G5 — انتخاب خودکار Outfit اکنون به timeOfDay/locationType هم توجه می‌کند (ADR-096)

محدودیت مستندشده‌ی ADR-095 بسته شد: `selectOutfitForScene`/
`selectExpressionForScene` قبلاً فقط `condition.weather` را مقایسه
می‌کردند؛ اکنون هر سه فیلد `OutfitCondition` را با منطق AND-روی-فیلد-پرشده
بررسی می‌کنند (یک فیلد `null` در condition یعنی «قیدی روی این بعد نیست»، نه
«باید صحنه هم null باشد»). `enforceCharacterContinuity` بدون تغییر امضا،
اکنون `scene.timeOfDay`/`scene.location.type` را هم از خودِ `scene` موجود
استخراج و پاس می‌دهد. `selectExpressionForScene` برای تقارن امضایش گسترش
یافت اما همچنان صفر فراخوان‌کننده‌ی واقعی دارد (کد مرده‌ی احتمالی، تأییدشده
با grep). جزئیات کامل در `docs/adr/096-g5-multi-field-outfit-condition-matching.md`.

### ✅ حذف کد یتیم selectOutfitForShot (ADR-097)

`domain/shot/ShotOutfitSelection.kt` (واحد ۰۵) از ابتدای پیاده‌سازی (ADR-006
§۵) هرگز به هیچ مصرف‌کننده‌ی واقعی وصل نبود — مسیر واقعی انتخاب خودکار Outfit
همیشه `enforceCharacterContinuity` (واحد ۱۱) بوده. با چند grep مستقل تأیید
شد صفر فراخوان‌کننده‌ی واقعی دارد؛ تصمیم صریح کاربر پروژه: حذف. فایل + تست
اختصاصی‌اش کامل حذف شدند؛ کامنت ارجاع‌دهنده در
`CharacterAssetFormViewModel.kt` اصلاح شد. جزئیات کامل در
`docs/adr/097-remove-dead-selectoutfitforshot.md`.

### ✅ G2 قدم ۱ از ۳ — لایه‌ی ذخیره‌سازی امن کلید API (ADR-098)

اولین زیرقدم از Option B (ADR-035) — فقط زیرساخت ذخیره‌سازی، بدون هیچ اتصال
HTTP یا UI. `SecureKeyRepository.kt` (چندسرویسی — یک کلید مستقل به‌ازای هر
`profileId`) با `androidx.security.crypto` (`EncryptedSharedPreferences` روی
`MasterKey`/Android Keystore واقعی) ساخته شد. دو یافته‌ی مهم حین این قدم: (۱)
این کتابخانه از نسخه‌ی `1.1.0-beta01` به بعد Deprecated است (گوگل جایگزین
مستقیم Keystore را توصیه می‌کند) — با این حال طبق تصمیم صریح معماری همچنان
استفاده شد؛ (۲) Robolectric (نسخه‌ی نصب‌شده‌ی پروژه) از `AndroidKeyStore`
واقعی پشتیبانی نمی‌کند (محدودیت شناخته‌شده‌ی خودِ Robolectric، تأییدشده
تجربی با شکست ۶ از ۶ تست اول) — برای همین یک `SharedPreferences` تزریق‌پذیر
(هم‌الگو با `idProvider`/`clock` در `ProjectRepository.kt`) اضافه شد تا
منطق CRUD واقعاً قابل‌تست بماند؛ تأیید رمزنگاری واقعی روی دستگاه واقعی هنوز
یک محدودیت مستند است. جزئیات کامل در `docs/adr/098-g2-step1-secure-key-storage.md`.

### ✅ تصمیم آگاهانه — عدم مهاجرت زودهنگام به datastore-tink (ADR-099)

بررسی مستقل بیشتر نشان داد جایگزین رسمی گوگل برای `security.crypto`
Deprecated (`androidx.datastore:datastore-tink`) هنوز در اولین انتشار
**Alpha** است (`1.3.0-alpha07`، ۱۱ آوریل ۲۰۲۶) — بدون تضمین پایداری API.
تصمیم صریح: فعلاً روی `security.crypto` (پایدار، کارکردی) باقی می‌مانیم؛
ریسک واقعی یک مهاجرت زودهنگام به یک کتابخانه‌ی Alpha بزرگ‌تر از هزینه‌ی
نگه‌داشتن یک هشدار Deprecation بی‌ضرر است، برای این مرحله از یک MVP شخصی.
شرط بازبینی (وقتی `datastore-tink` به Stable/RC برسد) به‌عنوان **R6** در
`docs/governance/risk-register.md` ثبت شد — جزئیات کامل در
`docs/adr/099-defer-datastore-tink-migration.md`.

### ✅ G2 قدم ۲ از ۳ — اتصال HTTP واقعی به Claude API (ADR-100)

`sendToAiConnector` (`AiConnector.kt`، تا این قدم عمداً `TODO()` طبق ADR-035
Option A) اکنون یک درخواست HTTP واقعی می‌فرستد — Ktor Client با OkHttp
Engine (`engine` تزریق‌پذیر، پیش‌فرض واقعی برای Production، `MockEngine`
برای تست‌ها؛ هم‌الگو با تزریق‌پذیری `SecureKeyRepository.kt`). اولین
`AiConnectorProfile` واقعی اضافه شد: **Claude API** (Endpoint/Header/بدنه/
مسیر پاسخ تأییدشده مستقیم از مستندات رسمی Anthropic، نه حدس). به‌جای یک
`data class` تایپ‌شده‌ی مخصوص یک سرویس، یک Parser عمومی مسیر نقطه‌ای
(`content[0].text`/`choices[0].message.content`) نوشته شد — چندسرویسی‌بودن
واقعی `responseJsonPath` را حفظ می‌کند. Timeout: ۱۵ ثانیه اتصال، ۶۰ ثانیه کل
درخواست (شبکه‌ی موبایل + زمان تولید پاسخ LLM). این قدم مستقل از G2 قدم ۱
(ADR-098) است — کلید همچنان پارامتر ورودی است؛ خواندنش از
`SecureKeyRepository` و UI انتخابگر مسیر، قدم ۳ (بعدی) است. جزئیات کامل در
`docs/adr/100-g2-step2-real-http-claude-api.md`.

### ✅ G2 قدم ۳ از ۳ (آخرین قدم) — UI کلید API + انتخابگر مسیر ۱/۲؛ **G2 اکنون کاملاً بسته شده** (ADR-101)

آخرین قدم G2 — این دو زیرساخت مستقل (ADR-098: ذخیره‌سازی امن کلید، ADR-100:
HTTP واقعی) برای اولین بار به UI وصل شدند. **Part A (Settings):** کارت تازه‌ی
«کلید API سرویس‌های AI» — برای هر `AiConnectorProfile` در
`BUILTIN_AI_CONNECTOR_PROFILES` (فعلاً فقط Claude، اما خودکار برای چندسرویسی)
یک ردیف با فیلد ماسک‌شده (`PasswordVisualTransformation`)، دکمه‌ی ذخیره،
وضعیت «ذخیره شده/نشده» (هرگز خودِ کلید دوباره نمایش داده نمی‌شود)، و دکمه‌ی
حذف. یک `ApiKeysViewModel` کوچک و مجزا ساخته شد (نه افزودن به
`WorkflowViewModel` — کامنت سربرگ خودِ آن فایل صریحاً انضباط دامنه‌اش را رد
می‌کرد). یک اعلان یک‌باره‌ی هزینه («هزینه‌ی واقعی استفاده... بر عهده‌ی شماست»)
زیر عنوان کارت — نه هشدار مزاحم هر استفاده. **Part B (AI Story Breakdown):**
در فاز ۱، مسیر ۲ («ارسال خودکار به Claude») «کنار» دکمه‌ی کپی موجود (مسیر ۱)
اضافه شد — طبق تصمیم محصولی صریح، نه جایگزین آن. شرط سخت‌گیرانه‌ی UI: تا کلید
ذخیره نشده، دکمه‌ی ارسال خودکار در سطح UI Disabled است (نه فقط خطای بعد از
کلیک). ارسال موفق دقیقاً از همان زنجیره‌ی Parse/Repair مشترک
(`applyProcessAiResponseResult`) عبور می‌کند که مسیر ۱ استفاده می‌کند — بدون
کد تکراری. شکست HTTP کاربر را گیر نمی‌اندازد: فاز عوض نمی‌شود، کاربر بلافاصله
می‌تواند از مسیر ۱ استفاده کند. یافته‌ی مهم حین راستی‌آزمایی: افزودن یک
فراخوان بدون‌شرط `hasApiKey` در `init{}` دو ViewModel، تست‌های موجود
(`AiStoryBreakdownViewModelFactoryTest`، `SettingsFlowTest` — ۹ از ۱۰ تست) را
واقعاً می‌شکست (Robolectric فاقد AndroidKeyStore واقعی است)؛ با `runCatching`
(fail-closed به «ذخیره‌نشده») رفع شد. جزئیات کامل (شامل این رگرسیون و رفعش)
در `docs/adr/101-g2-step3-api-key-ui-and-auto-send.md`.

**نتیجه: G2 («اتصال واقعی به AI Connector») اکنون به‌طور کامل بسته شده است —
هر سه قدم (ADR-098، ADR-100، ADR-101) انجام شده‌اند.** کاربر می‌تواند
end-to-end: کلید را در Settings وارد کند → داستان بنویسد → با یک کلیک به
Claude ارسال کند → مستقیماً Scene/Shot/Asset دریافت کند — یا در هر لحظه به
مسیر ۱ (کپی/پیست دستی) برگردد.

## Stack

- **زبان:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **معماری:** MVVM ساده (ViewModel + StateFlow) — بدون فریمورک DI در فاز اول
- **Navigation:** Navigation Compose (مسیرهای Type-Safe با `@Serializable`) — ساختار دو‌لایه طبق DDR-002 (واحد ۱۶ فاز ۰)
- **Preference سبک UI (زبان/تم/Layout A-B):** DataStore Preferences — جدا از Room (که فقط برای Entity های دامنه است)
- **ذخیره‌سازی:** Room 2.8.4 + KSP (روی SQLite) + kotlinx.serialization (برای فیلدهای `*DataJson`) — پشتیبانی از چند پروژه‌ی همزمان؛ کاملاً پیاده‌سازی و به دامنه وصل شده (واحد ۱۵ تکمیل‌شده)
- **اتصال AI (اختیاری):** Ktor Client — فقط وقتی کاربر کلید API شخصی وارد کند
- **ذخیره‌سازی امن کلید API:** `androidx.security.crypto` 1.1.0 (`EncryptedSharedPreferences` روی Android Keystore) — چندسرویسی (`SecureKeyRepository`، ADR-098) و کاملاً به UI وصل (Settings، ADR-101)؛ Deprecated طبق گوگل از نسخه‌ی 1.1.0-beta01 به بعد، اما همچنان استفاده می‌شود (تصمیم صریح معماری)

## ساختار

```
app/src/main/java/com/operaboys/cinemashotgenerator/
├── data/    → Room entities, DAO, Database, Repository (واحد ۱۵ + Project/Story Repository واحد ۱۶)
│   ├── entity/ → ۱۳ Room Entity (Project/Scene/Shot/Asset/PromptBlueprint/RenderedOutput/Override/Version/DependencyEdge/EventLog/ProjectDna/AudioContext/StoryContext) — ProjectEntity فیلد state گرفت (فاز ۱)؛ StoryContextEntity جدید (فاز ۲ قدم ۱، فیلدهای مسطح)
│   ├── dao/    → DAO های suspend/Flow متناظر + ProjectTransactionDao (اثبات Atomicity)؛ ProjectDao.getAllProjectsWithCounts با Correlated Subquery شمارش صحنه/شات؛ StoryDao جدید
│   ├── repository/ → Settings/Versioning/ImpactAnalysis/ProjectDna/Asset/Scene/AudioContext/Project/Story Repository + PromptGenerationRepository.collectData + DTO محلی (data ← domain مجاز، domain ← data ممنوع)
│   └── AppDatabase.kt → RoomDatabase + Singleton Provider (بدون DI)
├── domain/  → مدل‌های دامنه و منطق کسب‌وکار
│   ├── story/  → واحد ۰۱: Story Wizard + Human Override
│   ├── storybreakdown/ → واحد ۰۱ب: AI Story Breakdown (کامل — Prompt Builder + Chunk Combiner + JSON Doctor + Story-to-Domain Mapper + AI Connector Profile؛ HTTP واقعی Ktor و فهرست پروفایل‌های واقعی سرویس‌ها کار آینده هستند)
│   ├── dna/    → واحد ۰۲: DNA Manager (Soft Lock)
│   ├── asset/  → واحد ۰۶: Asset & Continuity (Hard Lock)
│   ├── validation/ → واحد ۰۷: Validation & Consistency Engine
│   ├── visualidentity/ → واحد ۰۳: Visual Identity (Style Matrix + Cinematic Language)
│   ├── shot/   → واحد ۰۵: Shot Engine
│   ├── scene/  → واحد ۰۴: Scene Engine (صاحب اصلی inheritOrOverride)
│   ├── camera/ → واحد ۰۹: Camera & Motion
│   ├── sceneconditions/ → واحد ۰۸: Scene Conditions (Lighting + Environment)
│   ├── project/ → واحد ۱۶ فاز ۱: Project/ProjectSummary + archiveProject (اجرای واقعی State Machine واحد ۱۲)
│   ├── workflow/ → واحد ۱۶: User Workflow (WorkflowState/canJumpToStep/QualityScore — رفع یافته‌ی F1 ممیزی pre-Unit 16؛ AppTheme/HomeLayoutVariant/ComposerLayoutVariant برای فاز ۰ UI اضافه شدند)
│   ├── audio/  → واحد ۱۰: Audio Context Generator
│   ├── promptengine/ → واحد ۱۱: Prompt Engineering Core (قلب سیستم — تجمیع همه‌ی واحدها)
│   ├── stateversioning/ → واحد ۱۲: State & Versioning (State Machine + Lock + Versioning + Impact Analysis)
│   ├── outputdelivery/ → واحد ۱۴: Output Delivery System (Model Profile + Renderer + Composer + Bilingual)
│   ├── promptfinalization/ → واحد ۱۳: Prompt Finalization Pipeline (Cleaner + Token Calculator)
│   └── storage/ → واحد ۱۵ (منطق خالص): validateReferentialIntegrity + قوانین جدول
├── ui/      → واحد ۱۶ (User Workflow/UI) — **به‌طور کامل تکمیل شده، هر ۶ فاز**
│   ├── theme/     → Design Tokens واقعی (Color/ExtendedColors/Type/Theme) طبق docs/design/README.md
│   ├── navigation/ → Navigation دو‌لایه‌ی DDR-002 (MainScaffold/AppNavHost/BottomNavBar/StudioTopTabRow/BackNavigation/NavDrawer)
│   ├── workflow/  → WorkflowViewModel (language/theme/layout picks با DataStore Preferences، WorkflowState)
│   ├── project/   → واحد ۱۶ فاز ۱: ProjectListViewModel مشترک Home/Projects + ProjectCard/ProjectListSection
│   ├── home/      → واحد ۱۶ فاز ۱: HomeScreen (Header/Greeting/Quick-Create/Recent Projects)
│   ├── studio/    → واحد ۱۶ فاز ۱: StudioShell (Header/۴ Tab: داستان/DNA/صحنه‌ها/خروجی، همگی واقعی) + Timer دوره‌ای Auto-Save و Auto-Backup (فاز ۶)
│   ├── story/     → واحد ۱۶ فاز ۲ قدم ۱: StoryViewModel/StoryTabContent/StoryLabels (Tab «داستان» واقعی)
│   ├── storybreakdown/ → واحد ۱۶ فاز ۲ قدم ۲: AiStoryBreakdownScreen/ViewModel
│   ├── dna/       → واحد ۱۶ فاز ۲ قدم ۳: DnaViewModel/DnaTabContent/DnaLabels (Tab «DNA» واقعی)
│   ├── assets/    → واحد ۱۶ فاز ۳: AssetsScreen + فرم‌های ساخت Character/Location/Object (فقط «ساخت»، نه «ویرایش»)
│   ├── scenes/    → واحد ۱۶ فاز ۴ قدم ۱: ScenesListScreen/SceneDetailScreen (فقط «ساخت»، نه «ویرایش»)
│   ├── shots/     → واحد ۱۶ فاز ۴ قدم ۲: ShotComposerScreen (فیلدهای سطح‌بالا + هر ۴ Tab: دوربین/نور و محیط/صدا)
│   ├── validation/ → واحد ۱۶ فاز ۵ قدم ۱: ValidationScreen/ViewModel (تجمیع سه‌سطحی)
│   ├── outputdelivery/ → واحد ۱۶ فاز ۵ قدم ۳: OutputDeliveryScreen/ViewModel (انتخاب مدل، پیش‌نمایش، Copy/Export)
│   ├── settings/  → واحد ۱۶ فاز ۶ قدم ۱: SettingsScreen/ViewModel (۷ کارت طبق سند طراحی)
│   ├── backups/   → واحد ۱۶ فاز ۶ قدم ۲ (آخرین قدم): BackupsScreen/ViewModel/BackupLabels (فهرست/ساخت/بازیابی/حذف)
│   ├── i18n/      → UiStrings (fa/en با domain.outputdelivery.t() واقعی) + BidiUtils (زیرساخت RTL)
│   └── App.kt     → ریشه‌ی درخت Compose (تم + جهت RTL/LTR + MainScaffold؛ همه‌ی Repository/Manager های مشترک اینجا ساخته می‌شوند)
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

- ~~ارسال خودکار پرامپت به AI بیرونی (`sendToAiConnector`) هنوز `TODO()`
  است~~ — **کاملاً رفع شد** (G2، ADR-098/ADR-100/ADR-101): `sendToAiConnector`
  یک درخواست HTTP واقعی به Claude API می‌فرستد و اکنون به‌طور کامل به UI وصل
  است. کاربر در Settings کلید API را وارد می‌کند (فیلد ماسک‌شده،
  `SecureKeyRepository`)، در فاز ۱ صفحه‌ی AI Story Breakdown دو مسیر «کنار هم»
  دارد: مسیر ۱ (کپی/پیست دستی، موجود از قبل) و مسیر ۲ («ارسال خودکار به
  Claude» — فقط وقتی کلید ذخیره شده Enabled است، طبق شرط سخت‌گیرانه‌ی UI).
  G2 با این قدم به‌طور کامل بسته شده — دیگر آگاهانه‌موکول نیست.
- **مقصد Navigation بعد از تأیید فاز ۳ صفحه‌ی AI Story Breakdown همیشه Tab
  «داستان» Studio است، نه Tab «صحنه‌ها»**: چون هنوز هیچ صفحه‌ی فهرست Scene/Shot
  مستقلی ساخته نشده و `selectedTab` در `StudioShell` یک State محلی است، نه
  پارامتر ورودی. جزئیات در `docs/adr/046-unit16-phase2-step2-ai-story-breakdown.md`.
- ~~Tab «DNA»: فیلدهای `forbiddenElements`/`mandatoryElements`/
  `maxShotDurationSeconds` بدون UI + Rule 1 با `dependentShotsCount` ثابت روی
  صفر~~ — **رفع شد.** هر سه فیلد اکنون در گروه «محدودیت‌های خروجی» قابل
  ویرایش‌اند و هشدار واقعی Rule 1 اکنون از شمار واقعی شات‌های پروژه (بازاستفاده
  از Query موجود `ProjectDao.getProjectWithCounts`) تغذیه می‌شود. جزئیات در
  `docs/adr/054-unit16-dependent-shots-count-output-constraints-ui.md`.
- ~~فرم‌های ساخت Asset فقط «ساخت» دارند، نه «ویرایش» Asset موجود~~ — **رفع شد**
  (یافته‌ی 🟠 G7 ممیزی `docs/audit/post-unit16-full-audit.md`): لمس یک کارت
  Asset موجود اکنون فرم را با داده‌ی واقعی پیش‌پر می‌کند و ذخیره همان شناسه را
  به‌روزرسانی می‌کند (نه رکورد تازه) — هر سه نوع Asset. جزئیات در
  `docs/adr/061-post-unit16-audit-g1-g7-fixes.md`.
  ~~همچنان مدیریت کامل چند-Outfit کاراکتر (تصمیم F5) فقط یک دکمه‌ی
  Placeholder دارد~~ — **رفع شد** (G5، ADR-067 بخش ه): فرم Character اکنون
  یک لیست واقعی Outfit با شرط (آب‌وهوا/زمان روز/نوع مکان) دارد — افزودن،
  حذف، و تعیین پیش‌فرض. جزئیات در `docs/adr/095-g5-multi-outfit-management.md`.
  بعد از ذخیره‌ی یک Asset تازه، فیلتر بالای صفحه‌ی Assets خودکار روی نوع
  تازه‌ساخته‌شده نمی‌ماند (همیشه به Characters بازمی‌گردد) — کاربر باید دستی فیلتر
  مربوطه را لمس کند. جزئیات کامل در `docs/adr/049-unit16-phase3-step2-asset-forms.md`.
- **Scene Detail هنوز «ویرایش» ندارد، فقط «ساخت»** — Tab «دارایی‌ها» این صفحه
  فعلاً فقط پیام خالی نشان می‌دهد. بعد از بازگشت از Scene Detail، Tab «صحنه‌ها»
  خودکار انتخاب نمی‌ماند (کاربر باید دستی دوباره لمس کند — همان محدودیت
  پذیرفته‌شده‌ی فیلتر Assets، ADR-049). جزئیات کامل در
  `docs/adr/050-unit16-phase4-step1-scene-detail.md`.
  ~~حذف Scene («Delete») هنوز پیاده نشده~~ — **رفع شد** (یافته‌ی 🟡 G22
  ممیزی `docs/audit/post-unit16-full-audit.md`): منوی سه‌نقطه‌ی هدر Scene
  Detail اکنون یک مسیر Delete واقعی دارد (با Rule واقعی Blocking «صحنه‌ی
  دارای Shot قابل حذف نیست»)؛ Delete Shot/Delete Asset هم به همین ترتیب
  اضافه شدند. جزئیات در `docs/adr/062-post-unit16-audit-g6-g22-fixes.md`.
- **Shot Composer: فیلدهای سطح‌بالا و هر ۴ Tab (دوربین، نور و محیط، صدا) واقعی‌اند — فاز ۴ کامل شد.**
  فیلدهای سطح‌بالا (عنوان/توصیف/هدف/نوع نما/مدت/سطح حرکت، خارج از همه‌ی
  Tab‌ها و همیشه قابل‌مشاهده)، Tab «دوربین» (angle/distance/lensType/حرکت با
  فرم شرطی هر ۶ نوع/Advanced/Attached References/سوییچ منبع-Override)، Tab
  «نور و محیط» (دو سوییچ منبع-Override مستقل برای نور و محیط، style/
  weatherType + بخش Advanced شامل بقیه‌ی فیلدهای Nullable هر دو data class،
  انتخاب چندگانه‌ی environmentalMotion با سقف ۳موردی) و Tab «صدا» (سوییچ
  فعال/غیرفعال، دکمه‌ی صریح «تولید صداهای محیط» طبق Rule 5 — هرگز خودکار،
  فرم افزودن دستی actionSounds/characterSounds) همگی واقعی و
  Auto-Save‌شونده‌اند (هم برای شات جدید هم برای ویرایش شات موجود). Tab
  «اصلی» همچنان فقط پیام Placeholder نشان می‌دهد — چون طبق طراحی ADR-051
  همه‌ی فیلدهای سطح‌بالای شات از قبل خارج از هر Tab و همیشه‌قابل‌مشاهده‌اند،
  این Tab عملاً محتوای اختصاصی معناداری ندارد که پیاده‌سازی کند (بدهی
  ثبت‌شده، نه فراموش‌شده). مدیریت واقعی آپلود تصویر برای Attached
  References هم هنوز پیاده نشده (فقط نوع + توضیح متنی — هیچ Infra
  انتخاب‌گر تصویر در کل کدبیس وجود ندارد). اعتبارسنجی زنده (Validation) نیز
  فقط برای ۴ Rule خودبسنده‌ی Tab «دوربین» وایر شده؛ هیچ‌کدام از ۱۳ Rule
  «نور و محیط» زنده وایر نشده‌اند (نیاز به وابستگی تازه به `SceneRepository`
  برای `TimeOfDay`/`locationType` — تصمیم آگاهانه، جزئیات در ADR-053).
  جزئیات کامل در `docs/adr/051-unit16-phase4-step2-shot-list-composer-skeleton.md`،
  `docs/adr/052-unit16-phase4-step3-camera-tab.md` و
  `docs/adr/053-unit16-phase4-step4-lighting-environment-sound.md`.
- ~~Output Delivery: «Export» فقط Copy to Clipboard است، نه نوشتن فایل/Share
  Intent واقعی~~ — **رفع شد** (آخرین یافته‌ی معماری باز از سه موردی که با هم
  تصمیم گرفتیم — G4/G18، ADR-067): دکمه‌ی Export اکنون واقعاً `composeOutput`
  (واحد ۱۴) را می‌سازد، فایل‌ها را روی `cacheDir/exports` می‌نویسد، و با
  `Intent.ACTION_SEND[_MULTIPLE]` (اولین `FileProvider` کل پروژه) واقعاً
  اشتراک می‌گذارد — رفتاری کاملاً متفاوت از Copy. جزئیات کامل (و محدودیت
  ترجمه‌ی فارسی زیر) در `docs/adr/069-real-output-export-and-bilingual-limitation.md`.
- **Settings — سه سوییچ Display (Dynamic Font/Min Touch Target/Reduced
  Motion) و Layout Variants (Home A/B، Shot Composer A/B) واقعاً Persist
  می‌شوند اما هیچ زیرساخت Runtime ای برایشان وجود ندارد** — نه `HomeScreen`
  نه `ShotComposerScreen` فعلاً به مقدار Layout Variant شاخه‌بندی می‌کنند؛
  سه سوییچ Display هم به هیچ رفتار واقعی (فونت/اندازه‌ی لمس/انیمیشن) وصل
  نیستند. «Choose Image» (Home Screen Image) هم پیام «به‌زودی» می‌دهد —
  هیچ File Picker ای در کدبیس نیست (هم‌کلاس محدودیت Attached
  References/Export). جزئیات کامل در
  `docs/adr/058-unit16-phase6-step1-settings-autosave.md`.
- ~~Backups — Restore/Delete بدون دیالوگ تأیید~~ — **رفع شد** (یافته‌ی 🔴
  G19/G20 ممیزی `docs/audit/post-unit16-full-audit.md`): هر دو عملیات
  اکنون یک `AlertDialog` تأیید واقعی دارند (هم‌الگو با Delete پروژه‌ی
  موجود) پیش از اجرا. جزئیات در
  `docs/adr/060-post-unit16-audit-critical-fixes.md`.
- **Toast/Snackbar — مدت‌زمان دقیق «~۲ ثانیه»ی سند طراحی رفع نشد** —
  `SnackbarDuration` استاندارد Material3 فقط سه مقدار گسسته
  (Short/Long/Indefinite) دارد، نه میلی‌ثانیه‌ی دلخواه؛ Short نزدیک‌ترین
  گزینه‌ی موجود است (محدودیت API، نه کد این پروژه).
- **`backTargetsByRouteKey` (نقشه‌ی Back Navigation طراحی‌شده در فاز ۰)
  در عمل هرگز استفاده نشد** — همه‌ی قوانین Back Navigation که به آرگومان
  Runtime نیاز دارند (اکثر مسیرهای واقعی این اپ) مستقیماً در `when` صریح
  `MainScaffold.kt` پیاده شدند، نه در آن Map (که فقط برای مسیرهای
  بدون‌آرگومان طراحی شده بود). محدودیت شناخته‌شده‌ی معماری، نه باگ.
- **فرم Scene Detail فقط «ساخت» دارد، هیچ صفحه‌ای «ویرایش» ندارد** —
  خلاصه‌ی محدودیت بالا (ADR-050): لمس یک کارت Scene موجود به فرم پیش‌پرشده
  وصل نیست. (فرم‌های Asset این محدودیت را دیگر ندارند — رفع شد در G7،
  `docs/adr/061-post-unit16-audit-g1-g7-fixes.md`.) در کل واحد ۱۶، تنها راه
  تغییر یک Scene موجود (غیر از فیلدهای سطح‌بالای Shot که Auto-Save واقعی
  دارند) ساخت دوباره از صفر است.
- **آپلود/انتخاب تصویر واقعی هیچ‌جای اپ پیاده نشده** — نه برای Attached
  References شات، نه برای Home Screen Image تنظیمات؛ کل کدبیس فاقد
  زیرساخت File Picker/Media Picker است (یک محدودیت واحد، تکرارشده در سه
  جا: Shot Composer، Settings — Output Delivery دیگر این محدودیت را ندارد،
  Export واقعی فایل رفع شد، `docs/adr/069-...`).
- **ترجمه‌ی واقعی فارسی پرامپت هنوز وجود ندارد — یک تصمیم معماری در حال
  بحث، نه یک TODO ساده:** `Bilingual.kt` عمداً `generateBilingualPrompt`/
  `translateToFarsi` را پیاده نکرده (کامنت صریح خودِ فایل) — یک موتور
  ترجمه‌ی واقعی خودش یک تصمیم معماری جداست که هنوز بین معمار و کاربر
  پروژه در حال بررسی است (گزینه‌های مطرح: ML Kit Translation آفلاین، یا
  یک فیلد دوگانه‌ی ورودی کاربر — چون `shotDescription`/`sceneContext` متن
  آزاد کاربرند، نه enum). تا آن تصمیم، `OutputPackage.bilingualPrompts`
  (اکنون واقعاً ساخته و Export می‌شود، ADR-069) هر دو نسخه‌ی en/fa را با
  همان متن رندرشده‌ی نهایی پر می‌کند — یک Fallback صادقانه، نه ترجمه‌ی
  جعلی.
- **صفحه‌ی مستقل «مرور همه‌ی Override های پروژه» (Revoke Screen) ساخته
  نشد** — Revoke فقط Inline روی همان کارت هشدار صفحه‌ی Validation ممکن
  است (ADR-068). دلیل: `OverrideEntity` (واحد ۱۵) فیلد `projectId` ندارد
  (`entityId` عمداً Polymorphic طراحی شده)، پس یک Query کارآمد «همه‌ی
  Override های این پروژه» بدون یک ستون تازه یا Join گران ممکن نیست —
  نیازمند یک تصمیم Schema تازه، نه فقط UI.
