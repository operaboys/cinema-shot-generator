package com.operaboys.cinemashotgenerator.ui.theme

import androidx.compose.ui.graphics.Color

// واحد ۱۶ — فاز ۰ (پایه‌ی مشترک): Design Tokens رنگ.
// منبع حقیقت: docs/design/README.md بخش «Design Tokens» (جدول‌های Dark/Light theme).
//
// این فایل فقط مقادیر خام hex را نگه می‌دارد؛ نگاشت این توکن‌ها به ColorScheme واقعی
// Material3 و ExtendedColors (fg2/fg3/fg4/success/warning/orange/...) در Theme.kt و
// ExtendedColors.kt انجام می‌شود.

object DarkTokens {
    val Bg = Color(0xFF0E1420)
    val Fg = Color(0xFFFFFFFF)
    val Fg2 = Color(0xFFC2C8D8)
    val Fg3 = Color(0xFFA3ABC2)
    val Fg4 = Color(0xFF8E96AE)
    val Accent = Color(0xFF7C5CFF)
    val AccentGradientEnd = Color(0xFF8E74FF)
    val Success = Color(0xFF3DDC97)
    val Warning = Color(0xFFFFB648)
    val Error = Color(0xFFFF5A6A)
    val Orange = Color(0xFFFF9A4A) // لاک/DNA accent

    val CardBorder = Color(0x29FFFFFF) // rgba(255,255,255,.16)
    val Hairline = Color(0x1AFFFFFF) // rgba(255,255,255,.10)
    val HairlineStrong = Color(0x2EFFFFFF) // rgba(255,255,255,.18)
    val Inset = Color(0x0FFFFFFF) // rgba(255,255,255,.06)

    // Implementation Notes بند ۱ (docs/design/README.md): فیلتر/Segmented-Control/Alert
    // هرگز نباید Low-Opacity باشد — این پرکننده‌ی Solid برای همان موارد است، نه Glass Card.
    val SolidSurface = Color(0xFF212B4A)
}

object LightTokens {
    val Bg = Color(0xFFEDF1F8)
    val Fg = Color(0xFF141A2B)
    val Fg2 = Color(0xFF3B4462)
    val Fg3 = Color(0xFF4C5678)
    val Fg4 = Color(0xFF636C8B)

    // طبق سند، accent/success/warning/error/orange بین دو تم یکسان‌اند.
    val Accent = DarkTokens.Accent
    val AccentGradientEnd = DarkTokens.AccentGradientEnd
    val Success = DarkTokens.Success
    val Warning = DarkTokens.Warning
    val Error = DarkTokens.Error
    val Orange = DarkTokens.Orange

    val CardBorder = Color(0xEBFFFFFF) // rgba(255,255,255,.92)
    val Hairline = Color(0x1A141E3C) // rgba(20,30,60,.10)
    val HairlineStrong = Color(0x24141E3C) // rgba(20,30,60,.14)
    val Inset = Color(0x0D141E3C) // rgba(20,30,60,.05)

    // Implementation Notes بند ۱: معادل روشن SolidSurface — طبق سند «light theme white».
    val SolidSurface = Color(0xFFFFFFFF)
}
