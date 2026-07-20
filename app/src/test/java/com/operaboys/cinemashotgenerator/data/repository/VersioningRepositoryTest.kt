package com.operaboys.cinemashotgenerator.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.entity.VersionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// واحد ۱۵ — قدم ۲: تست end-to-end واقعی: نسخه‌ی هدف در VersionDao ذخیره می‌شود →
// VersioningRepository.rollback آن را می‌خواند → rollbackToVersion واقعی واحد ۱۲ اجرا
// می‌شود (با createSnapshot/logEvent تزریق‌شده‌ی واقعی) → نسخه‌ی جدید در VersionDao و
// رویداد rollback_performed در EventLogDao واقعاً ذخیره شده‌اند.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VersioningRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: VersioningRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = VersioningRepository(
            database.versionDao(),
            database.eventLogDao(),
            idProvider = { "v_test0001" },
            clock = { "2026-07-20T12:00:00Z" }
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `rollback persists a new version and logs the rollback event`() = runBlocking {
        val targetVersion = VersionEntity(
            versionId = "v1.0.0",
            entityId = "shot_001",
            versionType = "safe",
            snapshotJson = "{\"shotDescription\":\"original\"}",
            createdAt = "2026-07-19T10:00:00Z",
            changeSummary = "نسخه‌ی اولیه",
            modifiedFieldsJson = "[]",
            parentVersionId = null
        )
        database.versionDao().saveVersion(targetVersion)

        val result = repository.rollback("v1.0.0")

        assertTrue(result.isSuccess)
        val newVersion = result.getOrThrow()
        assertEquals("v_test0001", newVersion.versionId)
        assertEquals("shot_001", newVersion.entityId)
        assertEquals("{\"shotDescription\":\"original\"}", newVersion.snapshotData)
        assertEquals("v1.0.0", newVersion.parentVersionId)

        val persisted = database.versionDao().loadVersion("v_test0001")
        assertEquals("shot_001", persisted!!.entityId)

        val events = database.eventLogDao().getEventsForEntity("shot_001").first()
        assertEquals(1, events.size)
        assertEquals("rollback_performed", events[0].eventType)
    }

    @Test
    fun `rollback fails when the target version does not exist`() = runBlocking {
        val result = repository.rollback("v_missing")
        assertTrue(result.isFailure)
    }
}
