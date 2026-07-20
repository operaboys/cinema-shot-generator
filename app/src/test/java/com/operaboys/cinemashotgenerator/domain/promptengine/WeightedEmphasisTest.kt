package com.operaboys.cinemashotgenerator.domain.promptengine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightedEmphasisTest {

    @Test
    fun `null weighted tags returns empty map`() {
        assertTrue(collectWeightedEmphasis(null).isEmpty())
    }

    @Test
    fun `given weighted tags are returned as-is`() {
        val tags = mapOf("cinematic" to 1.4f, "dramatic" to 1.2f)
        assertEquals(tags, collectWeightedEmphasis(tags))
    }
}
