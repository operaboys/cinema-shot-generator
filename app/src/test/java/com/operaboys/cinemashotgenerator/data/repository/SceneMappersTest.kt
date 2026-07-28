package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.SceneLocation
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Test

// واحد ۰۴ — تست round-trip برای Scene.locationAssetId
// (docs/adr/038-unit04-scene-location-asset-link.md، رفع F2 ممیزی pre-Unit 16):
// بدون این نگاشت، locationAssetId انتخاب‌شده در فرم «تنظیمات Scene» واحد ۱۶ حین
// ذخیره/بارگذاری Room بی‌صدا گم می‌شد — دقیقاً همان الگوی باگی که ADR-036 پیدا کرد.

class SceneMappersTest {

    private fun baseScene(locationAssetId: String?) = Scene(
        sceneId = "scene_001",
        sceneNumber = 1,
        narrativeRole = NarrativeRole.CLIMAX,
        location = SceneLocation(type = LocationType.OUTDOOR, description = "خیابان شلوغ شهری، شب"),
        locationAssetId = locationAssetId,
        timeOfDay = TimeOfDay.NIGHT,
        atmospherePrimary = Atmosphere.TENSE
    )

    @Test
    fun `Scene toDto then toDomain round-trips locationAssetId exactly, both when set and when null`() {
        val withLocationAssetId = baseScene(locationAssetId = "loc_office")
        assertEquals(withLocationAssetId, withLocationAssetId.toDto().toDomain())

        val withoutLocationAssetId = baseScene(locationAssetId = null)
        assertEquals(withoutLocationAssetId, withoutLocationAssetId.toDto().toDomain())
    }
}
