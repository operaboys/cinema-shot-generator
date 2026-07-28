package com.operaboys.cinemashotgenerator.domain.sceneconditions

import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LightingValidationTest {

    // --- نور خورشید + شب ---

    @Test
    fun `sunlight at night blocks`() {
        val issue = checkSunlightAtNight(LightingMotivation.SUNLIGHT, TimeOfDay.NIGHT)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `sunlight at noon is valid`() {
        assertNull(checkSunlightAtNight(LightingMotivation.SUNLIGHT, TimeOfDay.NOON))
    }

    // --- نور ماه + ظهر ---

    @Test
    fun `moonlight at noon blocks`() {
        val issue = checkMoonlightAtNoon(LightingMotivation.MOONLIGHT, TimeOfDay.NOON)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `moonlight at night is valid`() {
        assertNull(checkMoonlightAtNoon(LightingMotivation.MOONLIGHT, TimeOfDay.NIGHT))
    }

    // --- Noir + Contrast غیر High ---

    @Test
    fun `noir with medium contrast warns`() {
        val issue = checkNoirWithoutHighContrast(LightingStyle.LOW_KEY, ContrastRatio.MEDIUM)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `noir with high contrast is valid`() {
        assertNull(checkNoirWithoutHighContrast(LightingStyle.LOW_KEY, ContrastRatio.HIGH))
    }

    // --- Fill قوی + Dramatic/Noir ---

    @Test
    fun `strong fill with dramatic style warns`() {
        val issue = checkStrongFillWithDramaticStyle(FillLight.STRONG, LightingStyle.DRAMATIC_LIGHT)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `strong fill with noir style warns`() {
        val issue = checkStrongFillWithDramaticStyle(FillLight.STRONG, LightingStyle.LOW_KEY)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `strong fill with natural style is valid`() {
        assertNull(checkStrongFillWithDramaticStyle(FillLight.STRONG, LightingStyle.NATURAL_LIGHT))
    }

    // --- نور از پایین ---

    @Test
    fun `bottom key light warns`() {
        val issue = checkBottomKeyLight(KeyLightPosition.BOTTOM)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `side key light is valid`() {
        assertNull(checkBottomKeyLight(KeyLightPosition.SIDE))
    }
}
