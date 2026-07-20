package com.operaboys.cinemashotgenerator.domain.stateversioning

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LockMechanismTest {

    private val entity = Entity(id = "shot_001", type = "shot")

    // --- setLock ---

    @Test
    fun `setLock locks the entity and stamps lockedAt`() {
        val locked = setLock(entity, locked = true, reason = "در حال بازبینی", clock = { "2026-07-20T10:00:00Z" })
        assertTrue(locked.lock.locked)
        assertEquals("2026-07-20T10:00:00Z", locked.lock.lockedAt)
        assertEquals("در حال بازبینی", locked.lock.lockReason)
    }

    @Test
    fun `setLock unlocking has no lockedAt`() {
        val unlocked = setLock(entity, locked = false)
        assertFalse(unlocked.lock.locked)
        assertNull(unlocked.lock.lockedAt)
    }

    // --- unlockWithOverride ---

    @Test
    fun `unlockWithOverride clears the lock`() {
        val lockedEntity = entity.copy(lock = EntityLock(locked = true, lockedAt = "2026-07-20T10:00:00Z"))
        val result = unlockWithOverride(lockedEntity)
        assertFalse(result.lock.locked)
    }

    @Test
    fun `unlockWithOverride logs a lock_override event with the entity id`() {
        var loggedEventType: String? = null
        var loggedEntityId: String? = null
        val logger = StateVersioningEventLogger { eventType, entityId ->
            loggedEventType = eventType
            loggedEntityId = entityId
        }

        unlockWithOverride(entity, logger = logger)

        assertEquals("lock_override", loggedEventType)
        assertEquals("shot_001", loggedEntityId)
    }

    // --- validateEditPermission ---

    @Test
    fun `validateEditPermission is blocking for locked state without override`() {
        val issue = validateEditPermission(EntityState.LOCKED, hasOverride = false)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `validateEditPermission is blocking for final state without override`() {
        val issue = validateEditPermission(EntityState.FINAL, hasOverride = false)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    @Test
    fun `validateEditPermission is null for locked state with override`() {
        assertNull(validateEditPermission(EntityState.LOCKED, hasOverride = true))
    }

    @Test
    fun `validateEditPermission is null for draft state without override`() {
        assertNull(validateEditPermission(EntityState.DRAFT, hasOverride = false))
    }
}
