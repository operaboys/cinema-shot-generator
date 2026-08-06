package com.operaboys.cinemashotgenerator.ui.shots

import com.operaboys.cinemashotgenerator.domain.camera.AdvancedMovementType
import com.operaboys.cinemashotgenerator.domain.camera.BasicMovementType
import com.operaboys.cinemashotgenerator.domain.camera.CameraAngle
import com.operaboys.cinemashotgenerator.domain.camera.CameraDistance
import com.operaboys.cinemashotgenerator.domain.camera.DepthOfField
import com.operaboys.cinemashotgenerator.domain.camera.Framing
import com.operaboys.cinemashotgenerator.domain.camera.FocusMode
import com.operaboys.cinemashotgenerator.domain.camera.LensType
import com.operaboys.cinemashotgenerator.domain.camera.Stabilization
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.i18n.uiString

// واحد ۱۶ فاز ۴ — قدم ۳: نگاشت enum های واحد ۰۹ (Camera & Motion، بخش الف) به
// کلید ترجمه، هم‌الگو با ui/shots/ShotLabels.kt.

fun cameraAngleLabel(value: CameraAngle, language: Language): String = uiString(
    when (value) {
        CameraAngle.EYE_LEVEL -> "cameraAngle.eyeLevel"
        CameraAngle.LOW -> "cameraAngle.low"
        CameraAngle.HIGH -> "cameraAngle.high"
        CameraAngle.OVERHEAD -> "cameraAngle.overhead"
        CameraAngle.DUTCH -> "cameraAngle.dutch"
        CameraAngle.POV -> "cameraAngle.pov"
    },
    language
)

fun cameraDistanceLabel(value: CameraDistance, language: Language): String = uiString(
    when (value) {
        CameraDistance.EXTREME_WIDE -> "cameraDistance.extremeWide"
        CameraDistance.WIDE -> "cameraDistance.wide"
        CameraDistance.MEDIUM -> "cameraDistance.medium"
        CameraDistance.CLOSE_UP -> "cameraDistance.closeUp"
        CameraDistance.EXTREME_CLOSE_UP -> "cameraDistance.extremeCloseUp"
    },
    language
)

fun lensTypeLabel(value: LensType, language: Language): String = uiString(
    when (value) {
        LensType.ULTRA_WIDE -> "lensType.ultraWide"
        LensType.WIDE -> "lensType.wide"
        LensType.STANDARD -> "lensType.standard"
        LensType.PORTRAIT -> "lensType.portrait"
        LensType.TELEPHOTO -> "lensType.telephoto"
    },
    language
)

fun depthOfFieldLabel(value: DepthOfField, language: Language): String = uiString(
    when (value) {
        DepthOfField.SHALLOW -> "depthOfField.shallow"
        DepthOfField.MEDIUM -> "depthOfField.medium"
        DepthOfField.DEEP -> "depthOfField.deep"
    },
    language
)

fun focusModeLabel(value: FocusMode, language: Language): String = uiString(
    when (value) {
        FocusMode.AUTO -> "focusMode.auto"
        FocusMode.MANUAL -> "focusMode.manual"
        FocusMode.SUBJECT_TRACKING -> "focusMode.subjectTracking"
        FocusMode.RACK_FOCUS -> "focusMode.rackFocus"
    },
    language
)

fun stabilizationLabel(value: Stabilization, language: Language): String = uiString(
    when (value) {
        Stabilization.TRIPOD -> "stabilization.tripod"
        Stabilization.GIMBAL -> "stabilization.gimbal"
        Stabilization.HANDHELD -> "stabilization.handheld"
    },
    language
)

fun framingLabel(value: Framing, language: Language): String = uiString(
    when (value) {
        Framing.RULE_OF_THIRDS -> "framing.ruleOfThirds"
        Framing.CENTERED -> "framing.centered"
        Framing.SYMMETRICAL -> "framing.symmetrical"
        Framing.ASYMMETRICAL -> "framing.asymmetrical"
    },
    language
)

fun basicMovementTypeLabel(value: BasicMovementType, language: Language): String = uiString(
    when (value) {
        BasicMovementType.STATIC -> "basicMovementType.static"
        BasicMovementType.PAN_LEFT -> "basicMovementType.panLeft"
        BasicMovementType.PAN_RIGHT -> "basicMovementType.panRight"
        BasicMovementType.TILT_UP -> "basicMovementType.tiltUp"
        BasicMovementType.TILT_DOWN -> "basicMovementType.tiltDown"
        BasicMovementType.DOLLY_IN -> "basicMovementType.dollyIn"
        BasicMovementType.DOLLY_OUT -> "basicMovementType.dollyOut"
        BasicMovementType.TRACKING -> "basicMovementType.tracking"
        BasicMovementType.CRANE -> "basicMovementType.crane"
        BasicMovementType.HANDHELD -> "basicMovementType.handheld"
    },
    language
)

fun advancedMovementTypeLabel(value: AdvancedMovementType, language: Language): String = uiString(
    when (value) {
        AdvancedMovementType.ORBIT -> "advancedMovementType.orbit"
        AdvancedMovementType.DRONE_PATH -> "advancedMovementType.dronePath"
        AdvancedMovementType.DOLLY_ZOOM -> "advancedMovementType.dollyZoom"
        AdvancedMovementType.HANDHELD_SHAKE -> "advancedMovementType.handheldShake"
        AdvancedMovementType.COMPOUND -> "advancedMovementType.compound"
    },
    language
)
