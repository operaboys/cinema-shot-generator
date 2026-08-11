package com.operaboys.cinemashotgenerator.ui.theme

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// رفع بخشی G12 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، اولویت ۳
// بند ۸): minTouchTargetEnabled قبلاً فقط Persist می‌شد، هیچ اثر Runtime ای
// نداشت. هم‌الگو دقیق با ExtendedColors.kt (CompositionLocal برای مقادیر
// سراسری UI که باید به Composable های عمیقاً تودرتو برسند، بدون Thread کردن
// یک پارامتر تازه در هر لایه‌ی میانی — طبق دلیل مستند خودِ Theme.kt: تزریق
// مستقیم فقط وقتی معقول است که فراخوان مستقیماً یک لایه بالاتر دسترسی دارد).

private val LocalMinTouchTargetEnabled = staticCompositionLocalOf { false }

object CinemaAccessibility {
    val minTouchTargetEnabled: Boolean
        @Composable get() = LocalMinTouchTargetEnabled.current
}

@Composable
internal fun ProvideAccessibilityLocals(minTouchTargetEnabled: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMinTouchTargetEnabled provides minTouchTargetEnabled, content = content)
}

/**
 * حداقل اندازه‌ی لمس ۴۸dp (توصیه‌ی رسمی دسترس‌پذیری Material) — فقط وقتی
 * minTouchTargetEnabled فعال است اعمال می‌شود؛ در غیر این صورت هیچ تغییری
 * در Modifier ایجاد نمی‌کند (رفتار قبلی دقیقاً حفظ می‌شود).
 */
@Composable
fun Modifier.minTouchTargetIfEnabled(): Modifier =
    if (CinemaAccessibility.minTouchTargetEnabled) this.then(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) else this
