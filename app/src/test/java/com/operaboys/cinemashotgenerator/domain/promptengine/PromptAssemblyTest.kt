package com.operaboys.cinemashotgenerator.domain.promptengine

import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.Environment as AssetEnvironment
import com.operaboys.cinemashotgenerator.domain.asset.FacialFeatures
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.Hair
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.asset.Outfit
import com.operaboys.cinemashotgenerator.domain.asset.PhysicalAppearance
import com.operaboys.cinemashotgenerator.domain.audio.AudioContext
import com.operaboys.cinemashotgenerator.domain.audio.AmbientSound as DomainAmbientSound
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
import com.operaboys.cinemashotgenerator.domain.dna.AspectRatio
import com.operaboys.cinemashotgenerator.domain.dna.ColorTemperature
import com.operaboys.cinemashotgenerator.domain.dna.ContrastLevel
import com.operaboys.cinemashotgenerator.domain.dna.CoreIdentity
import com.operaboys.cinemashotgenerator.domain.dna.GlobalMoodBase
import com.operaboys.cinemashotgenerator.domain.dna.MasterPalette
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.dna.OutputConstraints
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.dna.QualityDirectives
import com.operaboys.cinemashotgenerator.domain.dna.RealismLevel
import com.operaboys.cinemashotgenerator.domain.dna.SaturationLevel
import com.operaboys.cinemashotgenerator.domain.dna.StyleConsistency
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.SceneLocation
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ContrastRatio
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.KeyLightPosition
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import com.operaboys.cinemashotgenerator.domain.shot.ActionSound
import com.operaboys.cinemashotgenerator.domain.shot.CharacterSound
import com.operaboys.cinemashotgenerator.domain.shot.Beat
import com.operaboys.cinemashotgenerator.domain.shot.BeatEventType
import com.operaboys.cinemashotgenerator.domain.shot.ImageReference
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.visualidentity.StyleInfluence
import com.operaboys.cinemashotgenerator.domain.visualidentity.combineStyles
import com.operaboys.cinemashotgenerator.domain.visualidentity.toStyleReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptAssemblyTest {

    private fun sampleDna() = ProjectDna(
        dnaId = "dna_001",
        projectId = "proj_001",
        coreIdentity = CoreIdentity(
            dominantVisualStyle = VisualStyle.CINEMATIC_STYLE,
            realismLevel = RealismLevel.GROUNDED,
            styleConsistency = StyleConsistency.STRICT,
            locked = true
        ),
        masterPalette = MasterPalette(
            colorTemperature = ColorTemperature.WARM,
            globalSaturation = SaturationLevel.MEDIUM,
            globalContrast = ContrastLevel.HIGH,
            colorGradingPreset = "natural"
        ),
        globalMoodBase = GlobalMoodBase(
            primaryEmotion = Mood.MYSTERIOUS,
            intensity = "medium",
            consistency = StyleConsistency.STRICT
        ),
        outputConstraints = OutputConstraints(
            forbiddenElements = emptyMap(),
            mandatoryElements = emptyList(),
            maxShotDurationSeconds = 10,
            aspectRatio = AspectRatio.ANAMORPHIC_2_39
        )
    )

    private fun sampleScene() = Scene(
        sceneId = "scene_001",
        sceneNumber = 1,
        narrativeRole = NarrativeRole.CLIMAX,
        location = SceneLocation(type = LocationType.OUTDOOR, description = "خیابان شلوغ شهری، شب"),
        timeOfDay = TimeOfDay.NIGHT,
        atmospherePrimary = Atmosphere.TENSE
    )

    private fun sampleShot() = Shot(
        shotId = "shot_001",
        sceneId = "scene_001",
        shotNumber = 1,
        shotDescription = "مرد جوان با کت مشکی وارد خیابان باران‌زده می‌شود",
        shotGoal = ShotGoal.ESTABLISHING,
        shotType = ShotType.WIDE,
        durationSeconds = 4f,
        motionLevel = MotionLevel.MODERATE,
        beats = listOf(Beat(1.5f, BeatEventType.SUBJECT_ACTION, "قدم اول")),
        imageReferences = listOf(ImageReference("character", "/storage/char_001_ref.jpg", "رفرنس چهره")),
        soundProfile = SoundProfile(enabled = true)
    )

    private fun sampleCharacter() = CharacterAsset(
        assetId = "char_001",
        characterTier = CharacterTier.MAIN,
        name = "Detective John",
        physicalAppearance = PhysicalAppearance(
            ageRange = "35-40",
            gender = Gender.MALE,
            height = "tall",
            build = "athletic",
            hair = Hair(color = "black", style = "short", length = "short"),
            facialFeatures = FacialFeatures(eyes = "brown")
        ),
        outfits = listOf(Outfit(id = "outfit_01", name = "Default Look", description = "black leather jacket, jeans", isDefault = true))
    )

    private fun sampleObjectAsset() = ObjectAsset(
        assetId = "obj_005",
        name = "Service Pistol",
        description = "توضیح نمونه",
        subtype = ObjectSubtype.PERSONAL_PROP,
        size = "small",
        materialAndColor = "worn black metal"
    )

    private fun sampleLocationAsset() = LocationAsset(
        assetId = "loc_002",
        name = "دفتر کارآگاه",
        description = "توضیح نمونه",
        environment = AssetEnvironment(type = "indoor", size = "small", lightingCondition = "dim")
    )

    private fun sampleCameraSettings() = CameraSettings(
        angle = CameraAngle.EYE_LEVEL,
        distance = CameraDistance.MEDIUM,
        movement = CameraMovement.Basic(type = BasicMovementType.STATIC),
        lensType = LensType.STANDARD,
        depthOfField = DepthOfField.MEDIUM,
        focusMode = FocusMode.SUBJECT_TRACKING,
        stabilization = Stabilization.GIMBAL,
        framing = Framing.RULE_OF_THIRDS
    )

    private fun sampleLightingSettings() = LightingSettings(
        style = LightingStyle.DRAMATIC_LIGHT,
        keyLightPosition = KeyLightPosition.SIDE,
        contrastRatio = ContrastRatio.HIGH
    )

    private fun sampleEnvironmentSettings(weatherType: WeatherType = WeatherType.RAIN) =
        EnvironmentSettings(weatherType = weatherType)

    private fun sampleAudioContext() = AudioContext(
        audioContextId = "audio_001",
        shotId = "shot_001",
        ambientSounds = listOf(DomainAmbientSound("rain", "heavy", "heavy rain on surfaces", "weather")),
        actionSounds = listOf(ActionSound(1.5f, "footstep", "footsteps on wet pavement")),
        characterSounds = listOf(CharacterSound("char_001", "breathing", "heavy breathing"))
    )

    private fun sampleInput(audioContext: AudioContext? = sampleAudioContext()) = PromptGenerationInput(
        dna = sampleDna(),
        scene = sampleScene(),
        shot = sampleShot(),
        characters = listOf(sampleCharacter()),
        objects = listOf(sampleObjectAsset()),
        locations = listOf(sampleLocationAsset()),
        camera = sampleCameraSettings(),
        lighting = sampleLightingSettings(),
        environment = sampleEnvironmentSettings(),
        audioContext = audioContext
    )

    @Test
    fun `assemblePromptBlueprint fills every structured part from a full input`() {
        val blueprint = assemblePromptBlueprint(
            input = sampleInput(),
            useSeed = true,
            weightedTags = mapOf("cinematic" to 1.4f),
            validationIssues = emptyList(),
            idProvider = { "prompt_test0001" }
        )

        assertEquals("prompt_test0001", blueprint.promptBlueprintId)
        assertEquals("shot_001", blueprint.shotId)

        val parts = blueprint.structuredParts
        assertTrue(parts.subjectDescription.contains("wearing black leather jacket, jeans"))
        assertTrue(parts.sceneContext.contains("TENSE"))
        assertEquals("مرد جوان با کت مشکی وارد خیابان باران‌زده می‌شود", parts.shotDescription)
        assertTrue(parts.cameraSpecs.contains("EYE_LEVEL"))
        assertTrue(parts.lightingSpecs.contains("DRAMATIC"))
        assertEquals("RAIN weather", parts.environmentSpecs)
        // اتصال کامل Style Matrix — قدم ۴-اتصال از ۸ زیرقدم (ADR-117): این تست
        // قبلاً انتظار نام خام enum ("CINEMATIC") را داشت. sampleDna() از قبل
        // dominantVisualStyle=VisualStyle.CINEMATIC_STYLE دارد (تأییدشده با
        // خواندن مستقیم sampleDna() بالا)؛ promptTokens واقعی آن (ADR-116) با
        // "cinematic style" شروع می‌شود — رفتار جدید و صحیح، نه چیزی که باید
        // حفظ می‌شد.
        assertTrue(parts.styleModifiers.contains("cinematic style"))
        assertNotNull(parts.timelineBeats)
        assertTrue(parts.timelineBeats!!.contains("قدم اول"))
        assertNotNull(parts.audioDescription)
        assertTrue(parts.audioDescription!!.contains("ambient"))

        assertEquals(1, blueprint.imageReferences.size)
        assertEquals(mapOf("cinematic" to 1.4f), blueprint.weightedEmphasis)
        assertEquals("shot_001".hashCode(), blueprint.seed)
        assertEquals(0, blueprint.conflictsResolved)
        assertTrue(blueprint.warnings.isEmpty())
    }

    @Test
    fun `assemblePromptBlueprint with null audio context produces null audio description`() {
        val blueprint = assemblePromptBlueprint(
            input = sampleInput(audioContext = null),
            useSeed = false,
            weightedTags = null,
            validationIssues = emptyList()
        )

        assertNull(blueprint.structuredParts.audioDescription)
        assertNull(blueprint.seed)
        assertTrue(blueprint.weightedEmphasis.isEmpty())
    }

    @Test
    fun `assemblePromptBlueprint with clear weather has null environment specs`() {
        val blueprint = assemblePromptBlueprint(
            input = sampleInput().copy(environment = sampleEnvironmentSettings(WeatherType.CLEAR)),
            useSeed = false,
            weightedTags = null,
            validationIssues = emptyList()
        )

        assertNull(blueprint.structuredParts.environmentSpecs)
    }

    // --- negativePrompt (docs/adr/030-unit14-reference-image-and-negative-prompt-migration.md، تکمیل ADR-028) ---

    @Test
    fun `assemblePromptBlueprint falls back to the DNA default negativePrompt when the shot has no override`() {
        val blueprint = assemblePromptBlueprint(
            input = sampleInput().copy(dna = sampleDna().copy(qualityDirectives = QualityDirectives(negativePrompt = "blurry, low quality"))),
            useSeed = false,
            weightedTags = null,
            validationIssues = emptyList()
        )
        assertEquals("blurry, low quality", blueprint.negativePrompt)
    }

    // اتصال کامل Style Matrix — قدم ۴-اتصال از ۸ زیرقدم (آخرین زیرقدم، ADR-117):
    // اثبات صریح که secondaryStyle/influence (ADR-113) واقعاً به styleModifiers
    // نهایی می‌رسند — نه فقط اینکه رشته‌ی primary دیگر خام نیست.

    @Test
    fun `assemblePromptBlueprint includes the secondary style with the correct influence modifier when both are set`() {
        val dnaWithSecondaryStyle = sampleDna().let {
            it.copy(coreIdentity = it.coreIdentity.copy(secondaryStyle = VisualStyle.STUDIO_GHIBLI, influence = StyleInfluence.STRONG))
        }
        val blueprint = assemblePromptBlueprint(
            input = sampleInput().copy(dna = dnaWithSecondaryStyle),
            useSeed = false,
            weightedTags = null,
            validationIssues = emptyList()
        )

        val expectedCombinedStyle = combineStyles(
            VisualStyle.CINEMATIC_STYLE.toStyleReference(),
            VisualStyle.STUDIO_GHIBLI.toStyleReference(),
            StyleInfluence.STRONG
        )
        assertTrue(blueprint.structuredParts.styleModifiers.startsWith(expectedCombinedStyle))
        assertTrue(blueprint.structuredParts.styleModifiers.contains("strongly influenced by"))
        assertTrue(blueprint.structuredParts.styleModifiers.contains(VisualStyle.STUDIO_GHIBLI.toStyleReference().promptTokens))
        // colorGradingPreset ("natural") همچنان در انتها باقی می‌ماند.
        assertTrue(blueprint.structuredParts.styleModifiers.endsWith("natural"))
    }

    @Test
    fun `assemblePromptBlueprint contains only the primary style's promptTokens when no secondary style is set`() {
        val blueprint = assemblePromptBlueprint(
            input = sampleInput(),
            useSeed = false,
            weightedTags = null,
            validationIssues = emptyList()
        )

        val primaryOnly = combineStyles(VisualStyle.CINEMATIC_STYLE.toStyleReference(), secondary = null, influence = null)
        assertEquals("$primaryOnly, natural", blueprint.structuredParts.styleModifiers)
    }

    @Test
    fun `assemblePromptBlueprint uses the shot's negativePromptOverride over the DNA default`() {
        val blueprint = assemblePromptBlueprint(
            input = sampleInput().copy(
                shot = sampleShot().copy(negativePromptOverride = "watermark"),
                dna = sampleDna().copy(qualityDirectives = QualityDirectives(negativePrompt = "blurry, low quality"))
            ),
            useSeed = false,
            weightedTags = null,
            validationIssues = emptyList()
        )
        assertEquals("watermark", blueprint.negativePrompt)
    }
}
