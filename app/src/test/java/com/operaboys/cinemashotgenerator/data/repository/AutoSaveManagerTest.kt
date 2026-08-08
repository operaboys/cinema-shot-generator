package com.operaboys.cinemashotgenerator.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// واحد ۱۵ — تست end-to-end واقعی برای AutoSaveManager (تکمیل ADR-017).

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutoSaveManagerTest {

    private lateinit var database: AppDatabase
    private lateinit var manager: AutoSaveManager

    private val fixedClockValue = "2026-07-21T12:00:00Z"

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        manager = AutoSaveManager(database.projectDao(), clock = { fixedClockValue })
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val originalProject = ProjectEntity(
        projectId = "proj_001",
        projectName = "Original Name",
        createdAt = "2026-07-01T00:00:00Z",
        lastModified = "2026-07-01T00:00:00Z",
        uiLanguage = "fa"
    )

    @Test
    fun `saveIfDirty with isDirty true actually saves and updates lastModified`() = runBlocking {
        database.projectDao().saveProject(originalProject)

        val result = manager.saveIfDirty(originalProject.copy(projectName = "Renamed"), isDirty = true)

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrThrow())

        val loaded = database.projectDao().loadProject("proj_001")
        assertEquals("Renamed", loaded?.projectName)
        assertEquals(fixedClockValue, loaded?.lastModified)
    }

    @Test
    fun `saveIfDirty with isDirty false does not save anything`() = runBlocking {
        database.projectDao().saveProject(originalProject)

        val result = manager.saveIfDirty(originalProject.copy(projectName = "Should Not Persist"), isDirty = false)

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrThrow())

        val loaded = database.projectDao().loadProject("proj_001")
        assertEquals("Original Name", loaded?.projectName)
        assertEquals("2026-07-01T00:00:00Z", loaded?.lastModified)
    }

    @Test
    fun `saveIfDirty with isDirty true on a brand new project id inserts it`() = runBlocking {
        val newProject = ProjectEntity(
            projectId = "proj_new",
            projectName = "New Project",
            createdAt = "2026-07-21T00:00:00Z",
            lastModified = "2026-07-21T00:00:00Z"
        )

        val result = manager.saveIfDirty(newProject, isDirty = true)

        assertTrue(result.isSuccess)
        val loaded = database.projectDao().loadProject("proj_new")
        assertTrue(loaded != null)
        assertEquals("New Project", loaded?.projectName)
    }

    // واحد ۱۶ فاز ۶ — قدم ۱: تست `touch` تازه (بدون تغییر در `saveIfDirty` موجود بالا).

    @Test
    fun `touch on an existing project loads it and updates lastModified`() = runBlocking {
        database.projectDao().saveProject(originalProject)

        val result = manager.touch("proj_001")

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrThrow())
        val loaded = database.projectDao().loadProject("proj_001")
        assertEquals(fixedClockValue, loaded?.lastModified)
        assertEquals("Original Name", loaded?.projectName)
    }

    @Test
    fun `touch on a project id that does not exist returns success false without throwing`() = runBlocking {
        val result = manager.touch("proj_does_not_exist")

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrThrow())
    }
}
