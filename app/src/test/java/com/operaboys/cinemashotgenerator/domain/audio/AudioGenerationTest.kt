package com.operaboys.cinemashotgenerator.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioGenerationTest {

    // --- generateWeatherSounds ---

    @Test
    fun `rain produces a single ambient sound with weather source`() {
        val sounds = generateWeatherSounds("rain", "heavy")
        assertEquals(1, sounds.size)
        assertEquals("rain", sounds[0].type)
        assertEquals("weather", sounds[0].source)
    }

    @Test
    fun `storm produces rain and thunder sounds`() {
        val sounds = generateWeatherSounds("storm", "heavy")
        assertEquals(2, sounds.size)
        assertTrue(sounds.any { it.type == "rain" })
        assertTrue(sounds.any { it.type == "thunder" })
    }

    @Test
    fun `fog produces an ambient sound`() {
        val sounds = generateWeatherSounds("fog", "medium")
        assertEquals(1, sounds.size)
        assertEquals("ambient", sounds[0].type)
    }

    @Test
    fun `unknown weather type produces no sounds`() {
        val sounds = generateWeatherSounds("sunny", "light")
        assertTrue(sounds.isEmpty())
    }

    // --- generateActionSounds ---

    @Test
    fun `walk in description produces footstep sound`() {
        val sounds = generateActionSounds("The detective walks into the room", "wet")
        assertEquals(1, sounds.size)
        assertEquals("footstep", sounds[0].type)
        assertTrue(sounds[0].description.contains("wet"))
    }

    @Test
    fun `run in description produces footstep sound`() {
        val sounds = generateActionSounds("She runs across the street", "dry")
        assertEquals(1, sounds.size)
        assertEquals("footstep", sounds[0].type)
    }

    @Test
    fun `description without walk or run produces no action sounds`() {
        val sounds = generateActionSounds("A quiet conversation over coffee", "dry")
        assertTrue(sounds.isEmpty())
    }

    // --- suggestBreathingSounds ---

    @Test
    fun `calm activity with no emotion suggests calm breathing`() {
        val suggestion = suggestBreathingSounds("standing", emotion = null)
        assertEquals("low", suggestion.intensity)
        assertEquals("calm breathing", suggestion.description)
    }

    @Test
    fun `running activity suggests heavy breathing`() {
        val suggestion = suggestBreathingSounds("running", emotion = null)
        assertEquals("heavy", suggestion.intensity)
    }

    @Test
    fun `fighting activity suggests heavy breathing`() {
        val suggestion = suggestBreathingSounds("fighting", emotion = null)
        assertEquals("heavy", suggestion.intensity)
    }

    @Test
    fun `terrified emotion overrides to panicked breathing`() {
        val suggestion = suggestBreathingSounds("standing", emotion = "terrified")
        assertEquals("high", suggestion.intensity)
        assertEquals("rapid panicked breathing", suggestion.description)
    }

    @Test
    fun `panicked emotion during running still overrides to panicked breathing`() {
        val suggestion = suggestBreathingSounds("running", emotion = "panicked")
        assertEquals("high", suggestion.intensity)
    }

    // --- generateAudioContext ---

    @Test
    fun `generateAudioContext always returns empty character sounds`() {
        val context = generateAudioContext(
            shotId = "shot_001",
            shotDescription = "A detective walks through the rain",
            weatherType = "rain",
            weatherIntensity = "heavy",
            groundState = "wet"
        )
        assertTrue(context.characterSounds.isEmpty())
    }

    @Test
    fun `generateAudioContext combines weather and action sounds`() {
        val context = generateAudioContext(
            shotId = "shot_001",
            shotDescription = "A detective walks through the rain",
            weatherType = "rain",
            weatherIntensity = "heavy",
            groundState = "wet",
            idProvider = { "audio_test0001" }
        )
        assertEquals("audio_test0001", context.audioContextId)
        assertEquals("shot_001", context.shotId)
        assertEquals(1, context.ambientSounds.size)
        assertEquals(1, context.actionSounds.size)
    }
}
