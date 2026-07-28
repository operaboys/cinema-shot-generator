package com.operaboys.cinemashotgenerator.domain.promptengine

// واحد ۱۱ — فاز ۲: سیستم اولویت‌بندی
// منبع حقیقت: docs/blueprints/11-prompt-engineering-core-v2.md
// دقیقاً طبق کد مفهومی بلوپرینت — Generic و بدون وابستگی خارجی.
//
// این اولویت‌بندی («Human Override > Shot > Scene > DNA») مستقل از سیستم Override
// ساده‌شده در واحد ۰۱ است — آنجا درباره‌ی مجاز بودن Override بود (Blocking/Warning)؛
// اینجا درباره‌ی ترتیب اعمال مقادیر وقتی چند منبع همزمان یک فیلد را تعیین می‌کنند.

enum class PriorityLevel { HUMAN_OVERRIDE, SHOT_SPECIFIC, CHARACTER_CONTINUITY, SCENE_CONTEXT, PROJECT_DNA }

/** ترتیب اولویت از بالا به پایین — اولین مقدار غیرخالی در این ترتیب برنده است. */
val PRIORITY_ORDER = listOf(
    PriorityLevel.HUMAN_OVERRIDE,
    PriorityLevel.SHOT_SPECIFIC,
    PriorityLevel.CHARACTER_CONTINUITY,
    PriorityLevel.SCENE_CONTEXT,
    PriorityLevel.PROJECT_DNA
)

fun <T> resolvePriority(valuesByLevel: Map<PriorityLevel, T?>): T? {
    for (level in PRIORITY_ORDER) {
        valuesByLevel[level]?.let { return it }
    }
    return null
}
