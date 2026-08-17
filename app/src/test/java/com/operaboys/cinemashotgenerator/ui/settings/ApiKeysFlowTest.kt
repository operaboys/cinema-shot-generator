package com.operaboys.cinemashotgenerator.ui.settings

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.SecureKeyRepository
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.storybreakdown.CLAUDE_API_PROFILE
import com.operaboys.cinemashotgenerator.ui.storybreakdown.AI_BREAKDOWN_GENERATE_PROMPT_BUTTON_TAG
import com.operaboys.cinemashotgenerator.ui.storybreakdown.AI_BREAKDOWN_SEND_AUTOMATICALLY_BUTTON_TAG
import com.operaboys.cinemashotgenerator.ui.storybreakdown.AI_BREAKDOWN_STORY_FIELD_TAG
import com.operaboys.cinemashotgenerator.ui.storybreakdown.AiStoryBreakdownScreen
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
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

// G2 قدم ۳ از ۳ (ADR-101) — تست‌های end-to-end واقعی UI: (الف) ذخیره‌ی کلید در
// Settings واقعاً از طریق SecureKeyRepository قابل بازیابی است و وضعیت نمایشی
// را عوض می‌کند؛ (ب) دکمه‌ی «ارسال خودکار» در AiStoryBreakdownScreen واقعاً
// enabled/disabled بودنش به همان کلید ذخیره‌شده وابسته است — شرط سخت‌گیرانه‌ی
// محصولی («تا کلید ذخیره نشده، دکمه در سطح UI غیرفعال است، نه فقط خطای بعد از
// کلیک») اینجا در سطح UI واقعی (نه فقط ViewModel) راستی‌آزمایی می‌شود.
//
// الگوی راه‌اندازی: رندر مستقیم صفحه (نه از طریق MainScaffold/AppNavHost —
// هردو خارج از Scope این قدم‌اند) — عیناً هم‌الگو با
// HomeScreenBackgroundImageTest.kt: composeRule.setContent { Theme { XScreen(...) } }
// با Repository های تست‌محور مستقیماً تزریق‌شده. SettingsScreen.kt/
// AiStoryBreakdownScreen.kt هر دو secureKeyRepository را دقیقاً برای همین
// سناریو تزریق‌پذیر کردند (بدون این پارامتر، MainScaffold نیازی به تغییر
// نداشت — طبق تصمیم مستندشده‌ی هر دو فایل).

private const val PROJECT_ID = "proj_api_keys_flow_test"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApiKeysFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dataStoreFileName: String
    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var database: AppDatabase
    private lateinit var secureKeyRepository: SecureKeyRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreFileName = "api_keys_flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(dataStoreFileName) }
        )
        workflowViewModel = WorkflowViewModel(
            application = context.applicationContext as Application,
            dataStore = dataStore,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        secureKeyRepository = SecureKeyRepository(context) { appContext ->
            appContext.getSharedPreferences("api_keys_flow_test_secure_prefs_${UUID.randomUUID()}", Context.MODE_PRIVATE)
        }
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        database.close()
    }

    private fun renderSettings() {
        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                SettingsScreen(
                    workflowViewModel = workflowViewModel,
                    onBack = {},
                    secureKeyRepository = secureKeyRepository
                )
            }
        }
    }

    private fun renderAiStoryBreakdown() {
        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                AiStoryBreakdownScreen(
                    projectId = PROJECT_ID,
                    language = Language.FA,
                    theme = AppTheme.DARK,
                    onBack = {},
                    onConfirmedAndSaved = {},
                    storyRepository = StoryRepository(database.storyDao(), database.storyBreakdownSessionDao()),
                    secureKeyRepository = secureKeyRepository,
                    httpClientEngine = MockEngine { respond(content = "{}", status = HttpStatusCode.OK) }
                )
            }
        }
    }

    /** یافته‌ی این قدم (هم‌الگو با SettingsFlowTest.kt): کارت کلید API هشتمین/آخرین کارت است — بدون performScrollTo() خارج از Viewport فعلی است و assertIsDisplayed()/performClick() شکست می‌خورد. */
    private fun existsInTree(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `saving an API key in Settings actually persists it via SecureKeyRepository and updates the status label`() = runBlocking {
        renderSettings()

        composeRule.onNodeWithTag(apiKeyStatusTag(CLAUDE_API_PROFILE.profileId)).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag(apiKeyFieldTag(CLAUDE_API_PROFILE.profileId)).performScrollTo().performTextInput("sk-ant-real-key")
        composeRule.onNodeWithTag(apiKeySaveButtonTag(CLAUDE_API_PROFILE.profileId)).performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { secureKeyRepository.loadApiKey(CLAUDE_API_PROFILE.profileId) } == "sk-ant-real-key"
        }
        assertEquals(
            "کلید باید از طریق واقعیِ همان SecureKeyRepository که UI به آن نوشته قابل بازیابی باشد",
            "sk-ant-real-key",
            secureKeyRepository.loadApiKey(CLAUDE_API_PROFILE.profileId)
        )

        composeRule.waitUntil(timeoutMillis = 5_000) { existsInTree(apiKeyDeleteButtonTag(CLAUDE_API_PROFILE.profileId)) }
    }

    @Test
    fun `deleting a saved API key in Settings actually removes it via SecureKeyRepository`() = runBlocking {
        secureKeyRepository.saveApiKey(CLAUDE_API_PROFILE.profileId, "sk-ant-to-delete")
        renderSettings()

        composeRule.waitUntil(timeoutMillis = 5_000) { existsInTree(apiKeyDeleteButtonTag(CLAUDE_API_PROFILE.profileId)) }
        composeRule.onNodeWithTag(apiKeyDeleteButtonTag(CLAUDE_API_PROFILE.profileId)).performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { secureKeyRepository.loadApiKey(CLAUDE_API_PROFILE.profileId) } == null
        }
    }

    // composeRule.setContent فقط یک‌بار در هر تست قابل‌فراخوانی است — پس این
    // سناریو («قبل از ذخیره غیرفعال، بعد از ذخیره فعال») به دو تست جدا با
    // Render مستقل تقسیم شده، نه یک تست با دو رندر.

    @Test
    fun `the automatic-send button in AiStoryBreakdown is disabled before any Claude key is saved`() {
        renderAiStoryBreakdown()

        composeRule.onNodeWithTag(AI_BREAKDOWN_STORY_FIELD_TAG).performScrollTo().performTextInput("A".repeat(60))
        composeRule.onNodeWithTag(AI_BREAKDOWN_GENERATE_PROMPT_BUTTON_TAG).performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { existsInTree(AI_BREAKDOWN_SEND_AUTOMATICALLY_BUTTON_TAG) }
        composeRule.onNodeWithTag(AI_BREAKDOWN_SEND_AUTOMATICALLY_BUTTON_TAG).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `the automatic-send button in AiStoryBreakdown is enabled once a Claude key has been saved`() {
        runBlocking { secureKeyRepository.saveApiKey(CLAUDE_API_PROFILE.profileId, "sk-ant-real-key") }
        renderAiStoryBreakdown()

        composeRule.onNodeWithTag(AI_BREAKDOWN_STORY_FIELD_TAG).performScrollTo().performTextInput("A".repeat(60))
        composeRule.onNodeWithTag(AI_BREAKDOWN_GENERATE_PROMPT_BUTTON_TAG).performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { existsInTree(AI_BREAKDOWN_SEND_AUTOMATICALLY_BUTTON_TAG) }
        composeRule.onNodeWithTag(AI_BREAKDOWN_SEND_AUTOMATICALLY_BUTTON_TAG).performScrollTo().assertIsEnabled()
    }
}
