package com.operaboys.cinemashotgenerator.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LogicConflictCheckerTest {

    // --- checkFastMotionLongTake ---

    @Test
    fun `dynamic motion with long take warns`() {
        val issue = checkFastMotionLongTake(motionLevel = "dynamic", cinematicMode = "long_take")

        assertNotNull(issue)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `extreme motion with long take warns`() {
        val issue = checkFastMotionLongTake(motionLevel = "extreme", cinematicMode = "long_take")

        assertNotNull(issue)
    }

    @Test
    fun `static motion with long take has no conflict`() {
        val issue = checkFastMotionLongTake(motionLevel = "static", cinematicMode = "long_take")

        assertNull(issue)
    }

    @Test
    fun `dynamic motion with fast cut has no conflict`() {
        val issue = checkFastMotionLongTake(motionLevel = "dynamic", cinematicMode = "fast_cut")

        assertNull(issue)
    }

    // --- checkStaticCameraInChase ---

    @Test
    fun `static camera in chase scene warns`() {
        val issue = checkStaticCameraInChase(
            cameraMovementType = "static",
            shotDescription = "The detective runs through a chase across rooftops"
        )

        assertNotNull(issue)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `static camera in calm scene has no conflict`() {
        val issue = checkStaticCameraInChase(
            cameraMovementType = "static",
            shotDescription = "A quiet conversation over coffee"
        )

        assertNull(issue)
    }

    @Test
    fun `tracking camera in chase scene has no conflict`() {
        val issue = checkStaticCameraInChase(
            cameraMovementType = "tracking",
            shotDescription = "A tense pursuit through the alley"
        )

        assertNull(issue)
    }
}
