package com.operaboys.cinemashotgenerator.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.operaboys.cinemashotgenerator.data.entity.EventLogEntity
import kotlinx.coroutines.flow.Flow

// واحد ۱۵ — Project Storage System (قدم ۲: اتصال واقعی StateVersioningEventLogger واحد ۱۲)
// منبع حقیقت: docs/blueprints/15-project-storage.md

@Dao
interface EventLogDao {
    @Insert
    suspend fun logEvent(event: EventLogEntity)

    @Query("SELECT * FROM event_log WHERE entityId = :entityId ORDER BY timestamp DESC")
    fun getEventsForEntity(entityId: String): Flow<List<EventLogEntity>>
}
