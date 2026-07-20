package com.operaboys.cinemashotgenerator.data.repository

import kotlinx.serialization.Serializable

// واحد ۱۵ — قدم ۳ (زیرقدم ۱): DTO های JSON برای فیلد ProjectDnaEntity.dnaDataJson.
// منبع حقیقت شکل داده: domain/dna/ProjectDna.kt (خوانده‌شده با grep کامل، نه از روی
// کد مفهومی خلاصه‌شده‌ی بلوپرینت). دلیل DTO محلی (نه @Serializable مستقیم روی
// domain/): همان دلیل ADR-018 — domain/ نباید تغییر کند. Enum ها به‌صورت String
// (نام Enum) ذخیره می‌شوند، طبق همان تصمیم قبلی.

@Serializable
data class CoreIdentityDto(
    val dominantVisualStyle: String,
    val realismLevel: String,
    val styleConsistency: String,
    val locked: Boolean = false
)

@Serializable
data class MasterPaletteDto(
    val colorTemperature: String,
    val globalSaturation: String,
    val globalContrast: String,
    val colorGradingPreset: String
)

@Serializable
data class GlobalMoodBaseDto(
    val primaryEmotion: String,
    val intensity: String,
    val consistency: String
)

@Serializable
data class ColorPhilosophyDto(
    val paletteType: String,
    val dominantColors: List<String>,
    val contrastPreference: String
)

@Serializable
data class CameraPreferencesDto(
    val preferredMovements: List<String>,
    val avoidMovements: List<String>
)

@Serializable
data class LightingPreferencesDto(
    val preferredStyles: List<String>,
    val avoidStyles: List<String>
)

@Serializable
data class StylePreferencesDto(
    val cinematicLanguage: String,
    val colorPhilosophy: ColorPhilosophyDto,
    val cameraPreferences: CameraPreferencesDto,
    val lightingPreferences: LightingPreferencesDto
)

@Serializable
data class OutputConstraintsDto(
    val forbiddenElements: Map<String, List<String>>,
    val mandatoryElements: List<String>,
    val maxShotDurationSeconds: Int,
    val aspectRatio: String
)

@Serializable
data class OverrideRulesDto(
    val allowSceneOverride: Boolean,
    val allowShotOverride: Boolean,
    val requiresHumanApproval: Boolean
)

@Serializable
data class ProjectDnaDto(
    val dnaId: String,
    val projectId: String,
    val coreIdentity: CoreIdentityDto,
    val masterPalette: MasterPaletteDto,
    val globalMoodBase: GlobalMoodBaseDto,
    val stylePreferences: StylePreferencesDto,
    val outputConstraints: OutputConstraintsDto,
    val overrideRules: OverrideRulesDto
)
