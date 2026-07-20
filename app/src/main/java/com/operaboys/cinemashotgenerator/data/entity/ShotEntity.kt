package com.operaboys.cinemashotgenerator.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// واحد ۱۵ — Project Storage System (قدم ۱: Entity/DAO/Database — بدون اتصال به دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md

@Entity(
    tableName = "shots",
    foreignKeys = [ForeignKey(
        entity = SceneEntity::class,
        parentColumns = ["sceneId"], childColumns = ["sceneId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sceneId")]
)
data class ShotEntity(
    @PrimaryKey val shotId: String,
    val sceneId: String,
    val shotDataJson: String
)
