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
import com.operaboys.cinemashotgenerator.domain.shot.Beat
import com.operaboys.cinemashotgenerator.domain.shot.BeatEventType
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
    outputConstraints = OutputConstraints(emptyMap(), 10, AspectRatio.LANDSCAPE_16_9),
    globalMoodBase = GlobalMoodBase(Mood.CALM),
    cinematicLanguage = CinematicLanguageSettings(globalMode = globalMode, sceneOverrides = sceneOverrides)
)

private fun testScene(
    sceneId: String = "scene_001",
    cinematicModeOverride: CinematicMode? = null,
    mood: Mood? = null
): Scene = Scene(
    sceneId = sceneId,
    sceneNumber = 1,
    narrativeRole = NarrativeRole.DEVELOPMENT,
    location = SceneLocation(LocationType.MIXED, "A neutral location"),
    timeOfDay = TimeOfDay.AFTERNOON,
    atmospherePrimary = Atmosphere.CALM,
    shotCount = 1,
    cinematicModeOverride = cinematicModeOverride,
    mood = mood
)

private fun testShot(
    cinematicModeOverride: CinematicMode? = null,
    shotGoal: ShotGoal = ShotGoal.ESTABLISHING,
    beats: List<Beat> = emptyList()
): Shot = Shot(
    shotId = "shot_001",
    sceneId = "scene_001",
    shotNumber = 1,
    shotDescription = "A perfectly neutral, valid shot description",
    shotGoal = shotGoal,
    shotType = ShotType.MEDIUM,
    durationSeconds = 4f,
    motionLevel = MotionLevel.SUBTLE,
    beats = beats,
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

    // --- resolveEffectiveCinematicMode: اتصال واقعی Hybrid هوشمند (تکمیل Rule یتیم —
    // قدم ۲ب از ۴ زیرقدم قدم ۲، ADR-108) — فقط زمانی اجرا می‌شود که هیچ Override دستی
    // (نه شات، نه صحنه، نه Map متمرکز پروژه) وجود نداشته باشد و globalMode واقعاً
    // BALANCED باشد.

    @Test
    fun `Hybrid wiring - ACTION shotGoal always resolves to FAST_CUT regardless of beat intensity, when globalMode is BALANCED and no override exists`() {
        val dna = testDna(globalMode = CinematicMode.BALANCED)
        val scene = testScene()
        val shot = testShot(shotGoal = ShotGoal.ACTION, beats = emptyList()) // شدت خنثی (۵)، اما sceneType=="action" اولویت دارد

        assertEquals(CinematicMode.FAST_CUT, resolveEffectiveCinematicMode(dna, scene, shot))
    }

    @Test
    fun `Hybrid wiring - high beat intensity resolves to FAST_CUT even for a DIALOGUE shotGoal, when globalMode is BALANCED`() {
        val dna = testDna(globalMode = CinematicMode.BALANCED)
        val scene = testScene()
        val highIntensityBeats = listOf(beatOf(BeatEventType.SUBJECT_ACTION), beatOf(BeatEventType.SUBJECT_ACTION)) // avg = 8f >= 7f
        val shot = testShot(shotGoal = ShotGoal.DIALOGUE, beats = highIntensityBeats)

        assertEquals(CinematicMode.FAST_CUT, resolveEffectiveCinematicMode(dna, scene, shot))
    }

    @Test
    fun `Hybrid wiring - moderate beat intensity resolves to BALANCED for a DIALOGUE shotGoal, when globalMode is BALANCED`() {
        val dna = testDna(globalMode = CinematicMode.BALANCED)
        val scene = testScene()
        val moderateBeats = listOf(beatOf(BeatEventType.CAMERA_MOVE)) // avg = 6f, not >=7f and not <=3f
        val shot = testShot(shotGoal = ShotGoal.DIALOGUE, beats = moderateBeats)

        assertEquals(CinematicMode.BALANCED, resolveEffectiveCinematicMode(dna, scene, shot))
    }

    @Test
    fun `Hybrid wiring - does not engage when globalMode is explicitly non-BALANCED, even for an ACTION shotGoal`() {
        val dna = testDna(globalMode = CinematicMode.LONG_TAKE)
        val scene = testScene()
        val shot = testShot(shotGoal = ShotGoal.ACTION, beats = emptyList())

        assertEquals(CinematicMode.LONG_TAKE, resolveEffectiveCinematicMode(dna, scene, shot))
    }

    @Test
    fun `Hybrid wiring - an explicit manual override still wins over Hybrid, even when globalMode is BALANCED and shotGoal is ACTION`() {
        val dna = testDna(globalMode = CinematicMode.BALANCED)
        val scene = testScene()
        val shot = testShot(shotGoal = ShotGoal.ACTION, beats = emptyList(), cinematicModeOverride = CinematicMode.LONG_TAKE)

        assertEquals(CinematicMode.LONG_TAKE, resolveEffectiveCinematicMode(dna, scene, shot))
    }

    // --- resolveEffectiveCinematicMode: اتصال Mood صحنه (قدم ۲ج از ۴ زیرقدم قدم ۲، ADR-109) ---
    // اولویت سه‌سطحی تصمیم‌شده توسط کاربر پروژه: Beat واقعی شات > Mood صحنه > shotGoal تنها.

    @Test
    fun `Mood wiring - an empty beat sheet with a DARK-category scene mood resolves via getPacingFromEmotion to FAST_CUT`() {
        val dna = testDna(globalMode = CinematicMode.BALANCED)
        val scene = testScene(mood = Mood.TENSE) // MoodCategory.DARK -> getPacingFromEmotion -> FAST_CUT
        val shot = testShot(shotGoal = ShotGoal.ESTABLISHING, beats = emptyList())

        assertEquals(CinematicMode.FAST_CUT, resolveEffectiveCinematicMode(dna, scene, shot))
    }

    @Test
    fun `Mood wiring - the same empty-beat scenario without a scene mood falls back to the previous shotGoal-only behavior`() {
        val dna = testDna(globalMode = CinematicMode.BALANCED)
        val scene = testScene(mood = null)
        val shot = testShot(shotGoal = ShotGoal.ESTABLISHING, beats = emptyList())

        // بدون Mood صحنه: رفتار قبلی ADR-108 دست‌نخورده — ESTABLISHING + شدت خنثی -> BALANCED.
        assertEquals(CinematicMode.BALANCED, resolveEffectiveCinematicMode(dna, scene, shot))
    }

    @Test
    fun `Mood wiring - a non-empty beat sheet wins over a contradicting scene mood (Beat takes absolute priority over Mood)`() {
        val dna = testDna(globalMode = CinematicMode.BALANCED)
        // Mood به‌تنهایی به LONG_TAKE اشاره می‌کند (CALM)، اما Beat/shotGoal به FAST_CUT.
        val scene = testScene(mood = Mood.SERENE) // MoodCategory.CALM -> getPacingFromEmotion -> LONG_TAKE
        val shot = testShot(shotGoal = ShotGoal.ACTION, beats = listOf(beatOf(BeatEventType.CAMERA_MOVE)))

        assertEquals(CinematicMode.FAST_CUT, resolveEffectiveCinematicMode(dna, scene, shot))
    }

    // --- beatIntensity / averageBeatIntensity (تکمیل Rule یتیم — قدم ۲الف از ۴ زیرقدم قدم ۲، ADR-107) ---

    private fun beatOf(eventType: BeatEventType, timestampSeconds: Float = 0f): Beat =
        Beat(timestampSeconds = timestampSeconds, eventType = eventType, description = "beat")

    @Test
    fun `beatIntensity maps SUBJECT_ACTION to the highest intensity`() {
        assertEquals(8f, beatIntensity(BeatEventType.SUBJECT_ACTION))
    }

    @Test
    fun `beatIntensity maps CAMERA_MOVE below SUBJECT_ACTION but above the environmental tier`() {
        assertEquals(6f, beatIntensity(BeatEventType.CAMERA_MOVE))
    }

    @Test
    fun `beatIntensity maps ENVIRONMENTAL to a moderate-low intensity`() {
        assertEquals(4f, beatIntensity(BeatEventType.ENVIRONMENTAL))
    }

    @Test
    fun `beatIntensity maps LIGHTING_CHANGE to the lowest intensity`() {
        assertEquals(2f, beatIntensity(BeatEventType.LIGHTING_CHANGE))
    }

    @Test
    fun `beatIntensity ranking is strictly SUBJECT_ACTION greater than CAMERA_MOVE greater than ENVIRONMENTAL greater than LIGHTING_CHANGE`() {
        assertTrue(beatIntensity(BeatEventType.SUBJECT_ACTION) > beatIntensity(BeatEventType.CAMERA_MOVE))
        assertTrue(beatIntensity(BeatEventType.CAMERA_MOVE) > beatIntensity(BeatEventType.ENVIRONMENTAL))
        assertTrue(beatIntensity(BeatEventType.ENVIRONMENTAL) > beatIntensity(BeatEventType.LIGHTING_CHANGE))
    }

    @Test
    fun `averageBeatIntensity computes the correct mean across multiple beats of different types`() {
        val beats = listOf(
            beatOf(BeatEventType.SUBJECT_ACTION), // 8
            beatOf(BeatEventType.CAMERA_MOVE),    // 6
            beatOf(BeatEventType.ENVIRONMENTAL),  // 4
            beatOf(BeatEventType.LIGHTING_CHANGE) // 2
        )
        assertEquals(5f, averageBeatIntensity(beats))
    }

    @Test
    fun `averageBeatIntensity returns the single beat's own intensity for a single-item list`() {
        assertEquals(8f, averageBeatIntensity(listOf(beatOf(BeatEventType.SUBJECT_ACTION))))
    }

    @Test
    fun `averageBeatIntensity returns the neutral midpoint for an empty beat list`() {
        assertEquals(5f, averageBeatIntensity(emptyList()))
    }

    // این سه تست عمداً determineHybridPacing را فراخوانی نمی‌کنند (طبق دستور صریح
    // این زیرقدم: «به‌هیچ‌عنوان determineHybridPacing را فراخوانی/وصل نکن») — فقط
    // خودِ عدد میانگین را در برابر آستانه‌های مستندشده‌ی آن (>=7f، <=3f) می‌سنجند
    // تا سازگاری طراحی این نگاشت با آن تابع (بدون وصل‌کردن واقعی) اثبات شود؛
    // خودِ اتصال، زیرقدم ۲ب جداگانه است.

    @Test
    fun `an action-heavy beat sheet's average reaches determineHybridPacing's documented FAST_CUT threshold (avgBeatIntensity greater or equal 7f)`() {
        val avg = averageBeatIntensity(List(3) { beatOf(BeatEventType.SUBJECT_ACTION) })
        assertTrue(avg >= 7f)
    }

    @Test
    fun `a lighting-change-heavy beat sheet's average reaches determineHybridPacing's documented LONG_TAKE threshold (avgBeatIntensity less or equal 3f)`() {
        val avg = averageBeatIntensity(List(3) { beatOf(BeatEventType.LIGHTING_CHANGE) })
        assertTrue(avg <= 3f)
    }

    @Test
    fun `an empty beat sheet's neutral average falls strictly between determineHybridPacing's two thresholds`() {
        val avg = averageBeatIntensity(emptyList())
        assertTrue(avg > 3f && avg < 7f)
    }
}
