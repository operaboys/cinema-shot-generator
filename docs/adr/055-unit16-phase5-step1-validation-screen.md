# ADR-055: واحد ۱۶ — فاز ۵، قدم ۱ — صفحه‌ی Validation

**تاریخ:** 2026-08-06
**وضعیت:** کامل شد. `gradle :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL` (۶۳۵ تست، ۰ شکست، ۰ خطا).

## Context

قدم اول فاز ۵ واحد ۱۶ — فقط صفحه‌ی Validation (Prompt Generation و Output
Delivery در قدم‌های جداگانه‌ی بعدی). منابع حقیقت دوگانه:
`docs/blueprints/16-user-workflow-v2.md` (مراحل ۶-۷-۸) و
`docs/design/README.md` بخش «۹. Validation».

## تحقیق اولیه (طبق دستور صریح کار، قبل از هر کدی)

**آیا `ValidationIssue` سراسری سه سطح Level 1/2/3 را پشتیبانی می‌کند؟**
خیر — تأییدشده با grep مستقیم `domain/validation/ValidationEngine.kt`:
`ValidationIssue(severity: Severity, field: String?, message: String,
suggestion: String?)` و `ValidationReport` هیچ فیلد Level‌ای ندارند. کامنت‌های
«Level 1: کامل بودن داده» که بالای چند تابع (`validateDataCompleteness` و
مشابه) دیده می‌شود فقط مستندسازی کد است، نه یک نوع Runtime — این دسته‌بندی
باید در همین قدم ساخته شود (دقیقاً همان‌طور که دستور کار پیش‌بینی کرده بود).

**فهرست کامل توابع `ValidationIssue`-بازگردان (grep در کل `domain/`):**
`domain/validation/` (ValidationEngine، LogicConflictChecker،
DependencyResolver)، `domain/shot/ShotValidation.kt` (واحد ۰۵)،
`domain/camera/CameraValidation.kt` + `MotionIntensityValidation.kt` (واحد
۰۹)، `domain/scene/SceneValidation.kt` (واحد ۰۴)،
`domain/sceneconditions/LightingValidation.kt` + `EnvironmentValidation.kt`
(واحد ۰۸، ۱۳ Rule)، `domain/dna/DnaValidation.kt` (واحد ۰۲)،
`domain/asset/AssetValidation.kt` (واحد ۰۶)، به‌علاوه‌ی واحدهایی خارج از
Scope این قدم (Story/StoryBreakdown/PromptEngine/OutputDelivery/Storage) —
طبق دستور کار صریح («Shot/Scene/DNA/Asset فعلی»)، فقط واحدهای مرتبط با یک
Shot در حال ویرایش (نه کل پروژه) در تجمیع این قدم وایر شدند.

## تصمیم ۱ (توافق با بلوپرینت ۱۶ درباره‌ی ساختار صفحه، نه تناقض واقعی)

بلوپرینت ۱۶ مرحله‌ی ۶ را «نمایش نامحسوس، نه یک صفحه‌ی جدا» توصیف می‌کند (یک
نوار کوچک بالای بخش «Full Video Prompt»)؛ design/README بخش ۹ یک صفحه‌ی
کاملاً مستقل با Header/دو کارت شمارش/سه بخش سطح توصیف می‌کند. این دو در
ظاهر متناقضند، اما design/README خودش (بخش Interactions) این سه ابزار
(Validation، Prompt Generator، Output Delivery) را سه لینک **جدا** در گروه
TOOLS منوی همبرگری فهرست می‌کند — یعنی design/README از قبل این سه را سه
صفحه/ابزار متمایز می‌داند، نه یک پنل واحد. تفسیر پذیرفته‌شده (بدون نیاز به
توقف/Escalation، چون خودِ دستور کار همین قدم صریحاً ساختار design/README را
برای «این صفحه» خواسته بود): توصیف نامحسوس بلوپرینت ۱۶ به نوار فشرده‌ی
داخل صفحه‌ی آینده‌ی Prompt Generation (قدم بعدی همین فاز) مربوط است؛ این
صفحه‌ی مستقل «Validation» (این قدم) یک ابزار عمیق‌تر و جداگانه است. اگر در
عمل این تفسیر با تصمیم آینده‌ی معمار برای Prompt Generation ناسازگار درآید،
باید به معمار گزارش شود — فعلاً پیشرفت را مسدود نکرد.

## تصمیم ۲: `LeveledValidationIssue` — نوع Wrapper تازه، نه فیلد تازه روی `ValidationIssue`

به‌جای افزودن `level: ValidationLevel` مستقیم به `ValidationIssue` سراسری
(که در ده‌ها محل دیگر بدون هیچ مفهوم Level‌ای بازاستفاده می‌شود و تغییرش
یک Migration پرخطر/غیرضروری بود)، یک Wrapper تازه در
`domain/validation/ValidationAggregator.kt` ساخته شد:
`data class LeveledValidationIssue(val level: ValidationLevel, val issue:
ValidationIssue)`. طبقه‌بندی سطح یک تصمیم «چگونه این Rule در این تجمیع
مصرف می‌شود» است، نه یک ویژگی ذاتی خودِ Issue.

## تصمیم ۳: دسته‌بندی سطح ۱/۲/۳ — معیار دقیق

- **سطح ۱ (کامل بودن داده):** آیا داده‌های الزامی خودِ Shot/Scene موجودند؟
  `validateShotDescription`، `validateShotHasSubject`،
  `validateShotBeatTimeline`، `validateNegativePromptOverride` (واحد ۰۵)،
  `validateSceneHasShotsBeforeFinalize` (واحد ۰۴).
- **سطح ۲ (سازگاری منطقی):** آیا ترکیب فیلدهای خودِ همین Shot/Scene با هم
  منطقی است؟ ۴ Rule دوربین (واحد ۰۹: `checkLensDistanceMismatch`,
  `checkStaticMovementWithHandheldStabilization`,
  `checkRackFocusSubjectCount`, `checkExtremeWideWithShallowDepthOfField`)،
  ۲ Rule صحنه (واحد ۰۴)، و هر ۱۳ Rule نور/محیط واحد ۰۸ (۵ نور + ۸ محیط).
- **سطح ۳ (تداوم و وابستگی):** آیا این Shot به موجودیت‌های *بیرونی*
  (تنظیمات DNA پروژه، Asset های متصل) وابسته و با آن‌ها سازگار است؟
  `validateShotDuration`/`validateShotAgainstDna` (واحد ۰۲، وابستگی به
  DNA)، `validateDefaultOutfitExists`/`validateObjectAsset`/
  `validateBasePrompt` برای هر Asset متصل (واحد ۰۶، وابستگی به کتابخانه).

## تصمیم ۴: وایر کردن هر ۱۳ Rule نور/محیط در همین صفحه (نه Tab نور/محیط Shot Composer)

طبق یادآوری صریح دستور کار (و دلیل مستند خودِ ADR-053): این Rule‌ها به
`TimeOfDay`/`locationType` صحنه نیاز دارند که در سطح Tab Shot Composer در
دسترس نبود (`ShotComposerViewModel` وابستگی به `SceneRepository` ندارد) اما
در این صفحه (که Scene را هم می‌خواند) در دسترس است — دقیقاً همان محل
معماری درستی که ADR-053 پیش‌بینی کرده بود.

## تصمیم ۵: Rule های عمداً وایر نشده (با دلیل، هم‌الگو با تصمیم عدم‌وایر ADR-053)

- `MotionIntensityValidation.kt` (`validateSpeedIntensity`/
  `validateMotionBlur`): `SubjectMotion` هیچ‌جای `Shot`/`CameraSettings`
  وایر نشده (تأییدشده با grep) — این Rule‌ها کاملاً یتیم‌اند.
- `validateCameraMovementDuration`: نیاز به `movementDurationSeconds` دارد
  که هیچ فیلد UI/دامنه‌ای برایش وجود ندارد (طبق کامنت خودِ
  `CameraValidation.kt`: «پارامتر خارجی مستقل»).
- `validateImageReferenceFile`: نیاز به `fileExists` واقعی دارد؛ طبق
  ADR-051/052 هیچ Infra انتخاب‌گر تصویر در کدبیس وجود ندارد.
- `checkMandatoryElementsPresent` (Rule 3 DNA): تعریف «عناصر شامل‌شده در
  یک Shot» مبهم است (نمونه‌های بلوپرینت مثل `atmospheric_depth` برچسب‌های
  کیفی‌اند، نه Character/Object ID) — وایرکردنش نیازمند اختراع یک نگاشت
  بدون‌مبنا بود.
- Rule های ساخت/حذف Asset (`validateAssetIdUniqueness`,
  `validateAssetDeletion`, `checkSimilarAssetName`,
  `validateReferenceImageFile`): مربوط به جریان *ساخت* Asset‌اند، نه
  اعتبارسنجی یک Shot موجود.

## تصمیم ۶: نقطه‌ی ورود از داخل Shot Composer، نه Nav Drawer

Drawer فعلاً برای هیچ‌کدام از مقصدهای وابسته‌به‌Context (Story/DNA/
Scenes/Shots) واقعاً Navigate نمی‌کند — فقط «دارایی‌ها» (بدون‌Context) واقعی
است (تصمیم مستند ADR-044). Validation هم به `projectId`/`sceneId`/`shotId`
مشخص نیاز دارد؛ ساختن یک جریان «انتخاب شات از Drawer» کاملاً خارج از Scope
این قدم بود. به‌جایش، یک دکمه‌ی «اعتبارسنجی این شات» در Header خودِ Shot
Composer اضافه شد (فقط وقتی `shotId != null`، یعنی شات از‌پیش‌ذخیره‌شده) —
دقیقاً همان الگوی SceneDetail→ShotComposer. مسیر تازه `Validation(projectId,
sceneId, sceneNumber, sceneDisplayTitle, shotId)` عمداً `sceneNumber`/
`sceneDisplayTitle` را هم تکرار می‌کند — نه برای نمایش، بلکه چون «برگشت»
باید دقیقاً به همان `ShotComposer` معتبر Navigate کند (قاعده‌ی صریح
«Back Navigation Contextual، نه popBackStack ساده»، `MainScaffold.kt`).

## تصمیم ۷: Contrast کارت‌های شمارش

طبق الزام صریح سند طراحی («solid/near-opaque fills + a visible 2dp
border»، نه یک Tint کم‌رنگ مثل باگ قبلی Asset Library/Camera chips)، دو
کارت از `Surface` با `color` کاملاً Solid (نه `.copy(alpha=...)`) ساخته
شدند: BLOCKING از `MaterialTheme.colorScheme.error` +
`MaterialTheme.colorScheme.onError` (توکن از‌پیش‌تعریف‌شده)، WARNING از
`CinemaTheme.extendedColors.warning` (زرد روشن، 0xFFFFB648) + متن سیاه
ثابت (هیچ توکن `onWarning` در تم موجود نبود؛ ساخت یک توکن تازه فقط برای
این یک کارت خارج از scope این قدم بود).

## یافته‌های دیباگ واقعی (سه باگ جدا، هر سه رفع شدند)

۱. **تداخل متن Nav Drawer**: انتظار تست روی متن دقیق «اعتبارسنجی» (عنوان
   صفحه) هرگز به «exactly 1» نمی‌رسید — چون `drawer.validation` (لینک
   Drawer، همیشه در درخت Semantics حاضر است حتی وقتی Drawer بسته/نامرئی
   است، طبق `ModalNavigationDrawer`) دقیقاً همان متن را دارد. رفع: انتظار
   تست روی testTag منحصربه‌فرد صفحه (کارت شمارش)، نه متن عنوان.
۲. **`CountCard` بدون `mergeDescendants`**: `Surface` غیرکلیک‌پذیر (برخلاف
   `OpaqueChip` که overload کلیک‌پذیر دارد و Material3 خودش merge می‌کند)
   متن دو `Text` فرزندش (شمار + برچسب) را در semantics خودش ادغام نمی‌کرد؛
   `assertTextContains` روی testTag کارت همیشه شکست می‌خورد. رفع:
   `Modifier.semantics(mergeDescendants = true) {}` روی `Surface`.
۳. **Fixture ناقص در تست**: `Scene` Seed‌شده در تست `shotCount` را صریح
   تنظیم نکرده بود (پیش‌فرض ۰) → Rule 1 واحد ۰۴
   (`validateSceneHasShotsBeforeFinalize`) یک BLOCKING اضافه‌ی ناخواسته
   تولید می‌کرد. رفع: `shotCount = 1` صریح در fixture.
۴. **جست‌وجوی سراسری متن به‌جای Scoped**: هر Issue Card هم برچسب شدت
   («BLOCKING»/«WARNING») نشان می‌دهد؛ جست‌وجوی سراسری صفحه با
   `onNodeWithText` بیش از یک گره پیدا می‌کرد وقتی حداقل یک Issue با همان
   شدت وجود داشت (رفتار UI صحیح، نه باگ). رفع: بررسی `assertTextContains`
   محدود به خودِ testTag کارت شمارش.

## تست‌ها

- `ValidationAggregatorTest.kt` (خالص، بدون Robolectric): تجمیع صفر-مشکل
  روی داده‌ی کاملاً خنثی؛ تجمیع هم‌زمان نقض از واحدهای مختلف (۰۵/۰۹/۰۲/۰۶) با
  دسته‌بندی سطح صحیح؛ شمارش دقیق BLOCKING/WARNING؛ `checkSunlightAtNight`
  (یکی از ۱۳ Rule) هم در حالت فعال و هم غیرفعال.
- `ValidationFlowTest.kt` (End-to-End، Robolectric): ورود واقعی از دکمه‌ی
  Shot Composer → صفحه‌ی Validation؛ شمارش دقیق BLOCKING=۱ برای یک شات با
  نقض واقعی «نور خورشید در شب» (یکی از ۱۳ Rule)؛ نمایش/Solid بودن ساختاری
  دو کارت شمارش.

## Consequences

- `ShotComposerScreen.kt` اکنون یک دکمه‌ی «اعتبارسنجی این شات» دارد (فقط
  برای شات‌های ذخیره‌شده).
- بدهی ثبت‌شده برای آینده: Nav Drawer's «اعتبارسنجی» همچنان placeholder
  «به‌زودی» می‌ماند (نیازمند جریان انتخاب شات مستقل از Context، خارج از
  Scope این قدم).
- قدم مستقل بعدی طبق دستور کار: قدم ۲ فاز ۵ (Prompt Generation).
