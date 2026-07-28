package com.operaboys.cinemashotgenerator.domain.asset

import org.junit.Assert.assertEquals
import org.junit.Test

// واحد ۰۶ — تست‌های Migration بخش دوم (docs/adr/029-unit06-continuity-tiers-migration-part1.md):
// CharacterTier/defaultLockLevelForTier و تفکیک ObjectAsset/LocationAsset (Option A).

class AssetModelsTest {

    // --- defaultLockLevelForTier ---

    @Test
    fun `defaultLockLevelForTier maps MAIN to FULL`() {
        assertEquals(CharacterContinuityLevel.FULL, defaultLockLevelForTier(CharacterTier.MAIN))
    }

    @Test
    fun `defaultLockLevelForTier maps SECONDARY to MEDIUM`() {
        assertEquals(CharacterContinuityLevel.MEDIUM, defaultLockLevelForTier(CharacterTier.SECONDARY))
    }

    @Test
    fun `defaultLockLevelForTier maps BACKGROUND to NONE`() {
        assertEquals(CharacterContinuityLevel.NONE, defaultLockLevelForTier(CharacterTier.BACKGROUND))
    }

    // --- CharacterAsset.continuityLockLevel default expression ---

    private fun sampleCharacter(tier: CharacterTier) = CharacterAsset(
        assetId = "char_001",
        characterTier = tier,
        name = "Detective John",
        physicalAppearance = PhysicalAppearance(
            ageRange = "35-40",
            gender = Gender.MALE,
            height = "tall",
            build = "athletic",
            hair = Hair(color = "black", style = "short", length = "short"),
            facialFeatures = FacialFeatures(eyes = "brown")
        ),
        outfits = listOf(Outfit(id = "outfit_01", name = "Default", description = "black jacket", isDefault = true))
    )

    @Test
    fun `CharacterAsset continuityLockLevel defaults from characterTier when not overridden`() {
        assertEquals(CharacterContinuityLevel.FULL, sampleCharacter(CharacterTier.MAIN).continuityLockLevel)
        assertEquals(CharacterContinuityLevel.MEDIUM, sampleCharacter(CharacterTier.SECONDARY).continuityLockLevel)
        assertEquals(CharacterContinuityLevel.NONE, sampleCharacter(CharacterTier.BACKGROUND).continuityLockLevel)
    }

    @Test
    fun `CharacterAsset continuityLockLevel can be explicitly overridden regardless of tier`() {
        val character = sampleCharacter(CharacterTier.SECONDARY).copy(continuityLockLevel = CharacterContinuityLevel.FULL)
        assertEquals(CharacterContinuityLevel.FULL, character.continuityLockLevel)
    }

    // --- تفکیک ObjectAsset/LocationAsset (Option A) ---

    @Test
    fun `LocationAsset no longer carries any Object-specific field (pure location, Option A)`() {
        val location = LocationAsset(
            assetId = "loc_001",
            name = "Detective's Office",
            description = "a dimly lit office",
            environment = Environment(type = "indoor", size = "small", lightingCondition = "dim")
        )
        assertEquals(LocationContinuityLevel.STYLE, location.continuityLockLevel)
    }

    @Test
    fun `ObjectAsset constructs independently with its own dedicated fields`() {
        val obj = ObjectAsset(
            assetId = "obj_001",
            name = "Service Pistol",
            description = "a worn revolver",
            subtype = ObjectSubtype.PERSONAL_PROP,
            size = "small",
            materialAndColor = "worn black metal",
            specialTrait = "engraved initials"
        )
        assertEquals(ObjectSubtype.PERSONAL_PROP, obj.subtype)
        assertEquals(PropContinuityLevel.FORM, obj.continuityLockLevel)
    }

    // --- PhysicalAppearance.toPromptString() (docs/adr/031-unit06-physical-appearance-gender-migration.md) ---

    @Test
    fun `toPromptString includes every optional field when all are provided`() {
        val appearance = PhysicalAppearance(
            ageRange = "35-40",
            gender = Gender.FEMALE,
            height = "tall",
            build = "athletic",
            hair = Hair(color = "black", style = "short", length = "short"),
            physicalFeatures = "a small scar above the left eyebrow",
            facialFeatures = FacialFeatures(eyes = "brown", distinctiveMarks = listOf("scar on left cheek"))
        )
        val result = appearance.toPromptString()

        assertEquals(
            "35-40 female, tall, athletic build, short black hair, short style, brown eyes, " +
                "scar on left cheek, a small scar above the left eyebrow",
            result
        )
    }

    @Test
    fun `toPromptString only includes ageRange and gender when every optional field is absent`() {
        val appearance = PhysicalAppearance(ageRange = "20-25", gender = Gender.OTHER)
        assertEquals("20-25 other", appearance.toPromptString())
    }
}
