package com.operaboys.cinemashotgenerator.domain.validation

// واحد ۰۷ — بخش ج: Dependency Resolver
// منبع حقیقت: docs/blueprints/07-validation-and-consistency.md

enum class DependencyType { STRONG, WEAK, REFERENCE }

data class DependencyEdge(val sourceId: String, val targetId: String, val type: DependencyType)

data class ImpactReport(
    val directlyAffected: List<String>,
    val transitivelyAffected: List<String>,
    val toInvalidate: List<String>,
    val toWarn: List<String>
)

/**
 * تحلیل اثر یک تغییر با پیمایش سطح مستقیم روی گراف وابستگی.
 *
 * transitivelyAffected عمداً خالی می‌ماند — دقیقاً طبق کد مفهومی بلوپرینت (که این
 * فیلد را به‌عنوان نمونه خالی گذاشته)؛ BFS واقعی چندسطحی در این قدم پیاده نشد چون
 * دستور کار صراحتاً همین رفتار را برای این قدم خواسته بود.
 */
fun analyzeImpact(changedNodeId: String, edges: List<DependencyEdge>): ImpactReport {
    val direct = edges.filter { it.sourceId == changedNodeId }
    val toInvalidate = direct.filter { it.type == DependencyType.STRONG }.map { it.targetId }
    val toWarn = direct.filter { it.type == DependencyType.WEAK }.map { it.targetId }
    return ImpactReport(
        directlyAffected = direct.map { it.targetId },
        transitivelyAffected = emptyList(),
        toInvalidate = toInvalidate,
        toWarn = toWarn
    )
}

/**
 * حذف یک Asset در حال استفاده مجاز نیست.
 *
 * NOTE: این تابع مستقیماً از کد مفهومی بلوپرینت ۰۷ کپی شده و امضای متفاوتی از
 * validateAssetDeletion (واحد ۰۶، Rule 3) دارد — آنجا لیست شات‌های استفاده‌کننده
 * (List<String>) می‌گیرد و ValidationIssue? سراسری برمی‌گرداند (بعد از Migration ۱،
 * docs/adr/010-cross-unit-migrations.md)؛ اینجا شمارش (Int) می‌گیرد و Result<Unit>
 * برمی‌گرداند. یکی‌سازی این دو، پیشنهاد باز باقی‌مانده در ADR-010 است — در این قدم اجرا نشد.
 */
fun canDeleteAsset(assetId: String, usageCount: Int): Result<Unit> {
    if (usageCount > 0) {
        return Result.failure(IllegalStateException("این Asset در $usageCount شات استفاده شده است"))
    }
    return Result.success(Unit)
}

/** تشخیص وابستگی دایره‌ای قبل از افزودن یک Edge جدید (DFS). */
fun wouldCreateCycle(source: String, target: String, existingEdges: List<DependencyEdge>): Boolean {
    val visited = mutableSetOf<String>()
    fun dfs(node: String): Boolean {
        if (node == source) return true
        if (!visited.add(node)) return false
        return existingEdges.filter { it.sourceId == node }.any { dfs(it.targetId) }
    }
    return dfs(target)
}
