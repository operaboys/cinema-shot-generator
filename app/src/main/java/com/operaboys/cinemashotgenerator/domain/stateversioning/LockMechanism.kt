package com.operaboys.cinemashotgenerator.domain.stateversioning

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import java.time.Instant

// واحد ۱۲ — State & Versioning (بخش ب: Lock Mechanism، نسخه‌ی ساده‌شده‌ی تک‌کاربره)
// منبع حقیقت: docs/blueprints/12-state-and-versioning.md
//
// طبق تصریح بلوپرینت: هیچ locked_by یا forceUnlock چندکاربره اضافه نشد — این پروژه
// تک‌کاربره است.

data class EntityLock(
    val locked: Boolean = false,
    val lockedAt: String? = null,
    val lockReason: String? = null
)

/**
 * هیچ نوع عمومی «Entity» (نمایندگی مشترک Project/Scene/Shot/Output طبق تعریف این
 * واحد در بلوپرینت) در هیچ‌جای پروژه وجود ندارد (تأیید شده با grep) — Scene/Shot/
 * ProjectDna هرکدام نوع کاملاً مستقل خودشان را دارند، بدون فیلد lock/state مشترک.
 * این یک تجمیع محلی حداقلی است: فقط همان فیلدهایی که کد مفهومی بلوپرینت واقعاً
 * می‌خواند (id، type، lock). اتصال واقعی این نوع به Scene/Shot/ProjectDna/Output
 * واقعی، وظیفه‌ی لایه‌ی Persistence (واحد ۱۵) است.
 */
data class Entity(
    val id: String,
    val type: String,
    val lock: EntityLock = EntityLock()
)

/**
 * مشابه الگوی OverrideEventLogger (واحد ۰۱، domain.story.OverrideActions) — fun
 * interface + NoOp — اما بازاستفاده‌ی مستقیم از خودِ OverrideEventLogger نشد، چون
 * Payload آن (OverrideEvent) فیلدهای overrideId/scope مخصوص Human Override دارد که
 * برای رویدادهای قفل Entity معنی ندارند؛ امضای logEvent(eventType, entityId) بلوپرینت
 * ۱۲ خودش دو رشته‌ی ساده است، نه یک Payload اختصاصی.
 */
fun interface StateVersioningEventLogger {
    fun log(eventType: String, entityId: String)

    companion object {
        /** پیش‌فرض بدون اثر؛ تا اتصال واقعی به تاریخچه‌ی رویداد (واحد ۱۵)، ثبت واقعی انجام نمی‌شود. */
        val NoOp: StateVersioningEventLogger = StateVersioningEventLogger { _, _ -> }
    }
}

fun setLock(
    entity: Entity,
    locked: Boolean,
    reason: String? = null,
    clock: () -> String = ::nowIso8601
): Entity {
    return entity.copy(lock = EntityLock(locked, if (locked) clock() else null, reason))
}

/** معادل Human Override برای باز کردن یک Entity قفل‌شده — همان الگوی Soft Lock در سرتاسر پروژه. */
fun unlockWithOverride(
    entity: Entity,
    logger: StateVersioningEventLogger = StateVersioningEventLogger.NoOp
): Entity {
    logger.log("lock_override", entity.id)
    return entity.copy(lock = EntityLock(locked = false))
}

/** Rule «ویرایش Entity در حالت Locked/Final بدون Override». */
fun validateEditPermission(state: EntityState, hasOverride: Boolean): ValidationIssue? {
    val isProtected = state == EntityState.LOCKED || state == EntityState.FINAL
    if (!isProtected || hasOverride) return null
    return ValidationIssue(
        severity = Severity.BLOCKING,
        message = "ویرایش در حالت $state بدون Override مجاز نیست"
    )
}

private fun nowIso8601(): String = Instant.now().toString()
