package com.operaboys.cinemashotgenerator.domain.visualidentity

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CinematicLanguageTest {

    // --- resolveEffectiveMode ---

    @Test
    fun `resolveEffectiveMode uses scene override when present`() {
        val settings = CinematicLanguageSettings(
            globalMode = CinematicMode.BALANCED,
            sceneOverrides = mapOf("scene_003" to CinematicMode.FAST_CUT)
        )
        assertEquals(CinematicMode.FAST_CUT, resolveEffectiveMode(settings, "scene_003"))
    }

    @Test
    fun `resolveEffectiveMode falls back to global mode when no override`() {
        val settings = CinematicLanguageSettings(globalMode = CinematicMode.LONG_TAKE)
        assertEquals(CinematicMode.LONG_TAKE, resolveEffectiveMode(settings, "scene_099"))
    }

    // --- determineHybridPacing ---

    @Test
    fun `determineHybridPacing action scene is fast cut`() {
        assertEquals(CinematicMode.FAST_CUT, determineHybridPacing("action", avgBeatIntensity = 5f))
    }

    @Test
    fun `determineHybridPacing high intensity is fast cut regardless of scene type`() {
        assertEquals(CinematicMode.FAST_CUT, determineHybridPacing("dialogue", avgBeatIntensity = 8f))
    }

    @Test
    fun `determineHybridPacing emotional scene is long take`() {
        assertEquals(CinematicMode.LONG_TAKE, determineHybridPacing("emotional", avgBeatIntensity = 5f))
    }

    @Test
    fun `determineHybridPacing dialogue scene is balanced`() {
        assertEquals(CinematicMode.BALANCED, determineHybridPacing("dialogue", avgBeatIntensity = 5f))
    }

    @Test
    fun `determineHybridPacing unknown scene type defaults to balanced`() {
        assertEquals(CinematicMode.BALANCED, determineHybridPacing("unknown", avgBeatIntensity = 5f))
    }

    // --- getPacingFromEmotion ---

    @Test
    fun `getPacingFromEmotion tense is fast cut`() {
        assertEquals(CinematicMode.FAST_CUT, getPacingFromEmotion("tense"))
    }

    @Test
    fun `getPacingFromEmotion melancholy is long take`() {
        assertEquals(CinematicMode.LONG_TAKE, getPacingFromEmotion("melancholy"))
    }

    @Test
    fun `getPacingFromEmotion unmapped emotion defaults to balanced`() {
        assertEquals(CinematicMode.BALANCED, getPacingFromEmotion("curious"))
    }

    // --- Rule 1 (Blocking): پارس مقدار خام ---

    @Test
    fun `rule1 parses all three valid raw values`() {
        assertEquals(CinematicMode.LONG_TAKE, parseCinematicMode("long_take").getOrThrow())
        assertEquals(CinematicMode.FAST_CUT, parseCinematicMode("fast_cut").getOrThrow())
        assertEquals(CinematicMode.BALANCED, parseCinematicMode("hybrid").getOrThrow())
    }

    @Test
    fun `rule1 invalid raw value fails`() {
        val result = parseCinematicMode("slow_motion")
        assertTrue(result.isFailure)
    }

    // --- Rule 2 (Warning): مدت شات خارج از محدوده ---

    @Test
    fun `rule2 duration within long take range is valid`() {
        assertNull(validateShotDurationForCinematicMode(15f, CinematicMode.LONG_TAKE))
    }

    @Test
    fun `rule2 duration too short for long take warns`() {
        val issue = validateShotDurationForCinematicMode(3f, CinematicMode.LONG_TAKE)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `rule2 duration too long for fast cut warns`() {
        val issue = validateShotDurationForCinematicMode(10f, CinematicMode.FAST_CUT)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `rule2 duration within balanced range is valid`() {
        assertNull(validateShotDurationForCinematicMode(10f, CinematicMode.BALANCED))
    }
}
