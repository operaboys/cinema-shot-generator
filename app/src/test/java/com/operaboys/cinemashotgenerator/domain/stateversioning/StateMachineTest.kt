package com.operaboys.cinemashotgenerator.domain.stateversioning

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StateMachineTest {

    // --- canTransition ---

    @Test
    fun `every allowed transition in the table returns true`() {
        ALLOWED_TRANSITIONS.forEach { (current, targets) ->
            targets.forEach { target ->
                assertTrue("$current -> $target باید مجاز باشد", canTransition(current, target))
            }
        }
    }

    @Test
    fun `draft to final directly is not allowed`() {
        assertFalse(canTransition(EntityState.DRAFT, EntityState.FINAL))
    }

    @Test
    fun `review to final directly is not allowed`() {
        assertFalse(canTransition(EntityState.REVIEW, EntityState.FINAL))
    }

    @Test
    fun `final to draft is not allowed`() {
        assertFalse(canTransition(EntityState.FINAL, EntityState.DRAFT))
    }

    @Test
    fun `archived has no outgoing transitions`() {
        EntityState.values().forEach { target ->
            assertFalse(canTransition(EntityState.ARCHIVED, target))
        }
    }

    // --- validateStateTransition ---

    @Test
    fun `validateStateTransition is null for an allowed transition`() {
        assertNull(validateStateTransition(EntityState.DRAFT, EntityState.REVIEW))
    }

    @Test
    fun `validateStateTransition is blocking for draft to final`() {
        val issue = validateStateTransition(EntityState.DRAFT, EntityState.FINAL)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    // --- validateReviewReadiness ---

    @Test
    fun `validateReviewReadiness is null when all required fields are present`() {
        assertNull(validateReviewReadiness(EntityState.REVIEW, emptyList()))
    }

    @Test
    fun `validateReviewReadiness is blocking when required fields are missing`() {
        val issue = validateReviewReadiness(EntityState.REVIEW, listOf("shot_description"))
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `validateReviewReadiness is null when target is not review`() {
        assertNull(validateReviewReadiness(EntityState.DRAFT, listOf("shot_description")))
    }

    // --- validateDependenciesFinalized ---

    @Test
    fun `validateDependenciesFinalized is null when all dependencies are final`() {
        assertNull(validateDependenciesFinalized(EntityState.FINAL, emptyList()))
    }

    @Test
    fun `validateDependenciesFinalized is blocking when a dependency is not final`() {
        val issue = validateDependenciesFinalized(EntityState.FINAL, listOf("scene_001"))
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `validateDependenciesFinalized is null when target is not final`() {
        assertNull(validateDependenciesFinalized(EntityState.LOCKED, listOf("scene_001")))
    }
}
