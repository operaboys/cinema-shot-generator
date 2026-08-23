package com.operaboys.cinemashotgenerator.domain.asset

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetValidationTest {

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

    // --- Rule 10: ObjectAsset.size/materialAndColor الزامی؛ specialTrait اختیاری ---

    private fun sampleObject(size: String = "small", materialAndColor: String = "worn black metal") = ObjectAsset(
        assetId = "obj_001",
        name = "Service Pistol",
        description = "a worn revolver",
        subtype = ObjectSubtype.PERSONAL_PROP,
        size = size,
        materialAndColor = materialAndColor
    )

    @Test
    fun `rule10 blank size is blocking`() {
        val issues = validateObjectAsset(sampleObject(size = "  "))
        assertEquals(1, issues.size)
        assertEquals(Severity.BLOCKING, issues[0].severity)
    }

    @Test
    fun `rule10 blank materialAndColor is blocking`() {
        val issues = validateObjectAsset(sampleObject(materialAndColor = ""))
        assertEquals(1, issues.size)
        assertEquals(Severity.BLOCKING, issues[0].severity)
    }

    @Test
    fun `rule10 both size and materialAndColor blank produce two blocking issues`() {
        val issues = validateObjectAsset(sampleObject(size = "", materialAndColor = "   "))
        assertEquals(2, issues.size)
        assertTrue(issues.all { it.severity == Severity.BLOCKING })
    }

    @Test
    fun `rule10 specialTrait has no rule and does not affect validity`() {
        val issues = validateObjectAsset(sampleObject())
        assertTrue(issues.isEmpty())
    }

    // --- Rule 11: basePrompt (هر سه نوع Asset) نباید whitespace-only باشد ---

    @Test
    fun `rule11 null basePrompt is valid (optional field)`() {
        assertNull(validateBasePrompt(null))
    }

    @Test
    fun `rule11 empty basePrompt is valid (optional field)`() {
        assertNull(validateBasePrompt(""))
    }

    @Test
    fun `rule11 whitespace-only basePrompt warns`() {
        val result = validateBasePrompt("   ")
        assertEquals(Severity.WARNING, result!!.severity)
    }

    @Test
    fun `rule11 non-blank basePrompt is valid`() {
        assertNull(validateBasePrompt("a lone figure under a flickering streetlamp"))
    }

    // --- Rule 12: ObjectAsset.subtype باید یکی از سه مقدار معتبر ObjectSubtype باشد ---
    // بدون تابع Runtime جداگانه — این تست همان تضمین Type System را اثبات می‌کند
    // (نمی‌توان یک ObjectAsset با subtype نامعتبر ساخت، پس چیزی برای رد کردن نیست).

    @Test
    fun `rule12 every ObjectSubtype value constructs a valid ObjectAsset`() {
        ObjectSubtype.values().forEach { subtype ->
            val asset = sampleObject().copy(subtype = subtype)
            assertEquals(subtype, asset.subtype)
        }
    }
}
