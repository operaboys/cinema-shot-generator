package com.operaboys.cinemashotgenerator.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.operaboys.cinemashotgenerator.data.entity.RenderedOutputEntity
import kotlinx.coroutines.flow.Flow

// واحد ۱۵ — Project Storage System (قدم ۱: DAO پایه — بدون منطق دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md

@Dao
interface RenderedOutputDao {
    @Query("SELECT * FROM rendered_outputs WHERE promptBlueprintId = :promptBlueprintId")
    fun getRenderedOutputsForPromptBlueprint(promptBlueprintId: String): Flow<List<RenderedOutputEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRenderedOutput(renderedOutput: RenderedOutputEntity)

    @Query("SELECT * FROM rendered_outputs WHERE renderedOutputId = :id")
    suspend fun loadRenderedOutput(id: String): RenderedOutputEntity?

    @Delete
    suspend fun deleteRenderedOutput(renderedOutput: RenderedOutputEntity)
}
