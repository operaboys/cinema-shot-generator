package com.operaboys.cinemashotgenerator.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationEngineTest {

    // --- validateDataCompleteness ---

    @Test
    fun `short shot description is blocking`() {
        val issues = validateDataCompleteness(
            shotDescription = "short",
            characterIds = listOf("char_001"),
            objectIds = emptyList()
        )

        assertTrue(issues.any { it.severity == Severity.BLOCKING && it.field == "shot_description" })
    }

    @Test
    fun `complete shot description with subjects has no issues`() {
        val issues = validateDataCompleteness(
            shotDescription = "A detective walks into a dimly lit office",
            characterIds = listOf("char_001"),
            objectIds = emptyList()
        )

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `shot without any subject warns but does not block`() {
        val issues = validateDataCompleteness(
            shotDescription = "A wide landscape shot of the mountains at dawn",
            characterIds = emptyList(),
            objectIds = emptyList()
        )

        assertEquals(1, issues.size)
        assertEquals(Severity.WARNING, issues[0].severity)
    }

    // --- validateLogicConsistency ---

    @Test
    fun `rain plus fire plus outdoor warns`() {
        val issues = validateLogicConsistency(
            weather = "rain",
            lightingMotivation = "fire",
            locationType = "outdoor"
        )

        assertEquals(1, issues.size)
        assertEquals(Severity.WARNING, issues[0].severity)
    }

    @Test
    fun `rain plus fire indoors has no conflict`() {
        val issues = validateLogicConsistency(
            weather = "rain",
            lightingMotivation = "fire",
            locationType = "indoor"
        )

        assertTrue(issues.isEmpty())
    }

    // --- validateBeatSheetTimeline ---

    @Test
    fun `beat within duration range is valid`() {
        val issues = validateBeatSheetTimeline(
            beatTimestampsSeconds = listOf(0f, 2.5f, 5f),
            durationSeconds = 10f
        )

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `beat outside duration range blocks`() {
        val issues = validateBeatSheetTimeline(
            beatTimestampsSeconds = listOf(2f, 15f, -1f),
            durationSeconds = 10f
        )

        assertEquals(2, issues.size)
        assertTrue(issues.all { it.severity == Severity.BLOCKING })
    }
}
