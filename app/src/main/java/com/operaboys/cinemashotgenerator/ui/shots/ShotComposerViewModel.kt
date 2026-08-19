package com.operaboys.cinemashotgenerator.ui.shots

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.camera.AdvancedMovementType
import com.operaboys.cinemashotgenerator.domain.camera.BasicMovementType
import com.operaboys.cinemashotgenerator.domain.camera.CameraAngle
import com.operaboys.cinemashotgenerator.domain.camera.CameraDistance
import com.operaboys.cinemashotgenerator.domain.camera.CameraMovement
import com.operaboys.cinemashotgenerator.domain.camera.CameraSettings
import com.operaboys.cinemashotgenerator.domain.camera.DepthOfField
import com.operaboys.cinemashotgenerator.domain.camera.Framing
import com.operaboys.cinemashotgenerator.domain.camera.FocusMode
import com.operaboys.cinemashotgenerator.domain.camera.LensType
import com.operaboys.cinemashotgenerator.domain.camera.Stabilization
import com.operaboys.cinemashotgenerator.domain.camera.checkExtremeWideWithShallowDepthOfField
import com.operaboys.cinemashotgenerator.domain.camera.checkLensDistanceMismatch
import com.operaboys.cinemashotgenerator.domain.camera.checkRackFocusSubjectCount
import com.operaboys.cinemashotgenerator.domain.camera.checkStaticMovementWithHandheldStabilization
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ColorTemperature
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ContrastRatio
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentalMotion
import com.operaboys.cinemashotgenerator.domain.sceneconditions.FillLight
import com.operaboys.cinemashotgenerator.domain.sceneconditions.GroundState
import com.operaboys.cinemashotgenerator.domain.sceneconditions.KeyLightPosition
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightSourceCount
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingMotivation
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ShadowQuality
import com.operaboys.cinemashotgenerator.domain.sceneconditions.TemperatureFeel
import com.operaboys.cinemashotgenerator.domain.sceneconditions.Visibility
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherIntensity
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WindStrength
import com.operaboys.cinemashotgenerator.domain.sceneconditions.mapEnvironmentToSound
import com.operaboys.cinemashotgenerator.domain.shot.ActionSound
import com.operaboys.cinemashotgenerator.domain.shot.AmbientSound
import com.operaboys.cinemashotgenerator.domain.shot.CharacterSound
import com.operaboys.cinemashotgenerator.domain.shot.ImageReference
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.shot.SourcedSettings
import com.operaboys.cinemashotgenerator.domain.shot.validateShotDescription
import com.operaboys.cinemashotgenerator.domain.validation.AggregatedValidationReport
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.domain.validation.aggregateShotValidation
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode
import com.operaboys.cinemashotgenerator.domain.visualidentity.resolveEffectiveCinematicMode
import com.operaboys.cinemashotgenerator.ui.dna.defaultProjectDna
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

// واحد ۱۶ فاز ۴ — قدم ۲ — بخش ب: ViewModel اسکلت Shot Composer. فیلدهای سطح‌بالا
// از قدم ۲. MIGRATED (فاز ۴ قدم ۳): محتوای واقعی Tab «دوربین» (CameraSettings کامل،
// واحد ۰۹ بخش الف) + imageReferences اضافه شد — جزئیات کامل در
// docs/adr/052-unit16-phase4-step3-camera-tab.md. هم‌الگو با SceneDetailViewModel
// برای بارگذاری یک Shot موجود (idProvider تزریق‌پذیر، `isReady` برای جلوگیری از
// Auto-Save زودهنگام قبل از تکمیل بارگذاری/محاسبه‌ی shotNumber).

internal fun generateShotId(): String = "shot_" + UUID.randomUUID().toString().replace("-", "").take(12)

/**
 * فقط UI — دو ردیف بلوپرینت ۰۹ («پایه:» / «پیشرفته (v1.1)»)؛ مستقیماً از روی نوع
 * زیرکلاس فعلی CameraMovement مشتق می‌شود (نه یک State مستقل موازی که ممکن است
 * Desync شود). `AdvancedMovementType` (موجود در دامنه از قبل، اما تا این قدم در
 * هیچ‌جای کد استفاده نشده بود) دقیقاً نقش انتخاب‌گر ۵ زیرکلاس پیشرفته را ایفا می‌کند.
 */
enum class CameraMovementTier { BASIC, ADVANCED }

internal fun CameraMovement.tier(): CameraMovementTier =
    if (this is CameraMovement.Basic) CameraMovementTier.BASIC else CameraMovementTier.ADVANCED

internal fun CameraMovement.advancedTypeOrNull(): AdvancedMovementType? = when (this) {
    is CameraMovement.Orbit -> AdvancedMovementType.ORBIT
    is CameraMovement.DronePath -> AdvancedMovementType.DRONE_PATH
    is CameraMovement.DollyZoom -> AdvancedMovementType.DOLLY_ZOOM
    is CameraMovement.HandheldShake -> AdvancedMovementType.HANDHELD_SHAKE
    is CameraMovement.Compound -> AdvancedMovementType.COMPOUND
    is CameraMovement.Basic -> null
}

private data class CameraValidationPartial(
    val lensType: LensType,
    val distance: CameraDistance,
    val movement: CameraMovement,
    val stabilization: Stabilization,
    val depthOfField: DepthOfField
)

class ShotComposerViewModel(
    application: Application,
    private val projectId: String,
    private val sceneId: String,
    private val existingShotId: String?,
    private val repository: ShotRepository = ShotRepository(AppDatabase.getInstance(application).shotDao()),
    private val sceneRepository: SceneRepository = SceneRepository(AppDatabase.getInstance(application).sceneDao()),
    private val projectDnaRepository: ProjectDnaRepository = ProjectDnaRepository(AppDatabase.getInstance(application).projectDnaDao()),
    private val assetRepository: AssetRepository = AssetRepository(AppDatabase.getInstance(application).assetDao()),
    private val idProvider: () -> String = ::generateShotId,
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    val shotId: String = existingShotId ?: idProvider()

    /** فیلدهای غیر-سطح‌بالا (Beats/تصاویر/دوربین/نور/صدا/...) از Shot موجود دست‌نخورده حفظ می‌شوند — این قدم فقط فیلدهای سطح‌بالا را می‌نویسد. */
    private var loadedShot: Shot? = null

    /**
     * یافته‌ی #۱۳ appendix ADR-081 (ADR-086) — برای شمارش زنده‌ی Blocking/Warning
     * پنل خلاصه‌ی پایین. Scene/DNA/Asset ها یک‌بار (پس از بارگذاری اولیه‌ی Shot)
     * Cache می‌شوند — نه هر بار دوباره خوانده — چون characterIds/objectIds/
     * locationIds در هیچ‌کدام از Tab های این صفحه ویرایش نمی‌شوند (تأییدشده با
     * کامنت موجود بالای `_subjectCount`)، پس این سه لیست هرگز بعد از بارگذاری
     * اولیه در طول عمر این ViewModel تغییر نمی‌کنند.
     */
    private var cachedScene: Scene? = null
    private var cachedDna: ProjectDna? = null
    private var cachedCharacterAssets: List<CharacterAsset> = emptyList()
    private var cachedObjectAssets: List<ObjectAsset> = emptyList()
    private var cachedLocationAssets: List<LocationAsset> = emptyList()

    private val _validationSummary = MutableStateFlow(AggregatedValidationReport(emptyList()))
    val validationSummary: StateFlow<AggregatedValidationReport> = _validationSummary.asStateFlow()

    private val _shotNumber = MutableStateFlow(0)
    val shotNumber: StateFlow<Int> = _shotNumber.asStateFlow()

    private val _shotTitle = MutableStateFlow("")
    val shotTitle: StateFlow<String> = _shotTitle.asStateFlow()

    private val _shotDescription = MutableStateFlow("")
    val shotDescription: StateFlow<String> = _shotDescription.asStateFlow()

    // سیستم Preview دوزبانه‌ی پرامپت — قدم ۳ از ۳ زیرقدم، پایانی (ADR-123):
    // هم‌الگو دقیق با shotDescription بالا — نام‌گذاری با پیشوند shot، طبق
    // قرارداد موجود Shot.shotDescriptionFaPreview.
    private val _shotDescriptionFaPreview = MutableStateFlow("")
    val shotDescriptionFaPreview: StateFlow<String> = _shotDescriptionFaPreview.asStateFlow()

    private val _shotGoal = MutableStateFlow(ShotGoal.ACTION)
    val shotGoal: StateFlow<ShotGoal> = _shotGoal.asStateFlow()

    private val _shotType = MutableStateFlow(ShotType.MEDIUM)
    val shotType: StateFlow<ShotType> = _shotType.asStateFlow()

    private val _durationSecondsText = MutableStateFlow("4")
    val durationSecondsText: StateFlow<String> = _durationSecondsText.asStateFlow()

    private val _motionLevel = MutableStateFlow(MotionLevel.SUBTLE)
    val motionLevel: StateFlow<MotionLevel> = _motionLevel.asStateFlow()

    // تکمیل Rule یتیم — قدم ۴ از ۴ (ADR-112): Override محلی سطح شات‌ Cinematic
    // Mode (ADR-106) — null یعنی «پیروی از صحنه/پروژه». هم‌الگو با
    // cinematicModeOverride موجود Scene (بدون هیچ فیلد source جدا — همان دلیل
    // مستندشده‌ی خودِ domain.shot.ShotModels.kt: nullable ساده، نه SourcedSettings).
    private val _cinematicModeOverride = MutableStateFlow<CinematicMode?>(null)
    val cinematicModeOverride: StateFlow<CinematicMode?> = _cinematicModeOverride.asStateFlow()

    /**
     * حالت مؤثر نهایی همین شات (خروجی واقعی resolveEffectiveCinematicMode) —
     * برای نمایش زنده در فرم، هم‌الگو دقیق با validationSummary موجود:
     * cachedScene/cachedDna از قبل برای همان محاسبه بارگذاری شده‌اند؛ این
     * StateFlow هم در همان [refreshValidationSummary] بازمحاسبه می‌شود، بدون
     * بار I/O یا محاسباتی اضافه (هر دو تابع خالص‌اند).
     */
    private val _effectiveCinematicMode = MutableStateFlow<CinematicMode?>(null)
    val effectiveCinematicMode: StateFlow<CinematicMode?> = _effectiveCinematicMode.asStateFlow()

    /** Rule 1 واقعی واحد ۰۵ (ShotValidation.kt) — نمایش زنده، بدون مسدودکردن Auto-Save (طبق تصمیم مستند، جزئیات در ADR-051). */
    val shotDescriptionValidation: StateFlow<ValidationIssue?> = _shotDescription
        .map { validateShotDescription(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // --- Tab «دوربین» (فاز ۴ قدم ۳) ---

    /** طبق SourcedSettings<CameraSettings> واحد ۰۵: "scene" یا "override". */
    private val _cameraSource = MutableStateFlow("scene")
    val cameraSource: StateFlow<String> = _cameraSource.asStateFlow()

    // پیش‌فرض‌ها دقیقاً از جدول «پارامترهای پایه»ی docs/blueprints/09-camera-and-motion.md.
    private val _cameraAngle = MutableStateFlow(CameraAngle.EYE_LEVEL)
    val cameraAngle: StateFlow<CameraAngle> = _cameraAngle.asStateFlow()

    private val _cameraDistance = MutableStateFlow(CameraDistance.MEDIUM)
    val cameraDistance: StateFlow<CameraDistance> = _cameraDistance.asStateFlow()

    private val _lensType = MutableStateFlow(LensType.STANDARD)
    val lensType: StateFlow<LensType> = _lensType.asStateFlow()

    private val _depthOfField = MutableStateFlow(DepthOfField.MEDIUM)
    val depthOfField: StateFlow<DepthOfField> = _depthOfField.asStateFlow()

    private val _focusMode = MutableStateFlow(FocusMode.SUBJECT_TRACKING)
    val focusMode: StateFlow<FocusMode> = _focusMode.asStateFlow()

    private val _stabilization = MutableStateFlow(Stabilization.GIMBAL)
    val stabilization: StateFlow<Stabilization> = _stabilization.asStateFlow()

    private val _framing = MutableStateFlow(Framing.RULE_OF_THIRDS)
    val framing: StateFlow<Framing> = _framing.asStateFlow()

    private val _cameraMovement = MutableStateFlow<CameraMovement>(CameraMovement.Basic(BasicMovementType.STATIC))
    val cameraMovement: StateFlow<CameraMovement> = _cameraMovement.asStateFlow()

    private val _imageReferences = MutableStateFlow<List<ImageReference>>(emptyList())
    val imageReferences: StateFlow<List<ImageReference>> = _imageReferences.asStateFlow()

    /** ثابت پس از بارگذاری — characterIds/objectIds در این Tab ویرایش نمی‌شوند (کار Tab «صدا»، قدم بعدی). فقط برای Rule «Rack Focus». */
    private val _subjectCount = MutableStateFlow(0)

    /**
     * هر ۴ Rule خودبسنده‌ی بخش الف واحد ۰۹ که ورودی‌شان کامل داخل همین Tab موجود
     * است (لنز/فاصله، Static+Handheld، Extreme Wide+Shallow DoF، Rack Focus+Subject
     * Count) — زنده نمایش داده می‌شوند اما Auto-Save را مسدود نمی‌کنند (همان تصمیم
     * ADR-051 برای shotDescriptionValidation). Rule پنجم («مدت حرکت دوربین») عمداً
     * وایر نشد — طبق ADR-008، هیچ‌کدام از ۶ Variant واقعاً فیلد duration ندارند (مدت
     * حرکت یک پارامتر خارجی مستقل است که Shot Composer فعلاً معادلی برایش ندارد).
     * `combine` با بیش از ۵ Flow امکان مستقیم ندارد — دو مرحله‌ای (۵تایی + ۳تایی).
     */
    val cameraValidationIssues: StateFlow<List<ValidationIssue>> = combine(
        _lensType, _cameraDistance, _cameraMovement, _stabilization, _depthOfField
    ) { lensType, distance, movement, stabilization, depthOfField ->
        CameraValidationPartial(lensType, distance, movement, stabilization, depthOfField)
    }.let { partial ->
        combine(partial, _focusMode, _subjectCount) { p, focusMode, subjectCount ->
            listOfNotNull(
                checkLensDistanceMismatch(p.lensType, p.distance),
                checkStaticMovementWithHandheldStabilization(p.movement, p.stabilization),
                checkExtremeWideWithShallowDepthOfField(p.distance, p.depthOfField),
                checkRackFocusSubjectCount(focusMode, subjectCount)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Tab «نور و محیط» (فاز ۴ قدم ۴) ---

    /**
     * طبق Shot.lighting/.environment (هر دو SourcedSettings مستقل واحد ۰۵) — این دو
     * منبع/Override کاملاً جدا از هم‌اند (نه یک سوییچ ترکیبی مشترک برای کل Tab)، چون
     * دامنه هم همین‌طور مدل‌شده: یک شات می‌تواند نور را Override کند اما محیط را از
     * صحنه ارث ببرد، یا برعکس.
     */
    private val _lightingSource = MutableStateFlow("scene")
    val lightingSource: StateFlow<String> = _lightingSource.asStateFlow()

    // پیش‌فرض‌ها دقیقاً از جدول «پارامترها»ی بخش الف docs/blueprints/08-scene-conditions.md.
    private val _lightingStyle = MutableStateFlow(LightingStyle.DRAMATIC_LIGHT)
    val lightingStyle: StateFlow<LightingStyle> = _lightingStyle.asStateFlow()

    private val _keyLightPosition = MutableStateFlow(KeyLightPosition.SIDE)
    val keyLightPosition: StateFlow<KeyLightPosition> = _keyLightPosition.asStateFlow()

    private val _contrastRatio = MutableStateFlow(ContrastRatio.MEDIUM)
    val contrastRatio: StateFlow<ContrastRatio> = _contrastRatio.asStateFlow()

    private val _fillLight = MutableStateFlow<FillLight?>(FillLight.SOFT)
    val fillLight: StateFlow<FillLight?> = _fillLight.asStateFlow()

    private val _lightingColorTemperature = MutableStateFlow<ColorTemperature?>(ColorTemperature.NEUTRAL)
    val lightingColorTemperature: StateFlow<ColorTemperature?> = _lightingColorTemperature.asStateFlow()

    private val _shadowQuality = MutableStateFlow<ShadowQuality?>(ShadowQuality.SOFT_SHADOWS)
    val shadowQuality: StateFlow<ShadowQuality?> = _shadowQuality.asStateFlow()

    private val _lightSourceCount = MutableStateFlow<LightSourceCount?>(LightSourceCount.DUAL)
    val lightSourceCount: StateFlow<LightSourceCount?> = _lightSourceCount.asStateFlow()

    private val _lightingMotivation = MutableStateFlow<LightingMotivation?>(LightingMotivation.ARTIFICIAL)
    val lightingMotivation: StateFlow<LightingMotivation?> = _lightingMotivation.asStateFlow()

    private val _environmentSource = MutableStateFlow("scene")
    val environmentSource: StateFlow<String> = _environmentSource.asStateFlow()

    private val _weatherType = MutableStateFlow(WeatherType.CLEAR)
    val weatherType: StateFlow<WeatherType> = _weatherType.asStateFlow()

    // بلوپرینت هیچ پیش‌فرضی برای weatherIntensity نمی‌دهد («—») — برخلاف بقیه‌ی
    // فیلدهای nullable این بخش، عمداً با null (نه یک مقدار حدسی) شروع می‌شود.
    private val _weatherIntensity = MutableStateFlow<WeatherIntensity?>(null)
    val weatherIntensity: StateFlow<WeatherIntensity?> = _weatherIntensity.asStateFlow()

    private val _windStrength = MutableStateFlow<WindStrength?>(WindStrength.NONE)
    val windStrength: StateFlow<WindStrength?> = _windStrength.asStateFlow()

    private val _groundState = MutableStateFlow<GroundState?>(GroundState.DRY)
    val groundState: StateFlow<GroundState?> = _groundState.asStateFlow()

    private val _visibility = MutableStateFlow<Visibility?>(Visibility.CLEAR)
    val visibility: StateFlow<Visibility?> = _visibility.asStateFlow()

    private val _temperatureFeel = MutableStateFlow<TemperatureFeel?>(TemperatureFeel.MILD)
    val temperatureFeel: StateFlow<TemperatureFeel?> = _temperatureFeel.asStateFlow()

    private val _environmentalMotion = MutableStateFlow<List<EnvironmentalMotion>>(emptyList())
    val environmentalMotion: StateFlow<List<EnvironmentalMotion>> = _environmentalMotion.asStateFlow()

    // --- Tab «صدا» (فاز ۴ قدم ۴) ---

    private val _soundEnabled = MutableStateFlow(false)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    /**
     * از Shot موجود بارگذاری و بدون تغییر دوباره ذخیره می‌شود — این قدم هیچ کنترل UI
     * برای این پرچم نمی‌سازد (دستور کار صریحاً آن را نخواسته بود؛ دکمه‌ی صریح «تولید
     * صداهای محیط» به‌تنهایی الزام Rule 5 را برآورده می‌کند، مستقل از مقدار این پرچم).
     */
    private var loadedAmbientAutoGenerate = true

    private val _ambientSounds = MutableStateFlow<List<AmbientSound>>(emptyList())
    val ambientSounds: StateFlow<List<AmbientSound>> = _ambientSounds.asStateFlow()

    private val _actionSounds = MutableStateFlow<List<ActionSound>>(emptyList())
    val actionSounds: StateFlow<List<ActionSound>> = _actionSounds.asStateFlow()

    private val _characterSounds = MutableStateFlow<List<CharacterSound>>(emptyList())
    val characterSounds: StateFlow<List<CharacterSound>> = _characterSounds.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        ioScope.launch {
            if (existingShotId != null) {
                val loaded = repository.loadShot(existingShotId).getOrNull()
                if (loaded != null) {
                    loadedShot = loaded
                    _shotNumber.value = loaded.shotNumber
                    _shotTitle.value = loaded.shotTitle ?: ""
                    _shotDescription.value = loaded.shotDescription
                    _shotDescriptionFaPreview.value = loaded.shotDescriptionFaPreview ?: ""
                    _shotGoal.value = loaded.shotGoal
                    _shotType.value = loaded.shotType
                    _durationSecondsText.value = loaded.durationSeconds.toString()
                    _motionLevel.value = loaded.motionLevel
                    _cinematicModeOverride.value = loaded.cinematicModeOverride
                    _imageReferences.value = loaded.imageReferences
                    _subjectCount.value = loaded.characterIds.size + loaded.objectIds.size
                    _cameraSource.value = loaded.camera.source
                    loaded.camera.overrideValue?.let { camera ->
                        _cameraAngle.value = camera.angle
                        _cameraDistance.value = camera.distance
                        _lensType.value = camera.lensType
                        _depthOfField.value = camera.depthOfField
                        _focusMode.value = camera.focusMode
                        _stabilization.value = camera.stabilization
                        _framing.value = camera.framing
                        _cameraMovement.value = camera.movement
                    }
                    _lightingSource.value = loaded.lighting.source
                    loaded.lighting.overrideValue?.let { lighting ->
                        _lightingStyle.value = lighting.style
                        _keyLightPosition.value = lighting.keyLightPosition
                        _contrastRatio.value = lighting.contrastRatio
                        _fillLight.value = lighting.fillLight
                        _lightingColorTemperature.value = lighting.colorTemperature
                        _shadowQuality.value = lighting.shadowQuality
                        _lightSourceCount.value = lighting.lightSourceCount
                        _lightingMotivation.value = lighting.lightingMotivation
                    }
                    _environmentSource.value = loaded.environment.source
                    loaded.environment.overrideValue?.let { environment ->
                        _weatherType.value = environment.weatherType
                        _weatherIntensity.value = environment.weatherIntensity
                        _windStrength.value = environment.windStrength
                        _groundState.value = environment.groundState
                        _visibility.value = environment.visibility
                        _temperatureFeel.value = environment.temperatureFeel
                        _environmentalMotion.value = environment.environmentalMotion
                    }
                    _soundEnabled.value = loaded.soundProfile.enabled
                    loadedAmbientAutoGenerate = loaded.soundProfile.ambientAutoGenerate
                    _ambientSounds.value = loaded.soundProfile.ambientSounds
                    _actionSounds.value = loaded.soundProfile.actionSounds
                    _characterSounds.value = loaded.soundProfile.characterSounds
                }
            } else {
                val existingShots = repository.loadAllShots(sceneId).first()
                _shotNumber.value = existingShots.size + 1
            }

            // یافته‌ی #۱۳ appendix ADR-081 (ADR-086) — بارگذاری یک‌باره‌ی Scene/DNA/
            // Asset های لازم برای شمارش زنده‌ی Blocking/Warning؛ هم‌الگو دقیق با
            // ValidationViewModel (بارگذاری Shot→Scene→DNA با Fallback خنثی→Asset ها).
            cachedScene = sceneRepository.loadScene(sceneId).getOrNull()
            cachedDna = projectDnaRepository.loadProjectDna(projectId).getOrNull()
                ?: defaultProjectDna(projectId) { "dna_placeholder" }
            val shotForAssets = loadedShot
            if (shotForAssets != null) {
                cachedCharacterAssets = assetRepository.loadCharacterAssets(shotForAssets.characterIds).getOrNull() ?: emptyList()
                cachedObjectAssets = assetRepository.loadObjectAssets(shotForAssets.objectIds).getOrNull() ?: emptyList()
                cachedLocationAssets = assetRepository.loadLocationAssets(shotForAssets.locationIds).getOrNull() ?: emptyList()
            }

            _isReady.value = true
            refreshValidationSummary()
        }
    }

    /**
     * یافته‌ی #۱۳ appendix ADR-081 (ADR-086) — بازمحاسبه‌ی همزمان (نه Coroutine
     * جدا) روی همان `Shot` تازه‌ساخته‌شده‌ی `save()`. تصمیم مستقل — بدون
     * Debounce: `aggregateShotValidation` یک تابع خالص (بدون I/O) روی چند
     * Enum/String ساده است؛ حتی برای یک Shot با تمام فیلدهای پر، اجرای آن چند
     * برابر سریع‌تر از خودِ عملیات I/O ذخیره‌سازی (`repository.saveShot`) است که
     * از قبل (بدون Debounce) با هر کلیدفشاری اجرا می‌شود — افزودن یک محاسبه‌ی
     * خالص و سبک به همان مسیر موجود هیچ ریسک Jank تازه‌ای اضافه نمی‌کند.
     */
    private fun refreshValidationSummary(shot: Shot = buildShot()) {
        val scene = cachedScene ?: return
        val dna = cachedDna ?: return
        _validationSummary.value = aggregateShotValidation(
            shot, scene, dna, cachedCharacterAssets, cachedObjectAssets, cachedLocationAssets
        )
        // تکمیل Rule یتیم — قدم ۴ از ۴ (ADR-112): همان scene/dna محاسبه‌ی بالا —
        // بدون بارگذاری/محاسبه‌ی جداگانه.
        _effectiveCinematicMode.value = resolveEffectiveCinematicMode(dna, scene, shot)
    }

    fun setShotTitle(value: String) { _shotTitle.value = value; save() }
    fun setShotDescription(value: String) { _shotDescription.value = value; save() }
    fun setShotDescriptionFaPreview(value: String) { _shotDescriptionFaPreview.value = value; save() }
    fun setShotGoal(value: ShotGoal) { _shotGoal.value = value; save() }
    fun setShotType(value: ShotType) { _shotType.value = value; save() }
    fun setDurationSecondsText(value: String) { _durationSecondsText.value = value; save() }
    fun setMotionLevel(value: MotionLevel) { _motionLevel.value = value; save() }
    fun setCinematicModeOverride(value: CinematicMode?) { _cinematicModeOverride.value = value; save() }

    fun setCameraSource(value: String) { _cameraSource.value = value; save() }
    fun setCameraAngle(value: CameraAngle) { _cameraAngle.value = value; save() }
    fun setCameraDistance(value: CameraDistance) { _cameraDistance.value = value; save() }
    fun setLensType(value: LensType) { _lensType.value = value; save() }
    fun setDepthOfField(value: DepthOfField) { _depthOfField.value = value; save() }
    fun setFocusMode(value: FocusMode) { _focusMode.value = value; save() }
    fun setStabilization(value: Stabilization) { _stabilization.value = value; save() }
    fun setFraming(value: Framing) { _framing.value = value; save() }

    /** طبق دستور کار: انتخاب هر Kind → یک نمونه‌ی پیش‌فرض معنادار از همان Variant (نه فقط پاک‌کردن مقادیر). */
    fun setMovementTier(tier: CameraMovementTier) {
        _cameraMovement.value = when (tier) {
            CameraMovementTier.BASIC -> CameraMovement.Basic(BasicMovementType.STATIC)
            CameraMovementTier.ADVANCED -> CameraMovement.Orbit(degrees = 180, speed = "medium", maintainEyeLevel = true)
        }
        save()
    }

    fun setAdvancedMovementType(type: AdvancedMovementType) {
        _cameraMovement.value = when (type) {
            AdvancedMovementType.ORBIT -> CameraMovement.Orbit(degrees = 180, speed = "medium", maintainEyeLevel = true)
            AdvancedMovementType.DRONE_PATH -> CameraMovement.DronePath(altitudeChange = "level", pathType = "straight", speed = "medium")
            AdvancedMovementType.DOLLY_ZOOM -> CameraMovement.DollyZoom(focalStart = 35, focalEnd = 85, direction = "in")
            AdvancedMovementType.HANDHELD_SHAKE -> CameraMovement.HandheldShake(intensity = 5, frequency = "medium")
            AdvancedMovementType.COMPOUND -> CameraMovement.Compound(primary = "dolly_in", secondary = "orbit", sync = "matched")
        }
        save()
    }

    /**
     * Setter های زیر فقط وقتی معنا دارند که Variant فعلی‌شان همان Variant باشد که
     * UI برایش صدا زده — تضمین‌شده توسط خودِ UI (فقط فیلدهای مربوط به Kind فعلی
     * رندر/فعال می‌شوند)، نه توسط این تابع. `setMovementTier`/`setAdvancedMovementType`
     * همیشه پیش از هر ویرایش فیلد، یک نمونه‌ی جدید از Variant درست می‌سازند.
     */
    private fun updateMovement(transform: (CameraMovement) -> CameraMovement) {
        _cameraMovement.value = transform(_cameraMovement.value)
        save()
    }

    fun setBasicMovementType(type: BasicMovementType) = updateMovement { (it as CameraMovement.Basic).copy(type = type) }
    fun setBasicSpeed(value: String) = updateMovement { (it as CameraMovement.Basic).copy(speed = value) }
    fun setOrbitDegrees(value: Int) = updateMovement { (it as CameraMovement.Orbit).copy(degrees = value) }
    fun setOrbitSpeed(value: String) = updateMovement { (it as CameraMovement.Orbit).copy(speed = value) }
    fun setOrbitMaintainEyeLevel(value: Boolean) = updateMovement { (it as CameraMovement.Orbit).copy(maintainEyeLevel = value) }
    fun setDronePathAltitudeChange(value: String) = updateMovement { (it as CameraMovement.DronePath).copy(altitudeChange = value) }
    fun setDronePathType(value: String) = updateMovement { (it as CameraMovement.DronePath).copy(pathType = value) }
    fun setDronePathSpeed(value: String) = updateMovement { (it as CameraMovement.DronePath).copy(speed = value) }
    fun setDollyZoomFocalStart(value: Int) = updateMovement { (it as CameraMovement.DollyZoom).copy(focalStart = value) }
    fun setDollyZoomFocalEnd(value: Int) = updateMovement { (it as CameraMovement.DollyZoom).copy(focalEnd = value) }
    fun setDollyZoomDirection(value: String) = updateMovement { (it as CameraMovement.DollyZoom).copy(direction = value) }
    fun setHandheldShakeIntensity(value: Int) = updateMovement { (it as CameraMovement.HandheldShake).copy(intensity = value) }
    fun setHandheldShakeFrequency(value: String) = updateMovement { (it as CameraMovement.HandheldShake).copy(frequency = value) }
    fun setCompoundPrimary(value: String) = updateMovement { (it as CameraMovement.Compound).copy(primary = value) }
    fun setCompoundSecondary(value: String) = updateMovement { (it as CameraMovement.Compound).copy(secondary = value) }
    fun setCompoundSync(value: String) = updateMovement { (it as CameraMovement.Compound).copy(sync = value) }

    /**
     * بدون مدیریت واقعی آپلود فایل (بدون Infra انتخاب‌گر تصویر در کل کدبیس —
     * تأییدشده با grep؛ خارج از Scope این قدم که فقط Tab دوربین است). `localFilePath`
     * خالی می‌ماند؛ کاربر فقط نوع + توضیح متنی وارد می‌کند. جزئیات در ADR-052.
     */
    fun addImageReference(type: String, description: String) {
        _imageReferences.value = _imageReferences.value + ImageReference(type = type, localFilePath = "", description = description)
        save()
    }

    fun removeImageReference(index: Int) {
        _imageReferences.value = _imageReferences.value.filterIndexed { i, _ -> i != index }
        save()
    }

    fun setLightingSource(value: String) { _lightingSource.value = value; save() }
    fun setLightingStyle(value: LightingStyle) { _lightingStyle.value = value; save() }
    fun setKeyLightPosition(value: KeyLightPosition) { _keyLightPosition.value = value; save() }
    fun setContrastRatio(value: ContrastRatio) { _contrastRatio.value = value; save() }
    fun setFillLight(value: FillLight?) { _fillLight.value = value; save() }
    fun setLightingColorTemperature(value: ColorTemperature?) { _lightingColorTemperature.value = value; save() }
    fun setShadowQuality(value: ShadowQuality?) { _shadowQuality.value = value; save() }
    fun setLightSourceCount(value: LightSourceCount?) { _lightSourceCount.value = value; save() }
    fun setLightingMotivation(value: LightingMotivation?) { _lightingMotivation.value = value; save() }

    fun setEnvironmentSource(value: String) { _environmentSource.value = value; save() }
    fun setWeatherType(value: WeatherType) { _weatherType.value = value; save() }
    fun setWeatherIntensity(value: WeatherIntensity?) { _weatherIntensity.value = value; save() }
    fun setWindStrength(value: WindStrength?) { _windStrength.value = value; save() }
    fun setGroundState(value: GroundState?) { _groundState.value = value; save() }
    fun setVisibility(value: Visibility?) { _visibility.value = value; save() }
    fun setTemperatureFeel(value: TemperatureFeel?) { _temperatureFeel.value = value; save() }

    /**
     * طبق تصریح بلوپرینت («۰ تا ۳») و دستور کار («غیرفعال‌کردن گزینه‌های بیشتر بعد از
     * انتخاب ۳تا») — محدودیت هم در دامنه (کامنت enum) و هم اینجا رعایت می‌شود: افزودن
     * موردی چهارم وقتی لیست پر است بی‌اثر است (نه Exception، فقط عدم تغییر State) —
     * دفاعی، چون UI خودش با غیرفعال‌کردن Chip از رسیدن این حالت جلوگیری می‌کند.
     */
    fun toggleEnvironmentalMotion(value: EnvironmentalMotion) {
        val current = _environmentalMotion.value
        _environmentalMotion.value = when {
            value in current -> current - value
            current.size < 3 -> current + value
            else -> current
        }
        save()
    }

    fun setSoundEnabled(value: Boolean) { _soundEnabled.value = value; save() }

    /**
     * Rule 5 (docs/design/README.md بخش ۷: «manual only / never auto-generated — only
     * on explicit user action») — این تابع فقط با کلیک صریح دکمه‌ی «تولید صداهای محیط»
     * فراخوانی می‌شود، هرگز از init{} یا هیچ Effect خودکاری. مستقیماً از
     * mapEnvironmentToSound (واحد ۰۸، بدون تغییر منطق) استفاده می‌کند — طبق امضای آن
     * تابع، پارامترها رشته‌ی lowercase هستند، نه enum مستقیم. لیست موجود جایگزین
     * می‌شود (نه Append) — دکمه معنای «بازتولید طبق آب‌وهوای فعلی» دارد.
     */
    fun generateAmbientSounds() {
        val suggestions = mapEnvironmentToSound(
            weatherType = _weatherType.value.name.lowercase(),
            weatherIntensity = (_weatherIntensity.value ?: WeatherIntensity.MEDIUM).name.lowercase(),
            windStrength = (_windStrength.value ?: WindStrength.NONE).name.lowercase()
        )
        // AmbientSoundSuggestion (واحد ۰۸) فیلد source معادل ندارد؛ این مسیر تنها راه
        // ساخت AmbientSound در این ViewModel است (بدون هیچ افزودن/ویرایش دستی)، پس
        // source همیشه "auto_generated" است — طبق ADR-074.
        _ambientSounds.value = suggestions.map { AmbientSound(type = it.type, intensity = it.intensity, description = it.description, source = "auto_generated") }
        save()
    }

    fun removeAmbientSound(index: Int) {
        _ambientSounds.value = _ambientSounds.value.filterIndexed { i, _ -> i != index }
        save()
    }

    fun addActionSound(timestampSeconds: Float, type: String, description: String) {
        _actionSounds.value = _actionSounds.value + ActionSound(timestampSeconds, type, description)
        save()
    }

    fun removeActionSound(index: Int) {
        _actionSounds.value = _actionSounds.value.filterIndexed { i, _ -> i != index }
        save()
    }

    /** بدون انتخاب‌گر کاراکتر از کتابخانه — فرم افزودن دستی ساده طبق تصریح دستور کار. */
    fun addCharacterSound(characterId: String, type: String, description: String) {
        _characterSounds.value = _characterSounds.value + CharacterSound(characterId, type, description)
        save()
    }

    fun removeCharacterSound(index: Int) {
        _characterSounds.value = _characterSounds.value.filterIndexed { i, _ -> i != index }
        save()
    }

    private fun buildLightingSettings(): LightingSettings = LightingSettings(
        style = _lightingStyle.value,
        keyLightPosition = _keyLightPosition.value,
        contrastRatio = _contrastRatio.value,
        fillLight = _fillLight.value,
        colorTemperature = _lightingColorTemperature.value,
        shadowQuality = _shadowQuality.value,
        lightSourceCount = _lightSourceCount.value,
        lightingMotivation = _lightingMotivation.value
    )

    private fun buildEnvironmentSettings(): EnvironmentSettings = EnvironmentSettings(
        weatherType = _weatherType.value,
        weatherIntensity = _weatherIntensity.value,
        windStrength = _windStrength.value,
        groundState = _groundState.value,
        visibility = _visibility.value,
        temperatureFeel = _temperatureFeel.value,
        environmentalMotion = _environmentalMotion.value
    )

    private fun buildCameraSettings(): CameraSettings = CameraSettings(
        angle = _cameraAngle.value,
        distance = _cameraDistance.value,
        movement = _cameraMovement.value,
        lensType = _lensType.value,
        depthOfField = _depthOfField.value,
        focusMode = _focusMode.value,
        stabilization = _stabilization.value,
        framing = _framing.value
    )

    private fun buildShot(): Shot {
        val base = loadedShot
        return Shot(
            shotId = shotId,
            sceneId = sceneId,
            shotNumber = _shotNumber.value,
            shotTitle = _shotTitle.value.ifBlank { null },
            shotDescription = _shotDescription.value,
            shotGoal = _shotGoal.value,
            shotType = _shotType.value,
            durationSeconds = _durationSecondsText.value.toFloatOrNull() ?: 0f,
            motionLevel = _motionLevel.value,
            beats = base?.beats ?: emptyList(),
            imageReferences = _imageReferences.value,
            // overrideValue همیشه با مقادیر فعلی فرم پر می‌شود (حتی وقتی source="scene")
            // تا سوییچ رفت‌وبرگشتی منبع/Override داده‌ی کاربر را گم نکند؛ resolveCameraSettings
            // (واحد ۰۵) خودش overrideValue را فقط وقتی source=="override" باشد در نظر می‌گیرد.
            camera = SourcedSettings(source = _cameraSource.value, overrideValue = buildCameraSettings()),
            // همان منطق camera بالا — overrideValue همیشه با فرم فعلی پر می‌شود.
            lighting = SourcedSettings(source = _lightingSource.value, overrideValue = buildLightingSettings()),
            environment = SourcedSettings(source = _environmentSource.value, overrideValue = buildEnvironmentSettings()),
            soundProfile = SoundProfile(
                enabled = _soundEnabled.value,
                ambientAutoGenerate = loadedAmbientAutoGenerate,
                ambientSounds = _ambientSounds.value,
                actionSounds = _actionSounds.value,
                characterSounds = _characterSounds.value
            ),
            negativePromptOverride = base?.negativePromptOverride,
            characterIds = base?.characterIds ?: emptyList(),
            objectIds = base?.objectIds ?: emptyList(),
            locationIds = base?.locationIds ?: emptyList(),
            overrideScene = base?.overrideScene ?: false,
            cinematicModeOverride = _cinematicModeOverride.value,
            shotDescriptionFaPreview = _shotDescriptionFaPreview.value.ifBlank { null }
        )
    }

    /**
     * Auto-Save بی‌صدا — هم‌الگو با DnaViewModel، طبق دستور کار صریح این قدم. تا
     * `isReady` نشود ذخیره نمی‌کند — چون قبل از آن shotNumber (برای شات جدید) هنوز
     * محاسبه‌نشده یا فیلدهای Shot موجود (برای ویرایش) هنوز بارگذاری‌نشده‌اند؛ ذخیره‌ی
     * زودهنگام یک Shot ناقص/با shotNumber=۰ می‌ساخت.
     */
    private fun save() {
        if (!_isReady.value) return
        val shot = buildShot()
        ioScope.launch { repository.saveShot(shot) }
        refreshValidationSummary(shot)
    }

    companion object {
        /**
         * یافته‌ی #۱۳ appendix ADR-081 (ADR-086): سه Repository تازه (Scene/DNA/
         * Asset) — هم‌الگو دقیق با ValidationViewModel.factory/StudioOutputViewModel.factory:
         * هرکدام مستقل بررسی می‌شوند (نه با «&&»)، وگرنه تزریق جزئی در تست بی‌صدا
         * به AppDatabase Production سقوط می‌کند.
         */
        fun factory(
            application: Application,
            projectId: String,
            sceneId: String,
            shotId: String?,
            repository: ShotRepository? = null,
            sceneRepository: SceneRepository? = null,
            projectDnaRepository: ProjectDnaRepository? = null,
            assetRepository: AssetRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (repository != null || sceneRepository != null || projectDnaRepository != null || assetRepository != null) {
                            ShotComposerViewModel(
                                application = application,
                                projectId = projectId,
                                sceneId = sceneId,
                                existingShotId = shotId,
                                repository = repository ?: ShotRepository(AppDatabase.getInstance(application).shotDao()),
                                sceneRepository = sceneRepository ?: SceneRepository(AppDatabase.getInstance(application).sceneDao()),
                                projectDnaRepository = projectDnaRepository ?: ProjectDnaRepository(AppDatabase.getInstance(application).projectDnaDao()),
                                assetRepository = assetRepository ?: AssetRepository(AppDatabase.getInstance(application).assetDao())
                            )
                        } else {
                            ShotComposerViewModel(application, projectId, sceneId, shotId)
                        }
                    ) as T
            }
    }
}
