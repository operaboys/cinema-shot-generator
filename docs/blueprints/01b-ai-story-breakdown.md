# واحد ۰۱ب: تجزیه‌ی داستان با AI بیرونی و تعمیرگر JSON (AI Story Breakdown + JSON Doctor)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**وضعیت:** فعال — واحد کاملاً جدید (پیش از این وجود نداشت)
**وابستگی:** Story & Override (واحد ۰۱، به‌خصوص `StoryContext`)؛ خروجی این واحد به Asset & Continuity (۰۶)، Scene Engine (۰۴)، و Shot Engine (۰۵) داده می‌شود.

این بلوپرینت از تحلیل یک نمونه‌ی اولیه‌ی کارکردی (پروژه‌ی مادر) استخراج شده و طی گفتگوی مستقیم با کاربر طراحی شده است. جایگاهش در توالی ۹ مرحله‌ای (واحد ۱۶): بلافاصله بعد از ۵ سؤال اصلی Story Wizard (واحد ۰۱، مرحله‌ی ۱)، به‌عنوان مرحله‌ی ۱ب، و پیش از رفتن به DNA/Scene/Shot/Asset.

---

## تعریف

Story Wizard (واحد ۰۱) فقط چارچوب مفهومی/سبکی پروژه (`StoryContext`) را می‌سازد — **هیچ داستان واقعی یا Scene/Shot واقعی تولید نمی‌کند.** این واحد آن خلأ را پر می‌کند: کاربر یک داستان کامل و آزاد می‌نویسد، این واحد آن را (همراه با `StoryContext` و چند پارامتر ساختاری) به یک **AI متنی خارجی** (Claude، ChatGPT، Gemini، یا هر مدل مشابه) می‌سپارد تا داستان را به کاراکترها، مکان‌ها، و یک فهرست شات به‌ترتیب بشکند. نتیجه‌ی این تجزیه (یک JSON) توسط این واحد Parse، تعمیر (در صورت خرابی)، و به Scene/Shot/Asset واقعی تبدیل می‌شود.

**اصل بنیادی این واحد (تعیین‌شده در گفتگوی طراحی):** این واحد **مسئولیت‌های معماری داخلی اپ را به AI بیرونی واگذار نمی‌کند.** از AI بیرونی فقط چیزهایی خواسته می‌شود که واقعاً در تخصص آن است (خلاقیت روایی، توصیف، گسترش داستان) — نه مفاهیم فنی داخلی مثل سطح Continuity Lock یا نوع دقیق enum ها. تبدیل داده‌ی ساده‌ی AI به ساختار دقیق داخلی، کاملاً بر عهده‌ی خودِ این واحد است، با مقادیر پیش‌فرض معقول.

---

## معماری کلی

```
StoryContext (واحد ۰۱) + داستان آزاد کاربر + تعداد شات + مدت هر شات
        ↓
Prompt Builder → یک پرامپت متنی آماده برای AI بیرونی
        ↓
        ├─ مسیر ۱: کپی/پیست دستی توسط کاربر (کاربر خودش می‌برد به Claude/ChatGPT/Gemini)
        └─ مسیر ۲: ارسال مستقیم از طریق AI Connector Profile (Ktor Client، کلید API کاربر)
        ↓
   (هر دو مسیر) → یک رشته‌ی متنی خام از AI بیرونی
        ↓
Chunk Combiner (اگر پاسخ در چند بخش آمده بود)
        ↓
JSON Parser
        ├─ موفق → ادامه
        └─ ناموفق → JSON Doctor (نمایش خطا + تعمیر خودکار + رابط دستی)
        ↓
Story-to-Domain Mapper (تبدیل ساختار ساده‌ی AI به Asset/Scene/Shot واقعی، با مقادیر پیش‌فرض)
        ↓
کتابخانه‌ی Asset کامل + تمام Scene/Shot پروژه (آماده برای مراحل بعدی گردش کار)
```

---

## بخش الف — Prompt Builder (ساخت پرامپت درخواست از AI بیرونی)

### ورودی‌های لازم از کاربر (علاوه بر ۵ سؤال اصلی Story Wizard)

| فیلد | توضیح | الزامی |
|---|---|---|
| `freeformStory` | متن کامل و آزاد داستان، به‌قلم کاربر | بله |
| `targetShotCount` | تعداد شات مدنظر برای کل داستان | بله |
| `defaultShotDurationSeconds` | مدت پیش‌فرض هر شات (ثانیه) — مدت کلی از ضرب این دو عدد محاسبه و به AI اعلام می‌شود، جداگانه پرسیده نمی‌شود | بله (پیش‌فرض: ۴) |

⚠️ **تصمیم طراحی (از گفتگو):** «مدت کلی» و «سبک/استایل» به‌عنوان سؤال جداگانه از کاربر گرفته نمی‌شوند — مدت کلی از `targetShotCount × defaultShotDurationSeconds` محاسبه می‌شود؛ سبک/استایل همان `StoryContext` (Genre/Mood/Visual Intent) موجود از ۵ سؤال اصلی Story Wizard است، بدون تکرار پرسش.

### ساختار پرامپت تولیدشده (نمونه‌ی الگو)

```
تو یک نویسنده/کارگردان حرفه‌ای هستی. داستان زیر را به دقیق [targetShotCount] شات سینمایی تقسیم کن،
هرکدام حدود [defaultShotDurationSeconds] ثانیه (مدت کل تقریبی: [targetShotCount × defaultShotDurationSeconds] ثانیه).

سبک/حال‌وهوا: [genre از StoryContext]، [mood از StoryContext]، هدف بصری [visualIntent از StoryContext].

داستان:
[freeformStory]

خروجی را دقیقاً در قالب JSON زیر بده، بدون هیچ توضیح اضافه قبل یا بعد از JSON:
{
  "characters": [ { "name": "...", "description": "...", "role": "main | secondary | background", "gender": "female | male | other" } ],
  "locations": [ { "name": "...", "description": "..." } ],
  "objects": [ { "name": "...", "description": "..." } ],
  "shots": [
    {
      "sceneName": "...",
      "shotNumber": 1,
      "description": "...",
      "characterNames": ["..."],
      "locationName": "...",
      "objectNames": ["..."]
    }
  ]
}

اگر پاسخ طولانی است و باید در چند بخش بفرستی، هر بخش (به‌جز آخری) را با دقیقاً این عبارت پایان بده: [CONTINUE]
```

⚠️ فیلد `role` در خروجی JSON (`main`/`secondary`/`background`) **به‌طور مستقیم به `character_tier` (واحد ۰۶) نگاشت می‌شود** — این یکی از معدود مفاهیم داخلی است که از AI بیرونی خواسته می‌شود، چون به زبان طبیعی و قابل‌فهم برای هر AI عمومی بیان شده (نه یک enum فنی)؛ در بخش «Story-to-Domain Mapper» پایین‌تر دقیق توضیح داده می‌شود.

### پیاده‌سازی مفهومی (Kotlin)

```kotlin
data class StoryBreakdownRequest(
    val storyContext: StoryContext,   // از واحد ۰۱
    val freeformStory: String,
    val targetShotCount: Int,
    val defaultShotDurationSeconds: Float = 4f
)

fun buildStoryBreakdownPrompt(request: StoryBreakdownRequest): String {
    val totalDuration = request.targetShotCount * request.defaultShotDurationSeconds
    // ساخت رشته‌ی پرامپت طبق الگوی بالا؛ genre/mood/visualIntent از request.storyContext خوانده می‌شوند
    return buildString {
        appendLine("تو یک نویسنده/کارگردان حرفه‌ای هستی. داستان زیر را به دقیق ${request.targetShotCount} شات سینمایی تقسیم کن،")
        appendLine("هرکدام حدود ${request.defaultShotDurationSeconds} ثانیه (مدت کل تقریبی: $totalDuration ثانیه).")
        appendLine()
        appendLine("سبک/حال‌وهوا: ${request.storyContext.genre}، ${request.storyContext.moodPrimary}، هدف بصری ${request.storyContext.visualIntent}.")
        appendLine()
        appendLine("داستان:")
        appendLine(request.freeformStory)
        appendLine()
        append(STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION)   // ثابت، شامل نمونه‌ی JSON بالا + دستور [CONTINUE]
    }
}
```

### قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| ۱ | `freeformStory` خالی یا بسیار کوتاه (کمتر از ۵۰ کاراکتر) | **Blocking** |
| ۲ | `targetShotCount` خارج از محدوده‌ی معقول (کمتر از ۱ یا بیشتر از ۱۵۰) | **Blocking** |
| ۳ | `targetShotCount` بیش از ۴۰ | **Warning** — به کاربر اطلاع داده شود که AI احتمالاً پاسخ را در چند بخش (`[CONTINUE]`) می‌فرستد؛ این طبیعی است، نه خطا |

---

## بخش ب — مسیر دریافت پاسخ از AI بیرونی (دو مسیر مستقل، خروجی یکسان)

هر دو مسیر زیر باید دقیقاً به یک تابع مشترک (`processAiResponse`، بخش پ) برسند — نوع مسیر تأثیری در منطق Parse/Repair/Mapping ندارد.

### مسیر ۱: کپی/پیست دستی (همیشه در دسترس، بدون هیچ پیش‌نیاز)

کاربر متن پرامپت (بخش الف) را کپی می‌کند، خودش به یک اپ/سایت AI (هر کدام) می‌رود، جواب را می‌گیرد، و در یک فیلد متنی در همین اپ Paste می‌کند.

### مسیر ۲: AI Connector Profile (اختیاری، فقط اگر کاربر کلید API وارد کند)

طبق تصمیم آینده‌نگر (نه وابسته به یک سرویس خاص)، این مسیر با یک رابط انتزاعی کار می‌کند — دقیقاً هم‌خانواده با فلسفه‌ی Data-driven بودن Model Profile Library (واحد ۱۴).

```kotlin
/** پروفایل یک سرویس AI متنی — کاملاً مستقل از پیاده‌سازی، شبیه ModelProfile در واحد ۱۴. */
data class AiConnectorProfile(
    val profileId: String,
    val displayName: String,           // مثلاً "Claude API" یا "OpenAI API"
    val endpointUrl: String,
    val requestBodyTemplate: String,   // شامل Placeholder ثابت {{PROMPT}} و {{API_KEY}} در صورت نیاز به بدنه
    val requestHeaders: Map<String, String> = emptyMap(),   // مثلاً "Authorization" -> "Bearer {{API_KEY}}"
    val responseJsonPath: String       // مسیر ساده (مثل "content[0].text" یا "choices[0].message.content") برای استخراج متن پاسخ از JSON پاسخ سرویس
)

/**
 * پروفایل‌های آماده برای معروف‌ترین سرویس‌ها (طبق تصمیم: چند پروفایل آماده + امکان افزودن دستی).
 * این‌ها باید طبق مستندات رسمی هر API (که در زمان پیاده‌سازی واقعی بررسی می‌شود، نه حدس) دقیق شوند.
 */
val BUILTIN_AI_CONNECTOR_PROFILES: List<AiConnectorProfile> = listOf(
    // نمونه‌ی مفهومی — جزئیات دقیق request/response هر سرویس در زمان پیاده‌سازی از مستندات رسمی گرفته شود
)

/** کاربر پیشرفته می‌تواند یک پروفایل کاملاً دستی برای سرویس ناشناخته/محلی بسازد. */
fun createCustomAiConnectorProfile(
    displayName: String, endpointUrl: String,
    requestBodyTemplate: String, responseJsonPath: String
): AiConnectorProfile = AiConnectorProfile(generateId(), displayName, endpointUrl, requestBodyTemplate, responseJsonPath = responseJsonPath)

/**
 * ارسال درخواست واقعی — از Ktor Client استفاده می‌کند (طبق Stack کلی پروژه، فقط وقتی کاربر کلید API دارد).
 * پیاده‌سازی واقعی HTTP در زمان کدنویسی مشخص می‌شود؛ این فقط قرارداد ورودی/خروجی است.
 */
suspend fun sendToAiConnector(
    profile: AiConnectorProfile, apiKey: String, prompt: String
): Result<String> {
    TODO("پیاده‌سازی واقعی HTTP Call با Ktor Client در فاز کدنویسی")
}
```

### قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| ۴ | مسیر ۲ انتخاب شده ولی کلید API خالی است | **Blocking** |
| ۵ | مسیر ۲: درخواست HTTP با خطا مواجه شود (شبکه، احراز هویت، Rate Limit) | **Blocking** — پیام خطا باید متن واقعی خطای سرویس را (به‌صورت خلاصه) نشان دهد، نه فقط «خطا رخ داد» |

---

## بخش پ — Chunk Combiner (چسباندن پاسخ‌های چندبخشی)

### چرا لازم است

وقتی `targetShotCount` بزرگ باشد، AI بیرونی ممکن است پاسخ را در چند پیام جدا بفرستد (هر پیام با `[CONTINUE]` تمام می‌شود، طبق دستور صریح در پرامپت بخش الف). این واحد باید این تکه‌ها را (چه از Paste دستی چند بار، چه از چند Response متوالی مسیر API) به یک متن واحد بچسباند، **قبل از** تلاش برای Parse JSON.

### پیاده‌سازی مفهومی

```kotlin
/**
 * تشخیص می‌دهد آیا یک بخش پاسخ ناقص است (با [CONTINUE] تمام شده) و باید منتظر بخش بعدی ماند.
 */
fun isPartialResponse(chunk: String): Boolean = chunk.trimEnd().endsWith("[CONTINUE]")

/**
 * چسباندن هوشمند چند تکه به یک متن واحد — علامت [CONTINUE] از انتهای هر تکه (به‌جز آخری) حذف می‌شود.
 * ترتیب چسباندن دقیقاً همان ترتیبی است که کاربر/سیستم تکه‌ها را وارد کرده (لیست ورودی، نه بازچینی خودکار).
 */
fun smartCombineChunks(chunks: List<String>): String {
    return chunks.joinToString(separator = "") { chunk ->
        chunk.trimEnd().removeSuffix("[CONTINUE]").trimEnd()
    }
}
```

### قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| ۶ | آخرین تکه‌ی واردشده هنوز با `[CONTINUE]` تمام می‌شود (یعنی کاربر فراموش کرده تکه‌ی بعدی را اضافه کند) | **Warning** — به کاربر یادآوری شود که پاسخ هنوز کامل نیست |

---

## بخش ت — JSON Doctor (تشخیص خطا، توضیح ساده، تعمیر خودکار، رابط دستی)

طبق تصمیم صریح در گفتگوی طراحی: این بخش باید **هم خودکار هم دستی** باشد، و خطا باید هم **موقعیت دقیق** هم **توضیح ساده به زبان غیرفنی** داشته باشد.

### جریان کار

```
متن JSON (بعد از Chunk Combiner)
        ↓
تلاش برای Parse مستقیم
        ├─ موفق → ادامه به Story-to-Domain Mapper
        └─ ناموفق ↓
تشخیص نوع خطای رایج (کاما اضافه، نقل‌قول هوشمند، براکت جفت‌نشده، بخش ناقص)
        ↓
تعمیر خودکار (اعمال اصلاح شناخته‌شده) → تلاش مجدد Parse
        ├─ موفق → نمایش نتیجه‌ی تعمیرشده به کاربر برای تأیید نهایی، سپس ادامه
        └‌── هنوز ناموفق ↓
نمایش رابط دستی: متن JSON در یک ویرایشگر متنی، با هایلایت موقعیت تقریبی خطا
+ توضیح ساده‌ی نوع مشکل (نه پیام خام Parser)
+ دکمه‌ی «تلاش دوباره» بعد از ویرایش دستی کاربر
```

### پیاده‌سازی مفهومی

```kotlin
enum class JsonErrorType {
    TRAILING_COMMA,       // کاما اضافه قبل از } یا ]
    SMART_QUOTES,         // نقل‌قول‌های تزئینی (“ ”) به‌جای نقل‌قول استاندارد (")
    UNMATCHED_BRACKET,    // براکت/آکولاد باز بدون بسته‌شدن متناظر
    INCOMPLETE_RESPONSE,  // به‌نظر می‌رسد پاسخ ناقص است (مثلاً یک رشته یا آبجکت نصفه رها شده)
    UNKNOWN               // خطای شناسایی‌نشده — فقط رابط دستی
}

data class JsonDiagnosis(
    val errorType: JsonErrorType,
    val approximateLine: Int?,          // موقعیت تقریبی، نه لزوماً دقیق خط‌به‌خط
    val simpleExplanation: String,      // به زبان ساده، نه پیام خام Parser
    val autoFixable: Boolean
)

/** تشخیص نوع خطا از روی پیام خام Parser + بازرسی ساختاری متن. */
fun diagnoseJsonError(rawJson: String, parserErrorMessage: String): JsonDiagnosis {
    // پیاده‌سازی واقعی: بررسی الگوهای رایج (کاما قبل از بسته‌شدن، وجود کاراکترهای “ ” در متن،
    // شمارش تعادل { } و [ ]، بررسی اینکه رشته‌ی خام با یک مقدار کامل (نه وسط رشته) تمام می‌شود)
    TODO("پیاده‌سازی واقعی الگوریتم تشخیص در فاز کدنویسی")
}

/** تعمیر خودکار — فقط برای انواع خطای autoFixable=true. */
fun attemptAutoFix(rawJson: String, diagnosis: JsonDiagnosis): String? {
    return when (diagnosis.errorType) {
        JsonErrorType.TRAILING_COMMA -> rawJson.replace(Regex(",\\s*([}\\]])"), "$1")
        JsonErrorType.SMART_QUOTES -> rawJson
            .replace('\u201C', '"').replace('\u201D', '"')
            .replace('\u2018', '\'').replace('\u2019', '\'')
        else -> null   // UNMATCHED_BRACKET/INCOMPLETE_RESPONSE/UNKNOWN نیاز به رابط دستی دارند
    }
}
```

### قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| ۷ | JSON بعد از تعمیر خودکار همچنان نامعتبر است | **Blocking** — رابط دستی الزامی می‌شود، پیشروی بدون آن ممکن نیست |
| ۸ | JSON معتبر است ولی فاقد کلیدهای الزامی (`characters`, `locations`, `shots`) | **Blocking** — پیام روشن که «ساختار JSON درست است اما اطلاعات لازم را ندارد» |

---

## بخش ث — Story-to-Domain Mapper (تبدیل داده‌ی ساده‌ی AI به Asset/Scene/Shot واقعی)

این حیاتی‌ترین بخش این واحد است — اینجا تصمیم گرفته شد که **AI بیرونی مسئولیت‌های فنی داخلی را بر عهده ندارد**؛ این تابع خودش با مقادیر پیش‌فرض معقول این تبدیل را انجام می‌دهد.

### نگاشت کاراکترها

```kotlin
// 🆕v2 gender اکنون از AI خواسته می‌شود (به زبان طبیعی: "female"/"male"/"other") چون این هم
// مثل role به‌سادگی قابل‌بیان است؛ نگاشت آن به enum Gender در همین Mapper انجام می‌شود، نه توسط AI.
data class SimpleCharacterFromAi(val name: String, val description: String, val role: String, val gender: String? = null)

/**
 * تبدیل خروجی ساده‌ی AI به CharacterAsset واقعی (واحد ۰۶، نسخه‌ی ۴).
 * character_tier و gender مستقیماً از AI می‌آیند (چون به زبان طبیعی بیان شده‌اند)؛
 * بقیه‌ی فیلدهای فنی (continuity_rules، outfits، expressions) مقدار پیش‌فرض معقول می‌گیرند
 * و کاربر می‌تواند بعداً در Asset Library (مرحله‌ی ۳ گردش کار) دستی دقیق‌شان کند.
 */
fun mapAiCharacterToAsset(aiChar: SimpleCharacterFromAi): CharacterAsset {
    val tier = when (aiChar.role.lowercase()) {
        "main" -> CharacterTier.MAIN
        "background" -> CharacterTier.BACKGROUND
        else -> CharacterTier.SECONDARY   // پیش‌فرض امن اگر AI مقدار غیرمنتظره برگرداند
    }
    // 🆕v2 نگاشت رشته‌ی آزاد AI به enum Gender — اگر AI مقدار ندهد یا نامعتبر باشد، OTHER (محافظه‌کارانه‌ترین پیش‌فرض)
    val gender = when (aiChar.gender?.lowercase()) {
        "female" -> Gender.FEMALE
        "male" -> Gender.MALE
        else -> Gender.OTHER
    }
    return CharacterAsset(
        assetId = generateAssetId(),
        characterTier = tier,
        name = aiChar.name,
        physicalAppearance = PhysicalAppearance(
            ageRange = "unspecified",   // AI معمولاً سن دقیق نمی‌دهد؛ کاربر بعداً در Asset Library تکمیل می‌کند
            gender = gender,
            physicalFeatures = aiChar.description   // توصیف خام AI اینجا هم نگه داشته می‌شود، هم در basePrompt
        ),
        outfits = listOf(defaultOutfitPlaceholder()),   // حداقل یک Outfit پیش‌فرض (طبق Rule 5 واحد ۰۶: حداقل یکی is_default)
        basePrompt = aiChar.description,               // توضیح خام AI، مستقیم به‌عنوان جزئیات آزاد نگه داشته می‌شود
        continuityRules = ContinuityRules()             // پیش‌فرض بلوپرینت ۰۶، مطابق defaultLockLevelForTier(tier)
    )
}
```

### نگاشت مکان‌ها و اشیا

```kotlin
data class SimpleLocationFromAi(val name: String, val description: String)
data class SimpleObjectFromAi(val name: String, val description: String)

/** LocationAsset با continuity_lock_level ثابت STYLE (طبق بلوپرینت ۰۶) — بدون نیاز به تصمیم AI. */
fun mapAiLocationToAsset(aiLoc: SimpleLocationFromAi): LocationAsset = LocationAsset(
    assetId = generateAssetId(), name = aiLoc.name,
    description = aiLoc.description, environment = deriveEnvironmentPlaceholder(),
    basePrompt = aiLoc.description
)

/**
 * 🆕v2 ObjectAsset با continuity_lock_level ثابت FORM (طبق بلوپرینت ۰۶).
 * subtype اکنون فیلد الزامی است (طبق بلوپرینت ۰۶ نسخه ۴) — چون AI بیرونی نمی‌تواند
 * قابل‌اعتماد تشخیص دهد یک شیء «شخصی»، «عمومی»، یا «لباس» است (این تمایز به‌اندازه‌ی
 * role کاراکتر به زبان طبیعی ساده نیست)، پیش‌فرض محافظه‌کارانه GENERAL_PROP انتخاب شد؛
 * کاربر در Asset Library (مرحله‌ی ۳) این را دستی دقیق می‌کند.
 */
fun mapAiObjectToAsset(aiObj: SimpleObjectFromAi): ObjectAsset = ObjectAsset(
    assetId = generateAssetId(), name = aiObj.name, description = aiObj.description,
    subtype = ObjectSubtype.GENERAL_PROP,   // 🆕v2 پیش‌فرض محافظه‌کارانه، کاربر بعداً دقیق می‌کند
    size = "medium", materialAndColor = "نامشخص — نیاز به بررسی کاربر",   // پیش‌فرض‌های حداقلی، کاربر بعداً دقیق می‌کند
    basePrompt = aiObj.description
)
```

### نگاشت شات‌ها (به Scene + Shot)

```kotlin
data class SimpleShotFromAi(
    val sceneName: String, val shotNumber: Int, val description: String,
    val characterNames: List<String>, val locationName: String, val objectNames: List<String>
)

/**
 * شات‌های با sceneName یکسان، به یک Scene واحد گروه‌بندی می‌شوند (طبق ترتیب اولین ظهور).
 * هر Scene با تنظیمات پیش‌فرض معقول (واحد ۰۴) ساخته می‌شود؛ کاربر بعداً می‌تواند دقیق‌شان کند.
 */
fun groupAiShotsIntoScenes(aiShots: List<SimpleShotFromAi>): List<Scene> {
    return aiShots.groupBy { it.sceneName }.map { (sceneName, shotsInScene) ->
        Scene(
            sceneId = generateSceneId(), sceneTitle = sceneName,
            sceneNumber = aiShots.indexOfFirst { it.sceneName == sceneName } + 1,
            narrativeRole = NarrativeRole.DEVELOPMENT,   // 🆕 اصلاح: RISING_ACTION در enum واقعی وجود ندارد (مقادیر واقعی طبق بلوپرینت ۰۴: INTRODUCTION/DEVELOPMENT/CLIMAX/RESOLUTION/TRANSITION)؛ DEVELOPMENT پیش‌فرض خنثی معقول‌تر است، کاربر بعداً دقیق می‌کند
            location = deriveSceneLocationPlaceholder(shotsInScene.first().locationName),
            timeOfDay = TimeOfDay.AFTERNOON, atmospherePrimary = Atmosphere.CALM,   // 🆕 اصلاح: NEUTRAL در enum واقعی Atmosphere وجود ندارد (مقادیر واقعی طبق بلوپرینت ۰۴: CALM/TENSE/DARK/BRIGHT/MYSTERIOUS/EMOTIONAL)؛ CALM نزدیک‌ترین پیش‌فرض خنثی
            shotCount = shotsInScene.size
        )
    }
}

/**
 * تبدیل هر شات ساده‌ی AI به Shot واقعی (واحد ۰۵، نسخه‌ی ۲) — با اتصال به Asset هایی
 * که در مراحل قبلی این Mapper ساخته شدند (بر اساس تطبیق نام؛ اگر نامی یافت نشد، هشدار داده می‌شود، رد نمی‌شود).
 */
fun mapAiShotToShot(
    aiShot: SimpleShotFromAi, sceneId: String,
    characterAssetsByName: Map<String, CharacterAsset>,
    locationAssetsByName: Map<String, LocationAsset>,
    objectAssetsByName: Map<String, ObjectAsset>
): Shot {
    val matchedCharacterIds = aiShot.characterNames.mapNotNull { characterAssetsByName[it]?.assetId }
    // اگر یک نام کاراکتر در aiShot.characterNames با هیچ Asset ساخته‌شده مطابقت نداشت،
    // این باید به‌عنوان یک ValidationIssue (Warning) گزارش شود، نه بی‌صدا نادیده گرفته شود.
    return Shot(
        shotId = generateShotId(), sceneId = sceneId, shotNumber = aiShot.shotNumber,
        shotDescription = aiShot.description,
        shotGoal = ShotGoal.ESTABLISHING, shotType = ShotType.MEDIUM,   // پیش‌فرض‌های خنثی، طبق بلوپرینت ۰۵
        durationSeconds = 4f, motionLevel = MotionLevel.MODERATE,
        soundProfile = SoundProfile(enabled = true),
        characterIds = matchedCharacterIds,
        objectIds = aiShot.objectNames.mapNotNull { objectAssetsByName[it]?.assetId },
        locationIds = locationAssetsByName[aiShot.locationName]?.let { listOf(it.assetId) } ?: emptyList()
    )
}
```

### قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| ۹ | یک نام کاراکتر/مکان/شیء در یک شات، در فهرست Asset های ساخته‌شده یافت نشود (Typo یا ناسازگاری AI) | **Warning** — شات ساخته می‌شود ولی بدون آن Asset خاص؛ کاربر می‌تواند بعداً دستی وصل کند |
| ۱۰ | تعداد شات واقعی تولیدشده توسط AI با `targetShotCount` درخواستی مغایرت قابل‌توجه دارد (مثلاً ±۲۰٪) | **Warning** — صرفاً اطلاع‌رسانی، مانع ادامه نمی‌شود |
| ۱۱ | نتیجه‌ی نهایی Mapper (کتابخانه‌ی Asset + Scene/Shot) پیش از اعمال قطعی، باید یک بار به کاربر برای تأیید نمایش داده شود | ساختاری (نه Validation، بلکه یک گام تعاملی الزامی در UI) |

---

## معیارهای موفقیت

- کاربر می‌تواند یک داستان کامل و آزاد بنویسد و بدون دانستن هیچ جزئیات فنی، یک پروژه‌ی کامل (Asset + Scene + Shot) دریافت کند.
- هر دو مسیر (کپی/پیست دستی و AI Connector مستقیم) به یک منطق پردازش یکسان می‌رسند.
- پاسخ‌های چندبخشی (به‌خاطر تعداد شات زیاد) به‌درستی و بدون از‌دست‌رفتن محتوا چسبانده می‌شوند.
- JSON خراب یا قابل تعمیر خودکار است یا کاربر با اطلاعات کافی (موقعیت + توضیح ساده) می‌تواند دستی تعمیرش کند.
- هیچ مفهوم فنی داخلی (Continuity Lock Level دقیق، enum های فنی) مستقیماً به AI بیرونی واگذار نمی‌شود، به‌جز مواردی که به زبان طبیعی قابل بیان‌اند (مثل `role`).
- کاربر همیشه فرصت بازبینی و تأیید نهایی قبل از اعمال قطعی نتیجه را دارد.

---

## یادداشت پیاده‌سازی (برای Claude Code، هنگام اجرای این بلوپرینت به‌روزشده)

**نسخه:** ۲ — بازنویسی برای هم‌راستاسازی با Migration بلوپرینت `06-asset-and-continuity-v2.md` (نسخه ۴): `mapAiCharacterToAsset` اکنون `Gender` واقعی می‌سازد (نه یک تابع Placeholder جداگانه)؛ `mapAiObjectToAsset` فیلد الزامی `subtype: ObjectSubtype` را با پیش‌فرض `GENERAL_PROP` پر می‌کند. **تغییرات با «🆕v2» علامت‌گذاری شده‌اند.**

این یک واحد **کاملاً جدید** است، نه Migration روی چیز موجود — اما به شدت به ساختارهای موجود (`CharacterAsset`, `LocationAsset`, `ObjectAsset` از واحد ۰۶ نسخه‌ی ۴؛ `Scene` از واحد ۰۴؛ `Shot` از واحد ۰۵ نسخه‌ی ۲؛ `StoryContext` از واحد ۰۱) وابسته است:

- این بلوپرینت باید **بعد از** اجرای Migration های بلوپرینت‌های ۰۲/۰۵/۰۶/۱۴ (که در ADR های قبلی مستند شده‌اند) اجرا شود — چون مستقیماً به فیلدهای جدید آن‌ها (`character_tier`, `defaultMood`, `basePrompt`, `negativePromptOverride`, 🆕v2 `Gender`, `ObjectSubtype`) ارجاع می‌دهد. با grep بررسی کن که آیا آن Migration ها قبلاً اجرا شده‌اند؛ اگر نه، این را به‌عنوان یک پیش‌نیاز مسدودکننده به معمار گزارش بده.
- بخش ب (AI Connector Profile واقعی، شامل `sendToAiConnector` با Ktor Client) در همین قدم فقط باید **ساختار/قرارداد** پیاده شود؛ پیاده‌سازی واقعی HTTP Call و لیست دقیق `BUILTIN_AI_CONNECTOR_PROFILES` (با فرمت request/response واقعی هر سرویس) می‌تواند به یک زیرقدم بعدی موکول شود اگر حجم واقعی حین کار بزرگ‌تر از انتظار بود — طبق الگوی تثبیت‌شده‌ی این پروژه («وقتی حجم واقعی بزرگ‌تر شد، تقسیم کن، ادامه نده»).
- 🆕v2 توابع `defaultOutfitPlaceholder`, `deriveEnvironmentPlaceholder`, `deriveSceneLocationPlaceholder` در این بلوپرینت فقط به‌عنوان مفهوم اشاره شده‌اند (نام‌گذاری نمونه) — پیاده‌سازی واقعی و دقیق مقادیر پیش‌فرض معقول این‌ها بر عهده‌ی تو است، با توجه کامل به Rule های الزامی بلوپرینت‌های ۰۴/۰۶ (مثل Rule 5 واحد ۰۶: هر کاراکتر باید حداقل یک Outfit با `is_default=true` داشته باشد). **توجه:** برخلاف نسخه‌ی قبلی این یادداشت، `derivePhysicalAppearancePlaceholder` دیگر لازم نیست — این نسخه مستقیماً `PhysicalAppearance` واقعی را با `Gender` نگاشت‌شده می‌سازد (بدون تابع Placeholder جداگانه)؛ اگر پیاده‌سازی قبلی چنین تابعی ساخته بود، باید حذف/جایگزین شود.
- 🆕v2 فیلد `gender` به schema JSON درخواست‌شده از AI بیرونی (بخش الف) اضافه شد — این هماهنگی بین بخش الف و بخش ث در همین بازنویسی انجام شده، نیازی به بررسی جداگانه نیست.
- تمام قوانین اعتبارسنجی این بلوپرینت (۱۱ Rule) باید با `ValidationIssue`/`Severity` سراسری واحد ۰۷ پیاده شوند، طبق الگوی تثبیت‌شده در سایر واحدها.
