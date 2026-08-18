package com.operaboys.cinemashotgenerator.domain.visualidentity

import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyleCategory

// واحد ۰۳ — بخش الف: Style Matrix
// منبع حقیقت: docs/blueprints/03-visual-identity.md

enum class StyleInfluence { SUBTLE, MODERATE, STRONG }

fun getInfluenceModifier(influence: StyleInfluence): String = when (influence) {
    StyleInfluence.SUBTLE -> "with subtle hints of"
    StyleInfluence.MODERATE -> "with elements of"
    StyleInfluence.STRONG -> "strongly influenced by"
}

data class StyleReference(val styleId: String, val name: String, val promptTokens: String)

fun combineStyles(primary: StyleReference, secondary: StyleReference?, influence: StyleInfluence?): String {
    var prompt = primary.promptTokens
    if (secondary != null && influence != null) {
        prompt += ", ${getInfluenceModifier(influence)} ${secondary.promptTokens}"
    }
    return prompt
}

enum class CompatibilityLevel { HIGH, MEDIUM, LOW, INCOMPATIBLE }

data class CompatibilityResult(val level: CompatibilityLevel, val warning: Boolean)

// اتصال کامل Style Matrix — قدم ۲الف از ۹ زیرقدم (ADR-114): ماتریس واقعی
// سازگاری سبک‌ها، جایگزین رفتار حداقلی قدیمی (همیشه MEDIUM/false). منبع
// محتوایی این ماتریس تحقیق مستقل معمار پروژه در منابع صنعت انیمیشن/هنر
// بصری است (نه حدس این پیاده‌سازی) — فهرست کامل قوانین Category/Override
// در ADR-114 مستند شده؛ اینجا فقط پیاده‌سازی است.

/**
 * سطح پایه‌ی سازگاری بین دو دسته‌ی VisualStyle — ۵×۵، متقارن. هر دو ترتیب
 * هر جفت با put زیر ثبت می‌شوند تا نیازی به نرمال‌سازی ترتیب در محل مصرف
 * نباشد. این نگاشت باید هر ۲۵ ترکیب مرتب‌شده‌ی ۵ دسته را کامل بپوشاند —
 * checkStyleCompatibility با checkNotNull این کامل‌بودن را در Runtime
 * تضمین می‌کند (نه یک Fallback خاموش که می‌تواند سطر جاافتاده را پنهان کند).
 */
private val categoryCompatibility: Map<Pair<VisualStyleCategory, VisualStyleCategory>, CompatibilityLevel> = buildMap {
    fun put(a: VisualStyleCategory, b: VisualStyleCategory, level: CompatibilityLevel) {
        this[a to b] = level
        this[b to a] = level
    }
    put(VisualStyleCategory.CINEMATIC, VisualStyleCategory.CINEMATIC, CompatibilityLevel.HIGH)
    put(VisualStyleCategory.CINEMATIC, VisualStyleCategory.ANIMATION_3D, CompatibilityLevel.MEDIUM)
    put(VisualStyleCategory.CINEMATIC, VisualStyleCategory.ANIMATION_2D, CompatibilityLevel.LOW)
    put(VisualStyleCategory.CINEMATIC, VisualStyleCategory.ARTISTIC, CompatibilityLevel.MEDIUM)
    put(VisualStyleCategory.CINEMATIC, VisualStyleCategory.GENRE, CompatibilityLevel.MEDIUM)
    put(VisualStyleCategory.ANIMATION_3D, VisualStyleCategory.ANIMATION_3D, CompatibilityLevel.HIGH)
    put(VisualStyleCategory.ANIMATION_3D, VisualStyleCategory.ANIMATION_2D, CompatibilityLevel.HIGH)
    put(VisualStyleCategory.ANIMATION_3D, VisualStyleCategory.ARTISTIC, CompatibilityLevel.LOW)
    put(VisualStyleCategory.ANIMATION_3D, VisualStyleCategory.GENRE, CompatibilityLevel.MEDIUM)
    put(VisualStyleCategory.ANIMATION_2D, VisualStyleCategory.ANIMATION_2D, CompatibilityLevel.HIGH)
    put(VisualStyleCategory.ANIMATION_2D, VisualStyleCategory.ARTISTIC, CompatibilityLevel.HIGH)
    put(VisualStyleCategory.ANIMATION_2D, VisualStyleCategory.GENRE, CompatibilityLevel.MEDIUM)
    put(VisualStyleCategory.ARTISTIC, VisualStyleCategory.ARTISTIC, CompatibilityLevel.HIGH)
    put(VisualStyleCategory.ARTISTIC, VisualStyleCategory.GENRE, CompatibilityLevel.LOW)
    put(VisualStyleCategory.GENRE, VisualStyleCategory.GENRE, CompatibilityLevel.HIGH)
}

/**
 * Override های دقیق سطح VisualStyle — روی قانون Category بالا اولویت دارند.
 * هر دو ترتیب هر جفت با put زیر ثبت می‌شوند، دقیقاً هم‌الگوی categoryCompatibility.
 */
private val explicitStyleOverrides: Map<Pair<VisualStyle, VisualStyle>, CompatibilityLevel> = buildMap {
    fun put(a: VisualStyle, b: VisualStyle, level: CompatibilityLevel) {
        this[a to b] = level
        this[b to a] = level
    }
    // ۱. Ghibli + Watercolor
    put(VisualStyle.STUDIO_GHIBLI, VisualStyle.WATERCOLOR, CompatibilityLevel.HIGH)
    // ۲. Oil Painting + Photorealistic
    put(VisualStyle.OIL_PAINTING, VisualStyle.PHOTOREALISTIC, CompatibilityLevel.LOW)
    // ۳. استودیوهای انیمیشن ۳D بزرگ + سبک‌های انیمیشن ۲D رایج
    val majorStudios3d = listOf(VisualStyle.PIXAR_DISNEY, VisualStyle.DREAMWORKS, VisualStyle.ILLUMINATION)
    val popular2d = listOf(VisualStyle.ANIME, VisualStyle.STUDIO_GHIBLI, VisualStyle.COMIC_BOOK)
    for (studio in majorStudios3d) for (style2d in popular2d) put(studio, style2d, CompatibilityLevel.HIGH)
    // ۴. Cyberpunk + سبک‌های هنری دستی
    val handmadeArtistic = listOf(VisualStyle.WATERCOLOR, VisualStyle.OIL_PAINTING, VisualStyle.PENCIL_SKETCH)
    for (style in handmadeArtistic) put(VisualStyle.CYBERPUNK, style, CompatibilityLevel.LOW)
    // ۵. Photorealistic + سبک‌های کارتونی/انیمیشنی غیرواقع‌گرا
    val nonRealisticAnimated = listOf(VisualStyle.CARTOON, VisualStyle.COMIC_BOOK, VisualStyle.CLAYMATION)
    for (style in nonRealisticAnimated) put(VisualStyle.PHOTOREALISTIC, style, CompatibilityLevel.LOW)
}

/**
 * قاعده‌ی warning: فقط LOW/INCOMPATIBLE هشدار می‌دهند؛ HIGH/MEDIUM هرگز.
 * این قدم هیچ جفتی را INCOMPATIBLE نمی‌گذارد (آن سطح برای Custom Style های
 * آینده نگه داشته می‌شود) — اما منطق warning عمومی و پیشگیرانه نوشته شده،
 * نه فقط برای LOW.
 */
fun checkStyleCompatibility(primary: VisualStyle, secondary: VisualStyle): CompatibilityResult {
    val level = when {
        primary == secondary -> CompatibilityLevel.HIGH
        else -> explicitStyleOverrides[primary to secondary]
            ?: checkNotNull(categoryCompatibility[primary.category to secondary.category]) {
                "categoryCompatibility ناقص است — ترکیب ${primary.category} × ${secondary.category} پوشش داده نشده"
            }
    }
    val warning = level == CompatibilityLevel.LOW || level == CompatibilityLevel.INCOMPATIBLE
    return CompatibilityResult(level = level, warning = warning)
}

data class CustomStyle(
    val customStyleId: String,
    val name: String,
    val baseStyleId: String,
    val modifications: Map<String, String> = emptyMap()
)

/**
 * حالت فعلی Style Matrix یک پروژه. primaryStyle غیر-nullable است — Rule 1
 * («سبک اصلی همیشه باید تعیین‌شده باشد») در سطح Type System تضمین می‌شود؛
 * ساخت یک StyleMatrix بدون سبک اصلی اصلاً کامپایل نمی‌شود.
 *
 * اتصال کامل Style Matrix — قدم ۲الف از ۹ زیرقدم (ADR-114): primaryStyle/
 * secondaryStyle از StyleReference به VisualStyle مستقیم تغییر کردند — قدم
 * ۱ (ADR-113) همین مفهوم (سبک اصلی/ثانویه) را از StyleReference جدا به
 * CoreIdentity منتقل کرد؛ ادامه‌ی استفاده از StyleReference اینجا منسوخ بود.
 * خودِ StyleReference/combineStyles/getInfluenceModifier دست‌نخورده ماندند —
 * برای promptTokens در قدم‌های آینده هنوز لازم‌اند.
 */
data class StyleMatrix(
    val styleMatrixId: String,
    val primaryStyle: VisualStyle,
    val secondaryStyle: VisualStyle? = null,
    val influence: StyleInfluence? = null,
    val customStyles: List<CustomStyle> = emptyList()
)

sealed class StyleUpdateResult {
    object Allowed : StyleUpdateResult()
    data class Blocked(val reason: String) : StyleUpdateResult()
}

/**
 * Rule 3 (Blocking روی تلاش حذف): سبک اصلی قابل حذف نیست، فقط قابل تعویض.
 * newPrimaryStyle مقدار پیشنهادی برای به‌روزرسانی است؛ null یعنی تلاش برای حذف.
 *
 * اتصال کامل Style Matrix — قدم ۳ از ۸ زیرقدم (ADR-115): امضا از
 * `StyleReference?` به `VisualStyle?` تغییر کرد — یافته‌ی گزارش‌شده در ADR-114:
 * از قدم ۲ (که `StyleMatrix.primaryStyle` را به `VisualStyle` تغییر داد) به
 * بعد، این تابع (که منطقاً برای اعتبارسنجی به‌روزرسانی همان فیلد نوشته شده
 * بود) با نوع واقعی‌اش ناسازگار بود. منطق داخلی (null=Blocked) تغییری نکرد.
 */
fun validatePrimaryStyleUpdate(newPrimaryStyle: VisualStyle?): StyleUpdateResult {
    if (newPrimaryStyle == null) {
        return StyleUpdateResult.Blocked("سبک اصلی قابل حذف نیست؛ فقط می‌توانید آن را با سبک دیگری تعویض کنید")
    }
    return StyleUpdateResult.Allowed
}

/**
 * Rule 2 (Warning، هرگز Blocking): ناسازگاری سبک اصلی/ثانویه، از checkStyleCompatibility.
 * وقتی سبک ثانویه‌ای انتخاب نشده باشد، چیزی برای بررسی سازگاری وجود ندارد (null).
 */
fun checkStyleMatrixCompatibility(matrix: StyleMatrix): CompatibilityResult? {
    val secondary = matrix.secondaryStyle ?: return null
    return checkStyleCompatibility(matrix.primaryStyle, secondary)
}
