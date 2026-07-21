package com.operaboys.cinemashotgenerator.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.entity.AssetEntity
import com.operaboys.cinemashotgenerator.data.entity.AudioContextEntity
import com.operaboys.cinemashotgenerator.data.entity.ProjectDnaEntity
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.data.entity.SceneEntity
import com.operaboys.cinemashotgenerator.data.entity.ShotEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// واحد ۱۵ — تست end-to-end واقعی برای exportProject/importProject (تکمیل ADR-017،
// آخرین مورد فهرست). از FakeBackupFileStorage همان کلاس تست BackupManagerTest.kt
// بازاستفاده شد (هم‌پکیج، بدون نیاز به تعریف دوباره).

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExportImportRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var fakeStorage: FakeBackupFileStorage

    private val projectId = "proj_001"

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        fakeStorage = FakeBackupFileStorage()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val validShotDataJson = """
        {"shotId":"shot_01","sceneId":"scene_01","shotNumber":1,"shotDescription":"d",
        "shotGoal":"ESTABLISH","shotType":"WIDE","durationSeconds":3.0,"motionLevel":"STATIC",
        "soundProfile":{"enabled":false},"characterIds":["asset_01"],"objectIds":[],"locationIds":[]}
    """.trimIndent()

    private suspend fun seedFullProject(db: AppDatabase) {
        db.projectDao().saveProject(
            ProjectEntity(projectId, "My Movie", "2026-07-01T00:00:00Z", "2026-07-01T00:00:00Z", "fa")
        )
        db.sceneDao().saveScene(SceneEntity("scene_01", projectId, """{"a":1}"""))
        db.shotDao().saveShot(ShotEntity("shot_01", "scene_01", validShotDataJson))
        db.assetDao().saveAsset(AssetEntity("asset_01", projectId, "character", """{"c":1}"""))
        db.projectDnaDao().saveProjectDna(ProjectDnaEntity("dna_01", projectId, """{"d":1}"""))
        db.audioContextDao().saveAudioContext(AudioContextEntity("audio_01", "shot_01", """{"e":1}"""))
    }

    @Test
    fun `exportProject writes a complete valid JSON snapshot with an export-named file`() = runBlocking {
        seedFullProject(database)

        val result = exportProject(
            projectId, database.projectDao(), database.sceneDao(), database.shotDao(), database.assetDao(),
            database.projectDnaDao(), database.audioContextDao(), fakeStorage, clock = { "12345" }
        )
        assertTrue(result.isSuccess)
        val path = result.getOrThrow()
        assertTrue(path.endsWith("${projectId}_export_12345.json"))

        val content = fakeStorage.readFile(path)
        val snapshot = deserializeFullProject(content).getOrThrow()
        assertEquals(projectId, snapshot.project.projectId)
        assertEquals(1, snapshot.scenes.size)
        assertEquals(1, snapshot.shots.size)
        assertEquals(1, snapshot.assets.size)
        assertEquals("dna_01", snapshot.projectDna?.dnaId)
        assertEquals(1, snapshot.audioContexts.size)
    }

    @Test
    fun `importProject with a healthy export file restores a full round trip into a different database`() = runBlocking {
        seedFullProject(database)
        val exportResult = exportProject(
            projectId, database.projectDao(), database.sceneDao(), database.shotDao(), database.assetDao(),
            database.projectDnaDao(), database.audioContextDao(), fakeStorage
        )
        assertTrue(exportResult.isSuccess)
        val fileUri = exportResult.getOrThrow()

        val freshDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        try {
            val importResult = importProject(
                fileUri, fakeStorage, freshDatabase.projectDao(), freshDatabase.sceneDao(),
                freshDatabase.shotDao(), freshDatabase.assetDao(), freshDatabase.projectDnaDao(),
                freshDatabase.audioContextDao()
            )
            assertTrue(importResult.isSuccess)
            assertEquals("My Movie", importResult.getOrThrow().projectName)

            assertEquals("scene_01", freshDatabase.sceneDao().loadScene("scene_01")?.sceneId)
            assertEquals(validShotDataJson, freshDatabase.shotDao().loadShot("shot_01")?.shotDataJson)
            assertEquals("asset_01", freshDatabase.assetDao().loadAsset("asset_01")?.assetId)
            assertEquals("dna_01", freshDatabase.projectDnaDao().loadProjectDnaForProject(projectId)?.dnaId)
            assertEquals("audio_01", freshDatabase.audioContextDao().loadAudioContextForShot("shot_01")?.audioContextId)
        } finally {
            freshDatabase.close()
        }
    }

    @Test
    fun `importProject with a broken scene reference fails and writes nothing to the target database`() = runBlocking {
        // یک Snapshot دستکاری‌شده که در آن shot_01 به یک sceneId جعلی (که در scenes
        // فهرست Snapshot اصلاً وجود ندارد) ارجاع می‌دهد.
        val brokenShotDataJson = """
            {"shotId":"shot_01","sceneId":"scene_ghost","shotNumber":1,"shotDescription":"d",
            "shotGoal":"ESTABLISH","shotType":"WIDE","durationSeconds":3.0,"motionLevel":"STATIC",
            "soundProfile":{"enabled":false},"characterIds":[],"objectIds":[],"locationIds":[]}
        """.trimIndent()
        val tamperedSnapshot = FullProjectSnapshot(
            project = ProjectEntityDto(projectId, "Broken Movie", "2026-07-01T00:00:00Z", "2026-07-01T00:00:00Z", "fa"),
            scenes = listOf(SceneEntityDto("scene_01", projectId, """{"a":1}""")),
            shots = listOf(ShotEntityDto("shot_01", "scene_ghost", brokenShotDataJson)),
            assets = emptyList(),
            projectDna = null,
            audioContexts = emptyList()
        )
        val tamperedJson = kotlinx.serialization.json.Json.encodeToString(FullProjectSnapshot.serializer(), tamperedSnapshot)
        val fileUri = fakeStorage.writeFile("tampered_export.json", tamperedJson)

        val importResult = importProject(
            fileUri, fakeStorage, database.projectDao(), database.sceneDao(), database.shotDao(),
            database.assetDao(), database.projectDnaDao(), database.audioContextDao()
        )

        assertTrue(importResult.isFailure)
        assertTrue(importResult.exceptionOrNull()?.message?.contains("scene_ghost") == true)

        // Atomicity: هیچ داده‌ای واقعاً در دیتابیس نوشته نشده باشد
        assertNull(database.projectDao().loadProject(projectId))
        assertNull(database.sceneDao().loadScene("scene_01"))
        assertNull(database.shotDao().loadShot("shot_01"))
    }

    @Test
    fun `importProject with a broken asset reference fails and writes nothing to the target database`() = runBlocking {
        val brokenShotDataJson = """
            {"shotId":"shot_01","sceneId":"scene_01","shotNumber":1,"shotDescription":"d",
            "shotGoal":"ESTABLISH","shotType":"WIDE","durationSeconds":3.0,"motionLevel":"STATIC",
            "soundProfile":{"enabled":false},"characterIds":["asset_ghost"],"objectIds":[],"locationIds":[]}
        """.trimIndent()
        val tamperedSnapshot = FullProjectSnapshot(
            project = ProjectEntityDto(projectId, "Broken Movie 2", "2026-07-01T00:00:00Z", "2026-07-01T00:00:00Z", "fa"),
            scenes = listOf(SceneEntityDto("scene_01", projectId, """{"a":1}""")),
            shots = listOf(ShotEntityDto("shot_01", "scene_01", brokenShotDataJson)),
            assets = emptyList(),
            projectDna = null,
            audioContexts = emptyList()
        )
        val tamperedJson = kotlinx.serialization.json.Json.encodeToString(FullProjectSnapshot.serializer(), tamperedSnapshot)
        val fileUri = fakeStorage.writeFile("tampered_export_2.json", tamperedJson)

        val importResult = importProject(
            fileUri, fakeStorage, database.projectDao(), database.sceneDao(), database.shotDao(),
            database.assetDao(), database.projectDnaDao(), database.audioContextDao()
        )

        assertTrue(importResult.isFailure)
        assertTrue(importResult.exceptionOrNull()?.message?.contains("asset_ghost") == true)
        assertNull(database.projectDao().loadProject(projectId))
    }
}
