package com.operaboys.cinemashotgenerator.domain.promptfinalization

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptCleanerTest {

    // --- detectConflicts ---

    @Test
    fun `detectConflicts finds a weather conflict`() {
        val result = detectConflicts("a sunny and rainy day")
        assertEquals(listOf("sunny" to "rainy"), result)
    }

    @Test
    fun `detectConflicts finds a logical conflict`() {
        val result = detectConflicts("the man is standing and sitting")
        assertEquals(listOf("standing" to "sitting"), result)
    }

    @Test
    fun `detectConflicts finds nothing in a consistent text`() {
        assertTrue(detectConflicts("a beautiful garden").isEmpty())
    }

    // --- removeSynonymDuplicates ---

    @Test
    fun `removeSynonymDuplicates keeps only the first synonym in a group`() {
        assertEquals("a beautiful garden", removeSynonymDuplicates("a beautiful pretty gorgeous garden"))
    }

    @Test
    fun `removeSynonymDuplicates leaves text unchanged when no group has duplicates`() {
        assertEquals("a fast car", removeSynonymDuplicates("a fast car"))
    }

    // --- cleanPrompt ---

    @Test
    fun `cleanPrompt resolves conflicts, redundancy, and stop words in a full scenario`() {
        val renderedText = "a sunny rainy day with a very very beautiful pretty scene that is just really nice"
        val (cleaned, report) = cleanPrompt(renderedText)

        assertEquals(1, report.conflictsDetected)
        assertEquals(listOf("sunny vs rainy"), report.conflictsResolved)
        assertEquals(1, report.redundancyRemoved)
        assertEquals(7, report.stopWordsRemoved)
        assertEquals(renderedText.length, report.originalLength)
        assertEquals(cleaned.length, report.cleanedLength)
        assertEquals("Sunny day with beautiful scene is nice.", cleaned)
    }

    @Test
    fun `cleanPrompt returns text unchanged when nothing needs cleaning`() {
        val renderedText = "Sunny day with beautiful scene."
        val (cleaned, report) = cleanPrompt(renderedText)

        assertEquals(renderedText, cleaned)
        assertEquals(0, report.conflictsDetected)
        assertEquals(0, report.redundancyRemoved)
        assertEquals(0, report.stopWordsRemoved)
    }

    // --- validateConflictsResolved ---

    @Test
    fun `validateConflictsResolved is null when the cleaned text has no remaining conflicts`() {
        val (cleaned, _) = cleanPrompt("a sunny rainy day")
        assertNull(validateConflictsResolved(cleaned))
    }

    @Test
    fun `validateConflictsResolved is blocking when a conflict remains in the text`() {
        val issue = validateConflictsResolved("a sunny rainy day")
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    // --- validateCompressionRatio ---

    @Test
    fun `validateCompressionRatio is null when reduction meets the threshold`() {
        val report = CleaningReport(
            conflictsDetected = 0, conflictsResolved = emptyList(), redundancyRemoved = 0,
            stopWordsRemoved = 0, originalLength = 100, cleanedLength = 60, compressionRatio = 0.4f
        )
        assertNull(validateCompressionRatio(report))
    }

    @Test
    fun `validateCompressionRatio warns when reduction is below the threshold`() {
        val report = CleaningReport(
            conflictsDetected = 0, conflictsResolved = emptyList(), redundancyRemoved = 0,
            stopWordsRemoved = 0, originalLength = 100, cleanedLength = 95, compressionRatio = 0.05f
        )
        val issue = validateCompressionRatio(report)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    // --- CleaningOptions.applyFinalPolish (ADR-026: برای Clean کردن مقادیر تکی فیلد JSON) ---

    @Test
    fun `cleanPrompt skips capitalization and terminal punctuation when applyFinalPolish is false`() {
        val (cleaned, _) = cleanPrompt("eye level, medium shot", CleaningOptions(applyFinalPolish = false))
        assertEquals("eye level, medium shot", cleaned)
    }

    @Test
    fun `cleanPrompt still applies final polish by default, unchanged`() {
        val (cleaned, _) = cleanPrompt("eye level, medium shot")
        assertEquals("Eye level, medium shot.", cleaned)
    }
}
