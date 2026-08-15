package com.operaboys.cinemashotgenerator.ui.studio

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.scenes.sceneDisplayTitle
import com.operaboys.cinemashotgenerator.ui.shots.shotCode
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// یافته‌ی #۱۴ appendix ADR-081 (ADR-084) — طبق mockup (`st.output`)، ۳ عنصر:
// (۱) کارت وضعیت Validation (کلیک → Validation) با شمارش واقعی Blocking/Warning
// کل پروژه؛ (۲) کارت آماده‌بودن Prompt Generation (بدون Quality Score جعلی —
// رجوع به ADR-084 برای دلیل)؛ (۳) دکمه‌ی گرادیانی اشتراک‌گذاری (کلیک → Output
// Delivery). چون هر دو مقصد Navigate (Validation/OutputDelivery) نیازمند یک
// shotId مشخص‌اند و این Tab سطح‌پروژه است، هر دو کلیک از یک منطق مشترک
// «تفکیک شات» عبور می‌کنند: صفر شات → غیرفعال؛ یک شات → مستقیم؛ چند شات →
// دیالوگ انتخاب کوتاه (نه صفحه‌ی سنگین تازه).

const val STUDIO_OUTPUT_VALIDATION_CARD_TAG = "studioOutput.validationCard"
const val STUDIO_OUTPUT_PROMPT_CARD_TAG = "studioOutput.promptCard"
const val STUDIO_OUTPUT_SHARE_BUTTON_TAG = "studioOutput.shareButton"
const val STUDIO_OUTPUT_EMPTY_STATE_TAG = "studioOutput.emptyState"

fun studioOutputShotPickerItemTag(shotId: String): String = "studioOutput.shotPicker.item.$shotId"

private sealed class PendingOutputAction {
    data object GoValidation : PendingOutputAction()
    data object GoOutputDelivery : PendingOutputAction()
}

@Composable
fun StudioOutputTabContent(
    projectId: String,
    language: Language,
    onNavigateToValidation: (sceneId: String, sceneNumber: Int, sceneDisplayTitle: String, shotId: String) -> Unit,
    onNavigateToOutputDelivery: (sceneId: String, sceneNumber: Int, sceneDisplayTitle: String, shotId: String) -> Unit,
    shotRepository: ShotRepository? = null,
    sceneRepository: SceneRepository? = null,
    projectDnaRepository: ProjectDnaRepository? = null,
    assetRepository: AssetRepository? = null,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: StudioOutputViewModel = viewModel(
        factory = StudioOutputViewModel.factory(application, projectId, shotRepository, sceneRepository, projectDnaRepository, assetRepository)
    )
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()

    var pendingAction by remember { mutableStateOf<PendingOutputAction?>(null) }

    fun navigate(action: PendingOutputAction, shot: StudioOutputShotSummary) {
        val displayTitle = sceneDisplayTitle(shot.sceneTitle, shot.sceneNumber, language)
        when (action) {
            PendingOutputAction.GoValidation -> onNavigateToValidation(shot.sceneId, shot.sceneNumber, displayTitle, shot.shotId)
            PendingOutputAction.GoOutputDelivery -> onNavigateToOutputDelivery(shot.sceneId, shot.sceneNumber, displayTitle, shot.shotId)
        }
    }

    fun startAction(action: PendingOutputAction) {
        val shots = summary.shots
        when {
            shots.isEmpty() -> Unit
            shots.size == 1 -> navigate(action, shots.first())
            else -> pendingAction = action
        }
    }

    if (!isLoaded) return

    if (summary.totalShots == 0) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = uiString("studioOutput.emptyState", language),
                style = MaterialTheme.typography.bodyLarge,
                color = CinemaTheme.extendedColors.fg3,
                modifier = Modifier.testTag(STUDIO_OUTPUT_EMPTY_STATE_TAG)
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            onClick = { startAction(PendingOutputAction.GoValidation) },
            colors = CardDefaults.cardColors(),
            modifier = Modifier.fillMaxWidth().testTag(STUDIO_OUTPUT_VALIDATION_CARD_TAG)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(uiString("studioOutput.validationCardTitle", language), style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Filled.Error, contentDescription = null, tint = Color(0xFFFF5A6A))
                }
                Text(
                    text = uiTemplate(
                        "studioOutput.validationCountsTemplate",
                        language,
                        "blocking" to summary.totalBlockingCount.toString(),
                        "warning" to summary.totalWarningCount.toString()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = CinemaTheme.extendedColors.fg2,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(),
            // بدون onClick (این کارت صرفاً نمایشی است)، Compose به‌طور خودکار
            // متن فرزندان را در Semantics ادغام نمی‌کند (برخلاف کارت Validation
            // بالا که با onClick این ادغام را رایگان می‌گیرد) — درخواست صریح
            // ادغام هم برای دسترس‌پذیری صحیح (خواننده‌ی صفحه این وضعیت را یک واحد
            // معنایی می‌خواند) و هم برای testTag همین کارت لازم است.
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .testTag(STUDIO_OUTPUT_PROMPT_CARD_TAG)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(uiString("studioOutput.promptGenerationCardTitle", language), style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(color = CinemaTheme.extendedColors.inset, shape = RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF3DDC97))
                    Text(
                        text = uiTemplate(
                            "studioOutput.readyShotsTemplate",
                            language,
                            "ready" to summary.readyShotCount.toString(),
                            "total" to summary.totalShots.toString()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = CinemaTheme.extendedColors.fg2
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { startAction(PendingOutputAction.GoOutputDelivery) }
                .background(
                    brush = Brush.linearGradient(listOf(Color(0xFF7C5CFF), Color(0xFF8E74FF))),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(vertical = 16.dp)
                .testTag(STUDIO_OUTPUT_SHARE_BUTTON_TAG),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.IosShare, contentDescription = null, tint = Color.White)
            Text(
                text = uiString("studioOutput.shareButtonLabel", language),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    val action = pendingAction
    if (action != null) {
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(uiString("studioOutput.pickShotDialogTitle", language)) },
            text = {
                Column {
                    summary.shots.forEach { shot ->
                        Text(
                            text = "${shotCode(shot.sceneNumber, shot.shotNumber)} — ${sceneDisplayTitle(shot.sceneTitle, shot.sceneNumber, language)}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navigate(action, shot)
                                    pendingAction = null
                                }
                                .testTag(studioOutputShotPickerItemTag(shot.shotId))
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text(uiString("project.rename.cancel", language))
                }
            }
        )
    }
}
