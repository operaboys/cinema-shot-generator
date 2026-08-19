# ADR-122: سیستم Preview دوزبانه‌ی پرامپت — قدم ۲ از ۳ زیرقدم: descriptionFaPreview روی Asset/Shot + اتصال StoryToDomainMapper به پاسخ دوزبانه‌ی AI

## زمینه

این ADR دومین زیرقدم از برنامه‌ی سه‌قدمی «سیستم Preview دوزبانه‌ی
پرامپت» است (ADR-121 → این ADR → قدم ۳):

- **ADR-121 (قدم ۱)**: Migration واقعی اول این پروژه، `previewLanguageEnabled`
  روی `StoryBreakdownSession`، و `STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION_BILINGUAL`
  در `PromptBuilder.kt` که به AI بیرونی دستور می‌دهد `descriptionEn`/
  `descriptionFa` مجزا برگرداند.
- **این قدم (قدم ۲)**: سمت Kotlin — مدل داده (`descriptionFaPreview`
  روی `CharacterAsset`/`LocationAsset`/`ObjectAsset`/`Shot`) و
  `StoryToDomainMapper.kt` که واقعاً این پاسخ دوزبانه را Parse و ذخیره
  می‌کند.
- **قدم ۳ (بعدی)**: نمایش خودِ Preview فارسی در UI (فرم‌های Composer) و
  اتصال Toggle واقعی به این زنجیره.

## بررسی مستقل — نتیجه: بدون هیچ انحراف واقعی از پیش‌بریفینگ

پیش از نوشتن کد، طبق الزام صریح دستور کار، این فایل‌ها به‌طور کامل
خوانده شدند: `AssetModels.kt` کامل (تأیید `CharacterAsset` خط ۱۸۰،
`LocationAsset` خط ۲۲۸، `ObjectAsset` خط ۲۵۰ — دقیقاً همان
شماره‌خط‌های پیش‌بریفینگ)، `ShotModels.kt` (`Shot.shotDescription`
خط ۹۴)، `AssetDto.kt`/`ShotDto.kt` کامل (تأیید هر دو `@Serializable`
ساده‌اند، بدون ستون Room جداگانه)، `DnaAssetMappers.kt`/`DtoMappers.kt`
کامل (تمام جفت‌های `toDomain()`/`toDto()`)، `StoryToDomainMapper.kt`
کامل، و `PromptBuilder.kt` نسخه‌ی بعد از ADR-121 (خصوصاً
`STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION_BILINGUAL`). **بدون هیچ
انحراف واقعی** — تمام جزئیات پیش‌بررسی‌شده (ساختار JSON خام، عدم وجود
ستون Room، نام‌های تابع/فایل) با کد واقعی مطابقت داشتند.

نکته‌ی تکمیلی مستقل: `validateRequiredKeysPresent` (Rule 8،
`JsonDoctor.kt`) فقط چهار کلید سطح‌بالا (`characters`/`locations`/
`objects`/`shots`) را بررسی می‌کند، نه شکل داخلی هر آیتم — پس این تابع
برای هر دو حالت تک/دوزبانه بدون تغییر کار می‌کند؛ نیازی به آگاهی از
`previewLanguageEnabled` نداشت.

## تصمیم فنی — چگونگی تفکیک description تکی/دوگانه در processAiResponse

**تصمیم**: `SimpleCharacterFromAi`/`SimpleLocationFromAi`/
`SimpleObjectFromAi`/`SimpleShotFromAi` (چهار نوع دامنه‌ی موجود، مصرف‌شده
توسط `mapAiCharacterToAsset` و بقیه) **بدون تغییر ساختاری** ماندند —
فقط یک فیلد تازه (`descriptionFa: String? = null`) گرفتند.
`description` همچنان همان معنای قبلی (انگلیسی، پایه‌ی Asset واقعی) را
دارد.

**مشکل**: شکل خام JSON برای دو حالت واقعاً متفاوت است — تک‌زبانه یک
کلید `description` دارد، دوزبانه دو کلید `descriptionEn`/
`descriptionFa` دارد. `kotlinx.serialization` نمی‌تواند این دو شکل را
مستقیماً با یک data class Decode کند (کلیدهای متفاوت).

**راه‌حل انتخاب‌شده**: چهار data class خصوصی تازه — `BilingualCharacterFromAi`/
`BilingualLocationFromAi`/`BilingualObjectFromAi`/`BilingualShotFromAi`
(و `BilingualAiResponse` بسته‌بندی‌کننده) — که **فقط** مسئول Parse شکل
خام دوزبانه‌اند؛ هرکدام یک تابع `toSimple()` دارد که بلافاصله به همان
`SimpleCharacterFromAi`/... موجود تبدیل می‌شود (`descriptionEn` →
`description`، `descriptionFa` → `descriptionFa`). `processAiResponse`
یک پارامتر تازه گرفت: `previewLanguageEnabled: Boolean = false`؛ بر
اساس آن، یا `SimpleAiResponse` یا `BilingualAiResponse.toSimple()`
Decode می‌شود — از همان نقطه به بعد، **بقیه‌ی تابع (mapAi*ToAsset،
`groupAiShotsIntoScenes`، `mapAiShotToShot`، هشدارهای Rule 9/10)
کاملاً یکسان برای هر دو حالت اجرا می‌شود**، بدون هیچ شاخه‌ی شرطی
اضافه در منطق اصلی.

**چرا این گزینه (نه Sealed Class یا دو تابع Parse کاملاً جدا)**: این
الگو بیشترین کد مشترک را حفظ می‌کند — منطق واقعی نگاشت (Tier/Gender/
Outfit پیش‌فرض/گروه‌بندی صحنه/تطبیق نام) فقط یک‌بار نوشته شده و برای
هر دو حالت اجرا می‌شود؛ تنها تفاوت واقعی (شکل خام JSON) در یک لایه‌ی
نازک Parse/تبدیل ایزوله شده. یک Sealed Class برای چهار نوع Simple*FromAi
باعث می‌شد `mapAiCharacterToAsset` و بقیه هم باید `when` بنویسند —
تکرار منطق در نقطه‌ی اشتباه.

## پیاده‌سازی

**۱. `AssetModels.kt`**: `CharacterAsset`/`LocationAsset`/`ObjectAsset`
هرکدام `val descriptionFaPreview: String? = null` گرفتند (در
`CharacterAsset`، هم‌سطح `basePrompt`؛ در دو مورد دیگر، هم‌سطح
`description`).

**۲. `ShotModels.kt`**: `Shot` فیلد `val shotDescriptionFaPreview: String? = null`
گرفت — نام‌گذاری هم‌الگو با `shotDescription` موجود (پیشوند `shot`)،
طبق دستور صریح دستور کار، نه `descriptionFaPreview` خام مثل
`AssetModels.kt`.

**۳. `AssetDto.kt`/`ShotDto.kt`**: فیلد Nullable معادل به هر چهار DTO —
**بدون Room Migration**، چون `AssetEntity.assetDataJson`/
`ShotEntity.shotDataJson` هر دو JSON خام باقی می‌مانند (تأییدشده در
بررسی مستقل بالا؛ کاملاً برخلاف `StoryBreakdownSessionEntity` قدم ۱
که ستون‌های تفکیک‌شده داشت) — الگوی `negativePromptOverride`/
`cinematicModeOverride` (ADR-028/ADR-106) دنبال شد.

**۴. `DnaAssetMappers.kt`/`DtoMappers.kt`**: هر چهار جفت
`toDomain()`/`toDto()` فیلد جدید را map کردند.

**۵. `StoryToDomainMapper.kt`**: طبق تصمیم فنی بالا — چهار
`Simple*FromAi` فیلد `descriptionFa` گرفتند؛ چهار `Bilingual*FromAi`
خصوصی + `toSimple()` اضافه شدند؛ `mapAiCharacterToAsset`/
`mapAiLocationToAsset`/`mapAiObjectToAsset`/`mapAiShotToShot` مقدار
`descriptionFa` را در `descriptionFaPreview`/`shotDescriptionFaPreview`
می‌ریزند (بدون تغییر `basePrompt`/`description`/`shotDescription` —
همیشه انگلیسی)؛ `processAiResponse` پارامتر
`previewLanguageEnabled: Boolean = false` گرفت.

## Scope — انحراف مستندشده و توجیه‌شده

طبق متن صریح دستور کار، این قدم **فقط** شش فایل زیر را تغییر داد:
`AssetModels.kt`، `ShotModels.kt`، `AssetDto.kt`، `ShotDto.kt`،
`DnaAssetMappers.kt`، `DtoMappers.kt`، `StoryToDomainMapper.kt`.
**`AiStoryBreakdownViewModel.kt` عمداً دست‌نخورده ماند** — یعنی
`processAiResponse` هنوز همه‌جا با مقدار پیش‌فرض
`previewLanguageEnabled=false` صدا زده می‌شود؛ StateFlow واقعی
`previewLanguageEnabled` (ساخته‌شده در قدم ۱) هنوز به این فراخوان‌ها
وصل نیست. این یک انحراف نیست، بلکه دقیقاً تفسیر محافظه‌کارانه‌ی مرز
صریح دستور کار («این قدم فقط ... را تغییر می‌دهد ... هیچ تغییری در
UI») است — اتصال واقعی Toggle به این زنجیره، مثل نمایش خودِ Preview
فارسی، به قدم ۳ سپرده شده.

## تست

**`StoryToDomainMapperTest.kt` (۹ تست تازه)**: سه تست carry-through
مستقیم (`mapAiCharacterToAsset`/`mapAiLocationToAsset`/
`mapAiObjectToAsset` با `descriptionFa` پر و `mapAiShotToShot` مشابه)
که اثبات می‌کنند `basePrompt`/`description`/`shotDescription` همچنان
انگلیسی می‌مانند و `descriptionFaPreview`/`shotDescriptionFaPreview`
دقیقاً مقدار فارسی را می‌گیرند؛ یک تست `null` صریح (بدون `descriptionFa`)؛
یک تست end-to-end با JSON دوزبانه‌ی واقعی (هر چهار نوع، حداقل یک آیتم
از هرکدام، `previewLanguageEnabled=true`) که کل زنجیره‌ی
`processAiResponse` را اثبات می‌کند؛ یک تست end-to-end صریح رگرسیون
(`previewLanguageEnabled=false` روی JSON تک‌زبانه‌ی موجود، تأیید
`descriptionFaPreview`/`shotDescriptionFaPreview` همه `null` می‌مانند).
**۵ تست end-to-end موجود دیگر (`processAiResponse succeeds...`،
`recovers from...`، `NeedsManualRepair`، `MissingRequiredKeys`)
بدون هیچ تغییری سبز ماندند** — چون این تست‌ها همان امضای قدیمی
`processAiResponse(chunks, targetShotCount)` را صدا می‌زنند و
`previewLanguageEnabled` پیش‌فرض `false` دارد؛ این خودش اثبات رگرسیون
صفر است (اگر پیش‌فرض اشتباه بود، همین تست‌ها روی JSON تک‌زبانه‌شان با
خطای Decode شکست می‌خوردند).

**`DtoMappersTest.kt` (۱ تست تازه)**: هم‌الگوی دقیق تست‌های
`cinematicModeOverride` موجود — `shotDescriptionFaPreview` هم وقتی
مقداردهی شده و هم وقتی `null` است، از `Shot.toDto().toDomain()` دقیقاً
بازمی‌گردد.

**`AssetRepositoryTest.kt` (۳ تست تازه)**: Round-trip واقعی
end-to-end (نه فقط Mapper، بلکه از طریق Room واقعی/`assetDataJson`)
برای هر سه نوع Asset با `descriptionFaPreview` پر — حالت `null` از قبل
توسط سه تست موجود (`fullCharacter`/`fullLocation`/`fullObject`، بدون
مقداردهی این فیلد) ضمنی اثبات می‌شد؛ اگر Mapper مقدار `null` را درست
حفظ نمی‌کرد، همان تست‌های موجود شکست می‌خوردند.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (فایل‌های تغییریافته/تازه، مجزا) | موفق — `StoryToDomainMapperTest`(۲۹)، `DtoMappersTest`(۸)، `AssetRepositoryTest`(۱۴)، `AiStoryBreakdownViewModelTest`(۲۴)، `PromptGenerationRepositoryTest`(۳) |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۹۲۷ تست، ۱ شکست نامرتبط (`OutputDeliveryFlowTest`) — در اجرای مجزا موفق؛ همان کلاس Flake محیطی مستندشده از ADR-044 تا کنون |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این قدم (عمداً)

- `PromptAssembly.kt`/`Renderer.kt` دست‌نخورده ماندند — همیشه فقط
  `basePrompt`/`description`/`shotDescription` انگلیسی را می‌خوانند؛
  فیلد فارسی تازه هرگز به این دو فایل راه پیدا نکرد (تأییدشده با
  `git status` پیش از commit).
- UI فرم‌های Character/Location/Object/Shot Composer — قدم ۳.
- اتصال واقعی `previewLanguageEnabled` (StateFlow قدم ۱) به فراخوان‌های
  `processAiResponse` در `AiStoryBreakdownViewModel.kt` — قدم ۳.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
