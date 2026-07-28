package com.operaboys.cinemashotgenerator.domain.storybreakdown

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonDoctorTest {

    private val validJson =
        """{"characters": [{"name": "John"}], "locations": [{"name": "Office"}], "shots": [{"sceneName": "S1"}]}"""

    private val trailingCommaJson = """{"characters": [], "locations": [], "shots": [],}"""

    private val smartQuotesJson = """{“characters”: [], "locations": [], "shots": []}"""

    private val unmatchedBracketJson =
        """{"characters": [{"name": "John"}], "locations": [{"name": "Office"}], "shots": [{"sceneName": "S1"}]"""

    private val incompleteJson = """{"characters": [{"name": "Detective John", "desc"""

    private val missingShotsJson = """{"characters": [], "locations": []}"""

    private val garbageText = "این یک متن کاملاً غیر JSON است و هیچ ساختار خاصی ندارد."

    // --- سناریوی کامل: JSON کاملاً معتبر ---

    @Test
    fun `fully valid JSON needs no repair and triggers neither Rule 7 nor Rule 8`() {
        val result = repairJson(validJson)
        assertTrue(result is JsonRepairResult.Success)
        assertFalse((result as JsonRepairResult.Success).wasAutoFixed)

        assertNull(validateJsonRepairResult(result))
        assertNull(validateRequiredKeysPresent(validJson))
    }

    // --- TRAILING_COMMA ---

    @Test
    fun `diagnoseJsonError detects a trailing comma as auto-fixable`() {
        val diagnosis = diagnoseJsonError(trailingCommaJson, "parser error")
        assertEquals(JsonErrorType.TRAILING_COMMA, diagnosis.errorType)
        assertTrue(diagnosis.autoFixable)
    }

    @Test
    fun `repairJson auto-fixes a trailing comma`() {
        val result = repairJson(trailingCommaJson)
        assertTrue(result is JsonRepairResult.Success)
        val success = result as JsonRepairResult.Success
        assertTrue(success.wasAutoFixed)
        assertFalse(success.repairedJson.contains(",}"))
    }

    // --- SMART_QUOTES ---

    @Test
    fun `diagnoseJsonError detects smart quotes as auto-fixable`() {
        val diagnosis = diagnoseJsonError(smartQuotesJson, "parser error")
        assertEquals(JsonErrorType.SMART_QUOTES, diagnosis.errorType)
        assertTrue(diagnosis.autoFixable)
    }

    @Test
    fun `repairJson auto-fixes smart quotes`() {
        val result = repairJson(smartQuotesJson)
        assertTrue(result is JsonRepairResult.Success)
        val success = result as JsonRepairResult.Success
        assertTrue(success.wasAutoFixed)
        assertFalse(success.repairedJson.contains('“'))
    }

    // --- UNMATCHED_BRACKET (not auto-fixable) ---

    @Test
    fun `diagnoseJsonError detects an unmatched bracket as not auto-fixable`() {
        val diagnosis = diagnoseJsonError(unmatchedBracketJson, "parser error")
        assertEquals(JsonErrorType.UNMATCHED_BRACKET, diagnosis.errorType)
        assertFalse(diagnosis.autoFixable)
    }

    @Test
    fun `repairJson needs manual repair for an unmatched bracket`() {
        val result = repairJson(unmatchedBracketJson)
        assertTrue(result is JsonRepairResult.NeedsManualRepair)
        assertEquals(
            JsonErrorType.UNMATCHED_BRACKET,
            (result as JsonRepairResult.NeedsManualRepair).diagnosis.errorType
        )
    }

    @Test
    fun `validateJsonRepairResult is blocking when manual repair is needed`() {
        val result = repairJson(unmatchedBracketJson)
        val issue = validateJsonRepairResult(result)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    // --- INCOMPLETE_RESPONSE ---

    @Test
    fun `diagnoseJsonError detects an incomplete response cut off mid-string`() {
        val diagnosis = diagnoseJsonError(incompleteJson, "parser error")
        assertEquals(JsonErrorType.INCOMPLETE_RESPONSE, diagnosis.errorType)
        assertFalse(diagnosis.autoFixable)
    }

    // --- Rule 8: کلید الزامی غایب ---

    @Test
    fun `validateRequiredKeysPresent is blocking when shots key is missing from otherwise valid JSON`() {
        val issue = validateRequiredKeysPresent(missingShotsJson)
        assertEquals(Severity.BLOCKING, issue!!.severity)
        assertTrue(issue.message.contains("shots"))
    }

    @Test
    fun `validateRequiredKeysPresent is null when JSON itself is invalid (Rule 7 covers that case instead)`() {
        assertNull(validateRequiredKeysPresent(unmatchedBracketJson))
    }

    // --- UNKNOWN: متن کاملاً غیرمرتبط، نه یک JSON بریده‌شده ---

    @Test
    fun `diagnoseJsonError falls back to UNKNOWN for text with no recognizable JSON pattern`() {
        val diagnosis = diagnoseJsonError(garbageText, "parser error")
        assertEquals(JsonErrorType.UNKNOWN, diagnosis.errorType)
        assertFalse(diagnosis.autoFixable)
        assertNull(diagnosis.approximateLine)
    }

    @Test
    fun `attemptAutoFix returns null for UNMATCHED_BRACKET and UNKNOWN`() {
        val bracketDiagnosis = diagnoseJsonError(unmatchedBracketJson, "x")
        val unknownDiagnosis = diagnoseJsonError(garbageText, "x")
        assertNull(attemptAutoFix(unmatchedBracketJson, bracketDiagnosis))
        assertNull(attemptAutoFix(garbageText, unknownDiagnosis))
    }
}
