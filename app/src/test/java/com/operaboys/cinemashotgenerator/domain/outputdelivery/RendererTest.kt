package com.operaboys.cinemashotgenerator.domain.outputdelivery

import com.operaboys.cinemashotgenerator.domain.promptengine.PromptBlueprint
import com.operaboys.cinemashotgenerator.domain.promptengine.StructuredParts
import com.operaboys.cinemashotgenerator.domain.shot.ImageReference
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RendererTest {

    private fun structuredParts(
        timelineBeats: String? = null,
        audioDescription: String? = null,
        environmentSpecs: String? = "rainy street"
    ) = StructuredParts(
        subjectDescription = "a detective",
        sceneContext = "tense atmosphere",
        shotDescription = "walks into the office",
        cameraSpecs = "eye level, medium shot",
        lightingSpecs = "dramatic lighting",
        environmentSpecs = environmentSpecs,
        styleModifiers = "cinematic style",
        timelineBeats = timelineBeats,
        audioDescription = audioDescription
    )

    private fun blueprint(
        parts: StructuredParts = structuredParts(),
        weightedEmphasis: Map<String, Float> = emptyMap(),
        imageReferences: List<ImageReference> = emptyList(),
        negativePrompt: String = ""
    ) = PromptBlueprint(
        promptBlueprintId = "prompt_001",
        shotId = "shot_001",
        structuredParts = parts,
        imageReferences = imageReferences,
        weightedEmphasis = weightedEmphasis,
        seed = null,
        conflictsResolved = 0,
        warnings = emptyList(),
        negativePrompt = negativePrompt
    )

    private fun sampleImageReference(id: String = "ref_1") =
        ImageReference(type = "character", localFilePath = "/storage/$id.jpg", description = "front-facing reference")

    private val videoCapableProfile = ModelProfile(
        profileId = "veo_3_1",
        platform = "veo",
        capabilities = ModelCapabilities(
            supportsVideo = true, supportsImage = false,
            supportsWeightedTags = false, supportsImagePrompt = false, supportsNegativePrompt = false
        ),
        constraints = ModelConstraints(maxPromptLength = 2000, maxTokens = 500),
        format = ModelFormat(type = "json", structure = "paragraph")
    )

    private val noVideoProfile = videoCapableProfile.copy(
        capabilities = videoCapableProfile.capabilities.copy(supportsVideo = false)
    )

    private val imagePromptProfile = videoCapableProfile.copy(
        capabilities = videoCapableProfile.capabilities.copy(supportsImagePrompt = true)
    )

    private val negativePromptProfile = videoCapableProfile.copy(
        capabilities = videoCapableProfile.capabilities.copy(supportsNegativePrompt = true)
    )

    // --- renderBlueprintToText ---

    @Test
    fun `renderBlueprintToText includes timeline and audio when the profile supports video`() {
        val bp = blueprint(structuredParts(timelineBeats = "walks at 1.5s", audioDescription = "rain sound"))
        val text = renderBlueprintToText(bp, videoCapableProfile)
        assertTrue(text.contains("Timeline: walks at 1.5s"))
        assertTrue(text.contains("Audio: rain sound"))
    }

    @Test
    fun `renderBlueprintToText excludes timeline and audio when the profile does not support video`() {
        val bp = blueprint(structuredParts(timelineBeats = "walks at 1.5s", audioDescription = "rain sound"))
        val text = renderBlueprintToText(bp, noVideoProfile)
        assertFalse(text.contains("Timeline:"))
        assertFalse(text.contains("Audio:"))
    }

    // --- optimizeForProfile ---

    @Test
    fun `optimizeForProfile leaves short text untouched`() {
        val bp = blueprint()
        val optimized = optimizeForProfile(bp, videoCapableProfile)
        assertFalse(optimized.endsWith("..."))
    }

    @Test
    fun `optimizeForProfile truncates text longer than maxPromptLength`() {
        val longDescription = "a".repeat(50)
        val bp = blueprint(structuredParts(environmentSpecs = longDescription))
        val tightProfile = videoCapableProfile.copy(constraints = ModelConstraints(maxPromptLength = 10, maxTokens = 500))
        val optimized = optimizeForProfile(bp, tightProfile)
        assertEquals(10, optimized.length)
        assertTrue(optimized.endsWith("..."))
    }

    // --- validatePromptLength ---

    @Test
    fun `validatePromptLength is warning when text exceeds the limit`() {
        val tightProfile = videoCapableProfile.copy(constraints = ModelConstraints(maxPromptLength = 5, maxTokens = 500))
        val issue = validatePromptLength("a much longer text than five chars", tightProfile)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `validatePromptLength is null when text is within the limit`() {
        assertNull(validatePromptLength("short", videoCapableProfile))
    }

    // --- validateUnsupportedFeatureUsage ---

    @Test
    fun `validateUnsupportedFeatureUsage warns when video-only content is dropped`() {
        val bp = blueprint(structuredParts(timelineBeats = "walks at 1.5s"))
        val issue = validateUnsupportedFeatureUsage(bp, noVideoProfile)
        assertEquals(Severity.WARNING, issue!!.severity)
    }

    @Test
    fun `validateUnsupportedFeatureUsage is null when the profile supports video`() {
        val bp = blueprint(structuredParts(timelineBeats = "walks at 1.5s"))
        assertNull(validateUnsupportedFeatureUsage(bp, videoCapableProfile))
    }

    // --- render() با format.type == "json" — رفع محدودیت شناخته‌شده‌ی ADR-021 (ADR-025) ---

    @Test
    fun `render with a json profile produces real, parseable JSON with the key structured fields`() {
        val bp = blueprint(structuredParts(timelineBeats = "walks at 1.5s", audioDescription = "rain sound"))
        val rendered = render(bp, videoCapableProfile)

        val parsed = Json.parseToJsonElement(rendered.formattedPrompt).jsonObject
        assertEquals("a detective", parsed.getValue("subject").jsonPrimitive.content)
        assertEquals("tense atmosphere", parsed.getValue("scene").jsonPrimitive.content)
        assertEquals("walks into the office", parsed.getValue("shot").jsonPrimitive.content)
        assertEquals("eye level, medium shot", parsed.getValue("camera").jsonPrimitive.content)
        assertEquals("dramatic lighting", parsed.getValue("lighting").jsonPrimitive.content)
        assertEquals("rainy street", parsed.getValue("environment").jsonPrimitive.content)
        assertEquals("cinematic style", parsed.getValue("style").jsonPrimitive.content)
        assertEquals("walks at 1.5s", parsed.getValue("timeline").jsonPrimitive.content)
        assertEquals("rain sound", parsed.getValue("audio").jsonPrimitive.content)
    }

    @Test
    fun `render with a json profile omits timeline and audio when the profile does not support video`() {
        val bp = blueprint(structuredParts(timelineBeats = "walks at 1.5s", audioDescription = "rain sound"))
        val rendered = render(bp, noVideoProfile)

        val parsed = Json.parseToJsonElement(rendered.formattedPrompt).jsonObject
        assertFalse(parsed.containsKey("timeline"))
        assertFalse(parsed.containsKey("audio"))
    }

    @Test
    fun `render with a json profile that does not support weighted tags never includes a weightedEmphasis object`() {
        val bp = blueprint(weightedEmphasis = mapOf("cinematic lighting" to 1.2f))
        val rendered = render(bp, videoCapableProfile) // videoCapableProfile.supportsWeightedTags == false

        val parsed = Json.parseToJsonElement(rendered.formattedPrompt).jsonObject
        assertFalse(parsed.containsKey("weightedEmphasis"))
    }

    @Test
    fun `render with a json profile that supports weighted tags includes weightedEmphasis as a separate JSON object`() {
        // در حال حاضر هیچ پروفایل واقعی این ترکیب (json + supportsWeightedTags) را
        // ندارد (تأیید در ADR-025) — این یک پروفایل فرضی برای پوشش این مسیر آینده است.
        val jsonWeightedProfile = videoCapableProfile.copy(
            capabilities = videoCapableProfile.capabilities.copy(supportsWeightedTags = true)
        )
        val bp = blueprint(weightedEmphasis = mapOf("cinematic lighting" to 1.2f))
        val rendered = render(bp, jsonWeightedProfile)

        val parsed = Json.parseToJsonElement(rendered.formattedPrompt).jsonObject
        assertTrue(parsed.containsKey("weightedEmphasis"))
        val weights = parsed.getValue("weightedEmphasis").jsonObject
        assertEquals(1.2f, weights.getValue("cinematic lighting").jsonPrimitive.content.toFloat())

        // فیلدهای متنی اصلی نباید با نحو دستکاری‌شده‌ی رشته‌ای آلوده شده باشند
        assertEquals("cinematic style", parsed.getValue("style").jsonPrimitive.content)
    }

    // --- applyWeightSyntax با platform="stable_diffusion" ---

    @Test
    fun `applyWeightSyntax applies the (tag weight) syntax for stable_diffusion`() {
        val sdProfile = videoCapableProfile.copy(platform = "stable_diffusion")
        val result = applyWeightSyntax("a scene with cinematic lighting", "cinematic lighting", 1.2f, sdProfile)
        assertEquals("a scene with (cinematic lighting:1.2)", result)
    }

    @Test
    fun `applyWeightSyntax still applies the midjourney syntax unchanged`() {
        val mjProfile = videoCapableProfile.copy(platform = "midjourney")
        val result = applyWeightSyntax("a scene with cinematic lighting", "cinematic lighting", 1.2f, mjProfile)
        assertEquals("a scene with cinematic lighting::1.2", result)
    }

    @Test
    fun `applyWeightSyntax leaves text untouched for a platform with no known weight syntax`() {
        val otherProfile = videoCapableProfile.copy(platform = "some_other_platform")
        val result = applyWeightSyntax("a scene with cinematic lighting", "cinematic lighting", 1.2f, otherProfile)
        assertEquals("a scene with cinematic lighting", result)
    }

    // --- رگرسیون: universal_default و پروفایل‌های غیر-JSON/غیر-SD بدون تغییر رفتار ---

    @Test
    fun `render with universal_default plain_text profile is unaffected by the json and stable_diffusion changes`() {
        val bp = blueprint(structuredParts(timelineBeats = "walks at 1.5s", audioDescription = "rain sound"))
        val rendered = render(bp, universalDefaultProfile)

        val expectedText = optimizeForProfile(bp, universalDefaultProfile)
        assertEquals(expectedText, rendered.formattedPrompt)
        // نباید JSON معتبر باشد — همچنان متن ساده است
        assertFalse(rendered.formattedPrompt.trim().startsWith("{"))
    }

    @Test
    fun `render with a command_string profile is unaffected by the json and stable_diffusion changes`() {
        val bp = blueprint()
        val commandProfile = videoCapableProfile.copy(
            format = ModelFormat(type = "command_string", commandPrefix = "/imagine prompt:")
        )
        val rendered = render(bp, commandProfile)
        val expectedText = optimizeForProfile(bp, commandProfile)
        assertEquals("/imagine prompt: $expectedText", rendered.formattedPrompt)
    }

    // --- تصمیم Truncation برای JSON (ADR-026): بدون کوتاه‌سازی مخرب، فقط هشدار ---

    @Test
    fun `render with a json profile never truncates the output even for very long structured parts`() {
        val longSubject = "a detective ".repeat(200)
        val bp = blueprint(structuredParts(environmentSpecs = longSubject))
        val tightProfile = videoCapableProfile.copy(constraints = ModelConstraints(maxPromptLength = 10, maxTokens = 500))

        val rendered = render(bp, tightProfile)

        // با وجود maxPromptLength=10، JSON باید کامل و معتبر بماند — نه بریده‌شده
        val parsed = Json.parseToJsonElement(rendered.formattedPrompt).jsonObject
        assertEquals(longSubject, parsed.getValue("environment").jsonPrimitive.content)
        assertTrue(rendered.formattedPrompt.length > tightProfile.constraints.maxPromptLength)
    }

    @Test
    fun `validatePromptLength still warns correctly for a long json-rendered output`() {
        val longSubject = "a detective ".repeat(200)
        val bp = blueprint(structuredParts(environmentSpecs = longSubject))
        val tightProfile = videoCapableProfile.copy(constraints = ModelConstraints(maxPromptLength = 10, maxTokens = 500))

        val rendered = render(bp, tightProfile)
        val issue = validatePromptLength(rendered.formattedPrompt, tightProfile)

        assertEquals(Severity.WARNING, issue!!.severity)
    }

    // --- buildReferenceImageInstruction (docs/adr/030-unit14-reference-image-and-negative-prompt-migration.md) ---

    @Test
    fun `buildReferenceImageInstruction returns null when imageReferences is empty`() {
        assertNull(buildReferenceImageInstruction(emptyList(), imagePromptProfile))
    }

    @Test
    fun `buildReferenceImageInstruction returns null when the profile does not support image prompts`() {
        assertNull(buildReferenceImageInstruction(listOf(sampleImageReference()), videoCapableProfile))
    }

    @Test
    fun `buildReferenceImageInstruction returns singular phrasing for exactly one reference image`() {
        val instruction = buildReferenceImageInstruction(listOf(sampleImageReference()), imagePromptProfile)
        assertEquals("use the attached reference image for this subject's appearance", instruction)
    }

    @Test
    fun `buildReferenceImageInstruction returns plural phrasing for multiple reference images`() {
        val instruction = buildReferenceImageInstruction(
            listOf(sampleImageReference("ref_1"), sampleImageReference("ref_2")),
            imagePromptProfile
        )
        assertEquals("use the attached reference images for these subjects' appearances", instruction)
    }

    @Test
    fun `buildReferenceImageInstruction never includes the file path or file name`() {
        val instruction = buildReferenceImageInstruction(listOf(sampleImageReference("ref_1")), imagePromptProfile)
        assertFalse(instruction!!.contains("ref_1"))
        assertFalse(instruction.contains(".jpg"))
    }

    // --- renderBlueprintToText: تزریق دستور عکس رفرنس در جایگاه دوم segments ---

    @Test
    fun `renderBlueprintToText inserts the reference image instruction near the start, not at the end`() {
        val bp = blueprint(imageReferences = listOf(sampleImageReference()))
        val text = renderBlueprintToText(bp, imagePromptProfile)

        // مقایسه‌ی صریح قبل/بعد طبق الزام دستور کار: قبل از این Migration، هیچ سناریوی
        // imageReferences غیرخالی در RendererTest.kt پوشش داده نشده بود (blueprint()
        // همیشه emptyList داشت) — پس این یک تست کاملاً جدید است، نه اصلاح یک انتظار
        // قدیمی. متن اکنون باید بلافاصله بعد از subjectDescription این جمله را داشته باشد.
        val expectedTextWithoutInstruction = renderBlueprintToText(bp.copy(imageReferences = emptyList()), imagePromptProfile)
        assertTrue(text.contains("use the attached reference image for this subject's appearance"))
        assertTrue(text.startsWith("a detective. use the attached reference image for this subject's appearance"))
        assertFalse(expectedTextWithoutInstruction.contains("use the attached reference image"))
    }

    @Test
    fun `renderBlueprintToText omits the reference image instruction when the profile does not support it`() {
        val bp = blueprint(imageReferences = listOf(sampleImageReference()))
        val text = renderBlueprintToText(bp, videoCapableProfile)
        assertFalse(text.contains("use the attached reference image"))
    }

    // --- negativePrompt (تکمیل ADR-028): فقط وقتی مدل پشتیبانی می‌کند اعمال می‌شود ---

    @Test
    fun `renderBlueprintToText includes negative prompt when the profile supports it`() {
        val bp = blueprint(negativePrompt = "blurry, low quality")
        val text = renderBlueprintToText(bp, negativePromptProfile)
        assertTrue(text.contains("Negative prompt: blurry, low quality"))
    }

    @Test
    fun `renderBlueprintToText silently omits negative prompt when the profile does not support it`() {
        val bp = blueprint(negativePrompt = "blurry, low quality")
        val text = renderBlueprintToText(bp, videoCapableProfile)
        assertFalse(text.contains("Negative prompt"))
    }

    @Test
    fun `renderBlueprintToText omits negative prompt segment when it is blank, even if the profile supports it`() {
        val bp = blueprint(negativePrompt = "")
        val text = renderBlueprintToText(bp, negativePromptProfile)
        assertFalse(text.contains("Negative prompt"))
    }

    @Test
    fun `render with a json profile includes negativePrompt when the profile supports it`() {
        val bp = blueprint(negativePrompt = "blurry, low quality")
        val jsonNegativeProfile = negativePromptProfile.copy(format = ModelFormat(type = "json", structure = "paragraph"))
        val rendered = render(bp, jsonNegativeProfile)

        val parsed = Json.parseToJsonElement(rendered.formattedPrompt).jsonObject
        assertEquals("blurry, low quality", parsed.getValue("negativePrompt").jsonPrimitive.content)
    }

    @Test
    fun `render with a json profile omits negativePrompt when the profile does not support it`() {
        val bp = blueprint(negativePrompt = "blurry, low quality")
        val rendered = render(bp, videoCapableProfile) // videoCapableProfile.supportsNegativePrompt == false
        val parsed = Json.parseToJsonElement(rendered.formattedPrompt).jsonObject
        assertFalse(parsed.containsKey("negativePrompt"))
    }
}
