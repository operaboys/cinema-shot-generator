package com.operaboys.cinemashotgenerator.ui.shots

import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ColorTemperature
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ContrastRatio
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentalMotion
import com.operaboys.cinemashotgenerator.domain.sceneconditions.FillLight
import com.operaboys.cinemashotgenerator.domain.sceneconditions.GroundState
import com.operaboys.cinemashotgenerator.domain.sceneconditions.KeyLightPosition
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightSourceCount
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingMotivation
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ShadowQuality
import com.operaboys.cinemashotgenerator.domain.sceneconditions.TemperatureFeel
import com.operaboys.cinemashotgenerator.domain.sceneconditions.Visibility
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherIntensity
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WindStrength
import com.operaboys.cinemashotgenerator.ui.i18n.uiString

// واحد ۱۶ فاز ۴ — قدم ۴ (آخرین قدم): نگاشت enum های واحد ۰۸ (Scene Conditions) به
// کلید ترجمه، هم‌الگو با ui/shots/CameraLabels.kt. `LightingStyle` (از domain.dna،
// ۲۲ مقدار) بازاستفاده نشد اینجا تعریف — تابع `lightingStyleLabel` از قبل در
// `ui.dna.DnaLabels` وجود دارد و مستقیماً از آن‌جا Import می‌شود (کلیدهای ترجمه‌ی
// «lightingStyle.*» هم از قبل موجودند، تکرار نشدند).
//
// «colorTemperature.*» قبلاً برای ColorTemperature سطح DNA (واحد ۰۲، فقط
// WARM/COOL/NEUTRAL) استفاده شده بود؛ چون ColorTemperature این واحد (۰۸) واژگان
// متفاوتی دارد (WARM/NEUTRAL/COLD/MIXED — طبق کامنت صریح LightingModels.kt، عمداً
// enum جدایی)، کلیدهای این فایل با پیشوند متفاوت «sceneConditionsColorTemperature.*»
// نوشته شدند تا با کلیدهای موجود تداخل/اشتباه معنایی نداشته باشند.

fun keyLightPositionLabel(value: KeyLightPosition, language: Language): String = uiString(
    when (value) {
        KeyLightPosition.FRONT -> "keyLightPosition.front"
        KeyLightPosition.SIDE -> "keyLightPosition.side"
        KeyLightPosition.BACK -> "keyLightPosition.back"
        KeyLightPosition.TOP -> "keyLightPosition.top"
        KeyLightPosition.BOTTOM -> "keyLightPosition.bottom"
    },
    language
)

fun fillLightLabel(value: FillLight, language: Language): String = uiString(
    when (value) {
        FillLight.NONE -> "fillLight.none"
        FillLight.SOFT -> "fillLight.soft"
        FillLight.STRONG -> "fillLight.strong"
    },
    language
)

fun contrastRatioLabel(value: ContrastRatio, language: Language): String = uiString(
    when (value) {
        ContrastRatio.LOW -> "contrastRatio.low"
        ContrastRatio.MEDIUM -> "contrastRatio.medium"
        ContrastRatio.HIGH -> "contrastRatio.high"
    },
    language
)

fun sceneConditionsColorTemperatureLabel(value: ColorTemperature, language: Language): String = uiString(
    when (value) {
        ColorTemperature.WARM -> "sceneConditionsColorTemperature.warm"
        ColorTemperature.NEUTRAL -> "sceneConditionsColorTemperature.neutral"
        ColorTemperature.COLD -> "sceneConditionsColorTemperature.cold"
        ColorTemperature.MIXED -> "sceneConditionsColorTemperature.mixed"
    },
    language
)

fun shadowQualityLabel(value: ShadowQuality, language: Language): String = uiString(
    when (value) {
        ShadowQuality.SOFT_SHADOWS -> "shadowQuality.softShadows"
        ShadowQuality.HARD_SHADOWS -> "shadowQuality.hardShadows"
    },
    language
)

fun lightSourceCountLabel(value: LightSourceCount, language: Language): String = uiString(
    when (value) {
        LightSourceCount.SINGLE -> "lightSourceCount.single"
        LightSourceCount.DUAL -> "lightSourceCount.dual"
        LightSourceCount.MULTI -> "lightSourceCount.multi"
    },
    language
)

fun lightingMotivationLabel(value: LightingMotivation, language: Language): String = uiString(
    when (value) {
        LightingMotivation.SUNLIGHT -> "lightingMotivation.sunlight"
        LightingMotivation.ARTIFICIAL -> "lightingMotivation.artificial"
        LightingMotivation.MOONLIGHT -> "lightingMotivation.moonlight"
        LightingMotivation.FIRE -> "lightingMotivation.fire"
        LightingMotivation.PRACTICAL -> "lightingMotivation.practical"
        LightingMotivation.MIXED -> "lightingMotivation.mixed"
    },
    language
)

fun weatherTypeLabel(value: WeatherType, language: Language): String = uiString(
    when (value) {
        WeatherType.CLEAR -> "weatherType.clear"
        WeatherType.RAIN -> "weatherType.rain"
        WeatherType.STORM -> "weatherType.storm"
        WeatherType.SNOW -> "weatherType.snow"
        WeatherType.FOG -> "weatherType.fog"
    },
    language
)

fun weatherIntensityLabel(value: WeatherIntensity, language: Language): String = uiString(
    when (value) {
        WeatherIntensity.LIGHT -> "weatherIntensity.light"
        WeatherIntensity.MEDIUM -> "weatherIntensity.medium"
        WeatherIntensity.HEAVY -> "weatherIntensity.heavy"
    },
    language
)

fun windStrengthLabel(value: WindStrength, language: Language): String = uiString(
    when (value) {
        WindStrength.NONE -> "windStrength.none"
        WindStrength.LIGHT -> "windStrength.light"
        WindStrength.STRONG -> "windStrength.strong"
    },
    language
)

fun groundStateLabel(value: GroundState, language: Language): String = uiString(
    when (value) {
        GroundState.DRY -> "groundState.dry"
        GroundState.WET -> "groundState.wet"
        GroundState.MUDDY -> "groundState.muddy"
        GroundState.SNOW_COVERED -> "groundState.snowCovered"
        GroundState.SANDY -> "groundState.sandy"
        GroundState.ICY -> "groundState.icy"
    },
    language
)

fun visibilityLabel(value: Visibility, language: Language): String = uiString(
    when (value) {
        Visibility.CLEAR -> "visibility.clear"
        Visibility.REDUCED -> "visibility.reduced"
        Visibility.LOW -> "visibility.low"
    },
    language
)

fun temperatureFeelLabel(value: TemperatureFeel, language: Language): String = uiString(
    when (value) {
        TemperatureFeel.HOT -> "temperatureFeel.hot"
        TemperatureFeel.MILD -> "temperatureFeel.mild"
        TemperatureFeel.COLD -> "temperatureFeel.cold"
    },
    language
)

fun environmentalMotionLabel(value: EnvironmentalMotion, language: Language): String = uiString(
    when (value) {
        EnvironmentalMotion.FALLING_RAIN -> "environmentalMotion.fallingRain"
        EnvironmentalMotion.BLOWING_LEAVES -> "environmentalMotion.blowingLeaves"
        EnvironmentalMotion.SNOWFALL -> "environmentalMotion.snowfall"
        EnvironmentalMotion.DUST_CLOUDS -> "environmentalMotion.dustClouds"
        EnvironmentalMotion.FLYING_DEBRIS -> "environmentalMotion.flyingDebris"
    },
    language
)
