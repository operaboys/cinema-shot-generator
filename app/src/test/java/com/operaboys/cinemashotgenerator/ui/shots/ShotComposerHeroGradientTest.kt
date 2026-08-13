package com.operaboys.cinemashotgenerator.ui.shots

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

// یافته‌ی ۵ appendix ADR-081 (ADR-082): نوار Hero Composer قبلاً یک رنگ تخت
// (CinemaTheme.extendedColors.inset) بود، نه گرادیان دقیق mockup. این تست ثابت
// می‌کند مقدار واقعی رنگ‌های گرادیان دقیقاً همان ۳ رنگ mockup است
// (docs/design/Cinema Studio.html، `cs-shot-hero`:
// linear-gradient(150deg,#2C4260 0%,#3C5570 48%,#6E5B72 100%)) — یک تست واحد
// ساده (نه Compose UI Test) چون Brush/رنگ پس‌زمینه در درخت Semantics قابل‌بازرسی
// نیست.

class ShotComposerHeroGradientTest {

    @Test
    fun `composer hero gradient matches the mockup's exact 3-stop colors`() {
        assertEquals(
            listOf(Color(0xFF2C4260), Color(0xFF3C5570), Color(0xFF6E5B72)),
            ComposerHeroGradientColors
        )
    }
}
