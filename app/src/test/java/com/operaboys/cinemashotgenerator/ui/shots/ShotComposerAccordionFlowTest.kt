package com.operaboys.cinemashotgenerator.ui.shots

import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
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
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode
import com.operaboys.cinemashotgenerator.domain.visualidentity.resolveEffectiveCinematicMode
import com.operaboys.cinemashotgenerator.domain.workflow.ComposerLayoutVariant
import com.operaboys.cinemashotgenerator.ui.dna.cinematicModeLabel
import com.operaboys.cinemashotgenerator.ui.dna.defaultProjectDna
import com.operaboys.cinemashotgenerator.ui.home.CREATE_PROJECT_NAME_FIELD_TAG
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

// رفع G12 باقی‌مانده — composerLayoutVariant.ACCORDION (دستور کار ۲۰۲۶-۰۸-۱۳،
// docs/adr/081-...، طبق mockup docs/design/Cinema Studio.html بخش composerB).
// تست End-to-End واقعی (نه فقط Persist) اثبات می‌کند: (۱) گروه «اصلی» طبق
// مقدار اولیه‌ی mockup (`group: 0`) پیش‌فرض باز است؛ (۲) کلیک روی یک گروه دیگر
// آن را باز و گروه قبلی را می‌بندد — طبق تصمیم دقیق mockup «همیشه فقط یک گروه
// باز»؛ (۳) کلیک دوباره روی همان گروه باز آن را کاملاً می‌بندد (بدون هیچ گروه
// بازی)، دقیقاً طبق `group: st.group === i ? -1 : i`.

private const val PROJECT_ID = "proj_composer_accordion_test"
private const val SCENE_ID = "scene_composer_accordion_test"

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShotComposerAccordionFlowTest {

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
        dataStoreFileName = "composer_accordion_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(dataStoreFileName) }
        )
        workflowViewModel = WorkflowViewModel(
            application = context.applicationContext as Application,
            dataStore = dataStore,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        workflowViewModel.setComposerLayoutVariant(ComposerLayoutVariant.ACCORDION)
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        projectListViewModel = ProjectListViewModel(
            application = context.applicationContext as Application,
            repository = ProjectRepository(database.projectDao(), idProvider = { PROJECT_ID })
        )
        sceneRepository = SceneRepository(database.sceneDao())
        shotRepository = ShotRepository(database.shotDao())
        val assetRepository = AssetRepository(database.assetDao())
        // ADR-086: ShotComposerViewModel اکنون برای پنل خلاصه‌ی زنده به
        // sceneRepository/projectDnaRepository/assetRepository نیاز دارد؛ بدون
        // این تزریق صریح، factory (هم‌الگو ValidationViewModel/StudioOutputViewModel)
        // بی‌صدا به AppDatabase.getInstance() واقعی Production برمی‌گشت — همان
        // باگ تاریخی مستندشده در بالای این فایل (ShotsFlowTest.kt، G16).
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
    }

    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction =
        performSemanticsAction(SemanticsActions.OnClick)

    private fun openShotComposer() {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput("Composer Accordion Test")
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag(studioTabTestTag(StudioTab.STORY)), timeoutMillis = 5_000)

        runBlocking {
            sceneRepository.saveScene(PROJECT_ID, seededScene)
            shotRepository.saveShot(
                Shot(
                    shotId = "shot_seed",
                    sceneId = SCENE_ID,
                    shotNumber = 1,
                    shotTitle = "Opening shot",
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
        composeRule.waitUntilExactlyOneExists(hasText("Opening shot"), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(shotCardTag("shot_seed")).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("shotComposer.title", Language.FA)), timeoutMillis = 5_000)
    }

    @Test
    fun `ACCORDION defaults to the Main group open, showing its real editable fields`() {
        openShotComposer()

        composeRule.onNodeWithTag(composerAccordionGroupHeaderTag("MAIN")).assertIsDisplayed()
        composeRule.onNodeWithTag(SHOT_COMPOSER_TITLE_FIELD_TAG).assertIsDisplayed()
        // Viewport تست بسیار کوچک است (۴۷۰px) — وقتی گروه «اصلی» باز است (پیش‌فرض)،
        // سربرگ «دوربین» ممکن است بیرون از ناحیه‌ی دیده‌شونده باشد؛ performScrollTo
        // (همان الگوی مستندشده‌ی این پروژه، مثلاً ShotsFlowTest.kt) لازم است.
        composeRule.onNodeWithTag(composerAccordionGroupHeaderTag("CAMERA")).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CAMERA_TAB_SOURCE_SCENE_TAG).assertDoesNotExist()
    }

    @Test
    fun `ACCORDION opening a different group closes the previously open one — only one group open at a time`() {
        openShotComposer()

        composeRule.onNodeWithTag(composerAccordionGroupHeaderTag("CAMERA")).performScrollTo().clickViaSemantics()
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("cameraTab.sourceSceneLabel", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(CAMERA_TAB_SOURCE_SCENE_TAG).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SHOT_COMPOSER_TITLE_FIELD_TAG).assertDoesNotExist()
    }

    @Test
    fun `ACCORDION clicking the currently open group again collapses it entirely`() {
        openShotComposer()

        composeRule.onNodeWithTag(composerAccordionGroupHeaderTag("MAIN")).clickViaSemantics()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithTagCount(SHOT_COMPOSER_TITLE_FIELD_TAG) == 0
        }
        composeRule.onNodeWithTag(SHOT_COMPOSER_TITLE_FIELD_TAG).assertDoesNotExist()
    }

    private fun onAllNodesWithTagCount(tag: String): Int =
        composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    // تکمیل Rule یتیم — قدم ۴ از ۴ (ADR-112): این دو تست، سومین و آخرین صفحه‌ی
    // مورد نیاز قدم ۴ (پس از DnaTabFlowTest و ScenesFlowTest) را پوشش می‌دهند —
    // فیلد Override محلی سطح شات و نمایش زنده‌ی حالت مؤثر نهایی
    // (resolveEffectiveCinematicMode). طبق درسِ باگ واقعی پیداشده در تست Mood
    // (ScenesFlowTest.kt)، همه‌ی کلیک‌های آیتم Dropdown از clickViaSemantics
    // استفاده می‌کنند، نه performClick خام.
    @Test
    fun `selecting a Shot Cinematic Mode override persists it and updates the live effective mode display`() {
        openShotComposer()

        composeRule.onNodeWithTag(SHOT_COMPOSER_CINEMATIC_MODE_FIELD_TAG).performScrollTo()
            .assert(hasText(uiString("shotComposer.cinematicModeFromSceneOrProject", Language.FA)))

        composeRule.onNodeWithTag(SHOT_COMPOSER_CINEMATIC_MODE_FIELD_TAG).performScrollTo().clickViaSemantics()
        val fastCutLabel = cinematicModeLabel(CinematicMode.FAST_CUT, Language.FA)
        composeRule.waitUntilExactlyOneExists(hasText(fastCutLabel), timeoutMillis = 5_000)
        composeRule.onNodeWithText(fastCutLabel).clickViaSemantics()

        val effectiveText = uiTemplate("shotComposer.effectiveCinematicModeTemplate", Language.FA, "mode" to fastCutLabel)
        composeRule.waitUntilExactlyOneExists(hasText(effectiveText), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(SHOT_COMPOSER_CINEMATIC_MODE_FIELD_TAG).assertIsDisplayed()
    }

    @Test
    fun `selecting the follow-scene-or-project option resets the Shot Cinematic Mode override to null`() {
        openShotComposer()

        composeRule.onNodeWithTag(SHOT_COMPOSER_CINEMATIC_MODE_FIELD_TAG).performScrollTo().clickViaSemantics()
        val longTakeLabel = cinematicModeLabel(CinematicMode.LONG_TAKE, Language.FA)
        composeRule.waitUntilExactlyOneExists(hasText(longTakeLabel), timeoutMillis = 5_000)
        composeRule.onNodeWithText(longTakeLabel).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(
            hasText(uiTemplate("shotComposer.effectiveCinematicModeTemplate", Language.FA, "mode" to longTakeLabel)),
            timeoutMillis = 5_000
        )

        composeRule.onNodeWithTag(SHOT_COMPOSER_CINEMATIC_MODE_FIELD_TAG).performScrollTo().clickViaSemantics()
        val followLabel = uiString("shotComposer.cinematicModeFromSceneOrProject", Language.FA)
        composeRule.waitUntilExactlyOneExists(hasText(followLabel), timeoutMillis = 5_000)
        composeRule.onNodeWithText(followLabel).clickViaSemantics()

        composeRule.waitUntilExactlyOneExists(hasText(followLabel), timeoutMillis = 5_000)
        val effectiveMode = runBlocking {
            val dnaRepository = ProjectDnaRepository(database.projectDnaDao())
            // مثل خودِ ShotComposerViewModel (init، خط ۴۰۷-۴۰۸)، اگر هنوز هیچ ردیف DNA
            // ذخیره نشده (کاربر هرگز تب DNA را باز نکرده)، defaultProjectDna جایگزین می‌شود.
            val dna = dnaRepository.loadProjectDna(PROJECT_ID).getOrThrow()
                ?: defaultProjectDna(PROJECT_ID) { "dna_placeholder" }
            val savedShot = shotRepository.loadShot("shot_seed").getOrThrow()!!
            assertEquals(null, savedShot.cinematicModeOverride)
            resolveEffectiveCinematicMode(dna, seededScene, savedShot)
        }
        composeRule.onNodeWithText(
            uiTemplate("shotComposer.effectiveCinematicModeTemplate", Language.FA, "mode" to cinematicModeLabel(effectiveMode, Language.FA))
        ).performScrollTo().assertIsDisplayed()
    }
}
