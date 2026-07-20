package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.domain.asset.AssetType
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.ContinuityRules
import com.operaboys.cinemashotgenerator.domain.asset.Environment
import com.operaboys.cinemashotgenerator.domain.asset.Expression
import com.operaboys.cinemashotgenerator.domain.asset.FacialFeatures
import com.operaboys.cinemashotgenerator.domain.asset.Hair
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.Outfit
import com.operaboys.cinemashotgenerator.domain.asset.OutfitCondition
import com.operaboys.cinemashotgenerator.domain.asset.PhysicalAppearance
import com.operaboys.cinemashotgenerator.domain.asset.Prop
import com.operaboys.cinemashotgenerator.domain.asset.ReferenceImage
import com.operaboys.cinemashotgenerator.domain.dna.CameraPreferences
import com.operaboys.cinemashotgenerator.domain.dna.ColorPhilosophy
import com.operaboys.cinemashotgenerator.domain.dna.ColorTemperature
import com.operaboys.cinemashotgenerator.domain.dna.CoreIdentity
import com.operaboys.cinemashotgenerator.domain.dna.GlobalMoodBase
import com.operaboys.cinemashotgenerator.domain.dna.IntensityLevel
import com.operaboys.cinemashotgenerator.domain.dna.LightingPreferences
import com.operaboys.cinemashotgenerator.domain.dna.MasterPalette
import com.operaboys.cinemashotgenerator.domain.dna.OutputConstraints
import com.operaboys.cinemashotgenerator.domain.dna.OverrideRules
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.dna.RealismLevel
import com.operaboys.cinemashotgenerator.domain.dna.SaturationLevel
import com.operaboys.cinemashotgenerator.domain.dna.StyleConsistency
import com.operaboys.cinemashotgenerator.domain.dna.StylePreferences
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle

// واحد ۱۵ — قدم ۳ (زیرقدم ۱): نگاشت دوطرفه‌ی DTO ↔ نوع واقعی دامنه برای ProjectDna
// و CharacterAsset/LocationAsset — هم‌الگو با DtoMappers.kt (ADR-018).

fun ProjectDnaDto.toDomain(): ProjectDna = ProjectDna(
    dnaId = dnaId,
    projectId = projectId,
    coreIdentity = CoreIdentity(
        dominantVisualStyle = VisualStyle.valueOf(coreIdentity.dominantVisualStyle),
        realismLevel = RealismLevel.valueOf(coreIdentity.realismLevel),
        styleConsistency = StyleConsistency.valueOf(coreIdentity.styleConsistency),
        locked = coreIdentity.locked
    ),
    masterPalette = MasterPalette(
        colorTemperature = ColorTemperature.valueOf(masterPalette.colorTemperature),
        globalSaturation = SaturationLevel.valueOf(masterPalette.globalSaturation),
        globalContrast = SaturationLevel.valueOf(masterPalette.globalContrast),
        colorGradingPreset = masterPalette.colorGradingPreset
    ),
    globalMoodBase = GlobalMoodBase(
        primaryEmotion = globalMoodBase.primaryEmotion,
        intensity = IntensityLevel.valueOf(globalMoodBase.intensity),
        consistency = StyleConsistency.valueOf(globalMoodBase.consistency)
    ),
    stylePreferences = StylePreferences(
        cinematicLanguage = stylePreferences.cinematicLanguage,
        colorPhilosophy = ColorPhilosophy(
            paletteType = stylePreferences.colorPhilosophy.paletteType,
            dominantColors = stylePreferences.colorPhilosophy.dominantColors,
            contrastPreference = stylePreferences.colorPhilosophy.contrastPreference
        ),
        cameraPreferences = CameraPreferences(
            preferredMovements = stylePreferences.cameraPreferences.preferredMovements,
            avoidMovements = stylePreferences.cameraPreferences.avoidMovements
        ),
        lightingPreferences = LightingPreferences(
            preferredStyles = stylePreferences.lightingPreferences.preferredStyles,
            avoidStyles = stylePreferences.lightingPreferences.avoidStyles
        )
    ),
    outputConstraints = OutputConstraints(
        forbiddenElements = outputConstraints.forbiddenElements,
        mandatoryElements = outputConstraints.mandatoryElements,
        maxShotDurationSeconds = outputConstraints.maxShotDurationSeconds,
        aspectRatio = outputConstraints.aspectRatio
    ),
    overrideRules = OverrideRules(
        allowSceneOverride = overrideRules.allowSceneOverride,
        allowShotOverride = overrideRules.allowShotOverride,
        requiresHumanApproval = overrideRules.requiresHumanApproval
    )
)

fun ProjectDna.toDto(): ProjectDnaDto = ProjectDnaDto(
    dnaId = dnaId,
    projectId = projectId,
    coreIdentity = CoreIdentityDto(
        dominantVisualStyle = coreIdentity.dominantVisualStyle.name,
        realismLevel = coreIdentity.realismLevel.name,
        styleConsistency = coreIdentity.styleConsistency.name,
        locked = coreIdentity.locked
    ),
    masterPalette = MasterPaletteDto(
        colorTemperature = masterPalette.colorTemperature.name,
        globalSaturation = masterPalette.globalSaturation.name,
        globalContrast = masterPalette.globalContrast.name,
        colorGradingPreset = masterPalette.colorGradingPreset
    ),
    globalMoodBase = GlobalMoodBaseDto(
        primaryEmotion = globalMoodBase.primaryEmotion,
        intensity = globalMoodBase.intensity.name,
        consistency = globalMoodBase.consistency.name
    ),
    stylePreferences = StylePreferencesDto(
        cinematicLanguage = stylePreferences.cinematicLanguage,
        colorPhilosophy = ColorPhilosophyDto(
            paletteType = stylePreferences.colorPhilosophy.paletteType,
            dominantColors = stylePreferences.colorPhilosophy.dominantColors,
            contrastPreference = stylePreferences.colorPhilosophy.contrastPreference
        ),
        cameraPreferences = CameraPreferencesDto(
            preferredMovements = stylePreferences.cameraPreferences.preferredMovements,
            avoidMovements = stylePreferences.cameraPreferences.avoidMovements
        ),
        lightingPreferences = LightingPreferencesDto(
            preferredStyles = stylePreferences.lightingPreferences.preferredStyles,
            avoidStyles = stylePreferences.lightingPreferences.avoidStyles
        )
    ),
    outputConstraints = OutputConstraintsDto(
        forbiddenElements = outputConstraints.forbiddenElements,
        mandatoryElements = outputConstraints.mandatoryElements,
        maxShotDurationSeconds = outputConstraints.maxShotDurationSeconds,
        aspectRatio = outputConstraints.aspectRatio
    ),
    overrideRules = OverrideRulesDto(
        allowSceneOverride = overrideRules.allowSceneOverride,
        allowShotOverride = overrideRules.allowShotOverride,
        requiresHumanApproval = overrideRules.requiresHumanApproval
    )
)

fun CharacterAssetDto.toDomain(): CharacterAsset = CharacterAsset(
    assetId = assetId,
    assetType = AssetType.valueOf(assetType),
    name = name,
    physicalAppearance = PhysicalAppearance(
        ageRange = physicalAppearance.ageRange,
        gender = physicalAppearance.gender,
        height = physicalAppearance.height,
        build = physicalAppearance.build,
        hair = Hair(physicalAppearance.hair.color, physicalAppearance.hair.style, physicalAppearance.hair.length),
        facialFeatures = FacialFeatures(physicalAppearance.facialFeatures.eyes, physicalAppearance.facialFeatures.distinctiveMarks)
    ),
    outfits = outfits.map {
        Outfit(it.id, it.name, it.description, it.isDefault, it.condition?.toDomain())
    },
    expressions = expressions.map {
        Expression(it.id, it.name, it.description, it.emotion, it.isDefault, it.condition?.toDomain())
    },
    props = props.map { Prop(it.id, it.name, it.description, it.category) },
    continuityRules = ContinuityRules(
        identityLock = continuityRules.identityLock,
        appearanceLock = continuityRules.appearanceLock,
        ageLock = continuityRules.ageLock,
        antiDrift = continuityRules.antiDrift,
        allowedOverrides = continuityRules.allowedOverrides
    ),
    referenceImages = referenceImages.map { ReferenceImage(it.localFilePath, it.description) }
)

fun CharacterAsset.toDto(): CharacterAssetDto = CharacterAssetDto(
    assetId = assetId,
    assetType = assetType.name,
    name = name,
    physicalAppearance = PhysicalAppearanceDto(
        ageRange = physicalAppearance.ageRange,
        gender = physicalAppearance.gender,
        height = physicalAppearance.height,
        build = physicalAppearance.build,
        hair = HairDto(physicalAppearance.hair.color, physicalAppearance.hair.style, physicalAppearance.hair.length),
        facialFeatures = FacialFeaturesDto(physicalAppearance.facialFeatures.eyes, physicalAppearance.facialFeatures.distinctiveMarks)
    ),
    outfits = outfits.map { OutfitDto(it.id, it.name, it.description, it.isDefault, it.condition?.toDto()) },
    expressions = expressions.map { ExpressionDto(it.id, it.name, it.description, it.emotion, it.isDefault, it.condition?.toDto()) },
    props = props.map { PropDto(it.id, it.name, it.description, it.category) },
    continuityRules = ContinuityRulesDto(
        identityLock = continuityRules.identityLock,
        appearanceLock = continuityRules.appearanceLock,
        ageLock = continuityRules.ageLock,
        antiDrift = continuityRules.antiDrift,
        allowedOverrides = continuityRules.allowedOverrides
    ),
    referenceImages = referenceImages.map { ReferenceImageDto(it.localFilePath, it.description) }
)

private fun OutfitConditionDto.toDomain(): OutfitCondition = OutfitCondition(weather, timeOfDay, locationType)
private fun OutfitCondition.toDto(): OutfitConditionDto = OutfitConditionDto(weather, timeOfDay, locationType)

fun LocationAssetDto.toDomain(): LocationAsset = LocationAsset(
    assetId = assetId,
    assetType = AssetType.valueOf(assetType),
    name = name,
    description = description,
    environment = Environment(environment.type, environment.size, environment.lightingCondition),
    timeCompatibility = timeCompatibility,
    weatherCompatibility = weatherCompatibility,
    keyElements = keyElements
)

fun LocationAsset.toDto(): LocationAssetDto = LocationAssetDto(
    assetId = assetId,
    assetType = assetType.name,
    name = name,
    description = description,
    environment = EnvironmentDto(environment.type, environment.size, environment.lightingCondition),
    timeCompatibility = timeCompatibility,
    weatherCompatibility = weatherCompatibility,
    keyElements = keyElements
)
