package com.operaboys.cinemashotgenerator.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.operaboys.cinemashotgenerator.data.dao.AssetDao
import com.operaboys.cinemashotgenerator.data.dao.DependencyEdgeDao
import com.operaboys.cinemashotgenerator.data.dao.OverrideDao
import com.operaboys.cinemashotgenerator.data.dao.ProjectDao
import com.operaboys.cinemashotgenerator.data.dao.ProjectTransactionDao
import com.operaboys.cinemashotgenerator.data.dao.PromptBlueprintDao
import com.operaboys.cinemashotgenerator.data.dao.RenderedOutputDao
import com.operaboys.cinemashotgenerator.data.dao.SceneDao
import com.operaboys.cinemashotgenerator.data.dao.ShotDao
import com.operaboys.cinemashotgenerator.data.dao.VersionDao
import com.operaboys.cinemashotgenerator.data.entity.AssetEntity
import com.operaboys.cinemashotgenerator.data.entity.DependencyEdgeEntity
import com.operaboys.cinemashotgenerator.data.entity.OverrideEntity
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.data.entity.PromptBlueprintEntity
import com.operaboys.cinemashotgenerator.data.entity.RenderedOutputEntity
import com.operaboys.cinemashotgenerator.data.entity.SceneEntity
import com.operaboys.cinemashotgenerator.data.entity.ShotEntity
import com.operaboys.cinemashotgenerator.data.entity.VersionEntity

// واحد ۱۵ — Project Storage System (قدم ۱: Entity/DAO/Database — بدون اتصال به دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md
//
// یک دیتابیس واحد با تمام Entity ها (نه فایل جدا per پروژه) — دقیقاً طبق تصمیم معماری
// «یک دیتابیس، چند پروژه» بلوپرینت. version=1 چون هنوز هیچ Migration واقعی لازم
// نیست (خارج از Scope این قدم). بدون فریمورک DI — طبق تصمیم قبلی پروژه — یک
// Singleton Provider ساده با Room.databaseBuilder.

@Database(
    entities = [
        ProjectEntity::class,
        SceneEntity::class,
        ShotEntity::class,
        AssetEntity::class,
        PromptBlueprintEntity::class,
        RenderedOutputEntity::class,
        OverrideEntity::class,
        VersionEntity::class,
        DependencyEdgeEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun sceneDao(): SceneDao
    abstract fun shotDao(): ShotDao
    abstract fun assetDao(): AssetDao
    abstract fun promptBlueprintDao(): PromptBlueprintDao
    abstract fun renderedOutputDao(): RenderedOutputDao
    abstract fun overrideDao(): OverrideDao
    abstract fun versionDao(): VersionDao
    abstract fun dependencyEdgeDao(): DependencyEdgeDao
    abstract fun projectTransactionDao(): ProjectTransactionDao

    companion object {
        private const val DATABASE_NAME = "cinema_shot_generator.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
        }
    }
}
