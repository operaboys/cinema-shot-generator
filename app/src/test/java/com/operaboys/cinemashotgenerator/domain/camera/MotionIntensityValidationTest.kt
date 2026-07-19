package com.operaboys.cinemashotgenerator.domain.camera

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MotionIntensityValidationTest {

    // --- validateSpeedIntensity ---

    @Test
    fun `slow speed with high intensity warns`() {
        val motion = SubjectMotion(speed = SubjectSpeed.SLOW, intensity = 8, motionType = MotionType.CONTINUOUS)
        val issue = validateSpeedIntensity(motion)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `hyperkinetic speed with low intensity warns`() {
        val motion = SubjectMotion(speed = SubjectSpeed.HYPERKINETIC, intensity = 3, motionType = MotionType.SUDDEN)
        val issue = validateSpeedIntensity(motion)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `normal speed with moderate intensity is valid`() {
        val motion = SubjectMotion(speed = SubjectSpeed.NORMAL, intensity = 5, motionType = MotionType.CONTINUOUS)
        assertNull(validateSpeedIntensity(motion))
    }

    // --- validateMotionBlur ---

    @Test
    fun `slow speed with extreme blur warns`() {
        val issue = validateMotionBlur(SubjectSpeed.SLOW, MotionBlurAmount.EXTREME)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `hyperkinetic speed with no blur warns`() {
        val issue = validateMotionBlur(SubjectSpeed.HYPERKINETIC, MotionBlurAmount.NONE)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `normal speed with cinematic blur is valid`() {
        assertNull(validateMotionBlur(SubjectSpeed.NORMAL, MotionBlurAmount.CINEMATIC))
    }
}
