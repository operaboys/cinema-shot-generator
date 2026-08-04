package com.operaboys.cinemashotgenerator.ui.navigation

// واحد ۱۶ — فاز ۰: Back Navigation Contextual (نه Stack Pop ساده) طبق
// docs/design/README.md بخش Interactions: «Composer→Shots، Shots/Breakdown→Studio،
// SceneDetail→Studio، همه‌جای دیگر→Home». چون Composer/Shots/Breakdown/SceneDetail
// هنوز ساخته نشده‌اند، این فاز فقط قانون Fallback («همه‌جای دیگر→Home») را پیاده
// می‌کند — با یک Map مسیر→مقصد که طراحی‌اش عمداً طوری است که افزودن قانون‌های بعدی
// فقط یک ردیف جدید به backTargetsByRouteKey است، بدون تغییر resolveContextualBackTarget.
//
// کلید Map، NavDestination.route است (رشته‌ی الگوی مسیر Type-Safe، نه مقدار واقعی
// آرگومان‌ها — مثلاً برای Studio(projectId) این کلید ثابت "...Studio" است، نه
// "...Studio/proj_001").

private val homeRouteKey: String = Home::class.qualifiedName!!

/**
 * قوانین صریح (فعلاً خالی — طبق دستور کار، فقط Fallback این فاز فعال است). وقتی
 * صفحات Composer/Shots/AiStoryBreakdown/SceneDetail در فازهای بعدی ساخته شدند،
 * ردیف‌هایشان اینجا اضافه می‌شود، مثلاً:
 * ShotComposer::class.qualifiedName!! to { shotId: String -> ShotsList(...) }
 */
private val backTargetsByRouteKey: Map<String, Any> = emptyMap()

/**
 * مقصد بازگشت Contextual برای مسیر فعلی. خروجی `null` یعنی رفتار پیش‌فرض سیستم
 * (خروج از اپ/Pop عادی Activity) باید اتفاق بیفتد — این فقط برای خودِ Home است.
 * برای هر مسیر دیگری که در backTargetsByRouteKey قانون صریح ندارد (که فعلاً یعنی
 * همه‌ی ۳ مسیر ریشه‌ی دیگر: Projects/Studio/Assets)، طبق قانون Fallback بلوپرینت ۱۶
 * همیشه Home برگردانده می‌شود.
 */
fun resolveContextualBackTarget(currentRouteKey: String?): Any? {
    if (currentRouteKey == null || currentRouteKey == homeRouteKey) return null
    return backTargetsByRouteKey[currentRouteKey] ?: Home
}
