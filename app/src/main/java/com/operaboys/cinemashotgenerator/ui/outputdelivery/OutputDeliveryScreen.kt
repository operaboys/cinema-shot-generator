package com.operaboys.cinemashotgenerator.ui.outputdelivery

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.data.repository.PromptGenerationRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ALL_MODEL_PROFILES
import com.operaboys.cinemashotgenerator.domain.outputdelivery.RenderedOutput
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelProfile
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.promptfinalization.TokenCheckResult
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormHeader
import com.operaboys.cinemashotgenerator.ui.assets.OpaqueChip
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۵ — قدم ۳ (آخرین قدم، طبق ADR-056 شامل هر دو وظیفه‌ی «تولید Prompt» و
// «تحویل خروجی»): صفحه‌ی Output Delivery. طبق docs/design/README.md بخش «۱۰. Output
// Delivery»: Model Picker (چیپ Wrap‌شونده + هزینه‌ی Token هر مدل) + Output Preview
// Card (Badge، متن Read-only، خط هزینه‌ی Token + هشدار Over-limit، ردیف Copy/
// Regenerate، دکمه‌ی تمام‌عرض Export) + بخش Warnings. جزئیات کامل تصمیمات
// (خصوصاً منبع «هزینه‌ی Token») در docs/adr/057-unit16-phase5-step3-output-delivery.md.

const val OUTPUT_DELIVERY_BACK_BUTTON_TAG = "outputDelivery.backButton"
const val OUTPUT_DELIVERY_PREVIEW_CARD_TAG = "outputDelivery.previewCard"
const val OUTPUT_DELIVERY_COPY_BUTTON_TAG = "outputDelivery.copyButton"
const val OUTPUT_DELIVERY_REGENERATE_BUTTON_TAG = "outputDelivery.regenerateButton"
const val OUTPUT_DELIVERY_EXPORT_BUTTON_TAG = "outputDelivery.exportButton"

fun outputDeliveryModelChipTag(profileId: String): String = "outputDelivery.modelChip.$profileId"

@Composable
fun OutputDeliveryScreen(
    shotId: String,
    language: Language,
    onBack: () -> Unit,
    initialModelProfileId: String?,
    onModelProfileSelected: (String) -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    promptGenerationRepository: PromptGenerationRepository? = null,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: OutputDeliveryViewModel = viewModel(
        factory = OutputDeliveryViewModel.factory(application, shotId, initialModelProfileId, promptGenerationRepository)
    )
    val selectedProfileId by viewModel.selectedProfileId.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    Column(modifier = modifier) {
        AssetFormHeader(
            title = uiString("drawer.outputDelivery", language),
            subtitle = uiString("outputDelivery.subtitle", language),
            onBack = onBack,
            backTestTag = OUTPUT_DELIVERY_BACK_BUTTON_TAG
        )

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModelPickerSection(
                selectedProfileId = selectedProfileId,
                language = language,
                onSelect = { profileId ->
                    viewModel.selectProfile(profileId)
                    onModelProfileSelected(profileId)
                }
            )

            when (val currentState = state) {
                is OutputDeliveryState.Loading -> Text(
                    text = uiString("outputDelivery.loadingState", language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CinemaTheme.extendedColors.fg3
                )
                is OutputDeliveryState.Error -> Text(
                    text = "${uiString("outputDelivery.errorPrefix", language)} ${currentState.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                is OutputDeliveryState.Ready -> {
                    val profile = viewModel.profileFor(selectedProfileId)
                    OutputPreviewCard(
                        renderedOutput = currentState.renderedOutput,
                        profile = profile,
                        language = language,
                        tokenCheck = currentState.tokenCheck,
                        cleaningSucceeded = currentState.cleaningSucceeded,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(currentState.renderedOutput.formattedPrompt))
                            onShowMessage(uiString("outputDelivery.copiedMessage", language))
                        },
                        onRegenerate = { viewModel.regenerate() },
                        onExport = {
                            clipboardManager.setText(AnnotatedString(currentState.renderedOutput.formattedPrompt))
                            onShowMessage(uiString("outputDelivery.exportedMessage", language))
                        }
                    )
                    WarningsSection(warnings = currentState.warnings, language = language)
                }
            }
        }
    }
}

/**
 * Grid چیپ Wrap‌شونده — پیاده‌سازی با تقسیم دستی فهرست به ردیف‌های ۲تایی داخل
 * `Row`، نه `LazyVerticalGrid`/`FlowRow`: این صفحه از قبل در یک `Column` با
 * Scroll عمودی است، و `LazyVerticalGrid` تو در توی یک Scroll دیگر نیاز به
 * ارتفاع صریح/محاسبه‌شده دارد (به‌خصوص با `GridCells.Adaptive` که تعداد ستون
 * واقعی‌اش فقط در Runtime معلوم می‌شود) — یک منبع باگ واقعی. `FlowRow` هم تا این
 * قدم هیچ‌جای کدبیس استفاده نشده (بدون سابقه‌ی اثبات‌شده در این نسخه‌ی Compose).
 * تقسیم دستی به ردیف‌های ثابت، ساده و بدون این ریسک است. چیپ‌ها از `OpaqueChip`
 * موجود بازاستفاده شدند (هم‌الگو با هر چیپ دیگر این اپ) — رنگ حالت انتخاب‌شده‌ی
 * آن از قبل Solid/Opaque است (`MaterialTheme.colorScheme.primary`)، دقیقاً همان
 * الزام Contrast سند طراحی؛ گرادیان بنفشِ متن سند طراحی برای این یک چیپ به‌صورت
 * بصری Bespoke ساخته نشد، چون هیچ چیپ دیگری در کل این پروژه هم گرادیان ندارد —
 * یکدستی با الگوی مستقر، نه ساخت یک نوع Chip تازه فقط برای این صفحه.
 */
@Composable
private fun ModelPickerSection(selectedProfileId: String, language: Language, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(uiString("outputDelivery.modelPickerSectionTitle", language), style = MaterialTheme.typography.titleMedium)
        ALL_MODEL_PROFILES.chunked(2).forEach { rowProfiles ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowProfiles.forEach { profile ->
                    Row(modifier = Modifier.weight(1f)) {
                        OpaqueChip(
                            label = "${modelProfileDisplayName(profile.profileId)} · ${profile.constraints.maxTokens}",
                            selected = profile.profileId == selectedProfileId,
                            onClick = { onSelect(profile.profileId) },
                            testTag = outputDeliveryModelChipTag(profile.profileId)
                        )
                    }
                }
                if (rowProfiles.size == 1) Row(modifier = Modifier.weight(1f)) {}
            }
        }
    }
}

@Composable
private fun OutputPreviewCard(
    renderedOutput: RenderedOutput,
    profile: ModelProfile,
    language: Language,
    tokenCheck: TokenCheckResult?,
    cleaningSucceeded: Boolean,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onExport: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag(OUTPUT_DELIVERY_PREVIEW_CARD_TAG)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = uiTemplate("outputDelivery.previewTitleTemplate", language, "model" to modelProfileDisplayName(profile.profileId)),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                // رفع G17 ممیزی post-Unit16: این بج تا این قدم بدون قید و شرط نمایش
                // داده می‌شد، بدون اینکه پاک‌سازی/بررسی توکن واقعی (واحد ۱۳) اجرا شده
                // باشد. اکنون فقط وقتی cleaningSucceeded=true (یعنی finalizePrompt
                // واقعاً و بدون خطا اجرا شده) نمایش داده می‌شود.
                if (cleaningSucceeded) {
                    Surface(shape = RoundedCornerShape(8.dp), color = CinemaTheme.extendedColors.solidSurface, border = BorderStroke(1.dp, CinemaTheme.extendedColors.cardBorder)) {
                        Text(
                            text = uiString("outputDelivery.cleanedFinalizedBadge", language),
                            style = MaterialTheme.typography.labelSmall,
                            color = CinemaTheme.extendedColors.fg3,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(text = renderedOutput.formattedPrompt, style = MaterialTheme.typography.bodySmall)

            // رفع G17: قبلاً همیشه فقط maxTokens (سقف پروفایل) نمایش داده می‌شد، بدون
            // هیچ عدد Estimated واقعی. اکنون که tokenCheck واقعی در دسترس است، تعداد
            // توکن تخمینی واقعی/سقف نمایش داده می‌شود؛ اگر Pipeline اجرا نشده باشد
            // (tokenCheck=null)، به همان متن قبلی (فقط سقف) Fallback می‌کند.
            Text(
                text = if (tokenCheck != null) {
                    "${uiString("outputDelivery.tokenCostLabel", language)}: ${tokenCheck.estimatedTokens}/${tokenCheck.maxTokens}"
                } else {
                    "${uiString("outputDelivery.tokenCostLabel", language)}: ${profile.constraints.maxTokens}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (tokenCheck?.withinLimit == false) MaterialTheme.colorScheme.error else CinemaTheme.extendedColors.fg3
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f).testTag(OUTPUT_DELIVERY_COPY_BUTTON_TAG)) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(uiString("outputDelivery.copyButton", language))
                }
                OutlinedButton(onClick = onRegenerate, modifier = Modifier.weight(1f).testTag(OUTPUT_DELIVERY_REGENERATE_BUTTON_TAG)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(uiString("outputDelivery.regenerateButton", language))
                }
            }

            Button(onClick = onExport, modifier = Modifier.fillMaxWidth().testTag(OUTPUT_DELIVERY_EXPORT_BUTTON_TAG)) {
                Text(uiString("outputDelivery.exportButton", language))
            }
        }
    }
}

@Composable
private fun WarningsSection(warnings: List<ValidationIssue>, language: Language) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(uiString("outputDelivery.warningsSectionTitle", language), style = MaterialTheme.typography.titleMedium)
        if (warnings.isEmpty()) {
            Text(
                text = uiString("outputDelivery.noWarnings", language),
                style = MaterialTheme.typography.bodySmall,
                color = CinemaTheme.extendedColors.fg3
            )
        } else {
            warnings.forEach { issue -> WarningRow(issue) }
        }
    }
}

@Composable
private fun WarningRow(issue: ValidationIssue) {
    val color = if (issue.severity == Severity.BLOCKING) MaterialTheme.colorScheme.error else CinemaTheme.extendedColors.warning
    Text(text = issue.message, color = color, style = MaterialTheme.typography.bodySmall)
}
