package com.operaboys.cinemashotgenerator.ui.shots

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.SecureKeyRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.SceneLocation
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.storybreakdown.GEMINI_API_PROFILE
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// سیستم Preview دوزبانه‌ی پرامپت — قدم ۳ از ۳ زیرقدم، پایانی (ADR-123): اولین
// تست ViewModel-level این کلاس — فقط برای فیلد تازه‌ی shotDescriptionFaPreview.
// برخلاف سه ViewModel فرم Asset (که save() با canSave/combine(viewModelScope)
// گیت می‌شود)، ShotComposerViewModel.save() فقط با _isReady (ست‌شده در
// init{}.ioScope.launch، روی ioScopeOverride تزریقی) گیت می‌شود.
//
// یافته‌ی واقعی دیباگ این کلاس: ShotEntity یک ForeignKey واقعی به SceneEntity
// دارد (data/entity/ShotEntity.kt) — بدون ساخت واقعی یک Project و Scene اول،
// repository.saveShot بی‌صدا با SQLiteConstraintException شکست می‌خورد (چون
// runCatching آن را می‌بلعد)، دقیقاً همان علت مستندشده‌ی مشابه در
// AppDatabaseDaoTest.kt/OutputDeliveryViewModelTest.kt.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShotComposerViewModelTest {

    private lateinit var injectedDatabase: AppDatabase

    private val projectId = "proj_shot_fa_preview_test"
    private val sceneId = "scene_shot_fa_preview_test"

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        injectedDatabase = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java).allowMainThreadQueries().build()
        runBlocking {
            ProjectRepository(injectedDatabase.projectDao(), idProvider = { projectId })
                .createProject("Shot Composer FA Preview Test").getOrThrow()
            SceneRepository(injectedDatabase.sceneDao()).saveScene(
                projectId,
                Scene(
                    sceneId = sceneId,
                    sceneNumber = 1,
                    narrativeRole = NarrativeRole.DEVELOPMENT,
                    location = SceneLocation(type = LocationType.MIXED, description = "a quiet courtyard"),
                    timeOfDay = TimeOfDay.AFTERNOON,
                    atmospherePrimary = Atmosphere.CALM,
                    shotCount = 1
                )
            ).getOrThrow()
        }
    }

    @After
    fun tearDown() {
        injectedDatabase.close()
    }

    private fun buildViewModel(
        shotId: String?,
        secureKeyRepository: SecureKeyRepository? = null,
        httpClientEngine: HttpClientEngine? = null
    ): ShotComposerViewModel = ShotComposerViewModel(
        application = ApplicationProvider.getApplicationContext(),
        projectId = projectId,
        sceneId = sceneId,
        existingShotId = shotId,
        repository = ShotRepository(injectedDatabase.shotDao()),
        sceneRepository = SceneRepository(injectedDatabase.sceneDao()),
        projectDnaRepository = ProjectDnaRepository(injectedDatabase.projectDnaDao()),
        assetRepository = AssetRepository(injectedDatabase.assetDao()),
        idProvider = { shotId ?: "shot_fa_preview_generated" },
        secureKeyRepository = secureKeyRepository ?: SecureKeyRepository(ApplicationProvider.getApplicationContext()),
        httpClientEngine = httpClientEngine ?: OkHttp.create(),
        ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
    )

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

    /**
     * save() در ShotComposerViewModel یک عملیات Fire-and-Forget است —
     * `ioScope.launch { repository.saveShot(shot) }` بدون هیچ سیگنال «تمام شد»
     * بیرونی (برخلاف saveCompleted در فرم‌های Asset). خواندن بلافاصله‌ی
     * repository.loadShot(id) بعد از یک setter ممکن است هنوز نوشتن واقعی Room
     * کامل نشده باشد — این Poll دقیقاً همان انتظار محدودی است که برای
     * saveSession در AiStoryBreakdownViewModelTest.kt لازم بود.
     */
    private suspend fun awaitLoadedShot(
        repository: ShotRepository,
        shotId: String,
        timeoutMs: Long = 3000,
        predicate: (Shot?) -> Boolean
    ): Shot? {
        var loaded = repository.loadShot(shotId).getOrNull()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate(loaded) && System.currentTimeMillis() < deadline) {
            Thread.sleep(5)
            loaded = repository.loadShot(shotId).getOrNull()
        }
        assertTrue("condition was never met within ${timeoutMs}ms, last loaded shot=$loaded", predicate(loaded))
        return loaded
    }

    @Test
    fun `setShotDescriptionFaPreview auto-saves a non-null value exactly as entered`() = runBlocking {
        val repository = ShotRepository(injectedDatabase.shotDao())
        val vm = buildViewModel(shotId = "shot_fa_preview_set")
        awaitCondition(vm.isReady) { it }

        vm.setShotDescriptionFaPreview("جان وارد می‌شود")

        val loaded = awaitLoadedShot(repository, "shot_fa_preview_set") { it?.shotDescriptionFaPreview != null }
        assertEquals("جان وارد می‌شود", loaded?.shotDescriptionFaPreview)
    }

    @Test
    fun `a shot never given a Farsi preview keeps shotDescriptionFaPreview null after auto-save`() = runBlocking {
        val repository = ShotRepository(injectedDatabase.shotDao())
        val vm = buildViewModel(shotId = "shot_fa_preview_null")
        awaitCondition(vm.isReady) { it }

        vm.setShotDescription("John enters")

        val loaded = awaitLoadedShot(repository, "shot_fa_preview_null") { it?.shotDescription == "John enters" }
        assertEquals("John enters", loaded?.shotDescription)
        assertNull(loaded?.shotDescriptionFaPreview)
    }

    @Test
    fun `loading an existing shot with a saved Farsi preview reloads it into shotDescriptionFaPreview`() = runBlocking {
        val repository = ShotRepository(injectedDatabase.shotDao())
        val firstVm = buildViewModel(shotId = "shot_fa_preview_reload").also {
            awaitCondition(it.isReady) { ready -> ready }
        }
        val shotId = firstVm.shotId
        firstVm.setShotDescriptionFaPreview("جان وارد می‌شود")
        // اثبات‌شده در دو تست بالا: save() یک عملیات Fire-and-Forget است — قبل از
        // ساختن secondVm (که این شات را از Room بارگذاری می‌کند)، باید مطمئن شویم
        // نوشتن واقعی firstVm.save() قبلاً کامل شده، وگرنه secondVm نسخه‌ی قدیمی
        // (بدون Preview فارسی) را می‌بیند.
        awaitLoadedShot(repository, shotId) { it?.shotDescriptionFaPreview != null }

        val secondVm = buildViewModel(shotId = shotId)
        awaitCondition(secondVm.isReady) { it }

        assertEquals("جان وارد می‌شود", secondVm.shotDescriptionFaPreview.value)
    }

    // فیچر مستقل «ترجمه‌ی مجدد با AI» (ADR-124) — سه تست امنیتی، هم‌الگو با
    // CharacterAssetFormViewModelTest.kt، اما اینجا (برخلاف آن) موفقیت باید
    // واقعاً Persist شود چون retranslate() در این ViewModel بلافاصله save()
    // را صدا می‌زند (تأییدشده با بررسی مستقل خودِ این کلاس، نه فرض یکسان‌بودن
    // با فرم‌های Asset).

    @Test
    fun `retranslate without a saved key never attempts a real HTTP call and sets a meaningful error`() = runBlocking {
        val secureKeyRepository = buildTestSecureKeyRepository("shot_retranslate_test_no_key_prefs")
        var engineCalled = false
        val engine = MockEngine {
            engineCalled = true
            respond(content = "{}", status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val vm = buildViewModel("shot_retranslate_no_key", secureKeyRepository = secureKeyRepository, httpClientEngine = engine)
        awaitCondition(vm.isReady) { it }
        vm.setShotDescription("John enters the dim office")

        vm.retranslate().join()

        assertFalse(
            "بدون کلید ذخیره‌شده نباید هیچ تلاش HTTP واقعی برای ترجمه‌ی مجدد انجام شود — مهم‌ترین تست امنیتی این فیچر",
            engineCalled
        )
        assertNotNull(vm.translationError.value)
        assertEquals("", vm.shotDescriptionFaPreview.value)
    }

    @Test
    fun `retranslate with a saved key and a successful response replaces and persists shotDescriptionFaPreview`() = runBlocking {
        val repository = ShotRepository(injectedDatabase.shotDao())
        val secureKeyRepository = buildTestSecureKeyRepository("shot_retranslate_test_success_prefs")
        secureKeyRepository.saveApiKey(GEMINI_API_PROFILE.profileId, "gemini-real-key")
        val engine = MockEngine {
            respond(
                content = """{"candidates":[{"content":{"parts":[{"text":"جان وارد دفتر کم‌نور می‌شود"}]}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val vm = buildViewModel("shot_retranslate_success", secureKeyRepository = secureKeyRepository, httpClientEngine = engine)
        awaitCondition(vm.isReady) { it }
        vm.setShotDescription("John enters the dim office")
        awaitCondition(vm.apiKeySavedForTranslationProfile) { it }

        vm.retranslate().join()

        assertEquals("جان وارد دفتر کم‌نور می‌شود", vm.shotDescriptionFaPreview.value)
        assertNull(vm.translationError.value)
        val loaded = awaitLoadedShot(repository, "shot_retranslate_success") { it?.shotDescriptionFaPreview != null }
        assertEquals("جان وارد دفتر کم‌نور می‌شود", loaded?.shotDescriptionFaPreview)
    }

    @Test
    fun `retranslate with a saved key but a failing HTTP response sets a meaningful translationError and leaves shotDescriptionFaPreview untouched`() = runBlocking {
        val secureKeyRepository = buildTestSecureKeyRepository("shot_retranslate_test_failure_prefs")
        secureKeyRepository.saveApiKey(GEMINI_API_PROFILE.profileId, "gemini-real-key")
        val engine = MockEngine {
            respond(
                content = """{"error":{"message":"invalid API key"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val vm = buildViewModel("shot_retranslate_failure", secureKeyRepository = secureKeyRepository, httpClientEngine = engine)
        awaitCondition(vm.isReady) { it }
        vm.setShotDescription("John enters the dim office")
        awaitCondition(vm.apiKeySavedForTranslationProfile) { it }

        vm.retranslate().join()

        assertEquals("", vm.shotDescriptionFaPreview.value)
        assertNotNull(vm.translationError.value)
        assertTrue(vm.translationError.value!!.contains("invalid API key"))
    }
}
