package com.operaboys.cinemashotgenerator.ui.storybreakdown

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.storybreakdown.JsonDiagnosis
import com.operaboys.cinemashotgenerator.domain.storybreakdown.StoryBreakdownResult
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme
import com.operaboys.cinemashotgenerator.ui.theme.minTouchTargetIfEnabled

// واحد ۱۶ فاز ۲ — قدم ۲: صفحه‌ی واقعی AI Story Breakdown — اولین UI کل زنجیره‌ی
// PromptBuilder→ChunkCombiner→JsonDoctor→StoryToDomainMapper (واحد ۰۱ب). سه فاز
// طبق docs/design/README.md بخش «۴. AI Story Breakdown». جزئیات کامل هر تصمیم
// (رنگ فعال/غیرفعال Stepper، امکان برگشت به فاز قبلی با لمس دایره، مقصد Navigation
// بعد از تأیید فاز ۳، و ...) در docs/adr/046-unit16-phase2-step2-ai-story-breakdown.md.

/** testTag برای فیلدهای متنی چندخطی — طبق الگوی CREATE_PROJECT_NAME_FIELD_TAG (HomeScreen.kt)، چون متن Label به‌تنهایی برای هدف‌گیری دقیق در تست کافی نیست. */
const val AI_BREAKDOWN_STORY_FIELD_TAG = "aiBreakdown.storyField"
const val AI_BREAKDOWN_PASTE_FIELD_TAG = "aiBreakdown.pasteField"

/** یافته‌ی واقعی تست: متن این دکمه («تولید پرامپت») عیناً با کلید موجود «drawer.promptGenerator» یکسان است — و چون محتوای Drawer همیشه در Composition زنده می‌ماند (طبق یادداشت BottomNavBar.kt)، onNodeWithText روی این متن Ambiguous می‌شود؛ این دکمه به testTag جدا نیاز دارد. */
const val AI_BREAKDOWN_GENERATE_PROMPT_BUTTON_TAG = "aiBreakdown.generatePromptButton"

/** رفع G14 ممیزی post-Unit16 — کارت خطای واقعی validateTargetShotCountRange (به‌جای کوتاه‌سازی خاموش). */
const val AI_BREAKDOWN_TARGET_SHOT_COUNT_ERROR_TAG = "aiBreakdown.targetShotCountError"

@Composable
fun AiStoryBreakdownScreen(
    projectId: String,
    language: Language,
    onBack: () -> Unit,
    onConfirmedAndSaved: () -> Unit,
    storyRepository: StoryRepository? = null,
    assetRepository: AssetRepository? = null,
    sceneRepository: SceneRepository? = null,
    shotRepository: ShotRepository? = null,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: AiStoryBreakdownViewModel = viewModel(
        factory = AiStoryBreakdownViewModel.factory(
            application, projectId, storyRepository, assetRepository, sceneRepository, shotRepository
        )
    )

    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val freeformStory by viewModel.freeformStory.collectAsStateWithLifecycle()
    val targetShotCount by viewModel.targetShotCount.collectAsStateWithLifecycle()
    val targetShotCountError by viewModel.targetShotCountError.collectAsStateWithLifecycle()
    val defaultShotDurationSeconds by viewModel.defaultShotDurationSeconds.collectAsStateWithLifecycle()
    val generatedPrompt by viewModel.generatedPrompt.collectAsStateWithLifecycle()
    val chunks by viewModel.chunks.collectAsStateWithLifecycle()
    val currentChunkInput by viewModel.currentChunkInput.collectAsStateWithLifecycle()
    val repairDiagnosis by viewModel.repairDiagnosis.collectAsStateWithLifecycle()
    val processingError by viewModel.processingError.collectAsStateWithLifecycle()
    val breakdownResult by viewModel.breakdownResult.collectAsStateWithLifecycle()
    val saveCompleted by viewModel.saveCompleted.collectAsStateWithLifecycle()

    LaunchedEffect(saveCompleted) {
        if (saveCompleted) onConfirmedAndSaved()
    }

    Column(modifier = modifier.fillMaxSize()) {
        BreakdownHeader(language = language, onBack = onBack)
        PhaseStepperRow(
            currentPhase = phase,
            onPhaseSelected = { selected ->
                if (selected.ordinal <= phase.ordinal) viewModel.setPhase(selected)
            }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (phase) {
                BreakdownPhase.WRITE_STORY -> Phase1WriteStory(
                    language = language,
                    freeformStory = freeformStory,
                    onFreeformStoryChange = viewModel::setFreeformStory,
                    targetShotCount = targetShotCount,
                    onTargetShotCountChange = viewModel::setTargetShotCount,
                    targetShotCountError = targetShotCountError,
                    defaultShotDurationSeconds = defaultShotDurationSeconds,
                    onDefaultShotDurationSecondsChange = viewModel::setDefaultShotDurationSeconds,
                    generatedPrompt = generatedPrompt,
                    onGeneratePrompt = viewModel::generatePrompt,
                    onProceedToPhase2 = { viewModel.setPhase(BreakdownPhase.PASTE_RESPONSE) }
                )
                BreakdownPhase.PASTE_RESPONSE -> Phase2PasteResponse(
                    language = language,
                    chunks = chunks,
                    currentChunkInput = currentChunkInput,
                    onCurrentChunkInputChange = viewModel::setCurrentChunkInput,
                    onAddChunk = viewModel::addChunk,
                    onContinue = viewModel::processResponse,
                    processingError = processingError,
                    onDismissProcessingError = viewModel::clearProcessingError
                )
                BreakdownPhase.FINAL_REVIEW -> breakdownResult?.let { result ->
                    Phase3FinalReview(
                        language = language,
                        result = result,
                        onConfirm = viewModel::confirmAndSave
                    )
                }
            }
        }
    }

    if (repairDiagnosis != null) {
        JsonRepairDialog(
            language = language,
            diagnosis = repairDiagnosis!!,
            onAutoFix = viewModel::attemptAutoFixAndCollapse,
            onManualEdit = viewModel::dismissRepairModalForManualEdit
        )
    }
}

@Composable
private fun BreakdownHeader(language: Language, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(text = uiString("drawer.aiBreakdown", language), style = MaterialTheme.typography.titleMedium)
            Text(
                text = uiString("aiBreakdown.subtitle", language),
                style = MaterialTheme.typography.labelSmall,
                color = CinemaTheme.extendedColors.fg3
            )
        }
    }
}

/**
 * ۳ دایره‌ی شماره‌دار به هم متصل — طبق تصریح صریح سند طراحی، حالت غیرفعال یک «کاشی
 * خنثی با حاشیه» است (cardBorder + سطح surface)، نه صرفاً نسخه‌ی کم‌شفافیت رنگ فعال
 * (که خوانایی روی پس‌زمینه‌ی محو را طبق یادداشت طراحی به خطر می‌انداخت).
 */
@Composable
private fun PhaseStepperRow(currentPhase: BreakdownPhase, onPhaseSelected: (BreakdownPhase) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BreakdownPhase.entries.forEachIndexed { index, entry ->
            PhaseCircle(
                number = index + 1,
                active = entry == currentPhase,
                enabled = entry.ordinal <= currentPhase.ordinal,
                onClick = { onPhaseSelected(entry) }
            )
            if (index != BreakdownPhase.entries.lastIndex) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            if (entry.ordinal < currentPhase.ordinal) MaterialTheme.colorScheme.primary
                            else CinemaTheme.extendedColors.cardBorder
                        )
                )
            }
        }
    }
}

@Composable
private fun PhaseCircle(number: Int, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val background = if (active) MaterialTheme.colorScheme.primary else CinemaTheme.extendedColors.solidSurface
    val contentColor = if (active) MaterialTheme.colorScheme.onPrimary else CinemaTheme.extendedColors.fg2
    val borderColor = if (active) MaterialTheme.colorScheme.primary else CinemaTheme.extendedColors.cardBorder

    // رفع بخشی G12 ممیزی post-Unit16 (اولویت ۳ بند ۸): minTouchTargetEnabled —
    // Box بیرونی (نه دایره‌ی ۳۲dp خودش) حداقل اندازه‌ی لمس را می‌گیرد تا شکل
    // بصری دایره وقتی سوییچ فعال است بزرگ نشود، فقط ناحیه‌ی واقعی لمس.
    Box(
        modifier = Modifier
            .minTouchTargetIfEnabled()
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(background, CircleShape)
                .border(1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(2.dp)
            )
        }
    }
}

@Composable
private fun Phase1WriteStory(
    language: Language,
    freeformStory: String,
    onFreeformStoryChange: (String) -> Unit,
    targetShotCount: Int,
    onTargetShotCountChange: (Int) -> Unit,
    targetShotCountError: String?,
    defaultShotDurationSeconds: Float,
    onDefaultShotDurationSecondsChange: (Float) -> Unit,
    generatedPrompt: String?,
    onGeneratePrompt: () -> Unit,
    onProceedToPhase2: () -> Unit
) {
    OutlinedTextField(
        value = freeformStory,
        onValueChange = onFreeformStoryChange,
        label = { Text(uiString("aiBreakdown.storyLabel", language)) },
        placeholder = { Text(uiString("aiBreakdown.storyPlaceholder", language)) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .testTag(AI_BREAKDOWN_STORY_FIELD_TAG)
    )

    IntStepperField(
        label = uiString("story.targetShotsLabel", language),
        value = targetShotCount,
        onValueChange = onTargetShotCountChange,
        step = 1
    )

    if (targetShotCountError != null) {
        Card(modifier = Modifier.fillMaxWidth().testTag(AI_BREAKDOWN_TARGET_SHOT_COUNT_ERROR_TAG)) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    text = targetShotCountError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    FloatStepperField(
        label = uiString("story.secondsPerShotLabel", language),
        value = defaultShotDurationSeconds,
        onValueChange = onDefaultShotDurationSecondsChange,
        step = 1f
    )

    Button(
        onClick = onGeneratePrompt,
        enabled = targetShotCountError == null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AI_BREAKDOWN_GENERATE_PROMPT_BUTTON_TAG)
    ) {
        Text(uiString("aiBreakdown.generatePromptButton", language))
    }

    if (generatedPrompt != null) {
        GeneratedPromptCard(language = language, prompt = generatedPrompt)
        Button(onClick = onProceedToPhase2, modifier = Modifier.fillMaxWidth()) {
            Text(uiString("aiBreakdown.proceedToPhase2Button", language))
        }
    }
}

@Composable
private fun GeneratedPromptCard(language: Language, prompt: String) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = uiString("aiBreakdown.promptCardHint", language),
                style = MaterialTheme.typography.labelMedium,
                color = CinemaTheme.extendedColors.fg3
            )
            Text(text = prompt, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick = { clipboardManager.setText(AnnotatedString(prompt)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(uiString("aiBreakdown.copyButton", language))
            }
        }
    }
}

@Composable
private fun Phase2PasteResponse(
    language: Language,
    chunks: List<String>,
    currentChunkInput: String,
    onCurrentChunkInputChange: (String) -> Unit,
    onAddChunk: () -> Unit,
    onContinue: () -> Unit,
    processingError: String?,
    onDismissProcessingError: () -> Unit
) {
    if (chunks.isNotEmpty()) {
        Text(
            text = uiTemplate("aiBreakdown.chunksCountTemplate", language, "count" to chunks.size.toString()),
            style = MaterialTheme.typography.labelMedium,
            color = CinemaTheme.extendedColors.fg3
        )
    }

    OutlinedTextField(
        value = currentChunkInput,
        onValueChange = onCurrentChunkInputChange,
        label = { Text(uiString("aiBreakdown.pasteLabel", language)) },
        placeholder = { Text(uiString("aiBreakdown.pastePlaceholder", language)) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .testTag(AI_BREAKDOWN_PASTE_FIELD_TAG)
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onAddChunk, modifier = Modifier.weight(1f)) {
            Text(uiString("aiBreakdown.newChunkButton", language))
        }
        Button(onClick = onContinue, modifier = Modifier.weight(1f)) {
            Text(uiString("aiBreakdown.continueButton", language))
        }
    }

    if (processingError != null) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    text = processingError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismissProcessingError) {
                    Text(uiString("aiBreakdown.dismissButton", language))
                }
            }
        }
    }
}

@Composable
private fun JsonRepairDialog(
    language: Language,
    diagnosis: JsonDiagnosis,
    onAutoFix: () -> Unit,
    onManualEdit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onManualEdit,
        title = { Text(uiString("aiBreakdown.repairModalTitle", language)) },
        text = { Text(diagnosis.simpleExplanation) },
        confirmButton = {
            if (diagnosis.autoFixable) {
                TextButton(onClick = onAutoFix) {
                    Text(uiString("aiBreakdown.repairAutoButton", language))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onManualEdit) {
                Text(uiString("aiBreakdown.repairManualButton", language))
            }
        }
    )
}

@Composable
private fun Phase3FinalReview(language: Language, result: StoryBreakdownResult, onConfirm: () -> Unit) {
    Text(text = uiString("aiBreakdown.reviewTitle", language), style = MaterialTheme.typography.titleMedium)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ReviewCountRow(uiString("aiBreakdown.reviewCharacters", language), result.characters.size)
            ReviewCountRow(uiString("aiBreakdown.reviewLocations", language), result.locations.size)
            ReviewCountRow(uiString("aiBreakdown.reviewObjects", language), result.objects.size)
            ReviewCountRow(uiString("aiBreakdown.reviewScenes", language), result.scenes.size)
            ReviewCountRow(uiString("aiBreakdown.reviewShots", language), result.shots.size)
        }
    }

    if (result.warnings.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = uiString("aiBreakdown.warningsHeader", language),
                style = MaterialTheme.typography.labelLarge,
                color = CinemaTheme.extendedColors.warning
            )
            result.warnings.forEach { issue -> WarningRow(issue) }
        }
    }

    Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
        Text(uiString("aiBreakdown.confirmButton", language))
    }
}

@Composable
private fun ReviewCountRow(label: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = count.toString(), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun WarningRow(issue: ValidationIssue) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = CinemaTheme.extendedColors.warning)
        Text(text = issue.message, style = MaterialTheme.typography.bodySmall, color = CinemaTheme.extendedColors.warning)
    }
}

@Composable
private fun IntStepperField(label: String, value: Int, onValueChange: (Int) -> Unit, step: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
private fun FloatStepperField(label: String, value: Float, onValueChange: (Float) -> Unit, step: Float) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
