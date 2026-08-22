package com.operaboys.cinemashotgenerator.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.data.entity.ShotEntity
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.Environment
import com.operaboys.cinemashotgenerator.domain.asset.FacialFeatures
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.Hair
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.asset.Outfit
import com.operaboys.cinemashotgenerator.domain.asset.PhysicalAppearance
import com.operaboys.cinemashotgenerator.domain.audio.AmbientSound
import com.operaboys.cinemashotgenerator.domain.audio.AudioContext
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
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.dna.MasterPalette
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.dna.OutputConstraints
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
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
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ContrastRatio
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.KeyLightPosition
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import com.operaboys.cinemashotgenerator.domain.shot.ActionSound
import com.operaboys.cinemashotgenerator.domain.shot.CharacterSound
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.shot.SourcedSettings
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// واحد ۱۵ — قدم ۳ (زیرقدم ۲، آخرین زیرقدم واحد ۱۵): تست end-to-end کامل collectData —
// تمام Repository ها (Settings/Versioning-independent/Impact-independent/ProjectDna/
// Asset/Scene/AudioContext) با هم یک PromptGenerationInput واقعی می‌سازند.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PromptGenerationRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: PromptGenerationRepository
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = PromptGenerationRepository(
            shotDao = database.shotDao(),
            sceneDao = database.sceneDao(),
            projectDnaRepository = ProjectDnaRepository(database.projectDnaDao()),
            assetRepository = AssetRepository(database.assetDao()),
            settingsResolutionRepository = SettingsResolutionRepository(database.shotDao(), database.sceneDao()),
            audioContextRepository = AudioContextRepository(database.audioContextDao())
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val cameraOverride = CameraSettings(
        angle = CameraAngle.LOW,
        distance = CameraDistance.CLOSE_UP,
        movement = CameraMovement.Basic(type = BasicMovementType.DOLLY_IN, speed = "slow"),
        lensType = LensType.TELEPHOTO,
        depthOfField = DepthOfField.SHALLOW,
        focusMode = FocusMode.SUBJECT_TRACKING,
        stabilization = Stabilization.GIMBAL,
        framing = Framing.CENTERED
    )
    private val lightingOverride = LightingSettings(LightingStyle.DRAMATIC_LIGHT, KeyLightPosition.SIDE, ContrastRatio.HIGH)
    private val environmentOverride = EnvironmentSettings(WeatherType.RAIN)

    private fun sampleShot() = Shot(
        shotId = "shot_001",
        sceneId = "scene_001",
        shotNumber = 1,
        shotDescription = "A detective walks into a dimly lit office",
        shotGoal = ShotGoal.ESTABLISHING,
        shotType = ShotType.CLOSE_UP,
        durationSeconds = 4f,
        motionLevel = MotionLevel.MODERATE,
        camera = SourcedSettings(source = "override", overrideValue = cameraOverride),
        lighting = SourcedSettings(source = "override", overrideValue = lightingOverride),
        environment = SourcedSettings(source = "override", overrideValue = environmentOverride),
        soundProfile = SoundProfile(enabled = true),
        characterIds = listOf("char_001"),
        objectIds = listOf("obj_001"),
        locationIds = listOf("loc_001")
    )

    private fun sampleDna() = ProjectDna(
        dnaId = "dna_001",
        projectId = "proj_001",
        coreIdentity = CoreIdentity(VisualStyle.CINEMATIC_STYLE, RealismLevel.GROUNDED, StyleConsistency.STRICT, locked = true),
        masterPalette = MasterPalette(ColorTemperature.WARM, SaturationLevel.MEDIUM, ContrastLevel.HIGH, "natural"),
        globalMoodBase = GlobalMoodBase(Mood.MYSTERIOUS, "medium", StyleConsistency.STRICT),
        outputConstraints = OutputConstraints(emptyMap(), 10, AspectRatio.ANAMORPHIC_2_39)
    )

    private fun sampleScene() = Scene(
        sceneId = "scene_001",
        sceneNumber = 1,
        narrativeRole = NarrativeRole.CLIMAX,
        location = SceneLocation(LocationType.OUTDOOR, "خیابان شلوغ شهری، شب"),
        timeOfDay = TimeOfDay.NIGHT,
        atmospherePrimary = Atmosphere.TENSE
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
            hair = Hair("black", "short", "short"),
            facialFeatures = FacialFeatures("brown")
        ),
        outfits = listOf(Outfit("outfit_01", "Default", "black jacket", isDefault = true))
    )

    private fun sampleObjectAsset() = ObjectAsset(
        assetId = "obj_001", name = "Service Pistol", description = "a worn revolver",
        subtype = ObjectSubtype.PERSONAL_PROP, size = "small", materialAndColor = "worn black metal"
    )

    private fun sampleLocationAsset() = LocationAsset(
        assetId = "loc_001", name = "Detective's Office",
        description = "a dimly lit office", environment = Environment("indoor", "small", "dim")
    )

    private fun sampleAudioContext() = AudioContext(
        audioContextId = "audio_001",
        shotId = "shot_001",
        ambientSounds = listOf(AmbientSound("rain", "heavy", "heavy rain", "weather")),
        actionSounds = listOf(ActionSound(1.5f, "footstep", "footsteps")),
        characterSounds = listOf(CharacterSound("char_001", "breathing", "heavy breathing"))
    )

    private suspend fun insertFullFixture(withAudioContext: Boolean) {
        database.projectDao().saveProject(
            ProjectEntity("proj_001", "Test Project", "2026-07-20T10:00:00Z", "2026-07-20T10:00:00Z")
        )
        ProjectDnaRepository(database.projectDnaDao()).saveProjectDna(sampleDna())
        SceneRepository(database.sceneDao()).saveScene("proj_001", sampleScene())
        database.shotDao().saveShot(
            ShotEntity("shot_001", "scene_001", json.encodeToString(ShotDto.serializer(), sampleShot().toDto()))
        )
        AssetRepository(database.assetDao()).saveCharacterAsset("proj_001", sampleCharacter())
        AssetRepository(database.assetDao()).saveObjectAsset("proj_001", sampleObjectAsset())
        AssetRepository(database.assetDao()).saveLocationAsset("proj_001", sampleLocationAsset())
        if (withAudioContext) {
            AudioContextRepository(database.audioContextDao()).saveAudioContext(sampleAudioContext())
        }
    }

    @Test
    fun `collectData assembles a real and complete PromptGenerationInput`() = runBlocking {
        insertFullFixture(withAudioContext = true)

        val result = repository.collectData("shot_001")

        assertTrue(result.isSuccess)
        val input = result.getOrThrow()

        assertEquals("dna_001", input.dna.dnaId)
        assertEquals(VisualStyle.CINEMATIC_STYLE, input.dna.coreIdentity.dominantVisualStyle)
        assertEquals("scene_001", input.scene.sceneId)
        assertEquals(NarrativeRole.CLIMAX, input.scene.narrativeRole)
        assertEquals("shot_001", input.shot.shotId)
        assertEquals("A detective walks into a dimly lit office", input.shot.shotDescription)
        assertEquals(1, input.characters.size)
        assertEquals("Detective John", input.characters[0].name)
        assertEquals(1, input.objects.size)
        assertEquals("Service Pistol", input.objects[0].name)
        assertEquals(1, input.locations.size)
        assertEquals("Detective's Office", input.locations[0].name)
        assertEquals(cameraOverride, input.camera)
        assertEquals(lightingOverride, input.lighting)
        assertEquals(environmentOverride, input.environment)
        assertNotNull(input.audioContext)
        assertEquals("audio_001", input.audioContext!!.audioContextId)
    }

    @Test
    fun `collectData succeeds with a null audioContext when none was generated`() = runBlocking {
        insertFullFixture(withAudioContext = false)

        val result = repository.collectData("shot_001")

        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrThrow().audioContext)
    }

    @Test
    fun `collectData fails when the shot does not exist`() = runBlocking {
        val result = repository.collectData("shot_missing")
        assertTrue(result.isFailure)
    }
}
