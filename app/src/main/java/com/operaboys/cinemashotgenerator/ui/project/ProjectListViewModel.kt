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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    private val repository: ProjectRepository = ProjectRepository(AppDatabase.getInstance(application).projectDao()),
    // رفع G15 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، docs/adr/063-...):
    // exportProject قبلاً مستقیماً AppDatabase.getInstance(getApplication()) را
    // صدا می‌زد، برخلاف الگوی تثبیت‌شده‌ی بقیه‌ی این کلاس (repository تزریقی) و
    // بقیه‌ی ViewModel های این واحد (BackupsViewModel/SceneDetailViewModel،
    // ADR-059). هم‌الگو دقیق: database تزریق‌پذیر، پیش‌فرض Singleton سراسری.
    database: AppDatabase? = null,
    // exportProject از Room به DAO های Suspend می‌رسد که به Executor داخلی خودشان
    // hop می‌کنند — طبق یافته‌ی مستند OutputDeliveryViewModelTest، Unconfined
    // به‌تنهایی برای همگام‌سازی تست کافی نیست؛ همان الگوی ioScopeOverride/Job
    // (regenerate، ADR-057) اینجا هم لازم است.
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val db: AppDatabase = database ?: AppDatabase.getInstance(application)
    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    val projectSummaries: StateFlow<List<ProjectSummary>> = repository.observeProjectSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _lastActionMessage = MutableStateFlow<String?>(null)
    val lastActionMessage: StateFlow<String?> = _lastActionMessage.asStateFlow()

    fun createProject(name: String, uiLanguage: Language = Language.FA, onCreated: (Project) -> Unit = {}) {
        ioScope.launch {
            repository.createProject(name, uiLanguage).fold(
                onSuccess = { onCreated(it) },
                onFailure = { _lastActionMessage.value = it.message }
            )
        }
    }

    fun renameProject(projectId: String, newName: String) {
        ioScope.launch {
            repository.renameProject(projectId, newName).onFailure { _lastActionMessage.value = it.message }
        }
    }

    /** ممکن است شکست بخورد (پروژه FINAL نیست) — پیام واقعی خطا در lastActionMessage نمایش داده می‌شود. */
    fun archiveProject(projectId: String) {
        ioScope.launch {
            repository.archiveProject(projectId).onFailure { _lastActionMessage.value = it.message }
        }
    }

    fun duplicateProject(projectId: String) {
        ioScope.launch {
            repository.duplicateProject(projectId).onFailure { _lastActionMessage.value = it.message }
        }
    }

    fun deleteProject(projectId: String) {
        ioScope.launch {
            repository.deleteProject(projectId).onFailure { _lastActionMessage.value = it.message }
        }
    }

    /**
     * exportProject (واحد ۱۵/ADR-024) مستقیماً بازاستفاده شد — این تابع دوباره
     * منطق Export نمی‌سازد، فقط آن را به این ViewModel وصل می‌کند (هم‌الگو با ADR-024:
     * «این قدم عمدتاً بازترکیب زیرساخت موجود است»).
     *
     * Job برگردانده می‌شود (نه Unit) — هم‌الگو با OutputDeliveryViewModel.regenerate
     * (ADR-057) — فقط تا تست مستقیم این ViewModel (ProjectListViewModelTest.kt)
     * بتواند .join() کند و مطمئن شود کار Export واقعاً قبل از خواندن
     * lastActionMessage.value کامل شده. تنها فراخوان تولیدی (ProjectListSection.kt)
     * مقدار برگشتی را نادیده می‌گیرد — بدون تغییر رفتار.
     */
    fun exportProject(projectId: String): Job {
        return ioScope.launch {
            exportProjectFile(
                projectId = projectId,
                projectDao = db.projectDao(),
                sceneDao = db.sceneDao(),
                shotDao = db.shotDao(),
                assetDao = db.assetDao(),
                projectDnaDao = db.projectDnaDao(),
                audioContextDao = db.audioContextDao(),
                backupFileStorage = DeviceBackupFileStorage(getApplication())
            ).onFailure { _lastActionMessage.value = it.message }
        }
    }

    fun clearLastActionMessage() {
        _lastActionMessage.value = null
    }

    companion object {
        fun factory(application: Application, database: AppDatabase? = null): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProjectListViewModel(application, database = database) as T
            }
    }
}
