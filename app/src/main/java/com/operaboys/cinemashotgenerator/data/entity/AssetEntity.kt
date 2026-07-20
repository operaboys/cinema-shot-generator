package com.operaboys.cinemashotgenerator.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// واحد ۱۵ — Project Storage System (قدم ۱: Entity/DAO/Database — بدون اتصال به دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md
//
// NOTE: بلوپرینت برای این Entity هیچ Index معرفی نکرده بود، اما بخش «مقیاس‌پذیری» همین
// سند صریحاً می‌گوید Asset Library باید ۱۰,۰۰۰+ آیتم را بدون افت کارایی پشتیبانی کند و
// کوئری‌ها باید Index مناسب داشته باشند — پس Index("projectId") اضافه شد (بدون
// ForeignKey، چون بلوپرینت هم برایش FK تعریف نکرده بود؛ جزئیات در ADR-017).

@Entity(tableName = "assets", indices = [Index("projectId")])
data class AssetEntity(
    @PrimaryKey val assetId: String,
    val projectId: String,
    val assetType: String, // "character" | "location" | "object"
    val assetDataJson: String
)
