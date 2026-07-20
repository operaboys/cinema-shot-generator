package com.operaboys.cinemashotgenerator.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// واحد ۱۵ — Project Storage System (قدم ۳، زیرقدم ۲: اتصال AudioContext)
// منبع حقیقت: docs/blueprints/15-project-storage.md، docs/blueprints/10-audio-context.md
//
// این Entity هم مثل ProjectDnaEntity (زیرقدم ۱) در بلوپرینت ۱۵ فهرست نشده بود.
// همان الگوی مستقر «هر Aggregate دامنه یک جدول مستقل با فیلد xDataJson»، با
// ForeignKey به ShotEntity (هر AudioContext دقیقاً متعلق به یک Shot است، طبق
// domain.audio.AudioContext.shotId) — Schema هنوز version=1 و منتشرنشده، پس این
// افزودن نیازی به سؤال جدید نداشت (هم‌جنس تصمیمات تأییدشده‌ی قبلی EventLogEntity/
// VersionEntity/ProjectDnaEntity). جزئیات در docs/adr/020-unit15-step3b-audio-collectdata-deviations.md.

@Entity(
    tableName = "audio_contexts",
    foreignKeys = [ForeignKey(
        entity = ShotEntity::class,
        parentColumns = ["shotId"], childColumns = ["shotId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("shotId")]
)
data class AudioContextEntity(
    @PrimaryKey val audioContextId: String,
    val shotId: String,
    val audioContextDataJson: String
)
