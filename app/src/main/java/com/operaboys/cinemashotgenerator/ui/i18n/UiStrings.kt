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
    "studioTab.output" to "خروجی"
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
    "studioTab.output" to "Output"
)

val uiTranslations: Map<Language, Map<String, String>> = mapOf(
    Language.FA to faStrings,
    Language.EN to enStrings
)

/** میان‌بر مختصر روی domain.outputdelivery.t() برای این پکیج، تا فراخوان مجبور به دادن translations در هر فراخوانی نباشد. */
fun uiString(key: String, language: Language): String = t(key, language, uiTranslations)
