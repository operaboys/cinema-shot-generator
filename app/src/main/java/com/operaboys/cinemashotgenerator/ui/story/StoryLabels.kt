package com.operaboys.cinemashotgenerator.ui.story

import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.story.Genre
import com.operaboys.cinemashotgenerator.domain.story.NarrativeIntensity
import com.operaboys.cinemashotgenerator.domain.story.StoryType
import com.operaboys.cinemashotgenerator.domain.story.VisualIntent
import com.operaboys.cinemashotgenerator.ui.i18n.uiString

// واحد ۱۶ فاز ۲ — قدم ۱: نگاشت enum های StoryContext به کلید ترجمه، هم‌الگو با
// stateChipLabel در ProjectCard.kt.

fun storyTypeLabel(type: StoryType, language: Language): String = uiString(
    when (type) {
        StoryType.NARRATIVE -> "storyType.narrative"
        StoryType.CONCEPTUAL -> "storyType.conceptual"
        StoryType.VISUAL_ONLY -> "storyType.visualOnly"
        StoryType.EXPERIMENTAL -> "storyType.experimental"
    },
    language
)

fun genreLabel(genre: Genre, language: Language): String = uiString(
    when (genre) {
        Genre.ACTION -> "genre.action"
        Genre.DRAMA -> "genre.drama"
        Genre.SCI_FI -> "genre.sciFi"
        Genre.FANTASY -> "genre.fantasy"
        Genre.HORROR -> "genre.horror"
        Genre.ROMANCE -> "genre.romance"
        Genre.DOCUMENTARY -> "genre.documentary"
        Genre.EXPERIMENTAL -> "genre.experimental"
    },
    language
)

fun moodLabel(mood: Mood, language: Language): String = uiString(
    when (mood) {
        Mood.EPIC -> "mood.epic"
        Mood.ACTION -> "mood.action"
        Mood.EXCITING -> "mood.exciting"
        Mood.ENERGETIC -> "mood.energetic"
        Mood.HAPPY -> "mood.happy"
        Mood.JOYFUL -> "mood.joyful"
        Mood.PLAYFUL -> "mood.playful"
        Mood.HOPEFUL -> "mood.hopeful"
        Mood.WARM -> "mood.warm"
        Mood.ROMANTIC -> "mood.romantic"
        Mood.EMOTIONAL -> "mood.emotional"
        Mood.MELANCHOLIC -> "mood.melancholic"
        Mood.NOSTALGIC -> "mood.nostalgic"
        Mood.BITTERSWEET -> "mood.bittersweet"
        Mood.DRAMATIC -> "mood.dramatic"
        Mood.DARK -> "mood.dark"
        Mood.MYSTERIOUS -> "mood.mysterious"
        Mood.TENSE -> "mood.tense"
        Mood.SCARY -> "mood.scary"
        Mood.EERIE -> "mood.eerie"
        Mood.CALM -> "mood.calm"
        Mood.PEACEFUL -> "mood.peaceful"
        Mood.SERENE -> "mood.serene"
        Mood.CONTEMPLATIVE -> "mood.contemplative"
        Mood.DREAMY_MOOD -> "mood.dreamyMood"
    },
    language
)

fun narrativeIntensityLabel(intensity: NarrativeIntensity, language: Language): String = uiString(
    when (intensity) {
        NarrativeIntensity.MINIMAL -> "narrativeIntensity.minimal"
        NarrativeIntensity.MODERATE -> "narrativeIntensity.moderate"
        NarrativeIntensity.HIGH -> "narrativeIntensity.high"
        NarrativeIntensity.EXTREME -> "narrativeIntensity.extreme"
    },
    language
)

fun visualIntentLabel(intent: VisualIntent, language: Language): String = uiString(
    when (intent) {
        VisualIntent.REALISTIC -> "visualIntent.realistic"
        VisualIntent.CINEMATIC -> "visualIntent.cinematic"
        VisualIntent.STYLIZED -> "visualIntent.stylized"
        VisualIntent.ARTISTIC -> "visualIntent.artistic"
        VisualIntent.ABSTRACT -> "visualIntent.abstract"
    },
    language
)
