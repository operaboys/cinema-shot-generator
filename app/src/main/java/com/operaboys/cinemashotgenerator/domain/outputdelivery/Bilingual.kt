package com.operaboys.cinemashotgenerator.domain.outputdelivery

import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue

// واحد ۱۴ — Output Delivery System (بخش ج: Bilingual System — فقط ساختار داده و منطق
// ترجمه؛ نه UI/Compose واقعی)
// منبع حقیقت: docs/blueprints/14-output-delivery.md
//
// NOTE (خارج از Scope این قدم، عمداً پیاده نشد): generateBilingualPrompt/
// translateToFarsi بلوپرینت اینجا پیاده نشدند — فهرست صریح این قدم آن‌ها را نام نبرده
// و یک موتور ترجمه‌ی واقعی (کدام سرویس/دیکشنری، دقت، هزینه) خودش یک تصمیم معماری
// جداست، نه یک جزئیات پیاده‌سازی محلی. composeOutput (بخش ب) هم bilingualPrompts را
// از بیرون می‌گیرد، نه این‌که خودش تولید کند — پس این حذف زنجیره را نمی‌شکند.

enum class Language { FA, EN }

data class LanguagePreferences(
    val uiLanguage: Language = Language.FA,
    val promptLanguage: Language = Language.FA, // بر اساس UI Language؛ بدون فرض ثابت
    val fallbackLanguage: Language = Language.EN // فقط برای ترجمه‌ی UI Labels، نه Prompt
)

/** تابع ترجمه‌ی UI با Fallback؛ کاملاً مستقل از promptLanguage. */
fun t(key: String, uiLanguage: Language, translations: Map<Language, Map<String, String>>): String {
    return translations[uiLanguage]?.get(key)
        ?: translations[Language.EN]?.get(key) // fallback فقط برای UI
        ?: key // در حالت توسعه، خودِ کلید نمایش داده می‌شود تا کلید گم‌شده فوراً مشخص شود
}

/** تست ساختاری: باید در CI اجرا شود تا اطمینان دهد دو فایل زبان دقیقاً هم‌ساختارند. */
fun validateTranslationCoverage(faKeys: Set<String>, enKeys: Set<String>): List<String> {
    val missingInEn = faKeys - enKeys
    val missingInFa = enKeys - faKeys
    return (missingInEn.map { "کلید '$it' در en.json موجود نیست" } +
        missingInFa.map { "کلید '$it' در fa.json موجود نیست" })
}

/**
 * Rule «کلید ترجمه در هیچ‌کدام از دو زبان یافت نشود». t() خودش fallback می‌کند به
 * خودِ key؛ این فقط هشدار گزارشی است.
 */
fun validateTranslationKeyFound(
    key: String,
    uiLanguage: Language,
    translations: Map<Language, Map<String, String>>
): ValidationIssue? {
    val found = translations[uiLanguage]?.containsKey(key) == true ||
        translations[Language.EN]?.containsKey(key) == true
    if (found) return null
    return ValidationIssue(
        severity = Severity.WARNING,
        message = "کلید ترجمه '$key' در هیچ‌کدام از دو زبان یافت نشد"
    )
}

/**
 * Rule «زبان درخواستی خارج از FA/EN». خودِ enum Language فقط FA/EN دارد — این Rule در
 * سطح Type System همیشه برقرار است برای هر پارامتری از نوع Language (مشابه تضمین
 * ساختاری UpdateResult واحد ۰۶). این تابع برای اعتبارسنجی یک رشته‌ی خام (مثل Locale
 * سیستم‌عامل) پیش از تبدیل به enum Language است — تنها جایی که «زبان نامعتبر» واقعاً
 * ممکن است در Runtime رخ دهد.
 */
fun validateLanguageSupported(languageCode: String): ValidationIssue? {
    val supported = Language.entries.any { it.name.equals(languageCode, ignoreCase = true) }
    if (supported) return null
    return ValidationIssue(
        severity = Severity.BLOCKING,
        message = "زبان '$languageCode' پشتیبانی نمی‌شود (فقط FA/EN)"
    )
}
