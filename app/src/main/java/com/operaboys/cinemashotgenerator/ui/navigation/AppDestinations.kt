package com.operaboys.cinemashotgenerator.ui.navigation

import com.operaboys.cinemashotgenerator.ui.assets.AssetKind
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

// واحد ۱۶ فاز ۲ — قدم ۲: مسیر مستقل سطح‌بالا (نه یک Sub-view داخل Tab «داستان») —
// طبق docs/design/README.md بخش «۴. AI Story Breakdown»، این صفحه Header/Back
// مستقل خودش را دارد (دقیقاً مثل ۴ مسیر ریشه‌ی بالا)، نه یک بخش اسکرول‌شونده‌ی
// دیگر داخل Story Tab. از Story Tab (دکمه‌ی «برو به تفکیک داستان با AI») و در
// آینده از Nav Drawer (فعلاً «به‌زودی») در دسترس است. جزئیات کامل در
// docs/adr/046-unit16-phase2-step2-ai-story-breakdown.md.
@Serializable
data class AiStoryBreakdown(val projectId: String)

// واحد ۱۶ فاز ۳ — قدم ۲: مسیر مستقل سطح‌بالا برای فرم ساخت Asset — دقیقاً هم‌الگو
// با AiStoryBreakdown بالا (Header/Back مستقل خودش، نه یک Sub-view داخل صفحه‌ی
// Assets). `projectId` عمداً در این مسیر نیامده — خودِ صفحه‌ی Assets هم آرگومان
// projectId ندارد و از PLACEHOLDER_ACTIVE_PROJECT_ID استفاده می‌کند
// (docs/adr/048)؛ فرم هم دقیقاً همان الگو را ادامه می‌دهد تا محدودیت شناخته‌شده‌ی
// موجود دوباره تکرار نشود، نه یک محدودیت تازه. `kind` مشخص می‌کند کدام‌یک از سه
// فرم (Character/Location/Object) باز شود — طبق فیلتر فعال صفحه‌ی Assets وقتی
// دکمه‌ی شناور لمس می‌شود.
@Serializable
data class AssetForm(val kind: AssetKind)

// واحد ۱۶ فاز ۴ — قدم ۱: مسیر مستقل سطح‌بالا برای Scene Detail — دقیقاً هم‌الگو با
// AiStoryBreakdown/AssetForm بالا (Header/Back مستقل خودش)، طبق تصریح صریح
// docs/design/README.md بخش Interactions («Composer→Shots، Shots/Breakdown→Studio،
// SceneDetail→Studio، همه‌جای دیگر→Home» — همان کامنت ui/navigation/BackNavigation.kt).
// برخلاف AssetForm، اینجا `projectId` لازم است چون Scene Detail برای برگشت باید
// دقیقاً به همان Studio(projectId) برود (نه یک PLACEHOLDER — این مسیر همیشه از
// داخل Studio یک پروژه‌ی مشخص باز می‌شود).
@Serializable
data class SceneDetail(val projectId: String, val sceneId: String)
