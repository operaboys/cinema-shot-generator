# Cinema Shot Generator

اپ اندرویدِ تک‌کاربره و On-Device (بدون سرور) برای تولید پرامپت‌های متنی سینمایی برای مدل‌های AI ویدیوساز (Sora، Veo، Runway، Kling و مشابه). این اپ فقط **متن** تولید می‌کند، نه تصویر یا ویدیو.

## وضعیت فعلی

**در حال پیاده‌سازی تدریجی واحدهای معماری — هنوز بدون Room/Persistence و بدون UI واقعی.**

Scaffold پروژه (یک صفحه‌ی تست «Hello World») برقرار است. تاکنون لایه‌ی دامنه‌ی خالص (Kotlin، بدون Room و بدون UI) این واحدها پیاده‌سازی شده:

- **واحد ۰۱ — Story & Override:** `domain/story/` — مدل‌های Story Wizard، قوانین اعتبارسنجی، مدل‌های Human Override.
- **واحد ۰۲ — DNA Manager:** `domain/dna/` — مدل‌های DNA پروژه، منطق Soft Lock، قوانین اعتبارسنجی Rule 1 تا Rule 5.
- **واحد ۰۶ — Asset & Continuity:** `domain/asset/` — مدل‌های Character/Location Asset، Hard Lock مطلق (identity/appearance/age)، انتخاب خودکار Outfit/Expression، اعتبارسنجی فایل تصویر مرجع.
- **واحد ۰۷ — Validation & Consistency Engine:** `domain/validation/` — Validation Engine (Severity/ValidationIssue/ValidationReport سراسری)، Logic Conflict Checker (اکنون با `MotionLevel`/`CinematicMode` واقعی)، Dependency Resolver (تحلیل اثر، تشخیص وابستگی دایره‌ای).
- **واحد ۰۳ — Visual Identity:** `domain/visualidentity/` — Style Matrix (ترکیب کیفی سبک‌ها، بررسی سازگاری)، Cinematic Language (صاحب اصلی `CinematicMode`، تعیین ریتم Hybrid، اعتبارسنجی مدت شات).
- **واحد ۰۵ — Shot Engine:** `domain/shot/` — مدل‌های Shot/Beat/SoundProfile، صاحب اصلی `MotionLevel`، ارث‌بری از Scene (از طریق `inheritOrOverride` واحد ۰۴)، انتخاب Outfit (با `CharacterAsset` واقعی واحد ۰۶)، قوانین اعتبارسنجی Rule 1 تا Rule 5.
- **واحد ۰۴ — Scene Engine:** `domain/scene/` — مدل‌های Scene/SceneLocation/SceneConstraints، **صاحب اصلی `inheritOrOverride`** (تنها نسخه‌ی موجود در پروژه)، حذف Scene، قوانین اعتبارسنجی Rule 1/4/5.
- **واحد ۰۹ — Camera & Motion:** `domain/camera/` — Camera System (زاویه/فاصله/لنز/حرکت پایه و پیشرفته، ۵ قانون اعتبارسنجی)، Motion Intensity (سرعت/شدت سوژه، سرعت دوربین، Motion Blur).
- **واحد ۰۸ — Scene Conditions:** `domain/sceneconditions/` — Lighting System (Mood-to-Lighting، ۵ قانون اعتبارسنجی)، Environment & Weather Engine (Environment-to-Sound، ۸ قانون اعتبارسنجی، یکی مشترک با واحد ۰۷).
- **واحد ۱۰ — Audio Context Generator:** `domain/audio/` — تولید خودکار Ambient/Action Sound، پیشنهاد Breathing، `ActionSound`/`CharacterSound` بازاستفاده‌شده از واحد ۰۵ (نه بازتعریف)، Rule اعتبارسنجی تعداد لایه‌ی صوتی و timeline (با delegation به واحد ۰۷).

بلوپرینت هر واحد در `docs/blueprints/` منبع حقیقت است؛ تصمیمات و انحرافات تأییدشده در `docs/adr/` ثبت شده‌اند.

### تصمیمات Migration انجام‌شده

چهار سؤال باز تجمیعی قبلی (ADR-004، ADR-006، ADR-007) در یک قدم Migration حل شدند — جزئیات کامل در `docs/adr/010-cross-unit-migrations.md`:

1. **واحدهای ۰۱/۰۲/۰۶ به `ValidationIssue`/`Severity` سراسری واحد ۰۷ Migrate شدند.** معادل «Valid» قبلی: `null` (توابع تک‌نتیجه‌ای) یا لیست خالی (`validateStoryContext`). استثنا: `UpdateResult` (Hard Lock واحد ۰۶) عمداً دست‌نخورده ماند تا تضمین سطح Type System «هرگز فقط Warning نیست» حفظ شود.
2. **واحد ۰۷ اکنون از `MotionLevel`/`CinematicMode` واقعی استفاده می‌کند** (به‌جای String موقت) — یک وابستگی چرخه‌ای آگاهانه بین پکیج‌های `domain.validation` و `domain.shot` ایجاد کرد (مستند در ADR-010، بدون مشکل کامپایل).
3. **نسخه‌ی تکراری `inheritOrOverride` در واحد ۰۵ حذف شد**؛ واحد ۰۴ تنها صاحب این تابع است. تست تکراری متناظر (`ShotInheritanceTest.kt`) هم حذف شد.
4. **تضاد Rule «Shot بدون Subject» رفع شد:** `validateDataCompleteness` واحد ۰۷ اکنون `locationIds` را هم بررسی می‌کند و Severity آن Blocking است — دقیقاً هم‌راستا با `validateShotHasSubject` واحد ۰۵. **پیشنهاد باز (اجرا نشده):** یکی‌سازی این دو تابع یا نگه‌داشتن عمدی‌شان به‌عنوان دو Rule مستقل.

**یادداشت‌های باقی‌مانده (نه سؤال باز جدی):** هم‌پوشانی مفهومی `SubjectSpeed` (واحد ۰۹) و `MotionLevel` (واحد ۰۵)؛ همپوشانی `canDeleteAsset` (واحد ۰۷) و `validateAssetDeletion` (واحد ۰۶)؛ **سه نوع «صدای محیطی» مستقل** — `domain.shot.AmbientSound` (۳ فیلد)، `domain.sceneconditions.AmbientSoundSuggestion` (۳ فیلد)، `domain.audio.AmbientSound` (۴ فیلد، با `source`) — یکی‌سازی واقعی‌شان به تصمیم معمار درباره‌ی پایپ‌لاین بین واحدهای ۰۵/۰۸/۱۰ نیاز دارد (جزئیات در ADR-009 و ADR-011). در مقابل، `ActionSound`/`CharacterSound` واحد ۱۰ همان نمونه‌ی واحد ۰۵ را بازاستفاده کردند (بدون تکرار).

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
│   ├── camera/ → واحد ۰۹: Camera & Motion
│   ├── sceneconditions/ → واحد ۰۸: Scene Conditions (Lighting + Environment)
│   └── audio/  → واحد ۱۰: Audio Context Generator
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
