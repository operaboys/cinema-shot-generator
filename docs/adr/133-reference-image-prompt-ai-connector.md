# ADR-133: زیرقدم ۳ از ۵ — مسیر اختیاری AI برای «پرامپت ساخت عکس مرجع»

## وضعیت

پذیرفته‌شده

## زمینه

ادامه‌ی ADR-131 (مدل داده) و ADR-132 (موتور Template). این زیرقدم مسیر
اختیاری/دوم (AI) را اضافه می‌کند: یک تابع دامنه که پرامپت Template
زیرقدم ۲ را به یک AI Connector می‌فرستد و نسخه‌ی حرفه‌ای‌تر انگلیسی +
ترجمه‌ی فارسی را با هم برمی‌گرداند. کاملاً مستقل از موتور اصلی پرامپت
ویدیو؛ بدون ViewModel/UI — فقط منطق دامنه.

### راستی‌آزمایی مستقل

پیش از نوشتن کد، `AiConnector.kt`، `PromptBuilder.kt`، و
`StoryToDomainMapper.kt` کامل خوانده شدند:

- `suspend fun sendToAiConnector(profile: AiConnectorProfile, apiKey:
  String, prompt: String, engine: HttpClientEngine =
  OkHttp.create()): Result<String>` — امضای دقیق تأیید شد؛ `engine`
  پیش‌فرض دارد (نه پارامتر الزامی)، نکته‌ای که در تست‌ها لحاظ شد.
- الگوی JSON دوزبانه در `PromptBuilder.kt` خط ۱۲۵-۱۲۶ (دقیقاً همان خطی
  که پیش‌بررسی گفته بود) — جمله‌ی توضیح‌دهنده («کاربر... می‌خواهد پیش از
  استفاده‌ی نهایی...») و ساختار JSON با دو کلید (`descriptionEn`/
  `descriptionFa`) تأیید شد؛ همان الگو برای پرامپت عکس تکرار شد.
- `StoryToDomainMapper.kt`: الگوی `@Serializable data class` +
  `Json.decodeFromString<T>` تأیید شد؛ همچنین کامنت سرفایل آن درباره‌ی
  «انحراف تأییدشده‌ی import مستقیم kotlinx.serialization در لایه‌ی
  domain» (ارجاع به ADR-025/`Renderer.kt`) — همان دلیل برای این فایل هم
  صدق می‌کند.
- `JsonDoctor.kt`: `repairJson`/`validateRequiredKeysPresent` کامل
  خوانده شدند (تصمیم پایین‌تر).
- `AiConnectorTest.kt`: الگوی دقیق `MockEngine { request -> respond(...)
  }` تأیید شد؛ همان الگو در تست‌های این زیرقدم تکرار شد.

هیچ مغایرتی با پیش‌بریفینگ پیدا نشد.

## تصمیم

### فایل تازه

`domain/asset/ImagePromptAiConnector.kt` — سه بخش:

**۱. `buildImagePromptAiRequest(templatePrompt, styleTokens): String`**:
متن دستورالعمل انگلیسی/فارسی به AI — غنی‌سازی پرامپت Template (جزئیات
فتوگرافی/سینمایی بیشتر، در همان چارچوب سبک) + درخواست دقیق JSON با دو
کلید `imagePromptEn`/`imagePromptFa`، هم‌الگو با
`STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION_BILINGUAL`.

**۲. `@Serializable data class ImagePromptAiResponse(imagePromptEn,
imagePromptFa)`** + `parseImagePromptAiResponse(rawResponseText):
Result<ImagePromptAiResponse>` — `runCatching { Json.decodeFromString
(...) }`، خطای Parse به `Result.failure` با پیام قابل‌فهم (نه Crash).

**۳. `suspend fun generateImagePromptWithAi(templatePrompt, styleTokens,
profile, apiKey, engine): Result<ImagePromptAiResponse>`** — ساخت
درخواست → `sendToAiConnector` (بدون هیچ تغییر در آن تابع) → Parse.

### چرا بدون `repairJson`/`smartCombineChunks` (JsonDoctor.kt/ChunkCombiner.kt)

طبق دستور کار صریح، این تصمیم مستقل بررسی و اینجا مستند شد:

- `smartCombineChunks` (ChunkCombiner.kt) مشکل پاسخ‌های **چندبخشی** AI
  Story Breakdown (که با `[CONTINUE]` پاره‌پاره می‌شوند، چون خروجی آن
  می‌تواند ده‌ها شات را شامل شود) را حل می‌کند. پاسخ این زیرقدم یک شیء
  JSON **تک‌شات و کوچک** با فقط دو کلید رشته‌ای است — هیچ‌جا درخواست
  `[CONTINUE]` داده نمی‌شود، پس این ابزار اصلاً موضوعیت ندارد.
- `repairJson` (schema-agnostic: trailing comma/smart quotes/bracket
  imbalance/incomplete-response) نظری می‌توانست کمک کند، اما
  `validateRequiredKeysPresent` کنارش مستقیماً قفل به کلیدهای Story
  Breakdown (`REQUIRED_TOP_LEVEL_KEYS = listOf("characters",
  "locations", "shots")`) است — برای Schema این زیرقدم
  (`imagePromptEn`/`imagePromptFa`) بدون تغییر قابل‌استفاده‌ی مجدد
  نیست؛ و طبق قانون این قدم، هیچ فایل دیگری (`JsonDoctor.kt`) نباید
  لمس/گسترش داده شود.
- خودِ `JsonDoctor.kt` در کامنت سرفایلش به
  `domain/promptfinalization/PromptFinalizationPipeline.kt` به‌عنوان
  الگوی ساده‌تر (`runCatching { Json.parseToJsonElement(...) }` بدون
  تعمیر کامل) برای مواردی که نیازی به جریان کامل Repair ندارند ارجاع
  می‌دهد — دقیقاً همین الگوی ساده‌تر (با `decodeFromString` کامل به‌جای
  `parseToJsonElement` لق، چون Schema این زیرقدم فقط دو رشته‌ی ساده و
  از پیش مشخص است) در این فایل استفاده شد.

اگر در عمل پاسخ‌های ناقص/بدشکل برای این Schema کوچک واقعاً رایج شد، این
تصمیم در زیرقدم‌های بعدی قابل‌بازبینی است.

### چرا `@Serializable` مستقیم در `domain/asset/`

هم‌الگو با انحراف تأییدشده‌ی مشابه در
`StoryToDomainMapper.kt`/`Renderer.kt` (ADR-025): این نوع مستقیماً از
JSON خام AI Decode می‌شود، بدون DTO میانی جداگانه در `data/repository/`
که در این قدم هنوز معنا ندارد — هیچ Repository ای این داده را ذخیره
نمی‌کند (ذخیره‌سازی در `imagePromptAi`/`imagePromptFaPreview` کار
UI/ViewModel زیرقدم ۴/۵ است).

## پیامدها

- موتور اصلی پرامپت ویدیو و `AiConnector.kt`/`PromptBuilder.kt`/
  `StoryToDomainMapper.kt`/`JsonDoctor.kt`/`ChunkCombiner.kt` کاملاً
  دست‌نخورده ماندند — فقط یک فایل تازه اضافه شد که از `sendToAiConnector`
  Import/فراخوانی می‌کند.
- ذخیره‌سازی خروجی این تابع در `imagePromptAi`/`imagePromptFaPreview`
  (ADR-131) هنوز کار زیرقدم‌های بعدی است.
- این فقط **زیرقدم ۳ از ۵** است. UI/ViewModel (شامل بررسی
  `SecureKeyRepository`/`validateApiKeyProvided`/وضعیت
  InProgress/Result/Error، هم‌الگو با `analyzePromptQualityWithAi`) و
  اتصال به فیلدهای ذخیره‌شده هنوز آغاز نشده‌اند — زیرقدم‌های ۴ و ۵.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`: موفق.
- `ImagePromptAiConnectorTest.kt` (۷ تست تازه — ساخت درخواست، Parse
  موفق دوزبانه، Parse ناموفق JSON ناقص، Parse ناموفق متن کاملاً
  غیر-JSON، مسیر کامل موفق با MockEngine، مسیر خطای HTTP، مسیر پاسخ
  HTTP موفق ولی JSON نامعتبر): همه Pass. هیچ فراخوان واقعی شبکه.
- `domain.asset.*`/`domain.storybreakdown.*` (شامل `AiConnectorTest`/
  `StoryToDomainMapper` تست‌های موجود): همه Pass، بدون رگرسیون — فقط دو
  فایل تازه (کد + تست) اضافه شدند.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۹۸۲ تست، ۹۸۱ Pass،
  ۱ Fail — `OutputDeliveryFlowTest`، از پیش در فهرست کلاس‌های ناپایدار
  (Flaky) مستندشده‌ی این پروژه (بدون هیچ ارتباطی با این تغییرات — تحویل
  خروجی، نه Asset). با اجرای مجدد و مجزا کامل Pass شد — تأیید شد
  Flakiness شناخته‌شده است، نه رگرسیون.
- `./gradlew :app:assembleDebug`: موفق.
