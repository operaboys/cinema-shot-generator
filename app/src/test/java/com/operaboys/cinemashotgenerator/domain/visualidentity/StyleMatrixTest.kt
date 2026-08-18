package com.operaboys.cinemashotgenerator.domain.visualidentity

import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    // --- Rule 1 (Blocking، تضمین در سطح Type System) ---

    @Test
    fun `rule1 style matrix always exposes a non-null primary style`() {
        // ساخت StyleMatrix بدون primaryStyle اصلاً کامپایل نمی‌شود؛ این تست فقط
        // مسیر مثبت را مستند می‌کند — مسیر منفی در سطح کامپایلر مسدود است، نه Runtime.
        val matrix = StyleMatrix(styleMatrixId = "sm_001", primaryStyle = VisualStyle.STUDIO_GHIBLI)
        assertNotNull(matrix.primaryStyle)
        assertEquals(VisualStyle.STUDIO_GHIBLI, matrix.primaryStyle)
    }

    // --- Rule 2 (Warning): ناسازگاری سبک اصلی/ثانویه ---

    @Test
    fun `rule2 with secondary style returns a real compatibility result`() {
        val matrix = StyleMatrix(
            styleMatrixId = "sm_001",
            primaryStyle = VisualStyle.STUDIO_GHIBLI,
            secondaryStyle = VisualStyle.PHOTOREALISTIC
        )
        val result = checkStyleMatrixCompatibility(matrix)
        assertNotNull(result)
        // STUDIO_GHIBLI (ANIMATION_2D) × PHOTOREALISTIC (CINEMATIC) = CINEMATIC×ANIMATION_2D = LOW
        assertEquals(CompatibilityLevel.LOW, result!!.level)
        assertTrue(result.warning)
    }

    @Test
    fun `rule2 without secondary style returns null`() {
        val matrix = StyleMatrix(styleMatrixId = "sm_001", primaryStyle = VisualStyle.STUDIO_GHIBLI)
        assertNull(checkStyleMatrixCompatibility(matrix))
    }

    // --- Rule 3 (Blocking روی تلاش حذف) — امضا در قدم ۳ (ADR-115) به VisualStyle? تغییر کرد ---

    @Test
    fun `rule3 attempting to null primary style is blocked`() {
        val result = validatePrimaryStyleUpdate(null)
        assertTrue(result is StyleUpdateResult.Blocked)
    }

    @Test
    fun `rule3 replacing primary style with another is allowed`() {
        val result = validatePrimaryStyleUpdate(VisualStyle.PHOTOREALISTIC)
        assertTrue(result is StyleUpdateResult.Allowed)
    }
}
