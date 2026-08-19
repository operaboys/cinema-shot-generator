package com.operaboys.cinemashotgenerator.ui.shots

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import com.operaboys.cinemashotgenerator.domain.workflow.ComposerLayoutVariant
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormEnumDropdownField
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormFlatEntries
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormHeader
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormValidationIssueRow
import com.operaboys.cinemashotgenerator.ui.dna.cinematicModeLabel
import com.operaboys.cinemashotgenerator.ui.dna.lightingStyleLabel
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.outputdelivery.modelProfileDisplayName
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
/** سیستم Preview دوزبانه‌ی پرامپت — قدم ۳ از ۳ زیرقدم، پایانی (ADR-123). */
const val SHOT_COMPOSER_DESCRIPTION_FA_PREVIEW_FIELD_TAG = "shotComposer.descriptionFaPreviewField"
const val SHOT_COMPOSER_GOAL_FIELD_TAG = "shotComposer.goalField"
const val SHOT_COMPOSER_TYPE_FIELD_TAG = "shotComposer.typeField"
const val SHOT_COMPOSER_DURATION_FIELD_TAG = "shotComposer.durationField"
const val SHOT_COMPOSER_MOTION_FIELD_TAG = "shotComposer.motionField"
/** تکمیل Rule یتیم — قدم ۴ از ۴ (ADR-112): Override محلی سطح شات Cinematic Mode. */
const val SHOT_COMPOSER_CINEMATIC_MODE_FIELD_TAG = "shotComposer.cinematicModeField"
const val SHOT_COMPOSER_CAMERA_TAB_TAG = "shotComposer.tab.camera"
const val SHOT_COMPOSER_LIGHTING_TAB_TAG = "shotComposer.tab.lighting"
const val SHOT_COMPOSER_AUDIO_TAB_TAG = "shotComposer.tab.audio"
const val SHOT_COMPOSER_BACK_BUTTON_TAG = "shotComposer.backButton"
const val SHOT_COMPOSER_VALIDATION_BUTTON_TAG = "shotComposer.validationButton"
const val SHOT_COMPOSER_OUTPUT_DELIVERY_BUTTON_TAG = "shotComposer.outputDeliveryButton"
const val SHOT_COMPOSER_TOGGLE_LANGUAGE_BUTTON_TAG = "shotComposer.toggleLanguageButton"
const val SHOT_COMPOSER_TOGGLE_THEME_BUTTON_TAG = "shotComposer.toggleThemeButton"

/**
 * یافته‌ی #۱۳ appendix ADR-081 (ADR-086) — دقیقاً همان زیرمجموعه‌ی ۴تایی
 * `chipKeys = ['veo', 'kling', 'runway', 'seedance']` mockup، نگاشت‌شده به
 * profileId های واقعی `ModelProfileLibrary` (نه هر ۱۳ پروفایل موجود).
 */
internal val ComposerSummaryModelChipKeys = listOf("veo_3_1", "kling_3_0", "runway_gen_4_5", "seedance_2_5")

fun shotComposerModelChipTag(profileId: String): String = "shotComposer.modelChip.$profileId"
fun shotComposerCopyModelButtonTag(profileId: String): String = "shotComposer.copyModelButton.$profileId"

/**
 * یافته‌ی ۵ appendix ADR-081 (ADR-082): گرادیان دقیق نوار Hero طبق mockup
 * (docs/design/Cinema Studio.html، `cs-shot-hero`/`cs-shot-hero-b`:
 * `linear-gradient(150deg,#2C4260 0%,#3C5570 48%,#6E5B72 100%)`)، هر دو
 * نسخه‌ی TABS و ACCORDION همین گرادیان را دارند. قبلاً یک رنگ تخت
 * (`CinemaTheme.extendedColors.inset`) بود. `internal` تا در تست واحد
 * جداگانه (نه Compose UI Test) مقادیر دقیق قابل‌تأیید باشند.
 */
internal val ComposerHeroGradientColors = listOf(Color(0xFF2C4260), Color(0xFF3C5570), Color(0xFF6E5B72))

@Composable
fun ShotComposerScreen(
    projectId: String,
    sceneId: String,
    sceneDisplayTitle: String,
    shotId: String?,
    language: Language,
    theme: AppTheme,
    onBack: () -> Unit,
    onToggleLanguage: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onNavigateToValidation: (shotId: String) -> Unit = {},
    onNavigateToOutputDelivery: (shotId: String) -> Unit = {},
    shotRepository: ShotRepository? = null,
    sceneRepository: SceneRepository? = null,
    projectDnaRepository: ProjectDnaRepository? = null,
    assetRepository: AssetRepository? = null,
    // یافته‌ی #۱۳ appendix ADR-081 (ADR-086) — چیپ‌های مدل پنل خلاصه فقط نمایشگر
    // مدل سراسری فعلی‌اند (WorkflowState.selectedModelProfileId، همان مفهوم
    // استفاده‌شده در OutputDeliveryScreen)، نه یک انتخاب مستقل هرشات.
    selectedModelProfileId: String? = null,
    onShowMessage: (String) -> Unit = {},
    // رفع G12 باقی‌مانده (دستور کار ۲۰۲۶-۰۸-۱۳، docs/adr/081-...): پیش‌فرض TABS
    // — رفتار موجود (تست‌ها/فراخوان‌های قدیمی) بدون تغییر می‌ماند.
    composerLayoutVariant: ComposerLayoutVariant = ComposerLayoutVariant.TABS,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ShotComposerViewModel = viewModel(
        factory = ShotComposerViewModel.factory(
            application, projectId, sceneId, shotId, shotRepository, sceneRepository, projectDnaRepository, assetRepository
        )
    )

    val shotNumber by viewModel.shotNumber.collectAsStateWithLifecycle()
    val shotTitle by viewModel.shotTitle.collectAsStateWithLifecycle()
    val shotDescription by viewModel.shotDescription.collectAsStateWithLifecycle()
    val shotGoal by viewModel.shotGoal.collectAsStateWithLifecycle()
    val shotType by viewModel.shotType.collectAsStateWithLifecycle()
    val durationSecondsText by viewModel.durationSecondsText.collectAsStateWithLifecycle()
    val motionLevel by viewModel.motionLevel.collectAsStateWithLifecycle()
    val descriptionValidation by viewModel.shotDescriptionValidation.collectAsStateWithLifecycle()
    val validationSummary by viewModel.validationSummary.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(ShotComposerTab.MAIN) }

    Column(modifier = modifier.fillMaxSize()) {
        AssetFormHeader(
            title = uiString("shotComposer.title", language),
            subtitle = uiTemplate("shotComposer.subtitleTemplate", language, "scene" to sceneDisplayTitle, "number" to shotNumber.toString()),
            onBack = onBack,
            backTestTag = SHOT_COMPOSER_BACK_BUTTON_TAG,
            language = language,
            theme = theme,
            onToggleLanguage = onToggleLanguage,
            onToggleTheme = onToggleTheme,
            toggleLanguageTestTag = SHOT_COMPOSER_TOGGLE_LANGUAGE_BUTTON_TAG,
            toggleThemeTestTag = SHOT_COMPOSER_TOGGLE_THEME_BUTTON_TAG
        )

        // یافته‌ی واقعی دیباگ ADR-086: قبلاً فقط این بخش (بین Hero و پنل خلاصه‌ی
        // تازه) خودش Scroll مستقل داشت (`weight(1f)` + `verticalScroll` جدا)، و
        // پنل خلاصه‌ + دکمه‌ی Output Delivery بیرون از آن، به‌صورت واقعاً Pinned
        // (طبق `flex:none` mockup) قرار داشتند. تست End-to-End واقعی این قدم
        // (ShotComposerAccordionFlowTest) این ترکیب را روی Viewport واقعی تست
        // (۳۲۰×۴۷۰dp) اجرا کرد و ثابت کرد: مجموع ارتفاع ثابت (Header+Hero+پنل
        // تازه+دکمه‌ی Output Delivery) از کل ارتفاع صفحه بیشتر می‌شود و ناحیه‌ی
        // Scroll میانی را به‌طور کامل به ۰dp/۰dp فشرده می‌کند (اندازه‌گیری مستقیم:
        // `boundsInRoot` گره MAIN، `Rect.fromLTRB(0,0,0,0)`) — یعنی محتوای فرم اصلاً
        // در درخت نمایش‌داده‌نشده باقی می‌ماند، نه صرفاً نیازمند Scroll بیشتر (Scroll
        // روی یک ناحیه‌ی صفر-ارتفاع کمکی نمی‌کند). رفع: به‌جای دو ناحیه‌ی Scroll
        // جدا (میانی + Pinned)، همه‌چیز بعد از Header (Hero + محتوای Tab/Accordion +
        // پنل خلاصه + دکمه‌ی Output Delivery) در یک Column با یک Scroll واحد قرار
        // گرفت — پنل خلاصه دیگر به‌صورت فنی «Pinned» نیست (انحراف مستند از
        // `flex:none` دقیق mockup)، اما تضمین می‌کند هیچ محتوایی هرگز واقعاً
        // غیرقابل‌دسترس نشود؛ روی هر دستگاه واقعی (که طول صفحه‌اش خیلی بیشتر از
        // این Viewport تستی است) همچنان بلافاصله بعد از محتوا و نزدیک پایین دیده
        // می‌شود.
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 16.dp)
                .background(brush = Brush.linearGradient(ComposerHeroGradientColors), shape = RoundedCornerShape(16.dp))
        )

        Column(
            modifier = Modifier.padding(16.dp),
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

        // یافته‌ی #۱۳ appendix ADR-081 (ADR-086): پنل خلاصه‌ی زنده — طبق تصمیم
        // مستند (ADR-055/ADR-057) فقط برای یک شات از قبل ذخیره‌شده معنا دارد
        // (shotId != null). این پنل اکنون بخشی از همان Scroll واحد بالاست (نه
        // یک بلوک Pinned جدا) — طبق یافته‌ی مستندشده‌ی بالای این تابع.
        if (shotId != null) {
            ComposerSummaryFooter(
                blockingCount = validationSummary.blockingCount,
                warningCount = validationSummary.warningCount,
                language = language,
                selectedModelProfileId = selectedModelProfileId,
                onValidationClick = { onNavigateToValidation(shotId) },
                onCopyModel = { _, displayName ->
                    onShowMessage(uiTemplate("shotComposer.summary.modelCopiedMessage", language, "model" to displayName))
                }
            )
            // ورودی مستقیم به Output Delivery — طبق تصمیم مستند ADR-057 («نقطه‌ی
            // ورود می‌تواند هم از Validation و هم مستقیم از Shot Composer باشد»).
            // یافته‌ی واقعی این قدم: mockup پنل پایین Composer هیچ دکمه‌ی مستقیم
            // Output Delivery ای ندارد (فقط نوار Validation + چیپ‌های مدل)؛ اما حذف
            // این دکمه یک تصمیم مستند قبلی (ADR-057) را بی‌سروصدا نقض می‌کرد — پس
            // بدون تغییر، فقط زیر پنل تازه نگه داشته شد.
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                TextButton(
                    onClick = { onNavigateToOutputDelivery(shotId) },
                    modifier = Modifier.testTag(SHOT_COMPOSER_OUTPUT_DELIVERY_BUTTON_TAG)
                ) {
                    Icon(Icons.Filled.MovieFilter, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(uiString("outputDelivery.entryButtonLabel", language))
                }
            }
        }
        }
    }
}

/**
 * یافته‌ی #۱۳ appendix ADR-081 (ADR-086) — پنل خلاصه‌ی زنده‌ی پایین Composer.
 * سه بخش دقیقاً طبق mockup (`docs/design/Cinema Studio.html`، آفست ۲٬۰۲۵٬۹۳۵):
 * (۱) نوار قابل‌کلیک Blocking/Warning (کل نوار → `onValidationClick`، دقیقاً
 * هم‌الگو با `act.goValidation` مسیر)؛ (۲) کارت برچسب «Full Video Prompt»
 * + هشدار شرطی وقتی Blocking>0؛ (۳) ردیف افقی Scroll‌شونده‌ی ۴ چیپ مدل
 * (veo/kling/runway/seedance — دقیقاً همان زیرمجموعه‌ی `chipKeys` mockup، نه
 * هر ۱۳ پروفایل). طبق خواندن دقیق markup خام mockup، خودِ چیپ هیچ
 * onClick ای ندارد (فقط آیکون `content_copy` داخلش دارد) — یعنی چیپ صرفاً
 * نشانگر بصری مدل سراسری فعلی است (رنگ/گرادیان بر اساس `selectedModelProfileId`)،
 * نه یک دکمه‌ی انتخاب.
 */
@Composable
private fun ComposerSummaryFooter(
    blockingCount: Int,
    warningCount: Int,
    language: Language,
    selectedModelProfileId: String?,
    onValidationClick: () -> Unit,
    onCopyModel: (profileId: String, displayName: String) -> Unit
) {
    // مستقیماً روی Clipboard واقعی می‌نویسد — دقیقاً همان الگوی
    // OutputDeliveryScreen.kt (`LocalClipboardManager.current.setText(...)` در
    // همان محل کلیک، نه از طریق callback بالادستی).
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color(0xFFFF5A6A).copy(alpha = 0.12f), shape = RoundedCornerShape(18.dp))
                .clickable(onClick = onValidationClick)
                .padding(horizontal = 16.dp)
                .heightIn(min = 40.dp)
                .testTag(SHOT_COMPOSER_VALIDATION_BUTTON_TAG),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.Error, contentDescription = null, tint = Color(0xFFFF5A6A))
            Text(
                text = uiTemplate("shotComposer.summary.blockingTemplate", language, "count" to blockingCount.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF5A6A)
            )
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFFFB648))
            Text(
                text = uiTemplate("shotComposer.summary.warningTemplate", language, "count" to warningCount.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFB648)
            )
            Box(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = CinemaTheme.extendedColors.fg3)
        }

        Card(colors = CardDefaults.cardColors(), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = uiString("shotComposer.summary.promptLabel", language),
                    style = MaterialTheme.typography.bodySmall,
                    color = CinemaTheme.extendedColors.fg3
                )
                if (blockingCount > 0) {
                    Text(
                        text = uiString("shotComposer.summary.blockedHint", language),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF5A6A),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ComposerSummaryModelChipKeys.forEach { profileId ->
                val selected = profileId == selectedModelProfileId
                val displayName = modelProfileDisplayName(profileId)
                Row(
                    modifier = Modifier
                        .background(
                            brush = if (selected) Brush.linearGradient(listOf(Color(0xFF7C5CFF), Color(0xFF8E74FF))) else Brush.linearGradient(listOf(CinemaTheme.extendedColors.solidSurface, CinemaTheme.extendedColors.solidSurface)),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .border(1.dp, CinemaTheme.extendedColors.hairline, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp)
                        .heightIn(min = 40.dp)
                        .testTag(shotComposerModelChipTag(profileId)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) Color.White else CinemaTheme.extendedColors.fg2
                    )
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = uiString("shotComposer.summary.copyModelAction", language),
                        tint = if (selected) Color.White else CinemaTheme.extendedColors.fg3,
                        modifier = Modifier
                            .clickable {
                                clipboardManager.setText(AnnotatedString(displayName))
                                onCopyModel(profileId, displayName)
                            }
                            .testTag(shotComposerCopyModelButtonTag(profileId))
                    )
                }
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

        // سیستم Preview دوزبانه‌ی پرامپت — قدم ۳ از ۳ زیرقدم، پایانی (ADR-123):
        // StateFlow مستقیماً از viewModel جمع‌آوری می‌شود (نه پارامتر تازه در
        // امضای این تابع) — عیناً همان الگوی cinematicModeOverride/
        // effectiveCinematicMode پایین‌تر همین تابع، تا فراخوان‌های موجود این
        // Composable (TABS و ACCORDION، هر دو) بدون تغییر بمانند.
        val shotDescriptionFaPreview by viewModel.shotDescriptionFaPreview.collectAsStateWithLifecycle()
        OutlinedTextField(
            value = shotDescriptionFaPreview,
            onValueChange = viewModel::setShotDescriptionFaPreview,
            label = { Text(uiString("shotComposer.shotDescriptionFaPreviewLabel", language)) },
            modifier = Modifier.fillMaxWidth().testTag(SHOT_COMPOSER_DESCRIPTION_FA_PREVIEW_FIELD_TAG)
        )

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

        // تکمیل Rule یتیم — قدم ۴ از ۴ (ADR-112، آخرین قدم کل فیچر): Override محلی
        // سطح شات‌ Cinematic Mode (ADR-106) + نمایش زنده‌ی حالت مؤثر نهایی (خروجی
        // واقعی resolveEffectiveCinematicMode، ADR-108/109) — تا این قدم کاربر هیچ
        // راهی برای دیدن یا تنظیم دستی این زنجیره نداشت. StateFlow ها مستقیماً از
        // خودِ viewModel جمع‌آوری می‌شوند (نه پارامتر تازه در امضای این تابع) تا
        // فراخوان‌های موجود (TABS و ACCORDION، هر دو) بدون تغییر بمانند.
        val cinematicModeOverride by viewModel.cinematicModeOverride.collectAsStateWithLifecycle()
        val effectiveCinematicMode by viewModel.effectiveCinematicMode.collectAsStateWithLifecycle()
        AssetFormEnumDropdownField(
            label = uiString("shotComposer.cinematicModeOverrideLabel", language),
            selectedLabel = cinematicModeOverride?.let { cinematicModeLabel(it, language) }
                ?: uiString("shotComposer.cinematicModeFromSceneOrProject", language),
            testTag = SHOT_COMPOSER_CINEMATIC_MODE_FIELD_TAG
        ) { onDismiss ->
            Column {
                DropdownMenuItem(
                    text = { Text(uiString("shotComposer.cinematicModeFromSceneOrProject", language)) },
                    onClick = { viewModel.setCinematicModeOverride(null); onDismiss() }
                )
                AssetFormFlatEntries(CinematicMode.entries, { cinematicModeLabel(it, language) }) { viewModel.setCinematicModeOverride(it); onDismiss() }
            }
        }
        effectiveCinematicMode?.let { mode ->
            Text(
                text = uiTemplate("shotComposer.effectiveCinematicModeTemplate", language, "mode" to cinematicModeLabel(mode, language)),
                style = MaterialTheme.typography.bodySmall,
                color = CinemaTheme.extendedColors.fg3
            )
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
