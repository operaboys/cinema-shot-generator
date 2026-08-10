package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.SceneDao
import com.operaboys.cinemashotgenerator.data.entity.SceneEntity
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.deleteScene as validateSceneDeletion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

// واحد ۱۵ — قدم ۳ (زیرقدم ۲): اتصال واقعی Scene (واحد ۰۴) به SceneDao.
// این Repository در قدم ۲ ساخته نشده بود چون هیچ تابع تزریق‌پذیری در واحد ۰۴/۱۲
// مستقیماً به Scene کامل نیاز نداشت؛ اما PromptGenerationInput.scene به آن نیاز دارد.
class SceneRepository(private val sceneDao: SceneDao) {
    private val json = Json { ignoreUnknownKeys = true }

    // واحد ۱۶ فاز ۴ — قدم ۱: کمبود مستندشده‌ی صریح دستور کار — SceneDao.getScenesForProject
    // از قبل موجود بود (واحد ۱۵)، اما هیچ معادل سطح دامنه‌ای برایش وجود نداشت. Flow
    // (نه suspend Result) — دقیقاً هم‌الگو با AssetRepository.loadAllCharacterAssets/...
    // (ADR-048): UI باید با ذخیره‌ی Scene جدید خودکار به‌روز شود.
    fun loadAllScenes(projectId: String): Flow<List<Scene>> =
        sceneDao.getScenesForProject(projectId).map { entities ->
            entities.map { json.decodeFromString(SceneDto.serializer(), it.sceneDataJson).toDomain() }
        }

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

    // رفع G22 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، docs/adr/062-...):
    // domain.scene.deleteScene (SceneValidation.kt) از قبل Rule واقعی «صحنه‌ی
    // دارای Shot قابل حذف نیست» را پیاده کرده بود، اما هیچ Repository/UI ای آن را
    // صدا نمی‌زد. اینجا فقط لایه‌ی نازک I/O روی همان Rule است — منطق واقعی همان‌جا
    // (دامنه) می‌ماند.
    suspend fun deleteScene(scene: Scene, existingShotsCount: Int): Result<Unit> = runCatching {
        validateSceneDeletion(scene, existingShotsCount).getOrThrow()
        sceneDao.loadScene(scene.sceneId)?.let { sceneDao.deleteScene(it) }
        Unit
    }
}
