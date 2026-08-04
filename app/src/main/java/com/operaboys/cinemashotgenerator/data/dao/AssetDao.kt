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

    // واحد ۱۶ فاز ۳ — قدم ۱: صفحه‌ی Asset Library به «همه‌ی Asset های یک نوع در یک
    // پروژه» نیاز دارد (نه فقط load(ids) که شناسه‌ها را از قبل معلوم فرض می‌کند).
    // یک Query پارامتری‌شده روی assetType (نه سه متد جدا برای هر نوع) چون جدول assets
    // یک جدول عمومی واحد است (بدون جدول جدا به‌ازای هر نوع) — سه متد جدا فقط سه نسخه‌ی
    // تقریباً یکسان از همین Query تولیدشده‌ی Room می‌بودند؛ تفکیک نوع‌محور واقعی در
    // سطح Repository (AssetRepository.loadAllCharacterAssets/loadAllLocationAssets/
    // loadAllObjectAssets) انجام می‌شود، جایی که خروجی به نوع Kotlin واقعی هم Map
    // می‌شود. جزئیات در docs/adr/048-unit16-phase3-step1-asset-library.md.
    @Query("SELECT * FROM assets WHERE projectId = :projectId AND assetType = :assetType")
    fun getAssetsForProjectByType(projectId: String, assetType: String): Flow<List<AssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAsset(asset: AssetEntity)

    @Query("SELECT * FROM assets WHERE assetId = :id")
    suspend fun loadAsset(id: String): AssetEntity?

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)
}
