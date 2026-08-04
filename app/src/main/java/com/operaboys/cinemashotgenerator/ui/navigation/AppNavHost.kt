package com.operaboys.cinemashotgenerator.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

// واحد ۱۶ — فاز ۰: Navigation Graph با ۴ مسیر ریشه‌ی خالی (Container) — طبق دستور
// کار: «مسیرهای داخلی Studio در فازهای بعدی اضافه می‌شوند، الان فقط Container های
// خالی کافی است». محتوای واقعی هر صفحه (Home/Projects/Assets/Studio Shell با ۴
// Tab) کار فازهای ۱ به بعد است.

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Home, modifier = modifier) {
        composable<Home> { EmptyRouteContainer(label = "Home") }
        composable<Projects> { EmptyRouteContainer(label = "Projects") }
        composable<Studio> { backStackEntry ->
            val studio: Studio = backStackEntry.toRoute()
            EmptyRouteContainer(label = "Studio (projectId=${studio.projectId})")
        }
        composable<Assets> { EmptyRouteContainer(label = "Assets") }
    }
}

@Composable
private fun EmptyRouteContainer(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
    }
}
