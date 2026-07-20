package com.operaboys.cinemashotgenerator.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.data.entity.SceneEntity
import com.operaboys.cinemashotgenerator.data.entity.ShotEntity
import com.operaboys.cinemashotgenerator.domain.audio.AmbientSound
import com.operaboys.cinemashotgenerator.domain.audio.AudioContext
import com.operaboys.cinemashotgenerator.domain.shot.ActionSound
import com.operaboys.cinemashotgenerator.domain.shot.CharacterSound
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// واحد ۱۵ — قدم ۳ (زیرقدم ۲): تست end-to-end round-trip واقعی برای AudioContext.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioContextRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: AudioContextRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = AudioContextRepository(database.audioContextDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val fullAudioContext = AudioContext(
        audioContextId = "audio_001",
        shotId = "shot_001",
        ambientSounds = listOf(AmbientSound("rain", "heavy", "heavy rain on surfaces", "weather")),
        actionSounds = listOf(ActionSound(1.5f, "footstep", "footsteps on wet pavement")),
        characterSounds = listOf(CharacterSound("char_001", "breathing", "heavy breathing"))
    )

    @Test
    fun `saveAudioContext then loadAudioContext round-trips the full structure exactly`() = runBlocking {
        database.projectDao().saveProject(
            ProjectEntity("proj_001", "Test", "2026-07-20T10:00:00Z", "2026-07-20T10:00:00Z")
        )
        database.sceneDao().saveScene(SceneEntity("scene_001", "proj_001", "{}"))
        database.shotDao().saveShot(ShotEntity("shot_001", "scene_001", "{}"))

        val saveResult = repository.saveAudioContext(fullAudioContext)
        assertTrue(saveResult.isSuccess)

        val loadResult = repository.loadAudioContext("shot_001")
        assertTrue(loadResult.isSuccess)
        assertEquals(fullAudioContext, loadResult.getOrThrow())
    }

    @Test
    fun `loadAudioContext returns null when none exists for the shot`() = runBlocking {
        val result = repository.loadAudioContext("shot_missing")
        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrThrow())
    }
}
