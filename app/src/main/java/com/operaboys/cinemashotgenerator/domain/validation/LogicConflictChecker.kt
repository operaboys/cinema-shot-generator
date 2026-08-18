package com.operaboys.cinemashotgenerator.domain.validation

import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode

// واحد ۰۷ — بخش ب: Logic Conflict Checker
// منبع حقیقت: docs/blueprints/07-validation-and-consistency.md
//
// MIGRATED (docs/adr/010-cross-unit-migrations.md، Migration ۲): checkFastMotionLongTake
// قبلاً motionLevel/cinematicMode را به‌صورت String موقت می‌گرفت (ADR-004) چون واحدهای
// ۰۵/۰۳ هنوز پیاده نشده بودند. حالا هر دو واحد پیاده شده‌اند؛ این تابع از نوع واقعی
// MotionLevel (واحد ۰۵) و CinematicMode (واحد ۰۳) استفاده می‌کند.

/** حرکت سریع در حالت Long-take (که ذاتاً آرام است) ناسازگار است. */
fun checkFastMotionLongTake(motionLevel: MotionLevel, cinematicMode: CinematicMode): ValidationIssue? {
    val isFast = motionLevel == MotionLevel.DYNAMIC || motionLevel == MotionLevel.EXTREME
    if (isFast && cinematicMode == CinematicMode.LONG_TAKE) {
        return ValidationIssue(
            Severity.WARNING,
            message = "حرکت سریع با حالت Long-take ناسازگار است",
            suggestion = "Cinematic Language را به Fast-cut تغییر دهید یا Motion Level را کاهش دهید"
        )
    }
    return null
}

/**
 * دوربین ثابت در یک صحنه‌ی تعقیب، انرژی لازم را ندارد.
 *
 * cameraMovementType عمداً String باقی ماند (بررسی‌شده در Migration ۲، ADR-010):
 * معادل واقعی‌اش در واحد ۰۹ یا BasicMovementType (فقط حرکات پایه را پوشش می‌دهد،
 * نه Orbit/DronePath/... پیشرفته) یا خودِ CameraMovement (sealed class، نیازمند
 * Pattern Matching اضافه در این تابع) است — هیچ‌کدام تناظر تمیز و مستقیمی مثل
 * MotionLevel/CinematicMode ندارند. shotDescription هم متن آزاد است، نه enum؛
 * اجباری‌کردن فقط یک پارامتر به enum و نگه‌داشتن دیگری String پیچیدگی بدون سود
 * اضافه می‌کرد.
 */
fun checkStaticCameraInChase(cameraMovementType: String, shotDescription: String): ValidationIssue? {
    val isChase = listOf("chase", "running", "pursuit").any { shotDescription.contains(it, ignoreCase = true) }
    if (cameraMovementType == "static" && isChase) {
        return ValidationIssue(
            Severity.WARNING,
            message = "صحنه‌ی تعقیب با دوربین ثابت انرژی لازم را ندارد",
            suggestion = "از Tracking Shot یا Handheld استفاده کنید"
        )
    }
    return null
}

/**
 * تکمیل Rule یتیم — قدم ۳ب از ۲ زیرقدم قدم ۳ (ADR-111، آخرین زیرقدم):
 * سومین Rule تداخل بلوپرینت ۰۳ («Slow Motion + دیالوگ»).
 *
 * چرا `MotionLevel` به‌جای `domain.camera.SubjectSpeed` (تصمیم صریح معمار،
 * تأییدشده): `SubjectSpeed`/`SubjectMotion` (domain.camera.MotionIntensityModels.kt)
 * معنای دقیق‌تری به «Slow Motion» می‌دهند، اما با grep مستقل تأیید شد کاملاً
 * یتیم‌اند — هیچ فیلدی از این نوع روی `Shot` وجود ندارد و در کل پروژه هیچ
 * مصرف‌کننده‌ی واقعی دیگری ندارند (فقط خودِ `MotionIntensityValidation.kt`).
 * استفاده از آن‌ها یعنی وصل‌کردن دو مدل یتیم همزمان در یک Rule کوچک —
 * `MotionLevel` (که از قبل واقعاً به `Shot.motionLevel` وصل است و همین الان
 * در `checkFastMotionLongTake` بالا استفاده شده) گزینه‌ی واقع‌گرایانه‌تر بود.
 *
 * چرا `STATIC`/`SUBTLE` (نه فقط `STATIC`): طبق ترتیب پنج‌مقداره‌ی موجود
 * `MotionLevel` (`STATIC, SUBTLE, MODERATE, DYNAMIC, EXTREME`)، `checkFastMotionLongTake`
 * بالا دو مقدار بالایی (`DYNAMIC`/`EXTREME`) را «سریع» می‌داند — به‌طور
 * متقارن، دو مقدار پایینی (`STATIC`/`SUBTLE`) معادل منطقی «آهسته/Slow
 * Motion» هستند، با `MODERATE` به‌عنوان نقطه‌ی خنثای وسط (نه سریع، نه
 * آهسته) — همان الگوی گروه‌بندی، نه یک تصمیم تازه و بی‌ربط.
 *
 * پیام Warning عمداً «غیرمعمول است» می‌گوید (نه «اشتباه است») — دقیقاً
 * منعکس‌کننده‌ی متن بلوپرینت («... مگر برای جلوه‌ی خاص عمدی»)، چون این
 * ترکیب می‌تواند کاملاً عمدی باشد (مثلاً یک نمای آهسته‌ی درام برای تأکید بر
 * یک خط دیالوگ کلیدی).
 */
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
