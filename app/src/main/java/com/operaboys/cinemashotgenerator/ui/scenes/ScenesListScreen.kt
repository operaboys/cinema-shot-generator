package com.operaboys.cinemashotgenerator.ui.scenes

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۴ — قدم ۱ — بخش ج: محتوای واقعی Tab «صحنه‌ها»، جایگزین Placeholder
// فاز ۱. طبق docs/blueprints/16-user-workflow-v2.md «مرحله ۴» + docs/design/README.md
// بخش «۳. Studio → Scenes tab» (scene cards: thumbnail، title، role/time/weather
// meta، shot-count + state chip). جزئیات کامل تصمیمات در
// docs/adr/050-unit16-phase4-step1-scene-detail.md.

const val SCENES_LIST_NEW_SCENE_FAB_TAG = "scenesList.newSceneFab"

@Composable
fun ScenesListScreen(
    projectId: String,
    language: Language,
    onOpenScene: (String) -> Unit,
    sceneRepository: SceneRepository? = null,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ScenesListViewModel = viewModel(
        factory = ScenesListViewModel.factory(application, projectId, sceneRepository)
    )
    val scenes by viewModel.scenes.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        if (scenes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = uiString("scenesList.emptyState", language),
                    style = MaterialTheme.typography.bodyLarge,
                    color = CinemaTheme.extendedColors.fg3
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(scenes, key = { it.sceneId }) { scene ->
                    SceneCard(scene = scene, language = language, onClick = { onOpenScene(scene.sceneId) })
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.createNewScene(onOpenScene) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag(SCENES_LIST_NEW_SCENE_FAB_TAG)
        ) {
            Icon(Icons.Filled.Add, contentDescription = uiString("scenesList.newScene", language))
        }
    }
}

@Composable
private fun SceneCard(scene: Scene, language: Language, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color = CinemaTheme.extendedColors.inset, shape = RoundedCornerShape(12.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = sceneDisplayTitle(scene.sceneTitle, scene.sceneNumber, language),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    EntityStateChip(state = scene.state, language = language)
                }
                Text(
                    text = listOf(
                        narrativeRoleLabel(scene.narrativeRole, language),
                        timeOfDayLabel(scene.timeOfDay, language),
                        atmosphereLabel(scene.atmospherePrimary, language)
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = CinemaTheme.extendedColors.fg2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * چیپ وضعیت مشترک Project/Scene — طبق تصمیم مستند در `SceneLabels.entityStateLabel`،
 * از همان کلیدهای ترجمه‌ی "project.state.*" استفاده می‌کند؛ رنگ‌ها هم عیناً از
 * `ProjectCard.stateChipColor` کپی شدند (آن تابع `private` است، بازاستفاده‌ی مستقیم
 * ممکن نبود).
 */
@Composable
internal fun EntityStateChip(state: EntityState, language: Language) {
    // یافته‌ی واقعی بازبینی نهایی (واحد ۱۶ فاز ۶ قدم ۲، ADR-059): همان باگ
    // Low-opacity Tinted Fill که در ProjectCard.kt پیدا و رفع شد — این چیپ عیناً
    // از همان تابع (که private بود) کپی شده بود، پس همان باگ را هم کپی کرده بود.
    // رفع هم‌الگو: پس‌زمینه‌ی Solid + حاشیه، متن سیاه ثابت.
    val color = entityStateColor(state)
    Surface(shape = RoundedCornerShape(18.dp), color = color, border = BorderStroke(2.dp, color)) {
        Text(
            text = entityStateLabel(state, language),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun entityStateColor(state: EntityState) = when (state) {
    EntityState.DRAFT -> MaterialTheme.colorScheme.outline
    EntityState.REVIEW -> CinemaTheme.extendedColors.warning
    EntityState.LOCKED -> CinemaTheme.extendedColors.orange
    EntityState.FINAL -> CinemaTheme.extendedColors.success
    EntityState.ARCHIVED -> CinemaTheme.extendedColors.fg4
}
