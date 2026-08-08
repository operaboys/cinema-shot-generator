package com.operaboys.cinemashotgenerator.ui.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AutoSaveManager
import com.operaboys.cinemashotgenerator.data.repository.BackupFileStorage
import com.operaboys.cinemashotgenerator.data.repository.BackupKind
import com.operaboys.cinemashotgenerator.data.repository.BackupManager
import com.operaboys.cinemashotgenerator.data.repository.DeviceBackupFileStorage
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.dna.DnaTabContent
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.navigation.StudioTab
import com.operaboys.cinemashotgenerator.ui.navigation.StudioTopTabRow
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.scenes.ScenesListScreen
import com.operaboys.cinemashotgenerator.ui.story.StoryTabContent
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// واحد ۱۶ فاز ۱ — Studio Shell طبق دستور کار بخش ج: Header (برگشت، عنوان، زیرعنوان
// شمارش صحنه/شات، چیپ «ذخیره شد») + ۴ Tab (فاز ۰، اینجا واقعاً به محتوا وصل شدند).
// MIGRATED (واحد ۱۶ فاز ۲، قدم ۱): Tab «داستان» اکنون محتوای واقعی دارد
// (StoryTabContent)؛ بقیه (DNA/صحنه‌ها/خروجی) همچنان Placeholder («این بخش در فاز
// بعدی تکمیل می‌شود») — کار فازهای ۲ (ادامه) تا ۵.
//
// بازساختاردهی نسبت به فاز ۰ (مستند در ADR-044): StudioTopTabRow قبلاً در
// MainScaffold (بیرون از Navigation Graph) رندر می‌شد، بدون این‌که به محتوای واقعی
// وصل باشد (چون محتوایی نبود). حالا که این فاز باید هر Tab را به یک بدنه‌ی واقعی
// وصل کند، TopTabRow + State انتخاب Tab به داخل همین Composable (مسیر Studio
// واقعی) منتقل شدند — یک بازسازی ضروری، نه Scope Creep.
//
// طبق docs/design/README.md بخش State Management («Active project tab — نه در
// دسته‌ی Persisted»)، انتخاب Tab با rememberSaveable محلی نگه داشته می‌شود، نه در
// WorkflowViewModel/DataStore.

// واحد ۱۶ فاز ۶ — قدم ۲: لازم برای تست End-to-End صفحه‌ی Backups (برگشت از Studio
// به Home، تنها راه واقعی رسیدن به Drawer — StudioShell خودش دکمه‌ی همبرگری ندارد).
const val STUDIO_BACK_BUTTON_TAG = "studio.backButton"

@Composable
fun StudioShell(
    projectId: String,
    workflowViewModel: WorkflowViewModel,
    projectListViewModel: ProjectListViewModel,
    onBack: () -> Unit,
    onWarning: (String) -> Unit,
    storyRepository: StoryRepository? = null,
    projectDnaRepository: ProjectDnaRepository? = null,
    sceneRepository: SceneRepository? = null,
    autoSaveManager: AutoSaveManager? = null,
    backupFileStorage: BackupFileStorage? = null,
    database: AppDatabase? = null,
    onNavigateToAiBreakdown: (String) -> Unit = {},
    onNavigateToScene: (String) -> Unit = {}
) {
    val language by workflowViewModel.language.collectAsStateWithLifecycle()
    val workflowState by workflowViewModel.workflowState.collectAsStateWithLifecycle()
    val autoSaveCadenceSeconds by workflowViewModel.autoSaveCadenceSeconds.collectAsStateWithLifecycle()
    val summaries by projectListViewModel.projectSummaries.collectAsStateWithLifecycle()
    val summary = summaries.find { it.project.projectId == projectId }
    val application = LocalContext.current.applicationContext as Application
    // یافته‌ی واقعی این قدم (BackupsFlowTest شکست می‌خورد): اینجا قبلاً همیشه
    // AppDatabase.getInstance(application) را مستقیم صدا می‌زد — یک Singleton
    // سراسری جدا از database تزریقی که تست‌ها برای ProjectListViewModel/سایر
    // ViewModel ها می‌سازند (Room.inMemoryDatabaseBuilder). در تست، پروژه در
    // آن database تزریقی ساخته می‌شد اما BackupManager از Singleton سراسری
    // (خالی) DAO می‌گرفت، پس هرگز پروژه را پیدا نمی‌کرد. رفع شد: database اکنون
    // پارامتر تزریقی است (هم‌الگو با autoSaveManager/backupFileStorage)، فقط
    // در نبود آن (مسیر واقعی اپ) به Singleton برمی‌گردد.
    val backupManager = remember(projectId, backupFileStorage, database) {
        val resolvedDatabase = database ?: AppDatabase.getInstance(application)
        BackupManager(
            projectId = projectId,
            projectDao = resolvedDatabase.projectDao(),
            sceneDao = resolvedDatabase.sceneDao(),
            shotDao = resolvedDatabase.shotDao(),
            assetDao = resolvedDatabase.assetDao(),
            projectDnaDao = resolvedDatabase.projectDnaDao(),
            audioContextDao = resolvedDatabase.audioContextDao(),
            backupFileStorage = backupFileStorage ?: DeviceBackupFileStorage(application)
        )
    }

    // اتصال واقعی startWorkflowSession (که در فاز ۰ ساخته شد ولی هیچ فراخوان
    // واقعی‌ای نداشت — ADR-042: «فراخوان واقعی این تابع هنگام ورود به Studio کار
    // فاز بعدی است») — دقیقاً همین لحظه. فقط وقتی Session فعلی برای پروژه‌ی دیگری
    // است (یا اصلاً وجود ندارد) دوباره ساخته می‌شود، نه در هر Recomposition.
    LaunchedEffect(projectId) {
        if (workflowState?.projectId != projectId) {
            workflowViewModel.startWorkflowSession(projectId)
        }
    }

    // واحد ۱۶ فاز ۶ — قدم ۱: اتصال واقعی Timer دوره‌ای Auto-Save (طبق تصمیم مستند
    // docs/adr/058-unit16-phase6-step1-settings-autosave.md) — ذخیره‌ی واقعی
    // فیلدهای هر Entity از قبل بلافاصله روی هر تغییر انجام می‌شود (بدون تغییر در
    // این قدم)؛ این حلقه فقط `ProjectEntity.lastModified` را هر Cadence (از
    // Settings) تازه نگه می‌دارد، دقیقاً تا وقتی این Composable برای این
    // projectId روی صفحه است (لغو خودکار با ترک/تغییر Composition، هم‌الگو با
    // LaunchedEffect بالا — بدون نیاز به مدیریت دستی Job).
    LaunchedEffect(projectId, autoSaveCadenceSeconds, autoSaveManager) {
        if (autoSaveManager == null) return@LaunchedEffect
        while (isActive) {
            delay(autoSaveCadenceSeconds * 1000L)
            autoSaveManager.touch(projectId)
        }
    }

    // واحد ۱۶ فاز ۶ — قدم ۲ (آخرین قدم کل واحد ۱۶، ADR-059): یافته‌ی صریح بازبینی
    // نهایی (بخش ب-۶ دستور کار این قدم) — backupIntervalMinutes از واحد ۱۵ تا این
    // لحظه هرگز واقعاً مصرف نشده بود (grep تأییدشده). هم‌الگو دقیق با Timer بالا:
    // یک بکاپ خودکار (BackupKind.AUTO، متمایز از بکاپ‌های دستی صفحه‌ی Backups)
    // هر backupIntervalMinutes، تا وقتی این Composable برای این projectId فعال
    // است.
    LaunchedEffect(projectId, backupManager) {
        while (isActive) {
            delay(backupManager.backupIntervalMinutes * 60_000L)
            backupManager.createBackup(BackupKind.AUTO)
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf(StudioTab.STORY) }

    Column(modifier = Modifier.fillMaxSize()) {
        StudioHeader(
            title = summary?.project?.projectName ?: projectId,
            sceneCount = summary?.sceneCount ?: 0,
            shotCount = summary?.shotCount ?: 0,
            language = language,
            onBack = onBack
        )
        StudioTopTabRow(
            selectedTab = selectedTab,
            workflowState = workflowState,
            language = language,
            onTabSelected = { selectedTab = it },
            onWarning = onWarning
        )
        when (selectedTab) {
            StudioTab.STORY -> StoryTabContent(
                projectId = projectId,
                projectListViewModel = projectListViewModel,
                language = language,
                storyRepository = storyRepository,
                onNavigateToAiBreakdown = { onNavigateToAiBreakdown(projectId) },
                modifier = Modifier.fillMaxSize()
            )
            StudioTab.DNA -> DnaTabContent(
                projectId = projectId,
                language = language,
                projectDnaRepository = projectDnaRepository,
                dependentShotsCount = summary?.shotCount ?: 0,
                onShowMessage = onWarning,
                modifier = Modifier.fillMaxSize()
            )
            StudioTab.SCENES -> ScenesListScreen(
                projectId = projectId,
                language = language,
                onOpenScene = onNavigateToScene,
                sceneRepository = sceneRepository,
                modifier = Modifier.fillMaxSize()
            )
            else -> StudioTabPlaceholder(language = language, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun StudioHeader(title: String, sceneCount: Int, shotCount: Int, language: Language, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag(STUDIO_BACK_BUTTON_TAG)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = uiTemplate("project.metaTemplate", language, "scenes" to sceneCount.toString(), "shots" to shotCount.toString()),
                style = MaterialTheme.typography.labelSmall,
                color = CinemaTheme.extendedColors.fg3
            )
        }
        // یافته‌ی واقعی بازبینی نهایی (واحد ۱۶ فاز ۶ قدم ۲، ADR-059): همان کلاس باگ
        // Low-opacity Tinted Fill. رفع شد؛ متن سیاه ثابت چون Success (0xFF3DDC97)
        // به‌اندازه‌ی کافی روشن است — دقیقاً همان استدلال مستندشده‌ی ADR-055.
        Surface(shape = RoundedCornerShape(18.dp), color = CinemaTheme.extendedColors.success) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.padding(0.dp))
                Text(text = uiString("studio.saved", language), style = MaterialTheme.typography.labelSmall, color = Color.Black)
            }
        }
    }
}

@Composable
private fun StudioTabPlaceholder(language: Language, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = uiString("studio.tabPlaceholder", language),
            style = MaterialTheme.typography.bodyLarge,
            color = CinemaTheme.extendedColors.fg3
        )
    }
}
