package com.operaboys.cinemashotgenerator.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * صفحه‌ی تست موقت — فقط برای اثبات این‌که Scaffold پروژه Build و Run می‌شود.
 * صفحه‌های واقعی طبق بلوپرینت‌های docs/blueprints پیاده‌سازی خواهند شد.
 */
@Composable
fun MainScreen() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Hello World — Cinema Shot Generator",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
