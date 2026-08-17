# ADR-110: تکمیل Rule یتیم CinematicLanguage — قدم ۳الف از ۲ زیرقدم قدم ۳ (اتصال checkFastMotionLongTake و checkStaticCameraInChase)

## زمینه

قدم ۲ (ADR-106 تا ADR-109) هوشمندی کامل `resolveEffectiveCinematicMode`
(زنجیره‌ی سه‌سطحی Override + دو تابع Hybrid/Mood) را وصل کرد. قدم ۳ به سه
Rule تعارض «دوربین و حرکت» مربوط است که بخش «تداخل با واحدهای دیگر»
بلوپرینت `docs/blueprints/03-visual-identity.md` صریح خواسته بود (هر سه
همیشه **Warning**، نه Blocking):

- حرکت سریع + Long-take.
- دوربین ثابت + صحنه‌ی تعقیب.
- Slow Motion + دیالوگ.

قدم ۳ خودش به **دو** زیرقدم تقسیم شده (نه سه): این زیرقدم (۳الف) دو Rule
اول را که از قبل کاملاً پیاده و تست شده بودند اما به هیچ‌جا وصل نبودند وصل
می‌کند. زیرقدم ۳ب (جداگانه، هنوز شروع‌نشده) سومین Rule (که هنوز نوشته
نشده) اضافه می‌کند.

## بررسی مستقل (پیش از نوشتن کد)

**یافته‌ی پیش‌بریفینگ (صفر فراخوان‌کننده‌ی واقعی):** با `grep` مستقیم روی
`app/src/main` تأیید شد `checkFastMotionLongTake` و
`checkStaticCameraInChase` (هر دو در
`domain/validation/LogicConflictChecker.kt`) فقط از تست خودشان
(`LogicConflictCheckerTest.kt`) فراخوانی می‌شدند — **تأیید کامل، بدون
اختلاف.**

**کامنت مستندشده‌ی خودِ `checkStaticCameraInChase` (ADR-010):** خوانده شد.
دلیل صریح برای String ماندن `cameraMovementType`: «معادل واقعی‌اش در واحد
۰۹ یا `BasicMovementType` (فقط حرکات پایه، نه Orbit/DronePath/...) یا
خودِ `CameraMovement` (sealed class، نیازمند Pattern Matching اضافه) است —
هیچ‌کدام تناظر تمیز و مستقیمی ندارند». این تصمیم صریح بود و بدون تغییر
باقی ماند — این ADR فقط نگاشت لازم را (در `ValidationAggregator.kt`، نه در
خودِ `LogicConflictChecker.kt`) اضافه می‌کند.

## تصمیم ۱ — سطح Validation: چرا این دو Rule در دو سطح متفاوت قرار گرفتند

پیش‌بریفینگ صریحاً پرسیده بود کدام سطح مناسب‌تر است. با خواندن دقیق تعریف
موجود دو سطح در `ValidationAggregator.kt`:

- **Level 2 (`LOGICAL_CONSISTENCY`):** «ترکیب فیلدهای خودِ همین Shot/Scene
  با هم» — بدون نیاز به DNA.
- **Level 3 (`CONTINUITY_AND_DEPENDENCY`):** «این Shot در برابر DNA پروژه و
  Asset های متصل».

نتیجه — **دو Rule، دو سطح متفاوت، با دلیل مستقل برای هرکدام:**

- **`checkFastMotionLongTake` → Level 3.** پارامتر دومش
  (`effectiveCinematicMode`) از `resolveEffectiveCinematicMode(dna, scene,
  shot)` می‌آید — دقیقاً همان تابع چندمنبعی (DNA + Scene + Shot) که
  `validateShotDurationForCinematicMode` را هم تغذیه می‌کند و در ADR-106
  **به همین دلیل دقیق** به Level 3 وصل شده بود («مدت شات را در برابر یک
  محدودیت مشتق‌شده از چند منبع می‌سنجد، نه صرفاً داده‌ی خودِ همین Shot»).
  برای سازگاری با همان تصمیم مستندشده، `checkFastMotionLongTake` هم که
  دقیقاً همان مقدار چندمنبعی را مصرف می‌کند، باید همان سطح را بگیرد —
  نه یک تصمیم دلبخواهی جدید.
- **`checkStaticCameraInChase` → Level 2.** هر دو پارامترش
  (`camera.movement`، `shot.shotDescription`) مستقیماً فیلدهای خودِ همین
  Shot هستند، بدون هیچ وابستگی به DNA یا Scene — دقیقاً هم‌جنس
  `checkLensDistanceMismatch`/`checkStaticMovementWithHandheldStabilization`
  موجود که هر دو در همان بلوک `shot.camera.overrideValue?.let { camera ->
  ... }` در Level 2 زندگی می‌کنند.

## تصمیم ۲ — نگاشت `CameraMovement` → `String`

```kotlin
private fun CameraMovement.toMovementTypeString(): String = when (this) {
    is CameraMovement.Basic -> type.name.lowercase()
    is CameraMovement.Orbit -> "orbit"
    is CameraMovement.DronePath -> "drone_path"
    is CameraMovement.DollyZoom -> "dolly_zoom"
    is CameraMovement.HandheldShake -> "handheld_shake"
    is CameraMovement.Compound -> "compound"
}
```

الزام واقعی این Rule (طبق کامنت خودِ `checkStaticCameraInChase`): فقط
`CameraMovement.Basic(type = BasicMovementType.STATIC)` باید دقیقاً
`"static"` تولید کند — `type.name.lowercase()` این را تضمین می‌کند، چون
`BasicMovementType.STATIC.name.lowercase() == "static"` و بقیه‌ی مقادیر
`BasicMovementType` (`PAN_LEFT`, `TRACKING`, ...) به رشته‌های غیر-"static"
تبدیل می‌شوند. برای پنج زیرکلاس «پیشرفته» (`Orbit`/`DronePath`/`DollyZoom`/
`HandheldShake`/`Compound`، که ذاتاً حرکات ثابت نیستند)، هر رشته‌ی
غیر-"static" کافی بود؛ نام کلاس (لغزیده به snake_case) برای خوانایی/
دیباگ‌پذیری انتخاب شد — بدون معنای خاصی فراتر از «این static نیست».
این تابع خصوصی و محلی به `ValidationAggregator.kt` است (نه تغییری در
`LogicConflictChecker.kt` یا `domain.camera`).

## تصمیم ۳ — بدون ارث‌بری (فقط `camera.overrideValue` صریح)

`checkStaticCameraInChase` داخل همان بلوک موجود `shot.camera.overrideValue
?.let { camera -> ... }` اضافه شد — دقیقاً هم‌الگو با چهار Rule دیگر همان
بلوک (`checkLensDistanceMismatch` و بقیه) که هیچ‌کدام روی مقدار
ارث‌بری‌شده از Scene اجرا نمی‌شوند، فقط روی Override صریح شات. این الگوی
از پیش موجود این فایل بود — بازاستفاده شد، نه اختراع یک رفتار تازه.

## پیاده‌سازی

```kotlin
// Level 2
shot.camera.overrideValue?.let { camera ->
    ...
    add(l2, checkStaticCameraInChase(camera.movement.toMovementTypeString(), shot.shotDescription))
    ...
}

// Level 3
val effectiveCinematicMode = resolveEffectiveCinematicMode(dna, scene, shot)
add(l3, validateShotDurationForCinematicMode(shot.durationSeconds, effectiveCinematicMode))
add(l3, checkFastMotionLongTake(shot.motionLevel, effectiveCinematicMode))
```

`resolveEffectiveCinematicMode` یک‌بار محاسبه و در یک `val` محلی
(`effectiveCinematicMode`) نگه داشته شد تا هم `validateShotDurationForCinematicMode`
(ADR-106) و هم `checkFastMotionLongTake` (این ADR) از همان مقدار محاسبه‌شده
استفاده کنند — بدون محاسبه‌ی تکراری.

## تست

**`ValidationAggregatorTest.kt` (۴ تست تازه، طبق مشخصات دقیق دستور کار):**
- `motionLevel=DYNAMIC` + `effectiveCinematicMode=LONG_TAKE` (با
  `durationSeconds=10f`، عمداً داخل بازه‌ی مجاز LONG_TAKE تا هشدار مدت‌زمان
  بی‌ربط قاطی نشود) → Warning واقعی در Level 3.
- `motionLevel=SUBTLE` + همان `LONG_TAKE` → بدون هشدار حرکت سریع.
- `camera.movement=Basic(STATIC)` (پیش‌فرض `neutralCamera()`) +
  `shotDescription` حاوی «chase» → Warning واقعی در Level 2.
- همان شرح، اما `camera.movement=Basic(TRACKING)` → بدون هشدار دوربین
  ثابت.

**رگرسیون — `LogicConflictCheckerTest.kt` (هر ۷ تست موجود، بدون تغییر
کد):** بررسی و اجرا شد. این قدم فقط محل فراخوانی جدید اضافه کرد، منطق
خودِ دو تابع در `LogicConflictChecker.kt` دست‌نخورده ماند — هر ۷ تست بدون
تغییر سبز ماندند.

**رگرسیون — بقیه‌ی ۹ تست موجود `ValidationAggregatorTest.kt` (بدون تغییر
کد):** بررسی و اجرا شد. `neutralShot()`/`neutralCamera()` پیش‌فرض
(`motionLevel=SUBTLE`، `camera.movement=Basic(STATIC)`، شرح بدون کلمات
تعقیب) با هیچ‌کدام از این دو Rule تازه تداخل ندارد — تست «صفر مشکل»
همچنان سبز است.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:compileDebugUnitTestKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`ValidationAggregatorTest`) | ۱۳ تست (۹ + ۴ تازه)، موفق |
| `gradle :app:testDebugUnitTest` (`LogicConflictCheckerTest`) | ۷ تست، همه موفق، **بدون تغییر کد در آن فایل** |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۶۳ تست (۸۵۹ + ۴ تازه)، ۲ شکست نامرتبط (`AssetFormFlowTest`، `OutputDeliveryFlowTest` — هیچ‌کدام در این قدم لمس نشدند) — هر دو در اجرای مجزا (`--tests`) موفق؛ Flaky شناخته‌شده‌ی محیط Compose/Robolectric این Sandbox، هم‌الگوی مستندشده‌ی ADR-105 تا ADR-109 |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این زیرقدم (عمداً)

- سومین Rule (Slow Motion + دیالوگ، هنوز نوشته نشده) — زیرقدم ۳ب.
- هیچ تغییری در `LogicConflictChecker.kt` — فقط محل فراخوانی جدید در
  `ValidationAggregator.kt`.
- UI — قدم ۴.

## نتیجه

دو Rule از سه Rule تعارض دوربین/حرکت بلوپرینت ۰۳ (بخش «تداخل با واحدهای
دیگر») اکنون واقعاً در محل مرکزی Validation وایر شده‌اند — با استدلال
مستقل و مستند برای اینکه هرکدام در کدام سطح (۲ یا ۳) قرار گرفت. سومین Rule
در زیرقدم ۳ب جداگانه اضافه می‌شود.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
