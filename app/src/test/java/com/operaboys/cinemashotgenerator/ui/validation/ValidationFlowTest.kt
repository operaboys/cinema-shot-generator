package com.operaboys.cinemashotgenerator.ui.validation

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
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.scenes.SCENE_DETAIL_SHOTS_TAB_TAG
import com.operaboys.cinemashotgenerator.ui.scenes.sceneDisplayTitle
import com.operaboys.cinemashotgenerator.ui.shots.SHOT_COMPOSER_VALIDATION_BUTTON_TAG
import com.operaboys.cinemashotgenerator.ui.shots.shotCardTag
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

// واحد ۱۶ فاز ۵ — قدم ۱: تست End-to-End واقعی صفحه‌ی Validation — طبق دستور کار:
// (۱) ورود از دکمه‌ی «اعتبارسنجی این شات» داخل Shot Composer؛ (۲) شمارش دقیق
// BLOCKING در کارت خلاصه؛ (۳) حداقل یکی از ۱۳ Rule نور/محیط (اینجا: نور خورشید در
// شب) واقعاً در صفحه ظاهر می‌شود؛ (۴) کارت‌های شمارش Opaque/قابل‌مشاهده‌اند. الگوی
// راه‌اندازی عیناً از ShotsFlowTest.kt گرفته شده. جزئیات کامل تصمیمات در
// docs/adr/055-unit16-phase5-step1-validation-screen.md.
//
// یافته‌ی مستندشده در ADR-055 درباره‌ی چرا این تست فقط وجود/متن/`assertIsDisplayed`
// کارت‌های شمارش را بررسی می‌کند، نه رنگ واقعی پیکسل: این کدبیس در کل جلسه هیچ
// زیرساخت screenshot/captureToImage ای ندارد (تأییدشده با grep)؛ الزام Opaque/
// بدون-Alpha در سطح کد با استفاده‌ی مستقیم از توکن‌های Solid تم (بدون
// `.copy(alpha=...)`) تضمین شده، هم‌الگو با OpaqueChip موجود.

private const val PROJECT_ID = "proj_validation_flow_test"
private const val SCENE_ID = "scene_validation_flow_test"
private const val SHOT_ID = "shot_validation_flow_test"

/**
 * `shotCount = 1` عمداً صریح تنظیم شده — پیش‌فرض واقعی `Scene.shotCount` صفر است؛
 * بدون این مقدار، Rule 1 واحد ۰۴ (`validateSceneHasShotsBeforeFinalize`) یک
 * BLOCKING اضافه‌ی ناخواسته تولید می‌کرد (یافته‌ی واقعی دیباگ این قدم — شمارش
 * «دقیقاً یک BLOCKING» تست بدون این فیلد هرگز درست نمی‌شد).
 */
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
class ValidationFlowTest {

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
        dataStoreFileName = "validation_flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
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
        // database.close() عمداً حذف شد — ریشه‌ی واقعی Flake تاریخی این Suite
        // (docs/adr/070-flake-root-cause-investigation.md، رفع در ADR-071):
        // Room.inMemoryDatabaseBuilder نیازی به Close صریح ندارد (بدون فایل روی
        // دیسک، GC آن را با نابودی نمونه‌ی این کلاس تست جمع می‌کند)؛ این خط قبلاً
        // با Coroutine های ناتمام viewModelScope روی Executor داخلی Room مسابقه
        // می‌داد — نه یک نشتی حافظه‌ی فراموش‌شده. waitForIdle() بالا همچنان برای
        // Idling خودِ Compose مفید است، فقط دیگر ایمنی close() را تضمین نمی‌کند.
    }

    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction =
        performSemanticsAction(SemanticsActions.OnClick)

    /**
     * پروژه‌ی واقعی می‌سازد، Scene/Shot را زیر همان projectId Seed می‌کند، و وارد صفحه‌ی
     * Validation آن شات می‌شود. طبق یافته‌ی این قدم (اجرای مکرر تحت بار سنگین این
     * Sandbox، نه یک باگ کد واقعی — تأییدشده با اجرای مجزا/تکراری): زمان‌بندی ۵۰۰۰ms
     * پیش‌فرض این خانواده‌ی تست (فایل‌های قبلی فاز ۴) گاهی زیر بار سنگین این محیط اجرا
     * کافی نیست؛ اینجا به ۱۵۰۰۰ms افزایش یافت — منطق/الگوی کلیک بدون تغییر.
     */
    private fun createProjectAndOpenValidation() {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput("Validation Flow Test")
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Validation Flow Test"), timeoutMillis = 15_000)

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

        composeRule.onNodeWithTag(SHOT_COMPOSER_VALIDATION_BUTTON_TAG).performClick()
        // یافته‌ی واقعی این قدم: انتظار روی متن دقیق «اعتبارسنجی» (uiString عنوان
        // صفحه) غیرقابل‌اعتماد است — Nav Drawer (که همیشه در درخت Semantics حاضر
        // است، حتی وقتی بسته/نامرئی است، طبق ModalNavigationDrawer) یک لینک با
        // دقیقاً همان متن دارد (drawer.validation)، پس «exactly 1» هرگز درست نمی‌شود
        // (همیشه ۲ گره). رفع با انتظار روی testTag منحصربه‌فرد این صفحه (کارت
        // شمارش BLOCKING)، نه متن عنوان.
        composeRule.waitUntilExactlyOneExists(hasTestTag(VALIDATION_BLOCKING_COUNT_CARD_TAG), timeoutMillis = 15_000)
    }

    @Test
    fun `opening Validation for a shot with a sunlight-at-night violation shows exactly one BLOCKING issue`() {
        createProjectAndOpenValidation()

        composeRule.waitUntilExactlyOneExists(hasText("نور خورشید در شب", substring = true), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(VALIDATION_BLOCKING_COUNT_CARD_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(VALIDATION_WARNING_COUNT_CARD_TAG).assertIsDisplayed()
        // یک BLOCKING واقعی — کارت باید عدد «۱» را نشان دهد.
        composeRule.onNodeWithTag(VALIDATION_BLOCKING_COUNT_CARD_TAG).assertTextContains("1", substring = true)
        composeRule.onNodeWithTag(VALIDATION_WARNING_COUNT_CARD_TAG).assertTextContains("0", substring = true)
    }

    @Test
    fun `the BLOCKING and WARNING summary cards render as distinct, fully visible, solid-styled nodes`() {
        createProjectAndOpenValidation()

        composeRule.onNodeWithTag(VALIDATION_BLOCKING_COUNT_CARD_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(VALIDATION_WARNING_COUNT_CARD_TAG).assertIsDisplayed()
        // یافته‌ی واقعی این قدم: هر Issue Card هم برچسب شدت («BLOCKING»/«WARNING»)
        // نشان می‌دهد، پس جست‌وجوی سراسری صفحه برای این متن می‌تواند بیش از یک گره
        // پیدا کند (وقتی حداقل یک Issue با همان شدت وجود دارد) — رفتار صحیح UI،
        // نه باگ. بررسی این‌جا عمداً محدود به خودِ کارت شمارش می‌شود (assertTextContains
        // روی testTag)، نه یک جست‌وجوی سراسری متن.
        composeRule.onNodeWithTag(VALIDATION_BLOCKING_COUNT_CARD_TAG)
            .assertTextContains(uiString("validation.blockingLabel", Language.FA), substring = true)
        composeRule.onNodeWithTag(VALIDATION_WARNING_COUNT_CARD_TAG)
            .assertTextContains(uiString("validation.warningLabel", Language.FA), substring = true)
    }
}
