package com.operaboys.cinemashotgenerator.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.operaboys.cinemashotgenerator.data.entity.DependencyEdgeEntity
import kotlinx.coroutines.flow.Flow

// واحد ۱۵ — Project Storage System (قدم ۱: DAO پایه — بدون منطق دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md

@Dao
interface DependencyEdgeDao {
    @Query("SELECT * FROM dependency_edges WHERE sourceId = :sourceId")
    fun getEdgesFromSource(sourceId: String): Flow<List<DependencyEdgeEntity>>

    @Query("SELECT * FROM dependency_edges")
    suspend fun getAllEdges(): List<DependencyEdgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEdge(edge: DependencyEdgeEntity)

    @Delete
    suspend fun deleteEdge(edge: DependencyEdgeEntity)
}
