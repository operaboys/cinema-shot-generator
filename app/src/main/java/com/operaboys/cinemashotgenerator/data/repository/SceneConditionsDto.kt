package com.operaboys.cinemashotgenerator.data.repository

import kotlinx.serialization.Serializable

// واحد ۱۵ — قدم ۲: DTO برای domain/sceneconditions (LightingSettings/EnvironmentSettings)
// دلیل DTO محلی (نه @Serializable مستقیم روی domain/): همان دلیل ShotDto.kt.
//
// MIGRATED (docs/adr/036-unit08-lighting-environment-ui-fields-migration.md): پنج/شش
// فیلد جدید nullable اضافه شدند تا مقادیر ویرایش‌شده در Tab «نور و محیط» واحد ۱۶
// واقعاً در Room ذخیره/بازیابی شوند — این یک یافته‌ی خارج از Blast Radius اعلام‌شده
// در دستور کار بود (که فقط ۳ فایل را فهرست کرده بود، نه این DTO)؛ بدون این تغییر،
// هر مقدار ویرایش‌شده‌ی کاربر برای این فیلدها در ذخیره/بارگذاری مجدد شات گم می‌شد.

@Serializable
data class LightingSettingsDto(
    val style: String,
    val keyLightPosition: String,
    val contrastRatio: String,
    val fillLight: String? = null,
    val colorTemperature: String? = null,
    val shadowQuality: String? = null,
    val lightSourceCount: String? = null,
    val lightingMotivation: String? = null
)

@Serializable
data class EnvironmentSettingsDto(
    val weatherType: String,
    val weatherIntensity: String? = null,
    val windStrength: String? = null,
    val groundState: String? = null,
    val visibility: String? = null,
    val temperatureFeel: String? = null,
    val environmentalMotion: List<String> = emptyList()
)
