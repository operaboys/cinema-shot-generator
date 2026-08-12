# ADR-073: Migration نسخه ۳ واحد ۰۷ — `validateLogicConsistency` به Enum واقعی

## زمینه

طبق بلوپرینت `docs/blueprints/07-validation-and-consistency-v2.md`،
`validateLogicConsistency` (`ValidationEngine.kt`) باید از `WeatherType`
و `LightingMotivation` واقعی (واحد ۰۸) استفاده کند، نه `String` موقتی —
امضای فعلی از زمانی مانده بود که واحدهای ۰۵/۰۸ هنوز پیاده نشده بودند.
فراخوان واقعی، `checkFireInRainOutdoors`
(`domain/sceneconditions/EnvironmentValidation.kt`)، همین enum ها را از
قبل داشت اما با `.name.lowercase()` به String تبدیل می‌کرد — دقیقاً
همان Type-Unsafety پنهانی که این Migration رفع می‌کند.

بازبینی مستقل بخش اول (نسخه ۲) بلوپرینت با `grep`: `validateDataCompleteness`
(خط ۴۶) از قبل `Severity.BLOCKING` و `locationIds` دارد، و
`checkFastMotionLongTake` (`LogicConflictChecker.kt` خط ۱۵) از قبل
`MotionLevel`/`CinematicMode` واقعی می‌گیرد — هر دو قبلاً Migrate شده
بودند (به ترتیب Migration ۴ و ۲، `docs/adr/010`)؛ **هیچ تغییری لازم
نبود.**

## تغییر

- `ValidationEngine.kt`: امضای `validateLogicConsistency` به
  `weatherType: WeatherType, lightingMotivation: LightingMotivation,
  locationType: LocationType` تغییر کرد؛ شرط داخلی به مقایسه‌ی مستقیم
  Enum بازنویسی شد.
- `EnvironmentValidation.kt`: `checkFireInRainOutdoors` دیگر
  `.name.lowercase()` نمی‌کند — enum ها مستقیم پاس داده می‌شوند.
- `ValidationAggregator.kt` (فراخوان واقعی): `scene.location.type.name.lowercase()`
  به `scene.location.type` ساده شد.
- کامنت هدر `ValidationEngine.kt` که به نام فایل بلوپرینت بدون پسوند
  `-v2` ارجاع می‌داد، اصلاح شد.
- `ValidationEngineTest.kt` و `EnvironmentValidationTest.kt`: دو تست هر
  فایل که با String فراخوانی می‌شدند، به فراخوانی Enum واقعی بازنویسی
  شدند (بدون Overload اضافه — طبق بلوپرینت این یک Breaking Change واقعی
  است). `grep` سراسری تأیید کرد هیچ فراخوان دیگری با امضای قدیمی باقی
  نمانده بود.

## تصمیم مستقل: `locationType` هم به Enum تبدیل شد (فراتر از دستور صریح بلوپرینت)

بلوپرینت فقط `weather`/`lightingMotivation` را نام برد و `locationType`
را عمداً String گذاشت (چون متعلق به Scene، واحد ۰۴، است). با `grep`
تأیید شد `Scene` از قبل مفهوم Location را با `SceneLocation(type:
LocationType, ...)` مدل کرده (`domain/scene/SceneModels.kt` خط ۹ و
۱۳) — یک enum واقعی، نه String — و همین enum از قبل جای دیگری
(`checkNoonLightingInIndoor`) مستقیم مصرف می‌شود. فراخوان واقعی همین
تابع (`ValidationAggregator.kt`) دقیقاً همان الگوی
`.name.lowercase()` را برای `locationType` هم داشت. چون این پارامتر
دقیقاً همان Type-Unsafety را داشت که Migration این ADR برای دو پارامتر
دیگر رفع کرد، `locationType` هم به `domain.scene.LocationType` (نه
`domain.asset.LocationType` — دو Enum هم‌نام و مستقل، این یکی مال
Scene است) تبدیل شد. معناشناسی دقیقاً حفظ شد (`LocationType.OUTDOOR`،
نه `MIXED`، هم‌ارز رشته‌ی قبلی `"outdoor"`).

## راستی‌آزمایی

- `gradle :app:compileDebugKotlin` → موفق (بدون Warning/Error).
- `gradle :app:testDebugUnitTest --tests "...domain.validation.*"
  --tests "...domain.sceneconditions.*"` → ۷۶ تست، ۰ شکست.

## Skills استفاده‌شده

`zero-hallucination-coder` (بررسی مستقل هر ادعای اولیه با `grep` روی
کد واقعی پیش از هر تغییر — امضاها، خطوط، و مقادیر Enum).
