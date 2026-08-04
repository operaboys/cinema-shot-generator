package com.operaboys.cinemashotgenerator.ui.theme

import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

// واحد ۱۶ — فاز ۰ (تکمیل): تست سوییچ خودکار فونت بر اساس زبان
// (docs/adr/043-unit16-phase0-real-fonts.md).

class TypeTest {

    @Test
    fun `cinemaFontFamily returns Vazirmatn for Persian and Inter for English`() {
        assertEquals(VazirmatnFontFamily, cinemaFontFamily(Language.FA))
        assertEquals(InterFontFamily, cinemaFontFamily(Language.EN))
        assertNotEquals(cinemaFontFamily(Language.FA), cinemaFontFamily(Language.EN))
    }

    @Test
    fun `cinemaTypography actually changes fontFamily when the language changes`() {
        val faTypography = cinemaTypography(Language.FA)
        val enTypography = cinemaTypography(Language.EN)

        assertEquals(VazirmatnFontFamily, faTypography.bodyLarge.fontFamily)
        assertEquals(InterFontFamily, enTypography.bodyLarge.fontFamily)
        assertNotEquals(faTypography.bodyLarge.fontFamily, enTypography.bodyLarge.fontFamily)
    }

    @Test
    fun `every one of the 5 documented type scale roles uses the resolved fontFamily`() {
        val typography = cinemaTypography(Language.FA)

        listOf(
            typography.headlineMedium,
            typography.titleLarge,
            typography.titleMedium,
            typography.bodyLarge,
            typography.labelSmall
        ).forEach { style ->
            assertEquals(VazirmatnFontFamily, style.fontFamily)
        }
    }
}
