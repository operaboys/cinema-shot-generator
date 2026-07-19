# ADR-007: تصمیمات پیاده‌سازی واحد ۰۴ (Scene Engine) — لایه‌ی دامنه

**تاریخ:** 2026-07-19
**وضعیت:** تصمیم محل `inheritOrOverride` نهایی شد؛ یک پیشنهاد (نه تصمیم) برای بازبینی معمار باقی مانده

## Context

پیاده‌سازی لایه‌ی دامنه‌ی خالص واحد ۰۴ طبق `docs/blueprints/04-scene-engine.md`. این واحد صاحب اصلی `inheritOrOverride<T>` است — تابعی که واحد ۰۵ (Shot Engine) قبلاً به‌صورت مستقل کپی کرده بود چون این واحد هنوز وجود نداشت (`docs/adr/006-unit05-shot-engine-deviations.md`، سؤال باز ۲).

## تصمیم نهایی‌شده

### ۱. محل `inheritOrOverride`: اینجا (domain/scene/) — طبق دستور صریح
تابع در `domain/scene/SceneInheritance.kt` به‌عنوان صاحب اصلی تعریف شد.
**پیشنهاد (نه تصمیم اجراشده):** نسخه‌ی تکراری در `domain/shot/ShotInheritance.kt` (واحد ۰۵) می‌تواند حذف و با `import com.operaboys.cinemashotgenerator.domain.scene.inheritOrOverride` جایگزین شود. این کار در همین قدم انجام **نشد** — طبق دستور کار صریح، تغییر یک فایل موجود در واحد دیگر (حتی حذف یک تابع تکراری) خارج از scope مستقیم این قدم است و نیاز به تأیید جداگانه دارد. اگر تأیید شود: یک تغییر یک‌خطی در `ShotInheritance.kt` (حذف تعریف محلی + افزودن import) با ریسک بسیار پایین، چون امضا و رفتار دو تابع کاملاً یکسانند.

## تصمیمات مستقل (بدون نیاز به تأیید جداگانه)

### ۲. Rule 2 — بدون تابع Runtime، تضمین در سطح Type System
`Shot.sceneId: String` (واحد ۰۵) از قبل غیر-nullable و بدون مقدار پیش‌فرض است؛ ساخت یک `Shot` بدون `sceneId` اصلاً کامپایل نمی‌شود. نوشتن یک تابع Runtime در واحد ۰۴ برای این بررسی، هم تکراری بود و هم یک وابستگی غیرضروری `domain.scene → domain.shot` ایجاد می‌کرد (که جهتش هم برخلاف معماری است — Scene باید مستقل از Shot باشد، نه برعکس).

### ۳. Rule 3 — بدون تابع جداگانه
توضیحی است («ارث‌بری پیش‌فرض، مگر Override صریح»)؛ خودِ `inheritOrOverride` این رفتار را پیاده می‌کند.

### ۴. Rule 5 — atmosphere primary/secondary (نه لیست) طبق کد مفهومی
بخش «تعریف» بلوپرینت، atmosphere را به‌صورت لیست («تا ۲ مورد») توصیف کرده، اما کد مفهومی Kotlin آن را به `atmospherePrimary`/`atmosphereSecondary` تفکیک کرده. طبق راهنمایی صریح دستور کار (نیت روشن است)، از کد مفهومی پیروی شد — بدون توقف برای پرسش، چون این مورد یک تناقض واقعی نبود، فقط دو سطح توصیف (اجمالی در «تعریف»، دقیق در «پیاده‌سازی مفهومی») بودند.

### ۵. `GlobalVisualStyleRef` افزوده شد
فیلد `global_visual_style` فقط در بخش «ساختار داده» (JSON) بود، نه در کد مفهومی. شکل آن (`source` + یک `override` تکی nullable) با `SourcedSettings` واحد ۰۵ (`source` + `settings: Map<String,String>`) متفاوت است چون خودِ ساختار JSON این دو فیلد متفاوت است — بنابراین نوع جدید و مستقلی تعریف شد، نه بازاستفاده از نوع واحد ۰۵ (که علاوه بر تفاوت شکل، وابستگی `domain.scene → domain.shot` را هم ایجاد می‌کرد).

### ۶. Rule 4/5 از `ValidationIssue`/`Severity` سراسری واحد ۰۷
ادامه‌ی الگوی تثبیت‌شده در واحدهای ۰۳ و ۰۵.

### ۷. توابع non-suspend
مشابه ADR های قبلی.

## سؤال‌های باز (تجمیعی، از قدم‌های قبلی + این قدم)

۱. Migration واحدهای ۰۱/۰۲/۰۶ به `ValidationIssue`/`ValidationReport` سراسری واحد ۰۷؟ (ADR-004)
۲. Migration پارامترهای `String` واحد ۰۷ (`cinematicMode`، `motionLevel`) به enum های واقعی واحدهای ۰۳/۰۵؟ (ADR-004، ADR-006)
۳. **[این قدم]** آیا نسخه‌ی تکراری `inheritOrOverride` در `domain/shot/ShotInheritance.kt` باید حذف و با import از `domain/scene/` جایگزین شود؟
۴. تضاد حل‌نشده‌ی Rule «Shot بدون Subject» بین واحد ۰۵ (Blocking) و واحد ۰۷ (Warning)؟ (ADR-006)

## Consequences

- **آسان می‌شود:** واحد ۰۴ اکنون صاحب واقعی و پایدار `inheritOrOverride` است؛ واحدهای بعدی (مثل UI یا Repository) می‌توانند مستقیماً از اینجا import کنند.
- **بدهی ثبت‌شده:** دو نسخه‌ی همسان اما مستقل از `inheritOrOverride` تا زمان تصمیم معمار درباره‌ی سؤال ۳ همچنان در کد وجود دارند.
