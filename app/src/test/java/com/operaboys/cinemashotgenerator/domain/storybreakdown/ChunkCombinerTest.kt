package com.operaboys.cinemashotgenerator.domain.storybreakdown

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChunkCombinerTest {

    // --- isPartialResponse ---

    @Test
    fun `isPartialResponse is true when the chunk ends with CONTINUE marker`() {
        assertTrue(isPartialResponse("some response text [CONTINUE]"))
    }

    @Test
    fun `isPartialResponse ignores trailing whitespace after the CONTINUE marker`() {
        assertTrue(isPartialResponse("some response text [CONTINUE]   \n"))
    }

    @Test
    fun `isPartialResponse is false when the chunk does not end with CONTINUE marker`() {
        assertFalse(isPartialResponse("a complete response ending normally."))
    }

    // --- smartCombineChunks ---

    @Test
    fun `smartCombineChunks returns the single chunk unchanged when there is only one`() {
        assertEquals("a complete response.", smartCombineChunks(listOf("a complete response.")))
    }

    @Test
    fun `smartCombineChunks joins multiple chunks in list order`() {
        val result = smartCombineChunks(listOf("first part", " second part", " third part"))
        assertEquals("first part second part third part", result)
    }

    @Test
    fun `smartCombineChunks removes the CONTINUE marker from every chunk that has one`() {
        val result = smartCombineChunks(listOf("first part [CONTINUE]", "second part [CONTINUE]", "third part"))
        assertEquals("first partsecond partthird part", result)
        assertFalse(result.contains("[CONTINUE]"))
    }

    // --- Rule 6: تکه‌ی آخر ناقص/کامل ---

    @Test
    fun `validateChunksComplete warns when the last chunk still ends with CONTINUE`() {
        val issue = validateChunksComplete(listOf("first part [CONTINUE]", "second part [CONTINUE]"))
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `validateChunksComplete is null when the last chunk is complete`() {
        val issue = validateChunksComplete(listOf("first part [CONTINUE]", "final part."))
        assertNull(issue)
    }

    @Test
    fun `validateChunksComplete is null for an empty chunk list`() {
        assertNull(validateChunksComplete(emptyList()))
    }
}
