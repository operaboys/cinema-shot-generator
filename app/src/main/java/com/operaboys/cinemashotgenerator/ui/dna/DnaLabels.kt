package com.operaboys.cinemashotgenerator.ui.dna

import com.operaboys.cinemashotgenerator.domain.dna.AspectRatio
import com.operaboys.cinemashotgenerator.domain.dna.ColorTemperature
import com.operaboys.cinemashotgenerator.domain.dna.ContrastLevel
import com.operaboys.cinemashotgenerator.domain.dna.LightingCategory
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.dna.MoodCategory
import com.operaboys.cinemashotgenerator.domain.dna.RealismLevel
import com.operaboys.cinemashotgenerator.domain.dna.SaturationLevel
import com.operaboys.cinemashotgenerator.domain.dna.StyleConsistency
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyleCategory
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.i18n.uiString

// واحد ۱۶ فاز ۲ — قدم ۳: نگاشت enum های ProjectDna به کلید ترجمه، هم‌الگو با
// ui/story/StoryLabels.kt. `moodLabel` قبلاً در StoryLabels.kt تعریف شده
// (domain.dna.Mood از ADR-027 مهاجرت کرد و از آن قدم به بعد مالک واحد نام «Mood»
// است) و مستقیماً از همان‌جا برای مقادیر Mood این صفحه هم بازاستفاده می‌شود — فقط
// برچسب‌های دسته‌ی Mood (که در Story Tab لازم نبود) اینجا اضافه شده‌اند.

fun visualStyleCategoryLabel(category: VisualStyleCategory, language: Language): String = uiString(
    when (category) {
        VisualStyleCategory.CINEMATIC -> "visualStyleCategory.cinematic"
        VisualStyleCategory.ANIMATION_3D -> "visualStyleCategory.animation3d"
        VisualStyleCategory.ANIMATION_2D -> "visualStyleCategory.animation2d"
        VisualStyleCategory.ARTISTIC -> "visualStyleCategory.artistic"
        VisualStyleCategory.GENRE -> "visualStyleCategory.genre"
    },
    language
)

fun visualStyleLabel(style: VisualStyle, language: Language): String = uiString(
    when (style) {
        VisualStyle.CINEMATIC_STYLE -> "visualStyle.cinematicStyle"
        VisualStyle.PHOTOREALISTIC -> "visualStyle.photorealistic"
        VisualStyle.FILM_NOIR -> "visualStyle.filmNoir"
        VisualStyle.VINTAGE_RETRO -> "visualStyle.vintageRetro"
        VisualStyle.DOCUMENTARY -> "visualStyle.documentary"
        VisualStyle.BLOCKBUSTER -> "visualStyle.blockbuster"
        VisualStyle.INDIE_ARTHOUSE -> "visualStyle.indieArthouse"
        VisualStyle.PIXAR_DISNEY -> "visualStyle.pixarDisney"
        VisualStyle.DREAMWORKS -> "visualStyle.dreamworks"
        VisualStyle.ILLUMINATION -> "visualStyle.illumination"
        VisualStyle.LOW_POLY -> "visualStyle.lowPoly"
        VisualStyle.CLAYMATION -> "visualStyle.claymation"
        VisualStyle.ISOMETRIC -> "visualStyle.isometric"
        VisualStyle.ANIME -> "visualStyle.anime"
        VisualStyle.STUDIO_GHIBLI -> "visualStyle.studioGhibli"
        VisualStyle.DISNEY_CLASSIC -> "visualStyle.disneyClassic"
        VisualStyle.CARTOON -> "visualStyle.cartoon"
        VisualStyle.COMIC_BOOK -> "visualStyle.comicBook"
        VisualStyle.MANGA -> "visualStyle.manga"
        VisualStyle.WATERCOLOR -> "visualStyle.watercolor"
        VisualStyle.OIL_PAINTING -> "visualStyle.oilPainting"
        VisualStyle.PENCIL_SKETCH -> "visualStyle.pencilSketch"
        VisualStyle.IMPRESSIONIST -> "visualStyle.impressionist"
        VisualStyle.POP_ART -> "visualStyle.popArt"
        VisualStyle.ART_NOUVEAU -> "visualStyle.artNouveau"
        VisualStyle.MINIMALIST -> "visualStyle.minimalist"
        VisualStyle.EPIC_FANTASY -> "visualStyle.epicFantasy"
        VisualStyle.SCI_FI -> "visualStyle.sciFi"
        VisualStyle.CYBERPUNK -> "visualStyle.cyberpunk"
        VisualStyle.STEAMPUNK -> "visualStyle.steampunk"
        VisualStyle.GOTHIC -> "visualStyle.gothic"
        VisualStyle.HORROR -> "visualStyle.horror"
        VisualStyle.SURREAL -> "visualStyle.surreal"
        VisualStyle.DREAMY -> "visualStyle.dreamy"
    },
    language
)

fun moodCategoryLabel(category: MoodCategory, language: Language): String = uiString(
    when (category) {
        MoodCategory.HIGH_ENERGY -> "moodCategory.highEnergy"
        MoodCategory.POSITIVE -> "moodCategory.positive"
        MoodCategory.EMOTIONAL -> "moodCategory.emotional"
        MoodCategory.DARK -> "moodCategory.dark"
        MoodCategory.CALM -> "moodCategory.calm"
    },
    language
)

fun lightingCategoryLabel(category: LightingCategory, language: Language): String = uiString(
    when (category) {
        LightingCategory.NATURAL -> "lightingCategory.natural"
        LightingCategory.NIGHT -> "lightingCategory.night"
        LightingCategory.STUDIO -> "lightingCategory.studio"
        LightingCategory.SPECIAL -> "lightingCategory.special"
    },
    language
)

fun lightingStyleLabel(style: LightingStyle, language: Language): String = uiString(
    when (style) {
        LightingStyle.NATURAL_LIGHT -> "lightingStyle.naturalLight"
        LightingStyle.DAYLIGHT -> "lightingStyle.daylight"
        LightingStyle.OVERCAST -> "lightingStyle.overcast"
        LightingStyle.GOLDEN_HOUR -> "lightingStyle.goldenHour"
        LightingStyle.BLUE_HOUR -> "lightingStyle.blueHour"
        LightingStyle.SUNSET -> "lightingStyle.sunset"
        LightingStyle.SUNRISE -> "lightingStyle.sunrise"
        LightingStyle.MOONLIGHT -> "lightingStyle.moonlight"
        LightingStyle.STARLIGHT -> "lightingStyle.starlight"
        LightingStyle.CITY_NIGHT -> "lightingStyle.cityNight"
        LightingStyle.SOFT_LIGHT -> "lightingStyle.softLight"
        LightingStyle.DRAMATIC_LIGHT -> "lightingStyle.dramaticLight"
        LightingStyle.HIGH_KEY -> "lightingStyle.highKey"
        LightingStyle.LOW_KEY -> "lightingStyle.lowKey"
        LightingStyle.RIM_LIGHT -> "lightingStyle.rimLight"
        LightingStyle.BACKLIT -> "lightingStyle.backlit"
        LightingStyle.SIDE_LIGHT -> "lightingStyle.sideLight"
        LightingStyle.NEON -> "lightingStyle.neon"
        LightingStyle.VOLUMETRIC -> "lightingStyle.volumetric"
        LightingStyle.CANDLELIGHT -> "lightingStyle.candlelight"
        LightingStyle.FIRELIGHT -> "lightingStyle.firelight"
        LightingStyle.BIOLUMINESCENT -> "lightingStyle.bioluminescent"
    },
    language
)

fun realismLevelLabel(level: RealismLevel, language: Language): String = uiString(
    when (level) {
        RealismLevel.GROUNDED -> "realismLevel.grounded"
        RealismLevel.SEMI_REALISTIC -> "realismLevel.semiRealistic"
        RealismLevel.FANTASTICAL -> "realismLevel.fantastical"
    },
    language
)

fun styleConsistencyLabel(consistency: StyleConsistency, language: Language): String = uiString(
    when (consistency) {
        StyleConsistency.STRICT -> "styleConsistency.strict"
        StyleConsistency.MODERATE -> "styleConsistency.moderate"
        StyleConsistency.FLEXIBLE -> "styleConsistency.flexible"
    },
    language
)

fun colorTemperatureLabel(temperature: ColorTemperature, language: Language): String = uiString(
    when (temperature) {
        ColorTemperature.WARM -> "colorTemperature.warm"
        ColorTemperature.COOL -> "colorTemperature.cool"
        ColorTemperature.NEUTRAL -> "colorTemperature.neutral"
    },
    language
)

fun saturationLevelLabel(level: SaturationLevel, language: Language): String = uiString(
    when (level) {
        SaturationLevel.LOW -> "saturationLevel.low"
        SaturationLevel.MEDIUM -> "saturationLevel.medium"
        SaturationLevel.HIGH -> "saturationLevel.high"
        SaturationLevel.VERY_HIGH -> "saturationLevel.veryHigh"
    },
    language
)

fun contrastLevelLabel(level: ContrastLevel, language: Language): String = uiString(
    when (level) {
        ContrastLevel.LOW -> "contrastLevel.low"
        ContrastLevel.MEDIUM -> "contrastLevel.medium"
        ContrastLevel.MEDIUM_HIGH -> "contrastLevel.mediumHigh"
        ContrastLevel.HIGH -> "contrastLevel.high"
    },
    language
)

/** [AspectRatio.displayValue] خودش خوانا و مستقل از زبان است (مثل "16:9")؛ نیازی به ترجمه‌ی جدا ندارد. */
fun aspectRatioLabel(ratio: AspectRatio): String = ratio.displayValue
