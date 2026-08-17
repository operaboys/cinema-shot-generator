package com.operaboys.cinemashotgenerator.ui.settings

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.repository.SecureKeyRepository
import com.operaboys.cinemashotgenerator.domain.storybreakdown.CLAUDE_API_PROFILE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// G2 قدم ۳ از ۳ (ADR-101): تست‌های ApiKeysViewModel — هم‌الگو دقیق با
// SecureKeyRepositoryTest.kt (SharedPreferences معمولی تزریق‌شده، نه
// EncryptedSharedPreferences واقعی — Robolectric فاقد Provider واقعی
// AndroidKeyStore است، طبق کامنت مستندشده‌ی همان فایل/ADR-098) و با
// ioScopeOverride=Dispatchers.Unconfined (هم‌الگو با AiStoryBreakdownViewModelFactoryTest.kt)
// تا init{}/coroutine های launch شده به‌جای viewModelScope واقعی، هم‌زمان
// (synchronous) اجرا شوند و تست قابل‌اطمینان بماند.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApiKeysViewModelTest {

    private lateinit var secureKeyRepository: SecureKeyRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        secureKeyRepository = SecureKeyRepository(context) { appContext ->
            appContext.getSharedPreferences("api_keys_viewmodel_test_prefs", Context.MODE_PRIVATE)
        }
    }

    private fun buildViewModel(): ApiKeysViewModel {
        val application = ApplicationProvider.getApplicationContext<Application>()
        return ApiKeysViewModel(
            application = application,
            secureKeyRepository = secureKeyRepository,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun `savedStatus starts false for a profileId with no saved key`() {
        val viewModel = buildViewModel()

        assertFalse(viewModel.savedStatus.value[CLAUDE_API_PROFILE.profileId] == true)
    }

    @Test
    fun `saveApiKey persists the key via SecureKeyRepository and updates savedStatus to true`() = runBlocking {
        val viewModel = buildViewModel()

        viewModel.saveApiKey(CLAUDE_API_PROFILE.profileId, "sk-ant-real-key").join()

        assertTrue(viewModel.savedStatus.value[CLAUDE_API_PROFILE.profileId] == true)
        assertEquals(
            "کلید باید واقعاً از طریق SecureKeyRepository قابل بازیابی باشد، نه فقط در State محلی",
            "sk-ant-real-key",
            secureKeyRepository.loadApiKey(CLAUDE_API_PROFILE.profileId)
        )
    }

    @Test
    fun `saveApiKey with a blank key is ignored, no meaningless key is saved`() = runBlocking {
        val viewModel = buildViewModel()

        viewModel.saveApiKey(CLAUDE_API_PROFILE.profileId, "   ").join()

        assertFalse(viewModel.savedStatus.value[CLAUDE_API_PROFILE.profileId] == true)
        assertFalse(secureKeyRepository.hasApiKey(CLAUDE_API_PROFILE.profileId))
    }

    @Test
    fun `deleteApiKey removes the key via SecureKeyRepository and updates savedStatus to false`() = runBlocking {
        val viewModel = buildViewModel()
        viewModel.saveApiKey(CLAUDE_API_PROFILE.profileId, "sk-ant-real-key").join()
        assertTrue(viewModel.savedStatus.value[CLAUDE_API_PROFILE.profileId] == true)

        viewModel.deleteApiKey(CLAUDE_API_PROFILE.profileId).join()

        assertFalse(viewModel.savedStatus.value[CLAUDE_API_PROFILE.profileId] == true)
        assertFalse(secureKeyRepository.hasApiKey(CLAUDE_API_PROFILE.profileId))
    }

    @Test
    fun `saved status of one profileId does not affect another`() = runBlocking {
        val viewModel = buildViewModel()

        viewModel.saveApiKey("claude_api", "sk-claude-key").join()

        assertTrue(viewModel.savedStatus.value["claude_api"] == true)
        assertFalse(viewModel.savedStatus.value["gemini_api"] == true)
    }
}
