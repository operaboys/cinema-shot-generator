package com.operaboys.cinemashotgenerator.domain.visualidentity

import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.dna.MoodCategory
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.shot.Shot
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

/**
 * تکمیل Rule یتیم — قدم ۱ از ۴ (ADR-106): زنجیره‌ی کامل سه‌سطحی بلوپرینت ۰۳
 * بخش ب — اول Override شات (`shot.cinematicModeOverride`، بالاترین اولویت؛
 * طبق تصریح بلوپرینت «allow_shot_override همیشه true است»)، بعد Override محلی
 * صحنه (`scene.cinematicModeOverride`، فیلد مستقیم روی خودِ Scene — هم‌الگو با
 * negativePromptOverride شات)، در نهایت [resolveEffectiveMode] موجود (که خودش
 * ابتدا `projectDna.cinematicLanguage.sceneOverrides` — مکانیزم مستقل و مکمل
 * بلوپرینت برای Override متمرکز چند صحنه از یک محل — و در نهایت globalMode را
 * بررسی می‌کند).
 *
 * تصمیم طراحی (چرا دو مسیر Override صحنه، نه فقط یکی): `scene.cinematicModeOverride`
 * یک فیلد مستقیم و محلی روی خودِ Scene است (مشابه locationAssetId/
 * negativePromptOverride) — برای ویرایش تک‌صحنه‌ای. `sceneOverrides` روی
 * ProjectDna دقیقاً همان ساختار Map مفهومی خودِ بلوپرینت (`scene_overrides`
 * در JSON) است — برای مدیریت متمرکز چند Override از یک محل واحد (مثلاً یک
 * صفحه‌ی تنظیمات آینده). این تابع هر دو را بدون تناقض پشتیبانی می‌کند: فیلد
 * مستقیم صحنه اولویت دارد؛ اگر خالی بود، به مکانیزم متمرکز پروژه برمی‌گردد.
 */
fun resolveEffectiveCinematicMode(projectDna: ProjectDna, scene: Scene, shot: Shot): CinematicMode {
    shot.cinematicModeOverride?.let { return it }
    scene.cinematicModeOverride?.let { return it }
    return resolveEffectiveMode(projectDna.cinematicLanguage, scene.sceneId)
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

/**
 * نگاشت احساس اصلی Scene (از DNA.global_mood_base) به یک پیش‌فرض ریتم.
 *
 * MIGRATED (docs/adr/039-unit03-unit08-mood-type-safety-migration.md، رفع F4 ممیزی
 * pre-Unit 16): امضا از emotion: String به emotion: Mood (domain.dna، ۲۵ مقدار
 * واقعی) تغییر کرد. نسخه‌ی قبلی رشته‌ای تعداد اندکی مقدار دستی را با grep بررسی
 * می‌کرد که با enum واقعی Mood هم‌راستا نبودند (مثلاً "terrified"/"furious"/
 * "melancholy" اصلاً در Mood وجود ندارند)؛ به‌جای تلاش برای حدس‌زدن نگاشت رشته‌به‌رشته،
 * این تابع اکنون Total روی `MoodCategory` (۵ دسته‌ی سراسری Mood) کار می‌کند —
 * هر Mood معتبر همیشه یک CinematicMode می‌گیرد، بدون نیاز به فهرست دستی رشته‌ها.
 */
fun getPacingFromEmotion(emotion: Mood): CinematicMode = when (emotion.category) {
    MoodCategory.HIGH_ENERGY, MoodCategory.DARK -> CinematicMode.FAST_CUT
    MoodCategory.EMOTIONAL, MoodCategory.CALM -> CinematicMode.LONG_TAKE
    MoodCategory.POSITIVE -> CinematicMode.BALANCED
}

/**
 * Rule 1 (Blocking): حالت انتخابی باید یکی از سه مقدار معتبر باشد.
 * برای ورودی String خام (مثلاً از JSON یا از منابع دیگری که هنوز رشته‌ی خام دارند).
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
