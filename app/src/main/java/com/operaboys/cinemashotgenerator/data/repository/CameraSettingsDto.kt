package com.operaboys.cinemashotgenerator.data.repository

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// واحد ۱۵ — قدم ۲: DTO برای domain/camera/CameraModels.kt (CameraSettings + CameraMovement)
// دلیل DTO محلی (نه @Serializable مستقیم روی domain/): همان دلیل ShotDto.kt.
//
// NOTE: فیلد Basic.movementType (نه type) — چون kotlinx.serialization برای Sealed
// Class ها به‌طور پیش‌فرض یک ستون Discriminator با نام "type" اضافه می‌کند؛ اگر خودِ
// فیلد هم "type" نامیده شود، تداخل می‌کند و در Runtime با IllegalStateException شکست
// می‌خورد (کشف‌شده با تست واقعی، نه فرضی). جزئیات در
// docs/adr/018-unit15-step2-repository-deviations.md.

@Serializable
sealed class CameraMovementDto {
    @Serializable
    @SerialName("basic")
    data class Basic(val movementType: String, val speed: String = "medium") : CameraMovementDto()

    @Serializable
    @SerialName("orbit")
    data class Orbit(val degrees: Int, val speed: String, val maintainEyeLevel: Boolean) : CameraMovementDto()

    @Serializable
    @SerialName("dronePath")
    data class DronePath(val altitudeChange: String, val pathType: String, val speed: String) : CameraMovementDto()

    @Serializable
    @SerialName("dollyZoom")
    data class DollyZoom(val focalStart: Int, val focalEnd: Int, val direction: String) : CameraMovementDto()

    @Serializable
    @SerialName("handheldShake")
    data class HandheldShake(val intensity: Int, val frequency: String) : CameraMovementDto()

    @Serializable
    @SerialName("compound")
    data class Compound(val primary: String, val secondary: String, val sync: String) : CameraMovementDto()
}

@Serializable
data class CameraSettingsDto(
    val angle: String,
    val distance: String,
    val movement: CameraMovementDto,
    val lensType: String,
    val depthOfField: String,
    val focusMode: String,
    val stabilization: String,
    val framing: String,
    val movementDurationSeconds: Float? = null
)
