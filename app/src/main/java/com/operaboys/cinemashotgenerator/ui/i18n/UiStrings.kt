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

    // فاز ۲ — Story Tab
    "story.titleLabel" to "عنوان پروژه",
    "story.storyTypeLabel" to "نوع داستان",
    "story.genreLabel" to "ژانر",
    "story.moodPrimaryLabel" to "حال‌وهوای اصلی",
    "story.moodSecondaryLabel" to "حال‌وهوای فرعی",
    "story.moodSecondaryNone" to "هیچ‌کدام",
    "story.narrativeIntensityLabel" to "شدت روایت",
    "story.visualIntentLabel" to "قصد بصری",
    "story.targetShotsLabel" to "هدف تعداد شات",
    "story.secondsPerShotLabel" to "ثانیه به‌ازای شات",
    "story.freeStoryPlaceholder" to "داستان آزاد در «تفکیک داستان با AI» نوشته می‌شود — اینجا فقط نمایش است.",
    "story.validationBlockingHeader" to "مسدودکننده",
    "story.validationWarningHeader" to "هشدار",

    "storyType.narrative" to "روایی",
    "storyType.conceptual" to "مفهومی",
    "storyType.visualOnly" to "فقط‌بصری",
    "storyType.experimental" to "آزمایشی",

    "genre.action" to "اکشن",
    "genre.drama" to "درام",
    "genre.sciFi" to "علمی‌تخیلی",
    "genre.fantasy" to "فانتزی",
    "genre.horror" to "ترسناک",
    "genre.romance" to "عاشقانه",
    "genre.documentary" to "مستند",
    "genre.experimental" to "آزمایشی",

    "mood.epic" to "حماسی",
    "mood.action" to "اکشن",
    "mood.exciting" to "هیجان‌انگیز",
    "mood.energetic" to "پرانرژی",
    "mood.happy" to "شاد",
    "mood.joyful" to "شادمانه",
    "mood.playful" to "بازیگوش",
    "mood.hopeful" to "امیدوارانه",
    "mood.warm" to "گرم",
    "mood.romantic" to "عاشقانه",
    "mood.emotional" to "احساسی",
    "mood.melancholic" to "غم‌انگیز",
    "mood.nostalgic" to "نوستالژیک",
    "mood.bittersweet" to "تلخ‌وشیرین",
    "mood.dramatic" to "دراماتیک",
    "mood.dark" to "تاریک",
    "mood.mysterious" to "مرموز",
    "mood.tense" to "پرتنش",
    "mood.scary" to "ترسناک",
    "mood.eerie" to "وهم‌آور",
    "mood.calm" to "آرام",
    "mood.peaceful" to "صلح‌آمیز",
    "mood.serene" to "آرام‌بخش",
    "mood.contemplative" to "تأملی",
    "mood.dreamyMood" to "رؤیایی",

    "narrativeIntensity.minimal" to "حداقلی",
    "narrativeIntensity.moderate" to "متوسط",
    "narrativeIntensity.high" to "زیاد",
    "narrativeIntensity.extreme" to "شدید",

    "visualIntent.realistic" to "واقع‌گرایانه",
    "visualIntent.cinematic" to "سینمایی",
    "visualIntent.stylized" to "استایل‌محور",
    "visualIntent.artistic" to "هنری",
    "visualIntent.abstract" to "انتزاعی",

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

    // Phase 2 — Story Tab
    "story.titleLabel" to "Project Title",
    "story.storyTypeLabel" to "Story Type",
    "story.genreLabel" to "Genre",
    "story.moodPrimaryLabel" to "Primary Mood",
    "story.moodSecondaryLabel" to "Secondary Mood",
    "story.moodSecondaryNone" to "None",
    "story.narrativeIntensityLabel" to "Narrative Intensity",
    "story.visualIntentLabel" to "Visual Intent",
    "story.targetShotsLabel" to "Target Shot Count",
    "story.secondsPerShotLabel" to "Seconds per Shot",
    "story.freeStoryPlaceholder" to "The free-form story is written in \"AI Story Breakdown\" — this is a read-only preview.",
    "story.validationBlockingHeader" to "Blocking",
    "story.validationWarningHeader" to "Warning",

    "storyType.narrative" to "Narrative",
    "storyType.conceptual" to "Conceptual",
    "storyType.visualOnly" to "Visual-only",
    "storyType.experimental" to "Experimental",

    "genre.action" to "Action",
    "genre.drama" to "Drama",
    "genre.sciFi" to "Sci-Fi",
    "genre.fantasy" to "Fantasy",
    "genre.horror" to "Horror",
    "genre.romance" to "Romance",
    "genre.documentary" to "Documentary",
    "genre.experimental" to "Experimental",

    "mood.epic" to "Epic",
    "mood.action" to "Action",
    "mood.exciting" to "Exciting",
    "mood.energetic" to "Energetic",
    "mood.happy" to "Happy",
    "mood.joyful" to "Joyful",
    "mood.playful" to "Playful",
    "mood.hopeful" to "Hopeful",
    "mood.warm" to "Warm",
    "mood.romantic" to "Romantic",
    "mood.emotional" to "Emotional",
    "mood.melancholic" to "Melancholic",
    "mood.nostalgic" to "Nostalgic",
    "mood.bittersweet" to "Bittersweet",
    "mood.dramatic" to "Dramatic",
    "mood.dark" to "Dark",
    "mood.mysterious" to "Mysterious",
    "mood.tense" to "Tense",
    "mood.scary" to "Scary",
    "mood.eerie" to "Eerie",
    "mood.calm" to "Calm",
    "mood.peaceful" to "Peaceful",
    "mood.serene" to "Serene",
    "mood.contemplative" to "Contemplative",
    "mood.dreamyMood" to "Dreamy",

    "narrativeIntensity.minimal" to "Minimal",
    "narrativeIntensity.moderate" to "Moderate",
    "narrativeIntensity.high" to "High",
    "narrativeIntensity.extreme" to "Extreme",

    "visualIntent.realistic" to "Realistic",
    "visualIntent.cinematic" to "Cinematic",
    "visualIntent.stylized" to "Stylized",
    "visualIntent.artistic" to "Artistic",
    "visualIntent.abstract" to "Abstract",

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
