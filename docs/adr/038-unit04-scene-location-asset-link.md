# ADR-038: واحد ۰۴ (Scene Engine) — اتصال `Scene` به کتابخانه‌ی `LocationAsset`

**تاریخ:** 2026-07-28
**وضعیت:** کامل شد و مستقل قابل‌کامپایل/تست است.

## Context

این Migration مستقیماً یافته‌ی F2 ممیزی `docs/audit/pre-unit16-audit.md` را رفع
می‌کند: `Scene.location` فقط `SceneLocation(type, description)` بود — یک برچسب
توصیفی آزاد، بدون هیچ ارجاعی به `LocationAsset` کتابخانه‌ی دارایی‌ها (واحد ۰۶). این
یافته 🔴 (مسدودکننده) اعلام شده بود چون بلوپرینت ۱۶ صراحتاً یک Dropdown انتخاب از
کتابخانه‌ی `LocationAsset` می‌خواهد، نه یک فیلد متن آزاد.

تصمیم معماری قطعی («گزینه‌ی الف») از سوی معمار داده شد: افزودن `locationAssetId`
به `Scene`، در کنار `SceneLocation` موجود.

## بخش الف — `Scene.locationAssetId`

`Scene` با `val locationAssetId: String? = null` گسترش یافت. `SceneLocation`
حذف/جایگزین نشد — طبق دستور صریح، چون `description` همچنان برای توصیف متنی آزاد
لازم است (خصوصاً خروجی خودکار واحد ۰۱ب که فقط توصیف متنی تولید می‌کند، نه یک Asset
واقعی از کتابخانه). `null` یعنی این Scene هنوز به کتابخانه وصل نشده.

## بخش ب — بررسی درخواست‌شده: `sceneLocationOverride` جدید در برابر `Shot.locationIds` موجود

دستور کار به‌صراحت از من خواست با grep بررسی کنم آیا «ارث‌بری تک‌مکانی از Scene»
(الگوی پیشنهادی `sceneLocationOverride: SourcedSettings<String>?`) و «فهرست
چندگانه‌ی مستقیم `Shot.locationIds`» دو مفهوم واقعاً جدا هستند یا یکی — و اگر یکی
بودند، فقط بخش الف را پیاده کنم.

**نتیجه‌ی بررسی: این دو مفهوم یکی هستند؛ فیلد جدید اضافه نشد.**

شواهد (با grep تأیید شده):

1. **بلوپرینت هیچ تمایزی قائل نشده.** تنها ارجاع واقعی به `locationIds` در
   بلوپرینت‌های ۰۵/۰۶ همان اعلان فیلد در `docs/blueprints/05-shot-engine-v2.md:198`
   است (`val locationIds: List<String> = emptyList()`) — بدون هیچ توضیح متنی
   درباره‌ی رابطه‌اش با یک مفهوم «ارث‌بری از Scene» مجزا. اگر این دو مفهوم واقعاً
   قرار بود جدا باشند، انتظار می‌رفت بلوپرینت ۰۵/۰۶ حداقل یک جمله درباره‌ی تفاوتشان
   بنویسد.
2. **`Shot.locationIds` از قبل، در عمل، دقیقاً همان نقش را ایفا می‌کند که
   `sceneLocationOverride` پیشنهادی قرار بود ایفا کند:**
   - `ShotValidation.validateShotHasSubject` (`domain/shot/ShotValidation.kt:34-47`)
     و `ValidationEngine.checkDataCompleteness` هر دو `locationIds` را دقیقاً
     هم‌تراز `characterIds`/`objectIds` در یک قانون **BLOCKING** («هر شات باید
     حداقل به یک Subject متصل باشد») قرار می‌دهند.
   - `PromptGenerationRepository.collectData` مستقیماً همین لیست را برای بارگذاری
     واقعی `LocationAsset` از کتابخانه به کار می‌برد
     (`assetRepository.loadLocationAssets(shot.locationIds)`).
3. **افزودن یک فیلد دوم و موازی که در هیچ‌کدام از این دو مصرف‌کننده‌ی واقعی
   سیم‌کشی نشود، همان الگوی «فیلد تزئینی/غیرمتصل» است** که این پروژه در ADR-036
   آگاهانه از آن پرهیز کرد (جایی که یک یافته‌ی مشابه — فیلدهای UI بدون مسیر DTO —
   به‌عنوان باگ خاموش واقعی شناسایی و رفع شد). سیم‌کشی یک `sceneLocationOverride`
   جدید به این دو مصرف‌کننده هم عملاً به معنای بازتعریف رفتار `locationIds` موجود
   با یک اسم دوم بود، نه افزودن قابلیت جدید.

**تصمیم:** فقط بخش الف (`Scene.locationAssetId`) پیاده شد. به‌جای فیلد جدید در
`Shot`، تابع خالص `resolveSceneLocation(shot: Shot, scene: Scene): List<String>`
در `domain/shot/ShotSettingsResolution.kt` اضافه شد که تهی‌بودن `locationIds`
موجود را به‌عنوان سیگنال ارث‌بری/Override بازتفسیر می‌کند:

```kotlin
fun resolveSceneLocation(shot: Shot, scene: Scene): List<String> {
    return shot.locationIds.ifEmpty { listOfNotNull(scene.locationAssetId) }
}
```

- خالی ⇐ این Shot مکان مستقلی انتخاب نکرده، پس `Scene.locationAssetId` ارث می‌رسد.
- غیرخالی ⇐ این Shot صریحاً Location(های) خودش را انتخاب/Override کرده.

**چرا `Result<T>` نه:** برخلاف `resolveCameraSettings`/`resolveLightingSettings`/
`resolveEnvironmentSettings` (که واقعاً می‌توانند شکست بخورند چون نبود camera/
lighting/environment یک وضعیت غیرمنتظره است)، فهرست خالی Location برای یک Shot
یک وضعیت کاملاً معتبر و رایج در این کدبیس است (مثلاً شاتی که فقط روی یک Character
تمرکز دارد و اصلاً به مکان خاصی متصل نیست) — نه یک خطا.

### سیم‌کشی واقعی (نه فقط تعریف تزئینی)

`resolveSceneLocation` در `PromptGenerationRepository.collectData` جایگزین
فراخوانی خام `shot.locationIds` شد (`assetRepository.loadLocationAssets(...)`).
این تغییر کاملاً Backward Compatible است — تمام تست‌های موجود `locationIds`
غیرخالی دارند، پس شاخه‌ی `ifEmpty` هرگز برایشان فعال نمی‌شود؛ اما از این پس، یک
Shot با `locationIds` خالی که به یک Scene با `locationAssetId` واقعی متصل است، آن
مکان را به‌درستی در پرامپت نهایی دریافت می‌کند، نه اینکه بی‌صدا نادیده گرفته شود.

## بخش ج — DTO/Room (Blast Radius)

`SceneDto`/`SceneMappers.kt` (`data/repository/`) با grep بررسی و تکمیل شدند:
`SceneDto.locationAssetId: String? = null` اضافه شد (nullable در سطح DTO هم،
چون `null` در دامنه یک مقدار کاملاً معتبر است، نه یک حالت جاافتاده که نیاز به
پیش‌فرض محافظه‌کارانه‌ی متفاوت داشته باشد — بر خلاف بعضی فیلدهای ADR-036 که
enum بودند و نیاز به یک مقدار پیش‌فرض غیر-null معنادار داشتند)، و هر دو جهت
`toDomain()`/`toDto()` در `SceneMappers.kt` تکمیل شدند.

چون بخش ب هیچ فیلد جدیدی به `Shot` اضافه نکرد، هیچ تغییری در `ShotDto`/
`DtoMappers.kt` لازم نبود — `locationIds` از قبل کاملاً در Room سیم‌کشی شده بود.

## تست

- `SceneModelsTest.kt` (جدید، ۳ تست): پیش‌فرض `null`؛ مقدار صریح؛ استقلال
  `SceneLocation.description` از `locationAssetId`.
- `ShotSettingsResolutionTest.kt` (۳ تست جدید برای `resolveSceneLocation`): ارث‌بری
  از Scene وقتی `locationIds` خالی و `locationAssetId` موجود است؛ فهرست خالی وقتی
  هر دو خالی/null هستند؛ Override با `locationIds` غیرخالی شات (نادیده‌گرفتن
  پیش‌فرض Scene).
- `SceneMappersTest.kt` (جدید، ۱ تست Round-Trip): `Scene.toDto().toDomain()` هم
  با `locationAssetId` پرشده و هم با `null`.

## Consequences

- **آسان می‌شود:** فرم «تنظیمات Scene» بلوپرینت ۱۶ اکنون می‌تواند واقعاً از
  کتابخانه‌ی `LocationAsset` انتخاب کند (نه یک Label توصیفی آزاد)، با تضمین
  ذخیره‌سازی صحیح در Room و اثر واقعی روی پرامپت نهایی از طریق
  `resolveSceneLocation`.
- **بدون بدهی جدید شناخته‌شده:** F2 ممیزی pre-Unit 16 کاملاً رفع شد — این آخرین
  یافته‌ی 🔴 (مسدودکننده) بود؛ هیچ یافته‌ی 🔴 دیگری باقی نمانده (فقط ۷ یافته‌ی 🟡 و
  ۳ یافته‌ی ⚪ غیرمسدودکننده از ممیزی اصلی باقی‌اند).
