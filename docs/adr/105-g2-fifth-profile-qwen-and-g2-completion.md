# ADR-105: G2 — پنجمین و آخرین پروفایل واقعی (Qwen API) — تکمیل کامل G2

## زمینه

ADR-100 اولین `AiConnectorProfile` واقعی (Claude) را اضافه کرد. ADR-102 دومین
(OpenAI) را اضافه کرد و انتخابگر مسیر ۲ را از هاردکد Claude به یک انتخابگر
واقعی چندپروفایلی تبدیل کرد. ADR-103 سومین (Gemini) و ADR-104 چهارمین
(DeepSeek) را اضافه کردند — هر دو با تأیید عملی مکرر که UI/ViewModel بدون
هیچ تغییری برای پروفایل جدید کار می‌کنند. این قدم پنجمین و **آخرین** پروفایل
برنامه‌ریزی‌شده (Qwen) را اضافه می‌کند. **با این قدم، G2 («اتصال واقعی به AI
Connector») به‌طور کامل بسته می‌شود.**

## بررسی مستقل (پیش از نوشتن کد)

بخش «AI Connector Profile» بلوپرینت `docs/blueprints/01b-ai-story-breakdown.md`
دوباره خوانده شد — قرارداد Placeholder بدون تغییر تأیید شد.

**بررسی مستقل فرمت رسمی Qwen (Alibaba Cloud Model Studio / DashScope):**
`WebFetch` مستقیم به `www.alibabacloud.com` مثل سه سرویس قبلی از این محیط
Sandbox مسدود بود (`EGRESS_BLOCKED`). با WebSearch چندمنبعی:

- **Qwen عمداً با فرمت OpenAI Chat Completions سازگار است** — همان
  `Authorization: Bearer <کلید>`، همان بدنه‌ی `{model, messages}`، همان
  مسیر پاسخ `choices[0].message.content`. **مطابق پیش‌بریفینگ، بدون اختلاف.**

## یافته‌ی معماری حیاتی — نوع Endpoint (تأییدشده، مطابق هشدار پیش‌بریفینگ)

بررسی مستقل چندمنبعی این هشدار حیاتی پیش‌بریفینگ را **کاملاً تأیید کرد**:
Alibaba Cloud برای برخی مناطق (Singapore/Tokyo و مشابه) یک الگوی URL می‌دهد
که `WorkspaceId` شخصیِ هر کاربر را داخل خودِ مسیر URL دارد:

```
https://{WorkspaceId}.ap-southeast-1.maas.aliyuncs.com/compatible-mode/v1/...
```

این با معماری `AiConnectorProfile` این پروژه **ناسازگار** است — `endpointUrl`
یک مقدار `String` ثابت و از‌پیش‌تعیین‌شده در کد است (`data class` ساده، بدون
هیچ مکانیزم Template/جایگزینی per-user برای بخش‌های خودِ URL، فقط برای
`{{PROMPT}}`/`{{API_KEY}}` طبق قرارداد بلوپرینت). اضافه‌کردن پشتیبانی از
یک Placeholder سوم (`{WorkspaceId}`) که در زمان اجرا از یک منبع دیگر (کجا؟
فیلد جدید کاربر در Settings؟) پر شود، یک گسترش معماری واقعی و غیرضروری برای
این قدم بود — **رد شد، عمداً، نه فراموش‌شده**.

به‌جایش از الگوی بدون `WorkspaceId` استفاده شد که رسماً هم وجود دارد و به‌طور
مستقل تأیید شد کاملاً کاربردی است:

```
https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions
```

**بین نوع داخل‌چین (`dashscope.aliyuncs.com`) و بین‌المللی
(`dashscope-intl.aliyuncs.com`):** بررسی مستقل نشان داد کلید API باید با
منطقه‌ی ثبت حساب Alibaba Cloud هم‌خوان باشد؛ نوع بین‌المللی صریحاً برای
کاربران خارج از چین مستند شده و استاندارد کلید DashScope را می‌پذیرد. چون
این پروژه به یک کشور خاص محدود نیست، نوع بین‌المللی انتخاب شد — **مطابق
پیشنهاد معمار، تأییدشده نه فرض**.

## تصمیم ۱ — `QWEN_API_PROFILE`

```kotlin
val QWEN_API_PROFILE: AiConnectorProfile = AiConnectorProfile(
    profileId = "qwen_api",
    displayName = "Qwen API",
    endpointUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions",
    requestBodyTemplate = """{"model":"qwen-plus","messages":[{"role":"user","content":"{{PROMPT}}"}]}""",
    requestHeaders = mapOf("Authorization" to "Bearer {{API_KEY}}"),
    responseJsonPath = "choices[0].message.content"
)
```

**مدل: `qwen-plus`** — یک نام پایدار و غیر-نسخه‌دار، نه یک نام نسخه‌دار مثل
`qwen3.6-plus`/`qwen3.7-plus` که هم در جست‌وجو دیده شدند اما ریسک بازنشستگی
دارند — دقیقاً همان درسی که از یافته‌ی مدل منسوخ DeepSeek (ADR-104) گرفته
شد. بررسی مستقل `qwen-plus` را به‌عنوان نامی پایدار و همچنان معتبر (کنار
`qwen-max`/`qwen-flash`) تأیید کرد — مطابق پیشنهاد معمار، بدون نیاز به
جایگزینی.

`BUILTIN_AI_CONNECTOR_PROFILES` اکنون `listOf(CLAUDE_API_PROFILE,
OPENAI_API_PROFILE, GEMINI_API_PROFILE, DEEPSEEK_API_PROFILE,
QWEN_API_PROFILE)` است — پنج عضو، پنج‌تای برنامه‌ریزی‌شده، کامل.

## تصمیم ۲ — بدون Helper مشترک (طبق ADR-104، بدون تغییر)

بررسی کد فعلی دلیل قانع‌کننده‌ای برای بازبینی تصمیم ADR-104 («بدون Helper
مشترک برای پروفایل‌های سازگار با OpenAI») به دست نداد — همان استدلال
Simplicity هنوز صادق است، حتی با سه پروفایل هم‌فرمت (OpenAI، DeepSeek،
Qwen). این پروفایل هم یک `val` کاملاً مستقل است.

## تصمیم ۳ — بدون هیچ تغییری در UI/ViewModel (سومین تأیید عملی متوالی)

با grep روی `SettingsScreen.kt`/`ApiKeysViewModel.kt`/
`AiStoryBreakdownViewModel.kt`/`AiStoryBreakdownScreen.kt` تأیید شد (هم‌الگو
با ADR-103/104) هیچ هاردکد به تعداد یا نام پروفایل خاصی وجود ندارد. **این
قدم هم صفر خط کد در این چهار فایل تغییر داد** — سومین تأیید عملی متوالی صحت
طراحی پویای ADR-102، اکنون با پنج پروفایل واقعی، برنامه‌ریزی‌شده به‌طور کامل.

## تست

- **`AiConnectorTest.kt` (۳ تست تازه + ۱ تست بازنویسی‌شده به تست نهایی
  لیست):** پاسخ موفق Qwen استخراج درست (`choices[0].message.content`،
  عیناً هم‌شکل OpenAI/DeepSeek)؛ درخواست واقعی Header
  `Authorization: Bearer` و بدنه شامل نام مدل پایدار (`qwen-plus`) را
  Capture‌شده دارد؛ **تست اختصاصی ریسک اصلی این قدم** — `endpointUrl` نهایی
  هیچ Placeholder جای‌خالی‌مانده (`{`) ندارد و از نوع بدون WorkspaceId
  است (نه الگوی `*.maas.aliyuncs.com`)؛ تست نهایی `BUILTIN_AI_CONNECTOR_PROFILES`
  را برای دقیقاً پنج پروفایل (نه کمتر، نه بیشتر) تأیید می‌کند.
- **`AiStoryBreakdownViewModelTest.kt` (۱ تست تازه):** ارسال موفق با پروفایل
  Qwen انتخاب‌شده هم دقیقاً همان زنجیره‌ی مشترک `applyProcessAiResponseResult`
  را طی می‌کند و به `FINAL_REVIEW` می‌رسد.
- **`ApiKeysFlowTest.kt` (۲ تست تازه، Compose Render مستقیم):** ردیف کلید
  Qwen در Settings خودکار ظاهر می‌شود (بدون تغییر کد)؛ انتخاب Qwen در چیپ
  پروفایل، دکمه‌ی ارسال خودکار را per-profile گیت می‌کند.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`AiConnectorTest`) | ۳۱ تست، موفق |
| `gradle :app:testDebugUnitTest` (`AiStoryBreakdownViewModelTest`) | ۲۴ تست، موفق |
| `gradle :app:testDebugUnitTest` (`ApiKeysFlowTest`) | ۱۲ تست، موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۲۷ تست (۸۲۱ + ۶ خالص تازه، دقیقاً مطابق انتظار)، ۱ شکست نامرتبط (`BackupsFlowTest`) — flaky شناخته‌شده‌ی محیط Sandbox، مستندشده پیش‌تر در ADR-078/097؛ در اجرای مجزا موفق |
| `gradle :app:assembleDebug` | موفق |

## نتیجه — G2 به‌طور کامل بسته شد

با این قدم، هر پنج پروفایل واقعی برنامه‌ریزی‌شده (Claude، OpenAI، Gemini،
DeepSeek، Qwen) اضافه شدند و G2 («اتصال واقعی به AI Connector») به‌طور کامل
بسته می‌شود. کاربر می‌تواند در Settings برای هر پنج سرویس کلید API وارد
کند، در AI Story Breakdown هر سرویس را از یک انتخابگر واقعی انتخاب کند، و
با یک کلیک درخواست را مستقیماً به سرویس انتخاب‌شده بفرستد — همه از طریق
همان زیرساخت مشترک (`sendToAiConnector`، `extractByJsonPath`،
`applyProcessAiResponseResult`) که در پنج قدم قبلی (ADR-098 تا ADR-105)
ساخته شد، بدون کد تکراری.

## خارج از Scope (کار آینده)

- سرویس‌های دیگر (اگر بعداً لازم شد) از طریق `createCustomAiConnectorProfile`
  (کاربر پیشرفته، از قبل موجود) یا یک پروفایل `BUILTIN` تازه با همین الگوی
  مستندشده اضافه می‌شوند.
- اگر نام مدل `qwen-plus` دوباره بازنشسته شد (طبق سابقه‌ی نام‌های مدل در این
  فایل)، این ADR منبع تاریخ‌دار برای رهگیری تغییر بعدی است.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد. `WebSearch`/`WebFetch` (ابزارهای
پایه، نه Skill) برای بررسی مستقل فرمت Qwen و نوع Endpoint استفاده شدند —
`WebFetch` مستقیم به `alibabacloud.com` توسط پراکسی شبکه‌ی این محیط مسدود بود
(`EGRESS_BLOCKED`)، همان محدودیت مستندشده‌ی ADR-102/103/104.
