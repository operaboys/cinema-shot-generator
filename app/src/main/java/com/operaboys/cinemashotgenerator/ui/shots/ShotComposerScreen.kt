package com.operaboys.cinemashotgenerator.ui.shots

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.workflow.ComposerLayoutVariant
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormEnumDropdownField
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormFlatEntries
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormHeader
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormValidationIssueRow
import com.operaboys.cinemashotgenerator.ui.dna.lightingStyleLabel
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۴ — قدم ۲ — بخش ب: اسکلت Shot Composer. طبق docs/design/README.md
// بخش «۷. Shot Composer»: نوار پیش‌نمایش (Hero، ۱۶۰dp)، ۴ Tab (اصلی/دوربین/نور و
// محیط/صدا)، فیلدهای سطح‌بالای مشترک (بیرون از هر Tab، طبق دستور کار صریح این
// قدم کامل و واقعی‌اند). محتوای واقعی هر Tab کار قدم ۳ است — فعلاً فقط Placeholder.
// AssetFormEnumDropdownField/AssetFormFlatEntries/AssetFormHeader/
// AssetFormValidationIssueRow از ui/assets/AssetFormSupport.kt بازاستفاده شدند —
// همان تصمیم بازاستفاده‌ی Cross-Feature قدم قبل (ADR-050). جزئیات کامل تصمیمات در
// docs/adr/051-unit16-phase4-step2-shot-list-composer-skeleton.md.

private enum class ShotComposerTab { MAIN, CAMERA, LIGHTING, AUDIO }

const val SHOT_COMPOSER_TITLE_FIELD_TAG = "shotComposer.titleField"
const val SHOT_COMPOSER_DESCRIPTION_FIELD_TAG = "shotComposer.descriptionField"
const val SHOT_COMPOSER_GOAL_FIELD_TAG = "shotComposer.goalField"
const val SHOT_COMPOSER_TYPE_FIELD_TAG = "shotComposer.typeField"
const val SHOT_COMPOSER_DURATION_FIELD_TAG = "shotComposer.durationField"
const val SHOT_COMPOSER_MOTION_FIELD_TAG = "shotComposer.motionField"
const val SHOT_COMPOSER_CAMERA_TAB_TAG = "shotComposer.tab.camera"
const val SHOT_COMPOSER_LIGHTING_TAB_TAG = "shotComposer.tab.lighting"
const val SHOT_COMPOSER_AUDIO_TAB_TAG = "shotComposer.tab.audio"
const val SHOT_COMPOSER_BACK_BUTTON_TAG = "shotComposer.backButton"
const val SHOT_COMPOSER_VALIDATION_BUTTON_TAG = "shotComposer.validationButton"
const val SHOT_COMPOSER_OUTPUT_DELIVERY_BUTTON_TAG = "shotComposer.outputDeliveryButton"

@Composable
fun ShotComposerScreen(
    sceneId: String,
    sceneDisplayTitle: String,
    shotId: String?,
    language: Language,
    onBack: () -> Unit,
    onNavigateToValidation: (shotId: String) -> Unit = {},
    onNavigateToOutputDelivery: (shotId: String) -> Unit = {},
    shotRepository: ShotRepository? = null,
    // رفع G12 باقی‌مانده (دستور کار ۲۰۲۶-۰۸-۱۳، docs/adr/081-...): پیش‌فرض TABS
    // — رفتار موجود (تست‌ها/فراخوان‌های قدیمی) بدون تغییر می‌ماند.
    composerLayoutVariant: ComposerLayoutVariant = ComposerLayoutVariant.TABS,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ShotComposerViewModel = viewModel(
        factory = ShotComposerViewModel.factory(application, sceneId, shotId, shotRepository)
    )

    val shotNumber by viewModel.shotNumber.collectAsStateWithLifecycle()
    val shotTitle by viewModel.shotTitle.collectAsStateWithLifecycle()
    val shotDescription by viewModel.shotDescription.collectAsStateWithLifecycle()
    val shotGoal by viewModel.shotGoal.collectAsStateWithLifecycle()
    val shotType by viewModel.shotType.collectAsStateWithLifecycle()
    val durationSecondsText by viewModel.durationSecondsText.collectAsStateWithLifecycle()
    val motionLevel by viewModel.motionLevel.collectAsStateWithLifecycle()
    val descriptionValidation by viewModel.shotDescriptionValidation.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(ShotComposerTab.MAIN) }

    Column(modifier = modifier.fillMaxSize()) {
        AssetFormHeader(
            title = uiString("shotComposer.title", language),
            subtitle = uiTemplate("shotComposer.subtitleTemplate", language, "scene" to sceneDisplayTitle, "number" to shotNumber.toString()),
            onBack = onBack,
            backTestTag = SHOT_COMPOSER_BACK_BUTTON_TAG
        )

        // دکمه‌ی ورود به صفحه‌ی Validation — طبق تصمیم مستند (ADR-055) فقط برای یک
        // شات از قبل ذخیره‌شده معنا دارد (shotId != null)؛ شات هنوز-ذخیره‌نشده چیزی
        // در Repository ندارد که Validate شود.
        if (shotId != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                TextButton(
                    onClick = { onNavigateToValidation(shotId) },
                    modifier = Modifier.testTag(SHOT_COMPOSER_VALIDATION_BUTTON_TAG)
                ) {
                    Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(uiString("validation.entryButtonLabel", language))
                }
                // ورودی مستقیم به Output Delivery — طبق دستور کار قدم ۳ فاز ۵ («نقطه‌ی
                // ورود می‌تواند هم از Validation و هم مستقیم از Shot Composer باشد»)،
                // دقیقاً هم‌الگو با دکمه‌ی Validation بالا (همان شرط shotId != null: شات
                // هنوز-ذخیره‌نشده چیزی برای رندر کردن ندارد).
                TextButton(
                    onClick = { onNavigateToOutputDelivery(shotId) },
                    modifier = Modifier.testTag(SHOT_COMPOSER_OUTPUT_DELIVERY_BUTTON_TAG)
                ) {
                    Icon(Icons.Filled.MovieFilter, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(uiString("outputDelivery.entryButtonLabel", language))
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 16.dp)
                .background(color = CinemaTheme.extendedColors.inset, shape = RoundedCornerShape(16.dp))
        )

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (composerLayoutVariant) {
                ComposerLayoutVariant.TABS -> {
                    MainFieldsSection(
                        viewModel = viewModel,
                        shotTitle = shotTitle,
                        shotDescription = shotDescription,
                        shotGoal = shotGoal,
                        shotType = shotType,
                        durationSecondsText = durationSecondsText,
                        motionLevel = motionLevel,
                        descriptionValidation = descriptionValidation,
                        language = language
                    )

                    SecondaryTabRow(selectedTabIndex = selectedTab.ordinal, modifier = Modifier.padding(top = 8.dp)) {
                        Tab(
                            selected = selectedTab == ShotComposerTab.MAIN,
                            onClick = { selectedTab = ShotComposerTab.MAIN },
                            text = { Text(uiString("shotComposer.tab.main", language)) }
                        )
                        Tab(
                            selected = selectedTab == ShotComposerTab.CAMERA,
                            onClick = { selectedTab = ShotComposerTab.CAMERA },
                            text = { Text(uiString("shotComposer.tab.camera", language)) },
                            modifier = Modifier.testTag(SHOT_COMPOSER_CAMERA_TAB_TAG)
                        )
                        Tab(
                            selected = selectedTab == ShotComposerTab.LIGHTING,
                            onClick = { selectedTab = ShotComposerTab.LIGHTING },
                            text = { Text(uiString("shotComposer.tab.lighting", language)) },
                            modifier = Modifier.testTag(SHOT_COMPOSER_LIGHTING_TAB_TAG)
                        )
                        Tab(
                            selected = selectedTab == ShotComposerTab.AUDIO,
                            onClick = { selectedTab = ShotComposerTab.AUDIO },
                            text = { Text(uiString("shotComposer.tab.audio", language)) },
                            modifier = Modifier.testTag(SHOT_COMPOSER_AUDIO_TAB_TAG)
                        )
                    }

                    when (selectedTab) {
                        ShotComposerTab.CAMERA -> CameraTabContent(
                            viewModel = viewModel,
                            language = language,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        ShotComposerTab.LIGHTING -> LightingEnvironmentTabContent(
                            viewModel = viewModel,
                            language = language,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        ShotComposerTab.AUDIO -> AudioTabContent(
                            viewModel = viewModel,
                            language = language,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        else -> Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = uiString("shotComposer.tabPlaceholderNextStep", language),
                                style = MaterialTheme.typography.bodyLarge,
                                color = CinemaTheme.extendedColors.fg3
                            )
                        }
                    }
                }

                ComposerLayoutVariant.ACCORDION -> ComposerAccordion(
                    viewModel = viewModel,
                    shotTitle = shotTitle,
                    shotDescription = shotDescription,
                    shotGoal = shotGoal,
                    shotType = shotType,
                    durationSecondsText = durationSecondsText,
                    motionLevel = motionLevel,
                    descriptionValidation = descriptionValidation,
                    language = language
                )
            }
        }
    }
}

/**
 * فیلدهای «اصلی» شات (عنوان/توصیف/هدف/نوع/مدت/میزان حرکت) — قبلاً همیشه بالای
 * نوار Tab رندر می‌شد (بدون تغییر رفتار در ComposerLayoutVariant.TABS)؛ اکنون
 * در ACCORDION همین بلوک عیناً بدنه‌ی گروه «اصلی» است — طبق mockup
 * (`docs/design/Cinema Studio.html`, `groupDefs[0]`: `{ title: 'اصلی — Shot',
 * icon: 'movie', rows: fieldSets[0] }`).
 */
@Composable
private fun MainFieldsSection(
    viewModel: ShotComposerViewModel,
    shotTitle: String,
    shotDescription: String,
    shotGoal: ShotGoal,
    shotType: ShotType,
    durationSecondsText: String,
    motionLevel: MotionLevel,
    descriptionValidation: com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue?,
    language: Language,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = shotTitle,
            onValueChange = viewModel::setShotTitle,
            label = { Text(uiString("shotComposer.shotTitleLabel", language)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(SHOT_COMPOSER_TITLE_FIELD_TAG)
        )
        OutlinedTextField(
            value = shotDescription,
            onValueChange = viewModel::setShotDescription,
            label = { Text(uiString("shotComposer.shotDescriptionLabel", language)) },
            modifier = Modifier.fillMaxWidth().testTag(SHOT_COMPOSER_DESCRIPTION_FIELD_TAG)
        )
        descriptionValidation?.let { AssetFormValidationIssueRow(it) }

        AssetFormEnumDropdownField(
            label = uiString("shotComposer.shotGoalLabel", language),
            selectedLabel = shotGoalLabel(shotGoal, language),
            testTag = SHOT_COMPOSER_GOAL_FIELD_TAG
        ) { onDismiss ->
            AssetFormFlatEntries(ShotGoal.entries, { shotGoalLabel(it, language) }) { viewModel.setShotGoal(it); onDismiss() }
        }
        AssetFormEnumDropdownField(
            label = uiString("shotComposer.shotTypeLabel", language),
            selectedLabel = shotTypeLabel(shotType, language),
            testTag = SHOT_COMPOSER_TYPE_FIELD_TAG
        ) { onDismiss ->
            AssetFormFlatEntries(ShotType.entries, { shotTypeLabel(it, language) }) { viewModel.setShotType(it); onDismiss() }
        }
        OutlinedTextField(
            value = durationSecondsText,
            onValueChange = viewModel::setDurationSecondsText,
            label = { Text(uiString("shotComposer.durationLabel", language)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(SHOT_COMPOSER_DURATION_FIELD_TAG)
        )
        AssetFormEnumDropdownField(
            label = uiString("shotComposer.motionLevelLabel", language),
            selectedLabel = motionLevelLabel(motionLevel, language),
            testTag = SHOT_COMPOSER_MOTION_FIELD_TAG
        ) { onDismiss ->
            AssetFormFlatEntries(MotionLevel.entries, { motionLevelLabel(it, language) }) { viewModel.setMotionLevel(it); onDismiss() }
        }
    }
}

private enum class ComposerAccordionGroup { MAIN, CAMERA, LIGHTING, AUDIO }

const val SHOT_COMPOSER_ACCORDION_GROUP_HEADER_TAG_PREFIX = "shotComposer.accordion.group."
fun composerAccordionGroupHeaderTag(group: String): String = SHOT_COMPOSER_ACCORDION_GROUP_HEADER_TAG_PREFIX + group.lowercase()

/**
 * `composerLayoutVariant.ACCORDION` — طبق mockup (`docs/design/Cinema Studio.html`،
 * بخش `composerGroups`، خط تقریبی ۲۰۱۱۲۸۰): ۴ گروه هم‌ارز (اصلی/دوربین/نور و
 * محیط/صدا)، هرکدام کارت جداگانه با آیکون+عنوان+خلاصه+Chevron؛ طبق تصمیم دقیق
 * mockup (`group: st.group === i ? -1 : i`) **همیشه فقط یک گروه باز می‌ماند**،
 * نه چند گروه هم‌زمان.
 *
 * **تصمیم مستقل — محاسبه‌ی خلاصه‌ی هر گروه:** mockup این را
 * `g.rows.slice(0, 3).map(r => r.value).join(' · ')` تعریف کرده (سه فیلد اول
 * همان گروه). چون بدنه‌ی واقعی هر گروه در این اپ یک Composable خودمختار است
 * (نه یک فهرست ساده‌ی Row های label/value)، این قدم برای هر گروه ۳ مقدار
 * واقعی و نماینده را مستقیماً از `ShotComposerViewModel` جمع‌آوری می‌کند:
 * MAIN → هدف/نوع/میزان حرکت (۳ Enum معنادار، نه عنوان/توصیف متن آزاد که
 * خلاصه‌ی خوبی نیستند)؛ CAMERA → زاویه/فاصله/نوع لنز (دقیقاً همان سه فیلد اول
 * واقعی `CameraTabContent`)؛ LIGHTING (که در این اپ نور+محیط را با هم
 * می‌پوشاند) → سبک نور/موقعیت نور اصلی/نوع آب‌وهوا (یک نماینده از هرکدام)؛
 * AUDIO چون فیلدهایش فهرست‌اند نه Enum تکی، شمارش هرکدام نمایش داده می‌شود.
 */
@Composable
private fun ComposerAccordion(
    viewModel: ShotComposerViewModel,
    shotTitle: String,
    shotDescription: String,
    shotGoal: ShotGoal,
    shotType: ShotType,
    durationSecondsText: String,
    motionLevel: MotionLevel,
    descriptionValidation: com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue?,
    language: Language
) {
    // پیش‌فرض «اصلی» باز است — دقیقاً طبق مقدار اولیه‌ی mockup (`group: 0`).
    // nullable چون طبق منطق دقیق mockup (`group: st.group === i ? -1 : i`)،
    // کلیک دوباره روی همان گروه بازِ فعلی آن را می‌بندد (هیچ گروهی باز نمی‌ماند)،
    // نه فقط سوییچ بین گروه‌ها.
    var expandedGroup by rememberSaveable { mutableStateOf<ComposerAccordionGroup?>(ComposerAccordionGroup.MAIN) }
    val toggle: (ComposerAccordionGroup) -> Unit = { group ->
        expandedGroup = if (expandedGroup == group) null else group
    }

    val cameraAngle by viewModel.cameraAngle.collectAsStateWithLifecycle()
    val cameraDistance by viewModel.cameraDistance.collectAsStateWithLifecycle()
    val lensType by viewModel.lensType.collectAsStateWithLifecycle()
    val lightingStyle by viewModel.lightingStyle.collectAsStateWithLifecycle()
    val keyLightPosition by viewModel.keyLightPosition.collectAsStateWithLifecycle()
    val weatherType by viewModel.weatherType.collectAsStateWithLifecycle()
    val ambientSounds by viewModel.ambientSounds.collectAsStateWithLifecycle()
    val actionSounds by viewModel.actionSounds.collectAsStateWithLifecycle()
    val characterSounds by viewModel.characterSounds.collectAsStateWithLifecycle()

    val mainSummary = listOf(
        shotGoalLabel(shotGoal, language),
        shotTypeLabel(shotType, language),
        motionLevelLabel(motionLevel, language)
    ).joinToString(" · ")
    val cameraSummary = listOf(
        cameraAngleLabel(cameraAngle, language),
        cameraDistanceLabel(cameraDistance, language),
        lensTypeLabel(lensType, language)
    ).joinToString(" · ")
    val lightingSummary = listOf(
        lightingStyleLabel(lightingStyle, language),
        keyLightPositionLabel(keyLightPosition, language),
        weatherTypeLabel(weatherType, language)
    ).joinToString(" · ")
    val audioSummary = uiTemplate(
        "shotComposer.accordion.audioSummaryTemplate",
        language,
        "ambient" to ambientSounds.size.toString(),
        "action" to actionSounds.size.toString(),
        "character" to characterSounds.size.toString()
    )

    ComposerAccordionGroupCard(
        group = ComposerAccordionGroup.MAIN,
        icon = Icons.Filled.Movie,
        titleKey = "shotComposer.tab.main",
        summary = mainSummary,
        expanded = expandedGroup == ComposerAccordionGroup.MAIN,
        language = language,
        onToggle = { toggle(ComposerAccordionGroup.MAIN) }
    ) {
        MainFieldsSection(
            viewModel = viewModel,
            shotTitle = shotTitle,
            shotDescription = shotDescription,
            shotGoal = shotGoal,
            shotType = shotType,
            durationSecondsText = durationSecondsText,
            motionLevel = motionLevel,
            descriptionValidation = descriptionValidation,
            language = language
        )
    }
    ComposerAccordionGroupCard(
        group = ComposerAccordionGroup.CAMERA,
        icon = Icons.Filled.Videocam,
        titleKey = "shotComposer.tab.camera",
        summary = cameraSummary,
        expanded = expandedGroup == ComposerAccordionGroup.CAMERA,
        language = language,
        onToggle = { toggle(ComposerAccordionGroup.CAMERA) }
    ) {
        CameraTabContent(viewModel = viewModel, language = language)
    }
    ComposerAccordionGroupCard(
        group = ComposerAccordionGroup.LIGHTING,
        icon = Icons.Filled.WbSunny,
        titleKey = "shotComposer.tab.lighting",
        summary = lightingSummary,
        expanded = expandedGroup == ComposerAccordionGroup.LIGHTING,
        language = language,
        onToggle = { toggle(ComposerAccordionGroup.LIGHTING) }
    ) {
        LightingEnvironmentTabContent(viewModel = viewModel, language = language)
    }
    ComposerAccordionGroupCard(
        group = ComposerAccordionGroup.AUDIO,
        icon = Icons.Filled.GraphicEq,
        titleKey = "shotComposer.tab.audio",
        summary = audioSummary,
        expanded = expandedGroup == ComposerAccordionGroup.AUDIO,
        language = language,
        onToggle = { toggle(ComposerAccordionGroup.AUDIO) }
    ) {
        AudioTabContent(viewModel = viewModel, language = language)
    }
}

@Composable
private fun ComposerAccordionGroupCard(
    group: ComposerAccordionGroup,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titleKey: String,
    summary: String,
    expanded: Boolean,
    language: Language,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .testTag(composerAccordionGroupHeaderTag(group.name)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = uiString(titleKey, language), style = MaterialTheme.typography.titleMedium)
                    if (!expanded) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.labelSmall,
                            color = CinemaTheme.extendedColors.fg3,
                            maxLines = 1
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = CinemaTheme.extendedColors.fg3
                )
            }
            if (expanded) {
                Box(modifier = Modifier.padding(top = 12.dp)) {
                    content()
                }
            }
        }
    }
}
