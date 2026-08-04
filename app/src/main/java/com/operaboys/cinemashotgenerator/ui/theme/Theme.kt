package com.operaboys.cinemashotgenerator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// واحد ۱۶ — فاز ۰ (پایه‌ی مشترک): Theme.kt واقعی طبق Design Tokens
// docs/design/README.md — جایگزین تم موقت M3 پیش‌فرض قدم‌های قبلی.
//
// نگاشت توکن‌های سند به ColorScheme استاندارد Material3: primary/secondary/tertiary
// از accent (طبق سند رنگ لهجه‌ی واحد است، نه سه رنگ جدا)، background/surface از bg،
// onBackground/onSurface از fg، error/onError طبق سند. توکن‌های بدون معادل مستقیم در
// ColorScheme (fg2/fg3/fg4/success/warning/orange/hairline/cardBorder/...) از طریق
// ExtendedColors (فایل جداگانه) در دسترس‌اند.
//
// «Liquid Glass» (Blur/Gradient/Glass Card واقعی) عمداً در این فاز پیاده نشد — این
// فاز فقط توکن‌های خام + ساختار تم را برقرار می‌کند؛ اعمال این توکن‌ها روی سطوح واقعی
// (Card/BottomSheet/Header با blur/gradient) کار فازهای بعدی (ساخت خودِ صفحات) است.

private val DarkColorScheme = darkColorScheme(
    primary = DarkTokens.Accent,
    onPrimary = DarkTokens.Fg,
    secondary = DarkTokens.Accent,
    tertiary = DarkTokens.Orange,
    background = DarkTokens.Bg,
    onBackground = DarkTokens.Fg,
    surface = DarkTokens.Bg,
    onSurface = DarkTokens.Fg,
    error = DarkTokens.Error,
    onError = DarkTokens.Fg
)

private val LightColorScheme = lightColorScheme(
    primary = LightTokens.Accent,
    onPrimary = LightTokens.Bg,
    secondary = LightTokens.Accent,
    tertiary = LightTokens.Orange,
    background = LightTokens.Bg,
    onBackground = LightTokens.Fg,
    surface = LightTokens.Bg,
    onSurface = LightTokens.Fg,
    error = LightTokens.Error,
    onError = LightTokens.Bg
)

@Composable
fun CinemaShotGeneratorTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    ProvideExtendedColors(colors = extendedColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = CinemaTypography,
            content = content
        )
    }
}
