package com.operaboys.cinemashotgenerator.ui.outputdelivery

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.AudioContextRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.data.repository.PromptGenerationRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.SecureKeyRepository
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
import com.operaboys.cinemashotgenerator.data.repository.ExportFileWriter
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ALL_MODEL_PROFILES
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ExportFile
import com.operaboys.cinemashotgenerator.domain.storybreakdown.CLAUDE_API_PROFILE
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
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.shot.SourcedSettings
import com.operaboys.cinemashotgenerator.ui.dna.defaultProjectDna
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

// واحد ۱۶ — رفع یافته‌ی 🔴 G17 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md):
// تست مستقیم OutputDeliveryViewModel (نه از طریق کل Compose Navigation Tree مثل
// OutputDeliveryFlowTest.kt — این تست فقط زنجیره‌ی regenerate()/finalizePrompt را
// می‌سنجد، پس ioScopeOverride=Dispatchers.Unconfined + ساخت مستقیم ViewModel سریع‌تر
// و مستقیم‌تر است). جزئیات کامل تصمیمات در docs/adr/060-...-output-delivery-cleaning-and-confirmation-dialogs.md.

private const val PROJECT_ID = "proj_output_delivery_vm_test"
private const val SCENE_ID = "scene_output_delivery_vm_test"
private const val SHOT_ID = "shot_output_delivery_vm_test"

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

/** هم‌الگو با camera/lighting/environment فیکسچر موجود OutputDeliveryFlowTest.kt — هر
 * سه باید source="override" باشند (طبق یافته‌ی مستندشده‌ی ADR-057). */
private fun buildShot(shotId: String, description: String) = Shot(
    shotId = shotId,
    sceneId = SCENE_ID,
    shotNumber = 1,
    shotTitle = "Courtyard pass",
    shotDescription = description,
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OutputDeliveryViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var sceneRepository: SceneRepository
    private lateinit var shotRepository: ShotRepository
    private lateinit var projectDnaRepository: ProjectDnaRepository
    private lateinit var promptGenerationRepository: PromptGenerationRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        sceneRepository = SceneRepository(database.sceneDao())
        shotRepository = ShotRepository(database.shotDao())
        val assetRepository = AssetRepository(database.assetDao())
        projectDnaRepository = ProjectDnaRepository(database.projectDnaDao())
        promptGenerationRepository = PromptGenerationRepository(
            database.shotDao(),
            database.sceneDao(),
            projectDnaRepository,
            assetRepository,
            SettingsResolutionRepository(database.shotDao(), database.sceneDao()),
            AudioContextRepository(database.audioContextDao())
        )
        // یافته‌ی واقعی دیباگ این تست: SceneEntity یک ForeignKey واقعی به ProjectEntity
        // دارد (data/entity/SceneEntity.kt) — بدون ساخت واقعی یک Project اول،
        // sceneRepository.saveScene بی‌صدا با SQLiteConstraintException شکست
        // می‌خورد (چون Result برگشتی‌اش چک نشده بود)، که به نوبه‌ی خودش باعث
        // می‌شد ShotEntity (با FK به همان Scene) هم بی‌صدا ذخیره نشود — دقیقاً
        // همان علت واقعی خطای «شات یافت نشد» در collectData.
        runBlocking {
            ProjectRepository(database.projectDao(), idProvider = { PROJECT_ID })
                .createProject("Output Delivery VM Test").getOrThrow()
            projectDnaRepository.saveProjectDna(defaultProjectDna(PROJECT_ID) { "dna_output_delivery_vm_test" })
            sceneRepository.saveScene(PROJECT_ID, seededScene).getOrThrow()
        }
    }

    @After
    fun tearDown() {
        // database.close() عمداً حذف شد — ریشه‌ی واقعی Flake تاریخی این Suite
        // (docs/adr/070-flake-root-cause-investigation.md، رفع در ADR-071):
        // Room.inMemoryDatabaseBuilder نیازی به Close صریح ندارد (بدون فایل روی
        // دیسک، GC آن را با نابودی نمونه‌ی این کلاس تست جمع می‌کند)؛ این خط قبلاً
        // با Coroutine های ناتمام viewModelScope روی Executor داخلی Room مسابقه
        // می‌داد — نه یک نشتی حافظه‌ی فراموش‌شده.
    }

    // هوشمندسازی و اتصال evaluatePromptQuality — قدم ۳ از ۳ زیرقدم (ADR-120):
    // دو پارامتر تازه، با همان مقادیر پیش‌فرض واقعی خودِ OutputDeliveryViewModel
    // (SecureKeyRepository(application)/OkHttp.create()) — تست‌های موجود بدون
    // تغییر رفتار همچنان کار می‌کنند (apiKeySavedForQualityAnalysis فقط false
    // می‌ماند، بدون Crash، طبق runCatching مستندشده‌ی ViewModel).
    private fun buildViewModel(
        shotId: String,
        initialModelProfileId: String?,
        exportFileWriter: ExportFileWriter = FakeExportFileWriter(),
        secureKeyRepository: SecureKeyRepository = SecureKeyRepository(ApplicationProvider.getApplicationContext()),
        httpClientEngine: HttpClientEngine = OkHttp.create()
    ): OutputDeliveryViewModel {
        val application = ApplicationProvider.getApplicationContext<Application>()
        return OutputDeliveryViewModel(
            application = application,
            shotId = shotId,
            initialModelProfileId = initialModelProfileId,
            promptGenerationRepository = promptGenerationRepository,
            exportFileWriter = exportFileWriter,
            secureKeyRepository = secureKeyRepository,
            httpClientEngine = httpClientEngine,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    private fun buildTestSecureKeyRepository(prefsName: String): SecureKeyRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return SecureKeyRepository(context) { appContext -> appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE) }
    }

    private fun <T> awaitCondition(flow: StateFlow<T>, timeoutMs: Long = 3000, predicate: (T) -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate(flow.value) && System.currentTimeMillis() < deadline) {
            Thread.sleep(5)
        }
        assertTrue("condition was never met within ${timeoutMs}ms, last value=${flow.value}", predicate(flow.value))
    }

    // هوشمندسازی و اتصال evaluatePromptQuality — قدم ۳ از ۳ زیرقدم (ADR-120):
    // سه تست امنیتی این قدم. هم‌الگو با AiStoryBreakdownViewModelTest.kt
    // (`sendPromptAutomatically without a saved key...`/`a successful automatic
    // send...`/`a failed automatic send...`). مهم‌ترین‌شان تست اول است: اثبات
    // قطعی که بدون کلید ذخیره‌شده، هیچ HTTP واقعی رخ نمی‌دهد — نه فقط این‌که
    // دکمه در UI غیرفعال به نظر می‌رسد.

    @Test
    fun `analyzePromptQualityWithAi without a saved key never attempts a real HTTP call and sets a meaningful error`() = runBlocking {
        val shotId = "${SHOT_ID}_ai_quality_no_key"
        shotRepository.saveShot(buildShot(shotId, "A calm establishing shot of the courtyard.")).getOrThrow()

        val secureKeyRepository = buildTestSecureKeyRepository("ai_quality_vm_test_no_key_prefs")
        var engineCalled = false
        val engine = MockEngine {
            engineCalled = true
            respond(content = "{}", status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val viewModel = buildViewModel(
            shotId,
            initialModelProfileId = "universal_default",
            secureKeyRepository = secureKeyRepository,
            httpClientEngine = engine
        )
        viewModel.regenerate().join()
        assertFalse(viewModel.apiKeySavedForQualityAnalysis.value)

        viewModel.analyzePromptQualityWithAi().join()

        assertFalse(
            "بدون کلید ذخیره‌شده نباید هیچ تلاش HTTP واقعی برای تحلیل عمیق‌تر AI انجام شود — این مهم‌ترین تست امنیتی این قدم است",
            engineCalled
        )
        assertNotNull(viewModel.qualityAnalysisError.value)
        assertEquals(null, viewModel.qualityAnalysisResult.value)
    }

    @Test
    fun `analyzePromptQualityWithAi with a saved key and a successful response populates qualityAnalysisResult`() = runBlocking {
        val shotId = "${SHOT_ID}_ai_quality_success"
        shotRepository.saveShot(buildShot(shotId, "A calm establishing shot of the courtyard.")).getOrThrow()

        val secureKeyRepository = buildTestSecureKeyRepository("ai_quality_vm_test_success_prefs")
        secureKeyRepository.saveApiKey(CLAUDE_API_PROFILE.profileId, "sk-ant-real-key")
        val engine = MockEngine {
            respond(
                content = """{"content":[{"type":"text","text":"Strong subject clarity, but the lighting description is generic."}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val viewModel = buildViewModel(
            shotId,
            initialModelProfileId = "universal_default",
            secureKeyRepository = secureKeyRepository,
            httpClientEngine = engine
        )
        viewModel.regenerate().join()
        awaitCondition(viewModel.apiKeySavedForQualityAnalysis) { it }

        viewModel.analyzePromptQualityWithAi().join()

        assertEquals(
            "Strong subject clarity, but the lighting description is generic.",
            viewModel.qualityAnalysisResult.value
        )
        assertEquals(null, viewModel.qualityAnalysisError.value)
    }

    @Test
    fun `analyzePromptQualityWithAi with a saved key but a failing HTTP response sets a meaningful qualityAnalysisError`() = runBlocking {
        val shotId = "${SHOT_ID}_ai_quality_failure"
        shotRepository.saveShot(buildShot(shotId, "A calm establishing shot of the courtyard.")).getOrThrow()

        val secureKeyRepository = buildTestSecureKeyRepository("ai_quality_vm_test_failure_prefs")
        secureKeyRepository.saveApiKey(CLAUDE_API_PROFILE.profileId, "sk-ant-real-key")
        val engine = MockEngine {
            respond(
                content = """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val viewModel = buildViewModel(
            shotId,
            initialModelProfileId = "universal_default",
            secureKeyRepository = secureKeyRepository,
            httpClientEngine = engine
        )
        viewModel.regenerate().join()
        awaitCondition(viewModel.apiKeySavedForQualityAnalysis) { it }

        viewModel.analyzePromptQualityWithAi().join()

        assertEquals(null, viewModel.qualityAnalysisResult.value)
        assertNotNull(viewModel.qualityAnalysisError.value)
        assertTrue(viewModel.qualityAnalysisError.value!!.contains("invalid x-api-key"))
    }

    @Test
    fun `regenerate resolves a real weather conflict via the Unit 13 cleaning pipeline and shows the Cleaned badge state`() = runBlocking {
        val shotId = "${SHOT_ID}_conflict"
        val description = "A sunny rainy day over the courtyard, camera holds perfectly steady."
        shotRepository.saveShot(buildShot(shotId, description)).getOrThrow()

        val viewModel = buildViewModel(shotId, initialModelProfileId = "universal_default")
        // یافته‌ی واقعی دیباگ این تست: ioScopeOverride=Dispatchers.Unconfined به‌تنهایی
        // تضمین نمی‌کند regenerate() (که init داخلی صدا می‌زند) قبل از خواندن
        // state.value کامل شده باشد — Room DAO های suspend روی Executor داخلی خودش
        // اجرا می‌شوند، نه لزوماً هم‌زمان روی همین Thread. regenerate() اکنون Job
        // برمی‌گرداند (هم‌الگو با WorkflowViewModel.setLanguage/...) دقیقاً برای
        // این‌که تست بتواند .join() کند و مطمئن باشد.
        viewModel.regenerate().join()
        val state = viewModel.state.value as OutputDeliveryState.Ready

        // cleanPrompt (واحد ۱۳) باید "rainy" (بازنده‌ی WEATHER_CONFLICTS) را واقعاً
        // از متن نهایی حذف کرده باشد، درحالی‌که "sunny" باقی می‌ماند — دقیقاً همان
        // چیزی که تا این قدم هرگز اتفاق نمی‌افتاد (بج بدون قید و شرط نمایش داده
        // می‌شد بدون اینکه این Pipeline واقعاً اجرا شود).
        assertFalse(state.renderedOutput.formattedPrompt.contains("rainy", ignoreCase = true))
        assertTrue(state.renderedOutput.formattedPrompt.contains("sunny", ignoreCase = true))
        assertTrue("cleaningSucceeded باید true باشد تا بج Cleaned/Finalized نمایش داده شود", state.cleaningSucceeded)
        assertNotNull(state.tokenCheck)

        // بعد از پاک‌سازی، دیگر هیچ تضاد باقی‌مانده‌ای نباید در warnings به‌عنوان
        // BLOCKING دیده شود (validateConflictsResolved).
        val hasUnresolvedConflictWarning = state.warnings.any { it.message.contains("تضاد حل‌نشده") }
        assertFalse(hasUnresolvedConflictWarning)
    }

    @Test
    fun `regenerate reports a real token over-limit warning for a small-maxTokens model`() = runBlocking {
        val shotId = "${SHOT_ID}_overlimit"
        // یافته‌ی واقعی دیباگ این تست: stable_diffusion_sd3 (فرمت plain_text) اولین
        // انتخاب بود، اما render() برای پروفایل‌های غیر-JSON متن را به‌طور خودکار به
        // maxPromptLength کوتاه می‌کند (Renderer.kt) — و طبق کامنت خودِ ModelProfiles.kt
        // («الگوی maxTokens ≈ maxPromptLength/4»)، متنِ کوتاه‌شده تقریباً همیشه دقیقاً
        // نزدیک سقف توکن همان پروفایل می‌ماند، نه واقعاً بالاتر — یعنی هرچقدر توضیح شات
        // طولانی‌تر باشد، فرقی نمی‌کند. veo_3_1 (فرمت json، طبق ADR-026 هرگز کوتاه
        // نمی‌شود) انتخاب شد تا طول ورودی واقعاً در طول خروجی نهایی اثر کند.
        val description = ("A slow, deliberate camera move across the ancient courtyard while golden light spills " +
            "over the weathered stone walls and distant bells echo through the quiet, misty morning air outside. ").repeat(20)
        shotRepository.saveShot(buildShot(shotId, description)).getOrThrow()

        val profile = ALL_MODEL_PROFILES.first { it.profileId == "veo_3_1" }
        val viewModel = buildViewModel(shotId, initialModelProfileId = profile.profileId)
        viewModel.regenerate().join()
        val state = viewModel.state.value as OutputDeliveryState.Ready

        assertNotNull(state.tokenCheck)
        assertEquals(profile.constraints.maxTokens, state.tokenCheck!!.maxTokens)
        assertFalse("این متن باید واقعاً از سقف توکن این مدل عبور کند", state.tokenCheck!!.withinLimit)
        assertTrue(
            "هشدار واقعی checkTokenLimit باید در warnings دیده شود",
            state.warnings.any { it.message.contains("توکن") && it.message.contains("بیشتر است") }
        )
    }

    /**
     * رفع یافته‌ی معماری «دکمه‌ی Export مستعار Copy است» (G4/G18، ADR-069) —
     * composeOutput واحد ۱۴ واقعاً از اینجا صدا زده می‌شود، نه فقط پیش‌بینی‌شده.
     */
    @Test
    fun `regenerate composes a real OutputPackage with both export files and the honest bilingual fallback`() = runBlocking {
        val shotId = "${SHOT_ID}_compose"
        shotRepository.saveShot(buildShot(shotId, "A calm establishing shot of the courtyard.")).getOrThrow()

        val viewModel = buildViewModel(shotId, initialModelProfileId = "universal_default")
        viewModel.regenerate().join()
        val state = viewModel.state.value as OutputDeliveryState.Ready

        val pkg = state.outputPackage
        assertEquals(shotId, pkg.shotId)
        assertEquals(state.renderedOutput, pkg.renderedOutputs.single())

        // محدودیت شناخته‌شده و آگاهانه (Bilingual.kt، ADR-069): تا زمانی که یک
        // موتور ترجمه‌ی واقعی وجود ندارد، هر دو نسخه دقیقاً همان متن رندرشده‌ی
        // نهایی‌اند — نه یک Placeholder جعلی و نه خالی.
        assertEquals(state.renderedOutput.formattedPrompt, pkg.bilingualPrompts.enVersion)
        assertEquals(state.renderedOutput.formattedPrompt, pkg.bilingualPrompts.faVersion)

        // composeOutput دو فایل bilingual + یک فایل به‌ازای هر RenderedOutput
        // می‌سازد (OutputComposer.kt) — اینجا دقیقاً یک مدل انتخاب شده، پس ۳ فایل.
        assertEquals(3, pkg.exportFiles.size)
        assertTrue(pkg.exportFiles.any { it.filename == "${shotId}_prompt_en.txt" })
        assertTrue(pkg.exportFiles.any { it.filename == "${shotId}_prompt_fa.txt" })
        assertTrue(pkg.exportFiles.any { it.filename == "${shotId}_universal_default.txt" })
        assertTrue(pkg.exportFiles.all { it.mimeType == "text/plain" })
    }

    /**
     * هوشمندسازی و اتصال evaluatePromptQuality — قدم ۲ از ۳ زیرقدم (ADR-119):
     * اثبات اتصال End-to-End واقعی — نه یک تست دوباره روی مقدار عددی دقیق
     * (که قبلاً در WorkflowModelsTest.kt/ADR-108 پوشش داده شده)، بلکه اثبات
     * اینکه regenerate() واقعی این ViewModel واقعاً evaluatePromptQuality را
     * صدا می‌زند و نتیجه‌اش در state.value منعکس می‌شود.
     */
    @Test
    fun `regenerate populates a real, in-range qualityScore in the Ready state`() = runBlocking {
        val shotId = "${SHOT_ID}_quality"
        shotRepository.saveShot(buildShot(shotId, "A calm establishing shot of the courtyard.")).getOrThrow()

        val viewModel = buildViewModel(shotId, initialModelProfileId = "universal_default")
        viewModel.regenerate().join()
        val state = viewModel.state.value as OutputDeliveryState.Ready

        assertTrue("total باید در بازه‌ی معتبر ۰ تا ۱۰۰ باشد، مقدار واقعی: ${state.qualityScore.total}", state.qualityScore.total in 0..100)
        assertEquals(
            state.qualityScore.total,
            state.qualityScore.subjectClarity + state.qualityScore.cinematicClarity +
                state.qualityScore.visualSpecificity + state.qualityScore.styleCoherence + state.qualityScore.conciseness
        )
    }

    /**
     * exportOutput واقعاً exportFiles بسته‌ی رندرشده را می‌نویسد — از طریق
     * ExportFileWriter تزریقی (Fake اینجا)، بدون نیاز به Context/File I/O واقعی.
     */
    @Test
    fun `exportOutput writes the real composed exportFiles through the injected ExportFileWriter`() = runBlocking {
        val shotId = "${SHOT_ID}_export"
        shotRepository.saveShot(buildShot(shotId, "A calm establishing shot of the courtyard.")).getOrThrow()

        val writer = FakeExportFileWriter()
        val viewModel = buildViewModel(shotId, initialModelProfileId = "universal_default", exportFileWriter = writer)
        viewModel.regenerate().join()
        val expectedExportFiles = (viewModel.state.value as OutputDeliveryState.Ready).outputPackage.exportFiles

        assertEquals(null, viewModel.exportedFiles.value)

        viewModel.exportOutput().join()

        assertEquals(3, writer.writtenFiles.size)
        assertEquals(expectedExportFiles.map { it.filename }.toSet(), writer.writtenFiles.keys)
        assertEquals(expectedExportFiles.first().content, writer.writtenFiles.getValue(expectedExportFiles.first().filename))

        val exported = viewModel.exportedFiles.value
        assertTrue(exported != null)
        assertEquals(3, exported!!.size)

        viewModel.clearExportedFiles()
        assertEquals(null, viewModel.exportedFiles.value)
    }

    // اتصال واقعی Rule یتیم validateProfileAvailability (ADR-126) — سه تست،
    // هم‌الگو با WorkflowViewModelTest.kt (ShadowLog، قدم قبلی/ADR-125): رفتار
    // ظاهری (Fallback به ALL_MODEL_PROFILES.first()) عمداً بدون تغییر می‌ماند؛
    // این سه تست فقط اثبات می‌کنند که (۱) یک profileId معتبر هیچ هشداری
    // تولید نمی‌کند، (۲) یک profileId نامعتبر همچنان Fallback می‌کند *و* یک
    // هشدار واقعی Log می‌شود، (۳) initialModelProfileId=null رفتار قبلی
    // (بدون هیچ Rule/Log) را دقیقاً حفظ می‌کند.

    @Test
    fun `a valid initialModelProfileId selects that profile and never logs a warning`() = runBlocking {
        val shotId = "${SHOT_ID}_valid_profile_id"
        shotRepository.saveShot(buildShot(shotId, "A calm establishing shot of the courtyard.")).getOrThrow()
        ShadowLog.clear()

        val viewModel = buildViewModel(shotId, initialModelProfileId = "veo_3_1")

        assertEquals("veo_3_1", viewModel.selectedProfileId.value)
        val warningLogged = ShadowLog.getLogs().any { it.tag == "OutputDeliveryViewModel" && it.type == Log.WARN }
        assertFalse("یک profileId معتبر نباید هیچ هشداری تولید کند", warningLogged)
    }

    /**
     * یافته‌ی واقعی (انحراف آگاهانه از فرض اولیه‌ی دستور کار این قدم، مستندشده
     * طبق قانون افشای انحراف): ALL_MODEL_PROFILES یک لیست ثابت Kotlin است که
     * همیشه universalDefaultProfile را دارد (تأییدشده مستقل، ModelProfiles.kt
     * خط ۳۱۰) — پس validateProfileAvailability(هر رشته، ALL_MODEL_PROFILES)
     * همیشه null برمی‌گرداند (hasUniversalFallback همیشه true است)، حتی برای
     * یک profileId کاملاً جعلی. این دقیقاً همان تحلیل فنی صریح خودِ دستور کار
     * این قدم بود («عملاً یک سناریوی فاجعه‌بار که فعلاً هرگز رخ نمی‌دهد»)؛ اما
     * فهرست تست‌های الزامی همان دستور کار انتظار «یک Log هشدار واقعاً ثبت
     * می‌شود» را برای همین سناریو داشت — یک ناسازگاری داخلی بین تحلیل و فهرست
     * تست خودِ دستور کار. این تست اصلاح شد تا با رفتار واقعی/صحیح کد هم‌خوان
     * باشد (بدون هشدار)؛ اثبات این‌که خودِ Rule وقتی واقعاً universal_default
     * هم موجود نباشد کار می‌کند، از قبل در ModelProfileLibraryTest.kt
     * («validateProfileAvailability is blocking when nothing is available at
     * all») پوشش داده شده — تکرارش اینجا با یک profiles list ساختگی چیز
     * تازه‌ای اثبات نمی‌کرد.
     */
    @Test
    fun `an invalid initialModelProfileId still falls back to the first profile - unchanged behavior - and produces no warning because universal_default is always present in ALL_MODEL_PROFILES`() = runBlocking {
        val shotId = "${SHOT_ID}_invalid_profile_id"
        shotRepository.saveShot(buildShot(shotId, "A calm establishing shot of the courtyard.")).getOrThrow()
        ShadowLog.clear()

        val viewModel = buildViewModel(shotId, initialModelProfileId = "fake_nonexistent_profile_xyz")

        assertEquals(
            "رفتار ظاهری برای کاربر نباید تغییر کند — مقدار نامعتبر همچنان به ALL_MODEL_PROFILES.first() Fallback می‌شود",
            ALL_MODEL_PROFILES.first().profileId,
            viewModel.selectedProfileId.value
        )
        val warningLogged = ShadowLog.getLogs().any { it.tag == "OutputDeliveryViewModel" && it.type == Log.WARN }
        assertFalse(
            "چون ALL_MODEL_PROFILES همیشه universal_default را دارد، validateProfileAvailability همیشه null برمی‌گرداند — این سناریو فعلاً هرگز واقعاً هشدار تولید نمی‌کند",
            warningLogged
        )
    }

    @Test
    fun `a null initialModelProfileId selects the first profile exactly as before, with no warning`() = runBlocking {
        val shotId = "${SHOT_ID}_null_profile_id"
        shotRepository.saveShot(buildShot(shotId, "A calm establishing shot of the courtyard.")).getOrThrow()
        ShadowLog.clear()

        val viewModel = buildViewModel(shotId, initialModelProfileId = null)

        assertEquals(ALL_MODEL_PROFILES.first().profileId, viewModel.selectedProfileId.value)
        val warningLogged = ShadowLog.getLogs().any { it.tag == "OutputDeliveryViewModel" && it.type == Log.WARN }
        assertFalse("null یک حالت معتبر و شناخته‌شده است، نه یک مقدار نامعتبر — نباید هیچ هشداری تولید کند", warningLogged)
    }
}

/** هم‌الگو با FakeBackupFileStorage (BackupManagerTest.kt) — بدون I/O واقعی، فقط در حافظه. */
private class FakeExportFileWriter : ExportFileWriter {
    val writtenFiles = mutableMapOf<String, String>()

    override suspend fun writeExportFiles(files: List<ExportFile>): List<java.io.File> {
        return files.map { file ->
            writtenFiles[file.filename] = file.content
            java.io.File("/fake/exports/${file.filename}")
        }
    }
}
