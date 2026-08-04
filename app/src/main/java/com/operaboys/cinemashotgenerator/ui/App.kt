package com.operaboys.cinemashotgenerator.ui

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val projectListViewModel: ProjectListViewModel = viewModel(factory = ProjectListViewModel.factory(application))

    val language by workflowViewModel.language.collectAsStateWithLifecycle()
    val theme by workflowViewModel.theme.collectAsStateWithLifecycle()

    val layoutDirection = if (language == Language.FA) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        CinemaShotGeneratorTheme(darkTheme = theme == AppTheme.DARK, language = language) {
            MainScaffold(workflowViewModel = workflowViewModel, projectListViewModel = projectListViewModel)
        }
    }
}
