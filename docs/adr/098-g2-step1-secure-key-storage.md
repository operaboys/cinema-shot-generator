# ADR-098: G2 قدم ۱ از ۳ — لایه‌ی ذخیره‌سازی امن کلید API

## زمینه

طبق ADR-035 (Option A)، `sendToAiConnector` عمداً `TODO()` ماند — پیاده‌سازی
واقعی HTTP (Option B) به قدم‌های جداگانه‌ی آینده موکول شد. این قدم اولین
زیرقدم از آن مسیر است: **فقط** لایه‌ی ذخیره‌سازی امن کلید API، کاملاً مستقل از
Ktor/`AiConnector.kt`/UI. قدم‌های ۲ (اتصال HTTP واقعی) و ۳ (UI مدیریت کلید)
در این قدم دست نخوردند.

## بررسی مستقل (پیش از نوشتن کد)

- `app/build.gradle.kts`: `compileSdk = 36`، `minSdk = 26`، `targetSdk = 36` —
  تأییدشده.
- `gradle/libs.versions.toml` / `data/repository/`: با grep تأیید شد هیچ
  کتابخانه‌ی رمزنگاری و هیچ `SecureKeyRepository` از قبل وجود ندارد.
- `docs/blueprints/01b-ai-story-breakdown.md`: امضای `AiConnectorProfile`
  دقیقاً تأییدشد — `profileId, displayName, endpointUrl, requestBodyTemplate,
  requestHeaders, responseJsonPath` (بدون فیلد `apiKey` روی خودِ Profile —
  کلید جداگانه پاس داده می‌شود، دقیقاً همان دلیلی که یک Repository مستقل
  کلید-به-ازای-profileId را معنادار می‌کند).
- نام‌گذاری متدها: با خواندن `ProjectRepository.kt`/`AssetRepository.kt`
  تأیید شد الگوی فعل+شیء (`save`/`load`/`delete`) رایج پروژه است —
  `saveApiKey`/`loadApiKey`/`deleteApiKey`/`hasApiKey` انتخاب شد.
- زیرساخت تست: با grep/find تأیید شد پروژه هیچ مسیر `androidTest` ندارد؛
  همه‌جا از Robolectric برای تست‌های نیازمند Context واقعی استفاده می‌شود —
  همان الگو دنبال شد.

## دو یافته‌ی مهم که پیش‌بررسی پوشش نداده بود

### ۱. `androidx.security.crypto` اکنون Deprecated است (از نسخه‌ی ۱.۱.۰-beta01)

با بررسی مستقیم صفحه‌ی رسمی androidx.security تأیید شد: از نسخه‌ی
`1.1.0-beta01` (ژوئن ۲۰۲۵) به بعد، تمام API های این کتابخانه (شامل
`EncryptedSharedPreferences`/`MasterKey`) **Deprecated** علامت‌گذاری شده‌اند —
گوگل جایگزین را «API های مستقیم پلتفرم / استفاده‌ی مستقیم از Android
Keystore» معرفی کرده. کامپایلر پروژه هم این را با ۱۰ هشدار Deprecated تأیید
کرد. آخرین نسخه‌ی پایدار منتشرشده همچنان `1.1.0` است (نه یک نسخه‌ی حذف‌شده) —
کتابخانه کار می‌کند، فقط دیگر توصیه‌ی «مسیر جدید» گوگل نیست.

**تصمیم:** طبق دستور صریح این قدم (تصمیم معماری با دلیل مستند: هم‌راستایی با
فلسفه‌ی On-Device، کلید در سخت‌افزار امن گوشی)، همچنان از
`androidx.security.crypto:1.1.0` استفاده شد — این وضعیت به معمار گزارش
می‌شود تا در صورت تمایل، تصمیم برای یک قدم آینده (مهاجرت به Keystore مستقیم)
بازبینی شود؛ خودسرانه این تصمیم معماری دور زده نشد.

### ۲. Robolectric (نسخه‌ی نصب‌شده، ۴.۱۵.۱) از Provider واقعی "AndroidKeyStore" پشتیبانی نمی‌کند

اولین نسخه‌ی تست‌ها (با پیاده‌سازی واقعی `EncryptedSharedPreferences` بدون
هیچ لایه‌ی تزریق‌پذیری) نوشته و اجرا شد — نتیجه: **۶ از ۶ شکست خوردند**، همگی
با:
```
java.security.KeyStoreException: AndroidKeyStore not found
Caused by: java.security.NoSuchAlgorithmException: AndroidKeyStore KeyStore not available
```
بررسی مستقل (GitHub issues رسمی Robolectric) تأیید کرد این یک محدودیت
شناخته‌شده و حل‌نشده‌ی خودِ Robolectric است — issue #1518 («Support
java.security.KeyStore») از ژانویه‌ی ۲۰۱۵ همچنان باز و در Backlog است؛ issue
#9793 (نوامبر ۲۰۲۴) دقیقاً همین خطا را با `EncryptedSharedPreferences` گزارش
کرده، بدون هیچ راه‌حل رسمی.

**تصمیم:** به‌جای رها کردن تست‌های رفتاری (که این قدم صریحاً الزامی کرده
بود)، یک پارامتر تزریق‌پذیر `sharedPreferencesFactory: (Context) ->
SharedPreferences` به `SecureKeyRepository` اضافه شد — هم‌الگو با
`idProvider`/`clock` در `ProjectRepository.kt` (تزریق‌پذیری برای تست، پیش‌فرض
واقعی برای Production). پیش‌فرض واقعی (`buildEncryptedSharedPreferences`)
همان `EncryptedSharedPreferences` روی `MasterKey`/Android Keystore واقعی
می‌سازد و در Production بدون تغییر به کار می‌رود؛ تست‌ها یک
`SharedPreferences` معمولی (بدون رمزنگاری، اما Robolectric کامل و بومی
پشتیبانی می‌کند) تزریق می‌کنند. همان کد واقعی CRUD (`edit/putString/remove/
apply/getString/contains`) در هر دو مسیر اجرا می‌شود — فقط backend
رمزنگاری AndroidKeyStore (که در این محیط اصلاً در دسترس نیست) جایگزین شده.

**محدودیت صریح باقی‌مانده:** خودِ رمزنگاری/یکپارچگی واقعی با Android Keystore
هرگز در این محیط قابل تست نبود و نیست — نه با Robolectric (محدودیت بالا)، نه
با یک دستگاه/شبیه‌ساز واقعی (این محیط چنین چیزی ندارد). این باید پیش از
انتشار واقعی، حداقل یک‌بار روی یک دستگاه/شبیه‌ساز واقعی Android دستی بررسی
شود — یک بدهی تست مستند، نه یک ادعای پنهان اتمام کامل.

## پیاده‌سازی

`SecureKeyRepository.kt` (بدون وابستگی به Ktor/`AiConnector.kt`/UI):

```kotlin
class SecureKeyRepository(
    context: Context,
    sharedPreferencesFactory: (Context) -> SharedPreferences = ::buildEncryptedSharedPreferences
) {
    suspend fun saveApiKey(profileId: String, apiKey: String)
    suspend fun loadApiKey(profileId: String): String?   // null یعنی ذخیره نشده
    suspend fun deleteApiKey(profileId: String)
    suspend fun hasApiKey(profileId: String): Boolean     // فقط true/false، خودِ کلید افشا نمی‌شود
}
```

چندسرویسی: یک فایل `EncryptedSharedPreferences` مشترک (`secure_api_keys`) با
کلید مجزا به‌ازای هر `profileId` — نه فایل جدا به‌ازای هر سرویس (بدون فایده‌ی
امنیتی اضافه، فقط هزینه‌ی مدیریتی).

## تست

`SecureKeyRepositoryTest.kt` — ۶ تست (Robolectric، با `SharedPreferences`
تزریقی طبق بالا): ذخیره سپس خواندن همان مقدار؛ خواندن یک profileId
ذخیره‌نشده null است؛ پاک‌کردن واقعاً حذف می‌کند؛ دو profileId کاملاً مستقل
(به‌روزرسانی یکی، دیگری را تغییر نمی‌دهد)؛ `hasApiKey` true/false درست؛
`hasApiKey` بعد از حذف false می‌شود.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`SecureKeyRepositoryTest`) | ۶ تست، موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۷۸۰ تست (۷۷۴ + ۶ تازه، دقیقاً مطابق انتظار)، ۲ شکست نامرتبط (`AssetFormFlowTest`، `OutputDeliveryFlowTest`) — flaky شناخته‌شده‌ی محیط؛ هر دو در اجرای مجزا موفق |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این قدم (کار آینده، نه بدهی پنهان)

- قدم ۲: اتصال واقعی HTTP (`sendToAiConnector`، Ktor) که از این Repository
  برای خواندن کلید استفاده کند.
- قدم ۳: UI مدیریت کلید (صفحه‌ی تنظیمات برای وارد/حذف‌کردن کلید هر سرویس).
- تأیید دستی رمزنگاری واقعی روی یک دستگاه/شبیه‌ساز Android واقعی (طبق
  محدودیت مستندشده‌ی بالا).

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
