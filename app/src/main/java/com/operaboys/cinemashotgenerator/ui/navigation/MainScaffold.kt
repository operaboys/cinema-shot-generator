package com.operaboys.cinemashotgenerator.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel

// واحد ۱۶ — فاز ۰: نقطه‌ی اتصال ساختار Navigation دو‌لایه — Scaffold بالاترین سطح
// اپ که هم نوار پایین ثابت (لایه‌ی ۱) و هم Top Tab Row شرطی درون‌Studio (لایه‌ی ۲) و
// هم Back Navigation Contextual را به هم وصل می‌کند. طبق دستور کار بند ۴: منطق Back
// اینجا مرکزی است (نه پراکنده در هر صفحه).

@Composable
fun MainScaffold(workflowViewModel: WorkflowViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val language by workflowViewModel.language.collectAsStateWithLifecycle()
    val workflowState by workflowViewModel.workflowState.collectAsStateWithLifecycle()

    val backTarget = resolveContextualBackTarget(currentDestination?.route)
    BackHandler(enabled = backTarget != null) {
        val target = backTarget
        if (target != null) {
            navController.navigate(target) { launchSingleTop = true }
        }
    }

    val isInStudio = currentDestination?.hierarchy?.any { it.hasRoute<Studio>() } == true
    var selectedStudioTab by rememberSaveable { mutableStateOf(StudioTab.STORY) }

    Scaffold(
        bottomBar = {
            AppBottomNavBar(
                currentDestination = currentDestination,
                language = language,
                onNavigate = { route ->
                    navController.navigate(route) { launchSingleTop = true }
                },
                onQuickCreate = {
                    // MIGRATED (فاز بعدی): رفتار «ایجاد سریع» Context-aware (کدام نوع
                    // Entity، بسته به صفحه‌ی فعلی) نیازمند تصمیم‌گیری UI/UX جداگانه است؛
                    // این فاز فقط دکمه را ساختاری/کلیک‌پذیر نگه می‌دارد.
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isInStudio) {
                StudioTopTabRow(
                    selectedTab = selectedStudioTab,
                    workflowState = workflowState,
                    language = language,
                    onTabSelected = { selectedStudioTab = it },
                    onWarning = {
                        // MIGRATED (فاز بعدی): نمایش واقعی هشدار (Snackbar/Toast طبق
                        // docs/design/README.md بخش Interactions) — این فاز فقط قلاب
                        // (callback) آن را فراهم می‌کند.
                    }
                )
            }
            AppNavHost(navController = navController, modifier = Modifier.fillMaxSize())
        }
    }
}
