package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate

// واحد ۱۶ فاز ۳ — قدم ۲ — بخش ج: صفحه‌ی فرم ساخت Object. طبق
// docs/blueprints/16-user-workflow-v2.md «مرحله ۳» + docs/design/README.md بخش
// «۸. Assets Library». تنها فرم این قدم با یک Rule Blocking واقعی (Rule ۱۰) —
// اعتبارسنجی زنده روی هر تغییر فیلد، دکمه‌ی ذخیره تا رفع Blocking غیرفعال است.
// جزئیات کامل تصمیمات در docs/adr/049-unit16-phase3-step2-asset-forms.md.

const val OBJECT_FORM_NAME_FIELD_TAG = "objectForm.nameField"
const val OBJECT_FORM_SUBTYPE_FIELD_TAG = "objectForm.subtypeField"
const val OBJECT_FORM_SIZE_FIELD_TAG = "objectForm.sizeField"
const val OBJECT_FORM_MATERIAL_FIELD_TAG = "objectForm.materialAndColorField"
const val OBJECT_FORM_SAVE_BUTTON_TAG = "objectForm.saveButton"
const val OBJECT_FORM_BACK_BUTTON_TAG = "objectForm.backButton"
const val OBJECT_FORM_TOGGLE_LANGUAGE_BUTTON_TAG = "objectForm.toggleLanguageButton"
const val OBJECT_FORM_TOGGLE_THEME_BUTTON_TAG = "objectForm.toggleThemeButton"

@Composable
fun ObjectAssetFormScreen(
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
    val viewModel: ObjectAssetFormViewModel = viewModel(
        factory = ObjectAssetFormViewModel.factory(application, projectId, assetRepository, existingAssetId)
    )

    val name by viewModel.name.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val subtype by viewModel.subtype.collectAsStateWithLifecycle()
    val size by viewModel.size.collectAsStateWithLifecycle()
    val materialAndColor by viewModel.materialAndColor.collectAsStateWithLifecycle()
    val specialTrait by viewModel.specialTrait.collectAsStateWithLifecycle()
    val basePrompt by viewModel.basePrompt.collectAsStateWithLifecycle()
    val validationIssues by viewModel.validationIssues.collectAsStateWithLifecycle()
    val canSave by viewModel.canSave.collectAsStateWithLifecycle()
    val saveCompleted by viewModel.saveCompleted.collectAsStateWithLifecycle()

    LaunchedEffect(saveCompleted) {
        if (saveCompleted) onSaved()
    }

    Column(modifier = modifier.fillMaxSize()) {
        AssetFormHeader(
            title = uiString("objectForm.title", language),
            subtitle = uiString("objectForm.subtitle", language),
            onBack = onBack,
            backTestTag = OBJECT_FORM_BACK_BUTTON_TAG,
            language = language,
            theme = theme,
            onToggleLanguage = onToggleLanguage,
            onToggleTheme = onToggleTheme,
            toggleLanguageTestTag = OBJECT_FORM_TOGGLE_LANGUAGE_BUTTON_TAG,
            toggleThemeTestTag = OBJECT_FORM_TOGGLE_THEME_BUTTON_TAG
        )

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = viewModel::setName,
                label = { Text(uiString("assetForm.nameLabel", language)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(OBJECT_FORM_NAME_FIELD_TAG)
            )
            OutlinedTextField(
                value = description, onValueChange = viewModel::setDescription,
                label = { Text(uiString("assetForm.descriptionLabel", language)) },
                modifier = Modifier.fillMaxWidth()
            )
            AssetFormEnumDropdownField(
                label = uiString("objectForm.subtypeLabel", language),
                selectedLabel = objectSubtypeLabel(subtype, language),
                testTag = OBJECT_FORM_SUBTYPE_FIELD_TAG
            ) { onDismiss ->
                AssetFormFlatEntries(ObjectSubtype.entries, { objectSubtypeLabel(it, language) }) { viewModel.setSubtype(it); onDismiss() }
            }
            Text(
                text = uiTemplate("assetForm.continuityLockLevelFixedTemplate", language, "level" to propContinuityLevelLabel(viewModel.continuityLockLevel, language)),
                style = MaterialTheme.typography.labelMedium
            )
            OutlinedTextField(
                value = size, onValueChange = viewModel::setSize,
                label = { Text(uiString("objectForm.sizeLabel", language)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(OBJECT_FORM_SIZE_FIELD_TAG)
            )
            OutlinedTextField(
                value = materialAndColor, onValueChange = viewModel::setMaterialAndColor,
                label = { Text(uiString("objectForm.materialAndColorLabel", language)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(OBJECT_FORM_MATERIAL_FIELD_TAG)
            )
            OutlinedTextField(
                value = specialTrait, onValueChange = viewModel::setSpecialTrait,
                label = { Text(uiString("objectForm.specialTraitLabel", language)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
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
            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag(OBJECT_FORM_SAVE_BUTTON_TAG)
        ) {
            Text(uiString("assetForm.saveButton", language))
        }
    }
}
