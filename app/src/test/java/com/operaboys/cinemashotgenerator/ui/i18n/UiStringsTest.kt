package com.operaboys.cinemashotgenerator.ui.i18n

import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.outputdelivery.t
import com.operaboys.cinemashotgenerator.domain.outputdelivery.validateTranslationCoverage
import com.operaboys.cinemashotgenerator.domain.outputdelivery.validateTranslationKeyFound
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// واحد ۱۶ — فاز ۰: تأیید ساختاری کلیدهای دو زبان با validateTranslationCoverage
// واحد ۱۴ (Bilingual.kt) — اولین مصرف‌کننده‌ی واقعی این تابع در کل پروژه (تأییدشده
// با grep پیش از این فاز؛ فقط در تست خودِ واحد ۱۴ استفاده شده بود).

class UiStringsTest {

    @Test
    fun `fa and en string maps have exactly the same set of keys`() {
        val missing = validateTranslationCoverage(faStrings.keys, enStrings.keys)
        assertTrue("expected no coverage gaps, found: $missing", missing.isEmpty())
    }

    @Test
    fun `uiString falls back to the English value when a key is missing from the requested language`() {
        val partialTranslations = mapOf(Language.EN to mapOf("only.english" to "English Only"))
        assertEquals("English Only", t("only.english", Language.FA, partialTranslations))
    }

    @Test
    fun `every nav and studioTab key resolves to a real, non-key value in both languages`() {
        faStrings.keys.forEach { key ->
            assertTrue("fa value for '$key' should not just echo the key", uiString(key, Language.FA) != key)
            assertTrue("en value for '$key' should not just echo the key", uiString(key, Language.EN) != key)
        }
    }

    // اتصال واقعی Rule یتیم validateTranslationKeyFound (ADR-125) — برخلاف تست
    // بالا (که فقط کلیدهای *تعریف‌شده* در faStrings را چک می‌کند، یعنی هرگز یک
    // تایپوی کامل کلید — که در هیچ‌کدام از دو Map نیست — را نمی‌بیند)، این تست
    // واقعاً استخراج می‌کند کدام رشته‌های کلید در سراسر ماژول app/src/main با
    // uiString("...") صدا زده می‌شوند و برای هر کدام Rule واقعی را صدا می‌زند.
    //
    // تصمیم راستی‌آزمایی‌شده: دسترسی فایل‌سیستم به app/src/main از یک تست JVM/
    // Robolectric این پروژه واقعاً ممکن است — Working Directory تسک تست Gradle
    // پیش‌فرض دایرکتوری همان ماژول (app/) است (تأییدشده تجربی با یک پروب موقت:
    // File("src/main/java/...") واقعاً وجود داشت، ۲۰۰ فایل .kt پیدا شد) — پس
    // نیازی به رویکرد جایگزینِ «لیست دستی کلیدهای نماینده» نبود.

    @Test
    fun `every uiString call's key literal found across app-src-main resolves via validateTranslationKeyFound in both languages`() {
        val keyLiteralPattern = Regex("""\buiString\(\s*"([^"]+)"""")
        val sourceRoot = File("src/main/java")
        assertTrue(
            "src/main/java باید از این تست JVM در دسترس باشد — اگر این Assert شکست بخورد، فرض Working Directory دیگر صادق نیست",
            sourceRoot.isDirectory
        )

        val keys = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> keyLiteralPattern.findAll(file.readText()).map { match -> match.groupValues[1] } }
            .toSet()

        assertTrue(
            "انتظار می‌رفت حداقل چند صد کلید واقعی uiString(...) در کل کدبیس پیدا شود؛ عدد کم یعنی Regex/مسیر درست کار نمی‌کند",
            keys.size > 100
        )

        val issues = keys.flatMap { key ->
            listOfNotNull(
                validateTranslationKeyFound(key, Language.FA, uiTranslations)?.let { "$key (fa): ${it.message}" },
                validateTranslationKeyFound(key, Language.EN, uiTranslations)?.let { "$key (en): ${it.message}" }
            )
        }
        assertTrue("expected no missing translation keys among real uiString(...) call sites, found: $issues", issues.isEmpty())
    }

    @Test
    fun `validateTranslationKeyFound returns a real WARNING ValidationIssue for a key that genuinely does not exist in either language`() {
        val issue = validateTranslationKeyFound("fake.nonexistent.key.xyz", Language.FA, uiTranslations)
        assertNotNull("این تست خودِ Rule را اثبات می‌کند — نه فرض این‌که همیشه null برمی‌گرداند", issue)
        assertEquals(Severity.WARNING, issue!!.severity)
    }
}
