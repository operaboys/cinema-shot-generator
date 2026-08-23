package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.SecureKeyRepository
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
import com.operaboys.cinemashotgenerator.domain.asset.ReferenceImage
import com.operaboys.cinemashotgenerator.domain.asset.UpdateResult
import com.operaboys.cinemashotgenerator.domain.asset.buildCharacterBaseImagePrompt
import com.operaboys.cinemashotgenerator.domain.asset.buildOutfitImagePrompt
import com.operaboys.cinemashotgenerator.domain.asset.checkSimilarAssetName
import com.operaboys.cinemashotgenerator.domain.asset.defaultLockLevelForTier
import com.operaboys.cinemashotgenerator.domain.asset.generateImagePromptWithAi
import com.operaboys.cinemashotgenerator.domain.asset.styleTokensForImagePrompt
import com.operaboys.cinemashotgenerator.domain.asset.validateBasePrompt
import com.operaboys.cinemashotgenerator.domain.asset.validateCharacterImagePromptInputs
import com.operaboys.cinemashotgenerator.domain.asset.validateCharacterUpdate
import com.operaboys.cinemashotgenerator.domain.asset.validateDefaultOutfitExists
import com.operaboys.cinemashotgenerator.domain.asset.validateOutfitImagePromptInputs
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import com.operaboys.cinemashotgenerator.domain.storybreakdown.BUILTIN_AI_CONNECTOR_PROFILES
import com.operaboys.cinemashotgenerator.domain.storybreakdown.GEMINI_API_PROFILE
import com.operaboys.cinemashotgenerator.domain.storybreakdown.translateToFarsi
import com.operaboys.cinemashotgenerator.domain.storybreakdown.validateApiKeyProvided
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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

// فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۴ از ۵ (ADR-134): دو نوع خصوصی
// واسط برای combine سه‌مرحله‌ای characterSnapshot پایین‌تر (بیش از ۵ Flow).
private data class SnapshotPartial1(val name: String, val ageRange: String, val gender: Gender, val height: String, val build: String)
private data class SnapshotPartial2(val p1: SnapshotPartial1, val hairColor: String, val hairStyle: String, val hairLength: String, val facialEyes: String)

/**
 * فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۴ از ۵ (ADR-134): استخراج‌شده از
 * منطق قبلاً تکراری save() تا هم توسط save() و هم توسط characterSnapshot
 * (Preview زنده) بدون کپی منطق hair/facialFeatures استفاده شود.
 */
private fun buildPhysicalAppearance(
    ageRange: String,
    gender: Gender,
    height: String,
    build: String,
    hairColor: String,
    hairStyle: String,
    hairLength: String,
    facialEyes: String,
    facialDistinctiveMarks: String,
    physicalFeatures: String
): PhysicalAppearance {
    val hair = if (hairColor.isNotBlank() || hairStyle.isNotBlank() || hairLength.isNotBlank()) {
        Hair(color = hairColor, style = hairStyle, length = hairLength)
    } else null
    val distinctiveMarks = facialDistinctiveMarks.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val facialFeatures = if (facialEyes.isNotBlank() || distinctiveMarks.isNotEmpty()) {
        FacialFeatures(eyes = facialEyes, distinctiveMarks = distinctiveMarks)
    } else null
    return PhysicalAppearance(
        ageRange = ageRange,
        gender = gender,
        height = height.ifBlank { null },
        build = build.ifBlank { null },
        hair = hair,
        physicalFeatures = physicalFeatures.ifBlank { null },
        facialFeatures = facialFeatures
    )
}

class CharacterAssetFormViewModel(
    application: Application,
    private val projectId: String,
    private val repository: AssetRepository = AssetRepository(AppDatabase.getInstance(application).assetDao()),
    private val idProvider: () -> String = { generateAssetFormId("char") },
    // رفع G7 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md): هم‌الگو
    // دقیق با ShotComposerViewModel.existingShotId — null یعنی «Asset جدید»
    // (رفتار قبلی، بدون تغییر)، غیر-null یعنی بارگذاری و ویرایش Asset موجود.
    private val existingAssetId: String? = null,
    // فیچر مستقل «ترجمه‌ی مجدد با AI» (ADR-124) — هم‌الگو دقیق با
    // AiStoryBreakdownViewModel/OutputDeliveryViewModel: تزریق‌پذیر تا تست‌ها
    // SharedPreferences معمولی/MockEngine جایگزین کنند.
    private val secureKeyRepository: SecureKeyRepository = SecureKeyRepository(application),
    private val httpClientEngine: HttpClientEngine = OkHttp.create(),
    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۴ از ۵ (ADR-134): برای
    // Style Tokens لازم برای buildCharacterBaseImagePrompt/buildOutfitImagePrompt
    // (زیرقدم ۲، ADR-132) به ProjectDna پروژه نیاز است. تزریق‌پذیر، هم‌الگو با
    // بقیه‌ی Repository های این ViewModel.
    private val projectDnaRepository: ProjectDnaRepository = ProjectDnaRepository(AppDatabase.getInstance(application).projectDnaDao()),
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

    // سیستم Preview دوزبانه‌ی پرامپت — قدم ۳ از ۳ زیرقدم، پایانی (ADR-123):
    // هم‌الگو دقیق با basePrompt بالا — Preview فارسی قابل‌ویرایش کاربر،
    // هرگز وارد basePrompt/پرامپت نهایی نمی‌شود.
    private val _descriptionFaPreview = MutableStateFlow("")
    val descriptionFaPreview: StateFlow<String> = _descriptionFaPreview.asStateFlow()

    // فیچر مستقل «ترجمه‌ی مجدد با AI» (ADR-124) — هم‌الگو دقیق با
    // AiStoryBreakdownViewModel.selectedProfileId/apiKeySavedForSelectedProfile:
    // شرط سخت‌گیرانه‌ی UI (بررسی پیش‌فعال hasApiKey، نه فقط واکنش به خطای بعد از
    // کلیک). پیش‌فرض Gemini چون تنها پروفایل با یک لایه‌ی رایگان دائمی است
    // (تحقیق کامل در ADR-124) — نه انحصار، کاربر هر پروفایل ذخیره‌شده را می‌تواند
    // انتخاب کند.
    private val _selectedTranslationProfileId = MutableStateFlow(GEMINI_API_PROFILE.profileId)
    val selectedTranslationProfileId: StateFlow<String> = _selectedTranslationProfileId.asStateFlow()

    private val _apiKeySavedForTranslationProfile = MutableStateFlow(false)
    val apiKeySavedForTranslationProfile: StateFlow<Boolean> = _apiKeySavedForTranslationProfile.asStateFlow()

    private val _translationInProgress = MutableStateFlow(false)
    val translationInProgress: StateFlow<Boolean> = _translationInProgress.asStateFlow()

    private val _translationError = MutableStateFlow<String?>(null)
    val translationError: StateFlow<String?> = _translationError.asStateFlow()

    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۴ از ۵ (ADR-134): پرامپت عکس
    // شخصیت پایه/خنثی (بدون لباس داستانی، مستقل از هر Outfit — Outfit.imagePromptQuick/...
    // پایین‌تر، از خودِ لیست _outfits می‌آید، بدون نیاز به StateFlow جداگانه).
    private val _projectDna = MutableStateFlow<ProjectDna?>(null)
    val projectDna: StateFlow<ProjectDna?> = _projectDna.asStateFlow()

    private val _imagePromptQuick = MutableStateFlow<String?>(null)
    val imagePromptQuick: StateFlow<String?> = _imagePromptQuick.asStateFlow()

    private val _imagePromptAi = MutableStateFlow<String?>(null)
    val imagePromptAi: StateFlow<String?> = _imagePromptAi.asStateFlow()

    private val _imagePromptFaPreview = MutableStateFlow<String?>(null)
    val imagePromptFaPreview: StateFlow<String?> = _imagePromptFaPreview.asStateFlow()

    private val _imagePromptGeneratedAt = MutableStateFlow<Long?>(null)
    val imagePromptGeneratedAt: StateFlow<Long?> = _imagePromptGeneratedAt.asStateFlow()

    private val _imagePromptAiInProgress = MutableStateFlow(false)
    val imagePromptAiInProgress: StateFlow<Boolean> = _imagePromptAiInProgress.asStateFlow()

    private val _imagePromptAiError = MutableStateFlow<String?>(null)
    val imagePromptAiError: StateFlow<String?> = _imagePromptAiError.asStateFlow()

    // اتصال Rule های یتیم ADR-132 (ADR-136): نتیجه‌ی validateCharacterImagePromptInputs
    // روی آخرین پرامپت تولیدشده (سریع یا AI، هرکدام آخر بار موفق بود) — Warning-only،
    // هیچ دکمه‌ای را مسدود نمی‌کند، فقط پس از هر تولید موفق دوباره محاسبه می‌شود.
    private val _imagePromptValidationIssues = MutableStateFlow<List<ValidationIssue>>(emptyList())
    val imagePromptValidationIssues: StateFlow<List<ValidationIssue>> = _imagePromptValidationIssues.asStateFlow()

    // هر Outfit در حال Generate با AI با شناسه‌ی خودش (outfit.id) ردیابی می‌شود —
    // چون هر ردیف Outfit مستقل از بقیه است (کاربر می‌تواند هم‌زمان روی دو Outfit
    // مختلف کلیک کند)، یک Boolean سراسری تک‌مقداره کافی نبود.
    private val _outfitImagePromptAiInProgress = MutableStateFlow<Set<String>>(emptySet())
    val outfitImagePromptAiInProgress: StateFlow<Set<String>> = _outfitImagePromptAiInProgress.asStateFlow()

    private val _outfitImagePromptAiError = MutableStateFlow<Map<String, String>>(emptyMap())
    val outfitImagePromptAiError: StateFlow<Map<String, String>> = _outfitImagePromptAiError.asStateFlow()

    // اتصال Rule های یتیم ADR-132 (ADR-136): هم‌الگو با outfitImagePromptAiInProgress —
    // نتیجه‌ی validateOutfitImagePromptInputs هر Outfit با outfit.id خودش ردیابی
    // می‌شود، چون نتیجه‌ی هر Outfit مستقل است و با تولید Outfit بعدی جایگزین نمی‌شود.
    private val _outfitImagePromptValidationIssues = MutableStateFlow<Map<String, List<ValidationIssue>>>(emptyMap())
    val outfitImagePromptValidationIssues: StateFlow<Map<String, List<ValidationIssue>>> = _outfitImagePromptValidationIssues.asStateFlow()

    /**
     * زمان آخرین ذخیره‌ی واقعی این Asset روی دیسک، قبل از این جلسه‌ی ویرایش —
     * `var` معمولی (نه StateFlow)، دقیقاً هم‌الگو با `loadedShot` در
     * ShotComposerViewModel.kt: فقط یک‌بار در applyLoadedAsset() پر می‌شود و در
     * طول همین جلسه‌ی ویرایش ثابت می‌ماند (تا save() واقعاً کلیک شود). تشخیص
     * «قدیمی‌شدن پرامپت عکس» این مقدار را با imagePromptGeneratedAt هر پرامپت
     * مقایسه می‌کند — نه یک updatedAt «زنده» که با هر کلیدفشاری تغییر کند.
     */
    private var loadedUpdatedAt: Long? = null

    /**
     * یافته‌ی حیاتی چکاپ نهایی (ADR-143/144): تصویر کامل CharacterAsset
     * بارگذاری‌شده — برخلاف loadedUpdatedAt (فقط یک Long)، اینجا کل Asset لازم
     * است تا checkContinuityBeforeSave بتواند فیلد به فیلد با وضعیت فعلی فرم
     * مقایسه کند. فقط یک‌بار در applyLoadedAsset پر می‌شود؛ Asset تازه
     * (existingAssetId == null) این مقدار را null نگه می‌دارد — یک Character
     * تازه هنوز چیزی برای قفل‌شدن ندارد.
     */
    private var loadedCharacterAsset: CharacterAsset? = null

    private val _continuityIssues = MutableStateFlow<List<ValidationIssue>>(emptyList())
    val continuityIssues: StateFlow<List<ValidationIssue>> = _continuityIssues.asStateFlow()

    // همیشه با دقیقاً یک Outfit پیش‌فرض شروع می‌شود — دقیقاً هم‌رفتار با مقدار
    // پیش‌فرض قدیمی («Default» + توضیح خالی)، تا Rule ۵ (validateDefaultOutfitExists)
    // برای یک Character تازه‌ساز بدون هیچ تعامل کاربر هم برقرار بماند.
    private val _outfits = MutableStateFlow(listOf(Outfit(id = generateAssetFormId("outfit"), name = "Default", description = "", isDefault = true)))
    val outfits: StateFlow<List<Outfit>> = _outfits.asStateFlow()

    // فیچر مستقل جدید «آپلود عکس مرجع واقعی Asset» — زیرقدم ۱ از ۳ (ADR-137):
    // description هر ReferenceImage عمداً همیشه در save() (پایین‌تر) از روی
    // وضعیت فعلی فرم بازمحاسبه می‌شود، نه در همین‌جا هنگام افزودن — تا اگر
    // کاربر بعداً نام/ظاهر شخصیت را عوض کرد، توضیح هم به‌روز بماند.
    private val _referenceImages = MutableStateFlow<List<ReferenceImage>>(emptyList())
    val referenceImages: StateFlow<List<ReferenceImage>> = _referenceImages.asStateFlow()

    fun addReferenceImage(uri: String) {
        _referenceImages.value = _referenceImages.value + ReferenceImage(localFilePath = uri, description = "")
    }

    fun removeReferenceImage(index: Int) {
        val current = _referenceImages.value
        if (index !in current.indices) return
        _referenceImages.value = current.filterIndexed { i, _ -> i != index }
    }

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

    /**
     * فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۴ از ۵ (ADR-134): CharacterAsset
     * زنده‌ی «Preview» — فقط فیلدهایی که buildCharacterBaseImagePrompt/
     * buildOutfitImagePrompt (ADR-132) واقعاً می‌خوانند (name/physicalAppearance/
     * defaultMood؛ outfits/tier اینجا بی‌اثرند، پس خالی/ثابت مانده‌اند). `combine`
     * با بیش از ۵ Flow امکان مستقیم ندارد — سه مرحله‌ای (۵+۵+۳)، هم‌الگو با
     * ShotComposerViewModel.cameraValidationIssues.
     */
    private val characterSnapshot: StateFlow<CharacterAsset> = combine(
        _name, _ageRange, _gender, _height, _build
    ) { name, ageRange, gender, height, build ->
        SnapshotPartial1(name, ageRange, gender, height, build)
    }.let { partial1 ->
        combine(partial1, _hairColor, _hairStyle, _hairLength, _facialEyes) { p1, hairColor, hairStyle, hairLength, facialEyes ->
            SnapshotPartial2(p1, hairColor, hairStyle, hairLength, facialEyes)
        }
    }.let { partial2 ->
        combine(partial2, _facialDistinctiveMarks, _physicalFeatures, _defaultMood) { p2, marks, physicalFeatures, defaultMood ->
            CharacterAsset(
                assetId = existingAssetId ?: "preview",
                characterTier = CharacterTier.MAIN,
                name = p2.p1.name,
                physicalAppearance = buildPhysicalAppearance(
                    ageRange = p2.p1.ageRange,
                    gender = p2.p1.gender,
                    height = p2.p1.height,
                    build = p2.p1.build,
                    hairColor = p2.hairColor,
                    hairStyle = p2.hairStyle,
                    hairLength = p2.hairLength,
                    facialEyes = p2.facialEyes,
                    facialDistinctiveMarks = marks,
                    physicalFeatures = physicalFeatures
                ),
                outfits = emptyList(),
                defaultMood = defaultMood.ifBlank { null }
            )
        }
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        CharacterAsset(assetId = "preview", characterTier = CharacterTier.MAIN, name = "", physicalAppearance = PhysicalAppearance(ageRange = "", gender = Gender.OTHER), outfits = emptyList())
    )

    /** پیش‌نمایش زنده‌ی پرامپت Template شخصیت پایه — بدون دکمه، بدون فراخوان AI، طبق تصمیم معماری این زیرقدم. */
    val characterBaseImagePromptPreview: StateFlow<String?> = combine(characterSnapshot, _projectDna) { character, dna ->
        dna?.let { buildCharacterBaseImagePrompt(character, it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
        ioScope.launch { refreshApiKeySavedForTranslation(_selectedTranslationProfileId.value) }
        // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۴ از ۵ (ADR-134): بارگذاری
        // یک‌باره‌ی ProjectDna برای Style Tokens — هم‌الگو با بارگذاری Asset بالا،
        // بدون Fallback ساختگی؛ null یعنی هنوز DNA ای برای این پروژه ذخیره نشده،
        // که یعنی دکمه‌های ساخت پرامپت (Preview/Quick/AI) فعلاً چیزی تولید
        // نمی‌کنند (طبق null-چک صریح هر سه مسیر پایین‌تر).
        ioScope.launch { _projectDna.value = projectDnaRepository.loadProjectDna(projectId).getOrNull() }
    }

    /**
     * یافته‌ی مستندشده‌ی G2 (هنوز صادق، AiStoryBreakdownViewModel.kt/
     * OutputDeliveryViewModel.kt): SecureKeyRepository پیش‌فرض‌تزریق‌نشده روی
     * Robolectric واقعاً KeyStoreException پرتاب می‌کند؛ runCatching این ریسک را
     * می‌بندد — fail-closed به false هم با شرط سخت‌گیرانه‌ی محصولی هم‌راستاست.
     */
    private suspend fun refreshApiKeySavedForTranslation(profileId: String) {
        _apiKeySavedForTranslationProfile.value = runCatching { secureKeyRepository.hasApiKey(profileId) }.getOrDefault(false)
    }

    /** هم‌الگو دقیق با AiStoryBreakdownViewModel.selectProfile — Job چون hasApiKey روی Dispatchers.IO واقعی اجرا می‌شود. */
    fun selectTranslationProfile(profileId: String): Job {
        _selectedTranslationProfileId.value = profileId
        return ioScope.launch { refreshApiKeySavedForTranslation(profileId) }
    }

    /**
     * دکمه‌ی «ترجمه‌ی مجدد با AI» — basePrompt (متن انگلیسی منبع) به
     * translateToFarsi داده می‌شود؛ نتیجه‌ی موفق مستقیم جایگزین
     * descriptionFaPreview می‌شود. طبق بررسی مستقل این ViewModel (برخلاف
     * ShotComposerViewModel): هیچ Setter دیگری اینجا (setBasePrompt/
     * setDescriptionFaPreview/...) خودکار save() را صدا نمی‌زند — فقط دکمه‌ی
     * صریح «ذخیره»/canSave این کار را می‌کند؛ پس این‌جا هم فقط StateFlow را
     * به‌روز می‌کند، هم‌الگو دقیق با بقیه‌ی Setter های این ViewModel، بدون
     * صدازدن save() مستقیم. Rule ۴ (validateApiKeyProvided) قبل از هر تلاش
     * واقعی HTTP اعمال می‌شود — عیناً هم‌الگو با sendPromptAutomatically/
     * analyzePromptQualityWithAi.
     */
    fun retranslate(): Job {
        if (_translationInProgress.value) return Job().apply { complete() }
        val profile = BUILTIN_AI_CONNECTOR_PROFILES.firstOrNull { it.profileId == _selectedTranslationProfileId.value }
            ?: return Job().apply { complete() }
        _translationError.value = null
        return ioScope.launch {
            val apiKey = secureKeyRepository.loadApiKey(profile.profileId)
            if (apiKey == null || validateApiKeyProvided(apiKey) != null) {
                _translationError.value = "ابتدا کلید API را در تنظیمات وارد کنید"
                return@launch
            }
            _translationInProgress.value = true
            val result = translateToFarsi(_basePrompt.value, profile, apiKey, httpClientEngine)
            _translationInProgress.value = false
            result.fold(
                onSuccess = { translated -> _descriptionFaPreview.value = translated },
                onFailure = {
                    _translationError.value = it.message ?: "درخواست به AI Connector با خطا مواجه شد"
                }
            )
        }
    }

    /**
     * فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۴ از ۵ (ADR-134): آیا پرامپت
     * عکس یک Asset/Outfit از آخرین ویرایش واقعی این Character قدیمی‌تر است —
     * `loadedUpdatedAt` (زمان بارگذاری، نه زمان زنده‌ی همین جلسه‌ی ویرایش) با
     * زمان تولید همان پرامپت مقایسه می‌شود. برای هر دو مصرف (پرامپت شخصیت پایه و
     * پرامپت هر Outfit) یکسان است — طبق ADR-131، `updatedAt` فقط روی خودِ
     * CharacterAsset تعریف شده، نه روی Outfit؛ چون ویرایش هر Outfit هم ویرایش
     * همان Character واحد است (هر دو با یک save() ذخیره می‌شوند)، همان
     * loadedUpdatedAt سطح Character برای Outfit ها هم معیار درستی است.
     */
    fun isImagePromptStale(generatedAt: Long?): Boolean =
        generatedAt != null && (loadedUpdatedAt ?: 0) > generatedAt

    /** پرامپت سریع (بدون AI) شخصیت پایه — فوری، بدون فراخوان شبکه. */
    fun generateCharacterBaseImagePromptQuick() {
        val dna = _projectDna.value ?: return
        val prompt = buildCharacterBaseImagePrompt(characterSnapshot.value, dna)
        _imagePromptQuick.value = prompt
        _imagePromptGeneratedAt.value = System.currentTimeMillis()
        _imagePromptValidationIssues.value = validateCharacterImagePromptInputs(characterSnapshot.value, dna, prompt)
    }

    /**
     * پرامپت حرفه‌ای (با AI) شخصیت پایه — هم‌الگو دقیق با retranslate()/
     * analyzePromptQualityWithAi (بررسی پیش‌فعال کلید API، وضعیت InProgress/
     * Error). طبق تصمیم مستقل این زیرقدم، از همان انتخاب‌گر پروفایل موجود فیچر
     * «ترجمه‌ی مجدد» (`_selectedTranslationProfileId`) بازاستفاده می‌شود — هر دو
     * فیچر مفهوم یکسانی («کدام AI Connector Profile برای این فرم») دارند؛ افزودن
     * یک انتخاب‌گر پروفایل کاملاً مستقل دوم برای همان فرم فقط پیچیدگی UI بی‌فایده
     * اضافه می‌کرد بدون سود واقعی.
     */
    fun generateCharacterBaseImagePromptWithAi(): Job {
        if (_imagePromptAiInProgress.value) return Job().apply { complete() }
        val dna = _projectDna.value ?: return Job().apply { complete() }
        val profile = BUILTIN_AI_CONNECTOR_PROFILES.firstOrNull { it.profileId == _selectedTranslationProfileId.value }
            ?: return Job().apply { complete() }
        _imagePromptAiError.value = null
        return ioScope.launch {
            val apiKey = secureKeyRepository.loadApiKey(profile.profileId)
            if (apiKey == null || validateApiKeyProvided(apiKey) != null) {
                _imagePromptAiError.value = "ابتدا کلید API را در تنظیمات وارد کنید"
                return@launch
            }
            _imagePromptAiInProgress.value = true
            val templatePrompt = buildCharacterBaseImagePrompt(characterSnapshot.value, dna)
            val result = generateImagePromptWithAi(templatePrompt, styleTokensForImagePrompt(dna), profile, apiKey, httpClientEngine)
            _imagePromptAiInProgress.value = false
            result.fold(
                onSuccess = { response ->
                    _imagePromptAi.value = response.imagePromptEn
                    _imagePromptFaPreview.value = response.imagePromptFa
                    _imagePromptGeneratedAt.value = System.currentTimeMillis()
                    _imagePromptValidationIssues.value = validateCharacterImagePromptInputs(characterSnapshot.value, dna, response.imagePromptEn)
                },
                onFailure = { _imagePromptAiError.value = it.message ?: "درخواست به AI Connector با خطا مواجه شد" }
            )
        }
    }

    private fun updateOutfitById(outfitId: String, transform: (Outfit) -> Outfit) {
        _outfits.value = _outfits.value.map { if (it.id == outfitId) transform(it) else it }
    }

    /** پرامپت سریع (بدون AI) یک Outfit مشخص — فوری، بدون فراخوان شبکه. */
    fun generateOutfitImagePromptQuick(index: Int) {
        val dna = _projectDna.value ?: return
        val outfit = _outfits.value.getOrNull(index) ?: return
        val prompt = buildOutfitImagePrompt(outfit, characterSnapshot.value, dna)
        updateOutfitById(outfit.id) { it.copy(imagePromptQuick = prompt, imagePromptGeneratedAt = System.currentTimeMillis()) }
        _outfitImagePromptValidationIssues.value = _outfitImagePromptValidationIssues.value + (outfit.id to validateOutfitImagePromptInputs(outfit, dna, prompt))
    }

    /**
     * پرامپت حرفه‌ای (با AI) یک Outfit مشخص. `outfit.id` (نه `index`) برای
     * ردیابی InProgress/Error و برای به‌روزرسانی نهایی استفاده می‌شود — چون
     * این یک عملیات Async است، لیست Outfit ها ممکن است در طول اجرای آن تغییر
     * کند (مثلاً کاربر Outfit دیگری را حذف کند)؛ index یک ثابت لحظه‌ای است، اما
     * id پایدار می‌ماند.
     */
    fun generateOutfitImagePromptWithAi(index: Int): Job {
        val outfit = _outfits.value.getOrNull(index) ?: return Job().apply { complete() }
        if (outfit.id in _outfitImagePromptAiInProgress.value) return Job().apply { complete() }
        val dna = _projectDna.value ?: return Job().apply { complete() }
        val profile = BUILTIN_AI_CONNECTOR_PROFILES.firstOrNull { it.profileId == _selectedTranslationProfileId.value }
            ?: return Job().apply { complete() }
        _outfitImagePromptAiError.value = _outfitImagePromptAiError.value - outfit.id
        return ioScope.launch {
            val apiKey = secureKeyRepository.loadApiKey(profile.profileId)
            if (apiKey == null || validateApiKeyProvided(apiKey) != null) {
                _outfitImagePromptAiError.value = _outfitImagePromptAiError.value + (outfit.id to "ابتدا کلید API را در تنظیمات وارد کنید")
                return@launch
            }
            _outfitImagePromptAiInProgress.value = _outfitImagePromptAiInProgress.value + outfit.id
            val templatePrompt = buildOutfitImagePrompt(outfit, characterSnapshot.value, dna)
            val result = generateImagePromptWithAi(templatePrompt, styleTokensForImagePrompt(dna), profile, apiKey, httpClientEngine)
            _outfitImagePromptAiInProgress.value = _outfitImagePromptAiInProgress.value - outfit.id
            result.fold(
                onSuccess = { response ->
                    updateOutfitById(outfit.id) {
                        it.copy(imagePromptAi = response.imagePromptEn, imagePromptFaPreview = response.imagePromptFa, imagePromptGeneratedAt = System.currentTimeMillis())
                    }
                    _outfitImagePromptValidationIssues.value = _outfitImagePromptValidationIssues.value + (outfit.id to validateOutfitImagePromptInputs(outfit, dna, response.imagePromptEn))
                },
                onFailure = {
                    _outfitImagePromptAiError.value = _outfitImagePromptAiError.value + (outfit.id to (it.message ?: "درخواست به AI Connector با خطا مواجه شد"))
                }
            )
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
        _descriptionFaPreview.value = asset.descriptionFaPreview.orEmpty()
        _outfits.value = asset.outfits
        // فیچر مستقل جدید «آپلود عکس مرجع واقعی Asset» — زیرقدم ۱ از ۳ (ADR-137).
        _referenceImages.value = asset.referenceImages
        // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۴ از ۵ (ADR-134).
        _imagePromptQuick.value = asset.imagePromptQuick
        _imagePromptAi.value = asset.imagePromptAi
        _imagePromptFaPreview.value = asset.imagePromptFaPreview
        _imagePromptGeneratedAt.value = asset.imagePromptGeneratedAt
        loadedUpdatedAt = asset.updatedAt
        loadedCharacterAsset = asset
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
    fun setDescriptionFaPreview(value: String) { _descriptionFaPreview.value = value }

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
     * ValidationIssue بلوکه‌شده — چون selectOutfitForScene (domain/asset/
     * AssetSelection.kt، مصرف‌کننده‌ی واقعی: enforceCharacterContinuity)
     * دقیقاً `outfits.first { it.isDefault }` را روی یک لیست خالی صدا می‌زند
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
    // ذخیره می‌شوند — قراردادی که این‌جا انتخاب شد دقیقاً همان چیزی بود که تکمیل G5
    // (ADR-096) به آن نیاز داشت: matching اکنون واقعاً به هر سه فیلد گسترش یافته
    // (selectOutfitForScene/selectExpressionForScene، domain/asset/AssetSelection.kt)،
    // بدون نیاز به هیچ تغییر Casing در این ViewModel.
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

    /**
     * یافته‌ی حیاتی چکاپ نهایی (ADR-143/144): validateCharacterUpdate تا این
     * قدم از هیچ Screen/ViewModel واقعی صدا زده نمی‌شد — این تابع دقیقاً همان
     * شکاف را می‌بندد. فقط برای ویرایش Asset موجود اجرا می‌شود
     * (loadedCharacterAsset != null). age_range عمداً از بقیه‌ی
     * physicalAppearance جدا مقایسه می‌شود، چون appearanceLock/ageLock دو
     * قفل کاملاً مستقل‌اند (طبق امضای واقعی سه‌آرگومانی
     * validateCharacterUpdate در AssetContinuity.kt) — یک تغییر فقط در
     * ageRange نباید appearanceLock را هم فعال کند؛ `physicalAppearance.copy
     * (ageRange = newPhysicalAppearance.ageRange)` دقیقاً همین تفکیک را با
     * خنثی‌کردن ageRange پیش از مقایسه‌ی برابری تضمین می‌کند. asset_id بررسی
     * نمی‌شود چون هیچ فیلد فرمی برای تغییرش وجود ندارد (existingAssetId در
     * طول ویرایش ثابت است). نتیجه: اگر هر فیلد تغییریافته Blocked برگرداند،
     * save() باید متوقف شود؛ Warned هرگز مانع نمی‌شود (طبق تعریف پروژه‌ای
     * Warning).
     */
    private fun checkContinuityBeforeSave(newName: String, newPhysicalAppearance: PhysicalAppearance): Boolean {
        val loaded = loadedCharacterAsset ?: run {
            _continuityIssues.value = emptyList()
            return true
        }
        val changedFields = mutableListOf<String>()
        if (loaded.name != newName) changedFields += "name"
        if (loaded.physicalAppearance.ageRange != newPhysicalAppearance.ageRange) changedFields += "age_range"
        if (loaded.physicalAppearance.copy(ageRange = newPhysicalAppearance.ageRange) != newPhysicalAppearance) {
            changedFields += "physical_appearance"
        }

        val rules = ContinuityRules()
        val level = continuityLockLevel.value
        _continuityIssues.value = changedFields.mapNotNull { field ->
            when (val result = validateCharacterUpdate(level, rules, field)) {
                is UpdateResult.Blocked -> ValidationIssue(severity = Severity.BLOCKING, field = field, message = result.reason)
                is UpdateResult.Warned -> ValidationIssue(severity = Severity.WARNING, field = field, message = result.message)
                UpdateResult.Allowed -> null
            }
        }
        return _continuityIssues.value.none { it.severity == Severity.BLOCKING }
    }

    fun save() {
        if (!canSave.value) return
        val physicalAppearance = buildPhysicalAppearance(
            ageRange = _ageRange.value,
            gender = _gender.value,
            height = _height.value,
            build = _build.value,
            hairColor = _hairColor.value,
            hairStyle = _hairStyle.value,
            hairLength = _hairLength.value,
            facialEyes = _facialEyes.value,
            facialDistinctiveMarks = _facialDistinctiveMarks.value,
            physicalFeatures = _physicalFeatures.value
        )
        if (!checkContinuityBeforeSave(_name.value, physicalAppearance)) return

        // فیچر مستقل جدید «آپلود عکس مرجع واقعی Asset» — زیرقدم ۱ از ۳ (ADR-137):
        // description هر ReferenceImage عمداً همین‌جا (نه در addReferenceImage)
        // از روی وضعیت فعلی فرم بازمحاسبه می‌شود — Derived، نه فیکس‌شده در لحظه‌ی
        // افزودن عکس، تا تغییر بعدی نام/ظاهر شخصیت را هم منعکس کند.
        val referenceImageDescription = "${_name.value} — ${physicalAppearance.toPromptString()}"
        val referenceImages = _referenceImages.value.map { it.copy(description = referenceImageDescription) }

        val asset = CharacterAsset(
            assetId = existingAssetId ?: idProvider(),
            characterTier = _tier.value,
            name = _name.value,
            physicalAppearance = physicalAppearance,
            outfits = _outfits.value,
            defaultMood = _defaultMood.value.ifBlank { null },
            basePrompt = _basePrompt.value.ifBlank { null },
            continuityRules = ContinuityRules(),
            continuityLockLevel = continuityLockLevel.value,
            referenceImages = referenceImages,
            descriptionFaPreview = _descriptionFaPreview.value.ifBlank { null },
            // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۴ از ۵ (ADR-134):
            // updatedAt همیشه زمان همین save() واقعی است — بدون این، تشخیص
            // «قدیمی‌شدن پرامپت عکس» (isImagePromptStale بالا) که کل این فیچر
            // برایش طراحی شده هرگز کار نمی‌کرد (فیلد تا این زیرقدم هرگز ست
            // نمی‌شد، طبق یافته‌ی مستند دستور کار این قدم).
            imagePromptQuick = _imagePromptQuick.value,
            imagePromptAi = _imagePromptAi.value,
            imagePromptFaPreview = _imagePromptFaPreview.value,
            imagePromptGeneratedAt = _imagePromptGeneratedAt.value,
            updatedAt = System.currentTimeMillis()
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
            existingAssetId: String? = null,
            projectDnaRepository: ProjectDnaRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    (
                        if (repository != null || projectDnaRepository != null) {
                            CharacterAssetFormViewModel(
                                application = application,
                                projectId = projectId,
                                repository = repository ?: AssetRepository(AppDatabase.getInstance(application).assetDao()),
                                existingAssetId = existingAssetId,
                                projectDnaRepository = projectDnaRepository ?: ProjectDnaRepository(AppDatabase.getInstance(application).projectDnaDao())
                            )
                        } else {
                            CharacterAssetFormViewModel(application, projectId, existingAssetId = existingAssetId)
                        }
                    ) as T
            }
    }
}
