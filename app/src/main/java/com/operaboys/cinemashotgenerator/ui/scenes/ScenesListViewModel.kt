package com.operaboys.cinemashotgenerator.ui.scenes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.SceneLocation
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

// واحد ۱۶ فاز ۴ — قدم ۱: ViewModel واقعی Tab «صحنه‌ها» — اولین اتصال UI به
// SceneRepository.loadAllScenes (تازه در همین قدم اضافه شد). هم‌الگو با
// AssetLibraryViewModel (Flow.stateIn، ADR-048).

internal fun generateSceneId(): String = "scene_" + UUID.randomUUID().toString().replace("-", "").take(12)

class ScenesListViewModel(
    application: Application,
    private val projectId: String,
    private val repository: SceneRepository = SceneRepository(AppDatabase.getInstance(application).sceneDao()),
    private val idProvider: () -> String = ::generateSceneId,
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    val scenes: StateFlow<List<Scene>> = repository.loadAllScenes(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * طبق 🆕v4 بلوپرینت ۱۶ («یک Scene خالی با نام پیش‌فرض «Scene N» بلافاصله ساخته
     * می‌شود، بدون Modal») — sceneTitle عمداً null می‌ماند (نمایش fallback با
     * sceneDisplayTitle در UI)، sceneNumber از شمار فعلی صحنه‌های همین پروژه
     * محاسبه می‌شود. فیلدهای دیگر مقدار خنثی/میانه می‌گیرند (کاربر بعداً از طریق
     * «تنظیمات Scene» دقیقشان می‌کند): NarrativeRole.DEVELOPMENT (نه INTRODUCTION —
     * فرض غلط «همیشه صحنه‌ی اول» نمی‌کند)، TimeOfDay.NOON، Atmosphere.CALM (هم‌راستا
     * با پیش‌فرض خنثی مشابه Mood.CALM در DnaViewModel).
     */
    fun createNewScene(onCreated: (String) -> Unit) {
        ioScope.launch {
            val nextNumber = scenes.value.size + 1
            val newScene = Scene(
                sceneId = idProvider(),
                sceneNumber = nextNumber,
                narrativeRole = NarrativeRole.DEVELOPMENT,
                location = SceneLocation(type = LocationType.CUSTOM, description = ""),
                timeOfDay = TimeOfDay.NOON,
                atmospherePrimary = Atmosphere.CALM
            )
            repository.saveScene(projectId, newScene)
            onCreated(newScene.sceneId)
        }
    }

    companion object {
        fun factory(application: Application, projectId: String, repository: SceneRepository? = null): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (repository != null) ScenesListViewModel(application, projectId, repository)
                        else ScenesListViewModel(application, projectId)
                    ) as T
            }
    }
}
