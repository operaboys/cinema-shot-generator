package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.LocationType
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel

// واحد ۱۶ فاز ۳ — قدم ۱: محتوای واقعی صفحه‌ی Assets، جایگزین Placeholder فاز ۱.
// طبق docs/design/README.md بخش «۸. Assets Library» + docs/blueprints/16-...md
// «مرحله ۳». تصمیمات مستقل کامل در
// docs/adr/048-unit16-phase3-step1-asset-library.md — خلاصه:
// - محدودیت بصری صریح سند طراحی («Contrast bug» مستندشده در Implementation
//   Notes): فیلتر Segmented بالا و هر Badge/Chip این صفحه Opaque و با حاشیه‌ی
//   واضح‌اند (رنگ `solidSurface`/`cardBorder` توکن‌های همان دستورالعمل رسمی —
//   #212B4A تیره/سفید روشن — نه هیچ `alpha` کم روی رنگ زمینه).
// - MIGRATED (رفع G6 ممیزی post-Unit16، docs/adr/062-...): `projectId` این صفحه
//   (مسیر ریشه‌ی Assets، بدون آرگومان) دیگر یک PLACEHOLDER ثابت نیست — از
//   `AppNavHost.kt` (با `resolveActiveOrRecentProjectId`، هم‌الگو دقیق با نقطه‌ی
//   ورود Studio از نوار پایین) به‌عنوان یک پارامتر واقعی `String?` تزریق می‌شود.
//   `null` (فقط وقتی اصلاً هیچ پروژه‌ای در کل اپ وجود ندارد) یک محتوای جایگزین
//   با دکمه‌ی «برو به Projects» نشان می‌دهد، به‌جای ساخت ViewModel با یک شناسه‌ی
//   جعلی.
// - واحد ۱۶ فاز ۳ — قدم ۲: دکمه‌ی شناور «افزودن Asset جدید» اکنون به فرم واقعی
//   نوع فعال (Character/Location/Object) Navigate می‌کند — دیگر فقط Snackbar
//   «به‌زودی» نیست (کار همین قدم بود). جزئیات کامل در
//   docs/adr/049-unit16-phase3-step2-asset-forms.md.

const val ASSET_FILTER_CHARACTERS_TAG = "assetLibrary.filter.characters"
const val ASSET_FILTER_LOCATIONS_TAG = "assetLibrary.filter.locations"
const val ASSET_FILTER_OBJECTS_TAG = "assetLibrary.filter.objects"
const val ASSET_LIBRARY_FAB_TAG = "assetLibrary.fab"
const val ASSET_LIBRARY_DELETE_CONFIRM_BUTTON_TAG = "assetLibrary.deleteConfirmButton"

fun assetCardTag(assetId: String): String = "assetLibrary.card.$assetId"
fun assetCardMenuButtonTag(assetId: String): String = "assetLibrary.card.$assetId.menuButton"
fun assetCardDeleteMenuItemTag(assetId: String): String = "assetLibrary.card.$assetId.deleteMenuItem"

@Composable
fun AssetsScreen(
    workflowViewModel: WorkflowViewModel,
    // رفع G6 ممیزی post-Unit16: `null` فقط وقتی اصلاً هیچ پروژه‌ای در کل اپ
    // وجود ندارد (resolveActiveOrRecentProjectId در AppNavHost.kt).
    projectId: String?,
    assetRepository: AssetRepository? = null,
    // رفع G22 ممیزی post-Unit16: برای Rule واقعی حذف Asset (validateAssetDeletion) —
    // هم‌الگو با AssetLibraryViewModel.factory.
    shotRepository: ShotRepository? = null,
    onAddAsset: (AssetKind) -> Unit = {},
    // رفع G7 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md): تا این
    // قدم کارت‌های این صفحه اصلاً onClick نداشتند — لمس یک Asset موجود هیچ
    // اتفاقی نمی‌افتاد. هم‌الگو دقیق با onAddAsset بالا/onOpenScene معادلش در
    // ScenesListScreen.kt.
    onOpenAsset: (AssetKind, String) -> Unit = { _, _ -> },
    onNavigateToProjects: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val language by workflowViewModel.language.collectAsStateWithLifecycle()

    if (projectId == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = uiString("assetLibrary.noActiveProject", language),
                style = MaterialTheme.typography.bodyLarge,
                color = CinemaTheme.extendedColors.fg2
            )
            Button(onClick = onNavigateToProjects, modifier = Modifier.padding(top = 16.dp)) {
                Text(uiString("assetLibrary.goToProjectsButton", language))
            }
        }
        return
    }

    val application = LocalContext.current.applicationContext as Application
    val viewModel: AssetLibraryViewModel = viewModel(
        factory = AssetLibraryViewModel.factory(application, projectId, assetRepository, shotRepository)
    )
    val selectedKind by viewModel.selectedKind.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val objects by viewModel.objects.collectAsStateWithLifecycle()
    val selectedCharacterTier by viewModel.selectedCharacterTier.collectAsStateWithLifecycle()
    val selectedLocationType by viewModel.selectedLocationType.collectAsStateWithLifecycle()
    val selectedObjectSubtype by viewModel.selectedObjectSubtype.collectAsStateWithLifecycle()
    val deleteBlockedMessage by viewModel.deleteBlockedMessage.collectAsStateWithLifecycle()
    // رفع G22: هم‌الگو با deleteTarget موجود ShotListScreen.kt — کلیک روی «حذف»
    // فقط هدف را ست می‌کند، دیالوگ تأیید واقعی خودِ حذف را انجام می‌دهد.
    var deleteTargetId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(deleteBlockedMessage) {
        deleteBlockedMessage?.let { onShowMessage(it); viewModel.clearDeleteBlockedMessage() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Text(text = uiString("assetLibrary.title", language), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = uiString("assetLibrary.subtitle", language),
                style = MaterialTheme.typography.bodyLarge,
                color = CinemaTheme.extendedColors.fg2
            )

            KindFilterRow(
                selectedKind = selectedKind,
                language = language,
                onSelect = viewModel::setSelectedKind,
                modifier = Modifier.padding(top = 16.dp)
            )

            when (selectedKind) {
                AssetKind.CHARACTER -> CharacterAssetList(
                    characters = characters,
                    selectedTier = selectedCharacterTier,
                    onTierSelected = viewModel::setSelectedCharacterTier,
                    language = language,
                    onOpenAsset = { assetId -> onOpenAsset(AssetKind.CHARACTER, assetId) },
                    onDeleteAsset = { assetId -> deleteTargetId = assetId }
                )
                AssetKind.LOCATION -> LocationAssetList(
                    locations = locations,
                    selectedType = selectedLocationType,
                    onTypeSelected = viewModel::setSelectedLocationType,
                    language = language,
                    onOpenAsset = { assetId -> onOpenAsset(AssetKind.LOCATION, assetId) },
                    onDeleteAsset = { assetId -> deleteTargetId = assetId }
                )
                AssetKind.OBJECT -> ObjectAssetList(
                    objects = objects,
                    selectedSubtype = selectedObjectSubtype,
                    onSubtypeSelected = viewModel::setSelectedObjectSubtype,
                    language = language,
                    onOpenAsset = { assetId -> onOpenAsset(AssetKind.OBJECT, assetId) },
                    onDeleteAsset = { assetId -> deleteTargetId = assetId }
                )
            }
        }

        FloatingActionButton(
            onClick = { onAddAsset(selectedKind) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag(ASSET_LIBRARY_FAB_TAG)
        ) {
            Icon(Icons.Filled.Add, contentDescription = uiString("assetLibrary.addAsset", language))
        }
    }

    val targetId = deleteTargetId
    if (targetId != null) {
        DeleteAssetDialog(
            language = language,
            onDismiss = { deleteTargetId = null },
            onConfirm = {
                viewModel.deleteAsset(targetId)
                deleteTargetId = null
            }
        )
    }
}

/**
 * رفع G22 ممیزی post-Unit16: هم‌الگو دقیق با DeleteShotDialog
 * (ShotListScreen.kt)/DeleteSceneDialog (SceneDetailScreen.kt). Rule واقعی
 * «Asset در حال استفاده» در ViewModel اجرا می‌شود (نه اینجا).
 */
@Composable
private fun DeleteAssetDialog(language: Language, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiString("assetLibrary.delete.title", language)) },
        text = { Text(uiString("assetLibrary.delete.message", language)) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(ASSET_LIBRARY_DELETE_CONFIRM_BUTTON_TAG)) {
                Text(uiString("assetLibrary.delete.confirm", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(uiString("assetLibrary.delete.cancel", language))
            }
        }
    )
}

@Composable
private fun KindFilterRow(selectedKind: AssetKind, language: Language, onSelect: (AssetKind) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OpaqueSegmentedButton(
            label = uiString("assetLibrary.filter.characters", language),
            selected = selectedKind == AssetKind.CHARACTER,
            onClick = { onSelect(AssetKind.CHARACTER) },
            testTag = ASSET_FILTER_CHARACTERS_TAG,
            modifier = Modifier.weight(1f)
        )
        OpaqueSegmentedButton(
            label = uiString("assetLibrary.filter.locations", language),
            selected = selectedKind == AssetKind.LOCATION,
            onClick = { onSelect(AssetKind.LOCATION) },
            testTag = ASSET_FILTER_LOCATIONS_TAG,
            modifier = Modifier.weight(1f)
        )
        OpaqueSegmentedButton(
            label = uiString("assetLibrary.filter.objects", language),
            selected = selectedKind == AssetKind.OBJECT,
            onClick = { onSelect(AssetKind.OBJECT) },
            testTag = ASSET_FILTER_OBJECTS_TAG,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * دکمه‌ی Segmented Opaque — طبق docs/design/README.md بخش Implementation Notes
 * (بند ۱): «solid/near-opaque fills ... + a visible 2dp border + drop shadow،
 * selected/active state = solid brand color». هرگز `alpha` کم روی رنگ زمینه —
 * دقیقاً همان باگ Contrast مستندشده که این صفحه نباید تکرار کند.
 */
@Composable
private fun OpaqueSegmentedButton(label: String, selected: Boolean, onClick: () -> Unit, testTag: String, modifier: Modifier = Modifier) {
    val background = if (selected) MaterialTheme.colorScheme.primary else CinemaTheme.extendedColors.solidSurface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else CinemaTheme.extendedColors.fg2
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else CinemaTheme.extendedColors.cardBorder
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = background,
        border = BorderStroke(2.dp, borderColor),
        shadowElevation = 2.dp,
        modifier = modifier.testTag(testTag)
    ) {
        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(text = label, color = contentColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * همان الگوی Opaque بالا، برای ردیف زیرفیلتر و Badge سطح — طبق محدودیت بصری بند ۲
 * دستور کار. `testTag` اختیاری — چون متن یک زیرفیلتر (مثلاً «کاراکتر اصلی») دقیقاً
 * همان متن Badge سطح روی کارت متناظرش هم هست (طبق طراحی، عمدی)، هدف‌گیری با متن
 * برای تست Ambiguous می‌شود؛ testTag این ابهام را رفع می‌کند.
 */
@Composable
internal fun OpaqueChip(label: String, selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier, testTag: String? = null) {
    val background = if (selected) MaterialTheme.colorScheme.primary else CinemaTheme.extendedColors.solidSurface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else CinemaTheme.extendedColors.fg2
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else CinemaTheme.extendedColors.cardBorder
    Surface(
        onClick = onClick ?: {},
        shape = RoundedCornerShape(16.dp),
        color = background,
        border = BorderStroke(2.dp, borderColor),
        modifier = if (testTag != null) modifier.testTag(testTag) else modifier
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/** Badge سطح — همیشه نارنجی Opaque (طبق «tier badge (orange)» سند طراحی)، نه یک Chip قابل‌کلیک. */
@Composable
private fun TierBadge(label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CinemaTheme.extendedColors.orange,
        border = BorderStroke(2.dp, CinemaTheme.extendedColors.orange),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ThumbnailPlaceholder() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(color = CinemaTheme.extendedColors.inset, shape = RoundedCornerShape(12.dp))
    )
}

@Composable
private fun AssetCard(
    assetId: String,
    name: String,
    tierLabel: String,
    description: String,
    continuityMeta: String,
    language: Language,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag(assetCardTag(assetId))) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThumbnailPlaceholder()
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    TierBadge(label = tierLabel)
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.testTag(assetCardMenuButtonTag(assetId))) {
                            Icon(Icons.Filled.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(uiString("assetLibrary.deleteMenuItem", language)) },
                                onClick = { menuExpanded = false; onDeleteClick() },
                                modifier = Modifier.testTag(assetCardDeleteMenuItemTag(assetId))
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = CinemaTheme.extendedColors.fg2,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = continuityMeta,
                    style = MaterialTheme.typography.labelSmall,
                    color = CinemaTheme.extendedColors.fg3,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(language: Language) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = uiString("assetLibrary.emptyState", language),
            style = MaterialTheme.typography.bodyLarge,
            color = CinemaTheme.extendedColors.fg3
        )
    }
}

@Composable
private fun CharacterAssetList(
    characters: List<CharacterAsset>,
    selectedTier: CharacterTier?,
    onTierSelected: (CharacterTier?) -> Unit,
    language: Language,
    onOpenAsset: (String) -> Unit,
    onDeleteAsset: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OpaqueChip(
            uiString("assetLibrary.subfilter.all", language), selected = selectedTier == null,
            onClick = { onTierSelected(null) }, testTag = "assetLibrary.subfilter.characterTier.ALL"
        )
        CharacterTier.entries.forEach { tier ->
            OpaqueChip(
                characterTierLabel(tier, language), selected = selectedTier == tier,
                onClick = { onTierSelected(tier) }, testTag = "assetLibrary.subfilter.characterTier.${tier.name}"
            )
        }
    }

    val filtered = if (selectedTier == null) characters else characters.filter { it.characterTier == selectedTier }
    if (filtered.isEmpty()) {
        EmptyState(language)
    } else {
        LazyColumn(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(filtered, key = { it.assetId }) { character ->
                AssetCard(
                    assetId = character.assetId,
                    name = character.name,
                    tierLabel = characterTierLabel(character.characterTier, language),
                    description = character.basePrompt ?: character.physicalAppearance.toPromptString(),
                    continuityMeta = uiTemplate(
                        "assetLibrary.continuityMetaTemplate", language,
                        "level" to characterContinuityLevelLabel(character.continuityLockLevel, language)
                    ),
                    language = language,
                    onClick = { onOpenAsset(character.assetId) },
                    onDeleteClick = { onDeleteAsset(character.assetId) }
                )
            }
        }
    }
}

@Composable
private fun LocationAssetList(
    locations: List<LocationAsset>,
    selectedType: LocationType?,
    onTypeSelected: (LocationType?) -> Unit,
    language: Language,
    onOpenAsset: (String) -> Unit,
    onDeleteAsset: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OpaqueChip(
            uiString("assetLibrary.subfilter.all", language), selected = selectedType == null,
            onClick = { onTypeSelected(null) }, testTag = "assetLibrary.subfilter.locationType.ALL"
        )
        LocationType.entries.forEach { type ->
            OpaqueChip(
                locationTypeLabel(type, language), selected = selectedType == type,
                onClick = { onTypeSelected(type) }, testTag = "assetLibrary.subfilter.locationType.${type.name}"
            )
        }
    }

    val filtered = if (selectedType == null) locations else locations.filter { it.locationType == selectedType }
    if (filtered.isEmpty()) {
        EmptyState(language)
    } else {
        LazyColumn(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(filtered, key = { it.assetId }) { location ->
                AssetCard(
                    assetId = location.assetId,
                    name = location.name,
                    tierLabel = locationTypeLabel(location.locationType, language),
                    description = location.basePrompt ?: location.description,
                    continuityMeta = uiTemplate(
                        "assetLibrary.continuityMetaTemplate", language,
                        "level" to locationContinuityLevelLabel(location.continuityLockLevel, language)
                    ),
                    language = language,
                    onClick = { onOpenAsset(location.assetId) },
                    onDeleteClick = { onDeleteAsset(location.assetId) }
                )
            }
        }
    }
}

@Composable
private fun ObjectAssetList(
    objects: List<ObjectAsset>,
    selectedSubtype: ObjectSubtype?,
    onSubtypeSelected: (ObjectSubtype?) -> Unit,
    language: Language,
    onOpenAsset: (String) -> Unit,
    onDeleteAsset: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OpaqueChip(
            uiString("assetLibrary.subfilter.all", language), selected = selectedSubtype == null,
            onClick = { onSubtypeSelected(null) }, testTag = "assetLibrary.subfilter.objectSubtype.ALL"
        )
        ObjectSubtype.entries.forEach { subtype ->
            OpaqueChip(
                objectSubtypeLabel(subtype, language), selected = selectedSubtype == subtype,
                onClick = { onSubtypeSelected(subtype) }, testTag = "assetLibrary.subfilter.objectSubtype.${subtype.name}"
            )
        }
    }

    val filtered = if (selectedSubtype == null) objects else objects.filter { it.subtype == selectedSubtype }
    if (filtered.isEmpty()) {
        EmptyState(language)
    } else {
        LazyColumn(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(filtered, key = { it.assetId }) { obj ->
                AssetCard(
                    assetId = obj.assetId,
                    name = obj.name,
                    tierLabel = objectSubtypeLabel(obj.subtype, language),
                    description = obj.basePrompt ?: obj.description,
                    continuityMeta = uiTemplate(
                        "assetLibrary.continuityMetaTemplate", language,
                        "level" to propContinuityLevelLabel(obj.continuityLockLevel, language)
                    ),
                    language = language,
                    onClick = { onOpenAsset(obj.assetId) },
                    onDeleteClick = { onDeleteAsset(obj.assetId) }
                )
            }
        }
    }
}
