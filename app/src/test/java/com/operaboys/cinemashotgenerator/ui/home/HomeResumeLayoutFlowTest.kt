package com.operaboys.cinemashotgenerator.ui.home

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.workflow.HomeLayoutVariant
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.navigation.BOTTOM_NAV_HOME_TAG
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.navigation.StudioTab
import com.operaboys.cinemashotgenerator.ui.navigation.studioTabTestTag
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

// رفع G12 باقی‌مانده — homeLayoutVariant.RESUME (دستور کار ۲۰۲۶-۰۸-۱۳،
// docs/adr/081-...، طبق mockup docs/design/Cinema Studio.html بخش homeB). تست
// End-to-End واقعی اثبات می‌کند: (۱) وقتی این Variant فعال است، کارت «ادامه» و
// دکمه‌ی آن به‌جای چیدمان Hero رندر می‌شود؛ (۲) دکمه‌ی «ادامه» واقعاً پروژه‌ی
// اخیر را باز می‌کند؛ (۳) کاشی «DNA» واقعاً Studio را روی Tab DNA باز می‌کند.

private const val PROJECT_ID = "proj_resume_flow_test"

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeResumeLayoutFlowTest {

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
        dataStoreFileName = "home_resume_flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(dataStoreFileName) }
        )
        workflowViewModel = WorkflowViewModel(
            application = context.applicationContext as Application,
            dataStore = dataStore,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        projectListViewModel = ProjectListViewModel(
            application = context.applicationContext as Application,
            repository = ProjectRepository(database.projectDao(), idProvider = { PROJECT_ID })
        )

        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    storyRepository = StoryRepository(database.storyDao(), database.storyBreakdownSessionDao()),
                    projectDnaRepository = ProjectDnaRepository(database.projectDnaDao()),
                    sceneRepository = SceneRepository(database.sceneDao())
                )
            }
        }
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(dataStoreFileName).delete()
    }

    private fun createProjectAndReturnHome() {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput("Resume Test Project")
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("studioTab.story", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(BOTTOM_NAV_HOME_TAG).performClick()
    }

    @Test
    fun `RESUME layout shows the resume card instead of the HERO greeting for the most recent project`() {
        createProjectAndReturnHome()
        workflowViewModel.setHomeLayoutVariant(HomeLayoutVariant.RESUME)
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("home.resume.caption", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(HOME_RESUME_CARD_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Resume Test Project").assertIsDisplayed()
    }

    @Test
    fun `RESUME resume button opens the most recent project into Studio`() {
        createProjectAndReturnHome()
        workflowViewModel.setHomeLayoutVariant(HomeLayoutVariant.RESUME)
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("home.resume.caption", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(HOME_RESUME_BUTTON_TAG).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("studioTab.story", Language.FA)), timeoutMillis = 5_000)
    }

    @Test
    fun `RESUME DNA quick tile opens Studio directly on the DNA tab`() {
        createProjectAndReturnHome()
        workflowViewModel.setHomeLayoutVariant(HomeLayoutVariant.RESUME)
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("home.resume.caption", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(HOME_RESUME_TILE_DNA_TAG).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("dna.group.coreIdentity", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.DNA)).assertIsDisplayed()
    }
}
