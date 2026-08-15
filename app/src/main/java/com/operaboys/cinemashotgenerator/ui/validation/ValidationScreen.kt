package com.operaboys.cinemashotgenerator.ui.validation

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.story.HumanOverride
import com.operaboys.cinemashotgenerator.domain.story.OverrideType
import com.operaboys.cinemashotgenerator.domain.validation.AggregatedValidationReport
import com.operaboys.cinemashotgenerator.domain.validation.LeveledValidationIssue
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.domain.validation.ValidationLevel
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormEnumDropdownField
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormFlatEntries
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormHeader
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۵ — قدم ۱: صفحه‌ی Validation. طبق docs/design/README.md بخش «۹.
// Validation»: Header + دو کارت شمارش کاملاً Opaque/حاشیه‌دار (BLOCKING/WARNING) +
// سه بخش سطح. الزام صریح Contrast سند طراحی («باید مثل یک هشدار فوری و کاملاً مات
// خوانده شود») اینجا با پس‌زمینه‌ی کاملاً Solid (نه tint کم‌رنگ مثل SoftLockBanner)
// رعایت شده — دقیقاً همان کلاس باگ Contrast که در Implementation Notes سند طراحی
// مستند است. جزئیات کامل تصمیمات در
// docs/adr/055-unit16-phase5-step1-validation-screen.md.

const val VALIDATION_BACK_BUTTON_TAG = "validation.backButton"
const val VALIDATION_BLOCKING_COUNT_CARD_TAG = "validation.blockingCountCard"
const val VALIDATION_WARNING_COUNT_CARD_TAG = "validation.warningCountCard"
const val VALIDATION_OUTPUT_DELIVERY_BUTTON_TAG = "validation.outputDeliveryButton"
const val VALIDATION_TOGGLE_LANGUAGE_BUTTON_TAG = "validation.toggleLanguageButton"
const val VALIDATION_TOGGLE_THEME_BUTTON_TAG = "validation.toggleThemeButton"

fun validationLevelSectionTag(level: ValidationLevel): String = "validation.levelSection.${level.name}"
fun validationIssueCardTag(level: ValidationLevel, index: Int): String = "validation.issueCard.${level.name}.$index"

@Composable
fun ValidationScreen(
    projectId: String,
    sceneId: String,
    shotId: String,
    language: Language,
    theme: AppTheme,
    onBack: () -> Unit,
    onToggleLanguage: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onNavigateToOutputDelivery: (shotId: String) -> Unit = {},
    shotRepository: ShotRepository? = null,
    sceneRepository: SceneRepository? = null,
    projectDnaRepository: ProjectDnaRepository? = null,
    assetRepository: AssetRepository? = null,
    database: AppDatabase? = null,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ValidationViewModel = viewModel(
        factory = ValidationViewModel.factory(
            application, projectId, sceneId, shotId,
            shotRepository, sceneRepository, projectDnaRepository, assetRepository, database
        )
    )
    val report by viewModel.report.collectAsStateWithLifecycle()
    // یافته‌ی واقعی دیباگ این قدم: viewModel::activeOverrideFor مستقیماً
    // _activeOverrides.value (یک MutableStateFlow خام) را می‌خواند — بدون
    // collectAsStateWithLifecycle، این خواندن هیچ Compose State ای را Observe
    // نمی‌کند، پس تغییر واقعی activeOverrides (بعد از createOverrideForIssue) هرگز
    // باعث Recomposition نمی‌شد و بج «Override شده» تا ابد ظاهر نمی‌شد — دقیقاً
    // همان علت واقعی Timeout مشاهده‌شده در تست End-to-End این قدم. overrideLookup
    // اکنون مستقیماً از همین State جمع‌آوری‌شده می‌خواند (نه از ViewModel)، دقیقاً
    // با همان منطق issueKey ViewModel (field ?: message) که private است.
    val activeOverrides by viewModel.activeOverrides.collectAsStateWithLifecycle()
    val overrideLookup: (ValidationIssue) -> HumanOverride? = { issue ->
        activeOverrides.firstOrNull { it.scope.field == (issue.field ?: issue.message) }
    }

    // رفع یافته‌ی معماری «Human Override هرگز به UI وصل نشده» (G3/ADR-067، ADR-068)
    // — نقطه‌ی ورود UI. دو Dialog State مستقل (نه یک enum مشترک) چون داده‌ی
    // پرسیده‌شده کاملاً متفاوت است (ValidationIssue برای Override، HumanOverride
    // موجود برای Revoke) و هم‌زمان بیش از یکی باز نمی‌شود.
    var overrideDialogIssue by remember { mutableStateOf<ValidationIssue?>(null) }
    var revokeDialogOverride by remember { mutableStateOf<HumanOverride?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        AssetFormHeader(
            title = uiString("validation.title", language),
            subtitle = uiString("validation.subtitle", language),
            onBack = onBack,
            backTestTag = VALIDATION_BACK_BUTTON_TAG,
            language = language,
            theme = theme,
            onToggleLanguage = onToggleLanguage,
            onToggleTheme = onToggleTheme,
            toggleLanguageTestTag = VALIDATION_TOGGLE_LANGUAGE_BUTTON_TAG,
            toggleThemeTestTag = VALIDATION_TOGGLE_THEME_BUTTON_TAG
        )

        // ورودی مستقیم به Output Delivery — طبق دستور کار قدم ۳ فاز ۵ («نقطه‌ی ورود
        // می‌تواند هم از Validation و هم مستقیم از Shot Composer باشد»)، هم‌الگو با
        // دکمه‌ی مشابه در ShotComposerScreen. برخلاف آن صفحه، اینجا هیچ شرط
        // shotId != null لازم نیست — Validation از قبل فقط برای یک شات ذخیره‌شده باز
        // می‌شود (shotId این پارامتر غیر-nullable است).
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            TextButton(
                onClick = { onNavigateToOutputDelivery(shotId) },
                modifier = Modifier.testTag(VALIDATION_OUTPUT_DELIVERY_BUTTON_TAG)
            ) {
                Icon(Icons.Filled.MovieFilter, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(uiString("outputDelivery.entryButtonLabel", language))
            }
        }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCountRow(
                blockingCount = report?.blockingCount ?: 0,
                warningCount = report?.warningCount ?: 0,
                language = language
            )

            if (report != null && report!!.blockingCount == 0 && report!!.warningCount == 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = CinemaTheme.extendedColors.success)
                    Text(
                        text = uiString("validation.allClearMessage", language),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            ValidationLevelSection(
                ValidationLevel.DATA_COMPLETENESS, "validation.level1Title", report, language,
                overrideLookup = overrideLookup,
                onOverrideClick = { overrideDialogIssue = it },
                onRevokeClick = { revokeDialogOverride = it }
            )
            ValidationLevelSection(
                ValidationLevel.LOGICAL_CONSISTENCY, "validation.level2Title", report, language,
                overrideLookup = overrideLookup,
                onOverrideClick = { overrideDialogIssue = it },
                onRevokeClick = { revokeDialogOverride = it }
            )
            ValidationLevelSection(
                ValidationLevel.CONTINUITY_AND_DEPENDENCY, "validation.level3Title", report, language,
                overrideLookup = overrideLookup,
                onOverrideClick = { overrideDialogIssue = it },
                onRevokeClick = { revokeDialogOverride = it }
            )
        }
    }

    overrideDialogIssue?.let { issue ->
        OverrideWarningDialog(
            language = language,
            onConfirm = { overrideType, reason ->
                viewModel.createOverrideForIssue(issue, overrideType, reason)
                overrideDialogIssue = null
            },
            onDismiss = { overrideDialogIssue = null }
        )
    }

    revokeDialogOverride?.let { override ->
        RevokeOverrideDialog(
            language = language,
            onConfirm = { reason ->
                viewModel.revokeOverrideAction(override, reason)
                revokeDialogOverride = null
            },
            onDismiss = { revokeDialogOverride = null }
        )
    }
}

/**
 * دو کارت شمارش Solid/بدون هیچ Alpha — طبق محدودیت صریح Implementation Notes سند
 * طراحی («never render... as a low-opacity tinted fill... Final spec: solid/near-
 * opaque fills + a visible 2dp border»). هیچ توکن «onWarning» در تم موجود نیست
 * (فقط onError برای BLOCKING تعریف شده)؛ چون زرد Warning (0xFFFFB648) به‌اندازه‌ی
 * کافی روشن است، از سیاه ثابت برای متن روی آن استفاده شد — ساخت یک توکن تم تازه
 * فقط برای این یک کارت خارج از scope این قدم بود.
 */
@Composable
private fun SummaryCountRow(blockingCount: Int, warningCount: Int, language: Language) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CountCard(
            count = blockingCount,
            label = uiString("validation.blockingLabel", language),
            backgroundColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            testTag = VALIDATION_BLOCKING_COUNT_CARD_TAG,
            modifier = Modifier.weight(1f)
        )
        CountCard(
            count = warningCount,
            label = uiString("validation.warningLabel", language),
            backgroundColor = CinemaTheme.extendedColors.warning,
            contentColor = Color.Black,
            testTag = VALIDATION_WARNING_COUNT_CARD_TAG,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CountCard(
    count: Int,
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(2.dp, backgroundColor),
        shadowElevation = 4.dp,
        // mergeDescendants=true لازم است — بدون آن، متن دو Text فرزند (شمار + برچسب)
        // در semantics خودِ این گره (testTag) ادغام نمی‌شود؛ برخلاف OpaqueChip (که
        // overload کلیک‌پذیر Surface را دارد و Material3 خودکار merge می‌کند)، این
        // Surface کاملاً غیرفعال است — یافته‌ی واقعی دیباگ این قدم.
        modifier = modifier.testTag(testTag).semantics(mergeDescendants = true) {}
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = count.toString(), style = MaterialTheme.typography.headlineMedium, color = contentColor)
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = contentColor)
        }
    }
}

@Composable
private fun ValidationLevelSection(
    level: ValidationLevel,
    titleKey: String,
    report: AggregatedValidationReport?,
    language: Language,
    overrideLookup: (ValidationIssue) -> HumanOverride?,
    onOverrideClick: (ValidationIssue) -> Unit,
    onRevokeClick: (HumanOverride) -> Unit
) {
    val issues = report?.issuesAtLevel(level) ?: emptyList()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag(validationLevelSectionTag(level))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = uiString(titleKey, language), style = MaterialTheme.typography.titleMedium)
            if (issues.isEmpty()) {
                Text(
                    text = uiString("validation.levelEmptyState", language),
                    style = MaterialTheme.typography.bodySmall,
                    color = CinemaTheme.extendedColors.fg3
                )
            } else {
                issues.forEachIndexed { index, leveled ->
                    IssueCard(
                        leveled, language, testTag = validationIssueCardTag(level, index),
                        activeOverride = overrideLookup(leveled.issue),
                        onOverrideClick = { onOverrideClick(leveled.issue) },
                        onRevokeClick = onRevokeClick
                    )
                }
            }
        }
    }
}

@Composable
private fun IssueCard(
    leveled: LeveledValidationIssue,
    language: Language,
    testTag: String,
    activeOverride: HumanOverride?,
    onOverrideClick: () -> Unit,
    onRevokeClick: (HumanOverride) -> Unit
) {
    val issue = leveled.issue
    val color = if (issue.severity == Severity.BLOCKING) MaterialTheme.colorScheme.error else CinemaTheme.extendedColors.warning
    val icon = if (issue.severity == Severity.BLOCKING) Icons.Filled.Error else Icons.Filled.Warning
    val severityLabel = if (issue.severity == Severity.BLOCKING) {
        uiString("validation.blockingLabel", language)
    } else {
        uiString("validation.warningLabel", language)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag(testTag)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = color)
                Text(text = severityLabel, style = MaterialTheme.typography.labelMedium, color = color)
                issue.field?.let {
                    Text(text = it, style = MaterialTheme.typography.labelMedium, color = CinemaTheme.extendedColors.fg3)
                }
            }
            Text(
                text = issue.message,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (activeOverride != null) TextDecoration.LineThrough else null
            )
            issue.suggestion?.let {
                Text(
                    text = "${uiString("validation.suggestionPrefix", language)} $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = CinemaTheme.extendedColors.fg3
                )
            }
            // دکمه‌ی Override/Revoke — طبق Rule 1 دامنه (checkOverridePermission)،
            // فقط WARNING قابل Override است؛ خودِ عبارت شرط پایین (نه یک صدازدن
            // جدا به viewModel.canOverride) کافی است چون این شرط دقیقاً همان Rule 1
            // را تکرار می‌کند (issue.severity == Severity.WARNING) — createOverrideForIssue
            // خودش هم دوباره این Rule را مستقل از UI اجرا می‌کند (دفاع دوم).
            if (activeOverride != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = uiString("validation.overriddenBadge", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = CinemaTheme.extendedColors.fg3,
                        modifier = Modifier.testTag("$testTag.overriddenBadge")
                    )
                    TextButton(onClick = { onRevokeClick(activeOverride) }, modifier = Modifier.testTag("$testTag.revokeButton")) {
                        Text(uiString("validation.revokeButton", language))
                    }
                }
            } else if (issue.severity == Severity.WARNING) {
                TextButton(onClick = onOverrideClick, modifier = Modifier.testTag("$testTag.overrideButton")) {
                    Text(uiString("validation.overrideButton", language))
                }
            }
        }
    }
}

/**
 * تصمیم مستقل — گفتگوی Override: طبق دستور کار («Dialog کوچک: reason (اختیاری)
 * و overrideType»)، فقط همین دو فیلد. AssetFormEnumDropdownField/AssetFormFlatEntries
 * از ui/assets/AssetFormSupport.kt بازاستفاده شدند — دقیقاً همان الگوی
 * SceneDetailScreen.kt (SceneSettingsDialog).
 */
@Composable
private fun OverrideWarningDialog(
    language: Language,
    onConfirm: (OverrideType, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var overrideType by remember { mutableStateOf(OverrideType.ARTISTIC) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiString("validation.overrideDialogTitle", language)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AssetFormEnumDropdownField(
                    label = uiString("validation.overrideTypeLabel", language),
                    selectedLabel = overrideTypeLabel(overrideType, language),
                    testTag = "validation.overrideDialog.typeField"
                ) { onDismissMenu ->
                    AssetFormFlatEntries(OverrideType.entries, { overrideTypeLabel(it, language) }) {
                        overrideType = it
                        onDismissMenu()
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(uiString("validation.overrideReasonLabel", language)) },
                    modifier = Modifier.fillMaxWidth().testTag("validation.overrideDialog.reasonField")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(overrideType, reason.ifBlank { null }) },
                modifier = Modifier.testTag("validation.overrideDialog.confirmButton")
            ) {
                Text(uiString("validation.overrideConfirmButton", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("validation.overrideDialog.cancelButton")) {
                Text(uiString("validation.overrideCancelButton", language))
            }
        }
    )
}

/**
 * تصمیم مستقل — گفتگوی Revoke: طبق دستور کار («حداقل ساده»)، فقط یک reason
 * اختیاری، بدون تکرار overrideType (که در خودِ HumanOverride موجود از قبل ثابت
 * است). Cancel این گفتگو از همان کلید validation.overrideCancelButton استفاده
 * می‌کند — یک دکمه‌ی «انصراف» عمومی، نه مفهومی مختص Override.
 */
@Composable
private fun RevokeOverrideDialog(
    language: Language,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiString("validation.revokeDialogTitle", language)) },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text(uiString("validation.revokeReasonLabel", language)) },
                modifier = Modifier.fillMaxWidth().testTag("validation.revokeDialog.reasonField")
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(reason.ifBlank { null }) },
                modifier = Modifier.testTag("validation.revokeDialog.confirmButton")
            ) {
                Text(uiString("validation.revokeConfirmButton", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("validation.revokeDialog.cancelButton")) {
                Text(uiString("validation.overrideCancelButton", language))
            }
        }
    )
}

private fun overrideTypeLabel(type: OverrideType, language: Language): String = when (type) {
    OverrideType.ARTISTIC -> uiString("validation.overrideType.artistic", language)
    OverrideType.NARRATIVE -> uiString("validation.overrideType.narrative", language)
    OverrideType.VISUAL -> uiString("validation.overrideType.visual", language)
    OverrideType.TECHNICAL -> uiString("validation.overrideType.technical", language)
}
