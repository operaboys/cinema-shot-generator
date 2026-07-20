package com.operaboys.cinemashotgenerator.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.operaboys.cinemashotgenerator.data.entity.AudioContextEntity

// واحد ۱۵ — قدم ۳ (زیرقدم ۲): DAO پایه برای AudioContextEntity.

@Dao
interface AudioContextDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAudioContext(audioContext: AudioContextEntity)

    @Query("SELECT * FROM audio_contexts WHERE shotId = :shotId")
    suspend fun loadAudioContextForShot(shotId: String): AudioContextEntity?

    @Delete
    suspend fun deleteAudioContext(audioContext: AudioContextEntity)
}
