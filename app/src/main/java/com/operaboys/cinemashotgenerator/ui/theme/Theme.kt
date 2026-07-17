package com.operaboys.cinemashotgenerator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * تم موقت با رنگ‌های پیش‌فرض Material 3 — پالت نهایی طبق بلوپرینت UI تعیین خواهد شد.
 */
@Composable
fun CinemaShotGeneratorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        content = content
    )
}
