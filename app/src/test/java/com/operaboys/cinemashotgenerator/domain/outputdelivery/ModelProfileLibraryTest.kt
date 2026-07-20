package com.operaboys.cinemashotgenerator.domain.outputdelivery

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelProfileLibraryTest {

    private val veoProfile = ModelProfile(
        profileId = "veo_3_1",
        platform = "veo",
        capabilities = ModelCapabilities(
            supportsVideo = true, supportsImage = false,
            supportsWeightedTags = false, supportsImagePrompt = false, supportsNegativePrompt = false
        ),
        constraints = ModelConstraints(maxPromptLength = 2000, maxTokens = 500),
        format = ModelFormat(type = "json", structure = "paragraph")
    )

    private val profiles = listOf(veoProfile, universalDefaultProfile)

    // --- selectProfile ---

    @Test
    fun `selectProfile returns the exact platform match when available`() {
        val result = selectProfile("veo", profiles)
        assertEquals("veo_3_1", result.profileId)
    }

    @Test
    fun `selectProfile falls back to universal_default for an unknown platform`() {
        val result = selectProfile("unknown_platform", profiles)
        assertEquals("universal_default", result.profileId)
    }

    // --- validateProfileAvailability ---

    @Test
    fun `validateProfileAvailability is null when an exact match exists`() {
        assertNull(validateProfileAvailability("veo", profiles))
    }

    @Test
    fun `validateProfileAvailability is null when only universal_default is available`() {
        assertNull(validateProfileAvailability("unknown_platform", profiles))
    }

    @Test
    fun `validateProfileAvailability is blocking when nothing is available at all`() {
        val issue = validateProfileAvailability("veo", emptyList())
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }
}
