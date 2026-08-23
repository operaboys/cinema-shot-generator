package com.operaboys.cinemashotgenerator.domain.asset

// واحد ۰۶ — Asset & Continuity (ساختار داده)
// منبع حقیقت: docs/blueprints/06-asset-and-continuity-v2.md (نسخه ۵)
//
// MIGRATION (docs/adr/029-unit06-continuity-tiers-migration-part1.md، هر دو بخش کامل
// شدند): AssetValidation.kt، مصرف‌کنندگان (AssetRepository.kt، PromptEngineModels.kt) و
// تست‌ها هم در بخش دوم به‌روزرسانی شدند — پروژه کامل کامپایل می‌شود و تست می‌گذراند.
//
// MIGRATION تکمیلی (docs/adr/031-unit06-physical-appearance-gender-migration.md):
// PhysicalAppearance/Gender (تغییرات v4/v5 بلوپرینت که در ADR-029 عمداً خارج از Scope
// مانده بودند) در این قدم Migrate شدند — gender:String→Gender enum، nullable شدن
// height/build/hair/facialFeatures، افزودن physicalFeatures/toPromptString.

/**
 * سه نوع Asset طبق بلوپرینت. AssetType همچنان سه مقدار دارد و در لایه‌ی Room (واحد ۱۵)
 * برای تفکیک ردیف‌ها لازم است؛ اما دیگر هیچ فیلد assetType روی خودِ LocationAsset/
 * ObjectAsset دامنه نیست (طبق Option A پایین‌تر) — نوع Kotlin خودش تفکیک‌کننده است.
 */
enum class AssetType { CHARACTER, LOCATION, OBJECT }

/** طبق فرم واقعی «Add New Asset» — سه مقدار دقیق، نه رشته‌ی آزاد. */
enum class Gender { FEMALE, MALE, OTHER }

data class Hair(
    val color: String,
    val style: String,
    val length: String
)

data class FacialFeatures(
    val eyes: String,
    val distinctiveMarks: List<String> = emptyList()
)

/**
 * MIGRATED (docs/adr/031-unit06-physical-appearance-gender-migration.md): gender از
 * String آزاد به Gender enum؛ height/build/hair/facialFeatures از غیر-nullable به
 * nullable (چون طبق فرم واقعی همه‌شان اختیاری‌اند)؛ physicalFeatures جدید (توصیف آزاد
 * تکمیلی، مستقل از hair/facialFeatures ساختاریافته). toPromptString() عیناً طبق کد
 * مفهومی بلوپرینت کپی شد.
 */
data class PhysicalAppearance(
    val ageRange: String,
    val gender: Gender,
    val height: String? = null,
    val build: String? = null,
    val hair: Hair? = null,
    val physicalFeatures: String? = null,
    val facialFeatures: FacialFeatures? = null
) {
    /**
     * تبدیل به یک جمله‌ی خوانا برای پرامپت — بدون این تابع، enforceCharacterContinuity
     * (واحد ۱۱) مجبور بود توصیف را دستی از فیلدهای خام بسازد.
     */
    fun toPromptString(): String {
        val parts = mutableListOf<String>()
        parts += "$ageRange ${gender.name.lowercase()}"
        height?.let { parts += it }
        build?.let { parts += "$it build" }
        hair?.let { parts += "${it.length} ${it.color} hair, ${it.style} style" }
        facialFeatures?.let { ff ->
            parts += "${ff.eyes} eyes"
            if (ff.distinctiveMarks.isNotEmpty()) parts += ff.distinctiveMarks.joinToString(", ")
        }
        physicalFeatures?.let { parts += it }
        return parts.joinToString(", ")
    }
}

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
    val condition: OutfitCondition? = null,
    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۱ از ۵ (ADR-131): کاملاً مجزا از
    // موتور اصلی پرامپت ویدیو (PromptAssembly.kt/Renderer.kt) — هرگز با basePrompt
    // بالا قاطی نمی‌شود. پرامپت عکس مخصوص همین Outfit (نه شخصیت پایه). هم‌الگو دقیق
    // با descriptionFaPreview موجود (ADR-122): Nullable، پیش‌فرض null.
    val imagePromptQuick: String? = null,
    val imagePromptAi: String? = null,
    val imagePromptFaPreview: String? = null,
    val imagePromptGeneratedAt: Long? = null
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
    val referenceImages: List<ReferenceImage> = emptyList(),
    // سیستم Preview دوزبانه‌ی پرامپت — قدم ۲ از ۳ زیرقدم (ADR-122): نسخه‌ی
    // فارسی basePrompt، فقط برای مرور کاربر فارسی‌زبان — هرگز به پرامپت
    // نهایی (PromptAssembly.kt/Renderer.kt که فقط basePrompt انگلیسی
    // بالا را می‌خوانند) راه پیدا نمی‌کند. null یعنی Preview فارسی برای
    // این Asset تولید/ذخیره نشده.
    val descriptionFaPreview: String? = null,
    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۱ از ۵ (ADR-131): کاملاً مجزا از
    // موتور اصلی پرامپت ویدیو — هرگز با basePrompt بالا قاطی نمی‌شود. این چهار
    // فیلد پرامپت عکس شخصیت پایه/خنثی است (بدون لباس داستانی)، مستقل از پرامپت
    // عکس هر Outfit (بالاتر، Outfit.imagePromptQuick/...). هم‌الگو دقیق با
    // descriptionFaPreview بالا.
    val imagePromptQuick: String? = null,
    val imagePromptAi: String? = null,
    val imagePromptFaPreview: String? = null,
    val imagePromptGeneratedAt: Long? = null,
    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۱ از ۵ (ADR-131): زمان آخرین
    // ویرایش این Asset — قدم‌های بعدی (۲ تا ۵) از این Timestamp برای تشخیص
    // «پرامپت عکس قدیمی شده یا نه» استفاده می‌کنند (مقایسه با زمان تولید پرامپت).
    val updatedAt: Long? = null
)

data class Environment(
    val type: String,
    val size: String,
    val lightingCondition: String
)

/**
 * واحد ۱۶ فاز ۳ — قدم ۱: نوع مستقل «دسته‌ی مکان» برای زیرفیلتر صفحه‌ی Asset
 * Library — طبق docs/design/README.md بخش «۸. Assets Library» («Sub-filter row
 * ... INDOOR/OUTDOOR/MIXED for locations») و تکرار همان مقادیر در دستور کار این
 * قدم. عمداً یک enum مستقل و تازه است، نه بازاستفاده‌ی مستقیم
 * `domain.scene.LocationType` (که با همین ۴ مقدار از قبل وجود دارد، ولی مفهوم
 * کاملاً متفاوتی را نمایندگی می‌کند — دسته‌بندی یک Scene، نه یک LocationAsset
 * دائمی در کتابخانه) — هم‌راستا با اصل صریح مستندشده در AssetContinuity.kt
 * («هرگز یک enum مشترک» برای مفاهیم Character/Location/Prop که فقط شباهت اسمی
 * دارند؛ همین اصل اینجا هم برای Asset در برابر Scene اعمال شد). جزئیات کامل در
 * docs/adr/048-unit16-phase3-step1-asset-library.md.
 */
enum class LocationType { INDOOR, OUTDOOR, MIXED, CUSTOM }

/**
 * MIGRATED (Option A — طبق تصمیم مستند در ADR-029): تا این قدم LocationAsset هر دو
 * AssetType.LOCATION و AssetType.OBJECT را پوشش می‌داد (یک assetType فیلد تفکیک‌کننده
 * داشت). از این قدم به بعد، LocationAsset فقط مکان است — ObjectAsset (پایین‌تر)
 * ساختار کاملاً مستقل خودش را دارد؛ فیلد assetType دیگر لازم نیست چون نوع Kotlin
 * خودش تفکیک‌کننده است (دقیقاً هم‌شکل با data class LocationAsset بلوپرینت).
 *
 * MIGRATED (واحد ۱۶ فاز ۳ قدم ۱): `locationType` اضافه شد — فیلد بدون پیش‌فرض قبلاً
 * روی این نوع وجود نداشت (نه در بلوپرینت ۰۶، نه در type-registry.md)؛ افزودنش با
 * مقدار پیش‌فرض `CUSTOM` (محافظه‌کارانه‌ترین گزینه — بدون فرض غلط داخلی/بیرونی)
 * Backward Compatible است و هیچ‌کدام از ۴ محل ساخت واقعی موجود این نوع را نمی‌شکند.
 */
data class LocationAsset(
    val assetId: String,
    val name: String,
    val description: String,
    val environment: Environment,
    val locationType: LocationType = LocationType.CUSTOM,
    val timeCompatibility: List<String> = emptyList(),
    val weatherCompatibility: List<String> = emptyList(),
    val keyElements: List<String> = emptyList(),
    val basePrompt: String? = null,
    val continuityLockLevel: LocationContinuityLevel = LocationContinuityLevel.STYLE,
    // فیچر مستقل جدید «آپلود عکس مرجع واقعی Asset» — زیرقدم ۱ از ۳ (ADR-137):
    // کاملاً مجزا از فیچر «پرامپت ساخت عکس مرجع» (ADR-131 تا ۱۳۶، که فقط متن
    // پرامپت می‌سازد، نه خودِ عکس). هم‌نوع مستقیم با CharacterAsset.referenceImages
    // موجود (از قبل تعریف‌شده، اینجا فقط برای Location/Object هم اضافه شد).
    val referenceImages: List<ReferenceImage> = emptyList(),
    // سیستم Preview دوزبانه‌ی پرامپت — قدم ۲ از ۳ زیرقدم (ADR-122): هم‌الگو
    // دقیق با CharacterAsset.descriptionFaPreview بالا.
    val descriptionFaPreview: String? = null,
    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۱ از ۵ (ADR-131): هم‌الگو دقیق با
    // CharacterAsset بالا؛ LocationAsset ساختار Outfit ندارد، پس این چهار فیلد
    // فقط یک‌بار روی خودِ Asset لازم است.
    val imagePromptQuick: String? = null,
    val imagePromptAi: String? = null,
    val imagePromptFaPreview: String? = null,
    val imagePromptGeneratedAt: Long? = null,
    val updatedAt: Long? = null
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
    val continuityLockLevel: PropContinuityLevel = PropContinuityLevel.FORM,
    // فیچر مستقل جدید «آپلود عکس مرجع واقعی Asset» — زیرقدم ۱ از ۳ (ADR-137):
    // هم‌الگو دقیق با LocationAsset بالا.
    val referenceImages: List<ReferenceImage> = emptyList(),
    // سیستم Preview دوزبانه‌ی پرامپت — قدم ۲ از ۳ زیرقدم (ADR-122): هم‌الگو
    // دقیق با CharacterAsset.descriptionFaPreview بالا.
    val descriptionFaPreview: String? = null,
    // فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۱ از ۵ (ADR-131): هم‌الگو دقیق با
    // CharacterAsset بالا؛ ObjectAsset ساختار Outfit ندارد، پس این چهار فیلد
    // فقط یک‌بار روی خودِ Asset لازم است.
    val imagePromptQuick: String? = null,
    val imagePromptAi: String? = null,
    val imagePromptFaPreview: String? = null,
    val imagePromptGeneratedAt: Long? = null,
    val updatedAt: Long? = null
)
