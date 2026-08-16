package com.operaboys.cinemashotgenerator.ui.storybreakdown

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.domain.storybreakdown.JsonDiagnosis
import com.operaboys.cinemashotgenerator.domain.storybreakdown.ProcessAiResponseResult
import com.operaboys.cinemashotgenerator.domain.storybreakdown.StoryBreakdownRequest
import com.operaboys.cinemashotgenerator.domain.storybreakdown.StoryBreakdownResult
import com.operaboys.cinemashotgenerator.domain.storybreakdown.attemptAutoFix
import com.operaboys.cinemashotgenerator.domain.storybreakdown.buildStoryBreakdownPrompt
import com.operaboys.cinemashotgenerator.domain.storybreakdown.processAiResponse
import com.operaboys.cinemashotgenerator.domain.storybreakdown.smartCombineChunks
import com.operaboys.cinemashotgenerator.domain.storybreakdown.validateChunksComplete
import com.operaboys.cinemashotgenerator.domain.storybreakdown.validateFreeformStoryLength
import com.operaboys.cinemashotgenerator.domain.storybreakdown.validateHighShotCount
import com.operaboys.cinemashotgenerator.domain.storybreakdown.validateTargetShotCountRange
import com.operaboys.cinemashotgenerator.ui.story.defaultStoryContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

// واحد ۱۶ فاز ۲ — قدم ۲: ViewModel واقعی صفحه‌ی AI Story Breakdown — اولین اتصال UI
// کل زنجیره‌ی واحد ۰۱ب (PromptBuilder→ChunkCombiner→JsonDoctor→StoryToDomainMapper)
// و اولین محل واقعی که خروجی این زنجیره را در AssetRepository/SceneRepository/
// ShotRepository ذخیره می‌کند. جزئیات کامل هر تصمیم در
// docs/adr/046-unit16-phase2-step2-ai-story-breakdown.md.

enum class BreakdownPhase { WRITE_STORY, PASTE_RESPONSE, FINAL_REVIEW }

class AiStoryBreakdownViewModel(
    application: Application,
    private val projectId: String,
    private val storyRepository: StoryRepository = StoryRepository(
        AppDatabase.getInstance(application).storyDao(),
        AppDatabase.getInstance(application).storyBreakdownSessionDao()
    ),
    private val assetRepository: AssetRepository = AssetRepository(AppDatabase.getInstance(application).assetDao()),
    private val sceneRepository: SceneRepository = SceneRepository(AppDatabase.getInstance(application).sceneDao()),
    private val shotRepository: ShotRepository = ShotRepository(AppDatabase.getInstance(application).shotDao()),
    private val clock: () -> String = ::nowIso8601,
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    private val _phase = MutableStateFlow(BreakdownPhase.WRITE_STORY)
    val phase: StateFlow<BreakdownPhase> = _phase.asStateFlow()

    private val _freeformStory = MutableStateFlow("")
    val freeformStory: StateFlow<String> = _freeformStory.asStateFlow()

    // رفع G14 «کاندید قدم بعدی» (ADR-064 تصمیم ۱۲، ADR-092): Rule 1 — هم‌الگو
    // دقیق با targetShotCountError پایین (خطای واقعی + Blocking دکمه‌ی «تولید
    // Prompt»).
    private val _freeformStoryError = MutableStateFlow<String?>(null)
    val freeformStoryError: StateFlow<String?> = _freeformStoryError.asStateFlow()

    private val _targetShotCount = MutableStateFlow(10)
    val targetShotCount: StateFlow<Int> = _targetShotCount.asStateFlow()

    // رفع G14 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، بخش الف،
    // تنها یافته‌ی 🟠 آن بخش): قبلاً setTargetShotCount با coerceIn(1,150) مقدار
    // خارج از محدوده را بی‌صدا کوتاه می‌کرد — validateTargetShotCountRange
    // (Rule 2 واقعی، PromptBuilder.kt) هرگز صدا زده نمی‌شد. اکنون مقدار خام کاربر
    // نگه داشته می‌شود و خطای واقعی این Rule در targetShotCountError نمایش
    // داده می‌شود؛ دکمه‌ی «تولید Prompt» تا وقتی خطا برطرف نشود غیرفعال است.
    private val _targetShotCountError = MutableStateFlow<String?>(null)
    val targetShotCountError: StateFlow<String?> = _targetShotCountError.asStateFlow()

    // رفع G14 «کاندید قدم بعدی» (ADR-064 تصمیم ۱۲، ADR-092): Rule 3 — روی همان
    // فیلد targetShotCount، اما فقط Warning (دکمه را مسدود نمی‌کند).
    private val _highShotCountWarning = MutableStateFlow<String?>(null)
    val highShotCountWarning: StateFlow<String?> = _highShotCountWarning.asStateFlow()

    private val _defaultShotDurationSeconds = MutableStateFlow(4f)
    val defaultShotDurationSeconds: StateFlow<Float> = _defaultShotDurationSeconds.asStateFlow()

    private val _generatedPrompt = MutableStateFlow<String?>(null)
    val generatedPrompt: StateFlow<String?> = _generatedPrompt.asStateFlow()

    /** تکه‌های قبلاً تأییدشده (طبق دکمه‌ی «تکه‌ی جدید») — متن در‌حال‌تایپ فعلی در [currentChunkInput] جداست. */
    private val _chunks = MutableStateFlow<List<String>>(emptyList())
    val chunks: StateFlow<List<String>> = _chunks.asStateFlow()

    private val _currentChunkInput = MutableStateFlow("")
    val currentChunkInput: StateFlow<String> = _currentChunkInput.asStateFlow()

    // رفع G14 «کاندید بهبود UX آینده» (ADR-064 تصمیم ۱۲، ADR-092): Rule 6 —
    // هشدار پیش از اینکه کاربر یک خطای Parse JSON نامفهوم ببیند (وقتی آخرین
    // تکه هنوز با [CONTINUE] تمام شده و فراموش کرده تکه‌ی بعدی را اضافه کند).
    private val _chunksCompleteWarning = MutableStateFlow<String?>(null)
    val chunksCompleteWarning: StateFlow<String?> = _chunksCompleteWarning.asStateFlow()

    private val _repairDiagnosis = MutableStateFlow<JsonDiagnosis?>(null)
    val repairDiagnosis: StateFlow<JsonDiagnosis?> = _repairDiagnosis.asStateFlow()

    private val _processingError = MutableStateFlow<String?>(null)
    val processingError: StateFlow<String?> = _processingError.asStateFlow()

    private val _breakdownResult = MutableStateFlow<StoryBreakdownResult?>(null)
    val breakdownResult: StateFlow<StoryBreakdownResult?> = _breakdownResult.asStateFlow()

    private val _saveCompleted = MutableStateFlow(false)
    val saveCompleted: StateFlow<Boolean> = _saveCompleted.asStateFlow()

    init {
        ioScope.launch {
            storyRepository.loadBreakdownSession(projectId).getOrNull()?.let { session ->
                _freeformStory.value = session.freeformStory
                _freeformStoryError.value = validateFreeformStoryLength(session.freeformStory)?.message
                _targetShotCount.value = session.targetShotCount
                _targetShotCountError.value = validateTargetShotCountRange(session.targetShotCount)?.message
                _highShotCountWarning.value = validateHighShotCount(session.targetShotCount)?.message
                _defaultShotDurationSeconds.value = session.defaultShotDurationSeconds
            }
        }
    }

    fun setPhase(newPhase: BreakdownPhase) {
        _phase.value = newPhase
    }

    fun setFreeformStory(text: String) {
        _freeformStory.value = text
        _freeformStoryError.value = validateFreeformStoryLength(text)?.message
        saveSession()
    }

    fun setTargetShotCount(count: Int) {
        _targetShotCount.value = count
        _targetShotCountError.value = validateTargetShotCountRange(count)?.message
        _highShotCountWarning.value = validateHighShotCount(count)?.message
        saveSession()
    }

    fun setDefaultShotDurationSeconds(seconds: Float) {
        _defaultShotDurationSeconds.value = seconds.coerceIn(1f, 60f)
        saveSession()
    }

    /** Auto-Save بی‌صدا — همان انضباط StoryViewModel/ADR-045. */
    private fun saveSession() {
        ioScope.launch {
            storyRepository.saveBreakdownSession(
                projectId, _freeformStory.value, _targetShotCount.value, _defaultShotDurationSeconds.value
            )
        }
    }

    /**
     * تولید Prompt واقعی — طبق یادآوری صریح دستور کار: sendToAiConnector هنوز
     * TODO() است، پس این تابع فقط متن Prompt را می‌سازد (برای Copy دستی کاربر)، نه
     * ارسال خودکار به یک API. StoryContext از StoryRepository خوانده می‌شود؛ اگر هنوز
     * ذخیره نشده (کاربر وارد Story Tab نشده)، همان پیش‌فرض خنثی StoryViewModel
     * استفاده می‌شود (defaultStoryContext مشترک).
     */
    fun generatePrompt() {
        if (_targetShotCountError.value != null || _freeformStoryError.value != null) return
        ioScope.launch {
            val storyContext = storyRepository.loadStoryContext(projectId).getOrNull() ?: defaultStoryContext(clock)
            val request = StoryBreakdownRequest(
                storyContext = storyContext,
                freeformStory = _freeformStory.value,
                targetShotCount = _targetShotCount.value,
                defaultShotDurationSeconds = _defaultShotDurationSeconds.value
            )
            _generatedPrompt.value = buildStoryBreakdownPrompt(request)
        }
    }

    fun setCurrentChunkInput(text: String) {
        _currentChunkInput.value = text
    }

    /** دکمه‌ی «تکه‌ی جدید» — طبق ChunkCombiner، متن فعلی را به فهرست تکه‌ها اضافه و جعبه را برای تکه‌ی بعدی خالی می‌کند. */
    fun addChunk() {
        if (_currentChunkInput.value.isBlank()) return
        _chunks.value = _chunks.value + _currentChunkInput.value
        _currentChunkInput.value = ""
        _chunksCompleteWarning.value = validateChunksComplete(_chunks.value)?.message
    }

    /**
     * دکمه‌ی «ادامه» — طبق تصمیم مستند، متن فعلی جعبه (اگر خالی نباشد) خودکار به‌عنوان
     * آخرین تکه لحاظ می‌شود (کاربر برای پاسخ تک‌تکه‌ای مجبور به کلیک «تکه‌ی جدید»
     * نیست). processAiResponse (زنجیره‌ی کامل Combine→Repair→Parse→Map) صدا زده
     * می‌شود؛ سه حالت خروجی مدیریت می‌شود.
     */
    fun processResponse() {
        val allChunks = _chunks.value + listOfNotNull(_currentChunkInput.value.takeIf { it.isNotBlank() })
        _chunksCompleteWarning.value = validateChunksComplete(allChunks)?.message
        when (val result = processAiResponse(allChunks, _targetShotCount.value)) {
            is ProcessAiResponseResult.Success -> {
                _breakdownResult.value = result.result
                _repairDiagnosis.value = null
                _processingError.value = null
                _phase.value = BreakdownPhase.FINAL_REVIEW
            }
            is ProcessAiResponseResult.NeedsManualRepair -> {
                _repairDiagnosis.value = result.diagnosis
            }
            is ProcessAiResponseResult.MissingRequiredKeys -> {
                _processingError.value = result.issue.message
            }
        }
    }

    fun clearProcessingError() {
        _processingError.value = null
    }

    /**
     * دکمه‌ی «تعمیر خودکار» در Modal — attemptAutoFix روی متن ترکیب‌شده‌ی فعلی صدا زده
     * می‌شود؛ نتیجه (چه موفق چه نه) در همان جعبه‌ی متنی Phase ۲ جایگزین می‌شود تا
     * کاربر نتیجه را ببیند و «ادامه» را دوباره بزند — تکه‌های قبلی هم‌زمان جمع
     * می‌شوند (چون بعد از ترکیب، دیگر معنای «تکه‌ی جدا» ندارند).
     */
    fun attemptAutoFixAndCollapse() {
        val diagnosis = _repairDiagnosis.value ?: return
        val combined = smartCombineChunks(_chunks.value + listOfNotNull(_currentChunkInput.value.takeIf { it.isNotBlank() }))
        val fixed = attemptAutoFix(combined, diagnosis)
        _currentChunkInput.value = fixed ?: combined
        _chunks.value = emptyList()
        _repairDiagnosis.value = null
    }

    /** دکمه‌ی «ویرایش دستی» — Modal بسته می‌شود، تکه‌ها هم‌زمان جمع می‌شوند تا کاربر کل متن را در یک جعبه‌ی واحد ویرایش کند. */
    fun dismissRepairModalForManualEdit() {
        val combined = smartCombineChunks(_chunks.value + listOfNotNull(_currentChunkInput.value.takeIf { it.isNotBlank() }))
        _currentChunkInput.value = combined
        _chunks.value = emptyList()
        _repairDiagnosis.value = null
    }

    /**
     * دکمه‌ی «تأیید و ادامه» فاز ۳ — ذخیره‌ی واقعی در سه Repository. ترتیب Scene قبل
     * از Shot عمدی است (طبق هشدار ADR-017: ForeignKey Shot→Scene، باید Scene قبلاً
     * وجود داشته باشد).
     *
     * Job برگردانده می‌شود (نه Unit) — هم‌الگو با exportProject/regenerate (رفع G16
     * ممیزی post-Unit16، docs/adr/063-...) — فقط تا AiStoryBreakdownViewModelFactoryTest.kt
     * بتواند .join() کند و مطمئن شود همه‌ی نوشتن‌ها (که با تزریق جزئی، بین چند
     * Repository/دیتابیس متفاوت پخش می‌شوند) واقعاً قبل از خواندن نتیجه کامل شده‌اند.
     * تنها فراخوان تولیدی (AiStoryBreakdownScreen.kt، `viewModel::confirmAndSave`)
     * مقدار برگشتی را نادیده می‌گیرد — بدون تغییر رفتار.
     */
    fun confirmAndSave(): Job {
        val result = _breakdownResult.value ?: return Job().apply { complete() }
        return ioScope.launch {
            result.characters.forEach { assetRepository.saveCharacterAsset(projectId, it) }
            result.locations.forEach { assetRepository.saveLocationAsset(projectId, it) }
            result.objects.forEach { assetRepository.saveObjectAsset(projectId, it) }
            result.scenes.forEach { sceneRepository.saveScene(projectId, it) }
            result.shots.forEach { shotRepository.saveShot(it) }
            _saveCompleted.value = true
        }
    }

    companion object {
        // رفع G16 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md،
        // docs/adr/063-...): قبلاً با `if (args.size == 4)` رفتار همه‌یا‌هیچ داشت —
        // اگر فقط ۱ تا ۳ از ۴ Repository داده می‌شد، هر ۴ تا (حتی آن‌هایی که واقعاً
        // تزریق شده بودند) بی‌صدا دور ریخته می‌شدند و به‌جایش پیش‌فرض
        // AppDatabase.getInstance ساخته می‌شد. رفع شد: هم‌الگو دقیق با
        // SceneDetailViewModel.factory (که همین باگ را قبلاً برای دو Repository
        // داشت و رفع شد) — هرکدام مستقل با ?: به پیش‌فرض خودش می‌رسد.
        fun factory(
            application: Application,
            projectId: String,
            storyRepository: StoryRepository? = null,
            assetRepository: AssetRepository? = null,
            sceneRepository: SceneRepository? = null,
            shotRepository: ShotRepository? = null,
            // فقط برای تست مستقیم factory (نه Compose) — رفع G16 نیازمند اثبات
            // رفتاری است که تزریق جزئی واقعاً استفاده می‌شود، نه دور ریخته شدن؛ بدون
            // این پارامتر، init{} این ViewModel (که یک DAO Suspend واقعی صدا می‌زند)
            // روی viewModelScope واقعی اجرا می‌شود و تست را غیرقابل‌اطمینان می‌کند —
            // هم‌دلیل دقیق مستندشده در OutputDeliveryViewModelTest. فراخوان تولیدی
            // (AiStoryBreakdownScreen.kt) این پارامتر را نمی‌دهد — بدون تغییر رفتار.
            ioScopeOverride: CoroutineScope? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val anyInjected = storyRepository != null || assetRepository != null || sceneRepository != null || shotRepository != null
                    return if (anyInjected) {
                        AiStoryBreakdownViewModel(
                            application = application,
                            projectId = projectId,
                            storyRepository = storyRepository ?: StoryRepository(
                                AppDatabase.getInstance(application).storyDao(),
                                AppDatabase.getInstance(application).storyBreakdownSessionDao()
                            ),
                            assetRepository = assetRepository ?: AssetRepository(AppDatabase.getInstance(application).assetDao()),
                            sceneRepository = sceneRepository ?: SceneRepository(AppDatabase.getInstance(application).sceneDao()),
                            shotRepository = shotRepository ?: ShotRepository(AppDatabase.getInstance(application).shotDao()),
                            ioScopeOverride = ioScopeOverride
                        )
                    } else {
                        AiStoryBreakdownViewModel(application, projectId, ioScopeOverride = ioScopeOverride)
                    } as T
                }
            }
    }
}

private fun nowIso8601(): String = Instant.now().toString()
