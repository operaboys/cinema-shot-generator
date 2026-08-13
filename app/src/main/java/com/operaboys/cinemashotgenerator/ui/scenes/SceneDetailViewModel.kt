package com.operaboys.cinemashotgenerator.ui.scenes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.scene.lockScene as applyLockTransition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// واحد ۱۶ فاز ۴ — قدم ۱: ViewModel واقعی صفحه‌ی Scene Detail. هم‌الگو با
// DnaViewModel (بارگذاری Async یک Entity تکی با idProvider تزریق‌پذیر) —
// AssetLibraryViewModel اینجا کاربرد ندارد چون این صفحه یک Entity تکی را ویرایش
// می‌کند، نه یک فهرست زنده‌ی Flow-محور.
class SceneDetailViewModel(
    application: Application,
    private val projectId: String,
    private val sceneId: String,
    private val sceneRepository: SceneRepository = SceneRepository(AppDatabase.getInstance(application).sceneDao()),
    private val assetRepository: AssetRepository = AssetRepository(AppDatabase.getInstance(application).assetDao()),
    private val shotRepository: ShotRepository = ShotRepository(AppDatabase.getInstance(application).shotDao()),
    private val idProvider: () -> String = ::generateSceneId,
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    private val _scene = MutableStateFlow<Scene?>(null)
    val scene: StateFlow<Scene?> = _scene.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _lastActionMessage = MutableStateFlow<String?>(null)
    /** پیام نتیجه‌ی آخرین عملیات (Lock/Duplicate ناموفق) — طبق همان الگوی «فراخوان مسئول نمایش دلیل Blocking واقعی است» (ProjectLifecycle.kt). */
    val lastActionMessage: StateFlow<String?> = _lastActionMessage.asStateFlow()

    /** برای دکمه‌ی «اتصال به کتابخانه» — طبق فاز ۳ قدم ۱ (AssetRepository.loadAllLocationAssets). */
    val locationAssets: StateFlow<List<LocationAsset>> = assetRepository.loadAllLocationAssets(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * یافته‌ی ۱ appendix ADR-081 (ADR-083): Badge عددی Tab «شات‌ها» طبق mockup
     * (`sceneTabs`، `t.badge`). هم‌الگو دقیق با `locationAssets` بالا — Flow زنده،
     * نه یک بار خواندن.
     */
    val shotCount: StateFlow<Int> = shotRepository.loadAllShots(sceneId)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _deleteBlockedMessage = MutableStateFlow<String?>(null)
    val deleteBlockedMessage: StateFlow<String?> = _deleteBlockedMessage.asStateFlow()

    init {
        ioScope.launch {
            _scene.value = sceneRepository.loadScene(sceneId).getOrNull()
            _isLoaded.value = true
        }
    }

    fun connectLocationAsset(locationAssetId: String) = updateAndSave { it.copy(locationAssetId = locationAssetId) }

    /**
     * طبق «مشخصات دقیق فیلدهای تنظیمات Scene» بلوپرینت ۱۶ — این فرم عمداً یک دکمه‌ی
     * ذخیره‌ی صریح دارد (نه Auto-Save بی‌صدای Tab «DNA»)؛ همه‌ی فیلد‌ها یک‌جا با
     * فراخوانی این تابع ذخیره می‌شوند.
     */
    /**
     * رفع G8 ممیزی post-Unit16 (docs/adr/076-...): globalVisualStyleOverride
     * تازه اضافه شد — null یعنی «پیش‌فرض DNA پروژه» (source همیشه "project_dna"
     * می‌ماند، فقط override عوض می‌شود)، غیر-null یک نام VisualStyle واقعی است.
     */
    fun saveSceneSettings(
        sceneTitle: String?,
        narrativeRole: NarrativeRole,
        timeOfDay: TimeOfDay,
        atmospherePrimary: Atmosphere,
        atmosphereSecondary: Atmosphere?,
        globalVisualStyleOverride: String?
    ) = updateAndSave {
        it.copy(
            sceneTitle = sceneTitle,
            narrativeRole = narrativeRole,
            timeOfDay = timeOfDay,
            atmospherePrimary = atmospherePrimary,
            atmosphereSecondary = atmosphereSecondary,
            globalVisualStyle = it.globalVisualStyle.copy(override = globalVisualStyleOverride)
        )
    }

    /** Rule واقعی State Machine واحد ۱۲ (domain.scene.lockScene) — true فقط اگر انتقال واقعاً مجاز بود. */
    fun lockScene(): Boolean {
        val current = _scene.value ?: return false
        val result = applyLockTransition(current)
        val locked = result.getOrNull()
        if (locked == null) {
            _lastActionMessage.value = result.exceptionOrNull()?.message
            return false
        }
        _scene.value = locked
        ioScope.launch { sceneRepository.saveScene(projectId, locked) }
        return true
    }

    /**
     * sceneNumber جدید از شمار فعلی صحنه‌های پروژه محاسبه می‌شود — این ViewModel
     * (برخلاف ScenesListViewModel) فهرست کامل صحنه‌ها را از قبل بارگذاری‌شده ندارد،
     * پس یک‌بار با `.first()` خوانده می‌شود (نه Flow زنده‌ی دائمی، چون فقط برای همین
     * محاسبه‌ی یک‌باره لازم است).
     */
    fun duplicateScene(onDuplicated: (String) -> Unit) {
        val current = _scene.value ?: return
        ioScope.launch {
            val allScenes = sceneRepository.loadAllScenes(projectId).first()
            val duplicate = current.copy(
                sceneId = idProvider(),
                sceneNumber = allScenes.size + 1,
                sceneTitle = null,
                state = com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState.DRAFT
            )
            sceneRepository.saveScene(projectId, duplicate)
            onDuplicated(duplicate.sceneId)
        }
    }

    fun clearLastActionMessage() { _lastActionMessage.value = null }

    /**
     * رفع G22 ممیزی post-Unit16: تنها مسیر واقعی حذف Scene از UI. Rule واقعی
     * (`SceneRepository.deleteScene` → `domain.scene.deleteScene`) خودش تصمیم
     * می‌گیرد — اگر صحنه هنوز Shot دارد، Blocking واقعی برمی‌گرداند (نه فقط یک
     * تأیید قوی‌تر) و `onDeleted` هرگز صدا زده نمی‌شود.
     *
     * یافته‌ی دیباگ واقعی: شمار Shot ها عمداً از یک StateFlow جدا (WhileSubscribed)
     * خوانده نمی‌شود — چون هیچ UI ای هرگز آن را collect نمی‌کند (فقط برای همین
     * تابع لازم است، نه نمایش)، آن Flow هرگز واقعاً شروع به کار نمی‌کرد و مقدارش
     * همیشه ۰ (پیش‌فرض اولیه) می‌ماند — یعنی Rule هرگز واقعاً مسدود نمی‌کرد. رفع
     * شد با یک خواندن مستقیم و یک‌باره (`.first()`) در همین‌جا.
     */
    fun deleteScene(onDeleted: () -> Unit) {
        val current = _scene.value ?: return
        ioScope.launch {
            val shotCount = shotRepository.loadAllShots(sceneId).first().size
            val result = sceneRepository.deleteScene(current, shotCount)
            if (result.isSuccess) {
                onDeleted()
            } else {
                _deleteBlockedMessage.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun clearDeleteBlockedMessage() { _deleteBlockedMessage.value = null }

    private fun updateAndSave(transform: (Scene) -> Scene) {
        val current = _scene.value ?: return
        val updated = transform(current)
        _scene.value = updated
        ioScope.launch { sceneRepository.saveScene(projectId, updated) }
    }

    companion object {
        fun factory(
            application: Application,
            projectId: String,
            sceneId: String,
            sceneRepository: SceneRepository? = null,
            assetRepository: AssetRepository? = null,
            shotRepository: ShotRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        // واحد ۱۶ فاز ۴ — قدم ۲: کمبود واقعی کشف‌شده — این شرط قبلاً هر دو
                        // Repository را با «&&» به‌هم‌بسته بود؛ یعنی اگر فقط یکی از دو
                        // Repository تزریق می‌شد (مثلاً فقط sceneRepository، بدون
                        // assetRepository)، به‌طور بی‌صدا هر دو به دیتابیس Production
                        // (AppDatabase.getInstance) برمی‌گشتند — در تست، یعنی SceneDetailViewModel
                        // با یک دیتابیس کاملاً متفاوت و خالی کار می‌کرد، بدون هیچ خطای
                        // قابل‌مشاهده (loadScene همیشه null برمی‌گرداند). رفع شد: هرکدام
                        // مستقل بررسی می‌شود، هم‌راستا با پیش‌فرض‌های خودِ سازنده‌ی کلاس.
                        // shotRepository (رفع G22) هم‌الگو با همین قاعده اضافه شد.
                        if (sceneRepository != null || assetRepository != null || shotRepository != null) {
                            SceneDetailViewModel(
                                application = application,
                                projectId = projectId,
                                sceneId = sceneId,
                                sceneRepository = sceneRepository ?: SceneRepository(AppDatabase.getInstance(application).sceneDao()),
                                assetRepository = assetRepository ?: AssetRepository(AppDatabase.getInstance(application).assetDao()),
                                shotRepository = shotRepository ?: ShotRepository(AppDatabase.getInstance(application).shotDao())
                            )
                        } else {
                            SceneDetailViewModel(application, projectId, sceneId)
                        }
                    ) as T
            }
    }
}
