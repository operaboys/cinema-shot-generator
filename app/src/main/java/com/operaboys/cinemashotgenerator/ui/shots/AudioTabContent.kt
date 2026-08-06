package com.operaboys.cinemashotgenerator.ui.shots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.assets.OpaqueChip
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۴ — قدم ۴ (آخرین قدم): محتوای واقعی Tab «صدا» Shot Composer. منبع
// حقیقت دوگانه: docs/blueprints/08-scene-conditions.md («Environment-to-Sound
// Mapping») و docs/design/README.md بخش «۷. Shot Composer» («Ambient — auto from
// Weather ... manual only / never auto-generated (Rule 5) — only on explicit user
// action»). جزئیات کامل تصمیمات در
// docs/adr/053-unit16-phase4-step4-lighting-environment-sound.md.

const val SOUND_ENABLED_SWITCH_TAG = "soundTab.enabledSwitch"
const val SOUND_GENERATE_AMBIENT_BUTTON_TAG = "soundTab.generateAmbientButton"
const val SOUND_ACTION_TIMESTAMP_FIELD_TAG = "soundTab.actionTimestampField"
const val SOUND_ACTION_TYPE_FIELD_TAG = "soundTab.actionTypeField"
const val SOUND_ACTION_DESCRIPTION_FIELD_TAG = "soundTab.actionDescriptionField"
const val SOUND_ADD_ACTION_BUTTON_TAG = "soundTab.addActionButton"
const val SOUND_CHARACTER_ID_FIELD_TAG = "soundTab.characterIdField"
const val SOUND_CHARACTER_TYPE_FIELD_TAG = "soundTab.characterTypeField"
const val SOUND_CHARACTER_DESCRIPTION_FIELD_TAG = "soundTab.characterDescriptionField"
const val SOUND_ADD_CHARACTER_BUTTON_TAG = "soundTab.addCharacterButton"

fun ambientSoundChipTag(index: Int): String = "soundTab.ambientChip.$index"
fun actionSoundChipTag(index: Int): String = "soundTab.actionChip.$index"
fun characterSoundChipTag(index: Int): String = "soundTab.characterChip.$index"

@Composable
fun AudioTabContent(viewModel: ShotComposerViewModel, language: Language, modifier: Modifier = Modifier) {
    val enabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val ambientSounds by viewModel.ambientSounds.collectAsStateWithLifecycle()
    val actionSounds by viewModel.actionSounds.collectAsStateWithLifecycle()
    val characterSounds by viewModel.characterSounds.collectAsStateWithLifecycle()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(uiString("soundTab.enabledLabel", language), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = viewModel::setSoundEnabled, modifier = Modifier.testTag(SOUND_ENABLED_SWITCH_TAG))
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(uiString("soundTab.ambientSectionTitle", language), style = MaterialTheme.typography.titleSmall)
            Text(
                text = uiString("soundTab.ambientInfoText", language),
                style = MaterialTheme.typography.bodySmall,
                color = CinemaTheme.extendedColors.fg3
            )
            if (ambientSounds.isEmpty()) {
                Text(
                    uiString("soundTab.ambientEmptyState", language),
                    style = MaterialTheme.typography.bodySmall,
                    color = CinemaTheme.extendedColors.fg3
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ambientSounds.forEachIndexed { index, sound ->
                        OpaqueChip(
                            label = "${sound.type} (${sound.intensity}): ${sound.description} ×",
                            selected = false,
                            onClick = { viewModel.removeAmbientSound(index) },
                            testTag = ambientSoundChipTag(index)
                        )
                    }
                }
            }
            Row {
                IconButton(
                    onClick = viewModel::generateAmbientSounds,
                    modifier = Modifier.testTag(SOUND_GENERATE_AMBIENT_BUTTON_TAG)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = uiString("soundTab.generateAmbientButton", language))
                }
                Text(
                    uiString("soundTab.generateAmbientButton", language),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }

        HorizontalDivider()

        ActionSoundsSection(viewModel, language, actionSounds)

        HorizontalDivider()

        CharacterSoundsSection(viewModel, language, characterSounds)
    }
}

@Composable
private fun ActionSoundsSection(
    viewModel: ShotComposerViewModel,
    language: Language,
    actionSounds: List<com.operaboys.cinemashotgenerator.domain.shot.ActionSound>
) {
    var timestampText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(uiString("soundTab.actionSoundsSectionTitle", language), style = MaterialTheme.typography.titleSmall)
        if (actionSounds.isEmpty()) {
            Text(
                uiString("soundTab.actionSoundsEmptyState", language),
                style = MaterialTheme.typography.bodySmall,
                color = CinemaTheme.extendedColors.fg3
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actionSounds.forEachIndexed { index, sound ->
                    OpaqueChip(
                        label = "${sound.type} @ ${sound.timestampSeconds}s: ${sound.description} ×",
                        selected = false,
                        onClick = { viewModel.removeActionSound(index) },
                        testTag = actionSoundChipTag(index)
                    )
                }
            }
        }
        OutlinedTextField(
            value = timestampText,
            onValueChange = { timestampText = it },
            label = { Text(uiString("soundTab.actionSoundTimestampLabel", language)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(SOUND_ACTION_TIMESTAMP_FIELD_TAG)
        )
        OutlinedTextField(
            value = type,
            onValueChange = { type = it },
            label = { Text(uiString("soundTab.actionSoundTypeLabel", language)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(SOUND_ACTION_TYPE_FIELD_TAG)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(uiString("soundTab.actionSoundDescriptionLabel", language)) },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag(SOUND_ACTION_DESCRIPTION_FIELD_TAG)
            )
            IconButton(
                onClick = {
                    val timestamp = timestampText.toFloatOrNull()
                    if (timestamp != null && type.isNotBlank() && description.isNotBlank()) {
                        viewModel.addActionSound(timestamp, type.trim(), description.trim())
                        timestampText = ""
                        type = ""
                        description = ""
                    }
                },
                modifier = Modifier.testTag(SOUND_ADD_ACTION_BUTTON_TAG)
            ) {
                Icon(Icons.Filled.Add, contentDescription = uiString("soundTab.addActionSoundButton", language))
            }
        }
    }
}

@Composable
private fun CharacterSoundsSection(
    viewModel: ShotComposerViewModel,
    language: Language,
    characterSounds: List<com.operaboys.cinemashotgenerator.domain.shot.CharacterSound>
) {
    var characterId by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(uiString("soundTab.characterSoundsSectionTitle", language), style = MaterialTheme.typography.titleSmall)
        if (characterSounds.isEmpty()) {
            Text(
                uiString("soundTab.characterSoundsEmptyState", language),
                style = MaterialTheme.typography.bodySmall,
                color = CinemaTheme.extendedColors.fg3
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                characterSounds.forEachIndexed { index, sound ->
                    OpaqueChip(
                        label = "${sound.characterId} — ${sound.type}: ${sound.description} ×",
                        selected = false,
                        onClick = { viewModel.removeCharacterSound(index) },
                        testTag = characterSoundChipTag(index)
                    )
                }
            }
        }
        OutlinedTextField(
            value = characterId,
            onValueChange = { characterId = it },
            label = { Text(uiString("soundTab.characterSoundCharacterIdLabel", language)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(SOUND_CHARACTER_ID_FIELD_TAG)
        )
        OutlinedTextField(
            value = type,
            onValueChange = { type = it },
            label = { Text(uiString("soundTab.characterSoundTypeLabel", language)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(SOUND_CHARACTER_TYPE_FIELD_TAG)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(uiString("soundTab.characterSoundDescriptionLabel", language)) },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag(SOUND_CHARACTER_DESCRIPTION_FIELD_TAG)
            )
            IconButton(
                onClick = {
                    if (characterId.isNotBlank() && type.isNotBlank() && description.isNotBlank()) {
                        viewModel.addCharacterSound(characterId.trim(), type.trim(), description.trim())
                        characterId = ""
                        type = ""
                        description = ""
                    }
                },
                modifier = Modifier.testTag(SOUND_ADD_CHARACTER_BUTTON_TAG)
            ) {
                Icon(Icons.Filled.Add, contentDescription = uiString("soundTab.addCharacterSoundButton", language))
            }
        }
    }
}
