package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.AudioContextDao
import com.operaboys.cinemashotgenerator.data.entity.AudioContextEntity
import com.operaboys.cinemashotgenerator.domain.audio.AudioContext
import kotlinx.serialization.json.Json

// واحد ۱۵ — قدم ۳ (زیرقدم ۲): اتصال واقعی AudioContext (واحد ۱۰) به AudioContextDao.
class AudioContextRepository(private val audioContextDao: AudioContextDao) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun saveAudioContext(audioContext: AudioContext): Result<Unit> = runCatching {
        audioContextDao.saveAudioContext(
            AudioContextEntity(
                audioContextId = audioContext.audioContextId,
                shotId = audioContext.shotId,
                audioContextDataJson = json.encodeToString(AudioContextDto.serializer(), audioContext.toDto())
            )
        )
    }

    /** nullable — طبق طراحی واقعی AudioContext (ممکن است هنوز برای این شات تولید نشده باشد). */
    suspend fun loadAudioContext(shotId: String): Result<AudioContext?> = runCatching {
        audioContextDao.loadAudioContextForShot(shotId)?.let {
            json.decodeFromString(AudioContextDto.serializer(), it.audioContextDataJson).toDomain()
        }
    }
}
