package com.operaboys.cinemashotgenerator.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// قدم ۱ از ۳ زیرقدم «سیستم Preview دوزبانه‌ی پرامپت» (ADR-121) — اولین Migration
// واقعی این پروژه (نسخه ۱ به ۲، افزودن previewLanguageEnabled به
// story_breakdown_session). این تست اثبات می‌کند MIGRATION_1_2 داده‌ی از‌قبل‌موجود
// کاربر را حفظ می‌کند و ستون تازه با مقدار پیش‌فرض معقول (false/0) پر می‌شود — نه
// fallbackToDestructiveMigration که داده را پاک می‌کرد.
//
// MigrationTestHelper به Schema های JSON صادرشده‌ی هر دو نسخه نیاز دارد
// (app/schemas/.../1.json و 2.json، تولیدشده توسط room.schemaLocation در
// app/build.gradle.kts) که با sourceSets.test.assets.srcDirs همان build.gradle.kts
// در دسترس Robolectric قرار گرفته‌اند.

private const val TEST_DB_NAME = "migration-test"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun `migrate1To2 preserves an existing row and defaults previewLanguageEnabled to false`() {
        helper.createDatabase(TEST_DB_NAME, 1).apply {
            execSQL(
                "INSERT INTO projects (projectId, projectName, createdAt, lastModified, uiLanguage, state) " +
                    "VALUES ('proj_migration_test', 'Migration Test Project', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z', 'fa', 'DRAFT')"
            )
            execSQL(
                "INSERT INTO story_breakdown_session (projectId, freeformStory, targetShotCount, defaultShotDurationSeconds) " +
                    "VALUES ('proj_migration_test', 'A pre-existing draft story.', 12, 4.5)"
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB_NAME, 2, true, MIGRATION_1_2)

        val cursor = migratedDb.query(
            "SELECT freeformStory, targetShotCount, defaultShotDurationSeconds, previewLanguageEnabled " +
                "FROM story_breakdown_session WHERE projectId = 'proj_migration_test'"
        )
        cursor.use {
            assertEquals("cursor باید دقیقاً همان یک ردیف موجود پیش از Migration را داشته باشد", 1, it.count)
            assertEquals(true, it.moveToFirst())
            assertEquals("A pre-existing draft story.", it.getString(it.getColumnIndexOrThrow("freeformStory")))
            assertEquals(12, it.getInt(it.getColumnIndexOrThrow("targetShotCount")))
            assertEquals(4.5f, it.getFloat(it.getColumnIndexOrThrow("defaultShotDurationSeconds")))
            assertEquals(
                "کاربران موجود (پیش از Migration) باید همان رفتار فعلی/فقط‌انگلیسی را بدون تغییر ناگهانی نگه دارند",
                0,
                it.getInt(it.getColumnIndexOrThrow("previewLanguageEnabled"))
            )
        }
    }
}
