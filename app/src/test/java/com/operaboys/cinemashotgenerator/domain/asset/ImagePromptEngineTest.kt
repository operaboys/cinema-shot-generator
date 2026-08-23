package com.operaboys.cinemashotgenerator.domain.asset

import com.operaboys.cinemashotgenerator.domain.dna.AspectRatio
import com.operaboys.cinemashotgenerator.domain.dna.ColorTemperature
import com.operaboys.cinemashotgenerator.domain.dna.ContrastLevel
import com.operaboys.cinemashotgenerator.domain.dna.CoreIdentity
import com.operaboys.cinemashotgenerator.domain.dna.GlobalMoodBase
import com.operaboys.cinemashotgenerator.domain.dna.MasterPalette
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.dna.OutputConstraints
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.dna.RealismLevel
import com.operaboys.cinemashotgenerator.domain.dna.SaturationLevel
import com.operaboys.cinemashotgenerator.domain.dna.StyleConsistency
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import com.operaboys.cinemashotgenerator.domain.visualidentity.StyleInfluence
import org.junit.Assert.assertTrue
import org.junit.Test

// فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۲ از ۵ (ADR-132). این تست‌ها فقط
// اثبات می‌کنند هر تابع خروجی معنادار (شامل Style Tokens واقعی و فیلدهای اصلی
// همان Asset) تولید می‌کند — بدون هیچ ارتباط با موتور اصلی پرامپت ویدیو.

private fun neutralDna(
    dominantVisualStyle: VisualStyle = VisualStyle.CINEMATIC_STYLE,
    secondaryStyle: VisualStyle? = null,
    influence: StyleInfluence? = null
): ProjectDna = ProjectDna(
    dnaId = "dna_001",
    projectId = "proj_001",
    coreIdentity = CoreIdentity(
        dominantVisualStyle = dominantVisualStyle,
        realismLevel = RealismLevel.SEMI_REALISTIC,
        styleConsistency = StyleConsistency.MODERATE,
        secondaryStyle = secondaryStyle,
        influence = influence
    ),
    masterPalette = MasterPalette(ColorTemperature.NEUTRAL, SaturationLevel.MEDIUM, ContrastLevel.MEDIUM, ""),
    outputConstraints = OutputConstraints(emptyMap(), 10, AspectRatio.LANDSCAPE_16_9),
    globalMoodBase = GlobalMoodBase(Mood.CALM)
)

private fun neutralCharacter(): CharacterAsset = CharacterAsset(
    assetId = "char_001",
    characterTier = CharacterTier.MAIN,
    name = "Jane",
    physicalAppearance = PhysicalAppearance(ageRange = "30s", gender = Gender.FEMALE),
    outfits = emptyList()
)

class ImagePromptEngineTest {

    @Test
    fun `buildCharacterBaseImagePrompt includes style tokens, character name, and physical appearance`() {
        val character = neutralCharacter()
        val prompt = buildCharacterBaseImagePrompt(character, neutralDna())

        assertTrue(prompt.contains("cinematic style"))
        assertTrue(prompt.contains("Jane"))
        assertTrue(prompt.contains("30s female"))
        assertTrue(prompt.contains("neutral clothing"))
    }

    @Test
    fun `buildCharacterBaseImagePrompt includes secondary style influence when set`() {
        val dna = neutralDna(
            dominantVisualStyle = VisualStyle.CINEMATIC_STYLE,
            secondaryStyle = VisualStyle.FILM_NOIR,
            influence = StyleInfluence.STRONG
        )
        val prompt = buildCharacterBaseImagePrompt(neutralCharacter(), dna)

        assertTrue(prompt.contains("strongly influenced by"))
        assertTrue(prompt.contains("film noir"))
    }

    @Test
    fun `buildOutfitImagePrompt references the base character and describes the outfit`() {
        val character = neutralCharacter()
        val outfit = Outfit(id = "outfit_1", name = "Detective Coat", description = "a long tan trench coat", isDefault = true)

        val prompt = buildOutfitImagePrompt(outfit, character, neutralDna())

        assertTrue(prompt.contains("matching the established character reference exactly"))
        assertTrue(prompt.contains("Jane"))
        assertTrue(prompt.contains("Detective Coat"))
        assertTrue(prompt.contains("a long tan trench coat"))
    }

    @Test
    fun `buildOutfitImagePrompt appends condition details when present`() {
        val character = neutralCharacter()
        val outfit = Outfit(
            id = "outfit_1",
            name = "Rain Gear",
            description = "a yellow raincoat",
            isDefault = false,
            condition = OutfitCondition(weather = "rainy", timeOfDay = "night")
        )

        val prompt = buildOutfitImagePrompt(outfit, character, neutralDna())

        assertTrue(prompt.contains("rainy"))
        assertTrue(prompt.contains("night"))
    }

    @Test
    fun `buildLocationImagePrompt includes description, environment, and key elements`() {
        val location = LocationAsset(
            assetId = "loc_001",
            name = "Old Warehouse",
            description = "a derelict warehouse near the docks",
            environment = Environment(type = "indoor", size = "large", lightingCondition = "dim"),
            keyElements = listOf("rusted crates", "broken skylight")
        )

        val prompt = buildLocationImagePrompt(location, neutralDna())

        assertTrue(prompt.contains("a derelict warehouse near the docks"))
        assertTrue(prompt.contains("indoor, large, dim"))
        assertTrue(prompt.contains("rusted crates"))
    }

    @Test
    fun `buildObjectImagePrompt includes description, size, material, and special trait`() {
        val objectAsset = ObjectAsset(
            assetId = "obj_001",
            name = "Ancient Locket",
            description = "a small heart-shaped locket",
            subtype = ObjectSubtype.PERSONAL_PROP,
            size = "small",
            materialAndColor = "tarnished silver",
            specialTrait = "engraved with a faded rose"
        )

        val prompt = buildObjectImagePrompt(objectAsset, neutralDna())

        assertTrue(prompt.contains("a small heart-shaped locket"))
        assertTrue(prompt.contains("small, tarnished silver"))
        assertTrue(prompt.contains("engraved with a faded rose"))
    }
}
