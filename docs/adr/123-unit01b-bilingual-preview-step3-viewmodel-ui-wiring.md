# ADR-123: سیستم Preview دوزبانه‌ی پرامپت — قدم ۳ از ۳ زیرقدم (پایانی): اتصال واقعی previewLanguageEnabled به processAiResponse + فیلد فارسی قابل‌ویرایش در فرم‌های Character/Location/Object/Shot Composer

## زمینه

این ADR سومین و **آخرین** زیرقدم برنامه‌ی «سیستم Preview دوزبانه‌ی
پرامپت» است (ADR-121 → ADR-122 → این ADR):

- **ADR-121 (قدم ۱)**: Migration واقعی اول این پروژه، `previewLanguageEnabled`
  روی `StoryBreakdownSession`، Toggle در Story Wizard، و
  `STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION_BILINGUAL` در `PromptBuilder.kt`.
- **ADR-122 (قدم ۲)**: `descriptionFaPreview`/`shotDescriptionFaPreview`
  روی مدل دامنه (`CharacterAsset`/`LocationAsset`/`ObjectAsset`/`Shot`)
  و `StoryToDomainMapper.kt` که واقعاً پاسخ دوزبانه‌ی AI را Parse
  می‌کند — اما `processAiResponse` هنوز همیشه با مقدار پیش‌فرض
  `previewLanguageEnabled=false` صدا زده می‌شد (محدودیت آگاهانه‌ی
  مستندشده‌ی همان ADR).
- **این قدم (قدم ۳، پایانی)**: (۱) دو فراخوانی واقعی `processAiResponse`
  در `AiStoryBreakdownViewModel.kt` را به `previewLanguageEnabled`
  واقعی کاربر وصل می‌کند؛ (۲) فیلد فارسی قابل‌ویرایش را در هر چهار
  فرم (Character/Location/Object/Shot Composer) اضافه می‌کند.

## بررسی مستقل — نتیجه: بدون هیچ انحراف واقعی از پیش‌بریفینگ

پیش از نوشتن کد، طبق الزام صریح دستور کار، این چهار ViewModel و چهار
Screen به‌طور کامل خوانده شدند: `CharacterAssetFormViewModel.kt`/
`Screen.kt`، `LocationAssetFormViewModel.kt`/`Screen.kt`،
`ObjectAssetFormViewModel.kt`/`Screen.kt`، `ShotComposerViewModel.kt`/
`ShotComposerScreen.kt`. **تأیید شد**: هر سه ViewModel فرم Asset الگوی
`basePrompt`/`description` (`MutableStateFlow("")` + `applyLoadedAsset`
+ setter + `ifBlank { null }` در `save()`) را دقیقاً پیروی می‌کنند؛
`ShotComposerViewModel.setShotDescription` واقعاً بلافاصله `save()`
صدا می‌زند (auto-save فوری، طبق پیش‌بریفینگ). **بدون هیچ انحراف واقعی.**

**یافته‌ی مفید مستقل (نه انحراف، یک بهینه‌سازی)**: `MainFieldsSection`
در `ShotComposerScreen.kt` یک Composable مشترک است که هم توسط
`ComposerLayoutVariant.TABS` و هم `ComposerLayoutVariant.ACCORDION`
صدا زده می‌شود؛ طبق الگوی مستندشده‌ی خودِ فایل (کامنت بالای
`cinematicModeOverride`)، StateFlow های تازه باید مستقیماً از داخل
همین تابع Collect شوند (نه پارامتر تازه در امضا) تا هر دو فراخوان
موجود بدون تغییر بمانند — همین الگو برای `shotDescriptionFaPreview`
هم به کار رفت.

## پیاده‌سازی

**۱. `AiStoryBreakdownViewModel.kt`**: هر دو فراخوانی
`processAiResponse` (در `processResponse()` و `sendPromptAutomatically()`)
اکنون پارامتر سوم `_previewLanguageEnabled.value` می‌دهند — تا این
قدم همیشه پیش‌فرض `false` بود، حتی وقتی کاربر Toggle (قدم ۱) را روشن
کرده بود.

**۲. `CharacterAssetFormViewModel.kt`/`LocationAssetFormViewModel.kt`/
`ObjectAssetFormViewModel.kt`**: هرکدام `_descriptionFaPreview`
(`MutableStateFlow("")`) + `descriptionFaPreview: StateFlow<String>` +
`setDescriptionFaPreview(value)` گرفتند، هم‌الگو دقیق با `basePrompt`
موجود؛ `applyLoadedAsset` آن را از `asset.descriptionFaPreview.orEmpty()`
پر می‌کند؛ `save()` (یا `buildPreviewAsset()` در Object) آن را با
`ifBlank { null }` در `descriptionFaPreview` می‌ریزد.

**۳. `ShotComposerViewModel.kt`**: `_shotDescriptionFaPreview` هم‌الگو
با `_shotDescription`؛ `setShotDescriptionFaPreview` هم‌الگو دقیق —
مقدار را فوری تغییر می‌دهد و بلافاصله `save()` صدا می‌زند (auto-save)؛
`init{}` آن را از `loaded.shotDescriptionFaPreview ?: ""` بارگذاری
می‌کند؛ `buildShot()` آن را با `ifBlank { null }` در
`shotDescriptionFaPreview` می‌ریزد.

**۴. `CharacterAssetFormScreen.kt`/`LocationAssetFormScreen.kt`/
`ObjectAssetFormScreen.kt`**: یک `OutlinedTextField` تازه، هم‌الگو
دقیق با `basePrompt`/`description` موجود، بلافاصله بعد از
`basePrompt`. برچسب مشترک `assetForm.descriptionFaPreviewLabel`
(هر سه فرم از همان کلید مشترک `assetForm.*` استفاده می‌کنند، طبق
همان الگوی موجود `assetForm.basePromptLabel`/`assetForm.descriptionLabel`).

**۵. `ShotComposerScreen.kt`**: `OutlinedTextField` تازه داخل
`MainFieldsSection` (بلافاصله بعد از فیلد `shotDescription`)، با
StateFlow مستقیماً Collect‌شده از `viewModel.shotDescriptionFaPreview`
(نه پارامتر تازه) — طبق یافته‌ی مستقل بالا. برچسب اختصاصی
`shotComposer.shotDescriptionFaPreviewLabel`.

**۶. `UiStrings.kt`**: دو کلید تازه، هر دو در هر دو نقشه (فارسی/
انگلیسی) هم‌زمان اضافه شدند — متن هر دو صریحاً می‌گوید این فیلد فقط
Preview است، هرگز وارد پرامپت نهایی نمی‌شود.

**۷. testTag های تازه**: `CHARACTER_FORM_DESCRIPTION_FA_PREVIEW_FIELD_TAG`،
`LOCATION_FORM_DESCRIPTION_FA_PREVIEW_FIELD_TAG`،
`OBJECT_FORM_DESCRIPTION_FA_PREVIEW_FIELD_TAG`،
`SHOT_COMPOSER_DESCRIPTION_FA_PREVIEW_FIELD_TAG`.

## یافته‌ی واقعی دیباگ تست‌ها — دو مشکل واقعی، نه فرضی

**۱. `canSave`/`validationIssues` روی `viewModelScope` واقعی**: هر سه
ViewModel فرم Asset، `canSave` را از `combine(...).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)`
می‌سازند (نه `ioScopeOverride`). بدون یک Collector واقعی، `.value`
همیشه همان مقدار اولیه (`false`) می‌ماند — دقیقاً همان محدودیت
مستندشده‌ی از‌قبل در کامنت‌های خودِ `CharacterAssetFormViewModelTest.kt`
برای `validationIssues`. تست‌های تازه‌ی این قدم اولین‌بار واقعاً به
`save()` (که `canSave.value` را چک می‌کند) نیاز داشتند؛ رفع شد با یک
Collector مصنوعی (`vm.canSave.collect {}` روی یک `CoroutineScope(Dispatchers.Unconfined)`
جدا) + `shadowOf(Looper.getMainLooper()).idle()` (چون Robolectric با
`LooperMode.PAUSED` پیش‌فرض نیاز به idle صریح برای اجرای کارهای
Post‌شده روی `Dispatchers.Main.immediate` دارد) — دقیقاً همان کاری که
UI واقعی (`collectAsStateWithLifecycle`) در تولید انجام می‌دهد.

**۲. `ShotEntity` یک ForeignKey واقعی به `SceneEntity` دارد**
(`data/entity/ShotEntity.kt`): بدون ساخت واقعی یک `Project` و `Scene`
اول، `ShotRepository.saveShot` بی‌صدا با `SQLiteConstraintException`
شکست می‌خورد (چون `runCatching` آن را می‌بلعد) — دقیقاً همان علت
مستندشده‌ی مشابه بارها در این جلسه (`story_breakdown_session`،
`AppDatabaseDaoTest.kt`). `ShotComposerViewModelTest.kt` اکنون در
`setUp()` یک Project و Scene واقعی می‌سازد.

## تست

**`AiStoryBreakdownViewModelTest.kt` (۲ تست تازه)**: یکی با
`setPreviewLanguageEnabled(true)` + JSON دوزبانه‌ی واقعی، اثبات می‌کند
`processResponse()` واقعاً از Schema دوزبانه استفاده می‌کند
(`breakdownResult.characters[0].descriptionFaPreview`/
`shots[0].shotDescriptionFaPreview` پر می‌شوند، `basePrompt`/
`shotDescription` همچنان انگلیسی می‌مانند)؛ دیگری اثبات معکوس می‌کند:
همان JSON دوزبانه بدون `setPreviewLanguageEnabled(true)` (یعنی هنوز
پیش‌فرض `false`) واقعاً شکست Decode می‌خورد — یعنی این پرچم واقعاً
تعیین‌کننده است، نه صرفاً یک پارامتر بلااثر.

**`CharacterAssetFormViewModelTest.kt` (۲ تست تازه)**، **جدید:
`LocationAssetFormViewModelTest.kt`/`ObjectAssetFormViewModelTest.kt`
(هرکدام ۲ تست، اولین تست ViewModel-level این دو کلاس)**: `save()`
با مقدار Preview فارسی پرشده round-trip دقیق را از Room اثبات
می‌کند؛ حالت `null` (فیلد هرگز پر نشده) هم صریح تست شد.

**جدید: `ShotComposerViewModelTest.kt` (۳ تست، اولین تست
ViewModel-level این کلاس)**: `setShotDescriptionFaPreview` auto-save
واقعی را اثبات می‌کند؛ یک تست اثبات می‌کند شاتی که هرگز Preview
فارسی نگرفته، بعد از auto-save همچنان `null` می‌ماند؛ یک تست بارگذاری
مجدد یک شات موجود را از Room اثبات می‌کند.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (فایل‌های تازه/تغییریافته، مجزا) | موفق — همه‌ی فایل‌های بالا |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۹۳۸ تست، ۱ شکست نامرتبط (`OutputDeliveryFlowTest`) — در اجرای مجزا موفق؛ همان کلاس Flake محیطی مستندشده از ADR-044 تا کنون |
| `gradle :app:assembleDebug` | موفق |

## End-to-End Verification — زنجیره‌ی کامل ۳ قدم واقعاً به‌هم وصل‌اند

سوال کلیدی این قدم پایانی: آیا سه قدم واقعاً یک زنجیره‌ی کامل و
کارکردی می‌سازند، یا فقط سه قطعه‌ی جدا مستقلاً درست کار می‌کنند؟

زنجیره‌ی کامل (اثبات‌شده با کد واقعی، نه فرض):

1. کاربر در Story Wizard (AI Story Breakdown) Toggle «همراه با Preview
   فارسی» را روشن می‌کند → `AiStoryBreakdownViewModel._previewLanguageEnabled.value = true`
   (قدم ۱/ADR-121).
2. کاربر «تولید پرامپت» می‌زند → `buildStoryBreakdownPrompt` با
   `request.previewLanguageEnabled=true` واقعاً
   `STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION_BILINGUAL` را در پرامپت
   ارسالی به AI بیرونی جای می‌دهد (قدم ۱/ADR-121، دست‌نخورده در این
   قدم — تأییدشده مستقیم با خواندن دوباره‌ی کد).
3. کاربر پاسخ AI (JSON با `descriptionEn`/`descriptionFa`) را
   می‌چسباند/خودکار ارسال می‌کند → **این قدم**:
   `processResponse()`/`sendPromptAutomatically()` اکنون
   `_previewLanguageEnabled.value` واقعی (نه پیش‌فرض `false`) را به
   `processAiResponse` می‌دهند → `BilingualAiResponse` (قدم ۲/ADR-122)
   Parse می‌شود → `CharacterAsset.descriptionFaPreview`/
   `Shot.shotDescriptionFaPreview` واقعاً پر می‌شوند.
4. کاربر «تأیید و ادامه» می‌زند → `confirmAndSave()`
   (`AiStoryBreakdownViewModel.kt`، بدون تغییر در هیچ قدم این برنامه)
   این Asset ها/Shot ها را دقیقاً همان‌طور که هستند (شامل
   `descriptionFaPreview` پرشده) در `AssetRepository`/`ShotRepository`
   ذخیره می‌کند.
5. کاربر بعداً همان Character/Location/Object/Shot را در فرم‌های
   Composer باز می‌کند → **این قدم**: `applyLoadedAsset`/`init{}`
   مقدار فارسی را از دیتابیس بارگذاری می‌کند و در فیلد تازه‌ی
   `descriptionFaPreview`/`shotDescriptionFaPreview` (قابل‌ویرایش)
   نمایش می‌دهد.

تست‌های end-to-end تازه‌ی `AiStoryBreakdownViewModelTest.kt` مرحله‌ی
۳ (پارامتر واقعی previewLanguageEnabled) را مستقیماً روی JSON دوزبانه‌ی
واقعی اثبات می‌کنند؛ تست‌های Round-Trip چهار ViewModel فرم مرحله‌ی ۵
(ذخیره/بارگذاری) را اثبات می‌کنند. مراحل ۱-۲ و ۴ در قدم‌های قبلی
(ADR-121/۱۲۲) با تست‌های مستقل خودشان اثبات شده‌اند و در این قدم
دست‌نخورده ماندند (تأییدشده با `git status` — `PromptBuilder.kt`،
`StoryToDomainMapper.kt` core logic هیچ‌کدام در این قدم تغییر نکردند).
در همه‌ی این مراحل، **پرامپت نهایی که به مدل تصویر/ویدیو می‌رود
(`PromptAssembly.kt`/`Renderer.kt`)** فقط و همیشه از `basePrompt`/
`description`/`shotDescription` انگلیسی می‌خواند — این دو فایل در
هیچ‌کدام از سه قدم این برنامه تغییر نکردند (تأییدشده مستقیم با
`git status` پیش از هر Commit این برنامه).

## خارج از Scope این قدم / کل برنامه (عمداً)

- `PromptAssembly.kt`/`Renderer.kt` در هر سه قدم دست‌نخورده ماندند.
- هیچ ترجمه‌ی خودکار انگلیسی↔فارسی در هیچ‌جای این برنامه پیاده نشد —
  فیلد فارسی همیشه یا از پاسخ AI بیرونی یا از تایپ دستی کاربر می‌آید.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
