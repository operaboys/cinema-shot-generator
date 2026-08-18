package com.operaboys.cinemashotgenerator.ui.scenes

import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyDescendant
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
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.Environment
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.asset.Outfit
import com.operaboys.cinemashotgenerator.domain.asset.PhysicalAppearance
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode
import com.operaboys.cinemashotgenerator.ui.dna.cinematicModeLabel
import com.operaboys.cinemashotgenerator.ui.dna.visualStyleLabel
import com.operaboys.cinemashotgenerator.ui.story.moodLabel
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
/** یافته‌ی #۱۱ appendix ADR-081 (ADR-085) — برای تست اتصال واقعی Asset↔Scene. */
private const val SEEDED_CHARACTER_ID = "char_scenes_flow_test"
private const val SEEDED_OBJECT_ID = "obj_scenes_flow_test"

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
    private lateinit var shotRepository: ShotRepository

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
        shotRepository = ShotRepository(database.shotDao())
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
            // یافته‌ی #۱۱ appendix ADR-081 (ADR-085) — برای تست‌های اتصال Asset↔Scene.
            assetRepository.saveCharacterAsset(
                PROJECT_ID,
                CharacterAsset(
                    assetId = SEEDED_CHARACTER_ID,
                    characterTier = CharacterTier.MAIN,
                    name = "Elias",
                    physicalAppearance = PhysicalAppearance(ageRange = "30s", gender = Gender.MALE),
                    outfits = listOf(Outfit(id = "outfit_default", name = "Default", description = "Default outfit", isDefault = true))
                )
            )
            assetRepository.saveObjectAsset(
                PROJECT_ID,
                ObjectAsset(
                    assetId = SEEDED_OBJECT_ID,
                    name = "Signal Receiver",
                    description = "an old radio device",
                    subtype = ObjectSubtype.PERSONAL_PROP,
                    size = "small",
                    materialAndColor = "metal, silver"
                )
            )
        }

        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    sceneRepository = SceneRepository(database.sceneDao()),
                    assetRepository = assetRepository,
                    shotRepository = shotRepository
                )
            }
        }
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        // database.close() عمداً حذف شد — ریشه‌ی واقعی Flake تاریخی این Suite
        // (docs/adr/070-flake-root-cause-investigation.md، رفع در ADR-071):
        // Room.inMemoryDatabaseBuilder نیازی به Close صریح ندارد (بدون فایل روی
        // دیسک، GC آن را با نابودی نمونه‌ی این کلاس تست جمع می‌کند)؛ این خط قبلاً
        // با Coroutine های ناتمام viewModelScope روی Executor داخلی Room مسابقه
        // می‌داد — نه یک نشتی حافظه‌ی فراموش‌شده.
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

    /**
     * رفع G8 ممیزی post-Unit16 (docs/adr/076-...) + رفع باگ ترجمه (docs/adr/077-...):
     * globalVisualStyle قبلاً فقط نمایشی بود (همیشه «از DNA پروژه»، هیچ مسیر
     * ویرایش). این تست ثابت می‌کند Dropdown تازه‌ی SceneSettingsDialog واقعاً
     * override را ذخیره می‌کند و OverviewTab آن را با `visualStyleLabel`
     * ترجمه‌شده نشان می‌دهد — نه نام خام Enum (که یک باگ واقعی بود، در همان
     * قدم اول این ویژگی وارد شد، در ADR-077 رفع شد).
     */
    @Test
    fun `choosing a global visual style override from SceneSettingsDialog saves it and shows the translated label in Overview`() {
        createProjectAndOpenScenesTab("Scenes Visual Style Test")

        composeRule.onNodeWithTag(SCENES_LIST_NEW_SCENE_FAB_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.tab.overview", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithText(uiString("sceneDetail.overview.globalVisualStyleFromDna", Language.FA)).assertExists()

        composeRule.onNodeWithTag(SCENE_DETAIL_EDIT_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.settingsDialogTitle", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithTag("sceneDetail.settings.globalVisualStyleField").clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(visualStyleLabel(VisualStyle.FILM_NOIR, Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithText(visualStyleLabel(VisualStyle.FILM_NOIR, Language.FA)).performClick()

        composeRule.onNodeWithTag(SCENE_DETAIL_SETTINGS_SAVE_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(visualStyleLabel(VisualStyle.FILM_NOIR, Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithText("FILM_NOIR").assertDoesNotExist()
    }

    /**
     * تکمیل Rule یتیم — قدم ۴ از ۴ (ADR-112): هم‌الگو دقیق با تست بالا برای
     * globalVisualStyleOverride — اثبات می‌کند Dropdown تازه‌ی cinematicModeOverride
     * واقعاً مقدار انتخاب‌شده را ذخیره می‌کند (نه یک مقدار جای‌گیر یا رشته‌ی خام)
     * و OverviewTab آن را با برچسب ترجمه‌شده نشان می‌دهد.
     */
    @Test
    fun `choosing a cinematic mode override from SceneSettingsDialog saves it and shows the translated label in Overview`() {
        createProjectAndOpenScenesTab("Scenes Cinematic Mode Test")

        composeRule.onNodeWithTag(SCENES_LIST_NEW_SCENE_FAB_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.tab.overview", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithText(uiString("sceneDetail.overview.cinematicModeFromProject", Language.FA)).assertExists()

        composeRule.onNodeWithTag(SCENE_DETAIL_EDIT_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.settingsDialogTitle", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithTag("sceneDetail.settings.cinematicModeField").clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(cinematicModeLabel(CinematicMode.FAST_CUT, Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithText(cinematicModeLabel(CinematicMode.FAST_CUT, Language.FA)).performClick()

        composeRule.onNodeWithTag(SCENE_DETAIL_SETTINGS_SAVE_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(cinematicModeLabel(CinematicMode.FAST_CUT, Language.FA)), timeoutMillis = 5_000)
    }

    /** هم‌الگو دقیق با تست بالا — برای فیلد `mood` تازه (ADR-109). */
    @Test
    fun `choosing a mood from SceneSettingsDialog saves it and shows the translated label in Overview`() {
        createProjectAndOpenScenesTab("Scenes Mood Test")

        composeRule.onNodeWithTag(SCENES_LIST_NEW_SCENE_FAB_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.tab.overview", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithText(uiString("sceneDetail.overview.moodUnset", Language.FA)).assertExists()

        composeRule.onNodeWithTag(SCENE_DETAIL_EDIT_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.settingsDialogTitle", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithTag("sceneDetail.settings.moodField").clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(moodLabel(Mood.MYSTERIOUS, Language.FA)), timeoutMillis = 5_000)
        // یافته‌ی واقعی این تست (هم‌خانواده با یافته‌ی مستندشده‌ی بالای این فایل برای FAB):
        // performClick() مبتنی بر مختصات، وقتی آیتم هدف (Mood.MYSTERIOUS، در جایگاه ۱۷ از
        // ۲۵ مقدار) پایین‌تر از ناحیه‌ی قابل‌مشاهده‌ی اولیه‌ی این Popup غیر-Lazy طولانی قرار
        // دارد، هیچ کلیکی Trigger نمی‌کند (گره در Semantics Tree پیدا می‌شود، اما لامبدای
        // onClick هرگز اجرا نمی‌شود) — رفع با همان راه‌حل تثبیت‌شده‌ی پروژه:
        // performSemanticsAction(SemanticsActions.OnClick) به‌جای performClick().
        composeRule.onNodeWithText(moodLabel(Mood.MYSTERIOUS, Language.FA)).clickViaSemantics()

        composeRule.onNodeWithTag(SCENE_DETAIL_SETTINGS_SAVE_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(moodLabel(Mood.MYSTERIOUS, Language.FA)), timeoutMillis = 5_000)
    }

    /**
     * یافته‌ی ۱ appendix ADR-081 (ADR-083): Badge عددی Tab «شات‌ها» طبق mockup
     * (`sceneTabs`، `t.badge`) — قبلاً هیچ شمارشی روی این Tab نبود. اثبات
     * می‌کند Badge با یک StateFlow زنده (نه یک‌بار خواندن) به تعداد واقعی
     * Shot های همان صحنه وصل است.
     */
    @Test
    fun `Shots tab badge shows the real live shot count for the scene, starting at 0 and updating after a shot is saved`() {
        createProjectAndOpenScenesTab("Scenes Shots Badge Test")

        composeRule.onNodeWithTag(SCENES_LIST_NEW_SCENE_FAB_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.tab.overview", Language.FA)), timeoutMillis = 5_000)

        // یافته‌ی واقعی دیباگ: Tab یک Composable قابل‌کلیک است — Compose به‌طور
        // پیش‌فرض متن فرزندان چنین گره‌ای را در Semantics Tree ادغام می‌کند
        // (برای Accessibility)، پس بدون useUnmergedTree=true گره‌ی متن Badge به‌
        // تنهایی در درخت ادغام‌شده پیدا نمی‌شود.
        composeRule.onNode(hasTestTag(SCENE_DETAIL_SHOTS_TAB_TAG) and hasAnyDescendant(hasText("0")), useUnmergedTree = true).assertExists()

        val sceneId = runBlocking { database.sceneDao().getScenesForProject(PROJECT_ID).first().first().sceneId }
        runBlocking {
            shotRepository.saveShot(
                Shot(
                    shotId = "shot_badge_test",
                    sceneId = sceneId,
                    shotNumber = 1,
                    shotTitle = "Badge Test Shot",
                    shotDescription = "proves the live badge count",
                    shotGoal = ShotGoal.ESTABLISHING,
                    shotType = ShotType.WIDE,
                    durationSeconds = 4f,
                    motionLevel = MotionLevel.STATIC,
                    soundProfile = SoundProfile(enabled = false)
                )
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag(SCENE_DETAIL_SHOTS_TAB_TAG) and hasAnyDescendant(hasText("1")), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasTestTag(SCENE_DETAIL_SHOTS_TAB_TAG) and hasAnyDescendant(hasText("1")), useUnmergedTree = true).assertExists()
    }

    /**
     * یافته‌ی #۱۱ appendix ADR-081 (ADR-085) — تست End-to-End واقعی مسیر کامل:
     * افزودن یک Asset واقعی (Character) به یک Scene از دیالوگ انتخاب، دیدن آن
     * در فهرست کارت‌های متصل، و به‌روزرسانی زنده‌ی Badge شمارشی Tab «دارایی‌ها».
     */
    @Test
    fun `adding a character asset from the picker links it, shows it as a card, and updates the assets badge`() {
        createProjectAndOpenScenesTab("Scenes Asset Link Test")

        composeRule.onNodeWithTag(SCENES_LIST_NEW_SCENE_FAB_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.tab.overview", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(SCENE_DETAIL_ASSETS_TAB_TAG).clickViaSemantics()
        composeRule.onNode(hasTestTag(SCENE_DETAIL_ASSETS_TAB_TAG) and hasAnyDescendant(hasText("0")), useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(uiString("sceneDetail.assetsEmptyState", Language.FA)).assertExists()

        composeRule.onNodeWithTag(SCENE_DETAIL_ADD_ASSET_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.assets.pickerTitle", Language.FA)), timeoutMillis = 5_000)
        // هر سه نوع (کاراکتر/مکان/شیء) باید در دیالوگ قابل‌انتخاب باشند — نه فقط Location.
        composeRule.onNodeWithTag(sceneDetailAssetPickerItemTag(SEEDED_CHARACTER_ID)).assertExists()
        composeRule.onNodeWithTag(sceneDetailAssetPickerItemTag(SEEDED_LOCATION_ID)).assertExists()
        composeRule.onNodeWithTag(sceneDetailAssetPickerItemTag(SEEDED_OBJECT_ID)).assertExists()

        composeRule.onNodeWithTag(sceneDetailAssetPickerItemTag(SEEDED_CHARACTER_ID)).clickViaSemantics()

        composeRule.waitUntilExactlyOneExists(hasTestTag(sceneDetailLinkedAssetCardTag(SEEDED_CHARACTER_ID)), timeoutMillis = 5_000)
        composeRule.onNodeWithText("Elias").assertExists()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag(SCENE_DETAIL_ASSETS_TAB_TAG) and hasAnyDescendant(hasText("1")), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // حذف اتصال — نیاز عملکردی بدیهی (mockup صریح نشانش نداده، اما بدون آن
        // اتصال اشتباه هرگز قابل‌رفع نیست).
        composeRule.onNodeWithTag(sceneDetailUnlinkAssetButtonTag(SEEDED_CHARACTER_ID)).clickViaSemantics()
        composeRule.waitUntilDoesNotExist(hasTestTag(sceneDetailLinkedAssetCardTag(SEEDED_CHARACTER_ID)), timeoutMillis = 5_000)
        composeRule.onNodeWithText(uiString("sceneDetail.assetsEmptyState", Language.FA)).assertExists()
        composeRule.onNode(hasTestTag(SCENE_DETAIL_ASSETS_TAB_TAG) and hasAnyDescendant(hasText("0")), useUnmergedTree = true).assertExists()
    }
}
