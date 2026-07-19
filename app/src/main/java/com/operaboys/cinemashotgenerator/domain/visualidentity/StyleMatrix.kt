package com.operaboys.cinemashotgenerator.domain.visualidentity

// واحد ۰۳ — بخش الف: Style Matrix
// منبع حقیقت: docs/blueprints/03-visual-identity.md

enum class StyleInfluence { SUBTLE, MODERATE, STRONG }

fun getInfluenceModifier(influence: StyleInfluence): String = when (influence) {
    StyleInfluence.SUBTLE -> "with subtle hints of"
    StyleInfluence.MODERATE -> "with elements of"
    StyleInfluence.STRONG -> "strongly influenced by"
}

data class StyleReference(val styleId: String, val name: String, val promptTokens: String)

fun combineStyles(primary: StyleReference, secondary: StyleReference?, influence: StyleInfluence?): String {
    var prompt = primary.promptTokens
    if (secondary != null && influence != null) {
        prompt += ", ${getInfluenceModifier(influence)} ${secondary.promptTokens}"
    }
    return prompt
}

enum class CompatibilityLevel { HIGH, MEDIUM, LOW, INCOMPATIBLE }

data class CompatibilityResult(val level: CompatibilityLevel, val warning: Boolean)

/**
 * پیاده‌سازی حداقلی طبق بلوپرینت — «نمونه قوانین» در کامنت بلوپرینت صرفاً مثال‌اند،
 * نه یک ماتریس سازگاری واقعی که جایی مشخص شده باشد. اختراع مقادیر واقعی برای
 * Oil Painting+8K یا Ghibli+Watercolor یعنی حدس زدن یک قانون کسب‌وکار که هیچ منبع
 * دیگری آن را تعریف نکرده — طبق قانون «بدون حدس زدن»، همان رفتار حداقلی بلوپرینت
 * (همیشه MEDIUM/false در نبود قانون خاص) حفظ شد.
 */
fun checkStyleCompatibility(primary: String, secondary: String): CompatibilityResult {
    return CompatibilityResult(level = CompatibilityLevel.MEDIUM, warning = false)
}

data class CustomStyle(
    val customStyleId: String,
    val name: String,
    val baseStyleId: String,
    val modifications: Map<String, String> = emptyMap()
)

/**
 * حالت فعلی Style Matrix یک پروژه. primaryStyle غیر-nullable است — Rule 1
 * («سبک اصلی همیشه باید تعیین‌شده باشد») در سطح Type System تضمین می‌شود؛
 * ساخت یک StyleMatrix بدون سبک اصلی اصلاً کامپایل نمی‌شود.
 */
data class StyleMatrix(
    val styleMatrixId: String,
    val primaryStyle: StyleReference,
    val secondaryStyle: StyleReference? = null,
    val influence: StyleInfluence? = null,
    val customStyles: List<CustomStyle> = emptyList()
)

sealed class StyleUpdateResult {
    object Allowed : StyleUpdateResult()
    data class Blocked(val reason: String) : StyleUpdateResult()
}

/**
 * Rule 3 (Blocking روی تلاش حذف): سبک اصلی قابل حذف نیست، فقط قابل تعویض.
 * newPrimaryStyle مقدار پیشنهادی برای به‌روزرسانی است؛ null یعنی تلاش برای حذف.
 */
fun validatePrimaryStyleUpdate(newPrimaryStyle: StyleReference?): StyleUpdateResult {
    if (newPrimaryStyle == null) {
        return StyleUpdateResult.Blocked("سبک اصلی قابل حذف نیست؛ فقط می‌توانید آن را با سبک دیگری تعویض کنید")
    }
    return StyleUpdateResult.Allowed
}

/**
 * Rule 2 (Warning، هرگز Blocking): ناسازگاری سبک اصلی/ثانویه، از checkStyleCompatibility.
 * وقتی سبک ثانویه‌ای انتخاب نشده باشد، چیزی برای بررسی سازگاری وجود ندارد (null).
 */
fun checkStyleMatrixCompatibility(matrix: StyleMatrix): CompatibilityResult? {
    val secondary = matrix.secondaryStyle ?: return null
    return checkStyleCompatibility(matrix.primaryStyle.styleId, secondary.styleId)
}
