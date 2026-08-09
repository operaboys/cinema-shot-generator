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

// MIGRATED (رفع G1 ممیزی post-Unit16، docs/adr/061-...): `initialTab` اضافه
// شد — دقیقاً هم‌الگو با `SceneDetail.initialTab` (رشته‌ی نام `StudioTab`،
// پیش‌فرض Backward Compatible «STORY» یعنی دقیقاً همان رفتار قبلی برای هر
// فراخوان موجودی که این پارامتر را نمی‌دهد). لازم برای اتصال واقعی لینک‌های
// Nav Drawer «مدیریت DNA»/«صحنه‌ها»/«شات‌ها» به Tab درست، نه همیشه Tab «داستان».
@Serializable
data class Studio(val projectId: String, val initialTab: String = "STORY")

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
//
// MIGRATED (رفع G7 ممیزی post-Unit16، docs/adr/061-...): `existingAssetId`
// اضافه شد — دقیقاً هم‌الگو با `ShotComposer.shotId` (واحد ۱۶ فاز ۴ قدم ۲):
// `null` یعنی «Asset جدید» (رفتار قبلی، بدون تغییر)، غیر-null یعنی «بارگذاری و
// ویرایش Asset موجود». تا این قدم، لمس یک کارت Asset موجود در صفحه‌ی Assets
// اصلاً هیچ onClick ای نداشت — این تنها راه واقعی رسیدن به این مسیر با یک
// شناسه‌ی موجود است.
@Serializable
data class AssetForm(val kind: AssetKind, val existingAssetId: String? = null)

// واحد ۱۶ فاز ۴ — قدم ۱: مسیر مستقل سطح‌بالا برای Scene Detail — دقیقاً هم‌الگو با
// AiStoryBreakdown/AssetForm بالا (Header/Back مستقل خودش)، طبق تصریح صریح
// docs/design/README.md بخش Interactions («Composer→Shots، Shots/Breakdown→Studio،
// SceneDetail→Studio، همه‌جای دیگر→Home» — همان کامنت ui/navigation/BackNavigation.kt).
// برخلاف AssetForm، اینجا `projectId` لازم است چون Scene Detail برای برگشت باید
// دقیقاً به همان Studio(projectId) برود (نه یک PLACEHOLDER — این مسیر همیشه از
// داخل Studio یک پروژه‌ی مشخص باز می‌شود).
//
// MIGRATED (واحد ۱۶ فاز ۴ قدم ۲): `initialTab` اضافه شد — رشته‌ی نام
// `SceneDetailTab` (پیش‌فرض «OVERVIEW»، Backward Compatible). لازم برای رعایت
// دقیق «Composer→Shots» (طبق کامنت بالا)؛ بدون آن، بازگشت از Shot Composer همیشه
// Tab «کلیات» را نشان می‌داد، نه Tab «شات‌ها» که کاربر واقعاً از آن آمده بود.
@Serializable
data class SceneDetail(val projectId: String, val sceneId: String, val initialTab: String = "OVERVIEW")

// واحد ۱۶ فاز ۴ — قدم ۲ — بخش ب: مسیر مستقل سطح‌بالا برای Shot Composer — دقیقاً
// هم‌الگو با SceneDetail بالا. `shotId` عمداً nullable است: null یعنی «شات جدید»
// (طبق بخش ب دستور کار: «دکمه‌ی شات جدید → Navigate به Shot Composer خالی/جدید» —
// برخلاف Scene که بدون فرم بلافاصله ساخته می‌شود، Shot طبق بلوپرینت یک فرم/Panel
// مستقل خودش را دارد). `sceneNumber`/`sceneDisplayTitle` به‌جای بارگذاری مجدد Scene
// از این مسیر مستقیماً منتقل می‌شوند — Scene Detail از قبل این دو مقدار را دارد،
// یک fetch تکراری لازم نیست.
@Serializable
data class ShotComposer(
    val projectId: String,
    val sceneId: String,
    val sceneNumber: Int,
    val sceneDisplayTitle: String,
    val shotId: String? = null
)

// واحد ۱۶ فاز ۵ — قدم ۱: مسیر مستقل سطح‌بالا برای صفحه‌ی Validation — دقیقاً
// هم‌الگو با SceneDetail/ShotComposer بالا (Header/Back مستقل خودش). برخلاف
// ShotComposer، `shotId` اینجا nullable نیست — Validation فقط برای یک Shot از
// قبل ذخیره‌شده معنا دارد (شات هنوز-ذخیره‌نشده چیزی برای Validate کردن در سطح
// Repository ندارد). طبق تصمیم مستند (ADR-055)، ورودی این مسیر از داخل خودِ
// Shot Composer است (نه Nav Drawer — «اعتبارسنجی» در Drawer هنوز placeholder
// «به‌زودی» می‌ماند، چون بدون Shot مشخص معنایی ندارد).
// `sceneNumber`/`sceneDisplayTitle` عیناً هم‌الگو با ShotComposer تکرار شدند —
// نه برای نمایش مستقیم در این صفحه، بلکه چون «برگشت» این صفحه باید دقیقاً به
// همان ShotComposer(sceneNumber, sceneDisplayTitle) واقعی برگردد (طبق قاعده‌ی
// صریح معماری «Back Navigation Contextual، نه popBackStack ساده» —
// ui/navigation/MainScaffold.kt)؛ بدون این دو فیلد، بازسازی یک مسیر ShotComposer
// معتبر برای Navigate ممکن نبود.
@Serializable
data class Validation(
    val projectId: String,
    val sceneId: String,
    val sceneNumber: Int,
    val sceneDisplayTitle: String,
    val shotId: String
)

// واحد ۱۶ فاز ۵ — قدم ۳ (آخرین قدم فاز ۵، طبق ADR-056 شامل هر دو وظیفه‌ی «تولید
// Prompt» و «تحویل خروجی»): مسیر مستقل Output Delivery — دقیقاً هم‌شکل با
// Validation بالا (همان دلیل‌ها: shotId غیر-nullable، sceneNumber/sceneDisplayTitle
// فقط برای بازسازی معتبر مسیر ShotComposer در برگشت). طبق دستور کار این قدم، نقطه‌ی
// ورود می‌تواند هم از Validation و هم مستقیم از Shot Composer باشد — اما مقصد
// «برگشت» در هر دو حالت یکسان و ثابت است (به خودِ ShotComposer)، دقیقاً هم‌الگو با
// Validation، نه دو مقصد متفاوت بسته به نقطه‌ی ورود (که با قاعده‌ی «Back
// Navigation Contextual» این پروژه پیچیدگی غیرضروری اضافه می‌کرد).
@Serializable
data class OutputDelivery(
    val projectId: String,
    val sceneId: String,
    val sceneNumber: Int,
    val sceneDisplayTitle: String,
    val shotId: String
)

// واحد ۱۶ فاز ۶ — قدم ۱: مسیر مستقل سطح‌بالا برای صفحه‌ی Settings — دقیقاً هم‌الگو
// با Assets بالا (`data object` بدون آرگومان، نه یک `data class`): Settings یک
// مفهوم کاملاً سراسری/مستقل از پروژه است (طبق docs/design/README.md بخش «۱۱.
// Settings» — همه‌ی مقادیرش از WorkflowViewModel/DataStore می‌آیند، نه از یک
// projectId مشخص). ورودی از Nav Drawer (گروه SYSTEM) است؛ «برگشت» طبق قاعده‌ی
// Fallback عمومی («همه‌جای دیگر→Home») به Home می‌رود — نیازی به قانون
// اختصاصی در resolveContextualBackTarget نیست، دقیقاً هم‌الگو با Assets.
@Serializable
data object Settings

// واحد ۱۶ فاز ۶ — قدم ۲ (آخرین قدم کل واحد ۱۶): مسیر مستقل سطح‌بالا برای صفحه‌ی
// Backups — هم‌الگو با Settings بالا (`data object` بدون آرگومان). بر خلاف
// Settings (که کاملاً سراسری است)، این صفحه واقعاً به یک projectId نیاز دارد
// (BackupManager per-project است، طبق docs/blueprints/15-project-storage.md) —
// اما آن projectId عمداً در خودِ این مسیر نیامده (تا هم‌الگو با Settings/Assets
// باقی بماند و از Nav Drawer یکسان در دسترس باشد)؛ در عوض از
// WorkflowState.projectId (Session فعال Studio، اگر باشد) در AppNavHost خوانده
// می‌شود — دقیقاً همان الگوی PLACEHOLDER_ACTIVE_PROJECT_ID که ADR-048 برای Assets
// استفاده کرد.
@Serializable
data object Backups
