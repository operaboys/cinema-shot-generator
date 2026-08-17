# ADR-100: G2 قدم ۲ از ۳ — اتصال HTTP واقعی به Claude API

## زمینه

ADR-035 (Option A) عمداً `sendToAiConnector` را `TODO()` نگه داشته بود.
ADR-098 (G2 قدم ۱) لایه‌ی ذخیره‌سازی امن کلید را مستقل ساخت — این قدم به آن
وابسته **نیست** (کلید همچنان پارامتر ورودی `sendToAiConnector` است؛ خواندنش
از `SecureKeyRepository` کار UI/قدم ۳ است). این قدم فقط اتصال HTTP واقعی و
اولین `AiConnectorProfile` واقعی (Claude API، الگوی سرویس‌های بعدی) را اضافه
می‌کند.

## بررسی مستقل (پیش از نوشتن کد)

- `app/build.gradle.kts`/`gradle/libs.versions.toml`: تأیید شد `ktor =
  "3.5.1"` و دو Artifact (`ktor-client-core`, `ktor-client-okhttp`) از قبل
  در کاتالوگ اعلام شده‌اند اما به هیچ ماژولی وصل نیستند.
- `docs/blueprints/01b-ai-story-breakdown.md` (بخش ب، مسیر ۲): امضای
  `AiConnectorProfile` و قرارداد Placeholder ها دوباره خوانده و تأیید شد —
  `{{PROMPT}}`/`{{API_KEY}}` هم در `requestBodyTemplate` و هم در
  `requestHeaders` به کار می‌روند (نمونه‌ی صریح خودِ بلوپرینت:
  `"Authorization" -> "Bearer {{API_KEY}}"`).
- فرمت رسمی Claude API: تأییدشده مستقیم از
  `platform.claude.com/docs/en/build-with-claude/working-with-messages`
  (نه حدس) — Endpoint، سه Header الزامی، بدنه، و `content[0].text` دقیقاً
  مطابق پیش‌بررسی معمار بودند.
- کتابخانه‌ی JSON: با grep تأیید شد پروژه از قبل `kotlinx-serialization-json`
  دارد (`AssetRepository.kt` و جاهای دیگر) — از همان استفاده شد؛
  `ktor-client-content-negotiation`/`ktor-serialization-kotlinx-json`
  **اضافه نشدند** (تصمیم مستقل، دلیل پایین‌تر).

## یافته‌ی جزئی (اصلاح‌شده): جایگزینی Placeholder فقط در بدنه نبود

دستور اجرایی این قدم فقط جایگزینی `{{PROMPT}}`/`{{API_KEY}}` در
`requestBodyTemplate` را خواسته بود و برای `requestHeaders` گفته بود «روی
درخواست اعمال کن» (بدون ذکر جایگزینی Placeholder). با خواندن دوباره‌ی خودِ
بلوپرینت مشخص شد این ناقص بود — بلوپرینت صریحاً نمونه‌ای با Placeholder در
Header نشان داده (`Bearer {{API_KEY}}`)، و پروفایل واقعی Claude API هم به
همین دلیل نیاز دارد (`x-api-key: {{API_KEY}}`). **پیاده‌سازی نهایی هر دو
(بدنه و Header ها) را Placeholder-جایگزینی می‌کند**، نه فقط بدنه.

## تصمیم ۱ — بدون ContentNegotiation/کتابخانه‌ی JSON تایپ‌شده؛ یک Parser عمومی مسیر نقطه‌ای

به‌جای نصب `ktor-client-content-negotiation` + یک `data class` تایپ‌شده
مخصوص Claude، یک تابع خصوصی عمومی (`extractByJsonPath`) نوشته شد که مسیر
ساده‌ی نقطه‌ای/براکت (`content[0].text`، `choices[0].message.content` — هر
دو نمونه‌ی صریح خودِ بلوپرینت) را روی هر `JsonElement` دنبال می‌کند.

**دلیل:** `responseJsonPath` خودش یک فیلد Data-driven روی هر `AiConnectorProfile`
است — قفل‌کردن Parse به یک `data class` مخصوص Claude، هدف اصلی چندسرویسی‌بودن
همین قرارداد را (که این قدم قرار است «الگوی سرویس‌های بعدی» باشد) نقض
می‌کرد. با یک Parser عمومی، افزودن OpenAI/Gemini/... در آینده فقط نیازمند
یک `AiConnectorProfile` جدید با `responseJsonPath` درست است، نه کد Parse
تازه.

## تصمیم ۲ — Timeout: اتصال ۱۵ ثانیه، کل درخواست/Socket ۶۰ ثانیه

شبکه‌ی موبایل فرض شد (نه همیشه‌سریع). ۱۵ ثانیه برای برقراری اتصال TCP/TLS
سخاوتمندانه است حتی روی شبکه‌ی ضعیف؛ ۶۰ ثانیه برای کل درخواست چون تولید پاسخ
توسط یک LLM (تا `max_tokens=4096`) خودش می‌تواند ده‌ها ثانیه طول بکشد —
جدا از تأخیر شبکه.

## تصمیم ۳ — HttpClient per-call (نه نمونه‌ی سراسری Reuse‌شونده)

`AiConnector.kt` کاملاً Stateless است (توابع top-level، بدون کلاس/چرخه‌حیات).
هر فراخوان `sendToAiConnector` یک `HttpClient` می‌سازد و در `finally` می‌بندد.
هزینه‌ی از‌دست‌رفته‌ی Connection Pooling ناچیز است — این درخواست‌ها با اقدام
مستقیم کاربر رخ می‌دهند (نه با فرکانس بالا)؛ نگه‌داشتن یک Client سراسری
نیازمند مدیریت چرخه‌حیات (کِی بسته شود؟) بود که با فلسفه‌ی Stateless این فایل
هم‌خوان نبود.

## پروفایل واقعی: `CLAUDE_API_PROFILE`

```kotlin
AiConnectorProfile(
    profileId = "claude_api",
    displayName = "Claude API",
    endpointUrl = "https://api.anthropic.com/v1/messages",
    requestBodyTemplate = """{"model":"claude-sonnet-5","max_tokens":4096,"messages":[{"role":"user","content":"{{PROMPT}}"}]}""",
    requestHeaders = mapOf("x-api-key" to "{{API_KEY}}", "anthropic-version" to "2023-06-01"),
    responseJsonPath = "content[0].text"
)
```

مدل `claude-sonnet-5` — تعادل توان/هزینه برای Story Breakdown (بلوپرینت خودش
ترجیحی برای انتخاب مدل نداده). `max_tokens=4096` — کافی برای خروجی JSON
چندشاته‌ی بخش الف؛ اگر پاسخ در چند بخش با `[CONTINUE]` بیاید،
`ChunkCombiner.kt` موجود پروژه (قدم اول واحد ۰۱ب) از قبل این را پوشش می‌دهد.

`BUILTIN_AI_CONNECTOR_PROFILES` اکنون شامل همین یک پروفایل است —
OpenAI/Gemini/DeepSeek/Qwen هرکدام نیازمند بررسی مستقل مستندات رسمی خودشان
هستند (خارج از Scope؛ طبق عنوان صریح این قدم: «اولین پروفایل واقعی، الگوی
سرویس‌های بعدی»، نه فهرست کامل).

## تست

`AiConnectorTest.kt`: تست `sendToAiConnector throws NotImplementedError`
(دیگر معتبر نیست، چون HTTP واقعی شد) حذف و با ۷ تست تازه با
`io.ktor.client.engine.mock.MockEngine` جایگزین شد (**بدون هیچ تماس واقعی
اینترنت**، طبق grep قبل از commit — تنها ارجاع‌های `api.anthropic.com` در
تست، مقایسه‌ی رشته‌ای با `.endpointUrl` است):
- پاسخ موفق → متن صحیح از `responseJsonPath` استخراج می‌شود.
- بدنه/Header واقعاً Placeholder جایگزین‌شده را می‌گیرند (Prompt/apiKey
  واقعی در درخواست ارسالی).
- ۴۰۱ با بدنه‌ی خطای Anthropic-محور → `Result.failure` با پیام واقعی
  (`error.message`).
- ۴۲۹ Rate Limit → همان الگو.
- شکل JSON غیرمنتظره (`responseJsonPath` پیدا نمی‌شود) → شکست تمیز، نه Crash.
- بدنه‌ی غیر-JSON → شکست تمیز، نه Crash.
- Timeout شبکه — چون `requestTimeoutMillis` واقعی (۶۰ ثانیه) هاردکد است،
  صبر واقعی در تست غیرمنطقی بود؛ MockEngine مستقیماً همان
  `java.net.SocketTimeoutException` را از داخل handler پرتاب می‌کند — دقیقاً
  همان مسیر catch واقعی را تست می‌کند، بدون Timer واقعی.

تست `BUILTIN_AI_CONNECTOR_PROFILES is empty` (دیگر معتبر نیست) با یک تست
تازه جایگزین شد که فیلدهای واقعی `CLAUDE_API_PROFILE` را تأیید می‌کند.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`AiConnectorTest`) | ۱۶ تست، موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۷۸۶ تست (۷۸۰ + ۶ خالص تازه)، ۲ شکست نامرتبط (`AssetFormFlowTest`، `OutputDeliveryFlowTest`) — flaky شناخته‌شده‌ی محیط؛ هر دو در اجرای مجزا موفق |
| `gradle :app:assembleDebug` | موفق |
| `grep api.anthropic.com app/src/test` | فقط مقایسه‌ی رشته‌ای، بدون تماس واقعی |

## خارج از Scope این قدم (کار آینده)

- قدم ۳: UI انتخابگر مسیر ۱ (کپی/پیست دستی)/۲ (AI Connector)، شامل خواندن
  کلید از `SecureKeyRepository.kt` (ADR-098).
- فهرست پروفایل‌های واقعی سرویس‌های دیگر (OpenAI، Gemini، DeepSeek، Qwen).

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
