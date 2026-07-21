package com.operaboys.cinemashotgenerator.domain.outputdelivery

import com.operaboys.cinemashotgenerator.domain.promptengine.PromptBlueprint
import com.operaboys.cinemashotgenerator.domain.promptengine.StructuredParts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های واحد ۱۴ — پروفایل‌های ۱۳ مدل واقعی بازار (ModelProfiles.kt).
 * پوشش: selectProfile برای هر ۱۳ پلتفرم، رگرسیون fallback به universal_default،
 * validateProfileAvailability با فهرست کامل، و رندر بدون کرش روی دو فرمت متفاوت
 * (Veo=json در برابر Midjourney=command_string).
 */
class ModelProfilesTest {

    private fun blueprint() = PromptBlueprint(
        promptBlueprintId = "prompt_001",
        shotId = "shot_001",
        structuredParts = StructuredParts(
            subjectDescription = "a detective",
            sceneContext = "tense atmosphere",
            shotDescription = "walks into the office",
            cameraSpecs = "eye level, medium shot",
            lightingSpecs = "dramatic lighting",
            environmentSpecs = "rainy street",
            styleModifiers = "cinematic style",
            timelineBeats = "walks at 1.5s",
            audioDescription = "rain sound"
        ),
        imageReferences = emptyList(),
        weightedEmphasis = emptyMap(),
        seed = null,
        conflictsResolved = 0,
        warnings = emptyList()
    )

    // --- selectProfile: هر ۱۳ پلتفرم پروفایل درست خودش را برمی‌گرداند ---

    @Test
    fun `selectProfile returns veo profile for veo platform`() {
        assertEquals("veo_3_1", selectProfile("veo", ALL_MODEL_PROFILES).profileId)
    }

    @Test
    fun `selectProfile returns kling profile for kling platform`() {
        assertEquals("kling_3_0", selectProfile("kling", ALL_MODEL_PROFILES).profileId)
    }

    @Test
    fun `selectProfile returns seedance profile for seedance platform`() {
        assertEquals("seedance_2_5", selectProfile("seedance", ALL_MODEL_PROFILES).profileId)
    }

    @Test
    fun `selectProfile returns happyhorse profile for happyhorse platform`() {
        assertEquals("happyhorse_1_0", selectProfile("happyhorse", ALL_MODEL_PROFILES).profileId)
    }

    @Test
    fun `selectProfile returns runway profile for runway platform`() {
        assertEquals("runway_gen_4_5", selectProfile("runway", ALL_MODEL_PROFILES).profileId)
    }

    @Test
    fun `selectProfile returns luma profile for luma platform`() {
        assertEquals("luma_ray3_14", selectProfile("luma", ALL_MODEL_PROFILES).profileId)
    }

    @Test
    fun `selectProfile returns hailuo profile for hailuo platform`() {
        assertEquals("hailuo_2_3", selectProfile("hailuo", ALL_MODEL_PROFILES).profileId)
    }

    @Test
    fun `selectProfile returns midjourney profile for midjourney platform`() {
        assertEquals("midjourney_v7", selectProfile("midjourney", ALL_MODEL_PROFILES).profileId)
    }

    @Test
    fun `selectProfile returns wan profile for wan platform`() {
        assertEquals("wan_2_2", selectProfile("wan", ALL_MODEL_PROFILES).profileId)
    }

    @Test
    fun `selectProfile returns hunyuan profile for hunyuan platform`() {
        assertEquals("hunyuanvideo_1_5", selectProfile("hunyuan", ALL_MODEL_PROFILES).profileId)
    }

    @Test
    fun `selectProfile returns ltx profile for ltx platform`() {
        assertEquals("ltx_2_3", selectProfile("ltx", ALL_MODEL_PROFILES).profileId)
    }

    @Test
    fun `selectProfile returns vidu profile for vidu platform`() {
        assertEquals("vidu_q3", selectProfile("vidu", ALL_MODEL_PROFILES).profileId)
    }

    @Test
    fun `selectProfile returns stable diffusion profile for stable_diffusion platform`() {
        assertEquals("stable_diffusion_sd3", selectProfile("stable_diffusion", ALL_MODEL_PROFILES).profileId)
    }

    // --- رگرسیون: fallback به universal_default نباید بشکند ---

    @Test
    fun `selectProfile still falls back to universal_default for an unknown platform among all 13 profiles`() {
        val result = selectProfile("some_unreleased_future_model", ALL_MODEL_PROFILES)
        assertEquals("universal_default", result.profileId)
    }

    // --- validateProfileAvailability: با فهرست کامل، هر ۱۳ پلتفرم null (تطبیق دقیق موجود) ---

    @Test
    fun `validateProfileAvailability is null for all 13 platforms when the full profile list is available`() {
        val platforms = listOf(
            "veo", "kling", "seedance", "happyhorse", "runway", "luma", "hailuo",
            "midjourney", "wan", "hunyuan", "ltx", "vidu", "stable_diffusion"
        )
        platforms.forEach { platform ->
            assertNull(
                "expected null for platform=$platform",
                validateProfileAvailability(platform, ALL_MODEL_PROFILES)
            )
        }
    }

    // --- رندر بدون کرش روی دو فرمت متفاوت (Veo=json در برابر Midjourney=command_string) ---
    // به‌روزرسانی (ADR-025): محدودیت «json از همان مسیر plain متن عبور می‌کند» که در
    // ADR-021 مستند شده بود، در این قدم رفع شد — Renderer.render اکنون برای
    // "json" واقعاً یک JSON معتبر می‌سازد (نه فقط متن ساده با برچسب json). این تست
    // همچنان عدم کرش + commandPrefix میجرنی را بررسی می‌کند؛ تست‌های اختصاصی صحت
    // ساختاری JSON در RendererTest.kt اضافه شدند.

    @Test
    fun `renderBlueprintToText and render run without crashing across json and command_string formats`() {
        val bp = blueprint()

        val veoText = renderBlueprintToText(bp, veoProfile)
        val midjourneyText = renderBlueprintToText(bp, midjourneyProfile)
        assertTrue(veoText.isNotBlank())
        assertTrue(midjourneyText.isNotBlank())

        val veoRendered = render(bp, veoProfile)
        val midjourneyRendered = render(bp, midjourneyProfile)

        assertEquals("veo_3_1", veoRendered.modelProfileId)
        assertEquals("midjourney_v7", midjourneyRendered.modelProfileId)
        assertTrue(midjourneyRendered.formattedPrompt.startsWith("/imagine prompt:"))
    }
}
