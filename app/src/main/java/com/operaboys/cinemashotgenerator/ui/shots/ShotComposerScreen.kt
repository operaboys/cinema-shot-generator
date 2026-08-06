package com.operaboys.cinemashotgenerator.ui.shots

import android.app.Application
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormEnumDropdownField
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormFlatEntries
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormHeader
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormValidationIssueRow
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

@Composable
fun ShotComposerScreen(
    sceneId: String,
    sceneDisplayTitle: String,
    shotId: String?,
    language: Language,
    onBack: () -> Unit,
    shotRepository: ShotRepository? = null,
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
    }
}
