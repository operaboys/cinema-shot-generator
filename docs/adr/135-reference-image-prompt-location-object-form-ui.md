# ADR-135: زیرقدم ۵ از ۵ (پایانی) — اتصال «پرامپت ساخت عکس مرجع» به فرم مکان/شیء

## وضعیت

پذیرفته‌شده

## زمینه

ادامه‌ی ADR-131 (مدل داده)، ADR-132 (موتور Template)، ADR-133 (مسیر AI) و
ADR-134 (اتصال به CharacterAssetForm). این زیرقدم `LocationAssetForm` و
`ObjectAssetForm` را سیم‌کشی می‌کند — هر دو ساختار ساده‌تری از Character
دارند (بدون Outfit، یک پرامپت تصویر واحد به‌ازای هر Asset). با این قدم، کل
فیچر مستقل «پرامپت ساخت عکس مرجع» (زیرقدم ۱ تا ۵) کامل می‌شود.

### راستی‌آزمایی مستقل

پیش از نوشتن کد، `LocationAssetFormViewModel.kt`، `LocationAssetFormScreen.kt`،
`ObjectAssetFormViewModel.kt`، `ObjectAssetFormScreen.kt`، و کل
`CharacterAssetFormViewModel.kt`/`CharacterAssetFormScreen.kt` (نسخه‌ی
به‌روزشده‌ی زیرقدم ۴، commit `57331a1`) کامل خوانده شدند:

- تأیید شد هر دو ViewModel هم‌الگوی دقیق `CharacterAssetFormViewModel.kt`
  هستند (بدون Outfit) — طبق پیش‌بریفینگ.
- تأیید شد هیچ‌کدام `updatedAt` را در `save()` ست نمی‌کردند — طبق
  پیش‌بریفینگ، رفع شد (پایین‌تر).
- نام دقیق توابع Template زیرقدم ۲ با `grep` مستقیم روی
  `ImagePromptEngine.kt` تأیید شد: `buildLocationImagePrompt(location,
  projectDna)`/`buildObjectImagePrompt(objectAsset, projectDna)` — دقیقاً
  همان نام‌هایی که پیش‌بریفینگ حدس زده بود؛ هیچ مغایرتی نبود.
- با خواندن کامل `ImagePromptEngine.kt` تأیید شد `buildLocationImagePrompt`
  فقط `description`/`environment.{type,size,lightingCondition}`/
  `keyElements`/`basePrompt` را می‌خواند (نه `name`/`locationType`/...) و
  `buildObjectImagePrompt` فقط `description`/`size`/`materialAndColor`/
  `specialTrait`/`basePrompt` را (نه `name`/`subtype`) — این یافته مستقیماً
  ساختار Preview زنده‌ی هر دو ViewModel را تعیین کرد (پایین‌تر).
- طبق تصمیم مستند و پذیرفته‌شده‌ی زیرقدم ۴، توابع اعتبارسنجی
  `validateLocationImagePromptInputs`/`validateObjectImagePromptInputs`
  (ADR-132) در این زیرقدم هم مستقیماً به UI وصل نشدند (فقط Warning هستند،
  نه Blocking) — برای یکدستی با تصمیم Character.

هیچ مغایرت دیگری با پیش‌بریفینگ پیدا نشد.

## تصمیم

### DRY کوچک: `styleTokensForImagePrompt` مشترک (فایل تازه)

زیرقدم ۴ به‌ناچار یک نسخه‌ی `private` از `styleTokensOf` (که در
`ImagePromptEngine.kt` طبق قانون این فیچر private و غیرقابل‌تغییر است) در
`CharacterAssetFormViewModel.kt` ساخته بود. پیش از افزودن نسخه‌ی سوم برای
Location/Object، این زیرقدم منطق را در یک فایل تازه‌ی مشترک
`domain/asset/ImagePromptStyleTokens.kt` (تابع public
`styleTokensForImagePrompt(projectDna)`) متمرکز کرد؛ نسخه‌ی محلی
`CharacterAssetFormViewModel.kt` حذف و به این تابع مشترک تغییر داده شد. هر
سه ViewModel اکنون از همین یک نسخه استفاده می‌کنند — یک بهبود کوچک DRY، بدون
تغییر رفتاری (تأییدشده با کامپایل/تست کامل `CharacterAssetFormViewModelTest.kt`
بدون تغییر، همه Pass).

### ViewModel ها (هم‌الگو دقیق با `CharacterAssetFormViewModel`، بدون Outfit)

هر دو ViewModel: `ProjectDnaRepository` تزریق‌پذیر (پیش‌فرض از
`AppDatabase`)؛ StateFlow های `imagePromptQuick`/`imagePromptAi`/
`imagePromptFaPreview`/`imagePromptGeneratedAt`/`imagePromptAiInProgress`/
`imagePromptAiError`؛ `isImagePromptStale(generatedAt)` با همان فرمول
زیرقدم ۴ (`loadedUpdatedAt` مقایسه‌شده با زمان تولید هر پرامپت)؛ `save()`
اکنون `updatedAt = System.currentTimeMillis()` ست می‌کند.

**Preview زنده**: طبق یافته‌ی راستی‌آزمایی بالا، هر ViewModel فقط دقیقاً
همان فیلدهایی را که تابع Template متناظرش می‌خواند دنبال می‌کند:
- `LocationAssetFormViewModel`: ۶ فیلد (`description`/`environmentType`/
  `environmentSize`/`environmentLighting`/`keyElements`/`basePrompt`) —
  بیش از ۵ آرگومان `combine` مستقیم، پس دو‌مرحله‌ای (۵+۱)، هم‌الگو با
  `characterSnapshot` زیرقدم ۴.
- `ObjectAssetFormViewModel`: دقیقاً ۵ فیلد (`description`/`size`/
  `materialAndColor`/`specialTrait`/`basePrompt`) — یک `combine` تک‌مرحله‌ای
  کافی بود (برخلاف Location).

**نام‌گذاری توابع AI (انحراف کوچک ثبت‌شده از دستور کار)**: دستور کار برای
هر دو ViewModel نام `generateImagePromptWithAi()` را پیشنهاد داده بود. چون
این نام دقیقاً با نام تابع دامنه‌ی وارداتی `generateImagePromptWithAi`
(`ImagePromptAiConnector.kt`، ADR-133) یکی است، نام متمایز
`generateLocationImagePromptWithAi()`/`generateObjectImagePromptWithAi()`
انتخاب شد — Kotlin هر دو حالت را به‌درستی بر مبنای Arity Resolve می‌کند (پس
هم‌نامی فنی مشکلی ایجاد نمی‌کرد)، اما نام متمایز خوانایی را بیشتر و ریسک
ابهام برای خواننده‌ی بعدی را صفر می‌کند؛ هم‌راستا با تصمیم مشابه (اما بدون
هم‌نامی) زیرقدم ۴. `generateImagePromptQuick()` بدون تغییر نام در هر دو
ViewModel استفاده شد (بدون هم‌نامی با هیچ Import).

### UI (`LocationAssetFormScreen.kt`/`ObjectAssetFormScreen.kt`)

هر دو Screen: یک Section «پرامپت عکس مرجع» جدید (هم‌الگو دقیق با
`CharacterImagePromptSection` زیرقدم ۴ — Preview زنده، دکمه‌ی دوگزینه‌ای
سریع/حرفه‌ای با `CircularProgressIndicator` حین AI، نمایش نتیجه‌ی
ذخیره‌شده + ترجمه‌ی فارسی، برچسب قدیمی‌شدن)، بین `RetranslateButton` و
لیست `validationIssues`. بدون بخش Outfit — هر دو نوع Asset یک پرامپت واحد
دارند. کلیدهای i18n مشترک (`assetForm.imagePrompt*`، از زیرقدم ۴) بازاستفاده
شدند بدون تکرار؛ فقط عنوان بخش (`locationForm.imagePromptSectionTitle`/
`objectForm.imagePromptSectionTitle`) تازه اضافه شد، چون آن یک کلید مخصوص
Character بود (`characterForm.imagePromptSectionTitle`).

## پیامدها

- `ImagePromptEngine.kt`/`ImagePromptValidation.kt`/`ImagePromptAiConnector.kt`/
  `AssetModels.kt`/`AssetDto.kt`/`DnaAssetMappers.kt`/`Renderer.kt`/
  `PromptAssembly.kt` فقط خوانده شدند، هرگز نوشته نشدند.
- `CharacterAssetFormViewModel.kt` فقط برای تغییر کوچک DRY (بالا) لمس شد —
  بدون تغییر رفتاری، تأییدشده با تست‌های بدون‌تغییر آن فایل.
- خروجی پرامپت (`imagePromptQuick`/`imagePromptAi`) همیشه انگلیسی می‌ماند.
- توابع اعتبارسنجی زیرقدم ۲ مستقیماً به UI وصل نشدند (همان انحراف ثبت‌شده‌ی
  زیرقدم ۴، برای یکدستی تکرار شد).
- **این آخرین زیرقدم بود — فیچر مستقل «پرامپت ساخت عکس مرجع» (ADR-131 تا
  ADR-135) اکنون کامل است**: هر سه نوع Asset (Character/Outfit، Location،
  Object) پرامپت عکس مرجع Template و AI را پشتیبانی می‌کنند.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`: موفق
  (شامل تأیید DRY بدون‌شکست `CharacterAssetFormViewModel.kt`).
- `LocationAssetFormViewModelTest.kt`/`ObjectAssetFormViewModelTest.kt` (۵
  تست تازه در هر کدام — تولید سریع، `save()` واقعاً `updatedAt` را ست
  می‌کند، مسیر AI بدون کلید هرگز HTTP نمی‌زند، مسیر AI موفق دوزبانه،
  `isImagePromptStale` هم حالت null هم حالت واقعی قدیمی/تازه): هر دو کلاس
  (۷ تست هرکدام، شامل ۲ تست از قبل موجود) Pass.
- `ui.assets.*`/`domain.asset.*`: بدون رگرسیون.
- `AssetFormFlowTest.kt`/`UiStringsTest.kt`: Pass.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۹۹۸ تست، ۹۹۷ Pass، ۱
  Fail — `OutputDeliveryFlowTest`، از پیش در فهرست کلاس‌های ناپایدار
  (Flaky) مستندشده‌ی این پروژه (بدون ارتباط با Asset). با اجرای مجدد و
  مجزا کامل Pass شد — Flakiness شناخته‌شده، نه رگرسیون. همچنین یک بار
  اجرای `ObjectAssetFormViewModelTest` در ترکیب با کل کلاس یک شکست گذرا
  (`saveCompleted` تایم‌اوت) نشان داد که با اجرای مجدد کامل Pass شد — همان
  الگوی «هزینه‌ی گرم‌شدن یک‌باره‌ی Room» مستندشده‌ی زیرقدم ۴، نه یک باگ
  منطقی.
- `./gradlew :app:assembleDebug`: موفق.
