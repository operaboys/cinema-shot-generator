# ADR-128: حذف کامل مفهوم mandatoryElements — نه فقط Rule یتیم، بلکه یک مفهوم تکراری با qualityTags

## وضعیت

پذیرفته‌شده

## زمینه

**این حذف مفهومی است، نه فقط حذف کد یتیم** (برخلاف ADR-127 که یک Rule
بی‌فایده‌ی معماری را حذف کرد، اما مفهوم زیرینش — نسبت تصویر — همچنان کاملاً
معتبر و استفاده‌شده باقی ماند). این ADR کل مفهوم «Mandatory Elements» —
هم Rule (`checkMandatoryElementsPresent`) و هم خودِ فیلد
(`OutputConstraints.mandatoryElements`) — را از پروژه حذف می‌کند، چون خودِ
مفهوم زائد است.

### راستی‌آزمایی مستقل

با `grep` مستقل تأیید شد `checkMandatoryElementsPresent`
(`domain/dna/DnaValidation.kt`) هیچ فراخوان‌کننده‌ی واقعی نداشت. کامنت
خودِ تابع صریحاً می‌گفت «اتصال واقعی به Finalize شدن یک Shot خارج از
Scope است» — اما بررسی عمیق‌تر نشان داد این تعلیق موقت هرگز قرار نبود
به یک اتصال واقعی برسد: مقادیر `mandatoryElements` (طبق نمونه‌ی بلوپرینت
اصلی، مثل `"atmospheric_depth"`) برچسب‌های کیفی/توصیفی متن آزادند، نه
شناسه‌ی قابل‌اندازه‌گیری در برابر یک Shot واقعی — برخلاف `forbiddenElements`
که با مقادیر enum واقعی (camera/lighting/weather) مقایسه می‌شود و واقعاً
در `ValidationAggregator.kt` وصل است.

**دو دور تحقیق مستقل معمار** در منابع صنعت prompt engineering
تصویر/ویدیو (ازجمله راهنمای رسمی Luma) تأیید کرد: مفهوم «Mandatory
Elements» به‌عنوان یک دسته‌ی جدا و متمایز از «Quality Tags»/«Master
References» در صنعت وجود ندارد — هر منبع بررسی‌شده دقیقاً همان الگویی را
توصیف می‌کند که در این پروژه با `QualityDirectives.qualityTags` از قبل
پیاده و کاملاً فعال است (استفاده‌ی واقعی تأییدشده در
`PromptAssembly.kt`/`Renderer.kt`). یعنی `mandatoryElements` نه فقط
یتیم، بلکه از نظر مفهومی یک مفهوم **تکراری/زائد** با یک فیچر از‌قبل‌کارآمد
همین پروژه بود — نه یک فیچر ناقص که باید تکمیل شود.

### ارجاع به تصمیمات قدیمی‌تر (این ADR آن‌ها را می‌بندد)

- **ADR-055**: `checkMandatoryElementsPresent` را در فهرست Rule های
  عمداً وایرنشده مستند کرد («تعریف مبهم عناصر شامل‌شده»)، اما بدون تصمیم
  قطعی رهایش کرد.
- **ADR-064** (تصمیم مشابه برای `validateShotAspectRatio`، G14): همان
  الگوی تحلیل — رهاشدن با شرط «مگر آینده‌ای اضافه کند» — بدون تصمیم
  قطعی حذف یا اتصال برای این Rule هم اتخاذ نکرد.

این ADR هر دو ابهام قدیمی را رسماً می‌بندد: **حذف، نه اتصال، نه رهاسازی
بیشتر.**

## تصمیم

**حذف کامل** — هم Rule و هم خودِ فیلد `mandatoryElements` — از سراسر
کدبیس:

1. `domain/dna/ProjectDna.kt`: فیلد `mandatoryElements` از
   `OutputConstraints` حذف شد.
2. `domain/dna/DnaValidation.kt`: تابع `checkMandatoryElementsPresent`
   کامل حذف شد.
3. `data/repository/ProjectDnaDto.kt`: فیلد معادل از `OutputConstraintsDto`
   حذف شد.
4. `data/repository/DnaAssetMappers.kt`: دو خط نگاشت (`toDomain`/`toDto`)
   حذف شدند.
5. `ui/dna/DnaViewModel.kt`: `addMandatoryElement`/`removeMandatoryElement`
   و مقدار پیش‌فرض در `defaultProjectDna` حذف شدند.
6. `ui/dna/DnaTabContent.kt`: فراخوانی و کل تابع `MandatoryElementsSection`
   (Composable خودکفا، تنها مصرف‌کننده‌ی همان یک فراخوانی) حذف شدند؛
   `fieldCount` گروه «محدودیت‌های خروجی» از ۴ به ۳ اصلاح شد (چهار فیلد
   قبلی رندرشده اکنون سه‌تاست: aspectRatio، maxShotDuration،
   forbiddenElements).
7. `ui/i18n/UiStrings.kt`: هر ۴ کلید `dna.outputConstraints.mandatoryElements*`
   از هر دو نقشه (فارسی/انگلیسی، ۸ خط) حذف شدند.

### `Json { ignoreUnknownKeys = true }` — بدون Migration رسمی

`ProjectDnaRepository.kt` از قبل `ignoreUnknownKeys = true` فعال دارد؛
حذف این فیلد از `OutputConstraintsDto` کاملاً امن است — پروژه‌های
موجود کاربر که این کلید را در JSON ذخیره‌شده دارند بدون خطا Decode
می‌شوند (کلید اضافی نادیده گرفته می‌شود). این ADR هیچ Migration رسمی
تازه‌ای اضافه نکرد — تأیید شد از قبل درست است، مطابق دستور کار صریح.

## پیامدها

- `qualityTags`/`QualityDirectives` کاملاً دست‌نخورده ماندند — این تنها
  فیچر معتبر برای «آنچه باید همیشه در پرامپت حاضر باشد» در این پروژه
  است.
- `forbiddenElements`/`maxShotDurationSeconds`/`aspectRatio` (بقیه‌ی
  `OutputConstraints`) دست‌نخورده ماندند.
- تست‌های زیادی که `OutputConstraints(...)` می‌ساختند (هم با نام‌گذاری
  صریح پارامتر و هم — یافته‌ی واقعی این قدم — چند مورد با آرگومان
  موقعیتی که با grep ساده‌ی کلمه‌ی `mandatoryElements` دیده نمی‌شدند:
  `ValidationOverrideFlowTest.kt`، `PromptGenerationRepositoryTest.kt`،
  `CinematicLanguageTest.kt`، `ValidationAggregatorTest.kt`) به‌روزرسانی
  شدند.
- یک تست E2E ترکیبی (`DnaTabFlowTest.kt`) که سه فیلد را هم‌زمان تست
  می‌کرد بازنویسی شد — بخش mandatoryElements حذف، بخش forbiddenElement/
  maxShotDuration دست‌نخورده باقی ماند.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`:
  موفق در همان اولین تلاش — تأییدی مستقل که همه‌ی نقاط ارجاع واقعاً پیدا
  و اصلاح شدند.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۹۴۹ تست (۲ کمتر از قدم
  قبلی، دقیقاً هم‌اندازه‌ی دو تست حذف‌شده‌ی `checkMandatoryElementsPresent`)،
  ۱ شکست — از کلاس ناپایدار از‌قبل‌مستندشده‌ی `OutputDeliveryFlowTest`
  (مستندشده از ADR-044، ریشه‌یابی‌شده ADR-070/071)؛ در اجرای ایزوله دوباره
  Pass شد. `DnaTabFlowTest.kt` (شامل تست بازنویسی‌شده) و
  `DnaValidationTest.kt` هر دو کامل در اجرای ایزوله Pass شدند.
- `./gradlew :app:assembleDebug`: موفق.
