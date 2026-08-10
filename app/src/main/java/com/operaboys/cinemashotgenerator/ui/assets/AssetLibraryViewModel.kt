package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.LocationType
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.asset.validateAssetDeletion
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// واحد ۱۶ فاز ۳ — قدم ۱: ViewModel واقعی صفحه‌ی Asset Library — اولین اتصال UI به
// AssetRepository.loadAllCharacterAssets/loadAllLocationAssets/loadAllObjectAssets
// (تازه در همین قدم اضافه شدند). هم‌الگو با ProjectListViewModel (تنها ViewModel
// موجود پروژه که از قبل الگوی `Flow.stateIn(...)` را به‌جای MutableStateFlow دستی
// به‌کار می‌برد — چون منبع این‌جا هم یک Flow واقعی Room است، نه یک بار بارگذاری‌شونده).
// جزئیات کامل تصمیمات در docs/adr/048-unit16-phase3-step1-asset-library.md.

// واحد ۱۶ فاز ۳ — قدم ۲: @Serializable اضافه شد تا AssetKind مستقیماً به‌عنوان
// آرگومان مسیر Navigation نوع‌ایمن AssetForm (ui/navigation/AppDestinations.kt)
// قابل‌استفاده باشد — بدون نیاز به تبدیل رفت‌وبرگشتی به String.
@Serializable
enum class AssetKind { CHARACTER, LOCATION, OBJECT }

class AssetLibraryViewModel(
    application: Application,
    private val projectId: String,
    private val repository: AssetRepository = AssetRepository(AppDatabase.getInstance(application).assetDao()),
    // رفع G22 ممیزی post-Unit16: برای Rule واقعی «Asset در حال استفاده قابل حذف
    // نیست» (validateAssetDeletion، AssetValidation.kt) — هم‌الگو با DnaViewModel که
    // برای dependentShotsCount به یک ShotRepository جدا نیاز داشت (ADR-054).
    private val shotRepository: ShotRepository = ShotRepository(AppDatabase.getInstance(application).shotDao())
) : AndroidViewModel(application) {

    private val _selectedKind = MutableStateFlow(AssetKind.CHARACTER)
    val selectedKind: StateFlow<AssetKind> = _selectedKind.asStateFlow()

    /** `null` یعنی «همه» — بدون فیلتر زیرگروه. */
    private val _selectedCharacterTier = MutableStateFlow<CharacterTier?>(null)
    val selectedCharacterTier: StateFlow<CharacterTier?> = _selectedCharacterTier.asStateFlow()

    private val _selectedLocationType = MutableStateFlow<LocationType?>(null)
    val selectedLocationType: StateFlow<LocationType?> = _selectedLocationType.asStateFlow()

    private val _selectedObjectSubtype = MutableStateFlow<ObjectSubtype?>(null)
    val selectedObjectSubtype: StateFlow<ObjectSubtype?> = _selectedObjectSubtype.asStateFlow()

    val characters: StateFlow<List<CharacterAsset>> = repository.loadAllCharacterAssets(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val locations: StateFlow<List<LocationAsset>> = repository.loadAllLocationAssets(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val objects: StateFlow<List<ObjectAsset>> = repository.loadAllObjectAssets(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSelectedKind(kind: AssetKind) {
        _selectedKind.value = kind
    }

    fun setSelectedCharacterTier(tier: CharacterTier?) {
        _selectedCharacterTier.value = tier
    }

    fun setSelectedLocationType(type: LocationType?) {
        _selectedLocationType.value = type
    }

    fun setSelectedObjectSubtype(subtype: ObjectSubtype?) {
        _selectedObjectSubtype.value = subtype
    }

    private val _deleteBlockedMessage = MutableStateFlow<String?>(null)
    val deleteBlockedMessage: StateFlow<String?> = _deleteBlockedMessage.asStateFlow()

    /**
     * رفع G22 ممیزی post-Unit16: تنها مسیر واقعی حذف Asset از UI. Rule واقعی
     * (validateAssetDeletion) اینجا اجرا می‌شود — اگر شات‌هایی این Asset را
     * استفاده می‌کنند، حذف Blocking واقعی است (نه فقط یک تأیید قوی‌تر).
     */
    fun deleteAsset(assetId: String) {
        viewModelScope.launch {
            val shotsUsingAsset = shotRepository.findShotIdsUsingAsset(projectId, assetId)
            val blocking = validateAssetDeletion(assetId, shotsUsingAsset)
            if (blocking != null) {
                _deleteBlockedMessage.value = blocking.message
            } else {
                repository.deleteAsset(assetId)
            }
        }
    }

    fun clearDeleteBlockedMessage() { _deleteBlockedMessage.value = null }

    companion object {
        fun factory(
            application: Application,
            projectId: String,
            repository: AssetRepository? = null,
            shotRepository: ShotRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (repository != null || shotRepository != null) {
                            AssetLibraryViewModel(
                                application = application,
                                projectId = projectId,
                                repository = repository ?: AssetRepository(AppDatabase.getInstance(application).assetDao()),
                                shotRepository = shotRepository ?: ShotRepository(AppDatabase.getInstance(application).shotDao())
                            )
                        } else {
                            AssetLibraryViewModel(application, projectId)
                        }
                    ) as T
            }
    }
}
