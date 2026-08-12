package com.operaboys.cinemashotgenerator.domain.shot

import com.operaboys.cinemashotgenerator.domain.camera.CameraSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingSettings

// واحد ۰۵ — Shot Engine (ساختار داده)
// منبع حقیقت: docs/blueprints/05-shot-engine-v2.md (نسخه ۳)
//
// این واحد صاحب اصلی MotionLevel است. واحد ۰۷ (Logic Conflict Checker) قبلاً
// پارامتر motionLevel را به‌صورت String موقت گرفته بود دقیقاً چون این enum هنوز
// جایی تعریف نشده بود (docs/adr/004-...) — حالا اینجا صاحب واقعی‌اش تعریف می‌شود.
// آیا واحد ۰۷ باید بعداً به این enum واقعی Migrate شود، سؤالی است که در
// docs/adr/006-unit05-shot-engine-deviations.md مطرح شده، نه اجراشده.
//
// MIGRATED (docs/adr/028-unit05-negative-prompt-override-migration.md):
// negativePromptOverride اضافه شد — پیش‌فرض null یعنی از
// domain.dna.QualityDirectives.negativePrompt (واحد ۰۲) ارث می‌برد؛ منبع ارث‌بری
// عمداً DNA پروژه است، نه Scene (برخلاف camera/lighting/environment).

enum class ShotGoal { ESTABLISHING, ACTION, EMOTIONAL, DIALOGUE, TRANSITION }
enum class ShotType { EXTREME_WIDE, WIDE, MEDIUM, CLOSE_UP, EXTREME_CLOSE_UP }
enum class MotionLevel { STATIC, SUBTLE, MODERATE, DYNAMIC, EXTREME }
enum class BeatEventType { CAMERA_MOVE, SUBJECT_ACTION, ENVIRONMENTAL, LIGHTING_CHANGE }

data class Beat(
    val timestampSeconds: Float,
    val eventType: BeatEventType,
    val description: String,
    val subjectId: String? = null
)

data class ImageReference(
    val type: String,           // "character" | "style" | "composition" | "lighting"
    val localFilePath: String,  // فقط فایل محلی — بدون URL خارجی
    val description: String
)

/**
 * MIGRATED (Migration نسخه ۳، docs/adr/074-...): فیلد `source` اضافه شد تا با
 * AmbientSound واحد ۱۰ (domain.audio) هماهنگ باشد — طبق بلوپرینت ۰۵ (نسخه ۳) و
 * بلوپرینت ۱۰ (نسخه ۲)، هر دو باید ساختار یکسان (type/intensity/description/source)
 * داشته باشند؛ فقط پیش‌فرض هرکدام متفاوت است (اینجا "auto_generated"، چون تنها راه
 * ساخت یک نمونه در این واحد از طریق generateAmbientSounds خودکار است). پیش از این
 * Migration، این اطلاعات («خودکار تولید شده یا کاربر دستی اضافه/ویرایش کرده») در
 * سطح تک‌تک AmbientSound گم می‌شد، با این‌که SoundProfile.ambientAutoGenerate همین
 * تمایز را در سطح بالاتر داشت.
 */
data class AmbientSound(val type: String, val intensity: String, val description: String, val source: String = "auto_generated")
data class ActionSound(val timestampSeconds: Float, val type: String, val description: String)

/**
 * صدای کاراکتر همیشه source="user_defined" است؛ هرگز خودکار تولید نمی‌شود (Rule 5).
 * برخلاف AmbientSound (که SoundProfile.ambientAutoGenerate برایش وجود دارد)، عمداً
 * هیچ پرچم یا تابع auto-generate ای برای CharacterSound در این فایل وجود ندارد —
 * تنها راه ساخت یک نمونه، فراخوانی مستقیم و صریح این constructor توسط کاربر (لایه‌ی
 * بالاتر/UI) است. جزئیات تضمین در docs/adr/006-unit05-shot-engine-deviations.md.
 */
data class CharacterSound(val characterId: String, val type: String, val description: String)

data class SoundProfile(
    val enabled: Boolean,
    val ambientAutoGenerate: Boolean = true,
    val ambientSounds: List<AmbientSound> = emptyList(),
    val actionSounds: List<ActionSound> = emptyList(),
    val characterSounds: List<CharacterSound> = emptyList() // همیشه دستی
)

/**
 * نمایندگی فیلدهای camera/lighting/environment طبق بخش «ارث‌بری و Override» بلوپرینت:
 * source مشخص می‌کند تنظیمات از Scene ارث رسیده ("scene") یا در همین Shot Override
 * شده ("override"). قبلاً `settings: Map<String, String>` بود (docs/adr/006-...)، چون
 * واحدهای ۰۸/۰۹ هنوز پیاده نشده بودند و آن Map هیچ schema/Parser واقعی به enum های
 * آن واحدها نداشت. حالا که هر دو واحد واقعی‌اند، generic شد تا `overrideValue` مقدار
 * Type-safe واقعی (نه رشته‌ی بدون‌ساختار) نگه دارد — docs/adr/013-....
 */
data class SourcedSettings<T>(
    val source: String = "scene",
    val overrideValue: T? = null
)

/**
 * Shot کوچک‌ترین واحد اجرایی پروژه. علاوه بر فیلدهای کد مفهومی بلوپرینت،
 * shotTitle، camera/lighting/environment، و overrideScene هم اضافه شدند —
 * این‌ها فقط در بخش «ساختار داده» (JSON) و بخش «ارث‌بری و Override» بودند،
 * نه در کد مفهومی، اما برای نمایندگی کامل رفتار توصیف‌شده لازم بودند.
 */
data class Shot(
    val shotId: String,
    val sceneId: String,
    val shotNumber: Int,
    val shotTitle: String? = null,
    val shotDescription: String, // الزامی
    val shotGoal: ShotGoal,
    val shotType: ShotType,
    val durationSeconds: Float,
    val motionLevel: MotionLevel,
    val beats: List<Beat> = emptyList(),
    val imageReferences: List<ImageReference> = emptyList(),
    val camera: SourcedSettings<CameraSettings> = SourcedSettings(),
    val lighting: SourcedSettings<LightingSettings> = SourcedSettings(),
    val environment: SourcedSettings<EnvironmentSettings> = SourcedSettings(),
    val soundProfile: SoundProfile,
    val negativePromptOverride: String? = null,   // null یعنی از DNA پروژه ارث می‌برد (resolveNegativePrompt)
    val characterIds: List<String> = emptyList(),
    val objectIds: List<String> = emptyList(),
    val locationIds: List<String> = emptyList(),
    val overrideScene: Boolean = false
)
