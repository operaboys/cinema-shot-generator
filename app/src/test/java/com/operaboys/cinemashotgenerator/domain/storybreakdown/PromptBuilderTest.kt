package com.operaboys.cinemashotgenerator.domain.storybreakdown

import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.story.CompletionStatus
import com.operaboys.cinemashotgenerator.domain.story.Genre
import com.operaboys.cinemashotgenerator.domain.story.StoryContext
import com.operaboys.cinemashotgenerator.domain.story.StoryType
import com.operaboys.cinemashotgenerator.domain.story.VisualIntent
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    private fun sampleStoryContext() = StoryContext(
        storyType = StoryType.NARRATIVE,
        genre = listOf(Genre.DRAMA, Genre.SCI_FI),
        moodPrimary = Mood.MYSTERIOUS,
        visualIntent = VisualIntent.CINEMATIC,
        createdAt = "2026-07-28T10:00:00Z",
        completionStatus = CompletionStatus.COMPLETE
    )

    private val longStory = "کارآگاهی در شبی بارانی وارد دفتر خود می‌شود و متوجه می‌شود پرونده‌ای گم شده است. " +
        "او تصمیم می‌گیرد ردی از دزد را دنبال کند و در طول شهر به دنبال سرنخ‌ها بگردد."

    // --- buildStoryBreakdownPrompt ---

    @Test
    fun `buildStoryBreakdownPrompt includes the freeform story text`() {
        val prompt = buildStoryBreakdownPrompt(
            StoryBreakdownRequest(storyContext = sampleStoryContext(), freeformStory = longStory, targetShotCount = 10)
        )
        assertTrue(prompt.contains(longStory))
    }

    @Test
    fun `buildStoryBreakdownPrompt includes the target shot count and computed total duration`() {
        val prompt = buildStoryBreakdownPrompt(
            StoryBreakdownRequest(
                storyContext = sampleStoryContext(),
                freeformStory = longStory,
                targetShotCount = 10,
                defaultShotDurationSeconds = 5f
            )
        )
        assertTrue(prompt.contains("دقیق 10 شات"))
        assertTrue(prompt.contains("50.0 ثانیه"))
    }

    @Test
    fun `buildStoryBreakdownPrompt includes genre, mood, and visual intent from StoryContext`() {
        val prompt = buildStoryBreakdownPrompt(
            StoryBreakdownRequest(storyContext = sampleStoryContext(), freeformStory = longStory, targetShotCount = 10)
        )
        assertTrue(prompt.contains("DRAMA"))
        assertTrue(prompt.contains("SCI_FI"))
        assertTrue(prompt.contains("MYSTERIOUS"))
        assertTrue(prompt.contains("CINEMATIC"))
    }

    @Test
    fun `buildStoryBreakdownPrompt includes the JSON schema instruction and CONTINUE directive`() {
        val prompt = buildStoryBreakdownPrompt(
            StoryBreakdownRequest(storyContext = sampleStoryContext(), freeformStory = longStory, targetShotCount = 10)
        )
        assertTrue(prompt.contains("\"characters\""))
        assertTrue(prompt.contains("\"shots\""))
        assertTrue(prompt.contains("[CONTINUE]"))
    }

    // --- قدم ۱ از ۳ زیرقدم «سیستم Preview دوزبانه‌ی پرامپت» (ADR-121):
    // STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION (previewLanguageEnabled=false،
    // پیش‌فرض) و STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION_BILINGUAL
    // (previewLanguageEnabled=true) ---

    @Test
    fun `STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION (single-language, unchanged) uses a single description field, never descriptionEn or descriptionFa`() {
        assertTrue(STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION.contains("\"description\": \"...\""))
        assertTrue(!STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION.contains("descriptionEn"))
        assertTrue(!STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION.contains("descriptionFa"))
    }

    @Test
    fun `STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION_BILINGUAL requires both descriptionEn and descriptionFa for all four item types`() {
        val instruction = STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION_BILINGUAL
        assertEquals(4, Regex("\"descriptionEn\"").findAll(instruction).count())
        assertEquals(4, Regex("\"descriptionFa\"").findAll(instruction).count())
        assertTrue("باید [CONTINUE] هم در نسخه‌ی دوزبانه حفظ شده باشد", instruction.contains("[CONTINUE]"))
    }

    @Test
    fun `STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION_BILINGUAL explicitly instructs the AI that the two fields must never be mixed into one string`() {
        val instruction = STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION_BILINGUAL
        assertTrue(
            "دستور صریح عدم‌ترکیب دو زبان در یک رشته باید در متن باشد",
            instruction.contains("قاطی") || instruction.contains("ترکیب نکن")
        )
    }

    @Test
    fun `buildStoryBreakdownPrompt with previewLanguageEnabled=false uses the single-language schema and no bilingual fields`() {
        val prompt = buildStoryBreakdownPrompt(
            StoryBreakdownRequest(
                storyContext = sampleStoryContext(),
                freeformStory = longStory,
                targetShotCount = 10,
                previewLanguageEnabled = false
            )
        )
        assertTrue(prompt.contains("\"description\": \"...\""))
        assertTrue(!prompt.contains("descriptionEn"))
        assertTrue(!prompt.contains("descriptionFa"))
    }

    @Test
    fun `buildStoryBreakdownPrompt with previewLanguageEnabled=true uses the bilingual schema and explains why to the AI`() {
        val prompt = buildStoryBreakdownPrompt(
            StoryBreakdownRequest(
                storyContext = sampleStoryContext(),
                freeformStory = longStory,
                targetShotCount = 10,
                previewLanguageEnabled = true
            )
        )
        assertTrue(prompt.contains("descriptionEn"))
        assertTrue(prompt.contains("descriptionFa"))
        assertTrue(
            "پرامپت باید توضیح دهد چرا این دو فیلد لازم است (کاربر فارسی‌زبان می‌خواهد محتوا را مرور کند)",
            prompt.contains("فارسی‌زبان")
        )
    }

    // --- Rule 1: freeformStory کمتر از ۵۰ کاراکتر ---

    @Test
    fun `rule1 empty freeformStory is blocking`() {
        val issue = validateFreeformStoryLength("")
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `rule1 freeformStory shorter than 50 characters is blocking`() {
        val issue = validateFreeformStoryLength("داستان خیلی کوتاه")
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `rule1 freeformStory of at least 50 characters is valid`() {
        assertNull(validateFreeformStoryLength(longStory))
    }

    // --- Rule 2: targetShotCount خارج از ۱ تا ۱۵۰ ---

    @Test
    fun `rule2 targetShotCount below 1 is blocking`() {
        val issue = validateTargetShotCountRange(0)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `rule2 targetShotCount above 150 is blocking`() {
        val issue = validateTargetShotCountRange(151)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `rule2 targetShotCount within range is valid`() {
        assertNull(validateTargetShotCountRange(50))
        assertNull(validateTargetShotCountRange(1))
        assertNull(validateTargetShotCountRange(150))
    }

    // --- Rule 3: targetShotCount بیش از ۴۰ (Warning) ---

    @Test
    fun `rule3 targetShotCount above 40 warns`() {
        val issue = validateHighShotCount(41)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `rule3 targetShotCount of 40 or less does not warn`() {
        assertNull(validateHighShotCount(40))
        assertNull(validateHighShotCount(10))
    }
}
