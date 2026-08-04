package com.operaboys.cinemashotgenerator.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

// واحد ۱۵ — Project Storage System (قدم ۱: DAO پایه — بدون منطق دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md
//
// MIGRATED (واحد ۱۶ فاز ۱، docs/adr/044-unit16-phase1-app-shell.md): ProjectWithCounts
// + getAllProjectsWithCounts/getProjectWithCounts اضافه شدند — کارت‌های پروژه‌ی
// Home/Projects به شمارش صحنه/شات نیاز دارند. تصمیم: یک Query واحد با Subquery
// همبسته (نه Flow.combine دستی در Repository) — چون Room به‌طور خودکار جدول‌های
// ارجاع‌شده در متن SQL (projects/scenes/shots) را برای Invalidation ردیابی می‌کند،
// این روش از هر Flow ای که با تغییر تعداد پروژه‌ها resubscribe دستی نیاز داشته باشد
// (مثل flatMapLatest، که Experimental هم هست) ساده‌تر و ایمن‌تر است.

/** شات‌ها فیلد projectId مستقیم ندارند (فقط sceneId) — شمارش از طریق Scene Join است. */
private const val SHOT_COUNT_SUBQUERY =
    "(SELECT COUNT(*) FROM shots sh WHERE sh.sceneId IN (SELECT sceneId FROM scenes WHERE projectId = p.projectId))"

data class ProjectWithCounts(
    @Embedded val project: ProjectEntity,
    val sceneCount: Int,
    val shotCount: Int
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>> // Flow برای به‌روزرسانی خودکار UI

    @Query(
        "SELECT p.*, " +
            "(SELECT COUNT(*) FROM scenes s WHERE s.projectId = p.projectId) AS sceneCount, " +
            "$SHOT_COUNT_SUBQUERY AS shotCount " +
            "FROM projects p ORDER BY p.lastModified DESC"
    )
    fun getAllProjectsWithCounts(): Flow<List<ProjectWithCounts>>

    @Query(
        "SELECT p.*, " +
            "(SELECT COUNT(*) FROM scenes s WHERE s.projectId = p.projectId) AS sceneCount, " +
            "$SHOT_COUNT_SUBQUERY AS shotCount " +
            "FROM projects p WHERE p.projectId = :projectId"
    )
    fun getProjectWithCounts(projectId: String): Flow<ProjectWithCounts?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProject(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE projectId = :id")
    suspend fun loadProject(id: String): ProjectEntity?

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}
