package com.operaboys.cinemashotgenerator.ui.outputdelivery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.AudioContextRepository
import com.operaboys.cinemashotgenerator.data.repository.PromptGenerationRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.SettingsResolutionRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ALL_MODEL_PROFILES
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelProfile
import com.operaboys.cinemashotgenerator.domain.outputdelivery.RenderedOutput
import com.operaboys.cinemashotgenerator.domain.outputdelivery.render
import com.operaboys.cinemashotgenerator.domain.outputdelivery.renderBlueprintToText
import com.operaboys.cinemashotgenerator.domain.outputdelivery.validatePromptLength
import com.operaboys.cinemashotgenerator.domain.outputdelivery.validateUnsupportedFeatureUsage
import com.operaboys.cinemashotgenerator.domain.promptengine.assemblePromptBlueprint
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.domain.validation.aggregateShotValidation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// واحد ۱۶ فاز ۵ — قدم ۳ (آخرین قدم، طبق ADR-056 شامل هر دو وظیفه‌ی «تولید Prompt» و
// «تحویل خروجی»): ViewModel صفحه‌ی Output Delivery. زنجیره‌ی پشت‌صحنه دقیقاً طبق
// پیش‌بررسی معمار: collectData(shotId) (واحد ۱۵→۱۱) → assemblePromptBlueprint(...)
// (واحد ۱۱) → render/optimizeForProfile (واحد ۱۴). جزئیات کامل تصمیمات در
// docs/adr/057-unit16-phase5-step3-output-delivery.md.

sealed class OutputDeliveryState {
    data object Loading : OutputDeliveryState()
    data class Error(val message: String) : OutputDeliveryState()
    data class Ready(
        val renderedOutput: RenderedOutput,
        val warnings: List<ValidationIssue>
    ) : OutputDeliveryState()
}

class OutputDeliveryViewModel(
    application: Application,
    private val shotId: String,
    initialModelProfileId: String?,
    private val promptGenerationRepository: PromptGenerationRepository = PromptGenerationRepository(
        AppDatabase.getInstance(application).shotDao(),
        AppDatabase.getInstance(application).sceneDao(),
        ProjectDnaRepository(AppDatabase.getInstance(application).projectDnaDao()),
        AssetRepository(AppDatabase.getInstance(application).assetDao()),
        SettingsResolutionRepository(AppDatabase.getInstance(application).shotDao(), AppDatabase.getInstance(application).sceneDao()),
        AudioContextRepository(AppDatabase.getInstance(application).audioContextDao())
    ),
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    private val _selectedProfileId = MutableStateFlow(
        ALL_MODEL_PROFILES.firstOrNull { it.profileId == initialModelProfileId }?.profileId
            ?: ALL_MODEL_PROFILES.first().profileId
    )
    val selectedProfileId: StateFlow<String> = _selectedProfileId.asStateFlow()

    private val _state = MutableStateFlow<OutputDeliveryState>(OutputDeliveryState.Loading)
    val state: StateFlow<OutputDeliveryState> = _state.asStateFlow()

    // یافته‌ی واقعی دیباگ این قدم: بدون لغو صریح، انتخاب سریع چند مدل پشت‌سرهم
    // یک Race واقعی می‌سازد — اگر زنجیره‌ی مدل قبلی (کندتر) بعد از زنجیره‌ی مدل
    // تازه به پایان برسد، نتیجه‌ی قدیمی/نامرتبط جای نتیجه‌ی تازه را می‌گیرد (آخرین
    // نوشتن روی _state.value برنده می‌شود، نه لزوماً آخرین انتخاب کاربر). هر
    // فراخوان regenerate تازه، هر Job قبلی هنوز درحال‌اجرا را صریحاً لغو می‌کند.
    private var regenerateJob: Job? = null

    init {
        regenerate()
    }

    fun selectProfile(profileId: String) {
        _selectedProfileId.value = profileId
        regenerate()
    }

    /**
     * زنجیره‌ی کامل را از صفر دوباره اجرا می‌کند — طبق تصمیم ADR-057، «Regenerate»
     * در این پروژه به‌معنای «محاسبه‌ی دوباره‌ی تازه» است، نه تولید یک نتیجه‌ی
     * تصادفی متفاوت: manageSeed یک مقدار Seed کاملاً Deterministic (وابسته به
      * shotId) برمی‌گرداند و هیچ‌جای Renderer.kt از آن برای ایجاد تنوع متن استفاده
     * نمی‌کند — پس با داده‌ی یکسان، نتیجه‌ی متن همیشه یکسان است (یک ابزار Refresh
     * مفید اگر کاربر Shot را در صفحه‌ای دیگر ویرایش کرده و به این‌جا برگشته، نه
     * یک تولیدکننده‌ی تصادفی).
     */
    fun regenerate() {
        regenerateJob?.cancel()
        regenerateJob = ioScope.launch {
            _state.value = OutputDeliveryState.Loading
            val input = promptGenerationRepository.collectData(shotId).getOrElse {
                _state.value = OutputDeliveryState.Error(it.message ?: "خطای نامشخص در بارگذاری داده‌های شات")
                return@launch
            }
            val profile = ALL_MODEL_PROFILES.firstOrNull { it.profileId == _selectedProfileId.value } ?: ALL_MODEL_PROFILES.first()

            // warnings واقعی این شات (هر سه سطح، ADR-055) دقیقاً از همان
            // تجمیع‌کننده‌ای گرفته می‌شود که صفحه‌ی Validation مصرف می‌کند — نه یک
            // محاسبه‌ی مستقل و تازه (طبق تصمیم مستند ADR-057: «هماهنگ با Validation،
            // نه یک منبع کاملاً جدا»).
            val shotLevelWarnings = aggregateShotValidation(
                shot = input.shot,
                scene = input.scene,
                dna = input.dna,
                characterAssets = input.characters,
                objectAssets = input.objects,
                locationAssets = input.locations
            ).issues.map { it.issue }.filter { it.severity == Severity.WARNING }

            val blueprint = assemblePromptBlueprint(
                input = input,
                useSeed = true,
                weightedTags = null,
                validationIssues = shotLevelWarnings
            )

            // validatePromptLength باید روی متنِ پیش از کوتاه‌سازی فراخوانی شود (طبق
            // کامنت خودِ Renderer.kt) — پس renderBlueprintToText جدا از render()
            // فراخوانی می‌شود تا نسخه‌ی کوتاه‌نشده هم در دسترس باشد.
            val preTruncationText = renderBlueprintToText(blueprint, profile)
            val renderedOutput = render(blueprint, profile)

            val renderWarnings = listOfNotNull(
                validatePromptLength(preTruncationText, profile),
                validateUnsupportedFeatureUsage(blueprint, profile)
            )

            _state.value = OutputDeliveryState.Ready(
                renderedOutput = renderedOutput,
                warnings = blueprint.warnings + renderWarnings
            )
        }
    }

    fun profileFor(profileId: String): ModelProfile = ALL_MODEL_PROFILES.first { it.profileId == profileId }

    companion object {
        fun factory(
            application: Application,
            shotId: String,
            initialModelProfileId: String?,
            promptGenerationRepository: PromptGenerationRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (promptGenerationRepository != null) {
                            OutputDeliveryViewModel(application, shotId, initialModelProfileId, promptGenerationRepository)
                        } else {
                            OutputDeliveryViewModel(application, shotId, initialModelProfileId)
                        }
                    ) as T
            }
    }
}
