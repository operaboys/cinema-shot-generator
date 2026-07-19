package com.operaboys.cinemashotgenerator.domain.story

// واحد ۰۱ — Story Wizard (بخش الف)
// منبع حقیقت: docs/blueprints/01-story-and-override.md

enum class StoryType { NARRATIVE, CONCEPTUAL, VISUAL_ONLY, EXPERIMENTAL }

enum class Genre { ACTION, DRAMA, SCI_FI, FANTASY, HORROR, ROMANCE, DOCUMENTARY, EXPERIMENTAL }

enum class Mood { CALM, DARK, EPIC, EMOTIONAL, MYSTERIOUS, TENSE, HOPEFUL, MELANCHOLIC }

enum class NarrativeIntensity { MINIMAL, MODERATE, HIGH, EXTREME }

enum class VisualIntent { REALISTIC, CINEMATIC, STYLIZED, ARTISTIC, ABSTRACT }

enum class CompletionStatus { COMPLETE, PARTIAL }

/**
 * چارچوب مفهومی پروژه — خروجی Story Wizard.
 *
 * فیلدهای الزامی ویزارد (مراحل ۱ تا ۵) در خودِ نوع داده non-null هستند؛
 * تنها فیلد الزامیِ قابل‌خالی‌بودن `genre` است (لیست تهی = پر نشده).
 * معادل snake_case این فیلدها در JSON/Entity در فاز سریالایز/Room مپ می‌شود.
 */
data class StoryContext(
    val storyType: StoryType,
    val genre: List<Genre>,
    val moodPrimary: Mood,
    val moodSecondary: Mood? = null,
    val narrativeIntensity: NarrativeIntensity = NarrativeIntensity.MODERATE,
    val visualIntent: VisualIntent = VisualIntent.CINEMATIC,
    val createdAt: String,
    val completionStatus: CompletionStatus
)
