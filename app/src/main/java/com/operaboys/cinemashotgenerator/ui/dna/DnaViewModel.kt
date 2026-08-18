package com.operaboys.cinemashotgenerator.ui.dna

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.domain.dna.ColorTemperature
import com.operaboys.cinemashotgenerator.domain.dna.ContrastLevel
import com.operaboys.cinemashotgenerator.domain.dna.CoreIdentity
import com.operaboys.cinemashotgenerator.domain.dna.GlobalMoodBase
import com.operaboys.cinemashotgenerator.domain.dna.updateCoreIdentity
import com.operaboys.cinemashotgenerator.domain.dna.LightingPreference
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.dna.MasterPalette
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.dna.OutputConstraints
import com.operaboys.cinemashotgenerator.domain.dna.AspectRatio
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.dna.QualityDirectives
import com.operaboys.cinemashotgenerator.domain.dna.RealismLevel
import com.operaboys.cinemashotgenerator.domain.dna.SaturationLevel
import com.operaboys.cinemashotgenerator.domain.dna.StyleConsistency
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// واحد ۱۶ فاز ۲ — قدم ۳ (آخرین قدم فاز ۲): ViewModel واقعی Tab «DNA» —
// اولین اتصال UI به `domain/dna/ProjectDna.kt` (واحد ۰۲) و
// `data/repository/ProjectDnaRepository.kt` (واحد ۱۵، از قبل کامل و آماده).
// هم‌الگو با StoryViewModel.kt (ADR-045) و AiStoryBreakdownViewModel.kt
// (ADR-046): تزریق‌پذیری Repository از همان ابتدا، بارگذاری Async، Auto-Save
// بی‌صدا روی هر تغییر فیلد. جزئیات کامل تصمیمات در
// docs/adr/047-unit16-phase2-step3-dna-tab.md.
class DnaViewModel(
    application: Application,
    private val projectId: String,
    private val repository: ProjectDnaRepository = ProjectDnaRepository(
        AppDatabase.getInstance(application).projectDnaDao()
    ),
    private val idProvider: () -> String = ::randomDnaId,
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    private val _dna = MutableStateFlow(defaultProjectDna(projectId, idProvider))
    val dna: StateFlow<ProjectDna> = _dna.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    // متن خام فیلد «حداکثر مدت شات» — هم‌الگو با `_durationSecondsText` در
    // ShotComposerViewModel: اگر مستقیماً از dna.outputConstraints.maxShotDurationSeconds
    // مشتق می‌شد، پاک‌کردن کامل فیلد برای تایپ دوباره غیرممکن می‌شد (رشته‌ی خالی
    // Int.parse نمی‌شود و بلافاصله به مقدار قبلی بازمی‌گشت).
    private val _maxShotDurationSecondsText = MutableStateFlow(_dna.value.outputConstraints.maxShotDurationSeconds.toString())
    val maxShotDurationSecondsText: StateFlow<String> = _maxShotDurationSecondsText.asStateFlow()

    /** پیام آخرین اقدام (هشدار Rule 1) — هم‌الگو با SceneDetailViewModel.lastActionMessage. */
    private val _lastActionMessage = MutableStateFlow<String?>(null)
    val lastActionMessage: StateFlow<String?> = _lastActionMessage.asStateFlow()

    init {
        ioScope.launch {
            repository.loadProjectDna(projectId).getOrNull()?.let { loaded ->
                _dna.value = loaded
                _maxShotDurationSecondsText.value = loaded.outputConstraints.maxShotDurationSeconds.toString()
            }
            _isLoaded.value = true
        }
    }

    fun clearLastActionMessage() { _lastActionMessage.value = null }

    // هویت اصلی — Soft Lock: طبق Rule 1 (DnaValidation.updateCoreIdentity)، تغییر
    // سبک بصری همیشه اعمال می‌شود؛ اگر شات وابسته‌ای وجود داشته باشد فقط هشدار
    // می‌دهد (هرگز Blocking). شمار واقعی شات‌های این پروژه دیگر همیشه ۰ نیست — طبق
    // تصمیم این قدم (رفع محدودیت ثبت‌شده در unit16-execution-plan.md)، از طریق
    // پارامتر `dependentShotsCount` از لایه‌ی UI (StudioShell، که از قبل
    // `ProjectListViewModel.projectSummaries` را برای Header خودش می‌خواند) گرفته
    // می‌شود، نه با یک اشتراک DB تازه داخل خودِ این ViewModel — جزئیات کامل تصمیم
    // (چرا این روش به یک Query/Repository جدید ترجیح داده شد) در
    // docs/adr/054-unit16-dependent-shots-count-output-constraints-ui.md.
    fun setDominantVisualStyle(style: VisualStyle, dependentShotsCount: Int = 0) {
        val result = updateCoreIdentity(_dna.value, style, dependentShotsCount)
        _dna.value = result.updatedDna
        ioScope.launch { repository.saveProjectDna(result.updatedDna) }
        result.warning?.let { _lastActionMessage.value = it }
    }
    fun setRealismLevel(level: RealismLevel) = updateAndSave {
        it.copy(coreIdentity = it.coreIdentity.copy(realismLevel = level))
    }
    fun setCoreStyleConsistency(consistency: StyleConsistency) = updateAndSave {
        it.copy(coreIdentity = it.coreIdentity.copy(styleConsistency = consistency))
    }

    // پالت اصلی
    fun setColorTemperature(temperature: ColorTemperature) = updateAndSave {
        it.copy(masterPalette = it.masterPalette.copy(colorTemperature = temperature))
    }
    fun setGlobalSaturation(level: SaturationLevel) = updateAndSave {
        it.copy(masterPalette = it.masterPalette.copy(globalSaturation = level))
    }
    fun setGlobalContrast(level: ContrastLevel) = updateAndSave {
        it.copy(masterPalette = it.masterPalette.copy(globalContrast = level))
    }
    fun setColorGradingPreset(text: String) = updateAndSave {
        it.copy(masterPalette = it.masterPalette.copy(colorGradingPreset = text))
    }

    /**
     * ویرایش یک Swatch با شاخص `index` — طبق تصمیم مستند، `colorPalette` همیشه یک
     * لیست فشرده (بدون رشته‌ی خالی میان‌مقداری) نگه داشته می‌شود، دقیقاً هم‌راستا با
     * توضیح خودِ `MasterPalette.colorPalette` («دقیقاً تا ۵ مقدار Hex»)؛ پاک‌کردن یک
     * Swatch میانی باعث می‌شود رنگ‌های بعدی یک شاخص به‌جلو بیایند (رفتاری ساده و
     * قابل‌پیش‌بینی، مستند در ADR-047).
     */
    fun setColorSwatch(index: Int, hex: String) = updateAndSave { current ->
        val padded = current.masterPalette.colorPalette.toMutableList()
        while (padded.size <= index) padded.add("")
        padded[index] = hex
        val compact = padded.filter { it.isNotBlank() }.take(5)
        current.copy(masterPalette = current.masterPalette.copy(colorPalette = compact))
    }

    // پایه‌ی احساسی کلی
    fun setPrimaryEmotion(mood: Mood) = updateAndSave {
        it.copy(globalMoodBase = it.globalMoodBase.copy(primaryEmotion = mood))
    }
    fun setMoodIntensity(intensity: String) = updateAndSave {
        it.copy(globalMoodBase = it.globalMoodBase.copy(intensity = intensity))
    }
    fun setMoodConsistency(consistency: StyleConsistency) = updateAndSave {
        it.copy(globalMoodBase = it.globalMoodBase.copy(consistency = consistency))
    }

    // ترجیح نور — nullable طبق طراحی («بدون ترجیح» یک گزینه‌ی صریح است).
    fun setPreferredLightingStyle(style: LightingStyle?) = updateAndSave {
        it.copy(lightingPreference = it.lightingPreference.copy(preferredStyle = style))
    }

    // تکمیل Rule یتیم — قدم ۴ از ۴ (ADR-112): زبان سینمایی سراسری پروژه —
    // غیر-nullable (سه گزینه‌ی ثابت)، هم‌الگو با setRealismLevel/setCoreStyleConsistency
    // بالا؛ resolveEffectiveCinematicMode (ADR-106/108/109) این مقدار را به‌عنوان
    // آخرین لایه‌ی Fallback زنجیره‌ی سه‌سطحی مصرف می‌کند.
    fun setGlobalCinematicMode(mode: CinematicMode) = updateAndSave {
        it.copy(cinematicLanguage = it.cinematicLanguage.copy(globalMode = mode))
    }

    // محدودیت‌های خروجی — این قدم سه فیلد باقی‌مانده‌ی OutputConstraints
    // (forbiddenElements/mandatoryElements/maxShotDurationSeconds، محدودیت
    // ثبت‌شده در ADR-047) را به UI اضافه می‌کند؛ جزئیات کامل در ADR-054.
    fun setAspectRatio(ratio: AspectRatio) = updateAndSave {
        it.copy(outputConstraints = it.outputConstraints.copy(aspectRatio = ratio))
    }

    fun setMaxShotDurationSecondsText(text: String) {
        _maxShotDurationSecondsText.value = text
        text.toIntOrNull()?.takeIf { it > 0 }?.let { seconds ->
            updateAndSave { it.copy(outputConstraints = it.outputConstraints.copy(maxShotDurationSeconds = seconds)) }
        }
    }

    fun addMandatoryElement(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        updateAndSave {
            it.copy(outputConstraints = it.outputConstraints.copy(mandatoryElements = it.outputConstraints.mandatoryElements + trimmed))
        }
    }

    fun removeMandatoryElement(index: Int) = updateAndSave {
        it.copy(outputConstraints = it.outputConstraints.copy(mandatoryElements = it.outputConstraints.mandatoryElements.filterIndexed { i, _ -> i != index }))
    }

    fun addForbiddenElement(category: String, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        updateAndSave {
            val current = it.outputConstraints.forbiddenElements[category] ?: emptyList()
            if (trimmed in current) return@updateAndSave it
            it.copy(
                outputConstraints = it.outputConstraints.copy(
                    forbiddenElements = it.outputConstraints.forbiddenElements + (category to (current + trimmed))
                )
            )
        }
    }

    fun removeForbiddenElement(category: String, value: String) = updateAndSave {
        val updatedList = (it.outputConstraints.forbiddenElements[category] ?: emptyList()) - value
        val updatedMap = if (updatedList.isEmpty()) {
            it.outputConstraints.forbiddenElements - category
        } else {
            it.outputConstraints.forbiddenElements + (category to updatedList)
        }
        it.copy(outputConstraints = it.outputConstraints.copy(forbiddenElements = updatedMap))
    }

    // دستورالعمل‌های کیفیت
    fun setQualityTags(text: String) = updateAndSave {
        it.copy(qualityDirectives = it.qualityDirectives.copy(qualityTags = text))
    }
    fun setNegativePrompt(text: String) = updateAndSave {
        it.copy(qualityDirectives = it.qualityDirectives.copy(negativePrompt = text))
    }

    /** Auto-Save بی‌صدا — همان انضباط StoryViewModel/AiStoryBreakdownViewModel. */
    private fun updateAndSave(transform: (ProjectDna) -> ProjectDna) {
        val updated = transform(_dna.value)
        _dna.value = updated
        ioScope.launch { repository.saveProjectDna(updated) }
    }

    companion object {
        /** تزریق‌پذیری Repository از همان ابتدا — طبق یافته‌ی واقعی تست ADR-045، اعمال‌شده روی این ViewModel از ابتدا (بدون نیاز به کشف مجدد همان باگ). */
        fun factory(application: Application, projectId: String, repository: ProjectDnaRepository? = null): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (repository != null) DnaViewModel(application, projectId, repository)
                        else DnaViewModel(application, projectId)
                    ) as T
            }
    }
}

private fun randomDnaId(): String = "dna_" + UUID.randomUUID().toString().replace("-", "").take(12)

/**
 * پیش‌فرض خنثی برای یک پروژه‌ی تازه (هنوز هیچ ProjectDna ای ذخیره نشده) — تا فرم
 * قابل نمایش/ویرایش باشد، هم‌الگو با defaultStoryContext (ui/story/StoryViewModel.kt،
 * ADR-045). انتخاب‌ها همه خنثی/میانه‌اند: CINEMATIC_STYLE (رایج‌ترین سبک عمومی)،
 * SEMI_REALISTIC، StyleConsistency.MODERATE، ColorTemperature.NEUTRAL،
 * SaturationLevel.MEDIUM، ContrastLevel.MEDIUM، Mood.CALM (هم‌راستا با پیش‌فرض
 * Mood موجود StoryViewModel)، AspectRatio.LANDSCAPE_16_9 (رایج‌ترین نسبت). مقدار
 * maxShotDurationSeconds=8 (پیش‌فرض بلوپرینت) و forbiddenElements/mandatoryElements
 * خالی — هر سه فیلد اکنون (ADR-054) در UI قابل ویرایش‌اند.
 */
internal fun defaultProjectDna(projectId: String, idProvider: () -> String): ProjectDna = ProjectDna(
    dnaId = idProvider(),
    projectId = projectId,
    coreIdentity = CoreIdentity(
        dominantVisualStyle = VisualStyle.CINEMATIC_STYLE,
        realismLevel = RealismLevel.SEMI_REALISTIC,
        styleConsistency = StyleConsistency.MODERATE
    ),
    masterPalette = MasterPalette(
        colorTemperature = ColorTemperature.NEUTRAL,
        globalSaturation = SaturationLevel.MEDIUM,
        globalContrast = ContrastLevel.MEDIUM,
        colorGradingPreset = ""
    ),
    outputConstraints = OutputConstraints(
        forbiddenElements = emptyMap(),
        mandatoryElements = emptyList(),
        maxShotDurationSeconds = 8,
        aspectRatio = AspectRatio.LANDSCAPE_16_9
    ),
    globalMoodBase = GlobalMoodBase(
        primaryEmotion = Mood.CALM
    ),
    lightingPreference = LightingPreference(),
    qualityDirectives = QualityDirectives()
)
