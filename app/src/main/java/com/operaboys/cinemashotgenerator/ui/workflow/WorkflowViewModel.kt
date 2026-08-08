package com.operaboys.cinemashotgenerator.ui.workflow

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import com.operaboys.cinemashotgenerator.domain.workflow.ComposerLayoutVariant
import com.operaboys.cinemashotgenerator.domain.workflow.HomeLayoutVariant
import com.operaboys.cinemashotgenerator.domain.workflow.ShotListViewMode
import com.operaboys.cinemashotgenerator.domain.workflow.WorkflowState
import com.operaboys.cinemashotgenerator.domain.workflow.WorkflowStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.UUID

/**
 * ViewModel پایه‌ی واحد ۱۶ فاز ۰ — طبق docs/design/README.md بخش State Management.
 * فقط بخش‌هایی که این فاز مسئول آن‌هاست: language/theme/layout A-B picks (واقعی و
 * Persist‌شده) + WorkflowState (اسکلت — nullable تا وقتی هیچ Session واقعی‌ای شروع
 * نشده). سایر State های همان بخش سند (تب فعال Studio/Scene/Composer، فیلترهای
 * Asset، مدل خروجی انتخابی، ...) عمداً اینجا نیستند — سند صریحاً این‌ها را جدا از
 * موارد «Persisted» فهرست کرده (State ناوبری/انتخاب لحظه‌ای، نه Preference)؛ محل
 * طبیعی‌شان `rememberSaveable` در همان Composable مصرف‌کننده است، نه این ViewModel
 * سراسری — جزئیات کامل در ADR-042.
 *
 * تصمیم مستند (ADR-042): خواندن اولیه‌ی DataStore با `runBlocking` در سازنده انجام
 * می‌شود (نه async در init) — عمداً، تا از «فلاش تم/زبان پیش‌فرض قبل از بارگذاری مقدار
 * ذخیره‌شده» روی هر Cold Start جلوگیری شود؛ فایل Preferences کوچک است، پس این یک
 * تأخیر ناچیز و یک‌باره است، نه یک I/O سنگین مسدودکننده. Setter ها همچنان async
 * (`ioScope.launch`) هستند و Job برمی‌گردانند تا فراخوان (یا تست) در صورت نیاز
 * بتواند منتظر تکمیل نوشتن بماند؛ مقدار StateFlow بلافاصله و همزمان (نه بعد از
 * نوشتن) به‌روزرسانی می‌شود — دقیقاً طبق الزام «اعمال فوری، بدون Restart».
 *
 * تصمیم مستند دیگر (ADR-042): scope نوشتن‌ها به‌جای `viewModelScope` هارد-کد شده،
 * پارامتر تزریق‌پذیر `ioScopeOverride` است (پیش‌فرض null ⇒ واقعاً از viewModelScope
 * استفاده می‌شود). دلیل: تست‌های Robolectric با `viewModelScope` (که از
 * `Dispatchers.Main` استفاده می‌کند) در معرض قفل‌شدگی واقعی هستند — Looper اصلی
 * Robolectric پیش‌فرض در حالت PAUSED است، پس `runBlocking { job.join() }` روی همان
 * Thread می‌تواند بی‌نهایت منتظر بماند. تست‌ها به‌جایش `CoroutineScope(Dispatchers.Unconfined)`
 * تزریق می‌کنند — کاملاً از kotlinx-coroutines-core موجود (بدون هیچ وابستگی تست
 * جدید)، که پس از تعلیق واقعی داخل DataStore.edit (که خودش هنوز روی Dispatchers.IO
 * واقعی می‌رود) روی همان Thread پس‌زمینه به کار خود ادامه می‌دهد، نه اینکه منتظر
 * Pump شدن Looper اصلی بماند.
 */
class WorkflowViewModel(
    application: Application,
    private val dataStore: DataStore<Preferences> = application.workflowDataStore,
    private val clock: () -> String = ::nowIso8601,
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    private val initialPrefs: Preferences = runBlocking { dataStore.data.first() }

    private val _language = MutableStateFlow(
        initialPrefs[WorkflowPrefKeys.LANGUAGE]?.let { raw -> Language.entries.find { it.name == raw } }
            ?: Language.FA
    )
    val language: StateFlow<Language> = _language.asStateFlow()

    private val _theme = MutableStateFlow(
        initialPrefs[WorkflowPrefKeys.THEME]?.let { raw -> AppTheme.entries.find { it.name == raw } }
            ?: AppTheme.DARK
    )
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _homeLayoutVariant = MutableStateFlow(
        initialPrefs[WorkflowPrefKeys.HOME_LAYOUT_VARIANT]?.let { raw -> HomeLayoutVariant.entries.find { it.name == raw } }
            ?: HomeLayoutVariant.HERO
    )
    val homeLayoutVariant: StateFlow<HomeLayoutVariant> = _homeLayoutVariant.asStateFlow()

    private val _composerLayoutVariant = MutableStateFlow(
        initialPrefs[WorkflowPrefKeys.COMPOSER_LAYOUT_VARIANT]?.let { raw -> ComposerLayoutVariant.entries.find { it.name == raw } }
            ?: ComposerLayoutVariant.TABS
    )
    val composerLayoutVariant: StateFlow<ComposerLayoutVariant> = _composerLayoutVariant.asStateFlow()

    // واحد ۱۶ فاز ۶ — قدم ۱ (صفحه‌ی Settings): کارت «نمایش» — طبق یافته‌ی صریح
    // این قدم (grep روی ui/theme/Theme.kt)، هیچ زیرساخت واقعی‌ای برای Dynamic
    // Font/Min Touch Target/Reduced Motion در این پروژه وجود ندارد؛ این سه فلگ
    // واقعاً Persist می‌شوند (هم‌الگو دقیق با ۴ فیلد بالا) اما فعلاً هیچ اثر
    // Runtime ای در جای دیگری از اپ ندارند — محدودیت شناخته‌شده و صریحاً مستند،
    // نه پنهان‌شده (جزئیات در docs/adr/058-...md).
    private val _dynamicFontEnabled = MutableStateFlow(initialPrefs[WorkflowPrefKeys.DYNAMIC_FONT_ENABLED] ?: false)
    val dynamicFontEnabled: StateFlow<Boolean> = _dynamicFontEnabled.asStateFlow()

    private val _minTouchTargetEnabled = MutableStateFlow(initialPrefs[WorkflowPrefKeys.MIN_TOUCH_TARGET_ENABLED] ?: false)
    val minTouchTargetEnabled: StateFlow<Boolean> = _minTouchTargetEnabled.asStateFlow()

    private val _reducedMotionEnabled = MutableStateFlow(initialPrefs[WorkflowPrefKeys.REDUCED_MOTION_ENABLED] ?: false)
    val reducedMotionEnabled: StateFlow<Boolean> = _reducedMotionEnabled.asStateFlow()

    /**
     * فاصله‌ی زمانی Timer واقعی Auto-Save (`StudioShell`، `AutoSaveManager.touch`) —
     * پیش‌فرض ۳۰ دقیقاً هم‌تراز `AutoSaveManager.intervalSeconds` پیش‌فرض. جزئیات
     * کامل تصمیم در docs/adr/058-unit16-phase6-step1-settings-autosave.md.
     */
    private val _autoSaveCadenceSeconds = MutableStateFlow(initialPrefs[WorkflowPrefKeys.AUTO_SAVE_CADENCE_SECONDS] ?: 30L)
    val autoSaveCadenceSeconds: StateFlow<Long> = _autoSaveCadenceSeconds.asStateFlow()

    /**
     * «پرش آزاد بین مراحل» — پیش‌فرض true چون رفتار واقعیِ فعلیِ Studio همین است
     * (تب‌های Story/DNA/Scenes/... همیشه آزادانه قابل‌کلیک‌اند، بدون هیچ Gate ای).
     * این سوییچ واقعاً Persist می‌شود، اما هیچ منطق Gate/هشدار واقعی‌ای در جای
     * دیگری از این اپ برای اجرای آن وجود ندارد — محدودیت شناخته‌شده و مستند.
     */
    private val _allowFreeStepJump = MutableStateFlow(initialPrefs[WorkflowPrefKeys.ALLOW_FREE_STEP_JUMP] ?: true)
    val allowFreeStepJump: StateFlow<Boolean> = _allowFreeStepJump.asStateFlow()

    /**
     * تصویر پس‌زمینه‌ی Home — طبق سند طراحی هم روی Home (Full-bleed) هم Blur‌شده
     * در پس‌زمینه‌ی بقیه‌ی صفحات استفاده می‌شود؛ چون هیچ زیرساخت File Picker ای در
     * کل این کدبیس وجود ندارد (grep تأییدشده، هم‌کلاس محدودیت شناخته‌شده‌ی Attached
     * References در Shot Composer)، این قدم فقط خودِ مقدار را Persist می‌کند —
     * نمایش واقعی آن روی Home/بقیه‌ی صفحات کار یک قدم بعدی است.
     */
    private val _homeScreenImageUri = MutableStateFlow(initialPrefs[WorkflowPrefKeys.HOME_SCREEN_IMAGE_URI])
    val homeScreenImageUri: StateFlow<String?> = _homeScreenImageUri.asStateFlow()

    /** null یعنی هنوز هیچ Session واقعی‌ای (ورود به یک پروژه‌ی مشخص) شروع نشده. */
    private val _workflowState = MutableStateFlow<WorkflowState?>(null)
    val workflowState: StateFlow<WorkflowState?> = _workflowState.asStateFlow()

    fun setLanguage(language: Language): Job {
        _language.value = language
        return ioScope.launch { dataStore.edit { it[WorkflowPrefKeys.LANGUAGE] = language.name } }
    }

    fun setTheme(theme: AppTheme): Job {
        _theme.value = theme
        return ioScope.launch { dataStore.edit { it[WorkflowPrefKeys.THEME] = theme.name } }
    }

    fun setHomeLayoutVariant(variant: HomeLayoutVariant): Job {
        _homeLayoutVariant.value = variant
        return ioScope.launch { dataStore.edit { it[WorkflowPrefKeys.HOME_LAYOUT_VARIANT] = variant.name } }
    }

    fun setComposerLayoutVariant(variant: ComposerLayoutVariant): Job {
        _composerLayoutVariant.value = variant
        return ioScope.launch { dataStore.edit { it[WorkflowPrefKeys.COMPOSER_LAYOUT_VARIANT] = variant.name } }
    }

    fun setDynamicFontEnabled(enabled: Boolean): Job {
        _dynamicFontEnabled.value = enabled
        return ioScope.launch { dataStore.edit { it[WorkflowPrefKeys.DYNAMIC_FONT_ENABLED] = enabled } }
    }

    fun setMinTouchTargetEnabled(enabled: Boolean): Job {
        _minTouchTargetEnabled.value = enabled
        return ioScope.launch { dataStore.edit { it[WorkflowPrefKeys.MIN_TOUCH_TARGET_ENABLED] = enabled } }
    }

    fun setReducedMotionEnabled(enabled: Boolean): Job {
        _reducedMotionEnabled.value = enabled
        return ioScope.launch { dataStore.edit { it[WorkflowPrefKeys.REDUCED_MOTION_ENABLED] = enabled } }
    }

    fun setAutoSaveCadenceSeconds(seconds: Long): Job {
        _autoSaveCadenceSeconds.value = seconds
        return ioScope.launch { dataStore.edit { it[WorkflowPrefKeys.AUTO_SAVE_CADENCE_SECONDS] = seconds } }
    }

    fun setAllowFreeStepJump(allow: Boolean): Job {
        _allowFreeStepJump.value = allow
        return ioScope.launch { dataStore.edit { it[WorkflowPrefKeys.ALLOW_FREE_STEP_JUMP] = allow } }
    }

    fun setHomeScreenImageUri(uri: String?): Job {
        _homeScreenImageUri.value = uri
        return ioScope.launch {
            dataStore.edit {
                if (uri != null) it[WorkflowPrefKeys.HOME_SCREEN_IMAGE_URI] = uri else it.remove(WorkflowPrefKeys.HOME_SCREEN_IMAGE_URI)
            }
        }
    }

    /**
     * واحد ۱۶ فاز ۴ قدم ۲: سوییچ Grid/Timeline فهرست شات‌ها — طبق 🆕 بلوپرینت ۱۶
     * («انتخاب کاربر باید در طول یک نشست حفظ شود، نه هر بار بازنشانی به پیش‌فرض»).
     * `WorkflowState.shotListViewMode` از فاز ۰ موجود بود ولی هیچ Setter ای نداشت.
     * عمداً در همین ViewModel سراسری (نه `rememberSaveable` محلی Scene Detail) —
     * چون `workflowState` خودش دقیقاً «طول یک نشست Studio» را نمایندگی می‌کند (یک‌بار
     * در ورود به Studio ساخته می‌شود، نه به‌ازای هر بازدید Scene Detail/Shot Composer)؛
     * این انتخاب یک اثر جانبی مفید هم دارد: بین رفت‌وبرگشت Shot List↔Shot Composer هم
     * حفظ می‌ماند، بدون نیاز به راه‌حل جداگانه. بدون نوشتن در DataStore — این فیلد
     * Preference بلندمدت نیست، دقیقاً هم‌جنس بقیه‌ی WorkflowState.
     */
    fun setShotListViewMode(mode: ShotListViewMode) {
        _workflowState.value = _workflowState.value?.copy(shotListViewMode = mode)
    }

    /** انتخاب مدل هدف در Output Delivery — هم‌الگو دقیق با setShotListViewMode بالا. */
    fun setSelectedModelProfileId(profileId: String) {
        _workflowState.value = _workflowState.value?.copy(selectedModelProfileId = profileId)
    }

    /** شروع یک Session گردش کار واقعی برای یک projectId مشخص — فراخوان واقعی این تابع (هنگام ورود به Studio) کار فاز بعدی است. */
    fun startWorkflowSession(projectId: String) {
        _workflowState.value = WorkflowState(
            sessionId = "session_" + UUID.randomUUID().toString().replace("-", "").take(12),
            projectId = projectId,
            currentStep = WorkflowStep.STORY_WIZARD,
            stepStatus = emptyMap(),
            startedAt = clock(),
            lastActionAt = clock()
        )
    }

    companion object {
        /**
         * AndroidViewModelFactory رسمی رفلکشن‌محور فقط سازنده‌ی تک‌پارامتریِ Application
         * را می‌شناسد؛ چون این سازنده پارامترهای پیش‌فرض اضافه (dataStore/clock، برای
         * تست‌پذیری) دارد، یک Factory دستی ساده لازم است.
         */
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    WorkflowViewModel(application) as T
            }
    }
}

private fun nowIso8601(): String = Instant.now().toString()
