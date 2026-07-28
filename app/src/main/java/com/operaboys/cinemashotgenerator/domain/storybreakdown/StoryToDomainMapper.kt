package com.operaboys.cinemashotgenerator.domain.storybreakdown

import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.ContinuityRules
import com.operaboys.cinemashotgenerator.domain.asset.Environment
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.asset.Outfit
import com.operaboys.cinemashotgenerator.domain.asset.PhysicalAppearance
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.SceneLocation
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.util.UUID
import kotlin.math.abs

// واحد ۰۱ب — AI Story Breakdown (بخش ث: Story-to-Domain Mapper)
// منبع حقیقت: docs/blueprints/01b-ai-story-breakdown.md (نسخه ۲)
//
// این حیاتی‌ترین بخش این واحد است — تبدیل داده‌ی ساده‌ی AI به Asset/Scene/Shot واقعی،
// با مقادیر پیش‌فرض معقول (اصل بنیادی بلوپرینت: AI بیرونی مسئولیت‌های فنی داخلی را بر
// عهده ندارد). ادامه‌ی قدم‌های اول (PromptBuilder/ChunkCombiner) و دوم (JsonDoctor).
// بخش ب (AI Connector) عمداً در این قدم هم دست‌نخورده ماند.
//
// @Serializable روی Simple*FromAi (لایه‌ی domain) — انحراف از قرارداد معمول پروژه
// (جدا نگه‌داشتن Serialization در لایه‌ی data/repository/) اما دقیقاً هم‌الگو با
// انحراف تأییدشده‌ی قبلی در Renderer.kt (ADR-025: «import مستقیم kotlinx.serialization
// در لایه‌ی domain/ طبق تصمیم صریح معمار» — نه یک الگوی عمومی جدید). این‌جا هم دلیل
// مشابه است: این ۴ نوع مستقیماً از JSON خام AI Decode می‌شوند (طبق دستور صریح دستور
// کار)، بدون یک DTO میانی جداگانه در data/repository/ که در این قدم هنوز معنا ندارد
// (چون هیچ Repository ای این داده را ذخیره نمی‌کند — مستقیم به Mapper می‌رود).

@Serializable
data class SimpleCharacterFromAi(
    val name: String,
    val description: String,
    val role: String,
    val gender: String? = null
)

@Serializable
data class SimpleLocationFromAi(val name: String, val description: String)

@Serializable
data class SimpleObjectFromAi(val name: String, val description: String)

@Serializable
data class SimpleShotFromAi(
    val sceneName: String,
    val shotNumber: Int,
    val description: String,
    val characterNames: List<String> = emptyList(),
    val locationName: String,
    val objectNames: List<String> = emptyList()
)

@Serializable
private data class SimpleAiResponse(
    val characters: List<SimpleCharacterFromAi> = emptyList(),
    val locations: List<SimpleLocationFromAi> = emptyList(),
    val objects: List<SimpleObjectFromAi> = emptyList(),
    val shots: List<SimpleShotFromAi> = emptyList()
)

private fun generateId(prefix: String): String = "${prefix}_" + UUID.randomUUID().toString().replace("-", "").take(12)

/**
 * حداقل یک Outfit پیش‌فرض معتبر (طبق Rule 5 واحد ۰۶: هر کاراکتر باید حداقل یک Outfit
 * با isDefault=true داشته باشد). توضیح صریح می‌گوید کاربر باید بعداً در Asset Library
 * دقیقش کند — هم‌الگو با عبارت‌های Placeholder دیگر بلوپرینت (مثل materialAndColor
 * در mapAiObjectToAsset).
 */
private fun defaultOutfitPlaceholder(): Outfit = Outfit(
    id = generateId("outfit"),
    name = "Default",
    description = "لباس پیش‌فرض — نیاز به بررسی و تکمیل کاربر در Asset Library",
    isDefault = true
)

/**
 * محیط خنثی و معقول برای LocationAsset — چون AI بیرونی فقط name/description متنی
 * می‌دهد، نه جزئیات محیط ساختاریافته (طبق اصل بنیادی بلوپرینت: مفاهیم فنی داخلی از AI
 * خواسته نمی‌شوند). مقادیر خنثی/میانه انتخاب شدند تا نه فرض غلط «همیشه داخلی» کنند نه
 * «همیشه بیرونی» — کاربر بعداً در Asset Library دقیق می‌کند.
 */
private fun deriveEnvironmentPlaceholder(): Environment = Environment(
    type = "unspecified",
    size = "medium",
    lightingCondition = "natural"
)

/**
 * SceneLocation از روی locationName خام AI — LocationType.CUSTOM انتخاب شد (نه
 * INDOOR/OUTDOOR حدسی) چون از یک رشته‌ی نام آزاد (مثل «خیابان شلوغ شهری») نمی‌توان با
 * اطمینان تشخیص داد مکان داخلی است یا بیرونی؛ ادعای نادرست INDOOR/OUTDOOR بدتر از یک
 * دسته‌ی صریحاً «نامشخص/سفارشی» است. هم‌راستا با انتخاب محافظه‌کارانه‌ی مشابه بلوپرینت
 * برای ObjectSubtype.GENERAL_PROP.
 */
private fun deriveSceneLocationPlaceholder(locationName: String): SceneLocation =
    SceneLocation(type = LocationType.CUSTOM, description = locationName)

/**
 * تبدیل خروجی ساده‌ی AI به CharacterAsset واقعی (واحد ۰۶، نسخه‌ی ۵).
 * character_tier و gender مستقیماً از AI می‌آیند (چون به زبان طبیعی بیان شده‌اند)؛
 * بقیه‌ی فیلدهای فنی مقدار پیش‌فرض معقول می‌گیرند و کاربر بعداً در Asset Library
 * (مرحله‌ی ۳ گردش کار) دستی دقیق‌شان می‌کند.
 */
fun mapAiCharacterToAsset(aiChar: SimpleCharacterFromAi): CharacterAsset {
    val tier = when (aiChar.role.lowercase()) {
        "main" -> CharacterTier.MAIN
        "background" -> CharacterTier.BACKGROUND
        else -> CharacterTier.SECONDARY
    }
    val gender = when (aiChar.gender?.lowercase()) {
        "female" -> Gender.FEMALE
        "male" -> Gender.MALE
        else -> Gender.OTHER
    }
    return CharacterAsset(
        assetId = generateId("char"),
        characterTier = tier,
        name = aiChar.name,
        physicalAppearance = PhysicalAppearance(
            ageRange = "unspecified",
            gender = gender,
            physicalFeatures = aiChar.description
        ),
        outfits = listOf(defaultOutfitPlaceholder()),
        basePrompt = aiChar.description,
        continuityRules = ContinuityRules()
    )
}

/** LocationAsset با continuityLockLevel پیش‌فرض STYLE (از خودِ data class، بدون نیاز به ست دستی). */
fun mapAiLocationToAsset(aiLoc: SimpleLocationFromAi): LocationAsset = LocationAsset(
    assetId = generateId("loc"),
    name = aiLoc.name,
    description = aiLoc.description,
    environment = deriveEnvironmentPlaceholder(),
    basePrompt = aiLoc.description
)

/**
 * ObjectAsset با continuityLockLevel پیش‌فرض FORM. subtype اکنون فیلد الزامی است
 * (طبق بلوپرینت ۰۶ نسخه ۴) — چون AI بیرونی نمی‌تواند قابل‌اعتماد تشخیص دهد یک شیء
 * «شخصی»، «عمومی»، یا «لباس» است، پیش‌فرض محافظه‌کارانه GENERAL_PROP انتخاب شد.
 */
fun mapAiObjectToAsset(aiObj: SimpleObjectFromAi): ObjectAsset = ObjectAsset(
    assetId = generateId("obj"),
    name = aiObj.name,
    description = aiObj.description,
    subtype = ObjectSubtype.GENERAL_PROP,
    size = "medium",
    materialAndColor = "نامشخص — نیاز به بررسی کاربر",
    basePrompt = aiObj.description
)

/**
 * شات‌های با sceneName یکسان، به یک Scene واحد گروه‌بندی می‌شوند (طبق ترتیب اولین
 * ظهور). narrativeRole=DEVELOPMENT/atmospherePrimary=CALM پیش‌فرض‌های خنثی معقول‌اند
 * (نه RISING_ACTION/NEUTRAL که در enum های واقعی وجود ندارند — تأییدشده با grep،
 * تأیید یک اصلاح قبلی بلوپرینت است، نه یافته‌ی جدید این قدم).
 */
fun groupAiShotsIntoScenes(aiShots: List<SimpleShotFromAi>): List<Scene> {
    return aiShots.groupBy { it.sceneName }.map { (sceneName, shotsInScene) ->
        Scene(
            sceneId = generateId("scene"),
            sceneTitle = sceneName,
            sceneNumber = aiShots.indexOfFirst { it.sceneName == sceneName } + 1,
            narrativeRole = NarrativeRole.DEVELOPMENT,
            location = deriveSceneLocationPlaceholder(shotsInScene.first().locationName),
            timeOfDay = TimeOfDay.AFTERNOON,
            atmospherePrimary = Atmosphere.CALM,
            shotCount = shotsInScene.size
        )
    }
}

/**
 * خروجی mapAiShotToShot — علاوه بر Shot، فهرست نام‌های یافت‌نشده (کاراکتر/مکان/شیء) را
 * هم برمی‌گرداند تا Rule 9 بتواند بدون نیاز به دوباره‌محاسبه‌کردن تطبیق نام‌ها، گزارش
 * بدهد. یک data class ساده انتخاب شد (نه Pair) چون دو مقدار برگشتی معنای متفاوت و
 * نام‌دار دارند — خواناتر از Pair<Shot, List<String>>.
 */
data class ShotMappingResult(val shot: Shot, val unmatchedNames: List<String>)

/**
 * تبدیل هر شات ساده‌ی AI به Shot واقعی — با اتصال به Asset هایی که قبلاً ساخته شدند
 * (بر اساس تطبیق نام دقیق). نام یافت‌نشده بی‌صدا نادیده گرفته نمی‌شود — در
 * unmatchedNames جمع می‌شود تا Rule 9 مصرفش کند.
 */
fun mapAiShotToShot(
    aiShot: SimpleShotFromAi,
    sceneId: String,
    characterAssetsByName: Map<String, CharacterAsset>,
    locationAssetsByName: Map<String, LocationAsset>,
    objectAssetsByName: Map<String, ObjectAsset>
): ShotMappingResult {
    val matchedCharacterIds = aiShot.characterNames.mapNotNull { characterAssetsByName[it]?.assetId }
    val unmatchedCharacterNames = aiShot.characterNames.filterNot { it in characterAssetsByName }

    val matchedObjectIds = aiShot.objectNames.mapNotNull { objectAssetsByName[it]?.assetId }
    val unmatchedObjectNames = aiShot.objectNames.filterNot { it in objectAssetsByName }

    val matchedLocation = locationAssetsByName[aiShot.locationName]
    val unmatchedLocationNames = if (matchedLocation == null) listOf(aiShot.locationName) else emptyList()

    val shot = Shot(
        shotId = generateId("shot"),
        sceneId = sceneId,
        shotNumber = aiShot.shotNumber,
        shotDescription = aiShot.description,
        shotGoal = ShotGoal.ESTABLISHING,
        shotType = ShotType.MEDIUM,
        durationSeconds = 4f,
        motionLevel = MotionLevel.MODERATE,
        soundProfile = SoundProfile(enabled = true),
        characterIds = matchedCharacterIds,
        objectIds = matchedObjectIds,
        locationIds = matchedLocation?.let { listOf(it.assetId) } ?: emptyList()
    )
    return ShotMappingResult(shot, unmatchedCharacterNames + unmatchedObjectNames + unmatchedLocationNames)
}

/** Rule 9 (Warning): نام کاراکتر/مکان/شیء در یک شات با هیچ Asset ساخته‌شده مطابقت ندارد. */
fun validateAllAssetNamesMatched(unmatchedNames: List<String>): ValidationIssue? {
    if (unmatchedNames.isEmpty()) return null
    return ValidationIssue(
        Severity.WARNING,
        message = "نام(های) زیر در فهرست Asset های ساخته‌شده یافت نشدند و بدون آن‌ها به شات وصل شدند: ${unmatchedNames.joinToString("، ")}"
    )
}

/** Rule 10 (Warning): تعداد شات واقعی تولیدشده با targetShotCount درخواستی بیش از ۲۰٪ مغایرت دارد. */
fun validateShotCountMatchesTarget(actualShotCount: Int, targetShotCount: Int): ValidationIssue? {
    if (targetShotCount <= 0) return null
    val deviation = abs(actualShotCount - targetShotCount).toDouble() / targetShotCount
    if (deviation > 0.2) {
        return ValidationIssue(
            Severity.WARNING,
            message = "تعداد شات واقعی تولیدشده ($actualShotCount) با تعداد درخواستی ($targetShotCount) بیش از ۲۰٪ مغایرت دارد"
        )
    }
    return null
}

// Rule 11 (بلوپرینت آن را «ساختاری، نه Validation» توصیف کرده): پیش از اعمال قطعی
// نتیجه‌ی این Mapper، کاربر باید یک بار برای تأیید نهایی آن را ببیند — این یک گام
// تعاملی UI است (واحد ۱۶، هنوز ساخته نشده)، نه منطق دامنه؛ عمداً هیچ تابعی برایش اینجا
// نوشته نشده.

/** نتیجه‌ی نهایی کامل این واحد — آماده برای نمایش تأییدیه‌ی کاربر (Rule 11) در واحد ۱۶. */
data class StoryBreakdownResult(
    val characters: List<CharacterAsset>,
    val locations: List<LocationAsset>,
    val objects: List<ObjectAsset>,
    val scenes: List<Scene>,
    val shots: List<Shot>,
    val warnings: List<ValidationIssue>
)

/** خروجی جریان کامل processAiResponse. */
sealed class ProcessAiResponseResult {
    data class Success(val result: StoryBreakdownResult) : ProcessAiResponseResult()
    data class NeedsManualRepair(val diagnosis: JsonDiagnosis) : ProcessAiResponseResult()
    data class MissingRequiredKeys(val issue: ValidationIssue) : ProcessAiResponseResult()
}

private val aiResponseJson = Json { ignoreUnknownKeys = true }

/**
 * جریان کامل بخش پ→ت→ث: smartCombineChunks → repairJson (قدم قبل) → در صورت موفقیت،
 * بررسی کلیدهای الزامی (Rule 8) → Parse به Simple*FromAi → سه Mapper (کاراکتر/مکان/شیء)
 * → groupAiShotsIntoScenes → mapAiShotToShot برای هر شات → StoryBreakdownResult نهایی
 * (شامل هشدارهای Rule 9/10).
 */
fun processAiResponse(chunks: List<String>, targetShotCount: Int): ProcessAiResponseResult {
    val combined = smartCombineChunks(chunks)

    val repairedJson = when (val repairResult = repairJson(combined)) {
        is JsonRepairResult.Success -> repairResult.repairedJson
        is JsonRepairResult.NeedsManualRepair -> return ProcessAiResponseResult.NeedsManualRepair(repairResult.diagnosis)
    }

    validateRequiredKeysPresent(repairedJson)?.let { return ProcessAiResponseResult.MissingRequiredKeys(it) }

    val aiResponse = aiResponseJson.decodeFromString<SimpleAiResponse>(repairedJson)

    val characters = aiResponse.characters.map { mapAiCharacterToAsset(it) }
    val locations = aiResponse.locations.map { mapAiLocationToAsset(it) }
    val objects = aiResponse.objects.map { mapAiObjectToAsset(it) }

    val charactersByName = characters.associateBy { it.name }
    val locationsByName = locations.associateBy { it.name }
    val objectsByName = objects.associateBy { it.name }

    val scenes = groupAiShotsIntoScenes(aiResponse.shots)
    val sceneIdByName = scenes.associate { (it.sceneTitle ?: "") to it.sceneId }

    val warnings = mutableListOf<ValidationIssue>()
    val shots = aiResponse.shots.map { aiShot ->
        val sceneId = sceneIdByName.getValue(aiShot.sceneName)
        val mapping = mapAiShotToShot(aiShot, sceneId, charactersByName, locationsByName, objectsByName)
        validateAllAssetNamesMatched(mapping.unmatchedNames)?.let { warnings += it }
        mapping.shot
    }

    validateShotCountMatchesTarget(shots.size, targetShotCount)?.let { warnings += it }

    return ProcessAiResponseResult.Success(
        StoryBreakdownResult(
            characters = characters,
            locations = locations,
            objects = objects,
            scenes = scenes,
            shots = shots,
            warnings = warnings
        )
    )
}
