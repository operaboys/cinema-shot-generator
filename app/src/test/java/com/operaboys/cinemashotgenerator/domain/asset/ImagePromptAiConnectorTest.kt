package com.operaboys.cinemashotgenerator.domain.asset

import com.operaboys.cinemashotgenerator.domain.storybreakdown.AiConnectorProfile
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۳ از ۵ (ADR-133). هیچ فراخوان واقعی
// شبکه — هم‌الگو دقیق با AiConnectorTest.kt موجود: profile ساده‌ی هم‌شکل با Claude
// (content[0].text)، MockEngine برای شبیه‌سازی پاسخ HTTP.

private val claudeLikeProfile = AiConnectorProfile(
    profileId = "test_profile",
    displayName = "Test Profile",
    endpointUrl = "https://example.com/v1/messages",
    requestBodyTemplate = """{"messages":[{"role":"user","content":"{{PROMPT}}"}]}""",
    requestHeaders = mapOf("x-api-key" to "{{API_KEY}}"),
    responseJsonPath = "content[0].text"
)

class ImagePromptAiConnectorTest {

    // --- buildImagePromptAiRequest ---

    @Test
    fun `buildImagePromptAiRequest includes the template prompt, style tokens, and bilingual JSON schema instruction`() {
        val request = buildImagePromptAiRequest(templatePrompt = "a determined detective, cinematic style", styleTokens = "cinematic style, dramatic composition")

        assertTrue(request.contains("a determined detective, cinematic style"))
        assertTrue(request.contains("cinematic style, dramatic composition"))
        assertTrue(request.contains("imagePromptEn"))
        assertTrue(request.contains("imagePromptFa"))
    }

    // --- parseImagePromptAiResponse ---

    @Test
    fun `parseImagePromptAiResponse successfully parses a valid bilingual JSON response`() {
        val raw = """{"imagePromptEn": "a refined cinematic portrait", "imagePromptFa": "یک پرتره‌ی سینمایی پالایش‌شده"}"""

        val result = parseImagePromptAiResponse(raw)

        assertTrue(result.isSuccess)
        assertEquals("a refined cinematic portrait", result.getOrNull()?.imagePromptEn)
        assertEquals("یک پرتره‌ی سینمایی پالایش‌شده", result.getOrNull()?.imagePromptFa)
    }

    @Test
    fun `parseImagePromptAiResponse fails with a meaningful message for malformed or incomplete JSON`() {
        val raw = """{"imagePromptEn": "missing the Farsi field"}"""

        val result = parseImagePromptAiResponse(raw)

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull()?.message)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("imagePromptEn/imagePromptFa"))
    }

    @Test
    fun `parseImagePromptAiResponse fails on completely non-JSON text`() {
        val result = parseImagePromptAiResponse("Sure, here is your prompt: a cinematic portrait.")
        assertTrue(result.isFailure)
    }

    // --- generateImagePromptWithAi ---

    @Test
    fun `generateImagePromptWithAi returns a successfully parsed response on a successful HTTP call`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"content":[{"type":"text","text":"{\"imagePromptEn\": \"a rich cinematic prompt\", \"imagePromptFa\": \"یک پرامپت سینمایی غنی\"}"}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = generateImagePromptWithAi(
            templatePrompt = "a detective in a trench coat",
            styleTokens = "cinematic style",
            profile = claudeLikeProfile,
            apiKey = "sk-test-123",
            engine = engine
        )

        assertTrue(result.isSuccess)
        assertEquals("a rich cinematic prompt", result.getOrNull()?.imagePromptEn)
        assertEquals("یک پرامپت سینمایی غنی", result.getOrNull()?.imagePromptFa)
    }

    @Test
    fun `generateImagePromptWithAi returns a failure when the HTTP call itself fails`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = generateImagePromptWithAi(
            templatePrompt = "a detective in a trench coat",
            styleTokens = "cinematic style",
            profile = claudeLikeProfile,
            apiKey = "sk-invalid",
            engine = engine
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("invalid x-api-key"))
    }

    @Test
    fun `generateImagePromptWithAi returns a failure when the HTTP call succeeds but the AI response is not valid JSON`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"content":[{"type":"text","text":"Sure! Here is an enriched prompt for you."}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = generateImagePromptWithAi(
            templatePrompt = "a detective in a trench coat",
            styleTokens = "cinematic style",
            profile = claudeLikeProfile,
            apiKey = "sk-test-123",
            engine = engine
        )

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("imagePromptEn/imagePromptFa"))
    }
}
