package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.asset.ReferenceImage
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.storybreakdown.BUILTIN_AI_CONNECTOR_PROFILES
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import com.operaboys.cinemashotgenerator.ui.common.RetranslateButton
import com.operaboys.cinemashotgenerator.ui.home.DecodedContentImage
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۳ — قدم ۲ — بخش ج: صفحه‌ی فرم ساخت Object. طبق
// docs/blueprints/16-user-workflow-v2.md «مرحله ۳» + docs/design/README.md بخش
// «۸. Assets Library». تنها فرم این قدم با یک Rule Blocking واقعی (Rule ۱۰) —
// اعتبارسنجی زنده روی هر تغییر فیلد، دکمه‌ی ذخیره تا رفع Blocking غیرفعال است.
// جزئیات کامل تصمیمات در docs/adr/049-unit16-phase3-step2-asset-forms.md.

const val OBJECT_FORM_NAME_FIELD_TAG = "objectForm.nameField"
const val OBJECT_FORM_SUBTYPE_FIELD_TAG = "objectForm.subtypeField"
const val OBJECT_FORM_SIZE_FIELD_TAG = "objectForm.sizeField"
const val OBJECT_FORM_MATERIAL_FIELD_TAG = "objectForm.materialAndColorField"
/** سیستم Preview دوزبانه‌ی پرامپت — قدم ۳ از ۳ زیرقدم، پایانی (ADR-123). */
const val OBJECT_FORM_DESCRIPTION_FA_PREVIEW_FIELD_TAG = "objectForm.descriptionFaPreviewField"
const val OBJECT_FORM_SAVE_BUTTON_TAG = "objectForm.saveButton"
const val OBJECT_FORM_BACK_BUTTON_TAG = "objectForm.backButton"
const val OBJECT_FORM_TOGGLE_LANGUAGE_BUTTON_TAG = "objectForm.toggleLanguageButton"
const val OBJECT_FORM_TOGGLE_THEME_BUTTON_TAG = "objectForm.toggleThemeButton"

// فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۵ از ۵ (ADR-135).
const val OBJECT_FORM_IMAGE_PROMPT_QUICK_BUTTON_TAG = "objectForm.imagePromptQuickButton"
const val OBJECT_FORM_IMAGE_PROMPT_AI_BUTTON_TAG = "objectForm.imagePromptAiButton"
const val OBJECT_FORM_IMAGE_PROMPT_STALE_WARNING_TAG = "objectForm.imagePromptStaleWarning"

// فیچر مستقل جدید «آپلود عکس مرجع واقعی Asset» — زیرقدم ۱ از ۳ (ADR-137).
const val OBJECT_FORM_ADD_REFERENCE_IMAGE_BUTTON_TAG = "objectForm.addReferenceImageButton"
fun objectReferenceImageRemoveButtonTag(index: Int): String = "objectForm.referenceImageRemoveButton.$index"

// یافته‌ی حیاتی چکاپ نهایی (ADR-143/144).
fun objectContinuityIssueRowTag(index: Int): String = "objectForm.continuityIssueRow.$index"

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
    val descriptionFaPreview by viewModel.descriptionFaPreview.collectAsStateWithLifecycle()
    val selectedTranslationProfileId by viewModel.selectedTranslationProfileId.collectAsStateWithLifecycle()
    val apiKeySavedForTranslationProfile by viewModel.apiKeySavedForTranslationProfile.collectAsStateWithLifecycle()
    val translationInProgress by viewModel.translationInProgress.collectAsStateWithLifecycle()
    val validationIssues by viewModel.validationIssues.collectAsStateWithLifecycle()
    // یافته‌ی حیاتی چکاپ نهایی (ADR-143/144).
    val continuityIssues by viewModel.continuityIssues.collectAsStateWithLifecycle()
    val canSave by viewModel.canSave.collectAsStateWithLifecycle()
    val saveCompleted by viewModel.saveCompleted.collectAsStateWithLifecycle()

    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۵ از ۵ (ADR-135).
    val imagePromptPreview by viewModel.imagePromptPreview.collectAsStateWithLifecycle()
    val imagePromptQuick by viewModel.imagePromptQuick.collectAsStateWithLifecycle()
    val imagePromptAi by viewModel.imagePromptAi.collectAsStateWithLifecycle()
    val imagePromptFaPreview by viewModel.imagePromptFaPreview.collectAsStateWithLifecycle()
    val imagePromptGeneratedAt by viewModel.imagePromptGeneratedAt.collectAsStateWithLifecycle()
    val imagePromptAiInProgress by viewModel.imagePromptAiInProgress.collectAsStateWithLifecycle()
    val imagePromptAiError by viewModel.imagePromptAiError.collectAsStateWithLifecycle()
    // اتصال Rule یتیم ADR-132 (ADR-136).
    val imagePromptValidationIssues by viewModel.imagePromptValidationIssues.collectAsStateWithLifecycle()

    // فیچر مستقل جدید «آپلود عکس مرجع واقعی Asset» — زیرقدم ۱ از ۳ (ADR-137):
    // هم‌الگو دقیق با chooseImageLauncher در SettingsScreen.kt — OpenDocument()
    // (نه GetContent()) + takePersistableUriPermission، چون localFilePath باید
    // بعد از بستن اپ هم معتبر بماند.
    val referenceImages by viewModel.referenceImages.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chooseReferenceImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.onFailure { error ->
                Log.w("ObjectAssetFormScreen", "takePersistableUriPermission failed for $it — the reference image may become inaccessible after app restart", error)
            }
            viewModel.addReferenceImage(it.toString())
        }
    }

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
            OutlinedTextField(
                value = descriptionFaPreview, onValueChange = viewModel::setDescriptionFaPreview,
                label = { Text(uiString("assetForm.descriptionFaPreviewLabel", language)) },
                modifier = Modifier.fillMaxWidth().testTag(OBJECT_FORM_DESCRIPTION_FA_PREVIEW_FIELD_TAG)
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

            ReferenceImagesSection(
                language = language,
                referenceImages = referenceImages,
                onAddClick = { chooseReferenceImageLauncher.launch(arrayOf("image/*")) },
                onRemove = viewModel::removeReferenceImage,
                addButtonTag = OBJECT_FORM_ADD_REFERENCE_IMAGE_BUTTON_TAG,
                removeButtonTag = ::objectReferenceImageRemoveButtonTag
            )

            ObjectImagePromptSection(
                viewModel = viewModel,
                language = language,
                preview = imagePromptPreview,
                imagePromptQuick = imagePromptQuick,
                imagePromptAi = imagePromptAi,
                imagePromptFaPreview = imagePromptFaPreview,
                imagePromptGeneratedAt = imagePromptGeneratedAt,
                aiInProgress = imagePromptAiInProgress,
                aiError = imagePromptAiError,
                apiKeySaved = apiKeySavedForTranslationProfile,
                promptValidationIssues = imagePromptValidationIssues
            )

            validationIssues.forEach { AssetFormValidationIssueRow(it) }
            // یافته‌ی حیاتی چکاپ نهایی (ADR-143/144).
            continuityIssues.forEachIndexed { index, issue ->
                AssetFormValidationIssueRow(issue, testTag = objectContinuityIssueRowTag(index))
            }
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

/**
 * فیچر مستقل جدید «آپلود عکس مرجع واقعی Asset» — زیرقدم ۱ از ۳ (ADR-137):
 * هم‌الگو دقیق با ReferenceImagesSection در CharacterAssetFormScreen.kt/
 * LocationAssetFormScreen.kt — تکرار عمدی به‌جای Promote به یک فایل مشترک.
 */
@Composable
private fun ReferenceImagesSection(
    language: Language,
    referenceImages: List<ReferenceImage>,
    onAddClick: () -> Unit,
    onRemove: (Int) -> Unit,
    addButtonTag: String,
    removeButtonTag: (Int) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = uiString("assetForm.referenceImagesSectionTitle", language), style = MaterialTheme.typography.titleSmall)
        // آپلود عکس مرجع واقعی Asset — زیرقدم ۲ از ۳ (ADR-138): پیش‌نمایش بزرگ و
        // واقعاً دیدنی اولین عکس، بالای فهرست متنی ساده‌ی زیرقدم ۱.
        referenceImages.firstOrNull()?.let { first ->
            DecodedContentImage(
                uriString = first.localFilePath,
                modifier = Modifier.size(140.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
        referenceImages.forEachIndexed { index, image ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = runCatching { Uri.parse(image.localFilePath).lastPathSegment }.getOrNull() ?: image.localFilePath,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemove(index) }, modifier = Modifier.testTag(removeButtonTag(index))) {
                    Icon(Icons.Filled.Close, contentDescription = uiString("assetForm.removeReferenceImageButton", language))
                }
            }
        }
        TextButton(onClick = onAddClick, modifier = Modifier.testTag(addButtonTag)) {
            Text(uiString("assetForm.addReferenceImageButton", language))
        }
    }
}

/**
 * فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۵ از ۵ (ADR-135): هم‌الگو دقیق با
 * CharacterImagePromptSection/LocationImagePromptSection.
 */
@Composable
private fun ObjectImagePromptSection(
    viewModel: ObjectAssetFormViewModel,
    language: Language,
    preview: String?,
    imagePromptQuick: String?,
    imagePromptAi: String?,
    imagePromptFaPreview: String?,
    imagePromptGeneratedAt: Long?,
    aiInProgress: Boolean,
    aiError: String?,
    apiKeySaved: Boolean,
    promptValidationIssues: List<ValidationIssue>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = uiString("objectForm.imagePromptSectionTitle", language), style = MaterialTheme.typography.titleSmall)
        preview?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = uiTemplate("assetForm.imagePromptPreviewTemplate", language, "prompt" to it),
                style = MaterialTheme.typography.bodySmall,
                color = CinemaTheme.extendedColors.fg3
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = viewModel::generateImagePromptQuick,
                modifier = Modifier.testTag(OBJECT_FORM_IMAGE_PROMPT_QUICK_BUTTON_TAG)
            ) {
                Text(uiString("assetForm.imagePromptQuickButton", language))
            }
            TextButton(
                onClick = { viewModel.generateObjectImagePromptWithAi() },
                enabled = apiKeySaved && !aiInProgress,
                modifier = Modifier.testTag(OBJECT_FORM_IMAGE_PROMPT_AI_BUTTON_TAG)
            ) {
                if (aiInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(uiString("assetForm.imagePromptAiButton", language))
                }
            }
        }
        if (!apiKeySaved) {
            Text(text = uiString("retranslate.noKeyHint", language), style = MaterialTheme.typography.labelSmall, color = CinemaTheme.extendedColors.fg3)
        }
        aiError?.let { AssetFormValidationIssueRow(ValidationIssue(Severity.BLOCKING, message = it)) }
        imagePromptQuick?.let {
            Text(text = uiTemplate("assetForm.imagePromptQuickResultTemplate", language, "prompt" to it), style = MaterialTheme.typography.bodySmall)
        }
        imagePromptAi?.let {
            Text(text = uiTemplate("assetForm.imagePromptAiResultTemplate", language, "prompt" to it), style = MaterialTheme.typography.bodySmall)
            imagePromptFaPreview?.let { fa ->
                Text(
                    text = uiTemplate("assetForm.imagePromptFaPreviewResultTemplate", language, "prompt" to fa),
                    style = MaterialTheme.typography.bodySmall,
                    color = CinemaTheme.extendedColors.fg3
                )
            }
        }
        promptValidationIssues.forEach { AssetFormValidationIssueRow(it) }
        if (viewModel.isImagePromptStale(imagePromptGeneratedAt)) {
            AssetFormValidationIssueRow(
                ValidationIssue(Severity.WARNING, message = uiString("assetForm.imagePromptStaleWarning", language)),
                testTag = OBJECT_FORM_IMAGE_PROMPT_STALE_WARNING_TAG
            )
        }
    }
}
