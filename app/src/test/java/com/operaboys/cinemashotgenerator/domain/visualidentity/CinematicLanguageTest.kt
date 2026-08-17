package com.operaboys.cinemashotgenerator.domain.visualidentity

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
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// تکمیل Rule یتیم — قدم ۱ از ۴ (ADR-106): داده‌های پایه برای تست‌های
// resolveEffectiveCinematicMode. هم‌الگو با neutralDna/neutralScene/neutralShot
// در ValidationAggregatorTest.kt (کمینه، بدون نقض هیچ Rule دیگری).

private fun testDna(globalMode: CinematicMode = CinematicMode.BALANCED, sceneOverrides: Map<String, CinematicMode> = emptyMap()): ProjectDna = ProjectDna(
    dnaId = "dna_001",
    projectId = "proj_001",
    coreIdentity = CoreIdentity(VisualStyle.CINEMATIC_STYLE, RealismLevel.SEMI_REALISTIC, StyleConsistency.MODERATE),
    masterPalette = MasterPalette(ColorTemperature.NEUTRAL, SaturationLevel.MEDIUM, ContrastLevel.MEDIUM, ""),
    outputConstraints = OutputConstraints(emptyMap(), emptyList(), 10, AspectRatio.LANDSCAPE_16_9),
    globalMoodBase = GlobalMoodBase(Mood.CALM),
    cinematicLanguage = CinematicLanguageSettings(globalMode = globalMode, sceneOverrides = sceneOverrides)
)

private fun testScene(sceneId: String = "scene_001", cinematicModeOverride: CinematicMode? = null): Scene = Scene(
    sceneId = sceneId,
    sceneNumber = 1,
    narrativeRole = NarrativeRole.DEVELOPMENT,
    location = SceneLocation(LocationType.MIXED, "A neutral location"),
    timeOfDay = TimeOfDay.AFTERNOON,
    atmospherePrimary = Atmosphere.CALM,
    shotCount = 1,
    cinematicModeOverride = cinematicModeOverride
)

private fun testShot(cinematicModeOverride: CinematicMode? = null): Shot = Shot(
    shotId = "shot_001",
    sceneId = "scene_001",
    shotNumber = 1,
    shotDescription = "A perfectly neutral, valid shot description",
    shotGoal = ShotGoal.ESTABLISHING,
    shotType = ShotType.MEDIUM,
    durationSeconds = 4f,
    motionLevel = MotionLevel.SUBTLE,
    soundProfile = SoundProfile(enabled = false),
    cinematicModeOverride = cinematicModeOverride
)

class CinematicLanguageTest {

    // --- resolveEffectiveMode ---

    @Test
    fun `resolveEffectiveMode uses scene override when present`() {
        val settings = CinematicLanguageSettings(
            globalMode = CinematicMode.BALANCED,
            sceneOverrides = mapOf("scene_003" to CinematicMode.FAST_CUT)
        )
        assertEquals(CinematicMode.FAST_CUT, resolveEffectiveMode(settings, "scene_003"))
    }

    @Test
    fun `resolveEffectiveMode falls back to global mode when no override`() {
        val settings = CinematicLanguageSettings(globalMode = CinematicMode.LONG_TAKE)
        assertEquals(CinematicMode.LONG_TAKE, resolveEffectiveMode(settings, "scene_099"))
    }

    // --- determineHybridPacing ---

    @Test
    fun `determineHybridPacing action scene is fast cut`() {
        assertEquals(CinematicMode.FAST_CUT, determineHybridPacing("action", avgBeatIntensity = 5f))
    }

    @Test
    fun `determineHybridPacing high intensity is fast cut regardless of scene type`() {
        assertEquals(CinematicMode.FAST_CUT, determineHybridPacing("dialogue", avgBeatIntensity = 8f))
    }

    @Test
    fun `determineHybridPacing emotional scene is long take`() {
        assertEquals(CinematicMode.LONG_TAKE, determineHybridPacing("emotional", avgBeatIntensity = 5f))
    }

    @Test
    fun `determineHybridPacing dialogue scene is balanced`() {
        assertEquals(CinematicMode.BALANCED, determineHybridPacing("dialogue", avgBeatIntensity = 5f))
    }

    @Test
    fun `determineHybridPacing unknown scene type defaults to balanced`() {
        assertEquals(CinematicMode.BALANCED, determineHybridPacing("unknown", avgBeatIntensity = 5f))
    }

    // --- getPacingFromEmotion (MIGRATED, docs/adr/039-...md، رفع F4 ممیزی pre-Unit 16) ---

    @Test
    fun `getPacingFromEmotion HIGH_ENERGY category mood is fast cut`() {
        assertEquals(CinematicMode.FAST_CUT, getPacingFromEmotion(Mood.EPIC))
    }

    @Test
    fun `getPacingFromEmotion DARK category mood is fast cut`() {
        assertEquals(CinematicMode.FAST_CUT, getPacingFromEmotion(Mood.TENSE))
    }

    @Test
    fun `getPacingFromEmotion EMOTIONAL category mood is long take`() {
        assertEquals(CinematicMode.LONG_TAKE, getPacingFromEmotion(Mood.MELANCHOLIC))
    }

    @Test
    fun `getPacingFromEmotion CALM category mood is long take`() {
        assertEquals(CinematicMode.LONG_TAKE, getPacingFromEmotion(Mood.SERENE))
    }

    @Test
    fun `getPacingFromEmotion POSITIVE category mood is balanced`() {
        assertEquals(CinematicMode.BALANCED, getPacingFromEmotion(Mood.HAPPY))
    }

    @Test
    fun `getPacingFromEmotion is total across every Mood value, never throwing`() {
        Mood.entries.forEach { mood -> getPacingFromEmotion(mood) }
    }

    // --- Rule 1 (Blocking): پارس مقدار خام ---

    @Test
    fun `rule1 parses all three valid raw values`() {
        assertEquals(CinematicMode.LONG_TAKE, parseCinematicMode("long_take").getOrThrow())
        assertEquals(CinematicMode.FAST_CUT, parseCinematicMode("fast_cut").getOrThrow())
        assertEquals(CinematicMode.BALANCED, parseCinematicMode("hybrid").getOrThrow())
    }

    @Test
    fun `rule1 invalid raw value fails`() {
        val result = parseCinematicMode("slow_motion")
        assertTrue(result.isFailure)
    }

    // --- Rule 2 (Warning): مدت شات خارج از محدوده ---

    @Test
    fun `rule2 duration within long take range is valid`() {
        assertNull(validateShotDurationForCinematicMode(15f, CinematicMode.LONG_TAKE))
    }

    @Test
    fun `rule2 duration too short for long take warns`() {
        val issue = validateShotDurationForCinematicMode(3f, CinematicMode.LONG_TAKE)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `rule2 duration too long for fast cut warns`() {
        val issue = validateShotDurationForCinematicMode(10f, CinematicMode.FAST_CUT)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `rule2 duration within balanced range is valid`() {
        assertNull(validateShotDurationForCinematicMode(10f, CinematicMode.BALANCED))
    }

    // --- resolveEffectiveCinematicMode (تکمیل Rule یتیم — قدم ۱ از ۴، ADR-106) ---
    // زنجیره‌ی سه‌سطحی بلوپرینت ۰۳ بخش ب: Override شات > Override صحنه > پیش‌فرض DNA پروژه.

    @Test
    fun `resolveEffectiveCinematicMode falls back to project global mode when no override exists at any tier`() {
        val dna = testDna(globalMode = CinematicMode.LONG_TAKE)
        val scene = testScene()
        val shot = testShot()

        assertEquals(CinematicMode.LONG_TAKE, resolveEffectiveCinematicMode(dna, scene, shot))
    }

    @Test
    fun `resolveEffectiveCinematicMode uses scene override when present without a shot override`() {
        val dna = testDna(globalMode = CinematicMode.LONG_TAKE)
        val scene = testScene(cinematicModeOverride = CinematicMode.FAST_CUT)
        val shot = testShot()

        assertEquals(CinematicMode.FAST_CUT, resolveEffectiveCinematicMode(dna, scene, shot))
    }

    @Test
    fun `resolveEffectiveCinematicMode uses project sceneOverrides map when scene has no direct override`() {
        val dna = testDna(globalMode = CinematicMode.LONG_TAKE, sceneOverrides = mapOf("scene_001" to CinematicMode.BALANCED))
        val scene = testScene(sceneId = "scene_001")
        val shot = testShot()

        assertEquals(CinematicMode.BALANCED, resolveEffectiveCinematicMode(dna, scene, shot))
    }

    @Test
    fun `resolveEffectiveCinematicMode shot override wins over everything else`() {
        val dna = testDna(globalMode = CinematicMode.LONG_TAKE, sceneOverrides = mapOf("scene_001" to CinematicMode.BALANCED))
        val scene = testScene(sceneId = "scene_001", cinematicModeOverride = CinematicMode.BALANCED)
        val shot = testShot(cinematicModeOverride = CinematicMode.FAST_CUT)

        assertEquals(CinematicMode.FAST_CUT, resolveEffectiveCinematicMode(dna, scene, shot))
    }
}
