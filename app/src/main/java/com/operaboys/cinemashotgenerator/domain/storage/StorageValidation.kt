package com.operaboys.cinemashotgenerator.domain.storage

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۱۵ — Project Storage System (قوانین اعتبارسنجی جدول — منطق خالص، جدا از Room)
// منبع حقیقت: docs/blueprints/15-project-storage.md

/** Rule «project_id نامعتبر یا خالی». */
fun validateProjectId(projectId: String): ValidationIssue? {
    if (projectId.isNotBlank()) return null
    return ValidationIssue(
        severity = Severity.BLOCKING,
        message = "شناسه‌ی پروژه (project_id) الزامی است"
    )
}

/**
 * Rule «فضای ذخیره‌سازی دستگاه پر شده». بررسی واقعی فضای دیسک یک API پلتفرمی است
 * (مثلاً File.usableSpace در اندروید) که به لایه‌ی دامنه‌ی خالص تعلق ندارد — طبق الگوی
 * تزریق‌پذیر مستقر در این پروژه (idProvider/clock/logger/findDependents و مشابه)،
 * نتیجه‌ی آن بررسی به‌عنوان یک Boolean از بیرون تزریق می‌شود.
 */
fun validateStorageAvailable(hasEnoughSpace: Boolean): ValidationIssue? {
    if (hasEnoughSpace) return null
    return ValidationIssue(
        severity = Severity.BLOCKING,
        message = "فضای ذخیره‌سازی دستگاه تمام شده؛ پروژه‌های قدیمی را Export/حذف کنید"
    )
}

/** Rule «Import فایلی با نسخه‌ی Schema قدیمی‌تر». */
fun validateSchemaVersion(importedVersion: Int, currentVersion: Int): ValidationIssue? {
    if (importedVersion >= currentVersion) return null
    return ValidationIssue(
        severity = Severity.WARNING,
        message = "فایل Import شده نسخه‌ی Schema قدیمی‌تری دارد ($importedVersion در برابر $currentVersion) — نیاز به Migration"
    )
}
