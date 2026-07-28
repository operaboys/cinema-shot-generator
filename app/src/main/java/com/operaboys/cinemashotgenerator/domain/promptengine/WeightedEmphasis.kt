package com.operaboys.cinemashotgenerator.domain.promptengine

// واحد ۱۱ — فاز ۶: Weighted Emphasis (بدون نحو خاص پلتفرم)
// منبع حقیقت: docs/blueprints/11-prompt-engineering-core-v2.md

/** فقط داده‌ی خام «کدام تگ چه‌قدر تأکید دارد» را نگه می‌دارد؛ نحو نمایش (پرانتز، ::عدد) در Renderer تعیین می‌شود. */
fun collectWeightedEmphasis(weightedTags: Map<String, Float>?): Map<String, Float> {
    return weightedTags ?: emptyMap()
}
