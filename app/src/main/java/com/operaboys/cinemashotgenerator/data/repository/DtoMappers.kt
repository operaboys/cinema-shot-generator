package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.domain.camera.BasicMovementType
import com.operaboys.cinemashotgenerator.domain.camera.CameraAngle
import com.operaboys.cinemashotgenerator.domain.camera.CameraDistance
import com.operaboys.cinemashotgenerator.domain.camera.CameraMovement
import com.operaboys.cinemashotgenerator.domain.camera.CameraSettings
import com.operaboys.cinemashotgenerator.domain.camera.DepthOfField
import com.operaboys.cinemashotgenerator.domain.camera.Framing
import com.operaboys.cinemashotgenerator.domain.camera.FocusMode
import com.operaboys.cinemashotgenerator.domain.camera.LensType
import com.operaboys.cinemashotgenerator.domain.camera.Stabilization
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ColorTemperature
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ContrastRatio
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentalMotion
import com.operaboys.cinemashotgenerator.domain.sceneconditions.FillLight
import com.operaboys.cinemashotgenerator.domain.sceneconditions.GroundState
import com.operaboys.cinemashotgenerator.domain.sceneconditions.KeyLightPosition
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightSourceCount
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingMotivation
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ShadowQuality
import com.operaboys.cinemashotgenerator.domain.sceneconditions.TemperatureFeel
import com.operaboys.cinemashotgenerator.domain.sceneconditions.Visibility
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherIntensity
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WindStrength
import com.operaboys.cinemashotgenerator.domain.shot.ActionSound
import com.operaboys.cinemashotgenerator.domain.shot.AmbientSound
import com.operaboys.cinemashotgenerator.domain.shot.Beat
import com.operaboys.cinemashotgenerator.domain.shot.BeatEventType
import com.operaboys.cinemashotgenerator.domain.shot.CharacterSound
import com.operaboys.cinemashotgenerator.domain.shot.ImageReference
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.shot.SourcedSettings

// واحد ۱۵ — قدم ۲: نگاشت دوطرفه‌ی DTO ↔ نوع واقعی دامنه. این تنها فایلی است که هم از
// data.repository (DTO ها) و هم از domain.* (انواع واقعی) import می‌کند — دقیقاً همان
// نقطه‌ای که Repository ها اجازه دارند (data.repository → domain.*)، نه برعکس.

fun CameraMovementDto.toDomain(): CameraMovement = when (this) {
    is CameraMovementDto.Basic -> CameraMovement.Basic(BasicMovementType.valueOf(movementType), speed)
    is CameraMovementDto.Orbit -> CameraMovement.Orbit(degrees, speed, maintainEyeLevel)
    is CameraMovementDto.DronePath -> CameraMovement.DronePath(altitudeChange, pathType, speed)
    is CameraMovementDto.DollyZoom -> CameraMovement.DollyZoom(focalStart, focalEnd, direction)
    is CameraMovementDto.HandheldShake -> CameraMovement.HandheldShake(intensity, frequency)
    is CameraMovementDto.Compound -> CameraMovement.Compound(primary, secondary, sync)
}

fun CameraMovement.toDto(): CameraMovementDto = when (this) {
    is CameraMovement.Basic -> CameraMovementDto.Basic(movementType = type.name, speed = speed)
    is CameraMovement.Orbit -> CameraMovementDto.Orbit(degrees, speed, maintainEyeLevel)
    is CameraMovement.DronePath -> CameraMovementDto.DronePath(altitudeChange, pathType, speed)
    is CameraMovement.DollyZoom -> CameraMovementDto.DollyZoom(focalStart, focalEnd, direction)
    is CameraMovement.HandheldShake -> CameraMovementDto.HandheldShake(intensity, frequency)
    is CameraMovement.Compound -> CameraMovementDto.Compound(primary, secondary, sync)
}

fun CameraSettingsDto.toDomain(): CameraSettings = CameraSettings(
    angle = CameraAngle.valueOf(angle),
    distance = CameraDistance.valueOf(distance),
    movement = movement.toDomain(),
    lensType = LensType.valueOf(lensType),
    depthOfField = DepthOfField.valueOf(depthOfField),
    focusMode = FocusMode.valueOf(focusMode),
    stabilization = Stabilization.valueOf(stabilization),
    framing = Framing.valueOf(framing)
)

fun CameraSettings.toDto(): CameraSettingsDto = CameraSettingsDto(
    angle = angle.name,
    distance = distance.name,
    movement = movement.toDto(),
    lensType = lensType.name,
    depthOfField = depthOfField.name,
    focusMode = focusMode.name,
    stabilization = stabilization.name,
    framing = framing.name
)

fun LightingSettingsDto.toDomain(): LightingSettings = LightingSettings(
    style = LightingStyle.valueOf(style),
    keyLightPosition = KeyLightPosition.valueOf(keyLightPosition),
    contrastRatio = ContrastRatio.valueOf(contrastRatio),
    fillLight = fillLight?.let { FillLight.valueOf(it) },
    colorTemperature = colorTemperature?.let { ColorTemperature.valueOf(it) },
    shadowQuality = shadowQuality?.let { ShadowQuality.valueOf(it) },
    lightSourceCount = lightSourceCount?.let { LightSourceCount.valueOf(it) },
    lightingMotivation = lightingMotivation?.let { LightingMotivation.valueOf(it) }
)

fun LightingSettings.toDto(): LightingSettingsDto = LightingSettingsDto(
    style = style.name,
    keyLightPosition = keyLightPosition.name,
    contrastRatio = contrastRatio.name,
    fillLight = fillLight?.name,
    colorTemperature = colorTemperature?.name,
    shadowQuality = shadowQuality?.name,
    lightSourceCount = lightSourceCount?.name,
    lightingMotivation = lightingMotivation?.name
)

fun EnvironmentSettingsDto.toDomain(): EnvironmentSettings = EnvironmentSettings(
    weatherType = WeatherType.valueOf(weatherType),
    weatherIntensity = weatherIntensity?.let { WeatherIntensity.valueOf(it) },
    windStrength = windStrength?.let { WindStrength.valueOf(it) },
    groundState = groundState?.let { GroundState.valueOf(it) },
    visibility = visibility?.let { Visibility.valueOf(it) },
    temperatureFeel = temperatureFeel?.let { TemperatureFeel.valueOf(it) },
    environmentalMotion = environmentalMotion.map { EnvironmentalMotion.valueOf(it) }
)

fun EnvironmentSettings.toDto(): EnvironmentSettingsDto = EnvironmentSettingsDto(
    weatherType = weatherType.name,
    weatherIntensity = weatherIntensity?.name,
    windStrength = windStrength?.name,
    groundState = groundState?.name,
    visibility = visibility?.name,
    temperatureFeel = temperatureFeel?.name,
    environmentalMotion = environmentalMotion.map { it.name }
)

fun ShotDto.toDomain(): Shot = Shot(
    shotId = shotId,
    sceneId = sceneId,
    shotNumber = shotNumber,
    shotTitle = shotTitle,
    shotDescription = shotDescription,
    shotGoal = ShotGoal.valueOf(shotGoal),
    shotType = ShotType.valueOf(shotType),
    durationSeconds = durationSeconds,
    motionLevel = MotionLevel.valueOf(motionLevel),
    beats = beats.map { Beat(it.timestampSeconds, BeatEventType.valueOf(it.eventType), it.description, it.subjectId) },
    imageReferences = imageReferences.map { ImageReference(it.type, it.localFilePath, it.description) },
    camera = SourcedSettings(camera.source, camera.overrideValue?.toDomain()),
    lighting = SourcedSettings(lighting.source, lighting.overrideValue?.toDomain()),
    environment = SourcedSettings(environment.source, environment.overrideValue?.toDomain()),
    soundProfile = SoundProfile(
        enabled = soundProfile.enabled,
        ambientAutoGenerate = soundProfile.ambientAutoGenerate,
        ambientSounds = soundProfile.ambientSounds.map { AmbientSound(it.type, it.intensity, it.description) },
        actionSounds = soundProfile.actionSounds.map { ActionSound(it.timestampSeconds, it.type, it.description) },
        characterSounds = soundProfile.characterSounds.map { CharacterSound(it.characterId, it.type, it.description) }
    ),
    negativePromptOverride = negativePromptOverride,
    characterIds = characterIds,
    objectIds = objectIds,
    locationIds = locationIds,
    overrideScene = overrideScene
)

fun Shot.toDto(): ShotDto = ShotDto(
    shotId = shotId,
    sceneId = sceneId,
    shotNumber = shotNumber,
    shotTitle = shotTitle,
    shotDescription = shotDescription,
    shotGoal = shotGoal.name,
    shotType = shotType.name,
    durationSeconds = durationSeconds,
    motionLevel = motionLevel.name,
    beats = beats.map { BeatDto(it.timestampSeconds, it.eventType.name, it.description, it.subjectId) },
    imageReferences = imageReferences.map { ImageReferenceDto(it.type, it.localFilePath, it.description) },
    camera = SourcedCameraSettingsDto(camera.source, camera.overrideValue?.toDto()),
    lighting = SourcedLightingSettingsDto(lighting.source, lighting.overrideValue?.toDto()),
    environment = SourcedEnvironmentSettingsDto(environment.source, environment.overrideValue?.toDto()),
    soundProfile = SoundProfileDto(
        enabled = soundProfile.enabled,
        ambientAutoGenerate = soundProfile.ambientAutoGenerate,
        ambientSounds = soundProfile.ambientSounds.map { AmbientSoundDto(it.type, it.intensity, it.description) },
        actionSounds = soundProfile.actionSounds.map { ActionSoundDto(it.timestampSeconds, it.type, it.description) },
        characterSounds = soundProfile.characterSounds.map { CharacterSoundDto(it.characterId, it.type, it.description) }
    ),
    negativePromptOverride = negativePromptOverride,
    characterIds = characterIds,
    objectIds = objectIds,
    locationIds = locationIds,
    overrideScene = overrideScene
)
