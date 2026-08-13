package com.operaboys.cinemashotgenerator.ui.navigation

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
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.home.CREATE_PROJECT_NAME_FIELD_TAG
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.studio.STUDIO_BACK_BUTTON_TAG
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

// یافته‌ی ۱ appendix ADR-081 (ADR-082): FAB مرکزی سراسری «ایجاد سریع» قبلاً
// onQuickCreate={} خالی بود — کاملاً بی‌اثر روی هر صفحه. تست End-to-End واقعی
// اثبات می‌کند: (۱) کلیک روی FAB از یک صفحه‌ی غیر-Home (Projects) دیالوگ ایجاد
// پروژه‌ی موجود را باز می‌کند؛ (۲) تأیید دیالوگ واقعاً یک پروژه‌ی تازه می‌سازد و
// وارد Studio می‌شود — نه فقط کامپایل می‌شود.

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuickCreateFabTest {

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
        dataStoreFileName = "quick_create_fab_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
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

    @Test
    fun `tapping the global quick-create FAB from the Projects screen opens the create-project dialog and actually creates a project`() {
        composeRule.onNodeWithTag(BOTTOM_NAV_PROJECTS_TAG).clickViaSemantics()
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("projects.title", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(BOTTOM_NAV_QUICK_CREATE_FAB_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(CREATE_PROJECT_NAME_FIELD_TAG), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput("FAB Quick Create Test")
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()

        composeRule.waitUntilExactlyOneExists(hasTestTag(STUDIO_BACK_BUTTON_TAG), timeoutMillis = 10_000)
        composeRule.onNodeWithTag(STUDIO_BACK_BUTTON_TAG).assertIsDisplayed()
    }
}
