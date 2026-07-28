package com.operaboys.cinemashotgenerator.domain.storybreakdown

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

// واحد ۰۱ب — AI Story Breakdown (بخش ت: JSON Doctor)
// منبع حقیقت: docs/blueprints/01b-ai-story-breakdown.md (نسخه ۲)
//
// این بخش دوم از واحد ۰۱ب است، ادامه‌ی قدم اول (PromptBuilder.kt/ChunkCombiner.kt،
// docs/adr/032-...). بخش‌های ب (AI Connector) و ث (Story-to-Domain Mapper) عمداً در
// این قدم دست‌نخورده ماندند.
//
// الگوی Parse لق (JsonElement/JsonObject به‌جای decodeFromString با data class کامل)
// از domain/promptfinalization/PromptFinalizationPipeline.kt گرفته شده (تنها محل
// مشابه در پروژه، تأییدشده با grep): runCatching { Json.parseToJsonElement(...) }
// سپس cast به JsonObject — چون در این قدم هنوز نمی‌خواهیم ساختار کامل AI را Parse
// کنیم (آن کار Story-to-Domain Mapper در قدم بعدی است).

enum class JsonErrorType {
    TRAILING_COMMA,
    SMART_QUOTES,
    UNMATCHED_BRACKET,
    INCOMPLETE_RESPONSE,
    UNKNOWN
}

data class JsonDiagnosis(
    val errorType: JsonErrorType,
    val approximateLine: Int?,
    val simpleExplanation: String,
    val autoFixable: Boolean
)

/** خروجی جریان کامل Parse→Diagnose→AutoFix؛ جزئیات کامل در docs/adr/033-...md. */
sealed class JsonRepairResult {
    data class Success(val repairedJson: String, val wasAutoFixed: Boolean) : JsonRepairResult()
    data class NeedsManualRepair(val diagnosis: JsonDiagnosis) : JsonRepairResult()
}

private val TRAILING_COMMA_REGEX = Regex(",\\s*([}\\]])")
private val SMART_QUOTE_CHARS = setOf('“', '”', '‘', '’')
private val REQUIRED_TOP_LEVEL_KEYS = listOf("characters", "locations", "shots")

private fun lineNumberAt(text: String, index: Int): Int {
    val safeIndex = index.coerceIn(0, text.length)
    return text.substring(0, safeIndex).count { it == '\n' } + 1
}

/**
 * بازرسی ساختاری متن خام برای تشخیص «پاسخ ناتمام» — طبق تصمیم مستند در ADR-033، این
 * بررسی فقط وقتی اعمال می‌شود که متن اصلاً شبیه شروع یک مقدار JSON باشد (با { یا [
 * شروع شود)؛ در غیر این صورت متن کاملاً غیرمرتبط (نه یک JSON بریده‌شده) است و باید
 * UNKNOWN تشخیص داده شود، نه INCOMPLETE_RESPONSE.
 */
private fun looksIncomplete(rawJson: String): Boolean {
    val trimmed = rawJson.trim()
    if (trimmed.isEmpty()) return false
    if (trimmed.first() != '{' && trimmed.first() != '[') return false

    val unescapedQuoteCount = Regex("(?<!\\\\)\"").findAll(trimmed).count()
    if (unescapedQuoteCount % 2 != 0) return true

    return trimmed.last() != '}' && trimmed.last() != ']'
}

/** شمارش تعادل { } و [ ] — تشخیص تقریبی طبق تصریح بلوپرینت، نه یک Parser کامل. */
private fun findBracketImbalance(rawJson: String): Boolean {
    val openBraces = rawJson.count { it == '{' }
    val closeBraces = rawJson.count { it == '}' }
    val openBrackets = rawJson.count { it == '[' }
    val closeBrackets = rawJson.count { it == ']' }
    return openBraces != closeBraces || openBrackets != closeBrackets
}

/**
 * تشخیص نوع خطای رایج JSON از روی پیام خام Parser + بازرسی ساختاری متن — طبق
 * تصمیم مستند در ADR-033، ترتیب بررسی TRAILING_COMMA → SMART_QUOTES →
 * INCOMPLETE_RESPONSE → UNMATCHED_BRACKET → UNKNOWN است (نه ترتیب تعریف enum، که
 * صرفاً یک فهرست است، نه اولویت).
 */
fun diagnoseJsonError(rawJson: String, parserErrorMessage: String): JsonDiagnosis {
    val trailingCommaMatch = TRAILING_COMMA_REGEX.find(rawJson)
    if (trailingCommaMatch != null) {
        return JsonDiagnosis(
            errorType = JsonErrorType.TRAILING_COMMA,
            approximateLine = lineNumberAt(rawJson, trailingCommaMatch.range.first),
            simpleExplanation = "یک کاما اضافه قبل از بسته‌شدن یک آکولاد یا براکت وجود دارد — معمولاً وقتی آخرین آیتم یک لیست یا آبجکت با کاما تمام شده باشد.",
            autoFixable = true
        )
    }

    val smartQuoteIndex = rawJson.indexOfFirst { it in SMART_QUOTE_CHARS }
    if (smartQuoteIndex != -1) {
        return JsonDiagnosis(
            errorType = JsonErrorType.SMART_QUOTES,
            approximateLine = lineNumberAt(rawJson, smartQuoteIndex),
            simpleExplanation = "متن حاوی نقل‌قول‌های تزئینی («») است که JSON استاندارد آن‌ها را نمی‌شناسد — معمولاً وقتی پاسخ از یک ویرایشگر متن با تصحیح خودکار نقل‌قول کپی شده باشد.",
            autoFixable = true
        )
    }

    if (looksIncomplete(rawJson)) {
        return JsonDiagnosis(
            errorType = JsonErrorType.INCOMPLETE_RESPONSE,
            approximateLine = lineNumberAt(rawJson, rawJson.length),
            simpleExplanation = "به‌نظر می‌رسد پاسخ ناتمام است — انگار وسط یک رشته یا مقدار قطع شده؛ ممکن است بخشی از پاسخ AI هنوز نرسیده باشد (به Chunk Combiner مراجعه کنید).",
            autoFixable = false
        )
    }

    if (findBracketImbalance(rawJson)) {
        return JsonDiagnosis(
            errorType = JsonErrorType.UNMATCHED_BRACKET,
            approximateLine = lineNumberAt(rawJson, rawJson.length),
            simpleExplanation = "تعداد آکولاد { } یا براکت [ ] باز با تعداد بسته‌شدنشان برابر نیست — یکی از آن‌ها جایی جا افتاده یا اضافه است.",
            autoFixable = false
        )
    }

    return JsonDiagnosis(
        errorType = JsonErrorType.UNKNOWN,
        approximateLine = null,
        simpleExplanation = "نوع خطای JSON قابل‌تشخیص خودکار نیست. پیام اصلی: $parserErrorMessage — لطفاً متن را در ویرایشگر بررسی و دستی اصلاح کنید.",
        autoFixable = false
    )
}

/** تعمیر خودکار — فقط برای انواع خطای autoFixable=true. عیناً طبق کد مفهومی بلوپرینت. */
fun attemptAutoFix(rawJson: String, diagnosis: JsonDiagnosis): String? {
    return when (diagnosis.errorType) {
        JsonErrorType.TRAILING_COMMA -> rawJson.replace(TRAILING_COMMA_REGEX, "$1")
        JsonErrorType.SMART_QUOTES -> rawJson
            .replace('“', '"').replace('”', '"')
            .replace('‘', '\'').replace('’', '\'')
        else -> null
    }
}

/**
 * جریان کامل طبق نمودار بلوپرینت: تلاش Parse مستقیم → در صورت خطا diagnoseJsonError
 * → اگر autoFixable، attemptAutoFix و تلاش مجدد Parse → نتیجه‌ی نهایی.
 */
fun repairJson(rawJson: String): JsonRepairResult {
    val directParse = runCatching { Json.parseToJsonElement(rawJson) }
    if (directParse.isSuccess) {
        return JsonRepairResult.Success(repairedJson = rawJson, wasAutoFixed = false)
    }

    val diagnosis = diagnoseJsonError(rawJson, directParse.exceptionOrNull()?.message ?: "خطای ناشناخته")
    if (diagnosis.autoFixable) {
        val fixed = attemptAutoFix(rawJson, diagnosis)
        if (fixed != null && runCatching { Json.parseToJsonElement(fixed) }.isSuccess) {
            return JsonRepairResult.Success(repairedJson = fixed, wasAutoFixed = true)
        }
    }
    return JsonRepairResult.NeedsManualRepair(diagnosis)
}

/** Rule 7 (Blocking): JSON بعد از تعمیر خودکار همچنان نامعتبر است. */
fun validateJsonRepairResult(result: JsonRepairResult): ValidationIssue? {
    if (result is JsonRepairResult.NeedsManualRepair) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "JSON پس از تلاش برای تعمیر خودکار هنوز نامعتبر است؛ ${result.diagnosis.simpleExplanation} — نیاز به ویرایش دستی دارید"
        )
    }
    return null
}

/**
 * Rule 8 (Blocking): JSON معتبر است ولی فاقد کلیدهای الزامی (characters, locations,
 * shots) — با JsonObject لق، بدون نیاز به data class کامل (آن کار Mapper قدم بعدی است).
 */
fun validateRequiredKeysPresent(rawJson: String): ValidationIssue? {
    val root = runCatching { Json.parseToJsonElement(rawJson) }.getOrNull() as? JsonObject ?: return null
    val missingKeys = REQUIRED_TOP_LEVEL_KEYS.filter { it !in root }
    if (missingKeys.isNotEmpty()) {
        return ValidationIssue(
            Severity.BLOCKING,
            message = "ساختار JSON درست است اما اطلاعات لازم را ندارد؛ کلید(های) غایب: ${missingKeys.joinToString("، ")}"
        )
    }
    return null
}
