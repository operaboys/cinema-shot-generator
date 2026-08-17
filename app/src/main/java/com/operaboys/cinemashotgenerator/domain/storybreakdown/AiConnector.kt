package com.operaboys.cinemashotgenerator.domain.storybreakdown

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.IOException
import java.net.SocketTimeoutException

// واحد ۰۱ب — AI Story Breakdown (بخش ب، مسیر ۲: AI Connector Profile)
// منبع حقیقت: docs/blueprints/01b-ai-story-breakdown.md (نسخه ۲)
//
// آخرین قدم این واحد — ادامه‌ی PromptBuilder.kt/ChunkCombiner.kt (قدم اول)،
// JsonDoctor.kt (قدم دوم)، StoryToDomainMapper.kt (قدم سوم).
//
// G2 قدم ۲ از ۳ (ADR-100): اتصال HTTP واقعی — تصمیم Option A قبلی (ADR-035:
// sendToAiConnector عمداً TODO()) اکنون با Option B جایگزین شد. قدم ۱ (ADR-098،
// SecureKeyRepository.kt) لایه‌ی ذخیره‌سازی امن کلید را مستقل ساخت — این فایل به
// آن وابسته نیست (کلید همچنان پارامتر ورودی است، خواندنش از Storage کار UI بود).
// G2 قدم ۳ (ADR-101) UI انتخابگر مسیر ۱/۲ را وصل کرد (اولش هاردکد به Claude).
// این قدم (ADR-102) دومین پروفایل واقعی (OpenAI) را اضافه می‌کند و همان هاردکد
// UI را به انتخابگر واقعی چندپروفایلی تبدیل می‌کند — بازبینی موعود خودِ ADR-101.

/** پروفایل یک سرویس AI متنی — کاملاً مستقل از پیاده‌سازی، هم‌خانواده با ModelProfile واحد ۱۴. */
data class AiConnectorProfile(
    val profileId: String,
    val displayName: String,
    val endpointUrl: String,
    val requestBodyTemplate: String,
    val requestHeaders: Map<String, String> = emptyMap(),
    val responseJsonPath: String
)

/**
 * اولین پروفایل واقعی — Claude API (Anthropic Messages API)، تأییدشده مستقیم از
 * platform.claude.com/docs/en/build-with-claude/working-with-messages (نه حدس):
 * Endpoint: POST https://api.anthropic.com/v1/messages
 * Header های الزامی: x-api-key (کلید خام، بدون Bearer)، anthropic-version: 2023-06-01
 * بدنه: {"model": ..., "max_tokens": N, "messages": [{"role": "user", "content": "..."}]}
 * پاسخ: content[0].text — دقیقاً همان مسیری که خودِ کد مفهومی بلوپرینت ۰۱ب به‌عنوان
 * مثال آورده بود؛ تصادفی نیست، طراحی اولیه‌ی بلوپرینت برای Claude API درست بوده.
 *
 * مدل/max_tokens: claude-sonnet-5 (تعادل توان/هزینه برای یک Story Breakdown — نه
 * مدل گران‌ترین/سریع‌ترین، بلوپرینت خودش نظری برای انتخاب مدل نداده) با
 * max_tokens=4096 (کافی برای خروجی JSON چندشاته‌ی بخش الف؛ اگر AI پاسخ را در
 * چند بخش با [CONTINUE] بفرستد، ChunkCombiner.kt موجود پروژه از قبل این را
 * پوشش می‌دهد — طبق تصمیم ۰۱ب همان قدم).
 */
val CLAUDE_API_PROFILE: AiConnectorProfile = AiConnectorProfile(
    profileId = "claude_api",
    displayName = "Claude API",
    endpointUrl = "https://api.anthropic.com/v1/messages",
    requestBodyTemplate = """{"model":"claude-sonnet-5","max_tokens":4096,"messages":[{"role":"user","content":"{{PROMPT}}"}]}""",
    requestHeaders = mapOf(
        "x-api-key" to "{{API_KEY}}",
        "anthropic-version" to "2023-06-01"
    ),
    responseJsonPath = "content[0].text"
)

/**
 * دومین پروفایل واقعی — OpenAI API (Chat Completions)، تأییدشده مستقیم از
 * مستندات رسمی OpenAI (developers.openai.com/api/reference — endpoint، Header
 * الزامی، بدنه، و choices[0].message.content):
 * Endpoint: POST https://api.openai.com/v1/chat/completions
 * Header الزامی: Authorization: Bearer <کلید> — برخلاف Claude (x-api-key خام،
 * بدون Bearer)، یک تفاوت واقعی بین دو سرویس، نه فرض یکسان‌بودن.
 * بدنه: {"model": ..., "messages": [{"role": "user", "content": "..."}]}
 * پاسخ: choices[0].message.content — دقیقاً همان نمونه‌ی دوم صریح خودِ
 * بلوپرینت ۰۱ب برای responseJsonPath (کنار content[0].text که برای Claude
 * استفاده شد) — تصادفی نیست، بلوپرینت از ابتدا هر دو فرمت را در نظر داشته.
 *
 * مدل: gpt-5-mini — تعادل توان/هزینه (هم‌رده با انتخاب claude-sonnet-5 در
 * پروفایل قبلی). بررسی مستقل (WebSearch، چون دسترسی مستقیم به
 * platform.openai.com/developers.openai.com از این محیط Sandbox مسدود است —
 * محدودیت شبکه، نه کوتاهی راستی‌آزمایی) این مدل را به‌عنوان یک Model ID واقعی
 * و رسمی تأیید کرد (صفحه‌ی مستند developers.openai.com/api/docs/models/gpt-5-mini).
 * نسخه‌های جدیدتر (gpt-5.4-mini و بعدتر) هم در جست‌وجو دیده شدند، اما بدون
 * امکان Fetch مستقیم صفحه‌ی رسمی برای تأیید کامل، ترجیح داده شد به مدل از‌قبل
 * تأییدشده (پیشنهاد معمار) پایبند بمانم — نه یک ID جدیدتر حدسی از خلاصه‌ی
 * موتور جستجو. جزئیات کامل در docs/adr/102-g2-second-profile-openai-and-profile-selector.md.
 */
val OPENAI_API_PROFILE: AiConnectorProfile = AiConnectorProfile(
    profileId = "openai_api",
    displayName = "OpenAI API",
    endpointUrl = "https://api.openai.com/v1/chat/completions",
    requestBodyTemplate = """{"model":"gpt-5-mini","messages":[{"role":"user","content":"{{PROMPT}}"}]}""",
    requestHeaders = mapOf("Authorization" to "Bearer {{API_KEY}}"),
    responseJsonPath = "choices[0].message.content"
)

/**
 * پروفایل‌های آماده — ADR-100 اولین پروفایل واقعی (Claude) را اضافه کرد و صریحاً
 * مستند کرد OpenAI/Gemini/DeepSeek/Qwen خارج از Scope آن قدم‌اند. این قدم دومین
 * پروفایل واقعی (OpenAI) را اضافه می‌کند — طبق همان الگو (بررسی مستقل مستندات
 * رسمی، نه حدس). Gemini/DeepSeek/Qwen همچنان خارج از Scope این قدم‌اند.
 */
val BUILTIN_AI_CONNECTOR_PROFILES: List<AiConnectorProfile> = listOf(CLAUDE_API_PROFILE, OPENAI_API_PROFILE)

/**
 * کاربر پیشرفته می‌تواند یک پروفایل کاملاً دستی برای سرویس ناشناخته/محلی بسازد.
 *
 * تصحیح یک ناهماهنگی جزئی در کد مفهومی بلوپرینت: امضای بلوپرینت پارامتر
 * `jsonPathResponse` را در فراخوانی سازنده به کار برده بود که با نام واقعی فیلد
 * `responseJsonPath` در همان data class (تعریف‌شده چند خط بالاتر در همان بلوپرینت)
 * مطابقت ندارد — یک Typo واقعی در متن بلوپرینت (تأییدشده با مقایسه‌ی مستقیم دو بخش)،
 * نه یک انحراف عمدی. اینجا با نام درست `responseJsonPath` اصلاح شد.
 */
fun createCustomAiConnectorProfile(
    displayName: String,
    endpointUrl: String,
    requestBodyTemplate: String,
    responseJsonPath: String
): AiConnectorProfile = AiConnectorProfile(
    profileId = generateId("aiconnector"),
    displayName = displayName,
    endpointUrl = endpointUrl,
    requestBodyTemplate = requestBodyTemplate,
    responseJsonPath = responseJsonPath
)

private val jsonCodec = Json { ignoreUnknownKeys = true }

/**
 * جایگزینی Placeholder — طبق کد مفهومی بلوپرینت، هم در requestBodyTemplate
 * ({{PROMPT}}/{{API_KEY}}) و هم در requestHeaders (نمونه‌ی صریح خودِ بلوپرینت:
 * `"Authorization" -> "Bearer {{API_KEY}}"`) رخ می‌دهد — نه فقط بدنه. مقدار prompt
 * با jsonCodec.encodeToString درست Escape می‌شود (نقل‌قول/بک‌اسلش/خط‌جدید) و فقط
 * محتوای داخل گیومه‌ی تولیدشده استفاده می‌شود — چون خودِ گیومه‌های اطراف
 * {{PROMPT}} از قبل در requestBodyTemplate هستند (طبق نمونه‌ی Claude پروفایل
 * بالا: `"content":"{{PROMPT}}"`)؛ apiKey هم برای احتیاط همین‌طور Escape می‌شود
 * (بدون فرض این‌که کلیدهای واقعی هرگز کاراکتر خاص ندارند).
 */
private fun fillPlaceholders(template: String, prompt: String, apiKey: String): String {
    val escapedPrompt = jsonStringEscape(prompt)
    val escapedApiKey = jsonStringEscape(apiKey)
    return template.replace("{{PROMPT}}", escapedPrompt).replace("{{API_KEY}}", escapedApiKey)
}

private fun jsonStringEscape(value: String): String {
    val encoded = jsonCodec.encodeToString(value)
    return encoded.substring(1, encoded.length - 1)
}

/**
 * استخراج یک مقدار از JSON با یک مسیر ساده مثل "content[0].text" یا
 * "choices[0].message.content" (هر دو نمونه‌ی صریح خودِ بلوپرینت برای
 * responseJsonPath) — یک Parser عمومی نوشته شد (نه فقط پشتیبانی از فرمت Claude)،
 * چون این فایل قرار است «الگوی سرویس‌های بعدی» باشد (طبق دستور صریح این قدم) و
 * responseJsonPath خودش یک فیلد Data-driven روی هر پروفایل است — قفل‌کردنش به
 * یک فرمت خاص، هدف چندسرویسی‌بودن خودِ AiConnectorProfile را نقض می‌کرد.
 */
private fun extractByJsonPath(root: JsonElement, path: String): String? {
    val tokens = Regex("[^.\\[\\]]+|\\[\\d+\\]").findAll(path).map { it.value }.toList()
    var current: JsonElement? = root
    for (token in tokens) {
        current = current ?: return null
        current = if (token.startsWith("[")) {
            val index = token.removeSurrounding("[", "]").toIntOrNull() ?: return null
            (current as? JsonArray)?.getOrNull(index)
        } else {
            (current as? JsonObject)?.get(token)
        }
    }
    return (current as? JsonPrimitive)?.content
}

/** پیام خطای معنادار از یک پاسخ HTTP ناموفق — طبق فرمت شناخته‌شده‌ی خطای Anthropic ({"error":{"message":...}}) در صورت وجود، وگرنه بدنه‌ی خام. */
private fun describeHttpError(statusCode: Int, body: String): String {
    val parsedMessage = runCatching {
        val root = jsonCodec.parseToJsonElement(body)
        ((root as? JsonObject)?.get("error") as? JsonObject)?.get("message")?.let { (it as? JsonPrimitive)?.content }
    }.getOrNull()
    return when {
        !parsedMessage.isNullOrBlank() -> "سرویس AI با کد وضعیت $statusCode خطا داد: $parsedMessage"
        body.isNotBlank() -> "سرویس AI با کد وضعیت $statusCode خطا داد: $body"
        else -> "سرویس AI با کد وضعیت $statusCode خطا داد، بدون پیام خطای اضافی"
    }
}

/**
 * ارسال درخواست واقعی به یک AI Connector — با Ktor Client (OkHttp Engine، طبق
 * Stack مصوب پروژه). `engine` تزریق‌پذیر است (هم‌الگو با idProvider/clock در
 * ProjectRepository.kt، sharedPreferencesFactory در SecureKeyRepository.kt) —
 * پیش‌فرض واقعی OkHttp؛ تست‌ها MockEngine (io.ktor:ktor-client-mock) تزریق
 * می‌کنند تا بدون تماس واقعی اینترنت اجرا شوند.
 *
 * Timeout: ۱۵ ثانیه اتصال (شبکه‌ی موبایل، نه فرض همیشه‌سریع)، ۶۰ ثانیه کل درخواست
 * (تولید پاسخ‌های بلند توسط LLM می‌تواند ده‌ها ثانیه طول بکشد، جدا از تأخیر شبکه).
 *
 * Client per-call ساخته و بسته می‌شود (نه یک نمونه‌ی سراسری Reuse‌شونده) — این
 * فایل کاملاً Stateless است (توابع top-level، بدون کلاس/چرخه‌حیات)، هم‌الگو با
 * بقیه‌ی این فایل؛ هزینه‌ی از‌دست‌رفته‌ی Connection Pooling ناچیز است چون این
 * درخواست‌ها با اقدام مستقیم کاربر رخ می‌دهند (نه با فرکانس بالا).
 */
suspend fun sendToAiConnector(
    profile: AiConnectorProfile,
    apiKey: String,
    prompt: String,
    engine: HttpClientEngine = OkHttp.create()
): Result<String> {
    val client = HttpClient(engine) {
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
    }
    return try {
        val requestBody = fillPlaceholders(profile.requestBodyTemplate, prompt, apiKey)
        val response = client.post(profile.endpointUrl) {
            profile.requestHeaders.forEach { (name, value) -> header(name, fillPlaceholders(value, prompt, apiKey)) }
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            Result.failure(IOException(describeHttpError(response.status.value, responseText)))
        } else {
            val extracted = runCatching { extractByJsonPath(jsonCodec.parseToJsonElement(responseText), profile.responseJsonPath) }.getOrNull()
            if (extracted != null) {
                Result.success(extracted)
            } else {
                Result.failure(IOException("پاسخ سرویس AI با مسیر '${profile.responseJsonPath}' قابل‌استخراج نبود: $responseText"))
            }
        }
    } catch (e: HttpRequestTimeoutException) {
        Result.failure(IOException("درخواست به سرویس AI در زمان مقرر (۶۰ ثانیه) پاسخ نداد", e))
    } catch (e: ConnectTimeoutException) {
        Result.failure(IOException("اتصال به سرویس AI برقرار نشد (Timeout اتصال)", e))
    } catch (e: SocketTimeoutException) {
        Result.failure(IOException("ارتباط با سرویس AI در حین انتقال داده قطع شد (Timeout شبکه)", e))
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.failure(IOException("خطای شبکه در ارتباط با سرویس AI: ${e.message}", e))
    } catch (e: Exception) {
        Result.failure(IOException("خطای غیرمنتظره در ارتباط با سرویس AI: ${e.message}", e))
    } finally {
        client.close()
    }
}

/** Rule 4 (Blocking): مسیر ۲ (AI Connector) انتخاب شده ولی apiKey خالی است. */
fun validateApiKeyProvided(apiKey: String): ValidationIssue? {
    if (apiKey.isBlank()) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "برای استفاده از مسیر AI Connector باید کلید API وارد شود؛ فیلد کلید خالی است"
        )
    }
    return null
}

/**
 * Rule 5 (Blocking): خطای مسیر ۲ باید متن واقعی خطای سرویس را (خلاصه) نشان دهد، نه
 * فقط «خطا رخ داد». اکنون که sendToAiConnector واقعی است (G2 قدم ۲)، این تابع دقیقاً
 * همان قرارداد قدیمی (Option A) را می‌خواند — منبع Exception واقعی HTTP شد، اما
 * قرارداد Rule خودش عوض نشد.
 */
fun validateAiConnectorErrorMessage(result: Result<String>): ValidationIssue? {
    val exception = result.exceptionOrNull() ?: return null
    val message = exception.message
    if (message.isNullOrBlank()) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "درخواست به AI Connector با خطا مواجه شد، اما پیام خطای واقعی سرویس در دسترس نیست"
        )
    }
    return ValidationIssue(Severity.BLOCKING, message = "درخواست به AI Connector با خطا مواجه شد: $message")
}
