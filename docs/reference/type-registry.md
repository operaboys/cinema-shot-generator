# ثبت تایپ‌های پروژه (Type Registry)

| متادیتا | مقدار |
|---|---|
| **نقش** | Reference — منبع واحد حقیقت برای تمام تایپ‌های کد |
| **موقعیت** | `docs/reference/type-registry.md` |
| **وضعیت** | فعال — باید با هر تغییر enum/data class به‌روز شود |
| **تاریخچه** | نسخه‌ی اول توسط یک بازبینی معماری مستقل (خارج از این چت) تهیه شد؛ این نسخه بعد از رفع تمام موارد بحرانی/مهم آن بازبینی به‌روزرسانی شده است. |

---

## هدف این سند

این سند **فهرست کامل تمام تایپ‌های تعریف‌شده در بلوپرینت‌ها** است، به تفکیک واحد مالک. سه مشکل را حل می‌کند:

1. **تایپ‌های گم‌شده** — تایپ‌هایی که استفاده شده‌اند ولی هرگز تعریف نشده‌اند (👻)
2. **هم‌نامی‌های متفاوت** — تایپ‌هایی با نام یکسان ولی ساختار متفاوت در واحدهای مختلف (⚠️)
3. **ناسازگاری JSON ↔ Kotlin** — فیلدهایی که در JSON یک شکل‌اند و در Kotlin شکلی دیگر (🔀)

> **قانون الزامی از این پس:** هر `enum` یا `data class` جدید باید **اول** در این سند ثبت شود، **بعد** در بلوپرینت استفاده شود. این قانون مستقیماً از درس دو تناقض واقعی این پروژه («enum Mood هم‌نام» و «type های گمشده در واحد ۱۱») نتیجه شده و به یک فرآیند دائمی تبدیل می‌شود (طبق یک خط اضافه‌شده در `ai-coding-guidelines.md`).

---

## راهنمای علائم (Legend)

| علامت | معنا |
|---|---|
| ✅ | تعریف‌شده و سازگار |
| 👻 | استفاده‌شده ولی **تعریف‌نشده** در هیچ بلوپرینتی — بحرانی |
| ⚠️ | هم‌نام با تایپ دیگر ولی **متفاوت** — بحرانی |
| 🔀 | ناسازگاری بین نمونه‌ی JSON و data class Kotlin |
| ✅🔧 | بحرانی بود، **در این دور بازبینی رفع شد** |

---

## بخش ۱: تایپ‌های تعریف‌شده (به تفکیک واحد مالک)

### واحد ۰۱ — Story & Override (`domain/story/`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `StoryType` | enum | NARRATIVE, CONCEPTUAL, VISUAL_ONLY, EXPERIMENTAL | ✅ |
| `Genre` | enum | ACTION, DRAMA, SCI_FI, FANTASY, HORROR, ROMANCE, DOCUMENTARY, EXPERIMENTAL | ✅ |
| `NarrativeIntensity` | enum | MINIMAL, MODERATE, HIGH, EXTREME | ✅ |
| `VisualIntent` | enum | REALISTIC, CINEMATIC, STYLIZED, ARTISTIC, ABSTRACT | ✅ |
| `CompletionStatus` | enum | COMPLETE, PARTIAL | ✅ |
| `StoryContext` | data class | storyType, genre, moodPrimary: Mood, moodSecondary, narrativeIntensity, visualIntent, createdAt, completionStatus | ✅ |
| `OverrideType` | enum | ARTISTIC, NARRATIVE, VISUAL, TECHNICAL | ✅ |
| `RuleSeverity` | enum | BLOCKING, WARNING | ✅ |
| `OverrideScope` | data class | entityType, entityId, field, originalValue, overrideValue | ✅ |
| `HumanOverride` | data class | overrideId, overrideType, createdAt, active, scope, reason, usageCount, lastApplied, revoked, revokedAt, revokedReason | ✅ |
| `OverridePermission` | sealed class | Allowed, Denied | ✅ |

> ✅🔧 `Mood` محلی این واحد (نسخه‌ی ۳) **حذف شد** و به `domain.dna.Mood` (واحد ۰۲) ارجاع می‌دهد — این تناقض هم‌نامی بحرانی بود که در فاز قبلی این پروژه رفع شد.

---

### واحد ۰۱ب — AI Story Breakdown (`domain/storybreakdown/`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `StoryBreakdownRequest` | data class | storyContext, freeformStory, targetShotCount, defaultShotDurationSeconds | ✅ |
| `AiConnectorProfile` | data class | profileId, displayName, endpointUrl, requestBodyTemplate, requestHeaders, responseJsonPath | ✅ |
| `JsonErrorType` | enum | TRAILING_COMMA, SMART_QUOTES, UNMATCHED_BRACKET, INCOMPLETE_RESPONSE, UNKNOWN | ✅ |
| `JsonDiagnosis` | data class | errorType, approximateLine, simpleExplanation, autoFixable | ✅ |
| `SimpleCharacterFromAi` | data class | name, description, role, gender | ✅ |
| `SimpleLocationFromAi` | data class | name, description | ✅ |
| `SimpleObjectFromAi` | data class | name, description | ✅ |
| `SimpleShotFromAi` | data class | sceneName, shotNumber, description, characterNames, locationName, objectNames | ✅ |

---

### واحد ۰۲ — DNA Manager (`domain/dna/`) — مالک enum های سراسری

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `VisualStyleCategory` | enum | CINEMATIC, ANIMATION_3D, ANIMATION_2D, ARTISTIC, GENRE | ✅ |
| `VisualStyle` | enum | ۳۴ مقدار در ۵ دسته | ✅ |
| `MoodCategory` | enum | HIGH_ENERGY, POSITIVE, EMOTIONAL, DARK, CALM | ✅ |
| `Mood` | enum | ۲۵ مقدار در ۵ دسته — **تنها مالک این نام در کل پروژه** | ✅ |
| `LightingCategory` | enum | NATURAL, NIGHT, STUDIO, SPECIAL | ✅ |
| `LightingStyle` | enum | ۲۲ مقدار در ۴ دسته | ✅ |
| `RealismLevel` | enum | GROUNDED, SEMI_REALISTIC, FANTASTICAL | ✅ |
| `StyleConsistency` | enum | STRICT, MODERATE, FLEXIBLE | ✅ |
| `ColorTemperature` | enum | WARM, COOL, NEUTRAL | ✅ |
| `SaturationLevel` | enum | LOW, MEDIUM, HIGH, VERY_HIGH | ✅ |
| `ContrastLevel` | enum | LOW, MEDIUM, MEDIUM_HIGH, HIGH | ✅🔧 enum جدید — قبلاً `globalContrast` اشتباهاً از `SaturationLevel` استفاده می‌کرد |
| `AspectRatio` | enum | ۱۱ مقدار (LANDSCAPE_16_9 ... SQUARE_1_1) | ✅ |
| `CoreIdentity` | data class | dominantVisualStyle, realismLevel, styleConsistency, locked | ✅ |
| `MasterPalette` | data class | colorTemperature, globalSaturation, globalContrast: ContrastLevel, colorGradingPreset, colorPalette | ✅🔧 نوع `globalContrast` اصلاح شد |
| `OutputConstraints` | data class | forbiddenElements, mandatoryElements, maxShotDurationSeconds, aspectRatio | ✅ |
| `QualityDirectives` | data class | qualityTags, negativePrompt | ✅ |
| `GlobalMoodBase` | data class | primaryEmotion: Mood, intensity, consistency | ✅ |
| `LightingPreference` | data class | preferredStyle: LightingStyle? | ✅ |
| `ProjectDna` | data class | dnaId, projectId, coreIdentity, masterPalette, outputConstraints, globalMoodBase, lightingPreference, qualityDirectives | ✅ |
| `DnaUpdateResult` | data class | updatedDna, warning | ✅ |
| `ValidationResult` | sealed class | Valid, Warning, Blocking | ⚠️ هم‌نام/هم‌مفهوم با سیستم Validation واحد ۰۷ — دو سیستم عمداً جدا (این واحد یک خروجی محلی و ساده دارد، ۰۷ سیستم سراسری Blocking/Warning را تعریف می‌کند) |

---

### واحد ۰۳ — Visual Identity (`domain/visualidentity/`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `StyleInfluence` | enum | SUBTLE, MODERATE, STRONG | ✅ |
| `StyleReference` | data class | styleId, name, promptTokens | ✅ |
| `CompatibilityLevel` | enum | HIGH, MEDIUM, LOW, INCOMPATIBLE | ✅ |
| `CompatibilityResult` | data class | level, warning | ✅ |
| `CinematicMode` | enum | LONG_TAKE, FAST_CUT, BALANCED — **مالک واقعی**؛ واحد ۰۷ import می‌کند | ✅ |
| `CinematicLanguageSettings` | data class | globalMode, sceneOverrides | ✅ |

`getPacingFromEmotion(emotion: Mood): CinematicMode` — پارامتر از نوع `Mood` سراسری (import از `domain.dna`)، نه رشته‌ی خام.

---

### واحد ۰۴ — Scene Engine (`domain/scene/`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `NarrativeRole` | enum | INTRODUCTION, DEVELOPMENT, CLIMAX, RESOLUTION, TRANSITION | ✅ |
| `LocationType` | enum | INDOOR, OUTDOOR, MIXED, CUSTOM | ✅ |
| `TimeOfDay` | enum | DAWN, MORNING, NOON, AFTERNOON, SUNSET, NIGHT | ✅ |
| `Atmosphere` | enum | CALM, TENSE, DARK, BRIGHT, MYSTERIOUS, EMOTIONAL | ✅ |
| `SceneLocation` | data class | type, description | ✅ |
| `SceneConstraints` | data class | cameraRestrictions, lightingRestrictions, environmentRestrictions | ✅ |
| `Scene` | data class | sceneId, sceneTitle, sceneNumber, narrativeRole, location, timeOfDay, atmospherePrimary, atmosphereSecondary, constraints, shotCount | ✅ |

---

### واحد ۰۵ — Shot Engine (`domain/shot/`) — مالک `SourcedSettings`/`LightingSettings`/`EnvironmentSettings`

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `ShotGoal` | enum | ESTABLISHING, ACTION, EMOTIONAL, DIALOGUE, TRANSITION | ✅ |
| `ShotType` | enum | EXTREME_WIDE, WIDE, MEDIUM, CLOSE_UP, EXTREME_CLOSE_UP | ✅ |
| `MotionLevel` | enum | STATIC, SUBTLE, MODERATE, DYNAMIC, EXTREME — مالک واقعی؛ واحد ۰۷ import می‌کند | ✅ |
| `BeatEventType` | enum | CAMERA_MOVE, SUBJECT_ACTION, ENVIRONMENTAL, LIGHTING_CHANGE | ✅ |
| `Beat` | data class | timestampSeconds, eventType, description, subjectId | ✅ |
| `SettingsSource` | enum | SCENE, OVERRIDE | ✅🔧 جدید |
| `SourcedSettings<T>` | data class | source: SettingsSource, settings: T | ✅🔧 جدید — قبلاً فقط در متن نثر واحد ۱۱ ارجاع می‌شد، هیچ‌جا تعریف نشده بود |
| `LightingSettings` | data class | style: LightingStyle, keyLightPosition, fillLight, contrastRatio, shadowQuality, colorTemperature, motivation | ✅🔧 جدید — تایپ گمشده‌ی بحرانی، حالا تعریف کامل شد |
| `EnvironmentSettings` | data class | weatherType, weatherIntensity, locationType, groundState, visibility, temperatureFeel | ✅🔧 جدید — تایپ گمشده‌ی بحرانی، حالا تعریف کامل شد |
| `ImageReference` | data class | type, localFilePath, description | ✅ (تفاوتش با `ReferenceImage` واحد ۰۶ — فیلد `type` اضافه — عمدی و مستند است، نه تناقض) |
| `AmbientSound` | data class | type, intensity, description, source | ✅🔧 فیلد `source` اضافه شد — اکنون کاملاً هم‌ساختار با `AmbientSound` واحد ۱۰ |
| `ActionSound` | data class | timestampSeconds, type, description | ✅ (یکسان با واحد ۱۰) |
| `CharacterSound` | data class | characterId, type, description | ✅ (یکسان با واحد ۱۰) |
| `SoundProfile` | data class | enabled, ambientAutoGenerate, ambientSounds, actionSounds, characterSounds | ✅ |
| `Shot` | data class | shotId, sceneId, shotNumber, shotTitle, shotDescription, shotGoal, shotType, durationSeconds, motionLevel, beats, imageReferences, camera: SourcedSettings\<CameraSettings\>?, lighting: SourcedSettings\<LightingSettings\>?, environment: SourcedSettings\<EnvironmentSettings\>?, soundProfile, negativePromptOverride, characterIds, objectIds, locationIds | ✅🔧 سه فیلد `camera`/`lighting`/`environment` + `shotTitle` اضافه شدند — قبلاً حتی در خودِ data class تعریف نشده بودند |

توابع `resolveCameraSettings`/`resolveLightingSettings`/`resolveEnvironmentSettings` همگی `Result<T>` برمی‌گردانند (نه مقدار مستقیم) — ✅🔧 جدید.

---

### واحد ۰۶ — Asset & Continuity (`domain/asset/`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `Gender` | enum | FEMALE, MALE, OTHER | ✅ |
| `Hair` | data class | color, style, length | ✅🔧 جدید — هماهنگ با ساختار Object در JSON |
| `FacialFeatures` | data class | eyes, distinctiveMarks: List\<String\> | ✅🔧 جدید — هماهنگ با ساختار Object در JSON |
| `PhysicalAppearance` | data class | ageRange, gender, height, build, hair: Hair?, physicalFeatures, facialFeatures: FacialFeatures? + متد `toPromptString()` | ✅🔧 `hair`/`facialFeatures` از `String?` به data class تغییر کردند؛ `toPromptString()` اضافه شد |
| `CharacterTier` | enum | MAIN, SECONDARY, BACKGROUND | ✅ |
| `CharacterContinuityLevel` | enum | FULL, MEDIUM, NONE | ✅ |
| `LocationContinuityLevel` | enum | STYLE | ✅ |
| `PropContinuityLevel` | enum | FORM | ✅ |
| `ObjectSubtype` | enum | PERSONAL_PROP, GENERAL_PROP, COSTUME | ✅ |
| `Outfit` | data class | id, name, description, isDefault, condition | ✅ |
| `OutfitCondition` | data class | weather, timeOfDay, locationType | ✅ |
| `ContinuityRules` | data class | identityLock, appearanceLock, ageLock, antiDrift, allowedOverrides | ✅ |
| `CharacterAsset` | data class | assetId, characterTier, name, physicalAppearance, outfits, expressions, props, defaultMood, basePrompt, continuityRules, continuityLockLevel, referenceImages | ✅🔧 `continuityLockLevel` اضافه شد (پیش‌فرض `defaultLockLevelForTier(characterTier)`؛ جزئیات در ADR-029) |
| `ObjectAsset` | data class | assetId, name, description, subtype, size, materialAndColor, specialTrait, basePrompt, continuityLockLevel | ✅ |
| `LocationAsset` | data class | assetId, name, description, environment, timeCompatibility, weatherCompatibility, keyElements, basePrompt, continuityLockLevel | ✅ |
| `ReferenceImage` | data class | localFilePath, description | ✅ (بدون `type` — تفاوت عمدی با `ImageReference` واحد ۰۵) |
| `UpdateResult` | sealed class | Allowed, Blocked, Warned | ✅ |
| `Expression` | data class | id, name, description, emotion | ✅ (تعریف کامل موجود در بلوپرینت) |
| `Prop` | data class | id, name, description, category | ✅ (تعریف کامل موجود در بلوپرینت) |

---

### واحد ۰۷ — Validation & Consistency (`domain/validation/`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `Severity` | enum | BLOCKING, WARNING — **مالک سراسری**، مصرف‌شده در تمام واحدها | ✅ |
| `ValidationIssue` | data class | severity, field, message, suggestion | ✅ |
| `ValidationReport` | data class | targetId, blockingErrors, warnings | ✅ |
| `DependencyType` | enum | STRONG, WEAK, REFERENCE | ✅ |
| `DependencyEdge` | data class | sourceId, targetId, type | ✅ |
| `ImpactReport` | data class | directlyAffected, transitivelyAffected, toInvalidate, toWarn | ✅ |

`validateLogicConsistency(shot, environment: EnvironmentSettings, lighting: LightingSettings)` — پارامترها اکنون از `domain.shot` import می‌شوند؛ ✅🔧 فیلد `environment.weatherType` اصلاح شد (قبلاً اشتباهاً `environment.weather`).

`checkFastMotionLongTake(motionLevel: MotionLevel, cinematicMode: CinematicMode)` — وابستگی چرخه‌ای `domain.validation ↔ domain.shot/domain.visualidentity` — ثبت‌شده در R4 (`risk-register.md`)، بدهی فنی پذیرفته‌شده.

---

### واحد ۰۸ — Scene Conditions (`domain/sceneconditions/`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `LightingPreset` | data class | style: LightingStyle, keyLightPosition, fillLight, contrastRatio, shadowQuality, colorTemperature | ✅ (پیش‌فرض پیشنهادی — متفاوت از `LightingSettings` که مقدار نهایی Resolve‌شده است) |
| `AmbientSoundSuggestion` | data class | type, intensity, description | ✅ (عمداً مستقل از `AmbientSound` — نقش متفاوت: پیشنهاد اولیه‌ی Pipeline، نه نوع ذخیره‌شده‌ی نهایی) |

`mapMoodToLighting(mood: Mood): LightingPreset` — Exhaustive روی `MoodCategory`، خروجی غیر-nullable.

---

### واحد ۰۹ — Camera & Motion (`domain/camera/`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `CameraAngle`, `CameraDistance`, `LensType` | enum | (فهرست کامل در بلوپرینت) | ✅ |
| `CameraMovement` | sealed class | Basic/Advanced variants | ✅ |
| `CameraSettings` | data class | angle, distance, lensType, movement, depthOfField | ✅ — مصرف‌شده در `SourcedSettings<CameraSettings>` (واحد ۰۵) |

---

### واحد ۱۰ — Audio Context (`domain/audio/`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `AmbientSound` | data class | type, intensity, description, source | ✅ (هم‌ساختار با `AmbientSound` واحد ۰۵ پس از رفع هم‌نامی) |
| `ActionSound`, `CharacterSound` | data class | (یکسان با واحد ۰۵، بازاستفاده‌شده) | ✅ |
| `AudioContext` | data class | audioContextId, shotId, ambientSounds, actionSounds, characterSounds | ✅ |

---

### واحد ۱۱ — Prompt Engineering Core (`domain/promptengine/`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `PriorityLevel` | enum | HUMAN_OVERRIDE, SHOT_SPECIFIC, CHARACTER_CONTINUITY, SCENE_CONTEXT, PROJECT_DNA | ✅ |
| `PromptGenerationInput` | data class | dna, scene, shot, characters, objects: List\<ObjectAsset\>, locations: List\<LocationAsset\>, camera: CameraSettings, lighting: LightingSettings, environment: EnvironmentSettings, audioContext | ✅🔧 `lighting`/`environment` اکنون import واقعی از `domain.shot` (نه type محلی/گمشده) |
| `StructuredParts` | data class | subjectDescription, sceneContext, shotDescription, cameraSpecs, lightingSpecs, environmentSpecs, styleModifiers, timelineBeats, audioDescription | ✅ |
| `PromptBlueprint` | data class | promptBlueprintId, shotId, structuredParts, imageReferences, weightedEmphasis, seed, conflictsResolved, warnings | ✅ |

`enforceCharacterContinuity` اکنون از `physicalAppearance.toPromptString()` استفاده می‌کند — ✅🔧 قبلاً `toString()` پیش‌فرض بود.

---

### واحد ۱۲ — State & Versioning (`domain/stateversioning/`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `EntityState` | enum | DRAFT, REVIEW, LOCKED, FINAL, ARCHIVED | ✅ |
| `Entity` | interface | id, type, state, lock, `withLock(newLock)` | ✅🔧 جدید — تایپ گمشده‌ی بحرانی؛ متد `withLock` جایگزین `copy()` (که روی interface ممکن نیست) |
| `EntityLock` | data class | locked, lockedAt, lockReason | ✅ |
| `VersionType` | enum | SAFE, RISKY | ✅ |
| `EntityVersion` | data class | (فهرست کامل در بلوپرینت) | ✅ |
| `ImpactResult` | data class | riskLevel و مشابه | ✅ |

---

### واحد ۱۳ — Prompt Finalization (`domain/promptfinalization/`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `CleaningReport` | data class | conflictsDetected, conflictsResolved, redundancyRemoved, stopWordsRemoved, originalLength, cleanedLength, compressionRatio | ✅ |
| `CleaningOptions` | data class | detectConflicts, removeRedundancy, filterStopWords, optimizeTokens, aggressiveMode | ✅ |
| `TokenCheckResult` | data class | estimatedTokens, maxTokens, withinLimit, warning | ✅ |

⚠️ رابطه‌ی این واحد با ۱۴ در سطح توصیف متنی «وابستگی متقابل» به‌نظر می‌رسید — ثبت‌شده در `risk-register.md` (R5)؛ در عمل ۱۳ یک مرحله‌ی داخلی جریان اجرای ۱۴ است، نه واحد کاملاً مستقل.

---

### واحد ۱۴ — Output Delivery (`domain/outputdelivery/`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `ModelCapabilities` | data class | supportsVideo, supportsImage, supportsWeightedTags, supportsImagePrompt, supportsNegativePrompt | ✅ |
| `ModelConstraints` | data class | maxPromptLength, maxTokens | ✅ |
| `ModelFormat` | data class | type, structure, commandPrefix | ✅ |
| `ModelProfile` | data class | profileId, platform, capabilities, constraints, format, optimizationRules | ✅ |
| `RenderedOutput` | data class | modelProfileId, formattedPrompt, language | ✅ |
| `OutputPackage` | data class | outputId, shotId, promptBlueprintId, bilingualPrompts, renderedOutputs, exportFiles | ✅ |
| `BilingualPrompts` | data class | enVersion, faVersion | ✅ |
| `ExportFile` | data class | filename, content, mimeType | ✅ |
| `Language` | enum | FA, EN | ✅ |
| `LanguagePreferences` | data class | uiLanguage, promptLanguage, fallbackLanguage | ✅ |

---

### واحد ۱۵ — Project Storage (`data/`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `ProjectEntity`, `SceneEntity`, `ShotEntity`, `AssetEntity`, `PromptBlueprintEntity`, `RenderedOutputEntity`, `OverrideEntity`, `VersionEntity` | data class (Room Entity) | (فهرست کامل در بلوپرینت) | ✅ |
| `IntegrityIssue` | data class | source, brokenReferenceTo, message | ✅ |
| `ProjectData` | data class | projectId, scenes: List\<Scene\>, assetIds: Set\<String\>, shots: List\<Shot\> | ✅🔧 جدید — تایپ گمشده‌ی بحرانی؛ `assetIds` به‌عمد `Set<String>` است نه لیست Asset کامل (سه نوع Asset فاقد کلاس پایه‌ی مشترک‌اند) |

---

### واحد ۱۶ — User Workflow (`domain/workflow/`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `WorkflowStep` | enum | STORY_WIZARD, AI_STORY_BREAKDOWN, DNA_CONFIG, ASSET_LIBRARY, SCENE_CREATION, SHOT_CREATION, VALIDATION, PROMPT_GENERATION, OUTPUT_DELIVERY | ✅ |
| `StepStatus` | enum | NOT_STARTED, IN_PROGRESS, COMPLETED | ✅ |
| `WorkflowState` | data class | sessionId, projectId, currentStep, stepStatus, startedAt, lastActionAt, shotListViewMode | ✅ |
| `ShotListViewMode` | enum | GRID, TIMELINE | ✅ |
| `FeedbackType` | enum | CONFIRMATION_REQUIRED, TRANSIENT_SUCCESS | ✅ |
| `QualityScore` | data class | subjectClarity, cinematicClarity, visualSpecificity, styleCoherence, conciseness | ✅ |

---

### Governance — Error Hierarchy (`ai-coding-guidelines.md`)

| تایپ | نوع | مقادیر / فیلدها | وضعیت |
|---|---|---|---|
| `CinemaShotError` | sealed class | userMessage, technicalDetails, suggestion | ✅ |
| `ValidationFailure` | data class | field, reason | ✅ |
| `LogicConflictDetected` | data class | conflictDescription, involvedFields | ✅ |
| `ContinuityLockViolation` | data class | characterId, lockedField | ✅ |
| `BrokenReference` | data class | sourceId, missingTargetId | ✅ |

---

## بخش ۲: هم‌نامی‌های باقی‌مانده (بررسی‌شده و آگاهانه پذیرفته‌شده، نه تناقض)

| مورد | تایپ‌ها | توضیح |
|---|---|---|
| هم‌مفهوم متفاوت (عمدی) | `SoundProfile` (۰۵) در برابر `AudioContext` (۱۰) | هر دو ambient/action/character sounds دارند؛ `Shot.soundProfile` سطح Shot است، `AudioContext` واحد تولید مستقل — رابطه‌شان در Pipeline از طریق `PromptGenerationInput.audioContext` برقرار می‌شود، نه یکسان‌سازی مستقیم دو نوع. |
| هم‌مفهوم متفاوت (عمدی) | `ImageReference` (۰۵: +`type`) در برابر `ReferenceImage` (۰۶: بدون `type`) | Asset از `ReferenceImage` (فقط فایل)، Shot از `ImageReference` (فایل + نوع کاربرد) استفاده می‌کند — تفاوت به این دلیل عمدی است که Shot باید بداند رفرنس برای «کاراکتر» است یا «سبک» یا «نور»، در حالی که خودِ Asset این تمایز را لازم ندارد. |
| دو سیستم Validation (عمدی) | `ValidationResult` (۰۲: sealed محلی) در برابر `ValidationIssue`/`ValidationReport` (۰۷: سراسری) | واحد ۰۲ برای یک تصمیم محلی و ساده (`updateCoreIdentity`) یک خروجی سبک‌تر دارد؛ ادغام کامل با سیستم سراسری ۰۷ برای این مورد خاص لازم تشخیص داده نشد. |

## بخش ۳: آمار کلی (بعد از این دور اصلاح)

| دسته | تعداد |
|---|---|
| enum تعریف‌شده | ~۵۲ (۲ مورد جدید: `ContrastLevel`, `SettingsSource`) |
| data class تعریف‌شده | ~۶۶ (۶ مورد جدید: `SourcedSettings`, `LightingSettings`, `EnvironmentSettings`, `Hair`, `FacialFeatures`, `ProjectData`) |
| sealed/interface جدید | ۱ (`Entity`) |
| تایپ گم‌شده (👻) باقی‌مانده | **۰** |
| هم‌نامی متفاوت (⚠️) واقعی باقی‌مانده | **۱** (`ValidationResult`، عمداً مستقل نگه داشته شده) |
| ناسازگاری JSON/Kotlin (🔀) باقی‌مانده | **۰** |

---

*این سند باید زنده بماند — هر تایپ جدید اول اینجا ثبت شود، بعد در بلوپرینت استفاده شود.*
