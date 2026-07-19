package com.operaboys.cinemashotgenerator.domain.validation

import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
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
