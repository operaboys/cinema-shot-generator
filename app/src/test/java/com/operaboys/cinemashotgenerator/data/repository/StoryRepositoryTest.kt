package com.operaboys.cinemashotgenerator.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.story.CompletionStatus
import com.operaboys.cinemashotgenerator.domain.story.Genre
import com.operaboys.cinemashotgenerator.domain.story.NarrativeIntensity
import com.operaboys.cinemashotgenerator.domain.story.StoryContext
import com.operaboys.cinemashotgenerator.domain.story.StoryType
import com.operaboys.cinemashotgenerator.domain.story.VisualIntent
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// واحد ۱۶ فاز ۲ — قدم ۱: تست Round-Trip واقعی StoryRepository — رفع کمبود
// ذخیره‌سازی مستندشده در docs/adr/045-unit16-phase2-step1-story-tab.md (StoryContext
// قبل از این قدم هیچ مسیر Persist ای نداشت).

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StoryRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: StoryRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = StoryRepository(database.storyDao(), database.storyBreakdownSessionDao())
        runBlocking {
            database.projectDao().saveProject(ProjectEntity("proj_story_test", "Test", "2026-08-04T10:00:00Z", "2026-08-04T10:00:00Z"))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val fullContext = StoryContext(
        storyType = StoryType.NARRATIVE,
        genre = listOf(Genre.ACTION, Genre.SCI_FI),
        moodPrimary = Mood.EPIC,
        moodSecondary = Mood.TENSE,
        narrativeIntensity = NarrativeIntensity.HIGH,
        visualIntent = VisualIntent.CINEMATIC,
        createdAt = "2026-08-04T10:00:00Z",
        completionStatus = CompletionStatus.COMPLETE
    )

    @Test
    fun `saveStoryContext then loadStoryContext returns an equal StoryContext (full round-trip)`() = runBlocking {
        repository.saveStoryContext("proj_story_test", fullContext).getOrThrow()

        val loaded = repository.loadStoryContext("proj_story_test").getOrThrow()

        assertEquals(fullContext, loaded)
    }

    @Test
    fun `loadStoryContext returns null when no StoryContext was ever saved for the project`() = runBlocking {
        val loaded = repository.loadStoryContext("proj_story_test").getOrThrow()

        assertNull(loaded)
    }

    @Test
    fun `saveStoryContext round-trips an empty genre list and a null moodSecondary correctly`() = runBlocking {
        val minimalContext = fullContext.copy(genre = emptyList(), moodSecondary = null)

        repository.saveStoryContext("proj_story_test", minimalContext).getOrThrow()
        val loaded = repository.loadStoryContext("proj_story_test").getOrThrow()

        assertEquals(emptyList<Genre>(), loaded?.genre)
        assertNull(loaded?.moodSecondary)
    }

    @Test
    fun `saveStoryContext overwrites the previous StoryContext for the same project (REPLACE)`() = runBlocking {
        repository.saveStoryContext("proj_story_test", fullContext).getOrThrow()
        val updated = fullContext.copy(storyType = StoryType.EXPERIMENTAL, genre = listOf(Genre.HORROR))

        repository.saveStoryContext("proj_story_test", updated).getOrThrow()
        val loaded = repository.loadStoryContext("proj_story_test").getOrThrow()

        assertEquals(StoryType.EXPERIMENTAL, loaded?.storyType)
        assertEquals(listOf(Genre.HORROR), loaded?.genre)
    }
}
