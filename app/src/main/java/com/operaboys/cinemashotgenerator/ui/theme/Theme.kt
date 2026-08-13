package com.operaboys.cinemashotgenerator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language

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

/**
 * MIGRATED (docs/adr/043-unit16-phase0-real-fonts.md): پارامتر `language` اضافه شد
 * تا `typography` بتواند طبق docs/design/README.md («Vazirmatn — used automatically
 * whenever lang = fa») بین Inter/Vazirmatn سوییچ کند — تصمیم مستند: تزریق مستقیم
 * `language` به این تابع (به‌جای خواندن آن از یک CompositionLocal جداگانه)، چون
 * فراخوان همیشه‌اش (`App.kt`) از قبل `language` را از `WorkflowViewModel` دارد و
 * مستقیماً همان الگوی موجود `darkTheme: Boolean` (که همان‌طور مستقیماً تزریق
 * می‌شود، نه از CompositionLocal) را دنبال می‌کند — بدون نیاز به یک لایه‌ی
 * انتزاعی جدید.
 */
// MIGRATED (رفع بخشی G12 ممیزی post-Unit16، اولویت ۳ بند ۸): minTouchTargetEnabled
// اضافه شد — پیش‌فرض false، پس فراخوان‌های تست/قدیمی موجود بدون تغییر رفتار
// کار می‌کنند. هم‌الگو با ExtendedColors (CompositionLocal برای Composable های
// عمیقاً تودرتو مثل AiStoryBreakdownScreen.PhaseCircle).
//
// رفع G12 باقی‌مانده (دستور کار ۲۰۲۶-۰۸-۱۳، docs/adr/080-...): dynamicFontEnabled
// هم‌الگو اضافه شد. برخلاف minTouchTargetEnabled (یک Modifier محلی روی یک هدف
// مشخص)، «فونت پویا» باید کل اپ را بدون دست‌کاری تک‌تک Composable ها بپوشاند —
// راه رسمی/idiomatic Compose برای این کار Override کردن LocalDensity.fontScale
// است (نه ضرب دستی هر اندازه‌ی sp)، چون تمام Text/TextField های موجود (و آینده)
// خودکار از آن پیروی می‌کنند. ضریب ۱.۳ (نه یک مقدار دلبخواه دیگر) انتخاب شد چون
// نزدیک‌ترین معادل به پیش‌فرض «Large Text» رسمی Android Accessibility
// (حدوداً ۱.۳x در تنظیمات Font Size استاندارد سیستم) است — به‌جای اختراع یک
// مقیاس اختیاری تازه. ضرب در `density.fontScale` موجود (نه جایگزینی کامل آن)
// عمداً است تا تنظیم «اندازه‌ی فونت» سطح سیستم کاربر هم همچنان محترم شمرده شود،
// نه Override کامل — این سوییچ فقط یک افزایش اضافه روی همان مقدار پایه است.
private const val DYNAMIC_FONT_SCALE_MULTIPLIER = 1.3f

@Composable
fun CinemaShotGeneratorTheme(
    darkTheme: Boolean,
    language: Language,
    minTouchTargetEnabled: Boolean = false,
    dynamicFontEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val baseDensity = LocalDensity.current
    val effectiveDensity = if (dynamicFontEnabled) {
        Density(density = baseDensity.density, fontScale = baseDensity.fontScale * DYNAMIC_FONT_SCALE_MULTIPLIER)
    } else {
        baseDensity
    }
    ProvideExtendedColors(colors = extendedColors) {
        ProvideAccessibilityLocals(minTouchTargetEnabled = minTouchTargetEnabled) {
            CompositionLocalProvider(LocalDensity provides effectiveDensity) {
                MaterialTheme(
                    colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
                    typography = cinemaTypography(language),
                    content = content
                )
            }
        }
    }
}
