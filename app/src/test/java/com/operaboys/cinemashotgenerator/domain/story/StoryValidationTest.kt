package com.operaboys.cinemashotgenerator.domain.story

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryValidationTest {

    private fun context(
        genre: List<Genre> = listOf(Genre.SCI_FI, Genre.DRAMA),
        moodPrimary: Mood = Mood.DARK,
        completionStatus: CompletionStatus = CompletionStatus.COMPLETE
    ) = StoryContext(
        storyType = StoryType.NARRATIVE,
        genre = genre,
        moodPrimary = moodPrimary,
        createdAt = "2026-01-26T10:30:00Z",
        completionStatus = completionStatus
    )

    // --- Rule 1 (Blocking): فیلد الزامی خالی ---

    @Test
    fun `rule1 empty genre is blocking`() {
        val result = validateStoryContext(context(genre = emptyList()))

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.field == "genre" && it.severity == RuleSeverity.BLOCKING })
    }

    @Test
    fun `rule1 filled context is valid`() {
        val result = validateStoryContext(context())

        assertTrue(result.valid)
        assertTrue(result.errors.isEmpty())
    }

    // --- Rule 2 (Blocking): محدودیت Documentary ---

    @Test
    fun `rule2 documentary with disallowed genre is blocking`() {
        val result = validateStoryContext(context(genre = listOf(Genre.DOCUMENTARY, Genre.ACTION)))

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.field == "genre" && it.severity == RuleSeverity.BLOCKING })
    }

    @Test
    fun `rule2 documentary with drama is valid`() {
        val result = validateStoryContext(context(genre = listOf(Genre.DOCUMENTARY, Genre.DRAMA)))

        assertTrue(result.valid)
    }

    @Test
    fun `rule2 documentary alone is valid`() {
        val result = validateStoryContext(context(genre = listOf(Genre.DOCUMENTARY)))

        assertTrue(result.valid)
    }

    // --- Rule 3 (Warning): ترکیب غیرمعمول، هرگز Blocking ---

    @Test
    fun `rule3 horror plus hopeful warns but does not block`() {
        val result = validateStoryContext(
            context(genre = listOf(Genre.HORROR), moodPrimary = Mood.HOPEFUL)
        )

        assertTrue(result.valid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.any { it.field == "mood" && it.severity == RuleSeverity.WARNING })
    }

    @Test
    fun `rule3 normal combination has no warnings`() {
        val result = validateStoryContext(
            context(genre = listOf(Genre.HORROR), moodPrimary = Mood.DARK)
        )

        assertTrue(result.valid)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `checkMoodGenreCompatibility flags only unusual pairs`() {
        assertNotNull(checkMoodGenreCompatibility(Genre.HORROR, Mood.HOPEFUL))
        assertNotNull(checkMoodGenreCompatibility(Genre.ROMANCE, Mood.DARK))
        assertNull(checkMoodGenreCompatibility(Genre.HORROR, Mood.DARK))
        assertNull(checkMoodGenreCompatibility(Genre.SCI_FI, Mood.HOPEFUL))
    }

    // --- Rule 4: deriveCompletionStatus ---

    @Test
    fun `rule4 all required fields filled derives complete`() {
        assertEquals(CompletionStatus.COMPLETE, deriveCompletionStatus(context()))
    }

    @Test
    fun `rule4 empty genre derives partial`() {
        assertEquals(CompletionStatus.PARTIAL, deriveCompletionStatus(context(genre = emptyList())))
    }
}
