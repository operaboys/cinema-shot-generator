package com.operaboys.cinemashotgenerator.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.repository.SecureKeyRepository
import com.operaboys.cinemashotgenerator.domain.storybreakdown.BUILTIN_AI_CONNECTOR_PROFILES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// G2 قدم ۳ از ۳ (ADR-101) — بخش الف: مدیریت کلید API در Settings.
//
// تصمیم: یک ViewModel کوچک و مجزا (نه افزودن به WorkflowViewModel). دلیل: خودِ
// کامنت سربرگ WorkflowViewModel.kt صریحاً می‌گوید این ViewModel فقط
// «language/theme/layout picks + WorkflowState اسکلت» را پوشش می‌دهد و «سایر
// State های همان بخش سند عمداً اینجا نیستند» — افزودن مدیریت کلید API (که پشت
// EncryptedSharedPreferences است، نه DataStore Preferences مثل بقیه‌ی
// WorkflowViewModel) این انضباط دامنه‌ی مستندشده را نقض می‌کرد. الگوی این فایل
// عیناً هم‌شکل AiStoryBreakdownViewModel/CharacterAssetFormViewModel است —
// Repository تزریق‌پذیر با پیش‌فرض واقعی، ioScope قابل Override برای تست.
//
// چندسرویسی طبق طراحی: وضعیت هر profileId در BUILTIN_AI_CONNECTOR_PROFILES
// مستقل نگه‌داشته می‌شود (Map<profileId, Boolean>) — SettingsScreen.kt خودش
// روی همین لیست پیمایش می‌کند، نه یک ردیف هاردکدشده‌ی Claude.

class ApiKeysViewModel(
    application: Application,
    private val secureKeyRepository: SecureKeyRepository = SecureKeyRepository(application),
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    private val _savedStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val savedStatus: StateFlow<Map<String, Boolean>> = _savedStatus.asStateFlow()

    init {
        ioScope.launch {
            // یافته‌ی واقعی راستی‌آزمایی این قدم (هم‌کلاس دقیق مشکلی که در
            // AiStoryBreakdownViewModel.claudeApiKeySaved رفع شد): SecureKeyRepository
            // پیش‌فرض تزریق‌نشده روی Robolectric واقعاً KeyStoreException پرتاب
            // می‌کند (طبق کامنت مستندشده‌ی خودِ SecureKeyRepository.kt/ADR-098) — و
            // چون تست‌های موجود SettingsFlowTest.kt این صفحه را از طریق MainScaffold
            // (بدون تزریق secureKeyRepository) رندر می‌کنند، بدون این runCatching
            // ۹ از ۱۰ تست آن فایل واقعاً شکست می‌خوردند (تأییدشده تجربی، نه فرضی).
            // fail-closed به false هم با شرط سخت‌گیرانه‌ی محصولی هم‌راستاست.
            val statuses = BUILTIN_AI_CONNECTOR_PROFILES.associate { profile ->
                profile.profileId to (runCatching { secureKeyRepository.hasApiKey(profile.profileId) }.getOrDefault(false))
            }
            _savedStatus.value = statuses
        }
    }

    /**
     * خالی/فقط‌فاصله نادیده گرفته می‌شود — هیچ کلید بی‌معنایی ذخیره نمی‌شود.
     *
     * Job برمی‌گرداند (هم‌الگو با OutputDeliveryViewModel.regenerate/exportOutput،
     * AiStoryBreakdownViewModel.confirmAndSave): SecureKeyRepository داخلاً
     * withContext(Dispatchers.IO) واقعی صدا می‌زند، پس فقط ioScopeOverride=Unconfined
     * کافی نیست تا تست بعد از این فراخوان مطمئن باشد ذخیره‌سازی واقعاً کامل شده —
     * تست باید .join() کند (یافته‌ی مستندشده‌ی همان دو تست دیگر).
     */
    fun saveApiKey(profileId: String, apiKey: String): Job {
        if (apiKey.isBlank()) return Job().apply { complete() }
        return ioScope.launch {
            secureKeyRepository.saveApiKey(profileId, apiKey)
            _savedStatus.value = _savedStatus.value + (profileId to true)
        }
    }

    fun deleteApiKey(profileId: String): Job {
        return ioScope.launch {
            secureKeyRepository.deleteApiKey(profileId)
            _savedStatus.value = _savedStatus.value + (profileId to false)
        }
    }

    companion object {
        fun factory(
            application: Application,
            secureKeyRepository: SecureKeyRepository? = null,
            ioScopeOverride: CoroutineScope? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (secureKeyRepository != null) {
                            ApiKeysViewModel(application, secureKeyRepository, ioScopeOverride)
                        } else {
                            ApiKeysViewModel(application, ioScopeOverride = ioScopeOverride)
                        }
                    ) as T
            }
    }
}
