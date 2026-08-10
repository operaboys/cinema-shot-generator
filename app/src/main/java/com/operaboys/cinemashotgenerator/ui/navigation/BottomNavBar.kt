package com.operaboys.cinemashotgenerator.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.i18n.uiString

// واحد ۱۶ — فاز ۰: نوار پایین سطح اپ — طبق DDR-002/بلوپرینت ۱۶ نسخه ۷: «همیشه و بدون
// استثنا روی هر صفحه‌ی ریشه نمایش داده می‌شود، هرگز شرطی/قابل‌خاموش‌شدن نیست» (تصحیح
// صریح docs/design/README.md: «Navigation structure is fixed, not an A/B variant»).
// ۴ آیتم + FAB مرکزی: Home · Projects · [FAB] · Studio · Assets.
//
// آیکون‌ها فعلاً از androidx.compose.material.icons.Icons.Filled هستند (مجموعه‌ی
// پایه‌ی Material Icons)، نه دقیقاً «Material Symbols Rounded» سند طراحی — تطبیق
// دقیق آیکون Google Symbols کار فاز ساخت واقعی صفحات (Visual Polish) است، نه این
// فاز ساختاری (همان محدودیت مستند فونت در Type.kt).
//
// MIGRATED (رفع G6 ممیزی post-Unit16، docs/audit/post-unit16-full-audit.md،
// docs/adr/062-...): `PLACEHOLDER_ACTIVE_PROJECT_ID` (رشته‌ی جعلی ثابت که این
// دکمه قبلاً همیشه به آن Navigate می‌کرد، مستقل از این‌که کاربر واقعاً کدام
// پروژه را باز کرده بود) کاملاً حذف شد. مقصد واقعی این دکمه اکنون توسط
// `onNavigateStudio` (محاسبه‌شده در MainScaffold.kt با
// `resolveActiveOrRecentProjectId` — ActiveProject.kt) تعیین می‌شود، نه یک
// رشته‌ی هاردکد اینجا.

// یافته‌ی فاز ۱ (docs/adr/044-...md): ModalNavigationDrawer محتوای drawerContent را
// همیشه در درخت Composition نگه می‌دارد (حتی وقتی بسته است، فقط بیرون از دید
// Translate می‌شود) — یعنی متن آیتم‌های Drawer (مثل «استودیو»/«دارایی‌ها» که دقیقاً
// همان برچسب این نوار پایین‌اند) هم‌زمان با این نوار در Semantics Tree حاضرند.
// جستجوی صرفاً متنی (onNodeWithText) در تست برای چنین برچسب‌های تکراری‌ای Ambiguous
// می‌شود («۲ گره یافت شد»). راه‌حل استاندارد Compose برای این کلاس مشکل، testTag
// است، نه تغییر محتوای ترجمه فقط برای فرار از تصادف تست.
const val BOTTOM_NAV_HOME_TAG = "bottomNav.home"
const val BOTTOM_NAV_PROJECTS_TAG = "bottomNav.projects"
const val BOTTOM_NAV_STUDIO_TAG = "bottomNav.studio"
const val BOTTOM_NAV_ASSETS_TAG = "bottomNav.assets"

@Composable
fun AppBottomNavBar(
    currentDestination: NavDestination?,
    language: Language,
    onNavigate: (Any) -> Unit,
    onNavigateStudio: () -> Unit,
    onQuickCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        NavigationBar {
            NavigationBarItem(
                selected = currentDestination.matches<Home>(),
                onClick = { onNavigate(Home) },
                icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                label = { Text(uiString("nav.home", language)) },
                modifier = Modifier.testTag(BOTTOM_NAV_HOME_TAG)
            )
            NavigationBarItem(
                selected = currentDestination.matches<Projects>(),
                onClick = { onNavigate(Projects) },
                icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                label = { Text(uiString("nav.projects", language)) },
                modifier = Modifier.testTag(BOTTOM_NAV_PROJECTS_TAG)
            )
            // جای خالی زیر FAB مرکزی — خودِ FAB به‌صورت یک لایه‌ی جدا روی این نوار قرار می‌گیرد.
            NavigationBarItem(
                selected = false,
                onClick = onQuickCreate,
                icon = { },
                label = { },
                enabled = false
            )
            NavigationBarItem(
                selected = currentDestination.matches<Studio>(),
                onClick = onNavigateStudio,
                icon = { Icon(Icons.Filled.Movie, contentDescription = null) },
                label = { Text(uiString("nav.studio", language)) },
                modifier = Modifier.testTag(BOTTOM_NAV_STUDIO_TAG)
            )
            NavigationBarItem(
                selected = currentDestination.matches<Assets>(),
                onClick = { onNavigate(Assets) },
                icon = { Icon(Icons.Filled.Apps, contentDescription = null) },
                label = { Text(uiString("nav.assets", language)) },
                modifier = Modifier.testTag(BOTTOM_NAV_ASSETS_TAG)
            )
        }
        FloatingActionButton(
            onClick = onQuickCreate,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-28).dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = uiString("nav.quickCreate", language))
        }
    }
}

private inline fun <reified T : Any> NavDestination?.matches(): Boolean {
    return this?.hierarchy?.any { it.hasRoute<T>() } == true
}
