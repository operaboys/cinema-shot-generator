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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// واحد ۱۵ — تست end-to-end واقعی برای BackupManager (تکمیل ADR-017/022/023).
// FakeBackupFileStorage جایگزین in-memory برای BackupFileStorage است — طبق تصمیم
// ADR-023، I/O فایل واقعی پشت این Interface انتزاع شد تا این تست بدون دستگاه واقعی
// اجرا شود.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupManagerTest {

    private lateinit var database: AppDatabase
    private lateinit var fakeStorage: FakeBackupFileStorage
    private lateinit var backupManager: BackupManager

    private val projectId = "proj_001"

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        fakeStorage = FakeBackupFileStorage()
        backupManager = buildManager(database, fakeStorage, maxBackupsToKeep = 10, backupId = "fixed_backup_01")
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun buildManager(
        db: AppDatabase,
        storage: FakeBackupFileStorage,
        maxBackupsToKeep: Int,
        backupId: String
    ) = BackupManager(
        projectId = projectId,
        projectDao = db.projectDao(),
        sceneDao = db.sceneDao(),
        shotDao = db.shotDao(),
        assetDao = db.assetDao(),
        projectDnaDao = db.projectDnaDao(),
        audioContextDao = db.audioContextDao(),
        backupFileStorage = storage,
        maxBackupsToKeep = maxBackupsToKeep,
        idProvider = { backupId }
    )

    private suspend fun seedFullProject(db: AppDatabase) {
        db.projectDao().saveProject(
            ProjectEntity(projectId, "My Movie", "2026-07-01T00:00:00Z", "2026-07-01T00:00:00Z", "fa")
        )
        db.sceneDao().saveScene(SceneEntity("scene_01", projectId, """{"a":1}"""))
        db.sceneDao().saveScene(SceneEntity("scene_02", projectId, """{"a":2}"""))
        db.shotDao().saveShot(ShotEntity("shot_01", "scene_01", """{"b":1}"""))
        db.shotDao().saveShot(ShotEntity("shot_02", "scene_02", """{"b":2}"""))
        db.assetDao().saveAsset(AssetEntity("asset_01", projectId, "character", """{"c":1}"""))
        db.projectDnaDao().saveProjectDna(ProjectDnaEntity("dna_01", projectId, """{"d":1}"""))
        db.audioContextDao().saveAudioContext(AudioContextEntity("audio_01", "shot_01", """{"e":1}"""))
    }

    @Test
    fun `createBackup writes a complete valid JSON snapshot of the full project`() = runBlocking {
        seedFullProject(database)

        val result = backupManager.createBackup()
        assertTrue(result.isSuccess)
        val path = result.getOrThrow()

        val content = fakeStorage.readFile(path)
        val snapshotResult = deserializeFullProject(content)
        assertTrue(snapshotResult.isSuccess)
        val snapshot = snapshotResult.getOrThrow()

        assertEquals("proj_001", snapshot.project.projectId)
        assertEquals(2, snapshot.scenes.size)
        assertEquals(2, snapshot.shots.size)
        assertEquals(1, snapshot.assets.size)
        assertEquals("dna_01", snapshot.projectDna?.dnaId)
        assertEquals(1, snapshot.audioContexts.size)
    }

    @Test
    fun `cleanOldBackups keeps only the newest maxBackupsToKeep backups`() = runBlocking {
        seedFullProject(database)
        var counter = 0
        val storage = FakeBackupFileStorage { (counter++).toString().padStart(6, '0') }
        var backupCounter = 0
        val manager = BackupManager(
            projectId = projectId,
            projectDao = database.projectDao(),
            sceneDao = database.sceneDao(),
            shotDao = database.shotDao(),
            assetDao = database.assetDao(),
            projectDnaDao = database.projectDnaDao(),
            audioContextDao = database.audioContextDao(),
            backupFileStorage = storage,
            maxBackupsToKeep = 3,
            idProvider = { "backup_${backupCounter++}" }
        )

        repeat(5) { manager.createBackup() }

        val prefix = "backup_${projectId.length}_${projectId}_"
        val remaining = storage.listFiles(prefix)
        assertEquals(3, remaining.size)
        // جدیدترین سه‌تا (backup_2، backup_3، backup_4) باید باقی مانده باشند
        val remainingIds = remaining.map { it.path.substringAfterLast('/') }.toSet()
        assertTrue(remainingIds.contains("${prefix}backup_4.json"))
        assertTrue(remainingIds.contains("${prefix}backup_3.json"))
        assertTrue(remainingIds.contains("${prefix}backup_2.json"))
    }

    @Test
    fun `cleanOldBackups does not cross-contaminate a different project whose id is a string prefix of this one`() = runBlocking {
        // رگرسیون برای باگ واقعی کشف‌شده در بازبینی: قبل از رمزگذاری طول projectId در
        // backupPrefix، پروژه‌ی "p1" پیشوند "backup_p1_" داشت که خودش پیشوند رشته‌ای
        // فایل‌های پروژه‌ی "p1_v2" هم بود ("backup_p1_v2_....json" با "backup_p1_"
        // شروع می‌شود) — یعنی cleanOldBackups پروژه‌ی "p1" می‌توانست بک‌آپ پروژه‌ی دیگر
        // را ببیند و حذف کند.
        database.projectDao().saveProject(
            ProjectEntity("p1", "Project One", "2026-07-01T00:00:00Z", "2026-07-01T00:00:00Z", "fa")
        )
        database.projectDao().saveProject(
            ProjectEntity("p1_v2", "Project One V2", "2026-07-01T00:00:00Z", "2026-07-01T00:00:00Z", "fa")
        )
        val sharedStorage = FakeBackupFileStorage()

        val managerP1 = BackupManager(
            projectId = "p1",
            projectDao = database.projectDao(),
            sceneDao = database.sceneDao(),
            shotDao = database.shotDao(),
            assetDao = database.assetDao(),
            projectDnaDao = database.projectDnaDao(),
            audioContextDao = database.audioContextDao(),
            backupFileStorage = sharedStorage,
            maxBackupsToKeep = 1,
            idProvider = { "only" }
        )
        val managerP1V2 = BackupManager(
            projectId = "p1_v2",
            projectDao = database.projectDao(),
            sceneDao = database.sceneDao(),
            shotDao = database.shotDao(),
            assetDao = database.assetDao(),
            projectDnaDao = database.projectDnaDao(),
            audioContextDao = database.audioContextDao(),
            backupFileStorage = sharedStorage,
            maxBackupsToKeep = 1,
            idProvider = { "only" }
        )

        val resultP1V2 = managerP1V2.createBackup()
        assertTrue(resultP1V2.isSuccess)

        // cleanOldBackups داخل createBackup پروژه‌ی "p1" فراخوانی می‌شود — نباید بک‌آپ
        // "p1_v2" را ببیند یا پاک کند.
        val resultP1 = managerP1.createBackup()
        assertTrue(resultP1.isSuccess)

        val p1V2Backups = sharedStorage.listFiles("backup_${"p1_v2".length}_p1_v2_")
        assertEquals(1, p1V2Backups.size)

        val restoreP1V2 = managerP1V2.restoreFromBackup("only")
        assertTrue(restoreP1V2.isSuccess)
    }

    @Test
    fun `restoreFromBackup restores a full round trip into a different database`() = runBlocking {
        seedFullProject(database)
        val createResult = backupManager.createBackup()
        assertTrue(createResult.isSuccess)

        val freshDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        try {
            val restoreManager = buildManager(freshDatabase, fakeStorage, maxBackupsToKeep = 10, backupId = "fixed_backup_01")
            val restoreResult = restoreManager.restoreFromBackup("fixed_backup_01")
            assertTrue(restoreResult.isSuccess)

            val restoredProject = freshDatabase.projectDao().loadProject(projectId)
            assertEquals("My Movie", restoredProject?.projectName)

            val restoredScene1 = freshDatabase.sceneDao().loadScene("scene_01")
            assertEquals("""{"a":1}""", restoredScene1?.sceneDataJson)

            val restoredShot1 = freshDatabase.shotDao().loadShot("shot_01")
            assertEquals("scene_01", restoredShot1?.sceneId)

            val restoredAsset = freshDatabase.assetDao().loadAsset("asset_01")
            assertEquals("""{"c":1}""", restoredAsset?.assetDataJson)

            val restoredDna = freshDatabase.projectDnaDao().loadProjectDnaForProject(projectId)
            assertEquals("dna_01", restoredDna?.dnaId)

            val restoredAudio = freshDatabase.audioContextDao().loadAudioContextForShot("shot_01")
            assertEquals("audio_01", restoredAudio?.audioContextId)
        } finally {
            freshDatabase.close()
        }
    }

    @Test
    fun `restoreFromBackup preserves a non-default project state (regression for ADR-044's state field)`() = runBlocking {
        database.projectDao().saveProject(
            ProjectEntity(projectId, "My Movie", "2026-07-01T00:00:00Z", "2026-07-01T00:00:00Z", "fa", "FINAL")
        )
        val createResult = backupManager.createBackup()
        assertTrue(createResult.isSuccess)

        val freshDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        try {
            val restoreManager = buildManager(freshDatabase, fakeStorage, maxBackupsToKeep = 10, backupId = "fixed_backup_01")
            assertTrue(restoreManager.restoreFromBackup("fixed_backup_01").isSuccess)

            val restoredProject = freshDatabase.projectDao().loadProject(projectId)
            assertEquals("FINAL", restoredProject?.state)
        } finally {
            freshDatabase.close()
        }
    }

    @Test
    fun `restoreFromBackup with an unknown backupId fails`() = runBlocking {
        seedFullProject(database)
        backupManager.createBackup()

        val result = backupManager.restoreFromBackup("does_not_exist")
        assertTrue(result.isFailure)
    }
}

/** Fake in-memory برای BackupFileStorage — تست‌پذیر بدون I/O واقعی دستگاه. */
class FakeBackupFileStorage(private val clock: () -> String = { java.time.Instant.now().toString() }) : BackupFileStorage {
    private val files = mutableMapOf<String, String>()
    private val createdAtByPath = mutableMapOf<String, String>()

    override suspend fun writeFile(fileName: String, content: String): String {
        val path = "/fake/$fileName"
        files[path] = content
        createdAtByPath[path] = clock()
        return path
    }

    override suspend fun readFile(path: String): String =
        files[path] ?: throw java.io.FileNotFoundException(path)

    override suspend fun listFiles(prefix: String): List<BackupFileInfo> =
        files.keys
            .filter { it.substringAfterLast('/').startsWith(prefix) }
            .map { path -> BackupFileInfo(path = path, createdAt = createdAtByPath.getValue(path)) }

    override suspend fun deleteFile(path: String) {
        files.remove(path)
        createdAtByPath.remove(path)
    }
}
