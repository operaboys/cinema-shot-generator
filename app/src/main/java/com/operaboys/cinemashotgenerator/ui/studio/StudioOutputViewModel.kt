package com.operaboys.cinemashotgenerator.ui.studio

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
import com.operaboys.cinemashotgenerator.domain.validation.aggregateShotValidation
import com.operaboys.cinemashotgenerator.ui.dna.defaultProjectDna
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// یافته‌ی #۱۴ appendix ADR-081 (بزرگ‌ترین یافته‌ی باز) — ADR-084: Tab «خروجی»
// Studio. طبق mockup (docs/design/Cinema Studio.html، `st.output`، آفست
// ~۱۹۸۸۲۸۶) این Tab یک Dashboard سطح‌پروژه است، نه خودِ صفحه‌ی مستقل
// OutputDeliveryScreen (که نیازمند shotId مشخص است). این ViewModel معادل
// دامنه‌ای «تجمیع اعتبارسنجی همه‌ی شات‌های پروژه» را می‌سازد — با بازاستفاده‌ی
// کامل از `aggregateShotValidation` موجود (واحد ۰۷/ADR-055)، فقط در یک حلقه
// روی همه‌ی Shot های پروژه، دقیقاً هم‌الگو با ValidationViewModel برای یک
// Shot تکی. هیچ Rule دامنه‌ی تازه‌ای نوشته نشد.

/** خلاصه‌ی یک Shot برای این Tab — فقط فیلدهای لازم برای شمارش/Navigate، نه کل Shot. */
data class StudioOutputShotSummary(
    val shotId: String,
    val sceneId: String,
    val sceneNumber: Int,
    val sceneTitle: String?,
    val shotNumber: Int,
    val blockingCount: Int,
    val warningCount: Int
)

data class ProjectOutputSummary(val shots: List<StudioOutputShotSummary>) {
    val totalShots: Int get() = shots.size
    val totalBlockingCount: Int get() = shots.sumOf { it.blockingCount }
    val totalWarningCount: Int get() = shots.sumOf { it.warningCount }
    /** «آماده» یعنی بدون هیچ خطای Blocking — دقیقاً همان تعریف خودِ mockup («تا رفع Blocking، Prompt Generation غیرفعال است»). */
    val readyShotCount: Int get() = shots.count { it.blockingCount == 0 }
}

class StudioOutputViewModel(
    application: Application,
    private val projectId: String,
    private val shotRepository: ShotRepository = ShotRepository(AppDatabase.getInstance(application).shotDao()),
    private val sceneRepository: SceneRepository = SceneRepository(AppDatabase.getInstance(application).sceneDao()),
    private val projectDnaRepository: ProjectDnaRepository = ProjectDnaRepository(AppDatabase.getInstance(application).projectDnaDao()),
    private val assetRepository: AssetRepository = AssetRepository(AppDatabase.getInstance(application).assetDao()),
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    private val _summary = MutableStateFlow(ProjectOutputSummary(emptyList()))
    val summary: StateFlow<ProjectOutputSummary> = _summary.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        ioScope.launch {
            val shots = shotRepository.loadAllShotsForProject(projectId)
            // یافته‌ی واقعی دیباگ این قدم: `loadAllScenes` یک Flow است (برای UI
            // زنده‌ی ScenesListScreen طراحی شده)؛ `.first()` روی آن اینجا (داخل
            // `ioScope.launch`، پیش‌فرض `viewModelScope` = Main.immediate زیر
            // Robolectric) هرگز Resolve نمی‌شد — تست End-to-End واقعی این Tab را
            // پیش از هر Commit با یک Timeout واقعی رد کرد. رفع با همان الگوی
            // اثبات‌شده‌ی ValidationViewModel: `sceneRepository.loadScene(sceneId)`
            // مستقیم (suspend یک‌باره، نه Flow) برای هر sceneId متمایز.
            val scenesById = shots.map { it.sceneId }.distinct()
                .mapNotNull { sceneId -> sceneRepository.loadScene(sceneId).getOrNull()?.let { sceneId to it } }
                .toMap()
            val dna = projectDnaRepository.loadProjectDna(projectId).getOrNull()
                ?: defaultProjectDna(projectId) { "dna_placeholder" }

            val shotSummaries = shots.mapNotNull { shot ->
                val scene = scenesById[shot.sceneId] ?: return@mapNotNull null
                val characterAssets = assetRepository.loadCharacterAssets(shot.characterIds).getOrNull() ?: emptyList()
                val objectAssets = assetRepository.loadObjectAssets(shot.objectIds).getOrNull() ?: emptyList()
                val locationAssets = assetRepository.loadLocationAssets(shot.locationIds).getOrNull() ?: emptyList()
                val report = aggregateShotValidation(shot, scene, dna, characterAssets, objectAssets, locationAssets)
                StudioOutputShotSummary(
                    shotId = shot.shotId,
                    sceneId = scene.sceneId,
                    sceneNumber = scene.sceneNumber,
                    sceneTitle = scene.sceneTitle,
                    shotNumber = shot.shotNumber,
                    blockingCount = report.blockingCount,
                    warningCount = report.warningCount
                )
            }
            _summary.value = ProjectOutputSummary(shotSummaries)
            _isLoaded.value = true
        }
    }

    companion object {
        /** هم‌الگو دقیق با ValidationViewModel.factory — چهار Repository مستقل بررسی می‌شوند، نه با «&&». */
        fun factory(
            application: Application,
            projectId: String,
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
                            StudioOutputViewModel(
                                application = application,
                                projectId = projectId,
                                shotRepository = shotRepository ?: ShotRepository(AppDatabase.getInstance(application).shotDao()),
                                sceneRepository = sceneRepository ?: SceneRepository(AppDatabase.getInstance(application).sceneDao()),
                                projectDnaRepository = projectDnaRepository ?: ProjectDnaRepository(AppDatabase.getInstance(application).projectDnaDao()),
                                assetRepository = assetRepository ?: AssetRepository(AppDatabase.getInstance(application).assetDao())
                            )
                        } else {
                            StudioOutputViewModel(application, projectId)
                        }
                    ) as T
            }
    }
}
