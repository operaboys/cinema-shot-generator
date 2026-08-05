package com.operaboys.cinemashotgenerator.ui.shots

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

// واحد ۱۶ فاز ۴ — قدم ۲ — بخش ب: ViewModel اسکلت Shot Composer. فقط فیلدهای
// سطح‌بالای مشترک (طبق دستور کار صریح این قدم) — فیلدهای هر Tab (دوربین/نور/صدا/
// اصلی-پیشرفته) کار قدم ۳ است. هم‌الگو با SceneDetailViewModel برای بارگذاری یک
// Shot موجود (idProvider تزریق‌پذیر، `isReady` برای جلوگیری از Auto-Save زودهنگام
// قبل از تکمیل بارگذاری/محاسبه‌ی shotNumber).

internal fun generateShotId(): String = "shot_" + UUID.randomUUID().toString().replace("-", "").take(12)

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
            imageReferences = base?.imageReferences ?: emptyList(),
            camera = base?.camera ?: SourcedSettings(),
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
