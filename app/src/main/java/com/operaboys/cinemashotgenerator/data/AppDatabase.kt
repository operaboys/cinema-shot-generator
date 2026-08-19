package com.operaboys.cinemashotgenerator.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.operaboys.cinemashotgenerator.data.dao.AssetDao
import com.operaboys.cinemashotgenerator.data.dao.AudioContextDao
import com.operaboys.cinemashotgenerator.data.dao.DependencyEdgeDao
import com.operaboys.cinemashotgenerator.data.dao.EventLogDao
import com.operaboys.cinemashotgenerator.data.dao.OverrideDao
import com.operaboys.cinemashotgenerator.data.dao.ProjectDao
import com.operaboys.cinemashotgenerator.data.dao.ProjectDnaDao
import com.operaboys.cinemashotgenerator.data.dao.ProjectTransactionDao
import com.operaboys.cinemashotgenerator.data.dao.PromptBlueprintDao
import com.operaboys.cinemashotgenerator.data.dao.RenderedOutputDao
import com.operaboys.cinemashotgenerator.data.dao.SceneDao
import com.operaboys.cinemashotgenerator.data.dao.ShotDao
import com.operaboys.cinemashotgenerator.data.dao.StoryBreakdownSessionDao
import com.operaboys.cinemashotgenerator.data.dao.StoryDao
import com.operaboys.cinemashotgenerator.data.dao.VersionDao
import com.operaboys.cinemashotgenerator.data.entity.AssetEntity
import com.operaboys.cinemashotgenerator.data.entity.AudioContextEntity
import com.operaboys.cinemashotgenerator.data.entity.DependencyEdgeEntity
import com.operaboys.cinemashotgenerator.data.entity.EventLogEntity
import com.operaboys.cinemashotgenerator.data.entity.OverrideEntity
import com.operaboys.cinemashotgenerator.data.entity.ProjectDnaEntity
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.data.entity.PromptBlueprintEntity
import com.operaboys.cinemashotgenerator.data.entity.RenderedOutputEntity
import com.operaboys.cinemashotgenerator.data.entity.SceneEntity
import com.operaboys.cinemashotgenerator.data.entity.ShotEntity
import com.operaboys.cinemashotgenerator.data.entity.StoryBreakdownSessionEntity
import com.operaboys.cinemashotgenerator.data.entity.StoryContextEntity
import com.operaboys.cinemashotgenerator.data.entity.VersionEntity

// واحد ۱۵ — Project Storage System (قدم ۱: Entity/DAO/Database — بدون اتصال به دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md
//
// یک دیتابیس واحد با تمام Entity ها (نه فایل جدا per پروژه) — دقیقاً طبق تصمیم معماری
// «یک دیتابیس، چند پروژه» بلوپرینت. version=1 چون هنوز هیچ نسخه‌ای منتشر نشده (نه در
// قدم ۱، نه در قدم ۲ که EventLogEntity را اضافه کرد — تأییدشده توسط معمار). بدون
// فریمورک DI — طبق تصمیم قبلی پروژه — یک Singleton Provider ساده با
// Room.databaseBuilder.
//
// EventLogEntity (قدم ۲): برای StateVersioningEventLogger واقعی واحد ۱۲ — جزئیات در
// docs/adr/018-unit15-step2-repository-deviations.md.
// ProjectDnaEntity (قدم ۳، زیرقدم ۱): بلوپرینت ۱۵ اصلاً Entity ای برای ProjectDna
// فهرست نکرده بود — جزئیات در docs/adr/019-unit15-step3a-dna-asset-deviations.md.
// AudioContextEntity (قدم ۳، زیرقدم ۲): همان دلیل، برای AudioContext — جزئیات در
// docs/adr/020-unit15-step3b-audio-collectdata-deviations.md.
// StoryContextEntity (واحد ۱۶ فاز ۲، قدم ۱): همان الگو، برای StoryContext واحد ۰۱
// که تا این قدم اصلاً هیچ مسیر ذخیره‌سازی‌ای نداشت — جزئیات در
// docs/adr/045-unit16-phase2-step1-story-tab.md.
// StoryBreakdownSessionEntity (واحد ۱۶ فاز ۲، قدم ۲): پیش‌نویس در‌حال‌کار AI Story
// Breakdown (واحد ۰۱ب فاز ۱) — جزئیات در
// docs/adr/046-unit16-phase2-step2-ai-story-breakdown.md.
//
// نسخه ۲ (قدم ۱ از ۳ زیرقدم «سیستم Preview دوزبانه‌ی پرامپت»، ADR-121): اولین
// Migration واقعی این پروژه. برخلاف Entity های دیگر (JSON خام در یک ستون Blob،
// افزودن فیلد بدون Migration رسمی مشکلی ایجاد نمی‌کند)، StoryBreakdownSessionEntity
// ستون‌های تفکیک‌شده‌ی واقعی دارد — افزودن previewLanguageEnabled بدون افزایش
// version و بدون Migration صریح، اپ را برای هر کاربری که از قبل پروژه‌ی
// ذخیره‌شده دارد Crash می‌کند (پیش‌فرض سخت‌گیرانه‌ی Room). MIGRATION_1_2 داده‌ی
// موجود کاربر را حفظ می‌کند (ALTER TABLE ADD COLUMN با مقدار پیش‌فرض 0/false —
// یعنی رفتار فعلی/فقط‌انگلیسی برای کاربران موجود، بدون تغییر ناگهانی رفتار)؛
// fallbackToDestructiveMigration عمداً استفاده نشد چون داده‌ی کاربر را پاک
// می‌کند — غیرقابل‌قبول برای اپ تولید محتوای واقعی.
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE story_breakdown_session ADD COLUMN previewLanguageEnabled INTEGER NOT NULL DEFAULT 0")
    }
}

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
        DependencyEdgeEntity::class,
        EventLogEntity::class,
        ProjectDnaEntity::class,
        AudioContextEntity::class,
        StoryContextEntity::class,
        StoryBreakdownSessionEntity::class
    ],
    version = 2,
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
    abstract fun eventLogDao(): EventLogDao
    abstract fun projectDnaDao(): ProjectDnaDao
    abstract fun audioContextDao(): AudioContextDao
    abstract fun storyDao(): StoryDao
    abstract fun storyBreakdownSessionDao(): StoryBreakdownSessionDao
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
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }
    }
}
