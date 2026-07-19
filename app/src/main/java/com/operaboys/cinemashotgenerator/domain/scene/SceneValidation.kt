package com.operaboys.cinemashotgenerator.domain.scene

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۰۴ — قوانین اعتبارسنجی Scene (Rule 1، ۴، ۵) و حذف Scene
// منبع حقیقت: docs/blueprints/04-scene-engine.md
//
// Rule 4/5 از ValidationIssue/Severity سراسری واحد ۰۷ استفاده می‌کنند — ادامه‌ی
// همان الگوی تثبیت‌شده در واحدهای ۰۳ و ۰۵.

/**
 * Rule 1 (Blocking): Scene قبل از Finalize باید حداقل یک Shot داشته باشد.
 */
fun validateSceneHasShotsBeforeFinalize(shotCount: Int): ValidationIssue? {
    if (shotCount < 1) {
        return ValidationIssue(
            Severity.BLOCKING,
            field = "shot_count",
            message = "صحنه باید قبل از Finalize حداقل یک شات داشته باشد"
        )
    }
    return null
}

// Rule 2 (Blocking): «Shot بدون اتصال به یک Scene قابل ایجاد نیست» — تابع Runtime
// جداگانه‌ای در این واحد نوشته نشد. Shot.sceneId (واحد ۰۵) از قبل به‌صورت
// `val sceneId: String` — غیر-nullable و بدون مقدار پیش‌فرض — تعریف شده؛ یعنی
// ساخت یک Shot بدون اتصال به یک Scene اصلاً کامپایل نمی‌شود. این قید در سطح
// Type System قبلاً برقرار است؛ نوشتن یک تابع Runtime اینجا فقط تکرار همان تضمین
// با هزینه‌ی یک وابستگی غیرضروری domain.scene → domain.shot بود.

// Rule 3: توضیحی است («تنظیمات Scene به‌طور پیش‌فرض به Shot های زیرمجموعه اعمال
// می‌شود، مگر Shot صریحاً Override کند»)، نه یک قانون قابل Validate جداگانه —
// خودِ inheritOrOverride پیاده‌سازی این رفتار است؛ تابع جداگانه‌ای اضافه نشد.

/** Rule 4 (Warning): نور ظهر (Noon) در فضای داخلی (Indoor) ممکن است غیرطبیعی به نظر برسد. */
fun checkNoonLightingInIndoor(timeOfDay: TimeOfDay, locationType: LocationType): ValidationIssue? {
    if (timeOfDay == TimeOfDay.NOON && locationType == LocationType.INDOOR) {
        return ValidationIssue(
            Severity.WARNING,
            message = "نور ظهر در فضای داخلی ممکن است غیرطبیعی به نظر برسد"
        )
    }
    return null
}

/** Rule 5 (Warning): اتمسفر آرام (Calm) برای صحنه‌ی اوج (Climax) غیرمعمول است. */
fun checkCalmAtmosphereInClimax(narrativeRole: NarrativeRole, atmospherePrimary: Atmosphere): ValidationIssue? {
    if (narrativeRole == NarrativeRole.CLIMAX && atmospherePrimary == Atmosphere.CALM) {
        return ValidationIssue(
            Severity.WARNING,
            message = "اتمسفر آرام برای صحنه‌ی اوج (Climax) غیرمعمول است"
        )
    }
    return null
}

/** حذف Scene ای که هنوز Shot دارد مجاز نیست؛ باید اول Shot ها حذف شوند. */
fun deleteScene(scene: Scene, existingShotsCount: Int): Result<Unit> {
    if (existingShotsCount > 0) {
        return Result.failure(
            IllegalStateException("این صحنه دارای $existingShotsCount شات است. ابتدا شات‌ها را حذف کنید.")
        )
    }
    return Result.success(Unit)
}
