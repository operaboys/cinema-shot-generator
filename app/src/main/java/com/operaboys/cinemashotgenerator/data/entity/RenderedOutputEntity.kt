package com.operaboys.cinemashotgenerator.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// واحد ۱۵ — Project Storage System (قدم ۱: Entity/DAO/Database — بدون اتصال به دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md
//
// NOTE: Index("promptBlueprintId") به کد نمونه‌ی بلوپرینت اضافه شد — بدون آن، Room
// روی ستون ForeignKey هشدار «ممکن است باعث Full Table Scan شود» می‌دهد؛ این فقط یک
// بهینه‌سازی فنی استاندارد Room است، نه تغییر در مدل داده.

@Entity(
    tableName = "rendered_outputs",
    foreignKeys = [ForeignKey(
        entity = PromptBlueprintEntity::class,
        parentColumns = ["promptBlueprintId"], childColumns = ["promptBlueprintId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("promptBlueprintId")]
)
data class RenderedOutputEntity(
    @PrimaryKey val renderedOutputId: String,
    val promptBlueprintId: String,
    val modelProfileId: String,
    val formattedPrompt: String,
    val language: String
)
