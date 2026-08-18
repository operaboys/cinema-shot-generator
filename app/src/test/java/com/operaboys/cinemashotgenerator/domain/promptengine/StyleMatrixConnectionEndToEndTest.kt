package com.operaboys.cinemashotgenerator.domain.promptengine

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
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
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
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
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.visualidentity.StyleInfluence
import com.operaboys.cinemashotgenerator.domain.visualidentity.toStyleReference
import com.operaboys.cinemashotgenerator.ui.dna.DnaViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// اتصال کامل Style Matrix — قدم ۴-اتصال از ۸ زیرقدم (آخرین زیرقدم، ADR-117):
// تست End-to-End واقعی، طبق الزام صریح دستور کار این قدم — تنها راه اثبات
// واقعی که کل زنجیره‌ی ۸ زیرقدم (ADR-113 تا ADR-117) واقعاً به‌هم وصل است،
// نه فقط تک‌تک قطعات جدا. برخلاف تست‌های PromptAssemblyTest.kt (که یک
// ProjectDna دستی می‌سازند)، این تست از خودِ DnaViewModel واقعی
// (setSecondaryVisualStyle/setStyleInfluence، ADR-113/115) + Repository
// واقعی Room (ذخیره/بارگذاری واقعی، نه شبیه‌سازی‌شده) استفاده می‌کند —
// دقیقاً همان مسیری که یک کاربر واقعی در تب DNA طی می‌کند.

private const val PROJECT_ID = "proj_style_matrix_e2e"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StyleMatrixConnectionEndToEndTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ProjectDnaRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = ProjectDnaRepository(database.projectDnaDao())
        runBlocking {
            ProjectRepository(database.projectDao(), idProvider = { PROJECT_ID }).createProject("Style Matrix E2E Test").getOrThrow()
        }
    }

    private fun awaitRepositoryState(timeoutMs: Long = 3000, predicate: (ProjectDna) -> Boolean): ProjectDna {
        val deadline = System.currentTimeMillis() + timeoutMs
        var last: ProjectDna? = null
        while (System.currentTimeMillis() < deadline) {
            last = runBlocking { repository.loadProjectDna(PROJECT_ID).getOrThrow() }
            if (last != null && predicate(last)) return last
            Thread.sleep(5)
        }
        assertTrue("condition was never met within ${timeoutMs}ms, last value=$last", false)
        error("unreachable")
    }

    private fun neutralPromptGenerationInput(dna: ProjectDna) = PromptGenerationInput(
        dna = dna,
        scene = Scene(
            sceneId = "scene_e2e",
            sceneNumber = 1,
            narrativeRole = NarrativeRole.DEVELOPMENT,
            location = SceneLocation(LocationType.MIXED, "a neutral location"),
            timeOfDay = TimeOfDay.AFTERNOON,
            atmospherePrimary = Atmosphere.CALM
        ),
        shot = Shot(
            shotId = "shot_e2e",
            sceneId = "scene_e2e",
            shotNumber = 1,
            shotDescription = "a neutral shot",
            shotGoal = ShotGoal.ESTABLISHING,
            shotType = ShotType.MEDIUM,
            durationSeconds = 4f,
            motionLevel = MotionLevel.SUBTLE,
            soundProfile = SoundProfile(enabled = false)
        ),
        characters = emptyList(),
        objects = emptyList(),
        locations = emptyList(),
        camera = CameraSettings(
            angle = CameraAngle.EYE_LEVEL,
            distance = CameraDistance.MEDIUM,
            movement = CameraMovement.Basic(type = BasicMovementType.STATIC),
            lensType = LensType.STANDARD,
            depthOfField = DepthOfField.MEDIUM,
            focusMode = FocusMode.SUBJECT_TRACKING,
            stabilization = Stabilization.GIMBAL,
            framing = Framing.RULE_OF_THIRDS
        ),
        lighting = LightingSettings(
            style = LightingStyle.NATURAL_LIGHT,
            keyLightPosition = KeyLightPosition.FRONT,
            contrastRatio = ContrastRatio.LOW
        ),
        environment = EnvironmentSettings(weatherType = WeatherType.CLEAR),
        audioContext = null
    )

    @Test
    fun `setting Secondary Style and Influence through the real DnaViewModel actually changes assemblePromptBlueprint's styleModifiers`() {
        val viewModel = DnaViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = PROJECT_ID,
            repository = repository,
            idProvider = { "dna_style_matrix_e2e" },
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )

        // پیش‌فرض dominantVisualStyle، CINEMATIC_STYLE است (defaultProjectDna).
        viewModel.setSecondaryVisualStyle(VisualStyle.STUDIO_GHIBLI)
        awaitRepositoryState { it.coreIdentity.secondaryStyle == VisualStyle.STUDIO_GHIBLI }
        viewModel.setStyleInfluence(StyleInfluence.STRONG)
        val loadedDna = awaitRepositoryState { it.coreIdentity.influence == StyleInfluence.STRONG }

        val blueprint = assemblePromptBlueprint(
            input = neutralPromptGenerationInput(loadedDna),
            useSeed = false,
            weightedTags = null,
            validationIssues = emptyList()
        )

        val styleModifiers = blueprint.structuredParts.styleModifiers
        assertTrue(
            "styleModifiers باید promptTokens سبک اصلی را داشته باشد: $styleModifiers",
            styleModifiers.contains(VisualStyle.CINEMATIC_STYLE.toStyleReference().promptTokens)
        )
        assertTrue(
            "styleModifiers باید promptTokens سبک ثانویه را داشته باشد: $styleModifiers",
            styleModifiers.contains(VisualStyle.STUDIO_GHIBLI.toStyleReference().promptTokens)
        )
        assertTrue(
            "styleModifiers باید Modifier درست بر اساس Influence=STRONG را داشته باشد: $styleModifiers",
            styleModifiers.contains("strongly influenced by")
        )
    }
}
