package com.operaboys.cinemashotgenerator.domain.dna

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DnaValidationTest {

    private fun sampleDna(
        forbiddenElements: Map<String, List<String>> = mapOf(
            "camera" to listOf("dutch_angle"),
            "lighting" to listOf("top_light"),
            "weather" to emptyList()
        ),
        mandatoryElements: List<String> = listOf("atmospheric_depth", "color_grading"),
        maxShotDurationSeconds: Int = 10,
        aspectRatio: String = "2.39:1",
        requiresHumanApproval: Boolean = true
    ) = ProjectDna(
        dnaId = "dna_001",
        projectId = "proj_001",
        coreIdentity = CoreIdentity(
            dominantVisualStyle = VisualStyle.CINEMATIC,
            realismLevel = RealismLevel.GROUNDED,
            styleConsistency = StyleConsistency.STRICT,
            locked = true
        ),
        masterPalette = MasterPalette(
            colorTemperature = ColorTemperature.WARM,
            globalSaturation = SaturationLevel.MEDIUM,
            globalContrast = SaturationLevel.HIGH,
            colorGradingPreset = "natural"
        ),
        globalMoodBase = GlobalMoodBase(
            primaryEmotion = "mysterious",
            intensity = IntensityLevel.MEDIUM,
            consistency = StyleConsistency.STRICT
        ),
        stylePreferences = StylePreferences(
            cinematicLanguage = "long_take",
            colorPhilosophy = ColorPhilosophy(
                paletteType = "natural",
                dominantColors = listOf("earth_tones", "muted_blues"),
                contrastPreference = "medium_high"
            ),
            cameraPreferences = CameraPreferences(
                preferredMovements = listOf("dolly", "tracking", "static"),
                avoidMovements = listOf("crane", "extreme_handheld")
            ),
            lightingPreferences = LightingPreferences(
                preferredStyles = listOf("cinematic", "dramatic"),
                avoidStyles = listOf("flat", "overly_bright")
            )
        ),
        outputConstraints = OutputConstraints(
            forbiddenElements = forbiddenElements,
            mandatoryElements = mandatoryElements,
            maxShotDurationSeconds = maxShotDurationSeconds,
            aspectRatio = aspectRatio
        ),
        overrideRules = OverrideRules(
            allowSceneOverride = true,
            allowShotOverride = true,
            requiresHumanApproval = requiresHumanApproval
        )
    )

    // --- Rule 1 (Soft Lock): تغییر Core Identity هرگز بلاک نمی‌شود ---

    @Test
    fun `updateCoreIdentity without dependent shots applies change with no warning`() {
        val result = updateCoreIdentity(sampleDna(), VisualStyle.STYLIZED, dependentShotsCount = 0)

        assertEquals(VisualStyle.STYLIZED, result.updatedDna.coreIdentity.dominantVisualStyle)
        assertEquals(null, result.warning)
    }

    @Test
    fun `updateCoreIdentity with dependent shots warns but still applies change`() {
        val result = updateCoreIdentity(sampleDna(), VisualStyle.STYLIZED, dependentShotsCount = 5)

        assertEquals(VisualStyle.STYLIZED, result.updatedDna.coreIdentity.dominantVisualStyle)
        assertTrue(result.warning != null)
        assertTrue(result.warning!!.contains("5"))
    }

    // --- Rule 2 (Blocking): forbidden_elements، پوشش هر سه دسته ---

    @Test
    fun `validateShotAgainstDna blocks forbidden camera element`() {
        val result = validateShotAgainstDna("dutch_angle", "camera", sampleDna())

        assertEquals(Severity.BLOCKING, result!!.severity)
    }

    @Test
    fun `validateShotAgainstDna blocks forbidden lighting element`() {
        val result = validateShotAgainstDna("top_light", "lighting", sampleDna())

        assertEquals(Severity.BLOCKING, result!!.severity)
    }

    @Test
    fun `validateShotAgainstDna allows permitted element`() {
        val result = validateShotAgainstDna("tracking", "camera", sampleDna())

        assertNull(result)
    }

    @Test
    fun `validateShotAgainstDna allows element in category with no forbidden list`() {
        val result = validateShotAgainstDna("rain", "weather", sampleDna())

        assertNull(result)
    }

    // --- Rule 4 (Blocking): technical_constraints ---

    @Test
    fun `validateShotDuration within limit is valid`() {
        val result = validateShotDuration(8, sampleDna(maxShotDurationSeconds = 10))

        assertNull(result)
    }

    @Test
    fun `validateShotDuration exceeding limit blocks`() {
        val result = validateShotDuration(15, sampleDna(maxShotDurationSeconds = 10))

        assertEquals(Severity.BLOCKING, result!!.severity)
    }

    @Test
    fun `validateShotAspectRatio matching is valid`() {
        val result = validateShotAspectRatio("2.39:1", sampleDna(aspectRatio = "2.39:1"))

        assertNull(result)
    }

    @Test
    fun `validateShotAspectRatio mismatch blocks`() {
        val result = validateShotAspectRatio("16:9", sampleDna(aspectRatio = "2.39:1"))

        assertEquals(Severity.BLOCKING, result!!.severity)
    }

    // --- Rule 3 (Warning helper): mandatory_elements ---

    @Test
    fun `checkMandatoryElementsPresent with all elements included is valid`() {
        val result = checkMandatoryElementsPresent(
            includedElements = listOf("atmospheric_depth", "color_grading", "extra_thing"),
            dna = sampleDna()
        )

        assertNull(result)
    }

    @Test
    fun `checkMandatoryElementsPresent with missing element warns`() {
        val result = checkMandatoryElementsPresent(
            includedElements = listOf("atmospheric_depth"),
            dna = sampleDna()
        )

        assertEquals(Severity.WARNING, result!!.severity)
        assertTrue(result.message.contains("color_grading"))
    }

    // --- Rule 5 (helper): requiresApprovalForOverride ---

    @Test
    fun `requiresApprovalForOverride reflects true flag`() {
        assertTrue(requiresApprovalForOverride(sampleDna(requiresHumanApproval = true)))
    }

    @Test
    fun `requiresApprovalForOverride reflects false flag`() {
        assertFalse(requiresApprovalForOverride(sampleDna(requiresHumanApproval = false)))
    }
}
