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

    // --- BUILTIN_AI_CONNECTOR_PROFILES (ADR-100: اولین پروفایل؛ ADR-102: دومین؛ ADR-103: سومین) ---

    @Test
    fun `BUILTIN_AI_CONNECTOR_PROFILES contains exactly Claude, OpenAI, and Gemini, matching each service's official API format`() {
        assertEquals(listOf(CLAUDE_API_PROFILE, OPENAI_API_PROFILE, GEMINI_API_PROFILE), BUILTIN_AI_CONNECTOR_PROFILES)
        assertEquals("https://api.anthropic.com/v1/messages", CLAUDE_API_PROFILE.endpointUrl)
        assertEquals("content[0].text", CLAUDE_API_PROFILE.responseJsonPath)
        assertEquals("2023-06-01", CLAUDE_API_PROFILE.requestHeaders["anthropic-version"])
        assertEquals("{{API_KEY}}", CLAUDE_API_PROFILE.requestHeaders["x-api-key"])
        assertTrue(CLAUDE_API_PROFILE.requestBodyTemplate.contains("{{PROMPT}}"))
    }

    @Test
    fun `OPENAI_API_PROFILE matches OpenAI's official Chat Completions format, distinct from Claude's`() {
        assertEquals("https://api.openai.com/v1/chat/completions", OPENAI_API_PROFILE.endpointUrl)
        assertEquals("choices[0].message.content", OPENAI_API_PROFILE.responseJsonPath)
        assertEquals("Bearer {{API_KEY}}", OPENAI_API_PROFILE.requestHeaders["Authorization"])
        assertTrue(OPENAI_API_PROFILE.requestBodyTemplate.contains("{{PROMPT}}"))
        assertFalse(
            "برخلاف Claude، OpenAI از x-api-key استفاده نمی‌کند — یک تفاوت واقعی بین دو سرویس",
            OPENAI_API_PROFILE.requestHeaders.containsKey("x-api-key")
        )
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

    // --- OPENAI_API_PROFILE واقعی (G2/ADR-102: دومین پروفایل واقعی) — روی خودِ
    // OPENAI_API_PROFILE تولیدی تست می‌شود (نه یک Fixture مشابه)، تا فرمت واقعی
    // (Authorization: Bearer، نه x-api-key؛ choices[0].message.content، نه
    // content[0].text) واقعاً از انتها به انتها محک بخورد.

    @Test
    fun `a successful OpenAI response extracts the correct text via choices index 0 message content`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"choices":[{"message":{"role":"assistant","content":"Hello from GPT"}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = sendToAiConnector(OPENAI_API_PROFILE, apiKey = "sk-openai-test-123", prompt = "hi", engine = engine)

        assertTrue(result.isSuccess)
        assertEquals("Hello from GPT", result.getOrNull())
    }

    @Test
    fun `the real request to OpenAI carries an Authorization Bearer header, not x-api-key, with the real prompt and key`() = runBlocking {
        var capturedBody: String? = null
        var capturedAuthHeader: String? = null
        var capturedApiKeyHeader: String? = null
        val engine = MockEngine { request ->
            capturedBody = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            capturedAuthHeader = request.headers["Authorization"]
            capturedApiKeyHeader = request.headers["x-api-key"]
            respond(
                content = """{"choices":[{"message":{"role":"assistant","content":"ok"}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        sendToAiConnector(OPENAI_API_PROFILE, apiKey = "sk-openai-secret", prompt = "write a story", engine = engine)

        assertTrue(capturedBody!!.contains("write a story"))
        assertEquals(
            "خودِ فرمت OpenAI: Authorization: Bearer <کلید> — نه x-api-key خام مثل Claude",
            "Bearer sk-openai-secret",
            capturedAuthHeader
        )
        assertNull("OpenAI هرگز نباید هدر x-api-key بگیرد — آن مخصوص Claude است", capturedApiKeyHeader)
    }

    @Test
    fun `a 401 OpenAI-shaped error body produces a Result failure with the real error message`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"error":{"message":"Incorrect API key provided","type":"invalid_request_error","param":null,"code":"invalid_api_key"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = sendToAiConnector(OPENAI_API_PROFILE, apiKey = "wrong-key", prompt = "hi", engine = engine)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Incorrect API key provided"))
    }

    // --- GEMINI_API_PROFILE واقعی (G2/ADR-103: سومین پروفایل واقعی، سومین
    // الگوی متفاوت احراز هویت/بدنه) — روی خودِ GEMINI_API_PROFILE تولیدی تست
    // می‌شود (نه یک Fixture مشابه).

    @Test
    fun `a successful Gemini response extracts the correct text via candidates index 0 content parts index 0 text`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"candidates":[{"content":{"parts":[{"text":"Hello from Gemini"}],"role":"model"}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = sendToAiConnector(GEMINI_API_PROFILE, apiKey = "gemini-test-key", prompt = "hi", engine = engine)

        assertTrue(result.isSuccess)
        assertEquals("Hello from Gemini", result.getOrNull())
    }

    @Test
    fun `the real request to Gemini carries an x-goog-api-key header, not x-api-key or Authorization, with the real prompt and key`() = runBlocking {
        var capturedBody: String? = null
        var capturedGoogHeader: String? = null
        var capturedApiKeyHeader: String? = null
        var capturedAuthHeader: String? = null
        val engine = MockEngine { request ->
            capturedBody = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            capturedGoogHeader = request.headers["x-goog-api-key"]
            capturedApiKeyHeader = request.headers["x-api-key"]
            capturedAuthHeader = request.headers["Authorization"]
            respond(
                content = """{"candidates":[{"content":{"parts":[{"text":"ok"}]}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        sendToAiConnector(GEMINI_API_PROFILE, apiKey = "gemini-secret-key", prompt = "write a story", engine = engine)

        assertTrue(capturedBody!!.contains("write a story"))
        assertEquals(
            "سومین روش متفاوت احراز هویت این پروژه: x-goog-api-key خام — نه x-api-key (Claude) و نه Authorization: Bearer (OpenAI)",
            "gemini-secret-key",
            capturedGoogHeader
        )
        assertNull("Gemini نباید هدر x-api-key بگیرد — آن مخصوص Claude است", capturedApiKeyHeader)
        assertNull("Gemini نباید هدر Authorization بگیرد — آن مخصوص OpenAI است", capturedAuthHeader)
    }

    @Test
    fun `a 400 Gemini-shaped error body produces a Result failure with the real error message`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT"}}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = sendToAiConnector(GEMINI_API_PROFILE, apiKey = "wrong-key", prompt = "hi", engine = engine)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("API key not valid"))
    }

    /**
     * یافته‌ی واقعی این قدم (تأییدشده با WebSearch از مستندات رسمی Gemini —
     * از‌جمله صفحه‌ی رسمی «Thought Signatures» و منابع دیگر، چون WebFetch
     * مستقیم به ai.google.dev از این محیط Sandbox مسدود بود): یک آیتم آرایه‌ی
     * parts می‌تواند فقط thoughtSignature داشته باشد (بدون فیلد text)، درحالی‌که
     * آیتم بعدی همان آرایه واقعاً متن اصلی را دارد. رفتار واقعی (نه فرضی) این
     * تست: extractByJsonPath (رفع‌شده در همین قدم) روی چنین ورودی‌ای اندیس‌های
     * بعدی همان آرایه‌ی parts را هم امتحان می‌کند تا اولین آیتمی که واقعاً
     * فیلد text دارد را پیدا کند — پیش از این رفع، parts[0] فاقد text باعث
     * Result.failure می‌شد (حتی وقتی متن واقعی در parts[1] موجود بود).
     */
    @Test
    fun `a Gemini response where parts index 0 has only a thoughtSignature and the real text is in parts index 1 still extracts successfully`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"candidates":[{"content":{"parts":[{"thoughtSignature":"opaque-signature-abc"},{"text":"Hello from Gemini after thinking"}],"role":"model"}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = sendToAiConnector(GEMINI_API_PROFILE, apiKey = "gemini-test-key", prompt = "hi", engine = engine)

        assertTrue(
            "extractByJsonPath باید اندیس بعدی parts را هم امتحان کند، نه فقط parts[0] که فقط thoughtSignature دارد",
            result.isSuccess
        )
        assertEquals("Hello from Gemini after thinking", result.getOrNull())
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
