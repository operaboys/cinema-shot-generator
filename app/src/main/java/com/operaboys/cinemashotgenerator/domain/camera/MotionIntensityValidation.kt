package com.operaboys.cinemashotgenerator.domain.camera

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۰۹ — قوانین اعتبارسنجی Motion Intensity (بخش ب)
// منبع حقیقت: docs/blueprints/09-camera-and-motion.md
// دقیقاً طبق کد مفهومی بلوپرینت.

/** بررسی سازگاری سرعت و شدت — تناقض‌ها همیشه Warning هستند. */
fun validateSpeedIntensity(motion: SubjectMotion): ValidationIssue? {
    return when {
        motion.speed == SubjectSpeed.SLOW && motion.intensity > 6 ->
            ValidationIssue(Severity.WARNING, message = "حرکت آرام با Intensity بالا تناقض دارد")
        motion.speed == SubjectSpeed.HYPERKINETIC && motion.intensity < 7 ->
            ValidationIssue(Severity.WARNING, message = "حرکت Hyperkinetic باید Intensity بالا داشته باشد (حداقل ۷)")
        else -> null
    }
}

fun validateMotionBlur(speed: SubjectSpeed, blur: MotionBlurAmount): ValidationIssue? {
    return when {
        speed == SubjectSpeed.SLOW && blur == MotionBlurAmount.EXTREME ->
            ValidationIssue(Severity.WARNING, message = "Blur شدید برای حرکت آرام غیرطبیعی است")
        speed == SubjectSpeed.HYPERKINETIC && blur == MotionBlurAmount.NONE ->
            ValidationIssue(Severity.WARNING, message = "حرکت بسیار سریع بدون Blur غیرواقعی است")
        else -> null
    }
}
