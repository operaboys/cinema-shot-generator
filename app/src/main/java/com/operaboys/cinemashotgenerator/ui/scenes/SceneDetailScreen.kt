package com.operaboys.cinemashotgenerator.ui.scenes

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormEnumDropdownField
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormFlatEntries
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۴ — قدم ۱ — بخش ب: صفحه‌ی Scene Detail. طبق docs/design/README.md
// بخش «۵. Scene Detail» + docs/blueprints/16-user-workflow-v2.md «مرحله ۴».
// جزئیات کامل تصمیمات (از‌جمله یافته‌ی «۳ Tab نه ۴» و افزودن Scene.state) در
// docs/adr/050-unit16-phase4-step1-scene-detail.md.
//
// AssetFormEnumDropdownField/AssetFormFlatEntries از ui/assets/AssetFormSupport.kt
// (فاز ۳ قدم ۲) بازاستفاده شدند — همان الگوی بازاستفاده‌ی Cross-Feature تأییدشده‌ی
// OpaqueChip در همان قدم؛ این دو تابع کاملاً عمومی‌اند (نه مختص Asset).

private enum class SceneDetailTab { OVERVIEW, SHOTS, ASSETS }

const val SCENE_DETAIL_CONNECT_LOCATION_BUTTON_TAG = "sceneDetail.connectLocationButton"
const val SCENE_DETAIL_EDIT_BUTTON_TAG = "sceneDetail.editButton"
const val SCENE_DETAIL_ADD_SHOT_BUTTON_TAG = "sceneDetail.addShotButton"
const val SCENE_DETAIL_DUPLICATE_BUTTON_TAG = "sceneDetail.duplicateButton"
const val SCENE_DETAIL_LOCK_BUTTON_TAG = "sceneDetail.lockButton"
const val SCENE_DETAIL_SHOTS_TAB_TAG = "sceneDetail.tab.shots"
const val SCENE_DETAIL_ASSETS_TAB_TAG = "sceneDetail.tab.assets"
const val SCENE_DETAIL_SETTINGS_TITLE_FIELD_TAG = "sceneDetail.settings.titleField"
const val SCENE_DETAIL_SETTINGS_SAVE_BUTTON_TAG = "sceneDetail.settings.saveButton"
const val SCENE_DETAIL_BACK_BUTTON_TAG = "sceneDetail.backButton"

fun sceneDetailLocationPickerItemTag(assetId: String): String = "sceneDetail.locationPicker.item.$assetId"

@Composable
fun SceneDetailScreen(
    projectId: String,
    sceneId: String,
    language: Language,
    onBack: () -> Unit,
    onNavigateToScene: (String) -> Unit,
    onShowMessage: (String) -> Unit = {},
    sceneRepository: SceneRepository? = null,
    assetRepository: AssetRepository? = null,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: SceneDetailViewModel = viewModel(
        factory = SceneDetailViewModel.factory(application, projectId, sceneId, sceneRepository, assetRepository)
    )
    val scene by viewModel.scene.collectAsStateWithLifecycle()
    val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
    val locationAssets by viewModel.locationAssets.collectAsStateWithLifecycle()
    val lastActionMessage by viewModel.lastActionMessage.collectAsStateWithLifecycle()

    LaunchedEffect(lastActionMessage) {
        lastActionMessage?.let { onShowMessage(it); viewModel.clearLastActionMessage() }
    }

    var selectedTab by remember { mutableStateOf(SceneDetailTab.OVERVIEW) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        SceneDetailHeader(
            title = scene?.let { sceneDisplayTitle(it.sceneTitle, it.sceneNumber, language) } ?: "",
            language = language,
            onBack = onBack,
            menuExpanded = showOverflowMenu,
            onMenuExpandedChange = { showOverflowMenu = it },
            onHistoryClick = { showOverflowMenu = false; onShowMessage(uiString("sceneDetail.historyComingSoon", language)) }
        )

        SecondaryTabRow(selectedTabIndex = selectedTab.ordinal) {
            Tab(
                selected = selectedTab == SceneDetailTab.OVERVIEW,
                onClick = { selectedTab = SceneDetailTab.OVERVIEW },
                text = { Text(uiString("sceneDetail.tab.overview", language)) }
            )
            Tab(
                selected = selectedTab == SceneDetailTab.SHOTS,
                onClick = { selectedTab = SceneDetailTab.SHOTS },
                text = { Text(uiString("sceneDetail.tab.shots", language)) },
                modifier = Modifier.testTag(SCENE_DETAIL_SHOTS_TAB_TAG)
            )
            Tab(
                selected = selectedTab == SceneDetailTab.ASSETS,
                onClick = { selectedTab = SceneDetailTab.ASSETS },
                text = { Text(uiString("sceneDetail.tab.assets", language)) },
                modifier = Modifier.testTag(SCENE_DETAIL_ASSETS_TAB_TAG)
            )
        }

        val currentScene = scene
        if (!isLoaded || currentScene == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "…", color = CinemaTheme.extendedColors.fg3)
            }
        } else {
            when (selectedTab) {
                SceneDetailTab.OVERVIEW -> OverviewTab(
                    scene = currentScene,
                    language = language,
                    locationAssets = locationAssets,
                    onConnectLocationClick = { showLocationPicker = true },
                    onEditClick = { showSettingsDialog = true },
                    onAddShotClick = { onShowMessage(uiString("sceneDetail.addShotComingSoon", language)) },
                    onDuplicateClick = { viewModel.duplicateScene(onNavigateToScene) },
                    onLockClick = {
                        val locked = viewModel.lockScene()
                        if (locked) onShowMessage(uiString("sceneDetail.lockSuccess", language))
                    }
                )
                SceneDetailTab.SHOTS -> EmptyTabState(uiString("sceneDetail.shotsEmptyState", language))
                SceneDetailTab.ASSETS -> EmptyTabState(uiString("sceneDetail.assetsEmptyState", language))
            }
        }
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            language = language,
            locationAssets = locationAssets,
            onDismiss = { showLocationPicker = false },
            onSelect = { assetId -> viewModel.connectLocationAsset(assetId); showLocationPicker = false }
        )
    }

    val sceneForSettings = scene
    if (showSettingsDialog && sceneForSettings != null) {
        SceneSettingsDialog(
            scene = sceneForSettings,
            language = language,
            onDismiss = { showSettingsDialog = false },
            onSave = { title, role, time, primary, secondary ->
                viewModel.saveSceneSettings(title, role, time, primary, secondary)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
private fun SceneDetailHeader(
    title: String,
    language: Language,
    onBack: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onHistoryClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag(SCENE_DETAIL_BACK_BUTTON_TAG)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 4.dp))
        Box {
            IconButton(onClick = { onMenuExpandedChange(true) }) {
                Icon(Icons.Filled.MoreVert, contentDescription = null)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenuExpandedChange(false) }) {
                DropdownMenuItem(text = { Text(uiString("sceneDetail.historyMenuItem", language)) }, onClick = onHistoryClick)
            }
        }
    }
}

@Composable
private fun OverviewTab(
    scene: Scene,
    language: Language,
    locationAssets: List<LocationAsset>,
    onConnectLocationClick: () -> Unit,
    onEditClick: () -> Unit,
    onAddShotClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onLockClick: () -> Unit
) {
    val connectedLocation = scene.locationAssetId?.let { id -> locationAssets.find { it.assetId == id } }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EntityStateChip(state = scene.state, language = language)

        InfoRow(
            label = uiString("sceneDetail.overview.locationLabel", language),
            value = connectedLocation?.name ?: scene.location.description.ifBlank { uiString("sceneDetail.overview.noLocationConnected", language) }
        )
        TextButton(onClick = onConnectLocationClick, modifier = Modifier.testTag(SCENE_DETAIL_CONNECT_LOCATION_BUTTON_TAG)) {
            Text(uiString("sceneDetail.overview.connectLocationButton", language))
        }
        InfoRow(label = uiString("sceneDetail.overview.typeLabel", language), value = sceneLocationTypeLabel(scene.location.type, language))
        InfoRow(label = uiString("sceneDetail.overview.timeOfDayLabel", language), value = timeOfDayLabel(scene.timeOfDay, language))
        InfoRow(
            label = uiString("sceneDetail.overview.atmosphereLabel", language),
            value = listOfNotNull(
                atmosphereLabel(scene.atmospherePrimary, language),
                scene.atmosphereSecondary?.let { atmosphereLabel(it, language) }
            ).joinToString(" / ")
        )
        InfoRow(label = uiString("sceneDetail.overview.narrativeRoleLabel", language), value = narrativeRoleLabel(scene.narrativeRole, language))
        InfoRow(
            label = uiString("sceneDetail.overview.globalVisualStyleLabel", language),
            value = scene.globalVisualStyle.override ?: uiString("sceneDetail.overview.globalVisualStyleFromDna", language)
        )

        QuickActionsRow(
            language = language,
            onEditClick = onEditClick,
            onAddShotClick = onAddShotClick,
            onDuplicateClick = onDuplicateClick,
            onLockClick = onLockClick
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = CinemaTheme.extendedColors.fg3)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun QuickActionsRow(
    language: Language,
    onEditClick: () -> Unit,
    onAddShotClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onLockClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionTile(Icons.Filled.Edit, uiString("sceneDetail.quickActions.editScene", language), onEditClick, SCENE_DETAIL_EDIT_BUTTON_TAG, Modifier.weight(1f))
        QuickActionTile(Icons.Filled.Add, uiString("sceneDetail.quickActions.addShot", language), onAddShotClick, SCENE_DETAIL_ADD_SHOT_BUTTON_TAG, Modifier.weight(1f))
        QuickActionTile(Icons.Filled.ContentCopy, uiString("sceneDetail.quickActions.duplicate", language), onDuplicateClick, SCENE_DETAIL_DUPLICATE_BUTTON_TAG, Modifier.weight(1f))
        QuickActionTile(Icons.Filled.Lock, uiString("sceneDetail.quickActions.lockScene", language), onLockClick, SCENE_DETAIL_LOCK_BUTTON_TAG, Modifier.weight(1f))
    }
}

@Composable
private fun QuickActionTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, testTag: String, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.testTag(testTag)) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null)
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun EmptyTabState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge, color = CinemaTheme.extendedColors.fg3)
    }
}

@Composable
private fun LocationPickerDialog(
    language: Language,
    locationAssets: List<LocationAsset>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiString("sceneDetail.locationPicker.title", language)) },
        text = {
            if (locationAssets.isEmpty()) {
                Text(uiString("sceneDetail.locationPicker.empty", language), color = CinemaTheme.extendedColors.fg3)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(locationAssets, key = { it.assetId }) { asset ->
                        Text(
                            text = asset.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(asset.assetId) }
                                .testTag(sceneDetailLocationPickerItemTag(asset.assetId))
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(uiString("assetForm.saveButton", language)) }
        }
    )
}

@Composable
private fun SceneSettingsDialog(
    scene: Scene,
    language: Language,
    onDismiss: () -> Unit,
    onSave: (String?, NarrativeRole, TimeOfDay, Atmosphere, Atmosphere?) -> Unit
) {
    var title by remember { mutableStateOf(scene.sceneTitle ?: "") }
    var narrativeRole by remember { mutableStateOf(scene.narrativeRole) }
    var timeOfDay by remember { mutableStateOf(scene.timeOfDay) }
    var atmospherePrimary by remember { mutableStateOf(scene.atmospherePrimary) }
    var atmosphereSecondary by remember { mutableStateOf(scene.atmosphereSecondary) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiString("sceneDetail.settingsDialogTitle", language)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(uiString("sceneDetail.settingsTitleLabel", language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(SCENE_DETAIL_SETTINGS_TITLE_FIELD_TAG)
                )
                AssetFormEnumDropdownField(
                    label = uiString("sceneDetail.overview.narrativeRoleLabel", language),
                    selectedLabel = narrativeRoleLabel(narrativeRole, language),
                    testTag = "sceneDetail.settings.narrativeRoleField"
                ) { onDismissMenu ->
                    AssetFormFlatEntries(NarrativeRole.entries, { narrativeRoleLabel(it, language) }) { narrativeRole = it; onDismissMenu() }
                }
                AssetFormEnumDropdownField(
                    label = uiString("sceneDetail.overview.timeOfDayLabel", language),
                    selectedLabel = timeOfDayLabel(timeOfDay, language),
                    testTag = "sceneDetail.settings.timeOfDayField"
                ) { onDismissMenu ->
                    AssetFormFlatEntries(TimeOfDay.entries, { timeOfDayLabel(it, language) }) { timeOfDay = it; onDismissMenu() }
                }
                AssetFormEnumDropdownField(
                    label = uiString("sceneDetail.overview.atmosphereLabel", language) + " (Primary)",
                    selectedLabel = atmosphereLabel(atmospherePrimary, language),
                    testTag = "sceneDetail.settings.atmospherePrimaryField"
                ) { onDismissMenu ->
                    AssetFormFlatEntries(Atmosphere.entries, { atmosphereLabel(it, language) }) { atmospherePrimary = it; onDismissMenu() }
                }
                AssetFormEnumDropdownField(
                    label = uiString("sceneDetail.overview.atmosphereLabel", language) + " (Secondary)",
                    selectedLabel = atmosphereSecondary?.let { atmosphereLabel(it, language) } ?: uiString("dna.lightingPreference.none", language),
                    testTag = "sceneDetail.settings.atmosphereSecondaryField"
                ) { onDismissMenu ->
                    Column {
                        DropdownMenuItem(
                            text = { Text(uiString("dna.lightingPreference.none", language)) },
                            onClick = { atmosphereSecondary = null; onDismissMenu() }
                        )
                        AssetFormFlatEntries(Atmosphere.entries, { atmosphereLabel(it, language) }) { atmosphereSecondary = it; onDismissMenu() }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.ifBlank { null }, narrativeRole, timeOfDay, atmospherePrimary, atmosphereSecondary) },
                modifier = Modifier.testTag(SCENE_DETAIL_SETTINGS_SAVE_BUTTON_TAG)
            ) {
                Text(uiString("sceneDetail.settingsSaveButton", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(uiString("aiBreakdown.dismissButton", language)) }
        }
    )
}
