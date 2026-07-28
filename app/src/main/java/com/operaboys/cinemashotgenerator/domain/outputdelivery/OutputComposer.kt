package com.operaboys.cinemashotgenerator.domain.outputdelivery

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import java.util.UUID

// واحد ۱۴ — Output Delivery System (بخش ب: Output Composer)
// منبع حقیقت: docs/blueprints/14-output-delivery-v2.md (نسخه ۳)
//
// طبق جایگاه واقعی در Pipeline (PromptBlueprint → Renderer این واحد → Prompt
// Finalization واحد ۱۳، هنوز پیاده نشده → Output Composer): renderedOutputs همیشه از
// بیرون تزریق می‌شود، چه خام از render() این فایل، چه بعداً Clean‌شده از واحد ۱۳؛
// composeOutput خودش هرگز render() را صدا نمی‌زند — دقیقاً طبق کد مفهومی بلوپرینت که
// renderedOutputs را پارامتر گرفته، نه خودش تولید کرده.

data class OutputPackage(
    val outputId: String,
    val shotId: String,
    val promptBlueprintId: String,
    val bilingualPrompts: BilingualPrompts,
    val renderedOutputs: List<RenderedOutput>,
    val exportFiles: List<ExportFile>
)

data class BilingualPrompts(val enVersion: String, val faVersion: String)
data class ExportFile(val filename: String, val content: String, val mimeType: String)

/**
 * فقط بسته‌بندی — بدون تغییر محتوای renderedOutputs دریافتی (Rule «نقض اصل فقط
 * بسته‌بندی» بلوپرینت یک تضمین ساختاری است، نه یک Runtime Check جداگانه: این تابع
 * هیچ‌جا rendered.formattedPrompt را تغییر نمی‌دهد، فقط برای ساخت ExportFile می‌خواند
 * — مشابه الگوی «تضمین ساختاری، نه Runtime» در ADR-006 واحد ۰۵).
 */
fun composeOutput(
    shotId: String,
    promptBlueprintId: String,
    renderedOutputs: List<RenderedOutput>,
    bilingualPrompts: BilingualPrompts,
    idProvider: () -> String = ::defaultOutputPackageId
): OutputPackage {
    require(renderedOutputs.isNotEmpty()) { "حداقل یک rendered_output لازم است" }

    val exportFiles = mutableListOf(
        ExportFile("${shotId}_prompt_en.txt", bilingualPrompts.enVersion, "text/plain"),
        ExportFile("${shotId}_prompt_fa.txt", bilingualPrompts.faVersion, "text/plain")
    )
    renderedOutputs.forEach { rendered ->
        exportFiles += ExportFile("${shotId}_${rendered.modelProfileId}.txt", rendered.formattedPrompt, "text/plain")
    }

    return OutputPackage(idProvider(), shotId, promptBlueprintId, bilingualPrompts, renderedOutputs, exportFiles)
}

/**
 * Rule «یکی از نسخه‌های زبانی موجود نیست». BilingualPrompts دو فیلد غیر-Null String
 * دارد (نه Nullable)؛ خالی/Blank بودن معادل «موجود نیست» است.
 */
fun validateBilingualCompleteness(bilingualPrompts: BilingualPrompts): ValidationIssue? {
    if (bilingualPrompts.enVersion.isNotBlank() && bilingualPrompts.faVersion.isNotBlank()) return null
    return ValidationIssue(
        severity = Severity.WARNING,
        message = "یکی از نسخه‌های زبانی (EN/FA) موجود نیست"
    )
}

private fun defaultOutputPackageId(): String =
    "output_" + UUID.randomUUID().toString().replace("-", "").take(12)
