package com.operaboys.cinemashotgenerator.domain.promptengine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeedManagementTest {

    @Test
    fun `useSeed false returns null regardless of explicit value`() {
        assertNull(manageSeed("shot_001", useSeed = false, explicitSeedValue = 42))
    }

    @Test
    fun `explicit seed value takes priority`() {
        assertEquals(42, manageSeed("shot_001", useSeed = true, explicitSeedValue = 42))
    }

    @Test
    fun `shotId hash is used when no explicit value given`() {
        assertEquals("shot_001".hashCode(), manageSeed("shot_001", useSeed = true, explicitSeedValue = null))
    }

    @Test
    fun `shotId hash based seed is idempotent across calls`() {
        val first = manageSeed("shot_001", useSeed = true, explicitSeedValue = null)
        val second = manageSeed("shot_001", useSeed = true, explicitSeedValue = null)
        assertEquals(first, second)
    }
}
