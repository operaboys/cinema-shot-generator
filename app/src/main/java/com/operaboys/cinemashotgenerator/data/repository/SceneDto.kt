package com.operaboys.cinemashotgenerator.data.repository

import kotlinx.serialization.Serializable

// واحد ۱۵ — قدم ۳ (زیرقدم ۲): DTO برای فیلد SceneEntity.sceneDataJson.
// منبع حقیقت شکل داده: domain/scene/SceneModels.kt (خوانده‌شده با grep کامل).
// دلیل DTO محلی (نه @Serializable مستقیم روی domain/): همان دلیل ADR-018/019.
//
// NOTE: Scene دامنه هیچ فیلد projectId ندارد (تأیید شده با grep) — projectId فقط
// در SceneEntity (لایه‌ی ذخیره‌سازی) وجود دارد، نه در این DTO.

@Serializable
data class SceneLocationDto(val type: String, val description: String)

@Serializable
data class SceneConstraintsDto(
    val cameraRestrictions: List<String> = emptyList(),
    val lightingRestrictions: List<String> = emptyList(),
    val environmentRestrictions: List<String> = emptyList()
)

@Serializable
data class GlobalVisualStyleRefDto(val source: String = "project_dna", val override: String? = null)

// MIGRATED (docs/adr/038-unit04-scene-location-asset-link.md، رفع یافته‌ی F2 ممیزی
// pre-Unit 16): locationAssetId اضافه شد — nullable با پیش‌فرض null، دقیقاً طبق الگوی
// محافظه‌کارانه‌ی ADR-036 (سطح DTO هم nullable، چون دامنه هم nullable است؛ اینجا نیازی
// به یک پیش‌فرض غیر-null محافظه‌کارانه‌تر نیست چون null در دامنه هم یک مقدار کاملاً
// معتبر است، نه یک حالت جاافتاده).
// یافته‌ی #۱۱ appendix ADR-081 (ADR-085): linkedAssetIds اضافه شد — همان الگوی
// محافظه‌کارانه‌ی locationAssetId بالا (پیش‌فرض، بدون Migration رسمی).
@Serializable
data class SceneDto(
    val sceneId: String,
    val sceneTitle: String? = null,
    val sceneNumber: Int,
    val narrativeRole: String,
    val location: SceneLocationDto,
    val locationAssetId: String? = null,
    val linkedAssetIds: List<String> = emptyList(),
    val timeOfDay: String,
    val atmospherePrimary: String,
    val atmosphereSecondary: String? = null,
    val globalVisualStyle: GlobalVisualStyleRefDto = GlobalVisualStyleRefDto(),
    val constraints: SceneConstraintsDto = SceneConstraintsDto(),
    val shotCount: Int = 0,
    // واحد ۱۶ فاز ۴ قدم ۱: هم‌الگو با ProjectEntity.state — رشته‌ی خام EntityState،
    // پیش‌فرض «DRAFT».
    val state: String = "DRAFT"
)
