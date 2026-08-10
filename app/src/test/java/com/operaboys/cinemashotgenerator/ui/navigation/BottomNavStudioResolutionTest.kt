package com.operaboys.cinemashotgenerator.ui.navigation

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.studio.STUDIO_BACK_BUTTON_TAG
import com.operaboys.cinemashotgenerator.ui.studio.STUDIO_TITLE_TAG
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

// رفع G6 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، docs/adr/062-...):
// تست‌های End-to-End واقعی دکمه‌ی Studio نوار پایین — قبلاً همیشه به یک
// PLACEHOLDER_ACTIVE_PROJECT_ID ثابت Navigate می‌کرد، مستقل از پروژه‌ی واقعی
// کاربر. فایل جدا از AppNavigationTest.kt چون این تست‌ها عمداً از صفر (بدون هیچ
// پروژه‌ی از‌پیش‌ساخته‌شده در setUp) شروع می‌شوند.

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BottomNavStudioResolutionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dataStoreFileName: String
    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var database: AppDatabase
    private lateinit var projectListViewModel: ProjectListViewModel
    private lateinit var projectRepository: ProjectRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreFileName = "bottom_nav_studio_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(dataStoreFileName) }
        )
        workflowViewModel = WorkflowViewModel(
            application = context.applicationContext as Application,
            dataStore = dataStore,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        var nextId = 0
        var nextClock = 0
        projectRepository = ProjectRepository(
            database.projectDao(),
            idProvider = { "proj_bottom_nav_${nextId++}" },
            // clock افزایشی صریح — تا ترتیب lastModified دو پروژه قطعی باشد (نه
            // وابسته به سرعت اجرای تست).
            clock = { "2026-01-01T00:00:%02dZ".format(nextClock++) }
        )
        projectListViewModel = ProjectListViewModel(
            application = context.applicationContext as Application,
            repository = projectRepository
        )
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        database.close()
    }

    private fun setContent() {
        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(workflowViewModel = workflowViewModel, projectListViewModel = projectListViewModel)
            }
        }
    }

    @Test
    fun `clicking Studio in the bottom nav with zero projects redirects to Projects instead of a fake placeholder project`() {
        setContent()

        composeRule.onNodeWithTag(BOTTOM_NAV_STUDIO_TAG).performClick()

        // «پروژه‌ها» (nav.projects) عیناً با عنوان صفحه‌ی Projects (projects.title)
        // یکسان است و نوار پایین همیشه در Composition حاضر است — پس hasText اینجا
        // Ambiguous می‌شود (همان یافته‌ی مستندشده‌ی NavDrawerNavigationTest.kt).
        // بررسی «انتخاب‌شدگی» آیتم نوار پایین Projects، اثبات دقیق‌تری از رسیدن
        // به مقصد واقعی است.
        composeRule.waitUntilExactlyOneExists(hasTestTag(BOTTOM_NAV_PROJECTS_TAG), timeoutMillis = 10_000)
        composeRule.onNodeWithTag(BOTTOM_NAV_PROJECTS_TAG).assertIsSelected()
    }

    @Test
    fun `clicking Studio in the bottom nav with no active session opens the most recently modified real project`() {
        runBlocking {
            projectRepository.createProject("Older Project").getOrThrow()
            projectRepository.createProject("Newer Project").getOrThrow()
        }
        setContent()
        // هم‌الگو با AppNavigationTest.kt (رفع G6): projectSummaries روی Executor
        // داخلی خودِ Room دوباره Query می‌شود — بدون این انتظار صریح، ممکن است
        // کلیک Studio پیش از بارگذاری واقعی لیست رخ دهد.
        composeRule.waitUntil(timeoutMillis = 5_000) { projectListViewModel.projectSummaries.value.size == 2 }

        composeRule.onNodeWithTag(BOTTOM_NAV_STUDIO_TAG).performClick()

        composeRule.waitUntilExactlyOneExists(hasTestTag(STUDIO_BACK_BUTTON_TAG), timeoutMillis = 10_000)
        // یافته‌ی واقعی دیباگ این تست: عنوان Studio هیچ testTag ای نداشت — با
        // debug print تأیید شد که hasText("Newer Project", substring=true) دقیقاً
        // ۲ گره پیدا می‌کرد (نه ۱)، یعنی Navigate واقعاً به پروژه‌ی درست رفته بود؛
        // این خودِ assertion (نه رفتار Production) Ambiguous بود. رفع شد با افزودن
        // STUDIO_TITLE_TAG (StudioShell.kt) — اثبات دقیق‌تر و بدون ابهام.
        composeRule.onNodeWithTag(STUDIO_TITLE_TAG).assertTextEquals("Newer Project")
    }
}
