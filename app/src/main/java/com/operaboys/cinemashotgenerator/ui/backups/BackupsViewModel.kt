package com.operaboys.cinemashotgenerator.ui.backups

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.BackupFileStorage
import com.operaboys.cinemashotgenerator.data.repository.BackupKind
import com.operaboys.cinemashotgenerator.data.repository.BackupManager
import com.operaboys.cinemashotgenerator.data.repository.BackupSummary
import com.operaboys.cinemashotgenerator.data.repository.DeviceBackupFileStorage
import com.operaboys.cinemashotgenerator.data.repository.DeviceExportFileWriter
import com.operaboys.cinemashotgenerator.data.repository.ExportFileWriter
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ExportFile
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// واحد ۱۶ فاز ۶ — قدم ۲ (آخرین قدم کل واحد ۱۶): ViewModel صفحه‌ی Backups.
// BackupManager per-project است (سازنده‌اش خودِ projectId می‌خواهد)، پس برخلاف
// بقیه‌ی Repository های این اپ، اینجا در سطح ViewModel ساخته می‌شود (نه یک نمونه‌ی
// مشترک App.kt) — تنها بخش تست‌پذیر تزریقی BackupFileStorage است (هم‌الگو دقیق با
// BackupManagerTest.kt موجود که از قبل FakeBackupFileStorage دارد).

class BackupsViewModel(
    application: Application,
    private val projectId: String,
    backupFileStorage: BackupFileStorage? = null,
    database: AppDatabase? = null,
    // رفع یافته‌ی #۱۲ appendix (ADR-089): هم‌الگو دقیق با
    // OutputDeliveryViewModel.exportFileWriter — تزریق‌پذیر برای تست، در اپ واقعی
    // همیشه پیش‌فرض DeviceExportFileWriter(application) است (بدون نیاز به سیم‌کشی
    // از BackupsScreen/AppNavHost، دقیقاً هم‌الگو با OutputDeliveryScreen).
    private val exportFileWriter: ExportFileWriter = DeviceExportFileWriter(application),
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    // یافته‌ی واقعی این قدم (تست دوباره‌اجراشده‌ی BackupsFlowTest): این کلاس قبلاً
    // همیشه AppDatabase.getInstance(application) را مستقیم صدا می‌زد — برخلاف
    // الگوی تزریق سراسری این پروژه (App.kt یک database واحد می‌سازد و پایین
    // تزریق می‌کند)، این یک singleton سراسری دیگر بود که با database تزریقی تست
    // (Room.inMemoryDatabaseBuilder جدا در BackupsFlowTest) هیچ ارتباطی نداشت —
    // پروژه‌ی واقعاً ساخته‌شده در دیتابیس تست هرگز برای BackupManager (که از
    // singleton سراسری DAO می‌گرفت) پیدا نمی‌شد، پس createBackup همیشه شکست
    // می‌خورد. رفع شد: database اکنون پارامتر تزریقی است (هم‌الگو دقیق با
    // autoSaveManager/backupFileStorage)، فقط وقتی null باشد (مسیر واقعی اپ) به
    // Singleton برمی‌گردد.
    private val backupManager: BackupManager = run {
        val resolvedDatabase = database ?: AppDatabase.getInstance(application)
        BackupManager(
            projectId = projectId,
            projectDao = resolvedDatabase.projectDao(),
            sceneDao = resolvedDatabase.sceneDao(),
            shotDao = resolvedDatabase.shotDao(),
            assetDao = resolvedDatabase.assetDao(),
            projectDnaDao = resolvedDatabase.projectDnaDao(),
            audioContextDao = resolvedDatabase.audioContextDao(),
            backupFileStorage = backupFileStorage ?: DeviceBackupFileStorage(application)
        )
    }

    private val _backups = MutableStateFlow<List<BackupSummary>>(emptyList())
    val backups: StateFlow<List<BackupSummary>> = _backups.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        ioScope.launch {
            _backups.value = backupManager.listBackups().getOrElse { emptyList() }
            _isLoaded.value = true
        }
    }

    fun createManualBackup(onDone: (Boolean) -> Unit = {}) {
        ioScope.launch {
            val result = backupManager.createBackup(BackupKind.MANUAL)
            refresh()
            onDone(result.isSuccess)
        }
    }

    fun restore(backupId: String, onDone: (Boolean) -> Unit) {
        ioScope.launch {
            val result = backupManager.restoreFromBackup(backupId)
            onDone(result.isSuccess)
        }
    }

    fun delete(backupId: String) {
        ioScope.launch {
            backupManager.deleteBackup(backupId)
            refresh()
        }
    }

    // رفع یافته‌ی #۱۲ appendix (ADR-089): هم‌الگو دقیق با
    // OutputDeliveryViewModel.exportedFiles/exportOutput/clearExportedFiles —
    // یک رویداد یک‌باره؛ UI این را Observe می‌کند تا Intent.ACTION_SEND[_MULTIPLE]
    // واقعی را بسازد/باز کند (فقط لایه‌ی UI با Context یک Activity می‌تواند این
    // کار را انجام دهد)، سپس clearExportedFiles را صدا می‌زند.
    private val _exportedFiles = MutableStateFlow<List<File>?>(null)
    val exportedFiles: StateFlow<List<File>?> = _exportedFiles.asStateFlow()

    fun clearExportedFiles() {
        _exportedFiles.value = null
    }

    /** تمام بکاپ‌های فعلی این پروژه را واقعاً روی دیسک می‌نویسد (برای Export All). */
    fun exportAll(): Job = ioScope.launch {
        val pairs = backupManager.readAllBackupsForExport().getOrElse { emptyList() }
        if (pairs.isEmpty()) return@launch
        val exportFiles = pairs.map { (fileName, content) -> ExportFile(fileName, content, "text/plain") }
        _exportedFiles.value = exportFileWriter.writeExportFiles(exportFiles)
    }

    fun importBackup(content: String, onDone: (Result<String>) -> Unit = {}) {
        ioScope.launch {
            val result = backupManager.importBackup(content)
            refresh()
            onDone(result)
        }
    }

    companion object {
        fun factory(
            application: Application,
            projectId: String,
            backupFileStorage: BackupFileStorage? = null,
            database: AppDatabase? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    BackupsViewModel(application, projectId, backupFileStorage, database) as T
            }
    }
}
