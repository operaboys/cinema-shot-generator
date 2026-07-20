package com.operaboys.cinemashotgenerator.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.operaboys.cinemashotgenerator.data.entity.SceneEntity
import kotlinx.coroutines.flow.Flow

// واحد ۱۵ — Project Storage System (قدم ۱: DAO پایه — بدون منطق دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md
//
// دو متد اول دقیقاً طبق کد مفهومی بلوپرینت؛ loadScene/deleteScene برای «حداقل عملیات
// CRUD پایه» صریح این قدم اضافه شدند (بلوپرینت فقط نمونه‌ی جزئی داده بود، نه CRUD کامل).

@Dao
interface SceneDao {
    @Query("SELECT * FROM scenes WHERE projectId = :projectId")
    fun getScenesForProject(projectId: String): Flow<List<SceneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveScene(scene: SceneEntity)

    @Query("SELECT * FROM scenes WHERE sceneId = :id")
    suspend fun loadScene(id: String): SceneEntity?

    @Delete
    suspend fun deleteScene(scene: SceneEntity)
}
