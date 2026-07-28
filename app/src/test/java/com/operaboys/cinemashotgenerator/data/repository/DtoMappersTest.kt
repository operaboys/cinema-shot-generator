package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ColorTemperature
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ContrastRatio
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentalMotion
import com.operaboys.cinemashotgenerator.domain.sceneconditions.FillLight
import com.operaboys.cinemashotgenerator.domain.sceneconditions.GroundState
import com.operaboys.cinemashotgenerator.domain.sceneconditions.KeyLightPosition
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightSourceCount
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingMotivation
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ShadowQuality
import com.operaboys.cinemashotgenerator.domain.sceneconditions.TemperatureFeel
import com.operaboys.cinemashotgenerator.domain.sceneconditions.Visibility
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherIntensity
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WindStrength
import org.junit.Assert.assertEquals
import org.junit.Test

// واحد ۱۵ — تست round-trip برای فیلدهای جدید LightingSettings/EnvironmentSettings
// (docs/adr/036-unit08-lighting-environment-ui-fields-migration.md): این یافته‌ی
// خارج از Blast Radius اعلام‌شده در دستور کار بود — بدون این نگاشت، مقادیر
// ویرایش‌شده‌ی کاربر در Tab «نور و محیط» حین ذخیره/بارگذاری Room گم می‌شدند.

class DtoMappersTest {

    @Test
    fun `LightingSettings toDto then toDomain round-trips every new field exactly, including nulls`() {
        val withAllFields = LightingSettings(
            style = LightingStyle.DRAMATIC_LIGHT,
            keyLightPosition = KeyLightPosition.SIDE,
            contrastRatio = ContrastRatio.HIGH,
            fillLight = FillLight.STRONG,
            colorTemperature = ColorTemperature.COLD,
            shadowQuality = ShadowQuality.HARD_SHADOWS,
            lightSourceCount = LightSourceCount.MULTI,
            lightingMotivation = LightingMotivation.FIRE
        )
        assertEquals(withAllFields, withAllFields.toDto().toDomain())

        val withNoOptionalFields = LightingSettings(
            style = LightingStyle.NATURAL_LIGHT,
            keyLightPosition = KeyLightPosition.FRONT,
            contrastRatio = ContrastRatio.LOW
        )
        assertEquals(withNoOptionalFields, withNoOptionalFields.toDto().toDomain())
    }

    @Test
    fun `EnvironmentSettings toDto then toDomain round-trips every new field exactly, including nulls and empty list`() {
        val withAllFields = EnvironmentSettings(
            weatherType = WeatherType.RAIN,
            weatherIntensity = WeatherIntensity.HEAVY,
            windStrength = WindStrength.STRONG,
            groundState = GroundState.WET,
            visibility = Visibility.REDUCED,
            temperatureFeel = TemperatureFeel.COLD,
            environmentalMotion = listOf(EnvironmentalMotion.FALLING_RAIN, EnvironmentalMotion.BLOWING_LEAVES)
        )
        assertEquals(withAllFields, withAllFields.toDto().toDomain())

        val withNoOptionalFields = EnvironmentSettings(weatherType = WeatherType.CLEAR)
        assertEquals(withNoOptionalFields, withNoOptionalFields.toDto().toDomain())
    }
}
