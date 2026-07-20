package com.operaboys.cinemashotgenerator.domain.storage

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۱۵ — Project Storage System (یکپارچگی ارجاعی — منطق خالص، جدا از Room)
// منبع حقیقت: docs/blueprints/15-project-storage.md
//
// طبق دستور این قدم، پارامترها نوع‌های ساده‌اند (لیست/Set از ID، نه Entity واقعی و نه
// یک نوع تجمیعی «ProjectData» که بلوپرینت هم هیچ‌جا تعریفش نکرده)، تا این تابع بدون
// وابستگی به Room قابل تست باشد.

data class IntegrityIssue(val source: String, val brokenReferenceTo: String, val message: String)

/**
 * فقط همان فیلدهایی از Shot که validateReferentialIntegrity واقعاً به آن‌ها نیاز دارد
 * — نه یک Shot/ShotEntity کامل.
 */
data class ShotReferenceData(
    val shotId: String,
    val sceneId: String,
    val characterIds: List<String> = emptyList(),
    val objectIds: List<String> = emptyList(),
    val locationIds: List<String> = emptyList()
)

/**
 * بررسی کامل یکپارچگی ارجاعی پروژه قبل از ذخیره یا Import — شامل سه نوع ارجاع رایج:
 * Shot→Scene، Shot→Character، Shot→Object/Location.
 */
fun validateReferentialIntegrity(
    shots: List<ShotReferenceData>,
    sceneIds: Set<String>,
    assetIds: Set<String>
): List<IntegrityIssue> {
    val issues = mutableListOf<IntegrityIssue>()

    shots.forEach { shot ->
        // Shot → Scene
        if (shot.sceneId !in sceneIds) {
            issues += IntegrityIssue(shot.shotId, shot.sceneId, "این شات به صحنه‌ای ارجاع می‌دهد که وجود ندارد")
        }
        // Shot → Character / Object / Location
        (shot.characterIds + shot.objectIds + shot.locationIds)
            .filterNot { it in assetIds }
            .forEach { missingId ->
                issues += IntegrityIssue(shot.shotId, missingId, "ارجاع به Asset حذف‌شده یا ناموجود")
            }
    }
    return issues
}

/**
 * Rule «ارجاع شکسته (Broken Reference) به Asset حذف‌شده» با ValidationIssue سراسری
 * واحد ۰۷ — یک Wrapper نازک روی validateReferentialIntegrity که شکل غنی‌تر
 * IntegrityIssue را برای پایپ‌لاین Rule-based پروژه (که همه‌جا از ValidationIssue/
 * Severity استفاده می‌کند) به Severity.BLOCKING نگاشت می‌کند؛ خودِ validateReferentialIntegrity
 * دست‌نخورده ماند تا شکل دقیق بلوپرینت (source/brokenReferenceTo/message) حفظ شود.
 */
fun validateReferentialIntegrityAsIssues(
    shots: List<ShotReferenceData>,
    sceneIds: Set<String>,
    assetIds: Set<String>
): List<ValidationIssue> {
    return validateReferentialIntegrity(shots, sceneIds, assetIds).map {
        ValidationIssue(
            severity = Severity.BLOCKING,
            field = it.source,
            message = "${it.message} (${it.brokenReferenceTo})"
        )
    }
}
