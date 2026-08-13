package com.operaboys.cinemashotgenerator.ui.theme

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// رفع G12 باقی‌مانده (دستور کار ۲۰۲۶-۰۸-۱۳، docs/adr/080-...): dynamicFontEnabled
// قبلاً هیچ اثر Runtime ای نداشت (فقط Persist می‌شد). این تست هم‌الگو دقیق با
// AccessibilityLocalsTest (minTouchTargetEnabled) ثابت می‌کند سوییچ واقعاً
// LocalDensity.fontScale مؤثر را عوض می‌کند — نه فقط یک مقدار بی‌اثر.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `dynamicFontEnabled true multiplies the effective fontScale by 1_3`() {
        var baseFontScale = 0f
        var effectiveFontScale = 0f
        composeRule.setContent {
            baseFontScale = LocalDensity.current.fontScale
            CinemaShotGeneratorTheme(darkTheme = false, language = Language.FA, dynamicFontEnabled = true) {
                effectiveFontScale = LocalDensity.current.fontScale
            }
        }
        composeRule.waitForIdle()

        assertEquals(baseFontScale * 1.3f, effectiveFontScale, 0.001f)
    }

    @Test
    fun `dynamicFontEnabled false leaves fontScale unchanged`() {
        var baseFontScale = 0f
        var effectiveFontScale = 0f
        composeRule.setContent {
            baseFontScale = LocalDensity.current.fontScale
            CinemaShotGeneratorTheme(darkTheme = false, language = Language.FA, dynamicFontEnabled = false) {
                effectiveFontScale = LocalDensity.current.fontScale
            }
        }
        composeRule.waitForIdle()

        assertEquals(baseFontScale, effectiveFontScale, 0.001f)
    }
}
