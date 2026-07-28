# ADR-029: Migration واحد ۰۶ (Asset & Continuity) — Tier/Lock Levels

**تاریخ:** 2026-07-28
**وضعیت:** هر دو بخش کامل شدند. پروژه کامل کامپایل می‌شود و تست می‌گذراند (420 تست، ۰
Failure، ۰ Error — `gradle :app:assembleDebug :app:testDebugUnitTest`، `BUILD SUCCESSFUL`).

## Context

`docs/blueprints/06-asset-and-continuity-v2.md` (نسخه ۵) نسبت به نسخه‌ی پیاده‌سازی‌شده‌ی
قبلی تغییرات بزرگی معرفی می‌کند: `CharacterTier`، سه enum مجزای سطح تداوم
(`CharacterContinuityLevel`/`LocationContinuityLevel`/`PropContinuityLevel`)، تفکیک کامل
`ObjectAsset` از `LocationAsset`، و تبدیل Hard Lock واحد ۰۶ از مطلق (فقط Blocked/Allowed) به
شرطی به Tier (با حالت جدید `Warned`). دستور کار این قدم صریحاً Scope را به دو فایل محدود کرد:
`domain/asset/AssetModels.kt` و `domain/asset/AssetContinuity.kt`.

## تصمیم ۱: Option A — تفکیک کامل `ObjectAsset` از `LocationAsset`

قبل از این Migration، `LocationAsset` هر دو `AssetType.LOCATION` و `AssetType.OBJECT` را با
یک فیلد تفکیک‌کننده‌ی `assetType: AssetType` پوشش می‌داد (طبق ADR-003 قدیمی). بلوپرینت
دو گزینه مطرح کرده بود: **Option A** (تفکیک کامل، پیشنهاد خودِ بلوپرینت) یا **Option B**
(نگه‌داشتن ساختار مشترک با فیلدهای Object به‌صورت nullable)، با این شرط که اگر grep هزینه‌ی
غیرمعقول در لایه‌های دیگر (به‌خصوص واحد ۱۵/Room) نشان دهد، Option B با توجیه صریح انتخاب شود.

**تصمیم: Option A.** دلایل:

1. توصیه‌ی صریح خودِ بلوپرینت با استدلال مشخص (فیلدهای Object و Location هیچ همپوشانی
   معنایی ندارند: `size`/`materialAndColor`/`specialTrait` در برابر
   `environment`/`timeCompatibility`/`weatherCompatibility`).
2. `docs/reference/type-registry.md` از قبل `ObjectAsset` را به‌عنوان یک نوع کاملاً مستقل
   ثبت کرده (وضعیت ✅) — این تأیید مستقل قوی‌ای است که Option A همان حالت هدف مورد انتظار
   پروژه است، نه صرفاً حدس امروز من.
3. بررسی Blast Radius با grep (`AssetType\.OBJECT`, `\bLocationAsset\(`,
   `UpdateResult`) نشان داد هزینه واقعی است اما محدود، نه «غیرمعقول بزرگ»:
   - `data/repository/AssetRepository.kt` (واحد ۱۵): `saveLocationAsset`/`loadAssets` فعلاً
     هر دو AssetType را با `LocationAsset`/`LocationAssetDto` مشترک مدیریت می‌کنند
     (کامنت موجود: «طبق ADR-003 واحد ۰۶»). این فایل نیاز به توابع/DTO جدید برای
     `ObjectAsset` خواهد داشت — **اما** جدول `AssetEntity` خودش (`assetId`/`projectId`/
     `assetType`/`assetDataJson`) یک جدول عمومی Blob است؛ هیچ Migration اسکیمای Room
     لازم نیست، فقط کد Kotlin جدید در لایه‌ی Mapper/Repository.
   - `domain/promptengine/PromptEngineModels.kt` (واحد ۱۱): فیلد
     `PromptGenerationInput.objects: List<LocationAsset>` به `List<ObjectAsset>` نیاز به
     تغییر نوع دارد.
   - ۲-۳ فایل تست (`PromptGenerationRepositoryTest.kt`, `PromptAssemblyTest.kt`) که از
     `AssetType.OBJECT` با `LocationAsset` استفاده می‌کنند.

   این فهرست، صریحاً بزرگ‌تر از چیزی است که عبارت کاربر («به‌خصوص واحد ۱۵/Room») تلویحاً
   اشاره داشت (شامل واحد ۱۱ هم می‌شود)، اما همچنان «هزینه‌ی غیرمعقول» نیست — بدون
   Migration اسکیمای Room، و محدود به چند فایل شناخته‌شده. این فهرست دقیق برای بخش دوم/
   قدم‌های بعدی یادداشت شد.

**نتیجه:** `LocationAsset` اکنون فقط مکان است (فیلد `assetType` حذف شد)؛ `ObjectAsset`
ساختار کاملاً مستقل جدید است (`ObjectSubtype`، `size`، `materialAndColor`، `specialTrait`،
`basePrompt`، `continuityLockLevel: PropContinuityLevel`).

## تصمیم ۲: `characterTier` — الزامی (بدون پیش‌فرض) در برابر پیش‌فرض `MAIN`

کد مفهومی بلوپرینت (`val characterTier: CharacterTier,`) خودش پیش‌فرض ندارد. اما بخش
یادداشت پیاده‌سازی بلوپرینت («character_tier باید Backward Compatible باشد ... پیشنهاد:
MAIN، تا رفتار فعلی/محافظه‌کارانه حفظ شود») یک پیش‌فرض احتمالی مطرح می‌کند و تصمیم نهایی را
صریحاً به من واگذار می‌کند.

**تصمیم: الزامی، بدون پیش‌فرض** (مطابق شکل تحت‌اللفظی کد مفهومی). دلایل:

- با grep در کل پروژه (`\bCharacterAsset\(`، word boundary) هر ۶ محل واقعی ساخت
  `CharacterAsset` پیدا و بررسی شد: `data/repository/PromptGenerationRepositoryTest.kt`,
  `data/repository/AssetRepositoryTest.kt`, `domain/shot/ShotOutfitSelectionTest.kt`,
  `domain/promptengine/CharacterContinuityTest.kt`, `domain/promptengine/PromptAssemblyTest.kt`
  (تست‌ها) و `data/repository/DnaAssetMappers.kt` (`CharacterAssetDto.toDomain()`، main) —
  **همه فقط از Named Argument استفاده می‌کنند**. پس الزامی‌کردن این فیلد هیچ خطر Silent
  Breakage ندارد؛ فقط خطای کامپایل بلند و صریح برای هر فراخوانی، که هرحال این دو فایل در
  این قدم کامپایل نخواهند شد.
- توجیه «Backward Compatible» بلوپرینت بیشتر ناظر به داده‌ی سریالایز‌شده‌ی موجود روی دیسک
  (JSON قدیمی بدون این فیلد) است، نه سازنده‌ی نوع دامنه‌ی Kotlin — آن نگرانی متعلق به لایه‌ی
  Mapper/DTO (`DnaAssetMappers.kt`) است، جایی که هنگام migrate کردن یک رکورد قدیمی می‌توان
  آگاهانه `CharacterTier.MAIN` را صریحاً انتخاب کرد؛ این تصمیم به بخش دوم (لایه‌ی
  مصرف‌کننده) موکول شد، نه به شکل نوع دامنه در این فایل.

## تصمیم ۳: عبارت پیش‌فرض `continuityLockLevel` روی `CharacterAsset`

`= defaultLockLevelForTier(characterTier)` به‌عنوان عبارت پیش‌فرض مستقیم سازنده انتخاب شد
(نه nullable + محاسبه‌ی تنبل)، چون Kotlin اجازه می‌دهد پارامترهای پیش‌فرض بعدی به پارامترهای
قبلی سازنده ارجاع دهند، و این با الگوی ساده‌ی موجود پروژه برای عبارات پیش‌فرض (مثل
`LightingPreference`/`QualityDirectives` از Migration واحد ۰۲) هم‌راستاست.

**یافته‌ی شفاف (عمدی، نه غفلت):** نه کد مفهومی بلوپرینت و نه `type-registry.md` فیلد
`continuityLockLevel` را روی `CharacterAsset` فهرست نکرده‌اند (فقط `LocationAsset`/
`ObjectAsset` این فیلد را دارند، چون هرکدام یک مقدار ثابت دارند؛ طراحی واقعی بلوپرینت این
است که `validateCharacterUpdate` مقدار `level` را از بیرون به‌عنوان پارامتر می‌گیرد، یعنی
فراخواننده مسئول نگه‌داشتن/محاسبه‌ی آن است). با این حال، افزودن این فیلد دستور صریح و
جزئی‌نگر دستور کار این قدم بود (نه تفسیر یا حدس من)؛ تصمیم گرفتم متوقف نشوم و AskUserQuestion
نزنم چون: (الف) دستور بسیار مشخص و آگاهانه بود، نه ارجاع کلی مبهم؛ (ب) تغییر کاملاً افزایشی/
Backward-Compatible است (فیلد جدید با پیش‌فرض محاسبه‌شده، نه حذف/تغییر معنایی چیزی موجود) —
برخلاف تصمیم‌های مخرب‌تر Migration واحد ۰۲ که واقعاً نیاز به توقف داشتند؛ (پ) این فیلد مستقیماً
همان قصد طراحی بلوپرینت (پیش‌فرض قابل Override توسط کاربر) را برآورده می‌کند، فقط بلوپرینت آن
را در سطح مکانیکی کد ننوشته بود. **این انحراف باید در `type-registry.md` هم منعکس شود** —
پیشنهاد می‌شود در قدم بعدی (یا یک قدم مستند‌سازی مجزا) سطر `CharacterAsset` آن سند به‌روزرسانی
شود تا `continuityLockLevel` را هم فهرست کند.

## تصمیم ۴: `PhysicalAppearance`/`Gender` عمداً دست‌نخورده ماندند

بلوپرینت نسخه‌ی ۴/۵ تغییرات بیشتری هم دارد (`gender: String` → `enum Gender`، nullable شدن
`height`/`build`/`hair`/`facialFeatures`، افزودن `physicalFeatures`/`toPromptString()`) که
هیچ‌کدام در فهرست هفت‌موردی صریح دستور کار این قدم برای `AssetModels.kt` نبودند. عمداً به قدم
بعدی موکول شدند تا از تغییرات ضمنی/غیرخواسته پرهیز شود.

## تصمیم ۵: `UpdateResult` — grep قبل از افزودن `Warned`

طبق دستور صریح، قبل از افزودن حالت سوم `Warned`، کل پروژه (نه فقط `domain/asset`) برای هر
`when (result)` بدون `else` روی `UpdateResult` grep شد. نتیجه:

- `grep -rln "UpdateResult"` در `app/src/main` و `app/src/test`: تطبیق‌های واقعی فقط در
  `domain/visualidentity/StyleMatrix.kt` (یک نوع بی‌ربط به نام مشابه `StyleUpdateResult` —
  False Positive)، و کامنت‌های صرف در `domain/asset/AssetValidation.kt`,
  `domain/outputdelivery/Bilingual.kt`, `domain/dna/DnaValidation.kt`,
  `domain/validation/ValidationEngine.kt` (False Positive).
- تنها `when` غیر-exhaustive واقعی روی `UpdateResult` در کل پروژه در
  `app/src/test/.../asset/AssetContinuityTest.kt` است — تست
  `hard lock fields never produce anything other than Blocked or Allowed`
  (`when (result) { is UpdateResult.Allowed -> ...; is UpdateResult.Blocked -> ... }`
  بدون `else`)، که با افزودن `Warned` دیگر Exhaustive نیست. این فایل هم به همان امضای
  قدیمی ۲-آرگومانی `validateCharacterUpdate` وابسته است. **هر دو اصلاح (امضا +
  Exhaustiveness) به بخش دوم موکول شدند** چون فایل تست است و دستور کار صریحاً تست‌ها را
  از Scope این قدم خارج کرده بود — این تنش بین «grep و همان‌جا اصلاح کن» و «به تست‌ها دست
  نزن» به نفع مرز کلی Scope حل شد، اما یافته کامل اینجا و در گزارش ثبت شد، نه نادیده گرفته.

## پیاده‌سازی `AssetContinuity.kt`

- `UpdateResult.Warned(message: String)` اضافه شد؛ `Allowed`/`Blocked` بدون تغییر.
- `validateCharacterUpdate(level, rules, fieldBeingChanged)`: منطق سه‌شاخه‌ای FULL/MEDIUM/NONE
  عیناً از کد مفهومی بلوپرینت کپی شد. شاخه‌ی FULL byte-for-byte با رفتار/پیام‌های خطای قبل از
  Migration یکسان است (بدون هیچ نرم‌ترشدن، طبق هشدار صریح «حساس‌ترین بخش» دستور کار).
- `validateLocationUpdate(fieldBeingChanged, isStyleField)` و
  `validatePropUpdate(fieldBeingChanged, isFormField)` عیناً طبق کد مفهومی بلوپرینت — هر دو
  فقط بین `Warned`/`Allowed` انتخاب می‌کنند، هرگز `Blocked` برنمی‌گردانند.

## بخش دوم — تکمیل Migration (کامپایل کامل + تست‌ها + Commit نهایی)

### Rule های جدید (`AssetValidation.kt`)

- **Rule 10 (Blocking):** `validateObjectAsset(asset: ObjectAsset): List<ValidationIssue>` —
  `size`/`materialAndColor` خالی/whitespace-only هرکدام یک `ValidationIssue(BLOCKING)`
  مستقل تولید می‌کنند؛ `specialTrait` طبق جدول بلوپرینت («فیلد اختیاری نیازی به Rule
  ندارد») هیچ بررسی‌ای ندارد.
- **Rule 11 (Warning):** `validateBasePrompt(basePrompt: String?): ValidationIssue?` — یک
  تابع عمومی روی `String?` به‌جای سه تابع تقریباً تکراری برای CharacterAsset/LocationAsset/
  ObjectAsset، چون این سه نوع هیچ Supertype مشترکی ندارند و منطق فقط به مقدار String متکی
  است، نه به نوع Asset. این دقیقاً همان الگوی تعمیم‌یافته‌ی قبلاً تأییدشده در ADR-027
  (تعمیم `validateShotAgainstDna` فراتر از امضای تحت‌اللفظی بلوپرینت) است — مستند شده اینجا
  چون یک تعمیم آگاهانه است، نه صرفاً پرکردن یک جزئیات مشخص‌نشده.
- **Rule 12 (Blocking):** عمداً **بدون تابع Runtime**. `ObjectAsset.subtype: ObjectSubtype`
  در Kotlin از نوع enum غیر-nullable است، پس این تضمین از قبل در سطح Type System برقرار
  است — کامپایلر اصلاً اجازه نمی‌دهد یک `ObjectAsset` با `subtype` نامعتبر ساخته شود. این
  دقیقاً همان الگوی اثبات‌شده با تست `AssetContinuityTest`(«hard lock fields never produce
  anything other than Blocked or Allowed») است: یک قانون که به‌جای کد Runtime، با خودِ Type
  System اجرا می‌شود. تست اثباتی این مورد (`rule12 every ObjectSubtype value constructs a
  valid ObjectAsset`) در `AssetValidationTest.kt` آمده، نه یک تابع validate جدید.

### مصرف‌کنندگان اصلاح‌شده (Blast Radius، طبق grep معمار + grep تکمیلی من)

- **`data/repository/AssetDto.kt`:** `ObjectAssetDto` جدید اضافه شد. `CharacterAssetDto`
  گسترش یافت (`characterTier: String = "MAIN"`, `defaultMood`, `basePrompt`,
  `continuityLockLevel: String?`). `LocationAssetDto` فیلد `assetType` را از دست داد (دیگر
  لازم نیست — `AssetEntity.assetType` ستون Room همچنان تفکیک می‌کند) و `basePrompt`/
  `continuityLockLevel` گرفت.
  **تصمیم مهم:** `characterTier`/`subtype` در سطح DTO عمداً `String` با پیش‌فرض
  محافظه‌کارانه هستند (`"MAIN"`/`"GENERAL_PROP"`، دقیقاً پیشنهاد خودِ بلوپرینت) — نه enum
  غیر-nullable مثل نوع دامنه. این تمایز آگاهانه است: نوع دامنه‌ی Kotlin این دو فیلد را
  الزامی نگه می‌دارد (هر ساخت جدید در کد باید صریح تصمیم بگیرد)، اما DTO مسئول Backward
  Compatibility داده‌ی واقعاً سریالایز‌شده‌ی قدیمی روی دیسک است (رکوردهایی که این فیلدها را
  نداشتند) — دو نگرانی متفاوت که باید در دو لایه‌ی متفاوت حل شوند.
- **`data/repository/DnaAssetMappers.kt`:** `CharacterAssetDto.toDomain()`/`toDto()`،
  `LocationAssetDto.toDomain()`/`toDto()` به‌روزرسانی شدند؛ `ObjectAssetDto.toDomain()`/
  `ObjectAsset.toDto()` جدید اضافه شدند. `continuityLockLevel` غایب در DTO قدیمی با
  `defaultLockLevelForTier(tier)` جایگزین می‌شود (نه یک مقدار هاردکد جدا).
- **`data/repository/AssetRepository.kt`:** `saveObjectAsset`/`loadObjectAssets` جدید؛
  `loadAssets` قدیمی (که Object/Location مشترک برمی‌گرداند) به `loadLocationAssets`
  (فقط Location) تغییر نام یافت. تفکیک AssetType اکنون صریحاً با
  `AssetType.LOCATION.name`/`AssetType.OBJECT.name` انجام می‌شود (نه از فیلد حذف‌شده‌ی
  `asset.assetType`). جدول Room (`AssetEntity`) بدون تغییر ماند.
- **`domain/promptengine/PromptEngineModels.kt`:** `PromptGenerationInput.objects` از
  `List<LocationAsset>` به `List<ObjectAsset>` تغییر کرد؛ `locations` بدون تغییر.
  **یافته‌ی مهم با grep:** هیچ مصرف‌کننده‌ی واقعی دیگری (`PromptAssembly.kt` یا هر جای
  دیگر `domain/promptengine`/`domain/outputdelivery`) این دو فیلد را نمی‌خواند — دقیقاً
  همان یافته‌ی ADR-028 («این دو فیلد هنوز به Pipeline رندر واقعی وصل نشده‌اند») — پس Blast
  Radius واقعی محدود به تغییر امضای نوع بود، بدون نیاز به تغییر منطق دیگری.
- **`data/repository/PromptGenerationRepository.kt`:** دو خط `collectData` به
  `loadObjectAssets`/`loadLocationAssets` جدید اشاره می‌کنند.
- **۶ فایل تست:** `AssetContinuityTest.kt` (بازنویسی کامل — امضای جدید + Exhaustiveness
  fix + تست‌های MEDIUM/NONE/Location/Prop جدید)، `AssetModelsTest.kt` (جدید)،
  `AssetValidationTest.kt` (Rule 10/11/12)، `AssetRepositoryTest.kt`،
  `PromptGenerationRepositoryTest.kt`، `PromptAssemblyTest.kt`، `ShotOutfitSelectionTest.kt`،
  `CharacterContinuityTest.kt` (این دو آخر فقط افزودن `characterTier` به ساخت
  `CharacterAsset` موجود نیاز داشتند).

### یافته‌ی جانبی حین اجرا (باگ واقعی، رفع شد)

نام تست `` `loadLocationAssets skips ids that are not locations (e.g. an object)` `` باعث
شکست کامپایل شد: `e: ... Name contains illegal characters: ..` — نقطه (`.`) در نام تابع
Backtick-quoted شده، چون این نام مستقیماً به‌عنوان نام متد JVM کامپایل می‌شود و نقطه در نام
متد JVM غیرمجاز است. اصلاح: حذف «`e.g.`» از متن نام تست (بدون تغییر معنایی).

### اجرای واقعی (طبق الزام دستور کار)

`gradle :app:assembleDebug :app:testDebugUnitTest` (اجرای اول: `BUILD FAILED` به‌خاطر باگ
بالا؛ اجرای دوم پس از اصلاح: `BUILD SUCCESSFUL`). شمارش واقعی از JUnit XML
(`app/build/test-results/testDebugUnitTest/*.xml`، جمع `tests="N"` با grep+awk):
**۴۲۰ تست، ۰ Failure، ۰ Error.**

### خارج از Scope این Migration (هر دو بخش) — واقعاً باقی‌مانده

- Migration `gender`/`PhysicalAppearance` به enum `Gender`/nullable fields/`toPromptString()`
  (بلوپرینت v4/v5) — هیچ‌کدام در فهرست صریح دستور کار هیچ‌یک از دو بخش نبود.
- به‌روزرسانی `docs/reference/type-registry.md` برای انعکاس `continuityLockLevel` روی
  ردیف `CharacterAsset` (تصمیم ۳ بخش اول) — یک بدهی مستندسازی کوچک، نه کد.

## Consequences

- **آسان می‌شود:** ساختار داده و منطق اعتبارسنجی واحد ۰۶ اکنون دقیقاً با بلوپرینت نسخه‌ی
  ۵ برای Tier/Lock Levels، تفکیک Object/Location، و Rule های ۱۰/۱۱/۱۲ هم‌راستاست؛ کل پروژه
  کامپایل می‌شود و ۴۲۰ تست (شامل هر مصرف‌کننده‌ی این Migration) موفق‌اند.
- **بدهی شناخته‌شده و مستند:** `type-registry.md` نیاز به یک به‌روزرسانی کوچک برای
  `continuityLockLevel` دارد؛ Migration `gender`/`PhysicalAppearance` به یک قدم جداگانه‌ی
  آینده موکول شد.
