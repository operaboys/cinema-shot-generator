# Cinema Shot Generator

اپ اندرویدِ تک‌کاربره و On-Device (بدون سرور) برای تولید پرامپت‌های متنی سینمایی برای مدل‌های AI ویدیوساز (Sora، Veo، Runway، Kling و مشابه). این اپ فقط **متن** تولید می‌کند، نه تصویر یا ویدیو.

## وضعیت فعلی

**در حال پیاده‌سازی تدریجی واحدهای معماری — هنوز بدون Room/Persistence و بدون UI واقعی.**

Scaffold پروژه (یک صفحه‌ی تست «Hello World») برقرار است. تاکنون لایه‌ی دامنه‌ی خالص (Kotlin، بدون Room و بدون UI) این واحدها پیاده‌سازی شده:

- **واحد ۰۱ — Story & Override:** `domain/story/` — مدل‌های Story Wizard، قوانین اعتبارسنجی، مدل‌های Human Override.
- **واحد ۰۲ — DNA Manager:** `domain/dna/` — مدل‌های DNA پروژه، منطق Soft Lock، قوانین اعتبارسنجی Rule 1 تا Rule 5.
- **واحد ۰۶ — Asset & Continuity:** `domain/asset/` — مدل‌های Character/Location Asset، Hard Lock مطلق (identity/appearance/age)، انتخاب خودکار Outfit/Expression، اعتبارسنجی فایل تصویر مرجع.
- **واحد ۰۷ — Validation & Consistency Engine:** `domain/validation/` — Validation Engine (Severity/ValidationIssue/ValidationReport سراسری)، Logic Conflict Checker، Dependency Resolver (تحلیل اثر، تشخیص وابستگی دایره‌ای).
- **واحد ۰۳ — Visual Identity:** `domain/visualidentity/` — Style Matrix (ترکیب کیفی سبک‌ها، بررسی سازگاری)، Cinematic Language (صاحب اصلی `CinematicMode`، تعیین ریتم Hybrid، اعتبارسنجی مدت شات).
- **واحد ۰۵ — Shot Engine:** `domain/shot/` — مدل‌های Shot/Beat/SoundProfile، صاحب اصلی `MotionLevel`، ارث‌بری از Scene (`inheritOrOverride`، نسخه‌ی محلی موقت)، انتخاب Outfit (با `CharacterAsset` واقعی واحد ۰۶)، قوانین اعتبارسنجی Rule 1 تا Rule 5.
- **واحد ۰۴ — Scene Engine:** `domain/scene/` — مدل‌های Scene/SceneLocation/SceneConstraints، **صاحب اصلی `inheritOrOverride`**، حذف Scene، قوانین اعتبارسنجی Rule 1/4/5.
- **واحد ۰۹ — Camera & Motion:** `domain/camera/` — Camera System (زاویه/فاصله/لنز/حرکت پایه و پیشرفته، ۵ قانون اعتبارسنجی)، Motion Intensity (سرعت/شدت سوژه، سرعت دوربین، Motion Blur).

بلوپرینت هر واحد در `docs/blueprints/` منبع حقیقت است؛ تصمیمات و انحرافات تأییدشده در `docs/adr/` ثبت شده‌اند. **سؤال‌های باز فعلی:** (۱) آیا Migration واحدهای ۰۱/۰۲/۰۶ به‌سمت ValidationIssue/ValidationReport سراسری واحد ۰۷ باید یک قدم اجرایی جداگانه باشد؟ (۲) آیا واحد ۰۷ باید پارامترهای `String` موقتش (`cinematicMode`، `motionLevel`) را به enum های واقعی واحدهای ۰۳/۰۵ Migrate کند؟ (۳) آیا نسخه‌ی تکراری `inheritOrOverride` در `domain/shot/ShotInheritance.kt` (واحد ۰۵) باید حذف و با import از واحد ۰۴ جایگزین شود؟ (۴) تضاد حل‌نشده‌ی Rule «Shot بدون Subject» بین واحد ۰۵ (Blocking) و واحد ۰۷ (Warning). **یادداشت (نه سؤال باز):** هم‌پوشانی مفهومی SubjectSpeed (واحد ۰۹) و MotionLevel (واحد ۰۵) — عمداً بدون نگاشت رسمی باقی ماندند. (جزئیات در `docs/adr/004-...md` تا `008-...md`)

## Stack

- **زبان:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **معماری:** MVVM ساده (ViewModel + StateFlow) — بدون فریمورک DI در فاز اول
- **ذخیره‌سازی:** Room (روی SQLite) — پشتیبانی از چند پروژه‌ی همزمان
- **اتصال AI (اختیاری):** Ktor Client — فقط وقتی کاربر کلید API شخصی وارد کند

## ساختار

```
app/src/main/java/com/operaboys/cinemashotgenerator/
├── data/    → Room entities, DAO, Database class (خالی)
├── domain/  → مدل‌های دامنه و منطق کسب‌وکار
│   ├── story/  → واحد ۰۱: Story Wizard + Human Override
│   ├── dna/    → واحد ۰۲: DNA Manager (Soft Lock)
│   ├── asset/  → واحد ۰۶: Asset & Continuity (Hard Lock)
│   ├── validation/ → واحد ۰۷: Validation & Consistency Engine
│   ├── visualidentity/ → واحد ۰۳: Visual Identity (Style Matrix + Cinematic Language)
│   ├── shot/   → واحد ۰۵: Shot Engine
│   ├── scene/  → واحد ۰۴: Scene Engine (صاحب اصلی inheritOrOverride)
│   └── camera/ → واحد ۰۹: Camera & Motion
├── ui/      → صفحه‌های Compose (فعلاً فقط صفحه‌ی تست)
└── di/      → (خالی، برای بعد)

docs/blueprints/  → بلوپرینت‌های معماری (منبع حقیقت) — قبل از پیاده‌سازی هر واحد بخوانید
docs/adr/         → تصمیمات و انحرافات تأییدشده در هر قدم اجرایی
```

## Build

نیازمندی‌ها: JDK 17+، Android SDK (compileSdk 36). نسخه‌ها: AGP 8.13.0، Gradle 8.14.3، Kotlin 2.2.21 — minSdk 26، targetSdk 36.

```
./gradlew :app:assembleDebug
```

## محدودیت‌های شناخته‌شده

(فعلاً خالی — طبق قانون پروژه، این بخش باید هر بار که محدودیتی اضافه یا رفع شد به‌روزرسانی شود.)
