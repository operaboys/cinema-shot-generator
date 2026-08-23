# ADR-131: زیرقدم ۱ از ۵ — مدل داده‌ی فیچر «پرامپت ساخت عکس مرجع»

## وضعیت

پذیرفته‌شده

## زمینه

این ADR شروع یک فیچر تازه و **کاملاً مستقل** است: «پرامپت ساخت عکس مرجع»
برای دارایی‌ها (Asset). این فیچر هیچ ارتباطی با موتور اصلی پرامپت ویدیو
(`domain/prompt/PromptAssembly.kt`/`Renderer.kt`) ندارد و طراحی شده تا
هرگز با آن قاطی نشود — پرامپت عکس مرجع صرفاً برای کمک به کاربر در ساخت
تصویر مرجع یک Asset (خارج از این اپ) است، نه بخشی از پرامپت نهایی
ویدیو. این زیرقدم (۱ از ۵) فقط مدل داده را اضافه می‌کند — بدون هیچ منطق
تولید پرامپت (Template یا AI) و بدون هیچ UI.

### راستی‌آزمایی مستقل الگوی descriptionFaPreview

طبق دستور کار، `AssetDto.kt` و `DnaAssetMappers.kt` پیش از شروع کار کامل
خوانده شدند تا الگوی دقیق `descriptionFaPreview` (ADR-122) شبیه‌سازی شود:

- در دامنه (`AssetModels.kt`): فیلد `Nullable` با پیش‌فرض `null`، مستقیم
  روی `CharacterAsset`/`LocationAsset`/`ObjectAsset`.
- در DTO (`AssetDto.kt`): همان فیلد، همان نام، همان پیش‌فرض — با کامنت
  توضیح‌دهنده که تصریح می‌کند این DTO فقط داخل ستون `assetDataJson`
  (جدول `assets`، `AssetEntity`) به‌صورت JSON خام ذخیره می‌شود، نه ستون
  تفکیک‌شده‌ی Room — پس **هیچ Room Migration لازم نیست**.
- در `DnaAssetMappers.kt`: هر دو جهت (`toDomain()`/`toDto()`) این فیلد را
  مستقیم نگاشت می‌کنند.

تمام این الگو دقیقاً تأیید شد و در این زیرقدم عیناً تکرار شد. هیچ
مغایرتی با پیش‌بریفینگ پیدا نشد.

## تصمیم

### فیلدهای تازه

چهار فیلد پرامپت عکس (`imagePromptQuick`/`imagePromptAi`/
`imagePromptFaPreview`/`imagePromptGeneratedAt`، همه `Nullable` با
پیش‌فرض `null`) به این سطوح اضافه شدند:

- **`Outfit`**: پرامپت عکس مخصوص همین لباس/ست — چون هر Outfit ظاهر
  بصری متفاوتی دارد و نیاز به عکس مرجع جدا دارد.
- **`CharacterAsset`**: پرامپت عکس شخصیت پایه/خنثی (بدون لباس داستانی)
  — مستقل از پرامپت عکس هر Outfit؛ کاربرد متفاوت (مرجع «هویت پایه»، نه
  «ظاهر در یک صحنه‌ی خاص»).
- **`LocationAsset`**/**`ObjectAsset`**: این دو نوع Asset ساختار Outfit
  ندارند، پس چهار فیلد فقط یک‌بار روی خودِ Asset لازم بود.

فیلد پنجم `updatedAt: Long?` به هر سه نوع Asset (`CharacterAsset`،
`LocationAsset`، `ObjectAsset`) اضافه شد — Asset Library تا این قدم هیچ
فیلد زمان آخرین ویرایش نداشت؛ زیرقدم‌های بعدی (۲ تا ۵) برای تشخیص
«پرامپت عکس قدیمی شده یا نه» به مقایسه‌ی این Timestamp با
`imagePromptGeneratedAt` نیاز خواهند داشت. `Outfit` این فیلد را ندارد
(فقط `CharacterAsset`/`LocationAsset`/`ObjectAsset`، طبق دستور کار
صریح — تشخیص «قدیمی‌شدن» در سطح کل Asset معنا دارد، نه هر Outfit جدا).

### چرا مستقیماً روی همان data class ها، نه یک نوع Wrapper تازه

دقیقاً هم‌الگو با `descriptionFaPreview` (ADR-122) — این فیلدها به‌عنوان
جزئیات تکمیلی همان Asset/Outfit موجود معنا دارند، نه یک مفهوم مستقل با
چرخه‌ی حیات جدا. افزودن یک `data class ImagePrompt` جدا فقط یک لایه‌ی
غیرضروری اضافه می‌کرد بدون فایده‌ی واقعی در این زیرقدم (که صرفاً مدل
داده است، بدون منطق تولید).

## پیامدها

- هیچ رفتار مشاهده‌پذیر تغییر نکرد — همه‌ی فیلدهای تازه `null` پیش‌فرض
  دارند؛ بدون UI، بدون منطق تولید پرامپت.
- بدون Room Migration — طبق همان دلیل `descriptionFaPreview`
  (`assetDataJson` یک Blob خام است).
- موتور اصلی پرامپت ویدیو (`PromptAssembly.kt`/`Renderer.kt`) کاملاً
  دست‌نخورده ماند — این فیلدها هیچ‌جا در آن مسیر خوانده نمی‌شوند.
- این فقط **زیرقدم ۱ از ۵** است. منطق تولید پرامپت عکس (Template/AI)، UI
  فرم Asset، و تشخیص «قدیمی‌شدن» بر اساس `updatedAt` هیچ‌کدام در این
  زیرقدم پیاده نشدند — زیرقدم‌های ۲ تا ۵.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`: موفق.
- تست‌های موجود Asset — `AssetValidationTest.kt` (۲۳)، `AssetModelsTest.kt`
  (۹)، `AssetSelectionTest.kt` (۱۰)، `AssetContinuityTest.kt` (۱۵)،
  `AssetRepositoryTest.kt` (۱۴)، `DtoMappersTest.kt` (۸)، و تمام
  `ui.assets.*` (`CharacterAssetFormViewModelTest`/`LocationAssetFormViewModelTest`/
  `ObjectAssetFormViewModelTest`/`AssetFormFlowTest`/`AssetsScreenFlowTest`):
  همه Pass، بدون رگرسیون — دقیقاً طبق انتظار Backward Compatible بودن
  فیلدهای Nullable با پیش‌فرض null.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۹۵۸ تست، ۹۵۷ Pass،
  ۱ Fail — `OutputDeliveryFlowTest`، از پیش در فهرست کلاس‌های ناپایدار
  (Flaky) مستندشده‌ی این پروژه (بدون هیچ ارتباطی با این تغییرات — تحویل
  خروجی، نه Asset). با اجرای مجدد و مجزا کامل Pass شد — تأیید شد
  Flakiness شناخته‌شده است، نه رگرسیون واقعی.
- `./gradlew :app:assembleDebug`: موفق.
