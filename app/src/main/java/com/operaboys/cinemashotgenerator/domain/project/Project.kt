package com.operaboys.cinemashotgenerator.domain.project

import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState

// واحد ۱۶ — فاز ۱: مدل دامنه‌ی Project — قبل از این فاز فقط ProjectEntity (لایه‌ی
// Room) وجود داشت، بدون هیچ معادل دامنه‌ی خالص، بر خلاف Scene/Shot/ProjectDna که هرکدام
// هم Entity Room و هم مدل دامنه‌ی جدا دارند (SceneMappers/DtoMappers). این فاز آن
// شکاف را می‌بندد تا لایه‌ی UI/ViewModel با نوع دامنه کار کند، نه مستقیم با Entity
// Room. جزئیات کامل تصمیم در docs/adr/044-unit16-phase1-app-shell.md.

/**
 * `state` مستقیماً از `domain.stateversioning.EntityState` (واحد ۱۲) بازاستفاده
 * می‌کند — طبق تصریح خودِ docs/blueprints/16-user-workflow-v2.md («هر Entity
 * (Project/Scene/Shot/Asset) یک وضعیت EntityState دارد»)؛ enum دوم/موازی ساخته نشد.
 */
data class Project(
    val projectId: String,
    val projectName: String,
    val createdAt: String,
    val lastModified: String,
    val uiLanguage: Language,
    val state: EntityState
)

/** مدل نمایشی کارت پروژه (Home/Projects) — Project + شمارش صحنه/شات محاسبه‌شده. */
data class ProjectSummary(
    val project: Project,
    val sceneCount: Int,
    val shotCount: Int
)
