package com.operaboys.cinemashotgenerator.domain.storybreakdown

import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryToDomainMapperTest {

    // --- mapAiCharacterToAsset ---

    @Test
    fun `mapAiCharacterToAsset maps main role to MAIN tier`() {
        val asset = mapAiCharacterToAsset(SimpleCharacterFromAi(name = "John", description = "a detective", role = "main"))
        assertEquals(CharacterTier.MAIN, asset.characterTier)
    }

    @Test
    fun `mapAiCharacterToAsset maps background role to BACKGROUND tier`() {
        val asset = mapAiCharacterToAsset(SimpleCharacterFromAi(name = "Passerby", description = "a passerby", role = "background"))
        assertEquals(CharacterTier.BACKGROUND, asset.characterTier)
    }

    @Test
    fun `mapAiCharacterToAsset maps an unexpected role to SECONDARY tier as a safe default`() {
        val asset = mapAiCharacterToAsset(SimpleCharacterFromAi(name = "X", description = "y", role = "sidekick"))
        assertEquals(CharacterTier.SECONDARY, asset.characterTier)
    }

    @Test
    fun `mapAiCharacterToAsset maps role case-insensitively`() {
        val asset = mapAiCharacterToAsset(SimpleCharacterFromAi(name = "John", description = "a detective", role = "MAIN"))
        assertEquals(CharacterTier.MAIN, asset.characterTier)
    }

    @Test
    fun `mapAiCharacterToAsset maps female and male gender correctly`() {
        val female = mapAiCharacterToAsset(SimpleCharacterFromAi(name = "A", description = "d", role = "main", gender = "female"))
        val male = mapAiCharacterToAsset(SimpleCharacterFromAi(name = "B", description = "d", role = "main", gender = "male"))
        assertEquals(Gender.FEMALE, female.physicalAppearance.gender)
        assertEquals(Gender.MALE, male.physicalAppearance.gender)
    }

    @Test
    fun `mapAiCharacterToAsset defaults to OTHER gender when absent or unrecognized`() {
        val absent = mapAiCharacterToAsset(SimpleCharacterFromAi(name = "A", description = "d", role = "main", gender = null))
        val unrecognized = mapAiCharacterToAsset(SimpleCharacterFromAi(name = "B", description = "d", role = "main", gender = "unknown"))
        assertEquals(Gender.OTHER, absent.physicalAppearance.gender)
        assertEquals(Gender.OTHER, unrecognized.physicalAppearance.gender)
    }

    @Test
    fun `mapAiCharacterToAsset always has exactly one default outfit satisfying Rule 5`() {
        val asset = mapAiCharacterToAsset(SimpleCharacterFromAi(name = "John", description = "a detective", role = "main"))
        assertEquals(1, asset.outfits.size)
        assertTrue(asset.outfits[0].isDefault)
    }

    @Test
    fun `mapAiCharacterToAsset preserves the AI description in basePrompt and physicalFeatures`() {
        val asset = mapAiCharacterToAsset(SimpleCharacterFromAi(name = "John", description = "a tall detective", role = "main"))
        assertEquals("a tall detective", asset.basePrompt)
        assertEquals("a tall detective", asset.physicalAppearance.physicalFeatures)
    }

    // سیستم Preview دوزبانه‌ی پرامپت — قدم ۲ از ۳ زیرقدم (ADR-122):
    // descriptionFaPreview باید از SimpleCharacterFromAi.descriptionFa بیاید،
    // basePrompt/physicalFeatures همچنان همان description انگلیسی بمانند —
    // نه فارسی، نه ترکیبی.

    @Test
    fun `mapAiCharacterToAsset carries descriptionFa into descriptionFaPreview, leaving basePrompt English`() {
        val asset = mapAiCharacterToAsset(
            SimpleCharacterFromAi(name = "John", description = "a tall detective", role = "main", descriptionFa = "یک کارآگاه بلندقد")
        )
        assertEquals("a tall detective", asset.basePrompt)
        assertEquals("یک کارآگاه بلندقد", asset.descriptionFaPreview)
    }

    @Test
    fun `mapAiCharacterToAsset leaves descriptionFaPreview null when descriptionFa is absent`() {
        val asset = mapAiCharacterToAsset(SimpleCharacterFromAi(name = "John", description = "a tall detective", role = "main"))
        assertNull(asset.descriptionFaPreview)
    }

    // --- mapAiLocationToAsset / mapAiObjectToAsset ---

    @Test
    fun `mapAiLocationToAsset maps name, description, and basePrompt`() {
        val asset = mapAiLocationToAsset(SimpleLocationFromAi(name = "Office", description = "a dim office"))
        assertEquals("Office", asset.name)
        assertEquals("a dim office", asset.description)
        assertEquals("a dim office", asset.basePrompt)
    }

    @Test
    fun `mapAiLocationToAsset carries descriptionFa into descriptionFaPreview, leaving description and basePrompt English`() {
        val asset = mapAiLocationToAsset(
            SimpleLocationFromAi(name = "Office", description = "a dim office", descriptionFa = "یک دفتر کم‌نور")
        )
        assertEquals("a dim office", asset.description)
        assertEquals("a dim office", asset.basePrompt)
        assertEquals("یک دفتر کم‌نور", asset.descriptionFaPreview)
    }

    @Test
    fun `mapAiObjectToAsset defaults to GENERAL_PROP subtype`() {
        val asset = mapAiObjectToAsset(SimpleObjectFromAi(name = "Gun", description = "a revolver"))
        assertEquals(ObjectSubtype.GENERAL_PROP, asset.subtype)
        assertEquals("a revolver", asset.basePrompt)
    }

    @Test
    fun `mapAiObjectToAsset carries descriptionFa into descriptionFaPreview, leaving description and basePrompt English`() {
        val asset = mapAiObjectToAsset(
            SimpleObjectFromAi(name = "Gun", description = "a revolver", descriptionFa = "یک هفت‌تیر")
        )
        assertEquals("a revolver", asset.description)
        assertEquals("a revolver", asset.basePrompt)
        assertEquals("یک هفت‌تیر", asset.descriptionFaPreview)
    }

    // --- groupAiShotsIntoScenes ---

    @Test
    fun `groupAiShotsIntoScenes groups shots with the same sceneName into a single Scene`() {
        val shots = listOf(
            SimpleShotFromAi(sceneName = "Intro", shotNumber = 1, description = "d1", locationName = "Office"),
            SimpleShotFromAi(sceneName = "Intro", shotNumber = 2, description = "d2", locationName = "Office")
        )
        val scenes = groupAiShotsIntoScenes(shots)
        assertEquals(1, scenes.size)
        assertEquals(2, scenes[0].shotCount)
        assertEquals("Intro", scenes[0].sceneTitle)
    }

    @Test
    fun `groupAiShotsIntoScenes creates separate scenes for different sceneNames in first-occurrence order`() {
        val shots = listOf(
            SimpleShotFromAi(sceneName = "Intro", shotNumber = 1, description = "d1", locationName = "Office"),
            SimpleShotFromAi(sceneName = "Climax", shotNumber = 2, description = "d2", locationName = "Street")
        )
        val scenes = groupAiShotsIntoScenes(shots)
        assertEquals(2, scenes.size)
        val introScene = scenes.first { it.sceneTitle == "Intro" }
        val climaxScene = scenes.first { it.sceneTitle == "Climax" }
        assertEquals(1, introScene.sceneNumber)
        assertEquals(2, climaxScene.sceneNumber)
        assertEquals(LocationType.CUSTOM, introScene.location.type)
    }

    // --- mapAiShotToShot ---

    @Test
    fun `mapAiShotToShot matches character, location, and object names successfully`() {
        val character = mapAiCharacterToAsset(SimpleCharacterFromAi(name = "John", description = "d", role = "main"))
        val location = mapAiLocationToAsset(SimpleLocationFromAi(name = "Office", description = "d"))
        val obj = mapAiObjectToAsset(SimpleObjectFromAi(name = "Gun", description = "d"))
        val aiShot = SimpleShotFromAi(
            sceneName = "Intro", shotNumber = 1, description = "John enters",
            characterNames = listOf("John"), locationName = "Office", objectNames = listOf("Gun")
        )

        val mapping = mapAiShotToShot(
            aiShot, sceneId = "scene_001",
            characterAssetsByName = mapOf("John" to character),
            locationAssetsByName = mapOf("Office" to location),
            objectAssetsByName = mapOf("Gun" to obj)
        )

        assertTrue(mapping.unmatchedNames.isEmpty())
        assertEquals(listOf(character.assetId), mapping.shot.characterIds)
        assertEquals(listOf(location.assetId), mapping.shot.locationIds)
        assertEquals(listOf(obj.assetId), mapping.shot.objectIds)
    }

    @Test
    fun `mapAiShotToShot reports unmatched names instead of silently dropping them`() {
        val aiShot = SimpleShotFromAi(
            sceneName = "Intro", shotNumber = 1, description = "Unknown enters",
            characterNames = listOf("Unknown"), locationName = "MissingLocation", objectNames = listOf("MissingObj")
        )

        val mapping = mapAiShotToShot(
            aiShot, sceneId = "scene_001",
            characterAssetsByName = emptyMap(),
            locationAssetsByName = emptyMap(),
            objectAssetsByName = emptyMap()
        )

        assertEquals(setOf("Unknown", "MissingObj", "MissingLocation"), mapping.unmatchedNames.toSet())
        assertTrue(mapping.shot.characterIds.isEmpty())
        assertTrue(mapping.shot.locationIds.isEmpty())
        assertTrue(mapping.shot.objectIds.isEmpty())
    }

    @Test
    fun `mapAiShotToShot carries descriptionFa into shotDescriptionFaPreview, leaving shotDescription English`() {
        val aiShot = SimpleShotFromAi(
            sceneName = "Intro", shotNumber = 1, description = "John enters",
            locationName = "Office", descriptionFa = "جان وارد می‌شود"
        )

        val mapping = mapAiShotToShot(
            aiShot, sceneId = "scene_001",
            characterAssetsByName = emptyMap(), locationAssetsByName = emptyMap(), objectAssetsByName = emptyMap()
        )

        assertEquals("John enters", mapping.shot.shotDescription)
        assertEquals("جان وارد می‌شود", mapping.shot.shotDescriptionFaPreview)
    }

    // --- Rule 9 ---

    @Test
    fun `validateAllAssetNamesMatched is null when nothing is unmatched`() {
        assertNull(validateAllAssetNamesMatched(emptyList()))
    }

    @Test
    fun `validateAllAssetNamesMatched warns when names are unmatched`() {
        val issue = validateAllAssetNamesMatched(listOf("Unknown"))
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    // --- Rule 10 ---

    @Test
    fun `validateShotCountMatchesTarget is null within the 20 percent tolerance`() {
        assertNull(validateShotCountMatchesTarget(actualShotCount = 11, targetShotCount = 10))
        assertNull(validateShotCountMatchesTarget(actualShotCount = 9, targetShotCount = 10))
    }

    @Test
    fun `validateShotCountMatchesTarget warns beyond the 20 percent tolerance`() {
        val issue = validateShotCountMatchesTarget(actualShotCount = 15, targetShotCount = 10)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    // --- processAiResponse: end-to-end ---

    private val validAiJson = """
        {"characters": [{"name": "John", "description": "a detective", "role": "main", "gender": "male"}],
         "locations": [{"name": "Office", "description": "a dim office"}],
         "objects": [{"name": "Gun", "description": "a revolver"}],
         "shots": [{"sceneName": "Intro", "shotNumber": 1, "description": "John enters", "characterNames": ["John"], "locationName": "Office", "objectNames": ["Gun"]}]}
    """.trimIndent()

    @Test
    fun `processAiResponse succeeds end-to-end for a fully valid JSON response`() {
        val result = processAiResponse(listOf(validAiJson), targetShotCount = 1)
        assertTrue(result is ProcessAiResponseResult.Success)
        val breakdown = (result as ProcessAiResponseResult.Success).result

        assertEquals(1, breakdown.characters.size)
        assertEquals("John", breakdown.characters[0].name)
        assertEquals(1, breakdown.locations.size)
        assertEquals(1, breakdown.objects.size)
        assertEquals(1, breakdown.scenes.size)
        assertEquals(1, breakdown.shots.size)
        assertTrue(breakdown.warnings.isEmpty())
    }

    @Test
    fun `processAiResponse recovers from a repairable JSON error (trailing comma) via JSON Doctor`() {
        val trailingCommaJson = """
            {"characters": [{"name": "John", "description": "a detective", "role": "main", "gender": "male"}],
             "locations": [{"name": "Office", "description": "a dim office"}],
             "objects": [],
             "shots": [{"sceneName": "Intro", "shotNumber": 1, "description": "John enters", "characterNames": ["John"], "locationName": "Office", "objectNames": []}],}
        """.trimIndent()

        val result = processAiResponse(listOf(trailingCommaJson), targetShotCount = 1)
        assertTrue(result is ProcessAiResponseResult.Success)
        val breakdown = (result as ProcessAiResponseResult.Success).result
        assertEquals(1, breakdown.shots.size)
    }

    @Test
    fun `processAiResponse returns NeedsManualRepair for a genuinely broken JSON response`() {
        val brokenJson = """{"characters": [{"name": "John"}], "locations": [], "shots": []"""
        val result = processAiResponse(listOf(brokenJson), targetShotCount = 1)
        assertTrue(result is ProcessAiResponseResult.NeedsManualRepair)
    }

    @Test
    fun `processAiResponse returns MissingRequiredKeys when shots key is absent from otherwise valid JSON`() {
        val missingShotsJson = """{"characters": [], "locations": []}"""
        val result = processAiResponse(listOf(missingShotsJson), targetShotCount = 1)
        assertTrue(result is ProcessAiResponseResult.MissingRequiredKeys)
    }

    // سیستم Preview دوزبانه‌ی پرامپت — قدم ۲ از ۳ زیرقدم (ADR-122):
    // previewLanguageEnabled پیش‌فرض false دارد — تست‌های end-to-end بالا
    // (بدون هیچ تغییری، همین امضای قدیمی processAiResponse(chunks, targetShotCount)
    // را صدا می‌زنند) دقیقاً همان اثبات رگرسیون صفر مطلوب دستور کار هستند:
    // اگر previewLanguageEnabled به یک مقدار پیش‌فرض اشتباه (مثلاً true) تغییر
    // کرده بود، این تست‌ها روی JSON تک‌زبانه‌ی موجودشان با خطای Decode شکست
    // می‌خوردند.

    private val bilingualAiJson = """
        {"characters": [{"name": "John", "descriptionEn": "a detective", "descriptionFa": "یک کارآگاه", "role": "main", "gender": "male"}],
         "locations": [{"name": "Office", "descriptionEn": "a dim office", "descriptionFa": "یک دفتر کم‌نور"}],
         "objects": [{"name": "Gun", "descriptionEn": "a revolver", "descriptionFa": "یک هفت‌تیر"}],
         "shots": [{"sceneName": "Intro", "shotNumber": 1, "descriptionEn": "John enters", "descriptionFa": "جان وارد می‌شود", "characterNames": ["John"], "locationName": "Office", "objectNames": ["Gun"]}]}
    """.trimIndent()

    @Test
    fun `processAiResponse with previewLanguageEnabled=true parses the bilingual schema, keeping English as the real field and Farsi as descriptionFaPreview`() {
        val result = processAiResponse(listOf(bilingualAiJson), targetShotCount = 1, previewLanguageEnabled = true)
        assertTrue(result is ProcessAiResponseResult.Success)
        val breakdown = (result as ProcessAiResponseResult.Success).result

        val character = breakdown.characters.single()
        assertEquals("a detective", character.basePrompt)
        assertEquals("یک کارآگاه", character.descriptionFaPreview)

        val location = breakdown.locations.single()
        assertEquals("a dim office", location.description)
        assertEquals("a dim office", location.basePrompt)
        assertEquals("یک دفتر کم‌نور", location.descriptionFaPreview)

        val obj = breakdown.objects.single()
        assertEquals("a revolver", obj.description)
        assertEquals("a revolver", obj.basePrompt)
        assertEquals("یک هفت‌تیر", obj.descriptionFaPreview)

        val shot = breakdown.shots.single()
        assertEquals("John enters", shot.shotDescription)
        assertEquals("جان وارد می‌شود", shot.shotDescriptionFaPreview)
    }

    @Test
    fun `processAiResponse with previewLanguageEnabled=false (default) still succeeds on the existing single-language JSON, proving zero regression`() {
        val result = processAiResponse(listOf(validAiJson), targetShotCount = 1)
        assertTrue(result is ProcessAiResponseResult.Success)
        val breakdown = (result as ProcessAiResponseResult.Success).result
        assertNull(breakdown.characters.single().descriptionFaPreview)
        assertNull(breakdown.locations.single().descriptionFaPreview)
        assertNull(breakdown.objects.single().descriptionFaPreview)
        assertNull(breakdown.shots.single().shotDescriptionFaPreview)
    }
}
