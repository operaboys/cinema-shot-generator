package com.operaboys.cinemashotgenerator.domain.story

import java.time.Instant
import java.util.UUID

// واحد ۰۱ — عملیات Human Override (بخش ب، قوانین ۱ تا ۳)
// منبع حقیقت: docs/blueprints/01-story-and-override-v2.md (نسخه ۴)

/** رویداد Override برای ثبت در State & Versioning (واحد ۱۲). */
data class OverrideEvent(
    val type: String,
    val overrideId: String,
    val scope: OverrideScope?
)

/**
 * NOTE: Placeholder — پیاده‌سازی واقعی ثبت رویداد متعلق به واحد ۱۲ (State & Versioning)
 * است و بعداً همین Interface را پیاده می‌کند. (ثبت‌شده در ADR-001)
 */
fun interface OverrideEventLogger {
    fun log(event: OverrideEvent)

    companion object {
        /** پیش‌فرض بدون اثر؛ تا اتصال واحد ۱۲، ثبت واقعی انجام نمی‌شود. */
        val NoOp: OverrideEventLogger = OverrideEventLogger { }
    }
}

/**
 * فقط بر اساس severity خودِ قانون تصمیم می‌گیرد، نه سطح اولویت Override.
 * Rule 1 (بخش ب): قوانین Blocking هرگز قابل Override نیستند.
 */
fun checkOverridePermission(ruleSeverity: RuleSeverity): OverridePermission {
    return when (ruleSeverity) {
        RuleSeverity.BLOCKING -> OverridePermission.Denied(
            "این قانون ساختاری است؛ بدون آن پرامپت معنی ندارد"
        )
        RuleSeverity.WARNING -> OverridePermission.Allowed(
            "Warning rule — کاربر تصمیم‌گیرنده‌ی نهایی است"
        )
    }
}

/**
 * ایجاد یک Override. اگر قانون هدف Blocking باشد، Failure برمی‌گرداند (Rule 1 بخش ب).
 * ثبت Scope خودکار و الزامی است؛ reason اختیاری است (Rule 2 بخش ب).
 *
 * پارامترهای idProvider/clock/logger برای تست‌پذیری تزریق‌پذیرند؛ پیش‌فرض‌ها رفتار عملیاتی‌اند.
 */
fun createOverride(
    overrideType: OverrideType,
    scope: OverrideScope,
    targetRuleSeverity: RuleSeverity,
    reason: String? = null,
    idProvider: () -> String = ::defaultOverrideId,
    clock: () -> String = ::nowIso8601,
    logger: OverrideEventLogger = OverrideEventLogger.NoOp
): Result<HumanOverride> {
    val permission = checkOverridePermission(targetRuleSeverity)
    if (permission is OverridePermission.Denied) {
        return Result.failure(IllegalStateException(permission.reason))
    }

    val override = HumanOverride(
        overrideId = idProvider(),
        overrideType = overrideType,
        createdAt = clock(),
        scope = scope,
        reason = reason
    )

    logger.log(OverrideEvent(type = "override_created", overrideId = override.overrideId, scope = scope))

    return Result.success(override)
}

/**
 * Override هرگز حذف نمی‌شود، فقط Revoke می‌شود (Rule 3 بخش ب).
 * نسخه‌ی خالص: کپی Revoke شده برمی‌گرداند؛ بازگردانی original_value در وضعیت پروژه
 * متعلق به واحدهای ۱۲/۱۵ است. (انحراف تأییدشده از امضای بلوپرینت — ADR-001)
 */
fun revokeOverride(
    override: HumanOverride,
    reason: String?,
    clock: () -> String = ::nowIso8601,
    logger: OverrideEventLogger = OverrideEventLogger.NoOp
): Result<HumanOverride> {
    val revokedCopy = override.copy(
        active = false,
        revoked = true,
        revokedAt = clock(),
        revokedReason = reason
    )

    logger.log(OverrideEvent(type = "override_revoked", overrideId = override.overrideId, scope = null))

    return Result.success(revokedCopy)
}

/** شناسه طبق قرارداد type_identifier در naming-conventions.md، مثل ovr_a1b2c3d4e5f6 */
private fun defaultOverrideId(): String =
    "ovr_" + UUID.randomUUID().toString().replace("-", "").take(12)

private fun nowIso8601(): String = Instant.now().toString()
