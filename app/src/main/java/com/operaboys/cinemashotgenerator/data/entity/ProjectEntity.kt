package com.operaboys.cinemashotgenerator.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// واحد ۱۵ — Project Storage System (قدم ۱: Entity/DAO/Database — بدون اتصال به دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val projectId: String,
    val projectName: String,
    val createdAt: String,
    val lastModified: String,
    val uiLanguage: String = "fa", // زبان UI، مستقل از prompt_language
    // MIGRATED (واحد ۱۶ فاز ۱، docs/adr/044-unit16-phase1-app-shell.md): وضعیت
    // EntityState (واحد ۱۲) — رشته‌ی خام (هم‌الگو با narrativeRole و مشابه در
    // SceneDto)؛ در انتهای لیست فیلدها اضافه شد تا هر سازنده‌ی Positional موجود
    // (تست‌ها) بدون تغییر کامپایل شود. version دیتابیس هنوز ۱ است (منتشرنشده) —
    // نیازی به Migration نیست.
    val state: String = "DRAFT"
)
