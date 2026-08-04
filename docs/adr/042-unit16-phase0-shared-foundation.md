# ADR-042: واحد ۱۶ — فاز ۰ (پایه‌ی مشترک): Design Tokens + Navigation دو‌لایه + WorkflowViewModel

**تاریخ:** 2026-08-04
**وضعیت:** کامل شد و مستقل قابل‌کامپایل/تست است. `gradle :app:assembleDebug
:app:testDebugUnitTest` → `BUILD SUCCESSFUL`، ۵۵۴ تست، ۰ Failure، ۰ Error.

## Context

این اولین فایل‌های Compose واقعی این پروژه‌اند — پیش از این فقط یک `MainScreen.kt`
«Hello World» و یک `Theme.kt` موقت با رنگ‌های پیش‌فرض M3 وجود داشت. منبع حقیقت
دوگانه‌ی این فاز: `docs/blueprints/16-user-workflow-v2.md` (منطق/ساختار Navigation،
نسخه ۷/DDR-002) و `docs/design/README.md` + `docs/design/Cinema Studio.html`
(توکن‌های بصری دقیق، mockup تعاملی handoff).

## بخش الف — Design Tokens

`Color.kt`/`ExtendedColors.kt`/`Type.kt`/`Theme.kt` طبق جدول‌های Dark/Light سند
طراحی پیاده شدند. توکن‌هایی که در `ColorScheme` استاندارد Material3 جایی ندارند
(fg2/fg3/fg4 سه‌سطحی، success/warning/orange، hairline/cardBorder/inset،
solidSurface طبق Implementation Notes بند ۱) از طریق یک `CompositionLocal` جداگانه
(`CinemaTheme.extendedColors`) در دسترس‌اند — الگوی رسمی توصیه‌شده‌ی Material3 برای
گسترش تم با توکن‌های برند-محور.

### تصمیم: فونت واقعی (Inter/Vazirmatn) در این فاز پیاده نشد

سند طراحی Inter (لاتین) و Vazirmatn (فارسی) می‌خواهد، اما هیچ فایل فونت واقعی
(`.ttf`/`.otf`) در `docs/design/` ضمیمه نشده — فقط `Cinema Studio.html` + ۱۲
Screenshot PNG. `Type.kt` مقیاس واقعی (اندازه/lineHeight/وزن هر ۵ سطح) را دقیقاً
پیاده کرد، اما `FontFamily` هنوز `FontFamily.Default` (فونت سیستم) است — یک
Placeholder موقت، مستند در کد. افزودن فایل‌های واقعی به `res/font/` (یا اتصال
Google Fonts Provider) یک کار آینده‌ی مجزا و مستند است، نه یک حدس در این فاز.

### تصمیم: «Liquid Glass» (Blur/Gradient) در این فاز پیاده نشد

این فاز فقط توکن‌های خام رنگ/تایپوگرافی/Spacing را برقرار می‌کند. اعمال این
توکن‌ها روی سطوح واقعی (Card/BottomSheet/Header با Blur/Gradient واقعی) کار
فازهای بعدی (ساخت خودِ صفحات) است — دقیقاً طبق تفکیک خودِ سند («Fidelity» بخش
مربوط به Visual Language، نه این فاز ساختاری).

### تصمیم: آیکون‌ها از `material-icons-extended`، نه `-core`

`material-icons-core` فقط ~۲۴ آیکون اصلی دارد و آیکون‌های لازم برای نوار پایین
(Folder/Movie/Apps) را ندارد. `material-icons-extended` اضافه شد — تطبیق دقیق
«Material Symbols Rounded» سند (نه دقیقاً همین کتابخانه‌ی Vector Icons قدیمی‌تر)
کار فاز Visual Polish است، نه این فاز ساختاری (همان محدودیت فونت بالا).
`isMinifyEnabled` این پروژه فعلاً `false` است، پس افزایش حجم APK ناشی از
Icons-Extended فعلاً در Release هم اعمال می‌شود — فعال‌کردن R8/Minify (خارج از
Scope این فاز) این را کوچک خواهد کرد.

## بخش ب — Navigation دو‌لایه (DDR-002)

- **Routes:** `Home`/`Projects`/`Studio(projectId)`/`Assets` به‌صورت
  `@Serializable data object/class` — Navigation Compose Type-Safe (نسخه‌ی
  مدرن و توصیه‌شده‌ی AndroidX از ۲.۸+، نه رشته‌های خام دستی). `kotlinx.serialization`
  از قبل در پروژه موجود بود (واحد ۱۵)، پس هیچ وابستگی جدیدی برای همین بخش لازم نبود.
- **`MainScaffold`:** نوار پایین (لایه‌ی ۱) همیشه و بدون شرط در `bottomBar` رندر
  می‌شود؛ `StudioTopTabRow` (لایه‌ی ۲) فقط وقتی `currentDestination` زیرمجموعه‌ی
  `Studio` است اضافه می‌شود — دو لایه‌ی مکمل، نه جایگزین، دقیقاً طبق تصحیح صریح
  سند («Navigation structure is fixed, not an A/B variant»).
- **`projectId` پیش‌فرض ورودی Studio از نوار پایین:** یک ثابت Placeholder
  (`PLACEHOLDER_ACTIVE_PROJECT_ID`) — تا وقتی مفهوم «آخرین/فعال پروژه» (وابسته به
  واحد ۱۵، کار فاز بعدی) وجود ندارد، هیچ projectId واقعی برای انتخاب نیست؛ این یک
  یافته‌ی صریحاً مستندشده است، نه فرض بی‌صدا.
- **Back Navigation Contextual:** `resolveContextualBackTarget` در
  `BackNavigation.kt` — یک تابع خالص روی `Map<String, Any>` (`backTargetsByRouteKey`،
  کلید = `NavDestination.route` که برای مسیرهای Type-Safe همان نام کامل کلاس
  است). طبق دستور کار، فقط قانون Fallback («همه‌جای دیگر → Home») این فاز فعال
  است (چون Composer/Shots/Breakdown/SceneDetail هنوز نساخته شده‌اند)؛ افزودن
  قانون‌های بعدی فقط یک ردیف جدید به این Map است، بدون تغییر منطق تابع. خروجی
  `null` برای Home یعنی رفتار پیش‌فرض سیستم (خروج از اپ) اعمال شود، نه Navigate
  به خودش.
- **`StudioTab`/`evaluateStudioTabJump`:** یک enum سطح UI محض (نه domain) — چون
  گروه‌بندی ۹ مرحله‌ی `WorkflowStep` به ۴ Tab یک تصمیم Navigation دو‌لایه است، نه
  یک مفهوم دامنه‌ای جدید. هر Tab به اولین `WorkflowStep` گروهش نگاشت شد تا
  `canJumpToStep` موجود (که هرگز Block نمی‌کند، فقط هشدار می‌دهد — ADR-037) بتواند
  مستقیماً فراخوانی شود.

## بخش ج — `WorkflowViewModel`

### تصمیم Persistence: DataStore Preferences، نه Room

زبان/تم/Layout A-B چهار Preference سبک UI هستند، نه یک Entity دامنه؛ افزودن یک
جدول Room جدید فقط برای این ۴ فلگ Scalar، برخلاف الگوی «هر Aggregate یک جدول»ی
است که واحد ۱۵ در کل پروژه رعایت کرده. `DataStore Preferences` مکانیزم
توصیه‌شده‌ی رسمی AndroidX دقیقاً برای همین سناریو است (جایگزین `SharedPreferences`).

### تصمیم: خواندن اولیه با `runBlocking` در سازنده (نه Async در `init`)

عمداً، تا از «فلاش تم/زبان پیش‌فرض قبل از بارگذاری مقدار ذخیره‌شده» روی هر Cold
Start جلوگیری شود — این دقیقاً همان مشکل UX واقعی («فلاش تم») است که خیلی از
اپ‌های واقعی با یک Splash/Loading State حل می‌کنند؛ چون فایل Preferences اینجا
کوچک است (۴ کلید رشته‌ای)، این یک تأخیر ناچیز و یک‌باره است، نه I/O سنگین
مسدودکننده. Setter ها همچنان async هستند (نوشتن هرگز UI را مسدود نمی‌کند) و
مقدار StateFlow بلافاصله و همزمان (پیش از نوشتن) به‌روزرسانی می‌شود — طبق الزام
«اعمال فوری، بدون Restart».

### تصمیم: `ioScopeOverride` تزریق‌پذیر (نه `viewModelScope` هارد-کد)

تست‌های Robolectric با `viewModelScope` واقعی (که از `Dispatchers.Main` استفاده
می‌کند) در معرض قفل‌شدگی واقعی هستند: Looper اصلی Robolectric پیش‌فرض در حالت
`PAUSED` است، پس `runBlocking { job.join() }` روی همان Thread می‌تواند بی‌نهایت
منتظر بماند (هیچ‌چیزی Looper را Pump نمی‌کند تا Job تکمیل شود). راه‌حل: یک
پارامتر سازنده‌ی تزریق‌پذیر `ioScopeOverride: CoroutineScope?` (پیش‌فرض `null` ⇒
واقعاً `viewModelScope`) — تست‌ها `CoroutineScope(Dispatchers.Unconfined)` تزریق
می‌کنند (کاملاً از `kotlinx-coroutines-core` موجود، **بدون هیچ وابستگی تست
جدید** مثل `kotlinx-coroutines-test`)؛ پس از تعلیق واقعی داخل `DataStore.edit`
(که خودش هنوز به‌طور واقعی روی `Dispatchers.IO` می‌رود)، ادامه‌ی اجرا روی همان
Thread پس‌زمینه اتفاق می‌افتد، نه اینکه منتظر Pump شدن Looper اصلی بماند — پس
`runBlocking { job.join() }` در تست‌ها بدون قفل‌شدگی و به‌صورت Deterministic کار
می‌کند.

### تصمیم: `AppLanguage` جدید تعریف نشد — `domain.outputdelivery.Language` بازاستفاده شد

با grep تأیید شد `domain.outputdelivery.Language` (FA/EN) از قبل در `Bilingual.kt`
واحد ۱۴ تعریف شده بود اما **هیچ مصرف‌کننده‌ی واقعی UI نداشت** — فقط تست خودِ واحد
۱۴. افزودن یک `AppLanguage` جدید و موازی همان الگوی «نوع تکراری» بود که این پروژه
بارها (ADR-036، ADR-038) از آن پرهیز کرده؛ به‌جایش `Language` مستقیماً بازاستفاده
شد و `domain.outputdelivery.t()` هم برای `ui/i18n/UiStrings.kt` (کلیدهای
Navigation/Tab این فاز) به کار رفت — **اولین مصرف‌کننده‌ی واقعی این تابع در کل
پروژه**. تست `validateTranslationCoverage` (که هم قبلاً فقط در تست خودش استفاده
شده بود) هم روی این Map جدید سیم‌کشی شد.

`AppTheme`/`HomeLayoutVariant`/`ComposerLayoutVariant` سه enum جدید در
`domain/workflow/WorkflowModels.kt` تعریف شدند (نه در `ui/`) — دقیقاً هم‌الگو با
`ShotListViewMode`/`FeedbackType` موجود همان فایل: enum های سبک UI که فقط لایه‌ی
Compose مصرف می‌کند، اما محل طبیعی‌شان `domain/workflow` است چون این دقیقاً پکیج
«وضعیت گردش‌کار/UI» پروژه است.

### تصمیم: State های ناوبری/انتخاب لحظه‌ای در `WorkflowViewModel` نیستند

`docs/design/README.md` بخش State Management صریحاً «Language/theme/layout
picks» را «Persisted across sessions» فهرست کرده، اما «Active project tab
(Studio: 0-3)» و مشابه‌هایش را در دسته‌ی جدا («Current screen + navigation
history») — یعنی خودِ سند هم این دو نوع State را از هم تفکیک کرده. طبق همین
تفکیک، انتخاب Tab فعلی Studio در `MainScaffold` با `rememberSaveable` محلی نگه
داشته می‌شود، نه در `WorkflowViewModel`/DataStore.

### `WorkflowState` — nullable

`WorkflowState` (`domain.workflow`) فیلدهای الزامی `sessionId`/`projectId` دارد —
قبل از این‌که کاربر واقعاً وارد یک پروژه شود (فراخوانی `startWorkflowSession`، کار
فاز بعدی هنگام ساخت جریان واقعی ورود به Studio)، هیچ Session واقعی‌ای وجود ندارد؛
پس `workflowState: StateFlow<WorkflowState?>` عمداً nullable است — به‌جای ساختن
یک Session جعلی با شناسه‌های Placeholder.

## الزام سخت‌گیرانه‌ی RTL

`App.kt` جهت `LocalLayoutDirection` را بر اساس زبان انتخابی کاربر (نه Locale
سیستم) Override می‌کند — چون کاربر می‌تواند مستقل از Locale دستگاه، زبان اپ را از
داخل خودِ اپ عوض کند. `ui/i18n/BidiUtils.kt` (`String.asLtrToken()`) زیرساخت
Unicode Bidi Isolate (FSI/PDI) را برای توکن‌های فنی لاتین/عددی آماده کرد —
هنوز هیچ Composable واقعی این تابع را مصرف نمی‌کند (این فاز هیچ محتوای متنی
فنی/کد ندارد)، اما زیرساخت از همان ابتدا آماده است، طبق الزام صریح «hard
requirement, not a nice-to-have» سند.

## تست

- `WorkflowViewModelTest.kt` (۵ تست): مقداردهی اولیه، اعمال فوری زبان/تم، Persistence
  واقعی Round-Trip (نه Mock — دو نمونه‌ی جدا از ViewModel روی همان فایل DataStore)،
  `startWorkflowSession`.
- `AppNavigationTest.kt` (۴ تست، اولین Compose UI Test پروژه با Robolectric):
  نوار پایین همیشه روی Home؛ Top Tab Row فقط بعد از ورود به Studio؛ نوار پایین
  همچنان روی Studio هم هست (لایه‌ی مکمل)؛ Top Tab Row بعد از خروج از Studio دوباره
  محو می‌شود.
- `BackNavigationTest.kt` (۵ تست): `null`/Home بدون مقصد؛ سه مسیر ریشه‌ی دیگر همه
  Fallback به Home.
- `StudioTopTabRowTest.kt` (۳ تست): جابه‌جایی هرگز Block نمی‌شود، حتی بدون
  WorkflowState فعال؛ هشدار فقط وقتی مراحل قبلی ناقص‌اند.
- `UiStringsTest.kt` (۳ تست): پوشش کامل کلید fa/en (`validateTranslationCoverage`
  واقعی)؛ Fallback به EN؛ هیچ کلیدی echo نشده.

جمعاً ۲۰ تست جدید (۵۳۴ → ۵۵۴). هیچ تست موجودی نشکست.

## Consequences

- **آسان می‌شود:** فازهای بعدی واحد ۱۶ (ساخت خودِ ۱۲ صفحه) می‌توانند مستقیماً از
  `CinemaTheme`/`MaterialTheme` و ساختار Navigation موجود استفاده کنند، بدون
  نیاز به بازطراحی زیرساخت.
- **کار آینده‌ی شناخته‌شده (نه بدهی پنهان):** فایل‌های فونت واقعی Inter/Vazirmatn؛
  Liquid Glass (Blur/Gradient) روی سطوح واقعی؛ آیکون‌های دقیق Material Symbols
  Rounded؛ مفهوم «آخرین/فعال پروژه» برای جایگزینی `PLACEHOLDER_ACTIVE_PROJECT_ID`؛
  قوانین Back Navigation اضافه‌تر (Composer→Shots و ...) وقتی آن صفحات ساخته
  شدند؛ مصرف واقعی `String.asLtrToken()` وقتی محتوای متنی فنی/کد ظاهر شود.
