# ADR-033: واحد ۰۱ب (AI Story Breakdown) — قدم دوم: JSON Doctor

**تاریخ:** 2026-07-28
**وضعیت:** کامل شد و مستقل قابل‌کامپایل/تست است. `gradle :app:assembleDebug
:app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۴۷۰ تست، ۰ Failure، ۰ Error.

## Context

`docs/blueprints/01b-ai-story-breakdown.md` بخش ت (JSON Doctor) الگوریتم واقعی
`diagnoseJsonError` را عمداً `TODO()` گذاشته بود («پیاده‌سازی واقعی در فاز کدنویسی») —
این قدم همان فاز کدنویسی است. بلوپرینت جهت کلی هر ۴ نوع خطا را توضیح داده بود اما
جزئیات دقیق الگوریتم (خصوصاً `INCOMPLETE_RESPONSE` و اولویت بررسی بین انواع خطا) کاملاً
باز گذاشته شده بود — تصمیم‌های زیر مربوط به همین بخش‌اند.

## بررسی الگوی موجود پروژه (طبق دستور کار)

با grep تأیید شد تنها الگوی مشابه «Parse لق JsonElement/JsonObject بدون data class
کامل» در کل پروژه در `domain/promptfinalization/PromptFinalizationPipeline.kt` است:
`runCatching { Json.parseToJsonElement(...) }.getOrNull() as? JsonObject`. این الگو
عیناً در `validateRequiredKeysPresent`/`repairJson` این فایل هم استفاده شد.

## پیاده‌سازی `JsonDoctor.kt`

`JsonErrorType`، `JsonDiagnosis` عیناً طبق بلوپرینت. `attemptAutoFix` عیناً کپی (regex
کاما اضافه + جایگزینی نقل‌قول‌های تزئینی).

### تصمیم ۱: ترتیب بررسی در `diagnoseJsonError` — TRAILING_COMMA → SMART_QUOTES →
INCOMPLETE_RESPONSE → UNMATCHED_BRACKET → UNKNOWN

ترتیب تعریف enum (که بلوپرینت داده) لزوماً اولویت بررسی نیست — بلوپرینت این را صریح
نکرده. تصمیم: دو خطای «متنی ساده» (کاما اضافه، نقل‌قول تزئینی) اول بررسی می‌شوند چون
تشخیصشان قطعی و بدون هم‌پوشانی با بقیه است. سپس `INCOMPLETE_RESPONSE` **قبل از**
`UNMATCHED_BRACKET` بررسی می‌شود — دلیل: یک پاسخ واقعاً بریده‌شده (چون AI وسط یک تکه قطع
کرده) تقریباً همیشه هم شمارش براکت را به‌هم می‌زند (چون بخشی از ساختار اصلاً نیامده) هم
تقریباً همیشه یک رشته‌ی نیمه‌کاره یا خاتمه در غیر یک بستارِ معتبر دارد. اگر ابتدا فقط
شمارش براکت را چک کنیم، این حالت را همیشه (نادرست) `UNMATCHED_BRACKET` تشخیص می‌دهیم و
`INCOMPLETE_RESPONSE` هرگز رخ نمی‌دهد. با بررسی اول علائم دقیق‌تر «ناتمامی» (نقل‌قول فرد،
یا خاتمه در غیر `}`/`]`) پیش از شمارش خام براکت، این دو نوع خطا واقعاً از هم تفکیک
می‌شوند — نمونه: `unmatchedBracketJson` تست (که با یک `]` معتبر تمام می‌شود ولی یک `}`
بیرونی جا افتاده) درست `UNMATCHED_BRACKET` تشخیص داده می‌شود، در حالی که `incompleteJson`
تست (رشته‌ی نیمه‌کاره، `"desc` بدون بسته‌شدن) درست `INCOMPLETE_RESPONSE` تشخیص داده
می‌شود — با اجرای واقعی تست تأیید شد.

### تصمیم ۲: الگوریتم `looksIncomplete` — نیازمند «شروع شبیه JSON»

بررسی «ناتمامی» فقط وقتی اعمال می‌شود که متن با `{` یا `[` شروع شده باشد. بدون این
شرط، هر متن غیر-JSON دلخواه (مثلاً یک جمله‌ی فارسی معمولی) که به‌طور تصادفی با `}`/`]`
تمام نمی‌شود هم `INCOMPLETE_RESPONSE` تشخیص داده می‌شد — که غلط است؛ آن متن اصلاً یک
تلاش JSON نبوده، باید `UNKNOWN` باشد. با این شرط، `garbageText` تست («این یک متن کاملاً
غیر JSON است...») درست به `UNKNOWN` می‌رسد.

الگوریتم نهایی (بعد از عبور شرط شروع): تعداد نقل‌قول‌های unescaped فرد → ناتمام (رشته‌ی
باز رها شده)؛ وگرنه آخرین کاراکتر غیر `}`/`]` → ناتمام (خاتمه در وسط کلید/مقدار). این
یک تشخیص ساده و تقریبی است، دقیقاً طبق تصریح بلوپرینت («یک تشخیص ساده و معقول کافی
است، نه یک Parser کامل از صفر»؛ «موقعیت تقریبی، نه لزوماً دقیق خط‌به‌خط»).

### تصمیم ۳: `findBracketImbalance` — شمارش خام، نه Parser با Stack

طبق تصریح بلوپرینت («شمارش تعادل { } و [ ]»)، پیاده‌سازی صرفاً تعداد کاراکترهای باز/بسته
را می‌شمارد، نه یک Parser واقعی با Stack که تودرتویی صحیح را تضمین کند. **محدودیت
شناخته‌شده و پذیرفته‌شده:** این شمارش نمی‌تواند `{`/`[` داخل یک رشته‌ی JSON (مثلاً
توصیف یک کاراکتر که تصادفاً شامل کاراکتر `{` باشد) را از براکت ساختاری واقعی تشخیص دهد
— یک False Positive نادر و بعید (AI بیرونی به‌ندرت چنین کاراکترهایی در توصیف متنی
می‌گذارد)، پذیرفته‌شده به‌عنوان همان سادگی‌ای که بلوپرینت خواسته بود.

### تصمیم ۴: `JsonRepairResult` (sealed class جریان کامل)

`Success(repairedJson, wasAutoFixed)` / `NeedsManualRepair(diagnosis)` — نام و ساختار
طبق دستور کار به من واگذار شده بود. `repairJson()` دقیقاً نمودار جریان کار بلوپرینت را
پیاده می‌کند: Parse مستقیم → موفق (Success، wasAutoFixed=false) → ناموفق →
diagnoseJsonError → اگر autoFixable، attemptAutoFix + Parse مجدد → موفق (Success،
wasAutoFixed=true) → هنوز ناموفق یا اصلاً autoFixable نبود → NeedsManualRepair.

### Rule 7/8

`validateJsonRepairResult(result)`: Blocking وقتی `NeedsManualRepair`. `validateRequiredKeysPresent(rawJson)`:
فقط وقتی JSON خودش معتبر Parse شود (`root: JsonObject`) کلیدهای `characters`/
`locations`/`shots` را چک می‌کند — اگر JSON اصلاً نامعتبر باشد `null` برمی‌گرداند (نه
Blocking)، چون آن حالت را Rule 7 پوشش می‌دهد، طبق شرط دقیق بلوپرینت («JSON معتبر است
ولی فاقد کلیدهای الزامی»). `objects` عمداً در فهرست کلیدهای الزامی نیست — متن دقیق
Rule 8 فقط `characters, locations, shots` را نام می‌برد.

## خارج از Scope این قدم

بخش ب (AI Connector) و بخش ث (Story-to-Domain Mapper) — طبق دستور کار صریح، در
قدم‌های بعدی این واحد می‌آیند.

## تست

`JsonDoctorTest.kt` — ۱۳ تست: JSON کاملاً معتبر (بدون Rule 7/8)؛ TRAILING_COMMA
(تشخیص + تعمیر خودکار)؛ SMART_QUOTES (تشخیص + تعمیر خودکار)؛ UNMATCHED_BRACKET
(تشخیص، عدم تعمیر خودکار، `NeedsManualRepair`، Rule 7 Blocking)؛ INCOMPLETE_RESPONSE
(تشخیص)؛ Rule 8 (کلید `shots` غایب Blocking؛ null وقتی خودِ JSON نامعتبر است)؛ UNKNOWN
(متن کاملاً غیرمرتبط)؛ `attemptAutoFix` روی انواع غیرقابل‌تعمیر null برمی‌گرداند.

جمعاً ۱۳ تست جدید (۴۵۷ → ۴۷۰).

## Consequences

- **آسان می‌شود:** بخش ث (Story-to-Domain Mapper، قدم بعدی) می‌تواند مستقیماً از
  `repairJson`/`JsonRepairResult.Success.repairedJson` به‌عنوان ورودی Parse ساختاریافته
  استفاده کند.
- **بدهی پذیرفته‌شده:** `findBracketImbalance` یک شمارش ساده است (تصمیم ۳)، نه یک
  Parser با آگاهی از رشته‌ها — ریسک عملی پایین، آگاهانه پذیرفته شد.
