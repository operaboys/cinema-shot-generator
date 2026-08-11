package com.operaboys.cinemashotgenerator.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// رفع بخشی G12 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، اولویت ۳
// بند ۸): minTouchTargetEnabled قبلاً هیچ اثر Runtime ای نداشت. این تست ثابت
// می‌کند minTouchTargetIfEnabled واقعاً اندازه‌ی قابل‌مشاهده‌ی عنصر را عوض می‌کند
// (نه فقط یک مقدار بی‌اثر Persist می‌شود).

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccessibilityLocalsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `minTouchTargetIfEnabled expands a small element to at least 48dp when the switch is on`() {
        composeRule.setContent {
            ProvideAccessibilityLocals(minTouchTargetEnabled = true) {
                Box(modifier = Modifier.testTag("target").minTouchTargetIfEnabled().size(24.dp))
            }
        }

        composeRule.onNodeWithTag("target")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `minTouchTargetIfEnabled leaves the element at its original size when the switch is off`() {
        composeRule.setContent {
            ProvideAccessibilityLocals(minTouchTargetEnabled = false) {
                Box(modifier = Modifier.testTag("target").minTouchTargetIfEnabled().size(24.dp))
            }
        }

        composeRule.onNodeWithTag("target")
            .assertWidthIsEqualTo(24.dp)
            .assertHeightIsEqualTo(24.dp)
    }
}
