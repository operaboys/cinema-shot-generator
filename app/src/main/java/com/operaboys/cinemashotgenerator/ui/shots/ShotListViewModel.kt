package com.operaboys.cinemashotgenerator.ui.shots

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

// واحد ۱۶ فاز ۴ — قدم ۲ — بخش الف: ViewModel واقعی Tab «شات‌ها» صفحه‌ی Scene
// Detail — اولین اتصال UI به ShotRepository.loadAllShots (تازه در همین قدم اضافه
// شد). هم‌الگو با ScenesListViewModel/AssetLibraryViewModel (Flow.stateIn،
// ADR-048/ADR-050).
class ShotListViewModel(
    application: Application,
    private val sceneId: String,
    repository: ShotRepository = ShotRepository(AppDatabase.getInstance(application).shotDao())
) : AndroidViewModel(application) {

    val shots: StateFlow<List<Shot>> = repository.loadAllShots(sceneId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(application: Application, sceneId: String, repository: ShotRepository? = null): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (repository != null) ShotListViewModel(application, sceneId, repository)
                        else ShotListViewModel(application, sceneId)
                    ) as T
            }
    }
}
