package com.operaboys.cinemashotgenerator.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.operaboys.cinemashotgenerator.data.entity.AssetEntity
import kotlinx.coroutines.flow.Flow

// واحد ۱۵ — Project Storage System (قدم ۱: DAO پایه — بدون منطق دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets WHERE projectId = :projectId")
    fun getAssetsForProject(projectId: String): Flow<List<AssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAsset(asset: AssetEntity)

    @Query("SELECT * FROM assets WHERE assetId = :id")
    suspend fun loadAsset(id: String): AssetEntity?

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)
}
