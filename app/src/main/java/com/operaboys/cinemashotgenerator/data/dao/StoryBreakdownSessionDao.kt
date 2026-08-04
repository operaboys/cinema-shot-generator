package com.operaboys.cinemashotgenerator.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.operaboys.cinemashotgenerator.data.entity.StoryBreakdownSessionEntity

// واحد ۱۶ فاز ۲ — قدم ۲: DAO پایه برای StoryBreakdownSessionEntity، هم‌الگو با StoryDao.

@Dao
interface StoryBreakdownSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: StoryBreakdownSessionEntity)

    @Query("SELECT * FROM story_breakdown_session WHERE projectId = :projectId")
    suspend fun loadSessionForProject(projectId: String): StoryBreakdownSessionEntity?

    @Delete
    suspend fun deleteSession(session: StoryBreakdownSessionEntity)
}
