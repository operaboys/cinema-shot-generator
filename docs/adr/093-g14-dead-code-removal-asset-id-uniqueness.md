# ADR-093: حذف کد مرده‌ی G14 — validateAssetIdUniqueness

## زمینه

طبق جدول تصمیمات ADR-064 (بخش ب، ممیزی G14، دسته‌ی «ج: منسوخ، کاندید
حذف»)، `validateAssetIdUniqueness` (واحد ۰۶، `AssetValidation.kt`) هیچ‌گاه
از UI/ViewModel صدا زده نشده بود. بررسی (grep سراسری `app/src`) تأیید کرد:
صفر فراخوان‌کننده‌ی واقعی — تنها ارجاع، تعریف خودِ تابع + دو تست اختصاصی
در `AssetValidationTest.kt` بود.

دلیل ریشه‌ای صفر-فراخوان: `assetId` هرگز توسط کاربر وارد نمی‌شود؛ همیشه با
UUID خودکار (`generateAssetFormId`) ساخته می‌شود، پس یکتایی از قبل توسط
همان تولید UUID تضمین است و این Rule عملاً هرگز موضوعیت نداشت.

## تصمیم

- تابع `validateAssetIdUniqueness` از `AssetValidation.kt` کامل حذف شد.
- دو تست اختصاصی آن (`rule1 duplicate asset id is blocking`،
  `rule1 unique asset id is valid`) از `AssetValidationTest.kt` کامل حذف
  شدند (نه غیرفعال).
- کامنت سربرگ فایل به‌روزرسانی شد تا شمارش Rule‌ها را منعکس کند و دلیل
  حذف را ثبت کند.

## یافته‌ی جداشده — OutputComposer.kt حذف نشد

قدم اجرایی همچنین حذف کل فایل `OutputComposer.kt` (شامل
`validateBilingualCompleteness`) را درخواست کرده بود، با این فرض که این
فایل یک طرح قدیمی واحد ۱۴ است که هرگز جایگزین صفحه‌ی واقعی Output Delivery
نشده. بررسی کامل فایل + grep سراسری `composeOutput`/`OutputPackage`/
`BilingualPrompts`/`ExportFile` این فرض را رد کرد: این چهار Symbol
هنوز مستقیماً توسط `OutputDeliveryViewModel.kt`، `OutputDeliveryScreen.kt`،
`ExportFileWriter.kt`، و `BackupsViewModel.kt` (ADR-089) استفاده می‌شوند.
تنها بخش واقعاً مرده‌ی این فایل، تابع `validateBilingualCompleteness`
(۶ خط) است — که صفر فراخوان‌کننده دارد (فقط دو تست اختصاصی خودش در
`OutputComposerTest.kt`).

طبق دستور صریح قدم اجرایی («اگر جای دیگری این دو مورد را استفاده می‌کند،
حذف نکن — در گزارش مشخص کن و منتظر بمان»)، این بخش حذف **نشد** و منتظر
تأیید کاربر برای یک نسخه‌ی محدودتر (فقط حذف `validateBilingualCompleteness`
+ دو تستش) ماند.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`domain.asset.*`) | موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۱ شکست نامرتبط (`OutputDeliveryFlowTest`، flaky شناخته‌شده‌ی محیط Sandbox؛ در اجرای مجزا موفق) |
| `gradle :app:assembleDebug` | موفق |
