# ADR-140: حذف کامل سه Rule یتیم اعتبارسنجی فایل عکس — نه فقط یتیم، بلکه محصول یک مسیر معماری کنارگذاشته‌شده

## وضعیت

پذیرفته‌شده

## زمینه

**این حذف مفهومی است، نه فقط حذف کد یتیم** — دقیقاً هم‌الگو با ADR-128
(حذف کامل مفهوم `mandatoryElements`). این ADR سه تابع اعتبارسنجی —
`validateImageFile`/`validateReferenceImageFile`
(`domain/asset/AssetValidation.kt`) و `validateImageReferenceFile`
(`domain/shot/ShotValidation.kt`) — را کامل از پروژه حذف می‌کند، چون
سناریویی که برایش طراحی شده بودند دیگر با معماری فعلی پروژه هم‌راستا
نیست.

این سه تابع برای سناریویی طراحی شده بودند که کاربر یک فایل عکس واقعی از
حافظه‌ی گوشی (`filePath`/`fileSizeBytes`/`mimeType`) مستقیم آپلود
می‌کند و آن فایل (فرمت، حجم، سلامت، وجود روی دیسک) اعتبارسنجی می‌شود.
دو فیچر بعدی نشان دادند این سناریو هرگز به این شکل پیاده نشد و نباید
هم می‌شد:

- فیچر «پرامپت ساخت عکس مرجع» (ADR-131 تا ۱۳۶) ثابت کرد اپ فقط **متن**
  پرامپت تولید عکس می‌سازد؛ خودِ عکس همیشه بیرون از اپ (توسط ابزار
  تولید تصویر) ساخته می‌شود.
- فیچر «آپلود عکس مرجع واقعی Asset» (ADR-137 تا ۱۳۹، که مستقیماً همان
  نیاز واقعی — نمایش/اتصال عکس مرجع واقعی — را حل کرد) از یک الگوی
  کاملاً متفاوت استفاده کرد: Storage Access Framework
  (`ActivityResultContracts.OpenDocument()` + `takePersistableUriPermission`)
  با ذخیره‌ی مستقیم رشته‌ی `Uri`، **بدون** کپی فایل، بدون اعتبارسنجی
  MIME/حجم/سلامت فایل، و بدون پیام‌های BLOCKING مسدودکننده.
  `ReferenceImage.localFilePath` در عمل یک `Uri` پایدار است، نه یک مسیر
  فایل محلی که `fileExists`/`fileSizeBytes`/`mimeType` روی آن معنای
  Rule ۶/۶ب/۴ قدیمی را داشته باشد.

پس این سه تابع محصول یک مسیر معماری کنارگذاشته‌شده‌اند، نه صرفاً کاری
که فراموش شده وصل شود.

### راستی‌آزمایی مستقل

با `grep` مستقیم روی کل `app/src/main/` و `app/src/test/` (نه صرفاً
اتکا به راستی‌آزمایی قبلی گفتگو) تأیید شد هر سه تابع **صفر فراخوان‌کننده‌ی
واقعی** دارند — فقط تعریف خودشان، تست‌های مستقیم‌شان، و یک کامنت
مرجع در `ShotValidation.kt`/`ValidationAggregator.kt`. هیچ فراخوانی از
قلم‌افتاده یافت نشد.

### ارجاع به تصمیمات قدیمی‌تر (این ADR آن‌ها را می‌بندد)

- **ADR-055**: `validateImageReferenceFile` را در فهرست Rule های عمداً
  وایرنشده‌ی `ValidationAggregator.kt` مستند کرد، بدون تصمیم قطعی حذف
  یا اتصال.

این ADR آن ابهام قدیمی را رسماً می‌بندد: **حذف، نه اتصال، نه رهاسازی
بیشتر.**

## تصمیم

**حذف کامل** — هر سه تابع و نوع کمکی‌شان — از سراسر کدبیس:

1. `domain/asset/AssetValidation.kt`: `validateImageFile` و
   `ImageValidationResult` (فقط همین تابع استفاده‌شان می‌کرد) و
   `validateReferenceImageFile` کامل حذف شدند. کامنت سرفایل («Rule ۲،
   ۳، ۵، ۶، ۶ب، ۷، …») به «Rule ۲، ۳، ۵، ۷، …» اصلاح شد و یک بند تازه
   با ارجاع به همین ADR اضافه شد.
2. `domain/shot/ShotValidation.kt`: `validateImageReferenceFile` کامل
   حذف شد. کامنت سرفایل («Rule 1 تا Rule 4») به «Rule 1 تا Rule 3»
   اصلاح شد و یک بند تازه با ارجاع به همین ADR اضافه شد.
3. `domain/validation/ValidationAggregator.kt`: کامنت مرجع بالای
   `aggregateShotValidation` — که `validateImageReferenceFile` را در
   فهرست Rule های عمداً وایرنشده می‌آورد — به‌روزرسانی شد؛ دقیقاً
   هم‌الگو با جمله‌ای که قبلاً برای `checkMandatoryElementsPresent`
   نوشته شده بود («بعداً کامل حذف شد — نه فقط یتیم، بلکه …»).
4. تست‌های مستقیم این سه تابع از `AssetValidationTest.kt`
   (۷ تست: ۴ تای `validateImageFile` + ۳ تای Rule ۶/۶ب) و
   `ShotValidationTest.kt` (۲ تست Rule ۴) حذف شدند — نه Skip، حذف
   کامل، چون تابعی که تست می‌کردند دیگر وجود ندارد. Import یتیم
   `assertFalse` (که فقط تست‌های حذف‌شده استفاده می‌کردند) هم از
   `AssetValidationTest.kt` حذف شد.

## پیامدها

- بقیه‌ی Rule های `AssetValidation.kt` (۳، ۵، ۷، ۱۰، ۱۱، ۱۲) و
  `ShotValidation.kt` (۱، ۲، ۳، ۵، ۸) دست‌نخورده ماندند.
- `AssetImageReferenceCollector.kt`/`PromptAssembly.kt` (فیچر «آپلود
  عکس مرجع واقعی Asset») دست‌نخورده ماندند — آن مسیر از این حذف کاملاً
  مستقل است.
- شماره‌گذاری Rule در کامنت‌های سرفایل شکاف پیدا کرد («Rule ۲، ۳، ۵،
  ۷» به‌جای دنباله‌ی پیوسته) — همان الگوی پذیرفته‌شده‌ی قبلی این پروژه
  برای Rule های حذف‌شده (مثل حذف Rule ۱ در ADR-093).

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`:
  موفق در همان اولین تلاش — تأییدی مستقل که همه‌ی نقاط ارجاع واقعاً
  پیدا و اصلاح شدند.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۱۰۱۰ تست (۹ کمتر از
  قدم قبلی، دقیقاً هم‌اندازه‌ی ۹ تست حذف‌شده)، ۱ شکست —
  `OutputDeliveryFlowTest`، از پیش در فهرست کلاس‌های ناپایدار (Flaky)
  مستندشده‌ی این پروژه (بدون ارتباط با این قدم)؛ در اجرای مجزا کامل
  Pass شد.
- `domain.asset.*`/`domain.shot.*`/`domain.validation.*`: بدون
  رگرسیون.
- `./gradlew :app:assembleDebug`: موفق.
