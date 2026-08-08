package com.operaboys.cinemashotgenerator.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.PromptGenerationRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.scenes.SceneDetailTab
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.launch

// واحد ۱۶ — فاز ۰/۱: نقطه‌ی اتصال ساختار Navigation دو‌لایه — Scaffold بالاترین سطح
// اپ که نوار پایین ثابت (لایه‌ی ۱)، Back Navigation Contextual، Drawer (بخش ه فاز ۱)،
// و SnackbarHost مشترک (برای پیام‌های واقعی/«به‌زودی») را به هم وصل می‌کند.
//
// MIGRATED (فاز ۱، docs/adr/044-unit16-phase1-app-shell.md): StudioTopTabRow/
// selectedStudioTab که در فاز ۰ اینجا بودند به داخل StudioShell (مسیر Studio واقعی)
// منتقل شدند — چون این فاز باید هر Tab را به یک بدنه‌ی واقعی وصل کند، نه فقط
// نمایش/عدم‌نمایش تب‌ها؛ جزئیات کامل در ADR-044.
//
// ProjectListViewModel اکنون پارامتر ورودی است (نه ساخته‌شده‌ی داخلی با
// viewModel(factory=...)) — هم‌الگو با WorkflowViewModel موجود از فاز ۰؛ این
// یکدستی دقیقاً همان چیزی است که تست‌پذیری این Composable را (بدون نیاز به
// چرخه‌ی واقعی ViewModelStore در تست) ممکن می‌کند — طبق همان الگویی که
// AppNavigationTest.kt فاز ۰ از قبل برای WorkflowViewModel استفاده می‌کرد.

@Composable
fun MainScaffold(
    workflowViewModel: WorkflowViewModel,
    projectListViewModel: ProjectListViewModel,
    storyRepository: StoryRepository? = null,
    assetRepository: AssetRepository? = null,
    sceneRepository: SceneRepository? = null,
    shotRepository: ShotRepository? = null,
    projectDnaRepository: ProjectDnaRepository? = null,
    promptGenerationRepository: PromptGenerationRepository? = null
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val language by workflowViewModel.language.collectAsStateWithLifecycle()

    // AiStoryBreakdown/SceneDetail/ShotComposer نمی‌توانند در backTargetsByRouteKey
    // (ui/navigation/BackNavigation.kt) بیایند چون مقصدشان به آرگومان‌های Runtime
    // نیاز دارد، در حالی که آن Map فقط برای مقصدهای بدون‌آرگومان طراحی شده — طبق
    // تصمیم مستند، این استثناها اینجا (محل واقعی navController) مدیریت می‌شوند،
    // بدون تغییر امضای تابع خالص تست‌شده‌ی resolveContextualBackTarget.
    // SceneDetail→Studio و Composer→Shots هر دو دقیقاً همان قانون صریح
    // docs/design/README.md بخش Interactions هستند. جزئیات کامل در
    // docs/adr/046-unit16-phase2-step2-ai-story-breakdown.md،
    // docs/adr/050-unit16-phase4-step1-scene-detail.md و
    // docs/adr/051-unit16-phase4-step2-shot-list-composer-skeleton.md.
    val backTarget = when {
        currentDestination?.hasRoute<AiStoryBreakdown>() == true ->
            backStackEntry?.toRoute<AiStoryBreakdown>()?.projectId?.let { Studio(it) }
        currentDestination?.hasRoute<SceneDetail>() == true ->
            backStackEntry?.toRoute<SceneDetail>()?.projectId?.let { Studio(it) }
        currentDestination?.hasRoute<ShotComposer>() == true ->
            backStackEntry?.toRoute<ShotComposer>()?.let { SceneDetail(it.projectId, it.sceneId, SceneDetailTab.SHOTS.name) }
        else -> resolveContextualBackTarget(currentDestination?.route)
    }
    BackHandler(enabled = backTarget != null) {
        val target = backTarget
        if (target != null) {
            navController.navigate(target) { launchSingleTop = true }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val comingSoonMessage = uiString("drawer.comingSoon", language)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavDrawerContent(
                language = language,
                onNavigateAssets = {
                    coroutineScope.launch { drawerState.close() }
                    navController.navigate(Assets) { launchSingleTop = true }
                },
                onComingSoon = {
                    coroutineScope.launch {
                        drawerState.close()
                        snackbarHostState.showSnackbar(comingSoonMessage)
                    }
                }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        // «ایجاد سریع» پروژه از داخل Home (ردیف Quick-create) در همین فاز
                        // پیاده شد — این FAB سراسری به همان جریان آینده موکول است.
                    }
                )
            }
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                workflowViewModel = workflowViewModel,
                projectListViewModel = projectListViewModel,
                onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                onShowMessage = { message -> coroutineScope.launch { snackbarHostState.showSnackbar(message) } },
                storyRepository = storyRepository,
                assetRepository = assetRepository,
                sceneRepository = sceneRepository,
                shotRepository = shotRepository,
                projectDnaRepository = projectDnaRepository,
                promptGenerationRepository = promptGenerationRepository,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}
