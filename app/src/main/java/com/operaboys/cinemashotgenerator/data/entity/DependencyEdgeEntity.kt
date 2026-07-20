package com.operaboys.cinemashotgenerator.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// واحد ۱۵ — Project Storage System (قدم ۱: Entity/DAO/Database — بدون اتصال به دامنه)
// منبع حقیقت: docs/blueprints/15-project-storage.md
//
// بلوپرینت ۱۵ این Entity را فقط با اسم آورده («DependencyEdgeEntity»)، بدون فیلد یا
// ساختار مشخص. طبق دستور این قدم، شکل آن از نوع واقعی دامنه
// (domain.validation.DependencyResolver.DependencyEdge: sourceId, targetId,
// type: DependencyType{STRONG,WEAK,REFERENCE}) الگو گرفته شد — اما عمداً بازتعریف
// مستقل شد (import از domain نشد)، چون لایه‌ی data/ نباید به domain/ وابسته باشد:
// Entity های Room مدل ذخیره‌سازی‌اند، نه مدل دامنه؛ اتصال/تبدیل بین این دو (Entity ↔
// DependencyEdge دامنه) کار قدم دوم این واحد است، نه اینجا. type به‌صورت String ذخیره
// می‌شود (نه enum Room-native)، هم‌راستا با الگوی versionType در VersionEntity.
// جزئیات کامل در docs/adr/017-unit15-project-storage-deviations.md.

@Entity(tableName = "dependency_edges", indices = [Index("sourceId"), Index("targetId")])
data class DependencyEdgeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val targetId: String,
    val type: String // "STRONG" | "WEAK" | "REFERENCE"
)
