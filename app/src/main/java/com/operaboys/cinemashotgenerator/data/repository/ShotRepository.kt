package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.ShotDao
import com.operaboys.cinemashotgenerator.data.entity.ShotEntity
import com.operaboys.cinemashotgenerator.domain.shot.Shot
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
}
