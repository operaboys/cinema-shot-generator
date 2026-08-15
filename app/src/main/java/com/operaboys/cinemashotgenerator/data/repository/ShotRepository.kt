package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.ShotDao
import com.operaboys.cinemashotgenerator.data.entity.ShotEntity
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

// واحد ۱۶ فاز ۲ — قدم ۲: اولین Repository مستقل برای Shot (واحد ۰۵) — یافته‌ی واقعی
// با grep (تأییدشده): تا این قدم هیچ Repository ای برای ذخیره‌ی یک Shot دامنه‌ای از
// صفر وجود نداشت؛ `ShotDao` مستقیماً فقط از داخل `ProjectTransactionDao`/
// `PromptGenerationRepository`/`SettingsResolutionRepository` (فقط خواندن) استفاده
// می‌شد. `Shot.toDto()`/`ShotDto` (data/repository/DtoMappers.kt، واحد ۱۵) از قبل
// موجود بودند — این Repository فقط آن‌ها را به ShotDao وصل می‌کند، هم‌الگو با
// AssetRepository/SceneRepository.
//
// تصمیم مستند: از `ProjectTransactionDao.saveShotWithSceneUpdate` استفاده نشد — آن
// تابع برای «به‌روزرسانی یک Scene موجود همراه یک Shot» طراحی شده (طبق کامنت خودش و
// ADR-017)؛ اینجا Scene و تمام Shot هایش هر دو تازه ساخته می‌شوند (نه یک Scene
// موجود که به‌روز شود)، پس SceneRepository.saveScene قبل از این تابع (طبق ترتیب FK،
// همان هشدار ADR-017) به‌اندازه‌ی کافی است.
class ShotRepository(private val shotDao: ShotDao) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun saveShot(shot: Shot): Result<Unit> = runCatching {
        shotDao.saveShot(
            ShotEntity(
                shotId = shot.shotId,
                sceneId = shot.sceneId,
                shotDataJson = json.encodeToString(ShotDto.serializer(), shot.toDto())
            )
        )
    }

    // واحد ۱۶ فاز ۴ — قدم ۲: کمبود مستندشده‌ی صریح دستور کار — تا این قدم هیچ متد
    // خواندنی‌ای در این Repository وجود نداشت (فقط saveShot). ShotDao.getShotsForScene
    // از قبل موجود بود (واحد ۱۵) اما هیچ معادل سطح دامنه‌ای نداشت — دقیقاً هم‌الگو با
    // SceneRepository.loadAllScenes/AssetRepository.loadAllCharacterAssets (Flow، نه
    // suspend Result، چون UI باید با ذخیره‌ی Shot جدید خودکار به‌روز شود).
    // مرتب‌سازی بر اساس shotNumber اینجا انجام می‌شود — خودِ Query مرتب‌سازی ندارد.
    fun loadAllShots(sceneId: String): Flow<List<Shot>> =
        shotDao.getShotsForScene(sceneId).map { entities ->
            entities
                .map { json.decodeFromString(ShotDto.serializer(), it.shotDataJson).toDomain() }
                .sortedBy { it.shotNumber }
        }

    /** برای Shot Composer — بارگذاری یک Shot موجود برای ویرایش (طبق «chevron → opens Shot Composer» سند طراحی). */
    suspend fun loadShot(shotId: String): Result<Shot?> = runCatching {
        shotDao.loadShot(shotId)?.let {
            json.decodeFromString(ShotDto.serializer(), it.shotDataJson).toDomain()
        }
    }

    /**
     * یافته‌ی #۱۴ appendix ADR-081 (ADR-084) — Tab «خروجی» Studio: تنها معادل
     * سطح دامنه‌ی `ShotDao.getShotsForProject` که تا این قدم وجود نداشت (فقط
     * `findShotIdsUsingAsset` زیر همین Query را داخلی Decode می‌کرد، بدون افشای
     * لیست کامل Shot). همان الگوی Decode دقیق آن تابع.
     */
    suspend fun loadAllShotsForProject(projectId: String): List<Shot> =
        shotDao.getShotsForProject(projectId)
            .map { json.decodeFromString(ShotDto.serializer(), it.shotDataJson).toDomain() }

    // رفع G22 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، docs/adr/062-...):
    // ShotDao.deleteShot از قبل موجود بود اما هیچ Repository/UI ای صدایش نمی‌زد.
    // برخلاف Scene/Asset، هیچ Rule دامنه‌ای حذف Shot را مشروط نکرده (بلوپرینت‌های
    // ۰۴/۰۶ فقط برای Scene/Asset چنین Ruleای دارند) — پس هیچ Validation ای اینجا
    // لازم نیست، فقط I/O مستقیم.
    suspend fun deleteShot(shotId: String): Result<Unit> = runCatching {
        shotDao.loadShot(shotId)?.let { shotDao.deleteShot(it) }
        Unit
    }

    /**
     * برای Rule واقعی حذف Asset (validateAssetDeletion، AssetValidation.kt) — کدام
     * Shot های این پروژه از این assetId (Character/Location/Object) استفاده
     * می‌کنند. shotDataJson یک Blob است، پس فیلتر بعد از Deserialize کامل انجام
     * می‌شود، نه با SQL (getShotsForProject).
     */
    suspend fun findShotIdsUsingAsset(projectId: String, assetId: String): List<String> =
        shotDao.getShotsForProject(projectId)
            .map { json.decodeFromString(ShotDto.serializer(), it.shotDataJson).toDomain() }
            .filter { assetId in it.characterIds || assetId in it.objectIds || assetId in it.locationIds }
            .map { it.shotId }
}
