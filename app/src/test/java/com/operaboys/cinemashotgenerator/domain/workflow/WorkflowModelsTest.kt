package com.operaboys.cinemashotgenerator.domain.workflow

import com.operaboys.cinemashotgenerator.domain.outputdelivery.RenderedOutput
import com.operaboys.cinemashotgenerator.domain.promptengine.PromptBlueprint
import com.operaboys.cinemashotgenerator.domain.promptengine.StructuredParts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// واحد ۱۶ — تست‌های domain/workflow/ (رفع یافته‌ی F1 ممیزی pre-Unit 16،
// docs/adr/037-unit16-workflow-models-quality-score.md).

class WorkflowModelsTest {

    private fun sampleState(stepStatus: Map<WorkflowStep, StepStatus>) = WorkflowState(
        sessionId = "session_001",
        projectId = "proj_001",
        currentStep = WorkflowStep.SHOT_CREATION,
        stepStatus = stepStatus,
        startedAt = "2026-07-28T10:00:00Z",
        lastActionAt = "2026-07-28T11:00:00Z"
    )

    // --- canJumpToStep: هرگز false برنمی‌گرداند (فقط هشدار، نه Block) ---

    @Test
    fun `canJumpToStep allows the jump with no warning when all previous steps are completed`() {
        val completedPreviousSteps = WorkflowStep.entries
            .filter { it.ordinal < WorkflowStep.SHOT_CREATION.ordinal }
            .associateWith { StepStatus.COMPLETED }
        val state = sampleState(completedPreviousSteps)

        val (allowed, warning) = canJumpToStep(state, WorkflowStep.SHOT_CREATION)

        assertTrue(allowed)
        assertNull(warning)
    }

    @Test
    fun `canJumpToStep still allows the jump but with a warning when previous steps are incomplete`() {
        val state = sampleState(emptyMap())

        val (allowed, warning) = canJumpToStep(state, WorkflowStep.SHOT_CREATION)

        assertTrue(allowed)
        assertNotNull(warning)
    }

    @Test
    fun `canJumpToStep never blocks, regardless of how incomplete the previous steps are`() {
        // طبق طراحی صریح بلوپرینت ۱۶: این تابع همیشه true برمی‌گرداند — عنصر اول
        // Pair هرگز false نیست، فقط پیام هشدار (عنصر دوم) بین null و یک متن تغییر می‌کند.
        WorkflowStep.entries.forEach { target ->
            val (allowed, _) = canJumpToStep(sampleState(emptyMap()), target)
            assertTrue("canJumpToStep must never block for target=$target", allowed)
        }
    }

    // --- WorkflowState.progressPercentage ---

    @Test
    fun `progressPercentage is 0 when no step is completed`() {
        val state = sampleState(WorkflowStep.entries.associateWith { StepStatus.NOT_STARTED })
        assertEquals(0, state.progressPercentage)
    }

    @Test
    fun `progressPercentage reflects a partial completion ratio`() {
        // ۹ مرحله در کل؛ ۳ مرحله کامل → ۳ * 100 / 9 = 33 (تقسیم صحیح Int)
        val stepStatus = WorkflowStep.entries.associateWith { StepStatus.NOT_STARTED } +
            mapOf(
                WorkflowStep.STORY_WIZARD to StepStatus.COMPLETED,
                WorkflowStep.AI_STORY_BREAKDOWN to StepStatus.COMPLETED,
                WorkflowStep.DNA_CONFIG to StepStatus.COMPLETED
            )
        val state = sampleState(stepStatus)
        assertEquals(33, state.progressPercentage)
    }

    @Test
    fun `progressPercentage is 100 when every step is completed`() {
        val state = sampleState(WorkflowStep.entries.associateWith { StepStatus.COMPLETED })
        assertEquals(100, state.progressPercentage)
    }

    // --- evaluatePromptQuality (Option A، ADR-037) ---

    private fun richStructuredParts() = StructuredParts(
        subjectDescription = "a tall detective in a black leather jacket, weathered face, sharp eyes",
        sceneContext = "tense atmosphere, night time in a rain-soaked city street",
        shotDescription = "detective walks slowly toward the camera, coat billowing in the wind",
        cameraSpecs = "low angle, close-up shot, dolly-in movement, shallow depth of field",
        lightingSpecs = "dramatic lighting, side key light, high contrast, cold color temperature",
        environmentSpecs = "heavy rain, wet ground, reduced visibility",
        styleModifiers = "cinematic style, noir color grading, anamorphic lens flares",
        timelineBeats = "[1.5s] detective raises head",
        audioDescription = "ambient: heavy rain (heavy); action sounds: footstep"
    )

    private fun minimalStructuredParts() = StructuredParts(
        subjectDescription = "",
        sceneContext = "",
        shotDescription = "a shot",
        cameraSpecs = "",
        lightingSpecs = "",
        environmentSpecs = null,
        styleModifiers = "",
        timelineBeats = null,
        audioDescription = null
    )

    private fun blueprintWith(parts: StructuredParts) = PromptBlueprint(
        promptBlueprintId = "prompt_001",
        shotId = "shot_001",
        structuredParts = parts,
        imageReferences = emptyList(),
        weightedEmphasis = emptyMap(),
        seed = null,
        conflictsResolved = 0,
        warnings = emptyList()
    )

    @Test
    fun `evaluatePromptQuality gives a high total score for a rich, fully-populated prompt`() {
        val blueprint = blueprintWith(richStructuredParts())
        val rendered = RenderedOutput(modelProfileId = "veo_3_1", formattedPrompt = "a".repeat(500), language = "en")

        val score = evaluatePromptQuality(rendered, blueprint)

        assertTrue("expected a high total score, got ${score.total}", score.total >= 80)
    }

    @Test
    fun `evaluatePromptQuality gives a low total score for a minimal, mostly-empty prompt`() {
        val blueprint = blueprintWith(minimalStructuredParts())
        val rendered = RenderedOutput(modelProfileId = "veo_3_1", formattedPrompt = "", language = "en")

        val score = evaluatePromptQuality(rendered, blueprint)

        assertTrue("expected a low total score, got ${score.total}", score.total <= 20)
    }

    @Test
    fun `evaluatePromptQuality does not penalize a null environmentSpecs (a valid clear-weather state)`() {
        val richButClearWeather = richStructuredParts().copy(environmentSpecs = null)
        val blueprint = blueprintWith(richButClearWeather)
        val rendered = RenderedOutput(modelProfileId = "veo_3_1", formattedPrompt = "a".repeat(500), language = "en")

        val score = evaluatePromptQuality(rendered, blueprint)

        // چون lightingSpecs غنی است، حتی وقتی environmentSpecs=null (Clear معتبر)،
        // visualSpecificity نباید به صفر یا نصف بی‌جهت افت کند.
        assertEquals(20, score.visualSpecificity)
    }
}
