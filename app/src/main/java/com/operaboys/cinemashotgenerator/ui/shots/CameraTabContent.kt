package com.operaboys.cinemashotgenerator.ui.shots

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import com.operaboys.cinemashotgenerator.domain.camera.AdvancedMovementType
import com.operaboys.cinemashotgenerator.domain.camera.BasicMovementType
import com.operaboys.cinemashotgenerator.domain.camera.CameraAngle
import com.operaboys.cinemashotgenerator.domain.camera.CameraDistance
import com.operaboys.cinemashotgenerator.domain.camera.CameraMovement
import com.operaboys.cinemashotgenerator.domain.camera.DepthOfField
import com.operaboys.cinemashotgenerator.domain.camera.Framing
import com.operaboys.cinemashotgenerator.domain.camera.FocusMode
import com.operaboys.cinemashotgenerator.domain.camera.LensType
import com.operaboys.cinemashotgenerator.domain.camera.Stabilization
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormEnumDropdownField
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormFlatEntries
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormValidationIssueRow
import com.operaboys.cinemashotgenerator.ui.assets.OpaqueChip
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme
import kotlin.math.roundToInt

// واحد ۱۶ فاز ۴ — قدم ۳: محتوای واقعی Tab «دوربین» Shot Composer. منبع حقیقت
// دوگانه: docs/blueprints/09-camera-and-motion.md (بخش الف) و
// docs/design/README.md بخش «۷. Shot Composer» («field rows label/value/expand»،
// «Advanced expandable section»، «Attached References chip row»). جزئیات کامل
// تصمیمات (خصوصاً طراحی فرم شرطی Movement) در
// docs/adr/052-unit16-phase4-step3-camera-tab.md.

const val CAMERA_TAB_SOURCE_SCENE_TAG = "cameraTab.sourceScene"
const val CAMERA_TAB_SOURCE_OVERRIDE_TAG = "cameraTab.sourceOverride"
const val CAMERA_TAB_ANGLE_FIELD_TAG = "cameraTab.angleField"
const val CAMERA_TAB_DISTANCE_FIELD_TAG = "cameraTab.distanceField"
const val CAMERA_TAB_LENS_TYPE_FIELD_TAG = "cameraTab.lensTypeField"
const val CAMERA_TAB_MOVEMENT_TIER_BASIC_TAG = "cameraTab.movementTierBasic"
const val CAMERA_TAB_MOVEMENT_TIER_ADVANCED_TAG = "cameraTab.movementTierAdvanced"
const val CAMERA_TAB_BASIC_MOVEMENT_TYPE_FIELD_TAG = "cameraTab.basicMovementTypeField"
const val CAMERA_TAB_BASIC_SPEED_FIELD_TAG = "cameraTab.basicSpeedField"
const val CAMERA_TAB_ADVANCED_MOVEMENT_TYPE_FIELD_TAG = "cameraTab.advancedMovementTypeField"
const val CAMERA_TAB_ORBIT_DEGREES_FIELD_TAG = "cameraTab.orbitDegreesField"
const val CAMERA_TAB_ORBIT_SPEED_FIELD_TAG = "cameraTab.orbitSpeedField"
const val CAMERA_TAB_ORBIT_MAINTAIN_EYE_LEVEL_TAG = "cameraTab.orbitMaintainEyeLevel"
const val CAMERA_TAB_DRONE_ALTITUDE_FIELD_TAG = "cameraTab.dronePathAltitudeField"
const val CAMERA_TAB_DRONE_PATH_TYPE_FIELD_TAG = "cameraTab.dronePathTypeField"
const val CAMERA_TAB_DRONE_SPEED_FIELD_TAG = "cameraTab.dronePathSpeedField"
const val CAMERA_TAB_DOLLY_FOCAL_START_FIELD_TAG = "cameraTab.dollyZoomFocalStartField"
const val CAMERA_TAB_DOLLY_FOCAL_END_FIELD_TAG = "cameraTab.dollyZoomFocalEndField"
const val CAMERA_TAB_DOLLY_DIRECTION_FIELD_TAG = "cameraTab.dollyZoomDirectionField"
const val CAMERA_TAB_HANDHELD_INTENSITY_FIELD_TAG = "cameraTab.handheldShakeIntensityField"
const val CAMERA_TAB_HANDHELD_FREQUENCY_FIELD_TAG = "cameraTab.handheldShakeFrequencyField"
const val CAMERA_TAB_COMPOUND_PRIMARY_FIELD_TAG = "cameraTab.compoundPrimaryField"
const val CAMERA_TAB_COMPOUND_SECONDARY_FIELD_TAG = "cameraTab.compoundSecondaryField"
const val CAMERA_TAB_COMPOUND_SYNC_FIELD_TAG = "cameraTab.compoundSyncField"
const val CAMERA_TAB_ADVANCED_SECTION_TOGGLE_TAG = "cameraTab.advancedSectionToggle"
const val CAMERA_TAB_DEPTH_OF_FIELD_FIELD_TAG = "cameraTab.depthOfFieldField"
const val CAMERA_TAB_FOCUS_MODE_FIELD_TAG = "cameraTab.focusModeField"
const val CAMERA_TAB_STABILIZATION_FIELD_TAG = "cameraTab.stabilizationField"
const val CAMERA_TAB_FRAMING_FIELD_TAG = "cameraTab.framingField"
const val CAMERA_TAB_REFERENCE_TYPE_FIELD_TAG = "cameraTab.referenceTypeField"
const val CAMERA_TAB_REFERENCE_DESCRIPTION_FIELD_TAG = "cameraTab.referenceDescriptionField"
const val CAMERA_TAB_ADD_REFERENCE_BUTTON_TAG = "cameraTab.addReferenceButton"
/** اتصال Rule یتیم validateCameraMovementDuration — قدم ۱ از ۲ (ADR-129). */
const val CAMERA_TAB_MOVEMENT_DURATION_FIELD_TAG = "cameraTab.movementDurationField"

fun cameraTabReferenceChipTag(index: Int): String = "cameraTab.referenceChip.$index"

private val referenceTypes = listOf("character", "style", "composition")

private fun referenceTypeLabel(type: String, language: Language): String = uiString(
    when (type) {
        "character" -> "cameraTab.referenceType.character"
        "style" -> "cameraTab.referenceType.style"
        else -> "cameraTab.referenceType.composition"
    },
    language
)

@Composable
fun CameraTabContent(viewModel: ShotComposerViewModel, language: Language, modifier: Modifier = Modifier) {
    val cameraSource by viewModel.cameraSource.collectAsStateWithLifecycle()
    val angle by viewModel.cameraAngle.collectAsStateWithLifecycle()
    val distance by viewModel.cameraDistance.collectAsStateWithLifecycle()
    val lensType by viewModel.lensType.collectAsStateWithLifecycle()
    val movement by viewModel.cameraMovement.collectAsStateWithLifecycle()
    val depthOfField by viewModel.depthOfField.collectAsStateWithLifecycle()
    val focusMode by viewModel.focusMode.collectAsStateWithLifecycle()
    val stabilization by viewModel.stabilization.collectAsStateWithLifecycle()
    val framing by viewModel.framing.collectAsStateWithLifecycle()
    val imageReferences by viewModel.imageReferences.collectAsStateWithLifecycle()
    val validationIssues by viewModel.cameraValidationIssues.collectAsStateWithLifecycle()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpaqueChip(
                label = uiString("cameraTab.sourceSceneLabel", language),
                selected = cameraSource != "override",
                onClick = { viewModel.setCameraSource("scene") },
                testTag = CAMERA_TAB_SOURCE_SCENE_TAG
            )
            OpaqueChip(
                label = uiString("cameraTab.sourceOverrideLabel", language),
                selected = cameraSource == "override",
                onClick = { viewModel.setCameraSource("override") },
                testTag = CAMERA_TAB_SOURCE_OVERRIDE_TAG
            )
        }

        if (cameraSource != "override") {
            Text(
                text = uiString("cameraTab.sourceSceneDescription", language),
                style = MaterialTheme.typography.bodyMedium,
                color = CinemaTheme.extendedColors.fg3
            )
            return@Column
        }

        AssetFormEnumDropdownField(
            label = uiString("cameraTab.angleLabel", language),
            selectedLabel = cameraAngleLabel(angle, language),
            testTag = CAMERA_TAB_ANGLE_FIELD_TAG
        ) { onDismiss ->
            AssetFormFlatEntries(CameraAngle.entries, { cameraAngleLabel(it, language) }) { viewModel.setCameraAngle(it); onDismiss() }
        }
        AssetFormEnumDropdownField(
            label = uiString("cameraTab.distanceLabel", language),
            selectedLabel = cameraDistanceLabel(distance, language),
            testTag = CAMERA_TAB_DISTANCE_FIELD_TAG
        ) { onDismiss ->
            AssetFormFlatEntries(CameraDistance.entries, { cameraDistanceLabel(it, language) }) { viewModel.setCameraDistance(it); onDismiss() }
        }
        AssetFormEnumDropdownField(
            label = uiString("cameraTab.lensTypeLabel", language),
            selectedLabel = lensTypeLabel(lensType, language),
            testTag = CAMERA_TAB_LENS_TYPE_FIELD_TAG
        ) { onDismiss ->
            AssetFormFlatEntries(LensType.entries, { lensTypeLabel(it, language) }) { viewModel.setLensType(it); onDismiss() }
        }

        validationIssues.forEach { issue -> AssetFormValidationIssueRow(issue) }

        MovementSection(viewModel = viewModel, movement = movement, language = language)

        AdvancedSection(
            viewModel = viewModel,
            depthOfField = depthOfField,
            focusMode = focusMode,
            stabilization = stabilization,
            framing = framing,
            language = language
        )

        AttachedReferencesSection(viewModel = viewModel, imageReferences = imageReferences, language = language)
    }
}

@Composable
private fun MovementSection(viewModel: ShotComposerViewModel, movement: CameraMovement, language: Language) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = uiString("cameraTab.movementSectionTitle", language), style = MaterialTheme.typography.titleSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpaqueChip(
                label = uiString("cameraTab.movementTier.basic", language),
                selected = movement.tier() == CameraMovementTier.BASIC,
                onClick = { viewModel.setMovementTier(CameraMovementTier.BASIC) },
                testTag = CAMERA_TAB_MOVEMENT_TIER_BASIC_TAG
            )
            OpaqueChip(
                label = uiString("cameraTab.movementTier.advanced", language),
                selected = movement.tier() == CameraMovementTier.ADVANCED,
                onClick = { viewModel.setMovementTier(CameraMovementTier.ADVANCED) },
                testTag = CAMERA_TAB_MOVEMENT_TIER_ADVANCED_TAG
            )
        }

        // اتصال Rule یتیم validateCameraMovementDuration — قدم ۱ از ۲ (ADR-129):
        // مستقل از نوع Variant انتخابی (سطح CameraSettings، نه CameraMovement)،
        // پس اینجا (بعد از انتخاب Tier، قبل از جزئیات خاص هر Variant) رندر
        // می‌شود، نه داخل هرکدام از شاخه‌های when زیر.
        val movementDurationSecondsText by viewModel.movementDurationSecondsText.collectAsStateWithLifecycle()
        OutlinedTextField(
            value = movementDurationSecondsText,
            onValueChange = viewModel::setMovementDurationSecondsText,
            label = { Text(uiString("cameraTab.movementDurationLabel", language)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(CAMERA_TAB_MOVEMENT_DURATION_FIELD_TAG)
        )
        Text(
            text = uiString("cameraTab.movementDurationHint", language),
            style = MaterialTheme.typography.labelSmall,
            color = CinemaTheme.extendedColors.fg3
        )

        when (movement) {
            is CameraMovement.Basic -> {
                AssetFormEnumDropdownField(
                    label = uiString("cameraTab.basicMovementTypeLabel", language),
                    selectedLabel = basicMovementTypeLabel(movement.type, language),
                    testTag = CAMERA_TAB_BASIC_MOVEMENT_TYPE_FIELD_TAG
                ) { onDismiss ->
                    AssetFormFlatEntries(BasicMovementType.entries, { basicMovementTypeLabel(it, language) }) { viewModel.setBasicMovementType(it); onDismiss() }
                }
                OutlinedTextField(
                    value = movement.speed,
                    onValueChange = viewModel::setBasicSpeed,
                    label = { Text(uiString("cameraTab.basicSpeedLabel", language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(CAMERA_TAB_BASIC_SPEED_FIELD_TAG)
                )
            }

            else -> {
                val advancedType = movement.advancedTypeOrNull()
                AssetFormEnumDropdownField(
                    label = uiString("cameraTab.advancedMovementTypeLabel", language),
                    selectedLabel = advancedType?.let { advancedMovementTypeLabel(it, language) }.orEmpty(),
                    testTag = CAMERA_TAB_ADVANCED_MOVEMENT_TYPE_FIELD_TAG
                ) { onDismiss ->
                    AssetFormFlatEntries(AdvancedMovementType.entries, { advancedMovementTypeLabel(it, language) }) { viewModel.setAdvancedMovementType(it); onDismiss() }
                }

                when (movement) {
                    is CameraMovement.Orbit -> {
                        AssetFormEnumDropdownField(
                            label = uiString("cameraTab.orbitDegreesLabel", language),
                            selectedLabel = "${movement.degrees}°",
                            testTag = CAMERA_TAB_ORBIT_DEGREES_FIELD_TAG
                        ) { onDismiss ->
                            AssetFormFlatEntries(listOf(90, 180, 360), { "$it°" }) { viewModel.setOrbitDegrees(it); onDismiss() }
                        }
                        OutlinedTextField(
                            value = movement.speed,
                            onValueChange = viewModel::setOrbitSpeed,
                            label = { Text(uiString("cameraTab.orbitSpeedLabel", language)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag(CAMERA_TAB_ORBIT_SPEED_FIELD_TAG)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(uiString("cameraTab.orbitMaintainEyeLevelLabel", language), modifier = Modifier.weight(1f))
                            Switch(
                                checked = movement.maintainEyeLevel,
                                onCheckedChange = viewModel::setOrbitMaintainEyeLevel,
                                modifier = Modifier.testTag(CAMERA_TAB_ORBIT_MAINTAIN_EYE_LEVEL_TAG)
                            )
                        }
                    }

                    is CameraMovement.DronePath -> {
                        AssetFormEnumDropdownField(
                            label = uiString("cameraTab.dronePathAltitudeChangeLabel", language),
                            selectedLabel = altitudeChangeLabel(movement.altitudeChange, language),
                            testTag = CAMERA_TAB_DRONE_ALTITUDE_FIELD_TAG
                        ) { onDismiss ->
                            AssetFormFlatEntries(listOf("ascending", "descending", "level"), { altitudeChangeLabel(it, language) }) { viewModel.setDronePathAltitudeChange(it); onDismiss() }
                        }
                        AssetFormEnumDropdownField(
                            label = uiString("cameraTab.dronePathTypeLabel", language),
                            selectedLabel = pathTypeLabel(movement.pathType, language),
                            testTag = CAMERA_TAB_DRONE_PATH_TYPE_FIELD_TAG
                        ) { onDismiss ->
                            AssetFormFlatEntries(listOf("straight", "curved", "spiral"), { pathTypeLabel(it, language) }) { viewModel.setDronePathType(it); onDismiss() }
                        }
                        OutlinedTextField(
                            value = movement.speed,
                            onValueChange = viewModel::setDronePathSpeed,
                            label = { Text(uiString("cameraTab.dronePathSpeedLabel", language)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag(CAMERA_TAB_DRONE_SPEED_FIELD_TAG)
                        )
                    }

                    is CameraMovement.DollyZoom -> {
                        OutlinedTextField(
                            value = movement.focalStart.toString(),
                            onValueChange = { text -> text.toIntOrNull()?.let(viewModel::setDollyZoomFocalStart) },
                            label = { Text(uiString("cameraTab.dollyZoomFocalStartLabel", language)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag(CAMERA_TAB_DOLLY_FOCAL_START_FIELD_TAG)
                        )
                        OutlinedTextField(
                            value = movement.focalEnd.toString(),
                            onValueChange = { text -> text.toIntOrNull()?.let(viewModel::setDollyZoomFocalEnd) },
                            label = { Text(uiString("cameraTab.dollyZoomFocalEndLabel", language)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag(CAMERA_TAB_DOLLY_FOCAL_END_FIELD_TAG)
                        )
                        AssetFormEnumDropdownField(
                            label = uiString("cameraTab.dollyZoomDirectionLabel", language),
                            selectedLabel = dollyDirectionLabel(movement.direction, language),
                            testTag = CAMERA_TAB_DOLLY_DIRECTION_FIELD_TAG
                        ) { onDismiss ->
                            AssetFormFlatEntries(listOf("in", "out"), { dollyDirectionLabel(it, language) }) { viewModel.setDollyZoomDirection(it); onDismiss() }
                        }
                    }

                    is CameraMovement.HandheldShake -> {
                        Text("${uiString("cameraTab.handheldShakeIntensityLabel", language)}: ${movement.intensity}")
                        Slider(
                            value = movement.intensity.toFloat(),
                            onValueChange = { viewModel.setHandheldShakeIntensity(it.roundToInt()) },
                            valueRange = 0f..10f,
                            steps = 9,
                            modifier = Modifier.fillMaxWidth().testTag(CAMERA_TAB_HANDHELD_INTENSITY_FIELD_TAG)
                        )
                        OutlinedTextField(
                            value = movement.frequency,
                            onValueChange = viewModel::setHandheldShakeFrequency,
                            label = { Text(uiString("cameraTab.handheldShakeFrequencyLabel", language)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag(CAMERA_TAB_HANDHELD_FREQUENCY_FIELD_TAG)
                        )
                    }

                    is CameraMovement.Compound -> {
                        OutlinedTextField(
                            value = movement.primary,
                            onValueChange = viewModel::setCompoundPrimary,
                            label = { Text(uiString("cameraTab.compoundPrimaryLabel", language)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag(CAMERA_TAB_COMPOUND_PRIMARY_FIELD_TAG)
                        )
                        OutlinedTextField(
                            value = movement.secondary,
                            onValueChange = viewModel::setCompoundSecondary,
                            label = { Text(uiString("cameraTab.compoundSecondaryLabel", language)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag(CAMERA_TAB_COMPOUND_SECONDARY_FIELD_TAG)
                        )
                        OutlinedTextField(
                            value = movement.sync,
                            onValueChange = viewModel::setCompoundSync,
                            label = { Text(uiString("cameraTab.compoundSyncLabel", language)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag(CAMERA_TAB_COMPOUND_SYNC_FIELD_TAG)
                        )
                    }

                    is CameraMovement.Basic -> Unit // این شاخه در Tier=ADVANCED هرگز رخ نمی‌دهد
                }
            }
        }
    }
}

private fun altitudeChangeLabel(value: String, language: Language): String = uiString(
    when (value) {
        "ascending" -> "cameraTab.altitudeChange.ascending"
        "descending" -> "cameraTab.altitudeChange.descending"
        else -> "cameraTab.altitudeChange.level"
    },
    language
)

private fun pathTypeLabel(value: String, language: Language): String = uiString(
    when (value) {
        "straight" -> "cameraTab.pathType.straight"
        "curved" -> "cameraTab.pathType.curved"
        else -> "cameraTab.pathType.spiral"
    },
    language
)

private fun dollyDirectionLabel(value: String, language: Language): String = uiString(
    if (value == "in") "cameraTab.dollyDirection.in" else "cameraTab.dollyDirection.out",
    language
)

/** طبق سند طراحی («an Advanced expandable section per tab where applicable (Camera, Lighting)»). */
@Composable
private fun AdvancedSection(
    viewModel: ShotComposerViewModel,
    depthOfField: DepthOfField,
    focusMode: FocusMode,
    stabilization: Stabilization,
    framing: Framing,
    language: Language
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { expanded = !expanded })
                .testTag(CAMERA_TAB_ADVANCED_SECTION_TOGGLE_TAG)
        ) {
            Text(uiString("cameraTab.advancedSectionToggle", language), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                AssetFormEnumDropdownField(
                    label = uiString("cameraTab.depthOfFieldLabel", language),
                    selectedLabel = depthOfFieldLabel(depthOfField, language),
                    testTag = CAMERA_TAB_DEPTH_OF_FIELD_FIELD_TAG
                ) { onDismiss ->
                    AssetFormFlatEntries(DepthOfField.entries, { depthOfFieldLabel(it, language) }) { viewModel.setDepthOfField(it); onDismiss() }
                }
                AssetFormEnumDropdownField(
                    label = uiString("cameraTab.focusModeLabel", language),
                    selectedLabel = focusModeLabel(focusMode, language),
                    testTag = CAMERA_TAB_FOCUS_MODE_FIELD_TAG
                ) { onDismiss ->
                    AssetFormFlatEntries(FocusMode.entries, { focusModeLabel(it, language) }) { viewModel.setFocusMode(it); onDismiss() }
                }
                AssetFormEnumDropdownField(
                    label = uiString("cameraTab.stabilizationLabel", language),
                    selectedLabel = stabilizationLabel(stabilization, language),
                    testTag = CAMERA_TAB_STABILIZATION_FIELD_TAG
                ) { onDismiss ->
                    AssetFormFlatEntries(Stabilization.entries, { stabilizationLabel(it, language) }) { viewModel.setStabilization(it); onDismiss() }
                }
                AssetFormEnumDropdownField(
                    label = uiString("cameraTab.framingLabel", language),
                    selectedLabel = framingLabel(framing, language),
                    testTag = CAMERA_TAB_FRAMING_FIELD_TAG
                ) { onDismiss ->
                    AssetFormFlatEntries(Framing.entries, { framingLabel(it, language) }) { viewModel.setFraming(it); onDismiss() }
                }
            }
        }
    }
}

/**
 * «Attached References» طبق سند طراحی («chip row character/style/composition»).
 * بدون مدیریت واقعی آپلود فایل — هیچ Infra انتخاب‌گر تصویر در کل کدبیس وجود ندارد
 * (تأییدشده با grep، خارج از Scope این قدم). فقط نوع + توضیح متنی.
 */
@Composable
private fun AttachedReferencesSection(
    viewModel: ShotComposerViewModel,
    imageReferences: List<com.operaboys.cinemashotgenerator.domain.shot.ImageReference>,
    language: Language
) {
    var selectedType by remember { mutableStateOf(referenceTypes.first()) }
    var description by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(uiString("cameraTab.attachedReferencesTitle", language), style = MaterialTheme.typography.titleSmall)

        if (imageReferences.isEmpty()) {
            Text(
                uiString("cameraTab.attachedReferencesEmpty", language),
                style = MaterialTheme.typography.bodySmall,
                color = CinemaTheme.extendedColors.fg3
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                imageReferences.forEachIndexed { index, reference ->
                    OpaqueChip(
                        label = "${referenceTypeLabel(reference.type, language)}: ${reference.description} ×",
                        selected = false,
                        onClick = { viewModel.removeImageReference(index) },
                        testTag = cameraTabReferenceChipTag(index)
                    )
                }
            }
        }

        AssetFormEnumDropdownField(
            label = uiString("cameraTab.referenceTypeLabel", language),
            selectedLabel = referenceTypeLabel(selectedType, language),
            testTag = CAMERA_TAB_REFERENCE_TYPE_FIELD_TAG
        ) { onDismiss ->
            AssetFormFlatEntries(referenceTypes, { referenceTypeLabel(it, language) }) { selectedType = it; onDismiss() }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(uiString("cameraTab.referenceDescriptionLabel", language)) },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag(CAMERA_TAB_REFERENCE_DESCRIPTION_FIELD_TAG)
            )
            IconButton(
                onClick = {
                    if (description.isNotBlank()) {
                        viewModel.addImageReference(selectedType, description.trim())
                        description = ""
                    }
                },
                modifier = Modifier.testTag(CAMERA_TAB_ADD_REFERENCE_BUTTON_TAG)
            ) {
                Icon(Icons.Filled.Add, contentDescription = uiString("cameraTab.addReferenceButton", language))
            }
        }
    }
}
