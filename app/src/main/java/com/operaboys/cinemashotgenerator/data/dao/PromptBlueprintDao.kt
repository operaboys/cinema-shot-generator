package com.operaboys.cinemashotgenerator.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.operaboys.cinemashotgenerator.data.entity.PromptBlueprintEntity
import kotlinx.coroutines.flow.Flow

// واحد ۱۵ — Project Storage System (قدم ۱: DAO پایه — بدون منطق دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md

@Dao
interface PromptBlueprintDao {
    @Query("SELECT * FROM prompt_blueprints WHERE shotId = :shotId")
    fun getPromptBlueprintsForShot(shotId: String): Flow<List<PromptBlueprintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePromptBlueprint(promptBlueprint: PromptBlueprintEntity)

    @Query("SELECT * FROM prompt_blueprints WHERE promptBlueprintId = :id")
    suspend fun loadPromptBlueprint(id: String): PromptBlueprintEntity?

    @Delete
    suspend fun deletePromptBlueprint(promptBlueprint: PromptBlueprintEntity)
}
