package com.operaboys.cinemashotgenerator.ui.dna

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.domain.dna.AspectRatio
import com.operaboys.cinemashotgenerator.domain.dna.ColorTemperature
import com.operaboys.cinemashotgenerator.domain.dna.ContrastLevel
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.dna.RealismLevel
import com.operaboys.cinemashotgenerator.domain.dna.SaturationLevel
import com.operaboys.cinemashotgenerator.domain.dna.StyleConsistency
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import com.operaboys.cinemashotgenerator.domain.dna.validateColorPalette
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode
import com.operaboys.cinemashotgenerator.domain.visualidentity.CompatibilityResult
import com.operaboys.cinemashotgenerator.domain.visualidentity.StyleInfluence
import com.operaboys.cinemashotgenerator.domain.visualidentity.checkStyleCompatibility
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.ui.assets.OpaqueChip
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.story.moodLabel
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۲ — قدم ۳ (آخرین قدم فاز ۲): محتوای واقعی Tab «DNA»، جایگزین
// Placeholder فاز ۱. طبق docs/design/README.md بخش «۳. Studio → DNA tab» +
// docs/blueprints/16-user-workflow-v2.md «مرحله ۲: DNA Configuration». تصمیمات
// مستقل کامل در docs/adr/047-unit16-phase2-step3-dna-tab.md — خلاصه:
// - سطح Collapse فقط روی ۶ گروه است (نه یک Accordion دوسطحی روی هر ردیف هم) —
//   سادگی عمدی؛ سود دیداری سطح دوم کاملاً بصری است، نه عملکردی، و هیچ‌کدام از
//   تست‌های الزامی این قدم به آن نیاز ندارد.
// - همه‌ی ۶ گروه پیش‌فرض باز (expanded) — کاربر بلافاصله همه‌ی فرم را می‌بیند.
// - `colorPalette` همیشه یک لیست فشرده (بدون رشته‌ی خالی میان‌مقداری) نگه داشته
//   می‌شود؛ اعتبارسنجی زنده‌اش مستقیماً از `validateColorPalette` (تنها تابع
//   DnaValidation.kt که واقعاً روی ویرایش زنده‌ی یک فیلد این فرم قابل‌اعمال است —
//   بقیه‌ی قوانین آن فایل یک Shot را در برابر DNA اعتبارسنجی می‌کنند، نه خودِ DNA).
// - `OutputConstraints` فقط با AspectRatio در UI نمایش داده می‌شود، طبق فهرست
//   صریح فیلدهای این قدم (forbiddenElements/mandatoryElements/
//   maxShotDurationSeconds در این قدم UI ندارند).

const val DNA_VISUAL_STYLE_FIELD_TAG = "dna.visualStyleField"
const val DNA_REALISM_LEVEL_FIELD_TAG = "dna.realismLevelField"
const val DNA_CORE_STYLE_CONSISTENCY_FIELD_TAG = "dna.coreStyleConsistencyField"
/** اتصال کامل Style Matrix — قدم ۳ از ۸ زیرقدم (ADR-115). */
const val DNA_SECONDARY_STYLE_FIELD_TAG = "dna.secondaryStyleField"
const val DNA_STYLE_INFLUENCE_FIELD_TAG = "dna.styleInfluenceField"
const val DNA_COLOR_TEMPERATURE_FIELD_TAG = "dna.colorTemperatureField"
const val DNA_SATURATION_FIELD_TAG = "dna.saturationField"
const val DNA_CONTRAST_FIELD_TAG = "dna.contrastField"
const val DNA_GRADING_PRESET_FIELD_TAG = "dna.gradingPresetField"
const val DNA_MOOD_FIELD_TAG = "dna.moodField"
const val DNA_MOOD_INTENSITY_FIELD_TAG = "dna.moodIntensityField"
const val DNA_MOOD_CONSISTENCY_FIELD_TAG = "dna.moodConsistencyField"
const val DNA_LIGHTING_STYLE_FIELD_TAG = "dna.lightingStyleField"
const val DNA_ASPECT_RATIO_FIELD_TAG = "dna.aspectRatioField"
const val DNA_MAX_SHOT_DURATION_FIELD_TAG = "dna.maxShotDurationField"
const val DNA_MANDATORY_ELEMENT_FIELD_TAG = "dna.mandatoryElementField"
const val DNA_ADD_MANDATORY_ELEMENT_BUTTON_TAG = "dna.addMandatoryElementButton"
const val DNA_FORBIDDEN_CATEGORY_FIELD_TAG = "dna.forbiddenCategoryField"
const val DNA_FORBIDDEN_VALUE_FIELD_TAG = "dna.forbiddenValueField"
const val DNA_ADD_FORBIDDEN_ELEMENT_BUTTON_TAG = "dna.addForbiddenElementButton"
const val DNA_QUALITY_TAGS_FIELD_TAG = "dna.qualityTagsField"
const val DNA_NEGATIVE_PROMPT_FIELD_TAG = "dna.negativePromptField"
/** تکمیل Rule یتیم — قدم ۴ از ۴ (ADR-112): حالت سراسری Cinematic Language. */
const val DNA_CINEMATIC_MODE_FIELD_TAG = "dna.cinematicModeField"

/** طبق «مثلاً» بلوپرینت ۰۲ (forbidden_elements: camera/lighting/weather) — همان سه دسته‌ای که Rule 2 (validateShotAgainstDna) واقعاً روی‌شان اعمال می‌شود. */
val DNA_FORBIDDEN_ELEMENT_CATEGORIES = listOf("camera", "lighting", "weather")

fun dnaForbiddenCategoryLabel(category: String, language: Language): String = when (category) {
    "camera" -> uiString("dna.outputConstraints.category.camera", language)
    "lighting" -> uiString("dna.outputConstraints.category.lighting", language)
    "weather" -> uiString("dna.outputConstraints.category.weather", language)
    else -> category
}

/** testTag برای Swatch شماره‌ی [index] (۰ تا ۴) — طبق الگوی testTag های فیلد در قدم قبل. */
fun dnaSwatchFieldTag(index: Int): String = "dna.swatchField.$index"

fun dnaMandatoryElementChipTag(index: Int): String = "dna.mandatoryElementChip.$index"
fun dnaForbiddenElementChipTag(index: Int): String = "dna.forbiddenElementChip.$index"

@Composable
fun DnaTabContent(
    projectId: String,
    language: Language,
    projectDnaRepository: ProjectDnaRepository? = null,
    dependentShotsCount: Int = 0,
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: DnaViewModel = viewModel(factory = DnaViewModel.factory(application, projectId, projectDnaRepository))
    val dna by viewModel.dna.collectAsStateWithLifecycle()
    val maxShotDurationSecondsText by viewModel.maxShotDurationSecondsText.collectAsStateWithLifecycle()
    val lastActionMessage by viewModel.lastActionMessage.collectAsStateWithLifecycle()

    LaunchedEffect(lastActionMessage) {
        lastActionMessage?.let { onShowMessage(it); viewModel.clearLastActionMessage() }
    }

    val colorPaletteIssue = remember(dna.masterPalette.colorPalette) { validateColorPalette(dna.masterPalette.colorPalette) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SoftLockBanner(language = language)

        // اتصال کامل Style Matrix — قدم ۳ از ۸ زیرقدم (ADR-115): تا این قدم کاربر
        // هیچ راهی برای دیدن یا تنظیم دستی secondaryStyle/influence (ADR-113) یا
        // برای دیدن هشدار زنده‌ی ناسازگاری (ADR-114) نداشت. secondaryStyleIssue
        // دقیقاً هم‌الگوی colorPaletteIssue بالا (remember + نمایش با
        // ValidationIssueRow) — فقط وقتی secondaryStyle واقعاً انتخاب شده باشد
        // محاسبه می‌شود (Influence بدون Secondary بی‌معناست، طبق تصمیم قدم ۱).
        val secondaryStyleIssue = remember(dna.coreIdentity.dominantVisualStyle, dna.coreIdentity.secondaryStyle, language) {
            dna.coreIdentity.secondaryStyle?.let { secondary ->
                checkStyleCompatibility(dna.coreIdentity.dominantVisualStyle, secondary)
                    .toValidationIssueOrNull(dna.coreIdentity.dominantVisualStyle, secondary, language)
            }
        }

        DnaGroup(title = uiString("dna.group.coreIdentity", language), fieldCount = 5) {
            EnumDropdownField(
                label = uiString("dna.coreIdentity.visualStyleLabel", language),
                selectedLabel = visualStyleLabel(dna.coreIdentity.dominantVisualStyle, language),
                testTag = DNA_VISUAL_STYLE_FIELD_TAG
            ) { onDismiss ->
                GroupedEntries(
                    entries = VisualStyle.entries,
                    categoryOf = VisualStyle::category,
                    categoryLabel = { visualStyleCategoryLabel(it, language) },
                    itemLabel = { visualStyleLabel(it, language) },
                    onSelected = { viewModel.setDominantVisualStyle(it, dependentShotsCount); onDismiss() }
                )
            }
            EnumDropdownField(
                label = uiString("dna.coreIdentity.realismLevelLabel", language),
                selectedLabel = realismLevelLabel(dna.coreIdentity.realismLevel, language),
                testTag = DNA_REALISM_LEVEL_FIELD_TAG
            ) { onDismiss ->
                FlatEntries(RealismLevel.entries, { realismLevelLabel(it, language) }) { viewModel.setRealismLevel(it); onDismiss() }
            }
            EnumDropdownField(
                label = uiString("dna.coreIdentity.styleConsistencyLabel", language),
                selectedLabel = styleConsistencyLabel(dna.coreIdentity.styleConsistency, language),
                testTag = DNA_CORE_STYLE_CONSISTENCY_FIELD_TAG
            ) { onDismiss ->
                FlatEntries(StyleConsistency.entries, { styleConsistencyLabel(it, language) }) { viewModel.setCoreStyleConsistency(it); onDismiss() }
            }
            EnumDropdownField(
                label = uiString("dna.coreIdentity.secondaryStyleLabel", language),
                selectedLabel = dna.coreIdentity.secondaryStyle?.let { visualStyleLabel(it, language) }
                    ?: uiString("dna.coreIdentity.secondaryStyleNone", language),
                testTag = DNA_SECONDARY_STYLE_FIELD_TAG
            ) { onDismiss ->
                Column {
                    DropdownMenuItem(
                        text = { Text(uiString("dna.coreIdentity.secondaryStyleNone", language)) },
                        onClick = { viewModel.setSecondaryVisualStyle(null); onDismiss() }
                    )
                    GroupedEntries(
                        entries = VisualStyle.entries,
                        categoryOf = VisualStyle::category,
                        categoryLabel = { visualStyleCategoryLabel(it, language) },
                        itemLabel = { visualStyleLabel(it, language) },
                        onSelected = { viewModel.setSecondaryVisualStyle(it); onDismiss() }
                    )
                }
            }
            if (dna.coreIdentity.secondaryStyle != null) {
                EnumDropdownField(
                    label = uiString("dna.coreIdentity.influenceLabel", language),
                    selectedLabel = dna.coreIdentity.influence?.let { styleInfluenceLabel(it, language) }
                        ?: uiString("dna.coreIdentity.influenceUnset", language),
                    testTag = DNA_STYLE_INFLUENCE_FIELD_TAG
                ) { onDismiss ->
                    FlatEntries(StyleInfluence.entries, { styleInfluenceLabel(it, language) }) { viewModel.setStyleInfluence(it); onDismiss() }
                }
            }
            secondaryStyleIssue?.let { ValidationIssueRow(it) }
        }

        DnaGroup(title = uiString("dna.group.masterPalette", language), fieldCount = 5) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = uiString("dna.masterPalette.colorPaletteLabel", language), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    for (index in 0 until 5) {
                        ColorSwatchField(
                            hex = dna.masterPalette.colorPalette.getOrNull(index) ?: "",
                            onHexChange = { viewModel.setColorSwatch(index, it) },
                            language = language,
                            testTag = dnaSwatchFieldTag(index),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                colorPaletteIssue?.let { ValidationIssueRow(it) }
            }
            EnumDropdownField(
                label = uiString("dna.masterPalette.colorTemperatureLabel", language),
                selectedLabel = colorTemperatureLabel(dna.masterPalette.colorTemperature, language),
                testTag = DNA_COLOR_TEMPERATURE_FIELD_TAG
            ) { onDismiss ->
                FlatEntries(ColorTemperature.entries, { colorTemperatureLabel(it, language) }) { viewModel.setColorTemperature(it); onDismiss() }
            }
            EnumDropdownField(
                label = uiString("dna.masterPalette.saturationLabel", language),
                selectedLabel = saturationLevelLabel(dna.masterPalette.globalSaturation, language),
                testTag = DNA_SATURATION_FIELD_TAG
            ) { onDismiss ->
                FlatEntries(SaturationLevel.entries, { saturationLevelLabel(it, language) }) { viewModel.setGlobalSaturation(it); onDismiss() }
            }
            EnumDropdownField(
                label = uiString("dna.masterPalette.contrastLabel", language),
                selectedLabel = contrastLevelLabel(dna.masterPalette.globalContrast, language),
                testTag = DNA_CONTRAST_FIELD_TAG
            ) { onDismiss ->
                FlatEntries(ContrastLevel.entries, { contrastLevelLabel(it, language) }) { viewModel.setGlobalContrast(it); onDismiss() }
            }
            OutlinedTextField(
                value = dna.masterPalette.colorGradingPreset,
                onValueChange = viewModel::setColorGradingPreset,
                label = { Text(uiString("dna.masterPalette.gradingPresetLabel", language)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(DNA_GRADING_PRESET_FIELD_TAG)
            )
        }

        DnaGroup(title = uiString("dna.group.globalMoodBase", language), fieldCount = 3) {
            EnumDropdownField(
                label = uiString("dna.globalMoodBase.moodLabel", language),
                selectedLabel = moodLabel(dna.globalMoodBase.primaryEmotion, language),
                testTag = DNA_MOOD_FIELD_TAG
            ) { onDismiss ->
                GroupedEntries(
                    entries = Mood.entries,
                    categoryOf = Mood::category,
                    categoryLabel = { moodCategoryLabel(it, language) },
                    itemLabel = { moodLabel(it, language) },
                    onSelected = { viewModel.setPrimaryEmotion(it); onDismiss() }
                )
            }
            OutlinedTextField(
                value = dna.globalMoodBase.intensity,
                onValueChange = viewModel::setMoodIntensity,
                label = { Text(uiString("dna.globalMoodBase.intensityLabel", language)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(DNA_MOOD_INTENSITY_FIELD_TAG)
            )
            EnumDropdownField(
                label = uiString("dna.globalMoodBase.consistencyLabel", language),
                selectedLabel = styleConsistencyLabel(dna.globalMoodBase.consistency, language),
                testTag = DNA_MOOD_CONSISTENCY_FIELD_TAG
            ) { onDismiss ->
                FlatEntries(StyleConsistency.entries, { styleConsistencyLabel(it, language) }) { viewModel.setMoodConsistency(it); onDismiss() }
            }
        }

        DnaGroup(title = uiString("dna.group.lightingPreference", language), fieldCount = 1) {
            EnumDropdownField(
                label = uiString("dna.lightingPreference.styleLabel", language),
                selectedLabel = dna.lightingPreference.preferredStyle?.let { lightingStyleLabel(it, language) }
                    ?: uiString("dna.lightingPreference.none", language),
                testTag = DNA_LIGHTING_STYLE_FIELD_TAG
            ) { onDismiss ->
                Column {
                    DropdownMenuItem(
                        text = { Text(uiString("dna.lightingPreference.none", language)) },
                        onClick = { viewModel.setPreferredLightingStyle(null); onDismiss() }
                    )
                    GroupedEntries(
                        entries = LightingStyle.entries,
                        categoryOf = LightingStyle::category,
                        categoryLabel = { lightingCategoryLabel(it, language) },
                        itemLabel = { lightingStyleLabel(it, language) },
                        onSelected = { viewModel.setPreferredLightingStyle(it); onDismiss() }
                    )
                }
            }
        }

        // تکمیل Rule یتیم — قدم ۴ از ۴ (ADR-112، آخرین قدم کل فیچر): حالت سراسری
        // Cinematic Language پروژه — تا این قدم فقط پیش‌فرض‌های برنامه‌نویسی‌شده
        // اعمال می‌شدند، بدون هیچ راهی برای تنظیم دستی توسط کاربر. سه مقدار ثابت
        // (غیر-nullable) — هم‌الگو دقیق با فیلدهای Enum ساده‌ی گروه‌های بالا
        // (RealismLevel/StyleConsistency).
        DnaGroup(title = uiString("dna.group.cinematicLanguage", language), fieldCount = 1) {
            EnumDropdownField(
                label = uiString("dna.cinematicLanguage.globalModeLabel", language),
                selectedLabel = cinematicModeLabel(dna.cinematicLanguage.globalMode, language),
                testTag = DNA_CINEMATIC_MODE_FIELD_TAG
            ) { onDismiss ->
                FlatEntries(CinematicMode.entries, { cinematicModeLabel(it, language) }) { viewModel.setGlobalCinematicMode(it); onDismiss() }
            }
        }

        DnaGroup(title = uiString("dna.group.outputConstraints", language), fieldCount = 4) {
            EnumDropdownField(
                label = uiString("dna.outputConstraints.aspectRatioLabel", language),
                selectedLabel = aspectRatioLabel(dna.outputConstraints.aspectRatio),
                testTag = DNA_ASPECT_RATIO_FIELD_TAG
            ) { onDismiss ->
                FlatEntries(AspectRatio.entries, { aspectRatioLabel(it) }) { viewModel.setAspectRatio(it); onDismiss() }
            }
            OutlinedTextField(
                value = maxShotDurationSecondsText,
                onValueChange = viewModel::setMaxShotDurationSecondsText,
                label = { Text(uiString("dna.outputConstraints.maxShotDurationLabel", language)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(DNA_MAX_SHOT_DURATION_FIELD_TAG)
            )
            MandatoryElementsSection(viewModel = viewModel, language = language, elements = dna.outputConstraints.mandatoryElements)
            ForbiddenElementsSection(viewModel = viewModel, language = language, forbiddenElements = dna.outputConstraints.forbiddenElements)
        }

        DnaGroup(title = uiString("dna.group.qualityDirectives", language), fieldCount = 2) {
            OutlinedTextField(
                value = dna.qualityDirectives.qualityTags,
                onValueChange = viewModel::setQualityTags,
                label = { Text(uiString("dna.qualityDirectives.qualityTagsLabel", language)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(DNA_QUALITY_TAGS_FIELD_TAG)
            )
            OutlinedTextField(
                value = dna.qualityDirectives.negativePrompt,
                onValueChange = viewModel::setNegativePrompt,
                label = { Text(uiString("dna.qualityDirectives.negativePromptLabel", language)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(DNA_NEGATIVE_PROMPT_FIELD_TAG)
            )
        }
    }
}

/**
 * بنر هشدار Soft Lock — نارنجی/Bold/حاشیه‌ی واضح طبق تأکید صریح سند طراحی
 * («this must read as clearly urgent»). این بنر خودِ نمایش بصری فلسفه‌ی Soft Lock
 * پروژه است: DNA Manager هرگز کاربر را Block نمی‌کند.
 */
@Composable
private fun SoftLockBanner(language: Language) {
    // یافته‌ی واقعی بازبینی نهایی (واحد ۱۶ فاز ۶ قدم ۲، ADR-059): همان کلاس باگ
    // Low-opacity Tinted Fill (این یکی هم حاشیه داشت اما پس‌زمینه‌اش فقط ۱۴٪
    // Alpha بود، نه Solid طبق الزام صریح سند طراحی «Final spec: solid/near-opaque
    // fills + a visible 2dp border»). رفع شد؛ رنگ متن/آیکون به سیاه ثابت تغییر
    // کرد چون نارنجی-روی-نارنجی-Solid دیگر خوانا نبود.
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CinemaTheme.extendedColors.orange,
        border = BorderStroke(2.dp, CinemaTheme.extendedColors.orange)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Color.Black)
            Column {
                Text(
                    text = uiString("dna.softLockTitle", language),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.Black
                )
                Text(
                    text = uiString("dna.softLockMessage", language),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun DnaGroup(title: String, fieldCount: Int, content: @Composable () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Surface(shape = CircleShape, color = CinemaTheme.extendedColors.inset) {
                        Text(
                            text = fieldCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = CinemaTheme.extendedColors.fg3,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                }
            }
            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnumDropdownField(
    label: String,
    selectedLabel: String,
    testTag: String,
    menuContent: @Composable (onDismiss: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag(testTag)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            menuContent { expanded = false }
        }
    }
}

@Composable
private fun <T> FlatEntries(entries: List<T>, itemLabel: (T) -> String, onSelected: (T) -> Unit) {
    entries.forEach { entry ->
        DropdownMenuItem(text = { Text(itemLabel(entry)) }, onClick = { onSelected(entry) })
    }
}

/**
 * فهرست گروه‌بندی‌شده بر اساس دسته — طبق تصریح صریح دستور کار («نه یک Dropdown
 * تخت»). `entries` باید از قبل به‌ترتیب دسته پیوسته باشد (هر سه enum مصرف‌کننده —
 * VisualStyle/Mood/LightingStyle — دقیقاً همین‌طور با گروه‌های پیوسته در بلوپرینت
 * تعریف شده‌اند، تأییدشده با خواندن مستقیم فایل)؛ یک سربرگ غیرقابل‌کلیک قبل از
 * اولین آیتم هر دسته‌ی جدید درج می‌شود.
 */
@Composable
private fun <T, C> GroupedEntries(
    entries: List<T>,
    categoryOf: (T) -> C,
    categoryLabel: (C) -> String,
    itemLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    var lastCategory: C? = null
    entries.forEach { entry ->
        val category = categoryOf(entry)
        if (category != lastCategory) {
            Text(
                text = categoryLabel(category),
                style = MaterialTheme.typography.labelSmall,
                color = CinemaTheme.extendedColors.fg3,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            lastCategory = category
        }
        DropdownMenuItem(text = { Text(itemLabel(entry)) }, onClick = { onSelected(entry) })
    }
}

@Composable
private fun ColorSwatchField(hex: String, onHexChange: (String) -> Unit, language: Language, testTag: String, modifier: Modifier = Modifier) {
    val hexPattern = remember { Regex("^#[0-9A-Fa-f]{6}$") }
    val previewColor = if (hexPattern.matches(hex)) runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull() else null

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(previewColor ?: CinemaTheme.extendedColors.inset, CircleShape)
                .border(1.dp, CinemaTheme.extendedColors.cardBorder, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        OutlinedTextField(
            value = hex,
            onValueChange = onHexChange,
            placeholder = { Text(uiString("dna.masterPalette.swatchPlaceholder", language), style = MaterialTheme.typography.labelSmall) },
            singleLine = true,
            textStyle = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth().testTag(testTag)
        )
    }
}

/**
 * اتصال کامل Style Matrix — قدم ۳ از ۸ زیرقدم (ADR-115): تبدیل خروجی خالص
 * دامنه‌ای `CompatibilityResult` (بدون هیچ متن، فقط level/warning) به یک
 * `ValidationIssue` محلی‌سازی‌شده — عمداً در همین فایل UI (نه به‌عنوان extension
 * در `domain/visualidentity/StyleMatrix.kt`) چون به `Language`/`uiTemplate`/
 * `visualStyleLabel` نیاز دارد؛ دامنه نباید به لایه‌ی i18n/UI وابسته شود
 * (همان انضباط جداسازی که `DnaAssetMappers.kt` هم رعایت می‌کند). فقط وقتی
 * `warning=true` (یعنی LOW/INCOMPATIBLE) یک نتیجه برمی‌گرداند؛ برای
 * HIGH/MEDIUM چیزی نمایش داده نمی‌شود (`null`).
 */
private fun CompatibilityResult.toValidationIssueOrNull(
    primary: VisualStyle,
    secondary: VisualStyle,
    language: Language
): ValidationIssue? {
    if (!warning) return null
    return ValidationIssue(
        severity = Severity.WARNING,
        message = uiTemplate(
            "dna.coreIdentity.styleCompatibilityWarningTemplate",
            language,
            "primary" to visualStyleLabel(primary, language),
            "secondary" to visualStyleLabel(secondary, language)
        )
    )
}

@Composable
private fun ValidationIssueRow(issue: ValidationIssue) {
    val color = if (issue.severity == Severity.BLOCKING) MaterialTheme.colorScheme.error else CinemaTheme.extendedColors.warning
    val icon = if (issue.severity == Severity.BLOCKING) Icons.Filled.Error else Icons.Filled.Warning
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = color)
        Text(text = issue.message, color = color, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * فرم افزودن دستی ساده — هم‌الگو با actionSounds/characterSounds Shot Composer
 * (docs/adr/053، تصمیم ۶): لیست چیپ قابل‌حذف + یک فیلد متنی + دکمه‌ی افزودن.
 */
@Composable
private fun MandatoryElementsSection(viewModel: DnaViewModel, language: Language, elements: List<String>) {
    var text by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(uiString("dna.outputConstraints.mandatoryElementsSectionTitle", language), style = MaterialTheme.typography.labelLarge)
        if (elements.isEmpty()) {
            Text(
                uiString("dna.outputConstraints.mandatoryElementsEmptyState", language),
                style = MaterialTheme.typography.bodySmall,
                color = CinemaTheme.extendedColors.fg3
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                elements.forEachIndexed { index, element ->
                    OpaqueChip(
                        label = "$element ×",
                        selected = false,
                        onClick = { viewModel.removeMandatoryElement(index) },
                        testTag = dnaMandatoryElementChipTag(index)
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(uiString("dna.outputConstraints.mandatoryElementFieldLabel", language)) },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag(DNA_MANDATORY_ELEMENT_FIELD_TAG)
            )
            IconButton(
                onClick = { if (text.isNotBlank()) { viewModel.addMandatoryElement(text); text = "" } },
                modifier = Modifier.testTag(DNA_ADD_MANDATORY_ELEMENT_BUTTON_TAG)
            ) {
                Icon(Icons.Filled.Add, contentDescription = uiString("dna.outputConstraints.addMandatoryElementButton", language))
            }
        }
    }
}

/**
 * سه دسته‌ی ثابت (camera/lighting/weather) طبق «مثلاً» بلوپرینت ۰۲ — نه یک فیلد
 * متنی آزاد برای دسته، چون هیچ enum دامنه‌ای برایش وجود ندارد (خودِ
 * `OutputConstraints.forbiddenElements` عمداً `Map<String, List<String>>` باز
 * مانده) اما یک Dropdown بسته از سه دسته‌ی مستند‌شده، از تایپوی بی‌اثر (کلیدی که
 * هیچ Rule ای با آن مطابقت نمی‌کند) جلوگیری می‌کند.
 */
@Composable
private fun ForbiddenElementsSection(viewModel: DnaViewModel, language: Language, forbiddenElements: Map<String, List<String>>) {
    var category by remember { mutableStateOf(DNA_FORBIDDEN_ELEMENT_CATEGORIES.first()) }
    var value by remember { mutableStateOf("") }
    val flattened = remember(forbiddenElements) {
        forbiddenElements.flatMap { (cat, values) -> values.map { cat to it } }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(uiString("dna.outputConstraints.forbiddenElementsSectionTitle", language), style = MaterialTheme.typography.labelLarge)
        if (flattened.isEmpty()) {
            Text(
                uiString("dna.outputConstraints.forbiddenElementsEmptyState", language),
                style = MaterialTheme.typography.bodySmall,
                color = CinemaTheme.extendedColors.fg3
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                flattened.forEachIndexed { index, (cat, elementValue) ->
                    OpaqueChip(
                        label = "${dnaForbiddenCategoryLabel(cat, language)}: $elementValue ×",
                        selected = false,
                        onClick = { viewModel.removeForbiddenElement(cat, elementValue) },
                        testTag = dnaForbiddenElementChipTag(index)
                    )
                }
            }
        }
        EnumDropdownField(
            label = uiString("dna.outputConstraints.forbiddenElementCategoryLabel", language),
            selectedLabel = dnaForbiddenCategoryLabel(category, language),
            testTag = DNA_FORBIDDEN_CATEGORY_FIELD_TAG
        ) { onDismiss ->
            FlatEntries(DNA_FORBIDDEN_ELEMENT_CATEGORIES, { dnaForbiddenCategoryLabel(it, language) }) { category = it; onDismiss() }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(uiString("dna.outputConstraints.forbiddenElementValueLabel", language)) },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag(DNA_FORBIDDEN_VALUE_FIELD_TAG)
            )
            IconButton(
                onClick = { if (value.isNotBlank()) { viewModel.addForbiddenElement(category, value); value = "" } },
                modifier = Modifier.testTag(DNA_ADD_FORBIDDEN_ELEMENT_BUTTON_TAG)
            ) {
                Icon(Icons.Filled.Add, contentDescription = uiString("dna.outputConstraints.addForbiddenElementButton", language))
            }
        }
    }
}
