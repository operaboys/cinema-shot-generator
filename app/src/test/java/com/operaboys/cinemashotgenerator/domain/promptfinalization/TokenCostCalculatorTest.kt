package com.operaboys.cinemashotgenerator.domain.promptfinalization

import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelCapabilities
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelConstraints
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelFormat
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenCostCalculatorTest {

    private val profile = ModelProfile(
        profileId = "veo_3_1",
        platform = "veo",
        capabilities = ModelCapabilities(
            supportsVideo = true, supportsImage = false,
            supportsWeightedTags = false, supportsImagePrompt = false, supportsNegativePrompt = false
        ),
        constraints = ModelConstraints(maxPromptLength = 2000, maxTokens = 10),
        format = ModelFormat(type = "json", structure = "paragraph")
    )

    // --- estimateTokensFromCharacters ---

    @Test
    fun `estimateTokensFromCharacters rounds up character count divided by four`() {
        assertEquals(3, estimateTokensFromCharacters("12345678910")) // 11 chars -> 2.75 -> 3
        assertEquals(1, estimateTokensFromCharacters("abc")) // 3 chars -> 0.75 -> 1
    }

    // --- estimateTokensFromWords ---

    @Test
    fun `estimateTokensFromWords rounds up word count times 1_33`() {
        assertEquals(3, estimateTokensFromWords("one two")) // 2 words * 1.33 = 2.66 -> 3
        assertEquals(2, estimateTokensFromWords("one")) // 1 word * 1.33 = 1.33 -> 2
    }

    // --- checkTokenLimit ---

    @Test
    fun `checkTokenLimit is within limit for a short text`() {
        val result = checkTokenLimit("short text", profile)
        assertTrue(result.withinLimit)
        assertNull(result.warning)
    }

    @Test
    fun `checkTokenLimit warns and is not within limit for a long text`() {
        val longText = (1..20).joinToString(" ") { "word" }
        val result = checkTokenLimit(longText, profile)
        assertFalse(result.withinLimit)
        assertNotNull(result.warning)
    }
}
