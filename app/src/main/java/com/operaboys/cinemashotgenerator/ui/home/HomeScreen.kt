package com.operaboys.cinemashotgenerator.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.project.ProjectSummary
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import com.operaboys.cinemashotgenerator.domain.workflow.HomeLayoutVariant
import com.operaboys.cinemashotgenerator.domain.workflow.WorkflowState
import com.operaboys.cinemashotgenerator.domain.workflow.WorkflowStep
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.project.ProjectListSection
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// واحد ۱۶ فاز ۱ — صفحه‌ی Home طبق docs/design/README.md بخش «۱. Home».
//
// محدودیت مستند (ADR-044): تصویر پس‌زمینه‌ی Full-bleed واقعی (آپلود کاربر) پیاده
// نشد — طبق تصریح خودِ دستور کار «فعلاً Placeholder gradient کافی است»؛ آپلود
// واقعی («Home Screen Image» در Settings) کار فاز Settings آینده است. لوگوی
// «Aperture C» (SVG سفارشی سند طراحی) هم با یک آیکون Material موقت جایگزین شد —
// وارد کردن SVG سفارشی به Compose (ImageVector از Path Data) کار جداگانه‌ای است
// که این فاز جزو Scope اش نبود.

// testTag (نه onNodeWithText) — چون عنوان AlertDialog دقیقاً همان متن عنوان
// QuickCreateRow است ("پروژه‌ی جدید")، و هر دو هم‌زمان در درخت Composition
// حاضرند وقتی دیالوگ باز است؛ همان الگوی رفع تصادف متنی BottomNavBar.kt.
const val CREATE_PROJECT_NAME_FIELD_TAG = "createProject.nameField"
// واحد ۱۶ فاز ۶ — قدم ۱: لازم برای تست End-to-End صفحه‌ی Settings (باز کردن Drawer
// از Home، تنها نقطه‌ی واقعی ورود به Settings).
const val HOME_OPEN_DRAWER_BUTTON_TAG = "home.openDrawerButton"
// رفع G12 باقی‌مانده (دستور کار ۲۰۲۶-۰۸-۱۳، docs/adr/080-...): لازم برای تست
// اثبات واقعی رندر homeScreenImageUri (نه فقط Persist شدنش).
const val HOME_BACKGROUND_IMAGE_TAG = "home.backgroundImage"
// رفع G12 باقی‌مانده — homeLayoutVariant.RESUME (دستور کار ۲۰۲۶-۰۸-۱۳،
// docs/adr/081-...، طبق mockup docs/design/Cinema Studio.html).
const val HOME_RESUME_CARD_TAG = "home.resume.card"
const val HOME_RESUME_BUTTON_TAG = "home.resume.button"
const val HOME_RESUME_TILE_BREAKDOWN_TAG = "home.resume.tile.breakdown"
const val HOME_RESUME_TILE_DNA_TAG = "home.resume.tile.dna"
const val HOME_RESUME_TILE_VALIDATION_TAG = "home.resume.tile.validation"
const val HOME_RESUME_TILE_OUTPUT_TAG = "home.resume.tile.output"
const val HOME_RESUME_OTHER_PROJECTS_TAG = "home.resume.otherProjects"

@Composable
fun HomeScreen(
    workflowViewModel: WorkflowViewModel,
    projectListViewModel: ProjectListViewModel,
    onOpenDrawer: () -> Unit,
    onOpenProject: (String) -> Unit,
    onViewAllProjects: () -> Unit,
    onShowMessage: (String) -> Unit,
    // رفع G12 باقی‌مانده (دستور کار ۲۰۲۶-۰۸-۱۳، docs/adr/081-...): لازم برای
    // کاشی‌های میان‌بر «DNA»/«اعتبارسنجی»/«خروجی» در چیدمان RESUME — هرکدام
    // باید بتواند Studio همان پروژه را روی یک Tab مشخص (نه همیشه STORY) باز کند.
    // پیش‌فرض به onOpenProject برمی‌گردد (initialTab نادیده گرفته می‌شود) تا هیچ
    // فراخوان موجودی (تست‌ها، سایر مسیرها) نشکند.
    onOpenProjectTab: (projectId: String, initialTab: String) -> Unit = { pid, _ -> onOpenProject(pid) },
    // برای کاشی «AI Breakdown» — پیش‌فرض به onOpenProject برمی‌گردد (باز شدن
    // Studio به‌جای صفحه‌ی AI Breakdown مستقیم) اگر فراخواننده این را وصل نکند.
    onOpenAiBreakdown: (projectId: String) -> Unit = { onOpenProject(it) }
) {
    val language by workflowViewModel.language.collectAsStateWithLifecycle()
    val theme by workflowViewModel.theme.collectAsStateWithLifecycle()
    val summaries by projectListViewModel.projectSummaries.collectAsStateWithLifecycle()
    val lastActionMessage by projectListViewModel.lastActionMessage.collectAsStateWithLifecycle()
    val homeScreenImageUri by workflowViewModel.homeScreenImageUri.collectAsStateWithLifecycle()
    val homeLayoutVariant by workflowViewModel.homeLayoutVariant.collectAsStateWithLifecycle()
    val workflowState by workflowViewModel.workflowState.collectAsStateWithLifecycle()

    LaunchedEffect(lastActionMessage) {
        lastActionMessage?.let { message ->
            onShowMessage(message)
            projectListViewModel.clearLastActionMessage()
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // رفع G12 باقی‌مانده (دستور کار ۲۰۲۶-۰۸-۱۳، docs/adr/080-...): وقتی کاربر
        // یک تصویر واقعی از Settings انتخاب کرده (`homeScreenImageUri`، از قبل با
        // OpenDocument()/takePersistableUriPermission واقعی Persist می‌شود، طبق
        // ADR-075)، به‌جای گرادیان ساده به‌عنوان پس‌زمینه‌ی Full-bleed رندر می‌شود —
        // دقیقاً طبق سند طراحی. یک Scrim گرادیانی (نه گرادیان تخت قبلی) روی آن
        // اعمال می‌شود تا متن‌های بالای صفحه هنوز خوانا بمانند. وقتی مقدار null
        // است (پیش‌فرض)، رفتار قبلی (فقط گرادیان تخت) بدون تغییر می‌ماند.
        val backgroundImageUri = homeScreenImageUri
        if (backgroundImageUri != null) {
            HomeBackgroundImage(uriString = backgroundImageUri, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.background, CinemaTheme.extendedColors.hairline)
                        )
                    )
            )
        }
        Column(modifier = Modifier.fillMaxSize()) {
            HomeHeader(
                language = language,
                theme = theme,
                onOpenDrawer = onOpenDrawer,
                onToggleLanguage = { workflowViewModel.setLanguage(if (language == Language.FA) Language.EN else Language.FA) },
                onToggleTheme = { workflowViewModel.setTheme(if (theme == AppTheme.DARK) AppTheme.LIGHT else AppTheme.DARK) }
            )

            when (homeLayoutVariant) {
                HomeLayoutVariant.HERO -> Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = uiString("home.greetingTitle", language),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = uiString("home.greetingSubtitle", language),
                        style = MaterialTheme.typography.bodyLarge,
                        color = CinemaTheme.extendedColors.fg2
                    )

                    QuickCreateRow(language = language, onClick = { showCreateDialog = true }, modifier = Modifier.padding(top = 24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = uiString("home.recentProjects", language), style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = uiString("home.all", language),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable(onClick = onViewAllProjects)
                        )
                    }

                    if (summaries.isEmpty()) {
                        Text(
                            text = uiString("home.emptyState", language),
                            style = MaterialTheme.typography.bodyLarge,
                            color = CinemaTheme.extendedColors.fg3,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        ProjectListSection(
                            summaries = summaries.take(3),
                            language = language,
                            viewModel = projectListViewModel,
                            onOpenProject = onOpenProject,
                            showThumbnails = true,
                            modifier = Modifier.weight(1f).padding(bottom = 24.dp)
                        )
                    }
                }

                HomeLayoutVariant.RESUME -> HomeResumeContent(
                    language = language,
                    summaries = summaries,
                    workflowState = workflowState,
                    onOpenProject = onOpenProject,
                    onOpenProjectTab = onOpenProjectTab,
                    onOpenAiBreakdown = onOpenAiBreakdown,
                    onShowCreateDialog = { showCreateDialog = true },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            language = language,
            onConfirm = { name ->
                projectListViewModel.createProject(name = name, uiLanguage = language, onCreated = { onOpenProject(it.projectId) })
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

/**
 * چیدمان `homeLayoutVariant.RESUME` — طبق docs/design/Cinema Studio.html
 * (بخش `is.homeB`، خط تقریبی ۲۰۸۲۷۶۵): کارت «ادامه» (آخرین پروژه‌ی
 * به‌روزرسانی‌شده، طبق `projectSummaries` که از قبل `ORDER BY lastModified
 * DESC` است — `ProjectDao.kt`) + نوار پیشرفت ۹ بخشی + دکمه‌ی ادامه؛ ۴ کاشی
 * میان‌بر (AI Breakdown/DNA/اعتبارسنجی/خروجی)؛ فهرست افقی «پروژه‌های دیگر».
 *
 * **محدودیت صادقانه‌ی مستند (نه یک باگ):** نوار پیشرفت/زیرعنوان مرحله فقط
 * وقتی دقیق است که `workflowState` واقعی این نشست برای دقیقاً همین پروژه
 * باشد (یعنی کاربر همین الان از Studio همین پروژه به Home برگشته — سناریوی
 * واقعی «ادامه از جایی که ماندید»). این اپ هیچ‌جا مرحله‌ی گردش‌کار را
 * per-project روی دیسک Persist نمی‌کند (`WorkflowState` عمداً یک‌بار-مصرفِ
 * همان نشست است، طبق ADR-042) — پس برای پروژه‌ای که این نشست هنوز باز نشده
 * (مثلاً بعد از باز کردن دوباره‌ی اپ)، ساختن یک «مرحله»ی جعلی گمراه‌کننده
 * بود؛ در آن حالت نوار خالی (نه پُر با حدس) و زیرعنوان از شمارش واقعی
 * صحنه/شات (`project.metaTemplate`، همان کلید StudioHeader) استفاده می‌کند.
 */
@Composable
private fun HomeResumeContent(
    language: Language,
    summaries: List<ProjectSummary>,
    workflowState: WorkflowState?,
    onOpenProject: (String) -> Unit,
    onOpenProjectTab: (projectId: String, initialTab: String) -> Unit,
    onOpenAiBreakdown: (projectId: String) -> Unit,
    onShowCreateDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mostRecent = summaries.firstOrNull()

    Column(modifier = modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        if (mostRecent == null) {
            Text(
                text = uiString("home.greetingTitle", language),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = uiString("home.emptyState", language),
                style = MaterialTheme.typography.bodyLarge,
                color = CinemaTheme.extendedColors.fg3,
                modifier = Modifier.padding(top = 8.dp)
            )
            QuickCreateRow(language = language, onClick = onShowCreateDialog, modifier = Modifier.padding(top = 24.dp))
            return@Column
        }

        ResumeProgressCard(
            language = language,
            summary = mostRecent,
            workflowState = workflowState,
            onResume = { onOpenProject(mostRecent.project.projectId) },
            modifier = Modifier.padding(top = 16.dp)
        )

        val quickTiles = resumeQuickTiles(mostRecent.project.projectId, language, onOpenAiBreakdown, onOpenProjectTab)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().height(192.dp).padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            gridItems(quickTiles) { tile ->
                ResumeQuickTile(tile)
            }
        }

        val others = summaries.drop(1)
        if (others.isNotEmpty()) {
            Text(
                text = uiString("home.resume.otherProjectsTitle", language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().testTag(HOME_RESUME_OTHER_PROJECTS_TAG),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(others, key = { it.project.projectId }) { summary ->
                    ResumeOtherProjectCard(
                        summary = summary,
                        language = language,
                        onClick = { onOpenProject(summary.project.projectId) },
                        modifier = Modifier.width(160.dp)
                    )
                }
            }
            Box(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResumeProgressCard(
    language: Language,
    summary: ProjectSummary,
    workflowState: WorkflowState?,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matchingState = workflowState?.takeIf { it.projectId == summary.project.projectId }
    val filledSteps = matchingState?.let { it.currentStep.ordinal + 1 } ?: 0
    val subtitle = if (matchingState != null) {
        uiTemplate(
            "home.resume.stepTemplate",
            language,
            "number" to (matchingState.currentStep.ordinal + 1).toString(),
            "name" to workflowStepLabel(matchingState.currentStep, language)
        )
    } else {
        uiTemplate("project.metaTemplate", language, "scenes" to summary.sceneCount.toString(), "shots" to summary.shotCount.toString())
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth().testTag(HOME_RESUME_CARD_TAG)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = uiString("home.resume.caption", language), style = MaterialTheme.typography.bodyMedium, color = CinemaTheme.extendedColors.fg3)
            Text(
                text = summary.project.projectName,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, color = CinemaTheme.extendedColors.fg2, modifier = Modifier.padding(top = 8.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WorkflowStep.entries.forEachIndexed { index, step ->
                    val filled = index < filledSteps
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(
                                    color = if (filled) MaterialTheme.colorScheme.primary else CinemaTheme.extendedColors.hairline,
                                    shape = RoundedCornerShape(18.dp)
                                )
                        )
                        Text(
                            text = (step.ordinal + 1).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (filled) MaterialTheme.colorScheme.onSurface else CinemaTheme.extendedColors.fg3
                        )
                    }
                }
            }

            Surface(
                onClick = onResume,
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp).testTag(HOME_RESUME_BUTTON_TAG)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Text(
                        text = uiString("home.resume.button", language),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

private data class ResumeQuickTileSpec(
    val tag: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val meta: String,
    val onClick: () -> Unit
)

/**
 * ۴ کاشی طبق `homeTiles` mockup (AI Breakdown/DNA/Validation/Output). دو
 * مورد اول (AI Breakdown/DNA) دقیقاً مطابق mockup مسیر پروژه-محور دارند —
 * بدون نیاز به یک Shot مشخص. **تصمیم مستقل مستند:** دو مورد بعدی
 * (Validation/Output) در mockup به یک «شات جاری» فرضی می‌روند که این اپ
 * (برخلاف mockup) per-project ذخیره نمی‌کند — نزدیک‌ترین مقصد واقعی و
 * بدون‌خطا، ورود به Tab «صحنه‌ها»ی همان Studio (جایی که کاربر یک شات واقعی
 * انتخاب می‌کند) برای Validation، و Tab «خروجی» برای Output است.
 */
@Composable
private fun resumeQuickTiles(
    projectId: String,
    language: Language,
    onOpenAiBreakdown: (String) -> Unit,
    onOpenProjectTab: (String, String) -> Unit
): List<ResumeQuickTileSpec> = listOf(
    ResumeQuickTileSpec(
        tag = HOME_RESUME_TILE_BREAKDOWN_TAG,
        icon = Icons.Filled.AutoAwesome,
        label = uiString("home.quickTile.breakdownLabel", language),
        meta = uiString("home.quickTile.breakdownMeta", language),
        onClick = { onOpenAiBreakdown(projectId) }
    ),
    ResumeQuickTileSpec(
        tag = HOME_RESUME_TILE_DNA_TAG,
        icon = Icons.Filled.Science,
        label = uiString("studioTab.dna", language),
        meta = uiString("home.quickTile.dnaMeta", language),
        onClick = { onOpenProjectTab(projectId, "DNA") }
    ),
    ResumeQuickTileSpec(
        tag = HOME_RESUME_TILE_VALIDATION_TAG,
        icon = Icons.AutoMirrored.Filled.FactCheck,
        label = uiString("home.quickTile.validationLabel", language),
        meta = uiString("home.quickTile.validationMeta", language),
        onClick = { onOpenProjectTab(projectId, "SCENES") }
    ),
    ResumeQuickTileSpec(
        tag = HOME_RESUME_TILE_OUTPUT_TAG,
        icon = Icons.Filled.MovieFilter,
        label = uiString("studioTab.output", language),
        meta = uiString("home.quickTile.outputMeta", language),
        onClick = { onOpenProjectTab(projectId, "OUTPUT") }
    )
)

@Composable
private fun ResumeQuickTile(spec: ResumeQuickTileSpec) {
    Surface(
        onClick = spec.onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().testTag(spec.tag)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(spec.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(text = spec.label, style = MaterialTheme.typography.titleSmall)
                Text(text = spec.meta, style = MaterialTheme.typography.labelSmall, color = CinemaTheme.extendedColors.fg3)
            }
        }
    }
}

@Composable
private fun ResumeOtherProjectCard(summary: ProjectSummary, language: Language, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(160f / 96f)
                    .background(
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, CinemaTheme.extendedColors.orange))
                    )
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = summary.project.projectName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Text(
                    text = uiString(projectStateKey(summary.project.state), language),
                    style = MaterialTheme.typography.labelSmall,
                    color = CinemaTheme.extendedColors.fg3
                )
            }
        }
    }
}

private fun projectStateKey(state: com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState): String = when (state) {
    com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState.DRAFT -> "project.state.draft"
    com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState.REVIEW -> "project.state.review"
    com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState.LOCKED -> "project.state.locked"
    com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState.FINAL -> "project.state.final"
    com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState.ARCHIVED -> "project.state.archived"
}

/** برچسب کوتاه هر `WorkflowStep` — فقط برای زیرعنوان کارت RESUME. */
private fun workflowStepLabel(step: WorkflowStep, language: Language): String = uiString(
    when (step) {
        WorkflowStep.STORY_WIZARD -> "workflowStep.storyWizard"
        WorkflowStep.AI_STORY_BREAKDOWN -> "workflowStep.aiStoryBreakdown"
        WorkflowStep.DNA_CONFIG -> "workflowStep.dnaConfig"
        WorkflowStep.ASSET_LIBRARY -> "workflowStep.assetLibrary"
        WorkflowStep.SCENE_CREATION -> "workflowStep.sceneCreation"
        WorkflowStep.SHOT_CREATION -> "workflowStep.shotCreation"
        WorkflowStep.VALIDATION -> "workflowStep.validation"
        WorkflowStep.PROMPT_GENERATION -> "workflowStep.promptGeneration"
        WorkflowStep.OUTPUT_DELIVERY -> "workflowStep.outputDelivery"
    },
    language
)

/**
 * دیکود واقعی `content://` Uri به Bitmap — بدون افزودن هیچ کتابخانه‌ی تصویر تازه
 * (Coil/Glide، تأییدشده با grep که کل کدبیس فعلاً هیچ‌کدام را ندارد)؛ همان API
 * بومی Android (`ContentResolver`/`BitmapFactory`) که Import/Export پروژه هم از
 * قبل برای فایل استفاده می‌کنند. دیکود روی `Dispatchers.IO` (نه Composition خام)
 * تا از Jank فریم اول جلوگیری شود. اگر Uri دیگر معتبر نیست (مثلاً کاربر دسترسی
 * را لغو کرده)، `runCatching` آن را به `null` تبدیل می‌کند — هیچ‌چیز رندر
 * نمی‌شود، بدون Crash.
 */
@Composable
private fun HomeBackgroundImage(uriString: String, modifier: Modifier = Modifier) {
    DecodedContentImage(uriString = uriString, modifier = modifier.testTag(HOME_BACKGROUND_IMAGE_TAG), contentScale = ContentScale.Crop)
}

/**
 * یافته‌ی ۲ appendix ADR-081 (ADR-083): decode بومی `content://` URI به Bitmap —
 * استخراج‌شده از `HomeBackgroundImage` بالا تا `SettingsScreen.kt` (کاشی
 * پیش‌نمایش ۱۴۰px) هم بدون تکرار کد از همان منطق استفاده کند. `internal`
 * (نه `private`) دقیقاً به همان دلیل `CreateProjectDialog` در همین فایل.
 */
@Composable
internal fun DecodedContentImage(
    uriString: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uriString) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uriString))?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
        }
    }
    bitmap?.let { loaded ->
        Image(bitmap = loaded.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = contentScale)
    }
}

@Composable
private fun HomeHeader(
    language: Language,
    theme: AppTheme,
    onOpenDrawer: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag(HOME_OPEN_DRAWER_BUTTON_TAG)) {
            Icon(Icons.Filled.Menu, contentDescription = null)
        }

        // یافته‌ی واقعی بازبینی نهایی (واحد ۱۶ فاز ۶ قدم ۲، ADR-059): همان کلاس باگ
        // Low-opacity Tinted Fill — این پیل روی پس‌زمینه‌ی Hero/Blur خودِ Home رندر
        // می‌شود، دقیقاً همان سناریوی صریح هشدارداده‌شده در Implementation Notes سند
        // طراحی. رفع شد: پس‌زمینه‌ی Solid + `onPrimary` (توکن رسمی Material3، از قبل
        // برای این دقیقاً همین منظور تعریف‌شده).
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                Text("Cinema Studio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Row {
            IconButton(onClick = onToggleLanguage) {
                Icon(Icons.Filled.Translate, contentDescription = uiString("home.toggleLanguageButton", language))
            }
            IconButton(onClick = onToggleTheme) {
                Icon(
                    if (theme == AppTheme.DARK) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = uiString("home.toggleThemeButton", language)
                )
            }
        }
    }
}

@Composable
private fun QuickCreateRow(language: Language, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Column {
                Text(text = uiString("home.newProjectTitle", language), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = uiString("home.newProjectSubtitle", language),
                    style = MaterialTheme.typography.labelSmall,
                    color = CinemaTheme.extendedColors.fg3
                )
            }
        }
    }
}

/**
 * رفع یافته‌ی ۱ ادr-082 (appendix ADR-081): این دیالوگ قبلاً `private` بود و فقط
 * از داخل خودِ `HomeScreen` (ردیف Quick-create) قابل‌فراخوانی بود؛ اکنون از
 * `MainScaffold` هم برای FAB سراسری «ایجاد سریع» بازاستفاده می‌شود — بدون
 * تکرار کد، طبق همان دیالوگ و همان اعتبارسنجی (نام خالی غیرفعال).
 */
@Composable
fun CreateProjectDialog(language: Language, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiString("home.newProjectTitle", language)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier.testTag(CREATE_PROJECT_NAME_FIELD_TAG)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(uiString("project.rename.confirm", language))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiString("project.rename.cancel", language)) } }
    )
}
