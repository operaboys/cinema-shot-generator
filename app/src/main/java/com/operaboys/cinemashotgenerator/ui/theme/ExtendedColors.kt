package com.operaboys.cinemashotgenerator.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// واحد ۱۶ — فاز ۰: توکن‌های رنگی که در ColorScheme استاندارد Material3 جایی ندارند
// (fg2/fg3/fg4 سه‌سطحی، success/warning/orange، hairline/cardBorder/inset، و
// SolidSurface طبق Implementation Notes بند ۱ docs/design/README.md). این‌ها از طریق
// یک CompositionLocal جداگانه در دسترس‌اند — دقیقاً همان الگوی توصیه‌شده‌ی رسمی
// Material3 برای گسترش تم با توکن‌های برند-محور فراتر از ColorScheme پیش‌فرض.

data class ExtendedColors(
    val fg2: Color,
    val fg3: Color,
    val fg4: Color,
    val success: Color,
    val warning: Color,
    val orange: Color,
    val cardBorder: Color,
    val hairline: Color,
    val hairlineStrong: Color,
    val inset: Color,
    val solidSurface: Color,
    val accentGradientEnd: Color
)

val DarkExtendedColors = ExtendedColors(
    fg2 = DarkTokens.Fg2,
    fg3 = DarkTokens.Fg3,
    fg4 = DarkTokens.Fg4,
    success = DarkTokens.Success,
    warning = DarkTokens.Warning,
    orange = DarkTokens.Orange,
    cardBorder = DarkTokens.CardBorder,
    hairline = DarkTokens.Hairline,
    hairlineStrong = DarkTokens.HairlineStrong,
    inset = DarkTokens.Inset,
    solidSurface = DarkTokens.SolidSurface,
    accentGradientEnd = DarkTokens.AccentGradientEnd
)

val LightExtendedColors = ExtendedColors(
    fg2 = LightTokens.Fg2,
    fg3 = LightTokens.Fg3,
    fg4 = LightTokens.Fg4,
    success = LightTokens.Success,
    warning = LightTokens.Warning,
    orange = LightTokens.Orange,
    cardBorder = LightTokens.CardBorder,
    hairline = LightTokens.Hairline,
    hairlineStrong = LightTokens.HairlineStrong,
    inset = LightTokens.Inset,
    solidSurface = LightTokens.SolidSurface,
    accentGradientEnd = LightTokens.AccentGradientEnd
)

private val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }

/** دسترسی به توکن‌های رنگی گسترش‌یافته، مشابه `MaterialTheme.colorScheme`. */
object CinemaTheme {
    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}

@Composable
internal fun ProvideExtendedColors(colors: ExtendedColors, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtendedColors provides colors, content = content)
}
