package com.operaboys.cinemashotgenerator.domain.promptengine

import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.audio.AudioContext
import com.operaboys.cinemashotgenerator.domain.camera.CameraSettings
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingSettings
import com.operaboys.cinemashotgenerator.domain.shot.ImageReference
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۱۱ — Prompt Engineering Core (ساختار داده — فاز ۱ و خروجی)
// منبع حقیقت: docs/blueprints/11-prompt-engineering-core.md
//
// همه‌ی انواع این فایل از پکیج‌های واقعی واحدهای از قبل پیاده‌شده import شده‌اند —
// جزئیات نگاشت‌های غیربدیهی (objects/locations) در
// docs/adr/012-unit11-prompt-engineering-core-deviations.md مستند شده‌اند.
// LightingSettings/EnvironmentSettings ابتدا اینجا تعریف شده بودند؛ به domain.sceneconditions
// منتقل شدند (docs/adr/013-...) چون واحد ۰۵ (Shot.lighting/.environment) هم به همین
// نوع واقعی نیاز پیدا کرد و domain.shot نباید به domain.promptengine (بالادست‌ترین واحد)
// وابسته شود.

/**
 * ورودی جمع‌آوری‌شده برای تولید یک PromptBlueprint.
 *
 * - MIGRATED (docs/adr/029-unit06-continuity-tiers-migration-part1.md، بخش دوم):
 *   objects اکنون از نوع ObjectAsset است (قبلاً LocationAsset، طبق تصمیم قدیمی
 *   ADR-003 که LocationAsset را برای هر دو AssetType.LOCATION و AssetType.OBJECT
 *   مشترک گرفته بود). با تفکیک Option A در واحد ۰۶، این دو مفهوم اکنون دو Kotlin
 *   type کاملاً مستقل‌اند؛ locations همچنان LocationAsset است، بدون تغییر.
 * - camera/lighting/environment: انواع واقعی (واحدهای ۰۹/۰۸) هستند. اتصال واقعی این‌ها
 *   از Shot.camera/.lighting/.environment (که پس از ADR-013 خودشان typed هستند) به‌عهده‌ی
 *   لایه‌ای است که collectData را در آینده پیاده می‌کند (خارج از Scope این قدم) — با
 *   resolveCameraSettings/resolveLightingSettings/resolveEnvironmentSettings (واحد ۰۵)
 *   که در ADR-013 اضافه شدند.
 */
data class PromptGenerationInput(
    val dna: ProjectDna,
    val scene: Scene,
    val shot: Shot,
    val characters: List<CharacterAsset>,
    val objects: List<ObjectAsset>,
    val locations: List<LocationAsset>,
    val camera: CameraSettings,
    val lighting: LightingSettings,
    val environment: EnvironmentSettings,
    val audioContext: AudioContext?
)

data class StructuredParts(
    val subjectDescription: String,
    val sceneContext: String,
    val shotDescription: String,
    val cameraSpecs: String,
    val lightingSpecs: String,
    val environmentSpecs: String?,
    val styleModifiers: String,
    val timelineBeats: String?,   // از Beat Sheet، اگر فعال باشد
    val audioDescription: String? // از Ambient/Action خودکار + Character دستی
)

/**
 * MIGRATED (docs/adr/030-unit14-reference-image-and-negative-prompt-migration.md):
 * negativePrompt تکمیل‌کننده‌ی ADR-028 (واحد ۰۵) است — آنجا resolveNegativePrompt
 * نوشته شد اما به هیچ فیلد واقعی در مسیر Blueprint→Render وصل نشده بود. اینجا اضافه
 * شد چون PromptBlueprint (نه PromptGenerationInput) خروجی «نهاییِ Resolve‌شده»ی
 * واحد ۱۱ است — دقیقاً همان الگویی که conflictsResolved/warnings/seed از قبل
 * دنبال می‌کنند (مقدار خام در PromptGenerationInput می‌ماند، مقدار Resolve‌شده در
 * PromptBlueprint می‌نشیند). پیش‌فرض "" (نه null) چون resolveNegativePrompt خودش
 * هم همیشه String غیر-nullable برمی‌گرداند (fallback به‌ dna.qualityDirectives.negativePrompt
 * که پیش‌فرضش "" است، نه null).
 */
data class PromptBlueprint(
    val promptBlueprintId: String,
    val shotId: String,
    val structuredParts: StructuredParts,
    val imageReferences: List<ImageReference>, // فقط local_file_path، بدون فرمت‌دهی
    val weightedEmphasis: Map<String, Float>,
    val seed: Int?,
    val conflictsResolved: Int,
    val warnings: List<ValidationIssue>,
    val negativePrompt: String = ""
)
