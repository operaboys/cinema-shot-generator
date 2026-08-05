package com.operaboys.cinemashotgenerator.ui.shots

import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.workflow.ShotListViewMode
import com.operaboys.cinemashotgenerator.ui.i18n.uiString

// واحد ۱۶ فاز ۴ — قدم ۲: نگاشت enum های واحد ۰۵ (Shot Engine) به کلید ترجمه،
// هم‌الگو با ui/scenes/SceneLabels.kt.

fun shotGoalLabel(goal: ShotGoal, language: Language): String = uiString(
    when (goal) {
        ShotGoal.ESTABLISHING -> "shotGoal.establishing"
        ShotGoal.ACTION -> "shotGoal.action"
        ShotGoal.EMOTIONAL -> "shotGoal.emotional"
        ShotGoal.DIALOGUE -> "shotGoal.dialogue"
        ShotGoal.TRANSITION -> "shotGoal.transition"
    },
    language
)

fun shotTypeLabel(type: ShotType, language: Language): String = uiString(
    when (type) {
        ShotType.EXTREME_WIDE -> "shotType.extremeWide"
        ShotType.WIDE -> "shotType.wide"
        ShotType.MEDIUM -> "shotType.medium"
        ShotType.CLOSE_UP -> "shotType.closeUp"
        ShotType.EXTREME_CLOSE_UP -> "shotType.extremeCloseUp"
    },
    language
)

fun motionLevelLabel(level: MotionLevel, language: Language): String = uiString(
    when (level) {
        MotionLevel.STATIC -> "motionLevel.static"
        MotionLevel.SUBTLE -> "motionLevel.subtle"
        MotionLevel.MODERATE -> "motionLevel.moderate"
        MotionLevel.DYNAMIC -> "motionLevel.dynamic"
        MotionLevel.EXTREME -> "motionLevel.extreme"
    },
    language
)

fun shotListViewModeLabel(mode: ShotListViewMode, language: Language): String = uiString(
    when (mode) {
        ShotListViewMode.GRID -> "shotsList.gridView"
        ShotListViewMode.TIMELINE -> "shotsList.timelineView"
    },
    language
)

/** طبق سند طراحی («Shot cards: code (03-SH.01)»): sceneNumber-SH.shotNumber، هر دو دو-رقمی. */
fun shotCode(sceneNumber: Int, shotNumber: Int): String = "%02d-SH.%02d".format(sceneNumber, shotNumber)

/** بدون معادل بلوپرینتی صریح برای Fallback عنوان Shot — هم‌الگو با sceneDisplayTitle (ui/scenes/SceneLabels.kt): shotTitle یا نسخه‌ی کوتاه‌شده‌ی shotDescription. */
fun shotDisplayTitle(shotTitle: String?, shotDescription: String): String =
    shotTitle ?: if (shotDescription.length > 40) shotDescription.take(40) + "…" else shotDescription
