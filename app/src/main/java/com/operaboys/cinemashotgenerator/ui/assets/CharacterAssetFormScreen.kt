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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate

// واحد ۱۶ فاز ۳ — قدم ۲ — بخش الف: صفحه‌ی فرم ساخت Character. طبق
// docs/blueprints/16-user-workflow-v2.md «مرحله ۳» + docs/design/README.md بخش
// «۸. Assets Library» + تصمیم F5 (ممیزی pre-unit16). جزئیات کامل تصمیمات در
// docs/adr/049-unit16-phase3-step2-asset-forms.md.

const val CHARACTER_FORM_NAME_FIELD_TAG = "characterForm.nameField"
const val CHARACTER_FORM_TIER_FIELD_TAG = "characterForm.tierField"
const val CHARACTER_FORM_AGE_RANGE_FIELD_TAG = "characterForm.ageRangeField"
const val CHARACTER_FORM_GENDER_FIELD_TAG = "characterForm.genderField"
const val CHARACTER_FORM_SAVE_BUTTON_TAG = "characterForm.saveButton"
const val CHARACTER_FORM_BACK_BUTTON_TAG = "characterForm.backButton"
const val CHARACTER_FORM_TOGGLE_LANGUAGE_BUTTON_TAG = "characterForm.toggleLanguageButton"
const val CHARACTER_FORM_TOGGLE_THEME_BUTTON_TAG = "characterForm.toggleThemeButton"

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
    val outfitName by viewModel.outfitName.collectAsStateWithLifecycle()
    val outfitDescription by viewModel.outfitDescription.collectAsStateWithLifecycle()
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

            Text(text = uiString("characterForm.outfitSectionTitle", language), style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = outfitName, onValueChange = viewModel::setOutfitName,
                label = { Text(uiString("characterForm.outfitNameLabel", language)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = outfitDescription, onValueChange = viewModel::setOutfitDescription,
                label = { Text(uiString("characterForm.outfitDescriptionLabel", language)) }, modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { onShowMessage(uiString("characterForm.manageOutfitsComingSoon", language)) }) {
                Text(uiString("characterForm.manageOutfits", language))
            }

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
