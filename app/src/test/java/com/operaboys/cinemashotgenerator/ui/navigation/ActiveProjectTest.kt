package com.operaboys.cinemashotgenerator.ui.navigation

import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.project.Project
import com.operaboys.cinemashotgenerator.domain.project.ProjectSummary
import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState
import com.operaboys.cinemashotgenerator.domain.workflow.StepStatus
import com.operaboys.cinemashotgenerator.domain.workflow.WorkflowState
import com.operaboys.cinemashotgenerator.domain.workflow.WorkflowStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// رفع G6 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، docs/adr/062-...):
// تست واحد خالص resolveActiveOrRecentProjectId — بدون هیچ Compose/Robolectric،
// چون این تابع کاملاً بدون وابستگی خارجی است (فقط دو پارامتر ساده می‌گیرد).

private fun summary(projectId: String) = ProjectSummary(
    project = Project(
        projectId = projectId,
        projectName = "Project $projectId",
        createdAt = "2026-01-01T00:00:00Z",
        lastModified = "2026-01-01T00:00:00Z",
        uiLanguage = Language.FA,
        state = EntityState.DRAFT
    ),
    sceneCount = 0,
    shotCount = 0
)

private fun activeState(projectId: String) = WorkflowState(
    sessionId = "session_1",
    projectId = projectId,
    currentStep = WorkflowStep.STORY_WIZARD,
    stepStatus = emptyMap(),
    startedAt = "2026-01-01T00:00:00Z",
    lastActionAt = "2026-01-01T00:00:00Z"
)

class ActiveProjectTest {

    @Test
    fun `an active WorkflowState session wins over any project summary`() {
        val result = resolveActiveOrRecentProjectId(
            workflowState = activeState("proj_active"),
            projectSummaries = listOf(summary("proj_most_recent"), summary("proj_older"))
        )
        assertEquals("proj_active", result)
    }

    @Test
    fun `no active session falls back to the first (most recently modified) project summary`() {
        val result = resolveActiveOrRecentProjectId(
            workflowState = null,
            projectSummaries = listOf(summary("proj_most_recent"), summary("proj_older"))
        )
        assertEquals("proj_most_recent", result)
    }

    @Test
    fun `no active session and no projects at all returns null`() {
        val result = resolveActiveOrRecentProjectId(workflowState = null, projectSummaries = emptyList())
        assertNull(result)
    }
}
