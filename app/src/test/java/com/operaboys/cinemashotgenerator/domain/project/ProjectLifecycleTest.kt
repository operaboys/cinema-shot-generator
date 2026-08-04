package com.operaboys.cinemashotgenerator.domain.project

import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// واحد ۱۶ فاز ۱ — تست Rule واقعی State Machine روی archiveProject
// (docs/adr/044-unit16-phase1-app-shell.md).

class ProjectLifecycleTest {

    private fun sampleProject(state: EntityState) = Project(
        projectId = "proj_001",
        projectName = "Test Project",
        createdAt = "2026-08-01T00:00:00Z",
        lastModified = "2026-08-01T00:00:00Z",
        uiLanguage = Language.FA,
        state = state
    )

    @Test
    fun `archiveProject succeeds only when the project is FINAL, per ALLOWED_TRANSITIONS`() {
        val result = archiveProject(sampleProject(EntityState.FINAL))

        assertTrue(result.isSuccess)
        assertEquals(EntityState.ARCHIVED, result.getOrThrow().state)
    }

    @Test
    fun `archiveProject fails for a DRAFT project, even though the overflow menu always shows Archive`() {
        val result = archiveProject(sampleProject(EntityState.DRAFT))

        assertTrue(result.isFailure)
    }

    @Test
    fun `archiveProject fails for REVIEW and LOCKED projects too`() {
        assertTrue(archiveProject(sampleProject(EntityState.REVIEW)).isFailure)
        assertTrue(archiveProject(sampleProject(EntityState.LOCKED)).isFailure)
    }

    @Test
    fun `archiveProject on an already-archived project fails (ARCHIVED has no outgoing transitions)`() {
        assertTrue(archiveProject(sampleProject(EntityState.ARCHIVED)).isFailure)
    }
}
