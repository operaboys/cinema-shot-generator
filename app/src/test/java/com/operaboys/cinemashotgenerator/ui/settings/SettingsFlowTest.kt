package com.operaboys.cinemashotgenerator.ui.settings

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
import com.operaboys.cinemashotgenerator.data.repository.AutoSaveManager
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.workflow.HomeLayoutVariant
import com.operaboys.cinemashotgenerator.ui.home.HOME_OPEN_DRAWER_BUTTON_TAG
import com.operaboys.cinemashotgenerator.ui.home.CREATE_PROJECT_NAME_FIELD_TAG
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

// واحد ۱۶ فاز ۶ — قدم ۱: تست End-to-End واقعی صفحه‌ی Settings + Timer دوره‌ای
// Auto-Save. الگوی راه‌اندازی عیناً از AppNavigationTest.kt/ValidationFlowTest.kt
// گرفته شده. جزئیات کامل تصمیمات در
// docs/adr/058-unit16-phase6-step1-settings-autosave.md.
//
// یافته‌ی مستندشده‌ی این قدم (تکرار همان یافته‌ی ADR-055/ADR-057): عنوان صفحه‌ی
// Settings عمداً از کلید موجود drawer.settings بازاستفاده شده که همیشه (حتی
// وقتی Drawer بسته است) یک گره‌ی دیگر با همان متن در درخت Semantics دارد —
// انتظار روی testTag منحصربه‌فرد این صفحه (دکمه‌ی برگشت)، نه متن عنوان.

private const val PROJECT_ID = "proj_settings_flow_test"

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dataStoreFileName: String
    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var database: AppDatabase
    private lateinit var projectListViewModel: ProjectListViewModel
    private lateinit var autoSaveManager: AutoSaveManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreFileName = "settings_flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
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
        autoSaveManager = AutoSaveManager(database.projectDao())
    }

    @After
    fun tearDown() {
        composeRule.waitForIdle()
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        database.close()
    }

    private fun setContent() {
        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    autoSaveManager = autoSaveManager
                )
            }
        }
    }

    /**
     * هم‌الگو دقیق با `clickViaSemantics` در ValidationFlowTest.kt/ShotsFlowTest.kt:
     * فراخوانی مستقیم Action واقعی OnClick ثبت‌شده در Semantics، مستقل از
     * مختصات صفحه‌نمایش (بدون نیاز به این‌که گره کاملاً داخل Viewport فعلی
     * باشد). یافته‌ی این قدم: performScrollTo()+performClick() برای چیپ‌های
     * ردیف Auto-Save Cadence (سه چیپ کنار هم، عریض‌تر از بقیه‌ی ردیف‌های
     * این صفحه) قابل‌اعتماد نبود؛ این مسیر مستحکم‌تر برای همه‌ی کلیک‌های این
     * فایل استفاده شد.
     */
    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction =
        performSemanticsAction(SemanticsActions.OnClick)

    private fun openSettingsFromDrawer() {
        composeRule.onNodeWithTag(HOME_OPEN_DRAWER_BUTTON_TAG).performClick()
        composeRule.waitForIdle()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("drawer.settings", Language.FA)), timeoutMillis = 10_000)
        // یافته‌ی واقعی این قدم (هم‌الگو با یافته‌ی مشابه ADR-057 برای Output
        // Delivery): «تنظیمات» آخرین گروه از ۱۱ آیتم این Drawer است — بدون
        // performScrollTo صریح، کلیک روی گره‌ی خارج از Viewport فعلی بی‌اثر است.
        composeRule.onNodeWithText(uiString("drawer.settings", Language.FA)).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(SETTINGS_BACK_BUTTON_TAG), timeoutMillis = 10_000)
    }

    @Test
    fun `Settings is reachable from the Nav Drawer, replacing the previous coming-soon placeholder`() {
        setContent()
        openSettingsFromDrawer()

        composeRule.onNodeWithTag(SETTINGS_BACK_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `switching the language radio in Settings persists and reflects immediately across the app`() {
        setContent()
        openSettingsFromDrawer()

        composeRule.onNodeWithTag(SETTINGS_LANGUAGE_EN_CHIP_TAG).clickViaSemantics()
        composeRule.waitForIdle()

        assertEquals(Language.EN, workflowViewModel.language.value)

        composeRule.onNodeWithTag(SETTINGS_BACK_BUTTON_TAG).performClick()
        // بازتاب فوری در بقیه‌ی اپ: بعد از برگشت به Home، عنوان‌های واقعی انگلیسی‌اند.
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("home.newProjectTitle", Language.EN)), timeoutMillis = 10_000)
    }

    @Test
    fun `switching the theme radio in Settings updates WorkflowViewModel-theme immediately`() {
        setContent()
        openSettingsFromDrawer()

        composeRule.onNodeWithTag(SETTINGS_THEME_LIGHT_CHIP_TAG).clickViaSemantics()
        composeRule.waitForIdle()

        assertEquals(com.operaboys.cinemashotgenerator.domain.workflow.AppTheme.LIGHT, workflowViewModel.theme.value)
    }

    @Test
    fun `switching the Auto-Save cadence chip persists the real intervalSeconds value`() {
        setContent()
        openSettingsFromDrawer()

        composeRule.onNodeWithTag(settingsAutoSaveCadenceChipTag(60L)).clickViaSemantics()
        composeRule.waitForIdle()

        assertEquals(60L, workflowViewModel.autoSaveCadenceSeconds.value)
    }

    @Test
    fun `switching a Display toggle persists (no runtime effect elsewhere, documented limitation)`() {
        setContent()
        openSettingsFromDrawer()

        composeRule.onNodeWithTag(SETTINGS_REDUCED_MOTION_SWITCH_TAG).performClick()
        composeRule.waitForIdle()

        assertEquals(true, workflowViewModel.reducedMotionEnabled.value)
    }

    /**
     * طبق یافته‌ی صریح این قدم (grep روی HomeScreen.kt/ShotComposerScreen.kt):
     * هیچ‌کدام از این دو صفحه فعلاً به homeLayoutVariant/composerLayoutVariant
     * شاخه‌بندی نمی‌کنند — پس «اعمال واقعی روی Home/Composer» هنوز چیزی برای
     * Assert کردن در سطح UI ندارد؛ این تست فقط لایه‌ی واقعاً موجود را تأیید
     * می‌کند: انتخاب از Settings، همان WorkflowState/DataStore واقعی
     * (`homeLayoutVariant`) را که Home/Composer در آینده باید از آن بخوانند،
     * درست به‌روزرسانی و Persist می‌کند.
     */
    @Test
    fun `switching the Home layout variant chip persists the real shared WorkflowViewModel state`() {
        setContent()
        openSettingsFromDrawer()

        composeRule.onNodeWithTag(SETTINGS_HOME_LAYOUT_RESUME_CHIP_TAG).clickViaSemantics()
        composeRule.waitForIdle()

        assertEquals(HomeLayoutVariant.RESUME, workflowViewModel.homeLayoutVariant.value)
    }

    /**
     * تصمیم AutoSave (ADR-058): یک Timer دوره‌ای واقعی در StudioShell به
     * AutoSaveManager.touch وصل شد. این تست Cadence را به ۱ ثانیه (از همین
     * صفحه‌ی Settings) کاهش می‌دهد تا در زمان معقول تست، واقعاً یک Tick کامل
     * پیش از assert اتفاق بیفتد — اثبات واقعی اینکه Timer به‌جای صرفاً
     * تعریف‌شدن، واقعاً هر Cadence اجرا می‌شود و ProjectEntity.lastModified را
     * تازه نگه می‌دارد.
     */
    @Test
    fun `the periodic Auto-Save timer actually touches ProjectEntity-lastModified while a Studio session is active`() {
        setContent()
        // Cadence مستقیماً روی ViewModel به ۱ ثانیه تنظیم می‌شود (نه از طریق کلیک
        // یکی از سه Preset واقعی صفحه‌ی Settings — همه‌شان ≥۱۵ ثانیه‌اند و تست را
        // کند می‌کردند). خودِ مسیر «چیپ → ViewModel.setAutoSaveCadenceSeconds» در
        // تست بالا (`switching the Auto-Save cadence chip ...`) جداگانه پوشش داده
        // شده؛ این تست فقط اثر واقعی نهاییِ خودِ Timer را بررسی می‌کند.
        workflowViewModel.setAutoSaveCadenceSeconds(1L)

        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput("AutoSave Timer Test")
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("AutoSave Timer Test"), timeoutMillis = 15_000)

        val initialLastModified = runBlocking { database.projectDao().loadProject(PROJECT_ID) }?.lastModified
        assertTrue(initialLastModified != null)

        composeRule.waitUntil(timeoutMillis = 8_000) {
            runBlocking { database.projectDao().loadProject(PROJECT_ID)?.lastModified } != initialLastModified
        }
    }
}
