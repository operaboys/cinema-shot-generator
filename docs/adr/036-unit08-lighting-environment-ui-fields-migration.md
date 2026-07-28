# ADR-036: واحد ۰۸ (Scene Conditions) — گسترش LightingSettings/EnvironmentSettings برای Tab «نور و محیط» واحد ۱۶

**تاریخ:** 2026-07-28
**وضعیت:** کامل شد. `gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD
SUCCESSFUL`، ۵۰۸ تست، ۰ Failure، ۰ Error.

## Context

`docs/blueprints/16-user-workflow-v2.md` تأکید دارد Tab های Shot Composer باید
مستقیماً data class های واقعی دامنه را ویرایش کنند، نه یک مدل UI موازی. اما
`LightingSettings`/`EnvironmentSettings` (که در ADR-013 عمداً «تجمیع محلیِ فقط
فیلدهای موردنیاز واحد ۱۱» تعریف شده بودند) فقط ۳/۱ فیلد از ۹/۷ پارامتر کامل بلوپرینت
۰۸ را داشتند — enum های کامل از قبل در `LightingModels.kt`/`EnvironmentModels.kt`
تعریف شده بودند اما روی خودِ این دو `data class` نبودند.

## بررسی تناقض ظاهری بین دستور کار و بلوپرینت ۱۶ (حل‌شده، نه یک تناقض واقعی)

بلوپرینت ۱۶ در توصیف تصویری Tab «نور و محیط» فقط یک زیرمجموعه از فیلدها را به‌صراحت
نام می‌برد (Lighting Style، و در «ردیف نور پیشرفته»: Key Light Position، Fill
Light، Contrast Ratio، Color Temperature؛ برای محیط: Weather، Weather Intensity،
Ground State). این فهرست `shadowQuality`/`lightSourceCount`/`lightingMotivation`/
`windStrength`/`visibility`/`environmentalMotion`/`temperatureFeel` را که دستور کار
خواسته بود مستقیماً نام نمی‌برد.

**نتیجه‌گیری: این یک تناقض واقعی نیست.** بلوپرینت ۰۸ خودش صراحتاً می‌گوید «همه‌ی ۹
پارامتر»/«همه‌ی ۷ پارامتر» را enum کرده (کامنت‌های هدر `LightingModels.kt`/
`EnvironmentModels.kt`)، یعنی مجموعه‌ی کامل از قبل به‌عنوان بخشی از طراحی رسمی واحد
۰۸ پذیرفته شده بود. توصیف بلوپرینت ۱۶ از UI یک Wireframe سطح‌بالا با یک «ردیف
پیشرفته (قابل جمع/باز)» است — نمونه‌ی نمایشی، نه فهرست جامع هر فیلد ورودی ممکن.
دستور کار معمار مستقیماً و با جزئیات دقیق (نام فیلد + enum دقیق) این فیلدها را
خواسته بود، پس بدون توقف ادامه داده شد.

## پیاده‌سازی

### `LightingSettings`/`EnvironmentSettings`

هر دو `data class` دقیقاً طبق دستور کار گسترش یافتند — همه‌ی فیلدهای جدید
`nullable = null` (یا `emptyList()` برای `environmentalMotion`)، پس Backward
Compatible کامل: هر محل ساخت Positional موجود (`LightingSettings(style,
keyLightPosition, contrastRatio)`, `EnvironmentSettings(weatherType)`) بدون تغییر
کامپایل می‌شود چون فیلدهای جدید انتهای لیست پارامترها اضافه شدند.

### تصمیم `PromptAssembly.kt`: عمداً دست‌نخورده ماند

با grep در `docs/blueprints/11-prompt-engineering-core-v2.md` هیچ ارجاعی به
`fillLight`/`colorTemperature`/`shadowQuality`/`lightSourceCount`/
`lightingMotivation`/`weatherIntensity`/`windStrength`/`groundState`/`visibility`/
`temperatureFeel`/`environmentalMotion` پیدا نشد — بلوپرینت ۱۱ فقط
`style`/`keyLightPosition`/`contrastRatio`/`weatherType` را برای ساخت
`lightingSpecs`/`environmentSpecs` می‌شناسد (تأییدشده، خط‌به‌خط `PromptAssembly.kt`
بدون تغییر ماند). طبق دستور کار («اگر بلوپرینت ۱۱ چیزی نمی‌گوید، دست نزن»)، این
فیلدها فقط برای مصرف مستقیم UI (واحد ۱۶) نگه داشته شدند. اتصال این فیلدها به متن
پرامپت نهایی (اگر لازم شود) باید یک Migration جداگانه‌ی آینده روی خودِ بلوپرینت ۱۱
باشد، نه حدسی در این قدم.

### یافته‌ی خارج از Blast Radius اعلام‌شده: `DtoMappers.kt`/`SceneConditionsDto.kt`

پیش‌بررسی معمار Blast Radius را «فقط ۳ فایل» اعلام کرده بود (`PromptEngineModels.kt`،
`ShotSettingsResolution.kt`، `PromptAssembly.kt`) — اما با grep من، یک چهارمین محل
واقعی پیدا شد که در آن پیش‌بررسی نبود: `data/repository/DtoMappers.kt` (و
`SceneConditionsDto.kt`، تعریف DTO). این فایل مسیر واقعی ذخیره/بارگذاری
`LightingSettings`/`EnvironmentSettings` در Room (از طریق `ShotDto`) است.

**چرا این یک یافته‌ی واقعی و نه صرفاً یک جزئیات قابل‌نادیده‌گرفتن بود:** اگر
`LightingSettingsDto`/`EnvironmentSettingsDto` بدون تغییر می‌ماندند، هر مقداری که
کاربر در Tab «نور و محیط» واحد ۱۶ برای این فیلدهای جدید انتخاب می‌کرد (مثلاً
`FillLight.STRONG`)، در همان لحظه‌ی ذخیره‌ی شات در Room **بی‌صدا گم می‌شد** — چون
`LightingSettings.toDto()`/`LightingSettingsDto.toDomain()` فقط ۳ فیلد اصلی را
Serialize می‌کردند. این دقیقاً برخلاف هدف اصلی این Migration («Tab باید مستقیماً
data class واقعی را ویرایش کند») بود؛ رفع‌نشدنش یک باگ خاموش واقعی در آینده (واحد
۱۶) ایجاد می‌کرد، نه فقط یک بدهی مستندسازی.

**تصمیم:** `LightingSettingsDto`/`EnvironmentSettingsDto` با همان الگوی nullable
گسترش یافتند (enum ها به‌صورت `String?` سریالایز می‌شوند، `environmentalMotion` به
`List<String>`)؛ `DtoMappers.kt` هر دو جهت (`toDomain`/`toDto`) را کامل کرد. یک تست
Round-Trip جدید (`DtoMappersTest.kt`) اضافه شد تا این تضمین صریحاً قفل شود.

## تست

- `LightingModelsTest.kt`/`EnvironmentModelsTest.kt` (هرکدام ۲ تست: پیش‌فرض
  null/خالی، مقدار صریح برای هر فیلد جدید).
- `DtoMappersTest.kt` (۲ تست round-trip کامل `toDto().toDomain()` — یکی با همه‌ی
  فیلدهای جدید پر، یکی بدون هیچ‌کدام — برای هر دو نوع).

جمعاً ۶ تست جدید (۵۰۲ → ۵۰۸). هیچ تست موجودی نشکست (طبق انتظار Backward
Compatibility کامل).

## Consequences

- **آسان می‌شود:** Tab «نور و محیط» واحد ۱۶ اکنون می‌تواند مستقیماً همه‌ی ۱۶ فیلد
  (۵+۶ جدید + ۳+۱ قدیمی) `LightingSettings`/`EnvironmentSettings` واقعی را بخواند/
  بنویسد، با تضمین ذخیره‌سازی صحیح در Room.
- **کار آینده‌ی شناخته‌شده (نه بدهی پنهان):** اگر بلوپرینت ۱۱ در آینده تصمیم بگیرد
  این فیلدهای جدید هم باید در `lightingSpecs`/`environmentSpecs` لحاظ شوند، آن یک
  Migration جداگانه‌ی صریح روی خودِ بلوپرینت ۱۱ خواهد بود.
