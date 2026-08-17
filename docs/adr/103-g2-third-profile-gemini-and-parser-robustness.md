# ADR-103: G2 — سومین پروفایل واقعی (Gemini API) + رفع یک یافته‌ی واقعی در extractByJsonPath

## زمینه

ADR-100 اولین `AiConnectorProfile` واقعی (Claude) را اضافه کرد. ADR-102 دومین
پروفایل (OpenAI) را اضافه کرد و انتخابگر مسیر ۲ در AI Story Breakdown را از
هاردکد Claude به یک انتخابگر واقعی چندپروفایلی تبدیل کرد
(`selectedProfileId`/`apiKeySavedForSelectedProfile`). این قدم سومین پروفایل
واقعی (Gemini) را اضافه می‌کند — طبق پیش‌بینی خودِ ADR-102 («اگر تعداد
پروفایل‌ها زیاد شود، تصمیم انتخابگر باید بازبینی شود»)، این قدم اولین آزمون
واقعی طراحی پویای ADR-102 با سومین پروفایل است.

## بررسی مستقل (پیش از نوشتن کد) — و اختلاف با پیش‌بریفینگ معمار

بخش «AI Connector Profile» بلوپرینت `docs/blueprints/01b-ai-story-breakdown.md`
دوباره خوانده شد (نه اعتماد کورکورانه). قرارداد Placeholder ها ({{PROMPT}}/
{{API_KEY}} در `requestBodyTemplate` و `requestHeaders`) دوباره تأیید شد —
**بدون اختلاف** با پیش‌بریفینگ. یافته‌ی تازه (نه در پیش‌بریفینگ): بلوپرینت
هیچ اشاره‌ای به Placeholder در `endpointUrl` ندارد — پس تصمیم Gemini (مدل در
خودِ URL، هاردکد، نه Placeholder) با طراحی موجود کاملاً سازگار است، بدون نیاز
به تغییری در `sendToAiConnector`/`fillPlaceholders`.

**بررسی مستقل فرمت رسمی Gemini:** دسترسی مستقیم به `ai.google.dev` از این
محیط Sandbox هم مثل `platform.openai.com` (ADR-102) توسط پراکسی شبکه مسدود
است (`EGRESS_BLOCKED`). با WebSearch، Endpoint
(`POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent`،
با نام مدل مستقیماً در URL)، Header (`x-goog-api-key`)، بدنه
(`{"contents":[{"parts":[{"text":"..."}]}]}`)، و مدل پیش‌فرض رسمی
(`gemini-2.5-flash`) همگی از یک مثال `curl` مستقیم در نتایج جست‌وجو تأیید
شدند — **مطابق پیش‌بریفینگ معمار، بدون اختلاف** (برخلاف ADR-102 که مدل
پیشنهادی معمار را نگه‌داشتم چون نمی‌توانستم نسخه‌ی جدیدتر را کامل تأیید کنم،
اینجا خودِ `gemini-2.5-flash` هم به‌عنوان «مدل پیش‌فرض» رسمی صریحاً تأیید شد).

## یافته‌ی مهم — بررسی مستقل `thoughtSignature` (اختلاف جزئی با قطعیت پیش‌بریفینگ، صریحاً گزارش‌شده)

پیش‌بریفینگ معمار ادعا کرده بود «یک آیتم `parts` ممکن است فقط
`thoughtSignature` داشته باشد، بدون `text`». WebSearch (چون WebFetch مستقیم
به `ai.google.dev` مسدود بود) این را تأیید کرد، اما با یک دقت بیشتر که در
پیش‌بریفینگ نبود: طبق چند منبع (از‌جمله صفحه‌ی رسمی «Thought Signatures» و
مستندات Google Cloud)، این رفتار عمدتاً در **پاسخ‌های Streaming (آخرین
Chunk)** یا **سناریوهای Function Calling چندمرحله‌ای** رخ می‌دهد — نه لزوماً
در یک `generateContent` ساده و تک‌شات، بدون Function Calling، مثل همین
پروفایل. یعنی این سناریو برای این پروفایل خاص محتمل‌تر از «همیشه» اما
کمتر از «قطعی» است — یک درجه از عدم‌قطعیت که پیش‌بریفینگ به آن اشاره نکرده
بود.

**تصمیم:** چون (۱) رفتار واقعاً مستند و تأییدشده است (نه فرضی محض)، (۲) دقیقاً
همان خانواده‌ی مدل (Gemini ۲.۵ با Thinking) که این پروفایل استفاده می‌کند را
هدف می‌گیرد، و (۳) هزینه‌ی رفع آن در `extractByJsonPath` ناچیز و کاملاً
Backward-Compatible بود (پایین‌تر)، رفع شد — نه فقط مستند. این تصمیم در
دامنه‌ی مجاز این قدم بود (`AiConnector.kt`)، پس نیازی به توقف/گزارش قبل از
رفع نبود.

## تصمیم ۱ — `GEMINI_API_PROFILE`: سومین الگوی متفاوت احراز هویت/بدنه

```kotlin
val GEMINI_API_PROFILE: AiConnectorProfile = AiConnectorProfile(
    profileId = "gemini_api",
    displayName = "Gemini API",
    endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
    requestBodyTemplate = """{"contents":[{"parts":[{"text":"{{PROMPT}}"}]}]}""",
    requestHeaders = mapOf("x-goog-api-key" to "{{API_KEY}}"),
    responseJsonPath = "candidates[0].content.parts[0].text"
)
```

جدول مقایسه‌ی سه الگوی متفاوت این پروژه (تأییدشده با تست، نه فرض یکسان‌بودن):

| | Claude | OpenAI | Gemini |
|---|---|---|---|
| Header احراز هویت | `x-api-key` (خام) | `Authorization: Bearer <کلید>` | `x-goog-api-key` (خام) |
| مدل | در بدنه (`model`) | در بدنه (`model`) | در خودِ `endpointUrl` |
| کلید بیرونی بدنه | `messages` | `messages` | `contents` |
| ساختار محتوا | `content: "..."` (رشته) | `content: "..."` (رشته) | `parts: [{"text": "..."}]` (آرایه) |
| مسیر پاسخ | `content[0].text` | `choices[0].message.content` | `candidates[0].content.parts[0].text` |

`BUILTIN_AI_CONNECTOR_PROFILES` اکنون `listOf(CLAUDE_API_PROFILE,
OPENAI_API_PROFILE, GEMINI_API_PROFILE)` است.

## تصمیم ۲ — رفع `extractByJsonPath`: Fallback به اندیس‌های بعدی همان آرایه

پیاده‌سازی قبلی (تک‌مسیره، بدون Backtrack) با یک نسخه‌ی بازگشتی جایگزین شد که
وقتی یک اندیس آرایه‌ی مشخص کل باقی‌مانده‌ی مسیر را حل نمی‌کند، اندیس‌های بعدی
همان آرایه را هم امتحان می‌کند تا اولین آیتمی که واقعاً حل می‌شود پیدا شود:

```kotlin
private fun resolveJsonPathTokens(current: JsonElement?, tokens: List<String>, tokenIndex: Int): String? {
    if (current == null) return null
    if (tokenIndex == tokens.size) return (current as? JsonPrimitive)?.content
    val token = tokens[tokenIndex]
    return if (token.startsWith("[")) {
        val startIndex = token.removeSurrounding("[", "]").toIntOrNull() ?: return null
        val array = current as? JsonArray ?: return null
        (startIndex until array.size).firstNotNullOfOrNull { index ->
            resolveJsonPathTokens(array.getOrNull(index), tokens, tokenIndex + 1)
        }
    } else {
        resolveJsonPathTokens((current as? JsonObject)?.get(token), tokens, tokenIndex + 1)
    }
}
```

**بدون تغییر رفتار برای Claude/OpenAI:** هر دو پروفایل همیشه دقیقاً یک آیتم
در آرایه‌ی مربوطه دارند — Fallback هرگز لمس نمی‌شود (تأییدشده: تمام تست‌های
موجود این دو پروفایل بدون تغییر سبز ماندند). این تغییر فقط یک مسیرِ شکست
قبلی («اندیس مشخص، بدون بررسی محتوا») را به یک مسیرِ موفقیت («اولین اندیس
واقعاً حل‌شونده») تبدیل می‌کند — هرگز یک استخراج قبلاً موفق را عوض نمی‌کند.

## تصمیم ۳ — بدون هیچ تغییری در UI/ViewModel

طبق ادعای ADR-102 («انتخابگر باید بدون تغییر کد برای پروفایل بعدی کار کند»)،
با grep روی `SettingsScreen.kt`، `ApiKeysViewModel.kt`،
`AiStoryBreakdownViewModel.kt`، `AiStoryBreakdownScreen.kt` تأیید شد **هیچ
هاردکد به تعداد یا نام پروفایل خاصی وجود ندارد** — همه‌جا `BUILTIN_AI_CONNECTOR_PROFILES`
به‌صورت پویا پیمایش می‌شود (`forEach`/`firstOrNull { profileId == ... }`/
`.first()` به‌عنوان پیش‌فرض عمومی، نه فرض تعداد ثابت). **این قدم صفر خط کد در
هیچ‌کدام از این چهار فایل تغییر نداد** — تأیید عملی صحت طراحی پویای ADR-102،
نه فقط ادعا.

## تست

- **`AiConnectorTest.kt` (۴ تست تازه):** پاسخ موفق Gemini استخراج درست
  (`candidates[0].content.parts[0].text`)؛ درخواست واقعی Header
  `x-goog-api-key` را Capture‌شده دارد (نه `x-api-key`/`Authorization`)؛
  خطای ۴۰۰ به‌فرمت واقعی Gemini پیام معنادار تولید می‌کند؛ **تست اختصاصی
  حالت `thoughtSignature`** — `parts[0]` فقط `thoughtSignature` دارد،
  `parts[1]` متن واقعی دارد → استخراج موفق (رفتار واقعی رفع‌شده، نه فرضی).
- **`AiStoryBreakdownViewModelTest.kt` (۱ تست تازه):** ارسال موفق با پروفایل
  Gemini انتخاب‌شده هم دقیقاً همان زنجیره‌ی مشترک `applyProcessAiResponseResult`
  را طی می‌کند و به `FINAL_REVIEW` می‌رسد.
- **`ApiKeysFlowTest.kt` (۲ تست تازه، Compose Render مستقیم):** ردیف کلید
  Gemini در Settings خودکار ظاهر می‌شود (بدون تغییر کد)؛ انتخاب Gemini در
  چیپ پروفایل، دکمه‌ی ارسال خودکار را per-profile (نه با کلید Claude) گیت
  می‌کند.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`AiConnectorTest`) | ۲۴ تست، موفق |
| `gradle :app:testDebugUnitTest` (`AiStoryBreakdownViewModelTest`) | ۲۲ تست، موفق |
| `gradle :app:testDebugUnitTest` (`ApiKeysFlowTest`) | ۸ تست، موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۱۵ تست (۸۰۸ + ۷ خالص تازه، دقیقاً مطابق انتظار)، ۲ شکست نامرتبط (`AssetFormFlowTest`، `OutputDeliveryFlowTest`) — هر دو flaky شناخته‌شده‌ی محیط، مستندشده پیش‌تر در ADR-097/098/100 (`AssetFormFlowTest`) و ADR-098/096/100/101/102 (`OutputDeliveryFlowTest`)؛ هر دو در اجرای مجزا موفق |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این قدم (کار آینده)

- DeepSeek/Qwen — هرکدام نیازمند بررسی مستقل مستندات رسمی خودشان.
- اگر تعداد پروفایل‌ها بیشتر شود، انتخابگر Chip Row (تصمیم ۳ ADR-102) باید
  بازبینی شود.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد. `WebSearch` (ابزار پایه، نه
Skill) برای بررسی مستقل فرمت Gemini و رفتار `thoughtSignature` استفاده شد —
`WebFetch` مستقیم به `ai.google.dev` توسط پراکسی شبکه‌ی این محیط مسدود بود
(`EGRESS_BLOCKED`)، همان محدودیت مستندشده‌ی ADR-102 برای OpenAI.
