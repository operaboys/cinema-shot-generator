# ADR-134: زیرقدم ۴ از ۵ — اتصال «پرامپت ساخت عکس مرجع» به CharacterAssetForm

## وضعیت

پذیرفته‌شده

## زمینه

ادامه‌ی ADR-131 (مدل داده)، ADR-132 (موتور Template) و ADR-133 (مسیر AI).
این زیرقدم فقط `CharacterAssetForm` را سیم‌کشی می‌کند — شخصیت پایه و هر
Outfit. Location/Object در زیرقدم ۵ جدا انجام می‌شود.

### راستی‌آزمایی مستقل

پیش از نوشتن کد، `CharacterAssetFormViewModel.kt`،
`CharacterAssetFormScreen.kt`، `ProjectDnaRepository.kt`،
`OutputDeliveryViewModel.kt` (بخش `analyzePromptQualityWithAi`) و
`RetranslateButton.kt` کامل خوانده شدند:

- تأیید شد `save()` تا این زیرقدم واقعاً `updatedAt` را ست نمی‌کرد —
  دقیقاً طبق پیش‌بریفینگ؛ رفع شد (پایین‌تر).
- تأیید شد `styleTokensOf` در `ImagePromptEngine.kt` واقعاً `private`
  است — طبق پیش‌بریفینگ، بدون تغییر آن فایل (قانون این قدم)، یک نسخه‌ی
  کوچک محلی در ViewModel بازسازی شد (`combineStyles` +
  `toStyleReference` که هر دو already-public هستند).
- تأیید شد `buildOutfitImagePrompt` فقط از `character.name` استفاده
  می‌کند (نه فیلد دیگری) — همین یافته باعث شد پیش‌نمایش زنده‌ی هر Outfit
  در Screen به یک `CharacterAsset` مینیمال (فقط name/tier) نیاز داشته
  باشد، نه یک StateFlow کامل جدا در ViewModel.
- تأیید شد `ProjectDnaEntity` یک ForeignKey واقعی به `ProjectEntity`
  دارد (کشف شده هنگام رفع تست‌ها — پایین‌تر)، هم‌الگو با یافته‌ی
  مستندشده‌ی `DnaViewModelTest.kt`.
- الگوی `analyzePromptQualityWithAi` (بررسی کلید → InProgress → نتیجه/
  خطا) و الگوی `RetranslateButton.kt` (فقط برای آشنایی با ظاهر، تک‌گزینه‌ای
  و غیرقابل‌استفاده‌ی مستقیم برای این فیچر دوگزینه‌ای) هر دو تأیید شدند.

هیچ مغایرت دیگری با پیش‌بریفینگ پیدا نشد.

## تصمیم

### تصمیمات معماری قطعی (از معمار، غیرقابل‌تفسیر مجدد)

- پیش‌نمایش زنده‌ی Template (بدون دکمه، خودکار via `combine`) برای هم
  شخصیت پایه و هم هر Outfit.
- دکمه‌ی «ساخت پرامپت عکس مرجع» با دو گزینه — «سریع» (فقط Template، در
  `imagePromptQuick`) و «حرفه‌ای» (AI، در `imagePromptAi` +
  `imagePromptFaPreview`) — یکی برای شخصیت پایه، یکی برای هر Outfit.
- هر دو مسیر روی موفقیت `imagePromptGeneratedAt` را ست می‌کنند.
- نشانگر قدیمی‌شدن: یک Warning غیرمسدودکننده وقتی
  `(asset.updatedAt ?: 0) > (imagePromptGeneratedAt ?: 0)` و
  `imagePromptGeneratedAt != null`.
- ماندگاری فقط با کلیک دکمه‌ی اصلی «ذخیره» — نتایج تولید تا آن لحظه فقط
  در StateFlow ViewModel می‌مانند.

### ViewModel (`CharacterAssetFormViewModel.kt`)

- `ProjectDnaRepository` تزریق‌پذیر (پیش‌فرض از `AppDatabase`، هم‌الگو با
  Repositoryهای دیگر این ViewModel)؛ در `init` یک‌بار `ProjectDna` پروژه
  بارگذاری می‌شود (`_projectDna`).
- `characterSnapshot` (private، `combine` ۵+۵+۴ سه‌مرحله‌ای، هم‌الگو با
  دقیقاً همین تکنیک در `ShotComposerViewModel.cameraValidationIssues`) +
  `characterBaseImagePromptPreview` (public) = پیش‌نمایش زنده‌ی شخصیت
  پایه.
- برای هر Outfit — چون خودِ `Outfit` (ADR-131) از قبل ۴ فیلد پرامپت را
  دارد، StateFlow جدا لازم نبود؛ `generateOutfitImagePromptQuick(index)`/
  `generateOutfitImagePromptWithAi(index): Job` مستقیماً `_outfits.value`
  را `copy` می‌کنند.
- `generateCharacterBaseImagePromptQuick()`/
  `generateCharacterBaseImagePromptWithAi(): Job` هم‌الگو با
  `retranslate()`/`analyzePromptQualityWithAi` (بررسی کلید API →
  InProgress → نتیجه یا خطا).
- StateFlow های خطا/InProgress برای Outfit به تشخیص خودم به‌صورت
  `Set<String>`/`Map<String, String>` بر مبنای `outfit.id` (نه Index، تا
  با افزودن/حذف Outfit دیگر نامعتبر نشوند).
- `isImagePromptStale(generatedAt: Long?): Boolean` — با `loadedUpdatedAt`
  (یک `var` معمولی، هم‌الگو با `ShotComposerViewModel.loadedShot`، ثبت‌شده
  در `applyLoadedAsset`) مقایسه می‌شود؛ چون Outfit فیلد `updatedAt`
  مستقل ندارد (تصمیم ADR-131)، همین یک مقدار سطح-Character برای هم
  شخصیت پایه و هم هر Outfit استفاده می‌شود — چون ویرایش هر Outfit در
  عمل ویرایش همان `CharacterAsset` والد است و با همان `save()` ذخیره
  می‌شود.
- `save()`: اکنون `updatedAt = System.currentTimeMillis()` ست می‌کند —
  رفع صریح یافته‌ی پیش‌بریفینگ، بدون آن تشخیص قدیمی‌شدن هرگز کار
  نمی‌کرد.

### UI (`CharacterAssetFormScreen.kt`)

- `CharacterImagePromptSection` جدید بین `RetranslateButton` و
  `OutfitsSection`: پیش‌نمایش، دو دکمه (با `CircularProgressIndicator`
  حین AI)، نمایش نتیجه‌ی ذخیره‌شده + ترجمه‌ی فارسی، برچسب قدیمی‌شدن.
- `OutfitRow` یک دکمه‌ی Expand/Collapse تازه («پرامپت این لباس») گرفت که
  `OutfitImagePromptSection` را باز می‌کند — الگوی فشرده‌تر به‌جای
  تکرار کامل بخش شخصیت پایه، چون `OutfitRow` از قبل شلوغ بود.
- برای پیش‌نمایش زنده‌ی هر Outfit، به‌جای Thread کردن یک Snapshot کامل
  از ViewModel، یک `CharacterAsset` مینیمال محلی (فقط `name`/`tier`)
  در Screen ساخته می‌شود — چون `buildOutfitImagePrompt` فقط
  `character.name` را می‌خواند (یافته‌ی راستی‌آزمایی بالا).
- تست‌تگ‌های تازه با پیشوند `CHARACTER_FORM_...`/`outfitImagePrompt...Tag`
  هم‌الگو با قرارداد موجود.
- کلیدهای i18n تازه در فضانام `assetForm.imagePrompt*` (مشترک، قابل
  استفاده‌ی زیرقدم ۵) و `characterForm.imagePromptSectionTitle`/
  `characterForm.outfitImagePromptToggleButton`.

## یافته‌ی دیباگ تست‌ها (مستقل از دستور کار، حین نوشتن تست)

`ProjectDnaEntity` یک ForeignKey واقعی به `ProjectEntity` دارد (همان
یافته‌ی مستندشده‌ی `DnaViewModelTest.kt`) — بدون یک ردیف Project واقعی،
`saveProjectDna` داخل `runCatching` بی‌صدا شکست می‌خورد و
`vm.projectDna` برای همیشه `null` می‌ماند. تست‌های این زیرقدم که به
`ProjectDna` نیاز داشتند اصلاح شدند تا ابتدا با
`ProjectRepository(...).createProject(...)` یک ردیف Project واقعی
بسازند. همچنین تست Outfit-Quick نیاز داشت `characterBaseImagePromptPreview`
را Collect کند تا `characterSnapshot` (StateFlow محاسبه‌شده با
`WhileSubscribed`) واقعاً نام تازه‌ی شخصیت را منعکس کند — همان یافته‌ی
از پیش مستندشده‌ی این فایل درباره‌ی `canSave`/`validationIssues`.

## پیامدها

- فقط `CharacterAssetForm` سیم‌کشی شد؛ `LocationAssetForm*`/
  `ObjectAssetForm*` دست‌نخورده ماندند — زیرقدم ۵ جدا.
- `ImagePromptEngine.kt`/`ImagePromptValidation.kt`/
  `ImagePromptAiConnector.kt`/`AssetModels.kt`/`AssetDto.kt`/
  `DnaAssetMappers.kt`/`Renderer.kt`/`PromptAssembly.kt` فقط خوانده
  شدند، هرگز نوشته نشدند.
- توابع اعتبارسنجی `validateCharacterImagePromptInputs`/
  `validateOutfitImagePromptInputs` (ADR-132) مستقیماً به UI وصل نشدند —
  در این زیرقدم دکمه‌های تولید همیشه فعال‌اند (به‌جز شرط کلید API برای
  مسیر AI)؛ این‌ها Warning-only هستند و هیچ مسیری را مسدود نمی‌کنند، پس
  عدم اتصال مستقیم یک محدودیت عملکردی ایجاد نکرد. این یک انحراف
  ثبت‌شده از دستور کار است — در صورت نیاز، اتصال آن‌ها به‌عنوان یک
  پالایش کوچک در زیرقدم ۵ یا بعدتر قابل‌افزودن است.
- خروجی پرامپت (`imagePromptQuick`/`imagePromptAi`) همیشه انگلیسی
  می‌ماند — هیچ منطق تولید متن تازه‌ای نوشته نشد، فقط UI/ViewModel.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`: موفق.
- `CharacterAssetFormViewModelTest.kt` (۶ تست تازه — تولید سریع شخصیت
  پایه، تولید سریع یک Outfit بدون دست‌زدن به بقیه، `save()` واقعاً
  `updatedAt` را ست می‌کند، مسیر AI بدون کلید هرگز HTTP واقعی نمی‌زند،
  مسیر AI با کلید و پاسخ دوزبانه‌ی موفق، `isImagePromptStale` هم حالت
  `null` و هم حالت واقعی قدیمی/تازه): همه‌ی ۱۹ تست این کلاس (۱۳ قبلی + ۶
  تازه) Pass.
- `ui.assets.*`/`domain.asset.*`: بدون رگرسیون.
- `AssetFormFlowTest.kt` (E2E سطح Compose، شامل ساخت/ویرایش شخصیت واقعی
  از طریق فرم): Pass — بدون رگرسیون از افزودن بخش تازه‌ی UI.
- `UiStringsTest.kt`: Pass — کلیدهای i18n تازه پوشش داده شدند.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۹۸۸ تست، ۹۸۷ Pass، ۱
  Fail — `OutputDeliveryFlowTest`، از پیش در فهرست کلاس‌های ناپایدار
  (Flaky) مستندشده‌ی این پروژه (بدون هیچ ارتباطی با Asset). با اجرای
  مجدد و مجزا کامل Pass شد — تأیید شد Flakiness شناخته‌شده است، نه
  رگرسیون.
- `./gradlew :app:assembleDebug`: موفق.

این فقط **زیرقدم ۴ از ۵** است. اتصال Location/Object به همین فیچر هنوز
آغاز نشده — زیرقدم ۵.
