package com.operaboys.cinemashotgenerator.domain.audio

import com.operaboys.cinemashotgenerator.domain.shot.ActionSound
import java.util.UUID

// واحد ۱۰ — تولید خودکار Ambient/Action + پیشنهاد Breathing
// منبع حقیقت: docs/blueprints/10-audio-context.md
// دقیقاً طبق کد مفهومی بلوپرینت.

/**
 * تولید خودکار صدای محیطی از آب‌وهوا.
 *
 * NOTE: این تابع دقیقاً طبق کد مفهومی بلوپرینت ۱۰ است — که برخلاف mapEnvironmentToSound
 * واحد ۰۸ (که snow و wind را هم پوشش می‌دهد)، فقط rain/storm/fog را می‌شناسد و پارامتر
 * wind ندارد. این یک واگرایی جزئی بین دو پیاده‌سازی مشابه است که خودِ بلوپرینت‌ها
 * ایجاد کرده‌اند، نه یک تصمیم این قدم — طبق دستور کار، دقیقاً طبق کد مفهومی همین
 * بلوپرینت (۱۰) پیاده شد.
 */
fun generateWeatherSounds(weatherType: String, intensity: String): List<AmbientSound> {
    return when (weatherType) {
        "rain" -> listOf(AmbientSound("rain", intensity, "$intensity rain on surfaces", "weather"))
        "storm" -> listOf(
            AmbientSound("rain", "heavy", "torrential rain and wind", "weather"),
            AmbientSound("thunder", "high", "loud thunder cracks", "weather")
        )
        "fog" -> listOf(AmbientSound("ambient", "low", "eerie silence with muffled sounds", "weather"))
        else -> emptyList()
    }
}

/** تولید خودکار صدای اکشن از توضیح فیزیکی Shot (نه از Character Sound). */
fun generateActionSounds(shotDescription: String, groundState: String): List<ActionSound> {
    val sounds = mutableListOf<ActionSound>()
    if (shotDescription.contains("walk", ignoreCase = true) || shotDescription.contains("run", ignoreCase = true)) {
        sounds += ActionSound(0f, "footstep", "footsteps on $groundState surface")
    }
    return sounds
}

/**
 * پیشنهادهای صدای کاراکتر — این تابع هرگز مستقیماً audioContext را پر نمی‌کند.
 * فقط یک پیشنهاد برای نمایش در UI برمی‌گرداند؛ کاربر باید صراحتاً یکی را انتخاب یا
 * خودش description بنویسد.
 */
fun suggestBreathingSounds(activity: String, emotion: String?): BreathingSuggestion {
    var intensity = "low"
    var description = "calm breathing"
    if (activity.contains("running") || activity.contains("fighting")) {
        intensity = "heavy"; description = "heavy breathing from exertion"
    }
    if (emotion == "terrified" || emotion == "panicked") {
        intensity = "high"; description = "rapid panicked breathing"
    }
    return BreathingSuggestion(intensity, description)
    // نکته: این فقط پیشنهاد است؛ افزودن واقعی به audioContext.characterSounds
    // فقط از طریق اقدام صریح کاربر (مثل addCharacterSound، خارج از Scope این قدم) اتفاق می‌افتد.
}

/**
 * تولید کامل Audio Context برای یک Shot — بدون پر کردن character_sounds.
 * idProvider تزریق‌پذیر است (پیش‌فرض: تولید واقعی) — برای تست‌پذیری بدون Mock،
 * مشابه الگوی defaultOverrideId در واحد ۰۱.
 */
fun generateAudioContext(
    shotId: String,
    shotDescription: String,
    weatherType: String,
    weatherIntensity: String,
    groundState: String,
    idProvider: () -> String = ::defaultAudioContextId
): AudioContext {
    return AudioContext(
        audioContextId = idProvider(),
        shotId = shotId,
        ambientSounds = generateWeatherSounds(weatherType, weatherIntensity),
        actionSounds = generateActionSounds(shotDescription, groundState),
        characterSounds = emptyList() // عمداً خالی — طبق اصل بنیادی این واحد
    )
}

/** شناسه طبق قرارداد type_identifier در naming-conventions.md، مثل audio_a1b2c3d4e5f6 */
private fun defaultAudioContextId(): String =
    "audio_" + UUID.randomUUID().toString().replace("-", "").take(12)
