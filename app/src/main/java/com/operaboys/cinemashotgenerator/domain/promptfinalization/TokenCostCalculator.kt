package com.operaboys.cinemashotgenerator.domain.promptfinalization

import com.operaboys.cinemashotgenerator.domain.outputdelivery.ModelProfile

// واحد ۱۳ — Prompt Finalization Pipeline (بخش ب: Token Cost Calculator)
// منبع حقیقت: docs/blueprints/13-prompt-finalization.md
//
// modelProfile از نوع واقعی domain.outputdelivery.ModelProfile (واحد ۱۴، از قبل
// پیاده‌شده) import شده — بدون بازتعریف؛ Budget Tracking/Cost Estimation طبق بلوپرینت
// یک فیچر اختیاری خارج از Scope اصلی است و پیاده نشد.

fun estimateTokensFromCharacters(text: String): Int = (text.length / 4.0).let { Math.ceil(it).toInt() }

fun estimateTokensFromWords(text: String): Int {
    val wordCount = text.trim().split(Regex("\\s+")).size
    return Math.ceil(wordCount * 1.33).toInt()
}

data class TokenCheckResult(val estimatedTokens: Int, val maxTokens: Int, val withinLimit: Boolean, val warning: String?)

/** بررسی نهایی محدودیت توکن — این تنها جایی است که این چک انجام می‌شود (بعد از Rendering، برای یک مدل مشخص). */
fun checkTokenLimit(cleanedText: String, modelProfile: ModelProfile): TokenCheckResult {
    val estimated = estimateTokensFromWords(cleanedText)
    val max = modelProfile.constraints.maxTokens
    return TokenCheckResult(
        estimatedTokens = estimated,
        maxTokens = max,
        withinLimit = estimated <= max,
        warning = if (estimated > max) "پرامپت (${estimated} توکن) از محدودیت این مدل (${max}) بیشتر است" else null
    )
}
