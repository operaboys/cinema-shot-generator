package com.operaboys.cinemashotgenerator.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.operaboys.cinemashotgenerator.data.entity.VersionEntity
import kotlinx.coroutines.flow.Flow

// واحد ۱۵ — Project Storage System (قدم ۱: DAO پایه — بدون منطق دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md

@Dao
interface VersionDao {
    @Query("SELECT * FROM versions WHERE entityId = :entityId ORDER BY createdAt DESC")
    fun getVersionsForEntity(entityId: String): Flow<List<VersionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVersion(version: VersionEntity)

    @Query("SELECT * FROM versions WHERE versionId = :id")
    suspend fun loadVersion(id: String): VersionEntity?

    @Delete
    suspend fun deleteVersion(version: VersionEntity)
}
