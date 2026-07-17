# Cinema Shot Generator

اپ اندرویدِ تک‌کاربره و On-Device (بدون سرور) برای تولید پرامپت‌های متنی سینمایی برای مدل‌های AI ویدیوساز (Sora، Veo، Runway، Kling و مشابه). این اپ فقط **متن** تولید می‌کند، نه تصویر یا ویدیو.

## وضعیت فعلی

**در حال راه‌اندازی اولیه — هنوز هیچ منطق کسب‌وکار پیاده‌سازی نشده.**

فعلاً فقط Scaffold پروژه (یک صفحه‌ی تست «Hello World») و ساختار پایه‌ی پوشه‌ها موجود است.

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
├── domain/  → مدل‌های دامنه (خالی)
├── ui/      → صفحه‌های Compose (فعلاً فقط صفحه‌ی تست)
└── di/      → (خالی، برای بعد)

docs/blueprints/  → بلوپرینت‌های معماری (منبع حقیقت) — قبل از پیاده‌سازی هر واحد بخوانید
```

## Build

نیازمندی‌ها: JDK 17+، Android SDK (compileSdk 36). نسخه‌ها: AGP 8.13.0، Gradle 8.14.3، Kotlin 2.2.21 — minSdk 26، targetSdk 36.

```
./gradlew :app:assembleDebug
```

## محدودیت‌های شناخته‌شده

(فعلاً خالی — طبق قانون پروژه، این بخش باید هر بار که محدودیتی اضافه یا رفع شد به‌روزرسانی شود.)
