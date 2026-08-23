package com.operaboys.cinemashotgenerator.domain.promptengine

import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.Environment as AssetEnvironment
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.asset.PhysicalAppearance
import com.operaboys.cinemashotgenerator.domain.asset.ReferenceImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// فیچر مستقل «آپلود عکس مرجع واقعی Asset» — زیرقدم ۳ از ۳، پایانی (ADR-139):
// تست مستقیم تابع خالص collectAssetImageReferences (AssetImageReferenceCollector.kt)،
// جدا از تست‌های یکپارچگی assemblePromptBlueprint در PromptAssemblyTest.kt.

class AssetImageReferenceCollectorTest {

    private fun sampleCharacter(referenceImages: List<ReferenceImage> = emptyList()) = CharacterAsset(
        assetId = "char_001",
        characterTier = CharacterTier.MAIN,
        name = "Detective John",
        physicalAppearance = PhysicalAppearance(ageRange = "35-40", gender = Gender.MALE),
        outfits = emptyList(),
        referenceImages = referenceImages
    )

    private fun sampleObject(referenceImages: List<ReferenceImage> = emptyList()) = ObjectAsset(
        assetId = "obj_001",
        name = "Service Pistol",
        description = "توضیح نمونه",
        subtype = ObjectSubtype.PERSONAL_PROP,
        size = "small",
        materialAndColor = "worn black metal",
        referenceImages = referenceImages
    )

    private fun sampleLocation(referenceImages: List<ReferenceImage> = emptyList()) = LocationAsset(
        assetId = "loc_001",
        name = "دفتر کارآگاه",
        description = "توضیح نمونه",
        environment = AssetEnvironment(type = "indoor", size = "small", lightingCondition = "dim"),
        referenceImages = referenceImages
    )

    @Test
    fun `with no referenceImages on any asset, returns an empty list`() {
        val result = collectAssetImageReferences(
            characters = listOf(sampleCharacter()),
            objects = listOf(sampleObject()),
            locations = listOf(sampleLocation())
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `maps every referenceImage of every character to an ImageReference of type character`() {
        val result = collectAssetImageReferences(
            characters = listOf(
                sampleCharacter(
                    referenceImages = listOf(
                        ReferenceImage(localFilePath = "/storage/char_a.jpg", description = "desc a"),
                        ReferenceImage(localFilePath = "/storage/char_b.jpg", description = "desc b")
                    )
                )
            ),
            objects = emptyList(),
            locations = emptyList()
        )

        assertEquals(2, result.size)
        assertTrue(result.all { it.type == "character" })
        assertEquals(setOf("/storage/char_a.jpg", "/storage/char_b.jpg"), result.map { it.localFilePath }.toSet())
        assertEquals("desc a", result.single { it.localFilePath == "/storage/char_a.jpg" }.description)
    }

    @Test
    fun `maps a location's referenceImage to an ImageReference of type composition`() {
        val result = collectAssetImageReferences(
            characters = emptyList(),
            objects = emptyList(),
            locations = listOf(sampleLocation(referenceImages = listOf(ReferenceImage(localFilePath = "/storage/loc_a.jpg", description = "desc"))))
        )

        assertEquals(1, result.size)
        assertEquals("composition", result.single().type)
    }

    @Test
    fun `maps an object's referenceImage to an ImageReference of type composition`() {
        val result = collectAssetImageReferences(
            characters = emptyList(),
            objects = listOf(sampleObject(referenceImages = listOf(ReferenceImage(localFilePath = "/storage/obj_a.jpg", description = "desc")))),
            locations = emptyList()
        )

        assertEquals(1, result.size)
        assertEquals("composition", result.single().type)
    }

    @Test
    fun `combines referenceImages across multiple characters, objects, and locations`() {
        val result = collectAssetImageReferences(
            characters = listOf(
                sampleCharacter(referenceImages = listOf(ReferenceImage("/storage/char_1.jpg", "d1"))),
                sampleCharacter(referenceImages = listOf(ReferenceImage("/storage/char_2.jpg", "d2")))
            ),
            objects = listOf(sampleObject(referenceImages = listOf(ReferenceImage("/storage/obj_1.jpg", "d3")))),
            locations = listOf(sampleLocation(referenceImages = listOf(ReferenceImage("/storage/loc_1.jpg", "d4"))))
        )

        assertEquals(4, result.size)
        assertEquals(
            setOf("/storage/char_1.jpg", "/storage/char_2.jpg", "/storage/obj_1.jpg", "/storage/loc_1.jpg"),
            result.map { it.localFilePath }.toSet()
        )
    }
}
