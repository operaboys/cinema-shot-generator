package com.operaboys.cinemashotgenerator.domain.promptfinalization

import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelProfile
import com.operaboys.cinemashotgenerator.domain.outputdelivery.RenderedOutput

// واحد ۱۳ — Prompt Finalization Pipeline (تابع پل‌زننده به واحد ۱۴)
// منبع حقیقت: docs/blueprints/13-prompt-finalization.md، docs/blueprints/14-output-delivery.md
//
// جایگاه واقعی در Pipeline: PromptBlueprint (واحد ۱۱) → Renderer (واحد ۱۴، بخش الف)
// → همین تابع (واحد ۱۳: Cleaner + Token Calculator) → Output Composer (واحد ۱۴، بخش
// ب). طبق ADR-015، composeOutput واحد ۱۴ عمداً renderedOutputs را از بیرون تزریق
// می‌گیرد تا این واحد دقیقاً همین‌جا (بین render() و composeOutput) جا بگیرد.

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
    val (cleanedText, _) = cleanPrompt(rendered.formattedPrompt, options)
    val tokenCheck = checkTokenLimit(cleanedText, profile)
    val cleanedRendered = rendered.copy(formattedPrompt = cleanedText)
    return cleanedRendered to tokenCheck
}
