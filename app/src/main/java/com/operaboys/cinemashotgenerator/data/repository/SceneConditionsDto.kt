package com.operaboys.cinemashotgenerator.data.repository

import kotlinx.serialization.Serializable

// واحد ۱۵ — قدم ۲: DTO برای domain/sceneconditions (LightingSettings/EnvironmentSettings)
// دلیل DTO محلی (نه @Serializable مستقیم روی domain/): همان دلیل ShotDto.kt.

@Serializable
data class LightingSettingsDto(
    val style: String,
    val keyLightPosition: String,
    val contrastRatio: String
)

@Serializable
data class EnvironmentSettingsDto(val weatherType: String)
