package com.operaboys.cinemashotgenerator.domain.asset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetContinuityTest {

    private val lockedRules = ContinuityRules(
        identityLock = true,
        appearanceLock = true,
        ageLock = true,
        antiDrift = true,
        allowedOverrides = listOf("emotion", "pose", "outfit", "expression", "prop")
    )

    // --- سطح FULL (Rule 4، Hard Lock بدون استثنا — رفتار/پیام‌های خطا باید بایت‌به‌بایت
    // با نسخه‌ی قبل از Migration بخش دوم یکسان بمانند) ---

    @Test
    fun `FULL identity_lock blocks name change`() {
        val result = validateCharacterUpdate(CharacterContinuityLevel.FULL, lockedRules, "name")
        assertTrue(result is UpdateResult.Blocked)
        assertEquals("این کاراکتر identity_lock دارد؛ نام و ID قابل تغییر نیستند", (result as UpdateResult.Blocked).reason)
    }

    @Test
    fun `FULL identity_lock blocks asset_id change`() {
        val result = validateCharacterUpdate(CharacterContinuityLevel.FULL, lockedRules, "asset_id")
        assertTrue(result is UpdateResult.Blocked)
    }

    @Test
    fun `FULL appearance_lock blocks physical_appearance change`() {
        val result = validateCharacterUpdate(CharacterContinuityLevel.FULL, lockedRules, "physical_appearance")
        assertTrue(result is UpdateResult.Blocked)
        assertEquals("این کاراکتر appearance_lock دارد؛ ظاهر پس از قفل‌شدن قابل تغییر نیست", (result as UpdateResult.Blocked).reason)
    }

    @Test
    fun `FULL age_lock blocks age_range change`() {
        val result = validateCharacterUpdate(CharacterContinuityLevel.FULL, lockedRules, "age_range")
        assertTrue(result is UpdateResult.Blocked)
        assertEquals("این کاراکتر age_lock دارد؛ سن قابل تغییر نیست", (result as UpdateResult.Blocked).reason)
    }

    @Test
    fun `FULL allowed override fields are allowed`() {
        listOf("emotion", "pose", "outfit", "expression", "prop").forEach { field ->
            val result = validateCharacterUpdate(CharacterContinuityLevel.FULL, lockedRules, field)
            assertTrue("field '$field' should be allowed", result is UpdateResult.Allowed)
        }
    }

    /**
     * سطح FULL هرگز نباید فقط هشدار بدهد، حتی پس از افزودن UpdateResult.Warned به
     * sealed class در این Migration — این تست همان تضمین قبلی را با شاخه‌ی Warned هم
     * حفظ می‌کند (اکنون Exhaustive روی هر سه حالت، نه فقط دو حالت قبلی).
     */
    @Test
    fun `FULL lock fields never produce Warned, only Blocked or Allowed`() {
        val lockedFields = listOf("name", "asset_id", "physical_appearance", "age_range")
        lockedFields.forEach { field ->
            val result = validateCharacterUpdate(CharacterContinuityLevel.FULL, lockedRules, field)
            assertTrue(result is UpdateResult.Blocked)
            val exhaustiveCheck: String = when (result) {
                is UpdateResult.Allowed -> "allowed"
                is UpdateResult.Blocked -> "blocked"
                is UpdateResult.Warned -> "warned"
            }
            assertTrue(exhaustiveCheck == "blocked")
        }
    }

    @Test
    fun `FULL locks disabled still allow change when field not in allowedOverrides`() {
        val unlockedRules = lockedRules.copy(identityLock = false, appearanceLock = false, ageLock = false)
        val result = validateCharacterUpdate(CharacterContinuityLevel.FULL, unlockedRules, "some_other_field")
        assertTrue(result is UpdateResult.Allowed)
    }

    // --- سطح MEDIUM (🆕 Rule 4ب/4پ) ---

    @Test
    fun `MEDIUM still blocks identity fields (name, asset_id)`() {
        listOf("name", "asset_id").forEach { field ->
            val result = validateCharacterUpdate(CharacterContinuityLevel.MEDIUM, lockedRules, field)
            assertTrue("field '$field' should be blocked", result is UpdateResult.Blocked)
        }
    }

    @Test
    fun `MEDIUM warns (does not block) on physical_appearance change`() {
        val result = validateCharacterUpdate(CharacterContinuityLevel.MEDIUM, lockedRules, "physical_appearance")
        assertTrue(result is UpdateResult.Warned)
    }

    @Test
    fun `MEDIUM allows fields other than identity or physical_appearance`() {
        val result = validateCharacterUpdate(CharacterContinuityLevel.MEDIUM, lockedRules, "age_range")
        assertTrue(result is UpdateResult.Allowed)
    }

    // --- سطح NONE (🆕 بدون هیچ محدودیتی) ---

    @Test
    fun `NONE always allows, regardless of the field being changed`() {
        listOf("name", "asset_id", "physical_appearance", "age_range", "anything_else").forEach { field ->
            val result = validateCharacterUpdate(CharacterContinuityLevel.NONE, lockedRules, field)
            assertTrue("field '$field' should be allowed at NONE level", result is UpdateResult.Allowed)
        }
    }

    // --- 🆕 validateLocationUpdate (Rule 8: سطح STYLE، همیشه فقط Warning) ---

    @Test
    fun `validateLocationUpdate warns when isStyleField is true`() {
        val result = validateLocationUpdate("visual_style", isStyleField = true)
        assertTrue(result is UpdateResult.Warned)
    }

    @Test
    fun `validateLocationUpdate allows when isStyleField is false`() {
        val result = validateLocationUpdate("name", isStyleField = false)
        assertTrue(result is UpdateResult.Allowed)
    }

    // --- 🆕 validatePropUpdate (Rule 9: سطح FORM، همیشه فقط Warning) ---

    @Test
    fun `validatePropUpdate warns when isFormField is true`() {
        val result = validatePropUpdate("size", isFormField = true)
        assertTrue(result is UpdateResult.Warned)
    }

    @Test
    fun `validatePropUpdate allows when isFormField is false`() {
        val result = validatePropUpdate("name", isFormField = false)
        assertTrue(result is UpdateResult.Allowed)
    }
}
