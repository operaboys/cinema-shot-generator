package com.operaboys.cinemashotgenerator.ui.navigation

import kotlinx.serialization.Serializable

// واحد ۱۶ — فاز ۰: مسیرهای ریشه‌ی Navigation Graph طبق بخش «مکانیزم Navigation
// کلی» بلوپرینت ۱۶ نسخه ۷ (DDR-002). از Navigation Compose Type-Safe (مسیرهای
// @Serializable، نه رشته‌های خام دستی) استفاده شده — نسخه‌ی مدرن و توصیه‌شده‌ی
// AndroidX از Navigation Compose 2.8+ به بعد؛ کاتالوگ kotlinx.serialization پروژه
// از قبل موجود بود (واحد ۱۵)، پس هیچ وابستگی جدیدی برای همین بخش لازم نبود.
//
// مسیرهای داخلی Studio (Story/DNA/Scenes/Output) در فازهای بعدی اضافه می‌شوند — این
// فاز فقط ۴ مسیر ریشه را تعریف می‌کند.

@Serializable
data object Home

@Serializable
data object Projects

@Serializable
data class Studio(val projectId: String)

@Serializable
data object Assets
