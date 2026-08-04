package com.operaboys.cinemashotgenerator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// واحد ۱۶ — فاز ۰: مقیاس تایپوگرافی طبق docs/design/README.md بخش Typography.
//
// محدودیت شناخته‌شده (مستند در docs/adr/042-...): سند طراحی Inter (لاتین/UI) و
// Vazirmatn (فارسی) را می‌خواهد، اما هیچ فایل فونت واقعی (.ttf/.otf) در
// docs/design/ ضمیمه نشده — فقط HTML/CSS/PNG. این فاز فقط Scale واقعی (اندازه/
// lineHeight/وزن) را دقیقاً پیاده می‌کند؛ FontFamily.Default (فونت سیستم) به‌عنوان
// Placeholder موقت استفاده شده — افزودن فایل‌های واقعی Inter/Vazirmatn به res/font/
// (یا اتصال Google Fonts Provider) یک کار آینده‌ی مجزا و مستند است، نه یک حدس اینجا.
val CinemaFontFamily: FontFamily = FontFamily.Default

/** ۲۸/۳۶، Bold — عنوان‌های بزرگ/اعداد بزرگ. */
val DisplayLargeStyle = TextStyle(
    fontFamily = CinemaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 36.sp
)

/** ۲۰/۲۸ — عنوان بخش. */
val TitleSectionStyle = TextStyle(
    fontFamily = CinemaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 28.sp
)

/** ۱۷/۲۴، Semibold — عنوان کارت. */
val TitleCardStyle = TextStyle(
    fontFamily = CinemaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 17.sp,
    lineHeight = 24.sp
)

/** ۱۵/۲۲ — متن اصلی. */
val BodyStyle = TextStyle(
    fontFamily = CinemaFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp
)

/** ۱۳/۱۸ — Caption/Meta/Label. */
val CaptionStyle = TextStyle(
    fontFamily = CinemaFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp
)

/**
 * نگاشت ۵ سطح Scale سند طراحی به نزدیک‌ترین نقش‌های Typography واقعی Material3
 * (که ۱۵ نقش دارد، نه ۵) — فقط ۵ نقشی که این فاز واقعاً استفاده می‌کند بازنویسی
 * شدند؛ بقیه‌ی نقش‌های Typography پیش‌فرض M3 دست‌نخورده ماندند (Composable های
 * فازهای بعدی می‌توانند مستقیماً از DisplayLargeStyle/... بالا هم استفاده کنند اگر
 * نقش M3 معادل دقیقی نداشت).
 */
val CinemaTypography = Typography(
    headlineMedium = DisplayLargeStyle,
    titleLarge = TitleSectionStyle,
    titleMedium = TitleCardStyle,
    bodyLarge = BodyStyle,
    labelSmall = CaptionStyle
)
