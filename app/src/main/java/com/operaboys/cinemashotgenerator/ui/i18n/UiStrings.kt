package com.operaboys.cinemashotgenerator.ui.i18n

import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.outputdelivery.t

// واحد ۱۶ — فاز ۰: اولین مصرف‌کننده‌ی واقعی domain.outputdelivery.t()/Language (واحد
// ۱۴، Bilingual System) — قبل از این فاز هیچ Composable ای این تابع را صدا نمی‌زد
// (تأییدشده با grep). این فایل فقط کلیدهای Navigation/Tab لازم برای فاز ۰ را دارد؛
// فازهای بعدی کلیدهای صفحه‌های خودشان را به همین دو Map اضافه می‌کنند (نه Map جدا).

val faStrings: Map<String, String> = mapOf(
    "nav.home" to "خانه",
    "nav.projects" to "پروژه‌ها",
    "nav.studio" to "استودیو",
    "nav.assets" to "دارایی‌ها",
    "nav.quickCreate" to "ایجاد سریع",
    "studioTab.story" to "داستان",
    "studioTab.dna" to "DNA",
    "studioTab.scenes" to "صحنه‌ها",
    "studioTab.output" to "خروجی",

    // فاز ۱ — Home
    "home.greetingTitle" to "وقت بخیر، Creator",
    "home.greetingSubtitle" to "بیایید چیزی سینمایی بسازیم.",
    "home.newProjectTitle" to "پروژه‌ی جدید",
    "home.newProjectSubtitle" to "شروع یک پروژه‌ی تازه",
    "home.recentProjects" to "پروژه‌های اخیر",
    "home.all" to "همه",
    "home.emptyState" to "هنوز پروژه‌ای نساخته‌اید — با «پروژه‌ی جدید» شروع کنید.",

    // فاز ۱ — Projects
    "projects.title" to "پروژه‌ها",
    "projects.subtitleTemplate" to "{count} پروژه‌ی محلی",

    // فاز ۱ — کارت پروژه
    "project.state.draft" to "پیش‌نویس",
    "project.state.review" to "بازبینی",
    "project.state.locked" to "قفل‌شده",
    "project.state.final" to "نهایی",
    "project.state.archived" to "بایگانی‌شده",
    "project.metaTemplate" to "{scenes} صحنه · {shots} شات",

    // فاز ۱ — منوی Overflow کارت پروژه
    "project.overflowMenu" to "گزینه‌های بیشتر",
    "project.overflow.rename" to "تغییر نام",
    "project.overflow.duplicate" to "تکثیر",
    "project.overflow.archive" to "آرشیو",
    "project.overflow.export" to "خروجی گرفتن",
    "project.overflow.delete" to "حذف",
    "project.rename.title" to "تغییر نام پروژه",
    "project.rename.confirm" to "ذخیره",
    "project.rename.cancel" to "انصراف",
    "project.delete.title" to "حذف پروژه",
    "project.delete.message" to "این پروژه و تمام صحنه‌ها/شات‌های آن برای همیشه حذف می‌شوند. این عمل قابل بازگشت نیست.",
    "project.delete.confirm" to "حذف کن",
    "project.delete.cancel" to "انصراف",

    // فاز ۱ — Studio Shell
    "studio.saved" to "ذخیره شد",
    "studio.tabPlaceholder" to "این بخش در فاز بعدی تکمیل می‌شود",

    // فاز ۱ — Assets Container
    "assets.placeholder" to "در فاز ۳ تکمیل می‌شود",

    // فاز ۱ — منوی همبرگری (Nav Drawer)
    "drawer.groupStudio" to "استودیو",
    "drawer.groupTools" to "ابزارها",
    "drawer.groupSystem" to "سیستم",
    "drawer.storyWizard" to "دستیار داستان",
    "drawer.aiBreakdown" to "تفکیک داستان با AI",
    "drawer.dnaManager" to "مدیریت DNA",
    "drawer.scenes" to "صحنه‌ها",
    "drawer.shots" to "شات‌ها",
    "drawer.assets" to "دارایی‌ها",
    "drawer.validation" to "اعتبارسنجی",
    "drawer.promptGenerator" to "تولید پرامپت",
    "drawer.outputDelivery" to "تحویل خروجی",
    "drawer.settings" to "تنظیمات",
    "drawer.backups" to "بکاپ‌ها",
    "drawer.comingSoon" to "این بخش به‌زودی در دسترس خواهد بود"
)

val enStrings: Map<String, String> = mapOf(
    "nav.home" to "Home",
    "nav.projects" to "Projects",
    "nav.studio" to "Studio",
    "nav.assets" to "Assets",
    "nav.quickCreate" to "Quick Create",
    "studioTab.story" to "Story",
    "studioTab.dna" to "DNA",
    "studioTab.scenes" to "Scenes",
    "studioTab.output" to "Output",

    // Phase 1 — Home
    "home.greetingTitle" to "Hello, Creator",
    "home.greetingSubtitle" to "Let's create something cinematic.",
    "home.newProjectTitle" to "New Project",
    "home.newProjectSubtitle" to "Start a new project",
    "home.recentProjects" to "Recent Projects",
    "home.all" to "All",
    "home.emptyState" to "No projects yet — start with \"New Project\".",

    // Phase 1 — Projects
    "projects.title" to "Projects",
    "projects.subtitleTemplate" to "{count} local projects",

    // Phase 1 — Project card
    "project.state.draft" to "Draft",
    "project.state.review" to "Review",
    "project.state.locked" to "Locked",
    "project.state.final" to "Final",
    "project.state.archived" to "Archived",
    "project.metaTemplate" to "{scenes} scenes · {shots} shots",

    // Phase 1 — project card overflow menu
    "project.overflowMenu" to "More options",
    "project.overflow.rename" to "Rename",
    "project.overflow.duplicate" to "Duplicate",
    "project.overflow.archive" to "Archive",
    "project.overflow.export" to "Export",
    "project.overflow.delete" to "Delete",
    "project.rename.title" to "Rename project",
    "project.rename.confirm" to "Save",
    "project.rename.cancel" to "Cancel",
    "project.delete.title" to "Delete project",
    "project.delete.message" to "This project and all of its scenes/shots will be permanently deleted. This cannot be undone.",
    "project.delete.confirm" to "Delete",
    "project.delete.cancel" to "Cancel",

    // Phase 1 — Studio Shell
    "studio.saved" to "Saved",
    "studio.tabPlaceholder" to "This section will be completed in a later phase",

    // Phase 1 — Assets Container
    "assets.placeholder" to "Completed in Phase 3",

    // Phase 1 — Nav Drawer
    "drawer.groupStudio" to "STUDIO",
    "drawer.groupTools" to "TOOLS",
    "drawer.groupSystem" to "SYSTEM",
    "drawer.storyWizard" to "Story Wizard",
    "drawer.aiBreakdown" to "AI Story Breakdown",
    "drawer.dnaManager" to "DNA Manager",
    "drawer.scenes" to "Scenes",
    "drawer.shots" to "Shots",
    "drawer.assets" to "Assets",
    "drawer.validation" to "Validation",
    "drawer.promptGenerator" to "Prompt Generator",
    "drawer.outputDelivery" to "Output Delivery",
    "drawer.settings" to "Settings",
    "drawer.backups" to "Backups",
    "drawer.comingSoon" to "This section is coming soon"
)

val uiTranslations: Map<Language, Map<String, String>> = mapOf(
    Language.FA to faStrings,
    Language.EN to enStrings
)

/** میان‌بر مختصر روی domain.outputdelivery.t() برای این پکیج، تا فراخوان مجبور به دادن translations در هر فراخوانی نباشد. */
fun uiString(key: String, language: Language): String = t(key, language, uiTranslations)

/** برای کلیدهای Template دار (مثل "{count} پروژه‌ی محلی") — جایگزینی ساده‌ی `{name}`. */
fun uiTemplate(key: String, language: Language, vararg replacements: Pair<String, String>): String {
    var result = uiString(key, language)
    replacements.forEach { (placeholder, value) -> result = result.replace("{$placeholder}", value) }
    return result
}
