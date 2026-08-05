package com.operaboys.cinemashotgenerator.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.SceneLocation
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
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

// واحد ۱۵ — قدم ۳ (زیرقدم ۲): تست end-to-end round-trip واقعی برای Scene.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SceneRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: SceneRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = SceneRepository(database.sceneDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val fullScene = Scene(
        sceneId = "scene_001",
        sceneTitle = "The Confrontation",
        sceneNumber = 3,
        narrativeRole = NarrativeRole.CLIMAX,
        location = SceneLocation(type = LocationType.OUTDOOR, description = "خیابان شلوغ شهری، شب"),
        timeOfDay = TimeOfDay.NIGHT,
        atmospherePrimary = Atmosphere.TENSE,
        atmosphereSecondary = Atmosphere.MYSTERIOUS,
        shotCount = 5
    )

    @Test
    fun `saveScene then loadScene round-trips the full structure exactly`() = runBlocking {
        database.projectDao().saveProject(
            ProjectEntity("proj_001", "Test", "2026-07-20T10:00:00Z", "2026-07-20T10:00:00Z")
        )

        val saveResult = repository.saveScene("proj_001", fullScene)
        assertTrue(saveResult.isSuccess)

        val loadResult = repository.loadScene("scene_001")
        assertTrue(loadResult.isSuccess)
        assertEquals(fullScene, loadResult.getOrThrow())
    }

    @Test
    fun `loadScene returns null when the scene does not exist`() = runBlocking {
        val result = repository.loadScene("scene_missing")
        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrThrow())
    }

    // واحد ۱۶ فاز ۴ — قدم ۱: تست‌های loadAllScenes (تازه اضافه‌شده — بخش الف دستور کار).

    @Test
    fun `loadAllScenes returns an empty list for a project with no scenes`() = runBlocking {
        database.projectDao().saveProject(
            ProjectEntity("proj_empty", "Empty", "2026-08-06T10:00:00Z", "2026-08-06T10:00:00Z")
        )
        val scenes = repository.loadAllScenes("proj_empty").first()
        assertEquals(emptyList<Any>(), scenes)
    }

    @Test
    fun `loadAllScenes returns every saved scene for the project, ignoring other projects`() = runBlocking {
        database.projectDao().saveProject(
            ProjectEntity("proj_001", "Test", "2026-07-20T10:00:00Z", "2026-07-20T10:00:00Z")
        )
        database.projectDao().saveProject(
            ProjectEntity("proj_other", "Other", "2026-07-20T10:00:00Z", "2026-07-20T10:00:00Z")
        )
        val secondScene = fullScene.copy(sceneId = "scene_002", sceneNumber = 4)
        val otherProjectScene = fullScene.copy(sceneId = "scene_other", sceneNumber = 1)

        repository.saveScene("proj_001", fullScene)
        repository.saveScene("proj_001", secondScene)
        repository.saveScene("proj_other", otherProjectScene)

        val scenes = repository.loadAllScenes("proj_001").first()
        assertEquals(setOf(fullScene, secondScene), scenes.toSet())
    }
}
