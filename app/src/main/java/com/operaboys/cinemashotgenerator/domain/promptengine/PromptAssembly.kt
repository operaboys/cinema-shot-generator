package com.operaboys.cinemashotgenerator.domain.promptengine

import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import com.operaboys.cinemashotgenerator.domain.shot.resolveNegativePrompt
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.domain.visualidentity.combineStyles
import com.operaboys.cinemashotgenerator.domain.visualidentity.toStyleReference
import java.util.UUID

// واحد ۱۱ — تابع اصلی مونتاژ PromptBlueprint (قلب معماری کل سیستم)
// منبع حقیقت: docs/blueprints/11-prompt-engineering-core-v2.md
//
// انحراف تأییدشده از کد مفهومی بلوپرینت: پارامتر چهارم (validationIssues) اضافه شد
// تا conflictsResolved/warnings از خروجی واقعی واحد ۰۷ پر شوند، نه هاردکد 0/emptyList
// (بلوپرینت این دو را TODO آینده گذاشته بود؛ چون واحد ۰۷ از قبل کامل پیاده شده، پر
// کردن واقعی همین‌جا ممکن و منطقی بود). جزئیات در ADR-012.
//
// MIGRATED (docs/adr/030-unit14-reference-image-and-negative-prompt-migration.md):
// resolveNegativePrompt (واحد ۰۵، ADR-028) اینجا فراخوانی می‌شود — این تابع همان‌جا
// نوشته شده بود اما تا این قدم به هیچ فیلد واقعی وصل نشده بود. این‌جا محل منطقی
// فراخوانی است چون input.shot و input.dna.qualityDirectives هر دو از قبل در دسترس‌اند
// و assemblePromptBlueprint همان لایه‌ای است که بقیه‌ی مقادیر Resolve‌شده (seed،
// conflictsResolved) را هم می‌سازد.

/** تابع اصلی مونتاژ PromptBlueprint از داده‌ی جمع‌آوری‌شده. */
fun assemblePromptBlueprint(
    input: PromptGenerationInput,
    useSeed: Boolean,
    weightedTags: Map<String, Float>?,
    validationIssues: List<ValidationIssue>,
    idProvider: () -> String = ::defaultPromptBlueprintId
): PromptBlueprint {
    val timelineBeats = input.shot.beats.takeIf { it.isNotEmpty() }
        ?.joinToString(", ") { "[${it.timestampSeconds}s] ${it.description}" }

    val audioDescription = input.audioContext?.let { audio ->
        val parts = mutableListOf<String>()
        if (audio.ambientSounds.isNotEmpty()) parts += "ambient: " + audio.ambientSounds.joinToString(", ") { "${it.type} (${it.intensity})" }
        if (audio.actionSounds.isNotEmpty()) parts += "action sounds: " + audio.actionSounds.joinToString(", ") { it.type }
        if (audio.characterSounds.isNotEmpty()) parts += "character sounds: " + audio.characterSounds.joinToString(", ") { it.description }
        parts.takeIf { it.isNotEmpty() }?.joinToString("; ")
    }

    // sceneWeather: پارامتر خارجی تأییدشده برای enforceCharacterContinuity، از
    // input.environment.weatherType مشتق می‌شود (چون Scene خودش weather ندارد؛ ADR-012).
    val sceneWeather = input.environment.weatherType.name.lowercase()

    // اتصال کامل Style Matrix — قدم ۴-اتصال از ۸ زیرقدم (آخرین زیرقدم، ADR-117):
    // تا این قدم styleModifiers فقط نام خام enum سبک اصلی بود (مثلاً
    // "CINEMATIC_STYLE style") و secondaryStyle/influence (ADR-113) هیچ اثری
    // روی پرامپت نهایی نداشتند. اکنون همان زنجیره‌ی واقعی دامنه (توابع
    // دست‌نخورده‌ی موجود، از قدم اول این پلن) فراخوانی می‌شود:
    // toStyleReference() (ADR-116، promptTokens غنی) + combineStyles (اگر
    // secondaryStyle/influence هر دو ست باشند، سبک ثانویه با Modifier درست
    // اضافه می‌شود؛ در غیر این صورت فقط سبک اصلی، دقیقاً طبق منطق موجود خودِ
    // combineStyles).
    val primaryStyleRef = input.dna.coreIdentity.dominantVisualStyle.toStyleReference()
    val secondaryStyleRef = input.dna.coreIdentity.secondaryStyle?.toStyleReference()
    val combinedStyle = combineStyles(primaryStyleRef, secondaryStyleRef, input.dna.coreIdentity.influence)

    val structuredParts = StructuredParts(
        subjectDescription = enforceCharacterContinuity(input.characters, input.scene, sceneWeather).joinToString(", "),
        sceneContext = "${input.scene.atmospherePrimary} atmosphere, ${input.scene.timeOfDay} time",
        shotDescription = input.shot.shotDescription,
        cameraSpecs = "${input.camera.angle} angle, ${input.camera.distance} shot, ${input.camera.lensType} lens",
        lightingSpecs = "${input.lighting.style} lighting, ${input.lighting.keyLightPosition} key light, ${input.lighting.contrastRatio} contrast",
        environmentSpecs = if (input.environment.weatherType != WeatherType.CLEAR) "${input.environment.weatherType} weather" else null,
        styleModifiers = "$combinedStyle, ${input.dna.masterPalette.colorGradingPreset}",
        timelineBeats = timelineBeats,
        audioDescription = audioDescription
    )

    val conflictResolution = summarizeConflictResolution(validationIssues)

    return PromptBlueprint(
        promptBlueprintId = idProvider(),
        shotId = input.shot.shotId,
        structuredParts = structuredParts,
        imageReferences = input.shot.imageReferences,
        weightedEmphasis = collectWeightedEmphasis(weightedTags),
        seed = manageSeed(input.shot.shotId, useSeed, null),
        conflictsResolved = conflictResolution.conflictsResolved,
        warnings = conflictResolution.warnings,
        negativePrompt = resolveNegativePrompt(input.shot, input.dna.qualityDirectives.negativePrompt)
    )
}

/** شناسه طبق قرارداد type_identifier در naming-conventions.md، مثل prompt_a1b2c3d4e5f6 */
private fun defaultPromptBlueprintId(): String =
    "prompt_" + UUID.randomUUID().toString().replace("-", "").take(12)
