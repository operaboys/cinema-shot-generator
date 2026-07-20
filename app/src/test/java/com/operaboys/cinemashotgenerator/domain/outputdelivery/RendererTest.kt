package com.operaboys.cinemashotgenerator.domain.outputdelivery

import com.operaboys.cinemashotgenerator.domain.promptengine.PromptBlueprint
import com.operaboys.cinemashotgenerator.domain.promptengine.StructuredParts
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RendererTest {

    private fun structuredParts(
        timelineBeats: String? = null,
        audioDescription: String? = null,
        environmentSpecs: String? = "rainy street"
    ) = StructuredParts(
        subjectDescription = "a detective",
        sceneContext = "tense atmosphere",
        shotDescription = "walks into the office",
        cameraSpecs = "eye level, medium shot",
        lightingSpecs = "dramatic lighting",
        environmentSpecs = environmentSpecs,
        styleModifiers = "cinematic style",
        timelineBeats = timelineBeats,
        audioDescription = audioDescription
    )

    private fun blueprint(
        parts: StructuredParts = structuredParts(),
        weightedEmphasis: Map<String, Float> = emptyMap()
    ) = PromptBlueprint(
        promptBlueprintId = "prompt_001",
        shotId = "shot_001",
        structuredParts = parts,
        imageReferences = emptyList(),
        weightedEmphasis = weightedEmphasis,
        seed = null,
        conflictsResolved = 0,
        warnings = emptyList()
    )

    private val videoCapableProfile = ModelProfile(
        profileId = "veo_3_1",
        platform = "veo",
        capabilities = ModelCapabilities(
            supportsVideo = true, supportsImage = false,
            supportsWeightedTags = false, supportsImagePrompt = false, supportsNegativePrompt = false
        ),
        constraints = ModelConstraints(maxPromptLength = 2000, maxTokens = 500),
        format = ModelFormat(type = "json", structure = "paragraph")
    )

    private val noVideoProfile = videoCapableProfile.copy(
        capabilities = videoCapableProfile.capabilities.copy(supportsVideo = false)
    )

    // --- renderBlueprintToText ---

    @Test
    fun `renderBlueprintToText includes timeline and audio when the profile supports video`() {
        val bp = blueprint(structuredParts(timelineBeats = "walks at 1.5s", audioDescription = "rain sound"))
        val text = renderBlueprintToText(bp, videoCapableProfile)
        assertTrue(text.contains("Timeline: walks at 1.5s"))
        assertTrue(text.contains("Audio: rain sound"))
    }

    @Test
    fun `renderBlueprintToText excludes timeline and audio when the profile does not support video`() {
        val bp = blueprint(structuredParts(timelineBeats = "walks at 1.5s", audioDescription = "rain sound"))
        val text = renderBlueprintToText(bp, noVideoProfile)
        assertFalse(text.contains("Timeline:"))
        assertFalse(text.contains("Audio:"))
    }

    // --- optimizeForProfile ---

    @Test
    fun `optimizeForProfile leaves short text untouched`() {
        val bp = blueprint()
        val optimized = optimizeForProfile(bp, videoCapableProfile)
        assertFalse(optimized.endsWith("..."))
    }

    @Test
    fun `optimizeForProfile truncates text longer than maxPromptLength`() {
        val longDescription = "a".repeat(50)
        val bp = blueprint(structuredParts(environmentSpecs = longDescription))
        val tightProfile = videoCapableProfile.copy(constraints = ModelConstraints(maxPromptLength = 10, maxTokens = 500))
        val optimized = optimizeForProfile(bp, tightProfile)
        assertEquals(10, optimized.length)
        assertTrue(optimized.endsWith("..."))
    }

    // --- validatePromptLength ---

    @Test
    fun `validatePromptLength is warning when text exceeds the limit`() {
        val tightProfile = videoCapableProfile.copy(constraints = ModelConstraints(maxPromptLength = 5, maxTokens = 500))
        val issue = validatePromptLength("a much longer text than five chars", tightProfile)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `validatePromptLength is null when text is within the limit`() {
        assertNull(validatePromptLength("short", videoCapableProfile))
    }

    // --- validateUnsupportedFeatureUsage ---

    @Test
    fun `validateUnsupportedFeatureUsage warns when video-only content is dropped`() {
        val bp = blueprint(structuredParts(timelineBeats = "walks at 1.5s"))
        val issue = validateUnsupportedFeatureUsage(bp, noVideoProfile)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `validateUnsupportedFeatureUsage is null when the profile supports video`() {
        val bp = blueprint(structuredParts(timelineBeats = "walks at 1.5s"))
        assertNull(validateUnsupportedFeatureUsage(bp, videoCapableProfile))
    }
}
