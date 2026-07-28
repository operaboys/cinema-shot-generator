package com.operaboys.cinemashotgenerator.domain.asset

// واحد ۰۶ — Asset & Continuity (ساختار داده)
// منبع حقیقت: docs/blueprints/06-asset-and-continuity-v2.md (نسخه ۵)
//
// MIGRATION بخش اول (docs/adr/029-unit06-continuity-tiers-migration-part1.md):
// این قدم فقط AssetModels.kt و AssetContinuity.kt را در بر می‌گیرد — AssetValidation.kt،
// تست‌ها، و مصرف‌کنندگان (data/repository/AssetRepository.kt، domain/promptengine/)
// عمداً در این قدم دست‌نخورده ماندند (طبق دستور کار)؛ این دو فایل تا تکمیل بخش دوم
// کامپایل نخواهند شد — این وضعیت مورد انتظار است، نه خطا.
//
// PhysicalAppearance/Gender (تغییرات v4/v5 بلوپرینت: gender:String→Gender enum،
// nullable شدن height/build/hair/facialFeatures، افزودن physicalFeatures/toPromptString)
// عمداً در این قدم دست‌نخورده ماندند — دستور کار این قدم صریحاً هفت مورد مشخص را برای
// AssetModels.kt فهرست کرده بود که PhysicalAppearance/Gender جزوشان نبود؛ جزئیات کامل
// در ADR-029.

/**
 * سه نوع Asset طبق بلوپرینت. AssetType همچنان سه مقدار دارد و در لایه‌ی Room (واحد ۱۵)
 * برای تفکیک ردیف‌ها لازم است؛ اما دیگر هیچ فیلد assetType روی خودِ LocationAsset/
 * ObjectAsset دامنه نیست (طبق Option A پایین‌تر) — نوع Kotlin خودش تفکیک‌کننده است.
 */
enum class AssetType { CHARACTER, LOCATION, OBJECT }

data class Hair(
    val color: String,
    val style: String,
    val length: String
)

data class FacialFeatures(
    val eyes: String,
    val distinctiveMarks: List<String> = emptyList()
)

data class PhysicalAppearance(
    val ageRange: String,
    val gender: String,
    val height: String,
    val build: String,
    val hair: Hair,
    val facialFeatures: FacialFeatures
)

/** شرط اختیاری برای انتخاب خودکار — هم برای Outfit و هم برای Expression استفاده می‌شود. */
data class OutfitCondition(
    val weather: String? = null,
    val timeOfDay: String? = null,
    val locationType: String? = null
)

data class Outfit(
    val id: String,
    val name: String,
    val description: String,
    val isDefault: Boolean,
    val condition: OutfitCondition? = null
)

/**
 * condition و isDefault در نمونه‌ی JSON بلوپرینت برای Expression نیامده بودند؛
 * افزودن این دو فیلد تأییدشده است تا selectExpressionForScene (طبق جمله‌ی بلوپرینت
 * «همین منطق برای expressions هم قابل استفاده است») معنای واقعی داشته باشد.
 * (ثبت‌شده در docs/adr/003-unit06-asset-continuity-deviations.md)
 */
data class Expression(
    val id: String,
    val name: String,
    val description: String,
    val emotion: String,
    val isDefault: Boolean,
    val condition: OutfitCondition? = null
)

data class Prop(
    val id: String,
    val name: String,
    val description: String,
    val category: String
)

/**
 * Hard Lock — برخلاف Soft Lock واحد ۰۲ (DNA Manager)، این فیلدها هیچ مسیر Override
 * ای ندارند. جزئیات: docs/governance/override-policy.md (حذف عمدی «سطح Character»).
 * MIGRATED: از این قدم به بعد، شدت واقعی این Hard Lock دیگر یکسان برای همه‌ی
 * کاراکترها نیست — بلکه شرطی به CharacterContinuityLevel است (پایین‌تر، AssetContinuity.kt).
 */
data class ContinuityRules(
    val identityLock: Boolean = true,
    val appearanceLock: Boolean = true,
    val ageLock: Boolean = true,
    val antiDrift: Boolean = true,
    val allowedOverrides: List<String> = listOf("emotion", "pose", "outfit", "expression", "prop")
)

data class ReferenceImage(
    val localFilePath: String,
    val description: String
)

/**
 * ساختار داده‌ی خام Evolution Timeline — بدون منطق اعمال آن.
 * منطق واقعی (اعمال/ثبت تغییرات در طول زمان) به واحد ۱۲ (State & Versioning) وابسته
 * است و در این قدم پیاده نشده — خارج از Scope طبق دستور کار.
 */
data class EvolutionEntry(
    val fromShot: String,
    val toShot: String,
    val changes: Map<String, List<String>>,
    val reason: String
)

data class EvolutionTimeline(
    val characterId: String,
    val entries: List<EvolutionEntry> = emptyList()
)

/**
 * سه enum کاملاً مجزا — طبق تأکید صریح بلوپرینت («هرگز یک enum مشترک»؛ طبق
 * concept-ownership-map.md، تداوم کاراکتر/مکان/شیء سه مفهوم متفاوتند که فقط
 * شباهت اسمی «Lock» دارند). جداسازی در سطح Type System از ترکیب‌های بی‌معنی
 * (مثل اعمال سطح Lock مکان روی یک کاراکتر) در زمان کامپایل جلوگیری می‌کند.
 */
enum class CharacterTier { MAIN, SECONDARY, BACKGROUND }

enum class CharacterContinuityLevel { FULL, MEDIUM, NONE }
enum class LocationContinuityLevel { STYLE }
enum class PropContinuityLevel { FORM }

/** پیش‌فرض سطح Lock بر اساس Tier — کاربر همیشه می‌تواند override کند. */
fun defaultLockLevelForTier(tier: CharacterTier): CharacterContinuityLevel = when (tier) {
    CharacterTier.MAIN -> CharacterContinuityLevel.FULL
    CharacterTier.SECONDARY -> CharacterContinuityLevel.MEDIUM
    CharacterTier.BACKGROUND -> CharacterContinuityLevel.NONE
}

/**
 * physicalAppearance غیر-nullable است — Rule 2 (Character باید physical_appearance
 * داشته باشد) در سطح نوع تضمین می‌شود.
 *
 * MIGRATED: characterTier بدون پیش‌فرض (الزامی) اضافه شد — کد مفهومی بلوپرینت
 * (`val characterTier: CharacterTier,`) خودش پیش‌فرض ندارد؛ بلوپرینت در بخش یادداشت
 * پیاده‌سازی («character_tier باید Backward Compatible باشد ... پیشنهاد: MAIN») یک
 * پیش‌فرض احتمالی را مطرح می‌کند اما تصمیم نهایی را صریحاً به من واگذار کرده. با grep
 * تأیید شد هر ۶ محل واقعی ساخت CharacterAsset در کل پروژه فقط Named Argument دارند، پس
 * الزامی‌کردن این فیلد هیچ خطر Silent Breakage ای ندارد — فقط خطای کامپایل بلند برای هر
 * فراخوانی که باید صریحاً Tier را مشخص کند. جزئیات کامل در ADR-029؛
 * defaultMood/basePrompt جدید با پیش‌فرض null (Backward Compatible)؛
 * continuityLockLevel جدید با پیش‌فرض defaultLockLevelForTier(characterTier)
 * — این فیلد در کد مفهومی بلوپرینت/type-registry.md روی CharacterAsset نیامده بود
 * (فقط روی LocationAsset/ObjectAsset)؛ افزودنش اینجا دستور صریح دستور کار بود، نه
 * تفسیر من — جزئیات کامل در ADR-029.
 */
data class CharacterAsset(
    val assetId: String,
    val assetType: AssetType = AssetType.CHARACTER,
    val characterTier: CharacterTier,
    val name: String,
    val physicalAppearance: PhysicalAppearance,
    val outfits: List<Outfit>,
    val expressions: List<Expression> = emptyList(),
    val props: List<Prop> = emptyList(),
    val defaultMood: String? = null,
    val basePrompt: String? = null,
    val continuityRules: ContinuityRules = ContinuityRules(),
    val continuityLockLevel: CharacterContinuityLevel = defaultLockLevelForTier(characterTier),
    val referenceImages: List<ReferenceImage> = emptyList()
)

data class Environment(
    val type: String,
    val size: String,
    val lightingCondition: String
)

/**
 * MIGRATED (Option A — طبق تصمیم مستند در ADR-029): تا این قدم LocationAsset هر دو
 * AssetType.LOCATION و AssetType.OBJECT را پوشش می‌داد (یک assetType فیلد تفکیک‌کننده
 * داشت). از این قدم به بعد، LocationAsset فقط مکان است — ObjectAsset (پایین‌تر)
 * ساختار کاملاً مستقل خودش را دارد؛ فیلد assetType دیگر لازم نیست چون نوع Kotlin
 * خودش تفکیک‌کننده است (دقیقاً هم‌شکل با data class LocationAsset بلوپرینت).
 */
data class LocationAsset(
    val assetId: String,
    val name: String,
    val description: String,
    val environment: Environment,
    val timeCompatibility: List<String> = emptyList(),
    val weatherCompatibility: List<String> = emptyList(),
    val keyElements: List<String> = emptyList(),
    val basePrompt: String? = null,
    val continuityLockLevel: LocationContinuityLevel = LocationContinuityLevel.STYLE
)

/** طبق فرم واقعی «Add New Asset» — سه زیرگروه Object. */
enum class ObjectSubtype { PERSONAL_PROP, GENERAL_PROP, COSTUME }

/**
 * MIGRATED (Option A، جدید در این قدم): ساختار مستقل و کامل برای Object/Prop Asset —
 * قبلاً با LocationAsset مشترک بود (طبق ADR-003 قدیمی). دلیل جداسازی در ADR-029؛
 * خلاصه: فیلدهای اختصاصی (size/materialAndColor/specialTrait) با Location
 * (environment/timeCompatibility/weatherCompatibility) هیچ همپوشانی معنایی ندارند.
 */
data class ObjectAsset(
    val assetId: String,
    val name: String,
    val description: String,
    val subtype: ObjectSubtype,
    val size: String,
    val materialAndColor: String,
    val specialTrait: String? = null,
    val basePrompt: String? = null,
    val continuityLockLevel: PropContinuityLevel = PropContinuityLevel.FORM
)
