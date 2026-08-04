package com.operaboys.cinemashotgenerator.ui.story

import android.app.Application
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.story.Genre
import com.operaboys.cinemashotgenerator.domain.story.NarrativeIntensity
import com.operaboys.cinemashotgenerator.domain.story.StoryType
import com.operaboys.cinemashotgenerator.domain.story.VisualIntent
import com.operaboys.cinemashotgenerator.domain.story.validateStoryContext
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۲ — قدم ۱ — بخش ب: محتوای واقعی Tab «داستان»، جایگزین Placeholder فاز ۱.
//
// طبق docs/design/README.md («Story tab: title field, story textarea (read-only
// display in prototype), target-shots stepper, seconds-per-shot stepper») +
// docs/blueprints/16-user-workflow-v2.md مرحله ۱ (StoryType/Genre/Mood/
// NarrativeIntensity/VisualIntent). تصمیمات مستقل کامل در
// docs/adr/045-unit16-phase2-step1-story-tab.md — خلاصه:
// - «فیلد عنوان» همان Project.projectName است (StoryContext فیلد عنوان ندارد)،
//   با بازاستفاده از ProjectListViewModel.renameProject موجود فاز ۱.
// - Stepper های «هدف تعداد شات»/«ثانیه به‌ازای شات» متعلق به StoryBreakdownRequest
//   واحد ۰۱ب هستند (نه StoryContext)؛ اینجا فقط State محلی (rememberSaveable) این
//   صفحه‌اند، آماده برای پاس‌شدن به فاز AI Breakdown (قدم ۲) — بدون ذخیره‌سازی Room.
// - «Story textarea» به‌صورت Placeholder فقط‌خواندنی نمایش داده می‌شود (طبق تصریح
//   صریح سند طراحی «read-only display in prototype») — ویرایش واقعی داستان آزاد
//   کار قدم ۲ (AI Story Breakdown) است.
// - VisualIntent/NarrativeIntensity به‌صورت Dropdown با مقادیر واقعی enum دامنه
//   رندر شدند، نه Textarea/Slider آزاد طبق یادداشت قدیمی‌تر v4 بلوپرینت (که با نوع
//   واقعی Kotlin این دو فیلد — enum، نه String — در تناقض بود؛ نوع دامنه‌ی واقعی
//   منبع حقیقت است).
// - `storyRepository` تزریق‌پذیر (پیش‌فرض null) — یافته‌ی واقعی تست: بدون این
//   پارامتر، StoryViewModel (که با viewModel(factory=...) داخل همین Composable
//   ساخته می‌شود) همیشه از AppDatabase.getInstance(application) (Singleton واقعی
//   دستگاه) استفاده می‌کرد و هیچ راهی برای جایگزینی با دیتابیس In-Memory تست وجود
//   نداشت — نوشتن‌ها بی‌صدا موفق می‌شدند اما به دیتابیس اشتباهی می‌رفتند. جزئیات
//   کامل در docs/adr/045-unit16-phase2-step1-story-tab.md.

/** طبق یادداشت BottomNavBar.kt: محتوای Drawer همیشه در Composition زنده می‌ماند، پس متن «drawer.aiBreakdown» به‌تنهایی برای onNodeWithText در تست Ambiguous است — این دکمه به testTag جدا نیاز دارد. */
const val STORY_TAB_AI_BREAKDOWN_BUTTON_TAG = "story.aiBreakdownButton"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryTabContent(
    projectId: String,
    projectListViewModel: ProjectListViewModel,
    language: Language,
    storyRepository: StoryRepository? = null,
    onNavigateToAiBreakdown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val storyViewModel: StoryViewModel = viewModel(factory = StoryViewModel.factory(application, projectId, storyRepository))
    val storyContext by storyViewModel.storyContext.collectAsStateWithLifecycle()

    val summaries by projectListViewModel.projectSummaries.collectAsStateWithLifecycle()
    val projectName = summaries.find { it.project.projectId == projectId }?.project?.projectName ?: ""

    var targetShots by rememberSaveable { mutableIntStateOf(10) }
    var secondsPerShot by rememberSaveable { mutableFloatStateOf(4f) }

    val issues = remember(storyContext) { validateStoryContext(storyContext) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = projectName,
            onValueChange = { newName -> projectListViewModel.renameProject(projectId, newName) },
            label = { Text(uiString("story.titleLabel", language)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        EnumDropdownField(
            label = uiString("story.storyTypeLabel", language),
            selectedLabel = storyTypeLabel(storyContext.storyType, language),
            options = StoryType.entries,
            optionLabel = { storyTypeLabel(it, language) },
            onSelected = storyViewModel::setStoryType
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = uiString("story.genreLabel", language), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Genre.entries.forEach { genre ->
                    FilterChip(
                        selected = genre in storyContext.genre,
                        onClick = { storyViewModel.toggleGenre(genre) },
                        label = { Text(genreLabel(genre, language)) }
                    )
                }
            }
        }

        EnumDropdownField(
            label = uiString("story.moodPrimaryLabel", language),
            selectedLabel = moodLabel(storyContext.moodPrimary, language),
            options = Mood.entries,
            optionLabel = { moodLabel(it, language) },
            onSelected = storyViewModel::setMoodPrimary
        )

        EnumDropdownField(
            label = uiString("story.moodSecondaryLabel", language),
            selectedLabel = storyContext.moodSecondary?.let { moodLabel(it, language) } ?: uiString("story.moodSecondaryNone", language),
            options = listOf<Mood?>(null) + Mood.entries,
            optionLabel = { it?.let { mood -> moodLabel(mood, language) } ?: uiString("story.moodSecondaryNone", language) },
            onSelected = storyViewModel::setMoodSecondary
        )

        EnumDropdownField(
            label = uiString("story.narrativeIntensityLabel", language),
            selectedLabel = narrativeIntensityLabel(storyContext.narrativeIntensity, language),
            options = NarrativeIntensity.entries,
            optionLabel = { narrativeIntensityLabel(it, language) },
            onSelected = storyViewModel::setNarrativeIntensity
        )

        EnumDropdownField(
            label = uiString("story.visualIntentLabel", language),
            selectedLabel = visualIntentLabel(storyContext.visualIntent, language),
            options = VisualIntent.entries,
            optionLabel = { visualIntentLabel(it, language) },
            onSelected = storyViewModel::setVisualIntent
        )

        Text(
            text = uiString("story.freeStoryPlaceholder", language),
            style = MaterialTheme.typography.bodyMedium,
            color = CinemaTheme.extendedColors.fg3
        )

        Button(
            onClick = onNavigateToAiBreakdown,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(STORY_TAB_AI_BREAKDOWN_BUTTON_TAG)
        ) {
            Text(uiString("drawer.aiBreakdown", language))
        }

        StepperField(
            label = uiString("story.targetShotsLabel", language),
            value = targetShots,
            onValueChange = { targetShots = it.coerceIn(1, 150) },
            step = 1
        )

        StepperField(
            label = uiString("story.secondsPerShotLabel", language),
            value = secondsPerShot,
            onValueChange = { secondsPerShot = it.coerceIn(1f, 60f) },
            step = 1f
        )

        if (issues.isNotEmpty()) {
            ValidationIssuesList(issues = issues, language = language)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdownField(
    label: String,
    selectedLabel: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StepperField(label: String, value: Int, onValueChange: (Int) -> Unit, step: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Row {
            IconButton(onClick = { onValueChange(value - step) }) {
                Icon(Icons.Filled.Remove, contentDescription = null)
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = { onValueChange(value + step) }) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }
    }
}

@Composable
private fun StepperField(label: String, value: Float, onValueChange: (Float) -> Unit, step: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Row {
            IconButton(onClick = { onValueChange(value - step) }) {
                Icon(Icons.Filled.Remove, contentDescription = null)
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = { onValueChange(value + step) }) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ValidationIssuesList(issues: List<ValidationIssue>, language: Language) {
    val blocking = issues.filter { it.severity == Severity.BLOCKING }
    val warnings = issues.filter { it.severity == Severity.WARNING }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (blocking.isNotEmpty()) {
            Text(
                text = uiString("story.validationBlockingHeader", language),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error
            )
            blocking.forEach { issue -> ValidationIssueRow(issue = issue, color = MaterialTheme.colorScheme.error, icon = Icons.Filled.Error) }
        }
        if (warnings.isNotEmpty()) {
            Text(
                text = uiString("story.validationWarningHeader", language),
                style = MaterialTheme.typography.labelLarge,
                color = CinemaTheme.extendedColors.warning
            )
            warnings.forEach { issue -> ValidationIssueRow(issue = issue, color = CinemaTheme.extendedColors.warning, icon = Icons.Filled.Warning) }
        }
    }
}

@Composable
private fun ValidationIssueRow(issue: ValidationIssue, color: Color, icon: ImageVector) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = color)
        Text(text = issue.message, color = color, style = MaterialTheme.typography.bodyMedium)
    }
}
