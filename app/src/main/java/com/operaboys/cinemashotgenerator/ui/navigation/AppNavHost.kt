package com.operaboys.cinemashotgenerator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.AutoSaveManager
import com.operaboys.cinemashotgenerator.data.repository.BackupFileStorage
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.PromptGenerationRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.ui.assets.AssetKind
import com.operaboys.cinemashotgenerator.ui.assets.AssetsScreen
import com.operaboys.cinemashotgenerator.ui.assets.CharacterAssetFormScreen
import com.operaboys.cinemashotgenerator.ui.assets.LocationAssetFormScreen
import com.operaboys.cinemashotgenerator.ui.assets.ObjectAssetFormScreen
import com.operaboys.cinemashotgenerator.ui.backups.BackupsScreen
import com.operaboys.cinemashotgenerator.ui.home.HomeScreen
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.project.ProjectsScreen
import com.operaboys.cinemashotgenerator.ui.scenes.SceneDetailScreen
import com.operaboys.cinemashotgenerator.ui.scenes.SceneDetailTab
import com.operaboys.cinemashotgenerator.ui.settings.SettingsScreen
import com.operaboys.cinemashotgenerator.ui.shots.ShotComposerScreen
import com.operaboys.cinemashotgenerator.ui.storybreakdown.AiStoryBreakdownScreen
import com.operaboys.cinemashotgenerator.ui.studio.StudioShell
import com.operaboys.cinemashotgenerator.ui.outputdelivery.OutputDeliveryScreen
import com.operaboys.cinemashotgenerator.ui.validation.ValidationScreen
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import com.operaboys.cinemashotgenerator.domain.workflow.ShotListViewMode

// واحد ۱۶ — فاز ۱: Navigation Graph با محتوای واقعی هر ۴ مسیر ریشه (فاز ۰ فقط
// Container خالی داشت). ProjectListViewModel یک نمونه‌ی مشترک است (نه یکی به‌ازای
// هر صفحه) — Home/Projects/StudioShell هر سه به همان یک فهرست پروژه‌ی زنده نیاز
// دارند؛ جزئیات در docs/adr/044-unit16-phase1-app-shell.md.

@Composable
fun AppNavHost(
    navController: NavHostController,
    workflowViewModel: WorkflowViewModel,
    projectListViewModel: ProjectListViewModel,
    onOpenDrawer: () -> Unit,
    onShowMessage: (String) -> Unit,
    storyRepository: StoryRepository? = null,
    assetRepository: AssetRepository? = null,
    sceneRepository: SceneRepository? = null,
    shotRepository: ShotRepository? = null,
    projectDnaRepository: ProjectDnaRepository? = null,
    promptGenerationRepository: PromptGenerationRepository? = null,
    autoSaveManager: AutoSaveManager? = null,
    backupFileStorage: BackupFileStorage? = null,
    database: AppDatabase? = null,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = Home, modifier = modifier) {
        composable<Home> {
            HomeScreen(
                workflowViewModel = workflowViewModel,
                projectListViewModel = projectListViewModel,
                onOpenDrawer = onOpenDrawer,
                onOpenProject = { projectId -> navController.navigate(Studio(projectId)) },
                onViewAllProjects = { navController.navigate(Projects) { launchSingleTop = true } },
                onShowMessage = onShowMessage
            )
        }
        composable<Projects> {
            ProjectsScreen(
                workflowViewModel = workflowViewModel,
                projectListViewModel = projectListViewModel,
                onOpenProject = { projectId -> navController.navigate(Studio(projectId)) },
                onShowMessage = onShowMessage
            )
        }
        composable<Studio> { backStackEntry ->
            val studio: Studio = backStackEntry.toRoute()
            StudioShell(
                projectId = studio.projectId,
                workflowViewModel = workflowViewModel,
                projectListViewModel = projectListViewModel,
                onBack = {
                    val target = resolveContextualBackTarget(Studio::class.qualifiedName) ?: Home
                    navController.navigate(target) { launchSingleTop = true }
                },
                onWarning = onShowMessage,
                storyRepository = storyRepository,
                projectDnaRepository = projectDnaRepository,
                sceneRepository = sceneRepository,
                autoSaveManager = autoSaveManager,
                backupFileStorage = backupFileStorage,
                database = database,
                initialTab = studio.initialTab,
                onNavigateToAiBreakdown = { targetProjectId ->
                    navController.navigate(AiStoryBreakdown(targetProjectId)) { launchSingleTop = true }
                },
                onNavigateToScene = { sceneId ->
                    navController.navigate(SceneDetail(studio.projectId, sceneId)) { launchSingleTop = true }
                }
            )
        }
        composable<Assets> {
            // رفع G6 ممیزی post-Unit16 (docs/adr/062-...): هم‌الگو دقیق با
            // MainScaffold.kt — دیگر به PLACEHOLDER_ACTIVE_PROJECT_ID متکی نیست.
            val workflowState by workflowViewModel.workflowState.collectAsStateWithLifecycle()
            val projectSummaries by projectListViewModel.projectSummaries.collectAsStateWithLifecycle()
            val activeProjectId = resolveActiveOrRecentProjectId(workflowState, projectSummaries)
            AssetsScreen(
                workflowViewModel = workflowViewModel,
                projectId = activeProjectId,
                assetRepository = assetRepository,
                shotRepository = shotRepository,
                onAddAsset = { kind -> navController.navigate(AssetForm(kind)) { launchSingleTop = true } },
                // رفع G7 ممیزی post-Unit16: تنها راه واقعی رسیدن به یک AssetForm با
                // existingAssetId غیر-null — لمس یک کارت Asset موجود.
                onOpenAsset = { kind, assetId ->
                    navController.navigate(AssetForm(kind, existingAssetId = assetId)) { launchSingleTop = true }
                },
                onNavigateToProjects = { navController.navigate(Projects) { launchSingleTop = true } },
                onShowMessage = onShowMessage
            )
        }
        composable<AssetForm> { backStackEntry ->
            val route: AssetForm = backStackEntry.toRoute()
            val language by workflowViewModel.language.collectAsStateWithLifecycle()
            val workflowState by workflowViewModel.workflowState.collectAsStateWithLifecycle()
            val projectSummaries by projectListViewModel.projectSummaries.collectAsStateWithLifecycle()
            val activeProjectId = resolveActiveOrRecentProjectId(workflowState, projectSummaries)
            val onFormBack = { navController.navigate(Assets) { launchSingleTop = true } }
            // این مسیر فقط از داخل AssetsScreen (شاخه‌ی projectId != null) قابل‌دسترس
            // است (grep تأیید کرد هیچ نقطه‌ی ورود دیگری در کل کدبیس وجود ندارد) — پس
            // این حالت در عمل هرگز رخ نمی‌دهد؛ فقط یک محافظ دفاعی است، نه یک مسیر
            // واقعی کاربر.
            if (activeProjectId == null) {
                LaunchedEffect(Unit) { onFormBack() }
                return@composable
            }
            when (route.kind) {
                AssetKind.CHARACTER -> CharacterAssetFormScreen(
                    language = language,
                    projectId = activeProjectId,
                    onBack = onFormBack,
                    onSaved = onFormBack,
                    onShowMessage = onShowMessage,
                    assetRepository = assetRepository,
                    existingAssetId = route.existingAssetId
                )
                AssetKind.LOCATION -> LocationAssetFormScreen(
                    language = language,
                    projectId = activeProjectId,
                    onBack = onFormBack,
                    onSaved = onFormBack,
                    assetRepository = assetRepository,
                    existingAssetId = route.existingAssetId
                )
                AssetKind.OBJECT -> ObjectAssetFormScreen(
                    language = language,
                    projectId = activeProjectId,
                    onBack = onFormBack,
                    onSaved = onFormBack,
                    assetRepository = assetRepository,
                    existingAssetId = route.existingAssetId
                )
            }
        }
        composable<AiStoryBreakdown> { backStackEntry ->
            val route: AiStoryBreakdown = backStackEntry.toRoute()
            val language by workflowViewModel.language.collectAsStateWithLifecycle()
            AiStoryBreakdownScreen(
                projectId = route.projectId,
                language = language,
                onBack = { navController.navigate(Studio(route.projectId)) { launchSingleTop = true } },
                onConfirmedAndSaved = { navController.navigate(Studio(route.projectId)) { launchSingleTop = true } },
                storyRepository = storyRepository,
                assetRepository = assetRepository,
                sceneRepository = sceneRepository,
                shotRepository = shotRepository
            )
        }
        composable<SceneDetail> { backStackEntry ->
            val route: SceneDetail = backStackEntry.toRoute()
            val language by workflowViewModel.language.collectAsStateWithLifecycle()
            val workflowState by workflowViewModel.workflowState.collectAsStateWithLifecycle()
            SceneDetailScreen(
                projectId = route.projectId,
                sceneId = route.sceneId,
                language = language,
                onBack = { navController.navigate(Studio(route.projectId)) { launchSingleTop = true } },
                onNavigateToScene = { newSceneId ->
                    navController.navigate(SceneDetail(route.projectId, newSceneId)) { launchSingleTop = true }
                },
                onNavigateToShot = { shotId, sceneNumber, sceneDisplayTitle ->
                    navController.navigate(ShotComposer(route.projectId, route.sceneId, sceneNumber, sceneDisplayTitle, shotId)) { launchSingleTop = true }
                },
                onShowMessage = onShowMessage,
                initialTab = route.initialTab,
                shotListViewMode = workflowState?.shotListViewMode ?: ShotListViewMode.GRID,
                onShotListViewModeChange = workflowViewModel::setShotListViewMode,
                sceneRepository = sceneRepository,
                assetRepository = assetRepository,
                shotRepository = shotRepository
            )
        }
        composable<ShotComposer> { backStackEntry ->
            val route: ShotComposer = backStackEntry.toRoute()
            val language by workflowViewModel.language.collectAsStateWithLifecycle()
            ShotComposerScreen(
                sceneId = route.sceneId,
                sceneDisplayTitle = route.sceneDisplayTitle,
                shotId = route.shotId,
                language = language,
                onBack = {
                    navController.navigate(SceneDetail(route.projectId, route.sceneId, SceneDetailTab.SHOTS.name)) { launchSingleTop = true }
                },
                onNavigateToValidation = { shotId ->
                    navController.navigate(
                        Validation(route.projectId, route.sceneId, route.sceneNumber, route.sceneDisplayTitle, shotId)
                    ) { launchSingleTop = true }
                },
                onNavigateToOutputDelivery = { shotId ->
                    navController.navigate(
                        OutputDelivery(route.projectId, route.sceneId, route.sceneNumber, route.sceneDisplayTitle, shotId)
                    ) { launchSingleTop = true }
                },
                shotRepository = shotRepository
            )
        }
        composable<Validation> { backStackEntry ->
            val route: Validation = backStackEntry.toRoute()
            val language by workflowViewModel.language.collectAsStateWithLifecycle()
            ValidationScreen(
                projectId = route.projectId,
                sceneId = route.sceneId,
                shotId = route.shotId,
                language = language,
                onBack = {
                    navController.navigate(
                        ShotComposer(route.projectId, route.sceneId, route.sceneNumber, route.sceneDisplayTitle, route.shotId)
                    ) { launchSingleTop = true }
                },
                onNavigateToOutputDelivery = { shotId ->
                    navController.navigate(
                        OutputDelivery(route.projectId, route.sceneId, route.sceneNumber, route.sceneDisplayTitle, shotId)
                    ) { launchSingleTop = true }
                },
                shotRepository = shotRepository,
                sceneRepository = sceneRepository,
                projectDnaRepository = projectDnaRepository,
                assetRepository = assetRepository
            )
        }
        composable<OutputDelivery> { backStackEntry ->
            val route: OutputDelivery = backStackEntry.toRoute()
            val language by workflowViewModel.language.collectAsStateWithLifecycle()
            val workflowState by workflowViewModel.workflowState.collectAsStateWithLifecycle()
            OutputDeliveryScreen(
                shotId = route.shotId,
                language = language,
                onBack = {
                    navController.navigate(
                        ShotComposer(route.projectId, route.sceneId, route.sceneNumber, route.sceneDisplayTitle, route.shotId)
                    ) { launchSingleTop = true }
                },
                initialModelProfileId = workflowState?.selectedModelProfileId,
                onModelProfileSelected = workflowViewModel::setSelectedModelProfileId,
                onShowMessage = onShowMessage,
                promptGenerationRepository = promptGenerationRepository
            )
        }
        composable<Settings> {
            SettingsScreen(
                workflowViewModel = workflowViewModel,
                onBack = { navController.navigate(Home) { launchSingleTop = true } },
                onShowMessage = onShowMessage
            )
        }
        composable<Backups> {
            // Backups per-project است (BackupManager سازنده‌اش projectId می‌خواهد) —
            // برخلاف Settings، این مسیر خودش هیچ آرگومانی ندارد (هم‌الگو با Assets)
            // پس projectId فعال از WorkflowState.projectId (Session جاری Studio، اگر
            // باشد) خوانده می‌شود. جزئیات کامل تصمیم در
            // docs/adr/059-unit16-phase6-step2-backups-final-review.md.
            val workflowState by workflowViewModel.workflowState.collectAsStateWithLifecycle()
            val language by workflowViewModel.language.collectAsStateWithLifecycle()
            val summaries by projectListViewModel.projectSummaries.collectAsStateWithLifecycle()
            val activeProjectId = workflowState?.projectId
            val activeProjectName = summaries.find { it.project.projectId == activeProjectId }?.project?.projectName ?: activeProjectId.orEmpty()
            BackupsScreen(
                projectId = activeProjectId,
                projectName = activeProjectName,
                language = language,
                onBack = { navController.navigate(Home) { launchSingleTop = true } },
                onShowMessage = onShowMessage,
                backupFileStorage = backupFileStorage,
                database = database
            )
        }
    }
}
