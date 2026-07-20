package com.operaboys.cinemashotgenerator.domain.outputdelivery

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۱۴ — Output Delivery System (بخش الف: Model Profile Library)
// منبع حقیقت: docs/blueprints/14-output-delivery.md

data class ModelCapabilities(
    val supportsVideo: Boolean,
    val supportsImage: Boolean,
    val supportsWeightedTags: Boolean,
    val supportsImagePrompt: Boolean,
    val supportsNegativePrompt: Boolean
)

data class ModelConstraints(val maxPromptLength: Int, val maxTokens: Int)

data class ModelFormat(val type: String, val structure: String? = null, val commandPrefix: String? = null)

data class ModelProfile(
    val profileId: String,
    val platform: String,
    val capabilities: ModelCapabilities,
    val constraints: ModelConstraints,
    val format: ModelFormat,
    val optimizationRules: Map<String, Any> = emptyMap()
)

/**
 * پروفایل Universal/Default — همیشه در دسترس، Fallback برای هر پلتفرم ناشناخته یا جدید.
 * طبق بلوپرینت: بدون هیچ نحو خاص پلتفرمی، فقط توصیف متنی واضح از structured_parts.
 * JSON نمونه‌ی بلوپرینت فقط ۳ فیلد از ۵ فیلد ModelCapabilities را ذکر کرده
 * (supports_video/supports_image/supports_weighted_tags)؛ دو فیلد باقی‌مانده
 * (supportsImagePrompt/supportsNegativePrompt) با مقدار محافظه‌کارانه‌ی false پر شدند،
 * چون این پروفایل قابلیت خاصی برای آن‌ها ادعا نکرده.
 */
val universalDefaultProfile = ModelProfile(
    profileId = "universal_default",
    platform = "generic",
    capabilities = ModelCapabilities(
        supportsVideo = true,
        supportsImage = true,
        supportsWeightedTags = false,
        supportsImagePrompt = false,
        supportsNegativePrompt = false
    ),
    constraints = ModelConstraints(maxPromptLength = 2000, maxTokens = 500),
    format = ModelFormat(type = "plain_text", structure = "paragraph"),
    optimizationRules = mapOf("avoid_platform_specific_syntax" to true)
)

/** انتخاب پروفایل با Fallback به آخرین نسخه‌ی فعال همان پلتفرم. */
fun selectProfile(targetPlatform: String, profiles: List<ModelProfile>): ModelProfile {
    return profiles.firstOrNull { it.platform == targetPlatform }
        ?: profiles.first { it.profileId == "universal_default" }
}

/**
 * Rule «Profile برای پلتفرم درخواستی یافت نشد». selectProfile خودش Fallback می‌کند و
 * فقط وقتی کرش می‌کند که حتی universal_default هم در profiles نباشد؛ این تابع همان
 * حالت واقعاً بحرانی (Blocking) را از پیش گزارش می‌کند، بدون این‌که selectProfile را
 * صدا بزند و ریسک IllegalStateException بگیرد.
 */
fun validateProfileAvailability(targetPlatform: String, profiles: List<ModelProfile>): ValidationIssue? {
    val hasExactMatch = profiles.any { it.platform == targetPlatform }
    val hasUniversalFallback = profiles.any { it.profileId == "universal_default" }
    if (hasExactMatch || hasUniversalFallback) return null
    return ValidationIssue(
        severity = Severity.BLOCKING,
        message = "پروفایلی برای پلتفرم '$targetPlatform' یافت نشد و universal_default هم در دسترس نیست"
    )
}
