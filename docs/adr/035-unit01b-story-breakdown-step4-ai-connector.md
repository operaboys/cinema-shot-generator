# ADR-035: واحد ۰۱ب (AI Story Breakdown) — قدم چهارم و آخر: AI Connector Profile

**تاریخ:** 2026-07-28
**وضعیت:** کامل شد و مستقل قابل‌کامپایل/تست است. `gradle :app:assembleDebug
:app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۵۰۲ تست، ۰ Failure، ۰ Error.
**آخرین قدم واحد ۰۱ب — با این قدم، کل واحد از نظر منطق دامنه کامل است.**

## Context

بخش ب بلوپرینت `01b-ai-story-breakdown.md` مسیر ۲ (AI Connector Profile) را تعریف
می‌کند — یک لایه‌ی انتزاعی برای ارسال مستقیم پرامپت به یک سرویس AI بیرونی (به‌جای
کپی/پیست دستی کاربر، مسیر ۱). بلوپرینت خودش صریحاً پیاده‌سازی واقعی HTTP را `TODO()`
گذاشته بود و اجازه داده بود این کار «اگر حجم واقعی بزرگ‌تر از انتظار بود» به قدمی
جداگانه موکول شود.

## تصمیم مرکزی: Option A (فقط قرارداد/ساختار، بدون Ktor واقعی)

معمار صریحاً Option A را ترجیح داده بود، با شرط بازبینی مستقل من. بررسی:

- **پیش‌بررسی:** با grep در `app/build.gradle.kts` تأیید شد Ktor Client هنوز به هیچ
  ماژولی وصل نشده؛ اما `gradle/libs.versions.toml` از قبل ورژن (`ktor = "3.5.1"`) و
  دو Artifact (`ktor-client-core`, `ktor-client-okhttp`) را از پیش اعلام کرده —
  کامنت خودِ فایل می‌گوید «Stack مصوب... اما هنوز به هیچ ماژولی وصل نشده‌اند».
- **هزینه‌ی واقعی Option B (اگر انتخاب می‌شد):** افزودن Ktor به `build.gradle.kts` +
  انتخاب Engine مناسب Android (OkHttp) + پیاده‌سازی HTTP واقعی طبق مستندات رسمی هر
  سرویس (که این پروژه هنوز حتی یک پروفایل واقعی AI Connector ندارد تا در برابرش تست
  شود) + زیرساخت Mock Engine برای تست بدون شبکه‌ی واقعی. این دقیقاً همان الگوی
  «Iceberg» است که این پروژه بارها با آن روبه‌رو شده (مثل تصمیم‌های قبلی «وقتی حجم
  واقعی بزرگ‌تر شد، تقسیم کن، ادامه نده» در ADR-017/۰۱b خودش).
- **نتیجه:** Option A انتخاب شد. `sendToAiConnector` عمداً `TODO()` می‌ماند —
  یک `body` معتبر Kotlin (نوع بازگشتی `Nothing`, زیرنوع هر تایپی از جمله
  `Result<String>`) که در Runtime `NotImplementedError` پرتاب می‌کند، نه خطای
  کامپایل. این با تست صریح تأیید شد (`sendToAiConnector throws NotImplementedError...`)
  تا مستند شود این عمداً پیاده نشده، نه یک باگ خاموش.

## پیاده‌سازی `AiConnector.kt`

`AiConnectorProfile` عیناً طبق بلوپرینت. `BUILTIN_AI_CONNECTOR_PROFILES = emptyList()`
— افزودن پروفایل‌های واقعی (Claude API، OpenAI API، ...) نیازمند بررسی مستندات رسمی هر
سرویس است (تحقیق API خارجی، نه کار دامنه‌محور این قدم) — به قدم جداگانه‌ی آینده موکول
شد (احتمالاً هم‌زمان با Option B، اگر آن روز رسید).

**یافته‌ی جزئی در متن بلوپرینت (اصلاح‌شده، نه انحراف عمدی):** کد مفهومی
`createCustomAiConnectorProfile` بلوپرینت پارامتر `jsonPathResponse` را در فراخوانی
سازنده به کار برده بود، درحالی‌که خودِ همان بلوپرینت چند خط بالاتر فیلد
`responseJsonPath` را در `data class AiConnectorProfile` تعریف کرده — این دو نام یکی
نیستند، یک Typo واقعی در متن بلوپرینت. اینجا با نام درست `responseJsonPath` پیاده شد.

### شناسه (`generateId`)

تابع `generateId(prefix)` که در `StoryToDomainMapper.kt` (قدم سوم) تعریف شده بود از
`private` به `internal` تغییر داد تا `AiConnector.kt` (همان پکیج) هم بتواند از همان
مولد شناسه‌ی مشترک استفاده کند (پیشوند `"aiconnector"`) — به‌جای تعریف یک تابع تکراری
مشابه در فایل جدید.

### Rule 4/5

`validateApiKeyProvided(apiKey)`: Blocking وقتی خالی/whitespace-only. `validateAiConnectorErrorMessage(result:
Result<String>)`: طبق تصمیم Option A، فقط ساختار `Result.failure` را بررسی می‌کند —
اگر `exception.message` غیرخالی باشد، همان پیام واقعی را در متن Issue تکرار می‌کند
(نه فقط «خطا رخ داد»)؛ اگر پیام غایب باشد، یک اطلاعیه‌ی صریح («پیام خطای واقعی سرویس
در دسترس نیست») برمی‌گرداند. وقتی Option B در آینده پیاده شود، همین قرارداد باید
حفظ شود — فقط منبع `Exception` واقعی HTTP خواهد بود.

## تست

`AiConnectorTest.kt` — ۱۰ تست: `BUILTIN_AI_CONNECTOR_PROFILES` خالی است (سلامت
تصمیم)؛ `createCustomAiConnectorProfile` (ساخت صحیح + شناسه‌ی یکتا در هر فراخوانی)؛
Rule 4 (خالی/whitespace/معتبر)؛ `sendToAiConnector` واقعاً `NotImplementedError`
می‌دهد (نه کرش خاموش دیگری)؛ Rule 5 (موفق→null، شکست با پیام→پیام واقعی در Issue،
شکست بدون پیام→اطلاعیه‌ی جایگزین صریح).

جمعاً ۱۰ تست جدید (۴۹۲ → ۵۰۲).

## خارج از Scope این قدم (کار آینده، نه بدهی پنهان)

- پیاده‌سازی واقعی HTTP با Ktor Client (افزودن dependency، انتخاب Engine، تست با Mock
  Engine).
- فهرست واقعی `BUILTIN_AI_CONNECTOR_PROFILES` (Claude API، OpenAI API، Gemini، ...)
  با جزئیات دقیق request/response از مستندات رسمی هرکدام.

هر دو به‌صراحت به‌عنوان کار آینده در کامنت کد و در README مستند شدند، نه یک ادعای
اتمام کامل.

## Consequences

- **آسان می‌شود:** قرارداد کامل مسیر ۲ (پروفایل، ساخت دستی، Rule های اعتبارسنجی)
  آماده است؛ وقتی Ktor واقعاً اضافه شود، فقط بدنه‌ی `sendToAiConnector` و فهرست
  `BUILTIN_AI_CONNECTOR_PROFILES` نیاز به تغییر دارند، نه امضای توابع یا Rule ها.
- **کار آینده‌ی شناخته‌شده (نه بدهی پنهان):** HTTP واقعی + پروفایل‌های واقعی سرویس‌ها.
