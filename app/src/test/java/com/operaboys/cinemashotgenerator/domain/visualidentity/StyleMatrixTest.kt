package com.operaboys.cinemashotgenerator.domain.visualidentity

import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// اتصال کامل Style Matrix — قدم ۲الف از ۹ زیرقدم (ADR-114): بازنویسی کامل
// این فایل — ورودی‌های رشته‌ای دلبخواه checkStyleCompatibility قدیمی دیگر
// کامپایل نمی‌شوند. فهرست کامل قوانین (Category + Override) در ADR-114
// مستند شده؛ این تست‌ها فقط زیرمجموعه‌ای برای اثبات پیاده‌سازی صحیح هستند،
// نه تکرار کل ماتریس.

class StyleMatrixTest {

    private val ghibli = StyleReference(
        styleId = "style_ghibli",
        name = "Studio Ghibli",
        promptTokens = "hand-drawn animation, watercolor backgrounds, soft pastel colors, detailed natural scenery"
    )
    private val photorealistic = StyleReference(
        styleId = "style_realistic",
        name = "Photorealistic",
        promptTokens = "realistic lighting and textures"
    )

    // --- getInfluenceModifier (دست‌نخورده) ---

    @Test
    fun `getInfluenceModifier covers all three levels`() {
        assertEquals("with subtle hints of", getInfluenceModifier(StyleInfluence.SUBTLE))
        assertEquals("with elements of", getInfluenceModifier(StyleInfluence.MODERATE))
        assertEquals("strongly influenced by", getInfluenceModifier(StyleInfluence.STRONG))
    }

    // --- combineStyles (دست‌نخورده — همچنان روی StyleReference، برای promptTokens آینده) ---

    @Test
    fun `combineStyles with primary only`() {
        val result = combineStyles(ghibli, secondary = null, influence = null)
        assertEquals(ghibli.promptTokens, result)
    }

    @Test
    fun `combineStyles with secondary but no influence ignores secondary`() {
        val result = combineStyles(ghibli, secondary = photorealistic, influence = null)
        assertEquals(ghibli.promptTokens, result)
    }

    @Test
    fun `combineStyles with secondary and influence appends modifier`() {
        val result = combineStyles(ghibli, secondary = photorealistic, influence = StyleInfluence.SUBTLE)
        assertEquals(
            "${ghibli.promptTokens}, with subtle hints of ${photorealistic.promptTokens}",
            result
        )
    }

    // --- checkStyleCompatibility: قانون Category ---

    @Test
    fun `same-category pair (CINEMATIC x CINEMATIC) resolves to HIGH via the category matrix`() {
        val result = checkStyleCompatibility(VisualStyle.FILM_NOIR, VisualStyle.BLOCKBUSTER)
        assertEquals(CompatibilityLevel.HIGH, result.level)
        assertFalse(result.warning)
    }

    @Test
    fun `cross-category pair (CINEMATIC x ANIMATION_2D) resolves to LOW via the category matrix`() {
        val result = checkStyleCompatibility(VisualStyle.FILM_NOIR, VisualStyle.MANGA)
        assertEquals(CompatibilityLevel.LOW, result.level)
        assertTrue(result.warning)
    }

    // --- checkStyleCompatibility: قانون primary == secondary ---

    @Test
    fun `identical primary and secondary always resolve to HIGH regardless of category rules`() {
        val result = checkStyleCompatibility(VisualStyle.ANIME, VisualStyle.ANIME)
        assertEquals(CompatibilityLevel.HIGH, result.level)
        assertFalse(result.warning)
    }

    // --- checkStyleCompatibility: هر ۵ Override دقیق ---

    @Test
    fun `override 1 - Studio Ghibli plus Watercolor is HIGH`() {
        val result = checkStyleCompatibility(VisualStyle.STUDIO_GHIBLI, VisualStyle.WATERCOLOR)
        assertEquals(CompatibilityLevel.HIGH, result.level)
        assertFalse(result.warning)
        // ترتیب معکوس هم باید همان نتیجه را بدهد (متقارن)
        assertEquals(CompatibilityLevel.HIGH, checkStyleCompatibility(VisualStyle.WATERCOLOR, VisualStyle.STUDIO_GHIBLI).level)
    }

    @Test
    fun `override 2 - Oil Painting plus Photorealistic is LOW, overriding the ARTISTIC x CINEMATIC category default of MEDIUM`() {
        // بررسی مستقل: طبق قانون Category، ARTISTIC × CINEMATIC = MEDIUM؛ این
        // جفت دقیقاً برای اثبات اینکه Override واقعاً روی Category اولویت دارد
        // انتخاب شد (نتیجه‌ی نهایی LOW است، نه MEDIUM).
        val result = checkStyleCompatibility(VisualStyle.OIL_PAINTING, VisualStyle.PHOTOREALISTIC)
        assertEquals(CompatibilityLevel.LOW, result.level)
        assertTrue(result.warning)
    }

    @Test
    fun `override 3 - major 3D animation studios plus popular 2D styles are HIGH`() {
        assertEquals(CompatibilityLevel.HIGH, checkStyleCompatibility(VisualStyle.PIXAR_DISNEY, VisualStyle.ANIME).level)
        assertEquals(CompatibilityLevel.HIGH, checkStyleCompatibility(VisualStyle.DREAMWORKS, VisualStyle.STUDIO_GHIBLI).level)
        assertEquals(CompatibilityLevel.HIGH, checkStyleCompatibility(VisualStyle.ILLUMINATION, VisualStyle.COMIC_BOOK).level)
    }

    @Test
    fun `override 4 - Cyberpunk plus handmade artistic styles are LOW`() {
        assertEquals(CompatibilityLevel.LOW, checkStyleCompatibility(VisualStyle.CYBERPUNK, VisualStyle.WATERCOLOR).level)
        assertEquals(CompatibilityLevel.LOW, checkStyleCompatibility(VisualStyle.CYBERPUNK, VisualStyle.OIL_PAINTING).level)
        assertEquals(CompatibilityLevel.LOW, checkStyleCompatibility(VisualStyle.CYBERPUNK, VisualStyle.PENCIL_SKETCH).level)
    }

    @Test
    fun `override 5 - Photorealistic plus non-realistic animated styles are LOW, including CLAYMATION which overrides the CINEMATIC x ANIMATION_3D category default of MEDIUM`() {
        assertEquals(CompatibilityLevel.LOW, checkStyleCompatibility(VisualStyle.PHOTOREALISTIC, VisualStyle.CARTOON).level)
        assertEquals(CompatibilityLevel.LOW, checkStyleCompatibility(VisualStyle.PHOTOREALISTIC, VisualStyle.COMIC_BOOK).level)
        // بررسی مستقل: PHOTOREALISTIC، CINEMATIC است؛ CLAYMATION، ANIMATION_3D
        // است؛ طبق قانون Category این ترکیب باید MEDIUM باشد — این تست دومین
        // اثبات صریح اولویت Override بر Category در همین فایل است.
        assertEquals(CompatibilityLevel.LOW, checkStyleCompatibility(VisualStyle.PHOTOREALISTIC, VisualStyle.CLAYMATION).level)
    }

    // --- VisualStyle.toStyleReference — اتصال کامل Style Matrix — قدم ۴-محتوا
    // از ۸ زیرقدم (ADR-116): یک تست رشته‌ی دقیق برای هرکدام از ۵ دسته + یک تست
    // کامل‌بودن (۳۴ ورودی). محتوای promptTokens عیناً از منبع رسمی معمار (ADR-116)
    // کپی شده — این تست‌ها فقط اثبات می‌کنند پیاده‌سازی دقیقاً همان متن را
    // برمی‌گرداند، نه بازتولید کل فهرست ۳۴تایی.

    @Test
    fun `toStyleReference for CINEMATIC_STYLE (CINEMATIC category) returns the exact promptTokens`() {
        val reference = VisualStyle.CINEMATIC_STYLE.toStyleReference()
        assertEquals("CINEMATIC_STYLE", reference.styleId)
        assertEquals(
            "cinematic style, professional color grading, shallow depth of field, dramatic composition, film-quality lighting",
            reference.promptTokens
        )
    }

    @Test
    fun `toStyleReference for PIXAR_DISNEY (ANIMATION_3D category) returns the exact promptTokens`() {
        val reference = VisualStyle.PIXAR_DISNEY.toStyleReference()
        assertEquals(
            "Pixar-style 3D animation, soft rounded character design, oversized expressive eyes, warm bounce lighting, polished rendered surfaces",
            reference.promptTokens
        )
    }

    @Test
    fun `toStyleReference for ANIME (ANIMATION_2D category) returns the exact promptTokens`() {
        val reference = VisualStyle.ANIME.toStyleReference()
        assertEquals(
            "anime style, bold clean linework, cel shading, expressive stylized eyes, dynamic action framing",
            reference.promptTokens
        )
    }

    @Test
    fun `toStyleReference for WATERCOLOR (ARTISTIC category) returns the exact promptTokens`() {
        val reference = VisualStyle.WATERCOLOR.toStyleReference()
        assertEquals(
            "watercolor painting, loose wet-on-wet technique, soft bleeding edges, visible paper texture, delicate translucent washes",
            reference.promptTokens
        )
    }

    @Test
    fun `toStyleReference for CYBERPUNK (GENRE category) returns the exact promptTokens`() {
        val reference = VisualStyle.CYBERPUNK.toStyleReference()
        assertEquals(
            "cyberpunk aesthetic, neon-lit rain-soaked streets, high-contrast magenta and cyan lighting, dense futuristic urban decay",
            reference.promptTokens
        )
    }

    @Test
    fun `toStyleReference covers all 34 VisualStyle entries, each with a unique styleId and non-blank promptTokens`() {
        val references = VisualStyle.entries.map { it.toStyleReference() }
        assertEquals(34, VisualStyle.entries.size)
        assertEquals(34, references.size)
        assertEquals(34, references.map { it.styleId }.toSet().size)
        references.forEach { assertTrue(it.promptTokens.isNotBlank()) }
    }
}
