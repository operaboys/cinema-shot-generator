package com.operaboys.cinemashotgenerator.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// واحد ۱۵ — Project Storage System (قدم ۱: Entity/DAO/Database — بدون اتصال به دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md
//
// NOTE: بدون ForeignKey عمداً — entityId اینجا هم Polymorphic است (مشابه OverrideEntity).
// Index("entityId") برای کارایی کوئری «تاریخچه‌ی نسخه‌های یک Entity» اضافه شد.
//
// NOTE (قدم ۲): changeSummary/modifiedFieldsJson/parentVersionId در قدم ۱ نبودند —
// نمونه‌ی Entity خودِ بلوپرینت هم این سه فیلد را نداشت، با این‌که domain.stateversioning.
// EntityVersion (واحد ۱۲) هر سه را دارد. برای این‌که هیچ داده‌ای حین نگاشت
// EntityVersion↔VersionEntity گم نشود، این سه فیلد اضافه شدند (با مقدار پیش‌فرض، پس
// Backward-Compatible) — دقیقاً هم‌جنس با تصمیم افزودن EventLogEntity (تأییدشده)، چون
// Schema هنوز version=1 و منتشرنشده است. جزئیات کامل در
// docs/adr/018-unit15-step2-repository-deviations.md.

@Entity(tableName = "versions", indices = [Index("entityId")])
data class VersionEntity(
    @PrimaryKey val versionId: String,
    val entityId: String,
    val versionType: String, // "safe" | "risky"
    val snapshotJson: String,
    val createdAt: String,
    val changeSummary: String = "",
    val modifiedFieldsJson: String = "[]",
    val parentVersionId: String? = null
)
