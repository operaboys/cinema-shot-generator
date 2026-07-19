package com.operaboys.cinemashotgenerator.domain.camera

// واحد ۰۹ — Camera System (بخش الف، ساختار داده)
// منبع حقیقت: docs/blueprints/09-camera-and-motion.md

enum class CameraAngle { EYE_LEVEL, LOW, HIGH, OVERHEAD, DUTCH, POV }
enum class CameraDistance { EXTREME_WIDE, WIDE, MEDIUM, CLOSE_UP, EXTREME_CLOSE_UP }
enum class LensType { ULTRA_WIDE, WIDE, STANDARD, PORTRAIT, TELEPHOTO }
enum class DepthOfField { SHALLOW, MEDIUM, DEEP }
enum class FocusMode { AUTO, MANUAL, SUBJECT_TRACKING, RACK_FOCUS }
enum class Stabilization { TRIPOD, GIMBAL, HANDHELD }
enum class Framing { RULE_OF_THIRDS, CENTERED, SYMMETRICAL, ASYMMETRICAL }

enum class BasicMovementType { STATIC, PAN_LEFT, PAN_RIGHT, TILT_UP, TILT_DOWN, DOLLY_IN, DOLLY_OUT, TRACKING, CRANE, HANDHELD }
enum class AdvancedMovementType { ORBIT, DRONE_PATH, DOLLY_ZOOM, HANDHELD_SHAKE, COMPOUND }

sealed class CameraMovement {
    data class Basic(val type: BasicMovementType, val speed: String = "medium") : CameraMovement()
    data class Orbit(val degrees: Int, val speed: String, val maintainEyeLevel: Boolean) : CameraMovement()
    data class DronePath(val altitudeChange: String, val pathType: String, val speed: String) : CameraMovement()
    data class DollyZoom(val focalStart: Int, val focalEnd: Int, val direction: String) : CameraMovement()
    data class HandheldShake(val intensity: Int, val frequency: String) : CameraMovement()
    data class Compound(val primary: String, val secondary: String, val sync: String) : CameraMovement()
}

data class CameraSettings(
    val angle: CameraAngle,
    val distance: CameraDistance,
    val movement: CameraMovement,
    val lensType: LensType,
    val depthOfField: DepthOfField,
    val focusMode: FocusMode,
    val stabilization: Stabilization,
    val framing: Framing
)
