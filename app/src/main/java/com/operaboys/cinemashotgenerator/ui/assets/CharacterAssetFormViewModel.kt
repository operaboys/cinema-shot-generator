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
import com.operaboys.cinemashotgenerator.domain.asset.OutfitCondition
import com.operaboys.cinemashotgenerator.domain.asset.PhysicalAppearance
import com.operaboys.cinemashotgenerator.domain.asset.checkSimilarAssetName
import com.operaboys.cinemashotgenerator.domain.asset.defaultLockLevelForTier
import com.operaboys.cinemashotgenerator.domain.asset.validateBasePrompt
import com.operaboys.cinemashotgenerator.domain.asset.validateDefaultOutfitExists
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

// واحد ۱۶ فاز ۳ — قدم ۲ — بخش الف: ViewModel فرم ساخت Character.
// هم‌الگو با AiStoryBreakdownViewModel/DnaViewModel: تزریق‌پذیری Repository،
// `ioScope` قابل‌Override برای تست، سیگنال `saveCompleted` برای برگشت خودکار به
// صفحه‌ی Assets بعد از ذخیره‌ی موفق. جزئیات کامل تصمیمات در
// docs/adr/049-unit16-phase3-step2-asset-forms.md.
//
// رفع G5 (ADR-067 بخش ه، ADR-095): مدیریت کامل چند-Outfit شرطی — قبلاً فرم فقط
// یک Outfit پیش‌فرض ساده می‌ساخت (تصمیم F5 قدیمی، Placeholder Snackbar روی دکمه‌ی
// «مدیریت لباس‌ها»)؛ اکنون `_outfits` یک لیست واقعی است. منطق انتخاب خودکار
// (`selectOutfitForScene`، domain/asset/AssetSelection.kt) و مصرف‌کننده‌ی واقعی‌اش
// (`enforceCharacterContinuity`، domain/promptengine/CharacterContinuity.kt) از قبل
// درست پیاده بودند و در این قدم تغییر نکردند — این قدم فقط UI را به آن‌ها وصل
// می‌کند.
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

    // همیشه با دقیقاً یک Outfit پیش‌فرض شروع می‌شود — دقیقاً هم‌رفتار با مقدار
    // پیش‌فرض قدیمی («Default» + توضیح خالی)، تا Rule ۵ (validateDefaultOutfitExists)
    // برای یک Character تازه‌ساز بدون هیچ تعامل کاربر هم برقرار بماند.
    private val _outfits = MutableStateFlow(listOf(Outfit(id = generateAssetFormId("outfit"), name = "Default", description = "", isDefault = true)))
    val outfits: StateFlow<List<Outfit>> = _outfits.asStateFlow()

    // رفع یافته‌ی G14 «کاندید وصل آینده» (ADR-064، ADR-092): existingNames از همان
    // Repository.loadAllCharacterAssets (Flow زنده، هم‌الگو با AssetLibraryViewModel)
    // می‌آید؛ خودِ Asset در حال ویرایش (existingAssetId) از فهرست کنار گذاشته
    // می‌شود تا هشدار کاذب «مشابه خودش» تولید نشود.
    private val existingNames = repository.loadAllCharacterAssets(projectId)
        .map { list -> list.filter { it.assetId != existingAssetId }.map { it.name } }

    /** Rule 11 (Warning) روی basePrompt — Rule 5 اکنون روی لیست واقعی `_outfits` بررسی می‌شود (نه یک preview مصنوعی که همیشه true بود). */
    val validationIssues: StateFlow<List<ValidationIssue>> = combine(_name, _outfits, _basePrompt, existingNames) { name, outfits, basePrompt, names ->
        listOfNotNull(
            validateDefaultOutfitExists(outfits),
            validateBasePrompt(basePrompt.ifBlank { null }),
            name.takeIf { it.isNotBlank() }?.let { checkSimilarAssetName(it, names) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saveCompleted = MutableStateFlow(false)
    val saveCompleted: StateFlow<Boolean> = _saveCompleted.asStateFlow()

    // outfits.isNotEmpty() دفاع دومی است (اولی: removeOutfit هرگز آخرین Outfit را
    // حذف نمی‌کند) — پوشش حالت مرزی ویرایش یک Character موجودی که مستقیم (نه از
    // طریق این فرم) با لیست Outfit خالی ساخته شده بود.
    val canSave: StateFlow<Boolean> = combine(_name, _ageRange, _outfits) { name, ageRange, outfits ->
        name.isNotBlank() && ageRange.isNotBlank() && outfits.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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
        _outfits.value = asset.outfits
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

    /** اولین Outfit افزوده‌شده به یک لیست خالی خودکار isDefault=true می‌شود — تنها راهی که لیست می‌تواند از صفر شروع کند و Rule ۵ همچنان برقرار بماند. */
    fun addOutfit(name: String, description: String) {
        if (name.isBlank()) return
        val newOutfit = Outfit(
            id = generateAssetFormId("outfit"),
            name = name,
            description = description,
            isDefault = _outfits.value.isEmpty()
        )
        _outfits.value = _outfits.value + newOutfit
    }

    /**
     * رفتار مرزی «حذف Outfit پیش‌فرض»: با grep در پروژه هیچ الگوی مشابه موجودی
     * (حذف از یک لیست با دقیقاً یک عضو الزامی/پیش‌فرض) پیدا نشد — این اولین مورد
     * است. تصمیم: حذف تنها Outfit باقی‌مانده مسدود می‌شود (no-op)، نه فقط با یک
     * ValidationIssue بلوکه‌شده — چون selectOutfitForScene/selectOutfitForShot
     * دقیقاً `outfits.first { it.isDefault }` را روی یک لیست خالی صدا می‌زنند
     * (NoSuchElementException واقعی در زمان اجرا، نه فقط یک نقض Rule قابل‌نادیده-
     * گرفتن) — تضمین ساختاری در سطح ViewModel امن‌تر از تکیه بر canSave/Validation
     * است (هم‌راستا با اصل «تضمین ساختاری، نه Runtime» دیده‌شده در ADR-006/ADR-093).
     * اگر Outfit حذف‌شده پیش‌فرض بود، اولین عضو باقی‌مانده خودکار پیش‌فرض می‌شود.
     */
    fun removeOutfit(index: Int) {
        val current = _outfits.value
        if (index !in current.indices || current.size <= 1) return
        val removedWasDefault = current[index].isDefault
        val updated = current.filterIndexed { i, _ -> i != index }
        _outfits.value = if (removedWasDefault) {
            updated.mapIndexed { i, outfit -> if (i == 0) outfit.copy(isDefault = true) else outfit }
        } else updated
    }

    fun setOutfitAsDefault(index: Int) {
        val current = _outfits.value
        if (index !in current.indices) return
        _outfits.value = current.mapIndexed { i, outfit -> outfit.copy(isDefault = i == index) }
    }

    // سه شرط condition — هر سه enum بسته‌ی از‌قبل‌موجود در دامنه (نه رشته‌ی آزاد،
    // طبق بررسی صریح این قدم): weather از WeatherType (sceneconditions/
    // EnvironmentModels.kt، هم‌الگو با sceneWeather واقعی در PromptAssembly.kt —
    // `weatherType.name.lowercase()`)؛ timeOfDay/locationType از TimeOfDay/
    // LocationType واحد ۰۴ (domain/scene/SceneModels.kt) — همان enum هایی که خودِ
    // Scene.locationType/timeOfDay استفاده می‌کنند، نه enum جدای Asset Library
    // (domain.asset.LocationType، مفهوم دیگری: دسته‌بندی فیلتر کتابخانه، طبق کامنت
    // صریح خودِ AssetModels.kt خط ۲۰۲-۲۱۲). هر سه با همان قرارداد `.name.lowercase()`
    // ذخیره می‌شوند تا اگر matching آینده به timeOfDay/locationType هم گسترش یابد
    // (فعلاً فقط weather در selectOutfitForScene مقایسه می‌شود)، بدون نیاز به تغییر
    // Casing کار کند.
    fun setOutfitConditionWeather(index: Int, weather: WeatherType?) {
        updateOutfitCondition(index) { it.copy(weather = weather?.name?.lowercase()) }
    }

    fun setOutfitConditionTimeOfDay(index: Int, timeOfDay: TimeOfDay?) {
        updateOutfitCondition(index) { it.copy(timeOfDay = timeOfDay?.name?.lowercase()) }
    }

    fun setOutfitConditionLocationType(index: Int, locationType: LocationType?) {
        updateOutfitCondition(index) { it.copy(locationType = locationType?.name?.lowercase()) }
    }

    private fun updateOutfitCondition(index: Int, transform: (OutfitCondition) -> OutfitCondition) {
        val current = _outfits.value
        if (index !in current.indices) return
        val newCondition = transform(current[index].condition ?: OutfitCondition())
        val normalized = if (newCondition.weather == null && newCondition.timeOfDay == null && newCondition.locationType == null) null else newCondition
        _outfits.value = current.mapIndexed { i, outfit -> if (i == index) outfit.copy(condition = normalized) else outfit }
    }

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
            outfits = _outfits.value,
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
