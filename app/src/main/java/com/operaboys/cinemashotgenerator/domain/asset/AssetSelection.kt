package com.operaboys.cinemashotgenerator.domain.asset

// واحد ۰۶ — انتخاب خودکار Outfit/Expression بر اساس شرایط صحنه
// منبع حقیقت: docs/blueprints/06-asset-and-continuity.md
//
// اولویت: Override دستی کاربر > شرط منطبق با صحنه > Fallback به Default.

/**
 * پیاده‌سازی دقیقاً طبق کد مفهومی بلوپرینت — از جمله استفاده‌ی عمدی از `.first { }`
 * (نه یک نسخه‌ی Result-محور امن‌تر): چون این تابع دقیقاً باید مطابق بلوپرینت باشد،
 * و اتصال واقعی (کنترل خطا در سطح فراخوان) به واحد ۰۵ (Shot Engine) واگذار شده.
 * (ثبت‌شده در docs/adr/003-unit06-asset-continuity-deviations.md)
 */
fun selectOutfitForScene(
    outfits: List<Outfit>,
    sceneWeather: String?,
    manualOverrideId: String? = null
): Outfit {
    if (manualOverrideId != null) {
        return outfits.first { it.id == manualOverrideId }
    }
    val matched = outfits.firstOrNull { it.condition?.weather == sceneWeather }
    return matched ?: outfits.first { it.isDefault }
}

/**
 * همان منطق selectOutfitForScene برای Expression — طبق بلوپرینت («همین منطق برای
 * expressions هم قابل استفاده است»). به‌عمد یک تابع مستقل نوشته شد نه یک Generic
 * مشترک: تنها دو محل مصرف وجود دارد و یک Interface/Generic مشترک در این مرحله
 * انتزاعی زودهنگام و غیرضروری بود (وضوح بر اختصار، طبق ai-governance قانون ۱۰).
 */
fun selectExpressionForScene(
    expressions: List<Expression>,
    sceneWeather: String?,
    manualOverrideId: String? = null
): Expression {
    if (manualOverrideId != null) {
        return expressions.first { it.id == manualOverrideId }
    }
    val matched = expressions.firstOrNull { it.condition?.weather == sceneWeather }
    return matched ?: expressions.first { it.isDefault }
}
