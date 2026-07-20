package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.AssetDao
import com.operaboys.cinemashotgenerator.data.entity.AssetEntity
import com.operaboys.cinemashotgenerator.domain.asset.AssetType
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import kotlinx.serialization.json.Json

// واحد ۱۵ — قدم ۳ (زیرقدم ۱): اتصال واقعی CharacterAsset/LocationAsset (واحد ۰۶) به
// AssetDao.
//
// دو تابع ذخیره‌ی جدا (نه یک saveAsset مشترک) چون هیچ نوع عمومی Asset در دامنه وجود
// ندارد (تأیید شده با grep در AssetModels.kt، هم‌راستا با یافته‌ی قبلی ADR-003 واحد
// ۰۶) — CharacterAsset و LocationAsset دو Kotlin type کاملاً مستقل‌اند.
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
                assetType = asset.assetType.name,
                assetDataJson = json.encodeToString(LocationAssetDto.serializer(), asset.toDto())
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

    /** برای Object/Location — هر دو با LocationAsset مدل می‌شوند (طبق ADR-003 واحد ۰۶). */
    suspend fun loadAssets(assetIds: List<String>): Result<List<LocationAsset>> = runCatching {
        assetIds.mapNotNull { id ->
            val entity = assetDao.loadAsset(id) ?: return@mapNotNull null
            if (entity.assetType.equals(AssetType.CHARACTER.name, ignoreCase = true)) return@mapNotNull null
            json.decodeFromString(LocationAssetDto.serializer(), entity.assetDataJson).toDomain()
        }
    }
}
