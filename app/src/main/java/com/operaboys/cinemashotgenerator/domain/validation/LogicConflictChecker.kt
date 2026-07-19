package com.operaboys.cinemashotgenerator.domain.validation

// واحد ۰۷ — بخش ب: Logic Conflict Checker
// منبع حقیقت: docs/blueprints/07-validation-and-consistency.md
//
// motionLevel/cinematicMode در این فایل به‌صورت String پذیرفته می‌شوند، نه enum:
// MotionLevel دقیقاً در بلوپرینت ۰۵ (Shot Engine) و CinematicMode دقیقاً در بلوپرینت ۰۳
// (Visual Identity، بخش Cinematic Language) با همین نام و مقادیر از پیش تعریف شده‌اند،
// اما هیچ‌کدام از آن واحدها هنوز پیاده نشده. تعریف یک enum موقت اینجا دو نسخه‌ی مستقل
// ایجاد می‌کرد که واحدهای ۰۳/۰۵ باید بعداً با آن‌ها یکی می‌شدند (ریسک دوباره‌کاری). این
// رویکرد (String) تأییدشده توسط کاربر است. مقادیر منتظره:
// motionLevel: "static" | "subtle" | "moderate" | "dynamic" | "extreme"
// cinematicMode: "long_take" | "fast_cut" | "balanced"
// (ثبت‌شده در docs/adr/004-unit07-validation-consistency-deviations.md)

/** حرکت سریع در حالت Long-take (که ذاتاً آرام است) ناسازگار است. */
fun checkFastMotionLongTake(motionLevel: String, cinematicMode: String): ValidationIssue? {
    val isFast = motionLevel in listOf("dynamic", "extreme")
    if (isFast && cinematicMode == "long_take") {
        return ValidationIssue(
            Severity.WARNING,
            message = "حرکت سریع با حالت Long-take ناسازگار است",
            suggestion = "Cinematic Language را به Fast-cut تغییر دهید یا Motion Level را کاهش دهید"
        )
    }
    return null
}

/** دوربین ثابت در یک صحنه‌ی تعقیب، انرژی لازم را ندارد. */
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
