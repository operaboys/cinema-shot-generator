package com.operaboys.cinemashotgenerator.domain.sceneconditions

import com.operaboys.cinemashotgenerator.domain.camera.CameraDistance
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.domain.validation.validateLogicConsistency

// واحد ۰۸ — قوانین اعتبارسنجی Environment & Weather Engine (بخش ب، جدول کامل — ۸ Rule)
// منبع حقیقت: docs/blueprints/08-scene-conditions.md
//
// «آب‌وهوای بارانی» (Rule 2/4) شامل RAIN و STORM هر دو تلقی شد — چون خودِ
// mapEnvironmentToSound این واحد، Storm را معادل «torrential rain and wind» در نظر
// می‌گیرد (نه یک پدیده‌ی کاملاً بی‌ربط به باران). جزئیات در ADR-009.
private fun isRainyWeather(weatherType: WeatherType): Boolean =
    weatherType == WeatherType.RAIN || weatherType == WeatherType.STORM

/** Rule: مه (fog) + دید واضح (clear) → ناسازگاری فیزیکی (Blocking). */
fun checkFogWithClearVisibility(weatherType: WeatherType, visibility: Visibility): ValidationIssue? {
    if (weatherType == WeatherType.FOG && visibility == Visibility.CLEAR) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "مه با دید واضح (Clear) فیزیکاً ناسازگار است"
        )
    }
    return null
}

/** Rule: falling_rain در environmental_motion بدون آب‌وهوای بارانی → ناسازگاری ساختاری (Blocking). */
fun checkFallingRainWithoutRainyWeather(
    environmentalMotion: List<EnvironmentalMotion>,
    weatherType: WeatherType
): ValidationIssue? {
    if (EnvironmentalMotion.FALLING_RAIN in environmentalMotion && !isRainyWeather(weatherType)) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "falling_rain بدون آب‌وهوای بارانی (Rain/Storm) ناسازگاری ساختاری دارد"
        )
    }
    return null
}

/** Rule: snowfall بدون آب‌وهوای برفی → ناسازگاری ساختاری (Blocking). */
fun checkSnowfallWithoutSnowyWeather(
    environmentalMotion: List<EnvironmentalMotion>,
    weatherType: WeatherType
): ValidationIssue? {
    if (EnvironmentalMotion.SNOWFALL in environmentalMotion && weatherType != WeatherType.SNOW) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "snowfall بدون آب‌وهوای برفی (Snow) ناسازگاری ساختاری دارد"
        )
    }
    return null
}

/** Rule: باران + زمین خشک → ناسازگاری منطقی (Warning). */
fun checkRainWithDryGround(weatherType: WeatherType, groundState: GroundState): ValidationIssue? {
    if (isRainyWeather(weatherType) && groundState == GroundState.DRY) {
        return ValidationIssue(
            Severity.WARNING,
            message = "باران با زمین خشک ناسازگاری منطقی دارد"
        )
    }
    return null
}

/** Rule: طوفان + بدون باد → ناسازگاری منطقی (Warning). */
fun checkStormWithoutWind(weatherType: WeatherType, windStrength: WindStrength): ValidationIssue? {
    if (weatherType == WeatherType.STORM && windStrength == WindStrength.NONE) {
        return ValidationIssue(
            Severity.WARNING,
            message = "طوفان بدون باد ناسازگاری منطقی دارد"
        )
    }
    return null
}

/** Rule: برف + زمین غیر برف‌پوش/یخ‌زده → ناسازگاری منطقی (Warning). */
fun checkSnowWithMismatchedGround(weatherType: WeatherType, groundState: GroundState): ValidationIssue? {
    val isSnowCompatibleGround = groundState == GroundState.SNOW_COVERED || groundState == GroundState.ICY
    if (weatherType == WeatherType.SNOW && !isSnowCompatibleGround) {
        return ValidationIssue(
            Severity.WARNING,
            message = "برف با زمین غیر برف‌پوش/یخ‌زده ناسازگاری منطقی دارد"
        )
    }
    return null
}

/**
 * Rule: Extreme Wide + دید کم → بی‌معنی بودن ترکیب (Warning).
 * از نوع واقعی CameraDistance (واحد ۰۹، از قبل پیاده‌سازی‌شده) استفاده می‌کند.
 */
fun checkExtremeWideWithLowVisibility(distance: CameraDistance, visibility: Visibility): ValidationIssue? {
    if (distance == CameraDistance.EXTREME_WIDE && visibility == Visibility.LOW) {
        return ValidationIssue(
            Severity.WARNING,
            message = "نمای Extreme Wide با دید کم ترکیب بی‌معنایی است"
        )
    }
    return null
}

/**
 * Rule: آتش + باران + فضای بیرونی → ناسازگاری منطقی (مشترک با واحد ۰۷).
 * مستقیماً از validateLogicConsistency واحد ۰۷ استفاده می‌کند (بدون تکرار منطق):
 * همان تابع weather/lightingMotivation/locationType را به‌صورت String می‌گیرد و
 * دقیقاً همین شرط (rain+fire+outdoor) را بررسی می‌کند. locationType پارامتر خارجی
 * است چون این واحد چنین فیلدی در پارامترهای خودش ندارد (متعلق به Scene، واحد ۰۴)؛
 * دقیقاً مثل الگوی locationType در خودِ امضای واحد ۰۷.
 */
fun checkFireInRainOutdoors(
    weatherType: WeatherType,
    lightingMotivation: LightingMotivation,
    locationType: String
): List<ValidationIssue> {
    return validateLogicConsistency(
        weather = weatherType.name.lowercase(),
        lightingMotivation = lightingMotivation.name.lowercase(),
        locationType = locationType
    )
}
