package com.operaboys.cinemashotgenerator.domain.shot

// واحد ۰۵ — ارث‌بری از Scene
// منبع حقیقت: docs/blueprints/05-shot-engine.md + docs/blueprints/04-scene-engine.md
//
// این تابع دقیقاً در بلوپرینت ۰۴ (Scene Engine) تعریف شده، اما کاملاً Generic است و
// هیچ وابستگی واقعی به نوع Scene ندارد. چون واحد ۰۴ هنوز پیاده نشده، نسخه‌ای مستقل
// اینجا (واحد ۰۵ که به آن نیاز دارد) کپی شد — نه Import از واحدی که وجود ندارد.
// سؤال باز: آیا این تابع باید در آینده به واحد ۰۴ منتقل شود یا همین‌جا بماند؟
// (docs/adr/006-unit05-shot-engine-deviations.md)

/**
 * هنگام ساخت یک Shot جدید، تنظیمات Scene به آن ارث می‌رسد؛
 * فیلدهای صریح Shot، مقادیر ارثی را Override می‌کنند.
 */
fun <T> inheritOrOverride(sceneValue: T, shotValue: T?): T = shotValue ?: sceneValue
