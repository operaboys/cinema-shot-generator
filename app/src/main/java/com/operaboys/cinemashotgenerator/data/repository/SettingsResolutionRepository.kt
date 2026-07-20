package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.SceneDao
import com.operaboys.cinemashotgenerator.data.dao.ShotDao
import com.operaboys.cinemashotgenerator.domain.camera.CameraSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingSettings
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.resolveCameraSettings
import com.operaboys.cinemashotgenerator.domain.shot.resolveEnvironmentSettings
import com.operaboys.cinemashotgenerator.domain.shot.resolveLightingSettings
import kotlinx.serialization.json.Json

// واحد ۱۵ — قدم ۲: اتصال واقعی resolveCameraSettings/resolveLightingSettings/
// resolveEnvironmentSettings (واحد ۰۵، domain/shot/ShotSettingsResolution.kt) به
// ShotDao/SceneDao.
//
// NOTE مهم درباره‌ی sceneDefault: طبق ADR-013 (واحد ۰۵)، Scene هیچ فیلد پیش‌فرض
// camera/lighting/environment ندارد (فقط globalVisualStyle و لیست‌های محدودکننده) —
// این تصمیم قبلاً گرفته و مستند شده، نه ابهام جدید. پس sceneDefault همیشه با مقدار
// null به resolve* پاس داده می‌شود؛ SceneDao فقط برای تأیید وجود واقعیِ صحنه‌ی این شات
// خوانده می‌شود (اگر صحنه حذف شده باشد، Result.failure برمی‌گردد)، نه برای استخراج
// یک مقدار پیش‌فرض که اصلاً در Schema فعلی جایی برای ذخیره ندارد. معنای عملی: فقط
// شات‌هایی که source="override" دارند (و مقدار override واقعی) قابل‌حل‌شدن‌اند —
// این دقیقاً رفتار صادقانه‌ی معماری فعلی است، نه یک باگ.
class SettingsResolutionRepository(
    private val shotDao: ShotDao,
    private val sceneDao: SceneDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun loadShotDomain(shotId: String): Result<Shot> {
        val shotEntity = shotDao.loadShot(shotId)
            ?: return Result.failure(IllegalArgumentException("شات '$shotId' یافت نشد"))
        sceneDao.loadScene(shotEntity.sceneId)
            ?: return Result.failure(IllegalStateException("صحنه‌ی '${shotEntity.sceneId}' برای این شات یافت نشد"))
        return runCatching {
            json.decodeFromString(ShotDto.serializer(), shotEntity.shotDataJson).toDomain()
        }
    }

    suspend fun resolveCameraSettingsFor(shotId: String): Result<CameraSettings> =
        loadShotDomain(shotId).fold(
            onSuccess = { shot -> resolveCameraSettings(shot, sceneDefault = null) },
            onFailure = { Result.failure(it) }
        )

    suspend fun resolveLightingSettingsFor(shotId: String): Result<LightingSettings> =
        loadShotDomain(shotId).fold(
            onSuccess = { shot -> resolveLightingSettings(shot, sceneDefault = null) },
            onFailure = { Result.failure(it) }
        )

    suspend fun resolveEnvironmentSettingsFor(shotId: String): Result<EnvironmentSettings> =
        loadShotDomain(shotId).fold(
            onSuccess = { shot -> resolveEnvironmentSettings(shot, sceneDefault = null) },
            onFailure = { Result.failure(it) }
        )
}
