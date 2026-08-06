package com.operaboys.cinemashotgenerator.ui.shots

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
import androidx.compose.ui.test.performScrollTo
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
import com.operaboys.cinemashotgenerator.domain.camera.AdvancedMovementType
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
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.navigation.StudioTab
import com.operaboys.cinemashotgenerator.ui.navigation.studioTabTestTag
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.scenes.SCENE_DETAIL_SHOTS_TAB_TAG
import com.operaboys.cinemashotgenerator.ui.scenes.sceneDisplayTitle
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

// واحد ۱۶ فاز ۴ — قدم ۲: تست‌های End-to-End واقعی Shot List + اسکلت Shot Composer
// — طبق دستور کار: (۱) نمایش صحیح شات‌های یک صحنه + سوییچ Grid/Timeline؛ (۲) ساخت
// شات جدید با فیلدهای سطح‌بالا → ذخیره‌ی واقعی → بازگشت به لیست؛ (۳) Navigation
// بین Shot List و Shot Composer. الگوی راه‌اندازی عیناً از
// ui/scenes/ScenesFlowTest.kt گرفته شده (شامل یافته‌ی مستندشده‌ی همان فایل درباره‌ی
// performClick() غیرقابل‌اعتماد روی FAB/Card — رفع با clickViaSemantics).
//
// دو یافته‌ی واقعی دیباگ این فایل (هیچ‌کدام باگ کلیک نبود، برخلاف فرض اولیه):
// (۱) Scene/Shot را نمی‌توان در setUp() پیش از ساخت خود پروژه (که طبق الگوی
// ScenesFlowTest.kt، از طریق UI در بدنه‌ی تست ساخته می‌شود) Seed کرد. تلاش اول: یک
// ProjectEntity موقت پیش از saveScene/saveShot Seed شد تا ForeignKey واقعی
// SceneEntity→ProjectEntity.projectId (تأییدشده با grep روی SceneEntity.kt) نقض
// نشود — اما این خودش یک باگ تازه ایجاد کرد: ProjectDao.saveProject از
// OnConflictStrategy.REPLACE استفاده می‌کند، که در SQLite معادل DELETE+INSERT است؛
// ساخت پروژه‌ی واقعی (با همان PROJECT_ID) از طریق UI، ردیف موقت را حذف می‌کند و همین
// حذف، به‌خاطر onDelete=CASCADE روی همان ForeignKey، تمام Scene/Shot از‌پیش‌Seed‌شده را
// هم پاک می‌کرد. رفع نهایی: Scene/Shot را در بدنه‌ی تست، *پس از* ساخت واقعی پروژه از
// طریق UI Seed می‌کنیم. (۲) کمبود واقعی در خودِ کد Production: قبل از رفع، ورود به
// Scene Detail همیشه با «…» (حالت بارگذاری‌نشده) گیر می‌کرد — چون
// SceneDetailViewModel.factory شرط «sceneRepository != null && assetRepository !=
// null» داشت (با &&، نه مستقل)؛ چون این تست فقط sceneRepository/shotRepository را
// به MainScaffold می‌داد (نه assetRepository)، این شرط false می‌شد و کل ViewModel
// (از‌جمله sceneRepository) بی‌صدا به دیتابیس Production خالی برمی‌گشت. رفع شد در
// SceneDetailViewModel.kt (هر دو Repository حالا مستقل بررسی می‌شوند) + این تست هم
// اکنون assetRepository را صریحاً می‌دهد. جزئیات کامل در
// docs/adr/051-unit16-phase4-step2-shot-list-composer-skeleton.md.
//
// MIGRATED (فاز ۴ قدم ۳): ۲ تست End-to-End برای Tab «دوربین» اضافه شد. دو یافته‌ی
// واقعی تازه‌ی دیباگ: (۱) کلیک روی فیلد Dropdown انتخاب نوع Movement پیشرفته
// (`AssetFormEnumDropdownField`) وقتی پایین‌تر از ناحیه‌ی دیده‌شده‌ی
// `verticalScroll` قرار دارد، بدون `performScrollTo()` صریح باز نمی‌شود — برخلاف
// فیلدهای بالای همان صفحه (Angle/Distance/...) که این مشکل را نداشتند. (۲) تستی که
// چند سوییچ UI سریع پشت‌سرهم انجام می‌دهد (هرکدام یک Auto-Save ناهمگام مستقل صف
// می‌کند) گاهی با `IllegalStateException: connection pool has been closed` شکست
// می‌خورد چون `tearDown()`'s `database.close()` ممکن بود پیش از تکمیل واقعی آخرین
// Coroutine صف‌شده اجرا شود؛ رفع شد با `composeRule.waitForIdle()` در `tearDown()`.
// جزئیات کامل در docs/adr/052-unit16-phase4-step3-camera-tab.md.

private const val PROJECT_ID = "proj_shots_flow_test"
private const val SCENE_ID = "scene_shots_flow_test"
private const val SEEDED_SHOT_TITLE = "Opening shot"

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShotsFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dataStoreFileName: String
    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var database: AppDatabase
    private lateinit var projectListViewModel: ProjectListViewModel
    private lateinit var sceneRepository: SceneRepository
    private lateinit var shotRepository: ShotRepository

    private val seededScene = Scene(
        sceneId = SCENE_ID,
        sceneTitle = "Dawn Over the City",
        sceneNumber = 1,
        narrativeRole = NarrativeRole.INTRODUCTION,
        location = SceneLocation(type = LocationType.OUTDOOR, description = "city skyline"),
        timeOfDay = TimeOfDay.DAWN,
        atmospherePrimary = Atmosphere.CALM
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreFileName = "shots_flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
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
        val assetRepository = AssetRepository(database.assetDao())

        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    sceneRepository = sceneRepository,
                    shotRepository = shotRepository,
                    assetRepository = assetRepository
                )
            }
        }
    }

    @After
    fun tearDown() {
        // یافته‌ی واقعی این قدم (متناوب، تست‌های با چند سوییچ سریع UI پشت‌سرهم):
        // هر تغییر فیلد یک Auto-Save ناهمگام مستقل (ioScope.launch) صف می‌کند؛ بدون
        // این خط، database.close() ممکن است پیش از تکمیل واقعی آخرین Coroutine
        // صف‌شده روی Main Dispatcher اجرا شود (IllegalStateException: connection pool
        // closed). waitForIdle() تضمین می‌کند همه‌ی کارهای صف‌شده واقعاً اجرا شده‌اند.
        composeRule.waitForIdle()
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        database.close()
    }

    /** طبق یافته‌ی مستندشده‌ی ScenesFlowTest.kt — performClick() روی FAB/Card این خانواده از صفحات غیرقابل‌اعتماد است. */
    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction =
        performSemanticsAction(SemanticsActions.OnClick)

    /**
     * پروژه‌ی واقعی از Home می‌سازد (تا ردیف Project واقعاً و پایدار وجود داشته
     * باشد)، سپس Scene/Shot را زیر همان projectId واقعی Seed می‌کند — طبق یافته‌ی
     * بالای فایل، Seed کردن پیش از این مرحله (در setUp) به‌خاطر
     * OnConflictStrategy.REPLACE + ForeignKey CASCADE داده را پاک می‌کرد.
     */
    private fun createProjectAndOpenShotsTab() {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput("Shots Flow Test")
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Shots Flow Test"), timeoutMillis = 5_000)

        runBlocking {
            sceneRepository.saveScene(PROJECT_ID, seededScene)
            shotRepository.saveShot(
                Shot(
                    shotId = "shot_seed",
                    sceneId = SCENE_ID,
                    shotNumber = 1,
                    shotTitle = SEEDED_SHOT_TITLE,
                    shotDescription = "A wide establishing view of the city skyline at dawn.",
                    shotGoal = ShotGoal.ESTABLISHING,
                    shotType = ShotType.WIDE,
                    durationSeconds = 6f,
                    motionLevel = MotionLevel.STATIC,
                    soundProfile = SoundProfile(enabled = false)
                )
            )
        }

        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.SCENES)).performClick()
        val sceneCardTitle = sceneDisplayTitle(seededScene.sceneTitle, seededScene.sceneNumber, Language.FA)
        composeRule.waitUntilExactlyOneExists(hasText(sceneCardTitle), timeoutMillis = 5_000)
        composeRule.onNodeWithText(sceneCardTitle).clickViaSemantics()

        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.tab.overview", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(SCENE_DETAIL_SHOTS_TAB_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(SEEDED_SHOT_TITLE), timeoutMillis = 5_000)
    }

    @Test
    fun `Shots tab shows the scene's shots and switching Grid to Timeline keeps the shot visible`() {
        createProjectAndOpenShotsTab()

        composeRule.onNodeWithText(SEEDED_SHOT_TITLE).assertExists()

        composeRule.onNodeWithTag(SHOTS_LIST_TIMELINE_TOGGLE_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(SEEDED_SHOT_TITLE), timeoutMillis = 5_000)
    }

    @Test
    fun `creating a new shot with the top-level fields saves it for real and shows it back in the list`() {
        createProjectAndOpenShotsTab()

        composeRule.onNodeWithTag(SHOTS_LIST_NEW_SHOT_FAB_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("shotComposer.title", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(SHOT_COMPOSER_TITLE_FIELD_TAG).performTextInput("The Reveal")
        composeRule.onNodeWithTag(SHOT_COMPOSER_DESCRIPTION_FIELD_TAG).performTextInput("The hero steps into the light, revealing the truth.")

        composeRule.onNodeWithTag(SHOT_COMPOSER_BACK_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText("The Reveal"), timeoutMillis = 5_000)
        // شات از‌پیش‌موجود هم باید همچنان در فهرست باشد.
        composeRule.onNodeWithText(SEEDED_SHOT_TITLE).assertExists()
    }

    /** شات از‌پیش‌Seed‌شده را باز می‌کند، وارد Tab «دوربین» می‌شود و به «سفارشی‌سازی برای این شات» سوییچ می‌کند تا فیلدهای CameraSettings در دسترس باشند. */
    private fun openCameraTabInOverrideMode() {
        createProjectAndOpenShotsTab()
        composeRule.onNodeWithTag(shotCardTag("shot_seed")).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("shotComposer.title", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(SHOT_COMPOSER_CAMERA_TAB_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(CAMERA_TAB_SOURCE_SCENE_TAG), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(CAMERA_TAB_SOURCE_OVERRIDE_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(CAMERA_TAB_ANGLE_FIELD_TAG), timeoutMillis = 5_000)
    }

    @Test
    fun `selecting each camera movement kind shows its own conditional fields`() {
        openCameraTabInOverrideMode()

        // پیش‌فرض: Tier=Basic — فیلدهای Basic باید موجود باشند.
        composeRule.onNodeWithTag(CAMERA_TAB_BASIC_MOVEMENT_TYPE_FIELD_TAG).assertExists()
        composeRule.onNodeWithTag(CAMERA_TAB_BASIC_SPEED_FIELD_TAG).assertExists()

        composeRule.onNodeWithTag(CAMERA_TAB_MOVEMENT_TIER_ADVANCED_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(CAMERA_TAB_ORBIT_DEGREES_FIELD_TAG), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(CAMERA_TAB_ORBIT_SPEED_FIELD_TAG).assertExists()
        composeRule.onNodeWithTag(CAMERA_TAB_ORBIT_MAINTAIN_EYE_LEVEL_TAG).assertExists()

        fun selectAdvanced(type: AdvancedMovementType, expectedFieldTag: String) {
            composeRule.onNodeWithTag(CAMERA_TAB_ADVANCED_MOVEMENT_TYPE_FIELD_TAG).performScrollTo().clickViaSemantics()
            composeRule.waitUntilExactlyOneExists(hasText(advancedMovementTypeLabel(type, Language.FA)), timeoutMillis = 5_000)
            composeRule.onNodeWithText(advancedMovementTypeLabel(type, Language.FA)).performClick()
            composeRule.waitUntilExactlyOneExists(hasTestTag(expectedFieldTag), timeoutMillis = 5_000)
        }

        selectAdvanced(AdvancedMovementType.DRONE_PATH, CAMERA_TAB_DRONE_ALTITUDE_FIELD_TAG)
        composeRule.onNodeWithTag(CAMERA_TAB_DRONE_PATH_TYPE_FIELD_TAG).assertExists()
        composeRule.onNodeWithTag(CAMERA_TAB_DRONE_SPEED_FIELD_TAG).assertExists()

        selectAdvanced(AdvancedMovementType.DOLLY_ZOOM, CAMERA_TAB_DOLLY_FOCAL_START_FIELD_TAG)
        composeRule.onNodeWithTag(CAMERA_TAB_DOLLY_FOCAL_END_FIELD_TAG).assertExists()
        composeRule.onNodeWithTag(CAMERA_TAB_DOLLY_DIRECTION_FIELD_TAG).assertExists()

        selectAdvanced(AdvancedMovementType.HANDHELD_SHAKE, CAMERA_TAB_HANDHELD_INTENSITY_FIELD_TAG)
        composeRule.onNodeWithTag(CAMERA_TAB_HANDHELD_FREQUENCY_FIELD_TAG).assertExists()

        selectAdvanced(AdvancedMovementType.COMPOUND, CAMERA_TAB_COMPOUND_PRIMARY_FIELD_TAG)
        composeRule.onNodeWithTag(CAMERA_TAB_COMPOUND_SECONDARY_FIELD_TAG).assertExists()
        composeRule.onNodeWithTag(CAMERA_TAB_COMPOUND_SYNC_FIELD_TAG).assertExists()

        selectAdvanced(AdvancedMovementType.ORBIT, CAMERA_TAB_ORBIT_DEGREES_FIELD_TAG)

        composeRule.onNodeWithTag(CAMERA_TAB_MOVEMENT_TIER_BASIC_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(CAMERA_TAB_BASIC_MOVEMENT_TYPE_FIELD_TAG), timeoutMillis = 5_000)
    }

    @Test
    fun `switching camera source between scene and override saves the correct source in the shot`() {
        openCameraTabInOverrideMode()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { shotRepository.loadShot("shot_seed").getOrNull()?.camera?.source } == "override"
        }

        composeRule.onNodeWithTag(CAMERA_TAB_SOURCE_SCENE_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("cameraTab.sourceSceneDescription", Language.FA)), timeoutMillis = 5_000)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { shotRepository.loadShot("shot_seed").getOrNull()?.camera?.source } == "scene"
        }
    }
}
