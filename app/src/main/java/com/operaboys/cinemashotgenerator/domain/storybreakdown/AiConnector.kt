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
// ADR-102 دومین پروفایل واقعی (OpenAI) را اضافه کرد و همان هاردکد UI را به
// انتخابگر واقعی چندپروفایلی تبدیل کرد. ADR-103 سومین پروفایل واقعی (Gemini)
// را اضافه کرد و یک یافته‌ی واقعی extractByJsonPath را رفع کرد. ADR-104
// چهارمین پروفایل واقعی (DeepSeek) را اضافه کرد و یافته‌ی حیاتی بازنشستگی
// نام مدل قدیمی را مستند کرد. این قدم (ADR-105) پنجمین و **آخرین** پروفایل
// برنامه‌ریزی‌شده (Qwen) را اضافه می‌کند — G2 با این قدم به‌طور کامل بسته
// می‌شود. در تمام این چهار قدم آخر، UI/ViewModel صفر خط تغییر داشتند.

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
 * سومین پروفایل واقعی — Gemini API (`generateContent`)، تأییدشده مستقیم از
 * مستندات رسمی Google (ai.google.dev/api — WebSearch، چون WebFetch مستقیم به
 * ai.google.dev هم مثل platform.openai.com از این محیط Sandbox مسدود است):
 * Endpoint: POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent
 * — سومین الگوی متفاوت در این پروژه: برخلاف Claude/OpenAI، نام مدل در خودِ
 * endpointUrl است، نه در بدنه‌ی JSON؛ پس این پروفایل نیازی به Placeholder مدل
 * در بدنه ندارد (مدل از قبل در URL هاردکد است).
 * Header الزامی: x-goog-api-key (کلید خام) — سومین روش متفاوت احراز هویت
 * (نه x-api-key مثل Claude، نه Authorization: Bearer مثل OpenAI).
 * بدنه: {"contents":[{"parts":[{"text":"..."}]}]} — کلید بیرونی «contents»
 * (نه «messages» مثل OpenAI)، هر آیتم «parts» است (نه یک content string ساده
 * مثل Claude/OpenAI).
 * پاسخ: candidates[0].content.parts[0].text.
 *
 * مدل: gemini-2.5-flash — طبق مستندات رسمی («مدل پیش‌فرض» صریح، پایدار،
 * غیر-Legacy) و طبق پیشنهاد معمار؛ بررسی مستقل (WebSearch) هر دو را تأیید
 * کرد، بدون نیاز به جایگزینی.
 *
 * یافته‌ی مهم (تأییدشده با WebSearch، نه فرض کورکورانه‌ی این پرامپت): مدل‌های
 * «Thinking» نسل ۲.۵/۳ Gemini می‌توانند یک `thoughtSignature` را به یک Part
 * ضمیمه کنند — طبق چند منبع (از‌جمله صفحه‌ی رسمی
 * ai.google.dev/gemini-api/docs/generate-content/thought-signatures) این
 * عمدتاً در پاسخ‌های Streaming (آخرین Chunk) یا سناریوهای Function Calling
 * چندمرحله‌ای رخ می‌دهد — نه لزوماً یک generateContent ساده و تک‌شات مثل این
 * پروفایل؛ اما چون تأیید کامل ۱۰۰٪ («هرگز رخ نمی‌دهد») از این محیط ممکن نبود
 * (WebFetch مسدود)، و هزینه‌ی رفع این حالت در extractByJsonPath (پایین‌تر)
 * ناچیز و بدون تغییر رفتار پروفایل‌های دیگر بود، این حالت واقعاً رفع شد (نه
 * فقط مستند) — جزئیات کامل در docs/adr/103-g2-third-profile-gemini-and-parser-robustness.md.
 */
val GEMINI_API_PROFILE: AiConnectorProfile = AiConnectorProfile(
    profileId = "gemini_api",
    displayName = "Gemini API",
    endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
    requestBodyTemplate = """{"contents":[{"parts":[{"text":"{{PROMPT}}"}]}]}""",
    requestHeaders = mapOf("x-goog-api-key" to "{{API_KEY}}"),
    responseJsonPath = "candidates[0].content.parts[0].text"
)

/**
 * چهارمین پروفایل واقعی — DeepSeek API، تأییدشده مستقل (WebFetch مستقیم به
 * api-docs.deepseek.com هم مثل platform.openai.com/ai.google.dev از این محیط
 * Sandbox مسدود بود — `EGRESS_BLOCKED`؛ با WebSearch چندمنبعی تأیید شد،
 * از‌جمله عنوان مستقیم صفحه‌ی رسمی «Chat Completions API | DeepSeek API Docs»
 * در نتایج جست‌وجو):
 * DeepSeek عمداً و کاملاً با فرمت OpenAI Chat Completions سازگار است — همان
 * Authorization: Bearer <کلید>، همان بدنه‌ی {model, messages}، همان مسیر
 * پاسخ choices[0].message.content. تنها تفاوت واقعی: endpointUrl و نام مدل.
 *
 * Endpoint: POST https://api.deepseek.com/chat/completions — **بدون** پیشوند
 * `/v1/` (برخلاف OpenAI). چند منبع کمی متفاوت نوشته بودند (برخی `/v1/` را هم
 * نشان می‌دادند)؛ بررسی چندمنبعی مشخص کرد `/v1/` فقط برای سازگاری با SDK های
 * رسمی OpenAI پذیرفته می‌شود، اما مسیر رسمی مستندشده در خودِ
 * api-docs.deepseek.com بدون `/v1/` است — همان چیزی که پیش‌بریفینگ معمار هم
 * گفته بود، تأییدشده نه فرض.
 *
 * ⚠️ یافته‌ی حیاتی (تأییدشده با چند منبع مستقل، تاریخ‌دار — طبق همان روش
 * محافظه‌کارانه‌ی ADR-102 برای مدل OpenAI): نام‌های قدیمی مدل `deepseek-chat`
 * و `deepseek-reasoner` در ساعت ۱۵:۵۹ UTC روز ۲۴ جولای ۲۰۲۶ عملاً بازنشسته
 * شدند و دیگر بدون خطا کار نمی‌کنند — هر دو نام قدیمی (نه فقط یکی) در دوره‌ی
 * انتقال به `deepseek-v4-flash` Route می‌شدند (نکته‌ای دقیق‌تر از آنچه
 * پیش‌بریفینگ گفته بود — پیش‌بریفینگ deepseek-v4-pro/deepseek-v4-flash را دو
 * جایگزین مستقل برای دو نام قدیمی معرفی کرده بود؛ منابع مستقل نشان دادند هر
 * دو نام قدیمی در واقع به همان مدل زیرین (V4، در دو حالت Thinking/Non-Thinking)
 * اشاره می‌کردند و هر دو به `deepseek-v4-flash` مهاجرت می‌کنند — `deepseek-v4-pro`
 * یک مدل جداگانه‌ی قوی‌تر است، نه جایگزین مستقیم هیچ‌کدام از دو نام قدیمی).
 * چون تاریخ فعلی پروژه (۲۰۲۶-۰۸-۱۷) پس از این بازنشستگی است، از نام تازه‌ی
 * تأییدشده `deepseek-v4-flash` استفاده شد (طبق پیشنهاد معمار هم، تأییدشده
 * مستقل) — نه نام‌های منسوخِ `deepseek-chat`/`deepseek-reasoner` که واقعاً
 * دیگر کار نمی‌کنند. جزئیات کامل (شامل منابع) در
 * docs/adr/104-g2-fourth-profile-deepseek-and-model-deprecation.md.
 *
 * تصمیم طراحی: به‌جای یک تابع/Helper مشترک برای «پروفایل‌های سازگار با فرمت
 * OpenAI»، این پروفایل هم مثل سه‌تای قبلی یک `val` کاملاً مستقل است — هم‌الگو
 * با بقیه‌ی این فایل. یک Helper مشترک فقط برای این یک نقطه‌ی استفاده‌ی دوم
 * (OpenAI + DeepSeek) پیچیدگی بی‌دلیل اضافه می‌کرد (طبق فلسفه‌ی Simplicity
 * پروژه)؛ اگر سرویس سازگار سوم/چهارمی بعداً اضافه شد، این تصمیم قابل‌بازبینی
 * است.
 */
val DEEPSEEK_API_PROFILE: AiConnectorProfile = AiConnectorProfile(
    profileId = "deepseek_api",
    displayName = "DeepSeek API",
    endpointUrl = "https://api.deepseek.com/chat/completions",
    requestBodyTemplate = """{"model":"deepseek-v4-flash","messages":[{"role":"user","content":"{{PROMPT}}"}]}""",
    requestHeaders = mapOf("Authorization" to "Bearer {{API_KEY}}"),
    responseJsonPath = "choices[0].message.content"
)

/**
 * پنجمین و آخرین پروفایل واقعی (طبق برنامه‌ی پنج‌پروفایلی G2) — Qwen API
 * (Alibaba Cloud Model Studio / DashScope)، تأییدشده مستقل (WebFetch مستقیم
 * به alibabacloud.com هم مثل سه سرویس قبلی از این محیط Sandbox مسدود بود —
 * `EGRESS_BLOCKED`؛ با WebSearch چندمنبعی تأیید شد).
 * Qwen عمداً و کاملاً با فرمت OpenAI Chat Completions سازگار است — همان
 * Authorization: Bearer <کلید>، همان بدنه‌ی {model, messages}، همان مسیر
 * پاسخ choices[0].message.content.
 *
 * ⚠️ نکته‌ی معماری حیاتی — دو نوع Endpoint متفاوت، فقط یکی سازگار با این
 * پروژه: برای برخی مناطق (Singapore و مشابه)، Alibaba Cloud یک الگوی URL
 * می‌دهد که WorkspaceId شخصیِ هر کاربر را داخل خودِ مسیر URL دارد (مثلاً
 * `https://{WorkspaceId}.ap-southeast-1.maas.aliyuncs.com/compatible-mode/v1/...`)
 * — این با معماری `AiConnectorProfile` این پروژه ناسازگار است، چون
 * `endpointUrl` یک مقدار ثابت و از‌پیش‌تعیین‌شده در کد است، نه چیزی که
 * per-user جایگزین شود؛ پروژه فعلاً هیچ مکانیزمی برای «پارامتر کاربر داخل
 * URL» ندارد و اضافه‌کردنش گسترش معماری غیرضروری برای این قدم بود — رد شد،
 * عمداً. به‌جایش از الگوی بدون WorkspaceId استفاده شد که رسماً هم وجود دارد
 * و کار می‌کند: `https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions`.
 * بین نوع داخل‌چین (`dashscope.aliyuncs.com`) و بین‌المللی
 * (`dashscope-intl.aliyuncs.com`)، بررسی مستقل نوع بین‌المللی را برای
 * کاربران عمومی/بدون محدودیت جغرافیایی تأیید کرد (منابع چندگانه: کلید API
 * باید با منطقه‌ی ثبت حساب Alibaba Cloud هم‌خوان باشد؛ چون این پروژه به یک
 * کشور خاص محدود نیست، نوع بین‌المللی منطقی‌تر است — مطابق پیشنهاد معمار،
 * تأییدشده نه فرض).
 *
 * مدل: `qwen-plus` — یک نام پایدار و غیر-نسخه‌دار (برخلاف نام‌های نسخه‌دار
 * مثل `qwen3.6-plus`/`qwen3.7-plus` که در جست‌وجو هم دیده شدند اما ریسک
 * بازنشستگی دارند — دقیقاً همان درسی که از یافته‌ی مدل منسوخ DeepSeek
 * (ADR-104) گرفته شد)؛ بررسی مستقل `qwen-plus` را به‌عنوان یک نام پایدار و
 * همچنان معتبر (کنار `qwen-max`/`qwen-flash`) تأیید کرد — مطابق پیشنهاد
 * معمار، بدون نیاز به جایگزینی. جزئیات کامل در
 * docs/adr/105-g2-fifth-profile-qwen-and-g2-completion.md.
 *
 * تصمیم طراحی (طبق ADR-104): این پروفایل هم مثل چهارتای قبلی یک `val`
 * کاملاً مستقل است، بدون Helper مشترک — بررسی کد فعلی دلیل قانع‌کننده‌ای
 * برای تغییر آن تصمیم به دست نداد (همان استدلال Simplicity هنوز صادق است).
 */
val QWEN_API_PROFILE: AiConnectorProfile = AiConnectorProfile(
    profileId = "qwen_api",
    displayName = "Qwen API",
    endpointUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions",
    requestBodyTemplate = """{"model":"qwen-plus","messages":[{"role":"user","content":"{{PROMPT}}"}]}""",
    requestHeaders = mapOf("Authorization" to "Bearer {{API_KEY}}"),
    responseJsonPath = "choices[0].message.content"
)

/**
 * پروفایل‌های آماده — ADR-100 اولین پروفایل واقعی (Claude) را اضافه کرد.
 * ADR-102 دومین (OpenAI)، ADR-103 سومین (Gemini)، و ADR-104 چهارمین
 * (DeepSeek) را اضافه کردند. این قدم (ADR-105) پنجمین و **آخرین** پروفایل
 * برنامه‌ریزی‌شده (Qwen) را اضافه می‌کند — با این قدم، G2 («اتصال واقعی به
 * AI Connector») به‌طور کامل بسته می‌شود.
 */
val BUILTIN_AI_CONNECTOR_PROFILES: List<AiConnectorProfile> =
    listOf(CLAUDE_API_PROFILE, OPENAI_API_PROFILE, GEMINI_API_PROFILE, DEEPSEEK_API_PROFILE, QWEN_API_PROFILE)

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
 *
 * G2/ADR-103 (سومین پروفایل، Gemini): رفع یک یافته‌ی واقعی (تأییدشده با
 * WebSearch از مستندات رسمی Gemini) — یک آیتم آرایه (مثلاً parts[0]) می‌تواند
 * فقط `thoughtSignature` داشته باشد، بدون فیلد نهایی مسیر (`text`)، درحالی‌که
 * آیتم بعدی همان آرایه واقعاً آن فیلد را دارد. اگر یک اندیس مشخص کل باقی‌مانده‌ی
 * مسیر را حل نکند، اندیس‌های بعدیِ همان آرایه هم امتحان می‌شوند تا اولین آیتمی
 * که واقعاً حل می‌شود پیدا شود — برای Claude/OpenAI (که همیشه فقط یک آیتم دارند)
 * این هیچ تغییر رفتاری ایجاد نمی‌کند (fallback هرگز لمس نمی‌شود).
 */
private fun extractByJsonPath(root: JsonElement, path: String): String? {
    val tokens = Regex("[^.\\[\\]]+|\\[\\d+\\]").findAll(path).map { it.value }.toList()
    return resolveJsonPathTokens(root, tokens, 0)
}

private fun resolveJsonPathTokens(current: JsonElement?, tokens: List<String>, tokenIndex: Int): String? {
    if (current == null) return null
    if (tokenIndex == tokens.size) return (current as? JsonPrimitive)?.content
    val token = tokens[tokenIndex]
    return if (token.startsWith("[")) {
        val startIndex = token.removeSurrounding("[", "]").toIntOrNull() ?: return null
        val array = current as? JsonArray ?: return null
        (startIndex until array.size).firstNotNullOfOrNull { index ->
            resolveJsonPathTokens(array.getOrNull(index), tokens, tokenIndex + 1)
        }
    } else {
        resolveJsonPathTokens((current as? JsonObject)?.get(token), tokens, tokenIndex + 1)
    }
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
