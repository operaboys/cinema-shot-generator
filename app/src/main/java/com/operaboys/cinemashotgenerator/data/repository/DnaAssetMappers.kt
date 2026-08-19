package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.domain.asset.AssetType
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterContinuityLevel
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.ContinuityRules
import com.operaboys.cinemashotgenerator.domain.asset.Environment
import com.operaboys.cinemashotgenerator.domain.asset.Expression
import com.operaboys.cinemashotgenerator.domain.asset.FacialFeatures
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.Hair
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.LocationContinuityLevel
import com.operaboys.cinemashotgenerator.domain.asset.LocationType
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.asset.Outfit
import com.operaboys.cinemashotgenerator.domain.asset.OutfitCondition
import com.operaboys.cinemashotgenerator.domain.asset.PhysicalAppearance
import com.operaboys.cinemashotgenerator.domain.asset.Prop
import com.operaboys.cinemashotgenerator.domain.asset.PropContinuityLevel
import com.operaboys.cinemashotgenerator.domain.asset.ReferenceImage
import com.operaboys.cinemashotgenerator.domain.asset.defaultLockLevelForTier
import com.operaboys.cinemashotgenerator.domain.dna.AspectRatio
import com.operaboys.cinemashotgenerator.domain.dna.ColorTemperature
import com.operaboys.cinemashotgenerator.domain.dna.ContrastLevel
import com.operaboys.cinemashotgenerator.domain.dna.CoreIdentity
import com.operaboys.cinemashotgenerator.domain.dna.GlobalMoodBase
import com.operaboys.cinemashotgenerator.domain.dna.LightingPreference
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.dna.MasterPalette
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.dna.OutputConstraints
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.dna.QualityDirectives
import com.operaboys.cinemashotgenerator.domain.dna.RealismLevel
import com.operaboys.cinemashotgenerator.domain.dna.SaturationLevel
import com.operaboys.cinemashotgenerator.domain.dna.StyleConsistency
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicLanguageSettings
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode
import com.operaboys.cinemashotgenerator.domain.visualidentity.StyleInfluence

// واحد ۱۵ — قدم ۳ (زیرقدم ۱): نگاشت دوطرفه‌ی DTO ↔ نوع واقعی دامنه برای ProjectDna
// و CharacterAsset/LocationAsset — هم‌الگو با DtoMappers.kt (ADR-018).

fun ProjectDnaDto.toDomain(): ProjectDna = ProjectDna(
    dnaId = dnaId,
    projectId = projectId,
    coreIdentity = CoreIdentity(
        dominantVisualStyle = VisualStyle.valueOf(coreIdentity.dominantVisualStyle),
        realismLevel = RealismLevel.valueOf(coreIdentity.realismLevel),
        styleConsistency = StyleConsistency.valueOf(coreIdentity.styleConsistency),
        locked = coreIdentity.locked,
        secondaryStyle = coreIdentity.secondaryStyle?.let { VisualStyle.valueOf(it) },
        influence = coreIdentity.influence?.let { StyleInfluence.valueOf(it) }
    ),
    masterPalette = MasterPalette(
        colorTemperature = ColorTemperature.valueOf(masterPalette.colorTemperature),
        globalSaturation = SaturationLevel.valueOf(masterPalette.globalSaturation),
        globalContrast = ContrastLevel.valueOf(masterPalette.globalContrast),
        colorGradingPreset = masterPalette.colorGradingPreset,
        colorPalette = masterPalette.colorPalette
    ),
    globalMoodBase = GlobalMoodBase(
        primaryEmotion = Mood.valueOf(globalMoodBase.primaryEmotion),
        intensity = globalMoodBase.intensity,
        consistency = StyleConsistency.valueOf(globalMoodBase.consistency)
    ),
    outputConstraints = OutputConstraints(
        forbiddenElements = outputConstraints.forbiddenElements,
        mandatoryElements = outputConstraints.mandatoryElements,
        maxShotDurationSeconds = outputConstraints.maxShotDurationSeconds,
        aspectRatio = AspectRatio.valueOf(outputConstraints.aspectRatio)
    ),
    lightingPreference = LightingPreference(
        preferredStyle = lightingPreference.preferredStyle?.let { LightingStyle.valueOf(it) }
    ),
    qualityDirectives = QualityDirectives(
        qualityTags = qualityDirectives.qualityTags,
        negativePrompt = qualityDirectives.negativePrompt
    ),
    cinematicLanguage = CinematicLanguageSettings(
        globalMode = CinematicMode.valueOf(cinematicLanguage.globalMode),
        sceneOverrides = cinematicLanguage.sceneOverrides.mapValues { CinematicMode.valueOf(it.value) }
    )
)

fun ProjectDna.toDto(): ProjectDnaDto = ProjectDnaDto(
    dnaId = dnaId,
    projectId = projectId,
    coreIdentity = CoreIdentityDto(
        dominantVisualStyle = coreIdentity.dominantVisualStyle.name,
        realismLevel = coreIdentity.realismLevel.name,
        styleConsistency = coreIdentity.styleConsistency.name,
        locked = coreIdentity.locked,
        secondaryStyle = coreIdentity.secondaryStyle?.name,
        influence = coreIdentity.influence?.name
    ),
    masterPalette = MasterPaletteDto(
        colorTemperature = masterPalette.colorTemperature.name,
        globalSaturation = masterPalette.globalSaturation.name,
        globalContrast = masterPalette.globalContrast.name,
        colorGradingPreset = masterPalette.colorGradingPreset,
        colorPalette = masterPalette.colorPalette
    ),
    globalMoodBase = GlobalMoodBaseDto(
        primaryEmotion = globalMoodBase.primaryEmotion.name,
        intensity = globalMoodBase.intensity,
        consistency = globalMoodBase.consistency.name
    ),
    outputConstraints = OutputConstraintsDto(
        forbiddenElements = outputConstraints.forbiddenElements,
        mandatoryElements = outputConstraints.mandatoryElements,
        maxShotDurationSeconds = outputConstraints.maxShotDurationSeconds,
        aspectRatio = outputConstraints.aspectRatio.name
    ),
    lightingPreference = LightingPreferenceDto(
        preferredStyle = lightingPreference.preferredStyle?.name
    ),
    qualityDirectives = QualityDirectivesDto(
        qualityTags = qualityDirectives.qualityTags,
        negativePrompt = qualityDirectives.negativePrompt
    ),
    cinematicLanguage = CinematicLanguageSettingsDto(
        globalMode = cinematicLanguage.globalMode.name,
        sceneOverrides = cinematicLanguage.sceneOverrides.mapValues { it.value.name }
    )
)

fun CharacterAssetDto.toDomain(): CharacterAsset {
    val tier = CharacterTier.valueOf(characterTier)
    return CharacterAsset(
        assetId = assetId,
        assetType = AssetType.valueOf(assetType),
        characterTier = tier,
        name = name,
        physicalAppearance = PhysicalAppearance(
            ageRange = physicalAppearance.ageRange,
            gender = Gender.valueOf(physicalAppearance.gender.uppercase()),
            height = physicalAppearance.height,
            build = physicalAppearance.build,
            hair = physicalAppearance.hair?.let { Hair(it.color, it.style, it.length) },
            physicalFeatures = physicalAppearance.physicalFeatures,
            facialFeatures = physicalAppearance.facialFeatures?.let { FacialFeatures(it.eyes, it.distinctiveMarks) }
        ),
        outfits = outfits.map {
            Outfit(it.id, it.name, it.description, it.isDefault, it.condition?.toDomain())
        },
        expressions = expressions.map {
            Expression(it.id, it.name, it.description, it.emotion, it.isDefault, it.condition?.toDomain())
        },
        props = props.map { Prop(it.id, it.name, it.description, it.category) },
        defaultMood = defaultMood,
        basePrompt = basePrompt,
        continuityRules = ContinuityRules(
            identityLock = continuityRules.identityLock,
            appearanceLock = continuityRules.appearanceLock,
            ageLock = continuityRules.ageLock,
            antiDrift = continuityRules.antiDrift,
            allowedOverrides = continuityRules.allowedOverrides
        ),
        continuityLockLevel = continuityLockLevel?.let { CharacterContinuityLevel.valueOf(it) } ?: defaultLockLevelForTier(tier),
        referenceImages = referenceImages.map { ReferenceImage(it.localFilePath, it.description) },
        descriptionFaPreview = descriptionFaPreview
    )
}

fun CharacterAsset.toDto(): CharacterAssetDto = CharacterAssetDto(
    assetId = assetId,
    assetType = assetType.name,
    characterTier = characterTier.name,
    name = name,
    physicalAppearance = PhysicalAppearanceDto(
        ageRange = physicalAppearance.ageRange,
        gender = physicalAppearance.gender.name,
        height = physicalAppearance.height,
        build = physicalAppearance.build,
        hair = physicalAppearance.hair?.let { HairDto(it.color, it.style, it.length) },
        physicalFeatures = physicalAppearance.physicalFeatures,
        facialFeatures = physicalAppearance.facialFeatures?.let { FacialFeaturesDto(it.eyes, it.distinctiveMarks) }
    ),
    outfits = outfits.map { OutfitDto(it.id, it.name, it.description, it.isDefault, it.condition?.toDto()) },
    expressions = expressions.map { ExpressionDto(it.id, it.name, it.description, it.emotion, it.isDefault, it.condition?.toDto()) },
    props = props.map { PropDto(it.id, it.name, it.description, it.category) },
    defaultMood = defaultMood,
    basePrompt = basePrompt,
    continuityRules = ContinuityRulesDto(
        identityLock = continuityRules.identityLock,
        appearanceLock = continuityRules.appearanceLock,
        ageLock = continuityRules.ageLock,
        antiDrift = continuityRules.antiDrift,
        allowedOverrides = continuityRules.allowedOverrides
    ),
    continuityLockLevel = continuityLockLevel.name,
    referenceImages = referenceImages.map { ReferenceImageDto(it.localFilePath, it.description) },
    descriptionFaPreview = descriptionFaPreview
)

private fun OutfitConditionDto.toDomain(): OutfitCondition = OutfitCondition(weather, timeOfDay, locationType)
private fun OutfitCondition.toDto(): OutfitConditionDto = OutfitConditionDto(weather, timeOfDay, locationType)

fun LocationAssetDto.toDomain(): LocationAsset = LocationAsset(
    assetId = assetId,
    name = name,
    description = description,
    environment = Environment(environment.type, environment.size, environment.lightingCondition),
    locationType = LocationType.valueOf(locationType),
    timeCompatibility = timeCompatibility,
    weatherCompatibility = weatherCompatibility,
    keyElements = keyElements,
    basePrompt = basePrompt,
    continuityLockLevel = LocationContinuityLevel.valueOf(continuityLockLevel),
    descriptionFaPreview = descriptionFaPreview
)

fun LocationAsset.toDto(): LocationAssetDto = LocationAssetDto(
    assetId = assetId,
    name = name,
    description = description,
    environment = EnvironmentDto(environment.type, environment.size, environment.lightingCondition),
    locationType = locationType.name,
    timeCompatibility = timeCompatibility,
    weatherCompatibility = weatherCompatibility,
    keyElements = keyElements,
    basePrompt = basePrompt,
    continuityLockLevel = continuityLockLevel.name,
    descriptionFaPreview = descriptionFaPreview
)

fun ObjectAssetDto.toDomain(): ObjectAsset = ObjectAsset(
    assetId = assetId,
    name = name,
    description = description,
    subtype = ObjectSubtype.valueOf(subtype),
    size = size,
    materialAndColor = materialAndColor,
    specialTrait = specialTrait,
    basePrompt = basePrompt,
    continuityLockLevel = PropContinuityLevel.valueOf(continuityLockLevel),
    descriptionFaPreview = descriptionFaPreview
)

fun ObjectAsset.toDto(): ObjectAssetDto = ObjectAssetDto(
    assetId = assetId,
    name = name,
    description = description,
    subtype = subtype.name,
    size = size,
    materialAndColor = materialAndColor,
    specialTrait = specialTrait,
    basePrompt = basePrompt,
    continuityLockLevel = continuityLockLevel.name,
    descriptionFaPreview = descriptionFaPreview
)
