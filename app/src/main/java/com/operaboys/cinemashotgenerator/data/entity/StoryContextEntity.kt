package com.operaboys.cinemashotgenerator.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// واحد ۱۶ فاز ۲ — قدم ۱: لایه‌ی ذخیره‌سازی StoryContext (واحد ۰۱) که تا این قدم
// اصلاً وجود نداشت (تأییدشده با grep) — داستانی که کاربر در Story Tab می‌نویسد بعد
// از بستن اپ گم می‌شد. جزئیات کامل تصمیم در docs/adr/045-unit16-phase2-step1-story-tab.md.
//
// تصمیم مستند: فیلدهای مسطح (نه dnaDataJson مانند ProjectDnaEntity) — چون
// StoryContext (برخلاف ProjectDna) ساختار تودرتو ندارد؛ تمام فیلدهایش Scalar/Enum
// ساده‌اند، پس نیازی به بسته‌بندی JSON نیست. تنها فیلد غیر-Scalar، `genre: List<Genre>`،
// به‌صورت رشته‌ی جدا‌شده با کاما ذخیره می‌شود (نه Room TypeConverter — پروژه در هیچ‌جای
// دیگری از TypeConverter استفاده نمی‌کند؛ Mapping دستی در StoryMappers.kt هم‌الگو با
// بقیه‌ی Repository هاست).
//
// تصمیم مستند دوم: `projectId` مستقیماً @PrimaryKey است، نه یک شناسه‌ی مصنوعی جدا
// (مثل dnaId در ProjectDnaEntity) — چون رابطه‌ی Project↔StoryContext واقعاً یک‌به‌یک
// است (خروجی Story Wizard، نه یک لیست از StoryContext های متعدد) و خودِ نوع دامنه
// StoryContext هیچ فیلد id ای ندارد؛ ساختن یک شناسه‌ی مصنوعی بدون معادل دامنه، دقیقاً
// همان نوع اختراع بی‌دلیل بود که این پروژه از آن پرهیز می‌کند.
//
// version دیتابیس هنوز ۱ است (منتشرنشده — تأییدشده در ADR-019/020/044)، پس افزودن
// این Entity جدید بدون Migration رسمی مجاز است.

@Entity(
    tableName = "story_context",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["projectId"], childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class StoryContextEntity(
    @PrimaryKey val projectId: String,
    val storyType: String,
    val genre: String,
    val moodPrimary: String,
    val moodSecondary: String? = null,
    val narrativeIntensity: String,
    val visualIntent: String,
    val createdAt: String,
    val completionStatus: String
)
