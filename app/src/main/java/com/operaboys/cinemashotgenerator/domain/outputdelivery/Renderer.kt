package com.operaboys.cinemashotgenerator.domain.outputdelivery

import com.operaboys.cinemashotgenerator.domain.promptengine.PromptBlueprint
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

// واحد ۱۴ — Output Delivery System (بخش الف: Renderer)
// منبع حقیقت: docs/blueprints/14-output-delivery.md
//
// PromptBlueprint/StructuredParts از domain.promptengine (واحد ۱۱، از قبل پیاده‌شده)
// import شده‌اند — بدون بازتعریف. تمام فیلدهای structuredParts که این فایل می‌خواند
// (subjectDescription, sceneContext, shotDescription, cameraSpecs, lightingSpecs,
// environmentSpecs, styleModifiers, timelineBeats, audioDescription) دقیقاً با کد
// مفهومی بلوپرینت ۱۴ یکی هستند (تأیید شده با grep، بدون هیچ نگاشت اسم لازم).
//
// رفع دو محدودیت شناخته‌شده‌ی ADR-021 (رندر JSON واقعی + وزن‌دهی واقعی SD)؛ جزئیات
// کامل تصمیمات در docs/adr/025-unit14-renderer-json-and-weighting-deviations.md.
// import مستقیم kotlinx.serialization.json در لایه‌ی domain/ طبق تصمیم صریح معمار
// همین قدم است (نه یک الگوی عمومی جدید برای این پروژه).

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

/**
 * نحو وزن‌دهی مخصوص هر پلتفرم. فقط دو پلتفرم فعلی نحو واقعی دارند — میجرنی
 * (`tag::weight`) و Stable Diffusion (`(tag:weight)`، نحو معروف A1111/ComfyUI)؛
 * بقیه‌ی پلتفرم‌ها (حتی اگر supportsWeightedTags=true باشند) متن را بدون تغییر
 * برمی‌گردانند. یک `when` صریح با `else` — نه یک Map/Registry عمومی — چون فقط دو
 * مورد واقعی وجود دارد؛ افزودن پلتفرم سوم در آینده فقط یک `case` جدید می‌خواهد.
 */
fun applyWeightSyntax(text: String, tag: String, weight: Float, profile: ModelProfile): String {
    return when (profile.platform) {
        "midjourney" -> text.replace(tag, "$tag::$weight")
        "stable_diffusion" -> text.replace(tag, "($tag:$weight)")
        else -> text
    }
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

/**
 * برای پروفایل‌های `format.type == "json"`، یک JSON واقعی و معتبر می‌سازد — مستقیماً
 * از فیلدهای ساختاریافته‌ی `blueprint.structuredParts` (نه از متن مسطح‌شده‌ی
 * `renderBlueprintToText`)، چون بلوپرینت مفهومی خودِ واحد ۱۴ هم هیچ نمونه‌ی واقعی
 * JSON خروجی نداد (فقط JSON بودن *تعریف پروفایل* را مثال زده بود، نه پرامپت نهایی) —
 * این ساختار یک طراحی معقول و مستقل است، نه استخراج از سند.
 *
 * weightedEmphasis فقط وقتی `profile.capabilities.supportsWeightedTags` باشد، به‌عنوان
 * یک object جدا (نه Inline در متن) اضافه می‌شود. تصمیم: applyWeightSyntax (که یک
 * جایگزینی رشته‌ای درون متن است) اصلاً روی این فیلدهای JSON اجرا نمی‌شود — برای یک
 * پروفایل JSON، «وزن» باید یک پارامتر ساختاریافته‌ی جدا باشد، نه متن دستکاری‌شده؛ این
 * هم با روح API های واقعی JSON سازگارتر است. فعلاً هیچ پروفایل موجودی هم‌زمان
 * json+supportsWeightedTags=true ندارد (تأیید با grep) — این مسیر برای پروفایل‌های
 * آینده‌ی این ترکیب مستند و آماده است، نه یک باگ فعلی.
 *
 * **تصمیم Truncation (ADR-026):** برخلاف مسیر `optimizeForProfile` (که رشته‌ی متن
 * مسطح را در صورت عبور از `maxPromptLength` با `.take(n) + "..."` کوتاه می‌کند)، این
 * تابع خروجی JSON را هرگز کوتاه نمی‌کند — کوتاه‌سازی رشته‌ای روی یک JSON معتبر تقریباً
 * همیشه آن را نامعتبر می‌کند. `validatePromptLength` موجود (بدون تغییر، چون فقط طول
 * کاراکتری رشته را می‌سنجد و به شکل داخلی متن کاری ندارد) همچنان به‌درستی روی طول
 * خروجی JSON هم کار می‌کند و Warning می‌دهد — این عمداً تنها مکانیزم هشدار برای طول
 * پروفایل‌های JSON است، بدون هیچ کوتاه‌سازی خودکار مخرب.
 */
private fun buildJsonPrompt(blueprint: PromptBlueprint, profile: ModelProfile): String {
    val parts = blueprint.structuredParts
    return buildJsonObject {
        put("subject", parts.subjectDescription)
        put("scene", parts.sceneContext)
        put("shot", parts.shotDescription)
        put("camera", parts.cameraSpecs)
        put("lighting", parts.lightingSpecs)
        parts.environmentSpecs?.let { put("environment", it) }
        put("style", parts.styleModifiers)
        if (profile.capabilities.supportsVideo) {
            parts.timelineBeats?.let { put("timeline", it) }
            parts.audioDescription?.let { put("audio", it) }
        }
        if (profile.capabilities.supportsWeightedTags && blueprint.weightedEmphasis.isNotEmpty()) {
            putJsonObject("weightedEmphasis") {
                blueprint.weightedEmphasis.forEach { (tag, weight) -> put(tag, weight) }
            }
        }
    }.toString()
}

/** خروجی نهایی برای یک پروفایل خاص. */
fun render(blueprint: PromptBlueprint, profile: ModelProfile): RenderedOutput {
    val optimizedText = optimizeForProfile(blueprint, profile)
    val formatted = when (profile.format.type) {
        "command_string" -> "${profile.format.commandPrefix} $optimizedText"
        "plain_text" -> optimizedText // Universal — بدون فرمت‌دهی اضافه
        "json" -> buildJsonPrompt(blueprint, profile)
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
