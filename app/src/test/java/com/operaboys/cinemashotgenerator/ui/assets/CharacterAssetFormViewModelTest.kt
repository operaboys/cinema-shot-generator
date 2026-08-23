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
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.Outfit
import com.operaboys.cinemashotgenerator.domain.asset.PhysicalAppearance
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
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

// رفع G5 (ADR-067 بخش ه، ADR-095): تست‌های واحد addOutfit/removeOutfit/
// setOutfitAsDefault/شرط Outfit — اولین تست ViewModel-level این کلاس (تا این
// قدم فقط AssetFormFlowTest.kt در سطح Compose E2E پوشش می‌داد). هم‌الگو با
// AiStoryBreakdownViewModelTest.kt (Room in-memory واقعی، بدون Compose).

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CharacterAssetFormViewModelTest {

    private lateinit var injectedDatabase: AppDatabase
    private lateinit var viewModel: CharacterAssetFormViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        injectedDatabase = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java).allowMainThreadQueries().build()
        viewModel = CharacterAssetFormViewModel(
            application = application,
            projectId = "proj_character_outfit_test",
            repository = AssetRepository(injectedDatabase.assetDao())
        )
    }

    @After
    fun tearDown() {
        injectedDatabase.close()
    }

    @Test
    fun `a new character form starts with exactly one default outfit, so Rule 5 holds without any user interaction`() {
        assertEquals(1, viewModel.outfits.value.size)
        assertTrue(viewModel.outfits.value.single().isDefault)
    }

    @Test
    fun `addOutfit adds a second outfit with its own condition, without touching the first`() {
        viewModel.addOutfit("Winter coat", "a heavy wool coat")
        assertEquals(2, viewModel.outfits.value.size)

        viewModel.setOutfitConditionWeather(1, WeatherType.SNOW)

        val outfits = viewModel.outfits.value
        assertEquals("Winter coat", outfits[1].name)
        assertEquals("snow", outfits[1].condition?.weather)
        assertEquals(null, outfits[0].condition)
    }

    @Test
    fun `a newly added outfit is not default while an existing default remains`() {
        viewModel.addOutfit("Winter coat", "a heavy wool coat")
        assertFalse(viewModel.outfits.value[1].isDefault)
        assertTrue(viewModel.outfits.value[0].isDefault)
    }

    @Test
    fun `removing a non-default outfit removes it and leaves the default untouched`() {
        viewModel.addOutfit("Winter coat", "a heavy wool coat")
        val defaultId = viewModel.outfits.value[0].id

        viewModel.removeOutfit(1)

        assertEquals(1, viewModel.outfits.value.size)
        assertEquals(defaultId, viewModel.outfits.value[0].id)
        assertTrue(viewModel.outfits.value[0].isDefault)
    }

    @Test
    fun `removing the default outfit promotes the first remaining outfit to default`() {
        viewModel.addOutfit("Winter coat", "a heavy wool coat")
        val winterCoatId = viewModel.outfits.value[1].id

        viewModel.removeOutfit(0)

        assertEquals(1, viewModel.outfits.value.size)
        assertEquals(winterCoatId, viewModel.outfits.value[0].id)
        assertTrue(
            "طبق تصمیم مستند در ViewModel، حذف Outfit پیش‌فرض باید اولین عضو باقی‌مانده را خودکار پیش‌فرض کند",
            viewModel.outfits.value[0].isDefault
        )
    }

    @Test
    fun `removing the only remaining outfit is blocked, keeping Rule 5 structurally true`() {
        val onlyOutfitId = viewModel.outfits.value[0].id

        viewModel.removeOutfit(0)

        assertEquals(
            "حذف تنها Outfit باقی‌مانده باید مسدود شود تا لیست هرگز خالی نشود (وگرنه selectOutfitForScene در زمان اجرا Crash می‌کند)",
            1,
            viewModel.outfits.value.size
        )
        assertEquals(onlyOutfitId, viewModel.outfits.value[0].id)
    }

    @Test
    fun `setOutfitAsDefault on a new outfit automatically un-defaults the previous default`() {
        viewModel.addOutfit("Winter coat", "a heavy wool coat")

        viewModel.setOutfitAsDefault(1)

        assertFalse(viewModel.outfits.value[0].isDefault)
        assertTrue(viewModel.outfits.value[1].isDefault)
    }

    @Test
    fun `setting all three outfit condition fields to non-null and back to null round-trips through OutfitCondition`() {
        viewModel.setOutfitConditionWeather(0, WeatherType.RAIN)
        viewModel.setOutfitConditionTimeOfDay(0, TimeOfDay.NIGHT)
        viewModel.setOutfitConditionLocationType(0, LocationType.OUTDOOR)

        val condition = viewModel.outfits.value[0].condition
        assertEquals("rain", condition?.weather)
        assertEquals("night", condition?.timeOfDay)
        assertEquals("outdoor", condition?.locationType)

        viewModel.setOutfitConditionWeather(0, null)
        viewModel.setOutfitConditionTimeOfDay(0, null)
        viewModel.setOutfitConditionLocationType(0, null)

        assertEquals(
            "وقتی هر سه فیلد شرط null می‌شوند، کل condition باید null شود (نه یک OutfitCondition با سه فیلد null)",
            null,
            viewModel.outfits.value[0].condition
        )
    }

    // یادداشت: تستی برای validationIssues (Rule ۵ روی لیست خالی) عمداً اینجا
    // نوشته نشد — validationIssues از combine(...).stateIn(WhileSubscribed) ساخته
    // می‌شود؛ بدون یک Collector واقعی (یا ioScopeOverride = Dispatchers.Unconfined،
    // که این فایل هم‌الگو با AiStoryBreakdownViewModelTest.kt عمداً ندارد)، `.value`
    // فقط مقدار اولیه (emptyList) را برمی‌گرداند، نه محاسبه‌ی واقعی — یک تست اینجا
    // چیزی را واقعاً اثبات نمی‌کرد. Rule ۵ خودش در AssetValidationTest.kt (سطح
    // دامنه) و از طریق آزمون‌های بالا (لیست هرگز خالی نمی‌شود) پوشش داده شده.

    // سیستم Preview دوزبانه‌ی پرامپت — قدم ۳ از ۳ زیرقدم، پایانی (ADR-123):
    // برخلاف تست‌های بالا، این دو تست واقعاً save() (شامل I/O واقعی Room) را
    // اثبات می‌کنند — پس یک نمونه‌ی جدا با ioScopeOverride=Dispatchers.Unconfined
    // و idProvider ثابت می‌سازند (نه viewModel کلاسی بالا).

    private fun <T> awaitCondition(flow: StateFlow<T>, timeoutMs: Long = 3000, predicate: (T) -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate(flow.value) && System.currentTimeMillis() < deadline) {
            Thread.sleep(5)
        }
        assertTrue("condition was never met within ${timeoutMs}ms, last value=${flow.value}", predicate(flow.value))
    }

    // یافته‌ی واقعی دیباگ این دو تست: (۱) canSave از combine(...).stateIn(WhileSubscribed)
    // ساخته می‌شود — بدون یک Collector واقعی (هم‌الگو با یادداشت بالای این کلاس
    // درباره‌ی validationIssues)، save() که canSave.value را چک می‌کند همیشه بی‌صدا
    // no-op می‌ماند؛ یک Collector مصنوعی + shadowOf(Looper.getMainLooper()).idle()
    // (چون این StateFlow روی viewModelScope واقعی/Dispatchers.Main.immediate است، نه
    // ioScopeOverride) دقیقاً همان اثری را دارد که UI واقعی (collectAsStateWithLifecycle)
    // در تولید ایجاد می‌کند. (۲) اولین نوشتن واقعی Room در این کلاس تست (که تا این
    // قدم فقط کار In-Memory با DAO خام synchronous می‌کرد) هزینه‌ی گرم‌شدن قابل‌توجهی
    // دارد (مقداردهی اولیه‌ی Thread Pool داخلی Room برای suspend DAO) — با
    // CoroutineExceptionHandler و لاگ مستقیم تأیید شد ioScope.launch{} واقعاً اجرا
    // می‌شود و بدون خطا کامل می‌شود، فقط دیرتر از ۳۰۰۰ms پیش‌فرض awaitCondition؛
    // timeout بزرگ‌تر (۱۰ ثانیه) این هزینه‌ی گرم‌شدنِ یک‌باره را پوشش می‌دهد.

    @Test
    fun `save persists a non-null descriptionFaPreview exactly as entered`() = runBlocking {
        val repository = AssetRepository(injectedDatabase.assetDao())
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_character_fa_preview_test",
            repository = repository,
            idProvider = { "char_fa_preview_set" },
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        CoroutineScope(Dispatchers.Unconfined).launch { vm.canSave.collect {} }
        vm.setName("Detective John")
        vm.setAgeRange("30-40")
        vm.setDescriptionFaPreview("یک کارآگاه بلندقد")
        shadowOf(Looper.getMainLooper()).idle()

        vm.save()
        awaitCondition(vm.saveCompleted, timeoutMs = 10_000) { it }

        val loaded = repository.loadCharacterAssets(listOf("char_fa_preview_set")).getOrThrow().single()
        assertEquals("یک کارآگاه بلندقد", loaded.descriptionFaPreview)
    }

    @Test
    fun `save leaves descriptionFaPreview null when the field was never filled in`() = runBlocking {
        val repository = AssetRepository(injectedDatabase.assetDao())
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_character_fa_preview_test",
            repository = repository,
            idProvider = { "char_fa_preview_null" },
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        CoroutineScope(Dispatchers.Unconfined).launch { vm.canSave.collect {} }
        vm.setName("Detective John")
        vm.setAgeRange("30-40")
        shadowOf(Looper.getMainLooper()).idle()

        vm.save()
        awaitCondition(vm.saveCompleted, timeoutMs = 10_000) { it }

        val loaded = repository.loadCharacterAssets(listOf("char_fa_preview_null")).getOrThrow().single()
        assertNull(loaded.descriptionFaPreview)
    }

    // فیچر مستقل «ترجمه‌ی مجدد با AI» (ADR-124، جدا از برنامه‌ی Preview دوزبانه‌ی
    // ADR-121 تا ۱۲۳) — سه تست امنیتی، هم‌الگو دقیق با
    // AiStoryBreakdownViewModelTest.kt/OutputDeliveryViewModelTest.kt (بدون
    // کلید → هیچ HTTP واقعی؛ با کلید و پاسخ موفق → descriptionFaPreview واقعاً
    // جایگزین می‌شود؛ با کلید و پاسخ ناموفق → پیام خطای معنادار). این کلاس
    // نماینده‌ی الگوی «سه ViewModel فرم Asset» است (تأییدشده با بررسی مستقل:
    // LocationAssetFormViewModel/ObjectAssetFormViewModel هم retranslate()
    // کاملاً هم‌ساختار دارند، فقط فیلد متن منبع فرق می‌کند)؛ تکرار کامل این سه
    // تست برای هر چهار ViewModel چیز تازه‌ای اثبات نمی‌کرد — طبق دستور صریح
    // این قدم برای تصمیم‌گیری مستقل درباره‌ی محدوده‌ی تست.

    private fun buildTestSecureKeyRepository(prefsName: String): SecureKeyRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return SecureKeyRepository(context) { appContext -> appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE) }
    }

    @Test
    fun `retranslate without a saved key never attempts a real HTTP call and sets a meaningful error`() = runBlocking {
        val secureKeyRepository = buildTestSecureKeyRepository("character_retranslate_test_no_key_prefs")
        var engineCalled = false
        val engine = MockEngine {
            engineCalled = true
            respond(content = "{}", status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_character_retranslate_test",
            repository = AssetRepository(injectedDatabase.assetDao()),
            idProvider = { "char_retranslate_no_key" },
            secureKeyRepository = secureKeyRepository,
            httpClientEngine = engine,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        vm.setBasePrompt("a grizzled veteran detective")

        vm.retranslate().join()

        assertFalse(
            "بدون کلید ذخیره‌شده نباید هیچ تلاش HTTP واقعی برای ترجمه‌ی مجدد انجام شود — مهم‌ترین تست امنیتی این فیچر",
            engineCalled
        )
        assertNotNull(vm.translationError.value)
        assertEquals("", vm.descriptionFaPreview.value)
    }

    @Test
    fun `retranslate with a saved key and a successful response replaces descriptionFaPreview with the AI's translation`() = runBlocking {
        val secureKeyRepository = buildTestSecureKeyRepository("character_retranslate_test_success_prefs")
        secureKeyRepository.saveApiKey(GEMINI_API_PROFILE.profileId, "gemini-real-key")
        val engine = MockEngine {
            respond(
                content = """{"candidates":[{"content":{"parts":[{"text":"یک کارآگاه کهنه‌کار"}]}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_character_retranslate_test",
            repository = AssetRepository(injectedDatabase.assetDao()),
            idProvider = { "char_retranslate_success" },
            secureKeyRepository = secureKeyRepository,
            httpClientEngine = engine,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        vm.setBasePrompt("a grizzled veteran detective")
        awaitCondition(vm.apiKeySavedForTranslationProfile) { it }

        vm.retranslate().join()

        assertEquals("یک کارآگاه کهنه‌کار", vm.descriptionFaPreview.value)
        assertNull(vm.translationError.value)
    }

    @Test
    fun `retranslate with a saved key but a failing HTTP response sets a meaningful translationError and leaves descriptionFaPreview untouched`() = runBlocking {
        val secureKeyRepository = buildTestSecureKeyRepository("character_retranslate_test_failure_prefs")
        secureKeyRepository.saveApiKey(GEMINI_API_PROFILE.profileId, "gemini-real-key")
        val engine = MockEngine {
            respond(
                content = """{"error":{"message":"invalid API key"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_character_retranslate_test",
            repository = AssetRepository(injectedDatabase.assetDao()),
            idProvider = { "char_retranslate_failure" },
            secureKeyRepository = secureKeyRepository,
            httpClientEngine = engine,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        vm.setBasePrompt("a grizzled veteran detective")
        awaitCondition(vm.apiKeySavedForTranslationProfile) { it }

        vm.retranslate().join()

        assertEquals("", vm.descriptionFaPreview.value)
        assertNotNull(vm.translationError.value)
        assertTrue(vm.translationError.value!!.contains("invalid API key"))
    }

    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۴ از ۵ (ADR-134): همان الگوی
    // دقیق تست‌های بالا (ioScopeOverride=Dispatchers.Unconfined، Collector مصنوعی
    // برای StateFlow های combine(...).stateIn(WhileSubscribed) — اینجا
    // characterBaseImagePromptPreview، طبق همان یافته‌ی مستندشده‌ی canSave بالا).

    private fun buildTestProjectDnaRepository(): ProjectDnaRepository = ProjectDnaRepository(injectedDatabase.projectDnaDao())

    // یافته‌ی واقعی دیباگ این ۳ تست: ProjectDnaEntity یک ForeignKey واقعی به
    // ProjectEntity دارد (هم‌الگو با یافته‌ی مستندشده‌ی DnaViewModelTest.kt) —
    // بدون یک ردیف Project واقعی، saveProjectDna داخل runCatching بی‌صدا شکست
    // می‌خورد و vm.projectDna برای همیشه null می‌ماند.
    private suspend fun createTestProject(projectId: String) {
        ProjectRepository(injectedDatabase.projectDao(), idProvider = { projectId }).createProject("Image Prompt Test").getOrThrow()
    }

    @Test
    fun `generateCharacterBaseImagePromptQuick populates imagePromptQuick and imagePromptGeneratedAt from the live Template preview`() = runBlocking {
        createTestProject("proj_character_outfit_test")
        val projectDnaRepository = buildTestProjectDnaRepository()
        projectDnaRepository.saveProjectDna(defaultProjectDna("proj_character_outfit_test") { "dna_image_prompt_quick_test" }).getOrThrow()
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_character_outfit_test",
            repository = AssetRepository(injectedDatabase.assetDao()),
            projectDnaRepository = projectDnaRepository,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        CoroutineScope(Dispatchers.Unconfined).launch { vm.characterBaseImagePromptPreview.collect {} }
        awaitCondition(vm.projectDna) { it != null }
        vm.setName("Jane Doe")
        vm.setAgeRange("30s")
        shadowOf(Looper.getMainLooper()).idle()

        vm.generateCharacterBaseImagePromptQuick()

        assertNotNull(vm.imagePromptQuick.value)
        assertTrue(vm.imagePromptQuick.value!!.contains("Jane Doe"))
        assertTrue(
            "پرامپت باید شامل Style Tokens واقعی سبک پروژه باشد، نه فقط فیلدهای شخصیت",
            vm.imagePromptQuick.value!!.contains("cinematic style")
        )
        assertNotNull(vm.imagePromptGeneratedAt.value)
    }

    @Test
    fun `generateOutfitImagePromptQuick populates only that outfit's own imagePromptQuick, leaving other outfits untouched`() = runBlocking {
        createTestProject("proj_character_outfit_test")
        val projectDnaRepository = buildTestProjectDnaRepository()
        projectDnaRepository.saveProjectDna(defaultProjectDna("proj_character_outfit_test") { "dna_outfit_quick_test" }).getOrThrow()
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_character_outfit_test",
            repository = AssetRepository(injectedDatabase.assetDao()),
            projectDnaRepository = projectDnaRepository,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        CoroutineScope(Dispatchers.Unconfined).launch { vm.characterBaseImagePromptPreview.collect {} }
        awaitCondition(vm.projectDna) { it != null }
        vm.setName("Jane Doe")
        vm.addOutfit("Winter Coat", "a heavy wool coat")
        shadowOf(Looper.getMainLooper()).idle()

        vm.generateOutfitImagePromptQuick(1)

        val outfits = vm.outfits.value
        assertNotNull(outfits[1].imagePromptQuick)
        assertTrue(outfits[1].imagePromptQuick!!.contains("Jane Doe"))
        assertTrue(outfits[1].imagePromptQuick!!.contains("Winter Coat"))
        assertNotNull(outfits[1].imagePromptGeneratedAt)
        assertNull(
            "تولید پرامپت برای Outfit دوم نباید Outfit اول (پیش‌فرض) را دست بزند",
            outfits[0].imagePromptQuick
        )
    }

    @Test
    fun `save persists a real updatedAt timestamp, without which stale-prompt detection could never work`() = runBlocking {
        val repository = AssetRepository(injectedDatabase.assetDao())
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_character_updated_at_test",
            repository = repository,
            idProvider = { "char_updated_at_test" },
            projectDnaRepository = buildTestProjectDnaRepository(),
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        CoroutineScope(Dispatchers.Unconfined).launch { vm.canSave.collect {} }
        val beforeSave = System.currentTimeMillis()
        vm.setName("Detective John")
        vm.setAgeRange("30-40")
        shadowOf(Looper.getMainLooper()).idle()

        vm.save()
        awaitCondition(vm.saveCompleted, timeoutMs = 10_000) { it }

        val loaded = repository.loadCharacterAssets(listOf("char_updated_at_test")).getOrThrow().single()
        assertNotNull(loaded.updatedAt)
        assertTrue(
            "updatedAt باید زمان همین save() واقعی باشد (نه null، نه یک مقدار قدیمی)",
            loaded.updatedAt!! >= beforeSave
        )
    }

    @Test
    fun `generateCharacterBaseImagePromptWithAi without a saved key never attempts a real HTTP call and sets a meaningful error`() = runBlocking {
        val secureKeyRepository = buildTestSecureKeyRepository("character_image_prompt_ai_no_key_prefs")
        var engineCalled = false
        val engine = MockEngine {
            engineCalled = true
            respond(content = "{}", status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        createTestProject("proj_character_image_prompt_ai_test")
        val projectDnaRepository = buildTestProjectDnaRepository()
        projectDnaRepository.saveProjectDna(defaultProjectDna("proj_character_image_prompt_ai_test") { "dna_image_prompt_ai_no_key_test" }).getOrThrow()
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_character_image_prompt_ai_test",
            repository = AssetRepository(injectedDatabase.assetDao()),
            secureKeyRepository = secureKeyRepository,
            httpClientEngine = engine,
            projectDnaRepository = projectDnaRepository,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        awaitCondition(vm.projectDna) { it != null }
        vm.setName("Jane Doe")
        vm.setAgeRange("30s")

        vm.generateCharacterBaseImagePromptWithAi().join()

        assertFalse(
            "بدون کلید ذخیره‌شده نباید هیچ تلاش HTTP واقعی انجام شود — مهم‌ترین تست امنیتی این فیچر",
            engineCalled
        )
        assertNotNull(vm.imagePromptAiError.value)
        assertNull(vm.imagePromptAi.value)
    }

    @Test
    fun `generateCharacterBaseImagePromptWithAi with a saved key and a successful bilingual response populates imagePromptAi and imagePromptFaPreview`() = runBlocking {
        val secureKeyRepository = buildTestSecureKeyRepository("character_image_prompt_ai_success_prefs")
        secureKeyRepository.saveApiKey(GEMINI_API_PROFILE.profileId, "gemini-real-key")
        val engine = MockEngine {
            respond(
                content = """{"candidates":[{"content":{"parts":[{"text":"{\"imagePromptEn\": \"a rich cinematic prompt\", \"imagePromptFa\": \"یک پرامپت سینمایی غنی\"}"}]}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        createTestProject("proj_character_image_prompt_ai_test")
        val projectDnaRepository = buildTestProjectDnaRepository()
        projectDnaRepository.saveProjectDna(defaultProjectDna("proj_character_image_prompt_ai_test") { "dna_image_prompt_ai_success_test" }).getOrThrow()
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_character_image_prompt_ai_test",
            repository = AssetRepository(injectedDatabase.assetDao()),
            secureKeyRepository = secureKeyRepository,
            httpClientEngine = engine,
            projectDnaRepository = projectDnaRepository,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        awaitCondition(vm.projectDna) { it != null }
        vm.setName("Jane Doe")
        vm.setAgeRange("30s")
        awaitCondition(vm.apiKeySavedForTranslationProfile) { it }

        vm.generateCharacterBaseImagePromptWithAi().join()

        assertEquals("a rich cinematic prompt", vm.imagePromptAi.value)
        assertEquals("یک پرامپت سینمایی غنی", vm.imagePromptFaPreview.value)
        assertNotNull(vm.imagePromptGeneratedAt.value)
        assertNull(vm.imagePromptAiError.value)
    }

    @Test
    fun `isImagePromptStale is true only when a prompt exists and the character was loaded with a newer updatedAt`() = runBlocking {
        assertFalse(
            "بدون هیچ imagePromptGeneratedAt ای، قدیمی‌بودن معنا ندارد",
            viewModel.isImagePromptStale(null)
        )

        val repository = AssetRepository(injectedDatabase.assetDao())
        repository.saveCharacterAsset(
            "proj_character_stale_test",
            CharacterAsset(
                assetId = "char_stale_test",
                characterTier = CharacterTier.MAIN,
                name = "Jane Doe",
                physicalAppearance = PhysicalAppearance(ageRange = "30s", gender = Gender.OTHER),
                outfits = emptyList(),
                updatedAt = 2_000L
            )
        ).getOrThrow()
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_character_stale_test",
            repository = repository,
            existingAssetId = "char_stale_test",
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        awaitCondition(vm.name) { it == "Jane Doe" }

        assertTrue(
            "وقتی پرامپت قبل از آخرین ویرایش Asset ساخته شده، باید قدیمی محسوب شود",
            vm.isImagePromptStale(1_000L)
        )
        assertFalse(
            "وقتی پرامپت بعد از آخرین ویرایش Asset ساخته شده، نباید قدیمی محسوب شود",
            vm.isImagePromptStale(3_000L)
        )
    }

    // اتصال Rule های یتیم ADR-132 (ADR-136): generatedPrompt به این توابع فقط
    // بعد از تولید واقعی موجود است — پس اینجا (نه در ImagePromptValidationTest.kt)
    // با generate...Quick() واقعی روی ViewModel تأیید می‌شود، نه فراخوانی مستقیم
    // تابع خالص دامنه.

    @Test
    fun `generateCharacterBaseImagePromptQuick with an empty age range populates imagePromptValidationIssues`() = runBlocking {
        createTestProject("proj_character_validation_test")
        val projectDnaRepository = buildTestProjectDnaRepository()
        projectDnaRepository.saveProjectDna(defaultProjectDna("proj_character_validation_test") { "dna_character_validation_test" }).getOrThrow()
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_character_validation_test",
            repository = AssetRepository(injectedDatabase.assetDao()),
            projectDnaRepository = projectDnaRepository,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        CoroutineScope(Dispatchers.Unconfined).launch { vm.characterBaseImagePromptPreview.collect {} }
        awaitCondition(vm.projectDna) { it != null }
        vm.setName("Jane Doe")
        // ageRange عمداً خالی می‌ماند — همان ورودی ناقصی که Rule
        // validateCharacterImagePromptInputs روی physicalAppearance.ageRange
        // بررسی می‌کند.
        shadowOf(Looper.getMainLooper()).idle()

        vm.generateCharacterBaseImagePromptQuick()

        assertTrue(
            "با physicalAppearance.ageRange خالی، imagePromptValidationIssues نباید خالی بماند",
            vm.imagePromptValidationIssues.value.isNotEmpty()
        )
    }

    @Test
    fun `generateOutfitImagePromptQuick with a style keyword conflict populates that outfit's imagePromptValidationIssues`() = runBlocking {
        createTestProject("proj_outfit_validation_test")
        val projectDnaRepository = buildTestProjectDnaRepository()
        // سبک پروژه عمداً PHOTOREALISTIC ست شد و توضیح Outfit شامل کلیدواژه‌ی
        // «anime» است — دقیقاً تناقض کلیدواژه‌ای که validateStyleKeywordConflict
        // (ImagePromptValidation.kt، فراخوانی‌شده از دل validateOutfitImagePromptInputs)
        // بررسی می‌کند.
        val dna = defaultProjectDna("proj_outfit_validation_test") { "dna_outfit_validation_test" }
            .let { it.copy(coreIdentity = it.coreIdentity.copy(dominantVisualStyle = VisualStyle.PHOTOREALISTIC)) }
        projectDnaRepository.saveProjectDna(dna).getOrThrow()
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_outfit_validation_test",
            repository = AssetRepository(injectedDatabase.assetDao()),
            projectDnaRepository = projectDnaRepository,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        awaitCondition(vm.projectDna) { it != null }
        vm.setName("Jane Doe")
        vm.addOutfit("Convention Outfit", "anime style costume")

        vm.generateOutfitImagePromptQuick(1)

        val outfits = vm.outfits.value
        assertTrue(
            "با تناقض سبک فوتورئال/انیمیشنی، imagePromptValidationIssues همان Outfit نباید خالی بماند",
            vm.outfitImagePromptValidationIssues.value[outfits[1].id].orEmpty().isNotEmpty()
        )
        assertTrue(
            "تولید پرامپت برای Outfit دوم نباید imagePromptValidationIssues Outfit اول (پیش‌فرض) را دست بزند",
            vm.outfitImagePromptValidationIssues.value[outfits[0].id].orEmpty().isEmpty()
        )
    }

    // فیچر مستقل جدید «آپلود عکس مرجع واقعی Asset» — زیرقدم ۱ از ۳ (ADR-137):
    // هم‌الگو دقیق با تست‌های معادل LocationAssetFormViewModelTest.kt/
    // ObjectAssetFormViewModelTest.kt.

    @Test
    fun `addReferenceImage appends an entry and removeReferenceImage removes it by index`() = runBlocking {
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_character_reference_image_test",
            repository = AssetRepository(injectedDatabase.assetDao()),
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )

        vm.addReferenceImage("content://media/external/images/1")
        vm.addReferenceImage("content://media/external/images/2")

        assertEquals(2, vm.referenceImages.value.size)
        assertEquals("content://media/external/images/1", vm.referenceImages.value[0].localFilePath)
        assertEquals("content://media/external/images/2", vm.referenceImages.value[1].localFilePath)

        vm.removeReferenceImage(0)

        assertEquals(1, vm.referenceImages.value.size)
        assertEquals("content://media/external/images/2", vm.referenceImages.value[0].localFilePath)
    }

    @Test
    fun `save persists referenceImages with a description recomputed from the current name and physicalAppearance at save time`() = runBlocking {
        val repository = AssetRepository(injectedDatabase.assetDao())
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_character_reference_image_save_test",
            repository = repository,
            idProvider = { "char_reference_image_save_test" },
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        CoroutineScope(Dispatchers.Unconfined).launch { vm.canSave.collect {} }
        vm.addReferenceImage("content://media/external/images/1")
        vm.setName("Detective John")
        vm.setAgeRange("30-40")
        shadowOf(Looper.getMainLooper()).idle()

        vm.save()
        awaitCondition(vm.saveCompleted, timeoutMs = 10_000) { it }

        val loaded = repository.loadCharacterAssets(listOf("char_reference_image_save_test")).getOrThrow().single()
        assertEquals(1, loaded.referenceImages.size)
        assertEquals("content://media/external/images/1", loaded.referenceImages[0].localFilePath)
        assertEquals(
            "توضیح باید در لحظه‌ی save() از name/physicalAppearance.toPromptString() زنده‌ی فرم محاسبه شود، نه در لحظه‌ی addReferenceImage()",
            "Detective John — 30-40 other",
            loaded.referenceImages[0].description
        )
    }

    // یافته‌ی حیاتی چکاپ نهایی (ADR-143/144): تا این قدم validateCharacterUpdate
    // از هیچ ViewModel واقعی صدا زده نمی‌شد — این سه تست دقیقاً همان شکاف را
    // اثبات می‌کنند که حالا بسته شده: سطح FULL (Tier MAIN) تغییر ظاهر را واقعاً
    // مسدود می‌کند، سطح MEDIUM (Tier SECONDARY) فقط هشدار می‌دهد و ذخیره را
    // متوقف نمی‌کند، و یک کاراکتر تازه (بدون Asset بارگذاری‌شده) اصلاً هیچ
    // بررسی‌ای نمی‌بیند.

    @Test
    fun `FULL level blocks an appearance change at save time and never persists it`() = runBlocking {
        val repository = AssetRepository(injectedDatabase.assetDao())
        val original = CharacterAsset(
            assetId = "char_full_lock_test",
            characterTier = CharacterTier.MAIN,
            name = "Original Name",
            physicalAppearance = PhysicalAppearance(ageRange = "30s", gender = Gender.OTHER, height = "180cm"),
            outfits = listOf(Outfit(id = "outfit_1", name = "Default", description = "", isDefault = true)),
            updatedAt = 1_000L
        )
        repository.saveCharacterAsset("proj_full_lock_test", original).getOrThrow()
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_full_lock_test",
            repository = repository,
            existingAssetId = "char_full_lock_test",
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        CoroutineScope(Dispatchers.Unconfined).launch { vm.canSave.collect {} }
        CoroutineScope(Dispatchers.Unconfined).launch { vm.continuityLockLevel.collect {} }
        awaitCondition(vm.name) { it == "Original Name" }
        awaitCondition(vm.continuityLockLevel) { it == com.operaboys.cinemashotgenerator.domain.asset.CharacterContinuityLevel.FULL }
        vm.setHeight("190cm")
        shadowOf(Looper.getMainLooper()).idle()

        vm.save()

        assertFalse(
            "با appearance_lock فعال در سطح FULL، save() نباید واقعاً چیزی ذخیره کند",
            vm.saveCompleted.value
        )
        assertEquals(1, vm.continuityIssues.value.size)
        val issue = vm.continuityIssues.value.single()
        assertEquals(Severity.BLOCKING, issue.severity)
        assertEquals("این کاراکتر appearance_lock دارد؛ ظاهر پس از قفل‌شدن قابل تغییر نیست", issue.message)
        val stillOriginal = repository.loadCharacterAssets(listOf("char_full_lock_test")).getOrThrow().single()
        assertEquals(
            "مقدار قدیمی روی دیسک باید دست‌نخورده بماند — این دقیقاً همان تضمین Hard Lock است",
            "180cm",
            stillOriginal.physicalAppearance.height
        )
    }

    @Test
    fun `MEDIUM level only warns on an appearance change and still saves it`() = runBlocking {
        val repository = AssetRepository(injectedDatabase.assetDao())
        val original = CharacterAsset(
            assetId = "char_medium_warn_test",
            characterTier = CharacterTier.SECONDARY,
            name = "Sidekick",
            physicalAppearance = PhysicalAppearance(ageRange = "20s", gender = Gender.OTHER, height = "170cm"),
            outfits = listOf(Outfit(id = "outfit_1", name = "Default", description = "", isDefault = true)),
            updatedAt = 1_000L
        )
        repository.saveCharacterAsset("proj_medium_warn_test", original).getOrThrow()
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_medium_warn_test",
            repository = repository,
            existingAssetId = "char_medium_warn_test",
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        CoroutineScope(Dispatchers.Unconfined).launch { vm.canSave.collect {} }
        // یافته‌ی دیباگ این تست (systematic-debugging): continuityLockLevel هم
        // مثل canSave از combine(...).stateIn(WhileSubscribed) روی viewModelScope
        // واقعی ساخته می‌شود — بدون این Collector، مقدار همچنان روی پیش‌فرض اولیه‌ی
        // stateIn (FULL، برای CharacterTier.MAIN) می‌ماند، نه MEDIUM واقعی این
        // کاراکتر SECONDARY، و checkContinuityBeforeSave به‌اشتباه Blocked برمی‌گرداند.
        CoroutineScope(Dispatchers.Unconfined).launch { vm.continuityLockLevel.collect {} }
        awaitCondition(vm.name) { it == "Sidekick" }
        awaitCondition(vm.continuityLockLevel) { it == com.operaboys.cinemashotgenerator.domain.asset.CharacterContinuityLevel.MEDIUM }
        vm.setHeight("175cm")
        shadowOf(Looper.getMainLooper()).idle()

        vm.save()
        awaitCondition(vm.saveCompleted, timeoutMs = 10_000) { it }

        assertEquals(1, vm.continuityIssues.value.size)
        val issue = vm.continuityIssues.value.single()
        assertEquals(Severity.WARNING, issue.severity)
        assertEquals("تغییر ظاهر یک کاراکتر Medium‌-lock — ممکن است باعث ناسازگاری جزئی شود", issue.message)
        val updated = repository.loadCharacterAssets(listOf("char_medium_warn_test")).getOrThrow().single()
        assertEquals(
            "برخلاف FULL، یک Warning هرگز نباید جلوی ذخیره‌ی واقعی را بگیرد",
            "175cm",
            updated.physicalAppearance.height
        )
    }

    @Test
    fun `a brand new character with no loaded asset skips continuity checking entirely`() = runBlocking {
        val repository = AssetRepository(injectedDatabase.assetDao())
        val vm = CharacterAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_new_character_no_lock_test",
            repository = repository,
            idProvider = { "char_new_no_lock_test" },
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        CoroutineScope(Dispatchers.Unconfined).launch { vm.canSave.collect {} }
        vm.setName("Brand New Character")
        vm.setAgeRange("40s")
        vm.setHeight("200cm")
        shadowOf(Looper.getMainLooper()).idle()

        vm.save()
        awaitCondition(vm.saveCompleted, timeoutMs = 10_000) { it }

        assertTrue(
            "بدون Asset بارگذاری‌شده، هیچ‌چیز برای مقایسه/قفل‌شدن وجود ندارد",
            vm.continuityIssues.value.isEmpty()
        )
    }
}
