package com.operaboys.cinemashotgenerator.domain.sceneconditions

import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import org.junit.Assert.assertEquals
import org.junit.Test

// واحد ۰۸ — تست‌های mapMoodToLighting پس از Type-Safety Migration
// (رفع F3 ممیزی pre-Unit 16، docs/adr/039-unit03-unit08-mood-type-safety-migration.md).

class LightingMoodMappingTest {

    @Test
    fun `tense mood maps to dramatic cold preset`() {
        val preset = mapMoodToLighting(Mood.TENSE)
        assertEquals(LightingStyle.DRAMATIC_LIGHT, preset.style)
        assertEquals(ColorTemperature.COLD, preset.colorTemperature)
    }

    @Test
    fun `calm mood maps to soft warm preset`() {
        val preset = mapMoodToLighting(Mood.CALM)
        assertEquals(LightingStyle.SOFT_LIGHT, preset.style)
        assertEquals(ColorTemperature.WARM, preset.colorTemperature)
    }

    @Test
    fun `dark mood maps to low-key preset`() {
        val preset = mapMoodToLighting(Mood.DARK)
        assertEquals(LightingStyle.LOW_KEY, preset.style)
    }

    @Test
    fun `mysterious mood maps to dramatic back-lit preset`() {
        val preset = mapMoodToLighting(Mood.MYSTERIOUS)
        assertEquals(LightingStyle.DRAMATIC_LIGHT, preset.style)
        assertEquals(KeyLightPosition.BACK, preset.keyLightPosition)
    }

    @Test
    fun `hopeful mood maps to high-key warm preset`() {
        val preset = mapMoodToLighting(Mood.HOPEFUL)
        assertEquals(LightingStyle.HIGH_KEY, preset.style)
    }

    @Test
    fun `emotional mood maps to soft warm preset`() {
        val preset = mapMoodToLighting(Mood.EMOTIONAL)
        assertEquals(LightingStyle.SOFT_LIGHT, preset.style)
    }

    // --- پوشش کامل: مقادیر بدون نگاشت اختصاصی از پیش‌فرض دسته‌شان استفاده می‌کنند ---
    // (Total، بدون null — طبق تصمیم مستند در ADR-039)

    @Test
    fun `a DARK-category mood without a dedicated mapping falls back to the DARK category default`() {
        // SCARY هیچ نگاشت اختصاصی ندارد اما هم‌دسته‌ی DARK با Mood.DARK است.
        assertEquals(LightingStyle.LOW_KEY, mapMoodToLighting(Mood.SCARY).style)
    }

    @Test
    fun `a HIGH_ENERGY-category mood falls back to the HIGH_ENERGY category default`() {
        assertEquals(LightingStyle.DRAMATIC_LIGHT, mapMoodToLighting(Mood.EPIC).style)
    }

    @Test
    fun `a POSITIVE-category mood falls back to the POSITIVE category default`() {
        assertEquals(LightingStyle.HIGH_KEY, mapMoodToLighting(Mood.HAPPY).style)
    }

    @Test
    fun `an EMOTIONAL-category mood falls back to the EMOTIONAL category default`() {
        assertEquals(LightingStyle.SOFT_LIGHT, mapMoodToLighting(Mood.MELANCHOLIC).style)
    }

    @Test
    fun `a CALM-category mood falls back to the CALM category default`() {
        assertEquals(LightingStyle.SOFT_LIGHT, mapMoodToLighting(Mood.SERENE).style)
    }
}
