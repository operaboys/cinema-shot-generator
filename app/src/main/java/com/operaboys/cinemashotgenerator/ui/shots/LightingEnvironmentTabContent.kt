package com.operaboys.cinemashotgenerator.ui.shots

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ColorTemperature
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ContrastRatio
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentalMotion
import com.operaboys.cinemashotgenerator.domain.sceneconditions.FillLight
import com.operaboys.cinemashotgenerator.domain.sceneconditions.GroundState
import com.operaboys.cinemashotgenerator.domain.sceneconditions.KeyLightPosition
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightSourceCount
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingMotivation
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ShadowQuality
import com.operaboys.cinemashotgenerator.domain.sceneconditions.TemperatureFeel
import com.operaboys.cinemashotgenerator.domain.sceneconditions.Visibility
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherIntensity
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WindStrength
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormEnumDropdownField
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormFlatEntries
import com.operaboys.cinemashotgenerator.ui.assets.OpaqueChip
import com.operaboys.cinemashotgenerator.ui.dna.lightingStyleLabel
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۴ — قدم ۴ (آخرین قدم): محتوای واقعی Tab «نور و محیط» Shot Composer.
// منبع حقیقت دوگانه: docs/blueprints/08-scene-conditions.md و
// docs/design/README.md بخش «۷. Shot Composer». دو بخش مستقل (نورپردازی/محیط) —
// هرکدام سوییچ منبع/Override خودش را دارد، طبق Shot.lighting/.environment (دو
// SourcedSettings کاملاً مستقل واحد ۰۵)، نه یک سوییچ ترکیبی مشترک. جزئیات کامل
// تصمیمات در docs/adr/053-unit16-phase4-step4-lighting-environment-sound.md.

const val LIGHTING_SOURCE_SCENE_TAG = "lightingTab.sourceScene"
const val LIGHTING_SOURCE_OVERRIDE_TAG = "lightingTab.sourceOverride"
const val LIGHTING_STYLE_FIELD_TAG = "lightingTab.styleField"
const val LIGHTING_ADVANCED_SECTION_TOGGLE_TAG = "lightingTab.advancedSectionToggle"
const val LIGHTING_KEY_LIGHT_POSITION_FIELD_TAG = "lightingTab.keyLightPositionField"
const val LIGHTING_CONTRAST_RATIO_FIELD_TAG = "lightingTab.contrastRatioField"
const val LIGHTING_FILL_LIGHT_FIELD_TAG = "lightingTab.fillLightField"
const val LIGHTING_COLOR_TEMPERATURE_FIELD_TAG = "lightingTab.colorTemperatureField"
const val LIGHTING_SHADOW_QUALITY_FIELD_TAG = "lightingTab.shadowQualityField"
const val LIGHTING_LIGHT_SOURCE_COUNT_FIELD_TAG = "lightingTab.lightSourceCountField"
const val LIGHTING_MOTIVATION_FIELD_TAG = "lightingTab.lightingMotivationField"

const val ENVIRONMENT_SOURCE_SCENE_TAG = "environmentTab.sourceScene"
const val ENVIRONMENT_SOURCE_OVERRIDE_TAG = "environmentTab.sourceOverride"
const val ENVIRONMENT_WEATHER_TYPE_FIELD_TAG = "environmentTab.weatherTypeField"
const val ENVIRONMENT_ADVANCED_SECTION_TOGGLE_TAG = "environmentTab.advancedSectionToggle"
const val ENVIRONMENT_WEATHER_INTENSITY_FIELD_TAG = "environmentTab.weatherIntensityField"
const val ENVIRONMENT_WIND_STRENGTH_FIELD_TAG = "environmentTab.windStrengthField"
const val ENVIRONMENT_GROUND_STATE_FIELD_TAG = "environmentTab.groundStateField"
const val ENVIRONMENT_VISIBILITY_FIELD_TAG = "environmentTab.visibilityField"
const val ENVIRONMENT_TEMPERATURE_FEEL_FIELD_TAG = "environmentTab.temperatureFeelField"

fun environmentalMotionChipTag(value: EnvironmentalMotion): String = "environmentTab.motionChip.${value.name}"

@Composable
fun LightingEnvironmentTabContent(viewModel: ShotComposerViewModel, language: Language, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        LightingSection(viewModel, language)
        HorizontalDivider()
        EnvironmentSection(viewModel, language)
    }
}

/**
 * Dropdown عمومی برای فیلدهای Nullable این دو Tab (fillLight/colorTemperature/
 * shadowQuality/lightSourceCount/lightingMotivation/weatherIntensity/windStrength/
 * groundState/visibility/temperatureFeel) — یک گزینه‌ی «تنظیم‌نشده» (معادل null)
 * همیشه اول فهرست است، هم‌الگو با AssetFormEnumDropdownField موجود.
 */
@Composable
private fun <T> NullableEnumDropdownField(
    label: String,
    selectedLabel: String,
    testTag: String,
    entries: List<T>,
    entryLabel: (T) -> String,
    notSetLabel: String,
    onSelect: (T?) -> Unit
) {
    AssetFormEnumDropdownField(label = label, selectedLabel = selectedLabel, testTag = testTag) { onDismiss ->
        DropdownMenuItem(
            text = { Text(notSetLabel) },
            onClick = { onSelect(null); onDismiss() }
        )
        AssetFormFlatEntries(entries, entryLabel) { onSelect(it); onDismiss() }
    }
}

@Composable
private fun SectionSourceSwitch(
    sceneLabel: String,
    overrideLabel: String,
    sceneDescription: String,
    source: String,
    sceneTag: String,
    overrideTag: String,
    onSourceChange: (String) -> Unit,
    content: @Composable () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OpaqueChip(label = sceneLabel, selected = source != "override", onClick = { onSourceChange("scene") }, testTag = sceneTag)
        OpaqueChip(label = overrideLabel, selected = source == "override", onClick = { onSourceChange("override") }, testTag = overrideTag)
    }
    if (source != "override") {
        Text(text = sceneDescription, style = MaterialTheme.typography.bodyMedium, color = CinemaTheme.extendedColors.fg3)
    } else {
        content()
    }
}

@Composable
private fun AdvancedToggleHeader(title: String, testTag: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag(testTag)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
    }
}

@Composable
private fun LightingSection(viewModel: ShotComposerViewModel, language: Language) {
    val source by viewModel.lightingSource.collectAsStateWithLifecycle()
    val style by viewModel.lightingStyle.collectAsStateWithLifecycle()
    val keyLightPosition by viewModel.keyLightPosition.collectAsStateWithLifecycle()
    val contrastRatio by viewModel.contrastRatio.collectAsStateWithLifecycle()
    val fillLight by viewModel.fillLight.collectAsStateWithLifecycle()
    val colorTemperature by viewModel.lightingColorTemperature.collectAsStateWithLifecycle()
    val shadowQuality by viewModel.shadowQuality.collectAsStateWithLifecycle()
    val lightSourceCount by viewModel.lightSourceCount.collectAsStateWithLifecycle()
    val lightingMotivation by viewModel.lightingMotivation.collectAsStateWithLifecycle()
    var advancedExpanded by remember { mutableStateOf(false) }
    val notSetLabel = uiString("shotComposer.notSet", language)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(uiString("lightingTab.sectionTitle", language), style = MaterialTheme.typography.titleMedium)
        SectionSourceSwitch(
            sceneLabel = uiString("lightingTab.sourceSceneLabel", language),
            overrideLabel = uiString("lightingTab.sourceOverrideLabel", language),
            sceneDescription = uiString("lightingTab.sourceSceneDescription", language),
            source = source,
            sceneTag = LIGHTING_SOURCE_SCENE_TAG,
            overrideTag = LIGHTING_SOURCE_OVERRIDE_TAG,
            onSourceChange = viewModel::setLightingSource
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssetFormEnumDropdownField(
                    label = uiString("lightingTab.styleLabel", language),
                    selectedLabel = lightingStyleLabel(style, language),
                    testTag = LIGHTING_STYLE_FIELD_TAG
                ) { onDismiss ->
                    AssetFormFlatEntries(LightingStyle.entries, { lightingStyleLabel(it, language) }) { viewModel.setLightingStyle(it); onDismiss() }
                }

                AdvancedToggleHeader(
                    title = uiString("lightingTab.advancedSectionToggle", language),
                    testTag = LIGHTING_ADVANCED_SECTION_TOGGLE_TAG,
                    expanded = advancedExpanded,
                    onToggle = { advancedExpanded = !advancedExpanded }
                )
                if (advancedExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssetFormEnumDropdownField(
                            label = uiString("lightingTab.keyLightPositionLabel", language),
                            selectedLabel = keyLightPositionLabel(keyLightPosition, language),
                            testTag = LIGHTING_KEY_LIGHT_POSITION_FIELD_TAG
                        ) { onDismiss ->
                            AssetFormFlatEntries(KeyLightPosition.entries, { keyLightPositionLabel(it, language) }) { viewModel.setKeyLightPosition(it); onDismiss() }
                        }
                        AssetFormEnumDropdownField(
                            label = uiString("lightingTab.contrastRatioLabel", language),
                            selectedLabel = contrastRatioLabel(contrastRatio, language),
                            testTag = LIGHTING_CONTRAST_RATIO_FIELD_TAG
                        ) { onDismiss ->
                            AssetFormFlatEntries(ContrastRatio.entries, { contrastRatioLabel(it, language) }) { viewModel.setContrastRatio(it); onDismiss() }
                        }
                        NullableEnumDropdownField(
                            label = uiString("lightingTab.fillLightLabel", language),
                            selectedLabel = fillLight?.let { fillLightLabel(it, language) } ?: notSetLabel,
                            testTag = LIGHTING_FILL_LIGHT_FIELD_TAG,
                            entries = FillLight.entries,
                            entryLabel = { fillLightLabel(it, language) },
                            notSetLabel = notSetLabel,
                            onSelect = viewModel::setFillLight
                        )
                        NullableEnumDropdownField(
                            label = uiString("lightingTab.colorTemperatureLabel", language),
                            selectedLabel = colorTemperature?.let { sceneConditionsColorTemperatureLabel(it, language) } ?: notSetLabel,
                            testTag = LIGHTING_COLOR_TEMPERATURE_FIELD_TAG,
                            entries = ColorTemperature.entries,
                            entryLabel = { sceneConditionsColorTemperatureLabel(it, language) },
                            notSetLabel = notSetLabel,
                            onSelect = viewModel::setLightingColorTemperature
                        )
                        NullableEnumDropdownField(
                            label = uiString("lightingTab.shadowQualityLabel", language),
                            selectedLabel = shadowQuality?.let { shadowQualityLabel(it, language) } ?: notSetLabel,
                            testTag = LIGHTING_SHADOW_QUALITY_FIELD_TAG,
                            entries = ShadowQuality.entries,
                            entryLabel = { shadowQualityLabel(it, language) },
                            notSetLabel = notSetLabel,
                            onSelect = viewModel::setShadowQuality
                        )
                        NullableEnumDropdownField(
                            label = uiString("lightingTab.lightSourceCountLabel", language),
                            selectedLabel = lightSourceCount?.let { lightSourceCountLabel(it, language) } ?: notSetLabel,
                            testTag = LIGHTING_LIGHT_SOURCE_COUNT_FIELD_TAG,
                            entries = LightSourceCount.entries,
                            entryLabel = { lightSourceCountLabel(it, language) },
                            notSetLabel = notSetLabel,
                            onSelect = viewModel::setLightSourceCount
                        )
                        NullableEnumDropdownField(
                            label = uiString("lightingTab.lightingMotivationLabel", language),
                            selectedLabel = lightingMotivation?.let { lightingMotivationLabel(it, language) } ?: notSetLabel,
                            testTag = LIGHTING_MOTIVATION_FIELD_TAG,
                            entries = LightingMotivation.entries,
                            entryLabel = { lightingMotivationLabel(it, language) },
                            notSetLabel = notSetLabel,
                            onSelect = viewModel::setLightingMotivation
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnvironmentSection(viewModel: ShotComposerViewModel, language: Language) {
    val source by viewModel.environmentSource.collectAsStateWithLifecycle()
    val weatherType by viewModel.weatherType.collectAsStateWithLifecycle()
    val weatherIntensity by viewModel.weatherIntensity.collectAsStateWithLifecycle()
    val windStrength by viewModel.windStrength.collectAsStateWithLifecycle()
    val groundState by viewModel.groundState.collectAsStateWithLifecycle()
    val visibility by viewModel.visibility.collectAsStateWithLifecycle()
    val temperatureFeel by viewModel.temperatureFeel.collectAsStateWithLifecycle()
    val environmentalMotion by viewModel.environmentalMotion.collectAsStateWithLifecycle()
    var advancedExpanded by remember { mutableStateOf(false) }
    val notSetLabel = uiString("shotComposer.notSet", language)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(uiString("environmentTab.sectionTitle", language), style = MaterialTheme.typography.titleMedium)
        SectionSourceSwitch(
            sceneLabel = uiString("environmentTab.sourceSceneLabel", language),
            overrideLabel = uiString("environmentTab.sourceOverrideLabel", language),
            sceneDescription = uiString("environmentTab.sourceSceneDescription", language),
            source = source,
            sceneTag = ENVIRONMENT_SOURCE_SCENE_TAG,
            overrideTag = ENVIRONMENT_SOURCE_OVERRIDE_TAG,
            onSourceChange = viewModel::setEnvironmentSource
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssetFormEnumDropdownField(
                    label = uiString("environmentTab.weatherTypeLabel", language),
                    selectedLabel = weatherTypeLabel(weatherType, language),
                    testTag = ENVIRONMENT_WEATHER_TYPE_FIELD_TAG
                ) { onDismiss ->
                    AssetFormFlatEntries(WeatherType.entries, { weatherTypeLabel(it, language) }) { viewModel.setWeatherType(it); onDismiss() }
                }

                Column {
                    Text(uiString("environmentTab.environmentalMotionLabel", language), style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        items(EnvironmentalMotion.entries) { motion ->
                            val selected = motion in environmentalMotion
                            val atCap = environmentalMotion.size >= 3 && !selected
                            OpaqueChip(
                                label = environmentalMotionLabel(motion, language),
                                selected = selected,
                                onClick = if (atCap) null else ({ viewModel.toggleEnvironmentalMotion(motion) }),
                                testTag = environmentalMotionChipTag(motion)
                            )
                        }
                    }
                }

                AdvancedToggleHeader(
                    title = uiString("environmentTab.advancedSectionToggle", language),
                    testTag = ENVIRONMENT_ADVANCED_SECTION_TOGGLE_TAG,
                    expanded = advancedExpanded,
                    onToggle = { advancedExpanded = !advancedExpanded }
                )
                if (advancedExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NullableEnumDropdownField(
                            label = uiString("environmentTab.weatherIntensityLabel", language),
                            selectedLabel = weatherIntensity?.let { weatherIntensityLabel(it, language) } ?: notSetLabel,
                            testTag = ENVIRONMENT_WEATHER_INTENSITY_FIELD_TAG,
                            entries = WeatherIntensity.entries,
                            entryLabel = { weatherIntensityLabel(it, language) },
                            notSetLabel = notSetLabel,
                            onSelect = viewModel::setWeatherIntensity
                        )
                        NullableEnumDropdownField(
                            label = uiString("environmentTab.windStrengthLabel", language),
                            selectedLabel = windStrength?.let { windStrengthLabel(it, language) } ?: notSetLabel,
                            testTag = ENVIRONMENT_WIND_STRENGTH_FIELD_TAG,
                            entries = WindStrength.entries,
                            entryLabel = { windStrengthLabel(it, language) },
                            notSetLabel = notSetLabel,
                            onSelect = viewModel::setWindStrength
                        )
                        NullableEnumDropdownField(
                            label = uiString("environmentTab.groundStateLabel", language),
                            selectedLabel = groundState?.let { groundStateLabel(it, language) } ?: notSetLabel,
                            testTag = ENVIRONMENT_GROUND_STATE_FIELD_TAG,
                            entries = GroundState.entries,
                            entryLabel = { groundStateLabel(it, language) },
                            notSetLabel = notSetLabel,
                            onSelect = viewModel::setGroundState
                        )
                        NullableEnumDropdownField(
                            label = uiString("environmentTab.visibilityLabel", language),
                            selectedLabel = visibility?.let { visibilityLabel(it, language) } ?: notSetLabel,
                            testTag = ENVIRONMENT_VISIBILITY_FIELD_TAG,
                            entries = Visibility.entries,
                            entryLabel = { visibilityLabel(it, language) },
                            notSetLabel = notSetLabel,
                            onSelect = viewModel::setVisibility
                        )
                        NullableEnumDropdownField(
                            label = uiString("environmentTab.temperatureFeelLabel", language),
                            selectedLabel = temperatureFeel?.let { temperatureFeelLabel(it, language) } ?: notSetLabel,
                            testTag = ENVIRONMENT_TEMPERATURE_FEEL_FIELD_TAG,
                            entries = TemperatureFeel.entries,
                            entryLabel = { temperatureFeelLabel(it, language) },
                            notSetLabel = notSetLabel,
                            onSelect = viewModel::setTemperatureFeel
                        )
                    }
                }
            }
        }
    }
}
