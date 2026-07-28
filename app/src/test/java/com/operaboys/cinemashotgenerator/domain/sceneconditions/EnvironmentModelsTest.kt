package com.operaboys.cinemashotgenerator.domain.sceneconditions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// واحد ۰۸ — تست‌های Migration فیلدهای جدید Tab «نور و محیط» واحد ۱۶
// (docs/adr/036-unit08-lighting-environment-ui-fields-migration.md).

class EnvironmentModelsTest {

    @Test
    fun `new fields default to null or empty when not provided (backward compatible)`() {
        val settings = EnvironmentSettings(weatherType = WeatherType.CLEAR)
        assertNull(settings.weatherIntensity)
        assertNull(settings.windStrength)
        assertNull(settings.groundState)
        assertNull(settings.visibility)
        assertNull(settings.temperatureFeel)
        assertTrue(settings.environmentalMotion.isEmpty())
    }

    @Test
    fun `each new field holds an explicit value when provided`() {
        val settings = EnvironmentSettings(
            weatherType = WeatherType.RAIN,
            weatherIntensity = WeatherIntensity.HEAVY,
            windStrength = WindStrength.STRONG,
            groundState = GroundState.WET,
            visibility = Visibility.REDUCED,
            temperatureFeel = TemperatureFeel.COLD,
            environmentalMotion = listOf(EnvironmentalMotion.FALLING_RAIN, EnvironmentalMotion.BLOWING_LEAVES)
        )
        assertEquals(WeatherIntensity.HEAVY, settings.weatherIntensity)
        assertEquals(WindStrength.STRONG, settings.windStrength)
        assertEquals(GroundState.WET, settings.groundState)
        assertEquals(Visibility.REDUCED, settings.visibility)
        assertEquals(TemperatureFeel.COLD, settings.temperatureFeel)
        assertEquals(listOf(EnvironmentalMotion.FALLING_RAIN, EnvironmentalMotion.BLOWING_LEAVES), settings.environmentalMotion)
    }
}
