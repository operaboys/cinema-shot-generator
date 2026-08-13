package com.operaboys.cinemashotgenerator.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.operaboys.cinemashotgenerator.BuildConfig
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import com.operaboys.cinemashotgenerator.domain.workflow.ComposerLayoutVariant
import com.operaboys.cinemashotgenerator.domain.workflow.HomeLayoutVariant
import com.operaboys.cinemashotgenerator.domain.workflow.ShotListViewMode
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormHeader
import com.operaboys.cinemashotgenerator.ui.assets.OpaqueChip
import com.operaboys.cinemashotgenerator.ui.home.DecodedContentImage
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel

// واحد ۱۶ فاز ۶ — قدم ۱ (اولین قدم آخرین فاز واحد ۱۶): صفحه‌ی Settings. طبق
// docs/design/README.md بخش «۱۱. Settings»: دقیقاً همان ۷ کارت (نمایش/گردش کار/
// حریم خصوصی/زبان و تم/تصویر Home/واریانت چیدمان/درباره)، چیزی اضافه/کم نشده.
// همه‌ی مقادیر این صفحه از خودِ WorkflowViewModel می‌آیند (هیچ ViewModel تازه‌ای
// لازم نبود — این صفحه چیزی جز نمایش/ویرایش State از‌پیش‌موجود آن ViewModel
// نیست، دقیقاً هم‌الگو با نحوه‌ی مصرف workflowViewModel در HomeScreen). جزئیات
// کامل تصمیمات (خصوصاً محدودیت‌های صریح Display/Jump-Between-Steps/Home Image)
// در docs/adr/058-unit16-phase6-step1-settings-autosave.md.

const val SETTINGS_BACK_BUTTON_TAG = "settings.backButton"
const val SETTINGS_LANGUAGE_FA_CHIP_TAG = "settings.language.fa"
const val SETTINGS_LANGUAGE_EN_CHIP_TAG = "settings.language.en"
const val SETTINGS_THEME_DARK_CHIP_TAG = "settings.theme.dark"
const val SETTINGS_THEME_LIGHT_CHIP_TAG = "settings.theme.light"
const val SETTINGS_HOME_LAYOUT_HERO_CHIP_TAG = "settings.homeLayout.hero"
const val SETTINGS_HOME_LAYOUT_RESUME_CHIP_TAG = "settings.homeLayout.resume"
const val SETTINGS_COMPOSER_LAYOUT_TABS_CHIP_TAG = "settings.composerLayout.tabs"
const val SETTINGS_COMPOSER_LAYOUT_ACCORDION_CHIP_TAG = "settings.composerLayout.accordion"
const val SETTINGS_SHOT_LIST_GRID_CHIP_TAG = "settings.shotListView.grid"
const val SETTINGS_SHOT_LIST_TIMELINE_CHIP_TAG = "settings.shotListView.timeline"
const val SETTINGS_DYNAMIC_FONT_SWITCH_TAG = "settings.dynamicFontSwitch"
const val SETTINGS_MIN_TOUCH_TARGET_SWITCH_TAG = "settings.minTouchTargetSwitch"
const val SETTINGS_REDUCED_MOTION_SWITCH_TAG = "settings.reducedMotionSwitch"
const val SETTINGS_ALLOW_FREE_STEP_JUMP_SWITCH_TAG = "settings.allowFreeStepJumpSwitch"
const val SETTINGS_CHOOSE_IMAGE_BUTTON_TAG = "settings.chooseImageButton"
const val SETTINGS_REMOVE_IMAGE_BUTTON_TAG = "settings.removeImageButton"
/** یافته‌ی ۲ appendix ADR-081 (ADR-083): کاشی پیش‌نمایش ۱۴۰px تصویر Home. */
const val SETTINGS_HOME_IMAGE_PREVIEW_TAG = "settings.homeImagePreview"
/** خودِ عنصر Image دیکودشده درون کاشی — برای اثبات رندر واقعی (نه فقط ظرف کارت). */
const val SETTINGS_HOME_IMAGE_PREVIEW_IMAGE_TAG = "settings.homeImagePreview.image"
/** یافته‌ی ۳ appendix ADR-081 (ADR-083): رشته‌ی نسخه‌ی واقعی کارت درباره. */
const val SETTINGS_ABOUT_VERSION_TAG = "settings.aboutVersion"

fun settingsAutoSaveCadenceChipTag(seconds: Long): String = "settings.autoSaveCadence.$seconds"

@Composable
fun SettingsScreen(
    workflowViewModel: WorkflowViewModel,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val language by workflowViewModel.language.collectAsStateWithLifecycle()
    val theme by workflowViewModel.theme.collectAsStateWithLifecycle()
    val homeLayoutVariant by workflowViewModel.homeLayoutVariant.collectAsStateWithLifecycle()
    val composerLayoutVariant by workflowViewModel.composerLayoutVariant.collectAsStateWithLifecycle()
    val workflowState by workflowViewModel.workflowState.collectAsStateWithLifecycle()
    val dynamicFontEnabled by workflowViewModel.dynamicFontEnabled.collectAsStateWithLifecycle()
    val minTouchTargetEnabled by workflowViewModel.minTouchTargetEnabled.collectAsStateWithLifecycle()
    val reducedMotionEnabled by workflowViewModel.reducedMotionEnabled.collectAsStateWithLifecycle()
    val autoSaveCadenceSeconds by workflowViewModel.autoSaveCadenceSeconds.collectAsStateWithLifecycle()
    val allowFreeStepJump by workflowViewModel.allowFreeStepJump.collectAsStateWithLifecycle()
    val homeScreenImageUri by workflowViewModel.homeScreenImageUri.collectAsStateWithLifecycle()

    // رفع G10 ممیزی post-Unit16 (docs/adr/075-...): برخلاف importLauncher
    // مشابه در ProjectsScreen.kt (که ActivityResultContracts.GetContent()
    // است)، اینجا عمداً OpenDocument() استفاده شد، نه یک کپی کامل از آن الگو
    // — homeScreenImageUri برخلاف Import (خواندن یک‌باره‌ی فوری محتوا) در
    // DataStore Persist و قرار است در اجراهای بعدی اپ دوباره خوانده شود؛
    // GetContent() (پشت ACTION_GET_CONTENT) تضمین نمی‌کند Uri بازگشتی پس از
    // بستن اپ هنوز معتبر بماند، اما OpenDocument() (پشت ACTION_OPEN_DOCUMENT،
    // Storage Access Framework) این تضمین را می‌دهد و takePersistableUriPermission
    // را واقعاً معتبر می‌کند.
    val context = LocalContext.current
    val chooseImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            workflowViewModel.setHomeScreenImageUri(it.toString())
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        AssetFormHeader(
            title = uiString("drawer.settings", language),
            subtitle = uiString("settings.subtitle", language),
            onBack = onBack,
            backTestTag = SETTINGS_BACK_BUTTON_TAG
        )

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DisplayCard(
                language = language,
                dynamicFontEnabled = dynamicFontEnabled,
                onDynamicFontChanged = workflowViewModel::setDynamicFontEnabled,
                minTouchTargetEnabled = minTouchTargetEnabled,
                onMinTouchTargetChanged = workflowViewModel::setMinTouchTargetEnabled,
                reducedMotionEnabled = reducedMotionEnabled,
                onReducedMotionChanged = workflowViewModel::setReducedMotionEnabled
            )

            WorkflowCard(
                language = language,
                shotListViewMode = workflowState?.shotListViewMode ?: ShotListViewMode.GRID,
                onShotListViewModeChanged = workflowViewModel::setShotListViewMode,
                autoSaveCadenceSeconds = autoSaveCadenceSeconds,
                onAutoSaveCadenceChanged = workflowViewModel::setAutoSaveCadenceSeconds,
                allowFreeStepJump = allowFreeStepJump,
                onAllowFreeStepJumpChanged = workflowViewModel::setAllowFreeStepJump
            )

            PrivacyCard(language = language)

            LanguageThemeCard(
                language = language,
                theme = theme,
                onLanguageChanged = workflowViewModel::setLanguage,
                onThemeChanged = workflowViewModel::setTheme
            )

            HomeImageCard(
                language = language,
                imageUri = homeScreenImageUri,
                onChooseImage = { chooseImageLauncher.launch(arrayOf("image/*")) },
                onRemoveImage = { workflowViewModel.setHomeScreenImageUri(null) }
            )

            LayoutVariantsCard(
                language = language,
                homeLayoutVariant = homeLayoutVariant,
                onHomeLayoutChanged = workflowViewModel::setHomeLayoutVariant,
                composerLayoutVariant = composerLayoutVariant,
                onComposerLayoutChanged = workflowViewModel::setComposerLayoutVariant
            )

            AboutCard(language = language)
        }
    }
}

@Composable
private fun SettingsCard(titleKey: String, language: Language, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(uiString(titleKey, language), style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, testTag: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(testTag))
    }
}

/**
 * طبق یافته‌ی صریح این قدم (grep روی ui/theme/Theme.kt): هیچ زیرساخت واقعی‌ای
 * برای Dynamic Font/Min Touch Target/Reduced Motion در این پروژه وجود ندارد.
 * سه سوییچ زیر واقعاً در DataStore Persist می‌شوند (نه صرفاً UI محلی)، اما
 * هیچ اثر Runtime ای در جای دیگری از اپ ندارند — محدودیت شناخته‌شده، صریحاً
 * مستند (نه پنهان).
 */
@Composable
private fun DisplayCard(
    language: Language,
    dynamicFontEnabled: Boolean,
    onDynamicFontChanged: (Boolean) -> Unit,
    minTouchTargetEnabled: Boolean,
    onMinTouchTargetChanged: (Boolean) -> Unit,
    reducedMotionEnabled: Boolean,
    onReducedMotionChanged: (Boolean) -> Unit
) {
    SettingsCard(titleKey = "settings.displayCardTitle", language = language) {
        SettingsSwitchRow(uiString("settings.dynamicFontLabel", language), dynamicFontEnabled, onDynamicFontChanged, SETTINGS_DYNAMIC_FONT_SWITCH_TAG)
        SettingsSwitchRow(uiString("settings.minTouchTargetLabel", language), minTouchTargetEnabled, onMinTouchTargetChanged, SETTINGS_MIN_TOUCH_TARGET_SWITCH_TAG)
        SettingsSwitchRow(uiString("settings.reducedMotionLabel", language), reducedMotionEnabled, onReducedMotionChanged, SETTINGS_REDUCED_MOTION_SWITCH_TAG)
    }
}

@Composable
private fun WorkflowCard(
    language: Language,
    shotListViewMode: ShotListViewMode,
    onShotListViewModeChanged: (ShotListViewMode) -> Unit,
    autoSaveCadenceSeconds: Long,
    onAutoSaveCadenceChanged: (Long) -> Unit,
    allowFreeStepJump: Boolean,
    onAllowFreeStepJumpChanged: (Boolean) -> Unit
) {
    SettingsCard(titleKey = "settings.workflowCardTitle", language = language) {
        Text(uiString("settings.shotListViewLabel", language), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpaqueChip(
                label = "GRID",
                selected = shotListViewMode == ShotListViewMode.GRID,
                onClick = { onShotListViewModeChanged(ShotListViewMode.GRID) },
                testTag = SETTINGS_SHOT_LIST_GRID_CHIP_TAG
            )
            OpaqueChip(
                label = "TIMELINE",
                selected = shotListViewMode == ShotListViewMode.TIMELINE,
                onClick = { onShotListViewModeChanged(ShotListViewMode.TIMELINE) },
                testTag = SETTINGS_SHOT_LIST_TIMELINE_CHIP_TAG
            )
        }

        Text(uiString("settings.autoSaveCadenceLabel", language), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15L, 30L, 60L).forEach { seconds ->
                OpaqueChip(
                    label = uiTemplate("settings.autoSaveCadenceOptionTemplate", language, "seconds" to seconds.toString()),
                    selected = autoSaveCadenceSeconds == seconds,
                    onClick = { onAutoSaveCadenceChanged(seconds) },
                    testTag = settingsAutoSaveCadenceChipTag(seconds)
                )
            }
        }

        SettingsSwitchRow(uiString("settings.allowFreeStepJumpLabel", language), allowFreeStepJump, onAllowFreeStepJumpChanged, SETTINGS_ALLOW_FREE_STEP_JUMP_SWITCH_TAG)
    }
}

/** طبق README: صرفاً نمایشی/اطلاعاتی — اپ کاملاً On-Device است، هیچ منطق قابل‌تغییری ندارد. */
@Composable
private fun PrivacyCard(language: Language) {
    SettingsCard(titleKey = "settings.privacyCardTitle", language = language) {
        PrivacyInfoRow(uiString("settings.privacyStorageLabel", language), uiString("settings.privacyStorageValue", language))
        PrivacyInfoRow(uiString("settings.privacyCloudSyncLabel", language), uiString("settings.privacyCloudSyncValue", language))
        PrivacyInfoRow(uiString("settings.privacyAnalyticsLabel", language), uiString("settings.privacyAnalyticsValue", language))
    }
}

@Composable
private fun PrivacyInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = CinemaTheme.extendedColors.fg3)
    }
}

/** رادیوهای صریح زبان/تم — Mirror دقیق WorkflowViewModel.language/theme موجود از فاز ۰ (نه State تازه). */
@Composable
private fun LanguageThemeCard(
    language: Language,
    theme: AppTheme,
    onLanguageChanged: (Language) -> Unit,
    onThemeChanged: (AppTheme) -> Unit
) {
    SettingsCard(titleKey = "settings.languageThemeCardTitle", language = language) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpaqueChip(label = "فارسی", selected = language == Language.FA, onClick = { onLanguageChanged(Language.FA) }, testTag = SETTINGS_LANGUAGE_FA_CHIP_TAG)
            OpaqueChip(label = "English", selected = language == Language.EN, onClick = { onLanguageChanged(Language.EN) }, testTag = SETTINGS_LANGUAGE_EN_CHIP_TAG)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpaqueChip(label = "Dark", selected = theme == AppTheme.DARK, onClick = { onThemeChanged(AppTheme.DARK) }, testTag = SETTINGS_THEME_DARK_CHIP_TAG)
            OpaqueChip(label = "Light", selected = theme == AppTheme.LIGHT, onClick = { onThemeChanged(AppTheme.LIGHT) }, testTag = SETTINGS_THEME_LIGHT_CHIP_TAG)
        }
    }
}

/**
 * طبق یافته‌ی صریح این قدم (grep روی کل کدبیس): هیچ زیرساخت File Picker ای وجود
 * ندارد (هم‌کلاس محدودیت شناخته‌شده‌ی Attached References در Shot Composer) —
 * دکمه‌ی «انتخاب تصویر» پیام «به‌زودی» نشان می‌دهد (نه یک انتخاب‌گر واقعی). مقدار
 * URI واقعاً در DataStore Persist می‌شود (برای اتصال آینده)؛ نمایش واقعی این
 * تصویر روی Home/پس‌زمینه‌ی بقیه‌ی صفحات هم کار یک قدم بعدی است — این قدم فقط
 * خودِ کارت Settings را می‌سازد.
 */
@Composable
private fun HomeImageCard(
    language: Language,
    imageUri: String?,
    onChooseImage: () -> Unit,
    onRemoveImage: () -> Unit
) {
    SettingsCard(titleKey = "settings.homeImageCardTitle", language = language) {
        // یافته‌ی ۲ appendix ADR-081 (ADR-083): کاشی پیش‌نمایش واقعی ۱۴۰dp طبق
        // mockup — قبلاً فقط رشته‌ی خام content-URI به‌صورت متن نشان داده می‌شد.
        // همان الگوی decode بومی `HomeBackgroundImage` (اکنون `DecodedContentImage`
        // مشترک، ui/home/HomeScreen.kt) بازاستفاده شد.
        Card(
            colors = CardDefaults.cardColors(containerColor = CinemaTheme.extendedColors.inset),
            modifier = Modifier.width(140.dp).height(140.dp).testTag(SETTINGS_HOME_IMAGE_PREVIEW_TAG)
        ) {
            if (imageUri != null) {
                DecodedContentImage(
                    uriString = imageUri,
                    modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)).testTag(SETTINGS_HOME_IMAGE_PREVIEW_IMAGE_TAG),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = CinemaTheme.extendedColors.fg3,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onChooseImage, modifier = Modifier.testTag(SETTINGS_CHOOSE_IMAGE_BUTTON_TAG)) {
                Text(uiString("settings.chooseImageButton", language))
            }
            OutlinedButton(onClick = onRemoveImage, modifier = Modifier.testTag(SETTINGS_REMOVE_IMAGE_BUTTON_TAG)) {
                Text(uiString("settings.removeImageButton", language))
            }
        }
    }
}

@Composable
private fun LayoutVariantsCard(
    language: Language,
    homeLayoutVariant: HomeLayoutVariant,
    onHomeLayoutChanged: (HomeLayoutVariant) -> Unit,
    composerLayoutVariant: ComposerLayoutVariant,
    onComposerLayoutChanged: (ComposerLayoutVariant) -> Unit
) {
    SettingsCard(titleKey = "settings.layoutVariantsCardTitle", language = language) {
        Text(uiString("settings.homeLayoutLabel", language), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpaqueChip(
                label = uiString("settings.homeLayoutHeroOption", language),
                selected = homeLayoutVariant == HomeLayoutVariant.HERO,
                onClick = { onHomeLayoutChanged(HomeLayoutVariant.HERO) },
                testTag = SETTINGS_HOME_LAYOUT_HERO_CHIP_TAG
            )
            OpaqueChip(
                label = uiString("settings.homeLayoutResumeOption", language),
                selected = homeLayoutVariant == HomeLayoutVariant.RESUME,
                onClick = { onHomeLayoutChanged(HomeLayoutVariant.RESUME) },
                testTag = SETTINGS_HOME_LAYOUT_RESUME_CHIP_TAG
            )
        }

        Text(uiString("settings.composerLayoutLabel", language), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpaqueChip(
                label = uiString("settings.composerLayoutTabsOption", language),
                selected = composerLayoutVariant == ComposerLayoutVariant.TABS,
                onClick = { onComposerLayoutChanged(ComposerLayoutVariant.TABS) },
                testTag = SETTINGS_COMPOSER_LAYOUT_TABS_CHIP_TAG
            )
            OpaqueChip(
                label = uiString("settings.composerLayoutAccordionOption", language),
                selected = composerLayoutVariant == ComposerLayoutVariant.ACCORDION,
                onClick = { onComposerLayoutChanged(ComposerLayoutVariant.ACCORDION) },
                testTag = SETTINGS_COMPOSER_LAYOUT_ACCORDION_CHIP_TAG
            )
        }
    }
}

@Composable
private fun AboutCard(language: Language) {
    SettingsCard(titleKey = "settings.aboutCardTitle", language = language) {
        Text(text = "Cinema Shot Generator", style = MaterialTheme.typography.titleMedium)
        Text(text = uiString("settings.aboutTagline", language), style = MaterialTheme.typography.bodySmall, color = CinemaTheme.extendedColors.fg3)
        // یافته‌ی ۳ appendix ADR-081 (ADR-083، رفع جزئی): رشته‌ی نسخه‌ی واقعی
        // (mockup: «v1.0.0 · On-Device») — لوگوی سفارشی Aperture-C همچنان
        // موکول است (بدهی طراحی گرافیکی، نه کدی؛ رجوع به ADR-083).
        Text(
            text = uiTemplate("settings.aboutVersionTemplate", language, "version" to BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall,
            color = CinemaTheme.extendedColors.fg4,
            modifier = Modifier.testTag(SETTINGS_ABOUT_VERSION_TAG)
        )
    }
}
