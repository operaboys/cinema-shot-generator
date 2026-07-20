package com.operaboys.cinemashotgenerator.data.repository

import kotlinx.serialization.Serializable

// واحد ۱۵ — قدم ۳ (زیرقدم ۱): DTO های JSON برای فیلد AssetEntity.assetDataJson.
// منبع حقیقت شکل داده: domain/asset/AssetModels.kt (خوانده‌شده با grep کامل).
//
// NOTE درباره‌ی outfitOverride: تأیید شد (با grep مستقیم) که CharacterAsset واقعی
// هیچ فیلد outfitOverride ندارد — دقیقاً مثل یافته‌ی ADR-012 (واحد ۱۱). پس
// CharacterAssetDto هم چنین فیلدی ندارد؛ اگر در آینده enforceCharacterContinuity/
// selectOutfitForScene وصل شوند، outfitOverride باید پارامتر جداگانه‌ی تابع
// Repository/Wiring باشد، نه فیلدی در Entity/DTO — طبق همان الگوی ADR-012.
//
// NOTE: EvolutionTimeline/EvolutionEntry (domain/asset) در Scope این زیرقدم نیستند —
// CharacterAsset اصلاً فیلدی از این نوع ندارد (EvolutionTimeline یک نوع مستقل با
// characterId خودش است، نه بخشی از CharacterAsset)، پس چیزی برای سریالایز کردن به
// این‌ها متصل به CharacterAsset وجود ندارد.

@Serializable
data class HairDto(val color: String, val style: String, val length: String)

@Serializable
data class FacialFeaturesDto(val eyes: String, val distinctiveMarks: List<String> = emptyList())

@Serializable
data class PhysicalAppearanceDto(
    val ageRange: String,
    val gender: String,
    val height: String,
    val build: String,
    val hair: HairDto,
    val facialFeatures: FacialFeaturesDto
)

@Serializable
data class OutfitConditionDto(
    val weather: String? = null,
    val timeOfDay: String? = null,
    val locationType: String? = null
)

@Serializable
data class OutfitDto(
    val id: String,
    val name: String,
    val description: String,
    val isDefault: Boolean,
    val condition: OutfitConditionDto? = null
)

@Serializable
data class ExpressionDto(
    val id: String,
    val name: String,
    val description: String,
    val emotion: String,
    val isDefault: Boolean,
    val condition: OutfitConditionDto? = null
)

@Serializable
data class PropDto(val id: String, val name: String, val description: String, val category: String)

@Serializable
data class ContinuityRulesDto(
    val identityLock: Boolean = true,
    val appearanceLock: Boolean = true,
    val ageLock: Boolean = true,
    val antiDrift: Boolean = true,
    val allowedOverrides: List<String> = listOf("emotion", "pose", "outfit", "expression", "prop")
)

@Serializable
data class ReferenceImageDto(val localFilePath: String, val description: String)

@Serializable
data class CharacterAssetDto(
    val assetId: String,
    val assetType: String = "CHARACTER",
    val name: String,
    val physicalAppearance: PhysicalAppearanceDto,
    val outfits: List<OutfitDto>,
    val expressions: List<ExpressionDto> = emptyList(),
    val props: List<PropDto> = emptyList(),
    val continuityRules: ContinuityRulesDto = ContinuityRulesDto(),
    val referenceImages: List<ReferenceImageDto> = emptyList()
)

@Serializable
data class EnvironmentDto(val type: String, val size: String, val lightingCondition: String)

@Serializable
data class LocationAssetDto(
    val assetId: String,
    val assetType: String,
    val name: String,
    val description: String,
    val environment: EnvironmentDto,
    val timeCompatibility: List<String> = emptyList(),
    val weatherCompatibility: List<String> = emptyList(),
    val keyElements: List<String> = emptyList()
)
