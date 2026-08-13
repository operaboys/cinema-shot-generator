package com.operaboys.cinemashotgenerator.ui.project

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.project.ProjectSummary
import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۱ — کارت پروژه، طبق docs/design/README.md بخش‌های ۱/۲ (Home/Projects
// project cards): Thumbnail، عنوان، چیپ وضعیت، خط Meta (شمارش صحنه/شات · زمان
// به‌روزرسانی)، منوی Overflow (تغییر نام/تکثیر/آرشیو/خروجی/حذف). عمداً بدون Blur/
// Gradient واقعی «Liquid Glass» — طبق تصمیم مستند در ADR-044، این فاز روی سیم‌کشی
// داده/عملیات واقعی تمرکز دارد، نه صیقل بصری نهایی (که یک قدم مجزای آینده است).

/**
 * رنگ/برچسب هر EntityState — طبق پیشنهاد مستندشده در F12 (docs/blueprints/16-user-workflow-v2.md:57).
 * `internal` (نه `private`) — یافته‌ی ۳ appendix ADR-081 (ADR-082) این دو تابع را
 * برای ردیف چیپ فیلتر وضعیت `ProjectsScreen.kt` هم بازاستفاده می‌کند، به‌جای
 * یک نگاشت رنگ/برچسب دوم و موازی.
 */
@Composable
internal fun stateChipLabel(state: EntityState, language: Language): String = when (state) {
    EntityState.DRAFT -> uiString("project.state.draft", language)
    EntityState.REVIEW -> uiString("project.state.review", language)
    EntityState.LOCKED -> uiString("project.state.locked", language)
    EntityState.FINAL -> uiString("project.state.final", language)
    EntityState.ARCHIVED -> uiString("project.state.archived", language)
}

@Composable
internal fun stateChipColor(state: EntityState) = when (state) {
    EntityState.DRAFT -> MaterialTheme.colorScheme.outline
    EntityState.REVIEW -> CinemaTheme.extendedColors.warning
    EntityState.LOCKED -> CinemaTheme.extendedColors.orange
    EntityState.FINAL -> CinemaTheme.extendedColors.success
    EntityState.ARCHIVED -> CinemaTheme.extendedColors.fg4
}

data class ProjectCardActions(
    val onOpen: (String) -> Unit,
    val onRename: (String) -> Unit,
    val onDuplicate: (String) -> Unit,
    val onArchive: (String) -> Unit,
    val onExport: (String) -> Unit,
    val onDeleteRequested: (String) -> Unit
)

@Composable
fun ProjectCard(
    summary: ProjectSummary,
    language: Language,
    actions: ProjectCardActions,
    showThumbnail: Boolean,
    modifier: Modifier = Modifier
) {
    val project = summary.project
    Card(
        onClick = { actions.onOpen(project.projectId) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors()
    ) {
        if (showThumbnail) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, CinemaTheme.extendedColors.orange)
                        )
                    )
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.projectName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = uiString("project.overflowMenu", language))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(uiString("project.overflow.rename", language)) },
                            onClick = { menuExpanded = false; actions.onRename(project.projectId) }
                        )
                        DropdownMenuItem(
                            text = { Text(uiString("project.overflow.duplicate", language)) },
                            onClick = { menuExpanded = false; actions.onDuplicate(project.projectId) }
                        )
                        DropdownMenuItem(
                            text = { Text(uiString("project.overflow.archive", language)) },
                            onClick = { menuExpanded = false; actions.onArchive(project.projectId) }
                        )
                        DropdownMenuItem(
                            text = { Text(uiString("project.overflow.export", language)) },
                            onClick = { menuExpanded = false; actions.onExport(project.projectId) }
                        )
                        DropdownMenuItem(
                            text = { Text(uiString("project.overflow.delete", language)) },
                            onClick = { menuExpanded = false; actions.onDeleteRequested(project.projectId) }
                        )
                    }
                }
            }

            // یافته‌ی واقعی بازبینی نهایی (واحد ۱۶ فاز ۶ قدم ۲، ADR-059): این چیپ از
            // فاز ۱ (پیش از الزام صریح Contrast که در ADR-055 مستند شد) با
            // `.copy(alpha = 0.16f)` رندر می‌شد — دقیقاً همان کلاس باگ Low-opacity
            // Tinted Fill که Implementation Notes سند طراحی صراحتاً منع کرده. رفع
            // شد: پس‌زمینه‌ی Solid + حاشیه‌ی ۲dp، هم‌الگو با CountCard در
            // ValidationScreen.kt. رنگ متن سیاه ثابت (نه رنگ خودِ State) چون هر ۵
            // رنگ این چیپ (outline/warning/orange/success/fg4) به‌اندازه‌ی کافی
            // روشن‌اند — دقیقاً همان استدلال مستندشده‌ی ADR-055 برای کارت WARNING.
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = stateChipColor(project.state),
                border = BorderStroke(2.dp, stateChipColor(project.state)),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(
                        text = stateChipLabel(project.state, language),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black
                    )
                }
            }

            Text(
                text = uiTemplate(
                    "project.metaTemplate",
                    language,
                    "scenes" to summary.sceneCount.toString(),
                    "shots" to summary.shotCount.toString()
                ) + " · " + project.lastModified.substringBefore('T'),
                style = MaterialTheme.typography.labelSmall,
                color = CinemaTheme.extendedColors.fg3,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
