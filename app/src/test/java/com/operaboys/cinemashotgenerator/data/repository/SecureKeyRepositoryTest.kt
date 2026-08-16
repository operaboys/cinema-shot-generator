package com.operaboys.cinemashotgenerator.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// G2 قدم ۱ از ۳ (ADR-098) — تست‌های واقعی منطق CRUD این Repository روی Robolectric.
//
// یافته‌ی مهم (تجربی، نه فرضی): اولین تلاش این فایل SecureKeyRepository را با
// پیاده‌سازی واقعی EncryptedSharedPreferences صدا زد و با
// `java.security.KeyStoreException: AndroidKeyStore not found` شکست خورد —
// Robolectric ۴.۱۵.۱ (نسخه‌ی نصب‌شده‌ی این پروژه) هیچ Provider واقعی برای
// "AndroidKeyStore" ندارد (محدودیت شناخته‌شده‌ی خودِ Robolectric، GitHub issue
// #1518، باز از سال ۲۰۱۵ — بدون راه‌حل رسمی). پروژه هیچ مسیر androidTest/دستگاه
// واقعی هم ندارد (تأییدشده با grep/find). برای همین، این تست‌ها یک SharedPreferences
// معمولی (بدون رمزنگاری، اما با پشتیبانی کامل و بومی Robolectric) به سازنده‌ی
// SecureKeyRepository تزریق می‌کنند — همان بدنه‌ی کد واقعی (edit/putString/remove/
// contains) اجرا می‌شود، فقط backend رمزنگاری AndroidKeyStore واقعی (که در این محیط
// اصلاً قابل دسترسی نیست) جایگزین شده. تأیید رمزنگاری واقعی/Android Keystore واقعی
// باید روی دستگاه/شبیه‌ساز واقعی انجام شود — این محدودیت صریحاً در ADR-098 ثبت شده،
// نه یک ادعای پنهان.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecureKeyRepositoryTest {

    private lateinit var repository: SecureKeyRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository = SecureKeyRepository(context) { appContext ->
            appContext.getSharedPreferences("secure_key_repository_test_prefs", Context.MODE_PRIVATE)
        }
    }

    @Test
    fun `saving then loading a key for the same profileId returns the same value`() = runBlocking {
        repository.saveApiKey("claude_api", "sk-ant-test-12345")

        val result = repository.loadApiKey("claude_api")

        assertEquals("sk-ant-test-12345", result)
    }

    @Test
    fun `loading a profileId that was never saved returns null`() = runBlocking {
        val result = repository.loadApiKey("never_saved_profile")

        assertNull(result)
    }

    @Test
    fun `deleting a key actually removes it, loadApiKey returns null afterwards`() = runBlocking {
        repository.saveApiKey("openai_api", "sk-openai-test-67890")
        assertEquals("sk-openai-test-67890", repository.loadApiKey("openai_api"))

        repository.deleteApiKey("openai_api")

        assertNull(repository.loadApiKey("openai_api"))
    }

    @Test
    fun `two different profileIds are stored fully independently, saving one does not affect the other`() = runBlocking {
        repository.saveApiKey("claude_api", "sk-ant-claude-key")
        repository.saveApiKey("gemini_api", "sk-gemini-key")

        assertEquals("sk-ant-claude-key", repository.loadApiKey("claude_api"))
        assertEquals("sk-gemini-key", repository.loadApiKey("gemini_api"))

        repository.saveApiKey("claude_api", "sk-ant-claude-key-rotated")

        assertEquals(
            "به‌روزرسانی کلید یک profileId نباید روی profileId دیگر اثر بگذارد",
            "sk-gemini-key",
            repository.loadApiKey("gemini_api")
        )
        assertEquals("sk-ant-claude-key-rotated", repository.loadApiKey("claude_api"))
    }

    @Test
    fun `hasApiKey is true for a saved profileId and false for one that was never saved`() = runBlocking {
        repository.saveApiKey("deepseek_api", "sk-deepseek-key")

        assertTrue(repository.hasApiKey("deepseek_api"))
        assertFalse(repository.hasApiKey("qwen_api"))
    }

    @Test
    fun `hasApiKey is false after the key is deleted`() = runBlocking {
        repository.saveApiKey("claude_api", "sk-ant-test")
        assertTrue(repository.hasApiKey("claude_api"))

        repository.deleteApiKey("claude_api")

        assertFalse(repository.hasApiKey("claude_api"))
    }
}
