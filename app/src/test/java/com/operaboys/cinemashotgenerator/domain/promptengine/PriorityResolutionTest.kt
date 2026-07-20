package com.operaboys.cinemashotgenerator.domain.promptengine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriorityResolutionTest {

    @Test
    fun `human override wins when present`() {
        val values = mapOf(
            PriorityLevel.HUMAN_OVERRIDE to "override",
            PriorityLevel.SHOT_SPECIFIC to "shot",
            PriorityLevel.PROJECT_DNA to "dna"
        )
        assertEquals("override", resolvePriority(values))
    }

    @Test
    fun `falls through to first non-null level in priority order`() {
        val values = mapOf<PriorityLevel, String?>(
            PriorityLevel.HUMAN_OVERRIDE to null,
            PriorityLevel.SHOT_SPECIFIC to null,
            PriorityLevel.CHARACTER_CONTINUITY to "continuity",
            PriorityLevel.SCENE_CONTEXT to "scene",
            PriorityLevel.PROJECT_DNA to "dna"
        )
        assertEquals("continuity", resolvePriority(values))
    }

    @Test
    fun `falls back to project dna when everything else is absent`() {
        val values = mapOf(PriorityLevel.PROJECT_DNA to "dna")
        assertEquals("dna", resolvePriority(values))
    }

    @Test
    fun `all levels empty returns null`() {
        assertNull(resolvePriority(emptyMap<PriorityLevel, String?>()))
    }
}
