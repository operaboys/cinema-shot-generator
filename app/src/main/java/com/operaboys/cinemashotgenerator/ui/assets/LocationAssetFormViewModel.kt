package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.SecureKeyRepository
import com.operaboys.cinemashotgenerator.domain.asset.Environment
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.LocationContinuityLevel
import com.operaboys.cinemashotgenerator.domain.asset.LocationType
import com.operaboys.cinemashotgenerator.domain.asset.buildLocationImagePrompt
import com.operaboys.cinemashotgenerator.domain.asset.checkSimilarAssetName
import com.operaboys.cinemashotgenerator.domain.asset.generateImagePromptWithAi
import com.operaboys.cinemashotgenerator.domain.asset.styleTokensForImagePrompt
import com.operaboys.cinemashotgenerator.domain.asset.validateBasePrompt
import com.operaboys.cinemashotgenerator.domain.asset.validateLocationImagePromptInputs
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.storybreakdown.BUILTIN_AI_CONNECTOR_PROFILES
import com.operaboys.cinemashotgenerator.domain.storybreakdown.GEMINI_API_PROFILE
import com.operaboys.cinemashotgenerator.domain.storybreakdown.translateToFarsi
import com.operaboys.cinemashotgenerator.domain.storybreakdown.validateApiKeyProvided
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

// واحد ۱۶ فاز ۳ — قدم ۲ — بخش ب: ViewModel فرم ساخت Location (شامل تصمیم F6).
// هم‌الگو با CharacterAssetFormViewModel.
//
// تصمیم F6 (نگهداری‌شده از ممیزی pre-unit16، تعدیل‌شده طبق یافته‌ی واقعی grep):
// دستور کار timeCompatibility/weatherCompatibility را از نوع List<Enum> فرض کرده
// بود؛ با grep مستقیم domain/asset/AssetModels.kt تأیید شد هر دو (و keyElements)
// از نوع List<String> (واژگان باز) هستند، نه یک enum بسته. به‌جای چندانتخابی روی
// مقادیر ثابت (که اصلاً برای این سه فیلد وجود ندارد)، ورودی «افزودن برچسب آزاد»
// پیاده شد (AssetFormTagListField در AssetFormSupport.kt) — کاربر متن دلخواه اضافه
// می‌کند. environment/keyElements هم به فرم اضافه شدند (نه Placeholder). LocationType
// (که در قدم قبل فقط نمایش/فیلتر بود) اولین‌بار در این قدم قابل‌ویرایش شد.

// فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۵ از ۵ (ADR-135): buildLocationImagePrompt
// (ADR-132) فقط description/environment/keyElements/basePrompt را می‌خواند (نه
// name/locationType/...، تأییدشده مستقیم با خواندن ImagePromptEngine.kt) —
// snapshot زنده‌ی Preview فقط همین ۵ فیلد را دنبال می‌کند. combine مستقیم ۶
// آرگومانی امکان ندارد؛ دو‌مرحله‌ای (۵+۱)، هم‌الگو با characterSnapshot
// CharacterAssetFormViewModel.kt (ADR-134).
private data class LocationSnapshotPartial(
    val description: String,
    val environmentType: String,
    val environmentSize: String,
    val environmentLighting: String,
    val keyElements: List<String>
)

class LocationAssetFormViewModel(
    application: Application,
    private val projectId: String,
    private val repository: AssetRepository = AssetRepository(AppDatabase.getInstance(application).assetDao()),
    private val idProvider: () -> String = { generateAssetFormId("loc") },
    // رفع G7 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md): هم‌الگو
    // دقیق با CharacterAssetFormViewModel.existingAssetId.
    private val existingAssetId: String? = null,
    // فیچر مستقل «ترجمه‌ی مجدد با AI» (ADR-124) — هم‌الگو دقیق با
    // CharacterAssetFormViewModel.
    private val secureKeyRepository: SecureKeyRepository = SecureKeyRepository(application),
    private val httpClientEngine: HttpClientEngine = OkHttp.create(),
    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۵ از ۵ (ADR-135): هم‌الگو دقیق
    // با CharacterAssetFormViewModel (ADR-134).
    private val projectDnaRepository: ProjectDnaRepository = ProjectDnaRepository(AppDatabase.getInstance(application).projectDnaDao()),
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _locationType = MutableStateFlow(LocationType.CUSTOM)
    val locationType: StateFlow<LocationType> = _locationType.asStateFlow()

    // پیش‌فرض‌های خنثی — دقیقاً هم‌ارزش deriveEnvironmentPlaceholder در
    // domain/storybreakdown/StoryToDomainMapper.kt (بازاستفاده‌ی همان مقادیر خنثی
    // برای یک LocationAsset تازه‌ساخته‌شده‌ی دستی هم منطقی است).
    private val _environmentType = MutableStateFlow("unspecified")
    val environmentType: StateFlow<String> = _environmentType.asStateFlow()
    private val _environmentSize = MutableStateFlow("medium")
    val environmentSize: StateFlow<String> = _environmentSize.asStateFlow()
    private val _environmentLighting = MutableStateFlow("natural")
    val environmentLighting: StateFlow<String> = _environmentLighting.asStateFlow()

    private val _timeCompatibility = MutableStateFlow<List<String>>(emptyList())
    val timeCompatibility: StateFlow<List<String>> = _timeCompatibility.asStateFlow()
    private val _weatherCompatibility = MutableStateFlow<List<String>>(emptyList())
    val weatherCompatibility: StateFlow<List<String>> = _weatherCompatibility.asStateFlow()
    private val _keyElements = MutableStateFlow<List<String>>(emptyList())
    val keyElements: StateFlow<List<String>> = _keyElements.asStateFlow()

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

    /** سطح تداوم LocationAsset فقط یک مقدار دارد (STYLE) — ثابت، بدون کنترل تعاملی. */
    val continuityLockLevel: LocationContinuityLevel = LocationContinuityLevel.STYLE

    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۵ از ۵ (ADR-135): هم‌الگو دقیق
    // با CharacterAssetFormViewModel (ADR-134).
    private val _projectDna = MutableStateFlow<ProjectDna?>(null)
    val projectDna: StateFlow<ProjectDna?> = _projectDna.asStateFlow()

    private val _imagePromptQuick = MutableStateFlow<String?>(null)
    val imagePromptQuick: StateFlow<String?> = _imagePromptQuick.asStateFlow()

    private val _imagePromptAi = MutableStateFlow<String?>(null)
    val imagePromptAi: StateFlow<String?> = _imagePromptAi.asStateFlow()

    private val _imagePromptFaPreview = MutableStateFlow<String?>(null)
    val imagePromptFaPreview: StateFlow<String?> = _imagePromptFaPreview.asStateFlow()

    private val _imagePromptGeneratedAt = MutableStateFlow<Long?>(null)
    val imagePromptGeneratedAt: StateFlow<Long?> = _imagePromptGeneratedAt.asStateFlow()

    private val _imagePromptAiInProgress = MutableStateFlow(false)
    val imagePromptAiInProgress: StateFlow<Boolean> = _imagePromptAiInProgress.asStateFlow()

    private val _imagePromptAiError = MutableStateFlow<String?>(null)
    val imagePromptAiError: StateFlow<String?> = _imagePromptAiError.asStateFlow()

    // اتصال Rule یتیم ADR-132 (ADR-136): نتیجه‌ی validateLocationImagePromptInputs
    // روی آخرین پرامپت تولیدشده — Warning-only، هیچ دکمه‌ای را مسدود نمی‌کند.
    private val _imagePromptValidationIssues = MutableStateFlow<List<ValidationIssue>>(emptyList())
    val imagePromptValidationIssues: StateFlow<List<ValidationIssue>> = _imagePromptValidationIssues.asStateFlow()

    /** هم‌الگو دقیق با CharacterAssetFormViewModel.loadedUpdatedAt (ADR-134). */
    private var loadedUpdatedAt: Long? = null

    private val locationSnapshot: StateFlow<LocationAsset> = combine(
        _description, _environmentType, _environmentSize, _environmentLighting, _keyElements
    ) { description, environmentType, environmentSize, environmentLighting, keyElements ->
        LocationSnapshotPartial(description, environmentType, environmentSize, environmentLighting, keyElements)
    }.let { partial ->
        combine(partial, _basePrompt) { p, basePrompt ->
            LocationAsset(
                assetId = existingAssetId ?: "preview",
                name = "",
                description = p.description,
                environment = Environment(type = p.environmentType, size = p.environmentSize, lightingCondition = p.environmentLighting),
                keyElements = p.keyElements,
                basePrompt = basePrompt.ifBlank { null }
            )
        }
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        LocationAsset(assetId = "preview", name = "", description = "", environment = Environment(type = "unspecified", size = "medium", lightingCondition = "natural"))
    )

    /** پیش‌نمایش زنده‌ی پرامپت Template — بدون دکمه، بدون فراخوان AI. */
    val imagePromptPreview: StateFlow<String?> = combine(locationSnapshot, _projectDna) { location, dna ->
        dna?.let { buildLocationImagePrompt(location, it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // رفع یافته‌ی G14 «کاندید وصل آینده» (ADR-064، ADR-092): هم‌الگو دقیق با
    // CharacterAssetFormViewModel.existingNames.
    private val existingNames = repository.loadAllLocationAssets(projectId)
        .map { list -> list.filter { it.assetId != existingAssetId }.map { it.name } }

    val validationIssues: StateFlow<List<ValidationIssue>> = combine(_name, _basePrompt, existingNames) { name, basePrompt, names ->
        listOfNotNull(
            validateBasePrompt(basePrompt.ifBlank { null }),
            name.takeIf { it.isNotBlank() }?.let { checkSimilarAssetName(it, names) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saveCompleted = MutableStateFlow(false)
    val saveCompleted: StateFlow<Boolean> = _saveCompleted.asStateFlow()

    val canSave: StateFlow<Boolean> = combine(_name, _description) { name, description -> name.isNotBlank() && description.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        existingAssetId?.let { id ->
            ioScope.launch {
                repository.loadLocationAssets(listOf(id)).getOrNull()?.firstOrNull()?.let { asset -> applyLoadedAsset(asset) }
            }
        }
        ioScope.launch { refreshApiKeySavedForTranslation(_selectedTranslationProfileId.value) }
        // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۵ از ۵ (ADR-135).
        ioScope.launch { _projectDna.value = projectDnaRepository.loadProjectDna(projectId).getOrNull() }
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

    private fun applyLoadedAsset(asset: LocationAsset) {
        _name.value = asset.name
        _description.value = asset.description
        _locationType.value = asset.locationType
        _environmentType.value = asset.environment.type
        _environmentSize.value = asset.environment.size
        _environmentLighting.value = asset.environment.lightingCondition
        _timeCompatibility.value = asset.timeCompatibility
        _weatherCompatibility.value = asset.weatherCompatibility
        _keyElements.value = asset.keyElements
        _basePrompt.value = asset.basePrompt.orEmpty()
        _descriptionFaPreview.value = asset.descriptionFaPreview.orEmpty()
        // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۵ از ۵ (ADR-135).
        _imagePromptQuick.value = asset.imagePromptQuick
        _imagePromptAi.value = asset.imagePromptAi
        _imagePromptFaPreview.value = asset.imagePromptFaPreview
        _imagePromptGeneratedAt.value = asset.imagePromptGeneratedAt
        loadedUpdatedAt = asset.updatedAt
    }

    /** هم‌الگو دقیق با CharacterAssetFormViewModel.isImagePromptStale (ADR-134). */
    fun isImagePromptStale(generatedAt: Long?): Boolean =
        generatedAt != null && (loadedUpdatedAt ?: 0) > generatedAt

    /** پرامپت سریع (بدون AI) — فوری، بدون فراخوان شبکه. */
    fun generateImagePromptQuick() {
        val dna = _projectDna.value ?: return
        val prompt = buildLocationImagePrompt(locationSnapshot.value, dna)
        _imagePromptQuick.value = prompt
        _imagePromptGeneratedAt.value = System.currentTimeMillis()
        _imagePromptValidationIssues.value = validateLocationImagePromptInputs(locationSnapshot.value, dna, prompt)
    }

    /**
     * پرامپت حرفه‌ای (با AI) — هم‌الگو دقیق با
     * generateCharacterBaseImagePromptWithAi (ADR-134). نام تابع عمداً از
     * generateImagePromptWithAi وارداتی دامنه (ImagePromptAiConnector.kt،
     * ADR-133) متفاوت گرفته شد — با اینکه هر دو با آرگومان‌های متفاوت به‌درستی
     * Resolve می‌شوند (تشخیص Kotlin بر مبنای Arity)، نام متفاوت خوانایی را
     * بیشتر و ریسک ابهام را صفر می‌کند؛ هم‌راستا با تصمیم مشابه زیرقدم ۴.
     */
    fun generateLocationImagePromptWithAi(): Job {
        if (_imagePromptAiInProgress.value) return Job().apply { complete() }
        val dna = _projectDna.value ?: return Job().apply { complete() }
        val profile = BUILTIN_AI_CONNECTOR_PROFILES.firstOrNull { it.profileId == _selectedTranslationProfileId.value }
            ?: return Job().apply { complete() }
        _imagePromptAiError.value = null
        return ioScope.launch {
            val apiKey = secureKeyRepository.loadApiKey(profile.profileId)
            if (apiKey == null || validateApiKeyProvided(apiKey) != null) {
                _imagePromptAiError.value = "ابتدا کلید API را در تنظیمات وارد کنید"
                return@launch
            }
            _imagePromptAiInProgress.value = true
            val templatePrompt = buildLocationImagePrompt(locationSnapshot.value, dna)
            val result = generateImagePromptWithAi(templatePrompt, styleTokensForImagePrompt(dna), profile, apiKey, httpClientEngine)
            _imagePromptAiInProgress.value = false
            result.fold(
                onSuccess = { response ->
                    _imagePromptAi.value = response.imagePromptEn
                    _imagePromptFaPreview.value = response.imagePromptFa
                    _imagePromptGeneratedAt.value = System.currentTimeMillis()
                    _imagePromptValidationIssues.value = validateLocationImagePromptInputs(locationSnapshot.value, dna, response.imagePromptEn)
                },
                onFailure = { _imagePromptAiError.value = it.message ?: "درخواست به AI Connector با خطا مواجه شد" }
            )
        }
    }

    fun setName(value: String) { _name.value = value }
    fun setDescription(value: String) { _description.value = value }
    fun setLocationType(value: LocationType) { _locationType.value = value }
    fun setEnvironmentType(value: String) { _environmentType.value = value }
    fun setEnvironmentSize(value: String) { _environmentSize.value = value }
    fun setEnvironmentLighting(value: String) { _environmentLighting.value = value }
    fun addTimeCompatibility(tag: String) { _timeCompatibility.value = _timeCompatibility.value + tag }
    fun removeTimeCompatibility(tag: String) { _timeCompatibility.value = _timeCompatibility.value - tag }
    fun addWeatherCompatibility(tag: String) { _weatherCompatibility.value = _weatherCompatibility.value + tag }
    fun removeWeatherCompatibility(tag: String) { _weatherCompatibility.value = _weatherCompatibility.value - tag }
    fun addKeyElement(tag: String) { _keyElements.value = _keyElements.value + tag }
    fun removeKeyElement(tag: String) { _keyElements.value = _keyElements.value - tag }
    fun setBasePrompt(value: String) { _basePrompt.value = value }
    fun setDescriptionFaPreview(value: String) { _descriptionFaPreview.value = value }

    fun save() {
        if (!canSave.value) return
        val asset = LocationAsset(
            assetId = existingAssetId ?: idProvider(),
            name = _name.value,
            description = _description.value,
            environment = Environment(type = _environmentType.value, size = _environmentSize.value, lightingCondition = _environmentLighting.value),
            locationType = _locationType.value,
            timeCompatibility = _timeCompatibility.value,
            weatherCompatibility = _weatherCompatibility.value,
            keyElements = _keyElements.value,
            basePrompt = _basePrompt.value.ifBlank { null },
            continuityLockLevel = continuityLockLevel,
            descriptionFaPreview = _descriptionFaPreview.value.ifBlank { null },
            // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۵ از ۵ (ADR-135):
            // هم‌الگو دقیق با CharacterAssetFormViewModel.save (ADR-134).
            imagePromptQuick = _imagePromptQuick.value,
            imagePromptAi = _imagePromptAi.value,
            imagePromptFaPreview = _imagePromptFaPreview.value,
            imagePromptGeneratedAt = _imagePromptGeneratedAt.value,
            updatedAt = System.currentTimeMillis()
        )
        ioScope.launch {
            repository.saveLocationAsset(projectId, asset)
            _saveCompleted.value = true
        }
    }

    companion object {
        fun factory(
            application: Application,
            projectId: String,
            repository: AssetRepository? = null,
            existingAssetId: String? = null,
            projectDnaRepository: ProjectDnaRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (repository != null || projectDnaRepository != null) {
                            LocationAssetFormViewModel(
                                application = application,
                                projectId = projectId,
                                repository = repository ?: AssetRepository(AppDatabase.getInstance(application).assetDao()),
                                existingAssetId = existingAssetId,
                                projectDnaRepository = projectDnaRepository ?: ProjectDnaRepository(AppDatabase.getInstance(application).projectDnaDao())
                            )
                        } else {
                            LocationAssetFormViewModel(application, projectId, existingAssetId = existingAssetId)
                        }
                    ) as T
            }
    }
}
