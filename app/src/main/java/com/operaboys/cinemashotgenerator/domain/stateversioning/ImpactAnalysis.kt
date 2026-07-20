package com.operaboys.cinemashotgenerator.domain.stateversioning

// واحد ۱۲ — State & Versioning (بخش د: Impact Analysis)
// منبع حقیقت: docs/blueprints/12-state-and-versioning.md
//
// NOTE: analyzeImpact واحد ۰۷ (domain.validation.DependencyResolver) ورودی متفاوتی
// دارد — List<DependencyEdge> (کل گراف وابستگی از پیش ساخته‌شده)، نه یک justify
// مستقیم برای findDependents این تابع (که فقط یک entityId می‌گیرد و مستقیماً
// List<String> برمی‌گرداند). پس مستقیماً به‌جای findDependents قابل‌جایگزینی نیست؛
// اتصال کامل بین این دو (مثلاً ساخت edges از داده‌ی واقعی و فراخوانی analyzeImpact
// داخل findDependents) به تصمیم معمار موکول شد — خارج از Scope این قدم.

data class ImpactResult(
    val affectedEntityIds: List<String>,
    val dependencyChanges: Boolean,
    val breakingChanges: Boolean,
    val riskLevel: String // "low" | "high" | "critical" — دقیقاً طبق کد مفهومی بلوپرینت (رشته‌ای، نه enum)
)

/**
 * findDependents تزریق‌پذیر است (I/O واقعی، وابسته به واحد ۰۷/۱۵)؛ پیش‌فرض بدون‌اثر
 * ({ emptyList() }) مشابه الگوی OverrideEventLogger.NoOp — فقط زمانی واقعاً فراخوانی
 * می‌شود که hasDependencyChange=true باشد.
 */
fun analyzeVersionImpact(
    entity: Entity,
    modifiedFields: List<String>,
    findDependents: (entityId: String) -> List<String> = { emptyList() }
): ImpactResult {
    val dependencyFields = listOf("scene_id", "asset_references")
    val hasDependencyChange = modifiedFields.any { it in dependencyFields }
    val riskLevel = when {
        entity.type == "project_dna" -> "critical" // اثر روی کل پروژه
        hasDependencyChange -> "high"
        else -> "low"
    }
    return ImpactResult(
        affectedEntityIds = if (hasDependencyChange) findDependents(entity.id) else listOf(entity.id),
        dependencyChanges = hasDependencyChange,
        breakingChanges = false,
        riskLevel = riskLevel
    )
}
