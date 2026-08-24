package com.operaboys.cinemashotgenerator.ui.outputdelivery

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.AudioContextRepository
import com.operaboys.cinemashotgenerator.data.repository.DeviceExportFileWriter
import com.operaboys.cinemashotgenerator.data.repository.ExportFileWriter
import com.operaboys.cinemashotgenerator.data.repository.PromptGenerationRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.SecureKeyRepository
import com.operaboys.cinemashotgenerator.data.repository.SettingsResolutionRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ALL_MODEL_PROFILES
import com.operaboys.cinemashotgenerator.domain.outputdelivery.BilingualPrompts
import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelProfile
import com.operaboys.cinemashotgenerator.domain.outputdelivery.OutputPackage
import com.operaboys.cinemashotgenerator.domain.outputdelivery.RenderedOutput
import com.operaboys.cinemashotgenerator.domain.outputdelivery.composeOutput
import com.operaboys.cinemashotgenerator.domain.outputdelivery.render
import com.operaboys.cinemashotgenerator.domain.outputdelivery.renderBlueprintToText
import com.operaboys.cinemashotgenerator.domain.outputdelivery.validatePromptLength
import com.operaboys.cinemashotgenerator.domain.outputdelivery.validateProfileAvailability
import com.operaboys.cinemashotgenerator.domain.outputdelivery.validateUnsupportedFeatureUsage
import com.operaboys.cinemashotgenerator.domain.promptengine.assemblePromptBlueprint
import com.operaboys.cinemashotgenerator.domain.promptfinalization.TokenCheckResult
import com.operaboys.cinemashotgenerator.domain.promptfinalization.finalizePrompt
import com.operaboys.cinemashotgenerator.domain.promptfinalization.validateCompressionRatio
import com.operaboys.cinemashotgenerator.domain.promptfinalization.validateConflictsResolved
import com.operaboys.cinemashotgenerator.domain.storybreakdown.BUILTIN_AI_CONNECTOR_PROFILES
import com.operaboys.cinemashotgenerator.domain.storybreakdown.sendToAiConnector
import com.operaboys.cinemashotgenerator.domain.storybreakdown.validateAiConnectorErrorMessage
import com.operaboys.cinemashotgenerator.domain.storybreakdown.validateApiKeyProvided
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.domain.validation.aggregateShotValidation
import com.operaboys.cinemashotgenerator.domain.workflow.QualityScore
import com.operaboys.cinemashotgenerator.domain.workflow.evaluatePromptQuality
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.io.File
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
        // MIGRATED (رفع G17 ممیزی post-Unit16، docs/adr/060): این فیلد تا این قدم
        // متن خام render() بود؛ اکنون متن واقعاً پاک‌سازی‌شده (finalizePrompt، واحد
        // ۱۳) است — دقیقاً همان متنی که Copy/Export/نمایش می‌شود.
        val renderedOutput: RenderedOutput,
        val warnings: List<ValidationIssue>,
        val tokenCheck: TokenCheckResult?,
        // فقط وقتی finalizePrompt واقعاً بدون خطا اجرا شده باشد true است — بج
        // «پاک‌سازی‌شده· نهایی‌شده» صفحه فقط وقتی این true است نمایش داده می‌شود
        // (نه یک Badge ثابت که همیشه نمایش داده می‌شد، طبق دستور کار این قدم).
        val cleaningSucceeded: Boolean,
        // رفع یافته‌ی معماری «دکمه‌ی Export مستعار Copy است» (G4/G18، ADR-069):
        // composeOutput (واحد ۱۴) اکنون بلافاصله بعد از رندر/پاک‌سازی موفق صدا
        // زده می‌شود، نه در لحظه‌ی کلیک Export — دکمه‌ی Export فقط exportFiles
        // موجود این بسته را روی دیسک می‌نویسد.
        val outputPackage: OutputPackage,
        // هوشمندسازی و اتصال evaluatePromptQuality — قدم ۲ از ۳ زیرقدم (ADR-119):
        // فقط QualityScore نهایی (نه کل blueprint) نگه داشته می‌شود — نگه‌داشتن
        // کل blueprint در state برای این یک مصرف (نمایش یک کارت اطلاعاتی) بی‌دلیل
        // بزرگ‌تر از لازم بود.
        val qualityScore: QualityScore
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
    private val exportFileWriter: ExportFileWriter = DeviceExportFileWriter(application),
    // هوشمندسازی و اتصال evaluatePromptQuality — قدم ۳ از ۳ زیرقدم (آخرین
    // زیرقدم، ADR-120): دو تزریق تازه، عیناً هم‌الگو با G2
    // (AiStoryBreakdownViewModel.kt) — تا تست‌ها بتوانند SharedPreferences
    // معمولی/MockEngine جایگزین کنند (بدون AndroidKeyStore واقعی یا تماس
    // واقعی اینترنت در تست).
    private val secureKeyRepository: SecureKeyRepository = SecureKeyRepository(application),
    private val httpClientEngine: HttpClientEngine = OkHttp.create(),
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    // یک رویداد یک‌باره (هم‌الگو با lastActionMessage/clearLastActionMessage
    // ProjectListViewModel) — وقتی exportOutput() فایل‌ها را واقعاً روی دیسک
    // می‌نویسد، UI این را Observe می‌کند تا Intent.ACTION_SEND واقعی را بسازد/باز
    // کند (کاری که فقط لایه‌ی UI با Context یک Activity می‌تواند انجام دهد، نه
    // ViewModel) و سپس clearExportedFiles را صدا می‌زند.
    private val _exportedFiles = MutableStateFlow<List<File>?>(null)
    val exportedFiles: StateFlow<List<File>?> = _exportedFiles.asStateFlow()

    fun clearExportedFiles() {
        _exportedFiles.value = null
    }

    /** فایل‌های exportFiles بسته‌ی فعلی (اگر state آماده باشد) را واقعاً روی دیسک می‌نویسد. */
    fun exportOutput(): Job = ioScope.launch {
        val currentState = _state.value as? OutputDeliveryState.Ready ?: return@launch
        _exportedFiles.value = exportFileWriter.writeExportFiles(currentState.outputPackage.exportFiles)
    }

    private val _selectedProfileId = MutableStateFlow(resolveInitialProfileId(initialModelProfileId))
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

    // هوشمندسازی و اتصال evaluatePromptQuality — قدم ۳ از ۳ زیرقدم (آخرین
    // زیرقدم، ADR-120): «تحلیل عمیق‌تر با AI»، مسیر ۲/اختیاری این برنامه —
    // عیناً همان معماری اثبات‌شده‌ی G2 (AiStoryBreakdownViewModel.kt) برای
    // یک صفحه‌ی دیگر تکرار شده، نه یک معماری موازی تازه. selectedAiConnectorProfileId
    // (AiConnectorProfile: Claude/OpenAI/...) کاملاً مستقل از selectedProfileId
    // بالا (ModelProfile: Veo/Kling/...) است — این دو مفهوم کاملاً متفاوتند و
    // هرگز نباید با هم اشتباه گرفته شوند.

    private val _selectedAiConnectorProfileId = MutableStateFlow(BUILTIN_AI_CONNECTOR_PROFILES.first().profileId)
    val selectedAiConnectorProfileId: StateFlow<String> = _selectedAiConnectorProfileId.asStateFlow()

    // شرط سخت‌گیرانه‌ی UI (همان ADR-101 G2): دکمه‌ی «تحلیل عمیق‌تر با AI» فقط
    // وقتی کلید پروفایل انتخاب‌شده واقعاً ذخیره شده باشد enabled است — یک
    // بررسی پیش‌فعال (پیش از کلیک)، نه فقط واکنش به خطای بعد از کلیک.
    private val _apiKeySavedForQualityAnalysis = MutableStateFlow(false)
    val apiKeySavedForQualityAnalysis: StateFlow<Boolean> = _apiKeySavedForQualityAnalysis.asStateFlow()

    private val _qualityAnalysisInProgress = MutableStateFlow(false)
    val qualityAnalysisInProgress: StateFlow<Boolean> = _qualityAnalysisInProgress.asStateFlow()

    private val _qualityAnalysisResult = MutableStateFlow<String?>(null)
    val qualityAnalysisResult: StateFlow<String?> = _qualityAnalysisResult.asStateFlow()

    private val _qualityAnalysisError = MutableStateFlow<String?>(null)
    val qualityAnalysisError: StateFlow<String?> = _qualityAnalysisError.asStateFlow()

    init {
        ioScope.launch { refreshApiKeySavedForQualityAnalysis(_selectedAiConnectorProfileId.value) }
    }

    /**
     * یافته‌ی مستندشده‌ی G2 (هنوز صادق، AiStoryBreakdownViewModel.kt): SecureKeyRepository
     * پیش‌فرض‌تزریق‌نشده روی Robolectric واقعاً KeyStoreException پرتاب می‌کند؛
     * چون تست‌های این ViewModel با ioScopeOverride=Dispatchers.Unconfined (Job
     * معمولی، نه SupervisorJob) اجرا می‌شوند، یک فرزند شکست‌خورده کل Job والد
     * (شامل زنجیره‌ی regenerate) را لغو می‌کند. runCatching این ریسک را می‌بندد؛
     * fail-closed به false هم با شرط سخت‌گیرانه‌ی محصولی هم‌راستاست.
     */
    private suspend fun refreshApiKeySavedForQualityAnalysis(profileId: String) {
        _apiKeySavedForQualityAnalysis.value = runCatching { secureKeyRepository.hasApiKey(profileId) }.getOrDefault(false)
    }

    /** هم‌الگو دقیق با AiStoryBreakdownViewModel.selectProfile — Job چون hasApiKey روی Dispatchers.IO واقعی اجرا می‌شود. */
    fun selectAiConnectorProfileForQuality(profileId: String): Job {
        _selectedAiConnectorProfileId.value = profileId
        return ioScope.launch { refreshApiKeySavedForQualityAnalysis(profileId) }
    }

    fun clearQualityAnalysisError() {
        _qualityAnalysisError.value = null
    }

    fun clearQualityAnalysisResult() {
        _qualityAnalysisResult.value = null
    }

    /**
     * «تحلیل عمیق‌تر با AI» — مسیر ۲/اختیاری این برنامه. برخلاف G2 (که پاسخ
     * JSON ساختاریافته را با processAiResponse/ChunkCombiner/JsonDoctor پارس
     * می‌کند)، پاسخ این فیچر یک متن آزاد توضیحی است — نتیجه‌ی خام
     * sendToAiConnector مستقیماً همان متن قابل‌نمایش است، بدون نیاز به هیچ
     * Parser تازه. Rule ۴ (validateApiKeyProvided) قبل از هر تلاش واقعی HTTP
     * اعمال می‌شود — عیناً هم‌الگو با sendPromptAutomatically G2.
     */
    fun analyzePromptQualityWithAi(): Job {
        val currentState = _state.value as? OutputDeliveryState.Ready ?: return Job().apply { complete() }
        if (_qualityAnalysisInProgress.value) return Job().apply { complete() }
        val profile = BUILTIN_AI_CONNECTOR_PROFILES.firstOrNull { it.profileId == _selectedAiConnectorProfileId.value }
            ?: return Job().apply { complete() }
        _qualityAnalysisError.value = null
        return ioScope.launch {
            val apiKey = secureKeyRepository.loadApiKey(profile.profileId)
            if (apiKey == null || validateApiKeyProvided(apiKey) != null) {
                _qualityAnalysisError.value = "ابتدا کلید API را در تنظیمات وارد کنید"
                return@launch
            }
            _qualityAnalysisInProgress.value = true
            val analysisPrompt = buildQualityAnalysisPrompt(currentState.renderedOutput.formattedPrompt, currentState.qualityScore)
            val result = sendToAiConnector(profile, apiKey, analysisPrompt, httpClientEngine)
            _qualityAnalysisInProgress.value = false
            result.fold(
                onSuccess = { responseText -> _qualityAnalysisResult.value = responseText },
                onFailure = {
                    _qualityAnalysisError.value = validateAiConnectorErrorMessage(result)?.message
                        ?: "درخواست به AI Connector با خطا مواجه شد"
                }
            )
        }
    }

    /**
     * زنجیره‌ی کامل را از صفر دوباره اجرا می‌کند — طبق تصمیم ADR-057، «Regenerate»
     * در این پروژه به‌معنای «محاسبه‌ی دوباره‌ی تازه» است، نه تولید یک نتیجه‌ی
     * تصادفی متفاوت: manageSeed یک مقدار Seed کاملاً Deterministic (وابسته به
      * shotId) برمی‌گرداند و هیچ‌جای Renderer.kt از آن برای ایجاد تنوع متن استفاده
     * نمی‌کند — پس با داده‌ی یکسان، نتیجه‌ی متن همیشه یکسان است (یک ابزار Refresh
     * مفید اگر کاربر Shot را در صفحه‌ای دیگر ویرایش کرده و به این‌جا برگشته، نه
     * یک تولیدکننده‌ی تصادفی).
     *
     * Job برگردانده‌شده (نه Unit) — هم‌الگو با WorkflowViewModel.setLanguage/... —
     * فقط برای اینکه تست‌های مستقیم ViewModel (بدون Compose، OutputDeliveryViewModelTest.kt)
     * بتوانند `.join()` کنند و مطمئن شوند زنجیره‌ی regenerate واقعاً قبل از خواندن
     * state.value کامل شده؛ فراخوان‌های موجود (init، selectProfile، دکمه‌ی UI) مقدار
     * برگشتی را نادیده می‌گیرند — بدون تغییر رفتار.
     */
    fun regenerate(): Job {
        regenerateJob?.cancel()
        val job = ioScope.launch {
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
                locationAssets = input.locations,
                // اتصال Rule یتیم گزارش‌شده در ADR-143 (checkTotalSoundLayerCount):
                // input.audioContext از قبل توسط promptGenerationRepository.collectData
                // بارگذاری شده — بدون نیاز به Repository جدید یا فراخوان I/O دوم.
                audioContext = input.audioContext
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

            // MIGRATED (رفع G17 ممیزی post-Unit16، docs/adr/060): تا این قدم واحد ۱۳
            // (Prompt Finalization Pipeline) هرگز از اینجا صدا زده نمی‌شد — متن خام
            // render() مستقیم به کاربر نمایش/Copy/Export می‌شد، در حالی که بج
            // «پاک‌سازی‌شده· نهایی‌شده» بدون قید و شرط نمایش داده می‌شد. finalizePrompt
            // روی renderedOutput.formattedPrompt (نه preTruncationText) اجرا می‌شود —
            // طبق ترتیب مستندشده‌ی Renderer.kt (ADR-026: Cleaning بعد از Render، چون
            // برای پروفایل‌های JSON باید روی خروجی نهایی JSON-آگاه عمل کند، نه متن
            // مسطح‌نشده‌ی پیش از رندر). runCatching صرفاً یک محافظ دفاعی است (نه
            // انتظار خطای واقعی) — اگر finalizePrompt به هر دلیل نامنتظره شکست بخورد،
            // به متن خام Fallback می‌کنیم (بدون Crash کل صفحه) و بج نمایش داده
            // نمی‌شود، دقیقاً طبق دستور کار.
            val finalization = runCatching { finalizePrompt(renderedOutput, profile) }.getOrNull()

            val cleaningWarnings = if (finalization != null) {
                listOfNotNull(
                    validateConflictsResolved(finalization.rendered.formattedPrompt),
                    validateCompressionRatio(finalization.cleaningReport),
                    finalization.tokenCheck.warning?.let { ValidationIssue(severity = Severity.WARNING, message = it) }
                )
            } else {
                emptyList()
            }

            val finalRenderedOutput = finalization?.rendered ?: renderedOutput

            // هوشمندسازی و اتصال evaluatePromptQuality — قدم ۲ از ۳ زیرقدم (ADR-119):
            // هر دو ورودی لازم (finalRenderedOutput، blueprint) همین‌جا در دسترس‌اند —
            // محاسبه بلافاصله بعد از ساخته‌شدن finalRenderedOutput انجام می‌شود، نه
            // یک محل جدا/دیرتر.
            val qualityScore = evaluatePromptQuality(finalRenderedOutput, blueprint)

            // رفع یافته‌ی معماری «دکمه‌ی Export مستعار Copy است» (G4/G18، ADR-067،
            // رفع در ADR-069). محدودیت شناخته‌شده و آگاهانه (نه بدهی فنی این قدم،
            // طبق Bilingual.kt که صریحاً generateBilingualPrompt/translateToFarsi
            // را عمداً پیاده نکرده — یک تصمیم معماری جدا و در حال بحث است، نه یک
            // TODO ساده): تا زمانی که یک موتور ترجمه‌ی واقعی انتخاب نشده،
            // enVersion/faVersion هر دو برابر همان متن رندرشده‌ی نهایی هستند
            // (RenderedOutput.language طبق کامنت PromptCleaner.kt همیشه "en" است؛
            // نسخه‌ی فارسی واقعی هنوز وجود ندارد). جزئیات کامل در
            // docs/adr/069-real-output-export-and-bilingual-limitation.md.
            val outputPackage = composeOutput(
                shotId = shotId,
                promptBlueprintId = blueprint.promptBlueprintId,
                renderedOutputs = listOf(finalRenderedOutput),
                bilingualPrompts = BilingualPrompts(
                    enVersion = finalRenderedOutput.formattedPrompt,
                    faVersion = finalRenderedOutput.formattedPrompt
                )
            )

            _state.value = OutputDeliveryState.Ready(
                renderedOutput = finalRenderedOutput,
                warnings = blueprint.warnings + renderWarnings + cleaningWarnings,
                tokenCheck = finalization?.tokenCheck,
                cleaningSucceeded = finalization != null,
                outputPackage = outputPackage,
                qualityScore = qualityScore
            )
        }
        regenerateJob = job
        return job
    }

    fun profileFor(profileId: String): ModelProfile = ALL_MODEL_PROFILES.first { it.profileId == profileId }

    companion object {
        // رفع G16 (docs/adr/063): تزریق جزئی — هرکدام مستقل با ?: به پیش‌فرض خودش
        // می‌رسد، نه رفتار همه‌یا‌هیچ. هم‌الگو دقیق با AiStoryBreakdownViewModel.factory.
        fun factory(
            application: Application,
            shotId: String,
            initialModelProfileId: String?,
            promptGenerationRepository: PromptGenerationRepository? = null,
            secureKeyRepository: SecureKeyRepository? = null,
            httpClientEngine: HttpClientEngine? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val anyInjected = promptGenerationRepository != null || secureKeyRepository != null || httpClientEngine != null
                    return (
                        if (anyInjected) {
                            OutputDeliveryViewModel(
                                application = application,
                                shotId = shotId,
                                initialModelProfileId = initialModelProfileId,
                                promptGenerationRepository = promptGenerationRepository ?: PromptGenerationRepository(
                                    AppDatabase.getInstance(application).shotDao(),
                                    AppDatabase.getInstance(application).sceneDao(),
                                    ProjectDnaRepository(AppDatabase.getInstance(application).projectDnaDao()),
                                    AssetRepository(AppDatabase.getInstance(application).assetDao()),
                                    SettingsResolutionRepository(AppDatabase.getInstance(application).shotDao(), AppDatabase.getInstance(application).sceneDao()),
                                    AudioContextRepository(AppDatabase.getInstance(application).audioContextDao())
                                ),
                                secureKeyRepository = secureKeyRepository ?: SecureKeyRepository(application),
                                httpClientEngine = httpClientEngine ?: OkHttp.create()
                            )
                        } else {
                            OutputDeliveryViewModel(application, shotId, initialModelProfileId)
                        }
                    ) as T
                }
            }
    }
}

/**
 * اتصال واقعی Rule یتیم validateProfileAvailability (domain/outputdelivery/
 * ModelProfileLibrary.kt، ADR-126) — تنها نقطه‌ی واقعی کل کدبیس که یک
 * profileId خارجی (initialModelProfileId، از Navigation/State قدیمی) به یک
 * ModelProfile واقعی موجود در ALL_MODEL_PROFILES تبدیل می‌شود.
 *
 * نکته‌ی معماری مهم: این Rule پارامتر اولش targetPlatform است (مثل "veo")،
 * نه profileId (مثل "veo_3_1") — دو مفهوم متفاوت (تأییدشده با بررسی مستقل
 * ModelProfiles.kt). اینجا فقط وقتی initialModelProfileId غیر-null است ولی
 * هیچ پروفایلی با آن profileId پیدا نشد، Rule صدا زده می‌شود — با همان
 * رشته‌ی نامعتبر به‌عنوان "targetPlatform". چون شرط اول تابع
 * (hasExactMatch با platform) در این حالت همیشه false است (این رشته اصلاً
 * یک platform واقعی نیست)، عملاً فقط شرط دوم (hasUniversalFallback) معنا
 * دارد — یعنی این استفاده در واقع فقط یک چیز را واقعاً می‌سنجد: آیا
 * universal_default همچنان در ALL_MODEL_PROFILES هست تا Fallback فعلی امن
 * باشد. چون ALL_MODEL_PROFILES یک لیست ثابت Kotlin است که همیشه
 * universalDefaultProfile را دارد (خط ۳۱۰، ModelProfiles.kt)، این Rule در
 * عمل فعلاً همیشه null برمی‌گرداند — یک محافظ برای سناریوی فاجعه‌بار
 * («حتی universal_default هم نیست»)، نه تشخیص هر profileId نامعتبر
 * به‌تنهایی (که خودِ resolveInitialProfileId، بدون کمک این Rule، هرحال
 * تشخیص می‌دهد و Log می‌کند).
 *
 * رفتار ظاهری عمداً بدون تغییر می‌ماند — Fallback به
 * ALL_MODEL_PROFILES.first() دقیقاً همان‌طور که قبلاً بود باقی می‌ماند؛
 * فقط این حالت اکنون با Log.w قابل‌ردیابی است (هم‌الگو دقیق با
 * resolveInitialLanguage در ui/workflow/WorkflowViewModel.kt، ADR-125).
 */
private fun resolveInitialProfileId(initialModelProfileId: String?): String {
    val matched = ALL_MODEL_PROFILES.firstOrNull { it.profileId == initialModelProfileId }
    if (matched != null) return matched.profileId
    if (initialModelProfileId != null) {
        validateProfileAvailability(initialModelProfileId, ALL_MODEL_PROFILES)?.let { issue ->
            Log.w("OutputDeliveryViewModel", "persisted model profile '$initialModelProfileId' is invalid, falling back: ${issue.message}")
        }
    }
    return ALL_MODEL_PROFILES.first().profileId
}

/**
 * هوشمندسازی و اتصال evaluatePromptQuality — قدم ۳ از ۳ زیرقدم (آخرین
 * زیرقدم، ADR-120): متن پرامپت ارزیابی — محتوای تولیدشده در Runtime (نه
 * رشته‌ی ثابت UI)، پس عمداً از UiStrings.kt نمی‌آید. زمینه دقیقاً همان دو
 * چیز موجود در همان لحظه در `regenerate()` است: متن نهایی رندرشده و
 * QualityScore پنج‌محوره‌ی محاسبه‌شده (ADR-118).
 */
private fun buildQualityAnalysisPrompt(formattedPrompt: String, qualityScore: QualityScore): String = """
You are an expert cinematic AI prompt engineer. Analyze the following image/video generation prompt and its automated quality breakdown (0-20 per axis, 100 total). Explain, in a few clear sentences, its real strengths and weaknesses, and give concrete suggestions to improve it — focus on what a fast, non-precise heuristic score cannot see (semantic coherence, cinematic intent, genuinely vague or generic wording).

Prompt:
$formattedPrompt

Automated quality breakdown (heuristic, not precise):
- Subject Clarity: ${qualityScore.subjectClarity}/20
- Cinematic Clarity (Camera): ${qualityScore.cinematicClarity}/20
- Visual Specificity (Lighting & Environment): ${qualityScore.visualSpecificity}/20
- Style Coherence: ${qualityScore.styleCoherence}/20
- Conciseness: ${qualityScore.conciseness}/20
- Total: ${qualityScore.total}/100
""".trimIndent()
