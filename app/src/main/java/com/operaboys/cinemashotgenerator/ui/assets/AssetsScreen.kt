package com.operaboys.cinemashotgenerator.ui.assets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel

// واحد ۱۶ فاز ۱ — بخش د: Assets Container — فقط Placeholder («در فاز ۳ تکمیل
// می‌شود»)؛ طبق پلن اجرایی، فرم‌های واقعی Character/Location/Object در فاز ۳ ساخته
// می‌شوند.

@Composable
fun AssetsScreen(workflowViewModel: WorkflowViewModel) {
    val language by workflowViewModel.language.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = uiString("assets.placeholder", language),
            style = MaterialTheme.typography.bodyLarge,
            color = CinemaTheme.extendedColors.fg3
        )
    }
}
