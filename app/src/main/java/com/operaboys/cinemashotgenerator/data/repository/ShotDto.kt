package com.operaboys.cinemashotgenerator.data.repository

import kotlinx.serialization.Serializable

// واحد ۱۵ — قدم ۲: DTO های JSON برای فیلد ShotEntity.shotDataJson.
// منبع حقیقت شکل داده: domain/shot/ShotModels.kt
//
// چرا DTO محلی به‌جای @Serializable مستقیم روی خودِ domain/shot/ShotModels.kt: طبق
// مرز صریح این قدم، domain/ نباید تغییر کند (حتی افزودن یک Annotation) — این‌ها فقط
// در data/repository/ زندگی می‌کنند و توسط DtoMappers.kt به/از انواع واقعی دامنه
// تبدیل می‌شوند. Enum های دامنه به‌صورت String (نام Enum) ذخیره می‌شوند — نیازی به
// DTO enum جداگانه نبود.

@Serializable
data class BeatDto(
    val timestampSeconds: Float,
    val eventType: String,
    val description: String,
    val subjectId: String? = null
)

@Serializable
data class ImageReferenceDto(
    val type: String,
    val localFilePath: String,
    val description: String
)

@Serializable
data class AmbientSoundDto(val type: String, val intensity: String, val description: String)

@Serializable
data class ActionSoundDto(val timestampSeconds: Float, val type: String, val description: String)

@Serializable
data class CharacterSoundDto(val characterId: String, val type: String, val description: String)

@Serializable
data class SoundProfileDto(
    val enabled: Boolean,
    val ambientAutoGenerate: Boolean = true,
    val ambientSounds: List<AmbientSoundDto> = emptyList(),
    val actionSounds: List<ActionSoundDto> = emptyList(),
    val characterSounds: List<CharacterSoundDto> = emptyList()
)

/**
 * سه نسخه‌ی مشخص (نه یک DTO عمومی SourcedSettingsDto<T>) — kotlinx.serialization برای
 * انواع Generic نیاز به تزریق دستی Serializer در هر فراخوانی دارد؛ چون فقط سه نمونه‌ی
 * مشخص (Camera/Lighting/Environment) لازم است، سه نوع غیر-Generic ساده‌تر و بدون
 * پیچیدگی اضافه است.
 */
@Serializable
data class SourcedCameraSettingsDto(val source: String = "scene", val overrideValue: CameraSettingsDto? = null)

@Serializable
data class SourcedLightingSettingsDto(val source: String = "scene", val overrideValue: LightingSettingsDto? = null)

@Serializable
data class SourcedEnvironmentSettingsDto(val source: String = "scene", val overrideValue: EnvironmentSettingsDto? = null)

@Serializable
data class ShotDto(
    val shotId: String,
    val sceneId: String,
    val shotNumber: Int,
    val shotTitle: String? = null,
    val shotDescription: String,
    val shotGoal: String,
    val shotType: String,
    val durationSeconds: Float,
    val motionLevel: String,
    val beats: List<BeatDto> = emptyList(),
    val imageReferences: List<ImageReferenceDto> = emptyList(),
    val camera: SourcedCameraSettingsDto = SourcedCameraSettingsDto(),
    val lighting: SourcedLightingSettingsDto = SourcedLightingSettingsDto(),
    val environment: SourcedEnvironmentSettingsDto = SourcedEnvironmentSettingsDto(),
    val soundProfile: SoundProfileDto,
    // واحد ۱۶ فاز ۴ قدم ۲: کمبود واقعی کشف‌شده — negativePromptOverride روی
    // domain/shot/ShotModels.kt از قبل موجود بود (ADR-028) اما هرگز به این DTO
    // اضافه نشده بود؛ یعنی هر Shot که از این مسیر Round-Trip می‌کرد، این فیلد را
    // بی‌صدا گم می‌کرد. رفع شد — پیش‌فرض null، Backward Compatible.
    val negativePromptOverride: String? = null,
    val characterIds: List<String> = emptyList(),
    val objectIds: List<String> = emptyList(),
    val locationIds: List<String> = emptyList(),
    val overrideScene: Boolean = false
)
