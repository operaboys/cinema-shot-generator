package com.operaboys.cinemashotgenerator.domain.storybreakdown

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۰۱ب — AI Story Breakdown (بخش پ: Chunk Combiner)
// منبع حقیقت: docs/blueprints/01b-ai-story-breakdown.md (نسخه ۲)
//
// وقتی targetShotCount بزرگ باشد، AI بیرونی ممکن است پاسخ را در چند پیام جدا بفرستد
// (هر پیام با [CONTINUE] تمام می‌شود، طبق دستور STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION
// در PromptBuilder.kt). این فایل این تکه‌ها را قبل از تلاش برای Parse JSON می‌چسباند.

private const val CONTINUE_MARKER = "[CONTINUE]"

/** تشخیص می‌دهد آیا یک بخش پاسخ ناقص است (با [CONTINUE] تمام شده) و باید منتظر بخش بعدی ماند. */
fun isPartialResponse(chunk: String): Boolean = chunk.trimEnd().endsWith(CONTINUE_MARKER)

/**
 * چسباندن هوشمند چند تکه به یک متن واحد — [CONTINUE] از انتهای هر تکه حذف می‌شود.
 * ترتیب چسباندن دقیقاً همان ترتیب لیست ورودی است (بدون بازچینی خودکار)، عیناً طبق
 * کد مفهومی بلوپرینت.
 */
fun smartCombineChunks(chunks: List<String>): String {
    return chunks.joinToString(separator = "") { chunk ->
        chunk.trimEnd().removeSuffix(CONTINUE_MARKER).trimEnd()
    }
}

/**
 * Rule 6 (Warning): آخرین تکه‌ی واردشده هنوز با [CONTINUE] تمام می‌شود — یعنی کاربر
 * فراموش کرده تکه‌ی بعدی را اضافه کند.
 */
fun validateChunksComplete(chunks: List<String>): ValidationIssue? {
    if (chunks.isNotEmpty() && isPartialResponse(chunks.last())) {
        return ValidationIssue(
            Severity.WARNING,
            message = "پاسخ هنوز کامل نیست؛ تکه‌ی آخر با [CONTINUE] تمام شده — لطفاً بخش بعدی پاسخ را هم اضافه کنید"
        )
    }
    return null
}
