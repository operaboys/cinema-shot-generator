# ADR-106: تکمیل Rule یتیم CinematicLanguage — قدم ۱ از ۴ (اتصال دامنه‌ای کامل، بدون UI)

## زمینه

`domain/visualidentity/CinematicLanguage.kt` (واحد ۰۳ بخش ب، بلوپرینت
`docs/blueprints/03-visual-identity.md`) از قبل کامل و درست پیاده‌سازی شده
بود — `CinematicMode`، `CinematicLanguageSettings`، `resolveEffectiveMode`،
`validateShotDurationForCinematicMode`، `determineHybridPacing`،
`getPacingFromEmotion` — اما یک Rule یتیم بود: با `grep` مستقیم روی
`app/src/main` تأیید شد که هیچ‌کدام از این نمادها در هیچ مسیر واقعی تولید
(نه فقط تست) فراخوانی نمی‌شدند. تنها ارجاع دیگر `LogicConflictChecker.kt`
بود (که خودش هم فقط از تست خودش فراخوانی می‌شود — یتیمی مستقل، خارج از Scope
این قدم) و یک اشاره‌ی کامنتی در `ProjectDna.kt` (نه فیلد واقعی).

طبق بلوپرینت ۰۳ بخش ب، این یک سیستم سه‌سطحی است: تنظیم سراسری پروژه
(`global_mode`) ← Override اختیاری سطح Scene ← Override اختیاری سطح Shot
(همیشه در دسترس؛ `allow_shot_override` طبق بلوپرینت همیشه `true` است).

**بررسی مستقل ADR-027 (پیش از نوشتن کد):** ADR-027 صراحتاً
`stylePreferences`/`overrideRules` (و به‌طور خاص `requiresApprovalForOverride`
— مفهوم تأیید دستی Override) را از `ProjectDna` حذف کرد. این با
`CinematicLanguageSettings` **کاملاً بی‌ربط** است — `CinematicLanguageSettings`
هیچ‌جای ADR-027 ذکر نشده و مفهومی کاملاً متفاوت است (نه تأیید دستی Override،
بلکه انتخاب حالت ریتم روایی). این قدم یک افزودنی تازه است، نه بازگرداندن چیزی
که عمداً حذف شده بود.

این قدم **فقط قدم ۱ از ۴** برنامه‌ریزی‌شده برای بستن کامل این Rule یتیم است:
- **قدم ۱ (همین ADR):** اتصال دامنه‌ای کامل (ProjectDna → Scene → Shot)، بدون UI.
- **قدم ۲ (بعدی):** اتصال `determineHybridPacing`/`getPacingFromEmotion` (منطق هوشمند Hybrid).
- **قدم ۳ (بعدی):** سه Rule تعارض دوربین و حرکت (`checkFastMotionLongTake` و مشابه، `LogicConflictChecker.kt`).
- **قدم ۴ (بعدی):** UI (فرم DNA، فرم Scene، فرم Shot).

## یافته‌های اختلاف با پیش‌بریفینگ (طبق دستور صریح، افشا می‌شوند)

1. **`SceneEntity`/`ShotEntity` نیازی به تغییر کد نداشتند.** پیش‌بریفینگ
   درخواست «افزودن فیلد به SceneEntity/ShotEntity» را داشت، اما با خواندن
   کامل این دو فایل تأیید شد هرکدام صرفاً یک Wrapper با یک ستون JSON Blob
   تک‌رشته‌ای هستند (`sceneDataJson`/`shotDataJson`) — بدون ستون‌های مجزای
   SQL. طبق الگوی تثبیت‌شده‌ی این پروژه برای هر افزودن مشابه قبلی
   (`locationAssetId`، `linkedAssetIds`، `state`، `negativePromptOverride`
   — هیچ‌کدام هم فایل Entity را لمس نکردند)، فیلدهای تازه فقط در
   دامنه+DTO+Mapper اضافه شدند؛ خودِ فایل‌های Entity بدون تغییر ماندند —
   بدون نیاز به Room Migration.
2. **محل مرکزی واقعی Validation، `validateShotAgainstDna` نبود.**
   پیش‌بریفینگ این نام را به‌عنوان مثال محل مرکزی پیشنهاد داده بود.
   `domain/shot/ShotValidation.kt` صراحتاً کامنت دارد که هیچ Aggregator ای
   در آن فایل وجود ندارد. `validateShotAgainstDna` واقعاً وجود دارد، اما یک
   Rule مستقل (در `domain/dna/`) است که خودش **از داخل** یک Aggregator
   دیگر فراخوانی می‌شود، نه خودِ Aggregator. محل مرکزی واقعی —
   تأییدشده با `grep` برای فراخوانی‌کنندگان واقعی —
   `aggregateShotValidation` در `domain/validation/ValidationAggregator.kt`
   است (فراخوانی‌شده از `ValidationViewModel`، `StudioOutputViewModel`،
   `ShotComposerViewModel`، و تست جریان Compose). این Rule جدید در همین محل
   واقعی وایر شد، نه یک مسیر فراخوانی تازه و مجزا.

## تصمیم ۱ — `ProjectDna.cinematicLanguage`

```kotlin
val cinematicLanguage: CinematicLanguageSettings =
    CinematicLanguageSettings(globalMode = CinematicMode.BALANCED)
```

آخرین فیلد `ProjectDna`، با پیش‌فرض — پروژه‌ها/تست‌های موجود بدون این فیلد
صریح هم بدون تغییر کار می‌کنند. `BALANCED` (نه `LONG_TAKE`/`FAST_CUT`)
به‌عنوان خنثی‌ترین پیش‌فرض سه‌گانه انتخاب شد.

در DTO (`ProjectDnaDto`)، `CinematicLanguageSettingsDto` اضافه شد:

```kotlin
data class CinematicLanguageSettingsDto(
    val globalMode: String = "BALANCED",
    val sceneOverrides: Map<String, String> = emptyMap()
)
```

**تصمیم شکل ذخیره‌سازی:** `sceneOverrides` دامنه (`Map<String,
CinematicMode>`) دقیقاً به همان شکل مفهومی — `Map<String, String>`
(sceneId → نام Enum) — ذخیره شد، نه یک ساختار جایگزین (مثل
`List<Pair<String,String>>`). دلیل: این هم‌الگوی دقیق بقیه‌ی این فایل است
(هر Enum دامنه به‌صورت نام رشته‌ای، بدون DTO Enum مجزا) و
kotlinx.serialization به‌طور بومی از `Map<String, String>` به‌عنوان یک شیء
JSON پشتیبانی می‌کند — نیازی به شکل جایگزین نبود.

## تصمیم ۲ — `Scene.cinematicModeOverride` و `Shot.cinematicModeOverride`

```kotlin
// Scene
val cinematicModeOverride: CinematicMode? = null   // null = پیروی از پیش‌فرض پروژه

// Shot
val cinematicModeOverride: CinematicMode? = null   // null = پیروی از حالت مؤثر صحنه/پروژه
```

هر دو فیلد Nullable، هم‌الگوی دقیق `Shot.negativePromptOverride` (ADR-028)
— نه `SourcedSettings<T>` (که برای camera/lighting/environment استفاده
می‌شود و یک فیلد `source` جداگانه دارد؛ اینجا لازم نبود چون فقط دو حالت
«Override دارد» / «ندارد» مطرح است، نه سه حالت منبع).

## تصمیم ۳ — `resolveEffectiveCinematicMode` (زنجیره‌ی سه‌سطحی)

اضافه‌شده در همان فایل `CinematicLanguage.kt` (نه فایل جدید — چون منطقاً
بخشی از همان مجموعه‌ی توابع Resolve این واحد است، کنار `resolveEffectiveMode`
موجود):

```kotlin
fun resolveEffectiveCinematicMode(projectDna: ProjectDna, scene: Scene, shot: Shot): CinematicMode {
    shot.cinematicModeOverride?.let { return it }
    scene.cinematicModeOverride?.let { return it }
    return resolveEffectiveMode(projectDna.cinematicLanguage, scene.sceneId)
}
```

**تصمیم مهم — دو مسیر Override سطح Scene، بدون تناقض:** پرسش پیش‌بریفینگ
این بود که آیا `scene.cinematicModeOverride` باید از طریق `resolveEffectiveMode`/
`sceneOverrides` موجود عبور کند یا مستقیم بررسی شود. تصمیم: **هر دو**،
لایه‌بندی‌شده. `scene.cinematicModeOverride` یک فیلد مستقیم و محلی روی خودِ
Scene است (مشابه `locationAssetId`/`negativePromptOverride`) — برای ویرایش
تک‌صحنه‌ای از فرم Scene (که در قدم ۴ UI خواهد گرفت). `sceneOverrides` روی
`ProjectDna` دقیقاً همان ساختار Map مفهومی خودِ بلوپرینت (`scene_overrides`
در JSON) است — برای مدیریت متمرکز چند Override از یک محل واحد. اولویت با
فیلد مستقیم Scene است؛ اگر خالی بود، به مکانیزم متمرکز پروژه (که خودِ
`resolveEffectiveMode` موجود آن را بررسی می‌کند) برمی‌گردد. هیچ منطق
تکراری نوشته نشد — `resolveEffectiveMode` عیناً بازاستفاده شد.

اولویت نهایی: **Override شات > Override صحنه (مستقیم یا متمرکز) > پیش‌فرض
سراسری پروژه** — دقیقاً مطابق «`allow_shot_override` همیشه `true` است»ِ
بلوپرینت.

## تصمیم ۴ — وایرینگ در `ValidationAggregator.kt` (Level 3)

```kotlin
add(l3, validateShotDurationForCinematicMode(shot.durationSeconds, resolveEffectiveCinematicMode(dna, scene, shot)))
```

در Level 3 (`CONTINUITY_AND_DEPENDENCY`)، کنار `validateShotDuration` موجود
— چون این Rule هم مدت شات را در برابر یک محدودیت مشتق‌شده از چند منبع
(DNA + Scene + Shot) می‌سنجد، نه صرفاً داده‌ی خودِ همین Shot به‌تنهایی
(که Level 1/2 هستند).

## خارج از Scope این قدم (عمداً)

- UI فرم DNA/Scene/Shot — **قدم ۴**.
- `determineHybridPacing`/`getPacingFromEmotion` — **قدم ۲**.
- سه Rule تعارض دوربین و حرکت (`LogicConflictChecker.kt`) — **قدم ۳**.

## تست

- **`CinematicLanguageTest.kt` (۴ تست تازه):** هر سه سطح زنجیره به‌صورت
  مجزا (فقط پیش‌فرض پروژه؛ Override صحنه بدون Override شات؛
  `sceneOverrides` متمرکز پروژه بدون Override مستقیم صحنه؛ Override شات
  که همه‌چیز دیگر را می‌پوشاند).
- **`ValidationAggregatorTest.kt` (۲ تست تازه):** اثبات وایرینگ واقعی —
  مدت شات خارج از بازه‌ی حالت مؤثر یک Warning واقعی در Level 3 تولید
  می‌کند؛ Override شات باعث می‌شود همان مدت دیگر هیچ هشداری نگیرد (چون
  حالت مؤثر واقعی عوض شده).
- **`SceneMappersTest.kt` (۲ تست تازه):** Round-Trip `cinematicModeOverride`
  (هم مقداردار هم `null`)؛ Decode یک JSON قدیمی صحنه بدون این کلید اصلاً —
  بدون خطا، مقدار پیش‌فرض `null`.
- **`DtoMappersTest.kt` (۳ تست تازه):** Round-Trip `Shot.cinematicModeOverride`؛
  Decode JSON قدیمی شات بدون این کلید؛ Decode JSON قدیمی DNA بدون کلید
  `cinematicLanguage` — پیش‌فرض `BALANCED`/`sceneOverrides` خالی.
- **`ProjectDnaRepositoryTest.kt`/`SceneRepositoryTest.kt`/`ShotRepositoryTest.kt`
  (بدون تست تازه، تست موجود گسترش‌یافته):** فیلدهای تازه با مقدار غیر-پیش‌فرض
  به `fullDna`/`fullScene`/`fullShot` اضافه شدند تا Round-Trip واقعی Room
  (نه فقط Mapper خالص) این فیلدها را هم بپوشاند.
- **رگرسیون:** تمام سازنده‌های `neutralDna()`/`neutralScene()`/`neutralShot()`
  در `ValidationAggregatorTest.kt` بدون تغییر ماندند (بدون مقدار صریح برای
  فیلدهای تازه) و تست «صفر مشکل» همچنان سبز است — اثبات عملی اینکه
  پیش‌فرض‌ها (`BALANCED`، `null`) هیچ رفتار موجودی را نمی‌شکنند.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:compileDebugUnitTestKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۳۸ تست (۸۲۷ + ۱۱ خالص تازه)، ۱ شکست نامرتبط (`OutputDeliveryFlowTest`، فایلی که این قدم اصلاً لمس نکرد) — در اجرای مجزا (`--tests`) موفق؛ Flaky شناخته‌شده‌ی محیط Compose/Robolectric این Sandbox، هم‌الگوی مستندشده‌ی قبلی این پروژه برای این دسته تست‌ها (مثلاً ADR-105 برای `BackupsFlowTest`) |
| `gradle :app:assembleDebug` | موفق |

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
