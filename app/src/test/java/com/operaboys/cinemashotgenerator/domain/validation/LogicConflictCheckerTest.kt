package com.operaboys.cinemashotgenerator.domain.validation

import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LogicConflictCheckerTest {

    // --- checkFastMotionLongTake ---

    @Test
    fun `dynamic motion with long take warns`() {
        val issue = checkFastMotionLongTake(motionLevel = MotionLevel.DYNAMIC, cinematicMode = CinematicMode.LONG_TAKE)

        assertNotNull(issue)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `extreme motion with long take warns`() {
        val issue = checkFastMotionLongTake(motionLevel = MotionLevel.EXTREME, cinematicMode = CinematicMode.LONG_TAKE)

        assertNotNull(issue)
    }

    @Test
    fun `static motion with long take has no conflict`() {
        val issue = checkFastMotionLongTake(motionLevel = MotionLevel.STATIC, cinematicMode = CinematicMode.LONG_TAKE)

        assertNull(issue)
    }

    @Test
    fun `dynamic motion with fast cut has no conflict`() {
        val issue = checkFastMotionLongTake(motionLevel = MotionLevel.DYNAMIC, cinematicMode = CinematicMode.FAST_CUT)

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
