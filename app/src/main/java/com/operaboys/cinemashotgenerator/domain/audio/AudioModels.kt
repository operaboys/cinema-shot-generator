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
// - AmbientSound این واحد در زمان نوشتن این تصمیم یک فیلد اضافه (`source`) داشت که
//   در AmbientSound واحد ۰۵ نبود — نوع محلی جدید تعریف شد. طبق MIGRATED
//   (docs/adr/074-...): این تفاوت رفع شد، AmbientSound واحد ۰۵ هم اکنون همین فیلد
//   را دارد (پیش‌فرض متفاوت: "auto_generated" آن‌جا در برابر پیش‌فرض این واحد در
//   پایین). دو نوع همچنان جدا نگه داشته شدند (نه یکی‌سازی) چون این تصمیم محدوده‌ی
//   Migration نسخه ۳ واحد ۰۵ نبود؛ AmbientSoundSuggestion واحد ۰۸ هنوز فاقد این
//   فیلد است (خودِ Suggestion، نه AmbientSound نهایی).

/**
 * طبق ساختار JSON این بلوپرینت، `source` provenance صدای محیطی (مثلاً "weather")
 * را در سطح AudioContext نهایی نگه می‌دارد. MIGRATED (docs/adr/074-...): AmbientSound
 * واحد ۰۵ هم اکنون همین فیلد را دارد (پیش‌فرض "auto_generated" آن‌جا)؛ اینجا پیش‌فرض
 * ندارد چون این واحد همیشه source واقعی (مثلاً "weather") را صریح پر می‌کند.
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
