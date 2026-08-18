# ADR-112: تکمیل Rule یتیم CinematicLanguage — قدم ۴ از ۴ (UI سه سطح: DNA/Scene/Shot) — کل فیچر بسته شد

## زمینه

ADR-106 تا ADR-111 کل زنجیره‌ی دامنه‌ی `CinematicLanguage` را ساختند:
وایرینگ فیلدها (۱۰۶)، `beatIntensity`/`averageBeatIntensity` (۱۰۷)،
منطق Hybrid (۱۰۸)، Fallback سه‌لایه‌ی Mood (۱۰۹)، و هر سه Rule تعارض
دوربین/حرکت بلوپرینت ۰۳ (۱۱۰، ۱۱۱). تا پیش از این قدم، کاربر **هیچ راهی**
برای دیدن یا تنظیم دستی `CinematicMode`/`Mood` نداشت — فقط پیش‌فرض‌های
برنامه‌ریزی‌شده اعمال می‌شدند. این قدم — **آخرین قدم از ۴ قدم کل فیچر** —
UI هر سه سطح را اضافه می‌کند: `ProjectDna.cinematicLanguage.globalMode`
(تب DNA)، `Scene.cinematicModeOverride`/`Scene.mood` (فرم تنظیمات صحنه)،
و `Shot.cinematicModeOverride` (فرم شات).

## بررسی مستقل — دو یافته‌ی واقعاً متفاوت از پیش‌بریفینگ معمار

پیش‌بریفینگ ادعا می‌کرد هر سه فایل هدف (`DnaTabContent.kt`،
`SceneDetailScreen.kt`، `ShotComposerScreen.kt`) هرکدام یک نسخه‌ی
خصوصی جدا از کامپوننت الگوی `EnumDropdownField` دارند. با خواندن کامل
هر سه فایل، این ادعا **فقط برای `DnaTabContent.kt` درست بود**:

- `DnaTabContent.kt`: واقعاً `EnumDropdownField`/`FlatEntries`/
  `GroupedEntries` خصوصی محلی دارد (تأیید شد).
- `SceneDetailScreen.kt` و `ShotComposerScreen.kt`: هر دو در واقع
  کامپوننت مشترک بین-فیچر `AssetFormEnumDropdownField`/
  `AssetFormFlatEntries` (تعریف‌شده در `ui/assets/AssetFormSupport.kt`،
  `internal`) را برای همه‌ی فیلدهای Enum موجودشان (`shotGoal`،
  `shotType`، `motionLevel` در Shot؛ `narrativeRole`، `timeOfDay`،
  `atmospherePrimary/Secondary`، `globalVisualStyle` در Scene)
  Import و بازاستفاده می‌کنند — **نه یک کپی خصوصی جدا**.

تصمیم: برای Scene/Shot، همان کامپوننت مشترک واقعی
(`AssetFormEnumDropdownField`) Import و بازاستفاده شد — نه اختراع یک
کپی خصوصی تازه طبق پیش‌بریفینگِ نادرست. این هم کد کمتری است و هم با
الگوی واقعی و از قبل جاافتاده‌ی همان دو فایل سازگارتر.

یافته‌ی دوم: هیچ فایلی با نام `SceneDetailFlowTest.kt` (یا هر نامی
حاوی «SceneDetail») در `app/src/test` وجود ندارد. فایل واقعی و کاملاً
معادل که `SceneDetailScreen`/`SceneSettingsDialog` را با جزئیات پوشش
می‌دهد `ui/scenes/ScenesFlowTest.kt` است (نام‌گذاری‌شده برای کل ناحیه‌ی
فیچر «Scenes» — لیست + جزئیات — نه فقط جزئیات). تست‌های تازه‌ی این قدم
به همین فایل واقعی اضافه شدند.

هر دو یافته طبق دستور صریح («اگر پیش‌بریفینگ با کد واقعی فرق داشت، به
یافته‌ی خودت اعتماد کن، نه پیش‌بریفینگ») به یافته‌ی مستقل خودم عمل شد؛
هر دو اینجا برای تأیید/رد معمار ثبت می‌شوند.

## کار انجام‌شده

**۱. `DnaTabContent.kt`/`DnaViewModel.kt`/`DnaLabels.kt`:** `DnaGroup`
تازه («زبان سینمایی») با یک `EnumDropdownField` غیر-nullable برای
`cinematicLanguage.globalMode` (۳ مقدار ثابت) — هم‌الگوی دقیق فیلد
`lightingPreference` موجود. `cinematicModeLabel(mode, language)` یک‌بار
در `DnaLabels.kt` تعریف شد (هم‌الگوی دقیق پیشینه‌ی `moodLabel` در
`ui/story/StoryLabels.kt` که از قبل بین `ui/dna/`، `ui/scenes/`،
`ui/shots/` بازاستفاده می‌شود) — تا `SceneDetailScreen.kt`/
`ShotComposerScreen.kt` هم آن را از همین محل واحد Import کنند، نه هرکدام
یک نسخه‌ی جدا.

**۲. `SceneDetailScreen.kt`/`SceneDetailViewModel.kt`:** دو فیلد
nullable تازه در `SceneSettingsDialog` — `cinematicModeOverride`
(گزینه‌ی صریح «پیروی از پروژه» = `null`) و `mood` (گزینه‌ی صریح
«تعیین‌نشده» = `null`) — هم‌الگوی دقیق فیلد nullable موجود
(`globalVisualStyle.override`). دو `InfoRow` تازه هم در تب Overview
مقدار فعلی هرکدام را نمایش می‌دهند. `saveSceneSettings` با دو پارامتر
انتهایی تازه گسترش یافت (بدون شکستن هیچ Caller دیگری — با `grep` تأیید
شد تنها Caller همین فایل است).

**۳. `ShotComposerScreen.kt`/`ShotComposerViewModel.kt`:** یک فیلد
nullable تازه (`cinematicModeOverride`، گزینه‌ی صریح «پیروی از
صحنه/پروژه» = `null`) داخل `MainFieldsSection`. StateFlow های تازه
مستقیماً از خودِ `viewModel` (که این تابع همین الان به‌عنوان پارامتر
دریافت می‌کند) با `collectAsStateWithLifecycle()` جمع‌آوری می‌شوند — نه
پارامتر تازه در امضای تابع — تا هر دو محل فراخوان موجود (چیدمان TABS و
ACCORDION) بدون هیچ تغییری خودکار این فیلد را دریافت کنند.

## تصمیم — نمایش «حالت مؤثر نهایی» فقط برای Shot، نه Scene

آیتم اختیاری دستور کار: آیا نمایش خروجی واقعی
`resolveEffectiveCinematicMode` (نه فقط مقدار خام Override) زیر
Dropdown ارزش افزوده‌ی واقعی دارد؟ تصمیم: **بله، فقط در Shot Composer.**

دلیل: `ShotComposerViewModel` از قبل یک نقطه‌ی محاسبه‌ی آماده و
تقریباً رایگان دارد — `refreshValidationSummary()`، که همین الان
`cachedScene`/`cachedDna` را در هر Save کش می‌کند و `aggregateShotValidation`
را محاسبه می‌کند؛ اضافه‌کردن `_effectiveCinematicMode` به همین تابع بدون
هیچ I/O تازه‌ای ممکن بود. در مقابل، حالت مؤثر نهایی واقعی (با منطق
هوشمند Hybrid/Mood، ADR-108/109) اساساً به داده‌ی سطح Shot
(`shot.beats`، `shot.shotGoal`) وابسته است که در سطح Scene اصلاً وجود
ندارد — نمایش یک پیش‌نمایش ناقص/بالقوه گمراه‌کننده در سطح Scene ارزش
پیچیدگی و ریسک سردرگمی کاربر را نداشت.

## باگ واقعی پیداشده و رفع‌شده — نه در کد Production، در کد تست

هنگام نوشتن تست انتخاب `Mood.MYSTERIOUS` در `ScenesFlowTest.kt`، تست
مکرراً با `ComposeTimeoutException` شکست می‌خورد (نه Flaky — تکرارپذیر).
فرضیه‌ی اول (تصادم متنی فارسی «مرموز» بین `Mood.MYSTERIOUS` و
`Atmosphere.MYSTERIOUS`) رد شد، چون `atmospherePrimary` پیش‌فرض صحنه‌ی
تازه `CALM` است، نه `MYSTERIOUS`. با ابزار دیباگ موقت
(`DEBUG_MATCH_COUNT`) علت واقعی پیدا شد: همان کلاس باگ مستندشده‌ی
بالای همین فایل برای FAB — `.performClick()` مبتنی‌بر مختصات، وقتی گره‌ی
هدف (اینجا: یک `DropdownMenuItem` در ایندکس ۱۷ از ۲۵، در یک `Column`
غیر-Lazy طولانی داخل `DropdownMenu`/Popup) بیرون از ناحیه‌ی اندازه‌گیری‌شده‌ی
اولیه باشد، بی‌صدا شکست می‌خورد — با اینکه گره در درخت Semantics درست
پیدا می‌شود. رفع: `.performClick()` به `.clickViaSemantics()` (همان
Helper موجود همین فایل، `performSemanticsAction(SemanticsActions.OnClick)`)
تغییر کرد. `CinematicMode` فقط ۳ مقدار دارد (همیشه نزدیک بالای لیست)،
پس این ریسک کمتر بود، اما همان الگوی ایمن (`.clickViaSemantics()`) برای
هر انتخاب آیتم Dropdown در تست‌های تازه‌ی این قدم (DNA، Scene، Shot) هم
رعایت شد.

## تست

**`DnaTabFlowTest.kt` (۲ تست تازه):** انتخاب `globalMode` واقعاً Persist
می‌شود؛ تست End-to-End — تغییر `globalMode` از تب DNA واقعاً روی خروجی
`resolveEffectiveCinematicMode` برای یک Shot بدون Override اثر می‌گذارد
(بارگذاری DNA واقعی از `ProjectDnaRepository` پس از ذخیره از طریق UI،
نه یک شیء دامنه‌ی دستی).

**`ScenesFlowTest.kt` (۲ تست تازه):** انتخاب `cinematicModeOverride` و
انتخاب `mood` هرکدام واقعاً Persist می‌شوند و برچسب ترجمه‌شده در Overview
نمایش داده می‌شود (شامل رفع باگ بالا).

**`ShotComposerAccordionFlowTest.kt` (۲ تست تازه):** انتخاب یک
`cinematicModeOverride` واقعاً Persist می‌شود و متن «حالت مؤثر فعلی»
واقعاً همان مقدار Override را نشان می‌دهد (چون Override همیشه بالاترین
اولویت را دارد)؛ انتخاب دوباره‌ی گزینه‌ی «پیروی از صحنه/پروژه» مقدار را
واقعاً به `null` برمی‌گرداند (نه رشته‌ی خالی یا Placeholder دیگر) — و
سپس همان مقدار Fallback واقعی محاسبه‌شده با `resolveEffectiveCinematicMode`
(روی DNA/Scene/Shot واقعاً بارگذاری‌شده از Repository) در UI نمایش داده
می‌شود.

این فایل (نه یک فایل تازه) به‌عنوان محل تست انتخاب شد چون
`openShotComposer()`/Fixture های موجودش (بدون پیچیدگی Clipboard/
`ComponentActivity` فایل `ShotComposerSummaryFlowTest.kt`) نزدیک‌ترین
الگوی آماده بود.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`DnaTabFlowTest`) | موفق (شامل ۲ تست تازه) |
| `gradle :app:testDebugUnitTest` (`ScenesFlowTest`) | ۹ تست (۷+۲ تازه)، موفق |
| `gradle :app:testDebugUnitTest` (`ShotComposerAccordionFlowTest`) | ۵ تست (۳+۲ تازه)، موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۷۷ تست (۸۷۱+۶ تازه)، ۱ شکست نامرتبط (`OutputDeliveryFlowTest`، ناحیه‌ی فیچر کاملاً جدا، هیچ‌جا در این قدم لمس نشد) — با `git stash` تأیید شد این شکست روی HEAD تمیز (بدون هیچ تغییر این قدم) هم دقیقاً به همین شکل رخ می‌دهد (۸۷۱ تست، ۱ شکست) و در اجرای مجزا (`--tests`) همیشه موفق است؛ همان کلاس Flaky محیط Compose/Robolectric این Sandbox که در ADR-105 تا ADR-111 مستند شده |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این قدم (عمداً، طبق دستور صریح)

هیچ تغییری در `CinematicLanguage.kt`، `ValidationAggregator.kt`،
`LogicConflictChecker.kt` — این سه فایل در قدم‌های ۱ تا ۳ کامل و درست
انجام شدند.

## نتیجه — کل فیچر «تکمیل Rule یتیم CinematicLanguage» به‌طور کامل و End-to-End بسته شد

با این قدم (۴ از ۴)، زنجیره‌ی کامل ADR-106 تا ADR-112 بسته شد. برای اولین
بار، یک کاربر واقعی می‌تواند: حالت روایی کلی پروژه را در تب DNA تنظیم
کند؛ آن را در سطح صحنه Override کند؛ دوباره در سطح شات Override کند؛
Mood صحنه را تنظیم کند — و همه‌ی این‌ها واقعاً روی هشدارهای Validation
واقعی (مدت شات، تعارض‌های دوربین/حرکت) اثر می‌گذارند؛ نه فقط در لایه‌های
ایزوله، بلکه در زنجیره‌ی کامل UI → ViewModel → Repository → دامنه →
Validation، با تست End-to-End صریح که این را اثبات می‌کند.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
