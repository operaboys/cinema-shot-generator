package com.operaboys.cinemashotgenerator.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.project.ProjectSummary
import com.operaboys.cinemashotgenerator.ui.i18n.uiString

// واحد ۱۶ فاز ۱ — بخش مشترک فهرست کارت پروژه، هم توسط Home (بخش «پروژه‌های اخیر») و
// هم Projects (فهرست کامل) استفاده می‌شود — طبق docs/design/README.md: «Same project
// card list as Home's Recent, full list, no hero image». دیالوگ‌های تغییرنام/تأیید
// حذف هم اینجا مشترک‌اند تا در دو صفحه دوباره نوشته نشوند.

// testTag برای فیلد نام (همان دلیل CREATE_PROJECT_NAME_FIELD_TAG در HomeScreen.kt) —
// عنوان AlertDialog می‌تواند با متن دیگری در همان درخت هم‌نام باشد.
const val RENAME_PROJECT_NAME_FIELD_TAG = "renameProject.nameField"

@Composable
fun ProjectListSection(
    summaries: List<ProjectSummary>,
    language: Language,
    viewModel: ProjectListViewModel,
    onOpenProject: (String) -> Unit,
    showThumbnails: Boolean,
    modifier: Modifier = Modifier
) {
    var renameTarget by remember { mutableStateOf<ProjectSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<ProjectSummary?>(null) }
    // رفع یافته‌ی 🔴 G21 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md):
    // آرشیو قبلاً مستقیماً از onArchive صدا زده می‌شد، بدون هیچ دیالوگ تأیید —
    // برخلاف Delete در همان منو که از قبل یک AlertDialog تأیید داشت. هم‌الگو دقیق
    // با renameTarget/deleteTarget بالا.
    var archiveTarget by remember { mutableStateOf<ProjectSummary?>(null) }

    val actions = remember(viewModel) {
        ProjectCardActions(
            onOpen = onOpenProject,
            onRename = { projectId -> renameTarget = summaries.find { it.project.projectId == projectId } },
            onDuplicate = { projectId -> viewModel.duplicateProject(projectId) },
            onArchive = { projectId -> archiveTarget = summaries.find { it.project.projectId == projectId } },
            onExport = { projectId -> viewModel.exportProject(projectId) },
            onDeleteRequested = { projectId -> deleteTarget = summaries.find { it.project.projectId == projectId } }
        )
    }

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(summaries, key = { it.project.projectId }) { summary ->
            ProjectCard(summary = summary, language = language, actions = actions, showThumbnail = showThumbnails)
        }
    }

    renameTarget?.let { target ->
        RenameProjectDialog(
            currentName = target.project.projectName,
            language = language,
            onConfirm = { newName ->
                viewModel.renameProject(target.project.projectId, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }

    deleteTarget?.let { target ->
        DeleteProjectDialog(
            language = language,
            onConfirm = {
                viewModel.deleteProject(target.project.projectId)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }

    archiveTarget?.let { target ->
        ArchiveProjectDialog(
            language = language,
            onConfirm = {
                viewModel.archiveProject(target.project.projectId)
                archiveTarget = null
            },
            onDismiss = { archiveTarget = null }
        )
    }
}

@Composable
private fun RenameProjectDialog(currentName: String, language: Language, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiString("project.rename.title", language)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier.testTag(RENAME_PROJECT_NAME_FIELD_TAG)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(uiString("project.rename.confirm", language))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiString("project.rename.cancel", language)) } }
    )
}

@Composable
private fun DeleteProjectDialog(language: Language, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiString("project.delete.title", language)) },
        text = { Text(uiString("project.delete.message", language)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(uiString("project.delete.confirm", language)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiString("project.delete.cancel", language)) } }
    )
}

/**
 * رفع یافته‌ی 🔴 G21 ممیزی post-Unit16: طبق ALLOWED_TRANSITIONS[ARCHIVED]=emptyList()
 * در domain/stateversioning/StateMachine.kt، آرشیو یک انتقال کاملاً پایانی است — هیچ
 * Unarchive ای در کل اپ وجود ندارد. متن دیالوگ صریحاً همین را بیان می‌کند.
 */
@Composable
private fun ArchiveProjectDialog(language: Language, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiString("project.archive.title", language)) },
        text = { Text(uiString("project.archive.message", language)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(uiString("project.archive.confirm", language)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiString("project.archive.cancel", language)) } }
    )
}
