# ADR-111: تکمیل Rule یتیم CinematicLanguage — قدم ۳ب از ۲ زیرقدم قدم ۳ (نوشتن و اتصال Slow Motion + Dialogue، آخرین زیرقدم قدم ۳)

## زمینه

ADR-110 (زیرقدم ۳الف) دو Rule موجود و آماده (`checkFastMotionLongTake`،
`checkStaticCameraInChase`) را وصل کرد. این زیرقدم (۳ب) سومین و **آخرین**
Rule بخش «تداخل با واحدهای دیگر» بلوپرینت
`docs/blueprints/03-visual-identity.md` را از صفر می‌نویسد و وصل می‌کند:

> «**Slow Motion + دیالوگ:** Slow Motion برای دیالوگ غیرمعمول است، مگر
> برای جلوه‌ی خاص عمدی.»

**با تکمیل این زیرقدم، قدم ۳ (کلاً دو زیرقدم: ۳الف و ۳ب) به‌طور کامل بسته
می‌شود** — هر سه Rule تعارض دوربین/حرکت بلوپرینت ۰۳ اکنون واقعاً وایرند.

## بررسی مستقل — تصمیم `MotionLevel` به‌جای `SubjectSpeed`

با `grep` مستقل روی `app/src/main` تأیید شد
`domain/camera/MotionIntensityModels.kt` یک `SubjectSpeed` دارد
(`SLOW, NORMAL, FAST, HYPERKINETIC`) که معنایی دقیق‌تر به «Slow Motion»
می‌دهد، اما `SubjectMotion`/`SubjectSpeed` در کل پروژه **کاملاً یتیم‌اند**
— نه `Shot` هیچ فیلدی از این نوع دارد، نه هیچ محل دیگری این نوع را
مصرف می‌کند (تنها مصرف‌کننده خودِ `MotionIntensityValidation.kt` است، که
خودش هم به هیچ‌جا وصل نیست — Rule جداگانه‌ی دیگری که در محدوده‌ی این ADR
نیست). استفاده از `SubjectSpeed` یعنی وصل‌کردن دو مدل یتیم همزمان در یک
Rule کوچک — پذیرفته نشد.

به‌جایش از `Shot.motionLevel: MotionLevel` استفاده شد — نوعی که از قبل
واقعاً به `Shot` وصل است و همین الان (ADR-110) در `checkFastMotionLongTake`
استفاده شده بود.

## تصمیم ۱ — کدام مقادیر `MotionLevel` معادل «Slow Motion»اند

```kotlin
enum class MotionLevel { STATIC, SUBTLE, MODERATE, DYNAMIC, EXTREME }
```

`checkFastMotionLongTake` (ADR-004/010) دو مقدار بالایی (`DYNAMIC`،
`EXTREME`) را «حرکت سریع» می‌داند. با تقارن مستقیم روی همین enum
پنج‌مقداره‌ی موجود، دو مقدار پایینی (`STATIC`، `SUBTLE`) معادل منطقی
«آهسته/Slow Motion» تشخیص داده شدند — با `MODERATE` به‌عنوان نقطه‌ی خنثای
وسط (نه سریع، نه آهسته؛ تست صریح این حالت هم اضافه شد). این یک الگوی
تازه‌اختراع‌شده نیست، بلکه بازاستفاده از همان منطق گروه‌بندی‌ای که خودِ
`checkFastMotionLongTake` از قبل دارد.

## تصمیم ۲ — تابع جدید در `LogicConflictChecker.kt`

```kotlin
fun checkSlowMotionInDialogue(motionLevel: MotionLevel, shotGoal: ShotGoal): ValidationIssue? {
    val isSlow = motionLevel == MotionLevel.STATIC || motionLevel == MotionLevel.SUBTLE
    if (isSlow && shotGoal == ShotGoal.DIALOGUE) {
        return ValidationIssue(
            Severity.WARNING,
            message = "Slow Motion برای دیالوگ غیرمعمول است",
            suggestion = "اگر این جلوه‌ی خاص عمدی نیست، Motion Level را افزایش دهید"
        )
    }
    return null
}
```

هم‌الگوی دقیق دو تابع موجود (امضای ساده، `Severity.WARNING` با
`message`/`suggestion`، `null` در نبود تعارض). پیام عمداً «غیرمعمول است»
می‌گوید، نه «اشتباه است» — مستقیماً منعکس‌کننده‌ی متن بلوپرینت («... مگر
برای جلوه‌ی خاص عمدی») که صراحتاً این ترکیب را یک خطای قطعی نمی‌داند،
بلکه یک الگوی غیرمعمول که می‌تواند کاملاً عمدی باشد (مثلاً یک نمای آهسته‌ی
درام برای تأکید بر یک خط دیالوگ کلیدی). `LogicConflictChecker.kt` هیچ
تغییر دیگری نگرفت — دو تابع موجود دست‌نخورده ماندند.

## تصمیم ۳ — سطح Validation: Level 2 (نه ۳)

برخلاف `checkFastMotionLongTake` (Level 3، چون به `effectiveCinematicMode`
چندمنبعی — DNA+Scene+Shot، از طریق `resolveEffectiveCinematicMode` —
وابسته است)، `checkSlowMotionInDialogue` فقط دو فیلد خودِ همین `Shot` را
با هم می‌سنجد (`motionLevel`، `shotGoal`) — بدون هیچ وابستگی به DNA یا
Scene. طبق همان معیار دقیقی که در ADR-110 برای تفکیک دو Rule قبلی استفاده
شد (Level 2 = مقایسه‌ی خالص فیلدهای همین Shot/Scene؛ Level 3 = مقایسه با
مقدار مشتق‌شده از چند منبع)، این Rule به **Level 2** تعلق دارد.

**بدون گیت `camera.overrideValue`:** برخلاف `checkStaticCameraInChase`
(که داخل بلوک `shot.camera.overrideValue?.let { ... }` است، چون به
تنظیمات دوربین Override-شده نیاز دارد)، `checkSlowMotionInDialogue` به
هیچ فیلد Override-پذیری نیاز ندارد — `motionLevel`/`shotGoal` هر دو
همیشه (غیر-nullable) روی هر `Shot` موجودند. پس به‌عنوان یک `add()`
مستقل، در ابتدای بخش Level 2 (قبل از بلوک `camera.overrideValue`) اضافه
شد.

## پیاده‌سازی در `ValidationAggregator.kt`

```kotlin
// --- Level 2 ---
val subjectCount = ...
add(l2, checkSlowMotionInDialogue(shot.motionLevel, shot.shotGoal))
shot.camera.overrideValue?.let { camera -> ... }
```

## تست

**`LogicConflictCheckerTest.kt` (۶ تست تازه):** `STATIC`+`DIALOGUE` →
Warning؛ `SUBTLE`+`DIALOGUE` → Warning؛ `STATIC`+`ACTION` (غیر-دیالوگ) →
بدون تعارض؛ `DYNAMIC`+`DIALOGUE` → بدون تعارض؛ `EXTREME`+`DIALOGUE` →
بدون تعارض؛ `MODERATE`+`DIALOGUE` → بدون تعارض (نقطه‌ی خنثای وسط، تست
اضافه‌ای فراتر از الزام صریح دستور کار، برای پوشش کامل هر پنج مقدار
`MotionLevel`).

**`ValidationAggregatorTest.kt` (۲ تست تازه):** یک Shot واقعی با
`motionLevel=STATIC`+`shotGoal=DIALOGUE` → `aggregateShotValidation`
واقعاً این Warning را در Level 2 برمی‌گرداند؛ همان سناریو با
`motionLevel=DYNAMIC` → بدون این هشدار.

**رگرسیون:** هر ۷ تست قبلی `LogicConflictCheckerTest.kt` (دو Rule ADR-110)
و هر ۱۳ تست قبلی `ValidationAggregatorTest.kt` بدون هیچ تغییر کدی سبز
ماندند — `neutralShot()` پیش‌فرض (`motionLevel=SUBTLE`،
`shotGoal=ESTABLISHING`) با این Rule تازه تداخل ندارد (چون `ESTABLISHING
!= DIALOGUE`)، پس تست «صفر مشکل» همچنان سبز است.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:compileDebugUnitTestKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`LogicConflictCheckerTest`) | ۱۳ تست (۷ + ۶ تازه)، موفق |
| `gradle :app:testDebugUnitTest` (`ValidationAggregatorTest`) | ۱۵ تست (۱۳ + ۲ تازه)، موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۷۱ تست (۸۶۳ + ۸ تازه)، ۲ شکست نامرتبط (`AssetFormFlowTest`، `OutputDeliveryFlowTest` — هیچ‌کدام در این قدم لمس نشدند) — هر دو در اجرای مجزا (`--tests`) موفق؛ Flaky شناخته‌شده‌ی محیط Compose/Robolectric این Sandbox، هم‌الگوی مستندشده‌ی ADR-105 تا ADR-110 |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این زیرقدم (عمداً)

- هیچ تغییری در `MotionIntensityModels.kt`/`MotionIntensityValidation.kt`
  — همچنان یتیم و خارج از Scope این ADR.
- UI — قدم ۴ (هنوز نرسیده).

## نتیجه — قدم ۳ به‌طور کامل بسته شد

با این زیرقدم، هر سه Rule تعارض دوربین/حرکت بلوپرینت ۰۳ («تداخل با
واحدهای دیگر») اکنون واقعاً در محل مرکزی `aggregateShotValidation` وایرند:
`checkFastMotionLongTake` (Level 3، ADR-110)، `checkStaticCameraInChase`
(Level 2، ADR-110)، و `checkSlowMotionInDialogue` (Level 2، همین ADR).
باقی‌مانده‌ی کل فیچر «تکمیل Rule یتیم CinematicLanguage» فقط **قدم ۴** است
— UI (فرم DNA/Scene/Shot).

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
