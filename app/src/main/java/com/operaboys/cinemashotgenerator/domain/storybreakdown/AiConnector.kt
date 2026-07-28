package com.operaboys.cinemashotgenerator.domain.storybreakdown

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۰۱ب — AI Story Breakdown (بخش ب، مسیر ۲: AI Connector Profile)
// منبع حقیقت: docs/blueprints/01b-ai-story-breakdown.md (نسخه ۲)
//
// آخرین قدم این واحد — ادامه‌ی PromptBuilder.kt/ChunkCombiner.kt (قدم اول)،
// JsonDoctor.kt (قدم دوم)، StoryToDomainMapper.kt (قدم سوم).
//
// تصمیم مستند (docs/adr/035-unit01b-story-breakdown-step4-ai-connector.md، Option
// A): پیاده‌سازی واقعی HTTP Call با Ktor Client عمداً به یک قدم کاملاً جداگانه‌ی آینده
// موکول شد — دقیقاً طبق اجازه‌ی صریح خودِ بلوپرینت («می‌تواند به یک زیرقدم بعدی موکول
// شود اگر حجم واقعی بزرگ‌تر از انتظار بود»). این فایل فقط قرارداد/ساختار مسیر ۲ را
// پیاده می‌کند؛ sendToAiConnector عمداً TODO() است (یک body معتبر Kotlin که
// NotImplementedError پرتاب می‌کند، نه خطای کامپایل) — آزمایش‌شده در AiConnectorTest.kt.

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
 * پروفایل‌های آماده برای معروف‌ترین سرویس‌ها — عمداً خالی در این قدم. بلوپرینت خودش
 * این فهرست را «نمونه‌ی مفهومی» گذاشته بود («جزئیات دقیق request/response هر سرویس
 * در زمان پیاده‌سازی واقعی از مستندات رسمی گرفته شود، نه حدس»). افزودن پروفایل‌های
 * واقعی (Claude API، OpenAI API، ...) نیازمند بررسی مستندات رسمی هرکدام است — این کار
 * دامنه‌محور نیست (تحقیق API خارجی)، پس به قدم جداگانه‌ی آینده موکول شد (احتمالاً
 * هم‌زمان با پیاده‌سازی واقعی HTTP، طبق تصمیم Option A بالا).
 */
val BUILTIN_AI_CONNECTOR_PROFILES: List<AiConnectorProfile> = emptyList()

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

/**
 * ارسال درخواست واقعی به یک AI Connector — طبق تصمیم Option A (ADR-035)، پیاده‌سازی
 * واقعی HTTP Call با Ktor Client عمداً به یک قدم کاملاً جداگانه‌ی آینده موکول شد. این
 * امضا فقط قرارداد ورودی/خروجی بلوپرینت را تثبیت می‌کند.
 */
@Suppress("UNUSED_PARAMETER")
suspend fun sendToAiConnector(profile: AiConnectorProfile, apiKey: String, prompt: String): Result<String> {
    TODO("پیاده‌سازی واقعی HTTP Call با Ktor Client — قدم جداگانه‌ی آینده (docs/adr/035-unit01b-story-breakdown-step4-ai-connector.md)")
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
 * فقط «خطا رخ داد». طبق تصمیم Option A (ADR-035)، چون sendToAiConnector هنوز HTTP
 * واقعی ندارد، این تابع فقط ساختار Result.failure را بررسی می‌کند — قرارداد مسیر
 * جداگانه‌ی آینده وقتی HTTP واقعی اضافه شود همین است: پیام Exception باید غیرخالی و
 * معنادار باشد.
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
