package com.operaboys.cinemashotgenerator.domain.promptfinalization

import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelCapabilities
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelConstraints
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelFormat
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelProfile
import com.operaboys.cinemashotgenerator.domain.outputdelivery.RenderedOutput
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    private val plainTextProfile = profile.copy(
        profileId = "universal_default",
        platform = "generic",
        format = ModelFormat(type = "plain_text", structure = "paragraph")
    )

    // این تست از قبل وجود داشت: profile.format.type=="json" است، اما formattedPrompt
    // خودش متن آزاد است (نه JSON واقعی) — دقیقاً سناریویی که Fallback جدید
    // cleanJsonPromptValues (وقتی Parse شکست بخورد) باید بدون تغییر رفتار پوشش دهد.
    @Test
    fun `finalizePrompt cleans the rendered text and reports token usage, ready for composeOutput`() {
        val rendered = RenderedOutput(
            modelProfileId = "veo_3_1",
            formattedPrompt = "a sunny rainy day with a very very beautiful pretty scene",
            language = "en"
        )

        val result = finalizePrompt(rendered, profile)

        assertEquals("veo_3_1", result.rendered.modelProfileId)
        assertEquals("en", result.rendered.language)
        assertFalse(result.rendered.formattedPrompt.contains("rainy", ignoreCase = true))
        assertFalse(result.rendered.formattedPrompt.contains("pretty", ignoreCase = true))
        assertTrue(result.tokenCheck.withinLimit)
        assertTrue(result.cleaningReport.conflictsDetected > 0)
    }

    // --- رفع محدودیت ADR-025/026: finalizePrompt نباید JSON واقعی را خراب کند ---

    @Test
    fun `finalizePrompt with a real JSON RenderedOutput stays valid and parseable JSON`() {
        val rawJson = """{"subject":"a detective","scene":"tense atmosphere","camera":"eye level, medium shot"}"""
        val rendered = RenderedOutput(modelProfileId = "veo_3_1", formattedPrompt = rawJson, language = "en")

        val cleanedRendered = finalizePrompt(rendered, profile).rendered

        val parsed = Json.parseToJsonElement(cleanedRendered.formattedPrompt).jsonObject
        assertTrue(parsed.containsKey("subject"))
        assertTrue(parsed.containsKey("scene"))
        assertTrue(parsed.containsKey("camera"))
    }

    @Test
    fun `finalizePrompt removes a duplicate synonym inside a single JSON field without breaking other keys`() {
        val rawJson = """{"subject":"a beautiful gorgeous detective","scene":"tense atmosphere"}"""
        val rendered = RenderedOutput(modelProfileId = "veo_3_1", formattedPrompt = rawJson, language = "en")

        val cleanedRendered = finalizePrompt(rendered, profile).rendered

        val parsed = Json.parseToJsonElement(cleanedRendered.formattedPrompt).jsonObject
        val subject = parsed.getValue("subject").jsonPrimitive.content
        assertFalse(subject.contains("gorgeous", ignoreCase = true))
        assertTrue(subject.contains("beautiful", ignoreCase = true))
        // فیلد دیگر باید کاملاً دست‌نخورده مانده باشد
        assertEquals("tense atmosphere", parsed.getValue("scene").jsonPrimitive.content)
    }

    @Test
    fun `finalizePrompt does not force capitalization or a trailing period onto individual JSON field values`() {
        val rawJson = """{"camera":"eye level, medium shot"}"""
        val rendered = RenderedOutput(modelProfileId = "veo_3_1", formattedPrompt = rawJson, language = "en")

        val cleanedRendered = finalizePrompt(rendered, profile).rendered

        val parsed = Json.parseToJsonElement(cleanedRendered.formattedPrompt).jsonObject
        assertEquals("eye level, medium shot", parsed.getValue("camera").jsonPrimitive.content)
    }

    @Test
    fun `finalizePrompt leaves the weightedEmphasis nested object untouched for a JSON profile`() {
        val rawJson = """{"subject":"a beautiful gorgeous detective","weightedEmphasis":{"cinematic lighting":1.2}}"""
        val rendered = RenderedOutput(modelProfileId = "veo_3_1", formattedPrompt = rawJson, language = "en")

        val cleanedRendered = finalizePrompt(rendered, profile).rendered

        val parsed = Json.parseToJsonElement(cleanedRendered.formattedPrompt).jsonObject
        val weights = parsed.getValue("weightedEmphasis").jsonObject
        assertEquals(1.2f, weights.getValue("cinematic lighting").jsonPrimitive.content.toFloat())
    }

    // --- رگرسیون صریح: پروفایل غیر-JSON دقیقاً مثل قبل رفتار می‌کند ---

    @Test
    fun `finalizePrompt with a non-json profile behaves exactly as before`() {
        val text = "a sunny rainy day with a very very beautiful pretty scene"
        val rendered = RenderedOutput(modelProfileId = "universal_default", formattedPrompt = text, language = "en")

        val result = finalizePrompt(rendered, plainTextProfile)
        val expectedCleaned = cleanPrompt(text, CleaningOptions()).first

        assertEquals(expectedCleaned, result.rendered.formattedPrompt)
        assertEquals(checkTokenLimit(expectedCleaned, plainTextProfile), result.tokenCheck)
    }

    @Test
    fun `finalizePrompt aggregates a CleaningReport across JSON fields for validateCompressionRatio`() {
        val rawJson = """{"subject":"a very very beautiful gorgeous detective","scene":"tense atmosphere"}"""
        val rendered = RenderedOutput(modelProfileId = "veo_3_1", formattedPrompt = rawJson, language = "en")

        val result = finalizePrompt(rendered, profile)

        assertTrue(result.cleaningReport.redundancyRemoved > 0)
        assertTrue(result.cleaningReport.stopWordsRemoved > 0)
        assertEquals(rawJson.length, result.cleaningReport.originalLength)
        assertEquals(result.rendered.formattedPrompt.length, result.cleaningReport.cleanedLength)
    }
}
