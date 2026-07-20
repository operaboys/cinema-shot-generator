package com.operaboys.cinemashotgenerator.domain.storage

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferentialIntegrityTest {

    private val sceneIds = setOf("scene_001")
    private val assetIds = setOf("char_001", "loc_001")

    @Test
    fun `no issues when every reference is valid`() {
        val shots = listOf(
            ShotReferenceData(shotId = "shot_001", sceneId = "scene_001", characterIds = listOf("char_001"))
        )
        assertTrue(validateReferentialIntegrity(shots, sceneIds, assetIds).isEmpty())
    }

    @Test
    fun `reports a shot referencing a non-existent scene`() {
        val shots = listOf(ShotReferenceData(shotId = "shot_001", sceneId = "scene_missing"))
        val issues = validateReferentialIntegrity(shots, sceneIds, assetIds)

        assertEquals(1, issues.size)
        assertEquals("shot_001", issues[0].source)
        assertEquals("scene_missing", issues[0].brokenReferenceTo)
    }

    @Test
    fun `reports a shot referencing a non-existent asset`() {
        val shots = listOf(
            ShotReferenceData(shotId = "shot_001", sceneId = "scene_001", characterIds = listOf("char_missing"))
        )
        val issues = validateReferentialIntegrity(shots, sceneIds, assetIds)

        assertEquals(1, issues.size)
        assertEquals("shot_001", issues[0].source)
        assertEquals("char_missing", issues[0].brokenReferenceTo)
    }

    @Test
    fun `validateReferentialIntegrityAsIssues maps every broken reference to a blocking ValidationIssue`() {
        val shots = listOf(ShotReferenceData(shotId = "shot_001", sceneId = "scene_missing"))
        val issues = validateReferentialIntegrityAsIssues(shots, sceneIds, assetIds)

        assertEquals(1, issues.size)
        assertEquals(Severity.BLOCKING, issues[0].severity)
        assertEquals("shot_001", issues[0].field)
    }
}
