package com.operaboys.cinemashotgenerator.domain.stateversioning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImpactAnalysisTest {

    @Test
    fun `project_dna entity type is always critical risk`() {
        val entity = Entity(id = "dna_001", type = "project_dna")
        val result = analyzeVersionImpact(entity, modifiedFields = listOf("shot_description"))
        assertEquals("critical", result.riskLevel)
    }

    @Test
    fun `dependency field change on a non-dna entity is high risk and calls findDependents`() {
        val entity = Entity(id = "shot_001", type = "shot")
        val result = analyzeVersionImpact(
            entity = entity,
            modifiedFields = listOf("scene_id"),
            findDependents = { entityId -> listOf("${entityId}_dependent") }
        )
        assertEquals("high", result.riskLevel)
        assertTrue(result.dependencyChanges)
        assertEquals(listOf("shot_001_dependent"), result.affectedEntityIds)
    }

    @Test
    fun `unrelated field change on a non-dna entity is low risk`() {
        val entity = Entity(id = "shot_001", type = "shot")
        val result = analyzeVersionImpact(entity, modifiedFields = listOf("shot_description"))
        assertEquals("low", result.riskLevel)
        assertFalse(result.dependencyChanges)
        assertEquals(listOf("shot_001"), result.affectedEntityIds)
    }
}
