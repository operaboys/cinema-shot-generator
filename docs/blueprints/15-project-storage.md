# واحد ۱۵: ذخیره‌سازی پروژه (Project Storage System)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**ادغام از:** Persistence Layer + Project Data Schema
**وضعیت:** فعال
**وابستگی:** State & Versioning (واحد ۱۲)

---

## تعریف

ذخیره‌سازی، بازیابی، و مدیریت کامل داده‌های چند پروژه به‌صورت هم‌زمان — کاملاً On-Device، بدون سرور، بدون Cloud.

## Stack فنی (بررسی‌شده با منابع رسمی، جولای ۲۰۲۶)

- **Room 2.8.4** (آخرین نسخه‌ی پایدار) روی SQLite
- **KSP** برای Annotation Processing (نه KAPT — رسماً توصیه‌ی گوگل و سریع‌تر)
- طبق Room نسخه‌های جدید (۳.۰ به بعد)، تمام DAO functions باید **suspend** باشند یا **Flow** برگردانند — توابع Blocking دیگر مجاز نیستند
- Room رسماً از **چند دیتابیس مجزا یا چند Entity گروه‌بندی‌شده در یک دیتابیس** پشتیبانی می‌کند؛ برای این پروژه یک دیتابیس واحد با جداول مرتبط (نه فایل جدا per پروژه) انتخاب شد

---

## معماری: یک دیتابیس، چند پروژه

```
User Actions (Create/Update/Delete)
        ↓
Project Storage System
├─ Room Database (یک دیتابیس واحد)
│   ├─ ProjectEntity (چند پروژه هم‌زمان)
│   ├─ SceneEntity, ShotEntity (کلید خارجی به Project)
│   ├─ AssetEntity (Character/Location/Object)
│   ├─ PromptBlueprintEntity, RenderedOutputEntity
│   ├─ OverrideEntity, VersionEntity
│   └─ DependencyEdgeEntity
│
├─ Auto-Save Manager (هر ۳۰ ثانیه یا بعد از هر تغییر مهم)
│
├─ Backup Manager (محلی — Snapshot دوره‌ای)
│
└─ Export/Import (انتقال دستی پروژه بین دستگاه‌ها توسط کاربر)
```

---

## Entity ها (Kotlin/Room، نمونه‌ی الگو)

```kotlin
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val projectId: String,
    val projectName: String,
    val createdAt: String,
    val lastModified: String,
    val uiLanguage: String = "fa"   // زبان UI، مستقل از prompt_language
)

@Entity(
    tableName = "scenes",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["projectId"], childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("projectId")]
)
data class SceneEntity(
    @PrimaryKey val sceneId: String,
    val projectId: String,
    val sceneDataJson: String  // structured_parts سریالایز‌شده
)

@Entity(
    tableName = "shots",
    foreignKeys = [ForeignKey(
        entity = SceneEntity::class,
        parentColumns = ["sceneId"], childColumns = ["sceneId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sceneId")]
)
data class ShotEntity(
    @PrimaryKey val shotId: String,
    val sceneId: String,
    val shotDataJson: String
)

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val assetId: String,
    val projectId: String,
    val assetType: String,   // "character" | "location" | "object"
    val assetDataJson: String
)

@Entity(tableName = "prompt_blueprints")
data class PromptBlueprintEntity(
    @PrimaryKey val promptBlueprintId: String,
    val shotId: String,
    val structuredPartsJson: String,
    val seed: Int?
)

@Entity(
    tableName = "rendered_outputs",
    foreignKeys = [ForeignKey(
        entity = PromptBlueprintEntity::class,
        parentColumns = ["promptBlueprintId"], childColumns = ["promptBlueprintId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class RenderedOutputEntity(
    @PrimaryKey val renderedOutputId: String,
    val promptBlueprintId: String,
    val modelProfileId: String,
    val formattedPrompt: String,
    val language: String
)

@Entity(tableName = "overrides")
data class OverrideEntity(
    @PrimaryKey val overrideId: String,
    val entityType: String,
    val entityId: String,
    val overrideDataJson: String,  // scope + reason (طبق override-policy.md، بدون priority)
    val active: Boolean = true
)

@Entity(tableName = "versions")
data class VersionEntity(
    @PrimaryKey val versionId: String,
    val entityId: String,
    val versionType: String,  // "safe" | "risky"
    val snapshotJson: String,
    val createdAt: String
)
```

### DAO — همیشه suspend یا Flow (الزام Room جدید)

```kotlin
@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>   // Flow برای به‌روزرسانی خودکار UI

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProject(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE projectId = :id")
    suspend fun loadProject(id: String): ProjectEntity?

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}

@Dao
interface SceneDao {
    @Query("SELECT * FROM scenes WHERE projectId = :projectId")
    fun getScenesForProject(projectId: String): Flow<List<SceneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveScene(scene: SceneEntity)
}
```

---

## Auto-Save و Backup

```kotlin
/** ذخیره‌ی خودکار — هر ۳۰ ثانیه یا بلافاصله بعد از تغییرات ساختاری مهم. */
class AutoSaveManager(private val projectDao: ProjectDao, private val intervalSeconds: Long = 30) {
    suspend fun saveIfDirty(project: ProjectEntity, isDirty: Boolean) {
        if (isDirty) {
            projectDao.saveProject(project.copy(lastModified = nowIso8601()))
        }
    }
}

/**
 * Backup محلی دوره‌ای — هر ۵ دقیقه (جدا از Auto-Save هر ۳۰ ثانیه؛
 * Auto-Save وضعیت جاری را ذخیره می‌کند، Backup یک نسخه‌ی کامل و
 * قابل‌بازگشت می‌سازد). حداکثر تعداد Backup نگه‌داشته‌شده محدود است
 * تا فضای ذخیره‌سازی دستگاه پر نشود؛ Backup های قدیمی‌تر خودکار حذف می‌شوند.
 */
class BackupManager(
    private val projectId: String,
    private val backupIntervalMinutes: Long = 5,
    private val maxBackupsToKeep: Int = 10
) {
    suspend fun createBackup(): Result<String> {
        val projectData = serializeFullProject(projectId)
        val backupId = generateBackupId()
        val backupPath = writeFileToDevice(
            "backup_${projectId}_${backupId}.json", projectData, "application/json"
        )
        cleanOldBackups()
        return Result.success(backupPath)
    }

    /** فقط جدیدترین N بک‌آپ نگه داشته می‌شود؛ بقیه حذف می‌شوند. */
    private suspend fun cleanOldBackups() {
        val backups = listBackupsForProject(projectId).sortedByDescending { it.createdAt }
        backups.drop(maxBackupsToKeep).forEach { deleteBackupFile(it.path) }
    }

    suspend fun restoreFromBackup(backupId: String): Result<Unit> {
        val backup = loadBackup(backupId) ?: return Result.failure(IllegalArgumentException("Backup یافت نشد"))
        restoreProjectFromSnapshot(projectId, backup.data)
        return Result.success(Unit)
    }
}
```

---

## Export/Import (انتقال دستی، نه Cloud Sync)

```kotlin
/** خروجی کامل پروژه برای انتقال دستی بین دستگاه‌ها — از Storage Access Framework استفاده می‌کند. */
suspend fun exportProject(projectId: String): Result<String> {
    val fullData = serializeFullProject(projectId)  // شامل تمام Entity های مرتبط
    return try {
        val fileUri = writeFileToDevice("${projectId}_export.json", fullData, "application/json")
        Result.success(fileUri)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun importProject(fileUri: String): Result<ProjectEntity> {
    val data = readFileFromDevice(fileUri)
    val project = deserializeFullProject(data)
    validateReferentialIntegrity(project)  // بررسی Broken Reference قبل از ذخیره
    return Result.success(project)
}
```

---

## یکپارچگی ارجاعی (Referential Integrity)

```kotlin
data class IntegrityIssue(val source: String, val brokenReferenceTo: String, val message: String)

/**
 * بررسی کامل یکپارچگی ارجاعی پروژه قبل از ذخیره یا Import — شامل
 * سه نوع ارجاع رایج: Shot→Scene، Shot→Character، Shot→Object/Location.
 */
fun validateReferentialIntegrity(project: ProjectData): List<IntegrityIssue> {
    val issues = mutableListOf<IntegrityIssue>()
    val sceneIds = project.scenes.map { it.sceneId }.toSet()
    val assetIds = project.assets.map { it.assetId }.toSet()

    project.shots.forEach { shot ->
        // Shot → Scene
        if (shot.sceneId !in sceneIds) {
            issues += IntegrityIssue(shot.shotId, shot.sceneId, "این شات به صحنه‌ای ارجاع می‌دهد که وجود ندارد")
        }
        // Shot → Character / Object / Location
        (shot.characterIds + shot.objectIds + shot.locationIds)
            .filterNot { it in assetIds }
            .forEach { missingId ->
                issues += IntegrityIssue(shot.shotId, missingId, "ارجاع به Asset حذف‌شده یا ناموجود")
            }
    }
    return issues
}
```

**قانون:** این بررسی باید همیشه **قبل از** ذخیره‌ی نهایی یا Import اجرا شود — یک پروژه با ارجاع شکسته نباید در حالت «Final» یا حتی «قابل‌استفاده» قرار بگیرد (طبق `state-and-versioning.md`، واحد ۱۲).

---

## قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| `project_id` نامعتبر یا خالی | شناسه‌ی پروژه الزامی است | **Blocking** |
| ارجاع شکسته (Broken Reference) به Asset حذف‌شده | باید قبل از ذخیره تشخیص داده شود | **Blocking** |
| فضای ذخیره‌سازی دستگاه پر شده | پیام «فضای ذخیره‌سازی دستگاه تمام شده؛ پروژه‌های قدیمی را Export/حذف کنید» | **Blocking** |
| Import فایلی با نسخه‌ی Schema قدیمی‌تر | نیاز به Migration | **Warning** (با مسیر migration مشخص) |

**نکته‌ی مهم درباره‌ی حذف کامل:** «Server Storage»، «Cloud Storage»، خطاهای «حساب Premium»، و «Network Error/Offline Mode» به‌طور کامل از این معماری کنار گذاشته شده‌اند — ذخیره‌سازی همیشه محلی و همیشه در دسترس است، بدون مفهوم آنلاین/آفلاین برای این لایه.

---

## مقیاس‌پذیری (Scalability) — از سند اصلی، جا افتاده بود در بازنویسی قبلی

این معماری باید بدون افت محسوس کارایی، این حجم‌ها را پشتیبانی کند:

| مورد | حداقل ظرفیت مورد انتظار |
|---|---|
| تعداد Scene در یک پروژه | ۱۰۰+ |
| تعداد Shot در یک پروژه | ۱۰۰۰+ |
| تعداد Asset در Asset Library | ۱۰,۰۰۰+ |
| تعداد Reference Image در یک پروژه | ۱۰۰۰+ |
| تعداد پروژه‌ی هم‌زمان در دیتابیس | چند ده پروژه، بدون افت کارایی لیست/جستجو |

**پیامد فنی:** کوئری‌های `getScenesForProject`/`getShotsForScene` باید Index مناسب داشته باشند (طبق Entity ها در همین سند، `indices = [Index("projectId")]` و مشابه) و از Pagination برای لیست‌های بزرگ استفاده کنند — نه Load کامل تمام رکوردها در حافظه هم‌زمان.

```kotlin
@Dao
interface ShotDao {
    // برای پروژه‌های بزرگ (۱۰۰۰+ Shot)، Paging به‌جای بارگذاری کامل
    @Query("SELECT * FROM shots WHERE sceneId = :sceneId LIMIT :limit OFFSET :offset")
    suspend fun getShotsPaged(sceneId: String, limit: Int, offset: Int): List<ShotEntity>
}
```

---

## یکپارچگی داده (Data Integrity) — اصل Atomicity

هر عملیات ذخیره‌سازی که چند جدول را هم‌زمان تغییر می‌دهد (مثلاً ذخیره‌ی یک Shot جدید + به‌روزرسانی شمارنده‌ی Scene) باید **Atomic** باشد — یا کامل انجام شود، یا اصلاً انجام نشود؛ هرگز در حالت نیمه‌تمام رها نشود.

```kotlin
/** استفاده از Room Transaction برای تضمین Atomicity در عملیات چندجدولی. */
@Dao
abstract class ProjectTransactionDao {
    @Transaction
    open suspend fun saveShotWithSceneUpdate(shot: ShotEntity, updatedScene: SceneEntity) {
        saveShot(shot)
        saveScene(updatedScene)
        // اگر هرکدام شکست بخورد، Room کل تراکنش را Rollback می‌کند
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveShot(shot: ShotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveScene(scene: SceneEntity)
}
```

**قانون:** هر تغییری که به بیش از یک Entity مرتبط اثر می‌گذارد (بروزرسانی Dependency Graph، تغییر State یک Shot که باعث Invalidate چند Entity دیگر می‌شود) باید در یک `@Transaction` انجام شود، نه چند فراخوانی جدا که ممکن است بین‌شان خطا رخ دهد.

---

## معیارهای موفقیت

- چند پروژه هم‌زمان در یک دیتابیس Room مدیریت می‌شوند، بدون فایل جدا per پروژه.
- تمام DAO functions به شکل `suspend` یا `Flow` هستند (بدون بلاک کردن Thread اصلی).
- Auto-Save بدون از‌دست‌رفتن داده کار می‌کند.
- Export/Import برای انتقال دستی بین دستگاه‌ها، بدون هیچ وابستگی به سرویس خارجی.
- Broken Reference همیشه قبل از ذخیره یا Import تشخیص داده می‌شود.
- سیستم بدون افت محسوس کارایی، پروژه‌های بزرگ (۱۰۰+ Scene، ۱۰۰۰+ Shot، ۱۰,۰۰۰+ Asset) را پشتیبانی می‌کند.
- عملیات چندجدولی همیشه Atomic هستند (با `@Transaction`)؛ هیچ حالت نیمه‌ذخیره‌شده‌ای رخ نمی‌دهد.
