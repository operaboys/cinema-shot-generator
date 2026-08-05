package com.operaboys.cinemashotgenerator.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.data.entity.SceneEntity
import com.operaboys.cinemashotgenerator.domain.shot.Beat
import com.operaboys.cinemashotgenerator.domain.shot.BeatEventType
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
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
        overrideScene = true
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
}
