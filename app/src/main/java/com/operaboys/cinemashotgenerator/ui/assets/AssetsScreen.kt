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
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.LocationType
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.navigation.PLACEHOLDER_ACTIVE_PROJECT_ID
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
// - `projectId` این صفحه (مسیر ریشه‌ی Assets، بدون آرگومان) از همان
//   `PLACEHOLDER_ACTIVE_PROJECT_ID` استفاده می‌کند که نقطه‌ی ورود Studio از نوار
//   پایین از قبل استفاده می‌کرد (BottomNavBar.kt) — تا مفهوم «آخرین/فعال پروژه»
//   واقعی ساخته شود (کار فاز بعدی)، این صفحه هم دقیقاً همان محدودیت شناخته‌شده‌ی
//   موجود Studio را به ارث می‌برد، نه یک محدودیت تازه.
// - دکمه‌ی شناور «افزودن Asset جدید» فقط Snackbar «به‌زودی» نشان می‌دهد — بدنه‌ی
//   فرم واقعی کار قدم بعدی فاز ۳ است (طبق دستور کار صریح این قدم).

const val ASSET_FILTER_CHARACTERS_TAG = "assetLibrary.filter.characters"
const val ASSET_FILTER_LOCATIONS_TAG = "assetLibrary.filter.locations"
const val ASSET_FILTER_OBJECTS_TAG = "assetLibrary.filter.objects"
const val ASSET_LIBRARY_FAB_TAG = "assetLibrary.fab"

@Composable
fun AssetsScreen(
    workflowViewModel: WorkflowViewModel,
    onShowMessage: (String) -> Unit = {},
    assetRepository: AssetRepository? = null,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: AssetLibraryViewModel = viewModel(
        factory = AssetLibraryViewModel.factory(application, PLACEHOLDER_ACTIVE_PROJECT_ID, assetRepository)
    )
    val language by workflowViewModel.language.collectAsStateWithLifecycle()
    val selectedKind by viewModel.selectedKind.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val objects by viewModel.objects.collectAsStateWithLifecycle()
    val selectedCharacterTier by viewModel.selectedCharacterTier.collectAsStateWithLifecycle()
    val selectedLocationType by viewModel.selectedLocationType.collectAsStateWithLifecycle()
    val selectedObjectSubtype by viewModel.selectedObjectSubtype.collectAsStateWithLifecycle()

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
                    language = language
                )
                AssetKind.LOCATION -> LocationAssetList(
                    locations = locations,
                    selectedType = selectedLocationType,
                    onTypeSelected = viewModel::setSelectedLocationType,
                    language = language
                )
                AssetKind.OBJECT -> ObjectAssetList(
                    objects = objects,
                    selectedSubtype = selectedObjectSubtype,
                    onSubtypeSelected = viewModel::setSelectedObjectSubtype,
                    language = language
                )
            }
        }

        FloatingActionButton(
            onClick = { onShowMessage(uiString("assetLibrary.addAssetComingSoon", language)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag(ASSET_LIBRARY_FAB_TAG)
        ) {
            Icon(Icons.Filled.Add, contentDescription = uiString("assetLibrary.addAsset", language))
        }
    }
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
private fun OpaqueChip(label: String, selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier, testTag: String? = null) {
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
private fun AssetCard(name: String, tierLabel: String, description: String, continuityMeta: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThumbnailPlaceholder()
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    TierBadge(label = tierLabel)
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
    language: Language
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
                    name = character.name,
                    tierLabel = characterTierLabel(character.characterTier, language),
                    description = character.basePrompt ?: character.physicalAppearance.toPromptString(),
                    continuityMeta = uiTemplate(
                        "assetLibrary.continuityMetaTemplate", language,
                        "level" to characterContinuityLevelLabel(character.continuityLockLevel, language)
                    )
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
    language: Language
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
                    name = location.name,
                    tierLabel = locationTypeLabel(location.locationType, language),
                    description = location.basePrompt ?: location.description,
                    continuityMeta = uiTemplate(
                        "assetLibrary.continuityMetaTemplate", language,
                        "level" to locationContinuityLevelLabel(location.continuityLockLevel, language)
                    )
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
    language: Language
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
                    name = obj.name,
                    tierLabel = objectSubtypeLabel(obj.subtype, language),
                    description = obj.basePrompt ?: obj.description,
                    continuityMeta = uiTemplate(
                        "assetLibrary.continuityMetaTemplate", language,
                        "level" to propContinuityLevelLabel(obj.continuityLockLevel, language)
                    )
                )
            }
        }
    }
}
