package com.operaboys.cinemashotgenerator.domain.sceneconditions

import com.operaboys.cinemashotgenerator.domain.camera.CameraDistance
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentValidationTest {

    // --- مه + دید واضح ---

    @Test
    fun `fog with clear visibility blocks`() {
        val issue = checkFogWithClearVisibility(WeatherType.FOG, Visibility.CLEAR)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `fog with reduced visibility is valid`() {
        assertNull(checkFogWithClearVisibility(WeatherType.FOG, Visibility.REDUCED))
    }

    // --- falling_rain بدون آب‌وهوای بارانی ---

    @Test
    fun `falling rain motion without rainy weather blocks`() {
        val issue = checkFallingRainWithoutRainyWeather(listOf(EnvironmentalMotion.FALLING_RAIN), WeatherType.CLEAR)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `falling rain motion with rain weather is valid`() {
        assertNull(checkFallingRainWithoutRainyWeather(listOf(EnvironmentalMotion.FALLING_RAIN), WeatherType.RAIN))
    }

    @Test
    fun `falling rain motion with storm weather is valid`() {
        assertNull(checkFallingRainWithoutRainyWeather(listOf(EnvironmentalMotion.FALLING_RAIN), WeatherType.STORM))
    }

    // --- snowfall بدون آب‌وهوای برفی ---

    @Test
    fun `snowfall motion without snowy weather blocks`() {
        val issue = checkSnowfallWithoutSnowyWeather(listOf(EnvironmentalMotion.SNOWFALL), WeatherType.CLEAR)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `snowfall motion with snow weather is valid`() {
        assertNull(checkSnowfallWithoutSnowyWeather(listOf(EnvironmentalMotion.SNOWFALL), WeatherType.SNOW))
    }

    // --- باران + زمین خشک ---

    @Test
    fun `rain with dry ground warns`() {
        val issue = checkRainWithDryGround(WeatherType.RAIN, GroundState.DRY)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `rain with wet ground is valid`() {
        assertNull(checkRainWithDryGround(WeatherType.RAIN, GroundState.WET))
    }

    // --- طوفان + بدون باد ---

    @Test
    fun `storm without wind warns`() {
        val issue = checkStormWithoutWind(WeatherType.STORM, WindStrength.NONE)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `storm with strong wind is valid`() {
        assertNull(checkStormWithoutWind(WeatherType.STORM, WindStrength.STRONG))
    }

    // --- برف + زمین غیر برف‌پوش/یخ‌زده ---

    @Test
    fun `snow with dry ground warns`() {
        val issue = checkSnowWithMismatchedGround(WeatherType.SNOW, GroundState.DRY)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `snow with snow-covered ground is valid`() {
        assertNull(checkSnowWithMismatchedGround(WeatherType.SNOW, GroundState.SNOW_COVERED))
    }

    @Test
    fun `snow with icy ground is valid`() {
        assertNull(checkSnowWithMismatchedGround(WeatherType.SNOW, GroundState.ICY))
    }

    // --- Extreme Wide + دید کم ---

    @Test
    fun `extreme wide with low visibility warns`() {
        val issue = checkExtremeWideWithLowVisibility(CameraDistance.EXTREME_WIDE, Visibility.LOW)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `extreme wide with clear visibility is valid`() {
        assertNull(checkExtremeWideWithLowVisibility(CameraDistance.EXTREME_WIDE, Visibility.CLEAR))
    }

    // --- آتش + باران + فضای بیرونی (delegation به واحد ۰۷) ---

    @Test
    fun `fire in rain outdoors warns via unit07 delegation`() {
        val issues = checkFireInRainOutdoors(WeatherType.RAIN, LightingMotivation.FIRE, LocationType.OUTDOOR)
        assertEquals(1, issues.size)
        assertEquals(Severity.WARNING, issues[0].severity)
    }

    @Test
    fun `fire in rain indoors has no conflict`() {
        val issues = checkFireInRainOutdoors(WeatherType.RAIN, LightingMotivation.FIRE, LocationType.INDOOR)
        assertTrue(issues.isEmpty())
    }
}
