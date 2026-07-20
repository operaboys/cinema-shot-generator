package com.operaboys.cinemashotgenerator.domain.promptfinalization

import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelCapabilities
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelConstraints
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelFormat
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelProfile
import com.operaboys.cinemashotgenerator.domain.outputdelivery.RenderedOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptFinalizationPipelineTest {

    private val profile = ModelProfile(
        profileId = "veo_3_1",
        platform = "veo",
        capabilities = ModelCapabilities(
            supportsVideo = true, supportsImage = false,
            supportsWeightedTags = false, supportsImagePrompt = false, supportsNegativePrompt = false
        ),
        constraints = ModelConstraints(maxPromptLength = 2000, maxTokens = 500),
        format = ModelFormat(type = "json", structure = "paragraph")
    )

    @Test
    fun `finalizePrompt cleans the rendered text and reports token usage, ready for composeOutput`() {
        val rendered = RenderedOutput(
            modelProfileId = "veo_3_1",
            formattedPrompt = "a sunny rainy day with a very very beautiful pretty scene",
            language = "en"
        )

        val (cleanedRendered, tokenCheck) = finalizePrompt(rendered, profile)

        assertEquals("veo_3_1", cleanedRendered.modelProfileId)
        assertEquals("en", cleanedRendered.language)
        assertFalse(cleanedRendered.formattedPrompt.contains("rainy", ignoreCase = true))
        assertFalse(cleanedRendered.formattedPrompt.contains("pretty", ignoreCase = true))
        assertTrue(tokenCheck.withinLimit)
    }
}
