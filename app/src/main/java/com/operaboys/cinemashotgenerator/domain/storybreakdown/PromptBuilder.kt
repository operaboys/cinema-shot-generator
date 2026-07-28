package com.operaboys.cinemashotgenerator.domain.storybreakdown

import com.operaboys.cinemashotgenerator.domain.story.StoryContext
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۰۱ب — AI Story Breakdown (بخش الف: Prompt Builder)
// منبع حقیقت: docs/blueprints/01b-ai-story-breakdown.md (نسخه ۲)
//
// این بخش اول از یک واحد کاملاً جدید است؛ فقط بخش الف (Prompt Builder) و بخش پ
// (Chunk Combiner، در ChunkCombiner.kt) در این قدم پیاده شدند. بخش‌های ب (AI
// Connector)، ت (JSON Doctor)، ث (Story-to-Domain Mapper) عمداً در این قدم دست‌نخورده
// ماندند — در قدم‌های بعدی این واحد می‌آیند (طبق دستور کار صریح این قدم).
//
// StoryContext (واحد ۰۱) عیناً import شده، بدون بازتعریف.

/**
 * ورودی‌های Prompt Builder — طبق بلوپرینت، علاوه بر ۵ سؤال اصلی Story Wizard
 * (که در storyContext از قبل موجودند)، فقط سه فیلد جدید لازم است.
 */
data class StoryBreakdownRequest(
    val storyContext: StoryContext,
    val freeformStory: String,
    val targetShotCount: Int,
    val defaultShotDurationSeconds: Float = 4f
)

/**
 * دستور فرمت خروجی JSON — عیناً طبق نمونه‌ی بلوپرینت (شامل چهار کلید اصلی
 * characters/locations/objects/shots و دستور [CONTINUE] برای پاسخ‌های چندبخشی).
 */
const val STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION = """خروجی را دقیقاً در قالب JSON زیر بده، بدون هیچ توضیح اضافه قبل یا بعد از JSON:
{
  "characters": [ { "name": "...", "description": "...", "role": "main | secondary | background", "gender": "female | male | other" } ],
  "locations": [ { "name": "...", "description": "..." } ],
  "objects": [ { "name": "...", "description": "..." } ],
  "shots": [
    {
      "sceneName": "...",
      "shotNumber": 1,
      "description": "...",
      "characterNames": ["..."],
      "locationName": "...",
      "objectNames": ["..."]
    }
  ]
}

اگر پاسخ طولانی است و باید در چند بخش بفرستی، هر بخش (به‌جز آخری) را با دقیقاً این عبارت پایان بده: [CONTINUE]"""

/**
 * ساخت پرامپت درخواست از AI بیرونی — عیناً طبق الگوی متنی بلوپرینت.
 *
 * انحراف کوچک از کد مفهومی بلوپرینت: `request.storyContext.genre` در کد واقعی
 * `List<Genre>` است (نه یک مقدار تکی، چون بلوپرینت ۰۱ (Story Wizard) اجازه‌ی چند
 * ژانر همزمان می‌دهد) — با grep در `domain/story/StoryContext.kt` تأیید شد. کد
 * مفهومی بلوپرینت `${request.storyContext.genre}` را مستقیم Interpolate می‌کرد که
 * روی یک `List` خروجی پیش‌فرض Kotlin (`[ACTION, DRAMA]`، با براکت) می‌داد — برای
 * پرامپتی که قرار است متن طبیعی خوانا برای یک AI باشد، `joinToString("، ")` انتخاب
 * شد تا فهرست ژانرها به‌صورت طبیعی («ACTION، DRAMA») نوشته شود، نه با نحو Kotlin.
 */
fun buildStoryBreakdownPrompt(request: StoryBreakdownRequest): String {
    val totalDuration = request.targetShotCount * request.defaultShotDurationSeconds
    return buildString {
        appendLine("تو یک نویسنده/کارگردان حرفه‌ای هستی. داستان زیر را به دقیق ${request.targetShotCount} شات سینمایی تقسیم کن،")
        appendLine("هرکدام حدود ${request.defaultShotDurationSeconds} ثانیه (مدت کل تقریبی: $totalDuration ثانیه).")
        appendLine()
        appendLine(
            "سبک/حال‌وهوا: ${request.storyContext.genre.joinToString("، ")}، " +
                "${request.storyContext.moodPrimary}، هدف بصری ${request.storyContext.visualIntent}."
        )
        appendLine()
        appendLine("داستان:")
        appendLine(request.freeformStory)
        appendLine()
        append(STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION)
    }
}

/** Rule 1 (Blocking): freeformStory خالی یا کمتر از ۵۰ کاراکتر. */
fun validateFreeformStoryLength(freeformStory: String): ValidationIssue? {
    if (freeformStory.trim().length < 50) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "داستان آزاد باید حداقل ۵۰ کاراکتر باشد؛ متن فعلی خالی یا بسیار کوتاه است"
        )
    }
    return null
}

/** Rule 2 (Blocking): targetShotCount خارج از محدوده‌ی معقول ۱ تا ۱۵۰. */
fun validateTargetShotCountRange(targetShotCount: Int): ValidationIssue? {
    if (targetShotCount < 1 || targetShotCount > 150) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "تعداد شات باید بین ۱ تا ۱۵۰ باشد؛ مقدار وارد‌شده ($targetShotCount) خارج از این محدوده است"
        )
    }
    return null
}

/**
 * Rule 3 (Warning): targetShotCount بیش از ۴۰ — یعنی AI بیرونی احتمالاً پاسخ را در
 * چند بخش با [CONTINUE] می‌فرستد؛ این طبیعی است، نه خطا.
 */
fun validateHighShotCount(targetShotCount: Int): ValidationIssue? {
    if (targetShotCount > 40) {
        return ValidationIssue(
            Severity.WARNING,
            message = "تعداد شات درخواستی ($targetShotCount) بیش از ۴۰ است؛ AI بیرونی ممکن است پاسخ را در چند بخش با [CONTINUE] بفرستد"
        )
    }
    return null
}
