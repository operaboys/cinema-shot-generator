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

// اتصال کامل Style Matrix — قدم ۴-محتوا از ۸ زیرقدم (ADR-116): promptTokens
// غنی و واقعی برای هر ۳۴ مقدار VisualStyle — تا این قدم، هیچ StyleReference
// واقعی برای هیچ سبکی در کل کدبیس ساخته نمی‌شد (combineStyles از قدم اول
// وجود داشت، اما ورودی واقعی نداشت). منبع محتوایی این متن‌ها تحقیق مستقل
// معمار پروژه در منابع صنعت prompt engineering (راهنماهای مدل‌های تولید
// تصویر/ویدیوی این پروژه طبق docs/blueprints/14-output-delivery-v2.md) و
// منابع هنر بصری است — نه حدس این پیاده‌سازی؛ فهرست کامل و مرجع رسمی هر
// ۳۴ مقدار در ADR-116 مستند شده.
//
// دو تصمیم فنی من (نه محتوای خودِ متن‌ها، که عیناً کپی شدند):
// (۱) `when` بدون `else` — نه یک Map: تضمین کامل‌بودن در سطح کامپایلر
// (نه فقط Runtime مثل `checkNotNull` قدم ۲) — اگر VisualStyle در آینده
// مقدار تازه‌ای بگیرد، این تابع دیگر کامپایل نمی‌شود تا یک سبک بدون
// promptTokens بی‌صدا نماند.
// (۲) `name` (فقط تزئینی/مرجع، در `combineStyles` مصرف نمی‌شود — فقط
// `promptTokens` مصرف می‌شود) از خودِ نام enum مشتق می‌شود (نه ۳۴ رشته‌ی
// دستی جدا) — چون هیچ منطقی واقعاً به آن وابسته نیست، نوشتن ۳۴ رشته‌ی
// دستی فقط ریسک تایپی بدون سود عملکردی اضافه می‌کرد؛ عمداً از
// `visualStyleLabel` (لایه‌ی UI/i18n) هم استفاده نشد — همان انضباط
// جداسازی «domain ← UI ممنوع» که در ADR-115 هم رعایت شد.
private fun VisualStyle.readableName(): String =
    name.split("_").joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercaseChar) }

/**
 * StyleReference واقعی و غنی برای این VisualStyle — `promptTokens` مستقیماً
 * وارد پرامپت مدل‌های تولید تصویر/ویدیو می‌شود (طبق `combineStyles`)، پس
 * عبارت‌های کوتاه Comma-separated به‌سبک Keyword‌اند، نه جمله‌ی کامل.
 */
fun VisualStyle.toStyleReference(): StyleReference = StyleReference(
    styleId = name,
    name = readableName(),
    promptTokens = when (this) {
        // سینمایی
        VisualStyle.CINEMATIC_STYLE -> "cinematic style, professional color grading, shallow depth of field, dramatic composition, film-quality lighting"
        VisualStyle.PHOTOREALISTIC -> "photorealistic, ultra-realistic, lifelike detail, natural lighting, high-fidelity texture, DSLR quality"
        VisualStyle.FILM_NOIR -> "film noir cinematography, dramatic chiaroscuro lighting, deep shadows, venetian blind light patterns, high-contrast black and white aesthetic"
        VisualStyle.VINTAGE_RETRO -> "vintage retro aesthetic, shot on 35mm film with natural grain, period color grading, warm faded tones"
        VisualStyle.DOCUMENTARY -> "documentary-style handheld camera, natural lighting, authentic unpolished look, observational cinematography"
        VisualStyle.BLOCKBUSTER -> "epic blockbuster style, dramatic wide shots, dynamic camera movement, rich color grading, theatrical lighting, Hollywood production value"
        VisualStyle.INDIE_ARTHOUSE -> "indie arthouse aesthetic, naturalistic lighting, muted color palette, intimate handheld framing, contemplative pacing"
        // انیمیشن ۳D
        VisualStyle.PIXAR_DISNEY -> "Pixar-style 3D animation, soft rounded character design, oversized expressive eyes, warm bounce lighting, polished rendered surfaces"
        VisualStyle.DREAMWORKS -> "DreamWorks-style 3D animation, expressive stylized characters, dynamic dramatic lighting, vibrant color palette, polished CGI render"
        VisualStyle.ILLUMINATION -> "Illumination-style 3D animation, bright saturated colors, exaggerated bouncy character design, playful comedic framing, glossy render"
        VisualStyle.LOW_POLY -> "low poly 3D art, geometric faceted shapes, minimal polygon count, clean angular aesthetic, flat shaded surfaces"
        VisualStyle.CLAYMATION -> "claymation stop-motion style, handcrafted plasticine texture, visible fingerprints and imperfections, warm tactile lighting, miniature set feel"
        VisualStyle.ISOMETRIC -> "isometric 3D illustration, 45-degree angled perspective, no perspective distortion, clean architectural miniature-diorama look"
        // انیمیشن ۲D
        VisualStyle.ANIME -> "anime style, bold clean linework, cel shading, expressive stylized eyes, dynamic action framing"
        VisualStyle.STUDIO_GHIBLI -> "Studio Ghibli style, hand-painted watercolor backgrounds, soft natural color palette, whimsical detailed nature, gentle painterly light"
        VisualStyle.DISNEY_CLASSIC -> "classic Disney hand-drawn animation, warm expressive character design, soft painterly backgrounds, storybook charm"
        VisualStyle.CARTOON -> "cartoon style, bold outlines, flat saturated colors, exaggerated proportions, playful simplified shapes"
        VisualStyle.COMIC_BOOK -> "comic book style, bold ink outlines, Ben-Day dot shading, dynamic panel-style composition, saturated primary colors"
        VisualStyle.MANGA -> "manga style, high-contrast black and white linework, screentone shading, dramatic speed lines, expressive stylized eyes"
        // هنری
        VisualStyle.WATERCOLOR -> "watercolor painting, loose wet-on-wet technique, soft bleeding edges, visible paper texture, delicate translucent washes"
        VisualStyle.OIL_PAINTING -> "oil painting, visible impasto brushstrokes, rich textured canvas, deep saturated color, classical painterly light"
        VisualStyle.PENCIL_SKETCH -> "pencil sketch, expressive graphite linework, cross-hatching shading, visible paper grain, monochrome hand-drawn feel"
        VisualStyle.IMPRESSIONIST -> "impressionist painting, visible loose brushstrokes, dappled natural light, soft dreamy color, emphasis on light over sharp form"
        VisualStyle.POP_ART -> "pop art style, bold flat colors, Ben-Day dots, high contrast graphic outlines, Warhol-inspired repetition"
        VisualStyle.ART_NOUVEAU -> "Art Nouveau style, organic flowing lines, ornate nature-inspired motifs, decorative elegant composition"
        VisualStyle.MINIMALIST -> "minimalist art, vast negative space, clean geometric shapes, limited color palette, deliberate simplicity"
        // ژانر
        VisualStyle.EPIC_FANTASY -> "epic fantasy illustration, dramatic wide vista, rich detailed world-building, painterly grandeur, mythic atmosphere"
        VisualStyle.SCI_FI -> "sci-fi aesthetic, sleek futuristic technology, cool metallic tones, advanced clean design, high-tech atmosphere"
        VisualStyle.CYBERPUNK -> "cyberpunk aesthetic, neon-lit rain-soaked streets, high-contrast magenta and cyan lighting, dense futuristic urban decay"
        VisualStyle.STEAMPUNK -> "steampunk style, Victorian-era machinery, brass gears and cogs, warm sepia tones, industrial ornate detail"
        VisualStyle.GOTHIC -> "gothic aesthetic, dark ornate atmosphere, dramatic deep shadows, moody desaturated palette, medieval architectural grandeur"
        VisualStyle.HORROR -> "horror atmosphere, desaturated color, unsettling deep shadows, tense unnerving composition, cold harsh lighting"
        VisualStyle.SURREAL -> "surrealist style, dreamlike impossible scene, uncanny juxtaposition, meticulous hyper-detailed rendering, subconscious symbolism"
        VisualStyle.DREAMY -> "dreamy atmosphere, soft hazy glow, pastel ethereal color, gentle diffused light, floating weightless quality"
    }
)

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

