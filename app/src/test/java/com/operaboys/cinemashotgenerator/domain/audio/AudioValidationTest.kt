package com.operaboys.cinemashotgenerator.domain.audio

import com.operaboys.cinemashotgenerator.domain.shot.ActionSound
import com.operaboys.cinemashotgenerator.domain.shot.CharacterSound
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioValidationTest {

    // --- Rule 1: تعداد کل لایه‌های صوتی ---

    @Test
    fun `eight or fewer sound layers is valid`() {
        val context = AudioContext(
            audioContextId = "audio_001",
            shotId = "shot_001",
            ambientSounds = List(3) { AmbientSound("rain", "medium", "d", "weather") },
            actionSounds = List(3) { ActionSound(1f, "footstep", "d") },
            characterSounds = List(2) { CharacterSound("char_001", "breathing", "d") }
        )

        assertNull(checkTotalSoundLayerCount(context))
    }

    @Test
    fun `more than eight sound layers warns`() {
        val context = AudioContext(
            audioContextId = "audio_001",
            shotId = "shot_001",
            ambientSounds = List(4) { AmbientSound("rain", "medium", "d", "weather") },
            actionSounds = List(3) { ActionSound(1f, "footstep", "d") },
            characterSounds = List(2) { CharacterSound("char_001", "breathing", "d") }
        )

        val issue = checkTotalSoundLayerCount(context)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    // --- Rule 2: Action Sound خارج از Duration شات ---

    @Test
    fun `action sounds within shot duration are valid`() {
        val sounds = listOf(ActionSound(0f, "footstep", "d"), ActionSound(3f, "footstep", "d"))
        val issues = validateActionSoundTimeline(sounds, durationSeconds = 4f)

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `action sound outside shot duration blocks`() {
        val sounds = listOf(ActionSound(0f, "footstep", "d"), ActionSound(10f, "footstep", "d"))
        val issues = validateActionSoundTimeline(sounds, durationSeconds = 4f)

        assertEquals(1, issues.size)
        assertEquals(Severity.BLOCKING, issues[0].severity)
    }
}
