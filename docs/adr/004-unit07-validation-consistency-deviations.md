# ADR-004: تصمیمات پیاده‌سازی واحد ۰۷ (Validation & Consistency Engine) — لایه‌ی دامنه

**تاریخ:** 2026-07-19
**وضعیت:** بخشی تأییدشده (توسط کاربر در جریان این قدم)، بخشی سؤال باز برای معمار (بخش «سؤال باز»)

## Context

پیاده‌سازی لایه‌ی دامنه‌ی خالص واحد ۰۷ (Validation Engine + Logic Conflict Checker + Dependency Resolver) طبق `docs/blueprints/07-validation-and-consistency.md`.

## تصمیمات تأییدشده

### ۱. CinematicMode/MotionLevel → پارامتر String (تأییدشده توسط کاربر)
**مشکل:** `checkFastMotionLongTake` به `MotionLevel` و `CinematicMode` نیاز دارد. هر دو enum دقیقاً در بلوپرینت واحدهای دیگر تعریف شده‌اند — `CinematicMode` در بلوپرینت ۰۳ (Cinematic Language، مقادیر LONG_TAKE/FAST_CUT/BALANCED) و `MotionLevel` در بلوپرینت ۰۵ (Shot Engine، مقادیر STATIC/SUBTLE/MODERATE/DYNAMIC/EXTREME) — اما هیچ‌کدام هنوز در کد پیاده نشده‌اند.
**گزینه‌ها:** (الف) تعریف موقت این دو enum در واحد ۰۷؛ (ب) بازنویسی امضا با پارامتر `String`؛ (ج) تأخیر کل تابع.
**تصمیم (تأییدشده):** گزینه‌ی ب — `checkFastMotionLongTake(motionLevel: String, cinematicMode: String)`. مقادیر منتظره مستند شده‌اند (`"static"|"subtle"|"moderate"|"dynamic"|"extreme"` و `"long_take"|"fast_cut"|"balanced"`).
**چرا:** گزینه‌ی الف دو enum موقت می‌ساخت که وقتی واحدهای ۰۳/۰۵ واقعاً پیاده شوند، باید حذف/یکی‌سازی شوند — ریسک دوباره‌کاری و ناهماهنگی. String از این مشکل اجتناب می‌کند، هرچند ایمنی نوع را در ازای آن از دست می‌دهد (پذیرفته‌شده تا اتصال واقعی به واحد ۰۳/۰۵).

### ۲. توابع بخش الف با پارامترهای مستقیم به‌جای Shot/EnvironmentSettings/LightingSettings/Beat
همان اصل تصمیم ۱ به‌طور یکسان روی بخش الف اعمال شد (این بخش پیش از پرسش از کاربر مطرح نشده بود، اما مستقیماً از همان تصمیم تأییدشده تبعیت می‌کند، نه یک انتخاب مستقل جدید):
- `validateDataCompleteness(shotDescription: String, characterIds: List<String>, objectIds: List<String>)` — به‌جای `Shot` (واحد ۰۵).
- `validateLogicConsistency(weather: String, lightingMotivation: String, locationType: String)` — به‌جای `Shot`/`EnvironmentSettings`/`LightingSettings` (واحدهای ۰۵/۰۸)؛ توجه: پارامتر `shot` در کد مفهومی بلوپرینت اصلاً در بدنه‌ی تابع استفاده نمی‌شد.
- `validateBeatSheetTimeline(beatTimestampsSeconds: List<Float>, durationSeconds: Float)` — به‌جای `List<Beat>` (واحد ۰۵).

### ۳. transitivelyAffected خالی می‌ماند (طبق دستور کار صریح)
`analyzeImpact` دقیقاً طبق کد مفهومی بلوپرینت پیاده شد — سطح مستقیم گراف، `transitivelyAffected` همیشه لیست خالی. BFS چندسطحی واقعی پیاده نشد، چون دستور کار صراحتاً همین رفتار را برای این قدم خواسته بود.

### ۴. توابع non-suspend
مشابه ADR-001 §۵، ADR-002 §۵، ADR-003 §۱۰.

## سؤال باز برای معمار (تصمیم این قدم نیست — فقط طرح سؤال طبق دستور کار)

### آیا Migration واحدهای ۰۱/۰۲/۰۶ به ValidationResult سراسری واحد ۰۷ باید قدم اجرایی جداگانه‌ی بعدی باشد؟

اکنون **چهار** نوع «نتیجه‌ی اعتبارسنجی» محلی مختلف در پروژه وجود دارد:
- واحد ۰۱: `ValidationResult` (`data class` با لیست `errors`/`warnings`)
- واحد ۰۲: `ValidationResult` (`sealed class` تک‌نتیجه‌ای Valid/Warning/Blocking)
- واحد ۰۶: `ValidationResult` (همان شکل sealed واحد ۰۲، پکیج مجزا) + `UpdateResult` (Allowed/Blocked، مخصوص Hard Lock — این یکی عمداً نباید با severity عمومی یکی شود)
- واحد ۰۷ (این قدم): `Severity`/`ValidationIssue`/`ValidationReport` — نوع سراسری که سه بلوپرینت قبلی به آن اشاره کرده بودند.

هم‌چنین یک همپوشانی مفهومی دیگر کشف شد: `canDeleteAsset(assetId, usageCount): Result<Unit>` در همین بلوپرینت ۰۷ عملاً همان بررسی `validateAssetDeletion` (واحد ۰۶، Rule 3) را با امضا و نوع بازگشتی متفاوت انجام می‌دهد.

**این قدم تصمیم نگرفت و migration/یکی‌سازی را اجرا نکرد** (طبق دستور کار صریح). سؤال برای معمار: آیا یک قدم اجرایی جداگانه برای Refactor واحدهای ۰۱/۰۲/۰۶ به سمت `ValidationIssue`/`ValidationReport` سراسری (و یکی‌سازی `canDeleteAsset`/`validateAssetDeletion`) در برنامه قرار بگیرد، یا این چهار نوع محلی فعلاً به همین شکل باقی بمانند تا واحدهای مصرف‌کننده‌ی واقعی (۰۴/۰۵/۰۷ در لایه‌ی بالاتر) به‌طور طبیعی این نیاز را روشن کنند؟

## Consequences

- **آسان می‌شود:** Logic Conflict Checker و Dependency Resolver بدون وابستگی زودهنگام به واحدهای پیاده‌نشده کار می‌کنند؛ تست‌پذیری کامل.
- **بدهی ثبت‌شده:** رشته‌های خام (`String`) به‌جای enum در بخش ب، تا اتصال واقعی واحد ۰۳/۰۵؛ چهار نوع ValidationResult محلی منتظر تصمیم معمار برای یکی‌سازی؛ همپوشانی `canDeleteAsset`/`validateAssetDeletion` حل‌نشده باقی مانده.
