package com.operaboys.cinemashotgenerator.ui.backups

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.BackupFileStorage
import com.operaboys.cinemashotgenerator.data.repository.BackupKind
import com.operaboys.cinemashotgenerator.data.repository.BackupSummary
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.assets.AssetFormHeader
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۶ — قدم ۲ (آخرین قدم کل واحد ۱۶): صفحه‌ی Backups. طبق
// docs/design/README.md بخش «۱۲. Backups»: فهرست فایل‌های بکاپ (نام/حجم/نوع/سن) +
// دکمه‌ی «ساخت بکاپ دستی» + بازیابی/حذف روی هر ردیف. چون BackupManager per-project
// است، این صفحه فقط برای یک Session فعال Studio معنا دارد (طبق workflowState.
// projectId) — بدون آن یک پیام واضح نشان می‌دهد، نه خطا. جزئیات کامل تصمیمات در
// docs/adr/059-unit16-phase6-step2-backups-final-review.md.

const val BACKUPS_BACK_BUTTON_TAG = "backups.backButton"
const val BACKUPS_CREATE_BUTTON_TAG = "backups.createButton"

fun backupRestoreButtonTag(backupId: String): String = "backups.restoreButton.$backupId"
fun backupDeleteButtonTag(backupId: String): String = "backups.deleteButton.$backupId"
fun backupRowTag(backupId: String): String = "backups.row.$backupId"

@Composable
fun BackupsScreen(
    projectId: String?,
    projectName: String,
    language: Language,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit = {},
    backupFileStorage: BackupFileStorage? = null,
    database: AppDatabase? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AssetFormHeader(
            title = uiString("drawer.backups", language),
            subtitle = uiString("backups.subtitle", language),
            onBack = onBack,
            backTestTag = BACKUPS_BACK_BUTTON_TAG
        )

        if (projectId == null) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    text = uiString("backups.noActiveProjectState", language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CinemaTheme.extendedColors.fg3
                )
            }
            return
        }

        val application = LocalContext.current.applicationContext as Application
        val viewModel: BackupsViewModel = viewModel(
            factory = BackupsViewModel.factory(application, projectId, backupFileStorage, database)
        )
        val backups by viewModel.backups.collectAsStateWithLifecycle()

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    viewModel.createManualBackup { success ->
                        if (success) onShowMessage(uiString("backups.createdMessage", language))
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag(BACKUPS_CREATE_BUTTON_TAG)
            ) {
                Text(uiString("backups.createManualButton", language))
            }

            if (backups.isEmpty()) {
                Text(
                    text = uiString("backups.emptyState", language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CinemaTheme.extendedColors.fg3
                )
            } else {
                backups.forEach { backup ->
                    BackupRow(
                        backup = backup,
                        projectName = projectName,
                        language = language,
                        onRestore = {
                            viewModel.restore(backup.backupId) { success ->
                                onShowMessage(
                                    uiString(if (success) "backups.restoredMessage" else "backups.restoreFailedMessage", language)
                                )
                            }
                        },
                        onDelete = {
                            viewModel.delete(backup.backupId)
                            onShowMessage(uiString("backups.deletedMessage", language))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupRow(
    backup: BackupSummary,
    projectName: String,
    language: Language,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag(backupRowTag(backup.backupId))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = displayBackupFileName(projectName, backup.createdAt),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                BackupKindBadge(backup.kind, language)
            }
            Text(
                text = "${formatBackupSize(backup.sizeBytes)} · ${formatBackupAge(backup.createdAt, language)}",
                style = MaterialTheme.typography.labelSmall,
                color = CinemaTheme.extendedColors.fg3
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRestore, modifier = Modifier.testTag(backupRestoreButtonTag(backup.backupId))) {
                    Text(uiString("backups.restoreButton", language))
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.testTag(backupDeleteButtonTag(backup.backupId))) {
                    Text(uiString("backups.deleteButton", language))
                }
            }
        }
    }
}

/**
 * طبق الزام صریح دستور کار این قدم («هر Chip/Badge نوع auto/manual باید Opaque با
 * حاشیه‌ی واضح باشد») — پس‌زمینه‌ی Solid + حاشیه، نه Tint کم‌رنگ (همان درسِ
 * ADR-055/ADR-059 که در این قدم چند جای دیگر هم رفع شد).
 */
@Composable
private fun BackupKindBadge(kind: BackupKind, language: Language) {
    val color = if (kind == BackupKind.MANUAL) MaterialTheme.colorScheme.primary else CinemaTheme.extendedColors.fg4
    val textColor = if (kind == BackupKind.MANUAL) MaterialTheme.colorScheme.onPrimary else Color.Black
    val labelKey = if (kind == BackupKind.MANUAL) "backups.kindManual" else "backups.kindAuto"
    Surface(shape = RoundedCornerShape(14.dp), color = color, border = BorderStroke(2.dp, color)) {
        Text(
            text = uiString(labelKey, language),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
