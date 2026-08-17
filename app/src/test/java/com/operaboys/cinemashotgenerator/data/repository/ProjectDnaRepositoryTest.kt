package com.operaboys.cinemashotgenerator.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
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
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicLanguageSettings
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// واحد ۱۵ — قدم ۳ (زیرقدم ۱): تست end-to-end round-trip واقعی برای ProjectDna.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProjectDnaRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ProjectDnaRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = ProjectDnaRepository(database.projectDnaDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val fullDna = ProjectDna(
        dnaId = "dna_001",
        projectId = "proj_001",
        coreIdentity = CoreIdentity(
            dominantVisualStyle = VisualStyle.CINEMATIC_STYLE,
            realismLevel = RealismLevel.GROUNDED,
            styleConsistency = StyleConsistency.STRICT,
            locked = true
        ),
        masterPalette = MasterPalette(
            colorTemperature = ColorTemperature.WARM,
            globalSaturation = SaturationLevel.MEDIUM,
            globalContrast = ContrastLevel.HIGH,
            colorGradingPreset = "natural",
            colorPalette = listOf("#3B82F6", "#8B5CF6")
        ),
        globalMoodBase = GlobalMoodBase(
            primaryEmotion = Mood.MYSTERIOUS,
            intensity = "medium",
            consistency = StyleConsistency.STRICT
        ),
        outputConstraints = OutputConstraints(
            forbiddenElements = mapOf("camera" to listOf("dutch_angle"), "weather" to listOf("snow")),
            mandatoryElements = listOf("subject_visible"),
            maxShotDurationSeconds = 10,
            aspectRatio = AspectRatio.ANAMORPHIC_2_39
        ),
        // تکمیل Rule یتیم — قدم ۱ از ۴ (ADR-106): پوشش صریح Round-Trip برای فیلد
        // تازه، با sceneOverrides غیرخالی (نه فقط پیش‌فرض) تا نگاشت Map<String, String> ↔
        // Map<String, CinematicMode> واقعاً تمرین شود.
        cinematicLanguage = CinematicLanguageSettings(
            globalMode = CinematicMode.LONG_TAKE,
            sceneOverrides = mapOf("scene_001" to CinematicMode.FAST_CUT, "scene_002" to CinematicMode.BALANCED)
        )
    )

    @Test
    fun `saveProjectDna then loadProjectDna round-trips the full structure exactly`() = runBlocking {
        database.projectDao().saveProject(
            ProjectEntity("proj_001", "Test", "2026-07-20T10:00:00Z", "2026-07-20T10:00:00Z")
        )

        val saveResult = repository.saveProjectDna(fullDna)
        assertTrue(saveResult.isSuccess)

        val loadResult = repository.loadProjectDna("proj_001")
        assertTrue(loadResult.isSuccess)
        assertEquals(fullDna, loadResult.getOrThrow())
    }

    @Test
    fun `loadProjectDna returns null when no dna exists for the project`() = runBlocking {
        database.projectDao().saveProject(
            ProjectEntity("proj_002", "Test", "2026-07-20T10:00:00Z", "2026-07-20T10:00:00Z")
        )
        val result = repository.loadProjectDna("proj_002")
        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrThrow())
    }
}
