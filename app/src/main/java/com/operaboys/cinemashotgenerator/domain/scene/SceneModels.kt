package com.operaboys.cinemashotgenerator.domain.scene

import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState

// واحد ۰۴ — Scene Engine (ساختار داده)
// منبع حقیقت: docs/blueprints/04-scene-engine.md

enum class NarrativeRole { INTRODUCTION, DEVELOPMENT, CLIMAX, RESOLUTION, TRANSITION }
enum class LocationType { INDOOR, OUTDOOR, MIXED, CUSTOM }
enum class TimeOfDay { DAWN, MORNING, NOON, AFTERNOON, SUNSET, NIGHT }
enum class Atmosphere { CALM, TENSE, DARK, BRIGHT, MYSTERIOUS, EMOTIONAL }

data class SceneLocation(val type: LocationType, val description: String)

data class SceneConstraints(
    val cameraRestrictions: List<String> = emptyList(),
    val lightingRestrictions: List<String> = emptyList(),
    val environmentRestrictions: List<String> = emptyList()
)

/**
 * نمایندگی ساده‌ی فیلد global_visual_style طبق ساختار JSON بلوپرینت (نه اتصال واقعی
 * به DNA Manager): source معمولاً "project_dna" است؛ override در صورت وجود، سبک
 * محلی صحنه را نشان می‌دهد. شکل این نوع (source + یک override تکی، نه یک Map
 * تنظیمات) با SourcedSettings واحد ۰۵ متفاوت است — چون ساختار JSON خودش متفاوت
 * است (`{"source": "project_dna", "override": null}` در برابر `{"source": ..., "settings": {...}}`).
 */
data class GlobalVisualStyleRef(
    val source: String = "project_dna",
    val override: String? = null
)

/**
 * MIGRATED (docs/adr/038-unit04-scene-location-asset-link.md، رفع یافته‌ی F2 ممیزی
 * pre-Unit 16): locationAssetId ارجاع اختیاری به LocationAsset.assetId در کتابخانه‌ی
 * دارایی‌ها است — در کنار SceneLocation موجود (نه جایگزین آن)، چون
 * SceneLocation.description همچنان برای توصیف متنی آزاد لازم است (خصوصاً خروجی خودکار
 * واحد ۰۱ب که فقط توصیف متنی تولید می‌کند، نه یک Asset واقعی). null یعنی این Scene
 * هنوز به کتابخانه وصل نشده.
 */
/**
 * MIGRATED (واحد ۱۶ فاز ۴ قدم ۱): `state` اضافه شد — طبق تصریح صریح
 * `docs/blueprints/16-user-workflow-v2.md` («هر Entity (Project/Scene/Shot/Asset) یک
 * وضعیت EntityState دارد»)، همان اصلی که در فاز ۱ برای `domain.project.Project`
 * اعمال شد (ADR-044). قبل از این قدم `Scene` هیچ فیلد وضعیتی نداشت — یک شکاف واقعی
 * بین بلوپرینت و کد موجود، نه فرض من. `EntityState` مستقیم بازاستفاده شد (نه enum
 * دوم/موازی)، دقیقاً هم‌الگو با `Project.state`؛ پیش‌فرض `DRAFT` (محافظه‌کارانه‌ترین
 * مقدار، سازگار با تمام محل‌های ساخت واقعی این نوع — با grep تأیید شد هر دو محل
 * ساخت واقعی main (`StoryToDomainMapper.kt`، `SceneMappers.kt`) و همه‌ی محل‌های
 * تست فقط Named Argument دارند).
 */
/**
 * MIGRATED (یافته‌ی #۱۱ appendix ADR-081): linkedAssetIds — ارجاع Character/
 * Location/Object Asset های متصل به این Scene (طبق mockup، `sc.assets`/
 * `linkedAssets`). برخلاف locationAssetId (تکی، فقط Location)، این فیلد
 * می‌تواند شامل هر سه نوع Asset باشد — طبق نمونه‌ی واقعی mockup
 * (`linkedAssets`) که یک کاراکتر + یک Location + یک Object را در یک فهرست
 * واحد نشان می‌دهد. پیش‌فرض لیست خالی — دقیقاً هم‌الگو با locationAssetId
 * (ADR-038): چون SceneEntity فقط یک sceneDataJson Blob است (بدون ستون‌های
 * مجزای SQL)، افزودن این فیلد نیازمند Migration رسمی Room نیست؛
 * kotlinx.serialization به‌طور پیش‌فرض هر کلید غایب در JSON قدیمی را با همین
 * مقدار پیش‌فرض (لیست خالی) پر می‌کند.
 */
data class Scene(
    val sceneId: String,
    val sceneTitle: String? = null,
    val sceneNumber: Int,
    val narrativeRole: NarrativeRole,
    val location: SceneLocation,
    val locationAssetId: String? = null,
    val linkedAssetIds: List<String> = emptyList(),
    val timeOfDay: TimeOfDay,
    val atmospherePrimary: Atmosphere,
    val atmosphereSecondary: Atmosphere? = null,
    val globalVisualStyle: GlobalVisualStyleRef = GlobalVisualStyleRef(),
    val constraints: SceneConstraints = SceneConstraints(),
    val shotCount: Int = 0,
    val state: EntityState = EntityState.DRAFT
)
