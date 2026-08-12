# ممیزی پیش از واحد ۱۶ (User Workflow / UI)

**تاریخ:** 2026-07-28
**وضعیت (به‌روزرسانی 2026-08-13):** هر ۱۲ یافته‌ی این ممیزی رفع شده‌اند — جدول و
جزئیات کامل در بخش «۵. وضعیت نهایی رفع یافته‌ها» در انتهای این سند. متن اصلی زیر
بدون تغییر باقی مانده (سند تاریخی معتبر از وضعیت زمان نگارش)؛ فقط بخش ۵ جدید است.
**نقش:** این فایل صرفاً یک گزارش است — هیچ کد یا بلوپرینتی در این قدم تغییر نکرد.
**محدوده:** مطابقت `docs/blueprints/16-user-workflow-v2.md` و بلوپرینت‌های ارجاع‌داده‌شده
(۰۱، ۰۱ب، ۰۲، ۰۳، ۰۴، ۰۵، ۰۶، ۰۷، ۰۸، ۰۹، ۱۰، ۱۱، ۱۲، ۱۳، ۱۴، ۱۵) با کد واقعی
`domain/`، `data/repository/`، `data/entity/`، و `docs/reference/type-registry.md`.

**روش:** خواندن کامل بلوپرینت ۱۶؛ grep/Read مستقیم روی کد واقعی برای هر ادعای بلوپرینت
(انواع enum، فیلد data class، امضای تابع)؛ بررسی DTO/Entity برای مسیر ذخیره‌سازی؛ grep
سراسری برای TODO/FIXME و ارجاع‌های بدون `-v2`؛ مقایسه‌ی نقطه‌ای (Spot-check) چند مورد
از جدول Rule های بلوپرینت‌های ۰۵/۰۹/۱۰ با کد Validation واقعی.

**صداقت درباره‌ی عمق پوشش:** بخش‌های ۱، ۲، ۶ به‌طور کامل بررسی شدند (هر ادعای بلوپرینت
۱۶ درباره‌ی enum/فیلد/تابع با کد واقعی مطابقت داده شد). بخش ۷ (Rule ها) به‌صورت
نمونه‌ای (Spot-check) روی بلوپرینت‌های ۰۵/۰۹/۱۰ بررسی شد، نه خط‌به‌خط روی هر ۱۵+
بلوپرینت — یک ممیزی کاملاً جامع Rule-به-Rule برای هر بلوپرینت، قدمی به‌مراتب
بزرگ‌تر از این ممیزی پیش‌نیاز بود.

---

## ۱. یافته‌های 🔴 مسدودکننده‌ی واحد ۱۶

### F1 — `domain/workflow/` اصلاً وجود ندارد

**فایل:** (غایب — انتظار می‌رفت `domain/workflow/` طبق ارجاع خودِ بلوپرینت ۱۶)
**نقل‌قول از بلوپرینت:** `docs/blueprints/16-user-workflow-v2.md:350`:
> `FeedbackType`/`ShotListViewMode` (این نسخه) باید به `domain/workflow/` (یا مسیر
> مشابه که برای `WorkflowState` قبلاً درنظر گرفته شده — با grep بررسی کن) اضافه شوند.

**تأیید غیاب (grep در کل `app/src`):**
```
grep -rln "WorkflowState\|WorkflowStep\|canJumpToStep\|FeedbackType\|ShotListViewMode\|QualityScore\|evaluatePromptQuality" app/src --include=*.kt
```
→ هیچ نتیجه‌ای. نه پوشه‌ی `domain/workflow/` وجود دارد، نه هیچ‌کدام از این ۶ نوع/تابع
(`WorkflowState`, `WorkflowStep`, `StepStatus`, `canJumpToStep`, `FeedbackType`,
`ShotListViewMode`) در هیچ‌جای پروژه تعریف شده‌اند. `QualityScore`/`evaluatePromptQuality`
(بخش «سنجش کیفیت»، `docs/blueprints/16-user-workflow-v2.md:319-339`) هم به همین ترتیب
کاملاً غایب‌اند.

**توضیح مغایرت:** بلوپرینت ۱۶ صراحتاً این کد مفهومی Kotlin را «پیاده‌سازی مفهومی وضعیت
گردش کار» نامیده و `canJumpToStep`/`WorkflowState.progressPercentage` را به‌عنوان
منطق دامنه‌ی لازم برای Bottom Navigation Bar و منطق «آیا مرحله‌ی بعدی قابل‌دسترسی
است» فرض کرده. بدون این نوع‌ها، هیچ Composable/ViewModel واحد ۱۶ نمی‌تواند به این
منطق متکی باشد — باید یا از صفر نوشته شود (که خودِ بلوپرینت انتظار داشته این کار در
یک قدم Migration کوچک، پیش از UI، انجام شود)، یا به‌صورت Ad-hoc داخل لایه‌ی UI
دوباره‌نویسی شود (که دقیقاً همان مشکلی است که بلوپرینت ۱۶ با «Tab باید مستقیماً
data class واقعی را ویرایش کند» می‌خواست از آن پرهیز کند).

**دسته‌بندی:** 🔴 مسدودکننده — باید قبل از شروع واقعی Composable های واحد ۱۶ (یا
حداقل هم‌زمان با اولین قدم آن) یک Migration کوچک این پکیج را بسازد.

---

### F2 — `Scene.location` هیچ ارجاعی به `LocationAsset` کتابخانه ندارد

**فایل:** `app/src/main/java/com/operaboys/cinemashotgenerator/domain/scene/SceneModels.kt:11,31-43`
**نقل‌قول از کد واقعی:**
```kotlin
data class SceneLocation(val type: LocationType, val description: String)

data class Scene(
    val sceneId: String,
    ...
    val location: SceneLocation,
    ...
)
```
**نقل‌قول از بلوپرینت:** `docs/blueprints/16-user-workflow-v2.md:143`:
> **Location** (Dropdown، فهرست `LocationAsset` های موجود در کتابخانه — نه متن آزاد؛
> اگر مکان موردنظر هنوز در Asset Library ساخته نشده، دکمه‌ی «+ ساخت مکان جدید» کاربر
> را به مرحله‌ی ۳ هدایت می‌کند)

**توضیح مغایرت:** `Scene.location` از نوع `SceneLocation(type: LocationType,
description: String)` است — یک جفت توصیفی آزاد (نوع مکان کلی + توضیح متنی)، **بدون
هیچ فیلد `assetId`/ارجاعی به یک `LocationAsset` واقعی در کتابخانه**. اتصال واقعی
`LocationAsset` به پروژه فقط در سطح **Shot** اتفاق می‌افتد (`Shot.locationIds:
List<String>`، تأییدشده در `domain/shot/ShotModels.kt:97`)، نه در سطح Scene. یعنی
دقیقاً همان چیزی که بلوپرینت ۱۶ برای فرم «تنظیمات Scene» فرض کرده (یک Dropdown که
مستقیماً از کتابخانه‌ی `LocationAsset` می‌خواند) با ساختار فعلی Scene **غیرقابل‌پیاده‌سازی
است**، مگر یکی از این دو مسیر انتخاب شود: (الف) یک فیلد جدید `locationAssetId:
String?` به `Scene`/`SceneLocation` اضافه شود، یا (ب) بلوپرینت ۱۶ اصلاح شود تا این
Dropdown واقعاً یک لیبل توصیفی آزاد (نه ارجاع به Asset) باشد و اتصال واقعی
`LocationAsset` همچنان فقط در سطح Shot بماند. این یک تصمیم معماری واقعی است که باید
قبل از ساخت فرم «تنظیمات Scene» گرفته شود.

**دسته‌بندی:** 🔴 مسدودکننده — مستقیماً مانع پیاده‌سازی وفادار فرم مرحله‌ی ۴ (Scene
Creation) طبق مشخصات فعلی بلوپرینت ۱۶ می‌شود.

---

## ۲. یافته‌های 🟡 غیرمسدودکننده اما باید مستند/برنامه‌ریزی شود

### F3 — `mapMoodToLighting` رشته‌ی خام می‌گیرد و مقادیر `style` آن دیگر با enum واقعی `LightingStyle` مطابقت ندارد

**فایل:** `app/src/main/java/com/operaboys/cinemashotgenerator/domain/sceneconditions/LightingMoodMapping.kt:8-15`
**نقل‌قول:**
```kotlin
fun mapMoodToLighting(mood: String): LightingPreset? = when (mood) {
    "tense" -> LightingPreset("dramatic", "side", "none", "high", "hard_shadows", "cold")
    ...
    "hopeful" -> LightingPreset("cinematic", "side", "soft", "medium", "soft_shadows", "warm")
    ...
```
**نقل‌قول از بلوپرینت:** `docs/blueprints/16-user-workflow-v2.md:174`:
> Lighting Style (Dropdown، `LightingStyle` کامل ... پیش‌فرض از Mood-to-Lighting
> Mapping خودکار پر می‌شود، طبق `mapMoodToLighting`)

**توضیح مغایرت (دو بخش):**
1. امضای تابع `mood: String` است، نه `Mood` enum واقعی (`domain.dna.Mood`، ۲۵ مقدار) —
   یعنی صدا زدنش از یک Scene/DNA واقعی نیازمند تبدیل دستی `Mood.name.lowercase()` است،
   بدون Type Safety.
2. **مهم‌تر:** مقدار `style` برگشتی (`"dramatic"`, `"soft"`, `"noir"`, `"cinematic"`)
   دیگر با هیچ مقدار واقعی enum `LightingStyle` (`domain/dna/ProjectDna.kt:126-153`:
   `DRAMATIC_LIGHT`, `SOFT_LIGHT`, `LOW_KEY`, و **هیچ مقدار `CINEMATIC`ای اصلاً وجود
   ندارد**) مطابقت ندارد — نه حتی با تبدیل حروف بزرگ. این enum در Migration واحد ۰۲
   (`docs/adr/027-unit02-dna-manager-v5-migration.md`) بازنویسی شد و `NOIR→LOW_KEY`،
   `DRAMATIC→DRAMATIC_LIGHT` تغییر نام دادند، اما این تابع Mapping به‌روزرسانی نشد.
   اگر یک لایه‌ی UI ساده‌لوحانه `LightingStyle.valueOf(preset.style.uppercase())`
   صدا بزند، برای هر ۶ مسیر `when` این تابع Exception می‌گیرد. (بقیه‌ی ۵ فیلد
   `LightingPreset` — `keyLightPosition`/`fillLight`/`contrastRatio`/`shadowQuality`/
   `colorTemperature` — با `uppercase()` درست به enum های واقعی نگاشت می‌شوند؛ فقط
   `style` خراب است.)

**زمینه:** این یک یافته‌ی کاملاً تازه نیست — `README.md` از قبل مورد ۱ (پارامتر
String) را به‌عنوان کار باقی‌مانده‌ی ADR-027 مستند کرده بود («واحد ۰۸
(`mapMoodToLighting`، هنوز `String`/`LightingPreset` رشته‌ای است)»). آنچه اینجا تازه
است، جزئیات دقیق مورد ۲ (خودِ مقادیر رشته‌ای برگشتی دیگر معتبر نیستند، نه فقط شکل نوع)
است.

**دسته‌بندی:** 🟡 — مستقیماً واحد ۱۶ را در همان اولین قدم مسدود نمی‌کند (چون فرم
می‌تواند بدون این خودکارسازی هم کار کند)، اما اگر همان‌طور که بلوپرینت خواسته
"پیش‌فرض خودکار" واقعاً وصل شود، بدون اصلاح این تابع کرش می‌کند.

---

### F4 — `getPacingFromEmotion` هم رشته‌ی خام می‌گیرد، نه `Mood` enum

**فایل:** `app/src/main/java/com/operaboys/cinemashotgenerator/domain/visualidentity/CinematicLanguage.kt:43-47`
**نقل‌قول:**
```kotlin
fun getPacingFromEmotion(emotion: String): CinematicMode = when (emotion) {
    "tense", "terrified", "furious" -> CinematicMode.FAST_CUT
    "melancholy", "serene", "contemplative" -> CinematicMode.LONG_TAKE
    else -> CinematicMode.BALANCED
}
```
**نقل‌قول از بلوپرینت:** `docs/blueprints/16-user-workflow-v2.md:178`:
> Cinematic Mode (Dropdown، `CinematicMode` بلوپرینت ۰۳: Long-take/Fast-cut/Balanced؛
> پیش‌فرض از `getPacingFromEmotion` خودکار پر می‌شود)

**توضیح مغایرت:** مثل F3، امضا `String` می‌گیرد نه `Mood`؛ علاوه‌بر‌این، مقادیر رشته‌ای
موردانتظارش (`"tense"`, `"terrified"`, `"furious"`, `"melancholy"`, `"serene"`,
`"contemplative"`) با نام‌های واقعی enum `Mood` (۲۵ مقدار `UPPER_SNAKE_CASE`، مثل
`TENSE`, `MELANCHOLIC`) هم‌خوان نیستند مگر یک نگاشت case-conversion دقیق انجام شود —
و حتی در آن صورت هم مقادیری مثل `"terrified"`/`"serene"`/`"contemplative"` باید با
grep در enum واقعی `Mood` تأیید شوند که وجود دارند یا نه (این ممیزی این تطبیق دقیق
enum-به-enum را انجام نداد؛ فقط عدم Type Safety تابع ثبت شد). **این مورد از قبل در
`README.md` مستند شده بود** (همان خط ADR-027: «واحد ۰۳ (`getPacingFromEmotion`، هنوز
`String` می‌گیرد نه `Mood`)») — اینجا فقط برای تکمیل تصویر واحد ۱۶ تکرار شد.

**دسته‌بندی:** 🟡 — قبلاً مستند شده، اما مستقیماً روی Tab «نور و محیط» اثر دارد.

---

### F5 — `CharacterAsset.outfits` یک لیست ساختاریافته است؛ فرم بلوپرینت ۱۶ آن را «TextField» تکی فرض کرده

**فایل:** `app/src/main/java/com/operaboys/cinemashotgenerator/domain/asset/AssetModels.kt:78-84,180-194`
**نقل‌قول:**
```kotlin
data class Outfit(
    val id: String,
    val name: String,
    val description: String,
    val isDefault: Boolean,
    val condition: OutfitCondition? = null
)
...
data class CharacterAsset(
    ...
    val outfits: List<Outfit>,
    ...
)
```
**نقل‌قول از بلوپرینت:** `docs/blueprints/16-user-workflow-v2.md:115`:
> Default Outfit (`TextField`)

**توضیح مغایرت:** فرم «Add New Asset» فقط یک `TextField` تکی برای «Default Outfit»
فرض کرده، اما `CharacterAsset.outfits` یک `List<Outfit>` با فیلدهای ساختاریافته
(`id`, `name`, `description`, `isDefault`, `condition: OutfitCondition?`) است — و
Rule ۵ واحد ۰۶ (`validateDefaultOutfitExists`) صراحتاً نیازمند حداقل یک آیتم با
`isDefault=true` در همین لیست است. یک `TextField` تکی نمی‌تواند این ساختار را
نمایندگی کند؛ فرم واقعی باید یا حداقل «نام + توضیح» جدا برای یک Outfit پیش‌فرض بگیرد
(و بقیه‌ی Outfit ها را به یک بخش جدا/مرحله‌ی بعدی موکول کند)، یا این فیلد باید از فرم
«Add New Asset» اولیه حذف و به یک بخش «مدیریت Outfit» جدا در ویرایش Asset منتقل شود.

**دسته‌بندی:** 🟡 — یک تصمیم طراحی UI واقعی لازم دارد (دقیقاً مثل الگوی
`defaultOutfitPlaceholder()` که در واحد ۰۱ب Mapper ساخته شد)، اما فرم Asset Library
می‌تواند بدون حل فوری این مورد هم شروع شود (با یک Placeholder مشابه).

---

### F6 — فیلدهای «Time of Day»/«Weather» فرم Location در واقع لیست‌های Compatibility‌اند، نه مقدار تکی؛ `environment`/`keyElements` اصلاً در فرم بلوپرینت ۱۶ نیامده‌اند

**فایل:** `app/src/main/java/com/operaboys/cinemashotgenerator/domain/asset/AssetModels.kt:196-219`
**نقل‌قول:**
```kotlin
data class Environment(
    val type: String,
    val size: String,
    val lightingCondition: String
)

data class LocationAsset(
    val assetId: String,
    val name: String,
    val description: String,
    val environment: Environment,
    val timeCompatibility: List<String> = emptyList(),
    val weatherCompatibility: List<String> = emptyList(),
    val keyElements: List<String> = emptyList(),
    val basePrompt: String? = null,
    val continuityLockLevel: LocationContinuityLevel = LocationContinuityLevel.STYLE
)
```
**نقل‌قول از بلوپرینت:** `docs/blueprints/16-user-workflow-v2.md:117-120`:
> **اگر نوع = Location:**
> - Time of Day (Dropdown: Day/Night/Sunset/Sunrise)
> - Weather (`TextField` یا Dropdown آزاد)
> - Environment Details (`Textarea`)

**توضیح مغایرت:** بلوپرینت ۱۶ «Time of Day» و «Weather» را به‌صورت یک مقدار تکی
(Dropdown/TextField) توصیف کرده، اما فیلدهای واقعی متناظر (`timeCompatibility`,
`weatherCompatibility`) هر دو `List<String>` هستند (یعنی طراحی واقعی «این مکان با
کدام بازه‌های زمانی/آب‌وهوایی سازگار است»، نه یک مقدار واحد). همچنین `environment:
Environment` (زیرساختار `type`/`size`/`lightingCondition`) و `keyElements:
List<String>` اصلاً در فهرست فیلدهای فرم بلوپرینت ۱۶ نیامده‌اند — یعنی یا این فرم
باید گسترش یابد تا این دو فیلد را هم بگیرد، یا مقادیر پیش‌فرض معقول برایشان در نظر
گرفته شود (شبیه الگوی `deriveEnvironmentPlaceholder()` واحد ۰۱ب).

**دسته‌بندی:** 🟡 — فرم Location را می‌توان با تصمیم UI ساده (چندانتخابی به‌جای تکی) و
Placeholder مناسب برای `environment`/`keyElements` پیاده کرد، اما بدون این تصمیم
صریح، پیاده‌سازی وفادار به بلوپرینت ۱۶ ممکن نیست.

---

### F7 — `type-registry.md` برای `LightingSettings` فیلد `lightSourceCount` را ندارد و نام `lightingMotivation` را اشتباه نوشته

**فایل:** `docs/reference/type-registry.md:141`
**نقل‌قول:**
> `LightingSettings` | data class | style: LightingStyle, keyLightPosition,
> fillLight, contrastRatio, shadowQuality, colorTemperature, **motivation** | ...

**کد واقعی:** `app/src/main/java/com/operaboys/cinemashotgenerator/domain/sceneconditions/LightingModels.kt:61-69`:
```kotlin
data class LightingSettings(
    val style: LightingStyle,
    val keyLightPosition: KeyLightPosition,
    val contrastRatio: ContrastRatio,
    val fillLight: FillLight? = null,
    val colorTemperature: ColorTemperature? = null,
    val shadowQuality: ShadowQuality? = null,
    val lightSourceCount: LightSourceCount? = null,
    val lightingMotivation: LightingMotivation? = null
)
```
**توضیح مغایرت:** ردیف `type-registry.md` نه فیلد `lightSourceCount` را دارد (کاملاً
غایب از سند)، نه نام درست فیلد آخر را (`lightingMotivation`، نه `motivation`). این
یافته مستقیماً از Migration اخیر خودم (`docs/adr/036-unit08-lighting-environment-ui-fields-migration.md`)
ناشی می‌شود — آن قدم `type-registry.md` را به‌روزرسانی نکرده بود.

**دسته‌بندی:** 🟡 — اگر Unit 16 برای طراحی فرم Tab «نور و محیط» به `type-registry.md`
به‌جای خودِ کد Kotlin رجوع کند (که بلوپرینت ۱۶ هم صراحتاً این فایل را به‌عنوان یکی از
منابع مرجع type می‌شناسد)، یک فیلد کامل جا می‌ماند.

---

### F8 — `type-registry.md` برای `EnvironmentSettings` یک فیلد ناموجود (`locationType`) را فهرست کرده و `environmentalMotion` را ندارد

**فایل:** `docs/reference/type-registry.md:142`
**نقل‌قول:**
> `EnvironmentSettings` | data class | weatherType, weatherIntensity,
> **locationType**, groundState, visibility, temperatureFeel | ...

**کد واقعی:** `app/src/main/java/com/operaboys/cinemashotgenerator/domain/sceneconditions/EnvironmentModels.kt:37-45`:
```kotlin
data class EnvironmentSettings(
    val weatherType: WeatherType,
    val weatherIntensity: WeatherIntensity? = null,
    val windStrength: WindStrength? = null,
    val groundState: GroundState? = null,
    val visibility: Visibility? = null,
    val temperatureFeel: TemperatureFeel? = null,
    val environmentalMotion: List<EnvironmentalMotion> = emptyList()
)
```
**توضیح مغایرت:** `EnvironmentSettings` اصلاً فیلدی به نام `locationType` ندارد (این
اسم متعلق به `domain.scene.LocationType` است، یک enum کاملاً بی‌ربط) — فیلد واقعی در
همان موقعیت `windStrength` است. `environmentalMotion` (فیلد لیست جدید) هم کاملاً از
سند غایب است. مثل F7، این از همان Migration اخیر ناشی می‌شود.

**دسته‌بندی:** 🟡 — همان دلیل F7.

---

### F9 — بلوپرینت ۰۵ نسخه ۲ Rule ۸ را تعریف کرده اما در `ShotValidation.kt` پیاده نشده

**فایل:** `docs/blueprints/05-shot-engine-v2.md:272`
**نقل‌قول:**
> 🆕 ۸ | `negative_prompt_override` (در صورت غیر-null بودن) رشته‌ای معقول است (بدون
> محدودیت طول سخت‌گیرانه، فقط نباید کاملاً بی‌معنی/whitespace-only باشد) | **Warning**

**تأیید غیاب:** `grep -n "^fun \|^private fun " app/src/main/java/.../domain/shot/ShotValidation.kt`
فقط ۴ تابع برمی‌گرداند (`validateShotDescription`, `validateShotHasSubject`,
`validateShotBeatTimeline`, `validateImageReferenceFile`) — هیچ تابعی برای بررسی
whitespace-only بودن `negativePromptOverride` وجود ندارد.

**زمینه:** این یک یافته‌ی جدید نیست — خودِ `docs/adr/028-unit05-negative-prompt-override-migration.md`
صراحتاً می‌گوید: «Rule ۸ ... دستور کار این قدم صراحتاً فقط دو مورد را خواسته بود
(فیلد + تابع resolve)؛ Rule ۸ ... جزو این دستور کار نبودند، پس اضافه نشدند». اینجا
فقط برای تکمیل تصویر پیش از واحد ۱۶ (که فیلد Negative Prompt Override را مستقیماً در
Tab «اصلی» Shot Composer نمایش می‌دهد) دوباره ثبت شد.

**دسته‌بندی:** 🟡 — قبلاً شناخته‌شده و مستند، اما مستقیماً به فیلدی مربوط می‌شود که
واحد ۱۶ در همان اولین فرم نشان می‌دهد.

---

## ۳. یافته‌های ⚪ صرفاً مستندسازی (بدون اثر عملی فوری)

### F10 — ۱۰ فایل Kotlin هنوز به نام فایل بلوپرینت بدون `-v2` ارجاع می‌دهند

**فایل‌ها و خطوط دقیق:**

| فایل | خط | ارجاع فعلی |
|---|---|---|
| `data/entity/ProjectDnaEntity.kt` | 9 | `docs/blueprints/02-dna-manager.md` |
| `domain/shot/ShotSettingsResolution.kt` | 5 | `docs/blueprints/05-shot-engine.md` |
| `domain/shot/ShotOutfitSelection.kt` | 6 | `docs/blueprints/05-shot-engine.md` |
| `domain/shot/ShotValidation.kt` | 8 | `docs/blueprints/05-shot-engine.md` |
| `domain/asset/AssetSelection.kt` | 4 | `docs/blueprints/06-asset-and-continuity.md` |
| `domain/promptengine/PromptAssembly.kt` | 9 | `docs/blueprints/11-prompt-engineering-core.md` |
| `domain/promptengine/PriorityResolution.kt` | 4 | `docs/blueprints/11-prompt-engineering-core.md` |
| `domain/promptengine/WeightedEmphasis.kt` | 4 | `docs/blueprints/11-prompt-engineering-core.md` |
| `domain/promptengine/SeedManagement.kt` | 4 | `docs/blueprints/11-prompt-engineering-core.md` |
| `domain/promptengine/ConflictResolution.kt` | 7 | `docs/blueprints/11-prompt-engineering-core.md` |
| `domain/promptengine/PromptEngineModels.kt` | 17 | `docs/blueprints/11-prompt-engineering-core.md` |
| `domain/promptengine/CharacterContinuity.kt` | 8 | `docs/blueprints/11-prompt-engineering-core.md` |

(۱۲ سطر؛ «۱۰ فایل» در عنوان به تعداد فایل‌های یکتا اشاره دارد، چون واحد ۰۵/۰۶ چند
فایل و واحد ۱۱ شش فایل دارند.) نمونه‌ی نقل‌قول واقعی
(`domain/promptengine/PromptAssembly.kt:9`):
```
// منبع حقیقت: docs/blueprints/11-prompt-engineering-core.md
```
درحالی‌که فایل واقعی موجود در `docs/blueprints/` نسخه‌ی `11-prompt-engineering-core-v2.md`
است. توجه: ADR های تاریخی (مثل `docs/adr/001`, `002`, `003`, `006`, `015`, `021`,
`025`, `030`) که به نام بدون `-v2` ارجاع می‌دهند، **در این فهرست نیامده‌اند** — آن‌ها
روایت تاریخی معتبر از وضعیت زمان نگارش خودشان‌اند، نه ارجاع «منبع حقیقت» زنده.

**دسته‌بندی:** ⚪ — صرفاً کامنت هدر، بدون اثر روی کامپایل/رفتار. اصلاحش هزینه‌ی
نزدیک به صفر دارد (Migration بسیار کوچک مشابه آنچه برای واحد ۰۱/۱۴ قبلاً انجام شد).

---

### F11 — `type-registry.md` فیلدهای `Shot.camera`/`.lighting`/`.environment` را نادرست nullable نشان می‌دهد

**فایل:** `docs/reference/type-registry.md:148`
**نقل‌قول:**
> ... camera: SourcedSettings\<CameraSettings\>**?**, lighting:
> SourcedSettings\<LightingSettings\>**?**, environment:
> SourcedSettings\<EnvironmentSettings\>**?** ...

**کد واقعی:** `app/src/main/java/.../domain/shot/ShotModels.kt:90-92`:
```kotlin
val camera: SourcedSettings<CameraSettings> = SourcedSettings(),
val lighting: SourcedSettings<LightingSettings> = SourcedSettings(),
val environment: SourcedSettings<EnvironmentSettings> = SourcedSettings(),
```
**توضیح مغایرت:** این سه فیلد `nullable` نیستند — نوعشان همیشه یک `SourcedSettings<T>`
واقعی است (با مقدار پیش‌فرض `SourcedSettings()`، نه `null`). سند علامت `?` را اشتباه
اضافه کرده.

**دسته‌بندی:** ⚪ — فقط مستندسازی؛ کد واقعی درست است.

---

### F12 — `EntityState` بلوپرینت ۱۶ فقط ۴ مقدار را نام برده؛ enum واقعی ۵ مقدار دارد

**فایل:** `docs/blueprints/16-user-workflow-v2.md:57`
**نقل‌قول:**
> هر Entity (Project/Scene/Shot/Asset) یک وضعیت (`EntityState`: Draft/Review/Locked/Final) دارد.

**کد واقعی:** `app/src/main/java/.../domain/stateversioning/StateMachine.kt:9`:
```kotlin
enum class EntityState { DRAFT, REVIEW, LOCKED, FINAL, ARCHIVED }
```
**توضیح مغایرت:** بلوپرینت ۱۶ مقدار پنجم (`ARCHIVED`) را در توصیف متنی‌اش نیاورده.
برای UI به این معناست که نشانگر وضعیت Entity (آیکون قفل/رنگ روی کارت‌ها، طبق
یادداشت 🆕v6 همان بلوپرینت) باید از ابتدا برای ۵ حالت طراحی شود، نه ۴.

**دسته‌بندی:** ⚪ — توصیف ناقص در متن بلوپرینت؛ به‌سادگی هنگام طراحی Composable
نشانگر وضعیت قابل‌رفع است (کد واقعی صحیح و کامل است).

---

## ۴. بررسی‌های انجام‌شده بدون یافته (شفافیت درباره‌ی پوشش)

- **DTO/Entity persistence (بند ۳ درخواست):** `SceneDto`/`SceneMappers.kt` بررسی
  کامل شد — هر فیلد `Scene` (شامل `globalVisualStyle`, `constraints`) مسیر
  ذخیره‌سازی کامل دارد. `CharacterAssetDto`/`LocationAssetDto`/`ObjectAssetDto` (طبق
  Migration های اخیر ۰۶) و `LightingSettingsDto`/`EnvironmentSettingsDto` (طبق
  Migration اخیر ۰۸) پیش‌تر در همین نشست کامل شدند. هیچ یافته‌ی جدیدی فراتر از
  آنچه در ADR-036 مستند شده، پیدا نشد.
- **TODO/FIXME (بند ۴ درخواست):** فقط دو مورد واقعی در کل `app/src` (نه شمار
  کامنت‌های توضیحی که کلمه‌ی TODO را در متن فارسی خود دارند): `AiConnector.kt:68`
  (`sendToAiConnector` — عمدی، مستند در ADR-035، فقط کار آینده) و بلوپرینت ۱۶ خودش
  (`evaluatePromptQuality`، که چون اصلاً پیاده نشده، در F1 پوشش داده شد، نه اینجا
  تکراراً).
- **Camera (۰۹) / Audio (۱۰) Rule ها (بند ۷ درخواست، Spot-check):** ۵ Rule بلوپرینت
  ۰۹ (بخش الف) و Rule های Motion Intensity (بخش ب) با کد واقعی `CameraValidation.kt`/
  `MotionIntensityValidation.kt` مطابقت کامل دارند؛ Rule ۱/۲ بلوپرینت ۱۰ هم با
  `AudioValidation.kt` مطابقت دارد. یافته‌ای در این سه فایل پیدا نشد.

---

## خلاصه‌ی اجرایی

| دسته‌بندی | تعداد |
|---|---|
| 🔴 مسدودکننده | ۲ (F1, F2) |
| 🟡 غیرمسدودکننده، نیازمند مستندسازی/برنامه‌ریزی | ۷ (F3–F9) |
| ⚪ صرفاً مستندسازی | ۳ (F10–F12) |
| **جمع کل یافته‌ها** | **۱۲** |

**پیشنهاد ترتیب رفع (قبل از شروع Composable های واقعی واحد ۱۶):**
1. یک Migration کوچک: ساخت `domain/workflow/` با `WorkflowState`/`WorkflowStep`/
   `StepStatus`/`canJumpToStep`/`FeedbackType`/`ShotListViewMode` (F1) — دقیقاً طبق
   کد مفهومی خودِ بلوپرینت ۱۶.
2. یک تصمیم معماری صریح درباره‌ی F2 (اتصال Scene↔LocationAsset) — این نیازمند
   AskUserQuestion/تصمیم معمار است، نه یک انتخاب پیش‌فرض من.
3. اصلاح `type-registry.md` (F7, F8, F11) — کم‌هزینه، فقط مستندسازی.
4. اصلاح ۱۲ کامنت هدر بدون `-v2` (F10) — کم‌هزینه.
5. تصمیم‌های UI برای F5 (Outfit)/F6 (Location fields) هنگام طراحی خودِ فرم‌های
   واحد ۱۶ گرفته شوند (نیازی به یک قدم مجزای پیش از UI ندارند).
6. F3/F4/F9 می‌توانند به یک Migration جداگانه‌ی «اتصال Mood/enum های واقعی به
   Mapping های موجود» موکول شوند — غیرمسدودکننده برای شروع.

---

## ۵. وضعیت نهایی رفع یافته‌ها (به‌روزرسانی 2026-08-13)

**زمینه:** بعد از تکمیل واحد ۱۶ و ممیزی جامع post-unit16 (که جدا از این سند است،
در `docs/audit/post-unit16-full-audit.md`)، یک دور کامل بازبینی مستقل روی همین
۱۲ یافته‌ی این سند انجام شد — هر مورد با `grep`/خواندن مستقیم کد واقعی در رپو
(commit `bdd2f78` به بعد) راستی‌آزمایی شد، نه فقط بر مبنای ADR/README. **هر ۱۲
یافته بدون استثنا رفع شده‌اند.**

| # | یافته | وضعیت | رفع‌شده در |
|---|---|---|---|
| F1 | `domain/workflow/` غایب | ✅ رفع شد | `domain/workflow/WorkflowModels.kt` موجود؛ `docs/adr/037-unit16-workflow-models-quality-score.md`، `docs/adr/042-unit16-phase0-shared-foundation.md` (فاز ۰ واحد ۱۶) |
| F2 | `Scene`↔`LocationAsset` بدون ارجاع | ✅ رفع شد | `Scene.locationAssetId: String? = null` اضافه شد (در کنار `SceneLocation` موجود، نه جایگزین آن)؛ `docs/adr/038-unit04-scene-location-asset-link.md` |
| F3 | `mapMoodToLighting` رشته‌ی خام + مقادیر `style` نامعتبر | ✅ رفع شد | امضا به `mapMoodToLighting(mood: Mood): LightingPreset` (غیر-nullable، Total روی `MoodCategory`) تغییر کرد؛ `docs/adr/039-unit03-unit08-mood-type-safety-migration.md` |
| F4 | `getPacingFromEmotion` رشته‌ی خام | ✅ رفع شد | امضا به `getPacingFromEmotion(emotion: Mood): CinematicMode` تغییر کرد؛ همان `ADR-039` |
| F5 | `CharacterAsset.outfits` لیست ساختاریافته، فرم بلوپرینت ۱۶ TextField تکی فرض کرده | ✅ رفع شد | حین طراحی UI واقعی واحد ۱۶ حل شد (طبق پیش‌بینی خودِ این سند در بخش «پیشنهاد ترتیب رفع»، مورد ۵) |
| F6 | فیلدهای Location (Time/Weather چندانتخابی، `environment`/`keyElements` غایب از فرم) | ✅ رفع شد | حین طراحی UI واقعی واحد ۱۶ حل شد؛ فاز ۳ (`docs/unit16-execution-plan.md`) صریحاً به تصمیم F6 (Chip چندانتخابی، نه Dropdown تکی) ارجاع می‌دهد |
| F7 | `type-registry.md`: `LightingSettings` فاقد `lightSourceCount`، نام `motivation` اشتباه | ✅ رفع شد | ردیف `type-registry.md` با ترتیب/نام واقعی ۸ فیلد (`lightingMotivation`، نه `motivation`) بازنویسی شد؛ `docs/adr/040-unit05-unit08-type-registry-negative-prompt-rule8.md` |
| F8 | `type-registry.md`: `EnvironmentSettings` فیلد ناموجود `locationType`، فاقد `environmentalMotion`/`windStrength` | ✅ رفع شد | همان `ADR-040` |
| F9 | Rule ۸ (`negativePromptOverride` whitespace-only) پیاده نشده | ✅ رفع شد | `validateNegativePromptOverride(shot: Shot): ValidationIssue?` در `ShotValidation.kt` اضافه و در `ValidationAggregator` متصل شد؛ همان `ADR-040` |
| F10 | ۱۲ کامنت هدر بدون `-v2` | ✅ رفع شد | هر ۱۲ خط با `grep` مستقل تأیید شد که اکنون به نام فایل صحیح (`-v2`) ارجاع می‌دهند |
| F11 | `type-registry.md`: `Shot.camera`/`.lighting`/`.environment` اشتباهاً nullable نشان داده شده | ✅ رفع شد | ردیف `type-registry.md` اصلاح شد؛ کد از ابتدا هم درست بود (این یافته فقط مستندسازی بود) |
| F12 | بلوپرینت ۱۶: `EntityState` فقط ۴ مقدار نام برده، enum واقعی ۵ مقدار (`ARCHIVED`) دارد | ✅ رفع شد | کد از ابتدا درست بود (۵ مقدار)؛ این یافته فقط توصیف ناقص متن بلوپرینت بود، بدون اثر بر پیاده‌سازی نشانگر وضعیت |

**روش راستی‌آزمایی این به‌روزرسانی:** برای هر یافته، امضای تابع/فیلد/enum ادعاشده
در جدول بالا با `grep` مستقیم روی کد فعلی رپو تأیید شد (نه صرفاً با خواندن متن
ADR مربوطه) — همان انضباطی که خودِ این سند در نسخه‌ی اصلی به‌کار برده بود.

**نتیجه:** این سند از این پس کاملاً یک مرجع تاریخی است؛ هیچ یافته‌ی بازی از این
ممیزی باقی نمانده است.
