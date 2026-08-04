package com.operaboys.cinemashotgenerator.ui.story

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.story.CompletionStatus
import com.operaboys.cinemashotgenerator.domain.story.Genre
import com.operaboys.cinemashotgenerator.domain.story.NarrativeIntensity
import com.operaboys.cinemashotgenerator.domain.story.StoryContext
import com.operaboys.cinemashotgenerator.domain.story.StoryType
import com.operaboys.cinemashotgenerator.domain.story.VisualIntent
import com.operaboys.cinemashotgenerator.domain.story.deriveCompletionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

// واحد ۱۶ فاز ۲ — قدم ۱ — بخش ب: ViewModel واقعی Story Tab.
//
// تصمیم مستند (docs/adr/045-...md): این ViewModel به‌ازای هر پروژه (projectId) یک
// نمونه‌ی مجزاست — نه یک نمونه‌ی مشترک سطح اپ مثل WorkflowViewModel/
// ProjectListViewModel — چون داده‌اش (StoryContext) واقعاً محدود به یک پروژه است.
// `viewModel(factory = StoryViewModel.factory(...))` داخل خودِ Composable مصرف‌کننده
// (StoryTabContent) فراخوانی می‌شود؛ چون آن Composable همیشه داخل
// `composable<Studio> { ... }` (که NavBackStackEntry مجزا به‌ازای هر projectId متفاوت
// می‌سازد) رندر می‌شود، این نمونه به‌طور طبیعی هر بار که کاربر وارد پروژه‌ی دیگری شود
// دوباره ساخته می‌شود — بدون نیاز به منطق «سوییچ پروژه»ی دستی مشابه
// WorkflowViewModel.startWorkflowSession.
//
// تصمیم مستند دوم: بارگذاری اولیه Async است (نه runBlocking مثل WorkflowViewModel) —
// چون منبع این‌جا یک Query واقعی Room است (نه یک فایل کوچک DataStore)، Block کردن
// Thread اصلی برای I/O دیتابیس روش درستی نیست؛ به‌جایش مقدار پیش‌فرض بلافاصله در
// دسترس است (`isLoaded=false` تا وقتی داده‌ی واقعی — اگر وجود داشته باشد — بارگذاری
// و جایگزین شود).
//
// تصمیم مستند سوم (پیش‌فرض StoryType/Mood): `StoryContext` دو فیلد الزامی بدون
// پیش‌فرض دارد (`storyType`, `moodPrimary`) — برای یک پروژه‌ی تازه (هنوز هیچ
// StoryContext ای ذخیره نشده)، باید یک مقدار اولیه‌ی معقول انتخاب شود تا شیء قابل
// نمایش/ویرایش باشد. انتخاب شد: `StoryType.NARRATIVE` (رایج‌ترین/عمومی‌ترین نوع) و
// `Mood.CALM` (خنثی‌ترین گزینه، نه یک Mood بارگذاری‌شده‌ی احساسی/ژانری خاص) —
// کاربر معمولاً همان قدم اول این هر دو را واقعاً انتخاب می‌کند.
class StoryViewModel(
    application: Application,
    private val projectId: String,
    private val repository: StoryRepository = StoryRepository(
        AppDatabase.getInstance(application).storyDao(),
        AppDatabase.getInstance(application).storyBreakdownSessionDao()
    ),
    private val clock: () -> String = ::nowIso8601,
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    private val _storyContext = MutableStateFlow(defaultStoryContext(clock))
    val storyContext: StateFlow<StoryContext> = _storyContext.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        ioScope.launch {
            repository.loadStoryContext(projectId).getOrNull()?.let { loaded ->
                _storyContext.value = loaded
            }
            _isLoaded.value = true
        }
    }

    fun setStoryType(type: StoryType) = updateAndSave { it.copy(storyType = type) }
    fun toggleGenre(genre: Genre) = updateAndSave {
        it.copy(genre = if (genre in it.genre) it.genre - genre else it.genre + genre)
    }
    fun setMoodPrimary(mood: Mood) = updateAndSave { it.copy(moodPrimary = mood) }
    fun setMoodSecondary(mood: Mood?) = updateAndSave { it.copy(moodSecondary = mood) }
    fun setNarrativeIntensity(intensity: NarrativeIntensity) = updateAndSave { it.copy(narrativeIntensity = intensity) }
    fun setVisualIntent(intent: VisualIntent) = updateAndSave { it.copy(visualIntent = intent) }

    /** Auto-Save بی‌صدا — طبق رفتار سراسری بلوپرینت ۱۶ («Auto-Save کاملاً خودکار و بی‌صداست»)، بدون دکمه‌ی Submit صریح. */
    private fun updateAndSave(transform: (StoryContext) -> StoryContext) {
        val updated = transform(_storyContext.value).let { it.copy(completionStatus = deriveCompletionStatus(it)) }
        _storyContext.value = updated
        ioScope.launch { repository.saveStoryContext(projectId, updated) }
    }

    companion object {
        /**
         * `repository` تزریق‌پذیر است — تصمیم مستند (docs/adr/045-...md، یافته‌ی
         * تست واقعی): پارامتر پیش‌فرض `StoryRepository` روی سازنده‌ی `StoryViewModel`
         * (`AppDatabase.getInstance(application)`) به‌تنهایی کافی نیست، چون این
         * Factory از داخل یک Composable (`viewModel(factory=...)`) صدا زده می‌شود و
         * بدون این پارامتر، هیچ راهی برای تست‌ها برای جایگزینی دیتابیس In-Memory
         * وجود نداشت — نوشتن‌ها بی‌صدا به دیتابیس واقعی Singleton دستگاه می‌رفتند،
         * نه دیتابیس In-Memory تست (باگ واقعی کشف‌شده، نه فرضی).
         */
        fun factory(application: Application, projectId: String, repository: StoryRepository? = null): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (repository != null) StoryViewModel(application, projectId, repository)
                        else StoryViewModel(application, projectId)
                    ) as T
            }
    }
}

/** internal (نه private) — AiStoryBreakdownViewModel (واحد ۱۶ فاز ۲ قدم ۲) هم برای «هنوز StoryContext ای ذخیره نشده» از همین پیش‌فرض استفاده می‌کند. */
internal fun defaultStoryContext(clock: () -> String): StoryContext {
    val context = StoryContext(
        storyType = StoryType.NARRATIVE,
        genre = emptyList(),
        moodPrimary = Mood.CALM,
        moodSecondary = null,
        createdAt = clock(),
        completionStatus = CompletionStatus.PARTIAL
    )
    return context.copy(completionStatus = deriveCompletionStatus(context))
}

private fun nowIso8601(): String = Instant.now().toString()
