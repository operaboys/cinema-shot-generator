# ADR-109: تکمیل Rule یتیم CinematicLanguage — قدم ۲ج از ۴ زیرقدم قدم ۲ (افزودن Mood به Scene + اتصال getPacingFromEmotion)

## زمینه

ADR-108 (زیرقدم ۲ب) اولین تابع هوشمند Hybrid — `determineHybridPacing`
(بر اساس `shot.shotGoal` و `averageBeatIntensity(shot.beats)`) — را به
`resolveEffectiveCinematicMode` وصل کرد. این زیرقدم (۲ج) دومین تابع
هوشمند — `getPacingFromEmotion(emotion: Mood)` — را وصل می‌کند، با یک
اولویت سه‌سطحی که **تصمیم صریح کاربر پروژه بود، نه حدس معمار**: Beat واقعی
شات > Mood صحنه > `shotGoal` تنها.

## بررسی مستقل (پیش از نوشتن کد) — تفکیک دو enum

با خواندن مستقیم (نه صرفاً پذیرفتن پیش‌بریفینگ) هر دو تعریف:

```kotlin
// domain/scene/SceneModels.kt
enum class Atmosphere { CALM, TENSE, DARK, BRIGHT, MYSTERIOUS, EMOTIONAL }  // ۶ مقدار

// domain/dna/ProjectDna.kt
enum class MoodCategory { HIGH_ENERGY, POSITIVE, EMOTIONAL, DARK, CALM }
enum class Mood(val category: MoodCategory) { EPIC(HIGH_ENERGY), ACTION(HIGH_ENERGY), ..., DREAMY_MOOD(CALM) }  // ۲۵ مقدار
```

**تأیید کامل هشدار پیش‌بریفینگ:** این دو enum کاملاً مستقل‌اند. `Atmosphere`
هیچ فیلد `category` ندارد، هیچ تبدیل خودکاری به `Mood` وجود ندارد (بدون
هیچ تابع نگاشتی بین این دو در کل کدبیس — تأییدشده با `grep`)، و
`getPacingFromEmotion` صرفاً `Mood` (نه `Atmosphere`) می‌پذیرد. استفاده از
`atmospherePrimary` موجود به‌جای یک فیلد `Mood` جداگانه، یک تبدیل ضمنی و
نادرست بین دو مفهوم متفاوت می‌ساخت (مثلاً `Atmosphere.BRIGHT` معادل کدام
`Mood`؟ هیچ نگاشت منطقی و بدون‌ابهامی وجود ندارد) — رد شد.

## تصمیم ۱ — `Scene.mood: Mood? = null`

```kotlin
val mood: Mood? = null
```

فیلد جدید و کاملاً مستقل از `atmospherePrimary`/`atmosphereSecondary`
موجود. `null` یعنی «هیچ Mood صریحی برای این صحنه تعیین نشده» — لایه‌ی
سوم Fallback (پایین) وقتی این مقدار خالی است فعال می‌شود.

**`SceneEntity` نیازی به تغییر نداشت** — با خواندن مستقیم
`data/entity/SceneEntity.kt` تأیید شد (طبق همان الگوی ADR-106) این Entity
صرفاً یک ستون JSON Blob تک‌رشته‌ای (`sceneDataJson`) است، بدون ستون‌های
مجزای SQL؛ افزودن فیلد جدید فقط در `SceneModels.kt` (دامنه) + `SceneDto.kt`
+ `SceneMappers.kt` لازم بود، بدون Migration رسمی Room.

## تصمیم ۲ — اولویت سه‌سطحی در `resolveEffectiveCinematicMode`

```kotlin
if (settings.globalMode != CinematicMode.BALANCED) return settings.globalMode
if (shot.beats.isNotEmpty()) {
    return determineHybridPacing(
        sceneType = shot.shotGoal.name.lowercase(),
        avgBeatIntensity = averageBeatIntensity(shot.beats)
    )
}
scene.mood?.let { return getPacingFromEmotion(it) }
return determineHybridPacing(
    sceneType = shot.shotGoal.name.lowercase(),
    avgBeatIntensity = averageBeatIntensity(shot.beats)
)
```

این منطق **فقط** در همان لایه‌ی نهایی Fallback ADR-108 اجرا می‌شود (بدون
هیچ Override دستی، و `globalMode` واقعاً `BALANCED`). سه‌سطحی داخلی:

1. **Beat واقعی شات (بالاترین اولویت):** `shot.beats.isNotEmpty()` →
   `determineHybridPacing` با شدت واقعی Beat Sheet.
2. **Mood صحنه:** فقط وقتی `shot.beats` کاملاً خالی است و `scene.mood`
   غیر-null است → `getPacingFromEmotion(scene.mood)`.
3. **`shotGoal` تنها (Fallback نهایی، بدون تغییر نسبت به ADR-108):**
   وقتی هم Beat خالی است و هم Mood تنظیم نشده.

**دلیل ترتیب (جزئی‌تر > کلی‌تر، هم‌راستا با الگوی موجود پروژه):** Beat
Sheet داده‌ای در سطح خودِ همین شات خاص است — دقیق‌ترین سیگنال ممکن. Mood
صحنه یک سیگنال کلی‌تر (برای کل صحنه، نه فقط این شات) است — فقط وقتی هیچ
داده‌ی دقیق‌تری در کار نیست به آن عقب‌نشینی می‌شود. این دقیقاً همان الگویی
است که خودِ `resolveEffectiveCinematicMode` از ADR-106 دارد (شات > صحنه >
پروژه).

## تصمیم ۳ — تشخیص «Beat خالی» با `shot.beats.isEmpty()` مستقیم، نه با مقدار میانگین

**چرا نه بررسی `averageBeatIntensity(shot.beats) == 5f`:** مقدار خنثی ۵
(ADR-107) *هم* برای لیست خالی *و هم* می‌تواند برای یک Beat Sheet واقعاً
غیرخالی رخ دهد — مثلاً یک Beat با `SUBJECT_ACTION` (شدت ۸) + یک Beat با
`LIGHTING_CHANGE` (شدت ۲) دقیقاً میانگین ۵ می‌دهد. اگر تشخیص «خالی بودن»
بر پایه‌ی مقدار میانگین بود، یک Beat Sheet واقعی (با داده‌ی معتبر) به‌اشتباه
نادیده گرفته می‌شد و مسیر Mood/shotGoal به‌جای مسیر Beat واقعی اجرا
می‌شد. بررسی مستقیم `shot.beats.isEmpty()` این ابهام را کاملاً حذف می‌کند.

## تست

**`CinematicLanguageTest.kt` (۳ تست تازه، طبق مشخصات دقیق دستور کار):**
- `shot.beats` خالی + `scene.mood = Mood.TENSE` (`MoodCategory.DARK`) →
  `FAST_CUT` (از `getPacingFromEmotion`، نه از `shotGoal`).
- همان سناریو با `scene.mood = null` → `BALANCED` (رفتار ADR-108
  دست‌نخورده — `ESTABLISHING` + شدت خنثی).
- `shot.beats` غیرخالی (یک Beat) + `shotGoal=ACTION` + `scene.mood =
  Mood.SERENE` (`MoodCategory.CALM`، که به‌تنهایی `LONG_TAKE` می‌داد) →
  `FAST_CUT` (Beat/shotGoal برنده می‌شود، نه Mood — اثبات مستقیم اولویت
  قطعی تصمیم‌شده).

**`SceneMappersTest.kt` (۲ تست تازه، هم‌الگو دقیق با
`cinematicModeOverride`):** Round-Trip `mood` (هم مقداردار هم `null`)؛
Decode یک JSON قدیمی صحنه بدون کلید `mood` اصلاً — بدون خطا، مقدار
پیش‌فرض `null`.

**`SceneRepositoryTest.kt` (بدون تست تازه، `fullScene` گسترش‌یافته):**
`mood = Mood.MYSTERIOUS` به Fixture مشترک اضافه شد تا Round-Trip واقعی
Room (نه فقط Mapper خالص) هم این فیلد را بپوشاند.

**رگرسیون — `ValidationAggregatorTest.kt` (هر ۹ تست موجود، بدون تغییر
کد):** بررسی و اجرا شد. `neutralScene()` هیچ‌وقت `mood` صریح تنظیم نمی‌کند
(پیش‌فرض `null`) و `neutralShot()` هم `beats` خالی دارد — یعنی این ۹ تست
همیشه به لایه‌ی سوم (Fallback نهایی `shotGoal` تنها) می‌رسند، دقیقاً همان
مسیری که در ADR-108 هم بدون تغییر بود. **هر ۹ تست بدون هیچ تغییر کدی سبز
ماندند.**

**رگرسیون — تست‌های قبلی `resolveEffectiveCinematicMode` (ADR-106/108،
۹ تست، بدون تغییر):** بررسی و اجرا شد — همه یا Override دستی صریح دارند
(لایه‌های بالاتر، دست‌نخورده)، یا `globalMode` غیر-`BALANCED` است، یا
`shot.beats` غیرخالی است (پس هرگز به مسیر Mood نمی‌رسند). همه سبز ماندند.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:compileDebugUnitTestKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`CinematicLanguageTest`) | ۴۲ تست (۳۹ + ۳ تازه)، موفق |
| `gradle :app:testDebugUnitTest` (`SceneMappersTest`) | ۷ تست (۵ + ۲ تازه)، موفق |
| `gradle :app:testDebugUnitTest` (`SceneRepositoryTest`) | همه موفق (بدون تست تازه، Fixture گسترش‌یافته) |
| `gradle :app:testDebugUnitTest` (`ValidationAggregatorTest`) | ۹ تست، همه موفق، **بدون تغییر کد در آن فایل** |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۵۹ تست (۸۵۴ + ۵ تازه)، ۲ شکست نامرتبط (`AssetFormFlowTest`، `OutputDeliveryFlowTest` — هیچ‌کدام در این قدم لمس نشدند) — هر دو در اجرای مجزا (`--tests`) موفق؛ Flaky شناخته‌شده‌ی محیط Compose/Robolectric این Sandbox، هم‌الگوی مستندشده‌ی ADR-105/106/107/108 |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این زیرقدم (عمداً)

- سه Rule تعارض دوربین و حرکت (`LogicConflictChecker.kt`) — قدم ۳.
- UI — قدم ۴. هیچ فایل UI/ViewModel لمس نشد (از‌جمله فرم Scene که در
  آینده باید `mood` را قابل‌ویرایش کند — کار قدم ۴).

## نتیجه

هر دو تابع هوشمند Hybrid بلوپرینت ۰۳ بخش ب (`determineHybridPacing` و
`getPacingFromEmotion`) اکنون واقعاً در `resolveEffectiveCinematicMode`
وصل‌اند، با یک اولویت سه‌سطحی مستند و تست‌شده (Beat > Mood صحنه > `shotGoal`
تنها). چون این همان تابعی است که `validateShotDurationForCinematicMode`
(از طریق `ValidationAggregator.kt`) را تغذیه می‌کند، این هوشمندی کامل بدون
هیچ کد اضافه‌ای در مسیر Validation واقعی هم منعکس می‌شود.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
