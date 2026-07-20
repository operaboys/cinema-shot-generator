package com.operaboys.cinemashotgenerator.domain.stateversioning

// واحد ۱۲ — State & Versioning (بخش ج: Versioning)
// منبع حقیقت: docs/blueprints/12-state-and-versioning.md
//
// توجه: VersionType (SAFE/RISKY) اینجا مربوط به ریسک نسخه‌بندی داده‌ی runtime است —
// با سطح‌بندی Low/Medium/High Risk در docs/governance/change-management.md (که برای
// تصمیمات معماری پس از Architecture Lock است) کاملاً بی‌ربط و مستقل است.

enum class VersionType { SAFE, RISKY }

data class EntityVersion(
    val versionId: String, // v1.2.3
    val entityId: String,
    val versionType: VersionType,
    val createdAt: String,
    val changeSummary: String,
    val modifiedFields: List<String>,
    val snapshotData: String, // JSON کامل موجودیت در این نسخه
    val parentVersionId: String?
)

/** تعیین نوع نسخه بر اساس این‌که آیا فیلدهای تغییریافته روی وابستگی‌ها اثر دارند. */
fun determineVersionType(modifiedFields: List<String>): VersionType {
    val riskyFields = listOf("scene_id", "asset_references", "core_identity")
    return if (modifiedFields.any { it in riskyFields }) VersionType.RISKY else VersionType.SAFE
}

/**
 * نسخه‌ی تزریق‌پذیر: loadVersion واقعی (I/O، وابسته به واحد ۱۵) از امضا حذف شد —
 * targetVersion از قبل توسط فراخوان بارگذاری‌شده فرض می‌شود؛ مسیر «نسخه‌ی هدف یافت
 * نشد» بلوپرینت اکنون به عهده‌ی همان بارگذاری بیرونی است، نه این تابع (targetVersion
 * دیگر nullable نیست).
 *
 * createSnapshot دقیقاً هر سه آرگومان createVersionFromSnapshot بلوپرینت (entityId،
 * snapshotData، changeSummary) را می‌گیرد — نه دو آرگومان، برای وفاداری کامل به
 * فراخوانی واقعی بلوپرینت. entityId/snapshotData از targetVersion گرفته می‌شوند
 * (نیازی به پارامتر entityId جدا نبود، چون خودِ EntityVersion این فیلد را دارد).
 *
 * Result<EntityVersion> برای هم‌راستایی با امضای بلوپرینت نگه داشته شد، هرچند با این
 * طراحی تزریق‌پذیر همیشه Result.success برمی‌گردد (createSnapshot تزریق‌شده امکان
 * گزارش شکست را ندارد، چون نوع بازگشتی‌اش EntityVersion غیر-Null است).
 */
fun rollbackToVersion(
    targetVersion: EntityVersion,
    createSnapshot: (entityId: String, snapshotData: String, changeSummary: String) -> EntityVersion,
    logEvent: (eventType: String, entityId: String) -> Unit = { _, _ -> }
): Result<EntityVersion> {
    val newVersion = createSnapshot(
        targetVersion.entityId,
        targetVersion.snapshotData,
        "Rollback به نسخه‌ی ${targetVersion.versionId}"
    )
    logEvent("rollback_performed", targetVersion.entityId)
    return Result.success(newVersion)
}
