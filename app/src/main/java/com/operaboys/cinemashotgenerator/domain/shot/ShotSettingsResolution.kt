package com.operaboys.cinemashotgenerator.domain.shot

// اتصال معماری بین Shot.camera/.lighting/.environment (SourcedSettings<T> واحد ۰۵) و
// انواع نهاییِ تایپ‌شده که واحد ۱۱ (Prompt Engineering Core) به آن‌ها نیاز دارد.
// منبع حقیقت: docs/blueprints/05-shot-engine.md (بخش «ارث‌بری و Override») و
// docs/blueprints/04-scene-engine.md (inheritOrOverride). جزئیات کامل و تصمیمات
// مستقل در docs/adr/013-unit05-settings-resolution-deviations.md.

import com.operaboys.cinemashotgenerator.domain.camera.CameraSettings
import com.operaboys.cinemashotgenerator.domain.scene.Scene
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

/**
 * MIGRATED (docs/adr/038-unit04-scene-location-asset-link.md، رفع یافته‌ی F2 ممیزی
 * pre-Unit 16): این تابع **عمداً** الگوی SourcedSettings<T> واحد ۰۵ (مثل
 * resolveCameraSettings/resolveLightingSettings/resolveEnvironmentSettings) را برای
 * محل ارث‌بری تکرار نمی‌کند و به Shot هیچ فیلد جدیدی اضافه نمی‌کند.
 *
 * دلیل (بررسی صریح خواسته‌شده در دستور کار): Shot.locationIds (List<String>) از قبل
 * دقیقاً همان نقش را ایفا می‌کند — با grep تأیید شد که این فیلد در دو محل واقعی
 * Load-Bearing است: (۱) ValidationEngine.checkDataCompleteness/
 * ShotValidation.validateShotHasSubject آن را هم‌تراز characterIds/objectIds در یک
 * قانون BLOCKING «حداقل یک Subject» می‌دانند؛ (۲)
 * PromptGenerationRepository.collectData مستقیماً همین لیست را برای بارگذاری واقعی
 * LocationAsset از کتابخانه استفاده می‌کند. افزودن یک فیلد دوم و موازی
 * (sceneLocationOverride: SourcedSettings<String>?) که در هیچ‌کدام از این دو مصرف‌کننده
 * واقعی سیم‌کشی نشود، دقیقاً همان الگوی «فیلد تزئینی/غیرمتصل» است که این پروژه در
 * ADR-036 آگاهانه از آن پرهیز کرد؛ در بلوپرینت ۰۵/۰۶ نیز هیچ عبارتی این دو مفهوم را
 * جدا از هم توصیف نکرده (تنها ارجاع، docs/blueprints/05-shot-engine-v2.md:198، صرفاً
 * اعلان فیلد است، بدون توضیح رابطه‌ی آن با Scene).
 *
 * به‌جای آن، تهی‌بودن locationIds به‌عنوان سیگنال ارث‌بری/Override بازتفسیر می‌شود:
 * خالی ⇐ این Shot مکان مستقلی انتخاب نکرده، پس مکان Scene ارث می‌رسد؛ غیرخالی ⇐ این
 * Shot صریحاً Location(های) خودش را انتخاب/Override کرده. برخلاف
 * resolveCameraSettings و مشابهانش، این تابع Result<T> برنمی‌گرداند — چون خالی‌بودن
 * لیست Location برای یک Shot، برخلاف نبود camera/lighting/environment، یک وضعیت
 * کاملاً معتبر و رایج در این کدبیس است (مثلاً Shot ای که فقط روی Character تمرکز دارد)، نه یک خطا.
 */
fun resolveSceneLocation(shot: Shot, scene: Scene): List<String> {
    return shot.locationIds.ifEmpty { listOfNotNull(scene.locationAssetId) }
}
