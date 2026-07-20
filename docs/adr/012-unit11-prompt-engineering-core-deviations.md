# ADR-012: تصمیمات پیاده‌سازی واحد ۱۱ (Prompt Engineering Core) — لایه‌ی دامنه

**تاریخ:** 2026-07-20
**وضعیت:** تمام تصمیمات مستقل + دو تصمیم تأییدشده توسط معمار (طبق دستور کار: واحد ۱۱ به همه‌ی ۱۰ واحد دیگر وابسته است، پس هر تصمیمی که با یک نوع واقعی برخورد کند باید دقیق مستند شود)

## Context

واحد ۱۱ («قلب معماری کل سیستم») تنها واحدی است که تا این نقطه به تمام ده واحد پیاده‌سازی‌شده‌ی قبلی هم‌زمان وابسته است: `ProjectDna` (۰۲)، `Scene` (۰۴)، `Shot`/`ImageReference` (۰۵)، `CharacterAsset`/`LocationAsset`/`selectOutfitForScene` (۰۶)، `CameraSettings` (۰۹)، enum‌های Lighting/Environment (۰۸)، `AudioContext` (۱۰)، `ValidationIssue`/`Severity` (۰۷). دستور کار صریحاً منع کرد که هر نوع Placeholder ساخته شود؛ هر ناهم‌خوانی باید یا با نگاشت فیلد واقعی حل شود یا در صورت ناسازگاری ساختاری جدی، از معمار پرسیده شود.

## تصمیمات محلی (بدون نیاز به پرسش — نگاشت مستقیم به نوع واقعی)

### `LightingSettings`/`EnvironmentSettings`: مجموعه‌ی محلی حداقلی، نه Placeholder
هیچ نوع تجمیعی `LightingSettings`/`EnvironmentSettings` در `domain.sceneconditions` (واحد ۰۸) وجود ندارد — فقط enum‌های مستقل (`LightingStyle`, `KeyLightPosition`, `ContrastRatio`, `WeatherType`, …) و `LightingPreset` رشته‌ای. کد مفهومی بلوپرینت ۱۱ به `input.lighting.style`/`.keyLightPosition`/`.contrastRatio` و `input.environment.weatherType` نیاز دارد. **تصمیم:** دو `data class` محلی در `PromptEngineModels.kt` تعریف شد که فقط از enum‌های واقعیِ واحد ۰۸ تشکیل شده‌اند — نه اختراع فیلد جدید، فقط تجمیع فیلدهای واقعی‌ای که بلوپرینت به آن‌ها نیاز دارد. این دقیقاً گزینه‌ی «(الف) از فیلد/متد واقعی نوع موجود استفاده کن» است، صرفاً در سطح یک Wrapper تجمیعی چون خودِ Aggregate در جای دیگر تعریف نشده.

### `objects`/`locations`: هر دو `List<LocationAsset>`
هیچ نوع عمومی `Asset` در پروژه وجود ندارد. طبق تصمیم پیشین ADR-003 (واحد ۰۶)، `LocationAsset` هر دو `AssetType.LOCATION` و `AssetType.OBJECT` را پوشش می‌دهد. **تصمیم:** فیلدهای `objects` و `locations` در `PromptGenerationInput` هر دو از نوع `List<LocationAsset>` تعریف شدند — بازاستفاده از یک تصمیم معماری موجود، نه اختراع نوع جدید.

### `describePhysicalAppearance`: تابع محلی جایگزین `.describe()`
طبق پیش‌بینیِ خود دستور کار، `PhysicalAppearance` (واحد ۰۶) متد `.describe()` ندارد (تأیید شده با grep). یک تابع سطح-پکیج `describePhysicalAppearance(appearance: PhysicalAppearance): String` در `CharacterContinuity.kt` نوشته شد که فیلدهای واقعی (`ageRange`, `gender`, `build`, `hair.color`, `hair.style`, `facialFeatures.eyes`) را ترکیب می‌کند.

## تصمیمات تأییدشده توسط معمار (پرسیده شد — ناسازگاری ساختاری واقعی)

هنگام پیاده‌سازی `enforceCharacterContinuity`، دو فیلدی که کد مفهومی بلوپرینت فرض کرده بود در هیچ‌جای پروژه وجود نداشتند:
- `scene.weather` — `Scene` (واحد ۰۴) اصلاً چنین فیلدی ندارد (تأیید شده با grep در `SceneModels.kt`).
- `character.outfitOverride` — `CharacterAsset` (واحد ۰۶) چنین فیلدی ندارد؛ هیچ نوع دیگری هم «override دستی لباس در سطح شات» را مدل نمی‌کند.

این‌ها با پرسش از معمار حل شدند (نه تصمیم محلی):

1. **`sceneWeather` به‌عنوان پارامتر خارجی جدید** به `enforceCharacterContinuity(characters, scene, sceneWeather: String?)` اضافه شد. فراخوان (`assemblePromptBlueprint`) مقدار را از `input.environment.weatherType.name.lowercase()` تأمین می‌کند.
2. **`manualOverrideId` همیشه `null`** در فراخوانی `selectOutfitForScene(character.outfits, sceneWeather, manualOverrideId = null)` — چون هیچ منبع داده‌ای برای override دستی در سطح پروژه وجود ندارد.

**تأیید معمار:** «بله، هر دو پیشنهاد را قبول کن.»

پارامتر `scene: Scene` در امضای `enforceCharacterContinuity` برای وفاداری به امضای کد مفهومی بلوپرینت نگه داشته شد، هرچند در بدنه‌ی فعلی استفاده نمی‌شود (هشدار کامپایلر بی‌خطر، نه خطا).

## `assemblePromptBlueprint`: پارامتر چهارم واقعی به‌جای هاردکد ۰/لیست خالی

کد مفهومی بلوپرینت ۱۱ برای `conflictsResolved`/`warnings` مقدار `0`/`emptyList()` (TODO) می‌گذارد. طبق دستور صریح این قدم، این هاردکد جایگزین شد با یک پارامتر واقعی چهارم: `validationIssues: List<ValidationIssue>`. یک تابع کمکی جدید `summarizeConflictResolution(issues: List<ValidationIssue>): ConflictResolutionSummary` (در `ConflictResolution.kt`) فیلترِ `Severity.WARNING` را می‌شمارد و برمی‌گرداند؛ منطق خودِ Rule تشخیص تعارض (که در واحد ۰۷ زندگی می‌کند) تکرار نشد — فقط خروجی آن مصرف می‌شود.

**چرا `List<ValidationIssue>` و نه `ValidationReport`:** ADR-010 قبلاً تأیید کرده بود که هیچ Rule واقعی در کل پروژه از `ValidationReport` استفاده نمی‌کند (همه از `List<ValidationIssue>` یا یک `ValidationIssue?` تکی استفاده می‌کنند). برای هم‌راستایی با این الگوی مستقر، نوع پارامتر `List<ValidationIssue>` انتخاب شد.

**یادداشت مهم:** این یک **بهبود نسبت به کد مفهومی بلوپرینت** است (نه صرفاً پیاده‌سازی وفادار آن) — طبق دستور صریح این قدم اجرا و اینجا مستند شد.

`BLOCKING` issues در این تابع فیلتر می‌شوند و در schema فعلی `PromptBlueprint` (که فیلدی برای «خطاهای مسدودکننده» ندارد) هیچ نمایشی ندارند — سازگار با قرارداد سراسری پروژه که «BLOCKING یعنی جریان اصلاً به این مرحله نمی‌رسد».

## بررسی سه‌گانگی `AmbientSound` (فقط تأیید، طبق دستور صریح رفع نشد)

`assemblePromptBlueprint` برای ساخت `audioDescription` فقط از `ambientSound.type` و `ambientSound.intensity` استفاده می‌کند. هر سه شکل موازی `AmbientSound`/`AmbientSoundSuggestion` (مستندشده در ADR-009/ADR-011) این دو فیلد را با همین نام دارند — بنابراین این سه‌گانگی برای این واحد **بی‌خطر** است. طبق دستور صریح این قدم، فقط تأیید شد؛ یکی‌سازی سه نوع رفع نشد (تصمیمش با معمار است).

## خارج از Scope این قدم

- **`collectData` پیاده نشد.** جمع‌آوری واقعی داده از Room (واحد ۱۵، هنوز وجود ندارد) به یک قدم بعدی نیاز دارد. `PromptGenerationInput` به‌عنوان یک struct آماده‌ی پرشدن باقی ماند؛ هیچ تابعی این ساختار را از منبع داده‌ی واقعی پر نمی‌کند.
- محدودیت طول/توکن (واحد ۱۳) و قالب‌بندی مخصوص پلتفرم (واحد ۱۴) پیاده نشدند — طبق دستور کار.
- Room و UI پیاده نشدند.

## Consequences

- **آسان می‌شود:** واحد ۱۱ اکنون نقطه‌ی تجمیع واقعی همه‌ی واحدهای قبلی است؛ هیچ Placeholder‌ای در مسیر بحرانی وجود ندارد، پس واحدهای بعدی (۱۲–۱۶) می‌توانند مستقیماً روی `PromptGenerationInput`/`PromptBlueprint` واقعی بسازند.
- **بدهی باقی‌مانده:** `collectData` هنوز پیاده نشده (وابسته به واحد ۱۵)؛ سه‌گانگی `AmbientSound` هنوز حل نشده (ADR-009/011)؛ `Shot.camera`/`.lighting`/`.environment` هنوز `SourcedSettings` هستند نه انواع واقعی — لایه‌ی اتصال بین این‌ها و `CameraSettings`/`LightingSettings`/`EnvironmentSettings` واقعی هنوز نوشته نشده (کار یک قدم/واحد آینده، نه این قدم).
