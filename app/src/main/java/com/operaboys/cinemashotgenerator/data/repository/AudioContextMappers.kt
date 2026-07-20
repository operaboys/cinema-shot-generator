package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.domain.audio.AmbientSound
import com.operaboys.cinemashotgenerator.domain.audio.AudioContext
import com.operaboys.cinemashotgenerator.domain.shot.ActionSound
import com.operaboys.cinemashotgenerator.domain.shot.CharacterSound

// واحد ۱۵ — قدم ۳ (زیرقدم ۲): نگاشت دوطرفه‌ی AudioContextDto ↔ AudioContext واقعی دامنه.
// در یک فایل جدا از DtoMappers.kt/DnaAssetMappers.kt نگه داشته شد تا با
// domain.shot.AmbientSound (که در DtoMappers.kt برای Shot.soundProfile import شده)
// تداخل نام import پیش نیاید — اینجا فقط domain.audio.AmbientSound لازم است.

fun AudioContextDto.toDomain(): AudioContext = AudioContext(
    audioContextId = audioContextId,
    shotId = shotId,
    ambientSounds = ambientSounds.map { AmbientSound(it.type, it.intensity, it.description, it.source) },
    actionSounds = actionSounds.map { ActionSound(it.timestampSeconds, it.type, it.description) },
    characterSounds = characterSounds.map { CharacterSound(it.characterId, it.type, it.description) }
)

fun AudioContext.toDto(): AudioContextDto = AudioContextDto(
    audioContextId = audioContextId,
    shotId = shotId,
    ambientSounds = ambientSounds.map { AudioContextAmbientSoundDto(it.type, it.intensity, it.description, it.source) },
    actionSounds = actionSounds.map { ActionSoundDto(it.timestampSeconds, it.type, it.description) },
    characterSounds = characterSounds.map { CharacterSoundDto(it.characterId, it.type, it.description) }
)
