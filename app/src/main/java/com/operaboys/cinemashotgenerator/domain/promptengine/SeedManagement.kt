package com.operaboys.cinemashotgenerator.domain.promptengine

// واحد ۱۱ — فاز ۵: مدیریت Seed
// منبع حقیقت: docs/blueprints/11-prompt-engineering-core-v2.md
//
// تصمیم آگاهانه‌ی بلوپرینت: seed یک پارامتر فرمت خروجی (مثل --ar) نیست — یک تصمیم
// محتوایی برای ثبات بصری بین شات‌هاست، هم‌رده با Character Continuity. بنابراین در
// PromptBlueprint باقی می‌ماند، نه در Output Delivery (واحد ۱۴).

/** Seed ثابت و قابل‌پیش‌بینی از shotId؛ شات‌های مشابه در یک صحنه می‌توانند Seed های نزدیک به هم بگیرند. */
fun manageSeed(shotId: String, useSeed: Boolean, explicitSeedValue: Int?): Int? {
    if (!useSeed) return null
    return explicitSeedValue ?: shotId.hashCode()
}
