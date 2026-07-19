# ADR-002: تصمیمات پیاده‌سازی واحد ۰۲ (DNA Manager) — لایه‌ی دامنه

**تاریخ:** 2026-07-19
**وضعیت:** تأییدشده (توسط کاربر/معمار در جریان تأیید پلن قدم اجرایی واحد ۰۲)

## Context

پیاده‌سازی لایه‌ی دامنه‌ی خالص واحد ۰۲ طبق `docs/blueprints/02-dna-manager.md`، به همان الگوی واحد ۰۱: بدون Room، بدون UI، بدون اتصال واقعی به StoryContext (واحد ۰۱) یا Scene/Validation Engine (واحد ۰۴/۰۷).

## تصمیمات

### ۱. تعمیم امضای validateShotAgainstDna (تعمیم آگاهانه، نه فقط جزئیات محلی)
**بلوپرینت (کد مفهومی):** `validateShotAgainstDna(shotCameraAngle: String, dna: ProjectDna)` — فقط دسته‌ی ثابت `"camera"` را بررسی می‌کند.
**پیاده‌سازی:** `validateShotAgainstDna(shotElement: String, elementCategory: String, dna: ProjectDna)`.
**دلیل:** `forbidden_elements` در ساختار داده‌ی بلوپرینت سه دسته دارد (`camera`, `lighting`, `weather`)؛ امضای اصلی فقط camera را پوشش می‌داد. این تعمیم یک تابع واحد را برای هر سه دسته کاربردپذیر می‌کند، به‌جای نیاز به سه تابع تقریباً تکراری.
**چرا جدا ثبت شد:** این یک **تعمیم آگاهانه‌ی امضای دقیق کد مفهومی بلوپرینت** است، نه صرفاً پرکردن یک جزئیات نامشخص — طبق تأیید صریح کاربر، به‌عنوان یک آیتم مجزا (نه ذیل تصمیمات محلی عادی) ثبت می‌شود.

### ۲. GlobalMoodBase.primaryEmotion — String، نه enum Mood واحد ۰۱
بلوپرینت این ساختار را با کامنت `// ... globalMoodBase, stylePreferences مشابه` بدون جزئیات رها کرده بود. برای پرهیز از وابستگی کامپایل بین واحدها (`domain.dna` → `domain.story`) — که این قدم صریحاً از Scope آن را حذف کرده بود — و برای پرهیز از تکرار مقادیر enum Mood تحت نام جدید (ریسک واگرایی آینده)، این فیلد `String` باقی ماند.

### ۳. IntensityLevel — enum محلی جدید
برای `GlobalMoodBase.intensity` (واژگان محدود سه‌مقداری، مفهوماً متفاوت از `SaturationLevel`).

### ۴. StylePreferences زیرساختارها — String/List<String>
`cinematicLanguage`, `colorPhilosophy`, `cameraPreferences`, `lightingPreferences` با فیلدهای `String`/`List<String>` مدل شدند — این‌ها واژگان باز متعلق به واحدهای آینده‌اند (مثلاً Camera & Motion = واحد ۰۹) که enum های خودشان را تعریف خواهند کرد؛ این واحد نباید آن واژگان را از پیش اختراع کند.

### ۵. افزودن OverrideRules به ProjectDna
در دستور کار صریحاً فقط GlobalMoodBase و StylePreferences نام برده شده بودند («مثل» = نمونه‌ی غیرجامع)، اما `override_rules` هم بخشی از همان بلوک «ساختار داده» JSON بلوپرینت است و برای تابع کمکی Rule 5 لازم بود؛ بنابراین اضافه شد.

### ۶. ValidationResult محلی و متفاوت از واحد ۰۱
`ValidationResult` در `domain.dna` یک `sealed class` تک‌نتیجه‌ای (Valid/Warning/Blocking) است، در حالی‌که `ValidationResult` واحد ۰۱ یک `data class` با لیست خطا/هشدار است. هر دو موقتی‌اند و دستور کار صریحاً این رویکرد را برای این واحد خواسته بود («مشابه رویکرد ValidationResult محلی در واحد ۰۱» یعنی محلی‌بودن، نه شکل یکسان). واحد ۰۷ در آینده نوع سراسری را تعریف می‌کند.

### ۷. Rule 3 helper با List<String> به‌جای نوع Shot واقعی
`checkMandatoryElementsPresent` به‌جای گرفتن یک نوع `Shot` واقعی (که در واحد ۰۵ تعریف می‌شود و هنوز وجود ندارد)، یک `List<String>` از عناصر نهاییِ شات می‌گیرد. اتصال واقعی به «لحظه‌ی Finalize شات» بعداً در واحد ۰۵ انجام می‌شود.

### ۸. Rule 5 helper — فقط یک پرچم
`requiresApprovalForOverride(dna): Boolean` صرفاً `overrideRules.requiresHumanApproval` را برمی‌گرداند. نمایش دیالوگ تأیید و ثبت خودکار Scope (بخشی از Rule 5) متعلق به `HumanOverride`/`OverrideScope` واحد ۰۱ به‌علاوه‌ی لایه‌ی UI است — هر دو خارج از Scope این قدم.

### ۹. توابع non-suspend
مشابه ADR-001 §5 — منطق خالص فعلاً نقطه‌ی تعلیق ندارد؛ kotlinx-coroutines وابستگی پروژه نیست.

## Consequences

- **آسان می‌شود:** پوشش کامل هر سه دسته‌ی forbidden_elements با یک تابع؛ تست‌پذیری بدون Android/Mock.
- **بدهی ثبت‌شده:** مهاجرت آینده‌ی ValidationResult به واحد ۰۷؛ اتصال واقعی Rule 3 به واحد ۰۵ و Rule 5 به HumanOverride واحد ۰۱ + UI؛ در صورت اتصال واقعی StoryContext در آینده، بازبینی primaryEmotion (احتمالاً نگاشت از Mood به String در لایه‌ی Repository/Mapper، نه در این واحد).
