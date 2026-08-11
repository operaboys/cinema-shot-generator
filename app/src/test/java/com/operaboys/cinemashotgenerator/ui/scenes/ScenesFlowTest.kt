package com.operaboys.cinemashotgenerator.ui.scenes

import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
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
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.domain.asset.Environment
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.home.CREATE_PROJECT_NAME_FIELD_TAG
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.navigation.StudioTab
import com.operaboys.cinemashotgenerator.ui.navigation.studioTabTestTag
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
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

// واحد ۱۶ فاز ۴ — قدم ۱: تست‌های End-to-End واقعی لیست صحنه‌ها + Scene Detail —
// طبق دستور کار: (۱) Navigation از لیست به Detail و برگشت؛ (۲) اتصال یک
// LocationAsset از کتابخانه → ذخیره‌ی locationAssetId → نمایش صحیح در Overview.
// الگوی راه‌اندازی (ساخت پروژه‌ی واقعی از Home) عیناً از DnaTabFlowTest.kt گرفته
// شده.
//
// یافته‌ی واقعی تست (هم‌خانواده با یافته‌ی مستندشده در ADR-045/047 برای
// FilterChip/DropdownMenuItem/Card): performClick() (لمس مبتنی بر مختصات) روی
// FloatingActionButton این صفحه (که داخل یک Box با `.align(Alignment.BottomEnd)`
// قرار دارد) هیچ کلیکی Trigger نمی‌کند — تأییدشده با دیباگ مستقیم (گره پیدا می‌شود،
// hasClickAction هم true است، اما لامبدای onClick هرگز اجرا نمی‌شود). رفع با همان
// راه‌حل تثبیت‌شده‌ی پروژه: performSemanticsAction(SemanticsActions.OnClick) به‌جای
// performClick() مبتنی بر مختصات. جزئیات کامل در
// docs/adr/050-unit16-phase4-step1-scene-detail.md.

private const val PROJECT_ID = "proj_scenes_flow_test"
private const val SEEDED_LOCATION_ID = "loc_scenes_flow_test"

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScenesFlowTest {

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
        dataStoreFileName = "scenes_flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
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

        val assetRepository = AssetRepository(database.assetDao())
        runBlocking {
            assetRepository.saveLocationAsset(
                PROJECT_ID,
                LocationAsset(
                    assetId = SEEDED_LOCATION_ID,
                    name = "Bridge of the Ship",
                    description = "the ship's command bridge",
                    environment = Environment(type = "indoor", size = "medium", lightingCondition = "bright")
                )
            )
        }

        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    sceneRepository = SceneRepository(database.sceneDao()),
                    assetRepository = assetRepository
                )
            }
        }
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        database.close()
    }

    /** طبق یافته‌ی مستندشده‌ی بالای فایل — performClick() روی این FAB غیرقابل‌اعتماد است. */
    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction =
        performSemanticsAction(SemanticsActions.OnClick)

    private fun createProjectAndOpenScenesTab(name: String) {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput(name)
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(name), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.SCENES)).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("scenesList.emptyState", Language.FA)), timeoutMillis = 5_000)
    }

    @Test
    fun `creating a new scene from the list navigates to Scene Detail and back to the list shows the new scene`() {
        createProjectAndOpenScenesTab("Scenes Nav Test")

        val firstSceneTitle = uiTemplate("scene.defaultTitleTemplate", Language.FA, "number" to "1")

        composeRule.onNodeWithTag(SCENES_LIST_NEW_SCENE_FAB_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.tab.overview", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithText(firstSceneTitle).assertExists()

        // بازگشت Studio(projectId) یک نمونه‌ی تازه‌ی StudioShell می‌سازد — انتخاب Tab
        // محلی (rememberSaveable) به پیش‌فرض STORY بازمی‌گردد (همان محدودیت
        // پذیرفته‌شده‌ی مستندشده در ADR-049 برای فیلتر صفحه‌ی Assets)، پس باید دوباره
        // روی Tab «صحنه‌ها» لمس شود تا لیست دیده شود.
        composeRule.onNodeWithTag(SCENE_DETAIL_BACK_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(studioTabTestTag(StudioTab.SCENES)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.SCENES)).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(firstSceneTitle), timeoutMillis = 5_000)
    }

    @Test
    fun `connecting a location asset from the library saves it and shows it in the Overview`() {
        createProjectAndOpenScenesTab("Scenes Location Test")

        composeRule.onNodeWithTag(SCENES_LIST_NEW_SCENE_FAB_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.tab.overview", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithText(uiString("sceneDetail.overview.noLocationConnected", Language.FA)).assertExists()

        composeRule.onNodeWithTag(SCENE_DETAIL_CONNECT_LOCATION_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.locationPicker.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(sceneDetailLocationPickerItemTag(SEEDED_LOCATION_ID)).clickViaSemantics()

        composeRule.waitUntilExactlyOneExists(hasText("Bridge of the Ship"), timeoutMillis = 5_000)
    }

    /**
     * رفع G8 ممیزی post-Unit16 (اولویت ۳ بند ۸): location قبلاً فقط از Quick Action
     * جدا («اتصال به کتابخانه»، Overview) قابل تغییر بود، نه از SceneSettingsDialog.
     * این تست ثابت می‌کند همان LocationPickerDialog از داخل Settings هم واقعاً
     * ذخیره می‌کند و بعد از بستن Dialog در Overview دیده می‌شود (ذخیره و بازیابی صحیح).
     */
    @Test
    fun `changing location from inside SceneSettingsDialog saves it and shows it in the Overview`() {
        createProjectAndOpenScenesTab("Scenes Settings Location Test")

        composeRule.onNodeWithTag(SCENES_LIST_NEW_SCENE_FAB_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.tab.overview", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(SCENE_DETAIL_EDIT_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.settingsDialogTitle", Language.FA)), timeoutMillis = 5_000)
        // هم پشت Dialog (Overview) و هم داخل خودِ SceneSettingsDialog همین متن
        // «هنوز به کتابخانه وصل نشده» را نشان می‌دهند (طبق رفع G8) — پس اینجا
        // assertExists با «دقیقاً یک گره» بی‌معنا است؛ فقط جریان اصلی زیر تست
        // می‌شود (کلیک دکمه‌ی تغییر Location داخل Settings → انتخاب → ذخیره).

        composeRule.onNodeWithTag(SCENE_DETAIL_SETTINGS_CHANGE_LOCATION_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.locationPicker.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(sceneDetailLocationPickerItemTag(SEEDED_LOCATION_ID)).clickViaSemantics()

        composeRule.waitUntilExactlyOneExists(hasText("Bridge of the Ship"), timeoutMillis = 5_000)
    }
}
