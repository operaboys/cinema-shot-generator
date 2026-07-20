package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.DependencyEdgeDao
import com.operaboys.cinemashotgenerator.domain.stateversioning.Entity
import com.operaboys.cinemashotgenerator.domain.stateversioning.ImpactResult
import com.operaboys.cinemashotgenerator.domain.stateversioning.analyzeVersionImpact

// واحد ۱۵ — قدم ۲: اتصال واقعی findDependents (پارامتر تزریق‌پذیر analyzeVersionImpact،
// واحد ۱۲) به DependencyEdgeDao.
//
// findDependents در امضای analyzeVersionImpact synchronous است (نه suspend)؛ برخلاف
// VersioningRepository (که برای هر Rollback فقط یک بار I/O لازم است)، اینجا تمام
// یال‌های وابستگی از پیش (Prefetch) با یک فراخوان suspend واحد خوانده می‌شوند و
// findDependents فقط روی همان داده‌ی از پیش‌بارگذاری‌شده فیلتر خالص انجام می‌دهد —
// بدون I/O درون Callback synchronous.
class ImpactAnalysisRepository(private val dependencyEdgeDao: DependencyEdgeDao) {
    suspend fun analyzeImpact(entityId: String, entityType: String, modifiedFields: List<String>): ImpactResult {
        val edges = dependencyEdgeDao.getAllEdges()
        return analyzeVersionImpact(
            entity = Entity(id = entityId, type = entityType),
            modifiedFields = modifiedFields,
            findDependents = { sourceId -> edges.filter { it.sourceId == sourceId }.map { it.targetId } }
        )
    }
}
