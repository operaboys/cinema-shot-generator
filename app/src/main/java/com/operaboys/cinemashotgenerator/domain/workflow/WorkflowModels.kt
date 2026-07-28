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

data class WorkflowState(
    val sessionId: String,
    val projectId: String,
    val currentStep: WorkflowStep,
    val stepStatus: Map<WorkflowStep, StepStatus>,
    val startedAt: String,
    val lastActionAt: String,
    val shotListViewMode: ShotListViewMode = ShotListViewMode.GRID
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

/**
 * تشخیص «غنای» یک بخش متنی از structuredParts — سه سطح ساده (خالی/کوتاه/کافی).
 * بلوپرینت هیچ الگوریتم دقیقی برای evaluatePromptQuality نداده (برخلاف
 * diagnoseJsonError در واحد ۰۱ب) — فقط پنج محور کیفی با توضیح یک‌خطی. این یک
 * تخمین ساده و معقول است، طبق تصمیم مستند در docs/adr/037-unit16-workflow-models-quality-score.md
 * (Option A) — نه یک الگوریتم تحلیل معنایی واقعی.
 */
private fun scoreTextRichness(text: String): Int {
    val trimmed = text.trim()
    return when {
        trimmed.isEmpty() -> 0
        trimmed.length < 10 -> 5
        trimmed.length < 25 -> 12
        else -> 20
    }
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
