package com.operaboys.cinemashotgenerator.ui.project

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState
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

/**
 * یافته‌ی ۳ appendix ADR-081 (ADR-082): testTag ردیف چیپ فیلتر وضعیت mockup
 * (`stateFilters`، docs/design/Cinema Studio.html). `null` یعنی چیپ «همه».
 */
fun projectsStateFilterTag(state: EntityState?): String = "projects.stateFilter.${state?.name ?: "ALL"}"

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

    // یافته‌ی ۳ appendix ADR-081 (ADR-082): ردیف چیپ فیلتر وضعیت mockup
    // (`stateFilters`)، پیش‌فرض «همه» (null). چون هیچ منطق فعلی/mockup صریحی
    // برای مخفی‌کردن پیش‌فرض ARCHIVED وجود ندارد (این صفحه از قبل بدون هیچ
    // فیلتری همه‌ی پروژه‌ها را نشان می‌داد)، «همه» یعنی واقعاً همه — بدون حدس
    // زدن یک استثنای مخفی.
    var selectedStateFilter by remember { mutableStateOf<EntityState?>(null) }
    val filteredSummaries = if (selectedStateFilter == null) {
        summaries
    } else {
        summaries.filter { it.project.state == selectedStateFilter }
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

        if (summaries.isNotEmpty()) {
            ProjectStateFilterRow(
                language = language,
                selected = selectedStateFilter,
                onSelect = { selectedStateFilter = it },
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        if (filteredSummaries.isEmpty()) {
            Text(
                text = uiString("home.emptyState", language),
                style = MaterialTheme.typography.bodyLarge,
                color = CinemaTheme.extendedColors.fg3,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            ProjectListSection(
                summaries = filteredSummaries,
                language = language,
                viewModel = projectListViewModel,
                onOpenProject = onOpenProject,
                showThumbnails = false,
                modifier = Modifier.weight(1f).padding(top = 16.dp)
            )
        }
    }
}

/**
 * ردیف چیپ فیلتر وضعیت — طبق mockup (`stateFilters`: نقطه‌ی رنگی + برچسب،
 * اسکرول‌پذیر افقی، ۶ مورد: «همه» + ۵ EntityState). رنگ/برچسب هر وضعیت از
 * همان نگاشت `ProjectCard.kt` بازاستفاده شد (نه یک نگاشت دوم موازی).
 */
@Composable
private fun ProjectStateFilterRow(
    language: Language,
    selected: EntityState?,
    onSelect: (EntityState?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            StateFilterChip(
                label = uiString("projects.filter.all", language),
                dotColor = MaterialTheme.colorScheme.primary,
                selected = selected == null,
                onClick = { onSelect(null) },
                testTag = projectsStateFilterTag(null)
            )
        }
        items(EntityState.entries) { state ->
            StateFilterChip(
                label = stateChipLabel(state, language),
                dotColor = stateChipColor(state),
                selected = selected == state,
                onClick = { onSelect(state) },
                testTag = projectsStateFilterTag(state)
            )
        }
    }
}

@Composable
private fun StateFilterChip(label: String, dotColor: Color, selected: Boolean, onClick: () -> Unit, testTag: String) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else CinemaTheme.extendedColors.cardBorder
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = CinemaTheme.extendedColors.solidSurface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).background(color = dotColor, shape = CircleShape))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = CinemaTheme.extendedColors.fg2)
        }
    }
}
