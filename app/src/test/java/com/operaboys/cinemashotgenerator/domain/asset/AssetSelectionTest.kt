package com.operaboys.cinemashotgenerator.domain.asset

import org.junit.Assert.assertEquals
import org.junit.Test

class AssetSelectionTest {

    private val outfits = listOf(
        Outfit(id = "outfit_01", name = "Default Look", description = "black leather jacket, jeans", isDefault = true),
        Outfit(
            id = "outfit_02",
            name = "Rain Coat",
            description = "long dark raincoat",
            isDefault = false,
            condition = OutfitCondition(weather = "rain")
        )
    )

    private val expressions = listOf(
        Expression(id = "exp_calm", name = "Calm", description = "neutral face, steady gaze", emotion = "calm", isDefault = true),
        Expression(
            id = "exp_exhausted",
            name = "Exhausted",
            description = "breathing heavily",
            emotion = "tired",
            isDefault = false,
            condition = OutfitCondition(weather = "rain")
        )
    )

    // --- selectOutfitForScene ---

    @Test
    fun `manual override takes priority over condition and default`() {
        val result = selectOutfitForScene(outfits, sceneWeather = "rain", manualOverrideId = "outfit_01")
        assertEquals("outfit_01", result.id)
    }

    @Test
    fun `matching weather condition is selected when no manual override`() {
        val result = selectOutfitForScene(outfits, sceneWeather = "rain")
        assertEquals("outfit_02", result.id)
    }

    @Test
    fun `falls back to default when no condition matches`() {
        val result = selectOutfitForScene(outfits, sceneWeather = "sunny")
        assertEquals("outfit_01", result.id)
    }

    // --- selectExpressionForScene (همان منطق) ---

    @Test
    fun `expression manual override takes priority`() {
        val result = selectExpressionForScene(expressions, sceneWeather = "rain", manualOverrideId = "exp_calm")
        assertEquals("exp_calm", result.id)
    }

    @Test
    fun `expression matching condition is selected`() {
        val result = selectExpressionForScene(expressions, sceneWeather = "rain")
        assertEquals("exp_exhausted", result.id)
    }

    @Test
    fun `expression falls back to default`() {
        val result = selectExpressionForScene(expressions, sceneWeather = "sunny")
        assertEquals("exp_calm", result.id)
    }
}
