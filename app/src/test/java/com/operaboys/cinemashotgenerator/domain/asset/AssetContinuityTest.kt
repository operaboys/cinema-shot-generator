package com.operaboys.cinemashotgenerator.domain.asset

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

    // --- Rule 4 (Hard Lock، بدون استثنا) ---

    @Test
    fun `identity_lock blocks name change`() {
        val result = validateCharacterUpdate(lockedRules, "name")
        assertTrue(result is UpdateResult.Blocked)
    }

    @Test
    fun `identity_lock blocks asset_id change`() {
        val result = validateCharacterUpdate(lockedRules, "asset_id")
        assertTrue(result is UpdateResult.Blocked)
    }

    @Test
    fun `appearance_lock blocks physical_appearance change`() {
        val result = validateCharacterUpdate(lockedRules, "physical_appearance")
        assertTrue(result is UpdateResult.Blocked)
    }

    @Test
    fun `age_lock blocks age_range change`() {
        val result = validateCharacterUpdate(lockedRules, "age_range")
        assertTrue(result is UpdateResult.Blocked)
    }

    @Test
    fun `allowed override fields are allowed`() {
        listOf("emotion", "pose", "outfit", "expression", "prop").forEach { field ->
            val result = validateCharacterUpdate(lockedRules, field)
            assertTrue("field '$field' should be allowed", result is UpdateResult.Allowed)
        }
    }

    /**
     * Hard Lock هرگز نباید فقط هشدار بدهد. UpdateResult اصلاً نوع Warning ندارد —
     * یعنی این تضمین در سطح Type System برقرار است، نه فقط منطق زمان اجرا: هر سه
     * قفل فقط بین Blocked/Allowed انتخاب می‌کنند، جای سومی برای «هشدار» وجود ندارد.
     */
    @Test
    fun `hard lock fields never produce anything other than Blocked or Allowed`() {
        val lockedFields = listOf("name", "asset_id", "physical_appearance", "age_range")
        lockedFields.forEach { field ->
            val result = validateCharacterUpdate(lockedRules, field)
            assertTrue(result is UpdateResult.Blocked)
            // UpdateResult sealed class فقط Allowed/Blocked دارد؛ کامپایل‌شدن این when
            // بدون else شاخه‌ی سومی (Warning) اثبات می‌کند که چنین حالتی قابل بیان نیست.
            val exhaustiveCheck: String = when (result) {
                is UpdateResult.Allowed -> "allowed"
                is UpdateResult.Blocked -> "blocked"
            }
            assertTrue(exhaustiveCheck == "blocked")
        }
    }

    @Test
    fun `locks disabled still allow change when field not in allowedOverrides`() {
        // طبق کد مفهومی بلوپرینت: اگر قفل غیرفعال باشد و فیلد در allowedOverrides هم نباشد،
        // شاخه‌ی else نهایی همچنان Allowed برمی‌گرداند.
        val unlockedRules = lockedRules.copy(identityLock = false, appearanceLock = false, ageLock = false)
        val result = validateCharacterUpdate(unlockedRules, "some_other_field")
        assertTrue(result is UpdateResult.Allowed)
    }
}
