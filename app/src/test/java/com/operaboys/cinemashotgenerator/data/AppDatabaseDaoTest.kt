package com.operaboys.cinemashotgenerator.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.data.entity.SceneEntity
import com.operaboys.cinemashotgenerator.data.entity.ShotEntity
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

// واحد ۱۵ — Project Storage System (قدم ۱: تست DAO با Room واقعی)
// منبع حقیقت: docs/blueprints/15-project-storage.md
//
// انتخاب Robolectric (نه instrumented test در androidTest): این محیط Sandbox دسترسی
// به امولاتور/دستگاه واقعی اندروید ندارد (محدودیت شناخته‌شده‌ی همین پروژه، مشابه
// محدودیت gradle wrapper در قدم‌های قبلی) — پس تست‌های androidTest اصلاً قابل اجرا
// نیستند. Robolectric کلاس‌های فریمورک اندروید (از جمله SQLite/Room) را روی JVM خالص
// شبیه‌سازی می‌کند، پس با همان gradle :app:testDebugUnitTest معمولی اجرا می‌شود — یک
// Room Database واقعی و درون‌حافظه‌ای (inMemoryDatabaseBuilder) در این تست‌ها ساخته
// می‌شود، نه Mock. @Config(sdk = [34]) صراحتاً یک نسخه‌ی SDK پایدار و پشتیبانی‌شده را
// هدف می‌گیرد (نه compileSdk=36 پروژه که ممکن است هنوز در جدیدترین Robolectric کاملاً
// پوشش داده نشده باشد) — این تصمیم صرفاً برای پایداری تست است، هیچ ربطی به minSdk/
// targetSdk واقعی اپ ندارد.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- ProjectDao ---

    @Test
    fun `saveProject then loadProject returns the same project`() = runBlocking {
        val project = ProjectEntity(
            projectId = "proj_001", projectName = "Test Project",
            createdAt = "2026-07-20T10:00:00Z", lastModified = "2026-07-20T10:00:00Z"
        )
        database.projectDao().saveProject(project)

        val loaded = database.projectDao().loadProject("proj_001")
        assertEquals(project, loaded)
    }

    @Test
    fun `getAllProjects emits saved projects ordered by lastModified descending`() = runBlocking {
        database.projectDao().saveProject(
            ProjectEntity("proj_old", "Old", "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z")
        )
        database.projectDao().saveProject(
            ProjectEntity("proj_new", "New", "2026-07-20T00:00:00Z", "2026-07-20T00:00:00Z")
        )

        val all = database.projectDao().getAllProjects().first()
        assertEquals(listOf("proj_new", "proj_old"), all.map { it.projectId })
    }

    @Test
    fun `deleteProject removes it from loadProject`() = runBlocking {
        val project = ProjectEntity("proj_001", "Test", "2026-07-20T10:00:00Z", "2026-07-20T10:00:00Z")
        database.projectDao().saveProject(project)

        database.projectDao().deleteProject(project)

        assertNull(database.projectDao().loadProject("proj_001"))
    }

    // --- SceneDao (foreign key to projects) ---

    @Test
    fun `saveScene requires an existing project and is retrievable by project`() = runBlocking {
        database.projectDao().saveProject(
            ProjectEntity("proj_001", "Test", "2026-07-20T10:00:00Z", "2026-07-20T10:00:00Z")
        )
        val scene = SceneEntity(sceneId = "scene_001", projectId = "proj_001", sceneDataJson = "{}")
        database.sceneDao().saveScene(scene)

        val scenes = database.sceneDao().getScenesForProject("proj_001").first()
        assertEquals(listOf(scene), scenes)
    }

    // --- ShotDao (paged, foreign key to scenes) ---

    @Test
    fun `getShotsPaged returns the requested slice ordered by insertion`() = runBlocking {
        database.projectDao().saveProject(
            ProjectEntity("proj_001", "Test", "2026-07-20T10:00:00Z", "2026-07-20T10:00:00Z")
        )
        database.sceneDao().saveScene(SceneEntity("scene_001", "proj_001", "{}"))
        (1..5).forEach { i ->
            database.shotDao().saveShot(ShotEntity(shotId = "shot_00$i", sceneId = "scene_001", shotDataJson = "{}"))
        }

        val page = database.shotDao().getShotsPaged(sceneId = "scene_001", limit = 2, offset = 2)
        assertEquals(2, page.size)
        assertTrue(page.all { it.sceneId == "scene_001" })
    }

    // --- ProjectTransactionDao (atomicity) ---

    @Test
    fun `saveShotWithSceneUpdate persists both the shot and the updated scene atomically`() = runBlocking {
        database.projectDao().saveProject(
            ProjectEntity("proj_001", "Test", "2026-07-20T10:00:00Z", "2026-07-20T10:00:00Z")
        )
        database.sceneDao().saveScene(SceneEntity("scene_001", "proj_001", "{\"shotCount\":0}"))

        val shot = ShotEntity("shot_001", "scene_001", "{}")
        val updatedScene = SceneEntity("scene_001", "proj_001", "{\"shotCount\":1}")
        database.projectTransactionDao().saveShotWithSceneUpdate(shot, updatedScene)

        assertEquals(shot, database.shotDao().loadShot("shot_001"))
        assertEquals(updatedScene, database.sceneDao().loadScene("scene_001"))
    }
}
