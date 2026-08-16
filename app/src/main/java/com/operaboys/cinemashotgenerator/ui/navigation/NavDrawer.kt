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
//
// رفع G1 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، docs/adr/061-...):
// از ۸ لینک COMING_SOON اولیه، ۷ تا اکنون واقعاً Navigate می‌کنند — مسیرهای
// واقعی‌شان از فاز‌های ۲ تا ۵ همین واحد از قبل وجود داشتند، فقط Drawer هرگز به‌روز
// نشده بود. «تولید پرامپت» (drawer.promptGenerator) طبق ADR-056 در «تحویل خروجی»
// ادغام شد و عمداً بدون تغییر COMING_SOON باقی می‌ماند — برچسبش («تولید پرامپت»)
// گمراه‌کننده نیست (هنوز مفهوم درستی را توصیف می‌کند)، پس دست‌نخورده ماند.
// STUDIO_STORY/STUDIO_DNA/STUDIO_SCENES هر سه به همان Studio(projectId, initialTab)
// می‌روند (فقط Tab متفاوت) — به همین دلیل یک callback واحد پارامتردار
// (onNavigateStudio) به‌جای ۳ callback جدا برایشان کافی است.
private enum class DrawerAction { COMING_SOON, ASSETS, SETTINGS, BACKUPS, STUDIO_STORY, STUDIO_DNA, STUDIO_SCENES, AI_BREAKDOWN, VALIDATION, OUTPUT_DELIVERY }

private data class DrawerLink(val labelKey: String, val action: DrawerAction = DrawerAction.COMING_SOON)

private val studioLinks = listOf(
    DrawerLink("drawer.storyWizard", action = DrawerAction.STUDIO_STORY),
    DrawerLink("drawer.aiBreakdown", action = DrawerAction.AI_BREAKDOWN),
    DrawerLink("drawer.dnaManager", action = DrawerAction.STUDIO_DNA),
    DrawerLink("drawer.scenes", action = DrawerAction.STUDIO_SCENES),
    // «شات‌ها» Tab مستقل خودش را در StudioTab ندارد (فقط STORY/DNA/SCENES/OUTPUT) —
    // شات‌ها زیرمجموعه‌ی SceneDetail.SHOTS هستند که فقط بعد از انتخاب یک صحنه‌ی
    // مشخص در دسترس است؛ چون Drawer هیچ صحنه‌ای نمی‌داند، مقصد واقع‌بینانه همان
    // Tab «صحنه‌ها» است (کاربر از آنجا صحنه را انتخاب می‌کند).
    DrawerLink("drawer.shots", action = DrawerAction.STUDIO_SCENES),
    DrawerLink("drawer.assets", action = DrawerAction.ASSETS)
)
private val toolsLinks = listOf(
    // «اعتبارسنجی»/«تحویل خروجی» هر دو به یک shotId مشخص نیاز دارند
    // (AppDestinations.kt: Validation/OutputDelivery غیر-nullable) که Drawer
    // نمی‌داند؛ مقصد Fallback مستند (docs/adr/061-...) همان Tab «صحنه‌ها» است —
    // دقیقاً هم‌مقصد با «شات‌ها» بالا.
    // رفع یافته‌ی #۱۰ appendix (ADR-088): این Fallback اکنون یک استثنا دارد —
    // وقتی پروژه‌ی جاری دقیقاً یک Shot دارد، منطق واقعی (در MainScaffold.kt، جایی
    // که Repository در دسترس است) به‌جای Fallback مستقیماً همان Shot را باز
    // می‌کند؛ Drawer خودش هنوز هیچ shotId ای نمی‌داند، فقط دو callback تازه
    // (onNavigateValidation/onNavigateOutputDelivery) را صدا می‌زند و تصمیم را به
    // فراخوان واگذار می‌کند.
    DrawerLink("drawer.validation", action = DrawerAction.VALIDATION),
    DrawerLink("drawer.promptGenerator"),
    DrawerLink("drawer.outputDelivery", action = DrawerAction.OUTPUT_DELIVERY)
)
// واحد ۱۶ فاز ۶ — قدم ۱/۲: «تنظیمات» و «بکاپ‌ها» اکنون هر دو واقعاً Navigate
// می‌کنند (نه دیگر «به‌زودی») — هم‌الگو دقیق با «دارایی‌ها».
private val systemLinks = listOf(
    DrawerLink("drawer.settings", action = DrawerAction.SETTINGS),
    DrawerLink("drawer.backups", action = DrawerAction.BACKUPS)
)

@Composable
fun NavDrawerContent(
    language: Language,
    onNavigateAssets: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateBackups: () -> Unit,
    // رفع G1: `initialTab` رشته‌ی نام StudioTab است ("STORY"/"DNA"/"SCENES") —
    // هم‌الگو با Studio.initialTab خودش.
    onNavigateStudio: (initialTab: String) -> Unit,
    onNavigateAiBreakdown: () -> Unit,
    onNavigateValidation: () -> Unit,
    onNavigateOutputDelivery: () -> Unit,
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
            DrawerGroup(titleKey = "drawer.groupStudio", links = studioLinks, language = language, onNavigateAssets = onNavigateAssets, onNavigateSettings = onNavigateSettings, onNavigateBackups = onNavigateBackups, onNavigateStudio = onNavigateStudio, onNavigateAiBreakdown = onNavigateAiBreakdown, onNavigateValidation = onNavigateValidation, onNavigateOutputDelivery = onNavigateOutputDelivery, onComingSoon = onComingSoon)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DrawerGroup(titleKey = "drawer.groupTools", links = toolsLinks, language = language, onNavigateAssets = onNavigateAssets, onNavigateSettings = onNavigateSettings, onNavigateBackups = onNavigateBackups, onNavigateStudio = onNavigateStudio, onNavigateAiBreakdown = onNavigateAiBreakdown, onNavigateValidation = onNavigateValidation, onNavigateOutputDelivery = onNavigateOutputDelivery, onComingSoon = onComingSoon)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DrawerGroup(titleKey = "drawer.groupSystem", links = systemLinks, language = language, onNavigateAssets = onNavigateAssets, onNavigateSettings = onNavigateSettings, onNavigateBackups = onNavigateBackups, onNavigateStudio = onNavigateStudio, onNavigateAiBreakdown = onNavigateAiBreakdown, onNavigateValidation = onNavigateValidation, onNavigateOutputDelivery = onNavigateOutputDelivery, onComingSoon = onComingSoon)
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
    onNavigateBackups: () -> Unit,
    onNavigateStudio: (String) -> Unit,
    onNavigateAiBreakdown: () -> Unit,
    onNavigateValidation: () -> Unit,
    onNavigateOutputDelivery: () -> Unit,
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
                DrawerAction.BACKUPS -> onNavigateBackups
                DrawerAction.STUDIO_STORY -> { { onNavigateStudio("STORY") } }
                DrawerAction.STUDIO_DNA -> { { onNavigateStudio("DNA") } }
                DrawerAction.STUDIO_SCENES -> { { onNavigateStudio("SCENES") } }
                DrawerAction.AI_BREAKDOWN -> onNavigateAiBreakdown
                DrawerAction.VALIDATION -> onNavigateValidation
                DrawerAction.OUTPUT_DELIVERY -> onNavigateOutputDelivery
                DrawerAction.COMING_SOON -> onComingSoon
            },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
