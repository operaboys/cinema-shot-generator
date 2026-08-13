package com.operaboys.cinemashotgenerator.ui.project

import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState
import com.operaboys.cinemashotgenerator.ui.home.CREATE_PROJECT_NAME_FIELD_TAG
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.navigation.BOTTOM_NAV_PROJECTS_TAG
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.studio.STUDIO_BACK_BUTTON_TAG
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

// یافته‌ی ۳ appendix ADR-081 (ADR-082): ردیف چیپ فیلتر وضعیت پروژه‌ها که قبلاً
// کاملاً غایب بود (فهرست همیشه بدون فیلتر همه‌ی پروژه‌ها را نشان می‌داد). تست
// End-to-End واقعی اثبات می‌کند: کلیک روی چیپ DRAFT پروژه‌ی ARCHIVED را واقعاً از
// فهرست حذف می‌کند، و چیپ «همه» دوباره هر دو را برمی‌گرداند.

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProjectsStateFilterTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dataStoreFileName: String
    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var database: AppDatabase
    private lateinit var projectListViewModel: ProjectListViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreFileName = "projects_state_filter_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(dataStoreFileName) }
        )
        workflowViewModel = WorkflowViewModel(
            application = context.applicationContext as Application,
            dataStore = dataStore,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        // بدون idProvider ثابت — این تست به دو شناسه‌ی واقعاً متفاوت نیاز دارد
        // (هر پروژه با یک وضعیت متفاوت).
        projectListViewModel = ProjectListViewModel(
            application = context.applicationContext as Application,
            repository = ProjectRepository(database.projectDao())
        )

        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel
                )
            }
        }
    }

    @After
    fun tearDown() {
        composeRule.waitForIdle()
        context.preferencesDataStoreFile(dataStoreFileName).delete()
    }

    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction =
        performSemanticsAction(SemanticsActions.OnClick)

    private fun createProjectFromHome(name: String) {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput(name)
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilExactlyOneExists(hasTestTag(STUDIO_BACK_BUTTON_TAG), timeoutMillis = 10_000)
        composeRule.onNodeWithTag(STUDIO_BACK_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("home.greetingTitle", Language.FA)), timeoutMillis = 10_000)
    }

    @Test
    fun `selecting the DRAFT state filter chip hides an ARCHIVED project, and the All chip brings it back`() {
        createProjectFromHome("Alpha Draft Project")
        createProjectFromHome("Beta Archived Project")

        val archivedProject = runBlocking {
            database.projectDao().getAllProjects().first().first { it.projectName == "Beta Archived Project" }
        }
        runBlocking {
            database.projectDao().saveProject(archivedProject.copy(state = EntityState.ARCHIVED.name))
        }

        composeRule.onNodeWithTag(BOTTOM_NAV_PROJECTS_TAG).clickViaSemantics()
        // پروژه‌ها به‌ترتیب lastModified DESC مرتب‌اند — Beta (تازه‌تر) اول
        // (بالای LazyColumn) قرار می‌گیرد، پس بدون Scroll هم قابل‌مشاهده است.
        composeRule.waitUntilAtLeastOneExists(hasText("Beta Archived Project"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Beta Archived Project").assertIsDisplayed()

        composeRule.onNodeWithTag(projectsStateFilterTag(EntityState.DRAFT)).clickViaSemantics()
        composeRule.waitUntilDoesNotExist(hasText("Beta Archived Project"), timeoutMillis = 10_000)
        // با فیلتر DRAFT، «Alpha» تنها آیتم فهرست است — پس همیشه بالای LazyColumn
        // و قابل‌مشاهده است، بدون نیاز به Scroll.
        composeRule.waitUntilAtLeastOneExists(hasText("Alpha Draft Project"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Alpha Draft Project").assertIsDisplayed()
        composeRule.onNodeWithText("Beta Archived Project").assertDoesNotExist()

        composeRule.onNodeWithTag(projectsStateFilterTag(null)).clickViaSemantics()
        composeRule.waitUntilAtLeastOneExists(hasText("Beta Archived Project"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Beta Archived Project").performScrollTo().assertIsDisplayed()
    }
}
