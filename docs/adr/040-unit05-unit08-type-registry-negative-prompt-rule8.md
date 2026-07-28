# ADR-040: اصلاح `type-registry.md` (F7/F8) + Rule 8 برای `negative_prompt_override` (F9)

**تاریخ:** 2026-07-28
**وضعیت:** کامل شد و مستقل قابل‌کامپایل/تست است.

## Context

این قدم سه یافته‌ی کوچک و مستقل ممیزی `docs/audit/pre-unit16-audit.md` را رفع
می‌کند: F7 و F8 (هر دو مستندسازی صرف در `type-registry.md`) و F9 (یک Rule
اعتبارسنجی جدید، پیاده‌سازی‌نشده).

## بخش الف — F7: ردیف `LightingSettings`

جدول قبلاً `motivation` (نام اشتباه) داشت و فاقد `lightSourceCount` بود. با grep
در `domain/sceneconditions/LightingModels.kt` تأیید شد ترتیب و نام واقعی ۸ فیلد:
`style, keyLightPosition, contrastRatio, fillLight, colorTemperature,
shadowQuality, lightSourceCount, lightingMotivation` است. ردیف دقیقاً با همین
ترتیب/نام بازنویسی شد.

## بخش ب — F8: ردیف `EnvironmentSettings`

جدول قبلاً `locationType` را فهرست کرده بود — نامی که هیچ‌جا در
`EnvironmentSettings` واقعی وجود ندارد (احتمالاً اشتباهاً از `domain.scene.LocationType`
که مفهوم کاملاً بی‌ربطی است، گرفته شده بود) — و فاقد `windStrength`/
`environmentalMotion` بود. با grep در `domain/sceneconditions/EnvironmentModels.kt`
تأیید شد ترتیب/نام واقعی ۷ فیلد: `weatherType, weatherIntensity, windStrength,
groundState, visibility, temperatureFeel, environmentalMotion` است. ردیف اصلاح شد.

هر دو ردیف با `grep -A9`/`grep -A8` مستقیم روی خودِ `data class` نهایی تأیید شدند
که اکنون کلمه‌به‌کلمه و به همان ترتیب مطابقت دارند.

## بخش ج — F9: Rule 8 (`validateNegativePromptOverride`)

طبق `docs/blueprints/05-shot-engine-v2.md:272`، `negative_prompt_override` (اگر
`null` نباشد) نباید کاملاً بی‌معنی/whitespace-only باشد — Severity=WARNING (نه
BLOCKING، چون این یک تصمیم سبکی کاربر است، نه یک الزام ساختاری).
`validateNegativePromptOverride(shot: Shot): ValidationIssue?` دقیقاً هم‌الگو با
۴ Rule موجود `ShotValidation.kt` اضافه شد: `null` ⇒ معتبر (یعنی از DNA پروژه ارث
می‌برد، طبق `resolveNegativePrompt` موجود واحد ۰۵)؛ رشته‌ی غیر-`null` اما
`isBlank()` ⇒ Warning؛ در غیر این صورت معتبر.

### یافته‌ی سیم‌کشی (مستند، نه رفع‌شده در این قدم)

با grep تأیید شد `ShotValidation.kt` هیچ تابع تجمیع‌کننده‌ی سطح‌بالا
(`validateShot(shot): List<ValidationIssue>`) ندارد — هر ۴ Rule موجود
(`validateShotDescription`/`validateShotHasSubject`/`validateShotBeatTimeline`/
`validateImageReferenceFile`) مستقل و جداگانه در جای دیگری (احتمالاً لایه‌ی UI/
ViewModel واحد ۱۶ که هنوز ساخته نشده) فراخوانی می‌شوند، نه از یک نقطه‌ی مرکزی در
همین فایل. طبق دستور کار، Rule 8 هم به همین سبک مستقل اضافه شد؛ سیم‌کشی واقعی‌اش
به جریان Validation (مثلاً هنگام ذخیره‌ی شات در Shot Composer واحد ۱۶) کار آینده
است — دقیقاً همان الگوی سیم‌کشی که ۴ Rule موجود هم هنوز منتظرش هستند.

## تست

- `ShotValidationTest.kt` (۳ تست جدید): `null` ⇒ معتبر؛ رشته‌ی معنادار ⇒ معتبر؛
  رشته‌ی فقط‌فاصله ⇒ Warning.

## Consequences

- **آسان می‌شود:** `type-registry.md` اکنون منبع مرجع دقیق و قابل‌اعتماد برای هر
  دو `data class` است؛ فرم‌های آینده‌ی واحد ۱۶ (Tab «نور و محیط») می‌توانند مستقیماً
  از این جدول برای فهرست فیلدها استفاده کنند بدون نیاز به grep مجدد کد.
- **بدون بدهی جدید شناخته‌شده:** Rule 8 مستند و تست‌شده است؛ سیم‌کشی‌نشدنش به یک
  جریان Validation مرکزی، دقیقاً هم‌سطح بدهی شناخته‌شده‌ی ۴ Rule موجود این فایل
  است — نه یک بدهی جدید یا پنهان.
