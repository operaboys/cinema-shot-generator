package com.operaboys.cinemashotgenerator.ui.shots

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
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
import com.operaboys.cinemashotgenerator.domain.shot.ImageReference
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.shot.SourcedSettings
import com.operaboys.cinemashotgenerator.domain.shot.validateShotDescription
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
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
    private val sceneId: String,
    private val existingShotId: String?,
    private val repository: ShotRepository = ShotRepository(AppDatabase.getInstance(application).shotDao()),
    private val idProvider: () -> String = ::generateShotId,
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    val shotId: String = existingShotId ?: idProvider()

    /** فیلدهای غیر-سطح‌بالا (Beats/تصاویر/دوربین/نور/صدا/...) از Shot موجود دست‌نخورده حفظ می‌شوند — این قدم فقط فیلدهای سطح‌بالا را می‌نویسد. */
    private var loadedShot: Shot? = null

    private val _shotNumber = MutableStateFlow(0)
    val shotNumber: StateFlow<Int> = _shotNumber.asStateFlow()

    private val _shotTitle = MutableStateFlow("")
    val shotTitle: StateFlow<String> = _shotTitle.asStateFlow()

    private val _shotDescription = MutableStateFlow("")
    val shotDescription: StateFlow<String> = _shotDescription.asStateFlow()

    private val _shotGoal = MutableStateFlow(ShotGoal.ACTION)
    val shotGoal: StateFlow<ShotGoal> = _shotGoal.asStateFlow()

    private val _shotType = MutableStateFlow(ShotType.MEDIUM)
    val shotType: StateFlow<ShotType> = _shotType.asStateFlow()

    private val _durationSecondsText = MutableStateFlow("4")
    val durationSecondsText: StateFlow<String> = _durationSecondsText.asStateFlow()

    private val _motionLevel = MutableStateFlow(MotionLevel.SUBTLE)
    val motionLevel: StateFlow<MotionLevel> = _motionLevel.asStateFlow()

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
                    _shotGoal.value = loaded.shotGoal
                    _shotType.value = loaded.shotType
                    _durationSecondsText.value = loaded.durationSeconds.toString()
                    _motionLevel.value = loaded.motionLevel
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
                }
            } else {
                val existingShots = repository.loadAllShots(sceneId).first()
                _shotNumber.value = existingShots.size + 1
            }
            _isReady.value = true
        }
    }

    fun setShotTitle(value: String) { _shotTitle.value = value; save() }
    fun setShotDescription(value: String) { _shotDescription.value = value; save() }
    fun setShotGoal(value: ShotGoal) { _shotGoal.value = value; save() }
    fun setShotType(value: ShotType) { _shotType.value = value; save() }
    fun setDurationSecondsText(value: String) { _durationSecondsText.value = value; save() }
    fun setMotionLevel(value: MotionLevel) { _motionLevel.value = value; save() }

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
            lighting = base?.lighting ?: SourcedSettings(),
            environment = base?.environment ?: SourcedSettings(),
            soundProfile = base?.soundProfile ?: SoundProfile(enabled = false),
            negativePromptOverride = base?.negativePromptOverride,
            characterIds = base?.characterIds ?: emptyList(),
            objectIds = base?.objectIds ?: emptyList(),
            locationIds = base?.locationIds ?: emptyList(),
            overrideScene = base?.overrideScene ?: false
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
        ioScope.launch { repository.saveShot(buildShot()) }
    }

    companion object {
        fun factory(application: Application, sceneId: String, shotId: String?, repository: ShotRepository? = null): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (repository != null) ShotComposerViewModel(application, sceneId, shotId, repository)
                        else ShotComposerViewModel(application, sceneId, shotId)
                    ) as T
            }
    }
}
