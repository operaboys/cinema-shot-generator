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
//
// MIGRATION بخش دوم (docs/adr/029-unit06-continuity-tiers-migration-part1.md):
// characterTier/subtype در سطح DTO عمداً String با یک پیش‌فرض محافظه‌کارانه دارند
// (نه enum غیر-nullable مثل نوع دامنه) — این دقیقاً همان تمایزی است که در ADR-029
// مستند شده: نوع دامنه‌ی Kotlin این دو فیلد را الزامی نگه می‌دارد (هر ساخت جدید باید
// صریح تصمیم بگیرد)، اما DTO مسئول Backward Compatibility داده‌ی واقعاً سریالایز‌شده‌ی
// قدیمی روی دیسک است (رکوردهایی که این فیلدها را نداشتند) — طبق پیشنهاد صریح خودِ
// بلوپرینت (character_tier → MAIN، subtype → GENERAL_PROP، هر دو محافظه‌کارترین حالت).
//
// MIGRATION (docs/adr/031-unit06-physical-appearance-gender-migration.md): همان الگو
// برای gender تکرار شد — در DTO همچنان String می‌ماند (نه Gender enum غیر-nullable)،
// چون داده‌ی قدیمی سریالایز‌شده ممکن است gender را با حروف کوچک ("male") ذخیره کرده
// باشد؛ نگاشت به enum با uppercase-normalize در DnaAssetMappers.kt انجام می‌شود. سایر
// فیلدها (height/build/hair/facialFeatures) اکنون nullable هستند — مستقیماً هم‌شکل با
// نوع دامنه، چون این فیلدها همیشه اختیاری بوده‌اند و «Backward Compatibility با مقدار
// پیش‌فرض جعلی» برایشان معنا ندارد (nullable→nullable طبیعی‌ترین نگاشت است، برخلاف
// characterTier/gender که واقعاً enum غیر-nullable با پیش‌فرض لازم دارند).

@Serializable
data class HairDto(val color: String, val style: String, val length: String)

@Serializable
data class FacialFeaturesDto(val eyes: String, val distinctiveMarks: List<String> = emptyList())

@Serializable
data class PhysicalAppearanceDto(
    val ageRange: String,
    val gender: String,
    val height: String? = null,
    val build: String? = null,
    val hair: HairDto? = null,
    val physicalFeatures: String? = null,
    val facialFeatures: FacialFeaturesDto? = null
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
    val condition: OutfitConditionDto? = null,
    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۱ از ۵ (ADR-131): هم‌شکل مستقیم با
    // domain/asset/AssetModels.kt — Nullable با پیش‌فرض null، بدون Room Migration
    // (این DTO فقط داخل assetDataJson به‌صورت JSON خام ذخیره می‌شود).
    val imagePromptQuick: String? = null,
    val imagePromptAi: String? = null,
    val imagePromptFaPreview: String? = null,
    val imagePromptGeneratedAt: Long? = null
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
    val characterTier: String = "MAIN",
    val name: String,
    val physicalAppearance: PhysicalAppearanceDto,
    val outfits: List<OutfitDto>,
    val expressions: List<ExpressionDto> = emptyList(),
    val props: List<PropDto> = emptyList(),
    val defaultMood: String? = null,
    val basePrompt: String? = null,
    val continuityRules: ContinuityRulesDto = ContinuityRulesDto(),
    val continuityLockLevel: String? = null,
    val referenceImages: List<ReferenceImageDto> = emptyList(),
    // سیستم Preview دوزبانه‌ی پرامپت — قدم ۲ از ۳ زیرقدم (ADR-122): هم‌شکل
    // مستقیم با domain/asset/AssetModels.kt — Nullable با پیش‌فرض null،
    // بدون نیاز به Room Migration (این DTO فقط در AssetEntity.assetDataJson
    // به‌صورت JSON خام ذخیره می‌شود، نه ستون تفکیک‌شده‌ی Room).
    val descriptionFaPreview: String? = null,
    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۱ از ۵ (ADR-131): هم‌شکل مستقیم
    // با domain/asset/AssetModels.kt.
    val imagePromptQuick: String? = null,
    val imagePromptAi: String? = null,
    val imagePromptFaPreview: String? = null,
    val imagePromptGeneratedAt: Long? = null,
    val updatedAt: Long? = null
)

@Serializable
data class EnvironmentDto(val type: String, val size: String, val lightingCondition: String)

/** MIGRATED (Option A): دیگر assetType ندارد — فقط برای LOCATION استفاده می‌شود، تمایز با ObjectAssetDto در نوع Kotlin است. */
@Serializable
data class LocationAssetDto(
    val assetId: String,
    val name: String,
    val description: String,
    val environment: EnvironmentDto,
    val locationType: String = "CUSTOM",
    val timeCompatibility: List<String> = emptyList(),
    val weatherCompatibility: List<String> = emptyList(),
    val keyElements: List<String> = emptyList(),
    val basePrompt: String? = null,
    val continuityLockLevel: String = "STYLE",
    // فیچر مستقل جدید «آپلود عکس مرجع واقعی Asset» — زیرقدم ۱ از ۳ (ADR-137):
    // هم‌شکل مستقیم با CharacterAssetDto.referenceImages موجود.
    val referenceImages: List<ReferenceImageDto> = emptyList(),
    // سیستم Preview دوزبانه‌ی پرامپت — قدم ۲ از ۳ زیرقدم (ADR-122).
    val descriptionFaPreview: String? = null,
    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۱ از ۵ (ADR-131).
    val imagePromptQuick: String? = null,
    val imagePromptAi: String? = null,
    val imagePromptFaPreview: String? = null,
    val imagePromptGeneratedAt: Long? = null,
    val updatedAt: Long? = null
)

/** جدید (Option A): معادل مستقل ObjectAssetDto برای AssetType.OBJECT. */
@Serializable
data class ObjectAssetDto(
    val assetId: String,
    val name: String,
    val description: String,
    val subtype: String = "GENERAL_PROP",
    val size: String,
    val materialAndColor: String,
    val specialTrait: String? = null,
    val basePrompt: String? = null,
    val continuityLockLevel: String = "FORM",
    // فیچر مستقل جدید «آپلود عکس مرجع واقعی Asset» — زیرقدم ۱ از ۳ (ADR-137):
    // هم‌شکل مستقیم با CharacterAssetDto.referenceImages موجود.
    val referenceImages: List<ReferenceImageDto> = emptyList(),
    // سیستم Preview دوزبانه‌ی پرامپت — قدم ۲ از ۳ زیرقدم (ADR-122).
    val descriptionFaPreview: String? = null,
    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۱ از ۵ (ADR-131).
    val imagePromptQuick: String? = null,
    val imagePromptAi: String? = null,
    val imagePromptFaPreview: String? = null,
    val imagePromptGeneratedAt: Long? = null,
    val updatedAt: Long? = null
)
