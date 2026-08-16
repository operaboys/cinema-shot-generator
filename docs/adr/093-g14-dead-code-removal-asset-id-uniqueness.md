# ADR-093: حذف کد مرده‌ی G14 — validateAssetIdUniqueness و validateBilingualCompleteness

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

## یافته‌ی جداشده — OutputComposer.kt کامل حذف نشد؛ فقط بخش مرده‌اش

قدم اجرایی اولیه همچنین حذف کل فایل `OutputComposer.kt` را درخواست کرده
بود، با این فرض که این فایل یک طرح قدیمی واحد ۱۴ است که هرگز جایگزین
صفحه‌ی واقعی Output Delivery نشده. بررسی کامل فایل + grep سراسری
`composeOutput`/`OutputPackage`/`BilingualPrompts`/`ExportFile` این فرض
را رد کرد: این چهار Symbol هنوز مستقیماً توسط `OutputDeliveryViewModel.kt`،
`OutputDeliveryScreen.kt`، `ExportFileWriter.kt`، و `BackupsViewModel.kt`
(ADR-089) استفاده می‌شوند. تنها بخش واقعاً مرده‌ی این فایل، تابع
`validateBilingualCompleteness` (۶ خط) بود — صفر فراخوان‌کننده (فقط دو
تست اختصاصی خودش در `OutputComposerTest.kt`).

طبق دستور صریح قدم اجرایی («اگر جای دیگری این دو مورد را استفاده می‌کند،
حذف نکن — در گزارش مشخص کن و منتظر بمان»)، ابتدا این بخش حذف **نشد** و
گزارش شد. با تأیید صریح کاربر در قدم بعدی، فقط همین تابع مرده + دو تست
اختصاصی‌اش (`OutputComposerTest.kt`) حذف شدند؛ بقیه‌ی فایل
(`OutputPackage`/`BilingualPrompts`/`ExportFile`/`composeOutput` و گروه
تست زنده‌ی `composeOutput`) دست‌نخورده ماند. سه ارجاع کامنتی موجود در
`OutputDeliveryScreen.kt:102`، `OutputDeliveryViewModelTest.kt:270`، و
`OutputDeliveryFlowTest.kt:385` همگی به `composeOutput`/فایل (نه به
`validateBilingualCompleteness`) اشاره دارند و همچنان درست‌اند — چون خودِ
فایل حذف نشد، ویرایشی لازم نبود.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` | موفق (هر دو مرحله) |
| `gradle :app:testDebugUnitTest` (`domain.asset.*`، `domain.outputdelivery.*`، `ui.outputdelivery.*`) | موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | شکست‌های نامرتبط (`OutputDeliveryFlowTest`، `AssetFormFlowTest`)، flaky شناخته‌شده‌ی محیط Sandbox؛ هر دو در اجرای مجزا موفق |
| `gradle :app:assembleDebug` | موفق |
