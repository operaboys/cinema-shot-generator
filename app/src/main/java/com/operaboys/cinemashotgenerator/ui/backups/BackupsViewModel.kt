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
import kotlinx.coroutines.CoroutineScope
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
