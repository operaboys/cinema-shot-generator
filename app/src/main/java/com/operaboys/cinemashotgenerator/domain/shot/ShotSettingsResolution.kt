package com.operaboys.cinemashotgenerator.domain.shot

// اتصال معماری بین Shot.camera/.lighting/.environment (SourcedSettings<T> واحد ۰۵) و
// انواع نهاییِ تایپ‌شده که واحد ۱۱ (Prompt Engineering Core) به آن‌ها نیاز دارد.
// منبع حقیقت: docs/blueprints/05-shot-engine.md (بخش «ارث‌بری و Override») و
// docs/blueprints/04-scene-engine.md (inheritOrOverride). جزئیات کامل و تصمیمات
// مستقل در docs/adr/013-unit05-settings-resolution-deviations.md.

import com.operaboys.cinemashotgenerator.domain.camera.CameraSettings
import com.operaboys.cinemashotgenerator.domain.scene.inheritOrOverride
import com.operaboys.cinemashotgenerator.domain.sceneconditions.EnvironmentSettings
import com.operaboys.cinemashotgenerator.domain.sceneconditions.LightingSettings

fun resolveCameraSettings(shot: Shot, sceneDefault: CameraSettings?): Result<CameraSettings> =
    resolveSourcedSettings(shot.camera, sceneDefault)

fun resolveLightingSettings(shot: Shot, sceneDefault: LightingSettings?): Result<LightingSettings> =
    resolveSourcedSettings(shot.lighting, sceneDefault)

fun resolveEnvironmentSettings(shot: Shot, sceneDefault: EnvironmentSettings?): Result<EnvironmentSettings> =
    resolveSourcedSettings(shot.environment, sceneDefault)

/**
 * حل نهایی Negative Prompt مؤثر این Shot (docs/blueprints/05-shot-engine-v2.md).
 * برخلاف resolveCameraSettings/resolveLightingSettings/resolveEnvironmentSettings
 * (که منبع ارث‌بری‌شان Scene است، پس sceneDefault می‌گیرند)، منبع ارث‌بری اینجا DNA
 * پروژه است — طبق تصریح بلوپرینت («نه یک تنظیم مخصوص یک صحنه‌ی خاص»).
 *
 * امضا عمداً String می‌گیرد (نه ProjectDna کامل) تا این تابع به کل ProjectDna
 * وابسته نشود — طبق پیشنهاد صریح بلوپرینت؛ فراخوان مسئول استخراج
 * dna.qualityDirectives.negativePrompt پیش از فراخوانی این تابع است.
 *
 * اگر Override دستی وجود دارد (حتی رشته‌ی خالی، برای غیرفعال‌کردن کامل negative
 * prompt در همین Shot)، همان استفاده می‌شود؛ وگرنه مقدار سطح DNA.
 */
fun resolveNegativePrompt(shot: Shot, dnaNegativePrompt: String): String {
    return shot.negativePromptOverride ?: dnaNegativePrompt
}

/**
 * sceneDefault بیرونی تزریق می‌شود چون Scene خودش فیلد پیش‌فرض camera/lighting/environment
 * ندارد (تأیید شده با grep در SceneModels.kt؛ ADR-013) — بلوپرینت ۰۴ فقط globalVisualStyle
 * و لیست‌های محدودکننده (restrictions) را روی Scene تعریف کرده، نه مقدار واقعی این سه.
 *
 * وقتی sceneDefault موجود است، خودِ inheritOrOverride واحد ۰۴ فراخوانی می‌شود (شکل واقعی
 * «override شات در برابر پیش‌فرض Scene»، نه یک if/else بازتعریف‌شده). shotOverride فقط وقتی
 * در نظر گرفته می‌شود که source صریحاً "override" باشد — صرف پرشدن overrideValue کافی نیست،
 * چون source همان فیلدی است که بلوپرینت ۰۵ آن را منبع تصمیم معرفی کرده.
 *
 * اگر نه sceneDefault و نه override شات مقداری نداشته باشند، هیچ مقدار قابل‌استنادی
 * وجود ندارد؛ طبق الگوی مستقر پروژه (DependencyResolver، OverrideActions،
 * SceneValidation، CinematicLanguage) با Result.failure(IllegalStateException) گزارش
 * می‌شود، نه throw خام یا بازگرداندن یک مقدار جعلی.
 */
private fun <T> resolveSourcedSettings(sourced: SourcedSettings<T>, sceneDefault: T?): Result<T> {
    val shotOverride = sourced.overrideValue.takeIf { sourced.source == "override" }
    return when {
        sceneDefault != null -> Result.success(inheritOrOverride(sceneDefault, shotOverride))
        shotOverride != null -> Result.success(shotOverride)
        else -> Result.failure(
            IllegalStateException("نه sceneDefault و نه override شات برای این تنظیمات موجود است")
        )
    }
}
