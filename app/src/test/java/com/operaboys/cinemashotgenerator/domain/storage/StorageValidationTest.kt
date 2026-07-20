package com.operaboys.cinemashotgenerator.domain.storage

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StorageValidationTest {

    // --- validateProjectId ---

    @Test
    fun `validateProjectId is null for a non-blank id`() {
        assertNull(validateProjectId("proj_001"))
    }

    @Test
    fun `validateProjectId is blocking for a blank id`() {
        val issue = validateProjectId("")
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    // --- validateStorageAvailable ---

    @Test
    fun `validateStorageAvailable is null when there is enough space`() {
        assertNull(validateStorageAvailable(hasEnoughSpace = true))
    }

    @Test
    fun `validateStorageAvailable is blocking when storage is full`() {
        val issue = validateStorageAvailable(hasEnoughSpace = false)
        assertEquals(Severity.BLOCKING, issue!!.severity)
    }

    // --- validateSchemaVersion ---

    @Test
    fun `validateSchemaVersion is null for a matching or newer version`() {
        assertNull(validateSchemaVersion(importedVersion = 1, currentVersion = 1))
        assertNull(validateSchemaVersion(importedVersion = 2, currentVersion = 1))
    }

    @Test
    fun `validateSchemaVersion warns for an older imported version`() {
        val issue = validateSchemaVersion(importedVersion = 1, currentVersion = 2)
        assertEquals(Severity.WARNING, issue!!.severity)
    }
}
