package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.ProjectWithCounts
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.project.Project
import com.operaboys.cinemashotgenerator.domain.project.ProjectSummary
import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState

// واحد ۱۶ فاز ۱ — نگاشت دوطرفه‌ی ProjectEntity ↔ Project دامنه (docs/adr/044-...md).

fun ProjectEntity.toDomain(): Project = Project(
    projectId = projectId,
    projectName = projectName,
    createdAt = createdAt,
    lastModified = lastModified,
    uiLanguage = Language.valueOf(uiLanguage.uppercase()),
    state = EntityState.valueOf(state)
)

fun Project.toEntity(): ProjectEntity = ProjectEntity(
    projectId = projectId,
    projectName = projectName,
    createdAt = createdAt,
    lastModified = lastModified,
    uiLanguage = uiLanguage.name.lowercase(),
    state = state.name
)

fun ProjectWithCounts.toSummary(): ProjectSummary = ProjectSummary(
    project = project.toDomain(),
    sceneCount = sceneCount,
    shotCount = shotCount
)
