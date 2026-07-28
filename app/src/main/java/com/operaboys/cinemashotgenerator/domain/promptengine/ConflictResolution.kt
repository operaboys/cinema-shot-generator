package com.operaboys.cinemashotgenerator.domain.promptengine

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۱۱ — فاز ۳: حل تضاد
// منبع حقیقت: docs/blueprints/11-prompt-engineering-core-v2.md
//
// منطق تشخیص تضاد کاملاً متعلق به واحد ۰۷ است و اینجا تکرار نمی‌شود — این فایل فقط
// خروجی واحد ۰۷ (List<ValidationIssue>؛ هیچ Rule-level function ای در پروژه از
// ValidationReport استفاده نمی‌کند، طبق یافته‌ی ADR-010) را به دو فیلد PromptBlueprint
// (conflictsResolved، warnings) تبدیل می‌کند.

data class ConflictResolutionSummary(
    val conflictsResolved: Int,
    val warnings: List<ValidationIssue>
)

/**
 * issues لیست خروجی واحد ۰۷ (از Rule های Validation/Logic Conflict Checker مربوط به
 * این Shot) است. فقط موارد WARNING به‌عنوان «تضاد حل‌شده و ثبت‌شده» شمرده می‌شوند —
 * موارد BLOCKING طبق تعریف پروژه یعنی فرآیند اصلاً به این مرحله نمی‌رسد (Blocking:
 * فرآیند متوقف می‌شود)، پس در این خلاصه نمایندگی نمی‌شوند؛ PromptBlueprint هم اصلاً
 * فیلدی برای نگهداری خطاهای Blocking ندارد.
 */
fun summarizeConflictResolution(issues: List<ValidationIssue>): ConflictResolutionSummary {
    val warnings = issues.filter { it.severity == Severity.WARNING }
    return ConflictResolutionSummary(conflictsResolved = warnings.size, warnings = warnings)
}
