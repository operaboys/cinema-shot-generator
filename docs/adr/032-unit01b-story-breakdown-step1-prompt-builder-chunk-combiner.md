# ADR-032: واحد ۰۱ب (AI Story Breakdown) — قدم اول: Prompt Builder + Chunk Combiner

**تاریخ:** 2026-07-28
**وضعیت:** کامل شد و مستقل قابل‌کامپایل/تست است (طبق الزام صریح دستور کار — بر خلاف
یک Migration قبلی که عمداً یک وضعیت میانی رها شد). `gradle :app:assembleDebug
:app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۴۵۷ تست، ۰ Failure، ۰ Error.

## Context

`docs/blueprints/01b-ai-story-breakdown.md` (نسخه ۲) یک واحد کاملاً جدید تعریف می‌کند
که ۵ بخش دارد (الف: Prompt Builder، ب: AI Connector، پ: Chunk Combiner، ت: JSON Doctor،
ث: Story-to-Domain Mapper). دستور کار این قدم صریحاً فقط بخش‌های الف و پ را خواسته بود؛
بخش‌های ب/ت/ث عمداً دست‌نخورده ماندند.

## بررسی پیش‌نیاز (طبق تصریح خودِ بلوپرینت)

بلوپرینت صراحتاً می‌گوید این واحد باید بعد از Migration های ۰۲/۰۵/۰۶/۱۴ اجرا شود، چون به
فیلدهای جدید آن‌ها ارجاع می‌دهد. با grep تأیید شد همه‌ی این Migration ها قبلاً کامل شده‌اند
(`character_tier`، `Gender`، `ObjectSubtype`، `defaultMood`، `basePrompt`،
`negativePromptOverride` — همگی در `domain/asset/`/`domain/dna/`/`domain/shot/` موجودند).
پیش‌نیاز مسدودکننده‌ای پیدا نشد.

## پیاده‌سازی

### `domain/storybreakdown/PromptBuilder.kt`

`StoryBreakdownRequest`، `STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION` (متن دقیق طبق نمونه‌ی
بلوپرینت)، `buildStoryBreakdownPrompt`، و سه تابع Rule (`validateFreeformStoryLength`،
`validateTargetShotCountRange`، `validateHighShotCount`) — هرکدام یک تابع مستقل، هم‌الگو
با سبک granular موجود در `AssetValidation.kt`/`DnaValidation.kt` (نه یک تابع تجمیعی
واحد که همه‌ی Rule ها را با هم چک کند).

**انحراف مستند از کد مفهومی بلوپرینت:** `request.storyContext.genre` در کد واقعی
`List<Genre>` است (نه یک مقدار تکی) — تأییدشده با grep در `domain/story/StoryContext.kt`
(واحد ۰۱ اجازه‌ی انتخاب چند ژانر همزمان می‌دهد). کد مفهومی بلوپرینت `${request.storyContext.genre}`
را مستقیم Interpolate می‌کرد که روی یک `List` خروجی پیش‌فرض Kotlin با براکت
(`[ACTION, DRAMA]`) تولید می‌کند — برای متنی که قرار است پرامپت طبیعی خوانا برای یک AI
باشد، `joinToString("، ")` انتخاب شد تا فهرست ژانرها طبیعی نوشته شود. `moodPrimary`/
`visualIntent` بدون تغییر مستقیم Interpolate شدند (خروجی enum پیش‌فرض Kotlin) — هم‌الگو با
سبک تثبیت‌شده‌ی پروژه در `PromptAssembly.kt` (مثل `"${input.dna.coreIdentity.dominantVisualStyle} style"`).

### `domain/storybreakdown/ChunkCombiner.kt`

`isPartialResponse`/`smartCombineChunks` عیناً از کد مفهومی بلوپرینت کپی شدند (بدون
تفسیر). `validateChunksComplete` (Rule 6) نام‌گذاری شد به‌جای نام مبهم‌تر
`validateLastChunk` یا مشابه — هم‌الگو با نام‌گذاری Rule های موجود پروژه (مثل
`validateAssetDeletion`، `validateChunksComplete` واضح‌تر بیان می‌کند موضوع خودِ کل
مجموعه‌ی chunks است، نه یک chunk تکی).

## خارج از Scope این قدم (طبق دستور کار صریح)

- بخش ب (AI Connector Profile واقعی، `sendToAiConnector` با Ktor Client، فهرست دقیق
  `BUILTIN_AI_CONNECTOR_PROFILES`)
- بخش ت (JSON Doctor — `diagnoseJsonError`, `attemptAutoFix`)
- بخش ث (Story-to-Domain Mapper — `mapAiCharacterToAsset`, `mapAiLocationToAsset`,
  `mapAiObjectToAsset`, `groupAiShotsIntoScenes`, `mapAiShotToShot`)

هرکدام در پرامپت‌های جداگانه‌ی بعدی این واحد پیاده می‌شوند — هرکدام هم باید طبق همان
الزام این قدم، در پایانش خودش کامل و کامپایل‌شونده تحویل شود.

## تست

- `PromptBuilderTest.kt`: ۴ تست برای `buildStoryBreakdownPrompt` (شامل داستان/تعداد
  شات/مدت محاسبه‌شده/ژانر-حال‌وهوا-هدف بصری/schema JSON و [CONTINUE])، ۳+۳+۲ تست برای
  Rule های ۱/۲/۳ (پاس و شکست هرکدام).
- `ChunkCombinerTest.kt`: ۳ تست `isPartialResponse`، ۳ تست `smartCombineChunks` (یک
  تکه، چند تکه، حذف صحیح [CONTINUE])، ۳ تست `validateChunksComplete` (ناقص/کامل/لیست
  خالی).

جمعاً ۲۱ تست جدید (۴۳۶ → ۴۵۷).

## Consequences

- **آسان می‌شود:** پکیج `domain/storybreakdown/` پایه‌گذاری شد؛ قدم‌های بعدی (ب/ت/ث)
  می‌توانند مستقیماً از `StoryBreakdownRequest`/`buildStoryBreakdownPrompt`/
  `smartCombineChunks` استفاده کنند.
- **بدون بدهی جدید شناخته‌شده** از این قدم — بخش‌های ب/ت/ث آگاهانه و صریحاً به قدم‌های
  بعدی موکول شدند، نه یک وضعیت میانی ناقص.
