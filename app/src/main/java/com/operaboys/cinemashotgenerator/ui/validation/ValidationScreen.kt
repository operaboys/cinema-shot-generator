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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.operaboys.cinemashotgenerator.domain.validation.AggregatedValidationReport
import com.operaboys.cinemashotgenerator.domain.validation.LeveledValidationIssue
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationLevel
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

fun validationLevelSectionTag(level: ValidationLevel): String = "validation.levelSection.${level.name}"
fun validationIssueCardTag(level: ValidationLevel, index: Int): String = "validation.issueCard.${level.name}.$index"

@Composable
fun ValidationScreen(
    projectId: String,
    sceneId: String,
    shotId: String,
    language: Language,
    onBack: () -> Unit,
    shotRepository: ShotRepository? = null,
    sceneRepository: SceneRepository? = null,
    projectDnaRepository: ProjectDnaRepository? = null,
    assetRepository: AssetRepository? = null,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ValidationViewModel = viewModel(
        factory = ValidationViewModel.factory(
            application, projectId, sceneId, shotId,
            shotRepository, sceneRepository, projectDnaRepository, assetRepository
        )
    )
    val report by viewModel.report.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth()) {
        AssetFormHeader(
            title = uiString("validation.title", language),
            subtitle = uiString("validation.subtitle", language),
            onBack = onBack,
            backTestTag = VALIDATION_BACK_BUTTON_TAG
        )

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

            ValidationLevelSection(ValidationLevel.DATA_COMPLETENESS, "validation.level1Title", report, language)
            ValidationLevelSection(ValidationLevel.LOGICAL_CONSISTENCY, "validation.level2Title", report, language)
            ValidationLevelSection(ValidationLevel.CONTINUITY_AND_DEPENDENCY, "validation.level3Title", report, language)
        }
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
    language: Language
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
                    IssueCard(leveled, language, testTag = validationIssueCardTag(level, index))
                }
            }
        }
    }
}

@Composable
private fun IssueCard(leveled: LeveledValidationIssue, language: Language, testTag: String) {
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
            Text(text = issue.message, style = MaterialTheme.typography.bodyMedium)
            issue.suggestion?.let {
                Text(
                    text = "${uiString("validation.suggestionPrefix", language)} $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = CinemaTheme.extendedColors.fg3
                )
            }
        }
    }
}
