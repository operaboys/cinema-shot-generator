package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.entity.StoryContextEntity
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.story.CompletionStatus
import com.operaboys.cinemashotgenerator.domain.story.Genre
import com.operaboys.cinemashotgenerator.domain.story.NarrativeIntensity
import com.operaboys.cinemashotgenerator.domain.story.StoryContext
import com.operaboys.cinemashotgenerator.domain.story.StoryType
import com.operaboys.cinemashotgenerator.domain.story.VisualIntent

// واحد ۱۶ فاز ۲ — قدم ۱: Mapper دستی Entity↔Domain برای StoryContext، هم‌الگو با
// ProjectMappers.kt (Language.valueOf/EntityState.valueOf) — نه kotlinx.serialization،
// چون فیلدها مسطح‌اند نه JSON.

fun StoryContextEntity.toDomain(): StoryContext = StoryContext(
    storyType = StoryType.valueOf(storyType),
    genre = if (genre.isBlank()) emptyList() else genre.split(",").map { Genre.valueOf(it) },
    moodPrimary = Mood.valueOf(moodPrimary),
    moodSecondary = moodSecondary?.let { Mood.valueOf(it) },
    narrativeIntensity = NarrativeIntensity.valueOf(narrativeIntensity),
    visualIntent = VisualIntent.valueOf(visualIntent),
    createdAt = createdAt,
    completionStatus = CompletionStatus.valueOf(completionStatus)
)

fun StoryContext.toEntity(projectId: String): StoryContextEntity = StoryContextEntity(
    projectId = projectId,
    storyType = storyType.name,
    genre = genre.joinToString(",") { it.name },
    moodPrimary = moodPrimary.name,
    moodSecondary = moodSecondary?.name,
    narrativeIntensity = narrativeIntensity.name,
    visualIntent = visualIntent.name,
    createdAt = createdAt,
    completionStatus = completionStatus.name
)
