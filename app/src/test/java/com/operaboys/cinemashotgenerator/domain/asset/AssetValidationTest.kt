package com.operaboys.cinemashotgenerator.domain.asset

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetValidationTest {

    // --- validateImageFile ---

    @Test
    fun `validateImageFile accepts allowed formats`() {
        listOf("image/jpeg", "image/png", "image/webp").forEach { mime ->
            val result = validateImageFile("/tmp/ref.img", fileSizeBytes = 1024, mimeType = mime)
            assertTrue(result.valid)
        }
    }

    @Test
    fun `validateImageFile rejects disallowed format`() {
        val result = validateImageFile("/tmp/ref.gif", fileSizeBytes = 1024, mimeType = "image/gif")
        assertFalse(result.valid)
    }

    @Test
    fun `validateImageFile rejects file exceeding max size`() {
        val result = validateImageFile("/tmp/big.jpg", fileSizeBytes = 11L * 1024 * 1024, mimeType = "image/jpeg")
        assertFalse(result.valid)
    }

    @Test
    fun `validateImageFile rejects empty file`() {
        val result = validateImageFile("/tmp/empty.jpg", fileSizeBytes = 0L, mimeType = "image/jpeg")
        assertFalse(result.valid)
    }

    // --- Rule 1: یکتایی asset_id ---

    @Test
    fun `rule1 duplicate asset id is blocking`() {
        val result = validateAssetIdUniqueness("char_001", existingIds = listOf("char_001", "loc_001"))
        assertEquals(Severity.BLOCKING, result!!.severity)
    }

    @Test
    fun `rule1 unique asset id is valid`() {
        val result = validateAssetIdUniqueness("char_002", existingIds = listOf("char_001", "loc_001"))
        assertNull(result)
    }

    // --- Rule 3: Asset در حال استفاده قابل حذف نیست ---

    @Test
    fun `rule3 asset in use cannot be deleted`() {
        val result = validateAssetDeletion("char_001", shotsUsingAsset = listOf("shot_001", "shot_002"))
        assertEquals(Severity.BLOCKING, result!!.severity)
    }

    @Test
    fun `rule3 unused asset can be deleted`() {
        val result = validateAssetDeletion("char_001", shotsUsingAsset = emptyList())
        assertNull(result)
    }

    // --- Rule 5: حداقل یک Outfit پیش‌فرض ---

    @Test
    fun `rule5 missing default outfit is blocking`() {
        val outfits = listOf(
            Outfit(id = "o1", name = "A", description = "d", isDefault = false)
        )
        val result = validateDefaultOutfitExists(outfits)
        assertEquals(Severity.BLOCKING, result!!.severity)
    }

    @Test
    fun `rule5 with default outfit is valid`() {
        val outfits = listOf(
            Outfit(id = "o1", name = "A", description = "d", isDefault = true)
        )
        val result = validateDefaultOutfitExists(outfits)
        assertNull(result)
    }

    // --- Rule 6 + 6ب: وجود فایل + فرمت/سایز ---

    @Test
    fun `rule6 missing file is blocking`() {
        val result = validateReferenceImageFile(
            localFilePath = "/storage/missing.jpg",
            fileSizeBytes = 1024,
            mimeType = "image/jpeg",
            fileExists = { false }
        )
        assertEquals(Severity.BLOCKING, result!!.severity)
    }

    @Test
    fun `rule6b existing file with invalid format is blocking`() {
        val result = validateReferenceImageFile(
            localFilePath = "/storage/ref.gif",
            fileSizeBytes = 1024,
            mimeType = "image/gif",
            fileExists = { true }
        )
        assertEquals(Severity.BLOCKING, result!!.severity)
    }

    @Test
    fun `rule6 plus 6b valid existing image file is valid`() {
        val result = validateReferenceImageFile(
            localFilePath = "/storage/ref.jpg",
            fileSizeBytes = 1024,
            mimeType = "image/jpeg",
            fileExists = { true }
        )
        assertNull(result)
    }

    // --- Rule 7: نام مشابه (Warning) ---

    @Test
    fun `rule7 similar name warns`() {
        val result = checkSimilarAssetName("Detective Jon", existingNames = listOf("Detective John"))
        assertEquals(Severity.WARNING, result!!.severity)
    }

    @Test
    fun `rule7 case and whitespace only difference warns`() {
        val result = checkSimilarAssetName("  detective john  ", existingNames = listOf("Detective John"))
        assertEquals(Severity.WARNING, result!!.severity)
    }

    @Test
    fun `rule7 clearly different name is valid`() {
        val result = checkSimilarAssetName("Detective John", existingNames = listOf("Old Warehouse"))
        assertNull(result)
    }
}
