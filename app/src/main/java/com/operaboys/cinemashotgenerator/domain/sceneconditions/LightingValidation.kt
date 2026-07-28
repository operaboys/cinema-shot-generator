package com.operaboys.cinemashotgenerator.domain.sceneconditions

import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۰۸ — قوانین اعتبارسنجی Lighting System (بخش الف، جدول کامل — ۵ Rule)
// منبع حقیقت: docs/blueprints/08-scene-conditions.md
//
// Rule 1/2 از TimeOfDay واقعی واحد ۰۴ استفاده می‌کنند (import، نه بازتعریف) —
// مشابه الگوی CharacterAsset در واحد ۰۵ و CameraDistance در واحد ۰۹.
//
// MIGRATED (docs/adr/027-unit02-dna-manager-v5-migration.md): LightingStyle اکنون
// از domain.dna می‌آید (۲۲ مقدار، نه ۶ مقدار قبلی). NOIR/DRAMATIC/NATURAL نام دقیقاً
// یکسان در enum جدید نداشتند — طبق تصمیم صریح معمار: NOIR→LOW_KEY (نزدیک‌ترین
// مفهومی، دسته‌ی STUDIO)، DRAMATIC→DRAMATIC_LIGHT، NATURAL→NATURAL_LIGHT. منطق Rule
// (چه‌وقت هشدار بدهد) عیناً دست‌نخورده ماند — فقط نام enum عوض شد.

/** Rule: نور خورشید (sunlight) + شب (night) → ناسازگاری فیزیکی مطلق (Blocking). */
fun checkSunlightAtNight(lightingMotivation: LightingMotivation, timeOfDay: TimeOfDay): ValidationIssue? {
    if (lightingMotivation == LightingMotivation.SUNLIGHT && timeOfDay == TimeOfDay.NIGHT) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "نور خورشید در شب فیزیکاً غیرممکن است"
        )
    }
    return null
}

/** Rule: نور ماه (moonlight) + ظهر (noon) → ناسازگاری فیزیکی مطلق (Blocking). */
fun checkMoonlightAtNoon(lightingMotivation: LightingMotivation, timeOfDay: TimeOfDay): ValidationIssue? {
    if (lightingMotivation == LightingMotivation.MOONLIGHT && timeOfDay == TimeOfDay.NOON) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "نور ماه در ظهر فیزیکاً غیرممکن است"
        )
    }
    return null
}

/** Rule: سبک Noir (LOW_KEY) + Contrast غیر از High → ناسازگاری سبکی (Warning). */
fun checkNoirWithoutHighContrast(style: LightingStyle, contrastRatio: ContrastRatio): ValidationIssue? {
    if (style == LightingStyle.LOW_KEY && contrastRatio != ContrastRatio.HIGH) {
        return ValidationIssue(
            Severity.WARNING,
            message = "سبک Noir معمولاً به Contrast بالا نیاز دارد"
        )
    }
    return null
}

/** Rule: Fill Light قوی + سبک Dramatic/Noir (DRAMATIC_LIGHT/LOW_KEY) → تضعیف کنتراست دراماتیک مدنظر (Warning). */
fun checkStrongFillWithDramaticStyle(fillLight: FillLight, style: LightingStyle): ValidationIssue? {
    val isDramaticStyle = style == LightingStyle.DRAMATIC_LIGHT || style == LightingStyle.LOW_KEY
    if (fillLight == FillLight.STRONG && isDramaticStyle) {
        return ValidationIssue(
            Severity.WARNING,
            message = "Fill Light قوی کنتراست دراماتیک سبک $style را تضعیف می‌کند"
        )
    }
    return null
}

/** Rule: نور از پایین (bottom) → غیرطبیعی، فقط برای هورور مناسب (Warning). */
fun checkBottomKeyLight(keyLightPosition: KeyLightPosition): ValidationIssue? {
    if (keyLightPosition == KeyLightPosition.BOTTOM) {
        return ValidationIssue(
            Severity.WARNING,
            message = "نور از پایین (Bottom) غیرطبیعی به نظر می‌رسد؛ فقط برای جلوه‌ی هورور مناسب است"
        )
    }
    return null
}
