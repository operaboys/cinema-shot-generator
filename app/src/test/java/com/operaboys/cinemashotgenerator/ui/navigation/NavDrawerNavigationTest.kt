package com.operaboys.cinemashotgenerator.ui.navigation

import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsSelected
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
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.SceneLocation
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.ui.home.CREATE_PROJECT_NAME_FIELD_TAG
import com.operaboys.cinemashotgenerator.ui.home.HOME_OPEN_DRAWER_BUTTON_TAG
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.outputdelivery.OUTPUT_DELIVERY_BACK_BUTTON_TAG
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.studio.STUDIO_BACK_BUTTON_TAG
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import com.operaboys.cinemashotgenerator.ui.validation.VALIDATION_BACK_BUTTON_TAG
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

// رفع G1 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، docs/adr/061-...):
// تست‌های End-to-End واقعی برای ۷ لینک Nav Drawer که قبلاً همه COMING_SOON بودند
// اما مسیر واقعی‌شان از قبل موجود بود. الگوی راه‌اندازی عیناً از BackupsFlowTest.kt
// گرفته شده (تنها راه واقعی فعال‌کردن WorkflowState.projectId، ورود واقعی به
// Studio با ساخت یک پروژه‌ی واقعی است).
//
// رفع یافته‌ی #۱۰ appendix (ADR-088): دو تست تازه‌ی انتهای فایل (پرش مستقیم با
// دقیقاً یک Shot، Fallback حفظ‌شده با بیش‌از‌یک Shot) — الگوی Seed مستقیم
// Scene/Shot از ValidationFlowTest.kt گرفته شده. حالت «صفر Shot» را همان دو تست
// موجود بالا («no shot context available») از قبل پوشش می‌دهند — چون پروژه‌ی
// تازه‌ساز آن‌ها هیچ Shot ای ندارد.

private const val PROJECT_ID = "proj_nav_drawer_test"
private const val SCENE_ID = "scene_nav_drawer_test"
private const val SHOT_ID_1 = "shot_nav_drawer_test_1"
private const val SHOT_ID_2 = "shot_nav_drawer_test_2"

private val seededScene = Scene(
    sceneId = SCENE_ID,
    sceneTitle = "Nav Drawer Scene",
    sceneNumber = 1,
    narrativeRole = NarrativeRole.DEVELOPMENT,
    location = SceneLocation(type = LocationType.MIXED, description = "a quiet street"),
    timeOfDay = TimeOfDay.NIGHT,
    atmospherePrimary = Atmosphere.CALM,
    shotCount = 1
)

private fun buildShot(shotId: String, shotNumber: Int) = Shot(
    shotId = shotId,
    sceneId = SCENE_ID,
    shotNumber = shotNumber,
    shotTitle = "Nav Drawer Shot $shotNumber",
    shotDescription = "A shot seeded for the Nav Drawer single-shot jump test.",
    shotGoal = ShotGoal.ESTABLISHING,
    shotType = ShotType.WIDE,
    durationSeconds = 6f,
    motionLevel = MotionLevel.STATIC,
    soundProfile = SoundProfile(enabled = false)
)

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavDrawerNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dataStoreFileName: String
    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var database: AppDatabase
    private lateinit var projectListViewModel: ProjectListViewModel
    private lateinit var sceneRepository: SceneRepository
    private lateinit var shotRepository: ShotRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreFileName = "nav_drawer_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
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
        sceneRepository = SceneRepository(database.sceneDao())
        shotRepository = ShotRepository(database.shotDao())
    }

    @After
    fun tearDown() {
        composeRule.waitForIdle()
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        // database.close() عمداً حذف شد — ریشه‌ی واقعی Flake تاریخی این Suite
        // (docs/adr/070-flake-root-cause-investigation.md، رفع در ADR-071):
        // Room.inMemoryDatabaseBuilder نیازی به Close صریح ندارد (بدون فایل روی
        // دیسک، GC آن را با نابودی نمونه‌ی این کلاس تست جمع می‌کند)؛ این خط قبلاً
        // با Coroutine های ناتمام viewModelScope روی Executor داخلی Room مسابقه
        // می‌داد — نه یک نشتی حافظه‌ی فراموش‌شده. waitForIdle() بالا همچنان برای
        // Idling خودِ Compose مفید است، فقط دیگر ایمنی close() را تضمین نمی‌کند.
    }

    private fun setContent() {
        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    storyRepository = StoryRepository(database.storyDao(), database.storyBreakdownSessionDao()),
                    sceneRepository = sceneRepository,
                    shotRepository = shotRepository,
                    database = database
                )
            }
        }
    }

    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction =
        performSemanticsAction(SemanticsActions.OnClick)

    /** پروژه‌ی واقعی می‌سازد و وارد Studio می‌شود (تنها راه واقعی فعال‌شدن WorkflowState.projectId). */
    private fun createProjectAndEnterStudio(name: String = "Nav Drawer Test") {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput(name)
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(name), timeoutMillis = 15_000)
        composeRule.waitUntilExactlyOneExists(hasTestTag(STUDIO_BACK_BUTTON_TAG), timeoutMillis = 15_000)
    }

    private fun backToHomeAndOpenDrawer() {
        composeRule.onNodeWithTag(STUDIO_BACK_BUTTON_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasTestTag(HOME_OPEN_DRAWER_BUTTON_TAG), timeoutMillis = 15_000)
        composeRule.onNodeWithTag(HOME_OPEN_DRAWER_BUTTON_TAG).performClick()
        composeRule.waitForIdle()
    }

    private fun clickDrawerLink(labelKey: String) {
        composeRule.waitUntilExactlyOneExists(hasText(uiString(labelKey, Language.FA)), timeoutMillis = 10_000)
        composeRule.onNodeWithText(uiString(labelKey, Language.FA)).performScrollTo().clickViaSemantics()
    }

    @Test
    fun `clicking storyWizard in the drawer navigates to Studio with the Story tab selected`() {
        setContent()
        createProjectAndEnterStudio()
        backToHomeAndOpenDrawer()

        clickDrawerLink("drawer.storyWizard")

        composeRule.waitUntilExactlyOneExists(hasTestTag(studioTabTestTag(StudioTab.STORY)), timeoutMillis = 10_000)
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.STORY)).assertIsSelected()
    }

    @Test
    fun `clicking dnaManager in the drawer navigates to Studio with the DNA tab selected`() {
        setContent()
        createProjectAndEnterStudio()
        backToHomeAndOpenDrawer()

        clickDrawerLink("drawer.dnaManager")

        composeRule.waitUntilExactlyOneExists(hasTestTag(studioTabTestTag(StudioTab.DNA)), timeoutMillis = 10_000)
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.DNA)).assertIsSelected()
    }

    @Test
    fun `clicking scenes in the drawer navigates to Studio with the Scenes tab selected`() {
        setContent()
        createProjectAndEnterStudio()
        backToHomeAndOpenDrawer()

        clickDrawerLink("drawer.scenes")

        composeRule.waitUntilExactlyOneExists(hasTestTag(studioTabTestTag(StudioTab.SCENES)), timeoutMillis = 10_000)
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.SCENES)).assertIsSelected()
    }

    // «شات‌ها» Tab مستقل خودش را ندارد — مقصد Fallback مستند (docs/adr/061-...)
    // همان Tab «صحنه‌ها» است، جایی که کاربر خودش صحنه‌ی مقصد را انتخاب می‌کند.
    @Test
    fun `clicking shots in the drawer navigates to Studio with the Scenes tab selected (no dedicated Shots tab exists)`() {
        setContent()
        createProjectAndEnterStudio()
        backToHomeAndOpenDrawer()

        clickDrawerLink("drawer.shots")

        composeRule.waitUntilExactlyOneExists(hasTestTag(studioTabTestTag(StudioTab.SCENES)), timeoutMillis = 10_000)
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.SCENES)).assertIsSelected()
    }

    // Validation/OutputDelivery هر دو به یک shotId مشخص نیاز دارند که Drawer آن را
    // نمی‌داند — مقصد Fallback مستند همان Tab «صحنه‌ها» است.
    @Test
    fun `clicking validation in the drawer navigates to Studio with the Scenes tab selected (no shot context available)`() {
        setContent()
        createProjectAndEnterStudio()
        backToHomeAndOpenDrawer()

        clickDrawerLink("drawer.validation")

        composeRule.waitUntilExactlyOneExists(hasTestTag(studioTabTestTag(StudioTab.SCENES)), timeoutMillis = 10_000)
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.SCENES)).assertIsSelected()
    }

    @Test
    fun `clicking outputDelivery in the drawer navigates to Studio with the Scenes tab selected (no shot context available)`() {
        setContent()
        createProjectAndEnterStudio()
        backToHomeAndOpenDrawer()

        clickDrawerLink("drawer.outputDelivery")

        composeRule.waitUntilExactlyOneExists(hasTestTag(studioTabTestTag(StudioTab.SCENES)), timeoutMillis = 10_000)
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.SCENES)).assertIsSelected()
    }

    @Test
    fun `clicking aiBreakdown in the drawer navigates to the real AI Story Breakdown screen`() {
        setContent()
        createProjectAndEnterStudio()
        backToHomeAndOpenDrawer()

        clickDrawerLink("drawer.aiBreakdown")

        composeRule.waitUntilExactlyOneExists(hasText(uiString("aiBreakdown.storyLabel", Language.FA)), timeoutMillis = 10_000)
    }

    @Test
    fun `clicking a Studio-tab drawer link with no active project redirects to Projects instead of crashing`() {
        setContent()
        // بدون createProjectAndEnterStudio — هیچ WorkflowState.projectId ای فعال نیست.
        composeRule.waitUntilExactlyOneExists(hasTestTag(HOME_OPEN_DRAWER_BUTTON_TAG), timeoutMillis = 15_000)
        composeRule.onNodeWithTag(HOME_OPEN_DRAWER_BUTTON_TAG).performClick()
        composeRule.waitForIdle()

        clickDrawerLink("drawer.scenes")

        // «پروژه‌ها» (nav.projects) عیناً با عنوان صفحه‌ی Projects (projects.title)
        // یکسان است و نوار پایین همیشه در Composition حاضر است — پس hasText اینجا
        // Ambiguous می‌شود (هم‌الگو با یادداشت BottomNavBar.kt درباره‌ی Nav Drawer).
        // بررسی «انتخاب‌شدگی» آیتم نوار پایین Projects، اثبات دقیق‌تری از رسیدن به
        // مقصد واقعی است.
        composeRule.waitUntilExactlyOneExists(hasTestTag(BOTTOM_NAV_PROJECTS_TAG), timeoutMillis = 10_000)
        composeRule.onNodeWithTag(BOTTOM_NAV_PROJECTS_TAG).assertIsSelected()
    }

    @Test
    fun `clicking validation in the drawer jumps directly to the Validation screen when the project has exactly one Shot`() {
        setContent()
        createProjectAndEnterStudio()
        runBlocking {
            sceneRepository.saveScene(PROJECT_ID, seededScene)
            shotRepository.saveShot(buildShot(SHOT_ID_1, shotNumber = 1))
        }
        backToHomeAndOpenDrawer()

        clickDrawerLink("drawer.validation")

        composeRule.waitUntilExactlyOneExists(hasTestTag(VALIDATION_BACK_BUTTON_TAG), timeoutMillis = 10_000)
    }

    @Test
    fun `clicking outputDelivery in the drawer jumps directly to the Output Delivery screen when the project has exactly one Shot`() {
        setContent()
        createProjectAndEnterStudio()
        runBlocking {
            sceneRepository.saveScene(PROJECT_ID, seededScene)
            shotRepository.saveShot(buildShot(SHOT_ID_1, shotNumber = 1))
        }
        backToHomeAndOpenDrawer()

        clickDrawerLink("drawer.outputDelivery")

        composeRule.waitUntilExactlyOneExists(hasTestTag(OUTPUT_DELIVERY_BACK_BUTTON_TAG), timeoutMillis = 10_000)
    }

    @Test
    fun `clicking validation in the drawer preserves the Scenes tab fallback when the project has more than one Shot`() {
        setContent()
        createProjectAndEnterStudio()
        runBlocking {
            sceneRepository.saveScene(PROJECT_ID, seededScene)
            shotRepository.saveShot(buildShot(SHOT_ID_1, shotNumber = 1))
            shotRepository.saveShot(buildShot(SHOT_ID_2, shotNumber = 2))
        }
        backToHomeAndOpenDrawer()

        clickDrawerLink("drawer.validation")

        composeRule.waitUntilExactlyOneExists(hasTestTag(studioTabTestTag(StudioTab.SCENES)), timeoutMillis = 10_000)
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.SCENES)).assertIsSelected()
    }
}
