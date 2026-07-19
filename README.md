# Cinema Shot Generator

اپ اندرویدِ تک‌کاربره و On-Device (بدون سرور) برای تولید پرامپت‌های متنی سینمایی برای مدل‌های AI ویدیوساز (Sora، Veo، Runway، Kling و مشابه). این اپ فقط **متن** تولید می‌کند، نه تصویر یا ویدیو.

## وضعیت فعلی

**در حال پیاده‌سازی تدریجی واحدهای معماری — هنوز بدون Room/Persistence و بدون UI واقعی.**

Scaffold پروژه (یک صفحه‌ی تست «Hello World») برقرار است. تاکنون لایه‌ی دامنه‌ی خالص (Kotlin، بدون Room و بدون UI) این واحدها پیاده‌سازی شده:

- **واحد ۰۱ — Story & Override:** `domain/story/` — مدل‌های Story Wizard، قوانین اعتبارسنجی، مدل‌های Human Override.
- **واحد ۰۲ — DNA Manager:** `domain/dna/` — مدل‌های DNA پروژه، منطق Soft Lock، قوانین اعتبارسنجی Rule 1 تا Rule 5.
- **واحد ۰۶ — Asset & Continuity:** `domain/asset/` — مدل‌های Character/Location Asset، Hard Lock مطلق (identity/appearance/age)، انتخاب خودکار Outfit/Expression، اعتبارسنجی فایل تصویر مرجع.

بلوپرینت هر واحد در `docs/blueprints/` منبع حقیقت است؛ تصمیمات و انحرافات تأییدشده در `docs/adr/` ثبت شده‌اند.

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
│   └── asset/  → واحد ۰۶: Asset & Continuity (Hard Lock)
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
