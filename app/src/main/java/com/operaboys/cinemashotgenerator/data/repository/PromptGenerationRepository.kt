package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.SceneDao
import com.operaboys.cinemashotgenerator.data.dao.ShotDao
import com.operaboys.cinemashotgenerator.domain.promptengine.PromptGenerationInput
import kotlinx.serialization.json.Json

// واحد ۱۵ — قدم ۳ (زیرقدم ۲، آخرین زیرقدم واحد ۱۵): اتصال نهایی collectData —
// نقطه‌ی تجمیع واقعیِ تمام Repository های واحد ۱۵ در یک PromptGenerationInput واقعی
// (واحد ۱۱)، دقیقاً هم‌الگو با نقش خودِ واحد ۱۱ در تجمیع دامنه.
//
// ترتیب Resolve دقیقاً طبق دستور این قدم: Shot → Scene (از shot.sceneId) → ProjectDna
// (از sceneEntity.projectId، چون Scene دامنه خودش projectId ندارد) → characters/
// objects/locations (از AssetRepository) → camera/lighting/environment (از
// SettingsResolutionRepository، sceneDefault=null طبق ADR-013) → audioContext
// (nullable، AudioContextRepository).
//
// NOTE فنی: Shot اینجا مستقیماً از ShotDao خوانده و decode می‌شود (نه از طریق
// SettingsResolutionRepository که این کار را داخلی/خصوصی انجام می‌دهد) چون
// PromptGenerationInput.shot به شیء کامل Shot نیاز دارد، نه فقط camera/lighting/
// environment حل‌شده — یعنی ShotEntity یک‌بار اینجا و احتمالاً یک‌بار دیگر داخل هر
// فراخوان resolve*For (که خودش دوباره از ShotDao می‌خواند) بارگذاری می‌شود. این یک
// خواندن تکراری بی‌خطر (SQLite ارزان) است، نه یک باگ — جزئیات در
// docs/adr/020-unit15-step3b-audio-collectdata-deviations.md.
class PromptGenerationRepository(
    private val shotDao: ShotDao,
    private val sceneDao: SceneDao,
    private val projectDnaRepository: ProjectDnaRepository,
    private val assetRepository: AssetRepository,
    private val settingsResolutionRepository: SettingsResolutionRepository,
    private val audioContextRepository: AudioContextRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun collectData(shotId: String): Result<PromptGenerationInput> {
        val shotEntity = shotDao.loadShot(shotId)
            ?: return Result.failure(IllegalArgumentException("شات '$shotId' یافت نشد"))
        val shot = runCatching { json.decodeFromString(ShotDto.serializer(), shotEntity.shotDataJson).toDomain() }
            .getOrElse { return Result.failure(it) }

        val sceneEntity = sceneDao.loadScene(shot.sceneId)
            ?: return Result.failure(IllegalStateException("صحنه‌ی '${shot.sceneId}' برای این شات یافت نشد"))
        val scene = runCatching { json.decodeFromString(SceneDto.serializer(), sceneEntity.sceneDataJson).toDomain() }
            .getOrElse { return Result.failure(it) }

        val dna = projectDnaRepository.loadProjectDna(sceneEntity.projectId)
            .getOrElse { return Result.failure(it) }
            ?: return Result.failure(IllegalStateException("ProjectDna برای پروژه‌ی '${sceneEntity.projectId}' یافت نشد"))

        val characters = assetRepository.loadCharacterAssets(shot.characterIds).getOrElse { return Result.failure(it) }
        val objects = assetRepository.loadObjectAssets(shot.objectIds).getOrElse { return Result.failure(it) }
        val locations = assetRepository.loadLocationAssets(shot.locationIds).getOrElse { return Result.failure(it) }

        val camera = settingsResolutionRepository.resolveCameraSettingsFor(shotId).getOrElse { return Result.failure(it) }
        val lighting = settingsResolutionRepository.resolveLightingSettingsFor(shotId).getOrElse { return Result.failure(it) }
        val environment = settingsResolutionRepository.resolveEnvironmentSettingsFor(shotId).getOrElse { return Result.failure(it) }

        val audioContext = audioContextRepository.loadAudioContext(shotId).getOrElse { return Result.failure(it) }

        return Result.success(
            PromptGenerationInput(
                dna = dna,
                scene = scene,
                shot = shot,
                characters = characters,
                objects = objects,
                locations = locations,
                camera = camera,
                lighting = lighting,
                environment = environment,
                audioContext = audioContext
            )
        )
    }
}
