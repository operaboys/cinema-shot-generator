package com.operaboys.cinemashotgenerator.ui.project

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.DeviceBackupFileStorage
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.data.repository.exportProject as exportProjectFile
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.project.Project
import com.operaboys.cinemashotgenerator.domain.project.ProjectSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// واحد ۱۶ فاز ۱ — ViewModel مشترک صفحه‌های Home و Projects (هر دو دقیقاً همان فهرست
// کارت پروژه را نشان می‌دهند — طبق docs/design/README.md: «Same project card list as
// Home's Recent, full list»)؛ به همین دلیل یک ViewModel مشترک ساخته شد، نه دو
// ViewModel تقریباً یکسان. جزئیات کامل در docs/adr/044-unit16-phase1-app-shell.md.
class ProjectListViewModel(
    application: Application,
    private val repository: ProjectRepository = ProjectRepository(AppDatabase.getInstance(application).projectDao())
) : AndroidViewModel(application) {

    val projectSummaries: StateFlow<List<ProjectSummary>> = repository.observeProjectSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _lastActionMessage = MutableStateFlow<String?>(null)
    val lastActionMessage: StateFlow<String?> = _lastActionMessage.asStateFlow()

    fun createProject(name: String, uiLanguage: Language = Language.FA, onCreated: (Project) -> Unit = {}) {
        viewModelScope.launch {
            repository.createProject(name, uiLanguage).fold(
                onSuccess = { onCreated(it) },
                onFailure = { _lastActionMessage.value = it.message }
            )
        }
    }

    fun renameProject(projectId: String, newName: String) {
        viewModelScope.launch {
            repository.renameProject(projectId, newName).onFailure { _lastActionMessage.value = it.message }
        }
    }

    /** ممکن است شکست بخورد (پروژه FINAL نیست) — پیام واقعی خطا در lastActionMessage نمایش داده می‌شود. */
    fun archiveProject(projectId: String) {
        viewModelScope.launch {
            repository.archiveProject(projectId).onFailure { _lastActionMessage.value = it.message }
        }
    }

    fun duplicateProject(projectId: String) {
        viewModelScope.launch {
            repository.duplicateProject(projectId).onFailure { _lastActionMessage.value = it.message }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId).onFailure { _lastActionMessage.value = it.message }
        }
    }

    /**
     * exportProject (واحد ۱۵/ADR-024) مستقیماً بازاستفاده شد — این تابع دوباره
     * منطق Export نمی‌سازد، فقط آن را به این ViewModel وصل می‌کند (هم‌الگو با ADR-024:
     * «این قدم عمدتاً بازترکیب زیرساخت موجود است»).
     */
    fun exportProject(projectId: String) {
        val database = AppDatabase.getInstance(getApplication())
        viewModelScope.launch {
            exportProjectFile(
                projectId = projectId,
                projectDao = database.projectDao(),
                sceneDao = database.sceneDao(),
                shotDao = database.shotDao(),
                assetDao = database.assetDao(),
                projectDnaDao = database.projectDnaDao(),
                audioContextDao = database.audioContextDao(),
                backupFileStorage = DeviceBackupFileStorage(getApplication())
            ).onFailure { _lastActionMessage.value = it.message }
        }
    }

    fun clearLastActionMessage() {
        _lastActionMessage.value = null
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProjectListViewModel(application) as T
            }
    }
}
