package com.operaboys.cinemashotgenerator.ui.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.navigation.StudioTab
import com.operaboys.cinemashotgenerator.ui.navigation.StudioTopTabRow
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.story.StoryTabContent
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel

// واحد ۱۶ فاز ۱ — Studio Shell طبق دستور کار بخش ج: Header (برگشت، عنوان، زیرعنوان
// شمارش صحنه/شات، چیپ «ذخیره شد») + ۴ Tab (فاز ۰، اینجا واقعاً به محتوا وصل شدند).
// MIGRATED (واحد ۱۶ فاز ۲، قدم ۱): Tab «داستان» اکنون محتوای واقعی دارد
// (StoryTabContent)؛ بقیه (DNA/صحنه‌ها/خروجی) همچنان Placeholder («این بخش در فاز
// بعدی تکمیل می‌شود») — کار فازهای ۲ (ادامه) تا ۵.
//
// بازساختاردهی نسبت به فاز ۰ (مستند در ADR-044): StudioTopTabRow قبلاً در
// MainScaffold (بیرون از Navigation Graph) رندر می‌شد، بدون این‌که به محتوای واقعی
// وصل باشد (چون محتوایی نبود). حالا که این فاز باید هر Tab را به یک بدنه‌ی واقعی
// وصل کند، TopTabRow + State انتخاب Tab به داخل همین Composable (مسیر Studio
// واقعی) منتقل شدند — یک بازسازی ضروری، نه Scope Creep.
//
// طبق docs/design/README.md بخش State Management («Active project tab — نه در
// دسته‌ی Persisted»)، انتخاب Tab با rememberSaveable محلی نگه داشته می‌شود، نه در
// WorkflowViewModel/DataStore.

@Composable
fun StudioShell(
    projectId: String,
    workflowViewModel: WorkflowViewModel,
    projectListViewModel: ProjectListViewModel,
    onBack: () -> Unit,
    onWarning: (String) -> Unit,
    storyRepository: StoryRepository? = null,
    onNavigateToAiBreakdown: (String) -> Unit = {}
) {
    val language by workflowViewModel.language.collectAsStateWithLifecycle()
    val workflowState by workflowViewModel.workflowState.collectAsStateWithLifecycle()
    val summaries by projectListViewModel.projectSummaries.collectAsStateWithLifecycle()
    val summary = summaries.find { it.project.projectId == projectId }

    // اتصال واقعی startWorkflowSession (که در فاز ۰ ساخته شد ولی هیچ فراخوان
    // واقعی‌ای نداشت — ADR-042: «فراخوان واقعی این تابع هنگام ورود به Studio کار
    // فاز بعدی است») — دقیقاً همین لحظه. فقط وقتی Session فعلی برای پروژه‌ی دیگری
    // است (یا اصلاً وجود ندارد) دوباره ساخته می‌شود، نه در هر Recomposition.
    LaunchedEffect(projectId) {
        if (workflowState?.projectId != projectId) {
            workflowViewModel.startWorkflowSession(projectId)
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf(StudioTab.STORY) }

    Column(modifier = Modifier.fillMaxSize()) {
        StudioHeader(
            title = summary?.project?.projectName ?: projectId,
            sceneCount = summary?.sceneCount ?: 0,
            shotCount = summary?.shotCount ?: 0,
            language = language,
            onBack = onBack
        )
        StudioTopTabRow(
            selectedTab = selectedTab,
            workflowState = workflowState,
            language = language,
            onTabSelected = { selectedTab = it },
            onWarning = onWarning
        )
        if (selectedTab == StudioTab.STORY) {
            StoryTabContent(
                projectId = projectId,
                projectListViewModel = projectListViewModel,
                language = language,
                storyRepository = storyRepository,
                onNavigateToAiBreakdown = { onNavigateToAiBreakdown(projectId) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            StudioTabPlaceholder(language = language, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun StudioHeader(title: String, sceneCount: Int, shotCount: Int, language: Language, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = uiTemplate("project.metaTemplate", language, "scenes" to sceneCount.toString(), "shots" to shotCount.toString()),
                style = MaterialTheme.typography.labelSmall,
                color = CinemaTheme.extendedColors.fg3
            )
        }
        Surface(shape = RoundedCornerShape(18.dp), color = CinemaTheme.extendedColors.success.copy(alpha = 0.16f)) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = CinemaTheme.extendedColors.success, modifier = Modifier.padding(0.dp))
                Text(text = uiString("studio.saved", language), style = MaterialTheme.typography.labelSmall, color = CinemaTheme.extendedColors.success)
            }
        }
    }
}

@Composable
private fun StudioTabPlaceholder(language: Language, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = uiString("studio.tabPlaceholder", language),
            style = MaterialTheme.typography.bodyLarge,
            color = CinemaTheme.extendedColors.fg3
        )
    }
}
