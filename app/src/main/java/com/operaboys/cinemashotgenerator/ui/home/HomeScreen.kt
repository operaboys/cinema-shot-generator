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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
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
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
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

@Composable
fun HomeScreen(
    workflowViewModel: WorkflowViewModel,
    projectListViewModel: ProjectListViewModel,
    onOpenDrawer: () -> Unit,
    onOpenProject: (String) -> Unit,
    onViewAllProjects: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val language by workflowViewModel.language.collectAsStateWithLifecycle()
    val theme by workflowViewModel.theme.collectAsStateWithLifecycle()
    val summaries by projectListViewModel.projectSummaries.collectAsStateWithLifecycle()
    val lastActionMessage by projectListViewModel.lastActionMessage.collectAsStateWithLifecycle()
    val homeScreenImageUri by workflowViewModel.homeScreenImageUri.collectAsStateWithLifecycle()

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

            Column(
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
        Image(
            bitmap = loaded.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.testTag(HOME_BACKGROUND_IMAGE_TAG),
            contentScale = ContentScale.Crop
        )
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

@Composable
private fun CreateProjectDialog(language: Language, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
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
