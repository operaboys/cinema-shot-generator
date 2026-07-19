package com.operaboys.cinemashotgenerator.domain.scene

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneValidationTest {

    private val sampleScene = Scene(
        sceneId = "scene_001",
        sceneNumber = 1,
        narrativeRole = NarrativeRole.CLIMAX,
        location = SceneLocation(type = LocationType.OUTDOOR, description = "خیابان شلوغ شهری، شب"),
        timeOfDay = TimeOfDay.NIGHT,
        atmospherePrimary = Atmosphere.TENSE
    )

    // --- Rule 1 ---

    @Test
    fun `rule1 scene with no shots is blocking`() {
        val issue = validateSceneHasShotsBeforeFinalize(0)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `rule1 scene with at least one shot is valid`() {
        assertNull(validateSceneHasShotsBeforeFinalize(1))
    }

    // --- Rule 4 ---

    @Test
    fun `rule4 noon lighting indoors warns`() {
        val issue = checkNoonLightingInIndoor(TimeOfDay.NOON, LocationType.INDOOR)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `rule4 noon lighting outdoors has no conflict`() {
        assertNull(checkNoonLightingInIndoor(TimeOfDay.NOON, LocationType.OUTDOOR))
    }

    // --- Rule 5 ---

    @Test
    fun `rule5 calm atmosphere in climax warns`() {
        val issue = checkCalmAtmosphereInClimax(NarrativeRole.CLIMAX, Atmosphere.CALM)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `rule5 tense atmosphere in climax has no conflict`() {
        assertNull(checkCalmAtmosphereInClimax(NarrativeRole.CLIMAX, Atmosphere.TENSE))
    }

    // --- deleteScene ---

    @Test
    fun `deleteScene succeeds when no shots exist`() {
        val result = deleteScene(sampleScene, existingShotsCount = 0)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteScene fails when shots still exist`() {
        val result = deleteScene(sampleScene, existingShotsCount = 5)
        assertTrue(result.isFailure)
    }
}
