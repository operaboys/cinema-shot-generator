package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.project.Project
import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState
import org.junit.Assert.assertEquals
import org.junit.Test

// واحد ۱۶ فاز ۱ — تست round-trip ProjectEntity ↔ Project، شامل فیلد state (رفع
// یافته‌ی پیش‌گیرانه‌ی مشابه ADR-036/ADR-038: بدون این تست، بازنشانی خاموش state
// حین نگاشت هرگز کشف نمی‌شد).

class ProjectMappersTest {

    @Test
    fun `ProjectEntity toDomain then toEntity round-trips every field exactly, for every EntityState`() {
        EntityState.entries.forEach { state ->
            val entity = ProjectEntity(
                projectId = "proj_001",
                projectName = "Test Project",
                createdAt = "2026-08-01T00:00:00Z",
                lastModified = "2026-08-02T00:00:00Z",
                uiLanguage = "en",
                state = state.name
            )
            assertEquals(entity, entity.toDomain().toEntity())
        }
    }

    @Test
    fun `toDomain correctly parses uiLanguage and state from their raw string storage`() {
        val entity = ProjectEntity(
            projectId = "proj_001",
            projectName = "Test",
            createdAt = "2026-08-01T00:00:00Z",
            lastModified = "2026-08-01T00:00:00Z",
            uiLanguage = "fa",
            state = "LOCKED"
        )

        val domain = entity.toDomain()

        assertEquals(Language.FA, domain.uiLanguage)
        assertEquals(EntityState.LOCKED, domain.state)
    }

    @Test
    fun `Project toEntity serializes uiLanguage and state back to their expected raw strings`() {
        val project = Project(
            projectId = "proj_001",
            projectName = "Test",
            createdAt = "2026-08-01T00:00:00Z",
            lastModified = "2026-08-01T00:00:00Z",
            uiLanguage = Language.EN,
            state = EntityState.REVIEW
        )

        val entity = project.toEntity()

        assertEquals("en", entity.uiLanguage)
        assertEquals("REVIEW", entity.state)
    }
}
