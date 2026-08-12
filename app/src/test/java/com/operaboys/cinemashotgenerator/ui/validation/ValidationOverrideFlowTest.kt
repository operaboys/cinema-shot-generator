package com.operaboys.cinemashotgenerator.ui.validation

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.HumanOverrideRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
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
import com.operaboys.cinemashotgenerator.domain.dna.AspectRatio
import com.operaboys.cinemashotgenerator.domain.dna.ColorTemperature
import com.operaboys.cinemashotgenerator.domain.dna.ContrastLevel
import com.operaboys.cinemashotgenerator.domain.dna.CoreIdentity
import com.operaboys.cinemashotgenerator.domain.dna.GlobalMoodBase
import com.operaboys.cinemashotgenerator.domain.dna.MasterPalette
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.dna.OutputConstraints
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.dna.RealismLevel
import com.operaboys.cinemashotgenerator.domain.dna.SaturationLevel
import com.operaboys.cinemashotgenerator.domain.dna.StyleConsistency
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
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
import com.operaboys.cinemashotgenerator.domain.validation.ValidationLevel
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// رفع یافته‌ی معماری «Human Override هرگز به هیچ اقدام واقعی کاربر وصل نشده»
// (G3/ADR-067، ADR-068) — تست End-to-End نقطه‌ی ورود UI روی صفحه‌ی Validation.
// هم‌الگو با DnaSoftLockWarningTest.kt: Mount مستقیم خودِ ValidationScreen (نه کل
// MainScaffold/Navigation) چون فقط رفتار مستقل این صفحه با یک شات از‌پیش Seed‌شده
// بررسی می‌شود، نه ناوبری کامل Studio.
//
// فیکسچر شات عمداً «خنثیِ کامل» است (هم‌الگو دقیق با neutralShot/neutralScene/
// neutralDna در ValidationAggregatorTest.kt) به‌جز negativePromptOverride="   "
// (فقط فاصله‌ی خالی) — طبق validateNegativePromptOverride (واحد ۰۵)، این دقیقاً
// یک WARNING تولید می‌کند (نه BLOCKING)، پس دکمه‌ی «تجاوز از این هشدار» باید
// نمایش داده شود و قابل کلیک باشد.

private const val PROJECT_ID = "proj_validation_override_flow_test"
private const val SCENE_ID = "scene_validation_override_flow_test"
private const val SHOT_ID = "shot_validation_override_flow_test"

private fun neutralCamera(): CameraSettings = CameraSettings(
    angle = CameraAngle.EYE_LEVEL,
    distance = CameraDistance.MEDIUM,
    movement = CameraMovement.Basic(BasicMovementType.STATIC),
    lensType = LensType.STANDARD,
    depthOfField = DepthOfField.MEDIUM,
    focusMode = FocusMode.AUTO,
    stabilization = Stabilization.TRIPOD,
    framing = Framing.RULE_OF_THIRDS
)

private fun neutralLighting(): LightingSettings = LightingSettings(
    style = LightingStyle.NATURAL_LIGHT,
    keyLightPosition = KeyLightPosition.SIDE,
    contrastRatio = ContrastRatio.MEDIUM,
    lightingMotivation = LightingMotivation.ARTIFICIAL
)

private fun neutralEnvironment(): EnvironmentSettings = EnvironmentSettings(weatherType = WeatherType.CLEAR)

/** طبق validateNegativePromptOverride (واحد ۰۵) — دقیقاً یک WARNING در سطح ۱. */
private val seededShot = Shot(
    shotId = SHOT_ID,
    sceneId = SCENE_ID,
    shotNumber = 1,
    shotDescription = "A perfectly neutral, valid shot description",
    shotGoal = ShotGoal.ESTABLISHING,
    shotType = ShotType.MEDIUM,
    durationSeconds = 4f,
    motionLevel = MotionLevel.SUBTLE,
    camera = SourcedSettings(source = "override", overrideValue = neutralCamera()),
    lighting = SourcedSettings(source = "override", overrideValue = neutralLighting()),
    environment = SourcedSettings(source = "override", overrideValue = neutralEnvironment()),
    soundProfile = SoundProfile(enabled = false),
    characterIds = listOf("char_001"),
    negativePromptOverride = "   "
)

private val seededScene = Scene(
    sceneId = SCENE_ID,
    sceneNumber = 1,
    narrativeRole = NarrativeRole.DEVELOPMENT,
    location = SceneLocation(LocationType.MIXED, "A neutral location"),
    timeOfDay = TimeOfDay.AFTERNOON,
    atmospherePrimary = Atmosphere.CALM,
    shotCount = 1
)

private fun seededDna(): ProjectDna = ProjectDna(
    dnaId = "dna_validation_override_flow_test",
    projectId = PROJECT_ID,
    coreIdentity = CoreIdentity(VisualStyle.CINEMATIC_STYLE, RealismLevel.SEMI_REALISTIC, StyleConsistency.MODERATE),
    masterPalette = MasterPalette(ColorTemperature.NEUTRAL, SaturationLevel.MEDIUM, ContrastLevel.MEDIUM, ""),
    outputConstraints = OutputConstraints(emptyMap(), emptyList(), 10, AspectRatio.LANDSCAPE_16_9),
    globalMoodBase = GlobalMoodBase(Mood.CALM)
)

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ValidationOverrideFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    private val level1IssueCardTag = validationIssueCardTag(ValidationLevel.DATA_COMPLETENESS, 0)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val sceneRepository = SceneRepository(database.sceneDao())
        val shotRepository = ShotRepository(database.shotDao())
        val projectDnaRepository = ProjectDnaRepository(database.projectDnaDao())
        val assetRepository = AssetRepository(database.assetDao())

        runBlocking {
            ProjectRepository(database.projectDao(), idProvider = { PROJECT_ID }).createProject("Validation Override Flow Test").getOrThrow()
            projectDnaRepository.saveProjectDna(seededDna())
            sceneRepository.saveScene(PROJECT_ID, seededScene).getOrThrow()
            shotRepository.saveShot(seededShot).getOrThrow()
        }

        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                ValidationScreen(
                    projectId = PROJECT_ID,
                    sceneId = SCENE_ID,
                    shotId = SHOT_ID,
                    language = Language.FA,
                    onBack = {},
                    shotRepository = shotRepository,
                    sceneRepository = sceneRepository,
                    projectDnaRepository = projectDnaRepository,
                    assetRepository = assetRepository,
                    database = database
                )
            }
        }
    }

    @After
    fun tearDown() {
        composeRule.waitForIdle()
        // database.close() عمداً حذف شد — ریشه‌ی واقعی Flake تاریخی این Suite
        // (docs/adr/070-flake-root-cause-investigation.md، رفع در ADR-071):
        // Room.inMemoryDatabaseBuilder نیازی به Close صریح ندارد (بدون فایل روی
        // دیسک، GC آن را با نابودی نمونه‌ی این کلاس تست جمع می‌کند)؛ این خط قبلاً
        // با Coroutine های ناتمام viewModelScope روی Executor داخلی Room مسابقه
        // می‌داد — نه یک نشتی حافظه‌ی فراموش‌شده. waitForIdle() بالا همچنان برای
        // Idling خودِ Compose مفید است، فقط دیگر ایمنی close() را تضمین نمی‌کند.
    }

    @Test
    fun `overriding a WARNING issue persists it, marks the issue Overridden, and revoking restores the Override button`() {
        composeRule.waitUntilExactlyOneExists(hasTestTag(VALIDATION_WARNING_COUNT_CARD_TAG), timeoutMillis = 15_000)
        composeRule.waitUntilExactlyOneExists(hasTestTag("$level1IssueCardTag.overrideButton"), timeoutMillis = 15_000)

        composeRule.onNodeWithTag("$level1IssueCardTag.overrideButton").performClick()

        composeRule.waitUntilExactlyOneExists(hasTestTag("validation.overrideDialog.reasonField"), timeoutMillis = 5_000)
        composeRule.onNodeWithTag("validation.overrideDialog.reasonField").performTextInput("تصمیم آگاهانه‌ی کارگردان")
        composeRule.onNodeWithTag("validation.overrideDialog.confirmButton").performClick()

        // یافته‌ی واقعی این تست: createOverrideForIssue یک عملیات Async واقعی
        // (Room suspend DAO) است — منتظر ظاهرشدن واقعی بج «Override شده» می‌ماند
        // (نه یک delay ثابت)، دقیقاً همان الگوی انتظار مبتنی‌بر Semantics که در کل
        // این پروژه استفاده شده.
        composeRule.waitUntilExactlyOneExists(hasTestTag("$level1IssueCardTag.overriddenBadge"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("$level1IssueCardTag.overriddenBadge").assertIsDisplayed()
        composeRule.onNodeWithTag("$level1IssueCardTag.revokeButton").assertIsDisplayed()

        // بازیابی واقعی روی Room: نمونه‌ای مستقل از HumanOverrideRepository باید
        // دقیقاً همان Override را برای این shotId ببیند.
        runBlocking {
            val overrides = HumanOverrideRepository(database.overrideDao()).loadActiveOverridesForEntity(SHOT_ID).getOrThrow()
            assertEquals(1, overrides.size)
            assertEquals("تصمیم آگاهانه‌ی کارگردان", overrides.first().reason)
        }

        composeRule.onNodeWithTag("$level1IssueCardTag.revokeButton").performClick()
        composeRule.waitUntilExactlyOneExists(hasTestTag("validation.revokeDialog.reasonField"), timeoutMillis = 5_000)
        composeRule.onNodeWithTag("validation.revokeDialog.reasonField").performTextInput("دیگر لازم نیست")
        composeRule.onNodeWithTag("validation.revokeDialog.confirmButton").performClick()

        composeRule.waitUntilExactlyOneExists(hasTestTag("$level1IssueCardTag.overrideButton"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("$level1IssueCardTag.overrideButton").assertIsDisplayed()
    }
}
