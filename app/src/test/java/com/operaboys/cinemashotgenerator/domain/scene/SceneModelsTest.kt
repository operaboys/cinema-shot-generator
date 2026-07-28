package com.operaboys.cinemashotgenerator.domain.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// واحد ۰۴ — تست‌های Migration Scene.locationAssetId (رفع یافته‌ی F2 ممیزی pre-Unit 16،
// docs/adr/038-unit04-scene-location-asset-link.md).

class SceneModelsTest {

    private fun baseScene(locationAssetId: String? = null) = Scene(
        sceneId = "scene_001",
        sceneNumber = 1,
        narrativeRole = NarrativeRole.CLIMAX,
        location = SceneLocation(type = LocationType.OUTDOOR, description = "خیابان شلوغ شهری، شب"),
        locationAssetId = locationAssetId,
        timeOfDay = TimeOfDay.NIGHT,
        atmospherePrimary = Atmosphere.TENSE
    )

    @Test
    fun `locationAssetId defaults to null when not provided (backward compatible)`() {
        val scene = baseScene()
        assertNull(scene.locationAssetId)
    }

    @Test
    fun `locationAssetId holds an explicit reference when the scene is linked to the asset library`() {
        val scene = baseScene(locationAssetId = "loc_office")
        assertEquals("loc_office", scene.locationAssetId)
    }

    @Test
    fun `SceneLocation description remains independent of locationAssetId`() {
        // طبق تصمیم بخش الف: locationAssetId در کنار SceneLocation موجود می‌ماند،
        // نه جایگزین آن — یک Scene می‌تواند locationAssetId داشته باشد و توصیف متنی
        // متفاوتی هم داشته باشد (یا برعکس، فقط توصیف متنی داشته باشد بدون Asset).
        val scene = baseScene(locationAssetId = "loc_office")
        assertEquals("خیابان شلوغ شهری، شب", scene.location.description)
    }
}
