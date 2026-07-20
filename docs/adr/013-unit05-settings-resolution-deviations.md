# ADR-013: بستن شکاف SourcedSettings (واحد ۰۵) ↔ انواع واقعی (واحدهای ۰۸/۰۹/۱۱)

**تاریخ:** 2026-07-20
**وضعیت:** دو تصمیم معماری با تأیید صریح معمار + چند تصمیم پیاده‌سازی محلی

## Context

واحد ۱۱ (ADR-012) نشان داد `Shot.camera`/`.lighting`/`.environment` هنوز از نوع `SourcedSettings(source: String, settings: Map<String, String>)` (واحد ۰۵) هستند — یک Placeholder ساخته‌شده در زمانی که واحدهای ۰۸/۰۹ هنوز وجود نداشتند (`docs/adr/006-unit05-shot-engine-deviations.md`, تصمیم ۶). این قدم آن اتصال را می‌بندد: تابع «حل‌کننده»ای که با استفاده از `source` واقعی و `inheritOrOverride` (واحد ۰۴)، مقدار نهایی تایپ‌شده‌ی `CameraSettings`/`LightingSettings`/`EnvironmentSettings` را تولید می‌کند.

## یافته‌های grep پیش از کدنویسی

- `Scene` (واحد ۰۴) هیچ فیلد پیش‌فرض `camera`/`lighting`/`environment` ندارد — فقط `globalVisualStyle` و `constraints` (لیست‌های محدودکننده، نه مقدار). بلوپرینت ۰۴ هم چنین فیلدهایی تعریف نکرده. **نتیجه:** `sceneDefault` باید پارامتر خارجی تزریق‌شده باشد، نه خوانده‌شده از خودِ `Scene` — دقیقاً همان‌طور که در دستور این قدم پیش‌بینی شده بود.
- `Shot.camera.settings: Map<String, String>` بدون schema است. خودِ بلوپرینت ۰۵ در JSON ساختار داده‌اش می‌نویسد: `"settings": { "...": "جزئیات کامل در واحد ۰۹" }` — یعنی از ابتدا این جزئیات را به واحد ۰۹ موکول کرده بود. هیچ Parser یا قرارداد نام‌گذاری کلید از این Map به enum های واقعی (`CameraAngle`, `LightingStyle`, …) در هیچ‌جای پروژه وجود ندارد.

## تصمیم معماری ۱ (پرسیده و تأییدشده): بازتعریف `Shot.camera`/`.lighting`/`.environment` در واحد ۰۵

سه گزینه برای منبع مقدار واقعی override مطرح شد: (الف) پارامتر سوم صریح (`shotOverride`) بدون تغییر `ShotModels.kt`، (ب) Parse از روی Map با قرارداد کلید جدید، (ج) تغییر خودِ `ShotModels.kt` تا `SourcedSettings` نوع واقعی نگه دارد.

**تصمیم معمار: گزینه (ج).**

`SourcedSettings` به `SourcedSettings<T>(source: String = "scene", overrideValue: T? = null)` تغییر کرد (فیلد قبلی `settings: Map<String, String>` حذف شد). `Shot.camera: SourcedSettings<CameraSettings>`, `Shot.lighting: SourcedSettings<LightingSettings>`, `Shot.environment: SourcedSettings<EnvironmentSettings>`. هیچ تست موجودی این فیلدها را با مقدار غیر-پیش‌فرض نمی‌ساخت (بررسی شد)، پس این تغییر نوع هیچ تست از قبل موجودی را نشکست.

## تصمیم معماری ۲ (پرسیده و تأییدشده): مکان `LightingSettings`/`EnvironmentSettings`

تصمیم ۱ یک وابستگی cross-package جدید ایجاد می‌کرد: `domain.shot` باید `LightingSettings`/`EnvironmentSettings` را import کند، اما این دو نوع فعلاً محلیِ `domain.promptengine` (واحد ۱۱، بالادست‌ترین واحد) بودند — یعنی `domain.shot → domain.promptengine`، دقیقاً جهت معکوسِ الگوی معماری پروژه (واحدهای پایه‌ای‌تر نباید به واحدهای بالادست وابسته باشند).

**تصمیم معمار: انتقال `LightingSettings`/`EnvironmentSettings` از `domain.promptengine` به `domain.sceneconditions` (واحد ۰۸).**

این دو نوع اکنون در `LightingModels.kt`/`EnvironmentModels.kt` (واحد ۰۸) تعریف شده‌اند — جایی که enum های تشکیل‌دهنده‌شان (`LightingStyle`, `KeyLightPosition`, `ContrastRatio`, `WeatherType`) از قبل زندگی می‌کنند. `domain.shot` و `domain.promptengine` هر دو از `domain.sceneconditions` import می‌کنند؛ جهت وابستگی درست شد. بررسی شد که `domain.camera`/`domain.sceneconditions` به `domain.shot` وابسته نیستند (فقط یک اشاره‌ی متنی در کامنت `EnvironmentModels.kt`، نه import واقعی) — پس این تغییر هیچ وابستگی چرخه‌ای جدیدی ایجاد نکرد.

**فایل‌های تحت تأثیر خارج از `domain/shot/` (طبق دستور این قدم، صریحاً پرچم‌گذاری می‌شود):**
- `domain/sceneconditions/LightingModels.kt`, `EnvironmentModels.kt` — افزودن دو data class (منتقل‌شده، نه منطق جدید).
- `domain/promptengine/PromptEngineModels.kt` — حذف دو data class محلی، جایگزینی با import از `domain.sceneconditions` (بدون تغییر شکل `PromptGenerationInput`).
- `app/src/test/.../domain/promptengine/PromptAssemblyTest.kt` — فقط افزودن دو خط import (`LightingSettings`, `EnvironmentSettings` از `domain.sceneconditions`)؛ هیچ Assertion یا منطق تستی تغییر نکرد.

## تصمیمات پیاده‌سازی محلی (بدون نیاز به پرسش)

### `source` معیار تصمیم است، نه صرفِ پر بودن `overrideValue`
`shotOverride` فقط وقتی در نظر گرفته می‌شود که `sourced.source == "override"` باشد — حتی اگر `overrideValue` مقدار داشته باشد ولی `source == "scene"`، نادیده گرفته می‌شود. این مطابق بلوپرینت ۰۵ است که صریحاً `source` را فیلد تصمیم‌گیرنده معرفی کرده، نه صرفِ وجود مقدار.

### بازگشت `Result<T>` به‌جای `T` غیرقابل‌Null
امضای پیشنهادی اولیه (`resolveCameraSettings(shot, sceneDefault: CameraSettings?): CameraSettings`) یک تناقض داشت: اگر `sceneDefault` هم `null` باشد و `source` هم `"scene"` (یا override بدون مقدار)، هیچ مقدار واقعی برای بازگرداندن وجود ندارد. بازگرداندن یک مقدار جعلی یا `throw` خام، هر دو با الگوی مستقر پروژه ناسازگار بودند. طبق الگوی موجود در `DependencyResolver`, `OverrideActions`, `SceneValidation`, `CinematicLanguage` (همه از `Result.failure(IllegalStateException(...))` برای حالت‌های واقعاً استثنایی استفاده می‌کنند)، امضا به `Result<T>` تغییر کرد — تصمیم پیاده‌سازی محلی، نه پرسیده‌شده، چون این‌جا صرفاً پیروی از یک الگوی از قبل تثبیت‌شده در همین پروژه است.

### استفاده‌ی واقعی از `inheritOrOverride` (نه بازتعریف منطق)
وقتی `sceneDefault` موجود است، مستقیماً `inheritOrOverride(sceneDefault, shotOverride)` واحد ۰۴ فراخوانی می‌شود — طبق دستور صریح این قدم. فقط وقتی `sceneDefault == null` (که خودِ `inheritOrOverride` امکان پذیرش آن را ندارد، چون پارامتر اولش `T` غیر-nullable است)، منطق fallback جداگانه‌ای برای حالت override-only نوشته شد.

## Consequences

- **آسان می‌شود:** `Shot.camera`/`.lighting`/`.environment` اکنون Type-safe هستند؛ بدهی فنی ADR-006 (تصمیم ۶) و یادداشت باقی‌مانده‌ی ADR-012 («لایه‌ی اتصال بین این‌ها و انواع واقعی هنوز نوشته نشده») رفع شد. `LightingSettings`/`EnvironmentSettings` اکنون در جای معماری درست خود (`domain.sceneconditions`، کنار enum های تشکیل‌دهنده) هستند.
- **بدهی باقی‌مانده:** خودِ `collectData` (اتصال واقعی Room ← `PromptGenerationInput`) هنوز پیاده نشده (وابسته به واحد ۱۵)؛ این قدم فقط تابع «حل‌کننده» را آماده کرد، نه سیم‌کشی واقعی بین `Shot` ذخیره‌شده در دیتابیس و `PromptGenerationInput`.
