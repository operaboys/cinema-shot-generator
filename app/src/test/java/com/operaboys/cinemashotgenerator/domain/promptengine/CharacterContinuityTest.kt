package com.operaboys.cinemashotgenerator.domain.promptengine

import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.FacialFeatures
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.Hair
import com.operaboys.cinemashotgenerator.domain.asset.Outfit
import com.operaboys.cinemashotgenerator.domain.asset.OutfitCondition
import com.operaboys.cinemashotgenerator.domain.asset.PhysicalAppearance
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.SceneLocation
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterContinuityTest {

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

    private val scene = Scene(
        sceneId = "scene_001",
        sceneNumber = 1,
        narrativeRole = NarrativeRole.CLIMAX,
        location = SceneLocation(type = LocationType.OUTDOOR, description = "خیابان شلوغ شهری، شب"),
        timeOfDay = TimeOfDay.NIGHT,
        atmospherePrimary = Atmosphere.TENSE
    )

    // NOTE: character.outfitOverride و manualOverrideId هیچ منبع داده‌ای در پروژه
    // ندارند (تأییدشده، ADR-012) — همیشه null هستند. تنوع رفتاری قابل‌تست واقعی همان
    // انتخاب شرطی بر اساس sceneWeather است (مثل selectOutfitForScene خودش).

    @Test
    fun `matching scene weather selects the conditional outfit description`() {
        val result = enforceCharacterContinuity(listOf(character), scene, sceneWeather = "rain")
        assertTrue(result[0].contains("long dark raincoat"))
    }

    @Test
    fun `no matching scene weather falls back to default outfit description`() {
        val result = enforceCharacterContinuity(listOf(character), scene, sceneWeather = "sunny")
        assertTrue(result[0].contains("black leather jacket, jeans"))
    }

    @Test
    fun `description includes physical appearance fields`() {
        val result = enforceCharacterContinuity(listOf(character), scene, sceneWeather = null)
        assertTrue(result[0].contains("athletic build"))
        assertTrue(result[0].contains("brown eyes"))
    }
}
