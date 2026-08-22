package com.operaboys.cinemashotgenerator.domain.dna

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// واحد ۰۲ — تست‌های Migration بلوپرینت ۰۲ نسخه ۵ (جزئیات کامل در
// docs/adr/027-unit02-dna-manager-v5-migration.md).
//
// تغییرات نسبت به نسخه‌ی قبلی این فایل:
// - sampleDna() دیگر stylePreferences/overrideRules نمی‌سازد (این دو فیلد از
//   ProjectDna حذف شدند، طبق تصمیم صریح معمار).
// - دو تست `requiresApprovalForOverride reflects true/false flag` حذف شدند —
//   خودِ تابعی که تست می‌کردند (requiresApprovalForOverride) حذف شد، چون overrideRules
//   دیگر بخشی از ProjectDna نیست. این حذف مستقیم و عمدی است، نه فراموشی.
// - validateShotAspectRatio تست‌ها اکنون AspectRatio enum می‌گیرند، نه String.
// - تست‌های validateColorPalette (Rule ۶/۷ جدید) اضافه شدند.

class DnaValidationTest {

    private fun sampleDna(
        forbiddenElements: Map<String, List<String>> = mapOf(
            "camera" to listOf("dutch_angle"),
            "lighting" to listOf("top_light"),
            "weather" to emptyList()
        ),
        maxShotDurationSeconds: Int = 10,
        aspectRatio: AspectRatio = AspectRatio.ANAMORPHIC_2_39,
        colorPalette: List<String> = emptyList()
    ) = ProjectDna(
        dnaId = "dna_001",
        projectId = "proj_001",
        coreIdentity = CoreIdentity(
            dominantVisualStyle = VisualStyle.CINEMATIC_STYLE,
            realismLevel = RealismLevel.GROUNDED,
            styleConsistency = StyleConsistency.STRICT,
            locked = true
        ),
        masterPalette = MasterPalette(
            colorTemperature = ColorTemperature.WARM,
            globalSaturation = SaturationLevel.MEDIUM,
            globalContrast = ContrastLevel.HIGH,
            colorGradingPreset = "natural",
            colorPalette = colorPalette
        ),
        globalMoodBase = GlobalMoodBase(
            primaryEmotion = Mood.MYSTERIOUS,
            intensity = "medium",
            consistency = StyleConsistency.STRICT
        ),
        outputConstraints = OutputConstraints(
            forbiddenElements = forbiddenElements,
            maxShotDurationSeconds = maxShotDurationSeconds,
            aspectRatio = aspectRatio
        )
    )

    // --- Rule 1 (Soft Lock): تغییر Core Identity هرگز بلاک نمی‌شود ---

    @Test
    fun `updateCoreIdentity without dependent shots applies change with no warning`() {
        val result = updateCoreIdentity(sampleDna(), VisualStyle.PHOTOREALISTIC, dependentShotsCount = 0)

        assertEquals(VisualStyle.PHOTOREALISTIC, result.updatedDna.coreIdentity.dominantVisualStyle)
        assertEquals(null, result.warning)
    }

    @Test
    fun `updateCoreIdentity with dependent shots warns but still applies change`() {
        val result = updateCoreIdentity(sampleDna(), VisualStyle.PHOTOREALISTIC, dependentShotsCount = 5)

        assertEquals(VisualStyle.PHOTOREALISTIC, result.updatedDna.coreIdentity.dominantVisualStyle)
        assertTrue(result.warning != null)
        assertTrue(result.warning!!.contains("5"))
    }

    // --- Rule 2 (Blocking): forbidden_elements، پوشش هر سه دسته ---

    @Test
    fun `validateShotAgainstDna blocks forbidden camera element`() {
        val result = validateShotAgainstDna("dutch_angle", "camera", sampleDna())

        assertEquals(Severity.BLOCKING, result!!.severity)
    }

    @Test
    fun `validateShotAgainstDna blocks forbidden lighting element`() {
        val result = validateShotAgainstDna("top_light", "lighting", sampleDna())

        assertEquals(Severity.BLOCKING, result!!.severity)
    }

    @Test
    fun `validateShotAgainstDna allows permitted element`() {
        val result = validateShotAgainstDna("tracking", "camera", sampleDna())

        assertNull(result)
    }

    @Test
    fun `validateShotAgainstDna allows element in category with no forbidden list`() {
        val result = validateShotAgainstDna("rain", "weather", sampleDna())

        assertNull(result)
    }

    // --- Rule 4 (Blocking): technical_constraints ---

    @Test
    fun `validateShotDuration within limit is valid`() {
        val result = validateShotDuration(8, sampleDna(maxShotDurationSeconds = 10))

        assertNull(result)
    }

    @Test
    fun `validateShotDuration exceeding limit blocks`() {
        val result = validateShotDuration(15, sampleDna(maxShotDurationSeconds = 10))

        assertEquals(Severity.BLOCKING, result!!.severity)
    }

    // --- Rule 6/7 (جدید): validateColorPalette ---

    @Test
    fun `validateColorPalette is valid for an empty palette`() {
        assertNull(validateColorPalette(emptyList()))
    }

    @Test
    fun `validateColorPalette is valid for up to 5 valid hex colors`() {
        val result = validateColorPalette(listOf("#3B82F6", "#8B5CF6", "#EC4899", "#F59E0B", "#10B981"))

        assertNull(result)
    }

    @Test
    fun `validateColorPalette warns when more than 5 colors are given`() {
        val result = validateColorPalette(listOf("#3B82F6", "#8B5CF6", "#EC4899", "#F59E0B", "#10B981", "#111111"))

        assertEquals(Severity.WARNING, result!!.severity)
    }

    @Test
    fun `validateColorPalette blocks an invalid hex value`() {
        val result = validateColorPalette(listOf("#3B82F6", "not-a-hex-color"))

        assertEquals(Severity.BLOCKING, result!!.severity)
        assertTrue(result.message.contains("not-a-hex-color"))
    }
}
