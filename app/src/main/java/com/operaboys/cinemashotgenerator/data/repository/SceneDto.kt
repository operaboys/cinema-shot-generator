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

@Serializable
data class SceneDto(
    val sceneId: String,
    val sceneTitle: String? = null,
    val sceneNumber: Int,
    val narrativeRole: String,
    val location: SceneLocationDto,
    val timeOfDay: String,
    val atmospherePrimary: String,
    val atmosphereSecondary: String? = null,
    val globalVisualStyle: GlobalVisualStyleRefDto = GlobalVisualStyleRefDto(),
    val constraints: SceneConstraintsDto = SceneConstraintsDto(),
    val shotCount: Int = 0
)
