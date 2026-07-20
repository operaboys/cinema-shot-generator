package com.operaboys.cinemashotgenerator.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import com.operaboys.cinemashotgenerator.data.entity.SceneEntity
import com.operaboys.cinemashotgenerator.data.entity.ShotEntity

// واحد ۱۵ — Project Storage System (قدم ۱: DAO پایه — بدون منطق دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md
//
// فقط برای اثبات کارکرد Atomicity در این قدم، بدون اتصال به منطق واقعی State &
// Versioning (واحد ۱۲) که کار قدم دوم است. abstract class (نه interface) چون
// @Transaction به یک بدنه‌ی متد واقعی نیاز دارد که متدهای دیگر همین کلاس را فراخوانی
// کند.
//
// ⚠️ ترتیب فراخوانی عمداً برخلاف ترتیب نوشتاری کد مفهومی بلوپرینت (saveShot سپس
// saveScene) برعکس شد: saveScene ابتدا فراخوانی می‌شود. این یک باگ واقعی بود که با
// تست AppDatabaseDaoTest کشف شد، نه یک تصمیم سلیقه‌ای — جزئیات کامل در
// docs/adr/017-unit15-project-storage-deviations.md.

@Dao
abstract class ProjectTransactionDao {
    @Transaction
    open suspend fun saveShotWithSceneUpdate(shot: ShotEntity, updatedScene: SceneEntity) {
        saveScene(updatedScene)
        saveShot(shot)
        // اگر هرکدام شکست بخورد، Room کل تراکنش را Rollback می‌کند
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveShot(shot: ShotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveScene(scene: SceneEntity)
}
