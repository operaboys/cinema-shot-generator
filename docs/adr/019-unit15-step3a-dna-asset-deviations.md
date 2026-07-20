# ADR-019: تصمیمات پیاده‌سازی واحد ۱۵ — قدم ۳، زیرقدم ۱ (اتصال ProjectDna و Asset)

**تاریخ:** 2026-07-20
**وضعیت:** یک تصمیم Entity جدید (هم‌جنس تصمیمات تأییدشده‌ی قبلی) + چند تصمیم پیاده‌سازی محلی، بدون ابهام معماری جدید

## Context

اولین زیرقدم از دو زیرقدم قدم سوم واحد ۱۵: اتصال `ProjectDna` (واحد ۰۲) و `CharacterAsset`/`LocationAsset` (واحد ۰۶) به Room، طبق همان الگوی DTO محلی که در ADR-018 (قدم ۲) جواب داد. `AudioContext` و اتصال نهایی `collectData` به زیرقدم دوم موکول شدند.

## پیش از کدنویسی: تأیید `outfitOverride` (طبق درخواست صریح)

با grep مستقیم روی `domain/asset/AssetModels.kt` تأیید شد: `CharacterAsset` هیچ فیلد `outfitOverride` ندارد — دقیقاً هم‌راستا با یافته‌ی ADR-012 (واحد ۱۱). پس `CharacterAssetDto` هم چنین فیلدی ندارد؛ اگر در آینده `enforceCharacterContinuity`/`selectOutfitForScene` به این Repository ها وصل شوند، `outfitOverride`/`manualOverrideId` باید پارامتر جداگانه‌ی تابع باشد، نه فیلدی در Entity/DTO — طبق همان الگوی تأییدشده‌ی ADR-012. این قدم به آن اتصال دست نزد (خارج از Scope).

همچنین تأیید شد `EvolutionTimeline`/`EvolutionEntry` (در `domain/asset/AssetModels.kt` تعریف شده‌اند) اصلاً فیلدی از `CharacterAsset` نیستند (یک نوع مستقل با `characterId` خودش)، پس چیزی برای سریالایز کردن متصل به `CharacterAsset` در این زیرقدم لازم نبود.

## تصمیم: افزودن `ProjectDnaEntity` (هم‌جنس تصمیمات تأییدشده‌ی قبلی، نه پرسش جدید)

بلوپرینت ۱۵ اصلاً هیچ Entity ای برای `ProjectDna` فهرست نکرده بود (فقط Project/Scene/Shot/Asset/PromptBlueprint/RenderedOutput/Override/Version/DependencyEdge). طبق الگوی مستقر «هر Aggregate دامنه یک جدول مستقل با فیلد `xDataJson`» (که در `SceneEntity`/`ShotEntity`/`AssetEntity`/`PromptBlueprintEntity` از قدم ۱ به کار رفته)، یک `ProjectDnaEntity(dnaId PK, projectId FK→Project CASCADE, dnaDataJson)` مستقل اضافه شد — نه فشرده‌کردن `dnaDataJson` داخل `ProjectEntity` موجود (که فیلدهای ساده‌ی متادیتا دارد، نه JSON بزرگ تو در تو).

**چرا این‌بار پرسیده نشد:** این دقیقاً هم‌جنس دو تصمیم قبلی است که معمار صریحاً تأیید کرده بود («افزودن `EventLogEntity`» در قدم ۲، «افزودن سه فیلد به `VersionEntity`» هم در قدم ۲) — یعنی «افزودن/گسترش Entity وقتی Schema هنوز `version=1` و منتشرنشده کافی است» از قبل به‌عنوان یک الگوی مجاز تثبیت شده بود، نه یک انشعاب معماری جدید. `AppDatabase` به‌روزرسانی شد (`ProjectDnaEntity::class` + `abstract fun projectDnaDao()`)؛ `version` همچنان `1` ماند.

## تصمیم: دو تابع ذخیره‌ی جدا در `AssetRepository` (نه یک `saveAsset` مشترک)

هیچ نوع عمومی `Asset` در دامنه وجود ندارد (تأیید شده با grep، هم‌راستا با یافته‌ی قبلی ADR-003 واحد ۰۶) — `CharacterAsset` و `LocationAsset` دو Kotlin type کاملاً مستقل‌اند، پس یک امضای `saveAsset(asset: Asset)` عمومی از ابتدا امکان‌پذیر نبود. `saveCharacterAsset(projectId, asset: CharacterAsset)` و `saveLocationAsset(projectId, asset: LocationAsset)` جدا نوشته شدند؛ هر دو از `assetType` واقعی (`AssetType.CHARACTER`/`AssetType.LOCATION`/`AssetType.OBJECT`) برای پرکردن ستون Discriminator موجودِ `AssetEntity.assetType` استفاده می‌کنند (بدون تغییر در خودِ `AssetEntity`، چون همان فیلد قدم ۱ کافی بود).

`loadCharacterAssets`/`loadAssets` هر دو با بررسی `entity.assetType` تصمیم می‌گیرند کدام DTO را Decode کنند؛ رکورد با نوع نامنطبق یا ناموجود بی‌صدا نادیده گرفته می‌شود (`mapNotNull`) — سازگار با این‌که این توابع «لیستی از ID» می‌گیرند و ID‌های نامعتبر باید فقط از نتیجه حذف شوند، نه باعث شکست کل عملیات شوند.

## تصمیم: DTO محلی در `data/repository/` (تکرار الگوی ADR-018)

`ProjectDnaDto.kt` و `AssetDto.kt` دقیقاً هم‌الگو با `ShotDto.kt`/`CameraSettingsDto.kt` (ADR-018): enum های دامنه به‌صورت `String` (نام Enum) ذخیره می‌شوند، بدون DTO enum جداگانه. `DnaAssetMappers.kt` تنها فایلی است که هم `data.repository` و هم `domain.dna`/`domain.asset` را import می‌کند — همان جهت مجاز `data.repository → domain.*`.

`Map<String, List<String>>` (فیلد `OutputConstraints.forbiddenElements`) مستقیماً توسط kotlinx.serialization پشتیبانی می‌شود (کلید `String`) — نیازی به تبدیل خاصی نبود.

## تست: Round-Trip کامل، نه نمونه‌ی مینیمال

طبق دستور صریح، هر دو تست (`ProjectDnaRepositoryTest`, `AssetRepositoryTest`) از یک نمونه‌ی **کاملاً پرشده** استفاده می‌کنند (همه‌ی زیرساختارهای اختیاری هم پر شده‌اند: `Expression` با `condition`، `Outfit` با `condition`، `distinctiveMarks` غیرخالی، `forbiddenElements` با چند کلید) و `assertEquals` روی کل شیء دامنه (نه فقط چند فیلد) — هر دو تست بدون هیچ خطایی در اولین اجرا موفق شدند (تفاوت با قدم‌های ۲/۱ که هرکدام یک باگ واقعی را در اولین اجرا آشکار کردند)، که نشان می‌دهد الگوی DTO/Mapper از ADR-018 به‌درستی تکرار شد.

## خارج از Scope (طبق دستور کار، بدون تغییر)

`AudioContext` (واحد ۱۰)، اتصال نهایی `collectData` به `PromptGenerationInput` کامل — زیرقدم دوم.

## Consequences

- **آسان می‌شود:** `ProjectDna` و `CharacterAsset`/`LocationAsset` اکنون کاملاً به Room وصل‌اند؛ زیرقدم دوم فقط `AudioContext` را کم دارد تا `collectData` واقعی نوشته شود.
- **بدهی باقی‌مانده:** `AudioContext` هنوز سریالایز نشده؛ `collectData` نهایی هنوز نوشته نشده؛ اتصال `outfitOverride`/`manualOverrideId` (اگر در آینده لازم شود) هنوز طراحی نشده.
