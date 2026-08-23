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
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۲ از ۵ (ADR-132). تمام قوانین این
// فایل فقط Severity.WARNING تولید می‌کنند — هیچ‌کدام BLOCKING نیست، چون این
// پرامپت هرگز به موتور اصلی وارد نمی‌شود.

private fun neutralDna(dominantVisualStyle: VisualStyle = VisualStyle.CINEMATIC_STYLE): ProjectDna = ProjectDna(
    dnaId = "dna_001",
    projectId = "proj_001",
    coreIdentity = CoreIdentity(
        dominantVisualStyle = dominantVisualStyle,
        realismLevel = RealismLevel.SEMI_REALISTIC,
        styleConsistency = StyleConsistency.MODERATE
    ),
    masterPalette = MasterPalette(ColorTemperature.NEUTRAL, SaturationLevel.MEDIUM, ContrastLevel.MEDIUM, ""),
    outputConstraints = OutputConstraints(emptyMap(), 10, AspectRatio.LANDSCAPE_16_9),
    globalMoodBase = GlobalMoodBase(Mood.CALM)
)

private fun neutralCharacter(ageRange: String = "30s", basePrompt: String? = null): CharacterAsset = CharacterAsset(
    assetId = "char_001",
    characterTier = CharacterTier.MAIN,
    name = "Jane",
    physicalAppearance = PhysicalAppearance(ageRange = ageRange, gender = Gender.FEMALE),
    outfits = emptyList(),
    basePrompt = basePrompt
)

private val validPromptLength = "a".repeat(50)

class ImagePromptValidationTest {

    // --- طول پرامپت ---

    @Test
    fun `a too-short generated prompt produces a warning`() {
        val issues = validateCharacterImagePromptInputs(neutralCharacter(), neutralDna(), generatedPrompt = "short")
        assertTrue(issues.any { it.message.contains("کوتاه") })
        assertTrue(issues.all { it.severity == Severity.WARNING })
    }

    @Test
    fun `a too-long generated prompt produces a warning`() {
        val issues = validateCharacterImagePromptInputs(neutralCharacter(), neutralDna(), generatedPrompt = "a".repeat(900))
        assertTrue(issues.any { it.message.contains("بلند") })
    }

    @Test
    fun `a reasonably-sized generated prompt produces no length warning`() {
        val issues = validateCharacterImagePromptInputs(neutralCharacter(), neutralDna(), generatedPrompt = validPromptLength)
        assertTrue(issues.none { it.message.contains("کوتاه") || it.message.contains("بلند") })
    }

    // --- فیلد حداقلی خالی ---

    @Test
    fun `a blank ageRange on CharacterAsset produces a warning`() {
        val issues = validateCharacterImagePromptInputs(neutralCharacter(ageRange = "  "), neutralDna(), generatedPrompt = validPromptLength)
        assertTrue(issues.any { it.field == "physical_appearance" })
    }

    @Test
    fun `a blank description or environment type on LocationAsset produces a warning`() {
        val location = LocationAsset(
            assetId = "loc_001",
            name = "Empty Place",
            description = "",
            environment = Environment(type = "indoor", size = "large", lightingCondition = "dim")
        )
        val issues = validateLocationImagePromptInputs(location, neutralDna(), generatedPrompt = validPromptLength)
        assertTrue(issues.any { it.field == "description" })
    }

    @Test
    fun `a blank materialAndColor on ObjectAsset produces a warning`() {
        val objectAsset = ObjectAsset(
            assetId = "obj_001",
            name = "Mystery Object",
            description = "a strange item",
            subtype = ObjectSubtype.GENERAL_PROP,
            size = "small",
            materialAndColor = "  "
        )
        val issues = validateObjectImagePromptInputs(objectAsset, neutralDna(), generatedPrompt = validPromptLength)
        assertTrue(issues.any { it.field == "material_and_color" })
    }

    // --- تناقض کلیدواژه‌ای سبک ---

    @Test
    fun `an animated project style with photorealistic keyword in free text produces a warning`() {
        val character = neutralCharacter(basePrompt = "make it photorealistic please")
        val issues = validateCharacterImagePromptInputs(character, neutralDna(VisualStyle.ANIME), generatedPrompt = validPromptLength)
        assertTrue(issues.any { it.message.contains("تناقض بصری") })
    }

    @Test
    fun `a photorealistic project style with an animated keyword in free text produces a warning (the reverse direction)`() {
        val character = neutralCharacter(basePrompt = "should look like anime")
        val issues = validateCharacterImagePromptInputs(character, neutralDna(VisualStyle.PHOTOREALISTIC), generatedPrompt = validPromptLength)
        assertTrue(issues.any { it.message.contains("تناقض بصری") })
    }

    @Test
    fun `matching style and free text produces no style-conflict warning`() {
        val character = neutralCharacter(basePrompt = "a determined expression")
        val issues = validateCharacterImagePromptInputs(character, neutralDna(VisualStyle.CINEMATIC_STYLE), generatedPrompt = validPromptLength)
        assertTrue(issues.none { it.message.contains("تناقض بصری") })
    }

    // --- بدون هیچ نقض، بدون هیچ Issue ---

    @Test
    fun `a fully valid character input produces zero issues`() {
        val issues = validateCharacterImagePromptInputs(neutralCharacter(), neutralDna(), generatedPrompt = validPromptLength)
        assertEquals(0, issues.size)
    }

    @Test
    fun `a fully valid outfit input produces zero issues`() {
        val outfit = Outfit(id = "outfit_1", name = "Casual", description = "a plain t-shirt and jeans", isDefault = true)
        val issues = validateOutfitImagePromptInputs(outfit, neutralDna(), generatedPrompt = validPromptLength)
        assertEquals(0, issues.size)
    }
}
