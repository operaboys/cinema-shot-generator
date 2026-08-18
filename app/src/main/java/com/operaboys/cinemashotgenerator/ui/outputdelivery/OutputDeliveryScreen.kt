package com.operaboys.cinemashotgenerator.ui.outputdelivery

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.operaboys.cinemashotgenerator.domain.storybreakdown.AiConnectorProfile
import com.operaboys.cinemashotgenerator.domain.storybreakdown.BUILTIN_AI_CONNECTOR_PROFILES
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import com.operaboys.cinemashotgenerator.domain.workflow.QualityScore
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
const val OUTPUT_DELIVERY_TOGGLE_LANGUAGE_BUTTON_TAG = "outputDelivery.toggleLanguageButton"
const val OUTPUT_DELIVERY_TOGGLE_THEME_BUTTON_TAG = "outputDelivery.toggleThemeButton"
/** هوشمندسازی و اتصال evaluatePromptQuality — قدم ۲ از ۳ زیرقدم (ADR-119). */
const val OUTPUT_DELIVERY_QUALITY_CARD_TAG = "outputDelivery.qualityCard"
/** هوشمندسازی و اتصال evaluatePromptQuality — قدم ۳ از ۳ زیرقدم (آخرین زیرقدم، ADR-120). */
const val OUTPUT_DELIVERY_ANALYZE_WITH_AI_BUTTON_TAG = "outputDelivery.analyzeWithAiButton"
const val OUTPUT_DELIVERY_AI_ANALYSIS_RESULT_TAG = "outputDelivery.aiAnalysisResult"
const val OUTPUT_DELIVERY_AI_ANALYSIS_ERROR_TAG = "outputDelivery.aiAnalysisError"

fun outputDeliveryAiConnectorProfileChipTag(profileId: String): String = "outputDelivery.aiConnectorProfileChip.$profileId"

fun outputDeliveryModelChipTag(profileId: String): String = "outputDelivery.modelChip.$profileId"

@Composable
fun OutputDeliveryScreen(
    shotId: String,
    language: Language,
    theme: AppTheme,
    onBack: () -> Unit,
    initialModelProfileId: String?,
    onToggleLanguage: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
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
    // هوشمندسازی و اتصال evaluatePromptQuality — قدم ۳ از ۳ زیرقدم (ADR-120).
    val selectedAiConnectorProfileId by viewModel.selectedAiConnectorProfileId.collectAsStateWithLifecycle()
    val apiKeySavedForQualityAnalysis by viewModel.apiKeySavedForQualityAnalysis.collectAsStateWithLifecycle()
    val qualityAnalysisInProgress by viewModel.qualityAnalysisInProgress.collectAsStateWithLifecycle()
    val qualityAnalysisResult by viewModel.qualityAnalysisResult.collectAsStateWithLifecycle()
    val qualityAnalysisError by viewModel.qualityAnalysisError.collectAsStateWithLifecycle()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // رفع یافته‌ی معماری «دکمه‌ی Export مستعار Copy است» (G4/G18، ADR-069): وقتی
    // ViewModel واقعاً فایل‌ها را روی دیسک نوشت (exportOutput→exportedFiles)، این
    // Effect یک Intent.ACTION_SEND واقعی می‌سازد و Chooser سیستم را باز می‌کند —
    // این کار فقط از لایه‌ی UI (Context یک Activity واقعی) ممکن است، نه ViewModel؛
    // بعد از باز کردن، clearExportedFiles صدا زده می‌شود تا این یک رویداد یک‌باره
    // بماند (هم‌الگو با lastActionMessage/clearLastActionMessage ProjectListViewModel).
    val exportedFiles by viewModel.exportedFiles.collectAsStateWithLifecycle()
    LaunchedEffect(exportedFiles) {
        val files = exportedFiles ?: return@LaunchedEffect
        // همه‌ی ExportFile های composeOutput همیشه "text/plain" هستند
        // (OutputComposer.kt) — نیازی به خواندن دوباره‌ی mimeType از state نیست.
        val shareIntent = buildExportShareIntent(context, files, "text/plain")
        context.startActivity(Intent.createChooser(shareIntent, uiString("outputDelivery.exportChooserTitle", language)))
        viewModel.clearExportedFiles()
    }

    Column(modifier = modifier) {
        AssetFormHeader(
            title = uiString("drawer.outputDelivery", language),
            subtitle = uiString("outputDelivery.subtitle", language),
            onBack = onBack,
            backTestTag = OUTPUT_DELIVERY_BACK_BUTTON_TAG,
            language = language,
            theme = theme,
            onToggleLanguage = onToggleLanguage,
            onToggleTheme = onToggleTheme,
            toggleLanguageTestTag = OUTPUT_DELIVERY_TOGGLE_LANGUAGE_BUTTON_TAG,
            toggleThemeTestTag = OUTPUT_DELIVERY_TOGGLE_THEME_BUTTON_TAG
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
                        // تصمیم مستقل — Export دیگر مستعار Copy نیست (ADR-069):
                        // دکمه‌ی Copy فقط Clipboard را عوض می‌کند (رفتار بدون تغییر)؛
                        // دکمه‌ی Export اکنون رفتار واقعاً متفاوتی دارد — نوشتن فایل +
                        // اشتراک‌گذاری واقعی با Intent.ACTION_SEND (بالا). هیچ پیام
                        // Clipboard دیگر اینجا نشان داده نمی‌شود — خودِ Chooser سیستم
                        // بازخورد کافی است.
                        onExport = { viewModel.exportOutput() }
                    )
                    // ترتیب عمدی: بلافاصله بعد از خودِ کارت پیش‌نمایش (متن واقعی پرامپت)
                    // و پیش از بخش هشدارها — این کارت مستقیماً درباره‌ی همان متنی است که
                    // در OutputPreviewCard دیده شد (نه یک بخش مستقل جدا)، پس منطقی است
                    // بلافاصله زیرش بیاید؛ هشدارها (که می‌توانند به دلایل کاملاً متفاوتی
                    // مثل تضاد Validation باشند، نه فقط کیفیت نوشتاری) بعد از آن.
                    PromptQualityCard(score = currentState.qualityScore, language = language)
                    // بلافاصله زیر کارت شاخص کیفیت — این بخش دقیقاً درباره‌ی تحلیل
                    // عمیق‌تر همان امتیاز است، نه یک ابزار مستقل جدا.
                    AiQualityAnalysisCard(
                        language = language,
                        selectedProfileId = selectedAiConnectorProfileId,
                        onSelectProfile = { viewModel.selectAiConnectorProfileForQuality(it) },
                        apiKeySaved = apiKeySavedForQualityAnalysis,
                        inProgress = qualityAnalysisInProgress,
                        result = qualityAnalysisResult,
                        error = qualityAnalysisError,
                        onAnalyze = { viewModel.analyzePromptQualityWithAi() },
                        onDismissError = { viewModel.clearQualityAnalysisError() }
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

/**
 * هوشمندسازی و اتصال evaluatePromptQuality — قدم ۲ از ۳ زیرقدم (ADR-119):
 * کارت کاملاً اطلاعاتی/غیرمسدودکننده — هیچ رنگ خطا/Blocking برای امتیاز پایین
 * نمایش داده نمی‌شود (هم‌راستا با کامنت خودِ evaluatePromptQuality در
 * WorkflowModels.kt: «کمک به کاربر، نه بلاک‌کردن جریان کار»). هم‌الگو با
 * OutputPreviewCard (همان Card/رنگ‌بندی CinemaTheme).
 */
@Composable
private fun PromptQualityCard(score: QualityScore, language: Language) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag(OUTPUT_DELIVERY_QUALITY_CARD_TAG)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = uiString("outputDelivery.qualityCardTitle", language), style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = uiString("outputDelivery.qualityScoreTotalLabel", language), style = MaterialTheme.typography.bodyMedium)
                Text(text = "${score.total}/100", style = MaterialTheme.typography.headlineSmall)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                QualityAxisRow(uiString("outputDelivery.qualitySubjectClarityLabel", language), score.subjectClarity)
                QualityAxisRow(uiString("outputDelivery.qualityCinematicClarityLabel", language), score.cinematicClarity)
                QualityAxisRow(uiString("outputDelivery.qualityVisualSpecificityLabel", language), score.visualSpecificity)
                QualityAxisRow(uiString("outputDelivery.qualityStyleCoherenceLabel", language), score.styleCoherence)
                QualityAxisRow(uiString("outputDelivery.qualityConcisenessLabel", language), score.conciseness)
            }
        }
    }
}

@Composable
private fun QualityAxisRow(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = CinemaTheme.extendedColors.fg3)
        Text(text = "$value/20", style = MaterialTheme.typography.bodySmall, color = CinemaTheme.extendedColors.fg3)
    }
}

/**
 * هوشمندسازی و اتصال evaluatePromptQuality — قدم ۳ از ۳ زیرقدم (آخرین
 * زیرقدم، ADR-120): «تحلیل عمیق‌تر با AI» — عیناً همان الگوی اثبات‌شده‌ی G2
 * (GeneratedPromptCard، AiStoryBreakdownScreen.kt) برای این صفحه‌ی دیگر
 * تکرار شده: چیپ‌های انتخاب پروفایل (اگر بیش از یک پروفایل باشد)، دکمه با
 * قفل امنیتی پیش‌فعال (enabled فقط وقتی apiKeySaved=true — نه فقط واکنش به
 * خطای بعد از کلیک)، CircularProgressIndicator هنگام بارگذاری، متن راهنما
 * وقتی کلید ذخیره نشده، و نمایش نتیجه/خطا. Composable مستقل (نه داخل
 * PromptQualityCard) چون این یک اقدام کاملاً جدا (فراخوانی واقعی HTTP) است،
 * نه بخشی از خودِ محاسبه‌ی محلی امتیاز — جداسازی این دو، خوانایی هرکدام را
 * بالا نگه می‌دارد.
 */
@Composable
private fun AiQualityAnalysisCard(
    language: Language,
    selectedProfileId: String,
    onSelectProfile: (String) -> Unit,
    apiKeySaved: Boolean,
    inProgress: Boolean,
    result: String?,
    error: String?,
    onAnalyze: () -> Unit,
    onDismissError: () -> Unit
) {
    val selectedProfile: AiConnectorProfile? = BUILTIN_AI_CONNECTOR_PROFILES.firstOrNull { it.profileId == selectedProfileId }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (BUILTIN_AI_CONNECTOR_PROFILES.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BUILTIN_AI_CONNECTOR_PROFILES.forEach { profile ->
                        OpaqueChip(
                            label = profile.displayName,
                            selected = profile.profileId == selectedProfileId,
                            onClick = { onSelectProfile(profile.profileId) },
                            testTag = outputDeliveryAiConnectorProfileChipTag(profile.profileId)
                        )
                    }
                }
            }

            Button(
                onClick = onAnalyze,
                enabled = apiKeySaved && !inProgress,
                modifier = Modifier.fillMaxWidth().testTag(OUTPUT_DELIVERY_ANALYZE_WITH_AI_BUTTON_TAG)
            ) {
                if (inProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(uiString("outputDelivery.analyzeWithAiButton", language))
            }
            if (!apiKeySaved) {
                Text(
                    text = uiString("outputDelivery.analyzeWithAiNoKeyHint", language),
                    style = MaterialTheme.typography.labelSmall,
                    color = CinemaTheme.extendedColors.fg3
                )
            }

            if (result != null) {
                Text(
                    text = result,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag(OUTPUT_DELIVERY_AI_ANALYSIS_RESULT_TAG)
                )
            }

            if (error != null) {
                Card(modifier = Modifier.fillMaxWidth().testTag(OUTPUT_DELIVERY_AI_ANALYSIS_ERROR_TAG)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissError) {
                            Text(uiString("aiBreakdown.dismissButton", language))
                        }
                    }
                }
            }

            val selectedName = selectedProfile?.displayName
            if (selectedName != null) {
                // یادآوری ظریف اینکه کدام سرویس واقعاً استفاده می‌شود — عمداً یک
                // برچسب جدا (نه داخل خودِ متن دکمه، طبق تصمیم این قدم: دکمه ثابت
                // «تحلیل عمیق‌تر با AI» می‌ماند، نه یک قالب هرباره‌ی متفاوت مثل G2).
                Text(
                    text = selectedName,
                    style = MaterialTheme.typography.labelSmall,
                    color = CinemaTheme.extendedColors.fg3
                )
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
