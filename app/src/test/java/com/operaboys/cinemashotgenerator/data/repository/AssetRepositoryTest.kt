package com.operaboys.cinemashotgenerator.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.ContinuityRules
import com.operaboys.cinemashotgenerator.domain.asset.Environment
import com.operaboys.cinemashotgenerator.domain.asset.Expression
import com.operaboys.cinemashotgenerator.domain.asset.FacialFeatures
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.Hair
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.asset.Outfit
import com.operaboys.cinemashotgenerator.domain.asset.OutfitCondition
import com.operaboys.cinemashotgenerator.domain.asset.PhysicalAppearance
import com.operaboys.cinemashotgenerator.domain.asset.Prop
import com.operaboys.cinemashotgenerator.domain.asset.ReferenceImage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// واحد ۱۵ — قدم ۳ (زیرقدم ۱): تست end-to-end round-trip واقعی برای
// CharacterAsset/LocationAsset.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AssetRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: AssetRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = AssetRepository(database.assetDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val fullCharacter = CharacterAsset(
        assetId = "char_001",
        characterTier = CharacterTier.MAIN,
        name = "Detective John",
        physicalAppearance = PhysicalAppearance(
            ageRange = "35-40",
            gender = Gender.MALE,
            height = "tall",
            build = "athletic",
            hair = Hair(color = "black", style = "short", length = "short"),
            facialFeatures = FacialFeatures(eyes = "brown", distinctiveMarks = listOf("scar on left cheek"))
        ),
        outfits = listOf(
            Outfit("outfit_01", "Default Look", "black leather jacket, jeans", isDefault = true),
            Outfit(
                "outfit_02", "Rain Coat", "long dark raincoat", isDefault = false,
                condition = OutfitCondition(weather = "rain", timeOfDay = "night", locationType = "outdoor")
            )
        ),
        expressions = listOf(
            Expression("expr_01", "Neutral", "calm face", emotion = "neutral", isDefault = true),
            Expression(
                "expr_02", "Angry", "furrowed brow", emotion = "anger", isDefault = false,
                condition = OutfitCondition(weather = null, timeOfDay = "night", locationType = null)
            )
        ),
        props = listOf(Prop("prop_01", "Revolver", "service revolver", category = "weapon")),
        continuityRules = ContinuityRules(
            identityLock = true, appearanceLock = true, ageLock = true, antiDrift = true,
            allowedOverrides = listOf("emotion", "outfit")
        ),
        referenceImages = listOf(ReferenceImage("/storage/char_001_ref.jpg", "front-facing reference"))
    )

    private val fullLocation = LocationAsset(
        assetId = "loc_001",
        name = "Detective's Office",
        description = "A dimly lit office with rain-streaked windows",
        environment = Environment(type = "indoor", size = "small", lightingCondition = "dim"),
        timeCompatibility = listOf("night", "dusk"),
        weatherCompatibility = listOf("rain", "storm"),
        keyElements = listOf("desk", "window", "filing cabinet"),
        basePrompt = "a dimly lit detective's office"
    )

    private val fullObject = ObjectAsset(
        assetId = "obj_001",
        name = "Service Pistol",
        description = "a worn service revolver",
        subtype = ObjectSubtype.PERSONAL_PROP,
        size = "small",
        materialAndColor = "worn black metal",
        specialTrait = "engraved initials",
        basePrompt = "a worn revolver with engraved initials"
    )

    @Test
    fun `saveCharacterAsset then loadCharacterAssets round-trips the full structure exactly`() = runBlocking {
        val result = repository.saveCharacterAsset("proj_001", fullCharacter)
        assertTrue(result.isSuccess)

        val loaded = repository.loadCharacterAssets(listOf("char_001"))
        assertTrue(loaded.isSuccess)
        assertEquals(listOf(fullCharacter), loaded.getOrThrow())
    }

    @Test
    fun `saveLocationAsset then loadLocationAssets round-trips the full structure exactly`() = runBlocking {
        val result = repository.saveLocationAsset("proj_001", fullLocation)
        assertTrue(result.isSuccess)

        val loaded = repository.loadLocationAssets(listOf("loc_001"))
        assertTrue(loaded.isSuccess)
        assertEquals(listOf(fullLocation), loaded.getOrThrow())
    }

    @Test
    fun `saveObjectAsset then loadObjectAssets round-trips the full structure exactly`() = runBlocking {
        val result = repository.saveObjectAsset("proj_001", fullObject)
        assertTrue(result.isSuccess)

        val loaded = repository.loadObjectAssets(listOf("obj_001"))
        assertTrue(loaded.isSuccess)
        assertEquals(listOf(fullObject), loaded.getOrThrow())
    }

    @Test
    fun `loadLocationAssets skips ids that are not locations, such as an object`() = runBlocking {
        repository.saveObjectAsset("proj_001", fullObject)
        val loaded = repository.loadLocationAssets(listOf("obj_001"))
        assertTrue(loaded.isSuccess)
        assertTrue(loaded.getOrThrow().isEmpty())
    }

    @Test
    fun `loadCharacterAssets skips ids that do not exist or are not characters`() = runBlocking {
        repository.saveLocationAsset("proj_001", fullLocation)
        val loaded = repository.loadCharacterAssets(listOf("loc_001", "char_missing"))
        assertTrue(loaded.isSuccess)
        assertTrue(loaded.getOrThrow().isEmpty())
    }
}
