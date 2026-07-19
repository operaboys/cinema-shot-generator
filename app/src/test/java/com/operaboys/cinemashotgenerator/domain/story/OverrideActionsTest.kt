package com.operaboys.cinemashotgenerator.domain.story

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverrideActionsTest {

    private class RecordingLogger : OverrideEventLogger {
        val events = mutableListOf<OverrideEvent>()
        override fun log(event: OverrideEvent) {
            events.add(event)
        }
    }

    private val scope = OverrideScope(
        entityType = "shot",
        entityId = "shot_001",
        field = "camera.lens_type",
        originalValue = "portrait",
        overrideValue = "ultra_wide"
    )

    // --- checkOverridePermission: مدل دو سطحی ---

    @Test
    fun `blocking severity is always denied`() {
        val permission = checkOverridePermission(RuleSeverity.BLOCKING)

        assertTrue(permission is OverridePermission.Denied)
    }

    @Test
    fun `warning severity is always allowed`() {
        val permission = checkOverridePermission(RuleSeverity.WARNING)

        assertTrue(permission is OverridePermission.Allowed)
    }

    @Test
    fun `every severity maps to exactly denied or allowed`() {
        // مدل دو سطحی: هیچ حالت سومی وجود ندارد
        RuleSeverity.values().forEach { severity ->
            val permission = checkOverridePermission(severity)
            when (severity) {
                RuleSeverity.BLOCKING -> assertTrue(permission is OverridePermission.Denied)
                RuleSeverity.WARNING -> assertTrue(permission is OverridePermission.Allowed)
            }
        }
    }

    // --- createOverride ---

    @Test
    fun `createOverride on blocking rule fails and logs nothing`() {
        val logger = RecordingLogger()

        val result = createOverride(
            overrideType = OverrideType.ARTISTIC,
            scope = scope,
            targetRuleSeverity = RuleSeverity.BLOCKING,
            logger = logger
        )

        assertTrue(result.isFailure)
        assertTrue(logger.events.isEmpty())
    }

    @Test
    fun `createOverride on warning rule succeeds with automatic scope and event`() {
        val logger = RecordingLogger()

        val result = createOverride(
            overrideType = OverrideType.ARTISTIC,
            scope = scope,
            targetRuleSeverity = RuleSeverity.WARNING,
            reason = null,
            idProvider = { "ovr_test00000001" },
            clock = { "2026-01-26T10:30:00Z" },
            logger = logger
        )

        val override = result.getOrThrow()
        assertEquals("ovr_test00000001", override.overrideId)
        assertEquals(OverrideType.ARTISTIC, override.overrideType)
        assertEquals("2026-01-26T10:30:00Z", override.createdAt)
        assertEquals(scope, override.scope)
        assertNull(override.reason) // reason اختیاری است (Rule 2 بخش ب)
        assertTrue(override.active)
        assertFalse(override.revoked)

        assertEquals(1, logger.events.size)
        assertEquals("override_created", logger.events[0].type)
        assertEquals("ovr_test00000001", logger.events[0].overrideId)
        assertEquals(scope, logger.events[0].scope) // ثبت Scope خودکار و الزامی
    }

    @Test
    fun `createOverride default id follows ovr prefix convention`() {
        val result = createOverride(
            overrideType = OverrideType.TECHNICAL,
            scope = scope,
            targetRuleSeverity = RuleSeverity.WARNING
        )

        val override = result.getOrThrow()
        assertTrue(override.overrideId.matches(Regex("ovr_[0-9a-f]{12}")))
    }

    // --- revokeOverride ---

    @Test
    fun `revokeOverride returns revoked copy and logs event`() {
        val logger = RecordingLogger()
        val original = createOverride(
            overrideType = OverrideType.NARRATIVE,
            scope = scope,
            targetRuleSeverity = RuleSeverity.WARNING,
            idProvider = { "ovr_test00000002" },
            clock = { "2026-01-26T10:30:00Z" }
        ).getOrThrow()

        val result = revokeOverride(
            override = original,
            reason = "دیگر لازم نیست",
            clock = { "2026-01-26T11:00:00Z" },
            logger = logger
        )

        val revoked = result.getOrThrow()
        assertFalse(revoked.active)
        assertTrue(revoked.revoked)
        assertEquals("2026-01-26T11:00:00Z", revoked.revokedAt)
        assertEquals("دیگر لازم نیست", revoked.revokedReason)
        // فیلدهای اصلی دست‌نخورده می‌مانند (Revoke، نه حذف — Rule 3 بخش ب)
        assertEquals(original.overrideId, revoked.overrideId)
        assertEquals(original.scope, revoked.scope)
        assertEquals(original.createdAt, revoked.createdAt)

        assertEquals(1, logger.events.size)
        assertEquals("override_revoked", logger.events[0].type)
        assertEquals("ovr_test00000002", logger.events[0].overrideId)
    }
}
