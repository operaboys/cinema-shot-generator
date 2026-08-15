package com.operaboys.cinemashotgenerator.ui.scenes

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.asset.AssetType
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import com.operaboys.cinemashotgenerator.domain.workflow.ShotListViewMode
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormEnumDropdownField
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormFlatEntries
import com.operaboys.cinemashotgenerator.ui.assets.assetTypeLabel
import com.operaboys.cinemashotgenerator.ui.assets.characterContinuityLevelLabel
import com.operaboys.cinemashotgenerator.ui.assets.characterTierLabel
import com.operaboys.cinemashotgenerator.ui.assets.locationContinuityLevelLabel
import com.operaboys.cinemashotgenerator.ui.assets.objectSubtypeLabel
import com.operaboys.cinemashotgenerator.ui.assets.propContinuityLevelLabel
import com.operaboys.cinemashotgenerator.ui.dna.visualStyleLabel
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.shots.ShotListScreen
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۴ — قدم ۱ — بخش ب: صفحه‌ی Scene Detail. طبق docs/design/README.md
// بخش «۵. Scene Detail» + docs/blueprints/16-user-workflow-v2.md «مرحله ۴».
// جزئیات کامل تصمیمات (از‌جمله یافته‌ی «۳ Tab نه ۴» و افزودن Scene.state) در
// docs/adr/050-unit16-phase4-step1-scene-detail.md.
//
// AssetFormEnumDropdownField/AssetFormFlatEntries از ui/assets/AssetFormSupport.kt
// (فاز ۳ قدم ۲) بازاستفاده شدند — همان الگوی بازاستفاده‌ی Cross-Feature تأییدشده‌ی
// OpaqueChip در همان قدم؛ این دو تابع کاملاً عمومی‌اند (نه مختص Asset).

// واحد ۱۶ فاز ۴ — قدم ۲: internal (نه private) شد — AppNavHost.kt هنگام برگشت از
// Shot Composer باید بتواند صریحاً Tab «شات‌ها» را انتخاب کند («Composer→Shots»،
// طبق کامنت BackNavigation.kt/docs/design/README.md بخش Interactions)، نه همیشه
// بازنشانی به OVERVIEW. جزئیات کامل در docs/adr/051-unit16-phase4-step2-shot-list-composer-skeleton.md.
internal enum class SceneDetailTab { OVERVIEW, SHOTS, ASSETS }

const val SCENE_DETAIL_CONNECT_LOCATION_BUTTON_TAG = "sceneDetail.connectLocationButton"
const val SCENE_DETAIL_EDIT_BUTTON_TAG = "sceneDetail.editButton"
const val SCENE_DETAIL_ADD_SHOT_BUTTON_TAG = "sceneDetail.addShotButton"
const val SCENE_DETAIL_DUPLICATE_BUTTON_TAG = "sceneDetail.duplicateButton"
const val SCENE_DETAIL_LOCK_BUTTON_TAG = "sceneDetail.lockButton"
const val SCENE_DETAIL_SHOTS_TAB_TAG = "sceneDetail.tab.shots"
const val SCENE_DETAIL_ASSETS_TAB_TAG = "sceneDetail.tab.assets"
const val SCENE_DETAIL_SETTINGS_TITLE_FIELD_TAG = "sceneDetail.settings.titleField"
const val SCENE_DETAIL_SETTINGS_SAVE_BUTTON_TAG = "sceneDetail.settings.saveButton"
/** رفع G8 ممیزی post-Unit16 — دکمه‌ی تغییر Location از داخل SceneSettingsDialog. */
const val SCENE_DETAIL_SETTINGS_CHANGE_LOCATION_BUTTON_TAG = "sceneDetail.settings.changeLocationButton"
const val SCENE_DETAIL_BACK_BUTTON_TAG = "sceneDetail.backButton"
const val SCENE_DETAIL_MENU_BUTTON_TAG = "sceneDetail.menuButton"
const val SCENE_DETAIL_DELETE_MENU_ITEM_TAG = "sceneDetail.deleteMenuItem"
const val SCENE_DETAIL_DELETE_CONFIRM_BUTTON_TAG = "sceneDetail.deleteConfirmButton"
/** یافته‌ی #۱۱ appendix ADR-081 (ADR-085) — دکمه‌ی «افزودن Asset» پایین Tab. */
const val SCENE_DETAIL_ADD_ASSET_BUTTON_TAG = "sceneDetail.addAssetButton"
const val SCENE_DETAIL_TOGGLE_LANGUAGE_BUTTON_TAG = "sceneDetail.toggleLanguageButton"
const val SCENE_DETAIL_TOGGLE_THEME_BUTTON_TAG = "sceneDetail.toggleThemeButton"

fun sceneDetailLocationPickerItemTag(assetId: String): String = "sceneDetail.locationPicker.item.$assetId"
fun sceneDetailLinkedAssetCardTag(assetId: String): String = "sceneDetail.linkedAsset.$assetId"
fun sceneDetailUnlinkAssetButtonTag(assetId: String): String = "sceneDetail.unlinkAssetButton.$assetId"
fun sceneDetailAssetPickerItemTag(assetId: String): String = "sceneDetail.assetPicker.item.$assetId"

@Composable
fun SceneDetailScreen(
    projectId: String,
    sceneId: String,
    language: Language,
    theme: AppTheme,
    onBack: () -> Unit,
    onToggleLanguage: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onNavigateToScene: (String) -> Unit,
    /** (shotId، سطر null یعنی «شات جدید»، sceneNumber، عنوان نمایشی صحنه) — این دو مقدار آخر مستقیماً از اینجا منتقل می‌شوند تا Shot Composer نیازی به بارگذاری مجدد Scene نداشته باشد. */
    onNavigateToShot: (String?, Int, String) -> Unit = { _, _, _ -> },
    onShowMessage: (String) -> Unit = {},
    /** نام `SceneDetailTab` (پیش‌فرض «OVERVIEW») — عمداً String در امضای public، نه خودِ enum internal، طبق قانون Kotlin («public function نمی‌تواند نوع internal را افشا کند»). */
    initialTab: String = SceneDetailTab.OVERVIEW.name,
    shotListViewMode: ShotListViewMode = ShotListViewMode.GRID,
    onShotListViewModeChange: (ShotListViewMode) -> Unit = {},
    sceneRepository: SceneRepository? = null,
    assetRepository: AssetRepository? = null,
    shotRepository: ShotRepository? = null,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: SceneDetailViewModel = viewModel(
        factory = SceneDetailViewModel.factory(application, projectId, sceneId, sceneRepository, assetRepository, shotRepository)
    )
    val scene by viewModel.scene.collectAsStateWithLifecycle()
    val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
    val locationAssets by viewModel.locationAssets.collectAsStateWithLifecycle()
    val characterAssets by viewModel.characterAssets.collectAsStateWithLifecycle()
    val objectAssets by viewModel.objectAssets.collectAsStateWithLifecycle()
    val shotCount by viewModel.shotCount.collectAsStateWithLifecycle()
    val lastActionMessage by viewModel.lastActionMessage.collectAsStateWithLifecycle()
    val deleteBlockedMessage by viewModel.deleteBlockedMessage.collectAsStateWithLifecycle()

    LaunchedEffect(lastActionMessage) {
        lastActionMessage?.let { onShowMessage(it); viewModel.clearLastActionMessage() }
    }
    LaunchedEffect(deleteBlockedMessage) {
        deleteBlockedMessage?.let { onShowMessage(it); viewModel.clearDeleteBlockedMessage() }
    }

    var selectedTab by remember { mutableStateOf(SceneDetailTab.entries.find { it.name == initialTab } ?: SceneDetailTab.OVERVIEW) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAssetPicker by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    // رفع G22 ممیزی post-Unit16: هم‌الگو دقیق با ArchiveProjectDialog/DeleteBackupDialog
    // (docs/adr/060-...) — یک AlertDialog تأیید واقعی، نه حذف بی‌واسطه از منو.
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        SceneDetailHeader(
            title = scene?.let { sceneDisplayTitle(it.sceneTitle, it.sceneNumber, language) } ?: "",
            language = language,
            theme = theme,
            onBack = onBack,
            onToggleLanguage = onToggleLanguage,
            onToggleTheme = onToggleTheme,
            menuExpanded = showOverflowMenu,
            onMenuExpandedChange = { showOverflowMenu = it },
            onHistoryClick = { showOverflowMenu = false; onShowMessage(uiString("sceneDetail.historyComingSoon", language)) },
            onDeleteClick = { showOverflowMenu = false; showDeleteDialog = true }
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
                text = {
                    // یافته‌ی ۱ appendix ADR-081 (ADR-083): Badge عددی طبق mockup
                    // (`sceneTabs`، `t.badge`) — برچسب + شمارش زنده در رنگ کم‌رنگ‌تر،
                    // دقیقاً هم‌چیدمان mockup.
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(uiString("sceneDetail.tab.shots", language))
                        Text(shotCount.toString(), color = CinemaTheme.extendedColors.fg3)
                    }
                },
                modifier = Modifier.testTag(SCENE_DETAIL_SHOTS_TAB_TAG)
            )
            Tab(
                selected = selectedTab == SceneDetailTab.ASSETS,
                onClick = { selectedTab = SceneDetailTab.ASSETS },
                text = {
                    // یافته‌ی #۱۱ appendix ADR-081 (ADR-085): اتصال واقعی Asset↔Scene
                    // اکنون پیاده شد — Badge این Tab هم‌الگو دقیق با Badge Tab SHOTS
                    // بالا (ADR-083) اضافه شد (قبلاً عمداً بدون Badge مانده بود چون این
                    // اتصال هنوز وجود نداشت).
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(uiString("sceneDetail.tab.assets", language))
                        Text((scene?.linkedAssetIds?.size ?: 0).toString(), color = CinemaTheme.extendedColors.fg3)
                    }
                },
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
                    onAddShotClick = { onNavigateToShot(null, currentScene.sceneNumber, sceneDisplayTitle(currentScene.sceneTitle, currentScene.sceneNumber, language)) },
                    onDuplicateClick = { viewModel.duplicateScene(onNavigateToScene) },
                    onLockClick = {
                        val locked = viewModel.lockScene()
                        if (locked) onShowMessage(uiString("sceneDetail.lockSuccess", language))
                    }
                )
                SceneDetailTab.SHOTS -> ShotListScreen(
                    sceneId = sceneId,
                    sceneNumber = currentScene.sceneNumber,
                    viewMode = shotListViewMode,
                    language = language,
                    onViewModeChange = onShotListViewModeChange,
                    onOpenShot = { shotId -> onNavigateToShot(shotId, currentScene.sceneNumber, sceneDisplayTitle(currentScene.sceneTitle, currentScene.sceneNumber, language)) },
                    onAddShot = { onNavigateToShot(null, currentScene.sceneNumber, sceneDisplayTitle(currentScene.sceneTitle, currentScene.sceneNumber, language)) },
                    shotRepository = shotRepository,
                    modifier = Modifier.fillMaxSize()
                )
                SceneDetailTab.ASSETS -> AssetsTab(
                    scene = currentScene,
                    characterAssets = characterAssets,
                    locationAssets = locationAssets,
                    objectAssets = objectAssets,
                    language = language,
                    onAddClick = { showAssetPicker = true },
                    onRemoveClick = { assetId -> viewModel.unlinkAsset(assetId) },
                    modifier = Modifier.fillMaxSize()
                )
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

    val sceneForAssetPicker = scene
    if (showAssetPicker && sceneForAssetPicker != null) {
        AssetLinkPickerDialog(
            scene = sceneForAssetPicker,
            characterAssets = characterAssets,
            locationAssets = locationAssets,
            objectAssets = objectAssets,
            language = language,
            onDismiss = { showAssetPicker = false },
            onSelect = { assetId -> viewModel.linkAsset(assetId); showAssetPicker = false }
        )
    }

    val sceneForSettings = scene
    if (showSettingsDialog && sceneForSettings != null) {
        SceneSettingsDialog(
            scene = sceneForSettings,
            language = language,
            connectedLocationName = locationAssets.find { it.assetId == sceneForSettings.locationAssetId }?.name,
            onChangeLocationClick = { showSettingsDialog = false; showLocationPicker = true },
            onDismiss = { showSettingsDialog = false },
            onSave = { title, role, time, primary, secondary, visualStyleOverride ->
                viewModel.saveSceneSettings(title, role, time, primary, secondary, visualStyleOverride)
                showSettingsDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        DeleteSceneDialog(
            language = language,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteScene(onDeleted = onBack)
            }
        )
    }
}

/**
 * رفع G22 ممیزی post-Unit16: هم‌الگو دقیق با ArchiveProjectDialog
 * (ProjectListSection.kt)/RestoreBackupDialog (BackupsScreen.kt) — متن هشدار
 * صریح غیرقابل‌بازگشت‌بودن. Rule واقعی «صحنه‌ی دارای Shot» در ViewModel اجرا
 * می‌شود (نه اینجا) — این دیالوگ فقط تأیید Confirm/Cancel است.
 */
@Composable
private fun DeleteSceneDialog(language: Language, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiString("sceneDetail.delete.title", language)) },
        text = { Text(uiString("sceneDetail.delete.message", language)) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(SCENE_DETAIL_DELETE_CONFIRM_BUTTON_TAG)) {
                Text(uiString("sceneDetail.delete.confirm", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(uiString("sceneDetail.delete.cancel", language))
            }
        }
    )
}

@Composable
private fun SceneDetailHeader(
    title: String,
    language: Language,
    theme: AppTheme,
    onBack: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onHistoryClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag(SCENE_DETAIL_BACK_BUTTON_TAG)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 4.dp))
        IconButton(onClick = onToggleLanguage, modifier = Modifier.testTag(SCENE_DETAIL_TOGGLE_LANGUAGE_BUTTON_TAG)) {
            Icon(Icons.Filled.Translate, contentDescription = uiString("home.toggleLanguageButton", language))
        }
        IconButton(onClick = onToggleTheme, modifier = Modifier.testTag(SCENE_DETAIL_TOGGLE_THEME_BUTTON_TAG)) {
            Icon(
                if (theme == AppTheme.DARK) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = uiString("home.toggleThemeButton", language)
            )
        }
        Box {
            IconButton(onClick = { onMenuExpandedChange(true) }, modifier = Modifier.testTag(SCENE_DETAIL_MENU_BUTTON_TAG)) {
                Icon(Icons.Filled.MoreVert, contentDescription = null)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenuExpandedChange(false) }) {
                DropdownMenuItem(text = { Text(uiString("sceneDetail.historyMenuItem", language)) }, onClick = onHistoryClick)
                DropdownMenuItem(
                    text = { Text(uiString("sceneDetail.deleteMenuItem", language)) },
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag(SCENE_DETAIL_DELETE_MENU_ITEM_TAG)
                )
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
            value = scene.globalVisualStyle.override?.let { visualStyleLabel(VisualStyle.valueOf(it), language) }
                ?: uiString("sceneDetail.overview.globalVisualStyleFromDna", language)
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
private fun QuickActionTile(icon: ImageVector, label: String, onClick: () -> Unit, testTag: String, modifier: Modifier = Modifier) {
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

/**
 * یافته‌ی #۱۱ appendix ADR-081 (ADR-085) — خلاصه‌ی نمایشی یک Asset متصل، مستقل
 * از سه نوع Kotlin کاملاً جدای CharacterAsset/LocationAsset/ObjectAsset (طبق
 * mockup `linkedAssets`: یک فهرست ناهمگون، نه سه فهرست جدا). فقط UI — دامنه
 * (`domain/asset/`) بدون تغییر ماند.
 */
private data class LinkedAssetSummary(
    val assetId: String,
    val kind: AssetType,
    val name: String,
    val meta: String,
    val lockLabel: String
)

/**
 * سه فهرست Character/Location/Object را به یک فهرست واحد `LinkedAssetSummary`
 * تبدیل می‌کند — meta/lock هرکدام از Label های موجود `AssetLabels.kt` می‌آیند
 * (بدون هیچ رشته‌ی جدید اختراع‌شده): meta = «نوع · زیرگروه» (طبق نمونه‌ی واقعی
 * mockup: «CHARACTER · MAIN»)، lock = همان continuityLockLevel هر نوع (که خودِ
 * appendix صراحتاً به آن اشاره کرده بود).
 */
private fun buildAssetSummaries(
    characterAssets: List<CharacterAsset>,
    locationAssets: List<LocationAsset>,
    objectAssets: List<ObjectAsset>,
    language: Language
): List<LinkedAssetSummary> = buildList {
    characterAssets.forEach { asset ->
        add(
            LinkedAssetSummary(
                assetId = asset.assetId,
                kind = AssetType.CHARACTER,
                name = asset.name,
                meta = "${assetTypeLabel(AssetType.CHARACTER, language)} · ${characterTierLabel(asset.characterTier, language)}",
                lockLabel = characterContinuityLevelLabel(asset.continuityLockLevel, language)
            )
        )
    }
    locationAssets.forEach { asset ->
        add(
            LinkedAssetSummary(
                assetId = asset.assetId,
                kind = AssetType.LOCATION,
                name = asset.name,
                meta = assetTypeLabel(AssetType.LOCATION, language),
                lockLabel = locationContinuityLevelLabel(asset.continuityLockLevel, language)
            )
        )
    }
    objectAssets.forEach { asset ->
        add(
            LinkedAssetSummary(
                assetId = asset.assetId,
                kind = AssetType.OBJECT,
                name = asset.name,
                meta = "${assetTypeLabel(AssetType.OBJECT, language)} · ${objectSubtypeLabel(asset.subtype, language)}",
                lockLabel = propContinuityLevelLabel(asset.continuityLockLevel, language)
            )
        )
    }
}

/** طبق نمونه‌ی واقعی mockup (`linkedAssets`: icon 'person'/'forest'/'radio' برای هر سه نوع) — تابعی از نوع Asset، نه یک فیلد تازه روی خودِ دامنه. */
private fun assetKindIcon(kind: AssetType): ImageVector = when (kind) {
    AssetType.CHARACTER -> Icons.Filled.Person
    AssetType.LOCATION -> Icons.Filled.Forest
    AssetType.OBJECT -> Icons.Filled.Radio
}

@Composable
private fun AssetsTab(
    scene: Scene,
    characterAssets: List<CharacterAsset>,
    locationAssets: List<LocationAsset>,
    objectAssets: List<ObjectAsset>,
    language: Language,
    onAddClick: () -> Unit,
    onRemoveClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allSummaries = remember(characterAssets, locationAssets, objectAssets, language) {
        buildAssetSummaries(characterAssets, locationAssets, objectAssets, language)
    }
    val linkedSummaries = allSummaries.filter { it.assetId in scene.linkedAssetIds }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (linkedSummaries.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = uiString("sceneDetail.assetsEmptyState", language),
                    style = MaterialTheme.typography.bodyLarge,
                    color = CinemaTheme.extendedColors.fg3
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(linkedSummaries, key = { it.assetId }) { summary ->
                    LinkedAssetCard(summary = summary, language = language, onRemoveClick = { onRemoveClick(summary.assetId) })
                }
            }
        }
        AddAssetButton(language = language, onClick = onAddClick)
    }
}

@Composable
private fun LinkedAssetCard(summary: LinkedAssetSummary, language: Language, onRemoveClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().testTag(sceneDetailLinkedAssetCardTag(summary.assetId))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(assetKindIcon(summary.kind), contentDescription = null, tint = Color(0xFF7C5CFF))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = summary.name, style = MaterialTheme.typography.bodyLarge)
                Text(text = summary.meta, style = MaterialTheme.typography.bodySmall, color = CinemaTheme.extendedColors.fg3)
            }
            Text(text = summary.lockLabel, style = MaterialTheme.typography.bodySmall, color = Color(0xFF3DDC97))
            IconButton(onClick = onRemoveClick, modifier = Modifier.testTag(sceneDetailUnlinkAssetButtonTag(summary.assetId))) {
                Icon(Icons.Filled.Close, contentDescription = uiString("sceneDetail.assets.removeAction", language))
            }
        }
    }
}

/**
 * طبق mockup (`act.openSheet`، `x.k34`) این دکمه در mockup یک ModalBottomSheet
 * باز می‌کرد؛ این پروژه عمداً از ModalBottomSheet استفاده نمی‌کند (تصمیم
 * ADR-052، مستند در appendix ADR-081 «عناصر مشترک») — همان‌جا که mockup Sheet
 * باز می‌کرد، اینجا (Tab Assets صحنه) هم مثل بقیه‌ی نقاط مشابه یک Dialog باز
 * می‌شود (`AssetLinkPickerDialog` پایین‌تر).
 */
@Composable
private fun AddAssetButton(language: Language, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, CinemaTheme.extendedColors.hairlineStrong), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp)
            .testTag(SCENE_DETAIL_ADD_ASSET_BUTTON_TAG),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF7C5CFF))
        Text(text = uiString("sceneDetail.assets.addButton", language), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun AssetLinkPickerDialog(
    scene: Scene,
    characterAssets: List<CharacterAsset>,
    locationAssets: List<LocationAsset>,
    objectAssets: List<ObjectAsset>,
    language: Language,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val unlinkedSummaries = remember(characterAssets, locationAssets, objectAssets, scene.linkedAssetIds, language) {
        buildAssetSummaries(characterAssets, locationAssets, objectAssets, language)
            .filter { it.assetId !in scene.linkedAssetIds }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiString("sceneDetail.assets.pickerTitle", language)) },
        text = {
            if (unlinkedSummaries.isEmpty()) {
                Text(uiString("sceneDetail.assets.pickerEmpty", language), color = CinemaTheme.extendedColors.fg3)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(unlinkedSummaries, key = { it.assetId }) { summary ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(summary.assetId) }
                                .testTag(sceneDetailAssetPickerItemTag(summary.assetId))
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(assetKindIcon(summary.kind), contentDescription = null, tint = Color(0xFF7C5CFF))
                            Column {
                                Text(text = summary.name, style = MaterialTheme.typography.bodyLarge)
                                Text(text = summary.meta, style = MaterialTheme.typography.bodySmall, color = CinemaTheme.extendedColors.fg3)
                            }
                        }
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

// رفع G8 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، اولویت ۳ بند ۸):
// location قبلاً از SceneSettingsDialog قابل ویرایش نبود — فقط از یک Quick Action
// جدا («اتصال به کتابخانه»، OverviewTab). به‌جای ساخت یک انتخابگر تازه، همان
// LocationPickerDialog/connectLocationAsset موجود (فاز ۴ واحد ۱۶) از داخل همین
// Dialog هم قابل‌فراخوانی شد — تصمیم مستقل: تغییر location بلافاصله ذخیره می‌شود
// (هم‌الگو دقیق با رفتار موجود Quick Action)، نه منتظر دکمه‌ی «ذخیره» عمومی این
// Dialog — چون connectLocationAsset از قبل یک عملیات مستقل و فوری است، نه بخشی
// از saveSceneSettings؛ تغییر این رفتار موجود خارج از Scope این قدم بود.
@Composable
private fun SceneSettingsDialog(
    scene: Scene,
    language: Language,
    connectedLocationName: String?,
    onChangeLocationClick: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String?, NarrativeRole, TimeOfDay, Atmosphere, Atmosphere?, String?) -> Unit
) {
    var title by remember { mutableStateOf(scene.sceneTitle ?: "") }
    var narrativeRole by remember { mutableStateOf(scene.narrativeRole) }
    var timeOfDay by remember { mutableStateOf(scene.timeOfDay) }
    var atmospherePrimary by remember { mutableStateOf(scene.atmospherePrimary) }
    var atmosphereSecondary by remember { mutableStateOf(scene.atmosphereSecondary) }
    var globalVisualStyleOverride by remember { mutableStateOf(scene.globalVisualStyle.override) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = uiString("sceneDetail.overview.locationLabel", language), style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = connectedLocationName ?: uiString("sceneDetail.overview.noLocationConnected", language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CinemaTheme.extendedColors.fg3
                        )
                    }
                    TextButton(onClick = onChangeLocationClick, modifier = Modifier.testTag(SCENE_DETAIL_SETTINGS_CHANGE_LOCATION_BUTTON_TAG)) {
                        Text(uiString("sceneDetail.overview.connectLocationButton", language))
                    }
                }
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
                // رفع G8 ممیزی post-Unit16 (docs/adr/076-...): globalVisualStyle
                // قبلاً فقط نمایشی بود. GlobalVisualStyleRef.override یک String?
                // خام است (نه VisualStyle enum مستقیم — طبق شکل JSON بلوپرینت)؛
                // این Dropdown مقادیر VisualStyle واقعی (۳۴ مورد، واحد ۰۲) را
                // نشان می‌دهد اما override را با .name رشته‌ای ذخیره می‌کند —
                // بدون تغییر نوع دیتامدل. source همیشه "project_dna" می‌ماند؛
                // فقط override بین null (پیش‌فرض DNA) و یک VisualStyle سوییچ می‌شود.
                AssetFormEnumDropdownField(
                    label = uiString("sceneDetail.overview.globalVisualStyleLabel", language),
                    selectedLabel = globalVisualStyleOverride?.let { visualStyleLabel(VisualStyle.valueOf(it), language) }
                        ?: uiString("sceneDetail.overview.globalVisualStyleFromDna", language),
                    testTag = "sceneDetail.settings.globalVisualStyleField"
                ) { onDismissMenu ->
                    Column {
                        DropdownMenuItem(
                            text = { Text(uiString("sceneDetail.overview.globalVisualStyleFromDna", language)) },
                            onClick = { globalVisualStyleOverride = null; onDismissMenu() }
                        )
                        AssetFormFlatEntries(VisualStyle.entries, { visualStyleLabel(it, language) }) { globalVisualStyleOverride = it.name; onDismissMenu() }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.ifBlank { null }, narrativeRole, timeOfDay, atmospherePrimary, atmosphereSecondary, globalVisualStyleOverride) },
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
