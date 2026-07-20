package com.operaboys.cinemashotgenerator.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// واحد ۱۵ — Project Storage System (قدم ۱: Entity/DAO/Database — بدون اتصال به دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md
//
// NOTE: بدون ForeignKey عمداً — entityId اینجا Polymorphic است (می‌تواند به Scene،
// Shot، Asset یا هر Entity دیگری اشاره کند)، پس نمی‌تواند به یک جدول واحد FK بخورد.
// Index("entityId") برای کارایی کوئری «تمام Override های یک Entity» اضافه شد.

@Entity(tableName = "overrides", indices = [Index("entityId")])
data class OverrideEntity(
    @PrimaryKey val overrideId: String,
    val entityType: String,
    val entityId: String,
    val overrideDataJson: String, // scope + reason (طبق override-policy.md، بدون priority)
    val active: Boolean = true
)
