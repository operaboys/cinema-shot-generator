# ADR-010: چهار Migration سراسری روی واحدهای ۰۱/۰۲/۰۵/۰۶/۰۷

**تاریخ:** 2026-07-19
**وضعیت:** تمام ۴ Migration اجراشده و تأییدشده — سؤال‌های باز تجمیعی قبلی حل شدند

## Context

طبق تصمیم معمار، چهار سؤال باز تجمیعی که در ADR-004، ADR-006، و ADR-007 مطرح شده بودند در یک قدم Migration/Refactor حل شدند: (۱) یکی‌سازی ValidationResult محلی واحدهای ۰۱/۰۲/۰۶ با نوع سراسری واحد ۰۷، (۲) اتصال واقعی CinematicMode/MotionLevel در واحد ۰۷، (۳) حذف نسخه‌ی تکراری `inheritOrOverride`، (۴) رفع تضاد Rule «Subject» بین واحد ۰۵ و ۰۷. هیچ منطق Rule ای عوض نشد به‌جز Migration ۴ که خودش یک تغییر منطق عمدی و تأییدشده بود.

---

## Migration ۱: واحدهای ۰۱/۰۲/۰۶ → ValidationIssue/Severity سراسری

### فایل‌های تغییریافته
- `domain/story/StoryValidation.kt` — `ValidationIssue`/`ValidationResult` محلی حذف شد؛ `validateStoryContext` اکنون `List<ValidationIssue>` برمی‌گرداند (خالی = Valid).
- `domain/dna/DnaValidation.kt` — `sealed class ValidationResult` محلی حذف شد؛ `validateShotAgainstDna`، `validateShotDuration`، `validateShotAspectRatio`، `checkMandatoryElementsPresent` اکنون `ValidationIssue?` برمی‌گردانند (`null` = Valid).
- `domain/asset/AssetValidation.kt` — همان الگو برای `validateAssetIdUniqueness`، `validateAssetDeletion`، `validateDefaultOutfitExists`، `validateReferenceImageFile`، `checkSimilarAssetName`.

### نگاشت Valid → معادل سراسری
سه تابع محلی قبلی، حالت «Valid» جداگانه‌ای در enum خودشان داشتند (`ValidationResult.Valid`/`valid: Boolean = true`). نوع سراسری واحد ۰۷ اصلاً چنین حالتی ندارد — فقط `BLOCKING`/`WARNING`. دو الگوی نگاشت استفاده شد:
- **توابع تک‌نتیجه‌ای** (واحد ۰۲ و ۰۶، هرکدام حداکثر یک مشکل در هر فراخوانی): `ValidationResult.Valid` → `null` (خروجی `ValidationIssue?`). این دقیقاً همان الگویی است که واحدهای ۰۳/۰۸/۰۹ از ابتدا استفاده کرده بودند.
- **تابع چندنتیجه‌ای** (واحد ۰۱، `validateStoryContext` که می‌تواند هم‌زمان چند خطا/هشدار داشته باشد): `valid=true, errors=[]` → لیست خالی (خروجی `List<ValidationIssue>`)، دقیقاً مثل الگوی موجود `validateDataCompleteness` در واحد ۰۷.

### چرا نه `ValidationReport`؟
عنوان Migration ۱ به «ValidationReport سراسری» اشاره داشت، اما `ValidationReport` به یک `targetId: String` نیاز دارد که `StoryContext` (و بقیه‌ی انواع مرتبط) فاقد آن هستند. بررسی نشان داد **هیچ‌کدام** از توابع Rule-level موجود در پروژه (واحدهای ۰۳/۰۵/۰۷/۰۸/۰۹) از `ValidationReport` استفاده نمی‌کنند — همه `ValidationIssue?` یا `List<ValidationIssue>` برمی‌گردانند. `ValidationReport` احتمالاً برای یک نقطه‌ی تجمیع سطح بالاتر (مثلاً Prompt Finalization) در نظر گرفته شده، نه توابع Rule تکی. طبق همین الگوی مستقر، سه واحد هم به `ValidationIssue?`/`List<ValidationIssue>` migrate شدند، نه `ValidationReport`.

### استثنای تأییدشده: `UpdateResult` (Hard Lock واحد ۰۶) دست‌نخورده ماند
`domain/asset/AssetContinuity.kt` (Hard Lock، `UpdateResult`: Allowed/Blocked) عمداً از این Migration مستثنا شد — طبق تأیید صریح کاربر. `ValidationIssue` یک فیلد `severity: Severity` قابل‌تغییر دارد؛ اگر Hard Lock هم از این نوع استفاده می‌کرد، تضمین سطح Type System «این قوانین هرگز فقط Warning نیستند» (ADR-003 §۶) از بین می‌رفت. `UpdateResult` بدون تغییر باقی ماند.

### استثنای دیگر (بدون نیاز به تأیید): `RuleSeverity` در `domain/story/HumanOverride.kt`
`RuleSeverity` (BLOCKING/WARNING) در `HumanOverride.kt`/`OverrideActions.kt` برای مدل مجوز Override (`checkOverridePermission`, `createOverride`) استفاده می‌شود — یک مفهوم متفاوت از «severity یک مشکل اعتبارسنجی». دستور کار صریحاً فقط `StoryValidation.kt` را برای این واحد نام برده بود؛ `RuleSeverity` دست‌نخورده ماند.

---

## Migration ۲: واحد ۰۷ → CinematicMode/MotionLevel واقعی

### فایل تغییریافته
`domain/validation/LogicConflictChecker.kt` — `checkFastMotionLongTake(motionLevel: String, cinematicMode: String)` به `checkFastMotionLongTake(motionLevel: MotionLevel, cinematicMode: CinematicMode)` تغییر کرد (import از `domain.shot.MotionLevel` و `domain.visualidentity.CinematicMode`).

### `checkStaticCameraInChase` — بدون تغییر (بررسی‌شده، تصمیم مستقل)
بررسی شد که آیا `cameraMovementType: String` معادل واقعی در `domain.camera` دارد. نتیجه: نه enum پایه‌ی `BasicMovementType` (فقط حرکات پایه، نه Orbit/DronePath/...) و نه `CameraMovement` (sealed class، نیازمند Pattern Matching اضافه) تناظر تمیزی مثل MotionLevel/CinematicMode ندارند؛ `shotDescription` (پارامتر دوم) هم متن آزاد است، نه enum. تبدیل فقط یک پارامتر به enum و نگه‌داشتن دیگری String پیچیدگی بدون سود واقعی اضافه می‌کرد — بدون تغییر ماند.

### نکته‌ی معماری قابل‌توجه: وابستگی چرخه‌ای بین پکیج‌ها
این Migration باعث شد `domain.validation` هم به `domain.shot` و هم به `domain.visualidentity` وابسته شود؛ `domain.shot` از قبل (برای `validateBeatSheetTimeline`) به `domain.validation` وابسته بود. یعنی اکنون یک چرخه‌ی وابستگی پکیجی واقعی وجود دارد: `domain.validation ↔ domain.shot`. این در Kotlin/JVM مشکل کامپایل ایجاد نمی‌کند (همه در یک ماژول Gradle‌اند، بدون مرز اجرایی بین پکیج‌ها) — build با موفقیت انجام شد — اما از منظر مرزهای معماری، این یک وابستگی دوطرفه‌ی آگاهانه است که طبق دستور کار صریح پذیرفته شد.

---

## Migration ۳: حذف نسخه‌ی تکراری `inheritOrOverride`

### تغییرات
- `domain/shot/ShotInheritance.kt` **حذف شد** (نه فقط ویرایش) — هیچ فایل تولیدی دیگری در `domain/shot` از این تابع محلی استفاده نمی‌کرد (بررسی‌شده با grep، فقط تست خودش آن را صدا می‌زد).
- `app/src/test/.../domain/shot/ShotInheritanceTest.kt` **حذف شد** — هر سه تست آن (با/بدون shotValue، نوع Generic غیر-String) دقیقاً معادلش در `domain/scene/SceneInheritanceTest.kt` (صاحب اصلی) از قبل وجود داشت؛ نگه‌داشتن یک کپی تکراری از همان تست بی‌فایده بود.
- هیچ import یا فایل دیگری نیاز به تغییر نداشت (هیچ مصرف‌کننده‌ی دیگری وجود نداشت).

نتیجه: ۳ تست از مجموع کل کم شد (۱۹۳ → ۱۹۰ بعد از این Migration به‌تنهایی).

---

## Migration ۴: رفع تضاد Rule «Subject» بین واحد ۰۵ و ۰۷

### تغییر
`domain/validation/ValidationEngine.kt`، تابع `validateDataCompleteness`:
- پارامتر `locationIds: List<String>` اضافه شد.
- شرط Rule 2 اکنون هر سه دسته (`characterIds`/`objectIds`/`locationIds`) را بررسی می‌کند.
- Severity از `WARNING` به `BLOCKING` تغییر کرد.
- پیام خطا با پیام `validateShotHasSubject` واحد ۰۵ یکسان شد.

این تنها Migration در این قدم است که **منطق Rule واقعی** را تغییر داد — عمدی و تأییدشده، نه یک انحراف کشف‌شده.

### آیا `validateDataCompleteness` و `validateShotHasSubject` اکنون کاملاً یکسانند؟
**نه کاملاً، اما بخش Subject‌شان بله.** `validateDataCompleteness` دو Rule را در یک تابع لیست-محور ترکیب می‌کند (Rule 1: طول توضیح + Rule 2: Subject)، در حالی‌که واحد ۰۵ این دو را به دو تابع مستقل `ValidationIssue?`-محور تفکیک کرده (`validateShotDescription` و `validateShotHasSubject`). اما شرط و Severity بخش Subject (Rule 2) اکنون بایت‌به‌بایت یکسان است.

**پیشنهاد (اجرا نشده، منتظر تصمیم معمار):** یکی از این دو مسیر:
1. منطق مشترک Rule 2 به یک تابع واحد (مثلاً در `domain.validation`) استخراج شود و هر دو واحد آن را صدا بزنند (مثل الگوی Rule 3/`validateBeatSheetTimeline` که از قبل به همین شکل به اشتراک گذاشته شده).
2. یا این دو تابع به‌عمد جدا نگه داشته شوند، چون در دو واحد مختلف با دلایل مستقل (Level 1/2 عمومی در برابر Rule های اختصاصی Shot) استفاده می‌شوند و افزونگی جزئی هزینه‌ی قابل‌قبولی برای استقلال دو واحد است.

این تصمیم اجرا نشد؛ ADR فقط سؤال را مستند می‌کند.

---

## خلاصه‌ی شمارش تست (تأییدشده بعد از هر Migration، جدا)

| مرحله | جمع تست | تغییر | توضیح |
|---|---|---|---|
| قبل از این قدم | 193 | — | — |
| بعد از Migration ۱ | 193 | ۰ | فقط نوع بازگشتی عوض شد، نه تعداد تست |
| بعد از Migration ۲ | 193 | ۰ | فقط نوع پارامتر عوض شد |
| بعد از Migration ۳ | 190 | −۳ | حذف `ShotInheritanceTest.kt` (۳ تست تکراری) |
| بعد از Migration ۴ (نهایی) | 191 | +۱ | یک تست جدید (`location-only subject`) اضافه شد |

## Consequences

- **آسان می‌شود:** یک نوع ValidationIssue واحد در سراسر ۶ از ۹ واحد پیاده‌شده؛ Logic Conflict Checker اکنون Type-Safe برای Motion/Cinematic؛ فقط یک نسخه از `inheritOrOverride`؛ هیچ تضاد Rule ای بین واحد ۰۵ و ۰۷ باقی نمانده.
- **بدهی باقی‌مانده (جدید یا هنوز باز):** وابستگی چرخه‌ای پکیجی `domain.validation ↔ domain.shot` (Migration ۲، مستند شده، نه رفع‌شده)؛ افزونگی جزئی بین `validateDataCompleteness`/`validateShotHasSubject` (Migration ۴، پیشنهاد باز)؛ `UpdateResult` (Hard Lock) و `ImageValidationResult` هنوز محلی‌اند (عمدی)؛ همپوشانی `AmbientSoundSuggestion`/`AmbientSound` (ADR-009) و `canDeleteAsset`/`validateAssetDeletion` (ADR-004) هنوز حل‌نشده.
