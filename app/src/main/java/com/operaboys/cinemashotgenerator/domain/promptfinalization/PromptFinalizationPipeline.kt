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
 * MIGRATED (رفع G17 ممیزی post-Unit16، docs/adr/060-...): این تابع تا این قدم هیچ
 * فراخوان واقعی‌ای در ui/ نداشت — OutputDeliveryViewModel هرگز آن را صدا نمی‌زد، پس
 * بج «Cleaned · Finalized» صفحه‌ی Output Delivery همیشه نمایش داده می‌شد بدون اینکه
 * پاک‌سازی/بررسی توکن واقعی اتفاق بیفتد. برای وصل‌شدن به Rule های
 * `validateConflictsResolved`/`validateCompressionRatio` (هر دو در PromptCleaner.kt،
 * که یکی به کل CleaningReport نیاز دارد)، خروجی این تابع از Pair به FinalizationResult
 * تغییر کرد تا CleaningReport هم (نه فقط متن/TokenCheckResult) بیرون بدهد — نه یک
 * Breaking Change واقعی، چون این تابع تا این لحظه صفر فراخوان‌کننده در کل پروژه
 * داشت (فقط `PromptFinalizationPipelineTest.kt` که در همین قدم به‌روزرسانی شد).
 */
data class FinalizationResult(
    val rendered: RenderedOutput,
    val tokenCheck: TokenCheckResult,
    val cleaningReport: CleaningReport
)

fun finalizePrompt(
    rendered: RenderedOutput,
    profile: ModelProfile,
    options: CleaningOptions = CleaningOptions()
): FinalizationResult {
    val (cleanedText, report) = if (profile.format.type == "json") {
        cleanJsonPromptValues(rendered.formattedPrompt, options)
            ?: cleanPrompt(rendered.formattedPrompt, options)
    } else {
        cleanPrompt(rendered.formattedPrompt, options)
    }
    val tokenCheck = checkTokenLimit(cleanedText, profile)
    val cleanedRendered = rendered.copy(formattedPrompt = cleanedText)
    return FinalizationResult(cleanedRendered, tokenCheck, report)
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
 *
 * MIGRATED (رفع G17 ممیزی post-Unit16): علاوه‌بر متن Clean‌شده، یک CleaningReport
 * تجمیعی هم برمی‌گرداند (جمع شمارنده‌های هر فیلد + طول کل رشته‌ی JSON قبل/بعد) —
 * لازم برای `validateCompressionRatio` که به یک CleaningReport واحد نیاز دارد، نه
 * فیلد به فیلد. `conflictsDetected`/`redundancyRemoved`/`stopWordsRemoved` مجموع
 * واقعی همه‌ی فیلدهاست؛ compressionRatio از طول کل JSON (نه مجموع طول فیلدها)
 * محاسبه می‌شود چون همان چیزی است که واقعاً به کاربر/مدل تحویل داده می‌شود.
 */
private fun cleanJsonPromptValues(rawJson: String, options: CleaningOptions): Pair<String, CleaningReport>? {
    val root = runCatching { Json.parseToJsonElement(rawJson) }.getOrNull() as? JsonObject ?: return null
    val fieldOptions = options.copy(applyFinalPolish = false)
    var conflictsDetected = 0
    val conflictsResolved = mutableListOf<String>()
    var redundancyRemoved = 0
    var stopWordsRemoved = 0
    val cleanedEntries = root.mapValues { (_, value) ->
        if (value is JsonPrimitive && value.isString) {
            val (cleanedValue, fieldReport) = cleanPrompt(value.content, fieldOptions)
            conflictsDetected += fieldReport.conflictsDetected
            conflictsResolved += fieldReport.conflictsResolved
            redundancyRemoved += fieldReport.redundancyRemoved
            stopWordsRemoved += fieldReport.stopWordsRemoved
            JsonPrimitive(cleanedValue)
        } else {
            value // weightedEmphasis و هر مقدار غیر-رشته‌ای دیگر دست‌نخورده می‌ماند
        }
    }
    val cleanedJson = JsonObject(cleanedEntries).toString()
    val report = CleaningReport(
        conflictsDetected = conflictsDetected,
        conflictsResolved = conflictsResolved,
        redundancyRemoved = redundancyRemoved,
        stopWordsRemoved = stopWordsRemoved,
        originalLength = rawJson.length,
        cleanedLength = cleanedJson.length,
        compressionRatio = (rawJson.length - cleanedJson.length).toFloat() / rawJson.length
    )
    return cleanedJson to report
}
