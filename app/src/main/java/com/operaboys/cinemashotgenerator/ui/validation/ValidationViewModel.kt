package com.operaboys.cinemashotgenerator.ui.validation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.validation.AggregatedValidationReport
import com.operaboys.cinemashotgenerator.domain.validation.aggregateShotValidation
import com.operaboys.cinemashotgenerator.ui.dna.defaultProjectDna
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// واحد ۱۶ فاز ۵ — قدم ۱: ViewModel صفحه‌ی Validation — هم‌الگو با
// SceneDetailViewModel (بارگذاری Async چند Repository برای یک Entity تکی).
// چهار Repository موجود (Shot/Scene/ProjectDna/Asset) بدون هیچ متد تازه‌ای کافی
// بودند — جزئیات کامل در docs/adr/055-unit16-phase5-step1-validation-screen.md.
//
// اگر پروژه هنوز هیچ ProjectDna ذخیره‌شده‌ای نداشته باشد (کاربر هرگز Tab «DNA» را
// باز نکرده — ADR-047: DnaViewModel فقط با اولین ویرایش ذخیره می‌کند)، از همان
// `defaultProjectDna` خنثی که خودِ DnaViewModel برای این حالت استفاده می‌کند
// بازاستفاده شد (به‌جای تکرار مقادیر پیش‌فرض) — این تابع `internal` است، نه
// `private`، دقیقاً برای همین بازاستفاده‌ی درون-ماژولی.
class ValidationViewModel(
    application: Application,
    private val projectId: String,
    private val sceneId: String,
    private val shotId: String,
    private val shotRepository: ShotRepository = ShotRepository(AppDatabase.getInstance(application).shotDao()),
    private val sceneRepository: SceneRepository = SceneRepository(AppDatabase.getInstance(application).sceneDao()),
    private val projectDnaRepository: ProjectDnaRepository = ProjectDnaRepository(AppDatabase.getInstance(application).projectDnaDao()),
    private val assetRepository: AssetRepository = AssetRepository(AppDatabase.getInstance(application).assetDao()),
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    private val _report = MutableStateFlow<AggregatedValidationReport?>(null)
    val report: StateFlow<AggregatedValidationReport?> = _report.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        ioScope.launch {
            val shot = shotRepository.loadShot(shotId).getOrNull()
            val scene = sceneRepository.loadScene(sceneId).getOrNull()
            if (shot != null && scene != null) {
                val dna = projectDnaRepository.loadProjectDna(projectId).getOrNull()
                    ?: defaultProjectDna(projectId) { "dna_placeholder" }
                val characterAssets = assetRepository.loadCharacterAssets(shot.characterIds).getOrNull() ?: emptyList()
                val objectAssets = assetRepository.loadObjectAssets(shot.objectIds).getOrNull() ?: emptyList()
                val locationAssets = assetRepository.loadLocationAssets(shot.locationIds).getOrNull() ?: emptyList()
                _report.value = aggregateShotValidation(shot, scene, dna, characterAssets, objectAssets, locationAssets)
            }
            _isLoaded.value = true
        }
    }

    companion object {
        /**
         * چهار Repository جداگانه به‌جای هم بررسی می‌شوند (نه با «&&» به‌هم‌بسته) —
         * دقیقاً همان یافته‌ی مستندشده در SceneDetailViewModel.factory (ADR فاز ۴ قدم
         * ۱): اگر فقط یکی تزریق شود، بقیه نباید بی‌صدا به AppDatabase Production
         * برگردند.
         */
        fun factory(
            application: Application,
            projectId: String,
            sceneId: String,
            shotId: String,
            shotRepository: ShotRepository? = null,
            sceneRepository: SceneRepository? = null,
            projectDnaRepository: ProjectDnaRepository? = null,
            assetRepository: AssetRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (shotRepository != null || sceneRepository != null || projectDnaRepository != null || assetRepository != null) {
                            ValidationViewModel(
                                application = application,
                                projectId = projectId,
                                sceneId = sceneId,
                                shotId = shotId,
                                shotRepository = shotRepository ?: ShotRepository(AppDatabase.getInstance(application).shotDao()),
                                sceneRepository = sceneRepository ?: SceneRepository(AppDatabase.getInstance(application).sceneDao()),
                                projectDnaRepository = projectDnaRepository ?: ProjectDnaRepository(AppDatabase.getInstance(application).projectDnaDao()),
                                assetRepository = assetRepository ?: AssetRepository(AppDatabase.getInstance(application).assetDao())
                            )
                        } else {
                            ValidationViewModel(application, projectId, sceneId, shotId)
                        }
                    ) as T
            }
    }
}
