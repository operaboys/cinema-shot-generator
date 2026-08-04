package com.operaboys.cinemashotgenerator.ui.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel

// واحد ۱۶ فاز ۱ — صفحه‌ی Projects طبق docs/design/README.md بخش «۲. Projects»:
// Header با عنوان + زیرعنوان (تعداد پروژه‌ی محلی، پویا — نه Hardcode)، همان لیست
// کارت پروژه‌ی Home اما کامل و بدون Hero Image.

@Composable
fun ProjectsScreen(
    workflowViewModel: WorkflowViewModel,
    projectListViewModel: ProjectListViewModel,
    onOpenProject: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val language by workflowViewModel.language.collectAsStateWithLifecycle()
    val summaries by projectListViewModel.projectSummaries.collectAsStateWithLifecycle()
    val lastActionMessage by projectListViewModel.lastActionMessage.collectAsStateWithLifecycle()

    LaunchedEffect(lastActionMessage) {
        lastActionMessage?.let { message ->
            onShowMessage(message)
            projectListViewModel.clearLastActionMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(text = uiString("projects.title", language), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = uiTemplate("projects.subtitleTemplate", language, "count" to summaries.size.toString()),
            style = MaterialTheme.typography.bodyLarge,
            color = CinemaTheme.extendedColors.fg2
        )

        if (summaries.isEmpty()) {
            Text(
                text = uiString("home.emptyState", language),
                style = MaterialTheme.typography.bodyLarge,
                color = CinemaTheme.extendedColors.fg3,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            ProjectListSection(
                summaries = summaries,
                language = language,
                viewModel = projectListViewModel,
                onOpenProject = onOpenProject,
                showThumbnails = false,
                modifier = Modifier.weight(1f).padding(top = 16.dp)
            )
        }
    }
}
