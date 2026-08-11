package com.operaboys.cinemashotgenerator.ui.project

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel

// واحد ۱۶ فاز ۱ — صفحه‌ی Projects طبق docs/design/README.md بخش «۲. Projects»:
// Header با عنوان + زیرعنوان (تعداد پروژه‌ی محلی، پویا — نه Hardcode)، همان لیست
// کارت پروژه‌ی Home اما کامل و بدون Hero Image.
//
// MIGRATED (رفع یافته‌ی معماری «عدم قرینگی Export/Import»، G14/ADR-064/ADR-065):
// دکمه‌ی «وارد کردن پروژه» — تنها نقطه‌ی ورود File Picker واقعی برای Import،
// چون Import یک عملیات سطح-فهرست است (پروژه‌ی جدید می‌سازد)، نه یک عملیات
// per-card مثل Export.

/** testTag دکمه‌ی Import (نه onNodeWithText، هم‌الگو با CREATE_PROJECT_NAME_FIELD_TAG). */
const val IMPORT_PROJECT_BUTTON_TAG = "projects.importButton"

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

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { projectListViewModel.importProject(it.toString()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = uiString("projects.title", language), style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = uiTemplate("projects.subtitleTemplate", language, "count" to summaries.size.toString()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = CinemaTheme.extendedColors.fg2
                )
            }
            IconButton(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.testTag(IMPORT_PROJECT_BUTTON_TAG)
            ) {
                Icon(Icons.Filled.FileUpload, contentDescription = uiString("projects.importButton", language))
            }
        }

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
