package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterContinuityLevel
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.ContinuityRules
import com.operaboys.cinemashotgenerator.domain.asset.FacialFeatures
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.Hair
import com.operaboys.cinemashotgenerator.domain.asset.Outfit
import com.operaboys.cinemashotgenerator.domain.asset.PhysicalAppearance
import com.operaboys.cinemashotgenerator.domain.asset.defaultLockLevelForTier
import com.operaboys.cinemashotgenerator.domain.asset.validateBasePrompt
import com.operaboys.cinemashotgenerator.domain.asset.validateDefaultOutfitExists
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

// واحد ۱۶ فاز ۳ — قدم ۲ — بخش الف: ViewModel فرم ساخت Character (شامل تصمیم F5).
// هم‌الگو با AiStoryBreakdownViewModel/DnaViewModel: تزریق‌پذیری Repository،
// `ioScope` قابل‌Override برای تست، سیگنال `saveCompleted` برای برگشت خودکار به
// صفحه‌ی Assets بعد از ذخیره‌ی موفق. جزئیات کامل تصمیمات در
// docs/adr/049-unit16-phase3-step2-asset-forms.md.
//
// تصمیم F5 (نگهداری‌شده از ممیزی pre-unit16): فرم فقط یک Outfit پیش‌فرض ساده
// می‌سازد (نام + توضیح، isDefault همیشه true) — دقیقاً هم‌الگو با
// defaultOutfitPlaceholder در domain/storybreakdown/StoryToDomainMapper.kt.
// مدیریت کامل چند-Outfit («مدیریت لباس‌ها») به یک بخش پیشرفته‌ی آینده موکول شده؛
// دکمه‌اش در این قدم فقط Placeholder (Snackbar) است.
class CharacterAssetFormViewModel(
    application: Application,
    private val projectId: String,
    private val repository: AssetRepository = AssetRepository(AppDatabase.getInstance(application).assetDao()),
    private val idProvider: () -> String = { generateAssetFormId("char") },
    // رفع G7 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md): هم‌الگو
    // دقیق با ShotComposerViewModel.existingShotId — null یعنی «Asset جدید»
    // (رفتار قبلی، بدون تغییر)، غیر-null یعنی بارگذاری و ویرایش Asset موجود.
    private val existingAssetId: String? = null,
    ioScopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val ioScope: CoroutineScope = ioScopeOverride ?: viewModelScope

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _tier = MutableStateFlow(CharacterTier.MAIN)
    val tier: StateFlow<CharacterTier> = _tier.asStateFlow()

    /** `null` یعنی «همان پیش‌فرض Tier فعلی را دنبال کن» — طبق تست الزامی «تغییر Tier → تغییر خودکار سطح پیش‌فرض». */
    private val _continuityLockLevelOverride = MutableStateFlow<CharacterContinuityLevel?>(null)
    val continuityLockLevel: StateFlow<CharacterContinuityLevel> = combine(_tier, _continuityLockLevelOverride) { tier, override ->
        override ?: defaultLockLevelForTier(tier)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), defaultLockLevelForTier(CharacterTier.MAIN))

    private val _ageRange = MutableStateFlow("")
    val ageRange: StateFlow<String> = _ageRange.asStateFlow()

    private val _gender = MutableStateFlow(Gender.OTHER)
    val gender: StateFlow<Gender> = _gender.asStateFlow()

    private val _height = MutableStateFlow("")
    val height: StateFlow<String> = _height.asStateFlow()

    private val _build = MutableStateFlow("")
    val build: StateFlow<String> = _build.asStateFlow()

    private val _hairColor = MutableStateFlow("")
    val hairColor: StateFlow<String> = _hairColor.asStateFlow()
    private val _hairStyle = MutableStateFlow("")
    val hairStyle: StateFlow<String> = _hairStyle.asStateFlow()
    private val _hairLength = MutableStateFlow("")
    val hairLength: StateFlow<String> = _hairLength.asStateFlow()

    private val _facialEyes = MutableStateFlow("")
    val facialEyes: StateFlow<String> = _facialEyes.asStateFlow()
    private val _facialDistinctiveMarks = MutableStateFlow("")
    val facialDistinctiveMarks: StateFlow<String> = _facialDistinctiveMarks.asStateFlow()

    private val _physicalFeatures = MutableStateFlow("")
    val physicalFeatures: StateFlow<String> = _physicalFeatures.asStateFlow()

    private val _defaultMood = MutableStateFlow("")
    val defaultMood: StateFlow<String> = _defaultMood.asStateFlow()

    private val _basePrompt = MutableStateFlow("")
    val basePrompt: StateFlow<String> = _basePrompt.asStateFlow()

    private val _outfitName = MutableStateFlow("Default")
    val outfitName: StateFlow<String> = _outfitName.asStateFlow()
    private val _outfitDescription = MutableStateFlow("")
    val outfitDescription: StateFlow<String> = _outfitDescription.asStateFlow()

    /** Rule 11 (Warning) روی basePrompt — Rule 5 (Outfit پیش‌فرض) همیشه ارضا می‌شود چون فرم همیشه یک Outfit با isDefault=true می‌سازد؛ همچنان برای اثبات صریح فراخوانی می‌شود. */
    val validationIssues: StateFlow<List<ValidationIssue>> = combine(_outfitName, _outfitDescription, _basePrompt) { outfitName, outfitDescription, basePrompt ->
        listOfNotNull(
            validateDefaultOutfitExists(listOf(Outfit(id = "preview", name = outfitName, description = outfitDescription, isDefault = true))),
            validateBasePrompt(basePrompt.ifBlank { null })
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saveCompleted = MutableStateFlow(false)
    val saveCompleted: StateFlow<Boolean> = _saveCompleted.asStateFlow()

    val canSave: StateFlow<Boolean> = combine(_name, _ageRange) { name, ageRange -> name.isNotBlank() && ageRange.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // رفع G7 ممیزی post-Unit16: بارگذاری واقعی Asset موجود برای پیش‌پرکردن فرم —
    // loadCharacterAssets (نه یک متد load-by-id تازه) بازاستفاده شد، چون از قبل
    // دقیقاً همین کار را برای یک شناسه‌ی تکی هم انجام می‌دهد (بدون نیاز به
    // AssetDao/Repository تازه).
    init {
        existingAssetId?.let { id ->
            ioScope.launch {
                repository.loadCharacterAssets(listOf(id)).getOrNull()?.firstOrNull()?.let { asset -> applyLoadedAsset(asset) }
            }
        }
    }

    private fun applyLoadedAsset(asset: CharacterAsset) {
        _name.value = asset.name
        _tier.value = asset.characterTier
        _continuityLockLevelOverride.value = asset.continuityLockLevel
        _ageRange.value = asset.physicalAppearance.ageRange
        _gender.value = asset.physicalAppearance.gender
        _height.value = asset.physicalAppearance.height.orEmpty()
        _build.value = asset.physicalAppearance.build.orEmpty()
        _hairColor.value = asset.physicalAppearance.hair?.color.orEmpty()
        _hairStyle.value = asset.physicalAppearance.hair?.style.orEmpty()
        _hairLength.value = asset.physicalAppearance.hair?.length.orEmpty()
        _facialEyes.value = asset.physicalAppearance.facialFeatures?.eyes.orEmpty()
        _facialDistinctiveMarks.value = asset.physicalAppearance.facialFeatures?.distinctiveMarks?.joinToString(", ").orEmpty()
        _physicalFeatures.value = asset.physicalAppearance.physicalFeatures.orEmpty()
        _defaultMood.value = asset.defaultMood.orEmpty()
        _basePrompt.value = asset.basePrompt.orEmpty()
        val defaultOutfit = asset.outfits.find { it.isDefault } ?: asset.outfits.firstOrNull()
        if (defaultOutfit != null) {
            _outfitName.value = defaultOutfit.name
            _outfitDescription.value = defaultOutfit.description
        }
    }

    fun setName(value: String) { _name.value = value }
    fun setTier(value: CharacterTier) { _tier.value = value }
    fun setContinuityLockLevel(value: CharacterContinuityLevel) { _continuityLockLevelOverride.value = value }
    fun setAgeRange(value: String) { _ageRange.value = value }
    fun setGender(value: Gender) { _gender.value = value }
    fun setHeight(value: String) { _height.value = value }
    fun setBuild(value: String) { _build.value = value }
    fun setHairColor(value: String) { _hairColor.value = value }
    fun setHairStyle(value: String) { _hairStyle.value = value }
    fun setHairLength(value: String) { _hairLength.value = value }
    fun setFacialEyes(value: String) { _facialEyes.value = value }
    fun setFacialDistinctiveMarks(value: String) { _facialDistinctiveMarks.value = value }
    fun setPhysicalFeatures(value: String) { _physicalFeatures.value = value }
    fun setDefaultMood(value: String) { _defaultMood.value = value }
    fun setBasePrompt(value: String) { _basePrompt.value = value }
    fun setOutfitName(value: String) { _outfitName.value = value }
    fun setOutfitDescription(value: String) { _outfitDescription.value = value }

    fun save() {
        if (!canSave.value) return
        val hair = if (_hairColor.value.isNotBlank() || _hairStyle.value.isNotBlank() || _hairLength.value.isNotBlank()) {
            Hair(color = _hairColor.value, style = _hairStyle.value, length = _hairLength.value)
        } else null
        val distinctiveMarks = _facialDistinctiveMarks.value.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val facialFeatures = if (_facialEyes.value.isNotBlank() || distinctiveMarks.isNotEmpty()) {
            FacialFeatures(eyes = _facialEyes.value, distinctiveMarks = distinctiveMarks)
        } else null

        val asset = CharacterAsset(
            assetId = existingAssetId ?: idProvider(),
            characterTier = _tier.value,
            name = _name.value,
            physicalAppearance = PhysicalAppearance(
                ageRange = _ageRange.value,
                gender = _gender.value,
                height = _height.value.ifBlank { null },
                build = _build.value.ifBlank { null },
                hair = hair,
                physicalFeatures = _physicalFeatures.value.ifBlank { null },
                facialFeatures = facialFeatures
            ),
            outfits = listOf(Outfit(id = generateAssetFormId("outfit"), name = _outfitName.value, description = _outfitDescription.value, isDefault = true)),
            defaultMood = _defaultMood.value.ifBlank { null },
            basePrompt = _basePrompt.value.ifBlank { null },
            continuityRules = ContinuityRules(),
            continuityLockLevel = continuityLockLevel.value
        )
        ioScope.launch {
            repository.saveCharacterAsset(projectId, asset)
            _saveCompleted.value = true
        }
    }

    companion object {
        fun factory(
            application: Application,
            projectId: String,
            repository: AssetRepository? = null,
            existingAssetId: String? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (repository != null) CharacterAssetFormViewModel(application, projectId, repository, existingAssetId = existingAssetId)
                        else CharacterAssetFormViewModel(application, projectId, existingAssetId = existingAssetId)
                    ) as T
            }
    }
}
