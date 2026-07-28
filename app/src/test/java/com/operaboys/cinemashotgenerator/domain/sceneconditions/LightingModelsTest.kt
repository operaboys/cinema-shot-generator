package com.operaboys.cinemashotgenerator.domain.sceneconditions

import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// واحد ۰۸ — تست‌های Migration فیلدهای جدید Tab «نور و محیط» واحد ۱۶
// (docs/adr/036-unit08-lighting-environment-ui-fields-migration.md).

class LightingModelsTest {

    private fun baseLightingSettings(
        fillLight: FillLight? = null,
        colorTemperature: ColorTemperature? = null,
        shadowQuality: ShadowQuality? = null,
        lightSourceCount: LightSourceCount? = null,
        lightingMotivation: LightingMotivation? = null
    ) = LightingSettings(
        style = LightingStyle.DRAMATIC_LIGHT,
        keyLightPosition = KeyLightPosition.SIDE,
        contrastRatio = ContrastRatio.HIGH,
        fillLight = fillLight,
        colorTemperature = colorTemperature,
        shadowQuality = shadowQuality,
        lightSourceCount = lightSourceCount,
        lightingMotivation = lightingMotivation
    )

    @Test
    fun `new fields default to null when not provided (backward compatible)`() {
        val settings = LightingSettings(
            style = LightingStyle.NATURAL_LIGHT,
            keyLightPosition = KeyLightPosition.FRONT,
            contrastRatio = ContrastRatio.LOW
        )
        assertNull(settings.fillLight)
        assertNull(settings.colorTemperature)
        assertNull(settings.shadowQuality)
        assertNull(settings.lightSourceCount)
        assertNull(settings.lightingMotivation)
    }

    @Test
    fun `each new field holds an explicit value when provided`() {
        val settings = baseLightingSettings(
            fillLight = FillLight.STRONG,
            colorTemperature = ColorTemperature.COLD,
            shadowQuality = ShadowQuality.HARD_SHADOWS,
            lightSourceCount = LightSourceCount.MULTI,
            lightingMotivation = LightingMotivation.FIRE
        )
        assertEquals(FillLight.STRONG, settings.fillLight)
        assertEquals(ColorTemperature.COLD, settings.colorTemperature)
        assertEquals(ShadowQuality.HARD_SHADOWS, settings.shadowQuality)
        assertEquals(LightSourceCount.MULTI, settings.lightSourceCount)
        assertEquals(LightingMotivation.FIRE, settings.lightingMotivation)
    }
}
