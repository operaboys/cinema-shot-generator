# ADR-031: Migration واحد ۰۶ (Asset & Continuity) — PhysicalAppearance/Gender

**تاریخ:** 2026-07-28
**وضعیت:** کامل شد. پروژه کامل کامپایل می‌شود و تست می‌گذراند (۴۳۶ تست، ۰ Failure، ۰
Error — `gradle :app:assembleDebug :app:testDebugUnitTest`، `BUILD SUCCESSFUL`).

## Context

`docs/blueprints/06-asset-and-continuity-v2.md` (نسخه ۵) دو تغییر روی `PhysicalAppearance`
تعریف کرده بود که در دو Migration قبلی واحد ۰۶ (ADR-029) عمداً خارج از Scope مانده بودند
چون دستور کار آن قدم‌ها صریحاً هفت مورد مشخص را برای `AssetModels.kt` فهرست کرده بود که
`PhysicalAppearance`/`Gender` جزوشان نبود. این قدم آن دو مورد را تکمیل می‌کند — پیش‌نیاز
واحد ۰۱ب.

## پیاده‌سازی `AssetModels.kt`

- `enum class Gender { FEMALE, MALE, OTHER }` جدید — طبق فرم واقعی «Add New Asset».
- `PhysicalAppearance` بازنویسی شد: `gender: String` → `gender: Gender`؛
  `height`/`build`/`hair`/`facialFeatures` از غیر-nullable به nullable (چون طبق فرم واقعی
  همه‌شان اختیاری‌اند)؛ `physicalFeatures: String? = null` جدید (توصیف آزاد تکمیلی).
  `toPromptString()` عیناً از کد مفهومی بلوپرینت کپی شد — بدون تفسیر شخصی.

## `CharacterContinuity.kt` (واحد ۱۱)

تابع محلی `describePhysicalAppearance` حذف شد؛ `enforceCharacterContinuity` اکنون
`character.physicalAppearance.toPromptString()` را مستقیماً صدا می‌زند.

**یافته‌ی شفاف:** بلوپرینت این Migration را برای رفع یک باگ واقعی توصیف کرده بود («بدون
`toPromptString`، `enforceCharacterContinuity` مجبور بود از `toString()` پیش‌فرض Kotlin
استفاده کند که خروجی غیرقابل‌استفاده تولید می‌کرد»). با خواندن کد واقعی این پروژه تأیید شد
این باگ در این پروژه **رخ نداده بود** — `describePhysicalAppearance` از ابتدا توصیف صریح
دستی از فیلدهای خام می‌ساخت (نه `toString()` پیش‌فرض)، پس خروجی همیشه معنادار بوده. با
این حال، تابع محلی حذف و با `toPromptString()` جایگزین شد، چون: (۱) دستور کار صریحاً همین
را خواسته بود؛ (۲) دو محل مستقل برای یک منطق («توصیف فیزیکی به متن») بدهی تکنیکی واقعی
است، فارغ از این‌که باگ خاصی داشته یا نه — منبع واحد حقیقت برای این منطق اکنون فقط
`PhysicalAppearance.toPromptString()` است.

## Blast Radius (grep در کل پروژه)

### `data/repository/AssetDto.kt` — تصمیم: nullable↔nullable طبیعی، gender همچنان String

`PhysicalAppearanceDto`: `height`/`build`/`hair`/`facialFeatures` به nullable تغییر
کردند (مستقیماً هم‌شکل با دامنه)؛ `physicalFeatures: String? = null` جدید. `gender` اما
**همچنان `String`** ماند (نه `Gender` enum) — همان الگوی ADR-029 برای `characterTier`/
`subtype`: نوع دامنه enum غیر-nullable الزامی است، اما DTO مسئول Backward Compatibility
داده‌ی قدیمی سریالایز‌شده است. تفاوت مهم با `characterTier`/`subtype`: آنجا نگرانی «مقدار
غایب» بود (پیش‌فرض محافظه‌کارانه لازم)؛ اینجا نگرانی «حروف‌بندی متفاوت» است (داده‌ی قدیمی
احتمالاً `"male"` با حروف کوچک ذخیره کرده، نه `"MALE"`). راه‌حل: `DnaAssetMappers.kt` با
`Gender.valueOf(physicalAppearance.gender.uppercase())` به enum تبدیل می‌کند —
uppercase-normalize، نه فقط `valueOf` مستقیم (که برای `"male"` با Exception شکست می‌خورد).

### `data/repository/DnaAssetMappers.kt`

`CharacterAssetDto.toDomain()`/`CharacterAsset.toDto()` برای `physicalAppearance` کامل
به‌روزرسانی شدند: `gender` با uppercase-normalize (بالا)؛ `hair`/`facialFeatures` با
`?.let { ... }` (nullable-safe)؛ `physicalFeatures` pass-through مستقیم.

### ۶ فایل تست

`PromptGenerationRepositoryTest.kt` (تنها محلی که `PhysicalAppearance(...)` را Positional
می‌ساخت — به Named Argument تبدیل شد چون افزودن `physicalFeatures` بین `hair` و
`facialFeatures` در امضای جدید، موقعیت‌های Positional را جابه‌جا می‌کرد)،
`AssetRepositoryTest.kt`, `ShotOutfitSelectionTest.kt`, `AssetModelsTest.kt`,
`CharacterContinuityTest.kt`, `PromptAssemblyTest.kt` — همه فقط `gender = "male"` را به
`gender = Gender.MALE` تغییر دادند (`import Gender` اضافه شد جز `AssetModelsTest.kt` که در
همان پکیج `domain.asset` است).

## `docs/reference/type-registry.md`

- ردیف `PhysicalAppearance` و enum `Gender` از قبل شکل کامل نسخه ۵ را مستند کرده بودند
  (وضعیت ✅🔧/✅) — بدون نیاز به تغییر، تأییدشده با grep.
- ردیف `CharacterAsset` فیلد `continuityLockLevel` را نداشت (یافته‌ی مستندشده در
  ADR-029) — اضافه شد.

## تست

- ۲ تست جدید برای `PhysicalAppearance.toPromptString()` در `AssetModelsTest.kt`: همه‌ی
  فیلدهای اختیاری پر (شامل `physicalFeatures`/`distinctiveMarks`) و فقط فیلدهای الزامی
  (`ageRange`/`gender`) پر.
- هر ۶ فایل تست موجود به‌روزرسانی شدند (نه حذف).

جمعاً ۲ تست جدید (۴۳۴ → ۴۳۶).

## Consequences

- **آسان می‌شود:** `PhysicalAppearance` اکنون دقیقاً با بلوپرینت نسخه ۵ هم‌راستاست؛ هر دو
  محدودیت مستندشده در README (بخش پایانی) برطرف شدند؛ واحد ۰۱ب اکنون می‌تواند
  `Gender` enum واقعی را مصرف کند، نه رشته‌ی آزاد.
- **بدون بدهی جدید شناخته‌شده** از این قدم.
