package com.operaboys.cinemashotgenerator.domain.scene

// واحد ۰۴ — Scene Engine (ساختار داده)
// منبع حقیقت: docs/blueprints/04-scene-engine.md

enum class NarrativeRole { INTRODUCTION, DEVELOPMENT, CLIMAX, RESOLUTION, TRANSITION }
enum class LocationType { INDOOR, OUTDOOR, MIXED, CUSTOM }
enum class TimeOfDay { DAWN, MORNING, NOON, AFTERNOON, SUNSET, NIGHT }
enum class Atmosphere { CALM, TENSE, DARK, BRIGHT, MYSTERIOUS, EMOTIONAL }

data class SceneLocation(val type: LocationType, val description: String)

data class SceneConstraints(
    val cameraRestrictions: List<String> = emptyList(),
    val lightingRestrictions: List<String> = emptyList(),
    val environmentRestrictions: List<String> = emptyList()
)

/**
 * نمایندگی ساده‌ی فیلد global_visual_style طبق ساختار JSON بلوپرینت (نه اتصال واقعی
 * به DNA Manager): source معمولاً "project_dna" است؛ override در صورت وجود، سبک
 * محلی صحنه را نشان می‌دهد. شکل این نوع (source + یک override تکی، نه یک Map
 * تنظیمات) با SourcedSettings واحد ۰۵ متفاوت است — چون ساختار JSON خودش متفاوت
 * است (`{"source": "project_dna", "override": null}` در برابر `{"source": ..., "settings": {...}}`).
 */
data class GlobalVisualStyleRef(
    val source: String = "project_dna",
    val override: String? = null
)

data class Scene(
    val sceneId: String,
    val sceneTitle: String? = null,
    val sceneNumber: Int,
    val narrativeRole: NarrativeRole,
    val location: SceneLocation,
    val timeOfDay: TimeOfDay,
    val atmospherePrimary: Atmosphere,
    val atmosphereSecondary: Atmosphere? = null,
    val globalVisualStyle: GlobalVisualStyleRef = GlobalVisualStyleRef(),
    val constraints: SceneConstraints = SceneConstraints(),
    val shotCount: Int = 0
)
