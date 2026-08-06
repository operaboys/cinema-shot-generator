# ADR-056: واحد ۱۶ — فاز ۵، قدم ۲ — Prompt Generation با قدم ۳ (Output Delivery) ادغام شد

**تاریخ:** 2026-08-06
**وضعیت:** تصمیم ثبت شد؛ هیچ کد UI تازه‌ای در این قدم ساخته نشد (طبق دلیل زیر). زنجیره‌ی
دامنه‌ی پیش‌نیاز (که کاملاً از قبل موجود بود) با ۶۳۵ تست موجود پروژه دوباره تأیید شد.

## Context

قدم دوم فاز ۵ طبق برنامه‌ی اولیه، «صفحه‌ی Prompt Generation» بود. پیش از هر کدی،
تحقیق اولیه‌ی الزامی (طبق دستور کار) این بود: با grep دقیق، شماره‌ی بخش
Prompt Generation را در `docs/design/README.md` — بین بخش ۹ (Validation) و
۱۰ (Output Delivery) — پیدا کنم.

## یافته (تناقض واقعی، نه جزئیات محلی)

با grep مستقیم فهرست کامل «Screens / Views» در `docs/design/README.md`
(۱۲ صفحه، شماره‌گذاری‌شده ۱ تا ۱۲)، بخش ۹ مستقیماً به بخش ۱۰ می‌رسد —
**هیچ بخش مستقلی برای «Prompt Generation» بین این دو وجود ندارد**. تنها
اشاره به این نام در کل فایل، یک لینک در فهرست Drawer است («TOOLS
(Validation, Prompt Generator, Output Delivery)»)، بدون هیچ مشخصات UI
مستقل خودش.

این یافته دقیقاً با متن صریح `docs/blueprints/16-user-workflow-v2.md`
هم‌راستاست: «**مرحله ۷ (Prompt Generation) — کاملاً پشت‌صحنه، بدون UI
مستقل:** ... این مرحله هیچ نمایش بصری مستقل خودش را ندارد؛ نتیجه‌اش مستقیم
وارد مرحله‌ی ۸ می‌شود.» بخش ۱۰ (Output Delivery) خودِ `docs/design/README.md`
هم از قبل شامل «Model picker» + «Output Preview card» (بلوک پرامپت نهایی،
شمار Token، Copy/Regenerate، Export) است — یعنی هر دو منبع حقیقت، بدون
استثنا، مدل UI یکسانی دارند: انتخاب مدل + پیش‌نمایش/تولید همه در یک صفحه‌ی
واحد («Output Delivery»)، نه دو صفحه‌ی جدا.

## تصمیم (تأییدشده مستقیماً توسط معمار)

طبق پرسش صریح از معمار (به‌دلیل اینکه این یک تناقض معمارانه‌ی واقعی بود، نه
جزئیات پیاده‌سازی محلی که خودم تصمیم بگیرم)، پاسخ دریافت‌شده: **این قدم با
قدم ۳ ادغام شود.** یعنی:

- هیچ صفحه‌ی مستقل «Prompt Generation» ساخته نشد.
- انتخاب مدل هدف، نمایش warnings، پیش‌نمایش structuredParts، و دکمه‌ی
  تولید نهایی — همگی به قدم بعدی (که اکنون نقش «Output Delivery» کامل را
  دارد، شامل هر دو وظیفه‌ی توصیف‌شده در این قدم و قدم قبلی) موکول شدند.
- زنجیره‌ی دامنه‌ی پیش‌نیاز (`ShotRepository`+`SceneRepository`+
  `ProjectDnaRepository`+`AssetRepository` → `PromptGenerationRepository.
  collectData(shotId): Result<PromptGenerationInput>` (واحد ۱۵→۱۱) →
  `assemblePromptBlueprint(...)` (واحد ۱۱، تولید `PromptBlueprint` با
  `structuredParts`/`weightedEmphasis`/`seed`/`conflictsResolved`/
  `warnings`/`negativePrompt`) → `optimizeForProfile`/`render(blueprint,
  profile)` (واحد ۱۴)) از قبل کامل، تست‌شده، و بدون هیچ کمبود شناخته‌شده
  بود (تأییدشده با خواندن مستقیم `PromptGenerationRepository.kt` و
  `PromptAssembly.kt` توسط معمار، پیش از این قدم) — پس هیچ کار دامنه‌ای
  تازه‌ای هم برای این قدم لازم نبود.

## Consequences

- فاز ۵ اکنون عملاً دو قدم باقی‌مانده به‌جای سه قدم دارد: قدم ۱ (Validation،
  کامل شد) و یک قدم ترکیبی «Prompt Generation + Output Delivery» (نامش در
  گزارش‌های بعدی همان «Output Delivery» خواهد بود، طبق نامگذاری
  `docs/design/README.md`).
- سؤالات معماری باقی‌مانده‌ای که این قدم قرار بود پاسخ دهد (آیا انتخابگر مدل
  یک Composable مشترک باشد؛ آیا warnings این صفحه با warnings Validation
  هم‌پوشانی دارند) به قدم ترکیبی بعدی موکول شدند — چون اکنون هر دو در همان
  یک صفحه پاسخ داده می‌شوند، نه بین دو صفحه‌ی جدا.
- هیچ فایل کد (main یا test) در این قدم تغییر نکرد؛ فقط این ADR و
  `README.md` (اعلام رسمی این تصمیم) commit شدند.
