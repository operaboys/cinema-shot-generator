package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.ProjectDnaDao
import com.operaboys.cinemashotgenerator.data.entity.ProjectDnaEntity
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import kotlinx.serialization.json.Json

// واحد ۱۵ — قدم ۳ (زیرقدم ۱): اتصال واقعی ProjectDna (واحد ۰۲) به ProjectDnaDao.

class ProjectDnaRepository(private val projectDnaDao: ProjectDnaDao) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun saveProjectDna(dna: ProjectDna): Result<Unit> = runCatching {
        projectDnaDao.saveProjectDna(
            ProjectDnaEntity(
                dnaId = dna.dnaId,
                projectId = dna.projectId,
                dnaDataJson = json.encodeToString(ProjectDnaDto.serializer(), dna.toDto())
            )
        )
    }

    suspend fun loadProjectDna(projectId: String): Result<ProjectDna?> = runCatching {
        projectDnaDao.loadProjectDnaForProject(projectId)?.let {
            json.decodeFromString(ProjectDnaDto.serializer(), it.dnaDataJson).toDomain()
        }
    }
}
