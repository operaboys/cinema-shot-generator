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
