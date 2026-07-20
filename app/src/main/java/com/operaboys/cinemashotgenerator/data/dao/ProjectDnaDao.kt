package com.operaboys.cinemashotgenerator.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.operaboys.cinemashotgenerator.data.entity.ProjectDnaEntity

// واحد ۱۵ — قدم ۳ (زیرقدم ۱): DAO پایه برای ProjectDnaEntity.

@Dao
interface ProjectDnaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProjectDna(dna: ProjectDnaEntity)

    @Query("SELECT * FROM project_dna WHERE projectId = :projectId")
    suspend fun loadProjectDnaForProject(projectId: String): ProjectDnaEntity?

    @Delete
    suspend fun deleteProjectDna(dna: ProjectDnaEntity)
}
