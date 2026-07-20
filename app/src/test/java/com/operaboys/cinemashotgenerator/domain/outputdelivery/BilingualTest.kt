package com.operaboys.cinemashotgenerator.domain.outputdelivery

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BilingualTest {

    private val translations = mapOf(
        Language.FA to mapOf("common.save" to "ذخیره"),
        Language.EN to mapOf("common.save" to "Save", "common.cancel" to "Cancel")
    )

    // --- t() ---

    @Test
    fun `t returns the value from the requested ui language when present`() {
        assertEquals("ذخیره", t("common.save", Language.FA, translations))
    }

    @Test
    fun `t falls back to english when the key is missing in the ui language`() {
        assertEquals("Cancel", t("common.cancel", Language.FA, translations))
    }

    @Test
    fun `t returns the key itself when missing from both languages`() {
        assertEquals("common.missing", t("common.missing", Language.FA, translations))
    }

    // --- validateTranslationCoverage ---

    @Test
    fun `validateTranslationCoverage is empty when key sets match`() {
        val result = validateTranslationCoverage(setOf("a", "b"), setOf("a", "b"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `validateTranslationCoverage reports keys missing on either side`() {
        val result = validateTranslationCoverage(setOf("a", "b"), setOf("b", "c"))
        assertEquals(2, result.size)
        assertTrue(result.any { it.contains("'a'") && it.contains("en.json") })
        assertTrue(result.any { it.contains("'c'") && it.contains("fa.json") })
    }

    // --- validateTranslationKeyFound ---

    @Test
    fun `validateTranslationKeyFound is null when key exists`() {
        assertNull(validateTranslationKeyFound("common.save", Language.FA, translations))
    }

    @Test
    fun `validateTranslationKeyFound warns when key is missing from both languages`() {
        val issue = validateTranslationKeyFound("common.missing", Language.FA, translations)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    // --- validateLanguageSupported ---

    @Test
    fun `validateLanguageSupported is null for a supported code`() {
        assertNull(validateLanguageSupported("fa"))
        assertNull(validateLanguageSupported("EN"))
    }

    @Test
    fun `validateLanguageSupported is blocking for an unsupported code`() {
        val issue = validateLanguageSupported("fr")
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }
}
