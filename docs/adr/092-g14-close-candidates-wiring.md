# ADR-092: وصل شدن گروه «کاندید رفع نزدیک» G14 — چهار Rule یتیم کوچک

## زمینه

طبق جدول تصمیمات ADR-064 (بخش ب، ممیزی G14)، این چهار Rule تعریف‌شده و
تست‌شده بودند اما هیچ‌کدام از UI/ViewModel صدا زده نمی‌شدند: تجمیع سطح‌بالای
Level 1 (`validateDataCompleteness`، تصمیم ۲)، Audio Timeline
(`validateActionSoundTimeline`، تصمیم ۹)، هشدار نام مشابه Asset
(`checkSimilarAssetName`، تصمیم ۷)، و سه Rule باقی‌مانده‌ی AI Story
Breakdown (`validateFreeformStoryLength`/`validateHighShotCount`/
`validateChunksComplete`، تصمیم ۱۲).

## یافته‌ی مستقل — مورد ۱ (`validateDataCompleteness`) جدا شد؛ وصل نشد

طبق دستور صریح این قدم («اگر متوجه شدی یکی نیازمند تصمیم بزرگ‌تری است، جدا
کن»): با خواندن کامل `domain/shot/ShotValidation.kt` (نه صرفاً grep سطحی)
مشخص شد ADR-064 یک واقعیت قدیمی‌تر را از قلم انداخته بود. کامنت صریح خودِ
`validateShotHasSubject` (که از قبل در `ValidationAggregator` Level 1 وایر
است) می‌گوید:

> «این Rule قبلاً با نمونه‌ی مشابه در واحد ۰۷ (`validateDataCompleteness`)
> در تضاد بود ... این تضاد در Migration ۴ (`docs/adr/010-cross-unit-
> migrations.md`) با هم‌ترازکردن واحد ۰۷ با همین‌جا رفع شد — منطق این تابع
> بدون تغییر ماند، فقط واحد ۰۷ به‌روزرسانی شد.»

یعنی از همان Migration ۴ به بعد، `validateDataCompleteness` (تجمیع
`ValidationEngine.kt`) دقیقاً همان دو بررسی `validateShotDescription` +
`validateShotHasSubject` (طول توضیح <۱۰، حداقل یک Subject) را با همان
Severity/field/message عیناً تکرار می‌کند — با مقایسه‌ی مستقیم هر سه تابع
تأیید شد: **byte-identical**. وصل کردن آن طبق پیشنهاد تحت‌اللفظی ADR-064
(`add(l1, validateDataCompleteness(...))`) باعث می‌شد هر نقض واقعی دقیقاً
دوبار در فهرست Issue های صفحه‌ی Validation ظاهر شود (شمارش دوبرابر، کارت‌های
تکراری) — یک رگرسیون واقعی، نه رفع یک Gap.

**تصمیم:** وصل نشد. `validateDataCompleteness` (ValidationEngine.kt) اکنون
هم‌ردیف `validateAssetIdUniqueness` (تصمیم ۷ ADR-064) — کد زائد ساختاری
(نه صرفاً یتیم)، کاندید حذف احتمالی در یک قدم پاکسازی آینده، نه وصل‌شدن.

## پیاده‌سازی — سه مورد باقی‌مانده

### Audio (`validateActionSoundTimeline`، تصمیم ۹)

یک خط در `ValidationAggregator.kt` Level 1، هم‌الگو دقیق با
`validateShotBeatTimeline` (که همان `validateBeatSheetTimeline` زیرین را
برای Beat Sheet صدا می‌زند):
```kotlin
addAll(l1, validateActionSoundTimeline(shot.soundProfile.actionSounds, shot.durationSeconds))
```

### Asset (`checkSimilarAssetName`، تصمیم ۷)

هر سه ViewModel فرم Asset (`Character`/`Location`/`Object`) اکنون یک Flow
تازه‌ی `existingNames` دارند (`repository.loadAllXxxAssets(projectId)`،
خودِ Asset در حال ویرایش با `existingAssetId` از فهرست کنار گذاشته می‌شود
تا هشدار کاذب «مشابه خودش» نباشد) که در `validationIssues` موجود هر فرم
(از قبل زنده روی هر تغییر فیلد محاسبه می‌شود) ترکیب شد. هیچ تغییر UI جدا
لازم نبود — هر سه Screen از قبل `validationIssues.forEach {
AssetFormValidationIssueRow(it) }` را عمومی رندر می‌کنند.

### AI Story Breakdown Rule 1/3/6 (تصمیم ۱۲)

هم‌الگو دقیق با Rule 2 موجود (`targetShotCountError`) در
`AiStoryBreakdownViewModel.kt`:
- **Rule 1** (`validateFreeformStoryLength`، Blocking): `freeformStoryError`
  تازه، ست‌شده در `setFreeformStory`/بارگذاری Session ذخیره‌شده؛
  `generatePrompt()` اکنون هر دو Guard (`targetShotCountError` **و**
  `freeformStoryError`) را چک می‌کند.
- **Rule 3** (`validateHighShotCount`، Warning): `highShotCountWarning`
  تازه، روی همان فیلد/همان محل‌های Rule 2 (چون هر دو `targetShotCount`
  می‌خوانند)، اما دکمه‌ی «تولید Prompt» را مسدود نمی‌کند.
- **Rule 6** (`validateChunksComplete`، Warning): `chunksCompleteWarning`
  تازه، در `addChunk()` (afterAddChunk) **و** `processResponse()`
  (afterCombine) بازمحاسبه می‌شود — طبق یافته‌ی مستند ADR-064، بدون این،
  کاربری که تکه‌ی آخر را با `[CONTINUE]` رها کند و مستقیم «ادامه» بزند فقط
  یک خطای خام Parse JSON می‌بیند؛ اکنون این هشدار واضح پیش از آن (بعد از
  افزودن همان تکه) ظاهر می‌شود.

`AiStoryBreakdownScreen.kt`: سه کارت تازه (هم‌الگو دقیق بصری با کارت
`targetShotCountError` موجود — `Icon`+`Text` رنگی داخل `Card`، testTag
اختصاصی)، دو‌تای Warning با `CinemaTheme.extendedColors.warning`/
`Icons.Filled.Warning` (نه رنگ Error).

## تست

- `ValidationAggregatorTest.kt`: ۲ تست تازه (Timestamp خارج/داخل مدت شات).
- `AssetFormFlowTest.kt`: ۱ تست E2E نماینده (فرم Character؛ منطق سه فرم
  عیناً یکسان است، فقط منبع Repository فرق دارد — تکرار سه‌باره متناسب
  نبود).
- `AiStoryBreakdownViewModelTest.kt`: ۸ تست تازه (هم‌الگو دقیق با تست‌های
  موجود Rule 2 — سطح ViewModel، بدون Compose). یافته‌ی دیباگ: تست موفقیت
  کامل `generatePrompt()` (مسیر غیر-Blocking) بدون `ioScopeOverride` در
  این فایل قابل‌اطمینان نیست (کامنت بالای فایل هم همین را از قبل مستند
  کرده بود) — به‌جای تغییر `setUp()` مشترک، تست Rule 3 فقط حالت‌های
  synchronous (`highShotCountWarning` ست می‌شود، `targetShotCountError`
  خالی می‌ماند) را می‌سنجد، بدون فراخوان واقعی `generatePrompt()`.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (پوشه‌های validation/audio/asset/storybreakdown) | ۱۹۹ تست، ۰ شکست |
| `gradle :app:testDebugUnitTest` (کل Suite) | موفق |
| `gradle :app:assembleDebug` | موفق |
