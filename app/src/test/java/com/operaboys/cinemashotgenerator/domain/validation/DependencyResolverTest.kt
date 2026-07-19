package com.operaboys.cinemashotgenerator.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DependencyResolverTest {

    // --- analyzeImpact ---

    @Test
    fun `analyzeImpact distributes strong to invalidate and weak to warn`() {
        val edges = listOf(
            DependencyEdge("dna_001", "scene_001", DependencyType.STRONG),
            DependencyEdge("dna_001", "scene_002", DependencyType.STRONG),
            DependencyEdge("dna_001", "shot_099", DependencyType.WEAK),
            DependencyEdge("dna_001", "obj_bg", DependencyType.REFERENCE),
            DependencyEdge("scene_001", "shot_001", DependencyType.STRONG) // نامرتبط، sourceId متفاوت
        )

        val report = analyzeImpact("dna_001", edges)

        assertEquals(listOf("scene_001", "scene_002", "shot_099", "obj_bg"), report.directlyAffected)
        assertEquals(listOf("scene_001", "scene_002"), report.toInvalidate)
        assertEquals(listOf("shot_099"), report.toWarn)
        assertTrue(report.transitivelyAffected.isEmpty())
    }

    @Test
    fun `analyzeImpact with no matching edges returns empty report`() {
        val report = analyzeImpact("unrelated_node", edges = emptyList())

        assertTrue(report.directlyAffected.isEmpty())
        assertTrue(report.toInvalidate.isEmpty())
        assertTrue(report.toWarn.isEmpty())
    }

    // --- canDeleteAsset ---

    @Test
    fun `canDeleteAsset succeeds when unused`() {
        val result = canDeleteAsset("char_001", usageCount = 0)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `canDeleteAsset fails when in use`() {
        val result = canDeleteAsset("char_001", usageCount = 3)

        assertTrue(result.isFailure)
    }

    // --- wouldCreateCycle ---

    @Test
    fun `acyclic graph reports no cycle`() {
        val edges = listOf(
            DependencyEdge("dna_001", "scene_001", DependencyType.STRONG),
            DependencyEdge("scene_001", "shot_001", DependencyType.STRONG)
        )

        // افزودن یال جدید shot_001 -> shot_002 چرخه نمی‌سازد
        assertFalse(wouldCreateCycle(source = "shot_001", target = "shot_002", existingEdges = edges))
    }

    @Test
    fun `adding edge that closes a loop is detected as a cycle`() {
        val edges = listOf(
            DependencyEdge("dna_001", "scene_001", DependencyType.STRONG),
            DependencyEdge("scene_001", "shot_001", DependencyType.STRONG)
        )

        // افزودن یال جدید shot_001 -> dna_001 یک چرخه می‌سازد: dna_001 -> scene_001 -> shot_001 -> dna_001
        assertTrue(wouldCreateCycle(source = "shot_001", target = "dna_001", existingEdges = edges))
    }
}
