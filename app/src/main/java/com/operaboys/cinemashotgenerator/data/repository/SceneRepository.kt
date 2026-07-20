package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.SceneDao
import com.operaboys.cinemashotgenerator.data.entity.SceneEntity
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import kotlinx.serialization.json.Json

// واحد ۱۵ — قدم ۳ (زیرقدم ۲): اتصال واقعی Scene (واحد ۰۴) به SceneDao.
// این Repository در قدم ۲ ساخته نشده بود چون هیچ تابع تزریق‌پذیری در واحد ۰۴/۱۲
// مستقیماً به Scene کامل نیاز نداشت؛ اما PromptGenerationInput.scene به آن نیاز دارد.
class SceneRepository(private val sceneDao: SceneDao) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun saveScene(projectId: String, scene: Scene): Result<Unit> = runCatching {
        sceneDao.saveScene(
            SceneEntity(
                sceneId = scene.sceneId,
                projectId = projectId,
                sceneDataJson = json.encodeToString(SceneDto.serializer(), scene.toDto())
            )
        )
    }

    suspend fun loadScene(sceneId: String): Result<Scene?> = runCatching {
        sceneDao.loadScene(sceneId)?.let {
            json.decodeFromString(SceneDto.serializer(), it.sceneDataJson).toDomain()
        }
    }
}
