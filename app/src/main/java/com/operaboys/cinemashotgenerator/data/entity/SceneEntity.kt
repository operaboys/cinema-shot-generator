package com.operaboys.cinemashotgenerator.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// واحد ۱۵ — Project Storage System (قدم ۱: Entity/DAO/Database — بدون اتصال به دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md

@Entity(
    tableName = "scenes",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["projectId"], childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("projectId")]
)
data class SceneEntity(
    @PrimaryKey val sceneId: String,
    val projectId: String,
    val sceneDataJson: String // structured_parts سریالایز‌شده
)
