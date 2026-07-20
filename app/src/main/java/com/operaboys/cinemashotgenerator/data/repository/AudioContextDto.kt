package com.operaboys.cinemashotgenerator.data.repository

import kotlinx.serialization.Serializable

// واحد ۱۵ — قدم ۳ (زیرقدم ۲): DTO برای فیلد AudioContextEntity.audioContextDataJson.
// منبع حقیقت شکل داده: domain/audio/AudioModels.kt (خوانده‌شده با grep کامل).
//
// ActionSoundDto/CharacterSoundDto از ShotDto.kt بازاستفاده شدند (هم‌شکل دقیق با
// domain.shot.ActionSound/CharacterSound که خودِ AudioContext هم مستقیماً بازاستفاده
// می‌کند — بدون بازتعریف). AmbientSound اما اینجا نوع محلی جدید (AudioContextAmbientSoundDto)
// گرفت چون domain.audio.AmbientSound یک فیلد اضافه (source) نسبت به domain.shot.AmbientSound
// دارد (که AmbientSoundDto موجود در ShotDto.kt برایش نوشته شده) — دقیقاً همان تمایز
// واقعی که ADR-011 (واحد ۱۰) مستند کرده بود.

@Serializable
data class AudioContextAmbientSoundDto(
    val type: String,
    val intensity: String,
    val description: String,
    val source: String
)

@Serializable
data class AudioContextDto(
    val audioContextId: String,
    val shotId: String,
    val ambientSounds: List<AudioContextAmbientSoundDto> = emptyList(),
    val actionSounds: List<ActionSoundDto> = emptyList(),
    val characterSounds: List<CharacterSoundDto> = emptyList()
)
