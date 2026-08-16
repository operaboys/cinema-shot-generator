package com.operaboys.cinemashotgenerator.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// G2 (docs/audit/post-unit16-full-audit.md خط ۱۳۷) قدم ۱ از ۳ — ذخیره‌سازی امن کلید
// API، طبق ADR-035 (Option A) که sendToAiConnector را عمداً TODO() گذاشته بود.
// این فایل فقط زیرساخت ذخیره‌سازی است — هیچ وابستگی به Ktor/AiConnector.kt/UI ندارد؛
// اتصال HTTP واقعی (قدم ۲) و UI مدیریت کلید (قدم ۳) خارج از Scope همین قدم‌اند.
//
// از androidx.security.crypto (EncryptedSharedPreferences روی MasterKey ساخته‌شده در
// Android Keystore — سخت‌افزار امن گوشی، نه یک کلید نرم‌افزاری دستی) استفاده شده —
// تصمیم معماری صریح، هم‌راستا با فلسفه‌ی On-Device/بدون سرور پروژه. جزئیات کامل
// (شامل دو یافته‌ی مهم: وضعیت Deprecated این کتابخانه، و محدودیت واقعی Robolectric
// با AndroidKeyStore) در ADR-098. تصمیم آگاهانه‌ی «فعلاً مهاجرت نکن» به جایگزین
// رسمی (androidx.datastore:datastore-tink، هنوز Alpha) — ADR-099 و R6 در
// docs/governance/risk-register.md.
//
// چندسرویسی: هر کلید با یک profileId مستقل ذخیره می‌شود (هم‌قرارداد با
// AiConnectorProfile.profileId، domain/storybreakdown/AiConnector.kt) — یک فایل
// EncryptedSharedPreferences مشترک با کلیدهای مجزا به‌ازای هر profileId، نه یک فایل
// جدا به‌ازای هر سرویس (فایل جدا هزینه‌ی مدیریتی اضافه‌ای می‌داد بدون فایده‌ی امنیتی
// واقعی — MasterKey و رمزنگاری همان سطح برای هر دو حالت یکسان است).
//
// نام‌گذاری متدها هم‌الگو با Repository های موجود پروژه (AssetRepository.saveXAsset/
// loadXAssets، AssetRepository.deleteAsset): saveApiKey/loadApiKey/deleteApiKey/
// hasApiKey — فعل + شیء، نه get/set انگلیسی خام.
//
// یافته‌ی حیاتی (تأییدشده تجربی، نه فرض): Robolectric (نسخه‌ی نصب‌شده‌ی پروژه، ۴.۱۵.۱)
// Provider واقعی "AndroidKeyStore" را ندارد — MasterKey.Builder.build() با
// java.security.KeyStoreException: AndroidKeyStore not found شکست می‌خورد. این یک
// محدودیت شناخته‌شده و حل‌نشده‌ی خودِ Robolectric است (GitHub issue #1518، باز از
// ۲۰۱۵). چون پروژه هیچ مسیر androidTest/دستگاه واقعی ندارد (تأییدشده با grep/find)،
// sharedPreferencesFactory زیر برای تزریق‌پذیری اضافه شد — هم‌الگو با idProvider/clock
// در ProjectRepository.kt: پیش‌فرض واقعی (EncryptedSharedPreferences روی Android
// Keystore واقعی) برای Production؛ تست‌ها یک SharedPreferences معمولی (بدون رمزنگاری،
// اما Robolectric کامل پشتیبانی می‌کند) تزریق می‌کنند تا خودِ منطق CRUD این Repository
// (نه رمزنگاری AndroidKeyStore، که خارج از دسترس تست‌پذیری این محیط است) واقعاً
// راستی‌آزمایی شود.
private const val PREFS_FILE_NAME = "secure_api_keys"

class SecureKeyRepository(
    context: Context,
    sharedPreferencesFactory: (Context) -> SharedPreferences = ::buildEncryptedSharedPreferences
) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy { sharedPreferencesFactory(appContext) }

    suspend fun saveApiKey(profileId: String, apiKey: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(profileId, apiKey).apply()
    }

    /** `null` یعنی هیچ کلیدی برای این profileId ذخیره نشده — نه یک خطا. */
    suspend fun loadApiKey(profileId: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(profileId, null)
    }

    suspend fun deleteApiKey(profileId: String) = withContext(Dispatchers.IO) {
        prefs.edit().remove(profileId).apply()
    }

    /** فقط true/false — خودِ کلید هرگز از این متد افشا نمی‌شود. */
    suspend fun hasApiKey(profileId: String): Boolean = withContext(Dispatchers.IO) {
        prefs.contains(profileId)
    }
}

private fun buildEncryptedSharedPreferences(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    return EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
