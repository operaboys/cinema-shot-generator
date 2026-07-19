package com.operaboys.cinemashotgenerator.domain.dna

// واحد ۰۲ — DNA Manager (ساختار داده)
// منبع حقیقت: docs/blueprints/02-dna-manager.md

enum class VisualStyle { REALISTIC, STYLIZED, CINEMATIC, HYBRID }

enum class RealismLevel { GROUNDED, SEMI_REALISTIC, FANTASTICAL }

enum class StyleConsistency { STRICT, MODERATE, FLEXIBLE }

enum class ColorTemperature { WARM, COOL, NEUTRAL }

enum class SaturationLevel { LOW, MEDIUM, HIGH, VERY_HIGH }

/** واژگان محدود شدت حس کلی پروژه — مستقل از SaturationLevel (مفهوم متفاوت). */
enum class IntensityLevel { LOW, MEDIUM, HIGH }

/** Core Identity — Soft Lock: همیشه قابل تغییر، حتی پس از locked=true. */
data class CoreIdentity(
    val dominantVisualStyle: VisualStyle,
    val realismLevel: RealismLevel,
    val styleConsistency: StyleConsistency,
    val locked: Boolean = false
)

data class MasterPalette(
    val colorTemperature: ColorTemperature,
    val globalSaturation: SaturationLevel,
    val globalContrast: SaturationLevel,
    val colorGradingPreset: String
)

/**
 * primaryEmotion به‌عمد String است، نه enum Mood واحد ۰۱ — این واحد نباید به
 * domain.story وابسته شود (خارج از Scope این قدم). جزئیات در ADR-002.
 */
data class GlobalMoodBase(
    val primaryEmotion: String,
    val intensity: IntensityLevel,
    val consistency: StyleConsistency
)

data class ColorPhilosophy(
    val paletteType: String,
    val dominantColors: List<String>,
    val contrastPreference: String
)

data class CameraPreferences(
    val preferredMovements: List<String>,
    val avoidMovements: List<String>
)

data class LightingPreferences(
    val preferredStyles: List<String>,
    val avoidStyles: List<String>
)

/** قابل Override در سطح Shot — واژگان باز (Camera/Lighting) متعلق به واحدهای بعدی است. */
data class StylePreferences(
    val cinematicLanguage: String,
    val colorPhilosophy: ColorPhilosophy,
    val cameraPreferences: CameraPreferences,
    val lightingPreferences: LightingPreferences
)

data class OutputConstraints(
    val forbiddenElements: Map<String, List<String>>, // مثلاً "camera" -> ["dutch_angle"]
    val mandatoryElements: List<String>,
    val maxShotDurationSeconds: Int,
    val aspectRatio: String
)

/** Rule 5: کنترل Override در سطح DNA — requiresHumanApproval پایه‌ی تابع کمکی مربوطه است. */
data class OverrideRules(
    val allowSceneOverride: Boolean,
    val allowShotOverride: Boolean,
    val requiresHumanApproval: Boolean
)

data class ProjectDna(
    val dnaId: String,
    val projectId: String,
    val coreIdentity: CoreIdentity,
    val masterPalette: MasterPalette,
    val globalMoodBase: GlobalMoodBase,
    val stylePreferences: StylePreferences,
    val outputConstraints: OutputConstraints,
    val overrideRules: OverrideRules
)
