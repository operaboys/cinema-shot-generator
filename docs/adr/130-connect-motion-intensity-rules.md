# ADR-130: قدم ۲ از ۲ (پایانی) — اتصال Rule های یتیم validateSpeedIntensity/validateMotionBlur

## وضعیت

پذیرفته‌شده

## زمینه

`domain/camera/MotionIntensityValidation.kt` دو Rule یتیم داشت —
`validateSpeedIntensity(motion: SubjectMotion)` و
`validateMotionBlur(speed: SubjectSpeed, blur: MotionBlurAmount)`. با `grep`
مستقل تأیید شد هیچ‌کدام فراخوان‌کننده‌ی واقعی نداشتند؛
`domain/camera/MotionIntensityModels.kt` (`SubjectMotion`/`MotionBlur`) هم
هیچ معادلی در `domain/shot/ShotModels.kt` نداشت.

این قدم دوم و پایانی برنامه‌ی «اتصال Rule های یتیم دوربین و حرکت» است —
قدم اول (ADR-129) `validateCameraMovementDuration` را با افزودن
`CameraSettings.movementDurationSeconds` وصل کرد؛ این قدم مشابه، اما با
وابستگی داده‌ی متفاوت (`SubjectMotion` هیچ معادلی در `Shot` نداشت، برخلاف
ADR-129 که فقط یک فیلد به یک data class موجود اضافه می‌کرد).

### چرا SubjectSpeed و MotionLevel به هم نگاشت نشدند — ADR-008 (تصمیم ۴)

`docs/adr/008-unit09-camera-motion-deviations.md` (تصمیم ۴) این را از قبل
مستند کرده بود: «طبق دستور کار صریح، این دو enum مستقل باقی ماندند
(چهار مقداره در برابر پنج مقداره، بدون تناظر یک‌به‌یک واضح)... این یک
سؤال باز جدی نیست، صرفاً یادداشت برای آگاهی معمار.» این قدم آن استقلال
را دقیقاً حفظ می‌کند — `subjectMotion`/`motionBlur` تازه هیچ رابطه‌ای با
`motionLevel` موجود برقرار نمی‌کنند، نه در دامنه و نه در UI.

### راستی‌آزمایی مستقل

- `ShotModels.kt`، `MotionIntensityModels.kt`/`MotionIntensityValidation.kt`،
  `ShotDto.kt`/`DtoMappers.kt`، `ValidationAggregator.kt` کامل خوانده شدند —
  همه دقیقاً همان‌طور که پیش‌بررسی توصیف کرده بود.
- `ShotComposerViewModel.kt`/`ShotComposerScreen.kt` (بخش‌های
  `motionLevel`/`cinematicModeOverride`) کامل خوانده شدند — یافته‌ی مهم:
  `cinematicModeOverride`/`negativePromptOverride` (نه `camera`/`lighting`/
  `environment`) الگوی درست برای این دو فیلد تازه بودند (پایین‌تر توضیح
  داده شده).
- `resolveCameraSettings`/`resolveLightingSettings`/`resolveEnvironmentSettings`
  (`ShotSettingsResolution.kt`) و فراخوان‌های واقعی‌شان
  (`SettingsResolutionRepository.kt`) بررسی شد: `sceneDefault` در تمام
  فراخوان‌های واقعی همیشه `null` است — یعنی حتی مکانیزم ارث‌بری موجود
  camera/lighting/environment از Scene، در عمل امروز یک منبع واقعی ندارد.
- تأیید شد `motionLevel` دقیقاً در همان ۱۰ فایلی است که پیش‌بررسی گفته بود
  (بدون فایل کم/زیاد) — این ۱۰ فایل کاملاً دست‌نخورده ماندند.

هیچ مغایرتی با پیش‌بریفینگ پیدا نشد.

## تصمیم

### چرا subjectMotion/motionBlur مستقیماً روی Shot‌اند، نه یک SourcedSettings تازه

`camera`/`lighting`/`environment` روی `Shot` هرکدام `SourcedSettings<T>`اند
چون یک معادل واقعی در سطح Scene دارند که می‌توانند از آن ارث ببرند
(source="scene" در برابر "override"). `subjectMotion`/`motionBlur` چنین
معادلی ندارند — `domain/scene/SceneModels.kt` هیچ فیلد
`CameraSettings`/`LightingSettings`/`EnvironmentSettings`-مانندی برای
Motion Intensity ندارد تا از آن ارث برد؛ حتی برای همان سه فیلد موجود،
راستی‌آزمایی بالا نشان داد `sceneDefault` واقعی امروز همیشه `null` است.
افزودن یک `SourcedSettings` تازه بدون هیچ منبع Scene واقعی، فقط پیچیدگی
بی‌فایده (یک سوییچ source که همیشه معنای یکسان دارد) اضافه می‌کرد.

الگوی درست، همان `cinematicModeOverride`/`negativePromptOverride` موجود
است — هر دو nullable مستقیم روی `Shot`، بدون `SourcedSettings`، دقیقاً
برای همین دلیل (Override محلی سطح شات، بدون ارث‌بری واقعی از یک منبع
دیگر). `subjectMotion`/`motionBlur` همان الگو را دنبال می‌کنند.

### چرا فقط MotionBlurAmount، نه کل data class MotionBlur

`data class MotionBlur(val amount: MotionBlurAmount)` تنها یک فیلد دارد و
امضای واقعی `validateMotionBlur(speed: SubjectSpeed, blur: MotionBlurAmount)`
فقط همان یک فیلد را می‌خواهد. نگه‌داشتن `MotionBlur` به‌عنوان Wrapper روی
`Shot` یک لایه‌ی بی‌فایده بود؛ `Shot.motionBlur: MotionBlurAmount?` مستقیم
هم ساده‌تر است و هم دقیقاً همان چیزی که Rule نیاز دارد.

### پیاده‌سازی

**۱. دامنه** (`ShotModels.kt`): دو فیلد تازه روی `Shot`:

```kotlin
val subjectMotion: SubjectMotion? = null,
val motionBlur: MotionBlurAmount? = null
```

**۲. DTO/Mapper**: `ShotDto.kt` یک `SubjectMotionDto` تازه (معادل مستقیم
`SubjectMotion`، enum ها به‌صورت String — هم‌الگو با بقیه‌ی این فایل) +
`subjectMotion: SubjectMotionDto? = null` / `motionBlur: String? = null`.
`Json { ignoreUnknownKeys = true }` موجود یعنی بدون Migration، شات‌های
موجود کاربر (که این دو فیلد را ندارند) بدون خطا Decode می‌شوند. `DtoMappers.kt`
(هر دو جهت) به‌روزرسانی شد.

**۳. `ValidationAggregator.kt`** (داخل بلوک Level 2، مستقیماً روی `shot`،
نه `shot.camera.overrideValue` — هم‌الگو با `checkSlowMotionInDialogue`
بالاش که آن هم فقط فیلدهای مستقیم همین Shot را می‌سنجد):

```kotlin
shot.subjectMotion?.let { motion ->
    add(l2, validateSpeedIntensity(motion))
    shot.motionBlur?.let { blur -> add(l2, validateMotionBlur(motion.speed, blur)) }
}
```

`validateSpeedIntensity` فقط با `subjectMotion` غیر-null صدا زده می‌شود؛
`validateMotionBlur` فقط وقتی هر دو `subjectMotion` و `motionBlur`
غیر-null باشند (امضای آن هر دو را می‌خواهد).

**۴. UI/ViewModel**: `ShotComposerViewModel.kt` چهار StateFlow تازه
(`subjectSpeed: SubjectSpeed?`، `subjectMotionType: MotionType?`،
`subjectIntensityText: String` — متن آزاد چون بلوپرینت intensity را عدد
صحیح ۰ تا ۱۰ تعریف کرده نه Enum، `motionBlur: MotionBlurAmount?`) + چهار
Setter؛ `buildSubjectMotion()` فقط وقتی هر سه زیرفیلد واقعاً پر شده باشند
یک `SubjectMotion` واقعی می‌سازد، وگرنه `null`. `ShotComposerScreen.kt`
(`MainFieldsSection`) یک بخش UI کاملاً **مجزا** از `motionLevel` اضافه کرد
(نه ادغام‌شده در همان بخش) — با یک عنوان («حرکت سوژه (مستقل)») و متن
راهنمای صریح که توضیح می‌دهد این فیلدها مستقل از «سطح حرکت» بالاترند؛ طبق
ADR-008 (تصمیم ۴) این دو Enum عمداً به هم نگاشت نشده‌اند، پس نمایش‌شان
کنار هم بدون توضیح می‌توانست به کاربر القا کند این دو یک مفهوم واحدند.

## پیامدها

- هیچ رفتار مشاهده‌پذیر برای پروژه‌های موجود کاربر تغییر نکرد — هر دو فیلد
  جدید `null` پیش‌فرض دارند و هر دو Rule فقط با مقدار واقعی صدا زده
  می‌شوند.
- `motionLevel` و هر ۱۰ فایل وابسته به آن کاملاً دست‌نخورده ماندند — هیچ
  رابطه‌ای بین `subjectMotion`/`motionBlur` و `motionLevel` برقرار نشد.
- کامنت «فهرست Rule های عمداً وایرنشده» در `ValidationAggregator.kt`
  به‌روزرسانی شد تا وضعیت جدید (وصل‌شده) را منعکس کند — همان انضباط
  ADR-128/ADR-129.
- این قدم، **قدم ۲ از ۲ و پایانی** برنامه‌ی «اتصال Rule های یتیم دوربین و
  حرکت» (ADR-129 + این ADR) را می‌بندد.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`: موفق.
- `ValidationAggregatorTest.kt`: ۲۴ تست (۱۸ قبلی + ۶ تازه)، همه Pass —
  هر دو فیلد null→بدون Issue، SLOW+intensity>6→Warning، HYPERKINETIC+
  intensity<7→Warning، SLOW+EXTREME blur→Warning، HYPERKINETIC+NONE
  blur→Warning، ترکیب معتبر→بدون Issue.
- `MotionIntensityValidationTest.kt` (۶ تست موجود، بدون تغییر امضا)،
  `UiStringsTest.kt` (۵ تست)، `DtoMappersTest.kt` (۸ تست): همه Pass،
  بدون تغییر رفتار.
- کل `com.operaboys.cinemashotgenerator.ui.shots.*` (شامل
  `ShotComposerViewModelTest`/`ShotComposerAccordionFlowTest`/
  `ShotComposerSummaryFlowTest`/`ShotComposerHeroGradientTest`): همه Pass
  — بدون رگرسیون از بخش UI تازه.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۹۵۸ تست، ۹۵۶ Pass، ۲ Fail —
  `AppNavigationTest` و `OutputDeliveryFlowTest`، هر دو از پیش در فهرست
  کلاس‌های ناپایدار (Flaky) مستندشده‌ی این پروژه (بدون هیچ ارتباطی با
  تغییرات این ADR — ناوبری/تحویل خروجی، نه Shot Composer/دوربین/حرکت). با
  اجرای مجدد و مجزا (`--tests`) هر دو کلاس کامل Pass شدند — تأیید شد
  Flakiness شناخته‌شده است، نه رگرسیون واقعی.
- `./gradlew :app:assembleDebug`: موفق.
