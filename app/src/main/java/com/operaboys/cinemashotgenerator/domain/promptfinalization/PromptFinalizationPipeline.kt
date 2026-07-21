package com.operaboys.cinemashotgenerator.domain.promptfinalization

import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelProfile
import com.operaboys.cinemashotgenerator.domain.outputdelivery.RenderedOutput
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// واحد ۱۳ — Prompt Finalization Pipeline (تابع پل‌زننده به واحد ۱۴)
// منبع حقیقت: docs/blueprints/13-prompt-finalization.md، docs/blueprints/14-output-delivery.md
//
// جایگاه واقعی در Pipeline: PromptBlueprint (واحد ۱۱) → Renderer (واحد ۱۴، بخش الف)
// → همین تابع (واحد ۱۳: Cleaner + Token Calculator) → Output Composer (واحد ۱۴، بخش
// ب). طبق ADR-015، composeOutput واحد ۱۴ عمداً renderedOutputs را از بیرون تزریق
// می‌گیرد تا این واحد دقیقاً همین‌جا (بین render() و composeOutput) جا بگیرد.
//
// رفع محدودیت مستندشده در ADR-025/026: cleanPrompt (بخش الف واحد ۱۳) فرض می‌کند
// ورودی یک جمله‌ی متن آزاد است — روی یک رشته‌ی JSON واقعی (از Renderer.buildJsonPrompt،
// واحد ۱۴) اجرای مستقیم آن می‌تواند ساختار را خراب کند (بدترینش: applyFinalPolish
// بعد از `}` پایانی یک نقطه اضافه می‌کند → JSON نامعتبر). جزئیات کامل تصمیم در
// docs/adr/026-unit13-json-aware-finalization-deviations.md.

/**
 * ورودی/خروجی این تابع عمداً RenderedOutput واقعی واحد ۱۴ است (نه یک نوع محلی جدید):
 * خروجی‌اش باید مستقیماً در لیست renderedOutputs که به composeOutput داده می‌شود
 * قابل استفاده باشد، بدون هیچ تبدیل میانی. فقط formattedPrompt تغییر می‌کند (با
 * .copy() — بدون جهش/mutation)؛ modelProfileId/language دست‌نخورده می‌مانند چون
 * Cleaning متن را تغییر می‌دهد، نه این‌که مدل هدف را عوض کند.
 *
 * TokenCheckResult جداگانه (نه داخل RenderedOutput) برگردانده می‌شود چون طبق بلوپرینت
 * ۱۴، RenderedOutput فقط سه فیلد (modelProfileId, formattedPrompt, language) دارد —
 * افزودن فیلد به آن نوع خارج از Scope این قدم بود (فقط مصرف واحد ۱۴، بدون تغییرش).
 *
 * CleaningReport میانی این تابع نگه داشته نمی‌شود (فقط متن و نتیجه‌ی توکن لازم است
 * برای اتصال به composeOutput)؛ در صورت نیاز به جزئیات تمیزکاری، cleanPrompt مستقیماً
 * قابل فراخوانی است.
 */
fun finalizePrompt(
    rendered: RenderedOutput,
    profile: ModelProfile,
    options: CleaningOptions = CleaningOptions()
): Pair<RenderedOutput, TokenCheckResult> {
    val cleanedText = if (profile.format.type == "json") {
        cleanJsonPromptValues(rendered.formattedPrompt, options)
            ?: cleanPrompt(rendered.formattedPrompt, options).first
    } else {
        cleanPrompt(rendered.formattedPrompt, options).first
    }
    val tokenCheck = checkTokenLimit(cleanedText, profile)
    val cleanedRendered = rendered.copy(formattedPrompt = cleanedText)
    return cleanedRendered to tokenCheck
}

/**
 * نسخه‌ی JSON-آگاه cleanPrompt: به‌جای اجرای Cleaner روی کل رشته‌ی JSON (که ساختار
 * را خراب می‌کند)، JSON را Parse می‌کند، cleanPrompt را فقط روی مقدار هر فیلد رشته‌ای
 * اجرا می‌کند (با applyFinalPolish=false — Phase 5 برای یک مقدار تکی، نه یک جمله‌ی
 * کامل، بی‌معناست)، و دوباره Serialize می‌کند. فیلدهای غیر-رشته‌ای (مثل Object تودرتوی
 * weightedEmphasis که مقادیرش Float هستند) دست‌نخورده کپی می‌شوند.
 *
 * اگر rendered.formattedPrompt واقعاً JSON معتبر نباشد (مثلاً یک RenderedOutput
 * دستی/قدیمی که فرمت پروفایلش json است ولی متنش آزاد است — دقیقاً وضعیت تستِ موجود
 * PromptFinalizationPipelineTest)، این تابع null برمی‌گرداند تا finalizePrompt به
 * مسیر متن آزاد قبلی Fallback کند، نه این‌که کرش کند.
 *
 * محدودیت شناخته‌شده (پذیرفته‌شده، مستند در ADR-026): چون هر فیلد جدا Clean می‌شود،
 * تشخیص تضاد (detectConflicts) بین‌فیلدی را از دست می‌دهد (مثلاً «آفتابی» در یک فیلد
 * و «بارانی» در فیلدی دیگر دیگر با هم دیده نمی‌شوند) — فقط تضاد درون یک فیلد واحد
 * تشخیص داده می‌شود. رفع این محدودیت خارج از Scope این قدم است.
 */
private fun cleanJsonPromptValues(rawJson: String, options: CleaningOptions): String? {
    val root = runCatching { Json.parseToJsonElement(rawJson) }.getOrNull() as? JsonObject ?: return null
    val fieldOptions = options.copy(applyFinalPolish = false)
    val cleanedEntries = root.mapValues { (_, value) ->
        if (value is JsonPrimitive && value.isString) {
            JsonPrimitive(cleanPrompt(value.content, fieldOptions).first)
        } else {
            value // weightedEmphasis و هر مقدار غیر-رشته‌ای دیگر دست‌نخورده می‌ماند
        }
    }
    return JsonObject(cleanedEntries).toString()
}
