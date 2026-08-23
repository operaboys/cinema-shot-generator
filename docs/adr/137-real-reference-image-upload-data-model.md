# ADR-137: آپلود عکس مرجع واقعی Asset — زیرقدم ۱ از ۳: مدل داده + انتخاب/ذخیره‌ی Uri

## وضعیت

پذیرفته‌شده

## زمینه

این ADR شروع یک فیچر مستقل و تازه است — **کاملاً مجزا** از فیچر «پرامپت
ساخت عکس مرجع» (ADR-131 تا ۱۳۶) که تا اینجا تکمیل شده بود. آن فیچر فقط
**متن** پرامپت تولید عکس را می‌سازد (Quick/AI)، هرگز خودِ عکس را. این فیچر
جدید امکان آپلود واقعی یک **عکس مرجع** (فایل تصویری واقعی روی دستگاه) برای
هر Asset را اضافه می‌کند — سه زیرقدم مستقل: (۱) مدل داده + منطق
انتخاب/ذخیره‌ی Uri (همین قدم)، (۲) UI کارت لیست کتابخانه، (۳) اتصال به
پرامپت شات. این قدم فقط زیرقدم ۱ را می‌بندد.

### راستی‌آزمایی مستقل پیش از کدنویسی

طبق پیش‌بریفینگ معمار، `CharacterAsset.referenceImages: List<ReferenceImage>`
و `ReferenceImageDto` از قبل در کد وجود داشتند اما کاملاً یتیم بودند. با
grep/خواندن مستقیم فایل‌ها تأیید شد:

- `domain/asset/AssetModels.kt`: `ReferenceImage(localFilePath: String,
  description: String)` و فیلد `CharacterAsset.referenceImages` از قبل
  موجودند.
- `data/repository/AssetDto.kt`: `ReferenceImageDto` و
  `CharacterAssetDto.referenceImages` از قبل موجودند.
- `data/repository/DnaAssetMappers.kt`: نگاشت دوطرفه‌ی همین فیلد برای
  Character از قبل موجود است (خط ۱۷۳/۲۲۲ طبق پیش‌بریفینگ).
- هیچ‌جای دیگری (`LocationAsset`/`ObjectAsset`/`LocationAssetDto`/
  `ObjectAssetDto`/ViewModel/Screen/تست) این فیلد را نداشت — فقط در
  `AssetModels.kt`، `AssetDto.kt`، `DnaAssetMappers.kt`، و یک تست Repository
  ظاهر می‌شد؛ کاملاً منطبق با پیش‌بریفینگ، بدون هیچ مغایرتی.
- الگوی انتخاب فایل موجود `SettingsScreen.kt` (`chooseImageLauncher`) با
  `ActivityResultContracts.OpenDocument()` (نه `GetContent()`) +
  `takePersistableUriPermission` (در `runCatching`) با متن پیش‌بریفینگ عیناً
  یکسان بود — همان کامنت مستندشده‌ی «`GetContent()` معتبربودن Uri را بعد از
  Restart اپ تضمین نمی‌کند» در همان فایل موجود است.

هیچ مغایرتی بین پیش‌بریفینگ و کد واقعی یافت نشد.

## تصمیم

### مدل داده (بخش ۱)

نوع `ReferenceImage` موجود بازاستفاده شد (نه یک نوع تازه) — فیلد
`referenceImages: List<ReferenceImage> = emptyList()` به `LocationAsset` و
`ObjectAsset` هم اضافه شد (تصمیم قفل‌شده‌ی معمار: هر سه نوع Asset، نه فقط
Character). همان‌الگو در `LocationAssetDto`/`ObjectAssetDto`
(`List<ReferenceImageDto>`) و نگاشت دوطرفه‌ی `DnaAssetMappers.kt` تکرار شد —
داخل همان بلوب JSON موجود (`assetDataJson`)، بدون هیچ Migration رومی.

### ViewModel (بخش ۲)

هر سه ViewModel یک `referenceImages: StateFlow<List<ReferenceImage>>` (در
`applyLoadedAsset` پر می‌شود، برای Asset تازه خالی)، `addReferenceImage(uri:
String)` (افزودن با `description = ""`)، و `removeReferenceImage(index:
Int)` دارند.

**توضیح (description) عمداً کاربر-وارد-نمی‌کند و در لحظه‌ی افزودن ثابت
نمی‌شود** — طبق تصمیم صریح معمار، در لحظه‌ی `save()` از فیلدهای زنده‌ی فرم
دوباره محاسبه می‌شود (نه در `addReferenceImage()`)، تا اگر کاربر بعداً
name/توضیف را ویرایش کند، توضیح جدید منعکس شود:
- Character: `"${name} — ${physicalAppearance.toPromptString()}"`
- Location: `"${name} — ${description}"`
- Object: `"${name} — ${materialAndColor}"`

چون توضیح کل Asset را توصیف می‌کند (نه یک عکس خاص)، همین یک رشته‌ی محاسبه‌شده
روی همه‌ی عکس‌های فهرست آن Asset یکسان اعمال می‌شود.

### UI (بخش ۳)

هر سه Screen یک دکمه‌ی «افزودن عکس مرجع» با
`rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())`
دارند — عیناً هم‌الگو با `chooseImageLauncher` در `SettingsScreen.kt`
(`takePersistableUriPermission` در `runCatching`، محدود به `image/*`). فقط
یک دکمه + یک لیست متنی ساده (نام فایل استخراج‌شده از `Uri.lastPathSegment`،
با fallback به خودِ Uri) — Thumbnail واقعی و اتصال به کارت کتابخانه عمداً به
زیرقدم‌های ۲/۳ موکول شد.

`ReferenceImagesSection` یک Composable خصوصی (`private`) است که در هر سه
فایل Screen **تکرار** می‌شود (نه Promote به یک فایل مشترک) — چون
`AssetFormSupport.kt` در فهرست فایل‌های مجاز این قدم نبود، و این پروژه از
قبل همین الگوی تکرار عمدی را برای Composable های کوچک private دارد
(`NullableEnumDropdownField` در `CharacterAssetFormScreen.kt`).

## پیامدها

- `ImagePromptEngine.kt`/`ImagePromptValidation.kt`/`ImagePromptAiConnector.kt`/
  `PromptAssembly.kt`/`Renderer.kt`/`ShotModels.kt` دست‌نخورده ماندند — اتصال
  عکس مرجع به پرامپت شات کار زیرقدم ۳ است.
- هیچ UI کارت لیست کتابخانه‌ای اضافه نشد — کار زیرقدم ۲ است.
- بدون Migration رومی — همان الگوی بلوب JSON موجود.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`: موفق.
- ۶ تست تازه (۲ به‌ازای هر نوع Asset): افزودن/حذف با ایندکس صحیح، و `save()`
  که واقعاً `referenceImages` را با توضیح محاسبه‌شده‌ی درست از فیلدهای زنده‌ی
  فرم (نه از لحظه‌ی افزودن) ذخیره می‌کند.
- `CharacterAssetFormViewModelTest.kt`/`LocationAssetFormViewModelTest.kt`/
  `ObjectAssetFormViewModelTest.kt`/`AssetFormFlowTest.kt` (کل هر چهار
  کلاس)، `ui.assets.*`/`domain.asset.*`/`data.repository.*`/
  `UiStringsTest.kt`: بدون رگرسیون.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۱۰۰۸ تست، ۱۰۰۷ Pass، ۱
  Fail — `OutputDeliveryFlowTest`، از پیش در فهرست کلاس‌های ناپایدار
  (Flaky) مستندشده‌ی این پروژه (بدون ارتباط با این قدم). با اجرای مجدد و
  مجزا کامل Pass شد.
- `./gradlew :app:assembleDebug`: موفق.
