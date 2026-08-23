package com.operaboys.cinemashotgenerator.domain.asset

import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.visualidentity.combineStyles
import com.operaboys.cinemashotgenerator.domain.visualidentity.toStyleReference

// فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۲ از ۵ (ADR-132)
// کاملاً مستقل از موتور اصلی پرامپت ویدیو (PromptAssembly.kt/Renderer.kt) — خروجی
// این فایل هرگز نباید به آن مسیر راه پیدا کند. این پرامپت‌ها صرفاً به کاربر کمک
// می‌کنند خارج از این اپ (در یک مدل تولید تصویر) عکس مرجع یک Asset را بسازد.
//
// همه‌ی توابع خالص‌اند (بدون Side Effect/I/O) و فقط رشته برمی‌گردانند — ذخیره‌سازی
// در imagePromptQuick/imagePromptAi (زیرقدم ۱، ADR-131) کار UI/ViewModel زیرقدم‌های
// بعدی است، نه این فایل. خروجی همیشه انگلیسی است — ترجمه‌ی فارسی
// (imagePromptFaPreview) کار زیرقدم ۳ (AI) است.
//
// منبع Style Tokens: dna.coreIdentity (نه domain.visualidentity.StyleMatrix — آن
// data class یک Struct واسط جداست که در مسیر واقعی مصرف نمی‌شود؛ توابع Top-level
// combineStyles/toStyleReference مستقل از آن قابل استفاده‌اند، طبق تأییدشده‌ی
// دستور کار با grep روی ui/dna/DnaTabContent.kt خط ۱۷۲).

private fun styleTokensOf(projectDna: ProjectDna): String {
    val coreIdentity = projectDna.coreIdentity
    return combineStyles(
        primary = coreIdentity.dominantVisualStyle.toStyleReference(),
        secondary = coreIdentity.secondaryStyle?.toStyleReference(),
        influence = coreIdentity.influence
    )
}

/**
 * پرامپت شخصیت پایه/خنثی — بدون لباس داستانی، بدون Outfit خاص. برای مرجع «هویت
 * پایه‌ی بصری» شخصیت، مستقل از پرامپت هر Outfit (پایین‌تر).
 */
fun buildCharacterBaseImagePrompt(character: CharacterAsset, projectDna: ProjectDna): String {
    val parts = mutableListOf<String>()
    parts += styleTokensOf(projectDna)
    parts += "character reference sheet of ${character.name}"
    parts += character.physicalAppearance.toPromptString()
    character.defaultMood?.takeIf { it.isNotBlank() }?.let { parts += "$it expression" }
    parts += "plain neutral clothing"
    parts += "neutral standing pose, front-facing, plain studio background"
    return parts.joinToString(", ")
}

/**
 * پرامپت یک Outfit خاص — صریحاً به شخصیت پایه ارجاع می‌دهد تا مدل تولید تصویر
 * همان هویت را حفظ کند، سپس توصیف لباس + شرایط (در صورت وجود) را اضافه می‌کند.
 */
fun buildOutfitImagePrompt(outfit: Outfit, character: CharacterAsset, projectDna: ProjectDna): String {
    val parts = mutableListOf<String>()
    parts += styleTokensOf(projectDna)
    parts += "matching the established character reference exactly"
    parts += "${character.name} wearing ${outfit.name}"
    outfit.description.takeIf { it.isNotBlank() }?.let { parts += it }
    outfit.condition?.let { condition ->
        val conditionParts = listOfNotNull(condition.weather, condition.timeOfDay, condition.locationType)
        if (conditionParts.isNotEmpty()) parts += conditionParts.joinToString(", ")
    }
    return parts.joinToString(", ")
}

fun buildLocationImagePrompt(location: LocationAsset, projectDna: ProjectDna): String {
    val parts = mutableListOf<String>()
    parts += styleTokensOf(projectDna)
    location.description.takeIf { it.isNotBlank() }?.let { parts += it }
    parts += "${location.environment.type}, ${location.environment.size}, ${location.environment.lightingCondition}"
    if (location.keyElements.isNotEmpty()) parts += location.keyElements.joinToString(", ")
    location.basePrompt?.takeIf { it.isNotBlank() }?.let { parts += it }
    return parts.joinToString(", ")
}

fun buildObjectImagePrompt(objectAsset: ObjectAsset, projectDna: ProjectDna): String {
    val parts = mutableListOf<String>()
    parts += styleTokensOf(projectDna)
    objectAsset.description.takeIf { it.isNotBlank() }?.let { parts += it }
    parts += "${objectAsset.size}, ${objectAsset.materialAndColor}"
    objectAsset.specialTrait?.takeIf { it.isNotBlank() }?.let { parts += it }
    objectAsset.basePrompt?.takeIf { it.isNotBlank() }?.let { parts += it }
    return parts.joinToString(", ")
}
