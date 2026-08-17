package com.operaboys.cinemashotgenerator.domain.validation

import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.asset.validateBasePrompt
import com.operaboys.cinemashotgenerator.domain.asset.validateDefaultOutfitExists
import com.operaboys.cinemashotgenerator.domain.asset.validateObjectAsset
import com.operaboys.cinemashotgenerator.domain.audio.validateActionSoundTimeline
import com.operaboys.cinemashotgenerator.domain.camera.CameraMovement
import com.operaboys.cinemashotgenerator.domain.camera.checkExtremeWideWithShallowDepthOfField
import com.operaboys.cinemashotgenerator.domain.camera.checkLensDistanceMismatch
import com.operaboys.cinemashotgenerator.domain.camera.checkRackFocusSubjectCount
import com.operaboys.cinemashotgenerator.domain.camera.checkStaticMovementWithHandheldStabilization
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.dna.validateShotAgainstDna
import com.operaboys.cinemashotgenerator.domain.dna.validateShotDuration
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.checkCalmAtmosphereInClimax
import com.operaboys.cinemashotgenerator.domain.scene.checkNoonLightingInIndoor
import com.operaboys.cinemashotgenerator.domain.scene.validateSceneHasShotsBeforeFinalize
import com.operaboys.cinemashotgenerator.domain.sceneconditions.checkBottomKeyLight
import com.operaboys.cinemashotgenerator.domain.sceneconditions.checkExtremeWideWithLowVisibility
import com.operaboys.cinemashotgenerator.domain.sceneconditions.checkFallingRainWithoutRainyWeather
import com.operaboys.cinemashotgenerator.domain.sceneconditions.checkFireInRainOutdoors
import com.operaboys.cinemashotgenerator.domain.sceneconditions.checkFogWithClearVisibility
import com.operaboys.cinemashotgenerator.domain.sceneconditions.checkMoonlightAtNoon
import com.operaboys.cinemashotgenerator.domain.sceneconditions.checkNoirWithoutHighContrast
import com.operaboys.cinemashotgenerator.domain.sceneconditions.checkRainWithDryGround
import com.operaboys.cinemashotgenerator.domain.sceneconditions.checkSnowWithMismatchedGround
import com.operaboys.cinemashotgenerator.domain.sceneconditions.checkSnowfallWithoutSnowyWeather
import com.operaboys.cinemashotgenerator.domain.sceneconditions.checkStormWithoutWind
import com.operaboys.cinemashotgenerator.domain.sceneconditions.checkStrongFillWithDramaticStyle
import com.operaboys.cinemashotgenerator.domain.sceneconditions.checkSunlightAtNight
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.validateNegativePromptOverride
import com.operaboys.cinemashotgenerator.domain.shot.validateShotBeatTimeline
import com.operaboys.cinemashotgenerator.domain.shot.validateShotDescription
import com.operaboys.cinemashotgenerator.domain.shot.validateShotHasSubject
import com.operaboys.cinemashotgenerator.domain.visualidentity.resolveEffectiveCinematicMode
import com.operaboys.cinemashotgenerator.domain.visualidentity.validateShotDurationForCinematicMode

// واحد ۱۶ فاز ۵ — قدم ۱: صفحه‌ی Validation. منبع حقیقت دوگانه:
// docs/blueprints/16-user-workflow-v2.md («مراحل ۶-۷-۸») و docs/design/README.md
// بخش «۹. Validation». جزئیات کامل تصمیمات (خصوصاً چرا سطح ۱/۲/۳ یک نوع Wrapper
// تازه است، نه فیلد تازه روی ValidationIssue سراسری واحد ۰۷) در
// docs/adr/055-unit16-phase5-step1-validation-screen.md.
//
// تحقیق اولیه (تأییدشده با grep مستقیم): ValidationIssue/Severity/ValidationReport
// (این فایل، ValidationEngine.kt) هیچ فیلد Level ای ندارند — کامنت‌های «Level 1/2/3»
// موجود در چند فایل فقط مستندسازی است، نه یک نوع Runtime. یک Wrapper تازه
// (LeveledValidationIssue) این دسته‌بندی را فقط در خروجی این تجمیع‌کننده اضافه
// می‌کند، بدون دست‌زدن به خودِ ValidationIssue سراسری (که در ده‌ها محل دیگر بدون
// هیچ مفهوم Level ای بازاستفاده می‌شود).

enum class ValidationLevel { DATA_COMPLETENESS, LOGICAL_CONSISTENCY, CONTINUITY_AND_DEPENDENCY }

data class LeveledValidationIssue(val level: ValidationLevel, val issue: ValidationIssue)

data class AggregatedValidationReport(val issues: List<LeveledValidationIssue>) {
    val blockingCount: Int get() = issues.count { it.issue.severity == Severity.BLOCKING }
    val warningCount: Int get() = issues.count { it.issue.severity == Severity.WARNING }
    fun issuesAtLevel(level: ValidationLevel): List<LeveledValidationIssue> = issues.filter { it.level == level }
}

/**
 * تجمیع همه‌ی Rule های موجود پروژه (تا این قدم) برای یک Shot/Scene/DNA/Asset
 * مشخص، در سه سطح Level 1 (کامل‌بودن داده) / Level 2 (سازگاری منطقی) / Level 3
 * (تداوم و وابستگی). تابعی خالص — بدون I/O؛ بارگذاری واقعی Shot/Scene/DNA/Asset
 * کار ValidationViewModel است.
 *
 * دلیل انتخاب دقیق اینکه کدام Rule کجا می‌رود، و کدام Rule های موجود در دامنه
 * عمداً وایر نشدند (مثل MotionIntensityValidation، validateCameraMovementDuration،
 * validateImageReferenceFile، checkMandatoryElementsPresent) در ADR-055 آمده.
 */
fun aggregateShotValidation(
    shot: Shot,
    scene: Scene,
    dna: ProjectDna,
    characterAssets: List<CharacterAsset>,
    objectAssets: List<ObjectAsset>,
    locationAssets: List<LocationAsset>
): AggregatedValidationReport {
    val issues = mutableListOf<LeveledValidationIssue>()
    fun add(level: ValidationLevel, issue: ValidationIssue?) {
        if (issue != null) issues += LeveledValidationIssue(level, issue)
    }
    fun addAll(level: ValidationLevel, list: List<ValidationIssue>) {
        list.forEach { issues += LeveledValidationIssue(level, it) }
    }

    val l1 = ValidationLevel.DATA_COMPLETENESS
    val l2 = ValidationLevel.LOGICAL_CONSISTENCY
    val l3 = ValidationLevel.CONTINUITY_AND_DEPENDENCY

    // --- Level 1: کامل بودن داده (واحد ۰۵ Shot، واحد ۰۴ Scene) ---
    add(l1, validateShotDescription(shot.shotDescription))
    add(l1, validateShotHasSubject(shot.characterIds, shot.objectIds, shot.locationIds))
    addAll(l1, validateShotBeatTimeline(shot.beats, shot.durationSeconds))
    add(l1, validateNegativePromptOverride(shot))
    add(l1, validateSceneHasShotsBeforeFinalize(scene.shotCount))
    // رفع یافته‌ی G14 «کاندید رفع نزدیک» (ADR-064 تصمیم ۹، ADR-092): Action Sound
    // با Timestamp خارج از durationSeconds شات قبلاً هیچ هشداری نمی‌گرفت — هم‌الگو
    // دقیق با validateShotBeatTimeline بالا (که همین Rule زیرین، validateBeatSheetTimeline،
    // را برای Beat Sheet صدا می‌زند).
    addAll(l1, validateActionSoundTimeline(shot.soundProfile.actionSounds, shot.durationSeconds))

    // --- Level 2: سازگاری منطقی (ترکیب فیلدهای خودِ همین Shot/Scene با هم) ---
    val subjectCount = shot.characterIds.size + shot.objectIds.size + shot.locationIds.size
    shot.camera.overrideValue?.let { camera ->
        add(l2, checkLensDistanceMismatch(camera.lensType, camera.distance))
        add(l2, checkStaticMovementWithHandheldStabilization(camera.movement, camera.stabilization))
        add(l2, checkRackFocusSubjectCount(camera.focusMode, subjectCount))
        add(l2, checkExtremeWideWithShallowDepthOfField(camera.distance, camera.depthOfField))
        // تکمیل Rule یتیم — قدم ۳الف از ۲ زیرقدم قدم ۳ (ADR-110): تعارض دوربین ثابت
        // در صحنه‌ی تعقیب. هم‌الگو با بقیه‌ی این بلوک — فقط روی camera.overrideValue
        // صریح شات اجرا می‌شود، نه ارث‌بری‌شده از Scene (همان الگوی موجود سایر Rule
        // های این بلوک، اختراع نشده).
        add(l2, checkStaticCameraInChase(camera.movement.toMovementTypeString(), shot.shotDescription))
        shot.environment.overrideValue?.visibility?.let { visibility ->
            add(l2, checkExtremeWideWithLowVisibility(camera.distance, visibility))
        }
    }
    add(l2, checkNoonLightingInIndoor(scene.timeOfDay, scene.location.type))
    add(l2, checkCalmAtmosphereInClimax(scene.narrativeRole, scene.atmospherePrimary))

    // ۵ Rule «نور» واحد ۰۸ (بلوپرینت 08-scene-conditions.md بخش الف)
    shot.lighting.overrideValue?.let { lighting ->
        lighting.lightingMotivation?.let { motivation ->
            add(l2, checkSunlightAtNight(motivation, scene.timeOfDay))
            add(l2, checkMoonlightAtNoon(motivation, scene.timeOfDay))
        }
        add(l2, checkNoirWithoutHighContrast(lighting.style, lighting.contrastRatio))
        lighting.fillLight?.let { fillLight -> add(l2, checkStrongFillWithDramaticStyle(fillLight, lighting.style)) }
        add(l2, checkBottomKeyLight(lighting.keyLightPosition))
    }

    // ۸ Rule «محیط» واحد ۰۸ (بلوپرینت 08-scene-conditions.md بخش ب)
    shot.environment.overrideValue?.let { environment ->
        environment.visibility?.let { visibility -> add(l2, checkFogWithClearVisibility(environment.weatherType, visibility)) }
        add(l2, checkFallingRainWithoutRainyWeather(environment.environmentalMotion, environment.weatherType))
        add(l2, checkSnowfallWithoutSnowyWeather(environment.environmentalMotion, environment.weatherType))
        environment.groundState?.let { groundState ->
            add(l2, checkRainWithDryGround(environment.weatherType, groundState))
            add(l2, checkSnowWithMismatchedGround(environment.weatherType, groundState))
        }
        environment.windStrength?.let { windStrength -> add(l2, checkStormWithoutWind(environment.weatherType, windStrength)) }
        shot.lighting.overrideValue?.lightingMotivation?.let { motivation ->
            addAll(l2, checkFireInRainOutdoors(environment.weatherType, motivation, scene.location.type))
        }
    }

    // --- Level 3: تداوم و وابستگی (این Shot در برابر DNA پروژه و Asset های متصل) ---
    add(l3, validateShotDuration(shot.durationSeconds.toInt(), dna))
    // تکمیل Rule یتیم — قدم ۱ از ۴ (ADR-106): validateShotDurationForCinematicMode
    // (domain.visualidentity، تا این قدم هیچ‌جا فراخوانی نمی‌شد) اکنون با حالت مؤثر
    // سه‌سطحی واقعی (resolveEffectiveCinematicMode: Override شات → Override صحنه →
    // پیش‌فرض DNA پروژه) در همین محل مرکزی Wire شد — نه یک مسیر فراخوانی جدا.
    val effectiveCinematicMode = resolveEffectiveCinematicMode(dna, scene, shot)
    add(l3, validateShotDurationForCinematicMode(shot.durationSeconds, effectiveCinematicMode))
    // تکمیل Rule یتیم — قدم ۳الف از ۲ زیرقدم قدم ۳ (ADR-110): تعارض حرکت سریع با
    // Long-take. در Level 3 (نه ۲) چون از همان مقدار effectiveCinematicMode
    // چندمنبعی (DNA + Scene + Shot، از طریق resolveEffectiveCinematicMode) تغذیه
    // می‌شود — دقیقاً هم‌الگو با علتِ قرارگیری validateShotDurationForCinematicMode
    // در همین سطح (ADR-106): مقایسه‌ی «داده‌ی خودِ Shot» با «مقداری مشتق‌شده از چند
    // منبع»، نه صرفاً ترکیب دو فیلد ساده‌ی همین Shot با هم (که Level 2 است).
    add(l3, checkFastMotionLongTake(shot.motionLevel, effectiveCinematicMode))
    val cameraForDna = shot.camera.overrideValue
    if (cameraForDna != null) {
        add(l3, validateShotAgainstDna(cameraForDna.angle.name.lowercase(), "camera", dna))
        add(l3, validateShotAgainstDna(cameraForDna.distance.name.lowercase(), "camera", dna))
        add(l3, validateShotAgainstDna(cameraForDna.lensType.name.lowercase(), "camera", dna))
    }
    shot.lighting.overrideValue?.lightingMotivation?.let { motivation ->
        add(l3, validateShotAgainstDna(motivation.name.lowercase(), "lighting", dna))
    }
    shot.environment.overrideValue?.let { environment ->
        add(l3, validateShotAgainstDna(environment.weatherType.name.lowercase(), "weather", dna))
    }

    characterAssets.forEach { character ->
        add(l3, validateDefaultOutfitExists(character.outfits))
        add(l3, validateBasePrompt(character.basePrompt))
    }
    objectAssets.forEach { obj ->
        addAll(l3, validateObjectAsset(obj))
        add(l3, validateBasePrompt(obj.basePrompt))
    }
    locationAssets.forEach { location ->
        add(l3, validateBasePrompt(location.basePrompt))
    }

    return AggregatedValidationReport(issues)
}

/**
 * تکمیل Rule یتیم — قدم ۳الف از ۲ زیرقدم قدم ۳ (ADR-110): نگاشت
 * `domain.camera.CameraMovement` (sealed class واقعی) به همان قرارداد
 * `String` که `checkStaticCameraInChase` می‌پذیرد (طبق کامنت مستندشده‌ی خودِ
 * آن تابع، ADR-010 — بدون تناظر تمیز enum ای برای این پارامتر، عمداً String
 * ماند). فقط الزام واقعی این Rule: `CameraMovement.Basic(type =
 * BasicMovementType.STATIC)` باید دقیقاً `"static"` تولید کند تا Rule فعال
 * شود؛ برای بقیه‌ی انواع (که ذاتاً ثابت نیستند)، هر رشته‌ی غیر-"static"
 * معنادار کافی است — اینجا نام هر نوع Basic (لغزیده به حروف کوچک) یا نام
 * کلاس (برای انواع پیشرفته) استفاده شد، صرفاً برای خوانایی/دیباگ‌پذیری.
 */
private fun CameraMovement.toMovementTypeString(): String = when (this) {
    is CameraMovement.Basic -> type.name.lowercase()
    is CameraMovement.Orbit -> "orbit"
    is CameraMovement.DronePath -> "drone_path"
    is CameraMovement.DollyZoom -> "dolly_zoom"
    is CameraMovement.HandheldShake -> "handheld_shake"
    is CameraMovement.Compound -> "compound"
}
