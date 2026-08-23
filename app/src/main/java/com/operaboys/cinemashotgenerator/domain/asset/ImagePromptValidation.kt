package com.operaboys.cinemashotgenerator.domain.asset

import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyleCategory
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۲ از ۵ (ADR-132)
// کاملاً مستقل از موتور اصلی پرامپت ویدیو و از تجمیع‌کننده‌ی اصلی
// (domain/validation/ValidationAggregator.kt) — این قوانین هرگز در آن مسیر
// فراخوانی نمی‌شوند، چون این پرامپت به موتور اصلی وارد نمی‌شود و تصمیم نهایی
// همیشه با کاربر است. طبق دستور کار صریح، همه‌ی قوانین این فایل فقط
// Severity.WARNING تولید می‌کنند — هیچ‌کدام BLOCKING نیست.

private const val MIN_IMAGE_PROMPT_LENGTH = 20
private const val MAX_IMAGE_PROMPT_LENGTH = 800

// Heuristic ساده و کوچک — نه NLP کامل، فقط تشخیص تناقض آشکار سبک فوتورئال در
// برابر سبک انیمیشنی پروژه (و برعکس).
private val PHOTOREALISTIC_KEYWORDS = listOf("photorealistic", "photo real", "photo-real")
private val ANIMATED_KEYWORDS = listOf("anime", "cartoon", "animated")

private fun validatePromptLength(prompt: String): ValidationIssue? = when {
    prompt.length < MIN_IMAGE_PROMPT_LENGTH ->
        ValidationIssue(Severity.WARNING, message = "پرامپت تولیدشده خیلی کوتاه است — ممکن است جزئیات کافی برای یک تصویر مرجع مناسب نداشته باشد")
    prompt.length > MAX_IMAGE_PROMPT_LENGTH ->
        ValidationIssue(Severity.WARNING, message = "پرامپت تولیدشده خیلی بلند است — برخی مدل‌های تولید تصویر ممکن است بخشی از آن را نادیده بگیرند")
    else -> null
}

/**
 * تناقض کلیدواژه‌ای ساده بین سبک پروژه و متن آزاد کاربر — بررسی سبک، نه هوش
 * مصنوعی. جهت اول: سبک انیمیشنی (۲D/۳D) + کلیدواژه‌ی «فوتورئال» در متن. جهت دوم
 * («برعکس»، طبق دستور کار): سبک PHOTOREALISTIC + کلیدواژه‌ی انیمیشنی در متن.
 */
private fun validateStyleKeywordConflict(dominantVisualStyle: VisualStyle, freeText: String): ValidationIssue? {
    if (freeText.isBlank()) return null
    val lower = freeText.lowercase()
    val isAnimatedCategory = dominantVisualStyle.category == VisualStyleCategory.ANIMATION_2D ||
        dominantVisualStyle.category == VisualStyleCategory.ANIMATION_3D
    if (isAnimatedCategory && PHOTOREALISTIC_KEYWORDS.any { lower.contains(it) }) {
        return ValidationIssue(
            Severity.WARNING,
            message = "متن آزاد شامل کلیدواژه‌ی سبک فوتورئال است اما سبک بصری پروژه انیمیشنی است — احتمال تناقض بصری"
        )
    }
    if (dominantVisualStyle == VisualStyle.PHOTOREALISTIC && ANIMATED_KEYWORDS.any { lower.contains(it) }) {
        return ValidationIssue(
            Severity.WARNING,
            message = "متن آزاد شامل کلیدواژه‌ی سبک انیمیشنی است اما سبک بصری پروژه فوتورئال است — احتمال تناقض بصری"
        )
    }
    return null
}

fun validateCharacterImagePromptInputs(character: CharacterAsset, projectDna: ProjectDna, generatedPrompt: String): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    validatePromptLength(generatedPrompt)?.let { issues += it }
    if (character.physicalAppearance.ageRange.isBlank()) {
        issues += ValidationIssue(Severity.WARNING, field = "physical_appearance", message = "بازه‌ی سنی این شخصیت خالی است — ظاهر پایه ممکن است ناقص به نظر برسد")
    }
    val freeText = listOfNotNull(character.basePrompt, character.physicalAppearance.physicalFeatures).joinToString(" ")
    validateStyleKeywordConflict(projectDna.coreIdentity.dominantVisualStyle, freeText)?.let { issues += it }
    return issues
}

fun validateOutfitImagePromptInputs(outfit: Outfit, projectDna: ProjectDna, generatedPrompt: String): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    validatePromptLength(generatedPrompt)?.let { issues += it }
    validateStyleKeywordConflict(projectDna.coreIdentity.dominantVisualStyle, outfit.description)?.let { issues += it }
    return issues
}

fun validateLocationImagePromptInputs(location: LocationAsset, projectDna: ProjectDna, generatedPrompt: String): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    validatePromptLength(generatedPrompt)?.let { issues += it }
    if (location.description.isBlank() || location.environment.type.isBlank()) {
        issues += ValidationIssue(Severity.WARNING, field = "description", message = "توصیف یا نوع محیط این مکان خالی است — عکس مرجع ممکن است بی‌جزئیات باشد")
    }
    val freeText = listOfNotNull(location.basePrompt, location.description).joinToString(" ")
    validateStyleKeywordConflict(projectDna.coreIdentity.dominantVisualStyle, freeText)?.let { issues += it }
    return issues
}

fun validateObjectImagePromptInputs(objectAsset: ObjectAsset, projectDna: ProjectDna, generatedPrompt: String): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    validatePromptLength(generatedPrompt)?.let { issues += it }
    if (objectAsset.materialAndColor.isBlank()) {
        issues += ValidationIssue(Severity.WARNING, field = "material_and_color", message = "جنس و رنگ این شیء مشخص نشده است — عکس مرجع ممکن است بی‌جزئیات باشد")
    }
    val freeText = listOfNotNull(objectAsset.basePrompt, objectAsset.specialTrait).joinToString(" ")
    validateStyleKeywordConflict(projectDna.coreIdentity.dominantVisualStyle, freeText)?.let { issues += it }
    return issues
}
