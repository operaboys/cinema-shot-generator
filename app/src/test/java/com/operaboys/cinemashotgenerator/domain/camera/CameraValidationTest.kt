package com.operaboys.cinemashotgenerator.domain.camera

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraValidationTest {

    // --- لنز Ultra-wide + Close-up/Extreme Close-up ---

    @Test
    fun `ultra-wide lens with close-up blocks`() {
        val issue = checkLensDistanceMismatch(LensType.ULTRA_WIDE, CameraDistance.CLOSE_UP)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `ultra-wide lens with extreme close-up blocks`() {
        val issue = checkLensDistanceMismatch(LensType.ULTRA_WIDE, CameraDistance.EXTREME_CLOSE_UP)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `standard lens with close-up is valid`() {
        assertNull(checkLensDistanceMismatch(LensType.STANDARD, CameraDistance.CLOSE_UP))
    }

    // --- Static Movement + Handheld Stabilization ---

    @Test
    fun `static movement with handheld stabilization warns`() {
        val movement = CameraMovement.Basic(type = BasicMovementType.STATIC)
        val issue = checkStaticMovementWithHandheldStabilization(movement, Stabilization.HANDHELD)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `static movement with tripod is valid`() {
        val movement = CameraMovement.Basic(type = BasicMovementType.STATIC)
        assertNull(checkStaticMovementWithHandheldStabilization(movement, Stabilization.TRIPOD))
    }

    @Test
    fun `non-static movement with handheld is valid`() {
        val movement = CameraMovement.Basic(type = BasicMovementType.TRACKING)
        assertNull(checkStaticMovementWithHandheldStabilization(movement, Stabilization.HANDHELD))
    }

    // --- Rack Focus با کمتر از ۲ Subject ---

    @Test
    fun `rack focus with one subject blocks`() {
        val issue = checkRackFocusSubjectCount(FocusMode.RACK_FOCUS, subjectCount = 1)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `rack focus with two subjects is valid`() {
        assertNull(checkRackFocusSubjectCount(FocusMode.RACK_FOCUS, subjectCount = 2))
    }

    @Test
    fun `non rack focus mode with one subject is valid`() {
        assertNull(checkRackFocusSubjectCount(FocusMode.AUTO, subjectCount = 1))
    }

    // --- Extreme Wide + Very Shallow DoF ---

    @Test
    fun `extreme wide with shallow dof warns`() {
        val issue = checkExtremeWideWithShallowDepthOfField(CameraDistance.EXTREME_WIDE, DepthOfField.SHALLOW)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `extreme wide with deep dof is valid`() {
        assertNull(checkExtremeWideWithShallowDepthOfField(CameraDistance.EXTREME_WIDE, DepthOfField.DEEP))
    }

    // --- مدت حرکت دوربین بیشتر از Duration شات ---

    @Test
    fun `movement duration exceeding shot duration blocks`() {
        val issue = validateCameraMovementDuration(movementDurationSeconds = 6f, shotDurationSeconds = 4f)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `movement duration within shot duration is valid`() {
        assertNull(validateCameraMovementDuration(movementDurationSeconds = 3f, shotDurationSeconds = 4f))
    }
}
