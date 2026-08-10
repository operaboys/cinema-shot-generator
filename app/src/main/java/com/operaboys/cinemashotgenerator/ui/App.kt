package com.operaboys.cinemashotgenerator.ui

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.AudioContextRepository
import com.operaboys.cinemashotgenerator.data.repository.AutoSaveManager
import com.operaboys.cinemashotgenerator.data.repository.BackupFileStorage
import com.operaboys.cinemashotgenerator.data.repository.DeviceBackupFileStorage
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.PromptGenerationRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.SettingsResolutionRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel

// واحد ۱۶ — فاز ۰/۱: ریشه‌ی درخت Compose کل اپ — جایگزین MainScreen.kt موقت
// (Hello World) قدم‌های قبلی. مسئولیت‌های این فایل دقیقاً طبق الزامات سند طراحی:
// (۱) تم (Dark/Light) از WorkflowViewModel، نه isSystemInDarkTheme خام —
// docs/design/README.md: «Dark/Light theme toggle ... independent of language»؛
// (۲) الزام سخت‌گیرانه‌ی RTL — LocalLayoutDirection بر اساس زبان انتخابی کاربر
// Override می‌شود (نه Locale سیستم)، چون کاربر می‌تواند مستقل از Locale دستگاه،
// زبان اپ را از داخل خودِ اپ عوض کند؛ (۳) اتصال WorkflowViewModel/ProjectListViewModel
// (هر دو Application-scoped از طریق viewModel(factory=...)) به کل درخت.
//
// MIGRATED (فاز ۱، docs/adr/044-unit16-phase1-app-shell.md): ProjectListViewModel
// اینجا (نه داخل MainScaffold) ساخته و به MainScaffold تزریق می‌شود — هم‌الگو با
// WorkflowViewModel موجود؛ این یکدستی همان چیزی است که تست‌پذیری MainScaffold را
// (بدون نیاز به viewModel(factory=...) واقعی در تست) ممکن می‌کند.

@Composable
fun App() {
    val application = LocalContext.current.applicationContext as Application
    val workflowViewModel: WorkflowViewModel = viewModel(factory = WorkflowViewModel.factory(application))
    // واحد ۱۶ فاز ۲ — قدم ۱: هم‌الگو با workflowViewModel/projectListViewModel — یک
    // نمونه‌ی مشترک ساخته و به کل درخت تزریق می‌شود (نه هر Composable مصرف‌کننده
    // خودش از AppDatabase.getInstance بسازد)، دقیقاً برای همان دلیل تست‌پذیری —
    // جزئیات کامل در docs/adr/045-unit16-phase2-step1-story-tab.md.
    //
    // MIGRATED (رفع G15 ممیزی post-Unit16، docs/adr/063-...): قبل از
    // projectListViewModel ساخته می‌شود (نه بعدش) تا بتواند به factory آن تزریق
    // شود — ProjectListViewModel.exportProject دیگر خودش AppDatabase.getInstance
    // را مستقیماً صدا نمی‌زند.
    val database = remember { AppDatabase.getInstance(application) }
    val projectListViewModel: ProjectListViewModel = viewModel(factory = ProjectListViewModel.factory(application, database))
    val storyRepository = remember { StoryRepository(database.storyDao(), database.storyBreakdownSessionDao()) }
    // واحد ۱۶ فاز ۲ — قدم ۲: همان الگو، برای ذخیره‌ی واقعی خروجی AI Story Breakdown
    // (Character/Location/Object Asset + Scene + Shot) — جزئیات در
    // docs/adr/046-unit16-phase2-step2-ai-story-breakdown.md.
    val assetRepository = remember { AssetRepository(database.assetDao()) }
    val sceneRepository = remember { SceneRepository(database.sceneDao()) }
    val shotRepository = remember { ShotRepository(database.shotDao()) }
    // واحد ۱۶ فاز ۲ — قدم ۳ (آخرین قدم فاز ۲): همان الگو، برای Tab «DNA» — جزئیات در
    // docs/adr/047-unit16-phase2-step3-dna-tab.md.
    val projectDnaRepository = remember { ProjectDnaRepository(database.projectDnaDao()) }
    // واحد ۱۶ فاز ۵ — قدم ۳ (آخرین قدم فاز ۵): همان الگو، برای صفحه‌ی Output Delivery —
    // جزئیات در docs/adr/057-unit16-phase5-step3-output-delivery.md.
    val promptGenerationRepository = remember {
        PromptGenerationRepository(
            database.shotDao(),
            database.sceneDao(),
            projectDnaRepository,
            assetRepository,
            SettingsResolutionRepository(database.shotDao(), database.sceneDao()),
            AudioContextRepository(database.audioContextDao())
        )
    }
    // واحد ۱۶ فاز ۶ — قدم ۱: همان الگو، برای Timer دوره‌ای Auto-Save واقعی
    // (StudioShell) — جزئیات در docs/adr/058-unit16-phase6-step1-settings-autosave.md.
    val autoSaveManager = remember { AutoSaveManager(database.projectDao()) }
    // واحد ۱۶ فاز ۶ — قدم ۲ (آخرین قدم کل واحد ۱۶): همان الگو، برای Backup واقعی
    // (StudioShell + صفحه‌ی Backups) — BackupManager خودش per-project است (سازنده‌اش
    // projectId می‌خواهد)، پس فقط لایه‌ی I/O مشترک (BackupFileStorage) یک‌بار اینجا
    // ساخته می‌شود؛ خودِ BackupManager هر جا لازم است تازه ساخته می‌شود — جزئیات در
    // docs/adr/059-unit16-phase6-step2-backups-final-review.md.
    val backupFileStorage: BackupFileStorage = remember { DeviceBackupFileStorage(application) }

    val language by workflowViewModel.language.collectAsStateWithLifecycle()
    val theme by workflowViewModel.theme.collectAsStateWithLifecycle()

    val layoutDirection = if (language == Language.FA) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        CinemaShotGeneratorTheme(darkTheme = theme == AppTheme.DARK, language = language) {
            MainScaffold(
                workflowViewModel = workflowViewModel,
                projectListViewModel = projectListViewModel,
                storyRepository = storyRepository,
                assetRepository = assetRepository,
                sceneRepository = sceneRepository,
                shotRepository = shotRepository,
                projectDnaRepository = projectDnaRepository,
                promptGenerationRepository = promptGenerationRepository,
                autoSaveManager = autoSaveManager,
                backupFileStorage = backupFileStorage,
                database = database
            )
        }
    }
}
