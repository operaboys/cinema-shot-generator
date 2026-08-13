package com.operaboys.cinemashotgenerator.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.workflow.StepStatus
import com.operaboys.cinemashotgenerator.domain.workflow.WorkflowState
import com.operaboys.cinemashotgenerator.domain.workflow.WorkflowStep
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// رفع G12 باقی‌مانده (دستور کار ۲۰۲۶-۰۸-۱۳، docs/adr/080-...): allowFreeStepJump
// قبلاً هیچ اثر Runtime ای نداشت — evaluateStudioTabJump/canJumpToStep (ADR-037)
// هرگز پرش را Block نمی‌کند، فقط هشدار می‌دهد؛ این تست ثابت می‌کند سوییچ اکنون
// واقعاً کنترل می‌کند که آیا آن هشدار به‌صورت غیرمزاحم (Snackbar) نمایش داده
// می‌شود یا یک Dialog تأیید صریح قبل از پرش لازم است.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudioTopTabRowJumpConfirmationTest {

    @get:Rule
    val composeRule = createComposeRule()

    // هم‌الگو دقیق با StudioTopTabRowTest.sampleState — هیچ مرحله‌ای COMPLETED
    // نیست، پس evaluateStudioTabJump(..., StudioTab.OUTPUT) همیشه هشدار برمی‌گرداند.
    private val stateWithWarning = WorkflowState(
        sessionId = "session_001",
        projectId = "proj_001",
        currentStep = WorkflowStep.SCENE_CREATION,
        stepStatus = emptyMap<WorkflowStep, StepStatus>(),
        startedAt = "2026-07-28T10:00:00Z",
        lastActionAt = "2026-07-28T11:00:00Z"
    )

    @Test
    fun `allowFreeStepJump true jumps immediately without a confirmation dialog`() {
        var selected: StudioTab? = null
        var warned: String? = null
        composeRule.setContent {
            StudioTopTabRow(
                selectedTab = StudioTab.STORY,
                workflowState = stateWithWarning,
                language = Language.FA,
                allowFreeStepJump = true,
                onTabSelected = { selected = it },
                onWarning = { warned = it }
            )
        }

        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.OUTPUT)).performClick()

        assertEquals(StudioTab.OUTPUT, selected)
        assert(warned != null) { "باید همچنان هشدار Snackbar داده شود" }
    }

    @Test
    fun `allowFreeStepJump false shows a confirmation dialog and does not jump until confirmed`() {
        var selected: StudioTab? = null
        composeRule.setContent {
            StudioTopTabRow(
                selectedTab = StudioTab.STORY,
                workflowState = stateWithWarning,
                language = Language.FA,
                allowFreeStepJump = false,
                onTabSelected = { selected = it },
                onWarning = {}
            )
        }

        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.OUTPUT)).performClick()

        assertNull(selected)
        composeRule.onNodeWithText(uiString("studioTab.jump.confirmTitle", Language.FA)).assertIsDisplayed()

        composeRule.onNodeWithTag(STUDIO_TAB_JUMP_CONFIRM_TAG).performClick()
        assertEquals(StudioTab.OUTPUT, selected)
    }

    @Test
    fun `allowFreeStepJump false and cancelling the dialog does not jump`() {
        var selected: StudioTab? = null
        composeRule.setContent {
            StudioTopTabRow(
                selectedTab = StudioTab.STORY,
                workflowState = stateWithWarning,
                language = Language.FA,
                allowFreeStepJump = false,
                onTabSelected = { selected = it },
                onWarning = {}
            )
        }

        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.OUTPUT)).performClick()
        composeRule.onNodeWithTag(STUDIO_TAB_JUMP_CANCEL_TAG).performClick()

        assertNull(selected)
    }

    @Test
    fun `allowFreeStepJump false but no warning jumps immediately (no active WorkflowState)`() {
        var selected: StudioTab? = null
        composeRule.setContent {
            StudioTopTabRow(
                selectedTab = StudioTab.STORY,
                workflowState = null,
                language = Language.FA,
                allowFreeStepJump = false,
                onTabSelected = { selected = it },
                onWarning = {}
            )
        }

        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.OUTPUT)).performClick()

        assertEquals(StudioTab.OUTPUT, selected)
    }
}
