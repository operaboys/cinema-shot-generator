package com.operaboys.cinemashotgenerator.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// واحد ۱۵ — Project Storage System (قدم ۱: Entity/DAO/Database — بدون اتصال به دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md
//
// NOTE: بلوپرینت برای shotId نه ForeignKey نه Index تعریف کرده بود (برخلاف
// RenderedOutputEntity که برای promptBlueprintId مشابه FK دارد) — Index("shotId")
// برای کارایی کوئری اضافه شد (بدون FK، چون بلوپرینت هم آن را نخواسته بود؛ جزئیات در
// ADR-017).

@Entity(tableName = "prompt_blueprints", indices = [Index("shotId")])
data class PromptBlueprintEntity(
    @PrimaryKey val promptBlueprintId: String,
    val shotId: String,
    val structuredPartsJson: String,
    val seed: Int?
)
