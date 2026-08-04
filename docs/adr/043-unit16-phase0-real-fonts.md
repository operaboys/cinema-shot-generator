# ADR-043: واحد ۱۶ فاز ۰ (تکمیل) — اتصال فونت‌های واقعی Inter/Vazirmatn

**تاریخ:** 2026-08-04
**وضعیت:** کامل شد و مستقل قابل‌کامپایل/تست است.

## Context

`docs/adr/042-unit16-phase0-shared-foundation.md` یک محدودیت شناخته‌شده مستند
کرده بود: سند طراحی فونت Inter (لاتین/UI) و Vazirmatn (فارسی) می‌خواست، اما هیچ
فایل فونت واقعی (`.ttf`) در `docs/design/` ضمیمه نشده بود — `Type.kt` فقط
`FontFamily.Default` (فونت سیستم) را به‌عنوان Placeholder موقت استفاده می‌کرد.

این قدم آن محدودیت را رفع می‌کند: ۸ فایل واقعی
(`inter_regular/medium/semibold/bold.ttf`، `vazirmatn_regular/medium/semibold/bold.ttf`)
اکنون در `app/src/main/res/font/` موجودند — با `file` تأیید شد هر ۸ فایل واقعاً
TrueType Font معتبر هستند (Inter Project Authors / Vazirmatn Project Authors،
نه Placeholder/Corrupt).

**یادداشت شفافیت:** پیش از این قدم، `git fetch` یک واگرایی واقعی remote نشان
داد — دو Commit خارجی (`Create 1.ttf` سپس `Delete app/src/main/res/font/1.ttf`،
ظاهراً یک تست آپلود از سوی معمار) قبل از Commit واقعی `Add files via upload`
(هر ۸ فایل واقعی) روی remote بودند. با `git log --stat` بررسی و تأیید شد HEAD
نهایی remote تمیز است (بدون `1.ttf` باقی‌مانده) و دقیقاً ۸ فایل نام‌برده‌شده در
دستور کار را دارد؛ با `git merge --ff-only` (بدون Conflict) این تغییرات قبل از
شروع کار محلی merge شدند.

## پیاده‌سازی

### `Type.kt`

دو `FontFamily` واقعی ساخته شدند (`InterFontFamily`، `VazirmatnFontFamily`)، هرکدام
با ۴ وزن (`Font(R.font.xxx, FontWeight.xxx)`). تابع
`cinemaFontFamily(language: Language): FontFamily` تنها نقطه‌ی تصمیم‌گیری
Inter/Vazirmatn است — `Language.FA → VazirmatnFontFamily`، `Language.EN →
InterFontFamily`، طبق `docs/design/README.md` («Vazirmatn — used automatically
whenever lang = fa»).

چون `FontFamily` اکنون به `language` وابسته است (نه یک مقدار ثابت سطح‌فایل)،
۵ `TextStyle` سطح‌بالای قبلی (`DisplayLargeStyle`/...) به توابع خصوصی
(`displayLargeStyle(fontFamily)`/...) تبدیل شدند — با grep تأیید شد هیچ فایل
دیگری این ۵ نام یا `CinemaTypography`/`CinemaFontFamily` قدیمی را مستقیماً
Import نمی‌کرد (فقط خودِ `Theme.kt`، از طریق `CinemaTypography`) — پس این
تبدیل به Private کاملاً بی‌خطر بود. `CinemaFontFamily` (مقدار ثابت
`FontFamily.Default`) کاملاً حذف شد (نه Alias منسوخ) — چون هیچ مصرف‌کننده‌ی
دیگری نداشت، نگه‌داشتنش فقط یک مسیر خاموش/فراموش‌شده به فونت سیستم می‌ساخت،
دقیقاً همان چیزی که دستور کار صریحاً منع کرده بود («هیچ Composable ای نباید
بعد از این تغییر همچنان از فونت سیستم استفاده کند»).

`CinemaTypography` (یک `val` ثابت) به `cinemaTypography(language: Language): Typography`
تبدیل شد.

### تصمیم مستقل: تزریق `language` مستقیم به `CinemaShotGeneratorTheme` (نه CompositionLocal جدید)

`CinemaShotGeneratorTheme` یک پارامتر جدید `language: Language` گرفت (کنار
`darkTheme: Boolean` موجود). با grep تأیید شد در فاز ۰، `App.kt` (تنها فراخوان
واقعی) از قبل `language` را مستقیماً از `WorkflowViewModel.language`
(`collectAsStateWithLifecycle`) در دسترس دارد — دقیقاً همان الگوی موجود
`darkTheme` (که خودش هم مستقیماً تزریق می‌شود، نه از یک `CompositionLocal`
جداگانه خوانده می‌شود). افزودن یک `CompositionLocal` جدید مخصوص `language` فقط
یک لایه‌ی انتزاعی موازی و غیرضروری می‌ساخت برای دقیقاً همان داده‌ای که یک خط
بالاتر همین تابع از قبل در دسترس دارد؛ پس تزریق مستقیم پارامتر انتخاب شد —
سازگار با الگوی موجود، بدون پیچیدگی اضافه.

## بررسی‌های اضافی

- با grep تأیید شد هیچ Composable/Preview دیگری در فاز ۰ مستقیماً از
  `CinemaFontFamily` قدیمی استفاده نمی‌کرد — تنها مصرف‌کننده خودِ `Type.kt` بود.
- هر دو فراخوان `CinemaShotGeneratorTheme(...)` موجود (`App.kt` production،
  `AppNavigationTest.kt` تست) برای پارامتر جدید `language` به‌روزرسانی شدند؛
  هیچ‌کدام حذف نشدند.

## تست

- `TypeTest.kt` (جدید، ۳ تست): `cinemaFontFamily` مقدار درست هر زبان + عدم
  تساوی بین دو زبان؛ `cinemaTypography` واقعاً fontFamily را با تغییر زبان عوض
  می‌کند (نه فقط ادعا)؛ هر ۵ نقش مستندشده‌ی Type Scale از همان FontFamily حل‌شده
  استفاده می‌کنند.
- `AppNavigationTest.kt` (فاز ۰) بدون تغییر منطقی، فقط فراخوانی
  `CinemaShotGeneratorTheme` برای پارامتر جدید به‌روزرسانی شد — هر ۴ تست موجود
  همچنان پاس می‌شوند.

## Consequences

- **آسان می‌شود:** محدودیت فونت مستندشده در ADR-042 کاملاً بسته شد؛ فازهای بعدی
  (ساخت خودِ صفحات) اکنون واقعاً از Inter/Vazirmatn رندر می‌کنند، نه فونت سیستم.
- **بدون بدهی جدید شناخته‌شده:** «Liquid Glass» (Blur/Gradient روی سطوح واقعی)
  و تطبیق دقیق آیکون «Material Symbols Rounded» همچنان کار فازهای بعدی هستند —
  این دو محدودیت مستقل از فونت بودند و همچنان مستقل باقی می‌مانند.
