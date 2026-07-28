# ADR-034: واحد ۰۱ب (AI Story Breakdown) — قدم سوم: Story-to-Domain Mapper

**تاریخ:** 2026-07-28
**وضعیت:** کامل شد و مستقل قابل‌کامپایل/تست است. `gradle :app:assembleDebug
:app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۴۹۲ تست، ۰ Failure، ۰ Error.

## Context

بخش ث بلوپرینت `01b-ai-story-breakdown.md` (نسخه ۲) حیاتی‌ترین بخش این واحد است —
تبدیل خروجی ساده‌ی AI بیرونی به `CharacterAsset`/`LocationAsset`/`ObjectAsset`/`Scene`/
`Shot` واقعی. بلوپرینت کد مفهومی دقیقی برای Mapper های اصلی داده بود، اما چند تابع
Placeholder (`defaultOutfitPlaceholder`, `deriveEnvironmentPlaceholder`,
`deriveSceneLocationPlaceholder`) و ساختار دقیق خروجی جریان کامل (`processAiResponse`)
را عمداً به تصمیم پیاده‌سازی واگذار کرده بود.

## پیش‌بررسی (تأییدشده با grep، طبق دستور کار)

- `NarrativeRole`/`Atmosphere` واقعی (`DEVELOPMENT`/`CALM` معتبرند، بدون
  `RISING_ACTION`/`NEUTRAL`) — تأیید یک اصلاح قبلی بلوپرینت، نه یافته‌ی جدید.
- `CharacterAsset`/`LocationAsset`/`ObjectAsset` دقیقاً طبق نسخه ۵ بلوپرینت ۰۶ (دو
  Migration قبلی این پروژه) موجودند.
- `Shot` دقیقاً ساختار مورد انتظار را دارد.

## تصمیم‌های مستقل

### ۱. `@Serializable` روی Simple*FromAi (لایه‌ی domain)

دستور کار صریحاً خواسته بود این چهار نوع مستقیماً با kotlinx.serialization از JSON خام
Decode شوند (بدون یک DTO میانی در `data/repository/`، چون این داده اصلاً توسط هیچ
Repository ای ذخیره نمی‌شود). این انحراف از قرارداد معمول پروژه (Serialization فقط در
لایه‌ی data/) عمداً پذیرفته شد — دقیقاً هم‌الگو با انحراف تأییدشده‌ی قبلی در
`Renderer.kt` (ADR-025: «import مستقیم kotlinx.serialization در لایه‌ی domain/ طبق
تصمیم صریح معمار همین قدم — نه یک الگوی عمومی جدید»).

فیلدهای JSON (`sceneName`, `shotNumber`, `characterNames`, ...) دقیقاً با نام فیلدهای
Kotlin این نوع‌ها یکی هستند (خودِ `STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION` قدم اول
همین اسم‌ها را در دستور به AI تولید کرده)، پس نیازی به `@SerialName` نبود.

### ۲. شناسه‌ها (`generateId`) — پیشوند تایپ‌شده، نه یک `generateAssetId()` عمومی

بلوپرینت هر سه Mapper (کاراکتر/مکان/شیء) را با یک تابع نام‌یکسان `generateAssetId()`
نشان داده بود. اما `docs/reference/naming-conventions.md` قرارداد صریح `[نوع]_[شناسه]`
دارد (`char_001`, `loc_001`, `scene_intro_01`) و کل داده‌ی تست موجود پروژه از همین
الگو پیروی می‌کند. تصمیم: یک `generateId(prefix: String)` خصوصی مشترک (هم‌الگو با
`UUID.randomUUID()...take(12)` تثبیت‌شده در `PromptAssembly.kt`/`OutputComposer.kt`/...)،
با پیشوندهای متمایز per-type (`char`, `loc`, `obj`, `scene`, `shot`, `outfit`) — این
همان روح تک‌تابعی بلوپرینت را حفظ می‌کند (یک پیاده‌سازی مشترک) ولی خروجی را با
قرارداد واقعی و خوانای پروژه هماهنگ می‌کند.

### ۳. `defaultOutfitPlaceholder()`

`Outfit(id، name="Default"، description توضیحی صریح «نیاز به بررسی کاربر»،
isDefault=true)` — الزام Rule 5 واحد ۰۶ (حداقل یک Outfit پیش‌فرض) را برآورده می‌کند؛
متن توضیح هم‌الگو با عبارت مشابه بلوپرینت خودش برای `materialAndColor` در
`mapAiObjectToAsset` است.

### ۴. `deriveEnvironmentPlaceholder()`

مقادیر خنثی انگلیسی (`type="unspecified"`, `size="medium"`,
`lightingCondition="natural"`) — هم‌راستا با قرارداد واقعی مشاهده‌شده در داده‌ی تست
موجود پروژه برای `Environment` (مقادیر کوتاه انگلیسی مثل `"indoor"`/`"small"`/`"dim"`)،
نه توصیف فارسی طولانی (که برای فیلدهای واقعاً آزاد/توصیفی مثل `materialAndColor`
مناسب‌تر است).

### ۵. `deriveSceneLocationPlaceholder(locationName)` — `LocationType.CUSTOM`

از یک رشته‌ی نام آزاد (مثلاً «خیابان شلوغ شهری») نمی‌توان با اطمینان تشخیص داد مکان
داخلی/بیرونی است. حدس نادرست `INDOOR`/`OUTDOOR` بدتر از یک برچسب صریح «سفارشی/نامشخص»
است — دقیقاً همان استدلال محافظه‌کارانه‌ای که بلوپرینت خودش برای
`ObjectSubtype.GENERAL_PROP` به کار برده بود، اینجا هم برای `LocationType.CUSTOM`
تکرار شد.

### ۶. گزارش نام‌های یافت‌نشده — `ShotMappingResult` (data class، نه `Pair`)

`mapAiShotToShot` اکنون `ShotMappingResult(shot, unmatchedNames: List<String>)`
برمی‌گرداند به‌جای فقط `Shot`. یک `data class` نام‌دار به‌جای `Pair<Shot, List<String>>`
انتخاب شد چون دو مقدار برگشتی معنای متفاوت دارند — خواناتر در محل فراخوانی
(`mapping.shot`/`mapping.unmatchedNames` در برابر `.first`/`.second`).

### ۷. `StoryBreakdownResult`/`ProcessAiResponseResult` (ساختار جریان کامل)

`StoryBreakdownResult(characters, locations, objects, scenes, shots, warnings)` —
دقیقاً فیلدهای درخواستی دستور کار. `ProcessAiResponseResult` سه زیرکلاس دارد (نه دو):
`Success`، `NeedsManualRepair` (Rule 7، از `repairJson` قدم قبل)، و
`MissingRequiredKeys` (Rule 8) — این سومی چون Rule 8 هم Blocking است و باید قبل از
Parse ساختاریافته کامل متوقف شود، نه فقط به‌عنوان یک Warning در `warnings` جمع شود.

### ۸. Rule 11 — بدون تابع دامنه

طبق تصریح خودِ بلوپرینت («ساختاری، نه Validation»)، هیچ تابعی برایش نوشته نشد — فقط
در کامنت کد مستند شد که این گام تعاملی تأیید کاربر به واحد ۱۶ (UI) موکول است.

## تست

`StoryToDomainMapperTest.kt` — ۲۲ تست: `mapAiCharacterToAsset` (هر سه role، gender
معتبر/نامعتبر/غایب، حساسیت به حروف بزرگ/کوچک، Outfit پیش‌فرض، حفظ description)؛
`mapAiLocationToAsset`/`mapAiObjectToAsset` (حالت پایه)؛ `groupAiShotsIntoScenes`
(گروه‌بندی + جداسازی + ترتیب sceneNumber)؛ `mapAiShotToShot` (تطبیق موفق، نام‌های
یافت‌نشده)؛ Rule 9/10 (پاس و شکست)؛ `processAiResponse` end-to-end (JSON معتبر، JSON
قابل‌تعمیر با کاما اضافه، JSON واقعاً خراب → `NeedsManualRepair`، کلید غایب →
`MissingRequiredKeys`).

جمعاً ۲۲ تست جدید (۴۷۰ → ۴۹۲).

## خارج از Scope این قدم

بخش ب (AI Connector Profile) — طبق دستور کار صریح، آخرین قدم جداگانه‌ی این واحد است.

## Consequences

- **آسان می‌شود:** با تکمیل این قدم، سه بخش اصلی منطق دامنه‌ی واحد ۰۱ب (الف/پ/ت/ث)
  کامل شدند؛ تنها بخش باقی‌مانده (ب) صرفاً یک لایه‌ی اتصال I/O (Ktor HTTP) است، بدون
  منطق دامنه‌ی پیچیده‌ی جدید.
- **بدون بدهی جدید شناخته‌شده** از این قدم.
