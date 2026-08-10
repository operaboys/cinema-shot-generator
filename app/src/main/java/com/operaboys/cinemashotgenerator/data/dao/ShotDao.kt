package com.operaboys.cinemashotgenerator.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.operaboys.cinemashotgenerator.data.entity.ShotEntity
import kotlinx.coroutines.flow.Flow

// واحد ۱۵ — Project Storage System (قدم ۱: DAO پایه — بدون منطق دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md
//
// getShotsPaged دقیقاً طبق کد مفهومی بخش «مقیاس‌پذیری» بلوپرینت (پروژه‌های با ۱۰۰۰+
// Shot باید Pagination داشته باشند، نه Load کامل). getShotsForScene/save/load/delete
// برای CRUD پایه اضافه شدند (بلوپرینت فقط نسخه‌ی Paged را نشان داده بود).

@Dao
interface ShotDao {
    @Query("SELECT * FROM shots WHERE sceneId = :sceneId")
    fun getShotsForScene(sceneId: String): Flow<List<ShotEntity>>

    // رفع G22 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، docs/adr/062-...):
    // برای Rule واقعی «Asset در حال استفاده قابل حذف نیست» (validateAssetDeletion،
    // AssetValidation.kt) به همه‌ی Shot های یک پروژه (نه فقط یک Scene) نیاز است —
    // Asset یک مفهوم سطح-پروژه است، نه سطح-صحنه. shotDataJson یک Blob است (نه
    // ستون‌های جدا)، پس فیلتر «کدام Shot از این assetId استفاده می‌کند» در لایه‌ی
    // Repository (بعد از Deserialize) انجام می‌شود، نه اینجا با SQL.
    @Query("SELECT sh.* FROM shots sh WHERE sh.sceneId IN (SELECT sceneId FROM scenes WHERE projectId = :projectId)")
    suspend fun getShotsForProject(projectId: String): List<ShotEntity>

    // برای پروژه‌های بزرگ (۱۰۰۰+ Shot)، Paging به‌جای بارگذاری کامل
    @Query("SELECT * FROM shots WHERE sceneId = :sceneId LIMIT :limit OFFSET :offset")
    suspend fun getShotsPaged(sceneId: String, limit: Int, offset: Int): List<ShotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveShot(shot: ShotEntity)

    @Query("SELECT * FROM shots WHERE shotId = :id")
    suspend fun loadShot(id: String): ShotEntity?

    @Delete
    suspend fun deleteShot(shot: ShotEntity)
}
