package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.EventLogDao
import com.operaboys.cinemashotgenerator.data.dao.VersionDao
import com.operaboys.cinemashotgenerator.data.entity.EventLogEntity
import com.operaboys.cinemashotgenerator.data.entity.VersionEntity
import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityVersion
import com.operaboys.cinemashotgenerator.domain.stateversioning.VersionType
import com.operaboys.cinemashotgenerator.domain.stateversioning.rollbackToVersion
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// واحد ۱۵ — قدم ۲: اتصال واقعی rollbackToVersion (واحد ۱۲، domain/stateversioning/
// Versioning.kt) به VersionDao/EventLogDao.
//
// نکته‌ی طراحی مهم: createSnapshot و logEvent در امضای rollbackToVersion هر دو
// synchronous هستند (نه suspend)، در حالی‌که نوشتن واقعی در Room همیشه suspend است.
// این Repository این تناقض را با یک الگوی «Buffer-then-persist» حل می‌کند: createSnapshot
// فقط یک EntityVersion جدید در حافظه می‌سازد (بدون I/O)؛ logEvent فقط رویداد را در یک
// لیست موقت جمع می‌کند. بعد از این‌که rollbackToVersion برمی‌گردد (خارج از آن دو
// Callback synchronous)، نتیجه‌ی نهایی و رویدادهای جمع‌شده به‌صورت واقعی و suspend در
// VersionDao/EventLogDao نوشته می‌شوند. این از GlobalScope.launch (Concurrency
// بی‌ساختار) یا runBlocking (بلاک‌کردن نامناسب Thread) درون یک Callback synchronous
// اجتناب می‌کند — جزئیات کامل در docs/adr/018-unit15-step2-repository-deviations.md.
class VersioningRepository(
    private val versionDao: VersionDao,
    private val eventLogDao: EventLogDao,
    private val idProvider: () -> String = ::defaultVersionId,
    private val clock: () -> String = ::nowIso8601
) {
    private val stringListSerializer = ListSerializer(String.serializer())

    suspend fun rollback(targetVersionId: String): Result<EntityVersion> {
        val targetEntity = versionDao.loadVersion(targetVersionId)
            ?: return Result.failure(IllegalArgumentException("نسخه‌ی هدف یافت نشد"))
        val targetVersion = targetEntity.toDomain()

        val pendingEvents = mutableListOf<EventLogEntity>()
        val result = rollbackToVersion(
            targetVersion = targetVersion,
            createSnapshot = { entityId, snapshotData, changeSummary ->
                EntityVersion(
                    versionId = idProvider(),
                    entityId = entityId,
                    // determineVersionType (واحد ۱۲) به modifiedFields نیاز دارد که این
                    // Callback دریافت نمی‌کند؛ Rollback بازگشت به وضعیت قبلی است، نه
                    // تغییر ساختاری جدید — SAFE یک پیش‌فرض محافظه‌کارانه و مستند است.
                    versionType = VersionType.SAFE,
                    createdAt = clock(),
                    changeSummary = changeSummary,
                    modifiedFields = emptyList(),
                    snapshotData = snapshotData,
                    parentVersionId = targetVersion.versionId
                )
            },
            logEvent = { eventType, entityId ->
                pendingEvents += EventLogEntity(eventType = eventType, entityId = entityId, timestamp = clock())
            }
        )

        result.onSuccess { newVersion ->
            versionDao.saveVersion(newVersion.toEntity())
            pendingEvents.forEach { eventLogDao.logEvent(it) }
        }
        return result
    }

    private fun VersionEntity.toDomain(): EntityVersion = EntityVersion(
        versionId = versionId,
        entityId = entityId,
        versionType = VersionType.valueOf(versionType.uppercase()),
        createdAt = createdAt,
        changeSummary = changeSummary,
        modifiedFields = Json.decodeFromString(stringListSerializer, modifiedFieldsJson),
        snapshotData = snapshotJson,
        parentVersionId = parentVersionId
    )

    private fun EntityVersion.toEntity(): VersionEntity = VersionEntity(
        versionId = versionId,
        entityId = entityId,
        versionType = versionType.name.lowercase(),
        snapshotJson = snapshotData,
        createdAt = createdAt,
        changeSummary = changeSummary,
        modifiedFieldsJson = Json.encodeToString(stringListSerializer, modifiedFields),
        parentVersionId = parentVersionId
    )
}

private fun defaultVersionId(): String = "v_" + UUID.randomUUID().toString().replace("-", "").take(12)
private fun nowIso8601(): String = Instant.now().toString()
