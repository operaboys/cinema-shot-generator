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

/** Backup محلی دوره‌ای — snapshot کامل، نه Cloud Sync. */
suspend fun createLocalBackup(projectId: String): Result<String> {
    val snapshot = serializeFullProject(projectId)
    val backupPath = writeFileToDevice("backup_${projectId}_${nowIso8601()}.json", snapshot, "application/json")
    return Result.success(backupPath)
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

/** بررسی این‌که همه‌ی ارجاعات (مثلاً character_id در یک Shot) واقعاً در Asset Library وجود دارند. */
fun validateReferentialIntegrity(project: ProjectData): List<IntegrityIssue> {
    val issues = mutableListOf<IntegrityIssue>()
    val assetIds = project.assets.map { it.assetId }.toSet()
    project.shots.forEach { shot ->
        shot.characterIds.filterNot { it in assetIds }.forEach { missingId ->
            issues += IntegrityIssue(shot.shotId, missingId, "ارجاع به Asset حذف‌شده")
        }
    }
    return issues
}
```

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

## معیارهای موفقیت

- چند پروژه هم‌زمان در یک دیتابیس Room مدیریت می‌شوند، بدون فایل جدا per پروژه.
- تمام DAO functions به شکل `suspend` یا `Flow` هستند (بدون بلاک کردن Thread اصلی).
- Auto-Save بدون از‌دست‌رفتن داده کار می‌کند.
- Export/Import برای انتقال دستی بین دستگاه‌ها، بدون هیچ وابستگی به سرویس خارجی.
- Broken Reference همیشه قبل از ذخیره یا Import تشخیص داده می‌شود.
