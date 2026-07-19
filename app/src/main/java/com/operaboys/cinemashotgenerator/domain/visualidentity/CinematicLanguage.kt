package com.operaboys.cinemashotgenerator.domain.visualidentity

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۰۳ — بخش ب: Cinematic Language
// منبع حقیقت: docs/blueprints/03-visual-identity.md
//
// این واحد صاحب اصلی CinematicMode است. واحد ۰۷ (Logic Conflict Checker) قبلاً
// پارامتر cinematicMode را به‌صورت String موقت گرفته بود دقیقاً چون این enum هنوز
// جایی تعریف نشده بود (docs/adr/004-...) — حالا اینجا صاحب واقعی‌اش تعریف می‌شود.
// آیا واحد ۰۷ باید بعداً به این enum واقعی Migrate شود، سؤالی است که در
// docs/adr/005-unit03-visual-identity-deviations.md مطرح شده، نه اجراشده.
enum class CinematicMode { LONG_TAKE, FAST_CUT, BALANCED }

data class CinematicLanguageSettings(
    val globalMode: CinematicMode,
    val sceneOverrides: Map<String, CinematicMode> = emptyMap() // sceneId -> override mode
)

/**
 * تعیین حالت مؤثر یک Scene: اول Override محلی، بعد حالت سراسری.
 * کاربر همیشه از طریق Override می‌تواند این را دستی تغییر دهد.
 */
fun resolveEffectiveMode(
    settings: CinematicLanguageSettings,
    sceneId: String
): CinematicMode {
    return settings.sceneOverrides[sceneId] ?: settings.globalMode
}

/** بر اساس شدت میانگین Beat های یک Scene، حالت مناسب Hybrid را تعیین می‌کند. */
fun determineHybridPacing(sceneType: String, avgBeatIntensity: Float): CinematicMode {
    return when {
        sceneType == "action" || avgBeatIntensity >= 7f -> CinematicMode.FAST_CUT
        sceneType == "emotional" || avgBeatIntensity <= 3f -> CinematicMode.LONG_TAKE
        sceneType == "dialogue" -> CinematicMode.BALANCED
        else -> CinematicMode.BALANCED
    }
}

/** نگاشت احساس اصلی Scene (از DNA.global_mood_base) به یک پیش‌فرض ریتم. */
fun getPacingFromEmotion(emotion: String): CinematicMode = when (emotion) {
    "tense", "terrified", "furious" -> CinematicMode.FAST_CUT
    "melancholy", "serene", "contemplative" -> CinematicMode.LONG_TAKE
    else -> CinematicMode.BALANCED
}

/**
 * Rule 1 (Blocking): حالت انتخابی باید یکی از سه مقدار معتبر باشد.
 * برای ورودی String خام (مثلاً از JSON یا از واحد ۰۷ که فعلاً String می‌گیرد).
 * نگاشت "hybrid" → BALANCED طبق مقدار global_mode در ساختار JSON بلوپرینت است؛
 * enum مفهومی بلوپرینت این حالت را BALANCED نام‌گذاری کرده، نه HYBRID.
 */
fun parseCinematicMode(raw: String): Result<CinematicMode> {
    return when (raw) {
        "long_take" -> Result.success(CinematicMode.LONG_TAKE)
        "fast_cut" -> Result.success(CinematicMode.FAST_CUT)
        "hybrid" -> Result.success(CinematicMode.BALANCED)
        else -> Result.failure(IllegalArgumentException("حالت '$raw' یکی از مقادیر معتبر Cinematic Language نیست"))
    }
}

/**
 * Rule 2 (Warning): مدت شات خارج از محدوده‌ی مجاز حالت انتخابی (جدول بخش ب:
 * Long-take ۸-۶۰ ثانیه، Fast-cut ۱-۵ ثانیه، Hybrid/Balanced ۳-۲۰ ثانیه).
 *
 * از ValidationIssue/Severity سراسری واحد ۰۷ استفاده می‌کند — اولین وابستگی
 * واقعی بین دو پکیج domain در این پروژه (domain.visualidentity → domain.validation)،
 * تأییدشده توسط کاربر پیش از پیاده‌سازی. جزئیات در ADR-005.
 */
fun validateShotDurationForCinematicMode(durationSeconds: Float, mode: CinematicMode): ValidationIssue? {
    val allowedRange = when (mode) {
        CinematicMode.LONG_TAKE -> 8f..60f
        CinematicMode.FAST_CUT -> 1f..5f
        CinematicMode.BALANCED -> 3f..20f
    }
    if (durationSeconds !in allowedRange) {
        return ValidationIssue(
            Severity.WARNING,
            field = "duration_seconds",
            message = "مدت شات ($durationSeconds ثانیه) خارج از محدوده‌ی مجاز حالت $mode " +
                "(${allowedRange.start}-${allowedRange.endInclusive} ثانیه) است"
        )
    }
    return null
}
