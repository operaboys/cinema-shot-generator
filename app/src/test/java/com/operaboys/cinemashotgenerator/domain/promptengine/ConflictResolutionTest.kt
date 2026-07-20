package com.operaboys.cinemashotgenerator.domain.promptengine

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictResolutionTest {

    @Test
    fun `only warning issues are counted and returned`() {
        val issues = listOf(
            ValidationIssue(Severity.WARNING, message = "w1"),
            ValidationIssue(Severity.BLOCKING, message = "b1"),
            ValidationIssue(Severity.WARNING, message = "w2")
        )

        val summary = summarizeConflictResolution(issues)

        assertEquals(2, summary.conflictsResolved)
        assertEquals(2, summary.warnings.size)
        assertTrue(summary.warnings.all { it.severity == Severity.WARNING })
    }

    @Test
    fun `no issues produces an empty summary`() {
        val summary = summarizeConflictResolution(emptyList())

        assertEquals(0, summary.conflictsResolved)
        assertTrue(summary.warnings.isEmpty())
    }
}
