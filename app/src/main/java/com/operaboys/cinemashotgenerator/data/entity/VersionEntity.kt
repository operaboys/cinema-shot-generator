package com.operaboys.cinemashotgenerator.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// واحد ۱۵ — Project Storage System (قدم ۱: Entity/DAO/Database — بدون اتصال به دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md
//
// NOTE: بدون ForeignKey عمداً — entityId اینجا هم Polymorphic است (مشابه OverrideEntity).
// Index("entityId") برای کارایی کوئری «تاریخچه‌ی نسخه‌های یک Entity» اضافه شد.

@Entity(tableName = "versions", indices = [Index("entityId")])
data class VersionEntity(
    @PrimaryKey val versionId: String,
    val entityId: String,
    val versionType: String, // "safe" | "risky"
    val snapshotJson: String,
    val createdAt: String
)
