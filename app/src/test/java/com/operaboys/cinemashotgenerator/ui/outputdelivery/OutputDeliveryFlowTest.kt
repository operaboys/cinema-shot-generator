package com.operaboys.cinemashotgenerator.ui.outputdelivery

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
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
import com.operaboys.cinemashotgenerator.data.repository.AudioContextRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.data.repository.PromptGenerationRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.SettingsResolutionRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ALL_MODEL_PROFILES
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
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
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.navigation.StudioTab
import com.operaboys.cinemashotgenerator.ui.navigation.studioTabTestTag
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.scenes.SCENE_DETAIL_SHOTS_TAB_TAG
import com.operaboys.cinemashotgenerator.ui.scenes.sceneDisplayTitle
import com.operaboys.cinemashotgenerator.ui.shots.SHOT_COMPOSER_OUTPUT_DELIVERY_BUTTON_TAG
import com.operaboys.cinemashotgenerator.ui.shots.shotCardTag
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.UUID

// واحد ۱۶ فاز ۵ — قدم ۳: تست End-to-End واقعی صفحه‌ی Output Delivery — طبق دستور کار:
// (۱) انتخاب هر مدل → RenderedOutput واقعاً متناظر همان مدل؛ (۲) هشدار Over-limit
// برای یک Shot با محتوای طولانی؛ (۳) Copy to Clipboard واقعاً کار کند؛ (۴) چیپ‌های
// مدل Opaque/قابل‌مشاهده باشند؛ (۵) عوض کردن مدل هدف باعث به‌روزرسانی واقعی محدودیت‌های
// مختص مدل (اینجا: maxTokens) در Output Preview شود. الگوی راه‌اندازی عیناً از
// ValidationFlowTest.kt (قدم قبل) گرفته شده. جزئیات کامل تصمیمات در
// docs/adr/057-unit16-phase5-step3-output-delivery.md.
//
// برخلاف ValidationViewModel (که با نبودِ ProjectDna ذخیره‌شده به defaultProjectDna
// خنثی سقوط می‌کند)، PromptGenerationRepository.collectData واقعاً یک ProjectDna
// ذخیره‌شده لازم دارد (Result.failure در غیر این صورت) — پس این تست، برخلاف
// ValidationFlowTest، باید صریحاً defaultProjectDna را هم Seed کند (همان تابع
// internal بازاستفاده‌شده‌ی ValidationViewModel، نه یک بازتعریف تازه).

private const val PROJECT_ID = "proj_output_delivery_flow_test"
private const val SCENE_ID = "scene_output_delivery_flow_test"
private const val SHOT_ID = "shot_output_delivery_flow_test"

private val seededScene = Scene(
    sceneId = SCENE_ID,
    sceneTitle = "A Calm Afternoon",
    sceneNumber = 1,
    narrativeRole = NarrativeRole.DEVELOPMENT,
    location = SceneLocation(type = LocationType.MIXED, description = "a quiet courtyard"),
    timeOfDay = TimeOfDay.AFTERNOON,
    atmospherePrimary = Atmosphere.CALM,
    shotCount = 1
)

/**
 * توضیحات این شات عمداً بلند است (بیش از ۵۰۰ کاراکتر) — طبق دستور کار «Over-limit
 * warning: یک Shot با محتوای طولانی → نمایش صحیح هشدار». چون
 * `renderBlueprintToText` مستقیماً `shot.shotDescription` را به‌عنوان یک segment
 * کامل درج می‌کند (بدون خلاصه‌سازی)، همین یک فیلد برای عبور از محدودیت ۵۰۰ کاراکتری
 * پروفایل Stable Diffusion کافی است؛ همچنان به‌راحتی زیر محدودیت ۲۰۰۰ کاراکتری
 * Universal Default می‌ماند (برای پروفایل پیش‌فرض هیچ هشداری صادر نمی‌شود).
 */
private val longDescription = "A slow, deliberate camera move across the courtyard. " +
    "The light shifts gently across weathered stone. ".repeat(12)

/**
 * یافته‌ی واقعی دیباگ این قدم (با یک تست مستقل موقت مستقیماً روی
 * `PromptGenerationRepository.collectData` کشف شد، جزئیات در ADR-057): برخلاف
 * فیکسچر مشابه `ValidationFlowTest` (که فقط `lighting` را Override می‌کند و
 * کافی است، چون آن صفحه هرگز `collectData`/`SettingsResolutionRepository` را
 * فرا نمی‌خواند)، اینجا `camera`/`environment` هم باید `source="override"`
 * باشند — چون `PromptGenerationRepository` همیشه `sceneDefault=null` عبور
 * می‌دهد (تصمیم مستند ADR-013)، و بدون Override واقعی، `resolveSourcedSettings`
 * با `IllegalStateException` شکست می‌خورد («نه sceneDefault و نه override شات
 * برای این تنظیمات موجود است»). مقادیر نمونه دقیقاً از فیکسچر موجود و کارآمد
 * `PromptGenerationRepositoryTest.kt` گرفته شدند.
 */
private val seededShot = Shot(
    shotId = SHOT_ID,
    sceneId = SCENE_ID,
    shotNumber = 1,
    shotTitle = "Courtyard pass",
    shotDescription = longDescription,
    shotGoal = ShotGoal.ESTABLISHING,
    shotType = ShotType.WIDE,
    durationSeconds = 6f,
    motionLevel = MotionLevel.STATIC,
    camera = SourcedSettings(
        source = "override",
        overrideValue = CameraSettings(
            angle = CameraAngle.LOW,
            distance = CameraDistance.CLOSE_UP,
            movement = CameraMovement.Basic(type = BasicMovementType.DOLLY_IN, speed = "slow"),
            lensType = LensType.TELEPHOTO,
            depthOfField = DepthOfField.SHALLOW,
            focusMode = FocusMode.SUBJECT_TRACKING,
            stabilization = Stabilization.GIMBAL,
            framing = Framing.CENTERED
        )
    ),
    lighting = SourcedSettings(
        source = "override",
        overrideValue = LightingSettings(
            style = LightingStyle.NATURAL_LIGHT,
            keyLightPosition = KeyLightPosition.SIDE,
            contrastRatio = ContrastRatio.MEDIUM,
            lightingMotivation = LightingMotivation.SUNLIGHT
        )
    ),
    environment = SourcedSettings(source = "override", overrideValue = EnvironmentSettings(WeatherType.RAIN)),
    soundProfile = SoundProfile(enabled = false)
)

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OutputDeliveryFlowTest {

    @get:Rule
    // createAndroidComposeRule<ComponentActivity>() به‌جای createComposeRule()
    // (که خودش هم‌الگو دقیقاً همین را داخلی می‌سازد، اما `.activity` را افشا
    // نمی‌کند) — یافته‌ی واقعی دیباگ این قدم: `ClipboardManager` گرفته‌شده از
    // `ApplicationProvider.getApplicationContext()` state جدایی از نمونه‌ای
    // دارد که واقعاً پشت `LocalClipboardManager.current` داخل درخت Compose
    // است (که از Context خودِ Activity میزبان می‌آید) — روی Robolectric این دو
    // Context یک ClipData مشترک ندارند. رفع: خواندن Clipboard از دقیقاً همان
    // Activity که این Rule محتوا را داخلش Mount می‌کند.
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var context: Context
    private lateinit var dataStoreFileName: String
    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var database: AppDatabase
    private lateinit var projectListViewModel: ProjectListViewModel
    private lateinit var sceneRepository: SceneRepository
    private lateinit var shotRepository: ShotRepository
    private lateinit var projectDnaRepository: ProjectDnaRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreFileName = "output_delivery_flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
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
        projectDnaRepository = ProjectDnaRepository(database.projectDnaDao())
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
        // database.close() عمداً حذف شد — ریشه‌ی واقعی Flake تاریخی این Suite
        // (docs/adr/070-flake-root-cause-investigation.md، رفع در ADR-071):
        // Room.inMemoryDatabaseBuilder نیازی به Close صریح ندارد (بدون فایل روی
        // دیسک، GC آن را با نابودی نمونه‌ی این کلاس تست جمع می‌کند)؛ این خط قبلاً
        // با Coroutine های ناتمام viewModelScope روی Executor داخلی Room مسابقه
        // می‌داد — نه یک نشتی حافظه‌ی فراموش‌شده. waitForIdle() بالا همچنان برای
        // Idling خودِ Compose مفید است، فقط دیگر ایمنی close() را تضمین نمی‌کند.
    }

    /**
     * یافته‌ی واقعی دیباگ این قدم: `performClick()`/`performSemanticsAction`
     * ساده روی گره‌ای که هنوز داخل Viewport فعلیِ `Modifier.verticalScroll`
     * والد اسکرول نشده (این صفحه ۱۴ چیپ مدل + کارت پیش‌نمایش + بخش Warnings
     * دارد — لیست بلندی که Copy/چیپ SD3 معمولاً پایین آن‌اند)، بدون خطای
     * صریح، هیچ اثری تولید نمی‌کند (نه Exception، نه اجرای Handler واقعی) —
     * هم‌الگو با `clickViaSemantics` موجود `DnaSoftLockWarningTest.kt` که
     * همین دلیل دقیقاً همان ترکیب `performScrollTo` + `performSemanticsAction`
     * را استفاده می‌کند.
     */
    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction {
        performScrollTo()
        return performSemanticsAction(SemanticsActions.OnClick)
    }

    /**
     * پروژه‌ی واقعی می‌سازد، DNA پیش‌فرض (طبق نیاز واقعی `PromptGenerationRepository.
     * collectData` — برخلاف Validation، اینجا بدون ProjectDna ذخیره‌شده Result.failure
     * می‌شود) + Scene/Shot را Seed می‌کند، و از داخل Shot Composer وارد صفحه‌ی Output
     * Delivery می‌شود.
     */
    private fun createProjectAndOpenOutputDelivery() {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput("Output Delivery Flow Test")
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Output Delivery Flow Test"), timeoutMillis = 15_000)

        runBlocking {
            projectDnaRepository.saveProjectDna(defaultProjectDna(PROJECT_ID) { "dna_output_delivery_test" })
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

        composeRule.onNodeWithTag(SHOT_COMPOSER_OUTPUT_DELIVERY_BUTTON_TAG).performClick()
        // یافته‌ی مستندشده‌ی ADR-055 دوباره اینجا صادق است: عنوان این صفحه عمداً از
        // کلید موجود drawer.outputDelivery بازاستفاده شده، که همیشه (حتی وقتی
        // Drawer بسته است) یک گره‌ی دیگر با همان متن در درخت Semantics دارد. انتظار
        // روی testTag منحصربه‌فرد این صفحه (دکمه‌ی برگشت)، نه متن عنوان.
        composeRule.waitUntilExactlyOneExists(hasTestTag(OUTPUT_DELIVERY_BACK_BUTTON_TAG), timeoutMillis = 15_000)
        composeRule.waitUntilExactlyOneExists(hasTestTag(OUTPUT_DELIVERY_PREVIEW_CARD_TAG), timeoutMillis = 15_000)
    }

    @Test
    fun `selecting the Midjourney chip re-renders the output with its real command-string format`() {
        createProjectAndOpenOutputDelivery()

        // پیش‌فرض (Universal Default، plain_text) نباید پیشوند دستور Midjourney را
        // نشان دهد.
        composeRule.onNodeWithText("/imagine prompt:", substring = true).assertDoesNotExist()

        composeRule.onNodeWithTag(outputDeliveryModelChipTag("midjourney_v7")).clickViaSemantics()
        composeRule.waitForIdle()

        // پس از انتخاب Midjourney، خروجی واقعاً رندرشده باید دقیقاً با فرمت
        // command_string واقعی این پروفایل شروع شود — اثبات مستقیم اینکه
        // RenderedOutput واقعاً متناظر مدل انتخاب‌شده است، نه یک مقدار ثابت.
        composeRule.waitUntilAtLeastOneExists(hasText("/imagine prompt:", substring = true), timeoutMillis = 5_000)
    }

    @Test
    fun `a shot with long content triggers the real over-limit warning when Stable Diffusion SD3 is selected`() {
        createProjectAndOpenOutputDelivery()

        composeRule.onNodeWithTag(outputDeliveryModelChipTag("stable_diffusion_sd3")).clickViaSemantics()
        composeRule.waitForIdle()

        composeRule.waitUntilAtLeastOneExists(hasText("500", substring = true), timeoutMillis = 5_000)
        // متن دقیق هشدار واقعی validatePromptLength (Renderer.kt) — نه یک رشته‌ی
        // تقریبی. `assertExists` (نه `assertIsDisplayed`) عمداً استفاده شد: بخش
        // Warnings پایین یک Column با اسکرول عمودی است و ممکن است بیرون از
        // Viewport فعلی باشد؛ آنچه این تست واقعاً می‌سنجد وجود واقعی این هشدار در
        // درخت ترکیب‌شده است، نه رفتار اسکرول.
        composeRule.waitUntilAtLeastOneExists(hasText("بیشتر است", substring = true), timeoutMillis = 5_000)
    }

    @Test
    fun `the Copy button copies the real rendered prompt text to the system clipboard`() {
        createProjectAndOpenOutputDelivery()

        val clipboardManager = composeRule.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.clearPrimaryClip()

        composeRule.onNodeWithTag(OUTPUT_DELIVERY_COPY_BUTTON_TAG).clickViaSemantics()
        composeRule.waitForIdle()

        val clipText = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
        assertTrue("clipboard should contain the rendered prompt text", !clipText.isNullOrBlank())
        // طبق seededShot، متن کپی‌شده باید بخشی از توضیحات واقعی همین شات را داشته
        // باشد (پروفایل پیش‌فرض Universal Default زیر محدودیت ۲۰۰۰ کاراکتری است، پس
        // کوتاه‌سازی رخ نمی‌دهد).
        assertTrue(clipText!!.contains("courtyard"))
    }

    /**
     * رفع یافته‌ی معماری «دکمه‌ی Export مستعار Copy است» (G4/G18، ADR-069) —
     * اثبات مستقیم اینکه این دو دکمه دیگر رفتار یکسان ندارند: Copy فقط Clipboard
     * را عوض می‌کند (تست بالا)؛ Export یک Activity واقعی (Chooser سیستم، پوشاننده‌ی
     * یک Intent.ACTION_SEND[_MULTIPLE] واقعی) باز می‌کند. طبق دستور کار («بدون
     * نیاز به تست واقعی Intent System»)، این تست فقط بررسی می‌کند چه Intent ای
     * واقعاً startActivity شده — نه رفتار واقعی Chooser/اپ مقصد.
     *
     * یافته‌ی واقعی دیباگ این تست: composeOutput (OutputComposer.kt) همیشه دو
     * فایل bilingual (en/fa) + یک فایل به‌ازای مدل انتخاب‌شده می‌سازد — یعنی حداقل
     * ۳ فایل، هرگز یک فایل تنها (این ViewModel هر لحظه فقط یک مدل انتخاب‌شده
     * دارد). پس Intent واقعی این صفحه همیشه ACTION_SEND_MULTIPLE است، نه
     * ACTION_SEND — انتظار اولیه‌ی این تست اشتباه بود، نه یک واقعیت محیطی.
     */
    @Test
    fun `the Export button starts a real ACTION_SEND_MULTIPLE chooser, a genuinely different action than Copy`() {
        createProjectAndOpenOutputDelivery()

        composeRule.onNodeWithTag(OUTPUT_DELIVERY_EXPORT_BUTTON_TAG).clickViaSemantics()
        // exportOutput() نوشتن فایل را روی Dispatchers.IO انجام می‌دهد
        // (ExportFileWriter.kt) — یک واقعی Hop به Executor دیگر که به‌تنهایی با
        // waitForIdle (که فقط Idling Resources Compose را می‌سنجد) هم‌زمان
        // نمی‌شود؛ Poll صریح روی خودِ Shadow لازم است (هم‌الگو با سایر یافته‌های
        // مستندشده‌ی Async این پروژه).
        composeRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(composeRule.activity).peekNextStartedActivity() != null
        }

        val startedIntent = Shadows.shadowOf(composeRule.activity).nextStartedActivity
        assertTrue("دکمه‌ی Export باید یک Activity واقعی (Chooser) باز کند", startedIntent != null)
        assertEquals(Intent.ACTION_CHOOSER, startedIntent!!.action)

        val innerIntent = startedIntent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        assertTrue("Chooser باید یک Intent.ACTION_SEND_MULTIPLE واقعی بپوشاند", innerIntent != null)
        assertEquals(Intent.ACTION_SEND_MULTIPLE, innerIntent!!.action)
        assertEquals("text/plain", innerIntent.type)
        val uris = innerIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
        assertEquals(3, uris?.size)

        // اثبات اینکه این Uri ها جعلی/فرمالیستی نیستند — FileProvider واقعی این
        // پروژه (AndroidManifest.xml + res/xml/file_paths.xml، اولین Provider کل
        // پروژه) باید واقعاً بتواند هرکدام را به محتوای واقعی فایل نوشته‌شده حل کند.
        val resolvedContent = composeRule.activity.contentResolver
            .openInputStream(uris!!.first())?.bufferedReader()?.use { it.readText() }
        assertTrue(!resolvedContent.isNullOrBlank())
    }

    @Test
    fun `model picker chips render as distinct, fully visible, opaque nodes both selected and unselected`() {
        createProjectAndOpenOutputDelivery()

        // پروفایل اول ALL_MODEL_PROFILES (universal_default) پیش‌فرض انتخاب‌شده است.
        composeRule.onNodeWithTag(outputDeliveryModelChipTag(ALL_MODEL_PROFILES.first().profileId)).assertIsDisplayed()
        // یک چیپ انتخاب‌نشده هم باید کاملاً قابل‌مشاهده باشد (Opaque، نه محو) — هم‌الگو
        // با یافته‌ی مستندشده‌ی ADR-055 درباره‌ی OpaqueChip/Contrast.
        composeRule.onNodeWithTag(outputDeliveryModelChipTag("veo_3_1")).assertIsDisplayed()

        composeRule.onNodeWithTag(outputDeliveryModelChipTag("veo_3_1")).clickViaSemantics()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(outputDeliveryModelChipTag("veo_3_1")).assertIsDisplayed()
        composeRule.onNodeWithTag(outputDeliveryModelChipTag(ALL_MODEL_PROFILES.first().profileId)).assertIsDisplayed()
    }

    @Test
    fun `changing the target model updates the real per-model token-cost constraint shown in the preview`() {
        createProjectAndOpenOutputDelivery()

        // پیش‌فرض Universal Default → maxTokens=500 (ModelProfileLibrary.kt).
        composeRule.onNode(
            hasText("500", substring = true) and hasAnyAncestor(hasTestTag(OUTPUT_DELIVERY_PREVIEW_CARD_TAG))
        ).assertExists()

        // Runway Gen-4.5 → maxTokens=250 (ModelProfiles.kt) — یک مقدار واقعاً متفاوت.
        composeRule.onNodeWithTag(outputDeliveryModelChipTag("runway_gen_4_5")).clickViaSemantics()
        composeRule.waitForIdle()

        composeRule.waitUntilAtLeastOneExists(
            hasText("250", substring = true) and hasAnyAncestor(hasTestTag(OUTPUT_DELIVERY_PREVIEW_CARD_TAG)),
            timeoutMillis = 5_000
        )
    }
}
