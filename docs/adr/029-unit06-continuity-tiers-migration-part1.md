# ADR-029: Migration واحد ۰۶ (Asset & Continuity) — بخش اول: Tier/Lock Levels

**تاریخ:** 2026-07-28
**وضعیت:** بخش اول کامل شد (فقط `AssetModels.kt` و `AssetContinuity.kt`). این دو فایل هنوز
به‌تنهایی کامپایل نمی‌شوند — `AssetValidation.kt`، تست‌ها و مصرف‌کنندگان عمداً در این قدم
دست‌نخورده ماندند و بخش دوم Migration را تشکیل می‌دهند. هنوز commit نشده (طبق دستور کار).

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

## خارج از Scope این قدم (بخش دوم)

- `domain/asset/AssetValidation.kt`
- `app/src/test/.../asset/AssetContinuityTest.kt` (امضای جدید + Exhaustiveness fix)
- مصرف‌کنندگان: `data/repository/AssetRepository.kt` (توابع/DTO جدید برای `ObjectAsset`)،
  `domain/promptengine/PromptEngineModels.kt` (`PromptGenerationInput.objects` نوعش باید
  `List<ObjectAsset>` شود)، ۲-۳ فایل تست دیگر که `AssetType.OBJECT` را با `LocationAsset`
  می‌سازند.
- به‌روزرسانی `docs/reference/type-registry.md` برای انعکاس `continuityLockLevel` روی
  `CharacterAsset` (تصمیم ۳ بالا).
- Migration `gender`/`PhysicalAppearance` (تصمیم ۴ بالا).

## Consequences

- **آسان می‌شود:** ساختار داده‌ی واحد ۰۶ اکنون دقیقاً با بلوپرینت نسخه‌ی ۵ برای Tier/Lock
  Levels و تفکیک Object/Location هم‌راستاست؛ `AssetContinuity.kt` آماده‌ی استفاده توسط
  `AssetValidation.kt` در بخش دوم است.
- **بدهی شناخته‌شده و مستند:** دو فایل این قدم به‌تنهایی کامپایل نمی‌شوند تا بخش دوم کامل
  شود (مورد انتظار، نه خطا)؛ `type-registry.md` نیاز به یک به‌روزرسانی کوچک برای
  `continuityLockLevel` دارد.
