package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.SecureKeyRepository
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.asset.PropContinuityLevel
import com.operaboys.cinemashotgenerator.domain.asset.checkSimilarAssetName
import com.operaboys.cinemashotgenerator.domain.asset.validateBasePrompt
import com.operaboys.cinemashotgenerator.domain.asset.validateObjectAsset
import com.operaboys.cinemashotgenerator.domain.storybreakdown.BUILTIN_AI_CONNECTOR_PROFILES
import com.operaboys.cinemashotgenerator.domain.storybreakdown.GEMINI_API_PROFILE
import com.operaboys.cinemashotgenerator.domain.storybreakdown.translateToFarsi
import com.operaboys.cinemashotgenerator.domain.storybreakdown.validateApiKeyProvided
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// واحد ۱۶ فاز ۳ — قدم ۲ — بخش ج: ViewModel فرم ساخت Object. هم‌الگو با
// CharacterAssetFormViewModel/LocationAssetFormViewModel. برخلاف آن دو، اینجا
// یک Rule Blocking واقعی (Rule 10: size/materialAndColor الزامی، AssetValidation.kt)
// وجود دارد — validationIssues زنده روی هر تغییر فیلد محاسبه می‌شود (هم‌الگو با
// validateColorPalette زنده‌ی DnaTabContent.kt)، و دکمه‌ی ذخیره تا رفع Blocking
// غیرفعال می‌ماند؛ طبق تست الزامی این قدم («یک Rule Blocking نمایش داده شود»).
class ObjectAssetFormViewModel(
    application: Application,
    private val projectId: String,
    private val repository: AssetRepository = AssetRepository(AppDatabase.getInstance(application).assetDao()),
    private val idProvider: () -> String = { generateAssetFormId("obj") },
    // رفع G7 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md): هم‌الگو
    // دقیق با CharacterAssetFormViewModel.existingAssetId.
    private val existingAssetId: String? = null,
    // فیچر مستقل «ترجمه‌ی مجدد با AI» (ADR-124) — هم‌الگو دقیق با
    // CharacterAssetFormViewModel.
    private val secureKeyRepository: SecureKeyRepository = SecureKeyRepository(application),
    private val httpClientEngine: HttpClientEngine = OkHttp.create(),
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    // GENERAL_PROP به‌عنوان پیش‌فرض محافظه‌کارانه — هم‌راستا با همان انتخاب
    // محافظه‌کارانه‌ی مستندشده در StoryToDomainMapper.kt برای این enum.
    private val _subtype = MutableStateFlow(ObjectSubtype.GENERAL_PROP)
    val subtype: StateFlow<ObjectSubtype> = _subtype.asStateFlow()

    private val _size = MutableStateFlow("")
    val size: StateFlow<String> = _size.asStateFlow()
    private val _materialAndColor = MutableStateFlow("")
    val materialAndColor: StateFlow<String> = _materialAndColor.asStateFlow()
    private val _specialTrait = MutableStateFlow("")
    val specialTrait: StateFlow<String> = _specialTrait.asStateFlow()
    private val _basePrompt = MutableStateFlow("")
    val basePrompt: StateFlow<String> = _basePrompt.asStateFlow()

    // سیستم Preview دوزبانه‌ی پرامپت — قدم ۳ از ۳ زیرقدم، پایانی (ADR-123):
    // هم‌الگو دقیق با basePrompt بالا.
    private val _descriptionFaPreview = MutableStateFlow("")
    val descriptionFaPreview: StateFlow<String> = _descriptionFaPreview.asStateFlow()

    // فیچر مستقل «ترجمه‌ی مجدد با AI» (ADR-124) — هم‌الگو دقیق با
    // CharacterAssetFormViewModel.
    private val _selectedTranslationProfileId = MutableStateFlow(GEMINI_API_PROFILE.profileId)
    val selectedTranslationProfileId: StateFlow<String> = _selectedTranslationProfileId.asStateFlow()

    private val _apiKeySavedForTranslationProfile = MutableStateFlow(false)
    val apiKeySavedForTranslationProfile: StateFlow<Boolean> = _apiKeySavedForTranslationProfile.asStateFlow()

    private val _translationInProgress = MutableStateFlow(false)
    val translationInProgress: StateFlow<Boolean> = _translationInProgress.asStateFlow()

    private val _translationError = MutableStateFlow<String?>(null)
    val translationError: StateFlow<String?> = _translationError.asStateFlow()

    /** سطح تداوم ObjectAsset فقط یک مقدار دارد (FORM) — ثابت، بدون کنترل تعاملی. */
    val continuityLockLevel: PropContinuityLevel = PropContinuityLevel.FORM

    private fun buildPreviewAsset(): ObjectAsset = ObjectAsset(
        assetId = "preview",
        name = _name.value,
        description = _description.value,
        subtype = _subtype.value,
        size = _size.value,
        materialAndColor = _materialAndColor.value,
        specialTrait = _specialTrait.value.ifBlank { null },
        basePrompt = _basePrompt.value.ifBlank { null },
        continuityLockLevel = continuityLockLevel,
        descriptionFaPreview = _descriptionFaPreview.value.ifBlank { null }
    )

    // رفع یافته‌ی G14 «کاندید وصل آینده» (ADR-064، ADR-092): هم‌الگو دقیق با
    // CharacterAssetFormViewModel.existingNames.
    private val existingNames = repository.loadAllObjectAssets(projectId)
        .map { list -> list.filter { it.assetId != existingAssetId }.map { it.name } }

    val validationIssues: StateFlow<List<ValidationIssue>> = combine(_name, _size, _materialAndColor, _basePrompt, existingNames) { name, _, _, _, names ->
        validateObjectAsset(buildPreviewAsset()) + listOfNotNull(
            validateBasePrompt(_basePrompt.value.ifBlank { null }),
            name.takeIf { it.isNotBlank() }?.let { checkSimilarAssetName(it, names) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saveCompleted = MutableStateFlow(false)
    val saveCompleted: StateFlow<Boolean> = _saveCompleted.asStateFlow()

    val canSave: StateFlow<Boolean> = combine(_name, validationIssues) { name, issues ->
        name.isNotBlank() && issues.none { it.severity == Severity.BLOCKING }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        existingAssetId?.let { id ->
            ioScope.launch {
                repository.loadObjectAssets(listOf(id)).getOrNull()?.firstOrNull()?.let { asset -> applyLoadedAsset(asset) }
            }
        }
        ioScope.launch { refreshApiKeySavedForTranslation(_selectedTranslationProfileId.value) }
    }

    private suspend fun refreshApiKeySavedForTranslation(profileId: String) {
        _apiKeySavedForTranslationProfile.value = runCatching { secureKeyRepository.hasApiKey(profileId) }.getOrDefault(false)
    }

    fun selectTranslationProfile(profileId: String): Job {
        _selectedTranslationProfileId.value = profileId
        return ioScope.launch { refreshApiKeySavedForTranslation(profileId) }
    }

    /** description (متن انگلیسی منبع) به translateToFarsi داده می‌شود — هم‌الگو دقیق با CharacterAssetFormViewModel.retranslate. */
    fun retranslate(): Job {
        if (_translationInProgress.value) return Job().apply { complete() }
        val profile = BUILTIN_AI_CONNECTOR_PROFILES.firstOrNull { it.profileId == _selectedTranslationProfileId.value }
            ?: return Job().apply { complete() }
        _translationError.value = null
        return ioScope.launch {
            val apiKey = secureKeyRepository.loadApiKey(profile.profileId)
            if (apiKey == null || validateApiKeyProvided(apiKey) != null) {
                _translationError.value = "ابتدا کلید API را در تنظیمات وارد کنید"
                return@launch
            }
            _translationInProgress.value = true
            val result = translateToFarsi(_description.value, profile, apiKey, httpClientEngine)
            _translationInProgress.value = false
            result.fold(
                onSuccess = { translated -> _descriptionFaPreview.value = translated },
                onFailure = { _translationError.value = it.message ?: "درخواست به AI Connector با خطا مواجه شد" }
            )
        }
    }

    private fun applyLoadedAsset(asset: ObjectAsset) {
        _name.value = asset.name
        _description.value = asset.description
        _subtype.value = asset.subtype
        _size.value = asset.size
        _materialAndColor.value = asset.materialAndColor
        _specialTrait.value = asset.specialTrait.orEmpty()
        _basePrompt.value = asset.basePrompt.orEmpty()
        _descriptionFaPreview.value = asset.descriptionFaPreview.orEmpty()
    }

    fun setName(value: String) { _name.value = value }
    fun setDescription(value: String) { _description.value = value }
    fun setSubtype(value: ObjectSubtype) { _subtype.value = value }
    fun setSize(value: String) { _size.value = value }
    fun setMaterialAndColor(value: String) { _materialAndColor.value = value }
    fun setSpecialTrait(value: String) { _specialTrait.value = value }
    fun setBasePrompt(value: String) { _basePrompt.value = value }
    fun setDescriptionFaPreview(value: String) { _descriptionFaPreview.value = value }

    fun save() {
        if (!canSave.value) return
        val asset = buildPreviewAsset().copy(assetId = existingAssetId ?: idProvider())
        ioScope.launch {
            repository.saveObjectAsset(projectId, asset)
            _saveCompleted.value = true
        }
    }

    companion object {
        fun factory(
            application: Application,
            projectId: String,
            repository: AssetRepository? = null,
            existingAssetId: String? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (repository != null) ObjectAssetFormViewModel(application, projectId, repository, existingAssetId = existingAssetId)
                        else ObjectAssetFormViewModel(application, projectId, existingAssetId = existingAssetId)
                    ) as T
            }
    }
}
