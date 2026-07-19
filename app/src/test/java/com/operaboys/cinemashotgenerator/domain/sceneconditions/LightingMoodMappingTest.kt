package com.operaboys.cinemashotgenerator.domain.sceneconditions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LightingMoodMappingTest {

    @Test
    fun `tense mood maps to dramatic cold preset`() {
        val preset = mapMoodToLighting("tense")
        assertEquals("dramatic", preset!!.style)
        assertEquals("cold", preset.colorTemperature)
    }

    @Test
    fun `calm mood maps to soft warm preset`() {
        val preset = mapMoodToLighting("calm")
        assertEquals("soft", preset!!.style)
        assertEquals("warm", preset.colorTemperature)
    }

    @Test
    fun `dark mood maps to noir preset`() {
        val preset = mapMoodToLighting("dark")
        assertEquals("noir", preset!!.style)
    }

    @Test
    fun `mysterious mood maps to dramatic back-lit preset`() {
        val preset = mapMoodToLighting("mysterious")
        assertEquals("dramatic", preset!!.style)
        assertEquals("back", preset.keyLightPosition)
    }

    @Test
    fun `hopeful mood maps to cinematic warm preset`() {
        val preset = mapMoodToLighting("hopeful")
        assertEquals("cinematic", preset!!.style)
    }

    @Test
    fun `emotional mood maps to soft warm preset`() {
        val preset = mapMoodToLighting("emotional")
        assertEquals("soft", preset!!.style)
    }

    @Test
    fun `unknown mood returns null`() {
        assertNull(mapMoodToLighting("euphoric"))
    }
}
