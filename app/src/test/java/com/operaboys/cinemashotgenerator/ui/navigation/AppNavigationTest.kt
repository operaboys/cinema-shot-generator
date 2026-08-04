package com.operaboys.cinemashotgenerator.ui.navigation

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
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

// واحد ۱۶ — فاز ۰/۱: تست‌های Navigation دو‌لایه (docs/adr/042-...md، docs/adr/044-...md).
// اولین Compose UI Test این پروژه — با Robolectric (نه instrumented androidTest؛
// همان دلیل انتخاب Robolectric برای تست‌های Room واحد ۱۵: این محیط به
// امولاتور/دستگاه واقعی دسترسی ندارد). createComposeRule() (نه
// createAndroidComposeRule<...>()) استفاده شد چون نیازی به یک Activity واقعی
// میزبان نیست — MainScaffold مستقیماً با یک WorkflowViewModel/ProjectListViewModel
// تزریق‌شده Compose می‌شود.
//
// MIGRATED (فاز ۱): onNodeWithText → onNodeWithTag برای آیتم‌های نوار پایین/Tab —
// یافته‌ی فاز ۱: NavDrawer همیشه در Composition حاضر است (حتی بسته)، پس برچسب‌های
// تکراری بین Drawer و نوار پایین/Tab (مثل «استودیو»، «دارایی‌ها»، «صحنه‌ها») با
// onNodeWithText به یک نتیجه‌ی Ambiguous می‌رسند — جزئیات کامل در BottomNavBar.kt.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppNavigationTest {

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
        dataStoreFileName = "nav_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
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
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        database.close()
    }

    private fun setContentUnderTest() {
        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(workflowViewModel = workflowViewModel, projectListViewModel = projectListViewModel)
            }
        }
    }

    @Test
    fun `the app-level bottom nav is always visible on the Home root screen`() {
        setContentUnderTest()

        composeRule.onNodeWithTag(BOTTOM_NAV_HOME_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(BOTTOM_NAV_PROJECTS_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(BOTTOM_NAV_STUDIO_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(BOTTOM_NAV_ASSETS_TAG).assertIsDisplayed()
    }

    @Test
    fun `the in-project top tab row is hidden on Home and appears only after entering Studio`() {
        setContentUnderTest()

        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.STORY)).assertDoesNotExist()

        composeRule.onNodeWithTag(BOTTOM_NAV_STUDIO_TAG).performClick()

        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.STORY)).assertIsDisplayed()
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.DNA)).assertIsDisplayed()
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.SCENES)).assertIsDisplayed()
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.OUTPUT)).assertIsDisplayed()
    }

    @Test
    fun `the bottom nav stays visible while inside Studio too (complementary layers, not alternatives)`() {
        setContentUnderTest()

        composeRule.onNodeWithTag(BOTTOM_NAV_STUDIO_TAG).performClick()

        composeRule.onNodeWithTag(BOTTOM_NAV_HOME_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(BOTTOM_NAV_STUDIO_TAG).assertIsDisplayed()
    }

    @Test
    fun `the top tab row disappears again after leaving Studio for another root screen`() {
        setContentUnderTest()

        composeRule.onNodeWithTag(BOTTOM_NAV_STUDIO_TAG).performClick()
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.STORY)).assertIsDisplayed()

        composeRule.onNodeWithTag(BOTTOM_NAV_PROJECTS_TAG).performClick()
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.STORY)).assertDoesNotExist()
    }
}
