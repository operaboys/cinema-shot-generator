package com.operaboys.cinemashotgenerator.domain.story

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
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
        val issues = validateStoryContext(context(genre = emptyList()))

        assertTrue(issues.any { it.field == "genre" && it.severity == Severity.BLOCKING })
    }

    @Test
    fun `rule1 filled context is valid`() {
        val issues = validateStoryContext(context())

        assertTrue(issues.none { it.severity == Severity.BLOCKING })
    }

    // --- Rule 2 (Blocking): محدودیت Documentary ---

    @Test
    fun `rule2 documentary with disallowed genre is blocking`() {
        val issues = validateStoryContext(context(genre = listOf(Genre.DOCUMENTARY, Genre.ACTION)))

        assertTrue(issues.any { it.field == "genre" && it.severity == Severity.BLOCKING })
    }

    @Test
    fun `rule2 documentary with drama is valid`() {
        val issues = validateStoryContext(context(genre = listOf(Genre.DOCUMENTARY, Genre.DRAMA)))

        assertTrue(issues.none { it.severity == Severity.BLOCKING })
    }

    @Test
    fun `rule2 documentary alone is valid`() {
        val issues = validateStoryContext(context(genre = listOf(Genre.DOCUMENTARY)))

        assertTrue(issues.none { it.severity == Severity.BLOCKING })
    }

    // --- Rule 3 (Warning): ترکیب غیرمعمول، هرگز Blocking ---

    @Test
    fun `rule3 horror plus hopeful warns but does not block`() {
        val issues = validateStoryContext(
            context(genre = listOf(Genre.HORROR), moodPrimary = Mood.HOPEFUL)
        )

        assertTrue(issues.none { it.severity == Severity.BLOCKING })
        assertTrue(issues.any { it.field == "mood" && it.severity == Severity.WARNING })
    }

    @Test
    fun `rule3 normal combination has no warnings`() {
        val issues = validateStoryContext(
            context(genre = listOf(Genre.HORROR), moodPrimary = Mood.DARK)
        )

        assertTrue(issues.isEmpty())
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
