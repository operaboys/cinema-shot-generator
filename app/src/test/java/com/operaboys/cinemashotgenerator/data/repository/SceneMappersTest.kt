package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.SceneLocation
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    // یافته‌ی #۱۱ appendix ADR-081 (ADR-085): تست round-trip linkedAssetIds — هم‌الگو
    // با تست بالا برای locationAssetId.
    @Test
    fun `Scene toDto then toDomain round-trips linkedAssetIds exactly, both populated and empty`() {
        val withLinkedAssets = baseScene(locationAssetId = null).copy(linkedAssetIds = listOf("char_1", "loc_2", "obj_3"))
        assertEquals(withLinkedAssets, withLinkedAssets.toDto().toDomain())

        val withoutLinkedAssets = baseScene(locationAssetId = null)
        assertEquals(emptyList<String>(), withoutLinkedAssets.linkedAssetIds)
        assertEquals(withoutLinkedAssets, withoutLinkedAssets.toDto().toDomain())
    }

    /**
     * یافته‌ی #۱۱ appendix ADR-081 (ADR-085) — الزام صریح دستور کار: اثبات اینکه
     * داده‌ی JSON قدیمیِ ذخیره‌شده (پیش از این قدم، بدون کلید linkedAssetIds اصلاً)
     * بدون خطا Decode می‌شود و مقدار پیش‌فرض (لیست خالی) می‌گیرد — نه یک فرض
     * نظری درباره‌ی رفتار kotlinx.serialization، بلکه یک تست واقعی روی یک رشته‌ی
     * JSON دستی که عمداً این کلید را ندارد (دقیقاً شبیه یک ردیف واقعی Room که قبل
     * از این قدم ذخیره شده بود).
     */
    @Test
    fun `decoding an old SceneDataJson without the linkedAssetIds key succeeds with an empty default`() {
        val json = Json { ignoreUnknownKeys = true }
        val oldJsonWithoutLinkedAssetIds = """
            {
              "sceneId": "scene_legacy",
              "sceneNumber": 1,
              "narrativeRole": "DEVELOPMENT",
              "location": { "type": "OUTDOOR", "description": "a quiet street" },
              "timeOfDay": "NIGHT",
              "atmospherePrimary": "CALM"
            }
        """.trimIndent()

        val decoded = json.decodeFromString(SceneDto.serializer(), oldJsonWithoutLinkedAssetIds)

        assertTrue(decoded.linkedAssetIds.isEmpty())
        assertEquals("scene_legacy", decoded.sceneId)
    }

    // تکمیل Rule یتیم — قدم ۱ از ۴ (ADR-106): هم‌الگو دقیق با دو تست بالا برای
    // cinematicModeOverride تازه‌اضافه‌شده.

    @Test
    fun `Scene toDto then toDomain round-trips cinematicModeOverride exactly, both when set and when null`() {
        val withOverride = baseScene(locationAssetId = null).copy(cinematicModeOverride = CinematicMode.FAST_CUT)
        assertEquals(withOverride, withOverride.toDto().toDomain())

        val withoutOverride = baseScene(locationAssetId = null)
        assertEquals(null, withoutOverride.cinematicModeOverride)
        assertEquals(withoutOverride, withoutOverride.toDto().toDomain())
    }

    @Test
    fun `decoding an old SceneDataJson without the cinematicModeOverride key succeeds with a null default`() {
        val json = Json { ignoreUnknownKeys = true }
        val oldJsonWithoutCinematicModeOverride = """
            {
              "sceneId": "scene_legacy",
              "sceneNumber": 1,
              "narrativeRole": "DEVELOPMENT",
              "location": { "type": "OUTDOOR", "description": "a quiet street" },
              "timeOfDay": "NIGHT",
              "atmospherePrimary": "CALM"
            }
        """.trimIndent()

        val decoded = json.decodeFromString(SceneDto.serializer(), oldJsonWithoutCinematicModeOverride)

        assertEquals(null, decoded.cinematicModeOverride)
        assertEquals("scene_legacy", decoded.sceneId)
    }
}
