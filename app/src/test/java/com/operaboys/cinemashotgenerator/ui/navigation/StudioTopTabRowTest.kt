package com.operaboys.cinemashotgenerator.ui.navigation

import com.operaboys.cinemashotgenerator.domain.workflow.StepStatus
import com.operaboys.cinemashotgenerator.domain.workflow.WorkflowState
import com.operaboys.cinemashotgenerator.domain.workflow.WorkflowStep
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// واحد ۱۶ — فاز ۰: تست evaluateStudioTabJump — پوشش می‌دهد که جابه‌جایی هیچ‌وقت
// Block نمی‌شود (طبق canJumpToStep واقعی واحد ۱۶، ADR-037)، فقط هشدار می‌دهد.

class StudioTopTabRowTest {

    private fun sampleState(stepStatus: Map<WorkflowStep, StepStatus>) = WorkflowState(
        sessionId = "session_001",
        projectId = "proj_001",
        currentStep = WorkflowStep.SCENE_CREATION,
        stepStatus = stepStatus,
        startedAt = "2026-07-28T10:00:00Z",
        lastActionAt = "2026-07-28T11:00:00Z"
    )

    @Test
    fun `jumping to any tab is always allowed even with no active WorkflowState`() {
        StudioTab.entries.forEach { tab ->
            val (allowed, warning) = evaluateStudioTabJump(workflowState = null, tab = tab)
            assertTrue(allowed)
            assertNull(warning)
        }
    }

    @Test
    fun `jumping to a tab with all previous steps completed is allowed without a warning`() {
        val completed = WorkflowStep.entries
            .filter { it.ordinal < WorkflowStep.SCENE_CREATION.ordinal }
            .associateWith { StepStatus.COMPLETED }
        val (allowed, warning) = evaluateStudioTabJump(sampleState(completed), StudioTab.SCENES)

        assertTrue(allowed)
        assertNull(warning)
    }

    @Test
    fun `jumping to a tab with incomplete previous steps is still allowed, but warns`() {
        val (allowed, warning) = evaluateStudioTabJump(sampleState(emptyMap()), StudioTab.OUTPUT)

        assertTrue(allowed)
        assertNotNull(warning)
    }
}
