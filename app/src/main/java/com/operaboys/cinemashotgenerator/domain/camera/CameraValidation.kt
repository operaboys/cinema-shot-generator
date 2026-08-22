package com.operaboys.cinemashotgenerator.domain.camera

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۰۹ — قوانین اعتبارسنجی Camera System (بخش الف، جدول کامل — ۵ Rule)
// منبع حقیقت: docs/blueprints/09-camera-and-motion.md
//
// همه از ValidationIssue/Severity سراسری واحد ۰۷ استفاده می‌کنند — الگوی تثبیت‌شده.

/** Rule: لنز Ultra-wide + Close-up/Extreme Close-up → اعوجاج شدید چهره (Blocking). */
fun checkLensDistanceMismatch(lensType: LensType, distance: CameraDistance): ValidationIssue? {
    val isCloseRange = distance == CameraDistance.CLOSE_UP || distance == CameraDistance.EXTREME_CLOSE_UP
    if (lensType == LensType.ULTRA_WIDE && isCloseRange) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "لنز Ultra-wide با نمای Close-up/Extreme Close-up باعث اعوجاج شدید چهره می‌شود"
        )
    }
    return null
}

/** Rule: Static Movement + Handheld Stabilization → ناسازگاری منطقی (Warning). */
fun checkStaticMovementWithHandheldStabilization(
    movement: CameraMovement,
    stabilization: Stabilization
): ValidationIssue? {
    val isStatic = movement is CameraMovement.Basic && movement.type == BasicMovementType.STATIC
    if (isStatic && stabilization == Stabilization.HANDHELD) {
        return ValidationIssue(
            Severity.WARNING,
            message = "حرکت Static با تثبیت‌کننده‌ی Handheld ناسازگار است"
        )
    }
    return null
}

/** Rule: Rack Focus با کمتر از ۲ Subject → نیاز به حداقل دو سوژه (Blocking). */
fun checkRackFocusSubjectCount(focusMode: FocusMode, subjectCount: Int): ValidationIssue? {
    if (focusMode == FocusMode.RACK_FOCUS && subjectCount < 2) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "Rack Focus نیاز به حداقل دو سوژه دارد"
        )
    }
    return null
}

/**
 * Rule: Extreme Wide + Very Shallow DoF → فیزیکاً دشوار (Warning).
 * DepthOfField فقط سه مقدار دارد (SHALLOW/MEDIUM/DEEP)؛ «Very Shallow» در جدول
 * بلوپرینت به بالاترین درجه‌ی موجود (SHALLOW) نگاشت شد — یک ناهماهنگی واژگانی جزئی
 * مشابه موارد مشابه در واحدهای قبلی (مثل "hybrid"→BALANCED در واحد ۰۳)، نه ابهام معماری.
 */
fun checkExtremeWideWithShallowDepthOfField(distance: CameraDistance, depthOfField: DepthOfField): ValidationIssue? {
    if (distance == CameraDistance.EXTREME_WIDE && depthOfField == DepthOfField.SHALLOW) {
        return ValidationIssue(
            Severity.WARNING,
            message = "ترکیب Extreme Wide با عمق میدان بسیار کم (Shallow DoF) فیزیکاً دشوار است"
        )
    }
    return null
}

/**
 * Rule: مدت حرکت دوربین بیشتر از Duration شات → ناسازگاری زمانی (Blocking).
 *
 * هیچ‌کدام از شش variant سازنده‌ی CameraMovement (Basic/Orbit/DronePath/DollyZoom/
 * HandheldShake/Compound) فیلد duration ندارند؛ طبق تصمیم تأییدشده، مدت حرکت به‌عنوان
 * پارامتر خارجی مستقل گرفته می‌شود (مشابه الگوی «پارامتر مستقیم به‌جای فیلد ناموجود»
 * که در واحدهای ۰۳/۰۵/۰۷ هم استفاده شد)، نه افزودن فیلد به CameraMovement.
 *
 * اتصال واقعی — قدم ۱ از ۲ (ADR-129): این Rule دیگر یتیم نیست. مقدار واقعی
 * از CameraSettings.movementDurationSeconds (فیلد جدید، سطح CameraSettings
 * نه CameraMovement) می‌آید و در ValidationAggregator.kt وصل شده — دقیقاً همان
 * «آینده»ای که این کامنت (از ابتدا، ADR-008) به آن اشاره کرده بود.
 */
fun validateCameraMovementDuration(movementDurationSeconds: Float, shotDurationSeconds: Float): ValidationIssue? {
    if (movementDurationSeconds > shotDurationSeconds) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "مدت حرکت دوربین ($movementDurationSeconds ثانیه) از مدت شات ($shotDurationSeconds ثانیه) بیشتر است"
        )
    }
    return null
}
