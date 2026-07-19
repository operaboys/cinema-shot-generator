package com.operaboys.cinemashotgenerator.domain.visualidentity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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

    // --- getInfluenceModifier ---

    @Test
    fun `getInfluenceModifier covers all three levels`() {
        assertEquals("with subtle hints of", getInfluenceModifier(StyleInfluence.SUBTLE))
        assertEquals("with elements of", getInfluenceModifier(StyleInfluence.MODERATE))
        assertEquals("strongly influenced by", getInfluenceModifier(StyleInfluence.STRONG))
    }

    // --- combineStyles ---

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

    // --- checkStyleCompatibility (رفتار حداقلی طبق بلوپرینت) ---

    @Test
    fun `checkStyleCompatibility defaults to MEDIUM without warning`() {
        val result = checkStyleCompatibility("style_ghibli", "style_realistic")
        assertEquals(CompatibilityLevel.MEDIUM, result.level)
        assertFalse(result.warning)
    }

    // --- Rule 1 (Blocking، تضمین در سطح Type System) ---

    @Test
    fun `rule1 style matrix always exposes a non-null primary style`() {
        // ساخت StyleMatrix بدون primaryStyle اصلاً کامپایل نمی‌شود؛ این تست فقط
        // مسیر مثبت را مستند می‌کند — مسیر منفی در سطح کامپایلر مسدود است، نه Runtime.
        val matrix = StyleMatrix(styleMatrixId = "sm_001", primaryStyle = ghibli)
        assertNotNull(matrix.primaryStyle)
        assertEquals("style_ghibli", matrix.primaryStyle.styleId)
    }

    // --- Rule 2 (Warning): ناسازگاری سبک اصلی/ثانویه ---

    @Test
    fun `rule2 with secondary style returns compatibility result`() {
        val matrix = StyleMatrix(
            styleMatrixId = "sm_001",
            primaryStyle = ghibli,
            secondaryStyle = photorealistic
        )
        val result = checkStyleMatrixCompatibility(matrix)
        assertNotNull(result)
        assertFalse(result!!.warning)
    }

    @Test
    fun `rule2 without secondary style returns null`() {
        val matrix = StyleMatrix(styleMatrixId = "sm_001", primaryStyle = ghibli)
        assertNull(checkStyleMatrixCompatibility(matrix))
    }

    // --- Rule 3 (Blocking روی تلاش حذف) ---

    @Test
    fun `rule3 attempting to null primary style is blocked`() {
        val result = validatePrimaryStyleUpdate(null)
        assertTrue(result is StyleUpdateResult.Blocked)
    }

    @Test
    fun `rule3 replacing primary style with another is allowed`() {
        val result = validatePrimaryStyleUpdate(photorealistic)
        assertTrue(result is StyleUpdateResult.Allowed)
    }
}
