package com.operaboys.cinemashotgenerator.domain.shot

import org.junit.Assert.assertEquals
import org.junit.Test

class ShotInheritanceTest {

    @Test
    fun `inheritOrOverride uses shot value when explicitly provided`() {
        val result = inheritOrOverride(sceneValue = "scene_natural", shotValue = "shot_override")
        assertEquals("shot_override", result)
    }

    @Test
    fun `inheritOrOverride falls back to scene value when shot value is null`() {
        val result = inheritOrOverride(sceneValue = "scene_natural", shotValue = null)
        assertEquals("scene_natural", result)
    }

    @Test
    fun `inheritOrOverride works generically with non-string types`() {
        val result = inheritOrOverride(sceneValue = MotionLevel.MODERATE, shotValue = MotionLevel.EXTREME)
        assertEquals(MotionLevel.EXTREME, result)
    }
}
