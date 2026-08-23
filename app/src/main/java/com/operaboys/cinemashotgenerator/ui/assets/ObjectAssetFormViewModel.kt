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
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.asset.PropContinuityLevel
import com.operaboys.cinemashotgenerator.domain.asset.ReferenceImage
import com.operaboys.cinemashotgenerator.domain.asset.buildObjectImagePrompt
import com.operaboys.cinemashotgenerator.domain.asset.checkSimilarAssetName
import com.operaboys.cinemashotgenerator.domain.asset.generateImagePromptWithAi
import com.operaboys.cinemashotgenerator.domain.asset.styleTokensForImagePrompt
import com.operaboys.cinemashotgenerator.domain.asset.validateBasePrompt
import com.operaboys.cinemashotgenerator.domain.asset.validateObjectAsset
import com.operaboys.cinemashotgenerator.domain.asset.validateObjectImagePromptInputs
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
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

    // اتصال Rule یتیم ADR-132 (ADR-136): نتیجه‌ی validateObjectImagePromptInputs
    // روی آخرین پرامپت تولیدشده — Warning-only، هیچ دکمه‌ای را مسدود نمی‌کند.
    private val _imagePromptValidationIssues = MutableStateFlow<List<ValidationIssue>>(emptyList())
    val imagePromptValidationIssues: StateFlow<List<ValidationIssue>> = _imagePromptValidationIssues.asStateFlow()

    /** هم‌الگو دقیق با CharacterAssetFormViewModel.loadedUpdatedAt (ADR-134). */
    private var loadedUpdatedAt: Long? = null

    // فیچر مستقل جدید «آپلود عکس مرجع واقعی Asset» — زیرقدم ۱ از ۳ (ADR-137):
    // description هر ReferenceImage عمداً همیشه در save() (پایین‌تر) از روی
    // وضعیت فعلی فرم بازمحاسبه می‌شود، نه در همین‌جا هنگام افزودن — هم‌الگو دقیق
    // با CharacterAssetFormViewModel.
    private val _referenceImages = MutableStateFlow<List<ReferenceImage>>(emptyList())
    val referenceImages: StateFlow<List<ReferenceImage>> = _referenceImages.asStateFlow()

    fun addReferenceImage(uri: String) {
        _referenceImages.value = _referenceImages.value + ReferenceImage(localFilePath = uri, description = "")
    }

    fun removeReferenceImage(index: Int) {
        val current = _referenceImages.value
        if (index !in current.indices) return
        _referenceImages.value = current.filterIndexed { i, _ -> i != index }
    }

    /**
     * buildObjectImagePrompt (ADR-132) فقط description/size/materialAndColor/
     * specialTrait/basePrompt را می‌خواند (نه name/subtype/...، تأییدشده مستقیم
     * با خواندن ImagePromptEngine.kt) — دقیقاً ۵ فیلد، پس (برخلاف
     * LocationAssetFormViewModel) یک combine تک‌مرحله‌ای کافی است.
     */
    private val objectSnapshot: StateFlow<ObjectAsset> = combine(
        _description, _size, _materialAndColor, _specialTrait, _basePrompt
    ) { description, size, materialAndColor, specialTrait, basePrompt ->
        ObjectAsset(
            assetId = existingAssetId ?: "preview",
            name = "",
            description = description,
            subtype = ObjectSubtype.GENERAL_PROP,
            size = size,
            materialAndColor = materialAndColor,
            specialTrait = specialTrait.ifBlank { null },
            basePrompt = basePrompt.ifBlank { null }
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        ObjectAsset(assetId = "preview", name = "", description = "", subtype = ObjectSubtype.GENERAL_PROP, size = "", materialAndColor = "")
    )

    /** پیش‌نمایش زنده‌ی پرامپت Template — بدون دکمه، بدون فراخوان AI. */
    val imagePromptPreview: StateFlow<String?> = combine(objectSnapshot, _projectDna) { objectAsset, dna ->
        dna?.let { buildObjectImagePrompt(objectAsset, it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    private fun applyLoadedAsset(asset: ObjectAsset) {
        _name.value = asset.name
        _description.value = asset.description
        _subtype.value = asset.subtype
        _size.value = asset.size
        _materialAndColor.value = asset.materialAndColor
        _specialTrait.value = asset.specialTrait.orEmpty()
        _basePrompt.value = asset.basePrompt.orEmpty()
        _descriptionFaPreview.value = asset.descriptionFaPreview.orEmpty()
        // فیچر مستقل جدید «آپلود عکس مرجع واقعی Asset» — زیرقدم ۱ از ۳ (ADR-137).
        _referenceImages.value = asset.referenceImages
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
        val prompt = buildObjectImagePrompt(objectSnapshot.value, dna)
        _imagePromptQuick.value = prompt
        _imagePromptGeneratedAt.value = System.currentTimeMillis()
        _imagePromptValidationIssues.value = validateObjectImagePromptInputs(objectSnapshot.value, dna, prompt)
    }

    /**
     * پرامپت حرفه‌ای (با AI) — هم‌الگو دقیق با
     * generateCharacterBaseImagePromptWithAi (ADR-134)؛ نام تابع عمداً از
     * generateImagePromptWithAi وارداتی دامنه متفاوت گرفته شد (هم‌راستا با
     * همان تصمیم مستقل LocationAssetFormViewModel، برای خوانایی/عدم ابهام).
     */
    fun generateObjectImagePromptWithAi(): Job {
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
            val templatePrompt = buildObjectImagePrompt(objectSnapshot.value, dna)
            val result = generateImagePromptWithAi(templatePrompt, styleTokensForImagePrompt(dna), profile, apiKey, httpClientEngine)
            _imagePromptAiInProgress.value = false
            result.fold(
                onSuccess = { response ->
                    _imagePromptAi.value = response.imagePromptEn
                    _imagePromptFaPreview.value = response.imagePromptFa
                    _imagePromptGeneratedAt.value = System.currentTimeMillis()
                    _imagePromptValidationIssues.value = validateObjectImagePromptInputs(objectSnapshot.value, dna, response.imagePromptEn)
                },
                onFailure = { _imagePromptAiError.value = it.message ?: "درخواست به AI Connector با خطا مواجه شد" }
            )
        }
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
        // فیچر مستقل جدید «آپلود عکس مرجع واقعی Asset» — زیرقدم ۱ از ۳ (ADR-137):
        // هم‌الگو دقیق با CharacterAssetFormViewModel.save.
        val referenceImageDescription = "${_name.value} — ${_materialAndColor.value}"
        val referenceImages = _referenceImages.value.map { it.copy(description = referenceImageDescription) }
        val asset = buildPreviewAsset().copy(
            assetId = existingAssetId ?: idProvider(),
            referenceImages = referenceImages,
            // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۵ از ۵ (ADR-135):
            // هم‌الگو دقیق با CharacterAssetFormViewModel.save (ADR-134).
            imagePromptQuick = _imagePromptQuick.value,
            imagePromptAi = _imagePromptAi.value,
            imagePromptFaPreview = _imagePromptFaPreview.value,
            imagePromptGeneratedAt = _imagePromptGeneratedAt.value,
            updatedAt = System.currentTimeMillis()
        )
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
            existingAssetId: String? = null,
            projectDnaRepository: ProjectDnaRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (repository != null || projectDnaRepository != null) {
                            ObjectAssetFormViewModel(
                                application = application,
                                projectId = projectId,
                                repository = repository ?: AssetRepository(AppDatabase.getInstance(application).assetDao()),
                                existingAssetId = existingAssetId,
                                projectDnaRepository = projectDnaRepository ?: ProjectDnaRepository(AppDatabase.getInstance(application).projectDnaDao())
                            )
                        } else {
                            ObjectAssetFormViewModel(application, projectId, existingAssetId = existingAssetId)
                        }
                    ) as T
            }
    }
}
