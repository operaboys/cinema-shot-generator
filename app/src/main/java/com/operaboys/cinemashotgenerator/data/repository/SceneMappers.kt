package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.GlobalVisualStyleRef
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.SceneConstraints
import com.operaboys.cinemashotgenerator.domain.scene.SceneLocation
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState

// واحد ۱۵ — قدم ۳ (زیرقدم ۲): نگاشت دوطرفه‌ی SceneDto ↔ Scene واقعی دامنه.

fun SceneDto.toDomain(): Scene = Scene(
    sceneId = sceneId,
    sceneTitle = sceneTitle,
    sceneNumber = sceneNumber,
    narrativeRole = NarrativeRole.valueOf(narrativeRole),
    location = SceneLocation(LocationType.valueOf(location.type), location.description),
    locationAssetId = locationAssetId,
    linkedAssetIds = linkedAssetIds,
    timeOfDay = TimeOfDay.valueOf(timeOfDay),
    atmospherePrimary = Atmosphere.valueOf(atmospherePrimary),
    atmosphereSecondary = atmosphereSecondary?.let { Atmosphere.valueOf(it) },
    globalVisualStyle = GlobalVisualStyleRef(globalVisualStyle.source, globalVisualStyle.override),
    constraints = SceneConstraints(
        constraints.cameraRestrictions,
        constraints.lightingRestrictions,
        constraints.environmentRestrictions
    ),
    shotCount = shotCount,
    state = EntityState.valueOf(state)
)

fun Scene.toDto(): SceneDto = SceneDto(
    sceneId = sceneId,
    sceneTitle = sceneTitle,
    sceneNumber = sceneNumber,
    narrativeRole = narrativeRole.name,
    location = SceneLocationDto(location.type.name, location.description),
    locationAssetId = locationAssetId,
    linkedAssetIds = linkedAssetIds,
    timeOfDay = timeOfDay.name,
    atmospherePrimary = atmospherePrimary.name,
    atmosphereSecondary = atmosphereSecondary?.name,
    globalVisualStyle = GlobalVisualStyleRefDto(globalVisualStyle.source, globalVisualStyle.override),
    constraints = SceneConstraintsDto(
        constraints.cameraRestrictions,
        constraints.lightingRestrictions,
        constraints.environmentRestrictions
    ),
    shotCount = shotCount,
    state = state.name
)
