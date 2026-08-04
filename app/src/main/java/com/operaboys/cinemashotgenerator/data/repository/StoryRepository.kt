package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.StoryBreakdownSessionDao
import com.operaboys.cinemashotgenerator.data.dao.StoryDao
import com.operaboys.cinemashotgenerator.data.entity.StoryBreakdownSessionEntity
import com.operaboys.cinemashotgenerator.domain.story.StoryContext

// واحد ۱۶ فاز ۲ — قدم ۱: اتصال واقعی StoryContext (واحد ۰۱) به StoryDao — رفع کمبود
// ذخیره‌سازی مستندشده در docs/adr/045-unit16-phase2-step1-story-tab.md.
//
// قدم ۲: متدهای Session پیش‌نویس AI Story Breakdown هم به همین کلاس اضافه شدند (نه
// یک Repository کاملاً جدا) — چون هر دو داده‌ی «ناحیه‌ی داستان» یک پروژه‌اند؛ فقط
// خودِ Entity ها مستقل ماندند (StoryBreakdownSessionEntity، جزئیات در
// docs/adr/046-unit16-phase2-step2-ai-story-breakdown.md).

/** بدون `projectId` عمداً — فراخوان همیشه از قبل projectId را می‌داند (پارامتر ورودی خودِ Repository). */
data class StoryBreakdownSession(
    val freeformStory: String,
    val targetShotCount: Int,
    val defaultShotDurationSeconds: Float
)

class StoryRepository(
    private val storyDao: StoryDao,
    private val storyBreakdownSessionDao: StoryBreakdownSessionDao
) {
    suspend fun saveStoryContext(projectId: String, context: StoryContext): Result<Unit> = runCatching {
        storyDao.saveStoryContext(context.toEntity(projectId))
    }

    suspend fun loadStoryContext(projectId: String): Result<StoryContext?> = runCatching {
        storyDao.loadStoryContextForProject(projectId)?.toDomain()
    }

    suspend fun saveBreakdownSession(
        projectId: String,
        freeformStory: String,
        targetShotCount: Int,
        defaultShotDurationSeconds: Float
    ): Result<Unit> = runCatching {
        storyBreakdownSessionDao.saveSession(
            StoryBreakdownSessionEntity(
                projectId = projectId,
                freeformStory = freeformStory,
                targetShotCount = targetShotCount,
                defaultShotDurationSeconds = defaultShotDurationSeconds
            )
        )
    }

    suspend fun loadBreakdownSession(projectId: String): Result<StoryBreakdownSession?> = runCatching {
        storyBreakdownSessionDao.loadSessionForProject(projectId)?.let {
            StoryBreakdownSession(it.freeformStory, it.targetShotCount, it.defaultShotDurationSeconds)
        }
    }
}
