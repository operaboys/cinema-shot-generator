package com.operaboys.cinemashotgenerator.ui.project

import android.app.Application
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.DeviceBackupFileStorage
import com.operaboys.cinemashotgenerator.data.repository.FullProjectSnapshot
import com.operaboys.cinemashotgenerator.data.repository.ProjectEntityDto
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotDto
import com.operaboys.cinemashotgenerator.data.repository.ShotEntityDto
import com.operaboys.cinemashotgenerator.data.repository.SoundProfileDto
import com.operaboys.cinemashotgenerator.data.repository.exportProject as exportProjectFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

// رفع یافته‌ی معماری «عدم قرینگی Export/Import» (کشف‌شده در G14، مستند در
// docs/adr/064-...، رفع در docs/adr/065-...): importProject تا این قدم هیچ
// فراخوان‌کننده‌ای در UI نداشت — کاربر می‌توانست پروژه Export کند اما هرگز
// Import نکند. این تست‌ها سه سناریوی الزامی دستور کار را پوشش می‌دهند: Round-Trip
// واقعی (Export → حذف → Import همان فایل)، فایل خراب/نامعتبر، و یکپارچگی
// ارجاعی نقض‌شده (Shot → Scene ناموجود).

private const val PROJECT_ID = "proj_import_roundtrip"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProjectListViewModelImportTest {

    private lateinit var injectedDatabase: AppDatabase
    private lateinit var application: Application
    private lateinit var viewModel: ProjectListViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        injectedDatabase = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java).allowMainThreadQueries().build()
        viewModel = ProjectListViewModel(
            application = application,
            repository = ProjectRepository(injectedDatabase.projectDao(), idProvider = { PROJECT_ID }),
            database = injectedDatabase,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @After
    fun tearDown() {
        injectedDatabase.close()
    }

    @Test
    fun `round trip - exporting a project, deleting it, then importing the same file restores it`() = runBlocking {
        ProjectRepository(injectedDatabase.projectDao(), idProvider = { PROJECT_ID })
            .createProject("Round Trip Project").getOrThrow()

        val exportedPath = exportProjectFile(
            projectId = PROJECT_ID,
            projectDao = injectedDatabase.projectDao(),
            sceneDao = injectedDatabase.sceneDao(),
            shotDao = injectedDatabase.shotDao(),
            assetDao = injectedDatabase.assetDao(),
            projectDnaDao = injectedDatabase.projectDnaDao(),
            audioContextDao = injectedDatabase.audioContextDao(),
            backupFileStorage = DeviceBackupFileStorage(application)
        ).getOrThrow()

        // شبیه‌سازی «حذف تصادفی/نصب دیگر» — دقیقاً همان سناریوی محصولی مستند در
        // ADR-064 که این قدم رفعش می‌کند.
        val existingBeforeDelete = injectedDatabase.projectDao().loadProject(PROJECT_ID)
        assertNotNull(existingBeforeDelete)
        injectedDatabase.projectDao().deleteProject(existingBeforeDelete!!)
        assertNull(injectedDatabase.projectDao().loadProject(PROJECT_ID))

        val fileUri = Uri.fromFile(File(exportedPath))
        viewModel.importProject(fileUri.toString()).join()

        assertNull(
            "اگر Import موفق باشد، lastActionMessage نباید هیچ پیام خطایی داشته باشد",
            viewModel.lastActionMessage.value
        )
        val restored = injectedDatabase.projectDao().loadProject(PROJECT_ID)
        assertNotNull("پروژه باید بعد از Import دوباره در دیتابیس ظاهر شود", restored)
        assertEquals("Round Trip Project", restored?.projectName)
    }

    @Test
    fun `importing a corrupted file shows a real error and does not modify the database`() = runBlocking {
        val corruptFile = File(application.cacheDir, "corrupt_import_test.json")
        corruptFile.writeText("این یک JSON معتبر نیست { { {")

        viewModel.importProject(Uri.fromFile(corruptFile).toString()).join()

        assertNotNull(
            "فایل خراب باید پیام خطای واقعی نشان دهد، نه شکست بی‌صدا",
            viewModel.lastActionMessage.value
        )
        assertEquals(
            "هیچ پروژه‌ای نباید از یک فایل خراب ساخته شده باشد",
            0,
            injectedDatabase.projectDao().getAllProjects().first().size
        )
    }

    @Test
    fun `importing a file with a broken shot-to-scene reference is rejected with a real message`() = runBlocking {
        val brokenShotDto = ShotDto(
            shotId = "shot_broken",
            sceneId = "scene_that_does_not_exist",
            shotNumber = 1,
            shotDescription = "A shot referencing a missing scene",
            shotGoal = "ESTABLISHING",
            shotType = "WIDE",
            durationSeconds = 4f,
            motionLevel = "STATIC",
            soundProfile = SoundProfileDto(enabled = false)
        )
        val brokenSnapshot = FullProjectSnapshot(
            project = ProjectEntityDto(
                projectId = "proj_broken_ref",
                projectName = "Broken Reference Project",
                createdAt = "2026-01-01T00:00:00Z",
                lastModified = "2026-01-01T00:00:00Z",
                uiLanguage = "FA"
            ),
            scenes = emptyList(),
            shots = listOf(
                ShotEntityDto(
                    shotId = "shot_broken",
                    sceneId = "scene_that_does_not_exist",
                    shotDataJson = Json.encodeToString(ShotDto.serializer(), brokenShotDto)
                )
            ),
            assets = emptyList(),
            projectDna = null,
            audioContexts = emptyList()
        )
        val brokenFile = File(application.cacheDir, "broken_reference_import_test.json")
        brokenFile.writeText(Json.encodeToString(FullProjectSnapshot.serializer(), brokenSnapshot))

        viewModel.importProject(Uri.fromFile(brokenFile).toString()).join()

        val message = viewModel.lastActionMessage.value
        assertNotNull("یکپارچگی ارجاعی نقض‌شده باید رد شود، نه بی‌صدا Import شود", message)
        assertTrue(
            "پیام خطا باید واقعاً به یکپارچگی ارجاعی اشاره کند: $message",
            message?.contains("یکپارچگی ارجاعی") == true
        )
        assertNull(
            "طبق تضمین صفر-نوشتن مستند در ExportImportRepository.kt، پروژه نباید نوشته شده باشد",
            injectedDatabase.projectDao().loadProject("proj_broken_ref")
        )
    }
}
