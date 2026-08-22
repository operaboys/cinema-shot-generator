package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.domain.dna.AspectRatio
import com.operaboys.cinemashotgenerator.domain.dna.ColorTemperature as DnaColorTemperature
import com.operaboys.cinemashotgenerator.domain.dna.ContrastLevel
import com.operaboys.cinemashotgenerator.domain.dna.CoreIdentity
import com.operaboys.cinemashotgenerator.domain.dna.GlobalMoodBase
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.dna.MasterPalette
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.dna.OutputConstraints
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.dna.RealismLevel
import com.operaboys.cinemashotgenerator.domain.dna.SaturationLevel
import com.operaboys.cinemashotgenerator.domain.dna.StyleConsistency
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
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
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode
import com.operaboys.cinemashotgenerator.domain.visualidentity.StyleInfluence
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    // تکمیل Rule یتیم — قدم ۱ از ۴ (ADR-106): هم‌الگو با تست‌های بالا برای
    // Shot.cinematicModeOverride تازه‌اضافه‌شده.

    private fun baseShot(cinematicModeOverride: CinematicMode?) = Shot(
        shotId = "shot_001",
        sceneId = "scene_001",
        shotNumber = 1,
        shotDescription = "A neutral shot description",
        shotGoal = ShotGoal.ESTABLISHING,
        shotType = ShotType.MEDIUM,
        durationSeconds = 4f,
        motionLevel = MotionLevel.SUBTLE,
        soundProfile = SoundProfile(enabled = false),
        cinematicModeOverride = cinematicModeOverride
    )

    @Test
    fun `Shot toDto then toDomain round-trips cinematicModeOverride exactly, both when set and when null`() {
        val withOverride = baseShot(cinematicModeOverride = CinematicMode.LONG_TAKE)
        assertEquals(withOverride, withOverride.toDto().toDomain())

        val withoutOverride = baseShot(cinematicModeOverride = null)
        assertEquals(withoutOverride, withoutOverride.toDto().toDomain())
    }

    // سیستم Preview دوزبانه‌ی پرامپت — قدم ۲ از ۳ زیرقدم (ADR-122): هم‌الگو
    // دقیق با تست‌های cinematicModeOverride بالا برای shotDescriptionFaPreview
    // تازه‌اضافه‌شده.

    @Test
    fun `Shot toDto then toDomain round-trips shotDescriptionFaPreview exactly, both when set and when null`() {
        val withPreview = baseShot(cinematicModeOverride = null).copy(shotDescriptionFaPreview = "جان وارد می‌شود")
        assertEquals(withPreview, withPreview.toDto().toDomain())

        val withoutPreview = baseShot(cinematicModeOverride = null).copy(shotDescriptionFaPreview = null)
        assertEquals(withoutPreview, withoutPreview.toDto().toDomain())
    }

    @Test
    fun `decoding an old ShotDataJson without the cinematicModeOverride key succeeds with a null default`() {
        val json = Json { ignoreUnknownKeys = true }
        val oldJsonWithoutCinematicModeOverride = """
            {
              "shotId": "shot_legacy",
              "sceneId": "scene_001",
              "shotNumber": 1,
              "shotDescription": "legacy shot",
              "shotGoal": "ESTABLISHING",
              "shotType": "MEDIUM",
              "durationSeconds": 4.0,
              "motionLevel": "SUBTLE",
              "soundProfile": { "enabled": false }
            }
        """.trimIndent()

        val decoded = json.decodeFromString(ShotDto.serializer(), oldJsonWithoutCinematicModeOverride)

        assertTrue(decoded.cinematicModeOverride == null)
        assertEquals("shot_legacy", decoded.shotId)
    }

    // تکمیل Rule یتیم — قدم ۱ از ۴ (ADR-106): همان الگو برای ProjectDnaDto.cinematicLanguage
    // تازه‌اضافه‌شده — اثبات اینکه dnaDataJson قدیمی (پیش از این قدم، بدون این کلید)
    // بدون خطا Decode می‌شود و پیش‌فرض BALANCED/sceneOverrides خالی می‌گیرد.

    @Test
    fun `decoding an old ProjectDnaDataJson without the cinematicLanguage key succeeds with the BALANCED default`() {
        val json = Json { ignoreUnknownKeys = true }
        val oldJsonWithoutCinematicLanguage = """
            {
              "dnaId": "dna_legacy",
              "projectId": "proj_001",
              "coreIdentity": {
                "dominantVisualStyle": "CINEMATIC_STYLE",
                "realismLevel": "SEMI_REALISTIC",
                "styleConsistency": "MODERATE"
              },
              "masterPalette": {
                "colorTemperature": "NEUTRAL",
                "globalSaturation": "MEDIUM",
                "globalContrast": "MEDIUM",
                "colorGradingPreset": ""
              },
              "outputConstraints": {
                "forbiddenElements": {},
                "mandatoryElements": [],
                "maxShotDurationSeconds": 10,
                "aspectRatio": "LANDSCAPE_16_9"
              },
              "globalMoodBase": { "primaryEmotion": "CALM", "intensity": "medium", "consistency": "MODERATE" }
            }
        """.trimIndent()

        val decoded = json.decodeFromString(ProjectDnaDto.serializer(), oldJsonWithoutCinematicLanguage)

        assertEquals("BALANCED", decoded.cinematicLanguage.globalMode)
        assertTrue(decoded.cinematicLanguage.sceneOverrides.isEmpty())
        assertEquals("dna_legacy", decoded.dnaId)
    }

    // اتصال Style Matrix — قدم ۱الف از ۱۰ زیرقدم (ADR-113): هم‌الگو با تست‌های
    // بالا برای CoreIdentity.secondaryStyle/influence تازه‌اضافه‌شده. CoreIdentity
    // برخلاف Shot/LightingSettings تابع toDto()/toDomain() مستقل ندارد (نگاشتش
    // مستقیماً داخل ProjectDna.toDto()/toDomain() نوشته شده — DnaAssetMappers.kt)،
    // پس Round-Trip از طریق کل ProjectDna تست می‌شود، نه یک تابع جدا روی خودِ
    // CoreIdentity.

    private fun baseProjectDna(secondaryStyle: VisualStyle?, influence: StyleInfluence?) = ProjectDna(
        dnaId = "dna_style_matrix_test",
        projectId = "proj_style_matrix_test",
        coreIdentity = CoreIdentity(
            dominantVisualStyle = VisualStyle.CINEMATIC_STYLE,
            realismLevel = RealismLevel.SEMI_REALISTIC,
            styleConsistency = StyleConsistency.MODERATE,
            secondaryStyle = secondaryStyle,
            influence = influence
        ),
        masterPalette = MasterPalette(
            colorTemperature = DnaColorTemperature.NEUTRAL,
            globalSaturation = SaturationLevel.MEDIUM,
            globalContrast = ContrastLevel.MEDIUM,
            colorGradingPreset = ""
        ),
        outputConstraints = OutputConstraints(
            forbiddenElements = emptyMap(),
            maxShotDurationSeconds = 8,
            aspectRatio = AspectRatio.LANDSCAPE_16_9
        ),
        globalMoodBase = GlobalMoodBase(primaryEmotion = Mood.CALM)
    )

    @Test
    fun `CoreIdentity secondaryStyle and influence round-trip via ProjectDna toDto then toDomain, both when set and when null`() {
        val withBothFields = baseProjectDna(secondaryStyle = VisualStyle.FILM_NOIR, influence = StyleInfluence.STRONG)
        assertEquals(withBothFields, withBothFields.toDto().toDomain())

        val withNoOptionalFields = baseProjectDna(secondaryStyle = null, influence = null)
        assertEquals(withNoOptionalFields, withNoOptionalFields.toDto().toDomain())
    }

    @Test
    fun `decoding an old CoreIdentityDto without secondaryStyle or influence keys succeeds with null defaults`() {
        val json = Json { ignoreUnknownKeys = true }
        val oldJsonWithoutSecondaryStyleOrInfluence = """
            {
              "dominantVisualStyle": "CINEMATIC_STYLE",
              "realismLevel": "SEMI_REALISTIC",
              "styleConsistency": "MODERATE"
            }
        """.trimIndent()

        val decoded = json.decodeFromString(CoreIdentityDto.serializer(), oldJsonWithoutSecondaryStyleOrInfluence)

        assertTrue(decoded.secondaryStyle == null)
        assertTrue(decoded.influence == null)
        assertEquals("CINEMATIC_STYLE", decoded.dominantVisualStyle)
    }
}
