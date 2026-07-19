package com.operaboys.cinemashotgenerator.domain.shot

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShotValidationTest {

    // --- Rule 1 ---

    @Test
    fun `rule1 short description is blocking`() {
        val issue = validateShotDescription("short")
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `rule1 sufficient description is valid`() {
        assertNull(validateShotDescription("A detective walks into a dimly lit office"))
    }

    // --- Rule 2 ---

    @Test
    fun `rule2 no subjects at all is blocking`() {
        val issue = validateShotHasSubject(emptyList(), emptyList(), emptyList())
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `rule2 location only subject is valid`() {
        // طبق بلوپرینت ۰۵، Location هم یک Subject معتبر است، نه فقط Character/Object
        assertNull(validateShotHasSubject(emptyList(), emptyList(), listOf("loc_002")))
    }

    @Test
    fun `rule2 character subject is valid`() {
        assertNull(validateShotHasSubject(listOf("char_001"), emptyList(), emptyList()))
    }

    // --- Rule 3 ---

    @Test
    fun `rule3 beats within duration are valid`() {
        val beats = listOf(
            Beat(0.0f, BeatEventType.CAMERA_MOVE, "شروع dolly in"),
            Beat(3.0f, BeatEventType.ENVIRONMENTAL, "رعد و برق")
        )
        val issues = validateShotBeatTimeline(beats, durationSeconds = 4f)
        assertTrue(issues.isEmpty())
    }

    @Test
    fun `rule3 beat outside duration blocks`() {
        val beats = listOf(
            Beat(5.0f, BeatEventType.CAMERA_MOVE, "خارج از محدوده")
        )
        val issues = validateShotBeatTimeline(beats, durationSeconds = 4f)
        assertEquals(1, issues.size)
        assertEquals(Severity.BLOCKING, issues[0].severity)
    }

    // --- Rule 4 ---

    @Test
    fun `rule4 missing file is blocking`() {
        val issue = validateImageReferenceFile("/storage/missing.jpg", fileExists = { false })
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `rule4 existing file is valid`() {
        assertNull(validateImageReferenceFile("/storage/ref.jpg", fileExists = { true }))
    }
}
