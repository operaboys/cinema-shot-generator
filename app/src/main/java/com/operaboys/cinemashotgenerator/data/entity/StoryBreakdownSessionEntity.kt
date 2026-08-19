package com.operaboys.cinemashotgenerator.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// واحد ۱۶ فاز ۲ — قدم ۲: پیش‌نویس در‌حال‌کار «AI Story Breakdown» (واحد ۰۱ب، فاز ۱:
// داستان آزاد + Stepper هدف‌شات/ثانیه‌به‌ازای‌شات). جزئیات کامل تصمیم در
// docs/adr/046-unit16-phase2-step2-ai-story-breakdown.md.
//
// تصمیم مستند: Entity کاملاً مستقل از StoryContextEntity (نه افزودن این سه فیلد به
// آن جدول) — این دو مفهوم متفاوت‌اند: StoryContextEntity پاسخ‌های ۵ سؤال Story
// Wizard (واحد ۰۱) را ذخیره می‌کند، این Entity یک پیش‌نویس در‌حال‌کار غیرنهایی
// `StoryBreakdownRequest` (واحد ۰۱ب) است — ترکیب‌شان در یک جدول یعنی دو مفهوم
// نامرتبط را مصنوعاً به هم می‌چسباند. `projectId` مستقیماً PrimaryKey است (همان
// دلیل StoryContextEntity: رابطه یک‌به‌یک، بدون شناسه‌ی مصنوعی بی‌دلیل).
//
// previewLanguageEnabled (قدم ۱ از ۳ زیرقدم «سیستم Preview دوزبانه‌ی پرامپت»،
// ADR-121): آیا کاربر می‌خواهد AI بیرونی علاوه بر description انگلیسی
// (همیشه پایه‌ی پرامپت نهایی)، یک نسخه‌ی فارسی هم فقط برای Preview خودش
// بفرستد. این ستون واقعی (نه JSON خام) است — افزودنش نیازمند AppDatabase
// MIGRATION_1_2 بود (جزئیات کامل در AppDatabase.kt/ADR-121).

@Entity(
    tableName = "story_breakdown_session",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["projectId"], childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class StoryBreakdownSessionEntity(
    @PrimaryKey val projectId: String,
    val freeformStory: String,
    val targetShotCount: Int,
    val defaultShotDurationSeconds: Float,
    val previewLanguageEnabled: Boolean = false
)
