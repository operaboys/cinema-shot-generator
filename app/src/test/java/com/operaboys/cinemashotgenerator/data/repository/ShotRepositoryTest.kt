package com.operaboys.cinemashotgenerator.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.data.entity.SceneEntity
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
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.shot.SourcedSettings
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// واحد ۱۶ فاز ۴ — قدم ۲ — بخش الف: اولین تست‌های ShotRepository — تا این قدم هیچ
// تستی برای این Repository وجود نداشت (فقط saveShot بدون هیچ متد خواندنی). طبق
// دستور کار: نمایش صحیح شات‌های یک صحنه (loadAllShots). هم‌الگو با
// SceneRepositoryTest.kt.
//
// MIGRATED (فاز ۴ قدم ۳): ۶ تست Round-Trip جداگانه برای هر ۶ Variant واقعی
// CameraMovement — طبق دستور کار صریح («این حیاتی است چون یک باگ مشابه قبلی، DTO
// ناقص، در Migration‌های قبلی چندبار رخ داده»). با grep تأیید شد CameraSettingsDto/
// CameraMovementDto (واحد ۱۵) از قبل هر ۶ Variant را پشتیبانی می‌کنند — این تست‌ها
// آن پوشش را برای اولین بار از طریق ShotRepository واقعی اثبات می‌کنند (تست قبلی
// این فایل فقط SourcedSettings() پیش‌فرض/خالی را برای camera امتحان می‌کرد).

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShotRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ShotRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = ShotRepository(database.shotDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val fullShot = Shot(
        shotId = "shot_001",
        sceneId = "scene_001",
        shotNumber = 2,
        shotTitle = "Close-up on the letter",
        shotDescription = "A trembling hand unfolds an old letter under dim candlelight.",
        shotGoal = ShotGoal.EMOTIONAL,
        shotType = ShotType.CLOSE_UP,
        durationSeconds = 5f,
        motionLevel = MotionLevel.SUBTLE,
        beats = listOf(Beat(timestampSeconds = 1.5f, eventType = BeatEventType.SUBJECT_ACTION, description = "hand trembles")),
        soundProfile = SoundProfile(enabled = true),
        negativePromptOverride = "no modern objects",
        characterIds = listOf("char_001"),
        overrideScene = true,
        // تکمیل Rule یتیم — قدم ۱ از ۴ (ADR-106): پوشش صریح Round-Trip برای فیلد تازه.
        cinematicModeOverride = CinematicMode.LONG_TAKE
    )

    private suspend fun seedProjectAndScene(projectId: String, sceneId: String) {
        database.projectDao().saveProject(
            ProjectEntity(projectId, "Test", "2026-08-07T10:00:00Z", "2026-08-07T10:00:00Z")
        )
        database.sceneDao().saveScene(SceneEntity(sceneId = sceneId, projectId = projectId, sceneDataJson = "{}"))
    }

    @Test
    fun `saveShot then loadShot round-trips the full structure exactly, including negativePromptOverride`() = runBlocking {
        seedProjectAndScene("proj_001", "scene_001")

        val saveResult = repository.saveShot(fullShot)
        assertTrue(saveResult.isSuccess)

        val loadResult = repository.loadShot("shot_001")
        assertTrue(loadResult.isSuccess)
        assertEquals(fullShot, loadResult.getOrThrow())
    }

    @Test
    fun `loadShot returns null when the shot does not exist`() = runBlocking {
        val result = repository.loadShot("shot_missing")
        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrThrow())
    }

    @Test
    fun `loadAllShots returns an empty list for a scene with no shots`() = runBlocking {
        seedProjectAndScene("proj_001", "scene_empty")
        val shots = repository.loadAllShots("scene_empty").first()
        assertEquals(emptyList<Any>(), shots)
    }

    @Test
    fun `loadAllShots returns every saved shot for the scene, sorted by shotNumber, ignoring other scenes`() = runBlocking {
        seedProjectAndScene("proj_001", "scene_001")
        database.sceneDao().saveScene(SceneEntity(sceneId = "scene_other", projectId = "proj_001", sceneDataJson = "{}"))

        val firstShot = fullShot.copy(shotId = "shot_first", shotNumber = 1)
        val secondShot = fullShot // shotNumber = 2
        val otherSceneShot = fullShot.copy(shotId = "shot_other", sceneId = "scene_other", shotNumber = 1)

        // عمداً به ترتیب معکوس ذخیره می‌شوند تا مرتب‌سازی واقعاً روی shotNumber باشد، نه ترتیب درج.
        repository.saveShot(secondShot)
        repository.saveShot(firstShot)
        repository.saveShot(otherSceneShot)

        val shots = repository.loadAllShots("scene_001").first()
        assertEquals(listOf(firstShot, secondShot), shots)
    }

    private fun cameraSettings(movement: CameraMovement) = CameraSettings(
        angle = CameraAngle.DUTCH,
        distance = CameraDistance.WIDE,
        movement = movement,
        lensType = LensType.TELEPHOTO,
        depthOfField = DepthOfField.SHALLOW,
        focusMode = FocusMode.MANUAL,
        stabilization = Stabilization.TRIPOD,
        framing = Framing.ASYMMETRICAL
    )

    private suspend fun assertCameraRoundTrip(movement: CameraMovement) {
        seedProjectAndScene("proj_001", "scene_001")
        val shot = fullShot.copy(camera = SourcedSettings(source = "override", overrideValue = cameraSettings(movement)))

        val saveResult = repository.saveShot(shot)
        assertTrue(saveResult.isSuccess)

        val loadResult = repository.loadShot("shot_001")
        assertTrue(loadResult.isSuccess)
        assertEquals(shot, loadResult.getOrThrow())
    }

    @Test
    fun `saveShot then loadShot round-trips CameraMovement Basic exactly`() = runBlocking {
        assertCameraRoundTrip(CameraMovement.Basic(type = BasicMovementType.CRANE, speed = "slow"))
    }

    @Test
    fun `saveShot then loadShot round-trips CameraMovement Orbit exactly`() = runBlocking {
        assertCameraRoundTrip(CameraMovement.Orbit(degrees = 180, speed = "medium", maintainEyeLevel = true))
    }

    @Test
    fun `saveShot then loadShot round-trips CameraMovement DronePath exactly`() = runBlocking {
        assertCameraRoundTrip(CameraMovement.DronePath(altitudeChange = "ascending", pathType = "spiral", speed = "fast"))
    }

    @Test
    fun `saveShot then loadShot round-trips CameraMovement DollyZoom exactly`() = runBlocking {
        assertCameraRoundTrip(CameraMovement.DollyZoom(focalStart = 35, focalEnd = 85, direction = "in"))
    }

    @Test
    fun `saveShot then loadShot round-trips CameraMovement HandheldShake exactly`() = runBlocking {
        assertCameraRoundTrip(CameraMovement.HandheldShake(intensity = 7, frequency = "high"))
    }

    @Test
    fun `saveShot then loadShot round-trips CameraMovement Compound exactly`() = runBlocking {
        assertCameraRoundTrip(CameraMovement.Compound(primary = "dolly_in", secondary = "orbit", sync = "matched"))
    }

    @Test
    fun `saveShot then loadShot round-trips LightingSettings and EnvironmentSettings with every nullable field populated`() = runBlocking {
        seedProjectAndScene("proj_001", "scene_001")
        val lighting = LightingSettings(
            style = LightingStyle.LOW_KEY,
            keyLightPosition = KeyLightPosition.BOTTOM,
            contrastRatio = ContrastRatio.HIGH,
            fillLight = FillLight.STRONG,
            colorTemperature = ColorTemperature.COLD,
            shadowQuality = ShadowQuality.HARD_SHADOWS,
            lightSourceCount = LightSourceCount.MULTI,
            lightingMotivation = LightingMotivation.FIRE
        )
        val environment = EnvironmentSettings(
            weatherType = WeatherType.STORM,
            weatherIntensity = WeatherIntensity.HEAVY,
            windStrength = WindStrength.STRONG,
            groundState = GroundState.MUDDY,
            visibility = Visibility.LOW,
            temperatureFeel = TemperatureFeel.COLD,
            environmentalMotion = listOf(EnvironmentalMotion.DUST_CLOUDS, EnvironmentalMotion.FLYING_DEBRIS)
        )
        val shot = fullShot.copy(
            lighting = SourcedSettings(source = "override", overrideValue = lighting),
            environment = SourcedSettings(source = "override", overrideValue = environment),
            soundProfile = SoundProfile(
                enabled = true,
                ambientAutoGenerate = true,
                ambientSounds = listOf(AmbientSound(type = "rain", intensity = "heavy", description = "torrential rain and wind", source = "auto_generated")),
                actionSounds = listOf(ActionSound(timestampSeconds = 2.5f, type = "door_slam", description = "heavy door slamming shut")),
                characterSounds = listOf(CharacterSound(characterId = "char_001", type = "gasp", description = "sharp intake of breath"))
            )
        )

        val saveResult = repository.saveShot(shot)
        assertTrue(saveResult.isSuccess)

        val loadResult = repository.loadShot("shot_001")
        assertTrue(loadResult.isSuccess)
        assertEquals(shot, loadResult.getOrThrow())
    }

    @Test
    fun `saveShot then loadShot round-trips LightingSettings and EnvironmentSettings with every nullable field null`() = runBlocking {
        seedProjectAndScene("proj_001", "scene_001")
        val lighting = LightingSettings(
            style = LightingStyle.NATURAL_LIGHT,
            keyLightPosition = KeyLightPosition.FRONT,
            contrastRatio = ContrastRatio.LOW,
            fillLight = null,
            colorTemperature = null,
            shadowQuality = null,
            lightSourceCount = null,
            lightingMotivation = null
        )
        val environment = EnvironmentSettings(
            weatherType = WeatherType.CLEAR,
            weatherIntensity = null,
            windStrength = null,
            groundState = null,
            visibility = null,
            temperatureFeel = null,
            environmentalMotion = emptyList()
        )
        val shot = fullShot.copy(
            lighting = SourcedSettings(source = "override", overrideValue = lighting),
            environment = SourcedSettings(source = "override", overrideValue = environment)
        )

        val saveResult = repository.saveShot(shot)
        assertTrue(saveResult.isSuccess)

        val loadResult = repository.loadShot("shot_001")
        assertTrue(loadResult.isSuccess)
        assertEquals(shot, loadResult.getOrThrow())
    }
}
