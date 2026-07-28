package com.operaboys.cinemashotgenerator.domain.storybreakdown

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AiConnectorTest {

    // --- BUILTIN_AI_CONNECTOR_PROFILES (Option A، ADR-035) ---

    @Test
    fun `BUILTIN_AI_CONNECTOR_PROFILES is empty in this step, deferred to a future step per ADR-035`() {
        assertTrue(BUILTIN_AI_CONNECTOR_PROFILES.isEmpty())
    }

    // --- createCustomAiConnectorProfile ---

    @Test
    fun `createCustomAiConnectorProfile builds a profile with all provided fields`() {
        val profile = createCustomAiConnectorProfile(
            displayName = "Claude API",
            endpointUrl = "https://api.anthropic.com/v1/messages",
            requestBodyTemplate = """{"prompt": "{{PROMPT}}"}""",
            responseJsonPath = "content[0].text"
        )
        assertTrue(profile.profileId.isNotBlank())
        assertEquals("Claude API", profile.displayName)
        assertEquals("https://api.anthropic.com/v1/messages", profile.endpointUrl)
        assertEquals("""{"prompt": "{{PROMPT}}"}""", profile.requestBodyTemplate)
        assertEquals("content[0].text", profile.responseJsonPath)
        assertTrue(profile.requestHeaders.isEmpty())
    }

    @Test
    fun `createCustomAiConnectorProfile generates a distinct profileId on each call`() {
        val first = createCustomAiConnectorProfile("A", "https://a.example.com", "{}", "text")
        val second = createCustomAiConnectorProfile("A", "https://a.example.com", "{}", "text")
        assertTrue(first.profileId != second.profileId)
    }

    // --- Rule 4: apiKey خالی ---

    @Test
    fun `rule4 empty apiKey is blocking`() {
        val issue = validateApiKeyProvided("")
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `rule4 blank apiKey is blocking`() {
        val issue = validateApiKeyProvided("   ")
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `rule4 non-empty apiKey is valid`() {
        assertNull(validateApiKeyProvided("sk-real-key-123"))
    }

    // --- sendToAiConnector (Option A: TODO عمدی، نه باگ خاموش) ---

    @Test
    fun `sendToAiConnector throws NotImplementedError since real HTTP is deferred per ADR-035 (Option A)`() {
        val profile = createCustomAiConnectorProfile(
            displayName = "Test API",
            endpointUrl = "https://example.com/api",
            requestBodyTemplate = """{"prompt": "{{PROMPT}}"}""",
            responseJsonPath = "content[0].text"
        )
        val exception = assertThrows(NotImplementedError::class.java) {
            runBlocking { sendToAiConnector(profile, apiKey = "key123", prompt = "test prompt") }
        }
        assertTrue(exception.message!!.contains("Ktor"))
    }

    // --- Rule 5: پیام خطای واقعی (طبق ساختار Result، Option A) ---

    @Test
    fun `rule5 is null for a successful result`() {
        assertNull(validateAiConnectorErrorMessage(Result.success("ok response")))
    }

    @Test
    fun `rule5 includes the real exception message when the result is a failure`() {
        val result = Result.failure<String>(IllegalStateException("HTTP 429 Too Many Requests"))
        val issue = validateAiConnectorErrorMessage(result)
        assertEquals(Severity.BLOCKING, issue!!.severity)
        assertTrue(issue.message.contains("HTTP 429 Too Many Requests"))
    }

    @Test
    fun `rule5 falls back to a clear notice when the failure has no message`() {
        val result = Result.failure<String>(RuntimeException())
        val issue = validateAiConnectorErrorMessage(result)
        assertEquals(Severity.BLOCKING, issue!!.severity)
        assertFalse(issue.message.isBlank())
    }
}
