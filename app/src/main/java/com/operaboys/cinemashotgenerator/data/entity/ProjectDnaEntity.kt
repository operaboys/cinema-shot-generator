package com.operaboys.cinemashotgenerator.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// واحد ۱۵ — Project Storage System (قدم ۳، زیرقدم ۱: اتصال ProjectDna)
// منبع حقیقت: docs/blueprints/15-project-storage.md، docs/blueprints/02-dna-manager-v2.md
//
// این Entity در قدم ۱ وجود نداشت — بلوپرینت ۱۵ اصلاً هیچ Entity ای برای ProjectDna
// فهرست نکرده بود (فقط Project/Scene/Shot/Asset/PromptBlueprint/RenderedOutput/
// Override/Version/DependencyEdge). طبق همان الگوی «هر Aggregate دامنه یک جدول
// مستقل با فیلد xDataJson» که در SceneEntity/ShotEntity/AssetEntity/
// PromptBlueprintEntity به کار رفته (نه فشرده‌کردن dnaDataJson داخل ProjectEntity)،
// یک Entity مستقل با ForeignKey به Project اضافه شد — دقیقاً هم‌جنس با تصمیم افزودن
// EventLogEntity در قدم ۲ (Schema هنوز version=1، منتشرنشده). جزئیات کامل در
// docs/adr/019-unit15-step3a-dna-asset-deviations.md.

@Entity(
    tableName = "project_dna",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["projectId"], childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("projectId")]
)
data class ProjectDnaEntity(
    @PrimaryKey val dnaId: String,
    val projectId: String,
    val dnaDataJson: String
)
