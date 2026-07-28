package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.AssetDao
import com.operaboys.cinemashotgenerator.data.entity.AssetEntity
import com.operaboys.cinemashotgenerator.domain.asset.AssetType
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import kotlinx.serialization.json.Json

// واحد ۱۵ — قدم ۳ (زیرقدم ۱): اتصال واقعی CharacterAsset/LocationAsset (واحد ۰۶) به
// AssetDao.
//
// سه تابع ذخیره‌ی جدا (نه یک saveAsset مشترک) چون هیچ نوع عمومی Asset در دامنه وجود
// ندارد — CharacterAsset/LocationAsset/ObjectAsset سه Kotlin type کاملاً مستقل‌اند.
//
// MIGRATED (docs/adr/029-unit06-continuity-tiers-migration-part1.md، بخش دوم — Option
// A): قبلاً LocationAsset هر دو AssetType.LOCATION و AssetType.OBJECT را با فیلد
// assetType خودش تفکیک می‌کرد (ADR-003 قدیمی)؛ اکنون LocationAsset دیگر assetType
// ندارد، پس این تفکیک اینجا صریحاً با AssetType.LOCATION.name/AssetType.OBJECT.name
// انجام می‌شود (نه از روی asset.assetType). جدول AssetEntity خودش (assetId/projectId/
// assetType/assetDataJson) بدون تغییر مانده — هیچ Migration اسکیمای Room لازم نبود.
class AssetRepository(private val assetDao: AssetDao) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun saveCharacterAsset(projectId: String, asset: CharacterAsset): Result<Unit> = runCatching {
        assetDao.saveAsset(
            AssetEntity(
                assetId = asset.assetId,
                projectId = projectId,
                assetType = asset.assetType.name,
                assetDataJson = json.encodeToString(CharacterAssetDto.serializer(), asset.toDto())
            )
        )
    }

    suspend fun saveLocationAsset(projectId: String, asset: LocationAsset): Result<Unit> = runCatching {
        assetDao.saveAsset(
            AssetEntity(
                assetId = asset.assetId,
                projectId = projectId,
                assetType = AssetType.LOCATION.name,
                assetDataJson = json.encodeToString(LocationAssetDto.serializer(), asset.toDto())
            )
        )
    }

    suspend fun saveObjectAsset(projectId: String, asset: ObjectAsset): Result<Unit> = runCatching {
        assetDao.saveAsset(
            AssetEntity(
                assetId = asset.assetId,
                projectId = projectId,
                assetType = AssetType.OBJECT.name,
                assetDataJson = json.encodeToString(ObjectAssetDto.serializer(), asset.toDto())
            )
        )
    }

    suspend fun loadCharacterAssets(characterIds: List<String>): Result<List<CharacterAsset>> = runCatching {
        characterIds.mapNotNull { id ->
            val entity = assetDao.loadAsset(id) ?: return@mapNotNull null
            if (!entity.assetType.equals(AssetType.CHARACTER.name, ignoreCase = true)) return@mapNotNull null
            json.decodeFromString(CharacterAssetDto.serializer(), entity.assetDataJson).toDomain()
        }
    }

    suspend fun loadLocationAssets(assetIds: List<String>): Result<List<LocationAsset>> = runCatching {
        assetIds.mapNotNull { id ->
            val entity = assetDao.loadAsset(id) ?: return@mapNotNull null
            if (!entity.assetType.equals(AssetType.LOCATION.name, ignoreCase = true)) return@mapNotNull null
            json.decodeFromString(LocationAssetDto.serializer(), entity.assetDataJson).toDomain()
        }
    }

    suspend fun loadObjectAssets(assetIds: List<String>): Result<List<ObjectAsset>> = runCatching {
        assetIds.mapNotNull { id ->
            val entity = assetDao.loadAsset(id) ?: return@mapNotNull null
            if (!entity.assetType.equals(AssetType.OBJECT.name, ignoreCase = true)) return@mapNotNull null
            json.decodeFromString(ObjectAssetDto.serializer(), entity.assetDataJson).toDomain()
        }
    }
}
