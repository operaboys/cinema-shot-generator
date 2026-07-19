package com.operaboys.cinemashotgenerator.domain.sceneconditions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentSoundMappingTest {

    @Test
    fun `rain produces a rain sound with matching intensity`() {
        val sounds = mapEnvironmentToSound("rain", "heavy", "none")
        assertEquals(1, sounds.size)
        assertEquals("rain", sounds[0].type)
        assertEquals("heavy", sounds[0].intensity)
    }

    @Test
    fun `storm produces both rain and thunder sounds`() {
        val sounds = mapEnvironmentToSound("storm", "heavy", "none")
        assertEquals(2, sounds.size)
        assertTrue(sounds.any { it.type == "rain" })
        assertTrue(sounds.any { it.type == "thunder" })
    }

    @Test
    fun `snow produces a gentle wind sound`() {
        val sounds = mapEnvironmentToSound("snow", "light", "none")
        assertEquals(1, sounds.size)
        assertEquals("wind", sounds[0].type)
        assertEquals("low", sounds[0].intensity)
    }

    @Test
    fun `fog produces an ambient sound`() {
        val sounds = mapEnvironmentToSound("fog", "medium", "none")
        assertEquals(1, sounds.size)
        assertEquals("ambient", sounds[0].type)
    }

    @Test
    fun `wind adds an extra wind sound regardless of weather`() {
        val sounds = mapEnvironmentToSound("clear", "light", "strong")
        assertEquals(1, sounds.size)
        assertEquals("wind", sounds[0].type)
        assertEquals("high", sounds[0].intensity)
    }

    @Test
    fun `light wind produces medium intensity wind sound`() {
        val sounds = mapEnvironmentToSound("clear", "light", "light")
        assertEquals("medium", sounds[0].intensity)
    }
}
