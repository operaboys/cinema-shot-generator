# ADR-047: واحد ۱۶ — فاز ۲، قدم ۳ (آخرین قدم فاز ۲): Tab «DNA» واقعی

**تاریخ:** 2026-08-04
**وضعیت:** کامل شد. `gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL` (۵۸۸ تست، ۰ شکست، ۰ خطا).

## Context

`domain.dna.ProjectDna` (واحد ۰۲، Migration نسخه ۵) و `data/repository/ProjectDnaRepository.kt`
(واحد ۱۵) قبل از این قدم کامل و تست‌شده بودند اما هیچ UI ای به آن‌ها وصل نبود — این
قدم اولین اتصال است، و آخرین قدم فاز ۲ واحد ۱۶ (بعد از Story Tab و AI Story
Breakdown). طبق docs/design/README.md بخش «۳. Studio → DNA tab» و
docs/blueprints/16-user-workflow-v2.md «مرحله ۲».

## تصمیم ۱: Collapse فقط در سطح ۶ گروه (نه Accordion دوسطحی روی هر ردیف هم)

سند طراحی از «ردیف‌های قابل‌گسترش با Label/Value/Chevron» صحبت می‌کند که می‌تواند
به معنای Accordion تودرتو (هر فیلد هم جدا Expand/Collapse شود) خوانده شود. تصمیم
گرفته شد فقط سطح گروه (۶ گروه) واقعاً Collapsible باشد و هر فیلد مستقیماً
Dropdown/TextField خودش را نشان دهد (بدون یک لایه‌ی Expand اضافه‌ی دیگر) — چون
سود لایه‌ی دوم صرفاً تراکم دیداری است، نه عملکردی، و هیچ‌کدام از تست‌های الزامی
این قدم (Round-Trip، اعتبارسنجی زنده، انتخاب گروه‌بندی‌شده) به آن نیاز ندارد؛
افزودنش پیچیدگی بی‌دلیل بود (طبق اصل «بدون انتزاع فراتر از نیاز کار»).

## تصمیم ۲: هر ۶ گروه پیش‌فرض باز (expanded) هستند

کاربر بلافاصله کل فرم را می‌بیند؛ Chevron برای بستن دستی هر گروه در دسترس است. این
تصمیم UX محلی، معکوس‌پذیر و بدون اثر روی منطق دامنه است.

## تصمیم ۳: `colorPalette` همیشه یک لیست فشرده (بدون رشته‌ی خالی میان‌مقداری)

نوار ۵ Swatch با شاخص (index) مستقیماً به `masterPalette.colorPalette.getOrNull(index)`
وصل است. پاک‌کردن یک Swatch میانی، رنگ‌های بعدی را یک شاخص جلو می‌آورد (نه نگه‌داشتن
جای خالی) — چون فیلد دامنه («دقیقاً تا ۵ مقدار Hex») به‌صراحت یک لیست فشرده است، نه
۵ خانه‌ی ثابت؛ نگه‌داشتن رشته‌های خالی در لیست دامنه هم با توضیح خودِ فیلد در تناقض
بود و هم می‌توانست مصرف‌کننده‌های آینده‌ی این فیلد (مثل Renderer) را با مقادیر خالی
غیرمنتظره روبه‌رو کند.

## تصمیم ۴: `validateColorPalette` تنها تابع `DnaValidation.kt` است که مستقیماً روی این فرم اعمال می‌شود

با خواندن کامل `DnaValidation.kt` تأیید شد: `updateCoreIdentity` (Rule 1)/
`validateShotAgainstDna` (Rule 2)/`validateShotDuration`/`validateShotAspectRatio`
(Rule 4)/`checkMandatoryElementsPresent` (Rule 3) همگی یک Shot را در برابر DNA
اعتبارسنجی می‌کنند (کار Shot Composer، واحد آینده)، نه خودِ فرم DNA را حین ویرایش.
فقط `validateColorPalette` (Rule 6/7) واقعاً یک فیلد این صفحه (لیست رنگ) را
مستقیماً اعتبارسنجی می‌کند — دقیقاً همان چیزی که سناریوی تست «Hex نامعتبر → Blocking
فوری» درخواست کرده بود.

## تصمیم ۵: `updateCoreIdentity` (Rule 1، Soft Lock) به‌صورت مستقیم فراخوانی نشد — تغییر VisualStyle معادل `dependentShotsCount=0` اعمال شد

`updateCoreIdentity(currentDna, newStyle, dependentShotsCount)` برای هشدار دادن
به شمار واقعی شات‌های وابسته نیاز دارد. با grep تأیید شد هیچ Repository ای هنوز
«همه‌ی شات‌های یک پروژه» (نه فقط یک Scene) را نمی‌شمارد — ساختن این شمارشگر تجمعی
جدید خارج از Scope این قدم بود (فهرست فیلدهای صریح دستور کار این قدم را نداشت).
بنر ثابت Soft Lock بالای صفحه، خودِ فلسفه (تغییر همیشه مجاز است) را به کاربر منتقل
می‌کند؛ تغییر واقعی VisualStyle مستقیماً و بدون هشدار اعمال می‌شود — از نظر رفتاری
با فراخوانی `updateCoreIdentity` با ۰ شات وابسته یکسان است.

## تصمیم ۶: `OutputConstraints` فقط با `AspectRatio` در UI نمایش داده می‌شود

دستور کار صریح این قدم فقط `AspectRatio` را برای گروه «محدودیت‌های خروجی» فهرست
کرده بود؛ `forbiddenElements`/`mandatoryElements`/`maxShotDurationSeconds` در آن
فهرست نیامدند. این سه فیلد با مقدار پیش‌فرض ثابت (نقشه/لیست خالی، ۸ ثانیه) باقی
می‌مانند — بدون کنترل UI در این قدم؛ افزودن UI برای آن‌ها به قدم/واحد بعدی
(احتمالاً هم‌زمان با Shot Composer که واقعاً از این محدودیت‌ها استفاده می‌کند)
موکول شد.

## تصمیم ۷: `DnaViewModel` از همان ابتدا با الگوی تزریق‌پذیری Repository ساخته شد

طبق یافته‌ی واقعی ADR-045 (StoryViewModel بدون تزریق صریح، بی‌صدا از
`AppDatabase.getInstance()` استفاده می‌کرد)، همان الگو (پارامتر `repository`
Nullable در `factory(...)`) از همان ابتدا اعمال شد — بدون نیاز به کشف مجدد همان
باگ. `HomeProjectsStudioFlowTest.kt`/`AppNavigationTest.kt` (تست‌های عمومی سطح
`MainScaffold` که حالا واقعاً به Tab DNA هم می‌رسند) هم به‌روزرسانی شدند تا
`projectDnaRepository` تزریق‌شده را پاس بدهند.

## تصمیم ۸: پیش‌فرض خنثی `defaultProjectDna` برای پروژه‌ی تازه

`ProjectDna`/`CoreIdentity`/`MasterPalette`/`OutputConstraints` فیلدهای الزامی
بدون پیش‌فرض دارند. برای یک پروژه‌ی تازه، مقادیر خنثی/میانه انتخاب شدند (هم‌راستا
با انتخاب‌های خنثی مشابه `defaultStoryContext`، ADR-045):
`VisualStyle.CINEMATIC_STYLE` (عمومی‌ترین سبک)، `RealismLevel.SEMI_REALISTIC`،
`StyleConsistency.MODERATE`، `ColorTemperature.NEUTRAL`،
`SaturationLevel.MEDIUM`، `ContrastLevel.MEDIUM`، `Mood.CALM` (هم‌راستا با
پیش‌فرض Mood موجود StoryViewModel)، `AspectRatio.LANDSCAPE_16_9` (رایج‌ترین
نسبت)، `maxShotDurationSeconds=8`.

## تصمیم ۹ (یافته‌ی واقعی تست، دو مرحله‌ای): `performClick()` روی کارت پروژه (`ProjectCard.kt`) گاهی هیچ ناوبری‌ای Trigger نمی‌کند

هنگام نوشتن تست Round-Trip (تغییر DNA → بازگشت به Home → ورود دوباره از کارت
پروژه → کلیک Tab «DNA»)، دو یافته‌ی جدا با دیباگ مستقیم شمارش گره‌های Semantics
کشف شد:
1. انتظار روی `hasText(name)` بعد از کلیک کارت پروژه سیگنال قابل‌اعتمادی نیست —
   چون کارت همان پروژه از قبل، همان لحظه هم روی صفحه‌ی Home («پروژه‌های اخیر»)
   با همان متن قابل‌مشاهده است؛ شرط انتظار پیش از وقوع `navigate()` هم از قبل
   `true` بوده. رفع شد با انتظار روی سیگنال مختص Studio (وجود Tag Tab «داستان»
   — که همیشه صرف‌نظر از Tab انتخاب‌شده رندر می‌شود).
2. یافته‌ی مهم‌تر: با شمارش مستقیم گره‌ها تأیید شد `performClick()` (لمس مبتنی بر
   مختصات) روی خودِ کارت پروژه، در این سناریوی خاص (بازگشت به Home از عمق Tab
   «DNA» استودیو)، هیچ ناوبری‌ای اجرا نمی‌کند — صفحه دقیقاً همان Home باقی
   می‌ماند. این عیناً همان کلاس مشکل تثبیت‌شده در ADR-045 برای
   FilterChip/DropdownMenuItem است (لمس مختصاتی «موفق» گزارش می‌شود اما اثر
   واقعی ندارد)، این‌بار روی یک Card؛ رفع شد با همان راه‌حل تثبیت‌شده —
   `performSemanticsAction(SemanticsActions.OnClick)` به‌جای `performClick()`.

## تست

`DnaTabFlowTest.kt` (۳ تست End-to-End، `MainScaffold` کامل + Room واقعی
In-Memory):
- **Round-Trip:** تغییر Visual Style → بازگشت واقعی به Home → ورود دوباره از کارت
  پروژه → سوییچ به Tab «DNA» → مقدار تغییریافته باقی مانده است.
- **اعتبارسنجی زنده:** رنگ Hex نامعتبر در یک Swatch → پیام Blocking فوری
  (`validateColorPalette`)؛ فیلد دیگری (پریست درجه‌بندی رنگ) هم‌زمان همچنان
  قابل‌تایپ است — اثبات مستقیم Soft Lock Philosophy («هیچ‌چیز کاربر را از ادامه‌ی
  تایپ منع نمی‌کند»).
- **انتخاب گروه‌بندی‌شده:** یک Mood و یک LightingStyle از فهرست گروه‌بندی‌شده
  (بر اساس دسته) انتخاب و ذخیره‌ی صحیح مقدار تأیید شد.

`HomeProjectsStudioFlowTest.kt` (موجود، به‌روزرسانی شد): تست عمومی «۴ Tab
قابل‌سوییچ» چون Tab «DNA» دیگر Placeholder نیست، حالا محتوای واقعی
(`dna.group.coreIdentity`) را روی DNA بررسی می‌کند و برای اثبات «هنوز Placeholder»
از Tab «صحنه‌ها» استفاده می‌کند؛ `projectDnaRepository` هم به `MainScaffold` این
تست و `AppNavigationTest.kt` تزریق شد (همان انضباط تزریق‌پذیری).

## Consequences

- **فاز ۲ واحد ۱۶ کامل شد** (هر ۳ قدم: Story Tab، AI Story Breakdown، DNA Tab).
- **کار آینده‌ی شناخته‌شده:** UI برای `forbiddenElements`/`mandatoryElements`/
  `maxShotDurationSeconds` (بخشی از `OutputConstraints`) هنوز ساخته نشده؛ شمارشگر
  تجمعی «همه‌ی شات‌های یک پروژه» (برای فعال‌سازی کامل Rule 1 با هشدار واقعی) هنوز
  وجود ندارد — هر دو به فاز/واحد بعدی (احتمالاً هم‌زمان با Shot Composer) موکول
  شدند.
