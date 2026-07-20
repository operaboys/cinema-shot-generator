# ADR-020: تصمیمات پیاده‌سازی واحد ۱۵ — قدم ۳، زیرقدم ۲ (AudioContext + collectData نهایی)

**تاریخ:** 2026-07-20
**وضعیت:** آخرین زیرقدم واحد ۱۵ — یک تصمیم Entity جدید (هم‌جنس تصمیمات تأییدشده‌ی قبلی) + چند تصمیم پیاده‌سازی محلی، بدون ابهام معماری جدید

## Context

آخرین زیرقدم واحد ۱۵: اتصال `AudioContext` (واحد ۱۰) به Room + نوشتن نهایی `PromptGenerationRepository.collectData` که تمام Repository های ساخته‌شده در این واحد (Settings/Versioning/Impact از قدم ۲؛ ProjectDna/Asset از زیرقدم ۱؛ Scene/AudioContext از همین زیرقدم) را در یک `PromptGenerationInput` واقعی (واحد ۱۱) تجمیع می‌کند.

## تصمیم: افزودن `AudioContextEntity` (هم‌جنس تصمیمات تأییدشده‌ی قبلی، نه پرسش جدید)

بلوپرینت ۱۵ برای `AudioContext` هم Entity ای فهرست نکرده بود (دقیقاً مثل `ProjectDna` در زیرقدم قبلی). طبق همان الگوی مستقر «هر Aggregate دامنه یک جدول مستقل با فیلد `xDataJson`»، `AudioContextEntity(audioContextId PK, shotId FK→Shot CASCADE, audioContextDataJson)` اضافه شد. این سومین بار است که این الگو به کار می‌رود (`EventLogEntity` در قدم ۲، `ProjectDnaEntity` در زیرقدم قبلی، حالا `AudioContextEntity`) — با توجه به این پیشینه‌ی تثبیت‌شده، این‌بار هم نیازی به پرسش جدید ندید.

## تصمیم: `SceneRepository` هم لازم بود (طبق پیش‌بینی صریح دستور کار)

بررسی با grep تأیید کرد `SceneEntity`/`SceneDao` (قدم ۱) کافی نبودند برای برگرداندن یک `Scene` دامنه‌ی کامل — `SceneEntity.sceneDataJson` یک رشته‌ی خام JSON است که تا این زیرقدم هیچ DTO/Mapper ای برای Decode آن به `Scene` واقعی وجود نداشت. `SceneDto.kt` + `SceneMappers.kt` + `SceneRepository.kt` دقیقاً هم‌الگو با `ADR-018`/`ADR-019` اضافه شدند.

**یافته‌ی مهم:** `Scene` دامنه (تأیید شده با grep در `SceneModels.kt`) هیچ فیلد `projectId` ندارد — این فیلد فقط در `SceneEntity` (لایه‌ی ذخیره‌سازی) وجود دارد. پس در `collectData`، `ProjectDna` از `sceneEntity.projectId` (نه از خودِ شیء دامنه‌ی `Scene`) بارگذاری می‌شود — دقیقاً طبق پیش‌بینی صریح دستور کار.

## `AudioContext` — بازاستفاده‌ی جزئی از DTO موجود، نه بازتعریف کامل

`AudioContextDto` از `ActionSoundDto`/`CharacterSoundDto` که در `ShotDto.kt` (قدم ۲، برای `Shot.soundProfile`) از قبل تعریف شده بودند بازاستفاده کرد — چون `domain.audio.AudioContext.actionSounds`/`characterSounds` مستقیماً از `domain.shot.ActionSound`/`CharacterSound` بازاستفاده می‌کنند (بدون بازتعریف، طبق ADR-011 واحد ۱۰). فقط یک DTO محلی جدید لازم بود: `AudioContextAmbientSoundDto` (۴ فیلد، با `source`) — چون `domain.audio.AmbientSound` یک فیلد اضافه نسبت به `AmbientSoundDto` موجود در `ShotDto.kt` (که برای نسخه‌ی ۳-فیلدی `domain.shot.AmbientSound` نوشته شده بود) دارد. این تمایز از پیش در ADR-011 مستند شده بود، نه ابهام جدید.

**تصمیم فایل‌بندی:** نگاشت‌های AudioContext در یک فایل جدا (`AudioContextMappers.kt`) نوشته شدند، نه در `DtoMappers.kt`/`DnaAssetMappers.kt` موجود — چون `DtoMappers.kt` از قبل `domain.shot.AmbientSound` را import کرده (برای `Shot.soundProfile`)، و این فایل به `domain.audio.AmbientSound` نیاز داشت؛ دو نام یکسان با پکیج متفاوت در یک فایل بدون Alias تداخل می‌کردند. فایل جدا از نیاز به Alias بی‌معنی جلوگیری کرد.

## `collectData`: ترتیب Resolve و علت هر تصمیم

دقیقاً طبق ترتیب صریح دستور کار پیاده شد: `Shot` (از `ShotDao`) → `Scene` (از `SceneDao`، با `shot.sceneId`) → `ProjectDna` (از `sceneEntity.projectId`) → `characters`/`objects`/`locations` (از `AssetRepository`، ترتیبی نه موازی — سادگی روی بهینه‌سازی زودهنگام ترجیح داده شد، چون این توابع I/O سبک روی SQLite محلی‌اند، نه شبکه) → `camera`/`lighting`/`environment` (از `SettingsResolutionRepository`، همان سه تابع آماده‌ی قدم ۲) → `audioContext` (nullable، `AudioContextRepository`).

**نکته‌ی فنی (نه باگ):** `Shot` مستقیماً در `PromptGenerationRepository` از `ShotDao` خوانده و Decode می‌شود (نه از طریق متد خصوصی مشابه در `SettingsResolutionRepository`)، چون `PromptGenerationInput.shot` به شیء کامل `Shot` نیاز دارد، نه فقط تنظیمات Resolve‌شده. این یعنی هنگام فراخوانی سه تابع `resolve*For` (که هرکدام دوباره از `ShotDao` می‌خوانند)، `ShotEntity` مجموعاً چهار بار خوانده/Decode می‌شود (یک‌بار مستقیم + سه‌بار داخل `SettingsResolutionRepository`). این یک هزینه‌ی خواندن تکراری بی‌خطر روی SQLite محلی است (نه I/O شبکه)، پذیرفته شد به‌نفع سادگی (بدون افزودن یک متد عمومی جدید فقط برای این استفاده‌ی یک‌باره).

اگر resolve* هرکدام شکست بخورند (مثلاً `source="scene"` بدون Override و بدون `sceneDefault`، طبق ADR-013)، `collectData` کل عملیات را Fail می‌کند — چون `camera`/`lighting`/`environment` فیلدهای غیر-nullable `PromptGenerationInput` هستند.

## تست: Round-Trip کامل + End-to-End — بدون باگ جدید

هر سه فایل تست (`SceneRepositoryTest`, `AudioContextRepositoryTest`, `PromptGenerationRepositoryTest`) در همان اجرای اول موفق شدند — سومین زیرقدم پیاپی بدون باگ جدید (بعد از زیرقدم ۱ که هم همین‌طور بود؛ فقط قدم ۱ و ۲ هرکدام یک باگ واقعی داشتند)، که ثبات الگوی DTO/Mapper تثبیت‌شده در ADR-018 را تأیید می‌کند.

## Consequences

- **آسان می‌شود:** واحد ۱۵ اکنون کاملاً تکمیل شده است. تمام توابع تزریق‌پذیر پروژه (`resolveCameraSettings`/`resolveLightingSettings`/`resolveEnvironmentSettings`، `collectData`، `createSnapshot`/`logEvent`، `findDependents`) به پیاده‌سازی واقعی Room وصل شده‌اند.
- **بدهی باقی‌مانده:** `AutoSaveManager`/`BackupManager`/Export-Import هنوز فقط امضا دارند (طبق ADR-017، خارج از Scope همیشه)؛ `Scene` هنوز فیلد پیش‌فرض camera/lighting/environment ندارد (تصمیم قدیمی ADR-013، نه این قدم)؛ خواندن تکراری `ShotEntity` در `collectData` یک بهینه‌سازی احتمالی آینده است، نه یک باگ.
