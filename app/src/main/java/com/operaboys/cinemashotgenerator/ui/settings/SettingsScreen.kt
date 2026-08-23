package com.operaboys.cinemashotgenerator.ui.settings

import android.app.Application
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.BuildConfig
import com.operaboys.cinemashotgenerator.R
import com.operaboys.cinemashotgenerator.data.repository.SecureKeyRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.storybreakdown.AiConnectorProfile
import com.operaboys.cinemashotgenerator.domain.storybreakdown.BUILTIN_AI_CONNECTOR_PROFILES
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
//
// G2 قدم ۳ از ۳ (ADR-101): کارت هشتم اضافه شد — «کلید API سرویس‌های AI». برخلاف
// ۷ کارت بالا، این یکی از یک ViewModel تازه (ApiKeysViewModel، همین پکیج)
// می‌خواند، نه WorkflowViewModel — دلیل کامل (چرا افزودن به WorkflowViewModel
// انضباط دامنه‌ی مستندشده‌ی خودش را نقض می‌کرد) در سربرگ ApiKeysViewModel.kt.

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
const val SETTINGS_TOGGLE_LANGUAGE_BUTTON_TAG = "settings.toggleLanguageButton"
const val SETTINGS_TOGGLE_THEME_BUTTON_TAG = "settings.toggleThemeButton"

fun settingsAutoSaveCadenceChipTag(seconds: Long): String = "settings.autoSaveCadence.$seconds"

// G2 قدم ۳ از ۳ (ADR-101): کارت کلید API — testTag ها با profileId پارامتری‌اند
// (نه هاردکد Claude) چون BUILTIN_AI_CONNECTOR_PROFILES می‌تواند چندتایی باشد.
fun apiKeyFieldTag(profileId: String): String = "settings.apiKeyField.$profileId"
fun apiKeySaveButtonTag(profileId: String): String = "settings.apiKeySaveButton.$profileId"
fun apiKeyDeleteButtonTag(profileId: String): String = "settings.apiKeyDeleteButton.$profileId"
fun apiKeyStatusTag(profileId: String): String = "settings.apiKeyStatus.$profileId"

@Composable
fun SettingsScreen(
    workflowViewModel: WorkflowViewModel,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit = {},
    // G2 قدم ۳: تزریق‌پذیر برای تست — هم‌الگو با assetRepository در
    // CharacterAssetFormScreen.kt. پیش‌فرض null یعنی ApiKeysViewModel خودش
    // SecureKeyRepository واقعی (EncryptedSharedPreferences) می‌سازد.
    secureKeyRepository: SecureKeyRepository? = null,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val apiKeysViewModel: ApiKeysViewModel = viewModel(
        factory = ApiKeysViewModel.factory(application, secureKeyRepository)
    )
    val apiKeysSavedStatus by apiKeysViewModel.savedStatus.collectAsStateWithLifecycle()

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
            backTestTag = SETTINGS_BACK_BUTTON_TAG,
            language = language,
            theme = theme,
            onToggleLanguage = { workflowViewModel.setLanguage(if (language == Language.FA) Language.EN else Language.FA) },
            onToggleTheme = { workflowViewModel.setTheme(if (theme == AppTheme.DARK) AppTheme.LIGHT else AppTheme.DARK) },
            toggleLanguageTestTag = SETTINGS_TOGGLE_LANGUAGE_BUTTON_TAG,
            toggleThemeTestTag = SETTINGS_TOGGLE_THEME_BUTTON_TAG
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

            ApiKeysCard(
                language = language,
                savedStatus = apiKeysSavedStatus,
                onSave = apiKeysViewModel::saveApiKey,
                onDelete = apiKeysViewModel::deleteApiKey
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
 * دکمه‌ی «انتخاب تصویر» یک SAF File Picker واقعی است (ADR-075،
 * `chooseImageLauncher`/`OpenDocument()` — این کامنت پیش‌تر قدیمی مانده
 * بود و هنوز حالت «به‌زودی» را توصیف می‌کرد). مقدار URI در DataStore
 * Persist می‌شود؛ Attached References در Shot Composer همچنان محدودیت
 * جداگانه‌ی خودش را دارد (هنوز بدون File Picker واقعی).
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
        // رفع یافته‌ی #۸ appendix (ADR-090): لوگوی سفارشی Aperture-C که در
        // ADR-083 عمداً موکول شده بود (بدهی طراحی گرافیکی، نه کدی) — حالا که
        // دارایی واقعی وجود دارد، اضافه شد. اندازه/چیدمان (کادر ۶۴dp با
        // پس‌زمینه‌ی inset + حاشیه‌ی hairlineStrong، نشان ۴۴dp داخلش) دقیقاً
        // مطابق مقادیر خودِ mockup (`docs/design/Cinema Studio.html`، بخش
        // is.settings، ردیف «درباره») است.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CinemaTheme.extendedColors.inset),
                border = BorderStroke(1.dp, CinemaTheme.extendedColors.hairlineStrong),
                modifier = Modifier.size(64.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_aperture_c_logo),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
            Column {
                Text(text = "Cinema Shot Generator", style = MaterialTheme.typography.titleMedium)
                Text(text = uiString("settings.aboutTagline", language), style = MaterialTheme.typography.bodySmall, color = CinemaTheme.extendedColors.fg3)
                // یافته‌ی ۳ appendix ADR-081 (ADR-083): رشته‌ی نسخه‌ی واقعی
                // (mockup: «v1.0.0 · On-Device»).
                Text(
                    text = uiTemplate("settings.aboutVersionTemplate", language, "version" to BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelSmall,
                    color = CinemaTheme.extendedColors.fg4,
                    modifier = Modifier.testTag(SETTINGS_ABOUT_VERSION_TAG)
                )
            }
        }
    }
}

/**
 * G2 قدم ۳ از ۳ (ADR-101): برای هر پروفایل در BUILTIN_AI_CONNECTOR_PROFILES یک
 * ردیف مستقل (طبق دستور صریح: خودکار برای چندتایی، نه یک ردیف هاردکد Claude).
 * متن شفافیت هزینه یک‌بار زیر عنوان کارت — نه هشدار تکراری per-ردیف (طبق تصمیم
 * محصولی: «نه هشدار مزاحم در هر استفاده»).
 */
@Composable
private fun ApiKeysCard(
    language: Language,
    savedStatus: Map<String, Boolean>,
    onSave: (profileId: String, apiKey: String) -> Unit,
    onDelete: (profileId: String) -> Unit
) {
    SettingsCard(titleKey = "settings.apiKeysCardTitle", language = language) {
        Text(
            text = uiString("settings.apiKeysCostNotice", language),
            style = MaterialTheme.typography.bodySmall,
            color = CinemaTheme.extendedColors.fg3
        )
        BUILTIN_AI_CONNECTOR_PROFILES.forEachIndexed { index, profile ->
            if (index != 0) {
                HorizontalDivider()
            }
            ApiKeyRow(
                language = language,
                profile = profile,
                isSaved = savedStatus[profile.profileId] == true,
                onSave = { key -> onSave(profile.profileId, key) },
                onDelete = { onDelete(profile.profileId) }
            )
        }
    }
}

/**
 * شرط سخت‌گیرانه‌ی تصمیم محصولی (این قدم مسئول نیست، فقط منبع وضعیت است):
 * دکمه‌ی «ارسال خودکار» در AiStoryBreakdownScreen.kt از همین savedStatus (از
 * طریق hasApiKey واقعی) خوانده می‌شود — تا وقتی isSaved اینجا false است، آن
 * دکمه در سطح UI غیرفعال می‌ماند، نه فقط خطای بعد از کلیک.
 */
@Composable
private fun ApiKeyRow(
    language: Language,
    profile: AiConnectorProfile,
    isSaved: Boolean,
    onSave: (String) -> Unit,
    onDelete: () -> Unit
) {
    var keyInput by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = profile.displayName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = uiString(if (isSaved) "settings.apiKeySavedStatus" else "settings.apiKeyNotSavedStatus", language),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSaved) CinemaTheme.extendedColors.success else CinemaTheme.extendedColors.fg3,
                modifier = Modifier.testTag(apiKeyStatusTag(profile.profileId))
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text(uiString("settings.apiKeyFieldLabel", language)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.weight(1f).testTag(apiKeyFieldTag(profile.profileId))
            )
            Button(
                onClick = { onSave(keyInput); keyInput = "" },
                enabled = keyInput.isNotBlank(),
                modifier = Modifier.testTag(apiKeySaveButtonTag(profile.profileId))
            ) {
                Text(uiString("settings.apiKeySaveButton", language))
            }
        }
        if (isSaved) {
            OutlinedButton(onClick = onDelete, modifier = Modifier.testTag(apiKeyDeleteButtonTag(profile.profileId))) {
                Text(uiString("settings.apiKeyDeleteButton", language))
            }
        }
    }
}
