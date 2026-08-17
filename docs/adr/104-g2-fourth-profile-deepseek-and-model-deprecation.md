# ADR-104: G2 — چهارمین پروفایل واقعی (DeepSeek API) + یافته‌ی حیاتی بازنشستگی نام مدل قدیمی

## زمینه

ADR-100 اولین `AiConnectorProfile` واقعی (Claude) را اضافه کرد. ADR-102 دومین
(OpenAI) را اضافه کرد و انتخابگر مسیر ۲ را از هاردکد Claude به یک انتخابگر
واقعی چندپروفایلی تبدیل کرد. ADR-103 سومین (Gemini) را اضافه کرد و با
grep/تست عملاً تأیید کرد UI/ViewModel بدون هیچ تغییری برای پروفایل سوم کار
می‌کنند. این قدم چهارمین پروفایل واقعی (DeepSeek) را اضافه می‌کند — دومین
آزمون عملی طراحی پویای ADR-102.

## بررسی مستقل (پیش از نوشتن کد)

بخش «AI Connector Profile» بلوپرینت `docs/blueprints/01b-ai-story-breakdown.md`
دوباره خوانده شد — قرارداد Placeholder ({{PROMPT}}/{{API_KEY}} در
`requestBodyTemplate`/`requestHeaders`) بدون تغییر تأیید شد.

**بررسی مستقل فرمت رسمی DeepSeek:** `WebFetch` مستقیم به `api-docs.deepseek.com`
مثل `platform.openai.com` (ADR-102) و `ai.google.dev` (ADR-103) توسط پراکسی
شبکه‌ی این محیط مسدود بود (`EGRESS_BLOCKED`). با WebSearch چندمنبعی:

- **DeepSeek عمداً با فرمت OpenAI Chat Completions سازگار است** — همان
  `Authorization: Bearer <کلید>`، همان بدنه‌ی `{model, messages}`، همان
  مسیر پاسخ `choices[0].message.content`. **مطابق پیش‌بریفینگ، بدون اختلاف.**
- **Endpoint — رفع ابهام `/v1/`:** پیش‌بریفینگ صریحاً به یک ابهام واقعی اشاره
  کرده بود (منابع مختلف کمی متفاوت). بررسی چندمنبعی (از‌جمله عنوان مستقیم
  صفحه‌ی رسمی «Chat Completions API | DeepSeek API Docs» در نتایج جست‌وجو)
  مشخص کرد مسیر رسمی مستندشده **بدون** `/v1/` است
  (`https://api.deepseek.com/chat/completions`)؛ `/v1/` فقط برای سازگاری با
  SDK های رسمی OpenAI پذیرفته می‌شود، نه بخشی از مسیر رسمی خودِ DeepSeek. این
  ابهام واقعاً رفع شد، نه فرض.

## یافته‌ی حیاتی — بازنشستگی نام‌های مدل قدیمی (تأییدشده چندمنبعی، تاریخ‌دار)

پیش‌بریفینگ معمار ادعا کرده بود `deepseek-chat`/`deepseek-reasoner` در
۲۴ جولای ۲۰۲۶ بازنشسته شدند و باید با `deepseek-v4-pro`/`deepseek-v4-flash`
جایگزین شوند. WebSearch چندمنبعی (چند دامنه‌ی مستقل، همگی با جزئیات
سازگار — ساعت دقیق ۱۵:۵۹ UTC، تاریخ دقیق، مکانیزم Routing) این را **تأیید
کرد**، با یک اصلاح جزئی نسبت به پیش‌بریفینگ:

> `deepseek-chat` و `deepseek-reasoner` هرگز مدل‌های مستقل نبودند — دو
> برچسب Routing به دو حالت (Non-Thinking/Thinking) همان مدل زیرین بودند. در
> دوره‌ی گذار، **هر دو** به `deepseek-v4-flash` Route می‌شدند (نه یکی به
> `-pro` و دیگری به `-flash` به‌عنوان دو جایگزین مستقل، آن‌طور که پیش‌بریفینگ
> فرض کرده بود). `deepseek-v4-pro` یک مدل جداگانه و قوی‌تر است، نه جایگزین
> مستقیم هیچ‌کدام از دو نام قدیمی.

چون تاریخ فعلی پروژه (۲۰۲۶-۰۸-۱۷) نزدیک یک ماه پس از این بازنشستگی است، و
درخواست‌های با نام‌های قدیمی دیگر بدون خطا کار نمی‌کنند (تأییدشده چندمنبعی،
بدون Grace Period/Alias)، از `deepseek-v4-flash` استفاده شد — همان مدل
پیشنهادی معمار، مستقل تأییدشده، **نه** نام‌های منسوخ.

**سطح اطمینان و شفافیت محدودیت:** مثل ADR-102 (مدل OpenAI)، تأیید ۱۰۰٪ از
صفحه‌ی رسمی خودِ DeepSeek ممکن نبود (`EGRESS_BLOCKED`). منابع WebSearch
چندگانه و به‌شدت سازگار بودند (تاریخ/ساعت/مکانیزم یکسان در چند دامنه‌ی
مستقل)، که اطمینان را نسبت به یک منبع تنها بالا می‌برد، اما همچنان یک تأیید
غیرمستقیم (نه Fetch مستقیم صفحه‌ی رسمی) است — این محدودیت صریحاً ثبت می‌شود
تا اگر بعداً دوباره نام مدل تغییر کرد (طبق دستور صریح: «قابل ردیابی باشد»)،
منشأ این تصمیم روشن باشد.

## تصمیم ۱ — `DEEPSEEK_API_PROFILE`

```kotlin
val DEEPSEEK_API_PROFILE: AiConnectorProfile = AiConnectorProfile(
    profileId = "deepseek_api",
    displayName = "DeepSeek API",
    endpointUrl = "https://api.deepseek.com/chat/completions",
    requestBodyTemplate = """{"model":"deepseek-v4-flash","messages":[{"role":"user","content":"{{PROMPT}}"}]}""",
    requestHeaders = mapOf("Authorization" to "Bearer {{API_KEY}}"),
    responseJsonPath = "choices[0].message.content"
)
```

`BUILTIN_AI_CONNECTOR_PROFILES` اکنون `listOf(CLAUDE_API_PROFILE,
OPENAI_API_PROFILE, GEMINI_API_PROFILE, DEEPSEEK_API_PROFILE)` است.

## تصمیم ۲ — بدون Helper مشترک برای «پروفایل‌های سازگار با OpenAI»

با اینکه `DEEPSEEK_API_PROFILE`/`OPENAI_API_PROFILE` ساختاری تقریباً یکسان
دارند (فقط `endpointUrl`/نام مدل متفاوت)، تصمیم گرفته شد **بدون** یک تابع/
Helper مشترک، این پروفایل هم مثل سه‌تای قبلی یک `val` کاملاً مستقل باشد.
**دلیل:** هم‌الگو با بقیه‌ی این فایل (هر پروفایل مستند و مستقل، با KDoc
اختصاصی توضیح‌دهنده‌ی منبع تأیید هر فیلد)؛ یک Helper مشترک فقط برای دومین
نقطه‌ی استفاده (پس از OpenAI) پیچیدگی بی‌دلیل اضافه می‌کرد (طبق فلسفه‌ی
Simplicity پروژه — YAGNI). اگر سرویس سازگار سوم/چهارمی (مثلاً یک سرویس
دیگر با فرمت OpenAI) بعداً اضافه شد، این تصمیم قابل‌بازبینی است.

## تصمیم ۳ — بدون هیچ تغییری در UI/ViewModel

با grep روی `SettingsScreen.kt`/`ApiKeysViewModel.kt`/
`AiStoryBreakdownViewModel.kt`/`AiStoryBreakdownScreen.kt` تأیید شد (هم‌الگو
با ADR-103) هیچ هاردکد به تعداد یا نام پروفایل خاصی وجود ندارد. **این قدم هم
صفر خط کد در این چهار فایل تغییر داد** — دومین تأیید عملی متوالی صحت طراحی
پویای ADR-102، اکنون با چهار پروفایل واقعی.

## تست

- **`AiConnectorTest.kt` (۳ تست تازه):** پاسخ موفق DeepSeek استخراج درست
  (`choices[0].message.content`، عیناً هم‌شکل OpenAI)؛ درخواست واقعی Header
  `Authorization: Bearer` را Capture‌شده دارد و بدنه شامل نام مدل تازه
  (`deepseek-v4-flash`) است؛ **تست اختصاصی «قاطی نشدن»** — OpenAI و DeepSeek
  در یک اجرا صدا زده می‌شوند و صریحاً تأیید می‌شود هرکدام واقعاً به
  `endpointUrl` خودشان می‌رود، نه دیگری (نه فرض «چون فرمت یکسان است، احتمالاً
  درست کار می‌کند»).
- **`AiStoryBreakdownViewModelTest.kt` (۱ تست تازه):** ارسال موفق با پروفایل
  DeepSeek انتخاب‌شده هم دقیقاً همان زنجیره‌ی مشترک
  `applyProcessAiResponseResult` را طی می‌کند و به `FINAL_REVIEW` می‌رسد.
- **`ApiKeysFlowTest.kt` (۲ تست تازه، Compose Render مستقیم):** ردیف کلید
  DeepSeek در Settings خودکار ظاهر می‌شود (بدون تغییر کد)؛ انتخاب DeepSeek
  در چیپ پروفایل، دکمه‌ی ارسال خودکار را per-profile گیت می‌کند.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`AiConnectorTest`) | ۲۷ تست، موفق |
| `gradle :app:testDebugUnitTest` (`AiStoryBreakdownViewModelTest`) | ۲۳ تست، موفق |
| `gradle :app:testDebugUnitTest` (`ApiKeysFlowTest`) | ۱۰ تست، موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۲۱ تست (۸۱۵ + ۶ خالص تازه، دقیقاً مطابق انتظار)، ۱ شکست نامرتبط (`OutputDeliveryFlowTest`) — همان flaky شناخته‌شده‌ی محیط، مستندشده پیش‌تر در چندین ADR؛ در اجرای مجزا موفق |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این قدم (کار آینده)

- Qwen — نیازمند بررسی مستقل مستندات رسمی خودش.
- اگر نام مدل `deepseek-v4-flash` دوباره تغییر کرد (طبق سابقه‌ی این سرویس در
  همین چند ماه)، این ADR منبع تاریخ‌دار برای رهگیری تغییر بعدی است.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد. `WebSearch` (ابزار پایه، نه
Skill) برای بررسی مستقل فرمت DeepSeek و وضعیت نام مدل استفاده شد —
`WebFetch` مستقیم به `api-docs.deepseek.com` توسط پراکسی شبکه‌ی این محیط
مسدود بود (`EGRESS_BLOCKED`)، همان محدودیت مستندشده‌ی ADR-102/ADR-103.
