package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.OverrideDao
import com.operaboys.cinemashotgenerator.data.entity.OverrideEntity
import com.operaboys.cinemashotgenerator.domain.story.HumanOverride
import com.operaboys.cinemashotgenerator.domain.story.OverrideScope
import com.operaboys.cinemashotgenerator.domain.story.OverrideType
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// رفع یافته‌ی معماری «Human Override هرگز به UI وصل نشده» (کشف‌شده در G3/ADR-067،
// رفع در ADR-068) — لایه‌ی ذخیره‌سازی HumanOverride.
//
// یافته‌ی مهم بازبینی مستقل (خلاف پیش‌بررسی دستور کار): OverrideEntity/OverrideDao
// از قدم اول واحد ۱۵ (`docs/blueprints/15-project-storage.md`) از قبل ساخته شده
// بودند («overrideDataJson: scope + reason، طبق override-policy.md») — دقیقاً
// همان الگوی EventLogDao (قدم قبل، G3) که ساخته شد اما هرگز وصل نشد. یعنی این
// قدم به Entity/Dao/Migration تازه نیاز ندارد — فقط یک Repository لازم است که
// HumanOverride دامنه را به همان OverrideEntity موجود نگاشت کند.
//
// OverrideDataDto فقط فیلدهایی از HumanOverride را نگه می‌دارد که در ستون‌های
// مسطح OverrideEntity (overrideId/entityType/entityId/active) جا نمی‌شوند —
// هم‌الگو دقیق با ProjectEntityDto/ShotDto (DTO محلی @Serializable، جدا از
// Entity Room).
@Serializable
private data class OverrideDataDto(
    val overrideType: String,
    val createdAt: String,
    val field: String,
    val originalValue: String,
    val overrideValue: String,
    val reason: String? = null,
    val usageCount: Int = 0,
    val lastApplied: String? = null,
    val revoked: Boolean = false,
    val revokedAt: String? = null,
    val revokedReason: String? = null
)

private val overrideJson = Json { ignoreUnknownKeys = true }

class HumanOverrideRepository(private val overrideDao: OverrideDao) {

    suspend fun saveOverride(override: HumanOverride): Result<Unit> = runCatching {
        val data = OverrideDataDto(
            overrideType = override.overrideType.name,
            createdAt = override.createdAt,
            field = override.scope.field,
            originalValue = override.scope.originalValue,
            overrideValue = override.scope.overrideValue,
            reason = override.reason,
            usageCount = override.usageCount,
            lastApplied = override.lastApplied,
            revoked = override.revoked,
            revokedAt = override.revokedAt,
            revokedReason = override.revokedReason
        )
        overrideDao.saveOverride(
            OverrideEntity(
                overrideId = override.overrideId,
                entityType = override.scope.entityType,
                entityId = override.scope.entityId,
                overrideDataJson = overrideJson.encodeToString(OverrideDataDto.serializer(), data),
                active = override.active
            )
        )
    }

    /** فقط Override های فعال (Revoke‌نشده) یک Entity واحد (مثلاً یک shotId). */
    suspend fun loadActiveOverridesForEntity(entityId: String): Result<List<HumanOverride>> = runCatching {
        overrideDao.getOverridesForEntity(entityId).first()
            .filter { it.active }
            .map { it.toDomain() }
    }

    private fun OverrideEntity.toDomain(): HumanOverride {
        val data = overrideJson.decodeFromString(OverrideDataDto.serializer(), overrideDataJson)
        return HumanOverride(
            overrideId = overrideId,
            overrideType = OverrideType.valueOf(data.overrideType),
            createdAt = data.createdAt,
            active = active,
            scope = OverrideScope(
                entityType = entityType,
                entityId = entityId,
                field = data.field,
                originalValue = data.originalValue,
                overrideValue = data.overrideValue
            ),
            reason = data.reason,
            usageCount = data.usageCount,
            lastApplied = data.lastApplied,
            revoked = data.revoked,
            revokedAt = data.revokedAt,
            revokedReason = data.revokedReason
        )
    }
}
