package com.operaboys.cinemashotgenerator.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.entity.SceneEntity
import com.operaboys.cinemashotgenerator.data.entity.ShotEntity
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// واحد ۱۶ فاز ۱ — تست‌های ProjectRepository با Room واقعی (نه Mock)، هم‌الگو با
// SceneRepositoryTest/... موجود. جزئیات در docs/adr/044-unit16-phase1-app-shell.md.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProjectRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ProjectRepository
    private var idCounter = 0
    private var clockValue = "2026-08-04T10:00:00Z"

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = ProjectRepository(
            projectDao = database.projectDao(),
            idProvider = { "proj_test_${idCounter++}" },
            clock = { clockValue }
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `createProject then observeProjectSummaries returns it with zero scene-shot counts`() = runBlocking {
        val created = repository.createProject("My Movie", Language.FA).getOrThrow()

        val summaries = repository.observeProjectSummaries().first()

        assertEquals(1, summaries.size)
        assertEquals(created.projectId, summaries[0].project.projectId)
        assertEquals("My Movie", summaries[0].project.projectName)
        assertEquals(EntityState.DRAFT, summaries[0].project.state)
        assertEquals(0, summaries[0].sceneCount)
        assertEquals(0, summaries[0].shotCount)
    }

    @Test
    fun `observeProjectSummaries counts scenes and shots correctly through the scene-shot join`() = runBlocking {
        val project = repository.createProject("Counted").getOrThrow()
        database.sceneDao().saveScene(SceneEntity("scene_1", project.projectId, "{}"))
        database.sceneDao().saveScene(SceneEntity("scene_2", project.projectId, "{}"))
        database.shotDao().saveShot(ShotEntity("shot_1", "scene_1", "{}"))
        database.shotDao().saveShot(ShotEntity("shot_2", "scene_1", "{}"))
        database.shotDao().saveShot(ShotEntity("shot_3", "scene_2", "{}"))

        val summary = repository.observeProjectSummary(project.projectId).first()

        assertEquals(2, summary?.sceneCount)
        assertEquals(3, summary?.shotCount)
    }

    @Test
    fun `renameProject persists the new name and updates lastModified`() = runBlocking {
        val project = repository.createProject("Old Name").getOrThrow()
        clockValue = "2026-08-04T11:00:00Z"

        repository.renameProject(project.projectId, "New Name").getOrThrow()

        val summary = repository.observeProjectSummary(project.projectId).first()
        assertEquals("New Name", summary?.project?.projectName)
        assertEquals("2026-08-04T11:00:00Z", summary?.project?.lastModified)
    }

    @Test
    fun `archiveProject succeeds for a FINAL project and fails for a fresh DRAFT project`() = runBlocking {
        val draftProject = repository.createProject("Draft").getOrThrow()
        val blockedResult = repository.archiveProject(draftProject.projectId)
        assertTrue(blockedResult.isFailure)
        assertEquals(EntityState.DRAFT, repository.observeProjectSummary(draftProject.projectId).first()?.project?.state)

        // یک پروژه‌ی FINAL را مستقیماً می‌سازیم (چون هنوز UI انتقال وضعیت وجود ندارد)
        val finalProject = repository.createProject("Final").getOrThrow()
        database.projectDao().loadProject(finalProject.projectId)!!.let {
            database.projectDao().saveProject(it.copy(state = EntityState.FINAL.name))
        }

        val archivedResult = repository.archiveProject(finalProject.projectId)
        assertTrue(archivedResult.isSuccess)
        assertEquals(EntityState.ARCHIVED, repository.observeProjectSummary(finalProject.projectId).first()?.project?.state)
    }

    @Test
    fun `duplicateProject copies only the project shell as a fresh DRAFT, with a new id`() = runBlocking {
        val original = repository.createProject("Original", Language.EN).getOrThrow()

        val duplicate = repository.duplicateProject(original.projectId).getOrThrow()

        assertTrue(duplicate.projectId != original.projectId)
        assertEquals("Original (کپی)", duplicate.projectName)
        assertEquals(EntityState.DRAFT, duplicate.state)
        assertEquals(Language.EN, duplicate.uiLanguage)

        val summaries = repository.observeProjectSummaries().first()
        assertEquals(2, summaries.size)
    }

    @Test
    fun `deleteProject removes it from observeProjectSummaries`() = runBlocking {
        val project = repository.createProject("To Delete").getOrThrow()
        assertEquals(1, repository.observeProjectSummaries().first().size)

        repository.deleteProject(project.projectId).getOrThrow()

        assertEquals(0, repository.observeProjectSummaries().first().size)
        assertNull(database.projectDao().loadProject(project.projectId))
    }

    @Test
    fun `deleteProject cascades to its scenes and shots (existing ForeignKey CASCADE)`() = runBlocking {
        val project = repository.createProject("Cascade").getOrThrow()
        database.sceneDao().saveScene(SceneEntity("scene_x", project.projectId, "{}"))
        database.shotDao().saveShot(ShotEntity("shot_x", "scene_x", "{}"))

        repository.deleteProject(project.projectId).getOrThrow()

        assertNull(database.sceneDao().loadScene("scene_x"))
        assertNull(database.shotDao().loadShot("shot_x"))
    }
}
