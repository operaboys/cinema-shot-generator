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
import com.operaboys.cinemashotgenerator.domain.asset.LocationType
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate

// واحد ۱۶ فاز ۳ — قدم ۲ — بخش ب: صفحه‌ی فرم ساخت Location. طبق
// docs/blueprints/16-user-workflow-v2.md «مرحله ۳» + docs/design/README.md بخش
// «۸. Assets Library» + تصمیم F6 (ممیزی pre-unit16، تعدیل‌شده طبق یافته‌ی واقعی
// grep — timeCompatibility/weatherCompatibility/keyElements از نوع List<String>
// هستند، نه List<Enum>). جزئیات کامل تصمیمات در
// docs/adr/049-unit16-phase3-step2-asset-forms.md.

const val LOCATION_FORM_NAME_FIELD_TAG = "locationForm.nameField"
const val LOCATION_FORM_DESCRIPTION_FIELD_TAG = "locationForm.descriptionField"
const val LOCATION_FORM_LOCATION_TYPE_FIELD_TAG = "locationForm.locationTypeField"
const val LOCATION_FORM_TIME_TAG_INPUT_TAG = "locationForm.timeCompatibilityInput"
const val LOCATION_FORM_WEATHER_TAG_INPUT_TAG = "locationForm.weatherCompatibilityInput"
const val LOCATION_FORM_KEY_ELEMENT_INPUT_TAG = "locationForm.keyElementsInput"
const val LOCATION_FORM_SAVE_BUTTON_TAG = "locationForm.saveButton"
const val LOCATION_FORM_BACK_BUTTON_TAG = "locationForm.backButton"
const val LOCATION_FORM_TOGGLE_LANGUAGE_BUTTON_TAG = "locationForm.toggleLanguageButton"
const val LOCATION_FORM_TOGGLE_THEME_BUTTON_TAG = "locationForm.toggleThemeButton"

@Composable
fun LocationAssetFormScreen(
    language: Language,
    theme: AppTheme,
    // رفع G6 ممیزی post-Unit16 (docs/adr/062-...): دیگر PLACEHOLDER_ACTIVE_PROJECT_ID
    // نیست — از AppNavHost.kt (resolveActiveOrRecentProjectId) تزریق می‌شود.
    projectId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onToggleLanguage: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    assetRepository: AssetRepository? = null,
    existingAssetId: String? = null,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: LocationAssetFormViewModel = viewModel(
        factory = LocationAssetFormViewModel.factory(application, projectId, assetRepository, existingAssetId)
    )

    val name by viewModel.name.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val locationType by viewModel.locationType.collectAsStateWithLifecycle()
    val environmentType by viewModel.environmentType.collectAsStateWithLifecycle()
    val environmentSize by viewModel.environmentSize.collectAsStateWithLifecycle()
    val environmentLighting by viewModel.environmentLighting.collectAsStateWithLifecycle()
    val timeCompatibility by viewModel.timeCompatibility.collectAsStateWithLifecycle()
    val weatherCompatibility by viewModel.weatherCompatibility.collectAsStateWithLifecycle()
    val keyElements by viewModel.keyElements.collectAsStateWithLifecycle()
    val basePrompt by viewModel.basePrompt.collectAsStateWithLifecycle()
    val validationIssues by viewModel.validationIssues.collectAsStateWithLifecycle()
    val canSave by viewModel.canSave.collectAsStateWithLifecycle()
    val saveCompleted by viewModel.saveCompleted.collectAsStateWithLifecycle()

    LaunchedEffect(saveCompleted) {
        if (saveCompleted) onSaved()
    }

    Column(modifier = modifier.fillMaxSize()) {
        AssetFormHeader(
            title = uiString("locationForm.title", language),
            subtitle = uiString("locationForm.subtitle", language),
            onBack = onBack,
            backTestTag = LOCATION_FORM_BACK_BUTTON_TAG,
            language = language,
            theme = theme,
            onToggleLanguage = onToggleLanguage,
            onToggleTheme = onToggleTheme,
            toggleLanguageTestTag = LOCATION_FORM_TOGGLE_LANGUAGE_BUTTON_TAG,
            toggleThemeTestTag = LOCATION_FORM_TOGGLE_THEME_BUTTON_TAG
        )

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = viewModel::setName,
                label = { Text(uiString("assetForm.nameLabel", language)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(LOCATION_FORM_NAME_FIELD_TAG)
            )
            OutlinedTextField(
                value = description, onValueChange = viewModel::setDescription,
                label = { Text(uiString("assetForm.descriptionLabel", language)) },
                modifier = Modifier.fillMaxWidth().testTag(LOCATION_FORM_DESCRIPTION_FIELD_TAG)
            )
            AssetFormEnumDropdownField(
                label = uiString("locationForm.locationTypeLabel", language),
                selectedLabel = locationTypeLabel(locationType, language),
                testTag = LOCATION_FORM_LOCATION_TYPE_FIELD_TAG
            ) { onDismiss ->
                AssetFormFlatEntries(LocationType.entries, { locationTypeLabel(it, language) }) { viewModel.setLocationType(it); onDismiss() }
            }
            Text(
                text = uiTemplate("assetForm.continuityLockLevelFixedTemplate", language, "level" to locationContinuityLevelLabel(viewModel.continuityLockLevel, language)),
                style = MaterialTheme.typography.labelMedium
            )

            Text(text = uiString("locationForm.environmentSectionTitle", language), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = environmentType, onValueChange = viewModel::setEnvironmentType, label = { Text(uiString("locationForm.environmentTypeLabel", language)) }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = environmentSize, onValueChange = viewModel::setEnvironmentSize, label = { Text(uiString("locationForm.environmentSizeLabel", language)) }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = environmentLighting, onValueChange = viewModel::setEnvironmentLighting, label = { Text(uiString("locationForm.environmentLightingLabel", language)) }, singleLine = true, modifier = Modifier.weight(1f))
            }

            AssetFormTagListField(
                label = uiString("locationForm.timeCompatibilityLabel", language),
                addButtonLabel = uiString("assetForm.addTagButton", language),
                tags = timeCompatibility,
                onAdd = viewModel::addTimeCompatibility,
                onRemove = viewModel::removeTimeCompatibility,
                testTag = LOCATION_FORM_TIME_TAG_INPUT_TAG
            )
            AssetFormTagListField(
                label = uiString("locationForm.weatherCompatibilityLabel", language),
                addButtonLabel = uiString("assetForm.addTagButton", language),
                tags = weatherCompatibility,
                onAdd = viewModel::addWeatherCompatibility,
                onRemove = viewModel::removeWeatherCompatibility,
                testTag = LOCATION_FORM_WEATHER_TAG_INPUT_TAG
            )
            AssetFormTagListField(
                label = uiString("locationForm.keyElementsLabel", language),
                addButtonLabel = uiString("assetForm.addTagButton", language),
                tags = keyElements,
                onAdd = viewModel::addKeyElement,
                onRemove = viewModel::removeKeyElement,
                testTag = LOCATION_FORM_KEY_ELEMENT_INPUT_TAG
            )

            OutlinedTextField(
                value = basePrompt, onValueChange = viewModel::setBasePrompt,
                label = { Text(uiString("assetForm.basePromptLabel", language)) }, modifier = Modifier.fillMaxWidth()
            )

            validationIssues.forEach { AssetFormValidationIssueRow(it) }
        }

        Button(
            onClick = viewModel::save,
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag(LOCATION_FORM_SAVE_BUTTON_TAG)
        ) {
            Text(uiString("assetForm.saveButton", language))
        }
    }
}
