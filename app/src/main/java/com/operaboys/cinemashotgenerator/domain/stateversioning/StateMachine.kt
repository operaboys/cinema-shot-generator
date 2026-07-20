package com.operaboys.cinemashotgenerator.domain.stateversioning

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۱۲ — State & Versioning (بخش الف: State Machine)
// منبع حقیقت: docs/blueprints/12-state-and-versioning.md

enum class EntityState { DRAFT, REVIEW, LOCKED, FINAL, ARCHIVED }

val ALLOWED_TRANSITIONS: Map<EntityState, List<EntityState>> = mapOf(
    EntityState.DRAFT to listOf(EntityState.REVIEW, EntityState.LOCKED),
    EntityState.REVIEW to listOf(EntityState.DRAFT, EntityState.LOCKED),
    EntityState.LOCKED to listOf(EntityState.FINAL, EntityState.DRAFT),
    EntityState.FINAL to listOf(EntityState.ARCHIVED),
    EntityState.ARCHIVED to emptyList()
)

fun canTransition(current: EntityState, target: EntityState): Boolean =
    target in (ALLOWED_TRANSITIONS[current] ?: emptyList())

/**
 * جدول بلوپرینت دو Rule جدا برای «مسیر غیرمجاز» و «رسیدن به Final بدون عبور از
 * Locked» فهرست کرده، و بخش پایانی «قوانین اعتبارسنجی» دوباره «انتقال وضعیت خارج از
 * مسیر مجاز» را تکرار می‌کند — اما طبق خودِ ALLOWED_TRANSITIONS، تنها راه رسیدن به
 * FINAL از LOCKED است؛ پس هر سه‌ی این عبارت‌ها دقیقاً همان یک بررسی canTransition
 * هستند، نه سه Rule مستقل. یک تابع مشترک نوشته شد؛ جزئیات در
 * docs/adr/014-unit12-state-and-versioning-deviations.md.
 */
fun validateStateTransition(current: EntityState, target: EntityState): ValidationIssue? {
    if (canTransition(current, target)) return null
    return ValidationIssue(
        severity = Severity.BLOCKING,
        message = "انتقال از $current به $target مجاز نیست"
    )
}

/**
 * Rule «انتقال به Review با فیلدهای الزامی ناقص». تشخیص این‌که کدام فیلدها الزامی و
 * ناقص‌اند، به نوع مشخص Entity (Scene/Shot/...) وابسته است و این واحد چنین دانشی
 * ندارد — طبق دستور کار، این تابع نتیجه‌ی از پیش‌محاسبه‌شده را می‌گیرد.
 */
fun validateReviewReadiness(target: EntityState, missingRequiredFields: List<String>): ValidationIssue? {
    if (target != EntityState.REVIEW || missingRequiredFields.isEmpty()) return null
    return ValidationIssue(
        severity = Severity.BLOCKING,
        message = "فیلدهای الزامی ناقص برای ورود به Review: ${missingRequiredFields.joinToString(", ")}"
    )
}

/** Rule «انتقال به Final با وابستگی‌های غیر-Final». */
fun validateDependenciesFinalized(target: EntityState, nonFinalDependencyIds: List<String>): ValidationIssue? {
    if (target != EntityState.FINAL || nonFinalDependencyIds.isEmpty()) return null
    return ValidationIssue(
        severity = Severity.BLOCKING,
        message = "وابستگی‌های غیر-Final مانع رسیدن به Final هستند: ${nonFinalDependencyIds.joinToString(", ")}"
    )
}
