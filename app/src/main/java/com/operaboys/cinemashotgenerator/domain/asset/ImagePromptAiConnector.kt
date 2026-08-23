package com.operaboys.cinemashotgenerator.domain.asset

import com.operaboys.cinemashotgenerator.domain.storybreakdown.AiConnectorProfile
import com.operaboys.cinemashotgenerator.domain.storybreakdown.sendToAiConnector
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException

// فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۳ از ۵ (ADR-133)
// مسیر اختیاری/دوم (AI): پرامپت Template زیرقدم ۲ (ImagePromptEngine.kt) را به یک
// AI Connector می‌فرستد تا نسخه‌ی حرفه‌ای‌تر انگلیسی + ترجمه‌ی فارسی برگرداند. کاملاً
// مستقل از موتور اصلی پرامپت ویدیو و از AI Story Breakdown (domain/storybreakdown) —
// فقط تابع عمومی و آماده‌ی sendToAiConnector را وارد/فراخوانی می‌کند، بدون هیچ
// تغییری در خودِ آن فایل.
//
// @Serializable در این فایل (لایه‌ی domain/asset) — هم‌الگو با انحراف تأییدشده‌ی
// مشابه در domain/storybreakdown/StoryToDomainMapper.kt (که خودش به ADR-025/
// Renderer.kt ارجاع می‌دهد): این نوع مستقیماً از JSON خام AI Decode می‌شود، بدون
// DTO میانی جداگانه در data/repository/ که در این قدم هنوز معنا ندارد — هیچ
// Repository ای این داده را ذخیره نمی‌کند (ذخیره‌سازی در imagePromptAi/
// imagePromptFaPreview کار UI/ViewModel زیرقدم ۴/۵ است، نه این تابع).
//
// تصمیم مستقل — بدون repairJson/smartCombineChunks (JsonDoctor.kt/ChunkCombiner.kt):
// هر دو ابزار برای مشکل متفاوتی طراحی شده‌اند — پاسخ چندبخشی/بلند AI Story
// Breakdown (که با [CONTINUE] پاره‌پاره می‌شود) و ساختار تودرتوی بزرگ آن (characters/
// locations/objects/shots). پاسخ این زیرقدم یک شیء JSON تک‌شات و کوچک با فقط دو
// کلید رشته‌ای است — نه بلند/چندبخشی. `validateRequiredKeysPresent` هم مستقیماً
// قفل به کلیدهای Story Breakdown (`REQUIRED_TOP_LEVEL_KEYS`) است، برای این
// Schema قابل‌استفاده‌ی مجدد نیست. `runCatching { Json.decodeFromString(...) }`
// همان الگوی ساده‌تری است که خودِ JsonDoctor.kt در کامنت سرفایلش به
// domain/promptfinalization/PromptFinalizationPipeline.kt ارجاع می‌دهد — برای این
// حجم پاسخ کوچک، همان کافی و متناسب است؛ اگر در عمل پاسخ‌های ناقص/بدشکل این
// Schema کوچک واقعاً رایج شد، این تصمیم در زیرقدم‌های بعدی قابل‌بازبینی است.

/** پاسخ Parse‌شده‌ی AI — imagePromptEn پرامپت نهایی واقعی (به مدل تولید تصویر می‌رود)، imagePromptFa فقط برای مرور کاربر. */
@Serializable
data class ImagePromptAiResponse(val imagePromptEn: String, val imagePromptFa: String)

private val imagePromptAiJson = Json { ignoreUnknownKeys = true }

/**
 * متن درخواستی به AI — دستور غنی‌سازی پرامپت Template + قالب JSON دوزبانه‌ی
 * خروجی، هم‌الگو دقیق با STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION_BILINGUAL
 * (PromptBuilder.kt) اما برای دامنه‌ی پرامپت عکس، نه شکست داستان.
 */
fun buildImagePromptAiRequest(templatePrompt: String, styleTokens: String): String = buildString {
    appendLine("تو یک متخصص Prompt Engineering برای مدل‌های تولید تصویر هستی.")
    appendLine(
        "پرامپت پیش‌نویس زیر را حرفه‌ای‌تر و غنی‌تر کن — جزئیات فتوگرافی/سینمایی بیشتر " +
            "(نورپردازی، ترکیب‌بندی، لنز، بافت) اضافه کن، دقیقاً در همان چارچوب سبک بصری زیر، " +
            "بدون تغییر موضوع اصلی پرامپت."
    )
    appendLine()
    appendLine("سبک بصری پروژه: $styleTokens")
    appendLine()
    appendLine("پرامپت پیش‌نویس:")
    appendLine(templatePrompt)
    appendLine()
    appendLine("خروجی را دقیقاً در قالب JSON زیر بده، بدون هیچ توضیح اضافه قبل یا بعد از JSON:")
    appendLine("""{"imagePromptEn": "...", "imagePromptFa": "..."}""")
    appendLine()
    append(
        "imagePromptEn پرامپت نهایی و کامل به زبان انگلیسی است (این فیلد الزامی است و مستقیم " +
            "برای مدل تولید تصویر استفاده می‌شود). imagePromptFa ترجمه‌ی دقیق و کامل همان پرامپت " +
            "به زبان فارسی است (این فیلد هم الزامی است، فقط برای مرور کاربر فارسی‌زبان، هرگز به مدل " +
            "تولید تصویر فرستاده نمی‌شود). این دو فیلد باید کاملاً مستقل و ترجمه‌ی دقیق یکدیگر باشند — " +
            "هرگز دو زبان را در یک رشته قاطی/ترکیب نکن و هرگز هیچ‌کدام را خالی نگذار."
    )
}

/** Parse پاسخ خام AI به [ImagePromptAiResponse] — خطای Parse به‌جای Crash، Result.failure با پیام قابل‌فهم برمی‌گرداند. */
fun parseImagePromptAiResponse(rawResponseText: String): Result<ImagePromptAiResponse> {
    val parsed = runCatching { imagePromptAiJson.decodeFromString<ImagePromptAiResponse>(rawResponseText) }
    return parsed.exceptionOrNull()?.let {
        Result.failure(IOException("پاسخ سرویس AI به‌صورت JSON معتبر با دو کلید imagePromptEn/imagePromptFa قابل‌تفسیر نبود: ${it.message}"))
    } ?: parsed
}

/**
 * تابع سطح‌بالا — ساخت درخواست، ارسال با `sendToAiConnector` موجود (بدون تغییر در
 * آن تابع)، سپس Parse. ذخیره‌سازی نتیجه در imagePromptAi/imagePromptFaPreview کار
 * UI/ViewModel زیرقدم بعدی است، نه این تابع.
 */
suspend fun generateImagePromptWithAi(
    templatePrompt: String,
    styleTokens: String,
    profile: AiConnectorProfile,
    apiKey: String,
    engine: HttpClientEngine = OkHttp.create()
): Result<ImagePromptAiResponse> {
    val request = buildImagePromptAiRequest(templatePrompt, styleTokens)
    val sendResult = sendToAiConnector(profile, apiKey, request, engine)
    return sendResult.fold(
        onSuccess = { rawText -> parseImagePromptAiResponse(rawText) },
        onFailure = { Result.failure(it) }
    )
}
