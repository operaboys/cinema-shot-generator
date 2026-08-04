package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.StoryDao
import com.operaboys.cinemashotgenerator.domain.story.StoryContext

// واحد ۱۶ فاز ۲ — قدم ۱: اتصال واقعی StoryContext (واحد ۰۱) به StoryDao — رفع کمبود
// ذخیره‌سازی مستندشده در docs/adr/045-unit16-phase2-step1-story-tab.md.

class StoryRepository(private val storyDao: StoryDao) {
    suspend fun saveStoryContext(projectId: String, context: StoryContext): Result<Unit> = runCatching {
        storyDao.saveStoryContext(context.toEntity(projectId))
    }

    suspend fun loadStoryContext(projectId: String): Result<StoryContext?> = runCatching {
        storyDao.loadStoryContextForProject(projectId)?.toDomain()
    }
}
