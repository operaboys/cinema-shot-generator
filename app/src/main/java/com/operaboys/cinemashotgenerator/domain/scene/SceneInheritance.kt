package com.operaboys.cinemashotgenerator.domain.scene

// واحد ۰۴ — ارث‌بری از Scene به Shot (صاحب اصلی این تابع)
// منبع حقیقت: docs/blueprints/04-scene-engine.md
//
// این واحد صاحب اصلی inheritOrOverride است. واحد ۰۵ (Shot Engine) قبلاً یک نسخه‌ی
// مستقل از همین تابع را در domain/shot/ShotInheritance.kt کپی کرده بود، دقیقاً چون
// این واحد (۰۴) هنوز وجود نداشت (docs/adr/006-...). آیا آن نسخه‌ی تکراری باید حذف
// و با import از اینجا جایگزین شود، در docs/adr/007-unit04-scene-engine-deviations.md
// به‌عنوان پیشنهاد مطرح شده — این قدم آن فایل را تغییر نداد.

/**
 * هنگام ساخت یک Shot جدید، تنظیمات Scene به آن ارث می‌رسد؛
 * فیلدهای صریح Shot، مقادیر ارثی را Override می‌کنند.
 */
fun <T> inheritOrOverride(sceneValue: T, shotValue: T?): T = shotValue ?: sceneValue
