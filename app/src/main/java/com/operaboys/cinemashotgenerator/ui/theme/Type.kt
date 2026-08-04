package com.operaboys.cinemashotgenerator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.operaboys.cinemashotgenerator.R
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language

// واحد ۱۶ — فاز ۰ (تکمیل‌شده): مقیاس تایپوگرافی طبق docs/design/README.md بخش
// Typography — اکنون با فونت‌های واقعی Inter/Vazirmatn.
//
// MIGRATED (docs/adr/043-unit16-phase0-real-fonts.md): محدودیت مستندشده در
// docs/adr/042-unit16-phase0-shared-foundation.md («هیچ فایل فونت واقعی ضمیمه
// نشده بود؛ FontFamily.Default به‌عنوان Placeholder») رفع شد — فایل‌های واقعی
// Inter (۴ وزن) و Vazirmatn (۴ وزن) اکنون در app/src/main/res/font/ موجودند.

/** لاتین/UI — طبق docs/design/README.md. */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

/** فارسی — طبق docs/design/README.md («used automatically whenever lang = fa»). */
val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

/** سوییچ خودکار فونت بر اساس زبان فعلی UI — تنها نقطه‌ی تصمیم‌گیری Inter/Vazirmatn. */
fun cinemaFontFamily(language: Language): FontFamily = when (language) {
    Language.FA -> VazirmatnFontFamily
    Language.EN -> InterFontFamily
}

/** ۲۸/۳۶، Bold — عنوان‌های بزرگ/اعداد بزرگ. */
private fun displayLargeStyle(fontFamily: FontFamily) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 36.sp
)

/** ۲۰/۲۸ — عنوان بخش. */
private fun titleSectionStyle(fontFamily: FontFamily) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 28.sp
)

/** ۱۷/۲۴، Semibold — عنوان کارت. */
private fun titleCardStyle(fontFamily: FontFamily) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 17.sp,
    lineHeight = 24.sp
)

/** ۱۵/۲۲ — متن اصلی. */
private fun bodyStyle(fontFamily: FontFamily) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp
)

/** ۱۳/۱۸ — Caption/Meta/Label. */
private fun captionStyle(fontFamily: FontFamily) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp
)

/**
 * نگاشت ۵ سطح Scale سند طراحی به نزدیک‌ترین نقش‌های Typography واقعی Material3
 * (که ۱۵ نقش دارد، نه ۵) — فقط ۵ نقشی که این فاز واقعاً استفاده می‌کند بازنویسی
 * شدند؛ بقیه‌ی نقش‌های Typography پیش‌فرض M3 دست‌نخورده ماندند. `language` تعیین
 * می‌کند کدام FontFamily (Inter/Vazirmatn) در همه‌ی ۵ نقش استفاده شود.
 */
fun cinemaTypography(language: Language): Typography {
    val fontFamily = cinemaFontFamily(language)
    return Typography(
        headlineMedium = displayLargeStyle(fontFamily),
        titleLarge = titleSectionStyle(fontFamily),
        titleMedium = titleCardStyle(fontFamily),
        bodyLarge = bodyStyle(fontFamily),
        labelSmall = captionStyle(fontFamily)
    )
}
