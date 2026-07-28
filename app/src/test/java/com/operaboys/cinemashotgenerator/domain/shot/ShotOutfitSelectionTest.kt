package com.operaboys.cinemashotgenerator.domain.shot

import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.FacialFeatures
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.Hair
import com.operaboys.cinemashotgenerator.domain.asset.Outfit
import com.operaboys.cinemashotgenerator.domain.asset.OutfitCondition
import com.operaboys.cinemashotgenerator.domain.asset.PhysicalAppearance
import org.junit.Assert.assertEquals
import org.junit.Test

class ShotOutfitSelectionTest {

    private val character = CharacterAsset(
        assetId = "char_001",
        characterTier = CharacterTier.MAIN,
        name = "Detective John",
        physicalAppearance = PhysicalAppearance(
            ageRange = "35-40",
            gender = Gender.MALE,
            height = "tall",
            build = "athletic",
            hair = Hair(color = "black", style = "short", length = "short"),
            facialFeatures = FacialFeatures(eyes = "brown")
        ),
        outfits = listOf(
            Outfit(id = "outfit_01", name = "Default Look", description = "black leather jacket, jeans", isDefault = true),
            Outfit(
                id = "outfit_02",
                name = "Rain Coat",
                description = "long dark raincoat",
                isDefault = false,
                condition = OutfitCondition(weather = "rain")
            )
        )
    )

    @Test
    fun `manual override takes priority over condition and default`() {
        val result = selectOutfitForShot(character, sceneWeather = "rain", manualOverrideOutfitId = "outfit_01")
        assertEquals("outfit_01", result)
    }

    @Test
    fun `matching weather condition is selected when no manual override`() {
        val result = selectOutfitForShot(character, sceneWeather = "rain", manualOverrideOutfitId = null)
        assertEquals("outfit_02", result)
    }

    @Test
    fun `falls back to default when no condition matches`() {
        val result = selectOutfitForShot(character, sceneWeather = "sunny", manualOverrideOutfitId = null)
        assertEquals("outfit_01", result)
    }
}
