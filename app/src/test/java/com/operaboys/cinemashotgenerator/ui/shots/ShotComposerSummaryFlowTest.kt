package com.operaboys.cinemashotgenerator.ui.shots

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.SceneLocation
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ContrastRatio
import com.operaboys.cinemashotgenerator.domain.sceneconditions.KeyLightPosition
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingMotivation
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingSettings
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.shot.SourcedSettings
import com.operaboys.cinemashotgenerator.ui.home.CREATE_PROJECT_NAME_FIELD_TAG
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.navigation.StudioTab
import com.operaboys.cinemashotgenerator.ui.navigation.studioTabTestTag
import com.operaboys.cinemashotgenerator.ui.outputdelivery.modelProfileDisplayName
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.scenes.SCENE_DETAIL_SHOTS_TAB_TAG
import com.operaboys.cinemashotgenerator.ui.scenes.sceneDisplayTitle
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

// یافته‌ی #۱۳ appendix ADR-081 (ADR-086) — تست End-to-End واقعی پنل خلاصه‌ی زنده‌ی
// پایین Shot Composer: (۱) یک شات با نقض واقعی Blocking (نور خورشید در شب — همان
// فیکسچر ValidationFlowTest.kt) → شمارش صحیح در نوار بالا؛ (۲) کلیک آیکون کپی یک
// چیپ مدل → محتوای واقعی Clipboard سیستم. الگوی راه‌اندازی/Clipboard عیناً از
// ValidationFlowTest.kt و OutputDeliveryFlowTest.kt گرفته شده (createAndroidComposeRule
// به‌جای createComposeRule، طبق یافته‌ی مستندشده‌ی همان قدم درباره‌ی جداییِ
// ClipboardManager گرفته‌شده از ApplicationProvider در برابر LocalClipboardManager).
// جزئیات کامل تصمیمات در docs/adr/086-shot-composer-summary-panel.md.

private const val PROJECT_ID = "proj_composer_summary_flow_test"
private const val SCENE_ID = "scene_composer_summary_flow_test"
private const val SHOT_ID = "shot_composer_summary_flow_test"

private val seededScene = Scene(
    sceneId = SCENE_ID,
    sceneTitle = "A Night Scene",
    sceneNumber = 1,
    narrativeRole = NarrativeRole.DEVELOPMENT,
    location = SceneLocation(type = LocationType.MIXED, description = "a quiet street"),
    timeOfDay = TimeOfDay.NIGHT,
    atmospherePrimary = Atmosphere.CALM,
    shotCount = 1
)

/** طبق Rule «نور خورشید در شب» (checkSunlightAtNight، واحد ۰۸) — یک نقض عمدی Blocking. */
private val seededShot = Shot(
    shotId = SHOT_ID,
    sceneId = SCENE_ID,
    shotNumber = 1,
    shotTitle = "Impossible sunlight",
    shotDescription = "A shot with an impossible lighting motivation for its scene's time of day.",
    shotGoal = ShotGoal.ESTABLISHING,
    shotType = ShotType.WIDE,
    durationSeconds = 6f,
    motionLevel = MotionLevel.STATIC,
    lighting = SourcedSettings(
        source = "override",
        overrideValue = LightingSettings(
            style = LightingStyle.NATURAL_LIGHT,
            keyLightPosition = KeyLightPosition.SIDE,
            contrastRatio = ContrastRatio.MEDIUM,
            lightingMotivation = LightingMotivation.SUNLIGHT
        )
    ),
    soundProfile = SoundProfile(enabled = false),
    characterIds = listOf("char_placeholder")
)

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShotComposerSummaryFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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
        dataStoreFileName = "composer_summary_flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
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
        val projectDnaRepository = ProjectDnaRepository(database.projectDnaDao())

        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    sceneRepository = sceneRepository,
                    shotRepository = shotRepository,
                    assetRepository = assetRepository,
                    projectDnaRepository = projectDnaRepository
                )
            }
        }
    }

    @After
    fun tearDown() {
        composeRule.waitForIdle()
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        // database.close() عمداً حذف شد — طبق یافته‌ی مستندشده‌ی ADR-071
        // (docs/adr/070-flake-root-cause-investigation.md)، هم‌الگو با
        // ValidationFlowTest.kt/OutputDeliveryFlowTest.kt.
    }

    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction {
        performScrollTo()
        return performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun createProjectAndOpenComposer() {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput("Composer Summary Flow Test")
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Composer Summary Flow Test"), timeoutMillis = 15_000)

        runBlocking {
            sceneRepository.saveScene(PROJECT_ID, seededScene)
            shotRepository.saveShot(seededShot)
        }

        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.SCENES)).performClick()
        val sceneCardTitle = sceneDisplayTitle(seededScene.sceneTitle, seededScene.sceneNumber, Language.FA)
        composeRule.waitUntilExactlyOneExists(hasText(sceneCardTitle), timeoutMillis = 15_000)
        composeRule.onNodeWithText(sceneCardTitle).clickViaSemantics()

        composeRule.waitUntilExactlyOneExists(hasText(uiString("sceneDetail.tab.overview", Language.FA)), timeoutMillis = 15_000)
        composeRule.onNodeWithTag(SCENE_DETAIL_SHOTS_TAB_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(seededShot.shotTitle!!), timeoutMillis = 15_000)

        composeRule.onNodeWithTag(shotCardTag(SHOT_ID)).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("shotComposer.title", Language.FA)), timeoutMillis = 15_000)
        composeRule.waitUntilExactlyOneExists(hasText("Impossible sunlight"), timeoutMillis = 15_000)
    }

    @Test
    fun `the summary panel shows the real Blocking count for a shot with a sunlight-at-night violation`() {
        createProjectAndOpenComposer()

        // aggregateShotValidation واقعاً روی داده‌ی Seed‌شده اجرا می‌شود؛ منتظر
        // به‌روزرسانی StateFlow (init block + refreshValidationSummary) می‌مانیم.
        composeRule.waitUntilExactlyOneExists(hasText("1 Blocking", substring = true), timeoutMillis = 15_000)

        composeRule.onNodeWithTag(SHOT_COMPOSER_VALIDATION_BUTTON_TAG).assertTextContains("1", substring = true)
        composeRule.onNodeWithTag(SHOT_COMPOSER_VALIDATION_BUTTON_TAG).assertTextContains("0", substring = true)
    }

    @Test
    fun `clicking a model chip's copy icon copies the real model display name to the system clipboard`() {
        createProjectAndOpenComposer()
        composeRule.waitUntilExactlyOneExists(hasText("1 Blocking", substring = true), timeoutMillis = 15_000)

        val clipboardManager = composeRule.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.clearPrimaryClip()

        // یافته‌ی واقعی دیباگ این تست: برخلاف OutputDeliveryFlowTest (که بعد از
        // کلیک کپی صبر می‌کند)، اینجا composeRule.waitForIdle() منجر به OutOfMemoryError
        // واقعی می‌شد (ArrayDeque در ShadowTrace.endSection، بعد از +۴ دقیقه Pump شدن
        // فریم) — چون کلیک آیکون کپی هم‌زمان onShowMessage را صدا می‌زند که یک
        // Snackbar واقعی (`snackbarHostState.showSnackbar`، MainScaffold.kt) باز
        // می‌کند؛ Animation/تایمر آن Snackbar زیر TestMonotonicFrameClock این محیط
        // هرگز به Idle واقعی نمی‌رسید. رفع لازم نیست چون نوشتن Clipboard خودش همگام
        // (Synchronous) و *قبل* از فراخوانی onShowMessage داخل همان‌ Lambda کلیک انجام
        // می‌شود — performSemanticsAction(OnClick) خودِ Lambda را بلافاصله اجرا می‌کند،
        // پس نیازی به waitForIdle() برای مشاهده‌ی نتیجه‌ی واقعی روی Clipboard نیست.
        val targetProfileId = ComposerSummaryModelChipKeys.first()
        composeRule.onNodeWithTag(shotComposerCopyModelButtonTag(targetProfileId)).clickViaSemantics()

        val clipText = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
        assertTrue("clipboard should contain the model display name", !clipText.isNullOrBlank())
        assertTrue(clipText == modelProfileDisplayName(targetProfileId))
    }
}
