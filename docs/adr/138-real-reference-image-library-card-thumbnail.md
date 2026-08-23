# ADR-138: آپلود عکس مرجع واقعی Asset — زیرقدم ۲ از ۳: نمایش اولین عکس در کارت‌های لیست کتابخانه

## وضعیت

پذیرفته‌شده

## زمینه

ادامه‌ی زیرقدم ۱ از ۳ (ADR-137، که فقط مدل داده + منطق انتخاب/ذخیره‌ی Uri را
اضافه کرد). این زیرقدم فقط نمایش اولین `referenceImages` هر Asset را در
کارت‌های لیست کتابخانه (`AssetsScreen.kt`) اضافه می‌کند — طبق mockup رسمی
(`docs/design/README.md` بخش «۸. Assets Library»: «Asset cards: thumbnail,
name, tier badge…»). اتصال به پرامپت شات همچنان کار زیرقدم ۳ است.

### راستی‌آزمایی مستقل پیش از کدنویسی

- `AssetsScreen.kt` (کامل خوانده شد): کارت هر Asset از `ThumbnailPlaceholder()`
  (یک `Box` ساده ۵۶dp بدون عکس) استفاده می‌کرد — دقیقاً طبق پیش‌بریفینگ.
- `HomeScreen.kt`: `internal fun DecodedContentImage(uriString, modifier,
  contentScale)` واقعاً در خطوط ۵۹۸-۶۱۷ (نه ۵۹۸-۶۱۵ دقیق پیش‌بریفینگ — فقط
  یک خط اختلاف بی‌اهمیت، به‌خاطر خط کامنت اضافه‌ی بالای امضای تابع) تعریف
  شده و `internal` است — طبق تأیید مستقیم، `SettingsScreen.kt` همین تابع را
  دقیقاً با همین امضا از پکیج `ui.home` Import می‌کند (خط ۵۹). پس Import
  مستقیم از `ui.assets` هم بدون مانع واقعی ممکن بود — نیازی به انتقال تابع
  به یک پکیج سوم نبود.
- هر سه Screen فرم Asset (نسخه‌ی به‌روزشده‌ی زیرقدم ۱) خوانده شدند: هر سه
  دارای `ReferenceImagesSection` مشابه با فهرست متنی ساده هستند؛ هیچ‌کدام
  پیش‌نمایش گرافیکی نداشتند — دقیقاً طبق پیش‌بریفینگ.
- تنها مغایرت واقعی یافت‌شده: فایل تست واقعی `AssetsScreenTest.kt` نام
  ندارد — نام واقعی آن `AssetsScreenFlowTest.kt` است (پیش‌بریفینگ این را
  «AssetsScreenTest.kt» نامیده بود؛ صرفاً یک اختلاف نام‌گذاری کوچک،
  بدون اثر بر محتوای کار).

## تصمیم

### کارت‌های لیست کتابخانه (`AssetsScreen.kt`)

`AssetCard` یک پارامتر تازه‌ی `referenceImageUri: String? = null` گرفت. در
بدنه: اگر غیر-null، `DecodedContentImage` (وارد‌شده مستقیماً از `ui.home`،
بدون هیچ تغییری در خودِ `HomeScreen.kt`) با همان سایز/گردی گوشه‌ی
`ThumbnailPlaceholder` فعلی (۵۶dp، `RoundedCornerShape(12.dp)`،
`ContentScale.Crop`) رندر می‌شود؛ در غیر این صورت `ThumbnailPlaceholder`
بدون تغییر fallback می‌ماند. هر سه محل فراخوانی (`CharacterAssetList`/
`LocationAssetList`/`ObjectAssetList`) مقدار
`asset.referenceImages.firstOrNull()?.localFilePath` را پاس می‌دهند.

### پیش‌نمایش بزرگ‌تر در فرم ویرایش (هر سه Screen فرم Asset)

طبق یادداشت طراح («no real images are final… treat every image area as
needing real production photography/renders») و خواسته‌ی صریح معمار که
تصویر باید «واقعاً دیدنی باشه، نه بندانگشتی»: بالای فهرست متنی ساده‌ی
زیرقدم ۱ (داخل همان `ReferenceImagesSection` هر Screen)، اولین
`referenceImages` — در صورت وجود — با `DecodedContentImage` در سایز ۱۴۰dp
(هم‌اندازه کاشی پیش‌نمایش ۱۴۰px اشاره‌شده در کامنت خودِ `DecodedContentImage`)
نمایش داده می‌شود.

### یافته‌ی جدید تست (Semantics Merge)

`Card(onClick = ...)` در Compose یک مرز Merge سمانتیک است — فرزندان ساده
(یک `Box` بدون کنترل کلیک خودش، مثل `ThumbnailPlaceholder`) در تلاش Query
پیش‌فرض (Merged Tree، `onNodeWithTag` بدون `useUnmergedTree`) در سمانتیک
خودِ Card ادغام و از دید `onNodeWithTag` ناپیدا می‌شوند — برخلاف
`IconButton` (که خودش هم مرز Merge است، پس مستقل Query-پذیر می‌ماند؛
دقیقاً چرا `assetCardMenuButtonTag`/`assetCardDeleteMenuItemTag` بدون این
پرچم همیشه کار کرده‌اند). تست‌های تازه‌ی این قدم برای اولین‌بار یک تگ روی
یک `Box` غیرکلیک‌پذیر داخل `AssetCard` هدف می‌گیرند، پس اولین‌بار بود که
این رفتار کشف شد؛ رفع با `useUnmergedTree = true` در هر دو Query تازه.

## پیامدها

- `HomeScreen.kt` دست‌نخورده ماند — فقط از `DecodedContentImage` موجود
  Import/استفاده شد (طبق قانون فایل‌های قابل‌لمس این قدم).
- اتصال عکس مرجع به پرامپت شات (`PromptAssembly.kt`/`Renderer.kt`/
  `ShotModels.kt`) دست‌نخورده ماند — زیرقدم ۳.
- هیچ Migration رومی یا تغییر مدل داده در این قدم لازم نبود — زیرقدم ۱
  از قبل همه‌ی فیلدهای لازم را ساخته بود.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`: موفق.
- ۲ تست تازه در `AssetsScreenFlowTest.kt`: کارت بدون `referenceImages`
  همچنان `ThumbnailPlaceholder` را می‌سازد (`assertExists` — نه
  `assertIsDisplayed`، چون این تگ عضو یک `LazyColumn` است، هم‌الگو با
  `outfitRowTag(1).assertExists()` در `AssetFormFlowTest.kt`)؛ کارتی با
  `referenceImages` غیرخالی دیگر `ThumbnailPlaceholder` نمی‌سازد (با
  به‌روزرسانی واقعی همان Asset موجود `obj_lib_001` در دیتابیس درون‌حافظه‌ای
  تست، نه یک ردیف دوم فرضی — تا رفتار «Compose نشدن آیتم دوم در
  LazyColumn هنوز اسکرول‌نشده» نتیجه را مخدوش نکند). هر دو Query با
  `useUnmergedTree = true` (یافته‌ی بالا).
- `ui.assets.*`/`ui.home.*`/`ui.settings.*`/`domain.asset.*`/
  `UiStringsTest.kt`/`AppNavigationTest.kt`: بدون رگرسیون.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۱۰۱۰ تست، ۲ Fail —
  `AppNavigationTest`/`OutputDeliveryFlowTest`، هر دو از پیش در فهرست
  کلاس‌های ناپایدار (Flaky) مستندشده‌ی این پروژه (بدون ارتباط با این قدم).
  با اجرای مجدد و مجزا هر دو کامل Pass شدند.
- `./gradlew :app:assembleDebug`: موفق.
