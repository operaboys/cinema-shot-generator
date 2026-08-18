package com.operaboys.cinemashotgenerator.domain.workflow

import com.operaboys.cinemashotgenerator.domain.outputdelivery.RenderedOutput
import com.operaboys.cinemashotgenerator.domain.promptengine.PromptBlueprint

// واحد ۱۶ — پیاده‌سازی مفهومی وضعیت گردش کار + سنجش کیفیت
// منبع حقیقت: docs/blueprints/16-user-workflow-v2.md
//
// این پکیج مستقیماً یافته‌ی F1 ممیزی pre-Unit 16 را رفع می‌کند
// (docs/audit/pre-unit16-audit.md): domain/workflow/ قبلاً کاملاً غایب بود، در حالی
// که docs/reference/type-registry.md از قبل جدول کامل این واحد را با وضعیت ✅ نشان
// می‌داد — طبق تصمیم صریح دستور کار، کد به جدول موجود type-registry.md مطابق شد
// (نه برعکس)، چون آن جدول از قبل درست بود و فقط کد جا مانده بود.
//
// این پکیج کاملاً مستقل است — به هیچ‌چیز دیگری در domain/ وابسته نیست به‌جز
// RenderedOutput/PromptBlueprint (فقط برای امضای evaluatePromptQuality، بدون
// وابستگی معکوس واقعی چون این دو نوع در واحدهای پایین‌دست‌تر (۱۱/۱۴) تعریف شده‌اند).

enum class WorkflowStep {
    STORY_WIZARD, AI_STORY_BREAKDOWN, DNA_CONFIG, ASSET_LIBRARY, SCENE_CREATION,
    SHOT_CREATION, VALIDATION, PROMPT_GENERATION, OUTPUT_DELIVERY
}

enum class StepStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

/** حالت نمایش فهرست شات‌ها در مرحله‌ی Shot Creation. */
enum class ShotListViewMode { GRID, TIMELINE }

/**
 * دسته‌بندی نوع بازخورد لازم برای یک عملیات UI — طبق الگوی مرحله‌ی ۳ بلوپرینت ۱۶.
 * این enum مشخص می‌کند کدام Composable (AlertDialog در برابر Snackbar) باید استفاده
 * شود؛ پیاده‌سازی واقعی UI در واحد ۱۶ (لایه‌ی Compose) انجام می‌شود، این فقط
 * طبقه‌بندی مفهومی است.
 */
enum class FeedbackType {
    CONFIRMATION_REQUIRED,
    TRANSIENT_SUCCESS
}

// MIGRATED (docs/adr/042-unit16-phase0-shared-foundation.md، فاز ۰ واحد ۱۶):
// AppTheme/HomeLayoutVariant/ComposerLayoutVariant سه enum سبک UI هستند که فقط توسط
// لایه‌ی Compose مصرف می‌شوند (هیچ Rule کسب‌وکاری به آن‌ها وابسته نیست) — طبق همان
// اصل جانمایی ShotListViewMode/FeedbackType بالا (اینجا نگه داشته شدند، نه در ui/،
// چون domain/workflow دقیقاً پکیج «وضعیت گردش کار/UI» این پروژه است).
//
// عمداً هیچ AppLanguage جدیدی اینجا تعریف نشد — domain.outputdelivery.Language
// (FA/EN، از قبل موجود در Bilingual.kt واحد ۱۴) مستقیماً بازاستفاده می‌شود، چون این
// enum دقیقاً همان دو مقدار را با همان معنا دارد؛ افزودن یک enum زبان دوم و موازی
// همان الگوی «فیلد/نوع تکراری» بود که این پروژه بارها (ADR-036، ADR-038) از آن
// پرهیز کرده است.

/** تم بصری اپ — مستقل از زبان (docs/design/README.md: «Dark/Light theme toggle is independent of language»). */
enum class AppTheme { DARK, LIGHT }

/** واریانت A/B صفحه‌ی Home (Hero/Resume) — فقط انتخاب کاربر؛ محتوای هرکدام کار فاز ساخت خودِ صفحه‌ی Home است. */
enum class HomeLayoutVariant { HERO, RESUME }

/** واریانت A/B فرم Shot Composer (۴ Tab/Accordion) — فقط انتخاب کاربر؛ محتوای هرکدام کار فاز ساخت خودِ Shot Composer است. */
enum class ComposerLayoutVariant { TABS, ACCORDION }

data class WorkflowState(
    val sessionId: String,
    val projectId: String,
    val currentStep: WorkflowStep,
    val stepStatus: Map<WorkflowStep, StepStatus>,
    val startedAt: String,
    val lastActionAt: String,
    val shotListViewMode: ShotListViewMode = ShotListViewMode.GRID,
    // واحد ۱۶ فاز ۵ قدم ۳: آخرین مدل هدف انتخاب‌شده در صفحه‌ی Output Delivery — طبق
    // docs/design/README.md بخش State Management («Selected output model»، در همان
    // فهرست Language/Theme/shotListViewMode، نه یک فیلد مختص یک Shot خاص). دقیقاً
    // هم‌جنس shotListViewMode: طول یک Session Studio، نه DataStore بلندمدت.
    val selectedModelProfileId: String? = null
) {
    val progressPercentage: Int
        get() = (stepStatus.values.count { it == StepStatus.COMPLETED } * 100) / WorkflowStep.entries.size
}

/**
 * پیشروی به مرحله‌ی بعد فقط با هشدار مجاز است اگر مراحل قبلی کامل نباشند (نه
 * Blocking) — این تابع هرگز `false` برنمی‌گرداند؛ فقط پیام هشدار متفاوت است.
 */
fun canJumpToStep(state: WorkflowState, target: WorkflowStep): Pair<Boolean, String?> {
    val previousSteps = WorkflowStep.entries.filter { it.ordinal < target.ordinal }
    val allCompleted = previousSteps.all { state.stepStatus[it] == StepStatus.COMPLETED }
    return if (allCompleted) true to null
    else true to "برخی مراحل قبلی کامل نشده‌اند — ادامه با احتیاط"
}

data class QualityScore(
    val subjectClarity: Int,      // 0-20
    val cinematicClarity: Int,    // 0-20
    val visualSpecificity: Int,   // 0-20
    val styleCoherence: Int,      // 0-20
    val conciseness: Int          // 0-20
) {
    val total: Int get() = subjectClarity + cinematicClarity + visualSpecificity + styleCoherence + conciseness
}

// هوشمندسازی و اتصال evaluatePromptQuality — قدم ۱ از ۳ زیرقدم (ADR-118):
// دو سیگنال مستقل، تحقیق‌شده، برای تقویت scoreTextRichness (که تا این قدم فقط
// طول خام متن را می‌سنجید). منبع محتوایی هر دو (فهرست کلمات مبهم، انتخاب
// MATTR/آستانه‌ها) تحقیق مستقل معمار پروژه در منابع صنعت نوشتن پرامپت AI
// تصویر/ویدیو است — نه پیشنهاد این پیاده‌سازی؛ جزئیات کامل در ADR-118.

/**
 * صفت‌های ذهنی مبهم بدون جزئیات بصری — سیگنال کیفیت پایین، حتی در متن طولانی.
 * انگلیسی برای فیلدهای عمدتاً Enum-based (styleModifiers/cameraSpecs)، فارسی
 * برای فیلدهایی که می‌توانند شامل توضیح دستی کاربر باشند (مثل subjectDescription).
 */
private val ENGLISH_VAGUE_WORDS = setOf(
    "nice", "beautiful", "amazing", "cool", "good", "great", "awesome", "wonderful",
    "stunning", "lovely", "pretty", "fine", "several", "various", "many", "some"
)

private val PERSIAN_VAGUE_WORDS = setOf(
    "قشنگ", "زیبا", "خوب", "عالی", "جالب", "باحال", "خفن", "فوق‌العاده", "چندتا", "چند", "خیلی", "بعضی"
)

/**
 * تقسیم متن به کلمات با حذف علائم نگارشی از ابتدا/انتهای هر واحد — نه یک
 * regex محدود به `\w` (که کاراکترهای فارسی را نمی‌شناسد)، بلکه بر اساس
 * `Char.isLetterOrDigit()` که Unicode-aware است و هر دو زبان را پوشش می‌دهد.
 * پایه‌ی مشترک هم برای تطبیق کلمه‌به‌کلمه‌ی countVagueWords (نه substring خام
 * — «goodness» هرگز واحد جدایی از «good» شمرده نمی‌شود) و هم برای calculateMattr.
 */
private fun tokenizeWords(text: String): List<String> =
    text.split(Regex("\\s+"))
        .map { it.trim { c -> !c.isLetterOrDigit() } }
        .filter { it.isNotEmpty() }

/** شمارش کلمات مبهم (هر دو زبان) — case-insensitive برای انگلیسی (nice/Nice/NICE هر سه شمرده می‌شوند). */
private fun countVagueWords(text: String): Int =
    tokenizeWords(text).count { word ->
        val lower = word.lowercase()
        lower in ENGLISH_VAGUE_WORDS || lower in PERSIAN_VAGUE_WORDS
    }

/**
 * MATTR (Moving-Average Type-Token Ratio) — طبق یافته‌ی تحقیقی معمار، قوی‌ترین
 * پیش‌بینی‌کننده‌ی منفرد کیفیت پرامپت (Cohen's d=۰.۷۰۷ روی ۱۰,۰۰۰ پرامپت واقعی).
 * اگر کلمات متن کمتر از windowSize باشند، از کل متن به‌عنوان یک پنجره‌ی واحد
 * استفاده می‌شود (نه خطا/تقسیم بر صفر). متن خالی از کلمه → ۱.۰ (نه ۰/NaN؛ نبود
 * کلمه نباید به «تنوع صفر» تعبیر شود — خودِ خالی‌بودن متن از قبل توسط شرط طول
 * صفر در scoreTextRichness پوشش داده شده).
 */
private fun calculateMattr(text: String, windowSize: Int = 10): Double {
    val words = tokenizeWords(text).map { it.lowercase() }
    if (words.isEmpty()) return 1.0
    if (words.size < windowSize) {
        return words.toSet().size.toDouble() / words.size
    }
    val windowCount = words.size - windowSize + 1
    val totalTtr = (0 until windowCount).sumOf { start ->
        words.subList(start, start + windowSize).toSet().size.toDouble() / windowSize
    }
    return totalTtr / windowCount
}

/** آستانه‌ها: بیش از نیمی از کلمات متن مبهم‌اند؛ یا نیمی از کلمات یک پنجره تکراری‌اند. */
private const val VAGUE_WORD_MAJORITY_THRESHOLD = 0.5
private const val LOW_MATTR_THRESHOLD = 0.5

/** چهار سطح موجود scoreTextRichness، به‌ترتیب افزایشی — برای «پله پایین آوردن» جریمه. */
private val RICHNESS_LEVELS = listOf(0, 5, 12, 20)

/**
 * تشخیص «غنای» یک بخش متنی از structuredParts. پایه همان منطق قبلی طول خام
 * است (Option A، docs/adr/037-unit16-workflow-models-quality-score.md) — این
 * قدم آن را کاملاً جایگزین نمی‌کند، بلکه با دو جریمه‌ی مستقل تقویتش می‌کند:
 * اگر بیش از نیمی از کلمات متن مبهم باشند، یا MATTR زیر آستانه باشد، امتیاز
 * یک «پله» در فهرست ثابت RICHNESS_LEVELS (۰→۵→۱۲→۲۰) پایین می‌آید — اگر هر دو
 * شرط همزمان رخ دهند، دو پله. تصمیم فنی (نه محتوایی): جمع جریمه‌های مستقل
 * روی شاخص سطح (نه ضرب ضریب یا فرمول پیوسته) چون رفتار قابل‌پیش‌بینی، قابل‌تست‌
 * دقیق (یک تست = یک شرط ایزوله‌شده)، و همچنان محدود به همان چهار سطح گسسته‌ی
 * قبلی است — بدون نیاز به تغییر نوع بازگشتی یا معماری QualityScore.
 */
private fun scoreTextRichness(text: String): Int {
    val trimmed = text.trim()
    val baseScore = when {
        trimmed.isEmpty() -> return 0
        trimmed.length < 10 -> 5
        trimmed.length < 25 -> 12
        else -> 20
    }

    val wordCount = tokenizeWords(trimmed).size
    if (wordCount == 0) return baseScore

    var penaltySteps = 0
    if (countVagueWords(trimmed).toDouble() / wordCount > VAGUE_WORD_MAJORITY_THRESHOLD) penaltySteps++
    if (calculateMattr(trimmed) < LOW_MATTR_THRESHOLD) penaltySteps++
    if (penaltySteps == 0) return baseScore

    val baseIndex = RICHNESS_LEVELS.indexOf(baseScore)
    return RICHNESS_LEVELS[(baseIndex - penaltySteps).coerceAtLeast(0)]
}

/**
 * محیط (environmentSpecs) عمداً nullable است حتی در یک Blueprint کاملاً سالم —
 * null یعنی «آب‌وهوای Clear» (یک حالت معتبر)، نه داده‌ی جاافتاده. پس وقتی غایب است،
 * فقط بر اساس lightingSpecs قضاوت می‌شود (بدون جریمه کردن یک حالت معتبر).
 */
private fun scoreVisualSpecificity(lightingSpecs: String, environmentSpecs: String?): Int {
    val lightingScore = scoreTextRichness(lightingSpecs)
    val environmentScore = environmentSpecs?.let { scoreTextRichness(it) } ?: lightingScore
    return (lightingScore + environmentScore) / 2
}

/**
 * ایجاز — طول متن نهایی رندرشده را با چند آستانه‌ی ساده می‌سنجد (نه تشخیص واقعی
 * تکرار معنایی). پرامپت‌های خیلی طولانی‌تر معمولاً نشانه‌ی تکرار زائد یا جزئیات
 * غیرضروری‌اند.
 */
private fun scoreConciseness(formattedPrompt: String): Int {
    val length = formattedPrompt.trim().length
    return when {
        length == 0 -> 0
        length <= 800 -> 20
        length <= 1500 -> 15
        length <= 2500 -> 10
        else -> 5
    }
}

/**
 * این تابع یک ابزار کمکی/اختیاری است — نه بخشی الزامی از Validation (که در واحد
 * ۰۷ تعریف شده). هدف: کمک به کاربر برای دیدن نقاط ضعف احتمالی پرامپت قبل از ارسال
 * به مدل AI، نه بلاک‌کردن جریان کار.
 *
 * MIGRATED (Option A، docs/adr/037-unit16-workflow-models-quality-score.md): بلوپرینت
 * این تابع را TODO() گذاشته بود؛ چون ورودی‌های لازم (`blueprint.structuredParts`،
 * `renderedOutput.formattedPrompt`) در دسترس‌اند و این ابزار غیرحیاتی/اختیاری است
 * (ریسک پایین اشتباه جزئی در الگوریتم)، یک پیاده‌سازی حداقلی و معقول نوشته شد —
 * نه TODO باقی گذاشته شد.
 */
fun evaluatePromptQuality(renderedOutput: RenderedOutput, blueprint: PromptBlueprint): QualityScore {
    val parts = blueprint.structuredParts
    return QualityScore(
        subjectClarity = scoreTextRichness(parts.subjectDescription),
        cinematicClarity = scoreTextRichness(parts.cameraSpecs),
        visualSpecificity = scoreVisualSpecificity(parts.lightingSpecs, parts.environmentSpecs),
        styleCoherence = scoreTextRichness(parts.styleModifiers),
        conciseness = scoreConciseness(renderedOutput.formattedPrompt)
    )
}
