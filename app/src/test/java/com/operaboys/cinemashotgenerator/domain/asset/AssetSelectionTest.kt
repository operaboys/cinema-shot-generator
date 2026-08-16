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

    // --- تکمیل G5 (ADR-096): matching چندفیلدی (weather/timeOfDay/locationType) ---
    // outfit_02 و outfit_03 عمداً مقدار weather متفاوت دارند (rain در برابر storm)
    // تا هر تست بتواند رفتار یکی را بدون دخالت دیگری بسنجد.

    private val multiConditionOutfits = listOf(
        Outfit(id = "outfit_01", name = "Default Look", description = "black leather jacket, jeans", isDefault = true),
        Outfit(
            id = "outfit_02",
            name = "Rain Coat",
            description = "long dark raincoat",
            isDefault = false,
            condition = OutfitCondition(weather = "rain")
        ),
        Outfit(
            id = "outfit_03",
            name = "Storm Night Outdoor Gear",
            description = "tactical gear with flashlight",
            isDefault = false,
            condition = OutfitCondition(weather = "storm", timeOfDay = "night", locationType = "outdoor")
        )
    )

    @Test
    fun `an outfit whose condition only specifies weather matches on weather alone, ignoring timeOfDay and locationType`() {
        val result = selectOutfitForScene(multiConditionOutfits, sceneWeather = "rain", sceneTimeOfDay = "noon", sceneLocationType = "indoor")
        assertEquals("outfit_02", result.id)
    }

    @Test
    fun `an outfit whose condition specifies all three fields is selected only when all three match the scene`() {
        val result = selectOutfitForScene(multiConditionOutfits, sceneWeather = "storm", sceneTimeOfDay = "night", sceneLocationType = "outdoor")
        assertEquals("outfit_03", result.id)
    }

    @Test
    fun `an outfit requiring all three fields is not selected when only some of them match, and falls back to default`() {
        // weather=storm منطبق است، اما timeOfDay صحنه noon است نه night — طبق AND، outfit_03 رد می‌شود.
        // outfit_02 هم رد می‌شود چون weather صحنه‌اش storm است نه rain. نتیجه باید Default باشد.
        val result = selectOutfitForScene(multiConditionOutfits, sceneWeather = "storm", sceneTimeOfDay = "noon", sceneLocationType = "outdoor")
        assertEquals("outfit_01", result.id)
    }

    @Test
    fun `falls back to default when nothing matches any condition`() {
        val result = selectOutfitForScene(multiConditionOutfits, sceneWeather = "sunny", sceneTimeOfDay = "dawn", sceneLocationType = "mixed")
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
