# ADR-027: Migration واحد ۰۲ (DNA Manager) به بلوپرینت نسخه ۵ — بزرگ‌ترین Breaking Change تا کنون

**تاریخ:** 2026-07-28
**وضعیت:** Migration کامل شد. جزئیات کامل ترتیب اجرا، تناقض‌های واقعی کشف‌شده، و تصمیمات نهایی در ادامه.

## Context

`docs/blueprints/02-dna-manager-v2.md` (نسخه ۵) بزرگ‌ترین Breaking Change این بلوپرینت تا کنون را معرفی می‌کند: `VisualStyle` از ۴ مقدار کلی به ۳۴ مقدار دقیق در ۵ دسته؛ `Mood` و `LightingStyle` که تا این نسخه اصلاً enum مستقل نبودند (به‌ترتیب String خام در `domain.story`/رشته‌های خام در `domain.sceneconditions`، یا enum محلی ۸/۶ مقداری) اکنون enum کامل با ۲۵/۲۲ مقدار در `domain.dna` می‌شوند؛ `ContrastLevel` جدید (رفع باگ واقعی)؛ `AspectRatio` جدید (Breaking Change واقعی از String)؛ `QualityDirectives` کاملاً جدید.

## پیش‌شرط: بررسی نصب اپ روی دستگاه/امولاتور

این Session یک محیط ابری/Headless است — هیچ دستگاه یا امولاتور واقعی با اپ نصب‌شده در این Session وجود ندارد. اقدام Uninstall درخواستی از قبل موضوعیت نداشت.

## دو تناقض واقعی کشف‌شده و حل‌شده (توسط معمار، نه به‌صورت خودسرانه)

### تناقض ۱: ProjectDna — stylePreferences/overrideRules

بخش «ساختار داده» (نمونه‌ی JSON) بلوپرینت ۰۲ نسخه ۵ همچنان `style_preferences` و `override_rules` را دارد؛ اما `data class ProjectDna` در بخش «پیاده‌سازی مفهومی (Kotlin)» و ردیف `ProjectDna` در `type-registry.md` هر دو این دو فیلد را کاملاً حذف کرده بودند. کد فعلی هر دو فیلد را داشت؛ `DnaValidationTest.kt` مستقیماً `overrideRules` را تست می‌کرد (`requiresApprovalForOverride`) و `stylePreferences` را در هر `sampleDna()` می‌ساخت. حذف این دو فیلد به‌معنای نقض صریح خودِ همین بلوپرینت («تمام تست‌های موجود DnaValidationTest.kt باید بدون تغییر pass شوند») بود.

**تصمیم معمار:** حذف دقیقاً طبق Kotlin/type-registry. `stylePreferences`/`overrideRules` کاملاً از `ProjectDna` حذف شدند؛ `requiresApprovalForOverride` و دو تست مرتبطش هم حذف شدند — به‌عنوان یک تغییر مستقل و مستند (نه انحراف خودسرانه‌ی من).

### تناقض ۲: نگاشت نام‌های LightingStyle قدیمی که در enum جدید وجود ندارند

`LightingStyle` جدید (۲۲ مقدار) هیچ مقداری دقیقاً به نام `NOIR` ندارد؛ `DRAMATIC` هم نیست (`DRAMATIC_LIGHT` هست)؛ `NATURAL` هم نیست (`NATURAL_LIGHT` هست). این سه مستقیماً در منطق Rule واقعی `LightingValidation.kt` استفاده شده بودند (`checkNoirWithoutHighContrast`, `checkStrongFillWithDramaticStyle`) — نه فقط داده‌ی تست، پس نمی‌شد خودسرانه حدس زد.

**تصمیم معمار:** `NOIR→LOW_KEY` (نزدیک‌ترین مفهومی، دسته‌ی STUDIO)، `DRAMATIC→DRAMATIC_LIGHT`، `NATURAL→NATURAL_LIGHT`. منطق Rule (چه‌وقت هشدار بدهد) عیناً دست‌نخورده ماند — فقط نام enum عوض شد.

## یک خودتصحیحی (بدون توقف، چون طبق ADR موجود قابل‌حل بود)

کد مفهومی بلوپرینت برای `validateColorPalette` از یک `sealed class ValidationResult` محلی (Valid/Warning/Blocking) استفاده می‌کند. اما `docs/adr/010-cross-unit-migrations.md` قبلاً (Migration واقعی و ثبت‌شده) دقیقاً همین نوع را از `domain/dna/DnaValidation.kt` حذف کرده بود و هر تابع این فایل را به `ValidationIssue?` سراسری واحد ۰۷ مهاجرت داده بود. بازتعریف `ValidationResult` یعنی نقض بی‌صدای یک Migration واقعی قبلی. **تصمیم:** `ValidationResult` بازتعریف نشد؛ `validateColorPalette` هم مثل هر تابع دیگر این فایل `ValidationIssue?` برمی‌گرداند (`null`=Valid, `WARNING`, `BLOCKING`) — منطق دقیقاً طبق بلوپرینت، فقط نوع بازگشتی با الگوی موجود هماهنگ شد. این خودم قبل از تحویل فایل اول تشخیص دادم و اصلاح کردم (نه بعد از توقف/پرسش، چون ADR-010 از قبل این تصمیم را برای این واحد نهایی کرده بود).

## فایل‌های تغییریافته (ترتیب اجرا)

### ۱. `domain/dna/ProjectDna.kt` (بازنویسی کامل)
- `VisualStyleCategory`(۵)/`VisualStyle`(۳۴)، `MoodCategory`(۵)/`Mood`(۲۵)، `LightingCategory`(۴)/`LightingStyle`(۲۲) — کپی مستقیم از بلوپرینت، بدون اختراع؛ اعداد با `type-registry.md` تطبیق داده شدند (دقیقاً یکسان).
- `ContrastLevel` جدید — `MasterPalette.globalContrast` که با grep تأیید شد اشتباهاً `SaturationLevel` بود (باگ واقعی، مطابق ادعای بلوپرینت)، اصلاح شد.
- `AspectRatio` جدید (۱۱ مقدار) — `OutputConstraints.aspectRatio` از `String` به این enum تغییر کرد (Breaking Change واقعی).
- `MasterPalette.colorPalette`, `QualityDirectives`, `GlobalMoodBase` (کامل، `primaryEmotion: Mood`)، `LightingPreference` — همه جدید/کامل‌شده طبق بلوپرینت.
- `GlobalMoodBase.intensity` عمداً `String = "medium"` ماند (نه `IntensityLevel` قدیمی) — طبق تصریح متن بلوپرینت («رشته‌ی ساده، چون فقط سه سطح دارد»)؛ `IntensityLevel` enum قدیمی به همین دلیل حذف شد (هیچ مصرف‌کننده‌ی دیگری نداشت، تأیید با grep).
- `stylePreferences`/`overrideRules`/`ColorPhilosophy`/`CameraPreferences`/`LightingPreferences`(قدیمی)/`StylePreferences`/`OverrideRules` کاملاً حذف شدند (تناقض ۱).

### ۲. `domain/dna/DnaValidation.kt` (بازنویسی)
- `updateCoreIdentity`, `validateShotAgainstDna`, `validateShotDuration`, `checkMandatoryElementsPresent` بدون تغییر منطق.
- `validateShotAspectRatio` اکنون `AspectRatio` می‌گیرد، نه `String`.
- `requiresApprovalForOverride` حذف شد (تناقض ۱).
- `validateColorPalette` اضافه شد (خودتصحیحی بالا).

### ۳. `domain/dna/DnaValidationTest.kt` (بازنویسی)
- `sampleDna()` با شکل جدید؛ ۲ تست `requiresApprovalForOverride` حذف (مستند، نه فراموشی)؛ ۲ تست `validateShotAspectRatio` به‌روزرسانی؛ ۴ تست `validateColorPalette` اضافه.

### ۴-۵. `domain/story/StoryContext.kt` + `StoryValidation.kt`
- `Mood` محلی (۸ مقدار) حذف؛ `import domain.dna.Mood`. هر ۸ مقدار قدیمی (`CALM, DARK, EPIC, EMOTIONAL, MYSTERIOUS, TENSE, HOPEFUL, MELANCHOLIC`) عیناً در enum جدید هم موجودند (تأیید دستی، یک‌به‌یک) — بدون نیاز به نگاشت نام.

### ۶-۷. `domain/sceneconditions/LightingModels.kt` + `LightingValidation.kt`
- `LightingStyle` محلی (۶ مقدار) حذف؛ `import domain.dna.LightingStyle`. `SOFT`/`HARD`/`CINEMATIC` (۳ مقدار قدیمی) هیچ مصرف‌کننده‌ای در کل پروژه نداشتند (تأیید با grep) — نیازی به تصمیم نگاشت برایشان نبود. `NOIR`/`DRAMATIC` در `LightingValidation.kt` طبق تناقض ۲ نگاشت شدند.

### ۸. `data/repository/ProjectDnaDto.kt` + `DnaAssetMappers.kt` (واحد ۱۵ — مصرف‌کننده‌ی مستقیم، بدون اصلاحش کامپایل نمی‌شد)
- `StylePreferencesDto`/`ColorPhilosophyDto`/`CameraPreferencesDto`/`LightingPreferencesDto`/`OverrideRulesDto` حذف؛ `LightingPreferenceDto`/`QualityDirectivesDto`/`MasterPaletteDto.colorPalette` اضافه.
- `globalContrast`/`aspectRatio`/`primaryEmotion` همچنان به‌صورت رشته‌ی نام Enum ذخیره می‌شوند (شکل JSON عوض نشد، فقط نوع Kotlin سمت domain).
- `DtoMappers.kt` (فایل جداگانه‌ی واحد ۱۵، برای `Shot.lighting`) هم `import domain.sceneconditions.LightingStyle` قدیمی داشت — با grep پیدا و به `domain.dna.LightingStyle` اصلاح شد؛ منطق `.valueOf`/`.name` متقارن و بدون تغییر رفتار.

### ۹. تست‌های مصرف‌کننده
`ProjectDnaRepositoryTest.kt`, `PromptGenerationRepositoryTest.kt`, `PromptAssemblyTest.kt`, `ShotSettingsResolutionTest.kt`, `LightingValidationTest.kt`, `StoryValidationTest.kt` — همگی برای کامپایل با انواع جدید به‌روزرسانی شدند. مقادیر نمونه‌ی بی‌اهمیت (`VisualStyle.CINEMATIC`→`CINEMATIC_STYLE`، `VisualStyle.STYLIZED`→`PHOTOREALISTIC`) صرفاً داده‌ی تست دلخواه بودند، بدون وابستگی منطقی — انتخاب مستقیم من، بدون نیاز به تأیید معمار.

**یافته‌ی فرعی تأییدشده:** `PromptAssemblyTest.kt` دو Assertion دارد (`parts.lightingSpecs.contains("DRAMATIC")`, `parts.styleModifiers.contains("CINEMATIC")`) که روی زیررشته کار می‌کنند — چون `"DRAMATIC_LIGHT".contains("DRAMATIC")` و `"CINEMATIC_STYLE".contains("CINEMATIC")` هر دو `true` هستند، این دو Assertion بدون تغییر و بدون خطر Pass ماندند؛ تأیید شد با خواندن منبع `PromptAssembly.kt` (که از `"${enum}"` ساده استفاده می‌کند، نه فرمت دیگر).

## خارج از Scope این قدم (طبق تصریح معمار — دست‌نخورده ماندند)

- **واحد ۰۳** (`domain/visualidentity/`, `getPacingFromEmotion`) — هنوز پارامتر `String` می‌گیرد، نه `Mood`. طبق `type-registry.md` باید به `Mood` وصل شود؛ Migration جداگانه.
- **واحد ۰۸** (`domain/sceneconditions/LightingMoodMapping.kt`, `mapMoodToLighting`) — هنوز `mood: String` می‌گیرد و `LightingPreset` (رشته‌ای) برمی‌گرداند، نه `LightingStyle`. Migration جداگانه.
- **`docs/reference/universal-technical-variables.md`** — دست‌نخورده ماند (طبق تصریح معمار).

## تست

هر ۹ فایل کد اصلی + ۷ فایل تست تغییریافته با `gradle :app:testDebugUnitTest :app:assembleDebug` واقعی اجرا شدند — نتیجه‌ی دقیق در گزارش نهایی همین قدم (شمارش مستقیم فایل‌به‌فایل XML، نه از حافظه).

## Consequences

- **آسان می‌شود:** `Mood`/`LightingStyle`/`VisualStyle` اکنون enum های کامل و صحیح در یک محل واحد (`domain/dna/`) هستند؛ واحدهای ۰۳/۰۸ در Migration های بعدی می‌توانند مستقیماً به این‌ها وصل شوند بدون نیاز به تعریف موازی.
- **بدهی مستند باقی‌مانده:** واحد ۰۳/۰۸ هنوز به این enum ها وصل نشده‌اند (بالا)؛ `universal-technical-variables.md` هنوز بازبینی نشده.
