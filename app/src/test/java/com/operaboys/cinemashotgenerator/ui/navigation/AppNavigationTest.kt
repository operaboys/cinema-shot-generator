package com.operaboys.cinemashotgenerator.ui.navigation

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
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

// واحد ۱۶ — فاز ۰: تست‌های Navigation دو‌لایه (docs/adr/042-...md).
// اولین Compose UI Test این پروژه — با Robolectric (نه instrumented androidTest؛
// همان دلیل انتخاب Robolectric برای تست‌های Room واحد ۱۵: این محیط به
// امولاتور/دستگاه واقعی دسترسی ندارد). createComposeRule() (نه
// createAndroidComposeRule<...>()) استفاده شد چون نیازی به یک Activity واقعی
// میزبان نیست — MainScaffold مستقیماً با یک WorkflowViewModel تزریق‌شده Compose
// می‌شود.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dataStoreFileName: String
    private lateinit var workflowViewModel: WorkflowViewModel

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
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(dataStoreFileName).delete()
    }

    private fun setContentUnderTest() {
        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(workflowViewModel = workflowViewModel)
            }
        }
    }

    @Test
    fun `the app-level bottom nav is always visible on the Home root screen`() {
        setContentUnderTest()

        composeRule.onNodeWithText(uiString("nav.home", Language.FA)).assertIsDisplayed()
        composeRule.onNodeWithText(uiString("nav.projects", Language.FA)).assertIsDisplayed()
        composeRule.onNodeWithText(uiString("nav.studio", Language.FA)).assertIsDisplayed()
        composeRule.onNodeWithText(uiString("nav.assets", Language.FA)).assertIsDisplayed()
    }

    @Test
    fun `the in-project top tab row is hidden on Home and appears only after entering Studio`() {
        setContentUnderTest()

        composeRule.onNodeWithText(uiString("studioTab.story", Language.FA)).assertDoesNotExist()

        composeRule.onNodeWithText(uiString("nav.studio", Language.FA)).performClick()

        composeRule.onNodeWithText(uiString("studioTab.story", Language.FA)).assertIsDisplayed()
        composeRule.onNodeWithText(uiString("studioTab.dna", Language.FA)).assertIsDisplayed()
        composeRule.onNodeWithText(uiString("studioTab.scenes", Language.FA)).assertIsDisplayed()
        composeRule.onNodeWithText(uiString("studioTab.output", Language.FA)).assertIsDisplayed()
    }

    @Test
    fun `the bottom nav stays visible while inside Studio too (complementary layers, not alternatives)`() {
        setContentUnderTest()

        composeRule.onNodeWithText(uiString("nav.studio", Language.FA)).performClick()

        composeRule.onNodeWithText(uiString("nav.home", Language.FA)).assertIsDisplayed()
        composeRule.onNodeWithText(uiString("nav.studio", Language.FA)).assertIsDisplayed()
    }

    @Test
    fun `the top tab row disappears again after leaving Studio for another root screen`() {
        setContentUnderTest()

        composeRule.onNodeWithText(uiString("nav.studio", Language.FA)).performClick()
        composeRule.onNodeWithText(uiString("studioTab.story", Language.FA)).assertIsDisplayed()

        composeRule.onNodeWithText(uiString("nav.projects", Language.FA)).performClick()
        composeRule.onNodeWithText(uiString("studioTab.story", Language.FA)).assertDoesNotExist()
    }
}
