package com.operaboys.cinemashotgenerator.domain.audio

import com.operaboys.cinemashotgenerator.domain.shot.ActionSound
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.domain.validation.validateBeatSheetTimeline

// واحد ۱۰ — قوانین اعتبارسنجی Audio Context (Rule 1، ۲؛ Rule ۳ ساختاری، پایین توضیح داده شده)
// منبع حقیقت: docs/blueprints/10-audio-context.md

/** Rule 1 (Warning): تعداد کل لایه‌های صوتی (ambient+action+character) بیش از ۸. */
fun checkTotalSoundLayerCount(audioContext: AudioContext): ValidationIssue? {
    val total = audioContext.ambientSounds.size + audioContext.actionSounds.size + audioContext.characterSounds.size
    if (total > 8) {
        return ValidationIssue(
            Severity.WARNING,
            message = "تعداد کل لایه‌های صوتی ($total) بیش از حد مجاز (۸) است"
        )
    }
    return null
}

/**
 * Rule 2 (Blocking): Action Sound با timestamp خارج از Duration شات.
 * مستقیماً از validateBeatSheetTimeline واحد ۰۷ استفاده می‌کند (بدون تکرار منطق) —
 * همان الگوی فیلتر بازه‌ی زمانی (0..durationSeconds) که برای Beat Sheet هم استفاده
 * شده بود، عیناً برای Action Sound هم صادق است.
 */
fun validateActionSoundTimeline(actionSounds: List<ActionSound>, durationSeconds: Float): List<ValidationIssue> {
    return validateBeatSheetTimeline(actionSounds.map { it.timestampSeconds }, durationSeconds)
}

// Rule 3 (ساختاری، نه Runtime): character_sounds با source غیر از user_defined نقض
// اصل بنیادی این واحد است.
//
// تضمین: CharacterSound (بازاستفاده‌شده از واحد ۰۵، domain.shot.CharacterSound) اصلاً
// فیلد `source` ندارد — ساخت یک نمونه با source غیر از "user_defined" در سطح
// Type System اصلاً قابل‌بیان نیست، چون چنین فیلدی وجود ندارد که مقداردهی شود. این
// تضمین حتی از الگوی مشابه واحد ۰۵ (که در آن‌جا فقط «تابع auto-generate ننوشتیم»
// تضمین‌کننده بود) قوی‌تر است: اینجا کامپایلر خودش امکان بیان نادرست را حذف می‌کند.
