package com.operaboys.cinemashotgenerator.domain.promptengine

import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.audio.AudioContext
import com.operaboys.cinemashotgenerator.domain.camera.CameraSettings
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ContrastRatio
import com.operaboys.cinemashotgenerator.domain.sceneconditions.KeyLightPosition
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingStyle
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import com.operaboys.cinemashotgenerator.domain.shot.ImageReference
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۱۱ — Prompt Engineering Core (ساختار داده — فاز ۱ و خروجی)
// منبع حقیقت: docs/blueprints/11-prompt-engineering-core.md
//
// همه‌ی انواع این فایل از پکیج‌های واقعی واحدهای از قبل پیاده‌شده import شده‌اند —
// جزئیات نگاشت‌های غیربدیهی (objects/locations، LightingSettings/EnvironmentSettings)
// در docs/adr/012-unit11-prompt-engineering-core-deviations.md مستند شده‌اند.

/**
 * تجمیع محلیِ فقط همان فیلدهایی از Lighting که کد مفهومی بلوپرینت واقعاً می‌خواند
 * (style، keyLightPosition، contrastRatio). هیچ نوع «LightingSettings» تجمیعی در
 * واحد ۰۸ وجود ندارد (فقط enum های مجزا)؛ این نوع فقط ترکیبی از همان enum های واقعی
 * import‌شده است، نه یک Placeholder با فیلد حدسی.
 */
data class LightingSettings(
    val style: LightingStyle,
    val keyLightPosition: KeyLightPosition,
    val contrastRatio: ContrastRatio
)

/** مشابه LightingSettings — فقط weatherType که کد مفهومی بلوپرینت واقعاً می‌خواند. */
data class EnvironmentSettings(
    val weatherType: WeatherType
)

/**
 * ورودی جمع‌آوری‌شده برای تولید یک PromptBlueprint.
 *
 * - objects/locations هر دو از نوع LocationAsset اند (نه یک نوع «Asset» عمومی که در
 *   پروژه وجود ندارد) — طبق تصمیم ADR-003 (واحد ۰۶)، LocationAsset از قبل هر دو
 *   AssetType.LOCATION و AssetType.OBJECT را پوشش می‌دهد؛ تمایز فقط با فیلد assetType
 *   نمونه‌ها مشخص می‌شود، نه با دو نوع Kotlin جدا.
 * - camera: CameraSettings واقعی (واحد ۰۹) است؛ توجه: Shot.camera فعلاً از نوع
 *   SourcedSettings (Placeholder ساده‌ی واحد ۰۵) است، نه CameraSettings — تبدیل واقعی
 *   بین این دو، وظیفه‌ی لایه‌ای است که collectData را در آینده پیاده می‌کند (خارج از
 *   Scope این قدم؛ همین‌طور برای lighting/environment).
 */
data class PromptGenerationInput(
    val dna: ProjectDna,
    val scene: Scene,
    val shot: Shot,
    val characters: List<CharacterAsset>,
    val objects: List<LocationAsset>,
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

data class PromptBlueprint(
    val promptBlueprintId: String,
    val shotId: String,
    val structuredParts: StructuredParts,
    val imageReferences: List<ImageReference>, // فقط local_file_path، بدون فرمت‌دهی
    val weightedEmphasis: Map<String, Float>,
    val seed: Int?,
    val conflictsResolved: Int,
    val warnings: List<ValidationIssue>
)
