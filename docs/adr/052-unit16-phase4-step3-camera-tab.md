# ADR-052: واحد ۱۶ — فاز ۴، قدم ۳: محتوای کامل Tab «دوربین» Shot Composer

**تاریخ:** 2026-08-06
**وضعیت:** کامل شد. `gradle :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL` (۶۲۰ تست، ۰ شکست، ۰ خطا).

## Context

سومین قدم فاز ۴ واحد ۱۶ — عمداً محدود به Tab «دوربین» (پیچیده‌ترین Tab این
صفحه)؛ Tab‌های «نور و محیط» و «صدا» قدم بعدی جداگانه‌اند. منابع حقیقت:
`docs/blueprints/09-camera-and-motion.md` (بخش الف — Camera System) و
`docs/design/README.md` بخش «۷. Shot Composer».

## تصمیم ۱ (چالش اصلی دستور کار): طراحی فرم شرطی Movement — دو ردیف Basic/Advanced

`CameraMovement` (`sealed class`، ۶ زیرکلاس با فیلدهای کاملاً متفاوت) اولین
sealed class این کدبیس بود که نیاز به فرم UI شرطی «انتخاب Variant → فیلدهای
همان Variant» داشت — با جست‌وجوی کامل `ui/` (grep روی همه‌ی sealed class های
دامنه) تأیید شد هیچ الگوی مشابه از قبل در پروژه وجود ندارد که بتوان مستقیماً
از آن پیروی کرد (سایر sealed class ها همگی نوع Result/Permission داخلی‌اند،
نه مدل فرم قابل‌ویرایش).

طراحی انتخاب‌شده: دقیقاً همان ساختار دو‌لایه‌ی خودِ بلوپرینت («**پایه:**
Static, Pan Left/Right, ...» در برابر «**پیشرفته (v1.1)**: Orbit, Drone
Path, ...») — یک سوییچ Tier (Basic/Advanced، دو گزینه)، سپس اگر Advanced، یک
Dropdown دوم برای انتخاب یکی از ۵ Variant پیشرفته. یافته‌ی جانبی: enum
`AdvancedMovementType` (۵ مقدار، دقیقاً منطبق با ۵ Variant پیشرفته) از قبل در
`domain/camera/CameraModels.kt` تعریف شده بود اما با grep تأیید شد در هیچ‌جای
کد (نه دامنه، نه UI) تا این قدم استفاده نشده بود — این قدم اولین مصرف‌کننده‌ی
واقعی آن است، دقیقاً به‌عنوان مقدار انتخاب‌گر Dropdown دوم؛ اختراع یک enum
موازی («UI فقط») غیرضروری بود.

State مدیریت شده به‌صورت یک `MutableStateFlow<CameraMovement>` تکی (نه یک
مجموعه‌ی State موازی به‌ازای هر فیلد هر Variant) — Tier/Variant فعلی همیشه
مستقیماً از روی نوع Runtime همین یک مقدار مشتق می‌شود
(`CameraMovement.tier()`/`advancedTypeOrNull()`، توابع Extension در
`ShotComposerViewModel.kt`)، نه از یک متغیر جدا که ممکن است Desync شود.
انتخاب یک Kind/Variant جدید همیشه یک نمونه‌ی پیش‌فرض معنادار از همان Variant
می‌سازد (نه پاک‌کردن/صفر‌کردن فیلدها) — مثلاً سوییچ به Advanced پیش‌فرض
`Orbit(degrees=180, speed="medium", maintainEyeLevel=true)` می‌سازد.

## تصمیم ۲: واژگان بسته (Closed Vocabulary) کجا Dropdown و کجا متن آزاد

با خواندن دقیق جدول «حرکات پیشرفته» بلوپرینت (نه فقط امضای Kotlin که این
مقادیر را `Int`/`String` تایپ کرده)، مشخص شد چند فیلد — با اینکه در دامنه
`Int`/`String` هستند — عملاً واژگان بسته دارند:

- `Orbit.degrees`: بلوپرینت صریحاً «۹۰/۱۸۰/۳۶۰» را نام برده — Dropdown با
  دقیقاً همین ۳ مقدار، نه ورودی عددی آزاد.
- `DronePath.altitudeChange`: «ascending/descending/level» — Dropdown ۳تایی.
- `DronePath.pathType`: «straight/curved/spiral» — Dropdown ۳تایی.
- `DollyZoom.direction`: بلوپرینت مقداری صریح نداده، اما معنای فیزیکی خودِ
  Dolly Zoom («Vertigo Effect»، زوم هم‌زمان با حرکت مخالف دوربین) فقط دو
  جهت منطقی دارد: «به‌سمت داخل»/«به‌سمت بیرون» — Dropdown دوتایی، یک واژگان
  استنتاج‌شده از معنای دامنه (هم‌الگو با تصمیمات مشابه قبلی پروژه، نه یک
  اختراع بدون‌مبنا).
- بقیه‌ی فیلدهای `String` (سرعت‌ها، `frequency`، `primary`/`secondary`/`sync`
  ترکیب) هیچ واژگان بسته‌ای در بلوپرینت ندارند — فیلد متن آزاد باقی ماندند.
- `HandheldShake.intensity` (بلوپرینت: «۰ تا ۱۰») — `Slider` با
  `valueRange=0f..10f`، `steps=9` (۱۱ مقدار گسسته)، نه ورودی متنی.
- `DollyZoom.focalStart`/`focalEnd` (میلی‌متر لنز) — بدون واژگان بسته‌ی
  مشخص در بلوپرینت، ورودی عددی آزاد (`OutlinedTextField` با پارس Int).

## تصمیم ۳: منبع/Override — فیلدها فقط در حالت Override رندر می‌شوند (نه Disabled)

طبق دستور کار («سوییچ منبع/Override») و طبق ADR-013 (واحد ۰۵) — Scene هیچ
فیلد پیش‌فرض camera ندارد (`sceneDefault` همیشه `null` در تنها فراخوان واقعی
`resolveCameraSettings`، `SettingsResolutionRepository.kt`) — یعنی حالت
«scene» هیچ مقدار واقعاً قابل‌نمایشی برای ارث‌بری ندارد. به‌جای رندر همه‌ی
فیلدها با `enabled=false` (که نیاز به افزودن پارامتر `enabled` به
`AssetFormEnumDropdownField` مشترک داشت و برای این حالت گمراه‌کننده بود —
چیزی برای Disable-نمایش‌دادن واقعاً وجود ندارد)، طراحی ساده‌تر انتخاب شد: در
حالت «scene» فقط یک توضیح («این شات از تنظیمات دوربین صحنه پیروی می‌کند»)
نمایش داده می‌شود؛ کل فرم فقط با انتخاب «سفارشی‌سازی برای این شات» ظاهر
می‌شود. مقدار `overrideValue` همیشه (حتی در حالت «scene») با آخرین State فرم
در حافظه پر و ذخیره می‌شود — طوری که سوییچ رفت‌وبرگشتی بین دو حالت هیچ داده‌ی
کاربر را گم نمی‌کند؛ `resolveCameraSettings` خودش این مقدار را فقط وقتی
`source=="override"` باشد در نظر می‌گیرد (منطق موجود واحد ۰۵، بدون تغییر).

## تصمیم ۴: ۴ Rule خودبسنده‌ی بخش الف واحد ۰۹ زنده وایر شدند؛ Rule پنجم عمداً نه

از ۵ Rule جدول اعتبارسنجی بلوپرینت ۰۹، چهارتای اول ورودی‌شان کاملاً داخل
همین Tab (یا از قبل در Shot موجود است) در دسترس است — زنده نمایش داده
می‌شوند (`AssetFormValidationIssueRow`، غیر-مسدودکننده‌ی Auto-Save، دقیقاً
هم‌الگو با تصمیم `shotDescriptionValidation` در ADR-051):

- لنز Ultra-wide + Close-up/Extreme Close-up (Blocking)
- Static Movement + Handheld Stabilization (Warning)
- Extreme Wide + Shallow DoF (Warning)
- Rack Focus + کمتر از ۲ Subject (Blocking) — `subjectCount` از
  `characterIds.size + objectIds.size` شات بارگذاری‌شده محاسبه می‌شود (این
  دو فیلد در این Tab ویرایش نمی‌شوند، فقط خوانده می‌شوند).

Rule پنجم («مدت حرکت دوربین بیشتر از Duration شات») عمداً وایر **نشد** —
طبق ADR-008 (واحد ۰۹)، هیچ‌کدام از ۶ Variant سازنده‌ی `CameraMovement` فیلد
`duration` ندارند؛ تابع دامنه‌ی `validateCameraMovementDuration` مدت حرکت را
به‌عنوان پارامتر خارجی مستقل می‌گیرد که Shot Composer فعلاً معادل واقعی‌اش
را ندارد (نه یک فیلد فراموش‌شده، یک محدودیت شناخته‌شده‌ی از پیش مستند).

## تصمیم ۵: «Attached References» — فقط نوع + توضیح متنی، بدون آپلود واقعی فایل

طبق تصریح دستور کار («با grep بررسی کن آیا مدیریت کامل آپلود تصویر لازم است
یا فقط نمایش/افزودن ساده کافی است»): با grep کامل `ui/` تأیید شد **هیچ**
Infra انتخاب‌گر تصویر (`ActivityResultContracts`, `PickVisualMedia`,
`rememberLauncherForActivityResult`, ...) در کل کدبیس وجود ندارد. ساخت این
زیرساخت از صفر، خارج از Scope یک قدم که فقط Tab دوربین را هدف گرفته بود.
پیاده‌سازی: کاربر نوع مرجع (character/style/composition — دقیقاً طبق سند
طراحی) را از Dropdown انتخاب و توضیح متنی وارد می‌کند؛ `ImageReference`
واقعی با `localFilePath=""` ساخته و ذخیره می‌شود (فیلد Domain بدون تغییر
می‌ماند). مدیریت واقعی آپلود/انتخاب فایل محلی، بدهی ثبت‌شده برای یک قدم
مستقل آینده است.

## تصمیم ۶: بخش «ب» بلوپرینت ۰۹ (Motion Intensity) خارج از Scope این Tab

با grep تأیید شد `MotionIntensitySettings`/`SubjectMotion`/`CameraMotion`/
`MotionBlur` (بخش ب بلوپرینت، `domain/camera/MotionIntensityModels.kt`) در
هیچ‌جای `Shot` (`domain/shot/ShotModels.kt`) ارجاع داده نشده‌اند — `Shot`
فقط `motionLevel: MotionLevel` (enum ساده‌ی پنج‌مقداره، قبلاً در فاز ۴ قدم ۲
به فیلدهای سطح‌بالا وصل شده) را دارد، نه هیچ فیلدی از نوع
`MotionIntensitySettings`. چون `Shot.camera` دقیقاً `SourcedSettings<CameraSettings>`
است (فقط بخش الف)، و بخش ب اصلاً به هیچ فیلدی از `Shot` وصل نیست، اضافه‌کردن
UI برایش در این Tab به معنای اختراع یک فیلد جدید روی `Shot` بود — خارج از
Scope این قدم (که فقط باید محتوای Tab دوربین موجود Shot Composer را کامل
می‌کرد، نه دامنه‌ای جدید می‌ساخت). این یک هم‌پوشانی مفهومی از‌پیش‌مستندشده
است (ADR-008، تصمیم ۴)، نه یک کمبود تازه.

## یافته‌ی تست ۱: کلیک روی Dropdown پایین‌تر از ناحیه‌ی دیده‌شده بدون `performScrollTo()` باز نمی‌شود

هنگام نوشتن تست UI برای سوییچ ۶ نوع Movement، کلیک ساده
(`performClick()`/`clickViaSemantics()`) روی فیلد Dropdown «حرکت پیشرفته»
(`AssetFormEnumDropdownField`) — که پایین‌تر از ناحیه‌ی اولیه‌ی دیده‌شده‌ی
`verticalScroll` این صفحه قرار دارد — منو را باز نمی‌کرد؛ تأییدشده با دیباگ
مستقیم (`onAllNodesWithText` بعد از کلیک همچنان فقط ۱ مورد پیدا می‌کرد، نه
۲ = فیلد بسته + آیتم منو). فیلدهای بالای همان صفحه (Angle/Distance/...) این
مشکل را نداشتند. رفع شد با `performScrollTo()` صریح پیش از کلیک — یک نمونه‌ی
تازه از همان کلاس مشکل مستندشده‌ی قبلی پروژه («کلیک غیرقابل‌اعتماد» —
ADR-045/047/050)، این‌بار برای المان‌های پایین‌تر از Viewport اولیه، نه
FAB/Card/DropdownMenuItem.

## یافته‌ی تست ۲: Race واقعی بین Auto-Save ناهمگام و `database.close()` در `tearDown()`

تستی که چند سوییچ UI سریع پشت‌سرهم انجام می‌دهد (هرکدام یک Auto-Save ناهمگام
مستقل `ioScope.launch` صف می‌کند) گاهی — نه همیشه — با
`IllegalStateException: Cannot perform this operation because the
connection pool has been closed` شکست می‌خورد: `tearDown()`'s
`database.close()` گاهی پیش از تکمیل واقعی آخرین Coroutine صف‌شده روی Main
Dispatcher اجرا می‌شد. رفع شد با افزودن `composeRule.waitForIdle()` در
ابتدای `tearDown()` (پیش از `database.close()`) در `ShotsFlowTest.kt` —
تضمین می‌کند همه‌ی کارهای صف‌شده‌ی معلق واقعاً اجرا شده‌اند پیش از بستن
دیتابیس. تأییدشده پایدار در چند اجرای متوالی پس از رفع.

## Consequences

- `ShotComposerScreen.kt`/`ShotComposerViewModel.kt` بدون تغییر رفتار سایر
  Tab‌ها (اصلی/نور و محیط/صدا همچنان Placeholder، طبق Scope دقیق این قدم).
- `AssetFormHeader`/`AssetFormEnumDropdownField`/`AssetFormFlatEntries`/
  `AssetFormValidationIssueRow`/`OpaqueChip` (همگی از `ui/assets`) دوباره
  بازاستفاده شدند — همان الگوی Cross-Feature تثبیت‌شده‌ی قدم‌های قبل، بدون
  نیاز به تغییر در خودِ آن اجزا.
- کار آینده‌ی شناخته‌شده: قدم بعدی فاز ۴ (Tab «نور و محیط» + «صدا»)؛ مدیریت
  واقعی آپلود تصویر برای Attached References (خارج از Scope این قدم، بدهی
  ثبت‌شده)؛ Motion Intensity (بخش ب واحد ۰۹) همچنان به هیچ فیلدی از `Shot`
  وصل نیست (هم‌پوشانی مفهومی از‌پیش‌مستندشده، ADR-008).
