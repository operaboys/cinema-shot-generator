package com.operaboys.cinemashotgenerator.domain.audio

import com.operaboys.cinemashotgenerator.domain.shot.ActionSound
import com.operaboys.cinemashotgenerator.domain.shot.CharacterSound

// واحد ۱۰ — Audio Context Generator (ساختار داده)
// منبع حقیقت: docs/blueprints/10-audio-context.md
//
// تصمیم یکی‌سازی سه نوع «صدا» (بررسی‌شده پیش از کدنویسی، طبق دستور کار — تصمیم محلی،
// نه سؤال معماری؛ جزئیات کامل در docs/adr/011-unit10-audio-context-deviations.md):
// - ActionSound و CharacterSound این واحد دقیقاً هم‌شکل با نسخه‌ی از قبل پیاده‌شده‌ی
//   واحد ۰۵ (domain.shot) هستند (صفر تفاوت فیلد) — بازاستفاده شدند، نه بازتعریف.
// - AmbientSound این واحد اما یک فیلد اضافه (`source`) دارد که نه در AmbientSound
//   واحد ۰۵ و نه در AmbientSoundSuggestion واحد ۰۸ وجود دارد؛ چون این تفاوت واقعی و
//   طبق ساختار JSON خودِ این بلوپرینت است (نه یک شباهت تصادفی)، نوع محلی جدید
//   تعریف شد.

/**
 * برخلاف AmbientSound واحد ۰۵ (بدون source) و AmbientSoundSuggestion واحد ۰۸ (بدون
 * source)، این نسخه فیلد `source` دارد — طبق ساختار JSON این بلوپرینت که provenance
 * صدای محیطی (مثلاً "weather") را در سطح AudioContext نهایی نگه می‌دارد.
 */
data class AmbientSound(val type: String, val intensity: String, val description: String, val source: String)

data class AudioContext(
    val audioContextId: String,
    val shotId: String,
    val ambientSounds: List<AmbientSound> = emptyList(),
    val actionSounds: List<ActionSound> = emptyList(),
    val characterSounds: List<CharacterSound> = emptyList() // خالی می‌ماند مگر کاربر پر کند
)

/**
 * پیشنهاد صدای نفس — هرگز مستقیماً audioContext را پر نمی‌کند، فقط برای نمایش در UI
 * است؛ افزودن واقعی فقط با اقدام صریح کاربر اتفاق می‌افتد.
 */
data class BreathingSuggestion(val intensity: String, val description: String)
