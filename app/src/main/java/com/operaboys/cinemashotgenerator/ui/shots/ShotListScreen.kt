package com.operaboys.cinemashotgenerator.ui.shots

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

fun shotCardTag(shotId: String): String = "shotsList.card.$shotId"

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
                        ShotCard(shot = shot, sceneNumber = sceneNumber, language = language, onClick = { onOpenShot(shot.shotId) })
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(shots, key = { it.shotId }) { shot ->
                        ShotCard(shot = shot, sceneNumber = sceneNumber, language = language, onClick = { onOpenShot(shot.shotId) })
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
}

/**
 * نشانگر کوچک Override (طبق دستور کار بخش الف): اگر camera/lighting/environment
 * هرکدام source=="override" باشند، یعنی این شات تنظیمات صحنه را Override کرده.
 */
private fun Shot.hasOverriddenSettings(): Boolean =
    camera.source == "override" || lighting.source == "override" || environment.source == "override"

@Composable
private fun ShotCard(shot: Shot, sceneNumber: Int, language: Language, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag(shotCardTag(shot.shotId))) {
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
