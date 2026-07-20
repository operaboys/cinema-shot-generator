package com.operaboys.cinemashotgenerator.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.entity.DependencyEdgeEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// واحد ۱۵ — قدم ۲: تست end-to-end واقعی: یال‌های وابستگی در DependencyEdgeDao ذخیره
// می‌شوند → ImpactAnalysisRepository.analyzeImpact آن‌ها را می‌خواند → analyzeVersionImpact
// واقعی واحد ۱۲ (با findDependents تزریق‌شده‌ی واقعی) روی آن‌ها اجرا می‌شود.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImpactAnalysisRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ImpactAnalysisRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = ImpactAnalysisRepository(database.dependencyEdgeDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `analyzeImpact finds real dependents when a dependency field changed`() = runBlocking {
        database.dependencyEdgeDao().saveEdge(DependencyEdgeEntity(sourceId = "shot_001", targetId = "shot_002", type = "STRONG"))
        database.dependencyEdgeDao().saveEdge(DependencyEdgeEntity(sourceId = "shot_001", targetId = "shot_003", type = "WEAK"))

        val result = repository.analyzeImpact(entityId = "shot_001", entityType = "shot", modifiedFields = listOf("scene_id"))

        assertTrue(result.dependencyChanges)
        assertEquals("high", result.riskLevel)
        assertEquals(setOf("shot_002", "shot_003"), result.affectedEntityIds.toSet())
    }

    @Test
    fun `analyzeImpact is critical risk for project_dna regardless of edges`() = runBlocking {
        val result = repository.analyzeImpact(entityId = "dna_001", entityType = "project_dna", modifiedFields = listOf("shot_description"))
        assertEquals("critical", result.riskLevel)
    }

    @Test
    fun `analyzeImpact is low risk with only the entity itself affected when nothing changed`() = runBlocking {
        val result = repository.analyzeImpact(entityId = "shot_001", entityType = "shot", modifiedFields = listOf("shot_description"))
        assertEquals("low", result.riskLevel)
        assertEquals(listOf("shot_001"), result.affectedEntityIds)
    }
}
