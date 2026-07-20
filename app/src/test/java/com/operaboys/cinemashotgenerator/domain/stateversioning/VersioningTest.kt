package com.operaboys.cinemashotgenerator.domain.stateversioning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersioningTest {

    // --- determineVersionType ---

    @Test
    fun `scene_id modification is risky`() {
        assertEquals(VersionType.RISKY, determineVersionType(listOf("scene_id")))
    }

    @Test
    fun `asset_references modification is risky`() {
        assertEquals(VersionType.RISKY, determineVersionType(listOf("asset_references")))
    }

    @Test
    fun `core_identity modification is risky`() {
        assertEquals(VersionType.RISKY, determineVersionType(listOf("core_identity")))
    }

    @Test
    fun `unrelated field modification is safe`() {
        assertEquals(VersionType.SAFE, determineVersionType(listOf("shot_description")))
    }

    // --- rollbackToVersion ---

    private val targetVersion = EntityVersion(
        versionId = "v1.0.0",
        entityId = "shot_001",
        versionType = VersionType.SAFE,
        createdAt = "2026-07-19T10:00:00Z",
        changeSummary = "نسخه‌ی اولیه",
        modifiedFields = emptyList(),
        snapshotData = "{\"shotDescription\":\"...\"}",
        parentVersionId = null
    )

    @Test
    fun `rollbackToVersion calls createSnapshot with the target version's data and logs the event`() {
        var snapshotEntityId: String? = null
        var snapshotData: String? = null
        var snapshotChangeSummary: String? = null
        var loggedEventType: String? = null
        var loggedEntityId: String? = null

        val result = rollbackToVersion(
            targetVersion = targetVersion,
            createSnapshot = { entityId, data, changeSummary ->
                snapshotEntityId = entityId
                snapshotData = data
                snapshotChangeSummary = changeSummary
                targetVersion.copy(versionId = "v1.0.1", changeSummary = changeSummary)
            },
            logEvent = { eventType, entityId ->
                loggedEventType = eventType
                loggedEntityId = entityId
            }
        )

        assertTrue(result.isSuccess)
        assertEquals("shot_001", snapshotEntityId)
        assertEquals(targetVersion.snapshotData, snapshotData)
        assertTrue(snapshotChangeSummary!!.contains("v1.0.0"))
        assertEquals("rollback_performed", loggedEventType)
        assertEquals("shot_001", loggedEntityId)
        assertEquals("v1.0.1", result.getOrThrow().versionId)
    }
}
