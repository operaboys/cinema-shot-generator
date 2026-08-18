package com.operaboys.cinemashotgenerator.domain.validation

import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
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

    // --- checkSlowMotionInDialogue (تکمیل Rule یتیم — قدم ۳ب از ۲ زیرقدم قدم ۳، ADR-111) ---

    @Test
    fun `static motion with dialogue warns`() {
        val issue = checkSlowMotionInDialogue(motionLevel = MotionLevel.STATIC, shotGoal = ShotGoal.DIALOGUE)

        assertNotNull(issue)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `subtle motion with dialogue warns`() {
        val issue = checkSlowMotionInDialogue(motionLevel = MotionLevel.SUBTLE, shotGoal = ShotGoal.DIALOGUE)

        assertNotNull(issue)
    }

    @Test
    fun `static motion with a non-dialogue goal has no conflict`() {
        val issue = checkSlowMotionInDialogue(motionLevel = MotionLevel.STATIC, shotGoal = ShotGoal.ACTION)

        assertNull(issue)
    }

    @Test
    fun `dynamic motion with dialogue has no conflict`() {
        val issue = checkSlowMotionInDialogue(motionLevel = MotionLevel.DYNAMIC, shotGoal = ShotGoal.DIALOGUE)

        assertNull(issue)
    }

    @Test
    fun `extreme motion with dialogue has no conflict`() {
        val issue = checkSlowMotionInDialogue(motionLevel = MotionLevel.EXTREME, shotGoal = ShotGoal.DIALOGUE)

        assertNull(issue)
    }

    @Test
    fun `moderate motion with dialogue has no conflict (neutral middle, neither slow nor fast)`() {
        val issue = checkSlowMotionInDialogue(motionLevel = MotionLevel.MODERATE, shotGoal = ShotGoal.DIALOGUE)

        assertNull(issue)
    }
}
