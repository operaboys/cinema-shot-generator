package com.operaboys.cinemashotgenerator.ui.i18n

import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.outputdelivery.t
import com.operaboys.cinemashotgenerator.domain.outputdelivery.validateTranslationCoverage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
