package com.operaboys.cinemashotgenerator.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// واحد ۱۶ فاز ۱ — بخش ه: منوی همبرگری، طبق docs/design/README.md بخش Interactions:
// گروه‌بندی STUDIO/TOOLS/SYSTEM. تصمیم مستند (docs/adr/044-unit16-phase1-app-shell.md):
// چون بیشتر این مقصدها هنوز مسیر Navigation واقعی ندارند، فقط «دارایی‌ها» (که با
// نوار پایین هم یکی است) واقعاً Navigate می‌کند؛ بقیه یک پیام «به‌زودی» نشان
// می‌دهند (نه یک ظاهر غیرفعال/خاکستری) — چون حالت Disabled در تست دستی معمولاً به
// چشم «خراب» می‌آید، در حالی که یک Snackbar واضح می‌گوید این بخش هنوز ساخته
// نشده، نه اینکه از کار افتاده.

private data class DrawerLink(val labelKey: String, val onNavigateAssets: Boolean = false)

private val studioLinks = listOf(
    DrawerLink("drawer.storyWizard"),
    DrawerLink("drawer.aiBreakdown"),
    DrawerLink("drawer.dnaManager"),
    DrawerLink("drawer.scenes"),
    DrawerLink("drawer.shots"),
    DrawerLink("drawer.assets", onNavigateAssets = true)
)
private val toolsLinks = listOf(
    DrawerLink("drawer.validation"),
    DrawerLink("drawer.promptGenerator"),
    DrawerLink("drawer.outputDelivery")
)
private val systemLinks = listOf(
    DrawerLink("drawer.settings"),
    DrawerLink("drawer.backups")
)

@Composable
fun NavDrawerContent(
    language: Language,
    onNavigateAssets: () -> Unit,
    onComingSoon: () -> Unit
) {
    ModalDrawerSheet {
        DrawerGroup(titleKey = "drawer.groupStudio", links = studioLinks, language = language, onNavigateAssets = onNavigateAssets, onComingSoon = onComingSoon)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        DrawerGroup(titleKey = "drawer.groupTools", links = toolsLinks, language = language, onNavigateAssets = onNavigateAssets, onComingSoon = onComingSoon)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        DrawerGroup(titleKey = "drawer.groupSystem", links = systemLinks, language = language, onNavigateAssets = onNavigateAssets, onComingSoon = onComingSoon)
    }
}

@Composable
private fun DrawerGroup(
    titleKey: String,
    links: List<DrawerLink>,
    language: Language,
    onNavigateAssets: () -> Unit,
    onComingSoon: () -> Unit
) {
    Text(
        text = uiString(titleKey, language),
        style = MaterialTheme.typography.labelSmall,
        color = CinemaTheme.extendedColors.fg3,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
    links.forEach { link ->
        NavigationDrawerItem(
            label = { Text(uiString(link.labelKey, language)) },
            selected = false,
            onClick = if (link.onNavigateAssets) onNavigateAssets else onComingSoon,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
