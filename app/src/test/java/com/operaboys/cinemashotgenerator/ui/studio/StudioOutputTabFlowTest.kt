package com.operaboys.cinemashotgenerator.ui.studio

import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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
import com.operaboys.cinemashotgenerator.data.repository.AudioContextRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.data.repository.PromptGenerationRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.SettingsResolutionRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.camera.BasicMovementType
import com.operaboys.cinemashotgenerator.domain.camera.CameraAngle
import com.operaboys.cinemashotgenerator.domain.camera.CameraDistance
import com.operaboys.cinemashotgenerator.domain.camera.CameraMovement
import com.operaboys.cinemashotgenerator.domain.camera.CameraSettings
import com.operaboys.cinemashotgenerator.domain.camera.DepthOfField
import com.operaboys.cinemashotgenerator.domain.camera.Framing
import com.operaboys.cinemashotgenerator.domain.camera.FocusMode
import com.operaboys.cinemashotgenerator.domain.camera.LensType
import com.operaboys.cinemashotgenerator.domain.camera.Stabilization
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.SceneLocation
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ContrastRatio
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.KeyLightPosition
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingMotivation
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.shot.SourcedSettings
import com.operaboys.cinemashotgenerator.ui.dna.defaultProjectDna
import com.operaboys.cinemashotgenerator.ui.home.CREATE_PROJECT_NAME_FIELD_TAG
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.navigation.StudioTab
import com.operaboys.cinemashotgenerator.ui.navigation.studioTabTestTag
import com.operaboys.cinemashotgenerator.ui.outputdelivery.OUTPUT_DELIVERY_PREVIEW_CARD_TAG
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import com.operaboys.cinemashotgenerator.ui.validation.VALIDATION_BLOCKING_COUNT_CARD_TAG
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

// یافته‌ی #۱۴ appendix ADR-081 (ADR-084) — تست End-to-End واقعی Tab «خروجی»
// Studio: (۱) کارت وضعیت Validation شمارش واقعی Blocking/Warning کل پروژه را
// نشان می‌دهد؛ (۲) کارت آماده‌بودن Prompt Generation شمارش واقعی «آماده» را
// نشان می‌دهد؛ (۳) با دقیقاً یک Shot در پروژه، هم کلیک کارت Validation و هم
// دکمه‌ی اشتراک‌گذاری مستقیماً به مقصد واقعی (ValidationScreen/OutputDeliveryScreen)
// می‌روند؛ (۴) با چند Shot، دیالوگ کوتاه انتخاب شات ظاهر می‌شود و انتخاب واقعاً
// Navigate می‌کند. الگوی راه‌اندازی عیناً از ValidationFlowTest.kt گرفته شده.

private const val PROJECT_ID_SINGLE = "proj_studio_output_flow_single"
private const val SCENE_ID_SINGLE = "scene_studio_output_flow_single"
private const val SHOT_ID_SINGLE = "shot_studio_output_flow_single"

private const val PROJECT_ID_MULTI = "proj_studio_output_flow_multi"
private const val SCENE_ID_A = "scene_studio_output_flow_multi_a"
private const val SHOT_ID_A = "shot_studio_output_flow_multi_a"
private const val SCENE_ID_B = "scene_studio_output_flow_multi_b"
private const val SHOT_ID_B = "shot_studio_output_flow_multi_b"

private val blockingScene = Scene(
    sceneId = SCENE_ID_SINGLE,
    sceneTitle = "A Night Scene",
    sceneNumber = 1,
    narrativeRole = NarrativeRole.DEVELOPMENT,
    location = SceneLocation(type = LocationType.MIXED, description = "a quiet street"),
    timeOfDay = TimeOfDay.NIGHT,
    atmospherePrimary = Atmosphere.CALM,
    shotCount = 1
)

/**
 * ترکیب دوربین/محیط «بی‌خطر» — طبق یافته‌ی واقعی دیباگ این قدم
 * (docs/adr/057-unit16-phase5-step3-output-delivery.md، هم‌الگو با فیکسچر
 * اثبات‌شده‌ی `OutputDeliveryFlowTest.kt`): رسیدن واقعی به `OutputDeliveryScreen`
 * از طریق `PromptGenerationRepository.collectData` نیازمند این است که camera و
 * environment هم `source="override"` باشند (نه فقط lighting) — وگرنه
 * `resolveSourcedSettings` با `IllegalStateException` شکست می‌خورد. مقادیر این
 * دو زیر عمداً طوری انتخاب شدند که هیچ‌کدام از Rule های سطح ۲ دوربین/محیط
 * (`CameraValidation.kt`/`EnvironmentValidation.kt`) را نقض نکنند.
 */
private val safeCamera = SourcedSettings(
    source = "override",
    overrideValue = CameraSettings(
        angle = CameraAngle.EYE_LEVEL,
        distance = CameraDistance.MEDIUM,
        movement = CameraMovement.Basic(type = BasicMovementType.PAN_LEFT, speed = "slow"),
        lensType = LensType.STANDARD,
        depthOfField = DepthOfField.MEDIUM,
        focusMode = FocusMode.AUTO,
        stabilization = Stabilization.TRIPOD,
        framing = Framing.RULE_OF_THIRDS
    )
)
private val safeEnvironment = SourcedSettings(source = "override", overrideValue = EnvironmentSettings(weatherType = WeatherType.CLEAR))

/** طبق Rule «نور خورشید در شب» (checkSunlightAtNight، واحد ۰۸) — یک نقض عمدی Blocking. */
private val blockingShot = Shot(
    shotId = SHOT_ID_SINGLE,
    sceneId = SCENE_ID_SINGLE,
    shotNumber = 1,
    shotTitle = "Impossible sunlight",
    shotDescription = "A shot with an impossible lighting motivation for its scene's time of day.",
    shotGoal = ShotGoal.ESTABLISHING,
    shotType = ShotType.WIDE,
    durationSeconds = 6f,
    motionLevel = MotionLevel.STATIC,
    camera = safeCamera,
    lighting = SourcedSettings(
        source = "override",
        overrideValue = LightingSettings(
            style = LightingStyle.NATURAL_LIGHT,
            keyLightPosition = KeyLightPosition.SIDE,
            contrastRatio = ContrastRatio.MEDIUM,
            lightingMotivation = LightingMotivation.SUNLIGHT
        )
    ),
    environment = safeEnvironment,
    soundProfile = SoundProfile(enabled = false),
    characterIds = listOf("char_placeholder")
)

private fun cleanScene(sceneId: String, sceneNumber: Int) = Scene(
    sceneId = sceneId,
    sceneTitle = "Clean Scene $sceneNumber",
    sceneNumber = sceneNumber,
    narrativeRole = NarrativeRole.DEVELOPMENT,
    location = SceneLocation(type = LocationType.MIXED, description = "a quiet street"),
    timeOfDay = TimeOfDay.NIGHT,
    atmospherePrimary = Atmosphere.CALM,
    shotCount = 1
)

/**
 * همان `safeCamera`/`safeEnvironment` بالا + یک Lighting که هیچ Rule ای را نقض
 * نمی‌کند (`PRACTICAL` — نه SUNLIGHT/MOONLIGHT، پس بی‌ربط به ساعت صحنه؛
 * `keyLightPosition=SIDE` نه BOTTOM؛ style=NATURAL_LIGHT نه LOW_KEY) — این شات
 * تضمین‌شده صفر Blocking/Warning دارد (طبق ValidationAggregator.kt) و هم‌زمان
 * (برخلاف نسخه‌ی قبلی این فیکسچر که camera/environment را Override نمی‌کرد)
 * واقعاً تا OutputDeliveryScreen هم می‌رسد بدون IllegalStateException.
 */
private fun cleanShot(shotId: String, sceneId: String) = Shot(
    shotId = shotId,
    sceneId = sceneId,
    shotNumber = 1,
    shotTitle = "Clean shot",
    shotDescription = "A perfectly valid shot with no rule violations at all.",
    shotGoal = ShotGoal.ESTABLISHING,
    shotType = ShotType.WIDE,
    durationSeconds = 4f,
    motionLevel = MotionLevel.STATIC,
    camera = safeCamera,
    lighting = SourcedSettings(
        source = "override",
        overrideValue = LightingSettings(
            style = LightingStyle.NATURAL_LIGHT,
            keyLightPosition = KeyLightPosition.SIDE,
            contrastRatio = ContrastRatio.MEDIUM,
            lightingMotivation = LightingMotivation.PRACTICAL
        )
    ),
    environment = safeEnvironment,
    soundProfile = SoundProfile(enabled = false),
    characterIds = listOf("char_placeholder")
)

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudioOutputTabFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dataStoreFileName: String
    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var database: AppDatabase
    private lateinit var projectListViewModel: ProjectListViewModel
    private lateinit var sceneRepository: SceneRepository
    private lateinit var shotRepository: ShotRepository
    private lateinit var projectDnaRepository: ProjectDnaRepository

    private fun setUpWithProjectId(projectId: String) {
        context = ApplicationProvider.getApplicationContext()
        dataStoreFileName = "studio_output_flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
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
            repository = ProjectRepository(database.projectDao(), idProvider = { projectId })
        )
        sceneRepository = SceneRepository(database.sceneDao())
        shotRepository = ShotRepository(database.shotDao())
        val assetRepository = AssetRepository(database.assetDao())
        projectDnaRepository = ProjectDnaRepository(database.projectDnaDao())
        // یافته‌ی واقعی دیباگ این قدم (هم‌الگو با OutputDeliveryFlowTest.kt): بدون
        // این Repository صریح، صفحه‌ی OutputDelivery به AppDatabase.getInstance
        // (دیتابیس واقعی/سراسری Production) سقوط می‌کرد، نه دیتابیس In-Memory این
        // تست — یعنی Scene/Shot/DNA Seed‌شده‌ی این تست هرگز دیده نمی‌شدند.
        val promptGenerationRepository = PromptGenerationRepository(
            database.shotDao(),
            database.sceneDao(),
            projectDnaRepository,
            assetRepository,
            SettingsResolutionRepository(database.shotDao(), database.sceneDao()),
            AudioContextRepository(database.audioContextDao())
        )

        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    sceneRepository = sceneRepository,
                    shotRepository = shotRepository,
                    assetRepository = assetRepository,
                    projectDnaRepository = projectDnaRepository,
                    promptGenerationRepository = promptGenerationRepository
                )
            }
        }
    }

    @After
    fun tearDown() {
        composeRule.waitForIdle()
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        // database.close() عمداً حذف شد — همان دلیل مستندشده در ValidationFlowTest.kt
        // (docs/adr/070-flake-root-cause-investigation.md، رفع در ADR-071).
    }

    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction =
        performSemanticsAction(SemanticsActions.OnClick)

    /**
     * یافته‌ی واقعی دیباگ این قدم: `SceneEntity`/`ShotEntity` هر دو `ForeignKey`
     * واقعی به `projects.projectId` دارند (`onDelete = CASCADE`) و Room این
     * محدودیت را حتی روی دیتابیس In-Memory تست هم اجرا می‌کند. نسخه‌ی اولیه‌ی این
     * تست Scene/Shot را **قبل از** ایجاد واقعی ردیف Project (از طریق UI) Seed
     * می‌کرد — چون `sceneRepository.saveScene`/`shotRepository.saveShot` هر دو
     * با `runCatching` پیچیده شده‌اند، نقض این محدودیت بی‌صدا به `Result.failure`
     * تبدیل می‌شد (نه یک Exception قابل‌مشاهده)، یعنی Scene/Shot هرگز واقعاً
     * ذخیره نمی‌شدند — پروژه با ۰ Shot باز می‌شد (حالت خالی Tab، نه کارت‌های
     * وضعیت)، دقیقاً همان چیزی که `waitUntilExactlyOneExists(STUDIO_OUTPUT_VALIDATION_CARD_TAG)`
     * را با Timeout رد می‌کرد. رفع با هم‌ترازکردن ترتیب با الگوی اثبات‌شده‌ی
     * `ValidationFlowTest.kt`: اول ایجاد واقعی پروژه از طریق UI، بعد Seed.
     */
    private fun createProjectAndOpenOutputTab(projectName: String, seed: () -> Unit) {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput(projectName)
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(projectName), timeoutMillis = 15_000)

        seed()

        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.OUTPUT)).performClick()
        composeRule.waitUntilExactlyOneExists(hasTestTag(STUDIO_OUTPUT_VALIDATION_CARD_TAG), timeoutMillis = 15_000)
    }

    @Test
    fun `Output tab shows real project-wide Blocking count and readiness, single shot navigates Validation card directly`() {
        setUpWithProjectId(PROJECT_ID_SINGLE)

        createProjectAndOpenOutputTab("Output Single Shot Test") {
            runBlocking {
                projectDnaRepository.saveProjectDna(defaultProjectDna(PROJECT_ID_SINGLE) { "dna_studio_output_single" })
                sceneRepository.saveScene(PROJECT_ID_SINGLE, blockingScene)
                shotRepository.saveShot(blockingShot)
            }
        }

        composeRule.onNodeWithTag(STUDIO_OUTPUT_VALIDATION_CARD_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(STUDIO_OUTPUT_VALIDATION_CARD_TAG).assertTextContains(
            uiTemplate("studioOutput.validationCountsTemplate", Language.FA, "blocking" to "1", "warning" to "0"),
            substring = true
        )
        composeRule.onNodeWithTag(STUDIO_OUTPUT_PROMPT_CARD_TAG).assertTextContains(
            uiTemplate("studioOutput.readyShotsTemplate", Language.FA, "ready" to "0", "total" to "1"),
            substring = true
        )

        // فقط یک Shot در پروژه — کلیک کارت Validation باید مستقیماً (بدون دیالوگ
        // انتخاب) به صفحه‌ی واقعی Validation برود.
        composeRule.onNodeWithTag(STUDIO_OUTPUT_VALIDATION_CARD_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(VALIDATION_BLOCKING_COUNT_CARD_TAG), timeoutMillis = 15_000)
        // یافته‌ی واقعی دیباگ این قدم (هم‌الگو با ValidationFlowTest.kt): این کارت
        // بدون هیچ isLoaded Gate ای همیشه رندر می‌شود (`report?.blockingCount ?: 0`
        // در ValidationScreen.kt) — یعنی خودِ testTag بلافاصله (با «۰» موقت) ظاهر
        // می‌شود، پیش از تکمیل واقعی بارگذاری Async. صبر واقعی روی متن خودِ نقض
        // (نشانه‌ی غیرمبهم اتمام بارگذاری)، نه صرفاً وجود testTag کارت.
        composeRule.waitUntilAtLeastOneExists(hasText("نور خورشید در شب", substring = true), timeoutMillis = 15_000)
        composeRule.onNodeWithTag(VALIDATION_BLOCKING_COUNT_CARD_TAG).assertTextContains("1", substring = true)
    }

    @Test
    fun `share button with a single shot navigates directly to the real Output Delivery screen`() {
        setUpWithProjectId(PROJECT_ID_SINGLE)

        createProjectAndOpenOutputTab("Output Single Shot Share Test") {
            runBlocking {
                projectDnaRepository.saveProjectDna(defaultProjectDna(PROJECT_ID_SINGLE) { "dna_studio_output_share" })
                sceneRepository.saveScene(PROJECT_ID_SINGLE, blockingScene)
                shotRepository.saveShot(blockingShot)
            }
        }

        composeRule.onNodeWithTag(STUDIO_OUTPUT_SHARE_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(OUTPUT_DELIVERY_PREVIEW_CARD_TAG), timeoutMillis = 15_000)
        // یافته‌ی واقعی دیباگ این قدم: OutputDeliveryScreen یک Column بلند و
        // اسکرول‌شونده است (چیپ‌های مدل + کارت پیش‌نمایش + بخش Warnings) — در
        // Viewport کوچک تست، کارت پیش‌نمایش معمولاً پایین‌تر از ناحیه‌ی دیده‌شونده‌ی
        // اولیه است؛ همان راه‌حل مستندشده‌ی این پروژه (`performScrollTo` پیش از
        // `assertIsDisplayed`).
        composeRule.onNodeWithTag(OUTPUT_DELIVERY_PREVIEW_CARD_TAG).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `with multiple shots, the share button shows a shot picker dialog and the chosen shot navigates to Output Delivery`() {
        setUpWithProjectId(PROJECT_ID_MULTI)
        val sceneA = cleanScene(SCENE_ID_A, sceneNumber = 1)
        val sceneB = cleanScene(SCENE_ID_B, sceneNumber = 2)
        val shotA = cleanShot(SHOT_ID_A, SCENE_ID_A)
        val shotB = cleanShot(SHOT_ID_B, SCENE_ID_B)

        createProjectAndOpenOutputTab("Output Multi Shot Test") {
            runBlocking {
                projectDnaRepository.saveProjectDna(defaultProjectDna(PROJECT_ID_MULTI) { "dna_studio_output_multi" })
                sceneRepository.saveScene(PROJECT_ID_MULTI, sceneA)
                sceneRepository.saveScene(PROJECT_ID_MULTI, sceneB)
                shotRepository.saveShot(shotA)
                shotRepository.saveShot(shotB)
            }
        }

        // هر دو شات بدون نقض Rule — «۲ از ۲ آماده».
        composeRule.onNodeWithTag(STUDIO_OUTPUT_PROMPT_CARD_TAG).assertTextContains(
            uiTemplate("studioOutput.readyShotsTemplate", Language.FA, "ready" to "2", "total" to "2"),
            substring = true
        )
        composeRule.onNodeWithTag(STUDIO_OUTPUT_VALIDATION_CARD_TAG).assertTextContains(
            uiTemplate("studioOutput.validationCountsTemplate", Language.FA, "blocking" to "0", "warning" to "0"),
            substring = true
        )

        composeRule.onNodeWithTag(STUDIO_OUTPUT_SHARE_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("studioOutput.pickShotDialogTitle", Language.FA)), timeoutMillis = 15_000)
        composeRule.onNodeWithTag(studioOutputShotPickerItemTag(SHOT_ID_A)).assertIsDisplayed()
        composeRule.onNodeWithTag(studioOutputShotPickerItemTag(SHOT_ID_B)).assertIsDisplayed()

        composeRule.onNodeWithTag(studioOutputShotPickerItemTag(SHOT_ID_A)).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(OUTPUT_DELIVERY_PREVIEW_CARD_TAG), timeoutMillis = 15_000)
        // یافته‌ی واقعی دیباگ این قدم: OutputDeliveryScreen یک Column بلند و
        // اسکرول‌شونده است (چیپ‌های مدل + کارت پیش‌نمایش + بخش Warnings) — در
        // Viewport کوچک تست، کارت پیش‌نمایش معمولاً پایین‌تر از ناحیه‌ی دیده‌شونده‌ی
        // اولیه است؛ همان راه‌حل مستندشده‌ی این پروژه (`performScrollTo` پیش از
        // `assertIsDisplayed`).
        composeRule.onNodeWithTag(OUTPUT_DELIVERY_PREVIEW_CARD_TAG).performScrollTo().assertIsDisplayed()
    }
}
