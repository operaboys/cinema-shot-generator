package com.operaboys.cinemashotgenerator.domain.outputdelivery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutputComposerTest {

    private val renderedOutputs = listOf(
        RenderedOutput(modelProfileId = "veo_3_1", formattedPrompt = "a cinematic shot", language = "en")
    )

    private val bilingualPrompts = BilingualPrompts(enVersion = "a cinematic shot", faVersion = "یک نمای سینمایی")

    // --- composeOutput ---

    @Test(expected = IllegalArgumentException::class)
    fun `composeOutput throws when renderedOutputs is empty`() {
        composeOutput(
            shotId = "shot_001",
            promptBlueprintId = "prompt_001",
            renderedOutputs = emptyList(),
            bilingualPrompts = bilingualPrompts
        )
    }

    @Test
    fun `composeOutput builds the package with correct export files without mutating renderedOutputs`() {
        val result = composeOutput(
            shotId = "shot_001",
            promptBlueprintId = "prompt_001",
            renderedOutputs = renderedOutputs,
            bilingualPrompts = bilingualPrompts,
            idProvider = { "output_test0001" }
        )

        assertEquals("output_test0001", result.outputId)
        assertEquals("shot_001", result.shotId)
        assertEquals("prompt_001", result.promptBlueprintId)
        assertEquals(renderedOutputs, result.renderedOutputs)

        assertEquals(3, result.exportFiles.size)
        assertTrue(result.exportFiles.any { it.filename == "shot_001_prompt_en.txt" && it.content == "a cinematic shot" })
        assertTrue(result.exportFiles.any { it.filename == "shot_001_prompt_fa.txt" && it.content == "یک نمای سینمایی" })
        assertTrue(result.exportFiles.any { it.filename == "shot_001_veo_3_1.txt" && it.content == "a cinematic shot" })
    }
}
