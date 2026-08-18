# ADR-117: اتصال کامل Style Matrix — قدم ۴-اتصال (آخرین زیرقدم): اتصال واقعی به PromptAssembly.kt؛ کل زنجیره «اتصال کامل Style Matrix» به‌طور کامل و End-to-End بسته شد

## زمینه

این آخرین زیرقدم از برنامه‌ی ۸ زیرقدمی «اتصال کامل Style Matrix» است.
تا این قدم، `styleModifiers` در `PromptAssembly.kt` فقط نام خام enum
سبک اصلی بود (`"${dominantVisualStyle} style"`، مثلاً `"CINEMATIC_STYLE
style"`) و `secondaryStyle`/`influence` (ADR-113) هیچ اثری روی پرامپت
خروجی نهایی نداشتند — مقادیری که کاربر در تب DNA (ADR-115) انتخاب
می‌کرد، تا این‌جا کاملاً بی‌اثر بودند.

## بررسی مستقل — تأیید کامل پیش‌بریفینگ، بدون هیچ تفاوتی

با خواندن کامل `PromptAssembly.kt` و `PromptAssemblyTest.kt` (خصوصاً
`sampleDna()`) پیش از نوشتن هر خط کد: خط `styleModifiers` دقیقاً همان
متن پیش‌بینی‌شده در خط ۵۳ بود؛ `sampleDna().coreIdentity.dominantVisualStyle`
واقعاً `VisualStyle.CINEMATIC_STYLE` بود (نه فرض، تأییدشده مستقیم) —
پس جایگزین پیشنهادی خط ۲۱۳ تست (`contains("cinematic style")`) درست
بود. `secondaryStyle`/`influence` در `sampleDna()` تنظیم نشده بودند
(پیش‌فرض `null`)، پس رفتار پیشین این تست فقط توسط سبک اصلی تعیین
می‌شد.

**بررسی گسترده‌تر مستقل (طبق دستور صریح):** با `grep` روی کل
`app/src`، تنها مصرف‌کننده‌ی `assemblePromptBlueprint` که واقعاً روی
`styleModifiers` Assert می‌کند `PromptAssemblyTest.kt` بود.
`RendererTest.kt`، `ModelProfilesTest.kt`، `WorkflowModelsTest.kt` هر
سه `StructuredParts(styleModifiers = "...")` را مستقیماً و دستی
می‌سازند (نه از `assemblePromptBlueprint`) — این پیش‌بینی معمار با
اجرای واقعی کل Suite تأیید شد: هر سه فایل بدون هیچ تغییری سبز ماندند.

## پیاده‌سازی

```kotlin
val primaryStyleRef = input.dna.coreIdentity.dominantVisualStyle.toStyleReference()
val secondaryStyleRef = input.dna.coreIdentity.secondaryStyle?.toStyleReference()
val combinedStyle = combineStyles(primaryStyleRef, secondaryStyleRef, input.dna.coreIdentity.influence)
// ...
styleModifiers = "$combinedStyle, ${input.dna.masterPalette.colorGradingPreset}"
```

سه تابع/فیلد موجود از قدم‌های قبلی — `VisualStyle.toStyleReference()`
(ADR-116)، `combineStyles` (از ابتدا در `StyleMatrix.kt`، دست‌نخورده
در تمام قدم‌های قبلی)، `CoreIdentity.secondaryStyle`/`influence`
(ADR-113) — بدون هیچ بازنویسی، فقط فراخوانی شدند. `colorGradingPreset`
همچنان در انتها اضافه می‌شود، دقیقاً هم‌الگوی قبلی.

## تست

**`PromptAssemblyTest.kt`:** خط ۲۱۳ (`contains("CINEMATIC")`) به
`contains("cinematic style")` تغییر کرد — رفتار جدید و صحیح، نه چیزی
که باید حفظ می‌شد. دو تست تازه: یکی اثبات می‌کند وقتی
`secondaryStyle`/`influence` هر دو ست باشند، `styleModifiers` واقعاً
هم `promptTokens` سبک ثانویه و هم Modifier درست
(`"strongly influenced by"`) را دارد؛ دیگری اثبات می‌کند وقتی
`secondaryStyle` تنظیم نشده، `styleModifiers` دقیقاً برابر
`combineStyles(primary, null, null) + ", " + colorGradingPreset` است
(نه چیز دیگری).

**`StyleMatrixConnectionEndToEndTest.kt` (فایل تازه) — بخش End-to-End
Verification الزامی این قدم:** برخلاف تست‌های بالا (که یک `ProjectDna`
دستی می‌سازند)، این تست از خودِ `DnaViewModel` واقعی
(`setSecondaryVisualStyle`/`setStyleInfluence`) + `ProjectDnaRepository`
واقعی Room (ذخیره/بارگذاری واقعی، نه شبیه‌سازی‌شده) استفاده می‌کند —
دقیقاً همان مسیری که یک کاربر واقعی در تب DNA طی می‌کند:

1. `DnaViewModel.setSecondaryVisualStyle(VisualStyle.STUDIO_GHIBLI)` +
   `setStyleInfluence(StyleInfluence.STRONG)` — متدهای واقعی ADR-113.
2. Poll تا ذخیره‌ی Async واقعاً در دیتابیس Room بنشیند (همان یافته‌ی
   مستندشده‌ی race در `DnaViewModelTest.kt`، ADR-113).
3. بارگذاری مجدد `ProjectDna` از `ProjectDnaRepository` واقعی (نه یک
   شیء دامنه‌ای دستی).
4. فراخوانی واقعی `assemblePromptBlueprint` با این DNA بارگذاری‌شده.
5. تأیید صریح: `styleModifiers` نهایی هم `promptTokens` سبک اصلی
   (`CINEMATIC_STYLE`، پیش‌فرض) و هم `promptTokens` سبک ثانویه
   (`STUDIO_GHIBLI`) و هم Modifier درست (`"strongly influenced by"`،
   بر اساس `StyleInfluence.STRONG`) را دارد.

این تست تنها راه اثبات واقعی است که کل زنجیره‌ی ۸ زیرقدم
(UI/ViewModel از ADR-113/115 → Repository/Room واقعی → domain
`toStyleReference`/`combineStyles` از ADR-116 → `PromptAssembly` از
همین ADR) واقعاً به‌هم وصل است، نه فقط لایه‌های ایزوله.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`PromptAssemblyTest`، مجزا) | ۸ تست (۶+۲ تازه)، موفق |
| `gradle :app:testDebugUnitTest` (`StyleMatrixConnectionEndToEndTest`، مجزا) | ۱ تست End-to-End، موفق |
| `gradle :app:testDebugUnitTest` (`RendererTest`/`ModelProfilesTest`/`WorkflowModelsTest`، مجزا) | بدون هیچ تغییر، همگی سبز — پیش‌بینی معمار تأیید شد |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۹۰۱ تست (۸۹۸+۳ تازه)، ۱ شکست نامرتبط (`OutputDeliveryFlowTest`) — در مسیر وابستگی این قدم نیست؛ در اجرای مجزا موفق؛ همان کلاس Flake محیطی مستندشده از ADR-044 تا کنون |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این قدم (عمداً)

`StyleMatrix.kt` (تمام توابع)، `CoreIdentity`، `DnaTabContent.kt`،
`DnaViewModel.kt` — همگی دست‌نخورده؛ زیرساخت لازم از قدم‌های قبلی
کاملاً آماده بود، این قدم فقط اتصال نهایی بود.

## نتیجه — کل برنامه‌ی «اتصال کامل Style Matrix» به‌طور کامل و End-to-End بسته شد

با این قدم، زنجیره‌ی کامل ADR-113 تا ADR-117 بسته شد:

- **ADR-113 (قدم ۱الف):** `secondaryStyle`/`influence` به `CoreIdentity`
  واقعی اضافه شد (مدل داده + DTO + Mapper + ViewModel).
- **ADR-114 (قدم ۲الف):** `checkStyleCompatibility` با ماتریس واقعی
  سازگاری (تحقیق‌شده، نه Placeholder) وصل شد؛ `StyleMatrix` از
  `StyleReference` منسوخ به `VisualStyle` مستقیم مهاجرت کرد.
- **ADR-115 (قدم ۳):** UI انتخاب Secondary Style + Influence + نمایش
  زنده‌ی هشدار ناسازگاری در تب DNA.
- **ADR-116 (قدم ۴-محتوا):** `promptTokens` غنی و واقعی (تحقیق‌شده در
  منابع صنعت prompt engineering) برای هر ۳۴ مقدار `VisualStyle`.
- **ADR-117 (قدم ۴-اتصال، همین ADR):** اتصال نهایی همه‌ی موارد بالا
  به `PromptAssembly.kt` — خروجی واقعی پرامپت نهایی اکنون واقعاً هم
  سبک اصلی، هم سبک ثانویه (با شدت تأثیر درست) را منعکس می‌کند.

برای اولین بار، یک کاربر واقعی می‌تواند سبک ثانویه و شدت تأثیر آن را
در تب DNA تنظیم کند و ببیند این انتخاب — از طریق زنجیره‌ی کامل
UI → ViewModel → Repository/Room → دامنه (`checkStyleCompatibility`،
`toStyleReference`، `combineStyles`) → `PromptAssembly` — واقعاً وارد
پرامپت نهایی ارسالی به مدل‌های تولید تصویر/ویدیو می‌شود؛ تست
End-to-End این ADR این را با یک سناریوی واقعی (نه شبیه‌سازی‌شده) اثبات
می‌کند.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
