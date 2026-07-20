package com.operaboys.cinemashotgenerator.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.operaboys.cinemashotgenerator.data.entity.OverrideEntity
import kotlinx.coroutines.flow.Flow

// واحد ۱۵ — Project Storage System (قدم ۱: DAO پایه — بدون منطق دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md

@Dao
interface OverrideDao {
    @Query("SELECT * FROM overrides WHERE entityId = :entityId")
    fun getOverridesForEntity(entityId: String): Flow<List<OverrideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOverride(override: OverrideEntity)

    @Query("SELECT * FROM overrides WHERE overrideId = :id")
    suspend fun loadOverride(id: String): OverrideEntity?

    @Delete
    suspend fun deleteOverride(override: OverrideEntity)
}
