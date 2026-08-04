package com.operaboys.cinemashotgenerator.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.operaboys.cinemashotgenerator.data.entity.StoryContextEntity

// واحد ۱۶ فاز ۲ — قدم ۱: DAO پایه برای StoryContextEntity، هم‌الگو با ProjectDnaDao.

@Dao
interface StoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStoryContext(story: StoryContextEntity)

    @Query("SELECT * FROM story_context WHERE projectId = :projectId")
    suspend fun loadStoryContextForProject(projectId: String): StoryContextEntity?

    @Delete
    suspend fun deleteStoryContext(story: StoryContextEntity)
}
