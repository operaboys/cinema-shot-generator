package com.operaboys.cinemashotgenerator.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

private enum class DrawerAction { COMING_SOON, ASSETS, SETTINGS }

private data class DrawerLink(val labelKey: String, val action: DrawerAction = DrawerAction.COMING_SOON)

private val studioLinks = listOf(
    DrawerLink("drawer.storyWizard"),
    DrawerLink("drawer.aiBreakdown"),
    DrawerLink("drawer.dnaManager"),
    DrawerLink("drawer.scenes"),
    DrawerLink("drawer.shots"),
    DrawerLink("drawer.assets", action = DrawerAction.ASSETS)
)
private val toolsLinks = listOf(
    DrawerLink("drawer.validation"),
    DrawerLink("drawer.promptGenerator"),
    DrawerLink("drawer.outputDelivery")
)
// واحد ۱۶ فاز ۶ — قدم ۱: «تنظیمات» اکنون واقعاً Navigate می‌کند (نه دیگر «به‌زودی»)
// — دومین لینک این Drawer که مسیر واقعی پیدا کرد، هم‌الگو دقیق با «دارایی‌ها».
private val systemLinks = listOf(
    DrawerLink("drawer.settings", action = DrawerAction.SETTINGS),
    DrawerLink("drawer.backups")
)

@Composable
fun NavDrawerContent(
    language: Language,
    onNavigateAssets: () -> Unit,
    onNavigateSettings: () -> Unit,
    onComingSoon: () -> Unit
) {
    // واحد ۱۶ فاز ۶ — قدم ۱: یافته‌ی واقعی این قدم — با ۱۱ آیتم در ۳ گروه، محتوای
    // Drawer می‌تواند از ارتفاع صفحه‌های واقعی/متوسط بیشتر شود؛ ModalDrawerSheet
    // به‌خودی‌خود محتوایش را Scroll نمی‌کند (فقط یک Surface/Column ساده است) — بدون
    // این Modifier.verticalScroll، آیتم‌های گروه SYSTEM (تنظیمات/بکاپ‌ها) در
    // صفحه‌های کوچک‌تر عملاً غیرقابل‌دسترس بودند. این یک باگ واقعی UX بود، نه فقط
    // یافته‌ی تست — با پیدایش در تست End-to-End صفحه‌ی Settings (SettingsFlowTest.kt).
    ModalDrawerSheet {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            DrawerGroup(titleKey = "drawer.groupStudio", links = studioLinks, language = language, onNavigateAssets = onNavigateAssets, onNavigateSettings = onNavigateSettings, onComingSoon = onComingSoon)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DrawerGroup(titleKey = "drawer.groupTools", links = toolsLinks, language = language, onNavigateAssets = onNavigateAssets, onNavigateSettings = onNavigateSettings, onComingSoon = onComingSoon)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DrawerGroup(titleKey = "drawer.groupSystem", links = systemLinks, language = language, onNavigateAssets = onNavigateAssets, onNavigateSettings = onNavigateSettings, onComingSoon = onComingSoon)
        }
    }
}

@Composable
private fun DrawerGroup(
    titleKey: String,
    links: List<DrawerLink>,
    language: Language,
    onNavigateAssets: () -> Unit,
    onNavigateSettings: () -> Unit,
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
            onClick = when (link.action) {
                DrawerAction.ASSETS -> onNavigateAssets
                DrawerAction.SETTINGS -> onNavigateSettings
                DrawerAction.COMING_SOON -> onComingSoon
            },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
