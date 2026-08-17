package com.operaboys.cinemashotgenerator.data.repository

import kotlinx.serialization.Serializable

// واحد ۱۵ — قدم ۳ (زیرقدم ۱): DTO های JSON برای فیلد ProjectDnaEntity.dnaDataJson.
// منبع حقیقت شکل داده: domain/dna/ProjectDna.kt (خوانده‌شده با grep کامل، نه از روی
// کد مفهومی خلاصه‌شده‌ی بلوپرینت). دلیل DTO محلی (نه @Serializable مستقیم روی
// domain/): همان دلیل ADR-018 — domain/ نباید تغییر کند. Enum ها به‌صورت String
// (نام Enum) ذخیره می‌شوند، طبق همان تصمیم قبلی.
//
// MIGRATED (docs/adr/027-unit02-dna-manager-v5-migration.md): این فایل با
// domain/dna/ProjectDna.kt (بلوپرینت ۰۲ نسخه ۵) هم‌گام‌سازی شد:
// - StylePreferencesDto/ColorPhilosophyDto/CameraPreferencesDto/LightingPreferencesDto/
//   OverrideRulesDto کاملاً حذف شدند (فیلدهای متناظرشان از ProjectDna حذف شدند).
// - MasterPaletteDto.colorPalette (جدید)، LightingPreferenceDto (جدید)،
//   QualityDirectivesDto (جدید) اضافه شدند.
// - globalContrast/aspectRatio/primaryEmotion همچنان String ذخیره می‌شوند (نام Enum)
//   — نوع Kotlin سمت domain عوض شد (ContrastLevel/AspectRatio/Mood) ولی شکل
//   ذخیره‌سازی JSON (رشته‌ی نام Enum) تغییری نکرد.

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
    val colorGradingPreset: String,
    val colorPalette: List<String> = emptyList()
)

@Serializable
data class GlobalMoodBaseDto(
    val primaryEmotion: String,
    val intensity: String,
    val consistency: String
)

@Serializable
data class OutputConstraintsDto(
    val forbiddenElements: Map<String, List<String>>,
    val mandatoryElements: List<String>,
    val maxShotDurationSeconds: Int,
    val aspectRatio: String
)

@Serializable
data class LightingPreferenceDto(
    val preferredStyle: String? = null
)

@Serializable
data class QualityDirectivesDto(
    val qualityTags: String = "",
    val negativePrompt: String = ""
)

// تکمیل Rule یتیم — قدم ۱ از ۴ (ADR-106): sceneOverrides دامنه (Map<String,
// CinematicMode>) به همان شکل (Map<String, String> — sceneId به نام Enum)
// در JSON ذخیره می‌شود، دقیقاً هم‌الگو با بقیه‌ی این فایل (هر Enum دامنه به‌صورت
// نام رشته‌ای ذخیره می‌شود، نه یک نوع DTO جدا) — kotlinx.serialization به‌طور
// بومی از Map<String, String> به‌عنوان یک شیء JSON پشتیبانی می‌کند، نیازی به
// شکل جایگزین (مثلاً List<Pair>) نبود.
@Serializable
data class CinematicLanguageSettingsDto(
    val globalMode: String = "BALANCED",
    val sceneOverrides: Map<String, String> = emptyMap()
)

@Serializable
data class ProjectDnaDto(
    val dnaId: String,
    val projectId: String,
    val coreIdentity: CoreIdentityDto,
    val masterPalette: MasterPaletteDto,
    val outputConstraints: OutputConstraintsDto,
    val globalMoodBase: GlobalMoodBaseDto,
    val lightingPreference: LightingPreferenceDto = LightingPreferenceDto(),
    val qualityDirectives: QualityDirectivesDto = QualityDirectivesDto(),
    val cinematicLanguage: CinematicLanguageSettingsDto = CinematicLanguageSettingsDto()
)
