# ADR-102: G2 — دومین پروفایل واقعی (OpenAI API) + تبدیل هاردکد Claude در UI به انتخابگر واقعی چندپروفایلی

## زمینه

ADR-100 اولین `AiConnectorProfile` واقعی (Claude) را اضافه کرد، و صریحاً مستند
کرد که OpenAI/Gemini/DeepSeek/Qwen خارج از Scope همان قدم‌اند. ADR-101 (G2 قدم
۳) این پروفایل را برای اولین بار به UI وصل کرد، اما صادقانه مستند کرد: «مسیر
۲ در AI Story Breakdown مستقیماً `CLAUDE_API_PROFILE` را صدا می‌زند، نه یک
انتخابگر عمومی... وقتی پروفایل دوم واقعی اضافه شود، این تصمیم باید بازبینی
شود». این قدم دقیقاً همان بازبینی موعود است: دومین پروفایل واقعی (OpenAI) و
تبدیل هاردکد به انتخابگر واقعی.

## بررسی مستقل (پیش از نوشتن کد) — و اختلاف با پیش‌بریفینگ معمار

طبق دستور صریح، بخش «AI Connector Profile» بلوپرینت
`docs/blueprints/01b-ai-story-breakdown.md` دوباره خوانده شد (نه اعتماد
کورکورانه به این پرامپت). یافته: بلوپرینت خودش از ابتدا **هر دو** فرمت را در
نظر داشته — تعریف `responseJsonPath` صریحاً می‌گوید «مسیر ساده (مثل
`content[0].text` یا `choices[0].message.content`)» و کامنت `displayName`
صریحاً می‌گوید «مثلاً "Claude API" یا "OpenAI API"». یعنی OpenAI از روز اول
جزو طراحی این قرارداد بوده، نه یک افزوده‌ی بعدی حدسی — **مطابق پیش‌بریفینگ
معمار، بدون اختلاف**.

**بررسی مستقل فرمت رسمی OpenAI:** دسترسی مستقیم به `platform.openai.com`/
`developers.openai.com` از این محیط Sandbox توسط پراکسی شبکه مسدود است
(`EGRESS_BLOCKED` — محدودیت زیرساخت، نه کوتاهی راستی‌آزمایی). به‌جایش از
WebSearch استفاده شد که موارد زیر را از چند منبع (از‌جمله لینک مستقیم به
صفحات `developers.openai.com/api/reference/...` در نتایج جست‌وجو) تأیید کرد:
Endpoint (`POST https://api.openai.com/v1/chat/completions`)، Header
Authorization با فرمت `Bearer <کلید>`، و بدنه‌ی حداقلی `{model, messages}`.
مسیر پاسخ (`choices[0].message.content`) و فرمت خطا (`{"error":{"message":...}}`،
هم‌ساختار با آنچه `describeHttpError` موجود از قبل پشتیبانی می‌کند) از
دانش عمومی مستندشده‌ی OpenAI تأیید شد (بدون نیاز به تغییر `AiConnector.kt`
خارج از افزودن پروفایل تازه).

**یافته‌ی مهم درباره‌ی نام مدل (اختلاف جزئی با پیش‌بریفینگ، صریحاً گزارش‌شده):**
جست‌وجوی مدل «تعادل توان/هزینه» نشان داد `gpt-5-mini` (پیشنهاد معمار) یک
Model ID واقعی و رسمی است (صفحه‌ی مستند
`developers.openai.com/api/docs/models/gpt-5-mini` در نتایج جست‌وجو دیده
شد)، اما نسخه‌های جدیدتر (`gpt-5.4-mini` و بعدتر — طبق چند منبع قیمت‌گذاری
غیررسمی، احتمالاً نسخه‌های جدیدتری هم بعد از آن منتشر شده‌اند) هم در جست‌وجو
دیده شدند. چون دسترسی مستقیم به صفحه‌ی رسمی هر کدام برای تأیید کامل مسدود
بود (`EGRESS_BLOCKED`)، و منابع غیررسمی (سایت‌های مقایسه‌ی قیمت) برای جزئیات
دقیق مدل‌های تازه قابل‌اعتماد نیستند، **تصمیم گرفته شد به مدل از‌قبل
تأییدشده (`gpt-5-mini`، پیشنهاد معمار) پایبند بمانم** — نه یک ID جدیدتر
حدسی از خلاصه‌ی موتور جستجو. این محدودیت شناخته‌شده‌ی محیط است، نه یک تصمیم
پنهان.

## تصمیم ۱ — `OPENAI_API_PROFILE`: دومین پروفایل واقعی، هم‌الگو دقیق با Claude

```kotlin
val OPENAI_API_PROFILE: AiConnectorProfile = AiConnectorProfile(
    profileId = "openai_api",
    displayName = "OpenAI API",
    endpointUrl = "https://api.openai.com/v1/chat/completions",
    requestBodyTemplate = """{"model":"gpt-5-mini","messages":[{"role":"user","content":"{{PROMPT}}"}]}""",
    requestHeaders = mapOf("Authorization" to "Bearer {{API_KEY}}"),
    responseJsonPath = "choices[0].message.content"
)
```

`BUILTIN_AI_CONNECTOR_PROFILES` اکنون `listOf(CLAUDE_API_PROFILE,
OPENAI_API_PROFILE)` است (افزوده به لیست، نه جایگزینی).

**تفاوت واقعی بین دو سرویس، تأییدشده با تست (نه فرض یکسان‌بودن):** Claude از
`x-api-key` (کلید خام) استفاده می‌کند؛ OpenAI از `Authorization: Bearer
<کلید>`. تستی که این تفاوت را Capture می‌کند (`the real request to OpenAI
carries an Authorization Bearer header, not x-api-key...`) صریحاً بررسی
می‌کند که درخواست واقعی ارسالی به OpenAI هرگز هدر `x-api-key` ندارد.

**Parser عمومی مسیر نقطه‌ای موجود (`extractByJsonPath`) بدون هیچ تغییری برای
فرمت OpenAI هم کار می‌کند** — دقیقاً همان طراحی ADR-100 که برای همین سناریو
ساخته شده بود؛ این ادعا با تست واقعی (`a successful OpenAI response extracts
the correct text via choices index 0 message content`) اثبات شد، نه فرض.

## تصمیم ۲ — `selectedProfileId`/`apiKeySavedForSelectedProfile`، نه یک انتخابگر پیچیده‌تر

`AiStoryBreakdownViewModel`'s `claudeApiKeySaved: StateFlow<Boolean>`
(تک‌پروفایلی، ADR-101) با دو State جایگزین شد — دقیقاً همان نام‌گذاری
پیشنهادی دستور اجرایی:

```kotlin
val selectedProfileId: StateFlow<String>              // پیش‌فرض: اولین پروفایل (Claude)
val apiKeySavedForSelectedProfile: StateFlow<Boolean>  // فقط برای پروفایل انتخاب‌شده
```

`selectProfile(profileId: String): Job` هر دو را به‌روزرسانی می‌کند — هم‌الگو
با `sendPromptAutomatically`/`confirmAndSave` (Job برمی‌گرداند چون
`hasApiKey` داخلاً روی `Dispatchers.IO` واقعی اجرا می‌شود، نه صرفاً
`ioScopeOverride=Unconfined`؛ همان یافته‌ی مستندشده‌ی ADR-101).

`sendPromptAutomatically()` اکنون پروفایل را از `BUILTIN_AI_CONNECTOR_PROFILES`
بر اساس `selectedProfileId` پیدا می‌کند (`firstOrNull`)، نه هاردکد
`CLAUDE_API_PROFILE` — این خودِ بازبینی موعود ADR-101 است.

**شرط سخت‌گیرانه‌ی UI حالا per-profile است:** اگر کاربر OpenAI را انتخاب کرده
باشد ولی فقط کلید Claude ذخیره شده، دکمه‌ی «ارسال خودکار» غیرفعال می‌ماند —
تأییدشده با تست ViewModel (`selectProfile switches ... apiKeySavedForSelectedProfile
reflects only the newly-selected profile's own key`) و تست UI واقعی
(`switching to OpenAI with only a Claude key saved keeps the send button
disabled`).

## تصمیم ۳ — انتخابگر UI: `OpaqueChip` موجود، نه یک Dropdown تازه

دستور اجرایی «مثلاً یک Dropdown ساده» را پیشنهاد داده بود، اما بررسی مستقل
کدبیس (`SettingsScreen.kt`: Language FA/EN، Theme Dark/Light، HomeLayout
Hero/Resume، ComposerLayout Tabs/Accordion — همه دقیقاً همین اندازه‌ی
مجموعه‌ی گزینه‌ها) نشان داد الگوی جاافتاده‌ی این پروژه برای انتخاب از یک
مجموعه‌ی کوچک گزینه‌های منحصربه‌فرد، ردیف `OpaqueChip` (`ui/assets`،
`internal`، از قبل در چند فایل دیگر بازاستفاده‌شده) است، نه Dropdown.
**انحراف از پیشنهاد دستور اجرایی، با دلیل:** یک Dropdown تازه یک الگوی
تعامل جدید به صفحه‌ای اضافه می‌کرد که هیچ‌جای دیگرش از Dropdown استفاده
نمی‌کند؛ Chip Row با زبان طراحی موجود کاملاً هم‌خوان است و برای تعداد کم
پروفایل (فعلاً ۲) به همان اندازه (یا بهتر، چون هر دو گزینه هم‌زمان دیده
می‌شوند بدون باز کردن Menu) کار می‌کند. اگر تعداد پروفایل‌ها در آینده خیلی
زیاد شود (مثلاً ۵+)، این تصمیم باید بازبینی شود — محدودیت شناخته‌شده، نه
پنهان.

ردیف چیپ فقط وقتی `BUILTIN_AI_CONNECTOR_PROFILES.size > 1` نمایش داده می‌شود
(طبق دستور صریح — قبل از این قدم چیزی برای انتخاب نبود).

## تصمیم ۴ — `SettingsScreen.kt`/`ApiKeysViewModel.kt` بدون تغییر

طبق ADR-101، هر دو فایل از قبل روی `BUILTIN_AI_CONNECTOR_PROFILES` پیمایش
می‌کنند (نه یک ردیف هاردکد Claude). این با grep و یک تست واقعی
(`Settings automatically shows a key row for the second real profile
(OpenAI) with no code change needed`) تأیید شد — **بدون نیاز به هیچ تغییری
در این دو فایل**، دقیقاً همان‌طور که ADR-101 وعده داده بود.

## تست

- **`AiConnectorTest.kt` (۴ تست تازه):** `OPENAI_API_PROFILE` فرمت رسمی
  OpenAI را دارد (Endpoint، `responseJsonPath`، Header `Authorization`، بدون
  `x-api-key`)؛ پاسخ موفق واقعی `choices[0].message.content` را استخراج
  می‌کند؛ درخواست واقعی ارسالی Header `Authorization: Bearer <کلید>` را
  Capture‌شده دارد (نه فرض)؛ پاسخ خطای ۴۰۱ به‌فرمت واقعی OpenAI پیام معنادار
  تولید می‌کند.
- **`AiStoryBreakdownViewModelTest.kt` (۲ تست تازه، ۲ تست بازنام‌گذاری‌شده):**
  `selectProfile` واقعاً `selectedProfileId`/`apiKeySavedForSelectedProfile`
  را per-profile به‌روز می‌کند؛ ارسال موفق با پروفایل OpenAI انتخاب‌شده هم
  دقیقاً همان زنجیره‌ی مشترک `applyProcessAiResponseResult` را طی می‌کند و
  به `FINAL_REVIEW` می‌رسد.
- **`ApiKeysFlowTest.kt` (۲ تست تازه، Compose Render مستقیم):** ردیف کلید
  OpenAI در Settings خودکار ظاهر می‌شود؛ چیپ انتخاب پروفایل در AI Story
  Breakdown ظاهر می‌شود و انتخاب OpenAI (بدون کلید ذخیره‌شده‌ی OpenAI) دکمه‌ی
  ارسال خودکار را واقعاً غیرفعال نگه می‌دارد.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`AiConnectorTest`) | ۲۰ تست، موفق |
| `gradle :app:testDebugUnitTest` (`AiStoryBreakdownViewModelTest`) | ۲۱ تست، موفق |
| `gradle :app:testDebugUnitTest` (`ApiKeysFlowTest`) | ۶ تست، موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۰۸ تست (۸۰۰ + ۸ خالص تازه، دقیقاً مطابق انتظار)، ۱ شکست نامرتبط (`OutputDeliveryFlowTest`) — همان flaky شناخته‌شده‌ی محیط مستندشده در ADR-098/096/100/101، در اجرای مجزا موفق |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این قدم (کار آینده)

- Gemini/DeepSeek/Qwen — هرکدام نیازمند بررسی مستقل مستندات رسمی خودشان
  (طبق همان الگوی این دو پروفایل)، خارج از Scope این قدم.
- اگر تعداد پروفایل‌ها به‌طور قابل‌توجه زیاد شود، انتخابگر Chip Row باید به
  یک الگوی مقیاس‌پذیرتر (مثلاً Dropdown واقعی) بازبینی شود (تصمیم ۳ بالا).

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد. `WebSearch` (ابزار پایه، نه
Skill) برای بررسی مستقل فرمت/مدل OpenAI استفاده شد — `WebFetch` مستقیم به
`platform.openai.com`/`developers.openai.com`/`vercel.com`/
`community.openai.com` توسط پراکسی شبکه‌ی این محیط مسدود بود
(`EGRESS_BLOCKED`)، پس فقط از خلاصه‌ی نتایج `WebSearch` استفاده شد — این
محدودیت صریحاً در بخش «بررسی مستقل» بالا مستند شده.
