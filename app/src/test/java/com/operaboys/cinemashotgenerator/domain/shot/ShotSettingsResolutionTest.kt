package com.operaboys.cinemashotgenerator.domain.shot

import com.operaboys.cinemashotgenerator.domain.camera.BasicMovementType
import com.operaboys.cinemashotgenerator.domain.camera.CameraAngle
import com.operaboys.cinemashotgenerator.domain.camera.CameraDistance
import com.operaboys.cinemashotgenerator.domain.camera.CameraMovement
import com.operaboys.cinemashotgenerator.domain.camera.CameraSettings
import com.operaboys.cinemashotgenerator.domain.camera.DepthOfField
import com.operaboys.cinemashotgenerator.domain.camera.Framing
import com.operaboys.cinemashotgenerator.domain.camera.FocusMode
import com.operaboys.cinemashotgenerator.domain.camera.LensType
import com.operaboys.cinemashotgenerator.domain.camera.Stabilization
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.sceneconditions.ContrastRatio
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.KeyLightPosition
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShotSettingsResolutionTest {

    private fun baseShot(
        camera: SourcedSettings<CameraSettings> = SourcedSettings(),
        lighting: SourcedSettings<LightingSettings> = SourcedSettings(),
        environment: SourcedSettings<EnvironmentSettings> = SourcedSettings()
    ) = Shot(
        shotId = "shot_001",
        sceneId = "scene_001",
        shotNumber = 1,
        shotDescription = "A detective walks into a dimly lit office",
        shotGoal = ShotGoal.ESTABLISHING,
        shotType = ShotType.WIDE,
        durationSeconds = 4f,
        motionLevel = MotionLevel.MODERATE,
        camera = camera,
        lighting = lighting,
        environment = environment,
        soundProfile = SoundProfile(enabled = true)
    )

    private val sceneCameraDefault = CameraSettings(
        angle = CameraAngle.EYE_LEVEL,
        distance = CameraDistance.WIDE,
        movement = CameraMovement.Basic(type = BasicMovementType.STATIC),
        lensType = LensType.STANDARD,
        depthOfField = DepthOfField.DEEP,
        focusMode = FocusMode.AUTO,
        stabilization = Stabilization.TRIPOD,
        framing = Framing.RULE_OF_THIRDS
    )

    private val shotCameraOverride = sceneCameraDefault.copy(angle = CameraAngle.LOW, distance = CameraDistance.CLOSE_UP)

    private val sceneLightingDefault = LightingSettings(
        style = LightingStyle.NATURAL_LIGHT,
        keyLightPosition = KeyLightPosition.FRONT,
        contrastRatio = ContrastRatio.LOW
    )

    private val shotLightingOverride = sceneLightingDefault.copy(style = LightingStyle.DRAMATIC_LIGHT)

    private val sceneEnvironmentDefault = EnvironmentSettings(weatherType = WeatherType.CLEAR)
    private val shotEnvironmentOverride = EnvironmentSettings(weatherType = WeatherType.RAIN)

    // --- resolveCameraSettings ---

    @Test
    fun `resolveCameraSettings uses scene default when source is scene`() {
        val shot = baseShot(camera = SourcedSettings(source = "scene", overrideValue = shotCameraOverride))
        val result = resolveCameraSettings(shot, sceneCameraDefault)
        assertEquals(sceneCameraDefault, result.getOrThrow())
    }

    @Test
    fun `resolveCameraSettings uses shot override when source is override`() {
        val shot = baseShot(camera = SourcedSettings(source = "override", overrideValue = shotCameraOverride))
        val result = resolveCameraSettings(shot, sceneCameraDefault)
        assertEquals(shotCameraOverride, result.getOrThrow())
    }

    @Test
    fun `resolveCameraSettings fails when neither scene default nor override exists`() {
        val shot = baseShot(camera = SourcedSettings(source = "scene", overrideValue = null))
        val result = resolveCameraSettings(shot, null)
        assertTrue(result.isFailure)
    }

    // --- resolveLightingSettings ---

    @Test
    fun `resolveLightingSettings uses scene default when source is scene`() {
        val shot = baseShot(lighting = SourcedSettings(source = "scene", overrideValue = shotLightingOverride))
        val result = resolveLightingSettings(shot, sceneLightingDefault)
        assertEquals(sceneLightingDefault, result.getOrThrow())
    }

    @Test
    fun `resolveLightingSettings uses shot override when source is override`() {
        val shot = baseShot(lighting = SourcedSettings(source = "override", overrideValue = shotLightingOverride))
        val result = resolveLightingSettings(shot, sceneLightingDefault)
        assertEquals(shotLightingOverride, result.getOrThrow())
    }

    @Test
    fun `resolveLightingSettings fails when neither scene default nor override exists`() {
        val shot = baseShot(lighting = SourcedSettings(source = "override", overrideValue = null))
        val result = resolveLightingSettings(shot, null)
        assertTrue(result.isFailure)
    }

    // --- resolveEnvironmentSettings ---

    @Test
    fun `resolveEnvironmentSettings uses scene default when source is scene`() {
        val shot = baseShot(environment = SourcedSettings(source = "scene", overrideValue = shotEnvironmentOverride))
        val result = resolveEnvironmentSettings(shot, sceneEnvironmentDefault)
        assertEquals(sceneEnvironmentDefault, result.getOrThrow())
    }

    @Test
    fun `resolveEnvironmentSettings uses shot override when source is override`() {
        val shot = baseShot(environment = SourcedSettings(source = "override", overrideValue = shotEnvironmentOverride))
        val result = resolveEnvironmentSettings(shot, sceneEnvironmentDefault)
        assertEquals(shotEnvironmentOverride, result.getOrThrow())
    }

    @Test
    fun `resolveEnvironmentSettings falls back to override when scene default is null`() {
        val shot = baseShot(environment = SourcedSettings(source = "override", overrideValue = shotEnvironmentOverride))
        val result = resolveEnvironmentSettings(shot, null)
        assertEquals(shotEnvironmentOverride, result.getOrThrow())
    }
}
