package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.data.repository.SecureKeyRepository
import com.operaboys.cinemashotgenerator.domain.asset.Environment
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.storybreakdown.GEMINI_API_PROFILE
import com.operaboys.cinemashotgenerator.ui.dna.defaultProjectDna
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// سیستم Preview دوزبانه‌ی پرامپت — قدم ۳ از ۳ زیرقدم، پایانی (ADR-123): اولین
// تست ViewModel-level این کلاس (تا این قدم فقط AssetFormFlowTest.kt در سطح
// Compose E2E پوشش می‌داد) — فقط برای فیلد تازه‌ی descriptionFaPreview، هم‌الگو
// دقیق با تست‌های معادل تازه‌اضافه‌شده‌ی CharacterAssetFormViewModelTest.kt.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocationAssetFormViewModelTest {

    private lateinit var injectedDatabase: AppDatabase

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        injectedDatabase = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        injectedDatabase.close()
    }

    private fun <T> awaitCondition(flow: StateFlow<T>, timeoutMs: Long = 10_000, predicate: (T) -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate(flow.value) && System.currentTimeMillis() < deadline) {
            Thread.sleep(5)
        }
        assertTrue("condition was never met within ${timeoutMs}ms, last value=${flow.value}", predicate(flow.value))
    }

    @Test
    fun `save persists a non-null descriptionFaPreview exactly as entered`() = runBlocking {
        val repository = AssetRepository(injectedDatabase.assetDao())
        val vm = LocationAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_location_fa_preview_test",
            repository = repository,
            idProvider = { "loc_fa_preview_set" },
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        // هم‌الگو دقیق با یافته‌ی دیباگ CharacterAssetFormViewModelTest.kt: canSave
        // از combine(...).stateIn(WhileSubscribed) روی viewModelScope واقعی ساخته
        // می‌شود — بدون یک Collector واقعی + idle صریح، save() بی‌صدا no-op می‌ماند.
        CoroutineScope(Dispatchers.Unconfined).launch { vm.canSave.collect {} }
        vm.setName("Detective's Office")
        vm.setDescription("a dimly lit office")
        vm.setDescriptionFaPreview("یک دفتر کم‌نور")
        shadowOf(Looper.getMainLooper()).idle()

        vm.save()
        awaitCondition(vm.saveCompleted) { it }

        val loaded = repository.loadLocationAssets(listOf("loc_fa_preview_set")).getOrThrow().single()
        assertEquals("یک دفتر کم‌نور", loaded.descriptionFaPreview)
    }

    @Test
    fun `save leaves descriptionFaPreview null when the field was never filled in`() = runBlocking {
        val repository = AssetRepository(injectedDatabase.assetDao())
        val vm = LocationAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_location_fa_preview_test",
            repository = repository,
            idProvider = { "loc_fa_preview_null" },
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        CoroutineScope(Dispatchers.Unconfined).launch { vm.canSave.collect {} }
        vm.setName("Detective's Office")
        vm.setDescription("a dimly lit office")
        shadowOf(Looper.getMainLooper()).idle()

        vm.save()
        awaitCondition(vm.saveCompleted) { it }

        val loaded = repository.loadLocationAssets(listOf("loc_fa_preview_null")).getOrThrow().single()
        assertNull(loaded.descriptionFaPreview)
    }

    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۵ از ۵ (ADR-135): تست‌های تازه،
    // هم‌الگو دقیق با CharacterAssetFormViewModelTest.kt (ADR-134)، بدون بخش
    // Outfit (LocationAsset ساختار Outfit ندارد).

    private fun buildTestProjectDnaRepository(): ProjectDnaRepository = ProjectDnaRepository(injectedDatabase.projectDnaDao())

    private fun buildTestSecureKeyRepository(prefsName: String): SecureKeyRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return SecureKeyRepository(context) { appContext -> appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE) }
    }

    // یافته‌ی دیباگ مستندشده‌ی ADR-134/DnaViewModelTest.kt: ProjectDnaEntity یک
    // ForeignKey واقعی به ProjectEntity دارد — بدون یک ردیف Project واقعی،
    // saveProjectDna داخل runCatching بی‌صدا شکست می‌خورد.
    private suspend fun createTestProject(projectId: String) {
        ProjectRepository(injectedDatabase.projectDao(), idProvider = { projectId }).createProject("Image Prompt Test").getOrThrow()
    }

    @Test
    fun `generateImagePromptQuick populates imagePromptQuick and imagePromptGeneratedAt from the live Template preview`() = runBlocking {
        createTestProject("proj_location_image_prompt_test")
        val projectDnaRepository = buildTestProjectDnaRepository()
        projectDnaRepository.saveProjectDna(defaultProjectDna("proj_location_image_prompt_test") { "dna_location_quick_test" }).getOrThrow()
        val vm = LocationAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_location_image_prompt_test",
            repository = AssetRepository(injectedDatabase.assetDao()),
            projectDnaRepository = projectDnaRepository,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        CoroutineScope(Dispatchers.Unconfined).launch { vm.imagePromptPreview.collect {} }
        awaitCondition(vm.projectDna) { it != null }
        vm.setDescription("a dimly lit detective office")
        vm.setEnvironmentType("indoor")
        shadowOf(Looper.getMainLooper()).idle()

        vm.generateImagePromptQuick()

        assertNotNull(vm.imagePromptQuick.value)
        assertTrue(vm.imagePromptQuick.value!!.contains("a dimly lit detective office"))
        assertTrue(
            "پرامپت باید شامل Style Tokens واقعی سبک پروژه باشد",
            vm.imagePromptQuick.value!!.contains("cinematic style")
        )
        assertNotNull(vm.imagePromptGeneratedAt.value)
    }

    @Test
    fun `save persists a real updatedAt timestamp, without which stale-prompt detection could never work`() = runBlocking {
        val repository = AssetRepository(injectedDatabase.assetDao())
        val vm = LocationAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_location_updated_at_test",
            repository = repository,
            idProvider = { "loc_updated_at_test" },
            projectDnaRepository = buildTestProjectDnaRepository(),
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        CoroutineScope(Dispatchers.Unconfined).launch { vm.canSave.collect {} }
        val beforeSave = System.currentTimeMillis()
        vm.setName("Detective's Office")
        vm.setDescription("a dimly lit office")
        shadowOf(Looper.getMainLooper()).idle()

        vm.save()
        awaitCondition(vm.saveCompleted, timeoutMs = 10_000) { it }

        val loaded = repository.loadLocationAssets(listOf("loc_updated_at_test")).getOrThrow().single()
        assertNotNull(loaded.updatedAt)
        assertTrue(
            "updatedAt باید زمان همین save() واقعی باشد (نه null، نه یک مقدار قدیمی)",
            loaded.updatedAt!! >= beforeSave
        )
    }

    @Test
    fun `generateLocationImagePromptWithAi without a saved key never attempts a real HTTP call and sets a meaningful error`() = runBlocking {
        val secureKeyRepository = buildTestSecureKeyRepository("location_image_prompt_ai_no_key_prefs")
        var engineCalled = false
        val engine = MockEngine {
            engineCalled = true
            respond(content = "{}", status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        createTestProject("proj_location_image_prompt_ai_test")
        val projectDnaRepository = buildTestProjectDnaRepository()
        projectDnaRepository.saveProjectDna(defaultProjectDna("proj_location_image_prompt_ai_test") { "dna_location_ai_no_key_test" }).getOrThrow()
        val vm = LocationAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_location_image_prompt_ai_test",
            repository = AssetRepository(injectedDatabase.assetDao()),
            secureKeyRepository = secureKeyRepository,
            httpClientEngine = engine,
            projectDnaRepository = projectDnaRepository,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        awaitCondition(vm.projectDna) { it != null }
        vm.setDescription("a dimly lit detective office")

        vm.generateLocationImagePromptWithAi().join()

        assertFalse(
            "بدون کلید ذخیره‌شده نباید هیچ تلاش HTTP واقعی انجام شود — مهم‌ترین تست امنیتی این فیچر",
            engineCalled
        )
        assertNotNull(vm.imagePromptAiError.value)
        assertNull(vm.imagePromptAi.value)
    }

    @Test
    fun `generateLocationImagePromptWithAi with a saved key and a successful bilingual response populates imagePromptAi and imagePromptFaPreview`() = runBlocking {
        val secureKeyRepository = buildTestSecureKeyRepository("location_image_prompt_ai_success_prefs")
        secureKeyRepository.saveApiKey(GEMINI_API_PROFILE.profileId, "gemini-real-key")
        val engine = MockEngine {
            respond(
                content = """{"candidates":[{"content":{"parts":[{"text":"{\"imagePromptEn\": \"a rich cinematic prompt\", \"imagePromptFa\": \"یک پرامپت سینمایی غنی\"}"}]}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        createTestProject("proj_location_image_prompt_ai_test")
        val projectDnaRepository = buildTestProjectDnaRepository()
        projectDnaRepository.saveProjectDna(defaultProjectDna("proj_location_image_prompt_ai_test") { "dna_location_ai_success_test" }).getOrThrow()
        val vm = LocationAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_location_image_prompt_ai_test",
            repository = AssetRepository(injectedDatabase.assetDao()),
            secureKeyRepository = secureKeyRepository,
            httpClientEngine = engine,
            projectDnaRepository = projectDnaRepository,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        awaitCondition(vm.projectDna) { it != null }
        vm.setDescription("a dimly lit detective office")
        awaitCondition(vm.apiKeySavedForTranslationProfile) { it }

        vm.generateLocationImagePromptWithAi().join()

        assertEquals("a rich cinematic prompt", vm.imagePromptAi.value)
        assertEquals("یک پرامپت سینمایی غنی", vm.imagePromptFaPreview.value)
        assertNotNull(vm.imagePromptGeneratedAt.value)
        assertNull(vm.imagePromptAiError.value)
    }

    @Test
    fun `isImagePromptStale is true only when a prompt exists and the asset was loaded with a newer updatedAt`() = runBlocking {
        val repository = AssetRepository(injectedDatabase.assetDao())
        val vm0 = LocationAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_location_stale_no_asset_test",
            repository = repository,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        assertFalse(
            "بدون هیچ imagePromptGeneratedAt ای، قدیمی‌بودن معنا ندارد",
            vm0.isImagePromptStale(null)
        )

        repository.saveLocationAsset(
            "proj_location_stale_test",
            LocationAsset(
                assetId = "loc_stale_test",
                name = "Detective's Office",
                description = "a dimly lit office",
                environment = Environment(type = "indoor", size = "medium", lightingCondition = "dim"),
                updatedAt = 2_000L
            )
        ).getOrThrow()
        val vm = LocationAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_location_stale_test",
            repository = repository,
            existingAssetId = "loc_stale_test",
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        awaitCondition(vm.name) { it == "Detective's Office" }

        assertTrue(
            "وقتی پرامپت قبل از آخرین ویرایش Asset ساخته شده، باید قدیمی محسوب شود",
            vm.isImagePromptStale(1_000L)
        )
        assertFalse(
            "وقتی پرامپت بعد از آخرین ویرایش Asset ساخته شده، نباید قدیمی محسوب شود",
            vm.isImagePromptStale(3_000L)
        )
    }

    // اتصال Rule یتیم ADR-132 (ADR-136): generatedPrompt به این تابع فقط بعد از
    // تولید واقعی موجود است — پس با generateImagePromptQuick() واقعی روی
    // ViewModel تأیید می‌شود، نه فراخوانی مستقیم تابع خالص دامنه.
    @Test
    fun `generateImagePromptQuick with an empty description populates imagePromptValidationIssues`() = runBlocking {
        val projectId = "proj_location_validation_test"
        ProjectRepository(injectedDatabase.projectDao(), idProvider = { projectId }).createProject("Image Prompt Test").getOrThrow()
        val projectDnaRepository = ProjectDnaRepository(injectedDatabase.projectDnaDao())
        projectDnaRepository.saveProjectDna(defaultProjectDna(projectId) { "dna_location_validation_test" }).getOrThrow()
        val vm = LocationAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = projectId,
            repository = AssetRepository(injectedDatabase.assetDao()),
            projectDnaRepository = projectDnaRepository,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        CoroutineScope(Dispatchers.Unconfined).launch { vm.imagePromptPreview.collect {} }
        awaitCondition(vm.projectDna) { it != null }
        // description عمداً خالی می‌ماند — همان ورودی ناقصی که Rule
        // validateLocationImagePromptInputs (description.isBlank() ||
        // environment.type.isBlank()) بررسی می‌کند.
        shadowOf(Looper.getMainLooper()).idle()

        vm.generateImagePromptQuick()

        assertTrue(
            "با description خالی، imagePromptValidationIssues نباید خالی بماند",
            vm.imagePromptValidationIssues.value.isNotEmpty()
        )
    }
}
