package com.operaboys.cinemashotgenerator.domain.scene

import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// رفع بدهی فنی مستند در docs/adr/064-g14-orphaned-rules-decisions.md (تصمیم ۳) و
// docs/adr/066-...: lockScene تا این قدم هیچ تست مستقلی نداشت (فقط غیرمستقیم از
// طریق SceneDetailViewModel/UI). این فایل عیناً هم‌الگو با
// ProjectLifecycleTest.kt (archiveProject) است — هر مسیر مجاز/غیرمجاز
// State Machine برای Scene، به‌علاوه‌ی تأیید صریح حفظ پیام سفارشی بعد از
// Refactor به validateStateTransition.

class SceneLifecycleTest {

    private fun sampleScene(state: EntityState) = Scene(
        sceneId = "scene_001",
        sceneNumber = 1,
        narrativeRole = NarrativeRole.CLIMAX,
        location = SceneLocation(LocationType.OUTDOOR, "خیابان شلوغ شهری، شب"),
        timeOfDay = TimeOfDay.NIGHT,
        atmospherePrimary = Atmosphere.TENSE,
        state = state
    )

    @Test
    fun `lockScene succeeds from DRAFT, per ALLOWED_TRANSITIONS`() {
        val result = lockScene(sampleScene(EntityState.DRAFT))

        assertTrue(result.isSuccess)
        assertEquals(EntityState.LOCKED, result.getOrThrow().state)
    }

    @Test
    fun `lockScene succeeds from REVIEW too`() {
        val result = lockScene(sampleScene(EntityState.REVIEW))

        assertTrue(result.isSuccess)
        assertEquals(EntityState.LOCKED, result.getOrThrow().state)
    }

    @Test
    fun `lockScene fails for an already-LOCKED scene`() {
        assertTrue(lockScene(sampleScene(EntityState.LOCKED)).isFailure)
    }

    @Test
    fun `lockScene fails for FINAL and ARCHIVED scenes`() {
        assertTrue(lockScene(sampleScene(EntityState.FINAL)).isFailure)
        assertTrue(lockScene(sampleScene(EntityState.ARCHIVED)).isFailure)
    }

    @Test
    fun `lockScene failure keeps the exact custom message, not the generic validateStateTransition wording`() {
        val result = lockScene(sampleScene(EntityState.FINAL))

        assertEquals(
            "صحنه در وضعیت FINAL است — طبق قوانین State Machine واحد ۱۲، این انتقال به Locked مجاز نیست",
            result.exceptionOrNull()?.message
        )
    }
}
