# ADR-064: G14 — تصمیم آگاهانه درباره‌ی Rule های یتیم (بخش الف رفع، بخش ب مستندسازی)

## زمینه

`docs/audit/post-unit16-full-audit.md` بخش G14 بیش از ۳۰ Rule اعتبارسنجی
دامنه را «واقعاً یتیم» فهرست کرده بود — تعریف‌شده، تست‌شده، کامپایل‌شونده،
اما بدون هیچ فراخوان مستقیم/غیرمستقیم از `ui/`. طبق دستور خودِ ممیزی («یا
وصل شوند، یا رسماً به‌عنوان تصمیم مستند شوند، نه بی‌صدا فراموش بمانند»)،
این قدم دو کار متفاوت انجام می‌دهد:

- **بخش الف** — رفع واقعی تنها یافته‌ی 🟠 این بخش (`validateTargetShotCountRange`).
- **بخش ب** — تصمیم آگاهانه (نه وصل‌کردن کد) برای باقی گروه‌ها.

## بازبینی مستقل پیش‌بررسی‌های دستور کار (طبق دستور صریح)

هر Rule یتیم فهرست‌شده در ممیزی با `grep -rln` مستقل روی `app/src/main/`
بازبینی شد. **دو مغایرت واقعی پیدا شد:**

1. **گروه Prompt Finalization باقی‌مانده کاملاً غلط بود.** دستور کار
   می‌گفت «`checkTokenLimit` در قدم قبلی G17 وصل شد؛ این دو
   (`validateConflictsResolved`/`validateCompressionRatio`) یکی
   مانده‌اند» — اما `grep` نشان داد هر دو **از قبل** در
   `OutputDeliveryViewModel.kt:172-173` صدا زده می‌شوند (بخشی از همان رفع
   G17/ADR-060، نه فقط `checkTokenLimit`). این گروه کاملاً بسته است، هیچ
   Rule یتیمی در آن باقی نمانده — جزئیات در تصمیم ۵ پایین.
2. **`validateAssetDeletion`** در فهرست اصلی ممیزی به‌عنوان یتیم آمده
   بود، اما در قدم G22 (`docs/adr/062-...`) وصل شد — دستور کار این‌بار
   صریحاً آن را در فهرست تکرار نکرده بود (درست)، اما تأیید مستقل لازم
   بود که واقعاً دیگر یتیم نیست — تأیید شد.

هیچ مغایرت دیگری در ادعاهای «یتیم بودن» پیدا نشد (هر مورد دیگر با
`grep -rln "<fn>" app/src/main/java/.../ui/` صفر نتیجه داد).

---

## بخش الف — رفع: `validateTargetShotCountRange`

**قبل:** `AiStoryBreakdownViewModel.setTargetShotCount` با
`count.coerceIn(1, 150)` مقدار خارج از محدوده را بی‌صدا کوتاه می‌کرد؛
Rule 2 واقعی (`validateTargetShotCountRange`، `PromptBuilder.kt:92`)
هرگز صدا زده نمی‌شد.

**بعد:**
- `setTargetShotCount` مقدار خام کاربر را نگه می‌دارد (بدون `coerceIn`)
  و نتیجه‌ی واقعی `validateTargetShotCountRange` را در
  `targetShotCountError: StateFlow<String?>` تازه قرار می‌دهد.
- `init{}` (بارگذاری Session ذخیره‌شده) هم همین Rule را روی مقدار
  بازیابی‌شده اجرا می‌کند — برای یکدستی، اگر یک مقدار نامعتبر قدیمی از
  قبل ذخیره شده باشد.
- `generatePrompt()` یک محافظ synchronous اضافه گرفت
  (`if (_targetShotCountError.value != null) return`) — دفاع دوم،
  مستقل از UI (در صورت فراخوانی مستقیم، مثل تست).
- UI (`AiStoryBreakdownScreen.kt`): یک کارت خطای واقعی (هم‌الگو با
  `processingError` فاز ۲) زیر `IntStepperField` نمایش داده می‌شود؛ دکمه‌ی
  «تولید Prompt» با `enabled = targetShotCountError == null` غیرفعال
  می‌شود — دقیقاً طبق دستور کار («Blocking برای دکمه‌ی ادامه، نه اصلاح
  خودکار مقدار»).

**تصمیم فرعی — کدام دکمه Block می‌شود:** فاز ۱ دو دکمه دارد («تولید
Prompt» سپس «ادامه به فاز ۲»)؛ دومی فقط بعد از تولید موفق Prompت
نمایش داده می‌شود. چون Blocking باید *قبل* از تولید متن جلوگیری کند
(نه بعدش)، دکمه‌ی «تولید Prompt» انتخاب شد — دومی به‌طور طبیعی هرگز
ظاهر نمی‌شود اگر اولی غیرفعال بماند.

**تست‌ها** (`AiStoryBreakdownViewModelTest.kt`، تازه، مستقیم بدون
Compose):
- ورودی `200`/`0` → مقدار خام نگه داشته می‌شود + `targetShotCountError`
  واقعی ظاهر می‌شود.
- ورودی معتبر بعد از نامعتبر → خطا پاک می‌شود.
- `generatePrompt()` با خطای فعال → `generatedPrompt.value` بدون تغییر
  می‌ماند (Blocking تأیید می‌شود).
- مسیر «ورودی معتبر → تولید واقعی Prompt» از قبل در
  `AiStoryBreakdownFlowTest.kt` (happy path) پوشش کامل دارد — تکرار
  نشد.

---

## بخش ب — تصمیمات آگاهانه برای باقی گروه‌ها

### تصمیم ۱ — دوربین/حرکت: تصمیم قبلی هنوز معتبر است (ADR-055)

`validateSpeedIntensity`/`validateMotionBlur`/
`validateCameraMovementDuration` از قبل در `docs/adr/055-...md` («تصمیم
۵») آگاهانه رد شدند: `SubjectMotion` هیچ‌جای `Shot`/`CameraSettings`
وایر نشده، و `movementDurationSeconds` هیچ فیلد UI/دامنه‌ای ندارد. با
grep تأیید شد این وضعیت هنوز صادق است — هیچ فیلد جدیدی از آن زمان
اضافه نشده. **تصمیم:** بدون تغییر؛ اگر «حرکت موضوع» یا «مدت حرکت
دوربین جدا از مدت کل شات» یک فاز آینده‌ی UI شود، همین‌جا وصل شوند.

### تصمیم ۲ — تجمیع سطح‌بالا: `validateDataCompleteness` — یافته‌ی تازه، کاندید رفع نزدیک

برخلاف بقیه‌ی این بخش، این یکی **در هیچ تصمیم قبلی پوشش داده نشده** و با
بقیه‌ی Rule های Level ۱ موجود در `ValidationAggregator.kt`
(`validateShotBeatTimeline`، `validateNegativePromptOverride`،
`validateSceneHasShotsBeforeFinalize`) هم جایگزین نشده — یک شات با
توضیح خیلی کوتاه و بدون هیچ Character/Object/Location متصل، امروز در
صفحه‌ی Validation هیچ خطایی نمی‌گیرد. **تصمیم:** کاندید واقعی رفع در یک
قدم کوچک آینده — افزودن یک خط به بخش Level ۱ همان Aggregator
(`add(l1, validateDataCompleteness(shot.description, shot.characterIds, shot.objectIds, shot.locationIds))`)،
هزینه‌ی پایین، بدون تغییر معماری.

### تصمیم ۳ — State Machine/Lock: تکرار منطق واقعی است، نه Bypass — نظر: کاندید یکسان‌سازی، نه فوری

`domain/scene/SceneLifecycle.kt:14`/`domain/project/ProjectLifecycle.kt:17`
مستقیماً از `canTransition` (Boolean خام) استفاده می‌کنند، نه از
`validateStateTransition` (`StateMachine.kt:30`، همان لایه‌ی
`ValidationIssue`-برگردان که دقیقاً برای همین منظور ساخته شده). با
خواندن کامل `SceneLifecycle.kt` تأیید شد: **این یک Bypass ساختگی
نیست** — `lockScene`/`archiveProject` وقتی `canTransition` false است،
یک `Result.failure(IllegalStateException(پیام واقعی))` برمی‌گردانند،
و ViewModel های مربوطه همان پیام را از طریق `lastActionMessage` به
کاربر واقعی نشان می‌دهند. یعنی امروز کاربر پیام Blocking واقعی و درست
می‌بیند — فقط از یک مسیر کد موازی، نه از لایه‌ی `ValidationIssue`.

**نظر من:** این یک بدهی فنی واقعی (تکرار منطق، دو پیام کمی متفاوت برای
دقیقاً یک Rule) است، اما **نه یک باگ رفتاری فوری** — رفتار کاربر امروز
درست است. یکسان‌سازی (تغییر `SceneLifecycle`/`ProjectLifecycle` به
فراخوانی `validateStateTransition` و تبدیل `ValidationIssue` آن به
`Result.failure`) یک Refactor کوچک و کم‌ریسک است اما ۲ فایل دامنه + دو
فایل تست موجودشان را لمس می‌کند — به‌نظر من ارزش یک قدم کوچک اختصاصی
آینده را دارد (نه فوری، نه در همین قدم چون خارج از Scope مستندسازی-محور
بخش ب است). **این مورد را صریحاً برای تصمیم مشترک با معمار پروژه علامت
می‌زنم** (طبق درخواست صریح دستور کار).

### تصمیم ۴ — Output Delivery/i18n: بخشی یتیم کم‌ارزش، بخشی زائد ساختاری

- `validateProfileAvailability`: `targetPlatform` در UI واقعی همیشه از
  یک فهرست ثابت (`ALL_MODEL_PROFILES`) انتخاب می‌شود، هرگز رشته‌ی آزاد
  — این Rule فقط برای یک ورودی خارجی/پویا معنا دارد که امروز وجود
  ندارد. **تصمیم:** Post-MVP، فقط اگر انتخاب پلتفرم سفارشی/پویا اضافه
  شود.
- `validateTranslationKeyFound`/`validateLanguageSupported`: تابع
  `t()` خودش با Fallback طراحی شده (زبان → EN → خودِ کلید) — یعنی
  کلید گم‌شده همین الان با نمایش خودِ کلید در UI قابل‌کشف است، بدون نیاز
  به این Rule. `validateLanguageSupported` هم برای اعتبارسنجی یک رشته‌ی
  زبان خارجی (مثل Locale سیستم) است — با grep تأیید شد اپ هرگز
  `Locale.getDefault()` نمی‌خواند؛ تنها مسیر انتخاب زبان یک Toggle
  Type-Safe (`Language` enum) در Settings است. **تصمیم:** زائد در طراحی
  فعلی؛ فقط اگر تشخیص خودکار Locale سیستم اضافه شود، مرتبط می‌شوند.
- `validateBilingualCompleteness`: با بررسی عمیق‌تر، **کل ماژول
  `OutputComposer.kt` (`composeOutput`/`OutputPackage`/
  `BilingualPrompts`)** — نه فقط این یک Rule — هیچ فراخوان‌کننده‌ای در
  `ui/` ندارد؛ صفحه‌ی واقعی Output Delivery خروجی را از مسیر کاملاً
  دیگری صادر می‌کند (`ExportImportRepository`، G15). این یک طراحی واحد
  ۱۴ قدیمی است که هرگز توسط پیاده‌سازی واقعی‌تر جایگزین شد. **تصمیم:**
  علامت‌گذاری برای حذف/ادغام احتمالی آینده (نه در این قدم) — منسوخ، نه
  در انتظار وصل‌شدن.

### تصمیم ۵ — Prompt Finalization: **این گروه از قبل کاملاً بسته است** (تصحیح پیش‌بررسی)

طبق بازبینی مستقل بالا: `validateConflictsResolved`/
`validateCompressionRatio` هر دو در `OutputDeliveryViewModel.kt:172-173`
صدا زده می‌شوند؛ `checkTokenLimit` هم از طریق `finalizePrompt` →
`PromptFinalizationPipeline.kt:61` اجرا و نتیجه‌اش
(`tokenCheck`) واقعاً در `OutputDeliveryScreen.kt` (برچسب هزینه‌ی
توکن، رنگ خطا اگر خارج از سقف) نمایش داده می‌شود. **تصمیم:** بدون
اقدام — هیچ Rule یتیمی در این گروه باقی نمانده.

### تصمیم ۶ — Storage/Import: یافته‌ی محصولی، نه فقط Rule یتیم

با grep روی کل `app/src/main/` (نه فقط `ui/`) تأیید شد `importProject`
(`ExportImportRepository.kt`) **هیچ فراخوان‌کننده‌ای در کل پروژه ندارد**
— نه در UI، نه در جای دیگر. یعنی «Restore از بکاپ داخلی اپ»
(`BackupManager.restoreFromBackup`، واقعی و وایرشده) و «Import پروژه از
فایل JSON خارجی» (`importProject`، همان فرمتی که `exportProject`/G15
تولید می‌کند) **دو قابلیت کاملاً متفاوت‌اند** — فقط اولی UI دارد. کاربر
می‌تواند پروژه را Export کند اما هرگز نمی‌تواند همان فایل را (روی یک
نصب دیگر، یا بعد از حذف تصادفی) Import کند.

**تصمیم:** موافق ارزیابی خودِ دستور کار — این یک یافته‌ی محصولی
Post-MVP واقعی است (نه یک Rule یتیم کوچک)، چون نیازمند یک صفحه‌ی UI
تازه + File Picker است، نه فقط چند خط اتصال. **این مورد را هم صریحاً
برای تصمیم مشترک علامت می‌زنم.**

### تصمیم ۷ — Asset: یک مورد قبلاً رفع شد، یک مورد ساختاری زائد، بقیه Post-MVP یا کاندید آینده

- `validateAssetDeletion`: **دیگر یتیم نیست** (G22/ADR-062) — تصحیح
  فهرست اصلی ممیزی.
- `validateImageFile`/`validateReferenceImageFile`: با grep تأیید شد
  هیچ فرم Asset (`CharacterAssetFormScreen.kt`/`LocationAssetFormScreen.kt`/
  `ObjectAssetFormScreen.kt`) هیچ Image/File Picker ای ندارد — هم‌الگو
  با تصمیم قبلی ADR-055 برای `validateImageReferenceFile` (Shot). Post-MVP.
- `validateAssetIdUniqueness`: با grep تأیید شد `assetId` همیشه توسط
  `AssetFormSupport.generateAssetFormId` (UUID) تولید می‌شود — کاربر
  هرگز مستقیماً assetId وارد نمی‌کند. **تصمیم:** ساختاری زائد (نه فقط
  یتیم) — یکتایی توسط تولید UUID عملاً تضمین است. علامت‌گذاری برای حذف
  احتمالی آینده (نه در این قدم).
- `checkSimilarAssetName`: برخلاف بالا، این یکی روی *نام* (فیلد آزاد
  کاربر) کار می‌کند، نه ID — یک هشدار UX واقعاً مفید («نام مشابه یک
  Asset موجود») که هنوز معنادار است. **تصمیم:** کاندید وصل در یک قدم
  آینده‌ی کوچک (فرم‌های Asset، هنگام Submit).

### تصمیم ۸ — Shot: تصمیم قبلی هنوز معتبر است (ADR-055)

`validateImageReferenceFile` همان دلیل بالا (بدون Infra انتخاب‌گر
تصویر) — از قبل در ADR-055 مستند شده. بدون تغییر.

### تصمیم ۹ — Audio: `validateActionSoundTimeline` — یافته‌ی تازه، کاندید رفع نزدیک

با grep تأیید شد `actionSounds`/`timestampSeconds` واقعاً در Sound Tab
(`ShotComposerViewModel.addActionSound`) کاملاً کاربرقابل‌ویرایش است،
اما هیچ‌جا (نه در Aggregator، نه جای دیگر) در برابر `durationSeconds`
شات بررسی نمی‌شود — کاربر می‌تواند یک Timestamp خارج از مدت شات ثبت
کند بدون هیچ هشداری. برخلاف گروه دوربین/حرکت، این یکی در ADR-055 هم
پوشش داده نشده بود. **تصمیم:** کاندید واقعی رفع نزدیک — Aggregator
از قبل `shot.durationSeconds` را در Scope دارد؛ فقط نیاز به افزودن
`addAll(l1, validateActionSoundTimeline(shot.soundProfile.actionSounds, shot.durationSeconds))`
دارد.

### تصمیم ۱۰ — DNA: `validateShotAspectRatio` هم‌دلیل `checkMandatoryElementsPresent` (ADR-055)

`checkMandatoryElementsPresent` از قبل در ADR-055 مستند شده
(«تعریف مبهم عناصر شامل‌شده»). `validateShotAspectRatio` در فهرست آن
ADR نبود، اما با grep تأیید شد **دلیل مشابه صادق است**: `Shot`/
`CameraSettings` هیچ فیلد `aspectRatio` مستقلی ندارد — تنها نسبت
تصویر موجود در کل پروژه، `ProjectDna.outputConstraints.aspectRatio`
است (یک مقدار سراسری، نه per-Shot) — پس چیزی برای «مقایسه‌ی aspectRatio
شات با DNA» وجود ندارد. **تصمیم:** هم‌ردیف با ADR-055 — بدون اقدام،
مگر اگر یک فاز آینده per-Shot aspect ratio override اضافه کند.

### تصمیم ۱۱ — Visual Identity/Style Matrix: ماژول منسوخ (نه در انتظار وصل)، به‌جز یک بخش واقعاً آینده‌دار

با خواندن کامل `StyleMatrix.kt` تأیید شد: این ماژول (`StyleMatrix`/
`primaryStyle`/`secondaryStyle: StyleReference?`) یک مدل داده‌ی کاملاً
موازی و **جایگزین‌شده** با مدل واقعی DNA Tab (`ProjectDna.CoreIdentity.
dominantVisualStyle: VisualStyle`، تک‌مقداری، بدون مفهوم «سبک ثانویه»)
است — واحد ۰۳ (اولیه) هرگز توسط پیاده‌سازی واقعی‌تر واحد ۱۶ فاز ۲ جایگزین
یا حذف نشد. علاوه بر این، خودِ `checkStyleCompatibility` طبق کامنت خودش
یک Placeholder حداقلی است (همیشه `MEDIUM/false` برمی‌گرداند، چون
بلوپرینت هیچ ماتریس سازگاری واقعی تعریف نکرده) — یعنی حتی اگر وصل شود،
امروز هیچ هشدار معناداری تولید نمی‌کند.

**تصمیم:** `checkStyleCompatibility`/`validatePrimaryStyleUpdate`/
`checkStyleMatrixCompatibility` (و کل `StyleMatrix.kt`) علامت‌گذاری
برای حذف/ادغام احتمالی — منسوخ توسط طراحی واقعی DNA Tab، نه در انتظار
وصل‌شدن.

**استثنا — `CinematicLanguage.kt`:** `validateShotDurationForCinematicMode`/
`CinematicMode`/`parseCinematicMode` یک مفهوم مجزا و هنوز منسجم است
(حالت روایی Long-take/Fast-cut/Balanced وابسته به Mood صحنه) که هیچ‌جای
DNA Tab/Shot Composer فعلی وجود ندارد. **تصمیم:** برخلاف بقیه‌ی این
گروه، این یک کاندید واقعی ویژگی آینده است (نه منسوخ) — اگر یک فاز
آینده «حالت سینمایی» را به DNA/Shot اضافه کند.

### تصمیم ۱۲ — AI Story Breakdown باقی‌مانده: هر ۴ مورد جداگانه

- **Rule 1 (`validateFreeformStoryLength`):** نزدیک‌ترین همسایه به همان
  چیزی که در بخش الف همین قدم رفع شد (همان الگو: خطای واقعی + Blocking
  دکمه‌ی «تولید Prompt»). **تصمیم:** کاندید طبیعی قدم کوچک بعدی، همان
  الگوی دقیق بخش الف.
- **Rule 3 (`validateHighShotCount`، Warning):** اولویت پایین‌تر از
  Rule 1 (فقط Warning، نه Blocking) — می‌تواند همراه Rule 1 در همان قدم
  آینده وصل شود.
- **Rule 6 (`validateChunksComplete`، Warning):** با بررسی رفتار فعلی
  تأیید شد این یک شکاف واقعی UX است — اگر کاربر فراموش کند تکه‌ی آخر
  (`[CONTINUE]`) را کامل اضافه کند و مستقیم «ادامه» بزند، پیام خطای
  فعلی یک خطای خام Parse JSON نامفهوم است، نه پیام واضح این Rule
  («پاسخ هنوز کامل نیست»). **تصمیم:** کاندید بهبود UX واقعی برای آینده.
- **Rule 7 (`validateJsonRepairResult`، Blocking):** با بررسی
  `AiStoryBreakdownViewModel.processResponse()` تأیید شد این Rule
  **عملاً توسط پیاده‌سازی بهتری جایگزین شده** — همان شرط
  (`JsonRepairResult.NeedsManualRepair`) از قبل به `JsonRepairDialog`
  (Modal با دکمه‌ی تعمیر خودکار/ویرایش دستی، UX به‌مراتب بهتر از یک پیام
  ValidationIssue ساده) منتهی می‌شود. **تصمیم:** بدون نیاز به وصل —
  منسوخ‌شده توسط یک راه‌حل بهتر، نه فراموش‌شده.
- **Rule 4/5 (`validateApiKeyProvided`/`validateAiConnectorErrorMessage`):**
  طبق ارزیابی خودِ دستور کار، درست و بدون نیاز به بررسی بیشتر —
  `sendToAiConnector` خودش TODO است (G2)، این دو Rule منطقاً همراه آن
  حل می‌شوند.

---

## خلاصه‌ی جدول تصمیمات

| گروه | یتیم واقعی؟ | تصمیم |
|---|---|---|
| دوربین/حرکت | بله | بدون تغییر (ADR-055 هنوز معتبر) |
| تجمیع سطح‌بالا (`validateDataCompleteness`) | بله | کاندید رفع نزدیک |
| State Machine/Lock | نه دقیقاً (منطق تکراری، نه Bypass) | کاندید یکسان‌سازی — **برای تصمیم مشترک** |
| Output Delivery/i18n (پروفایل/ترجمه) | بله، کم‌ارزش | Post-MVP |
| Output Delivery (`OutputComposer`/Bilingual) | بله | منسوخ — علامت حذف احتمالی |
| Prompt Finalization | **خیر** | تصحیح — از قبل کاملاً وصل است |
| Storage/Import | بله | یافته‌ی محصولی — **برای تصمیم مشترک** |
| Asset (`validateAssetDeletion`) | **خیر** | تصحیح — قبلاً رفع شد (G22) |
| Asset (image/reference file) | بله | Post-MVP (بدون Infra) |
| Asset (`validateAssetIdUniqueness`) | بله | زائد ساختاری — علامت حذف احتمالی |
| Asset (`checkSimilarAssetName`) | بله | کاندید وصل آینده |
| Shot (`validateImageReferenceFile`) | بله | بدون تغییر (ADR-055 هنوز معتبر) |
| Audio (`validateActionSoundTimeline`) | بله | کاندید رفع نزدیک |
| DNA (`checkMandatoryElementsPresent`) | بله | بدون تغییر (ADR-055 هنوز معتبر) |
| DNA (`validateShotAspectRatio`) | بله | هم‌دلیل بالا — بدون تغییر |
| Visual Identity (Style Matrix) | بله | منسوخ — علامت حذف احتمالی |
| Visual Identity (Cinematic Language) | بله | کاندید ویژگی آینده |
| AI Story Breakdown Rule 1/3 | بله | کاندید قدم بعدی (همان الگوی بخش الف) |
| AI Story Breakdown Rule 6 | بله | کاندید بهبود UX آینده |
| AI Story Breakdown Rule 7 | نه دقیقاً | منسوخ — جایگزین بهتر از قبل موجود |
| AI Story Breakdown Rule 4/5 | بله (عمدی) | وابسته به G2 (`sendToAiConnector`) |

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
