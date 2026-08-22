package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.Outfit
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import com.operaboys.cinemashotgenerator.domain.storybreakdown.BUILTIN_AI_CONNECTOR_PROFILES
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import com.operaboys.cinemashotgenerator.ui.common.RetranslateButton
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.scenes.sceneLocationTypeLabel
import com.operaboys.cinemashotgenerator.ui.scenes.timeOfDayLabel
import com.operaboys.cinemashotgenerator.ui.shots.weatherTypeLabel
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۳ — قدم ۲ — بخش الف: صفحه‌ی فرم ساخت Character. طبق
// docs/blueprints/16-user-workflow-v2.md «مرحله ۳» + docs/design/README.md بخش
// «۸. Assets Library». جزئیات کامل تصمیمات در
// docs/adr/049-unit16-phase3-step2-asset-forms.md.
//
// رفع G5 (ADR-067 بخش ه، ADR-095): بخش Outfit اکنون یک لیست واقعی است (نه
// تصمیم F5 قدیمی «فقط یک Outfit پیش‌فرض ساده»)، هم‌الگو با ActionSoundsSection
// در ui/shots/AudioTabContent.kt.

const val CHARACTER_FORM_NAME_FIELD_TAG = "characterForm.nameField"
const val CHARACTER_FORM_TIER_FIELD_TAG = "characterForm.tierField"
const val CHARACTER_FORM_AGE_RANGE_FIELD_TAG = "characterForm.ageRangeField"
const val CHARACTER_FORM_GENDER_FIELD_TAG = "characterForm.genderField"
/** سیستم Preview دوزبانه‌ی پرامپت — قدم ۳ از ۳ زیرقدم، پایانی (ADR-123). */
const val CHARACTER_FORM_DESCRIPTION_FA_PREVIEW_FIELD_TAG = "characterForm.descriptionFaPreviewField"
const val CHARACTER_FORM_SAVE_BUTTON_TAG = "characterForm.saveButton"
const val CHARACTER_FORM_BACK_BUTTON_TAG = "characterForm.backButton"
const val CHARACTER_FORM_TOGGLE_LANGUAGE_BUTTON_TAG = "characterForm.toggleLanguageButton"
const val CHARACTER_FORM_TOGGLE_THEME_BUTTON_TAG = "characterForm.toggleThemeButton"

// رفع G5 (ADR-067 بخش ه، ADR-095): UI مدیریت کامل چند-Outfit — هم‌الگو با
// SOUND_ACTION_*_FIELD_TAG/actionSoundChipTag در ui/shots/AudioTabContent.kt
// (ActionSoundsSection).
const val CHARACTER_FORM_OUTFIT_NAME_FIELD_TAG = "characterForm.outfitNameField"
const val CHARACTER_FORM_OUTFIT_DESCRIPTION_FIELD_TAG = "characterForm.outfitDescriptionField"
const val CHARACTER_FORM_ADD_OUTFIT_BUTTON_TAG = "characterForm.addOutfitButton"
fun outfitRowTag(index: Int): String = "characterForm.outfitRow.$index"
fun outfitSetDefaultButtonTag(index: Int): String = "characterForm.outfitSetDefaultButton.$index"
fun outfitRemoveButtonTag(index: Int): String = "characterForm.outfitRemoveButton.$index"
fun outfitConditionWeatherFieldTag(index: Int): String = "characterForm.outfitConditionWeatherField.$index"
fun outfitConditionTimeOfDayFieldTag(index: Int): String = "characterForm.outfitConditionTimeOfDayField.$index"
fun outfitConditionLocationTypeFieldTag(index: Int): String = "characterForm.outfitConditionLocationTypeField.$index"

@Composable
fun CharacterAssetFormScreen(
    language: Language,
    theme: AppTheme,
    // رفع G6 ممیزی post-Unit16 (docs/adr/062-...): دیگر PLACEHOLDER_ACTIVE_PROJECT_ID
    // نیست — از AppNavHost.kt (resolveActiveOrRecentProjectId) تزریق می‌شود.
    projectId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onToggleLanguage: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    assetRepository: AssetRepository? = null,
    existingAssetId: String? = null,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: CharacterAssetFormViewModel = viewModel(
        factory = CharacterAssetFormViewModel.factory(application, projectId, assetRepository, existingAssetId)
    )

    val name by viewModel.name.collectAsStateWithLifecycle()
    val tier by viewModel.tier.collectAsStateWithLifecycle()
    val continuityLockLevel by viewModel.continuityLockLevel.collectAsStateWithLifecycle()
    val ageRange by viewModel.ageRange.collectAsStateWithLifecycle()
    val gender by viewModel.gender.collectAsStateWithLifecycle()
    val height by viewModel.height.collectAsStateWithLifecycle()
    val build by viewModel.build.collectAsStateWithLifecycle()
    val hairColor by viewModel.hairColor.collectAsStateWithLifecycle()
    val hairStyle by viewModel.hairStyle.collectAsStateWithLifecycle()
    val hairLength by viewModel.hairLength.collectAsStateWithLifecycle()
    val facialEyes by viewModel.facialEyes.collectAsStateWithLifecycle()
    val facialDistinctiveMarks by viewModel.facialDistinctiveMarks.collectAsStateWithLifecycle()
    val physicalFeatures by viewModel.physicalFeatures.collectAsStateWithLifecycle()
    val defaultMood by viewModel.defaultMood.collectAsStateWithLifecycle()
    val basePrompt by viewModel.basePrompt.collectAsStateWithLifecycle()
    val descriptionFaPreview by viewModel.descriptionFaPreview.collectAsStateWithLifecycle()
    val selectedTranslationProfileId by viewModel.selectedTranslationProfileId.collectAsStateWithLifecycle()
    val apiKeySavedForTranslationProfile by viewModel.apiKeySavedForTranslationProfile.collectAsStateWithLifecycle()
    val translationInProgress by viewModel.translationInProgress.collectAsStateWithLifecycle()
    val outfits by viewModel.outfits.collectAsStateWithLifecycle()
    val validationIssues by viewModel.validationIssues.collectAsStateWithLifecycle()
    val canSave by viewModel.canSave.collectAsStateWithLifecycle()
    val saveCompleted by viewModel.saveCompleted.collectAsStateWithLifecycle()

    LaunchedEffect(saveCompleted) {
        if (saveCompleted) onSaved()
    }

    Column(modifier = modifier.fillMaxSize()) {
        AssetFormHeader(
            title = uiString("characterForm.title", language),
            subtitle = uiString("characterForm.subtitle", language),
            onBack = onBack,
            backTestTag = CHARACTER_FORM_BACK_BUTTON_TAG,
            language = language,
            theme = theme,
            onToggleLanguage = onToggleLanguage,
            onToggleTheme = onToggleTheme,
            toggleLanguageTestTag = CHARACTER_FORM_TOGGLE_LANGUAGE_BUTTON_TAG,
            toggleThemeTestTag = CHARACTER_FORM_TOGGLE_THEME_BUTTON_TAG
        )

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = viewModel::setName,
                label = { Text(uiString("assetForm.nameLabel", language)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(CHARACTER_FORM_NAME_FIELD_TAG)
            )
            AssetFormEnumDropdownField(
                label = uiString("characterForm.tierLabel", language),
                selectedLabel = characterTierLabel(tier, language),
                testTag = CHARACTER_FORM_TIER_FIELD_TAG
            ) { onDismiss ->
                AssetFormFlatEntries(CharacterTier.entries, { characterTierLabel(it, language) }) { viewModel.setTier(it); onDismiss() }
            }
            Text(
                text = uiTemplate("assetForm.continuityLockLevelFixedTemplate", language, "level" to characterContinuityLevelLabel(continuityLockLevel, language)),
                style = MaterialTheme.typography.labelMedium
            )
            OutlinedTextField(
                value = ageRange, onValueChange = viewModel::setAgeRange,
                label = { Text(uiString("characterForm.ageRangeLabel", language)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(CHARACTER_FORM_AGE_RANGE_FIELD_TAG)
            )
            AssetFormEnumDropdownField(
                label = uiString("characterForm.genderLabel", language),
                selectedLabel = genderLabel(gender, language),
                testTag = CHARACTER_FORM_GENDER_FIELD_TAG
            ) { onDismiss ->
                AssetFormFlatEntries(Gender.entries, { genderLabel(it, language) }) { viewModel.setGender(it); onDismiss() }
            }
            OutlinedTextField(
                value = height, onValueChange = viewModel::setHeight,
                label = { Text(uiString("characterForm.heightLabel", language)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = build, onValueChange = viewModel::setBuild,
                label = { Text(uiString("characterForm.buildLabel", language)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            Text(text = uiString("characterForm.hairSectionTitle", language), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = hairColor, onValueChange = viewModel::setHairColor, label = { Text(uiString("characterForm.hairColorLabel", language)) }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = hairStyle, onValueChange = viewModel::setHairStyle, label = { Text(uiString("characterForm.hairStyleLabel", language)) }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = hairLength, onValueChange = viewModel::setHairLength, label = { Text(uiString("characterForm.hairLengthLabel", language)) }, singleLine = true, modifier = Modifier.weight(1f))
            }

            Text(text = uiString("characterForm.facialFeaturesSectionTitle", language), style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = facialEyes, onValueChange = viewModel::setFacialEyes,
                label = { Text(uiString("characterForm.eyesLabel", language)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = facialDistinctiveMarks, onValueChange = viewModel::setFacialDistinctiveMarks,
                label = { Text(uiString("characterForm.distinctiveMarksLabel", language)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = physicalFeatures, onValueChange = viewModel::setPhysicalFeatures,
                label = { Text(uiString("characterForm.physicalFeaturesLabel", language)) }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = defaultMood, onValueChange = viewModel::setDefaultMood,
                label = { Text(uiString("characterForm.defaultMoodLabel", language)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = basePrompt, onValueChange = viewModel::setBasePrompt,
                label = { Text(uiString("assetForm.basePromptLabel", language)) }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = descriptionFaPreview, onValueChange = viewModel::setDescriptionFaPreview,
                label = { Text(uiString("assetForm.descriptionFaPreviewLabel", language)) },
                modifier = Modifier.fillMaxWidth().testTag(CHARACTER_FORM_DESCRIPTION_FA_PREVIEW_FIELD_TAG)
            )
            RetranslateButton(
                profiles = BUILTIN_AI_CONNECTOR_PROFILES,
                selectedProfileId = selectedTranslationProfileId,
                onSelectProfile = viewModel::selectTranslationProfile,
                apiKeySaved = apiKeySavedForTranslationProfile,
                inProgress = translationInProgress,
                onRetranslate = viewModel::retranslate,
                language = language
            )

            OutfitsSection(viewModel = viewModel, language = language, outfits = outfits)

            validationIssues.forEach { AssetFormValidationIssueRow(it) }
        }

        Button(
            onClick = viewModel::save,
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag(CHARACTER_FORM_SAVE_BUTTON_TAG)
        ) {
            Text(uiString("assetForm.saveButton", language))
        }
    }
}

/**
 * رفع G5 (ADR-067 بخش ه، ADR-095) — هم‌الگو دقیق با ActionSoundsSection
 * (ui/shots/AudioTabContent.kt): empty-state وقتی لیست خالی است، ردیف‌های
 * موجود بالا، فرم «افزودن» ثابت پایین (State محلی، بعد از افزودن پاک می‌شود).
 * برخلاف Sound Chips (فقط نمایش+حذف)، هر ردیف Outfit تعامل بیشتری لازم دارد
 * (پیش‌فرض‌کردن، سه Dropdown شرط) — پس به‌جای OpaqueChip از یک Card ردیفی
 * استفاده شد (هم‌الگو با LinkedAssetCard در ui/scenes/SceneDetailScreen.kt).
 */
@Composable
private fun OutfitsSection(viewModel: CharacterAssetFormViewModel, language: Language, outfits: List<Outfit>) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = uiString("characterForm.outfitSectionTitle", language), style = MaterialTheme.typography.titleSmall)
        if (outfits.isEmpty()) {
            Text(
                uiString("characterForm.outfitEmptyState", language),
                style = MaterialTheme.typography.bodySmall,
                color = CinemaTheme.extendedColors.fg3
            )
        } else {
            outfits.forEachIndexed { index, outfit ->
                OutfitRow(
                    outfit = outfit,
                    index = index,
                    language = language,
                    canRemove = outfits.size > 1,
                    onSetDefault = { viewModel.setOutfitAsDefault(index) },
                    onRemove = { viewModel.removeOutfit(index) },
                    onWeatherChange = { viewModel.setOutfitConditionWeather(index, it) },
                    onTimeOfDayChange = { viewModel.setOutfitConditionTimeOfDay(index, it) },
                    onLocationTypeChange = { viewModel.setOutfitConditionLocationType(index, it) }
                )
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(uiString("characterForm.outfitNameLabel", language)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(CHARACTER_FORM_OUTFIT_NAME_FIELD_TAG)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(uiString("characterForm.outfitDescriptionLabel", language)) },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag(CHARACTER_FORM_OUTFIT_DESCRIPTION_FIELD_TAG)
            )
            IconButton(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addOutfit(name.trim(), description.trim())
                        name = ""
                        description = ""
                    }
                },
                modifier = Modifier.testTag(CHARACTER_FORM_ADD_OUTFIT_BUTTON_TAG)
            ) {
                Icon(Icons.Filled.Add, contentDescription = uiString("characterForm.addOutfitButton", language))
            }
        }
    }
}

@Composable
private fun OutfitRow(
    outfit: Outfit,
    index: Int,
    language: Language,
    canRemove: Boolean,
    onSetDefault: () -> Unit,
    onRemove: () -> Unit,
    onWeatherChange: (WeatherType?) -> Unit,
    onTimeOfDayChange: (TimeOfDay?) -> Unit,
    onLocationTypeChange: (LocationType?) -> Unit
) {
    val notSetLabel = uiString("shotComposer.notSet", language)
    // condition.weather/timeOfDay/locationType به‌صورت name.lowercase() ذخیره
    // می‌شوند (CharacterAssetFormViewModel) — بازخوانی با equals(ignoreCase) نه
    // valueOf، تا داده‌ی قدیمی/غیرمنتظره باعث Crash نشود، فقط «تنظیم‌نشده» نمایش
    // داده می‌شود.
    val selectedWeather = outfit.condition?.weather?.let { stored -> WeatherType.entries.firstOrNull { it.name.equals(stored, ignoreCase = true) } }
    val selectedTimeOfDay = outfit.condition?.timeOfDay?.let { stored -> TimeOfDay.entries.firstOrNull { it.name.equals(stored, ignoreCase = true) } }
    val selectedLocationType = outfit.condition?.locationType?.let { stored -> LocationType.entries.firstOrNull { it.name.equals(stored, ignoreCase = true) } }

    Card(modifier = Modifier.fillMaxWidth().testTag(outfitRowTag(index))) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = outfit.name, style = MaterialTheme.typography.bodyLarge)
                        if (outfit.isDefault) {
                            Text(
                                text = uiString("characterForm.outfitDefaultBadge", language),
                                style = MaterialTheme.typography.labelSmall,
                                color = CinemaTheme.extendedColors.fg3
                            )
                        }
                    }
                    if (outfit.description.isNotBlank()) {
                        Text(text = outfit.description, style = MaterialTheme.typography.bodySmall, color = CinemaTheme.extendedColors.fg3)
                    }
                }
                if (!outfit.isDefault) {
                    TextButton(onClick = onSetDefault, modifier = Modifier.testTag(outfitSetDefaultButtonTag(index))) {
                        Text(uiString("characterForm.outfitSetDefaultButton", language))
                    }
                }
                IconButton(onClick = onRemove, enabled = canRemove, modifier = Modifier.testTag(outfitRemoveButtonTag(index))) {
                    Icon(Icons.Filled.Close, contentDescription = uiString("characterForm.outfitRemoveButton", language))
                }
            }

            NullableEnumDropdownField(
                label = uiString("characterForm.outfitConditionWeatherLabel", language),
                selectedLabel = selectedWeather?.let { weatherTypeLabel(it, language) } ?: notSetLabel,
                testTag = outfitConditionWeatherFieldTag(index),
                entries = WeatherType.entries,
                entryLabel = { weatherTypeLabel(it, language) },
                notSetLabel = notSetLabel,
                onSelect = onWeatherChange
            )
            NullableEnumDropdownField(
                label = uiString("characterForm.outfitConditionTimeOfDayLabel", language),
                selectedLabel = selectedTimeOfDay?.let { timeOfDayLabel(it, language) } ?: notSetLabel,
                testTag = outfitConditionTimeOfDayFieldTag(index),
                entries = TimeOfDay.entries,
                entryLabel = { timeOfDayLabel(it, language) },
                notSetLabel = notSetLabel,
                onSelect = onTimeOfDayChange
            )
            NullableEnumDropdownField(
                label = uiString("characterForm.outfitConditionLocationTypeLabel", language),
                selectedLabel = selectedLocationType?.let { sceneLocationTypeLabel(it, language) } ?: notSetLabel,
                testTag = outfitConditionLocationTypeFieldTag(index),
                entries = LocationType.entries,
                entryLabel = { sceneLocationTypeLabel(it, language) },
                notSetLabel = notSetLabel,
                onSelect = onLocationTypeChange
            )
        }
    }
}

/**
 * دقیقاً همان الگوی `NullableEnumDropdownField` (private) در
 * ui/shots/LightingEnvironmentTabContent.kt — یک گزینه‌ی «تنظیم‌نشده» (معادل
 * null) همیشه اول فهرست. آن نسخه private است (قابل import از این پکیج نیست)؛
 * تکرار محلی همان تصمیم طراحی خودِ آن فایل است (کوچک، تک‌فایلی، به‌جای
 * Promote کردن یک composable عمومی جدید برای یک مصرف‌کننده‌ی دوم).
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
