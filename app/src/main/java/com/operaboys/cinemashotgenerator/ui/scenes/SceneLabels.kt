package com.operaboys.cinemashotgenerator.ui.scenes

import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate

// واحد ۱۶ فاز ۴ — قدم ۱: نگاشت enum های واحد ۰۴ (Scene Engine) به کلید ترجمه،
// هم‌الگو با ui/dna/DnaLabels.kt و ui/assets/AssetLabels.kt.

fun narrativeRoleLabel(role: NarrativeRole, language: Language): String = uiString(
    when (role) {
        NarrativeRole.INTRODUCTION -> "narrativeRole.introduction"
        NarrativeRole.DEVELOPMENT -> "narrativeRole.development"
        NarrativeRole.CLIMAX -> "narrativeRole.climax"
        NarrativeRole.RESOLUTION -> "narrativeRole.resolution"
        NarrativeRole.TRANSITION -> "narrativeRole.transition"
    },
    language
)

fun timeOfDayLabel(time: TimeOfDay, language: Language): String = uiString(
    when (time) {
        TimeOfDay.DAWN -> "timeOfDay.dawn"
        TimeOfDay.MORNING -> "timeOfDay.morning"
        TimeOfDay.NOON -> "timeOfDay.noon"
        TimeOfDay.AFTERNOON -> "timeOfDay.afternoon"
        TimeOfDay.SUNSET -> "timeOfDay.sunset"
        TimeOfDay.NIGHT -> "timeOfDay.night"
    },
    language
)

fun atmosphereLabel(atmosphere: Atmosphere, language: Language): String = uiString(
    when (atmosphere) {
        Atmosphere.CALM -> "atmosphere.calm"
        Atmosphere.TENSE -> "atmosphere.tense"
        Atmosphere.DARK -> "atmosphere.dark"
        Atmosphere.BRIGHT -> "atmosphere.bright"
        Atmosphere.MYSTERIOUS -> "atmosphere.mysterious"
        Atmosphere.EMOTIONAL -> "atmosphere.emotional"
    },
    language
)

/**
 * [domain.scene.LocationType] از [com.operaboys.cinemashotgenerator.domain.asset.LocationType]
 * (واحد ۰۶، فاز ۳ قدم ۱) مستقل است — دو enum جدا با همان ۴ مقدار (طبق همان اصل
 * «هرگز یک enum مشترک» که برای این دو مفهوم قبلاً در ADR-048 اعمال شد). چون متن
 * ترجمه‌شده‌ی هر ۴ مقدار عیناً یکسان است، همان کلیدهای موجود "locationType.*"
 * بازاستفاده شدند — کلید ترجمه‌ی تکراری جدید ساخته نشد.
 */
fun sceneLocationTypeLabel(type: LocationType, language: Language): String = uiString(
    when (type) {
        LocationType.INDOOR -> "locationType.indoor"
        LocationType.OUTDOOR -> "locationType.outdoor"
        LocationType.MIXED -> "locationType.mixed"
        LocationType.CUSTOM -> "locationType.custom"
    },
    language
)

/**
 * کلیدهای موجود "project.state.*" (`ProjectCard.kt`) عمداً بازاستفاده شدند — با
 * اینکه نامشان با پیشوند «project» است، `EntityState` مفهومی سراسری و مشترک بین
 * Project/Scene/Shot/Asset است (طبق docs/blueprints/16-user-workflow-v2.md)؛ سند
 * طراحی هم صریحاً می‌گوید چیپ وضعیت Scene باید «matching project states» باشد —
 * ساختن کلیدهای موازی «scene.state.*» با همان متن، تکرار بی‌دلیل رشته بود.
 */
fun entityStateLabel(state: EntityState, language: Language): String = uiString(
    when (state) {
        EntityState.DRAFT -> "project.state.draft"
        EntityState.REVIEW -> "project.state.review"
        EntityState.LOCKED -> "project.state.locked"
        EntityState.FINAL -> "project.state.final"
        EntityState.ARCHIVED -> "project.state.archived"
    },
    language
)

/** طبق بلوپرینت ۱۶ («یک Scene خالی با نام پیش‌فرض «Scene N» بلافاصله ساخته می‌شود»؛ localized. */
fun sceneDisplayTitle(sceneTitle: String?, sceneNumber: Int, language: Language): String =
    sceneTitle ?: uiTemplate("scene.defaultTitleTemplate", language, "number" to sceneNumber.toString())
