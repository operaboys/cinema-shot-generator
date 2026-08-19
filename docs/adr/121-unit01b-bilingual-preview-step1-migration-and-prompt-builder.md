# ADR-121: سیستم Preview دوزبانه‌ی پرامپت — قدم ۱ از ۳ زیرقدم: Migration دیتابیس (نسخه ۱→۲) + مدل داده + Prompt Builder دوزبانه + Toggle UI در AI Story Breakdown

## زمینه

این ADR اولین زیرقدم از یک برنامه‌ی سه‌قدمی تازه («سیستم Preview دوزبانه‌ی
پرامپت») است — قدم‌های بعدی (اتصال Toggle به Mapper واقعی/StoryToDomainMapper،
و نمایش خودِ Preview فارسی به کاربر در فاز بازبینی نهایی) در قدم‌های بعدی این
برنامه می‌آیند.

هدف: افزودن یک Toggle به AI Story Breakdown («تحلیل داستان با AI») که به
کاربر فارسی‌زبان اجازه می‌دهد از AI بیرونی بخواهد علاوه بر توضیح انگلیسی
(همیشه پایه‌ی پرامپت نهایی)، یک نسخه‌ی فارسی هم فقط برای مرور خودش بفرستد.

**تصمیم محتوایی صریح معمار/کاربر پروژه (نه Claude Code)**: این کادر فقط دو
گزینه دارد — «فقط انگلیسی» یا «انگلیسی + فارسی» — هرگز «فقط فارسی»، چون
پرامپت نهایی این اپ (`PromptAssembly.kt`/`Renderer.kt`، در این قدم
دست‌نخورده) همیشه و بدون استثنا انگلیسی است. زبان فارسی صرفاً یک Preview
دائمی برای خودِ کاربر است.

## یافته‌ی حیاتی — اولین Migration واقعی این پروژه

برخلاف `AssetEntity`/`ShotEntity`/`ProjectDnaEntity` (که JSON خام در یک ستون
Blob ذخیره می‌کنند)، `StoryBreakdownSessionEntity` ستون‌های تفکیک‌شده‌ی واقعی
دارد. با `grep` مستقل روی کل کدبیس تأیید شد: **صفر** مورد
`androidx.room.migration.Migration` یا `fallbackToDestructiveMigration` در
هیچ‌جای پروژه وجود ندارد؛ تمام ارجاعات موجود به کلمه‌ی «Migration» صرفاً
اصطلاح مفهومی کامنت‌ها برای «تغییر مدل داده» است (بدون ربط به Room). همچنین
تأیید شد `AppDatabase.kt` تا این قدم `version = 1` بود.

**نتیجه**: افزودن یک ستون تازه به `StoryBreakdownSessionEntity` بدون افزایش
`version` و بدون یک `Migration` رسمی، اپ را برای هر کاربری که از قبل
پروژه‌ی ذخیره‌شده دارد Crash می‌کند — پیش‌فرض سخت‌گیرانه‌ی Room وقتی هیچ
مسیر Migration/Fallback تعریف نشده باشد.

**تصمیم فنی (توجیه‌شده، طبق دستور صریح این قدم)**: `version` از ۱ به ۲
افزایش یافت، همراه با یک `Migration(1, 2)` واقعی (`MIGRATION_1_2` در
`AppDatabase.kt`) که ستون `previewLanguageEnabled` را با
`ALTER TABLE story_breakdown_session ADD COLUMN previewLanguageEnabled INTEGER NOT NULL DEFAULT 0`
اضافه می‌کند — یعنی داده‌ی موجود کاربر کاملاً حفظ می‌شود و رفتار پیش‌فرض
برای کاربران موجود همان «فقط انگلیسی» فعلی می‌ماند، بدون تغییر ناگهانی.
`fallbackToDestructiveMigration` عمداً استفاده نشد — داده‌ی کاربر را پاک
می‌کند، غیرقابل‌قبول برای اپی که محتوای واقعی تولید می‌کند.

چون این اولین Migration واقعی این پروژه است، `MIGRATION_1_2` باید مرجع
دقیقی برای Migration های بعدی باشد — کامنت‌های آن در `AppDatabase.kt` این
استدلال کامل را مستند می‌کنند.

## بررسی مستقل — نتیجه: بدون هیچ انحراف واقعی از پیش‌بریفینگ

پیش از نوشتن کد، طبق الزام صریح دستور کار، این فایل‌ها به‌طور کامل خوانده
شدند: `AppDatabase.kt` (تأیید `version=1`، بدون Migration)،
`StoryBreakdownSessionEntity.kt`، `StoryRepository.kt`
(`StoryBreakdownSession`/`saveBreakdownSession`/`loadBreakdownSession`)،
`PromptBuilder.kt` (`StoryBreakdownRequest`،
`STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION`، `buildStoryBreakdownPrompt`)،
`AiStoryBreakdownViewModel.kt` کامل، `AiStoryBreakdownScreen.kt` (بخش
`Phase1WriteStory`). با `grep` مستقل تأیید شد `saveBreakdownSession`/
`loadBreakdownSession` فقط در `AiStoryBreakdownViewModel.kt` و خودِ
`StoryRepository.kt` فراخوانی می‌شوند — پس تغییر امضای این دو تابع هیچ
فراخوان دیگری را نمی‌شکند. **بدون هیچ انحراف واقعی از پیش‌بریفینگ.**

## پیاده‌سازی

**۱. `AppDatabase.kt`**: `version = 2`؛ `MIGRATION_1_2` (بالا)؛
`Room.databaseBuilder(...).addMigrations(MIGRATION_1_2).build()`.

**۲. `StoryBreakdownSessionEntity.kt`**:
`val previewLanguageEnabled: Boolean = false` اضافه شد.

**۳. `StoryRepository.kt`**: `StoryBreakdownSession` (data class دامنه) فیلد
`previewLanguageEnabled: Boolean = false` گرفت؛ `saveBreakdownSession` و
`loadBreakdownSession` هر دو با آن هماهنگ شدند (پارامتر با مقدار پیش‌فرض —
بدون شکستن هیچ فراخوان موجود).

**۴. `PromptBuilder.kt`**:
- `StoryBreakdownRequest` فیلد `previewLanguageEnabled: Boolean = false`
  گرفت.
- `STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION` موجود **بدون تغییر** باقی ماند
  (برای `previewLanguageEnabled=false`، رفتار قبلی دقیق).
- ثابت تازه‌ی `STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION_BILINGUAL` اضافه شد
  (برای `previewLanguageEnabled=true`): هر `description` قبلی به دو فیلد
  مستقل تبدیل شده — `descriptionEn` (الزامی، همیشه انگلیسی، پایه‌ی پرامپت
  نهایی) و `descriptionFa` (الزامی در این حالت، فارسی، فقط Preview)، برای
  هر چهار نوع (characters/locations/objects/shots). دستور صریح در متن
  تأکید می‌کند این دو فیلد باید کاملاً مستقل و ترجمه‌ی دقیق یکدیگر باشند،
  هرگز در یک رشته قاطی/ترکیب نشوند.
- `buildStoryBreakdownPrompt` بر اساس `request.previewLanguageEnabled`
  schema درست را انتخاب می‌کند؛ در حالت دوزبانه، یک جمله‌ی توضیحی هم اضافه
  می‌شود که به AI می‌گوید چرا این دو فیلد لازم است (کاربر فارسی‌زبان
  می‌خواهد محتوا را پیش از استفاده مرور کند).

**۵. `AiStoryBreakdownViewModel.kt`**: `_previewLanguageEnabled`
(`MutableStateFlow<Boolean>(false)`)، `previewLanguageEnabled: StateFlow<Boolean>`
— هم‌الگوی دقیق `targetShotCount`/`defaultShotDurationSeconds`.
`setPreviewLanguageEnabled(enabled)` مقدار را فوری تغییر می‌دهد و
`saveSession()` را صدا می‌زند (Auto-Save بی‌صدا، هم‌الگوی موجود). `init{}`
مقدار را از `loadBreakdownSession` می‌خواند (هم‌الگوی بقیه‌ی فیلدهای همان
تابع). `generatePrompt()` اکنون `previewLanguageEnabled` را هم به
`StoryBreakdownRequest` می‌دهد — بدون این اتصال، خودِ Toggle هیچ اثری روی
پرامپت واقعی نداشت.

**۶. `AiStoryBreakdownScreen.kt`**: یک `Row` تازه (`Text` + `Switch`،
هم‌الگوی دقیق `SettingsSwitchRow` در `SettingsScreen.kt`) بلافاصله بعد از
`FloatStepperField` مدت‌زمان‌هرشات و پیش از دکمه‌ی «تولید پرامپت» —
منطقی‌ترین جا، چون این آخرین تنظیم فرم ورودی پیش از تولید پرامپت است.
testTag تازه: `AI_BREAKDOWN_PREVIEW_LANGUAGE_TOGGLE_TAG`.

**۷. `UiStrings.kt`**: کلید تازه `aiBreakdown.previewLanguageToggleLabel`
در **هر دو** نقشه (فارسی/انگلیسی) هم‌زمان اضافه شد — متن صریحاً می‌گوید
این فقط یک Preview است، نه تغییر زبان پرامپت نهایی («همراه با Preview
فارسی (فقط برای مرور شما — پرامپت نهایی همیشه انگلیسی می‌ماند)»).

## زیرساخت تست — MigrationTestHelper زیر Robolectric

**یافته‌ی واقعی دیباگ (مهم برای Migration های بعدی)**: `MigrationTestHelper`
(از `androidx.room.testing`) با `isIncludeAndroidResources=true` فقط
`android_merged_assets` را می‌خواند — که خروجی `mergeDebugAssets` (assets
واقعی نسخه‌ی **debug**) است، نه `sourceSets.test.assets`. تلاش اول (افزودن
Schema ها به `sourceSets.getByName("test").assets.srcDirs`) با
`FileNotFoundException` شکست خورد؛ راه‌حل نهایی افزودن آن‌ها به
`sourceSets.getByName("debug").assets.srcDirs("$projectDir/schemas")` در
`app/build.gradle.kts` بود — چون `release` این Source Set را ندارد، این
فایل‌های JSON هرگز در بیلد واقعی release باندل نمی‌شوند.

## تست

**`AppDatabaseMigrationTest.kt` (تازه، ۱ تست)**: با `MigrationTestHelper`،
یک دیتابیس نسخه‌ی ۱ با یک `Project` و یک `story_breakdown_session` واقعی
ساخته می‌شود (شامل `freeformStory`/`targetShotCount`/
`defaultShotDurationSeconds`)؛ بعد از `runMigrationsAndValidate(..., 2, true, MIGRATION_1_2)`،
همان ردیف با همان مقادیر قبلی باقی می‌ماند و `previewLanguageEnabled` آن
دقیقاً `0` (false) است.

**`PromptBuilderTest.kt` (تازه، ۴ تست)**: `STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION`
(بدون تغییر) فقط یک `description` دارد، هرگز `descriptionEn`/`descriptionFa`؛
`STORY_BREAKDOWN_JSON_SCHEMA_INSTRUCTION_BILINGUAL` دقیقاً ۴ بار
`descriptionEn` و ۴ بار `descriptionFa` دارد (هر چهار نوع آیتم) و
`[CONTINUE]` را حفظ کرده؛ دستور صریح عدم‌ترکیب دو زبان در متن هست؛
`buildStoryBreakdownPrompt` با `previewLanguageEnabled=false/true` schema
درست را انتخاب می‌کند و در حالت `true` جمله‌ی توضیحی «فارسی‌زبان» را هم
دارد.

**`AiStoryBreakdownViewModelTest.kt` (تازه، ۲ تست)**:
۱) `previewLanguageEnabled` پیش‌فرض `false` است و `setPreviewLanguageEnabled(true)`
فوری StateFlow را تغییر می‌دهد.
۲) اثبات ذخیره/بارگذاری واقعی: یک `Project` واقعی ساخته می‌شود (یافته‌ی
دیباگ: `story_breakdown_session` یک ForeignKey واقعی به `projects` دارد —
بدون آن، `saveBreakdownSession` بی‌صدا با `SQLiteConstraintException`
شکست می‌خورد، چون توسط `runCatching` بلعیده می‌شود — دقیقاً همان علت
مستندشده‌ی مشابه در `AppDatabaseDaoTest.kt`/`OutputDeliveryViewModelTest.kt`)؛
یک `AiStoryBreakdownViewModel` با `ioScopeOverride=Dispatchers.Unconfined`
ساخته می‌شود، `setPreviewLanguageEnabled(true)` صدا زده می‌شود؛ یک انتظار
محدود (Polling) اثبات می‌کند مقدار واقعاً در Room نشسته (چون Room حتی زیر
Unconfined یک نقطه‌ی تعلیق واقعی به Executor خودش دارد)؛ سپس یک نمونه‌ی
تازه‌ی ViewModel برای همان پروژه ساخته می‌شود و `previewLanguageEnabled`
آن (با `awaitCondition`) دوباره `true` بارگذاری می‌شود.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (فایل‌های تازه/تغییریافته، مجزا) | موفق — `AppDatabaseMigrationTest`(۱)، `PromptBuilderTest`(+۴)، `AiStoryBreakdownViewModelTest`(+۲)، `UiStringsTest`(هر دو نقشه هماهنگ) |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۹۱۶ تست، ۱ شکست نامرتبط (`OutputDeliveryFlowTest`) — در اجرای مجزا موفق؛ همان کلاس Flake محیطی مستندشده از ADR-044 تا کنون |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این قدم (عمداً)

- `StoryToDomainMapper.kt` (`Simple*FromAi`/Mapper ها) و
  `CharacterAsset`/`LocationAsset`/`ObjectAsset`/`Shot` دست‌نخورده ماندند
  — طبق دستور صریح این قدم، آن قدم ۲ این برنامه است.
- **محدودیت شناخته‌شده و آگاهانه (تا قدم ۲)**: اگر کاربر Toggle را روشن
  کند و پاسخ واقعی AI (با `descriptionEn`/`descriptionFa`) را در فاز ۲
  بچسباند، `StoryToDomainMapper` فعلی همچنان فقط دنبال کلید `description`
  می‌گردد — یعنی `MissingRequiredKeys`/رفتار Parse ناقص ممکن است دیده
  شود. این محدودیت گذرا، آگاهانه، و طبق برنامه‌ی سه‌قدمی صریح خودِ کاربر
  است — نه یک باگ ناخواسته؛ قدم ۲ این برنامه دقیقاً این را می‌بندد.
- نمایش خودِ Preview فارسی به کاربر (فاز بازبینی نهایی) — قدم ۳.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
