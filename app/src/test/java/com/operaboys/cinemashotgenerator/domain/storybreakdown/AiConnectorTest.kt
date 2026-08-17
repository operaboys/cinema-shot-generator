package com.operaboys.cinemashotgenerator.domain.storybreakdown

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.content.OutgoingContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiConnectorTest {

    // --- BUILTIN_AI_CONNECTOR_PROFILES (G2 قدم ۲، ADR-100: اولین پروفایل واقعی) ---

    @Test
    fun `BUILTIN_AI_CONNECTOR_PROFILES contains exactly the Claude API profile, matching the official Messages API format`() {
        assertEquals(listOf(CLAUDE_API_PROFILE), BUILTIN_AI_CONNECTOR_PROFILES)
        assertEquals("https://api.anthropic.com/v1/messages", CLAUDE_API_PROFILE.endpointUrl)
        assertEquals("content[0].text", CLAUDE_API_PROFILE.responseJsonPath)
        assertEquals("2023-06-01", CLAUDE_API_PROFILE.requestHeaders["anthropic-version"])
        assertEquals("{{API_KEY}}", CLAUDE_API_PROFILE.requestHeaders["x-api-key"])
        assertTrue(CLAUDE_API_PROFILE.requestBodyTemplate.contains("{{PROMPT}}"))
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

    // --- sendToAiConnector (G2 قدم ۲، ADR-100: HTTP واقعی با Ktor؛ MockEngine در تست، بدون تماس واقعی اینترنت) ---

    private val claudeLikeProfile = createCustomAiConnectorProfile(
        displayName = "Test API",
        endpointUrl = "https://example.com/api",
        requestBodyTemplate = """{"model":"test-model","max_tokens":1024,"messages":[{"role":"user","content":"{{PROMPT}}"}]}""",
        responseJsonPath = "content[0].text"
    ).copy(requestHeaders = mapOf("x-api-key" to "{{API_KEY}}"))

    @Test
    fun `a successful response extracts the correct text via responseJsonPath`() = runBlocking {
        val engine = MockEngine { request ->
            respond(
                content = """{"content":[{"type":"text","text":"Hello from AI"}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = sendToAiConnector(claudeLikeProfile, apiKey = "sk-test-123", prompt = "hi", engine = engine)

        assertTrue(result.isSuccess)
        assertEquals("Hello from AI", result.getOrNull())
    }

    @Test
    fun `requestBodyTemplate and headers actually receive the substituted prompt and apiKey`() = runBlocking {
        var capturedBody: String? = null
        var capturedApiKeyHeader: String? = null
        val engine = MockEngine { request ->
            capturedBody = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            capturedApiKeyHeader = request.headers["x-api-key"]
            respond(
                content = """{"content":[{"type":"text","text":"ok"}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        sendToAiConnector(claudeLikeProfile, apiKey = "sk-secret-key", prompt = "write a story", engine = engine)

        assertTrue(capturedBody!!.contains("write a story"))
        assertEquals("sk-secret-key", capturedApiKeyHeader)
    }

    @Test
    fun `a 401 response with an error message body produces a Result failure with a meaningful message`() = runBlocking {
        val engine = MockEngine { request ->
            respond(
                content = """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = sendToAiConnector(claudeLikeProfile, apiKey = "wrong-key", prompt = "hi", engine = engine)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("invalid x-api-key"))
    }

    @Test
    fun `a 429 rate limit response also produces a Result failure with the real status and message`() = runBlocking {
        val engine = MockEngine { request ->
            respond(
                content = """{"type":"error","error":{"type":"rate_limit_error","message":"Rate limited"}}""",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = sendToAiConnector(claudeLikeProfile, apiKey = "sk-test", prompt = "hi", engine = engine)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Rate limited"))
    }

    @Test
    fun `an unexpected JSON shape where responseJsonPath cannot be resolved fails cleanly, not a crash`() = runBlocking {
        val engine = MockEngine { request ->
            respond(
                content = """{"unexpected":"shape"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = sendToAiConnector(claudeLikeProfile, apiKey = "sk-test", prompt = "hi", engine = engine)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("content[0].text"))
    }

    @Test
    fun `malformed non-JSON response body fails cleanly, not a crash`() = runBlocking {
        val engine = MockEngine { request ->
            respond(
                content = "not even json",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = sendToAiConnector(claudeLikeProfile, apiKey = "sk-test", prompt = "hi", engine = engine)

        assertTrue(result.isFailure)
        assertFalse(result.exceptionOrNull()!!.message.isNullOrBlank())
    }

    // یافته‌ی راستی‌آزمایی این قدم: requestTimeoutMillis واقعی (۶۰ ثانیه) داخل خودِ
    // sendToAiConnector هاردکد است (نه پارامتر تزریق‌پذیر جدا) — صبرکردن واقعی ۶۰
    // ثانیه در یک تست واحد غیرمنطقی بود. به‌جایش MockEngine مستقیماً همان Exception
    // واقعی‌ای که HttpTimeout در دنیای واقعی پرتاب می‌کند را شبیه‌سازی می‌کند —
    // این دقیقاً همان مسیر catch را در sendToAiConnector تست می‌کند (نه یک Timer
    // واقعی)، طبق قابلیت مستند MockEngine («throw هر Exception دلخواه از داخل
    // handler»).
    @Test
    fun `a network timeout (simulated via a thrown SocketTimeoutException) produces a Result failure with a meaningful message`() = runBlocking {
        val engine = MockEngine { throw java.net.SocketTimeoutException("timeout") }

        val result = sendToAiConnector(claudeLikeProfile, apiKey = "sk-test", prompt = "hi", engine = engine)

        assertTrue(result.isFailure)
        assertFalse(result.exceptionOrNull()!!.message.isNullOrBlank())
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
