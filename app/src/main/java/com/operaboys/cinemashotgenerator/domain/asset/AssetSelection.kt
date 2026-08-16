package com.operaboys.cinemashotgenerator.domain.asset

// واحد ۰۶ — انتخاب خودکار Outfit/Expression بر اساس شرایط صحنه
// منبع حقیقت: docs/blueprints/06-asset-and-continuity-v2.md
//
// اولویت: Override دستی کاربر > شرط منطبق با صحنه > Fallback به Default.
//
// تکمیل G5 (ADR-096، محدودیت مستندشده‌ی ADR-095 را می‌بندد): بلوپرینت خودش
// «کد مفهومی» selectOutfitForScene را فقط با weather نوشته بود (نسخه‌ی این
// فایل پیش از این قدم عیناً همان بود)، در حالی که OutfitCondition از همان
// ابتدا سه فیلد دارد (weather/timeOfDay/locationType) — بلوپرینت هیچ منطق
// ترکیب چندفیلدی مشخص نکرده، تصمیمش را به پیاده‌سازی واگذار کرده. منطق
// انتخاب‌شده: AND روی هر فیلد پرشده‌ی condition (matchesSceneCondition پایین)
// — نه امتیازدهی/اولویت ثابت؛ چون یک condition معنایش «این شرط دقیق باید
// برقرار باشد»، نه «نزدیک‌ترین تطابق»: یک Rain Coat نباید فقط چون locationType
// اتفاقی مطابقت داشت، در هوای غیربارانی انتخاب شود.

/**
 * تطابق شرط یک Outfit/Expression با صحنه: هر فیلد پرشده‌ی `condition` باید
 * دقیقاً با مقدار متناظر صحنه یکی باشد (AND)؛ فیلد null در condition یعنی
 * «قیدی روی این بعد نیست» (Wildcard)، نه «باید صحنه هم null باشد». یک
 * `OutfitCondition` غیر-null اما با هر سه فیلد null (که عملاً نباید از UI
 * برسد — CharacterAssetFormViewModel چنین حالتی را به `null` نرمال می‌کند)
 * هم مثل خودِ `condition == null` هرگز تطبیق نمی‌دهد — یک condition واقعی
 * باید حداقل یک قید داشته باشد تا معنادار باشد.
 */
private fun matchesSceneCondition(
    condition: OutfitCondition?,
    sceneWeather: String?,
    sceneTimeOfDay: String?,
    sceneLocationType: String?
): Boolean {
    if (condition == null) return false
    if (condition.weather == null && condition.timeOfDay == null && condition.locationType == null) return false
    if (condition.weather != null && condition.weather != sceneWeather) return false
    if (condition.timeOfDay != null && condition.timeOfDay != sceneTimeOfDay) return false
    if (condition.locationType != null && condition.locationType != sceneLocationType) return false
    return true
}

/**
 * پیاده‌سازی پایه طبق کد مفهومی بلوپرینت — از جمله استفاده‌ی عمدی از `.first { }`
 * (نه یک نسخه‌ی Result-محور امن‌تر): چون این تابع دقیقاً باید مطابق بلوپرینت باشد،
 * و اتصال واقعی (کنترل خطا در سطح فراخوان) به واحد ۰۵ (Shot Engine) واگذار شده.
 * (ثبت‌شده در docs/adr/003-unit06-asset-continuity-deviations.md). sceneTimeOfDay/
 * sceneLocationType اختیاری‌اند (پیش‌فرض null، رفتار قدیمی weather-only را برای هر
 * فراخوان‌کننده‌ای که این دو را پاس نمی‌دهد بدون تغییر نگه می‌دارند) — ADR-096.
 */
fun selectOutfitForScene(
    outfits: List<Outfit>,
    sceneWeather: String?,
    sceneTimeOfDay: String? = null,
    sceneLocationType: String? = null,
    manualOverrideId: String? = null
): Outfit {
    if (manualOverrideId != null) {
        return outfits.first { it.id == manualOverrideId }
    }
    val matched = outfits.firstOrNull { matchesSceneCondition(it.condition, sceneWeather, sceneTimeOfDay, sceneLocationType) }
    return matched ?: outfits.first { it.isDefault }
}

/**
 * همان منطق selectOutfitForScene برای Expression — طبق بلوپرینت («همین منطق برای
 * expressions هم قابل استفاده است»). به‌عمد یک تابع مستقل نوشته شد نه یک Generic
 * مشترک: تنها دو محل مصرف وجود دارد و یک Interface/Generic مشترک در این مرحله
 * انتزاعی زودهنگام و غیرضروری بود (وضوح بر اختصار، طبق ai-governance قانون ۱۰).
 *
 * یادداشت G5/ADR-096: با grep تأیید شد این تابع صفر فراخوان‌کننده‌ی واقعی دارد
 * (فقط تست اختصاصی خودش، AssetSelectionTest.kt) — کد مرده‌ی احتمالی، نه یک باگ
 * فعال. امضایش برای تقارن با selectOutfitForScene گسترش یافت (طبق دستور صریح
 * این قدم)، اما وصل‌کردنش به یک مصرف‌کننده‌ی واقعی (اگر اصلاً لازم باشد) خارج از
 * Scope همین قدم است.
 */
fun selectExpressionForScene(
    expressions: List<Expression>,
    sceneWeather: String?,
    sceneTimeOfDay: String? = null,
    sceneLocationType: String? = null,
    manualOverrideId: String? = null
): Expression {
    if (manualOverrideId != null) {
        return expressions.first { it.id == manualOverrideId }
    }
    val matched = expressions.firstOrNull { matchesSceneCondition(it.condition, sceneWeather, sceneTimeOfDay, sceneLocationType) }
    return matched ?: expressions.first { it.isDefault }
}
