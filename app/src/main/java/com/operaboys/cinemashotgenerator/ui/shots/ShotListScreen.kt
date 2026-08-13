package com.operaboys.cinemashotgenerator.ui.shots

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.workflow.ShotListViewMode
import com.operaboys.cinemashotgenerator.ui.assets.OpaqueChip
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۴ — قدم ۲ — بخش الف: محتوای واقعی Tab «شات‌ها» صفحه‌ی Scene Detail،
// جایگزین Container خالی قدم قبل. طبق docs/design/README.md بخش «۶. Shots List».
// جزئیات کامل تصمیمات در docs/adr/051-unit16-phase4-step2-shot-list-composer-skeleton.md.

const val SHOTS_LIST_NEW_SHOT_FAB_TAG = "shotsList.newShotFab"
const val SHOTS_LIST_GRID_TOGGLE_TAG = "shotsList.gridToggle"
const val SHOTS_LIST_TIMELINE_TOGGLE_TAG = "shotsList.timelineToggle"
const val SHOTS_LIST_DELETE_CONFIRM_BUTTON_TAG = "shotsList.deleteConfirmButton"

fun shotCardTag(shotId: String): String = "shotsList.card.$shotId"
fun shotCardMenuButtonTag(shotId: String): String = "shotsList.card.$shotId.menuButton"
fun shotCardDeleteMenuItemTag(shotId: String): String = "shotsList.card.$shotId.deleteMenuItem"

/**
 * یافته‌ی ۴ appendix ADR-081 (ADR-082): testTag نقطه‌ی ریل Timeline — تنها راه
 * تست End-to-End برای اثبات این‌که حالت Timeline واقعاً از Grid متفاوت است
 * (قبلاً هر دو حالت دقیقاً همان `ShotCard` را بدون هیچ تزیین اضافه رندر می‌کردند).
 */
fun shotTimelineDotTag(shotId: String): String = "shotsList.timeline.$shotId.dot"

@Composable
fun ShotListScreen(
    sceneId: String,
    sceneNumber: Int,
    viewMode: ShotListViewMode,
    language: Language,
    onViewModeChange: (ShotListViewMode) -> Unit,
    onOpenShot: (String) -> Unit,
    onAddShot: () -> Unit,
    shotRepository: ShotRepository? = null,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ShotListViewModel = viewModel(
        factory = ShotListViewModel.factory(application, sceneId, shotRepository)
    )
    val shots by viewModel.shots.collectAsStateWithLifecycle()
    // رفع G22 ممیزی post-Unit16 (docs/adr/062-...): هم‌الگو با archiveTarget/
    // restoreTarget موجود (ProjectListSection.kt/BackupsScreen.kt) — کلیک روی
    // «حذف» فقط هدف را ست می‌کند، دیالوگ تأیید واقعی خودِ حذف را انجام می‌دهد.
    var deleteTarget by remember { mutableStateOf<Shot?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OpaqueChip(
                    label = uiString("shotsList.gridView", language),
                    selected = viewMode == ShotListViewMode.GRID,
                    onClick = { onViewModeChange(ShotListViewMode.GRID) },
                    testTag = SHOTS_LIST_GRID_TOGGLE_TAG
                )
                OpaqueChip(
                    label = uiString("shotsList.timelineView", language),
                    selected = viewMode == ShotListViewMode.TIMELINE,
                    onClick = { onViewModeChange(ShotListViewMode.TIMELINE) },
                    testTag = SHOTS_LIST_TIMELINE_TOGGLE_TAG
                )
            }

            if (shots.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiString("shotsList.emptyState", language),
                        style = MaterialTheme.typography.bodyLarge,
                        color = CinemaTheme.extendedColors.fg3
                    )
                }
            } else if (viewMode == ShotListViewMode.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(shots, key = { it.shotId }) { shot ->
                        ShotCard(
                            shot = shot,
                            sceneNumber = sceneNumber,
                            language = language,
                            onClick = { onOpenShot(shot.shotId) },
                            onDeleteClick = { deleteTarget = shot }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(shots, key = { it.shotId }) { shot ->
                        ShotTimelineRow(
                            shot = shot,
                            sceneNumber = sceneNumber,
                            language = language,
                            onClick = { onOpenShot(shot.shotId) },
                            onDeleteClick = { deleteTarget = shot }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddShot,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag(SHOTS_LIST_NEW_SHOT_FAB_TAG)
        ) {
            Icon(Icons.Filled.Add, contentDescription = uiString("shotsList.newShot", language))
        }
    }

    val target = deleteTarget
    if (target != null) {
        DeleteShotDialog(
            language = language,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.deleteShot(target.shotId)
                deleteTarget = null
            }
        )
    }
}

/**
 * رفع G22 ممیزی post-Unit16: هم‌الگو دقیق با DeleteSceneDialog
 * (SceneDetailScreen.kt)/DeleteBackupDialog (BackupsScreen.kt). برخلاف Scene،
 * هیچ Rule دامنه‌ای حذف Shot را مسدود نمی‌کند — پس فقط تأیید ساده، بدون شاخه‌ی
 * Blocked.
 */
@Composable
private fun DeleteShotDialog(language: Language, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiString("shotsList.delete.title", language)) },
        text = { Text(uiString("shotsList.delete.message", language)) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(SHOTS_LIST_DELETE_CONFIRM_BUTTON_TAG)) {
                Text(uiString("shotsList.delete.confirm", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(uiString("shotsList.delete.cancel", language))
            }
        }
    )
}

/**
 * نشانگر کوچک Override (طبق دستور کار بخش الف): اگر camera/lighting/environment
 * هرکدام source=="override" باشند، یعنی این شات تنظیمات صحنه را Override کرده.
 */
private fun Shot.hasOverriddenSettings(): Boolean =
    camera.source == "override" || lighting.source == "override" || environment.source == "override"

/**
 * یافته‌ی ۴ appendix ADR-081 (ADR-082): طراحی واقعی Timeline mockup — ریل
 * عمودی (نقطه‌ی ۱۲dp بنفش + خط اتصال ۲dp تا شات بعدی)، دقیقاً طبق mockup
 * (docs/design/Cinema Studio.html، `vm.timeline`). طبق دستور کار این قدم،
 * محتوای خودِ کارت همان `ShotCard` مشترک با Grid است — فقط ریل اطراف آن
 * تازه اضافه شد.
 */
@Composable
private fun ShotTimelineRow(shot: Shot, sceneNumber: Int, language: Language, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.width(32.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(12.dp)
                    .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                    .testTag(shotTimelineDotTag(shot.shotId))
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(2.dp)
                    .background(color = CinemaTheme.extendedColors.hairline)
            )
        }
        ShotCard(
            shot = shot,
            sceneNumber = sceneNumber,
            language = language,
            onClick = onClick,
            onDeleteClick = onDeleteClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ShotCard(
    shot: Shot,
    sceneNumber: Int,
    language: Language,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(onClick = onClick, modifier = modifier.fillMaxWidth().testTag(shotCardTag(shot.shotId))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = shotCode(sceneNumber, shot.shotNumber), style = MaterialTheme.typography.labelMedium, color = CinemaTheme.extendedColors.fg3)
                if (shot.hasOverriddenSettings()) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = uiString("shotCard.overrideIndicator", language),
                        tint = CinemaTheme.extendedColors.orange,
                        modifier = Modifier.testTag("shotsList.card.${shot.shotId}.overrideIndicator")
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.testTag(shotCardMenuButtonTag(shot.shotId))) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(uiString("shotsList.deleteMenuItem", language)) },
                            onClick = { menuExpanded = false; onDeleteClick() },
                            modifier = Modifier.testTag(shotCardDeleteMenuItemTag(shot.shotId))
                        )
                    }
                }
            }
            Text(
                text = shotDisplayTitle(shot.shotTitle, shot.shotDescription),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OpaqueChip(label = shotTypeLabel(shot.shotType, language), selected = false, onClick = null)
            }
            Text(
                text = uiTemplate("shotCard.durationTemplate", language, "seconds" to shot.durationSeconds.toString()),
                style = MaterialTheme.typography.labelSmall,
                color = CinemaTheme.extendedColors.fg3,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
