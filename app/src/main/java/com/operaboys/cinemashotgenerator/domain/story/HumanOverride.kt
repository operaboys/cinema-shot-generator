package com.operaboys.cinemashotgenerator.domain.story

// واحد ۰۱ — Human Override (بخش ب)
// منبع حقیقت: docs/blueprints/01-story-and-override.md

enum class OverrideType { ARTISTIC, NARRATIVE, VISUAL, TECHNICAL }

/** مدل مجوز دو سطحی — هیچ سلسله‌مراتب اولویت جداگانه‌ای وجود ندارد. */
enum class RuleSeverity { BLOCKING, WARNING }

/** Scope هر Override همیشه خودکار و الزامی ثبت می‌شود. */
data class OverrideScope(
    val entityType: String,
    val entityId: String,
    val field: String,
    val originalValue: String,
    val overrideValue: String
)

data class HumanOverride(
    val overrideId: String,
    val overrideType: OverrideType,
    val createdAt: String,
    val active: Boolean = true,
    val scope: OverrideScope,
    val reason: String? = null,
    val usageCount: Int = 0,
    val lastApplied: String? = null,
    val revoked: Boolean = false,
    val revokedAt: String? = null,
    val revokedReason: String? = null
)

sealed class OverridePermission {
    data class Allowed(val reason: String) : OverridePermission()
    data class Denied(val reason: String) : OverridePermission()
}
