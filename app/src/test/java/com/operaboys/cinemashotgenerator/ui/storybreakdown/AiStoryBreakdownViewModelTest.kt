package com.operaboys.cinemashotgenerator.ui.storybreakdown

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.SecureKeyRepository
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.domain.storybreakdown.CLAUDE_API_PROFILE
import com.operaboys.cinemashotgenerator.domain.storybreakdown.OPENAI_API_PROFILE
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
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

// رفع G14 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، بخش الف،
// docs/adr/064-g14-orphaned-rules-decisions.md): validateTargetShotCountRange
// (Rule 2، PromptBuilder.kt) قبلاً هرگز صدا زده نمی‌شد — setTargetShotCount با
// coerceIn(1,150) مقدار خارج از محدوده را بی‌صدا کوتاه می‌کرد. این تست اثبات
// می‌کند اکنون: (۱) مقدار خام کاربر نگه داشته می‌شود (نه کوتاه‌سازی خاموش)، (۲)
// خطای واقعی این Rule در targetShotCountError ظاهر می‌شود، (۳) دکمه‌ی «تولید
// Prompt» (از طریق generatePrompt) تا وقتی خطا برطرف نشود مسدود است. مسیر
// «ورودی معتبر → تولید واقعی Prompt» از قبل در
// AiStoryBreakdownFlowTest.kt («happy path») پوشش داده شده — اینجا فقط رفتار
// تازه (بخش الف) بدون نیاز به Compose/ioScopeOverride تست می‌شود، چون
// setTargetShotCount/generatePrompt's guard هر دو synchronous هستند.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiStoryBreakdownViewModelTest {

    private lateinit var injectedDatabase: AppDatabase
    private lateinit var viewModel: AiStoryBreakdownViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        injectedDatabase = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java).allowMainThreadQueries().build()
        viewModel = AiStoryBreakdownViewModel(
            application = application,
            projectId = "proj_target_shot_count_test",
            storyRepository = StoryRepository(injectedDatabase.storyDao(), injectedDatabase.storyBreakdownSessionDao())
        )
    }

    @After
    fun tearDown() {
        injectedDatabase.close()
    }

    @Test
    fun `setTargetShotCount above 150 keeps the raw value and shows a real blocking error, not a silent clamp`() {
        viewModel.setTargetShotCount(200)

        assertEquals(200, viewModel.targetShotCount.value)
        assertNotNull(
            "طبق رفع G14، مقدار خارج از محدوده باید یک خطای واقعی نشان دهد، نه کوتاه‌سازی خاموش",
            viewModel.targetShotCountError.value
        )
    }

    @Test
    fun `setTargetShotCount below 1 keeps the raw value and shows a real blocking error, not a silent clamp`() {
        viewModel.setTargetShotCount(0)

        assertEquals(0, viewModel.targetShotCount.value)
        assertNotNull(viewModel.targetShotCountError.value)
    }

    @Test
    fun `setTargetShotCount within 1-150 clears the error, matching previous valid-input behavior`() {
        viewModel.setTargetShotCount(200)
        assertNotNull(viewModel.targetShotCountError.value)

        viewModel.setTargetShotCount(50)

        assertEquals(50, viewModel.targetShotCount.value)
        assertNull(viewModel.targetShotCountError.value)
    }

    @Test
    fun `generatePrompt is blocked while targetShotCountError is set`() {
        viewModel.setTargetShotCount(200)

        viewModel.generatePrompt()

        assertNull(
            "generatePrompt باید قبل از هرگونه تولید متن، به‌خاطر خطای Rule 2، بی‌اثر برگردد",
            viewModel.generatedPrompt.value
        )
    }

    // رفع G14 «کاندید قدم بعدی» (ADR-064 تصمیم ۱۲، ADR-092): Rule 1
    // (validateFreeformStoryLength) — هم‌الگو دقیق با تست‌های Rule 2 بالا.

    @Test
    fun `setFreeformStory with a too-short story shows a real blocking error`() {
        viewModel.setFreeformStory("short")

        assertNotNull(viewModel.freeformStoryError.value)
    }

    @Test
    fun `setFreeformStory with 50+ characters clears the error`() {
        viewModel.setFreeformStory("short")
        assertNotNull(viewModel.freeformStoryError.value)

        viewModel.setFreeformStory("A".repeat(60))

        assertNull(viewModel.freeformStoryError.value)
    }

    @Test
    fun `generatePrompt is blocked while freeformStoryError is set`() {
        viewModel.setTargetShotCount(10)
        viewModel.setFreeformStory("short")

        viewModel.generatePrompt()

        assertNull(
            "generatePrompt باید قبل از هرگونه تولید متن، به‌خاطر خطای Rule 1، بی‌اثر برگردد",
            viewModel.generatedPrompt.value
        )
    }

    // رفع G14 «کاندید رفع نزدیک» (ADR-064 تصمیم ۱۲، ADR-092): Rule 3
    // (validateHighShotCount) — روی همان فیلد targetShotCount، فقط Warning.

    @Test
    fun `setTargetShotCount above 40 shows a real warning, but does not set the blocking error`() {
        viewModel.setTargetShotCount(50)

        assertNotNull(viewModel.highShotCountWarning.value)
        // اثبات «فقط Warning، نه Blocking»: چون targetShotCountError (تنها Guard
        // واقعی generatePrompt، طبق تست‌های Rule 2 بالا) خالی می‌ماند، دکمه‌ی «تولید
        // Prompt» مسدود نمی‌شود — بدون نیاز به اجرای واقعی مسیر Async (که در این
        // فایل بدون ioScopeOverride قابل‌اطمینان نیست، طبق کامنت بالای فایل).
        assertNull("Rule 3 یک Warning است، نباید targetShotCountError را ست کند", viewModel.targetShotCountError.value)
    }

    @Test
    fun `setTargetShotCount at or below 40 clears the high shot count warning`() {
        viewModel.setTargetShotCount(50)
        assertNotNull(viewModel.highShotCountWarning.value)

        viewModel.setTargetShotCount(20)

        assertNull(viewModel.highShotCountWarning.value)
    }

    // رفع G14 «کاندید بهبود UX آینده» (ADR-064 تصمیم ۱۲، ADR-092): Rule 6
    // (validateChunksComplete).

    @Test
    fun `addChunk with a chunk ending in the CONTINUE marker shows a real warning, not silence`() {
        viewModel.setCurrentChunkInput("some ai response text...[CONTINUE]")

        viewModel.addChunk()

        assertNotNull(viewModel.chunksCompleteWarning.value)
    }

    @Test
    fun `addChunk with a complete chunk shows no warning`() {
        viewModel.setCurrentChunkInput("a complete, final response chunk")

        viewModel.addChunk()

        assertNull(viewModel.chunksCompleteWarning.value)
    }

    @Test
    fun `processResponse refreshes the chunks complete warning for the current input too`() {
        viewModel.setCurrentChunkInput("still going...[CONTINUE]")

        viewModel.processResponse()

        assertNotNull(viewModel.chunksCompleteWarning.value)
    }

    // G2 قدم ۳ از ۳ (ADR-101): تست‌های مسیر ۲ (ارسال خودکار) — apiKeySavedForSelectedProfile،
    // sendPromptAutomatically. هم‌الگو با AiConnectorTest.kt (MockEngine) و
    // SecureKeyRepositoryTest.kt (SharedPreferences معمولی تزریقی، نه AndroidKeyStore
    // واقعی). sendPromptAutomatically اکنون Job برمی‌گرداند (هم‌الگو با
    // confirmAndSave/OutputDeliveryViewModel.regenerate — یافته‌ی مستندشده‌ی همان
    // فایل) دقیقاً برای این‌که تست بتواند .join() کند و مطمئن شود کل زنجیره‌ی
    // async (که شامل withContext(Dispatchers.IO) واقعی داخل SecureKeyRepository است)
    // قبل از assert کامل شده. اما init{} که apiKeySavedForSelectedProfile را
    // بارگذاری می‌کند Job برنمی‌گرداند (داخل سازنده است، نه یک تابع صدازدنی از
    // بیرون) — برای آن یک انتظار محدود (awaitCondition، هم‌الگو با
    // composeRule.waitUntil در SettingsFlowTest.kt) استفاده شده؛ مقدار پیش‌فرض
    // false نیازی به انتظار ندارد چون MutableStateFlow(false) همان مقدار اولیه‌ی
    // هم‌زمان (synchronous) است.
    //
    // G2/ADR-102: claudeApiKeySaved (تک‌پروفایلی) با selectedProfileId/
    // apiKeySavedForSelectedProfile (چندپروفایلی، بازبینی موعود ADR-101) جایگزین
    // شد — تست‌های زیر انتخابگر واقعی (selectProfile) و ارسال موفق با پروفایل
    // OpenAI (نه فقط Claude) را هم اضافه می‌کنند تا اثبات شود کد این ViewModel
    // واقعاً پروفایل انتخاب‌شده را استفاده می‌کند، نه هاردکد باقی‌مانده.

    private fun buildTestSecureKeyRepository(prefsName: String): SecureKeyRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return SecureKeyRepository(context) { appContext ->
            appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        }
    }

    private fun <T> awaitCondition(flow: StateFlow<T>, timeoutMs: Long = 3000, predicate: (T) -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate(flow.value) && System.currentTimeMillis() < deadline) {
            Thread.sleep(5)
        }
        assertTrue("condition was never met within ${timeoutMs}ms, last value=${flow.value}", predicate(flow.value))
    }

    private fun buildAutoSendViewModel(
        projectId: String,
        secureKeyRepository: SecureKeyRepository,
        engine: MockEngine
    ): AiStoryBreakdownViewModel = AiStoryBreakdownViewModel(
        application = ApplicationProvider.getApplicationContext(),
        projectId = projectId,
        storyRepository = StoryRepository(injectedDatabase.storyDao(), injectedDatabase.storyBreakdownSessionDao()),
        secureKeyRepository = secureKeyRepository,
        httpClientEngine = engine,
        ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
    )

    private fun noopEngine() = MockEngine {
        respond(content = "{}", status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }

    @Test
    fun `apiKeySavedForSelectedProfile is false when no key has been saved`() {
        val secureKeyRepository = buildTestSecureKeyRepository("auto_send_vm_test_no_key_prefs")
        val vm = buildAutoSendViewModel("proj_claude_key_absent_test", secureKeyRepository, noopEngine())

        assertFalse(vm.apiKeySavedForSelectedProfile.value)
    }

    @Test
    fun `apiKeySavedForSelectedProfile becomes true when a Claude key was already saved before the ViewModel is created`() = runBlocking {
        val secureKeyRepository = buildTestSecureKeyRepository("auto_send_vm_test_has_key_prefs")
        secureKeyRepository.saveApiKey(CLAUDE_API_PROFILE.profileId, "sk-ant-real-key")
        val vm = buildAutoSendViewModel("proj_claude_key_present_test", secureKeyRepository, noopEngine())

        assertEquals(
            "پیش‌فرض selectedProfileId باید اولین پروفایل (Claude) باشد — همان رفتار قبلی، بدون Special-case",
            CLAUDE_API_PROFILE.profileId,
            vm.selectedProfileId.value
        )
        awaitCondition(vm.apiKeySavedForSelectedProfile) { it }
    }

    @Test
    fun `selectProfile switches selectedProfileId and apiKeySavedForSelectedProfile reflects only the newly-selected profile's own key`() = runBlocking {
        val secureKeyRepository = buildTestSecureKeyRepository("auto_send_vm_test_select_profile_prefs")
        secureKeyRepository.saveApiKey(CLAUDE_API_PROFILE.profileId, "sk-ant-real-key")
        val vm = buildAutoSendViewModel("proj_select_profile_test", secureKeyRepository, noopEngine())
        awaitCondition(vm.apiKeySavedForSelectedProfile) { it }

        vm.selectProfile(OPENAI_API_PROFILE.profileId).join()

        assertEquals(OPENAI_API_PROFILE.profileId, vm.selectedProfileId.value)
        assertFalse(
            "کلید فقط برای Claude ذخیره شده — انتخاب OpenAI نباید apiKeySavedForSelectedProfile را true نگه دارد",
            vm.apiKeySavedForSelectedProfile.value
        )

        secureKeyRepository.saveApiKey(OPENAI_API_PROFILE.profileId, "sk-openai-real-key")
        vm.selectProfile(OPENAI_API_PROFILE.profileId).join()

        assertTrue(vm.apiKeySavedForSelectedProfile.value)
    }

    @Test
    fun `sendPromptAutomatically without a saved key applies Rule 4, shows a hint, and never attempts an HTTP call`() = runBlocking {
        val secureKeyRepository = buildTestSecureKeyRepository("auto_send_vm_test_rule4_prefs")
        var engineCalled = false
        val engine = MockEngine {
            engineCalled = true
            respond(content = "{}", status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val vm = buildAutoSendViewModel("proj_send_no_key_test", secureKeyRepository, engine)
        vm.setTargetShotCount(5)
        vm.setFreeformStory("A".repeat(60))
        vm.generatePrompt()
        awaitCondition(vm.generatedPrompt) { it != null }

        vm.sendPromptAutomatically().join()

        assertFalse("بدون کلید ذخیره‌شده نباید هیچ تلاش HTTP واقعی انجام شود", engineCalled)
        assertNotNull(vm.autoSendError.value)
        assertEquals(BreakdownPhase.WRITE_STORY, vm.phase.value)
    }

    @Test
    fun `a successful automatic send goes through the same processAiResponse chain and reaches FINAL_REVIEW, just like manual paste`() = runBlocking {
        val secureKeyRepository = buildTestSecureKeyRepository("auto_send_vm_test_success_prefs")
        secureKeyRepository.saveApiKey(CLAUDE_API_PROFILE.profileId, "sk-ant-real-key")
        val innerBreakdownJson = """{"characters":[{"name":"Nora","description":"A cartographer","role":"main","gender":"female"}],"locations":[{"name":"Harbor","description":"A foggy harbor"}],"objects":[],"shots":[]}"""
        val escapedInner = innerBreakdownJson.replace("\"", "\\\"")
        val engine = MockEngine {
            respond(
                content = """{"content":[{"type":"text","text":"$escapedInner"}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val vm = buildAutoSendViewModel("proj_auto_send_success_test", secureKeyRepository, engine)
        vm.setTargetShotCount(5)
        vm.setFreeformStory("A".repeat(60))
        vm.generatePrompt()
        awaitCondition(vm.generatedPrompt) { it != null }

        vm.sendPromptAutomatically().join()

        assertEquals(
            "همان applyProcessAiResponseResult مسیر ۱ (Paste دستی) — پاسخ موفق باید فاز را به FINAL_REVIEW ببرد",
            BreakdownPhase.FINAL_REVIEW,
            vm.phase.value
        )
        assertNotNull(vm.breakdownResult.value)
        assertEquals(1, vm.breakdownResult.value!!.characters.size)
        assertEquals("Nora", vm.breakdownResult.value!!.characters.first().name)
        assertNull(vm.autoSendError.value)
        assertFalse(vm.autoSendInProgress.value)
    }

    @Test
    fun `a successful automatic send with the OpenAI profile selected also reaches FINAL_REVIEW through the same shared chain`() = runBlocking {
        val secureKeyRepository = buildTestSecureKeyRepository("auto_send_vm_test_openai_success_prefs")
        secureKeyRepository.saveApiKey(OPENAI_API_PROFILE.profileId, "sk-openai-real-key")
        val innerBreakdownJson = """{"characters":[{"name":"Nora","description":"A cartographer","role":"main","gender":"female"}],"locations":[{"name":"Harbor","description":"A foggy harbor"}],"objects":[],"shots":[]}"""
        val escapedInner = innerBreakdownJson.replace("\"", "\\\"")
        val engine = MockEngine {
            // فرمت واقعی OpenAI (choices[0].message.content) — نه content[0].text
            // Claude — تا اثبات شود انتخابگر واقعاً پروفایل درست را استفاده می‌کند.
            respond(
                content = """{"choices":[{"message":{"role":"assistant","content":"$escapedInner"}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val vm = buildAutoSendViewModel("proj_auto_send_openai_success_test", secureKeyRepository, engine)
        vm.selectProfile(OPENAI_API_PROFILE.profileId).join()
        vm.setTargetShotCount(5)
        vm.setFreeformStory("A".repeat(60))
        vm.generatePrompt()
        awaitCondition(vm.generatedPrompt) { it != null }

        vm.sendPromptAutomatically().join()

        assertEquals(BreakdownPhase.FINAL_REVIEW, vm.phase.value)
        assertNotNull(vm.breakdownResult.value)
        assertEquals("Nora", vm.breakdownResult.value!!.characters.first().name)
        assertNull(vm.autoSendError.value)
    }

    @Test
    fun `a failed automatic send shows a meaningful error and does not lock the user out of the manual path`() = runBlocking {
        val secureKeyRepository = buildTestSecureKeyRepository("auto_send_vm_test_failure_prefs")
        secureKeyRepository.saveApiKey(CLAUDE_API_PROFILE.profileId, "sk-ant-real-key")
        val engine = MockEngine {
            respond(
                content = """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val vm = buildAutoSendViewModel("proj_auto_send_failure_test", secureKeyRepository, engine)
        vm.setTargetShotCount(5)
        vm.setFreeformStory("A".repeat(60))
        vm.generatePrompt()
        awaitCondition(vm.generatedPrompt) { it != null }

        vm.sendPromptAutomatically().join()

        assertEquals(
            "شکست ارسال خودکار نباید کاربر را در فاز دیگری گیر بیندازد — باید بتواند فوری از مسیر ۱ (کپی) استفاده کند",
            BreakdownPhase.WRITE_STORY,
            vm.phase.value
        )
        assertNotNull(vm.autoSendError.value)
        assertTrue(vm.autoSendError.value!!.contains("invalid x-api-key"))
        assertFalse(vm.autoSendInProgress.value)
        assertNotNull(
            "مسیر ۱ (Copy) باید همچنان قابل‌استفاده بماند — generatedPrompt نباید پاک شود",
            vm.generatedPrompt.value
        )

        vm.clearAutoSendError()
        assertNull(vm.autoSendError.value)
    }
}
