# ADR-009: تصمیمات پیاده‌سازی واحد ۰۸ (Scene Conditions) — لایه‌ی دامنه

**تاریخ:** 2026-07-19
**وضعیت:** تمام تصمیمات مستقل، بدون سؤال باز جدید نیازمند توقف

## Context

پیاده‌سازی لایه‌ی دامنه‌ی خالص واحد ۰۸ (Lighting System + Environment & Weather Engine) طبق `docs/blueprints/08-scene-conditions.md`. هر دو جدول کامل قوانین مستقیماً از فایل استخراج شد: بخش الف ۵ Rule، بخش ب ۸ Rule.

## تصمیمات مستقل (بدون نیاز به توقف/پرسش — همه از الگوهای تثبیت‌شده در واحدهای قبلی پیروی می‌کنند)

### ۱. همه‌ی پارامترهای Lighting/Environment به‌صورت enum
هر ۹ پارامتر بخش الف و هر ۷ پارامتر بخش ب گزینه‌های ثابت و محدود دارند (به‌جز `rim_light` که Boolean است) — طبق راهنمای صریح دستور کار، همه enum شدند.

### ۲. `ColorTemperature` محلی، نه بازاستفاده از واحد ۰۲
`domain.dna.ColorTemperature` مقادیر `{WARM, COOL, NEUTRAL}` دارد؛ بلوپرینت ۰۸ مقادیر `{Warm, Neutral, Cold, Mixed}` می‌خواهد — واژگان یکسان نیست («Cold» به‌جای «Cool»، به‌علاوه‌ی «Mixed»). یک enum محلی جدید در `domain.sceneconditions` تعریف شد؛ بازاستفاده از نوع واحد ۰۲ اشتباه بود چون مقادیرش می‌گفتند COOL نه COLD.

### ۳. `LightingPreset` دقیقاً با فیلدهای String (نه enum) — طبق کد مفهومی بلوپرینت
هرچند سایر پارامترهای Lighting به enum تبدیل شدند، `LightingPreset` (خروجی `mapMoodToLighting`) دقیقاً همان‌طور که بلوپرینت با فیلدهای `String` (مقادیر snake_case مثل `"hard_shadows"`) تعریف کرده، بدون تغییر ماند — این دو نوع (تنظیمات فعلی enum-محور در برابر خروجی Preset رشته‌ای) هدف‌های متفاوتی دارند.

### ۴. Rule 1/2 بخش الف از `TimeOfDay` واقعی (واحد ۰۴) استفاده می‌کنند
مشابه الگوی `CharacterAsset` در واحد ۰۵ و `CameraDistance` در واحد ۰۹ — نوع واقعی import شد، نه بازتعریف.

### ۵. Rule «آتش + باران + بیرونی» مستقیماً از `validateLogicConsistency` واحد ۰۷ استفاده می‌کند
طبق بررسی دستوری با grep، امضای واحد ۰۷ (`weather: String, lightingMotivation: String, locationType: String`) با enum های محلی این واحد (`WeatherType`, `LightingMotivation`) کاملاً سازگار بود — با `.name.lowercase()` (مثلاً `RAIN`→`"rain"`, `FIRE`→`"fire"`) دقیقاً همان رشته‌هایی تولید می‌شود که واحد ۰۷ بررسی می‌کند. `locationType` به‌صورت `String` خارجی گرفته شد (نه enum `LocationType` واحد ۰۴ import شود) چون خودِ امضای واحد ۰۷ هم همین‌طور طراحی شده بود — تطابق کامل، بدون نیاز به وابستگی اضافه به `domain.scene`.

### ۶. تفسیر «آب‌وهوای بارانی» = RAIN یا STORM
Rule 2 (`falling_rain` بدون آب‌وهوای بارانی) و Rule 4 (باران + زمین خشک) به «باران» ارجاع می‌دهند. چون خودِ `mapEnvironmentToSound` همین واحد، Storm را معادل «torrential rain and wind» می‌داند (نه پدیده‌ای کاملاً جدا از باران)، هر دو Rule برای `WeatherType.STORM` هم اعمال شدند، نه فقط `RAIN`.

### ۷. `AmbientSoundSuggestion` طبق دستور صریح ساخته شد؛ همپوشانی با `AmbientSound` واحد ۰۵ فقط یادداشت شد
دو نوع از نظر شکل داده‌ای **کاملاً یکسان**اند (هر دو: `type/intensity/description` از جنس `String`) — نه فقط شبیه. بلوپرینت ۰۸ عمداً نام جدیدی تعریف کرده؛ طبق دستور کار، این یکسانی فقط یادداشت شد (نه ادغام/Migration، که تصمیمش به‌عهده‌ی معمار است).

### ۸. توابع non-suspend
مشابه ADR های قبلی.

## سؤال‌های باز تجمیعی (بدون تغییر نسبت به قدم قبلی)

۱. Migration واحدهای ۰۱/۰۲/۰۶ به `ValidationIssue` سراسری؟ (ADR-004)
۲. Migration پارامترهای `String` واحد ۰۷ به enum های واقعی واحدهای ۰۳/۰۵؟ (ADR-004، ADR-006)
۳. آیا نسخه‌ی تکراری `inheritOrOverride` در واحد ۰۵ باید حذف و با import از واحد ۰۴ جایگزین شود؟ (ADR-007)
۴. تضاد حل‌نشده‌ی Rule «Shot بدون Subject» بین واحد ۰۵ و واحد ۰۷؟ (ADR-006)
۵. **[یادداشت جدید، نه سؤال باز جدی]** یکسانی کامل شکل داده‌ای `AmbientSoundSuggestion` (این واحد) و `AmbientSound` (واحد ۰۵) — ادغام احتمالی به تصمیم معمار واگذار شد.

## Consequences

- **آسان می‌شود:** پوشش کامل هر ۱۳ Rule (۵+۸) دو بخش این واحد؛ Rule مشترک با واحد ۰۷ بدون تکرار منطق.
- **بدهی ثبت‌شده:** یکسانی `AmbientSoundSuggestion`/`AmbientSound` (بند ۷) در انتظار تصمیم ادغام؛ `ColorTemperature` این واحد و واحد ۰۲ دو نوع مستقل با واژگان نزدیک اما غیریکسان — منبع بالقوه‌ی سردرگمی برای خواننده‌ی بعدی کد، مستندسازی شد اما رفع نشد.
