package com.operaboys.cinemashotgenerator.domain.validation

import com.operaboys.cinemashotgenerator.domain.asset.AssetType
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.Outfit
import com.operaboys.cinemashotgenerator.domain.asset.PhysicalAppearance
import com.operaboys.cinemashotgenerator.domain.camera.BasicMovementType
import com.operaboys.cinemashotgenerator.domain.camera.CameraAngle
import com.operaboys.cinemashotgenerator.domain.camera.CameraDistance
import com.operaboys.cinemashotgenerator.domain.camera.CameraMovement
import com.operaboys.cinemashotgenerator.domain.camera.CameraSettings
import com.operaboys.cinemashotgenerator.domain.camera.DepthOfField
import com.operaboys.cinemashotgenerator.domain.camera.Framing
import com.operaboys.cinemashotgenerator.domain.camera.FocusMode
import com.operaboys.cinemashotgenerator.domain.camera.LensType
import com.operaboys.cinemashotgenerator.domain.camera.Stabilization
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
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.SceneLocation
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ContrastRatio
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.KeyLightPosition
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingMotivation
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.shot.ActionSound
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.shot.SourcedSettings
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicLanguageSettings
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// واحد ۱۶ فاز ۵ — قدم ۱: تست تجمیع‌کننده‌ی خالص Validation. همه‌ی داده‌های پایه
// («neutral»، بدون هیچ نقض قانونی) طوری انتخاب شده‌اند که هیچ‌کدام از Rule های
// وایرشده در aggregateShotValidation را (عمداً یا تصادفی) لمس نکنند؛ هر تست فقط
// دقیقاً همان فیلد(هایی) را که برای همان یک Rule لازم است تغییر می‌دهد. جزئیات
// کامل تصمیمات در docs/adr/055-unit16-phase5-step1-validation-screen.md.

private fun neutralCamera(): CameraSettings = CameraSettings(
    angle = CameraAngle.EYE_LEVEL,
    distance = CameraDistance.MEDIUM,
    movement = CameraMovement.Basic(BasicMovementType.STATIC),
    lensType = LensType.STANDARD,
    depthOfField = DepthOfField.MEDIUM,
    focusMode = FocusMode.AUTO,
    stabilization = Stabilization.TRIPOD,
    framing = Framing.RULE_OF_THIRDS
)

private fun neutralLighting(): LightingSettings = LightingSettings(
    style = LightingStyle.NATURAL_LIGHT,
    keyLightPosition = KeyLightPosition.SIDE,
    contrastRatio = ContrastRatio.MEDIUM,
    lightingMotivation = LightingMotivation.ARTIFICIAL
)

private fun neutralEnvironment(): EnvironmentSettings = EnvironmentSettings(weatherType = WeatherType.CLEAR)

private fun neutralShot(
    shotDescription: String = "A perfectly neutral, valid shot description",
    camera: CameraSettings = neutralCamera(),
    lighting: LightingSettings = neutralLighting(),
    environment: EnvironmentSettings = neutralEnvironment(),
    durationSeconds: Float = 4f
): Shot = Shot(
    shotId = "shot_001",
    sceneId = "scene_001",
    shotNumber = 1,
    shotDescription = shotDescription,
    shotGoal = ShotGoal.ESTABLISHING,
    shotType = ShotType.MEDIUM,
    durationSeconds = durationSeconds,
    motionLevel = MotionLevel.SUBTLE,
    camera = SourcedSettings(source = "override", overrideValue = camera),
    lighting = SourcedSettings(source = "override", overrideValue = lighting),
    environment = SourcedSettings(source = "override", overrideValue = environment),
    soundProfile = SoundProfile(enabled = false),
    characterIds = listOf("char_001")
)

private fun neutralScene(
    timeOfDay: TimeOfDay = TimeOfDay.AFTERNOON,
    narrativeRole: NarrativeRole = NarrativeRole.DEVELOPMENT,
    atmospherePrimary: Atmosphere = Atmosphere.CALM,
    locationType: LocationType = LocationType.MIXED
): Scene = Scene(
    sceneId = "scene_001",
    sceneNumber = 1,
    narrativeRole = narrativeRole,
    location = SceneLocation(locationType, "A neutral location"),
    timeOfDay = timeOfDay,
    atmospherePrimary = atmospherePrimary,
    shotCount = 1
)

private fun neutralDna(maxShotDurationSeconds: Int = 10): ProjectDna = ProjectDna(
    dnaId = "dna_001",
    projectId = "proj_001",
    coreIdentity = CoreIdentity(VisualStyle.CINEMATIC_STYLE, RealismLevel.SEMI_REALISTIC, StyleConsistency.MODERATE),
    masterPalette = MasterPalette(ColorTemperature.NEUTRAL, SaturationLevel.MEDIUM, ContrastLevel.MEDIUM, ""),
    outputConstraints = OutputConstraints(emptyMap(), emptyList(), maxShotDurationSeconds, AspectRatio.LANDSCAPE_16_9),
    globalMoodBase = GlobalMoodBase(Mood.CALM)
)

private fun neutralCharacterAsset(outfits: List<Outfit> = listOf(Outfit("outfit_1", "Default", "desc", isDefault = true))): CharacterAsset =
    CharacterAsset(
        assetId = "char_001",
        assetType = AssetType.CHARACTER,
        characterTier = CharacterTier.MAIN,
        name = "Neutral Character",
        physicalAppearance = PhysicalAppearance(ageRange = "30s", gender = Gender.OTHER),
        outfits = outfits
    )

class ValidationAggregatorTest {

    @Test
    fun `a fully neutral shot produces zero issues at every level`() {
        val report = aggregateShotValidation(
            neutralShot(), neutralScene(), neutralDna(), listOf(neutralCharacterAsset()), emptyList(), emptyList()
        )
        assertEquals(0, report.blockingCount)
        assertEquals(0, report.warningCount)
        assertTrue(report.issues.isEmpty())
    }

    @Test
    fun `aggregation collects violations from multiple different units for the same shot`() {
        // Level 1 (واحد ۰۵): توضیح کوتاه‌تر از ۱۰ کاراکتر.
        // Level 2 (واحد ۰۹): لنز Ultra-wide با Close-up.
        // Level 3 (واحد ۰۲ + واحد ۰۶): مدت شات بیشتر از سقف DNA + Outfit پیش‌فرض غایب.
        val shot = neutralShot(
            shotDescription = "short",
            camera = neutralCamera().copy(lensType = LensType.ULTRA_WIDE, distance = CameraDistance.CLOSE_UP),
            durationSeconds = 20f
        )
        val characterAsset = neutralCharacterAsset(outfits = listOf(Outfit("outfit_1", "NotDefault", "desc", isDefault = false)))

        val report = aggregateShotValidation(shot, neutralScene(), neutralDna(maxShotDurationSeconds = 10), listOf(characterAsset), emptyList(), emptyList())

        val level1 = report.issuesAtLevel(ValidationLevel.DATA_COMPLETENESS)
        val level2 = report.issuesAtLevel(ValidationLevel.LOGICAL_CONSISTENCY)
        val level3 = report.issuesAtLevel(ValidationLevel.CONTINUITY_AND_DEPENDENCY)

        assertTrue(level1.any { it.issue.field == "shot_description" })
        assertTrue(level2.any { it.issue.message.contains("Ultra-wide") })
        assertTrue(level3.any { it.issue.message.contains("مدت این شات") })
        assertTrue(level3.any { it.issue.message.contains("Outfit") })
    }

    @Test
    fun `blocking and warning counts are exact`() {
        // یک Blocking (توضیح کوتاه) + یک Warning (negativePromptOverride فقط فاصله).
        val shot = neutralShot(shotDescription = "short").copy(negativePromptOverride = "   ")

        val report = aggregateShotValidation(shot, neutralScene(), neutralDna(), listOf(neutralCharacterAsset()), emptyList(), emptyList())

        assertEquals(1, report.blockingCount)
        assertEquals(1, report.warningCount)
        assertEquals(2, report.issues.size)
    }

    @Test
    fun `checkSunlightAtNight (one of the 13 lighting-environment rules) fires correctly when its condition holds`() {
        val shot = neutralShot(lighting = neutralLighting().copy(lightingMotivation = LightingMotivation.SUNLIGHT))
        val scene = neutralScene(timeOfDay = TimeOfDay.NIGHT)

        val report = aggregateShotValidation(shot, scene, neutralDna(), listOf(neutralCharacterAsset()), emptyList(), emptyList())

        val sunlightIssue = report.issuesAtLevel(ValidationLevel.LOGICAL_CONSISTENCY)
            .firstOrNull { it.issue.message.contains("نور خورشید در شب") }
        assertTrue(sunlightIssue != null)
        assertEquals(Severity.BLOCKING, sunlightIssue!!.issue.severity)
    }

    @Test
    fun `checkSunlightAtNight does not fire when time of day is not night`() {
        val shot = neutralShot(lighting = neutralLighting().copy(lightingMotivation = LightingMotivation.SUNLIGHT))
        val scene = neutralScene(timeOfDay = TimeOfDay.AFTERNOON)

        val report = aggregateShotValidation(shot, scene, neutralDna(), listOf(neutralCharacterAsset()), emptyList(), emptyList())

        assertTrue(report.issuesAtLevel(ValidationLevel.LOGICAL_CONSISTENCY).none { it.issue.message.contains("نور خورشید در شب") })
    }

    // رفع یافته‌ی G14 «کاندید رفع نزدیک» (ADR-064 تصمیم ۹، ADR-092):
    // validateActionSoundTimeline اکنون در Level 1 وایر است.
    @Test
    fun `an action sound timestamp outside the shot's duration produces a real Level 1 blocking issue`() {
        val shot = neutralShot(durationSeconds = 4f).copy(
            soundProfile = SoundProfile(
                enabled = true,
                actionSounds = listOf(ActionSound(timestampSeconds = 10f, type = "door_slam", description = "Door slams shut"))
            )
        )

        val report = aggregateShotValidation(shot, neutralScene(), neutralDna(), listOf(neutralCharacterAsset()), emptyList(), emptyList())

        val level1 = report.issuesAtLevel(ValidationLevel.DATA_COMPLETENESS)
        assertTrue(level1.any { it.issue.severity == Severity.BLOCKING })
    }

    @Test
    fun `an action sound timestamp inside the shot's duration produces no issue`() {
        val shot = neutralShot(durationSeconds = 10f).copy(
            soundProfile = SoundProfile(
                enabled = true,
                actionSounds = listOf(ActionSound(timestampSeconds = 5f, type = "door_slam", description = "Door slams shut"))
            )
        )

        val report = aggregateShotValidation(shot, neutralScene(), neutralDna(), listOf(neutralCharacterAsset()), emptyList(), emptyList())

        assertTrue(report.issues.isEmpty())
    }

    // تکمیل Rule یتیم — قدم ۱ از ۴ (ADR-106): validateShotDurationForCinematicMode
    // اکنون از طریق resolveEffectiveCinematicMode واقعاً در این تجمیع‌کننده فراخوانی
    // می‌شود — این تست‌ها ثابت می‌کنند خروجی واقعاً به Level 3 اضافه می‌شود، نه فقط
    // اینکه تابع مستقلاً درست کار می‌کند (که در CinematicLanguageTest.kt پوشش دارد).

    @Test
    fun `a shot duration outside the project's global cinematic mode range produces a Level 3 warning`() {
        val dna = neutralDna().copy(cinematicLanguage = CinematicLanguageSettings(globalMode = CinematicMode.LONG_TAKE))
        val shot = neutralShot(durationSeconds = 4f) // خارج از بازه‌ی مجاز LONG_TAKE (۸-۶۰ ثانیه)

        val report = aggregateShotValidation(shot, neutralScene(), dna, listOf(neutralCharacterAsset()), emptyList(), emptyList())

        val level3 = report.issuesAtLevel(ValidationLevel.CONTINUITY_AND_DEPENDENCY)
        val cinematicIssue = level3.firstOrNull { it.issue.field == "duration_seconds" }
        assertTrue(cinematicIssue != null)
        assertEquals(Severity.WARNING, cinematicIssue!!.issue.severity)
    }

    @Test
    fun `a shot override wins over the project's global cinematic mode in the aggregated report`() {
        // پروژه LONG_TAKE (نیازمند ۸-۶۰ ثانیه) است، اما خودِ شات Override به FAST_CUT دارد
        // (نیازمند ۱-۵ ثانیه) — با مدت ۴ ثانیه، هیچ هشداری نباید تولید شود چون FAST_CUT
        // (نه LONG_TAKE) واقعاً حالت مؤثر است.
        val dna = neutralDna().copy(cinematicLanguage = CinematicLanguageSettings(globalMode = CinematicMode.LONG_TAKE))
        val shot = neutralShot(durationSeconds = 4f).copy(cinematicModeOverride = CinematicMode.FAST_CUT)

        val report = aggregateShotValidation(shot, neutralScene(), dna, listOf(neutralCharacterAsset()), emptyList(), emptyList())

        assertTrue(report.issuesAtLevel(ValidationLevel.CONTINUITY_AND_DEPENDENCY).none { it.issue.field == "duration_seconds" })
    }
}
