package com.operaboys.cinemashotgenerator.domain.outputdelivery

import com.operaboys.cinemashotgenerator.domain.promptengine.PromptBlueprint
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۱۴ — Output Delivery System (بخش الف: Renderer)
// منبع حقیقت: docs/blueprints/14-output-delivery.md
//
// PromptBlueprint/StructuredParts از domain.promptengine (واحد ۱۱، از قبل پیاده‌شده)
// import شده‌اند — بدون بازتعریف. تمام فیلدهای structuredParts که این فایل می‌خواند
// (subjectDescription, sceneContext, shotDescription, cameraSpecs, lightingSpecs,
// environmentSpecs, styleModifiers, timelineBeats, audioDescription) دقیقاً با کد
// مفهومی بلوپرینت ۱۴ یکی هستند (تأیید شده با grep، بدون هیچ نگاشت اسم لازم).

/** قلب Renderer: تبدیل structured_parts به یک متن پایه بر اساس ترجیح ساختاری پروفایل. */
fun renderBlueprintToText(blueprint: PromptBlueprint, profile: ModelProfile): String {
    val parts = blueprint.structuredParts
    val segments = listOfNotNull(
        parts.subjectDescription, parts.sceneContext, parts.shotDescription,
        parts.cameraSpecs, parts.lightingSpecs, parts.environmentSpecs, parts.styleModifiers
    ).toMutableList()

    if (parts.timelineBeats != null && profile.capabilities.supportsVideo) segments += "Timeline: ${parts.timelineBeats}"
    if (parts.audioDescription != null && profile.capabilities.supportsVideo) segments += "Audio: ${parts.audioDescription}"

    var text = segments.joinToString(if (profile.format.structure == "paragraph") ". " else ", ")

    if (profile.capabilities.supportsWeightedTags) {
        for ((tag, weight) in blueprint.weightedEmphasis) {
            text = applyWeightSyntax(text, tag, weight, profile)
        }
    }
    return text
}

fun applyWeightSyntax(text: String, tag: String, weight: Float, profile: ModelProfile): String {
    return if (profile.platform == "midjourney") text.replace(tag, "$tag::$weight") else text
}

/** بهینه‌سازی نهایی: Rendering + کوتاه‌سازی خودکار در صورت عبور از max_prompt_length. */
fun optimizeForProfile(blueprint: PromptBlueprint, profile: ModelProfile): String {
    var optimized = renderBlueprintToText(blueprint, profile)
    if (optimized.length > profile.constraints.maxPromptLength) {
        optimized = optimized.take(profile.constraints.maxPromptLength - 3) + "..."
    }
    return optimized
}

data class RenderedOutput(val modelProfileId: String, val formattedPrompt: String, val language: String)

/** خروجی نهایی برای یک پروفایل خاص. */
fun render(blueprint: PromptBlueprint, profile: ModelProfile): RenderedOutput {
    val optimizedText = optimizeForProfile(blueprint, profile)
    val formatted = when (profile.format.type) {
        "command_string" -> "${profile.format.commandPrefix} $optimizedText"
        "plain_text" -> optimizedText // Universal — بدون فرمت‌دهی اضافه
        else -> optimizedText
    }
    return RenderedOutput(profile.profileId, formatted, language = "en")
}

/**
 * Rule «طول متن Render شده بیشتر از max_prompt_length». optimizeForProfile خودش کوتاه
 * می‌کند؛ این فقط هشدار گزارشی است — روی متنِ پیش از کوتاه‌سازی فراخوانی می‌شود.
 */
fun validatePromptLength(renderedText: String, profile: ModelProfile): ValidationIssue? {
    if (renderedText.length <= profile.constraints.maxPromptLength) return null
    return ValidationIssue(
        severity = Severity.WARNING,
        message = "متن Render شده (${renderedText.length} کاراکتر) از محدودیت پروفایل (${profile.constraints.maxPromptLength}) بیشتر است؛ به‌صورت خودکار کوتاه می‌شود"
    )
}

/**
 * Rule «استفاده از فیچر پشتیبانی‌نشده». renderBlueprintToText خودش این بخش‌ها را نادیده
 * می‌گیرد؛ این فقط هشدار گزارشی است.
 */
fun validateUnsupportedFeatureUsage(blueprint: PromptBlueprint, profile: ModelProfile): ValidationIssue? {
    val hasVideoOnlyContent = blueprint.structuredParts.timelineBeats != null ||
        blueprint.structuredParts.audioDescription != null
    if (!hasVideoOnlyContent || profile.capabilities.supportsVideo) return null
    return ValidationIssue(
        severity = Severity.WARNING,
        message = "این پروفایل از ویدیو پشتیبانی نمی‌کند؛ Timeline/Audio این Blueprint نادیده گرفته شدند"
    )
}
