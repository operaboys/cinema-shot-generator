package com.operaboys.cinemashotgenerator.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.data.entity.SceneEntity
import com.operaboys.cinemashotgenerator.data.entity.ShotEntity
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// واحد ۱۵ — قدم ۲: تست end-to-end واقعی: داده در ShotDao/SceneDao ذخیره می‌شود →
// SettingsResolutionRepository آن را می‌خواند و deserialize می‌کند → resolveCameraSettings
// واقعی واحد ۰۵ روی آن اجرا می‌شود → نتیجه تأیید می‌شود.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsResolutionRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: SettingsResolutionRepository
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = SettingsResolutionRepository(database.shotDao(), database.sceneDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val overrideCamera = CameraSettings(
        angle = CameraAngle.LOW,
        distance = CameraDistance.CLOSE_UP,
        movement = CameraMovement.Basic(type = BasicMovementType.DOLLY_IN, speed = "slow"),
        lensType = LensType.TELEPHOTO,
        depthOfField = DepthOfField.SHALLOW,
        focusMode = FocusMode.SUBJECT_TRACKING,
        stabilization = Stabilization.GIMBAL,
        framing = Framing.CENTERED
    )

    private fun sampleShot(cameraOverride: CameraSettings?, cameraSource: String) = Shot(
        shotId = "shot_001",
        sceneId = "scene_001",
        shotNumber = 1,
        shotDescription = "A detective walks into a dimly lit office",
        shotGoal = ShotGoal.ESTABLISHING,
        shotType = ShotType.CLOSE_UP,
        durationSeconds = 4f,
        motionLevel = MotionLevel.MODERATE,
        camera = SourcedSettings(source = cameraSource, overrideValue = cameraOverride),
        soundProfile = SoundProfile(enabled = true)
    )

    private suspend fun insertProjectAndScene() {
        database.projectDao().saveProject(
            ProjectEntity("proj_001", "Test", "2026-07-20T10:00:00Z", "2026-07-20T10:00:00Z")
        )
        database.sceneDao().saveScene(SceneEntity("scene_001", "proj_001", "{}"))
    }

    @Test
    fun `resolveCameraSettingsFor returns the shot's override when source is override`() = runBlocking {
        insertProjectAndScene()
        val shot = sampleShot(cameraOverride = overrideCamera, cameraSource = "override")
        val shotJson = json.encodeToString(ShotDto.serializer(), shot.toDto())
        database.shotDao().saveShot(ShotEntity(shot.shotId, shot.sceneId, shotJson))

        val result = repository.resolveCameraSettingsFor("shot_001")

        assertTrue(result.isSuccess)
        assertEquals(overrideCamera, result.getOrThrow())
    }

    @Test
    fun `resolveCameraSettingsFor fails when source is scene and no scene default exists`() = runBlocking {
        insertProjectAndScene()
        val shot = sampleShot(cameraOverride = null, cameraSource = "scene")
        val shotJson = json.encodeToString(ShotDto.serializer(), shot.toDto())
        database.shotDao().saveShot(ShotEntity(shot.shotId, shot.sceneId, shotJson))

        val result = repository.resolveCameraSettingsFor("shot_001")

        assertTrue(result.isFailure)
    }

    @Test
    fun `resolveCameraSettingsFor fails when the shot does not exist`() = runBlocking {
        val result = repository.resolveCameraSettingsFor("shot_missing")
        assertTrue(result.isFailure)
    }
}
