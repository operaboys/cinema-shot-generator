package com.operaboys.cinemashotgenerator.domain.asset

// واحد ۰۶ — Asset & Continuity (ساختار داده)
// منبع حقیقت: docs/blueprints/06-asset-and-continuity.md

/**
 * سه نوع Asset طبق بلوپرینت: Character و Location/Object (بخش «Location / Object Asset»
 * یک ساختار JSON مشترک دارد؛ به همین دلیل کلاس جداگانه‌ای برای Object ساخته نشد —
 * LocationAsset هر دو مقدار LOCATION و OBJECT را پوشش می‌دهد).
 */
enum class AssetType { CHARACTER, LOCATION, OBJECT }

data class Hair(
    val color: String,
    val style: String,
    val length: String
)

data class FacialFeatures(
    val eyes: String,
    val distinctiveMarks: List<String> = emptyList()
)

data class PhysicalAppearance(
    val ageRange: String,
    val gender: String,
    val height: String,
    val build: String,
    val hair: Hair,
    val facialFeatures: FacialFeatures
)

/** شرط اختیاری برای انتخاب خودکار — هم برای Outfit و هم برای Expression استفاده می‌شود. */
data class OutfitCondition(
    val weather: String? = null,
    val timeOfDay: String? = null,
    val locationType: String? = null
)

data class Outfit(
    val id: String,
    val name: String,
    val description: String,
    val isDefault: Boolean,
    val condition: OutfitCondition? = null
)

/**
 * condition و isDefault در نمونه‌ی JSON بلوپرینت برای Expression نیامده بودند؛
 * افزودن این دو فیلد تأییدشده است تا selectExpressionForScene (طبق جمله‌ی بلوپرینت
 * «همین منطق برای expressions هم قابل استفاده است») معنای واقعی داشته باشد.
 * (ثبت‌شده در docs/adr/003-unit06-asset-continuity-deviations.md)
 */
data class Expression(
    val id: String,
    val name: String,
    val description: String,
    val emotion: String,
    val isDefault: Boolean,
    val condition: OutfitCondition? = null
)

data class Prop(
    val id: String,
    val name: String,
    val description: String,
    val category: String
)

/**
 * Hard Lock مطلق — برخلاف Soft Lock واحد ۰۲ (DNA Manager)، این فیلدها هیچ مسیر
 * Override ای ندارند. جزئیات: docs/governance/override-policy.md (حذف عمدی «سطح Character»).
 */
data class ContinuityRules(
    val identityLock: Boolean = true,
    val appearanceLock: Boolean = true,
    val ageLock: Boolean = true,
    val antiDrift: Boolean = true,
    val allowedOverrides: List<String> = listOf("emotion", "pose", "outfit", "expression", "prop")
)

data class ReferenceImage(
    val localFilePath: String,
    val description: String
)

/**
 * ساختار داده‌ی خام Evolution Timeline — بدون منطق اعمال آن.
 * منطق واقعی (اعمال/ثبت تغییرات در طول زمان) به واحد ۱۲ (State & Versioning) وابسته
 * است و در این قدم پیاده نشده — خارج از Scope طبق دستور کار.
 */
data class EvolutionEntry(
    val fromShot: String,
    val toShot: String,
    val changes: Map<String, List<String>>,
    val reason: String
)

data class EvolutionTimeline(
    val characterId: String,
    val entries: List<EvolutionEntry> = emptyList()
)

/** physicalAppearance غیر-nullable است — Rule 2 (Character باید physical_appearance داشته باشد) در سطح نوع تضمین می‌شود. */
data class CharacterAsset(
    val assetId: String,
    val assetType: AssetType = AssetType.CHARACTER,
    val name: String,
    val physicalAppearance: PhysicalAppearance,
    val outfits: List<Outfit>,
    val expressions: List<Expression> = emptyList(),
    val props: List<Prop> = emptyList(),
    val continuityRules: ContinuityRules = ContinuityRules(),
    val referenceImages: List<ReferenceImage> = emptyList()
)

data class Environment(
    val type: String,
    val size: String,
    val lightingCondition: String
)

/** پوشش‌دهنده‌ی هر دو AssetType.LOCATION و AssetType.OBJECT (ساختار JSON یکسان در بلوپرینت). */
data class LocationAsset(
    val assetId: String,
    val assetType: AssetType,
    val name: String,
    val description: String,
    val environment: Environment,
    val timeCompatibility: List<String> = emptyList(),
    val weatherCompatibility: List<String> = emptyList(),
    val keyElements: List<String> = emptyList()
)
