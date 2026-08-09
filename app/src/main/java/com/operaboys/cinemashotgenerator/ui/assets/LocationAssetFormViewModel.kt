package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.domain.asset.Environment
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.LocationContinuityLevel
import com.operaboys.cinemashotgenerator.domain.asset.LocationType
import com.operaboys.cinemashotgenerator.domain.asset.validateBasePrompt
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import kotlinx.coroutines.CoroutineScope
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
class LocationAssetFormViewModel(
    application: Application,
    private val projectId: String,
    private val repository: AssetRepository = AssetRepository(AppDatabase.getInstance(application).assetDao()),
    private val idProvider: () -> String = { generateAssetFormId("loc") },
    // رفع G7 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md): هم‌الگو
    // دقیق با CharacterAssetFormViewModel.existingAssetId.
    private val existingAssetId: String? = null,
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

    /** سطح تداوم LocationAsset فقط یک مقدار دارد (STYLE) — ثابت، بدون کنترل تعاملی. */
    val continuityLockLevel: LocationContinuityLevel = LocationContinuityLevel.STYLE

    val validationIssues: StateFlow<List<ValidationIssue>> = _basePrompt.map { basePrompt ->
        listOfNotNull(validateBasePrompt(basePrompt.ifBlank { null }))
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
            continuityLockLevel = continuityLockLevel
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
            existingAssetId: String? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (repository != null) LocationAssetFormViewModel(application, projectId, repository, existingAssetId = existingAssetId)
                        else LocationAssetFormViewModel(application, projectId, existingAssetId = existingAssetId)
                    ) as T
            }
    }
}
