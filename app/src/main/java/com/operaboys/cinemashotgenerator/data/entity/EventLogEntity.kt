package com.operaboys.cinemashotgenerator.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// واحد ۱۵ — Project Storage System (قدم ۲: اتصال واقعی StateVersioningEventLogger واحد ۱۲)
// منبع حقیقت: docs/blueprints/15-project-storage.md، docs/blueprints/12-state-and-versioning.md
//
// این Entity در قدم ۱ وجود نداشت (خارج از فهرست آن قدم بود؛ ثبت‌شده به‌عنوان بدهی در
// ADR-017) — با تأیید صریح معمار در همین قدم اضافه شد تا StateVersioningEventLogger
// (واحد ۱۲) یک محل ذخیره‌سازی واقعی داشته باشد. Schema همچنان version=1 می‌ماند چون
// هنوز هیچ نسخه‌ای منتشر نشده. بدون ForeignKey عمداً — entityId اینجا هم Polymorphic
// است (مشابه OverrideEntity/VersionEntity).

@Entity(tableName = "event_log", indices = [Index("entityId")])
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val entityId: String,
    val timestamp: String
)
